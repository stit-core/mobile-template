---
name: navigation
description: Route — sealed в shared, DeepLinkParser — чистая функция Url → Route? с тестами; Android — Navigation 3 (Destination : NavKey, rememberNavBackStack, NavDisplay + entryProvider из <group>Entries), iOS — AppRoute/Router + NavigationStack(path:) на таб; pending route после логина, строковых маршрутов нет. Грузится при правке deeplink/, core/navigation, *Nav*.kt, Routing/, *Route*.swift.
paths:
  - "shared/**/deeplink/**"
  - "androidApp/core/navigation/**"
  - "androidApp/feature/**/*Nav*.kt"
  - "iosApp/**/Routing/**"
  - "iosApp/**/*Route*.swift"
---

# Навигация и deep links

## Инварианты

- **`Route` — один `sealed` тип в `shared/…/core/deeplink/Route.kt`** (`Home`, `Login`, `Profile`,
  `Settings`, `TodoList`, `TodoDetail(id)`; `requiresAuth` — всё, кроме `Login`/`Settings`).
  Варианты — `data object`/`data class` с id в аргументах. Строковых маршрутов нет.
- **`DeepLinkParser(host, scheme).parse(url): Route?` — чистая функция** без I/O: принимает
  `https://<DEEP_LINK_HOST>/…` и `<DEEP_LINK_SCHEME>://…`; каждая строка
  `docs/stack/deep-links.md#paths-to-route` и каждый `pathPrefix` манифеста покрыты
  `DeepLinkParserTest` (стадия `deeplinks-check`).
- **Android — Navigation 3** (`androidApp/core/navigation`): `Destination : NavKey` —
  `@Serializable sealed interface` (`Login`, `TodoList`, `TodoDetail(id)`, `Profile`, `Sessions`,
  `DeleteAccount`, `Settings`, `PushOptIn`; `isTopLevel` — корни вкладок), `Route.toDestination()`;
  `BackStack.kt` — `navigateTo` (push без дубля), `resetTo` (смена корня), `pop`, `current`.
  `app/AppNavigation.kt` — единственное место, где фичи знают друг о друге:
  `rememberNavBackStack(Destination.TodoList)` + `NavDisplay(entryProvider { authEntries();
  todosEntries(); profileEntries(); settingsEntries() })`; фича регистрирует экраны в
  `<Group>NavEntry.kt` — `fun EntryProviderScope<NavKey>.<group>Entries(колбэки)`
  (`entry<Destination.X> { XRoute(...) }`), переходы наружу — колбэками. Back —
  `NavDisplay(onBack = backStack.pop())` + `enableOnBackInvokedCallback` в манифесте;
  `NavHost`/`NavController`/`androidx.navigation` 2.x — detekt `ForbiddenImport`.
- **iOS — `Core/Routing`**: `AppRoute` — enum-зеркало `Route` (+ `sessions`, `deleteAccount` без
  deep link; `tab: AppTab?`), `Router` (`@Observable`: `tab`, `paths[tab]`, `open/push/pop/popToRoot`);
  `RootView` — `TabView` + `NavigationStack(path:)` на таб, `navigationDestination(for: AppRoute.self)`;
  `AppRoute(Route)` — `SharedBridge/RouteBridge.swift`; `NavigationLink(value:)` только с
  `navigationDestination` в том же стеке.
- **Pending route**: `ShellIntent.OpenLink(url)` → `DeepLinkParser`; при `Route.requiresAuth`
  без сессии маршрут ждёт в `ShellState.pendingRoute` и переходит в `route` после `SignedIn`;
  UI выполняет `route` (`ShellRoute.onRoute` / `RootView.onChange(route)`) и шлёт
  `ShellIntent.RouteConsumed`. Ссылка не несёт секретов — только id и одноразовые коды с TTL.
- **Порядок старта** (`ShellPresenter.Start`): demo-режим → кэши → сессия → auth-gate → route;
  `RequirementsPresenter` (блокирующие оверлеи) — параллельно. UI не решает, куда идти, —
  берёт `Route`/`AuthState` из состояния презентера (`ShellRoute.onAuthChange` меняет корень стека).
- **Push → deep link**: payload несёт https-URL из таблицы `deep-links.md`; роутер один и тот же
  (`MainActivity.openLink` / `AppDelegate` → `ShellIntent.OpenLink`; rule `deep-links-and-push`).

## Добавить маршрут

1. Строка в `docs/stack/deep-links.md` (и `entry-points.md` карты в `<org>/strategy`).
2. Вариант `Route`, ветка `DeepLinkParser` + тест (RED → GREEN); `make api-dump` — `Route` часть фасада.
3. Android: `Destination` + `Route.toDestination()` + `entry<…>` в `<group>Entries`; iOS: case `AppRoute`
   + `RouteBridge` + `Router.open` + `RootView.destination`. Intent-filter/`applinks` — INFRA-HANDOFF при новом хосте.
