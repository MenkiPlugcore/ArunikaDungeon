package site.cadera.arunika.dungeon.paper;

/** Round-robin GUI refresh planning: bounded work, deterministic fairness, no Bukkit dependency. */
final class GatewayWorkBudget {
    record Window(int start, int count, int next) { }
    private GatewayWorkBudget() { }
    static Window next(int size, int cursor, int budget) {
        if (size <= 0) return new Window(0, 0, 0);
        if (budget <= 0) throw new IllegalArgumentException("budget must be >0");
        int start = Math.floorMod(cursor, size);
        int count = Math.min(size, budget);
        return new Window(start, count, (start + count) % size);
    }
}
