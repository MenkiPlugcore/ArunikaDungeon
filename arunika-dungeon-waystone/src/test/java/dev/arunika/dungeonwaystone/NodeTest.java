package dev.arunika.dungeonwaystone;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.UUID;
class NodeTest {
 @Test void nodeIdentityAndRoleAreValidated(){
  UUID id=UUID.randomUUID(),world=UUID.randomUUID();
  var hub=new Node(id,Node.Role.HUB,"",world,"hub",1,64,1);assertEquals(id,hub.id());
  assertThrows(IllegalArgumentException.class,()->new Node(id,Node.Role.HUB,"boss/ice",world,"hub",1,64,1));
  assertThrows(IllegalArgumentException.class,()->new Node(id,Node.Role.RETURN,"../../data",world,"hub",1,64,1));
  assertThrows(IllegalArgumentException.class,()->new Node(null,Node.Role.HUB,"",world,"hub",1,64,1));
  assertEquals("warden/ice",new Node(id,Node.Role.RETURN,"warden/ice",world,"arena",1,64,1).target());
 }
}
