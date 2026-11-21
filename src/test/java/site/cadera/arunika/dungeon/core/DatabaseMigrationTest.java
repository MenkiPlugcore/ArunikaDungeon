package site.cadera.arunika.dungeon.core;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
public final class DatabaseMigrationTest {
    static void check(boolean b,String s){if(!b)throw new AssertionError(s);System.out.println("PASS "+s);}
    public static void main(String[] args)throws Exception{
        Path root=Files.createTempDirectory("adg-db-test-");try{Path db=root.resolve("data/dungeon.db");Files.createDirectories(db.getParent());Class.forName("org.sqlite.JDBC");String url="jdbc:sqlite:"+db;
            try(var c=DriverManager.getConnection(url);var s=c.createStatement()){
                s.execute("CREATE TABLE foundation_metadata (key TEXT PRIMARY KEY,value TEXT NOT NULL)");s.execute("INSERT INTO foundation_metadata VALUES ('release','0.1.0'),('custom','preserve')");
                s.execute("CREATE TABLE migration_imports (id TEXT PRIMARY KEY,manifest_sha256 TEXT NOT NULL,imported_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)");s.execute("INSERT INTO migration_imports(id,manifest_sha256) VALUES ('legacy-1','abc')");s.execute("PRAGMA user_version=1");}
            FoundationDatabase.initialize(db);FoundationDatabase.initialize(db);
            try(var c=DriverManager.getConnection(url);var s=c.createStatement()){
                try(var r=s.executeQuery("PRAGMA user_version")){check(r.next()&&r.getInt(1)==2,"database schema 2");}
                try(var r=s.executeQuery("SELECT value FROM foundation_metadata WHERE key='custom'")){check(r.next()&&r.getString(1).equals("preserve"),"existing metadata preserved");}
                try(var r=s.executeQuery("SELECT count(*) FROM migration_imports")){check(r.next()&&r.getInt(1)==1,"migration ledger preserved");}
                try(var r=s.executeQuery("PRAGMA integrity_check")){check(r.next()&&r.getString(1).equals("ok"),"database integrity");}}
            try(var list=Files.list(root.resolve("backups"))){check(list.count()==1,"one automatic database backup; restart idempotent");}
            try(var c=DriverManager.getConnection(url);var s=c.createStatement()){s.execute("PRAGMA user_version=99");}
            try{FoundationDatabase.initialize(db);throw new AssertionError("Future schema accepted");}catch(SQLException expected){System.out.println("PASS future DB schema refused");}
            System.out.println("TOTAL 6 PASS");
        }finally{try(var walk=Files.walk(root)){for(Path p:walk.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}}
    }
}
