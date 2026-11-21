package site.cadera.arunika.dungeon.battle;
import java.util.*;
import java.util.random.RandomGenerator;
/** Immutable per-mode loot policy. Independent rolls; bounded stacks handled by adapter. */
public record LootPlan(int delay,int collection,int radius,Map<Integer,Table> waves,Table boss) {
 public enum Trigger { MOB_KILL,WAVE_CLEAR,BOSS_KILL }
 public enum Type { VANILLA,CUSTOM,MMOITEMS,ITEMSADDER,MYTHICMOBS,SERIALIZED }
 public record Entry(Type type,String id,String itemType,double chance,int min,int max){
  public Entry{Objects.requireNonNull(type);Objects.requireNonNull(id);Objects.requireNonNull(itemType);if(id.isBlank()||id.length()>100000||!Double.isFinite(chance)||chance<0||chance>100||min<1||max<min||max>4096)throw new IllegalArgumentException("Invalid loot entry");}
 }
 public record Roll(Entry entry,int amount){}
 public record Table(Trigger trigger,List<Entry> entries){
  public Table{Objects.requireNonNull(trigger);entries=List.copyOf(entries);if(entries.size()>128)throw new IllegalArgumentException("Max 128 entries per table");}
  public List<Roll> roll(RandomGenerator rng){List<Roll> out=new ArrayList<>();for(Entry e:entries)if(e.chance()>0&&(e.chance()==100||rng.nextDouble(100)<e.chance()))out.add(new Roll(e,rng.nextInt(e.min(),e.max()+1)));return List.copyOf(out);}
 }
 public LootPlan{waves=Map.copyOf(waves);Objects.requireNonNull(boss);if(delay<0||delay>60||collection<1||collection>300||delay>=collection||radius<0||radius>32)throw new IllegalArgumentException("Loot delay must be shorter than collection window");}
 private static final Map<Trigger,Table> EMPTY=Map.of(Trigger.MOB_KILL,new Table(Trigger.MOB_KILL,List.of()),Trigger.WAVE_CLEAR,new Table(Trigger.WAVE_CLEAR,List.of()),Trigger.BOSS_KILL,new Table(Trigger.BOSS_KILL,List.of()));
 public Table table(Trigger t,int wave){Table a=t==Trigger.BOSS_KILL?boss:waves.get(wave);return a!=null&&a.trigger()==t?a:EMPTY.get(t);}
}
