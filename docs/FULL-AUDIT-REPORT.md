# Full audit — 1.0.0 staging

## Baseline and scope

Baseline: GitHub Actions run 44885102867, commit 35f397807cc4f848f589673a9bf1cca33f4cfa37, source artifact 14699682075. The downloaded artifact contains ArunikaDungeon-v1.0.0-RC6-source.zip. SOURCE-PROVENANCE.json records its SHA-256 and the reviewed source inventory.

Reviewed all 36 baseline production Java files across battle, config, core, Exchange, Paper adapters, Private and Shared; reviewed resource defaults, test runners, verifier and previous workflow. The result is a code review and automated test evidence, not an exhaustive proof against all defects.

## Findings and disposition

| Finding | Impact | Disposition |
|---|---|---|
| Finite remaining stock could exceed configured stock | Invalid stored state could permit overselling | Fail closed before reservation; SQLite regression |
| Unlimited stock accepted a finite remaining sentinel | Corrupt accounting accepted | Require configured=-1 and remaining=-1 |
| Direct ledger reservation trusted reward numeric ranges | Invalid internal caller could bypass catalog parsing | Validate cost/stock/limit/revision at accounting boundary |
| Recovery restored early files before checking later journal entries | Broken late entry left a partially restored configuration | Validate/preload all preimages before first mutation |
| Existing Exchange children could be symlinked | Opening unintended database/configuration | Reject symlink children, retain data |
| Exchange ticket appearance survived config reload | New tickets used stale presentation | Reconfigure ticket adapter after snapshot apply |
| Universal Ticket GUI used ordinary reward-edit commands | Cost/toggle buttons rejected the reserved item | Route to the dedicated ticket command |
| Drop editor lacked pending-return/storage guard | Inconsistent edit admission compared with other editors | Apply the same guards |
| Loot budget/location truncation discarded unissued quantities | Reward loss on resource pressure | Durable unissued-loot recovery batches; manual review |
| Diagnostics omitted existing measured counts | Poor operational visibility | Add direct state counters; no polling job |

## Preserved invariants

One ticket per Private party; max five players; default three active Private sessions; free Shared with 30-minute cooldown after boss defeat. Ticket serial/generation and EXPOSED state prevent repeat redemption/refund after exposure. Exchange reserves stock and purchase limits atomically. Inventory and SQLite are separate durability systems: uncertain transactions remain pending rather than replaying delivery.

Source consolidation does not replace server configuration, databases or existing ticket records. Waystone plugins remain separate JARs. Optional providers are detected and unavailable features remain locked. No remote server files were modified.

## Limits

Native summon/pet ownership, Mythic skill side effects, Geyser proxy interactions, third-party inventories, client GUI behavior and realistic load remain acceptance gates. Unissued loot requires supervised compensation; the new record is evidence, not an automatic delivery engine. See KNOWN-ISSUES.md and RUNTIME-ACCEPTANCE-CHECKLIST.md.
