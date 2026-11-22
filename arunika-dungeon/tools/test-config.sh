#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
out=$(mktemp -d); trap 'rm -rf "$out"' EXIT
base=src/main/java/site/cadera/arunika/dungeon
javac --release 21 -d "$out" "$base/core/RpgPolicy.java" "$base/core/DungeonCatalog.java" "$base/core/SessionRegistry.java" "$base/battle/BattlePlan.java" "$base/battle/LootPlan.java" "$base/config/SmartConfiguration.java" "$base/config/ConfigTransaction.java" src/test/java/site/cadera/arunika/dungeon/config/ConfigTest.java
java -cp "$out" site.cadera.arunika.dungeon.config.ConfigTest
