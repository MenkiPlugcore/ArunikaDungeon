#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
# Remove stale output before any attempt; never distribute target/ as verified.
rm -rf target/verified
mkdir -p build-logs
exec > >(tee build-logs/verified-build.log) 2>&1
command -v mvn >/dev/null || { echo 'Maven missing; install Maven 3.9.x and Java 21.'; exit 127; }
java -version
mvn -version
# Maven compiles tests but the main-method suites are executed explicitly below.
mvn -B -ntp clean verify -DskipTests org.apache.maven.plugins:maven-dependency-plugin:3.8.1:build-classpath -Dmdep.outputFile=target/dependencies.txt
cp="target/classes:target/test-classes:$(cat target/dependencies.txt)"
java -cp "$cp" site.cadera.arunika.dungeon.core.CoreTest
java -cp "$cp" site.cadera.arunika.dungeon.battle.BattleTest
java -cp "$cp" site.cadera.arunika.dungeon.battle.GatewayTest
java -cp "$cp" site.cadera.arunika.dungeon.battle.LootTest
java -cp "$cp" site.cadera.arunika.dungeon.shared.SharedTest
java -cp "$cp" site.cadera.arunika.dungeon.privategame.PrivateTest
java -cp "$cp" site.cadera.arunika.dungeon.config.ConfigTest site.cadera.arunika.dungeon.config.SnakeYamlCodec
java -cp "$cp" site.cadera.arunika.dungeon.core.DatabaseMigrationTest
java -cp "$cp" site.cadera.arunika.dungeon.exchange.ExchangeTest jdbc
java -cp "$cp" site.cadera.arunika.dungeon.config.AdminConfigTest site.cadera.arunika.dungeon.config.SnakeYamlCodec
java -cp "$cp" site.cadera.arunika.dungeon.battle.RpgTest
java -cp "$cp" site.cadera.arunika.dungeon.core.ShutdownTest
java -cp "$cp" site.cadera.arunika.dungeon.core.PerformanceGuardTest
java -cp "$cp" site.cadera.arunika.dungeon.exchange.ExchangeStressTest
java -cp "$cp" site.cadera.arunika.dungeon.config.RecoveryJournalTest
java -cp "$cp" site.cadera.arunika.dungeon.exchange.AccountingGuardTest
java -cp "$cp" site.cadera.arunika.dungeon.battle.LootRecoveryTest
python3 tools/test-smoke.py
java tools/VerifyArtifact.java target/ArunikaDungeon-1.0.0.jar target/dependencies.txt target/verified
