# Чек-листы полей по kind

По разделу на каждый из 16 kinds: **обязательные H2**, **что в Generates**, **cross-kind**,
**anti-patterns**, **порядок копания**. Полный канон — `docs/stack/README.md` +
`_templates/<kind>.md`; здесь — выжимка для скилла. Значения (хосты, id, отпечатки, числа) —
только через Other; enum'ы и anti-patterns — опциями.

---

## service (W1)

**H2**: `Identity`, `Platforms`, `Ecosystem`, `Generates`.
**Generates**: `applicationId`/suffix'ы в `config/env/*.properties` и `androidApp/app/build.gradle.kts`,
bundle id в `iosApp/Config/*.xcconfig`, `README.md`-шапка, `platforms`/`preset` для скиллов.
**Cross-kind**: `platforms` → все iOS-секции; `preset` → условные kinds, каналы `distribution`.
**Anti-patterns**: `applicationId` шаблонный (`tech.stit.mobile`); `preset` пуст; `surface` не из
реестра; App Group при `ios ∈ platforms` не задан, хотя в карте есть виджеты.
**Порядок**: имя, `project-slug`, `strategy-path`/`backend-path` (дефолты — опции) → id per env
(Other) → bundle id расширения виджетов и App Group при `ios ∈ platforms` (Other) → `platforms`
(multi-select) → `preset` (enum) → `auth.mode` (enum) → схема deep link, локали → владелец
аккаунтов (Other) → `Generates`.

## runtime (W2)

**H2**: `Toolchains`, `Targets`, `Concurrency`, `Mac build-host`, `Update policy`, `Generates`.
**Generates**: `gradle/libs.versions.toml` (SDK-уровни), `gradle.properties`, `.xcode-version`,
`.swift-version`, `iosApp/.test-simulator`.
**Cross-kind**: `ios ∈ platforms` ⇒ `Mac build-host` заполнен или `blocked-by`; версии →
`dependencies`.
**Anti-patterns**: alpha/beta/rc в пинах; `compileSdk < 37`; Mac не упомянут при iOS.
**Порядок**: подтвердить дефолты toolchain'ов (опции «дефолт шаблона» / Other) → minSdk/target/
deployment target → Swift-mode → `MAC_HOST` (Other) → политика обновлений → `Generates`.

## api (W3)

**H2**: `Spec`, `Codegen`, `Client`, `Auth scheme`, `Errors and retries`, `Compatibility`, `Mock`, `Generates`.
**Generates**: `api/openapi.yaml` + `api/openapi.lock`, `config/openapi/kotlin-models.yaml`,
`core/api/generated/**`, `<Resource>Api.kt`, `make gen`/`gen-check`.
**Cross-kind**: `Auth scheme` ↔ `auth.md#mode`; `Mock` ↔ `shared-api.md` карты; `min backend
version` → `config`/`requirements`.
**Anti-patterns**: клиент генерируется; спека правится руками; `/api/v1` в клиенте; версия
бэкенда не зафиксирована.
**Порядок**: `backend-path` и наличие спеки → операции reference-ресурсов (multi-select из
спеки) → политика совместимости → mock-сценарии (подтвердить 4 дефолтных) → `Generates`.

## auth (W3)

**H2**: `Mode`, `Tokens`, `Storage`, `Sessions`, `Biometrics`, `Pinning`, `Account deletion`, `Generates`.
**Generates**: `AUTH_MODE` в `config/env`, `core/auth` политика TTL, экраны `auth`
(оставить/удалить), BACKEND-HANDOFF B-1…B-3 при `cookie`/отсутствии refresh.
**Cross-kind**: `mode = bearer` ⇔ `POST /api/auth/refresh` в спеке; `Storage` → `compliance`
(шифрование); `Account deletion` → `compliance` (URL веб-удаления).
**Anti-patterns**: токен в `SharedPreferences`/`UserDefaults`; `cookie` как постоянный режим;
pinning на leaf; нет удаления аккаунта.
**Порядок**: `mode` (enum, дефолт bearer) → TTL/ротация (подтвердить) → биометрия (yes/no) →
сессии (yes/no) → pinning (enum off/roots) → удаление аккаунта (обязателен при регистрации) → `Generates`.

## deep-links (W4)

**H2**: `Hosts and schemes`, `Fingerprints`, `Paths to Route`, `Generates`.
**Generates**: intent-filters `AndroidManifest.xml`, `applinks:` entitlements, тесты
`DeepLinkParser`, INFRA-HANDOFF `MOBILE_*`.
**Cross-kind**: хосты → `config` per env; отпечатки == `signing`; пути ↔ `entry-points.md` карты
и `pages.path` web-surface того же продукта.
**Anti-patterns**: один хост на все среды; один отпечаток; custom scheme как основной; путь
без `Route`.
**Порядок**: хосты per env (Other) → схема → отпечатки (Other; «ещё нет» → blocker до `signing`)
→ таблица путей из `entry-points.md` (опции из карты + Other) → `Generates`.

## push (W4)

**H2**: `Providers`, `Registration`, `Channels and categories`, `Actions`, `Permission flow`, `Payload`, `Generates`.
**Generates**: `PUSH_PROVIDERS` в `config/env`, транспорты `core:platform`/`Core/Platform`,
`PlatformDeps.pushTransports`, кнопки категорий (`NotificationActionReceiver`,
`UNNotificationAction`), BACKEND-HANDOFF B-18.
**Cross-kind**: провайдеры → `dependencies` (SDK) + `compliance` (privacy manifest) + `config`;
категории ↔ `entry-points.md` (push); локальные категории ↔ `background.md` (`result:
notification:<slug>`); кнопки ↔ `system-actions.md` (канал `notification:<slug>`).
**Anti-patterns**: только FCM для RU; разрешение на первом экране; секреты провайдера в
приложении; payload с ПДн; локальная категория с BACKEND-HANDOFF; текст кнопки литералом.
**Порядок**: `providers: []`? → провайдеры per flavor (multi-select) → регистрация (подтвердить
`PUT /api/v1/devices`) → каналы/категории из карты (источник: событие домена или
`background:<slug>`) → кнопки из `system-actions.md` → permission-flow → `Generates`.

## background (W4)

**H2**: `Timing to mechanism`, `Tasks`, `Identifiers`, `Limits and permissions`, `Generates`.
**Generates**: `core/background` + `PlatformDeps.background`, `AndroidBackgroundScheduler`/
`BackgroundTaskWorker`/`BackgroundAlarmReceiver`/`BackgroundDelivery`, `BgTaskScheduler`/`LocalNotifier`, `BGTaskSchedulerPermittedIdentifiers` и
`UIBackgroundModes` в `Info.plist`, разрешения манифеста, стадия `capability-check`.
**Cross-kind**: реестр задач ← `background-tasks.md` карты; `result: notification:<slug>` →
`push` (локальная категория); `result: widget:<slug>` → модуль виджетов; точные будильники, тип
foreground service и background modes → `compliance`; WorkManager → `dependencies`; App Group →
`service`/`config`.
**Anti-patterns**: `exact` для исполнения кода, а не для показа уведомления; foreground service
под работу без видимого хода; задача без `fallback`; интервалы в минутах как обещание;
идентификатор `Info.plist` без зарегистрированной задачи.
**Порядок**: задач нет? → подтвердить таблицу «`timing` → механизм» (дефолты шаблона опциями) →
строки задач из карты (опции из `background-tasks.md`) → идентификаторы → лимиты, разрешения и
App Group → `Generates`.

## observability (W5)

**H2**: `Receiver`, `DSN per env`, `PII policy`, `Logging`, `Symbols`, `Quality thresholds`, `Generates`.
**Generates**: `SENTRY_DSN` в `config/env`/xcconfig, инициализация в `core:platform`/`Core/Platform`,
`SENTRY_AUTH_TOKEN` в release-preflight, пороги для STORE-HANDOFF.
**Cross-kind**: DSN → `config`; символы → `distribution` (preflight); согласие → `compliance`.
**Anti-patterns**: DSN prod пуст; `sendDefaultPii`; символы «потом»; OTel без коллектора.
**Порядок**: приёмник (enum GlitchTip/Sentry SaaS/none) → DSN per env (Other; dev пусто) →
PII → пороги (подтвердить дефолты) → `Generates`.

## styling (W5)

**H2**: `Tokens`, `Theme`, `Components`, `Accessibility`, `i18n`, `Generates`.
**Generates**: `androidApp/core/designsystem/*`, `Pure/DesignSystemTokens`, `values-<lang>/`,
`Localizable.xcstrings`.
**Cross-kind**: шрифты → `dependencies`; локали ↔ `shared-lib.md` карты; `compliance` (a11y).
**Anti-patterns**: сырые значения в фичах; палитра вместо ролей; локали не названы.
**Порядок**: таблица токенов (подтвердить имена, значения — Other) → тема → Material 3 /
Liquid Glass политика → a11y-минимум → локали (default `ru`) → `Generates`.

## distribution (W6)

**H2**: `Channels`, `STOP-1 / STOP-2`, `Accounts`, `Moderation`, `Release notes`, `App Review notes`, `Deadlines`, `Generates`.
**Generates**: `CHANNELS` для `scripts/release/*`, STORE-HANDOFF-шаблон, структура `dist/`.
**Cross-kind**: каналы ⊆ `platforms`, по `preset`; аккаунты → `signing`; demo-доступ ↔
`entry-points.md` (demo) и `mock`.
**Anti-patterns**: Play при `enterprise`; каналы без аккаунтов; нет demo для ревью.
**Порядок**: каналы (multi-select по пресету) → аккаунты и владелец (Other) → сроки модерации
(подтвердить) → release-notes-политика → App Review notes → дедлайны → `Generates`.

## signing (W6)

**H2**: `Inventory`, `Custody`, `Fingerprints`, `Rotation`, `Generates`.
**Generates**: `signingConfigs` из env (`STIT_KEYSTORE_*`), `.env.local.example`,
`iosApp/Config/Local.xcconfig.example`, чек отпечатков `/de-template`.
**Cross-kind**: отпечатки == `deep-links`; аккаунты ↔ `distribution`.
**Anti-patterns**: ключи не в Vaultwarden; upload == app-signing; нет резервной копии.
**Порядок**: инвентарь (multi-select по платформам) → custody (Vaultwarden коллекция — Other) →
отпечатки (Other) → ротация и резервная копия → `Generates`.

## compliance (W6)

**H2**: `Data collected`, `Permissions`, `Background and off-screen`, `SDK privacy manifests`,
`152-FZ`, `Age rating`, `Encryption`, `Legal documents`, `Checklist`, `Generates`.
**Generates**: `PrivacyInfo.xcprivacy`, permissions и `foregroundServiceType` в
`AndroidManifest.xml`, `UIBackgroundModes` и App Group, экраны `legal`, INFRA-HANDOFF I-6.
**Cross-kind**: permissions ← `permissions.md` карты; SDK ← `dependencies`/`push`/`analytics`;
URL ← `config` (runtime `links`); off-screen ← `background`, `widgets.md` (`lock`/`live`),
`system-actions.md` («на заблокированном экране»).
**Anti-patterns**: Data safety ≠ поведение; permission без обоснования; плейсхолдеры в политике;
ПДн в снимке App Group или на экране блокировки; точный будильник без обещанного момента.
**Порядок**: данные (таблица) → permissions из карты → фон и поверхности вне экранов (типы
foreground service, background modes, данные на локскрине, снимок App Group) → манифесты SDK →
152-ФЗ статус → рейтинг → шифрование → документы → чек-лист L-1…L-17 с датой → `Generates`.

## dependencies (W7)

**H2**: `Libraries`, `SDKs`, `Version policy exceptions`, `Seams`, `Generates`.
**Generates**: `gradle/libs.versions.toml`, `Package.swift` deps, строки `compliance.md`.
**Cross-kind**: → `compliance`; ← `push`, `background` (WorkManager, Glance, AppFunctions),
`observability`, `analytics`, `payments`, `styling`.
**Anti-patterns**: библиотека без лицензии/манифеста; зависимость без iOS-таргета в `commonMain`;
Firebase кроме FCM; не-stable зависимость без строки в `Version policy exceptions`.
**Порядок**: подтвердить базовую подборку шаблона → добавленные SDK (Other, по одному) →
исключения политики версий (причина + триггер снятия) → швы (`MapSurface` и др.) → `Generates`.

## config (W8, rollup)

**H2**: `Build-time`, `Release-time`, `Runtime`, `Requirements`, `Release checklist`, `Generates`.
**Generates**: `config/env/*.properties`, `iosApp/Config/*.xcconfig`, `.env.local.example`, `AppConfig`.
**Cross-kind**: читает все kinds: `auth` (`AUTH_MODE`), `deep-links` (хосты), `push`
(`PUSH_PROVIDERS`), `background`/`service` (`APP_GROUP_ID`), `observability` (`SENTRY_DSN`),
`signing` (release-time), `api` (`API_BASE_URL`).
**Anti-patterns**: секрет в build-time; ключ не во всех env; runtime-значение в build-time.
**Порядок**: таблица build-time per env (заполняется из kinds, дыры — Other) → release-time
(имена) → runtime (`requirements` схема, флаги) → чек-лист → `Generates`.

## analytics (условный, W4)

**H2**: `Provider`, `Events`, `Consent`, `Identifiers`, `Generates`.
**Generates**: инициализация SDK, `consent`, строки `compliance.md`/`dependencies.md`.
**Anti-patterns**: `AD_ID`/ATT; события с ПДн; без согласия.
**Порядок**: провайдер (enum AppMetrica / self-host / none) → реестр событий (Other) → согласие →
идентификаторы → `Generates`.

## payments (условный, W4)

**H2**: `Providers`, `Products`, `Restore`, `Server validation`, `Store rules`, `Generates`.
**Generates**: шов `Payments` в `core:platform`/`Core/Platform`, BACKEND-HANDOFF (валидация чеков).
**Anti-patterns**: внешняя оплата в приложении (3.1.1); без серверной валидации.
**Порядок**: провайдеры по платформам (multi-select) → продукты (Other) → восстановление →
валидация → правила магазинов → `Generates`.
