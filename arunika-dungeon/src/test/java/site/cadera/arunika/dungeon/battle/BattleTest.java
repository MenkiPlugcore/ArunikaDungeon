package site.cadera.arunika.dungeon.battle;
import java.time.*;
import java.util.*;
import site.cadera.arunika.dungeon.core.SessionRegistry;
import static site.cadera.arunika.dungeon.core.SessionRegistry.Mode.*;
import static site.cadera.arunika.dungeon.battle.BattlePlan.*;

public final class BattleTest {
 static int tests;
 static void check(boolean b,String text){if(!b)throw new AssertionError(text);tests++;System.out.println("PASS "+text);}
 static void reject(Runnable r,String text){try{r.run();}catch(IllegalArgumentException|IllegalStateException e){check(true,text);return;}throw new AssertionError(text);}
 static Point p(double x,double y,double z){return new Point("preview",x,y,z);}
 static Arena arena(String id,SessionRegistry.Mode mode,int x){return new Arena(id,mode,new Bounds(p(x,0,0),p(x+20,20,20)),p(x+1,1,1),List.of(p(x+6,1,6),p(x+10,1,10)),p(x+15,1,15),p(-100,1,0));}
 static BattlePlan plan(String floor,Arena... a){return new BattlePlan(floor,floor,60,List.of(List.of(new Mob("minion_a",3)),List.of(new Mob("elite",1))),"boss",List.of(a),0,3);}
 static class Fake implements BattleEngine.Port {
  Map<UUID,String> live=new LinkedHashMap<>();Map<UUID,UUID> session=new HashMap<>();Set<UUID> present=new HashSet<>();List<BattleEngine.View> finished=new ArrayList<>();boolean fail;
  public UUID spawn(UUID s,String id,Point p,boolean boss,int players){if(fail)throw new IllegalStateException("spawn failure");UUID u=UUID.randomUUID();live.put(u,id);session.put(u,s);return u;}
  public boolean alive(UUID u){return live.containsKey(u);}public void remove(UUID u){live.remove(u);session.remove(u);}public boolean present(UUID p,Arena a){return present.contains(p);}
  public void update(BattleEngine.View v,Set<UUID> players){} public void finish(BattleEngine.View v,Set<UUID> players,Arena a){finished.add(v);}
  void kill(BattleEngine e,UUID u,long t){remove(u);e.death(u,t);}
  void killAll(BattleEngine e,long t){for(UUID u:new ArrayList<>(live.keySet()))kill(e,u,t);}
 }
 static SessionRegistry registry(){return new SessionRegistry(Set.of("bee","warden"),5,3,Duration.ofMinutes(30),Duration.ofMinutes(1),Clock.systemUTC());}
 public static void main(String[] args){
  var shared=arena("shared",SHARED,0);var priv=arena("private",PRIVATE,30);var other=arena("other",SHARED,60);var plan=plan("bee",shared,priv);var warden=plan("warden",other);
  BattlePlan.validateIsolation(List.of(plan,warden));check(true,"non-overlapping arena layouts accepted");reject(()->BattlePlan.validateIsolation(List.of(plan,plan("warden",arena("overlap",SHARED,10)))),"overlapping arena rejected");reject(()->new Arena("bad",SHARED,shared.bounds(),p(200,1,1),shared.mobSpawns(),shared.bossSpawn(),shared.exit()),"out-of-region spawn refused");
  var r=registry();var f=new Fake();var e=new BattleEngine(r,f,2,2);UUID player=UUID.randomUUID();f.present.add(player);var s=r.reserve("bee",SHARED,player,Set.of(player));e.start(s,plan,shared,0);check(e.permits(player,shared.playerSpawn(),true),"member permitted only in own arena");check(!e.permits(player,priv.playerSpawn(),true)&&!e.permits(player,shared.exit(),false),"living participant cannot escape or jump arenas");check(!e.permits(UUID.randomUUID(),shared.playerSpawn(),true)&&!e.permits(UUID.randomUUID(),priv.playerSpawn(),true),"outsiders denied active and configured empty arena");check(e.permits(UUID.randomUUID(),shared.exit(),false),"survival movement outside arena allowed");e.tick(2000,true);check(f.live.isEmpty(),"preparation delay");e.tick(3000,false);check(f.live.isEmpty(),"performance guard pauses spawn");
  e.tick(3250,true);e.tick(3500,true);e.tick(3750,true);check(f.live.size()==2&&e.activeEntities()==2,"global tracked-entity budget blocks excess");check(e.views(3750).getFirst().pending()==1,"cached pending count after two spawns");var first=f.live.keySet().iterator().next();f.kill(e,first,4000);e.death(first,4000);e.tick(4000,true);check(f.live.size()==2&&e.views(4000).getFirst().wave()==1,"duplicate death ignored and pending minion replenished");f.killAll(e,4250);e.tick(4250,true);check(f.live.values().equals(List.of("elite"))||f.live.size()==1&&f.live.containsValue("elite"),"wave two only after pending and live mobs cleared");f.killAll(e,4500);e.tick(4500,true);check(f.live.size()==1&&f.live.containsValue("boss"),"boss spawns only after last wave");check(r.remaining("bee").isZero(),"no cooldown before boss kill");var boss=f.live.keySet().iterator().next();f.kill(e,boss,4750);e.death(boss,4750);check(e.idle()&&f.finished.size()==1&&f.finished.getFirst().phase()==BattleEngine.Phase.COMPLETED,"boss death completes exactly once");check(r.remaining("bee").toSeconds()>=1799&&r.remaining("warden").isZero(),"shared cooldown isolated by floor");check(r.participant(player).isEmpty()&&e.activeEntities()==0,"participant and entity index cleaned");var r2=registry();r2.restoreCooldowns(r.cooldownSnapshot());check(r2.remaining("bee").toSeconds()>=1799,"cooldown survives registry replacement");
  s=r.reserve("bee",PRIVATE,player,Set.of(player));e.start(s,plan,priv,5000);e.tick(8000,true);UUID vanished=f.live.keySet().iterator().next();f.remove(vanished);e.tick(8250,true);check(e.idle()&&f.finished.getLast().phase()==BattleEngine.Phase.FAILED,"despawn aborts rather than pretending death");
  s=r.reserve("bee",PRIVATE,player,Set.of(player));e.start(s,plan,priv,9000);f.fail=true;e.tick(12000,true);f.fail=false;check(e.idle()&&r.participant(player).isEmpty(),"spawn failure cleans reservation");
  s=r.reserve("bee",PRIVATE,player,Set.of(player));e.start(s,plan,priv,13000);e.tick(16000,true);e.eliminate(player);check(!e.permits(player,priv.playerSpawn(),true)&&e.permits(player,priv.exit(),false),"eliminated player can exit but cannot reenter");e.tick(16250,true);check(e.idle()&&f.live.isEmpty(),"all eliminated abort and cleanup");
  s=r.reserve("bee",PRIVATE,player,Set.of(player));e.start(s,plan,priv,17000);e.tick(20000,true);e.tick(77000,true);check(e.idle()&&f.finished.getLast().reason().equals("Time expired"),"timeout cleanup");
  UUID p2=UUID.randomUUID();f.present.add(p2);s=r.reserve("bee",PRIVATE,player,Set.of(player));var t=r.reserve("warden",SHARED,p2,Set.of(p2));e.start(s,plan,priv,80000);e.start(t,warden,other,80000);e.tick(83000,true);check(f.live.size()==2&&new HashSet<>(f.session.values()).size()==2,"simultaneous modes share global budget fairly");check(e.owner(f.live.keySet().iterator().next()).isPresent(),"entity ownership indexed");e.abort(s.id(),83250,"manual stop");check(e.views(83250).size()==1&&f.live.size()==1,"abort affects only matching session");e.abortAll(83500);check(e.idle()&&f.live.isEmpty()&&r.snapshot().isEmpty(),"shutdown cleans all sessions");
  var scaled=new BattlePlan("bee","bee",60,plan.waves(),"boss",List.of(shared));check(scaled.scaledAmount(new Mob("x",10),1)==10&&scaled.scaledAmount(new Mob("x",10),100)==30,"participant spawn scaling bounded");
  // high-player-count standalone simulation, not a Paper stress benchmark
  var raid=new HashSet<UUID>();for(int i=0;i<101;i++)raid.add(UUID.randomUUID());var rr=registry();var ff=new Fake();ff.present.addAll(raid);var ee=new BattleEngine(rr,ff,10,5);var raidSession=rr.reserve("warden",SHARED,raid.iterator().next(),raid);ee.start(raidSession,warden,other,0);ee.tick(3000,true);check(ee.views(3000).size()==1&&raidSession.participants().size()==101,"101 participants accepted by pure engine");ee.abortAll(4000);
  System.out.println("TOTAL "+tests+" PASS");
 }
}
