package dev.arunika.dungeonwaystone.bedrock;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.event.EventRegistrar;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineEntitiesEvent;
import org.geysermc.geyser.api.event.java.ServerSpawnEntityEvent;
import org.geysermc.geyser.api.event.bedrock.SessionLoadResourcePacksEvent;
import org.geysermc.geyser.api.entity.custom.CustomEntityDefinition;
import org.geysermc.geyser.api.entity.data.GeyserEntityDataTypes;
import dev.arunika.dungeonwaystone.DungeonWaystonePlugin;
import dev.arunika.dungeonwaystone.Node;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Separate Bedrock-only visuals for DungeonWaystone. Gameplay and clicks belong to
 * ArunikaDungeon. Existing Java furniture and CdrWaystone data are never edited.
 * Two original meshes are represented by distinct Geyser entity identifiers.
 */
public final class DungeonWaystoneBedrockPlugin extends JavaPlugin implements Listener {
    private record VisualProxy(UUID nodeId, ArmorStand stand, String entityId) {}

    private final Map<UUID,VisualProxy> visuals = new HashMap<>();
    private final LinkedHashMap<UUID,Long> retiredProxies=new LinkedHashMap<>();
    private final Map<UUID,String> proxyTypes = new ConcurrentHashMap<>();
    private final Map<String,CustomEntityDefinition> definitions = new ConcurrentHashMap<>();
    private final Map<UUID,Set<UUID>> hiddenOriginals = new HashMap<>();
    private final Map<UUID,Set<UUID>> shownProxies = new HashMap<>();
    private final Map<UUID,Set<UUID>> preparedProxies = new ConcurrentHashMap<>();
    private final Set<org.geysermc.geyser.api.connection.GeyserConnection> packConnections = ConcurrentHashMap.newKeySet();
    private static final UUID PACK_ID=UUID.fromString("370b11e6-501f-529c-a183-85deb81d6246");
    private volatile float renderScale=1f;
    private volatile boolean callbackFailed;
    private boolean packValid;
    private boolean packsRequired;

    private DungeonWaystonePlugin core;
    private GeyserApi geyserApi;
    private EventRegistrar registrar;
    private BukkitTask ticker;
    private volatile boolean definitionsReady;
    private String apiStatus = "UNAVAILABLE";

    @Override public void onLoad() {
        // Geyser-Spigot loads its API in onLoad and initializes registries at ServerLoadEvent.
        // Subscribe before onEnable to avoid relying on incidental enable ordering.
        try { connectGeyser(); }
        catch (Exception | LinkageError e) { apiStatus="API unavailable: "+e; }
    }
    @Override public void onEnable() {
        saveDefaultConfig();
        if("ArunikaDungeonWaystone-Bedrock-Models-v0.2.0.mcpack".equals(getConfig().getString("pack.name"))){
            try{
                Path cfg=getDataFolder().toPath().resolve("config.yml");
                Files.copy(cfg,cfg.resolveSibling("config-before-1.0.0-"+UUID.randomUUID()+".yml"));
                getConfig().set("pack.name","ArunikaDungeonWaystone-Bedrock-Models.mcpack");saveConfig();
            }catch(Exception e){getLogger().warning("Pack path migration skipped: "+e);}
        }
        core = (DungeonWaystonePlugin) Bukkit.getPluginManager().getPlugin("ArunikaDungeonWaystone");
        if (core == null || !core.isEnabled()) {
            getLogger().severe("DungeonWaystone core is unavailable. No Bedrock models created.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        Bukkit.getPluginManager().registerEvents(this,this);
        try {
            if (geyserApi==null) connectGeyser();
        } catch (Exception | LinkageError e) {
            apiStatus = "Geyser Entity API initialization failed: " + e.getMessage();
            getLogger().severe(apiStatus);
        }
        checkPack();
        renderScale=(float)clamp(getConfig().getDouble("render.scale",1),.25,3);
        long period = clamp(getConfig().getInt("render.update-interval-ticks",20),10,100);
        ticker = Bukkit.getScheduler().runTaskTimer(this, this::sync, 20L, period);
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (!definitionsReady) getLogger().warning(
                "Dungeon Bedrock model definitions not registered. Perform FULL server STOP/START; check Geyser API compatibility.");
        },100L);
        getLogger().info("Dungeon Bedrock renderer v1.0.1-RC2: " + apiStatus
              + "; uses Blackstone=HUB, Portstone=RETURN; model API ready=" + definitionsReady);
    }

    private void connectGeyser() {
        geyserApi=GeyserApi.api();
        if(geyserApi==null)throw new IllegalStateException("Geyser API not loaded");
        registrar=EventRegistrar.of(this);
        var bus=geyserApi.eventBus();
        bus.subscribe(registrar,GeyserDefineEntitiesEvent.class,this::defineEntities);
        bus.subscribe(registrar,ServerSpawnEntityEvent.class,this::replaceProxy);
        bus.subscribe(registrar,SessionLoadResourcePacksEvent.class,event->{
            boolean offered=event.resourcePacks().stream().anyMatch(p->PACK_ID.equals(p.uuid()));
            if(offered&&packConnections.size()<1024)packConnections.add(event.connection());else packConnections.remove(event.connection());
        });
        bus.subscribe(registrar,org.geysermc.geyser.api.event.bedrock.SessionDisconnectEvent.class,event->{
            packConnections.remove(event.connection());
            UUID id=event.connection().javaUuid();if(id!=null)preparedProxies.values().forEach(set->set.remove(id));
        });
        apiStatus="Typed Geyser 2.12.1 API subscribed before registry initialization";
    }
    private boolean bedrock(Player p) {
        return geyserApi!=null && geyserApi.isBedrockPlayer(p.getUniqueId());
    }
    private boolean hasPack(Player p) {
        if(geyserApi==null)return false;var connection=geyserApi.connectionByUuid(p.getUniqueId());return connection!=null&&packConnections.contains(connection);
    }
    private void defineEntities(GeyserDefineEntitiesEvent event) {
        definitionsReady=false;definitions.clear();
        try {
            for(String id:List.of(ModelCatalog.HUB,ModelCatalog.RETURN)) {
                CustomEntityDefinition def=CustomEntityDefinition.of(id);
                event.register(def);definitions.put(id,def);
            }
            definitionsReady=definitions.size()==2 && definitions.values().stream().allMatch(CustomEntityDefinition::registered);
        }catch(Exception|LinkageError e){apiStatus="Registration failed: "+e;getLogger().severe(apiStatus);}
    }
    private void replaceProxy(ServerSpawnEntityEvent event) {
        String type=proxyTypes.get(event.uuid());
        if(type==null)return;
        CustomEntityDefinition def=definitions.get(type);
        if(!definitionsReady||callbackFailed||def==null||!packConnections.contains(event.connection())){
            event.setCancelled(true);return;
        }
        // Event callbacks run on Geyser threads: only immutable settings/concurrent state here.
        try {
            event.definition(def);
            event.preSpawnConsumer(entity->{
                try {
                    entity.override(GeyserEntityDataTypes.SCALE,renderScale);
                    entity.override(GeyserEntityDataTypes.WIDTH,1f);
                    entity.override(GeyserEntityDataTypes.HEIGHT,2f);
                    entity.override(GeyserEntityDataTypes.VERTICAL_OFFSET,0f);
                    preparedProxies.computeIfAbsent(event.uuid(),k->ConcurrentHashMap.newKeySet())
                        .add(event.connection().javaUuid());
                }catch(Exception|LinkageError e){callbackFailed=true;}
            });
        }catch(Exception|LinkageError e){callbackFailed=true;event.setCancelled(true);}
    }

    /** One sync job, no per-player scheduler, and only registered dungeon nodes. */
    private void sync() {
        long retireTime=System.nanoTime();
        for(var it=retiredProxies.entrySet().iterator();it.hasNext();){var e=it.next();if(retireTime-e.getValue()>=0){proxyTypes.remove(e.getKey(),"");it.remove();}}
        if(!definitionsReady || callbackFailed || !packValid || !packsRequired || core==null || !core.isEnabled() ||
           !getConfig().getBoolean("render.enabled",true)) {cleanupProxies();restoreAll();return;}
        Collection<Node> nodes=core.getNodes();
        List<Player> clients=new ArrayList<>();
        for(Player p:Bukkit.getOnlinePlayers())if(!p.isDead()&&bedrock(p)&&hasPack(p))clients.add(p);
        if(clients.isEmpty()){cleanupProxies();restoreAll();return;}
        double radius=clamp(getConfig().getDouble("render.radius-blocks",48),8,96);
        double squared=radius*radius;
        int max=clamp(getConfig().getInt("render.max-models",48),1,64);
        Set<UUID> required=new HashSet<>();
        int created=0;
        for(Node n:nodes) {
            if(created>=max)break;
            try {
                UUID id=n.id();
                if (!core.dedicated(n)) continue;
                String entityId=ModelCatalog.forRole(n.role().name());
                if(entityId==null)continue;
                Location loc=core.location(n);
                if(loc==null||loc.getWorld()==null ||
                    !loc.getWorld().isChunkLoaded(loc.getBlockX()>>4,loc.getBlockZ()>>4))continue;
                Location modelAt=loc.clone().add(.5,clamp(getConfig().getDouble("render.offset-y",0),-2,2),.5);
                modelAt.setYaw((float)core.getConfig().getDouble("visuals.rotation-degrees",0));
                boolean near=false;
                for(Player p:clients)if(p.getWorld()==loc.getWorld()&&p.getLocation().distanceSquared(modelAt)<=squared){near=true;break;}
                if(!near)continue;
                required.add(id);created++;
                VisualProxy old=visuals.get(id);
                if(old!=null&&(!old.stand().isValid() || !old.entityId().equals(entityId)
                 || old.stand().getWorld()!=loc.getWorld() || old.stand().getLocation().distanceSquared(modelAt)>.01 || Math.abs(old.stand().getLocation().getYaw()-modelAt.getYaw())>.01)){
                     removeProxy(old);old=null;
                }
                if(old==null) {
                    ArmorStand stand=loc.getWorld().spawn(modelAt,ArmorStand.class, s->{
                        s.setVisibleByDefault(false);
                        s.setVisible(true);
                        s.setMarker(false);
                        s.setGravity(false);
                        s.setSilent(true);
                        s.setInvulnerable(true);
                        s.setPersistent(false);
                        s.setCollidable(false);
                        s.getPersistentDataContainer().set(new NamespacedKey(this,"proxy_node"),PersistentDataType.STRING,id.toString());
                        proxyTypes.put(s.getUniqueId(),entityId);
                    });
                    old=new VisualProxy(id,stand,entityId);
                    visuals.put(id,old);
                }
            }catch(Exception|LinkageError e){getLogger().warning("Could not sync dungeon model: "+e.getMessage());}
        }
        for(VisualProxy item:List.copyOf(visuals.values()))if(!required.contains(item.nodeId()))removeProxy(item);
        // Scan original furniture once per node per cycle, not once per Bedrock player.
        Map<UUID,List<Entity>> originals=new HashMap<>();
        if(getConfig().getBoolean("render.hide-original-for-bedrock",true)) {
            for(VisualProxy item:visuals.values()) {
                List<Entity> matches=new ArrayList<>();
                for(Entity e:item.stand().getWorld().getNearbyEntities(
                        item.stand().getLocation().clone().add(0,1,0),2,3,2)) {
                    if(ownedFurniture(e,item.nodeId())) matches.add(e);
                }
                originals.put(item.nodeId(),matches);
            }
        }
        for(Player p:Bukkit.getOnlinePlayers()) {
            boolean isBedrock=clients.contains(p);
            Set<UUID> shown=shownProxies.computeIfAbsent(p.getUniqueId(),k->new HashSet<>());
            Set<UUID> now=new HashSet<>();
            for(VisualProxy item:visuals.values()) {
                if(item.stand().getWorld()!=p.getWorld())continue;
                boolean near=isBedrock && p.getLocation().distanceSquared(item.stand().getLocation())<=squared;
                if(near) {
                    if(shown.add(item.stand().getUniqueId()))p.showEntity(this,item.stand());
                    if(!preparedProxies.getOrDefault(item.stand().getUniqueId(),Set.of()).contains(p.getUniqueId()))continue;
                    for(Entity e:originals.getOrDefault(item.nodeId(),List.of())) {
                        now.add(e.getUniqueId());
                        if(!hiddenOriginals.getOrDefault(p.getUniqueId(),Set.of()).contains(e.getUniqueId()))p.hideEntity(this,e);
                    }
                } else if(shown.remove(item.stand().getUniqueId())){
                    p.hideEntity(this,item.stand());
                    Set<UUID> prepared=preparedProxies.get(item.stand().getUniqueId());if(prepared!=null)prepared.remove(p.getUniqueId());
                }
            }
            reconcileHidden(p,now);
        }
    }
    private boolean ownedFurniture(Entity entity,UUID nodeId){
        if(entity instanceof Player||entity instanceof ArmorStand a && proxyTypes.containsKey(a.getUniqueId()))return false;
        NamespacedKey owner=new NamespacedKey(core,"model_owner");
        NamespacedKey node=new NamespacedKey(core,"model_node_id");
        return Byte.valueOf((byte)1).equals(entity.getPersistentDataContainer().get(owner,PersistentDataType.BYTE))
         && nodeId.toString().equals(entity.getPersistentDataContainer().get(node,PersistentDataType.STRING));
    }
    private void reconcileHidden(Player p,Set<UUID> current) {
        Set<UUID> was=hiddenOriginals.getOrDefault(p.getUniqueId(),Set.of());
        for(UUID entityId:was)if(!current.contains(entityId)) {
            Entity e=Bukkit.getEntity(entityId);if(e!=null)p.showEntity(this,e);
        }
        if(current.isEmpty())hiddenOriginals.remove(p.getUniqueId());
        else hiddenOriginals.put(p.getUniqueId(),current);
    }
    private void restoreAll(){
        for(var e:new ArrayList<>(hiddenOriginals.entrySet())) {
            Player p=Bukkit.getPlayer(e.getKey());if(p==null)continue;
            for(UUID id:e.getValue()) {Entity entity=Bukkit.getEntity(id);if(entity!=null)p.showEntity(this,entity);}
        }
        hiddenOriginals.clear();
    }
    private void removeProxy(VisualProxy item) {
        visuals.remove(item.nodeId());UUID entity=item.stand().getUniqueId();
        // Keep UUID mapping until remove() queues despawn; an in-flight spawn must never become a vanilla stand.
        proxyTypes.put(entity,"");item.stand().remove();preparedProxies.remove(entity);
        if(retiredProxies.size()>=256){UUID first=retiredProxies.keySet().iterator().next();retiredProxies.remove(first);proxyTypes.remove(first,"");}
        retiredProxies.put(entity,System.nanoTime()+5_000_000_000L);
        shownProxies.values().forEach(set->set.remove(entity));
    }
    private void cleanupProxies() {for(VisualProxy item:List.copyOf(visuals.values()))removeProxy(item);}
    @EventHandler public void quit(PlayerQuitEvent event){UUID id=event.getPlayer().getUniqueId();hiddenOriginals.remove(id);shownProxies.remove(id);
        preparedProxies.values().forEach(set->set.remove(id));
        if(geyserApi!=null){var c=geyserApi.connectionByUuid(id);if(c!=null)packConnections.remove(c);}}

    private void checkPack() {
        packValid=false;
        Plugin geyser=Bukkit.getPluginManager().getPlugin("Geyser-Spigot");
        if(geyser==null)return;
        Path name=geyser.getDataFolder().toPath().resolve("packs")
           .resolve(getConfig().getString("pack.name","ArunikaDungeonWaystone-Bedrock-Models.mcpack"));
        packValid=PackValidator.valid(name);
        // If packs are optional an offered pack is not proof that the joining client accepted it.
        // Do not hide Java furniture or spawn replacements in that ambiguous mode.
        try{
            var config=org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(geyser.getDataFolder().toPath().resolve("config.yml").toFile());
            packsRequired=config.getBoolean("gameplay.force-resource-packs",config.getBoolean("force-resource-packs",false));
        }catch(Exception e){packsRequired=false;}
        if(!packsRequired)getLogger().warning("Geyser resource packs are optional: keeping original visuals. Enable gameplay.force-resource-packs and restart to use custom dungeon models.");
        if(!packValid)getLogger().warning("Dungeon pack missing/invalid; original visuals and anchor remain available: "+name);
    }
    @Override public boolean onCommand(CommandSender sender,Command cmd,String label,String[] args) {
        if(!sender.hasPermission("arunika.dungeonwaystone.admin")){sender.sendMessage("No permission.");return true;}
        String op=args.length==0?"status":args[0].toLowerCase(Locale.ROOT);
        if(op.equals("reload")) {reloadConfig();renderScale=(float)clamp(getConfig().getDouble("render.scale",1),.25,3);checkPack();sender.sendMessage("Config reloaded. Geyser entity definitions require a full server restart.");return true;}
        if(op.equals("refresh")){sync();sender.sendMessage("Dungeon Bedrock model sync requested.");return true;}
        if(!op.equals("status")){sender.sendMessage("/adwbedrock <status|refresh|reload>");return true;}
        long bedrock=Bukkit.getOnlinePlayers().stream().filter(this::bedrock).count();
        sender.sendMessage("[ADW Bedrock 1.0.1-RC2] "+apiStatus+" | registered="+definitionsReady
         +" ("+definitions.size()+"/2) | packValid="+packValid+" | packsRequired="+packsRequired+" | callbackFailed="+callbackFailed+" | proxies="+visuals.size()+" | Bedrock online="+bedrock);
        return true;
    }
    @Override public void onDisable() {
        if(ticker!=null){ticker.cancel();ticker=null;}
        restoreAll();cleanupProxies();
        if(geyserApi!=null&&registrar!=null)geyserApi.eventBus().unregisterAll(registrar);
        definitions.clear();proxyTypes.clear();retiredProxies.clear();preparedProxies.clear();shownProxies.clear();packConnections.clear();
        geyserApi=null;registrar=null;definitionsReady=false;
    }
    @EventHandler public void chunkUnload(org.bukkit.event.world.ChunkUnloadEvent e){
        for(VisualProxy v:List.copyOf(visuals.values()))if(v.stand().getWorld()==e.getWorld() &&
          (v.stand().getLocation().getBlockX()>>4)==e.getChunk().getX() &&
          (v.stand().getLocation().getBlockZ()>>4)==e.getChunk().getZ())removeProxy(v);
    }
    @EventHandler public void worldChange(org.bukkit.event.player.PlayerChangedWorldEvent e){
        reconcileHidden(e.getPlayer(),Set.of());Set<UUID> shown=shownProxies.remove(e.getPlayer().getUniqueId());
        if(shown!=null)for(UUID id:shown){Entity entity=Bukkit.getEntity(id);if(entity!=null)e.getPlayer().hideEntity(this,entity);}
        preparedProxies.values().forEach(set->set.remove(e.getPlayer().getUniqueId()));
    }
    private static int clamp(int v,int min,int max){return Math.max(min,Math.min(max,v));}
    private static double clamp(double v,double min,double max){
        return Double.isFinite(v)?Math.max(min,Math.min(max,v)):min;
    }
}
