---
id: stack.signing
kind: signing
name: signing
status: draft
---

# signing

## Inventory

| ключ | платформа | где хранится | потеря = |
|---|---|---|---|
| Android upload keystore (`.jks`) + пароли | Android | Vaultwarden `mobile/<org>/<app>` · `android-upload-key` (файл — вложение) | восстановимо через Play Console (при Play App Signing) |
| Android release keystore для RuStore | Android | Vaultwarden + офлайн-копия у владельца | **новое приложение в магазине** — самый ценный секрет |
| Google Play service account JSON | Android | Vaultwarden · `play-service-account` | отзываемо |
| RuStore key id + private key | Android | Vaultwarden · `rustore-api-key` | перевыпускается |
| Apple distribution cert + provisioning profiles | iOS | `fastlane match` → приватный репо `<org>/ios-certificates`; пароль — Vaultwarden · `apple-signing` | `match nuke` + перевыпуск |
| App Store Connect API key (`.p8`) + issuer id | iOS | Vaultwarden · `apple-asc-api-key` | перевыпускается |
| APNs auth key (`.p8`) + key id + team id | бэкенд | Vaultwarden · `apns-key` (≤ 2 team-ключей на env) | перевыпускается |

## Custody

Ключи создаёт человек один раз (`keytool` / Apple Developer portal / `match init`), кладёт в
Vaultwarden; в репо — только `*.example`; подпись — из env на время `make release-*`
(`STIT_KEYSTORE_PATH`, `STIT_KEYSTORE_PASSWORD`, `STIT_KEY_ALIAS`, `STIT_KEY_PASSWORD`).
Резервная копия релизного keystore на втором носителе — пункт онбординга.

## Fingerprints

Upload-key ≠ Play app-signing key ≠ RuStore-ключ — все три SHA-256 в `deep-links.md#fingerprints`
и в `assetlinks.json` (`wellknown`).

| ключ | SHA-256 |
|---|---|
| upload | _…_ |
| app-signing (Play) | _…_ |
| RuStore | _…_ |

## Rotation

Ротация/перевыпуск — запись здесь + INFRA-HANDOFF (отпечатки `MOBILE_ANDROID_SHA256_*` в
`wellknown`); сброс upload-ключа — процедура Play Console; RuStore — новый ключ в консоли.

## Generates

- `configureReleaseSigning` (`build-logic`) — `signingConfigs` из env `STIT_KEYSTORE_*`
- `.env.local.example` — имена release-time ключей; `iosApp/Config/Local.xcconfig.example` — team
- чек `/de-template`: отпечатки здесь == `deep-links.md`
- INFRA-HANDOFF при ротации

## Открытые вопросы

- _что_
