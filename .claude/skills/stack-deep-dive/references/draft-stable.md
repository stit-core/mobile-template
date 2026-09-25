# Контракт `draft → stable`

Артефакт становится `stable`, когда **все 6 пунктов одновременно** ✅. Скилл проверяет,
архитектор подтверждает, скилл применяет правку `status:`.

## Чек-лист

### 1. Обязательные секции заполнены

`references/field-checklists.md` — по каждому kind перечислены обязательные H2. К stable —
осмысленное содержимое без плейсхолдеров `<…>`, `_…_`, `TBD`. Секции, неприменимые к
`platforms` (iOS при `[android]`), — отсутствуют, а не пусты.

### 2. `## Generates` конкретна

**Главный пункт**: это контракт для `/de-template` и `/scaffold`. Конкретно — это:
- пути: `config/env/prod.properties`, `iosApp/Config/Prod.xcconfig`,
  `androidApp/app/src/main/AndroidManifest.xml`, `shared/src/commonMain/kotlin/tech/stit/mobile/core/deeplink/Route.kt`;
- имена ключей: `API_BASE_URL`, `PUSH_PROVIDERS`, `DEEP_LINK_HOST`;
- скрипты/вербы: `make gen`, `scripts/release/build-android.sh`, `CHANNELS=rustore,play`;
- handoff-блоки: `INFRA-HANDOFF I-1 → .well-known`, `BACKEND-HANDOFF B-17`.

Не считается: «соответствующий код», «нужные env», «настройки платформы» без имён.

### 3. Cross-kind-инварианты выполнены

`docs/stack/README.md § Cross-kind-инварианты` (1–8). Программная проверка для kind'а:
- каждый env/ключ, упомянутый в теле или `Generates`, есть в `config.md` в нужном классе
  (build-time / release-time / runtime);
- каждый провайдер `push.md` — SDK в `dependencies.md` + строка в `compliance.md`;
- каждый хост `deep-links.md` — в `config.md` per env и в INFRA-HANDOFF; отпечатки ==
  `signing.md`;
- `auth.md#mode = bearer` ⇔ спека в `api.md` содержит `POST /api/auth/refresh`, иначе —
  `blocked-by: B-1…B-3`;
- `distribution.md#channels` ⊆ `service.md#platforms`; `ios ∈ platforms` ⇒
  `runtime.md#mac-host` заполнен или `blocked-by: Mac`;
- `observability.md#receiver ≠ none` ⇒ `SENTRY_DSN` per env в `config.md`.

Дрейф → скилл предлагает автодополнение через `AskUserQuestion`, stable не выдаёт.

### 4. Frontmatter валиден

```yaml
id: stack.<name>     # == "stack." + basename
kind: <name>         # один из 16
name: <name>         # == basename файла без .md
status: draft|stable
```

Проверка: `grep -E '^(id|kind|name|status):' docs/stack/<kind>.md`.

### 5. Тест-критерий kind'а подтверждён

В этой сессии, через `AskUserQuestion` с ✅/❌-примерами. Шаблоны — ниже.

### 6. State чист по `**blocker**`

`state/<kind>.md` не содержит вопросов с `**blocker**`. Не-blocker вопросы при stable
допустимы.

## Тест-критерий по kind

- **service** — ✅ `applicationId tech.acme.mobile` (+ `.dev`/`.staging`/`.demo`), bundle
  `tech.acme.mobile`, `platforms: [android, ios]`, `preset: companion`, `project-slug acme`,
  `surface app`, владелец аккаунтов — заказчик. ❌ `tech.stit.mobile` (не сменили), `preset` пуст.
- **runtime** — ✅ Kotlin 2.4.20 / AGP 9.4.0 / Gradle 9.7.1 / JDK 21; minSdk 26 / target 37;
  Xcode 27.0 / Swift 6.4 / iOS 18; `MAC_HOST` задан или `blocked-by`; политика latest stable с
  2-недельной выдержкой. ❌ «актуальные версии» без чисел; Mac не упомянут при iOS.
- **api** — ✅ `backend-path ../backend`, копия + lock, генератор 7.25 models-only,
  конверт `data`, `ErrorModel.code`, `X-Request-ID`, `Idempotency-Key`, `min backend version`.
  ❌ «клиент генерируется», версия бэкенда не зафиксирована.
- **auth** — ✅ `mode: bearer`, access 15 мин в памяти, refresh 90 д в `SecureStore`, ротация,
  сессии, удаление аккаунта, биометрия как unlock, pinning off. ❌ `mode` не выбран; токен в
  `SharedPreferences`.
- **deep-links** — ✅ хосты per env, отпечатки upload/Play/RuStore, таблица путь → `Route` →
  сессия → pending, INFRA-HANDOFF `MOBILE_*`. ❌ один хост «на все среды», отпечаток один.
- **push** — ✅ провайдеры per flavor (`rustore,fcm` / `apns`), регистрация `PUT /api/v1/devices`,
  каналы/категории, permission-flow «в момент ценности», payload data-first. ❌ «FCM» без
  RuStore для RU; запрос разрешения на первом экране.
- **observability** — ✅ приёмник (GlitchTip/Sentry) и DSN per env (dev пусто), PII-политика,
  mapping/dSYM — шаг `/release` до STOP-2, пороги crash-free 99.5 %/48 ч. ❌ «Sentry есть» без
  DSN-политики и символов.
- **styling** — ✅ таблица токенов DTCG (`token · light · dark · android · ios`), тема
  system/light/dark, Material 3 / Liquid Glass политика, i18n-локали `ru`(default)/`en`.
  ❌ «Material Theme» без токенов; локали не названы.
- **distribution** — ✅ каналы per платформа по пресету, STOP-1/STOP-2 состав, аккаунты
  (владелец), сроки модерации, release-notes-политика, App Review notes с demo-доступом.
  ❌ каналы без аккаунтов; demo для ревью не описан.
- **signing** — ✅ инвентарь ключей (upload, app-signing, RuStore, Apple certs/profiles, APNs
  key), custody вне репозитория (где хранится каждый ключ и кто владелец), оба отпечатка,
  ротация, резервная копия. ❌ «ключи у разработчика»; отпечатки не записаны.
- **compliance** — ✅ Data safety / Nutrition Label таблица, permissions-матрица с обоснованием,
  privacy manifests SDK, 152-ФЗ-статус, возрастной рейтинг, шифрование, URL политики/удаления,
  чек-лист L-1…L-17 с датой. ❌ «политика будет»; permission без обоснования.
- **dependencies** — ✅ каждая библиотека/SDK: назначение, лицензия, KMP-совместимость,
  privacy-manifest-статус; швы (`MapSurface`, аналитика, платежи) названы. ❌ список без
  лицензий и статуса манифестов.
- **config** — ✅ три таблицы build-time / release-time / runtime per env, схема
  `/api/app/requirements`, флаги, release-checklist. ❌ «как в шаблоне»; секрет в build-time.
- **analytics** — ✅ провайдер (AppMetrica / self-host / none), реестр событий, согласие,
  `AD_ID`/ATT не запрашиваются. ❌ SDK без согласия и записи в compliance.
- **payments** — ✅ RuStore Pay / Play Billing / StoreKit 2: продукты, восстановление,
  серверная валидация, правила 3.1.x. ❌ «оплата на сайте» в приложении.

## Алгоритм проверки скиллом

1. `Read` артефакт; `Read` `state/<kind>.md`, если есть.
2. Пункты 1, 2, 3, 4, 6 — программно (grep секций, плейсхолдеров, ключей).
3. Все 5 ✅ → раунд `AskUserQuestion` по тест-критерию (✅/❌ + Other).
4. Пункт 5 ✅ → `AskUserQuestion` «Готов к stable, повышаем?» (`Да` / `Нет`).
5. «Да» → `Edit` `status: draft → stable`; повторная проверка frontmatter.

## Что не делать

- Не повышать сам; не понижать `stable → draft` сам.
- Не считать `Generates` заполненной без конкретных путей/ключей.
- Не считать cross-kind sync согласованным, если архитектор сказал «потом» — `**blocker**`
  в state rollup'а, stable не выдавать.
