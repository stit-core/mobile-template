# Architecture

Три места кода — `shared/` (KMP-ядро), `androidApp/` (Compose), `iosApp/` (SwiftUI) — и правило
раздела: в `shared/` всё, что **не касается экрана и ОС** (модели, репозитории, презентеры,
API-клиент, auth, deep links, mock, конфиг); в платформах — UI, навигационный контейнер и
реализации платформенных интерфейсов. Логика состояний экрана тоже в `shared/`, чтобы
тестироваться на JVM на Linux. Path-scoped правила (`.claude/rules/*`) держат terse-инварианты и
ссылаются сюда за «как и почему».

## Дерево

```
shared/                 KMP-модуль :shared — commonMain / commonTest / androidMain / androidHostTest / iosMain / jvmMain / jvmTest
androidApp/             Gradle-проекты :androidApp:{app, core/{designsystem,ui,platform,navigation,testing}, feature/{shell,auth,profile,todos,settings}, lint}
iosApp/                 Xcode-проект (Mac) + SwiftPM-пакеты Packages/{Pure,Core,Features,TestSupport}; Pure собирается на Linux
build-logic/            convention-плагины stit.* — единственное место конфигурации Gradle
config/                 detekt/, lint/, env/<flavor>.properties, openapi/
api/                    openapi.yaml (копия контракта бэкенда) + openapi.lock
mock/                   scenarios.yaml + fixtures/<scenario>/<entity>.json — одна база для тестов, demo, скриншотов, Maestro
e2e/                    Maestro-флоу (вне гейта)
scripts/                tools/ (гейт, doctor, status, mac), release/, git-hooks/
.gate-fixtures/         негативные фикстуры NF-NN — самотест гейта
docs/                   README (карта), getting-started, contributing, reference/, stack/
```

## `shared/` — пакеты и слои

```
shared/src/commonMain/kotlin/tech/stit/mobile/
├── SharedContainer.kt     composition root: SharedContainer(platform: PlatformDeps, config: AppConfig) — ленивые val + фабрики <x>Presenter()
├── PlatformDeps.kt        что даёт платформа: appInfo, secureStore, settings, filesDir, pushTransports, notificationPermission, telemetry, logSink, locale, connectivity
├── core/                  kit-граница — не импортирует entity/ и feature/
│   ├── api/               ApiClient (Ktor: X-Request-ID, X-Client, User-Agent, Accept-Language, Idempotency-Key на не-GET, RetryPolicy, problem+json → ApiError), ApiJson, HttpEngine (expect), {App,Auth,Devices,Todos,Users}Api, generated/
│   ├── auth/              AuthPlugin (Bearer + повтор после 401), SessionStore (access в памяти, single-flight refresh), SecureStore, TokenPair, AuthState
│   ├── config/            AppConfig (parse из сырых строк), AppEnv, ApiMode, AuthMode
│   ├── requirements/      RequirementsClient (GET /api/app/requirements, FileCache, не чаще раза в 5 мин), AppRequirements (status, flags → flag(name), links)
│   ├── push/              PushRegistrar (PUT /api/v1/devices, снимок DeviceRegistration в Settings), PushTransport/PushProvider, NotificationPermission, PushStatus
│   ├── deeplink/          Route (sealed, requiresAuth), DeepLinkParser: url → Route? (чистая функция)
│   ├── mock/              MockTransport (Ktor MockEngine + MockState), MockScenario, DemoMode; Fixtures — из :shared:embedFixtures (build/generated)
│   ├── observability/     Logger + LogSink (LogLevel, NoopLogSink), TelemetrySink (NoopTelemetrySink)
│   ├── i18n/              MessageKey (error.<code> → ключ ресурса, KnownErrorCodes), LocaleProvider
│   └── lib/               attempt {} → Result, presenterScope, newUuid, FileCache<T> (okio, JSON), Settings + SettingsKeys, AppInfo + Store, ConnectivityMonitor, DispatcherProvider (ioDispatcher — expect), Platform + currentPlatform (expect)
├── entity/<slug>/         <Slug>.kt (модель), <Slug>Repository.kt (final class, internal constructor; SSOT в памяти + FileCache), <Slug>Mapper.kt (internal <Slug>View.toModel()) — user, session, todo, device
└── feature/<group>/       <X>Presenter { state: StateFlow<XState>; dispatch(XIntent); close() }, XState (data class), XIntent (sealed) — shell/Shell, auth/{Login,Sessions,DeleteAccount}, profile/Profile, todos/{TodoList,TodoDetail}, settings/Settings, push/PushOptIn, requirements/Requirements, demo/DemoPanel
```

| слой | может импортировать | не импортирует |
|---|---|---|
| `core/**` | внешние библиотеки, `core/**` | `entity`, `feature` |
| `entity/**` | `core` | `feature` |
| `feature/<a>` | `core`, `entity` | `feature/<b>` |

Платформенные source-set'ы: `expect/actual` — только `currentPlatform` (`Platform.*.kt`),
`ioDispatcher` (`Dispatchers.*.kt`) и `platformHttpEngine` (`HttpEngine.*.kt`: OkHttp на
Android/JVM, Darwin на iOS). Остальное — реализации интерфейсов: `androidMain` —
`KeystoreSecureStore` (Keystore AES-GCM + DataStore), `DataStoreSettings`, `LogcatSink`
(единственный законный `android.util.Log`); `iosMain` — `KeychainSecureStore`
(`platform.Security`, `AfterFirstUnlockThisDeviceOnly`, без iCloud-синхронизации),
`UserDefaultsSettings`, `NSLogSink`; `jvmMain` — только actual'ы (тесты и Konsist). Каталог для
`FileCache` платформа передаёт строкой `PlatformDeps.filesDir`. UserNotifications /
LocalAuthentication / Network / Sentry — Swift-реализации `PlatformDeps` в `Core/Platform` и
Kotlin в `androidApp/core/platform`. Так `shared` компилируется в klib на Linux без cinterop
(`scripts/tools/kmp-check.sh` проверяет и запрет cinterop, и наличие klib-каталога —
`linkDebugFramework*` на Linux SKIPPED с кодом 0).

### Презентер

```kotlin
class TodoListPresenter internal constructor(todos: TodoRepository, connectivity: ConnectivityMonitor, dispatchers: DispatcherProvider) {
    val state: StateFlow<TodoListState>      // immutable data class: todos, loading, error: MessageKey?, offline, newTitle
    fun dispatch(intent: TodoListIntent)     // sealed class: Start, Refresh, TitleChanged, Create, Toggle, Delete
    fun close()                              // отменяет presenterScope — вызывает платформа (onCleared / deinit)
}
```

Одно состояние вниз, интенты вверх, one-off событий нет. `attempt {}` превращает `ApiError` в
`MessageKey` внутри state (`Throwable.toMessageKey()`), `CancellationException` проходит насквозь.
Сценарные тесты — `commonTest` через `testContainer(scenario)` (`SharedContainer` на
`MockTransport` + `FakePlatformDeps`; `runTest`, `StandardTestDispatcher`, Turbine). Use-case
заводится только там, где есть логика — в reference-фичах их нет.

### DI и composition root

`androidApp/` — Hilt (`@HiltViewModel` оборачивает презентер; `SharedModule` в `app` отдаёт
`SharedContainer` и его членов). `shared/` — без DI-фреймворка: `SharedContainer` с ленивыми `val`
и фабриками презентеров — одновременно плоский фасад для Swift и источник графа для Hilt;
`internal`-конструктор принимает `DispatcherProvider`, `Clock`, `FileSystem` (тесты подставляют
`TestDispatchers`, `FakeClock`, `FakeFileSystem`).
Триггер пересмотра (Metro): `SharedContainer` > ~40 членов или третья платформа.

### Фасад для Swift

Obj-C-экспорт `Shared.framework` (static). Публично — final-классы, `data`/`sealed`, `suspend`,
`StateFlow`; без generic'ов и `Any` в сигнатурах (rule `kmp-public-api`). Снимок API —
`shared/api/jvm/shared.api` + `shared/api/shared.klib.api` (BCV, `make api-dump` / `make api-check`;
строгая klib-валидация — `-Pstit.klibStrict=true` в `check-mac`). SKIE не подключён — фасад
должен быть удобен и без него. Публичные `suspend`-функции, из которых может вылететь `ApiError`,
помечены `@Throws(ApiError::class, CancellationException::class)` (второй класс требует
Kotlin/Native): без аннотации Obj-C-экспорт пропускает в Swift только `CancellationException`,
остальное роняет процесс.

### Persistence

БД в v1 нет: репозитории держат in-memory SSOT + `FileCache<T>` (okio, JSON) для последнего
снимка офлайн-чтения; настройки — `Settings` (DataStore / `NSUserDefaults`; ключи — `SettingsKeys`);
секреты — `SecureStore`. Шов для offline-first — `entity/<slug>/cache` + запись в `docs/stack/dependencies.md`
(кандидат SQLDelight — по спайку компиляции на Linux без cinterop).

## `androidApp/` — модули и convention-плагины

| проект | плагины `stit.*` | содержимое |
|---|---|---|
| `:androidApp:app` | `android.application` + `android.compose` + `android.hilt` + `konsist` | `StitApplication`, `MainActivity`, `NavDisplay`, Hilt-граф (`SharedModule`), flavors `env = dev\|staging\|prod\|demo`, `ArchitectureTest` (Konsist), агрегат Kover (`gate` = devDebug, пол 70 %) |
| `:androidApp:core:designsystem` | `android.library` + `android.compose` + `roborazzi` | токены → `StitTheme` (`CompositionLocal` поверх Material 3), компоненты, превью, скриншоты |
| `:androidApp:core:ui` | `android.library` + `android.compose` + `roborazzi` | композиты состояний (`Loading/Empty/Error/Offline/UpdateRequired/Maintenance`), pull-to-refresh, диалоги |
| `:androidApp:core:platform` | `android.library` + `android.hilt` | реализации `PlatformDeps`: `FcmTransport`, `RuStoreTransport`, `NotificationChannels`, `PermissionsGate`, `BiometricUnlock`, `SentryCrashReporter`, `ConnectivityMonitor` |
| `:androidApp:core:navigation` | `android.library` + `android.compose` | `NavKey`-обёртки над `Route`, back-stack, `DeepLink → NavKey` |
| `:androidApp:core:testing` | `android.library` | тестовые правила, фейки, Roborazzi-хелперы; `robolectric.properties` (`sdk=35`, как и в `shared/src/androidHostTest/resources/`) |
| `:androidApp:feature:<slug>` | `android.feature` (= library + compose + hilt + roborazzi + зависимости слоя) | `<X>ViewModel`, `<X>Screen`, `<X>Route`, `<X>NavEntry`, превью, тесты, `screenshots/` |
| `:androidApp:lint` | `jvm.library` + `com.android.lint` | `StitIssueRegistry` и проектные детекторы |

Convention-плагины (`build-logic/convention`, AGP 9: `ApplicationExtension`/`LibraryExtension`
напрямую, общее — `tech/stit/convention/AndroidDsl.kt`):

| id | что настраивает |
|---|---|
| `stit.android.application` | AGP 9.4 + встроенный Kotlin, SDK-уровни из `libs.versions.toml`, версия из `APP_VERSION`/`APP_BUILD`, flavors из `config/env/*.properties` → `BuildConfig`, R8 в release, подпись из `STIT_KEYSTORE_*`, Kover-агрегат с исключениями Hilt/Preview/NavEntry |
| `stit.android.library` | то же для библиотек; `resourcePrefix` из пути модуля (`core_ui_`); flavors `env`; `isIncludeAndroidResources` |
| `stit.android.feature` | library + compose + hilt + roborazzi + `implementation(shared, designsystem, ui, navigation, platform)` + Nav3/lifecycle; **feature → feature падает на конфигурации** |
| `stit.android.compose` | Compose BOM, compiler reports по `-PcomposeCompilerReports=true` |
| `stit.android.hilt` | Hilt + KSP (`kspTest` для Robolectric с `@HiltAndroidTest`) |
| `stit.android.lint` | Lint: `warningsAsErrors`, `abortOnError`, `checkDependencies`, `config/lint/lint.xml`, без baseline |
| `stit.kmp.library` | targets `androidLibrary { withHostTest }`, `jvm`, `iosArm64`, `iosSimulatorArm64`; framework `Shared` static; BCV klib; Kover 80 % (без `generated/`) |
| `stit.jvm.library` | Kotlin/JVM 17 + standalone Lint (иначе Lint не смотрит внутрь JVM-модуля) |
| `stit.quality` | detekt + compose-rules (без detekt-formatting; читает весь `src/`, исключая `generated/`), Kover на каждом модуле, Spotless (ktlint 1.8), sort-dependencies |
| `stit.roborazzi` | Robolectric + Roborazzi + Compose test в unitTest; эталоны `src/test/screenshots/` |
| `stit.konsist` | Konsist + inputs на исходники всего репо (иначе тест UP-TO-DATE после нарушения в другом модуле) |

## `iosApp/` — пакеты

| пакет | таргеты | импортирует | собирается на Linux |
|---|---|---|---|
| `Pure` | `AppFoundation` (`AppConfig` из `Bundle`, форматтеры, `LogSink` над `os.Logger`), `DesignSystemTokens` (значения токенов); тесты Swift Testing | Foundation | **да** — единственная часть iOS в Linux-гейте (`swift build`/`test`, `coverage-floor.sh` 70 %) |
| `Core` | `DesignSystem` (тема, Liquid Glass), `Routing` (`Router`, `[Route]`, pending route), `Persistence` (`KeychainSecureStore`), `Platform` (`ApnsRegistrar`, `NotificationCategories`, `Permissions`, `Biometrics`, `SentryCrashReporter`, `Connectivity`), `SharedBridge` (bootstrap `SharedContainer`, `AsyncStream`-адаптеры, `KotlinThrowable → AppError`) | Pure, Shared, SwiftUI | нет |
| `Features` | `Shell`, `AuthFeature`, `ProfileFeature`, `TodosFeature`, `SettingsFeature`; umbrella-продукт `Features` — добавление фичи не трогает `.pbxproj` | Pure, `Core/{DesignSystem,Routing,SharedBridge}`, Shared | нет |
| `TestSupport` | фейки `PlatformDeps`, `withScenario`, конфиг snapshot-testing | Core, Shared | нет |

`App/` — composition root: `AppEnvironment` собирает `SharedContainer` из Swift-реализаций и
`AppConfig` из `Info.plist` (xcconfig per схема `Dev|Staging|Prod|Demo`). Один `.xcodeproj`,
buildable folders, три test plans (`Unit`, `Snapshot`, `UI`). Всё, что импортирует `Shared`/
SwiftUI, верифицируется только `make check-mac` (`docs/reference/mac-host.md`); iOS-половина
помечена «не верифицирована на Mac», пока `build/gate-logs/mac.last` пуст.

## Матрица зависимостей и её enforcement

Android / shared (строка → может зависеть от столбца):

| ↓ от \ на → | shared | designsystem | ui | platform | navigation | testing | feature:* | app |
|---|---|---|---|---|---|---|---|---|
| shared | — | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ |
| core:designsystem | ✗ | — | ✗ | ✗ | ✗ | test | ✗ | ✗ |
| core:ui | ✓ | ✓ | — | ✗ | ✗ | test | ✗ | ✗ |
| core:platform | ✓ | ✗ | ✗ | — | ✗ | test | ✗ | ✗ |
| core:navigation | ✓ | ✗ | ✗ | ✗ | — | test | ✗ | ✗ |
| core:testing | ✓ | ✓ | ✓ | ✓ | ✓ | — | ✗ | ✗ |
| feature:X | ✓ | ✓ | ✓ | ✓ (интерфейсы) | ✓ | test | **✗** | ✗ |
| app | ✓ | ✓ | ✓ | ✓ | ✓ | test | ✓ | — |
| lint | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ |

iOS: `App` → всё; `Features/<X>` → `Pure`, `Core/{DesignSystem,Routing,SharedBridge}`, `Shared`;
`Features/<X>` ✗ `Features/<Y>`, ✗ `Core/{Persistence,Platform}`; `Core/*` → `Pure`, `Shared`;
`Pure` → Foundation.

| механизм | что ловит | стадия гейта |
|---|---|---|
| Gradle-граф + `stit.android.feature` (`afterEvaluate`) | feature → feature, feature → app | конфигурация |
| Konsist `ArchitectureTest` (`shared/src/jvmTest`) | `commonMain` без `android./androidx./java./javax./platform.`; слои `core < entity < feature`, `feature/<a>` ↛ `feature/<b>`; презентеры — final без generic'ов, со `state`/`dispatch`; `State` — data, `Intent` — sealed; `SharedContainer` без generic'ов | `shared jvm tests` |
| Konsist `ArchitectureTest` (`:androidApp:app`) | feature ↛ feature по импортам; `*ViewModel` только в `feature/*`; `*Screen` в файлах `*Screen.kt` | `android unit tests` |
| `gen-check.sh`, `mock-check.sh` + `FixturesTest` | модели ≠ регенерация / lock ≠ спека; фикстура не по строгой схеме | `gen-check`, `mock-check` |
| detekt `ForbiddenImport` (`config/detekt/detekt.yml`) | `android.util.Log` (кроме `LogcatSink`), `java.util.Date`, `java.util.logging.*`, `GlobalScope`, `androidx.compose.material.*` (M2), `retrofit2.*`, `androidx.security.crypto.*`, Compose test v1 | `detekt` |
| detekt `MagicNumber` + compose-rules | сырые `0xFF…`/`dp` в композаблах; правила Compose | `detekt` |
| Android Lint + `:androidApp:lint` | `warningsAsErrors`, `checkDependencies`, проектные детекторы | `android lint` |
| `kmp-check.sh` | `cinterops {`/`native.cocoapods` в `shared/build.gradle.kts`; отсутствие klib-каталога | `kmp` |
| BCV `apiCheck` | изменение фасада без `make api-dump` | `shared api` |
| SwiftLint custom rules + `Tools/archlint` | `import <Other>Feature`, `import Persistence/Platform` в `Features`, `URLSession(` вне `SharedBridge`, `print(`, `import Combine`, `@preconcurrency` без WHY; граф `Package.swift` | Swift-стадии |

## Гейт

`make check` (`scripts/tools/check.sh`) — Linux-полный, без fail-fast: `format kotlin` →
`format swift` → `gen-check` → `detekt` → `android lint` → `shared jvm tests` → `mock-check` →
`android unit tests` → `screenshots` → `coverage kotlin` → `kmp` → `shared api` →
`assembleDevDebug` (+ `assembleProdRelease` при `GATE_RELEASE=1`); `make check-fast` —
`format swift` → `format kotlin` → `detekt` → `shared jvm tests`. Затем явный SKIP-блок macOS-стадий (`shared link framework`, `shared iosSimulatorArm64Test`,
`xcodebuild build`, `test plan Unit`, `test plan Snapshot`) и код 0. Одна строка `OK:`/`FAIL:` на
стадию, логи — `build/gate-logs/<стадия>.log`, отметка последнего прогона — `check.last`.
`make check-mac` — стадии M1–M8 по SSH (`mac.sh`, пишет `mac.last`); `make check-full` — оба.
`make gate-selftest` — каждая фикстура `.gate-fixtures/NF-NN/fixture.sh` (`EXPECT`, `TOUCHES`,
`apply`) обязана уронить свою стадию; откат из резервной копии, не через git.

Полы покрытия: `shared` 80 % строк (`:shared:koverVerify`, без `generated/`), Android 70 %
(`:androidApp:app:koverVerifyGate`, без Hilt/Factory/Preview/NavEntry/BuildConfig), Swift `Pure`
70 % (`llvm-cov` через `coverage-floor.sh`). Логика намеренно сосредоточена в `shared` — там пол
выше, UI-слои держат Roborazzi/snapshot.

## Reference-фичи {#reference}

| # | фича | `shared/` | `androidApp/` | `iosApp/` | что демонстрирует | `/de-template` |
|---|---|---|---|---|---|---|
| 1 | **`demo`** — mock-режим | `core/mock` (`MockTransport`, `MockScenario`, `DemoMode`), `feature/demo/DemoPanelPresenter` | flavor `demo` (`.demo`, `API_MODE=mock`), debug-панель в dev/staging, флаг `demo_login` в prod | схема `Demo`, та же панель | одна фикстурная база для тестов, demo, скриншотов, Maestro; ревьюер магазина без бэкенда | оставить |
| 2 | `shell` | `feature/shell/ShellPresenter` (auth-gate, offline, pending route) | `NavDisplay` + табы, оверлеи, edge-to-edge, predictive back | `RootView`, `NavigationStack`, Liquid Glass-таб-бар | старт как единый сценарий: demo → кэши → session → route; requirements — свой презентер | оставить |
| 3 | `requirements` | `core/requirements`, `feature/requirements/RequirementsPresenter` | экраны в `core:ui` | то же | `GET /api/app/requirements`: `min_version`, maintenance, флаги | оставить |
| 4 | `auth` | `entity/{session,device}`, `feature/auth/{Login,Sessions,DeleteAccount}Presenter`, `core/auth` | `LoginScreen`, `SessionsScreen`, `DeleteAccountScreen`, биометрия (опция) | те же View | Bearer + refresh, `SecureStore`, удаление аккаунта, сессии | оставить при `auth.md#mode ≠ none` |
| 5 | `profile` | `feature/profile/ProfilePresenter` (`entity/user` остаётся — его читают `shell` и `auth`) | экраны + pull-to-refresh + состояния | те же | форма с валидацией, оптимистичное обновление, `ErrorModel.code → MessageKey` | **удалить** |
| 6 | `todos` | `entity/todo`, `feature/todos/{TodoList,TodoDetail}Presenter`, `Route.TodoList`/`TodoDetail` в `core/deeplink` | список/деталь/CRUD, deep link `/todos/{id}`, pending route, `Idempotency-Key`, offline read-only | те же + Universal Links | CRUD-образец, deep links end-to-end, идемпотентность | **удалить** (инфраструктура deep links остаётся) |
| 7 | `settings` | `feature/settings/SettingsPresenter`, `Settings`/`SettingsKeys` | тема, язык, уведомления, «О приложении» (`APP_ENV`), правовые документы до логина | те же | i18n, токены, permission-flow | оставить |
| 8 | `push` | `core/push` (`PushRegistrar`, `DeviceRegistration`, `PushStatus`), `feature/push/PushOptInPresenter` | `FcmTransport` + `RuStoreTransport`, каналы, `PUT /api/v1/devices` | `ApnsRegistrar`, категории | data-first payload → deep link; провайдеры по `PUSH_PROVIDERS` | оставить при непустом `push.md#providers` |

Общие образцы: тест презентера на JVM с `MockTransport`; Roborazzi на состояния
`loading/empty/populated/error` (Android) и snapshot (iOS, Mac); Maestro-флоу в `e2e/common`
на сценарии `populated`. Prune-лист — `.claude/skills/de-template/references/prune.md`.

## Ловушки {#traps}

| симптом | причина | что делать |
|---|---|---|
| `Cannot add extension with name 'kotlin'` | применён `org.jetbrains.kotlin.android` | AGP 9 со встроенным Kotlin — убрать; `kotlinOptions {}` → `kotlin { compilerOptions {} }`; kapt → KSP |
| `OutOfMemoryError: Metaspace` в Lint | `MaxMetaspaceSize < 1g` | `gradle.properties` из шаблона; `*.hprof` пишутся в `build/` |
| `linkDebugFramework*` SKIPPED, `bin/` нет | Linux не линкует Apple-фреймворки | `kmp-check` проверяет klib-каталог; framework — `check-mac` |
| `Inconsistent JVM-target` | Kotlin 21 vs Java 17 | JVM 17 везде (`JvmLibraryConventionPlugin`, `jvmToolchain(17)`) |
| `InaccessibleObjectException` в Robolectric | JDK 17+ без `--add-opens` | `configureUnitTests` плагина; `robolectric.properties` `sdk=35` |
| `createComposeRule` deprecated / тест «зависает» | Compose test v1; v2 дефолтит `StandardTestDispatcher` | `androidx.compose.ui.test.junit4.v2` + явная синхронизация |
| `hiltViewModel` не резолвится | переехал | `androidx.hilt.lifecycle.viewmodel.compose` |
| compose-rules ≥ 0.5 не грузятся | требуют detekt 2.0-alpha | detekt 1.23.8 + compose-rules 0.4.28 |
| detekt vs Spotless спорят об отступах | `detekt-formatting` (ktlint 0.50) | не подключать; формат — только Spotless (ktlint 1.8) |
| `swift format lint` vs SwiftLint цикл | `trailing_comma` | `disabled_rules: [trailing_comma]` |
| `no such module 'SwiftUI'` на Linux | таргет вне `Pure` | верифицировать на Mac |
| `Lint will treat :x as an external dependency` | JVM-модуль без `com.android.lint` | `stit.jvm.library` применяет standalone Lint |
| Konsist-тест UP-TO-DATE после нарушения | inputs только classpath | `stit.konsist` добавляет исходники репо в inputs; в `shared` — `jvmTest` получает `inputs.dir("src")` |
| Kover падает на корневом `kover(project)` | плагин не на каждом модуле | `stit.quality` применяет Kover везде |
| `resource mipmap/ic_launcher not found` после переименования | устаревшие merged-ресурсы | `--rerun-tasks` |
| Keychain «помнит» токен после удаления приложения | Keychain переживает удаление | очистка по флагу первого запуска |
| Universal Links не работают часами | Apple CDN кэширует AASA до 24 ч | `?mode=developer` в dev; каждый сабдомен — свой AASA |
| App Links «verified: false» на части сборок | один отпечаток в `assetlinks.json` | все отпечатки (upload, Play, RuStore) массивом |
| Kotlin-генератор кладёт токен в query | `in: cookie` не поддерживается | models-only + рукописный `AuthPlugin` |
| RuStore публикация 4xx | JWE-токен живёт 900 с, `timestamp ± 60 с` | перевыпустить токен |
| `sdkmanager` предупреждает | deprecated в cmdline-tools 23 | `android` CLI с `--no-metrics` |

## Где что лежит

| что | где |
|---|---|
| версии и SDK-уровни | `gradle/libs.versions.toml` (единственное место) |
| конфиг гейта | `config/detekt/detekt.yml`, `config/lint/lint.xml`, `.editorconfig`, `iosApp/.swiftlint.yml`, `iosApp/.swift-format` |
| вербы | `Makefile` (группы `##@ LOOP / GATE / KMP / MAC / RELEASE / SETUP / INTEL / META`) |
| build-time env | `config/env/<flavor>.properties`, `iosApp/Config/<Env>.xcconfig` |
| контракт API | `api/openapi.yaml` + `api/openapi.lock`, `config/openapi/kotlin-models.yaml` |
| фикстуры | `mock/scenarios.yaml`, `mock/fixtures/<scenario>/<entity>.json` → `Fixtures.kt` (`:shared:embedFixtures`, `shared/build/generated`) |
| эталоны | `androidApp/**/src/test/screenshots/`, `iosApp/**/__Snapshots__/` |
| логи гейта | `build/gate-logs/<стадия>.log`, `check.last`, `mac.last` |
| план | `.claude/plans/roadmap.md` |
| карта продукта | `<strategy-path>/docs/projects/<slug>/mobile/<surface>/` (read-only) |
