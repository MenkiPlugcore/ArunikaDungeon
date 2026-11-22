package dev.arunika.dungeonwaystone.bedrock;
public final class ModelCatalogTest {
    public static void main(String[] args) {
        int passed = 0;
        if (!"arunika_dungeon:waystone_hub".equals(ModelCatalog.forRole("HUB"))) throw new AssertionError(); passed++;
        if (!"arunika_dungeon:waystone_return".equals(ModelCatalog.forRole("RETURN"))) throw new AssertionError(); passed++;
        if (ModelCatalog.forRole("PERSONAL") != null) throw new AssertionError(); passed++;
        if (ModelCatalog.forRole("") != null) throw new AssertionError(); passed++;
        if (ModelCatalog.forRole("hub") != null) throw new AssertionError(); passed++;
        if (!ModelCatalog.HUB.startsWith("arunika_dungeon:")) throw new AssertionError(); passed++;
        if (!ModelCatalog.RETURN.startsWith("arunika_dungeon:")) throw new AssertionError(); passed++;
        if (ModelCatalog.HUB.equals(ModelCatalog.RETURN)) throw new AssertionError(); passed++;
        System.out.println("ModelCatalogTest: " + passed + "/8 PASS");
    }
}