# scaffold › entry-point — deep link, push, shortcut, share

Читает строку `entry-points.md` карты (`slug · kind (deep-link|push|shortcut|share|widget|demo)
· паттерн · целевой экран · требует сессии · pending после логина · status`). Целевой экран
и его `Route.<Key>` уже существуют.

## Вопросы до начала (≤ 3)

1. `kind`: `deep-link` (путь) или `push` (категория)?
2. Хост/схема per env уже в `docs/stack/deep-links.md`? Новый хост — INFRA-HANDOFF.
3. Требует сессии → pending route после логина (дефолт для приватных экранов)?

## deep-link

```
shared/…/core/deeplink/DeepLinkParser.kt           # + ветка: https://<host>/<pattern> и <scheme>://<pattern> → Route.<Key>(id)
shared/src/commonTest/…/DeepLinkParserTest.kt      # + строка таблицы: url → Route (и негатив: чужой хост → null)
androidApp/app/src/main/AndroidManifest.xml         # intent-filter autoVerify: scheme https + host ${DEEP_LINK_HOST} + pathPattern; custom scheme
iosApp/App/App.entitlements                          # applinks:<host> (через xcconfig per env)
docs/stack/deep-links.md                             # + строка «путь → Route → сессия → pending»
```

Шаги: строка в `deep-links.md` → RED `DeepLinkParserTest` → ветка парсера → intent-filter +
`applinks` → `make api-dump` (если `Route` новый) → INFRA-HANDOFF при новом хосте (AASA
`components` меняет `<org>/infra`) → `make check`.

## push

```
androidApp/core/platform/…/push/NotificationChannels.kt   # + channel_id, важность, строки; payload → уведомление
iosApp/Packages/Core/Sources/Platform/NotificationCategories.swift   # + UNNotificationCategory
shared/…/core/deeplink/DeepLinkParser.kt             # deep_link из payload → Route (тап → ShellIntent.OpenLink(url)); в shared своего парсера payload нет
docs/stack/push.md                                   # + строка категории (провайдеры, канал, payload)
```

Шаги: строка в `push.md` → RED тест `DeepLinkParserTest` на `deep_link` категории → категория/канал
на платформах (`notification_id` — дедуп, ключи локализации — ресурсы) → BACKEND-HANDOFF (событие
домена, которое шлёт бэкенд, — `trigger` записи карты) → `make check`.

## shortcut / share / widget

Платформенные точки входа без `shared`-логики сверх `Route`: Android — `shortcuts.xml` /
intent-filter `ACTION_SEND`; iOS — `UIApplicationShortcutItems` / share extension (отдельный
таргет — сначала спросить). Все ведут в `Route` через тот же роутер.

## Anti-patterns

- ❌ Строковый маршрут в intent-filter/AASA без `Route`.
- ❌ Секрет или токен в ссылке/payload.
- ❌ Custom scheme как единственный вход для веб-ссылок.
- ❌ Хост в манифесте без строки в `deep-links.md` и без `wellknown`.
