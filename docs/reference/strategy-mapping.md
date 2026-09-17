# Карта `mobile/<surface>/` → код

Маппинг девяти kinds карты приложения (слой `mobile/<surface>/` в `<org>/strategy`, канон —
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
| entry-point `<slug>` | `core/deeplink/DeepLinkParser.kt` + `DeepLinkParserTest` (`deep_link` из push тоже идёт сюда через `ShellIntent.OpenLink`) | `app/src/main/AndroidManifest.xml` (intent-filter, `${DEEP_LINK_HOST}`/`${DEEP_LINK_SCHEME}`, `pathPrefix`); `core/platform/…/push/{NotificationChannels,PushMessage,PushNotifier}.kt` | `App/App.entitlements` (`applinks:$(DEEP_LINK_HOST)`), `App/Info.plist` (`CFBundleURLSchemes`); `Core/Sources/Platform/PushPayload.swift` (`NotificationCategories`, `NotificationDeduplicator`) | `docs/stack/deep-links.md` (стадия `deeplinks-check`) / `push.md`; INFRA-HANDOFF (`wellknown`) |
| permission `<slug>` | интерфейс в `PlatformDeps.kt` (образец — `core/push/NotificationPermission`) | `core/platform/…/<Slug>PermissionGate.kt` (образец `NotificationPermissionGate`), `<uses-permission>` в `core/platform/src/main/AndroidManifest.xml`, pre-prompt-экран (`feature/settings/PushOptInScreen.kt`) | `Core/Sources/Platform/<Slug>Permission.swift` (образец `NotificationCenterPermission`), `Info.plist` usage-key, `Features/…/PushOptInView.swift` | `docs/stack/compliance.md` |
| shared-api ресурс | `core/api/<Resource>Api.kt` (+ сгенерированные модели) | — | — | `api/openapi.yaml` (проверка операций), `mock/fixtures/*/<resource>.json` |
| shared-lib | `core/i18n/MessageKey.kt` (`KnownErrorCodes`), `core/requirements/AppRequirements.kt` (`flags`, `flag(name)`), `core/observability` | `res/values{,-ru}/strings.xml` каждого модуля (`i18n-check`: паритет ключей, `core_ui_error_<code>`) | `Resources/Localizable.xcstrings` каждого таргета (`error.<code>` — в `DesignSystem`; advisory `i18n-check`) | — |

## Идентификаторы

| в карте | в коде |
|---|---|
| `key` экрана `TodoDetail` | `Route.TodoDetail`, `Destination.TodoDetail`, `AppRoute.todoDetail`, `TodoDetailScreen.kt`, `TodoDetailView.swift`, `todo_detail_populated.png` |
| `slug` фичи `delete-account` | `feature/auth/DeleteAccountPresenter.kt` в `shared` (пакет — группа экранов); на платформах — экраны внутри модуля группы (`androidApp/feature/auth`, `Features/AuthFeature`) |
| `slug` сущности `todo` | `entity/todo/Todo.kt`, `TodoRepository`, `todo.json` |
| push-категория `ORDER_STATUS` | `channel = "order_status"` (`PushMessage.parse` → `NotificationChannels`), `UNNotificationCategory("ORDER_STATUS")` (`NotificationCategories`); `deep_link` → `DeepLinkParser` |
| `states` | поля `data class <Key>State`: `loading`, пустой список, данные, `error: MessageKey?`, `offline`, … |

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
| `screens.md`: `Login`, `Sessions`, `DeleteAccount`, `Profile` (форма + смена пароля), `TodoList`, `TodoDetail`, `Settings` (секции «О приложении», правовые ссылки, demo-панель), `PushOptIn` | `feature/{auth,profile,todos,settings}` (Android), `Features/{AuthFeature,ProfileFeature,TodosFeature,SettingsFeature}` (iOS) |
| `flows.md`: `app-start`, `login`, `todos` | `ShellPresenterTest`, `DemoFlowTest`, `e2e/{android,ios}/*.yaml` |
| `features.md`: `login`, `logout`, `delete-account`, `sessions`, `edit-profile`, `change-password`, `todo-*`, `switch-theme`, `switch-language`, `toggle-notifications` | `feature/*` презентеры |
| `entry-points.md`: `demo`, `todo-link` (`/todos/{id}`), `push` | `core/mock`, `DeepLinkParser`, `core/push` |
| `permissions.md`: `notifications` | `core:platform` (`NotificationPermissionGate`) + pre-prompt `PushOptIn` в `settings` |
| `shared-api.md`: `auth`, `users`, `app`, `devices`, `todos` | `core/api/*Api.kt` |
| `shared-lib.md`: `ru`/`en`, `error.<code>`, `demo_login` | `MessageKey`, `AppRequirements.flags`, ресурсы |
