# Dungeon Waystone Bedrock 0.2.0 — staging

**Core:** ArunikaDungeonWaystone 0.2.0 (Phase 10). **Gameplay:** ArunikaDungeon 1.0.1 (Phase 9). **Requires:** Geyser-Spigot 2.12.x and optionally Floodgate; ItemsAdder for Java furniture.

Only two custom Geyser entity IDs are registered:
- `arunika_dungeon:waystone_hub`: **original Blackstone** geometry and texture.
- `arunika_dungeon:waystone_return`: **original Portstone** geometry and texture.

The resource pack is supplied separately as `ArunikaDungeonWaystone-Bedrock-Models-v0.2.0.mcpack`, derived from user-provided original 19-skin pack. Pack ID and entity IDs are distinct from original CdrWaystone to avoid collisions.

## Install (staging)
1. Stop server and BACKUP `plugins/ArunikaDungeonWaystone/`, `plugins/ArunikaDungeon/`, and Geyser config.
2. Leave `ArunikaDungeonWaystone-0.2.0.jar` and `ArunikaDungeon-1.0.1.jar` installed. Replace ONLY `ArunikaDungeonWaystoneBedrock-0.1.0.jar` with this `...-0.2.0.jar`.
3. Copy Dungeon model `.mcpack` to `plugins/Geyser-Spigot/packs/`. Keep existing survival pack; do not overwrite it.
4. Stop/start server fully, NOT PlugMan or a plugin reload. Let Bedrock client download/apply resource packs.
5. Test `/adw status`, `/adw refresh`, `/adwbedrock status`, `/adg gateway check`.
6. Connect an actual Bedrock Android/client, check Blackstone appears at Dungeon HUB, Portstone at arena RETURN. Tap the model/barrier and verify GUI/return destination. Java users must continue seeing the ItemsAdder furniture without duplicated entities.

## Architecture
- No gameplay mutations or extra GUI handlers. Dungeon still owns interaction and ticket/party checks.
- Geyser model layer uses its *experimental* custom-entity event API, loaded reflectively and disabled safely on API incompatibility.
- One bounded renderer task, maximum 48 model proxies by default, only near Bedrock players.
- Hides **only DungeonWaystone-owned furniture** from nearby Bedrock clients when replacement proxy is present, never legacy survival CdrWaystone entities.
- If Geyser registration fails, `/adwbedrock status` shows `registered=false`; no proxies spawn, and normal anchor interaction remains. This is NOT proof the model renders on devices.

## Rollback
Stop server; replace `ArunikaDungeonWaystoneBedrock-0.2.0.jar` with the previous 0.1.0 JAR. Restore config if necessary. No player registry or dungeon data is changed. Optional: remove only the new separate Dungeon `.mcpack` from Geyser packs.

## Acceptance tests (must be performed on the actual server)
- Geyser entity definitions registered `2/2` after full restart.
- Pack sent and accepted on Bedrock Android.
- Hub = Blackstone, Return = Portstone; Java model not affected.
- Bedrock click opens exactly one Dungeon GUI. Shared/Private selection and solo/party behave as expected.
- Return sends to configured `/warp dungeon` destination, not arbitrary world.
- Logout, world change, chunk unload, and restart do not leave proxy entities or hidden originals.
- No duplicate entity registrations with the **survival** CdrWaystoneBedrock.
- TPS/MSPT profiling with spark.

**Do not call this production-ready until runtime verification is done.**
