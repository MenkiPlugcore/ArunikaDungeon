param([string]$LegacyRoot = "")
$ErrorActionPreference = 'Stop'
Push-Location (Join-Path $PSScriptRoot '..')
$out = Join-Path ([System.IO.Path]::GetTempPath()) ([guid]::NewGuid().ToString())
New-Item -ItemType Directory $out | Out-Null
try {
  $sources = @(Get-ChildItem src/main/java/site/cadera/arunika/dungeon/core/*.java | ForEach-Object FullName)
  $sources += (Resolve-Path src/test/java/site/cadera/arunika/dungeon/core/CoreTest.java).Path
  & javac --release 21 -d $out @sources
  if ($LASTEXITCODE -ne 0) { throw 'Compilation failed' }
  if ($LegacyRoot) { & java -cp $out site.cadera.arunika.dungeon.core.CoreTest $LegacyRoot } else { & java -cp $out site.cadera.arunika.dungeon.core.CoreTest }
  if ($LASTEXITCODE -ne 0) { throw 'Tests failed' }
} finally { Remove-Item -Recurse -Force $out; Pop-Location }
