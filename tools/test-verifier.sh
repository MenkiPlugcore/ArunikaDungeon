#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
out=$(mktemp -d); trap 'rm -rf "$out"' EXIT
javac --release 21 -d "$out" tools/VerifyArtifact.java tools/RC2VerifierTest.java src/main/java/site/cadera/arunika/dungeon/core/StagingPolicy.java
java -cp "$out" RC2VerifierTest "$out"
