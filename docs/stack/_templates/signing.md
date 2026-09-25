---
id: stack.signing
kind: signing
name: signing
status: draft
---

# signing

## Inventory

| ключ | платформа | где хранится (вне репозитория) | потеря = |
|---|---|---|---|
| Android upload keystore (`.jks`) + пароли | Android | _<где и у кого>_; в сборку — env `STIT_KEYSTORE_*` | восстановимо через Play Console (при Play App Signing) |
| Android release keystore для RuStore | Android | _<где и у кого>_ + офлайн-копия у владельца | **новое приложение в магазине** — самый ценный секрет |
| Google Play service account JSON | Android | _<где и у кого>_; env `PLAY_SERVICE_ACCOUNT_JSON` | отзываемо |
| RuStore key id + private key | Android | _<где и у кого>_; env `RUSTORE_KEY_ID`, `RUSTORE_PRIVATE_KEY` | перевыпускается |
| Apple distribution cert + provisioning profiles | iOS | `fastlane match` → приватный репо `<org>/ios-certificates`; пароль `MATCH_PASSWORD` — _<где и у кого>_ | `match nuke` + перевыпуск |
| App Store Connect API key (`.p8`) + issuer id | iOS | _<где и у кого>_; env `ASC_*` | перевыпускается |
| APNs auth key (`.p8`) + key id + team id | бэкенд | _<на стороне бэкенда>_ (≤ 2 team-ключей на env) | перевыпускается |

## Custody

Ключи создаёт человек один раз (`keytool` / Apple Developer portal / `match init`) и хранит вне
репозитория — место и владелец каждого ключа в таблице выше; в репо — только `*.example`;
подпись — из env на время `make release-*` (`STIT_KEYSTORE_PATH`, `STIT_KEYSTORE_PASSWORD`,
`STIT_KEY_ALIAS`, `STIT_KEY_PASSWORD`). Резервная копия релизного keystore на втором носителе —
пункт онбординга.

## Fingerprints

Upload-key ≠ Play app-signing key ≠ RuStore-ключ — все три SHA-256 в `deep-links.md#fingerprints`
и в `assetlinks.json` (раздаёт `<org>/infra` по INFRA-HANDOFF I-1).

| ключ | SHA-256 |
|---|---|
| upload | _…_ |
| app-signing (Play) | _…_ |
| RuStore | _…_ |

## Rotation

Ротация/перевыпуск — запись здесь + INFRA-HANDOFF I-1 (новые отпечатки в `assetlinks.json`);
сброс upload-ключа — процедура Play Console; RuStore — новый ключ в консоли.

## Generates

- `configureReleaseSigning` (`build-logic`) — `signingConfigs` из env `STIT_KEYSTORE_*`
- `.env.local.example` — имена release-time ключей; `iosApp/Config/Local.xcconfig.example` — team
- чек `/de-template`: отпечатки здесь == `deep-links.md`
- INFRA-HANDOFF при ротации

## Открытые вопросы

- _что_
