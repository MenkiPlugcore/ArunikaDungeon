package site.cadera.arunika.dungeon.privategame;
import java.util.*;
import java.time.*;
import java.nio.file.*;
import site.cadera.arunika.dungeon.core.SessionRegistry;
import static site.cadera.arunika.dungeon.core.SessionRegistry.Mode.*;

public class PrivateTest {
 static int checks;
 static void ok(boolean b,String m){if(!b)throw new AssertionError(m);checks++;System.out.println("PASS "+m);}
 interface Try{void run()throws Exception;}
 static void reject(Try r,String m){try{r.run();}catch(Exception e){ok(true,m);return;}throw new AssertionError(m);}
 static UUID id(){return UUID.randomUUID();}
 static SessionRegistry registry(){return new SessionRegistry(Set.of("bee","warden"),5,3,Duration.ofMinutes(30),Duration.ofSeconds(60),Clock.systemUTC());}
 static class Port implements PrivateParties.Port{
  final SessionRegistry r;final TicketLedger l;final Map<UUID,TicketLedger.Ticket> hands=new HashMap<>();final Set<String> busy=new HashSet<>();final Set<UUID> offline=new HashSet<>();final List<SessionRegistry.Session> runs=new ArrayList<>();boolean fail;
  Port(SessionRegistry r,TicketLedger l){this.r=r;this.l=l;}
  public boolean eligible(UUID u,String floor){return !offline.contains(u);}
  public boolean arenaReady(String floor){return !busy.contains(floor);}
  public boolean ticketReady(UUID u,TicketLedger.Ticket t)throws Exception{return t.equals(hands.get(u))&&l.available(t);}
  public void start(SessionRegistry.Session s,TicketLedger.Ticket t)throws Exception{l.hold(t,s.id(),s.leader());if(fail){l.refund(t,s.id());throw new IllegalStateException("teleport failed");}r.activate(s.id());l.commit(t,s.id());hands.remove(s.leader());runs.add(s);}
 }
 public static void main(String[] args)throws Exception{
  Path dir=Files.createTempDirectory("adg-ticket-test");try{
   var l=new TicketLedger(dir);var r=registry();var p=new Port(r,l);var q=new PrivateParties(r,Map.of("bee",5,"warden",2),p);UUID lead=id(),a=id(),b=id(),c=id(),d=id(),e=id();
   q.create("bee",lead,0);reject(()->r.enqueue("warden",lead),"party blocks Shared queue");reject(()->r.reserve("bee",PRIVATE,lead,Set.of(lead)),"party blocks unrelated reservation");reject(()->q.create("warden",lead,0),"one party per UUID");
   q.invite(lead,a,0);reject(()->q.accept(a,60000),"invitation expires");q.invite(lead,a,60001);q.accept(a,60002);ok(q.status(a).members().size()==2,"accept invite");reject(()->q.invite(a,b,60003),"leader only invites");q.invite(lead,b,60003);ok(q.decline(b),"decline invitation");
   q.ready(lead,true,60004);q.invite(lead,b,60005);q.accept(b,60006);ok(q.status(lead).ready().isEmpty(),"membership change clears ready");for(UUID u:List.of(c,d)){q.invite(lead,u,60007);q.accept(u,60008);}reject(()->q.invite(lead,e,60009),"five members maximum");
   var ticket=l.issue(lead);l.acknowledge(ticket,lead);p.hands.put(lead,ticket);ok(q.status(lead).ready().isEmpty(),"ready is optional for leader");for(UUID u:List.of(lead,a,b,c,d))q.ready(u,true,60011);reject(()->q.enqueue(a,ticket,60012),"leader only starts");q.enqueue(lead,ticket,60013);reject(()->q.kick(lead,a,60014),"queued roster immutable");p.busy.add("bee");q.tick(60015,true);ok(q.status(lead).queued()&&l.available(ticket),"busy arena queues without charging");
   UUID solo=id();q.create("warden",solo,60015);q.ready(solo,true,60016);var t2=l.issue(solo);p.hands.put(solo,t2);q.enqueue(solo,t2,60017);q.tick(60018,false);ok(p.runs.isEmpty(),"performance guard delays dispatch");q.tick(60019,true);ok(p.runs.size()==1&&p.runs.getFirst().floor().equals("warden"),"other floor not blocked");ok(!l.available(t2),"solo ticket spent");p.busy.clear();q.tick(60020,true);ok(p.runs.size()==2&&p.runs.getLast().participants().size()==5,"one ticket pays whole party");ok(!l.available(ticket)&&r.participant(a).isPresent(),"active participant indexed");reject(()->l.hold(ticket,id(),lead),"copied spent ticket rejected");
   var session=p.runs.getLast();var refunded=l.refund(ticket,session.id());ok(refunded.generation()==1&&!l.available(ticket)&&l.available(refunded),"refund invalidates old copies");reject(()->l.refund(ticket,session.id()),"double refund rejected");reject(()->l.claim(ticket.serial(),a),"only payer may claim refund");ok(l.claim(ticket.serial(),lead).equals(refunded),"refund claim preserved");var reopened=new TicketLedger(dir);ok(reopened.claim(ticket.serial(),lead).equals(refunded),"refund survives restart");reopened.acknowledge(refunded,lead);reject(()->reopened.claim(ticket.serial(),lead),"delivered claim cannot replay");
   var held=l.issue(e);UUID interrupted=id();l.hold(held,interrupted,e);reject(()->l.commit(held,id()),"session mismatch rejected");ok(reopened.recoverHeld().contains(held.serial()),"interrupted hold recovered");ok(reopened.claim(held.serial(),e).generation()==1,"interrupted hold refund generation");ok(reopened.recoverHeld().isEmpty(),"recovery idempotent");ok(!reopened.available(t2),"spent ticket not automatically refunded on restart");reopened.complete(t2,p.runs.getFirst().id());ok(reopened.read(t2.serial()).state()==TicketLedger.State.CONSUMED,"completed ticket terminal state");reject(()->reopened.refund(t2,p.runs.getFirst().id()),"completed encounter cannot be refunded");reject(()->l.available(new TicketLedger.Ticket(id(),0)),"unknown serial rejected");
   r.close(session.id());r.close(p.runs.getFirst().id());q.create("warden",e,70000);q.invite(e,a,70001);q.accept(a,70002);reject(()->q.invite(e,b,70003),"per-floor lower limit respected");q.transfer(e,a,70004);ok(q.status(e).leader().equals(a),"transfer leader");q.kick(a,e,70005);ok(r.party(e).isEmpty(),"kick releases UUID");q.ready(a,true,70006);var moved=l.issue(a);p.hands.put(a,moved);q.enqueue(a,moved,70007);p.hands.remove(a);q.tick(70008,true);ok(!q.status(a).queued()&&l.available(moved),"moved ticket cancels queue without charge");
   q.ready(a,true,70009);p.hands.put(a,moved);q.enqueue(a,moved,70010);p.fail=true;q.tick(70011,true);ok(r.participant(a).isEmpty()&&l.claim(moved.serial(),a).generation()==1,"failed start releases reservation and refunds");
   q.create("bee",b,80000);q.invite(b,c,80001);q.accept(c,80002);p.offline.add(b);q.tick(80003,true);ok(r.party(c).isEmpty(),"leader disconnect disbands party");q.create("bee",d,80004);q.tick(680004,true);ok(r.party(d).isEmpty(),"inactive party expires");q.clear();ok(q.empty(),"shutdown clears parties");
   var pending=l.issue(a);ok(!l.availableFor(pending,b)&&l.availableFor(pending,a),"undelivered claim restricted to owner");reject(()->l.hold(pending,id(),b),"stolen pending claim denied");l.acknowledge(pending,a);ok(l.availableFor(pending,b),"acknowledged ticket tradeable");UUID exposure=id();l.hold(pending,exposure,b);l.commit(pending,exposure);l.markExposed(pending,exposure);l.markExposed(pending,exposure);var restart=new TicketLedger(dir);ok(restart.read(pending.serial()).state()==TicketLedger.State.EXPOSED,"reward exposure survives restart");reject(()->restart.refund(pending,exposure),"refund after loot exposure rejected even after crash");reject(()->restart.markExposed(pending,id()),"exposure session mismatch denied");restart.complete(pending,exposure);ok(restart.read(pending.serial()).state()==TicketLedger.State.CONSUMED,"exposed ticket completes normally");
   System.out.println("Private tests passed: "+checks);
  }finally{try(var files=Files.walk(dir)){for(var file:files.sorted(Comparator.reverseOrder()).toList())Files.delete(file);}}
 }
}
