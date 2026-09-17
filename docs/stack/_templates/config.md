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
| `ANALYTICS` | `none` | `none` | `none` | `none` | `analytics.md` |
| `FEATURE_FLAGS_DEFAULT` | _`flag=false,…`_ | _…_ | _…_ | _…_ | `requirements` |
| `API_BASE_URL_CHOICES` (только dev) | _`…,http://10.0.2.2:8080`_ | — | — | — | шаблон |

## Release-time

Нужны сборке/публикации, **не** в артефакте: env процесса `make release-*` из Vaultwarden
`mobile/<org>/<app>`; локально — `.env.local` (gitignored, образец `.env.local.example`).

| ключ | зачем | запись Vaultwarden |
|---|---|---|
| `STIT_KEYSTORE_PATH`, `STIT_KEYSTORE_PASSWORD`, `STIT_KEY_ALIAS`, `STIT_KEY_PASSWORD` | подпись AAB/APK | `android-upload-key` |
| `PLAY_SERVICE_ACCOUNT_JSON` | Play internal | `play-service-account` |
| `RUSTORE_KEY_ID`, `RUSTORE_PRIVATE_KEY` | RuStore | `rustore-api-key` |
| `ASC_KEY_ID`, `ASC_ISSUER_ID`, `ASC_KEY_P8`, `MATCH_GIT_URL`, `MATCH_PASSWORD` | TestFlight, подпись IPA | `apple-asc-api-key`, `apple-signing` |
| `SENTRY_AUTH_TOKEN`, `SENTRY_ORG`, `SENTRY_PROJECT_*`, `SENTRY_URL` | символы | `sentry` |
| `GITEA_TOKEN`, `NTFY_TOPIC`, `NTFY_TOKEN`, `MAC_HOST` | STOP-1, уведомления, iOS-стадии | `gitea-release-token`, `ntfy`, — |

## Runtime

Меняется без релиза — бэкенд `GET /api/app/requirements?platform&version&build&store`
(`Cache-Control: max-age=300`; запрос на старте и при возврате из фона, не чаще раза в 5 мин).

| поле | назначение |
|---|---|
| `status` | `ok \| update_recommended \| update_required \| maintenance` |
| `min_version`, `latest_version`, `store_url` | kill-switch старых версий, ссылка на магазин |
| `maintenance_until` | экран обслуживания |
| `flags` | feature flags (строки; дефолты — `FEATURE_FLAGS_DEFAULT`), в т. ч. `demo_login`, `pinning` |
| `links` | `privacy_policy`, `account_deletion`, `support` |

## Requirements

Реализация — `core/requirements/RequirementsClient`; 404 у бэкенда ⇒ «требований нет»
(BACKEND-HANDOFF B-17).

## Release checklist

- новые ключи build-time — во всех `config/env/*.properties` и `*.xcconfig`;
- секреты — только release-time, ни одного в build-time;
- `SENTRY_DSN` prod задан, `API_BASE_URL` prod — боевой хост;
- `.env.local.example` содержит все имена release-time ключей.

## Generates

- `config/env/{dev,staging,prod,demo}.properties`, `iosApp/Config/{Dev,Staging,Prod,Demo}.xcconfig`
- `.env.local.example`, `local.properties.example`, `iosApp/Config/Local.xcconfig.example`
- `shared/…/core/config/{AppConfig,AppEnv,ApiMode,AuthMode}.kt` (`AppConfig.parse(...)` из сырых строк), `AppFoundation.AppConfig`
- seed для INFRA-HANDOFF (хосты, DSN) и `release-preflight` (release-time ключи)

## Открытые вопросы

- _что_
