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
| _<slug>_ | _…_ | _`NS…UsageDescription`_ | _…_ | _…_ | _…_ |

`AD_ID` и ATT не запрашиваются.

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
- `androidApp/{app,core/platform}/src/main/AndroidManifest.xml` — `<uses-permission>` по таблице (в модуле, который использует)
- правовые ссылки — секция «О приложении» в `feature/settings` из `requirements.links`; экран согласия — шов (в шаблоне нет)
- INFRA-HANDOFF I-6 (URL политики и удаления), блок `LEGAL` STORE-HANDOFF

## Открытые вопросы

- _что_
