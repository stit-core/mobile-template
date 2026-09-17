# Observability

Крэши и ошибки — Sentry SDK обеих платформ (`sentry-android`, `sentry-cocoa`) с приёмником
**GlitchTip self-host** (Sentry-совместимый DSN); логи — свой фасад; OpenTelemetry — шов.
Инварианты для агента — rule `observability`; значения проекта — `docs/stack/observability.md`.

## Sentry / GlitchTip

- **Пустой DSN = SDK не инициализируется** (dev, demo по умолчанию); DSN — build-time
  (`SENTRY_DSN` в `config/env/*.properties` и `iosApp/Config/*.xcconfig`), public by design.
- `environment = APP_ENV`, `release = <applicationId>@X.Y.Z+<code>` (одинаков на обеих
  платформах — общий дашборд), `dist = <code>`.
- Инициализация — в `core:platform` (`SentryCrashReporter`) / `Core/Platform`
  (`SentryCrashReporter.swift`) — реализации `TelemetrySink` из `PlatformDeps`
  (`breadcrumb(category, message)`, `captureError(throwable, requestId)`, `setUserId(id)`);
  `shared` про Sentry не знает, при пустом DSN платформа отдаёт `NoopTelemetrySink`. Только
  **после согласия** пользователя на крэш-репорты (152-ФЗ; флаг в `Settings`).
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
| `shared` / Android | `core/observability/Logger` — `Logger(tag, sink)` с уровнями `Debug/Info/Warn/Error`; `LogSink` даёт платформа (`PlatformDeps.logSink`): debug — `LogcatSink` (`androidMain`), release — sink только в breadcrumbs ≥ warn | `android.util.Log` (кроме `LogcatSink`), `println`, `java.util.logging` (detekt `ForbiddenImport`), Timber, Kermit |
| iOS | `AppFoundation.log` над `os.Logger` (`subsystem:category:`, privacy-аннотации `.private`); для `shared` в debug — `NSLogSink` (`iosMain`) | `print`, `debugPrint`, `NSLog` вне `NSLogSink` (SwiftLint) |

Структурные поля: `request_id`, `screen`, `user_id`. Никогда: токены, cookie, тела ответов, ПДн.

## Символы

| артефакт | как заливается | когда |
|---|---|---|
| `mapping.txt` (R8) | Sentry Gradle-плагин на `assembleProdRelease` (`SENTRY_AUTH_TOKEN`, `SENTRY_ORG`, `SENTRY_PROJECT_*`, `SENTRY_URL`) | `make release-android` |
| dSYM | `sentry-cli debug-files upload --include-sources` на Mac-хосте | iOS-шаг `/release` |

Без символов крэши нечитаемы, а перезалить для уже прилетевших нельзя — `release-preflight`
проверяет до STOP-2. `mapping.txt` дублируется ассетом Release для retrace без Sentry.

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
`config/env/*.properties` / `*.xcconfig` (`SENTRY_DSN`), `androidApp/app/build.gradle.kts`
(`sentry { }` — mapping), `.claude/skills/release/references/steps.md` (dSYM-шаг).
