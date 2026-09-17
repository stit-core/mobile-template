---
name: observability
description: Логи только через Logger + LogSink из shared / os.Logger; пустой DSN = Sentry выключен; PII-скраббинг в beforeSend; breadcrumbs по навигации и сети без тел; X-Request-ID в событиях; mapping/dSYM загружаются релизом; TelemetrySink — шов для OTel. Грузится при правке core/observability, Sentry*, *Logger*, logging/.
paths:
  - "shared/**/core/observability/**"
  - "androidApp/**/Sentry*"
  - "iosApp/**/Sentry*"
  - "**/*Logger*"
  - "**/logging/**"
---

# Observability

Полное описание — `docs/reference/observability.md`; значения проекта —
`docs/stack/observability.md`.

## Инварианты

- **Логи — только через `Logger(tag, sink)`** из `core/observability/Logger.kt` в
  `shared`/Android и `AppFoundation.log` (`os.Logger`) на iOS; `LogSink` даёт платформа
  (`PlatformDeps.logSink`): debug — `LogcatSink`/`NSLogSink`, release — sink в breadcrumbs
  Sentry уровня ≥ warn. `android.util.Log` (кроме `LogcatSink`), `println`,
  `java.util.logging` — detekt `ForbiddenImport`; `print`/`NSLog` — SwiftLint.
- **Пустой `SENTRY_DSN` = SDK не инициализируется** (dev/demo по умолчанию); `environment =
  APP_ENV`, `release = <applicationId>@X.Y.Z+<code>` (одинаков на обеих платформах),
  `dist = <code>`. Приёмник — self-host GlitchTip (Sentry-совместимый DSN).
- **Инициализация после согласия** пользователя на крэш-репорты (rule `privacy-and-law`).
- **PII не утекает**: `sendDefaultPii = false`, `attachScreenshot = false`, `beforeSend`
  Sentry на платформе режет `Authorization`, `Cookie`, email, тела запросов; из `shared` в
  `TelemetrySink` уходят только `breadcrumb(category, message)`, `captureError(throwable,
  requestId)`, `setUserId(id)` — `UserRepository` ставит id после логина и `null` при logout.
- **Breadcrumbs**: навигация (`Route`) и сеть (`method path status`, без тел и заголовков
  auth); `X-Request-ID` каждого запроса — breadcrumb + tag `request_id` на событии ошибки
  API; пользователю при ошибке показываются первые 8 символов («код обращения»).
- **Символы — часть релиза**: `mapping.txt` заливает Sentry Gradle-плагин на
  `assembleProdRelease` (Linux), dSYM — `sentry-cli debug-files upload` на Mac; проверяется в
  `release-preflight` до STOP-2.
- **`TelemetrySink` — шов** (`PlatformDeps.telemetry`): `NoopTelemetrySink` в `shared`,
  Sentry-реализация — на платформе; OTel (`otlp`) — только когда у проекта есть коллектор,
  env `OTEL_EXPORTER_OTLP_ENDPOINT`.
- **Пороги качества** — поле `docs/stack/observability.md`: crash-free ≥ 99.5 % / 48 ч,
  ANR < 0.47 %, hang rate iOS < 1 %; проверяются человеком перед промоушеном (STORE-HANDOFF).
- Timber, Kermit, Firebase Crashlytics, `sentry-kotlin-multiplatform` — не подключаются: SDK
  платформ (`sentry-android`, `sentry-cocoa`) инициализируются в `core:platform` /
  `Core/Platform` (`SentryCrashReporter` реализует `TelemetrySink` из `PlatformDeps`).

## Сигналы

- detekt `ForbiddenImport: android.util.Log` → замени на `Logger` из `shared` (NF-01).
- Крэши без стека в GlitchTip → символы не залиты (проверь `SENTRY_AUTH_TOKEN` при релизе).
