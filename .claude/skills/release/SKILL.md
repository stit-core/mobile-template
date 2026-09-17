---
name: release
description: Выпустить версию мобильного приложения — аннотированный тег vX.Y.Z, preflight (check-full на HEAD, версия монотонна, чек-лист магазинов), подписанные артефакты (AAB/APK на Linux, IPA/dSYM на Mac-хосте), символы в GlitchTip/Sentry, STOP-1 = Gitea Release с ассетами, STOP-2 = подача в каналы (Play internal / RuStore модерация / TestFlight), STORE-HANDOFF человеку. Флаги --platform android|ios|both, --stop 1|2, --channels, --dry-run. Активируй когда юзер говорит "релиз", "катим версию", "сделай release vX.Y.Z", "/release".
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

## Когда НЕ применять

- «Собери APK посмотреть» → `make build` (dev-debug), не релиз.
- Feature-ветка / грязное дерево → preflight отвергнет.
- Юзер не озвучил OK на релиз → STOP, спроси.

## Pre-flight (BEFORE команд)

Любой пункт не закрыт — не лезь:

- [ ] Юзер явно сказал OK и назвал версию `vX.Y.Z` (не угадывать; semver-bump: patch — фикс,
      minor — новые экраны/поля, major — несовместимость с бэкендом/`min_version`).
- [ ] `make status`: `main`, working tree clean, `Handoffs: none` или блоки уже в плане.
- [ ] `make check-full` зелёный на этом HEAD (`build/gate-logs/mac.last` == HEAD при iOS);
      `--platform android` — достаточно `GATE_RELEASE=1 make check`.
- [ ] Чек-лист L-1…L-9 (автоматика preflight) зелёный; L-10…L-17 — подтверждены человеком
      поимённо (`docs/reference/distribution.md`), скилл спрашивает через `AskUserQuestion`.
- [ ] Секреты экспортированы человеком в env текущей сессии (`STIT_KEYSTORE_*`, `GITEA_TOKEN`,
      каналы — по `--channels`; таблица — `references/steps.md`); значения агент не читает.
- [ ] iOS: `MAC_HOST` доступен (`make mac-doctor`), профили/сертификаты на хосте.
- [ ] `**Infra impact.**` активных планов собран — уедет в STORE-HANDOFF (`BACKEND`, `ENV`).

## Шаги

| # | шаг | где | команда |
|---|---|---|---|
| 0 | preflight: `main`, чистое дерево, аннотированный тег на HEAD == `TAG`, тег новее всех, `make doctor`, `GATE_RELEASE=1` гейт, `check-mac` (кроме `PLATFORM=android`) | Linux | `make release-preflight TAG=vX.Y.Z` (`PLATFORM=android` при `--platform android`) |
| 1 | аннотированный тег `vX.Y.Z` на HEAD — ставит человек по явному OK (`git tag -a vX.Y.Z -m "<release notes>"`); push тега — после STOP-1 | Linux | человек |
| 2 | Android: `bundleProdRelease` + `assembleProdRelease` с подписью из env → `dist/<app>-X.Y.Z-<code>-prod.{aab,apk}`, `mapping-X.Y.Z-<code>.txt`, `apksigner verify`, `checksums.sha256`; Sentry Gradle-плагин заливает mapping | Linux | `make release-android TAG=vX.Y.Z` |
| 3 | iOS: архив → `-exportArchive` (method `app-store-connect`) → `dist/*.ipa` + `-dSYM.zip`; `sentry-cli debug-files upload`; подпись — профили на Mac-хосте | Mac | по `docs/reference/mac-host.md` и `references/steps.md` |
| 4 | release-notes ru/en (черновик из `git log` от предыдущего тега + план; правит человек), `openapi.lock`, `store-handoff.md` → `dist/` | Linux | скилл пишет файлы |
| 5 | **STOP-1**: `git push origin vX.Y.Z`; Gitea Release на теге, ассеты по одному через API; ntfy `releases-<org>` | Linux | `references/steps.md § STOP-1` |
| 6 | **STOP-2** (по `--channels`): Play internal, RuStore черновик → модерация, TestFlight (Mac); ntfy | Linux + Mac | `references/steps.md § STOP-2` |
| 7 | STORE-HANDOFF в чат и в `dist/store-handoff.md`; секция в roadmap с человеческими шагами | — | форма — `references/steps.md` |

`--stop 1` — остановиться после шага 5; `--dry-run` — выполнить 0 и напечатать план 1–7 без
мутаций (тег не ставится, ничего не загружается).

## После команды

Артефакты в Gitea Release, каналы получили сборку. **Зона репо закрыта.** Покажи STORE-HANDOFF
юзеру: промоушен в production (после crash-free ≥ 99.5 % за 48 ч), ответы модерации, листинги,
Data safety — человек в консолях магазинов. Отклонение модерации → фикс → **новый тег `Z+1`**,
тот же процесс.

## Что `/release` не делает

- Не генерирует ключи (`keytool` — deny), не читает Vaultwarden (`bw` — deny): экспорт секретов
  в env — человек.
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
