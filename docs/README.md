# Документация

Роутер по docs. Три аудитории + design source-of-truth.

## Потребителю шаблона
- [getting-started.md](getting-started.md) — путь: форк → `/init-service` → карта в `<org>/strategy` → `/stack-deep-dive` → `/de-template` → handoffs → первый `/release`. Android-first; Mac build-host — предусловие iOS-ветки. **[META]** — удаляется при де-темплейтизации.

## Контрибьютору шаблона
- [contributing.md](contributing.md) — устройство `.claude`-системы, кто что enforce'ит (Konsist / detekt / Lint / плагины / скрипты гейта / хуки / deny), как добавить модуль, фичу, kind, правило, стадию, фикстуру; обновление toolchain'ов; troubleshooting. **[META]**.

## Reference (переезжает в проект)
- [reference/architecture.md](reference/architecture.md) — `shared/` ↔ `androidApp/` ↔ `iosApp/`, матрица зависимостей и её enforcement, презентер, фасад для Swift, reference-фичи, гейт, ловушки.
- [reference/ecosystem.md](reference/ecosystem.md) — место в экосистеме (7 шаблонов + библиотеки), контракт artifact-in-release, соседи и sibling-checkout, триггер kit-библиотек.
- [reference/release.md](reference/release.md) — версия из тега, STOP-1/STOP-2, вербы, секреты и custody, STORE-HANDOFF.
- [reference/distribution.md](reference/distribution.md) — магазины, аккаунты, дедлайны, demo-режим как требование, чек-лист L-1…L-17, две подписи.
- [reference/observability.md](reference/observability.md) — Sentry/GlitchTip, DSN per env, PII, логи, символы, пороги, `TelemetrySink`.
- [reference/strategy-mapping.md](reference/strategy-mapping.md) — kinds карты `mobile/<surface>/` → пути в трёх местах кода; reference-соответствия.
- [reference/mac-host.md](reference/mac-host.md) — Mac build-host: SSH, `~/ci/<slug>/`, стадии `check-mac`, подпись, гигиена.
- [reference/kinship.md](reference/kinship.md) — родство с Go/SPA/bot-шаблонами; `make kin-diff`.

## Design source-of-truth
- **Карта приложения** — слой `mobile/<surface>/` (9 kinds: entities · screens · flows · features · shell · entry-points · permissions · shared-api · shared-lib) в соседнем `<org>/strategy` (`docs/projects/<slug>/mobile/<surface>/`, ведёт `/mobile-deep-dive` там). Здесь read-only; копия канона для strategy-форков без него — [reference/strategy-mobile-canon/README.md](reference/strategy-mobile-canon/README.md).
- [stack/README.md](stack/README.md) — артефакты платформы: 13 обязательных + 2 условных kinds, волны, cross-kind-инварианты, вход для `/de-template`. Остаётся в этом репо.

Точка входа агента в репо — `CLAUDE.md` (ядро + роутер). Дневной флоу — skill `/smartdev`.
