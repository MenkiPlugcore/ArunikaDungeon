package site.cadera.arunika.dungeon.paper;
import java.io.IOException;
import java.util.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import site.cadera.arunika.dungeon.privategame.TicketLedger;

final class PaperTickets {
 final TicketLedger ledger;private final NamespacedKey serial,generation;private final Material material;private final String name;
 PaperTickets(JavaPlugin plugin,TicketLedger ledger,String material,String name){this.ledger=ledger;this.material=Material.valueOf(material);if(!this.material.isItem()||this.material.isAir())throw new IllegalArgumentException("Invalid ticket material");this.name=name;serial=new NamespacedKey(plugin,"dungeon_ticket_serial");generation=new NamespacedKey(plugin,"dungeon_ticket_generation");}
 Optional<TicketLedger.Ticket> read(ItemStack item){if(item==null||item.getType().isAir()||!item.hasItemMeta())return Optional.empty();var p=item.getItemMeta().getPersistentDataContainer();try{return Optional.of(new TicketLedger.Ticket(UUID.fromString(p.get(serial,PersistentDataType.STRING)),p.get(generation,PersistentDataType.INTEGER)));}catch(RuntimeException e){return Optional.empty();}}
 TicketLedger.Ticket hand(Player p){return read(p.getInventory().getItemInMainHand()).orElseThrow(()->new IllegalStateException("Hold a Universal Dungeon Ticket"));}
 boolean has(Player p,TicketLedger.Ticket t)throws IOException{return p!=null&&read(p.getInventory().getItemInMainHand()).filter(t::equals).isPresent()&&ledger.availableFor(t,p.getUniqueId());}
 void remove(Player p,TicketLedger.Ticket t){var item=p.getInventory().getItemInMainHand();if(!read(item).filter(t::equals).isPresent())throw new IllegalStateException("Ticket moved");if(item.getAmount()==1)p.getInventory().setItemInMainHand(new ItemStack(Material.AIR));else item.setAmount(item.getAmount()-1);}
 ItemStack item(TicketLedger.Ticket t){var item=new ItemStack(material);var meta=item.getItemMeta();meta.displayName(net.kyori.adventure.text.Component.text(name));var p=meta.getPersistentDataContainer();p.set(serial,PersistentDataType.STRING,t.serial().toString());p.set(generation,PersistentDataType.INTEGER,t.generation());item.setItemMeta(meta);return item;}
 void deliver(Player p,TicketLedger.Ticket t)throws IOException{if(!p.getInventory().addItem(item(t)).isEmpty()){p.sendMessage("Inventory full. /adg ticket claim "+t.serial());return;}p.saveData();ledger.acknowledge(t,p.getUniqueId());p.sendMessage("Universal Dungeon Ticket delivered.");}
}
