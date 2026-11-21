package site.cadera.arunika.dungeon.core;
/** RC3 admission gate; legacy data directories are allowed but its running/installed JAR is not. */
public final class StagingPolicy {
    private StagingPolicy() {}
    public static void requireBattle(boolean legacySpawnerPresent, boolean mythicEnabled) {
        if (legacySpawnerPresent) throw new IllegalStateException("Remove the old CdrDungeonSpawner JAR from the stopped staging clone, retain its data folder");
        if (!mythicEnabled) throw new IllegalStateException("MythicMobs not enabled");
    }
}
