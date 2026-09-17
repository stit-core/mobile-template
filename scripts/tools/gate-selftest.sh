#!/usr/bin/env bash
# Самотест гейта: каждая негативная фикстура .gate-fixtures/NF-*/fixture.sh обязана уронить свою стадию.
# Фикстура объявляет EXPECT (стадия check.sh), TOUCHES (пути, которые она меняет) и функцию apply.
# Откат — из резервной копии, а не через git: самотест работает и на незакоммиченном дереве.
#   gate-selftest.sh [NF-03]   — все фикстуры или одна
set -uo pipefail
cd "$(dirname "$0")/../.."
FILTER="${1:-}"
BACKUP=$(mktemp -d)
trap 'rm -rf "$BACKUP"' EXIT
red=0
green=0
broken=0

restore() {
    local id="$1" path
    for path in $TOUCHES; do
        rm -rf "$path"
        if [ -e "$BACKUP/$id/$path" ]; then
            mkdir -p "$(dirname "$path")"
            cp -a "$BACKUP/$id/$path" "$path"
        else
            rmdir -p --ignore-fail-on-non-empty "$(dirname "$path")" 2>/dev/null || true
        fi
    done
}

echo "→ gate-selftest: фикстуры обязаны краснеть"
for dir in .gate-fixtures/NF-*/; do
    id=$(basename "$dir")
    [ -n "$FILTER" ] && [ "$id" != "$FILTER" ] && continue
    unset EXPECT TOUCHES
    unset -f apply
    # shellcheck disable=SC1090
    source "$dir/fixture.sh"
    why=$(sed -n '1s/^# *//p' "$dir/fixture.sh")

    for path in $TOUCHES; do
        [ -e "$path" ] || continue
        mkdir -p "$BACKUP/$id/$(dirname "$path")"
        cp -a "$path" "$BACKUP/$id/$path"
    done

    start=$(date +%s)
    if ! (set -e; apply) >"$BACKUP/$id.apply.log" 2>&1; then
        printf 'ERR:  %-6s фикстура не применилась: %s\n' "$id" "$(tail -1 "$BACKUP/$id.apply.log")"
        broken=$((broken + 1))
        restore "$id"
        continue
    fi
    STIT_LOGDIR="build/gate-logs/selftest/$id" bash scripts/tools/check.sh --only "$EXPECT" >"$BACKUP/$id.stage.log" 2>&1
    rc=$?
    restore "$id"
    dur=$(( $(date +%s) - start ))
    if [ "$rc" -ne 0 ]; then
        printf 'RED:  %-6s %-20s %3ds  %s\n' "$id" "$EXPECT" "$dur" "$why"
        red=$((red + 1))
    else
        printf 'GREEN?: %-6s %-20s %3ds  гейт не покраснел — %s (лог: build/gate-logs/selftest/%s)\n' "$id" "$EXPECT" "$dur" "$why" "$id"
        green=$((green + 1))
    fi
done

total=$((red + green + broken))
if [ "$total" -eq 0 ]; then
    echo "✗ gate-selftest: фикстур не найдено (${FILTER:-.gate-fixtures/NF-*})"
    exit 1
elif [ "$green" -eq 0 ] && [ "$broken" -eq 0 ]; then
    echo "✓ gate-selftest: красных $red из $total"
else
    echo "✗ gate-selftest: не покраснело $green, не применилось $broken (из $total)"
fi
exit $((green + broken))
