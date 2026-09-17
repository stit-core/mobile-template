#!/usr/bin/env bash
# Официальные Android-скиллы для агента (08 §3.5): не вендорятся — ставятся закреплённым списком через
# `android skills add` в .claude/skills/<id>/ (эти каталоги в .gitignore); обновление — повторный make setup.
# Телеметрия CLI выключена через ~/.androidrc. Список id — `android skills list`.
set -uo pipefail
cd "$(dirname "$0")/../.."
SKILLS="android-cli agp-9-upgrade edge-to-edge navigation-3 navigation-event r8-analyzer testing-setup"

SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
[ -z "$SDK" ] && [ -f local.properties ] && SDK=$(sed -n 's/^sdk\.dir=//p' local.properties)
ANDROID="$SDK/cmdline-tools/latest/bin/android"
if [ ! -x "$ANDROID" ]; then
    echo "WARN : android CLI не найден ($ANDROID) — скиллы не установлены; нужен cmdline-tools ≥ 23"
    exit 0
fi
grep -qs -- '--no-metrics' ~/.androidrc || echo '--no-metrics' >>~/.androidrc

for skill in $SKILLS; do
    if "$ANDROID" --no-metrics skills add --agent=claude-code --project=. "$skill" >/dev/null 2>&1; then
        echo "OK   : skill $skill"
    else
        echo "WARN : skill $skill не установился — android --no-metrics skills add --agent=claude-code $skill"
    fi
done
