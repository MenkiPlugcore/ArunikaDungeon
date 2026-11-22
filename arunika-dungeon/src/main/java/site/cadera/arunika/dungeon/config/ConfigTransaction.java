package site.cadera.arunika.dungeon.config;

import java.io.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.util.*;
import static java.nio.file.StandardCopyOption.*;

/** Recoverable multi-file update. No writes to legacy plugin directories. */
public final class ConfigTransaction {
    private final Path root;
    public ConfigTransaction(Path root) throws IOException { this.root=root.toAbsolutePath().normalize(); Files.createDirectories(this.root); if(Files.isSymbolicLink(this.root))throw new IOException("Symlink root"); }
    public interface Work<T>{T run() throws IOException;}
    public <T> T locked(Work<T> work)throws IOException{
        try(var channel=FileChannel.open(path(".config.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);var lock=channel.tryLock()){
            if(lock==null)throw new IOException("Configuration is locked"); recover();return work.run();
        }catch(OverlappingFileLockException e){throw new IOException("Configuration is locked",e);}
    }
    Path path(String rel)throws IOException{
        Path p=root.resolve(rel).normalize();if(!p.startsWith(root)||p.equals(root))throw new IOException("Invalid path: "+rel);
        for(Path x=p;!x.equals(root);x=x.getParent())if(Files.isSymbolicLink(x))throw new IOException("Symlink refused: "+x);
        return p;
    }
    static void atomic(Path p,byte[] bytes)throws IOException{
        Files.createDirectories(p.getParent());Path tmp=Files.createTempFile(p.getParent(),".adg-",".tmp");
        try{Files.write(tmp,bytes);try(var ch=FileChannel.open(tmp,StandardOpenOption.WRITE)){ch.force(true);}Files.move(tmp,p,ATOMIC_MOVE,REPLACE_EXISTING);try(var dir=FileChannel.open(p.getParent(),StandardOpenOption.READ)){dir.force(true);}catch(IOException|UnsupportedOperationException ignored){}}finally{Files.deleteIfExists(tmp);}
    }
    void recover()throws IOException{
        Path marker=path(".config-pending");if(!Files.exists(marker))return;
        String id=Files.readString(marker).strip();if(!id.matches("auto-[a-f0-9-]{36}"))throw new IOException("Invalid migration journal");
        Path backup=path("backups/"+id);List<String> lines=Files.readAllLines(path("backups/"+id+"/manifest.txt"));
        if(lines.isEmpty()||lines.size()>20000)throw new IOException("Invalid migration manifest size");
        Set<String> seen=new HashSet<>();Map<Path,byte[]> originals=new LinkedHashMap<>();List<Path> created=new ArrayList<>();
        for(String line:lines){
            if(line.length()<3||line.charAt(1)!=' '||"01".indexOf(line.charAt(0))<0)throw new IOException("Invalid migration manifest");
            String rel=line.substring(2);if(!managed(rel)||!seen.add(rel))throw new IOException("Invalid/duplicate manifest path");Path destination=path(rel);
            if(line.charAt(0)=='1'){Path original=path("backups/"+id+"/original/"+rel);if(!Files.isRegularFile(original,LinkOption.NOFOLLOW_LINKS)||Files.size(original)>2_000_000)throw new IOException("Invalid recovery preimage");originals.put(destination,Files.readAllBytes(original));}
            else created.add(destination);
        }
        for(var entry:originals.entrySet())atomic(entry.getKey(),entry.getValue());
        for(Path destination:created)Files.deleteIfExists(destination);
        Files.delete(marker);
    }
    static boolean managed(String rel){return rel.matches("[a-zA-Z0-9_/-]+\\.yml")&&!rel.contains("..")&&!rel.startsWith("/")&&(rel.startsWith("dungeons/")||rel.startsWith("integrations/")||rel.startsWith("exchange/")||!rel.contains("/"));}
    /** Called under locked(). Validate all files before invoking. Returns retained backup ID. */
    interface Writer {void write(Path path,byte[] bytes)throws IOException;}
    String commit(Map<String,byte[]> changes)throws IOException{return commit(changes,ConfigTransaction::atomic);}
    String commit(Map<String,byte[]> changes,Writer writer)throws IOException{
        if(changes.isEmpty())return "none";
        String id="auto-"+UUID.randomUUID();Path backup=path("backups/"+id);Files.createDirectories(backup);List<String> manifest=new ArrayList<>();
        for(var e:changes.entrySet()) {String rel=e.getKey();if(!managed(rel))throw new IOException("Unmanaged file");Path p=path(rel);boolean exists=Files.exists(p);if(exists)atomic(path("backups/"+id+"/original/"+rel),Files.readAllBytes(p));manifest.add((exists?"1 ":"0 ")+rel);}
        atomic(backup.resolve("manifest.txt"),String.join("\n",manifest).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        atomic(path(".config-pending"),id.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        try{for(var e:changes.entrySet())writer.write(path(e.getKey()),e.getValue());Files.delete(path(".config-pending"));}catch(IOException|RuntimeException e){try{recover();}catch(IOException rollback){e.addSuppressed(rollback);}throw e;}
        return id;
    }
    void prune(int retain)throws IOException{
        Path b=path("backups");if(!Files.isDirectory(b))return;
        List<Path> dirs;try(var stream=Files.list(b)){dirs=stream.filter(p->p.getFileName().toString().matches("auto-[a-f0-9-]{36}")&&Files.isDirectory(p,LinkOption.NOFOLLOW_LINKS)&&Files.isRegularFile(p.resolve("manifest.txt"),LinkOption.NOFOLLOW_LINKS)).sorted(Comparator.comparingLong((Path p)->p.toFile().lastModified()).reversed()).toList();}
        for(Path p:dirs.subList(Math.min(retain,dirs.size()),dirs.size()))try(var walk=Files.walk(p)){for(Path x:walk.sorted(Comparator.reverseOrder()).toList())Files.delete(x);}
    }
}
