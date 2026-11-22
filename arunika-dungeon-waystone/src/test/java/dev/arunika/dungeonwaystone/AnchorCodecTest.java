package dev.arunika.dungeonwaystone;
import java.util.*;
import java.nio.file.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
class AnchorCodecTest {
 @TempDir Path temp;
 Node node(UUID id,Node.Role role,String target,int x){return new Node(id,role,target,UUID.fromString("00000000-0000-0000-0000-000000000002"),"dungeon_world",x,64,0);}
 @Test void oldUnnamedRegistryLoadsWithoutTouchingFile()throws Exception{
  UUID id=UUID.randomUUID();Node hub=node(id,Node.Role.HUB,"",0);var yaml=AnchorCodec.encode(Map.of(id,hub),Map.of(id,"Hub Dungeon"));yaml.set("nodes."+id+".name",null);
  Path file=temp.resolve("anchors.yml");Files.writeString(file,yaml.saveToString());byte[] before=Files.readAllBytes(file);var data=AnchorCodec.read(file.toFile());assertEquals(hub,data.nodes().get(id));assertEquals(NodeNames.fallback(hub),data.names().get(id));assertArrayEquals(before,Files.readAllBytes(file));
 }
 @Test void renamingPersistsIdentityAndDestinationUnchanged()throws Exception{
  UUID id=UUID.randomUUID();Node back=node(id,Node.Role.RETURN,"queen_bee/shared_1",10);var yaml=AnchorCodec.encode(Map.of(id,back),Map.of(id,"Return Queen Bee"));Path file=temp.resolve("anchors.yml");yaml.save(file.toFile());var data=AnchorCodec.read(file.toFile());assertEquals(back,data.nodes().get(id));assertEquals("Return Queen Bee",data.names().get(id));
 }
 @Test void corruptSchemaAndDuplicateLabelsFailClosed()throws Exception{
  var future=new YamlConfiguration();future.set("schema",2);assertThrows(java.io.IOException.class,()->AnchorCodec.decode(future));
  UUID a=UUID.randomUUID(),b=UUID.randomUUID();var hub=node(a,Node.Role.HUB,"",0);var back=node(b,Node.Role.RETURN,"queen_bee/shared_1",10);
  var y=AnchorCodec.encode(Map.of(a,hub,b,back),Map.of(a,"Hub",b,"Return"));y.set("nodes."+b+".name","HUB");var bad=y;assertThrows(java.io.IOException.class,()->AnchorCodec.decode(bad));
 }
}
