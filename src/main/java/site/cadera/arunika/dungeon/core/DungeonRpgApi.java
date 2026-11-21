package site.cadera.arunika.dungeon.core;
import java.util.*;
/** Contract for a separately implemented Nusa Karsa adapter. Provider must deduplicate session/player IDs durably. Main-thread callback, no blocking I/O. */
public interface DungeonRpgApi {
 int apiVersion();
 record BossClear(UUID session,String floor,SessionRegistry.Mode mode,UUID player,int classExp,boolean awakeningProgress){public BossClear{Objects.requireNonNull(session);Objects.requireNonNull(player);Objects.requireNonNull(floor);Objects.requireNonNull(mode);if(classExp<0||classExp>100000)throw new IllegalArgumentException("EXP range");}public String eventId(){return session+":"+player;}}
 void bossClear(BossClear event);
}
