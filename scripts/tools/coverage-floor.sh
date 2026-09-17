#!/usr/bin/env bash
# Пол покрытия Swift-пакета по llvm-cov JSON: в Swift 6.4 отчёт лежит в $(swift build --show-bin-path)/codecov/<Package>.json (06 L-17).
# Kotlin-полы держит Kover (koverVerify) — здесь только Swift, чтобы не было второго способа.
#   coverage-floor.sh <package-dir> <floor%>   → OK:/FAIL: coverage swift; SKIP (код 0), пока пакета нет
set -uo pipefail
PKG="${1:?package-dir}"
FLOOR="${2:?floor}"
cd "$(dirname "$0")/../.."
LABEL="coverage swift"

if [ ! -d "$PKG" ]; then
    printf 'SKIP: %-24s %s отсутствует\n' "$LABEL" "$PKG"
    exit 0
fi
[ -n "${SWIFT_TOOLCHAIN:-}" ] && export PATH="$SWIFT_TOOLCHAIN:$PATH"
command -v swift >/dev/null 2>&1 || { printf 'FAIL: %-24s нет swift в PATH (SWIFT_TOOLCHAIN)\n' "$LABEL"; exit 1; }

LOGDIR="${STIT_LOGDIR:-build/gate-logs}"
mkdir -p "$LOGDIR"
LOG="$LOGDIR/coverage_swift.log"
start=$(date +%s)
swift test --package-path "$PKG" --enable-code-coverage >"$LOG" 2>&1 \
    || { printf 'FAIL: %-24s swift test упал (лог: %s)\n' "$LABEL" "$LOG"; exit 1; }
json=$(ls "$(swift build --package-path "$PKG" --show-bin-path)"/codecov/*.json 2>/dev/null | head -1)
[ -n "$json" ] || { printf 'FAIL: %-24s нет codecov/*.json после swift test\n' "$LABEL"; exit 1; }
percent=$(python3 -c 'import json, sys; print(round(json.load(open(sys.argv[1]))["data"][0]["totals"]["lines"]["percent"], 1))' "$json")
dur=$(( $(date +%s) - start ))
if awk -v p="$percent" -v f="$FLOOR" 'BEGIN { exit !(p + 0 >= f + 0) }'; then
    printf 'OK:   %-24s %3ds  %s%% ≥ %s%%\n' "$LABEL" "$dur" "$percent" "$FLOOR"
else
    printf 'FAIL: %-24s %3ds  %s%% < %s%% (%s)\n' "$LABEL" "$dur" "$percent" "$FLOOR" "$PKG"
    exit 1
fi
