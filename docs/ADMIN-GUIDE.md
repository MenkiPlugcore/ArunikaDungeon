# Admin management (staging)
Permission `arunika.dungeon.admin` (default OP). `/adg admin` opens a native 54-slot inventory. Floor, wave and Exchange catalog lists paginate when necessary; sessions are read on demand. No repeating admin GUI task. Permission, owner, active inventory and click type are revalidated; drag, shift and number-key inventory transfers are cancelled. All production switches remain disabled by default.

## Arena
1. `/adg admin` -> floor -> Arena Setup. Choose shared_1/private_1 or run `/adg setup arena <floor> <arena-id> <shared\|private>`.
2. Click Corner A/B and capture positions by standing at each corner. Every capture closes GUI so you can move; reopen draft with `/adg setup resume`. Capture player spawn, boss spawn, one or more minion positions, and exit. The exit must be outside all arenas. For a bound Waystone gateway, exits must equal its configured hub point. Draft is in memory until Save (lost on disconnect).
3. Save validates coordinates, max arena extent (512 blocks each axis), existing enabled arena isolation and complete config before atomic commit. Replacing an arena with the same ID requires deliberately selecting that ID. No region/world cloning.
4. For a protected active dungeon, configure new points before enabling it and ensure spawn chunks are loaded during staging checks.

## Wave and boss
GUI presents existing wave numbers and count controls. Actual Mythic IDs are entered through command, not fabricated from legacy names. Commands:
```
/adg setup disable queen_bee
/adg setup count queen_bee 3
/adg setup wave queen_bee 1 ActualMinionID 8
/adg setup append-wave queen_bee 1 ActualEliteID 2
/adg setup wave queen_bee 2 ActualMinionID 12
/adg setup wave queen_bee 3 ActualEliteID 3
/adg setup boss queen_bee ActualBossID
/adg setup enable queen_bee
```
`wave` replaces the group list in that wave; `append-wave` adds another group. Actual MythicMobs definition/spawn method validated when editing ID. No actual spawn for this check. The enable button enables the battle configuration only; global preview flags must still be configured separately. Some prior enabled legacy settings impose additional requirements. A wave count increase while enabled requires valid existing waves; disable first. Remove count does not delete YAML/drop files.

## Loot
Choose floor, mode, stage; adjust chance by 5%, min/max by 1, then Add held item and confirm. Actual held ItemStack serialized through the existing editor; ticket-tagged templates rejected. Entry keys generated as `gui_<uuid>`. Drop files automatically backed up, and `/adg reload all` applies saved drops. Existing entry count displayed; arbitrary exact chance, removal and editing by key remain available with original `/adg loot addhand ...` and `/adg loot remove ...` commands/YAML. Ticket custom entry is YAML-based (provider creates a fresh serial). GUI does not take items from players' inventory. Loot remains physical free-for-all and mode-specific.

## Exchange
The native admin panel lists imported rewards and permits cost changes, enable/disable and adding a held item to Items category. Advanced category/stock/limit/permission choices retain original command/YAML route. Legacy plugin present means Exchange stays locked. Never run both purchase engines concurrently. All edits routed through existing Exchange validation/transaction lock and backup. No money-based Private entry.

## Monitoring and safety
Session monitor shows floor/phase, arena ID, session UUID, wave, alive/pending mobs and time remaining. Refresh manually; stop requires confirmation. Existing `/adg shared status` shows queue/participants, `/adg status` shows plugin readiness. Config edits are refused for active sessions, pending returns or queue/party state, and Exchange writes/transactions. Do not use production until Paper build, staging migration, inventory events, actual Java/Bedrock GUI, Geyser crossplay and rollback are tested. Config commit validated before write; Paper runtime failure after save is reported, not guaranteed to roll back API failures.
