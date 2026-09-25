---
name: env-and-config
description: Три класса значений — build-time (flavor/xcconfig → BuildConfig/Info.plist), release-time (env процесса make release-*, значения вне репозитория, не в артефакте), runtime (GET /api/app/requirements); флейворы dev|staging|prod|demo = схемы; AppConfig собирается платформой и передаётся в SharedContainer, платформенные ключи — AppOptions; секретов в build-time нет; новый ключ → строка в docs/stack/config.md. Грузится при правке config/env, xcconfig, core/config, манифеста.
paths:
  - "config/env/**"
  - "iosApp/Config/**"
  - "shared/**/core/config/**"
  - "androidApp/app/src/*/AndroidManifest.xml"
---

# env и конфиг

Значения проекта per env — `docs/stack/config.md` (rollup всех kinds). Здесь — инварианты.

## Три класса значений

| класс | где живёт | секрет? | примеры |
|---|---|---|---|
| **build-time** (в артефакте, публичен) | `config/env/<flavor>.properties` → `BuildConfig.<KEY>` + `manifestPlaceholders`; `iosApp/Config/<Env>.xcconfig` → `Info.plist` → `AppFoundation.AppConfig` | нет | `APP_ENV`, `API_BASE_URL`, `API_MODE`, `AUTH_MODE`, `DEEP_LINK_HOST`, `DEEP_LINK_SCHEME`, `SENTRY_DSN` (public by design), `PUSH_PROVIDERS`, `RUSTORE_PROJECT_ID`, `AUTH_BIOMETRIC_UNLOCK` |
| **release-time** (нужны сборке/публикации, не в артефакте) | env процесса `make release-*`: значения хранятся вне репозитория, экспортирует человек; `.env.local` gitignored | да | `STIT_KEYSTORE_*`, `PLAY_SERVICE_ACCOUNT_JSON`, `RUSTORE_*`, `ASC_*`, `SENTRY_AUTH_TOKEN`, `GITEA_TOKEN` |
| **runtime** (меняется без релиза) | бэкенд: `GET /api/app/requirements` (`min_version`, `maintenance`, `flags`, `links`), кэш локально | нет | min-version, kill-switch, URL политики, флаги |

## Инварианты

- **Flavors = схемы**: `dev | staging | prod | demo`; список flavors — файлы `config/env/`
  (`envFlavors()` в `build-logic`), одна ось `env` на все модули; `demo` — `API_MODE=mock`,
  `applicationIdSuffix ".demo"`, хост `.invalid` (в сеть не ходит). `APP_ENV` виден на экране
  «О приложении» (`SettingsState.env`).
- **Секретов в build-time нет**: каждый ключ `config/env` попадает в APK как строка.
  Платный API — public-by-design ключ с ограничением по applicationId/bundle или прокси через
  бэкенд.
- **`AppConfig` собирает платформа** (`BuildConfig` в `di/ConfigModule.kt` / `Bundle` в
  `AppEnvironment.swift`) через `AppConfig.parse(env, apiBaseUrl, apiMode, authMode, deepLinkHost,
  deepLinkScheme, sentryDsn)` — сырые строки, одна точка разбора `AppEnv`/`ApiMode`/`AuthMode` — и
  передаёт в `SharedContainer(platform, config)`; `shared` env напрямую не читает. Ключи только для
  платформы — `AppOptions` (Android: `pushProviders`, `rustoreProjectId`, `biometricUnlock`) и
  `AppFoundation.AppConfig.pushProviders` (iOS; пустыми могут быть только `SENTRY_DSN`, `PUSH_PROVIDERS`).
- **Runtime-значения — только через `requirements`** (`core/requirements`), не через свои
  endpoint'ы; дефолт флага — аргумент `AppRequirements.flag(name, default)`; отдельного ключа
  офлайн-дефолтов в `config/env` нет. Переключателя среды в приложении нет: смена среды = другой
  flavor/схема.
- **Новый ключ** — строка в `docs/stack/config.md` тем же коммитом + значение во всех
  `config/env/*.properties` (иначе `envFlavors()` соберёт неполный `BuildConfig`) и `*.xcconfig` +
  `Info.plist` (иначе `AppConfigError.missingKey`). Хосты (`API_BASE_URL`, `DEEP_LINK_HOST`), DSN —
  INFRA-HANDOFF (rule `infra-impact`).
- **`AndroidManifest.xml`**: permissions — только с записью в `docs/stack/compliance.md`
  (rule `privacy-and-law`; шаблон объявляет `INTERNET` в `app`, `ACCESS_NETWORK_STATE`,
  `POST_NOTIFICATIONS`, `USE_BIOMETRIC` в `core:platform`); intent-filters deep links — из
  `deep-links.md` через плейсхолдеры (стадия `deeplinks-check`); `allowBackup`/`dataExtractionRules`
  — `res/xml/backup_rules.xml`, `data_extraction_rules.xml`.
- `local.properties` (`sdk.dir`) и `Local.xcconfig` — локальные, gitignored; образцы —
  `local.properties.example`, `iosApp/Config/Local.xcconfig.example`.

## Сигналы

- `config/env пуст: нужен хотя бы один <flavor>.properties` → плагин не нашёл flavors.
- `make status` → `Platforms`/`Critical` читают `docs/stack/service.md` и `config/env`.
