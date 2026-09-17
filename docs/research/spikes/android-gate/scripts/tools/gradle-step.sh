#!/usr/bin/env bash
# Один шаг гейта: гоняет Gradle-таск, печатает одну строку OK:/FAIL:.
# При падении — только релевантный кусок лога (а не 400 строк Gradle-шума).
# Использование: gradle-step.sh <ярлык> <таск...> [-- <доп. флаги>]
set -uo pipefail
LABEL="$1"; shift
LOGDIR="${STIT_LOGDIR:-build/gate-logs}"
mkdir -p "$LOGDIR"
LOG="$LOGDIR/${LABEL// /_}.log"
start=$(date +%s)
./gradlew "$@" --console=plain --warning-mode=none > "$LOG" 2>&1
rc=$?
dur=$(( $(date +%s) - start ))
if [ $rc -eq 0 ]; then
  printf 'OK:   %-22s %3ds\n' "$LABEL" "$dur"
else
  printf 'FAIL: %-22s %3ds  (лог: %s)\n' "$LABEL" "$dur" "$LOG"
  # Сухая выжимка: строки инструментов, а не Gradle-обвязка
  grep -E '^(e:|w: .*error)|^\S+\.(kt|kts|xml|java):[0-9]+|Analysis failed|There were failing tests|format violations|Error:|FAILURE:' "$LOG" \
    | grep -vE 'Run with --|Get more help|^\* Try|Deprecated Gradle' | head -25
fi
exit $rc
