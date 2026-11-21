package site.cadera.arunika.dungeon.config;
import java.nio.file.*;
import java.util.*;
import site.cadera.arunika.dungeon.battle.BattlePlan.*;
import site.cadera.arunika.dungeon.core.SessionRegistry.Mode;
public final class AdminConfigTest {
 static int n;
 static void check(boolean ok,String name){if(!ok)throw new AssertionError(name);n++;System.out.println("PASS "+name);}
 static Point p(int x,int y,int z){return new Point("world",x,y,z);}
 static Arena arena(String id,Mode mode,int x){return new Arena(id,mode,new Bounds(p(x,0,0),p(x+10,10,10)),p(x+1,1,1),List.of(p(x+2,1,2)),p(x+3,1,3),p(-100,1,0));}
 public static void main(String[] args)throws Exception{
  Path root=Files.createTempDirectory("adg-admin-test");try{
   var codec=args.length==0?new ConfigTest.FixtureCodec():(SmartConfiguration.Codec)Class.forName(args[0]).getConstructor().newInstance();var c=new SmartConfiguration(root,codec);var init=c.load();check(!init.rpg().active(),"RPG off by default");
   Path base=root.resolve("dungeons/01_queen_bee");Path boss=base.resolve("boss.yml");byte[] before=Files.readAllBytes(boss);
   var a=c.editBattle("queen_bee","boss","TestQueen",0,0,null);check(a.floors().getFirst().bossReference().equals("TestQueen"),"boss edit applied to snapshot");check(Arrays.equals(before,Files.readAllBytes(root.resolve("backups/"+a.backup()+"/original/dungeons/01_queen_bee/boss.yml"))),"boss original backed up");
   var w=c.editBattle("queen_bee","count",null,1,0,null);check(w.lootPlans().get("queen_bee:shared").waves().size()==1,"wave count updated");
   c.editBattle("queen_bee","wave","Minion",1,2,null);c.editBattle("queen_bee","append-wave","Elite",1,1,null);
   c.editBattle("queen_bee","arena",null,0,0,arena("shared_1",Mode.SHARED,0));var ready=c.editBattle("queen_bee","enable",null,0,0,null);check(ready.battles().get("queen_bee").waves().getFirst().size()==2,"wave append retained two mob groups");check(ready.battles().get("queen_bee").arenas().size()==1,"arena wizard shape accepted");
   Path battle=base.resolve("battle.yml");byte[] good=Files.readAllBytes(battle);try{c.editBattle("queen_bee","arena",null,0,0,arena("private_1",Mode.PRIVATE,5));throw new AssertionError("overlap");}catch(java.io.IOException expected){check(Arrays.equals(good,Files.readAllBytes(battle)),"overlap rejected without writing");}
   try{c.editBattle("queen_bee","count",null,5,0,null);throw new AssertionError("empty new waves");}catch(java.io.IOException expected){check(!Files.exists(base.resolve("shared/wave-5-drops.yml"))&&c.load().battles().get("queen_bee").waves().size()==1,"invalid enabled edit creates no templates");}
   c.editBattle("queen_bee","arena",null,0,0,arena("private_1",Mode.PRIVATE,30));check(c.load().battles().get("queen_bee").arenas().size()==2,"second isolated mode saved");
   c.editBattle("queen_bee","disable",null,0,0,null);var expanded=c.editBattle("queen_bee","count",null,4,0,null);check(expanded.lootPlans().get("queen_bee:private").waves().size()==4&&Files.exists(base.resolve("private/wave-4-drops.yml")),"disabled dungeon expands safely");
   Path rpg=root.resolve("integrations/nusa-karsa.yml");var map=codec.read(Files.readAllBytes(rpg));map.put("enabled",true);map.put("class-exp-per-boss",250);map.put("awakening-progress",true);Files.write(rpg,codec.write(map));check(c.load().rpg().active()&&c.load().rpg().classExpPerBoss()==250,"optional progression validated");map.put("class-exp-per-boss",-1);Files.write(rpg,codec.write(map));try{c.load();throw new AssertionError("negative exp");}catch(java.io.IOException expected){check(true,"negative EXP refused");}
   System.out.println("TOTAL "+n+" PASS");
  }finally{try(var paths=Files.walk(root)){for(Path p:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}}
 }
}
