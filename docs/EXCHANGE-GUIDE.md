# Unified Exchange — v0.7.0 staging guide

## Cutover (staging only)
1. Stop a staging clone of Arunika. Preserve full independent backup (plugins, playerdata, databases). Keep `plugins/ArunikaDungeonExchange/` unchanged, including any WAL/SHM. Remove only that old Exchange JAR from the staging plugins folder. The original production server stays unchanged.
2. Build v0.7.0 using the included verified-build scripts. Install only after build and dependency tests pass. The old Spawner and Waystone are not replaced by this milestone.
3. Start staging. New plugin copies legacy config/reward files and SQLite data into `ArunikaDungeon/exchange/live/`; the migration marker means the source is not reimported on later startups. If the old plugin is still present, no unified Exchange transactions are possible. If migration failed, inspect logs; never delete an already used migrated DB to retry.
4. `/opdg status` or `/adg exchange admin status`; `/opdg rewards` lists imported entries. `exchange/settings.yml` remains locked by default regardless of old `transactions-enabled`. Check 23 rewards, token and stock against old data. Verify several items in Java and Bedrock. `PERMISSION` reward icons are previews only; item rewards keep original amount and metadata.
5. For ticket testing: `/opdg ticket 50 on` (50 is adjustable) and `/opdg exchange on`. Then `/dg`, choose category, reward, confirm. No Vault payment. Verify actual token consumption, persistent claim, Private entry on staging. No production gateway in this milestone.
6. For the four existing temporary mount permissions: manually set `permission-rewards-enabled: true` and exact `allowed-permission-nodes` in `exchange/settings.yml`, e.g. `mcpet.capybara_mount`, `mcpet.horse_spartan`, `mcpet.raptor_red`, `mcpet.phantom_ghost`. Restart or `/opdg reload`. They require LuckPerms and duplicate REJECT. The previous allowlist did not authorize these nodes; no automatic widening. Test real permissions and expiry, including offline and failure cases. COMMAND/BUNDLE are intentionally locked; none in the supplied active catalog.

## Files
```
ArunikaDungeon/exchange/
  settings.yml                 # enable flags, ticket cost, permission allowlist
  live/
    config.yml                 # original token and legacy configuration
    rewards.yml                # original reward IDs, item bytes, cost, stock, limits
    exchange.db                # original history + new ADG transactions, stock, purchase limit
    migration-complete.txt     # source hashes
    legacy-original/           # raw preserved files including WAL/SHM if present
ArunikaDungeon/data/tickets/   # shared universal ticket ledger
ArunikaDungeon/backups/exchange-edit-*/ # per-editor previous YAML
```

## Commands (source implementation, runtime unverified)
`/dg` catalog, `/dg claim <serial>`, `/opdg status`, `/opdg reload`, `/opdg rewards`, `/opdg token set` (held exact token), `/opdg exchange on\|off`, `/opdg ticket <token-cost> <on\|off>`, `/opdg addhand <new-key> <cost> <category>`, `/opdg cost <key> <cost>`, `/opdg category <key> <name>`, `/opdg enable <key> true\|false`, `/opdg tx list`, `/opdg tx resolve <id> <no-debit\|delivered>` (OP only). `/adg exchange` and `/adg exchange admin ...` fallbacks. Changes save atomically, retain previous YAML; existing reward UUID remains unchanged. Stock and purchase limits are imported. Stock edits/reconciliation are not exposed by the editor; mismatched stock config refuses purchase rather than resetting stock. For token changes stop battle preview and restart after edit so the loot adapter does not retain old cached token. Do not replace original token unless intentionally migrating currency. Admin GUI editing will be expanded later.

## Failure semantics
Stock and per-user purchase limit are reserved in one database transaction, with durable `ADG_RESERVED`. Before changing inventory, an unavailable player, insufficient tokens or full inventory cancels safely and restores stock/limit. After inventory changes, issue no automatic refund or reward replay. `ADG_DEBITED` means debit recorded; it does not guarantee item/permission delivery. On hard crash, inspect both inventory/player files, ticket record (transaction UUID), and LuckPerms before verifying `/opdg tx resolve`. `no-debit` requires a RESERVED transaction and proof of no token debit/delivery; `delivered` is a manual acknowledgment, does not issue any reward. Operator must manually reconcile and document exceptional compensation. If uncertain, leave pending; that player cannot purchase again until resolved. Ticket claims are replay-safe at redemption (same serial, not repeated value); offline and full inventory claims remain in ledger. Never delete ledger. SQLite/Paper/LuckPerms are distinct persistence systems, not an atomic distributed transaction. Do not live migrate; do not restore old DB after new player transactions.

## Runtime checklist
Paper build & startup; compare all 23 icons/prices; legacy token exact match (storage + offhand), invalid token rejection; full inventory; rapid click/shift/drag/number key; stale menu; stock 1 concurrent; limit; restart while reserved/debited; ticket claim then Private entry; LuckPerms duplicate and expiry; Web/Geyser inventory navigation on actual Bedrock mobile device; old `/dg` priority when old plugin present; TPS/MSPT & disk latency. Native inventory is intended to work through Geyser but has not been client-tested. No source-data snapshot with live player identifiers is included in the distributed ZIP.
