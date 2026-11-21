# Dungeon setup

Defaults create Queen Bee, Mega Warden, The Doom, Goblin and Skeleton folders, without activating guessed bosses or arenas. Existing customized files are retained.

1. Use `/adg admin` → floor → Arena setup, or `/adg setup arena <floor> <id> <shared\|private>`. Capture two bounds corners, player spawn, one or more minion spawns, boss spawn and exit. Every capture closes the GUI so you can walk to the next point; `/adg setup resume` reopens the draft.
2. Arena bounds may span at most 512 blocks per axis. Spawns must be inside; exits outside every battle arena. Different configured enabled arenas must not overlap. Prepare loaded chunks; plugin does not force-load or clone a world.
3. Set actual installed MythicMobs IDs with `/adg setup boss`, `wave`, `append-wave`; set count 1..30. Configure only test mobs whose skills do not spawn independent loot, pets/summons or alter terrain during first acceptance.
4. Enable floor battle configuration with `/adg setup enable <floor>`. `battle.yml` contains executable arena list and scaling. Retained legacy `arena.yml` is not automatically interpreted as the new bounds format.
5. Mode settings: Shared is FREE, max-players=-1, cooldown=1800 seconds, countdown default 30. Private uses dungeon_ticket, ticket-cost=1 for the whole party, max-players 1..5 and cooldown=0; global max-private-active defaults to 3.
6. Add physical drops using held item editor or mode-specific YAML. Each wave table has MOB_KILL or WAVE_CLEAR trigger; boss uses BOSS_KILL. Example: `/adg loot addhand queen_bee shared boss diamond_reward 100 1 3`. SERIALIZED preserves real item metadata; use CUSTOM dungeon_ticket to issue new unique serials rather than a serialized ticket.
7. Configure pickup delay (default 3 seconds), collection (45 seconds) and scatter radius (5). Delay must be shorter than collection. Budget defaults: 256 live loot entities, at most 32 stack emissions per event. Overflow/location-failed quantities are saved for staff review, never auto-replayed.
8. Set global `battle-preview`, `shared-preview`, `private-preview`, `loot-preview` explicitly for your test; keep foundation-only true. Configure hub/returns and RPG policy separately. `/adg reload all` applies when no active operations exist.

Use `/adg shared status`, `/adg private status`, `/adg diagnostics`, `/adg admin` session monitor. Test elimination, disconnect, reconnect, failed teleport, time limit, shutdown, cooldown restart persistence and all three simultaneous Private sessions. No Vault fee is used.
