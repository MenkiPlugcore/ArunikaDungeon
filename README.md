# ArunikaDungeon — Guardian Awakening RC3

Release candidate; not production-ready. This repository is initially a build bootstrap, not the source distribution.

## Build RC3

1. Upload the exact `ArunikaDungeon-v1.0.0-RC3-source.zip` archive from the chat to the repository root on `main` (do not rename or extract it).
2. Expected SHA-256: `d3fd3eacfa681c74b7ce8d959b0ace8ffc6bc7a50d9f10eb931abb719061cbdb`.
3. Open [Actions](https://github.com/MenkiPlugcore/ArunikaDungeon/actions). The archive build workflow runs on upload, or can be started manually once the archive exists.
4. Download the verified staging artifact only if every build step succeeds. If it fails, inspect the diagnostics and compiler output.

This repository is public. Change it to private before uploading if the source should not be public. Never upload production databases, player data, credentials, or third-party plugin JARs.

Target: Java 21, Paper 1.21.11. A successful build is not proof of runtime compatibility. Do not replace production dungeon plugins before staging tests pass.
