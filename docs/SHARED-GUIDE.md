# Shared Dungeon System v0.4.0

## Opt-in testing (not production)
In the generated `config.yml` retain `foundation-only: true` and set:
```yaml
battle-preview: true
shared-preview: true
```
Both flags default false. `shared-preview` requires `battle-preview`. Configure `battle.yml` with `enabled: true` and one SHARED arena for each floor you want to test. Follow BATTLE-GUIDE.md for coordinates, minions and boss IDs. Use a dedicated staging world and separately protect inactive regions. All spawn and return chunks must already be loaded; the adapter does not force-load or generate chunks. `settings.yml`'s legacy enabled flag is not the preview switch. Set the mode's `shared/settings.yml`:
```yaml
schema-version: 2
enabled: true
entry-cost: FREE
max-players: -1
queue-countdown-seconds: 30
cooldown-seconds: 1800
loot:
  mode: FREE_FOR_ALL
  pickup-delay-seconds: 3
  collection-time-seconds: 45
  scatter-radius: 5
```
Loot options are reserved config only, not implemented here. Config migration adds `shared-preview: false` if missing; previous schema version remains 2 because change is additive. Valid wave and arena setup is required; a floor with disabled Shared mode or missing SHARED arena is omitted from the queue. Admin previews remain separate/testing only.

Commands (test permission `arunika.dungeon.shared.test`, default op; admin still needs `arunika.dungeon.admin` for management):
- `/adg shared join <floor-id>` — register in one floor queue for free.
- `/adg shared leave` — remove waiting registration (not active session escape).
- `/adg shared status` — state, remaining seconds and participant counts for each configured Shared floor.
- `/adg shared status <floor-id>` — also list queued / registered active / living participants.
- `/adg preview stop <session-id>` — admin abort. `/adg preview status` shows session IDs; `/adg preview exit` retries failed return for admin testers. Non-admin test accounts with failed return require staff assistance.

## State and timing
AVAILABLE -> COUNTDOWN -> IN_BATTLE -> COOLDOWN (after boss defeat) -> AVAILABLE. A queue can be populated during IN_BATTLE or COOLDOWN; its countdown begins once available. Every eligible waiting player at dispatch is included. No separate hard limit, but actual server player capacity and hardware still apply. `PAUSED` represents unavailable launch, TPS/MSPT guard, storage failure or retry backoff. Guard interrupts the countdown; it starts again when safe. A failed launch releases its reservation and requeues eligible participants, with 10-second retry backoff. A failed battle does not trigger 30-minute cooldown; the next queue can start a new countdown. Participants who die or quit are eliminated under the inherited v0.3 battle policy. No reconnect to an active battle yet.

## Persistence
`data/shared-cooldowns.txt` is a small, versioned UTF-8 data file holding absolute UTC epoch millisecond expiry per floor. It is not configuration. Write is atomic replacement and file fsync, only upon successful Shared boss defeat, not each tick. The rare synchronous write prioritizes committing cooldown before completion; slow storage can still cause a tick hitch. Directory fsync is best effort on unsupported filesystems. Keep file on local disk with atomic rename support. An invalid file refuses startup instead of silently clearing cooldowns. Runtime write failure locks all Shared entry until operator intervention; inspect logs and repair disk/snapshot before restart (a failed write cannot guarantee durability). Successful persisted cooldowns survive server restart or config reload, and elapsed downtime counts. Live queue, active session and pending exits are in memory only; restart clears them; no crash-resume capability. Stop staging after sessions finish and participants exit. Clock changes can affect absolute expiry. Store independent of legacy exchange and foundation DB.

## API integration
Existing DungeonGatewayApi `session(UUID)` stays unchanged; `queuedFloor(UUID)` reports current wait registration. `sharedStatus()` returns immutable floor snapshots of state, seconds, queued UUIDs, active set and living set on the main thread. `entryEnabled()` remains false: CdrWaystone must not consider these methods an entry authorization. No Waystone JAR update at this milestone.

## Performance and staging limitations
One queue poll per second piggybacks on battle scheduler; no tasks per player. Membership indexes avoid full player scans on queue insertion. Battle spawn budget and batching retained. Launch validation/teleport and boss defeat file write occur on main thread. Teleport burst and chunk behavior with many real players remain unprofiled. Adapter uses the v0.3 Mythic bridge; direct mobs tracked, custom Mythic summons and custom drops still need integration and real test coverage. Do not use production boss definitions with built-in loot, commands or external effects. No official Java/Bedrock runtime verification or production anti-exploit certification.

## Acceptance tests still required on real server
Load dependencies; validate boss and minion IDs and all points; run two independent Shared floors and verify wave completion and countdown. Queue during battle, defeat boss, check 30-minute cooldown, restart after all players returned and verify remaining time; test Private preview independence. Check disconnect, launch rollback, external teleport prevention, low TPS guard and loaded-chunk requirement. Check real Java/Bedrock UI and spark MSPT with 10/20/30/50 players on staging. Test source and target worlds with existing plugins; keep legacy system running until production cutover is explicitly verified.
