# Observability

Крэши и ошибки — Sentry SDK обеих платформ (`sentry-android`, `sentry-cocoa`) с
Sentry-совместимым трекером ошибок проекта по `SENTRY_DSN` (GlitchTip или Sentry — выбор проекта в
`docs/stack/observability.md`, дефолт формы — GlitchTip self-host); логи — свой фасад;
OpenTelemetry — шов. Инварианты для агента — rule `observability`.

## Sentry / GlitchTip

- **Пустой DSN = SDK не инициализируется** (dev, demo по умолчанию); DSN — build-time
  (`SENTRY_DSN` в `config/env/*.properties` и `iosApp/Config/*.xcconfig`), public by design.
- `environment = APP_ENV`, `release = <applicationId>@X.Y.Z+<code>` (одинаков на обеих
  платформах — общий дашборд), `dist = <code>`.
- Инициализация — `SentryCrashReporter.init` в `StitApplication.onCreate` (`core:platform`) /
  `SentryCrashReporter(dsn:…)` в `AppEnvironment.init` (`Core/Platform`) — реализации
  `TelemetrySink` из `PlatformDeps` (`breadcrumb(category, message)`, `captureError(throwable,
  requestId)`, `setUserId(id)`); `shared` про Sentry не знает, при пустом DSN Android отдаёт
  `NoopTelemetrySink`. Согласие пользователя на крэш-репорты (152-ФЗ) — шов: экрана согласия и
  флага в `SettingsKeys` нет, DSN в prod включает репорты без него (rule `privacy-and-law`).
- **PII**: `sendDefaultPii = false`, `attachScreenshot = false`, `beforeSend` на платформе режет
  `Authorization`, `Cookie`, email, тела запросов; из `shared` в sink уходит только
  `method path → status [request_id]`; `user.id` — только id (`UserRepository` ставит после
  логина, `null` при logout).
- **Breadcrumbs**: навигация (`Route`), сеть (`method path status request_id`, без тел и auth);
  `X-Request-ID` каждого запроса — tag `request_id` на событии ошибки API; пользователю
  показываются первые 8 символов («код обращения») — корреляция с логами бэкенда (`grep
  request_id`).
- GlitchTip символицирует слабее полного Sentry — символы обязательны; полный Sentry self-host
  — тяжёлый вариант по решению владельца.

## Логи

| платформа | инструмент | что запрещено |
|---|---|---|
| `shared` / Android | `core/observability/Logger` — `Logger(tag, sink)` с уровнями `Debug/Info/Warn/Error`; `LogSink` даёт платформа (`PlatformDeps.logSink`): debug — `LogcatSink` (`androidMain`), release — `SentryLogSink` (`core:platform`, breadcrumbs ≥ warn) | `android.util.Log` (кроме `LogcatSink`), `println`, `java.util.logging` (detekt `ForbiddenImport`), Timber, Kermit |
| iOS | `AppFoundation.AppLogger(category:)` над `OSLogSink` (`os.Logger`, `subsystem` = bundle id); логи `shared` — `SharedLogSink` (`Core/Platform`) в тот же `os.Logger`; `NSLogSink` (`iosMain`) — запасная реализация `LogSink` | `print`, `debugPrint`, `NSLog` (SwiftLint `no_print`/`no_nslog`) |

Структурные поля: `request_id`, `screen`, `user_id`. Никогда: токены, cookie, тела ответов, ПДн.

## Символы

| артефакт | как заливается | когда |
|---|---|---|
| `mapping.txt` (R8) | `make release-android` кладёт в `dist/` (ассет STOP-1); в трекер заливает `sentry-cli upload-proguard` (`SENTRY_AUTH_TOKEN`, `SENTRY_ORG`, `SENTRY_PROJECT_*`, `SENTRY_URL`); Sentry Gradle-плагин не подключён | шаг 2 `/release` |
| dSYM | `sentry-cli debug-files upload --include-sources` на Mac-хосте | iOS-шаг `/release` |

Без символов крэши нечитаемы, а перезалить для уже прилетевших нельзя — `/release` загружает
их до STOP-2 (в `preflight.sh` проверки нет). `mapping.txt` — ассет Release для retrace без Sentry.

## Пороги качества

| метрика | источник | порог (дефолт) | где проверяется |
|---|---|---|---|
| crash-free sessions/users | Sentry release health | ≥ 99.5 % за 48 ч на internal/TestFlight | STORE-HANDOFF перед промоушеном |
| ANR rate (Android) | Sentry ANR, Android vitals | < 0.47 % | там же |
| hang rate (iOS) | `enableAppHangTracking` | < 1 % | там же |
| холодный старт | app-start spans / Macrobenchmark (устройство) | p50 < 1.5 с, p90 < 3 с | релиз-чеклист |
| размер артефакта | `bundletool` | рост ≤ +10 % за релиз | релиз-чеклист |

## TelemetrySink — шов

`core/observability/TelemetrySink` (`PlatformDeps.telemetry`): Sentry на платформе (дефолт) ·
`NoopTelemetrySink` в `shared` (пустой DSN, тесты) · `otlp`. OpenTelemetry на
клиенте включается, когда у проекта есть коллектор и нужен сквозной trace mobile → backend
(`traceparent` уже понимает `httpkit`); env `OTEL_EXPORTER_OTLP_ENDPOINT`. В экосистеме
коллектора нет — не дефолт.

## Где живёт

`docs/stack/observability.md` (приёмник, DSN per env, PII, пороги — значения проекта),
`config/env/*.properties` / `*.xcconfig` (`SENTRY_DSN`), `androidApp/core/platform/…/SentryCrashReporter.kt`,
`iosApp/Packages/Core/Sources/Platform/SentryCrashReporter.swift`, `.claude/skills/release/references/steps.md` (символы).
