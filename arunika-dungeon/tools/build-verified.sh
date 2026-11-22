#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
mvn -B -ntp clean verify
bash arunika-dungeon/tools/test-verifier.sh
python3 arunika-dungeon/tools/test-smoke.py
python3 scripts/package.py
