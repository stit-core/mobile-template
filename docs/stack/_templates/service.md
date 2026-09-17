---
id: stack.service
kind: service
name: service
status: draft
---

# service

## Identity

| поле | значение |
|---|---|
| name | _<имя приложения>_ |
| description | _<одно предложение: что это за клиент и для кого>_ |
| package | `tech.stit.mobile` _(→ `<reverse-domain>.<app>` на `/de-template`)_ |
| applicationId | `tech.stit.mobile` · dev `.dev` · staging `.staging` · demo `.demo` · debug-build `.debug` |
| bundle id | `tech.stit.mobile` · dev `.dev` · staging `.staging` · demo `.demo` |
| deep-link scheme | `stit` _(`DEEP_LINK_SCHEME`)_ |
| locales | `ru` (default), `en` |
| store accounts owner | _заказчик; stit — admin_ |

## Platforms

| поле | значение |
|---|---|
| platforms | `[android, ios]` |
| preset | `companion` _(`companion` \| `consumer` \| `enterprise`)_ |
| auth.mode | `bearer` _(`bearer` \| `cookie` \| `none`; детали — `auth.md`)_ |
| ios verified | _`make check-mac` на HEAD / не верифицирован (долг в roadmap)_ |

## Ecosystem

Sibling-checkout'ы форков одной org (`~/git/<org>/…`); соседа нет — скиллы печатают next-step.

| поле | значение |
|---|---|
| project-slug | _папка продукта в `<strategy-path>/docs/projects/<slug>/`_ |
| strategy-path | `../strategy` |
| backend-path | `../backend` |
| infra-path | `../infra` |
| surface | _slug поверхности `type: mobile` из `surface.md` strategy_ |

## Generates

- `androidApp/app/build.gradle.kts` — `applicationId`, `applicationIdSuffix` (`demo`)
- `config/env/*.properties` — `DEEP_LINK_SCHEME`
- `iosApp/Config/*.xcconfig` — `PRODUCT_BUNDLE_IDENTIFIER` per схема
- `README.md` — шапка проекта
- поля `platforms` / `preset` / `strategy-path` / `backend-path` / `surface` — читают `/scaffold`,
  `/de-template`, `/release`, `make status`

## Открытые вопросы

- _что_
