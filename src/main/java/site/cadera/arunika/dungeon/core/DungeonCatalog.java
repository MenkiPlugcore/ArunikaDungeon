package site.cadera.arunika.dungeon.core;

import java.util.*;

/** Immutable descriptors; old floor numbers do not represent new arenas. */
public final class DungeonCatalog {
    public record Floor(String id, int number, String name, String bossReference) {}
    public static final List<Floor> INITIAL = List.of(
        new Floor("queen_bee", 1, "Queen Bee", ""),
        new Floor("mega_warden", 2, "Mega Warden", ""),
        new Floor("the_doom", 3, "The Doom", ""),
        new Floor("goblin", 4, "Goblin", "ARU_F5_Goblin_Boss"),
        new Floor("skeleton", 5, "Skeleton", "ARU_F9_SkeletonKing")
    );
    private DungeonCatalog() {}
}
