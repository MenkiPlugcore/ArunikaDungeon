#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
out=$(mktemp -d); trap 'rm -rf "$out"' EXIT
base=src/main/java/site/cadera/arunika/dungeon
javac --release 21 -d "$out" "$base/core/SessionRegistry.java" "$base/battle/"*.java src/test/java/site/cadera/arunika/dungeon/battle/BattleTest.java
java -cp "$out" site.cadera.arunika.dungeon.battle.BattleTest
