# ArunikaDungeon 1.0.0 — Guardian Awakening

Verified staging candidate. **Production gate remains open** pending real clients, providers and load acceptance. See [release notes](docs/RELEASE-NOTES.md) and [known issues](docs/KNOWN-ISSUES.md).

## Build

Java 21 and Maven are required. `bash tools/build-verified.sh` compiles directly from canonical src/, executes real-dependency regression suites and verifies archive/manifest/dependency hashes before producing `target/verified/ArunikaDungeon-1.0.0.jar`. `python3 tools/package-release.py` packages source, documentation, log and checksums. A bare Maven test invocation is not the entire main-method test gate.

## Setup

Start with [installation](docs/INSTALLATION-GUIDE.md), [migration](docs/MIGRATION-GUIDE.md), [dungeon setup](docs/DUNGEON-SETUP-GUIDE.md), [Waystone setup](docs/WAYSTONE-SETUP-GUIDE.md), [Exchange setup](docs/EXCHANGE-SETUP-GUIDE.md), [commands](docs/ADMIN-COMMANDS.md) and [rollback](docs/ROLLBACK-GUIDE.md).

Shared/Private behavior and separate Waystone JARs are preserved. Defaults stay locked until an admin configures valid arenas, mobs, currency/rewards and explicit preview switches. No server data is bundled.

## Provenance and release

Source comes from the successful RC6 workflow 44885102867 at 35f397807cc4f848f589673a9bf1cca33f4cfa37. [Provenance](docs/SOURCE-PROVENANCE.json), [audit](docs/FULL-AUDIT-REPORT.md), [regression](docs/REGRESSION-TEST-REPORT.md), [performance](docs/PERFORMANCE-REPORT.md), [dependency evidence](docs/DEPENDENCY-COMPATIBILITY.md) and [runtime checklist](docs/RUNTIME-ACCEPTANCE-CHECKLIST.md) distinguish tested and untested scope.

Historical RC3 archive, patch chain and workflows remain available for reproducibility; the Guardian Awakening workflow builds canonical root source and prepares a draft release only. Never infer production readiness from the version number or successful compilation.
