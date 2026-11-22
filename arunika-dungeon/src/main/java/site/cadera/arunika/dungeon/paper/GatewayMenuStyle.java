package site.cadera.arunika.dungeon.paper;

import java.util.ArrayList;
import java.util.List;

/** Pure Java GUI geometry and copy limits; no Bukkit calls or new scheduler. */
final class GatewayMenuStyle {
    static final int PAGE_SIZE = 36;
    static final int MENU_SIZE = 54;
    private GatewayMenuStyle() {}

    static int pages(int count) {
        if (count < 0) throw new IllegalArgumentException("negative count");
        return count == 0 ? 1 : 1 + (count - 1) / PAGE_SIZE;
    }

    static int page(int requested, int count) {
        return Math.max(0, Math.min(requested, pages(count) - 1));
    }

    static String pageLabel(int requested, int count) {
        return "Page " + (page(requested, count) + 1) + " / " + pages(count);
    }

    static int slot(int page, int index) {
        if (page < 0 || index < 0) return -1;
        long result = (long) index - (long) page * PAGE_SIZE;
        return result >= 0 && result < PAGE_SIZE ? (int) result : -1;
    }

    static String compact(String input, int limit) {
        if (limit < 4) throw new IllegalArgumentException("limit < 4");
        if (input == null) return "";
        // A player-defined floor name is data, never a chat-formatting instruction.
        String safe = input.replaceAll("(?i)§[0-9a-fk-orx]", "")
            .replaceAll("[\\p{Cntrl}\\p{Cf}]", " ").trim();
        int count = safe.codePointCount(0, safe.length());
        if (count <= limit) return safe;
        int end = safe.offsetByCodePoints(0, limit - 3);
        return safe.substring(0, end).stripTrailing() + "...";
    }

    static List<String> lore(List<String> original) {
        if (original == null || original.isEmpty()) return List.of();
        List<String> out = new ArrayList<>();
        for (String line : original) {
            if (out.size() >= 7) break;
            out.add(compact(line, 60));
        }
        if (original.size() > out.size()) out.set(6, "...more information in dungeon");
        return List.copyOf(out);
    }
}
