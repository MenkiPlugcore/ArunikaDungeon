# v0.6.0 Loot configuration and staging guide

1. Keep the old plugins and folders. Use an isolated staging server after compiling. `foundation-only: true`, `battle-preview: true`, `shared-preview: true` and/or `private-preview: true`, `loot-preview: true`. Defaults are off. Existing arena setup is described in BATTLE-GUIDE.md. The old `/adg preview start` command deliberately cannot generate loot or free Private rewards.
2. Each mode has `settings.yml` with `loot.mode: FREE_FOR_ALL`, `pickup-delay-seconds: 3`, `collection-time-seconds: 45`, `scatter-radius: 5`. Pickup delay must be shorter than collection duration. `performance.yml` has `max-active-loot-items: 256`.
3. Example: `dungeons/01_queen_bee/shared/wave-1-drops.yml` (illustrative; no item reward enabled by default):

```yaml
schema-version: 2
enabled: true
trigger: WAVE_CLEAR
drops:
  honey:
    type: VANILLA
    material: HONEYCOMB
    chance: 100
    min: 8
    max: 16
  token:
    type: CUSTOM
    id: dungeon_token
    chance: 50
    min: 1
    max: 3
```

Each entry rolls independently. `MOB_KILL` rolls upon each tracked minion death, `WAVE_CLEAR` once on wave completion (not both in one table in this version). Boss table uses `BOSS_KILL` and its own entries. Private files are distinct; no automatic loot multiplier. Amounts are total arena drops, not per player. `chance` is 0–100, `min/max` are inclusive 1–4096. 128 entries per table. Keep totals low to avoid cap truncation (32 item entities per triggering event and global configurable cap). Prior loot items are cleaned at session end. Wave-clear loot spawns near the configured boss point; other loot uses death location. All configured positions should be safe and easily reachable. Collection does not extend cooldown. Time uses monotonic server timing for pickup and collection, not ticks alone.

### Provider examples (IDs illustrative)

| type | configuration | notes |
|---|---|---|
| VANILLA | `material: DIAMOND` | Bukkit material |
| CUSTOM | `id: dungeon_ticket` | Unique ledger-backed nonstackable ticket |
| CUSTOM | `id: dungeon_token` | Read-only legacy Exchange token template, cached on first use; no old data modified |
| SERIALIZED | `id: <base64>` | Use addhand rather than hand-writing binary |
| ITEMSADDER | `id: namespace:item` | Optional provider API, requires runtime validation |
| MMOITEMS | `item-type: SWORD`, `id: QUEEN_BLADE` | Optional provider API, requires runtime validation |
| MYTHICMOBS | `id: MythicReward` | Optional provider API, requires runtime validation |

All types require chance/min/max. Held item snapshot preserves item metadata as supported by Paper serialization; a snapshot of a unique third-party item may duplicate its unique metadata, so use a provider for such items. Do not use unique third-party items until tested. The ticket editor refuses copies of this plugin's own PDC ticket. Use CUSTOM dungeon_ticket instead. Ticket serial creation is synchronous (disk I/O), so keep ticket count very low, and avoid guaranteed self-funding Private ticket loops. For quality of service, failed/missing providers and cap truncation are logged and skipped rather than retried or substituted.

### Admin commands (permission arunika.dungeon.admin)

```
/adg loot list queen_bee shared
/adg loot addhand queen_bee shared wave-1 honey 100 8 16
/adg loot addhand mega_warden private boss sculk_reward 15 1 2
/adg loot remove queen_bee shared wave-1 honey
/adg reload all
```

Hold the item before `addhand`; editor saves an immutable serialized copy, without consuming held item. Reusing a key replaces its entry. `list` shows the active snapshot's table counts; inspect YAML for keys and values. `remove` removes that key. Edits use a transactional file backup. Manual reload applies all validated settings once no active sessions, queues, parties, or pending returns remain. No inventory GUI yet.

### Staging checklist (not executed in this environment)

- Compile with real provided Paper API, start Paper 1.21.11 with MythicMobs 5.12.1, and verify startup. Use test-only world/arena and actual mob IDs. Enable just a low-value vanilla drop first.
- Test Shared and Private concurrently, compare mode-specific drops, kill multiple minions including duplicate events, check boss cooldown and 45s final collection. Check player death, disconnection, exit and admin stop.
- Test tagged pickup with Java and Bedrock clients, hopper, hopper minecart, item magnet, pet, merging/stacker, AutoSell, boss custom skill drops, item destruction, chunk unload and restart. External plugins that directly edit inventory or use custom drops can bypass the basic listeners; configure their exclusions or disable on staging before real rewards.
- Test every custom item provider and compare metadata to original; test unique ticket duplicate redemption, refund before loot, no refund after loot, and no refund after boss victory. Hard crash settlement still requires admin verification.
- Profile MSPT/TPS and disk latency for item-heavy waves, and keep total drop quantities below caps, especially tickets. If a real runtime failure occurs, disable preview; keep legacy production systems intact.

References used to check adapter call patterns (not runtime verification): Paper 1.21.11 World API https://jd.papermc.io/paper/1.21.11/org/bukkit/World.html ; MMOItems API https://docs.phoenixdevt.fr/mmoitems/api/main.html .
