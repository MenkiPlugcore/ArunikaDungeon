# Dependency compatibility

| Dependency | Evidence | Status |
|---|---|---|
| Java 21 | Real compiler/runtime | Compile PASS |
| Paper API 1.21.11 | Maven dependency + class hashes | Compile PASS |
| SnakeYAML 2.3 | Real parse/serialization/migration tests | PASS |
| SQLite JDBC 3.46.1.3 | Real migration, stress, reopen, integrity | PASS |
| LuckPerms API 5.4 | Real compile dependency | Compile PASS; 5.5.85 live grant NOT TESTED |
| CdrWaystone 0.7.0 | Public ABI inspected from original JAR | ABI reviewed; actual teleport/GUI NOT TESTED |
| CdrWaystoneBedrock 0.2.0 | Separate original JAR available | Client interaction NOT TESTED |
| MythicMobs 5.12.1 | Cached public reflection adapter in source | Spawn/runtime NOT TESTED |
| ArunikaNusaKarsa 1.2.9 | Native fallback deliberately refuses this version | External DungeonRpgApi provider required for bonus progression |
| ArunikaNusaKarsa 1.2.10 | Previously audited owned nkxp command fallback; source reviewed | Live integration NOT TESTED |
| Geyser 2.12.1 / Floodgate 2.2.5 | Native inventory transport design | Exact versions/client NOT TESTED |
| ItemsAdder 4.0.16 / MMOItems | Lazy public provider lookup | Exact runtime item resolve NOT TESTED |
| ModelEngine / AE | No direct damage/metadata rewrite | Combined combat NOT TESTED |
| Multiverse/Core/Inventories / WorldGuard | Optional lifecycle ordering; no proprietary integration | Teleport/inventory/regions NOT TESTED |
| spark | Owner profiling checklist | Gameplay load profile NOT TESTED |

Missing optional plugins do not prevent foundation startup; battle requires MythicMobs, gateway requires CdrWaystone, permission rewards require LuckPerms. RPG native command fallback is intentionally restricted to 1.2.10; do not widen version acceptance without API/command validation. No separate Race EXP system or damage formula override is introduced. Summon/pet attribution remains a production blocker.
