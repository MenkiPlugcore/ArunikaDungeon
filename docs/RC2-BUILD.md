# RC2 — Build gate & verification report

## Outcome
- Java 21.0.12.1 available; Maven missing (exit 127). DNS resolution fails for Maven Central and Paper repository. No cached Paper/Spigot API or Maven found. No connected ArunikaDungeon repository found. Official dependency compilation was NOT completed; no compiler diagnostics against the actual Paper API were available to fix. No Minecraft plugin JAR produced. No gameplay change and no API substitute/stub build.
- RC1 source verified against RC2: all 62 source/resource files identical except version strings/comments. Config schema/staging restrictions remain unchanged.
- 286 standalone Java assertions rerun across ten suites (incl. 2 legacy archive checks); 16 SQLite statement checks; offline migration retained 113 transactions, 109 purchases, 32 stock rows and 22 unchanged source files. 45 Java files parsed (syntax only). Four negative artifact gate checks passed. Real SnakeYAML, SQLite JDBC, Paper, Java/Bedrock, and provider integration remain untested. See `rc2-tests/`.

## Build pipeline changes
- Explicit compiler release 21; fixed compiler, JAR, Surefire, dependency-plugin versions; fixed output timestamp. Paper SNAPSHOT remains mutable; hashes record the dependency files used, not an independent verification of their publisher or a lockfile.
- Maven `clean verify -DskipTests` compiles tests, then scripts run all 11 main-method suites explicitly (including real SnakeYAML codec and JDBC tests). Any nonzero result stops publication. Candidate JAR in `target/` is NOT verified.
- `VerifyArtifact.java` checks archive entries, class headers and Java major version 65, class completeness against source, identical resource files, CRC, plugin metadata, no unexpected bundled classes, and expected direct dependencies with representative classes. It records SHA-256 for all dependency JARs and final JAR. This is a structural/build gate, not runtime linkage certification or cryptographic publisher verification.
- Verified output directory is removed at attempt start, then published only after all stages pass. CI uploads verified directory on success and build logs even when compilation fails. Workflow not executed in this environment. Windows script is prepared but not run here.

## Windows (local build)
1. Install JDK 21 and Maven 3.9.x from https://maven.apache.org/download.cgi . Set JAVA_HOME to JDK 21 and add Maven bin to PATH. Open a new PowerShell and run `java -version` and `mvn -version`; both should report JDK 21.
2. Extract source ZIP and open PowerShell in the inner folder containing `pom.xml`. Run:

   ```powershell
   powershell -ExecutionPolicy Bypass -File tools/build-verified.ps1
   ```

3. On success check `target/verified/ArunikaDungeon-1.0.0-RC2.jar`, `.jar.sha256`, `dependencies.sha256` and `BUILD-STATUS.txt`. Compare the checksum with `Get-FileHash target/verified/ArunikaDungeon-1.0.0-RC2.jar -Algorithm SHA256`. Never install an unverified candidate from `target/` or a source ZIP. On failure copy the full console output including `[ERROR]` lines; resolve actual diagnostics before trying again.

## GitHub Actions
Create/use a project repository you control (private recommended), upload extracted folder contents including `.github/workflows/build.yml`, and open Actions > Verified dependency build > Run workflow. The workflow also triggers on push. Java 21 and Maven are installed by the runner/setup action. On success download artifact `ArunikaDungeon-1.0.0-RC2-staging`. On failure download `RC2-build-diagnostics` or open the failed step log. No repo was created or changed by this task. No credentials or server player data should be uploaded.

## Runtime approval
After successful build: follow RC1 installation/rollback guide on an isolated staging server, verify startup and actual plugin versions, test MythicMobs, old CdrWaystone/Bedrock, Nusa Karsa, Java and Bedrock inventory and movement, transaction/crash tests and spark profiling. RC2 verified build (if successful later) is still not production approval. Do not replace existing production dungeon or Waystones before these pass.
