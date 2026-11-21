# Battle preview configuration — v0.3.0
This version has two opt-in gates (`battle-preview: true` in config.yml and `enabled: true` in the floor's battle.yml); the legacy `settings.yml enabled` controls old validation and is not required for admin preview. `foundation-only` must stay true. All commands require `arunika.dungeon.admin`. Use a dedicated staging world with WorldGuard and no custom summon/drop/terrain effects. No guessed boss IDs. Existing arena.yml survives updates; new battle.yml defines actual preview arena regions. To capture coordinates, stand at the point and run `/adg preview point` after loading preview. It prints a YAML mapping; it does not automatically write it.

Example `dungeons/01_queen_bee/battle.yml` (all coordinates and world name are examples):
```yaml
schema-version: 2
enabled: true
scaling:
  minion-per-extra-player: 0.15
  max-multiplier: 3.0
arenas:
  - id: shared_1
    mode: SHARED
    bounds:
      min: {world: dungeon_test, x: 0, y: 60, z: 0}
      max: {world: dungeon_test, x: 199, y: 110, z: 199}
    player-spawn: {world: dungeon_test, x: 10, y: 65, z: 10}
    mob-spawns:
      - {world: dungeon_test, x: 60, y: 65, z: 60}
      - {world: dungeon_test, x: 140, y: 65, z: 140}
    boss-spawn: {world: dungeon_test, x: 100, y: 65, z: 100}
    exit: {world: dungeon_test, x: -25, y: 65, z: 0}
  - id: private_1
    mode: PRIVATE
    bounds:
      min: {world: dungeon_test, x: 500, y: 60, z: 0}
      max: {world: dungeon_test, x: 699, y: 110, z: 199}
    player-spawn: {world: dungeon_test, x: 510, y: 65, z: 10}
    mob-spawns:
      - {world: dungeon_test, x: 560, y: 65, z: 60}
      - {world: dungeon_test, x: 640, y: 65, z: 140}
    boss-spawn: {world: dungeon_test, x: 600, y: 65, z: 100}
    exit: {world: dungeon_test, x: -25, y: 65, z: 0}
```
`/adg preview start` still requires testers to enter manually. The v0.4 Shared queue performs staging teleport to `player-spawn`; see SHARED-GUIDE.md. Ensure all spawn chunks and exit chunk already loaded in staging. Minion quantities scale with participant count fixed at session start, rounding up and capped at 1000 per mob entry; no scaling of Mythic boss HP/damage in v0.3.

`waves.yml` example (replace every ID with installed MythicMobs test mob ID):
```yaml
schema-version: 2
waves:
  wave-1:
    mobs:
      - mythicmob-id: TestBee
        amount: 8
  wave-2:
    mobs:
      - mythicmob-id: TestBeeGuard
        amount: 12
  wave-3:
    mobs:
      - mythicmob-id: TestBeeElite
        amount: 3
```
`boss.yml`: set `mythicmob-id: TestQueenBee`; leave old `spawn` map intact, per-arena `boss-spawn` is used. `settings.yml` wave-count must equal the number of active waves. `performance.yml` sets `max-active-minions`, `spawn-batch-size`, `minimum-tps`, `maximum-mspt`. Default 100 tracked entities across sessions; not a cap on other Minecraft entities or Mythic skill summons. Scheduler `battle-tick-interval: 5` ticks by default. Guard pauses spawn; timeout continues.

## Staging acceptance tests (not performed here)
- Start server with real dependencies; check plugin loads and commands work, valid public API reflection. Check attempted unknown Mythic IDs refuse before session start.
- Start wave preview with a test player and loaded chunks. Kill minions, ensure next wave waits for pending spawn count. Verify boss only spawns after final wave; kill it; verify Shared cooldown and no duplicated completion. Preview has no real loot or ticket transaction. Check old native drops are not emitted by test mob definitions.
- Concurrent Shared/Private arenas in separate regions. Attempt outsider access and duplicate session. Kill a tester/quit, verify failed session and exit handling. Force chunk unload/low TPS in staging, verify guard/cleanup. Confirm only session entities are removed and no other plugin data is modified.
- Confirm bossbar and actionbar on Java and Bedrock, and performance via spark. Test full Mythic skill integrations before enabling players (not supported as a complete anti-exploit system here).
