---
name: release-flow
description: STOP-1 = Gitea Release на аннотированном теге vX.Y.Z с подписанными артефактами и символами; STOP-2 = подача в каналы; версия из тега (versionCode = X·1e6 + Y·1e3 + Z); символы до публикации; check-full до тега; отклонение модерации → Z+1; STORE-HANDOFF; ntfy. Грузится при правке scripts/release/, Makefile, distribution.md, release.md, .gitea/.
paths:
  - "scripts/release/**"
  - "Makefile"
  - "docs/stack/distribution.md"
  - "docs/reference/release.md"
  - ".gitea/**"
---

# Релиз

Процесс, вербы, секреты и STORE-HANDOFF — `docs/reference/release.md`; skill — `/release`.
Здесь — инварианты.

## Инварианты

- **Тег первичен**: аннотированный `vX.Y.Z` на чистом `main`; `versionName` =
  `CFBundleShortVersionString` = `X.Y.Z`, `versionCode` = `CFBundleVersion` =
  `X·1 000 000 + Y·1 000 + Z` (`scripts/tools/version-from-tag.sh`; dev-сборки — `0.0.0`/`1`).
  Тег ставит человек (или `/release` по явному OK), только через верб.
- **Версии не убывают, отката нет**: сожжённая версия → `Z+1`; отклонение модерации →
  новый тег, тот же процесс; повторное использование тега запрещено (`preflight.sh`).
- **`check-full` на этом HEAD до тега**: `make release-preflight` = `main` + чистое дерево +
  тег новее всех + `make doctor` + `GATE_RELEASE=1` гейт + `check-mac` (кроме
  `PLATFORM=android`).
- **STOP-1 — Gitea Release** на теге с ассетами: `<app>-<X.Y.Z>-<code>-prod.aab` + `.apk`,
  `mapping-<X.Y.Z>-<code>.txt`, (iOS) `.ipa` + `-dSYM.zip`, `checksums.sha256`,
  `release-notes.{ru,en}.md`, `openapi.lock`, `store-handoff.md`. Локальная стадия — `dist/`
  (gitignored), не STOP.
- **Символы до публикации**: `mapping.txt` — `sentry-cli upload-proguard` (шаг `/release`, Linux;
  Gradle-плагин Sentry не подключён), dSYM — `sentry-cli debug-files upload` на Mac; без символов
  крэши нечитаемы, а перезалить для уже прилетевших нельзя.
- **STOP-2 — подача в каналы**: Play internal, RuStore модерация, TestFlight; промоушен в
  production, ответы модерации, листинги — человек по STORE-HANDOFF (crash-free ≥ 99.5 % за
  48 ч — `observability.md`).
- **Каналы по `preset`**: `companion`/`consumer` — RuStore + Play (+ App Store при iOS);
  `enterprise` — `gitea` (APK как ассет) + ABM; Play не используется.
- **Секреты — env из Vaultwarden на время команды** (rule `signing-and-secrets`); релиз без
  env падает, debug-ключом не подписывается.
- **Уведомление** — ntfy-топик `releases-<org>` на STOP-1 и STOP-2.
- **Partial release** — `--platform android|ios|both`; iOS-часть только с Mac-хоста
  (`docs/reference/mac-host.md`).
- Что `/release` не делает: не генерирует ключи (`keytool` deny), не читает Vaultwarden,
  не отвечает модерации, не переключает production-трек.
- Скрипты `scripts/release/*.sh` — `set -euo pipefail`, одна строка `OK:`/`FAIL:`, версия из
  тега через `eval "$(version-from-tag.sh)"`, без хардкода имён приложения (берётся из
  `basename $PWD` / `service.md`).

## Сигналы

- `release-preflight: тег не annotated` → `git tag -a`.
- `build/gate-logs/mac.last` ≠ HEAD → `check-mac` устарел, iOS-релиз невозможен.
