# Exchange setup

Never run both old and unified Exchange writers. Stop server and remove the legacy Exchange JAR from the clone, retaining its original data folder. The unified module refuses transactions while that legacy plugin is installed.

Fresh startup creates `exchange/live/exchange.db`, config, empty format-2 reward catalog and migration marker. Ticket offer starts at 50 but disabled. No synthetic currency is generated; transactions remain locked.

1. `/opdg status` verifies initialization.
2. Hold your genuine Dungeon Token item, with its real metadata, and run `/opdg token set`. Ticket-tagged currency is refused.
3. `/opdg ticket 50 on` enables Universal Dungeon Ticket offer.
4. `/opdg exchange on` enables transactions after currency/catalog validation. Requires `arunika.dg.toggle` in addition to admin access.
5. Hold a genuine reward and run `/opdg addhand <new-key> <cost> <category>`. Cost range 1..1,000,000. Existing UUIDs remain stable through edits. `/opdg rewards`, `/opdg cost`, `/opdg category`, `/opdg enable` manage ordinary entries. Universal Ticket uses dedicated `ticket` command or corrected admin GUI.
6. `/dg` opens native catalog, category/pagination and confirmation. Test full inventory, insufficient token, offhand tokens, repeated click and stale menu. One valid ticket pays for the entire Private party.
7. `/opdg tx list` shows pending ADG transactions. Inspect affected inventory/save/ticket state before resolving. `tx resolve <id> no-debit` requires RESERVED plus proof of no debit; `delivered` acknowledges externally verified settlement and delivers nothing.

Legacy migration copies original reward IDs, stock, purchase limits and history without resetting. Existing complete unified state wins. Corrupt/incomplete state or inconsistent stock is refused; never delete database files to clear an error.

ITEM and TICKET are available after validation. Temporary PERMISSION rewards require LuckPerms, explicit allowlist, REJECT duplicate behavior and positive duration. COMMAND/BUNDLE execution remains locked in the baseline; importing their configuration does not enable arbitrary console commands. This is a known feature limitation retained from RC6.
