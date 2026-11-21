package site.cadera.arunika.dungeon.battle;
import java.nio.file.*;
import java.util.*;
public final class LootRecoveryTest {
 static int count;
 static void check(boolean value,String label){if(!value)throw new AssertionError(label);count++;System.out.println("PASS "+label);}
 public static void main(String[] args)throws Exception{
  Path root=Files.createTempDirectory("adg-loot-recovery");
  try{
   var store=new LootRecoveryStore(root);UUID session=UUID.randomUUID();var entry=new LootPlan.Entry(LootPlan.Type.SERIALIZED,"YWJjZA==","",100,1,4096);Path first=store.save(session,List.of(new LootRecoveryStore.Unissued(entry,127,"budget")));
   Properties data=new Properties();try(var in=Files.newInputStream(first)){data.load(in);}check(data.getProperty("session").equals(session.toString()),"loot recovery retains session attribution");check(data.getProperty("entry.0.amount").equals("127")&&data.getProperty("entry.0.id").equals(entry.id()),"unissued amount and exact serialized metadata retained");check(data.getProperty("state").equals("UNISSUED_REQUIRES_REVIEW"),"recovery explicitly requires manual review");
   Path second=store.save(session,List.of(new LootRecoveryStore.Unissued(entry,1,"location")));check(!first.equals(second)&&Files.exists(first),"multiple recovery batches do not overwrite rewards");try(var files=Files.list(root)){check(files.noneMatch(p->p.toString().endsWith(".tmp")),"no leftover temporary recovery files");}
   System.out.println("TOTAL "+count+" PASS");
  }finally{try(var paths=Files.walk(root)){for(var p:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(p);}}
 }
}
