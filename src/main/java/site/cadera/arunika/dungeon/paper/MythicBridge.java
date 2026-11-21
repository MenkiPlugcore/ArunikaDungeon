package site.cadera.arunika.dungeon.paper;
import java.lang.reflect.*;
import java.util.*;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
/** Optional dependency via cached public API methods; no Mythic classes loaded until preview starts. */
final class MythicBridge {
 private final Object manager;private final Method getMob,adapt;private final Map<Class<?>,Method> spawnMethods=new HashMap<>();
 MythicBridge(Plugin plugin)throws ReflectiveOperationException{
  ClassLoader loader=plugin.getClass().getClassLoader();Class<?> api=Class.forName("io.lumine.mythic.bukkit.MythicBukkit",true,loader);
  Object instance=api.getMethod("inst").invoke(null);manager=api.getMethod("getMobManager").invoke(instance);
  getMob=manager.getClass().getMethod("getMythicMob",String.class);adapt=Class.forName("io.lumine.mythic.bukkit.BukkitAdapter",true,loader).getMethod("adapt",Location.class);
 }
 private Object definition(String id)throws ReflectiveOperationException{return ((Optional<?>)getMob.invoke(manager,id)).orElseThrow(()->new IllegalArgumentException("Unknown MythicMob: "+id));}
 void validate(Set<String> ids)throws ReflectiveOperationException{for(String id:ids){Object mob=definition(id);spawnMethod(mob);}}
 private Method spawnMethod(Object mob)throws NoSuchMethodException{Class<?> c=mob.getClass();Method m=spawnMethods.get(c);if(m==null){m=Arrays.stream(c.getMethods()).filter(x->x.getName().equals("spawn")&&x.getParameterCount()==2&&x.getParameterTypes()[0].isAssignableFrom(adapt.getReturnType())&&(x.getParameterTypes()[1]==double.class||x.getParameterTypes()[1]==int.class)).findFirst().orElseThrow(()->new NoSuchMethodException("MythicMob.spawn(location,level)"));spawnMethods.put(c,m);}return m;}
 Entity spawn(String id,Location at,int players)throws ReflectiveOperationException{
  Object mob=definition(id);Method m=spawnMethod(mob); // Mythic level stays 1; HP/damage scaling can be configured separately in later releases.
  Object active=m.invoke(mob,adapt.invoke(null,at),m.getParameterTypes()[1]==int.class?(Object)Integer.valueOf(1):Double.valueOf(1));
  Object abstractEntity=active.getClass().getMethod("getEntity").invoke(active);return (Entity)abstractEntity.getClass().getMethod("getBukkitEntity").invoke(abstractEntity);
 }
}
