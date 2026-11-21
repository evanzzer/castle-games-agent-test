#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p build/check
sources=()
for source in src/castle/*.java; do
    if ! rg -q '^import android' "$source"; then sources+=("$source"); fi
done
checks=()
for source in tests/*Check.java; do
    case "$source" in *UiCheck.java|*TerrainBakeCheck.java) continue;; esac
    checks+=("$source")
done
javac --release 8 -d build/check "${sources[@]}" "${checks[@]}"
for source in "${checks[@]}"; do
    name="${source##*/}"
    java -cp build/check "castle.${name%.java}"
done
python3 tests/architecture_check.py
python3 tests/assets_check.py
python3 tests/frontier_data_check.py
python3 tests/wilds_data_check.py
