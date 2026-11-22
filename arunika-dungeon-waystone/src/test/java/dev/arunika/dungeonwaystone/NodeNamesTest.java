package dev.arunika.dungeonwaystone;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
class NodeNamesTest {
 @Test void normalizesLabelsAndRejectsUnsafeNames(){
  assertEquals("Hub Dungeon",NodeNames.clean("  Hub   Dungeon  "));assertEquals("hub dungeon",NodeNames.key("Hub Dungeon"));
  for(String s:List.of("", "§aAdmin", "a\nb", "../../data", "Hub/warp", "x".repeat(41),"a\u200bb"))assertThrows(IllegalArgumentException.class,()->NodeNames.clean(s),s);
 }
 @Test void namesAreUniqueButRenameDoesNotChangeUuid(){
  UUID id=UUID.randomUUID();Map<UUID,String> values=Map.of(id,"Hub Dungeon");
  NodeNames.requireUnique(values,id,"HUB DUNGEON");assertThrows(IllegalArgumentException.class,()->NodeNames.requireUnique(values,UUID.randomUUID(),"hub dungeon"));
  Node n=new Node(id,Node.Role.RETURN,"queen_bee/shared_1",UUID.randomUUID(),"world",0,64,0);assertTrue(NodeNames.fallback(n).startsWith("RETURN-"));assertEquals(id,n.id());
 }
}
