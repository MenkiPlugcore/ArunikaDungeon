$ErrorActionPreference = 'Stop'
Push-Location (Join-Path $PSScriptRoot '..')
try {
  if (Test-Path target/verified) { Remove-Item target/verified -Recurse -Force }
  if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) { throw 'Maven missing; install Maven 3.9.x and Java 21.' }
  & java -version
  & mvn -version
  & mvn -B -ntp clean verify '-DskipTests' 'org.apache.maven.plugins:maven-dependency-plugin:3.8.1:build-classpath' '-Dmdep.outputFile=target/dependencies.txt'
  if ($LASTEXITCODE -ne 0) { throw 'Maven build failed' }
  $cp = 'target/classes;target/test-classes;' + (Get-Content target/dependencies.txt -Raw).Trim()
  foreach ($test in @('site.cadera.arunika.dungeon.core.CoreTest','site.cadera.arunika.dungeon.battle.BattleTest','site.cadera.arunika.dungeon.battle.GatewayTest','site.cadera.arunika.dungeon.battle.LootTest','site.cadera.arunika.dungeon.shared.SharedTest','site.cadera.arunika.dungeon.privategame.PrivateTest','site.cadera.arunika.dungeon.config.ConfigTest','site.cadera.arunika.dungeon.core.DatabaseMigrationTest','site.cadera.arunika.dungeon.config.AdminConfigTest','site.cadera.arunika.dungeon.battle.RpgTest','site.cadera.arunika.dungeon.core.ShutdownTest')) {
    if ($test.EndsWith('ConfigTest')) { & java -cp $cp $test site.cadera.arunika.dungeon.config.SnakeYamlCodec } else { & java -cp $cp $test }
    if ($LASTEXITCODE -ne 0) { throw "Test failed: $test" }
  }
  & java -cp $cp site.cadera.arunika.dungeon.exchange.ExchangeTest jdbc
  if ($LASTEXITCODE -ne 0) { throw 'Exchange JDBC test failed' }
  & java tools/VerifyArtifact.java target/ArunikaDungeon-1.0.0.jar target/dependencies.txt target/verified
  if ($LASTEXITCODE -ne 0) { throw 'Artifact verification failed' }
} finally { Pop-Location }
