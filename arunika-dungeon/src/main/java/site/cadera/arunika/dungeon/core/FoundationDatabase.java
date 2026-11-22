package site.cadera.arunika.dungeon.core;
import java.nio.file.*;
import java.sql.*;
/** Isolated database, no connection to legacy exchange.db. Call on a worker thread. */
public final class FoundationDatabase {
    private FoundationDatabase() {}
    public static void initialize(Path file) throws SQLException, java.io.IOException, ClassNotFoundException {
        Files.createDirectories(file.toAbsolutePath().getParent());
        if (Files.isSymbolicLink(file)) throw new java.io.IOException("Database symlink refused");
        Class.forName("org.sqlite.JDBC",true,FoundationDatabase.class.getClassLoader());
        try(Connection c=DriverManager.getConnection("jdbc:sqlite:"+file.toAbsolutePath()); Statement s=c.createStatement()) {
            s.execute("PRAGMA busy_timeout=3000"); s.execute("PRAGMA foreign_keys=ON");
            int version; try(ResultSet r=s.executeQuery("PRAGMA user_version")){if(!r.next())throw new SQLException("No schema version");version=r.getInt(1);}
            if(version>2)throw new SQLException("Newer database schema; refusing downgrade");
            // Isolated foundation DB only. Snapshot v1 before changing its schema.
            if(version==1){
                Path backups=file.toAbsolutePath().getParent().getParent().resolve("backups");
                if(Files.isSymbolicLink(backups))throw new java.io.IOException("Backup symlink refused");
                Files.createDirectories(backups);
                Path snapshot=backups.resolve("foundation-db-v1-"+java.util.UUID.randomUUID()+".db");
                s.execute("VACUUM INTO '"+snapshot.toString().replace("'","''")+"'");
            }
            c.setAutoCommit(false);
            try {
                s.execute("CREATE TABLE IF NOT EXISTS foundation_metadata (key TEXT PRIMARY KEY, value TEXT NOT NULL)");
                s.execute("CREATE TABLE IF NOT EXISTS migration_imports (id TEXT PRIMARY KEY, manifest_sha256 TEXT NOT NULL, imported_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)");
                s.execute("CREATE TABLE IF NOT EXISTS config_schema_history (schema_version INTEGER PRIMARY KEY, migrated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)");
                s.execute("INSERT OR IGNORE INTO config_schema_history(schema_version) VALUES (2)");
                s.execute("INSERT INTO foundation_metadata(key,value) VALUES ('release','1.0.0') ON CONFLICT(key) DO UPDATE SET value=excluded.value");
                s.execute("PRAGMA user_version=2"); c.commit();
            } catch(SQLException e){c.rollback();throw e;}
        }
    }
}
