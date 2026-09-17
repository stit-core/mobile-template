---
name: swift-concurrency
description: Swift 6 language mode и approachable concurrency — MainActor по умолчанию, предупреждения = ошибки, без Combine, Sendable на границе с Shared, структурированная отмена Task, @preconcurrency только с WHY-комментарием. Грузится при правке .swift и Package.swift в iosApp/.
paths:
  - "iosApp/**/*.swift"
  - "iosApp/**/Package.swift"
---

# Swift concurrency

## Инварианты

- **Swift 6 language mode** во всех пакетах (`swiftLanguageModes: [.v6]`) + approachable
  concurrency: `SWIFT_DEFAULT_ACTOR_ISOLATION = MainActor`,
  `SWIFT_APPROACHABLE_CONCURRENCY = YES`; upcoming features включены в `Package.swift`.
  Предупреждения изоляции = ошибки; `-warnings-as-errors` в гейте.
- **Всё на `MainActor` по умолчанию.** Уход с главного актора — `@concurrent` /
  `nonisolated` с однострочным WHY-комментарием; без обоснования — нет.
- **`@preconcurrency` — только с комментарием-причиной** (какая библиотека и какой тип не
  `Sendable`); SwiftLint ловит `@preconcurrency` без комментария.
- **Combine запрещён** (`import Combine` — SwiftLint). Потоки — `AsyncSequence`/`AsyncStream`;
  подписка на `StateFlow` из `Shared` — `for await` в `Task` внутри `.task {}` модификатора.
- **`Sendable` на границе с `Shared`**: типы, пересекающие акторы, — `struct`/`enum` или
  `final class` с `Sendable`; классы Kotlin оборачиваются в `Sendable`-адаптеры
  `Core/SharedBridge`, а не помечаются `@unchecked Sendable` в фичах.
- **Структурированная отмена.** `Task { }` живёт в `.task {}` или в модели с явным
  `cancel()` при `deinit`/исчезновении; `Task.detached` — только с WHY.
- **Без `DispatchQueue`/семафоров** для синхронизации; `withCheckedContinuation` — только
  на границе с callback-API платформы (`Core/Platform`).
- **Тесты** — Swift Testing; suite с общим состоянием — `.serialized`; `XCTAssert*` внутри
  `@Test` не смешивать.

## Где это проверяется

SwiftLint `custom_rules` (`iosApp/.swiftlint.yml`) — на Linux; изоляция и `Sendable` —
компилятором на Mac (`make check-mac`). Ловушки платформы —
`docs/reference/architecture.md#traps`.
