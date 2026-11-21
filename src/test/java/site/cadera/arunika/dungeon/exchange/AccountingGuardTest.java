package site.cadera.arunika.dungeon.exchange;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
public final class AccountingGuardTest {
 static int count;
 static void check(boolean value,String label){if(!value)throw new AssertionError(label);count++;System.out.println("PASS "+label);}
 static ExchangeCatalog.Reward reward(UUID id,int cost,int stock){return new ExchangeCatalog.Reward("test",id,ExchangeCatalog.Type.ITEM,"",cost,stock,0,true,1,"test",false,List.of());}
 static void refused(ExchangeLedger ledger,ExchangeCatalog.Reward reward)throws Exception{try{ledger.reserve(UUID.randomUUID(),"test",reward);throw new AssertionError("invalid accounting accepted");}catch(IllegalStateException expected){}}
 public static void main(String[] args)throws Exception{
  Path root=Files.createTempDirectory("adg-accounting-gate");
  try{
   Path live=root.resolve("live");ExchangeMigration.importOnce(root.resolve("missing"),live);Path file=live.resolve("exchange.db");UUID id=UUID.randomUUID();
   try(var ledger=new ExchangeLedger(file)){
    refused(ledger,reward(id,0,10));check(ledger.pending().isEmpty(),"zero-cost direct reservation has no transaction");
    refused(ledger,reward(id,1,-2));check(ledger.pending().isEmpty(),"invalid negative stock rejected");
    var tx=ledger.reserve(UUID.randomUUID(),"seed",reward(id,1,10));ledger.cancel(tx);
    try(var c=DriverManager.getConnection("jdbc:sqlite:"+file);var stmt=c.prepareStatement("UPDATE reward_stock SET remaining=? WHERE reward_id=?")){stmt.setInt(1,11);stmt.setString(2,id.toString());stmt.executeUpdate();}
    refused(ledger,reward(id,1,10));check(ledger.pending().isEmpty(),"stock above configured ceiling cannot oversell");
    try(var c=DriverManager.getConnection("jdbc:sqlite:"+file);var stmt=c.prepareStatement("UPDATE reward_stock SET configured=-1,remaining=5 WHERE reward_id=?")){stmt.setString(1,id.toString());stmt.executeUpdate();}
    refused(ledger,reward(id,1,-1));check(ledger.pending().isEmpty(),"inconsistent unlimited-stock sentinel rejected");
   }
   Path db=live.resolve("exchange.db"),saved=live.resolve("preserved.db");Files.move(db,saved);Files.createSymbolicLink(db,saved);boolean refused=false;try{ExchangeMigration.importOnce(root.resolve("missing"),live);}catch(java.io.IOException expected){refused=true;}check(refused&&Files.size(saved)>0,"existing Exchange symlink refused without resetting database");
   System.out.println("TOTAL "+count+" PASS");
  }finally{try(var paths=Files.walk(root)){for(var p:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(p);}}
 }
}
