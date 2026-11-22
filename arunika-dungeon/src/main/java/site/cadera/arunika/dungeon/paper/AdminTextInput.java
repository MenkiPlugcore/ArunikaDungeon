package site.cadera.arunika.dungeon.paper;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
/** One short, expiring input per admin. Chat is cancelled only for an active prompt. */
final class AdminTextInput implements Listener,AutoCloseable {
 private record Request(long until,Consumer<String> done,java.util.function.BooleanSupplier allowed){}
 private final ArunikaDungeonPlugin plugin;private final Map<UUID,Request> pending=new ConcurrentHashMap<>();
 AdminTextInput(ArunikaDungeonPlugin plugin){this.plugin=plugin;Bukkit.getPluginManager().registerEvents(this,plugin);}
 void ask(Player p,String label,java.util.function.BooleanSupplier allowed,Consumer<String> done){
  if(!allowed.getAsBoolean())return;if(pending.size()>=128&&!pending.containsKey(p.getUniqueId())){p.sendMessage("Terlalu banyak input admin; coba lagi");return;}
  pending.put(p.getUniqueId(),new Request(System.nanoTime()+60_000_000_000L,done,allowed));p.closeInventory();p.sendMessage(label+" — ketik di chat dalam 60 detik. Ketik batal untuk membatalkan.");
 }
 @EventHandler(priority=EventPriority.LOWEST) public void chat(AsyncPlayerChatEvent e){
  UUID id=e.getPlayer().getUniqueId();Request r=pending.get(id);if(r==null)return;
  if(System.nanoTime()-r.until()>=0){pending.remove(id,r);return;}
  if(!pending.remove(id,r))return;e.setCancelled(true);String value=e.getMessage();Player p=e.getPlayer();
  Bukkit.getScheduler().runTask(plugin,()->{if(!p.isOnline()||!r.allowed().getAsBoolean())return;if(value.equalsIgnoreCase("batal")){p.sendMessage("Dibatalkan");return;}try{if(value.length()>160)throw new IllegalArgumentException("Input terlalu panjang");r.done().accept(value);}catch(RuntimeException ex){p.sendMessage("Input ditolak: "+ex.getMessage());}});
 }
 @EventHandler public void quit(PlayerQuitEvent e){pending.remove(e.getPlayer().getUniqueId());}
 void expire(){long now=System.nanoTime();pending.entrySet().removeIf(e->now-e.getValue().until()>=0);}
 @Override public void close(){pending.clear();HandlerList.unregisterAll(this);}
}
