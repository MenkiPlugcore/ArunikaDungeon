package site.cadera.arunika.dungeon.shared;

import java.util.*;
import java.time.*;
import site.cadera.arunika.dungeon.core.SessionRegistry;

/** Main-thread queue orchestrator. One countdown per floor, no participant cap or per-player tasks. */
public final class SharedQueue {
 public enum State { AVAILABLE, COUNTDOWN, COOLDOWN, IN_BATTLE, LOOT, PAUSED }
 public record View(String floor, State state, long seconds, List<UUID> queued, Set<UUID> active, Set<UUID> living){public View{queued=List.copyOf(queued);active=Set.copyOf(active);living=Set.copyOf(living);}}
 public interface Port {
  boolean eligible(UUID player,String floor);
  boolean ready(String floor);
  void start(SessionRegistry.Session session) throws Exception;
  Set<UUID> living(SessionRegistry.Session session);
  default void notify(UUID player,String message){}
 }
 private static final class Floor {final int countdown;final LinkedHashSet<UUID> waiting=new LinkedHashSet<>();long deadline=-1,retryAt;Floor(int seconds){if(seconds<5||seconds>300)throw new IllegalArgumentException("countdown");countdown=seconds;}}
 private final Map<String,Floor> floors=new LinkedHashMap<>();private final SessionRegistry registry;private final Port port;
 public SharedQueue(SessionRegistry registry,Map<String,Integer> settings,Port port){this.registry=Objects.requireNonNull(registry);this.port=Objects.requireNonNull(port);settings.forEach((id,seconds)->floors.put(id,new Floor(seconds)));}
 public void join(String floor,UUID player){Floor f=require(floor);if(!port.eligible(player,floor))throw new IllegalStateException("Player not eligible");registry.enqueue(floor,player);f.waiting.add(player);}
 public boolean leave(UUID player){var id=registry.queuedFloor(player);if(id.isEmpty())return false;Floor f=floors.get(id.get());if(f==null)return false;boolean removed=f.waiting.remove(player);registry.dequeue(id.get(),player);if(f.waiting.isEmpty())f.deadline=-1;return removed;}
 public boolean empty(){return floors.values().stream().allMatch(f->f.waiting.isEmpty());}
 public void clear(){for(var e:floors.entrySet()){for(UUID p:e.getValue().waiting)registry.dequeue(e.getKey(),p);e.getValue().waiting.clear();e.getValue().deadline=-1;}}
 private Floor require(String id){Floor f=floors.get(id);if(f==null)throw new IllegalArgumentException("Shared floor unavailable");return f;}
 private Map<String,SessionRegistry.Session> active(){Map<String,SessionRegistry.Session> out=new HashMap<>();for(var s:registry.snapshot())if(s.mode()==SessionRegistry.Mode.SHARED)out.put(s.floor(),s);return out;}
 private long cooldown(String id){return Math.max(0,registry.remaining(id).toMillis());}
 public void tick(long now,boolean performanceOk){var active=active();for(var e:floors.entrySet()){String id=e.getKey();Floor f=e.getValue();
  for(UUID p:new ArrayList<>(f.waiting))if(!port.eligible(p,id))leave(p);
  if(f.waiting.isEmpty()||active.containsKey(id)||cooldown(id)>0||!registry.storageHealthy()||!performanceOk||now<f.retryAt||!port.ready(id)){f.deadline=-1;continue;}
  if(f.deadline<0){f.deadline=now+1000L*f.countdown;for(UUID p:f.waiting)port.notify(p,"Shared "+id+" starts in "+f.countdown+"s");continue;}
  if(now<f.deadline)continue;
  var members=new LinkedHashSet<>(f.waiting);SessionRegistry.Session s=null;
  try{s=registry.reserveQueued(id,members.getFirst(),members);port.start(s);f.waiting.clear();f.deadline=-1;}
  catch(Exception|LinkageError ex){if(s!=null){registry.close(s.id());for(UUID p:members){try{registry.enqueue(id,p);}catch(RuntimeException ignored){f.waiting.remove(p);}}}f.deadline=-1;f.retryAt=now+10000;for(UUID p:f.waiting)port.notify(p,"Shared start postponed: "+ex.getMessage());}
 }}
 public List<View> views(long now,boolean performanceOk){var active=active();List<View> result=new ArrayList<>();for(var e:floors.entrySet()){String id=e.getKey();Floor f=e.getValue();var run=active.get(id);long cd=cooldown(id);State state;long seconds=0;if(run!=null){state=run.state()==SessionRegistry.State.LOOT?State.LOOT:State.IN_BATTLE;if(state==State.LOOT)seconds=(cd+999)/1000;}else if(cd>0){state=State.COOLDOWN;seconds=(cd+999)/1000;}else if(!registry.storageHealthy()||!performanceOk||now<f.retryAt||!port.ready(id))state=State.PAUSED;else if(f.deadline>=0){state=State.COUNTDOWN;seconds=Math.max(0,(f.deadline-now+999)/1000);}else state=State.AVAILABLE;result.add(new View(id,state,seconds,new ArrayList<>(f.waiting),run==null?Set.of():run.participants(),run==null?Set.of():port.living(run)));}return List.copyOf(result);}
}
