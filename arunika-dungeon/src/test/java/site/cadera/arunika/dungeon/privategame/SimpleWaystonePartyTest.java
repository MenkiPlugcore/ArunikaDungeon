package site.cadera.arunika.dungeon.privategame;
import java.util.*;
import java.time.*;
import java.nio.file.*;
import site.cadera.arunika.dungeon.core.SessionRegistry;
/** Pure Java scenario test: cross-world invites, Ready filtering, solo and leader-only tickets. */
public final class SimpleWaystonePartyTest {
 static int passed;
 static void check(boolean ok,String desc){if(!ok)throw new AssertionError(desc);System.out.println("PASS "+desc);passed++;}
 static final class Fake implements PrivateParties.Port{
  final TicketLedger tickets;final SessionRegistry registry;final Map<UUID,String> worlds=new HashMap<>();final Map<UUID,TicketLedger.Ticket> hand=new HashMap<>();final List<SessionRegistry.Session> runs=new ArrayList<>();final List<String> notices=new ArrayList<>();final List<String> arenas=new ArrayList<>();
  Fake(TicketLedger t,SessionRegistry r){tickets=t;registry=r;}
  public boolean eligible(UUID u,String floor){return worlds.containsKey(u);}
  public boolean arenaReady(String floor){return true;}
  public boolean arenaExists(String floor,String arena){return arena.equals("ice")||arena.equals("crypt");}
  public boolean eligibleForStart(UUID user,UUID leader,String floor){return eligible(user,floor)&&Objects.equals(worlds.get(user),worlds.get(leader));}
  public boolean ticketReady(UUID u,TicketLedger.Ticket t)throws Exception{return t!=null&&t.equals(hand.get(u))&&tickets.available(t);}
  public void start(SessionRegistry.Session s,TicketLedger.Ticket t)throws Exception{start(s,t,"");}
  public void start(SessionRegistry.Session s,TicketLedger.Ticket t,String arena)throws Exception{
   tickets.hold(t,s.id(),s.leader());registry.activate(s.id());tickets.commit(t,s.id());runs.add(s);arenas.add(arena);hand.remove(s.leader());
  }
  public void notify(UUID id,String msg){notices.add(id+": "+msg);}
 }
 static TicketLedger.Ticket ticket(TicketLedger l,Fake f,UUID who)throws Exception{var t=l.issue(who);l.acknowledge(t,who);f.hand.put(who,t);return t;}
 public static void main(String[] args)throws Exception{
  Path dir=Files.createTempDirectory("simple-waystone-party-");
  try {
   var r=new SessionRegistry(Set.of("warden"),5,3,Duration.ofMinutes(30),Duration.ofSeconds(60),Clock.systemUTC());
   var ledger=new TicketLedger(dir);var fake=new Fake(ledger,r);var parties=new PrivateParties(r,Map.of("warden",5),fake);
   UUID leader=UUID.randomUUID(),ready=UUID.randomUUID(),notReady=UUID.randomUUID(),wrongWorld=UUID.randomUUID();
   fake.worlds.put(leader,"hub");fake.worlds.put(ready,"hub");fake.worlds.put(notReady,"hub");fake.worlds.put(wrongWorld,"elsewhere");
   var v=parties.create("warden","ice",leader,0);check(v.arena().equals("ice"),"chosen arena persisted");
   for(UUID f:List.of(ready,notReady,wrongWorld)){parties.invite(leader,f,1);parties.accept(f,2);}
   parties.ready(ready,true,3);parties.ready(wrongWorld,true,3);
   var one=ticket(ledger,fake,leader);parties.enqueue(leader,one,4);parties.tick(5,true);
   check(fake.runs.size()==1,"leader starts even with unready and different-world friends");
   var session=fake.runs.getFirst();check(session.participants().equals(Set.of(leader,ready)),"only same-world Ready friend joins");
   check(fake.arenas.getFirst().equals("ice"),"selected arena forwarded to battle");
   check(!ledger.available(one)&&session.leader().equals(leader),"one leader ticket spent");
   check(r.party(notReady).isEmpty()&&r.party(wrongWorld).isEmpty(),"excluded members released");
   check(fake.notices.size()==2,"excluded members notified");
   r.close(session.id());
   var next=parties.create("warden","crypt",leader,100);var solo=ticket(ledger,fake,leader);
   parties.enqueue(leader,solo,101);parties.tick(102,true);
   check(fake.runs.size()==2&&fake.runs.getLast().participants().equals(Set.of(leader)),"solo does not need Ready");
   check(fake.arenas.getLast().equals("crypt"),"solo respects selected arena");
   check(!ledger.available(solo),"solo spends one ticket");
   System.out.println("Simple Waystone private tests passed: "+passed);
  }finally{try(var files=Files.walk(dir)){for(Path file:files.sorted(Comparator.reverseOrder()).toList())Files.delete(file);}}
 }
}
