# 03 — Сколько кодовых баз и на чём: развилка мобильного шаблона stit

Поток `03-shared-strategy`. Дата среза фактов — **2026-09-17**. Все версии и статусы
проверены в этот день через первоисточники (GitHub Releases API, Maven Central /
Google Maven metadata, официальные доки) — у каждого факта стоит пометка
«проверено (источник, дата)» либо «предположение».

Вопрос потока: **сколько кодовых баз и на чём** для мобильного шаблона экосистемы stit.
Ограничения владельца: «Swift или Kotlin, Flutter точно не хочу»; разработчик работает
на Linux, Mac под рукой нет; процесс AI-driven (агенты — основной исполнитель);
профиль — бизнес-приложения-компаньоны к Go-бэкенду, рынок РФ, open-core.

---

## TL;DR

1. **Рекомендация: один репо `mobile-template` (KMP: `shared/` + `androidApp/` + `iosApp/`),
   кандидат B — «общая логика на Kotlin, нативный UI на каждой платформе».** Матрица по
   6 критериям брифа даёт B = **86.0**, C (Compose Multiplatform как общий UI) = **81.0**,
   A (два нативных шаблона) = **75.0** из 100. B устойчив по всей области весов
   (см. § «Чувствительность»), C выигрывает только если одновременно обесценить
   «нативное качество» и поднять «долю общего кода».
2. **Главное свежее свидетельство против общей кодовой базы — разворот Shopify
   от 2026-09-10** (за неделю до среза): «Native is now the future of mobile at Shopify»,
   уход с React Native обратно на Swift + Kotlin **именно потому, что кодовые агенты
   удешевили «писать дважды»**. Это нельзя игнорировать, и в отчёте ему посвящён
   отдельный раздел (§ 3.4). Итог разбора: Shopify **не опровергает B, а подтверждает
   его архитектурный принцип** — их центральный вывод «business logic should be completely
   decoupled from the UI and be able to run headlessly on desktop… agents iterate in
   milliseconds instead of minutes» — это ровно описание KMP-модуля `shared`, только
   Shopify пришлось строить его вручную (свой CLI + Tardis + Helix), а KMP даёт
   `:shared:jvmTest` из коробки. Их измерения — «native против React Native», а не
   «native против KMP»: RN проигрывал по старту, размеру, стабильности и времени сборки,
   у KMP этих проигрышей нет. И у них есть парк Mac'ов и нативная экспертиза на обеих
   платформах — наше ограничение другое.
3. **Публичный roadmap придётся поправить**: две строки `android-template` + `ios-template`
   заменяются одной `mobile-template`. Это не потеря, а экономия: одна агентная обвязка,
   один гейт, один `docs/stack`, один мост в `<org>/strategy` вместо двух.
4. **Главный технический факт, ради которого стоило считать**: Kotlin/Native официально
   позволяет **на Linux-хосте собирать `.klib` для Apple-таргетов** (без cinterop/CocoaPods),
   но **не собирать финальные бинарники для Apple** — проверено
   (https://kotlinlang.org/docs/native-target-support.html, раздел «Hosts», обновлено
   2026-08-19). То есть `./gradlew :shared:compileKotlinIosArm64` на Linux — реалистичная
   цель; `linkDebugFrameworkIosArm64` — нет. Плюс `:shared:jvmTest` — это полноценный
   быстрый агентный цикл для всей общей логики без Mac.
5. **Agent-friendliness — не вкусовщина, а измеримое преимущество Kotlin-ветки.**
   У Kotlin есть: `llms.txt` на `kotlinlang.org` (49 КБ) и на
   `jetbrains.com/help/kotlin-multiplatform-dev` (1.5 МБ) — проверено curl'ом 2026-09-17;
   официальный **Kotlin Benchmark для AI-агентов** (105 задач, лидер — Claude Code
   Opus 4.7 xhigh, 85.71 %); **klibs.io MCP-сервер** + скилл `kmp-libraries-expert`;
   **Compose Hot Reload MCP-сервер** (CMP 1.12.0); **Kotlin LSP** под Apache-2.0 и
   расширение «Java & Kotlin by IntelliJ IDEA» для VS Code/Cursor; официальная
   рекомендация писать `AGENTS.md` с KMP-специфичными acceptance-критериями.
   У `developer.android.com`, `developer.apple.com`, `swift.org`, `skip.tools`
   **`llms.txt` нет** (HTTP 404, проверено 2026-09-17).
6. **Skip (Swift → SwiftUI + Compose) отпадает по жёсткому ограничению**: «Skip requires a
   macOS 15+ development machine» — прямая цитата из его же getting-started
   (https://skip.tools/docs/gettingstarted/, проверено 2026-09-17). Ни один шаг цикла
   не воспроизводится на Linux. Плюс Skip Fuse добавляет **≈60 МБ** к Android-бандлу
   (его же доки). Версия 1.9.9 (2026-09-16), MPL-2.0, 3.2k звёзд, вендор — микрокоманда.
7. **Swift Android SDK стал официальным в Swift 6.3 (2026-03-24)** и в Swift 6.4
   (2026-09-15) уже собран на LTS NDK 30 с поддержкой Android в Swift Build/SwiftPM.
   Это меняет картину на горизонте 2–3 лет, но **UI-фреймворка для Android у Apple нет**
   и не планируется — «Swift на Android» сегодня = общая логика, а не общий UI.
8. **Swift export (Kotlin → идиоматичный Swift без Objective-C) — Alpha**, проверено
   (https://kotlinlang.org/docs/native-swift-export.html, 2026-09-17). В роадмапе Kotlin
   (обновлён август 2026) отдельным пунктом стоит «Swift Export: from Alpha to Beta».
   До Beta граница Kotlin↔Swift в шаблоне проектируется **под Objective-C-экспорт + SKIE**
   (0.10.14, 2026-07-27) либо под «плоский» DTO-фасад, который переживёт переезд на Swift export.
9. **Google официально поддерживает KMP** и держит tier-1 (Android + iOS + JVM, полное
   CI-покрытие) для `annotation`, `collection`, `datastore`, `lifecycle`, `viewmodel`,
   `savedstate`, `sqlite`, `room`, `paging` — проверено (developer.android.com/kotlin/multiplatform,
   2026-09-17) и подтверждено наличием iOS-артефактов в Google Maven
   (`room-runtime-iosarm64` 2.8.5, `paging-common-iosarm64` 3.5.1, `annotation-iosarm64`
   1.11.0-rc01 и т. д.). Это снимает исторический риск «KMP — игрушка одного вендора»
   на уровне библиотек данных.
10. **JetBrains в мае 2026 сменил дефолтную структуру KMP-проекта ровно на ту, что нужна
   шаблону**: `shared/` (KMP-библиотека) + отдельные `androidApp/`, `iosApp/`, `desktopApp/`;
   при нативном UI — два общих модуля `sharedLogic/` и `sharedUI/`; при наличии сервера
   добавляется `core/` и `server/`. Причина — AGP 9.0 запрещает применять Android
   application-плагин в multiplatform-модуле. Проверено
   (https://blog.jetbrains.com/kotlin/2026/05/new-kmp-default-structure/).
11. **Compose Multiplatform на iOS — stable с 1.8.0 (май 2025), текущий stable 1.12.0
    (2026-08-25)**, но «нативным» он не становится: рендер — Skia/Metal на канве,
    а системный look iOS 26 (Liquid Glass) достижим **только через нативную SwiftUI-оболочку** —
    это официальная позиция самих JetBrains, у них есть отдельный туториал
    «Liquid Glass in a Compose Multiplatform app» с миграцией KotlinConf-app на
    `TabView` + `NavigationStack` вокруг Compose-контента. Для компаньон-приложения
    бизнес-заказчика это означает: CMP не освобождает от Swift, он лишь двигает границу.
12. **Rust-ядро (uniffi) — рабочая, но дорогая схема**: Element X (element-hq) держит один
    `matrix-rust-sdk` под SwiftUI и Jetpack Compose. Цена — третий язык в найме,
    FFI-граница, и всё равно два UI. Для CRM-подобных приложений выгода Rust
    (крипта, протокол, e2ee) отсутствует. **Go-ядро через `gomobile` — нет**:
    `golang.org/x/mobile` до сих пор без тега, самоописан как experimental, биндинги —
    Objective-C, не Swift (проверено pkg.go.dev, 2026-09-17).
13. **Найм в РФ (hh.ru, вся Россия, 2026-09-17)**: «Android разработчик» — 333 вакансии,
    «iOS разработчик» — 243, «Kotlin» — 323, «Swift» — 211, «Jetpack Compose» — 35,
    «SwiftUI» — 28, «Flutter» — 89, «React Native» — 46, **«Kotlin Multiplatform» — 9**,
    «Compose Multiplatform» — 5, «KMM» — 2. Вывод: **KMP-специалистов как отдельного
    рынка нет**, но KMP их и не требует — нужны обычный Kotlin-разработчик и обычный
    Swift-разработчик. Это аргумент **против** C (CMP) и **за** B.
14. **РФ-практика подтверждает именно схему B**: AvitoTech держит на KMP **ядро BDUI**,
    UI оставлен нативным «чтобы сохранить look&feel каждой платформы и её дизайн-систему»
    (прямая цитата Android-инженера Avito на Хабре); X5 Tech — KMP с соотношением команды
    1 iOS : 3 Android и оценкой «разработка ускорилась в два раза»; CleverPumpkin на
    Kassir.ru вынес в KMP сетевой слой (Ktor вместо Retrofit, Koin вместо Dagger 2) и
    отдаёт iOS XCFramework через SPM.
15. **Ответ на «80 % работы над iOS без Mac»**: да, объективно грамотный вариант есть, и
    это B. При схеме «KMP shared (домен + сеть + кеш + правила + тесты) на Linux +
    тонкий SwiftUI-слой» доля кода, которую агент на Linux пишет **и верифицирует**,
    реалистично **65–80 %** (общая логика 45–60 % + Android-слой 20–25 %), а слепым
    остаётся SwiftUI-слой на 20–30 % (расчёт — § 6.1). Mac нужен для линковки фреймворка,
    сборки/подписи, симулятора и отправки в App Store. Честная оговорка: верхняя граница
    достигается только если держать SwiftUI-слой тонким — это архитектурное обязательство,
    а не свойство технологии.
16. **Запасной вариант (если KMP-ветка не пойдёт) — A (два нативных шаблона)**, не C.
    Откат от B к A дешёвый (shared-модуль «растворяется» в две нативные реализации,
    UI не трогается), откат от C к чему угодно означает переписывание всего UI.

---

## 1. Срез версий и статусов на 2026-09-17

Всё в таблице проверено в день среза; способ проверки указан.

| Что | Версия / статус | Дата | Способ проверки |
|---|---|---|---|
| Kotlin | **2.4.20** (stable) | 2026-09-07 | GitHub Releases API `JetBrains/kotlin` |
| Kotlin Gradle plugin | 2.4.20 | 2026-09-07 | Maven Central `kotlin-gradle-plugin` metadata |
| Kotlin Multiplatform | **Stable** (с ноября 2023) | — | kotlinlang.org/docs/multiplatform.html, developer.android.com/kotlin/multiplatform |
| Compose Multiplatform | **1.12.0** (stable), 1.13.0-alpha01 | 2026-08-25 / 2026-09-10 | GitHub Releases API `JetBrains/compose-multiplatform` |
| CMP для iOS | **Stable** (с 1.8.0, май 2025) | — | blog.jetbrains.com (1.8.0), release notes 1.12.0 |
| **Swift export** (Kotlin→Swift) | **Alpha** | — | kotlinlang.org/docs/native-swift-export.html |
| Kotlin roadmap: Swift export | «from Alpha to Beta» — **в фокусе сейчас** | обновлён 08.2026 | kotlinlang.org/docs/roadmap.html |
| SKIE (Touchlab) | **0.10.14** | 2026-07-27 | GitHub Releases API `touchlab/SKIE` |
| Swift | **6.4** | 2026-09-15 | swift.org/blog |
| Swift SDK for Android | официальный с **6.3**; в 6.4 — LTS NDK 30, Swift Build в SwiftPM | 2026-03-24 / 2026-09-15 | swift.org/blog/swift-6.3-released, swift-6.4-released |
| swift-java | **0.6.0** | 2026-09-05 | GitHub Releases API `swiftlang/swift-java` |
| Skip | **1.9.9**, MPL-2.0, free/open-source | 2026-09-16 | github.com/skiptools/skip/releases, LICENSE.TXT |
| Skip требует | **macOS 15+**, Xcode 26, Android Studio | — | skip.tools/docs/gettingstarted |
| Amper → **Kotlin Toolchain** | **Alpha**, 0.12.0; репо `JetBrains/amper` архивирован 2026-05-29 | 09.2026 | blog.jetbrains.com/amper, blog.jetbrains.com/kotlin |
| AGP (Android Gradle Plugin) | **9.4.0** stable; 9.5.0-alpha05 | — | Google Maven `com.android.tools.build:gradle` metadata |
| Ktor client | **3.6.0** | — | Maven Central metadata |
| kotlinx-coroutines | 1.11.0 | — | Maven Central metadata |
| kotlinx-serialization-json | 1.12.0-RC (stable ветка ниже) | — | Maven Central metadata |
| kotlinx-io-core | 0.9.1 | — | Maven Central metadata |
| SQLDelight | 2.4.0-rc2 | — | Maven Central `app.cash.sqldelight:runtime` |
| Koin | 4.2.2 | — | Maven Central metadata |
| androidx.room (KMP) | **2.8.5**, есть `room-runtime-iosarm64` | — | Google Maven metadata |
| androidx.paging (KMP) | 3.5.1, есть `paging-common-iosarm64` | — | Google Maven metadata |
| androidx.lifecycle/viewmodel (KMP) | 2.11.0 stable / 2.12.0-alpha03 | 2026-09-09 | developer.android.com/kotlin/multiplatform + Google Maven |
| androidx.datastore (KMP) | 1.2.1 / 1.3.0-alpha11 | — | то же |
| androidx.sqlite (KMP) | 2.7.1 / 2.8.0-alpha01 | — | то же |
| Navigation 3 (JetBrains KMP-сборка) | `org.jetbrains.androidx.navigation3:navigation3-ui` 1.2.0-beta01 | 2026-09-09 | Maven Central metadata |
| `golang.org/x/mobile` | без тега, **experimental** | псевдоверсия 2026-09-08 | pkg.go.dev/golang.org/x/mobile |
| uniffi-rs | активен, полная поддержка Kotlin/Swift/Python | — | mozilla.github.io/uniffi-rs |

**Важный отрицательный факт**: `llms.txt` есть у `kotlinlang.org` (HTTP 200, 49 494 байта)
и у `jetbrains.com/help/kotlin-multiplatform-dev` (HTTP 200, 1 501 200 байт); **нет** у
`developer.android.com`, `developer.apple.com`, `www.swift.org`, `skip.tools`, `ktor.io`
(все HTTP 404). Проверено curl'ом 2026-09-17.

---

## 2. Кандидаты

### 2.A. Нативно ×2 — Kotlin/Compose + Swift/SwiftUI, две независимые базы

**Что общего**: только контракт. OpenAPI 3.1 отдаёт `go-backend-template` в рантайме
(`/openapi.json`) — обе стороны генерят/пишут клиент по нему. Плюс, возможно, общие
дизайн-токены (JSON → Kotlin-объект и Swift-структура) и общие доки.

**Что нативного**: всё. Модели, сеть, кеш, use-case'ы, валидация, аналитика, навигация,
UI — по два раза.

**Как выглядит репо**: два репо (`android-template` → `<org>/mobile-android`,
`ios-template` → `<org>/mobile-ios`) либо один монорепо с двумя корнями. Для экосистемы
stit (один шаблон = одна форк-цель) естественнее два репо.

**Инструменты**: Gradle 9.x + AGP 9.4 + Android SDK / Xcode 26 + SwiftPM.

**Что агент на Linux может собрать и протестировать**:

| Часть | На Linux | Комментарий |
|---|---|---|
| Android-приложение: сборка APK/AAB | **да** | Gradle headless, `sdkmanager` без GUI |
| Android: unit-тесты, Robolectric, lint, detekt/ktlint | **да** | полный гейт |
| Android: instrumented-тесты | условно | нужен эмулятор + KVM; на этой машине памяти мало |
| iOS: SwiftPM-пакет с доменной логикой (Foundation, swift-testing) | **да** | Swift 6.4 toolchain ставится на Debian; `swift build`/`swift test` работают |
| iOS: что угодно с `import UIKit` / `import SwiftUI` | **нет** | фреймворков на Linux нет |
| iOS: сборка `.app`, симулятор, архив, подпись | **нет** | нужен Xcode/macOS |

То есть **iOS-приложение целиком слепое**, но при дисциплине «доменный слой — отдельный
SwiftPM-пакет без UIKit» часть Swift-кода агент на Linux всё-таки компилирует и тестирует.
Это недооценённый нюанс, и он чуть поднимает A относительно наивной оценки.

**Скорость цикла**: Android — обычный Gradle-цикл (десятки секунд инкрементально).
iOS — цикл целиком на Mac (или на удалённом Mac), агент на Linux работает «вслепую»
+ пуш → удалённая сборка → лог обратно. Практически это минуты и высокая цена ошибки.

**Документация для агентов**: Android — `developer.android.com` без `llms.txt`, но
Kotlin-часть закрыта `kotlinlang.org/llms.txt`. iOS — `developer.apple.com` без `llms.txt`,
без MCP на Linux; Xcode 26.3 сам стал MCP-сервером (`xcrun mcpbridge`) — но это опять macOS.

**Зрелость/риски**: максимальные обязательства вендоров (Google + Apple), day-0 доступ к
API, никакого lock-in. Каденс ломающих изменений — годовой цикл ОС + AGP/Kotlin.

**Стоимость**: две кодовые базы, две обвязки `.claude/`, два гейта, два `docs/stack`,
два `/scaffold`, два моста в strategy, двойной ревью контракта. Расхождение логики между
платформами — системный, а не случайный риск (об этом прямо говорят все опрошенные
РФ-команды, см. § 3).

**Найм в РФ**: лучший — 333 + 243 вакансии (hh.ru, 2026-09-17), т. е. и кандидатов, и
рынка больше всего.

**Лицензии**: Apache-2.0 (Kotlin, AndroidX), Apache-2.0 (Swift), проприетарный Xcode.

---

### 2.B. KMP shared logic + native UI (рекомендуемый)

**Что общего**: `shared/` — Kotlin Multiplatform-библиотека: модели (kotlinx.serialization),
HTTP-клиент (Ktor 3.6.0), репозитории, use-case'ы, кеш (Room KMP 2.8.5 или SQLDelight 2.4),
маппинг ошибок, правила валидации, аналитические события, i18n-ключи, и — главное —
**тесты на всё это, которые бегут на JVM за секунды**.

**Что нативного**: весь UI и вся платформенная интеграция. Android — Jetpack Compose,
iOS — SwiftUI. Push, deep links, камера, карты, биометрия — через `expect/actual` либо
через интерфейс в shared + две реализации.

**Как выглядит репо**: **один**. И это ровно та структура, которую JetBrains с мая 2026
делает дефолтной (проверено, blog.jetbrains.com/kotlin/2026/05/new-kmp-default-structure/):

```
mobile-template/
├── shared/            # KMP-библиотека: commonMain + androidMain + iosMain
│   └── src/{commonMain,commonTest,androidMain,androidUnitTest,iosMain,iosTest}/kotlin
├── androidApp/        # Android-приложение (AGP application plugin) — Compose
├── iosApp/            # Xcode-проект — SwiftUI
├── gradle/libs.versions.toml
└── settings.gradle.kts
```

При желании разделить «общая логика» и «общий UI» JetBrains предлагает `sharedLogic/`
+ `sharedUI/` — это готовая точка расширения для опционального CMP-модуля (см. § 6).

Отдельно важно: **AGP 9.0+ запрещает применять Android application-плагин в
multiplatform-модуле**, поэтому `androidApp` как отдельный модуль — не стилистика, а
требование тулчейна (проверено, тот же пост).

**Инструменты**: Gradle 9.x + Kotlin 2.4.20 + AGP 9.4.0 на обеих сторонах; Xcode 26 +
SwiftPM/direct integration для `iosApp`.

**Интеграция shared → iOS** (проверено, kotlinlang.org/docs/multiplatform/multiplatform-ios-integration-overview.html,
обновлено 2026-07-21) — 4 варианта:

| Вариант | Когда | Цена |
|---|---|---|
| **Direct integration** (скрипт `embedAndSignAppleFrameworkForXcode` в Build Phase) | монорепо, нет CocoaPods-зависимостей; **по умолчанию** в KMP IDE-плагине | Xcode-сборка тянет Gradle |
| **SwiftPM с локальным пакетом** | монорепо + iOS-проект на Swift packages | чуть больше конфигурации |
| CocoaPods с локальным podspec | есть Pod-зависимости в KMP | тянет CocoaPods |
| **SwiftPM + XCFramework (remote)** | хотим версионировать shared отдельно | нужен процесс публикации XCFramework |

Для шаблона логичен **direct integration** (дефолт, наименьшая конфигурация); вариант
«SwiftPM + XCFramework» держим в доке как путь для команд, где iOS живёт отдельно —
именно так делает CleverPumpkin на Kassir.ru (CI собирает XCFramework и публикует в
отдельный репо, iOS подключает как SPM-зависимость).

**Граница Kotlin ↔ Swift.** Сегодня это Objective-C-экспорт (со всеми его мозолями:
`KotlinInt`, префиксы, потеря дженериков, sealed → не exhaustive switch). Смягчается
двумя способами:

- **SKIE 0.10.14** (Touchlab, 2026-07-27) — Gradle-плагин, который поверх Obj-C-фреймворка
  генерит Swift-дружественный слой: sealed → enum с exhaustive switch, `suspend` → `async`,
  `Flow` → `AsyncSequence`, дефолтные аргументы. Зрелый, но вендор — небольшая компания.
- **Swift export — Alpha** (kotlinlang.org/docs/native-swift-export.html, проверено
  2026-09-17). Уже умеет: multi-module, packages, typealias, честный `Int?` без боксинга,
  overloads, `flattenPackage`, `suspend → async`, `Flow → AsyncSequence`, cross-language
  inheritance (Swift-класс наследует Kotlin-интерфейс и передаётся обратно в Kotlin —
  «reverse import», ровно то, что нужно, чтобы отдать в shared платформенную реализацию).
  Не умеет: дженерики (стираются до upper bound), не-final классы, наследники
  `List`/`Set`/`Map` (KT-80416, KT-80417), работает только при direct integration.
  В роадмапе Kotlin отдельной строкой «Swift Export: from Alpha to Beta».

**Практический вывод для шаблона**: проектировать публичный API `shared` как **плоский
фасад из финальных классов, data-классов, sealed-иерархий и `suspend`-функций, без
дженериков в сигнатурах**. Такой фасад одинаково хорошо ложится и на Obj-C+SKIE сегодня,
и на Swift export завтра — миграция не потребует переписывания iOS-слоя. Это должно стать
правилом `.claude/rules/kmp-public-api.md`.

**Что агент на Linux может собрать и протестировать**:

| Задача | На Linux | Источник |
|---|---|---|
| `:shared:jvmTest` / `:shared:testDebugUnitTest` — вся общая логика | **да** | обычный Gradle/JVM |
| `:shared:compileKotlinIosArm64` → `.klib` (если нет cinterop) | **да, по докам** | kotlinlang.org/docs/native-target-support.html § Hosts |
| `:shared:linkDebugFrameworkIosArm64` / XCFramework | **нет** | там же: «Building final binaries for Apple targets on Linux and Windows is also not possible» |
| `:shared:iosSimulatorArm64Test` (запуск iOS-тестов) | **нет** | там же: запуск тестов Apple-таргетов — только на macOS-хосте |
| `:androidApp:assembleDebug`, `lint`, `detekt`, Compose-тесты на JVM | **да** | — |
| `iosApp` (SwiftUI) — сборка/тесты | **нет** | Xcode |

Это и есть ключевое отличие B от A: **вся общая логика, включая её iOS-компиляцию на
уровне klib, проверяется агентом на Linux**, а «слепой» остаётся только SwiftUI-оболочка.
Подтверждение того, что klib для Apple собирается на Linux, — из докдиаграммы «Hosts»;
эмпирически это должен проверить поток 06 (см. § 7).

**Скорость цикла**: `:shared:jvmTest` — секунды (обычный JVM). `compileKotlinIosArm64` —
десятки секунд до минут (Kotlin/Native медленнее; в роадмапе Kotlin отдельные пункты
«Native compiler caches in release mode» и «Enable Native tasks parallelization without
configuration cache»). Android-цикл — обычный.

**Документация для агентов** — сильнейшая сторона:
- `kotlinlang.org/llms.txt` + `jetbrains.com/help/kotlin-multiplatform-dev/llms.txt`
  (проверено, HTTP 200);
- официальная страница `kotlinlang.org/docs/ai-for-development.html` с прямыми
  рекомендациями по `AGENTS.md` для KMP: «Run target-specific tests after introducing
  changes», «Verify that all configured KMP targets successfully build before considering
  a task complete», «Review the implementation for platform-specific APIs leaking into
  common code» — это буквально готовые acceptance-критерии для нашего гейта;
- **klibs.io MCP-сервер** + агентный скилл `kmp-libraries-expert` (4 200+ KMP-проектов
  в каталоге);
- **JetBrains IDE MCP**, **Compose Hot Reload MCP** (CMP 1.12.0, «trigger reloads, take
  screenshots, read semantic trees»);
- **Kotlin LSP** (Apache-2.0, бесплатный) и «Java & Kotlin by IntelliJ IDEA» для
  VS Code/Cursor (preview бесплатный, дальше — IDEA Ultimate);
- **Kotlin Benchmark** (kotlinlang.org/benchmark) — 105 задач из реальных Kotlin-репо,
  методология SWE-bench; лидер Claude Code с Opus 4.7 xhigh — 90/105 = 85.71 %.

**Зрелость/риски**:
- KMP stable ~2.8 года; **Google официально поддерживает** и держит tier-1 Jetpack-библиотеки —
  это снимает моно-вендорность на уровне данных;
- iOS-сторона в схеме B — **чистый Apple-стек**, то есть нулевой риск на самой рискованной
  платформе;
- lock-in низкий: `shared` — обычная библиотека; «выход» = реализовать её API дважды
  нативно, UI не трогается;
- остаточные риски: JetBrains как единственный владелец компилятора Kotlin/Native;
  Swift export ещё Alpha; SKIE от небольшого вендора; каденс Kotlin — минорные релизы
  раз в ~2–3 месяца (2.4.0 → 2.4.10 → 2.4.20 за июнь–сентябрь 2026).

**Найм в РФ**: нужны **обычные** Kotlin- и Swift-разработчики (323 + 211 вакансий как
индикатор рынка). Отдельный «KMP-разработчик» не нужен — и хорошо, потому что такого
рынка нет (9 вакансий).

**Лицензии**: Apache-2.0 (Kotlin, Ktor, AndroidX, coroutines/serialization),
Apache-2.0 (SQLDelight, Koin), Apache-2.0 (SKIE).

---

### 2.C. Compose Multiplatform — общий UI на Kotlin для обеих платформ

**Что общего**: почти всё — логика + UI (`@Composable`-экраны, навигация, ресурсы, темы).
Реальные кейсы дают 90–100 % общего кода (Markaz: «100+ экранов, полностью на Compose
Multiplatform»; Bitkey by Block — 95 % мобильного кода).

**Что нативного**: тонкая `ComposeUIViewController`-оболочка на iOS + платформенные
интеграции (камера, пуши, биометрия) через `expect/actual` и `UIKitView`/`UIKitViewController`.

**Как выглядит репо**: один, структура как у B, но `iosApp` вырождается до
«хост-контроллер + Info.plist + entitlements», а весь UI живёт в `sharedUI/`.

**Статус (проверено 2026-09-17)**: stable для iOS с 1.8.0 (2025-05-06), текущий stable —
**1.12.0** (2026-08-25), в разработке 1.13.0-alpha01. Релиз-ноты 1.12.0 показывают, что
iOS-часть всё ещё активно «допиливается» по мелочам платформенного поведения: селекшн-хэндлы
в стиле iOS, пункт «Select» в контекстном меню текстового поля, `LiveRegion` в accessibility,
`VerbatimTtsAnnotation`/`LocaleList` для VoiceOver, фикс прыжка контента в начале
swipe-back, конфликт swipe-back с `HorizontalPager`, Scribble (Apple Pencil) в TextField,
краш на iOS 14 из-за `CAFrameRateRangeDefault`. Это здоровый темп зрелого продукта, но и
честный индикатор: «нативность» достигается ручным допиливанием, а не наследуется.

**Ключевое ограничение — системный look**. Официальный туториал JetBrains
«Liquid Glass in a Compose Multiplatform app» (kotlinlang.org/docs/multiplatform/ios-liquid-glass.html)
начинается словами: чтобы получить Liquid Glass, **нужна нативная SwiftUI-оболочка**,
потому что эффект рендерит система через нативные `TabView`, `NavigationStack` и toolbar API.
Рекомендуемая архитектура после миграции:

```
ContentView
└── TabView (Liquid Glass, iOS 26)
    ├── Tab: Schedule → NavigationStack → Compose-экраны
    └── Tab: Info     → NavigationStack → Compose-экраны
```

При этом из Kotlin приходится: вынести `title` в объекты роутов, добавить `onNavigate`/
`onActivate`-колбэки в `App()`, перехватывать навигацию на уровне Compose и отдавать её
Swift, построить «standalone screen renderer» для iOS, прятать собственный Compose-навбар.
То есть **CMP не убирает Swift из проекта — он делает Swift-слой «управляющим», а
Kotlin-навигацию — подчинённой**. Для бизнес-приложения-компаньона, где заказчик ждёт
«как родное», это существенная поправка к обещанию «один UI на всё».

**Размер приложения**: кейс Markaz (kotlinlang.org/lp/multiplatform/case-studies/) —
«under 10MB on Android and **137MB on iOS**». Цифра по iOS, вероятно, про install size,
но порядок показателен: Skia-рантайм + ресурсы на iOS стоят дорого. Для приложения,
которое раздаётся сотрудникам заказчика, это терпимо; для массового B2C — аргумент против.

**Что агент на Linux может**: **больше всего из всех кандидатов.**
- весь UI собирается и запускается как Android-таргет и как desktop-таргет → агент
  **видит** результат (скриншот, `ComposeUiTest`), не имея Mac;
- **Compose Hot Reload MCP-сервер** (1.12.0) даёт агенту «trigger reloads, take screenshots,
  read semantic trees» на живом приложении; Kotlin Toolchain 0.12 добавил Compose Hot
  Reload из командной строки;
- `ComposeUiTest` в 1.12.0 выровнен по idle-семантике с Android — значит UI-тесты
  детерминированнее;
- слепой остаётся только хост-контроллер iOS (десятки строк).

**Тестируемость**: общая логика + общий UI тестируются один раз. Минус — **XCUITest поверх
Compose-канвы работает плохо** (Compose-иерархия не является нативной view-иерархией);
это регулярно называют среди рисков CMP. Для e2e на iOS придётся либо принять
Compose-собственные тесты, либо строить отдельный accessibility-мост.

**Зрелость/риски**: один вендор (JetBrains) на весь UI; lock-in максимальный — «выход» из
CMP = переписать UI; каденс — минор раз в ~3 месяца с migration notes (в 1.12.0 есть
`Deprecation level ... changed to ERROR`); найм в РФ — 5 вакансий на «Compose Multiplatform»;
день-0 доступ к новым iOS-API отсутствует (ждём JetBrains).

**Кто реально в проде**: Markaz (Пакистан, 5 млн+ загрузок, 100+ экранов), Bitkey by Block
(95 % кода), Instabee, Music Work (100 % UI), Fast&Fit (90 %+ вместе с UI), Sony
(companion-app для флагманских наушников — «unified KMP and Compose Multiplatform workflow»).
Проверено — kotlinlang.org/lp/multiplatform/case-studies/.

---

### 2.D-1. Skip (skip.tools) — Swift/SwiftUI → нативный SwiftUI + Jetpack Compose

**Идея**: пишешь всё на Swift/SwiftUI; на iOS оно работает как есть, на Android Skip либо
транспилирует Swift → Kotlin (**Skip Lite**), либо компилирует Swift нативно через
официальный Swift SDK for Android (**Skip Fuse**), а SwiftUI отображает в настоящий
Jetpack Compose. Режим выбирается **на уровне модуля**, режимы смешиваются.

**Статус (проверено 2026-09-17)**: версия **1.9.9** (2026-09-16), лицензия **MPL-2.0**,
3.2k звёзд на GitHub, позиционирование — «free, open source, and community-funded».
Skip Fuse — рекомендованный режим.

**Дисквалифицирующий факт для нас**: «**Skip requires a macOS 15+ development machine**»
(skip.tools/docs/gettingstarted). Ставится через `brew install skip`, `skip checkup`
проверяет наличие Xcode 26.2, Swift 6.2.3, Gradle, JDK, adb. Xcode — основная IDE
(«Stay in Xcode for day-to-day development»). На Linux не работает **ничего** из цикла:
ни `skip create`, ни сборка Android-стороны. Agent-friendliness при нашей постановке — 1.

**Прочие ограничения (из их же доков)**:
- Skip Fuse добавляет **≈60 МБ** к Android-бандлу (Swift stdlib + Foundation + ICU);
  в примере `skip checkup` debug-APK = 80 МБ против 175 КБ у IPA;
- Skip Fuse **не «ejectable» на Android**: бросив Skip, остаёшься с рабочим iOS-приложением
  и **без** самостоятельной Kotlin-кодовой базы (в Lite-режиме — остаёшься, но Lite
  беднее по языку и stdlib);
- отладка native-Swift на Android сложнее, сборка медленнее;
- Lite-режим: «It isn't real Swift» — часть языковых конструкций не мапится в Kotlin,
  детерминированное освобождение объектов на JVM не воспроизводится.

**Зрелость/риск**: продукт молодой, вендор — микрокоманда (релизы подписывает один
мейнтейнер), рынка найма нет вообще, экосистема — «thousands of Swift packages» (это
правда про Swift-пакеты как таковые, а не про Skip-специфичные). Скорость релизов высокая
(1.9.0 → 1.9.9 за короткий период), что для шаблона означает нестабильную поверхность.

**Вывод**: интересная технология и отличный индикатор тренда «Swift выходит за пределы
Apple», но для stit на 2026-09-17 — нет: жёсткое требование macOS убивает весь агентный
процесс, а вендор-риск несопоставим с KMP/Google.

---

### 2.D-2. Swift Android SDK + swift-java «голыми руками» (без Skip)

**Идея**: общая логика пишется на Swift как SwiftPM-пакет, кросс-компилируется под Android
официальным Swift SDK, подключается в Android-приложение через JNI/`swift-java`; UI —
SwiftUI на iOS и Compose на Android. Зеркало кандидата B, только общий язык — Swift.

**Статус (проверено 2026-09-17)**:
- Swift Android Workgroup создан в 2025; nightly SDK появился в марте 2026; **официальный
  Swift SDK for Android — часть Swift 6.3 (2026-03-24)**; в **Swift 6.4 (2026-09-15)** SDK
  собран на **LTS NDK 30**, Android-таргет поддержан в Swift Build внутри SwiftPM
  (пропал пост-инсталл-скрипт) — swift.org/blog;
- поддержаны aarch64 / x86_64 / armv7 / i686, минимальный Android API 28; в наборе —
  Foundation, Dispatch, XCTest, swift-log;
- `swift-java` **0.6.0** (2026-09-05): в 6.4 добавлены async/throwing в protocol- и
  callback-обёртках, автоматический маппинг замыканий в `Runnable`, variadic-параметры,
  Java record types.

**Чего нет и не будет**: UI-фреймворка Apple для Android. Поэтому вариант — только
«общая логика», и он конкурирует не с C, а с B.

**Что агент на Linux может**: Swift toolchain ставится на Debian, `swift build`/`swift test`
для общего пакета работают на Linux, Android SDK ставится через `swift sdk install` и
кросс-компилирует на Linux. Android-приложение (Kotlin) собирается. Слепым остаётся
`iosApp`. По agent-friendliness это **сопоставимо с B**, что неожиданно.

**Почему всё же не выбираем**:
1. **Экосистема логики на Swift для мобильного бизнес-приложения бедна относительно
   Kotlin**: у KMP есть Ktor, kotlinx.serialization, Room/SQLDelight, DataStore,
   ViewModel/Lifecycle, Paging — всё с iOS-таргетами и tier-1 CI от Google. На стороне
   Swift-на-Android эквивалент придётся собирать вручную.
2. **Цена в бандле**: Swift-рантайм на Android — тот же порядок ≈60 МБ, что у Skip Fuse
   (та же природа: stdlib + Foundation + ICU). Для РФ-дистрибуции (RuStore, слабые сети)
   это заметно.
3. **Доки для агентов**: у `swift.org` нет `llms.txt`; MCP-инструменты Swift завязаны на
   Xcode (`xcrun mcpbridge`, Xcode 26.3 как MCP-сервер) — то есть на macOS.
4. **Возраст**: официальному SDK ~6 месяцев. Для шаблона, который отдают заказчику как
   фундамент, это преждевременно.

**Держим в поле зрения как триггер пересмотра R-3** (см. § 7).

---

### 2.E-1. Rust-ядро + native UI (uniffi-rs)

**Кто так делает**: Element X — `matrix-rust-sdk` как единое ядро, `element-x-ios` на
SwiftUI, `element-x-android` на Jetpack Compose, биндинги — uniffi (Kotlin/Swift);
`element-hq/matrix-rust-components-swift` — Swift-пакет с компонентами SDK. Похожая схема
исторически у Mozilla (application-services), 1Password, Bitwarden.

**Что даёт**: одно эталонное ядро под все клиенты (включая web через wasm-bindgen),
максимальная производительность и контроль памяти, отличная тестируемость ядра
(`cargo test` на Linux — идеальный агентный цикл).

**Цена**:
- **третий язык** в проекте и в найме; для заказчика, который получает исходники, это
  прямое удорожание поддержки;
- FFI-граница: uniffi генерит биндинги, но асинхронность, ошибки и коллбэки через FFI —
  отдельный класс багов;
- **UI всё равно пишется дважды**, и, в отличие от B, дважды же пишется весь слой
  «адаптация ядра к платформе» (ViewModel/Observable, навигация, кеш представлений);
- линковка Rust под iOS всё равно требует macOS для финальной сборки приложения.

**Когда оправдано**: когда в ядре лежит нечто, что действительно нельзя написать дважды —
криптография, e2ee-протокол, парсер, синхронизация CRDT. У наших приложений
(CRM/задачи/сделки/календарь) такого ядра нет: там HTTP + модели + правила, то есть ровно
то, что KMP закрывает дешевле.

---

### 2.E-2. Go-ядро через `gomobile`

**Соблазн** понятен: бэкенд экосистемы на Go, значит модели и правила можно было бы
переиспользовать буквально.

**Реальность (проверено 2026-09-17, pkg.go.dev/golang.org/x/mobile)**: у модуля **нет ни
одного тега**, псевдоверсия `v0.0.0-20260908204917-…`; официальная формулировка —
«The Go Mobile project is experimental. Use this at your own risk. While we are working
hard to improve it, neither Google nor the Go team can provide end-user support».
`gobind` генерит биндинги для **Java и Objective-C** (не Swift), с сильно урезанным
подмножеством типов (без дженериков, без произвольных структур в сигнатурах), тянет
Go-рантайм и GC в приложение, `gomobile bind` для iOS требует Xcode/macOS.

**Вывод**: не кандидат. Единственная разумная форма «переиспользования Go» в нашей
экосистеме — **контракт**, а не код: OpenAPI 3.1 из huma + (опционально) генерация
Kotlin-клиента по нему.

---

### 2.F. Flutter / React Native — почему нет (по одной строке)

- **Flutter** — прямой запрет владельца («Flutter точно не хочу»); плюс свой рантайм и
  свой рендер (не системные компоненты), Dart как третий язык, 89 вакансий на hh.ru против
  333+243 нативных.
- **React Native** — JS-рантайм и мост/JSI, чужая для нас культура сборки; исторические
  уроки: **Airbnb свернула RN в 2018** («Sunsetting React Native»), **Dropbox в 2019
  отказался от общего C++-ядра** между iOS и Android («The (not so) hidden cost of sharing
  code between iOS and Android»), **Shopify ушёл all-in в RN в 2020, подтвердил ставку в
  январе 2025 — и развернулся обратно в native 2026-09-10** (§ 3.4). То есть за шесть лет
  флагманский RN-кейс индустрии успел стать и главным аргументом «за», и главным
  аргументом «против».

Общая мораль для нас: **платить надо за то, что реально дублируется (логика), а не за то,
что и должно различаться (UI)** — это ровно тезис кандидата B; и ни один кросс-платформенный
слой с собственным рантаймом этой границы не соблюдает.

---

## 3. Кто как делает (первоисточники)

### 3.1. Мир — что именно шарят

Все строки ниже — из официального каталога кейсов JetBrains
(https://kotlinlang.org/lp/multiplatform/case-studies/, проверено 2026-09-17).

| Компания | Что шарят | Комментарий |
|---|---|---|
| **Netflix** | логику для mobile studio apps | только логика |
| **Bitkey by Block** (Cash App/Block) | **95 % мобильного кода**, мигрировали на CMP | логика + UI |
| **Autodesk** | offline-синхронизацию и модели данных (iOS/Android/Windows) | только логика |
| **Baidu** (Wonder App) | data-слой и бизнес-логику | только логика |
| **Quizlet** | shared-код переведён с JavaScript на Kotlin; 100 млн установок | логика |
| **Philips**, **VMware** (Workspace ONE) | модули логики | логика |
| **Duolingo** | «чем больше используем KMP, тем быстрее шипим» | логика |
| **Sony** | companion-app для флагманских наушников на KMP + CMP | логика + UI |
| **H&M** | общая библиотека feature-toggle | логика |
| **Instabee** | CMP: «уровень общего кода без роста сложности — ошеломляющий» | логика + UI |
| **Markaz** (PK, 5 млн+ загрузок) | 100+ экранов целиком на CMP; **<10 МБ Android / 137 МБ iOS** | логика + UI |
| **OpenSooq** | 91–98 % общего кода в ключевых модулях, >99.7 % crash-free | логика |
| **Worldline / Eroski** | миграция с Java + Objective-C, >99 % crash-free, 800 тыс. MAU | логика |
| **Posten Bring** | единый слой бизнес-логики для десятков фронтов и бэкендов | логика |
| **Mirego**, **Memrise**, **Music Work**, **Fast&Fit** | от «только логика» до «100 % UI» | разное |
| **Philo** | 3 года KMP в проде на mobile/TV/web | см. цитату ниже |

Отдельно — **самая релевантная нам цитата** (Philo Engineering, тот же каталог):

> «…More recently, we've adopted agentic AI workflows across several processes. We still
> see substantial value in a shared business-logic project in an agentic development world.
> The calculus hasn't changed… Kotlin is approachable for both humans and robots, and a
> smaller code surface area helps agents keep context sizes reasonable and token burn
> efficient.»

Это прямое эмпирическое свидетельство в пользу B **именно в AI-driven процессе**:
меньшая поверхность кода → меньше контекста и токенов у агента.

### 3.2. Российские компании

Источник: статья CleverPumpkin + AvitoTech + X5 Tech на Хабре,
https://habr.com/ru/companies/cleverpumpkin/articles/1012648/ (2026-03-23, проверено
2026-09-17). Все цитаты — оттуда.

| Компания | Что делают | Прямое свидетельство |
|---|---|---|
| **AvitoTech** | KMP — **ядро BDUI-движка**; UI остаётся нативным на Android, iOS и Web (MAV/Desktop) | «На Kotlin Multiplatform мы решили делать именно ядро BDUI, а UI оставили нативным. Во-первых, хотелось сохранить look&feel каждой платформы и использовать её дизайн-систему. Во-вторых, опыт других фреймворков показывает, что поддержка единого интерфейса для разных платформ требует значительных трудозатрат. Для нас эти риски были неприемлемы» — Андрей Колпаков, Android Engineer |
| **X5 Tech** | KMP как основная модель для новых фич; соотношение команды **1 iOS : 3 Android**; сначала Android, потом переиспользование в iOS | «Смело могу сказать, что разработка ускорилась в два раза. Единая кодовая база для двух платформ — это не шутки» — Дмитрий Алексеенков, лид мобильной разработки |
| **MAGNIT OMNI** | ранняя стадия KMP; честно про боль | «Были проблемы, что перестройка под KMP ломала CI, были даже серьёзные креши на проде из-за того, что никто не привык работать с конкретной KMP-библиотекой» — Алексей Гребенец |
| **CleverPumpkin / Kassir.ru** | KMP для сетевого слоя; **Retrofit → Ktor**, **Dagger 2 → Koin**; CI собирает XCFramework и публикует в отдельный репо, iOS подключает через **SPM** | «логика запросов стала единой, багфиксы и изменения в API больше не требовали двойной работы» |

Оценка эффекта из той же статьи: «финансовый эффект KMP особенно заметен после переноса
**20–30 % бизнес-логики** в общий слой и увеличивается в долгосрочной перспективе».

**Чего в РФ-материалах не видно**: ни одного публичного кейса крупной РФ-компании,
которая переписала бы **весь UI** на Compose Multiplatform. Публичная позиция Avito прямо
противоположна. Это сильный локальный аргумент за B, а не за C. (Помечаю как «наблюдение
по доступным источникам», не как доказательство отсутствия.)

### 3.3. Движение в обе стороны

- **Airbnb → из RN в native (2018)**, **Dropbox → из общего C++-ядра в native (2019)** —
  классические «уходы»; в обоих случаях причина одна: цена общего слоя превысила
  экономию, потому что общим сделали слишком много (UI и платформенные интеграции).
  Формулировка Dropbox: писать в нестандартной манере — это накладные расходы, которых
  не было бы при платформенных умолчаниях, и эти накладные расходы оказались дороже,
  чем написать код дважды.
- **Shopify → all-in на RN (2020), подтверждение (01.2025), разворот в native (09.2026)** —
  разобран отдельно в § 3.4, потому что причина разворота — **кодовые агенты**, то есть
  ровно наш процесс.
- **Block/Bitkey, Sony, Markaz, Instabee → из native в CMP** — движение к общему UI есть,
  но в сегментах, где системный look не является продуктовым требованием.

Мораль, которую стоит зафиксировать в `docs/` шаблона: **граница «общее / нативное»
проходит там, где заканчивается бизнес-логика и начинается платформенный опыт**. Все
успешные долгие кейсы держат эту границу; все откаты — следствие её сдвига вверх.

### 3.4. Разворот Shopify (2026-09-10): «агенты удешевили писать дважды»

Это самый свежий и самый релевантный нашему вопросу первоисточник: два поста в
инженерном блоге Shopify от 2026-09-10 —
**«Native is now the future of mobile at Shopify»** (Mustafa Ali, Head of Mobile,
https://shopify.engineering/back-to-native) и
**«Migrating Shop app from React Native to native»**
(https://shopify.engineering/shop-app-migration). Оба проверены 2026-09-17.

**Что утверждается** (прямые цитаты из первого поста):

> «…since then, coding models have gotten dramatically better, and for our apps and our
> team, building the same feature in Swift and Kotlin no longer carries the cost it used to.»

> «Native still means building and maintaining software on two platforms, that cost has not
> disappeared. What changed is that agents can now do enough of the implementation,
> translation, testing, and review work that it's no longer the deciding factor it was in 2020.»

> «We are making this change because agents have reduced the advantages of sharing
> implementation, while the advantages of building for each platform remain.»

Что агенты у них умеют:
- реализовать фичу на Android, используя iOS-версию как референс, и наоборот;
- помогать разработчику работать вне его основного стека;
- «резко снизить стоимость поддержания паритета между платформами через **общие
  спецификации, тесты и review-чекпойнты**».

**Измеренные результаты** (второй пост, Shop app; сравнение native против React Native):

| Метрика | Native | React Native | Δ |
|---|---|---|---|
| Холодный старт iOS | 2466 мс | 3200 мс | −23 % |
| Холодный старт Android | 2233 мс | 4433 мс | −50 % |
| Session stability | 99.95 %+ | 99.5 %+ | 10× меньше падающих сессий |
| Release-сборка Android | 184 МБ | 293 МБ | −109 МБ (−37 %) |
| Release-сборка iOS | 68 МБ | 67 МБ | +1 МБ |
| Время release-сборки Android | — | — | −75 % |
| Скролл ленты Android | 120 FPS | — | «с минимальными оптимизациями» |

**Процесс** (это самое ценное для нашего шаблона):
- PoC — **один инженер за одну неделю**; полная миграция Shop app — **шесть инженеров за
  12 недель** до релиза в сторах; greenfield, а не brownfield;
- стандартизация на **SwiftUI + Jetpack Compose**, чтобы использовать «structural and
  mental model symmetries between platforms to guide autonomous agents»;
- **Helix** — система пошаговой миграции: агент читает RN-код экрана и предлагает
  последовательность **чекпойнтов** (маленьких упорядоченных срезов работы, каждый
  ревьюится за минуты); каждый чекпойнт обязан доказать поведение тестами, совпасть с
  работающим приложением в визуальном ревью, **пережить двух adversarial-ревьюеров** и
  получить «кивок» человека — только после этого коммит и следующий чекпойнт; фидбэк из
  ревью запоминается, и цикл со временем становится автономнее;
- **приёмка плана привязана к хешу его содержимого**: изменение плана аннулирует
  предыдущее одобрение — прямая идея для нашего `/plan`;
- **Tardis** — отладочный инструмент, дающий агентам структурный доступ к живым событиям,
  логам и состоянию нативного приложения плюс возможность слать ему команды; отдельная
  фича Tardis снимает скриншоты и окна событий у RN- и native-версий в именованных
  чекпойнтах для сравнения паритета аналитики;
- «Native expertise remained essential»: сгенерированный код мог удовлетворять требованиям
  и при этом вносить дублирование, архитектурный дрейф и проблемы производительности —
  спасали repository guidance, линтеры, тесты, статанализ, перф-проверки и ревью;
- паритет iOS/Android теперь держится **процессом**, а не общей кодовой базой:
  «Android and iOS must be at feature parity at all times».

**Центральная архитектурная цитата** — и вот здесь Shopify фактически описывает KMP:

> «Agentic control of simulators has been a bottleneck… Agents can make code changes in
> seconds, but it takes them several minutes to test the output… The core principle here is
> that **business logic should be completely decoupled from the UI and be able to run
> headlessly on desktop**. We then make it available to agents via a CLI that allows them to
> iterate on it **in milliseconds instead of minutes** without involving simulators.»

**Как это читать применительно к нашей развилке.** Разбор по пунктам:

1. **Тезис Shopify — про стоимость дублирования, а не про его объём.** В нашей матрице
   это влияет не на оценку кандидата A (у него по-прежнему 1 балл за «доля общего кода» —
   общего нет), а на **вес** этого критерия. Соответствующий сценарий чувствительности
   посчитан ниже (§ 5.2, сценарий 4).
2. **Их «до» — не KMP, а React Native.** Все выигрыши в таблице (старт, размер, стабильность,
   время сборки) — это выигрыши над JS-рантаймом и мостом. У KMP ничего из этого нет:
   `shared` — обычная нативная библиотека, UI и так нативный, рантайма нет. Сравнения
   «native против KMP» Shopify не делал, и переносить их цифры на нашу развилку нельзя.
3. **Принцип, к которому они пришли, — это и есть `shared`.** «Бизнес-логика полностью
   отделена от UI и гоняется headless на десктопе, агент дергает её через CLI за
   миллисекунды» — дословное описание KMP-модуля с `:shared:jvmTest`. Shopify построил это
   руками (CLI + Tardis + Helix, и это работа отдельной команды); KMP отдаёт тот же
   feedback-loop как свойство сборки. **Для соло-разработчика с агентом это решающая
   разница**: у нас нет ресурса построить свой Tardis.
4. **Их ограничение — не наше.** У Shopify парк Mac'ов, симуляторы, нативная экспертиза
   на обеих платформах и десятки инженеров. Наше связывающее ограничение — **отсутствие
   Mac**: «агент реализует Android по iOS-референсу и наоборот» предполагает, что обе
   стороны можно собрать и проверить. У нас iOS-сторону проверить нечем, и чем больше
   кода на ней написано «вслепую», тем выше риск. A (два нативных) удваивает объём слепого
   iOS-кода относительно B.
5. **Обратная цитата существует и тоже свежая.** Philo (каталог кейсов KMP, § 3.1) прямо
   об агентном мире: «We still see substantial value in a shared business-logic project in
   an agentic development world. The calculus hasn't changed… Kotlin is approachable for
   both humans and robots, and a smaller code surface area helps agents keep context sizes
   reasonable and token burn efficient.» То есть в 2026 году индустрия по этому вопросу
   **разделена**, и обе позиции подкреплены практикой.
6. **Что берём безусловно** (вне зависимости от выбора A/B/C) — это процессные находки
   Shopify, они технологически нейтральны и прямо ложатся на нашу обвязку:
   чекпойнт-цикл с обязательным «докажи тестом + визуальное ревью + adversarial-ревью +
   человек»; хеш плана как условие действительности приёмки; агенто-адресуемое состояние
   приложения; правило «паритет платформ держится процессом и спецификацией»;
   «native expertise remained essential» — то есть гейт из линтеров/статанализа/перф-проверок
   обязателен, агент его не заменяет. Это вход для потока 08.

**Вывод по § 3.4**: разворот Shopify **сокращает разрыв A ↔ B и уничтожает часть
аргументации за C** (общий UI — это и есть «abstraction layer», от которого они ушли),
но при нашем ограничении «Linux без Mac» он не переворачивает решение: B даёт тот самый
headless-цикл, ради которого Shopify построил отдельную инфраструктуру.

---

## 4. Найм в РФ (hh.ru, 2026-09-17)

Методика: страница `https://hh.ru/search/vacancy?text=<запрос>&area=113` (area 113 =
Россия), из встроенного JSON берётся поле `totalResults`. Дата снятия — **2026-09-17**.
(API `api.hh.ru` без авторизации отдаёт 403, поэтому считано со страницы поиска.)

| Запрос | Вакансий |
|---|---|
| `Android` (всюду) | 16 732 |
| `iOS` (всюду) | 8 629 |
| `мобильный разработчик` | 2 119 |
| **`Android разработчик`** | **333** |
| **`Kotlin`** | **323** |
| **`iOS разработчик`** | **243** |
| **`Swift`** | **211** |
| `Flutter` | 89 |
| `React Native` | 46 |
| `Jetpack Compose` | 35 |
| `Dart` | 35 |
| `SwiftUI` | 28 |
| `Objective-C` | 18 |
| **`Kotlin Multiplatform`** | **9** |
| `Compose Multiplatform` | 5 |
| `KMM` | 2 |

Резюме (соискателей) с этого же ресурса без работодательского аккаунта не считываются —
**не проверено**, в отчёте не используется.

**Чтение цифр для нашей развилки**:
- рынок нативных Kotlin/Swift-разработчиков на порядок больше любого кроссплатформенного;
- **KMP не создаёт отдельного рынка найма и не требует его** — в схеме B вакансия
  формулируется как «Android-разработчик (Kotlin)» и «iOS-разработчик (Swift)», а KMP —
  строка в стеке;
- **C (CMP) создаёт узкое место**: человек, который умеет Compose Multiplatform *и*
  понимает iOS, — редкость (5 вакансий = рынка нет), и заказчику, который получает
  исходники, его искать будет трудно. Это прямой риск open-core-модели «заказчик владеет
  кодом».

---

## 5. Матрица оценок

Критерии и веса — из брифа (§ «Рабочие критерии оценки подхода»). Оценки 1–5.
Формула: `итог = Σ(оценка × вес) / Σ(вес) × 20`. При базовой сумме весов 100 это
эквивалентно `Σ(оценка × вес) / 5`; в сценариях чувствительности сумма весов другая,
поэтому используется общая формула — все итоги остаются в шкале 0–100 и сравнимы между
сценариями.

| Кандидат | Нативное качество (20) | Зрелость и риск (20) | Agent-friendliness (20) | Доля общего кода (15) | Тестируемость (15) | Экосистема (10) | **Итог** |
|---|---|---|---|---|---|---|---|
| **B. KMP shared + native UI** | 5 | 4 | 4 | 3 | 5 | 5 | **86.0** |
| **C. Compose Multiplatform (общий UI)** | 3 | 3 | 5 | 5 | 4 | 5 | **81.0** |
| **A. Нативно ×2** | 5 | 5 | 3 | 1 | 4 | 4 | **75.0** |
| E-Rust. Rust-ядро + native UI | 5 | 3 | 3 | 2 | 3 | 3 | **65.0** |
| D-2. Swift Android SDK (без Skip) | 5 | 2 | 3 | 3 | 3 | 2 | **62.0** |
| D-1. Skip (Fuse/Lite) | 4 | 2 | 1 | 4 | 3 | 3 | **55.0** |
| E-Go. Go-ядро (gomobile) | 5 | 1 | 3 | 2 | 2 | 3 | **54.0** |

**Топ-2**: B (86.0) и C (81.0). **Разрыв — 5.0 балла** (~6 %). Третий — A (75.0),
отставание от лидера 11 баллов.

### 5.1. Обоснование каждой клетки (по строке на клетку)

**B. KMP shared logic + native UI**

| Критерий | Оценка | Обоснование |
|---|---|---|
| Нативное качество | 5 | UI на обеих платформах — родной (Compose / SwiftUI), доступ к API day-0, системные компоненты, accessibility «из коробки», никакого рантайм-налога в бандле |
| Зрелость и риск | 4 | KMP stable с 11.2023, Google официально поддерживает и держит tier-1 Jetpack KMP-библиотеки; iOS-сторона — чистый Apple-стек; минус балл за Swift export в Alpha, SKIE от малого вендора и моновендорность Kotlin/Native |
| Agent-friendliness | 4 | `shared` целиком (включая `compileKotlinIosArm64`) и весь Android-таргет проверяются на Linux; `llms.txt`, MCP (klibs.io, IDE), Kotlin LSP, официальные AGENTS.md-рекомендации, Kotlin Benchmark; минус балл — SwiftUI-слой пишется вслепую |
| Доля общего кода | 3 | Домен, сеть, кеш, правила, тесты — один раз (типично 30–50 % кода бизнес-приложения); экраны и платформенные интеграции — дважды |
| Тестируемость | 5 | Вся общая логика тестируется один раз на JVM за секунды (быстрейший гейт из всех кандидатов); нативные UI-тесты остаются первоклассными на каждой платформе |
| Экосистема | 5 | Один Kotlin-клиент по OpenAPI Go-бэкенда, cookie-auth через Ktor (`HttpCookies`), self-host observability, Apache-2.0 везде, ничего обязательно-SaaS, дистрибуция не затронута |

**C. Compose Multiplatform (общий UI)**

| Критерий | Оценка | Обоснование |
|---|---|---|
| Нативное качество | 3 | Рендер на канве (Skia/Metal), системный look iOS 26 достижим только через нативную SwiftUI-оболочку (официальный туториал JetBrains), день-0 доступ к iOS-API — через JetBrains, iOS-бандл заметно больше (кейс Markaz: 137 МБ) |
| Зрелость и риск | 3 | Stable на iOS чуть больше года (с 1.8.0, 05.2025), один вендор на весь UI, максимальный lock-in (выход = переписать UI), релиз-ноты 1.12.0 показывают активную доводку платформенного поведения и `ERROR`-депрекации |
| Agent-friendliness | 5 | Весь UI собирается/запускается/скриншотится на Linux (Android + desktop-таргет), Compose Hot Reload **MCP-сервер**, `ComposeUiTest` выровнен с Android; слепым остаётся десяток строк хост-контроллера |
| Доля общего кода | 5 | 90–100 % по публичным кейсам (Markaz, Bitkey, Music Work) |
| Тестируемость | 4 | Логика и UI тестируются один раз; минус балл — XCUITest поверх Compose-канвы работает плохо, e2e на iOS деградирует |
| Экосистема | 5 | Та же, что у B (Ktor/OpenAPI/self-host/Apache-2.0) |

**A. Нативно ×2**

| Критерий | Оценка | Обоснование |
|---|---|---|
| Нативное качество | 5 | Эталон по определению |
| Зрелость и риск | 5 | Обязательства Google и Apple напрямую, никакого промежуточного вендора, нулевой lock-in |
| Agent-friendliness | 3 | Android-половина полностью headless на Linux; из iOS-половины на Linux доступен только SwiftPM-пакет без UIKit (Swift 6.4 + swift-testing работают на Linux), всё остальное — Mac |
| Доля общего кода | 1 | Общий только OpenAPI-контракт (и, возможно, дизайн-токены) |
| Тестируемость | 4 | На каждой платформе первоклассные инструменты, но всё пишется дважды; iOS-тесты недоступны на Linux |
| Экосистема | 4 | Совместимо, но два кодогена по OpenAPI, два mock-слоя, две обвязки и двойная синхронизация контракта |

**E-Rust. Rust-ядро + native UI**

| Критерий | Оценка | Обоснование |
|---|---|---|
| Нативное качество | 5 | Оба UI нативные, ядро не влияет на UX |
| Зрелость и риск | 3 | uniffi зрелый (Kotlin/Swift/Python — полная поддержка), проверено Element X / Mozilla / Bitwarden, но экосистема мобильного Rust узкая и найм в РФ по Rust мал |
| Agent-friendliness | 3 | `cargo test`/`cargo build` на Linux идеальны, но два нативных UI остаются, а линковка под iOS требует macOS |
| Доля общего кода | 2 | Только ядро; слой адаптации ядра к платформе и весь UI — дважды |
| Тестируемость | 3 | Ядро тестируется отлично; FFI-граница и два UI-слоя — нет |
| Экосистема | 3 | Третий язык в стеке open-core-шаблона, который заказчик должен поддерживать |

**D-2. Swift Android SDK (без Skip)**

| Критерий | Оценка | Обоснование |
|---|---|---|
| Нативное качество | 5 | UI нативный на обеих платформах |
| Зрелость и риск | 2 | Официальному SDK ~6 месяцев (Swift 6.3, 03.2026), UI-фреймворка для Android нет, `swift-java` 0.6.0 |
| Agent-friendliness | 3 | Swift toolchain и `swift sdk install` работают на Linux, общий пакет тестируется на Linux; но нет `llms.txt`, MCP-инструменты Swift завязаны на Xcode, iOS-приложение слепое |
| Доля общего кода | 3 | Как у B по объёму, но экосистема общих библиотек беднее |
| Тестируемость | 3 | `swift test` на Linux хорош; вокруг — пусто |
| Экосистема | 2 | ≈60 МБ Swift-рантайма в Android-бандле, нет зрелого OpenAPI/HTTP/DB-стека под этот сценарий |

**D-1. Skip**

| Критерий | Оценка | Обоснование |
|---|---|---|
| Нативное качество | 4 | На iOS — чистый SwiftUI (5); на Android — генерируемый Compose, но покрытие SwiftUI→Compose неполно, а Fuse даёт +≈60 МБ |
| Зрелость и риск | 2 | 1.9.9, вендор-микрокоманда, MPL-2.0, рынка найма нет, поверхность API быстро меняется |
| Agent-friendliness | 1 | **Требует macOS 15+**; на Linux не работает ни один шаг; `llms.txt` нет |
| Доля общего кода | 4 | Swift + SwiftUI шарятся почти целиком, минус платформенная доводка |
| Тестируемость | 3 | Swift-тесты общие, но только на Mac; Android-сторона тестируется через сгенерированный Kotlin |
| Экосистема | 3 | iOS-часть «ejectable», Android-часть в Fuse-режиме — нет; +60 МБ против РФ-дистрибуции |

**E-Go. gomobile**

| Критерий | Оценка | Обоснование |
|---|---|---|
| Нативное качество | 5 | UI нативный |
| Зрелость и риск | 1 | Нет тега, самоописан experimental, поддержки нет, биндинги Objective-C, не Swift |
| Agent-friendliness | 3 | Go на Linux прекрасен, но `gomobile bind` для iOS требует Xcode |
| Доля общего кода | 2 | Урезанное подмножество типов резко ограничивает, что реально вынести |
| Тестируемость | 2 | `go test` отличен, но за FFI-границей ничего |
| Экосистема | 3 | Теоретически переиспользует Go-модели бэкенда — единственный плюс |

### 5.2. Чувствительность

**Сценарий 1 — вес «agent-friendliness» падает 20 → 10** (остальные без изменений,
нормируем на сумму 90):

| Кандидат | Итог | Δ |
|---|---|---|
| **B** | **86.7** | +0.7 |
| C | 78.9 | −2.1 |
| A | 76.7 | +1.7 |

Победитель не меняется, **разрыв B над C растёт до 7.8**. Логично: C выигрывает именно
за счёт «всё видно на Linux»; обесценили — потерял.

**Сценарий 2 — вес «доля общего кода» растёт 15 → 25** (сумма 110):

| Кандидат | Итог | Δ |
|---|---|---|
| **B** | **83.6** | −2.4 |
| C | 82.7 | +1.7 |
| A | 70.0 | −5.0 |

Победитель тот же, но **B и C сходятся до 0.9 балла — практически ничья**. Значит, если
владелец решит, что «сколько пишется дважды» — главная боль, решение становится вопросом
вкуса и требует дополнительного критерия-тайбрейкера (мы предлагаем «обратимость»: из B
выйти дёшево, из C — нет).

**Сценарий 3 — оба изменения сразу + «нативное качество» 20 → 10** (профиль «внутренний
корпоративный инструмент, полировка платформы не важна»; веса 10/20/20/25/15/10):

| Кандидат | Итог |
|---|---|
| **C** | **85.0** |
| B | 82.0 |
| A | 67.0 |

**Точка переворота найдена**: C побеждает только тогда, когда одновременно (а) поднят вес
доли общего кода и (б) обесценено нативное качество. Это и есть честный критерий выбора
между B и C для конкретного проекта — см. § 6.3.

**Сценарий 4 — «тезис Shopify»: вес «доля общего кода» падает 15 → 5** (агенты удешевили
дублирование настолько, что объём общего кода почти не важен; веса 20/20/20/5/15/10 = 90):

| Кандидат | Итог | Δ к базе |
|---|---|---|
| **B** | **88.9** | +2.9 |
| A | 81.1 | +6.1 |
| C | 78.9 | −2.1 |

Результат важный и неочевидный: **даже если принять тезис Shopify целиком, B остаётся
первым**, а C уходит на третье место. Причина в том, что преимущество B — не только
«меньше писать дважды», но и «единственный быстрый headless-цикл для всей бизнес-логики»
(критерии agent-friendliness и тестируемость, вместе 35 из 100). Это ровно то, что Shopify
построил себе руками (CLI + Tardis), убрав логику из UI. A при этом заметно подтягивается
(75.0 → 81.1) и становится реальной альтернативой — но не победителем.

**Сценарий 5 — «тезис Shopify» + есть постоянный доступ к Mac** (веса как в сценарии 4,
но оценка agent-friendliness у A поднимается 3 → 4, потому что iOS-половина перестаёт
быть слепой; у B — 4 → 5 по той же причине):

| Кандидат | Итог |
|---|---|
| **B** | **93.3** |
| A | 85.6 |
| C | 78.9 |

Наличие Mac улучшает **оба** варианта и не меняет порядок. То есть «купить Mac» —
не аргумент в пользу смены кандидата, а аргумент в пользу того, чтобы вообще делать
мобильный шаблон.

### 5.3. Сводка: что должно было бы измениться, чтобы победил не B

| Победитель | Необходимые условия |
|---|---|
| **C** (Compose Multiplatform) | Обесценить «нативное качество» (20 → ~10, т. е. заказчик не требует «как родное») **и** поднять «долю общего кода» (15 → ~25). Плюс закрыть риск найма (сейчас 5 вакансий в РФ) |
| **A** (два нативных) | Принять тезис Shopify **и** дополнительно обесценить agent-friendliness (20 → ~10, т. е. Mac всегда под рукой и слепая половина — не проблема). При весах 20/20/10/5/15/10 получаем **A = 83.8 против B = 90.0** — даже так B впереди; A выигрывает **только** если сверх этого признать риск JetBrains неприемлемым (оценка зрелости B: 4 → 2, тогда **A = 83.8 против B = 80.0**) |
| D/E-кандидаты | Ни при каких разумных весах: их отставание (≥ 20 баллов) держится на зрелости и agent-friendliness, а не на весах |

---

## 6. Ответы на три вопроса

### 6.1. (а) Есть ли вариант, при котором Linux-разработчик с агентом делает 80 % работы над iOS без Mac?

**Короткий ответ: да — но «80 % работы над iOS-приложением» надо читать как «80 % кода,
который в итоге исполняется на iOS», и достигается это только архитектурным
обязательством держать SwiftUI-слой тонким.**

Что именно можно (все пункты — по докам, помечено):

| Действие | На Linux | Статус факта |
|---|---|---|
| Писать и прогонять все тесты общей логики (`:shared:jvmTest`) | да | проверено (обычный JVM/Gradle) |
| Компилировать общий код под `iosArm64`/`iosSimulatorArm64` в `.klib` | да, **если нет cinterop/CocoaPods** | проверено (kotlinlang.org/docs/native-target-support.html § Hosts) — эмпирика за потоком 06 |
| Проверять, что в `commonMain` не протекли платформенные API | да | это обычная компиляция таргета |
| Собрать `.framework` / `XCFramework` для iOS | **нет** | проверено (там же: «Building final binaries for Apple targets on Linux and Windows is also not possible») |
| Запустить iOS-тесты (`iosSimulatorArm64Test`) | **нет** | проверено (там же: запуск тестов Apple-таргетов — только на macOS-хосте) |
| Компилировать/проверять Swift-код, не использующий UIKit/SwiftUI (чистый домен) | да | Swift 6.4 toolchain на Linux, `swift build`/`swift test` — проверено по swift.org |
| Компилировать SwiftUI-код | **нет** | SwiftUI/UIKit на Linux отсутствуют |
| Симулятор, архив, подпись, TestFlight/App Store | **нет** | требуется macOS + Xcode |

**Реалистичная арифметика для бизнес-приложения-компаньона** (предположение, основанное
на пропорциях типового CRM-клиента, не измерение):

- домен + сеть + кеш + правила + маппинг ошибок + аналитика + их тесты: **45–60 %** кода —
  пишется и верифицируется агентом на Linux;
- Android UI + Android-платформенные интеграции: **20–25 %** — тоже полностью на Linux;
- iOS SwiftUI + iOS-платформенные интеграции: **20–30 %** — вслепую;
- сборка/подпись/симулятор — не код.

То есть **доля кода, которую агент на Linux пишет и проверяет, ≈ 70–80 %**, а доля
«iOS-специфичного кода, написанного вслепую» ≈ 20–30 %. Формулировка владельца «80 %
работы над iOS без Mac» достижима в смысле «80 % того, что работает на iOS, написано и
проверено без Mac».

**Рабочий контур** (предложение, требует подтверждения потоком 06 и владельцем):

1. **Гейт на Linux** (`make check`): `ktlint/detekt` + `:shared:jvmTest` (+ coverage-пол) +
   `:shared:compileKotlinIosArm64` (проверка, что общий код вообще собирается под iOS) +
   `:androidApp:lint` + `:androidApp:assembleDebug` + Compose-тесты Android. Это полный
   агентный цикл, ничего не требует от Mac.
2. **Second stage — «iOS verify»**: удалённый Mac (свой mini / арендованный
   MacStadium / macincloud / VM у РФ-провайдера — вопрос к потоку 05), на котором по
   команде выполняется `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64` +
   `xcodebuild build test -scheme App -destination 'platform=iOS Simulator,...'`.
   Запускается не на каждый коммит, а перед `claim «готово»` по iOS-части.
3. **Контракт-тесты вместо слепоты**: публичный API `shared` фиксируется snapshot-тестом
   сигнатур (Kotlin binary-compatibility-validator умеет `.api`-дамп) — агент на Linux
   видит, что он «сломал iOS», ещё до Mac.
4. **SwiftUI пишется по канону**: один reference-экран в `iosApp` + правило
   `.claude/rules/` «SwiftUI-слой — только представление; любая логика уезжает в shared».
   Тогда слепая часть — предсказуемая и мелкая.

**Честное ограничение**: без Mac **нельзя** выпустить iOS-приложение вообще
(подпись, архив, App Store Connect). Mac не «желателен» — он обязателен как последний
метр. Вопрос, где он живёт (железо / аренда / чужая машина), — к потоку 05.

### 6.2. (б) Когда два нативных шаблона правильнее одного KMP?

Правило «когда A > B» — по пяти триггерам, любой из которых достаточен:

1. **Команды разделены организационно и не пересекаются.** Если Android- и iOS-разработчик
   работают в разных компаниях/подрядах, ревьюят разный код и не встречаются, общий модуль
   деградирует. Все опрошенные РФ-команды говорят одно и то же: KMP-модуль **обязан**
   проектироваться обеими командами, с ревьюерами обеих платформ в каждом MR
   (CleverPumpkin), с еженедельными обменами и словарём терминов (MAGNIT OMNI).
   Нет этого процесса — нет и KMP.
2. **Приложения не «компаньоны одного бэкенда», а разные продукты.** Если у iOS и Android
   разные сценарии, разные API, разный жизненный цикл — общего почти нет, и KMP платит
   налог сборки, не давая экономии.
3. **Профиль — тяжёлые платформенные возможности** (AR, камера-heavy, аудио/видео-пайплайн,
   HealthKit/CarPlay/WidgetKit против Wear/Auto): общая часть вырождается, а expect/actual
   превращается в два параллельных кода с общим заголовком.
4. **Заказчику нужно отдать «просто Android-приложение»** (или только iOS) — например,
   внутренний инструмент для складских терминалов на Android. Тогда KMP-обвязка — чистый
   оверхед, а `mobile-template` всё равно можно форкнуть и удалить `iosApp` (см. § 6.3,
   это дешёвая операция).
5. **Риск JetBrains материализовался** (см. триггеры R-1…R-2 в § 7).

**Размер команды**: соло-разработчик с агентом — сильный аргумент **за** B (одна обвязка,
один гейт, меньше контекста). Пара «Android + iOS» — тоже за B при условии совместного
ревью. Три и больше независимых мобильных команд без общего техлида — против B.

**Найм**: сам по себе найм **не** аргумент против B, потому что B не требует
«KMP-разработчиков» (9 вакансий на всю РФ). Он становится аргументом против **C**
(5 вакансий на Compose Multiplatform), т. к. там весь UI требует редкой квалификации.

### 6.3. (в) Что это значит для формы шаблона(ов)

Четыре формы и их цена для четырёх наших измерений: агентная обвязка, гейт,
STOP-контракт, strategy-мост.

#### Вариант 1 — два репо: `android-template` + `ios-template`

| Измерение | Цена |
|---|---|
| Обвязка | **Две** `CLAUDE.md`, два набора `.claude/rules/`, два комплекта скиллов (`/smartdev`, `/plan`, `/init-service`, `/stack-deep-dive`, `/de-template`, `/scaffold`, `/release`). Дрейф между ними — вопрос времени |
| Гейт | Два: `make check` (Android, работает на Linux) и `make check` (iOS, **не работает на Linux**) — асимметрия, которую нечем закрыть |
| STOP-контракт | Два разных (AAB в RuStore/Play vs IPA в App Store), но это и так придётся определить |
| strategy-мост | Две карты в `<org>/strategy` для одной и той же поверхности `type: mobile` — либо одна карта, читаемая двумя репо (тогда зачем два репо) |
| Плюс | Форк берут по одной платформе; каждая обвязка проста и однородна |
| Минус | Дублирование контракта с бэкендом, дублирование mock-слоя, дублирование i18n, расхождение логики — системный риск |

#### Вариант 2 — один репо `mobile-template` (KMP: `shared/` + `androidApp/` + `iosApp/`) — **рекомендуется**

| Измерение | Цена |
|---|---|
| Обвязка | **Одна**, но с **path-scoped** правилами (у нас уже есть этот механизм): `.claude/rules/kmp-shared.md` → `shared/**`, `android-ui.md` → `androidApp/**`, `ios-ui.md` → `iosApp/**`, `kmp-public-api.md` → `shared/src/commonMain/**/api/**`. Это ровно то, как устроен `go-backend-template` |
| Гейт | **Один** `make check`, честно разделённый на «Linux-полный» и «macOS-only» стадии (см. § 6.1). Гейт печатает, какие проверки пропущены из-за отсутствия Mac, — не изображает зелёный |
| STOP-контракт | Один репо → два артефакта. Кандидат: **STOP = подписанный AAB + подписанный IPA в `dist/`, плюс handoff-блок**; загрузка в сторы — вне репо (аналог «image-in-registry» у сервис-шаблонов). Окончательно — поток 05 и 08 |
| strategy-мост | **Одна** карта в `<org>/strategy` для `type: mobile`: слайсы/модули + экраны, из которых `/scaffold` выводит и `shared/`-модуль, и `androidApp/`-экран, и `iosApp/`-view. Один источник — один мост |
| Плюс | Совпадает с дефолтной структурой JetBrains (2026-05), совместимо с требованием AGP 9.0, один `docs/stack`, один roadmap-план, один набор зависимостей в `gradle/libs.versions.toml` |
| Минус | Публичный roadmap придётся переписать (две строки → одна). Форкающему нужен только Android — удаляет `iosApp` (это ~1 модуль + 1 строка в `settings.gradle.kts`, дешевле, чем поддерживать два шаблона) |

#### Вариант 3 — три: общая библиотека + два приложения

| Измерение | Цена |
|---|---|
| Обвязка | Три (библиотека + два app-репо), как `golibinfra` + сервисы |
| Гейт | Три, плюс релизный цикл библиотеки (версии, changelog, совместимость) |
| STOP-контракт | Добавляется четвёртый артефакт — XCFramework/Maven-публикация shared |
| strategy-мост | Мост к библиотеке отдельно от мостов к приложениям |
| Когда оправдано | Когда shared реально переиспользуется **между проектами** (как `golibinfra`). У мобильных приложений разных заказчиков общего домена нет — значит, преждевременно |
| Примечание | Это ровно схема CleverPumpkin/Kassir.ru (CI собирает XCFramework в отдельный репо). Она уместна при разделённых командах, т. е. в сценарии из § 6.2, п. 1 |

#### Вариант 4 — гибрид: два нативных шаблона + опциональный KMP-модуль `shared`

| Измерение | Цена |
|---|---|
| Обвязка | Худшее из обоих миров: две обвязки **плюс** правила про опциональный общий модуль, который то есть, то нет |
| Гейт | Гейт, зависящий от наличия модуля, — источник «то зелёное, то красное» |
| Когда оправдано | Как **миграционный путь** для существующих нативных приложений (именно так делают Avito/X5/MAGNIT) — но у нас шаблон для greenfield |
| Вывод | Для шаблона — нет. Для документации «как встроить KMP в уже существующее приложение заказчика» — да, отдельной главой |

#### Рекомендуемая форма (детализация варианта 2)

```
mobile-template/                      → форк: <org>/mobile
├── CLAUDE.md                         # ядро + роутер (≤ ~200 строк, как в go-backend)
├── Makefile                          # единый интерфейс: make check / build / test / gen / release
├── .claude/
│   ├── rules/
│   │   ├── kmp-shared.md             # paths: shared/**
│   │   ├── kmp-public-api.md         # paths: shared/src/commonMain/**  — фасад под Swift export
│   │   ├── android-ui.md             # paths: androidApp/**
│   │   ├── ios-ui.md                 # paths: iosApp/**
│   │   ├── tests-pipeline.md
│   │   ├── env-local.md
│   │   ├── comments.md
│   │   └── forward-only.md
│   ├── skills/                       # /smartdev /plan /init-service /stack-deep-dive
│   │                                 # /de-template /scaffold /release
│   └── settings.json                 # deny --no-verify, force-push, правка сгенерированного
├── docs/
│   ├── stack/                        # 8 kinds, status: draft|stable, секция Generates
│   └── reference/{architecture,ecosystem,release,observability}.md
├── gradle/libs.versions.toml         # единый version catalog
├── settings.gradle.kts
├── shared/
│   └── src/{commonMain,commonTest,androidMain,androidUnitTest,iosMain,iosTest}/kotlin
├── androidApp/                       # AGP application plugin (обязательно отдельный модуль, AGP 9.0+)
└── iosApp/                           # Xcode project, SwiftUI, direct integration с shared
```

**Когда форкающему предлагать другую форму** (это решает `/init-service` вопросом):

| Ситуация | Форма |
|---|---|
| Нужен только Android | `mobile-template` + `/de-template` удаляет `iosApp` и iOS-таргеты из `shared` |
| Нужен только iOS | то же наоборот (и тогда `shared` вырождается — можно предложить чистый SwiftPM-проект) |
| Полировка платформы не критична, а экономия важнее (см. чувствительность, сценарий 3) | включить опциональный `sharedUi/` (Compose Multiplatform) и оставить `iosApp` как хост + Liquid-Glass-оболочку |
| Существующие нативные приложения заказчика | не форк, а глава «встраивание KMP-модуля» + вариант 3 (shared как отдельный репо/XCFramework) |

### 6.4. Рекомендации для шаблона: черновики конфигов, правил и гейта

Всё ниже — **черновик** уровня «чтобы поток 08 не начинал с нуля», не финальный дизайн.
Версии — из § 1 (срез 2026-09-17).

#### `gradle/libs.versions.toml` (черновик)

```toml
[versions]
kotlin = "2.4.20"
agp = "9.4.0"
ktor = "3.6.0"
coroutines = "1.11.0"
serialization = "1.11.0"          # stable-ветка; 1.12.0-RC не берём в шаблон
datetime = "0.8.0"
room = "2.8.5"
sqlite = "2.7.1"
datastore = "1.2.1"
lifecycle = "2.11.0"
navigation3 = "1.1.7"
koin = "4.2.2"
mockk = "1.14.11"
composeMultiplatform = "1.12.0"   # только если включён опциональный sharedUi
skie = "0.10.14"                  # опционально, до Beta Swift export

[libraries]
ktor-client-core        = { module = "io.ktor:ktor-client-core",        version.ref = "ktor" }
ktor-client-okhttp      = { module = "io.ktor:ktor-client-okhttp",      version.ref = "ktor" }   # androidMain
ktor-client-darwin      = { module = "io.ktor:ktor-client-darwin",      version.ref = "ktor" }   # iosMain
ktor-client-content-neg = { module = "io.ktor:ktor-client-content-negotiation", version.ref = "ktor" }
ktor-serialization-json = { module = "io.ktor:ktor-serialization-kotlinx-json",  version.ref = "ktor" }
ktor-client-mock        = { module = "io.ktor:ktor-client-mock",        version.ref = "ktor" }   # commonTest — mock-режим
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "coroutines" }
kotlinx-serialization   = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "serialization" }
androidx-room-runtime   = { module = "androidx.room:room-runtime",      version.ref = "room" }
androidx-datastore      = { module = "androidx.datastore:datastore-preferences", version.ref = "datastore" }
androidx-lifecycle-vm   = { module = "androidx.lifecycle:lifecycle-viewmodel", version.ref = "lifecycle" }
koin-core               = { module = "io.insert-koin:koin-core",        version.ref = "koin" }

[plugins]
kotlinMultiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
kotlinSerialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
androidApplication  = { id = "com.android.application", version.ref = "agp" }
androidLibrary      = { id = "com.android.library",     version.ref = "agp" }
```

Открытый вопрос к потоку 01/06: **Room KMP против SQLDelight** для `shared`. Room
официально в tier-1 у Google и имеет `room-runtime-iosarm64`, но нативный SQLite-драйвер
на iOS почти наверняка тянет cinterop — а это ломает возможность собирать klib на Linux
(ловушка № 1 в § 8). SQLDelight 2.4 имеет ту же проблему по той же причине. Если проверка
06-2 покажет, что cinterop неизбежен, вариант — **держать персистентность за
`expect/actual`**: интерфейс `LocalStore` в `commonMain`, Room на `androidMain`, реализация
на Swift (Core Data / GRDB / SQLite.swift) на iOS-стороне через reverse-import. Тогда
`shared` остаётся cinterop-free и Linux-цикл цел.

#### `shared/build.gradle.kts` (черновик, только каркас)

```kotlin
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    androidTarget()
    iosArm64()
    iosSimulatorArm64()
    jvm()                       // WHY: агентный цикл — тесты общей логики на JVM без эмулятора

    listOf(iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.neg)
            implementation(libs.ktor.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.koin.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.ktor.client.mock)   // mock-сценарии: empty | populated | authenticated | error
        }
        androidMain.dependencies { implementation(libs.ktor.client.okhttp) }
        iosMain.dependencies    { implementation(libs.ktor.client.darwin) }
    }
}
```

#### Гейт `make check` (черновик; честное разделение Linux / macOS)

```makefile
# Полный гейт, выполнимый на Linux. Всё, что требует macOS, вынесено в check-ios.
check: fmt-check lint test-shared compile-ios-klib api-check test-android build-android
	@echo "OK (Linux-часть). Стадия iOS: make check-ios — требует macOS/Xcode"

fmt-check:          ; ./gradlew ktlintCheck
lint:               ; ./gradlew detekt :androidApp:lintDebug
test-shared:        ; ./gradlew :shared:jvmTest          # основной агентный цикл, секунды
compile-ios-klib:   ; ./gradlew :shared:compileKotlinIosArm64   # ловит «протёк платформенный API»
api-check:          ; ./gradlew apiCheck                 # binary-compatibility-validator: диф публичного API shared
test-android:       ; ./gradlew :androidApp:testDebugUnitTest
build-android:      ; ./gradlew :androidApp:assembleDebug

# Только macOS. В CI-нет-раннеров-модели экосистемы запускается разработчиком/на удалённом Mac.
check-ios:
	./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
	./gradlew :shared:iosSimulatorArm64Test
	xcodebuild test -project iosApp/iosApp.xcodeproj -scheme App \
	  -destination 'platform=iOS Simulator,name=iPhone 17'
```

Принцип, унаследованный от `go-backend-template`: **гейт не изображает зелёный**. На Linux
`make check` печатает явную строку «стадия iOS пропущена, нужна macOS», а не молчит.

#### Правила `.claude/rules/` (черновик заголовков и сути)

| Файл | `paths:` | Суть |
|---|---|---|
| `kmp-shared.md` | `shared/**` | Что можно и нельзя в `commonMain`: никаких платформенных API; **никаких cinterop-зависимостей** (ломает Linux-цикл); платформенное — за `expect/actual` или за интерфейсом; side-effects не в конструкторах |
| `kmp-public-api.md` | `shared/src/commonMain/**` | Публичный фасад под Swift: только final-классы, data-классы, sealed-иерархии и `suspend`; **без дженериков в сигнатурах**; без наследников `List/Set/Map`; изменение фасада → `apiDump` в том же коммите |
| `android-ui.md` | `androidApp/**` | Compose, Material 3, собственный токен-слой (аналог правила `shared-ui` у SPA — без внешней DS), состояние только из `shared` |
| `ios-ui.md` | `iosApp/**` | SwiftUI — **только представление**; любая логика уезжает в `shared`; никаких сетевых вызовов и бизнес-правил в View; экран = «взять состояние из shared + отрисовать» |
| `tests-pipeline.md` | `**` | `commonTest` — обязателен для каждого use-case; coverage-пол на `shared`; UI-тесты Android на JVM/Robolectric; e2e — отдельная стадия |
| `env-local.md` | `**` | build-time (`BuildConfig` / Xcode `xcconfig`) против runtime (конфиг из бэкенда); секретов в бандле нет; **токен не в `UserDefaults`/`SharedPreferences`** (аналог правила SPA про localStorage) — Keychain / EncryptedSharedPreferences |
| `release-artifacts.md` | `**` | Что такое STOP-граница мобильного репо; запрет коммитить keystore/`.p12`/provisioning profiles |
| `comments.md`, `forward-only.md` | `**` | как в остальных шаблонах |

#### Процессные заимствования из практики Shopify (§ 3.4) — в скиллы

| Что | Куда |
|---|---|
| Чекпойнт-цикл: срез работы → тест доказывает поведение → визуальное ревью → adversarial-ревью → человек → коммит | `/scaffold` и `/plan` |
| Хеш плана как условие действительности приёмки (правка плана аннулирует одобрение) | `/plan` |
| Агенто-адресуемое состояние: CLI, который дергает `shared` headless и печатает состояние | отдельный `:shared:cli` таргет (JVM) — заменяет Tardis дешёвой ценой |
| «Паритет платформ держится спецификацией, а не кодом» | мост в `<org>/strategy`: карта `type: mobile` — единственный источник поведения обеих платформ |
| «Native expertise remained essential» → линтеры/статанализ/перф обязательны | `make check` |

---

## 7. Рекомендация, триггеры пересмотра, запасной вариант, запросы к потоку 06

### 7.1. Рекомендация

**Один репозиторий `mobile-template` (форк-цель `<org>/mobile`), кандидат B:
Kotlin Multiplatform для общей логики + нативный UI (Jetpack Compose на Android,
SwiftUI на iOS), с опциональным `sharedUi/` на Compose Multiplatform как
явно-включаемым расширением.**

Обоснование в одном абзаце: B выигрывает матрицу (86.0 против 81.0 у C и 75.0 у A) и,
что важнее, **выигрывает устойчиво** — он остаётся первым и когда agent-friendliness
обесценивают, и когда долю общего кода переоценивают; он единственный даёт агенту на
Linux полноценный быстрый цикл по всей бизнес-логике (`:shared:jvmTest` + klib под iOS),
сохраняя при этом нативное качество и нулевой вендор-риск на самой рискованной платформе
(iOS остаётся чистым Apple-стеком); он не требует несуществующего в РФ рынка
KMP/CMP-специалистов; он подтверждён и мировой практикой (Netflix, Autodesk, Baidu,
Philo, Quizlet), и российской (Avito — ядро BDUI, X5 Tech — «ускорились в два раза»,
CleverPumpkin/Kassir.ru — сетевой слой); и он обратим — выход из B стоит «реализовать
API shared дважды», выход из C стоит «переписать весь UI».

**Отдельно — почему разворот Shopify (§ 3.4) не меняет рекомендацию.** Их тезис
(«агенты удешевили писать дважды») в нашей матрице означает падение веса критерия
«доля общего кода»; посчитанный сценарий 4 даёт B = 88.9, A = 81.1, C = 78.9 — **B не
просто удерживает первое место, а увеличивает отрыв**. Причина: главный вклад B — не
экономия на дублировании, а единственный быстрый headless-цикл для всей бизнес-логики,
и это ровно то, что Shopify пришлось строить руками (CLI + Tardis + Helix). Их «до» было
React Native с рантаймом и мостом, а не KMP с нативным UI; их ограничение — не наше
(парк Mac'ов, десятки инженеров, экспертиза на обеих платформах). При этом **процессные
находки Shopify берём безусловно** (§ 6.4, таблица «Процессные заимствования») — они
технологически нейтральны и улучшают нашу обвязку независимо от выбора кандидата.

**Публичный roadmap**: строки `android-template` и `ios-template` заменяются одной
`mobile-template` — нативный Android + нативный iOS на общем Kotlin-ядре. Формулировку
для `stit-public` пусть предложит поток 08.

### 7.2. Условия пересмотра (триггеры)

| ID | Триггер | Что делать |
|---|---|---|
| **R-1** | JetBrains снижает приоритет KMP: из роадмапа Kotlin исчезает секция Multiplatform как ключевой приоритет, либо Swift export замораживается на Alpha > 12 месяцев (сейчас в фокусе «Alpha → Beta») | Заморозить развитие `shared`; новые фичи писать нативно; готовить откат к A |
| **R-2** | Google снимает KMP-поддержку с Jetpack: библиотеки из tier-1 (room, datastore, lifecycle, paging, savedstate, sqlite, annotation, collection) перестают выпускать iOS-артефакты | То же, что R-1; пересмотреть `shared` на «только Ktor + kotlinx» |
| **R-3** | Swift SDK for Android получает стабильный тег 1.0 **и** появляется зрелый слой общих библиотек (HTTP/DB/DI) для Swift-на-Android | Пересчитать матрицу для D-2: при равном agent-friendliness Swift-центричный вариант становится привлекательнее для команды с iOS-перекосом |
| **R-4** | Профиль заказчиков сдвигается: полировка платформы перестаёт быть требованием, а темп «одна фича — оба стора» становится главным KPI | Включать `sharedUi/` (CMP) по умолчанию — см. сценарий 3 чувствительности |
| **R-5** | Kotlin/Native перестаёт собирать Apple-klib на Linux-хосте (регрессия в докдиаграмме Hosts) | Агентный цикл теряет ключевую проверку → пересмотреть гейт, возможно поднять требование к удалённому Mac |
| **R-6** | Skip или аналог начинает поддерживать Linux как dev-хост | Пересчитать D-1: сегодня он падает почти целиком на одном ограничении |
| **R-7** | Рынок найма в РФ по Compose Multiplatform выходит за ~50 вакансий (сейчас 5) | C становится реальной альтернативой для заказчиков, которые получают исходники |
| **R-8** | «Эффект Shopify» подтверждается на нашем масштабе: за 2–3 реальных проекта видно, что агент реализует iOS-экран по Android-референсу без потери качества **и** что слепые правки ловятся на удалённом Mac не дороже, чем локальные | Пересмотреть в пользу A: `shared` перестаёт окупать свою сложность. Признак для измерения — доля коммитов, которые пришлось чинить после стадии `check-ios` |
| **R-9** | Появляется индустриальный кейс «ушли с KMP обратно в native из-за агентов» (сейчас таких нет; уходили с RN и с общего C++) | Тот же пересмотр, что R-8, но с внешним подтверждением |

### 7.3. Запасной вариант

**A — два нативных шаблона.** Не C. Причина: стоимость выхода. Откат B → A выполняется
эволюционно (домен за доменом «растворяется» в две нативные реализации, UI не трогается,
гейт распадается на два), и в любой момент проект остаётся рабочим. Откат C → что угодно
означает переписывание всего UI, то есть остановку продуктовой разработки.

Практическое следствие для шаблона: **`shared` не должен быть «единственным местом, где
живёт правда»** — каждая фича обязана иметь описание в `<org>/strategy`, из которого её
можно реализовать заново на любой платформе. Это уже принцип экосистемы («проектирование
первично, код вторичен»), здесь он просто становится страховкой от вендор-риска.

### 7.4. Запросы к потоку 06 (эмпирические проверки на Linux)

Приоритет сверху вниз; каждая проверка — с фиксацией времени, диска и точного вывода.

| # | Проверка | Зачем | Ожидание по докам |
|---|---|---|---|
| **06-1** | `./gradlew :shared:compileKotlinIosArm64` на Debian x86_64 для минимального KMP-модуля (Kotlin 2.4.20, без cinterop/CocoaPods). Зафиксировать: собирается ли `.klib`, сколько качает Kotlin/Native (`~/.konan`), сколько времени | **Ключевой факт всего отчёта**: возможность проверять общий код под iOS без Mac | Должно работать (native-target-support.html § Hosts) |
| **06-2** | То же с зависимостями `ktor-client-core` 3.6.0, `kotlinx-serialization-json`, `room-runtime` 2.8.5 — не появляется ли cinterop, который ломает Linux-путь | Реальный `shared` будет не пустым | Не проверено; **главный риск сценария** — Room/SQLDelight на iOS тянут cinterop к SQLite |
| **06-3** | `./gradlew :shared:jvmTest` (или `testDebugUnitTest`) — время холодного и тёплого прогона на этой машине (load average ~15) | Это и есть агентный цикл | Секунды–десятки секунд |
| **06-4** | `./gradlew :shared:linkDebugFrameworkIosArm64` — **убедиться, что падает**, и зафиксировать точный текст ошибки | Чтобы гейт и `CLAUDE.md` честно объясняли агенту, почему шаг пропущен | Должно падать |
| **06-5** | `./gradlew :shared:iosSimulatorArm64Test` — убедиться, что недоступно, зафиксировать сообщение | То же | Должно падать |
| **06-6** | Полный Android-гейт на Linux для `androidApp` (AGP 9.4.0, Compose): `lint` + `detekt`/`ktlint` + unit-тесты + `assembleDebug`. Время, диск, объём скачиваний | Базовая проверка, что «Android — да» не только в теории | Должно работать |
| **06-7** | Compose UI-тесты (`ComposeUiTest`) на JVM/Robolectric без эмулятора | Тестируемость без эмулятора — важный пункт гейта | Должно работать |
| **06-8** | Установить Swift 6.4 toolchain на Debian 13 и прогнать `swift build` + `swift test` для SwiftPM-пакета с Foundation и swift-testing (без UIKit) | Проверяет нюанс кандидата A и D-2: часть Swift-кода агент может верифицировать на Linux | Должно работать |
| **06-9** | `swift sdk install` Android-SDK на Linux + кросс-компиляция того же пакета под `aarch64-unknown-linux-android` | Проверяет D-2 как реальную опцию, а не как пресс-релиз | Не проверено |
| **06-10** | Compose Multiplatform: собрать `desktopApp` или Android-таргет с общим UI на Linux и **сделать скриншот** headless | Проверяет тезис «C даёт агенту видеть UI без Mac» | Должно работать |
| **06-11** | Compose Hot Reload MCP-сервер (CMP 1.12.0 / Kotlin Toolchain 0.12): поднимается ли на Linux, отвечает ли на `take screenshot` / `read semantic tree` | Это главный агентный козырь ветки Kotlin — проверить, а не поверить | Не проверено |
| **06-12** | Kotlin LSP (Apache-2.0) на Linux в headless-режиме на KMP-проекте: время индексации, работают ли go-to-definition/references | Влияет на стоимость агентного цикла | Не проверено |
| **06-13** | Оценить дисковую и сетевую цену полного KMP-тулчейна: Gradle + Kotlin 2.4.20 + Kotlin/Native + Android SDK (`~/.gradle`, `~/.konan`, `~/Android/Sdk`) | Для доков шаблона «что скачается при первом `make setup`» | ~несколько десятков ГБ, точное число нужно |
| **06-14** | `binary-compatibility-validator` (`apiDump` / `apiCheck`) на KMP-модуле с iOS-таргетами на Linux-хосте | Это предлагаемый механизм «агент видит, что сломал iOS, до Mac» (§ 6.1, п. 3) — нужно убедиться, что он вообще работает без macOS | Не проверено |
| **06-15** | Прототип «agent-addressable CLI»: JVM-таргет `shared`, который из командной строки дергает use-case'ы и печатает состояние JSON'ом (дешёвый аналог Tardis из § 3.4) | Проверяет, что headless-цикл Shopify воспроизводим на KMP «бесплатно», а не требует отдельной инфраструктуры | Должно работать; интересна эргономика и время запуска |

---

## 8. Ловушки

| Ловушка | Суть | Обход | Источник |
|---|---|---|---|
| **cinterop убивает Linux-путь** | Kotlin/Native на Linux делает `.klib` для Apple-таргетов **только без cinterop-зависимостей** (включая CocoaPods) | Держать в `shared` только чистые KMP-библиотеки; всё, что требует cinterop (нативный SQLite-драйвер, Keychain), выносить за `expect/actual` в `iosMain`, реализуя через Swift-сторону | kotlinlang.org/docs/native-target-support.html § Hosts |
| **AGP 9.0+ и multiplatform-модуль** | Нельзя применять Android application-плагин в KMP-модуле → старая структура `composeApp` ломается | Сразу закладывать `shared/` + `androidApp/` | blog.jetbrains.com/kotlin/2026/05/new-kmp-default-structure/ |
| **Swift export не терпит дженериков** | Kotlin-дженерики стираются до upper bound; наследники `List/Set/Map` игнорируются (KT-80416/80417); только final-классы от `Any` | Публичный API `shared` — плоский фасад из final-классов, data-классов, sealed и `suspend`, без дженериков | kotlinlang.org/docs/native-swift-export.html |
| **Swift export требует direct integration** | Работает только при direct integration, не при CocoaPods | Если нужен remote-XCFramework — остаёмся на Obj-C+SKIE до Beta | там же |
| **Liquid Glass недостижим из Compose** | Системные эффекты iOS 26 рендерит только нативный `TabView`/`NavigationStack` | Если выбран C — сразу проектировать SwiftUI-оболочку и отдавать ей навигацию | kotlinlang.org/docs/multiplatform/ios-liquid-glass.html |
| **XCUITest поверх Compose-канвы** | Compose-иерархия не является нативной view-иерархией → e2e на iOS деградирует | В C: полагаться на `ComposeUiTest`, не на XCUITest; в B проблемы нет | публичные разборы CMP; **не проверено первоисточником** |
| **Skip = macOS-only** | Ни один шаг не воспроизводится на Linux | Не выбирать; если владелец захочет — сначала убедиться, что Mac есть у всех | skip.tools/docs/gettingstarted |
| **Skip Fuse / Swift-на-Android: +≈60 МБ** | Swift stdlib + Foundation + ICU в APK | Только Skip Lite (беднее) или отказ | skip.tools/docs/modes |
| **Amper больше нет как отдельного продукта** | Репо `JetBrains/amper` архивирован 2026-05-29; продукт стал «Kotlin Toolchain», статус Alpha | В шаблоне — **Gradle + version catalog**, Kotlin Toolchain держать под наблюдением | blog.jetbrains.com/amper, blog.jetbrains.com/kotlin |
| **`gomobile` без тега** | Псевдоверсия, experimental, Objective-C-биндинги | Не использовать; Go-контракт — через OpenAPI | pkg.go.dev/golang.org/x/mobile |
| **Каденс Kotlin** | 2.4.0 (06.2026) → 2.4.10 (07.2026) → 2.4.20 (09.2026) — минор раз в ~1.5–3 месяца | Пин версий в `libs.versions.toml`; обновление — через план, а не «на месте» | GitHub Releases `JetBrains/kotlin` |
| **CMP депрекации со статусом ERROR** | В 1.12.0 `NativeCanvas`/`NativePaint` подняты до `ERROR` | Если выбран C — читать Migration Notes каждого минора как обязательный шаг | релиз-ноты CMP 1.12.0 |
| **hh.ru закрыл API** | `api.hh.ru/vacancies` без авторизации → HTTP 403 | Считать со страницы поиска (`totalResults` в встроенном JSON) — метод, которым получены цифры § 4 | проверено 2026-09-17 |

---

## 9. Риски и открытые вопросы владельцу

### 9.1. Риски выбранного варианта (B)

| ID | Риск | Вероятность | Влияние | Митигация |
|---|---|---|---|---|
| RB-1 | Swift export задерживается в Alpha; интероп остаётся «через Objective-C» с его мозолями | средняя | средн. | Правило «плоский фасад» + SKIE; фасад переживёт миграцию |
| RB-2 | SKIE (Touchlab) перестаёт развиваться | низкая | средн. | SKIE опционален: без него остаётся голый Obj-C-экспорт, приложение работает |
| RB-3 | Cinterop-зависимость проползает в `shared` и ломает Linux-цикл (`compileKotlinIosArm64`) | **высокая** | **высокое** | Гейт на Linux включает `compileKotlinIosArm64` — поломка ловится сразу; правило в `.claude/rules/kmp-shared.md` |
| RB-4 | Слепой SwiftUI-слой накапливает баги, которые видны только на Mac | средняя | средн. | Тонкий слой + snapshot-тесты API `shared` + отдельная стадия «iOS verify» |
| RB-5 | Заказчику, получившему исходники, некого нанять на «KMP» | низкая | средн. | В доках прямо писать: нужны обычные Kotlin- и Swift-разработчики; KMP — деталь стека |
| RB-6 | Kotlin/Native медленно компилируется, агентный цикл раздувается | средняя | низкое | `jvmTest` как основной цикл, `compileKotlinIosArm64` — реже (pre-push, не pre-commit) |
| RB-7 | Один репо на две платформы усложняет ролевое разделение команды | низкая | низкое | Путь «вариант 3» задокументирован как опция |

### 9.2. Вопросы владельцу (решения, которые не может принять ресёрч)

1. **Согласен ли владелец заменить в публичном roadmap две строки (`android-template`,
   `ios-template`) одной `mobile-template`?** Если нет — остаётся вариант 1 (два репо),
   и тогда общий Kotlin-слой либо исчезает (A, 75.0), либо переезжает в третий репо
   (вариант 3), что дороже по обвязке.
2. **Есть ли доступ к macOS-машине (своей или арендованной) и на каких условиях?**
   От этого зависит, реалистична ли вообще ветка «iOS verify» и что именно попадёт в
   гейт. Смежно — вопрос к потоку 05 (аренда Mac из РФ, оплата Apple Developer Program).
3. **Подтверждается ли профиль приложений** («бизнес-компаньоны к Go-бэкенду: авторизация,
   списки/формы/детали, push, deep links, камера/QR, карты, файлы; offline-tolerant,
   не offline-first; ru/en; РФ-первый рынок»)? Матрица чувствительна к этому: при сдвиге
   в сторону «внутренний инструмент, полировка не важна» побеждает C.
4. **Насколько «как родное» — продуктовое требование?** Если заказчик принимает
   Material-подобный интерфейс на iOS, C экономит ~40 % кода. Если нет — B.
5. **Нужен ли шаблону desktop/web-таргет** (KMP это даёт почти бесплатно)? Влияет на то,
   закладывать ли `sharedUi/` с самого начала.
6. **Кто будет писать iOS-часть** — владелец сам, агент вслепую, или привлечённый
   iOS-разработчик? Если «агент вслепую» — правило «тонкий SwiftUI» становится жёстким
   инвариантом гейта, а не рекомендацией.
7. **Приемлема ли MPL-2.0 / лицензионная политика для опциональных зависимостей?**
   Ядро выбранного стека — Apache-2.0, но если когда-нибудь появится желание взять
   Skip (MPL-2.0) — это отдельное решение по open-core.
8. **Что считать STOP-границей мобильного репо?** Предложение — «подписанные AAB + IPA
   в `dist/` + handoff-блок», загрузка в сторы вне репо. Финальное слово — за владельцем
   и потоком 05/08.
9. **Принимает ли владелец тезис Shopify целиком?** Это ключевая развилка мнения, а не
   фактов: индустрия в сентябре 2026 разделена — Shopify («агенты удешевили писать дважды»,
   уход с общей кодовой базы) против Philo («в агентном мире общая бизнес-логика по-прежнему
   ценна, меньшая поверхность кода = меньше контекста и токенов»). Наш расчёт (сценарий 4)
   говорит, что даже при полном принятии тезиса Shopify побеждает B — но если владелец
   хочет **сознательно** воспроизвести путь Shopify (два нативных приложения + агентный
   процесс + собственный headless-CLI вместо `shared`), это законный выбор, и тогда надо
   идти в A + вариант формы 1, а не в B. Ресёрч не может решить это за владельца, потому
   что решение зависит от того, во что он верит про траекторию агентов на 2–3 года.
10. **Есть ли готовность заимствовать процессные находки Shopify** (чекпойнт-цикл,
    хеш плана, agent-addressable CLI, adversarial-ревью)? Они дороже текущей обвязки по
    времени на задачу, но резко снижают долю «слопа». Это изменение `/plan` и `/scaffold`,
    то есть общее для всей экосистемы, а не только для мобильного шаблона.

### 9.3. Что осталось непроверенным в этом потоке

- Эмпирика Linux-сборки KMP — вся передана потоку 06 (§ 7.4), здесь только доки.
- Резюме (соискатели) на hh.ru — недоступны без работодательского аккаунта; цифры § 4 —
  только вакансии.
- Позиция Ozon / Яндекса / Сбера / VK / Wildberries / 2ГИС по KMP — прямых первоисточников
  с конкретикой в срез не нашлось (есть доклад Яндекса 2021 г. и упоминания в дайджестах);
  зафиксировано как **не проверено**, в матрице не используется.
- Реальный размер IPA/AAB для наших профилей — нужен spike (поток 06 или 08).
- Стоимость и легальность аренды macOS-раннера из РФ — поток 05.
- Пост Airbnb «Sunsetting React Native» (2018) — доступ не проверен, Medium отдаёт боту
  HTTP 403; содержание взято из вторичных разборов (InfoQ).
- **Долговечность «эффекта Shopify»** — принципиально не проверяемо сейчас: их посты от
  2026-09-10, первый мигрированный продукт (Shop app) в сторах меньше недели. Ни цифр
  поддержки после релиза, ни стоимости поддержания паритета «процессом» за год у нас нет.
  Использовать как свидетельство о направлении, не как доказанный результат.
- Заявленная «структурная симметрия SwiftUI ↔ Jetpack Compose», на которую опирается
  аргументация Shopify, эмпирически не измерялась — насколько хорошо агент переносит
  экран с одной декларативной системы на другую, надо проверять на своём коде
  (кандидат в задачи потока 06 или отдельного spike).
- Соотношение «доля общей логики / доля UI» для наших профилей (45–60 % / 20–30 %) —
  **предположение**, основанное на структуре типового CRM-клиента, а не измерение.
  Уточняется первым реальным проектом.

---

## 10. Сводная таблица версий

| Пакет / инструмент | Версия | Дата / срез | Лицензия | URL |
|---|---|---|---|---|
| Kotlin | 2.4.20 | 2026-09-07 | Apache-2.0 | https://github.com/JetBrains/kotlin/releases |
| Kotlin Gradle plugin | 2.4.20 | 2026-09-07 | Apache-2.0 | https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/kotlin-gradle-plugin/ |
| Kotlin Multiplatform | Stable | с 11.2023 | Apache-2.0 | https://kotlinlang.org/docs/multiplatform.html |
| Compose Multiplatform | 1.12.0 (stable) | 2026-08-25 | Apache-2.0 | https://github.com/JetBrains/compose-multiplatform/releases |
| Compose Multiplatform | 1.13.0-alpha01 | 2026-09-10 | Apache-2.0 | там же |
| Compose Hot Reload | 1.2.0 (в составе CMP 1.12.0) | 2026-08-25 | Apache-2.0 | https://github.com/JetBrains/compose-hot-reload/releases |
| Swift export (Kotlin→Swift) | **Alpha** | срез 2026-09-17 | Apache-2.0 | https://kotlinlang.org/docs/native-swift-export.html |
| SKIE | 0.10.14 | 2026-07-27 | Apache-2.0 | https://github.com/touchlab/SKIE |
| Swift | 6.4 | 2026-09-15 | Apache-2.0 w/ Runtime Library Exception | https://www.swift.org/blog/swift-6.4-released/ |
| Swift | 6.3 (первый офиц. Android SDK) | 2026-03-24 | там же | https://www.swift.org/blog/swift-6.3-released/ |
| swift-java | 0.6.0 | 2026-09-05 | Apache-2.0 | https://github.com/swiftlang/swift-java/releases |
| Skip | 1.9.9 | 2026-09-16 | **MPL-2.0** | https://github.com/skiptools/skip/releases |
| Kotlin Toolchain (ex-Amper) | 0.12.0, **Alpha** | 09.2026 | Apache-2.0 | https://blog.jetbrains.com/kotlin/ |
| AGP | 9.4.0 (stable) | срез 2026-09-17 | Apache-2.0 | Google Maven `com.android.tools.build:gradle` |
| AGP | 9.5.0-alpha05 | срез 2026-09-17 | Apache-2.0 | там же |
| Ktor client | 3.6.0 | срез 2026-09-17 | Apache-2.0 | Maven Central `io.ktor:ktor-client-core` |
| kotlinx-coroutines-core | 1.11.0 | срез 2026-09-17 | Apache-2.0 | Maven Central |
| kotlinx-serialization-json | 1.12.0-RC | срез 2026-09-17 | Apache-2.0 | Maven Central |
| kotlinx-io-core | 0.9.1 | срез 2026-09-17 | Apache-2.0 | Maven Central |
| kotlinx-datetime | 0.8.0 (+compat-вариант) | срез 2026-09-17 | Apache-2.0 | Maven Central |
| atomicfu | 0.33.0 | срез 2026-09-17 | Apache-2.0 | Maven Central |
| SQLDelight | 2.4.0-rc2 | срез 2026-09-17 | Apache-2.0 | Maven Central `app.cash.sqldelight` |
| Koin | 4.2.2 | срез 2026-09-17 | Apache-2.0 | Maven Central `io.insert-koin` |
| MockK | 1.14.11 | срез 2026-09-17 | Apache-2.0 | Maven Central |
| androidx.room (KMP) | 2.8.5 (`room-runtime-iosarm64` есть) | срез 2026-09-17 | Apache-2.0 | Google Maven |
| androidx.paging (KMP) | 3.5.1 (`paging-common-iosarm64` есть) | 2026-08-12 | Apache-2.0 | Google Maven |
| androidx.lifecycle / viewmodel (KMP) | 2.11.0 / 2.12.0-alpha03 | 2026-09-09 | Apache-2.0 | developer.android.com/kotlin/multiplatform |
| androidx.datastore (KMP) | 1.2.1 / 1.3.0-alpha11 | срез 2026-09-17 | Apache-2.0 | там же |
| androidx.sqlite (KMP) | 2.7.1 / 2.8.0-alpha01 | срез 2026-09-17 | Apache-2.0 | там же |
| androidx.savedstate (KMP) | 1.5.0 / 1.6.0-alpha03 | 2026-09-09 | Apache-2.0 | там же |
| androidx.annotation (KMP) | 1.10.0 / 1.11.0-rc01 | 2026-09-09 | Apache-2.0 | там же |
| androidx.collection (KMP) | 1.6.0 / 1.7.0-alpha01 | 2026-09-09 | Apache-2.0 | Google Maven group-index |
| Navigation (JetBrains KMP) | 2.10.1 / 2.10.0-beta01 (iOS-артефакт) | 2026-09-09 | Apache-2.0 | Maven Central `org.jetbrains.androidx.navigation` |
| Navigation3 (JetBrains KMP) | 1.1.7 / 1.2.0-beta01 (iOS-артефакт) | 2026-09-09 | Apache-2.0 | Maven Central `org.jetbrains.androidx.navigation3` |
| Kotlin LSP | активен, расширение «Kotlin by JetBrains» (Alpha с 05.2026) | 2026 | **Apache-2.0** | https://github.com/Kotlin/kotlin-lsp |
| «Java & Kotlin by IntelliJ IDEA» (VS Code) | preview | 08.2026 | проприетарная (после preview — IDEA Ultimate) | https://blog.jetbrains.com/idea/2026/08/intellij-idea-goes-lsp/ |
| Kotlin Benchmark (AI-агенты) | 105 задач; лидер 85.71 % | 07–08.2026 | — | https://kotlinlang.org/benchmark/ |
| klibs.io | 4 200+ KMP-проектов, MCP-сервер, скилл `kmp-libraries-expert` | 08.2026 | — | https://klibs.io/ |
| uniffi-rs | активен (Kotlin/Swift/Python — полная поддержка) | срез 2026-09-17 | MPL-2.0 | https://mozilla.github.io/uniffi-rs/ |
| `golang.org/x/mobile` | **без тега**, experimental | псевдоверсия 2026-09-08 | BSD-3-Clause | https://pkg.go.dev/golang.org/x/mobile |
| Xcode (для iOS-сборки) | 26.x (Skip требует ≥ 26.2) | срез 2026-09-17 | проприетарная | developer.apple.com |

---

## 11. Источники

**Kotlin / KMP / Compose Multiplatform**
- https://kotlinlang.org/docs/multiplatform.html
- https://kotlinlang.org/docs/roadmap.html (обновлён 08.2026, следующее обновление 02.2027)
- https://kotlinlang.org/docs/native-target-support.html (§ Hosts — ключевой факт про Linux)
- https://kotlinlang.org/docs/native-swift-export.html (Swift export — Alpha)
- https://kotlinlang.org/docs/multiplatform/multiplatform-ios-integration-overview.html
- https://kotlinlang.org/docs/multiplatform/ios-liquid-glass.html
- https://kotlinlang.org/docs/ai-for-development.html (AGENTS.md, MCP, Kotlin AI skills)
- https://kotlinlang.org/lp/multiplatform/case-studies/
- https://kotlinlang.org/benchmark/
- https://kotlinlang.org/llms.txt (HTTP 200, 49 494 B)
- https://www.jetbrains.com/help/kotlin-multiplatform-dev/llms.txt (HTTP 200, 1 501 200 B)
- https://github.com/JetBrains/kotlin/releases
- https://github.com/JetBrains/compose-multiplatform/releases (в т. ч. тело релиза 1.12.0)
- https://blog.jetbrains.com/kotlin/ (индекс: CMP 1.12.0, Kotlin Toolchain 0.12, Kotlin Benchmark, klibs.io)
- https://blog.jetbrains.com/kotlin/2026/05/new-kmp-default-structure/
- https://blog.jetbrains.com/kotlin/2026/07/introducing-the-kotlin-benchmark-evaluate-ai-coding-agents-on-real-world-kotlin-tasks/
- https://blog.jetbrains.com/kotlin/2026/08/klibsio-grows-to-4200-kmp-projects-with-smarter-discovery-and-new-ai-integrations/
- https://blog.jetbrains.com/idea/2026/08/intellij-idea-goes-lsp/
- https://blog.jetbrains.com/kotlin/2025/05/compose-multiplatform-1-8-0-released-compose-multiplatform-for-ios-is-stable-and-production-ready/
- https://klibs.io/

**Google / AndroidX**
- https://developer.android.com/kotlin/multiplatform (официальная поддержка KMP, tier-таблица)
- https://dl.google.com/dl/android/maven2/androidx/*/group-index.xml и `maven-metadata.xml`
  (проверка наличия `-iosarm64`-артефактов)

**Swift / Apple**
- https://www.swift.org/blog/ (индекс)
- https://www.swift.org/blog/swift-6.4-released/
- https://www.swift.org/blog/swift-6.3-released/
- https://www.swift.org/blog/nightly-swift-sdk-for-android/
- https://github.com/swiftlang/swift-java/releases
- https://github.com/swiftlang/sourcekit-lsp
- https://github.com/swiftlang/swift-format

**Skip**
- https://skip.tools/
- https://skip.tools/docs/
- https://skip.tools/docs/gettingstarted/ (требование macOS 15+)
- https://skip.tools/docs/modes/ (Fuse vs Lite, +≈60 МБ)
- https://github.com/skiptools/skip/releases (1.9.9, 2026-09-16)
- https://raw.githubusercontent.com/skiptools/skip/main/LICENSE.TXT (MPL-2.0)

**Индустриальные развороты (кросс-платформа ↔ native)**
- https://shopify.engineering/back-to-native — «Native is now the future of mobile at
  Shopify», Mustafa Ali, 2026-09-10 (**ключевой источник § 3.4**)
- https://shopify.engineering/shop-app-migration — «Migrating Shop app from React Native
  to native», 2026-09-10 (цифры старта/размера/стабильности, Helix, Tardis)
- https://www.infoq.com/news/2026/09/shopify-drops-react-native/ — разбор и дискуссия
  сообщества, 2026-09-16
- https://shopify.engineering/five-years-of-react-native-at-shopify — январь 2025,
  подтверждение ставки на RN (контекст разворота)
- https://dropbox.tech/mobile/the-not-so-hidden-cost-of-sharing-code-between-ios-and-android
  — Dropbox, 2019
- https://www.infoq.com/news/2019/11/mobile-share-code-costs/ — Dropbox + Slack, разбор
- https://medium.com/airbnb-engineering/sunsetting-react-native-1868ba28e30a — Airbnb, 2018
  (**доступ не проверен**: Medium отдаёт боту HTTP 403; содержание известно по вторичным
  разборам, в т. ч. по цитированию в InfoQ выше)
- https://www.theregister.com/2020/01/30/shopify_shifts_its_mobile_development_to_react_native/
  — Shopify → RN, 2020

**Rust / Go**
- https://mozilla.github.io/uniffi-rs/latest/
- https://github.com/element-hq/element-x-ios
- https://github.com/element-hq/element-x-android
- https://github.com/element-hq/matrix-rust-components-swift
- https://pkg.go.dev/golang.org/x/mobile

**Amper / Kotlin Toolchain**
- https://blog.jetbrains.com/amper/2026/06/kotlin-toolchain-0-11/
- https://blog.jetbrains.com/amper/2026/03/amper-0-10/
- https://github.com/JetBrains/amper (архивирован 2026-05-29)

**Российская практика**
- https://habr.com/ru/companies/cleverpumpkin/articles/1012648/ — CleverPumpkin + AvitoTech
  + X5 Tech + MAGNIT OMNI, 2026-03-23 (все цитаты § 3.2 отсюда)
- https://habr.com/ru/companies/yandex/articles/542454/ — доклад Яндекса о KMP (исторический)
- https://mobiusconf.com/archive/2025%20Spring/talks/20006894-kotlin-multiplatform-in-a-large-project/
- https://hh.ru/search/vacancy?text=<...>&area=113 — цифры найма § 4, снято 2026-09-17

**Внутренние (read-only контекст экосистемы)**
- `/home/mm/git/stit-core/go-backend-template/CLAUDE.md`
- `/home/mm/git/stit-core/spa-frontend-template/CLAUDE.md`
- `/home/mm/git/stit-core/corp-infra-template/docs/ecosystem.md`
- `/home/mm/git/stit-core/stit-public/docs/roadmap.md`
- `/home/mm/git/stit-core/mobile-template/docs/research/brief.md`
