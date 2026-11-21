# Audit archive 2026-11-21T054813Z
Basis: local TAR.GZ, plugin.yml, JAR entries/public bytecode signatures, YAML, SQLite. No live server, MythicMobs definitions, actual worlds, configs/registry of CdrWaystone, or original Java source supplied. Static audit only, not a complete vulnerability or behavioral audit.

| Plugin | Observed version | Decision for v0.1.0 |
|---|---|---|
| CdrDungeonSpawner | 1.0.5 | Remains active; stage configs for later migration |
| ArunikaDungeonExchange | 0.4.2 | Remains active; keep /dg and /opdg; preserve token bytes and SQL ledger |
| CdrWaystone | 0.7.1-InventorySafety | Unmodified; future gateway bridge |
| CdrWaystoneBedrock | 0.2.1-CoreCompatibility | Unmodified; depends on CdrWaystone and Geyser-Spigot |

- 10 floor YAMLs, 9 active; 69 group definitions; world `dungeon1`. Legacy global cap 160 mobs and max 8 spawn attempts per tick. These are observations, not performance benchmarks.
- `bosses.yml` registry is empty. Goblin and Skeleton King references exist in old floor groups (`ARU_F5_Goblin_Boss`, `ARU_F9_SkeletonKing`). Queen Bee, Mega Warden and The Doom are not confirmed by this archive. No invented IDs, no automatic reuse of corridor coordinates for new arenas.
- Exchange catalog contains 23 enabled reward definitions. Stored item and token Base64 templates are preserved byte-for-byte as YAML; deserialization and in-game compatibility are not tested.
- SQLite integrity_check = ok; 113 transactions (all DELIVERED), 109 purchase rows, 32 reward_stock rows. Stock/history may include historical rewards outside the active catalog. Do not drop them.
- Exchange TokenService and ExchangeLedger are package-private; no stable public integration contract was confirmed. Do not use reflection to mutate transactions or private classes.
- Waystone's PublicApiBridge exposes Bedrock forms, reflective calls and WorldGuard helper, not an ArunikaDungeon session contract. New API is proposed, not connected to the old binary.
- Main risk: command collision (/dg, /opdg, /cds, /cws), duplicate drops/transactions, unsafe copying of live SQLite WAL, conflicting world coordinates. Foundation avoids owning any legacy commands or modifying any legacy files.
- No full decompilation or consolidation of old gameplay in v0.1.0. Source reconstruction and behavior parity remain future milestones.
