package site.cadera.arunika.dungeon.battle;
/**
 * Pure fail-closed boundary policy for player teleport transitions.
 * An exact, short-lived server-issued teleport permit is checked by the caller
 * before this policy; it is NOT equivalent to a command or plugin teleport.
 */
public final class TeleportSafety {
    private TeleportSafety() {}
    public static boolean allowExternal(boolean coreAllowsDestination,
                                        boolean livingParticipant,
                                        boolean sourceInsideOwnArena,
                                        boolean destinationInsideOwnArena,
                                        boolean commandCause) {
        if (!coreAllowsDestination) return false;
        if (!livingParticipant) return true;
        // No /tp even within arena; non-command skill teleports must stay inside.
        return !commandCause && sourceInsideOwnArena && destinationInsideOwnArena;
    }
}
