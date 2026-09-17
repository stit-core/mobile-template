---
name: store-compliance
description: Требования магазинов как инварианты кода — App Review 4.8 (SiwA при стороннем логине), 5.1.1(v) удаление аккаунта, 3.1.3 оплата вне (companion), demo-доступ для ревью; Play Data safety, targetSdk-дедлайны, developer verification; RuStore модерация и 14 дней черновика; версии монотонны, отката нет. Грузится при правке манифеста, Info.plist, PrivacyInfo, distribution/compliance.md, auth-фич, legal.
paths:
  - "androidApp/app/src/main/AndroidManifest.xml"
  - "iosApp/App/Info.plist"
  - "iosApp/App/PrivacyInfo.xcprivacy"
  - "docs/stack/distribution.md"
  - "docs/stack/compliance.md"
  - "**/feature/auth/**"
  - "**/AuthFeature/**"
  - "**/legal/**"
---

# Магазины

Факты магазинов, сроки и чек-лист L-1…L-17 — `docs/reference/distribution.md`; значения
проекта — `docs/stack/{distribution,compliance}.md`. Здесь — что нельзя сломать кодом.

## Инварианты

- **Demo-режим — reference-фича №1**: App Review требует «fully-featured demo mode»;
  `demo`-flavor/схема + runtime-переключатель по `requirements.flags.demo_login` в prod;
  demo не ходит в сеть (тест). Demo-аккаунт живёт дольше, чем идёт ревью (L-17).
- **Удаление аккаунта — экран в приложении, достижимый из настроек** (App Store 5.1.1(v),
  L-6) + web-URL удаления для Play (L-16, `compliance.md#legal`). Регистрация без удаления —
  реджект.
- **Правовые документы доступны до логина** (`legal/privacy`, L-5): URL политики — в
  `requirements.links` и `PRIVACY_POLICY_URL` build-time.
- **Сторонний логин ⇒ Sign in with Apple** (4.8); по умолчанию соцвход выключен — включение
  = запись в `auth.md` + `compliance.md`.
- **Оплата**: `preset: companion` — платежей в приложении нет (3.1.3(c)); `consumer` —
  только IAP/StoreKit 2 + RuStore Pay + Play Billing (kind `payments`), никаких ссылок «оплатить
  на сайте» (3.1.1).
- **Play**: Data safety == реальное поведение (L-13); `targetSdk` ≥ актуального требования
  (`libs.versions.toml`, дедлайны — `distribution.md#deadlines`); AAB обязателен; free-only
  для расчётного счёта в РФ; developer verification — организационный процесс.
- **RuStore**: AAB или APK, модерация 1–5 рабочих дней, черновик живёт 14 дней; без
  `notification.title` пуш не показывается (rule `deep-links-and-push`).
- **App Store**: `PrivacyInfo.xcprivacy` валиден (лишние ключи — реджект), `NS*UsageDescription`
  непустые (L-3), Nutrition Label == поведение (L-14), iOS 27 SDK (Xcode 27).
- **Версии монотонны** (`versionCode`/`CFBundleVersion` из тега), отката нет; отклонение
  модерации → тег `Z+1` (rule `release-flow`).
- **Возрастной рейтинг** одинаков во всех магазинах (L-15); `AD_ID`/ATT не запрашиваются
  (rule `privacy-and-law`).
- Каждый новый permission/SDK/экран сбора данных — строка в `compliance.md` тем же
  коммитом; `/release` печатает L-10…L-17 человеку в STORE-HANDOFF.

## Сигналы

- `make release-preflight` красный на L-1…L-9 → чинить код/манифест, не чек-лист.
- Ревью-вопрос «как войти» → demo-режим не описан в App Review notes (`distribution.md`).
