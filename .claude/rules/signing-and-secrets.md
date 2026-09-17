---
name: signing-and-secrets
description: Ключи подписи и store-credentials только в Vaultwarden mobile/<org>/<app>; в репо — *.example; подпись из env при make release-*; upload-key ≠ app-signing-key, оба отпечатка в assetlinks; ITSAppUsesNonExemptEncryption; ATS без исключений; cleartext off. Грузится при правке app/build.gradle.kts, entitlements, Info.plist, Config/, scripts/release/, *.example.
paths:
  - "androidApp/app/build.gradle.kts"
  - "iosApp/**/*.entitlements"
  - "iosApp/App/Info.plist"
  - "iosApp/Config/**"
  - "scripts/release/**"
  - ".env.local.example"
  - "local.properties.example"
---

# Подпись и релизные секреты

Инвентарь ключей и custody — `docs/stack/signing.md`; процедура релиза —
`docs/reference/release.md`.

## Инварианты

- **Ключи живут в Vaultwarden, коллекция `mobile/<org>/<app>`** (keystore — вложение, пароли
  — поля; Apple-сертификаты — `fastlane match` в приватном репо `<org>/ios-certificates`).
  В репозитории — только `*.example` (`local.properties.example`,
  `iosApp/Config/Local.xcconfig.example`; `.env.local.example` с именами release-time ключей
  пишет `/de-template`).
- **Никогда не коммитятся**: `*.jks`, `*.keystore`, `*.p12`, `*.p8`, `*.mobileprovision`,
  `*.cer`, `local.properties`, `Local.xcconfig`, `.env.local`, `google-services.json` с
  prod-ключами — `.gitignore` + `pre-commit` + `permissions.deny` на чтение/правку.
- **Подпись — только из env процесса** (`STIT_KEYSTORE_PATH`, `STIT_KEYSTORE_PASSWORD`,
  `STIT_KEY_ALIAS`, `STIT_KEY_PASSWORD` — `configureReleaseSigning` в `build-logic`); значения
  экспортирует человек на время `make release-android`. Без env `assembleProdRelease` в гейте
  подписывается debug-ключом (R8 проверяется без секретов), а `build-android.sh` падает.
- **`keytool`, `bw`, `altool`, `fastlane` — не агент**: генерация ключей и экспорт из
  Vaultwarden — человек; `permissions.deny`.
- **Upload-key ≠ app-signing-key** (Play App Signing) ≠ ключ RuStore: все отпечатки
  SHA-256 — в `docs/stack/signing.md` и `deep-links.md`, в `assetlinks.json` — массивом
  (иначе App Links «verified: false» на части сборок). Ротация — запись в `signing.md` +
  INFRA-HANDOFF (`wellknown`).
- **Потеря релизного keystore RuStore = новое приложение**: резервная копия на втором
  носителе — пункт онбординга (`signing.md#custody`).
- **`Info.plist`**: `ITSAppUsesNonExemptEncryption` задан; ATS без `NSAllowsArbitraryLoads`;
  `NS*UsageDescription` непустые и не плейсхолдеры. **Android**: `usesCleartextTraffic`
  запрещён в release; `debug-overrides` в NSC только для dev-корня.
- В `.env.local` — только релизные переменные (`docs/reference/release.md#секреты`); агент
  их не читает (`Read(.env.local)` — deny) и в чат не цитирует; нужен факт «задано» —
  `make release-preflight` проверяет сам.

## Сигналы

- `pre-commit: в коммит попали ключи/локальный конфиг` → убрать из индекса, ротировать, если
  файл уже был в истории.
- `apksigner verify` в `build-android.sh` без `Verifies` → подписано не тем ключом.
