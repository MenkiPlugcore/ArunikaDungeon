package site.cadera.arunika.dungeon.battle;
import java.util.*;
import site.cadera.arunika.dungeon.core.SessionRegistry;
import static site.cadera.arunika.dungeon.battle.BattlePlan.*;

/** Main-thread state machine. No world scans and no per-mob scheduled tasks. */
public final class BattleEngine {
 public enum Phase { PREPARING, WAVE, BOSS, LOOT, COMPLETED, FAILED }
 public record View(UUID id,String floor,String arena,Phase phase,int wave,int waveCount,int pending,int alive,long remainingSeconds,String reason){}
 public interface Port {
  UUID spawn(UUID session,String mob,Point at,boolean boss,int participants) throws Exception;
  boolean alive(UUID mob); void remove(UUID mob); boolean present(UUID player,Arena arena);
  default void bossClear(SessionRegistry.Session session,Set<UUID> living){}
  default int collectionSeconds(SessionRegistry.Session s){return 0;}
  default boolean drop(SessionRegistry.Session s,Arena a,LootPlan.Trigger trigger,int wave,UUID mob){return false;}
  void update(View view,Set<UUID> participants); void finish(View view,Set<UUID> participants,Arena arena);
 }
 private record Owner(UUID session,boolean boss){}
 private static final class Run {
  final SessionRegistry.Session session;final BattlePlan plan;final Arena arena;final Set<UUID> living;final Set<UUID> mobs=new HashSet<>();final ArrayDeque<Mob> pending=new ArrayDeque<>();final long deadline;long ready;int wave=-1,point=0,pendingCount;Phase phase=Phase.PREPARING;String reason="";boolean bossSpawned,lootExposed,lootAllowed=true;long lootUntil;
  Run(SessionRegistry.Session s,BattlePlan p,Arena a,long now){session=s;plan=p;arena=a;living=new HashSet<>(s.participants());deadline=now+p.durationSeconds()*1000L;ready=now+3000;}
 }
 private final SessionRegistry registry;private final Port port;private final int budget,batch;private final Map<UUID,Run> runs=new LinkedHashMap<>();private final Map<UUID,Owner> owners=new HashMap<>();private int cursor;
 public BattleEngine(SessionRegistry registry,Port port,int budget,int batch){this.registry=Objects.requireNonNull(registry);this.port=Objects.requireNonNull(port);if(budget<1||batch<1||batch>budget)throw new IllegalArgumentException("spawn budget");this.budget=budget;this.batch=batch;}
 public UUID start(SessionRegistry.Session s,BattlePlan p,Arena a,long now){return start(s,p,a,now,true);}
 public UUID start(SessionRegistry.Session s,BattlePlan p,Arena a,long now,boolean lootAllowed){if(!s.floor().equals(p.floor())||a.mode()!=s.mode()||!p.arenas().contains(a)||runs.containsKey(s.id())||runs.values().stream().anyMatch(r->r.arena.bounds().overlaps(a.bounds())))throw new IllegalStateException("arena unavailable");if(s.participants().stream().anyMatch(u->!port.present(u,a)))throw new IllegalStateException("participant not in arena");registry.activate(s.id());Run r=new Run(s,p,a,now);r.lootAllowed=lootAllowed;runs.put(s.id(),r);return s.id();}
 public Optional<UUID> owner(UUID mob){return Optional.ofNullable(owners.get(mob)).map(Owner::session);}
 public Optional<UUID> participant(UUID player){return registry.participant(player).filter(s->runs.containsKey(s.id())).map(SessionRegistry.Session::id);}
 public boolean eligible(UUID player,UUID session){Run r=runs.get(session);return r!=null&&r.living.contains(player);}
 public Optional<Arena> arena(UUID session){return Optional.ofNullable(runs.get(session)).map(r->r.arena);}
 public List<View> views(long now){return runs.values().stream().map(r->view(r,now)).toList();}
 public Map<UUID,Arena> activeArenas(){Map<UUID,Arena> out=new LinkedHashMap<>();runs.forEach((id,r)->out.put(id,r.arena));return out;}
 public boolean permits(UUID player,Point dest,boolean configured){
  UUID own=participant(player).orElse(null);Run mine=own==null?null:runs.get(own);
  if(mine!=null&&mine.living.contains(player))return mine.arena.bounds().contains(dest);
  if(configured)return false;
  for(Run r:runs.values())if(r.arena.bounds().contains(dest))return false;
  return true;
 }
 public boolean containsActive(Point point){for(Run r:runs.values())if(r.arena.bounds().contains(point))return true;return false;}
 public boolean overlapsActive(Bounds bounds){for(Run r:runs.values())if(r.arena.bounds().overlaps(bounds))return true;return false;}
 public boolean idle(){return runs.isEmpty();}
 public int activeEntities(){return owners.size();}
 public void eliminate(UUID player){participant(player).ifPresent(id->runs.get(id).living.remove(player));}
 public void death(UUID mob,long now){Owner o=owners.remove(mob);if(o==null)return;Run r=runs.get(o.session);if(r==null)return;r.mobs.remove(mob);if(o.boss&&r.phase==Phase.BOSS){try{registry.bossDefeated(r.session.id());}catch(RuntimeException e){close(r,Phase.FAILED,e.getMessage(),now);return;}r.phase=Phase.LOOT;r.lootUntil=now+(r.lootAllowed?port.collectionSeconds(r.session):0)*1000L;emit(r,LootPlan.Trigger.BOSS_KILL,mob);if(r.lootAllowed)try{port.bossClear(r.session,Set.copyOf(r.living));}catch(RuntimeException|LinkageError ignored){}if(r.lootUntil<=now)close(r,Phase.COMPLETED,"Boss defeated",now);}else if(r.phase==Phase.WAVE)emit(r,LootPlan.Trigger.MOB_KILL,mob);}
 private void emit(Run r,LootPlan.Trigger trigger,UUID mob){if(!r.lootAllowed)return;try{r.lootExposed|=port.drop(r.session,r.arena,trigger,r.wave+1,mob);}catch(RuntimeException|LinkageError e){r.lootExposed=true;}}
 public boolean collecting(UUID session){Run r=runs.get(session);return r!=null&&r.phase==Phase.LOOT;}
 public void tick(long now,boolean allowSpawn){List<Run> order=new ArrayList<>(runs.values());if(order.isEmpty())return;int start=Math.floorMod(cursor++,order.size());int allowance=batch;for(int i=0;i<order.size();i++){Run r=order.get((start+i)%order.size());if(!runs.containsKey(r.session.id()))continue;try{
  if(r.phase==Phase.LOOT){r.living.removeIf(u->!port.present(u,r.arena));if(now>=r.lootUntil||r.living.isEmpty())close(r,Phase.COMPLETED,"Boss defeated",now);else port.update(view(r,now),r.session.participants());continue;}
  if(now>=r.deadline){close(r,Phase.FAILED,"Time expired",now);continue;}
  r.living.removeIf(u->!port.present(u,r.arena));if(r.living.isEmpty()){close(r,Phase.FAILED,"All participants eliminated or left",now);continue;}
  // disappearance is not a kill and must never grant completion or advance waves
  if(r.mobs.stream().anyMatch(u->!port.alive(u))){close(r,Phase.FAILED,"Tracked mob missing",now);continue;}
  if(r.phase==Phase.PREPARING&&now>=r.ready)next(r);
  if(r.phase==Phase.WAVE&&r.pending.isEmpty()&&r.mobs.isEmpty()){emit(r,LootPlan.Trigger.WAVE_CLEAR,null);next(r);}
  if(allowSpawn&&allowance>0&&owners.size()<budget){if(r.phase==Phase.WAVE&&!r.pending.isEmpty()){
   // one spawn per run per pass; subsequent scheduler ticks rotate starts for fairness
   Mob m=r.pending.removeFirst();spawn(r,m.id(),false);r.pendingCount--;allowance--;if(m.amount()>1)r.pending.addFirst(new Mob(m.id(),m.amount()-1));
  }else if(r.phase==Phase.BOSS&&!r.bossSpawned){spawn(r,r.plan.boss(),true);r.bossSpawned=true;allowance--;}}
  port.update(view(r,now),r.session.participants());
 }catch(Exception|LinkageError e){close(r,Phase.FAILED,"Battle error: "+e.getClass().getSimpleName()+": "+e.getMessage(),now);}}}
 private void next(Run r){r.wave++;if(r.wave>=r.plan.waves().size()){r.phase=Phase.BOSS;return;}r.phase=Phase.WAVE;for(Mob m:r.plan.waves().get(r.wave)){int amount=r.plan.scaledAmount(m,r.session.participants().size());r.pending.add(new Mob(m.id(),amount));r.pendingCount+=amount;}}
 private void spawn(Run r,String id,boolean boss)throws Exception{Point p=boss?r.arena.bossSpawn():r.arena.mobSpawns().get(Math.floorMod(r.point++,r.arena.mobSpawns().size()));UUID u=Objects.requireNonNull(port.spawn(r.session.id(),id,p,boss,r.session.participants().size()));if(owners.containsKey(u))throw new IllegalStateException("duplicate mob UUID");owners.put(u,new Owner(r.session.id(),boss));r.mobs.add(u);}
 public void abort(UUID id,long now,String reason){Run r=runs.get(id);if(r!=null)close(r,Phase.FAILED,reason,now);}
 public void abortAll(long now){for(Run r:new ArrayList<>(runs.values()))close(r,Phase.FAILED,"Shutdown",now);}
 private void close(Run r,Phase phase,String reason,long now){runs.remove(r.session.id());if(r.phase==Phase.LOOT){phase=Phase.COMPLETED;reason="Boss defeated";}else if(phase==Phase.FAILED&&r.lootExposed)reason="Loot exposed; "+reason;r.phase=phase;r.reason=reason;for(UUID u:r.mobs){owners.remove(u);try{port.remove(u);}catch(RuntimeException|LinkageError ignored){}}r.mobs.clear();r.pending.clear();r.pendingCount=0;registry.close(r.session.id());try{port.finish(view(r,now),r.session.participants(),r.arena);}catch(RuntimeException|LinkageError ignored){}}
 private View view(Run r,long now){return new View(r.session.id(),r.plan.floor(),r.arena.id(),r.phase,Math.min(r.wave+1,r.plan.waves().size()),r.plan.waves().size(),r.pendingCount,r.mobs.size(),Math.max(0,((r.phase==Phase.LOOT?r.lootUntil:r.deadline)-now+999)/1000),r.reason);}
}
