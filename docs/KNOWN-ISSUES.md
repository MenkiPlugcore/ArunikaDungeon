# Known issues / production blockers

1. Actual Java/Bedrock Waystone, Shared/Private battle, loot/Exchange and third-party integration acceptance remains NOT TESTED. Pure policy regression does not prove client compatibility.
2. Direct spawned entities/projectiles are tracked; arbitrary Mythic summons, pets, custom explosions/terrain and independent provider loot are not fully attributed. Such mobs can break expected combat/protection/reward semantics. Use controlled test definitions; production is blocked until audited on the actual stack.
3. Loot that cannot be emitted within entity/event budget or at a safe location now creates `data/loot-recovery/*.loot` evidence. It requires supervised manual compensation; there is no automatic deferred delivery. A crash before that exceptional evidence write or during provider/entity creation can remain ambiguous. Never automatically reissue uncertain rewards.
4. Ticket/cooldown/player-save and exceptional loot evidence writes are synchronous. A slow filesystem may stall main thread. No measured CPU/RAM gain or player capacity is claimed.
5. Ticket serial files grow over time and startup scans them. Pending return coordinates are primarily in memory; startup recovery detects stranded players in configured arenas, but removal/change of arena configuration while offline can limit recovery.
6. Native Class XP fallback recognizes exactly Nusa Karsa 1.2.10; 1.2.9/other versions require a verified DungeonRpgApi provider for bonus progression. Optional progression may be skipped if provider is absent or bounded queue expires; no replay is performed.
7. Baseline imported COMMAND/BUNDLE Exchange reward execution remains disabled. No new arbitrary console-command reward engine is introduced.
8. Source retains staging switches/permissions and `entryEnabled() == false`. Version 1.0.0 denotes the requested candidate version; this artifact is not an official production-ready release.

Any known limitation affecting required acceptance keeps the release in draft/staging. No RC7/RC8 is introduced.
