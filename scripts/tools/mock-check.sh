#!/usr/bin/env bash
# Фикстуры mock/ (07 §3): каталоги = сценарии из mock/scenarios.yaml, каждый JSON валиден, и FixturesTest
# (строгий разбор сгенерированными моделями) зелёный — лишнее поле в фикстуре красит гейт.
#   mock-check.sh   → OK:/FAIL: mock fixtures + стадия Gradle «mock fixtures test»
set -uo pipefail
cd "$(dirname "$0")/../.."
LABEL="mock fixtures"
LOGDIR="${STIT_LOGDIR:-build/gate-logs}"
mkdir -p "$LOGDIR"
LOG="$LOGDIR/mock_fixtures.log"
: >"$LOG"
start=$(date +%s)
fails=0
note() { echo "$*" >>"$LOG"; fails=$((fails + 1)); }

declared=$(sed -n 's/^  \([a-z_]*\):$/\1/p' mock/scenarios.yaml | LC_ALL=C sort)
present=$(find mock/fixtures -mindepth 1 -maxdepth 1 -type d -printf '%f\n' | LC_ALL=C sort)
[ "$declared" = "$present" ] || note "сценарии в scenarios.yaml ($(echo "$declared" | tr '\n' ' ')) ≠ каталогам mock/fixtures ($(echo "$present" | tr '\n' ' '))"

for scenario in $declared; do
    expected=$(sed -n "/^  $scenario:/,/^  [a-z_]*:/s/^    entities: \[\(.*\)\]/\1/p" mock/scenarios.yaml | tr -d ' ' | tr ',' '\n' | LC_ALL=C sort)
    actual=$(find "mock/fixtures/$scenario" -maxdepth 1 -name '*.json' -printf '%f\n' 2>/dev/null | sed 's/\.json$//' | LC_ALL=C sort)
    [ "$expected" = "$actual" ] || note "$scenario: entities в scenarios.yaml ($(echo "$expected" | tr '\n' ' ')) ≠ файлам ($(echo "$actual" | tr '\n' ' '))"
done

while IFS= read -r file; do
    python3 -m json.tool "$file" >/dev/null 2>>"$LOG" || note "невалидный JSON: $file"
done < <(find mock/fixtures -name '*.json')

dur=$(( $(date +%s) - start ))
if [ "$fails" -gt 0 ]; then
    printf 'FAIL: %-24s %3ds  (лог: %s)\n' "$LABEL" "$dur" "$LOG"
    head -5 "$LOG"
    exit 1
fi
printf 'OK:   %-24s %3ds\n' "$LABEL" "$dur"
bash scripts/tools/gradle-step.sh "mock fixtures test" :shared:jvmTest --tests 'tech.stit.mobile.core.mock.FixturesTest'
