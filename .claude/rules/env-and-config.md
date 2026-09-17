---
name: env-and-config
description: Три класса значений — build-time (flavor/xcconfig → BuildConfig/Info.plist), release-time (env из Vaultwarden, не в артефакте), runtime (GET /api/app/requirements); флейворы dev|staging|prod|demo = схемы; AppConfig собирается платформой и передаётся в SharedContainer; секретов в build-time нет; новый ключ → строка в docs/stack/config.md. Грузится при правке config/env, xcconfig, core/config, манифеста, .env.local.example.
paths:
  - "config/env/**"
  - "iosApp/Config/**"
  - "shared/**/core/config/**"
  - "androidApp/app/src/*/AndroidManifest.xml"
  - ".env.local.example"
---

# env и конфиг

Значения проекта per env — `docs/stack/config.md` (rollup всех kinds). Здесь — инварианты.

## Три класса значений

| класс | где живёт | секрет? | примеры |
|---|---|---|---|
| **build-time** (в артефакте, публичен) | `config/env/<flavor>.properties` → `BuildConfig.<KEY>`; `iosApp/Config/<Env>.xcconfig` → `Info.plist` → `AppConfig` | нет | `APP_ENV`, `API_BASE_URL`, `API_MODE`, `AUTH_MODE`, `DEEP_LINK_HOST`, `DEEP_LINK_SCHEME`, `SENTRY_DSN` (public by design), `PUSH_PROVIDERS` |
| **release-time** (нужны сборке/публикации, не в артефакте) | env процесса `make release-*` из Vaultwarden; `.env.local` gitignored | да | `STIT_KEYSTORE_*`, `PLAY_SERVICE_ACCOUNT_JSON`, `RUSTORE_*`, `ASC_*`, `SENTRY_AUTH_TOKEN`, `GITEA_TOKEN` |
| **runtime** (меняется без релиза) | бэкенд: `GET /api/app/requirements` (`min_version`, `maintenance`, `flags`, `links`), кэш локально | нет | min-version, kill-switch, URL политики, флаги |

## Инварианты

- **Flavors = схемы**: `dev | staging | prod | demo`; список flavors — файлы `config/env/`,
  одна ось `env` на все модули; `demo` — `API_MODE=mock`, `applicationIdSuffix ".demo"`,
  хост `.invalid` (в сеть не ходит). `APP_ENV` виден на экране «О приложении».
- **Секретов в build-time нет**: каждый ключ `config/env` попадает в APK как строка.
  Платный API — public-by-design ключ с ограничением по applicationId/bundle или прокси через
  бэкенд.
- **`AppConfig` собирает платформа** (`BuildConfig` / `Bundle`) через `AppConfig.parse(env,
  apiBaseUrl, apiMode, authMode, deepLinkHost, deepLinkScheme, sentryDsn)` — сырые строки, одна
  точка разбора `AppEnv`/`ApiMode`/`AuthMode` — и передаёт в `SharedContainer(platform, config)`;
  `shared` env напрямую не читает.
- **Runtime-значения — только через `requirements`** (`core/requirements`), не через свои
  endpoint'ы; офлайн-дефолты флагов — `FEATURE_FLAGS_DEFAULT` в build-time.
- **Debug-переключатель среды** (список `API_BASE_URL_CHOICES`) — только в `dev`-flavor;
  смена среды = очистка `SecureStore` и кэшей.
- **Новый ключ** — строка в `docs/stack/config.md` тем же коммитом + значение во всех
  `config/env/*.properties` и `*.xcconfig` (иначе `envFlavors()` соберёт неполный
  `BuildConfig`). Хосты (`API_BASE_URL`, `DEEP_LINK_HOST`), DSN — INFRA-HANDOFF
  (rule `infra-impact`).
- **`AndroidManifest.xml`**: permissions — только с записью в `docs/stack/compliance.md`
  (rule `privacy-and-law`); intent-filters deep links — из `deep-links.md`; `allowBackup`/
  `dataExtractionRules` исключают токены.
- `local.properties` (`sdk.dir`) и `Local.xcconfig` — локальные, gitignored; образцы —
  `*.example`.

## Сигналы

- `config/env пуст: нужен хотя бы один <flavor>.properties` → плагин не нашёл flavors.
- `make status` → `Platforms`/`Critical` читают `docs/stack/service.md` и `config/env`.
