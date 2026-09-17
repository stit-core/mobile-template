---
name: swift-concurrency
description: Swift 6 language mode и approachable concurrency — MainActor по умолчанию, предупреждения = ошибки, без Combine, Sendable на границе с Shared только в SharedBridge, структурированная отмена Task, Mutex вместо очередей, @preconcurrency только с WHY-комментарием. Грузится при правке .swift и Package.swift в iosApp/.
paths:
  - "iosApp/**/*.swift"
  - "iosApp/**/Package.swift"
---

# Swift concurrency

## Инварианты

- **Одна политика строгости на все таргеты** — цикл `for target in package.targets` в каждом
  `Package.swift`: `.defaultIsolation(MainActor.self)`, `.swiftLanguageMode(.v6)`,
  `.treatAllWarnings(as: .error)`, upcoming features `NonisolatedNonsendingByDefault`,
  `InferIsolatedConformances`, `ExistentialAny`, `InternalImportsByDefault`,
  `MemberImportVisibility`; в `Config/Base.xcconfig` — `SWIFT_DEFAULT_ACTOR_ISOLATION = MainActor`,
  `SWIFT_APPROACHABLE_CONCURRENCY = YES`, `SWIFT_STRICT_CONCURRENCY = complete`, warnings = errors.
- **Всё на `MainActor` по умолчанию.** Уход с главного актора — `nonisolated` с однострочным
  WHY: утилиты `Pure` (их зовут из `deinit` и callback'ов Kotlin), реализации `PlatformDeps` в
  `Core/Platform` (`ApnsRegistrar`, `NetworkConnectivity`, `SharedLogSink`, …), `IosPlatformDeps`.
  `@concurrent`/`Task.detached` — только с WHY.
- **`@preconcurrency` — только с комментарием-причиной** в той же строке (какая библиотека, какой
  тип не `Sendable`); SwiftLint `preconcurrency_needs_why` (NF-24).
- **Combine запрещён** (`import Combine` — SwiftLint `no_combine`). Потоки — `AsyncSequence`/
  `AsyncStream`: `KotlinStateFlow.stream(of:)` (`SharedBridge/FlowBridge.swift`, коллектор через
  Obj-C-экспорт `Kotlinx_coroutines_coreFlowCollector`, без SKIE); `PresenterModel` подписывается
  в `init` задачей внутри `Mutex<Task?>` и отменяет её в `deinit`.
- **`Sendable` на границе с `Shared`** — только в `Core/SharedBridge/SendableStates.swift`
  (`extension XState: @retroactive @unchecked Sendable` для immutable data class'ов и
  `ConnectivityState`); в фичах `@unchecked Sendable` — нет. `MemberImportVisibility`: модуль,
  чьи члены используются, импортируется явно (`import UIKit` рядом с `UIColor`).
- **Структурированная отмена.** `Task { }` живёт в `.task {}` или в модели с отменой при `deinit`;
  состояние, пересекающее акторы и `deinit`, — `Mutex` из `Synchronization` (`ApnsRegistrar`,
  `NotificationDeduplicator`, `PresenterModel`), не `DispatchQueue`/семафоры;
  `withCheckedContinuation` — только на границе с callback-API платформы (`ApnsRegistrar.token()`).
- **Kotlin `suspend fun …: Boolean`** приходит боксированным `KotlinBoolean`
  (`NotificationCenterPermission`), `Instant` — `KotlinInstant.date` (`KotlinInterop.swift`);
  ошибки `NSError` с `userInfo["KotlinException"]` → `AppError` (`SharedBridge/AppError.swift`).
- **Тесты** — Swift Testing; suite с общим состоянием — `.serialized`; ожидание асинхронного
  состояния — `waitUntil` (`TestSupport`), не `sleep`; `XCTAssert*` внутри `@Test` не смешивать.

## Где это проверяется

SwiftLint `custom_rules` (`iosApp/.swiftlint.yml`) — на Linux; изоляция и `Sendable` —
компилятором на Mac (`make check-mac`; на Linux — только `Packages/Pure` и `Tools/archlint`).
Ловушки платформы — `docs/reference/architecture.md#traps`.
