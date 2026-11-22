package site.cadera.arunika.dungeon.battle;
import java.util.*;
import site.cadera.arunika.dungeon.core.*;
import static site.cadera.arunika.dungeon.core.SessionRegistry.Mode.*;
public final class RpgTest {
 static int n;static void check(boolean b,String name){if(!b)throw new AssertionError(name);n++;System.out.println("PASS "+name);}
 static final class Port extends BattleTest.Fake {int clears; Set<UUID> eligible=Set.of(); boolean fail;
  public void bossClear(SessionRegistry.Session s,Set<UUID> alive){clears++;eligible=alive;if(fail)throw new IllegalStateException("provider unavailable");}
 }
 static void run(boolean reward,boolean fail,boolean abort,int expected){var registry=BattleTest.registry();var a=BattleTest.arena("private",PRIVATE,0);var plan=BattleTest.plan("bee",a);var p=new Port();p.fail=fail;var e=new BattleEngine(registry,p,5,5);UUID u=UUID.randomUUID(),v=UUID.randomUUID();p.present.addAll(Set.of(u,v));var s=registry.reserve("bee",PRIVATE,u,Set.of(u,v));e.start(s,plan,a,0,reward);e.eliminate(v);if(abort){e.abort(s.id(),1,"abort");check(p.clears==0,"failed run does not dispatch progression");return;}UUID boss=null;for(long t=3000;t<30000&&!e.idle();t+=250){e.tick(t,true);for(UUID mob:new ArrayList<>(p.live.keySet())){if(p.live.get(mob).equals("boss"))boss=mob;p.kill(e,mob,t);}}if(boss!=null)e.death(boss,31000);check(p.clears==expected,"callback count "+expected+" (reward="+reward+", fail="+fail+")");check(e.idle(),"session closes despite provider status");if(expected>0)check(p.eligible.equals(Set.of(u)),"eliminated player excluded from progression");}
 public static void main(String[] a){run(true,false,false,1);run(false,false,false,0);run(true,true,false,1);run(true,false,true,0);var id=UUID.randomUUID();var user=UUID.randomUUID();var event=new DungeonRpgApi.BossClear(id,"bee",PRIVATE,user,100,true);check(event.eventId().equals(id+":"+user),"stable idempotency key");check(!new RpgPolicy(false,10,true).active(),"disabled policy is inactive");System.out.println("TOTAL "+n+" PASS");}
}
