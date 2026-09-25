# Distribution

Факты магазинов для RU-first приложения, чек-лист «до публикации» и матрица каналов по
пресету. Значения проекта — `docs/stack/distribution.md`, `signing.md`, `compliance.md`;
процесс — [`release.md`](release.md); инварианты — rules `store-compliance`, `privacy-and-law`.

## Матрица каналов

```
Android
 ├── RuStore .............. основной канал РФ: бесплатно, ИП/ЮЛ/самозанятый, AAB или APK (≤ 5 ГБ), публикация по API, модерация 1–5 раб. дней, черновик живёт 14 дней
 ├── Google Play .......... вторичный, free-only: $25, organization + D-U-N-S, AAB обязателен, трек internal → production (staged rollout от 1 %, только обновления);
 │                          монетизация для расчётного счёта в РФ выключена; новый personal-аккаунт — 12 тестеров × 14 дней; developer verification (глобально 2027)
 ├── AppGallery / GetApps / Galaxy Store ... опция за флагом (доля устройств)
 └── Gitea Release APK .... внутренние сборки / заказчик; STOP-1 всегда; канал — preset enterprise

iOS
 ├── App Store ............ единственный продуктовый канал: Apple Developer Program $99/год (оплата из РФ — партнёр/юрлицо); IPA через App Store Connect; ревью 24–48 ч
 ├── TestFlight ........... internal (100, без review) → external (10 000, Beta App Review, сборка живёт 90 дней)
 └── Apple Business Manager ... custom apps вне магазина (preset enterprise)

Всегда
 └── GET /api/app/requirements — единственный надёжный «убийца» старых версий (min_version, maintenance)
```

Пресеты (`docs/stack/service.md#preset`): `companion` — RuStore + Play (free) + App Store,
demo-режим обязателен, без монетизации; `consumer` — + kinds `analytics`/`payments`, расширенный
чек-лист (рейтинг, SiwA при стороннем логине, ATT только по записи); `enterprise` — `gitea` +
`abm`, App Review-блоки выключены, demo необязателен.

## Аккаунты и сроки

| магазин | аккаунт | стоимость | узкое место |
|---|---|---|---|
| RuStore | VK ID; ИП/ЮЛ/самозанятый | бесплатно | RuStore Pay — только ИП/ЮЛ |
| Google Play | organization (+ D-U-N-S ~30 дней) | $25 | верификация; free-only для РФ |
| App Store | Apple Developer Program | $99/год | оплата из РФ; решение владельца до фазы iOS |

Владелец аккаунтов по умолчанию — заказчик, stit — admin (`distribution.md#accounts`).

## Дедлайны

| дата | требование |
|---|---|
| 2026-08-31 (продление до 2026-11-01) | Play: `targetSdk ≥ 36` для новых приложений и обновлений |
| 2026-09-30 → 2027 | Android developer verification: регионально, затем глобально (касается и sideload) |
| 2027-02 | Play: 16 KB page size (весь граф зависимостей); R8 DEX-оптимизация ≥ 25 % при DEX > 10 МБ |
| 2027-04 | App Store Connect: iOS 27 SDK; Play: Restore Credentials |

Даты и статус проекта — `docs/stack/distribution.md#deadlines`; `/release` напоминает
ближайшие в блоке `RISK` STORE-HANDOFF.

## Demo-режим — требование магазина

App Review требует «active demo account or fully-featured demo mode»: reference-фича `demo`
(flavor/схема `demo`, `MockTransport`, фикстуры `populated`) + runtime-переключатель в prod по
`requirements.flags.demo_login`; demo не ходит в сеть; demo-аккаунт живёт дольше ревью (L-17).
Описание — в App Review notes (`distribution.md`).

## Чек-лист «до публикации» (L-1…L-17)

Агент проверяет по списку перед `make release-preflight` (в `preflight.sh` этих проверок нет;
красный пункт — чинить код/манифест, не чек-лист):

| # | проверка |
|---|---|
| L-1 | в merged manifest нет `com.google.android.gms.permission.AD_ID` |
| L-2 | нет зависимостей из чёрного списка (`com.google.firebase:*` кроме FCM-транспорта, `com.onesignal:*`, `com.appsflyer:*`, …) без записи в `docs/stack/dependencies.md` |
| L-3 | все `NS*UsageDescription` в `Info.plist` непустые и не плейсхолдеры |
| L-4 | `ITSAppUsesNonExemptEncryption` присутствует |
| L-5 | роут `legal/privacy` достижим до логина |
| L-6 | экран удаления аккаунта достижим из настроек |
| L-7 | плейсхолдеры `{{ORG_NAME}}`, `{{ORG_INN}}`, `{{DPO_EMAIL}}` в правовых текстах заменены |
| L-8 | `targetSdk` ≥ актуального требования Play |
| L-9 | `PrivacyInfo.xcprivacy` присутствует и содержит `NSPrivacyAccessedAPITypes` |

Подтверждает человек (блок `LEGAL` STORE-HANDOFF):

| # | пункт | кто |
|---|---|---|
| L-10 | РКН уведомлён о намерении обрабатывать ПДн (ст. 22) | заказчик |
| L-11 | иностранные SDK → отдельное уведомление о трансграничной передаче | заказчик |
| L-12 | политика ПДн опубликована по постоянному HTTPS-URL, ссылка во всех карточках | оператор |
| L-13 | Data safety (Play) заполнена и совпадает с поведением | разработчик |
| L-14 | Privacy Nutrition Label (App Store) заполнена и совпадает | разработчик |
| L-15 | возрастной рейтинг одинаков во всех магазинах | разработчик |
| L-16 | URL веб-удаления аккаунта работает без установки приложения | разработчик |
| L-17 | demo-аккаунт для ревью создан, пароль в handoff, живёт дольше ревью | разработчик |

## Две подписи одного приложения

Play App Signing подписывает пользовательские сборки своим ключом, RuStore и direct-сборки —
релизным ключом проекта, загрузка в Play — upload-ключом. `assetlinks.json` содержит **все**
отпечатки массивом, иначе App Links верифицируются только у части пользователей; инвентарь —
`signing.md`, данные для `.well-known` — INFRA-HANDOFF I-1.

## Тест-дистрибуция без SaaS

Play internal (100 тестеров), TestFlight internal/external, RuStore закрытое тестирование, Gitea
Release + APK (до глобального этапа developer verification). Ad Hoc + `itms-services` (≤ 100
устройств, только HTTPS) — запасной вариант. Firebase App Distribution — нет.

## Обновление версий

Обязательное обновление — `requirements.status = update_required` (блокирующий экран + `store_url`);
in-app updates (Play Core, RuStore `appupdate`) — надстройка; на iOS — только ссылка в App Store.
Staged rollout Play: 10 % → 50 % → 100 % с проверкой crash-free между ступенями.
