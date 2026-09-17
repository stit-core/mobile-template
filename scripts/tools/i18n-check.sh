#!/usr/bin/env bash
# i18n (08 §4.2): у каждого модуля androidApp набор ключей values/strings.xml == values-ru/strings.xml; коды ошибок shared
# (MessageKey.*, KnownErrorCodes) имеют строку core_ui_error_<code>; iOS Localizable.xcstrings несёт те же error.* (если файл есть).
#   i18n-check.sh → OK:/FAIL: i18n-check (+ SKIP-строка iOS, пока Localizable.xcstrings нет)
set -uo pipefail
cd "$(dirname "$0")/../.."
LABEL="i18n-check"
LOGDIR="${STIT_LOGDIR:-build/gate-logs}"
mkdir -p "$LOGDIR"
LOG="$LOGDIR/i18n_check.log"
: >"$LOG"
start=$(date +%s)
fails=0
note() { echo "$*" >>"$LOG"; fails=$((fails + 1)); }
keys() { grep -oE '<string name="[^"]+"' "$1" | sed 's/<string name="//; s/"$//' | LC_ALL=C sort; }

while IFS= read -r en; do
    ru="${en%/values/strings.xml}/values-ru/strings.xml"
    if [ ! -f "$ru" ]; then
        note "нет перевода: $ru"
    elif ! diff <(keys "$en") <(keys "$ru") >>"$LOG" 2>&1; then
        note "ключи расходятся: $en ↔ $ru"
    fi
done < <(find androidApp -path '*/src/main/res/values/strings.xml' -not -path '*/build/*' | LC_ALL=C sort)

# Реестр ключей ошибок shared → ресурсы core:ui (07 §2.6: error.<code> — единственный ключ локализации).
registry=shared/src/commonMain/kotlin/tech/stit/mobile/core/i18n/MessageKey.kt
ui_strings=androidApp/core/ui/src/main/res/values/strings.xml
codes=$(
    {
        grep -oE 'const val [A-Z_]+ = "[A-Z_]+"' "$registry" | sed 's/.*= "//; s/"//'
        grep -oE 'MessageKey\("error\.[a-z_]+"\)' "$registry" | sed 's/.*"error\.//; s/".*//'
    } | tr '[:upper:]' '[:lower:]' | LC_ALL=C sort -u
)
for code in $codes; do
    grep -q "name=\"core_ui_error_$code\"" "$ui_strings" || note "нет строки core_ui_error_$code для error.$code ($ui_strings)"
done

# Паритет с iOS — advisory: ключ error.<code> ищется по всем Localizable.xcstrings (каталоги строк живут в пакетах).
xcstrings=$(find iosApp -name 'Localizable.xcstrings' -not -path '*/.build/*' 2>/dev/null | LC_ALL=C sort)
if [ -z "$xcstrings" ]; then
    printf 'SKIP: %-24s iOS Localizable.xcstrings отсутствует\n' "$LABEL (ios)"
else
    missing=$(for code in $codes; do grep -lq "\"error\.$code\"" $xcstrings || echo "error.$code"; done | tr '\n' ' ')
    [ -z "$missing" ] || printf 'WARN: %-24s в iosApp/**/Localizable.xcstrings нет ключей: %s\n' "$LABEL (ios)" "$missing"
fi

dur=$(( $(date +%s) - start ))
if [ "$fails" -gt 0 ]; then
    printf 'FAIL: %-24s %3ds  (лог: %s)\n' "$LABEL" "$dur" "$LOG"
    grep -E '^(нет|ключи)' "$LOG" | head -5
    exit 1
fi
printf 'OK:   %-24s %3ds\n' "$LABEL" "$dur"
