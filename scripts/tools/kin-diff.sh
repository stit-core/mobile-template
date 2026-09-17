#!/usr/bin/env bash
# Advisory-сверка «идентичных» файлов с эталоном экосистемы (docs/reference/kinship.md § Идентичные файлы):
# форк вправе разойтись, но должен знать. Эталон — sibling-checkout go-backend-template (KIN_DIR, дефолт
# ../go-backend-template) либо `git archive --remote` (KIN_REMOTE). WARN при расхождении, SKIP без эталона; код всегда 0.
set -uo pipefail
cd "$(dirname "$0")/../.."
LABEL="kin-diff"
KINSHIP="docs/reference/kinship.md"
KIN_DIR="${KIN_DIR:-../go-backend-template}"
KIN_REMOTE="${KIN_REMOTE:-git@git.stit.tech:stit-core/go-backend-template.git}"

# Строки таблицы вида `| <файл> | go-backend-template@main:<путь> |` — единственный источник списка.
mapfile -t rows < <(grep -E '^\| *`[^`]+` *\| *`go-backend-template@[^:]+:[^`]+` *\|' "$KINSHIP" |
    sed -E 's/^\| *`([^`]+)` *\| *`go-backend-template@([^:]+):([^`]+)` *\|.*/\1 \2 \3/')
[ ${#rows[@]} -gt 0 ] || { printf 'FAIL: %-24s в %s нет таблицы идентичных файлов\n' "$LABEL" "$KINSHIP"; exit 0; }

fetch_ref() {
    local ref="$1" path="$2"
    if [ -d "$KIN_DIR" ]; then
        cat "$KIN_DIR/$path" 2>/dev/null
    else
        git archive --remote="$KIN_REMOTE" "$ref" "$path" 2>/dev/null | tar -xO 2>/dev/null
    fi
}

start=$(date +%s)
warn=0
same=0
skip=0
for row in "${rows[@]}"; do
    read -r local ref path <<<"$row"
    if [ ! -f "$local" ]; then
        printf 'WARN: %-24s локального файла нет: %s\n' "$LABEL" "$local"
        warn=$((warn + 1))
        continue
    fi
    expected=$(fetch_ref "$ref" "$path")
    if [ -z "$expected" ]; then
        printf 'SKIP: %-24s эталон недоступен: go-backend-template@%s:%s (KIN_DIR=%s)\n' "$LABEL" "$ref" "$path" "$KIN_DIR"
        skip=$((skip + 1))
        continue
    fi
    if diff -q <(printf '%s\n' "$expected") "$local" >/dev/null; then
        same=$((same + 1))
    else
        printf 'WARN: %-24s %s разошёлся с go-backend-template@%s:%s\n' "$LABEL" "$local" "$ref" "$path"
        warn=$((warn + 1))
    fi
done
dur=$(( $(date +%s) - start ))
printf '%s %-24s %3ds  идентичных: %d, расхождений: %d, недоступно: %d\n' \
    "$([ "$warn" -eq 0 ] && echo 'OK:  ' || echo 'WARN:')" "$LABEL" "$dur" "$same" "$warn" "$skip"
exit 0
