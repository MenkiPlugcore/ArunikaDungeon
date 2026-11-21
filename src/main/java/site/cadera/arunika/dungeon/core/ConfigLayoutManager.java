package site.cadera.arunika.dungeon.core;

import java.io.*;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.time.*;
import java.util.*;

/** Owns only plugins/ArunikaDungeon. Never reads or rewrites legacy plugin configuration. */
public final class ConfigLayoutManager {
    public record Change(String path,String action) {}
    public record Report(int created,int updated,int unchanged,boolean recovered,List<Change> changes) {
        public Report {changes=List.copyOf(changes);}
    }
    @FunctionalInterface public interface FaultInjector {void afterWrite(int writes) throws IOException;}
    private record FilePlan(String path,byte[] output,boolean exists,String action) {}
    private static final String PENDING=".config-migration-pending";
    private static final int MAX_BYTES=1_048_576;
    private static final Set<String> DIRS=Set.of("data","backups","logs","dungeons","exchange","integrations");
    private ConfigLayoutManager() {}
    private static String template(String name) throws IOException {
        try(InputStream in=ConfigLayoutManager.class.getClassLoader().getResourceAsStream("templates/"+name)){
            if(in==null)throw new IOException("Missing bundled config template: "+name);
            return new String(in.readAllBytes(),StandardCharsets.UTF_8);
        }
    }
    private static String fill(String content,DungeonCatalog.Floor f) {
        String legacy=f.bossReference().isEmpty()?"No legacy MythicMobs ID confirmed":("Reference from old archive: "+f.bossReference()+" (NOT enabled)");
        return content.replace("{{ID}}",f.id()).replace("{{FLOOR}}",Integer.toString(f.number()))
            .replace("{{NAME}}",f.name()).replace("{{LEGACY_NOTE}}",legacy);
    }
    private static Map<String,String> layouts() throws IOException {
        Map<String,String> out=new LinkedHashMap<>();
        for(String name:List.of("config.yml","messages.yml","performance.yml","ticket.yml","exchange/settings.yml","integrations/waystone.yml","integrations/nusa-karsa.yml"))
            out.put(name,template(name));
        for(var f:DungeonCatalog.INITIAL) {
            if(!f.id().matches("[a-z][a-z0-9_]*")||f.number()<1||f.number()>999)throw new IOException("Invalid catalog ID");
            String base="dungeons/%02d_%s/".formatted(f.number(),f.id());
            for(String name:List.of("settings.yml","arena.yml","waves.yml","boss.yml"))out.put(base+name,fill(template(name),f));
            for(String mode:List.of("shared","private")){
                out.put(base+mode+"/settings.yml",template(mode+"/settings.yml"));
                for(int wave=1;wave<=3;wave++)out.put(base+mode+"/wave-"+wave+"-drops.yml",template("drops.yml").replace("{{TRIGGER}}","WAVE_CLEAR"));
                out.put(base+mode+"/boss-drops.yml",template("drops.yml").replace("{{TRIGGER}}","BOSS_KILL"));
            }
        }
        return out;
    }
    private static Path safe(Path root,String path) throws IOException {
        Path rel=Path.of(path);
        if(rel.isAbsolute()||rel.normalize().startsWith("..")||!rel.normalize().equals(rel))throw new IOException("Unsafe relative path: "+path);
        Path dst=root.resolve(rel);
        Path p=root;
        if(Files.isSymbolicLink(p))throw new IOException("Symlink root refused: "+p);
        for(Path part:rel){p=p.resolve(part);if(Files.isSymbolicLink(p))throw new IOException("Symlink refused: "+p);}
        return dst;
    }
    private static void checkRoot(Path root) throws IOException {
        if(Files.isSymbolicLink(root))throw new IOException("Symlink config directory refused");
        if(Files.exists(root,LinkOption.NOFOLLOW_LINKS)&&!Files.isDirectory(root,LinkOption.NOFOLLOW_LINKS))throw new IOException("Config root must be a directory");
    }
    private static void atomic(Path dst,byte[] b) throws IOException {
        Path parent=dst.getParent();Files.createDirectories(parent);
        Path tmp=Files.createTempFile(parent,".adg-",".tmp");
        try {
            Files.write(tmp,b,StandardOpenOption.TRUNCATE_EXISTING);
            try{Files.move(tmp,dst,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException ex){Files.move(tmp,dst,StandardCopyOption.REPLACE_EXISTING);}
        }finally{Files.deleteIfExists(tmp);}
    }
    private static byte[] read(Path file) throws IOException {
        if(!Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS))throw new IOException("Not a regular managed file: "+file);
        if(Files.size(file)>MAX_BYTES)throw new IOException("Managed file too large: "+file);
        return Files.readAllBytes(file);
    }
    private static List<FilePlan> plans(Path root,Map<String,String> templates) throws IOException {
        List<FilePlan> planned=new ArrayList<>();
        for(var e:templates.entrySet()){
            Path dst=safe(root,e.getKey());boolean exists=Files.exists(dst,LinkOption.NOFOLLOW_LINKS);
            byte[] next;
            if(exists){
                byte[] b=read(dst);
                String current=new String(b,StandardCharsets.UTF_8);
                String merged=YamlDefaultsMerge.merge(current,e.getValue());
                if(merged.equals(current))next=b;else next=merged.getBytes(StandardCharsets.UTF_8);
                planned.add(new FilePlan(e.getKey(),next,true,Arrays.equals(b,next)?"UNCHANGED":"UPDATED"));
            }else planned.add(new FilePlan(e.getKey(),e.getValue().getBytes(StandardCharsets.UTF_8),false,"CREATED"));
        }
        return planned;
    }
    /** Non-mutating inspection, useful for release review. */
    public static List<Change> preview(Path root) throws IOException {
        checkRoot(root);List<FilePlan> plans=plans(root,layouts());
        return plans.stream().map(p->new Change(p.path(),p.action())).toList();
    }
    public static Report migrate(Path root) throws IOException {return migrate(root,writes->{});}
    /** Test seam permits deterministic interruption after any file write. */
    public static Report migrate(Path root,FaultInjector fault) throws IOException {
        Objects.requireNonNull(fault);root=root.toAbsolutePath().normalize();checkRoot(root);
        Files.createDirectories(root);
        // Lock remains exclusive throughout recovery and migration.
        Path lock=safe(root,".migration.lock");
        try(FileChannel channel=FileChannel.open(lock,StandardOpenOption.CREATE,StandardOpenOption.WRITE);
            FileLock held=channel.tryLock()) {
            if(held==null)throw new IOException("Another configuration migration is running");
            boolean recovered=recover(root);
            for(String dir:DIRS){Path p=safe(root,dir);Files.createDirectories(p);}
            List<FilePlan> files=plans(root,layouts());
            List<FilePlan> pending=files.stream().filter(x->!x.action().equals("UNCHANGED")).toList();
            if(pending.isEmpty())return report(files,recovered);
            Path backupDir=safe(root,"backups/config-"+Instant.now().toEpochMilli()+"-"+UUID.randomUUID());
            Files.createDirectories(backupDir);
            // Store recovery manifest and every original BEFORE touching a managed file.
            StringBuilder manifest=new StringBuilder("ARUNIKA_CONFIG_MIGRATION_V1\n");
            for(FilePlan f:pending){
                manifest.append(f.exists()?"UPDATED":"CREATED").append('\t').append(f.path()).append('\n');
                if(f.exists()){
                    Path copy=safe(backupDir,"original/"+f.path());Files.createDirectories(copy.getParent());
                    Files.copy(safe(root,f.path()),copy,StandardCopyOption.COPY_ATTRIBUTES);
                }
            }
            atomic(backupDir.resolve("manifest.tsv"),manifest.toString().getBytes(StandardCharsets.UTF_8));
            String backupName=backupDir.getFileName().toString();
            atomic(safe(root,PENDING),(backupName+"\n").getBytes(StandardCharsets.UTF_8));
            try{
                int written=0;
                for(FilePlan f:pending){
                    Path dst=safe(root,f.path());atomic(dst,f.output());
                    fault.afterWrite(++written);
                }
                atomic(backupDir.resolve("COMPLETE"),"OK\n".getBytes(StandardCharsets.UTF_8));
                Files.deleteIfExists(safe(root,PENDING));
                prune(root,5);
                return report(files,recovered);
            }catch(Exception ex){
                try{recover(root);}catch(Exception restore){ex.addSuppressed(restore);}
                if(ex instanceof IOException io)throw io;
                throw new IOException("Config migration failed",ex);
            }
        }catch(OverlappingFileLockException ex){throw new IOException("Another migration is running in the same JVM",ex);}
    }
    private static Report report(List<FilePlan> files,boolean recovered){
        return new Report((int)files.stream().filter(f->f.action().equals("CREATED")).count(),
            (int)files.stream().filter(f->f.action().equals("UPDATED")).count(),
            (int)files.stream().filter(f->f.action().equals("UNCHANGED")).count(),recovered,
            files.stream().map(f->new Change(f.path(),f.action())).toList());
    }
    private static boolean recover(Path root) throws IOException {
        Path marker=safe(root,PENDING);if(!Files.exists(marker,LinkOption.NOFOLLOW_LINKS))return false;
        String backupName=Files.readString(marker,StandardCharsets.UTF_8).trim();
        if(!backupName.matches("config-[a-zA-Z0-9-]+"))throw new IOException("Invalid migration recovery marker");
        Path backup=safe(root,"backups/"+backupName);
        if(Files.exists(safe(backup,"COMPLETE"),LinkOption.NOFOLLOW_LINKS)){
            Files.deleteIfExists(marker);return false;
        }
        Path man=safe(backup,"manifest.tsv");if(!Files.isRegularFile(man,LinkOption.NOFOLLOW_LINKS))throw new IOException("Recovery manifest missing");
        List<String> lines=Files.readAllLines(man,StandardCharsets.UTF_8);
        if(lines.isEmpty()||!lines.getFirst().equals("ARUNIKA_CONFIG_MIGRATION_V1"))throw new IOException("Invalid migration recovery manifest");
        var allow=layouts().keySet();
        // Validate ALL paths and preimages before modifying anything.
        List<String[]> entries=new ArrayList<>();
        for(String line:lines.subList(1,lines.size())) {
            String[] fields=line.split("\\t",-1);
            if(fields.length!=2||!Set.of("UPDATED","CREATED").contains(fields[0])||!allow.contains(fields[1]))throw new IOException("Unsafe recovery entry");
            safe(root,fields[1]);
            if(fields[0].equals("UPDATED"))read(safe(backup,"original/"+fields[1]));
            entries.add(fields);
        }
        for(String[] entry:entries){
            Path to=safe(root,entry[1]);
            if(entry[0].equals("CREATED"))Files.deleteIfExists(to);
            else atomic(to,read(safe(backup,"original/"+entry[1])));
        }
        atomic(safe(backup,"ROLLED_BACK"),"Recovery completed\n".getBytes(StandardCharsets.UTF_8));
        Files.deleteIfExists(marker);return true;
    }
    private static void prune(Path root,int keep) throws IOException {
        Path backups=safe(root,"backups");
        try(var stream=Files.list(backups)){
            List<Path> completed=stream.filter(p->Files.isDirectory(p,LinkOption.NOFOLLOW_LINKS)
                &&p.getFileName().toString().startsWith("config-")
                &&Files.isRegularFile(p.resolve("COMPLETE"),LinkOption.NOFOLLOW_LINKS)).sorted(Comparator.comparing(Path::toString).reversed()).toList();
            for(Path old:completed.subList(Math.min(keep,completed.size()),completed.size())){
                // Only delete known, fully completed auto-generated backups; do not follow symlinks.
                try(var walk=Files.walk(old)){
                    for(Path file:walk.sorted(Comparator.reverseOrder()).toList()){
                        if(Files.isSymbolicLink(file))throw new IOException("Unsafe symlink in backup: "+file);
                        Files.delete(file);
                    }
                }
            }
        }
    }
}
