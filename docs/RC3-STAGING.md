# RC3 — Runtime stabilization preparation (build blocked)

## Actual results
Based on the exact RC2 source ZIP, extracted and CRC checked. No gameplay features added. Java 21 present; Maven not installed. Both Maven Central and Paper repository DNS requests failed again. The alternative download route could not retrieve Maven or Paper metadata. No connected ArunikaDungeon repository found. The official build script exits 127 before compilation; no actual Paper compile diagnostics, no plugin JAR, and no live runtime verification.

## Source changes
- onEnable catches LinkageError as well as Exception, logs the full stack trace, and disables the plugin instead of hiding the original failure.
- ShutdownSteps isolates RuntimeException and LinkageError for each cleanup operation; failures retain their full stack trace. Fatal errors such as AssertionError are not swallowed. Scheduler cancellation occurs before module cleanup. Service unregistration and registry cleanup have independent steps.
- PaperExchange.close calls worker.shutdown in a finally block even if inventory/listener teardown fails. This does not prove thread termination or transaction durability; actual shutdown and fault injection testing remain outstanding.
- Version metadata and the official dependency build gate advanced to RC3. New ShutdownTest runs as part of the verified pipeline. Config schema, session/loot/ticket/Exchange algorithms and permissions remain unchanged. No API stubs.

## Regression evidence
See docs/rc3-tests/: 286 prior standalone checks, 3 new shutdown checks, 4 negative artifact checks, 5 Python smoke-tool tests. 16 SQLite statement checks against a disposable migrated legacy archive copy. Migration retained 113 transaction rows, 109 purchase rows, 32 stock rows and 22 input files without alteration. Standalone configuration tests use fixture codec. Real SnakeYAML, SQLite JDBC and Paper remain untested until official build.

## Next action: compile once on a networked machine
Use JDK 21, Maven 3.9.x, and internet access to official repositories. Windows PowerShell, from the extracted project folder:

```powershell
powershell -ExecutionPolicy Bypass -File tools/build-verified.ps1
```

Or upload project contents to your own private GitHub repo including `.github/workflows/build.yml` and run **Verified dependency build**. Download `RC3-build-diagnostics` on failure or `ArunikaDungeon-1.0.0-RC3-staging` on success. No repo has been created or modified by this task. Send the failed compiler output if it fails. Verified artifact path: `target/verified/ArunikaDungeon-1.0.0-RC3.jar` and adjacent `.sha256`. Never use an unverified candidate.

## Isolated smoke test (prepared, not executed against Paper)
Requirements: Python 3.11+, JDK 21, official Paper 1.21.11 server JAR, verified RC3 JAR with adjacent checksum; internet may be required to bootstrap Paper and plugin libraries. Read Minecraft EULA before passing `--accept-eula`. Use a new folder and an unused local TCP port; no production server files are copied. The tool does not download Paper for you or certify the publisher of the supplied JAR. It checks plugin hash and manifest, records both JAR hashes and binds a loopback-only test server with whitelist, no query/RCON. The default configuration keeps battle, loot, and gateway disabled.

```powershell
python tools/smoke-paper.py --paper "C:\Paper\paper-1.21.11.jar" --plugin "target\verified\ArunikaDungeon-1.0.0-RC3.jar" --output "rc3-smoke" --accept-eula
```

It starts twice in the same new test directory, checks enable markers, namespaced status, foundation DB ready, config reload, safe disabled flags, shutdown marker and nonzero exit/error diagnostics. Reports `boot-1.log`, `boot-2.log`, and `smoke-result.json` (if reached). A missing legacy Exchange catalog is expected in this blank instance; Exchange is not tested. The tool is a smoke gate only, not a server performance or anti-dupe certification. Unit tests cover log parsing and rejection of invalid artifacts, not a Paper process. Do not use an existing production folder or execute a live reload command in production. For a second smoke run choose a new output directory.

## Remaining gates
1. Official Maven compile, dependency-backed tests, actual JAR integrity and checksum.
2. Blank Paper smoke run and restart, check no exceptions and clean process exit.
3. Separate backed-up staging clone: MythicMobs, CdrWaystone/Bedrock, Nusa Karsa, LuckPerms, MMOItems, ItemsAdder and Geyser/Floodgate; legacy migrations; conflict gates. Do not remove old production plugins.
4. Real Java and Bedrock clients, ticket/refund, Exchange crash reconciliation, pickup/external plugins, projectile/summon isolation, and spark profiling. RC3 is NOT approved for production or called runtime-stable.

Stop increasing version numbers until the genuine compile and runtime log are available.
