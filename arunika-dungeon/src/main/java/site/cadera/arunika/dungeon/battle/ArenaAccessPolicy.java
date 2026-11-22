package site.cadera.arunika.dungeon.battle;
import java.util.*;
import static site.cadera.arunika.dungeon.battle.BattlePlan.*;
/** Pure policy for configured/active arenas; a permit is exact, not a broad bypass. */
public final class ArenaAccessPolicy {
 public record Active(Bounds bounds,boolean livingMember){}
 private ArenaAccessPolicy(){}
 public static boolean near(Point a,Point b,double radius){if(a==null||b==null||!a.world().equals(b.world())||!Double.isFinite(radius)||radius<0)return false;double x=a.x()-b.x(),y=a.y()-b.y(),z=a.z()-b.z();return x*x+y*y+z*z<=radius*radius;}
 public static boolean permitted(Point dest,boolean configured,Collection<Active> active){boolean memberInside=false;for(Active a:active){boolean inside=a.bounds().contains(dest);if(inside!=a.livingMember())return false;if(inside)memberInside=true;}return !configured||memberInside;}
 public static boolean permitMatches(Point permit,Point destination){return permit!=null&&permit.equals(destination);}
 public static boolean returnMatches(String binding,String floor,String arena,Bounds bounds,Point stone,Point player,boolean living){return living&&binding.equals(floor+"/"+arena)&&bounds.contains(stone)&&bounds.contains(player);}
}
