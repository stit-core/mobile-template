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
androidApp/             Gradle-проекты :androidApp:{app, core/{designsystem,ui,platform,navigation,testing}, feature/{shell,auth,profile,todos,settings}, widget, lint}
iosApp/                 App/ (composition root), Widgets/ (appex), Packages/{Pure,Core,Features,TestSupport}, Tools/archlint, Config/*.xcconfig, TestPlans/, UITests/, iosApp.xcodeproj
build-logic/            convention-плагины stit.* — единственное место конфигурации Gradle
design/                 tokens.json (DTCG) → make gen-tokens → Tokens.kt / Tokens.swift
config/                 detekt/, lint/, env/<flavor>.properties, openapi/
api/                    openapi.yaml (копия контракта бэкенда) + openapi.lock
mock/                   scenarios.yaml + fixtures/<scenario>/<entity>.json — одна база для тестов, demo, скриншотов, Maestro
e2e/                    Maestro-флоу android/ и ios/ (вне гейта, make e2e)
scripts/                tools/ (стадии гейта, doctor, status, mac, gen-tokens.py, app-id.sh), release/, git-hooks/
.gate-fixtures/         негативные фикстуры NF-01…27 — самотест гейта
docs/                   README (карта), getting-started, contributing, reference/, stack/
```

## `shared/` — пакеты и слои

```
shared/src/commonMain/kotlin/tech/stit/mobile/
├── SharedContainer.kt     composition root: SharedContainer(platform: PlatformDeps, config: AppConfig) — ленивые val + фабрики <x>Presenter()
├── PlatformDeps.kt        что даёт платформа: appInfo, secureStore, settings, filesDir, pushTransports, notificationPermission, background, telemetry, logSink, locale, connectivity
├── core/                  kit-граница — не импортирует entity/ и feature/
│   ├── api/               ApiClient (Ktor: X-Request-ID, X-Client, User-Agent, Accept-Language, Idempotency-Key на не-GET, RetryPolicy, problem+json → ApiError), ApiJson, HttpEngine (expect), {App,Auth,Devices,Todos,Users}Api, generated/
│   ├── auth/              AuthPlugin (Bearer + повтор после 401), SessionStore (access в памяти, single-flight refresh), SecureStore, TokenPair, AuthState
│   ├── config/            AppConfig (parse из сырых строк), AppEnv, ApiMode, AuthMode
│   ├── requirements/      RequirementsClient (GET /api/app/requirements, FileCache, не чаще раза в 5 мин), AppRequirements (status, flags → flag(name), links)
│   ├── push/              PushRegistrar (PUT /api/v1/devices, снимок DeviceRegistration в Settings), PushTransport/PushProvider, NotificationPermission, PushStatus
│   ├── background/        BackgroundScheduler (порт планировщика в PlatformDeps: schedule/cancel, не suspend) + BackgroundRunner (suspend run(): BackgroundResult), BackgroundTask (id, trigger, timing, conditions, periodMinutes, at — зеркало словарей карты), BackgroundResult (Notify | RefreshWidget | FreshData | Rejected(BackgroundRejection))
│   ├── widget/            WidgetState (Placeholder | Populated | Empty | SignedOut | Stale) — словарь состояний виджетов системы
│   ├── deeplink/          Route (sealed, requiresAuth), DeepLinkParser: url → Route? (чистая функция)
│   ├── mock/              MockTransport (Ktor MockEngine + MockState), MockScenario, DemoMode; DemoGate (runtime-вход по флагу demo_login), DemoStorage (SecureStore и Settings demo-сессии — в памяти), DemoControl (что про demo знает оболочка); Fixtures — из :shared:embedFixtures (build/generated)
│   ├── observability/     Logger + LogSink (LogLevel, NoopLogSink), TelemetrySink (NoopTelemetrySink)
│   ├── i18n/              MessageKey (error.<code> → ключ ресурса, KnownErrorCodes), LocaleProvider
│   └── lib/               attempt {} → Result, presenterScope, newUuid, FileCache<T> (okio, JSON), Settings + SettingsKeys, AppInfo + Store, ConnectivityMonitor + ConnectivityState (holder для платформ), DispatcherProvider (ioDispatcher — expect), Platform + currentPlatform (expect)
├── entity/<slug>/         <Slug>.kt (модель), <Slug>Repository.kt (final class, internal constructor; SSOT в памяти + FileCache), <Slug>Mapper.kt (internal <Slug>View.toModel()) — user, session, todo, device
└── feature/<group>/       <X>Presenter { state: StateFlow<XState>; dispatch(XIntent); close() }, XState (data class), XIntent (sealed) — shell/Shell, auth/{Login,Sessions,DeleteAccount}, profile/Profile, todos/{TodoList,TodoDetail}, settings/Settings, push/PushOptIn, requirements/Requirements, demo/DemoPanel
                           носители вне экранов в той же группе: <X>Action (suspend fun run(...): <X>Result — системное действие; todos/{CompleteTodoAction,AddTodoAction}), <X>Task (BackgroundRunner — фоновая задача; todos/{RefreshTodosTask,TodoReminderTask}), <X>Widget{Snapshot,Provider} (срез для виджета системы; todos/TodayTodosWidget*)
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
`FileCache` платформа передаёт строкой `PlatformDeps.filesDir`. UserNotifications / Network / APNs /
Sentry — Swift-реализации `PlatformDeps` в `Core/Platform` (`NetworkConnectivity` обновляет
Kotlin-holder `ConnectivityState` из `NWPathMonitor`); ConnectivityManager / FCM / RuStore /
BiometricPrompt / Sentry — Kotlin в `androidApp/core/platform`. Так `shared` компилируется в klib на
Linux без cinterop (`scripts/tools/kmp-check.sh` проверяет и запрет cinterop, и наличие
klib-каталога — `linkDebugFramework*` на Linux SKIPPED с кодом 0).

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

### Вне экранов: действие, виджет, фоновая задача

Три kind'а карты (`system-actions`, `widgets`, `background-tasks`) живут без экрана, поэтому
`StateFlow`-презентер им не подходит: система вызывает их однократно и вне процесса приложения.
Форма — три плоских носителя в той же `feature/<group>/`, что и презентеры группы:

```kotlin
internal class RefreshTodosTask(todos: TodoRepository, connectivity: ConnectivityMonitor) : BackgroundRunner {
    override suspend fun run(): BackgroundResult   // RefreshWidget | Rejected(Offline | Failed)
    companion object { const val ID = "refresh-todos"; val TASK = BackgroundTask(id = ID, …) }
}
internal class TodoReminderTask(todoId: String, todos: TodoRepository, deepLinkHost: String) : BackgroundRunner {
    override suspend fun run(): BackgroundResult   // Notify(category, titleKey, bodyKey, args, deepLink, notificationId)
    companion object { const val ID_PREFIX = "todo-reminder:"; fun task(todoId: String, at: Instant): BackgroundTask }
}
class CompleteTodoAction internal constructor(todos, session, connectivity, widget: TodayTodosWidgetProvider) {
    suspend fun run(todoId: String): CompleteTodoResult   // Done(title) | AlreadyDone(title) | SignedOut | Failed(error: MessageKey)
}
class TodayTodosWidgetProvider internal constructor(todos, session, connectivity, cache, deepLinkHost, clock) {
    suspend fun snapshot(): TodayTodosWidgetSnapshot   // состояние по сессии, возрасту снимка и сети
    suspend fun publish(): TodayTodosWidgetSnapshot    // собирает срез и пишет его в общий каталог
    companion object { const val SLUG = "today-todos" }
}
@Serializable data class TodayTodosWidgetSnapshot(state: WidgetState, items: List<TodayTodosWidgetItem>, generatedAt: Instant, tapUrl: String)
```

Правила: бизнес-правила остаются в фиче — носитель только оркеструет; класс публичный, конструктор
`internal` (Swift и платформы берут его фабрикой контейнера). Отказ не бросается, а возвращается
(`BackgroundResult.Rejected(reason)`, `CompleteTodoResult.SignedOut`) — сессия наружу не отдаётся,
а платформа сама решает про повтор; мутация идемпотентна (`AlreadyDone`), офлайн отвечает
`Failed(MessageKey.NetworkError)` без запроса. Платформенная обёртка (worker, Glance-виджет,
обработчик канала, интент) не содержит логики и зовёт носитель через `SharedContainer`. Ключ
задачи — её `slug`; задача на конкретную запись несёт id в ключе (`todo-reminder:<todoId>`), и по
нему же `backgroundRunner(id)` её восстанавливает. Виджет и расширение читают **снимок**, не
репозиторий: `publish()` сериализует срез в `<filesDir>/cache/widget-<slug>.json` (на iOS — каталог
App Group), поэтому расширение обходится без сети и токена; снимок публикует и само действие после
успешной мутации. `Stale` — снимок старше часа **и** нет сети: при сети старый срез остаётся
`Populated`, потому что следующее обновление его выровняет. Точки входа контейнера —
`backgroundTasks`, `backgroundRunner(id)`, `todoReminderTask(todoId, at)`, `completeTodoAction()`,
`addTodoAction()`, `todayTodosWidgetProvider()`; реестр, снимки и результаты публичны (часть фасада
для Swift). Реестр задач планируется на старте приложения (`StitApplication` / `AppDelegate`),
задача на запись — по действию пользователя.

### DI и composition root

`androidApp/` — Hilt (`@HiltViewModel` оборачивает презентер; `app/di/SharedModule` отдаёт
`SharedContainer`, `ConfigModule` — `AppConfig` + `AppOptions` из `BuildConfig`, `PlatformModule` —
`AndroidPlatformDeps` и `BiometricUnlock`; тесты подменяют их `di/TestModules.kt`). `iosApp/` —
`App/AppEnvironment.swift` (`IosPlatformDeps` → `SharedContainer`, фабрики `<x>Model()`). `shared/` —
без DI-фреймворка: `SharedContainer` с ленивыми `val` и фабриками презентеров — одновременно плоский
фасад для Swift и источник графа для Hilt; `internal`-конструктор принимает `DispatcherProvider`,
`Clock`, `FileSystem` (тесты подставляют `TestDispatchers`, `FakeClock`, `FakeFileSystem`).
Триггер пересмотра (Metro): `SharedContainer` > ~40 членов или третья платформа.

### Фасад для Swift

Obj-C-экспорт `Shared.framework` (static). Публично — final-классы, `data`/`sealed`, `suspend`,
`StateFlow`; без generic'ов и `Any` в сигнатурах (rule `kmp-public-api`). Снимок API —
`shared/api/jvm/shared.api` + `shared/api/shared.klib.api` (BCV, `make api-dump` / `make api-check`;
строгая klib-валидация — `-Pstit.klibStrict=true` в `check-mac`). SKIE не подключён — фасад
должен быть удобен и без него: `Core/SharedBridge` даёт `KotlinStateFlow.stream(of:)` → `AsyncStream`,
`PresenterModel`, `AppError`, `KotlinInstant.date`, `KotlinBoolean` для `suspend … : Boolean`.
Публичные `suspend`-функции, из которых может вылететь `ApiError`, помечены
`@Throws(ApiError::class, CancellationException::class)` (второй класс требует Kotlin/Native): без
аннотации Obj-C-экспорт пропускает в Swift только `CancellationException`, остальное роняет процесс.

### Persistence

БД в v1 нет: репозитории держат in-memory SSOT + `FileCache<T>` (okio, JSON) для последнего
снимка офлайн-чтения; настройки — `Settings` (DataStore / `NSUserDefaults`; ключи — `SettingsKeys`);
секреты — `SecureStore`. Шов для offline-first — `entity/<slug>/cache` + запись в `docs/stack/dependencies.md`
(кандидат SQLDelight — по спайку компиляции на Linux без cinterop).

### Demo в обычной сборке

App Review нужен проход по приложению без бэкенда, и отдельная сборка для этого не годится —
решает `shared`. `DemoGate` существует только при `API_MODE=real`; вход показывается, пока
`requirements.flag("demo_login")` истинен (404 или офлайн ⇒ `AppRequirements.Default`, флага нет,
кнопки нет — безопасный дефолт). `enter()` подменяет движок `ApiClient` на `MockTransport.engine`
(`useEngine`), включает песочницу `DemoStorage` (`SecureStore` и `Settings` demo-сессии живут в
памяти), делает `SessionStore.reset()` и сеет сессию сценарием `authenticated`: реальный
refresh-токен не читается и не перезаписывается, в боевой бэкенд не уходит ни один запрос
(`DemoGateTest`). `leave()` возвращает движок и настоящие хранилища, перечитывает сессию и
сбрасывает `requirements`, чтобы флаги и правовые ссылки фикстур не пережили выход. Метка
`app.demo.active` (`SettingsKeys.DEMO_ACTIVE`) пишется **до** подмены, то есть в реальные
настройки: первый старт после убитого в demo процесса вычищает снимки фикстур. Флаг, выключенный
посреди demo, обрывает не текущую сессию, а следующий вход. Поверхности вне экранов идут через тот
же контейнер и в demo автоматически работают на фикстурах. Панель сценариев остаётся mock-only
(flavor `demo`): ревьюеру сценарий не переключается, вход фиксирован на `authenticated`.

## `androidApp/` — модули и convention-плагины

| проект | плагины `stit.*` | содержимое |
|---|---|---|
| `:androidApp:app` | `android.application` + `android.compose` + `android.hilt` + `konsist` | `StitApplication` (каналы push, `SentryCrashReporter.init`), `MainActivity : AppCompatActivity` (edge-to-edge, `NotificationPermissionGate.register`, intent → `ShellIntent.OpenLink`), `StitApp` (тема из `SettingsViewModel`, `testTagsAsResourceId`), `AppNavigation` (`NavDisplay`, back stack), `AppOptions`, `di/{Config,Platform,Shared}Module`; flavors `env = dev\|staging\|prod\|demo` (`demo` — `.demo`), `generateLocaleConfig` + `localeFilters en,ru`; тесты `ArchitectureTest` (Konsist), `DemoFlowTest` (Hilt + Robolectric, `di/TestModules`); агрегат Kover (`gate` = devDebug, пол 70 %) |
| `:androidApp:core:designsystem` | `android.library` + `android.compose` + `roborazzi` | `generated/Tokens.kt` (из `design/tokens.json`), `StitColors/StitTypography/StitSpacing/StitShapes/StitIcons.kt`, `Theme.kt` (`StitTheme`, `LocalStit*` поверх Material 3), `component/Stit{Button,ButtonVariant,TextField,ListItem,StatusBadge,Tone,TopBar}.kt`, `ComponentsTest`, скриншоты `theme_*`, `components_*` |
| `:androidApp:core:ui` | `android.library` + `android.compose` + `roborazzi` | состояния `LoadingState`, `EmptyState`, `ErrorState`, `OfflineBanner`, `UpdateRecommendedBanner`, `DemoBanner` (demo-сессия с выходом), `BlockingScreens.kt` (`UpdateRequired`, `Maintenance`, `LockedState`), `MessageText.kt` (`MessageKey` → `core_ui_error_*`), `DateTimeText`, `UiTestTags` |
| `:androidApp:core:platform` | `android.library` + `android.hilt` | реализации `PlatformDeps`: `AndroidPlatformDeps`, `AndroidAppInfo`, `AndroidConnectivityMonitor` (`ConnectivityManager`, VALIDATED), `AndroidLocaleProvider`, `NotificationPermissionGate`, `BiometricUnlock`/`DefaultBiometricUnlock`, `SentryCrashReporter` + `SentryLogSink`, `push/{PushTransports,FcmTransport,RuStoreTransport,NotificationChannels,PushMessage,PushNotifier,StitFirebaseMessagingService,StitRuStoreMessagingService}`, `background/{AndroidBackgroundScheduler,BackgroundTaskWorker,BackgroundAlarmReceiver,BackgroundDelivery}` (WorkManager + будильник; воркер берёт граф Hilt EntryPoint'ом, вход — `task_id`), `action/{CompleteTodoActionHandler,AddTodoActionHandler,NotificationActionReceiver,AppFunctions}` (обработчики каналов + реестр действий для ассистента), `widget/WidgetRefresher.kt` (порт перерисовки по `slug`; реализация — в модуле виджетов); свой `AndroidManifest.xml` (`ACCESS_NETWORK_STATE`, `POST_NOTIFICATIONS`, `USE_BIOMETRIC`, `SCHEDULE_EXACT_ALARM`; приёмники `BackgroundAlarmReceiver` и `NotificationActionReceiver`; `StitAppFunctionService` с `BIND_APP_FUNCTION_SERVICE` и `tools:targetApi="36"`; сервисы FCM/RuStore — RuStore `exported=true` по контракту SDK) |
| `:androidApp:core:navigation` | `android.library` + `android.compose` | `Destination : NavKey` (sealed, `@Serializable`), `Route.toDestination()`, `BackStack.kt` (`navigateTo/resetTo/pop/current`); `api(navigation3-runtime)` |
| `:androidApp:core:testing` | `android.library` + `android.compose` | `Fakes.kt` (`FakePlatformDeps`, `demoConfig()`, `Fake*`), `FakeBiometricUnlock`, `MainDispatcherRule`, база `ScreenshotTest` (`capture(name, darkTheme)`, `w360dp-h640dp-xhdpi`, UTC); `robolectric.properties` (`sdk=35`, как и в `shared/src/androidHostTest/resources/`); только в `testImplementation` |
| `:androidApp:feature:<group>` | `android.feature` (= library + compose + hilt + roborazzi + зависимости слоя) | `<Key>Route/<Key>Screen/<Key>ViewModel.kt`, `<Group>NavEntry.kt` (`<group>Entries`), `<Group>Previews.kt`, `<Group>TestTags.kt`, `res/values{,-ru}/strings.xml`; тесты `<Key>ScreenTest`, `<Group>ScreenshotTest`, `<Group>Fixtures`, `src/test/screenshots/<key>_<state>[_dark].png` |
| `:androidApp:widget` | `android.library` + `android.compose` + `android.hilt` + `roborazzi` (+ `glance-appwidget`) | виджеты системы всех поверхностей: `<Slug>Widget.kt` (`GlanceAppWidget`; тап — `actionStartActivity(ACTION_VIEW, snapshot.tapUrl)` c `setPackage`), `<Slug>WidgetReceiver.kt`, `<Slug>WidgetAction.kt` (`ActionCallback` — кнопка строки, зовёт обработчик канала), `WidgetTokens.kt` (роли токенов → day/night `ColorProvider` Glance), `GlanceWidgetRefresher.kt` (реализация порта `WidgetRefresher`: `publish()` + `updateAll`), `WidgetUpdates.kt` (`on-app-use`: коллектор `todoRepository.todos`, стартует в `StitApplication`), `WidgetGraph.kt` (Hilt entry point + `@Binds` порта), `res/xml/widget_<slug>_info.xml` (префикс модуля — Lint `ResourceName`), `<receiver>` в своём `AndroidManifest.xml`, `res/values{,-ru}/strings.xml` (`widget_`); тесты `<Slug>Widget{,Action}Test`, `WidgetRefreshTest`, `WidgetFixtures` + Roborazzi `src/test/screenshots/widget_<slug>_<state>[_dark].png`; reference — `today-todos`. Отдельный модуль, а не `feature/<group>`: у виджета своя точка входа в ОС, свой receiver и опциональная зависимость Glance, а срез может собираться из сущностей нескольких групп |
| `:androidApp:lint` | `jvm.library` + `com.android.lint` | `StitIssueRegistry` (`Lint-Registry-v2`), `DesignSystemDetector` — issue `StitRawDesignValue` (`Color(литерал)`, `.dp`/`.sp` вне `core.designsystem`) |

Convention-плагины (`build-logic/convention`, AGP 9: `ApplicationExtension`/`LibraryExtension`
напрямую, общее — `tech/stit/convention/AndroidDsl.kt`):

| id | что настраивает |
|---|---|
| `stit.android.application` | AGP 9.4 + встроенный Kotlin, SDK-уровни из `libs.versions.toml`, версия из `APP_VERSION`/`APP_BUILD`, flavors из `config/env/*.properties` → `BuildConfig` + `manifestPlaceholders` (хост/схема deep link в intent-filter), `applicationIdSuffix .debug`, R8 в release, подпись из `STIT_KEYSTORE_*`, Kover-агрегат листовых модулей (без `:lint`, `:core:testing`; исключены Hilt/Factory/Preview/NavEntry/BuildConfig) |
| `stit.android.library` | то же для библиотек + плагин kotlinx-serialization (`NavKey`); `resourcePrefix` из пути модуля (`core_ui_`); flavors `env`; `isIncludeAndroidResources` |
| `stit.android.feature` | library + compose + hilt + roborazzi + `implementation(shared, designsystem, ui, navigation, platform)` + activity-compose/Nav3/lifecycle; **feature → feature падает на конфигурации** |
| `stit.android.compose` | Compose BOM, compiler reports по `-PcomposeCompilerReports=true` |
| `stit.android.hilt` | Hilt + KSP (`kspTest` для Robolectric с `@HiltAndroidTest`, `failOnNoDiscoveredTests=false`) |
| `stit.android.lint` | Lint: `warningsAsErrors`, `abortOnError`, `checkDependencies`, `config/lint/lint.xml`, без baseline |
| `stit.kmp.library` | targets `androidLibrary { withHostTest }`, `jvm`, `iosArm64`, `iosSimulatorArm64`; framework `Shared` static; BCV klib (`strictValidation` по `-Pstit.klibStrict`); Kover 80 % (без `generated/`) |
| `stit.jvm.library` | Kotlin/JVM 17 + standalone Lint (иначе Lint не смотрит внутрь JVM-модуля) |
| `stit.quality` | detekt + compose-rules (без detekt-formatting; читает весь `src/`, исключая `generated/`), Kover на каждом модуле, Spotless (ktlint 1.8), sort-dependencies |
| `stit.roborazzi` | Robolectric + Roborazzi + Compose test в unitTest; эталоны `src/test/screenshots/` |
| `stit.konsist` | Konsist + inputs на исходники `androidApp/` и `shared/src` (иначе тест UP-TO-DATE после нарушения в другом модуле) |

## `iosApp/` — пакеты

| пакет | таргеты | импортирует | собирается на Linux |
|---|---|---|---|
| `Pure` | `AppFoundation` (`AppConfig` из `Bundle`/`Info.plist`, `AppEnv`, `AppFormat`, `AppLogger` + `OSLogSink`/`AppLogSink`, `AppStrings`, `AppTimestamp`, `BackgroundTaskIdentifier` — `<bundle>.<slug>` ↔ slug, `WidgetSnapshotFile` — имя файла снимка), `DesignSystemTokens` (`Tokens.swift` из `design/tokens.json`, `ArgbColor` — контраст); тесты `AppFoundationTests`, `DesignSystemTokensTests` | Foundation | **да** — стадии `swift pure build+tests` и `coverage swift` (`coverage-floor.sh`, 70 %) |
| `Core` | `DesignSystem` (`Theme` через `@Environment(\.theme)`, `PrimaryButtonStyle`, `.card()`, `StateViews`: `LoadingStateView/EmptyStateView/ErrorStateView/ErrorText/OfflineBanner`, каталог `error.*`), `Routing` (`AppTab`, `AppRoute`, `Router`), `Persistence` (`FilesDirectory`, `FirstLaunchReset`), `Platform` (`ApnsRegistrar`, `DeviceInfo`/`DeviceLocale`, `NetworkConnectivity`, `NotificationCenterPermission`, `PushPayload`/`NotificationCategories`/`NotificationDeduplicator`, `Background/{BgTaskScheduler,LocalNotifier,BackgroundDelivery}`, `SystemActionsBridge` (`SharedSystemActionRunner` — контейнер за протоколом `SystemActionRunner`), `NotificationActions` (кнопка `COMPLETE_TODO` в `NotificationCategories`), `WidgetRefresh`, `SentryCrashReporter`, `SharedLogSink`), `SystemActions` (`CompleteTodoIntent`, `AddTodoIntent`, `SystemActions` — имя таргета не `Intents`: так зовётся системный модуль SiriKit), `Widgets` (`AppGroupContainer`, `TodayTodosEntry`, `TodayTodosSnapshotStore` — `Codable`-зеркало снимка `shared` из контейнера App Group), `SharedBridge` (`Exports` — `@_exported import Shared`, `FlowBridge`, `PresenterModel`, `RouteBridge`, `AppError`, `KotlinInterop`, `SendableStates`); тесты `RoutingTests`, `SharedBridgeTests`, `DesignSystemTests`, `SystemActionsTests`, `WidgetsTests` | Pure, Shared, SwiftUI, `sentry-cocoa` 9.28 | `SystemActions` и `Widgets` — да (только Foundation), остальное нет |
| `Features` | `Shell` (`ShellModel`, `RequirementsModel`, `RequirementsOverlayView`), `AuthFeature` (`Login/Sessions/DeleteAccount{View,Model}`), `ProfileFeature`, `TodosFeature` (`TodoList/TodoDetail{View,Model}`), `SettingsFeature` (`Settings/PushOptIn{View,Model}`, `DemoPanelModel`, `DemoPanelSection`), `WidgetsFeature` (`<Slug>WidgetView` — вью виджетов системы, `switch` по состоянию снимка, generic по интенту кнопки); umbrella `Features` из `featureNames` — добавление фичи не трогает `.pbxproj`; `WidgetsFeature` вне `featureNames` (своя фабрика таргета: зависит от `Core/Widgets`, но не от `Routing`/`SharedBridge`); тесты `<X>Tests` + `SnapshotTests` | Pure, `Core/{DesignSystem,Routing,SharedBridge}`, Shared; `WidgetsFeature` — `Core/{DesignSystem,Widgets}` | нет |
| `TestSupport` | `FakePlatformDeps` (+ `InMemorySecureStore`, `InMemorySettings`, `FakeNotificationPermission`, `FakeLocale`), `Scenario.container(_:)` (demo-конфиг на `MockTransport`), `Snapshots.assertScreen` (light + dark, `ViewImageConfig.iPhone13`), `waitUntil` | `Core/SharedBridge`, Pure, `swift-snapshot-testing` 1.19.5 | нет |
| `Tools/archlint` | `ArchLintCore` (`Manifest` над `swift package dump-package`, `ArchGraph`, `ArchRules`, `Report`), исполняемый `archlint <Packages> [--graph]`; тесты `ArchLintCoreTests` | Foundation | **да** — стадия `archlint` |

`App/` — composition root: `AppEnvironment` собирает `SharedContainer` из `IosPlatformDeps`
(включая `background`) и `AppFoundation.AppConfig` из `Info.plist` (xcconfig
`Dev|Staging|Prod|Demo`), `AppEnvironment.registerBackgroundTasks()` поднимает задачи реестра, а
`filesDir` приложения указывает в контейнер App Group, чтобы снимок виджета видело расширение;
`AppDelegate` — APNs, `UNUserNotificationCenterDelegate`, регистрация фоновых задач и публикация
снимка при уходе в фон; `AppShortcutsProvider` — фразы системных действий с каналом `voice`
(провайдер один на приложение и живёт в app-таргете); `RootView` — auth-gate → `TabView` с
`NavigationStack` на таб, `iOSApp.swift` — `StitApp`. `iosApp/Widgets/`
(`StitWidgets.swift`, `Info.plist`, `Widgets.entitlements`) — appex виджетов: `@main` собирает вью
из `Features/WidgetsFeature` над записями `Core/Widgets`, читает снимок из контейнера App Group и
в сеть не ходит (`stale` выводит по возрасту снимка). `App/Info.plist` несёт
`BGTaskSchedulerPermittedIdentifiers` и `UIBackgroundModes` (`fetch`, `processing`,
`remote-notification`), `Config/Base.xcconfig` — `APP_GROUP_ID = group.$(BUNDLE_ID)` и
`WIDGETS_BUNDLE_ID`. `iosApp.xcodeproj`: таргеты `iosApp` и `iosAppWidgets`, конфигурации на
`Config/Dev.xcconfig`, App Group в entitlements обоих, shared-схема `Dev`, build phase
`scripts/embed-shared.sh`; три test plans (`Unit`, `Snapshot`, `UI`).
Всё, что импортирует `Shared`/SwiftUI, верифицируется только `make check-mac`; подключение локальных
пакетов, per-env схем и UI-таргета к проекту — на Mac (`docs/reference/mac-host.md`); iOS-половина
помечена «не верифицирована на Mac», пока `build/gate-logs/mac.last` пуст.

## Матрица зависимостей и её enforcement

Android / shared (строка → может зависеть от столбца):

| ↓ от \ на → | shared | designsystem | ui | platform | navigation | testing | feature:* | widget | app |
|---|---|---|---|---|---|---|---|---|---|
| shared | — | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ |
| core:designsystem | ✗ | — | ✗ | ✗ | ✗ | test | ✗ | ✗ | ✗ |
| core:ui | ✓ | ✓ | — | ✗ | ✗ | test | ✗ | ✗ | ✗ |
| core:platform | ✓ | ✗ | ✗ | — | ✗ | test | ✗ | ✗ | ✗ |
| core:navigation | ✓ | ✗ | ✗ | ✗ | — | test | ✗ | ✗ | ✗ |
| core:testing | ✓ | ✓ | ✗ | ✓ | ✗ | — | ✗ | ✗ | ✗ |
| feature:X | ✓ | ✓ | ✓ | ✓ | ✓ | test | **✗** | ✗ | ✗ |
| widget | ✓ | ✓ | ✗ | ✓ | ✗ | test | **✗** | — | ✗ |
| app | ✓ | ✓ | ✓ | ✓ | ✓ | test | ✓ | ✓ | — |
| lint | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ |

iOS: `App` → всё; `Features/<X>` → `Pure`, `Core/{DesignSystem,Routing,SharedBridge}`, `Shared`;
`Features/<X>` ✗ `Features/<Y>` (кроме umbrella и тестов своей фичи), ✗ `Core/{Persistence,Platform}`;
`Core/*` → `Pure`, `Shared`; `TestSupport` ✗ `Features`; `Pure` → Foundation; граф ацикличен.
`Core/SystemActions` → `Pure` (тип интента виден расширению, поэтому `Shared` в него не тянется;
контейнер приходит мостом `Platform/SystemActionsBridge` через протокол `SystemActionRunner`);
`Core/Widgets` → `Pure` (снимок — `Codable` над Foundation, расширение остаётся лёгким);
`Core/Platform` → `SharedBridge`, `SystemActions`; `Features/WidgetsFeature` →
`Core/{DesignSystem,Widgets}` (без `Routing`/`SharedBridge`) и рисует вью виджетов.
Композиционные корни `App/` и appex `iosApp/Widgets/` — bundle-таргеты `.xcodeproj`, в графе
`Package.swift` их нет, поэтому `archlint` их не видит: границы там держит ревью.

| механизм | что ловит | стадия гейта |
|---|---|---|
| Gradle-граф + `stit.android.feature` (`afterEvaluate`) | feature → feature, feature → app | конфигурация |
| Konsist `ArchitectureTest` (`shared/src/jvmTest`) | `commonMain` без `android./androidx./java./javax./platform.`; слои `core < entity < feature`, `feature/<a>` ↛ `feature/<b>`; презентеры — final без generic'ов, со `state`/`dispatch`; `State` — data, `Intent` — sealed; `SharedContainer` без generic'ов | `shared jvm tests` |
| Konsist `ArchitectureTest` (`:androidApp:app`) | feature ↛ feature, core ↛ feature и widget ↛ feature по импортам; `commonMain` без Android/JVM; наследники `ViewModel` — `@HiltViewModel`, `*ViewModel`, в `feature/`; `@Composable *Screen` — в `*Screen.kt` без параметров `*ViewModel`; `hiltViewModel()` — только в `*Route` и `app/`; `*Route`/`*Entries` — в `feature/`; наследники `GlanceAppWidget` — `*Widget` в `androidApp/widget/`, `ActionCallback` — `*WidgetAction` там же | `android unit tests` |
| `gen-check.sh`, `mock-check.sh` + `FixturesTest` | модели ≠ регенерация / lock ≠ спека; фикстура не по строгой схеме | `gen-check`, `mock-check` |
| `tokens-check.sh` | `Tokens.kt`/`Tokens.swift` ≠ регенерация из `design/tokens.json` (NF-19) | `tokens-check` |
| `i18n-check.sh` | ключи `values/` ≠ `values-ru/` в модуле (NF-17); код `MessageKey`/`KnownErrorCodes` без `core_ui_error_<code>`; iOS `Localizable.xcstrings` без `error.<code>` — WARN | `i18n-check` |
| `deeplinks-check.sh` | хост/схема в манифесте литералом (NF-18); ключ пуст в env; `pathPrefix` или строка `deep-links.md` без теста в `DeepLinkParserTest` | `deeplinks-check` |
| `capability-check.sh` | реестр — код `shared`: задача (`*Task : BackgroundRunner`) без объявленного ключа, без ветки `backgroundRunner(id)` или без теста; `BGTaskSchedulerPermittedIdentifiers` ≠ множеству задач, которые ведёт `BGTaskScheduler`; виджет без эталона и теста на каждое состояние `WidgetState`; строка `core_platform_action_*`/`widget_*` только в одном из `values{,-ru}` (iOS-каталог — WARN); `android:name=".X"` манифеста без класса или строковой константы. Строки `docs/stack/background.md` сверяются дополнительно, если файл заведён; пустой набор (форк после `/de-template`) — зелёный | `capability-check` |
| detekt `ForbiddenImport` (`config/detekt/detekt.yml`) | `android.util.Log` (кроме `LogcatSink`), `java.util.Date`, `java.util.logging.*`, `GlobalScope`, `androidx.compose.material.*` (M2), `retrofit2.*`, `androidx.navigation.*` (Nav 2), `androidx.security.crypto.*`, Compose test v1 | `detekt` |
| detekt `MagicNumber` + compose-rules | сырые `0xFF…`/`dp` в композаблах; `CompositionLocalAllowlist` (`LocalStit*`); правила Compose | `detekt` |
| Android Lint + `:androidApp:lint` | `warningsAsErrors`, `checkDependencies`; `StitRawDesignValue` — `Color(литерал)`, `.dp`/`.sp` вне designsystem (и в property-декларациях) | `android lint` |
| `kmp-check.sh` | `cinterops {`/`native.cocoapods` в `shared/build.gradle.kts`; отсутствие klib-каталога | `kmp` |
| BCV `apiCheck` | изменение фасада без `make api-dump` | `shared api` |
| SwiftLint custom rules (`iosApp/.swiftlint.yml`) | `no_print`, `no_nslog`, `no_combine`, `no_raw_urlsession` (вне `SharedBridge`), `no_feature_import`, `no_platform_import_in_features`, `no_hardcoded_color` и `no_fixed_font_size` (вне `DesignSystem`), `preconcurrency_needs_why` | `swiftlint` |
| `Tools/archlint` над `swift package dump-package` | `Pure` только Pure; `Core`/`TestSupport` ↛ `Features`; `Features` ↛ `Core/{Persistence,Platform}`; фича ↛ фича (кроме umbrella и своих тестов); цикл в графе | `archlint` |
| `swift test` в `Packages/Pure` + `coverage-floor.sh` | `import UIKit`/SwiftUI в `Pure` — нет модуля на Linux (NF-22); покрытие `Pure` < 70 % | `swift pure build+tests`, `coverage swift` |

## Гейт

`make check` (`scripts/tools/check.sh`) — Linux-полный, без fail-fast, стадии от дешёвых к дорогим
и Swift до Gradle: `format kotlin` → `format swift` → `swiftlint` → `archlint` → `swift pure build+tests`
→ `coverage swift` → `tokens-check` → `i18n-check` → `deeplinks-check` → `capability-check` →
`gen-check` → `detekt` →
`android lint` → `shared jvm tests` → `mock-check` → `android unit tests` → `screenshots` →
`coverage kotlin` → `kmp` → `shared api` → `assembleDevDebug` (+ `assembleProdRelease` при
`GATE_RELEASE=1`); `make check-fast` (`FAST=1`) — `format swift` → `swiftlint` → `format kotlin` →
`detekt` → `shared jvm tests`. Затем явный SKIP-блок macOS-стадий (`shared link framework`,
`shared iosSimulatorArm64Test`, `xcodebuild build`, `test plan Unit`, `test plan Snapshot`) и код 0.
Одна строка `OK:`/`FAIL:` на стадию, логи — `build/gate-logs/<стадия>.log`, отметка последнего
прогона — `check.last`; `check.sh --only <стадия>` — одна стадия (так гоняет `gate-selftest`).
`make check-mac` — `mac.sh check` по SSH: doctor → `linkDebugFrameworkIosSimulatorArm64` →
`iosSimulatorArm64Test` → `apiCheck -Pstit.klibStrict=true` → `xcodebuild build` → планы `Unit`,
`Snapshot` (+ `UI` при `MAC_UI=1`); пишет `mac.last`. `make check-full` — оба.
`make gate-selftest` — 27 фикстур `.gate-fixtures/NF-NN/fixture.sh` (`EXPECT`, `TOUCHES`, `apply`):
NF-01…10 — Android/KMP-каркас (detekt, Spotless, Lint, Gradle-граф, JVM-target, Roborazzi, Kover),
NF-11…16 — `shared` (Konsist, kmp, gen-check, mock-check), NF-17…19 — `i18n-check`, `deeplinks-check`,
`tokens-check`, NF-20…24 — Swift (SwiftLint, archlint, `import UIKit` в Pure), NF-25…27 —
`capability-check` (идентификатор задачи убран из `BGTaskSchedulerPermittedIdentifiers`; удалён
эталон состояния виджета; ответ действия остался без русской локали); каждая обязана уронить свою
стадию, откат из резервной копии, не через git.

Полы покрытия: `shared` 80 % строк (`:shared:koverVerify`, без `generated/`), Android 70 %
(`:androidApp:app:koverVerifyGate`, без Hilt/Factory/Preview/NavEntry/BuildConfig), Swift `Pure`
70 % (`llvm-cov` по всем `*Tests.so` через `coverage-floor.sh`). Логика намеренно сосредоточена в
`shared` — там пол выше, UI-слои держат Roborazzi/snapshot.

## Reference-фичи {#reference}

| # | фича | `shared/` | `androidApp/` | `iosApp/` | что демонстрирует | `/de-template` |
|---|---|---|---|---|---|---|
| 1 | **`demo`** — mock-режим | `core/mock` (`MockTransport`, `MockScenario`, `DemoMode`, `DemoGate`, `DemoStorage`, `DemoControl`), `feature/demo/DemoPanelPresenter`; фасад — `LoginIntent.EnterDemo` + `LoginState.demoAvailable`, `ShellIntent.LeaveDemo` + `ShellState.demo` | flavor `demo` (`.demo`, `API_MODE=mock`); в обычной сборке — кнопка demo-входа на `LoginScreen` при `demoAvailable` и `DemoBanner` (`core:ui`) поверх контента; панель сценариев — секция `SettingsScreen` при `DemoPanelState.available` (только `API_MODE=mock`); `DemoFlowTest`, эталоны `login_demo`, `shell_demo` | схема `Demo`, `DemoBanner` в `Core/DesignSystem`, `LoginView`/`ShellModel`/`RootView`, `DemoPanelSection` в `SettingsView`, `Scenario.container` в тестах | одна фикстурная база для тестов, demo, скриншотов, Maestro; ревьюер магазина без бэкенда и без отдельной сборки — вход открывает флаг `demo_login` из `requirements` | оставить |
| 2 | `shell` | `feature/shell/ShellPresenter` (auth-gate, offline, pending route) | `AppNavigation` (`NavDisplay`, back stack), `ShellRoute`/`ShellScreen`/`ShellTab` (табы `Todos/Profile/Settings` при `SignedIn`, `OfflineBanner`, `LockedState` биометрии), edge-to-edge | `RootView` (`TabView`, `NavigationStack` на таб, `Router`), `ShellModel`, `RequirementsOverlayView` | старт как единый сценарий: demo → кэши → session → route; requirements — свой презентер | оставить |
| 3 | `requirements` | `core/requirements`, `feature/requirements/RequirementsPresenter` | `RequirementsViewModel` (`feature/shell`), оверлеи `core:ui/BlockingScreens.kt` (`UpdateRequired`, `Maintenance`), `UpdateRecommendedBanner` | `RequirementsModel`, `RequirementsOverlayView` (`Features/Shell`) | `GET /api/app/requirements`: `min_version`, maintenance, флаги, `links` | оставить |
| 4 | `auth` | `entity/{session,device}`, `feature/auth/{Login,Sessions,DeleteAccount}Presenter`, `core/auth` | `Login/Sessions/DeleteAccount{Route,Screen,ViewModel}`; биометрия — `BiometricUnlock` (`AUTH_BIOMETRIC_UNLOCK`, замок `ShellViewModel.unlocked`) | `Login/Sessions/DeleteAccount{View,Model}` (биометрии нет) | Bearer + refresh, `SecureStore`, удаление аккаунта, сессии | оставить при `auth.md#mode ≠ none` |
| 5 | `profile` | `feature/profile/ProfilePresenter` (`entity/user` остаётся — его читают `shell` и `auth`) | `Profile{Route,Screen,ViewModel}`: форма, смена пароля, кнопка `Refresh`, состояния | `Profile{View,Model}` | форма с валидацией, оптимистичное обновление, `ErrorModel.code → MessageKey` | **удалить** |
| 6 | `todos` | `entity/todo`, `feature/todos/{TodoList,TodoDetail}Presenter`, `Route.TodoList`/`TodoDetail` в `core/deeplink` | `TodoList/TodoDetail{Route,Screen,ViewModel}`, `Destination.TodoList` — стартовый корень стека, deep link `/todos/{id}`, pending route, `Idempotency-Key`, offline read-only | `TodoList/TodoDetail{View,Model}`, `NavigationLink(value: AppRoute.todoDetail)`, Universal Links | CRUD-образец, deep links end-to-end, идемпотентность | **удалить** (инфраструктура deep links остаётся) |
| 7 | `settings` | `feature/settings/SettingsPresenter` (`ThemeMode`, `AppLanguage`, уведомления, версия/`APP_ENV`, `links` из requirements), `Settings`/`SettingsKeys` | `Settings{Route,Screen,ViewModel}` (язык — `AppCompatDelegate.setApplicationLocales`), «О приложении» с правовыми ссылками из `requirements.links`; доступен до логина (`LoginScreen.onOpenSettings`) | `Settings{View,Model}` (язык — системные per-app настройки, своего переключателя нет), ссылки `sessions`/`deleteAccount` | i18n, токены, permission-flow | оставить |
| 8 | `push` | `core/push` (`PushRegistrar`, `DeviceRegistration`, `PushStatus`), `feature/push/PushOptInPresenter` | `FcmTransport` + `RuStoreTransport` по `PUSH_PROVIDERS`, `NotificationChannels`, `PushMessage`/`PushNotifier`, `PushOptIn{Route,Screen,ViewModel}` (`feature/settings`), `PUT /api/v1/devices` | `ApnsRegistrar` + `AppDelegate`, `NotificationCategories`/`NotificationDeduplicator` (`PushPayload.swift`), `PushOptIn{View,Model}` | data-first payload → deep link; провайдеры по `PUSH_PROVIDERS` | оставить при непустом `push.md#providers` |
| 9 | `background` | `core/background` (`BackgroundScheduler` + `BackgroundRunner`, `BackgroundTask`, `BackgroundResult`), `PlatformDeps.background`, `feature/todos/{RefreshTodosTask,TodoReminderTask}`, реестр `backgroundTasks` и `backgroundRunner(id)`/`todoReminderTask(...)` в `SharedContainer` | `core/platform/background/{AndroidBackgroundScheduler,BackgroundTaskWorker,BackgroundAlarmReceiver,BackgroundDelivery}`, `SCHEDULE_EXACT_ALARM` и `<receiver>` в манифесте, планирование в `StitApplication` | `Core/Platform/Background/{BgTaskScheduler,LocalNotifier,BackgroundDelivery}`, регистрация через `AppEnvironment.registerBackgroundTasks()` из `AppDelegate`, ключи `Info.plist` (`exact` → локальное уведомление, `periodic`/`opportunistic` → app refresh, иначе processing; `continuous` и `remote-event` планировщику не отдаются) | один порт на оба механизма; `timing` → механизм; `exact` как точный **показ уведомления**; отказ как `Rejected`, а не исключение; задача на запись через ключ `<slug>:<id>` | `core/background` и платформенные реализации оставить; задачи `refresh-todos` и `todo-reminder` удалить |
| 10 | `widget` | `core/widget/WidgetState`, `feature/todos/TodayTodosWidget{Snapshot,Item,Provider}` (`publish()` — срез в `<filesDir>/cache/widget-today-todos.json`), `todayTodosWidgetProvider()` | модуль `:androidApp:widget`: `TodayTodosWidget{,Receiver,Action}`, `WidgetTokens`, `GlanceWidgetRefresher` + порт `core/platform/widget/WidgetRefresher`, `WidgetUpdates` в `StitApplication`, 6 эталонов состояний | `Core/Widgets/{AppGroupContainer,TodayTodosEntry,TodayTodosSnapshotStore}`, `Platform/WidgetRefresh`, `Features/WidgetsFeature/TodayTodosWidgetView`, appex `iosApp/Widgets/` | срез вместо приложения; пять состояний включая `signed-out` и `stale`; тап — тем же https-адресом, что и deep link; расширение без сети и токена (снимок в App Group) | `core/widget`, порт, модуль и appex оставить; виджет `today-todos` удалить |
| 11 | `system-action` | `feature/todos/{CompleteTodoAction,AddTodoAction}` — публичный класс с `internal constructor`, результаты `Done`/`AlreadyDone`/`Added` · `SignedOut` · `Failed(error)`; фабрики `completeTodoAction()`/`addTodoAction()` | `core/platform/action/{CompleteTodoActionHandler,AddTodoActionHandler,NotificationActionReceiver,AppFunctions}`, кнопка уведомления в `push/PushNotifier`, кнопка строки виджета `TodayTodosWidgetAction` | `Core/SystemActions/{CompleteTodoIntent,AddTodoIntent}` + мост `Platform/{SystemActionsBridge,NotificationActions}`, `App/AppShortcutsProvider` | одна логика на все каналы; идемпотентность (`AlreadyDone`); сессия наружу не отдаётся; ответ — строка ресурса по результату | инфраструктуру каналов оставить; действия `complete-todo` и `add-todo` удалить |

Общие образцы: тест презентера на JVM с `MockTransport`; Roborazzi на состояния
`loading/empty/populated/error/offline` (+ `_dark`) на Android и snapshot light/dark (iOS, Mac);
для виджетов — Roborazzi на состояния снимка (`placeholder/populated/empty/signed-out/stale`);
Maestro-флоу `e2e/{android,ios}` на сценарии `populated`. Prune-лист —
`.claude/skills/de-template/references/prune.md`.

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
| `import Intents` резолвится в SiriKit, а не в свой таргет | системный модуль Apple занимает это имя | таргет системных действий называется `SystemActions` |
| `Shared.framework` неожиданно попал в appex виджетов | таргет расширения зависит от `Platform`/`SharedBridge` | расширение собирается из `Core/{Widgets,SystemActions}` (только Foundation) и `Features/WidgetsFeature`; контейнер приходит в приложение мостом `Platform/SystemActionsBridge` |
| `no such module 'UIKit'` в `swift pure build+tests` | `import UIKit` в `Packages/Pure` | `Pure` — только Foundation (NF-22); UIKit-код — в `Core` |
| `ExportedService` Lint на сервисе RuStore | SDK доставляет из своего процесса | `exported=true` + `tools:ignore="ExportedService"` в `core/platform` |
| `getToken()` deprecated в FCM 25.1 | новый API `register()`/`onRegistered` | `@Suppress("DEPRECATION")` в `FcmTransport`; запрос по требованию остаётся рабочим |
| `tokens-check`/`gen-tokens` падают без вывода | нет `python3` в PATH | поставить `python3` (генератор без зависимостей) |
| `Lint will treat :x as an external dependency` | JVM-модуль без `com.android.lint` | `stit.jvm.library` применяет standalone Lint |
| Konsist-тест UP-TO-DATE после нарушения | inputs только classpath | `stit.konsist` добавляет исходники репо в inputs; в `shared` — `jvmTest` получает `inputs.dir("src")` |
| Kover падает на корневом `kover(project)` | плагин не на каждом модуле | `stit.quality` применяет Kover везде |
| `resource mipmap/ic_launcher not found` после переименования | устаревшие merged-ресурсы | `--rerun-tasks` |
| Keychain «помнит» токен после удаления приложения | Keychain переживает удаление | `FirstLaunchReset` — очистка по флагу первого запуска |
| Universal Links не работают часами | Apple CDN кэширует AASA до 24 ч | `?mode=developer` в dev; каждый сабдомен — свой AASA |
| `RemoteServiceException: a foreground service of type dataSync did not stop within its timeout` | суммарный лимит `dataSync` — 6 ч на 24 ч, дальше `onTimeout()` и пара секунд на `stopSelf()` | реализовать `onTimeout`; работу без видимого пользователю хода вести WorkManager'ом, не FGS |
| Напоминание приходит не в обещанную минуту | исполнение кода в фоне точнее окна не бывает; с Android 14 `SCHEDULE_EXACT_ALARM` по умолчанию не выдано, и планировщик уходит на неточный будильник (`canScheduleExactAlarms`) | точный будильник показывает **уведомление**, данные берутся из снимка кеша; расхождение закрывает `fallback` записи карты |
| `BackgroundTaskWorker` падает с `Result.failure()` сразу | `inputData` без ключа `task_id` или `backgroundRunner(id)` не знает такого id | ключ задачи — её `slug` (у задачи на запись — `<slug>:<id>`), ветка в `SharedContainer.backgroundRunner` обязательна |
| appex виджетов не описывается в `Package.swift` | SwiftPM не умеет bundle-таргеты (`.app`, `.appex`), entitlements и подпись | таргет — в `iosApp.xcodeproj` (Mac), код — в пакетах `Core/Widgets` + `Features/WidgetsFeature` |
| Виджет держится в `placeholder` после установки | снимок в общем каталоге ещё не записан | `WidgetUpdates` публикует срез на каждое изменение репозитория, фоновая задача и действия — после мутации; `stale` рисуется по возрасту снимка и только без сети |
| Lint `ResourceName` на `<slug>_widget_info.xml` | у библиотечного модуля `resourcePrefix` из пути (`widget_`) | имя ресурса — `widget_<slug>_info.xml`; то же для строк `widget_<slug>_*` |
| Glance-виджет нечем накрыть Roborazzi | `GlanceAppWidget` не рисуется как обычный композабл | `compose()` → `RemoteViews.apply()` → `Bitmap` → `captureRoboImage` (`glance-appwidget-testing`, `@GraphicsMode(NATIVE)`) |
| `StitAppFunctionService` не поднимается на устройстве | AppFunctions — платформенный API Android 16; ниже система реестр не читает | `@RequiresApi(36)` + `tools:targetApi="36"`; на JVM сервис не тестируется (Robolectric пинит `sdk=35`), поэтому в нём только делегирование в обработчик |
| App Links «verified: false» на части сборок | один отпечаток в `assetlinks.json` | все отпечатки (upload, Play, RuStore) массивом |
| Kotlin-генератор кладёт токен в query | `in: cookie` не поддерживается | models-only + рукописный `AuthPlugin` |
| RuStore публикация 4xx | JWE-токен живёт 900 с, `timestamp ± 60 с` | перевыпустить токен |
| `sdkmanager` предупреждает | deprecated в cmdline-tools 23 | `android` CLI с `--no-metrics` |

## Где что лежит

| что | где |
|---|---|
| версии и SDK-уровни | `gradle/libs.versions.toml` (единственное место) |
| токены дизайна | `design/tokens.json` → `make gen-tokens` → `androidApp/core/designsystem/…/generated/Tokens.kt`, `iosApp/Packages/Pure/Sources/DesignSystemTokens/Tokens.swift` |
| конфиг гейта | `config/detekt/detekt.yml`, `config/lint/lint.xml`, `.editorconfig`, `iosApp/.swiftlint.yml`, `iosApp/.swift-format` |
| вербы | `Makefile` (группы `##@ LOOP / GATE / KMP / CONTRACT / SWIFT (Linux-часть) / MAC / RELEASE / SETUP / INTEL / META`) |
| build-time env | `config/env/<flavor>.properties`, `iosApp/Config/<Env>.xcconfig` |
| контракт API | `api/openapi.yaml` + `api/openapi.lock`, `config/openapi/kotlin-models.yaml` |
| фикстуры | `mock/scenarios.yaml`, `mock/fixtures/<scenario>/<entity>.json` → `Fixtures.kt` (`:shared:embedFixtures`, `shared/build/generated`) |
| эталоны | `androidApp/**/src/test/screenshots/`, `iosApp/**/__Snapshots__/` |
| реестр фоновых задач и таблица «`timing` → механизм» | `docs/stack/background.md` |
| снимки виджетов | `<PlatformDeps.filesDir>/cache/widget-<slug>.json` (`FileCache`, JSON: enum по имени, `Instant` ISO-8601); на iOS каталог — контейнер App Group из `APP_GROUP_ID` |
| e2e | `e2e/android/*.yaml`, `e2e/ios/*.yaml` (`make e2e P=android|ios`) |
| логи гейта | `build/gate-logs/<стадия>.log`, `check.last`, `mac.last` |
| план | `.claude/plans/roadmap.md` |
| карта продукта | `<strategy-path>/docs/projects/<slug>/mobile/<surface>/` (read-only) |
