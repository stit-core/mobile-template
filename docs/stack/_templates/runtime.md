---
id: stack.runtime
kind: runtime
name: runtime
status: draft
---

# runtime

## Toolchains

| зона | версия | где пин |
|---|---|---|
| Kotlin / KSP | 2.4.20 / 2.3.12 | `gradle/libs.versions.toml` |
| AGP / android tools | 9.4.0 / 32.4.0 (встроенный Kotlin, без kapt) | `gradle/libs.versions.toml` |
| Gradle / JDK | 9.7.1 (wrapper + sha256) / 21 (Temurin; toolchain 17 для байткода) | `gradle/wrapper`, `build-logic` |
| Compose BOM / Material 3 | 2026.09.00 / 1.4 (без Expressive) | `gradle/libs.versions.toml` |
| Xcode / Swift | 27.0 / 6.4 | `.xcode-version`, `.swift-version` |
| SwiftLint / swift-format | 0.65.1 / из тулчейна Swift | `iosApp/.swiftlint.yml`, `iosApp/.swift-format` |

## Targets

| платформа | значение |
|---|---|
| Android | `minSdk 26` · `targetSdk 37` · `compileSdk 37.2` |
| iOS | deployment target 18.0 · iOS 27 SDK |
| KMP `shared` | `androidLibrary` (host tests) · `jvm` · `iosArm64` · `iosSimulatorArm64`; framework `Shared` (static) |

## Concurrency

Swift 6 language mode, approachable concurrency (`MainActor` по умолчанию), предупреждения =
ошибки; Kotlin — coroutines + `StateFlow`; `DispatcherProvider` (`DefaultDispatchers`, `ioDispatcher` —
expect/actual) инжектируется в `SharedContainer`, тесты подставляют `TestDispatchers`.

## Mac build-host

| поле | значение |
|---|---|
| MAC_HOST | _`user@host` (SSH, ключ без пароля; Tailscale) — или `blocked-by: Mac`_ |
| simulator | _`iosApp/.test-simulator`_ |
| ci dir | `~/ci/<slug>/` (rsync из `scripts/tools/mac.sh`) |

## Update policy

Latest stable с пином и двухнедельной выдержкой; alpha/beta/rc — только строкой в
`dependencies.md#version-policy-exceptions` (решение владельца, триггер снятия). Пары `kotlin`/`ksp`/`hilt`
и `agp`/`androidTools` обновляются вместе; Xcode — с каждым major по дедлайну App Store
(`distribution.md#deadlines`). Обновление — через `/plan`.

## Generates

- `gradle/libs.versions.toml` — версии и SDK-уровни (`compileSdk`, `compileSdkMinor`, `minSdk`, `targetSdk`)
- `gradle.properties` — память/кеши (не трогать без плана)
- `.xcode-version`, `.swift-version`, `iosApp/.test-simulator`
- `MAC_HOST` в `.env.local` (не секрет) — читают `make doctor`, `make check-mac`

## Открытые вопросы

- _что_
