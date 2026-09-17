#!/usr/bin/env bash
# Ссылки в документации и обвязке резолвятся в существующие файлы: относительные `[…](path)` и `[…](path#anchor)`
# в docs/**, README.md, CLAUDE.md, AGENTS.md, .claude/**. Файлы — по git (gitignored сборки и скиллы не в счёт);
# fenced-блоки и код-спаны пропускаются; внешние (http/mailto) и якоря внутри файла не проверяются.
# Одна строка OK:/FAIL: в стиле gradle-step.sh; при FAIL — список «файл: ссылка».
#   links-check.sh            — весь набор;  links-check.sh <файл...> — только указанные
set -uo pipefail
cd "$(dirname "$0")/../.."
LABEL="links-check"

if [ $# -gt 0 ]; then
    files=("$@")
else
    mapfile -t files < <(git ls-files --cached --others --exclude-standard -- 'docs/**/*.md' 'docs/*.md' \
        README.md CLAUDE.md AGENTS.md '.claude/**/*.md' | sort -u)
fi

# Текст без fenced-блоков и код-спанов: примеры вида `[…](path)` — не ссылки.
prose() {
    awk '/^[[:space:]]*(```|~~~)/ { fence = !fence; next } !fence { print }' "$1" | sed -E 's/`[^`]*`//g'
}

start=$(date +%s)
total=0
broken=()
for file in "${files[@]}"; do
    dir=$(dirname "$file")
    while IFS= read -r target; do
        [ -z "$target" ] && continue
        case "$target" in
            http://* | https://* | mailto:* | '#'*) continue ;;
        esac
        path="${target%%#*}"
        [ -z "$path" ] && continue
        total=$((total + 1))
        [ -e "$dir/$path" ] || broken+=("$file: $target")
    done < <(prose "$file" | grep -oE '\]\([^)]+\)' | sed -E 's/^\]\((<)?//; s/(>)?\)$//; s/ "[^"]*"$//')
done
dur=$(( $(date +%s) - start ))

if [ ${#broken[@]} -eq 0 ]; then
    printf 'OK:   %-24s %3ds  ссылок: %d, файлов: %d\n' "$LABEL" "$dur" "$total" "${#files[@]}"
    exit 0
fi
printf 'FAIL: %-24s %3ds  битых ссылок: %d из %d\n' "$LABEL" "$dur" "${#broken[@]}" "$total"
printf '  %s\n' "${broken[@]}"
exit 1
