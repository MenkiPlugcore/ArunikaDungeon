package site.cadera.arunika.dungeon.paper;
import site.cadera.arunika.dungeon.core.RpgPolicy;
import site.cadera.arunika.dungeon.core.DungeonRpgApi;
import site.cadera.arunika.dungeon.core.SessionRegistry;

import java.util.*;
import org.bukkit.*;
import org.bukkit.boss.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import site.cadera.arunika.dungeon.battle.*;
import static site.cadera.arunika.dungeon.battle.BattlePlan.*;

/** Opt-in staging adapter. Direct spawned mobs and projectiles only; custom Mythic summons need later integration. */
final class PaperBattlePort implements BattleEngine.Port, Listener {
 private final JavaPlugin plugin; private final MythicBridge mythic; private final NamespacedKey tag;
 private final Map<UUID,Entity> tracked=new HashMap<>(); private final Map<UUID,LivingEntity> bossMobs=new HashMap<>(); private final Map<UUID,UUID> projectiles=new HashMap<>();
 private final Map<UUID,BossBar> bars=new HashMap<>(); private final Map<UUID,Point> returns=new HashMap<>(); private final Map<UUID,Long> lastUpdate=new HashMap<>();private final Map<UUID,BattleEngine.Phase> lastPhase=new HashMap<>();
 private BattleEngine engine;
 private Map<String,List<Arena>> protectedArenas=Map.of(); private Point safeHub;
 private final Map<UUID,Point> teleportPermits=new HashMap<>();
 void protect(Collection<BattlePlan> plans,Point hub){safeHub=hub;Map<String,List<Arena>> world=new HashMap<>();for(var plan:plans)for(var a:plan.arenas())world.computeIfAbsent(a.bounds().min().world(),k->new ArrayList<>()).add(a);protectedArenas=Map.copyOf(world);for(Player p:Bukkit.getOnlinePlayers())recoverStranded(p);}
 private void recoverStranded(Player p){if(engine.participant(p.getUniqueId()).isPresent())return;Point at=point(p.getLocation());for(Arena a:protectedArenas.getOrDefault(at.world(),List.of()))if(a.bounds().contains(at)){returnPlayer(p,safeHub==null?a.exit():safeHub);return;}}
 private boolean configured(Point p){for(Arena a:protectedArenas.getOrDefault(p.world(),List.of()))if(a.bounds().contains(p))return true;return false;}
 boolean authorizedTeleport(Player p,Location dest){if(!safeDestination(dest))return false;UUID id=p.getUniqueId();if(teleportPermits.putIfAbsent(id,point(dest))!=null)return false;try{return p.teleport(dest)&&point(p.getLocation()).equals(point(dest));}finally{teleportPermits.remove(id);}}
 static boolean safeDestination(Location dest){
  if(dest==null||dest.getWorld()==null||!loaded(dest)||!dest.getWorld().getWorldBorder().isInside(dest))return false;
  int y=dest.getBlockY();if(y<dest.getWorld().getMinHeight()||y+1>=dest.getWorld().getMaxHeight())return false;
  var head=dest.clone().add(0,1,0).getBlock();var feet=dest.getBlock();
  return feet.isPassable()&&head.isPassable()&&!Set.of(Material.LAVA,Material.FIRE,Material.SOUL_FIRE,Material.POWDER_SNOW).contains(feet.getType())&&!Set.of(Material.LAVA,Material.FIRE,Material.SOUL_FIRE,Material.POWDER_SNOW).contains(head.getType());
 }
 boolean returnEligible(Player p,String floor,String arena,Location stone){if(stone==null||stone.getWorld()==null||!p.isOnline()||p.isDead()||p.isFlying()||p.getGameMode()==GameMode.CREATIVE||p.getGameMode()==GameMode.SPECTATOR)return false;var id=engine.participant(p.getUniqueId());if(id.isEmpty()||!engine.eligible(p.getUniqueId(),id.get()))return false;var a=engine.arena(id.get()).orElse(null);if(a==null||!a.id().equals(arena)||!engine.views(System.nanoTime()/1_000_000).stream().anyMatch(v->v.id().equals(id.get())&&v.floor().equals(floor))||!ArenaAccessPolicy.returnMatches(floor+"/"+arena,floor,a.id(),a.bounds(),point(stone),point(p.getLocation()),engine.eligible(p.getUniqueId(),id.get()))||!ArenaAccessPolicy.near(point(stone.clone().add(.5,.5,.5)),point(p.getLocation()),Math.min(6.5,((ArunikaDungeonPlugin)plugin).adminSnapshot().gateway().radius())))return false;return true;}
 boolean returnFromArena(Player p,String floor,String arena,Location stone){if(!returnEligible(p,floor,arena,stone))return false;var a=engine.participant(p.getUniqueId()).flatMap(engine::arena).orElse(null);if(a==null)return false;Location exit=location(safeHub==null?a.exit():safeHub);if(!loaded(exit)||!authorizedTeleport(p,exit))return false;engine.eliminate(p.getUniqueId());return true;}
 private RpgPolicy rpg=new RpgPolicy(false,0,false);
 private java.util.function.Predicate<UUID> beforeReward=id->true;
 void beforeReward(java.util.function.Predicate<UUID> hook){beforeReward=hook;}
 private final Map<UUID,Set<UUID>> contributors=new HashMap<>();
 void progression(RpgPolicy policy){rpg=policy;}
 private record PendingXp(DungeonRpgApi.BossClear event,DungeonRpgApi provider){UUID session(){return event.session();}UUID player(){return event.player();}}
 private final ArrayDeque<PendingXp> pendingXp=new ArrayDeque<>();
 @Override public void bossClear(SessionRegistry.Session s,Set<UUID> living){
  if(!rpg.active())return;
  var reg=Bukkit.getServicesManager().getRegistration(DungeonRpgApi.class);
  boolean provider=reg!=null&&reg.getPlugin().isEnabled()&&reg.getProvider().apiVersion()==1;
  if(!provider&&(reg!=null||!rpg.nativeClassXp()||rpg.classExpPerBoss()==0||nativeCommand()==null)){plugin.getLogger().warning("RPG provider unavailable; no bonus progression granted");return;}
  Set<UUID> active=contributors.getOrDefault(s.id(),Set.of());int skipped=0;
  for(UUID id:living){if(!active.contains(id)||!s.participants().contains(id))continue;Player p=Bukkit.getPlayer(id);if(p==null||!p.isOnline()||p.isDead()||!engine.arena(s.id()).map(a->present(id,a)).orElse(false))continue;
   if(pendingXp.size()<512){if(!beforeReward.test(s.id()))return;pendingXp.add(new PendingXp(new DungeonRpgApi.BossClear(s.id(),s.floor(),s.mode(),id,rpg.classExpPerBoss(),rpg.awakeningProgress()),provider?reg.getProvider():null));}else skipped++;
  }
  if(skipped>0)plugin.getLogger().warning("RPG progression queue full: "+skipped+" awards skipped for "+s.id());
 }
 /** Audited Nusa Karsa 1.2.10: its owned nkxp command checks class/permission/world and applies COMBAT XP modifiers. No arbitrary templates. */
 private org.bukkit.command.PluginCommand nativeCommand(){var nk=Bukkit.getPluginManager().getPlugin("ArunikaNusaKarsa");if(nk==null||!nk.isEnabled()||!nk.getDescription().getVersion().equals("1.2.10"))return null;var cmd=Bukkit.getPluginCommand("arunikanusakarsa:nkxp");if(cmd==null)cmd=Bukkit.getPluginCommand("nkxp");return cmd!=null&&cmd.getPlugin()==nk?cmd:null;}
 void drainProgression(){if(pendingXp.isEmpty())return;var job=pendingXp.poll();if(!engine.collecting(job.session())||!engine.eligible(job.player(),job.session()))return;Player p=Bukkit.getPlayer(job.player());if(p==null||!p.isOnline()||p.isDead()||!engine.arena(job.session()).map(a->present(job.player(),a)).orElse(false))return;try{if(job.provider()!=null){var reg=Bukkit.getServicesManager().getRegistration(DungeonRpgApi.class);if(reg==null||!reg.getPlugin().isEnabled()||reg.getProvider()!=job.provider()||job.provider().apiVersion()!=1)return;job.provider().bossClear(job.event());}else{var cmd=nativeCommand();if(cmd==null){plugin.getLogger().warning("Native Class XP unavailable; skipped "+job.session()+" / "+job.player());return;}cmd.execute(Bukkit.getConsoleSender(),"nkxp",new String[]{"grant",p.getName(),Integer.toString(job.event().classExp()),"COMBAT"});}}catch(RuntimeException|LinkageError e){plugin.getLogger().warning("Native Class XP dispatch failed; never replayed: "+job.session()+" / "+job.player());}}
 @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void contribution(EntityDamageByEntityEvent e){if(!rpg.active()||e.getFinalDamage()<=0)return;UUID run=engine.owner(e.getEntity().getUniqueId()).orElse(null);if(run==null||engine.collecting(run))return;Entity src=e.getDamager();if(src instanceof Projectile pr&&pr.getShooter() instanceof Entity shooter)src=shooter;if(src instanceof Player p&&engine.eligible(p.getUniqueId(),run))contributors.computeIfAbsent(run,k->new HashSet<>()).add(p.getUniqueId());}
 private PaperLoot loot; private Location deathLocation;
 void loot(PaperLoot loot){this.loot=loot;}
 @Override public int collectionSeconds(site.cadera.arunika.dungeon.core.SessionRegistry.Session s){return loot==null?0:loot.collection(s);}
 @Override public boolean drop(site.cadera.arunika.dungeon.core.SessionRegistry.Session s,Arena a,LootPlan.Trigger trigger,int wave,UUID mob){return loot!=null&&loot.emit(s,a,trigger,wave,mob==null?null:deathLocation); }
 private java.util.function.Consumer<BattleEngine.View> finishListener=v->{};
 void onFinish(java.util.function.Consumer<BattleEngine.View> listener){finishListener=listener;}
 PaperBattlePort(JavaPlugin plugin,MythicBridge mythic){this.plugin=plugin;this.mythic=mythic;tag=new NamespacedKey(plugin,"preview_session");}
 void attach(BattleEngine engine){this.engine=engine;}
 static Point point(Location l){return new Point(Objects.requireNonNull(l.getWorld()).getName(),l.getX(),l.getY(),l.getZ());}
 static Location location(Point p){World w=Bukkit.getWorld(p.world());if(w==null)throw new IllegalArgumentException("Missing world: "+p.world());return new Location(w,p.x(),p.y(),p.z());}
 static boolean loaded(Location l){return l.getWorld().isChunkLoaded(l.getBlockX()>>4,l.getBlockZ()>>4);}
 void validate(BattlePlan plan,Arena a){try{mythic.validate(plan.mobIds());}catch(ReflectiveOperationException e){throw new IllegalStateException("Mythic API unavailable",e);}var points=new ArrayList<>(a.mobSpawns());points.add(a.playerSpawn());points.add(a.bossSpawn());points.add(a.exit());for(Point p:points){Location loc=location(p);if(loc.getBlockY()<loc.getWorld().getMinHeight()||loc.getBlockY()>=loc.getWorld().getMaxHeight()||!loaded(loc))throw new IllegalArgumentException("World position/chunk unavailable: "+p);}}
 @Override public UUID spawn(UUID session,String mob,Point at,boolean boss,int participants)throws Exception{
  Location loc=location(at);if(!loaded(loc))throw new IllegalStateException("Spawn chunk not loaded");Entity e=mythic.spawn(mob,loc,participants);if(e==null)throw new IllegalStateException("Mythic spawn returned null");
  try{if(!(e instanceof LivingEntity)||!e.isValid()||e.isDead())throw new IllegalStateException("Invalid Mythic entity");e.getPersistentDataContainer().set(tag,PersistentDataType.STRING,session.toString());e.setPersistent(false);if(e instanceof org.bukkit.entity.Mob m)m.setRemoveWhenFarAway(false);tracked.put(e.getUniqueId(),e);if(boss)bossMobs.put(session,(LivingEntity)e);return e.getUniqueId();}catch(RuntimeException err){e.remove();throw err;}
 }
 @Override public boolean alive(UUID id){Entity e=tracked.get(id);if(e==null||!e.isValid()||e.isDead())return false;return engine.owner(id).flatMap(engine::arena).map(a->a.bounds().contains(point(e.getLocation()))).orElse(false);}
 @Override public void remove(UUID id){Entity e=tracked.remove(id);if(e!=null)e.remove();}
 @Override public boolean present(UUID id,Arena a){Player p=Bukkit.getPlayer(id);return p!=null&&p.isOnline()&&!p.isDead()&&p.getGameMode()!=GameMode.CREATIVE&&p.getGameMode()!=GameMode.SPECTATOR&&!p.isFlying()&&a.bounds().contains(point(p.getLocation()));}
 @Override public void update(BattleEngine.View v,Set<UUID> participants){long second=v.remainingSeconds();boolean sameSecond=Objects.equals(lastUpdate.put(v.id(),second),second);if(lastPhase.put(v.id(),v.phase())==v.phase()&&sameSecond)return;
  BossBar bar=bars.computeIfAbsent(v.id(),id->Bukkit.createBossBar("Dungeon Preview",BarColor.PURPLE,BarStyle.SOLID));
  String state=v.phase()==BattleEngine.Phase.WAVE?"Wave "+v.wave()+"/"+v.waveCount()+" | Mobs: "+(v.alive()+v.pending()):v.phase()==BattleEngine.Phase.LOOT?"LOOT FRENZY":v.phase().name();bar.setTitle(v.floor()+" | "+state+" | "+second+"s");
  LivingEntity boss=bossMobs.get(v.id()); double progress=v.phase()==BattleEngine.Phase.BOSS?(boss==null?1.0:boss.getHealth()/Math.max(1,boss.getMaxHealth())):(double)v.wave()/Math.max(1,v.waveCount());bar.setProgress(Math.max(0,Math.min(1,progress)));
  for(Player shown:bar.getPlayers())if(!shown.isOnline()||!engine.eligible(shown.getUniqueId(),v.id()))bar.removePlayer(shown);
  Set<Player> displayed=new HashSet<>(bar.getPlayers());for(UUID id:participants){Player p=Bukkit.getPlayer(id);if(p!=null&&engine.eligible(id,v.id())){if(!displayed.contains(p))bar.addPlayer(p);p.sendActionBar(net.kyori.adventure.text.Component.text(state+" | "+second+"s"));}else if(p!=null)bar.removePlayer(p);}
 }
 @Override public void finish(BattleEngine.View v,Set<UUID> participants,Arena a){try{contributors.remove(v.id());long discarded=pendingXp.stream().filter(j->j.session().equals(v.id())).count();pendingXp.removeIf(j->j.session().equals(v.id()));if(discarded>0)plugin.getLogger().warning("Native Class XP expired with arena: "+discarded+" / "+v.id());if(loot!=null)loot.cleanup(v.id());bossMobs.remove(v.id());BossBar b=bars.remove(v.id());if(b!=null)b.removeAll();lastUpdate.remove(v.id());lastPhase.remove(v.id());for(var e:new ArrayList<>(projectiles.entrySet()))if(e.getValue().equals(v.id())){Entity projectile=Bukkit.getEntity(e.getKey());if(projectile!=null)projectile.remove();projectiles.remove(e.getKey());}
  for(UUID id:participants){Player p=Bukkit.getPlayer(id);if(p!=null)p.sendMessage("[Dungeon Preview] "+v.reason()+" (staging)");if(p==null||p.isDead()){returns.put(id,a.exit());continue;}if(a.bounds().contains(point(p.getLocation())))returnPlayer(p,a.exit());}
  }finally{finishListener.accept(v);}
 }
 private void returnPlayer(Player p,Point exit){try{Location l=location(exit);if(loaded(l)&&authorizedTeleport(p,l)){returns.remove(p.getUniqueId());return;}}catch(RuntimeException ignored){}returns.put(p.getUniqueId(),exit);p.sendMessage("Dungeon exit unavailable; inform staff. /adg preview exit");}
 boolean hasPendingReturn(UUID id){return returns.containsKey(id);}
 void returnStagingPlayer(Player p,Point exit){returnPlayer(p,exit);}
 boolean hasPendingReturns(){return !returns.isEmpty();}
 boolean retryExit(Player p){Point exit=returns.get(p.getUniqueId());if(exit==null)return false;returnPlayer(p,exit);return true;}
 private UUID owner(Entity e){if(e instanceof Player)return engine.participant(e.getUniqueId()).orElse(null);UUID direct=engine.owner(e.getUniqueId()).orElse(null);if(direct!=null)return direct;if(e instanceof Projectile)return projectiles.get(e.getUniqueId());return null;}
 private boolean restricted(Location l){Point p=point(l);if(configured(p))return true;if(engine.idle())return false;return engine.containsActive(p);}
 private boolean permitted(Player p,Location l){Point dest=point(l);return engine.permits(p.getUniqueId(),dest,configured(dest));}
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void move(PlayerMoveEvent e){if(e instanceof PlayerTeleportEvent)return;if(e.getTo()!=null&&(e.getFrom().getX()!=e.getTo().getX()||e.getFrom().getY()!=e.getTo().getY()||e.getFrom().getZ()!=e.getTo().getZ())&&(!engine.idle()||!protectedArenas.isEmpty())&&!permitted(e.getPlayer(),e.getTo()))e.setCancelled(true);}
 /** The ONLY bypass is an exact, one-shot server-issued permit for a dungeon teleport.
  *  External teleport must stay within the living participant's arena, and
  *  /tp may not reposition a participant even when its target is inside. */
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void teleport(PlayerTeleportEvent e){
  if(e.getTo()==null||e.getFrom()==null){e.setCancelled(true);return;}
  UUID playerId=e.getPlayer().getUniqueId();
  Point permit=teleportPermits.get(playerId);
  if(ArenaAccessPolicy.permitMatches(permit,point(e.getTo()))&&teleportPermits.remove(playerId,permit))return;
  var session=engine.participant(playerId);
  boolean living=session.isPresent()&&engine.eligible(playerId,session.get());
  var own=session.flatMap(engine::arena).orElse(null);
  boolean originInside=own!=null&&own.bounds().contains(point(e.getFrom()));
  boolean destinationInside=own!=null&&own.bounds().contains(point(e.getTo()));
  boolean permitted=site.cadera.arunika.dungeon.battle.TeleportSafety.allowExternal(
     permitted(e.getPlayer(),e.getTo()),living,originInside,destinationInside,
     e.getCause()==PlayerTeleportEvent.TeleportCause.COMMAND);
  if(!permitted)e.setCancelled(true);
 }
 /** A ridden vehicle may bypass PlayerMoveEvent on some implementations.
  * Block entering vehicles during active combat without affecting survival. */
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void vehicleEnter(VehicleEnterEvent e){
  if(e.getEntered() instanceof Player p){var id=engine.participant(p.getUniqueId());
   if(id.isPresent()&&engine.eligible(p.getUniqueId(),id.get()))e.setCancelled(true);}
 }
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void glide(EntityToggleGlideEvent e){
  if(e.isGliding()&&e.getEntity() instanceof Player p){var id=engine.participant(p.getUniqueId());
   if(id.isPresent()&&engine.eligible(p.getUniqueId(),id.get()))e.setCancelled(true);}
 }
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void breakBlock(BlockBreakEvent e){if(restricted(e.getBlock().getLocation()))e.setCancelled(true);}
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void placeBlock(BlockPlaceEvent e){if(restricted(e.getBlock().getLocation()))e.setCancelled(true);}
 @EventHandler(priority=EventPriority.LOWEST,ignoreCancelled=true) public void earlyDamage(EntityDamageByEntityEvent e){damage(e);}
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void damage(EntityDamageByEntityEvent e){UUID a=owner(e.getDamager()),b=owner(e.getEntity());if(a==null&&b==null)return;if(a==null||!a.equals(b)||engine.collecting(a)){e.setCancelled(true);return;}Entity source=e.getDamager();if(source instanceof Projectile pr&&pr.getShooter() instanceof Entity s)source=s;if(source instanceof Player p&&(!engine.eligible(p.getUniqueId(),a)||!engine.arena(a).map(ar->present(p.getUniqueId(),ar)).orElse(false))||e.getEntity() instanceof Player p2&&!engine.eligible(p2.getUniqueId(),a)||source instanceof Player&&e.getEntity() instanceof Player)e.setCancelled(true);}
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void launch(ProjectileLaunchEvent e){if(e.getEntity().getShooter() instanceof Entity source){UUID id=owner(source);if(id!=null){if(projectiles.size()>=512){e.setCancelled(true);return;}projectiles.put(e.getEntity().getUniqueId(),id);}}}
 @EventHandler(priority=EventPriority.HIGHEST) public void death(EntityDeathEvent e){UUID id=e.getEntity().getUniqueId();if(engine.owner(id).isPresent()){e.getDrops().clear();e.setDroppedExp(0);tracked.remove(id);deathLocation=e.getEntity().getLocation();try{engine.death(id,System.nanoTime()/1_000_000);}finally{deathLocation=null;}}}
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void pickup(EntityPickupItemEvent e){if(loot!=null&&loot.tagged(e.getItem())&&!loot.canPickup(e.getItem(),e.getEntity(),engine))e.setCancelled(true);}
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void hopper(org.bukkit.event.inventory.InventoryPickupItemEvent e){if(loot!=null&&loot.tagged(e.getItem()))e.setCancelled(true);}
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void merge(ItemMergeEvent e){if(loot!=null&&(loot.tagged(e.getEntity())||loot.tagged(e.getTarget())))e.setCancelled(true);}
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void lootDamage(EntityDamageEvent e){if(loot!=null&&e.getEntity() instanceof Item i&&loot.tagged(i))e.setCancelled(true);}
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void fly(PlayerToggleFlightEvent e){if(e.isFlying()&&engine.participant(e.getPlayer().getUniqueId()).isPresent())e.setCancelled(true);}
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void mode(PlayerGameModeChangeEvent e){if(engine.participant(e.getPlayer().getUniqueId()).isPresent()&&(e.getNewGameMode()==GameMode.CREATIVE||e.getNewGameMode()==GameMode.SPECTATOR))e.setCancelled(true);}
 @EventHandler public void playerDeath(PlayerDeathEvent e){engine.eliminate(e.getEntity().getUniqueId());engine.participant(e.getEntity().getUniqueId()).flatMap(engine::arena).ifPresent(a->returns.put(e.getEntity().getUniqueId(),a.exit()));}
 @EventHandler public void quit(PlayerQuitEvent e){UUID id=e.getPlayer().getUniqueId();engine.participant(id).ifPresent(s->{BossBar bar=bars.get(s);if(bar!=null)bar.removePlayer(e.getPlayer());});engine.participant(id).flatMap(engine::arena).ifPresent(a->returns.put(id,a.exit()));engine.eliminate(id);}
 @EventHandler public void respawn(PlayerRespawnEvent e){Point p=returns.get(e.getPlayer().getUniqueId());if(p!=null){try{Location l=location(p);if(loaded(l)){e.setRespawnLocation(l);returns.remove(e.getPlayer().getUniqueId());}}catch(RuntimeException ignored){}}}
 @EventHandler public void join(PlayerJoinEvent e){Bukkit.getScheduler().runTask(plugin,()->{if(returns.containsKey(e.getPlayer().getUniqueId()))retryExit(e.getPlayer());else recoverStranded(e.getPlayer());});}
 @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void hit(ProjectileHitEvent e){if(projectiles.containsKey(e.getEntity().getUniqueId())){UUID id=e.getEntity().getUniqueId();Bukkit.getScheduler().runTask(plugin,()->{projectiles.remove(id);e.getEntity().remove();});}}
 int lootCount(){return loot==null?0:loot.count();}
 void maintenance(){if(loot!=null)loot.maintenance(engine);projectiles.keySet().removeIf(id->{Entity e=Bukkit.getEntity(id);return e==null||!e.isValid();});}
}
