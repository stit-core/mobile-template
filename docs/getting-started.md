# Getting started — от шаблона к своему приложению

**[META]** Этот файл — для потребителя шаблона. Описывает путь от форка до проекта, который
ничего не помнит про шаблон. Удаляется на шаге де-темплейтизации.

Принцип: **не правь фундамент руками — сначала спроектируй, потом дай скиллам развернуть.**
Карта продукта живёт в `<org>/strategy`, платформа — в `docs/stack/`; код пишется по ним.
Путь **Android-first**: всё до релиза в RuStore/Play выполнимо на Linux; iOS-ветка ждёт Mac-хост.

## 0. Предусловия

| что | зачем |
|---|---|
| Linux-машина: JDK 21 (Temurin), Android SDK (`platforms/android-37.2`, build-tools, platform-tools, `cmdline-tools` с `android` CLI), Swift 6.4 + SwiftLint в `PATH`; RAM ≥ 6 ГБ, диск ≥ 15 ГБ | гейт и Android-релиз целиком; Swift-стадии Linux-части |
| `export JAVA_HOME=… ANDROID_HOME=… SWIFT_TOOLCHAIN=…` (или `local.properties` из `local.properties.example`) | `make doctor` печатает next-step, если чего-то нет |
| Соседи sibling-checkout'ами: `../strategy` (форк `business-strategy-template`, поверхность `type: mobile` в `surface.md`), `../backend` (`api/openapi.yaml`), `../infra` (операционный хаб проекта) | карта, контракт, адресат INFRA-HANDOFF (`.well-known`, хосты, DSN); соседа нет — скиллы печатают next-step |
| `OPENAPI_GENERATOR_JAR` (openapi-generator-cli 7.25.0) + `python3` с PyYAML (`python3-yaml`) | `make gen` (модели из спеки соседа) и полная `make gen-check`; без них `gen-check` сверяет только lock; после `make gen` — `make api-dump` (модели — часть фасада `shared`) |
| **Mac build-host** (Apple silicon, Xcode 27) по SSH — **предусловие iOS-ветки** (`docs/reference/mac-host.md`) | линковка `Shared.framework`, `Core`/`Features`, snapshot-тесты, IPA; без него iOS помечен «не верифицирован» |
| Аккаунты магазинов: RuStore (бесплатно, сразу), Google Play ($25, D-U-N-S ~30 дней, verification), Apple Developer Program ($99/год, оплата из РФ — решить заранее) | STOP-2; сроки — `docs/reference/distribution.md` |
| Место для ключей подписи и store-credentials вне репозитория (решение владельца; инвентарь — `docs/stack/signing.md`) | на время релиза значения экспортируются в env (`STIT_KEYSTORE_*` и др.) |

## 1. Форк

1. Gitea → `mobile-template` → **Use this template** → `<org>/mobile`.
2. `git clone git@<gitea>:<org>/mobile.git ~/git/<org>/mobile` — рядом с `strategy`, `backend`, `infra`.
3. `make setup` — doctor + git-хуки (`core.hooksPath`) + официальные Android-скиллы
   (`.claude/skills/<id>/`, gitignored) + прогрев демона Gradle.
4. `make check` — зелёный Linux-гейт с явным SKIP-блоком macOS-стадий.

## 2. `/smartdev` → `/init-service`

В Claude Code: `/smartdev` — `make doctor` + `make status`; на свежем форке cockpit переключит в
`/init-service`. Оркестратор ведёт стадии 3–7, сам не проектирует и не де-темплейтит. Обычно
онбординг запускает агент из операционного хаба `<org>/infra` по общему плану проекта; прямой
запуск здесь — тот же путь с той же обвязкой.

## 3. Идентичность и env — `docs/stack/service.md`, `config.md`

`project-slug`, имя, `applicationId`/bundle id per env, `platforms: [android, ios]` (или одна),
`preset: companion | consumer | enterprise`, `auth.mode`, схема deep link, локали, хосты API,
`SENTRY_DSN`-политика, `PUSH_PROVIDERS`, `ANALYTICS`. Значения — только от тебя; дефолты шаблона
предлагаются как опции.

## 4. Карта приложения — в `<org>/strategy`

Слой `docs/projects/<slug>/mobile/<surface>/` — 12 kinds (entities · screens · flows · features ·
shell · entry-points · permissions · system-actions · widgets · background-tasks · shared-api ·
shared-lib), ведёт `/mobile-deep-dive` там. Нужны stable-записи для первого разворота.
Канона `mobile/` в форке strategy нет — путь без PR:

```bash
cp -r docs/reference/strategy-mobile-canon/. ../strategy/docs/projects/_template/mobile/
mkdir -p ../strategy/docs/projects/<slug>/mobile/<surface>
cp ../strategy/docs/projects/_template/mobile/_template/*.md ../strategy/docs/projects/<slug>/mobile/<surface>/
ls ../strategy/docs/projects/<slug>/mobile/<surface>/*.md | wc -l   # 12 форм
```

Форма — [reference/strategy-mobile-canon/README.md](reference/strategy-mobile-canon/README.md);
маппинг в код — [reference/strategy-mapping.md](reference/strategy-mapping.md). Соседа нет —
`/scaffold --from-args` со строкой в `.claude/plans/backlog.md` «завести карту».

## 5. Стек — `/stack-deep-dive`

14 обязательных kinds + `analytics`/`payments` по пресету ([stack/README.md](stack/README.md)):
волны W1…W8 вопросами, каждый артефакт — до `status: stable` с конкретной `## Generates`;
cross-kind-инварианты 1–8. При `platforms: [android]` iOS-секций нет.

## 6. Фундамент — `/de-template`

Gate: стек `stable`, карта stable (или `--no-strategy`), `make check` зелёный, при iOS —
`make check-mac` или `--ios-unverified` (долг в `.claude/plans/backlog.md`). Phase A: пакет `tech.stit.mobile` →
свой, `applicationId`/bundle id, конфиги env/xcconfig, README, обрезка платформы
(`--platform android|ios`), strip меты. Phase B: prune reference-фич `profile`/`todos`
(остаются `demo`, `shell`, `requirements`, `auth`, `settings`, `push`), пресетные kinds, docs.
Затем `/scaffold` по stable-записям карты (entity / feature / screen / flow / entry-point /
permission / system-action / widget / background-task / api).

## 7. Handoffs соседям — `/infra-handoff`

- **`<org>/backend`** (BACKEND-HANDOFF): `POST /api/auth/refresh` + `X-Client` (Bearer),
  `GET /api/app/requirements`, `PUT /api/v1/devices`, `UserView.version` (B-20), закоммиченная
  `api/openapi.yaml`; до них
  работают fallback'и (`AuthMode.Cookie`, reference-спека, 404 ⇒ «требований нет»).
- **`<org>/infra`** (INFRA-HANDOFF): раздача `.well-known` на хостах deep links
  (`assetlinks.json` — `applicationId` и SHA-256 отпечатки, AASA — Team ID · bundle id; per env),
  `SENTRY_DSN`, URL политики ПДн и веб-удаления аккаунта.
- **`<org>/strategy`** (STRATEGY-HANDOFF): расхождение кода с картой.

Блоки — в ответ (юзеру или хабу `<org>/infra`) и в раздел `## Handoff` плана; из этого репо в
соседей не пишут.

## 8. Первый релиз — `/release --stop 1`

`make check-full` (или `GATE_RELEASE=1 make check` при `--platform android`) → аннотированный тег
`vX.Y.Z` → `make release-preflight` → `make release-android` → Gitea Release с ассетами (STOP-1)
→ `--stop 2`: Play internal / RuStore модерация / TestFlight → STORE-HANDOFF человеку.
Подробно — [reference/release.md](reference/release.md).

## Готово

Проект больше не помнит про шаблон: свой пакет и id, свои экраны по карте, стек `stable`.
Дальше — `/smartdev` → `/plan` → `/scaffold`; iOS-верификация — при появлении Mac-хоста
(`make check-mac` по всему, атомы «Mac-verify»). Устройство `.claude`-системы —
[contributing.md](contributing.md).
