package site.cadera.arunika.dungeon.config;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.util.*;
import site.cadera.arunika.dungeon.core.SessionRegistry.Mode;
import site.cadera.arunika.dungeon.battle.BattlePlan.*;
class GuiSetupTest {
 @TempDir Path root;
 @Test void modeToggleAndSpawnCaptureKeepExistingArenaData()throws Exception{
  var codec=new SnakeYamlCodec();var config=new SmartConfiguration(root,codec);config.load();
  config.editBattle("queen_bee","boss","QueenBee",0,0,null);config.editBattle("queen_bee","count",null,1,0,null);config.editBattle("queen_bee","wave","Bee",1,1,null);
  Arena old=AdminConfigTest.arena("shared_1",Mode.SHARED,0);config.editBattle("queen_bee","arena",null,0,0,old);config.editBattle("queen_bee","enable",null,0,0,null);
  var enabled=config.setModes(true);assertTrue(enabled.battlePreview()&&enabled.sharedPreview()&&enabled.privatePreview()&&enabled.lootPreview());
  assertTrue(codec.read(Files.readAllBytes(root.resolve("config.yml"))).get("foundation-only").equals(true));
  Point target=new Point("world",5,1,5);Arena moved=new Arena(old.id(),old.mode(),old.bounds(),target,old.mobSpawns(),old.bossSpawn(),old.exit());
  var updated=config.editBattle("queen_bee","arena",null,0,0,moved).battles().get("queen_bee").arenas().getFirst();assertEquals(target,updated.playerSpawn());assertEquals(old.bounds(),updated.bounds());assertEquals(old.bossSpawn(),updated.bossSpawn());assertEquals(old.exit(),updated.exit());
  assertThrows(IllegalArgumentException.class,()->new Arena(old.id(),old.mode(),old.bounds(),new Point("otherworld",5,1,5),old.mobSpawns(),old.bossSpawn(),old.exit()));
  var off=config.setModes(false);assertFalse(off.battlePreview()||off.sharedPreview()||off.privatePreview()||off.lootPreview());assertEquals("QueenBee",off.battles().get("queen_bee").boss());
 }
}
