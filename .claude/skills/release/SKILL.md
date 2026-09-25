---
name: release
description: Выпустить версию мобильного приложения в магазины — аннотированный тег vX.Y.Z, preflight (check-full на HEAD, версия монотонна, чек-лист магазинов), подписанные артефакты (AAB/APK на Linux, IPA/dSYM на Mac-хосте), символы в трекер ошибок (sentry-cli), STOP-1 = Gitea Release с ассетами, STOP-2 = подача в каналы (Play internal / RuStore модерация / TestFlight), STORE-HANDOFF человеку. Флаги --platform android|ios|both, --stop 1|2, --channels, --dry-run. Применяй, когда юзер просит выпустить или выкатить версию (релиз, тег vX.Y.Z, /release); собрать APK «посмотреть» — не релиз.
---

# Skill: release

Канонический release-флоу. **Зона репо: код → подписанные артефакты в Gitea Release (STOP-1) →
подача в каналы (STOP-2). Дальше — человек по STORE-HANDOFF.** Rollback не существует: откат =
новая версия выше. Процесс, секреты и формы — `docs/reference/release.md`; шаги и ассеты —
`references/steps.md`; инварианты — rule `release-flow`.

Флаги: `--platform android|ios|both` (дефолт — `docs/stack/service.md#platforms`), `--stop 1|2`
(дефолт 2), `--channels gitea,play,rustore,testflight` (дефолт — `distribution.md#channels`),
`--dry-run`.

## Когда применять

- «Релизим v1.2.3» / «катим версию» / «/release»
- После закрытия задачи, зелёного `make check-full` и явного «пора катить»

## Когда не применять

- «Собери APK посмотреть» → `make build` (dev-debug), не релиз.
- Feature-ветка / грязное дерево → preflight отвергнет.
- Юзер не озвучил OK на релиз → остановись и спроси: тег и загрузки в магазины необратимы.

## Pre-flight (до команд)

Начинай, только когда закрыт каждый пункт:

- [ ] Юзер явно сказал OK и назвал версию `vX.Y.Z` (не угадывать; semver-bump: patch — фикс,
      minor — новые экраны/поля, major — несовместимость с бэкендом/`min_version`).
- [ ] `make status`: `main`, working tree clean, `Handoffs: none` или блоки уже в плане.
- [ ] `make check-full` зелёный на этом HEAD (`build/gate-logs/mac.last` == HEAD при iOS);
      `--platform android` — достаточно `GATE_RELEASE=1 make check`.
- [ ] Чек-лист L-1…L-9 пройден агентом по `docs/reference/distribution.md` (в `preflight.sh`
      этих проверок нет); L-10…L-17 — подтверждены человеком поимённо, скилл спрашивает через
      `AskUserQuestion`.
- [ ] Секреты экспортированы человеком в env текущей сессии (`STIT_KEYSTORE_*`, `GITEA_TOKEN`,
      каналы — по `--channels`; таблица — `references/steps.md`); значения агент не читает.
- [ ] iOS: `MAC_HOST` доступен (`make mac-doctor`), профили/сертификаты на хосте.
- [ ] Handoff активных планов собран (`bash scripts/tools/validate-plan.sh --handoff`) — уедет в STORE-HANDOFF (`BACKEND`, `ENV`).

## Шаги

| # | шаг | где | команда |
|---|---|---|---|
| 0 | preflight: `main`, чистое дерево, аннотированный тег на HEAD == `TAG`, тег новее всех, `make doctor`, `GATE_RELEASE=1` гейт, `check-mac` (кроме `PLATFORM=android`) | Linux | `make release-preflight TAG=vX.Y.Z` (`PLATFORM=android` при `--platform android`) |
| 1 | аннотированный тег `vX.Y.Z` на HEAD — ставит человек по явному OK (`git tag -a vX.Y.Z -m "<release notes>"`); push тега — после STOP-1 | Linux | человек |
| 2 | Android: `bundleProdRelease` + `assembleProdRelease` с подписью из env → `dist/<app>-X.Y.Z-<code>-prod.{aab,apk}`, `mapping-X.Y.Z-<code>.txt`, `apksigner verify`, `checksums.sha256`; mapping в трекер ошибок — `sentry-cli upload-proguard` (Gradle-плагин Sentry не подключён) | Linux | `make release-android TAG=vX.Y.Z` + `sentry-cli` |
| 3 | iOS: `xcodebuild archive -scheme Prod` (per-env схема проекта на Mac, `docs/reference/mac-host.md`) → `-exportArchive` (method `app-store-connect`) → `dist/*.ipa` + `-dSYM.zip`; `sentry-cli debug-files upload`; подпись — профили на Mac-хосте | Mac | по `docs/reference/mac-host.md` и `references/steps.md` |
| 4 | release-notes ru/en (черновик из `git log` от предыдущего тега + план; правит человек), `openapi.lock`, `store-handoff.md` → `dist/` | Linux | скилл пишет файлы |
| 5 | **STOP-1**: `git push origin vX.Y.Z`; Gitea Release на теге, ассеты по одному через API | Linux | `references/steps.md § STOP-1` |
| 6 | **STOP-2** (по `--channels`): Play internal, RuStore черновик → модерация, TestFlight (Mac) | Linux + Mac | `references/steps.md § STOP-2` |
| 7 | STORE-HANDOFF в чат и в `dist/store-handoff.md`; человеческие шаги — план `.claude/plans/release-v<X.Y.Z>.md` | — | форма — `references/steps.md` |

`--stop 1` — остановиться после шага 5; `--dry-run` — выполнить 0 и напечатать план 1–7 без
мутаций (тег не ставится, ничего не загружается).

## После команды

Артефакты в Gitea Release, каналы получили сборку. **Зона репо закрыта.** Покажи STORE-HANDOFF
юзеру (релиз запущен задачей из хаба `<org>/infra` — и в ответ хабу): промоушен в production
(после crash-free ≥ 99.5 % за 48 ч), ответы модерации, листинги, Data safety — человек в консолях
магазинов. Отклонение модерации → фикс → **новый тег `Z+1`**, тот же процесс.

## Что `/release` не делает

- Не генерирует ключи (`keytool` — deny) и не экспортирует секреты: значения хранятся вне
  репозитория, в env их кладёт человек.
- Не ставит тег без явного OK; не пушит `main`.
- Не отвечает магазину, не переключает production-трек, не меняет `min_version` на бэкенде
  (BACKEND-HANDOFF в STORE-HANDOFF).
- Не релизит iOS без `check-mac` на HEAD.

## Anti-patterns

- ❌ Lightweight-тег или тег не на HEAD — preflight отвергнет.
- ❌ Публиковать без загруженных символов — крэши нечитаемы, задним числом не починить.
- ❌ Понижать `versionCode`/`CFBundleVersion`; перезаливать сожжённую версию.
- ❌ Собирать релиз на машине, где keystore лежит постоянно.
- ❌ Промоутить в production из этого репо.
- ❌ Релизить, не передав STORE-HANDOFF.
