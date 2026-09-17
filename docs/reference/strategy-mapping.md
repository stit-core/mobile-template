# Карта `mobile/<surface>/` → код

Маппинг девяти kinds карты приложения (слой `mobile/<surface>/` в `<org>/strategy`, канон —
[`strategy-mobile-canon/README.md`](strategy-mobile-canon/README.md)) на три места кода этого
репо. Потребляют `/scaffold` (по вариантам) и `/de-template`; резолв соседа — `strategy-path` /
`project-slug` / `surface` в `docs/stack/service.md` (дефолт `../strategy`). Контракт read-only и
односторонний: расхождение кода с картой — STRATEGY-HANDOFF, не правка карты отсюда.

`<pkg>` — `tech.stit.mobile` (в форке — `<reverse-domain>.<app>`); `<Slug>`/`<Key>` — PascalCase из
`slug`/`key` записи.

## Kinds → пути

| kind → | `shared/src/commonMain/kotlin/<pkg>/` | `androidApp/` | `iosApp/Packages/` | прочее |
|---|---|---|---|---|
| entity `<slug>` | `entity/<slug>/{<Slug>,<Slug>Repository,<Slug>Mapper}.kt` + `commonTest` | — | — | `mock/fixtures/*/<slug>.json`, маршруты `MockTransport` |
| screen `<Key>` (фича `<slug>`) | `Route.<Key>` в `core/deeplink/Route.kt`; состояние — `feature/<group>/<Slug>State.kt` | `feature/<slug>/…/{<Key>Route,<Key>Screen,<Key>ViewModel,<Key>NavEntry,<Key>Previews}.kt`, `src/test/screenshots/<Key>_<state>.png`, `res/values*/strings.xml` (`feature_<slug>_*`) | `Features/Sources/<Slug>Feature/{<Key>View,<Key>Model}.swift`, `__Snapshots__/`, `Localizable.xcstrings` (`<slug>.<key>.*`) | — |
| flow `<slug>` | `feature/<…>/<Slug>FlowTest.kt` (сценарный тест презентеров) | — | — | `e2e/common/<slug>.yaml` (+ `e2e/{android,ios}/`) |
| feature `<slug>` | `feature/<group>/{<Slug>Presenter,<Slug>State,<Slug>Intent}.kt` + `commonTest`; фабрика `<slug>Presenter()` в `SharedContainer` | модуль `:androidApp:feature:<slug>` (`stit.android.feature`) | таргет `<Slug>Feature` + реэкспорт в umbrella `Features` | `shared: false` — только платформенные части |
| shell | `feature/shell/ShellPresenter.kt` (+ `feature/requirements/RequirementsPresenter.kt` — оверлеи) | `app/…/MainActivity.kt` (`NavDisplay`, табы), оверлеи в `core/ui` | `App/RootView.swift`, `Features/Sources/Shell/` | — |
| entry-point `<slug>` | `core/deeplink/DeepLinkParser.kt` + тест (`deep_link` из push тоже идёт сюда через `ShellIntent.OpenLink`) | `app/src/main/AndroidManifest.xml` (intent-filter); `core/platform/…/push/NotificationChannels.kt` | `App/App.entitlements` (`applinks:`); `Core/Sources/Platform/NotificationCategories.swift` | `docs/stack/deep-links.md` / `push.md`; INFRA-HANDOFF (`wellknown`) |
| permission `<slug>` | интерфейс в `PlatformDeps.kt` (если нужно презентеру) | `core/platform/…/permissions/<Slug>Gate.kt`, rationale-экран в `core/ui`, `<uses-permission>` | `Core/Sources/Platform/Permissions/<Slug>.swift`, `Info.plist` usage-key | `docs/stack/compliance.md` |
| shared-api ресурс | `core/api/<Resource>Api.kt` (+ сгенерированные модели) | — | — | `api/openapi.yaml` (проверка операций), `mock/fixtures/*/<resource>.json` |
| shared-lib | `core/i18n/MessageKey.kt` (`KnownErrorCodes`), `core/requirements/AppRequirements.kt` (`flags`, `flag(name)`), `core/observability` | `res/values-<lang>/strings.xml` | `Localizable.xcstrings` | — |

## Идентификаторы

| в карте | в коде |
|---|---|
| `key` экрана `TodoDetail` | `Route.TodoDetail`, `TodoDetailScreen.kt`, `TodoDetailView.swift`, `TodoDetail_populated.png` |
| `slug` фичи `delete-account` | `feature/auth/DeleteAccountPresenter.kt` в `shared` (пакет — группа экранов); на платформах — экраны внутри модуля группы (`androidApp/feature/auth`, `Features/AuthFeature`) |
| `slug` сущности `todo` | `entity/todo/Todo.kt`, `TodoRepository`, `todo.json` |
| push-категория `ORDER_STATUS` | `channel_id = "order_status"` (`NotificationChannels`), `UNNotificationCategory("ORDER_STATUS")`; `deep_link` → `DeepLinkParser` |
| `states` | поля `data class <Slug>State`: `loading`, пустой список, данные, `error: MessageKey?`, `offline`, … |

Gradle-модуль, SwiftPM-таргет и пакет `shared/…/feature/<group>/` заводятся на **группу экранов**
(reference: `shell`, `auth`, `profile`, `todos`, `settings`, `push`, `requirements`, `demo`), не на
каждую фичу карты — цена модуля выше цены пакета.

## Что не мапится 1:1

- `flows` desktop ↔ mobile веба — отсутствуют: у мобильной поверхности один viewport, есть
  навигационный граф (`flows.md` → back-stack).
- `widgets`/`pages` FSD — нет; переиспользуемые блоки живут в `core:ui` / `Core/DesignSystem`,
  адресация — `Route`, не URL.
- `block`-деревья на `@stit/ui` — нет; состояния экрана (`loading/empty/error/…`) переносятся как
  словарь `states`.

## Reference-соответствия

Таблица reference-фич шаблона ↔ записи карты (очищается `/de-template`):

| запись карты | reference-код |
|---|---|
| `entities.md`: `todo`, `user`, `session`, `device` | `entity/{todo,user,session,device}` |
| `screens.md`: `Login`, `Sessions`, `DeleteAccount`, `Profile`, `ProfileEdit`, `TodoList`, `TodoDetail`, `Settings`, `About`, `Legal` | `feature/{auth,profile,todos,settings}` (Android), `Features/{AuthFeature,ProfileFeature,TodosFeature,SettingsFeature}` (iOS) |
| `flows.md`: `app-start`, `login`, `deep-link-todo`, `review-demo` | `ShellPresenter`, `e2e/common/*.yaml` |
| `features.md`: `login`, `logout`, `delete-account`, `sessions`, `edit-profile`, `change-password`, `todo-*`, `switch-theme`, `switch-language`, `toggle-notifications` | `feature/*` презентеры |
| `entry-points.md`: `demo`, `todo-link` (`/todos/{id}`), `push` | `core/mock`, `DeepLinkParser`, `core/push` |
| `permissions.md`: `notifications` | `core:platform` push + rationale в `settings` |
| `shared-api.md`: `auth`, `users`, `app`, `devices`, `todos` | `core/api/*Api.kt` |
| `shared-lib.md`: `ru`/`en`, `error.<code>`, `demo_login` | `MessageKey`, `AppRequirements.flags`, ресурсы |
