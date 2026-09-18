---
name: deep-links-and-push
description: Таблица хостов/схем per env = docs/stack/deep-links.md, значения — config/env и xcconfig через плейсхолдеры (стадия deeplinks-check); AASA/assetlinks — в <org>/infra (wellknown); push — data-first payload, notification_id для дедупа, регистрация PUT /api/v1/devices, провайдеры по PUSH_PROVIDERS, пред-запрос разрешения; локальные уведомления фоновой задачи и кнопки категорий — без бэкенда. Грузится при правке манифеста, entitlements, core/deeplink, core/push, платформенных push, deep-links.md, push.md.
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

- **Источник правды — `docs/stack/deep-links.md`**: хосты/схемы per env, отпечатки, таблица
  `## Paths to Route` («путь → `Route` → требует сессии → pending»). Значения — `config/env/*.properties`
  (`DEEP_LINK_HOST`, `DEEP_LINK_SCHEME`) и `iosApp/Config/*.xcconfig`: intent-filter (`autoVerify`)
  берёт их плейсхолдерами `${DEEP_LINK_HOST}`/`${DEEP_LINK_SCHEME}` (`manifestPlaceholders`),
  `App.entitlements` — `applinks:$(DEEP_LINK_HOST)`, `Info.plist` — `CFBundleURLSchemes $(DEEP_LINK_SCHEME)`.
- **Стадия `deeplinks-check`** (`scripts/tools/deeplinks-check.sh`): `autoVerify` есть, хост и схема —
  из плейсхолдеров (литерал хоста — NF-18), оба ключа непусты в каждом env, каждый `pathPrefix`
  манифеста и каждая строка `## Paths to Route` покрыты `DeepLinkParserTest`.
- **`.well-known` отдаёт `<org>/infra`** (сервис `wellknown`): `assetlinks.json` со **всеми** отпечатками
  (upload, Play app-signing, RuStore), AASA на каждом сабдомене, HTTPS, без редиректов; env
  `MOBILE_ANDROID_APP_ID`, `MOBILE_ANDROID_SHA256_*`, `MOBILE_IOS_TEAM_ID`, `MOBILE_IOS_BUNDLE_ID` →
  INFRA-HANDOFF при любом изменении хоста/отпечатка.
- **Custom scheme — fallback** для писем/QR; в push и вебе — только https-URL; ссылка не несёт секретов.
  Apple CDN кэширует AASA до 24 ч; Android 15+ ре-верифицирует до 7 дней (`adb shell pm verify-app-links --re-verify <pkg>`).

## Push

- **Регистрация — `PUT /api/v1/devices`** (`provider`, `token`, `platform`, `app_version`, `locale`,
  `installation_id`, `sandbox`, `push_enabled`; upsert по `(provider, token)`) при логине, смене токена,
  локали или `push_enabled`, не чаще раза в сутки; `DELETE` при logout. Реализация — `core/push/PushRegistrar`
  (снимок `DeviceRegistration` в `Settings`), транспорты — `PlatformDeps.pushTransports`, разрешение — `NotificationPermission`.
- **Провайдеры по `PUSH_PROVIDERS`** (build-time). Android `rustore,fcm` → `pushTransports()`
  (`core/platform/push/PushTransports.kt`; неизвестное имя — ошибка): `FcmTransport` без `google-services.json`
  и `RuStoreTransport` без `RUSTORE_PROJECT_ID` отдают `token() == null`; сервисы
  `Stit{Firebase,RuStore}MessagingService` → `PushMessage.parse` → `PushNotifier` (канал
  `NotificationChannels.GENERAL`, тап — `ACTION_VIEW` deep link). iOS `apns` → `ApnsRegistrar` (токен через
  `AppDelegate`), `NotificationCategories` и `NotificationDeduplicator` — `Core/Platform/PushPayload.swift`.
- **Payload data-first**: `title_key`/`body_key` + `args`, `deep_link` (https из таблицы), `category`,
  `notification_id`; текст строит клиент (реестр — `PushMessage.TEXTS`); RuStore — дублировать
  `notification.title/body`, APNs — `loc-key`. Дедуп по `notification_id`; silent push — только бейдж/кэш.
- **Локальные уведомления** (`entry-points.trigger: background:<slug>`) показывает устройство по
  `BackgroundResult.Notify` (`PushNotifier` / `LocalNotifier`): провайдеры, `PushRegistrar` и
  BACKEND-HANDOFF не участвуют; категория и `deep_link` — из той же таблицы `push.md`.
- **Кнопки уведомления** (`system-actions` канал `notification:<slug>`): `NotificationCompat.Action`
  рисует `PushNotifier` → `NotificationActionReceiver` (адресат — из `deep_link` того же
  уведомления, `goAsync()` держит процесс), на iOS — `UNNotificationAction`; обе ветки идут в
  `<Slug>ActionHandler` → `<Slug>Action`, тексты — ресурсы. Успех снимает уведомление и
  перерисовывает виджет, отказ показывается новым уведомлением со ссылкой, где чинить.
- **Тап по виджету — тот же https-адрес** (`tapUrl` снимка → `ACTION_VIEW` → `DeepLinkParser`):
  путь обязан быть в таблице `deep-links.md` и покрыт тестом.
- **Разрешение — в момент ценности**: pre-prompt `PushOptIn{Route,Screen,ViewModel}` / `PushOptInView` →
  `NotificationPermissionGate` (Activity Result API) / `NotificationCenterPermission`; отказ → `push_enabled=false`.
- Серверные ключи (`FCM_SERVICE_ACCOUNT_JSON`, `RUSTORE_SERVICE_TOKEN`, `APNS_P8`) — только на бэкенде
  (`golibinfra/pushkit`); Firebase — только FCM-транспорт, Crashlytics/Analytics нет.

## Сигналы

- Ссылка открывается в браузере → `assetlinks`/AASA не отдаются или отпечаток не тот.
- `deeplinks-check` красный → путь без теста в `DeepLinkParserTest` или хост литералом в манифесте.
