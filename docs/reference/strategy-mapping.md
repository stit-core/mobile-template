# Карта `mobile/<surface>/` → код

Маппинг двенадцати kinds карты приложения (слой `mobile/<surface>/` в `<org>/strategy`, канон —
[`strategy-mobile-canon/README.md`](strategy-mobile-canon/README.md)) на три места кода этого
репо. Потребляют `/scaffold` (по вариантам) и `/de-template`; резолв соседа — `strategy-path` /
`project-slug` / `surface` в `docs/stack/service.md` (дефолт `../strategy`). Контракт read-only и
односторонний: расхождение кода с картой — STRATEGY-HANDOFF, не правка карты отсюда.

`<pkg>` — `tech.stit.mobile` (в форке — `<reverse-domain>.<app>`); `<Key>` — PascalCase ключ экрана,
`<Group>`/`<group>` — группа экранов (`auth`, `todos`, `profile`, `settings`, `shell`), `<Slug>` —
PascalCase из `slug` записи.

## Kinds → пути

| kind → | `shared/src/commonMain/kotlin/<pkg>/` | `androidApp/` | `iosApp/` | прочее |
|---|---|---|---|---|
| entity `<slug>` | `entity/<slug>/{<Slug>,<Slug>Repository,<Slug>Mapper}.kt` + `commonTest/…/entity/{Repositories,Mappers}Test.kt` | — | — | `mock/fixtures/*/<slug>.json`, маршруты `MockTransport` |
| screen `<Key>` (группа `<group>`) | `Route.<Key>` в `core/deeplink/Route.kt`; `feature/<group>/{<Key>Presenter,<Key>State,<Key>Intent}.kt` | `core/navigation/Destination.kt` (`Destination.<Key>`, `Route.toDestination()`); `feature/<group>/…/{<Key>Route,<Key>Screen,<Key>ViewModel}.kt`, `<Group>NavEntry.kt` (`<group>Entries`), `<Group>Previews.kt`, `<Group>TestTags.kt`; тесты `<Key>ScreenTest.kt`, `<Group>ScreenshotTest.kt` → `src/test/screenshots/<key>_<state>[_dark].png`; `res/values{,-ru}/strings.xml` (`feature_<group>_*`) | `Packages/Features/Sources/<Group>Feature/{<Key>View,<Key>Model}.swift` (`Shell` — `Sources/Shell/`), `Resources/Localizable.xcstrings` (`<group>.<key>.*`); `Core/Routing/AppRoute.swift` + `SharedBridge/RouteBridge.swift`; `App/AppEnvironment.<key>Model()`, `App/RootView.swift`; тесты `Tests/<Group>FeatureTests/<Key>ModelTests.swift`, `Tests/SnapshotTests/ScreenSnapshotTests.swift` + `__Snapshots__/` | — |
| flow `<slug>` | `commonTest/…/feature/<Group>PresentersTest.kt` (сценарий интентов; `ShellPresenterTest` — app-start) | `app/src/test/…/DemoFlowTest.kt` (Robolectric + Hilt, demo-сборка) | — | `e2e/android/<slug>.yaml`, `e2e/ios/<slug>.yaml` (Maestro, `make e2e`) |
| feature `<slug>` | `feature/<group>/{<Key>Presenter,<Key>State,<Key>Intent}.kt` + `commonTest`; фабрика `<key>Presenter()` в `SharedContainer` | модуль `:androidApp:feature:<group>` (`stit.android.feature`) | имя в `featureNames` (`Packages/Features/Package.swift`) + `@_exported import` в `Sources/Features/Features.swift` | `shared: false` — только платформенные части |
| shell | `feature/shell/ShellPresenter.kt` (+ `feature/requirements/RequirementsPresenter.kt` — оверлеи) | `app/…/AppNavigation.kt` (`NavDisplay`, back stack), `feature/shell/{ShellRoute,ShellScreen,ShellTab,ShellViewModel,RequirementsViewModel}.kt`, оверлеи `core/ui/BlockingScreens.kt` | `App/RootView.swift` (`TabView`, `NavigationStack` на таб), `Core/Routing/{AppRoute,Router}.swift`, `Features/Sources/Shell/{ShellModel,RequirementsModel,RequirementsOverlayView}.swift` | — |
| entry-point `<slug>` | `core/deeplink/DeepLinkParser.kt` + `DeepLinkParserTest` (`deep_link` из push тоже идёт сюда через `ShellIntent.OpenLink`) | `app/src/main/AndroidManifest.xml` (intent-filter, `${DEEP_LINK_HOST}`/`${DEEP_LINK_SCHEME}`, `pathPrefix`); `core/platform/…/push/{NotificationChannels,PushMessage,PushNotifier}.kt` | `App/App.entitlements` (`applinks:$(DEEP_LINK_HOST)`), `App/Info.plist` (`CFBundleURLSchemes`); `Core/Sources/Platform/PushPayload.swift` (`NotificationCategories`, `NotificationDeduplicator`) | `docs/stack/deep-links.md` (стадия `deeplinks-check`) / `push.md`; INFRA-HANDOFF I-1 (`.well-known`) |
| permission `<slug>` | интерфейс в `PlatformDeps.kt` (образец — `core/push/NotificationPermission`) | `core/platform/…/<Slug>PermissionGate.kt` (образец `NotificationPermissionGate`), `<uses-permission>` в `core/platform/src/main/AndroidManifest.xml`, pre-prompt-экран (`feature/settings/PushOptInScreen.kt`) | `Core/Sources/Platform/<Slug>Permission.swift` (образец `NotificationCenterPermission`), `Info.plist` usage-key, `Features/…/PushOptInView.swift` | `docs/stack/compliance.md` |
| system-action `<slug>` | `feature/<group>/<Slug>Action.kt` — `class <Slug>Action internal constructor(deps) { suspend fun run(...): <Slug>Result }` (публичный класс, конструктор `internal`; оркестрация фичи без `StateFlow` и UI, `run` не бросает; `<Slug>Result` — `sealed` с исходом-успехом, идемпотентным исходом, `SignedOut` и `Failed(error: MessageKey)`; офлайн → `Failed(MessageKey.NetworkError)` без запроса; после успешной мутации действие само зовёт `publish()` снимка виджета), фабрика `<slug>Action()` в `SharedContainer`; `mode: opens-app` — своего кода нет, только существующий `Route.<Key>`; тесты `commonTest/…/feature/<Group>ActionsTest.kt` | `core/platform/…/action/{<Slug>ActionHandler,NotificationActionReceiver,AppFunctions}.kt` (+ `ActionEntryPoint`/`AppFunctionEntryPoint`: обработчики зовёт система, граф берётся Hilt EntryPoint'ом); канал `notification:<slug>` — `NotificationCompat.Action` в `push/PushNotifier.kt` + `<receiver>` манифеста; `widget:<slug>` — `ActionCallback` в `:androidApp:widget`; `voice`/`automation` — `BaseStitAppFunctionService` с `@AppFunction`, KSP генерирует сервис и XML реестра, манифест объявляет их с `BIND_APP_FUNCTION_SERVICE` и `tools:targetApi="36"`; строки `core_platform_action_<slug>_*` в `res/values{,-ru}/strings.xml` | `Packages/Core/Sources/SystemActions/<Slug>Intent.swift` (`perform()` → `<slug>Action()`, `openAppWhenRun` при `opens-app`, `authenticationPolicy` из «на заблокированном экране»), `App/AppShortcutsProvider.swift` (`phrases`, только канал `voice`); `notification:<slug>` — `Core/Platform/PushPayload.swift` (`NotificationCategories`); строки `action.<slug>.*` в `Localizable.xcstrings` | `docs/stack/push.md` (кнопки категории), `compliance.md` (что доступно без разблокировки), `dependencies.md` (AppFunctions + исключение политики версий); `make api-dump` |
| widget `<slug>` | `core/widget/WidgetState.kt` (`Placeholder`/`Populated`/`Empty`/`SignedOut`/`Stale` — словарь `states`); `feature/<group>/{<Slug>WidgetSnapshot,<Slug>WidgetProvider}.kt` — `@Serializable` срез (`state`, `items`, `generatedAt`, `tapUrl`) + `suspend fun snapshot()`/`publish()` и `companion { SLUG }`; снимок пишется `FileCache` в `<filesDir>/cache/widget-<slug>.json` (на iOS каталог — контейнер App Group, оттуда его читает расширение); `Stale` — старше порога свежести **и** нет сети; фабрика `<slug>WidgetProvider()` в `SharedContainer`; тесты `commonTest/…/feature/<Group>WidgetsTest.kt` — строка на каждое состояние из `states` | модуль `:androidApp:widget` (`stit.android.{library,compose,hilt}` + `stit.roborazzi` + `glance-appwidget`): `<Slug>Widget.kt` (`GlanceAppWidget`; тап — `actionStartActivity(ACTION_VIEW, snapshot.tapUrl)` с `setPackage`), `<Slug>WidgetReceiver.kt`, `WidgetTokens.kt`, `GlanceWidgetRefresher.kt` + порт `core/platform/…/widget/WidgetRefresher.kt` (`refresh(slug)`; зовут `BackgroundDelivery` и `WidgetUpdates`), `WidgetUpdates.kt` (`on-app-use`, старт в `StitApplication`), `WidgetGraph.kt`, `res/xml/widget_<slug>_info.xml` (префикс модуля — Lint `ResourceName`), `<receiver>` в `src/main/AndroidManifest.xml`, строки `widget_<slug>_*`; тесты `<Slug>WidgetTest.kt`, `WidgetRefreshTest.kt` + Roborazzi через `compose()` → `RemoteViews` → `Bitmap` → `src/test/screenshots/widget_<slug>_<state>[_dark].png` | `Packages/Core/Sources/Widgets/<Slug>Entry.swift` (чтение снимка из App Group; сети и токена в расширении нет), `Packages/Features/Sources/WidgetsFeature/<Slug>WidgetView.swift`, appex-таргет `iosApp/Widgets/` + App Group в `App.entitlements` (Mac); снапшоты `__Snapshots__/` | `docs/stack/compliance.md` (данные на экране блокировки), `dependencies.md` (Glance), `service.md`/`config.md` (App Group id, bundle id расширения); `tap` — парная запись `entry-points` `kind: widget` |
| background-task `<slug>` | `core/background/{BackgroundScheduler,BackgroundTask,BackgroundResult}.kt` (`BackgroundScheduler.schedule(task)`/`cancel(id)` — не `suspend`; `BackgroundRunner.run(): BackgroundResult` — там же; `BackgroundTask(id, trigger, timing, conditions, periodMinutes, at)` + `BackgroundTrigger`/`BackgroundTiming`/`BackgroundCondition`; `BackgroundResult` = `Notify(category, titleKey, bodyKey, args, deepLink, notificationId)` \| `RefreshWidget(slug)` \| `FreshData` \| `Rejected(BackgroundRejection)`) + `PlatformDeps.background`; `feature/<group>/<Slug>Task.kt : BackgroundRunner` с `companion` (`ID`/`SLUG`, `TASK` или `task(...)`), подъём — `SharedContainer.backgroundRunner(id)`, реестр — `backgroundTasks`; тесты `commonTest/…/feature/<Group>TasksTest.kt` (результат · `Rejected` · offline) на `FakeBackgroundScheduler` (`testing/Fakes.kt`) | `core/platform/…/background/{AndroidBackgroundScheduler,BackgroundTaskWorker,BackgroundAlarmReceiver,BackgroundDelivery}.kt` (механизм по `timing` — `docs/stack/background.md`; вход воркера — `task_id`), `<receiver>` и `<uses-permission android:name="…SCHEDULE_EXACT_ALARM">` в `core/platform/src/main/AndroidManifest.xml`, планирование реестра — `StitApplication`; `Notify` — локальный показ через `push/{PushMessage,PushNotifier}`; `RefreshWidget` — обновление виджета модуля `:androidApp:widget` | `Packages/Core/Sources/Platform/Background/{BgTaskScheduler,LocalNotifier}.swift`, регистрация в `App/AppDelegate.swift`, ключи `BGTaskSchedulerPermittedIdentifiers` и `UIBackgroundModes` в `App/Info.plist` | `docs/stack/background.md` (таблица «timing → механизм», идентификаторы), `compliance.md` (точные будильники, тип foreground service, background modes), `push.md` (локальные категории), `dependencies.md` (WorkManager); `make api-dump` |
| shared-api ресурс | `core/api/<Resource>Api.kt` (+ сгенерированные модели) | — | — | `api/openapi.yaml` (проверка операций), `mock/fixtures/*/<resource>.json` |
| shared-lib | `core/i18n/MessageKey.kt` (`KnownErrorCodes`), `core/requirements/AppRequirements.kt` (`flags`, `flag(name)`), `core/observability` | `res/values{,-ru}/strings.xml` каждого модуля (`i18n-check`: паритет ключей, `core_ui_error_<code>`) | `Resources/Localizable.xcstrings` каждого таргета (`error.<code>` — в `DesignSystem`; advisory `i18n-check`) | — |

## Идентификаторы

| в карте | в коде |
|---|---|
| `key` экрана `TodoDetail` | `Route.TodoDetail`, `Destination.TodoDetail`, `AppRoute.todoDetail`, `TodoDetailScreen.kt`, `TodoDetailView.swift`, `todo_detail_populated.png` |
| `slug` фичи `delete-account` | `feature/auth/DeleteAccountPresenter.kt` в `shared` (пакет — группа экранов); на платформах — экраны внутри модуля группы (`androidApp/feature/auth`, `Features/AuthFeature`) |
| `slug` сущности `todo` | `entity/todo/Todo.kt`, `TodoRepository`, `todo.json` |
| push-категория `ORDER_STATUS` | `channel = "order_status"` (`PushMessage.parse` → `NotificationChannels`), `UNNotificationCategory("ORDER_STATUS")` (`NotificationCategories`); `deep_link` → `DeepLinkParser` |
| `slug` системного действия `add-todo` | `feature/todos/AddTodoAction.kt` + `AddTodoResult` (shared), `AddTodoActionHandler` и функция `addTodo` реестра AppFunctions (Android), `AddTodoIntent.swift` (iOS), строки `core_platform_action_add_todo_{added,signed_out,failed}` / `action.add-todo.*` |
| `slug` виджета `today-todos` | `feature/todos/TodayTodosWidget{Snapshot,Provider}.kt` (shared, `SLUG = "today-todos"`), `TodayTodosWidget{,Receiver,Action}.kt` + `res/xml/widget_today_todos_info.xml` и строки `widget_today_todos_*` (`:androidApp:widget`), `TodayTodosEntry.swift`/`TodayTodosWidgetView.swift` (iOS), снимок `<filesDir>/cache/widget-today-todos.json`, эталоны `widget_today_todos_<state>[_dark].png` |
| `slug` фоновой задачи `refresh-todos` | `feature/todos/RefreshTodosTask.kt` (shared, `ID = "refresh-todos"`), тот же id — ключ уникальной работы WorkManager и значение `task_id`, BG-идентификатор `<bundle-id>.refresh-todos` (iOS) |
| фоновая задача на конкретную запись (`todo-reminder`) | `TodoReminderTask` с `ID_PREFIX = "todo-reminder:"`: ключ планировщика — `todo-reminder:<todoId>`, `backgroundRunner(id)` достаёт id записи из ключа |
| канал `widget:<slug>` / `notification:<slug>` | `ActionCallback` строки виджета / `NotificationCompat.Action` → `NotificationActionReceiver` (Android), `UNNotificationAction` в `NotificationCategories` (iOS); обе ветки идут в один `<Slug>ActionHandler` и дальше в `<Slug>Action` |
| канал `voice` / `automation` | `@AppFunction`-функция реестра `BaseStitAppFunctionService` (Android; KSP генерирует `StitAppFunctionService` и `assets/stit_app_functions.xml`), `<Slug>Intent` + `AppShortcutsProvider` (iOS) |
| `response` системного действия | строка ресурса по ветке результата: `core_platform_action_<slug>_{<успех>,signed_out,failed}` — ассистенту возвращается текстом, каналу без ответа показывается уведомлением |
| `result: notification:<slug>` фоновой задачи | `BackgroundResult.Notify` собирается в `PushMessage.parse` тем же payload'ом, что push: категория (`TODO_REMINDER`) → канал `NotificationChannels` (`todo_reminder`), `titleKey`/`bodyKey` → реестр `PushMessage.TEXTS`; парная запись `entry-points` `push` с `trigger: background:<slug>` — бэкенда и `PushRegistrar` в цепочке нет |
| `states` экрана | поля `data class <Key>State`: `loading`, пустой список, данные, `error: MessageKey?`, `offline`, … |
| `states` виджета | `WidgetState` (`core/widget`) в снимке + эталон на каждое состояние |

Gradle-модуль, SwiftPM-таргет и пакет `shared/…/feature/<group>/` заводятся на **группу экранов**
(reference: `shell`, `auth`, `profile`, `todos`, `settings`, `push`, `requirements`, `demo`), не на
каждую фичу карты — цена модуля выше цены пакета. Виджеты — исключение: у них своя точка входа в
ОС, свой receiver и опциональная зависимость Glance, поэтому все виджеты поверхности живут в одном
модуле `:androidApp:widget` (снимок и провайдер остаются в `shared/…/feature/<group>/`).

## Что не мапится 1:1

- `flows` desktop ↔ mobile веба — отсутствуют: у мобильной поверхности один viewport, есть
  навигационный граф (`flows.md` → back-stack).
- `pages`/FSD-`widget` как блок страницы — нет; переиспользуемые блоки живут в `core:ui` /
  `Core/DesignSystem`, адресация — `Route`, не URL. `widgets.md` карты — виджеты системы, и у них
  своя раскладка (строка выше).
- `block`-деревья экранов веб-поверхностей — нет; состояния экрана (`loading/empty/error/…`)
  переносятся как словарь `states`.
- `freshness` виджета — требование смыслом; расписания в коде нет (частоту обновлений ограничивает
  система), в код превращается порог свежести снимка и политика `refresh`.
- `shows` виджета ограничен тем, что есть в контракте: поля, которых нет в `api/openapi.yaml`
  (например срок задачи), не выдумываются — виджет показывает доступное, а недостающее поле
  становится BACKEND-HANDOFF.
- `timing: exact` — одна строка карты разворачивается в два механизма: точный **показ уведомления**
  и окно для самой работы; точного исполнения кода в фоне не обещаем.
- `conditions` (`network` / `unmetered` / `charging`) — ограничения планировщика, не поля `State`.
- `kind: live` и `kind: tile` — механизмы платформ разные (iOS — живое событие, Android —
  прогресс-уведомление и плитка быстрых настроек), `platforms: all` не значит один механизм;
  выбор — `docs/stack/background.md`.
- `phrases` и `parameters` системного действия — локализованные строки объявления, а не
  идентификаторы: фраза не становится именем в коде.
- `mode: headless` — исполнение без экрана есть только у объявленного системе действия; канал
  без такого объявления сводится к `opens-app`.

## Reference-соответствия

Таблица reference-фич шаблона ↔ записи карты (очищается `/de-template`):

| запись карты | reference-код |
|---|---|
| `entities.md`: `todo`, `user`, `session`, `device` | `entity/{todo,user,session,device}` |
| `screens.md`: `Login`, `Sessions`, `DeleteAccount`, `Profile` (форма + смена пароля), `TodoList`, `TodoDetail`, `Settings` (секции «О приложении», правовые ссылки, demo-панель), `PushOptIn` | `feature/{auth,profile,todos,settings}` (Android), `Features/{AuthFeature,ProfileFeature,TodosFeature,SettingsFeature}` (iOS) |
| `flows.md`: `app-start`, `login`, `todos` | `ShellPresenterTest`, `DemoFlowTest`, `e2e/{android,ios}/*.yaml` |
| `features.md`: `login`, `logout`, `delete-account`, `sessions`, `edit-profile`, `change-password`, `todo-*`, `switch-theme`, `switch-language`, `toggle-notifications` | `feature/*` презентеры |
| `entry-points.md`: `demo` (flavor/схема `demo` **и** кнопка demo на экране входа в обычной сборке, пока `requirements.flags.demo_login` включён), `todo-link` (`/todos/{id}`), `push`, `todo-reminder` (`TODO_REMINDER`, `trigger: background:todo-reminder`), `open-today` (`kind: widget`, `pattern: today-todos` → `TodoList`) | `core/mock` (`MockTransport`, `DemoGate`, `DemoStorage`), `DeepLinkParser`, `core/push`, `NotificationChannels.TODO_REMINDER` + `PushMessage.TEXTS`, тап виджета через тот же роутер |
| `permissions.md`: `notifications`, `exact-alarm` | `core:platform` (`NotificationPermissionGate`) + pre-prompt `PushOptIn` в `settings`; `SCHEDULE_EXACT_ALARM` в манифесте `core:platform` + проверка `canScheduleExactAlarms` в `AndroidBackgroundScheduler` |
| `background-tasks.md`: `refresh-todos` (`periodic` · `window` · `network`), `todo-reminder` (`time` · `exact`) | `core/background`, `feature/todos/{RefreshTodosTask,TodoReminderTask}`, `SharedContainer.{backgroundTasks,backgroundRunner,todoReminderTask}`, `core/platform/background/*`, планирование в `StitApplication` |
| `widgets.md`: `today-todos` (`home`; `shows` — заголовок, отметка и «обновлено», `refresh` — `on-app-use` + `background:refresh-todos`) | `core/widget/WidgetState`, `feature/todos/TodayTodosWidget{Snapshot,Item,Provider}`, `:androidApp:widget` (`TodayTodosWidget{,Receiver,Action}`, `WidgetTokens`, `GlanceWidgetRefresher`, `WidgetUpdates`, `WidgetGraph`) + порт `core/platform/widget/WidgetRefresher`, `Core/Widgets/TodayTodosEntry` + `Features/WidgetsFeature/TodayTodosWidgetView` |
| `system-actions.md`: `complete-todo` (`widget:today-todos`, `notification:todo-reminder`), `add-todo` (`voice`, `automation`) | `feature/todos/{CompleteTodoAction,AddTodoAction}` + результаты, `core/platform/action/{CompleteTodoActionHandler,AddTodoActionHandler,NotificationActionReceiver,AppFunctions}`, кнопка уведомления в `push/PushNotifier`, `TodayTodosWidgetAction`, `Core/SystemActions/{CompleteTodoIntent,AddTodoIntent}` + мост `Platform/{SystemActionsBridge,NotificationActions}` + `App/AppShortcutsProvider` |
| `shared-api.md`: `auth`, `users`, `app`, `devices`, `todos` | `core/api/*Api.kt` |
| `shared-lib.md`: `ru`/`en`, `error.<code>`, `demo_login` | `MessageKey`, `AppRequirements.flags`, ресурсы |
