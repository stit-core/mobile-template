#!/usr/bin/env bash
# Один Gradle-шаг гейта: одна строка OK:/FAIL:, полный лог — build/gate-logs/<шаг>.log.
# При падении печатается выжимка инструмента (e:, file:line, FAILED), а не сотни строк Gradle-шума.
#   gradle-step.sh <ярлык> <таск...>
#   FORMAT=detailed — выжимка длиннее; VERBOSE=1 — сырой вывод в терминал; STIT_LOGDIR — куда класть логи.
set -uo pipefail
LABEL="$1"; shift
cd "$(dirname "$0")/../.."

LOGDIR="${STIT_LOGDIR:-build/gate-logs}"
mkdir -p "$LOGDIR"
LOG="$LOGDIR/${LABEL// /_}.log"
LINES=25
[ "${FORMAT:-}" = "detailed" ] && LINES=120

start=$(date +%s)
if [ -n "${VERBOSE:-}" ]; then
    ./gradlew "$@" --console=plain --warning-mode=none 2>&1 | tee "$LOG"
    rc=${PIPESTATUS[0]}
else
    ./gradlew "$@" --console=plain --warning-mode=none >"$LOG" 2>&1
    rc=$?
fi
dur=$(( $(date +%s) - start ))

if [ "$rc" -eq 0 ]; then
    printf 'OK:   %-24s %3ds\n' "$LABEL" "$dur"
else
    printf 'FAIL: %-24s %3ds  (лог: %s)\n' "$LABEL" "$dur" "$LOG"
    grep -E '^(e:|w: .*error)|^\S+\.(kt|kts|xml|java):[0-9]+|FAILED$|Analysis failed|failing tests|violations|Error:|FAILURE:|^> [A-Z]|^\* What went wrong|Rule violated|does not match|not found' "$LOG" \
        | grep -vE 'Run with --|Get more help|^\* Try|Deprecated Gradle|^> Task' \
        | head -"$LINES"
fi
exit "$rc"
