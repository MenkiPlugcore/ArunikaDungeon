package site.cadera.arunika.dungeon.battle;
import java.util.*;
import site.cadera.arunika.dungeon.core.SessionRegistry.Mode;

/** Immutable battle snapshot. All coordinates are in the same explicitly configured world. */
public record BattlePlan(String floor, String displayName, int durationSeconds, List<List<Mob>> waves, String boss, List<Arena> arenas, double minionPerExtraPlayer, double maxMultiplier) {
 public BattlePlan(String floor,String name,int duration,List<List<Mob>> waves,String boss,List<Arena> arenas){this(floor,name,duration,waves,boss,arenas,0.15,3.0);}
 public record Point(String world,double x,double y,double z) {
  public Point { Objects.requireNonNull(world); if(world.isBlank()||!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z)||Math.abs(x)>29999984||Math.abs(z)>29999984||Math.abs(y)>2048)throw new IllegalArgumentException("invalid point"); }
 }
 public record Bounds(Point min,Point max) {
  public Bounds { if(!min.world.equals(max.world)||min.x>max.x||min.y>max.y||min.z>max.z||max.x-min.x>512||max.z-min.z>512||max.y-min.y>512)throw new IllegalArgumentException("invalid arena bounds"); }
  public boolean contains(Point p){return min.world.equals(p.world)&&p.x>=min.x&&p.x<=max.x&&p.y>=min.y&&p.y<=max.y&&p.z>=min.z&&p.z<=max.z;}
  public boolean overlaps(Bounds b){return min.world.equals(b.min.world)&&min.x<=b.max.x&&max.x>=b.min.x&&min.y<=b.max.y&&max.y>=b.min.y&&min.z<=b.max.z&&max.z>=b.min.z;}
 }
 public record Arena(String id,Mode mode,Bounds bounds,Point playerSpawn,List<Point> mobSpawns,Point bossSpawn,Point exit) {
  public Arena { Objects.requireNonNull(mode);mobSpawns=List.copyOf(mobSpawns);if(!id.matches("[a-z][a-z0-9_-]{0,47}")||mobSpawns.isEmpty()||mobSpawns.size()>64||!bounds.contains(playerSpawn)||!bounds.contains(bossSpawn)||mobSpawns.stream().anyMatch(p->!bounds.contains(p))||bounds.contains(exit))throw new IllegalArgumentException("invalid arena "+id); }
 }
 public record Mob(String id,int amount){public Mob{if(id==null||id.isBlank()||amount<1||amount>1000)throw new IllegalArgumentException("invalid mob");}}
 public BattlePlan { waves=waves.stream().map(List::copyOf).toList();arenas=List.copyOf(arenas);if(!Double.isFinite(minionPerExtraPlayer)||minionPerExtraPlayer<0||minionPerExtraPlayer>1||!Double.isFinite(maxMultiplier)||maxMultiplier<1||maxMultiplier>5||floor.isBlank()||boss.isBlank()||durationSeconds<60||durationSeconds>7200||waves.isEmpty()||waves.size()>30||waves.stream().anyMatch(w->w.isEmpty()||w.stream().mapToInt(Mob::amount).sum()>5000)||arenas.isEmpty()||arenas.size()>32||arenas.stream().map(Arena::id).distinct().count()!=arenas.size()||arenas.stream().filter(a->a.mode()==Mode.SHARED).count()>1)throw new IllegalArgumentException("invalid battle plan"); }
 public int scaledAmount(Mob mob,int players){return Math.min(1000,(int)Math.ceil(mob.amount()*Math.min(maxMultiplier,1+Math.max(0,players-1)*minionPerExtraPlayer)));}
 public Set<String> mobIds(){Set<String> ids=new HashSet<>();ids.add(boss);for(var w:waves)for(var m:w)ids.add(m.id);return Set.copyOf(ids);}
 public static void validateIsolation(Collection<BattlePlan> plans){var all=plans.stream().flatMap(p->p.arenas.stream()).toList();for(int i=0;i<all.size();i++){for(int j=i+1;j<all.size();j++)if(all.get(i).bounds.overlaps(all.get(j).bounds))throw new IllegalArgumentException("overlapping arenas: "+all.get(i).id+" / "+all.get(j).id);for(var a:all)if(a.bounds.contains(all.get(i).exit))throw new IllegalArgumentException("exit inside battle arena");}}
}
