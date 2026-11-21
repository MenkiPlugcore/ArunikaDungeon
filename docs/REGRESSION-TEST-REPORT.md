# Regression tests — real dependencies

Baseline RC6 and final 1.0.0 were built locally with Java 21, real Paper API 1.21.11, SnakeYAML 2.3, SQLite JDBC 3.46.1.3 and LuckPerms API 5.4. No dependency stubs were used. Canonical command: `bash tools/build-verified.sh` (requires Maven and Java 21).

The suites use explicit main-method assertions. Maven compiles production/tests and packages the JAR; the build script then executes every suite and the artifact verifier. A bare `mvn test` is not the complete gate.

| Suite | Checks | Result |
|---|---:|---|
| Core | 27 | PASS |
| Battle | 30 | PASS |
| Gateway policy | 23 | PASS |
| Loot policy/lifecycle | 22 | PASS |
| Shared | 34 | PASS |
| Private/ticket lifecycle | 48 | PASS |
| Configuration with real SnakeYAML | 72 | PASS |
| Foundation DB migration | 6 | PASS |
| Exchange with real SQLite | 30 | PASS |
| Admin configuration | 12 | PASS |
| RPG policy | 11 | PASS |
| Shutdown isolation | 3 | PASS |
| Performance Guard | 10 | PASS |
| SQLite concurrent/recovery stress | 8 | PASS |
| New recovery-journal regression | 5 | PASS |
| New Exchange accounting/symlink regression | 5 | PASS |
| New unissued-loot evidence regression | 5 | PASS |
| Python smoke utility tests | 5 | PASS |

351 Java checks plus five Python utility tests. Artifact verifier checks class headers (Java 21), every source class/resource, archive CRC, direct dependency representative classes and SHA-256, plugin version and manifest version. It rejects bundled external APIs. Final archive has 107 plugin classes, including nested classes.

Gateway policy tests are not client interaction tests. SQLite stress produced exactly ten successful deliveries from fifty requests for stock ten, and integrity_check passed. Ticket lifecycle/claim/refund tests operate on durable real files. Owner client and third-party plugin checks are listed separately as NOT TESTED.

The build transcript is BUILD-LOG.txt. Source audit does not convert a NOT TESTED runtime gate into PASS.
