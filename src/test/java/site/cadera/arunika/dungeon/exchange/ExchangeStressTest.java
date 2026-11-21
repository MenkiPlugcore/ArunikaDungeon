package site.cadera.arunika.dungeon.exchange;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
/** Exercises the production single-owner SQLite model, not simulated Minecraft clients. */
public final class ExchangeStressTest {
 static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);System.out.println("PASS "+label);}
 public static void main(String[] args)throws Exception{
  Path dir=Files.createTempDirectory("adg-exchange-stress");
  var owner=Executors.newSingleThreadExecutor();
  var clients=Executors.newFixedThreadPool(8);
  try{
   Path live=dir.resolve("live");ExchangeMigration.importOnce(dir.resolve("absent"),live);
   Path file=live.resolve("exchange.db");
   var reward=new ExchangeCatalog.Reward("stress",UUID.randomUUID(),ExchangeCatalog.Type.ITEM,"",1,10,1,true,1,"Test",false,List.of());
   try(var db=new ExchangeLedger(file)){
    List<Future<Boolean>> results=new ArrayList<>();
    for(int i=0;i<50;i++){final UUID player=UUID.randomUUID();
     results.add(clients.submit(()->owner.submit(()->{
      ExchangeLedger.Tx tx;
      try{tx=db.reserve(player,"test",reward);}catch(IllegalStateException e){if(!e.getMessage().equals("Out of stock"))throw e;return false;}
      db.debit(tx);db.delivered(tx,"stress");return true;
     }).get(20,TimeUnit.SECONDS)));
    }
    int wins=0;for(var result:results)if(result.get(30,TimeUnit.SECONDS))wins++;
    check(wins==10,"50 requests, exactly 10 deliveries");
    check(db.pending().isEmpty(),"no pending transactions after successful delivery");
   }
   var pendingReward=new ExchangeCatalog.Reward("pending",UUID.randomUUID(),ExchangeCatalog.Type.ITEM,"",1,1,1,true,1,"Test",false,List.of());
   UUID player=UUID.randomUUID();ExchangeLedger.Tx pending;
   try(var db=new ExchangeLedger(file)){pending=db.reserve(player,"pending",pendingReward);}
   try(var db=new ExchangeLedger(file)){
    check(db.find(pending.id()).state()==ExchangeLedger.State.RESERVED,"reserved transaction survives reopen");
    boolean rejected=false;try{db.reserve(player,"retry",pendingReward);}catch(IllegalStateException e){rejected=true;}
    check(rejected,"duplicate retry blocked after reopen");
    db.cancel(pending);var tx=db.reserve(player,"retry",pendingReward);db.debit(tx);
    pending=tx;
   }
   try(var db=new ExchangeLedger(file)){
    check(db.find(pending.id()).state()==ExchangeLedger.State.DEBITED,"debited transaction survives reopen");
    boolean rejected=false;try{db.cancel(pending);}catch(IllegalStateException e){rejected=true;}
    check(rejected,"debited transaction cannot be refunded");
    db.delivered(pending,"manual verified");check(db.pending().isEmpty(),"manual resolution clears pending");
   }
   try(var c=DriverManager.getConnection("jdbc:sqlite:"+file);var s=c.createStatement();var rs=s.executeQuery("PRAGMA integrity_check")){check(rs.next()&&rs.getString(1).equals("ok"),"SQLite integrity after stress");}
  }finally{clients.shutdownNow();owner.shutdownNow();clients.awaitTermination(10,TimeUnit.SECONDS);owner.awaitTermination(10,TimeUnit.SECONDS);try(var walk=Files.walk(dir)){for(var f:walk.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(f);}}
 }
}
