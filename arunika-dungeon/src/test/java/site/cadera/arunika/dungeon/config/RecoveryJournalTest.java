package site.cadera.arunika.dungeon.config;
import java.nio.file.*;
import java.util.*;
/** Corrupt late entries must not cause partial restoration of earlier files. */
public final class RecoveryJournalTest {
 static int count;
 static void check(boolean value,String label){if(!value)throw new AssertionError(label);count++;System.out.println("PASS "+label);}
 static void refused(ConfigTransaction tx)throws Exception{try{tx.locked(()->null);throw new AssertionError("corrupt journal accepted");}catch(java.io.IOException expected){}}
 public static void main(String[] args)throws Exception{
  Path root=Files.createTempDirectory("adg-recovery-gate");
  try{
   var tx=new ConfigTransaction(root);String id="auto-"+UUID.randomUUID();Path original=root.resolve("backups/"+id+"/original");Files.createDirectories(original);Files.writeString(original.resolve("config.yml"),"original");Files.writeString(root.resolve("config.yml"),"live");Files.writeString(root.resolve(".config-pending"),id);Path manifest=original.getParent().resolve("manifest.txt");
   Files.writeString(manifest,"1 config.yml\n1 messages.yml");refused(tx);check(Files.readString(root.resolve("config.yml")).equals("live"),"missing late preimage cannot partially restore config");
   Files.writeString(manifest,"1 config.yml\n0 ../outside.yml");refused(tx);check(Files.readString(root.resolve("config.yml")).equals("live"),"late traversal cannot partially restore config");
   Files.writeString(manifest,"1 config.yml\n0 config.yml");refused(tx);check(Files.readString(root.resolve("config.yml")).equals("live"),"duplicate restore/delete target rejected atomically");
   Files.writeString(manifest,"1 config.yml");tx.locked(()->null);check(Files.readString(root.resolve("config.yml")).equals("original"),"valid recovery still restores preimage");check(Files.notExists(root.resolve(".config-pending")),"valid recovery clears marker");
   System.out.println("TOTAL "+count+" PASS");
  }finally{try(var paths=Files.walk(root)){for(var p:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(p);}}
 }
}
