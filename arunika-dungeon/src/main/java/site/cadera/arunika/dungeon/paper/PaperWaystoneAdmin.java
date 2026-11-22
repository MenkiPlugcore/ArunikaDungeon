package site.cadera.arunika.dungeon.paper;

import java.util.*;
import java.util.function.Consumer;
import java.lang.reflect.InvocationTargetException;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import org.bukkit.plugin.Plugin;
import net.kyori.adventure.text.Component;

/** Admin UI belongs to Dungeon; the core owns registry metadata and never teleports players. */
final class PaperWaystoneAdmin implements Listener,AutoCloseable {
 private final ArunikaDungeonPlugin plugin;
 private static final class Menu implements InventoryHolder {
  final UUID owner;final Map<Integer,Consumer<Player>> actions=new HashMap<>();Inventory inventory;boolean pending;
  Menu(UUID owner){this.owner=owner;}
  public Inventory getInventory(){return inventory;}
 }
 PaperWaystoneAdmin(ArunikaDungeonPlugin plugin){this.plugin=plugin;Bukkit.getPluginManager().registerEvents(this,plugin);}
 private boolean allowed(Player p){return p.isOnline()&&p.hasPermission("arunika.dungeon.admin")&&p.hasPermission("arunika.dungeonwaystone.admin");}
 private Plugin core(){var p=Bukkit.getPluginManager().getPlugin("ArunikaDungeonWaystone");if(p==null||!p.isEnabled())throw new IllegalStateException("Core Waystone belum aktif");return p;}
 private Object call(String name,Class<?>[] types,Object...args){
  try{var c=core();return c.getClass().getMethod(name,types).invoke(c,args);}
  catch(InvocationTargetException e){throw new IllegalStateException(e.getCause().getMessage(),e.getCause());}
  catch(ReflectiveOperationException e){throw new IllegalStateException("Update ketiga JAR ke RC2; API nama Waystone belum tersedia",e);}
 }
 @SuppressWarnings("unchecked") private List<Map<String,String>> nodes(){return (List<Map<String,String>>)call("adminNodes",new Class<?>[0]);}
 private Map<String,String> node(UUID id){return nodes().stream().filter(n->n.get("id").equals(id.toString())).findFirst().orElseThrow(()->new IllegalStateException("Waystone sudah dihapus"));}
 private Menu menu(Player p,String title){Menu m=new Menu(p.getUniqueId());m.inventory=Bukkit.createInventory(m,54,Component.text(title));return m;}
 private void button(Menu m,int slot,Material material,String title,Consumer<Player> action,String...lines){var item=new ItemStack(material);var meta=item.getItemMeta();meta.displayName(GuiTheme.name(title,material));meta.lore(Arrays.stream(lines).map(GuiTheme::lore).toList());item.setItemMeta(meta);m.inventory.setItem(slot,item);if(action!=null)m.actions.put(slot,action);}
 private void show(Player p,Menu m){if(!allowed(p))return;for(int i=45;i<54;i++)if(m.inventory.getItem(i)==null)button(m,i,Material.BLACK_STAINED_GLASS_PANE," ",null);p.openInventory(m.inventory);}
 private void confirm(Player p,String title,Consumer<Player> yes,Consumer<Player> back){Menu m=menu(p,"Konfirmasi | "+title);button(m,20,Material.LIME_CONCRETE,"Konfirmasi",yes);button(m,24,Material.RED_CONCRETE,"Batal",back);show(p,m);}
 void home(Player p,int page){
  if(!allowed(p))return;
  try{
   var values=nodes();int current=Math.max(0,Math.min(page,Math.max(0,(values.size()-1)/36)));Menu m=menu(p,"Arunika | Pengaturan Waystone");
   for(int i=current*36;i<Math.min(values.size(),(current+1)*36);i++){var n=values.get(i);UUID id=UUID.fromString(n.get("id"));button(m,i%36,Material.LODESTONE,n.get("name"),q->edit(q,id),"Jenis: "+n.get("role"),"Arena: "+(n.get("target").isEmpty()?"Menu Dungeon":n.get("target")),n.get("world")+" | "+n.get("position"));}
   if(values.isEmpty())button(m,13,Material.PAPER,"Belum ada Waystone",null,"Lihat Lodestone baru lalu pilih Buat HUB");
   button(m,36,Material.EMERALD,"Buat HUB pada Lodestone yang dilihat",q->create(q,"HUB","",captureBlock(q)),"Nama diisi lewat chat, tanpa command panjang");
   button(m,38,Material.ENDER_PEARL,"Buat RETURN pada Lodestone yang dilihat",q->chooseArena(q,0,"create",null,captureBlock(q)),"Pilih arena lalu beri nama");
   button(m,40,Material.COMPASS,"Titik pulang HUB = posisi saya",q->confirm(q,"Titik pulang HUB",x->{x.closeInventory();call("adminExport",new Class<?>[]{Player.class},x);plugin.applyWaystoneBindings(x);},x->home(x,current)),"Berdiri di titik aman dekat HUB","Menyimpan titik pulang dan menerapkan binding");
   button(m,42,Material.REDSTONE,"Terapkan binding terbaru",q->{q.closeInventory();call("adminSyncExport",new Class<?>[]{Player.class},q);plugin.applyWaystoneBindings(q);},"Gunakan setelah membuat/mengubah RETURN","Titik pulang HUB yang tersimpan dipertahankan");
   button(m,44,Material.LEVER,plugin.adminSnapshot().battlePreview()?"Nonaktifkan mode Dungeon":"Aktifkan mode Dungeon",q->confirm(q,"Mode Dungeon",x->{x.closeInventory();plugin.setDungeonModes(x,!plugin.adminSnapshot().battlePreview());},x->home(x,current)),"Battle, Shared, Private dan loot","Arena/boss tetap perlu dikonfigurasi");
   button(m,45,Material.ARROW,"Sebelumnya",q->home(q,current-1));button(m,47,Material.SPAWNER,"Atur arena/boss/wave",q->q.performCommand("adg admin"));button(m,48,Material.AMETHYST_SHARD,"Pakai model Java Dungeon",q->confirm(q,"Model Java addon",x->{call("adminDungeonModels",new Class<?>[]{Player.class},x);home(x,current);},x->home(x,current)),"Pasang addon ItemsAdder dan bangun pack dahulu");button(m,49,Material.CLOCK,"Refresh daftar",q->home(q,current));button(m,51,Material.ENDER_EYE,"Set spawn arena di posisi saya",q->chooseArena(q,0,"spawn",null,null));button(m,53,Material.ARROW,"Berikutnya",q->home(q,current+1));show(p,m);
  }catch(RuntimeException e){p.sendMessage("Panel Waystone: "+e.getMessage());}
 }
 private Block captureBlock(Player p){Block b=p.getTargetBlockExact(8);if(b==null||b.getType()!=Material.LODESTONE)throw new IllegalArgumentException("Lihat Lodestone baru dalam 8 blok, dengan AIR di atasnya");return b;}
 private void create(Player p,String role,String target,Block anchor){
  if(!allowed(p))return;
  if(role.equals("RETURN")&&!plugin.gatewayStoneInArena(target,anchor.getLocation()))throw new IllegalArgumentException("Lodestone RETURN harus berada di batas arena pilihan");
  plugin.adminInput().ask(p,"Nama Waystone (1–40 huruf/angka/spasi)",()->allowed(p),text->{
   UUID id=(UUID)call("adminCreate",new Class<?>[]{Player.class,String.class,String.class,String.class,Block.class},p,role,target,text,anchor);p.sendMessage("Waystone berhasil dibuat");edit(p,id);
  });
 }
 private void edit(Player p,UUID id){
  var n=node(id);Menu m=menu(p,"Waystone | "+n.get("name"));
  button(m,13,Material.LODESTONE,n.get("name"),null,"UUID: "+id,"Jenis: "+n.get("role"),"Arena: "+n.get("target"),n.get("world")+" | "+n.get("position"));
  button(m,20,Material.NAME_TAG,"Ganti nama",q->plugin.adminInput().ask(q,"Nama baru Waystone",()->allowed(q),text->{call("adminRename",new Class<?>[]{Player.class,UUID.class,String.class},q,id,text);edit(q,id);}));
  if(n.get("role").equals("RETURN")){
   button(m,22,Material.COMPASS,"Ganti binding arena",q->chooseArena(q,0,"bind",id,null),"RETURN hanya dapat dipakai peserta arena terikat");
   button(m,24,Material.ENDER_PEARL,"Set spawn arena terikat di posisi saya",q->{String[] target=node(id).get("target").split("/",2);if(target.length!=2)throw new IllegalStateException("Binding tidak valid");q.closeInventory();plugin.setArenaSpawn(q,target[0],target[1]);},"Berdiri di tujuan teleport di dalam arena","Tidak memindahkan Waystone");
  }else button(m,24,Material.COMPASS,"Set titik pulang HUB di posisi saya",q->confirm(q,"Titik pulang HUB",x->{x.closeInventory();call("adminExport",new Class<?>[]{Player.class},x);plugin.applyWaystoneBindings(x);},x->edit(x,id)));
  button(m,29,Material.PISTON,"Pindahkan ke Lodestone yang dilihat",q->confirm(q,"Pindah "+node(id).get("name"),x->{call("adminMove",new Class<?>[]{Player.class,UUID.class,Block.class},x,id,captureBlock(x));x.sendMessage("Waystone dipindah; periksa/tetapkan ulang binding HUB/RETURN");edit(x,id);},x->edit(x,id)),"Lihat Lodestone baru sebelum membuka /adw","UUID dan nama dipertahankan");
  button(m,31,Material.BOOK,"Lihat detail di chat",q->{q.sendMessage(n.get("name")+" | UUID "+id+" | "+n.get("role")+" | "+n.get("target")+" | "+n.get("world")+" "+n.get("position"));});
  button(m,49,Material.CLOCK,"Refresh model",q->{q.closeInventory();q.performCommand("adw refresh");home(q,0);});button(m,53,Material.ARROW,"Kembali",q->home(q,0));show(p,m);
 }
 private record ArenaRef(String floor,String arena,String title){}
 private void chooseArena(Player p,int page,String action,UUID nodeId,Block anchor){
  List<ArenaRef> values=new ArrayList<>();for(var f:plugin.floors()){var plan=plugin.adminSnapshot().battles().get(f.id());if(plan!=null)for(var a:plan.arenas())values.add(new ArenaRef(f.id(),a.id(),f.name()+" / "+a.id()));}
  int current=Math.max(0,Math.min(page,Math.max(0,(values.size()-1)/36)));Menu m=menu(p,"Pilih Arena | "+(action.equals("spawn")?"Tujuan Teleport":"RETURN"));
  for(int i=current*36;i<Math.min(values.size(),(current+1)*36);i++){var a=values.get(i);button(m,i%36,Material.SPAWNER,a.title(),q->{
   String target=a.floor()+"/"+a.arena();
   switch(action){
    case "create"->create(q,"RETURN",target,anchor);
    case "bind"->{var n=node(nodeId);Plugin c=core();Object node;try{node=c.getClass().getMethod("getNode",UUID.class).invoke(c,nodeId);Location l=(Location)c.getClass().getMethod("location",node.getClass()).invoke(c,node);if(!plugin.gatewayStoneInArena(target,l))throw new IllegalArgumentException("RETURN berada di luar arena pilihan");}catch(ReflectiveOperationException e){throw new IllegalStateException("Waystone tidak tersedia",e);}call("adminBind",new Class<?>[]{Player.class,UUID.class,String.class},q,nodeId,target);q.sendMessage("Binding disimpan. Terapkan binding terbaru dari panel utama.");edit(q,nodeId);}
    case "spawn"->{q.closeInventory();plugin.setArenaSpawn(q,a.floor(),a.arena());}
    default->throw new IllegalArgumentException("Aksi tidak valid");
   }
  },a.floor()+" / "+a.arena());}
  if(values.isEmpty())button(m,13,Material.PAPER,"Belum ada arena aktif",null,"Buka /adg admin untuk setup boss, wave dan arena");
  button(m,45,Material.ARROW,"Sebelumnya",q->chooseArena(q,current-1,action,nodeId,anchor));button(m,49,Material.COMPASS,"Kembali",q->home(q,0));button(m,53,Material.ARROW,"Berikutnya",q->chooseArena(q,current+1,action,nodeId,anchor));show(p,m);
 }
 @EventHandler(priority=EventPriority.HIGHEST) public void click(InventoryClickEvent e){
  if(!(e.getView().getTopInventory().getHolder() instanceof Menu m))return;boolean was=e.isCancelled();e.setCancelled(true);if(was)return;
  if(!(e.getWhoClicked() instanceof Player p)||!allowed(p)||!p.getUniqueId().equals(m.owner)||e.getRawSlot()<0||e.getRawSlot()>=54||(e.getClick()!=ClickType.LEFT&&e.getClick()!=ClickType.RIGHT))return;
  Consumer<Player> action=m.actions.get(e.getRawSlot());if(action==null||m.pending)return;m.pending=true;
  Bukkit.getScheduler().runTask(plugin,()->{m.pending=false;if(!allowed(p)||p.getOpenInventory().getTopInventory()!=m.inventory)return;try{action.accept(p);}catch(RuntimeException ex){p.sendMessage("Pengaturan ditolak: "+ex.getMessage());}});
 }
 @EventHandler(priority=EventPriority.HIGHEST) public void drag(InventoryDragEvent e){if(e.getView().getTopInventory().getHolder() instanceof Menu)e.setCancelled(true);}
 @Override public void close(){HandlerList.unregisterAll(this);for(Player p:Bukkit.getOnlinePlayers())if(p.getOpenInventory().getTopInventory().getHolder() instanceof Menu)p.closeInventory();}
}
