#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
cp="target/classes:target/test-classes:$(cat target/dependencies.txt)"
for cls in \
 site.cadera.arunika.dungeon.paper.GatewayAimTest \
 site.cadera.arunika.dungeon.battle.TeleportSafetyTest \
 site.cadera.arunika.dungeon.paper.GatewayMenuStyleTest \
 site.cadera.arunika.dungeon.paper.GatewayPhase6Test; do
  java -cp "$cp" "$cls"
done
