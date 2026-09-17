# Release

**Зона репо: код → подписанные артефакты в Gitea Release (STOP-1) → подача в каналы (STOP-2).
Дальше — человек по STORE-HANDOFF.** Rollback не существует: откат = новая версия выше. Skill —
`/release`; инварианты — rule `release-flow`; значения проекта — `docs/stack/distribution.md`,
`signing.md`.

## Версия из тега

Аннотированный тег `vX.Y.Z` на чистом `main` — единственный источник истины:
`scripts/tools/version-from-tag.sh` печатает `APP_VERSION=X.Y.Z` и
`APP_BUILD=X·1 000 000 + Y·1 000 + Z` (`v1.4.2` → `1004002`); Gradle и xcconfig читают их из env
(dev-сборки — `0.0.0`/`1`). `versionName` = `CFBundleShortVersionString`, `versionCode` =
`CFBundleVersion` — монотонность даёт сам тег. Sentry `release` = `<applicationId>@X.Y.Z+<code>`
на обеих платформах. Сожжённая версия или отклонение модерации → `Z+1`, тот же процесс.

## Две ступени STOP

```
STOP-1 «артефакт готов и проверяем» — Gitea Release на теге vX.Y.Z
  ├─ <app>-X.Y.Z-<code>-prod.aab + .apk (подписаны upload-ключом), mapping-X.Y.Z-<code>.txt
  ├─ <app>-X.Y.Z-<code>-ios.ipa + -ios-dSYM.zip                      (при ios ∈ platforms)
  ├─ checksums.sha256 · release-notes.ru.md · release-notes.en.md · openapi.lock · store-handoff.md
  └─ символы (mapping / dSYM) загружены в GlitchTip/Sentry до публикации

STOP-2 «подано в каналы» — по docs/stack/distribution.md#channels
  ├─ Google Play: трек internal (service account)
  ├─ RuStore: черновик → загрузка → модерация (JWE-токен 900 с; черновик живёт 14 дней)
  └─ TestFlight: экспорт с App Store Connect API key (Mac-хост)

──────────────── дальше не зона репо ────────────────
  промоушен internal → production (crash-free ≥ 99.5 % за 48 ч), ответы модерации, листинги,
  Data safety / Nutrition Label — человек по STORE-HANDOFF
```

`dist/` (gitignored) — локальная стадия сборки, не STOP. `preset: enterprise` — каналы `gitea`
(APK как ассет) и `abm` (Apple Business Manager, человек); Play не используется.

## Вербы

| верб | что делает |
|---|---|
| `make release-preflight TAG=vX.Y.Z` | `main`, чистое дерево, аннотированный тег на HEAD == `TAG`, тег новее всех, `make doctor`, `GATE_RELEASE=1` гейт (R8), `check-mac` (кроме `PLATFORM=android`) |
| `make release-android TAG=vX.Y.Z` | `bundleProdRelease` + `assembleProdRelease` с подписью из env → `dist/`, `mapping.txt`, `apksigner verify`, `checksums.sha256`; загрузка mapping в GlitchTip — `sentry-cli upload-proguard` на шаге `/release` (Sentry Gradle-плагин не подключён) |
| iOS | на Mac-хосте: `xcodebuild archive -scheme Prod` (per-env схема проекта, заводится на Mac — `docs/reference/mac-host.md`) → `-exportArchive` → `dist/*.ipa` + `dSYM.zip`, `sentry-cli debug-files upload` (`.claude/skills/release/references/steps.md`) |
| STOP-1 | `git push origin vX.Y.Z`; Release через Gitea API, ассеты по одному; ntfy `releases-<org>` |
| STOP-2 | Play Developer API / RuStore API / App Store Connect по `--channels`; ntfy |

Полные команды, форма ассетов и STORE-HANDOFF — `.claude/skills/release/references/steps.md`.

## Секреты и custody

Ключи создаёт человек один раз и кладёт в Vaultwarden `mobile/<org>/<app>`; в репо — только
`*.example`. На время команды значения экспортируются в env (`.env.local` gitignored, агент его не
читает):

| переменная | нужна для |
|---|---|
| `STIT_KEYSTORE_PATH`, `STIT_KEYSTORE_PASSWORD`, `STIT_KEY_ALIAS`, `STIT_KEY_PASSWORD` | подпись AAB/APK (upload-ключ; RuStore — тот же или отдельный по `signing.md`) |
| `PLAY_SERVICE_ACCOUNT_JSON` | STOP-2 Play |
| `RUSTORE_KEY_ID`, `RUSTORE_PRIVATE_KEY` | STOP-2 RuStore |
| `ASC_KEY_ID`, `ASC_ISSUER_ID`, `ASC_KEY_P8`, `MATCH_GIT_URL`, `MATCH_PASSWORD` | TestFlight, подпись IPA |
| `SENTRY_AUTH_TOKEN`, `SENTRY_ORG`, `SENTRY_PROJECT_*`, `SENTRY_URL` | символы |
| `GITEA_TOKEN` (`write:repository`), `NTFY_TOPIC`, `NTFY_TOKEN`, `MAC_HOST` | STOP-1, уведомления, iOS-стадии |

Без `STIT_KEYSTORE_*` `build-android.sh` падает; гейт (`GATE_RELEASE=1`) подписывает
`assembleProdRelease` debug-ключом — R8 проверяется без секретов. Upload-key ≠ Play app-signing
key ≠ RuStore-ключ — все отпечатки в `signing.md`, `deep-links.md` и `assetlinks.json`
(`wellknown`). Потеря релизного keystore RuStore = новое приложение — резервная копия на втором
носителе обязательна; сброс upload-ключа Play — процедура Play Console.

## STORE-HANDOFF

`/release` печатает блок (`ARTIFACTS / PLAY / RUSTORE / TESTFLIGHT / BACKEND / ENV / LEGAL / RISK`,
внутри — WHAT/WHY/ACTION/WHO/DECIDE/RISK) в чат, в `dist/store-handoff.md` (ассет STOP-1) и
секцией с человеческими шагами в `.claude/plans/roadmap.md`. `BACKEND` и `ENV` агрегируют
`**Infra impact.**` активных планов (`min_version` после раскатки, отпечатки в `wellknown`,
`SENTRY_DSN`). Чек-лист магазинов L-10…L-17 — `docs/reference/distribution.md`.

## Что после релиза

Промоушен трека и ответы модерации — человек; крэши — GlitchTip по `release`; следующий релиз —
новый тег. Baseline Profile (`androidApp/baselineprofile`, GMD/устройство) — шов вне v1:
`baseline-prof.txt` как необязательный ассет.
