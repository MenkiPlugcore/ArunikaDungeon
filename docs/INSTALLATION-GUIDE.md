# Installation — staging only

1. Stop an isolated clone. Back up plugins, configuration, worlds/playerdata, ticket ledger, SQLite databases and WAL/SHM together. Keep the original RC6 JAR.
2. Verify `sha256sum -c SHA256SUMS.txt` beside all package assets. Install only ArunikaDungeon-1.0.0.jar; remove the previous ArunikaDungeon JAR from the clone to avoid duplicate plugin names.
3. Retain CdrWaystone-0.7.0.jar and CdrWaystoneBedrock-0.2.0.jar as separate plugins. Do not merge JARs. Disable the old CdrDungeonSpawner JAR when testing the new battle controller; retain its data directory.
4. Paper 1.21.11 and Java 21 are required. Paper loads SnakeYAML/SQLite libraries declared in plugin.yml; allow dependency resolution or prepare the verified library cache. LuckPerms is required only for allowlisted permission rewards. MythicMobs is required only for battle.
5. Start with existing ArunikaDungeon data intact. `/adg status`, `/adg diagnostics`, `/opdg status`, `/opdg tx list`, `/adg gateway check`. Fresh setup starts locked and does not invent currency.
6. Configure arenas, actual Mythic IDs, hub/return stones and rewards using the setup guides. Keep `foundation-only: true`; existing `battle-preview`, `shared-preview`, `private-preview`, `loot-preview` are the actual opt-in switches in 1.0.0 staging.
7. Grant test permissions deliberately; default is OP. Test Java and Bedrock with real clients before opening access. The read-only API `entryEnabled()` remains false in this staging build.

Do not use Bukkit `/reload` or third-party plugin unloaders. `/adg reload all` is the supported configuration reload and refuses active sessions/queues/parties/pending returns/Exchange operations. Stop/start is required for replacing a JAR.
