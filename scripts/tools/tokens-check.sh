#!/usr/bin/env bash
# Паритет токенов (08 §4.2): Tokens.kt и Tokens.swift в дереве обязаны совпадать с регенерацией из design/tokens.json.
# Регенерация идёт во временный каталог и сравнивается diff'ом — проверка честна и на незакоммиченном дереве.
#   tokens-check.sh → OK:/FAIL: tokens-check
set -uo pipefail
cd "$(dirname "$0")/../.."
LABEL="tokens-check"
LOGDIR="${STIT_LOGDIR:-build/gate-logs}"
mkdir -p "$LOGDIR"
LOG="$LOGDIR/tokens_check.log"
start=$(date +%s)
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT

python3 scripts/tools/gen-tokens.py --out "$tmp" >"$LOG" 2>&1 || {
    printf 'FAIL: %-24s генератор упал (лог: %s)\n' "$LABEL" "$LOG"
    tail -3 "$LOG"
    exit 1
}
fails=0
while IFS= read -r generated; do
    relative="${generated#"$tmp"/}"
    if [ ! -f "$relative" ]; then
        echo "нет файла $relative — make gen-tokens" >>"$LOG"
        fails=$((fails + 1))
    elif ! diff -u "$relative" "$generated" >>"$LOG" 2>&1; then
        echo "$relative расходится с design/tokens.json — make gen-tokens" >>"$LOG"
        fails=$((fails + 1))
    fi
done < <(find "$tmp" -type f | LC_ALL=C sort)

dur=$(( $(date +%s) - start ))
if [ "$fails" -gt 0 ]; then
    printf 'FAIL: %-24s %3ds  (лог: %s)\n' "$LABEL" "$dur" "$LOG"
    grep -E 'расходится|нет файла' "$LOG" | head -5
    exit 1
fi
printf 'OK:   %-24s %3ds\n' "$LABEL" "$dur"
