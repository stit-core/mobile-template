#!/usr/bin/env bash
# Сводка для /smartdev: ветка, дерево, платформы, Mac-хост, последний гейт, критичный детект, handoffs,
# модули, план, карта strategy. Секции `== Name ==` парсятся агентом, последняя строка — `OK` / `ATTENTION: <что>`.
# Без Gradle: отвечает за секунду; устойчива к отсутствию api/openapi.lock, docs/stack/service.md, ../strategy.
set -uo pipefail
cd "$(dirname "$0")/../.."
ATTN=""

# Поле `| <имя> | <значение> |` из docs/stack/service.md: значение в бэктиках — приоритет, курсивный
# плейсхолдер формы (`_…_`) — не заполнено; без файла — пусто.
stack_field() {
    local cell
    cell=$(grep -m1 -E "^\| *$1 *\|" docs/stack/service.md 2>/dev/null | awk -F'|' '{print $3}' | sed -E 's/^ +| +$//g')
    [ -z "$cell" ] && return
    [[ "$cell" =~ ^_.*_$ ]] && return
    if [[ "$cell" == *'`'* ]]; then
        echo "$cell" | sed -E 's/^[^`]*`([^`]*)`.*$/\1/'
    else
        echo "$cell"
    fi
}

echo "== Branch =="
echo "$(git symbolic-ref --short HEAD 2>/dev/null || echo DETACHED)"
echo "last: $(git log -1 --format='%h %s (%ar)' 2>/dev/null || echo '—')"
echo

echo "== Working tree =="
modified=$(git diff --name-only 2>/dev/null | wc -l | tr -d ' ')
staged=$(git diff --cached --name-only 2>/dev/null | wc -l | tr -d ' ')
untracked=$(git ls-files --others --exclude-standard 2>/dev/null | wc -l | tr -d ' ')
echo "modified: $modified, staged: $staged, untracked: $untracked"
git diff --name-only HEAD 2>/dev/null | head -20 | sed 's|^|  |'
echo

echo "== Platforms =="
platforms=$(stack_field platforms)
preset=$(stack_field preset)
if [ -n "$platforms" ]; then
    echo "$platforms · preset: ${preset:-—}"
else
    # Без service.md платформы = что есть в дереве (/init-service ещё не проходил).
    echo "android $([ -d androidApp ] && echo '✓' || echo '—')  ios $([ -d iosApp ] && echo '✓' || echo '— (iosApp/ отсутствует)')  · service.md: нет (/init-service)"
fi
echo "ios verified: $(cat build/gate-logs/mac.last 2>/dev/null || echo 'never (make check-mac)')"
echo

echo "== Mac =="
want_xcode=$(cat .xcode-version)
if [ -z "${MAC_HOST:-}" ]; then
    echo "MAC_HOST not set"
else
    have_xcode=$(ssh -o BatchMode=yes -o ConnectTimeout=3 "$MAC_HOST" xcodebuild -version 2>/dev/null | awk 'NR==1 {print $2}')
    if [ -z "$have_xcode" ]; then
        echo "$MAC_HOST: unreachable"
    elif [ "$have_xcode" = "$want_xcode" ]; then
        echo "$MAC_HOST: reachable · Xcode $have_xcode ✓"
    else
        echo "$MAC_HOST: reachable · Xcode $have_xcode ≠ .xcode-version $want_xcode"
    fi
fi
echo

echo "== Gate =="
if [ -f build/gate-logs/check.last ]; then
    echo "check: $(cat build/gate-logs/check.last)"
    grep -q ' OK ' build/gate-logs/check.last || ATTN="$ATTN gate-red"
else
    echo "check: не запускался (make check)"
    ATTN="$ATTN gate-unknown"
fi
echo "check-mac: $(cat build/gate-logs/mac.last 2>/dev/null || echo never)"
echo

# Дёшево, по файлам и хэшам: дрейф спеки, дампы фасада, klib, stack-черновики, устаревшие скриншоты.
echo "== Critical =="
backend=$(stack_field backend-path)
backend="${backend:-../backend}"
if [ ! -f api/openapi.yaml ]; then
    echo "GEN: api/openapi.yaml отсутствует"
elif [ ! -f api/openapi.lock ]; then
    echo "GEN: openapi.lock отсутствует → make gen"
    ATTN="$ATTN gen-drift"
else
    spec_sha=$(sha256sum api/openapi.yaml | cut -c1-64)
    if grep -q "$spec_sha" api/openapi.lock; then gen="openapi.lock ok"; else gen="openapi.lock устарел → make gen-check"; ATTN="$ATTN gen-drift"; fi
    if [ -f "$backend/api/openapi.yaml" ] && ! cmp -s api/openapi.yaml "$backend/api/openapi.yaml"; then
        gen="$gen · backend drift: $backend/api/openapi.yaml differs → make gen"
    fi
    echo "GEN: $gen"
fi
if git diff --quiet -- shared/api 2>/dev/null; then
    echo "API-DUMP: ok"
else
    echo "API-DUMP: дампы изменены и не закоммичены (изменение фасада — решение в плане)"
    ATTN="$ATTN api-dump"
fi
klib="shared/build/classes/kotlin/iosArm64/main/klib"
[ -d "$klib" ] && [ -n "$(ls -A "$klib" 2>/dev/null)" ] && echo "KLIB: iosArm64 ok" || echo "KLIB: не собран (make kmp-check)"
sdrafts=$(grep -l '^status: draft' docs/stack/*.md 2>/dev/null | grep -vE '/README\.md$' | sed 's#docs/stack/##; s#\.md$##' | tr '\n' ' ')
[ -n "$sdrafts" ] && echo "STACK-DRAFTS: $sdrafts(/stack-deep-dive)" || echo "STACK-DRAFTS: none"
stale=$(find androidApp iosApp -path '*/build' -prune -o -name '*_compare.png' -print 2>/dev/null | wc -l | tr -d ' ')
if [ "$stale" -gt 0 ]; then echo "SCREENSHOTS: $stale stale (*_compare.png)"; ATTN="$ATTN screenshots"; else echo "SCREENSHOTS: ok"; fi
echo

# Незакоммиченный diff по поверхности соседей → /infra-handoff; бэкенд — по спеке соседа.
echo "== Handoffs =="
INFRA_RE='^docs/stack/(deep-links|push|config|distribution|signing|observability)\.md$|^api/openapi\.lock$|^shared/.*/core/(api|requirements)/|^androidApp/app/src/main/AndroidManifest\.xml$|^iosApp/.*\.entitlements$'
infra_hits=$(git status --porcelain 2>/dev/null | awk '{print $2}' | grep -E "$INFRA_RE" || true)
if [ -n "$infra_hits" ]; then
    echo "INFRA pending: $(echo "$infra_hits" | tr '\n' ' ')→ /infra-handoff"
    ATTN="$ATTN handoff"
else
    echo "INFRA: none"
fi
if [ -f "$backend/api/openapi.yaml" ]; then
    missing=""
    grep -q '/api/auth/refresh' "$backend/api/openapi.yaml" || missing="$missing B-1…B-3(refresh)"
    grep -q '/api/app/requirements' "$backend/api/openapi.yaml" || missing="$missing B-17(requirements)"
    grep -q '/api/v1/devices' "$backend/api/openapi.yaml" || missing="$missing B-18(devices)"
    [ -n "$missing" ] && echo "BACKEND pending:$missing → /infra-handoff" || echo "BACKEND: none"
else
    echo "BACKEND: $backend/api/openapi.yaml не найден (reference-спека шаблона)"
fi
echo

echo "== Modules =="
sed -n 's/^include("\(.*\)")$/  \1/p' settings.gradle.kts
echo

echo "== Plan =="
if [ -f .claude/plans/roadmap.md ]; then
    focus=$(grep -m1 '^\*\*Current focus:\*\*' .claude/plans/roadmap.md | sed -E 's/\*\*Current focus:\*\*[[:space:]]*//')
    [ -n "$focus" ] && echo "  focus: $focus"
    awk '
      function flush(){ if(slug!="") printf "  %s: %s | next: %s\n", slug, st, (nx==""?"—":nx) }
      /^## Active/ { inact=1; next }
      /^## /       { flush(); inact=0; slug=""; next }
      !inact       { next }
      /^### /      { flush(); slug=$0; sub(/^### /,"",slug); sub(/ .*/,"",slug); st="?"; nx=""; next }
      slug=="" { next }
      /\*\*Статус:\*\*/ { s=$0; sub(/.*\*\*Статус:\*\*[[:space:]]*/,"",s); sub(/[[:space:]].*$/,"",s); st=s }
      /^- \[ \] \*\*A[0-9]/ && nx=="" { n=$0; sub(/^- \[ \] \*\*/,"",n); sub(/:.*/,"",n); nx=n }
      END { flush() }
    ' .claude/plans/roadmap.md
else
    echo "  (no roadmap)"
fi
echo

# Карта mobile/<surface>/ соседа: kinds и зрелость (последняя колонка таблиц — status).
echo "== Strategy =="
strategy=$(stack_field strategy-path)
strategy="${strategy:-../strategy}"
slug=$(stack_field project-slug)
surface=$(stack_field surface)
map="$strategy/docs/projects/$slug/mobile/$surface"
if [ ! -d "$strategy" ]; then
    echo "$strategy: not found"
elif [ -z "$slug" ] || [ -z "$surface" ]; then
    echo "$strategy: project-slug/surface не заданы в docs/stack/service.md (/init-service)"
elif [ ! -d "$map" ]; then
    echo "$strategy: карта mobile/$surface/ не найдена (/mobile-deep-dive $slug $surface там)"
else
    kinds=$(ls "$map"/*.md 2>/dev/null | wc -l | tr -d ' ')
    stable=$(grep -hE '^\|.*\| *`stable` *\|$' "$map"/*.md 2>/dev/null | wc -l | tr -d ' ')
    draft=$(grep -hE '^\|.*\| *`draft` *\|$' "$map"/*.md 2>/dev/null | wc -l | tr -d ' ')
    echo "$strategy: mobile/$surface/ — $kinds/12 kinds, $stable stable, $draft draft"
fi
echo

echo "== Recent commits =="
git log --oneline -5 2>/dev/null | sed 's|^|  |'
echo

echo "== TODOs in changed files =="
changed=$(git diff --name-only HEAD 2>/dev/null | grep -E '\.(kt|kts|swift|sh)$' || true)
if [ -n "$changed" ]; then
    # shellcheck disable=SC2086
    hits=$(grep -nE '(TODO|FIXME|XXX|HACK):' $changed 2>/dev/null | head -10 || true)
    if [ -n "$hits" ]; then echo "$hits" | sed 's|^|  |'; ATTN="$ATTN todos"; else echo "  (none)"; fi
else
    echo "  (no changed source files)"
fi
echo

[ -n "$ATTN" ] && echo "ATTENTION:$ATTN" || echo "OK"
