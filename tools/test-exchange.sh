#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
out=$(mktemp -d); trap 'rm -rf "$out"' EXIT
base=src/main/java/site/cadera/arunika/dungeon
javac --release 21 -d "$out" "$base"/exchange/*.java "$base/privategame/TicketLedger.java" src/test/java/site/cadera/arunika/dungeon/exchange/ExchangeTest.java
java -cp "$out" site.cadera.arunika.dungeon.exchange.ExchangeTest
