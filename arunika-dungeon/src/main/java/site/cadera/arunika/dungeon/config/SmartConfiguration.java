package site.cadera.arunika.dungeon.config;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import site.cadera.arunika.dungeon.core.DungeonCatalog;
import site.cadera.arunika.dungeon.core.RpgPolicy;
import site.cadera.arunika.dungeon.core.SessionRegistry.Mode;
import site.cadera.arunika.dungeon.battle.BattlePlan;
import site.cadera.arunika.dungeon.battle.LootPlan;
import static site.cadera.arunika.dungeon.battle.BattlePlan.*;

/** Schema-aware configuration engine, independent of Bukkit. Gameplay is never activated here. */
public final class SmartConfiguration {
    public interface Codec { Map<String,Object> read(byte[] bytes)throws IOException; byte[] write(Map<String,Object> data)throws IOException; }
    public record Snapshot(List<DungeonCatalog.Floor> floors,int privatePlayers,int privateSlots,int reservationSeconds,int intervalTicks,int changedFiles,String backup,String warning, boolean battlePreview, int battleTicks, int mobBudget, int spawnBatch, double minTps, double maxMspt, Map<String,BattlePlan> battles, boolean sharedPreview, Map<String,Integer> sharedCountdowns, boolean privatePreview, Map<String,Integer> privateLimits, String ticketMaterial, String ticketName,boolean lootPreview,Map<String,LootPlan> lootPlans,int lootBudget,Gateway gateway,RpgPolicy rpg){public Snapshot{floors=List.copyOf(floors);battles=Map.copyOf(battles);sharedCountdowns=Map.copyOf(sharedCountdowns);privateLimits=Map.copyOf(privateLimits);lootPlans=Map.copyOf(lootPlans);}}
    public record Gateway(boolean enabled,UUID hubId,Point hub,double radius,Map<UUID,String> returns){public Gateway{returns=Map.copyOf(returns);}}
    private final ConfigTransaction tx;private final Codec codec;
    private static final int SCHEMA=2;
    public SmartConfiguration(Path root,Codec codec)throws IOException{tx=new ConfigTransaction(root);this.codec=codec;}
    static Map<String,Object> m(Object... args){Map<String,Object> result=new LinkedHashMap<>();for(int i=0;i<args.length;i+=2)result.put((String)args[i],args[i+1]);return result;}
    static Map<String,Object> section(Map<String,Object> map,String key)throws IOException{Object x=map.get(key);if(!(x instanceof Map<?,?> a))throw new IOException(key+" must be mapping");Map<String,Object> result=new LinkedHashMap<>();for(var e:a.entrySet()){if(!(e.getKey() instanceof String k))throw new IOException("Non-string key");result.put(k,e.getValue());}return result;}
    static int integer(Map<String,Object> map,String key,int min,int max)throws IOException{Object x=map.get(key);if(!(x instanceof Number n)||!Double.isFinite(n.doubleValue())||n.doubleValue()!=n.longValue()||n.longValue()<min||n.longValue()>max)throw new IOException(key+" must be integer "+min+".."+max);return n.intValue();}
    static String str(Map<String,Object> map,String k)throws IOException{if(!(map.get(k) instanceof String s))throw new IOException(k+" must be string");return s;}
    static boolean bool(Map<String,Object> map,String k)throws IOException{if(!(map.get(k) instanceof Boolean b))throw new IOException(k+" must be boolean");return b;}
    static void merge(Map<String,Object> target,Map<String,Object> defaults)throws IOException{for(var e:defaults.entrySet()){if(!target.containsKey(e.getKey()))target.put(e.getKey(),e.getValue());else if(e.getValue() instanceof Map<?,?>){var child=section(target,e.getKey());merge(child,section(defaults,e.getKey()));target.put(e.getKey(),child);}}}
    private Map<String,Object> file(String rel,Map<String,Object> defaults,Map<String,byte[]> plan)throws IOException{
        Path p=tx.path(rel);boolean exists=Files.exists(p);Map<String,Object> current=new LinkedHashMap<>();byte[] original=null;
        try{if(exists){if(!Files.isRegularFile(p)||Files.size(p)>2_000_000)throw new IOException("Invalid file or size");original=plan.containsKey(rel)?plan.get(rel):Files.readAllBytes(p);current=codec.read(original);if(current==null)throw new IOException("Empty YAML");}
            var before=new LinkedHashMap<>(current);if(current.containsKey("schema-version"))integer(current,"schema-version",1,SCHEMA);
            current.put("schema-version",SCHEMA);merge(current,defaults);
            if(!exists||!current.equals(before))plan.put(rel,codec.write(current));return current;
        }catch(IOException|RuntimeException e){throw new IOException(rel+": "+e.getMessage(),e);}
    }
    public Snapshot load()throws IOException{return tx.locked(()->build(null,0,null,null));}
    public Snapshot create(String id,int number,String name)throws IOException{if(!id.matches("[a-z][a-z0-9_]{0,47}")||number<1||number>999||name.isBlank())throw new IOException("Invalid dungeon ID/floor/name");return tx.locked(()->build(id,number,name,null));}
    public Snapshot reload(String id)throws IOException{if(!id.matches("[a-z][a-z0-9_]{0,47}"))throw new IOException("Invalid dungeon ID");return tx.locked(()->build(null,0,null,id));}
    private Snapshot build(String newId,int newNumber,String newName,String onlyId)throws IOException{
        return build(newId,newNumber,newName,onlyId,new LinkedHashMap<>());
    }
    private Snapshot build(String newId,int newNumber,String newName,String onlyId,Map<String,byte[]> plan)throws IOException{
        var global=file("config.yml",m("foundation-only",true,"battle-preview",false,"shared-preview",false,"private-preview",false,"loot-preview",false,"battle-tick-interval",5,"scheduler",m("interval-ticks",20),"sessions",m("max-private-players",5,"max-private-active",3,"shared-cooldown-seconds",1800,"reservation-timeout-seconds",60),"migration",m("backup-retention",5)),plan);
        if(!bool(global,"foundation-only"))throw new IOException("foundation-only must remain true in v1.0.0-RC2");
        var sessions=section(global,"sessions");int pp=integer(sessions,"max-private-players",1,5),ps=integer(sessions,"max-private-active",1,32),rs=integer(sessions,"reservation-timeout-seconds",5,600),it=integer(section(global,"scheduler"),"interval-ticks",20,200),ret=integer(section(global,"migration"),"backup-retention",1,20);integer(sessions,"shared-cooldown-seconds",1800,1800);
        file("messages.yml",m("prefix","[ArunikaDungeon]","entry-disabled","Dungeon Phase II is in development."),plan);
        var perf=file("performance.yml",m("max-active-minions",100,"max-active-loot-items",256,"spawn-batch-size",5,"minimum-tps",18.0,"maximum-mspt",50.0),plan);int budget=integer(perf,"max-active-minions",1,1000),batch=integer(perf,"spawn-batch-size",1,100); double tps=decimal(perf,"minimum-tps",0,20),mspt=decimal(perf,"maximum-mspt",1,1000);boolean preview=bool(global,"battle-preview"),sharedPreview=bool(global,"shared-preview"),privatePreview=bool(global,"private-preview");if(privatePreview&&!preview)throw new IOException("private-preview requires battle-preview");if(sharedPreview&&!preview)throw new IOException("shared-preview requires battle-preview");int battleTicks=integer(global,"battle-tick-interval",1,20);boolean lootPreview=bool(global,"loot-preview");if(lootPreview&&!preview)throw new IOException("loot-preview requires battle-preview");int lootBudget=integer(perf,"max-active-loot-items",1,4096);
        var ticket=file("ticket.yml",m("id","dungeon_ticket","material","PAPER","display-name","Universal Dungeon Ticket","cost-per-session",1),plan);integer(ticket,"cost-per-session",1,1);if(!str(ticket,"id").equals("dungeon_ticket"))throw new IOException("Universal ticket ID must remain dungeon_ticket");String ticketMaterial=str(ticket,"material"),ticketName=str(ticket,"display-name");if(ticketName.isBlank()||ticketMaterial.isBlank())throw new IOException("Invalid ticket item");
        var gatewayFile=file("integrations/waystone.yml",m("enabled",false,"gateway-id","","hub",m("world","","x",0,"y",0,"z",0),"interaction-radius",6,"return-waystones",m()),plan);
        var rpgFile=file("integrations/nusa-karsa.yml",m("enabled",false,"class-exp-per-boss",0,"awakening-progress",false,"native-class-xp",false),plan);
        RpgPolicy rpg=new RpgPolicy(bool(rpgFile,"enabled"),integer(rpgFile,"class-exp-per-boss",0,100000),bool(rpgFile,"awakening-progress"),bool(rpgFile,"native-class-xp"));
        if(rpg.active()&&rpg.nativeClassXp()&&!lootPreview)throw new IOException("Native Class XP requires loot-preview to process bounded awards in arena");
        file("exchange/settings.yml",m("enabled",false,"legacy-ownership",true,"ticket-exchange",m("enabled",false,"cost",50),"permission-rewards-enabled",false,"allowed-permission-nodes",List.of()),plan);
        Map<String,DungeonCatalog.Floor> defs=new LinkedHashMap<>();for(var floor:DungeonCatalog.INITIAL)defs.put(dir(floor),floor);
        Path dungeonRoot=tx.path("dungeons");if(Files.exists(dungeonRoot)){try(var list=Files.list(dungeonRoot)){for(Path p:list.toList()){if(Files.isSymbolicLink(p))throw new IOException("Symlink dungeon refused");String d=p.getFileName().toString();if(!Files.isDirectory(p)||!d.matches("[0-9]{2,3}_[a-z][a-z0-9_]{0,47}"))continue;int sep=d.indexOf('_'),number=Integer.parseInt(d.substring(0,sep));String id=d.substring(sep+1);if(number<1||!d.equals(dir(new DungeonCatalog.Floor(id,number,id,""))))throw new IOException("Non-canonical dungeon folder: "+d);defs.putIfAbsent(d,new DungeonCatalog.Floor(id,number,id,""));}}}
        if(newId!=null){if(defs.values().stream().anyMatch(f->f.id().equals(newId)||f.number()==newNumber))throw new IOException("Duplicate dungeon ID or floor number");var f=new DungeonCatalog.Floor(newId,newNumber,newName,"");defs.put(dir(f),f);}
        if(defs.size()>200)throw new IOException("Maximum 200 configured dungeons in foundation");
        Set<String> ids=new HashSet<>();Set<Integer> numbers=new HashSet<>();List<DungeonCatalog.Floor> floors=new ArrayList<>();Map<String,BattlePlan> battles=new LinkedHashMap<>();Map<String,LootPlan> lootPlans=new LinkedHashMap<>();Map<String,Integer> sharedCountdowns=new LinkedHashMap<>(),privateLimits=new LinkedHashMap<>();
        for(var entry:defs.entrySet()){
            var f=entry.getValue();if(!ids.add(f.id())||!numbers.add(f.number()))throw new IOException("Duplicate dungeon id/floor "+entry.getKey());String base="dungeons/"+entry.getKey()+"/";
            var settings=file(base+"settings.yml",m("id",f.id(),"floor",f.number(),"display-name",f.name(),"enabled",false,"max-duration-seconds",1200,"wave-count",3,"icon",m("material","PAPER","custom-model-data",0)),plan);
            if(!str(settings,"id").equals(f.id())||integer(settings,"floor",1,999)!=f.number())throw new IOException(base+"identity differs from folder");String name=str(settings,"display-name");if(name.isBlank())throw new IOException(base+"empty display-name");boolean enabled=bool(settings,"enabled");integer(settings,"max-duration-seconds",60,7200);int count=integer(settings,"wave-count",1,30);
            var arena=file(base+"arena.yml",m("shared",location(),"private-arenas",List.of(),"exit",location()),plan);
            var waves=file(base+"waves.yml",m("waves",m()),plan);var wm=section(waves,"waves");
            // Add new wave templates without replacing old/custom entries.
            for(int i=1;i<=count;i++){String key="wave-"+i;if(!wm.containsKey(key))wm.put(key,m("mobs",List.of()));var w=section(wm,key);if(!(w.get("mobs") instanceof List<?> mobs))throw new IOException(base+key+" mobs must be list");if(enabled&&mobs.isEmpty())throw new IOException(base+key+" requires mobs to enable");for(Object mob:mobs){var mm=section(m("mob",mob),"mob");if(str(mm,"mythicmob-id").isBlank())throw new IOException(base+key+" empty mob id");integer(mm,"amount",1,1000);}}
            if(!wm.equals(section(waves,"waves"))){waves.put("waves",wm);plan.put(base+"waves.yml",codec.write(waves));}
            var boss=file(base+"boss.yml",m("mythicmob-id",f.bossReference(),"spawn",location()),plan);String bossId=str(boss,"mythicmob-id");
            if(!(arena.get("private-arenas") instanceof List<?>))throw new IOException(base+"private-arenas must be list");
            if(enabled){if(bossId.isBlank())throw new IOException(base+"boss id required");validateLocation(section(arena,"shared"));validateLocation(section(arena,"exit"));validateLocation(section(boss,"spawn"));if(!(arena.get("private-arenas") instanceof List<?> a)||a.isEmpty())throw new IOException(base+"private arena required");for(Object obj:a){if(!(obj instanceof Map<?,?>))throw new IOException("Private arena mapping required");validateLocation(section(m("arena",obj),"arena"));}}
            for(String mode:List.of("shared","private")){
                boolean sh=mode.equals("shared");String b=base+mode+"/";
                var modeConfig=file(b+"settings.yml",sh?m("enabled",true,"entry-cost","FREE","max-players",-1,"queue-countdown-seconds",30,"cooldown-seconds",1800,"loot",loot()):m("enabled",true,"entry-cost","TICKET","ticket-id","dungeon_ticket","ticket-cost",1,"max-players",5,"cooldown-seconds",0,"loot",loot()),plan);
                boolean modeEnabled=bool(modeConfig,"enabled");if(!str(modeConfig,"entry-cost").equals(sh?"FREE":"TICKET"))throw new IOException(b+"incorrect entry cost");integer(modeConfig,"max-players",sh?-1:1,sh?-1:5);integer(modeConfig,"cooldown-seconds",sh?1800:0,sh?1800:0);if(sh){int countdown=integer(modeConfig,"queue-countdown-seconds",5,300);if(modeEnabled)sharedCountdowns.put(f.id(),countdown);}else{if(modeEnabled)privateLimits.put(f.id(),Math.min(pp,integer(modeConfig,"max-players",1,5)));integer(modeConfig,"ticket-cost",1,1);if(!str(modeConfig,"ticket-id").equals("dungeon_ticket"))throw new IOException(b+"ticket must be universal");}
                var l=section(modeConfig,"loot");if(!str(l,"mode").equals("FREE_FOR_ALL"))throw new IOException(b+"physical loot required");integer(l,"pickup-delay-seconds",0,60);integer(l,"collection-time-seconds",1,300);integer(l,"scatter-radius",0,32);
                Map<Integer,LootPlan.Table> tables=new LinkedHashMap<>();for(int i=1;i<=count;i++)tables.put(i,drop(b+"wave-"+i+"-drops.yml",false,plan));var bossTable=drop(b+"boss-drops.yml",true,plan);try{lootPlans.put(f.id()+":"+mode,new LootPlan(integer(l,"pickup-delay-seconds",0,60),integer(l,"collection-time-seconds",1,300),integer(l,"scatter-radius",0,32),tables,bossTable));}catch(IllegalArgumentException e){throw new IOException(b+e.getMessage(),e);}
            }
            var battle=file(base+"battle.yml",m("enabled",false,"arenas",List.of(),"scaling",m("minion-per-extra-player",0.15,"max-multiplier",3.0)),plan);
            boolean battleEnabled=bool(battle,"enabled");
            if(!(battle.get("arenas") instanceof List<?> arenaList))throw new IOException(base+"battle arenas must be list");
            if(battleEnabled){try{
                List<List<Mob>> mobWaves=new ArrayList<>();for(int i=1;i<=count;i++){List<Mob> mobs=new ArrayList<>();for(Object o:(List<?>)section(wm,"wave-"+i).get("mobs")){var v=section(m("obj",o),"obj");mobs.add(new Mob(str(v,"mythicmob-id"),integer(v,"amount",1,1000)));}mobWaves.add(mobs);}
                List<Arena> arenas=new ArrayList<>();for(Object o:arenaList){var a=section(m("obj",o),"obj");var b=section(a,"bounds");List<Point> spawns=new ArrayList<>();if(!(a.get("mob-spawns") instanceof List<?> locations))throw new IOException("mob-spawns must be list");for(Object loc:locations)spawns.add(point(section(m("obj",loc),"obj")));
                    arenas.add(new Arena(str(a,"id"),Mode.valueOf(str(a,"mode")),new Bounds(point(section(b,"min")),point(section(b,"max"))),point(section(a,"player-spawn")),spawns,point(section(a,"boss-spawn")),point(section(a,"exit"))));}
                battles.put(f.id(),new BattlePlan(f.id(),name,integer(settings,"max-duration-seconds",60,7200),mobWaves,bossId,arenas,decimal(section(battle,"scaling"),"minion-per-extra-player",0,1),decimal(section(battle,"scaling"),"max-multiplier",1,5)));
            }catch(IllegalArgumentException e){throw new IOException(base+"battle: "+e.getMessage(),e);}}
            floors.add(new DungeonCatalog.Floor(f.id(),f.number(),name,bossId));
        }
        if(onlyId!=null){String prefix=defs.entrySet().stream().filter(e->e.getValue().id().equals(onlyId)).map(e->"dungeons/"+e.getKey()+"/").findFirst().orElseThrow(()->new IOException("Unknown dungeon ID"));if(plan.keySet().stream().anyMatch(p->!p.startsWith(prefix)))throw new IOException("Other files require migration; run /adg reload all");}
        try{BattlePlan.validateIsolation(battles.values());}catch(IllegalArgumentException e){throw new IOException(e.getMessage(),e);}
        sharedCountdowns.keySet().removeIf(id->!battles.containsKey(id)||battles.get(id).arenas().stream().noneMatch(a->a.mode()==Mode.SHARED));
        privateLimits.keySet().removeIf(id->!battles.containsKey(id)||battles.get(id).arenas().stream().noneMatch(a->a.mode()==Mode.PRIVATE));
        Gateway gateway=parseGateway(gatewayFile,battles,preview);
        floors.sort(Comparator.comparingInt(DungeonCatalog.Floor::number));String backup=tx.commit(plan);String warning="";if(!plan.isEmpty())try{tx.prune(ret);}catch(IOException e){warning="Backup retention cleanup failed: "+e.getMessage();}
        return new Snapshot(floors,pp,ps,rs,it,plan.size(),backup,warning,preview,battleTicks,budget,Math.min(batch,budget),tps,mspt,battles,sharedPreview,sharedCountdowns,privatePreview,privateLimits,ticketMaterial,ticketName,lootPreview,lootPlans,lootBudget,gateway,rpg);
    }
    private static Gateway parseGateway(Map<String,Object> v,Map<String,BattlePlan> battles,boolean preview)throws IOException{
        boolean enabled=bool(v,"enabled");double radius=decimal(v,"interaction-radius",2,16);String raw=str(v,"gateway-id");UUID id=null;Point hub=null;Map<UUID,String> returns=new LinkedHashMap<>();
        try{if(!raw.isBlank())id=UUID.fromString(raw);var h=section(v,"hub");if(!str(h,"world").isBlank())hub=point(h);var entries=section(v,"return-waystones");if(entries.size()>6400)throw new IOException("Too many return bindings");for(String key:entries.keySet()){UUID stone=UUID.fromString(key);String target=str(entries,key);if(stone.equals(id)||returns.putIfAbsent(stone,target)!=null||!target.matches("[a-z][a-z0-9_]{0,47}/[a-z][a-z0-9_-]{0,47}"))throw new IOException("Invalid/duplicate return binding");} }catch(IllegalArgumentException e){throw new IOException("Invalid gateway UUID/point",e);}
        if(enabled){if(!preview||id==null||hub==null||battles.isEmpty())throw new IOException("Gateway requires battle-preview, hub UUID, hub location and configured arena");Set<String> actual=new HashSet<>();for(var b:battles.values())for(var a:b.arenas()){actual.add(b.floor()+"/"+a.id());if(a.bounds().contains(hub))throw new IOException("Hub inside arena");if(!a.exit().equals(hub))throw new IOException("All arena exits must equal the configured gateway hub: "+b.floor()+"/"+a.id());}if(!actual.containsAll(returns.values()))throw new IOException("Return binding refers to unknown arena");}
        return new Gateway(enabled,id,hub,radius,returns);
    }
    private static double decimal(Map<String,Object> map,String key,double min,double max)throws IOException{Object v=map.get(key);if(!(v instanceof Number n)||!Double.isFinite(n.doubleValue())||n.doubleValue()<min||n.doubleValue()>max)throw new IOException(key+" outside range");return n.doubleValue();}
    private static Point point(Map<String,Object> a)throws IOException{return new Point(str(a,"world"),decimal(a,"x",-29999984,29999984),decimal(a,"y",-2048,2048),decimal(a,"z",-29999984,29999984));}
    private LootPlan.Table drop(String rel,boolean boss,Map<String,byte[]> plan)throws IOException{
        var d=file(rel,m("enabled",true,"trigger",boss?"BOSS_KILL":"WAVE_CLEAR","drops",m()),plan);return parseDrop(rel,boss,d);}
    private LootPlan.Table parseDrop(String rel,boolean boss,Map<String,Object> d)throws IOException{bool(d,"enabled");String trigger=str(d,"trigger");if(boss?!trigger.equals("BOSS_KILL"):!Set.of("WAVE_CLEAR","MOB_KILL").contains(trigger))throw new IOException(rel+" invalid trigger");var drops=section(d,"drops");List<LootPlan.Entry> entries=new ArrayList<>();if(drops.size()>128)throw new IOException(rel+" too many drop entries");for(String key:drops.keySet()){var item=section(drops,key);String type=str(item,"type");if(!Set.of("VANILLA","CUSTOM","MMOITEMS","ITEMSADDER","MYTHICMOBS","SERIALIZED").contains(type))throw new IOException(rel+" unknown drop type");if(str(item,type.equals("VANILLA")?"material":"id").isBlank())throw new IOException(rel+" empty item ID");if(type.equals("MMOITEMS")&&str(item,"item-type").isBlank())throw new IOException(rel+" empty item-type");Object chance=item.get("chance");if(!(chance instanceof Number n)||!Double.isFinite(n.doubleValue())||n.doubleValue()<0||n.doubleValue()>100)throw new IOException(rel+" chance must be 0..100");int min=integer(item,"min",1,4096);int max=integer(item,"max",min,4096);try{entries.add(new LootPlan.Entry(LootPlan.Type.valueOf(type),str(item,type.equals("VANILLA")?"material":"id"),type.equals("MMOITEMS")?str(item,"item-type"):"",n.doubleValue(),min,max));}catch(IllegalArgumentException e){throw new IOException(rel+" invalid entry: "+key,e);}}return new LootPlan.Table(LootPlan.Trigger.valueOf(trigger),bool(d,"enabled")?entries:List.of());}
    /** Edit a single existing drop table atomically. Caller reloads after save. */
    public String editDrop(String floor,String mode,String stage,String key,Map<String,Object> value)throws IOException{
        if(!floor.matches("[a-z][a-z0-9_]{0,47}")||!Set.of("shared","private").contains(mode)||!stage.matches("boss|wave-([1-9]|[12][0-9]|30)")||!key.matches("[a-zA-Z0-9_-]{1,48}"))throw new IOException("Invalid drop editor arguments");
        return tx.locked(()->{List<Path> matches;try(var stream=Files.list(tx.path("dungeons"))){matches=stream.filter(p->p.getFileName().toString().matches("[0-9]{2,3}_"+floor)).toList();}if(matches.size()!=1)throw new IOException("Unknown/ambiguous floor");String rel="dungeons/"+matches.getFirst().getFileName()+"/"+mode+"/"+stage+"-drops.yml";Path p=tx.path(rel);if(!Files.isRegularFile(p)||Files.size(p)>2_000_000)throw new IOException("Drop table unavailable");var d=codec.read(Files.readAllBytes(p));integer(d,"schema-version",1,SCHEMA);var drops=section(d,"drops");if(value==null){if(drops.remove(key)==null)throw new IOException("Unknown entry");}else drops.put(key,new LinkedHashMap<>(value));d.put("drops",drops);parseDrop(rel,stage.equals("boss"),d);byte[] bytes=codec.write(d);if(bytes.length>2_000_000)throw new IOException("Drop table too large");String backup=tx.commit(Map.of(rel,bytes));try{tx.prune(5);}catch(IOException ignored){}return backup;});
    }
    /** Typed edits share the normal validator and atomic transaction; no raw paths from commands. */
    public Snapshot editBattle(String floor,String action,String id,int number,int amount,Arena arena)throws IOException{
        if(!floor.matches("[a-z][a-z0-9_]{0,47}"))throw new IOException("Invalid floor");
        return tx.locked(()->{
            List<Path> matches;try(var ls=Files.list(tx.path("dungeons"))){matches=ls.filter(p->p.getFileName().toString().matches("[0-9]{2,3}_"+floor)).toList();}
            if(matches.size()!=1)throw new IOException("Unknown floor");String base="dungeons/"+matches.getFirst().getFileName()+"/";
            Map<String,byte[]> plan=new LinkedHashMap<>();String rel=base+switch(action){case "boss"->"boss.yml";case "wave","append-wave"->"waves.yml";case "count"->"settings.yml";case "arena","enable","disable"->"battle.yml";default->throw new IOException("Unknown edit");};
            Path path=tx.path(rel);if(!Files.isRegularFile(path)||Files.size(path)>2_000_000)throw new IOException("Invalid config file");var data=codec.read(Files.readAllBytes(path));integer(data,"schema-version",1,SCHEMA);
            switch(action){
                case "boss"->{if(id==null||!id.matches("[A-Za-z0-9_.:-]{1,128}"))throw new IOException("Invalid Mythic ID");data.put("mythicmob-id",id);}
                case "wave","append-wave"->{if(number<1||number>30||amount<1||amount>1000||id==null||!id.matches("[A-Za-z0-9_.:-]{1,128}"))throw new IOException("Invalid wave mob");var waves=section(data,"waves");var wave=section(waves,"wave-"+number);List<Object> mobs=new ArrayList<>();if(action.equals("append-wave"))mobs.addAll((List<?>)wave.get("mobs"));mobs.add(m("mythicmob-id",id,"amount",amount));if(mobs.size()>128)throw new IOException("Too many mob entries");wave.put("mobs",mobs);waves.put("wave-"+number,wave);data.put("waves",waves);}
                case "count"->{if(number<1||number>30)throw new IOException("Wave count 1..30");data.put("wave-count",number);}
                case "enable","disable"->data.put("enabled",action.equals("enable"));
                case "arena"->{if(arena==null)throw new IOException("Arena required");List<Object> list=new ArrayList<>((List<?>)data.get("arenas"));list.removeIf(o->o instanceof Map<?,?> map&&arena.id().equals(map.get("id")));list.add(m("id",arena.id(),"mode",arena.mode().name(),"bounds",m("min",pointMap(arena.bounds().min()),"max",pointMap(arena.bounds().max())),"player-spawn",pointMap(arena.playerSpawn()),"mob-spawns",arena.mobSpawns().stream().map(SmartConfiguration::pointMap).toList(),"boss-spawn",pointMap(arena.bossSpawn()),"exit",pointMap(arena.exit())));if(list.size()>32)throw new IOException("Too many arenas");data.put("arenas",list);}
            }
            byte[] bytes=codec.write(data);if(bytes.length>2_000_000)throw new IOException("Edit too large");plan.put(rel,bytes);return build(null,0,null,null,plan);
        });
    }
    /** Import reviewed dungeon-only bindings and synchronize exits in one recoverable config transaction. */
    public Snapshot importGateway(Map<String,Object> exported,boolean enable)throws IOException{
        return tx.locked(()->{
            Map<String,Object> gateway=new LinkedHashMap<>(exported);
            gateway.put("schema-version",SCHEMA);gateway.put("enabled",enable);
            Point hub=point(section(gateway,"hub"));
            Map<String,byte[]> plan=new LinkedHashMap<>();plan.put("integrations/waystone.yml",codec.write(gateway));
            try(var dirs=Files.list(tx.path("dungeons"))){
                for(Path dir:dirs.filter(Files::isDirectory).toList()){
                    String rel="dungeons/"+dir.getFileName()+"/battle.yml";
                    Path file=tx.path(rel);if(!Files.isRegularFile(file))continue;
                    Map<String,Object> data=codec.read(Files.readAllBytes(file));
                    if(!(data.get("arenas") instanceof List<?> arenas))throw new IOException("Invalid arenas");
                    List<Object> changed=new ArrayList<>();
                    for(Object value:arenas){var a=section(m("arena",value),"arena");a.put("exit",pointMap(hub));changed.add(a);}
                    data.put("arenas",changed);plan.put(rel,codec.write(data));
                }
            }
            return build(null,0,null,null,plan);
        });
    }
    private static Map<String,Object> pointMap(Point p){return m("world",p.world(),"x",p.x(),"y",p.y(),"z",p.z());}
    private static String dir(DungeonCatalog.Floor f){return String.format(Locale.ROOT,"%02d_%s",f.number(),f.id());}
    private static Map<String,Object> location(){return m("world","","x",0,"y",0,"z",0);}
    private static Map<String,Object> loot(){return m("mode","FREE_FOR_ALL","pickup-delay-seconds",3,"collection-time-seconds",45,"scatter-radius",5);}
    private static void validateLocation(Map<String,Object> l)throws IOException{if(str(l,"world").isBlank())throw new IOException("World required");for(String k:List.of("x","y","z"))if(!(l.get(k) instanceof Number n)||!Double.isFinite(n.doubleValue()))throw new IOException("Invalid coordinate "+k);}
}
