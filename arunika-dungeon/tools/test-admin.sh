#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
out=$(mktemp -d); trap 'rm -rf "$out"' EXIT
base=src/main/java/site/cadera/arunika/dungeon
test=src/test/java/site/cadera/arunika/dungeon
javac --release 21 -d "$out" "$base/core/"*.java "$base/battle/"*.java "$base/config/SmartConfiguration.java" "$base/config/ConfigTransaction.java" "$test/config/ConfigTest.java" "$test/config/AdminConfigTest.java" "$test/battle/BattleTest.java" "$test/battle/RpgTest.java"
java -cp "$out" site.cadera.arunika.dungeon.config.AdminConfigTest
java -cp "$out" site.cadera.arunika.dungeon.battle.RpgTest
