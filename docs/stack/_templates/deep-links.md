---
id: stack.deep-links
kind: deep-links
name: deep-links
status: draft
---

# deep-links

## Hosts and schemes

| env | https-хост | кастомная схема | Android app id | iOS team id · bundle id |
|---|---|---|---|---|
| dev | `app.dev.<domain>` | `<scheme>-dev` | `<applicationId>.dev` | `<TEAM>.<bundle>.dev` |
| staging | `app.staging.<domain>` | `<scheme>-staging` | `<applicationId>.staging` | `<TEAM>.<bundle>.staging` |
| prod | `app.<domain>` | `<scheme>` | `<applicationId>` | `<TEAM>.<bundle>` |

`demo` — хост `app.demo.invalid`, ссылки не верифицируются. Кастомная схема — fallback для
писем/QR; в push и вебе — только https.

## Fingerprints

Все отпечатки — массивом в `assetlinks.json` (иначе App Links не верифицируются на части сборок).

| ключ | SHA-256 | источник |
|---|---|---|
| upload | _…_ | `signing.md#inventory` |
| app-signing (Play) | _…_ | Play Console |
| RuStore | _…_ | RuStore Console |

## Paths to Route

| путь | Route | требует сессии | pending после логина | тест |
|---|---|---|---|---|
| `/todos/{id}` | `TodoDetail(id)` | да | да | `DeepLinkParserTest` |
| _<путь>_ | _<Route>_ | _да/нет_ | _да/нет_ | _тест_ |

Проверка на устройстве: `adb shell pm verify-app-links --re-verify <pkg>`,
`adb shell am start -W -a android.intent.action.VIEW -d "https://app.dev.<domain>/todos/42" <pkg>`;
симулятор — `xcrun simctl openurl booted "https://app.dev.<domain>/todos/42"`.

## Generates

- `androidApp/app/src/main/AndroidManifest.xml` — intent-filter `autoVerify` с `${DEEP_LINK_HOST}` + `pathPrefix` по таблице путей и custom scheme `${DEEP_LINK_SCHEME}` (плейсхолдеры из `config/env`)
- `iosApp/App/App.entitlements` — `applinks:$(DEEP_LINK_HOST)`; `App/Info.plist` — `CFBundleURLSchemes $(DEEP_LINK_SCHEME)` (значения — `iosApp/Config/*.xcconfig`)
- `shared/…/core/deeplink/{Route,DeepLinkParser}.kt` + `DeepLinkParserTest` на каждую строку «Paths to Route»; стадия `deeplinks-check` сверяет манифест, env и тесты с этой таблицей
- `config/env/*.properties`, `iosApp/Config/*.xcconfig` — `DEEP_LINK_HOST`, `DEEP_LINK_SCHEME`
- INFRA-HANDOFF → `<org>/infra`: `MOBILE_ANDROID_APP_ID`, `MOBILE_ANDROID_SHA256_{UPLOAD,PLAY,RUSTORE}`,
  `MOBILE_IOS_TEAM_ID`, `MOBILE_IOS_BUNDLE_ID` для сервиса `wellknown` (per env)

## Открытые вопросы

- _что_
