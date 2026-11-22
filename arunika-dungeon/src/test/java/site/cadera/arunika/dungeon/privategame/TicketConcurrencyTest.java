package site.cadera.arunika.dungeon.privategame;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
class TicketConcurrencyTest {
 @TempDir Path root;
 @Test void duplicatePhysicalSerialHasOneReservation()throws Exception{
  var ledger=new TicketLedger(root);var ticket=ledger.issuePhysical();var worker=Executors.newFixedThreadPool(8);
  try{List<Callable<Boolean>> calls=new ArrayList<>();for(int i=0;i<50;i++)calls.add(()->{try{ledger.hold(ticket,UUID.randomUUID(),UUID.randomUUID());return true;}catch(IllegalStateException e){return false;}});
   int successes=0;for(var f:worker.invokeAll(calls))if(f.get())successes++;assertEquals(1,successes);
   var record=ledger.read(ticket.serial());var next=ledger.refund(ticket,record.session());assertFalse(ledger.available(ticket));assertTrue(ledger.availableFor(next,record.payer()));
   assertThrows(IllegalStateException.class,()->ledger.refund(ticket,record.session()));
   var reopened=new TicketLedger(root);assertEquals(next,reopened.claim(next.serial(),record.payer()));
  }finally{worker.shutdownNow();}
 }
}
