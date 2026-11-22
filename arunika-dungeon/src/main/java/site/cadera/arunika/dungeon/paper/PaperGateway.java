package site.cadera.arunika.dungeon.paper;

import java.util.*;
import java.util.function.Consumer;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.command.CommandSender;
import site.cadera.arunika.dungeon.battle.ArenaAccessPolicy;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.plugin.Plugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import site.cadera.arunika.dungeon.config.SmartConfiguration.Gateway;
import site.cadera.arunika.dungeon.core.DungeonGatewayApi.*;

/** Native inventory UI works via normal Geyser inventory translation; no proprietary form dependency. */
final class PaperGateway implements Listener {
 private final ArunikaDungeonPlugin plugin; private final Gateway cfg; private final DungeonOnlyBridge bridge;
 private final Map<UUID,Menu> menus=new HashMap<>();
 private final GatewayRateLimiter airLimiter=new GatewayRateLimiter(350_000_000L,1024);
 private final List<UUID> boundStones;
 private Plugin floodgateProvider;
 private java.lang.reflect.Method floodgateMethod;
 private Object floodgateApi;
 private int refreshCursor;
 private static final int PAGE=GatewayMenuStyle.PAGE_SIZE;
 private record Status(List<SharedFloorStatus> shared,List<PrivatePartyStatus> parties,List<site.cadera.arunika.dungeon.core.SessionRegistry.Session> sessions,List<site.cadera.arunika.dungeon.battle.BattleEngine.View> battles){}
 private Status cached;
 private Status status(){return cached!=null?cached:new Status(plugin.sharedStatus(),plugin.privateStatus(),plugin.gatewaySessions(),plugin.gatewayBattles());}
 private enum Screen{HOME,FLOORS,SHARED,PRIVATE_DETAIL,PARTY,INVITE,RETURN}
 private static final class Menu implements InventoryHolder {
  final UUID owner,stone; final Screen screen; final boolean priv; final String floor,arena; final int page;
  boolean clickPending;final Map<Integer,Consumer<Player>> actions=new HashMap<>(); final BitSet written=new BitSet(54); final Inventory inventory;
  Menu(Player p,UUID stone,Screen screen,boolean priv,String floor,String arena,int page){owner=p.getUniqueId();this.stone=stone;this.screen=screen;this.priv=priv;this.floor=floor;this.arena=arena;this.page=page;inventory=Bukkit.createInventory(this,54,Component.text("ARUNIKA | "+switch(screen){case HOME->"Dungeon Hub";case FLOORS->priv?"Private Floors":"Shared Floors";case SHARED->"Shared Participants";case PRIVATE_DETAIL->"Private Participants";case PARTY->"Private Party";case INVITE->"Invite Players";case RETURN->"Return to Hub";}).color(NamedTextColor.LIGHT_PURPLE));}
  @Override public Inventory getInventory(){return inventory;}
 }
 PaperGateway(ArunikaDungeonPlugin plugin,Gateway cfg)throws ReflectiveOperationException{this.plugin=plugin;this.cfg=cfg;Plugin ws=Bukkit.getPluginManager().getPlugin("ArunikaDungeonWaystone");if(ws==null||!ws.isEnabled())throw new IllegalStateException("ArunikaDungeonWaystone missing");bridge=new DungeonOnlyBridge(ws);Object hubStone=bridge.stone(cfg.hubId());if(!bridge.isHub(hubStone))throw new IllegalStateException("HUB must be a dedicated DungeonWaystone node");var anchor=bridge.location(hubStone);if(anchor==null||anchor.getWorld()==null||!ArenaAccessPolicy.near(PaperBattlePort.point(anchor.clone().add(.5,.5,.5)),cfg.hub(),cfg.radius()))throw new IllegalStateException("Hub exit must be in the same world and within interaction radius of its registered Waystone");
 if(cfg.returns().containsKey(cfg.hubId()))throw new IllegalStateException("Hub UUID cannot be reused as a Return Waystone");
 List<UUID> ids=new ArrayList<>(1+cfg.returns().size());ids.add(cfg.hubId());ids.addAll(cfg.returns().keySet());boundStones=List.copyOf(ids);
 for(var e:cfg.returns().entrySet()){Object stone=bridge.stone(e.getKey());if(!bridge.isReturn(stone,e.getValue())||!plugin.gatewayStoneInArena(e.getValue(),bridge.location(stone)))throw new IllegalStateException("Invalid return stone: "+e.getKey());}}
 boolean atHub(Player p){try{if(!p.hasPermission("arunika.dungeon.gateway.test")||!bridge.allowed(p,bridge.stone(cfg.hubId()),cfg.radius(),true))return false;var hub=PaperBattlePort.location(cfg.hub());return p.getWorld().equals(hub.getWorld())&&p.getLocation().distanceSquared(hub)<=cfg.radius()*cfg.radius();}catch(RuntimeException|LinkageError e){return false;}}
 private boolean bound(UUID id){return cfg.hubId().equals(id)||cfg.returns().containsKey(id);}
 @EventHandler(priority=EventPriority.NORMAL,ignoreCancelled=true) public void block(PlayerInteractEvent e){if(e.getAction()!=Action.RIGHT_CLICK_BLOCK||e.getHand()!=EquipmentSlot.HAND||e.getClickedBlock()==null||!bridge.available())return;try{Object stone=bridge.stone(e.getClickedBlock());if(stone==null||!bound(bridge.id(stone)))return;e.setCancelled(true);scheduleRoute(e.getPlayer(),bridge.id(stone));}catch(RuntimeException|LinkageError err){e.getPlayer().sendMessage("Dungeon Waystone unavailable.");}}
 @EventHandler(priority=EventPriority.NORMAL,ignoreCancelled=true) public void plainEntity(PlayerInteractEntityEvent e){if(!(e instanceof PlayerInteractAtEntityEvent))entityClick(e);}
 @EventHandler(priority=EventPriority.NORMAL,ignoreCancelled=true) public void entity(PlayerInteractAtEntityEvent e){entityClick(e);}
 private void entityClick(PlayerInteractEntityEvent e){if(e.getHand()!=EquipmentSlot.HAND||!bridge.available())return;try{Object stone=bridge.stone(e.getRightClicked());if(stone==null||!bound(bridge.id(stone)))return;e.setCancelled(true);scheduleRoute(e.getPlayer(),bridge.id(stone));}catch(RuntimeException|LinkageError err){e.getPlayer().sendMessage("Dungeon Waystone unavailable.");}}
 /** Bedrock visual proxy is a marker ArmorStand (no normal hitbox). Only deliberate
  *  right-click-air gestures with empty hand may open a *registered* Dungeon Waystone.
  *  All real permission, anchor, session, and cooldown checks remain in route(). */
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=false)
 public void bedrockAir(PlayerInteractEvent e){
  if(e.getAction()!=Action.RIGHT_CLICK_AIR||e.getHand()!=EquipmentSlot.HAND)return;
  Player p=e.getPlayer();
  if(!plugin.gatewayUsable()||!bridge.available()||!p.isOnline()||p.isDead())return;
  if(!p.getInventory().getItemInMainHand().getType().isAir())return;
  if(p.getOpenInventory().getTopInventory().getType()!=InventoryType.CRAFTING)return;
  if(!isFloodgatePlayer(p))return;
  long now=System.nanoTime();if(!airLimiter.ready(p.getUniqueId(),now))return;
  Location eye=p.getEyeLocation();var dir=eye.getDirection().normalize();
  GatewayAim.Vec epos=new GatewayAim.Vec(eye.getX(),eye.getY(),eye.getZ());
  GatewayAim.Vec aim=new GatewayAim.Vec(dir.getX(),dir.getY(),dir.getZ());
  // UUID bindings are immutable for this gateway instance; avoid allocating on every tap.
  UUID chosen=null;double score=Double.POSITIVE_INFINITY;boolean ambiguous=false;
  for(UUID id:boundStones){
   if(id==null)continue;
   try{
    Object stone=bridge.stone(id);if(!(cfg.hubId().equals(id)?bridge.isHub(stone):bridge.isReturn(stone,cfg.returns().get(id))))continue;
    Location l=bridge.location(stone);if(l==null||l.getWorld()==null||!l.getWorld().equals(eye.getWorld()))continue;
    if(p.getLocation().distanceSquared(l.clone().add(.5,.5,.5))>cfg.radius()*cfg.radius())continue;
    GatewayAim.Hit hit=GatewayAim.match(epos,aim,new GatewayAim.Vec(l.getBlockX(),l.getBlockY(),l.getBlockZ()),Math.min(cfg.radius(),6.0),0.70);
    if(hit==null)continue;
    Block first=p.getTargetBlockExact(Math.min(8,(int)Math.ceil(hit.distance()+1.0)));
    if(first!=null){Object seen=bridge.stone(first);if(seen==null||!id.equals(bridge.id(seen)))continue;}
    if(hit.deviationSquared()<score-0.02){score=hit.deviationSquared();chosen=id;ambiguous=false;}
    else if(Math.abs(hit.deviationSquared()-score)<=0.02)ambiguous=true;
   }catch(RuntimeException|LinkageError ignored){ /* fail closed; do not hijack normal gameplay */ }
  }
  if(chosen==null||ambiguous)return;
  airLimiter.commit(p.getUniqueId(),now);UUID finalId=chosen;
  Bukkit.getScheduler().runTask(plugin,()->{
   if(!p.isOnline()||!plugin.gatewayUsable())return;
   try{Object stone=bridge.stone(finalId);if(bridge.dedicated(stone))route(p,stone);}
   catch(RuntimeException|LinkageError err){p.sendMessage("Dungeon Waystone temporarily unavailable.");}
  });
 }
 /** Cache the Floodgate ABI per enabled plugin instance, not per right-click.
  * Fail closed when absent, changed, unloaded or throwing. */
 private boolean isFloodgatePlayer(Player p){
  Plugin f=Bukkit.getPluginManager().getPlugin("floodgate");
  if(f==null||!f.isEnabled()){
   floodgateProvider=null;floodgateMethod=null;floodgateApi=null;return false;
  }
  if(floodgateProvider!=f){
   floodgateProvider=f;floodgateMethod=null;floodgateApi=null;
   try{
    Class<?> c=Class.forName("org.geysermc.floodgate.api.FloodgateApi",false,f.getClass().getClassLoader());
    Object api=c.getMethod("getInstance").invoke(null);
    java.lang.reflect.Method method=c.getMethod("isFloodgatePlayer",UUID.class);
    floodgateApi=api;floodgateMethod=method;
   }catch(ReflectiveOperationException|LinkageError ex){return false;}
  }
  if(floodgateMethod==null||floodgateApi==null)return false;
  try{return Boolean.TRUE.equals(floodgateMethod.invoke(floodgateApi,p.getUniqueId()));}
  catch(ReflectiveOperationException|LinkageError ex){
   floodgateMethod=null;floodgateApi=null;return false;
  }
 }

 void openTarget(Player p){try{Block block=p.getTargetBlockExact((int)Math.ceil(cfg.radius()));Object stone=block==null?null:bridge.stone(block);if(stone==null||!bound(bridge.id(stone)))throw new IllegalStateException("Look at the bound Waystone anchor");route(p,stone);}catch(RuntimeException|LinkageError e){p.sendMessage("Gateway unavailable: "+e.getMessage());}}
 private void scheduleRoute(Player p,UUID id){Bukkit.getScheduler().runTask(plugin,()->{if(p.isOnline())try{Object stone=bridge.stone(id);if(stone!=null)route(p,stone);}catch(RuntimeException|LinkageError ignored){}});}
 private void route(Player p,Object stone){UUID id=bridge.id(stone);Menu existing=menus.get(p.getUniqueId());if(existing!=null&&existing.stone.equals(id)&&p.getOpenInventory().getTopInventory()==existing.inventory&&valid(p,existing))return;if(!plugin.gatewayUsable()||!p.hasPermission("arunika.dungeon.gateway.test")||!(cfg.hubId().equals(id)?bridge.isHub(stone):bridge.isReturn(stone,cfg.returns().get(id)))||!bridge.allowed(p,stone,cfg.radius(),cfg.hubId().equals(id))){p.sendMessage("Dungeon gateway unavailable or access denied.");return;}if(cfg.hubId().equals(id)){if(!atHub(p)){p.sendMessage("Stand at the Dungeon Hub.");return;}open(p,id,Screen.HOME,false,"",0);}else if(plugin.gatewayReturnEligible(p,cfg.returns().get(id),bridge.location(stone)))open(p,id,Screen.RETURN,false,cfg.returns().get(id),0);else p.sendMessage("Return Waystone requires your own active arena session.");}
 static void check(ArunikaDungeonPlugin plugin,CommandSender sender){try{Plugin ws=Bukkit.getPluginManager().getPlugin("ArunikaDungeonWaystone");if(ws==null||!ws.isEnabled())throw new IllegalStateException("ArunikaDungeonWaystone not enabled");DungeonOnlyBridge b=new DungeonOnlyBridge(ws);sender.sendMessage("DungeonWaystone API: OK | "+ws.getDescription().getVersion());Plugin bed=Bukkit.getPluginManager().getPlugin("ArunikaDungeonWaystoneBedrock");sender.sendMessage("Bedrock renderer: "+(bed==null?"missing":bed.getDescription().getVersion()+" / enabled="+bed.isEnabled())+" | native anchor + target fallback; proxy clicks require client test");var cfg=plugin.adminSnapshot().gateway();sender.sendMessage("Gateway config enabled="+cfg.enabled()+" | hub UUID="+cfg.hubId()+" | return bindings="+cfg.returns().size());if(cfg.hubId()!=null){Object s=b.stone(cfg.hubId());sender.sendMessage("Hub dedicated="+b.isHub(s)+(s==null?"":" | anchor="+b.location(s)));}for(var e:cfg.returns().entrySet()){Object s=b.stone(e.getKey());sender.sendMessage(e.getValue()+": "+e.getKey()+" | valid="+(b.isReturn(s,e.getValue())&&plugin.gatewayStoneInArena(e.getValue(),b.location(s))));}}catch(Exception|LinkageError e){sender.sendMessage("Waystone diagnostic failed: "+e);}}
 static void inspect(ArunikaDungeonPlugin plugin,Player p){try{Plugin ws=Bukkit.getPluginManager().getPlugin("ArunikaDungeonWaystone");if(ws==null||!ws.isEnabled())throw new IllegalStateException("ArunikaDungeonWaystone missing");DungeonOnlyBridge b=new DungeonOnlyBridge(ws);Block block=p.getTargetBlockExact(8);Object s=block==null?null:b.stone(block);if(s==null)throw new IllegalStateException("Look at a registered Waystone anchor block");p.sendMessage("DungeonWaystone UUID: "+b.id(s)+" | dedicated: "+b.dedicated(s)+" | HUB: "+b.isHub(s)+" | "+b.location(s));}catch(Exception|LinkageError e){p.sendMessage("Waystone inspect: "+e.getMessage());}}
 void close(){HandlerList.unregisterAll(this);for(var m:new ArrayList<>(menus.values())){Player p=Bukkit.getPlayer(m.owner);if(p!=null&&p.getOpenInventory().getTopInventory()==m.inventory)p.closeInventory();}menus.clear();airLimiter.clear();floodgateProvider=null;floodgateMethod=null;floodgateApi=null;refreshCursor=0;cached=null;}
 private boolean valid(Player p,Menu m){try{return plugin.gatewayUsable()&&p.getUniqueId().equals(m.owner)&&p.hasPermission("arunika.dungeon.gateway.test")&&bound(m.stone)&&(m.screen==Screen.RETURN?bridge.isReturn(bridge.stone(m.stone),cfg.returns().get(m.stone))&&bridge.allowed(p,bridge.stone(m.stone),cfg.radius(),false)&&plugin.gatewayReturnEligible(p,cfg.returns().get(m.stone),bridge.location(bridge.stone(m.stone))):bridge.isHub(bridge.stone(cfg.hubId()))&&atHub(p));}catch(RuntimeException|LinkageError e){return false;}}
 private void open(Player p,UUID id,Screen s,boolean priv,String floor,int page){open(p,id,s,priv,floor,"",page);}
 private void open(Player p,UUID id,Screen s,boolean priv,String floor,String arena,int page){Menu m=new Menu(p,id,s,priv,floor,arena,Math.max(0,page));if(!valid(p,m))return;render(p,m);p.openInventory(m.inventory);menus.put(p.getUniqueId(),m);}
 private void go(Player p,Menu m,Screen s,boolean priv,String floor,int page){open(p,m.stone,s,priv,floor,m.arena,page);}
 private void go(Player p,Menu m,Screen s,boolean priv,String floor,String arena,int page){open(p,m.stone,s,priv,floor,arena,page);}
 private void button(Menu m,int slot,Material mat,String name,List<String> lore,Consumer<Player> action){
  if(slot<0||slot>=GatewayMenuStyle.MENU_SIZE)throw new IllegalArgumentException("GUI slot out of range: "+slot);
  ItemStack item=new ItemStack(mat);
  var meta=item.getItemMeta();
  meta.displayName(GuiTheme.name(GatewayMenuStyle.compact(name,44),mat));
  meta.lore(GatewayMenuStyle.lore(lore).stream().map(GuiTheme::lore).toList());
  item.setItemMeta(meta);
  m.written.set(slot);
  if(!item.equals(m.inventory.getItem(slot)))m.inventory.setItem(slot,item);
  if(action!=null)m.actions.put(slot,action);
}
 private void button(Menu m,int slot,Material mat,String name,Consumer<Player> action){button(m,slot,mat,name,List.of(),action);}
 private String name(UUID id){Player p=Bukkit.getPlayer(id);return p==null?"Offline #"+id.toString().substring(0,8):p.getName();}
 private List<String> names(Collection<UUID> ids){List<String> n=ids.stream().limit(5).map(this::name).toList();var out=new ArrayList<>(n);if(ids.size()>5)out.add("+"+(ids.size()-5)+" more");return out;}
 private Optional<PrivatePartyStatus> party(Player p){return status().parties().stream().filter(v->v.members().contains(p.getUniqueId())).findFirst();}
 private void pageButtons(Menu m,int size){
  int last=GatewayMenuStyle.pages(size)-1;
  button(m,46,Material.PAPER,GatewayMenuStyle.pageLabel(m.page,size),List.of("Entries: "+size),null);
  if(m.page>0)button(m,45,Material.ARROW,"Previous page",p->go(p,m,m.screen,m.priv,m.floor,Math.min(m.page-1,last)));
  if(m.page<last)button(m,53,Material.ARROW,"Next page",p->go(p,m,m.screen,m.priv,m.floor,m.page+1));
  if(m.page>last)button(m,22,Material.PAPER,"List changed",List.of("Choose Previous page to see items"),null);
}
 private void chrome(Player p,Menu m){
  if(m.screen==Screen.HOME||m.screen==Screen.RETURN){
   for(int slot=0;slot<9;slot++)if(!m.written.get(slot))button(m,slot,Material.PURPLE_STAINED_GLASS_PANE," ",null);
  }
  if(!m.written.get(36))button(m,36,Material.NETHER_STAR,"ARUNIKA DUNGEON",List.of("Gateway: "+switch(m.screen){
   case HOME->"Choose dungeon mode";case FLOORS->m.priv?"Private floors":"Shared floors";
   case SHARED->"Shared dungeon";case PRIVATE_DETAIL->"Solo or friends";case PARTY->"Private party";
   case INVITE->"Invite online players";case RETURN->"Return Waystone";}),null);
  if(!m.written.get(44))button(m,44,Material.AMETHYST_SHARD,"Dungeon Gateway",List.of("Native inventory interface","No new GUI textures required"),null);
  if(!m.written.get(47))button(m,47,Material.BOOK,"Quick guide",switch(m.screen){
   case HOME->List.of("Click a mode, then choose boss","Private: leader pays one ticket");
   case FLOORS->List.of("Select arena and boss","Only boss cooldown is shown");
   case SHARED->List.of("Join queue to enter Shared","Leave queue before battle starts");
   case PRIVATE_DETAIL->List.of("Solo: start without invite","Friends: only ready + same world join");
   case PARTY->List.of("Leader can start without all Ready","Only same-world Ready friends join");
   case INVITE->List.of("Invite friends from any world","Other-world friends can Ready, but cannot join");
   case RETURN->List.of("Return leaves your battle","No refund for voluntary exit");},null);
  if(m.screen!=Screen.RETURN&&!m.written.get(51))button(m,51,Material.CLOCK,"Refresh information",List.of("Update queue and party information"),q->go(q,m,m.screen,m.priv,m.floor,m.page));
  for(int slot=36;slot<45;slot++)if(!m.written.get(slot))button(m,slot,Material.BLACK_STAINED_GLASS_PANE," ",null);
  for(int slot=45;slot<54;slot++)if(!m.written.get(slot))button(m,slot,Material.GRAY_STAINED_GLASS_PANE," ",null);
 }
 private void render(Player p,Menu m){
  m.written.clear();m.actions.clear();
  switch(m.screen){
   case HOME->{
    button(m,20,Material.EMERALD,"Shared Dungeon",List.of("Free","Choose a boss arena"),q->go(q,m,Screen.FLOORS,false,"","",0));
    button(m,24,Material.AMETHYST_SHARD,"Private Dungeon",List.of("1 leader ticket","Solo or invite up to 4 friends"),q->go(q,m,Screen.FLOORS,true,"","",0));
   }
   case FLOORS->{
    var choices=plugin.gatewayArenas(m.priv);int start=m.page*PAGE;
    if(choices.isEmpty())button(m,22,Material.BARRIER,"No arenas configured",List.of("Ask admin to configure boss arena"),null);
    for(int i=start;i<Math.min(start+PAGE,choices.size());i++){
     var a=choices.get(i);
     boolean ready=a.available()&&a.cooldownSeconds()==0;
     List<String> info=List.of("CD: "+(a.cooldownSeconds()>0?a.cooldownSeconds()+"s":"Ready"));
     button(m,i-start,ready?(m.priv?Material.AMETHYST_SHARD:Material.EMERALD):Material.GRAY_DYE,
      a.name()+" / "+a.boss(),info,ready?q->{
       if(m.priv)go(q,m,Screen.PRIVATE_DETAIL,true,a.floor(),a.arena(),0);
       else{plugin.gatewayAction(q,false,"join",a.floor(),a.arena());q.closeInventory();}
      }:null);
    }
    pageButtons(m,choices.size());button(m,49,Material.COMPASS,"Back",q->go(q,m,Screen.HOME,false,"","",0));
   }
   case PRIVATE_DETAIL->{
    button(m,20,Material.ENDER_PEARL,"Play Solo",List.of("Boss arena: "+m.arena,"Leader uses 1 ticket"),q->{
     if(party(q).isPresent()){go(q,m,Screen.PARTY,true,m.floor,m.arena,0);return;}
     plugin.gatewayAction(q,true,"create",m.floor,m.arena);
     if(party(q).isPresent())plugin.gatewayAction(q,true,"start");
     q.closeInventory();
    });
    button(m,24,Material.PLAYER_HEAD,"Invite Friends",List.of("Same world + Ready to join","Leader can start alone"),q->{
     if(party(q).isEmpty())plugin.gatewayAction(q,true,"create",m.floor,m.arena);
     go(q,m,Screen.PARTY,true,m.floor,m.arena,0);
    });
    button(m,49,Material.COMPASS,"Back",q->go(q,m,Screen.FLOORS,true,"","",0));
   }
   case PARTY->{
    var opt=party(p);
    if(opt.isEmpty())button(m,22,Material.PAPER,"No private party",q->go(q,m,Screen.FLOORS,true,"","",0));
    else{
     var v=opt.get();boolean leader=v.leader().equals(p.getUniqueId());
     int i=10;for(UUID id:v.members()){
      if(i>=17)break;
      Player member=Bukkit.getPlayer(id);
      boolean sameWorld=member!=null&&member.getWorld().equals(p.getWorld());
      button(m,i++,Material.PLAYER_HEAD,name(id),List.of(id.equals(v.leader())?"Leader":"Friend",v.ready().contains(id)?"Ready":"Not ready",sameWorld?"Same world":"Different world"),null);
     }
     button(m,22,Material.PAPER,"Arena: "+plugin.privateArena(p.getUniqueId()),List.of("Leader pays 1 Universal Ticket","Only ready friends in the leader world join"),null);
     if(!leader){
      button(m,28,Material.LIME_DYE,"Ready",q->{plugin.gatewayAction(q,true,"ready");go(q,m,Screen.PARTY,true,v.floor(),0);});
      button(m,29,Material.YELLOW_DYE,"Not Ready",q->{plugin.gatewayAction(q,true,"unready");go(q,m,Screen.PARTY,true,v.floor(),0);});
     }else{
      button(m,30,Material.PLAYER_HEAD,"Invite Friend",q->go(q,m,Screen.INVITE,true,v.floor(),0));
      button(m,32,Material.EMERALD,"START DUNGEON",List.of("Start now; skip unready or different-world friends","Uses ONLY your ticket"),q->{plugin.gatewayAction(q,true,"start");q.closeInventory();});
      button(m,33,Material.RED_DYE,"Cancel Start",q->{plugin.gatewayAction(q,true,"cancel");go(q,m,Screen.PARTY,true,v.floor(),0);});
     }
     button(m,34,Material.BARRIER,"Leave Party",q->{plugin.gatewayAction(q,true,"leave");go(q,m,Screen.HOME,false,"","",0);});
    }
    button(m,49,Material.COMPASS,"Back",q->go(q,m,Screen.HOME,false,"","",0));
   }
   case INVITE->{
    var candidates=Bukkit.getOnlinePlayers().stream().filter(q->!q.equals(p)&&q.hasPermission("arunika.dungeon.private.test")).sorted(Comparator.comparing(Player::getName)).toList();
    for(int i=m.page*PAGE;i<Math.min((m.page+1)*PAGE,candidates.size());i++){
     Player friend=candidates.get(i);UUID id=friend.getUniqueId();boolean same=friend.getWorld().equals(p.getWorld());
     button(m,i-m.page*PAGE,Material.PLAYER_HEAD,friend.getName(),List.of(same?"Same world":"Different world: invite allowed","Use /adg private accept, then ready"),q->{Player target=Bukkit.getPlayer(id);if(target!=null)plugin.gatewayAction(q,true,"invite",target.getName());go(q,m,Screen.PARTY,true,m.floor,0);});
    }
    if(candidates.isEmpty())button(m,22,Material.PAPER,"No friends online",null);
    pageButtons(m,candidates.size());button(m,49,Material.COMPASS,"Back",q->go(q,m,Screen.PARTY,true,m.floor,0));
   }
   case SHARED->{button(m,22,Material.EMERALD,"Join Shared",q->{plugin.gatewayAction(q,false,"join",m.floor);q.closeInventory();});button(m,49,Material.COMPASS,"Back",q->go(q,m,Screen.FLOORS,false,"","",0));}
   case RETURN->{button(m,22,Material.ENDER_PEARL,"Return to /warp dungeon",List.of("Return to configured Dungeon Hub","Voluntary exit: no ticket refund"),q->{String[] a=m.floor.split("/",2);if(a.length==2&&plugin.gatewayReturn(q,a[0],a[1],bridge.location(bridge.stone(m.stone))))q.closeInventory();else q.sendMessage("Return denied: you are not a participant of this arena, or hub is unavailable.");});button(m,31,Material.BARRIER,"Cancel",Player::closeInventory);}
  }
  chrome(p,m);
  for(int slot=0;slot<54;slot++)if(!m.written.get(slot)&&m.inventory.getItem(slot)!=null)m.inventory.clear(slot);
 }
 @EventHandler(priority=EventPriority.HIGHEST) public void click(InventoryClickEvent e){if(!(e.getView().getTopInventory().getHolder() instanceof Menu m))return;boolean wasCancelled=e.isCancelled();e.setCancelled(true);if(wasCancelled)return;if(!(e.getWhoClicked() instanceof Player p)||menus.get(p.getUniqueId())!=m||!valid(p,m)){e.getWhoClicked().closeInventory();return;}if(e.getRawSlot()<0||e.getRawSlot()>=54||!(e.getClick()==ClickType.LEFT||e.getClick()==ClickType.RIGHT))return;Consumer<Player> action=m.actions.get(e.getRawSlot());if(action==null||m.clickPending)return;m.clickPending=true;Bukkit.getScheduler().runTask(plugin,()->{m.clickPending=false;if(menus.get(p.getUniqueId())==m&&p.getOpenInventory().getTopInventory()==m.inventory&&valid(p,m))try{action.accept(p);}catch(RuntimeException ex){p.sendMessage("Dungeon action rejected: "+ex.getMessage());}});}
 @EventHandler(priority=EventPriority.HIGHEST) public void drag(InventoryDragEvent e){if(e.getView().getTopInventory().getHolder() instanceof Menu)e.setCancelled(true);}
 @EventHandler public void closed(InventoryCloseEvent e){if(e.getInventory().getHolder() instanceof Menu m)menus.remove(m.owner,m);}
 @EventHandler public void quit(PlayerQuitEvent e){menus.remove(e.getPlayer().getUniqueId());airLimiter.remove(e.getPlayer().getUniqueId());}
 @EventHandler public void worldChanged(PlayerChangedWorldEvent e){
  Menu m=menus.remove(e.getPlayer().getUniqueId());
  if(m!=null&&e.getPlayer().getOpenInventory().getTopInventory()==m.inventory)e.getPlayer().closeInventory();
 }
 /** Work limit: at most 16 menus per maintenance call. Click actions still validate immediately. */
 void refresh(){
  if(menus.isEmpty()){refreshCursor=0;return;}
  List<Menu> snapshot=new ArrayList<>(menus.values());
  GatewayWorkBudget.Window work=GatewayWorkBudget.next(snapshot.size(),refreshCursor,16);
  refreshCursor=work.next();cached=status();
  try{
   for(int i=0;i<work.count();i++){
    Menu m=snapshot.get((work.start()+i)%snapshot.size());
    Player p=Bukkit.getPlayer(m.owner);
    if(p==null||p.getOpenInventory().getTopInventory()!=m.inventory){menus.remove(m.owner,m);continue;}
    if(!valid(p,m)){p.closeInventory();continue;}
    if(m.screen==Screen.FLOORS||m.screen==Screen.SHARED||m.screen==Screen.PRIVATE_DETAIL||m.screen==Screen.PARTY)
     try{render(p,m);}catch(RuntimeException e){p.closeInventory();}
   }
  }finally{cached=null;}
 }
}
