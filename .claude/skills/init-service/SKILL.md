---
name: init-service
description: Оркестратор онбординга потребителя шаблона — ведёт путь форк → идентичность (platforms, preset, applicationId/bundle id, auth-режим) → env/config → связка с картой mobile/<surface>/ в соседнем <org>/strategy → черновики docs/stack (/stack-deep-dive) → handoffs соседям → /de-template, пока проект не перестанет помнить про шаблон. Применяй, когда юзер (или агент хаба <org>/infra) начинает свой проект из форка mobile-template или видишь свежий форк (applicationId ещё tech.stit.mobile, reference-фичи profile/todos на месте, docs/stack пуст). Сам не проектирует и не де-темплейтит — чейнит специализированные скиллы по порядку.
---

# Skill: init-service (онбординг-оркестратор)

Дирижёр пути «форк шаблона → свой проект». Не делает проектирование/генерацию сам, а
**ведёт по стадиям и чейнит** специализированные скиллы, держа фокус: docs первичны, код
вторичен и разворачивается по ним. Один проход доводит до проекта, который ничего не помнит про
шаблон. Полное описание пути для человека — `docs/getting-started.md`; этот скилл его исполняет.

## Когда применять

- «/init-service» / «онбординг» / «форкнул, что дальше»
- Свежий форк: `applicationId` ещё `tech.stit.mobile`, пакет `tech.stit.mobile.*`,
  reference-фичи `profile`/`todos` на месте, `docs/stack/` содержит только `README.md` и `_templates/`
- `--add-platform ios|android` — восстановление половины после `/de-template --platform`

## Когда не применять

- Проект уже де-темплейтизирован (свой `applicationId`, reference-фичи удалены) — обычный
  день-в-день через `/smartdev`.
- Нужно добавить экран/фичу в уже свой проект — `/scaffold`.

## Pre-flight (стадия 0)

- [ ] Форк создан (Gitea → **Use this template** → `<org>/mobile`), склонирован sibling'ом
      рядом с `<org>/strategy`, `<org>/backend`, `<org>/infra`.
- [ ] `make setup` прогнан (doctor + git-хуки + Android-скиллы + прогрев демона); `make doctor`
      OK или только WARN.
- [ ] Соседи: `../strategy`, `../backend`, `../infra` — что есть, что нет (нет — не стоп: скилл
      печатает next-step и деградирует).
- [ ] Mac-хост: `MAC_HOST` задан и `make mac-doctor` OK — предусловие **iOS-ветки**; без него
      `platforms: [android, ios]` допустим, но iOS помечается «не верифицирован» и
      `/de-template` потребует `--ios-unverified`.

Что-то из первого пункта нет — подскажи шаги 0 из `docs/getting-started.md`, дальше не иди.

## Стадии (чейнить по порядку, не делать самому)

### 1. Идентичность → `docs/stack/service.md`

Через `AskUserQuestion` (≤ 4 вопросов за раунд), значения — только от юзера:
`project-slug` (папка продукта в strategy), имя приложения, `applicationId` / bundle id per env
(`<reverse-domain>.<app>` + `.dev`/`.staging`/`.demo`), `platforms: [android, ios]` (или одна),
`preset: companion | consumer | enterprise` (дефолт `companion`), `auth.mode: bearer | cookie |
none`, схема deep link (`<scheme>://`), языки (`ru` default), владелец store-аккаунтов.
Скопируй `docs/stack/_templates/service.md` → `docs/stack/service.md`, заполни, `status: draft`.

### 2. Env и config → `docs/stack/config.md` (черновик)

Хосты API per env (`API_BASE_URL`), политика `SENTRY_DSN` (пусто = выключено), `PUSH_PROVIDERS`
(Android `rustore,fcm`; iOS `apns`; `[]` — без push), `ANALYTICS` (`none`), локали. Значения
уходят в `config/env/*.properties` и `iosApp/Config/*.xcconfig` на `/de-template`, а сейчас —
в черновик `config.md` (rollup соберёт `/stack-deep-dive`).

### 3. Связка с картой в `<org>/strategy`

- Резолв: `<strategy-path>/docs/projects/<project-slug>/` (дефолт `../strategy`); `surface` —
  строка реестра `surface.md` с `type: mobile` (пикер по именам через `AskUserQuestion`).
- Три ветки:
  1. **Канона `mobile/` нет в форке strategy** (`docs/projects/_template/mobile/` отсутствует)
     → печать next-step с точными командами (сам в соседа не пишешь):
     ```bash
     cp -r docs/reference/strategy-mobile-canon/. ../strategy/docs/projects/_template/mobile/
     mkdir -p ../strategy/docs/projects/<slug>/mobile/<surface>
     cp ../strategy/docs/projects/_template/mobile/_template/*.md ../strategy/docs/projects/<slug>/mobile/<surface>/
     ls ../strategy/docs/projects/<slug>/mobile/<surface>/*.md | wc -l   # 12 форм
     ```
     + строка в `.claude/plans/backlog.md`: «S-1 канон слоя mobile/ в strategy».
  2. **Канон есть, папки поверхности нет** → «спроектируй через `/mobile-deep-dive <slug> <surface>`
     в `<org>/strategy` и вернись»; можно продолжать стадии 4–5, `/de-template` подождёт.
  3. **Соседа нет вообще** → `strategy-path` не задан; `/scaffold` работает в режиме
     `--from-args` с предупреждением; строка в `.claude/plans/backlog.md` «завести карту».
- Если канон в strategy есть — сверь формы с копией:
  `diff -r docs/reference/strategy-mobile-canon/_template
  <strategy-path>/docs/projects/_template/mobile/_template` — 12 форм (entities · screens ·
  flows · features · shell · entry-points · permissions · system-actions · widgets ·
  background-tasks · shared-api · shared-lib). Расхождение или нехватка форм — WARN юзеру
  (источник правды — strategy). README копии несёт свой абзац про источник правды — это не
  расхождение, сверять её целиком не нужно.
- Запиши `strategy-path` / `surface` в `service.md`.

### 4. Черновики stack → `/stack-deep-dive`

Разложи недостающие kinds из `docs/stack/_templates/` (14 обязательных + `analytics`/`payments`
по пресету) с подставленными значениями стадий 1–2, `status: draft`. **Чейнь в
`/stack-deep-dive`** — волны W1…W8 до `status: stable` всех обязательных kinds с непустой
`## Generates`. Gate: все обязательные kinds `stable`, cross-kind-инварианты 1–8
(`docs/stack/README.md`) выполнены, `state/` без `**blocker**`.

### 5. Handoffs соседям → `/infra-handoff`

- **BACKEND-HANDOFF**: проверь `<backend-path>/api/openapi.yaml` (дефолт `../backend`) на
  `POST /api/auth/refresh`, `GET /api/app/requirements`, `PUT /api/v1/devices`; отсутствуют —
  печатай B-1…B-3 / B-17 / B-18 (`auth.mode: cookie` — режим совместимости до B-1…B-3).
  Спеки соседа нет — reference-спека шаблона остаётся.
- **INFRA-HANDOFF**: `.well-known` на хостах deep links (I-1: `applicationId`, отпечатки,
  Team ID · bundle id — по `deep-links.md`), `SENTRY_DSN` per env, URL политики/удаления
  аккаунта (I-6).
- Блоки — в ответ (юзеру или хабу `<org>/infra`, если онбординг запущен оттуда) и в
  раздел `## Handoff` плана онбординга (`.claude/plans/`).

### 6. Развернуть фундамент → `/de-template`

Gate `/de-template`: stack `stable`, карта `mobile/<surface>/` stable (или `--no-strategy` со
строкой в `.claude/plans/backlog.md`), `make check` зелёный, при `ios ∈ platforms` — `make check-mac` зелёный
или явный `--ios-unverified`. **Чейнь в `/de-template`** (Phase A идентичность → Phase B prune
и docs → Verify). Затем `/scaffold` по stable-записям карты.

### `--add-platform ios|android`

Восстановление половины из upstream-ветки шаблона (`git remote add template
<url mobile-template>`; `git checkout template/main -- iosApp/ .xcode-version .swift-version
scripts/tools/mac.sh docs/reference/mac-host.md` и iOS-секции kinds из `_templates`) + возврат
`iosArm64()`/`iosSimulatorArm64()` в `shared/build.gradle.kts` (плагин `stit.kmp.library` их
объявляет — достаточно вернуть таргеты). Путь ручной и документированный: после него —
`/stack-deep-dive` по iOS-секциям и `make check`.

## Финал

`make check` зелёный (и `make check-mac`, если iOS) → проект больше не помнит про шаблон.
Сообщи юзеру: онбординг закрыт, дальше `/smartdev`; первый релиз — `/release --stop 1`.
`docs/getting-started.md` (META) удалён `/de-template`.

## Что init-service не делает

- ❌ Не проектирует карту (`/mobile-deep-dive` в strategy) и стек (`/stack-deep-dive` здесь) сам.
- ❌ Не пишет в `../strategy`, `../backend`, `../infra` — только next-step и handoff-блоки.
- ❌ Не правит фундамент руками и не де-темплейтит — это `/de-template`.
- ❌ Не запускает `/de-template` до зелёных gate'ов.
- ❌ Не выдумывает `applicationId`, хосты, provider'ы — только от юзера.

## Anti-patterns

- ❌ Кодить фичи до того, как спроектированы артефакты.
- ❌ Удалять reference-фичи до того, как свои спроектированы (теряешь живой образец).
- ❌ Считать iOS готовым без Mac-хоста.
