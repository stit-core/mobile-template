# Мобильный шаблон экосистемы stit — итоги ресёрча (сводный синтез)

Дата: 2026-09-17. Цель ресёрча — решить, **как** делать мобильные приложения в экосистеме stit
(Swift/Kotlin, без Flutter): стек, архитектура, тесты, качество, структура репозитория,
дистрибуция в РФ, контракт с Go-бэкендом, агентная обвязка — и **сколько шаблонов заводить**
(вопрос владельца: два `android-template` + `ios-template`, четыре, один, иное). Это сводка
восьми потоков; подробности, листинги, измерения и источники — в отчётах потоков (карта — в
конце). Отчёты написаны как спецификации: точные версии, конфиги, ловушки с URL, — чтобы по ним
проектировать и писать шаблон без повторного ресёрча.

Критерии оценки подхода — `brief.md` (веса 20/20/20/15/15/10: нативное качество · зрелость и
риск · agent-friendliness · доля общего кода · тестируемость · совместимость с экосистемой).
Профиль приложений (предположение брифа, владелец подтверждает): бизнес-компаньоны к Go-бэкенду
(CRM/задачи/звонки/сделки), РФ-первый рынок, open-core, разработчик на Linux с AI-агентом.

---

## 1. Решения (TL;DR)

1. **Шаблонов — один: `mobile-template` → форк `<org>/mobile`.** Не два (`android-template` +
   `ios-template`) и не четыре (два шаблона + два kit'а). Kotlin Multiplatform-ядро `shared/`
   (модели, API-клиент, auth-сессия, репозитории, презентеры экранов, deep-link-роутер,
   mock-транспорт) + **нативный UI**: `androidApp/` (Jetpack Compose) и `iosApp/` (SwiftUI).
   Это кандидат B из матрицы потока 03 (**86,0** против 81,0 у Compose Multiplatform и 75,0 у
   двух нативных), принятый потоком 08 после сверки с ограничениями 05/06/07. Форки «только
   Android» / «только iOS» — не отдельные шаблоны, а `/de-template --platform android|ios`.
   Продуктовое деление (аналог «приложение vs сайт» веб-линейки) проходит по дистрибуции и
   обязательствам, а не по runtime, — это поле **`preset: companion | consumer | enterprise`** в
   `docs/stack/service.md`, не репозиторий. Kit-библиотеки (`mobilekit`/`swiftkit`, вариант «4 =
   2 + 2») — по триггеру «второй форк в эксплуатации + первый фикс, который должен доехать в оба»
   (прецедент `golibinfra` извлекали из работающих сервисов, не до них); до триггера kit-образный
   код живёт в `shared/…/core` и `iosApp/Packages/Core` с машинно проверяемой границей.
   Две строки публичного roadmap сворачиваются в одну (формулировки — §8.1).
2. **Почему один KMP-репо.** (а) Главный constraint экосистемы — разработчик и агент на Linux:
   только общее Kotlin-ядро даёт проверяемую работу над iOS-половиной без Mac — **65–80 % кода
   агент пишет и верифицирует на Linux** (shared 45–60 % + Android 20–25 %), слепым остаётся
   тонкий SwiftUI-слой (20–30 %); нативный `ios-template` был бы шаблоном, который владелец не
   может ни собрать, ни протестировать. (б) Один STOP, один тег `vX.Y.Z`, одна карта в
   strategy — мобильный клиент одна сущность продукта. (в) Владелец-соло: одна обвязка вместо
   двух. (г) Откат дешёв в одну сторону: B → два нативных — `subtree split` + «растворение»
   `shared/`; обратный путь — переписывание. Запасной вариант — A (два нативных), **не C**.
   Условия пересмотра — 03 §7.2 (R-1…R-9) и §11.
3. **Платформы сдвинулись прямо перед ресёрчем** (§2): Xcode 27 / iOS 27 / Swift 6.4 — 14–15.09.2026,
   с апреля 2027 App Store требует iOS 27 SDK, Liquid Glass обязателен; Android — AGP 9.4 сам
   компилирует Kotlin (kapt невозможен), Navigation 3 — канон, Compose Test API v2. Стек:
   Kotlin 2.4.20 · AGP 9.4.0 · Gradle 9.7.1 · JDK 21 · Compose BOM 2026.09.00 (Material 3 1.4, без
   Expressive) · compileSdk/targetSdk 37 · minSdk 26 · Hilt 2.60.1 (только `androidApp/`) · Ktor 3.6
   (в `shared/`) · Xcode 27 · Swift 6.4 · iOS 18 deployment target · SwiftPM-пакеты без
   Tuist/XcodeGen · Swift Testing · swift-snapshot-testing · SwiftLint + swift-format.
4. **Эмпирика на этой машине (06, §3):** Android-гейт целиком headless на Linux, включая
   визуальную регрессию (Roborazzi PNG, который агент читает) — `make check` 4,2 с без изменений
   / 37 с после правки файла / 94 с на свежем клоне; **KMP: klib для iOS-таргетов собирается на
   Linux, `linkDebugFrameworkIosArm64` — SKIPPED с кодом 0 без предупреждения** (гейт обязан
   проверять артефакт на диске); Swift 6.4 работает на Debian 13 без `apt` (`swift test` 0,72 с);
   `SwiftUI`/`UIKit` на Linux — `no such module`; память — 4,8 ГБ RSS демонов, `MaxMetaspaceSize ≥ 1g`
   обязателен; Google выпустил agent-first CLI `android` (`sdkmanager` deprecated).
5. **Архитектура внутри репо (§8.2):** DI — Hilt в `androidApp/`, в `shared/` — ручной composition
   root `SharedContainer` (он же плоский фасад для Swift; Koin/Metro/kotlin-inject — не сейчас);
   сеть — Ktor в `shared/`, **модели генерируются из `api/openapi.yaml` (models-only), клиенты
   пишутся руками**; Retrofit, Swift OpenAPI Generator, Room/SQLDelight, Compose Multiplatform,
   TCA/Combine, Tuist/XcodeGen, Periphery, Firebase (кроме FCM-транспорта) — в шаблон не входят
   (у каждого — задокументированный шов). Логика состояний экрана живёт в презентерах `shared/`
   (`StateFlow<State>` + `dispatch(Intent)`, JVM-тесты на Linux); Compose-экран и SwiftUI-view —
   тонкие адаптеры. Матрица зависимостей enforce'ится Gradle-графом + Konsist + detekt
   `ForbiddenImport` + Android Lint (Android/shared) и `Tools/archlint` + SwiftLint custom-rules (iOS).
6. **Контракт с бэкендом (07, §6):** бэкенд сегодня cookie-only и заточен под браузер, а
   `in: cookie` ломает кодоген на обеих платформах → канон для мобильных — **Bearer + refresh в
   JSON** (`X-Client`, сессии, ротация, отзыв цепочки) с BACKEND-HANDOFF B-1…B-19; cookie — режим
   совместимости `AuthMode.Cookie`. Спек — закоммиченная копия `api/openapi.yaml` + `openapi.lock`
   (бэкенд получает `make openapi` + `gen-check`). Хранение токенов — своя `SecureStore`
   (`security-crypto` deprecated целиком) / Keychain. Kill-switch и флаги — `GET /api/app/requirements`.
   Побочная находка: SPA-шаблон и бэкенд сегодня несовместимы по контракту (F-1).
7. **Дистрибуция и РФ (05, §5):** Android — **RuStore (основной, официальный API публикации →
   релиз скриптуется с Linux) + Google Play (только бесплатное)**; iOS — App Store, но
   **узкое место — оплата Apple Developer Program из РФ (с 01.04.2026) — решение владельца**;
   demo-режим — требование App Review → mock-режим = reference-фича №1; push — мульти-провайдер
   RuStore + FCM + APNs с первого дня; observability — Sentry SDK с пустым DSN + self-host
   GlitchTip; Firebase не закладывать; `ANALYTICS=none`; **Mac mini M4 (67–73 тыс. ₽) как
   build-host экосистемы, агент дирижирует по SSH**, Xcode Cloud — резерв; rollback не существует
   (только `Z+1`); версии из тега.
8. **STOP-контракт двухступенчатый (§8.6):** STOP-1 «артефакт готов» = **Gitea Release на
   аннотированном теге `vX.Y.Z`** с подписанными AAB+APK / IPA+dSYM.zip + `mapping.txt` +
   `checksums.sha256` + release-notes ru/en + `openapi.lock` + `store-handoff.md`, символы
   загружены — мобильный аналог «image-in-registry»; STOP-2 «сдано в каналы» = Play internal /
   RuStore на модерации / TestFlight; дальше — человек по STORE-HANDOFF. `/release --platform
   android|ios|both --stop 1|2`; отклонение модерации — штатная ветка.
9. **Гейт (§8.4):** один `make check` = Linux-полный (21 стадия: форматеры → detekt → Lint → JVM/
   Robolectric/Konsist → Roborazzi → Kover → `kmp` (klib с проверкой каталога) → `apiCheck` →
   assembleDebug → SwiftLint/archlint/`swift test` чистых пакетов → `gen-check`/`tokens`/`i18n`/
   `deeplinks`) и завершается **явным блоком «macOS-стадии не выполнены на этом хосте»** (код 0);
   `make check-mac` — по SSH на Mac; `make check-full` — обязателен для `/release`; `make
   check-fast`; **`make gate-selftest`** на негативных фикстурах (линтер обязан упасть — 10
   проверено красными в 06); один автофиксер формата на язык; полы покрытия shared 80 / Android
   70 / Swift `Pure` 70; `GATE_RELEASE=1` (R8) — в pre-push и `/release`.
10. **Обвязка (§8.3):** `CLAUDE.md` ≤ 200 строк с блоком «Где это в экосистеме» и инвариантами
    «что в 2026 иначе, чем агент помнит»; **23 path-scoped правила** (21 мобильное + `forward-only`,
    `comments`); `settings.json` с deny на релизные сборки/ключи/`sdkmanager`/сгенерированное;
    **9 скиллов** в форме Go/SPA (`/smartdev` с `make doctor` первым, `/plan` с атомом «Mac-verify»,
    `/triage-failure`, `/init-service`, `/stack-deep-dive`, `/de-template --platform`, `/scaffold`
    по kinds strategy, `/release`, `/infra-handoff` с тремя адресатами); `AGENTS.md` — указатель
    на `CLAUDE.md`; официальные Android-скиллы ставятся `make setup`, не вендорятся.
11. **Слой-мост в strategy (§8.5):** `docs/projects/<slug>/mobile/<surface>/` — 9 kinds
    (`entities`, `screens`, `flows`, `features`, `shell`, `entry-points`, `permissions`,
    `shared-api`, `shared-lib`) **замещает `screens/` для `type: mobile`** (как `telegram-bot-flow/`
    для ботов); FSD-имена не переносятся — переносятся принципы. Канон-PR S-1…S-3 в
    `business-strategy-template` (+ `/mobile-deep-dive`, `/mobile-audit`, ракурсы `/cross-audit`)
    и путь без PR (копия канона в `docs/reference/strategy-mobile-canon/`).
12. **`docs/stack` — 13 обязательных kinds + 2 условных**: `service` (+`platforms`, `preset`),
    `runtime`, `api`, `auth`, `deep-links`, `push`, `observability`, `styling` (+i18n),
    `distribution`, `signing`, `compliance`, `dependencies`, `config`; условные — `analytics`,
    `payments` (обязательны при `preset: consumer`). `navigation` — не kind.
13. **Cross-repo (§9):** P0 — corp-канон (7-й шаблон, контракт «artifact-in-release»,
    Vaultwarden-коллекция `mobile/<org>/<app>`, ntfy-топик), бэкенд B-1…B-3 (Bearer), B-12
    (`make openapi`), B-17 (`/api/app/requirements`), инфра I-1 (`wellknown` для `.well-known`) и
    I-6 (URL политики и удаления аккаунта), strategy S-1…S-3, stit-public P-1 (последним).
    Первый релиз блокируют только custody ключей (C-3) и правовые тексты (I-6/№ 35).
14. **План (§12):** 8 фаз с гейтами; **критический путь до Android-релиза (RuStore + Play
    internal) выполним целиком без Mac**; фаза iOS ждёт Mac-хост и Apple-аккаунт и не блокирует
    остальное — до неё iOS помечается «не верифицирован». Старт — `KMP-App-Template-Native` +
    `android create` + Xcode «SwiftUI App» + spike 06 (`spikes/android-gate` уже содержит
    прогнанный гейт). Исполнитель — Fable по этому синтезу и отчётам 01–08.
15. **Вопросов владельцу — 44 (§10), блокирующих — 5**; сам шаблон блокирует только один: «один
    репо вместо двух обещанных». Главные риски (§11): регресс Kotlin/Native на Linux (fallback A),
    слепая зона Swift до Mac, iOS-канал из РФ, Play developer verification 2027, рост времени
    гейта, RAM на общей машине.

---

## 2. Платформы на 2026-09-17: что изменилось и что фундаментально

### 2.1 Android (поток 01, `01-android-canon.md`, 3479 строк; 126 фактов проверено)

Стек, проверенный по Google Maven / Maven Central / kotlinlang: **Kotlin 2.4.20** (07.09.2026;
language-релиз раз в 6 мес → обязательный bump Kotlin раз в полгода, тянет KSP + Compose
compiler) · **AGP 9.4.0** (Gradle ≥ 9.6, JDK ≥ 17, потолок compileSdk 37) · **Gradle 9.7.1** ·
JDK-toolchain **21** (Robolectric 4.16+ требует 21 для SDK 36/37) · **Compose BOM 2026.09.00**
(compose 1.12.1, material3 1.4.0; Material 3 Expressive в stable **нет** — только 1.5.0-alpha) ·
**compileSdk/targetSdk 37** (Android 17 stable с 16.06.2026) · **minSdk 26** (96,1 % устройств
глобально; данных по РФ нет) · Hilt 2.60.1 · Navigation 3 (`androidx.navigation3` 1.1.7) ·
DataStore 1.2.1 · Room 2.8.5 (Room 3 = `androidx.room3` 3.0.3, KMP) · Retrofit 3 + OkHttp 5.5
(Android-only) / Ktor 3.6 (при KMP) · kotlinx-serialization 1.11.

**Три сдвига канона, которые агент из памяти напишет неверно:**

1. **AGP 9 сам компилирует Kotlin.** Плагин `org.jetbrains.kotlin.android` не применяется и
   несовместим с новым DSL; `android.kotlinOptions{}` → `kotlin { compilerOptions }`; **kapt
   невозможен** (только KSP); старый variant API удалён; отключить нельзя с AGP 10.
2. **Навигация — Navigation 3** (back-stack-as-a-list поверх Compose; stable с 19.11.2025):
   официальная рекомендация, её взяли Now in Android и Signal; Navigation Compose 2.x — предыдущее
   поколение.
3. **Compose Test API v2** (`androidx.compose.ui.test.junit4.v2`): старые
   `createComposeRule`/`runComposeUiTest` deprecated; v2 дефолтит `StandardTestDispatcher`.

Архитектура **не менялась**: `ui → domain (опц.) → data`, UDF, `ViewModel` + `StateFlow<UiState>` +
`collectAsStateWithLifecycle()`, репозиторий как SSOT; официально «Do not send events from the
ViewModel to the UI». MVI-фреймворки (Orbit, Circuit, Decompose, Mavericks) — ниша, не база.
Модульность — по фичам: `:app` + `:feature:<x>` + `:core:*` + `build-logic` (includeBuild,
convention-плагины `stit.*`) + version catalog. Архитектурный линтер уровня Steiger/depguard
собирается из трёх кусков: Gradle-граф (физически запрещает `feature:a → feature:b`), detekt
`ForbiddenImport`/`ForbiddenMethodCall` (аналог forbidigo) и Android Lint (свой `:lint` с
`Detector`'ами); Konsist — четвёртый слой с оговоркой (последний релиз 0.17.3 — 2024-12-08).

Гейт **целиком headless на Linux** (см. §3): Spotless + ktlint 1.8.0 (Pinterest прекратил
поддержку; продолжение `io.github.ktlint.core` 2.0.0-ALPHA-4) · detekt **1.23.8** (stable
заморожен; `dev.detekt` 2.0 — alpha.6) + compose-rules **0.4.28** (≥ 0.5.0 требует detekt
2.0-alpha) · Android Lint (`warningsAsErrors`) · JUnit 4 + coroutines-test + Turbine ·
**Robolectric 4.17** (SDK 23–37) + Compose-тесты · **Roborazzi 1.74.0** (официальный
`com.android.compose.screenshot` deprecated в alpha16) · Kover 0.9.9 (floor 75 %) ·
`assembleRelease` R8 full mode · `buildHealth` (не поддерживает AGP 9.4 — пока вне гейта).
Устройство/эмулятор нужны только для Espresso/UIAutomator, GMD, Macrobenchmark/Baseline Profiles
и Maestro.

**Официальный агентный тулинг Google появился в 2026**: CLI `android` в cmdline-tools 23.0
(`sdkmanager` объявлен deprecated; `android docs search/fetch`, `android skills add`, `android
create` — уже AGP 9 + Nav3, `android describe`, `android screen capture --annotate`) и репозиторий
`android/skills` (Apache-2.0) — прямой аналог `.claude/skills/`.

Мода, которую не брать: MVI-фреймворки как база, обязательный domain-слой/use-case на всё,
RxJava, kapt, View-система, Dagger вручную, LiveData, AndroidViewModel, GlobalScope, Accompanist,
`material-icons-*` (библиотека прекращена — иконки Material Symbols в `res/drawable`).

### 2.2 iOS (поток 02, `02-ios-canon.md`, 2061 строка; всё «по документации» — Mac нет)

**Платформа сдвинулась за три дня до ресёрча** (проверено на developer.apple.com): **Xcode 27
(27A266a) и iOS 27.0 — 14.09.2026, Swift 6.4 — 15.09.2026**; Xcode 27 требует macOS Tahoe 26.6+
и **Apple silicon** (macOS 27 — только ARM). **С апреля 2027 App Store Connect требует iOS 27
SDK** (объявлено 09.09.2026) → шаблон строится на Xcode 27 сразу. **Liquid Glass обязателен**:
`UIDesignRequiresCompatibility` игнорируется при сборке под iOS 27+. Deployment target —
**iOS 18.0** (iOS 26 — 86 % устройств за 4 года / 79 % всех, замер 07.06.2026; iOS 26 — если парк
заказчика управляемый).

Канон: **Swift 6 language mode + approachable concurrency** (новый проект Xcode 26/27 получает
`SWIFT_DEFAULT_ACTOR_ISOLATION = MainActor`, `SWIFT_APPROACHABLE_CONCURRENCY = YES` —
приложение снова однопоточное по умолчанию, уход — осознанный `@concurrent`); **SwiftUI +
`@Observable` state-объекты + протокольные зависимости через `Environment`/конструктор +
`NavigationStack` с типизированным путём** («MV / MVVM-lite»; спор MVVM vs MV —
терминологический). TCA (1.26.2) — живой, но дорогой (компиляция, кривая обучения, своя
вселенная) — для бизнес-CRUD не брать; точечно — `swift-dependencies` 1.17.1 и
`swift-snapshot-testing` 1.19.5. Данные — `URLSession` + `Codable` + кеш; ORM не нужна; GRDB
7.11.1, если реально нужен SQLite; SwiftData — боли миграций/CloudKit.

Модульность: **тонкий app-таргет + локальные SwiftPM-пакеты** (`Packages/Core`:
Domain/Routing/Networking/Persistence/DesignSystem/AppFoundation; `Packages/Features`;
`TestSupport`), **без Tuist/XcodeGen** — Xcode 16+ buildable folders сняли боль `.pbxproj`;
fallback — XcodeGen 2.46.0 (не Tuist: его ценность — платное облако). Тесты: **Swift Testing**
по умолчанию (в 6.4 — управляемый interop с XCTest), XCTest — UI/performance; тест-планы
Unit/Snapshot/UI; снапшоты привязаны к фиксированному симулятору (`.test-simulator`),
`make snapshot-record` — осознанное действие. Линт/формат: `swiftlint --strict` (0.65.1, есть
Linux-бинарь) + `swift format lint --strict` (в тулчейне); SwiftFormat (nicklockwood) — не
брать (третий инструмент). **Periphery архивирован 12.08.2026 и коммерциализирован** (последний
OSS 3.8.0 MIT). Архитектурного линтера уровня Steiger в Swift нет — свой `Tools/archlint`
(~200 строк) поверх `Package.swift` (эталон — JET `ModularArchitectureValidator`).

**Главный архитектурный драйвер — делить код на pure-Swift и UI/Apple-SDK**: pure-Swift
(домен, сеть, роутинг, модели экранов) собирается и тестируется `swift test` **на Linux**; всё
с SwiftUI/UIKit — только Mac. Гейт: `make check-fast` (Linux: fmt-check + lint + archlint +
test + cover-check) и `make check` (macOS: + build + Unit + Snapshot); pre-push — `check-fast`
везде, `check` только на macOS-хосте — «гейт не изображает зелёный».

Тонкости платформы (30 ловушек с URL в отчёте): подпись/provisioning/`fastlane match`; App
Review — 4.2 (minimum functionality), **5.1.1(v) удаление аккаунта обязательно**, 2.1 (login-only
без demo-режима → реджект); **privacy manifests** и required-reason API (`UserDefaults` — тоже);
Keychain (`AfterFirstUnlockThisDeviceOnly`, `Synchronizable=false`; переживает удаление
приложения); AASA через CDN Apple (до 24 ч; `?mode=developer`); APNs token-based `.p8` (≤ 2
ключей на среду); TestFlight 90 дней; Swift Testing параллелит по умолчанию; снапшоты только с
того же симулятора; `xcresulttool` legacy; `//` в `.xcconfig` — комментарий; Xcode 27 source
break SE-0508, `ld64` удалён, ODR deprecated, HW-checked pointer arithmetic + automatic signing
→ нельзя загрузить в App Store на macOS 26.6.

### 2.3 Что общего у обеих платформ (потоки 01, 02, 04)

Фундамент, который держится 10+ лет и совпадает у Google, Apple и живых open-source
проектов: unidirectional data flow + immutable state; single source of truth / repository;
конструкторная DI; модульность с машинной проверкой границ; фейки вместо моков (Google:
«Prefer fakes to mocks — strongly recommended»); тесты без устройства + скриншоты/снапшоты как
«глаза»; feature flags как инвентарь; offline-кеш; a11y/i18n с первого экрана; crash/perf как
гейт релиза; типизированная навигация и типизированный контракт API; **логика вне UI и вне
фреймворка** (JVM / pure-Swift тесты на Linux). Маятник «паттерн года» стоит в положении
«официальный канон вендора» — редкий момент, когда «как вендор» = «фундаментально».

---

## 3. Эмпирика: что реально делает агент на этой Linux-машине (поток 06, `06-tooling-agent.md`)

Единственный эмпирический поток: поставил тулчейны в `~/.cache/stit-mobile-research/` и собрал
два spike'а — `spikes/android-gate/` (7-модульный Android-проект с полным гейтом + KMP-модуль
`:shared`) и `spikes/swift-linux/` (SwiftPM-пакет с Swift Testing и OpenAPI-клиентом).
Исходники сохранены (сборочные выходы — в `.gitignore`).

| что | результат |
|---|---|
| Android-гейт на Linux без эмулятора и Android Studio | ✅ **целиком**: Spotless, detekt + compose-rules, Android Lint (`warningsAsErrors`), JVM-тесты, Robolectric Compose UI-тесты, **Roborazzi screenshot-тесты** (PNG за ~2,5 с/тест; агент читает кириллицу и зачёркнутый текст на скриншоте), Konsist, Kover-gate, `assembleDebug`, подписанный `assembleRelease` (R8), `bundleRelease` |
| `make check` (concise-обёртка `OK:/FAIL:`) | тёплый без изменений **4,2 с** (9 строк вывода) · правка одного файла **37 с** (12 строк) · свежий клон при тёплом `~/.gradle` **94 с** · + подписанный релиз +25 с |
| Цена входа | ~9,5 ГБ диска, ~8 мин скачиваний: Android SDK 806 МБ, JDK 346 МБ, `~/.gradle` 4,0 ГБ, `~/.konan` 822 МБ, Swift 3,6 ГБ, SwiftLint 245 МБ |
| Память | **узкое место**: Gradle-демон 3,6 ГБ RSS + Kotlin-демон 1,2 ГБ = 4,8 ГБ при `-Xmx2g`; Android Lint падает с `OutOfMemoryError: Metaspace` при 512m → `MaxMetaspaceSize ≥ 1g`; требование шаблона — ≥ 6 ГБ свободной RAM (с эмулятором 10+) |
| KMP на Linux | `:shared:jvmTest` ✅ (секунды); **`compileKotlinIosArm64` / `IosSimulatorArm64` собирают klib ✅** (66 с с K/N prebuilt); **`linkDebugFrameworkIosArm64` — SKIPPED с exit 0 без предупреждения** (ловушка: гейт зелёный при несобранном framework → в Linux-гейт только `compileKotlinIos*`, линковка/`iosTest` — Mac, проверять наличие артефакта); `iosSimulatorArm64Test` отключён с внятным сообщением; `commonTest` на новом `androidLibrary {}` не запускается без `withHostTest {}` |
| Swift 6.4 на Debian 13 | ✅ официальный тарболл, без `apt`, `ldd` чист; `swift build` 4 с, `swift test` (Swift Testing) **0,72 с** тёплый, `swift-format` 28 мс, SwiftLint 0.65.1 (нативный Linux-бинарь) 180 мс, coverage через `llvm-cov` |
| Swift OpenAPI Generator 1.13.1 на Linux | ✅ (911 строк из OpenAPI 3.1 в стиле huma; сам ставит `#if os(Linux)`), но **полностью игнорирует `securitySchemes`** — auth только через `ClientMiddleware` |
| Что на Linux невозможно (проверено компилятором) | `import SwiftUI` / `import UIKit` → `no such module`; `URLSession` только через `FoundationNetworking`; симулятор, `xcodebuild test`, `xcresult`, подпись, App Store — только Mac |
| Конфликты форматтеров (воспроизведены) | `detekt-formatting 1.23.8` (ktlint 0.50.0) vs Spotless (ktlint 1.8.0) переписывают код друг за другом; SwiftLint `trailing_comma` запрещает висящую запятую, `swift-format` `TrailingComma` требует → **один автофиксер на язык** |
| AGP 9 и convention-плагины NiA | `CommonExtension` больше не генерик, `defaultConfig`/`compileOptions`/`testOptions`/`lint` скрыты из `CommonExtension` → конфигурировать `ApplicationExtension`/`LibraryExtension` напрямую; `compileSdk = 37` + `compileSdkMinor = 2` |
| Google agent-first CLI | cmdline-tools 23.0 содержит `android` (v1.0.16261425): `docs search/fetch`, `skills`, `create` (AGP 9 + Nav3), `layout`, `screen capture --annotate`, `emulator`, `describe` |
| xtool (iOS-сборка на Linux) | по докам: требует Xcode.xip (серая зона EULA), без симулятора и App Store-подачи — не брать; не пробовался |

Итог по циклу «правка → проверка»: **Android — полный цикл на Linux** (сборка, тесты, UI-тесты,
скриншоты, линтеры, подписанный артефакт); **iOS — частичный** (домен/данные/сеть/тесты/линтеры
на Linux; всё UI-, симуляторное и подписное — удалённый Mac по SSH, `make check-mac` с
fail-fast). Это подтверждает архитектурный принцип обоих канонов (§2.3): максимум логики вне UI.

---

## 4. Развилка «сколько кодовых баз и на чём» (поток 03, `03-shared-strategy.md`, 1616 строк)

Матрица по критериям брифа (оценки 1–5, итог /100):

| кандидат | нативное ·20 | зрелость ·20 | agent ·20 | общий код ·15 | тесты ·15 | экосистема ·10 | **итог** |
|---|---|---|---|---|---|---|---|
| **B. KMP shared logic + нативный UI** | 5 | 4 | 4 | 3 | 5 | 5 | **86,0** |
| C. Compose Multiplatform (общий UI) | 3 | 3 | 5 | 5 | 4 | 5 | 81,0 |
| A. Нативно ×2 | 5 | 5 | 3 | 1 | 4 | 4 | 75,0 |
| E-Rust. Rust-ядро + нативный UI | 5 | 3 | 3 | 2 | 3 | 3 | 65,0 |
| D-2. Swift Android SDK (без Skip) | 5 | 2 | 3 | 3 | 3 | 2 | 62,0 |
| D-1. Skip | 4 | 2 | 1 | 4 | 3 | 3 | 55,0 |
| E-Go. gomobile | 5 | 1 | 3 | 2 | 2 | 3 | 54,0 |

B устойчив по всей области весов; C выигрывает только при одновременном обесценивании
«нативного качества» и переоценке «доли общего кода»; сценарий «тезис Shopify целиком» (вес
общего кода ↓) даёт B 88,9 · A 81,1 · C 78,9 — B увеличивает отрыв.

Факты, определившие решение (проверено 2026-09-17):

- **Kotlin/Native официально собирает `.klib` для Apple-таргетов на Linux-хосте** (без
  cinterop), но не финальные бинарники — подтверждено эмпирически потоком 06 (§3).
- **Google поддерживает KMP tier-1** (Android + iOS + JVM, полное CI-покрытие) для annotation,
  collection, datastore, lifecycle, viewmodel, savedstate, sqlite, room, paging — артефакты
  `*-iosarm64` есть в Google Maven. **JetBrains с мая 2026 сменил дефолтную структуру KMP** на
  `shared/` + `androidApp/` + `iosApp/` (AGP 9 запрещает application-плагин в multiplatform-модуле).
- **Skip дисквалифицирован**: «requires a macOS 15+ development machine»; Skip Fuse +≈60 МБ к
  Android-бандлу. Swift Android SDK официален с Swift 6.3, но UI-фреймворка нет → только логика.
  Swift export — Alpha (roadmap: → Beta); до Beta граница Kotlin↔Swift — Obj-C export + SKIE
  0.10.14 или плоский DTO-фасад. Amper архивирован → Kotlin Toolchain (Alpha). gomobile — без тега,
  experimental, биндинги Obj-C. Rust-ядро работает (Element X, Proton), но третий язык и без
  выгоды для CRM.
- **CMP на iOS stable** (1.12.0), но системный look iOS 26/27 (Liquid Glass) достижим только через
  нативную SwiftUI-оболочку (официальный туториал JetBrains) — CMP не освобождает от Swift.
- **Разворот Shopify (2026-09-10)** — уход с React Native на Swift + Kotlin «потому что агенты
  удешевили писать дважды»; их центральный принцип «бизнес-логика отделена от UI и гоняется
  headless» — это описание KMP-модуля, который Shopify построил вручную (CLI + Tardis + Helix);
  их «до» — RN с рантаймом и мостом, не KMP. Противоположная позиция — Philo. Процессные находки
  Shopify (чекпойнт-цикл, хеш плана, agent-addressable CLI, adversarial-ревью) берутся независимо
  от выбора.
- **Найм в РФ (hh.ru, 2026-09-17)**: Android 333 · iOS 243 · Kotlin 323 · Swift 211 · Jetpack
  Compose 35 · SwiftUI 28 · Flutter 89 · RN 46 · **KMP 9** · CMP 5. Рынка KMP-специалистов нет, но
  KMP их не требует — нужны обычные Kotlin- и Swift-разработчики (против C, за B).
- **РФ-практика — именно схема B**: Avito (ядро BDUI на KMP, UI нативный «чтобы сохранить
  look&feel»), X5 Tech (KMP, 1 iOS : 3 Android, «ускорились вдвое»), CleverPumpkin/Kassir.ru
  (сетевой слой на KMP, XCFramework через SPM), Т-Банк, Яндекс Карты; публичного RU-кейса «весь UI
  на CMP» нет, единственный подробный RU-разбор CMP — отказ (поток 04).
- **Ответ на «80 % iOS без Mac»**: при B агент на Linux пишет **и верифицирует** 65–80 % кода
  (shared 45–60 % + Android 20–25 %); слепым остаётся тонкий SwiftUI-слой (20–30 %), проверяемый
  на удалённом Mac. Условие — «тонкий SwiftUI» как инвариант гейта, а не рекомендация.

Формы шаблона (03 §6.3): (1) два репо — две обвязки, асимметричный гейт, две карты в strategy;
(2) **один `mobile-template`** — одна обвязка с path-scoped правилами, один гейт «Linux-полный +
macOS-only стадии», одна карта в strategy, совпадает с дефолтом JetBrains; (3) три (shared-lib +
два app) — преждевременно: общего домена между заказчиками нет; уместно при разделённых командах;
(4) гибрид — миграционный путь для существующих приложений, не шаблон. Запасной вариант — **A,
не C**: откат B → A эволюционный («растворить» shared в две реализации), C → что угодно =
переписать весь UI. Следствие: `shared` — не единственное место правды, каждая фича описана в
`<org>/strategy`. Триггеры пересмотра R-1…R-9 — 03 §7.2. Окончательное решение о форме — §8.

---

## 5. Дистрибуция, релизы, РФ (поток 05, `05-distribution-ru.md`, 1665 строк; инженерная сводка, не юридическая консультация)

**Сторы.** RU-first Android = **RuStore (основной) + Google Play (только бесплатное)**:
монетизация Play для расчётного счёта в РФ выключена с 26.12.2024; RuStore — бесплатный аккаунт
(ИП/ЮЛ/самозанятый), APK или AAB ≤ 5 ГБ, модерация 1–5 рабочих дней, **официальный публикационный
API** (`public-api.rustore.ru`: RSA-SHA512 → JWE-токен 900 с → черновик → загрузка → модерация
→ раскатка) — **Android-ветка `/release` полностью скриптуется с Linux**. Google Play 2026:
`targetSdk ≥ 36` для любого обновления с 31.08.2026 → targetSdk в гейте; 16 KB page size; D-U-N-S
+ до 30 дней; для новых аккаунтов 12 тестеров × 14 дней; **Android developer verification**
(enforcement в 4 странах с 30.09.2026, глобально 2027; статус РФ противоречив — верифицироваться
заранее; RuStore — не participating store). **iOS: канал один — App Store; узкое место не
техническое, а платёжное** — Apple прекратила обработку платежей в РФ с 01.04.2026 → вопрос
юрлица/оплаты Developer Program блокирует iOS-ветку (решение владельца). 194-ФЗ о RuStore на
iPhone (в силе с 01.09.2025) Apple не исполнила; ФАС в августе 2026 — дело (~4 млрд ₽); RuStore
на iOS не существует. Наш класс приложений спасают исключения Apple 4.8 (свой логин ⇒ Sign in
with Apple не нужен) и 3.1.3(c) (B2B вне IAP). **Demo-режим — требование App Review** («active
demo account or fully-featured demo mode») → mock-режим становится reference-фичей №1.
Владелец RuStore сменился 2026-07-16 (VK продала 100 % менеджменту).

**Push.** Замер с этой RU-машины 2026-09-17: `mtalk.google.com:5228/5229/5230/443` открыты,
FCM жив; `googletagmanager.com`/`google-analytics.com` TLS не поднимают — фильтрация
селективная → **мульти-провайдер RuStore + FCM (Android) + APNs (iOS) с первого дня**. RuStore
скопировал форму FCM HTTP v1 → второй адаптер дёшев; relay `vkpns-universal` не использовать
(требует передавать VK ваши FCM/APNs-креды). ntfy из корп control-plane — не продуктовый push
(для iOS обязан транзитом через ntfy.sh); его место — уведомления команде. 41-ФЗ (с 01.06.2025)
запрещает банкам/госсектору/телекому/крупным компаниям иностранные мессенджеры для клиентов →
push в своём приложении = способ соответствия (драйвер спроса).

**Observability.** Поправка к вводным: OTLP/Grafana в `corp-infra-template` **нет**.
Рекомендация: `sentry-android` 8.57.0 / `sentry-cocoa` 9.28.0 с **пустым DSN по умолчанию**
(SDK выключен), self-host **GlitchTip** (полный Sentry — 16–32 ГБ RAM); Firebase не закладывать
(трансграничка ПДн, деградация Google-аналитики в РФ, privacy manifests, vendor-lock); аналитика
`none` по умолчанию (AppMetrica за флагом); OTel Android/Swift — шов, не дефолт.

**Право.** 152-ФЗ (переиспользован SSR-ресёрч) + мобильная специфика; чек-лист: 9 пунктов
автоматически в гейте (нет `AD_ID`/ATT, экран удаления аккаунта, политика доступна до логина,
плейсхолдеры оператора заменены, targetSdk актуален, privacy manifest…), 8 подтверждает человек
(уведомление РКН, Data safety, Nutrition Label, демо-аккаунт для review).

**Сборка и релиз без CI-раннеров и без Mac под рукой.** Android — всё на Linux
(`./gradlew check → bundleRelease` с подписью из env ← Vaultwarden → Gradle Play Publisher →
Play internal; RuStore по HTTP API; `sentry-cli upload-proguard`). iOS — **один Mac mini M4
(67–73 тыс. ₽ разово в РФ) как build-host, агент на Linux дирижирует по SSH/Tailscale**
(`xcodebuild test → archive → exportArchive → fastlane match` с сертификатами в приватном git
Gitea → `pilot` → TestFlight → `sentry-cli upload-dif`); Xcode Cloud (25 ч/мес) — резерв;
аренда за рубежом (Scaleway M4 €149–199/мес, MacStadium $149–349/мес, AWS EC2 Mac ≥ 24 ч) дороже
и требует платёжного канала; xtool — нет (лицензия Apple). **Rollback на мобильных не
существует** — откат = релиз с большим номером; `versionCode`/`CFBundleVersion` выводятся из
тега детерминированно (`X*1e6+Y*1e3+Z`), монотонность — в гейте; символы (`mapping.txt`/`.dSYM`)
заливаются **до** публикации.

**STOP-граница мобильного репо — двухступенчатая** (мобильный аналог «image-in-registry»):
STOP-1 «артефакт готов» — подписанные AAB+APK / IPA+dSYM.zip как ассеты **Gitea Release на теге
`vX.Y.Z`** + checksums + release-notes ru/en + символы залиты + annotated tag; STOP-2 «сдано в
каналы» — Play internal, RuStore на модерации, TestFlight загружен; дальше (production, ответы
модератору, Beta App Review, phased release) — человек по STORE-HANDOFF-блоку. `/release`
мобильного: `--platform android|ios|both`, частичный handoff, провал модерации — штатная ветка
(`Z+1`, не перевыпуск тега), тег — первый шаг.

Дефолты шаблона: крэши — Sentry SDK с пустым DSN; `ANALYTICS=none`; push мульти-провайдер; нет
IAP (B2B вне приложения); карты — шов `MapSurface` с `none`; соцвход выключен (тянет Sign in with
Apple); `AD_ID`/ATT не запрашивать; feature flags и min-version — **свой эндпоинт бэкенда**
(Unleash/Flagsmith — ещё контейнер + БД; для пяти флагов дороже поля в JSON); тест-дистрибуция
Play internal + TestFlight internal. Env-контракт релиза — три категории: build-time (в артефакте,
публичен), **release-time секреты** (Vaultwarden → env `make release`, в артефакт не попадают),
runtime-конфиг (эндпоинт бэкенда). «Один образ на все среды» невозможен — три flavor'а/схемы
(`.dev`/`.staging`/prod), `APP_ENV` на экране «О приложении», ни одного секрета в артефакте.

---

## 6. Контракт с Go-бэкендом и экосистемой (поток 07, `07-backend-contract.md`, 1648 строк; есть эмпирика)

**Auth.** Бэкенд сегодня cookie-only и заточен под браузер: `tokenFromRequest` читает только
cookie `access_token`; cookies — session без `Max-Age` (тест это фиксирует), `SameSite=Strict`;
refresh только из cookie; CSRF нет; reuse-detect не отзывает цепочку; Idempotency middleware не
включён (store не провайдится — ключ молча игнорируется). Для нативного клиента это =
самописный шифрованный persistent cookie-jar на каждой платформе (OkHttp `NO_COOKIES` по
умолчанию; Ktor без persistent storage, Darwin-движок отключает системное хранилище;
`HTTPCookieStorage` session-cookie пропадают при перезапуске) и **сломанный кодоген** (`in:
cookie` не поддерживают ни Swift OpenAPI Generator, ни openapi-generator kotlin).
**Рекомендация — Bearer + refresh в JSON** (`Authorization: Bearer`, `POST /api/auth/refresh
{refresh_token}`, `X-Client: android|ios`, сессии `GET/DELETE /api/auth/sessions`); cookie для
web остаётся; BACKEND-HANDOFF B-1…B-11 — ~150 строк Go (`authgate/jwt.go`, `authgate/module.go`,
`auth/delivery/http/*`, `family_id` для отзыва цепочки, mobile refresh TTL 90 дней). Хранение:
`androidx.security:security-crypto` **целиком deprecated с 1.1.0** → своя `SecureStore`
(Keystore AES-GCM + DataStore, `setUserAuthenticationRequired` для биометрии); iOS — Keychain
`AfterFirstUnlockThisDeviceOnly`; access — только в памяти. OIDC/Authentik (AppAuth) и passkeys —
extension'ы; pinning выключен по умолчанию (шов + процедура; LE-ротация на Traefik).

**Спек и кодоген (эмпирика).** OpenAPI 3.1 добывается без БД: 30-строчный `cmd/openapidump`
(`httpkit.NewServer` + `Register(api, NewHandler(nil))` → `api.OpenAPI().YAML()`),
`openapi-generator validate` чист → **`make openapi` + закоммиченный `api/openapi.yaml` +
`gen-check` в бэкенде** (B-12); мобильный репо читает `../backend/api/openapi.yaml`, держит
`api/openapi.lock`. Дефекты спека: `$schema` в каждой body-схеме (huma `SchemaLinkTransformer`),
`data` списков `[array, null]`, `LoginOutputBody.user` вместо `data`, в `ErrorModel` нет `code`.
openapi-generator 7.25.0 `kotlin multiplatform` на реальном спеке: 3.1 «still in beta», Cookie
→ `ApiKeyAuth("query","access_token")` (токен в URL!), `refreshToken(refreshToken: Cookie?)`;
Swift OpenAPI Generator 1.13.1: 3.1 ✅, `securitySchemes`/cookie ✗. **Bearer + body делают спек
генерируемым на обеих платформах**: iOS — Swift OpenAPI Generator (плагин, types+client);
KMP/Android — генерация **моделей** + свой Ktor-клиент/Ktorfit 2.7.5.
**Побочная находка: SPA-шаблон и бэкенд несовместимы по контракту** (`/api/v1/*`, `app_session`,
`X-CSRF-Token` vs `/api/auth/*`, `access_token`) — F-1 в SPA.

**Конвенции для долгоживущих клиентов**: envelope `data`/`data+total`; `APIError.code` стабилен;
`X-Request-ID` на каждый запрос → breadcrumb Sentry ↔ логи бэкенда; `Idempotency-Key` на каждую
мутацию (httpkit: 24 ч, `422 IDEMPOTENCY_KEY_REUSED`, `409 IN_PROGRESS`); ретраи по
`Retry-After`/429/503; аддитивная эволюция без `/v1` + `ignoreUnknownKeys` + `oasdiff breaking`
в гейте бэкенда + kill-switch **`GET /api/app/requirements`** (min-version, флаги на `flagkit`,
ссылки) — B-17.

**Mock-режим**: JSON-фикстуры per сценарий (`empty|populated|authenticated|error`, формат =
ответы API) + `MockTransport` (Ktor `MockEngine` в shared / swift-openapi `ClientTransport`),
`demo`-flavor/схема (`applicationIdSuffix ".demo"`), debug-панель, **те же фейки в JVM/Swift-тестах**.

**Deep links**: новый always-on сервис **`wellknown`** (nginx) в `<org>/infra` (`PathPrefix(
/.well-known/) && priority 1000`, `.tpl` через `render-configs.sh`, env
`MOBILE_ANDROID_SHA256_{UPLOAD,PLAY,RUSTORE}` — **все отпечатки подписи**, `MOBILE_IOS_TEAM_ID/
BUNDLE_ID`); AASA на каждом сабдомене, без редиректов; черновик compose/nginx/tpl + INFRA-HANDOFF.

**Push**: `PUT /api/v1/devices {provider, token, platform, app_version, locale, installation_id,
sandbox, push_enabled}` (upsert по (provider, token)), `DELETE` при logout, отзыв по
`UNREGISTERED`/`410`; payload data-first (ключи локализации + args + `deep_link` +
`notification_id` + `category`), лимит 4096 Б; `golibinfra/pushkit` (FCM v1 / APNs ES256 /
RuStore) + outbox; reference-домены `device`/`notification` в бэкенде.

**Observability**: Sentry SDK с пустым DSN, release `package@version+build`, mapping через
Gradle-плагин на Linux, dSYM только с Mac (`sentry-cli`, лицензия FSL-1.1); логи Kermit /
`os.Logger` с запретом `Log`/`print`; `X-Request-ID` в breadcrumbs; OTel — шов.
**i18n/токены**: нативные ресурсы с единым реестром ключей; ошибки и пуши — по ключу; токены
независимые с DTCG-именами + проверка паритета имён с `index.css` SPA; Style Dictionary — позже
в `ui-lib`.

**Слой-мост в strategy для `type: mobile`**: FSD как есть не годится (нет `pages` как слоя —
адресация через NavKey/Route + deep link; `flow` = viewport веба; `block` на `@stit/ui`; нет
permissions/deep-link/push/offline/background) — переносить принципы, не имена. Предложен слой
**`mobile/<surface>/`** на место `screens/` для `type: mobile` (как bot-flow для ботов): 9 kind
— `entities` · `screens` (route-key, guard, states, offline, entry) · `flows` · `features`
(feature → feature запрещён; `shared: true` → KMP) · `shell` · `entry-points` (deep-link / push /
shortcut / share-in — вход для `deep-links.md`/`push.md`) · `permissions` (вход в Data safety /
Nutrition Label / `compliance.md`) · `shared-api` · `shared-lib`; inside-out порядок; инварианты
для `/mobile-audit`; таблица «что генерирует `/scaffold`». Канон-PR в strategy (~1200 строк md +
скиллы `/mobile-deep-dive`, `/mobile-audit`, углы `/cross-audit`) и **путь без PR** (копия канона в
мобильном репо, next-step из `/init-service`). Финальная форма — §8.

---

## 7. Индустрия 2025–2026 (поток 04, `04-industry.md`, 1349 строк)

- **Три вендора сошлись**: декларативный UI как единственный стандарт («Compose is our
  standard… Views in maintenance mode» — I/O 2026; SwiftUI + Liquid Glass обязателен с iOS 27
  SDK); агент — штатный участник цикла (Android CLI 1.0 + Skills; Xcode 26.3/27 с Claude Agent
  SDK/Codex/Gemini; MCP и Agent Client Protocol у Apple и JetBrains одновременно);
  типизированная навигация и UDF — общий знаменатель. **Ни один вендор не продвигает общий UI
  между платформами**; Google выпустил Migration Assistant *в* native.
- **Индустрия качнулась к нативу, «общая логика» стала нормой**: Shopify (2026-09-10) ушёл с RN;
  Proton (02.2026) — Rust-ядро ≈ 80 % + SwiftUI/Compose (crash-rate iOS 0,05 %); Netflix,
  Duolingo, McDonald's, Cash App, Wise, Monzo — KMP-логика; в РФ — Avito, Яндекс Карты, X5,
  Т-Банк. **РФ-специфика — BDUI** (Avito Beduin v2, Яндекс DivKit, Ozon Composer, Альфа) как обход
  релизного цикла с признанной ценой → для B2B-компаньона опция-шов, не база. Flutter в РФ — ниши.
- Опросы: SO 2025 Kotlin 10,8 % vs Swift 5,4 %; KMP в DevEco 7 % → 18 % за год; AI используют
  84–90 %, **«highly trust» 3,1 %**; DORA 2025: throughput ↑, stability ↓; Uber: > 70 % PR от
  агентов. Хабр Карьера I п/г 2026: медианы iOS 326 тыс. ₽, Android 320 тыс. ₽; **в РФ iOS ≈ 34 %
  мобильного трафика при ≈ 22–25 % парка → iOS-версия компаньона не «вторая»**.
- **Пороги качества нормативны**: Play — crash 1,09 % / ANR 0,47 % (28 дней); с 02.2027 бюджеты
  памяти и **DEX-оптимизация ≥ 25 %**; с 04.2027 **Zero-Tap Sign-In restoration** обязателен для
  приложений с логином; Xcode 27 Organizer — Metric Goals. Их место — гейт `/release`.
- `AGENTS.md`/`CLAUDE.md` есть у продовых репо (NiA, Element X, DuckDuckGo, Firefox, Wire iOS,
  Tuist 27 КБ, Telegram-iOS 24 КБ), нет — у всех community-шаблонов; шаблоны индустрии
  останавливаются на «проект собирается» (Google `architecture-templates` стагнирует с 2024-12,
  `compose-multiplatform-template` архивирован). **Стартовая точка кода: `KMP-App-Template-Native`
  + `android create empty-activity-agp-9` + Xcode «SwiftUI App»** + spike 06; остальное — из
  Go/SPA-шаблонов. Таблица «зашить / опция / избегать» — 04 §7.1; 20 инвариантов для
  `CLAUDE.md`/rules — 04 §7.2.

---

## 8. Форма шаблона и место в экосистеме (поток 08, `08-template-design.md`, 1540 строк)

### 8.1 Сколько шаблонов — решение и публичные формулировки

Четыре формы (два нативных репо · один KMP-репо · три: lib + два app · четыре: два нативных +
два kit'а) сравнены по обвязке/дрейфу, Linux-выполнимости гейта, STOP-контракту, мосту в
strategy, форк-пути потребителя, найму и передаче заказчику, публичному обещанию, стоимости
поддержки владельцем-соло, откатываемости и слепой зоне без Mac (08 §1.1). Принят **один репо
`mobile-template`** (§1 п. 1–2). Критерий отдельного репозитория (адаптация SSR-синтеза): другой
toolchain/runtime, другой состав `make check`, другой тип артефакта STOP-1, другой слой strategy
— ни один из пресетов его не пересекает; пересекают и остаются **вне шаблона** server-driven
UI/BDUI-рантайм (только шов), ОС «Аврора», десктоп, Compose-Multiplatform-UI (триггер, не пресет).

| Пресет | Для кого | Что меняет |
|---|---|---|
| `companion` (дефолт) | B2B/B2E-клиент к своему бэкенду: вход по логину, без монетизации, аналитика `none`; RuStore + Play (free) + App Store по исключениям 3.1.3(c)/4.8 | ничего сверх базы; `demo`-режим обязателен |
| `consumer` | B2C: публичная регистрация, монетизация, аналитика | kinds `analytics`/`payments` обязательны; расширенный compliance (рейтинг, SiwA при стороннем логине, ATT); store-listing-блок в `/release` |
| `enterprise` | распространение вне магазинов: Gitea Release APK + Apple Business Manager/MDM | каналы `gitea-release` + `abm`; App Review-блоки выключены; `demo` необязателен |

Публичные строки (08 §1.5): `stit-public/docs/roadmap.md` — две строки заменяются на
`| mobile-template | мобильное приложение: нативный Android (Compose) + нативный iOS (SwiftUI) на общем Kotlin-ядре (KMP) | в планах |`;
`docs/templates.md` — `| mobile-template | Android + iOS, KMP-ядро, нативный UI; форк-пути Android-only / iOS-only |`;
`README.md` — «мобильные приложения (Android + iOS, KMP-ядро, нативный UI)»; корп-канон
`corp-infra-template/docs/ecosystem.md` — 7-й шаблон (после SSR — 8-й), строка Identity
`| mobile-template | <org>/mobile | Android + iOS на KMP-ядре; STOP = артефакт в Gitea Release |`,
контракт «Artifact-in-release (mobile)», Vaultwarden-коллекция `mobile/<org>/<app>`, ntfy-топик
`releases-<org>`, sibling-checkout `+ mobile`, узел `mobile` в диаграмме как клиент `backend`
(Bearer) без маршрута Traefik.

### 8.2 Форма репозитория

Раскладка JetBrains (дефолт с мая 2026) `shared/ + androidApp/ + iosApp/`; внутри `androidApp/`
— NiA-модульность (`app`, `core/{designsystem,ui,platform,navigation,testing}`,
`feature/{shell,auth,profile,todos,settings}`, `lint`); внутри `iosApp/` — один `.xcodeproj` с
buildable folders + SwiftPM-пакеты `Packages/{Pure,Core,Features,TestSupport}` (`Pure` —
Linux-собираемые `AppFoundation`, `DesignSystemTokens`; `Core` — `DesignSystem`, `Routing`,
`Persistence`, `Platform`, `SharedBridge`; пакеты `Networking`/`Domain` не создаются — их роль у
`Shared.framework`). Корень: `Makefile` (словарь вербов), `build-logic/` (convention-плагины
`stit.*`, 11 штук), `gradle/libs.versions.toml` (единый каталог, включая SDK-уровни),
`config/{detekt,lint,env,openapi}`, `api/{openapi.yaml,openapi.lock}`, `mock/{scenarios.yaml,
fixtures/}` (одни фикстуры для `commonTest`, demo, Roborazzi/snapshot, Maestro), `e2e/`
(Maestro, вне гейта), `scripts/{tools,release,git-hooks}`, `.gate-fixtures/` (негативные
фикстуры), `docs/`, `.claude/`, `AGENTS.md`, `.xcode-version` (27.0), `.swift-version` (6.4).
Полное дерево — 08 §2.1; что в дереве **нет** (осознанно, с указанием шва) — там же.

Что где: `shared/src/commonMain/kotlin/<pkg>/{core,entity,feature}` — `core/**` не импортирует
`entity`/`feature`; `entity/**` не импортирует `feature`; `feature/<a>` не импортирует
`feature/<b>`. Android: `feature:*` не зависит от других фич (Gradle-граф физически), `core:*`
без `feature`, `app` — composition root. iOS: `Features/<X>` ↛ `Features/<Y>`, ↛
`Core/{Persistence,Platform}` (реализации только из `App`). Enforcement — §1 п. 5 и 08 §2.6.

DI/persistence/сеть/auth — §1 п. 5–6 и 08 §2.4–2.5; persistence v1 — in-memory кеш +
`FileCache<T>` (okio, JSON) + `Settings` + `SecureStore`, БД нет (Room KMP / SQLDelight тянут
cinterop, который ломает Linux-компиляцию iOS-klib; шов `entity/<slug>/cache`).

**Reference-фичи** (зеркальны SPA-шаблону, prune-лист для `/de-template` — 08 §2.7): `demo`
(mock-режим: `MockTransport` + фикстуры + флаг для App Review) · `shell` (root-навигация,
auth-gate, offline/update/maintenance-оверлеи) · `requirements` (min-version/kill-switch/флаги) ·
`auth` (Bearer-логин, `SecureStore`, биометрия как разблокировка ключа, **удаление аккаунта** —
5.1.1(v)) · `profile` · `todos` (CRUD, состояния, deep link) · `settings` (тема + язык) · `push`
(регистрация `PUT /api/v1/devices`, категории). Форк-пути: `--platform android` удаляет `iosApp/`
и iOS-таргеты `shared/`; `--platform ios` сохраняет `shared/` (Gradle остаётся iOS-команде;
альтернатива при росте — прекомпилированный XCFramework в Gitea Release).

### 8.3 Агентная обвязка

`CLAUDE.md` (листинг ≈ 150 строк — 08 §3.1): «Где это в экосистеме», точка входа `/smartdev`,
«проектирование первично», команды `make`, карта «где что», **инварианты «что в 2026 иначе, чем
агент помнит»** (AGP 9 без `kotlin-android`/kapt, Nav3, Compose Test v2, Material 3 без
Expressive, Swift 6 MainActor-by-default, Liquid Glass, KMP klib на Linux и SKIPPED-ловушка),
границы «делать всегда / сначала спросить / никогда», forward-only.

Правила (23, все ≤ 60 строк, path-scoped через `paths:`; 08 §3.2): `forward-only`, `comments`
(идентичны/по форме экосистеме); `kmp-shared`, `kmp-public-api` (фасад для Swift: final-классы,
sealed, `suspend`/`StateFlow`, без generic'ов; `apiDump` коммитится), `android-ui`, `ios-ui`,
`swift-concurrency`, `navigation` (Route — sealed в shared; `DeepLinkParser` — чистая функция),
`design-tokens` (DTCG-имена, паритет платформ `tokens-check`), `gradle-build`, `xcode-project`
(`.pbxproj` руками не править), `tests-pipeline`, `api-contract`, `auth-and-session`,
`signing-and-secrets`, `env-and-config` (build-time / release-time / runtime), `store-compliance`,
`privacy-and-law`, `release-flow`, `observability`, `mock-and-fixtures`, `deep-links-and-push`,
`infra-impact` (три адресата handoff: infra / backend / strategy).

`settings.json` (08 §3.3, по 06 §8.5): allow — всё читающее/проверяющее + детерминированные
автофиксы; deny — `assembleRelease`/`bundleRelease`/`publish`/`keytool`, `android sdk install`/
`sdkmanager`, правка `build/`/`GeneratedSources/`/wrapper, `pkill`/`killall`, чтение
`*.jks/*.p12/*.mobileprovision`. Скиллы — §1 п. 10 и 08 §3.4 (там же `make status` с секциями
`Platforms`/`Mac`/`Critical`/`Handoffs`/`Strategy`, каталог `/triage-failure`, стадии
`/init-service`, варианты `/scaffold`: `entity`, `feature`, `screen`, `flow`, `entry-point`,
`permission`, `api`, `stack|rule|skill`).

### 8.4 Гейт

Состав `make check` (21 стадия, времена из 06; 08 §4.2), хвост вывода на Linux —
явный список SKIP-стадий (`shared link framework → make check-mac`, `iosSimulatorArm64Test`,
`xcodebuild build`, test plans Unit/Snapshot) + отметка «последний check-mac: <дата> (git <sha>)»
из `build/gate-logs/mac.last`; `/release` требует `check-full` на текущем HEAD. Оценка: тёплый без
изменений ≈ 10 с, один файл ≈ 40 с, свежий клон 2–3 мин (`[spike]` — 06 мерил без Swift/KMP/
contract-стадий: 4,2 / 37 / 94 с). Pre-commit — Spotless/swift-format по изменённым + detekt;
pre-push — `make check` (+ `GATE_RELEASE=1`). `make gate-selftest` — 24 негативных фикстуры
(NF-01…10 из 06 проверены красными + Swift/KMP). Требования к машине: ≥ 6 ГБ свободной RAM,
`MaxMetaspaceSize=1g`, ~10 ГБ диска под тулчейны и кеши.

### 8.5 `docs/`, kinds, слой-мост в strategy

`docs/`: `README.md`, `getting-started.md` ([META], **Mac build-host — предусловие iOS-ветки**),
`contributing.md`, `reference/{architecture,ecosystem,release,observability,distribution,
strategy-mapping,mac-host,kinship}.md`, `reference/strategy-mobile-canon/` (путь без PR),
`stack/README.md` + 15 `_templates`. Kinship: идентичны `forward-only`/`comments`/`/plan`/
`settings.json`-deny/commitlint; по форме — `CLAUDE.md`, скиллы, `docs/stack`; не переезжает —
nginx/`config.js`, MSW в Service Worker, `routeTree.gen.ts`, React Compiler; сверка — `kin-diff`
(advisory). Kinds — §1 п. 12 (таблица с `Generates` и волнами — 08 §5.2; cross-kind инварианты:
`auth.mode=cookie` ⇒ BACKEND-HANDOFF, отпечатки `signing` ↔ `deep-links`, `preset` ⇒ условные kinds).

Слой `mobile/<surface>/` — §1 п. 11; формы 9 kinds с колонками, инвариантами и «куда
разворачивает `/scaffold`» — 08 §6.2; куда легли поля 05 §9.4 (магазины/min OS → stack
`distribution`/`runtime`; permissions rationale → strategy, декларация → stack `compliance`;
offline → `shell.md#Offline` + `entities.md#cache`; demo-сценарий → `entry-points.md`/`flows.md`)
— 08 §6.3. Правило разграничения: **strategy описывает продукт и его карту; stack — платформу
и её значения**.

### 8.6 STOP-контракт и релиз

Принят 05 §7.4 как есть (§1 п. 8); `dist/` из 03 — локальная стадия; Baseline Profile из 01 —
внутри AAB (в v1 не генерируется). Gitea Release: `FILE_MAX_SIZE` 2048 МБ достаточно,
`MAX_FILES` 5 на загрузку → ассеты по одному через API или `MAX_FILES=20` в corp `app.ini` (C-4).
Версия из тега: `versionName`=`CFBundleShortVersionString`=`X.Y.Z`, `versionCode`=
`CFBundleVersion`=`X·1e6+Y·1e3+Z`; `release-preflight` проверяет монотонность против тегов и
каналов, чистое дерево, `main`, `check-full` на HEAD; Sentry release
`<applicationId>@X.Y.Z+<code>` общий для платформ. Custody — Vaultwarden `mobile/<org>/<app>`
(upload-key ≠ app-signing-key, оба отпечатка в `assetlinks.json`; Apple certs/profiles/APNs `.p8`;
RuStore ключи), `fastlane match` — приватный git в Gitea. `/release` — форма Go-шаблона:
preflight → тег → сборка (Android на Linux; iOS на Mac по SSH) → символы → Gitea Release
(STOP-1) → каналы (STOP-2) → STORE-HANDOFF (WHAT/WHY/ACTION/WHO/DECIDE/RISK) → ntfy.

---

## 9. Cross-repo изменения (сводно, порядок обязателен; полный список — 08 §8.1)

| № | репо | что | приоритет | блокирует |
|---|---|---|---|---|
| C-1 | `corp-infra-template/docs/ecosystem.md` | 7-й шаблон (после SSR — 8), строка Identity, диаграмма, sibling `+ mobile`, happy path, Skill index | P0 | нет (канон первым) |
| C-2 | там же § Контракты | **Artifact-in-release (mobile)**: ассеты Gitea Release на теге `vX.Y.Z`; STOP-2 — подача в каналы | P0 | нет |
| C-3 | corp + org | Vaultwarden-коллекция `mobile/<org>/<app>`, ntfy-топик `releases-<org>` | P0 | **да** — без custody нет подписи |
| C-4 | corp `compose/gitea` | `[repository.release] MAX_FILES = 20`; body-limit Traefik → Gitea; scope токена `write:repository` | P1 | нет |
| C-5 | corp (GlitchTip/Sentry) | проекты `<app>-android`/`<app>-ios`, DSN per env, токен для символов | P1 | нет |
| C-6 | corp docs | Mac build-host как ресурс org (`MAC_HOST`, Tailscale); Xcode Cloud — резерв | P2 | нет |
| B-1…B-3 | `go-backend-template` | Bearer-выдача, `POST /api/auth/refresh {refresh_token}`, `Authorization: Bearer` в middleware (cookie для SPA остаётся), `X-Client` | P0 | нет для шаблона (fallback `AuthMode.Cookie`); **да** для проектов с внешними пользователями |
| B-12 | `go-backend-template` | `make openapi` + закоммиченный `api/openapi.yaml` + `gen-check` | P0 | нет (шаблон везёт reference-спеку) |
| B-17 | `go-backend-template` | `GET /api/app/requirements` (min-version, maintenance, флаги) — домен `app` | P0 | нет (404 ⇒ «требований нет») |
| B-13…B-15 | спека бэкенда | `$schema`, конверт/nullable-списки, `ErrorModel.code` | P1 | нет (пост-процессинг в `gen.sh`) |
| B-18 + G-1 | backend + `golibinfra/pushkit` | `PUT /api/v1/devices`, мульти-провайдерная отправка (RuStore/FCM/APNs), data-first payload | P1 | нет |
| B-4…B-11, B-16, B-19 | backend | сессии, отзыв цепочки, `IdempotencyStore`, `Retry-After`, `auth.md` два режима, тесты, `oasdiff breaking` | P1–P2 | нет |
| G-2…G-5 | golibinfra | `ErrorModel.code`, idempotency middleware, `X-Client` в логах, `Retry-After` helper | P1–P3 | нет |
| I-1 | `project-infra-template-compose` | сервис **`wellknown`** (nginx): `assetlinks.json` (все отпечатки) + `apple-app-site-association` из env `MOBILE_*`, Traefik `PathPrefix(/.well-known/) priority 1000` | P0 | нет для STOP-1; **да** для App/Universal Links в prod |
| I-6 | инфра | публичные URL политики конфиденциальности и страницы удаления аккаунта | P0 | **да** для STOP-2 (Play/App Store) |
| I-3…I-5 | инфра `.env.example` | `MOBILE_*`, `APP_MIN_VERSION_*`, push-креды провайдеров, DSN мобильных | P1–P2 | нет |
| S-1…S-3 | `business-strategy-template` | канон слоя `mobile/` (README + 9 форм), скиллы `/mobile-deep-dive`/`/mobile-audit` + ракурсы `/cross-audit`, формулировки в `surface.md`/`fsd/README.md`/`screens/README.md`/`docs/README.md`/`CLAUDE.md` | P1 | нет (путь без PR) |
| F-1 | `spa-frontend-template` | привести reference-мок и `client.ts` к контракту бэкенда (`/api/auth/*`, cookies `access_token`/`refresh_token`, без `X-CSRF-Token`); `ecosystem.md` sweep | P2 | нет |
| T-1 | telegram-bot / go-backend / infra / strategy | sweep «6 → 7 (8)» — одним PR на репо вместе с SSR | P2 | нет |
| P-1 | `stit-public` | roadmap/templates/README — строки §8.1 (последним) | P1 | нет |

Что мобильный репо **не** требует от экосистемы (08 §8.3): CI-раннеров, Traefik-маршрута,
Docker-образа, CORS-расширения под приложение.

---

## 10. Открытые вопросы владельцу (с рекомендациями; полная таблица из 44 — 08 §10)

**Блокирующие (5):**

| № | вопрос | рекомендация | блокирует |
|---|---|---|---|
| 1 | Один репо `mobile-template` (KMP-ядро + нативный UI) вместо обещанных `android-template` + `ios-template`? | **да**; roadmap-строки — §8.1 | фазу 0 — всё остальное |
| 7 | Mac mini M4 как build-host экосистемы (67–73 тыс. ₽): купить сейчас или после Android-релиза? | после фазы 4 (Android-релиз доказывает шаблон); до этого iOS помечен «не верифицирован» | фазу 5 и iOS-часть релиза |
| 8 | Apple Developer Program без платежей из РФ: иностранное юрлицо / партнёр / отложить? | решить до фазы 5; шаблон от ответа не зависит | iOS-канал (TestFlight/App Store/ABM) |
| 30 | Владелец store-аккаунтов — заказчик (stit как admin) или stit? | заказчик (open-core «заказчик владеет»); для demo-приложения шаблона — stit | STOP-2 реального проекта |
| 35 | Правовые тексты (политика ПДн, страница удаления аккаунта, согласия): кто пишет и хостит? | заказчик/юрист; шаблон везёт плейсхолдеры + INFRA-HANDOFF I-6 | STOP-2 |

**Остальные (по областям; «да» ничего не меняет — отчёты уже написаны по рекомендации):**

- *Форма и экосистема*: имя форка `<org>/mobile` (да); пресеты `companion|consumer|enterprise` (да);
  kit-библиотеки — по триггеру (да); CMP `sharedUi/` — только по триггеру 03 §7.2 (да); sweep числа
  шаблонов одним PR с SSR (да).
- *Mac и Apple*: Xcode Cloud — документировать как резерв (да); политика версий — latest stable с
  пином и двухнедельной выдержкой, alpha/beta никогда (detekt 2.0-alpha, Swift export Alpha).
- *Стек*: `minSdk 26` / `targetSdk 37` / iOS 18 (да); Hilt в `androidApp/`, ручной `SharedContainer`
  в `shared/` (да; Metro — триггер); без БД в v1 (да; offline-first проект — спайк SQLDelight);
  полы 80/70/70 (да); `GATE_RELEASE=1` в pre-push и `/release` (да); Konsist с fallback на detekt
  (да); Roborazzi-эталоны — порог + «стандартное окружение» (да); Ktor вместо Retrofit, без Swift
  OpenAPI Generator (да); модель экрана в pure-Swift таргете — снимается, логика в презентерах
  `shared/`; без Tuist/XcodeGen, Material 3 1.4 без Expressive (да).
- *Контракт с бэкендом*: Bearer + refresh JSON как канон, cookie — переходный (да; P0 для
  реального проекта); спек — закоммиченная копия + lock (да); кодоген только моделей (да —
  аналог решения SPA «рукописные контракты»); pinning выключен (да); GlitchTip self-host, пустой
  DSN = выключено (да); `requirements` в домене `app` (да); `pushkit` в golibinfra (да); паритет
  токенов с SPA — при первом общем продукте web+mobile.
- *Дистрибуция и право*: RuStore первым, Play-аккаунт параллельно (D-U-N-S ~30 дней, developer
  verification до 2027); `ANALYTICS=none`, AppMetrica — шов под `consumer`; xtool исключить;
  порог crash-free для промоушена в production 99,5 % / 48 ч — поле `observability.md`;
  demo-режим для App Review — `MockTransport` по флагу в prod-сборке **и** demo-аккаунт на бэкенде.
- *Обвязка и процесс*: `AGENTS.md` — указатель (да); Android-скиллы через `make setup`,
  телеметрия выключена (да); заимствования процесса Shopify (чекпойнт-цикл, хеш плана,
  adversarial-ревью) — не сейчас, только атом «Mac-verify» в `/plan`; Maestro `e2e/` в шаблоне
  вне гейта; ничего платного в шаблоне; BDUI — не проектируется (YAGNI); «Аврора» — вне.
- *Strategy*: слой `mobile/` — 9 kinds включая `permissions` (да); канон-PR — после dry-run
  фазы 6, до фазы 7.

---

## 11. Главные риски

| ID | риск | вероятность / удар | митигация | сигнал |
|---|---|---|---|---|
| RS-1 | Kotlin/Native перестаёт компилировать Apple-klib на Linux — рушится «65–80 % на Linux» | низкая / критический | fallback A через `subtree split`; `make kmp` ловит первым; версии K/N пинятся | два подряд релиза Kotlin с падением `compileKotlinIosArm64` |
| RS-2 | Слепая зона Swift до появления Mac: `Core`/`Features` пишутся без компиляции | высокая до фазы 5 / средний | `Pure` + SwiftLint + archlint на Linux; логика в `shared/`; README «iOS не верифицирован»; `--ios-unverified` как явный долг | появление Mac → `check-mac` по всему |
| RS-3 | iOS-канал недоступен из РФ (оплата Apple Developer Program) | средняя / высокий для iOS | Android-first; STOP-1 для iOS достижим без магазина; путь через партнёра в `distribution.md` | решение № 8 |
| RS-4 | Play developer verification 2027 + санкционный статус; RuStore — единственный гарантированный Android-канал | средняя / средний | RuStore primary; Play internal второй; дедлайны в `distribution.md#deadlines`, preflight печатает ближайшие | ежеквартальная сверка |
| RS-5 | Ограничения Obj-C-экспорта/SKIE просачиваются в фасад | средняя / средний | `kmp-public-api` + `apiCheck` + Konsist; спайк S-1 до кода фич; Swift export — триггер при Beta | `apiDump` diff |
| RS-6 | Рост времени гейта (два toolchain'а + contract-стадии) — агент перестаёт запускать `make check` | средняя / высокий | `check-fast`, configuration cache, дешёвое первым; бюджет тёплого прогона ≤ 60 с | тёплый `make check` > 90 с |
| RS-7 | RAM: демоны ≈ 4,8 ГБ на общей машине агентов, эмулятор +4 ГБ | высокая / средний | `doctor.sh`, `make clean`, `workers.max=4`, эмулятор вне гейта | `OutOfMemoryError: Metaspace`, swap |
| RS-8 | Качество кодогена моделей (дефекты спеки, cookie → query) | высокая до фиксов бэкенда / низкий | models-only + пост-процессинг; клиенты руками; `FixturesTest`; fallback — рукописные DTO | `gen-check` FAIL |
| RS-9 | Недетерминизм Roborazzi/snapshot между машинами | средняя / низкий | пин toolchain, порог различий, «эталоны пишет стандартное окружение» | ложные красные скриншоты |
| RS-10 | 23 правила раздувают контекст и противоречат друг другу | средняя / средний | path-scoping, ≤ 60 строк, реестр в `contributing.md`, ревизия на dry-run | правило цитируется не по теме |
| RS-11 | Дрейф канона `mobile/` между копией в шаблоне и strategy | средняя / низкий | `/init-service` сравнивает и печатает diff; канон-PR закрывает | WARN в `/init-service` |
| RS-12 | Владелец-соло держит два toolchain'а с жёсткими дедлайнами (iOS 27 SDK — апрель 2027; Kotlin каждые полгода) | высокая / средний | политика обновлений в `runtime.md`, `deps-report`, пины в одном месте | отставание > 2 minor |
| RS-13 | Правила магазинов меняются без сигнала (`compliance.md` устаревает) | высокая / средний | датированные поля, preflight печатает дедлайны, чек-лист с датой проверки | отказ модерации |
| RS-14 | Firebase/FCM политически хрупок; RuStore меняет правила (монетизация самозанятым, BillingClient) | средняя / средний | мульти-провайдерный push как архитектура; не строить продукт на RuStore Pay; API-публикация устойчивее SDK | замер доступности, новости |
| RS-15 | `MockTransport` и фикстуры в prod-сборке (demo для App Review) | низкая / средний | синтетические фикстуры, тест «demo не ходит в сеть», ревью-заметки | сетевой вызов в demo |
| RS-16 | Стагнирующие/коммерциализирующиеся инструменты (Konsist без релизов с 2024-12, Periphery коммерческий, detekt 2.0 в alpha, ktlint сменил владельца) | средняя / низкий | минимум внешних бинарей, fallback-правила в detekt/Lint, пины | нет релизов > 12 мес |

---

## 12. План реализации (08 §9)

Старт: `KMP-App-Template-Native` (JetBrains) + `android create empty-activity` (AGP 9) + Xcode
«SwiftUI App» + `spikes/android-gate` (convention-плагины, `check.sh`, `gradle-step.sh`,
`doctor.sh`, `detekt.yml`, Konsist, Roborazzi, Hilt — прогнано). Размер — в агент-сессиях
(S ≈ 1–2, M ≈ 3–6, L ≈ 7+). Исполнитель — Fable по этому синтезу и отчётам.

| фаза | содержание | гейт (Done) | размер | без Mac |
|---|---|---|---|---|
| **0 · Решения и P0 cross-repo** | ответы на блокирующие вопросы §10; C-1/C-2/C-3; B-1…B-3, B-12, B-17 (или принятые fallback'и); I-1/I-6; выбор Mac-варианта | решения в `.claude/plans/roadmap.md`; corp-PR слит; backend-PR слиты или fallback зафиксирован | S + M | да |
| **1 · Спайки S-1…S-15** | SKIE + direct integration (Mac); BCV klib-дамп на Linux; `android skills add`; кодоген моделей в iOS-klib; Ktor `AuthPlugin` single-flight + cookie-режим; воспроизводимость Roborazzi; Konsist по KMP-source-set'ам; `@Observable` в `Pure` на Linux; `generateTokens`; buildable folders + umbrella `Features` (Mac); Gitea Release API с 8–10 ассетами; RuStore/Play API на throwaway-приложении; `--add-platform`; `withHostTest`; `exportArchive` без fastlane (Mac) | строка результата на спайк в roadmap; блокирующих «нет» не осталось | M | все, кроме S-1, S-10, S-15 (Mac) и S-12 (аккаунты) |
| **2 · Каркас и Linux-гейт** | `settings.gradle.kts`, `build-logic` (11 плагинов), пустой `shared/` с таргетами, `androidApp/app`, `iosApp/` из JetBrains-шаблона + `Packages/Pure`, `Makefile`, `scripts/tools/*`, `settings.json`, `.editorconfig`, `gradle.properties`, `.gate-fixtures/NF-01…10`, `AGENTS.md` | `make check` зелёный ≤ 15 с тёплый; `gate-selftest` — все NF красные; `make doctor`; SKIP-блок печатается | M | да |
| **3 · `shared/`** | `core/*` (api, auth, config, requirements, push, deeplink, mock, observability, i18n, lib), `SharedContainer`/`PlatformDeps`, презентер-паттерн, `entity/{user,session,todo,device}`, `feature/*`, кодоген моделей + `gen.sh`/`gen-check.sh`, `FixturesTest`, `apiDump`, NF-11…16 | `:shared:jvmTest` ≥ 80 %; `make kmp`, `apiCheck` зелёные; фикстуры всех сценариев валидны; презентеры 8 фич покрыты | L | да |
| **4 · Android** | `core/{designsystem,ui,platform,navigation,testing}`, `gen-tokens`, фичи + `demo`-flavor + `requirements`/`push`, Nav3, Hilt, Roborazzi на все состояния, Konsist, `:lint`, deep links, `i18n`/`deeplinks`/`tokens`-check | `make check` и `GATE_RELEASE=1` зелёные; скриншоты всех состояний; ручной прогон 8 фич на эмуляторе; Maestro при KVM | L | да (эмулятор — при RAM/KVM) |
| **5 · iOS** | `Core/{DesignSystem,Routing,Persistence,Platform,SharedBridge}`, `Features/*` + umbrella, `TestSupport`, test plans, snapshot-тесты всех состояний, XCUITest smoke, `mac.sh`, `check-mac`, NF-20…24, `mac-host.md` | `make check-full` зелёный; ручной прогон 8 фич на симуляторе; `swiftlint analyze` чист | L | **нет** — Mac (№ 7); до него — `Pure`, archlint, SwiftLint, Swift-код по канону 02 без компиляции (не Done) |
| **6 · Обвязка и docs** | `CLAUDE.md`, 23 правила, 9 скиллов, `docs/*` (8 reference + `strategy-mobile-canon/`), `docs/stack/README.md` + 15 `_templates`, `kinship.md` + `kin-diff`, `README.md` | dry-run на scratch-форке в трёх режимах (both / `--platform android` / `--platform ios`): `/init-service → /stack-deep-dive (A) → /de-template`, после каждого `make check` зелёный и `gate-selftest` красный; `/scaffold` 8 вариантов даёт компилируемый код | L | да (`ios` — до `check-mac`) |
| **7 · Экосистема и первый релиз** | остаток C-*/B-*/I-*/S-*, `wellknown` на dev-VPS, бэкенд с B-17/B-1…B-3, `/release --stop 1` demo-приложения (org `stit`) → Gitea Release; `--stop 2` в RuStore и Play internal; TestFlight — при № 8; P-1 | Release-страница со всеми ассетами; STORE-HANDOFF напечатан; модерация RuStore пройдена (или `Z+1`); строки stit-public обновлены | M | STOP-1 Android — да; iOS — нет |
| **8 · Сопровождение** | политика обновлений (Kotlin/AGP/Compose ежемесячно; Xcode с каждым major); backlog: `buildHealth` при поддержке AGP 9.x, Swift export при Beta, `sharedUi/` (CMP) по триггеру, извлечение `mobilekit`/`swiftkit`, Baseline Profile + GMD, SQLDelight-шов | roadmap `## Backlog` заполнен, триггеры зафиксированы | — | — |

**Критический путь 0 → 1 → 2 → 3 → 4 → 6 → 7 (Android-релиз) выполним целиком без Mac**; фаза 5
включается с появлением Mac-хоста и не блокирует остальное. До Mac можно: весь `shared/`, весь
Android, гейт, обвязка, docs, strategy-канон, cross-repo PR'ы, STOP-1/STOP-2 для Android; нельзя:
линковка `Shared.framework`, `iosSimulatorArm64Test`, компиляция `Core`/`Features`, snapshot,
XCUITest, подпись, IPA, TestFlight, App Store.

---

## Приложение: карта отчётов (`docs/research/`)

| файл | поток | что внутри |
|---|---|---|
| `brief.md` | — | факты об экосистеме, ограничения, критерии и веса, формат отчётов |
| `01-android-canon.md` | 01 | канон Android 2026: версии, AGP 9, Nav3, архитектура, модульность, тесты, гейт, ловушки, черновики `libs.versions.toml`/convention-плагинов/detekt/Konsist/rules |
| `02-ios-canon.md` | 02 | канон iOS 2026: Xcode 27/Swift 6.4, approachable concurrency, SwiftUI + `@Observable`, SwiftPM-модульность, Swift Testing, snapshot, SwiftLint/swift-format, 30 ловушек, черновики `Package.swift`/`.swiftlint.yml`/Makefile/rules |
| `03-shared-strategy.md` | 03 | развилка «сколько кодовых баз»: матрица, кейсы (мир/РФ), разбор Shopify, найм hh.ru, формы шаблона, триггеры пересмотра, запросы к 06 |
| `04-industry.md` | 04 | тренды 2025–2026, опросы, компании, фундамент vs мода, AI-агенты, community-шаблоны, «зашить/опция/избегать», 20 инвариантов |
| `05-distribution-ru.md` | 05 | сторы (RuStore API, Play, App Store), push в РФ, платежи/карты/вход, observability, право, CI без Mac, версионирование, STOP-контракт, `/release`, cross-repo |
| `06-tooling-agent.md` + `spikes/` | 06 | эмпирика на Linux: Android-гейт (времена, память, диск), KMP-klib под iOS, Swift 6.4 + Swift Testing + OpenAPI Generator, конфликты форматтеров, AGP 9 и convention-плагины, `android` CLI |
| `07-backend-contract.md` | 07 | auth (Bearer + BACKEND-HANDOFF), спек и кодоген (эмпирика), mock, среды/конфиг, deep links (`wellknown`), push (`/api/v1/devices`, `pushkit`), observability, i18n/токены, слой `mobile/<surface>/` в strategy, kinds, cross-repo |
| `08-template-design.md` | 08 | форма шаблона(ов), обвязка, гейт, docs, слой-мост, STOP, cross-repo, план, консолидированные вопросы владельцу |
| `_synthesis-notes.md` | — | рабочие заметки оркестратора (выжимки потоков) |
| `prompts/PROMPTS.md` | — | промпты агентов как запускались, порядок волн, модели, хроника прогона |
| `HANDOFF.md` | — | точка входа для продолжения: решение, что где, как продолжать, что не проверено |
| `DECISION.md` | — | принятое решение: форма, стек, гейт, STOP, обвязка, дефолты по вопросам, отложенное, условия пересмотра |
| `prompts/NEXT-SESSION.md` | — | промпт следующей сессии: сборка репозитория Fable-агентами, промпты агентов, стандарт качества, тесты, протокол проверки |
