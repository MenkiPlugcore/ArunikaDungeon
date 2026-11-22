#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
out=$(mktemp -d); trap 'rm -rf "$out"' EXIT
base=src/main/java/site/cadera/arunika/dungeon
javac --release 21 -d "$out" "$base/core/SessionRegistry.java" "$base/privategame/"*.java src/test/java/site/cadera/arunika/dungeon/privategame/PrivateTest.java
java -cp "$out" site.cadera.arunika.dungeon.privategame.PrivateTest
