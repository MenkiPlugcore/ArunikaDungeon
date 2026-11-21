package site.cadera.arunika.dungeon.shared;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

/** Small atomic snapshot, written only on a boss defeat, never each tick. */
public final class CooldownStore {
 private final Path file;
 public CooldownStore(Path file){this.file=file.toAbsolutePath().normalize();}
 public Map<String,Instant> load() throws IOException {
  if(Files.isSymbolicLink(file)||Files.isSymbolicLink(file.getParent()))throw new IOException("Cooldown symlink refused");
  if(Files.notExists(file))return Map.of();
  if(!Files.isRegularFile(file)||Files.size(file)>256_000)throw new IOException("Invalid cooldown file");
  var lines=Files.readAllLines(file);if(lines.isEmpty()||!lines.getFirst().equals("ARUNIKA-COOLDOWNS-1"))throw new IOException("Invalid cooldown header");
  Map<String,Instant> map=new HashMap<>();
  try{for(String line:lines.subList(1,lines.size())){String[] p=line.split("=",-1);if(p.length!=2||!p[0].matches("[a-z][a-z0-9_]{0,47}")||map.putIfAbsent(p[0],Instant.ofEpochMilli(Long.parseLong(p[1])))!=null)throw new IllegalArgumentException("Invalid/duplicate cooldown");}}catch(RuntimeException e){throw new IOException("Invalid cooldown data",e);}return Map.copyOf(map);
 }
 public void save(Map<String,Instant> values)throws IOException{
  if(Files.isSymbolicLink(file)||Files.isSymbolicLink(file.getParent()))throw new IOException("Cooldown symlink refused");
  Files.createDirectories(file.getParent());Path tmp=Files.createTempFile(file.getParent(),"cooldowns-",".tmp");
  try{var lines=new ArrayList<String>();lines.add("ARUNIKA-COOLDOWNS-1");for(var e:new TreeMap<>(values).entrySet()){if(!e.getKey().matches("[a-z][a-z0-9_]{0,47}"))throw new IOException("Invalid floor ID");lines.add(e.getKey()+"="+e.getValue().toEpochMilli());}Files.write(tmp,lines);try(var c=FileChannel.open(tmp,StandardOpenOption.WRITE)){c.force(true);}Files.move(tmp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
   // Directory fsync is supported on typical Linux hosts; Windows may refuse it.
   try(var c=FileChannel.open(file.getParent(),StandardOpenOption.READ)){c.force(true);}catch(IOException|UnsupportedOperationException ignored){}
  }finally{Files.deleteIfExists(tmp);}
 }
}
