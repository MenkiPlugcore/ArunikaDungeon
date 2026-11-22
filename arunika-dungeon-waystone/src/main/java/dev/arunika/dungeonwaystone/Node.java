package dev.arunika.dungeonwaystone;

import java.util.UUID;

/** Immutable registry entry. World UUID guards against stale world-name reuse. */
public record Node(UUID id, Role role, String target, UUID worldId, String worldName, int x, int y, int z) {
    public enum Role { HUB, RETURN }
    public Node {
        if (id == null || role == null || worldId == null || worldName == null || worldName.isBlank()) throw new IllegalArgumentException("invalid node");
        if(Math.abs((long)x)>29999984||Math.abs((long)z)>29999984||y < -2048||y>2048)throw new IllegalArgumentException("node coordinates outside supported bounds");
        if(role==Role.HUB&&target==null)target="";
        if (role == Role.HUB && target != null && !target.isEmpty()) throw new IllegalArgumentException("hub cannot target arena");
        if (role == Role.RETURN && (target == null || !target.matches("[a-z][a-z0-9_]{0,47}/[a-z][a-z0-9_-]{0,47}"))) throw new IllegalArgumentException("invalid arena key");
    }
}
