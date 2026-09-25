# scaffold › stack / rule / skill — элементы обвязки

Из шаблонов обвязки, без карты strategy. Устройство `.claude`-системы — `docs/contributing.md`.

## `stack <kind>`

Только для 16 известных kinds (14 обязательных + 2 условных, `docs/stack/README.md`):
`cp docs/stack/_templates/<kind>.md docs/stack/<kind>.md`, frontmatter из имени, дальше —
`/stack-deep-dive <kind>` (режим C). Новый kind вне канона — сначала решение в плане и форма в
`_templates/` + строка в `docs/stack/README.md` + чек-лист в
`.claude/skills/stack-deep-dive/references/field-checklists.md`.

## `rule <name>`

```
.claude/rules/<name>.md   # ≈ 60 строк
---
name: <name>
description: <одна строка для людей: о чём правило и при правке каких путей грузится>
paths:
  - "<glob>"
---
# <Заголовок>
Ссылка на docs/reference/* за «как и почему». Секции: Инварианты · Сигналы.
```

Из frontmatter правила Claude Code читает только `paths:` (не `globs:`) — по нему правило
грузится, когда агент читает подходящий файл; `name`/`description` — для людей и таблиц
`docs/contributing.md`. Правило без `paths:` грузится всегда, как `CLAUDE.md`, — не заводить.
Terse-инварианты, не эссе; пересечение с существующими 24 правилами — расширить существующее, не
плодить. Добавить строку в таблицу «правило → кто enforce'ит» `docs/contributing.md`.

## `skill <name>`

```
.claude/skills/<name>/SKILL.md        # frontmatter name + description: что делает и когда применять
.claude/skills/<name>/references/     # чек-листы, формы, варианты — SKILL.md говорит, когда их читать
.claude/skills/<name>/state/          # только если скилл держит рабочую память (коммитится, forward-only)
```

`description` (вместе с необязательным `when_to_use`) — routing-текст: ключевой сценарий первым,
категории намерений вместо списков фраз; в листинге скиллов оба поля обрезаются на 1536 символах.
`SKILL.md` — меньше 500 строк; детали уходят в `references/`, когда тело перерастает ~150 строк.
Форма — как у `/scaffold` (роутер + references) или `/release` (preflight → шаги →
anti-patterns). Скилл ссылается только на существующие вербы `Makefile` и пути дерева;
`make help` — источник вербов. Официальные Android-скиллы (`android skills add`) не вендорятся —
список в `scripts/tools/android-skills.sh` и `.gitignore` (парное место правки,
`docs/contributing.md`).

## Verify

`bash scripts/tools/links-check.sh` (ссылки резолвятся), `wc -l .claude/rules/<name>.md` ≈ 60,
frontmatter правила с `paths:`, `make check` не тронут.
