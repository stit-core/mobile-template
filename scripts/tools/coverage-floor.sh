#!/usr/bin/env bash
# Пол покрытия Swift-пакета: swift test --enable-code-coverage, затем llvm-cov export по всем тест-бандлам пакета
# (codecov/<Package>.json от SwiftPM учитывает только первый — цифра была бы неполной), только Sources/ без тестов.
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
LLVM_COV="$(dirname "$(command -v swift)")/llvm-cov"
[ -x "$LLVM_COV" ] || LLVM_COV=$(command -v llvm-cov || true)
[ -n "$LLVM_COV" ] || { printf 'FAIL: %-24s нет llvm-cov рядом со swift\n' "$LABEL"; exit 1; }

LOGDIR="${STIT_LOGDIR:-build/gate-logs}"
mkdir -p "$LOGDIR"
LOG="$LOGDIR/coverage_swift.log"
start=$(date +%s)
swift test --package-path "$PKG" --enable-code-coverage >"$LOG" 2>&1 \
    || { printf 'FAIL: %-24s swift test упал (лог: %s)\n' "$LABEL" "$LOG"; exit 1; }
bin=$(swift build --package-path "$PKG" --show-bin-path)
profile="$bin/codecov/default.profdata"
[ -f "$profile" ] || { printf 'FAIL: %-24s нет codecov/default.profdata после swift test\n' "$LABEL"; exit 1; }
mapfile -t bundles < <(ls "$bin"/*Tests.so 2>/dev/null)
[ ${#bundles[@]} -gt 0 ] || { printf 'FAIL: %-24s нет тест-бандлов *Tests.so в %s\n' "$LABEL" "$bin"; exit 1; }
objects=()
for bundle in "${bundles[@]:1}"; do objects+=(-object "$bundle"); done
percent=$("$LLVM_COV" export -instr-profile "$profile" "${bundles[0]}" "${objects[@]}" \
    -ignore-filename-regex='/Tests/|/\.build/' 2>>"$LOG" \
    | python3 -c 'import json, sys; print(round(json.load(sys.stdin)["data"][0]["totals"]["lines"]["percent"], 1))')
dur=$(( $(date +%s) - start ))
if awk -v p="$percent" -v f="$FLOOR" 'BEGIN { exit !(p + 0 >= f + 0) }'; then
    printf 'OK:   %-24s %3ds  %s%% ≥ %s%%\n' "$LABEL" "$dur" "$percent" "$FLOOR"
else
    printf 'FAIL: %-24s %3ds  %s%% < %s%% (%s)\n' "$LABEL" "$dur" "$percent" "$FLOOR" "$PKG"
    exit 1
fi
