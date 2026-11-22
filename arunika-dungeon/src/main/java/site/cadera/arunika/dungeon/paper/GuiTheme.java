package site.cadera.arunika.dungeon.paper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
/** Shared native-inventory palette; no custom fonts, animations or extra scheduler. */
final class GuiTheme {
 private GuiTheme(){}
 static Component name(String text,Material material){
  NamedTextColor color=switch(material){
   case EMERALD,LIME_DYE,LIME_CONCRETE -> NamedTextColor.GREEN;
   case AMETHYST_SHARD,PURPLE_STAINED_GLASS_PANE -> NamedTextColor.LIGHT_PURPLE;
   case BARRIER,RED_DYE,RED_CONCRETE -> NamedTextColor.RED;
   case GOLD_INGOT,COMPASS,ARROW -> NamedTextColor.GOLD;
   default -> NamedTextColor.WHITE;
  };
  return Component.text(text,color).decoration(TextDecoration.ITALIC,false);
 }
 static Component lore(String text){return Component.text(text,NamedTextColor.GRAY).decoration(TextDecoration.ITALIC,false);}
}
