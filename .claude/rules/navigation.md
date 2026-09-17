---
name: navigation
description: Route — sealed в shared, DeepLinkParser — чистая функция Url → Route? с тестами, Android — Navigation 3 (NavKey ↔ Route, rememberNavBackStack), iOS — NavigationStack(path: [Route]), pending route после логина, строковых маршрутов нет. Грузится при правке deeplink/, core/navigation, *Nav*.kt, Routing/, *Route*.swift.
paths:
  - "shared/**/deeplink/**"
  - "androidApp/core/navigation/**"
  - "androidApp/feature/**/*Nav*.kt"
  - "iosApp/**/Routing/**"
  - "iosApp/**/*Route*.swift"
---

# Навигация и deep links

## Инварианты

- **`Route` — один `sealed` тип в `shared/…/core/deeplink/Route.kt`.** Варианты —
  `data object`/`data class` с идентификаторами в аргументах (не объекты). Строковых
  маршрутов нет ни на одной платформе.
- **`DeepLinkParser.parse(url): Route?` — чистая функция** без I/O и платформенных типов;
  принимает `https://<DEEP_LINK_HOST>/…` и `<DEEP_LINK_SCHEME>://…`; каждая строка таблицы
  `docs/stack/deep-links.md#paths-to-route` покрыта тестом в `commonTest`.
- **Android — Navigation 3**: `NavKey`-обёртки над `Route` в `androidApp/core/navigation`,
  `rememberNavBackStack` (переживает смерть процесса), `NavDisplay` с реестром
  `entryProvider`; `NavHost`/`NavController`/`androidx.navigation` 2.x — нет.
  Back — `BackHandler`/`PredictiveBackHandler`.
- **iOS — `NavigationStack(path: [Route])`** в `Core/Routing` (`Router`); `NavigationLink(value:)`
  только с `navigationDestination(for:)` в том же стеке.
- **Pending route**: `ShellIntent.OpenLink(url)` → `DeepLinkParser`; при `Route.requiresAuth`
  без сессии маршрут ждёт в `ShellState.pendingRoute` и переходит в `route` после `SignedIn`;
  UI выполняет `route` и шлёт `ShellIntent.RouteConsumed`. Ссылка не несёт секретов — только
  id и одноразовые коды с TTL.
- **Порядок старта** (`ShellPresenter.Start`): demo-режим → кэши → сессия → auth-gate → route;
  `RequirementsPresenter` (блокирующие оверлеи) — параллельно. UI не решает, куда идти, —
  берёт `Route` из состояния презентера.
- **Push → deep link**: payload несёт https-URL из таблицы `deep-links.md`; роутер один и
  тот же (rule `deep-links-and-push`).
- Хосты/схемы per env — `config/env/*.properties` (`DEEP_LINK_HOST`, `DEEP_LINK_SCHEME`) и
  `iosApp/Config/*.xcconfig`; intent-filter (`autoVerify`) и `applinks:` генерируются из
  `deep-links.md` (skill `/scaffold entry-point`).

## Добавить маршрут

1. Строка в `docs/stack/deep-links.md` (и `entry-points.md` карты в `<org>/strategy`).
2. Вариант `Route`, ветка `DeepLinkParser` + тест (RED → GREEN).
3. `NavKey`/`entryProvider` (Android), `navigationDestination` (iOS).
4. `make api-dump` — `Route` часть фасада; intent-filter/AASA — INFRA-HANDOFF при новом хосте.
