---
name: deep-links-and-push
description: Таблица хостов/схем per env = docs/stack/deep-links.md; AASA/assetlinks — в <org>/infra (wellknown); push — data-first payload, notification_id для дедупа, регистрация PUT /api/v1/devices, провайдеры по PUSH_PROVIDERS, пред-запрос разрешения. Грузится при правке манифеста, entitlements, core/deeplink, core/push, платформенных push, deep-links.md, push.md.
paths:
  - "androidApp/app/src/main/AndroidManifest.xml"
  - "iosApp/**/*.entitlements"
  - "shared/**/core/deeplink/**"
  - "shared/**/core/push/**"
  - "androidApp/core/platform/**/push/**"
  - "iosApp/**/Platform/**"
  - "docs/stack/deep-links.md"
  - "docs/stack/push.md"
---

# Deep links и push

Маршрутизация внутри приложения — rule `navigation`. Здесь — контракт с ОС, инфрой и бэкендом.

## Deep links

- **Источник правды — `docs/stack/deep-links.md`**: хосты/схемы per env, отпечатки подписи,
  таблица «путь → `Route` → требует сессии → pending». Intent-filters (`autoVerify`) и
  `applinks:` в entitlements выводятся из неё (`/scaffold entry-point`); хост в манифесте без
  строки в таблице — ошибка.
- **`.well-known` отдаёт `<org>/infra`** (сервис `wellknown`): `assetlinks.json` со **всеми**
  отпечатками (upload, Play app-signing, RuStore), `apple-app-site-association` на каждом
  конкретном сабдомене, HTTPS, `application/json`, без редиректов. Значения — env
  `MOBILE_ANDROID_APP_ID`, `MOBILE_ANDROID_SHA256_*`, `MOBILE_IOS_TEAM_ID`,
  `MOBILE_IOS_BUNDLE_ID` → INFRA-HANDOFF при любом изменении хоста/отпечатка.
- **Custom scheme (`<DEEP_LINK_SCHEME>://`) — fallback** для писем/QR; в push и вебе —
  только https-URL. Ссылка не несёт секретов.
- Apple CDN кэширует AASA до 24 ч; Android 15+ ре-верифицирует до 7 дней — проверка
  `adb shell pm verify-app-links --re-verify <pkg>`, чек-лист в `deep-links.md`.

## Push

- **Регистрация — `PUT /api/v1/devices`** (`provider`, `token`, `platform`, `app_version`,
  `locale`, `installation_id`, `sandbox`, `push_enabled`; upsert по `(provider, token)`) при
  логине, смене токена, локали или `push_enabled`, не чаще раза в сутки; `DELETE` при logout.
  Реализация — `core/push/PushRegistrar` (снимок `DeviceRegistration` в `Settings`), транспорты —
  `PlatformDeps.pushTransports: List<PushTransport>`, разрешение — `NotificationPermission`.
- **Провайдеры по `PUSH_PROVIDERS`** (build-time): Android — `rustore,fcm` (мульти-провайдер,
  регистрируются все доступные), iOS — `apns`; список и каналы/категории — `docs/stack/push.md`.
- **Payload data-first**: `title_key`/`body_key` + `args`, `deep_link` (https из таблицы),
  `category`, `notification_id`; текст строится клиентом из своих ресурсов. Для RuStore
  дублируется `notification.title/body` (иначе не показывается); для APNs — `loc-key`.
- **Дедуп по `notification_id`** (один сигнал через два транспорта); silent push — только
  бейдж/кэш, без бизнес-логики.
- **Разрешение — в момент ценности**, не на первом экране: pre-prompt (rationale) →
  системный запрос (`POST_NOTIFICATIONS` / `requestAuthorization`); отказ → `push_enabled=false`.
- Серверные ключи (`FCM_SERVICE_ACCOUNT_JSON`, `RUSTORE_SERVICE_TOKEN`, `APNS_P8`) — только на
  бэкенде (`golibinfra/pushkit`); в приложении — client-side ID провайдера.
- Firebase — только FCM-транспорт; Crashlytics/Analytics не подключаются.

## Сигналы

- Ссылка открывается в браузере → `assetlinks`/AASA не отдаются или отпечаток не тот.
- Пуш без deep link или с секретом в payload — нарушение контракта (`push.md`).
