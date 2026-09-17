---
name: privacy-and-law
description: 152-ФЗ и приватность — согласие до сбора, политика до логина, локализация ПДн; каждый SDK — запись в dependencies.md + privacy manifest; ANALYTICS=none по умолчанию; никаких AD_ID/ATT без записи; чек-лист L-1…L-17. Грузится при правке consent/, legal/, compliance.md, dependencies.md, PrivacyInfo.xcprivacy, libs.versions.toml.
paths:
  - "**/consent/**"
  - "**/legal/**"
  - "docs/stack/compliance.md"
  - "docs/stack/dependencies.md"
  - "**/PrivacyInfo.xcprivacy"
  - "gradle/libs.versions.toml"
---

# Приватность и право

Полный чек-лист L-1…L-17 и факты — `docs/reference/distribution.md`; статус проекта —
`docs/stack/compliance.md`. Здесь — инварианты для кода и зависимостей.

## Инварианты

- **Согласие до сбора**: крэш-репорты и любая аналитика — только после явного согласия,
  отдельными переключателями. Экрана согласия и флага в `SettingsKeys` в шаблоне нет:
  `SentryCrashReporter.init` стартует при непустом DSN (dev/demo — пусто); проект с DSN в
  prod заводит флаг перед инициализацией и строку в `compliance.md`.
- **Политика ПДн доступна до логина** (экран `legal`, URL из `requirements.links`); тексты
  политики/согласий — плейсхолдеры `{{ORG_NAME}}`, `{{ORG_INN}}`, `{{DPO_EMAIL}}`, которые
  `/de-template` требует заменить (L-7); хостинг URL — `<org>/infra` (INFRA-HANDOFF I-6).
- **`ANALYTICS=none` по умолчанию**; включение — `preset: consumer` или явное решение →
  kind `analytics`, согласие, строка Data safety / Nutrition Label.
- **`AD_ID` и ATT не запрашиваются**: `com.google.android.gms.permission.AD_ID` в merged
  manifest — красный (L-1); `NSUserTrackingUsageDescription` без записи в `compliance.md` —
  нет.
- **Каждый SDK — запись в `docs/stack/dependencies.md`** (назначение, лицензия, KMP-
  совместимость, privacy-manifest-статус) **и строка в `compliance.md`** (какие данные, куда
  уходят). Иностранный SaaS-SDK = трансграничная передача — отдельное уведомление РКН (L-11);
  чёрный список без записи (`com.google.firebase:*` кроме FCM-транспорта, `com.onesignal:*`,
  `com.appsflyer:*`) — L-2.
- **Privacy manifest**: `PrivacyInfo.xcprivacy` с `NSPrivacyAccessedAPITypes`
  (`UserDefaults` — required-reason API) и `NSPrivacyCollectedDataTypes`; манифесты
  зависимостей проверяются при добавлении.
- **Permission — только с обоснованием**: строка в карте `permissions.md` (strategy) с
  rationale → usage-description / rationale-экран → строка в `compliance.md`; запрос — в
  момент ценности, не на первом экране.
- **PII не логируется** (`beforeSend` Sentry на платформе, `sendDefaultPii=false`; `TelemetrySink`
  получает только `method path → status`); `user.id` в Sentry — только id.
- **Локализация ПДн**: приёмник крэшей и бэкенд — в РФ (GlitchTip self-host); данные в
  фикстурах `mock/` — синтетические.
- Возрастной рейтинг, D-U-N-S, уведомление РКН (ст. 22) — человек (L-10…L-17), фиксируется в
  `compliance.md` с датой проверки; `/release` печатает их в STORE-HANDOFF.

## Сигналы

- Новая строка в `libs.versions.toml` без строки в `dependencies.md` — стоп, сначала запись.
- L-1/L-2 (агент проверяет по чек-листу перед `make release-preflight`; в `preflight.sh`
  автоматики нет) → зависимость или permission без записи.
