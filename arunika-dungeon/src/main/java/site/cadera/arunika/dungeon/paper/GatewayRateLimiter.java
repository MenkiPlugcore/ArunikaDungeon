package site.cadera.arunika.dungeon.paper;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Single main-thread use only. Bounded per-player gesture limiter; no tasks or retained Player objects. */
final class GatewayRateLimiter {
    private final long cooldownNanos;
    private final int maxEntries;
    private final LinkedHashMap<UUID, Long> committed = new LinkedHashMap<>();

    GatewayRateLimiter(long cooldownNanos, int maxEntries) {
        if (cooldownNanos <= 0 || maxEntries < 1) throw new IllegalArgumentException("Invalid limiter limits");
        this.cooldownNanos = cooldownNanos;
        this.maxEntries = maxEntries;
    }

    boolean ready(UUID player, long now) {
        Objects.requireNonNull(player, "player");
        Long last = committed.get(player);
        return last == null || now - last >= cooldownNanos || now - last < 0;
    }

    void commit(UUID player, long now) {
        Objects.requireNonNull(player, "player");
        committed.remove(player); // refresh insertion order on a valid gesture
        if (committed.size() >= maxEntries) {
            Iterator<Map.Entry<UUID, Long>> it = committed.entrySet().iterator();
            while (it.hasNext() && committed.size() >= maxEntries) {
                Map.Entry<UUID, Long> item = it.next();
                if (now - item.getValue() >= cooldownNanos || now - item.getValue() < 0) it.remove();
            }
            if (committed.size() >= maxEntries) committed.remove(committed.keySet().iterator().next());
        }
        committed.put(player, now);
    }

    void remove(UUID player) { committed.remove(player); }
    void clear() { committed.clear(); }
    int size() { return committed.size(); }
}
