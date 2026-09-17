#!/usr/bin/env bash
# Канонический гейт мобильного шаблона. Печатает по строке на шаг,
# в конце — сводку. Полные логи — build/gate-logs/*.log.
set -uo pipefail
cd "$(dirname "$0")/../.."
STEP=scripts/tools/gradle-step.sh
fails=0
run() { bash "$STEP" "$@" || fails=$((fails+1)); }

echo "→ gate: android-gate"
run "format"      spotlessCheck
run "detekt"      detekt
run "android lint" lint
run "unit tests"  test
run "screenshots" verifyRoborazziDebug
run "coverage"    koverVerify
run "assembleDebug" :app:assembleDebug
if [ "${GATE_RELEASE:-0}" = "1" ]; then
  run "assembleRelease" :app:assembleRelease
fi

if [ $fails -eq 0 ]; then
  echo "✓ check clean"
else
  echo "✗ check: провалено шагов — $fails"
fi
exit $fails
