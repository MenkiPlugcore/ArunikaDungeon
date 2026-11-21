# Config guide v0.3.0
All managed YAML files use schema-version: 2. Startup generates missing files. Never edit the managed folder concurrently with an update/reload. Public access remains disabled. See BATTLE-GUIDE.md for the optional admin-only preview.

```
plugins/ArunikaDungeon/
  config.yml
  messages.yml
  performance.yml
  ticket.yml
  integrations/{waystone,nusa-karsa}.yml
  exchange/settings.yml
  dungeons/01_queen_bee/
    settings.yml
    arena.yml
    battle.yml (admin-only preview; see BATTLE-GUIDE.md)
    waves.yml
    boss.yml
    shared/{settings,wave-1-drops,wave-2-drops,wave-3-drops,boss-drops}.yml
    private/{settings,wave-1-drops,wave-2-drops,wave-3-drops,boss-drops}.yml
  dungeons/02_mega_warden/ (same pattern)
  dungeons/03_the_doom/
  dungeons/04_goblin/
  dungeons/05_skeleton/
  backups/
  data/dungeon.db
  logs/
```

Example `settings.yml` (partial):
```yaml
schema-version: 2
id: queen_bee
floor: 1
display-name: Queen Bee
enabled: false
wave-count: 3
max-duration-seconds: 1200
icon:
  material: HONEYCOMB
  custom-model-data: 0
```
Example `waves.yml` (mob IDs are placeholders, replace with existing MythicMobs IDs):
```yaml
schema-version: 2
waves:
  wave-1:
    mobs:
      - mythicmob-id: YOUR_BEE_MINION_ID
        amount: 8
  wave-2:
    mobs: []
  wave-3:
    mobs: []
```
Example `shared/wave-1-drops.yml`:
```yaml
schema-version: 2
enabled: true
trigger: WAVE_CLEAR
drops:
  honeycomb:
    type: VANILLA
    material: HONEYCOMB
    chance: 100
    min: 8
    max: 16
  token:
    type: CUSTOM
    id: dungeon_token
    chance: 20
    min: 1
    max: 2
```
Example `private/boss-drops.yml`:
```yaml
schema-version: 2
enabled: true
trigger: BOSS_KILL
drops:
  diamond:
    type: VANILLA
    material: DIAMOND
    chance: 75
    min: 2
    max: 5
  queen_blade:
    type: MMOITEMS
    item-type: SWORD
    id: QUEEN_BLADE
    chance: 0.5
    min: 1
    max: 1
```
IDs and rates above are illustrative only. Valid schema types: VANILLA, CUSTOM, MMOITEMS (requires item-type), ITEMSADDER, MYTHICMOBS. Wave trigger: WAVE_CLEAR or MOB_KILL; boss: BOSS_KILL. Chance 0..100, amounts 1..4096, max >= min. Physical drops not executed until later milestones.

Shared settings enforce FREE, max-players -1, cooldown-seconds 1800. Private entry-cost TICKET, ticket-id dungeon_ticket, ticket-cost 1, max-players 1..5, cooldown-seconds 0. Both loot.mode FREE_FOR_ALL. Template drop list defaults to empty to avoid inventing real rewards. Existing mode configs and wave loot are not silently reset. The internal session core still uses global settings; per-dungeon limits and actual loot consumption are future gameplay work.

If wave-count increases, auto generator adds missing wave templates and mode drop files. Decrease keeps unused files. `/adg create dragon 6 Dragon King` creates template for floor 6. Old floor number is not assumed equivalent to new dungeon arena. Config reload validates and refuses malformed or unsupported schema; does not overwrite invalid source files. Backups are not a replacement for regular server-wide backups.
