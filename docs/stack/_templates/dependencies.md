---
id: stack.dependencies
kind: dependencies
name: dependencies
status: draft
---

# dependencies

## Libraries

Базовая подборка шаблона (версии — только `gradle/libs.versions.toml` / `Package.swift`).

| зона | библиотека | лицензия | KMP | privacy manifest |
|---|---|---|---|---|
| сеть (`shared`) | Ktor 3.6 (core, content-negotiation, okhttp, darwin, mock) | Apache-2.0 | да | — |
| сериализация, время, файлы | kotlinx-serialization 1.11, kotlinx-coroutines 1.11, okio 3.18 (время — `kotlin.time` из stdlib) | Apache-2.0 | да | — |
| Android UI | Compose BOM 2026.09, Material 3 1.4, Navigation 3 1.1.7, Activity/Lifecycle | Apache-2.0 | — | — |
| DI (Android) | Hilt 2.60.1 + KSP 2.3.12, androidx.hilt 1.4.0 | Apache-2.0 | — | — |
| persistence | DataStore 1.2.1 (`DataStoreSettings`, `KeystoreSecureStore` в `androidMain`) | Apache-2.0 | androidMain | — |
| изображения | Coil 3 (Android), `AsyncImage` (iOS) | Apache-2.0 | — | — |
| тесты | JUnit 4, Robolectric 4.17, Compose test v2, Roborazzi 1.74, Turbine 1.2, okio-fakefilesystem, Konsist 0.17.3, Kover 0.9.9, Swift Testing, swift-snapshot-testing 1.19.5 | MIT/Apache | — | — |
| качество | Spotless 8.10 + ktlint 1.8, detekt 1.23.8 + compose-rules 0.4.28, sort-dependencies, BCV 0.18.2, SwiftLint 0.65.1, swift-format | Apache/MIT | — | — |

## SDKs

Каждый SDK — строка здесь **и** в `compliance.md#sdk-privacy-manifests`.

| SDK | зачем | лицензия | privacy manifest | трансграничка |
|---|---|---|---|---|
| sentry-android 8.x / sentry-cocoa 9.x | crash-репорты (self-host GlitchTip) | MIT | есть | нет |
| RuStore Push SDK · FCM (только транспорт) · APNs | push по `push.md#providers` | _…_ | _проверить_ | _…_ |
| _<новый SDK>_ | _…_ | _…_ | _…_ | _…_ |

Не подключаются: Retrofit, Koin/Metro, Room/SQLDelight (шов), Compose Multiplatform, Swift OpenAPI
Generator, TCA/Combine, Firebase кроме FCM-транспорта, `security-crypto`, `material-icons-*`,
Timber, Kermit (логи — `Logger`/`LogSink` в `shared`), kotlinx-datetime (время — `kotlin.time`),
аналитика/IAP/рекламные SDK по умолчанию.

## Seams

| шов | интерфейс | реализация по умолчанию |
|---|---|---|
| персистентность | `entity/<slug>/cache` (`FileCache<T>`) | in-memory + JSON-снимок; SQLDelight/Room — по спайку |
| карты | `MapSurface` | `none` |
| аналитика | `TelemetrySink` / kind `analytics` | `none` |
| платежи | `Payments` / kind `payments` | `none` |
| feature flags | `AppRequirements.flags` / `flag(name, default)` ← `GET /api/app/requirements` | бэкенд |

## Generates

- `gradle/libs.versions.toml`, `iosApp/Packages/*/Package.swift` (deps)
- строки `compliance.md#sdk-privacy-manifests`
- `dependencies.md` — единственное место решения «подключено / не подключено»

## Открытые вопросы

- _что_
