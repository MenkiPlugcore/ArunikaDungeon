package dev.arunika.dungeonwaystone;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Dungeon-only anchor registry. This plugin NEVER opens a GUI or teleports a player.
 *  All dungeon game rules and interaction are owned exclusively by ArunikaDungeon.
 */
public final class DungeonWaystonePlugin extends JavaPlugin implements Listener, TabExecutor {
    private final Map<UUID,Node> nodes = new LinkedHashMap<>();
    private final Map<String,UUID> byBlock = new HashMap<>();
    private File storage;
    private boolean ready;
    private DungeonFurnitureVisuals visuals;

    @Override public void onEnable() {
        saveDefaultConfig();
        storage = new File(getDataFolder(), "anchors.yml");
        try { readStorage(); ready = true; }
        catch (Exception e) { ready = false; getLogger().severe("Waystone registry locked to prevent data loss: " + e.getMessage()); }
        visuals = new DungeonFurnitureVisuals(this);
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getScheduler().runTaskTimer(this, () -> { if (ready && visuals != null) visuals.refreshLoaded(); }, 100L, 100L);
        if (getCommand("adw") != null) { getCommand("adw").setExecutor(this); getCommand("adw").setTabCompleter(this); }
        getLogger().info("Dungeon-only node registry: " + nodes.size() + " anchor(s), ready=" + ready + "; survival Waystones untouched");
    }
    @Override public void onDisable() { if (visuals != null) visuals.removeAll(true); ready = false; nodes.clear(); byBlock.clear(); }
    public boolean available() { return ready && isEnabled(); }
    public List<Node> getNodes() { return ready ? List.copyOf(nodes.values()) : List.of(); }
    public Node getNode(UUID id) { return ready && id != null ? nodes.get(id) : null; }
    public Node getNode(Block block) {
        if (!ready || block == null || (block.getType() != Material.LODESTONE && block.getType() != Material.BARRIER)) return null;
        UUID id = byBlock.get(key(block.getWorld().getUID(),block.getX(),block.getY(),block.getZ()));
        if (id == null && block.getType() == Material.BARRIER) id = byBlock.get(key(block.getWorld().getUID(),block.getX(),block.getY()-1,block.getZ()));
        Node n = id == null ? null : nodes.get(id);
        return n != null && validAnchor(n) ? n : null;
    }
    /** Return only model entities carrying our plugin-specific PDC ownership marker. */
    public Node getNode(Entity entity) {
        if(!ready||entity==null)return null;
        String proxy=entity.getPersistentDataContainer().get(new org.bukkit.NamespacedKey("arunikadungeonwaystonebedrock","proxy_node"),org.bukkit.persistence.PersistentDataType.STRING);
        if(proxy!=null)try{
            Node n=getNode(UUID.fromString(proxy));Location loc=location(n);
            return dedicated(n)&&loc!=null&&entity.getWorld()==loc.getWorld()&&entity.getLocation().distanceSquared(loc.clone().add(.5,0,.5))<=9?n:null;
        }catch(IllegalArgumentException e){return null;}
        return visuals!=null?visuals.node(entity):null;
    }
    public UUID id(Node n) { return n == null ? null : n.id(); }
    public Location location(Node n) {
        if (n == null) return null;
        World w = Bukkit.getWorld(n.worldId());
        return w == null ? null : new Location(w,n.x(),n.y(),n.z());
    }
    public boolean dedicated(Node n) {
        return available() && n != null && nodes.get(n.id()) == n && validAnchor(n) && !legacyCollision(location(n));
    }
    public boolean isHub(Node n) { return dedicated(n) && n.role() == Node.Role.HUB; }
    public boolean isReturn(Node n,String target) { return dedicated(n) && n.role() == Node.Role.RETURN && n.target().equals(target); }
    /** Authoritative distance and permission validation; no combat/session mutation here. */
    public boolean allowed(Player p,Node n,double radius,boolean entry) {
        if (p == null || !p.isOnline() || p.isDead() || !p.hasPermission("arunika.dungeonwaystone.use") || !dedicated(n)) return false;
        if (!Double.isFinite(radius) || radius < 1 || radius > 16) return false;
        Location l=location(n);
        if (l==null || p.getWorld()!=l.getWorld() || !l.getWorld().isChunkLoaded(l.getBlockX()>>4,l.getBlockZ()>>4)) return false;
        if (p.getLocation().distanceSquared(l.clone().add(.5,.5,.5)) > radius*radius) return false;
        Location eye=p.getEyeLocation(),center=l.clone().add(.5,1,.5);
        org.bukkit.util.Vector delta=center.toVector().subtract(eye.toVector());double distance=delta.length();
        if(distance>.001){
            var hit=l.getWorld().rayTraceBlocks(eye,delta.multiply(1/distance),distance,org.bukkit.FluidCollisionMode.NEVER,true);
            if(hit!=null&&hit.getHitBlock()!=null){Block block=hit.getHitBlock();if(block.getX()!=n.x()||block.getZ()!=n.z()||(block.getY()!=n.y()&&block.getY()!=n.y()+1))return false;}
        }
        return !entry || (isHub(n) && !p.isFlying() && p.getGameMode()!=GameMode.CREATIVE && p.getGameMode()!=GameMode.SPECTATOR);
    }
    private boolean validAnchor(Node n) {
        Location l=location(n); return l!=null && l.getWorld().isChunkLoaded(n.x()>>4,n.z()>>4) && (l.getBlock().getType()==Material.LODESTONE || l.getBlock().getType()==Material.BARRIER);
    }
    private static String key(UUID world,int x,int y,int z){return world+":"+x+":"+y+":"+z;}
    private String key(Node n){return key(n.worldId(),n.x(),n.y(),n.z());}
    private static String key(Block b){return key(b.getWorld().getUID(),b.getX(),b.getY(),b.getZ());}
    private void readStorage() throws IOException {
        nodes.clear(); byBlock.clear();
        if (!storage.exists()) return;
        YamlConfiguration y=YamlConfiguration.loadConfiguration(storage);
        if (y.getInt("schema",-1)!=1) throw new IOException("unsupported storage schema");
        ConfigurationSection root=y.getConfigurationSection("nodes");
        if (root==null && y.contains("nodes")) throw new IOException("corrupt nodes section");
        if (root==null) return;
        if (root.getKeys(false).size() > 10000) throw new IOException("oversize registry");
        Set<String> used=new HashSet<>(); int hubs=0;
        for (String raw:root.getKeys(false)) {
            try {
                UUID id=UUID.fromString(raw);
                ConfigurationSection s=root.getConfigurationSection(raw);
                if(s==null) throw new IllegalArgumentException("invalid entry");
                Node.Role role=Node.Role.valueOf(s.getString("role",""));
                UUID world=UUID.fromString(s.getString("world-id",""));
                for(String coord:List.of("x","y","z"))if(!s.isInt(coord))throw new IllegalArgumentException("Missing or invalid coordinate "+coord);
                Node n=new Node(id,role,s.getString("target",""),world,s.getString("world-name",""),s.getInt("x"),s.getInt("y"),s.getInt("z"));
                if (!used.add(key(n))) throw new IllegalArgumentException("duplicate block");
                if (role==Node.Role.HUB) hubs++;
                nodes.put(id,n); byBlock.put(key(n),id);
            } catch (RuntimeException e) { nodes.clear(); byBlock.clear(); throw new IOException("invalid entry " + raw,e); }
        }
        if (hubs>1) throw new IOException("multiple hub nodes");
    }
    /** Atomic replace; if write fails keep old registry and refuse mutation. */
    private void persist(Map<UUID,Node> next) throws IOException {
        YamlConfiguration y=new YamlConfiguration(); y.set("schema",1);
        for(Node n:next.values()){
            String p="nodes."+n.id()+".";
            y.set(p+"role",n.role().name());y.set(p+"target",n.target());y.set(p+"world-id",n.worldId().toString());y.set(p+"world-name",n.worldName());
            y.set(p+"x",n.x());y.set(p+"y",n.y());y.set(p+"z",n.z());
        }
        Files.createDirectories(getDataFolder().toPath());
        Path tmp=Files.createTempFile(getDataFolder().toPath(),"anchors-",".tmp");
        try {
            Files.writeString(tmp,y.saveToString());
            try(var ch=java.nio.channels.FileChannel.open(tmp,java.nio.file.StandardOpenOption.WRITE)){ch.force(true);}
            try{Files.move(tmp,storage.toPath(),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
            catch(AtomicMoveNotSupportedException e){Files.move(tmp,storage.toPath(),StandardCopyOption.REPLACE_EXISTING);}
            try(var ch=java.nio.channels.FileChannel.open(storage.toPath().getParent(),java.nio.file.StandardOpenOption.READ)){ch.force(true);}catch(IOException|UnsupportedOperationException ignored){}
        } finally {Files.deleteIfExists(tmp);}
        nodes.clear();nodes.putAll(next);byBlock.clear();for(Node n:nodes.values())byBlock.put(key(n),n.id());
    }
    private boolean legacyCollision(Location loc) {
        if(loc==null) return true;
        Plugin legacy=Bukkit.getPluginManager().getPlugin("CdrWaystone");
        if(legacy==null||!legacy.isEnabled())return false;
        try {
            Object registry=legacy.getClass().getMethod("registry").invoke(legacy);
            Object hit=registry.getClass().getMethod("find",Location.class).invoke(registry,loc);
            return hit!=null;
        } catch(Exception | LinkageError e) {return true;} // fail closed, no collisions
    }
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args) {
        if(!sender.hasPermission("arunika.dungeonwaystone.admin")){sender.sendMessage("No permission.");return true;}
        if(!ready){sender.sendMessage("Dungeon Waystone registry is locked. Inspect logs; do not reset data.");return true;}
        String action=args.length==0?"status":args[0].toLowerCase(Locale.ROOT);
        if (action.equals("refresh")) { visuals.refreshLoaded(); sender.sendMessage("Refreshed loaded Dungeon Waystone models."); return true; }
        if (action.equals("status")||action.equals("check")) {
            long broken=nodes.values().stream().filter(n->!validAnchor(n)).count();
            long collisions=nodes.values().stream().filter(n->legacyCollision(location(n))).count();
            sender.sendMessage("[Dungeon Waystone] nodes="+nodes.size()+" hub="+nodes.values().stream().filter(n->n.role()==Node.Role.HUB).count()+" broken="+broken+" legacy-collision="+collisions);
            sender.sendMessage("ArunikaDungeon="+pluginStatus("ArunikaDungeon")+" | Bedrock="+pluginStatus("ArunikaDungeonWaystoneBedrock"));return true;
        }
        if (action.equals("list")) { for(Node n:nodes.values())sender.sendMessage(n.role()+" "+n.id()+" "+n.target()+" @ "+n.worldName()+" "+n.x()+","+n.y()+","+n.z()); if(nodes.isEmpty())sender.sendMessage("No dungeon anchors registered.");return true; }
        if (action.equals("export")) {if(!(sender instanceof Player player)){sender.sendMessage("Stand at the safe Dungeon Hub return point, then /adw export.");return true;}try{writeExport(player.getLocation());sender.sendMessage("Export written to plugins/ArunikaDungeonWaystone/waystone-export.yml. Review before copying to ArunikaDungeon/integrations/waystone.yml");}catch(IOException e){sender.sendMessage("Export error: "+e.getMessage());}return true;}
        if(action.equals("model")){
            if(args.length!=3||!Set.of("hub","return").contains(args[1].toLowerCase(Locale.ROOT))||!args[2].matches("[a-z0-9_]+(:[a-z0-9_]+)?")){sender.sendMessage("/adw model <hub|return> <skin-or-namespaced-ID> (Java ItemsAdder)");return true;}
            visuals.removeAll(true);getConfig().set("visuals."+args[1].toLowerCase(Locale.ROOT)+"-skin",args[2]);saveConfig();visuals.refreshLoaded();sender.sendMessage("Java model selection saved. Bedrock keeps original Blackstone/Portstone models.");return true;
        }
        if(!(sender instanceof Player p)){sender.sendMessage("Player required. Stand near a Lodestone.");return true;}
        Block b=p.getTargetBlockExact(8);
        if(action.equals("inspect")){Node n=b==null?null:getNode(b);sender.sendMessage(n==null?"No Dungeon Waystone at target":n.role()+" "+n.id()+" "+n.target());return true;}
        if(b==null){sender.sendMessage("Look at a Lodestone block within 8 blocks.");return true;}
        if(action.equals("move")){
            if(args.length!=2){sender.sendMessage("/adw move <uuid> while looking at a NEW Lodestone");return true;}
            try{
                Node old=getNode(UUID.fromString(args[1]));
                if(old==null||b.getType()!=Material.LODESTONE||b.getRelative(0,1,0).getType()!=Material.AIR||byBlock.containsKey(key(b))||legacyCollision(b.getLocation()))throw new IllegalArgumentException("Invalid UUID or destination anchor");
                Location oldAt=location(old);if(oldAt==null||!oldAt.getWorld().isChunkLoaded(old.x()>>4,old.z()>>4))throw new IllegalArgumentException("Load the original anchor chunk before moving");
                Node moved=new Node(old.id(),old.role(),old.target(),b.getWorld().getUID(),b.getWorld().getName(),b.getX(),b.getY(),b.getZ());
                Map<UUID,Node> next=new LinkedHashMap<>(nodes);next.put(old.id(),moved);persist(next);visuals.remove(old,true);visuals.materialize(moved);
                sender.sendMessage("Moved UUID "+old.id()+". Re-export/import bindings and reload gateway before use.");
            }catch(Exception ex){sender.sendMessage("Move rejected: "+ex.getMessage());}return true;
        }
        if(action.equals("remove")) {Node n=getNode(b);if(n==null){sender.sendMessage("No Dungeon Waystone at target.");return true;}try{Map<UUID,Node> next=new LinkedHashMap<>(nodes);next.remove(n.id());persist(next);visuals.remove(n,true);sender.sendMessage("Unregistered "+n.id());}catch(IOException ex){sender.sendMessage("Save failed; registry unchanged: "+ex.getMessage());}return true;}
        if(!action.equals("hub")&&!action.equals("return")){sender.sendMessage("/adw [hub|return <floor> <arena>|move <uuid>|model <hub|return> <skin>|remove|list|status|inspect|export|check|refresh]");return true;}
        if(b.getType()!=Material.LODESTONE){sender.sendMessage("Target must be a vanilla Lodestone (native Java + Bedrock interaction).");return true;}
        if(b.getRelative(0,1,0).getType()!=Material.AIR){sender.sendMessage("The block above the Lodestone must be AIR for the original model hitbox.");return true;}
        if(legacyCollision(b.getLocation())){sender.sendMessage("This Lodestone belongs to CdrWaystone. Use a NEW Lodestone; survival data remains untouched.");return true;}
        if(byBlock.containsKey(key(b))){sender.sendMessage("Dungeon Waystone already registered here. Use /adw remove first.");return true;}
        if(nodes.size()>=Math.min(128,Math.max(1,getConfig().getInt("max-nodes",64)))){sender.sendMessage("Maximum Dungeon Waystones reached.");return true;}
        Node.Role role=action.equals("hub")?Node.Role.HUB:Node.Role.RETURN;
        String target="";
        if(role==Node.Role.RETURN){if(args.length!=3){sender.sendMessage("Usage: /adw return <floor> <arena>");return true;}target=args[1]+"/"+args[2];if(!target.matches("[a-z][a-z0-9_]{0,47}/[a-z][a-z0-9_-]{0,47}")){sender.sendMessage("Invalid floor/arena key");return true;}}
        if(role==Node.Role.HUB && nodes.values().stream().anyMatch(n->n.role()==Node.Role.HUB)){sender.sendMessage("One HUB only. Remove old HUB first.");return true;}
        if(role==Node.Role.RETURN){final String t=target;if(nodes.values().stream().anyMatch(n->n.role()==Node.Role.RETURN&&n.target().equals(t))){sender.sendMessage("Arena already has a Return Waystone.");return true;}}
        Node n=new Node(UUID.randomUUID(),role,target,b.getWorld().getUID(),b.getWorld().getName(),b.getX(),b.getY(),b.getZ());
        try{Map<UUID,Node> next=new LinkedHashMap<>(nodes);next.put(n.id(),n);persist(next);if(visuals.enabled() && !visuals.materialize(n))sender.sendMessage("§eModel not loaded: check ItemsAdder and skins.yml; Lodestone fallback active.");sender.sendMessage("Created "+role+" UUID="+n.id()+" target="+target+". Run /adw export, then /adg reload all.");}
        catch(IOException ex){sender.sendMessage("Save failed; registry unchanged: "+ex.getMessage());}
        return true;
    }
    private String pluginStatus(String name){Plugin p=Bukkit.getPluginManager().getPlugin(name);return p==null?"missing":p.getDescription().getVersion()+"/enabled="+p.isEnabled();}
    private void writeExport(Location destination)throws IOException{
        Node hub=nodes.values().stream().filter(n->n.role()==Node.Role.HUB).findFirst().orElseThrow(()->new IOException("register HUB first"));
        Location at=location(hub);
        if(at==null||destination.getWorld()!=at.getWorld()||destination.distanceSquared(at)>36||!destination.getBlock().isPassable()||!destination.clone().add(0,1,0).getBlock().isPassable())throw new IOException("Stand on a safe return point within 6 blocks of HUB; do not stand inside the Waystone");
        YamlConfiguration y=new YamlConfiguration();y.set("schema-version",2);y.set("enabled",false);y.set("gateway-id",hub.id().toString());
        y.set("hub.world",destination.getWorld().getName());y.set("hub.x",destination.getX());y.set("hub.y",destination.getY());y.set("hub.z",destination.getZ());
        y.set("interaction-radius",Math.max(2,Math.min(16,getConfig().getDouble("interaction-radius",6))));
        for(Node n:nodes.values())if(n.role()==Node.Role.RETURN)y.set("return-waystones."+n.id(),n.target());
        y.save(new File(getDataFolder(),"waystone-export.yml"));
    }
    @Override public List<String> onTabComplete(CommandSender sender,Command command,String alias,String[] args){return args.length==1?List.of("hub","return","remove","list","status","inspect","export","check","refresh","move","model").stream().filter(a->a.startsWith(args[0].toLowerCase(Locale.ROOT))).toList():List.of();}
    @EventHandler public void onChunkLoad(org.bukkit.event.world.ChunkLoadEvent e) {
        for (Node n : getNodes()) if (n.worldId().equals(e.getWorld().getUID()) && (n.x() >> 4) == e.getChunk().getX() && (n.z() >> 4) == e.getChunk().getZ()) {
            getServer().getScheduler().runTask(this, () -> { if (ready && visuals != null) visuals.materialize(n); });
        }
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void onChunkUnload(org.bukkit.event.world.ChunkUnloadEvent e){
        if(visuals==null)return;
        for(Node n:getNodes())if(n.worldId().equals(e.getWorld().getUID())&&(n.x()>>4)==e.getChunk().getX()&&(n.z()>>4)==e.getChunk().getZ())visuals.remove(n,true);
    }
    @EventHandler public void onProviderDisabled(org.bukkit.event.server.PluginDisableEvent e){
        if(e.getPlugin().getName().equals("ItemsAdder")&&visuals!=null)visuals.removeAll(true);
    }
    @EventHandler public void onProviderEnabled(org.bukkit.event.server.PluginEnableEvent e) {
        if (e.getPlugin().getName().equals("ItemsAdder")) getServer().getScheduler().runTaskLater(this, () -> { if(ready && visuals != null) visuals.refreshLoaded(); }, 80L);
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void blockBreak(BlockBreakEvent e){if(getNode(e.getBlock())!=null){e.setCancelled(true);e.getPlayer().sendMessage("§cDungeon Waystone is protected. Use /adw remove first.");}}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void entityExplode(EntityExplodeEvent e){e.blockList().removeIf(b->getNode(b)!=null);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void blockExplode(BlockExplodeEvent e){e.blockList().removeIf(b->getNode(b)!=null);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void pistonExtend(BlockPistonExtendEvent e){if(e.getBlocks().stream().anyMatch(b->getNode(b)!=null))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void pistonRetract(BlockPistonRetractEvent e){if(e.getBlocks().stream().anyMatch(b->getNode(b)!=null))e.setCancelled(true);}
}
