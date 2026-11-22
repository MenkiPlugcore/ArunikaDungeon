# RC2 — verification report, 22 November 2026

Release scope: admin exchange GUI, named Dungeon Waystone setup GUI, safe arena spawn capture, backward-compatible registry labels. Dungeon 1.0.3-RC2, core/Bedrock 1.0.1-RC2. RC/staging; no real-client acceptance claim.

| Check | Result |
|---|---|
| Maven compile/package with real Paper/Geyser dependencies | PASS; JDK 21, no fabricated dependency classes |
| JUnit tests | 41 passed, 0 failures/errors/skips (30 Dungeon, 6 core, 5 Bedrock) |
| Historical verifier rejection cases | 4 passed |
| Python smoke monitor tests | 5 passed |
| Two starts of isolated Paper 1.21.11 build132 + Geyser2.12.1 build1280 | PASS for startup, status/diagnostics/registry list, database readiness, reload, clean process exit |
| Typed Geyser entity registration | 2/2 registered on both starts |
| Delivered JARs versus tested JARs | SHA-256 matches, recorded in runtime-result.json |
| Source classes/resources vs JAR | PASS, Java class major65; no bundled API stubs |
| Registry migration/name validation | Legacy unnamed file read without overwrite; UUID/role/target/world/coords retained through codec round trip; duplicate/invalid names and malformed records rejected |
| Config GUI backend | Modes on/off retain foundation flag; spawn update preserves bounds/mob/boss/exit; wrong world rejected |
| New UI input validation | Invalid categories, command/control characters, negative/overflow/NaN prices rejected |
| GUI clicks, chat prompts and teleport on real Java/Bedrock clients | NOT TESTED |
| ItemsAdder model placement/scale/rotation and vendor gameplay | NOT TESTED |

Tests exercise codecs/config/domain/transaction code; they do not simulate a signed-in player clicking every GUI button. Real Paper test uses no ItemsAdder, MythicMobs or Floodgate. Arena/boss provisioning and gameplay session acceptance must be tested on a staging copy of the user's server.

Runtime caveats recorded in logs: isolated host could not resolve Mojang authentication/public-key and STUN hosts; Geyser reports interface discovery/network failures, transport notices and protocol26.3 compatibility warning, and a late locale-cache/classloader warning during shutdown. Xbox-auth disabling existed only in this loopback test fixture so bootstrap could be inspected. No test-fixture network/auth configuration is shipped. These starts establish plugin bootstrap/registration, not Bedrock connectivity, authentication or full provider compatibility.

No TPS/MSPT benchmark or real-client rendering measurement was performed. Existing survival Waystone data and production databases were not accessed. Legacy transaction/ticket tests remain passing; new GUI forwards to existing editor backend rather than duplicating debit/delivery logic.

See historical-RC1 for the baseline audit and GUI-SETUP-ID.md for current installation and acceptance steps.
