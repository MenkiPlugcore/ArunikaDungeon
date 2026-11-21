# Rollback

Before installation, stop the clone/server and take a matched snapshot of JARs, plugin directories, ticket/Exchange journals, SQLite main/WAL/SHM, worlds and player saves. Record artifact hashes. Test restoration in a second isolated directory.

If 1.0.0 has not processed gameplay or transactions, stop it, restore the RC6 JAR and matched pre-update configuration snapshot, then restart and verify diagnostics and integrity on the clone.

If players have transacted, do not restore old databases/ledgers alongside current inventory. Stop entry and Exchange, preserve the failed state and all logs, identify pending transaction IDs, SPENT/EXPOSED tickets and unissued-loot records. Prefer restoring only the older JAR with current compatible schema-2 data after validating it on a clone. A full rollback requires a matched snapshot of all value-bearing state and explicit accounting for subsequent transactions.

Check `PRAGMA integrity_check` offline on a copied database, `/opdg tx list`, affected player inventory and ticket records. Uncertain debit/delivery stays pending. `/opdg tx resolve` is manual acknowledgment, not automatic compensation. An EXPOSED ticket is never eligible for normal refund. Do not run ticket recovery on a successful or loot-exposed encounter.

Keep both pre-update and failed-state backups until acceptance. No automatic destructive rollback is included.
