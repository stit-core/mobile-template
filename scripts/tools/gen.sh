#!/usr/bin/env bash
# Модели API из спеки (07 §2.5): openapi-generator `kotlin` multiplatform, только models — клиент пишется
# руками над ApiClient. Источник — ../backend/api/openapi.yaml (sibling-checkout), иначе закоммиченная копия
# api/openapi.yaml. Дефекты дампа huma (07 §2.2: B-13 `$schema`, B-14 `[array, null]`, B-15 `ErrorModel.code`)
# снимаются на временной копии спеки — ни спека, ни сгенерированное руками не правятся.
#   gen.sh              обновить api/openapi.yaml (если сосед есть), core/api/generated/, api/openapi.lock
#   gen.sh --out <dir>  сгенерировать в <dir> и записать <dir>.lock, дерево не трогать (так работает gen-check.sh)
#   BACKEND_DIR (../backend), OPENAPI_GENERATOR_JAR (путь к jar) — из окружения
set -euo pipefail
cd "$(dirname "$0")/../.."

SPEC=api/openapi.yaml
LOCK=api/openapi.lock
CONFIG=config/openapi/kotlin-models.yaml
TARGET=shared/src/commonMain/kotlin/tech/stit/mobile/core/api/generated
BACKEND_DIR="${BACKEND_DIR:-../backend}"
OUT="$TARGET"
if [ "${1:-}" = "--out" ]; then
    OUT="${2:?каталог}"
    LOCK="$OUT.lock"
fi

JAR="${OPENAPI_GENERATOR_JAR:-}"
if [ ! -f "$JAR" ]; then
    echo "FAIL: gen — OPENAPI_GENERATOR_JAR не задан или файла нет: export OPENAPI_GENERATOR_JAR=<путь>/openapi-generator-cli-7.25.0.jar"
    exit 2
fi
if ! python3 -c 'import yaml' 2>/dev/null; then
    echo "FAIL: gen — нужен python3 с PyYAML (пакет python3-yaml) для подготовки спеки"
    exit 2
fi

# 1. Копия спеки соседа — только при записи в дерево; без соседа шаблон живёт на своей копии.
backend_commit=$(sed -n 's/^backend_commit=//p' api/openapi.lock 2>/dev/null || true)
if [ "$OUT" = "$TARGET" ] && [ -f "$BACKEND_DIR/api/openapi.yaml" ]; then
    cp "$BACKEND_DIR/api/openapi.yaml" "$SPEC"
    backend_commit=$(git -C "$BACKEND_DIR" rev-parse --short HEAD 2>/dev/null || echo unknown)
fi

tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT

# 2. Подготовка копии спеки. Строковые default тоже снимаются: генератор 7.25 печатает их как String.create(...).
python3 - "$SPEC" >"$tmp/openapi.yaml" <<'PY'
import sys, yaml

spec = yaml.safe_load(open(sys.argv[1]))
schemas = spec.get("components", {}).get("schemas", {})
for schema in schemas.values():
    props = schema.get("properties") or {}
    props.pop("$schema", None)
    for prop in props.values():
        if prop.get("type") == ["array", "null"]:
            prop["type"] = "array"
        prop.pop("default", None)
if "ErrorModel" in schemas:
    schemas["ErrorModel"].setdefault("properties", {}).setdefault("code", {"description": "Стабильный код ошибки httpkit.APIError", "type": "string"})
yaml.safe_dump(spec, sys.stdout, sort_keys=True, allow_unicode=True)
PY

# 3. Генерация во временный каталог и синхронизация: в дереве остаются только *.kt моделей.
if ! java -jar "$JAR" generate -i "$tmp/openapi.yaml" -c "$CONFIG" -o "$tmp/out" >"$tmp/gen.log" 2>&1; then
    tail -20 "$tmp/gen.log"
    echo "FAIL: gen — генератор упал"
    exit 1
fi
generated="$tmp/out/src/commonMain/kotlin/tech/stit/mobile/core/api/generated"
rm -rf "$OUT"
mkdir -p "$OUT"
cp "$generated"/*.kt "$OUT"/

# 4. Lock: по нему gen-check ловит правку моделей руками и устаревшую спеку без генератора.
models_sha=$(cd "$OUT" && find . -name '*.kt' | LC_ALL=C sort | xargs sha256sum | sha256sum | cut -d' ' -f1)
spec_sha=$(sha256sum "$SPEC" | cut -d' ' -f1)
generator="openapi-generator-cli $(java -jar "$JAR" version 2>/dev/null | tail -1)"
mkdir -p "$(dirname "$LOCK")"
printf 'backend_commit=%s\ngenerator=%s\nmodels_sha256=%s\nspec_sha256=%s\n' "${backend_commit:-reference}" "$generator" "$models_sha" "$spec_sha" >"$LOCK"
echo "OK:   gen — $(ls "$OUT" | wc -l) моделей → $OUT, lock: $LOCK"
