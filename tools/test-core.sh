#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
out=$(mktemp -d); trap 'rm -rf "$out"' EXIT
javac --release 21 -d "$out" src/main/java/site/cadera/arunika/dungeon/core/*.java src/test/java/site/cadera/arunika/dungeon/core/CoreTest.java
java -cp "$out" site.cadera.arunika.dungeon.core.CoreTest "$@"
