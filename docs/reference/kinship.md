# Родство с шаблонами экосистемы

Обвязка `mobile-template` — та же форма, что у `go-backend-template`, `spa-frontend-template`,
`telegram-bot-template`: `CLAUDE.md` = ядро + роутер, path-scoped правила, `settings.json`,
девять скиллов, два агента, планы `.claude/plans/`, гейт `make check`. Три степени родства:
**идентично** — файл побайтно общий с экосистемой; **по форме** — та же структура/секции, своё
содержание; **не переезжает** — экосистемный механизм, которого у мобильного репо нет.

| идентично | по форме | не переезжает |
|---|---|---|
| `.claude/rules/forward-only.md` | `CLAUDE.md` (секции: ядро/роутер, «Где это в экосистеме», Точка входа, Проектирование первично, Команды, Карта, Инварианты, Git workflow, Границы, Forward-only, Авто-память) | Dockerfile / nginx / compose-сервис / `<SVC>_TAG` / `make bump` |
| `LICENSE` | `.claude/rules/comments.md` (примеры — Kotlin/Swift) | STOP = image-in-registry (заменён на artifact-in-release) |
| `scripts/tools/validate-plan.sh` | `/plan`: профиль `references/repo.md` (шаг «Mac-verify», мобильные проверки) | MSW / lefthook / pnpm / Steiger / Biome / Vitest (`spa:`) |
| `/plan`: `SKILL.md`, `references/plan-template.md`; формат `.claude/plans/` (`<slug>.md` + `backlog.md`) | `Makefile` (группы `##@`, `OK:`/`FAIL:`, `check`, `status`, `doctor`, `setup`) | sqlc / mockery / fx / golangci (`go:`) |
| формат Conventional Commits (`scripts/git-hooks/commit-msg`; типы + `perf`, `style`) | `scripts/tools/{check,status,doctor}.sh` (секции, финал `OK`/`ATTENTION:`) | aiogram / tgbotkit / dialog-architecture (`bot:`) |
| deny-ядро `settings.json` (`--no-verify`, force-push, `reset --hard`, ключи, memory) | `/smartdev`, `/triage-failure`, `/init-service`, `/stack-deep-dive`, `/de-template`, `/scaffold`, `/release`, `/infra-handoff` | `docs/reference/deployment.md` |
| формат блока handoff (WHAT/WHY/ACTION/WHO/DECIDE/RISK) | `.claude/rules/{tests-pipeline,env-and-config,release-flow,infra-impact,android-ui,ios-ui}.md` | FSD-слой / `screens/` в strategy (заменён `mobile/`) |
| общие секции `.editorconfig` (`*.md`, `*.yaml`, `Makefile`) | `docs/{README,getting-started,contributing}.md`, `docs/reference/{architecture,ecosystem,release,observability}.md`, `SECURITY.md` | единый `COVER_FLOOR` (у мобильного три пола: 80 / 70 / 70) |
| | `docs/stack/README.md` + `_templates` (frontmatter, `status`, `## Generates`) | |
| | `.claude/agents/{explorer,reviewer}.md`, `.gate-fixtures/` + `gate-selftest` | |

## Идентичные файлы

Эталон — `go-backend-template` (ветка `main`); `make kin-diff` (`scripts/tools/kin-diff.sh`) сверяет
локальные файлы с эталоном и печатает `WARN` при расхождении — advisory: форк вправе разойтись,
но должен знать. Скрипт читает эту таблицу.

| файл | эталон |
|---|---|
| `.claude/rules/forward-only.md` | `go-backend-template@main:.claude/rules/forward-only.md` |
| `LICENSE` | `go-backend-template@main:LICENSE` |
| `scripts/tools/validate-plan.sh` | `go-backend-template@main:scripts/tools/validate-plan.sh` |
| `.claude/skills/plan/SKILL.md` | `go-backend-template@main:.claude/skills/plan/SKILL.md` |
| `.claude/skills/plan/references/plan-template.md` | `go-backend-template@main:.claude/skills/plan/references/plan-template.md` |

Источник эталона: sibling-checkout `../go-backend-template` (если есть) либо `git archive
--remote=<gitea>/stit-core/go-backend-template main <paths>`; переопределение — `KIN_DIR` /
`KIN_REMOTE`. Без доступа к эталону — `SKIP`, код 0.

## Что различается осознанно

- **STOP** — artifact-in-release вместо image-in-registry: у мобильного клиента нет образа, есть
  подписанные артефакты и магазины с модерацией (двухступенчатый STOP).
- **Гейт** — часть стадий выполнима только на macOS; гейт печатает явный SKIP-блок, а не «зелёный».
- **Три пола покрытия** — логика сосредоточена в `shared` (80 %), UI-слои держат скриншоты (70 %).
- **24 правила** вместо 8–10: две платформы × магазины × право; каждое path-scoped и короткое
  (≈ 60 строк).
- **Карта продукта** — слой `mobile/<surface>/` (12 kinds) в strategy вместо FSD/screens; копия
  канона — [`strategy-mobile-canon/README.md`](strategy-mobile-canon/README.md).
