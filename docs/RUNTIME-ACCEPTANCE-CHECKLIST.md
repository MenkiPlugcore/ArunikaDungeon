# Runtime acceptance — production gate remains OPEN

Use an isolated Paper 1.21.11 / Java 21 clone with owner EULA acceptance, backups and the exact intended dependency versions. Preserve logs and artifact SHA-256. Do not mark a row PASS merely because compilation or a pure-policy test passed.

- [ ] Verify original production configuration retained; offline backup and rollback restoration tested.
- [ ] Actual Mythic boss/minion spawn, death and custom-skill cleanup.
- [ ] Normal personal/public/global/survival Waystone teleport before and after integration.
- [ ] Java hub/return interaction, offhand/double event, remote fallback refusal.
- [ ] Bedrock real mobile client hub/entity/anchor fallback, pagination, party and return.
- [ ] Shared countdown, no mid-battle admission, cooldown 30 minutes persisted through restart.
- [ ] Private solo and five-person party; one ticket for all; three simultaneous sessions.
- [ ] Ready invalidation, invite expiry, queued/offline removal, arena isolation and capacity.
- [ ] Ticket forged/duplicated/spent/refund/EXPOSED cases, full inventory, disconnect and crash recovery.
- [ ] Loot participant-only pickup, hopper/merge/offhand, full inventory, unissued recovery record handling.
- [ ] Exchange item/permission/ticket; exact currency, stock/limit/concurrent click and uncertain crash transactions.
- [ ] Nusa Karsa native damage/enchant/rune/Forge/Reforge/attributes/Class EXP; no duplicate progression.
- [ ] Pet/summon ownership and damage attribution verified; configure exclusion until implemented.
- [ ] Multiverse-Inventories crossing and rejected teleport do not alter ticket or player inventory incorrectly.
- [ ] Shutdown/restart during queued, ACTIVE, LOOT and pending DB operations; no leaked task/entity/thread.
- [ ] 5 players: TPS/MSPT/heap/GC/entity/task/worker backlog/spark profile recorded.
- [ ] 15 players: same measurements.
- [ ] 30 players: same measurements.
- [ ] 50 players: same measurements; no claim based on SQLite request count.

Owner acceptance record: date/time, operator, Java/Paper versions, plugin hashes/versions, scenario, result and evidence links. Publish only after every mandatory row is PASS and known production blockers are resolved. The workflow only creates a draft release; it cannot declare these client gates satisfied.
