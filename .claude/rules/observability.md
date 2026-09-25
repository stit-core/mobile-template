---
name: observability
description: Логи только через Logger + LogSink из shared / AppLogger над os.Logger; пустой DSN = Sentry выключен; PII-скраббинг в beforeSend; breadcrumbs по сети и логам ≥ warn без тел; X-Request-ID в событиях; mapping/dSYM загружает /release; TelemetrySink — шов для OTel. Грузится при правке core/observability, Sentry*, *Logger*, *LogSink*, logging/.
paths:
  - "shared/**/core/observability/**"
  - "androidApp/**/Sentry*"
  - "iosApp/**/Sentry*"
  - "**/*Logger*"
  - "**/*LogSink*"
  - "**/logging/**"
---

# Observability

Полное описание — `docs/reference/observability.md`; значения проекта —
`docs/stack/observability.md`.

## Инварианты

- **Логи — только через `Logger(tag, sink)`** из `core/observability/Logger.kt` в `shared`/Android
  и `AppFoundation.AppLogger` (`OSLogSink` → `os.Logger`) в Swift; `LogSink` даёт платформа
  (`PlatformDeps.logSink`): Android debug — `LogcatSink` (`androidMain`), release — `SentryLogSink`
  (`core:platform`, breadcrumbs уровня ≥ warn); iOS — `SharedLogSink` (`Core/Platform`) в тот же
  `os.Logger`. `android.util.Log` (кроме `LogcatSink`), `println`, `java.util.logging` — detekt
  `ForbiddenImport` (NF-01); `print`/`NSLog` — SwiftLint `no_print`/`no_nslog` (NF-20).
- **Пустой `SENTRY_DSN` = SDK не инициализируется** (dev/demo по умолчанию):
  `SentryCrashReporter.init(context, config, appInfo)` в `StitApplication.onCreate`,
  `SentryCrashReporter(dsn:environment:release:)` в `AppEnvironment.init`; при пустом DSN Android
  отдаёт `NoopTelemetrySink`. `environment = APP_ENV`, `release = <applicationId>@X.Y.Z+<code>`
  (одинаков на обеих платформах), `dist = <code>`. Приёмник — Sentry-совместимый трекер ошибок
  проекта по `SENTRY_DSN` (GlitchTip/Sentry — `docs/stack/observability.md#receiver`).
- **Согласие на крэш-репорты** (rule `privacy-and-law`) — шов: экрана согласия и флага в
  `SettingsKeys` в шаблоне нет, SDK стартует при непустом DSN; проект с DSN в prod заводит
  флаг перед `SentryCrashReporter.init` и строку в `compliance.md`.
- **PII не утекает**: `sendDefaultPii = false`, `attachScreenshot = false`, `beforeSend` на
  платформе режет `Authorization`, `Cookie`, `Set-Cookie` и тело запроса; из `shared` в
  `TelemetrySink` уходят только `breadcrumb(category, message)`, `captureError(throwable,
  requestId)`, `setUserId(id)` — `UserRepository` ставит id после логина и `null` при logout.
- **Breadcrumbs**: сеть (`method path status`, без тел и заголовков auth) и логи ≥ warn;
  `X-Request-ID` каждого запроса — tag `request_id` на событии ошибки API; пользователю при
  ошибке показываются первые 8 символов («код обращения»).
- **Символы — часть релиза**: `mapping.txt` — ассет STOP-1 (`build-android.sh` → `dist/`); в
  трекер его и dSYM заливает `sentry-cli` на шагах `/release` до STOP-2 (Sentry Gradle-плагин
  не подключён; в `preflight.sh` проверки символов нет). Без символов крэши нечитаемы.
- **`TelemetrySink` — шов** (`PlatformDeps.telemetry`): `NoopTelemetrySink` в `shared`,
  Sentry-реализация — на платформе (`core:platform/SentryCrashReporter.kt`,
  `Core/Platform/SentryCrashReporter.swift`; `sentry-android` 8.57, `sentry-cocoa` 9.28); OTel
  (`otlp`) — только когда у проекта есть коллектор, env `OTEL_EXPORTER_OTLP_ENDPOINT`.
- **Пороги качества** — поле `docs/stack/observability.md`: crash-free ≥ 99.5 % / 48 ч,
  ANR < 0.47 %, hang rate iOS < 1 %; проверяются человеком перед промоушеном (STORE-HANDOFF).
- Timber, Kermit, Firebase Crashlytics, `sentry-kotlin-multiplatform` — не подключаются.

## Сигналы

- detekt `ForbiddenImport: android.util.Log` → замени на `Logger` из `shared` (NF-01).
- Крэши без стека в трекере → символы не залиты (проверь `SENTRY_AUTH_TOKEN` при релизе).
