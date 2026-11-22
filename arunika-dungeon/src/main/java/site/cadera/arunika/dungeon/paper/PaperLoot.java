package site.cadera.arunika.dungeon.paper;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.lang.reflect.*;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import site.cadera.arunika.dungeon.battle.*;
import site.cadera.arunika.dungeon.core.SessionRegistry;
import site.cadera.arunika.dungeon.privategame.TicketLedger;

/** Main-thread physical loot. Bound entity count; never retries a reward event. */
final class PaperLoot {
 private record Drop(UUID session,Item entity,long unlock){}
 private final JavaPlugin plugin;private final Map<String,LootPlan> plans;private final boolean enabled;private final int budget;
 private final LootRecoveryStore recovery;
 private final NamespacedKey tag;private final Map<UUID,Drop> items=new HashMap<>();private final Map<UUID,Set<UUID>> bySession=new HashMap<>();
 private final java.util.function.Predicate<UUID> beforeReward;
 private final PaperTickets tickets;private final TicketLedger ledger;private ItemStack legacyToken;private boolean legacyTokenLoaded;
 PaperLoot(JavaPlugin plugin,Map<String,LootPlan> plans,boolean enabled,int budget,PaperTickets tickets,TicketLedger ledger,java.util.function.Predicate<UUID> beforeReward){this.beforeReward=beforeReward;this.plugin=plugin;this.recovery=new LootRecoveryStore(plugin.getDataFolder().toPath().resolve("data/loot-recovery"));this.plans=plans;this.enabled=enabled;this.budget=budget;this.tickets=tickets;this.ledger=ledger;tag=new NamespacedKey(plugin,"loot_session");}
 private LootPlan plan(SessionRegistry.Session s){return enabled?plans.get(s.floor()+":"+s.mode().name().toLowerCase(Locale.ROOT)):null;}
 int collection(SessionRegistry.Session s){LootPlan p=plan(s);return p==null?0:p.collection();}
 int count(){return items.size();}
 boolean tagged(Item i){return i.getPersistentDataContainer().has(tag,PersistentDataType.STRING);}
 boolean canPickup(Item item,Entity picker,BattleEngine engine){Drop d=items.get(item.getUniqueId());return d!=null&&picker instanceof Player p&&System.nanoTime()/1_000_000>=d.unlock&&engine.eligible(p.getUniqueId(),d.session)&&engine.arena(d.session).map(a->a.bounds().contains(PaperBattlePort.point(p.getLocation()))&&a.bounds().contains(PaperBattlePort.point(item.getLocation()))).orElse(false);}
 boolean emit(SessionRegistry.Session s,BattlePlan.Arena a,LootPlan.Trigger trigger,int wave,Location origin){LootPlan p=plan(s);if(p==null)return false;boolean exposed=false;List<LootRecoveryStore.Unissued> unissued=new ArrayList<>();int allowance=32;var rng=ThreadLocalRandom.current();for(var roll:p.table(trigger,wave).roll(rng)){
  if(items.size()>=budget||allowance<=0){unissued.add(new LootRecoveryStore.Unissued(roll.entry(),roll.amount(),"entity/batch budget"));continue;}
  try{var entry=roll.entry();boolean ticket=entry.type()==LootPlan.Type.CUSTOM&&entry.id().equals("dungeon_ticket");ItemStack template=ticket?null:resolve(entry);if(!ticket&&hasTicketTag(template))throw new IllegalArgumentException("Serialized/provider ticket forbidden; use CUSTOM dungeon_ticket");int remaining=roll.amount();while(remaining>0&&items.size()<budget&&allowance>0){Location at=scatter(a,origin,p.radius(),rng);if(at==null)break;int amount=ticket?1:Math.min(remaining,Math.max(1,template.getMaxStackSize()));ItemStack stack;if(!beforeReward.test(s.id()))return exposed;if(ticket){if(tickets==null)throw new IllegalStateException("Ticket provider unavailable");stack=tickets.item(ledger.issuePhysical());}else{stack=template.clone();stack.setAmount(amount);}exposed=true;Item entity=at.getWorld().dropItem(at,stack,item->{item.getPersistentDataContainer().set(tag,PersistentDataType.STRING,s.id().toString());item.setPersistent(false);item.setPickupDelay(p.delay()*20);item.setVelocity(new org.bukkit.util.Vector(0,0,0));});UUID id=entity.getUniqueId();items.put(id,new Drop(s.id(),entity,System.nanoTime()/1_000_000+p.delay()*1000L));bySession.computeIfAbsent(s.id(),k->new HashSet<>()).add(id);remaining-=amount;allowance--;}if(remaining>0)unissued.add(new LootRecoveryStore.Unissued(entry,remaining,"safe location/entity/batch budget"));
  }catch(Exception|LinkageError e){plugin.getLogger().warning("Loot entry failed; not retried: "+s.id()+" "+e.getClass().getSimpleName()+" "+e.getMessage());}
 }if(!unissued.isEmpty()){
  // Record reward exposure before compensation evidence, so a technical refund cannot duplicate its value.
  if(!beforeReward.test(s.id()))throw new IllegalStateException("Unissued loot recovery blocked by ticket ledger");
  exposed=true;try{var file=recovery.save(s.id(),unissued);plugin.getLogger().severe("Unissued loot retained for staff review (never auto-replayed): "+file.getFileName()+"; batches="+unissued.size());}catch(java.io.IOException e){throw new IllegalStateException("Cannot persist unissued loot: "+s.id(),e);}
 }return exposed;}
 private static Location scatter(BattlePlan.Arena a,Location origin,int radius,Random rng){Location fallback=origin==null?PaperBattlePort.location(a.bossSpawn()):origin.clone();for(int i=0;i<9;i++){Location at=fallback.clone();if(i<8&&radius>0){double angle=rng.nextDouble()*Math.PI*2,r=Math.sqrt(rng.nextDouble())*radius;at.add(Math.cos(angle)*r,0,Math.sin(angle)*r);}if(a.bounds().contains(PaperBattlePort.point(at))&&PaperBattlePort.loaded(at)&&at.getBlock().isPassable())return at;}return null;}
 static boolean hasTicketTag(ItemStack item){return item!=null&&item.hasItemMeta()&&item.getItemMeta().getPersistentDataContainer().getKeys().stream().anyMatch(k->k.getKey().equals("dungeon_ticket_serial")||k.getKey().equals("dungeon_ticket_generation"));}
 private ItemStack resolve(LootPlan.Entry e)throws Exception{ItemStack result=switch(e.type()){
  case VANILLA -> new ItemStack(Material.valueOf(e.id().toUpperCase(Locale.ROOT)));
  case SERIALIZED -> ItemStack.deserializeBytes(Base64.getDecoder().decode(e.id()));
  case CUSTOM -> {if(!e.id().equals("dungeon_token"))throw new IllegalArgumentException("Unknown CUSTOM id; use SERIALIZED or provider type: "+e.id());if(plugin.getServer().getPluginManager().getPlugin("ArunikaDungeonExchange")==null){ItemStack current=plugin instanceof ArunikaDungeonPlugin adg?adg.exchangeTokenTemplate():null;if(current==null)throw new IllegalArgumentException("Unified Exchange token not loaded");yield current;}if(!legacyTokenLoaded){legacyTokenLoaded=true;var legacy=new java.io.File(plugin.getDataFolder().getParentFile(),"ArunikaDungeonExchange/config.yml");String template=org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(legacy).getString("token.template-base64");if(template!=null&&!template.isBlank())legacyToken=ItemStack.deserializeBytes(Base64.getDecoder().decode(template));}if(legacyToken==null)throw new IllegalArgumentException("Legacy token template missing; use addhand");yield legacyToken.clone();}
  case ITEMSADDER -> {Class<?> api=api("ItemsAdder","dev.lone.itemsadder.api.CustomStack");Object obj=api.getMethod("getInstance",String.class).invoke(null,e.id());if(obj==null)throw new IllegalArgumentException("Unknown ItemsAdder id");yield (ItemStack)obj.getClass().getMethod("getItemStack").invoke(obj);}
  case MYTHICMOBS -> {Class<?> api=api("MythicMobs","io.lumine.mythic.bukkit.MythicBukkit");Object inst=api.getMethod("inst").invoke(null),manager=inst.getClass().getMethod("getItemManager").invoke(inst);yield (ItemStack)manager.getClass().getMethod("getItemStack",String.class).invoke(manager,e.id());}
  case MMOITEMS -> {Class<?> api=api("MMOItems","net.Indyuce.mmoitems.MMOItems");Object inst=api.getField("plugin").get(null),types=api.getMethod("getTypes").invoke(inst),type=types.getClass().getMethod("get",String.class).invoke(types,e.itemType());if(type==null)throw new IllegalArgumentException("Unknown MMOItems type");Method method=Arrays.stream(api.getMethods()).filter(m->m.getName().equals("getItem")&&m.getParameterCount()==2&&m.getParameterTypes()[0].isInstance(type)&&m.getParameterTypes()[1]==String.class).findFirst().orElseThrow();yield (ItemStack)method.invoke(inst,type,e.id());}
 };if(result==null||result.getType().isAir()||!result.getType().isItem())throw new IllegalArgumentException("Invalid loot item");return result;}
 private Class<?> api(String name,String cls)throws Exception{var p=Bukkit.getPluginManager().getPlugin(name);if(p==null||!p.isEnabled())throw new IllegalStateException(name+" unavailable");return Class.forName(cls,true,p.getClass().getClassLoader());}
 void maintenance(BattleEngine engine){for(var it=items.entrySet().iterator();it.hasNext();){var e=it.next();Drop d=e.getValue();if(!d.entity.isValid()||engine.arena(d.session).map(a->!a.bounds().contains(PaperBattlePort.point(d.entity.getLocation()))).orElse(true)){if(d.entity.isValid())d.entity.remove();UUID id=e.getKey();it.remove();var set=bySession.get(d.session);if(set!=null){set.remove(id);if(set.isEmpty())bySession.remove(d.session);}}}}
 private void forget(UUID id,Drop d){items.remove(id);var set=bySession.get(d.session);if(set!=null){set.remove(id);if(set.isEmpty())bySession.remove(d.session);}}
 void cleanup(UUID session){var ids=bySession.remove(session);if(ids!=null)for(UUID id:ids){Drop d=items.remove(id);if(d!=null)d.entity.remove();}}
}
