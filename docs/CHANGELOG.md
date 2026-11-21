# 1.0.0 — Guardian Awakening (staging)

Canonical source imported from the exact RC6 workflow artifact, not reconstructed from RC3. Historical patch scripts and archives remain available.

- Reject inconsistent finite/unlimited stock and invalid direct ledger accounting before reservation.
- Refuse symlinked existing Exchange files without resetting the database.
- Validate the entire configuration recovery journal before restoring any preimage; reject duplicate paths and missing preimages.
- Refresh Exchange ticket appearance on configuration reload.
- Correct Universal Ticket admin GUI cost/toggle commands.
- Preserve unissued budget/location loot as durable staff-review records rather than silently truncating it. No automatic replay.
- Add measured arena, pending spawn, loot entity, queue and party counts to diagnostics.
- Apply consistent native inventory colors and black footer panes without animations.
- Check version 1.0.0 in POM, plugin.yml, manifest and artifact verifier.
- Add 15 regression checks for the new accounting, recovery and unissued-loot paths.

Production approval remains blocked by live provider/client acceptance. Existing preview switches and permissions remain explicit opt-ins.
