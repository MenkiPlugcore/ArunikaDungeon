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

import java.lang.reflect.*;
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
    private final Map<UUID,String> proxyTypes = new ConcurrentHashMap<>();
    private final Map<String,Object> definitions = new ConcurrentHashMap<>();
    private final Map<UUID,Set<UUID>> hiddenOriginals = new HashMap<>();
    private final List<Object> subscriptions = new ArrayList<>();

    private Plugin core;
    private Object geyserApi, geyserBus, registrar, floodgateApi;
    private Method floodgateCheck, geyserCheck;
    private BukkitTask ticker;
    private Class<?> dataTypes;
    private volatile boolean definitionsReady;
    private String apiStatus = "UNAVAILABLE";

    @Override public void onEnable() {
        saveDefaultConfig();
        core = Bukkit.getPluginManager().getPlugin("ArunikaDungeonWaystone");
        if (core == null || !core.isEnabled()) {
            getLogger().severe("DungeonWaystone core is unavailable. No Bedrock models created.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        Bukkit.getPluginManager().registerEvents(this,this);
        try {
            connectGeyser();
        } catch (Exception | LinkageError e) {
            apiStatus = "Geyser Entity API initialization failed: " + e.getMessage();
            getLogger().severe(apiStatus);
        }
        checkPack();
        long period = clamp(getConfig().getInt("render.update-interval-ticks",20),10,100);
        ticker = Bukkit.getScheduler().runTaskTimer(this, this::sync, 20L, period);
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (!definitionsReady) getLogger().warning(
                "Dungeon Bedrock model definitions not registered. Perform FULL server STOP/START; check Geyser API compatibility.");
        },100L);
        getLogger().info("Dungeon Bedrock renderer v0.2.0: " + apiStatus
              + "; uses Blackstone=HUB, Portstone=RETURN; model API ready=" + definitionsReady);
    }

    private void connectGeyser() throws ReflectiveOperationException {
        Plugin geyser = Bukkit.getPluginManager().getPlugin("Geyser-Spigot");
        if (geyser == null || !geyser.isEnabled()) throw new ClassNotFoundException("Geyser-Spigot");
        ClassLoader loader=geyser.getClass().getClassLoader();
        Class<?> apiClass=Class.forName("org.geysermc.geyser.api.GeyserApi",true,loader);
        geyserApi=apiClass.getMethod("api").invoke(null);
        if(geyserApi==null) throw new IllegalStateException("GeyserApi.api() returned null");
        geyserBus=call(geyserApi,"eventBus");
        Class<?> registrarType=Class.forName("org.geysermc.geyser.api.event.EventRegistrar",true,loader);
        registrar=java.lang.reflect.Proxy.newProxyInstance(loader,new Class<?>[]{registrarType},
          (p,m,a)->switch(m.getName()){
            case "equals" -> p==(a==null?null:a[0]);
            case "hashCode" -> System.identityHashCode(p);
            case "toString" -> "ArunikaDungeonWaystoneBedrock";
            default -> null;
          });
        dataTypes=Class.forName("org.geysermc.geyser.api.entity.data.GeyserEntityDataTypes",true,loader);
        // Validate methods up front, before claiming Geyser support.
        Class<?> spawnType=Class.forName("org.geysermc.geyser.api.event.java.ServerSpawnEntityEvent",true,loader);
        spawnType.getMethod("uuid");
        spawnType.getMethod("preSpawnConsumer",Consumer.class);
        Class<?> definitionType=Class.forName("org.geysermc.geyser.api.entity.definition.GeyserEntityDefinition",true,loader);
        spawnType.getMethod("definition",definitionType);
        subscribe(loader,"org.geysermc.geyser.api.event.lifecycle.GeyserDefineEntitiesEvent",this::defineEntities);
        subscribe(loader,"org.geysermc.geyser.api.event.java.ServerSpawnEntityEvent",this::replaceProxy);
        if (hasMethod(geyserApi,"isBedrockPlayer",UUID.class))
            geyserCheck=geyserApi.getClass().getMethod("isBedrockPlayer",UUID.class);
        connectFloodgate();
        apiStatus="Geyser event subscriptions active";
    }

    private void connectFloodgate() {
        try {
            Plugin p=Bukkit.getPluginManager().getPlugin("floodgate");
            if(p==null||!p.isEnabled())return;
            Class<?> c=Class.forName("org.geysermc.floodgate.api.FloodgateApi",true,p.getClass().getClassLoader());
            floodgateApi=c.getMethod("getInstance").invoke(null);
            floodgateCheck=c.getMethod("isFloodgatePlayer",UUID.class);
        }catch(Exception|LinkageError e){getLogger().warning("Floodgate API optional: "+e.getMessage());}
    }
    private boolean bedrock(Player p) {
        try {
            if(geyserCheck!=null && Boolean.TRUE.equals(geyserCheck.invoke(geyserApi,p.getUniqueId())))return true;
            return floodgateCheck!=null && Boolean.TRUE.equals(floodgateCheck.invoke(floodgateApi,p.getUniqueId()));
        }catch(Exception e){return false;}
    }

    private void subscribe(ClassLoader loader,String eventName,Consumer<Object> callback)
            throws ReflectiveOperationException {
        Class<?> eventClass=Class.forName(eventName,true,loader);
        Object token=call(geyserBus,"subscribe",registrar,eventClass,callback);
        if(token!=null)subscriptions.add(token);
    }
    private void defineEntities(Object event) {
        definitions.clear();definitionsReady=false;
        try {
            Class<?> cls=Class.forName("org.geysermc.geyser.api.entity.custom.CustomEntityDefinition",
                true,geyserApi.getClass().getClassLoader());
            for(String identifier:List.of(ModelCatalog.HUB,ModelCatalog.RETURN)) {
                Object def=createDefinition(cls,identifier);
                call(event,"register",def);
                definitions.put(identifier,def);
            }
            definitionsReady=definitions.size()==2;
        }catch(Exception|LinkageError e){
            definitions.clear();
            getLogger().severe("Custom entity registration failed: "+e.getMessage());
        }
    }
    private Object createDefinition(Class<?> cls,String identifier)throws ReflectiveOperationException {
        ReflectiveOperationException last=null;
        for(Method m:cls.getMethods())if(m.getName().equals("of") && Modifier.isStatic(m.getModifiers()) && m.getParameterCount()==1) {
            Class<?> p=m.getParameterTypes()[0];
            try {
                Object arg;
                if(p==String.class)arg=identifier;
                else {
                    Method of=p.getMethod("of",String.class);
                    arg=of.invoke(null,identifier);
                }
                return m.invoke(null,arg);
            }catch(ReflectiveOperationException e){last=e;}
        }
        throw last!=null?last:new NoSuchMethodException("CustomEntityDefinition.of(String/Identifier)");
    }
    private void replaceProxy(Object event) {
        try {
            Object id=call(event,"uuid");
            if(!(id instanceof UUID uuid))return;
            String type=proxyTypes.get(uuid);
            if(type==null || !definitionsReady)return;
            Object def=definitions.get(type);
            if(def==null)return;
            call(event,"definition",def);
            call(event,"preSpawnConsumer",(Consumer<Object>)entity->{
                try {
                    float scale=(float)clamp(getConfig().getDouble("render.scale",1.0),.25,3);
                    override(entity,"SCALE",scale);
                    override(entity,"WIDTH",0f);
                    override(entity,"HEIGHT",0f);
                    override(entity,"VERTICAL_OFFSET",0f);
                }catch(Exception e){getLogger().warning("Custom entity data override: "+e.getMessage());}
            });
        }catch(Exception|LinkageError e){getLogger().warning("Geyser proxy spawn override: "+e.getMessage());}
    }
    private void override(Object entity,String type,Object value)throws ReflectiveOperationException {
        call(entity,"override",dataTypes.getField(type).get(null),value);
    }

    /** One sync job, no per-player scheduler, and only registered dungeon nodes. */
    private void sync() {
        if(!definitionsReady || core==null || !core.isEnabled() ||
           !getConfig().getBoolean("render.enabled",true)) {cleanupProxies();restoreAll();return;}
        Collection<?> nodes;
        try {
            Object raw=call(core,"getNodes");
            if(!(raw instanceof Collection<?> c))return;
            nodes=c;
        }catch(ReflectiveOperationException e){apiStatus="DungeonWaystone core method missing";return;}
        List<Player> clients=new ArrayList<>();
        for(Player p:Bukkit.getOnlinePlayers())if(!p.isDead()&&bedrock(p))clients.add(p);
        if(clients.isEmpty()){cleanupProxies();restoreAll();return;}
        double radius=clamp(getConfig().getDouble("render.radius-blocks",48),8,96);
        double squared=radius*radius;
        int max=clamp(getConfig().getInt("render.max-models",48),1,64);
        Set<UUID> required=new HashSet<>();
        int created=0;
        for(Object n:nodes) {
            if(created>=max)break;
            try {
                UUID id=(UUID)call(n,"id");
                String entityId=ModelCatalog.forRole(call(n,"role").toString());
                if(entityId==null)continue;
                Location loc=(Location)call(core,"location",n);
                if(loc==null||loc.getWorld()==null ||
                    !loc.getWorld().isChunkLoaded(loc.getBlockX()>>4,loc.getBlockZ()>>4))continue;
                Location modelAt=loc.clone().add(.5,clamp(getConfig().getDouble("render.offset-y",0),-2,2),.5);
                boolean near=false;
                for(Player p:clients)if(p.getWorld()==loc.getWorld()&&p.getLocation().distanceSquared(modelAt)<=squared){near=true;break;}
                if(!near)continue;
                required.add(id);created++;
                VisualProxy old=visuals.get(id);
                if(old!=null&&(!old.stand().isValid() || !old.entityId().equals(entityId)
                 || old.stand().getWorld()!=loc.getWorld() || old.stand().getLocation().distanceSquared(modelAt)>.01)){
                     removeProxy(old);old=null;
                }
                if(old==null) {
                    ArmorStand stand=loc.getWorld().spawn(modelAt,ArmorStand.class, s->{
                        s.setVisibleByDefault(false);
                        s.setVisible(true);
                        s.setMarker(true);
                        s.setGravity(false);
                        s.setSilent(true);
                        s.setInvulnerable(true);
                        s.setPersistent(false);
                        s.setCollidable(false);
                        proxyTypes.put(s.getUniqueId(),entityId);
                    });
                    old=new VisualProxy(id,stand,entityId);
                    visuals.put(id,old);
                }
            }catch(Exception|LinkageError e){getLogger().warning("Could not sync dungeon model: "+e.getMessage());}
        }
        for(VisualProxy item:List.copyOf(visuals.values()))if(!required.contains(item.nodeId()))removeProxy(item);
        for(Player p:Bukkit.getOnlinePlayers()) {
            boolean isBedrock=clients.contains(p);
            Set<UUID> now=new HashSet<>();
            for(VisualProxy item:visuals.values()) {
                if(item.stand().getWorld()!=p.getWorld())continue;
                boolean near=isBedrock && p.getLocation().distanceSquared(item.stand().getLocation())<=squared;
                if(near) {
                    p.showEntity(this,item.stand());
                    if(getConfig().getBoolean("render.hide-original-for-bedrock",true)) {
                        for(Entity e:item.stand().getWorld().getNearbyEntities(item.stand().getLocation().clone().add(0,1,0),2,3,2)) {
                            if(ownedFurniture(e,item.nodeId())) {
                                now.add(e.getUniqueId());
                                p.hideEntity(this,e);
                            }
                        }
                    }
                } else p.hideEntity(this,item.stand());
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
        visuals.remove(item.nodeId());proxyTypes.remove(item.stand().getUniqueId());item.stand().remove();
    }
    private void cleanupProxies() {for(VisualProxy item:List.copyOf(visuals.values()))removeProxy(item);}
    @EventHandler public void quit(PlayerQuitEvent event){hiddenOriginals.remove(event.getPlayer().getUniqueId());}

    private void checkPack() {
        Plugin geyser=Bukkit.getPluginManager().getPlugin("Geyser-Spigot");
        if(geyser==null)return;
        Path name=geyser.getDataFolder().toPath().resolve("packs")
           .resolve(getConfig().getString("pack.name","ArunikaDungeonWaystone-Bedrock-Models-v0.2.0.mcpack"));
        if(!Files.isRegularFile(name))getLogger().warning(
           "Dungeon Bedrock pack missing at "+name+". Install provided .mcpack then restart Geyser/server.");
    }
    @Override public boolean onCommand(CommandSender sender,Command cmd,String label,String[] args) {
        if(!sender.hasPermission("arunika.dungeonwaystone.admin")){sender.sendMessage("No permission.");return true;}
        String op=args.length==0?"status":args[0].toLowerCase(Locale.ROOT);
        if(op.equals("reload")) {reloadConfig();checkPack();sender.sendMessage("Config reloaded. Geyser entity definitions require a full server restart.");return true;}
        if(op.equals("refresh")){sync();sender.sendMessage("Dungeon Bedrock model sync requested.");return true;}
        if(!op.equals("status")){sender.sendMessage("/adwbedrock <status|refresh|reload>");return true;}
        long bedrock=Bukkit.getOnlinePlayers().stream().filter(this::bedrock).count();
        sender.sendMessage("[ADW Bedrock 0.2.0] "+apiStatus+" | registered="+definitionsReady
         +" ("+definitions.size()+"/2) | proxies="+visuals.size()+" | Bedrock online="+bedrock);
        return true;
    }
    @Override public void onDisable() {
        if(ticker!=null){ticker.cancel();ticker=null;}
        restoreAll();cleanupProxies();
        if(geyserBus!=null&&registrar!=null)try{call(geyserBus,"unsubscribeAll",registrar);}catch(Exception ignored){}
        subscriptions.clear();definitions.clear();proxyTypes.clear();
        geyserApi=null;geyserBus=null;registrar=null;dataTypes=null;
        floodgateApi=null;geyserCheck=null;floodgateCheck=null;
        definitionsReady=false;
    }
    private static boolean hasMethod(Object object,String name,Class<?>...types){
        try{object.getClass().getMethod(name,types);return true;}catch(NoSuchMethodException e){return false;}
    }
    private static Object call(Object target,String method,Object...args)throws ReflectiveOperationException {
        if(target==null)throw new IllegalStateException("null receiver for "+method);
        for(Method m:target.getClass().getMethods()){
            if(!m.getName().equals(method)||m.getParameterCount()!=args.length)continue;
            Class<?>[] types=m.getParameterTypes();boolean matches=true;
            for(int i=0;i<args.length;i++)if(args[i]!=null&&!types[i].isAssignableFrom(args[i].getClass())
              &&!(types[i].isPrimitive()&&args[i] instanceof Number)){matches=false;break;}
            if(matches)return m.invoke(target,args);
        }
        throw new NoSuchMethodException(target.getClass().getName()+"."+method);
    }
    private static int clamp(int v,int min,int max){return Math.max(min,Math.min(max,v));}
    private static double clamp(double v,double min,double max){
        return Double.isFinite(v)?Math.max(min,Math.min(max,v)):min;
    }
}
