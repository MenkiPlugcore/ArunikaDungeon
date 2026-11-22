package site.cadera.arunika.dungeon.shared;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import site.cadera.arunika.dungeon.core.SessionRegistry;
import java.util.*;
import java.time.*;
public class SelectedArenaTest {
 @Test void exactArenaSurvivesCountdownAndNeverFallsBack(){
  var registry=new SessionRegistry(Set.of("boss"),5,3,Duration.ofMinutes(30),Duration.ofSeconds(60),Clock.systemUTC());
  List<String> starts=new ArrayList<>();Set<String> blocked=new HashSet<>();
  var port=new SharedQueue.Port(){
   public boolean eligible(UUID p,String f){return true;}
   public boolean ready(String f){return true;}
   public boolean ready(String f,String a){return Set.of("ice","crypt").contains(a)&&!blocked.contains(a);}
   public void start(SessionRegistry.Session s){throw new AssertionError("lost selected arena");}
   public void start(SessionRegistry.Session s,String a){starts.add(a);registry.activate(s.id());}
   public Set<UUID> living(SessionRegistry.Session s){return s.participants();}
  };
  var q=new SharedQueue(registry,Map.of("boss",5),port);UUID p=UUID.randomUUID();
  assertThrows(IllegalStateException.class,()->q.join("boss","unknown",p));
  q.join("boss","crypt",p);
  assertThrows(IllegalStateException.class,()->q.join("boss","ice",UUID.randomUUID()));
  q.tick(0,true);blocked.add("crypt");q.tick(5000,true);assertTrue(starts.isEmpty());
  blocked.clear();q.tick(6000,true);q.tick(11000,true);assertEquals(List.of("crypt"),starts);
  assertThrows(IllegalStateException.class,()->q.join("boss","crypt",UUID.randomUUID()));
  var s=registry.participant(p).orElseThrow();registry.bossDefeated(s.id());registry.close(s.id());
  assertThrows(IllegalStateException.class,()->q.join("boss","crypt",UUID.randomUUID()));
 }
}
