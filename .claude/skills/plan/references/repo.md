# Профиль репо для /plan — mobile-template

- **Роль:** сервис — мобильное приложение (`<org>/mobile`, Android + iOS). Работа приходит из
  корня проекта — задачей или фазой программы хаба `<org>/infra` (`parent: infra:<slug>`) — или
  прямым заходом. Итог — релиз в магазины своим `/release` (STOP-1 — Gitea Release, STOP-2 —
  подача в каналы); выкатка из хаба этот репо не касается.
- **Гейт перед «готово»:** `make check` (Linux-полный); «готово по iOS» — ещё `make check-mac`;
  перед `/release` — `make check-full`.
- **Проверка планов:** `make validate-plan`; pre-commit проверяет, когда меняется `.claude/plans/`.
- **Сводка планов:** `make status` → `== Plan ==`.
- **Типовые проверки шагов:** `./gradlew :shared:jvmTest --tests '*<Name>Test*'` → PASS ·
  `make check-fast` → зелёный · `make screenshots` → эталоны совпадают · `make api-check` → без
  дрейфа · `make gen-check` → без дрейфа · `make check` → зелёный · `make check-mac` → зелёный.
- **Шаг Mac-verify:** план, который трогает `iosApp/**` вне `Packages/Pure` или меняет фасад
  `shared` (Swift-адаптеры), содержит шаг Mac-verify с проверкой `make check-mac` → зелёный.
  Linux-гейт iOS-UI не доказывает, поэтому без Mac-хоста этот шаг остаётся открытым, а план —
  `status: blocked`, `blocked-by: Mac-хост`. Готов план — `make check` зелёный, плюс `make check-mac`,
  если в нём есть iOS-шаги. Типовая раскладка фичи: презентер с тестом на JVM → экран Android со
  скриншотами → Swift-адаптер (Mac-verify).
- **Handoff:** skill `/infra-handoff`, три адресата — INFRA-HANDOFF в `<org>/infra` (`.well-known`,
  DSN, хосты, URL политики), BACKEND-HANDOFF в `<org>/backend` (endpoint'ы, поля, auth-режим,
  `min_version`), STRATEGY-HANDOFF в `<org>/strategy` (расхождение с картой `mobile/<surface>/`).
  Блоки пишутся в раздел `## Handoff` плана; `/release` собирает их
  (`bash scripts/tools/validate-plan.sh --handoff`) в STORE-HANDOFF (`BACKEND`, `ENV`).
- **Разбор падений:** skill `/triage-failure` — сначала падающий тест.
- **До плана выясни** — только то, без чего шаги строятся на догадках; дизайн-решения — через
  `AskUserQuestion`:
  - платформы: обе или одна (`docs/stack/service.md#platforms`); что уходит в `shared`, что остаётся
    платформенным (UI, permissions, API ОС); есть ли Mac-хост для iOS-шагов;
  - карта в strategy: есть ли stable-запись в `mobile/<surface>/` (экран, фича, сущность, точка
    входа, разрешение); нет — сначала `/mobile-deep-dive` там или `--from-args` со строкой в бэклоге;
    какие состояния экрана и политика без сети;
  - контракт API: есть ли endpoint и поле в `api/openapi.yaml` соседа (нет — BACKEND-HANDOFF);
    идемпотентность мутации, пагинация, `Retry-After`; какие `ErrorModel.code` → какие `MessageKey`;
  - фасад `shared`: меняется ли публичный API (новый `Route`, `State`, презентер) — тогда
    `make api-dump` и Swift-адаптер (Mac-verify); нужен ли новый интерфейс `PlatformDeps`;
  - навигация, deep links, push: новый маршрут или deep link (строки в `deep-links.md` и
    `entry-points.md`, хост и пути в `.well-known` — INFRA-HANDOFF); push-категория: кто шлёт,
    payload, дедуп;
  - разрешения и право: rationale в `permissions.md`, строка в `compliance.md`, usage-description;
    новый SDK — запись в `dependencies.md`, privacy manifest; меняются ли собираемые данные;
  - безопасность: токены и секреты только в `SecureStore`, из auth ничего не логируется, операция
    проверяет сессию;
  - тесты: какой тест первым красный (презентер, экран, `DeepLinkParser`, `AuthPlugin`); нужна ли
    негативная фикстура гейта; меняются ли эталоны скриншотов осознанно (`make screenshots-record`);
  - релиз и совместимость: переживут ли изменение старые версии приложения, нужен ли `min_version`;
    влияет ли на `versionCode`, каналы и чек-лист магазинов;
  - инфра-поверхность (→ `## Handoff`): хост, отпечаток в `.well-known`, `SENTRY_DSN`,
    `API_BASE_URL`, endpoint или auth-режим бэкенда, расхождение с картой.
