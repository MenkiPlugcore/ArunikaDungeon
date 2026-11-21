# Performance report

| Setting | Default |
|---|---:|
| Minimum TPS | 18 |
| Maximum MSPT | 50 |
| Tracked mob budget | 100 |
| Spawn batch ceiling | 5 |
| Active loot entity budget | 256 |
| Maximum Private sessions | 3 |
| Guard sampling | At most once/second |
| Guard recovery | Five healthy samples with hysteresis |
| Exchange worker queue | 128 jobs |

Battle scheduler defaults to five ticks; maintenance and UI refresh occur at most once/second. Session registry uses player/queue/party indexes. Spawn controllers rotate sessions; batch is a ceiling, not a promise to spawn five mobs on every pass. No world clone or world-wide entity scan is performed per tick. Movement authorization still inspects configured arenas in the current world; benchmark if configuring many arenas.

Exchange SQLite runs on one worker. Configuration edits use asynchronous preparation and main-thread application. Ticket journal transitions, player save, Shared cooldown snapshots and exceptional unissued-loot recovery still perform synchronous disk work. This is an explicit limitation; a slow disk can stall the server. Ticket files accumulate to preserve anti-replay history. Never prune them blindly.

The SQLite stress test uses 50 requests, eight submitting threads and one DB owner, with stock 10. It does not represent 50 Minecraft players. No CPU/RAM improvement percentage is claimed. No 5/15/30/50-player benchmark or spark gameplay profile was produced. Record TPS, MSPT, heap, GC, live tracked entities, worker backlog and disk latency during owner acceptance.

Loot budgets do not expand automatically. Unissued quantities are retained as staff-review evidence; automatic deferred compensation is not implemented.
