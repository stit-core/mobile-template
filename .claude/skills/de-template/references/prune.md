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
   в `androidApp/app/build.gradle.kts`; их `src/test/screenshots/`; `NavEntry`-регистрации в
   `app`; строки `feature_todos_*`/`feature_profile_*` в `res/values*/strings.xml`.
3. **`iosApp/`** — `iosApp/Packages/Features/Sources/{TodosFeature,ProfileFeature}` +
   `__Snapshots__`, таргеты в `Packages/Features/Package.swift` и umbrella `Features`;
   ключи `todos.*`/`profile.*` в `Localizable.xcstrings`; пути `/todos/*` в `applinks`/AASA
   (INFRA-HANDOFF, если хост остаётся).
4. **e2e и фикстуры гейта** — `e2e/{common,android,ios}/{todos,profile}-*.yaml`;
   `.gate-fixtures/NF-*`, ссылающиеся на удалённые модули/пакеты, — перенацелить на
   оставшиеся (например, `feature/settings`), самотест обязан остаться красным.
5. **`api/openapi.yaml` — не трогать** (копия контракта бэкенда): удалённые фичи перестают
   вызывать операции; регенерировать модели `make gen` — неиспользуемые модели допускаются.
6. **Фикстуры mock** — `mock/fixtures/*/todos.json` (`user`/`login`/`session`/`devices`/`requirements`
   остаются — база demo-режима), маршруты `todos` в `MockTransport`, поле `todos` в `MockState.load`,
   `entities:` в `mock/scenarios.yaml`, модель в `FixturesTest`; `make mock-check` зелёный.
7. **Docs** — `docs/stack/*.md`: примеры `todos`/`profile` → проектные значения;
   `docs/reference/strategy-mapping.md` — таблица reference-соответствий очищается;
   `docs/reference/architecture.md#reference` — список оставшихся reference-фич;
   `docs/stack/deep-links.md` — строка `/todos/{id}` удалена.
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
