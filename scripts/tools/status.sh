#!/usr/bin/env bash
# Сводка для /smartdev: ветка, дерево, последний гейт, модули, план. Секции `== Name ==` парсятся агентом,
# последняя строка — `OK` / `ATTENTION: <что>`. Без Gradle: отвечает за секунду.
set -uo pipefail
cd "$(dirname "$0")/../.."
ATTN=""

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
