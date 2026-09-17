#!/usr/bin/env bash
# Deep links (08 §4.2): intent-filter берёт хост/схему из config/env через плейсхолдеры, каждый env задаёт оба ключа,
# каждый путь intent-filter и каждая строка «Paths to Route» docs/stack/deep-links.md (если файл есть) покрыты DeepLinkParserTest.
#   deeplinks-check.sh → OK:/FAIL: deeplinks-check
set -uo pipefail
cd "$(dirname "$0")/../.."
LABEL="deeplinks-check"
LOGDIR="${STIT_LOGDIR:-build/gate-logs}"
mkdir -p "$LOGDIR"
LOG="$LOGDIR/deeplinks_check.log"
: >"$LOG"
start=$(date +%s)
fails=0
note() { echo "$*" >>"$LOG"; fails=$((fails + 1)); }
manifest=androidApp/app/src/main/AndroidManifest.xml
test=shared/src/commonTest/kotlin/tech/stit/mobile/core/deeplink/DeepLinkParserTest.kt

grep -q 'android:autoVerify="true"' "$manifest" || note "в $manifest нет intent-filter с android:autoVerify"
grep -q 'android:scheme="${DEEP_LINK_SCHEME}"' "$manifest" || note "custom scheme в манифесте не из config/env (ожидается \${DEEP_LINK_SCHEME})"
grep -oE 'android:host="[^"]+"' "$manifest" | grep -v '"${DEEP_LINK_HOST}"' | while IFS= read -r host; do
    note "хост в манифесте задан литералом: $host (нужен \${DEEP_LINK_HOST} из config/env)"
done
grep -q 'android:host="${DEEP_LINK_HOST}"' "$manifest" || note "в $manifest нет android:host=\"\${DEEP_LINK_HOST}\""
fails=$((fails + $(grep -c 'задан литералом' "$LOG")))

for env in config/env/*.properties; do
    for key in DEEP_LINK_HOST DEEP_LINK_SCHEME; do
        grep -qE "^$key=.+" "$env" || note "$env: пустой или отсутствующий $key"
    done
done

while IFS= read -r path; do
    grep -q "$path" "$test" || note "путь $path из манифеста без теста в $test"
done < <(grep -oE 'android:pathPrefix="[^"]+"' "$manifest" | sed 's/.*="//; s/"//' | LC_ALL=C sort -u)

if [ -f docs/stack/deep-links.md ]; then
    while IFS= read -r path; do
        grep -q "${path%%\{*}" "$test" || note "путь $path из docs/stack/deep-links.md без теста в $test"
    done < <(sed -n '/^## Paths to Route/,/^## /p' docs/stack/deep-links.md | grep -oE '^\| *`/[^`]+`' | sed 's/^| *`//; s/`$//')
fi

dur=$(( $(date +%s) - start ))
if [ "$fails" -gt 0 ]; then
    printf 'FAIL: %-24s %3ds  (лог: %s)\n' "$LABEL" "$dur" "$LOG"
    head -5 "$LOG"
    exit 1
fi
printf 'OK:   %-24s %3ds\n' "$LABEL" "$dur"
