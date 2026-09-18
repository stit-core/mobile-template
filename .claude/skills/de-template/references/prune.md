# de-template › prune reference-фич

`profile` и `todos` — учебные reference-фичи (CRUD, deep link, идемпотентность, форма с
валидацией). Удаляются на де-темплейтизации **после** того, как свои фичи спроектированы в
`<org>/strategy` и скаффолжены (`/scaffold`) — иначе теряешь живой образец в трёх местах.
`demo`, `shell`, `requirements`, `auth`, `settings`, `push` — инфраструктурные, остаются
(`auth` — при `auth.md#mode ≠ none`, `push` — при непустом `push.md#providers`). Поверхности вне
экранов (`background`, `widget`, `system-action`) двухслойны: платформа остаётся, а задачи, виджет
и действие на `todo` уходят вместе с фичей (точка 9).
Недоудаление оставит висящие импорты, маршруты и фикстуры.

## Чеклист (9 точек)

1. **`shared/`** — `shared/src/commonMain/kotlin/<pkg>/{entity/todo,feature/todos,feature/profile}`
   + их `commonTest` (`TodoPresentersTest`, `ProfilePresenterTest`, части `RepositoriesTest`/`MappersTest`);
   варианты `Route.TodoList/TodoDetail/Profile` в `core/deeplink/Route.kt` (+ `requiresAuth`) и ветки
   `DeepLinkParser` + тесты; фабрики `todoListPresenter/todoDetailPresenter/profilePresenter` и
   `todoRepository` (+ `onSignedOut`) в `SharedContainer`; клиент `core/api/TodosApi.kt`.
   `entity/user` и `UsersApi` **остаются** — их читают `shell` (`restore`/`clear`) и `auth`
   (`DeleteAccountPresenter`). Затем `make api-dump` (фасад изменился — по этому решению).
2. **`androidApp/`** — модули `androidApp/feature/{todos,profile}` (`git rm -r`) + строки
   `include(...)` в `settings.gradle.kts` + `implementation(projects.androidApp.feature.{todos,profile})`
   в `androidApp/app/build.gradle.kts`; `Destination.TodoList/TodoDetail/Profile` и ветки
   `Route.toDestination()` в `core/navigation/Destination.kt`; в `app/AppNavigation.kt` —
   `todosEntries`/`profileEntries`, стартовый корень `rememberNavBackStack(Destination.TodoList)`,
   `ShellTab.Todos/Profile` (`feature/shell/ShellTab.kt`); `DemoFlowTest` (ходит по задачам);
   строки `feature_todos_*`/`feature_profile_*` в `res/values{,-ru}/strings.xml` (`i18n-check`).
3. **`iosApp/`** — `iosApp/Packages/Features/Sources/{TodosFeature,ProfileFeature}` (+ их
   `Tests/*FeatureTests`), имена в `featureNames` (`Packages/Features/Package.swift`) и
   `@_exported import` в `Sources/Features/Features.swift`; case `todoList/todoDetail/profile` в
   `Core/Routing/AppRoute.swift` + `AppTab.todos/profile`, ветки `RouteBridge`, `Router.open`;
   вкладки и `destination` в `App/RootView.swift`, фабрики в `AppEnvironment`; `@Test` todos в
   `Tests/SnapshotTests/ScreenSnapshotTests.swift` + `__Snapshots__`; строки в `Localizable.xcstrings`
   таргетов; пути `/todos/*` в AASA (INFRA-HANDOFF, если хост остаётся).
4. **e2e и фикстуры гейта** — `e2e/android/todos.yaml`; `.gate-fixtures/NF-13` (`feature/todos/PickerPresenter.kt`),
   `NF-17` (`feature/todos` strings), `NF-21` (`AuthFeature → TodosFeature`), `NF-23`
   (`TodosFeature/BadNetwork.swift`), NF-25 (идентификатор `refresh-todos` в `App/Info.plist`),
   NF-26 (эталон `widget_today_todos_stale.png`), NF-27 (строка
   `core_platform_action_complete_todo_signed_out` в `values-ru`) — перенацелить на оставшиеся
   модули/пакеты и на свою задачу, виджет и действие (например,
   `feature/settings`, `SettingsFeature`), самотест обязан остаться красным; `pathPrefix`
   `/todos`, `/profile` в манифесте и строки `DeepLinkParserTest` — убрать парой (`deeplinks-check`).
5. **`api/openapi.yaml` — не трогать** (копия контракта бэкенда): удалённые фичи перестают
   вызывать операции; регенерировать модели `make gen` — неиспользуемые модели допускаются.
6. **Фикстуры mock** — `mock/fixtures/*/todos.json` (`user`/`login`/`session`/`devices`/`requirements`
   остаются — база demo-режима), маршруты `todos` в `MockTransport`, поле `todos` в `MockState.load`,
   `entities:` в `mock/scenarios.yaml`, модель в `FixturesTest`; `make mock-check` зелёный.
7. **Docs** — `docs/stack/*.md`: примеры `todos`/`profile` → проектные значения;
   `docs/reference/strategy-mapping.md` — таблица reference-соответствий очищается;
   `docs/reference/architecture.md#reference` — список оставшихся reference-фич;
   `docs/stack/deep-links.md` — строка `/todos/{id}` удалена (`deeplinks-check` сверяет с тестами).
8. **Konsist и Lint** — `androidApp/app/src/test/.../ArchitectureTest.kt` не ссылается на
   удалённые модули (правила общие — обычно без правок); `:androidApp:lint` — детекторы без
   привязки к фичам.
9. **Поверхности вне экранов** (инфраструктура остаётся, `todo`-специфика уходит): в `shared` —
   `feature/todos/{RefreshTodosTask,TodoReminderTask,TodayTodosWidgetSnapshot,TodayTodosWidgetProvider,CompleteTodoAction,AddTodoAction}`
   (+ их `*Result`) и `commonTest/…/feature/{TodosTasksTest,TodosWidgetsTest,TodosActionsTest}.kt`;
   строка реестра `backgroundTasks`, ветки `backgroundRunner(id)` и фабрики (`todoReminderTask`,
   `todayTodosWidgetProvider`, `completeTodoAction`, `addTodoAction`) в `SharedContainer`,
   планирование реестра и `widgetUpdates.start` в `StitApplication`; в
   `:androidApp:widget` — `TodayTodosWidget{,Receiver,Action}`, ветка slug в
   `GlanceWidgetRefresher`, `res/xml/widget_today_todos_info.xml`, `<receiver>` манифеста, строки
   `widget_today_todos_*` (оба `values`), тесты (`TodayTodosWidget*Test`, `WidgetRefreshTest`,
   `WidgetFixtures`) и 6 эталонов `src/test/screenshots/widget_today_todos_*`; в `core:platform` —
   `action/{CompleteTodoActionHandler,AddTodoActionHandler}` с тестами, функция `addTodo` в реестре
   `BaseStitAppFunctionService`, строки `core_platform_action_{complete_todo,add_todo}_*`, кнопка
   категории в `PushNotifier`, канал `NotificationChannels.TODO_REMINDER` со строками
   `core_platform_channel_todo_reminder_*` и ключи `push.todo.reminder.*` в `PushMessage.TEXTS`
   (+ строки `core_platform_push_todo_reminder_*`); в `iosApp` —
   `Core/SystemActions/{CompleteTodoIntent,AddTodoIntent}`, `Core/Widgets/TodayTodosEntry`,
   `Features/WidgetsFeature/TodayTodosWidgetView` (+ `@main` appex), строки в
   `App/AppShortcutsProvider.swift`; в стеке — строки задач в `docs/stack/background.md`,
   локальная категория и кнопка в `push.md`, `BGTaskSchedulerPermittedIdentifiers` в `Info.plist`.
   `SCHEDULE_EXACT_ALARM` остаётся, только если своя задача `timing: exact` есть; реестр
   AppFunctions и его `<service>` — только если остаётся канал `voice`/`automation` (иначе уходят
   вместе с зависимостью и строкой исключения в `dependencies.md`). Модуль
   `:androidApp:widget` с портом `WidgetRefresher`, `WidgetUpdates`, `WidgetTokens` и
   `WidgetGraph`, appex и пакеты `Core/{SystemActions,Widgets}` остаются каркасами под первый свой
   виджет; `make capability-check` зелёный на пустом реестре.

## Что НЕ удалять

- `core/deeplink` (инфраструктура deep links остаётся без маршрутов `todos`).
- `core/background` и `core/widget` + `PlatformDeps.background`, `background/{AndroidBackgroundScheduler,
  BackgroundTaskWorker,BackgroundAlarmReceiver,BackgroundDelivery}`, `NotificationActionReceiver`,
  `BgTaskScheduler`/`LocalNotifier` — платформа поверхностей вне экранов, не reference.
- `:androidApp:widget` (порт `WidgetRefresher`, `GlanceWidgetRefresher`, `WidgetUpdates`,
  `WidgetTokens`, `WidgetGraph`), appex `iosApp/Widgets/`, `Core/{SystemActions,Widgets}`,
  `Features/WidgetsFeature` — каркасы (удаление таргета — правка `.pbxproj` на Mac).
- `entity/{user,session,device}`, `core/*` — платформа и `auth`/`shell`, не reference.
- `docs/stack/_templates/` — формы kinds для будущих артефактов.
- `mock/scenarios.yaml` и фикстуры `session` — база demo-режима.

## Verify

```bash
make gen && make mock-check     # модели/фикстуры без удалённых сущностей
make api-dump && make api-check # фасад без Todo*/Profile*
make capability-check           # пустой реестр задач и виджетов согласован
make check                      # нет висящих импортов, скриншотов, строк
make gate-selftest              # фикстуры перенацелены, все красные
git grep -nE 'Todo|todos|Profile|today.todos|todo.reminder' -- shared androidApp iosApp mock e2e docs/stack   # пусто (кроме --keep и generated/ — модели спеки допустимы)
```
