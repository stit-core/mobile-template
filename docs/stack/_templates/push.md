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
| Android | dev/staging/prod | `rustore,fcm` _(регистрируются все доступные)_ | `RUSTORE_PROJECT_ID`, `google-services.json` (не секрет) |
| Android | demo | `[]` | — |
| iOS | Dev/Staging/Prod | `apns` (`sandbox` в dev) | `aps-environment` entitlement |

Серверные ключи (`FCM_SERVICE_ACCOUNT_JSON`, `RUSTORE_SERVICE_TOKEN`, `APNS_P8`) — только на
бэкенде (`golibinfra/pushkit`); `providers: []` — приложение без push.

## Registration

`PUT /api/v1/devices` (`provider`, `token`, `platform`, `app_version`, `os_version`,
`device_model`, `locale`, `timezone`, `installation_id`, `sandbox`, `push_enabled`) — upsert по
`(provider, token)`; при логине, смене токена, локали, `push_enabled`; не чаще раза в сутки;
`DELETE /api/v1/devices/{id}` при logout. Реализация — `core/push/PushRegistrar` (снимок
`DeviceRegistration` и `installation_id` — в `Settings`).

## Channels and categories

| категория (UPPER_SNAKE) | Android channel_id · важность | iOS category | deep link | источник (карта `entry-points.md`) |
|---|---|---|---|---|
| _`ORDER_STATUS`_ | _`orders` · high_ | _`ORDER_STATUS`_ | _`https://app.<domain>/orders/{id}`_ | _`push:order-status`_ |

## Permission flow

Запрос (`POST_NOTIFICATIONS` на Android 13+, `requestAuthorization`) — в момент ценности, после
pre-prompt; не на первом экране; отказ → `push_enabled=false`.

## Payload

Data-first: `title_key`, `body_key`, `args`, `deep_link` (https из `deep-links.md`), `category`,
`notification_id` (дедуп); ≤ 4096 байт; RuStore — дублировать `notification.title/body`; APNs —
`loc-key`/`loc-args`; silent push — только бейдж/кэш.

## Generates

- `config/env/*.properties`, `iosApp/Config/*.xcconfig` — `PUSH_PROVIDERS`
- `androidApp/core/platform/…/push/{FcmTransport,RuStoreTransport,NotificationChannels}.kt`
- `iosApp/Packages/Core/Sources/Platform/{ApnsRegistrar,NotificationCategories}.swift`
- `shared/…/core/push/{PushRegistrar,DeviceRegistration,PushStatus,PushTransport}.kt`, `PlatformDeps.pushTransports`/`notificationPermission`
- `dependencies.md` (SDK провайдеров) + `compliance.md` (privacy manifests)
- BACKEND-HANDOFF B-18 (`PUT /api/v1/devices`, отправка через `pushkit`)

## Открытые вопросы

- _что_
