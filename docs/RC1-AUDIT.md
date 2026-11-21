# 1.0.0-RC1 audit and release gates
Base: exact v0.9.5 source archive. Target Paper 1.21.11/Java 21. Source release candidate only, not a certified server plugin.

## Changes
- SessionRegistry.find(UUID): direct indexed lookup replaces list allocation and scan on reward authorization and admin recovery checks.
- Battle initialization fails closed when the legacy CdrDungeonSpawner plugin is loaded (even disabled) or MythicMobs missing. Data directories remain untouched. All configured battle mob IDs now checked at startup before registering the battle adapter. If any missing, battle remains offline until corrected and reloaded. Unknown plugin API signatures also fail closed.
- Gateway/battle initialization failure detaches partially created gateway/listeners and cancels partial scheduler; no production bypass. Runtime status prints actual flags and detected plugin versions rather than only configured state.
- Exchange player use and claims require additional tester permission `arunika.dungeon.exchange.test` (OP default). Existing admin controls and legacy-owner lock preserved. No new transaction replay or refund behavior. Shared join messages accurately reflect loot-preview state.
- Keep config schema 2; no forced rewrite of user values, no new resource pack and no new recurring scheduler. All prior features/source retained. Previous v0.9.5 targeted hardening and EXPOSED ledger remain in force.

## Executed validation
- 286 Java standalone assertions (29 core incl 2 archive checks and 5 new RC checks, 68 fixture config, 30 battle, 34 Shared, 48 Private, 22 loot, 15 Exchange, 17 gateway, 12 admin, 11 RPG). See rc1-tests logs. No actual Paper types in these tests.
- 16 SQLite statement checks (Python sqlite3 on disposable copy; not Java JDBC). Offline legacy migration: 113 transactions, 109 purchases, 32 stock, 22 source files unchanged; all row values compared. No player data distributed.
- Java frontend parsing of 45 source files (syntax only). YAML resource syntax check with template substitutions, not production SnakeYAML validation. No real compile of Paper-dependent adapter, no runtime tests. No verified JAR.
- Maven not installed; official Paper repository DNS failure. Build command exits 127. Workflow available for official compilation but not executed. No fake API stubs or substitute jar.

## Audit limitations and risks (release blockers)
- The legacy Java Exchange migration, Bukkit/Paper calls, reflection hooks and actual GUI client behavior remain untested. No verified type checking of Paper adapter. Compile errors cannot be ruled out; any diagnostics must be fixed using the actual API. Native boss health follows MythicMobs config; only minion count scales. Direct spawned entities tracked; Mythic skill-created entities/summons, pets, NMS teleport or custom pickup paths are not certified. Existing teleport/death/exit and world inventory integrations need live verification. No production performance measurements, no zero-dupe guarantee.
- Synchronous ticket ledger, cooldown persistence, player saveData and native class XP can stall the main thread. No additional recurring work in RC1. Startup mob validation cost occurs on config apply only. Test actual disk latency.
- Staging holds public entry disabled; Exchange requires permission but only on separate test server. Runtime failures do not guarantee rollback of already saved config; fix configuration while stopped or restore validated backups. EXPOSED refund protection is conservative and can forfeit ticket if a candidate reward fails. DB and inventories are non-atomic across hard crash; reconcile uncertain records manually.

## Required checks (not performed)
1. Official dependency build + all dependency-backed tests via script/CI. Boot exact Paper version and inspect errors. Run shadow mode with legacy, then test cutover on clone and verify legacy token, catalogs, stock, permissions, serial ledger and original Waystones.
2. Shared floor independence with 10/20/30/50 test players; cooldown after boss death/restart; verify late join, overload guard, entity budget. Private solo/five, arena reservations, ticket moved/forged/duplicated, crash at hold/debit/reward/refund stages. Prevent cross-session pickup, hopper, magnet, external item stacker, autosell, NMS teleport, pet and summon interference; check cleanup after death/relog/restart.
3. Java and Bedrock real clients: inventory clicks/drags, fast click, session list, return stone, combat guards, movement, world change and flight, permission revocation. Test actual MythicMobs, MMOItems, ItemsAdder, ModelEngine and Nusa Karsa. Verify survival Waystones remain intact.
4. Run spark profiler under representative identical load; compare MSPT/TPS, heap after GC, counts of chunks/entities, scheduler activity, DB awaits and disk latency. Record measurements, investigate regressions; only then approve production release.
