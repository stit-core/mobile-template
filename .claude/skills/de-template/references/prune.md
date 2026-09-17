# de-template › prune reference-фич

`profile` и `todos` — учебные reference-фичи (CRUD, deep link, идемпотентность, форма с
валидацией). Удаляются на де-темплейтизации **после** того, как свои фичи спроектированы в
`<org>/strategy` и скаффолжены (`/scaffold`) — иначе теряешь живой образец в трёх местах.
`demo`, `shell`, `requirements`, `auth`, `settings`, `push` — инфраструктурные, остаются
(`auth` — при `auth.md#mode ≠ none`, `push` — при непустом `push.md#providers`).
Недоудаление оставит висящие импорты, маршруты и фикстуры.

## Чеклист (8 точек)

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
   (`TodosFeature/BadNetwork.swift`) — перенацелить на оставшиеся модули/пакеты (например,
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

## Что НЕ удалять

- `core/deeplink` (инфраструктура deep links остаётся без маршрутов `todos`).
- `entity/{user,session,device}`, `core/*` — платформа и `auth`/`shell`, не reference.
- `docs/stack/_templates/` — формы kinds для будущих артефактов.
- `mock/scenarios.yaml` и фикстуры `session` — база demo-режима.

## Verify

```bash
make gen && make mock-check     # модели/фикстуры без удалённых сущностей
make api-dump && make api-check # фасад без Todo*/Profile*
make check                      # нет висящих импортов, скриншотов, строк
make gate-selftest              # фикстуры перенацелены, все красные
git grep -nE 'Todo|todos|Profile' -- shared androidApp iosApp mock e2e docs/stack   # пусто (кроме --keep и generated/ — модели спеки допустимы)
```
