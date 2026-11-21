package site.cadera.arunika.dungeon.battle;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;
import static java.nio.file.StandardOpenOption.*;

/** Durable evidence for unissued loot. Never automatically replay uncertain rewards. */
public final class LootRecoveryStore {
    public record Unissued(LootPlan.Entry entry,int amount,String reason) {
        public Unissued { Objects.requireNonNull(entry);Objects.requireNonNull(reason);if(amount<1||amount>4096)throw new IllegalArgumentException("amount"); }
    }
    private final Path root;
    public LootRecoveryStore(Path root) {this.root=root.toAbsolutePath().normalize();}
    public Path save(UUID session,List<Unissued> entries) throws IOException {
        if(entries.isEmpty()||entries.size()>128)throw new IOException("Invalid recovery batch");
        Files.createDirectories(root);if(Files.isSymbolicLink(root))throw new IOException("Recovery directory symlink refused");
        Properties data=new Properties();data.setProperty("format","1");data.setProperty("session",session.toString());data.setProperty("state","UNISSUED_REQUIRES_REVIEW");data.setProperty("count",Integer.toString(entries.size()));
        for(int i=0;i<entries.size();i++) {
            var e=entries.get(i);String key="entry."+i+".";
            data.setProperty(key+"type",e.entry().type().name());data.setProperty(key+"id",e.entry().id());data.setProperty(key+"item-type",e.entry().itemType());data.setProperty(key+"amount",Integer.toString(e.amount()));data.setProperty(key+"reason",e.reason());
        }
        Path pending=Files.createTempFile(root,".pending-",".tmp"),dest=root.resolve(session+"-"+UUID.randomUUID()+".loot");
        try {
            try(var out=Files.newOutputStream(pending,WRITE,TRUNCATE_EXISTING)){data.store(out,"ArunikaDungeon unissued loot; verify before any manual compensation");}
            try(var file=FileChannel.open(pending,WRITE)){file.force(true);}
            Files.move(pending,dest,StandardCopyOption.ATOMIC_MOVE);
            try(var dir=FileChannel.open(root,READ)){dir.force(true);}catch(IOException|UnsupportedOperationException ignored){}
            return dest;
        }finally{Files.deleteIfExists(pending);}
    }
}
