---
id: stack.push
kind: push
name: push
status: draft
---

# push

## Providers

| платформа | flavor/схема | провайдеры (`PUSH_PROVIDERS`) | client-side id |
|---|---|---|---|
| Android | dev/staging/prod | `rustore,fcm` _(`pushTransports()`: регистрируются все, у кого `token() != null`)_ | `RUSTORE_PROJECT_ID` (`config/env`; пустой — RuStore выключен), `google-services.json` (не секрет; без него FCM отдаёт `null`) |
| Android | demo | `[]` | — |
| iOS | Dev/Staging/Prod | `apns` (`sandbox` в dev) | `aps-environment` entitlement |

Серверные ключи (`FCM_SERVICE_ACCOUNT_JSON`, `RUSTORE_SERVICE_TOKEN`, `APNS_P8`) — только на
бэкенде (отправка — BACKEND-HANDOFF B-18); `providers: []` — приложение без push.

## Registration

`PUT /api/v1/devices` (`provider`, `token`, `platform`, `app_version`, `os_version`,
`device_model`, `locale`, `timezone`, `installation_id`, `sandbox`, `push_enabled`) — upsert по
`(provider, token)`; при логине, смене токена, локали, `push_enabled`; не чаще раза в сутки;
`DELETE /api/v1/devices/{id}` при logout. Реализация — `core/push/PushRegistrar` (снимок
`DeviceRegistration` и `installation_id` — в `Settings`).

## Channels and categories

Колонка «источник» различает два происхождения: событие домена шлёт бэкенд, локальную категорию
показывает **само устройство** по фоновой задаче (`entry-points.trigger: background:<slug>`) — для
неё провайдеры, транспорты и `PushRegistrar` не участвуют.

| категория (UPPER_SNAKE) | Android channel_id · важность | iOS category | deep link | источник (карта `entry-points.md`) |
|---|---|---|---|---|
| _`ORDER_STATUS`_ | _`orders` · high_ | _`ORDER_STATUS`_ | _`https://app.<domain>/orders/{id}`_ | _`push:order-status` · бэкенд `event:orders.StatusChanged`_ |
| _`TODO_REMINDER`_ | _`todo_reminder` · default_ | _`TODO_REMINDER`_ | _`https://app.<domain>/todos/{id}`_ | _`push:todo-reminder` · локальная, `background:todo-reminder`_ |

## Actions

Кнопки в уведомлении — записи `system-actions.md` карты с каналом `notification:<slug>`; логика
живёт в `<Slug>Action` (`shared`), кнопка только её зовёт.

| категория | кнопка (`system-action`) | `mode` | требует разблокировки |
|---|---|---|---|
| _`TODO_REMINDER`_ | _`complete-todo`_ | _`headless`_ | _нет_ |

Android — `NotificationCompat.Action` (рисует `PushNotifier`) → `NotificationActionReceiver`,
адресат берётся из `deep_link` того же уведомления; iOS — `UNNotificationAction` в
`NotificationCategories`. Тексты кнопок и ответов — ресурсы (`core_platform_action_<slug>_*`,
`action.<slug>.*`), не литералы. Успех снимает уведомление и перерисовывает виджет; отказ
`headless`-канала виден единственным доступным способом — уведомлением со ссылкой, где чинить.

## Permission flow

Запрос (`POST_NOTIFICATIONS` на Android 13+, `requestAuthorization`) — в момент ценности, после
pre-prompt; не на первом экране; отказ → `push_enabled=false`.

## Payload

Data-first: `title_key`, `body_key`, `args`, `deep_link` (https из `deep-links.md`), `category`,
`notification_id` (дедуп); ≤ 4096 байт; RuStore — дублировать `notification.title/body`; APNs —
`loc-key`/`loc-args`; silent push — только бейдж/кэш.

## Generates

- `config/env/*.properties` (`PUSH_PROVIDERS`, `RUSTORE_PROJECT_ID`), `iosApp/Config/*.xcconfig` (`PUSH_PROVIDERS`, `APS_ENVIRONMENT`)
- `androidApp/core/platform/…/push/{PushTransports,FcmTransport,RuStoreTransport,NotificationChannels,PushMessage,PushNotifier,StitFirebaseMessagingService,StitRuStoreMessagingService}.kt` + сервисы в `core/platform/src/main/AndroidManifest.xml`
- `iosApp/Packages/Core/Sources/Platform/{ApnsRegistrar,PushPayload}.swift` (`NotificationCategories`, `NotificationDeduplicator`), `App/AppDelegate.swift`
- `shared/…/core/push/{PushRegistrar,DeviceRegistration,PushStatus,PushTransport}.kt`, `PlatformDeps.pushTransports`/`notificationPermission`
- кнопки категорий: `androidApp/core/platform/…/action/NotificationActionReceiver.kt`, `UNNotificationAction` в `Core/Platform/PushPayload.swift`, строки `core_platform_action_*` / `action.*`
- локальные категории: показ из `BackgroundResult.Notify` (`background.md`), без транспортов и регистрации
- `dependencies.md` (SDK провайдеров) + `compliance.md` (privacy manifests)
- BACKEND-HANDOFF B-18 (`PUT /api/v1/devices`, серверная отправка push) — только для категорий с источником-событием

## Открытые вопросы

- _что_
