package dev.arunika.dungeonwaystone;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Reuses the original CdrWaystone ItemsAdder furniture IDs without owning any survival records.
 * Only dedicated HUB and RETURN nodes are ever modified. Does not add new models or packs.
 * The two native barrier hitboxes make interactions possible when the model entity is a marker.
 */
final class DungeonFurnitureVisuals {
    private final DungeonWaystonePlugin plugin;
    private final NamespacedKey marker;
    private final NamespacedKey markerId;
    private final Map<String,String> skins = new LinkedHashMap<>();
    private Class<?> furnitureType;
    private Method precise, blockSpawn, byEntity, getEntity, getArmorstand, remove;
    private Plugin itemsAdder;
    private final Map<UUID,Long> nextWarning=new LinkedHashMap<>();
    private void warn(Node n,String message){long now=System.nanoTime();Long next=nextWarning.get(n.id());if(next!=null&&now<next)return;if(nextWarning.size()>=128)nextWarning.remove(nextWarning.keySet().iterator().next());nextWarning.put(n.id(),now+60_000_000_000L);plugin.getLogger().warning(message);}

    DungeonFurnitureVisuals(DungeonWaystonePlugin plugin) {
        this.plugin = plugin;
        marker = new NamespacedKey(plugin, "model_owner");
        markerId = new NamespacedKey(plugin, "model_node_id");
        File file = new File(plugin.getDataFolder(), "skins.yml");
        if (!file.exists()) plugin.saveResource("skins.yml", false);
        var section = YamlConfiguration.loadConfiguration(file).getConfigurationSection("skins");
        if (section != null) for (String key:section.getKeys(false)) {
            String val=section.getString(key);
            if (val!=null && val.matches("[a-z0-9_]+:[a-z0-9_]+")) skins.put(key.toLowerCase(java.util.Locale.ROOT),val);
        }
    }
    boolean enabled() { return plugin.getConfig().getBoolean("visuals.enabled", true); }
    boolean supported() { Plugin provider=Bukkit.getPluginManager().getPlugin("ItemsAdder");return provider!=null&&provider.isEnabled(); }
    private boolean loaded(Node n) {
        World w=Bukkit.getWorld(n.worldId());
        return w!=null&&w.isChunkLoaded(n.x()>>4,n.z()>>4);
    }
    private Location location(Node n){return plugin.location(n);}
    private String furnitureId(Node n) {
        String sel=plugin.getConfig().getString(n.role()==Node.Role.HUB?"visuals.hub-skin":"visuals.return-skin",n.role()==Node.Role.HUB?"blackstone":"portstone");
        if(sel==null)return null;
        if(sel.matches("[a-z0-9_]+:[a-z0-9_]+"))return sel;
        return skins.get(sel.toLowerCase(java.util.Locale.ROOT));
    }
    private void api() throws ReflectiveOperationException {
        Plugin provider=Bukkit.getPluginManager().getPlugin("ItemsAdder");
        if(provider==null||!provider.isEnabled()) throw new ClassNotFoundException("ItemsAdder plugin not enabled");
        if(provider==itemsAdder&&furnitureType!=null)return;
        Class<?> type=Class.forName("dev.lone.itemsadder.api.CustomFurniture",true,provider.getClass().getClassLoader());
        Method p=null,b=null,ge=null,ga=null;
        try {p=type.getMethod("spawnPreciseNonSolid",String.class,Location.class);}catch(NoSuchMethodException ignored){}
        try {b=type.getMethod("spawn",String.class,Block.class);}catch(NoSuchMethodException ignored){}
        try {ge=type.getMethod("getEntity");}catch(NoSuchMethodException ignored){}
        try {ga=type.getMethod("getArmorstand");}catch(NoSuchMethodException ignored){}
        if((p==null&&b==null)||(ge==null&&ga==null))throw new NoSuchMethodException("ItemsAdder furniture spawn/entity API unavailable");
        Method e=type.getMethod("byAlreadySpawned",Entity.class);
        Method r=type.getMethod("remove",boolean.class);
        furnitureType=type;precise=p;blockSpawn=b;getEntity=ge;getArmorstand=ga;byEntity=e;remove=r;itemsAdder=provider;
    }
    private Entity find(Node n) {
        if(!loaded(n))return null;
        Location base=location(n);if(base==null)return null;
        for(Entity e:base.getWorld().getNearbyEntities(base.clone().add(.5,1,.5),2,3,2)){
            if(isOwned(e)&&n.id().toString().equals(e.getPersistentDataContainer().get(markerId,PersistentDataType.STRING)))return e;
        }
        return null;
    }
    Node node(Entity entity) {
        if(entity==null||!isOwned(entity))return null;
        try {
            String str=entity.getPersistentDataContainer().get(markerId,PersistentDataType.STRING);
            Node n=str==null?null:plugin.getNode(UUID.fromString(str));
            return n!=null&&plugin.dedicated(n)?n:null;
        }catch(IllegalArgumentException ignored){return null;}
    }
    private boolean isOwned(Entity e) {
        return e!=null&&Byte.valueOf((byte)1).equals(e.getPersistentDataContainer().get(marker,PersistentDataType.BYTE));
    }
    /** Returns false rather than destroying the existing Lodestone if ItemsAdder is absent. */
    boolean materialize(Node n) {
        if(n==null||plugin.getNode(n.id())!=n||!loaded(n))return false;
        if(!enabled()||!supported()){remove(n,true);return false;}
        Location base=location(n);if(base==null)return false;
        Block bottom=base.getBlock(), top=bottom.getRelative(0,1,0);
        Material b=bottom.getType(),t=top.getType();
        if(!(b==Material.LODESTONE||b==Material.BARRIER))return false;
        if(!(t==Material.AIR||(b==Material.BARRIER&&t==Material.BARRIER)))return false;
        if(find(n)!=null)return true;
        String furniture=furnitureId(n);
        if(furniture==null){warn(n,"Invalid model skin for "+n.role()+" (check skins.yml)");return false;}
        try {
            api();
            // Ensure the original model is not hidden by the native anchor.
            bottom.setType(Material.AIR,false);
            if(t==Material.BARRIER)top.setType(Material.AIR,false);
            Object custom=null;
            if(precise!=null){Location pos=base.clone().add(.5,0,.5);pos.setYaw((float)plugin.getConfig().getDouble("visuals.rotation-degrees",0));custom=precise.invoke(null,furniture,pos);}
            if(custom==null&&blockSpawn!=null)custom=blockSpawn.invoke(null,furniture,bottom);
            Entity entity=null;
            if(custom!=null&&getEntity!=null){Object raw=getEntity.invoke(custom);if(raw instanceof Entity e)entity=e;}
            if(entity==null&&custom!=null&&getArmorstand!=null){Object raw=getArmorstand.invoke(custom);if(raw instanceof Entity e)entity=e;}
            if(entity==null){if(custom!=null)try{remove.invoke(custom,false);}catch(Exception ignored){}bottom.setType(Material.LODESTONE,false);if(t==Material.BARRIER)top.setType(Material.AIR,false);warn(n,"Furniture unavailable: "+furniture+"; Lodestone preserved");return false;}
            entity.getPersistentDataContainer().set(marker,PersistentDataType.BYTE,(byte)1);
            entity.getPersistentDataContainer().set(markerId,PersistentDataType.STRING,n.id().toString());
            entity.setInvulnerable(true);entity.setPersistent(false);
            bottom.setType(Material.BARRIER,false);top.setType(Material.BARRIER,false);
            return true;
        }catch(ReflectiveOperationException|LinkageError|RuntimeException ex){
            bottom.setType(Material.LODESTONE,false);if(t==Material.BARRIER)top.setType(Material.AIR,false);else top.setType(t,false);
            warn(n,"Model "+furniture+" failed: "+ex.getMessage()+". Anchor retained.");
            return false;
        }
    }
    void refreshLoaded() {
        for(Node n:plugin.getNodes())if(loaded(n))materialize(n);
    }
    void remove(Node n,boolean restoreLodestone) {
        if(n==null||!loaded(n))return;
        Entity e=find(n);
        if(e!=null){
            try {api();Object furniture=byEntity.invoke(null,e);if(furniture!=null)remove.invoke(furniture,false);else e.remove();}
            catch(ReflectiveOperationException|LinkageError|RuntimeException ex){e.remove();}
        }
        if(restoreLodestone){
            Block bottom=location(n).getBlock(), top=bottom.getRelative(0,1,0);
            // Only registered waystone positions are restored; never change arbitrary neighbors.
            if(bottom.getType()==Material.BARRIER){bottom.setType(Material.LODESTONE,false);if(top.getType()==Material.BARRIER)top.setType(Material.AIR,false);}
        }
    }
    void removeAll(boolean restore) {
        for(Node n:new ArrayList<>(plugin.getNodes()))remove(n,restore);
        itemsAdder=null;furnitureType=null;precise=null;blockSpawn=null;getEntity=null;getArmorstand=null;byEntity=null;remove=null;
    }
}
