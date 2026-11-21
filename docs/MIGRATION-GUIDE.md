# Migration from verified RC6

This is a canonical source consolidation, not a new production-data import. Retain the entire existing ArunikaDungeon directory. Schema remains 2; existing configured arenas, spawn points, mobs, waves and mode-specific reward tables are preserved. Unknown admin YAML keys remain preserved by the configuration loader. New defaults are added only when missing, with automatic preimage backup.

The legacy `arena.yml` is retained; executable arena bounds and spawns live in each floor's `battle.yml`. Existing `settings.yml enabled` has older validation semantics; use the documented battle enable command and do not reinterpret legacy coordinates automatically.

Exchange selection:

- Existing complete unified `exchange/live` with marker: reuse DB/catalog/history; never reset.
- Missing unified state plus complete legacy Exchange folder: require old plugin JAR absent, copy snapshot/WAL with hash consistency checks, validate integrity and preserve original files in the new directory.
- Neither present: create empty catalog, SQLite tables and disabled ticket offer (price 50). Currency remains unset until held valid currency is registered.
- Incomplete/corrupt existing state: refuse initialization. Preserve files and repair from a consistent backup; deleting the database to retry can duplicate value.

Configuration rollback journals now validate all preimages before restoring any file. New malformed/duplicate journal tests confirm no partial recovery occurs. Existing ticket generations and lifecycle states are unchanged. New `data/loot-recovery/*.loot` files retain evidence of unissued budget/location rewards; include them in backups.

Do not copy test data over production. Never restore an old Exchange DB or ticket ledger independently after players have transacted. See ROLLBACK-GUIDE.md.
