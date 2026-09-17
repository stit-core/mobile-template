---
id: stack.observability
kind: observability
name: observability
status: draft
---

# observability

## Receiver

| поле | значение |
|---|---|
| receiver | `glitchtip` _(self-host, Sentry-совместимый DSN; `sentry` SaaS — опция; `none`)_ |
| SDK | `sentry-android` 8.57 (Android) · `sentry-cocoa` 9.28 (iOS, SPM) — инициализация в `core:platform` / `Core/Platform` через `TelemetrySink` (`PlatformDeps.telemetry`); Gradle-плагин Sentry не подключён |
| projects | _`<app>-android`, `<app>-ios` (или один с тегом `platform`)_ |

## DSN per env

| env | `SENTRY_DSN` |
|---|---|
| dev, demo | пусто — SDK не инициализируется |
| staging | _`https://<key>@glitchtip.<domain>/<id>`_ |
| prod | _`https://<key>@glitchtip.<domain>/<id>`_ |

`environment = APP_ENV`, `release = <applicationId>@X.Y.Z+<code>`, `dist = <code>`.

## PII policy

`sendDefaultPii = false`, `attachScreenshot = false`; `beforeSend` на платформе режет
`Authorization`, `Cookie`, email, тела запросов; `user.id` — только id, сбрасывается при logout;
инициализация — после согласия пользователя (`compliance.md`).

## Logging

`Logger`/`LogSink` из `core/observability` (`LogcatSink` в debug, `SentryLogSink` в release, `SharedLogSink`
на iOS) и `AppFoundation.AppLogger` (`os.Logger`); запреты
`android.util.Log`/`println`/`print`/`NSLog` — detekt/SwiftLint; в release — breadcrumbs ≥ warn;
структурные поля `request_id`, `screen`, `user_id`; breadcrumbs навигации и сети без тел.

## Symbols

`mapping.txt` — ассет STOP-1 из `make release-android`, в GlitchTip — `sentry-cli upload-proguard` (шаг
`/release`); dSYM — `sentry-cli debug-files upload` на Mac; оба до STOP-2 (в `preflight.sh` проверки нет). Секреты —
`SENTRY_AUTH_TOKEN`, `SENTRY_ORG`, `SENTRY_PROJECT_*`, `SENTRY_URL` (release-time).

## Quality thresholds

| метрика | порог | где проверяется |
|---|---|---|
| crash-free sessions | ≥ 99.5 % за 48 ч на internal/TestFlight | STORE-HANDOFF перед промоушеном |
| ANR rate (Android) | < 0.47 % | Android vitals / Sentry |
| hang rate (iOS) | < 1 % | Sentry |
| холодный старт | p50 < 1.5 с, p90 < 3 с | релиз-чеклист (устройство) |

`TelemetrySink` — шов: Sentry на платформе (дефолт) · `NoopTelemetrySink` (`shared`) · `otlp` (при коллекторе проекта,
`OTEL_EXPORTER_OTLP_ENDPOINT`).

## Generates

- `config/env/*.properties`, `iosApp/Config/*.xcconfig` — `SENTRY_DSN`
- `androidApp/core/platform/…/SentryCrashReporter.kt`, `iosApp/Packages/Core/Sources/Platform/SentryCrashReporter.swift`
- шаг символов (mapping + dSYM через `sentry-cli`) в `/release`
- пороги — блок `RISK`/`PLAY` STORE-HANDOFF
- INFRA-HANDOFF I-5 (проекты и DSN в GlitchTip)

## Открытые вопросы

- _что_
