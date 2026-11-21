package dev.arunika.dungeonwaystone.bedrock;
/** Source-compatible, dependency-free mapping of original Waystone model skins. */
public final class ModelCatalog {
    public static final String HUB = "arunika_dungeon:waystone_hub";
    public static final String RETURN = "arunika_dungeon:waystone_return";
    private ModelCatalog(){}
    public static String forRole(String role) {
        return switch (role) {
            case "HUB" -> HUB;
            case "RETURN" -> RETURN;
            default -> null;
        };
    }
}