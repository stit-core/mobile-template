# 02 — Канон iOS 2026: стек, архитектура, структура, тесты, качество, тонкости

> Дата ресёрча: **2026-09-17**. Поток 02 мобильного ресёрча (см. `brief.md`).
> **Ограничение окружения: macOS/Xcode на машине нет.** Всё, что требует Xcode, симулятора
> или подписи, проверено **только по документации** — так и помечено. Эмпирически на этой
> машине проверяемы лишь pure-Swift вещи, и то при установленном тулчейне (его тоже нет —
> см. поток 06).
> Каждый факт помечен: **[✓ URL, дата]** — проверено по первоисточнику; **[~]** — вторичный
> источник / отраслевой консенсус; **[?]** — предположение автора отчёта.

---

## TL;DR

1. **Платформа сдвинулась 14 сентября 2026**: вышли **Xcode 27 (27A266a)**, **iOS 27.0
   (24A437)**, **Swift 6.4** (15.09.2026). Xcode 27 требует **macOS Tahoe 26.6+** и **Apple
   silicon**; on-device debugging — iOS 17+. Бета-поезд уже на **Xcode 27.2 beta** (16.09.2026).
   [✓ developer.apple.com/news/releases, 2026-09-17]
2. **Дедлайн, который определяет планирование шаблона: с апреля 2027** приложения, загружаемые
   в App Store Connect, должны собираться **iOS 27 SDK и новее**. Сейчас действующий минимум —
   Xcode 26 / iOS 26 SDK (с 28.04.2026).
   [✓ developer.apple.com/news/?id=k1mtkt1k, 2026-09-09; developer.apple.com/news/upcoming-requirements/]
3. **Liquid Glass больше не опционален**: ключ `UIDesignRequiresCompatibility` **игнорируется
   системой при сборке под iOS 27 / iPadOS 27 / macOS 27 / tvOS 27 и новее**. Т. е. с апреля
   2027 (SDK-требование) escape-hatch исчезает де-факто для всех.
   [✓ developer.apple.com/documentation/bundleresources/information-property-list/uidesignrequirescompatibility]
4. **Язык: Swift 6 language mode + «approachable concurrency» — это и есть канон 2026.** Новый
   проект в Xcode 26/27 по умолчанию получает `SWIFT_DEFAULT_ACTOR_ISOLATION = MainActor` и
   `SWIFT_APPROACHABLE_CONCURRENCY = YES` (последний включает `NonisolatedNonsendingByDefault`,
   `InferIsolatedConformances`, `InferSendableFromCaptures`, `GlobalActorIsolatedTypesUsability`,
   `DisableOutwardActorInference`). Практический смысл: **приложение снова однопоточное
   по умолчанию**, уход с main actor — осознанный (`@concurrent`). [~ SwiftLee/Donny Wals/useyourloaf]
5. **Архитектура-канон для бизнес-приложения: SwiftUI + `@Observable` state-объекты +
   протокольные зависимости через `Environment`/явный конструктор + `NavigationStack` с
   типизированным путём.** Apple не предписывает «MVVM», её документация описывает
   `@Observable`-модель данных и вью, которая трекает только читаемые свойства
   [✓ developer.apple.com/documentation/swiftui/managing-model-data-in-your-app]. Отрасль
   называет это «MV / MVVM-lite»; спор MVVM-vs-MV в 2026 — **терминологический**, не
   архитектурный.
6. **TCA (pointfreeco/swift-composable-architecture 1.26.2, MIT, ~14.9k★)** — живой и
   качественный, но **дорогой**: макросы кратно удлиняют компиляцию, кривая обучения высокая,
   и он навязывает свою вселенную. **Для шаблона-фундамента под бизнес-CRUD — не брать**;
   точечно оправдан там, где состояние — настоящий конечный автомат.
   [✓ github.com/pointfreeco/swift-composable-architecture/releases; ~ discussions #2776]
7. **Модульность: тонкий app-таргет + локальные SwiftPM-пакеты.** Это дефолт 2026 и он
   **убирает главную боль `.pbxproj`** (Xcode 16+ `PBXFileSystemSynchronizedRootGroup` —
   «buildable folders» — добивает остаток). **Tuist/XcodeGen для нашего профиля — избыточны**:
   XcodeGen (2.46.0, MIT) — валидный «страховочный» вариант, Tuist CLI MIT, но его ценность
   (cache/selective testing/insights) в платном облаке — это ровно то, чего экосистема stit
   избегает. [~ Tuist blog, JET/Qonto инженерные блоги; ✓ github licenses]
8. **Тесты: Swift Testing — дефолт, XCTest остаётся для UI-тестов и performance.** В Swift 6.4
   добавлен управляемый interop-режим (`SWIFT_TESTING_XCTEST_INTEROP_MODE`, дефолт `complete`
   при `swift-tools-version >= 6.4`) — можно мигрировать постепенно.
   [✓ developer.apple.com/documentation/testing/migratingfromxctest]
9. **Ключ к агентному циклу: делить код на «pure-Swift» и «UI/Apple-SDK».** Pure-Swift
   (domain, сеть, мапперы, схемы) собирается и тестируется `swift build`/`swift test` **на
   Linux** — быстрый детерминированный цикл для агента. Всё, что тянет SwiftUI/UIKit,
   требует Mac + симулятор. Это **главный архитектурный драйвер** структуры шаблона.
10. **Линт/формат: `swiftlint --strict` (0.65.1, MIT, есть Linux-бинарь) + `swift format lint
    --strict` (в тулчейне Swift 6+, Apache-2.0).** SwiftFormat (nicklockwood, 0.63.0) мощнее,
    но это **третий** инструмент; для агентного гейта важнее «один форматтер, ноль споров» —
    берём тот, что в тулчейне.
11. **Periphery (dead code) с 12.08.2026 архивирован и стал коммерческим продуктом**
    (periphery.pro). Последний OSS-релиз — **3.8.0 (25.07.2026, MIT)**, форк разрешён; бесплатно
    для indie/hobby и OSS любого размера. Для шаблона-в-открытом-коде это **риск и предмет
    решения владельца**. [~ periphery.pro / GitHub archive notice]
12. **Архитектурный линтер уровня Steiger/depguard в Swift не существует готовым.** Индустрия
    решает это **самописным валидатором графа зависимостей** (эталон: Just Eat Takeaway —
    `ModularArchitectureValidator`, Swift CLI, гоняется на каждом PR, матрица «кто на кого
    может зависеть», запрет циклов, запрет Feature→Feature). Для шаблона это нужно **написать
    самим** — ~200 строк Swift/Python поверх `Package.swift`/манифеста модулей.
    [~ albertodebortoli.com/2026/07/15/revisiting-the-jet-ios-modular-architecture-in-2026/]
13. **Deployment target для нового бизнес-приложения: iOS 18.0** (консервативно) или **iOS 26**
    (если заказчик корпоративный и парк управляемый). Apple публикует только долю последней
    мажорной: **iOS 26 — 86 %** устройств, выпущенных за последние 4 года, и **79 %** всех
    устройств (замер 07.06.2026). iOS 17 как минимум даёт Observation и `NavigationStack`,
    но в 2026 это уже «легаси-хвост».
    [✓ developer.apple.com/support/app-store/, замер 2026-06-07]
14. **Данные: для профиля «offline-tolerant бизнес-CRUD» никакая ORM не нужна.** Канон —
    `URLSession` + `Codable` + in-memory кэш + (опц.) файловый кэш. SwiftData — «дефолт для
    новых приложений» в блогах, но в проде остаются боли миграций и CloudKit; Core Data —
    тяжёл; GRDB (7.11.1, MIT) — лучший выбор, **если** реально нужен SQLite.
15. **Главные болевые точки платформы для AI-агента**: недетерминизм симулятора, шум
    `xcodebuild`, минуты на сборку, необходимость Mac, code signing, дрейф snapshot между
    версиями iOS, и то, что **половина гейта физически не запускается на Linux**. Шаблон
    должен это принять как данность и максимизировать «Linux-зелёную» часть.

---

## Findings

### §1. Версии и платформа (состояние на 2026-09-17)

#### 1.1. Xcode / Swift / iOS

| Что | Версия | Дата | Источник |
|---|---|---|---|
| Xcode | **27 (27A266a)** | 2026-09-14 | [✓ developer.apple.com/news/releases/] |
| Xcode (бета-поезд) | 27.2 beta (27B5019j) | 2026-09-16 | [✓ там же] |
| iOS / iPadOS | **27.0 (24A437)** | 2026-09-14 | [✓ там же] |
| iOS (бета) | 27.2 beta (24B5084k) | 2026-09-16 | [✓ там же] |
| Swift | **6.4** | 2026-09-15 | [✓ swift.org/blog/swift-6.4-released/] |
| Swift (предыдущий) | 6.3 | 2026-03-24 | [✓ swift.org/blog] |
| Требование Xcode 27 к хосту | **macOS Tahoe 26.6+**, Apple silicon | — | [✓ Xcode 27 Release Notes, Overview] |
| on-device debugging | iOS 17+, tvOS 17+, watchOS 10+, visionOS | — | [✓ там же] |

Прямая цитата из Overview релиз-ноутов (проверено):

> Xcode 27 includes Swift 6.4 and SDKs for iOS 27, iPadOS 27, tvOS 27, watchOS 27, macOS 27,
> and visionOS 27. Xcode 27 supports on-device debugging in iOS 17 and later… Xcode 27
> requires a Mac running macOS Tahoe 26.6 or later.
> [✓ developer.apple.com/documentation/xcode-release-notes/xcode-27-release-notes]

**Важное для планирования инфраструктуры:** macOS 26 — последний релиз с поддержкой Intel-маков
и Rosetta; **macOS 27 — только Apple silicon**
[✓ developer.apple.com/news/?id=k1mtkt1k, 2026-09-09]. То есть любая машина/мак-мини/арендованный
Mac под CI **обязан быть Apple silicon** и обновляться до 26.6+/27.

#### 1.2. SDK-требования App Store (жёсткие даты)

| Требование | С какой даты | Статус |
|---|---|---|
| сборка Xcode 26 / iOS 26 SDK+ | **28.04.2026** | действует [✓ upcoming-requirements] |
| сборка **iOS 27 SDK+** | **апрель 2027** | объявлено [✓ news/?id=k1mtkt1k] |
| валидный `PrivacyInfo.xcprivacy` для списка популярных сторонних SDK | 12.02.2025 | действует [✓ Apple docs] |
| описание required-reason API в privacy manifest | 01.05.2024 | действует [✓ Apple docs] |

> **[?] Вывод для шаблона:** шаблон должен собираться **iOS 27 SDK** (Xcode 27) уже сейчас —
> иначе к весне 2027 форки окажутся в миграции. Deployment target при этом отдельная величина
> (см. 1.6).

#### 1.3. Swift 6 language mode и «approachable concurrency»

Swift 6 language mode (строгая проверка data-race на этапе компиляции) — база с 2024 г.
Реальный сдвиг произошёл в **Swift 6.2** (Xcode 26) под зонтиком **approachable concurrency**:

| Proposal | Что даёт | Флаг upcoming feature |
|---|---|---|
| **SE-0466** Control default actor isolation inference | код по умолчанию изолирован на указанный глобальный актор → «однопоточная программа» снова норма | build setting `SWIFT_DEFAULT_ACTOR_ISOLATION` / флаг `-default-isolation MainActor` / `.defaultIsolation(MainActor.self)` в Package.swift |
| **SE-0461** Run nonisolated async functions on the caller's actor by default | `nonisolated(nonsending)` — async-функция исполняется на акторе вызывающего; `@concurrent` — явный уход в пул | `NonisolatedNonsendingByDefault` |
| **SE-0470** Global-actor isolated conformances | conformance можно ограничить глобальным актором | `InferIsolatedConformances` |
| **SE-0472** Starting tasks synchronously | `Task.immediate` — задача стартует синхронно, если может | — |

`SWIFT_APPROACHABLE_CONCURRENCY = YES` включает набор: `DisableOutwardActorInference`,
`GlobalActorIsolatedTypesUsability`, `InferIsolatedConformances`, `InferSendableFromCaptures`,
`NonisolatedNonsendingByDefault`. **Новый проект Xcode 26 создаётся с
`SWIFT_DEFAULT_ACTOR_ISOLATION = MainActor` и `SWIFT_APPROACHABLE_CONCURRENCY = YES`**;
существующие проекты при апгрейде получают `nonisolated`.
[~ avanderlee.com/concurrency/swift-6-2-concurrency-changes/, donnywals.com/setting-default-actor-isolation-in-xcode-26/, useyourloaf.com/blog/approachable-concurrency-in-swift-packages/]

Эквивалент для SwiftPM-пакета (`swift-tools-version: 6.2+`):

```swift
.target(
  name: "MyFeature",
  swiftSettings: [
    .defaultIsolation(MainActor.self),                        // SE-0466
    .enableUpcomingFeature("NonisolatedNonsendingByDefault"), // SE-0461
    .enableUpcomingFeature("InferIsolatedConformances"),      // SE-0470
  ]
)
```
[~ useyourloaf.com, листинг воспроизведён]

> **[?] Инвариант для шаблона:** `-default-isolation MainActor` включается **везде** (и в
> app-таргете, и в каждом локальном пакете). Правило кода: «всё на MainActor, уход в
> `@concurrent`/`nonisolated` — только с обоснованием в комментарии». Это резко снижает число
> ошибок data-race, с которыми агент не справляется без запуска компилятора.

**Ловушка (проверено):** в Xcode 27 известен баг — «A computed property with both an `init`
accessor and an array/dictionary literal initial value will no longer compile if the getter is
declared before the `init` accessor» (source break из SE-0508, radar 180969028); обходной путь —
объявить `init` accessor раньше геттера.
[✓ Xcode 27 Release Notes → Swift Compiler → Known Issues]

**Ловушка (по документации):** Swift dependency scanner в Xcode 27 оптимизирован и теперь
**требует уникальности имён Clang-модулей** в пределах одного скана — вендоренные `module.modulemap`,
переобъявляющие SDK-модуль, начинают падать ошибкой (radar 136303612).
[✓ Xcode 27 Release Notes → Swift Compiler → New Features]

#### 1.4. Observation, SwiftUI-first, где ещё нужен UIKit

- **Observation (`@Observable`) — iOS 17+**, это официальный механизм связи модели и вью.
  Apple явно пишет: вью формирует зависимость, только **читая свойство** в `body`; изменения
  непрочитанных свойств вью не трогают.
  [✓ developer.apple.com/documentation/swiftui/managing-model-data-in-your-app]
- Apple также предупреждает: **не конформить `Observable` вручную** — только макрос `@Observable`.
  [✓ там же]
- **SwiftUI-first — дефолт для нового бизнес-приложения.** UIKit остаётся нужен точечно:
  тонкая настройка `UITextView`/ввода, кастомные интерактивные переходы, некоторые
  camera/AVFoundation-сценарии, `UIPageViewController`-подобные вещи, а также интеграция
  legacy-экранов. Мост — `UIViewRepresentable` / `UIViewControllerRepresentable` и обратно
  `UIHostingController`. [~ отраслевой консенсус; Apple не публикует «списка исключений»]
- В Xcode 27 `#Preview` **явно исполняется на main actor** — можно вызывать main-actor API без
  предупреждений (radar 177394870). [✓ Xcode 27 Release Notes → Previews → Resolved Issues]

#### 1.5. Liquid Glass — что это значит для шаблона

Факты (проверено по Apple):

1. `UIDesignRequiresCompatibility` — «temporarily use this key while reviewing and refining your
   app's UI». **«The system ignores this key when you build for iOS 27 or later, iPadOS 27 or
   later, Mac Catalyst 27 or later, macOS 27 or later, or tvOS 27 or later.»**
   [✓ developer.apple.com/documentation/bundleresources/information-property-list/uidesignrequirescompatibility]
2. «If your app uses standard components from SwiftUI, UIKit, or AppKit, your interface picks up
   the latest look and feel… build your app with the latest SDKs».
   [✓ developer.apple.com/documentation/technologyoverviews/adopting-liquid-glass]
3. Apple прямо требует: **«Reduce your use of custom backgrounds in controls and navigation
   elements»** — кастомные фоны в split views, tab bars, toolbars конфликтуют с материалом и
   scroll edge effect. [✓ там же]
4. Кастомные элементы: `.glassEffect(...)`, `GlassEffectContainer`, `glassEffectID` (морфинг).
   API намеренно маленький; «Avoid overusing Liquid Glass effects… Limit these effects to the
   most important functional elements». [✓ Apple; ~ обзоры API]
5. Эталонный сэмпл Apple — **Landmarks** («Landmarks: Building an app with Liquid Glass»).
   [✓ developer.apple.com/documentation/technologyoverviews/liquid-glass → Sample code]

> **[?] Следствие для «design-system-agnostic токен-слоя»** (аналог нашего
> `app/styles/index.css` в SPA): на iOS токен-слой **не может** быть тотальным, как в вебе.
> Навигация/бары/контролы принадлежат системе, и перекрашивать их — прямое нарушение
> рекомендаций Apple и риск визуальных артефактов. Токенизируется то, что **наше**: контентные
> цвета (asset catalog с Any/Dark), типографика (обёртки над `Font` + Dynamic Type), spacing,
> радиусы контентных карточек, семантические цвета статусов. Это **уже** и есть правильная
> граница — она совпадает с Apple-гайдлайном.

#### 1.6. Deployment target

Единственные официальные цифры — со страницы Apple (замер **07.06.2026**):

| Платформа | Устройства последних 4 лет | Все устройства |
|---|---|---|
| iOS 26 на iPhone | **86 %** | **79 %** |
| iPadOS 26 на iPad | 79 % | 68 % |

[✓ developer.apple.com/support/app-store/]

На 2026-09-17 Apple ещё не публикует долю iOS 27 (релиз 3 дня назад).

**Практика рынка 2026** (вторичные данные, но сходятся): Element X iOS — **iOS 18.5**
[✓ project.yml, `deploymentTarget: iOS: '18.5'`]; крупные консьюмерские приложения обычно
держат N-2 (iOS 25/26 при iOS 27), корпоративные — N-1.

> **[?] Рекомендация для stit-шаблона:** дефолт **iOS 18.0**, с явным комментарием, что для
> корпоративного заказчика с управляемым парком поднимается до iOS 26. Ниже iOS 17 не
> опускаться никогда (там нет Observation). Само число вынести в `xcconfig`
> (`IPHONEOS_DEPLOYMENT_TARGET`) + `Package.swift` `platforms:` — одна точка правки.

#### 1.7. SwiftPM: состояние

- **Swift 6.4: «Swift Build is now the default in Swift Package Manager»** — единая система
  сборки для Linux/macOS/Windows. [✓ swift.org/blog/swift-6.4-released/]
- **Traits** — `swift package show-traits` появился в 6.3; фича стабилизировалась, но в
  прикладном iOS-приложении нужна редко (это инструмент авторов библиотек).
  [✓ swift.org/blog, Swift 6.3]
- `swift test` в Xcode 27: суммаризация падений; `--maximum-repetitions` + `--repeat-until
  [pass|fail]` — встроенная борьба с flaky-тестами.
  [✓ Xcode 27 Release Notes → Swift Package Manager]
- **Swift SDK для Android** — первый официальный релиз в Swift 6.3, в 6.4 собран с NDK 30.
  Это факт для потока 03, не для этого. [✓ swift.org/blog]

#### 1.8. Реестр библиотек и инструментов (проверено по GitHub, 2026-09-17)

| Пакет / инструмент | Версия | Дата релиза | Лицензия | ★ | Замечание |
|---|---|---|---|---|---|
| `pointfreeco/swift-composable-architecture` | 1.26.2 | 2026-08-28 | MIT | 14 915 | цена: макросы + компиляция |
| `pointfreeco/swift-dependencies` | 1.17.1 | 2026-08-28 | MIT | 2 188 | DI без фреймворка-вселенной |
| `pointfreeco/swift-navigation` | 2.11.2 | 2026-08-31 | MIT | 2 289 | state-driven навигация |
| `pointfreeco/swift-snapshot-testing` | 1.19.5 | 2026-09-15 | MIT | 4 336 | де-факто стандарт snapshot |
| `pointfreeco/swift-sharing` | 2.10.1 | 2026-08-31 | MIT | 799 | `@Shared` persistence |
| `nalexn/ViewInspector` | 0.10.3 | 2025-09-21 (push 2026-09-13) | MIT | 2 631 | релизы редкие, разработка идёт |
| `realm/SwiftLint` | **0.65.1** | 2026-08-21 | MIT | 19 735 | есть Linux-бинарь |
| `swiftlang/swift-format` | **604.0.0** | 2026-09-16 | Apache-2.0 | 2 956 | в тулчейне Swift 6+ (`swift format`) |
| `nicklockwood/SwiftFormat` | 0.63.0 | 2026-08-30 | MIT | 8 932 | мощнее, но третий инструмент |
| `peripheryapp/periphery` | 3.8.0 | 2026-07-25 | MIT | 6 183 | **репо архивирован 2026-08-12**, продукт коммерциализован |
| `tuist/tuist` | monorepo (tag `helm@0.125.0`) | 2026-09-16 | MIT (`cli/`), MPL-2.0 (`server/`,`kura/`,`atlas/`) | 5 804 | CLI OSS, ценность — в платном облаке |
| `yonaskolb/XcodeGen` | 2.46.0 | 2026-07-16 | MIT | 8 784 | живой, простой |
| `XcodesOrg/xcodes` | 2.1.0 | 2026-09-14 | MIT | 4 804 | пиннинг версии Xcode |
| `fastlane/fastlane` | 2.240.1 | 2026-09-15 | MIT | 42 117 | Ruby; жив и активен |
| `cpisciotta/xcbeautify` | 3.2.1 | 2026-04-04 | MIT | 1 537 | Swift-бинарь, **macOS + Linux**, JUnit |
| `a7ex/xcresultparser` | 2.2.0 | 2026-07-18 | MIT | 150 | `.xcresult` → JUnit/Cobertura/SonarQube |
| `mobile-dev-inc/maestro` | cli-2.10.0 | 2026-08-31 | Apache-2.0 | 15 648 | YAML e2e; **iOS — только симулятор** |
| `apple/swift-openapi-generator` | 1.13.1 | 2026-09-01 | Apache-2.0 | — | SSWG incubating |
| `apple/swift-openapi-runtime` | 1.12.1 | 2026-09-02 | Apache-2.0 | — | |
| `apple/swift-openapi-urlsession` | 1.3.1 | 2026-06-23 | Apache-2.0 | — | транспорт на URLSession |
| `groue/GRDB.swift` | v7.11.1 | 2026-06-30 | MIT | — | SQLite, если реально нужен |
| `stephencelis/SQLite.swift` | 0.16.0 | 2026-03-08 | MIT | — | реже обновляется |
| `hmlongco/Factory` | 3.4.0 | 2026-09-16 | MIT | — | лёгкий DI-контейнер |
| `Swinject/Swinject` | v2.10.0 | 2025-09-01 | MIT | — | **год без релиза** |
| `uber/needle` | v0.25.1 | 2024-09-18 | Apache-2.0 | — | **2 года без релиза → не брать** |
| `Kolos65/Mockable` | 0.6.4 | 2026-06-23 | MIT | — | макро-моки, 0.x |
| `uber/mockolo` | 2.6.1 | 2026-03-24 | Apache-2.0 | — | кодоген-моки |
| `getsentry/sentry-cocoa` | 9.28.0 | 2026-09-10 | MIT | — | self-host Sentry ок |
| `apple/swift-log` | 1.15.1 | 2026-09-08 | Apache-2.0 | — | на iOS обычно не нужен (есть `OSLog`) |

(Даты релизов — из GitHub releases feed, снято 2026-09-17.)


---

### §2. Архитектура — что реально рекомендуется в 2026

#### 2.1. Что говорит сама Apple

Apple **не публикует предписанной архитектуры приложения**. Она публикует правила data flow, и
они однозначны:

- Модель данных — «custom type that represents the data… provides separation between the data
  and the views… promotes modularity, improves testability»;
- пометить её `@Observable`;
- вью читает свойства напрямую; зависимость трекается **по факту чтения в `body`**;
- делить объект между экранами — через `Environment` (`@Environment(Library.self)`);
- локальное владение — через `@State`.
  [✓ developer.apple.com/documentation/swiftui/managing-model-data-in-your-app]

Это ровно то, что сообщество называет **MV** (Model–View) или «MVVM без отдельного слоя
ViewModel». Apple-сэмплы (Landmarks и др.) написаны именно так.

#### 2.2. MVVM с `@Observable`

Отраслевой дефолт 2026 — `@Observable`-класс, владеющий состоянием экрана и асинхронной
работой, вью держит его в `@State` (владение) или получает через `Environment`/init
(разделяемое). [~ множественные отраслевые источники; см. Источники]

Спор «MV vs MVVM» в 2026 — **терминологический**. Обе стороны пишут один и тот же код:
`@Observable`-объект + SwiftUI-вью. Разница только в том, называть ли объект `ViewModel` и
заводить ли его **на каждый** экран. Практичный компромисс и **рекомендация этого отчёта**:

> Экран без асинхронной работы и без ветвящегося состояния — просто `View` + `@State`.
> Экран с загрузкой/формой/валидацией — `@Observable final class <Screen>Model` рядом с вью.
> Не заводить «ViewModel-на-всё» ради симметрии.

#### 2.3. TCA — когда оправдан, когда нет

**Плюсы** (проверяемые): единый `Store`/`Reducer`, полностью тестируемое состояние, `TestStore`
с точной проверкой каждого перехода, exhaustive-тесты, зрелая экосистема
(`swift-dependencies`, `swift-navigation`, `swift-sharing`, `swift-case-paths`), живой релизный
цикл (1.26.2, 2026-08-28), MIT, ~14.9k★.

**Минусы**:
- **Время компиляции.** Макросы TCA — известная и обсуждаемая проблема: в тредах сообщества
  фигурируют кратные замедления сборки, `ComposableArchitectureMacros` съедает ядра
  [~ github.com/pointfreeco/swift-composable-architecture/discussions/2776].
- **Кривая обучения** и «всё или ничего»: TCA задаёт форму всего приложения.
- **Найм.** Для РФ-рынка бизнес-разработки TCA — нишевой навык.
- Для CRUD-экранов (список → деталь → форма) выгода от exhaustive reducer-тестов мала.

> **[?] Вывод:** в шаблон-фундамент **не брать**. Но взять у Point-Free **отдельные**
> библиотеки, которые не требуют TCA: `swift-dependencies` (DI + тестовые подмены) и
> `swift-snapshot-testing`. `swift-navigation` — опционально (см. 2.5).
>
> Эталон «как выглядит TCA-приложение целиком» — **isowords** (Point-Free): SwiftPM-монорепо,
> ~40 модулей, `platforms: .iOS(.v17)`, TCA + swift-dependencies + snapshot-testing, сервер
> тоже на Swift [✓ github.com/pointfreeco/isowords/blob/main/Package.swift].

#### 2.4. Clean / VIPER

VIPER и «классический» Clean с `Interactor`/`Presenter`/`Router`/`Entity` на каждый экран —
**устарели для SwiftUI**: они родились как ответ на массивные `UIViewController`, которых в
SwiftUI просто нет. То, что осталось живым из этой линии — **Coordinator для навигации** и
**выделение слоя use-case/repository** там, где логика реально сложная. [~ консенсус]

**Однако** оговорка: крупные продукты живут на «VIPER-подобных» схемах, названных иначе. Живой
пример — **Bitwarden iOS** (Swift/SwiftUI-переписка): `Coordinator` (только навигация) +
`Processor` (единственный источник мутаций состояния) + `State` + `Action` (синхронные
интеракции) + `Effect` (асинхронные) + `Store` (мост процессор↔вью) + `View` (никогда не мутирует
состояние напрямую). Навигация — enum-`Route` и `navigate(to:context:)`.
[✓ contributing.bitwarden.com/architecture/mobile-clients/ios/]

Это фактически **unidirectional data flow без TCA** — «Store + Reducer руками». Очень близко к
тому, что нужно нашему шаблону, и без макро-налога.

#### 2.5. Навигация и координаторы в SwiftUI

Базис — `NavigationStack` + типизированный путь:

```swift
@Observable final class AppRouter {
  var path: [Route] = []                 // или NavigationPath для гетерогенного пути
  var sheet: SheetRoute?
  func open(_ route: Route) { path.append(route) }
  func reset() { path.removeAll() }
}

enum Route: Hashable { case todoDetail(Todo.ID), profile, settings }
```

- `NavigationSplitView` — обязателен для iPad/Mac (двух-/трёхколоночный layout).
- Deep link превращается в **значение пути**, а не в императивный вызов: `AASA` → `URL` →
  `Route` → `router.path = [...]`. Это единственный способ сделать deep-link тестируемым без
  симулятора (`URL → [Route]` — чистая функция, тестируется на Linux).
- `swift-navigation` (pointfreeco, 2.11.2, MIT) даёт state-driven биндинги
  (`sheet(item:)`-подобные хелперы, `@Presents`) и работает **без TCA**; берётся, если ручной
  роутер начинает разрастаться. [✓ github.com/pointfreeco/swift-navigation]

**Боли (по документации и отчётам сообщества, [~]):**
- `NavigationStack` не любит программную модификацию пути «в середине» анимации; надёжно —
  менять путь целиком одним присваиванием;
- `sheet`/`fullScreenCover` не стекируются: попытка показать второй sheet поверх первого
  требует закрыть первый (или вложить `sheet` в контент первого);
- восстановление пути после холодного старта по deep link требует, чтобы root-вью уже был
  смонтирован — иначе путь «съедается»;
- `NavigationLink(value:)` требует `.navigationDestination(for:)` **в пределах того же
  NavigationStack**, иначе — молчаливое ничего.

#### 2.6. async/await vs Combine

- **Канон 2026 — `async/await` + `AsyncSequence`/`AsyncStream`.** Все новые Apple API
  асинхронные (`URLSession.data(for:)`, `for await` над нотификациями и т. п.).
- **Combine не deprecated формально, но заморожен**: новых API нет, Apple его не развивает,
  в новых фреймворках Combine-интерфейсов не появляется. [~ консенсус; Apple явного deprecation
  не объявляла]
- Практика: Combine остаётся в проектах ради `@Published`/`debounce`. В новом коде
  `debounce`/`throttle` берутся из `swift-async-algorithms` (Apple, `AsyncAlgorithms`) или
  делаются вручную на `Task` + `Clock`.

> **[?] Инвариант для шаблона:** Combine **не использовать вообще**. Правило в
> `.claude/rules/swift-concurrency.md`: «reactive-цепочки — `AsyncSequence`; импорт `Combine`
> требует обоснования». Проверяется custom-правилом SwiftLint (`import Combine` → warning).

#### 2.7. Эталонные open-source приложения — разбор

| Проект | Генерация проекта | Модульность | Архитектура | Тесты | Линт | Замечания |
|---|---|---|---|---|---|---|
| **Apple Landmarks** (сэмпл к Liquid Glass) | `.xcodeproj` | одна цель | SwiftUI + `@Observable` (MV) | нет | нет | канонический стиль Apple; смотреть на `glassEffect`, background extension effect [✓ Apple docs] |
| **Point-Free isowords** | SwiftPM + `.xcodeproj` shell | ~40 таргетов в одном `Package.swift` | TCA | `TestStore` + snapshot | — | эталон «полный TCA + монорепо-пакет» [✓ Package.swift] |
| **Bitwarden iOS** | **XcodeGen 2.44.1** (через **Mint**) | фреймворки-таргеты `BitwardenShared`/`BitwardenKit`/`AuthenticatorShared` + extensions | **Coordinator + Processor + State/Action/Effect + Store** | XCTest + **ViewInspector** + **SnapshotTesting**, раздельные test plans; моки — **Sourcery** `AutoMockable` | SwiftLint 0.63.1 + SwiftFormat 0.59.0 (`--swiftversion 6.2`) | самый близкий к нашему профилю эталон [✓ Mintfile, .swiftlint.yml, .swiftformat, contributing.bitwarden.com] |
| **Element X iOS** | **XcodeGen** (`project.yml`, `target.yml` на таргет) | таргеты: `ElementX`, `NSE`, `ShareExtension`, `Components/*` + внешние SPM | Screen + ViewModel + **FlowCoordinators**, Rust-ядро через FFI-пакет | `UnitTests`, **`PreviewTests`** (snapshot из `#Preview`), `UITests`, `IntegrationTests`, **`AccessibilityTests`** | SwiftLint + SwiftFormat + **Periphery**; custom-правила: запрет `print`, обязательный `spacing:` в `VStack`/`HStack` | `deploymentTarget iOS: 18.5`; все SPM-зависимости на `exactVersion` [✓ project.yml, .swiftlint.yml] |
| **Firefox iOS** | `.xcodeproj` + `Package.swift` + fastlane (`Gemfile`) | локальные пакеты | UIKit-наследие + SwiftUI | XCTest | SwiftLint | огромный legacy, не эталон структуры |
| **DuckDuckGo iOS**, **Signal iOS**, **Wire iOS**, **Nextcloud iOS**, **WordPress iOS** | `.xcodeproj` (+ `Package.swift` у WP), fastlane | разное | разное | XCTest | **SwiftLint у всех** | подтверждают «общий знаменатель»: SwiftLint + fastlane + SPM |
| **Just Eat Takeaway (описание архитектуры)** | **Tuist** для app-уровня + генератор `Package.swift` из YAML | 5 категорий модулей: **App / Feature / Foundation / SDK / Utility** | — | — | **`ModularArchitectureValidator`** — свой Swift CLI, валидирует граф на каждом PR | «Feature → Feature запрещён», «Foundation → Foundation разрешён», ацикличность; `emit-graph` → Mermaid/Graphviz [~ albertodebortoli.com, 2026-07-15] |

**Что видно из выборки (и это главный вывод §2):**

1. **Никто не использует один гигантский app-таргет.** Все режут на модули.
2. **Project generation — не обязателен, но популярен там, где таргетов Xcode много**
   (Bitwarden, Element X — XcodeGen; JET — Tuist). Проекты, чья модульность живёт в
   `Package.swift`, обходятся без него (isowords, WordPress).
3. **SwiftLint — универсален.** SwiftFormat — у половины. `swift-format` в крупных OSS почти
   не встречается (историческая инерция: он поздно появился в тулчейне).
4. **Custom SwiftLint-правила используются как архитектурный гейт** (Element X: `print` →
   error; обязательный `spacing:`). Это прямой аналог нашего `forbidigo`.
5. **Навигация везде вынесена в отдельный слой** (Coordinator/FlowCoordinator/Router) — даже в
   SwiftUI-first проектах.
6. **Моки — генерируются** (Bitwarden: Sourcery; другие: Mockolo/Mockable) либо задаются
   протоколом + ручным фейком.

#### 2.8. Вывод: канон 2026 для бизнес-приложения-компаньона

```
SwiftUI (iOS 18+) + Swift 6 language mode + MainActor-by-default
  ├─ Экран        : View + @Observable Model (только там, где нужно)
  ├─ Навигация    : @Observable Router c типизированным [Route]; deep link = чистая функция URL→[Route]
  ├─ Зависимости  : протокол + live/mock реализации; внедрение через init или @Environment
  │                 (контейнер — либо руками «ServiceContainer + Has<X>», либо swift-dependencies)
  ├─ Домен        : pure-Swift модуль, без импорта SwiftUI/UIKit  ← тестируется на Linux
  ├─ Сеть         : URLSession + Codable (+ опц. swift-openapi-generator), middleware для auth/логов
  ├─ Состояние    : async/await + AsyncSequence; Combine — нет
  └─ Персистенс   : ничего по умолчанию; Keychain для токенов; файловый кэш/GRDB — по необходимости
```

---

### §3. Модульность и структура проекта

#### 3.1. Tuist vs XcodeGen vs «голый SwiftPM + один .xcodeproj»

**Исходная боль — `project.pbxproj`:** бинарно-подобный текстовый файл, который Xcode
переписывает при любом добавлении файла → конфликты merge.

**Xcode 16 закрыл эту боль нативно**: `PBXFileSystemSynchronizedRootGroup` («buildable folders»
/ synchronized groups) — группа отображается **прямо на каталог на диске**, файлы больше не
перечисляются в `.pbxproj`. Добавил файл в каталог → он в сборке.
[~ pepicrft.me/blog/how-synchronized-groups-work-at-the-pbxproj-level/, Tuist blog 2025-03-21]

Сравнение под **наш** профиль (1–2 разработчика + агент, шаблон отдаётся заказчику, без
обязательных SaaS):

| | «SwiftPM-модули + один `.xcodeproj` с buildable folders» | XcodeGen | Tuist |
|---|---|---|---|
| Зависимость от внешнего тула | **нет** | `xcodegen` (Mint/brew) | `tuist` (свой установщик) |
| `.pbxproj` в git | да, но почти не меняется | **не коммитится** (генерится) | **не коммитится** |
| Конфликты merge | низкие (buildable folders) | нет | нет |
| Порог входа заказчика | **минимальный** — открыл и работаешь | нужно знать `project.yml` | нужно знать Tuist DSL |
| Поведение агента | предсказуемое | предсказуемое | предсказуемое |
| Цена | 0 | 0 (MIT) | CLI MIT; **ценность в платном облаке** (cache, selective testing, insights, registry, previews) |
| Модульность | через `Package.swift` | через таргеты + SPM | через Tuist-таргеты + SPM |
| Ломкость при новой Xcode | низкая | средняя (гонится за форматом) | средняя |

- **Tuist** — монорепо; лицензия: **MIT по умолчанию, кроме `server/`, `kura/`, `atlas/` —
  MPL-2.0** [✓ github.com/tuist/tuist/blob/main/LICENSE.md]. То есть CLI/генерация — свободны.
  Но продуктовое ядро Tuist в 2026 — платформа (`tuist.dev`): Cache, Selective testing,
  Registry, Build/Bundle insights, Previews [✓ tuist README]. Для экосистемы, у которой
  **принципиально нет CI-раннеров и нет обязательных SaaS**, это лишний вектор.
- **XcodeGen 2.46.0 (MIT, 2026-07-16)** — живой, простой, `project.yml` читается человеком и
  агентом. Используется Bitwarden и Element X.

> **[?] Рекомендация:** для stit-шаблона — **SwiftPM-модули + один `.xcodeproj` c buildable
> folders**, без генератора. Обоснование: минимум зависимостей у заказчика, ноль обучения,
> модульность и так живёт в `Package.swift`, а `.pbxproj` при 3–4 таргетах (app, tests,
> UITests, опц. extension) меняется редко.
> **Fallback**, зафиксировать в отчёте дизайна: если при реальной эксплуатации `.pbxproj`
> станет болью — переход на XcodeGen (а не Tuist), потому что он не тянет за собой облако.

#### 3.2. Ограничения SwiftPM, о которых надо знать заранее

Все — **по документации/отчётам сообщества, на этой машине не проверялись**:

| Ограничение | Суть | Обход |
|---|---|---|
| App-таргет нельзя описать в `Package.swift` | SwiftPM не умеет `.app`, entitlements, Info.plist, схемы, подпись | app-таргет живёт в `.xcodeproj`, всё остальное — в пакетах |
| `swift build` не собирает под iOS «из коробки» | нужен `-destination`/`xcodebuild`, а `swift test` для iOS-таргетов не работает | pure-Swift модули — `swift test` (в т. ч. Linux); UI-модули — `xcodebuild test` |
| Ресурсы | `.process(...)`/`.copy(...)`, доступ через `Bundle.module` | явно объявлять `resources:`; asset catalogs в пакете работают, но Xcode-превью ресурсов слабее |
| Тест-таргеты | нет UI-тестов в SwiftPM | XCUITest — только в `.xcodeproj` |
| Макросы | компилируются под хост, тянут swift-syntax → **дорого по времени сборки** | в Swift 6.3 появился prebuilt swift-syntax для макро-библиотек [✓ swift.org/blog] |
| Плагины | build tool plugins требуют подтверждения в Xcode («trust»), в CI — `-skipMacroValidation` / флаги | предпочитать команду в `Makefile` вместо build-plugin |
| Время сборки | много мелких модулей → много «emit-module» | Xcode 16+ **explicit modules** (`SWIFT_ENABLE_EXPLICIT_MODULES`) строит граф заранее и распараллеливает лучше [~ JET/Qonto] |

#### 3.3. Каноническое дерево каталогов (предлагаемое)

```
ios-template/
├─ App/                                  # тонкий app-таргет (.xcodeproj ссылается сюда)
│  ├─ AppMain.swift                      # @main, composition root
│  ├─ AppEnvironment.swift               # сборка зависимостей (live / mock)
│  ├─ Resources/
│  │  ├─ Assets.xcassets                 # AppIcon, контентные цвета (Any/Dark)
│  │  └─ Localizable.xcstrings           # String Catalog: ru + en
│  ├─ Info.plist
│  └─ PrivacyInfo.xcprivacy
├─ Packages/
│  ├─ Core/                              # один Package.swift на «ядро»
│  │  └─ Sources/
│  │     ├─ Domain/                      # pure-Swift: модели, ошибки, use-case-протоколы  ← Linux
│  │     ├─ Networking/                  # URLSession-клиент, middleware, декодеры         ← Linux
│  │     ├─ Persistence/                 # Keychain, файловый кэш
│  │     ├─ DesignSystem/                # токены, примитивы (нужен SwiftUI)
│  │     └─ AppFoundation/               # логгер, Clock, Result-хелперы                   ← Linux
│  ├─ Features/                          # один Package.swift на все фичи (или по пакету на фичу)
│  │  └─ Sources/
│  │     ├─ AuthFeature/
│  │     ├─ ProfileFeature/
│  │     ├─ TodosFeature/
│  │     └─ ThemeFeature/
│  └─ TestSupport/                        # фейки, фикстуры, snapshot-хелперы
├─ Config/
│  ├─ Base.xcconfig
│  ├─ Dev.xcconfig  Staging.xcconfig  Prod.xcconfig
│  └─ Shared/Version.xcconfig
├─ TestPlans/
│  ├─ Unit.xctestplan  Snapshot.xctestplan  UI.xctestplan
├─ e2e/                                   # Maestro flows (YAML), вне Swift-графа
├─ Tools/
│  ├─ archlint/                           # свой валидатор графа модулей (Swift CLI или python)
│  └─ scripts/
├─ .swiftlint.yml   .swift-format   .xcode-version
├─ Makefile         .claude/         docs/
└─ ios-template.xcodeproj
```

**Naming внутри feature-модуля** (единообразие важнее вкуса):

```
Sources/TodosFeature/
├─ TodosListView.swift          // SwiftUI-вью
├─ TodosListModel.swift         // @Observable, состояние + действия экрана
├─ TodoDetailView.swift
├─ TodoDetailModel.swift
├─ TodosRoute.swift             // enum Route этой фичи + разбор deep link
├─ TodosClient.swift            // протокол зависимости фичи (порт)
└─ Internal/                    // мелкие вью, форматтеры фичи
Tests/TodosFeatureTests/
├─ TodosListModelTests.swift
└─ __Snapshots__/
```

- **Доменные модели и use-case'ы живут в `Core/Domain`**, а не в фиче. Фича импортирует
  `Domain`, но **никогда другую фичу** (прямой аналог нашего `depguard` cross-domain-правила и
  FSD-запрета cross-slice).
- **DDD-домен бэкенда ложится на `Core/Domain`** один-в-один: сущность → `struct`, ошибки →
  `enum <X>Error: Error`, политика → чистая функция/`struct`, use-case → протокол + реализация.
  Слой `delivery` бэкенда соответствует `Networking` + сгенерированным DTO.

#### 3.4. Ресурсы, конфигурация сред, схемы

**`xcconfig` per environment** — это iOS-эквивалент нашего runtime config. В отличие от SPA, у
iOS **нет** рантайм-конфига «один образ на все среды»: build-time-значения запекаются в бандл.

```ini
# Config/Base.xcconfig
IPHONEOS_DEPLOYMENT_TARGET = 18.0
SWIFT_VERSION = 6.0
SWIFT_STRICT_CONCURRENCY = complete
SWIFT_APPROACHABLE_CONCURRENCY = YES
SWIFT_DEFAULT_ACTOR_ISOLATION = MainActor
SWIFT_TREAT_WARNINGS_AS_ERRORS = YES
ENABLE_USER_SCRIPT_SANDBOXING = YES
DEAD_CODE_STRIPPING = YES
PRODUCT_BUNDLE_IDENTIFIER = tech.stit.$(APP_SLUG)$(BUNDLE_ID_SUFFIX)

# Config/Dev.xcconfig
#include "Base.xcconfig"
API_BASE_URL = https:/$()/api.dev.example.com
APP_DISPLAY_NAME = AppName Dev
BUNDLE_ID_SUFFIX = .dev
APP_ENV = dev
```

> **Ловушка (известная, [~]):** в `xcconfig` последовательность `//` начинает комментарий —
> URL `https://host` ломается. Канонический обход — `https:/$()/host` (пустая подстановка
> разрывает `//`).

`Info.plist` читает значения через `$(API_BASE_URL)`, код — через `Bundle.main.object(
forInfoDictionaryKey:)`, обёрнутое в типизированный `AppConfig`.

**Build configurations**: `Debug` / `Release` × схема. Практично — 3 схемы (`App Dev`,
`App Staging`, `App Prod`) и 4 конфигурации (`Debug`, `Debug-Staging`, `Release-Staging`,
`Release`) либо 2 конфигурации + `xcconfig`-переключение через схему. Схемы **обязаны быть
shared** (`xcshareddata/xcschemes/`), иначе `xcodebuild -scheme` в CI не найдёт их.

**String Catalogs (`.xcstrings`)** — канон локализации с Xcode 15; в Xcode 27 добавлены
agent-инструменты перевода и извлечение `NSLocalizedString` из header-файлов
[✓ Xcode 27 Release Notes → Localization]. Для ru/en:
- один `Localizable.xcstrings` на app + по одному на каждый пакет с UI (пакет → `Bundle.module`);
- plural rules описываются прямо в каталоге (variations → plural);
- `BUILD_ONLY_KNOWN_LOCALIZATIONS` — полезно, но в Xcode 27 был фикс краша при нескольких asset
  catalog [✓ там же].

**Asset catalogs** — цвета с вариантами Any/Dark и (опц.) High Contrast; это и есть
«тёмная тема бесплатно». Именование — семантическое (`content.bg`, `text.secondary`,
`status.danger`), не сырое.

#### 3.5. Design-system-agnostic токен-слой на SwiftUI

Прямой перенос правила `shared-ui` из SPA. Структура:

```swift
// Core/DesignSystem/Tokens.swift
public struct Theme: Sendable {
  public struct Colors: Sendable {
    public let surface: Color, surfaceElevated: Color
    public let textPrimary: Color, textSecondary: Color
    public let accent: Color, danger: Color, success: Color
    public let divider: Color
  }
  public struct Metrics: Sendable {
    public let spacingXS: CGFloat, spacingS: CGFloat, spacingM: CGFloat, spacingL: CGFloat
    public let radiusControl: CGFloat, radiusCard: CGFloat
  }
  public struct Typography: Sendable {
    public let title: Font, body: Font, caption: Font   // всё через .system(...,design:) → Dynamic Type
  }
  public let colors: Colors, metrics: Metrics, typography: Typography
  public static let standard = Theme(/* значения ссылаются на asset-catalog цвета */)
}

extension EnvironmentValues {
  @Entry public var theme: Theme = .standard        // @Entry — iOS 18+ короткая форма
}
```

Правила (инварианты шаблона):

- ✅ `Color("content.bg", bundle: .module)` через токен; `theme.metrics.spacingM`
- ❌ `Color(red:green:blue:)`, `Color(hex:)`, магические `.padding(13)`
- ❌ перекраска системных баров/контролов кастомными фонами — прямо против Liquid Glass-гайда
  [✓ Apple, adopting-liquid-glass]
- Liquid Glass применяется **только** к нашим «важным функциональным» элементам через
  `.glassEffect()` внутри `GlassEffectContainer`, не тотально.
- Шрифты — всегда относительно Dynamic Type (`.font(.body)` / `Font.custom(_, relativeTo:)`),
  никогда фиксированный `size:` без `relativeTo:`.

#### 3.6. Accessibility как инвариант

- каждый интерактивный элемент имеет `accessibilityLabel` (или осмысленный текст);
- декоративные картинки — `.accessibilityHidden(true)`;
- Dynamic Type до `.accessibility3` не должен ломать экран (проверяется snapshot-вариантами);
- `XCUIVoiceOverService` — новый API в Xcode 27 для UI-тестов VoiceOver (фокус, озвучка,
  навигация) [✓ Xcode 27 Release Notes → Testing → New Features] — это даёт возможность
  сделать a11y частью автотестов, а не ревью.
- Element X держит отдельный таргет `AccessibilityTests` — прямой эталон
  [✓ element-x-ios project.yml].

---

### §4. Тесты — канон 2026

#### 4.1. Swift Testing vs XCTest: разделение зон

| Задача | Фреймворк | Почему |
|---|---|---|
| unit-тесты домена, сети, мапперов, моделей экранов | **Swift Testing** | `@Test`, `#expect`, `#require`, параметризация, traits, работает на Linux |
| component-тесты SwiftUI (ViewInspector) | Swift Testing или XCTest | ViewInspector умеет оба |
| snapshot-тесты | **Swift Testing** (`@Suite(.snapshots(record:))`) | `swift-snapshot-testing` 1.19.x поддерживает Swift Testing напрямую |
| **UI-тесты (XCUITest)** | **только XCTest** | `XCUIApplication`/`XCUIElement` живут в XCTest |
| **performance (`measure`)** | **только XCTest** | `XCTMetric`, baselines — XCTest-only |

**Interop** в Swift 6.4 управляем: переменная окружения `SWIFT_TESTING_XCTEST_INTEROP_MODE` со
значениями `none | limited | complete | strict`; дефолт зависит от тулчейна и tools-version:

| Toolchain | `swift-tools-version` | Дефолт |
|---|---|---|
| < 6.4 | любой | `none` |
| ≥ 6.4 | < 6.4 | `limited` |
| ≥ 6.4 | ≥ 6.4 | **`complete`** |

[✓ developer.apple.com/documentation/testing/migratingfromxctest]

Практически: `XCTAssert*` внутри хелпера, вызванного из `@Test`, **теперь честно валит тест**
(в режиме `complete`) — раньше падение проглатывалось. Это надо учесть при миграции чужого кода.

**Ключевые возможности Swift Testing, которые нужны шаблону** (API стабильное с 6.0):
`@Test("имя")`, `@Test(arguments: [...])` — параметризация, `@Suite`, `#expect(...)`,
`try #require(...)` (останавливает тест при nil/false), traits: `.tags(.smoke)`,
`.enabled(if:)`, `.disabled("причина")`, `.timeLimit(.minutes(1))`, **`.serialized`** (отключает
параллелизм внутри suite), `withKnownIssue { }`, `confirmation { }` для асинхронных ожиданий.
Swift Testing по умолчанию **гоняет тесты параллельно** — это ловушка для тестов с общим
состоянием, лечится `.serialized`.

Новое в Xcode 27 [✓ Xcode 27 Release Notes → Testing / SwiftPM]:
- `swift test --maximum-repetitions N --repeat-until [pass|fail]` — встроенный анти-flaky;
- «Test Repetition Mode» повторяет отдельные Swift Testing test cases, а не весь план;
- параметризованные test cases в отчёте различаются по хешу аргументов;
- `XCUIVoiceOverService` — управление VoiceOver из UI-тестов;
- в тест-плане настраивается реакция на краш приложения при UI-тестировании:
  `off | warning | failure (default) | fatal failure`;
- шаблон файла launch-тестов с `runsForEachTargetApplicationUIConfiguration` (прогон по всем
  комбинациям ориентации/локализации/оформления).

#### 4.2. Тест-планы (`.xctestplan`)

Разделение на планы — обязательная практика для шаблона (эталон — Bitwarden: отдельные планы
для unit / snapshot / ViewInspector [✓ contributing.bitwarden.com]):

- `Unit.xctestplan` — быстрый, без симулятора-зависимостей, параллельный;
- `Snapshot.xctestplan` — фиксированный симулятор/ОС (см. 4.3), **параллелизм выключен**;
- `UI.xctestplan` — XCUITest, отдельный CI-stage.

В плане настраиваются: code coverage (targets), локализация/регион, `Test Timeouts`,
`Execution Order` (random — полезно для поиска связанности), environment variables,
`repetition mode`. В Xcode 27 JSON тест-плана сортирует теги — diff'ы читаемее
[✓ release notes].

#### 4.3. Snapshot-тесты

`pointfreeco/swift-snapshot-testing` **1.19.5 (2026-09-15, MIT)** — де-факто стандарт.

Что важно знать (из README, проверено):

- Снапшот записывается при первом прогоне и тест **падает** с путём к файлу — это by design.
- Режимы записи: `record: .all`, `.failed`, `.missing`, `.never`; область — `withSnapshotTesting
  { }` или trait `@Suite(.snapshots(record: .failed))`.
- Снапшотить можно **что угодно**: `.image`, `.recursiveDescription`, `.json`, `.dump`, а также
  `URLRequest` (`as: .raw`) — очень полезно для тестов сетевого слоя **без симулятора**.
- Можно переопределять trait collection (size class, content size category) и снимать
  «device-agnostic» снапшоты **с одного симулятора**: `.image(on: .iPhoneSe)`,
  `.image(on: .iPadMini(.portrait))`.
- **Прямое предупреждение авторов:** «Snapshots must be compared using the exact same simulator
  that originally took the reference to avoid discrepancies between images.»
  [✓ github.com/pointfreeco/swift-snapshot-testing README]

> **Стратегия хранения и борьбы с дрейфом (рекомендация):**
> 1. Один **зафиксированный** симулятор и одна версия iOS для снапшотов; версии записаны в
>    файлы репозитория (`.test-simulator-device-name`, `.test-simulator-ios-version` — приём
>    Bitwarden [✓ их репозиторий]) и в `Makefile`.
> 2. Снапшоты лежат рядом с тестом в `__Snapshots__/`, коммитятся, PNG — через Git LFS **не**
>    класть (мелкие, дифф не нужен).
> 3. Апгрейд Xcode/iOS → отдельный коммит «rerecord snapshots», `make snapshot-record`.
> 4. Для «не-визуальных» проверок предпочитать `.recursiveDescription`/`.json`: они
>    текстовые, дифф читаемый, дрейф между версиями меньше.
> 5. `precision:`/`perceptualPrecision:` — крайняя мера: маскирует реальные регрессии.

#### 4.4. ViewInspector

`nalexn/ViewInspector` 0.10.3 (MIT). Позволяет обойти дерево SwiftUI в рантайме и достать
конкретные вью/значения: `try sut.inspect().find(button: "Back")`,
`try sut.inspect().find(CustomView.self).actualView()`, чтение атрибутов (`font()`, `isBold()`),
работа с `@Binding`/`@State`/`@Environment`.
[✓ github.com/nalexn/ViewInspector README]

- **Платформы: iOS / macOS / tvOS / watchOS / visionOS — Linux в списке НЕТ** (SwiftUI на Linux
  не существует) [✓ бейдж платформ в README].
- Ограничения: покрытие API SwiftUI неполное (`readiness.md`), при новой мажорной SwiftUI часть
  инспекторов ломается до обновления библиотеки — **это внешняя зависимость на приватную
  структуру SwiftUI**, риск.
- Релизы редкие (0.10.3 — сентябрь 2025), но репозиторий активен (push 2026-09-13).

> **[?] Рекомендация:** ViewInspector — **опционально**, не в ядре шаблона. Предпочтительнее
> тестировать `@Observable`-модель экрана (чистый Swift, работает на Linux), а визуальное —
> snapshot'ом. ViewInspector брать точечно, когда нужно проверить «кнопка задизейблена при
> невалидной форме» без снапшота.

#### 4.5. E2E: XCUITest vs Maestro

| | XCUITest | Maestro (cli-2.10.0, Apache-2.0) |
|---|---|---|
| Язык | Swift | YAML |
| Запуск | `xcodebuild test` | `maestro test flow.yaml` |
| Реальные iOS-устройства | да | **нет** — только симулятор (самая запрашиваемая фича с января 2023) [~ BrowserStack/DeviceLab обзоры] |
| Стабильность | своя (нужны `waitForExistence`) | встроенная толерантность к таймингам |
| Кросс-платформенность с Android | нет | **да** — один язык флоу для обеих платформ |
| MCP для агента | через Xcode MCP (см. 6.x) | **Maestro MCP** — «agentic UI testing» [✓ README] |

> **[?] Рекомендация:** в шаблоне — **XCUITest как минимум** (smoke: запуск, логин, главный
> экран) + **Maestro как кросс-платформенный e2e**, если поток 03 подтвердит парность
> Android/iOS шаблонов: один набор YAML-флоу на обе платформы — сильный аргумент. Maestro —
> отдельный CI-stage, не в `make check`.

#### 4.6. Моки и подмена зависимостей

Три жизнеспособных подхода:

1. **Протокол + рукописный фейк** (в `TestSupport`). Ноль зависимостей, максимум контроля,
   больше писанины. Подходит, когда протоколов немного.
2. **`swift-dependencies` (1.17.1, MIT)** — `@DependencyClient`-макрос делает struct-of-closures,
   `withDependencies { $0.apiClient.fetchTodos = { ... } } operation: { ... }` подменяет точечно;
   `testValue` по умолчанию «падает, если вызвали неожидаемо» — это очень ценное свойство для
   агентного цикла (тест ломается громко, а не молча). Не требует TCA.
3. **Кодоген**: `Sourcery` с `// sourcery: AutoMockable` (Bitwarden), `Mockolo` (Uber, 2.6.1),
   `Mockable` (макросы, 0.6.4). Даёт «mockery-подобный» опыт из go-backend-template, но:
   Sourcery/Mockolo — **лишний бинарь** в цепочке; `Mockable` — ещё 0.x.

> **[?] Рекомендация:** взять **`swift-dependencies`**. Это ближайший аналог того, что даёт
> go-backend-template через `mockery` + fx: явные зависимости, автоматические тестовые
> заглушки, никакого «магического» контейнера. Плюс он не тянет TCA.
> **Альтернатива без внешних зависимостей** — паттерн Bitwarden: `ServiceContainer` +
> `protocol HasAPIClient { var apiClient: APIClient { get } }` + `typealias Services = HasAPIClient
> & HasAuthRepository`. Читается прекрасно, но моки пишутся руками.

#### 4.7. Сетевые тесты без сети

- **`URLProtocol`-стаб** — канонический способ: подкласс `URLProtocol`, зарегистрированный в
  `URLSessionConfiguration.ephemeral.protocolClasses`, отдаёт заранее заданные ответы. Работает
  и на Linux (FoundationNetworking).
- **`swift-openapi-generator`**: генерируемый `Client` принимает любой `ClientTransport` →
  тестовый транспорт в 20 строк подменяет всю сеть. Middleware-абстракция (`ClientMiddleware`)
  — штатное место для auth-cookie/логирования/ретраев.
  [✓ github.com/apple/swift-openapi-generator README]
- Snapshot `URLRequest` (`assertSnapshot(of: request, as: .raw)`) — дёшево фиксирует контракт
  «какой именно запрос мы шлём».
- **Mock-режим приложения** (аналог MSW-сценариев SPA) делается тем же швом: `AppEnvironment`
  выбирает `LiveAPIClient` или `MockAPIClient(scenario:)` по build-флагу/launch-argument
  (`-ui-testing-scenario populated`). Это даёт детерминированные XCUITest/Maestro-прогоны.

#### 4.8. Команды `xcodebuild` для гейта (по man-странице)

Ключевые опции (проверено по man xcodebuild
[✓ keith.github.io/xcode-man-pages/xcodebuild.1.html]):

| Опция | Смысл |
|---|---|
| `-workspace` / `-project` / `-scheme` | что собирать; `-scheme` обязателен для workspace |
| `-destination` | целевое устройство/симулятор |
| `-configuration` | конфигурация сборки |
| `-derivedDataPath` | изолированный DerivedData (обязателен в CI) |
| `-resultBundlePath` | `.xcresult` с логами, покрытием, тестами, аттачментами |
| `-enableCodeCoverage YES\|NO` | покрытие, поверх настройки схемы |
| `-testPlan` | какой план гонять |
| `-only-testing` / `-skip-testing` | сузить/исключить |
| `-parallel-testing-enabled YES\|NO` | параллелизм тестов |
| `-maximum-concurrent-test-simulator-destinations N` | сколько симуляторов одновременно |
| `-test-iterations N`, `-retry-tests-on-failure`, `-run-tests-until-failure` | повторы/анти-flaky |
| `build-for-testing` / `test-without-building` | разделение сборки и прогона (даёт `.xctestrun`) |
| `analyze` | статический анализатор Clang/Swift |
| `-showBuildSettings`, `-list` | интроспекция без сборки |
| `-allowProvisioningUpdates` | автоматическая подпись (только с настроенным Apple ID) |
| `-quiet` | только warnings/errors |

**Форматирование вывода** — `xcbeautify` 3.2.1 (MIT, **macOS и Linux**, статический Swift-бинарь,
умеет **JUnit**, рендереры `github-actions`/`teamcity`/`azure-devops-pipelines`)
[✓ github.com/cpisciotta/xcbeautify README]. Каноническая обвязка:

```bash
set -o pipefail && NSUnbufferedIO=YES xcodebuild <flags> 2>&1 | xcbeautify
```

**Покрытие:**
- `xcrun xccov view --report --json <path>.xcresult` — весь отчёт одним вызовом (с Xcode 13);
- `xcrun xcresulttool get test-results summary|tests|test-details|activities` — **новый** CLI;
  старый `xcresulttool get object --format json` **deprecated с Xcode 16** и требует `--legacy`
  [~ множественные issue: fastlane#22132, flutter#151502, forums.developer.apple.com/thread/763050];
- `xcresultparser` 2.2.0 (MIT) конвертит `.xcresult` → JUnit / Cobertura / SonarQube-generic /
  markdown / html, умеет отмечать **flaky** (упал → прошёл на ретрае) [✓ README].
  Требует **macOS 12+** — на Linux не работает.

> **Порог покрытия как гейт**: считать по `xccov --json` + скрипт (аналог нашего
> `cover-check` с полом 75 %). Scope — **только pure-Swift модули** (`Domain`, `Networking`,
> модели экранов); UI/снапшоты в пол не включать — иначе цифра будет врать.

#### 4.9. Что запускается на Linux (ключ к агентному циклу)

| Что | Linux | Комментарий |
|---|---|---|
| `swift build` / `swift test` pure-Swift модулей | **да** | Foundation, FoundationNetworking, Swift Testing, XCTest — есть |
| Swift Testing | **да** | часть тулчейна |
| `swift format lint` / `swift format` | **да** [? проверить на живом тулчейне] | «Swift 6 … and above include swift-format in the toolchain… `swift format`» — тулчейны Linux собираются из того же релиза [✓ swift-format README]; отдельные artifact-бандлы — только macOS |
| SwiftLint | **да** | релиз 0.65.1 содержит `swiftlint_linux_amd64.zip` и `swiftlint_linux_arm64.zip` + `Dockerfile` [✓ github.com/realm/SwiftLint/releases/tag/0.65.1]; SourceKit ищется в `/usr/lib/libsourcekitdInProc.so` или `LINUX_SOURCEKIT_LIB_PATH` [✓ SwiftLint README] |
| `swift-snapshot-testing` — **текстовые** стратегии (`.json`, `.dump`, `URLRequest .raw`) | **да** | «any format of any value on any Swift platform» [✓ README] |
| `swift-snapshot-testing` — `.image` | **нет** | нужен UIKit/AppKit |
| ViewInspector | **нет** | SwiftUI отсутствует на Linux |
| `xcodebuild`, симулятор, XCUITest, Previews, Instruments | **нет** | только macOS |
| Подпись, архив, upload в App Store Connect | **нет** | только macOS |
| `xcbeautify` | **да** | но парсить нечего без xcodebuild |
| `xcresultparser` | **нет** | требует macOS 12+ |
| Periphery | **нет** | требует индекс от Xcode/SourceKit (index store) |

> **Это и есть главный вывод по тестам.** Архитектура шаблона должна быть такой, чтобы
> **бизнес-логика, сеть, разбор deep link, валидация форм, мапперы и модели экранов**
> (`@Observable`-классы без импорта SwiftUI — возможно, если вынести логику в отдельный тип)
> жили в pure-Swift модулях. Тогда агент на Linux получает цикл «правка → `swift test` →
> зелёно» за секунды, а Mac нужен только для сборки бандла, snapshot'ов и XCUITest.
>
> Практическая граница: `@Observable final class TodosListModel` **можно** держать в pure-Swift
> модуле — `Observation` входит в стандартную библиотеку Swift и доступна на Linux
> **[? требует эмпирической проверки в потоке 06]**; а `View` — нет.

#### 4.10. Политика «что обязано иметь тест» (проект правила)

| Слой | Обязательный минимум |
|---|---|
| `Core/Domain` | unit на каждую политику/валидацию/use-case; **в пол покрытия** |
| `Core/Networking` | тест маппинга DTO→модель, обработка ошибок/кодов, snapshot `URLRequest`; **в пол** |
| `Core/Persistence` (Keychain) | тест round-trip + удаления (на симуляторе) |
| `Features/*` — модель экрана | unit на каждое состояние (loading/empty/error/success) и каждое действие; **в пол** |
| `Features/*` — `View` | snapshot: light/dark × Dynamic Type default/`.accessibility1`; a11y-проверка |
| `Route`/deep link | unit `URL → [Route]` и обратно; **в пол** |
| app-уровень | 1 smoke XCUITest: cold start → login (mock) → главный экран |

---

### §5. Качество кода и гейт

#### 5.1. SwiftLint

`realm/SwiftLint` **0.65.1** (2026-08-21, MIT, 19.7k★). Универсально принят: SwiftLint есть у
Bitwarden, Element X, DuckDuckGo, Wire, Nextcloud, WordPress, Firefox.

**Как запускать.** Два пути: (а) CLI (`swiftlint --strict`), (б) SwiftPM-плагин — причём
официальный README указывает на **сторонний** репозиторий-обёртку
`https://github.com/SimplyDanny/SwiftLintPlugins` (command plugin и build tool plugin), а не на
сам `realm/SwiftLint` [✓ SwiftLint README → Installation]. Плагин удобен тем, что линт идёт при
каждой сборке, но: требует «доверия» плагину в Xcode, замедляет сборку, хуже управляем из
`Makefile` и добавляет зависимость от стороннего maintainer'а.
> **[?] Для шаблона — CLI**, вызываемый из `make lint`. Это ровно тот же контракт, что в
> go-backend-template (гейт — команда, а не IDE).

**Дистрибуция:** релиз 0.65.1 содержит `SwiftLint.pkg`, `SwiftLint.amd64.zip`/`arm64.zip`
(macOS), `SwiftLintBinary.artifactbundle.zip`, `portable_swiftlint.zip`, **`swiftlint_linux_amd64.zip`**
и **`swiftlint_linux_arm64.zip`**, плюс `bazel.tar.gz`; в репозитории есть `Dockerfile`
[✓ github.com/realm/SwiftLint/releases/tag/0.65.1]. Т. е. **Linux-путь официально поддержан** —
это ключ к тому, чтобы линт был частью Linux-гейта.

**`--strict`** превращает warnings в errors — обязателен в гейте.

**Custom rules как аналог `forbidigo`/`depguard`.** SwiftLint поддерживает регулярочные
`custom_rules` — Element X использует их именно так (запрет `print`, обязательный `spacing:`)
[✓ element-x-ios/.swiftlint.yml]. Черновик для нашего шаблона — в §7.

**Analyzer rules** (`swiftlint analyze`) — `unused_declaration`, `unused_import` — требуют
compiler log (`--compiler-log-path`), т. е. **только на Mac** и только после сборки. Bitwarden
их включает [✓ их `.swiftlint.yml`]. Для нас — опциональный Mac-only шаг.

#### 5.2. `swift-format` (Apple) vs SwiftFormat (nicklockwood)

| | `swift-format` (swiftlang) | SwiftFormat (nicklockwood) |
|---|---|---|
| Версия | 604.0.0 (2026-09-16) | 0.63.0 (2026-08-30) |
| Лицензия | Apache-2.0 | MIT |
| Поставка | **в тулчейне Swift 6+**: `swift format` (через пробел) | brew/Mint/SPM |
| Конфиг | `.swift-format` (JSON) | `.swiftformat` (флаги) |
| Правил | меньше, консервативнее | **много больше**, агрессивные преобразования |
| Linux | да (тулчейн) | да (собирается) |
| Режим проверки | `swift format lint --strict --recursive .` | `swiftformat --lint .` |

Apple прямо оговаривает: «No default Swift code style guidelines have yet been proposed. The
style that is currently applied by swift-format is just one possibility»
[✓ github.com/swiftlang/swift-format README].

> **[?] Рекомендация: `swift format`.** Причины: (1) ноль внешних бинарей — он уже в тулчейне,
> которым мы и так собираем; (2) один инструмент вместо двух (SwiftFormat у всех идёт **в
> дополнение** к SwiftLint, а не вместо); (3) для агента важнее «детерминированный
> форматтер под рукой», чем богатство правил. SwiftLint при этом остаётся линтером (правила
> смысла), swift-format — форматтером (правила вида) — ровно как `gofumpt` + `golangci-lint`.
>
> Если владелец предпочтёт мощность — SwiftFormat 0.63.0 с конфигом уровня Bitwarden
> (`--swiftversion 6.2`, явные `--enable`/`--disable`) тоже валиден, но это +1 бинарь и +1
> источник конфликтов с SwiftLint.

#### 5.3. Periphery (dead code) — предупреждение

- Последний OSS-релиз **3.8.0 (2026-07-25, MIT)**; репозиторий **архивирован 2026-08-12**,
  проект переведён в коммерческий продукт (`periphery.pro`). Форк разрешён (MIT), бесплатно
  для «small indie and hobby projects» и «open-source projects of any size».
  [~ архивная плашка GitHub + periphery.pro; точные условия лицензии коммерческой версии не
  проверены]
- Технически требует index store (сборка на Mac) — на Linux не работает.

> **[?] Предмет решения владельца:** (а) пиннить OSS-3.8.0 и жить, пока работает;
> (б) считать dead-code-гейт опциональным Mac-only шагом; (в) заменить на
> `swiftlint analyze --strict` с `unused_declaration`/`unused_import` (покрывает меньше, но
> бесплатно и вечно OSS). **Рекомендация отчёта — (в) + опционально (а)**.

#### 5.4. Компиляторные флаги как гейт

```ini
SWIFT_VERSION = 6.0                       # Swift 6 language mode
SWIFT_STRICT_CONCURRENCY = complete
SWIFT_APPROACHABLE_CONCURRENCY = YES
SWIFT_DEFAULT_ACTOR_ISOLATION = MainActor
SWIFT_TREAT_WARNINGS_AS_ERRORS = YES
GCC_TREAT_WARNINGS_AS_ERRORS = YES
SWIFT_UPCOMING_FEATURE_EXISTENTIAL_ANY = YES         # ExistentialAny: требовать any P
SWIFT_UPCOMING_FEATURE_MEMBER_IMPORT_VISIBILITY = YES
SWIFT_UPCOMING_FEATURE_INTERNAL_IMPORTS_BY_DEFAULT = YES
ENABLE_USER_SCRIPT_SANDBOXING = YES
CLANG_ANALYZER_LOCALIZABILITY_NONLOCALIZED = YES     # ловит нелокализованные строки в UI
```

- `ExistentialAny` — заставляет писать `any Protocol`, убирает скрытые боксы (перф + ясность).
- `InternalImportsByDefault` — импорты по умолчанию `internal`, публичный API модуля не течёт
  транзитивно. **Это прямой аналог нашего требования «public API только через index.ts»**.
- `MemberImportVisibility` — члены типа видны, только если модуль импортирован явно; убирает
  «магию» транзитивных расширений.
- Эквивалент в `Package.swift`: `.enableUpcomingFeature("ExistentialAny")` и т. д.

**`package` access level** (Swift 5.9+) — уровень видимости «внутри пакета»: позволяет делать
типы общими для нескольких таргетов **одного** SwiftPM-пакета, не делая их `public`. Это
инструмент, которым в iOS-шаблоне имитируется наш «internal within slice, public API наружу».

#### 5.5. Архитектурные тесты («Steiger/depguard для Swift»)

Готового инструмента нет. Варианты:

| Способ | Что ловит | Цена |
|---|---|---|
| **Свой валидатор графа** (эталон JET `ModularArchitectureValidator`) | циклы, запрещённые рёбра (Feature→Feature), категории модулей | ~200–400 строк; **полный контроль**; работает на Linux (парсит `Package.swift`/манифест) |
| SwiftLint `custom_rules` на `import` | `import OtherFeature` в файле фичи | дёшево, но регулярка; не видит транзитива |
| `swift package show-dependencies --format json` | фактический граф пакета | не даёт правил, но даёт вход для валидатора |
| `swift package dump-package` | JSON манифеста (targets + dependencies) | **лучший вход** для валидатора, чистый JSON |
| Tuist graph validation | то же, но через Tuist | тянет Tuist |
| Periphery | мёртвый код (не границы) | Mac-only, коммерциализован |
| `xcodebuild analyze` | Clang/Swift-анализатор | Mac-only, медленно |

> **[?] Рекомендация:** написать `Tools/archlint` — маленький валидатор поверх
> `swift package dump-package --package-path Packages/Features` (+ то же для `Core`), который:
> 1) строит граф `target → dependencies`; 2) проверяет ацикличность; 3) проверяет матрицу
> категорий (`App → Feature → Core/Domain`; `Feature ↛ Feature`; `Core/Domain ↛ всё, кроме
> AppFoundation`); 4) печатает mermaid-граф по флагу. **Работает на Linux**, входит в
> `make check`. Это ближайший аналог `depguard`-секции нашего `.golangci.yml`.

#### 5.6. Цепочка гейта `make check` (проект)

Разделена на **Linux-часть** (быстрая, доступна агенту всегда) и **Mac-часть**:

```make
## ── быстрый цикл (Linux ИЛИ Mac) ────────────────────────────────
fmt:            swift format --in-place --recursive Packages App Tools
fmt-check:      swift format lint --strict --recursive Packages App Tools
lint:           swiftlint --strict --quiet
archlint:       swift run --package-path Tools/archlint archlint
test-core:      swift test --package-path Packages/Core
test-features:  swift test --package-path Packages/Features      # только pure-Swift таргеты
cover-check:    scripts/cover-check.sh  # swift test --enable-code-coverage + llvm-cov, пол N%

check-fast:  fmt-check lint archlint test-core test-features cover-check

## ── полный гейт (только macOS) ──────────────────────────────────
build-app:
	set -o pipefail && NSUnbufferedIO=YES xcodebuild build \
	  -project ios-template.xcodeproj -scheme "App Dev" \
	  -destination "$(DEST)" -derivedDataPath .build/dd 2>&1 | xcbeautify

test-app:
	set -o pipefail && NSUnbufferedIO=YES xcodebuild test \
	  -project ios-template.xcodeproj -scheme "App Dev" \
	  -testPlan Unit -destination "$(DEST)" \
	  -derivedDataPath .build/dd -resultBundlePath .build/Unit.xcresult \
	  -enableCodeCoverage YES -parallel-testing-enabled YES 2>&1 | xcbeautify

test-snapshot:
	set -o pipefail && NSUnbufferedIO=YES xcodebuild test \
	  -project ios-template.xcodeproj -scheme "App Dev" \
	  -testPlan Snapshot -destination "$(SNAPSHOT_DEST)" \
	  -derivedDataPath .build/dd -resultBundlePath .build/Snapshot.xcresult \
	  -parallel-testing-enabled NO 2>&1 | xcbeautify

check: check-fast build-app test-app test-snapshot
```

Где `DEST = platform=iOS Simulator,name=iPhone 17,OS=27.0`, а `SNAPSHOT_DEST` — **жёстко
зафиксированная** пара «устройство + OS» из `.test-simulator-*`-файлов.

Ориентировочное время (**оценка, не измерено** — Mac недоступен):

| шаг | Linux | Mac (M-серия) |
|---|---|---|
| `fmt-check` | сек | сек |
| `swiftlint --strict` | 5–20 с | 5–20 с |
| `archlint` | < 1 с | < 1 с |
| `swift test` (Core+Features, pure) | 20–60 с (первая сборка дольше) | 20–60 с |
| `xcodebuild build` (чистый) | — | **3–10 мин** |
| `xcodebuild build` (инкрементально) | — | 20–90 с |
| `xcodebuild test` Unit-план | — | 1–4 мин |
| Snapshot-план | — | 1–5 мин |

**pre-commit / pre-push:**
- pre-commit → `fmt-check` + `swiftlint --strict` по изменённым файлам (быстро);
- pre-push → **`make check-fast`** на любой платформе; **`make check`** — только если хост
  macOS (детектить через `uname`). Это честно: иначе гейт станет невыполнимым на Linux и его
  начнут обходить.

`.xcode-version` (файл с `27.0`) + `xcodes` 2.1.0 — пиннинг версии Xcode на машине
разработчика; `xcodes install 27.0 && xcodes select 27.0`.

---

### §6. Проблемы и тонкости платформы

> Владелец спросил об этом отдельно. Ниже — всё, что реально кусает в production-iOS, с
> источником и версией. Раздел намеренно длинный: это та часть, которую в шаблоне придётся
> закрывать правилами и процедурами, а не кодом.

#### 6.1. Подпись, сертификаты, provisioning — главный налог платформы

- **Три сущности, которые надо держать в голове**: Apple Developer *certificate* (привязан к
  Apple ID/команде), *App ID* (+ capabilities/entitlements), *provisioning profile* (связка
  сертификат + App ID + устройства). Плюс **entitlements-файл** в проекте, который должен
  совпасть с профилем.
- **Automatic signing в Xcode** удобно локально, но требует интерактивного логина и
  `-allowProvisioningUpdates` в CLI; в CI без Apple ID оно не работает.
- **`fastlane match`** — индустриальный ответ: сертификаты и профили хранятся в приватном
  git-репо/S3 зашифрованными, команда/CI их «матчит». **Ruby-зависимость**, fastlane 2.240.1
  жив и активен (релиз 2026-09-15, MIT). Альтернатива без Ruby — App Store Connect API key
  (`.p8`) + `xcodebuild -allowProvisioningUpdates` + скрипт импорта в временный keychain.
- **Для экосистемы stit** (нет CI-раннеров, релиз делает разработчик локально) это означает:
  подпись живёт на машине разработчика; «STOP-граница» мобильного репо — не «образ в registry»,
  а **подписанный `.ipa`/`.aab` + загрузка в сторы/TestFlight**. Это открытый вопрос дизайна
  (см. брифинг п. «STOP-граница мобильного репо»).
- **Ловушка Xcode 27**: «Apps with Hardware-Checked Pointer Arithmetic Slice feature enabled
  cannot be uploaded to the App Store on macOS Tahoe 26.6 with Xcode automatic signing enabled»
  — обход: macOS 27 + Xcode 27 или ручная подпись (radar 187057599)
  [✓ Xcode 27 Release Notes → Enhanced Security → Known Issues].

#### 6.2. App Store Review — что чаще всего валит бизнес-приложения

Проверено по тексту App Review Guidelines
[✓ developer.apple.com/app-store/review/guidelines/]:

| Гайдлайн | Дословно / суть | Что делать в шаблоне |
|---|---|---|
| **4.2 Minimum Functionality** | «Your app should include features, content, and UI that elevate it beyond a repackaged website… If your app is not particularly useful, unique, or "app-like," it doesn't belong on the App Store.» | B2B-компаньон к CRM — типичная жертва. Держать нативные фичи: push, offline-кэш, камера/QR, шаринг, виджет |
| **4.2.2** | «apps shouldn't primarily be marketing materials, advertisements, web clippings, content aggregators, or a collection of links» | не делать WebView-обёртку |
| **4.2.3(i)** | «Your app should work on its own without requiring installation of another app» | — |
| **5.1.1(v) Account Sign-In** | «If your app doesn't include significant account-based features, let people use it without a login. **If your app supports account creation, you must also offer account deletion within the app.**» | **удаление аккаунта — экран в шаблоне**, не «напишите нам». Плюс: если регистрации в приложении нет (аккаунт заводит админ CRM) — требование удаления формально не срабатывает, но ревьюеры трактуют строго ⇒ безопаснее иметь |
| **2.1 App Completeness** | «include demo account info (and turn on your back-end service!) if your app includes a login. If you are unable to provide a demo account… you may include a built-in demo mode… with prior approval by Apple» | **mock-режим шаблона решает это буквально**: встроенный demo mode + инструкция в `docs/` |

Дополнительно (не в цитатах, но устойчивая практика [~]): для корпоративных приложений, которые
бесполезны без корпоративного аккаунта, Apple регулярно требует либо demo-доступ, либо
пере-подачу через **Apple Business Manager / custom apps** (частное распространение) — это
отдельный трек дистрибуции (поток 05).

#### 6.3. Privacy manifests (`PrivacyInfo.xcprivacy`)

- Формат: property list в корне бандла — `Sample.app/PrivacyInfo.xcprivacy`; для фреймворка —
  в корне `.framework`; для SwiftPM-пакета — в `Sources/<Target>/` (или в объявленных
  `resources:`). [✓ Apple: adding-a-privacy-manifest-to-your-app-or-third-party-sdk]
- Ключи: `NSPrivacyTracking`, `NSPrivacyTrackingDomains`, `NSPrivacyCollectedDataTypes`,
  `NSPrivacyAccessedAPITypes`.
- **Required reason API — ровно 5 категорий** (проверено по Apple-доке):
  `NSPrivacyAccessedAPICategoryUserDefaults`, `…FileTimestamp`, `…SystemBootTime`, `…DiskSpace`,
  `…ActiveKeyboards`. Каждая требует кода-причины (вида `CA92.1`, `C617.1`, …).
  [✓ developer.apple.com/documentation/bundleresources/app-privacy-configuration/nsprivacyaccessedapitypes/nsprivacyaccessedapitype]
- **`UserDefaults` в этом списке** — т. е. практически любое приложение обязано объявить эту
  категорию. Это первый источник «писем от Apple» после загрузки.
- Даты: с **01.05.2024** сборки без описания required-reason API **не принимаются** App Store
  Connect; с **12.02.2025** нужен валидный манифест **и подпись** для списка популярных
  сторонних SDK. Невалидный манифест (лишние ключи/значения) → **реджект сабмишена** и письмо
  с путями файлов. [✓ Apple docs, оба «important»-блока]

> **Для шаблона:** `PrivacyInfo.xcprivacy` — **обязательный файл в репо с первого коммита**, с
> объявленной категорией `UserDefaults` и пустыми `NSPrivacyCollectedDataTypes`
> (`NSPrivacyTracking = false`). Правило в `.claude/rules/signing-and-secrets.md`: добавил
> стороннюю зависимость — проверь её манифест.

#### 6.4. ATT, ATS, TLS

- **ATT (App Tracking Transparency)** нужен только при трекинге между приложениями/сайтами
  разных компаний. Для внутреннего бизнес-приложения — **не нужен**, и «на всякий случай» его
  добавлять вредно (лишний диалог + вопросы ревью).
- **ATS (App Transport Security)** требует HTTPS с TLS 1.2+ и forward secrecy. Исключения
  (`NSAllowsArbitraryLoads`, `NSExceptionDomains`) — красный флаг для ревью и должны быть
  запрещены правилом шаблона. Self-signed сертификаты в dev-среде решаются **не** отключением
  ATS, а доверенным корневым сертификатом на устройстве/симуляторе.

#### 6.5. Keychain — хранение токенов

- **Запрет:** access/refresh-токены в `UserDefaults`/файле. Только Keychain.
- Класс доступности: **`kSecAttrAccessibleAfterFirstUnlock`** — «The data in the keychain item
  cannot be accessed after a restart until the device has been unlocked once by the user…
  recommended for items that need to be accessed by background applications. Items with this
  attribute migrate to a new device when using encrypted backups.»
  [✓ developer.apple.com/documentation/security/ksecattraccessibleafterfirstunlock]
  Для токенов, нужных фоновому обновлению (push, background refresh) — это правильный выбор.
  Если миграция на новое устройство нежелательна — `…AfterFirstUnlockThisDeviceOnly`.
- `kSecAttrAccessGroup` + Keychain Sharing — нужны, если есть app extension (share/notification
  service) — Element X делает это через
  `KEYCHAIN_ACCESS_GROUP_IDENTIFIER = "$(DEVELOPMENT_TEAM).$(BASE_BUNDLE_IDENTIFIER)"`
  [✓ element-x-ios project.yml].
- **iCloud Keychain sync** для токенов выключать (`kSecAttrSynchronizable = false`).
- **Ловушка:** Keychain **переживает удаление приложения** — после переустановки в нём может
  лежать токен от прошлой установки. Канон: на первом запуске (флаг в `UserDefaults`) очищать
  Keychain.
- Биометрия — `LAContext` / `kSecAccessControl` с `.biometryCurrentSet`; при смене
  Face/Touch ID-набора запись становится недоступной (это фича, а не баг).

**Контракт с нашим Go-бэкендом — отдельная тонкость.** Бэкенд stit выдаёт **httpOnly cookies**
(`access_token`/`refresh_token`, `SameSite=Strict`) и **не поддерживает `Authorization: Bearer`**
[бриф, §Экосистема]. На iOS это означает:
- `URLSession` сам хранит cookies в `HTTPCookieStorage` — **но это не Keychain**, а
  незашифрованный контейнер приложения;
- надёжный путь: `HTTPCookieStorage` в режиме `.ephemeral` + ручное сохранение
  `refresh_token`-cookie в Keychain и восстановление при старте;
- либо договориться с бэкендом о bearer-режиме для мобильных клиентов.
  **Это открытый вопрос владельцу** (детально — поток 07).

#### 6.6. Universal Links / AASA

Проверено по Apple [✓ developer.apple.com/documentation/xcode/supporting-associated-domains]:

- файл `apple-app-site-association` (без расширения) по пути
  `https://<fqdn>/.well-known/apple-app-site-association`, **https с валидным сертификатом и
  без редиректов**;
- формат `appIDs: ["<TeamID>.<BundleID>"]`, `components` с `/`, `?`, `#` и `exclude`;
- каждый сабдомен требует **своей** записи в entitlement и **своего** файла;
- entitlement `com.apple.developer.associated-domains`, значения вида
  `applinks:example.com`; `*.example.com` матчит поддомены (кроме `appclips`);
- **с iOS 14 запрос идёт не на ваш сервер, а в CDN Apple**; CDN забирает файл **в течение
  24 часов**, устройства проверяют обновления **примерно раз в неделю** после установки. Это
  и есть источник классического «поменял AASA — ничего не изменилось»;
- для приватных/недоступных из интернета доменов — `?mode=developer` в entitlement (bypass CDN).

> **Инфра-поверхность:** `.well-known/apple-app-site-association` должен раздавать Traefik в
> `<org>/infra` — это cross-repo зависимость, которую шаблон обязан описать в handoff.

#### 6.7. Push (APNs)

Проверено [✓ developer.apple.com/documentation/usernotifications/establishing-a-token-based-connection-to-apns]:

- **Token-based auth (`.p8`)** — рекомендуемый путь: JWT, `alg = ES256`, `kid` = 10-символьный
  Key ID, `iss` = Team ID; токен **обновлять и переподписывать не реже раза в час**.
- Ключи бывают **team-scoped** (максимум **2 на среду** — Sandbox/Production) и
  **topic-specific** (до **200** на среду, до **400 топиков** на ключ).
- «APNs doesn't support authentication tokens from multiple developer accounts over a single
  connection.»
- В приложении: `UNUserNotificationCenter` (разрешение, категории, actions),
  `application(_:didRegisterForRemoteNotificationsWithDeviceToken:)` → отправка device token на
  бэкенд. Background-доставка — `content-available: 1` + capability `Background Modes / Remote
  notifications`; доставка **не гарантирована** и троттлится системой.
- **РФ-специфика** (push через APNs из РФ, альтернативы) — поток 05.

#### 6.8. SwiftUI: производительность и «странности»

Всё ниже — устойчивый отраслевой опыт **[~]**, Apple документирует лишь часть:

- **Identity.** SwiftUI пересоздаёт состояние вью при смене identity. Использование
  `.id(...)` «чтобы обновилось» сбрасывает `@State`; `ForEach` без стабильного `id` приводит к
  потере состояния строк и рваным анимациям.
- **`@State` не там.** Состояние, поднятое выше, чем нужно, приводит к перерисовке половины
  дерева. Observation смягчает: вью зависит только от **прочитанных** свойств
  [✓ Apple docs], но это работает только если вы читаете свойство **внутри** того вью, которому
  оно нужно, а не пробрасываете весь объект вниз.
- **`onChange(of:)`** в iOS 17+ поменял сигнатуру (двухпараметрическая версия) — старый
  однопараметрический вариант deprecated.
- **`task(id:)`** — правильный способ перезапускать асинхронную загрузку при смене входа;
  `onAppear` + `Task { }` протекает при быстрых перерисовках.
- **Дорогая работа в `body`** (форматтеры, сортировки, `DateFormatter()` в теле) — классическая
  причина лагов списка. Форматтеры — статические/закэшированные.
- Инструменты: **SwiftUI instrument** в Instruments (в Xcode 27 добавлены «Summary of Updates»
  и причины некэширования layout-проходов) [✓ Xcode 27 Release Notes → Instruments].

#### 6.9. Previews и модули

- `#Preview` требует, чтобы модуль собирался; в многомодульной структуре превью фичи
  **не должно** тянуть весь app.
- Ресурсы в превью пакета берутся из `Bundle.module` — если забыть `resources:`, превью падает
  с непонятной ошибкой.
- В Xcode 27 код в `#Preview` **явно на main actor** [✓ release notes]; там же появился
  `#Preview(arguments:)` с сеткой вариантов, Resizable Canvas, превью в другой локализации,
  и `RenderPreview` MCP-инструмент для агентов.
- **Element X использует превью как источник snapshot-тестов** (таргет `PreviewTests`) — очень
  экономный приём: один `#Preview` = и превью для человека, и снапшот для CI
  [✓ element-x-ios project.yml].

#### 6.10. Время сборки

- Главные пожиратели: макросы (swift-syntax), type inference в длинных выражениях, whole-module
  optimization в Debug, много мелких модулей без explicit modules.
- Средства: `SWIFT_ENABLE_EXPLICIT_MODULES` (Xcode 16+), `-Xfrontend
  -warn-long-function-bodies=500 -Xfrontend -warn-long-expression-type-checking=500`,
  разбиение сложных выражений, `swift build --explicit-target-dependency-import-check`.
- В Swift 6.3 — prebuilt swift-syntax для макро-библиотек [✓ swift.org/blog].
- Xcode 27: dependency scanner ускорен, симуляторы содержат **prebuilt dyld cache** → первый
  запуск симулятора заметно быстрее [✓ Xcode 27 Release Notes → Swift Compiler, Simulator].

#### 6.11. Xcode как зависимость

- Версия Xcode — **часть контракта сборки**: она определяет Swift, SDK и поведение тестов.
  Файл `.xcode-version` + `xcodes` (2.1.0, MIT) — стандарт де-факто.
- Между минорными версиями Xcode меняется рендер → **снапшоты дрейфуют**.
- Xcode 27 требует macOS 26.6+ и Apple silicon → машина/арендованный Mac обязан обновляться.

#### 6.12. Миграция на Swift 6 / data-race

- Типовые ошибки: «Sending value of non-Sendable type», «Main actor-isolated property can not be
  referenced from a nonisolated context», конформансы протоколов без изоляции.
- В 6.2+ с `-default-isolation MainActor` **большинство этих ошибок исчезает** — код
  по умолчанию на главном акторе. Цена: любая настоящая фоновая работа требует явного
  `@concurrent`/`Task.detached`/actor.
- Известная проблема тулчейна: **краш оптимизатора** при `-default-isolation MainActor` и `-O`
  [~ github.com/swiftlang/swift/issues/88173] — проверить актуальность на 6.4.
- Библиотеки без `Sendable`-аннотаций тянут `@preconcurrency import` — это допустимый временный
  шов, но он должен быть виден (правило: `@preconcurrency` требует комментария-почему).

#### 6.13. SwiftData — подводные камни

**[~ вторичные источники, Apple о проблемах не пишет]**
- Миграции: `VersionedSchema` + `SchemaMigrationPlan`; конфликты «разные таргеты с разными
  определениями модели» ломают базу.
- CloudKit: схема не появляется в production, пока вручную не нажать «Deploy Schema Changes» в
  CloudKit Dashboard; ограничения CloudKit (нет `unique`, все свойства опциональны или с
  дефолтом) протекают в модель SwiftData.
- Потоки: `ModelContext` привязан к актору; фоновая обработка требует `ModelActor`.
- Слезть со SwiftData на GRDB/SQLite потом — **дорого**, особенно с включённым CloudKit.
- iOS 26 добавил наследование моделей.

> **[?] Для нашего профиля** (offline-tolerant, данные — зеркало серверного состояния) SwiftData
> не нужен вовсе: кэш «последний успешный ответ» в файле + in-memory — проще, тестируемее и
> работает на Linux. Оставить как «опция, если заказчику нужен offline-first».

#### 6.14. Симулятор vs устройство

- Не воспроизводятся: реальная память/термалка, камера (частично), биометрия (симулируется),
  push (в симуляторе — только через `simctl push` с файлом payload), Keychain-ACL с биометрией,
  фоновые режимы, реальная сеть.
- `simctl`/`devicectl` в Xcode 27 умеют `reboot` [✓ release notes].
- Известная проблема: «Some simulator runtimes are not completely deleted when removed,
  re-appearing after a reboot» (radar 141290052) [✓ Xcode 27 Release Notes → Simulator → Known
  Issues] — в CI это заполняет диск.
- **Interface Builder в Xcode 27 больше не требует симулятора для компиляции xib/storyboard**
  (новый режим `toolchain`, включён по умолчанию; откат — `IBC_COCOATOUCH_COMPILER_MODE =
  simulator`) — «especially useful for build servers»
  [✓ Xcode 27 Release Notes → Interface Builder]. Для нас неважно (storyboard'ов не будет), но
  показывает направление: Apple убирает симулятор из сборочного пути.

#### 6.15. Xcode Cloud, macOS-only CI, App Store Connect API

- **CI для iOS требует macOS** — это не обходится (кроме pure-Swift частей на Linux).
- **Xcode Cloud** — Apple-хостинг, привязан к App Store Connect, тарифицируется по compute-часам,
  конфигурируется только через App Store Connect UI/API. Для экосистемы без SaaS — **не наш
  путь**, но как fallback для заказчика упомянуть стоит (поток 05).
- **App Store Connect API** — `.p8`-ключ (Issuer ID + Key ID), JWT ES256; позволяет создавать
  версии, загружать сборки (`altool`/`notarytool`/`xcrun altool` → `xcrun altool --upload-app`
  или `fastlane deliver/pilot`), управлять TestFlight.
- **TestFlight** [~ вторичные источники]: сборка живёт **90 дней**; внутренние тестировщики —
  до 100 (члены команды в App Store Connect), внешние — до 10 000 по e-mail/публичной ссылке;
  первая сборка каждой версии для внешних групп проходит **beta review**.

#### 6.16. Локализация

- `.xcstrings` (String Catalog) — канон; plural/варианты живут внутри каталога.
- В Xcode 27 экспорт локализаций извлекает `NSLocalizedString` **и из header-файлов**;
  строки с комментарием «do not translate» автоматически помечаются `Don't Translate` и
  `translate="no"` в XLIFF [✓ Xcode 27 Release Notes → Localization].
- Русские plural-правила (one/few/many/other) — каталог поддерживает; **тестировать надо**
  (типичный баг: «1 задача / 2 задачи / 5 задач»).
- `BUILD_ONLY_KNOWN_LOCALIZATIONS = YES` — чтобы в бандл не попали локали зависимостей.
- Превью в другой локализации — Xcode 27 [✓ там же].

#### 6.17. iPad / multitasking

- Apple требует поддержки всех ориентаций и Split View для iPad-приложений; отказ — через
  `UIRequiresFullScreen` (и это сигнал ревью).
- «Make your interface adaptable to changing window sizes» — прямая рекомендация Liquid Glass
  [✓ Apple, liquid-glass overview].
- Практически: `NavigationSplitView` + size-class-адаптив с первого дня, иначе переписывать.

#### 6.18. Что особенно болезненно **для AI-агента**

| Проблема | Почему больно агенту | Митигация в шаблоне |
|---|---|---|
| **Нужен Mac** | половина гейта недоступна на Linux-хосте | максимизировать pure-Swift слой; `make check-fast` на Linux — полноценный, не «урезанный» |
| **Шум `xcodebuild`** | тысячи строк, ошибка тонет | `xcbeautify` обязателен; `-quiet` где можно; парсинг `.xcresult` вместо чтения лога |
| **Долгая сборка** | цикл «правка → фидбек» в минутах, агент теряет контекст | pure-Swift `swift test` как основной цикл; `build-for-testing`/`test-without-building` |
| **Недетерминизм симулятора** | тесты «иногда» падают → агент делает ложные выводы | фиксированный симулятор/OS; `-parallel-testing-enabled NO` для snapshot; `--repeat-until pass` только для диагностики, не в гейте |
| **Дрейф снапшотов** | агент «чинит» код вместо перезаписи эталона | отдельная явная команда `make snapshot-record`; правило «снапшот не перезаписывается без причины в коммите» |
| **`.pbxproj`** | агент не должен его редактировать руками | buildable folders + модульность в `Package.swift`; правило «`.pbxproj` правится только Xcode» |
| **Подпись** | интерактивна, секретна | в `.claude/settings.json` — deny на всё, что трогает сертификаты; подпись — ручная операция человека |
| **Много «правильных» архитектур** | агент склонен смешивать паттерны | жёсткие инварианты в `CLAUDE.md` + `/scaffold`, который генерирует канон |
| **Xcode-only рефакторинги** | переименование файла/таргета | всё, что можно, — через файловую систему; таргеты трогать редко |

**Что, наоборот, стало лучше именно для агентов (Xcode 27)**
[✓ Xcode 27 Release Notes → Coding Intelligence]:
- **Xcode MCP server**: агенты могут **бутить симуляторы, ставить и запускать приложения,
  синтезировать тач-события, снимать скриншоты**, читать/менять run state, работать с
  debugger console, переключать схемы и run destination, читать/менять build settings,
  entitlements и `Info.plist` (radars 175179787, 176935844).
- `RenderPreview` / Preview Snapshot MCP-инструмент рендерит превью в вариантах
  (light/dark, ориентация, размеры шрифта) и возвращает платформу/устройство/OS симулятора.
- MCP-инструменты для String Catalogs (чтение/план/правка переводов).
- Поддержка **Agent Client Protocol** и плагинов со скиллами/MCP-серверами; slash-команды.
- Слой безопасности, контролирующий доступ агентов к файловой системе.

> **[?] Стратегический вывод:** Apple в 2026 официально встроила агентов в Xcode и открыла MCP.
> Для нашей экосистемы это значит: **на Mac у агента есть полноценный инструментарий**, и
> шаблон должен быть к нему готов (описать в `CLAUDE.md`, какие MCP-инструменты ожидаются).
> На Linux — только pure-Swift часть. Детальную проверку доступности Xcode MCP вне Xcode
> (как отдельного сервера) надо вынести в поток 06.

---

## Рекомендации для шаблона (черновики)

> Всё ниже — **черновик**, не финальный дизайн (финал — поток 08). Цель: чтобы по этому разделу
> можно было начать писать код без повторного ресёрча.

### 7.1. Дерево репозитория (черновик)

```
ios-template/
├─ CLAUDE.md                          # ядро + роутер (≤ 200 строк), по образцу go/spa
├─ Makefile                           # единый словарь агента: make <verb>
├─ .xcode-version                     # 27.0
├─ .swiftlint.yml
├─ .swift-format
├─ .test-simulator                    # device + OS для snapshot-плана (одна строка каждой)
├─ ios-template.xcodeproj             # тонкий проект: App, AppTests, AppUITests
├─ App/
│  ├─ AppMain.swift                   # @main + composition root
│  ├─ AppEnvironment.swift            # live/mock сборка зависимостей
│  ├─ RootView.swift                  # NavigationSplitView/Stack + Router
│  ├─ DeepLink.swift                  # thin wrapper: URL → Core.Routing
│  ├─ Resources/{Assets.xcassets, Localizable.xcstrings}
│  ├─ Info.plist
│  └─ PrivacyInfo.xcprivacy
├─ Packages/
│  ├─ Core/Package.swift
│  │  └─ Sources/{Domain, Routing, Networking, Persistence, AppFoundation, DesignSystem}
│  ├─ Features/Package.swift
│  │  └─ Sources/{AuthFeature, ProfileFeature, TodosFeature, ThemeFeature}
│  └─ TestSupport/Package.swift
├─ Config/{Base,Dev,Staging,Prod}.xcconfig
├─ TestPlans/{Unit,Snapshot,UI}.xctestplan
├─ e2e/                               # Maestro YAML (опционально, парно с Android)
├─ Tools/archlint/                    # валидатор графа модулей (SwiftPM executable)
├─ scripts/
├─ docs/{README.md, reference/architecture.md, stack/, getting-started.md}
└─ .claude/{rules,skills,plans,settings.json}
```

**Reference-фичи — зеркальные SPA-шаблону** (`features/auth`, `pages/profile`, `pages/todos`,
`features/theme`):

| Модуль | Что демонстрирует |
|---|---|
| `AuthFeature` | форма логина, валидация, ошибки сервера, хранение сессии в Keychain, экран удаления аккаунта (guideline 5.1.1(v)) |
| `ProfileFeature` | загрузка/редактирование профиля, смена пароля, состояние «loading/error/empty» |
| `TodosFeature` | список + пагинация + pull-to-refresh + деталь + создание/удаление, deep link `stit://todos/<id>` и universal link |
| `ThemeFeature` | переключение light/dark/system поверх токен-слоя; демонстрация `Theme` через `Environment` |

### 7.2. `Packages/Core/Package.swift` (черновик)

```swift
// swift-tools-version: 6.2
import PackageDescription

let package = Package(
  name: "Core",
  defaultLocalization: "ru",
  platforms: [.iOS(.v18)],
  products: [
    .library(name: "Domain",        targets: ["Domain"]),        // pure-Swift  → Linux
    .library(name: "Routing",       targets: ["Routing"]),       // pure-Swift  → Linux
    .library(name: "Networking",    targets: ["Networking"]),    // pure-Swift  → Linux
    .library(name: "AppFoundation", targets: ["AppFoundation"]), // pure-Swift  → Linux
    .library(name: "Persistence",   targets: ["Persistence"]),   // Security.framework
    .library(name: "DesignSystem",  targets: ["DesignSystem"]),  // SwiftUI
  ],
  dependencies: [
    .package(url: "https://github.com/pointfreeco/swift-dependencies", from: "1.17.1"),
  ],
  targets: [
    .target(name: "AppFoundation"),
    .target(name: "Domain", dependencies: ["AppFoundation"]),
    .target(name: "Routing", dependencies: ["Domain"]),
    .target(
      name: "Networking",
      dependencies: [
        "Domain", "AppFoundation",
        .product(name: "Dependencies", package: "swift-dependencies"),
        .product(name: "DependenciesMacros", package: "swift-dependencies"),
      ]
    ),
    .target(name: "Persistence", dependencies: ["Domain", "AppFoundation"]),
    .target(name: "DesignSystem", resources: [.process("Resources")]),

    .testTarget(name: "DomainTests",     dependencies: ["Domain"]),
    .testTarget(name: "RoutingTests",    dependencies: ["Routing"]),
    .testTarget(name: "NetworkingTests", dependencies: ["Networking"]),
  ]
)

// Единая политика конкурентности и строгости для всех таргетов пакета.
for target in package.targets {
  var s = target.swiftSettings ?? []
  s += [
    .defaultIsolation(MainActor.self),                        // SE-0466
    .enableUpcomingFeature("NonisolatedNonsendingByDefault"), // SE-0461
    .enableUpcomingFeature("InferIsolatedConformances"),      // SE-0470
    .enableUpcomingFeature("ExistentialAny"),
    .enableUpcomingFeature("InternalImportsByDefault"),
    .enableUpcomingFeature("MemberImportVisibility"),
    .swiftLanguageMode(.v6),
    .treatAllWarnings(as: .error),
  ]
  target.swiftSettings = s
}
```

> ⚠️ `.treatAllWarnings(as:)` и `.swiftLanguageMode(_:)` доступны с определённых tools-version —
> **проверить на живом тулчейне (поток 06)**; при недоступности заменить на
> `.unsafeFlags(["-warnings-as-errors"])` (но `unsafeFlags` запрещают публикацию пакета — для
> локального пакета это ок).

### 7.3. `Packages/Features/Package.swift` (фрагмент, одна фича)

```swift
// swift-tools-version: 6.2
import PackageDescription

let package = Package(
  name: "Features",
  defaultLocalization: "ru",
  platforms: [.iOS(.v18)],
  products: [
    .library(name: "TodosFeature", targets: ["TodosFeature"]),
    // …
  ],
  dependencies: [
    .package(path: "../Core"),
    .package(path: "../TestSupport"),
    .package(url: "https://github.com/pointfreeco/swift-snapshot-testing", from: "1.19.5"),
  ],
  targets: [
    .target(
      name: "TodosFeature",
      dependencies: [
        .product(name: "Domain",       package: "Core"),
        .product(name: "Routing",      package: "Core"),
        .product(name: "Networking",   package: "Core"),
        .product(name: "DesignSystem", package: "Core"),
      ],
      resources: [.process("Resources")]   // Localizable.xcstrings фичи
    ),
    // Логика экрана вынесена в отдельный pure-Swift таргет → тестируется на Linux.
    .testTarget(
      name: "TodosFeatureTests",
      dependencies: [
        "TodosFeature",
        .product(name: "TestSupport", package: "TestSupport"),
        .product(name: "SnapshotTesting", package: "swift-snapshot-testing"),
      ]
    ),
  ]
)
```

> **Проектное решение, которое стоит принять осознанно:** держать «модель экрана» (`@Observable`
> класс без импорта SwiftUI) в **отдельном таргете** `TodosFeatureCore`, а `TodosFeature` —
> только `View` + превью. Тогда 80 % тестов фичи гоняются `swift test` на Linux.
> Цена — вдвое больше таргетов. **[?] Решение владельца.**

### 7.4. `.swiftlint.yml` (черновик; идея — аналог `forbidigo` + `depguard`)

```yaml
strict: true          # эквивалент --strict, warnings → errors

analyzer_rules:
  - unused_declaration
  - unused_import

disabled_rules:
  - trailing_whitespace      # покрыто swift-format
  - todo

opt_in_rules:
  - anonymous_argument_in_multiline_closure
  - closure_spacing
  - collection_alignment
  - contains_over_filter_count
  - empty_collection_literal
  - empty_count
  - empty_string
  - explicit_init
  - fallthrough
  - file_name
  - first_where
  - force_unwrapping           # в проде запрещено, в тестах — исключение ниже
  - implicitly_unwrapped_optional
  - last_where
  - multiline_arguments
  - multiline_parameters
  - operator_usage_whitespace
  - overridden_super_call
  - prefer_self_in_static_references
  - private_action
  - redundant_nil_coalescing
  - shorthand_optional_binding
  - sorted_imports
  - toggle_bool
  - unowned_variable_capture
  - yoda_condition

included:
  - App
  - Packages
  - Tools
excluded:
  - .build
  - "**/.build"
  - "**/Generated"
  - "**/__Snapshots__"

line_length: { warning: 120, error: 160, ignores_urls: true }
type_body_length: { warning: 300, error: 400 }
function_body_length: { warning: 60, error: 100 }
cyclomatic_complexity: { ignores_case_statements: true }
identifier_name: { excluded: [id, ok, to] }

custom_rules:
  no_print:
    name: "print запрещён"
    regex: '\b(print|debugPrint|dump)\s*\('
    match_kinds: [identifier]
    message: "Используй AppFoundation.log (OSLog), не print"
    severity: error

  no_combine:
    name: "Combine запрещён"
    regex: '^\s*import\s+Combine'
    message: "Реактивные цепочки — AsyncSequence/AsyncStream, не Combine"
    severity: error

  no_raw_urlsession:
    name: "URLSession только в Networking"
    regex: 'URLSession\s*\('
    match_kinds: [identifier]
    message: "Сетевые вызовы — через Core.Networking, не напрямую URLSession"
    severity: error
    # включать в included только App/Packages/Features — см. заметку ниже

  no_userdefaults_for_secrets:
    name: "секреты не в UserDefaults"
    regex: 'UserDefaults[^\n]*(token|password|secret|credential)'
    message: "Токены и секреты — только Keychain (Core.Persistence)"
    severity: error

  no_hardcoded_color:
    name: "сырые цвета запрещены"
    regex: 'Color\(\s*(red|hex|#)'
    message: "Используй семантический токен из DesignSystem.Theme"
    severity: error

  no_fixed_font_size:
    name: "фиксированный размер шрифта"
    regex: '\.font\(\s*\.system\(size:'
    message: "Dynamic Type: .font(.body) или Font.custom(_, relativeTo:)"
    severity: warning

  explicit_stack_spacing:
    name: "явный spacing в стеках"
    regex: '(?-s)(VStack|HStack)((?!spacing:).)*\s*\{'
    match_kinds: [identifier]
    message: "Указывай spacing: явно (токен из Theme.metrics)"
    severity: warning

  no_force_try:
    name: "try! запрещён"
    regex: 'try!\s'
    message: "try! только в тестах"
    severity: error
```

> **Ограничение SwiftLint:** `custom_rules` не умеют «эта регулярка применяется только к такому
> каталогу». Обход, который использует Element X — **два конфига**: корневой `.swiftlint.yml`
> и `Packages/Core/Sources/Networking/.swiftlint.yml` (вложенный конфиг переопределяет правила
> для поддерева). Для `no_raw_urlsession` это обязательно.

### 7.5. `.swift-format` (черновик)

```json
{
  "version": 1,
  "lineLength": 120,
  "indentation": { "spaces": 2 },
  "maximumBlankLines": 1,
  "respectsExistingLineBreaks": true,
  "lineBreakBeforeControlFlowKeywords": false,
  "lineBreakBeforeEachArgument": false,
  "prioritizeKeepingFunctionOutputTogether": true,
  "indentConditionalCompilationBlocks": false,
  "rules": {
    "AllPublicDeclarationsHaveDocumentation": false,
    "AlwaysUseLowerCamelCase": true,
    "AmbiguousTrailingClosureOverload": true,
    "NoBlockComments": true,
    "NoLeadingUnderscores": false,
    "OrderedImports": true,
    "UseLetInEveryBoundCaseVariable": true,
    "UseShorthandTypeNames": true,
    "UseSynthesizedInitializer": true,
    "UseWhereClausesInForLoops": false,
    "ValidateDocumentationComments": true
  }
}
```

Проверка: `swift format lint --strict --parallel --recursive App Packages Tools`;
форматирование: `swift format --in-place --parallel --recursive App Packages Tools`.

### 7.6. `Makefile` — словарь вербов (черновик)

```
make help            # список
make setup           # хуки + проверка .xcode-version + swiftlint/…
make fmt             # swift format --in-place
make fmt-check       # swift format lint --strict           (Linux ✓)
make lint            # swiftlint --strict                   (Linux ✓)
make archlint        # валидатор графа модулей              (Linux ✓)
make test            # swift test по всем локальным пакетам (Linux ✓, только pure-Swift)
make cover-check     # пол покрытия по pure-Swift профилю   (Linux ✓)
make check-fast      # fmt-check + lint + archlint + test + cover-check
make build           # xcodebuild build -scheme "App Dev"        (macOS)
make test-app        # xcodebuild test -testPlan Unit            (macOS)
make test-snapshot   # xcodebuild test -testPlan Snapshot        (macOS)
make snapshot-record # перезапись эталонов (осознанное действие) (macOS)
make test-ui         # xcodebuild test -testPlan UI              (macOS)
make e2e             # maestro test e2e/                          (macOS)
make check           # check-fast + build + test-app + test-snapshot (macOS)
make archive         # xcodebuild archive + exportArchive          (macOS, подпись)
make upload          # TestFlight / App Store Connect              (macOS, ручное подтверждение)
make status / map / search   # интел для /smartdev
make validate-stack-docs     # валидатор docs/stack
```

### 7.7. Тест-план `Unit.xctestplan` (набросок ключевых настроек)

```
defaultOptions:
  codeCoverage: { targets: [Domain, Routing, Networking, *FeatureCore] }
  testExecutionOrdering: random
  testTimeoutsEnabled: true
  maximumTestExecutionTimeAllowance: 120
  language: ru        # + отдельная конфигурация en
  region: RU
testTargets: [CoreTests, FeaturesCoreTests]
```

`Snapshot.xctestplan`: `parallelizable: false`, фиксированная локаль/регион/оформление,
`codeCoverage: false`.

### 7.8. Правила `.claude/rules/` (предлагаемый набор)

| Файл | Аналог в go/spa | Содержание |
|---|---|---|
| `module-architecture.md` | `domain-architecture` / `fsd-architecture` | категории модулей (App/Feature/Core/TestSupport), матрица разрешённых зависимостей, запрет Feature→Feature, `package`-видимость, где живут домен/use-case/DTO |
| `swift-concurrency.md` | — (новое) | MainActor-by-default, когда `@concurrent`, запрет Combine, `Sendable`-политика, `@preconcurrency` только с комментарием |
| `tests-pipeline.md` | одноимённый | Swift Testing vs XCTest, что обязано иметь тест, где лежат снапшоты, фиксированный симулятор, `make snapshot-record`, coverage-пол |
| `design-tokens.md` | `shared-ui` | токен-слой, запрет сырых цветов/размеров, Liquid Glass: что можно кастомизировать, Dynamic Type, a11y-инварианты |
| `signing-and-secrets.md` | `env-local` + `infra-impact` | Keychain-политика, запрет секретов в репо/`Info.plist`, `.p8`/сертификаты вне репо, privacy manifest при добавлении зависимости, ATS-исключения запрещены |
| `env-and-config.md` | `env-local` | `xcconfig` per env, `Info.plist`-мост, что build-time / что нет, deep-link-схемы по средам |
| `comments.md` | одноимённый | только WHY, по-русски |
| `forward-only.md` | одноимённый | без «было/стало» |
| `store-compliance.md` | — (новое) | 4.2 / 5.1.1(v) удаление аккаунта / 2.1 demo-mode / privacy manifest / ATT — чек-лист перед релизом |

Все — **path-scoped** через `paths:` (как в go-backend-template), чтобы грузились по контексту.

### 7.9. Инварианты для `CLAUDE.md` (проект списка)

**Делать всегда**
- перед claim «готово» — `make check` (на Linux — `make check-fast`, и это проговаривается);
- изменение — через план (`/plan`, единый `.claude/plans/roadmap.md`);
- зависимость фичи — **порт (протокол) в фиче, реализация в `Core`**; cross-feature — только
  через `Core` или композицию в `App`;
- всё на `MainActor`; уход — `@concurrent` + комментарий-почему;
- новый экран — через `/scaffold` (View + Model + Route + тесты + снапшот-заглушка);
- добавил стороннюю зависимость — проверь её `PrivacyInfo.xcprivacy` и запиши в
  `docs/stack/dependencies.md`.

**Сначала спросить**
- изменение `IPHONEOS_DEPLOYMENT_TARGET`, добавление таргета/extension, правка `.pbxproj`;
- любая новая внешняя зависимость;
- перезапись snapshot-эталонов (`make snapshot-record`);
- всё, что касается сертификатов/профилей/`.p8`.

**Не делать никогда**
- `print`/`debugPrint` вместо логгера; `try!`/`force unwrap` вне тестов;
- `import Combine`; импорт внешней дизайн-системы;
- сырые цвета/размеры вместо токенов; кастомный фон системных баров;
- токены в `UserDefaults`; `NSAllowsArbitraryLoads`;
- коммитить `*.p8`, `*.p12`, `*.mobileprovision`, `*.cer`, `.env*`;
- редактировать `.pbxproj` руками; редактировать сгенерированный OpenAPI-код.

### 7.10. `docs/stack/` — kinds для iOS (перенос идеи 8 kinds)

| kind | что описывает |
|---|---|
| `service.md` | app-id, bundle id по средам, project-slug/strategy-path/surface, deployment target |
| `runtime.md` | Swift/Xcode/iOS версии, concurrency-политика, минимальный симулятор |
| `config.md` | `xcconfig` per env, ключи `Info.plist`, deep-link схемы, entitlements |
| `navigation.md` | карта `Route`, deep links, AASA-пути (замена `routing.md` SPA) |
| `api.md` | контракт с бэкендом: auth (cookie vs bearer), базовые URL, кодоген или рукописные DTO |
| `design.md` | токены, темы, Liquid Glass-политика, иконка (Icon Composer), a11y |
| `observability.md` | OSLog/Sentry, crash-репортинг, метрики (MetricKit) |
| `dependencies.md` | каждая внешняя зависимость + лицензия + privacy manifest |
| `distribution.md` | сторы, подпись, TestFlight, версионирование (может уйти в поток 05) |

### 7.11. Таблица «Linux / Mac / устройство»

| Операция | Linux | macOS | Реальное устройство |
|---|---|---|---|
| `swift build` / `swift test` (pure-Swift) | ✅ | ✅ | — |
| Swift Testing | ✅ | ✅ | — |
| `swift format lint` | ✅ | ✅ | — |
| `swiftlint --strict` | ✅ | ✅ | — |
| `swiftlint analyze` (unused_declaration) | ❌ (нужен compiler log Xcode) | ✅ | — |
| `archlint` (свой валидатор графа) | ✅ | ✅ | — |
| coverage по pure-Swift (`swift test --enable-code-coverage` + `llvm-cov`) | ✅ | ✅ | — |
| Компиляция под iOS (`xcodebuild`) | ❌ | ✅ | — |
| Тесты SwiftUI-вью, ViewInspector | ❌ | ✅ | — |
| Snapshot `.image` | ❌ | ✅ | — |
| Snapshot текстовые (`.json`, `.raw`, `.dump`) | ✅ | ✅ | — |
| XCUITest | ❌ | ✅ (симулятор) | ✅ |
| Maestro | ❌ | ✅ (симулятор) | ❌ (нет поддержки реальных iOS-устройств) |
| Previews / Instruments | ❌ | ✅ | частично |
| Periphery | ❌ | ✅ | — |
| Подпись, archive, export IPA | ❌ | ✅ | — |
| TestFlight / App Store upload | ❌ | ✅ | — |
| Push (реальный APNs) | ❌ | частично (`simctl push` — локальный payload) | ✅ |
| Биометрия, камера, фоновые режимы | ❌ | симуляция | ✅ |
| Universal Links (реальная проверка CDN) | ❌ | частично | ✅ |

---

## Ловушки — быстрый справочник

| # | Ловушка | Версия / контекст | Что делать | Источник |
|---|---|---|---|---|
| 1 | `UIDesignRequiresCompatibility` **игнорируется** при сборке под iOS 27+ | iOS 27 / Xcode 27 | планировать Liquid Glass как данность, не как опцию | [Apple: UIDesignRequiresCompatibility](https://developer.apple.com/documentation/bundleresources/information-property-list/uidesignrequirescompatibility) |
| 2 | С **апреля 2027** App Store Connect требует iOS 27 SDK | объявлено 09.09.2026 | собирать Xcode 27 уже сейчас | [Apple News k1mtkt1k](https://developer.apple.com/news/?id=k1mtkt1k) |
| 3 | **macOS 27 — только Apple silicon**, Xcode 27 требует macOS 26.6+ | — | Mac под сборку обязан быть ARM и обновляться | [Apple News k1mtkt1k], [Xcode 27 RN](https://developer.apple.com/documentation/xcode-release-notes/xcode-27-release-notes) |
| 4 | Source break SE-0508: computed property с `init` accessor **после** геттера не компилируется | Xcode 27, radar 180969028 | объявлять `init` accessor раньше геттера | Xcode 27 RN → Swift Compiler → Known Issues |
| 5 | Dependency scanner требует **уникальных имён Clang-модулей** | Xcode 27, radar 136303612 | вычистить дублирующие `module.modulemap` | Xcode 27 RN → Swift Compiler |
| 6 | `ld64` удалён, `-ld_classic` больше не поддерживается | Xcode 27, radar 165165518 | убрать флаг из скриптов | Xcode 27 RN → Linking |
| 7 | **On Demand Resources / `NSBundleResourceRequest` deprecated** | Xcode 27, radar 170066290 | использовать Background Assets | Xcode 27 RN → On Demand Resources |
| 8 | Hardware-Checked Pointer Arithmetic + automatic signing → **нельзя загрузить в App Store** на macOS 26.6 | Xcode 27, radar 187057599 | macOS 27 или ручная подпись | Xcode 27 RN → Enhanced Security |
| 9 | Снапшот сравнивается только с **того же** симулятора; иначе диффы | swift-snapshot-testing 1.19.5 | фиксировать device+OS, отдельная команда re-record | [README](https://github.com/pointfreeco/swift-snapshot-testing) |
| 10 | Swift Testing **параллелит тесты по умолчанию** | Swift 6.x | `.serialized` для suite с общим состоянием | [Apple Testing docs](https://developer.apple.com/documentation/testing) |
| 11 | `XCTAssert*` внутри хелпера из `@Test` теперь **валит тест** | Swift 6.4, interop `complete` | пересмотреть смешанные хелперы; `SWIFT_TESTING_XCTEST_INTEROP_MODE` | [Apple: migratingfromxctest](https://developer.apple.com/documentation/testing/migratingfromxctest) |
| 12 | `xcresulttool get object --format json` **deprecated**, нужен `--legacy` | с Xcode 16 | перейти на `xcresulttool get test-results …` или `xcresultparser` | [fastlane#22132](https://github.com/fastlane/fastlane/issues/22132), [forums 763050](https://developer.apple.com/forums/thread/763050) |
| 13 | `//` в `.xcconfig` — комментарий, ломает URL | все версии | `https:/$()/host` | [~ общеизвестный приём] |
| 14 | AASA раздаётся через **CDN Apple**: обновление до 24 ч, устройства проверяют ~раз в неделю | iOS 14+ | `?mode=developer` для dev; не ждать мгновенного эффекта | [Apple: supporting-associated-domains](https://developer.apple.com/documentation/xcode/supporting-associated-domains) |
| 15 | Каждый сабдомен требует **своего** AASA и своей записи в entitlement | iOS 14+ | — | там же |
| 16 | Keychain **переживает удаление приложения** | все версии | очистка на первом запуске по флагу в `UserDefaults` | [~ общеизвестно] |
| 17 | `UserDefaults` — **required reason API**; без объявления сборка не принимается | с 01.05.2024 | `NSPrivacyAccessedAPICategoryUserDefaults` + код причины | [Apple: describing-use-of-required-reason-api](https://developer.apple.com/documentation/bundleresources/describing-use-of-required-reason-api) |
| 18 | Невалидный privacy manifest (лишние ключи) → **реджект сабмишена** | с 12.02.2025 | валидировать; проверять манифесты зависимостей | [Apple: adding-a-privacy-manifest…](https://developer.apple.com/documentation/bundleresources/adding-a-privacy-manifest-to-your-app-or-third-party-sdk) |
| 19 | Приложение с регистрацией **обязано** иметь удаление аккаунта в приложении | 5.1.1(v) | экран удаления в reference-фиче Auth | [App Review Guidelines](https://developer.apple.com/app-store/review/guidelines/) |
| 20 | Login-only B2B-приложение без demo-доступа заворачивают | 2.1 + 4.2 | встроенный mock/demo-режим + описание в App Review notes | там же |
| 21 | APNs: team-scoped ключей максимум **2 на среду**; токен переподписывать не реже раза в час | — | планировать управление ключами | [Apple: token-based connection to APNs](https://developer.apple.com/documentation/usernotifications/establishing-a-token-based-connection-to-apns) |
| 22 | TestFlight-сборка живёт **90 дней** | — | регулярные загрузки при длинных бетах | [~ вторичные источники] |
| 23 | `sheet`/`fullScreenCover` не стекируются | SwiftUI все версии | один слой презентации, вложенность внутри контента | [~ отраслевой опыт] |
| 24 | `NavigationLink(value:)` без `navigationDestination(for:)` в том же стеке — молча ничего | SwiftUI | правило в rules + тест роутинга | [~] |
| 25 | `.id(...)` сбрасывает `@State` | SwiftUI | не использовать как «обнови» | [~] |
| 26 | `Periphery` архивирован, стал коммерческим | 12.08.2026 | пиннить 3.8.0 или заменить `swiftlint analyze` | [periphery.pro](https://periphery.pro/) |
| 27 | Симуляторные рантаймы не удаляются до конца, возвращаются после перезагрузки | radar 141290052 | чистить диск в CI | Xcode 27 RN → Simulator → Known Issues |
| 28 | Swift 6 + `-default-isolation MainActor` + `-O`: краш оптимизатора | Swift 6.2-эпоха | проверить на 6.4; issue открыт | [swift#88173](https://github.com/swiftlang/swift/issues/88173) |
| 29 | SwiftLint `custom_rules` не умеют path-scoping | 0.65.1 | вложенные `.swiftlint.yml` в подкаталогах | [SwiftLint README](https://github.com/realm/SwiftLint) |
| 30 | `swift-format` артефакт-бандлы только под macOS; на Linux — через тулчейн | 604.0.0 | `swift format` (subcommand), не отдельный бинарь | [swift-format README](https://github.com/swiftlang/swift-format) |

---

## Риски и открытые вопросы владельцу

### Требуют решения владельца

1. **Deployment target.** iOS 18 (шире охват, минус часть API iOS 26: новые Liquid Glass-API,
   `@Entry`-удобства и т. д.) **vs** iOS 26 (чище код, но отсекает часть парка). Предложение
   отчёта — **iOS 18.0**, с явным «подними до 26, если парк заказчика управляемый».
2. **Auth-контракт с Go-бэкендом.** Бэкенд stit — **cookie-only**, `Authorization: Bearer` не
   поддерживается. На iOS это неудобно (cookies не в Keychain). Варианты: (а) научить
   `golibinfra/authnkit` bearer-режиму для мобильных; (б) на клиенте вручную вынимать
   `refresh_token`-cookie в Keychain; (в) отдельный mobile-endpoint. **Требует решения до
   реализации `AuthFeature`** (детально — поток 07).
3. **Periphery.** Пиннить архивный OSS-3.8.0 (MIT), выкинуть, или заменить на
   `swiftlint analyze`? Влияет на состав гейта.
4. **`swift format` vs SwiftFormat.** Отчёт рекомендует `swift format` (в тулчейне).
   Если владелец хочет более агрессивное форматирование — SwiftFormat 0.63.0, но +1 бинарь.
5. **Разделение «модель экрана» в отдельный pure-Swift таргет** (см. 7.3). Даёт агенту
   Linux-цикл на 80 % тестов фичи, цена — вдвое больше таргетов и чуть больше церемонии.
6. **Tuist/XcodeGen** — отчёт рекомендует «без генератора». Если у владельца есть опыт боли с
   `.pbxproj` в команде — можно сразу взять XcodeGen.
7. **Maestro как общий e2e с Android** — решение зависит от потока 03 (одна кодовая база или
   две). Если два нативных шаблона — общий Maestro-слой становится сильным связующим звеном.
8. **STOP-граница мобильного репо.** Для backend/frontend это «image in registry». Для iOS
   кандидаты: (а) подписанный `.ipa` + артефакт в Gitea Releases; (б) загрузка в TestFlight;
   (в) тег + handoff человеку. **Ключевое ограничение: подпись требует Mac и секретов,
   которых у экосистемы нет в автоматике.**
9. **Кто и где держит Mac.** Без Mac невозможно: собрать, подписать, прогнать UI/snapshot,
   загрузить в стор. Это инфраструктурное решение (свой Mac mini / аренда / машина
   разработчика), от него зависит вся модель релиза.

### Требуют дополнительной проверки (эмпирика, поток 06)

- Доступна ли `Observation` (`@Observable`) в pure-Swift таргете **на Linux** в Swift 6.4
  (ожидается «да» — это часть стандартной библиотеки, но не проверено).
- Работают ли `.treatAllWarnings(as: .error)` и `.swiftLanguageMode(.v6)` в
  `swift-tools-version: 6.2` (синтаксис SwiftPM менялся).
- Реально ли `swiftlint --strict` работает на Debian 13 с Swift 6.4-тулчейном (бинарь
  `swiftlint_linux_amd64` есть; нужен `libsourcekitdInProc.so` / `LINUX_SOURCEKIT_LIB_PATH`).
- Есть ли `swift format` **в Linux-тулчейне** Swift 6.4 (в README сказано «included in the Swift
  Toolchain», но artifact-бандлы swift-format публикуются только под macOS — нужна проверка
  `swift format --version` на Linux).
- Компилируется ли `@Observable`-модель экрана в Linux-таргете, если она не импортирует SwiftUI,
  но использует `Observation` + `Foundation` (это условие §7.3).
- Время `swift build` полного `Packages/Core` на этой машине (важно для оценки цикла агента).
- Доступен ли **Xcode MCP server** как отдельный процесс (вне GUI Xcode) — это определяет,
  сможет ли агент водить симулятор из CLI-сессии.
- Точные условия лицензии коммерческой Periphery.

### Риски

| Риск | Вероятность | Влияние | Митигация |
|---|---|---|---|
| Liquid Glass ломает кастомный UI при апгрейде SDK | высокая | среднее | минимум кастомных фонов; snapshot-тесты ловят регресс |
| Нет Mac → шаблон нельзя собрать/выпустить | **достоверность 100 %** | высокое | явно описать требование в `getting-started.md`; max-Linux-часть гейта |
| Дрейф снапшотов при каждом Xcode-апгрейде | высокая | низкое | фиксация симулятора, `make snapshot-record`, текстовые стратегии где можно |
| ViewInspector отстаёт от новой SwiftUI | средняя | низкое | не делать его обязательным |
| Periphery/иные инструменты коммерциализируются | средняя | низкое | минимизировать число внешних бинарей (отсюда выбор `swift format`) |
| Apple меняет правила ревью для B2B-приложений | средняя | среднее | `store-compliance.md` как живой чек-лист |
| Agent-friendliness iOS принципиально ниже Android | **высокая** | высокое | это вход в поток 03: если «одна кодовая база», iOS-сторона всё равно требует Mac |

---

## Сводная таблица версий

| Пакет / инструмент | Версия | Дата | Лицензия | URL |
|---|---|---|---|---|
| Xcode | 27 (27A266a) | 2026-09-14 | proprietary | https://developer.apple.com/news/releases/ |
| Xcode (бета) | 27.2 beta (27B5019j) | 2026-09-16 | proprietary | там же |
| iOS / iPadOS | 27.0 (24A437) | 2026-09-14 | proprietary | там же |
| Swift | 6.4 | 2026-09-15 | Apache-2.0 | https://www.swift.org/blog/swift-6.4-released/ |
| Swift | 6.3 | 2026-03-24 | Apache-2.0 | https://www.swift.org/blog/ |
| swift-format (Apple) | 604.0.0 | 2026-09-16 | Apache-2.0 | https://github.com/swiftlang/swift-format |
| Swift Testing | в тулчейне 6.4 | 2026-09 | Apache-2.0 | https://github.com/swiftlang/swift-testing |
| SwiftLint | 0.65.1 | 2026-08-21 | MIT | https://github.com/realm/SwiftLint |
| SwiftFormat (nicklockwood) | 0.63.0 | 2026-08-30 | MIT | https://github.com/nicklockwood/SwiftFormat |
| Periphery | 3.8.0 (репо архивирован 2026-08-12) | 2026-07-25 | MIT | https://github.com/peripheryapp/periphery |
| Tuist | monorepo, CLI | 2026-09-16 | MIT (`cli/`), MPL-2.0 (`server/`,`kura/`,`atlas/`) | https://github.com/tuist/tuist |
| XcodeGen | 2.46.0 | 2026-07-16 | MIT | https://github.com/yonaskolb/XcodeGen |
| xcodes | 2.1.0 | 2026-09-14 | MIT | https://github.com/XcodesOrg/xcodes |
| fastlane | 2.240.1 | 2026-09-15 | MIT | https://github.com/fastlane/fastlane |
| xcbeautify | 3.2.1 | 2026-04-04 | MIT | https://github.com/cpisciotta/xcbeautify |
| xcresultparser | 2.2.0 | 2026-07-18 | MIT | https://github.com/a7ex/xcresultparser |
| Maestro | cli-2.10.0 | 2026-08-31 | Apache-2.0 | https://github.com/mobile-dev-inc/maestro |
| swift-composable-architecture | 1.26.2 | 2026-08-28 | MIT | https://github.com/pointfreeco/swift-composable-architecture |
| swift-dependencies | 1.17.1 | 2026-08-28 | MIT | https://github.com/pointfreeco/swift-dependencies |
| swift-navigation | 2.11.2 | 2026-08-31 | MIT | https://github.com/pointfreeco/swift-navigation |
| swift-snapshot-testing | 1.19.5 | 2026-09-15 | MIT | https://github.com/pointfreeco/swift-snapshot-testing |
| swift-sharing | 2.10.1 | 2026-08-31 | MIT | https://github.com/pointfreeco/swift-sharing |
| ViewInspector | 0.10.3 | 2025-09-21 | MIT | https://github.com/nalexn/ViewInspector |
| swift-openapi-generator | 1.13.1 | 2026-09-01 | Apache-2.0 | https://github.com/apple/swift-openapi-generator |
| swift-openapi-runtime | 1.12.1 | 2026-09-02 | Apache-2.0 | https://github.com/apple/swift-openapi-runtime |
| swift-openapi-urlsession | 1.3.1 | 2026-06-23 | Apache-2.0 | https://github.com/apple/swift-openapi-urlsession |
| GRDB.swift | 7.11.1 | 2026-06-30 | MIT | https://github.com/groue/GRDB.swift |
| SQLite.swift | 0.16.0 | 2026-03-08 | MIT | https://github.com/stephencelis/SQLite.swift |
| Factory | 3.4.0 | 2026-09-16 | MIT | https://github.com/hmlongco/Factory |
| Swinject | 2.10.0 | 2025-09-01 | MIT | https://github.com/Swinject/Swinject |
| Needle (Uber) | 0.25.1 | 2024-09-18 | Apache-2.0 | https://github.com/uber/needle |
| Mockable | 0.6.4 | 2026-06-23 | MIT | https://github.com/Kolos65/Mockable |
| Mockolo | 2.6.1 | 2026-03-24 | Apache-2.0 | https://github.com/uber/mockolo |
| sentry-cocoa | 9.28.0 | 2026-09-10 | MIT | https://github.com/getsentry/sentry-cocoa |
| swift-log | 1.15.1 | 2026-09-08 | Apache-2.0 | https://github.com/apple/swift-log |

(Все версии сняты 2026-09-17 через GitHub releases feed / Apple developer site.)

---

## Источники

**Apple (первоисточники)**
- Xcode 27 Release Notes — https://developer.apple.com/documentation/xcode-release-notes/xcode-27-release-notes
- iOS & iPadOS 27 Release Notes — https://developer.apple.com/documentation/ios-ipados-release-notes/ios-ipados-27-release-notes
- Releases (даты сборок) — https://developer.apple.com/news/releases/
- «App Store submissions now open for the latest OS releases» (09.09.2026, April 2027 SDK-требование, macOS 27 = Apple silicon) — https://developer.apple.com/news/?id=k1mtkt1k
- Upcoming Requirements — https://developer.apple.com/news/upcoming-requirements/
- App Store — доли версий iOS/iPadOS (замер 07.06.2026) — https://developer.apple.com/support/app-store/
- `UIDesignRequiresCompatibility` — https://developer.apple.com/documentation/bundleresources/information-property-list/uidesignrequirescompatibility
- Adopting Liquid Glass — https://developer.apple.com/documentation/technologyoverviews/adopting-liquid-glass
- Liquid Glass (обзор технологии + Landmarks) — https://developer.apple.com/documentation/technologyoverviews/liquid-glass
- Managing model data in your app (`@Observable`) — https://developer.apple.com/documentation/swiftui/managing-model-data-in-your-app
- Migrating from XCTest (interop modes) — https://developer.apple.com/documentation/testing/migratingfromxctest
- Privacy manifest files — https://developer.apple.com/documentation/bundleresources/privacy-manifest-files
- Adding a privacy manifest — https://developer.apple.com/documentation/bundleresources/adding-a-privacy-manifest-to-your-app-or-third-party-sdk
- Describing use of required reason API — https://developer.apple.com/documentation/bundleresources/describing-use-of-required-reason-api
- `NSPrivacyAccessedAPIType` (список категорий) — https://developer.apple.com/documentation/bundleresources/app-privacy-configuration/nsprivacyaccessedapitypes/nsprivacyaccessedapitype
- App Review Guidelines — https://developer.apple.com/app-store/review/guidelines/
- Supporting associated domains (AASA, CDN, `?mode=developer`) — https://developer.apple.com/documentation/xcode/supporting-associated-domains
- Establishing a token-based connection to APNs — https://developer.apple.com/documentation/usernotifications/establishing-a-token-based-connection-to-apns
- `kSecAttrAccessibleAfterFirstUnlock` — https://developer.apple.com/documentation/security/ksecattraccessibleafterfirstunlock
- `xcodebuild(1)` (зеркало man-страниц) — https://keith.github.io/xcode-man-pages/xcodebuild.1.html

**Swift.org**
- Swift 6.4 Released — https://www.swift.org/blog/swift-6.4-released/
- Swift blog (6.3, Android SDK, Networking Workgroup) — https://www.swift.org/blog/

**GitHub / инструменты**
- SwiftLint — https://github.com/realm/SwiftLint
- swift-format — https://github.com/swiftlang/swift-format
- SwiftFormat — https://github.com/nicklockwood/SwiftFormat
- Periphery (архив) — https://github.com/peripheryapp/periphery · https://periphery.pro/
- Tuist (лицензия) — https://github.com/tuist/tuist/blob/main/LICENSE.md
- XcodeGen — https://github.com/yonaskolb/XcodeGen
- xcbeautify — https://github.com/cpisciotta/xcbeautify
- xcresultparser — https://github.com/a7ex/xcresultparser
- Maestro — https://github.com/mobile-dev-inc/maestro
- swift-snapshot-testing — https://github.com/pointfreeco/swift-snapshot-testing
- swift-dependencies — https://github.com/pointfreeco/swift-dependencies
- swift-navigation — https://github.com/pointfreeco/swift-navigation
- ViewInspector — https://github.com/nalexn/ViewInspector
- swift-openapi-generator — https://github.com/apple/swift-openapi-generator
- `xcresulttool` deprecation — https://github.com/fastlane/fastlane/issues/22132 · https://developer.apple.com/forums/thread/763050
- Swift optimizer crash с `-default-isolation MainActor` — https://github.com/swiftlang/swift/issues/88173

**Эталонные приложения**
- Point-Free isowords — https://github.com/pointfreeco/isowords (`Package.swift`)
- Bitwarden iOS — https://github.com/bitwarden/ios (`Mintfile`, `.swiftlint.yml`, `.swiftformat`) · архитектура: https://contributing.bitwarden.com/architecture/mobile-clients/ios/
- Element X iOS — https://github.com/element-hq/element-x-ios (`project.yml`, `.swiftlint.yml`)
- Firefox iOS — https://github.com/mozilla-mobile/firefox-ios
- DuckDuckGo iOS — https://github.com/duckduckgo/iOS
- Signal iOS — https://github.com/signalapp/Signal-iOS
- Wire iOS — https://github.com/wireapp/wire-ios
- Nextcloud iOS — https://github.com/nextcloud/ios
- WordPress iOS — https://github.com/wordpress-mobile/WordPress-iOS

**Инженерные блоги и вторичные источники** (помечены `[~]` в тексте)
- Just Eat Takeaway, «Revisiting the JET iOS Modular Architecture in 2026» — https://albertodebortoli.com/2026/07/15/revisiting-the-jet-ios-modular-architecture-in-2026/
- Qonto, «Choosing the right tool to modularize our iOS codebase» — https://medium.com/qonto-way/choosing-the-right-tool-to-modularize-our-ios-codebase-869a8ef568b8
- Tuist, «Strategies to avoid merge conflicts in Xcode Projects» — https://tuist.dev/blog/2025/03/21/git-conflicts
- Pedro Piñera, «How synchronized groups work at the .pbxproj level» — https://pepicrft.me/blog/how-synchronized-groups-work-at-the-pbxproj-level/
- SwiftLee, «Swift 6.2: A first look at how it's changing Concurrency» — https://www.avanderlee.com/concurrency/swift-6-2-concurrency-changes/
- Donny Wals, «Setting default actor isolation in Xcode 26» — https://www.donnywals.com/setting-default-actor-isolation-in-xcode-26/ · «What is Approachable Concurrency in Xcode 26?» — https://www.donnywals.com/what-is-approachable-concurrency-in-xcode-26/
- Use Your Loaf, «Approachable Concurrency in Swift Packages» — https://useyourloaf.com/blog/approachable-concurrency-in-swift-packages/
- Point-Free, «Composable Architecture Frequently Asked Questions» — https://www.pointfree.co/blog/posts/141-composable-architecture-frequently-asked-questions
- TCA build-time discussion — https://github.com/pointfreeco/swift-composable-architecture/discussions/2776
