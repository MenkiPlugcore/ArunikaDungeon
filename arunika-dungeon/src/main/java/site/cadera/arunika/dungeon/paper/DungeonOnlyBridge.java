package site.cadera.arunika.dungeon.paper;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

/** Only the dungeon plugin owns UI/teleport; this bridge reads the dungeon-only anchor registry.
 *  Reflection avoids a hard compile dependency on optional plugin sources. */
final class DungeonOnlyBridge {
    private final Plugin plugin;
    private final Method byId,byBlock,byEntity,loc,id,dedicated,hub,returnNode,allowed;
    DungeonOnlyBridge(Plugin plugin)throws ReflectiveOperationException{
        this.plugin=plugin;
        Class<?> api=plugin.getClass();
        Class<?> node=Class.forName("dev.arunika.dungeonwaystone.Node",false,api.getClassLoader());
        byId=api.getMethod("getNode",UUID.class);
        byBlock=api.getMethod("getNode",Block.class);
        byEntity=api.getMethod("getNode",Entity.class);
        loc=api.getMethod("location",node);
        id=api.getMethod("id",node);
        dedicated=api.getMethod("dedicated",node);
        hub=api.getMethod("isHub",node);
        returnNode=api.getMethod("isReturn",node,String.class);
        allowed=api.getMethod("allowed",Player.class,node,double.class,boolean.class);
    }
    boolean available(){return plugin.isEnabled();}
    private Object invoke(Method method,Object... args){
        if(!available())throw new IllegalStateException("DungeonWaystone not enabled");
        try{return method.invoke(plugin,args);}
        catch(IllegalAccessException|InvocationTargetException e){throw new IllegalStateException("DungeonWaystone ABI error",e);}
    }
    Object stone(UUID u){return u==null?null:invoke(byId,u);}
    Object stone(Block block){return block==null?null:invoke(byBlock,block);}
    Object stone(Entity entity){return entity==null?null:invoke(byEntity,entity);}
    UUID id(Object n){return n==null?null:(UUID)invoke(id,n);}
    Location location(Object n){return n==null?null:(Location)invoke(loc,n);}
    boolean dedicated(Object n){return n!=null&&Boolean.TRUE.equals(invoke(dedicated,n));}
    boolean official(Object n){return dedicated(n);}
    boolean isHub(Object n){return n!=null&&Boolean.TRUE.equals(invoke(hub,n));}
    boolean isReturn(Object n,String key){return n!=null&&Boolean.TRUE.equals(invoke(returnNode,n,key));}
    boolean allowed(Player p,Object n,double radius,boolean entry){return n!=null&&Boolean.TRUE.equals(invoke(allowed,p,n,radius,entry));}
}
