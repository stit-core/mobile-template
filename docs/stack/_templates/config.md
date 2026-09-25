---
id: stack.config
kind: config
name: config
status: draft
---

# config

Rollup всех kinds: три класса значений per env. Новый ключ — строка здесь тем же коммитом.

## Build-time

В артефакте, публичен: `config/env/<flavor>.properties` → `BuildConfig.<KEY>`;
`iosApp/Config/<Env>.xcconfig` → `Info.plist` → `AppConfig`.

| ключ | dev | staging | prod | demo | источник |
|---|---|---|---|---|---|
| `APP_ENV` | `dev` | `staging` | `prod` | `demo` | шаблон |
| `API_BASE_URL` | _`https://api.dev.<domain>`_ | _…_ | _…_ | `https://api.demo.invalid` | `<org>/infra` |
| `API_MODE` | `real` | `real` | `real` | `mock` | `api.md` |
| `AUTH_MODE` | `bearer` | `bearer` | `bearer` | `bearer` | `auth.md` |
| `AUTH_BIOMETRIC_UNLOCK` | `false` | `false` | `false` | `false` | `auth.md` |
| `DEEP_LINK_HOST` | _`app.dev.<domain>`_ | _…_ | _`app.<domain>`_ | `app.demo.invalid` | `deep-links.md` |
| `DEEP_LINK_SCHEME` | `stit` | `stit` | `stit` | `stit` | `service.md` |
| `SENTRY_DSN` | пусто | _…_ | _…_ | пусто | `observability.md` |
| `PUSH_PROVIDERS` | `rustore,fcm` / `apns` | то же | то же | пусто | `push.md` |
| `RUSTORE_PROJECT_ID` (Android) | пусто | _…_ | _…_ | пусто | `push.md` |
| `APP_GROUP_ID` (iOS) | `group.<bundle id>.dev` | _…_ | `group.<bundle id>` | `group.<bundle id>.demo` | `service.md` |
| `ANALYTICS` | — (ключа в шаблоне нет; появляется вместе с kind `analytics`) | | | | `analytics.md` |

## Release-time

Нужны сборке/публикации, **не** в артефакте: env процесса `make release-*`, значения хранятся вне
репозитория (где — `signing.md#inventory`) и экспортируются человеком на время команды; локально —
`.env.local` (gitignored, образец `.env.local.example`).

| ключ | зачем |
|---|---|
| `STIT_KEYSTORE_PATH`, `STIT_KEYSTORE_PASSWORD`, `STIT_KEY_ALIAS`, `STIT_KEY_PASSWORD` | подпись AAB/APK |
| `PLAY_SERVICE_ACCOUNT_JSON` | Play internal |
| `RUSTORE_KEY_ID`, `RUSTORE_PRIVATE_KEY` | RuStore |
| `ASC_KEY_ID`, `ASC_ISSUER_ID`, `ASC_KEY_P8`, `MATCH_GIT_URL`, `MATCH_PASSWORD` | TestFlight, подпись IPA |
| `SENTRY_AUTH_TOKEN`, `SENTRY_ORG`, `SENTRY_PROJECT_*`, `SENTRY_URL` | символы |
| `GITEA_TOKEN`, `MAC_HOST` (не секрет) | STOP-1, iOS-стадии |

## Runtime

Меняется без релиза — бэкенд `GET /api/app/requirements?platform&version&build&store`
(`Cache-Control: max-age=300`; запрос на старте и при возврате из фона, не чаще раза в 5 мин).

| поле | назначение |
|---|---|
| `status` | `ok \| update_recommended \| update_required \| maintenance` |
| `min_version`, `latest_version`, `store_url` | kill-switch старых версий, ссылка на магазин |
| `maintenance_until` | экран обслуживания |
| `flags` | feature flags (строки; дефолт — аргумент `AppRequirements.flag(name, default)`), в т. ч. `demo_login`, `pinning` |
| `links` | `privacy_policy`, `account_deletion`, `support` |

## Requirements

Реализация — `core/requirements/RequirementsClient`; 404 у бэкенда ⇒ «требований нет»
(BACKEND-HANDOFF B-17).

## Release checklist

- новые ключи build-time — во всех `config/env/*.properties` и `*.xcconfig`;
- `APP_GROUP_ID` совпадает с App Group в entitlements приложения и расширения виджетов;
- секреты — только release-time, ни одного в build-time;
- `SENTRY_DSN` prod задан, `API_BASE_URL` prod — боевой хост;
- `.env.local.example` содержит все имена release-time ключей.

## Generates

- `config/env/{dev,staging,prod,demo}.properties`, `iosApp/Config/{Dev,Staging,Prod,Demo}.xcconfig`
- `.env.local.example`, `local.properties.example`, `iosApp/Config/Local.xcconfig.example`
- `shared/…/core/config/{AppConfig,AppEnv,ApiMode,AuthMode}.kt` (`AppConfig.parse(...)` из сырых строк), `AppFoundation.AppConfig`; платформенные ключи — `AppOptions` (`androidApp/app`)
- seed для INFRA-HANDOFF (хосты, DSN) и списка release-time ключей `/release` (факт «задано» для
  `STIT_KEYSTORE_*` проверяет `build-android.sh`)

## Открытые вопросы

- _что_
