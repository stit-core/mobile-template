#!/usr/bin/env bash
# Один Swift-шаг гейта — те же OK:/FAIL: и логи, что у gradle-step.sh. Тулчейн — SWIFT_TOOLCHAIN или PATH.
# Пока iosApp/ нет, честно печатает SKIP (код 0): Linux-гейт не изображает зелёный и не ломается без iOS-части.
#   swift-step.sh <ярлык> <команда...>
set -uo pipefail
LABEL="$1"; shift
cd "$(dirname "$0")/../.."

if [ ! -d iosApp ]; then
    printf 'SKIP: %-24s iosApp/ отсутствует\n' "$LABEL"
    exit 0
fi
[ -n "${SWIFT_TOOLCHAIN:-}" ] && export PATH="$SWIFT_TOOLCHAIN:$PATH"
if ! command -v "$1" >/dev/null 2>&1; then
    printf 'FAIL: %-24s нет %s в PATH — задай SWIFT_TOOLCHAIN=<toolchain>/usr/bin (make doctor)\n' "$LABEL" "$1"
    exit 1
fi

LOGDIR="${STIT_LOGDIR:-build/gate-logs}"
mkdir -p "$LOGDIR"
LOG="$LOGDIR/${LABEL// /_}.log"
LINES=25
[ "${FORMAT:-}" = "detailed" ] && LINES=120

start=$(date +%s)
if [ -n "${VERBOSE:-}" ]; then
    "$@" 2>&1 | tee "$LOG"
    rc=${PIPESTATUS[0]}
else
    "$@" >"$LOG" 2>&1
    rc=$?
fi
dur=$(( $(date +%s) - start ))

if [ "$rc" -eq 0 ]; then
    printf 'OK:   %-24s %3ds\n' "$LABEL" "$dur"
else
    printf 'FAIL: %-24s %3ds  (лог: %s)\n' "$LABEL" "$dur" "$LOG"
    grep -E ':[0-9]+:[0-9]+: (error|warning)|error:|FAIL|✘|Test Suite .* failed' "$LOG" | head -"$LINES"
fi
exit "$rc"
