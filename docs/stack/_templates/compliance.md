---
id: stack.compliance
kind: compliance
name: compliance
status: draft
---

# compliance

## Data collected

Таблица = Data safety (Play) = Privacy Nutrition Label (App Store) = реальное поведение.

| данные | зачем | куда уходит | обязательно | шифрование в пути | удаляемо |
|---|---|---|---|---|---|
| e-mail, пароль (хеш на сервере) | вход | `<org>/backend` | да | TLS | да (удаление аккаунта) |
| crash-репорты (без PII) | стабильность | GlitchTip (self-host, РФ) | нет (согласие) | TLS | — |
| push-токен, `installation_id` | уведомления | `<org>/backend` | нет | TLS | при logout |

## Permissions

Каждая строка — из карты `permissions.md` (strategy) с rationale.

| permission | Android id | iOS usage-key | зачем (пользователю) | когда | данные |
|---|---|---|---|---|---|
| notifications | `POST_NOTIFICATIONS` (`core/platform`) | `UNUserNotificationCenter` (без usage-key) | _…_ | pre-prompt `PushOptIn`, в момент ценности | push-токен |
| biometric-unlock | `USE_BIOMETRIC` (`core/platform`) | — (в шаблоне нет) | замок приложения при живой сессии (`AUTH_BIOMETRIC_UNLOCK`) | при старте, если включено | — |
| network-state | `ACCESS_NETWORK_STATE` (`core/platform`) | — | offline-баннер и read-only офлайн (`AndroidConnectivityMonitor`) | всегда | — |
| exact-alarm | `SCHEDULE_EXACT_ALARM` (`core/platform`) | — | _напоминание приходит в обещанный пользователем момент (`background-tasks` `timing: exact`)_ | при включении напоминания; не выдано ⇒ момент приблизительный | _—_ |
| _<slug>_ | _…_ | _`NS…UsageDescription`_ | _…_ | _…_ | _…_ |

`AD_ID` и ATT не запрашиваются. Точные будильники заводятся только под момент, обещанный
пользователю, — магазины спрашивают обоснование.

## Background and off-screen

Поверхности вне экранов приложения: фоновые задачи (`background.md`), виджеты и системные
действия. Механика — `background.md`, здесь — что это значит для ревью и приватности.

| что | значение проекта |
|---|---|
| типы foreground service | _`dataSync` для задач `continuous`/`user-started`; тип обязателен, `onTimeout` реализован_ |
| iOS background modes | _`remote-notification` при `remote-event`; остальные не объявлены_ |
| данные на экране блокировки | _виджеты `lock`/`live` показывают только неличные поля; чувствительное скрыто до разблокировки_ |
| снимок для виджетов | _JSON в `<filesDir>/cache/widget-<slug>.json` (на iOS — контейнер App Group `group.<bundle-id>`): только показываемые поля, без токенов и ПДн; расширение в сеть не ходит_ |
| действия без сессии | результат `SignedOut` — приложение предлагает войти, токен наружу не отдаётся |
| голосовые команды | _через ассистента системы (реестр AppFunctions на Android, интенты на iOS): наружу уходит текст задачи и короткое подтверждение; собственного прослушивания микрофона нет_ |
| сервис реестра действий | _экспортирован под `BIND_APP_FUNCTION_SERVICE` — поднять его может только система_ |

## SDK privacy manifests

| SDK | privacy manifest | данные | трансграничная передача |
|---|---|---|---|
| sentry-cocoa / sentry-android | есть | crash | нет (self-host в РФ) |
| _провайдер push_ | _…_ | _токен_ | _…_ |

## 152-FZ

| пункт | статус |
|---|---|
| уведомление РКН (ст. 22) | _подано / дата_ |
| локализация ПДн | бэкенд и приёмник крэшей — в РФ |
| согласия | отдельными переключателями до сбора (крэш-репорты, аналитика) |
| трансграничка | _нет иностранных SDK / уведомление подано_ |

## Age rating

_одинаков во всех магазинах: …_

## Encryption

`ITSAppUsesNonExemptEncryption = false` (только TLS); ATS без исключений; `cleartextTrafficPermitted = false`.

## Legal documents

| документ | URL | где показывается |
|---|---|---|
| политика обработки ПДн | _`https://<domain>/privacy`_ (INFRA-HANDOFF I-6) | экран `legal` до логина; карточки магазинов |
| веб-удаление аккаунта | _`https://<domain>/account/delete`_ | Play Data safety; экран удаления |
| согласия | тексты в приложении, плейсхолдеры `{{ORG_NAME}}`, `{{ORG_INN}}`, `{{DPO_EMAIL}}` заменены на `/de-template` (L-7) | первый запуск |

## Checklist

Агент перед `release-preflight` (в `preflight.sh` автоматики нет): L-1 нет `AD_ID` · L-2 нет SDK из чёрного списка без записи ·
L-3 `NS*UsageDescription` непустые · L-4 `ITSAppUsesNonExemptEncryption` · L-5 `legal/privacy`
до логина · L-6 экран удаления аккаунта · L-7 плейсхолдеры заменены · L-8 `targetSdk` актуален ·
L-9 `PrivacyInfo.xcprivacy` валиден. Человек (STORE-HANDOFF): L-10 РКН · L-11 трансграничка ·
L-12 политика по URL в карточках · L-13 Data safety · L-14 Nutrition Label · L-15 рейтинг ·
L-16 URL удаления работает без приложения · L-17 demo-аккаунт живёт дольше ревью.
Дата последней проверки: _…_

## Generates

- `iosApp/App/PrivacyInfo.xcprivacy` (`NSPrivacyAccessedAPITypes`, `NSPrivacyCollectedDataTypes`)
- `androidApp/{app,core/platform}/src/main/AndroidManifest.xml` — `<uses-permission>` по таблице (в модуле, который использует), тип `<service android:foregroundServiceType>` по таблице фона
- `iosApp/App/Info.plist` — `UIBackgroundModes`; `App.entitlements` — App Group обоих таргетов
- ограничение полей снимка виджета (`<Slug>WidgetSnapshot`) и `<Slug>Result.SignedOut` для действий без сессии
- правовые ссылки — секция «О приложении» в `feature/settings` из `requirements.links`; экран согласия — шов (в шаблоне нет)
- INFRA-HANDOFF I-6 (URL политики и удаления), блок `LEGAL` STORE-HANDOFF

## Открытые вопросы

- _что_
