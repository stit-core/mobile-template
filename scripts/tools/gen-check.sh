#!/usr/bin/env bash
# Дрейф контракта (07 §2.5): lock ↔ спека, lock ↔ сгенерированные модели (правка руками — красный), при наличии
# генератора — регенерация во временный каталог и diff; спека соседа ../backend отличается — WARN, не FAIL.
#   gen-check.sh   → OK:/FAIL: gen-check; лог — build/gate-logs/gen-check.log
set -uo pipefail
cd "$(dirname "$0")/../.."
LABEL="gen-check"
SPEC=api/openapi.yaml
LOCK=api/openapi.lock
TARGET=shared/src/commonMain/kotlin/tech/stit/mobile/core/api/generated
BACKEND_DIR="${BACKEND_DIR:-../backend}"
LOGDIR="${STIT_LOGDIR:-build/gate-logs}"
mkdir -p "$LOGDIR"
LOG="$LOGDIR/gen-check.log"
: >"$LOG"
start=$(date +%s)

fail() {
    printf 'FAIL: %-24s %3ds  %s (лог: %s)\n' "$LABEL" "$(( $(date +%s) - start ))" "$1" "$LOG"
    exit 1
}
lock_value() { sed -n "s/^$1=//p" "$LOCK"; }
tree_sha() { (cd "$1" && find . -name '*.kt' | LC_ALL=C sort | xargs sha256sum | sha256sum | cut -d' ' -f1); }

[ -f "$LOCK" ] || fail "нет $LOCK — make gen"
[ -d "$TARGET" ] || fail "нет $TARGET — make gen"

spec_sha=$(sha256sum "$SPEC" | cut -d' ' -f1)
[ "$spec_sha" = "$(lock_value spec_sha256)" ] || fail "спека изменилась без регенерации — make gen"
[ "$(tree_sha "$TARGET")" = "$(lock_value models_sha256)" ] || fail "модели в $TARGET не совпадают с lock: правка руками? — make gen"

# Регенерация — только при доступном генераторе; без него lock уже доказал целостность дерева.
if [ -n "${OPENAPI_GENERATOR_JAR:-}" ]; then
    tmp=$(mktemp -d)
    trap 'rm -rf "$tmp"' EXIT
    bash scripts/tools/gen.sh --out "$tmp/generated" >>"$LOG" 2>&1 || fail "генератор упал"
    diff -r "$tmp/generated" "$TARGET" >>"$LOG" 2>&1 || fail "регенерация отличается от закоммиченного — make gen"
    [ "$(sed -n 's/^generator=//p' "$tmp/generated.lock")" = "$(lock_value generator)" ] || fail "версия генератора не совпадает с lock — make gen"
    note="регенерация совпала"
else
    note="без регенерации: OPENAPI_GENERATOR_JAR не задан"
fi

if [ -f "$BACKEND_DIR/api/openapi.yaml" ] && [ "$(sha256sum "$BACKEND_DIR/api/openapi.yaml" | cut -d' ' -f1)" != "$spec_sha" ]; then
    printf 'WARN: %-24s спека соседа %s отличается от копии — make gen\n' "$LABEL" "$BACKEND_DIR/api/openapi.yaml"
fi
printf 'OK:   %-24s %3ds  %s\n' "$LABEL" "$(( $(date +%s) - start ))" "$note"
