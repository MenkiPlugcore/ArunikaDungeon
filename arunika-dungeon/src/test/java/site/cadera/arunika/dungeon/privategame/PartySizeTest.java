package site.cadera.arunika.dungeon.privategame;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.time.*;
import java.nio.file.*;
import site.cadera.arunika.dungeon.core.SessionRegistry;
class PartySizeTest {
 @TempDir Path temp;
 @Test void fiveMembersUseExactlyOneLeaderTicket()throws Exception{run(5);}
 @Test void twoMembersUseExactlyOneLeaderTicket()throws Exception{run(2);}
 void run(int size)throws Exception{
  var r=new SessionRegistry(Set.of("warden"),5,3,Duration.ofMinutes(30),Duration.ofSeconds(60),Clock.systemUTC());
  var ledger=new TicketLedger(temp);var fake=new SimpleWaystonePartyTest.Fake(ledger,r);
  var service=new PrivateParties(r,Map.of("warden",5),fake);UUID leader=UUID.randomUUID();fake.worlds.put(leader,"hub");
  service.create("warden","ice",leader,0);List<UUID> friends=new ArrayList<>();Map<UUID,TicketLedger.Ticket> untouched=new HashMap<>();
  for(int i=1;i<size;i++){UUID id=UUID.randomUUID();friends.add(id);fake.worlds.put(id,"hub");untouched.put(id,SimpleWaystonePartyTest.ticket(ledger,fake,id));service.invite(leader,id,1);service.accept(id,2);}
  UUID extra=UUID.randomUUID();fake.worlds.put(extra,"hub");if(size==5)assertThrows(IllegalStateException.class,()->service.invite(leader,extra,3));
  for(UUID id:friends)service.ready(id,true,4);
  var ticket=SimpleWaystonePartyTest.ticket(ledger,fake,leader);service.enqueue(leader,ticket,5);service.tick(6,true);
  assertEquals(1,fake.runs.size());assertEquals(size,fake.runs.getFirst().participants().size());assertFalse(ledger.available(ticket));
  for(var e:untouched.entrySet())assertTrue(ledger.availableFor(e.getValue(),e.getKey()));
 }
}
