package site.cadera.arunika.dungeon.paper;
import java.lang.reflect.*;
import java.util.UUID;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.plugin.Plugin;

/** Public ABI inspected in CdrWaystone 0.7.0 and 0.7.1. No private-field access or writes. */
final class WaystoneBridge {
 private final Plugin plugin; private final Method registry,visuals,access,guards,teleports,find,get,fromEntity,canAccess,guardStatus,isWarping,isSuppressed,id,location,isAdmin,permanent,coreActive,alwaysActive,collisionOwned,isAnchorValid,channeling,bound,hidden;
 WaystoneBridge(Plugin p)throws ReflectiveOperationException{
  plugin=p;Class<?> c=p.getClass();ClassLoader loader=c.getClassLoader();Class<?> data=Class.forName("store.menkiestes.cdrwaystone.WaystoneData",false,loader);Class<?> dungeon=Class.forName("store.menkiestes.cdrwaystone.dungeon.DungeonModule",false,loader);
  registry=c.getMethod("registry");visuals=c.getMethod("visuals");access=c.getMethod("access");guards=c.getMethod("guards");teleports=c.getMethod("teleports");
  find=registry.getReturnType().getMethod("find",Location.class);get=registry.getReturnType().getMethod("get",UUID.class);fromEntity=visuals.getReturnType().getMethod("waystoneFromEntity",Entity.class);canAccess=access.getReturnType().getMethod("canAccess",Player.class,data);guardStatus=guards.getReturnType().getMethod("status",Player.class);isWarping=teleports.getReturnType().getMethod("isWarping",Player.class);isSuppressed=teleports.getReturnType().getMethod("isSuppressed",data);isAnchorValid=visuals.getReturnType().getMethod("isAnchorValid",data);channeling=dungeon.getMethod("channeling",c,Player.class);bound=dungeon.getMethod("bound",c,data);hidden=dungeon.getMethod("hidden",c,data);
  id=data.getMethod("id");location=data.getMethod("location");isAdmin=data.getMethod("isAdmin");permanent=data.getMethod("permanent");coreActive=data.getMethod("coreActive");alwaysActive=data.getMethod("alwaysActive");collisionOwned=data.getMethod("collisionOwned");
 }
 boolean available(){return plugin.isEnabled();}
 private static Object call(Method m,Object o,Object... args){try{return m.invoke(o,args);}catch(ReflectiveOperationException e){throw new IllegalStateException("Waystone API unavailable",e);}}
 Object stone(UUID u){return call(get,call(registry,plugin),u);}
 Object stone(Block b){Object reg=call(registry,plugin),s=call(find,reg,b.getLocation());if(s==null&&b.getType()==Material.BARRIER){s=call(find,reg,b.getRelative(0,-1,0).getLocation());if(s!=null&&!Boolean.TRUE.equals(call(collisionOwned,s)))return null;}return s;}
 Object stone(Entity e){return call(fromEntity,call(visuals,plugin),e);}
 UUID id(Object s){return (UUID)call(id,s);}
 Location location(Object s){return (Location)call(location,s);}
 boolean official(Object s){return s!=null&&Boolean.TRUE.equals(call(isAdmin,s))&&Boolean.TRUE.equals(call(permanent,s));}
 boolean dedicated(Object s){return official(s)&&stone(id(s))==s&&!Boolean.TRUE.equals(call(bound,null,plugin,s))&&Boolean.TRUE.equals(call(hidden,null,plugin,s));}
 boolean allowed(Player p,Object s,double radius,boolean entry){if(!available()||!dedicated(s)||!p.isOnline()||p.isDead()||!p.hasPermission("cdrwaystone.use")||!p.hasPermission("cdrwaystone.dungeon.use")||!Boolean.TRUE.equals(call(canAccess,call(access,plugin),p,s)))return false;Location l=location(s);if(l==null||l.getWorld()==null||!p.getWorld().equals(l.getWorld())||!site.cadera.arunika.dungeon.battle.ArenaAccessPolicy.near(PaperBattlePort.point(l.clone().add(.5,.5,.5)),PaperBattlePort.point(p.getLocation()),Math.min(radius,Math.max(2,plugin.getConfig().getDouble("network.origin-radius",6.5))))||!l.getWorld().isChunkLoaded(l.getBlockX()>>4,l.getBlockZ()>>4)||!Boolean.TRUE.equals(call(isAnchorValid,call(visuals,plugin),s)))return false;
  return !entry||(!p.isFlying()&&p.getGameMode()!=GameMode.CREATIVE&&p.getGameMode()!=GameMode.SPECTATOR&&(Boolean.TRUE.equals(call(coreActive,s))||(official(s)&&plugin.getConfig().getBoolean("core.admin-bypass",true)))&&!Boolean.TRUE.equals(call(isSuppressed,call(teleports,plugin),s))&&!Boolean.TRUE.equals(call(isWarping,call(teleports,plugin),p))&&!Boolean.TRUE.equals(call(channeling,null,plugin,p))&&"READY".equals(((Enum<?>)call(guardStatus,call(guards,plugin),p)).name()));
 }
}
