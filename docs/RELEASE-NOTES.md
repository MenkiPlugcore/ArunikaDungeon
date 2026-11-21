# ARUNIKA DUNGEON v1.0.0 — GUARDIAN AWAKENING

**Verified build candidate — staging / draft. Production acceptance incomplete.**

This candidate consolidates the exact verified RC6 source into src/main/java, src/main/resources and src/test/java. Historical RC archives and patch scripts remain retained. Canonical builds compile directly with Java 21 and Maven, run real dependency regression and verify archive/manifest/SHA-256 before packaging.

Fixed stock accounting invariants, full-journal configuration recovery validation, Exchange file-symlink handling, ticket appearance reload, Universal Ticket admin buttons and drop-edit admission guards. Added durable evidence for unissued loot, more measured diagnostics and consistent native GUI colors.

Features and costs remain: Shared free, 30-minute boss-defeat cooldown; Private solo/party up to five, one Universal Ticket per party and default three simultaneous sessions. Universal ticket initial Exchange cost is 50 genuine Dungeon Tokens after the admin registers currency. Fresh Exchange initializes automatically but remains locked until explicitly configured.

Assets: verified JAR, exact source archive, SHA256SUMS and documentation ZIP. See FULL-AUDIT-REPORT.md, REGRESSION-TEST-REPORT.md, RUNTIME-TEST-REPORT.md and KNOWN-ISSUES.md. Back up the entire matched data/players/SQLite state; retain separate Waystone JARs. Do not deploy to live players until owner runtime/client and load gates pass.
