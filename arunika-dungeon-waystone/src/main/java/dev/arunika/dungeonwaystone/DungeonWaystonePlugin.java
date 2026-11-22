package dev.arunika.dungeonwaystone;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Dungeon-only anchor registry. Player gameplay/GUI/teleport belong to ArunikaDungeon; admin GUI commands delegate to it.
 *  All dungeon game rules and interaction are owned exclusively by ArunikaDungeon.
 */
public final class DungeonWaystonePlugin extends JavaPlugin implements Listener, TabExecutor {
    private final Map<UUID,Node> nodes = new LinkedHashMap<>();
    private final Map<UUID,String> names = new LinkedHashMap<>();
    private final Map<String,UUID> byBlock = new HashMap<>();
    private File storage;
    private boolean ready;
    private DungeonFurnitureVisuals visuals;

    @Override public void onEnable() {
        saveDefaultConfig();
        storage = new File(getDataFolder(), "anchors.yml");
        try { readStorage(); ready = true; }
        catch (Exception e) { ready = false; getLogger().severe("Waystone registry locked to prevent data loss: " + e.getMessage()); }
        visuals = new DungeonFurnitureVisuals(this);
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getScheduler().runTaskTimer(this, () -> { if (ready && visuals != null) visuals.refreshLoaded(); }, 100L, 100L);
        if (getCommand("adw") != null) { getCommand("adw").setExecutor(this); getCommand("adw").setTabCompleter(this); }
        getLogger().info("Dungeon-only node registry: " + nodes.size() + " anchor(s), ready=" + ready + "; survival Waystones untouched");
    }
    @Override public void onDisable() { if (visuals != null) visuals.removeAll(true); ready = false; nodes.clear(); byBlock.clear(); names.clear(); }
    public boolean available() { return ready && isEnabled(); }
    public List<Node> getNodes() { return ready ? List.copyOf(nodes.values()) : List.of(); }
    public Node getNode(UUID id) { return ready && id != null ? nodes.get(id) : null; }
    public Node getNode(Block block) {
        if (!ready || block == null || (block.getType() != Material.LODESTONE && block.getType() != Material.BARRIER)) return null;
        UUID id = byBlock.get(key(block.getWorld().getUID(),block.getX(),block.getY(),block.getZ()));
        if (id == null && block.getType() == Material.BARRIER) id = byBlock.get(key(block.getWorld().getUID(),block.getX(),block.getY()-1,block.getZ()));
        Node n = id == null ? null : nodes.get(id);
        return n != null && validAnchor(n) ? n : null;
    }
    /** Return only model entities carrying our plugin-specific PDC ownership marker. */
    public Node getNode(Entity entity) {
        if(!ready||entity==null)return null;
        String proxy=entity.getPersistentDataContainer().get(new org.bukkit.NamespacedKey("arunikadungeonwaystonebedrock","proxy_node"),org.bukkit.persistence.PersistentDataType.STRING);
        if(proxy!=null)try{
            Node n=getNode(UUID.fromString(proxy));Location loc=location(n);
            return dedicated(n)&&loc!=null&&entity.getWorld()==loc.getWorld()&&entity.getLocation().distanceSquared(loc.clone().add(.5,0,.5))<=9?n:null;
        }catch(IllegalArgumentException e){return null;}
        return visuals!=null?visuals.node(entity):null;
    }
    public UUID id(Node n) { return n == null ? null : n.id(); }
    public Location location(Node n) {
        if (n == null) return null;
        World w = Bukkit.getWorld(n.worldId());
        return w == null ? null : new Location(w,n.x(),n.y(),n.z());
    }
    public boolean dedicated(Node n) {
        return available() && n != null && nodes.get(n.id()) == n && validAnchor(n) && !legacyCollision(location(n));
    }
    public boolean isHub(Node n) { return dedicated(n) && n.role() == Node.Role.HUB; }
    public boolean isReturn(Node n,String target) { return dedicated(n) && n.role() == Node.Role.RETURN && n.target().equals(target); }
    /** Authoritative distance and permission validation; no combat/session mutation here. */
    public boolean allowed(Player p,Node n,double radius,boolean entry) {
        if (p == null || !p.isOnline() || p.isDead() || !p.hasPermission("arunika.dungeonwaystone.use") || !dedicated(n)) return false;
        if (!Double.isFinite(radius) || radius < 1 || radius > 16) return false;
        Location l=location(n);
        if (l==null || p.getWorld()!=l.getWorld() || !l.getWorld().isChunkLoaded(l.getBlockX()>>4,l.getBlockZ()>>4)) return false;
        if (p.getLocation().distanceSquared(l.clone().add(.5,.5,.5)) > radius*radius) return false;
        Location eye=p.getEyeLocation(),center=l.clone().add(.5,1,.5);
        org.bukkit.util.Vector delta=center.toVector().subtract(eye.toVector());double distance=delta.length();
        if(distance>.001){
            var hit=l.getWorld().rayTraceBlocks(eye,delta.multiply(1/distance),distance,org.bukkit.FluidCollisionMode.NEVER,true);
            if(hit!=null&&hit.getHitBlock()!=null){Block block=hit.getHitBlock();if(block.getX()!=n.x()||block.getZ()!=n.z()||(block.getY()!=n.y()&&block.getY()!=n.y()+1))return false;}
        }
        return !entry || (isHub(n) && !p.isFlying() && p.getGameMode()!=GameMode.CREATIVE && p.getGameMode()!=GameMode.SPECTATOR);
    }
    private boolean validAnchor(Node n) {
        Location l=location(n); return l!=null && l.getWorld().isChunkLoaded(n.x()>>4,n.z()>>4) && (l.getBlock().getType()==Material.LODESTONE || l.getBlock().getType()==Material.BARRIER);
    }
    private static String key(UUID world,int x,int y,int z){return world+":"+x+":"+y+":"+z;}
    private String key(Node n){return key(n.worldId(),n.x(),n.y(),n.z());}
    private static String key(Block b){return key(b.getWorld().getUID(),b.getX(),b.getY(),b.getZ());}
    private void readStorage() throws IOException {
        var data=AnchorCodec.read(storage);nodes.clear();byBlock.clear();names.clear();nodes.putAll(data.nodes());names.putAll(data.names());for(Node n:nodes.values())byBlock.put(key(n),n.id());
    }
    /** Atomic replace; if write fails keep old registry and refuse mutation. */
    private void persist(Map<UUID,Node> next) throws IOException {persist(next,new LinkedHashMap<>(names));}
    private void persist(Map<UUID,Node> next,Map<UUID,String> nextNames) throws IOException {
        Map<UUID,String> staged=new LinkedHashMap<>();
        for(Node n:next.values()){String name=NodeNames.clean(nextNames.getOrDefault(n.id(),NodeNames.fallback(n)));NodeNames.requireUnique(staged,n.id(),name);staged.put(n.id(),name);}
        YamlConfiguration y=AnchorCodec.encode(next,staged);
        Files.createDirectories(getDataFolder().toPath());
        Path backup=getDataFolder().toPath().resolve("anchors-before-RC2.yml");
        if(storage.isFile()&&!Files.exists(backup))Files.copy(storage.toPath(),backup);
        Path tmp=Files.createTempFile(getDataFolder().toPath(),"anchors-",".tmp");
        try {
            Files.writeString(tmp,y.saveToString());
            try(var ch=java.nio.channels.FileChannel.open(tmp,java.nio.file.StandardOpenOption.WRITE)){ch.force(true);}
            try{Files.move(tmp,storage.toPath(),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
            catch(AtomicMoveNotSupportedException e){Files.move(tmp,storage.toPath(),StandardCopyOption.REPLACE_EXISTING);}
            try(var ch=java.nio.channels.FileChannel.open(storage.toPath().getParent(),java.nio.file.StandardOpenOption.READ)){ch.force(true);}catch(IOException|UnsupportedOperationException ignored){}
        } finally {Files.deleteIfExists(tmp);}
        nodes.clear();nodes.putAll(next);names.clear();names.putAll(staged);byBlock.clear();for(Node n:nodes.values())byBlock.put(key(n),n.id());
    }
    private boolean legacyCollision(Location loc) {
        if(loc==null) return true;
        Plugin legacy=Bukkit.getPluginManager().getPlugin("CdrWaystone");
        if(legacy==null||!legacy.isEnabled())return false;
        try {
            Object registry=legacy.getClass().getMethod("registry").invoke(legacy);
            Object hit=registry.getClass().getMethod("find",Location.class).invoke(registry,loc);
            return hit!=null;
        } catch(Exception | LinkageError e) {return true;} // fail closed, no collisions
    }
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args) {
        if(!sender.hasPermission("arunika.dungeonwaystone.admin")){sender.sendMessage("No permission.");return true;}
        if(!ready){sender.sendMessage("Dungeon Waystone registry is locked. Inspect logs; do not reset data.");return true;}
        String action=args.length==0&&sender instanceof Player?"gui":args.length==0?"status":args[0].toLowerCase(Locale.ROOT);
        if(action.equals("gui")){
            if(!(sender instanceof Player player)){sender.sendMessage("Jalankan /adw di dalam game");return true;}
            Plugin dungeon=Bukkit.getPluginManager().getPlugin("ArunikaDungeon");
            try{if(dungeon==null||!dungeon.isEnabled())throw new IllegalStateException("ArunikaDungeon belum aktif");dungeon.getClass().getMethod("openWaystoneAdmin",Player.class).invoke(dungeon,player);}
            catch(Exception e){sender.sendMessage("GUI Waystone tidak tersedia: "+e.getMessage());}return true;
        }
        if(action.equals("sethub")){
            if(!(sender instanceof Player player)){sender.sendMessage("Player required");return true;}
            try{adminExport(player);Bukkit.dispatchCommand(player,"arunikadungeon:arunikadungeon gateway import");}catch(Exception e){sender.sendMessage("Titik pulang ditolak: "+e.getMessage());}return true;
        }
        if (action.equals("refresh")) { visuals.refreshLoaded(); sender.sendMessage("Refreshed loaded Dungeon Waystone models."); return true; }
        if (action.equals("status")||action.equals("check")) {
            long broken=nodes.values().stream().filter(n->!validAnchor(n)).count();
            long collisions=nodes.values().stream().filter(n->legacyCollision(location(n))).count();
            sender.sendMessage("[Dungeon Waystone] nodes="+nodes.size()+" hub="+nodes.values().stream().filter(n->n.role()==Node.Role.HUB).count()+" broken="+broken+" legacy-collision="+collisions);
            sender.sendMessage("ArunikaDungeon="+pluginStatus("ArunikaDungeon")+" | Bedrock="+pluginStatus("ArunikaDungeonWaystoneBedrock"));return true;
        }
        if (action.equals("list")) { for(Node n:nodes.values())sender.sendMessage(nodeName(n.id())+" | "+n.role()+" "+n.id()+" "+n.target()+" @ "+n.worldName()+" "+n.x()+","+n.y()+","+n.z()); if(nodes.isEmpty())sender.sendMessage("No dungeon anchors registered.");return true; }
        if (action.equals("export")) {if(!(sender instanceof Player player)){sender.sendMessage("Stand at the safe Dungeon Hub return point, then /adw export.");return true;}try{writeExport(player.getLocation());sender.sendMessage("Export written to plugins/ArunikaDungeonWaystone/waystone-export.yml. Gunakan tombol Terapkan Binding pada /adw atau /adg gateway import");}catch(IOException e){sender.sendMessage("Export error: "+e.getMessage());}return true;}
        if(action.equals("model")){
            if(args.length!=3||!Set.of("hub","return").contains(args[1].toLowerCase(Locale.ROOT))||!args[2].matches("[a-z0-9_]+(:[a-z0-9_]+)?")){sender.sendMessage("/adw model <hub|return> <skin-or-namespaced-ID> (Java ItemsAdder)");return true;}
            visuals.removeAll(true);getConfig().set("visuals."+args[1].toLowerCase(Locale.ROOT)+"-skin",args[2]);saveConfig();visuals.refreshLoaded();sender.sendMessage("Java model selection saved. Bedrock keeps original Blackstone/Portstone models.");return true;
        }
        if(!(sender instanceof Player p)){sender.sendMessage("Player required. Stand near a Lodestone.");return true;}
        Block b=p.getTargetBlockExact(8);
        if(action.equals("inspect")){Node n=b==null?null:getNode(b);sender.sendMessage(n==null?"No Dungeon Waystone at target":nodeName(n.id())+" | "+n.role()+" "+n.id()+" "+n.target());return true;}
        if(b==null){sender.sendMessage("Look at a Lodestone block within 8 blocks.");return true;}
        if(action.equals("rename")){
            try{Node n=getNode(b);if(n==null||args.length<2)throw new IllegalArgumentException("Lihat Waystone lalu /adw rename <nama>");adminRename(p,n.id(),String.join(" ",java.util.Arrays.copyOfRange(args,1,args.length)));p.sendMessage("Nama Waystone: "+nodeName(n.id()));}catch(Exception e){p.sendMessage(e.getMessage());}return true;
        }
        if(action.equals("move")){
            try{
                if(args.length<2)throw new IllegalArgumentException("/adw move <nama atau UUID> sambil melihat Lodestone baru");
                String labelName=String.join(" ",java.util.Arrays.copyOfRange(args,1,args.length));UUID id=resolveName(labelName);adminMove(p,id,b);sender.sendMessage("Waystone dipindah: "+nodeName(id)+". Atur titik pulang/terapkan binding lewat /adw.");
            }catch(Exception e){sender.sendMessage("Pindah ditolak: "+e.getMessage());}return true;
        }
        if(action.equals("remove")) {Node n=getNode(b);if(n==null){sender.sendMessage("No Dungeon Waystone at target.");return true;}try{Map<UUID,Node> next=new LinkedHashMap<>(nodes);next.remove(n.id());persist(next);visuals.remove(n,true);sender.sendMessage("Unregistered "+n.id());}catch(IOException ex){sender.sendMessage("Save failed; registry unchanged: "+ex.getMessage());}return true;}
        if(!action.equals("hub")&&!action.equals("return")){sender.sendMessage("/adw [hub|return <floor> <arena>|move <uuid>|model <hub|return> <skin>|remove|list|status|inspect|export|check|refresh]");return true;}
        try{
            String target="",name;
            if(action.equals("hub")){name=args.length>1?String.join(" ",java.util.Arrays.copyOfRange(args,1,args.length)):"Dungeon HUB";}
            else{
                if(args.length<3)throw new IllegalArgumentException("/adw return <floor> <arena> [nama]");
                target=args[1]+"/"+args[2];name=args.length>3?String.join(" ",java.util.Arrays.copyOfRange(args,3,args.length)):"Return "+args[1].substring(0,Math.min(18,args[1].length()))+" "+args[2].substring(0,Math.min(14,args[2].length()));
            }
            UUID id=adminCreate(p,action.toUpperCase(Locale.ROOT),target,name,b);
            p.sendMessage("Waystone dibuat: "+nodeName(id)+" ("+id+"). /adw membuka pengaturannya.");
        }catch(Exception e){p.sendMessage("Tidak dapat membuat Waystone: "+e.getMessage());}
        return true;
    }
    private void requireAdmin(Player p){
        if(!Bukkit.isPrimaryThread()||p==null||!p.isOnline()||!p.hasPermission("arunika.dungeonwaystone.admin")||!available())throw new IllegalStateException("Admin/registry tidak tersedia");
        Plugin owner=Bukkit.getPluginManager().getPlugin("ArunikaDungeon");
        if(owner!=null&&owner.isEnabled())try{if(Boolean.FALSE.equals(owner.getClass().getMethod("waystoneSetupIdle").invoke(owner)))throw new IllegalStateException("Selesaikan sesi/party/antrean dan operasi setup sebelum mengubah Waystone");}catch(NoSuchMethodException ignored){}catch(ReflectiveOperationException e){throw new IllegalStateException("Status Dungeon tidak dapat diverifikasi",e);}
    }
    public void adminDungeonModels(Player p){
        requireAdmin(p);visuals.removeAll(true);getConfig().set("visuals.hub-skin","arunika_dungeon_waystones:waystone_blackstone");getConfig().set("visuals.return-skin","arunika_dungeon_waystones:waystone_portstone");saveConfig();visuals.refreshLoaded();p.sendMessage("Model Java addon dipilih untuk HUB dan RETURN");
    }
    public String nodeName(UUID id){Node n=getNode(id);return n==null?"":names.getOrDefault(id,NodeNames.fallback(n));}
    public List<Map<String,String>> adminNodes(){
        List<Map<String,String>> out=new ArrayList<>();for(Node n:getNodes())out.add(Map.of("id",n.id().toString(),"name",nodeName(n.id()),"role",n.role().name(),"target",n.target(),"world",n.worldName(),"position",n.x()+", "+n.y()+", "+n.z()));return List.copyOf(out);
    }
    public UUID adminCreate(Player p,String role,String target,String name,Block b)throws IOException{
        requireAdmin(p);Node.Role kind=Node.Role.valueOf(role);String label=NodeNames.clean(name);NodeNames.requireUnique(names,null,label);
        if(b==null||b.getWorld()!=p.getWorld()||p.getEyeLocation().distanceSquared(b.getLocation().add(.5,.5,.5))>81||p.getTargetBlockExact(8)==null||!p.getTargetBlockExact(8).equals(b))throw new IllegalArgumentException("Lihat Lodestone tujuan dalam 8 blok");
        if(b.getType()!=Material.LODESTONE||b.getRelative(0,1,0).getType()!=Material.AIR||byBlock.containsKey(key(b)))throw new IllegalArgumentException("Pakai Lodestone baru dengan AIR di atasnya");
        if(legacyCollision(b.getLocation()))throw new IllegalArgumentException("Lodestone milik Waystone survival/tidak dapat diverifikasi");
        if(nodes.size()>=Math.min(128,Math.max(1,getConfig().getInt("max-nodes",64))))throw new IllegalStateException("Batas Waystone tercapai");
        if(kind==Node.Role.HUB&&nodes.values().stream().anyMatch(n->n.role()==Node.Role.HUB))throw new IllegalArgumentException("HUB sudah ada; pilih lewat /adw");
        if(kind==Node.Role.RETURN&&nodes.values().stream().anyMatch(n->n.role()==Node.Role.RETURN&&n.target().equals(target)))throw new IllegalArgumentException("Arena sudah punya RETURN");
        Node n=new Node(UUID.randomUUID(),kind,target,b.getWorld().getUID(),b.getWorld().getName(),b.getX(),b.getY(),b.getZ());
        Map<UUID,Node> next=new LinkedHashMap<>(nodes);next.put(n.id(),n);Map<UUID,String> labels=new LinkedHashMap<>(names);labels.put(n.id(),label);persist(next,labels);
        if(!visuals.materialize(n))p.sendMessage("Model belum tersedia; anchor Lodestone tetap aktif");return n.id();
    }
    public void adminRename(Player p,UUID id,String name)throws IOException{
        requireAdmin(p);if(getNode(id)==null)throw new IllegalArgumentException("Waystone sudah tidak ada");String label=NodeNames.clean(name);NodeNames.requireUnique(names,id,label);Map<UUID,String> labels=new LinkedHashMap<>(names);labels.put(id,label);persist(new LinkedHashMap<>(nodes),labels);
    }
    public UUID resolveName(String label){
        try{UUID id=UUID.fromString(label);if(getNode(id)!=null)return id;}catch(IllegalArgumentException ignored){}
        String key=NodeNames.key(label);return names.entrySet().stream().filter(e->NodeNames.key(e.getValue()).equals(key)).map(Map.Entry::getKey).findFirst().orElseThrow(()->new IllegalArgumentException("Nama/UUID Waystone tidak ditemukan"));
    }
    public void adminMove(Player p,UUID id,Block b)throws IOException{
        requireAdmin(p);Node old=getNode(id);if(old==null)throw new IllegalArgumentException("Waystone tidak ditemukan");
        if(b==null||b.getWorld()!=p.getWorld()||p.getTargetBlockExact(8)==null||!p.getTargetBlockExact(8).equals(b)||b.getType()!=Material.LODESTONE||b.getRelative(0,1,0).getType()!=Material.AIR||byBlock.containsKey(key(b))||legacyCollision(b.getLocation()))throw new IllegalArgumentException("Lihat Lodestone baru dengan AIR di atasnya");
        Location oldAt=location(old);if(oldAt==null||!oldAt.getWorld().isChunkLoaded(old.x()>>4,old.z()>>4))throw new IllegalArgumentException("Load chunk asal sebelum memindah Waystone");
        Node moved=new Node(old.id(),old.role(),old.target(),b.getWorld().getUID(),b.getWorld().getName(),b.getX(),b.getY(),b.getZ());Map<UUID,Node> next=new LinkedHashMap<>(nodes);next.put(id,moved);persist(next);visuals.remove(old,true);visuals.materialize(moved);
    }
    public void adminBind(Player p,UUID id,String target)throws IOException{
        requireAdmin(p);Node old=getNode(id);if(old==null||old.role()!=Node.Role.RETURN)throw new IllegalArgumentException("Pilih RETURN yang sah");
        if(nodes.values().stream().anyMatch(n->!n.id().equals(id)&&n.role()==Node.Role.RETURN&&n.target().equals(target)))throw new IllegalArgumentException("Arena sudah punya RETURN");
        Node nextNode=new Node(old.id(),old.role(),target,old.worldId(),old.worldName(),old.x(),old.y(),old.z());Map<UUID,Node> next=new LinkedHashMap<>(nodes);next.put(id,nextNode);persist(next);
    }
    public void adminExport(Player p)throws IOException{requireAdmin(p);writeExport(p.getLocation());}
    public void adminSyncExport(Player p)throws IOException{
        requireAdmin(p);File file=new File(getDataFolder(),"waystone-export.yml");if(!file.isFile())throw new IOException("Atur titik pulang HUB dahulu");
        var y=YamlConfiguration.loadConfiguration(file);World world=Bukkit.getWorld(y.getString("hub.world",""));
        if(world==null)throw new IOException("World HUB belum tersedia");writeExport(new Location(world,y.getDouble("hub.x"),y.getDouble("hub.y"),y.getDouble("hub.z")));
    }
    private String pluginStatus(String name){Plugin p=Bukkit.getPluginManager().getPlugin(name);return p==null?"missing":p.getDescription().getVersion()+"/enabled="+p.isEnabled();}
    private void writeExport(Location destination)throws IOException{
        Node hub=nodes.values().stream().filter(n->n.role()==Node.Role.HUB).findFirst().orElseThrow(()->new IOException("register HUB first"));
        Location at=location(hub);
        if(at==null||destination.getWorld()!=at.getWorld()||destination.distanceSquared(at)>36||!destination.getBlock().isPassable()||!destination.clone().add(0,1,0).getBlock().isPassable())throw new IOException("Stand on a safe return point within 6 blocks of HUB; do not stand inside the Waystone");
        YamlConfiguration y=new YamlConfiguration();y.set("schema-version",2);y.set("enabled",false);y.set("gateway-id",hub.id().toString());
        y.set("hub.world",destination.getWorld().getName());y.set("hub.x",destination.getX());y.set("hub.y",destination.getY());y.set("hub.z",destination.getZ());
        y.set("interaction-radius",Math.max(2,Math.min(16,getConfig().getDouble("interaction-radius",6))));
        for(Node n:nodes.values())if(n.role()==Node.Role.RETURN)y.set("return-waystones."+n.id(),n.target());
        y.save(new File(getDataFolder(),"waystone-export.yml"));
    }
    @Override public List<String> onTabComplete(CommandSender sender,Command command,String alias,String[] args){return args.length==1?List.of("hub","return","remove","list","status","inspect","export","check","refresh","move","model","rename","gui","sethub").stream().filter(a->a.startsWith(args[0].toLowerCase(Locale.ROOT))).toList():List.of();}
    @EventHandler public void onChunkLoad(org.bukkit.event.world.ChunkLoadEvent e) {
        for (Node n : getNodes()) if (n.worldId().equals(e.getWorld().getUID()) && (n.x() >> 4) == e.getChunk().getX() && (n.z() >> 4) == e.getChunk().getZ()) {
            getServer().getScheduler().runTask(this, () -> { if (ready && visuals != null) visuals.materialize(n); });
        }
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void onChunkUnload(org.bukkit.event.world.ChunkUnloadEvent e){
        if(visuals==null)return;
        for(Node n:getNodes())if(n.worldId().equals(e.getWorld().getUID())&&(n.x()>>4)==e.getChunk().getX()&&(n.z()>>4)==e.getChunk().getZ())visuals.remove(n,true);
    }
    @EventHandler public void onProviderDisabled(org.bukkit.event.server.PluginDisableEvent e){
        if(e.getPlugin().getName().equals("ItemsAdder")&&visuals!=null)visuals.removeAll(true);
    }
    @EventHandler public void onProviderEnabled(org.bukkit.event.server.PluginEnableEvent e) {
        if (e.getPlugin().getName().equals("ItemsAdder")) getServer().getScheduler().runTaskLater(this, () -> { if(ready && visuals != null) visuals.refreshLoaded(); }, 80L);
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void blockBreak(BlockBreakEvent e){if(getNode(e.getBlock())!=null){e.setCancelled(true);e.getPlayer().sendMessage("§cDungeon Waystone is protected. Use /adw remove first.");}}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void entityExplode(EntityExplodeEvent e){e.blockList().removeIf(b->getNode(b)!=null);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void blockExplode(BlockExplodeEvent e){e.blockList().removeIf(b->getNode(b)!=null);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void pistonExtend(BlockPistonExtendEvent e){if(e.getBlocks().stream().anyMatch(b->getNode(b)!=null))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void pistonRetract(BlockPistonRetractEvent e){if(e.getBlocks().stream().anyMatch(b->getNode(b)!=null))e.setCancelled(true);}
}
