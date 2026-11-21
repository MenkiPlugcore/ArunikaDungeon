package site.cadera.arunika.dungeon.core;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
/** Merely detects existing legacy inputs; never opens the live database or modifies legacy paths. */
public final class LegacyInventory {
    private static final List<String> PATHS=List.of("CdrDungeonSpawner/config.yml","CdrDungeonSpawner/floors","CdrDungeonSpawner/bosses.yml","ArunikaDungeonExchange/config.yml","ArunikaDungeonExchange/rewards.yml","ArunikaDungeonExchange/exchange.db","ArunikaDungeonExchange/exchange.db-wal","ArunikaDungeonExchange/exchange.db-shm","CdrWaystone");
    private LegacyInventory(){}
    public static List<String> inspect(Path plugins) {
        return PATHS.stream().map(p->p+": "+(Files.exists(plugins.resolve(p),LinkOption.NOFOLLOW_LINKS)?"present":"not present")).toList();
    }
    public static long countFloors(Path plugins) throws IOException {
        Path floors=plugins.resolve("CdrDungeonSpawner/floors"); if(!Files.isDirectory(floors,LinkOption.NOFOLLOW_LINKS))return 0;
        try(var entries=Files.list(floors)){return entries.filter(p->Files.isRegularFile(p,LinkOption.NOFOLLOW_LINKS)&&p.getFileName().toString().endsWith(".yml")).count();}
    }
}
