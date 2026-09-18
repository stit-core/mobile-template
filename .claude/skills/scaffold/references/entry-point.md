# scaffold › entry-point — deep link, push, shortcut, share, widget

Читает строку `entry-points.md` карты (`slug · kind (deep-link|push|shortcut|share|widget|demo)
· паттерн · целевой экран · требует сессии · pending после логина · trigger · status`). Целевой
экран и его `Route.<Key>` уже существуют.

## Вопросы до начала (≤ 3)

1. `kind`: `deep-link` (путь), `push` (категория), `shortcut`/`share`/`widget` (платформенный
   вход) или `demo`?
2. При `push`: сигнал бэкенда (`trigger: event:<ctx>.<Event>`) или локальное уведомление,
   которое показывает фоновая задача устройства (`trigger: background:<slug>`)?
3. Хост/схема per env уже в `docs/stack/deep-links.md` (новый хост — INFRA-HANDOFF); требует
   сессии → pending route после логина (дефолт для приватных экранов)?

## deep-link

```
shared/…/core/deeplink/DeepLinkParser.kt            # + ветка: https://<host>/<pattern> и <scheme>://<pattern> → Route.<Key>(id)
shared/src/commonTest/…/DeepLinkParserTest.kt       # + строка таблицы: url → Route (и негатив: чужой хост → null); deeplinks-check ищет здесь каждый путь
androidApp/app/src/main/AndroidManifest.xml          # + <data android:pathPrefix="/<pattern>"/> в intent-filter autoVerify (host — ${DEEP_LINK_HOST}, scheme — ${DEEP_LINK_SCHEME})
androidApp/core/navigation/…/Destination.kt          # + вариант Destination + ветка Route.toDestination()
iosApp/Packages/Core/Sources/Routing/AppRoute.swift  # + case (+ tab); SharedBridge/RouteBridge.swift — маппинг из Route; Router.open, RootView.destination
iosApp/App/App.entitlements                          # applinks:$(DEEP_LINK_HOST) уже есть — новый хост = новое значение в Config/*.xcconfig
docs/stack/deep-links.md                             # + строка «путь → Route → сессия → pending» в ## Paths to Route
```

Шаги: строка в `deep-links.md` → RED `DeepLinkParserTest` → ветка парсера → `pathPrefix` в
манифесте → `Destination`/`AppRoute` → `make api-dump` (если `Route` новый) → `make deeplinks-check`
→ INFRA-HANDOFF при новом хосте (AASA `components` меняет `<org>/infra`) → `make check`.

## push

```
androidApp/core/platform/…/push/NotificationChannels.kt   # + channel_id (= category в lower case), важность, строки core_platform_channel_*
androidApp/core/platform/…/push/PushMessage.kt            # + ключи title_key/body_key в реестр TEXTS → ресурсы core_platform_push_*
iosApp/Packages/Core/Sources/Platform/PushPayload.swift   # + UNNotificationCategory в NotificationCategories.register
shared/…/core/deeplink/DeepLinkParser.kt                  # deep_link из payload → Route (тап → ShellIntent.OpenLink(url)); своего парсера payload в shared нет
docs/stack/push.md                                        # + строка категории (провайдеры, канал, payload)
```

Шаги: строка в `push.md` → RED тест `DeepLinkParserTest` на `deep_link` категории → канал/категория
и ключи текста на платформах (`notification_id` — дедуп: `PushMessage.notificationId`,
`NotificationDeduplicator`) → BACKEND-HANDOFF (событие домена, которое шлёт бэкенд, — `trigger`
записи карты) → `make check`.

При `trigger: background:<slug>` уведомление показывает **устройство**: бэкенда, транспортов и
`PushRegistrar` в цепочке нет, категория и `deep_link` — те же, планирование — `/scaffold
background-task` (`result: notification:<slug>`), локальный показ — `PushNotifier` /
`LocalNotifier`. BACKEND-HANDOFF в этой ветке не нужен; строка категории в `push.md` помечается
как локальная.

Кнопки в уведомлении (ответ из шторки, «Готово») — не вход: это `/scaffold system-action` с
каналом `notification:<slug>`.

## shortcut / share

Платформенные точки входа без `shared`-логики сверх `Route`: Android — `shortcuts.xml` /
intent-filter `ACTION_SEND`; iOS — `UIApplicationShortcutItems` / share extension (отдельный
таргет — сначала спросить). Все ведут в `Route` через тот же роутер.

## widget

Вход **парный** записи `widgets.md`: `pattern` — slug виджета, `target` — экран, куда ведёт тап.
Своей платформенной раскладки у входа нет: провайдер кладёт в снимок `tapUrl` — тот же https-адрес,
что и deep link, — а `actionStartActivity(ACTION_VIEW, tapUrl)` с `setPackage` (Glance) и
`widgetURL` (iOS) отдают его тому же роутеру (`MainActivity.openLink` / `Router.open`). Сам
виджет — `/scaffold widget`, его кнопки — `/scaffold system-action` с каналом `widget:<slug>`.
Шаги: путь есть в `deep-links.md` и покрыт `DeepLinkParserTest` → `tapUrl` в снимке → `make check`.

## Anti-patterns

- ❌ Строковый маршрут в intent-filter/AASA без `Route`.
- ❌ Секрет или токен в ссылке/payload.
- ❌ Custom scheme как единственный вход для веб-ссылок.
- ❌ Хост в манифесте литералом (NF-18) или без строки в `deep-links.md` и без `wellknown`.
- ❌ Вход `kind: widget` без записи в `widgets.md` (и наоборот — виджет с `tap` без входа).
- ❌ Локальное уведомление (`trigger: background:<slug>`) через транспорты push и BACKEND-HANDOFF.
