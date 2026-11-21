package site.cadera.arunika.dungeon.exchange;
import java.nio.file.*;
import java.io.*;
import java.sql.*;
import java.util.*;
import java.security.*;
/** Run only when old Exchange not loaded. Copies WAL into disposable work dir; never connects to old DB. */
public final class ExchangeMigration {
 private static final List<String> FILES=List.of("config.yml","rewards.yml","exchange.db","exchange.db-wal","exchange.db-shm");
 private static Map<String,String> hashes(Path dir)throws Exception{var m=new LinkedHashMap<String,String>();for(String name:FILES){Path f=dir.resolve(name);if(Files.exists(f,LinkOption.NOFOLLOW_LINKS)){if(!Files.isRegularFile(f,LinkOption.NOFOLLOW_LINKS))throw new IOException("Unsafe legacy file "+name);try(var in=Files.newInputStream(f)){var md=MessageDigest.getInstance("SHA-256");byte[] buf=new byte[65536];for(int n;(n=in.read(buf))>=0;)md.update(buf,0,n);m.put(name,HexFormat.of().formatHex(md.digest()));}}}return m;}
 public static boolean importOnce(Path source,Path target)throws Exception{
  if(Files.exists(target,LinkOption.NOFOLLOW_LINKS)){if(Files.isSymbolicLink(target)||FILES.stream().anyMatch(name->Files.isSymbolicLink(target.resolve(name)))||!Files.isRegularFile(target.resolve("migration-complete.txt"))||!Files.isRegularFile(target.resolve("exchange.db"))||!Files.isRegularFile(target.resolve("rewards.yml"))||!Files.isRegularFile(target.resolve("config.yml")))throw new IOException("Incomplete existing Exchange; refused overwrite");return false;}
  if(Files.notExists(source,LinkOption.NOFOLLOW_LINKS))return fresh(target);
  if(!Files.isDirectory(source,LinkOption.NOFOLLOW_LINKS))throw new IOException("Unsafe legacy Exchange path");var before=hashes(source);if(!before.keySet().containsAll(List.of("config.yml","rewards.yml","exchange.db")))throw new IOException("Incomplete legacy Exchange");Files.createDirectories(target.getParent());Path temp=Files.createTempDirectory(target.getParent(),".exchange-import-");
  try{Path raw=Files.createDirectory(temp.resolve("legacy-original"));for(String f:before.keySet())Files.copy(source.resolve(f),raw.resolve(f));if(!before.equals(hashes(raw))||!before.equals(hashes(source)))throw new IOException("Legacy changed during import");Path work=Files.createDirectory(temp.resolve("work"));for(String f:List.of("exchange.db","exchange.db-wal"))if(Files.exists(raw.resolve(f)))Files.copy(raw.resolve(f),work.resolve(f));Files.copy(raw.resolve("config.yml"),temp.resolve("config.yml"));Files.copy(raw.resolve("rewards.yml"),temp.resolve("rewards.yml"));Class.forName("org.sqlite.JDBC");try(var c=DriverManager.getConnection("jdbc:sqlite:"+work.resolve("exchange.db"));var s=c.createStatement()){try(var r=s.executeQuery("PRAGMA integrity_check")){if(!r.next()||!r.getString(1).equals("ok")||r.next())throw new SQLException("Legacy integrity failure");}s.execute("VACUUM INTO '"+temp.resolve("exchange.db").toString().replace("'","''")+"'");}
   try(var c=DriverManager.getConnection("jdbc:sqlite:"+temp.resolve("exchange.db"));var s=c.createStatement()){for(String t:List.of("transactions","purchases","reward_stock"))try(var r=s.executeQuery("SELECT COUNT(*) FROM "+t)){if(!r.next())throw new SQLException("Missing table "+t);}}
   if(!before.equals(hashes(source)))throw new IOException("Legacy changed during migration");delete(work);Files.writeString(temp.resolve("migration-complete.txt"),"v0.7.0\n"+before+"\n");Files.move(temp,target,StandardCopyOption.ATOMIC_MOVE);return true;
  }finally{if(Files.exists(temp))delete(temp);}
 }
 private static boolean fresh(Path target)throws Exception{
  Files.createDirectories(target.getParent());Path temp=Files.createTempDirectory(target.getParent(),".exchange-fresh-");
  try{
   Files.writeString(temp.resolve("config.yml"),"token:\n  template-base64: ''\n  fresh-token-unset: true\n");
   Files.writeString(temp.resolve("rewards.yml"),"format: 2\nrewards: {}\n");
   Class.forName("org.sqlite.JDBC");
   try(var c=DriverManager.getConnection("jdbc:sqlite:"+temp.resolve("exchange.db"));var s=c.createStatement()){
    s.execute("CREATE TABLE reward_stock(reward_id TEXT PRIMARY KEY,configured INTEGER NOT NULL,remaining INTEGER NOT NULL)");
    s.execute("CREATE TABLE purchases(player_id TEXT NOT NULL,reward_id TEXT NOT NULL,count INTEGER NOT NULL,PRIMARY KEY(player_id,reward_id))");
    s.execute("CREATE TABLE transactions(id TEXT PRIMARY KEY,player_id TEXT NOT NULL,player_name TEXT NOT NULL,reward_id TEXT NOT NULL,reward_type TEXT NOT NULL,cost INTEGER NOT NULL,status TEXT NOT NULL,detail TEXT NOT NULL DEFAULT '',created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)");
    s.execute("CREATE INDEX idx_transactions_status ON transactions(status)");
    s.execute("CREATE INDEX idx_transactions_player ON transactions(player_id,status)");
    try(var r=s.executeQuery("PRAGMA integrity_check")){if(!r.next()||!"ok".equals(r.getString(1))||r.next())throw new SQLException("Fresh database integrity failure");}
   }
   Files.writeString(temp.resolve("migration-complete.txt"),"RC5 fresh initialization; no legacy data existed\n");
   if(!Files.notExists(target,LinkOption.NOFOLLOW_LINKS))throw new IOException("Exchange appeared during initialization; refused overwrite");
   Files.move(temp,target,StandardCopyOption.ATOMIC_MOVE);return true;
  }finally{if(Files.exists(temp))delete(temp);}
 }
 private static void delete(Path p)throws IOException{try(var walk=Files.walk(p)){for(Path f:walk.sorted(Comparator.reverseOrder()).toList())Files.delete(f);}}
}
