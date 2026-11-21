# v0.8.0 — Waystone Gateway staging integration

## Important: not a production release
The archive includes compiled CdrWaystone 0.7.1-InventorySafety and CdrWaystoneBedrock 0.2.1-CoreCompatibility but no full Java sources, Waystone registry/configuration or Geyser runtime. This release adds an adapter **inside ArunikaDungeon**, not rewritten Waystone JARs. Keep both original Waystone JARs and all their data unchanged. Native inventory + existing physical anchor interaction is the crossplay strategy; Bedrock rendering and click translation require client tests. Existing Bedrock proxy armor stands are marker entities with no Waystone visual PDC in their initialization; no claim that clicking these proxies works. Use the physical registered anchor or the target-checked fallback below. No new resource pack.

## Setup on a separate staging server
1. Build with `tools/build-verified.ps1` on Windows or `tools/build-verified.sh` on Linux; Java 21 and Maven with the dependencies in pom.xml are required. Keep original JARs on production unchanged.
2. Create separate staging arenas, not legacy DungeonSpawner combat areas. Configure battle.yml, waves.yml, boss.yml using BATTLE-GUIDE.md. Load required worlds/chunks and make sure spawn and exit coordinates are safe. Do not enable gateway in the live server yet.
3. Use existing CdrWaystone admin placement to create **permanent, accessible, active official Waystones**: one in the Hub and optional return stones in each arena. Set each dedicated stone hidden from the survival network with `/cws visibility hidden` while targeting it, using the existing command. Do not bind it to the old dungeon destination system; remove any previous binding before use. Do not hide or modify existing player Waystones. Give testers the normal `cdrwaystone.use` and `cdrwaystone.dungeon.use` permissions as well as `arunika.dungeon.gateway.test` and the relevant `arunika.dungeon.shared.test` / `arunika.dungeon.private.test` permission.
4. Target the anchor block and run `/adg gateway` as an admin to obtain its registry UUID. It also reports whether it is permanent/admin, hidden and unbound. In `integrations/waystone.yml` enter the actual UUID, actual hub exit position near the stone, and each return stone mapped to its floor ID / arena ID. The configuration validator rejects unknown arenas, hub/return UUID collisions, hub inside an arena, and arena exits that don't exactly equal the hub position. The runtime adapter requires dedicated registered stones; it rejects return stones outside their mapped arena. Example (UUIDs and coordinates are illustrative only):

```yaml
schema-version: 2
enabled: true
gateway-id: '11111111-1111-4111-8111-111111111111'
hub: {world: dungeon_hub, x: 10.5, y: 65, z: 10.5}
interaction-radius: 6
return-waystones:
  '22222222-2222-4222-8222-222222222222': queen_bee/shared_1
  '33333333-3333-4333-8333-333333333333': queen_bee/private_1
```

5. Set `battle-preview: true`, `shared-preview: true`, `private-preview: true` in config.yml for staging only. Keep `foundation-only: true`. Loot is separately controlled by `loot-preview`. Reload with `/adg reload all` or restart, and review console. If a binding is invalid, the gateway is unavailable. The existing configuration and transactional backups remain intact. Do not remove the legacy exchange or spawner from production.
6. Right-click the Hub stone. For Bedrock click translation problems, look directly at the registered anchor and run `/adg gateway open`. It checks the targeted registered UUID, proximity, permissions, and travel guard just like a physical click. It is not a remote teleport command.

## Player flow
Shared: open Shared > choose floor > see live active/queued roster and state > join queue. No participant cap per dungeon, 30 minute cooldown per floor after boss death. All testers must remain at the Hub until admission; moving away, disconnecting, combat lock or becoming ineligible removes the player from the queue. Private: choose floor > view participants > Create / My Party > Invite online player > invited player comes to Hub and accepts invitation > all Ready > leader holds valid Universal Dungeon Ticket in main hand > Start / Queue. Existing ledger validates, reserves, consumes, refunds according to prior rules. No money payment. A return stone offers confirmation to abandon the current battle; no voluntary-exit refund. Normal completion still returns players automatically. This uses staged command handlers without dispatching player commands. Direct staging join/create/invite/ready/start commands are refused when gateway is enabled. Exchange commands are unchanged.

## Runtime acceptance checks — not yet executed
- Test native click on Java anchor and ItemsAdder model, Bedrock anchor and fallback; confirm survival Waystones and Geyser renderer unaffected.
- Test left/right click, drag, shift, hotbar swap and double-click on inventories, paginated participant list, reload with GUI open, permission revocation and moved-away player.
- Verify two different floors waiting/active/cooldown independently, 30-minute persisted cooldown, unlimited Shared subject to server capacity; test invites, acceptance at the hub, ticket moved while queued, ticket refund, party limit and simultaneous modes.
- Test movement, /home, /tpa, /warp, portals, ender pearls, respawn, logout/reconnect and server crash. Dedicated arena guard rejects idle/active unauthorized entry while battle adapter runs; teleport permits are exact one-shot destinations. Voluntary exit requires own living session and bound return stone inside it. WorldGuard or equivalent protection must also block external access when ArunikaDungeon is disabled, since a disabled plugin cannot enforce events. Plugins using NMS or bypassing Bukkit events need separate tests. Test all chunk safety and world-inventory switching; no unsafe automatic production takeover.
- Check actual TPS/MSPT and allocations using spark with representative load. UI refresh reuses the existing maintenance tick, shares status snapshots and updates only changed inventory slots; no per-player repeating tasks, no ticket ledger polling on refresh. Remaining server API and external plugin calls are unprofiled.
