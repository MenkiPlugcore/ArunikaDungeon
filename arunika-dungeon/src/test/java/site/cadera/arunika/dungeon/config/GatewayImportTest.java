package site.cadera.arunika.dungeon.config;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.util.*;
import java.io.IOException;
import site.cadera.arunika.dungeon.core.SessionRegistry.Mode;
import site.cadera.arunika.dungeon.battle.BattlePlan.*;
class GatewayImportTest {
 @TempDir Path root;
 @Test void bindingsAndAllArenaExitsCommitTogether()throws Exception{
  var codec=new SnakeYamlCodec();var cfg=new SmartConfiguration(root,codec);cfg.load();
  cfg.editBattle("queen_bee","boss","Bee",0,0,null);cfg.editBattle("queen_bee","count",null,1,0,null);cfg.editBattle("queen_bee","wave","BeeMinion",1,1,null);
  cfg.editBattle("queen_bee","arena",null,0,0,AdminConfigTest.arena("shared_1",Mode.SHARED,0));cfg.editBattle("queen_bee","enable",null,0,0,null);
  var global=codec.read(Files.readAllBytes(root.resolve("config.yml")));global.put("battle-preview",true);Files.write(root.resolve("config.yml"),codec.write(global));cfg.load();
  UUID hub=UUID.randomUUID(),back=UUID.randomUUID();
  var exported=Map.<String,Object>of("schema-version",2,"enabled",false,"gateway-id",hub.toString(),"hub",Map.of("world","world","x",-50,"y",1,"z",0),"interaction-radius",6,"return-waystones",Map.of(back.toString(),"queen_bee/shared_1"));
  var next=cfg.importGateway(exported,true);assertTrue(next.gateway().enabled());assertEquals(hub,next.gateway().hubId());assertEquals(new Point("world",-50,1,0),next.battles().get("queen_bee").arenas().getFirst().exit());
  byte[] before=Files.readAllBytes(root.resolve("integrations/waystone.yml"));
  var invalid=new HashMap<>(exported);invalid.put("hub",Map.of("world","world","x",1,"y",1,"z",1));
  assertThrows(IOException.class,()->cfg.importGateway(invalid,true));assertArrayEquals(before,Files.readAllBytes(root.resolve("integrations/waystone.yml")));
 }
}
