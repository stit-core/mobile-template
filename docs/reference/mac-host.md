# Mac build-host

Всё, что импортирует `Shared`/SwiftUI, компилируется только на macOS: линковка `Shared.framework`,
`iosSimulatorArm64Test`, сборка `Core`/`Features`, snapshot-тесты, XCUITest, подпись, IPA,
TestFlight. Linux-гейт эти стадии **не изображает зелёными** — печатает SKIP-блок и код 0; «готово
по iOS» — только после `make check-mac`. Mac-хост — предусловие iOS-ветки: без него iOS-половина
помечена «не верифицирована», `/de-template` с `ios ∈ platforms` требует `--ios-unverified`, `/plan`
держит атом «Mac-verify» открытым.

## Схема

```
Linux (агент, гейт, релиз Android)
  ssh/rsync ──► Mac mini (Apple silicon, macOS ≥ 26.6, Xcode 27.0 — .xcode-version)
                  ~/ci/<slug>/  ← rsync дерева без build/ .build/ .gradle/ .kotlin/ .git/
                  ./gradlew :shared:linkDebugFrameworkIosSimulatorArm64 · iosSimulatorArm64Test · apiCheck -Pstit.klibStrict=true
                  xcodebuild build/test (-scheme Dev, test plans Unit + Snapshot [+ UI])
                  xcodebuild archive / -exportArchive · sentry-cli debug-files upload (релиз)
                  результаты (xcresult, snapshot-diff'ы, ipa) ──► rsync обратно в build/mac/, dist/
```

Один Mac на всю экосистему (ресурс org, не сервис control-plane): держит Xcode, симуляторы,
связку ключей; проекта на нём нет — только `~/ci/<slug>/`. Xcode Cloud (25 ч/мес в Developer
Program) — резерв для подписи и загрузки в TestFlight, не основной путь. Аренда Mac за рубежом —
только при зарубежном платёжном канале; виртуализация macOS на не-Apple железе — нарушение
лицензии Apple, не используется.

## Подключение

1. Mac: SSH-сервер, пользователь для CI, вход по ключу без пароля (`BatchMode=yes`); Tailscale —
   если хост вне локальной сети.
2. Xcode версии `.xcode-version` (27.0), симулятор из `iosApp/.test-simulator` установлен
   (`xcrun simctl list devices available`); `xcbeautify` — опционально (вывод компактнее).
3. JDK 21 и `~/.konan` на Mac — Gradle-стадии `shared` линкуют framework там же.
4. Linux: `export MAC_HOST=user@host` (не секрет; удобно в `.env.local`), `make mac-doctor`.

`scripts/tools/mac.sh` (`doctor | check | build | test | snapshot-record | run`) делает `rsync`,
запускает стадии по SSH с `set -o pipefail`, копирует результаты обратно и пишет
`build/gate-logs/mac.last` (`<дата> (git <sha>)`) при зелёном `check`. Без `MAC_HOST` — fail-fast:
`make check-full` и релиз не изображают зелёный.

## Первый `check-mac`

iOS не верифицирован на Mac: Linux-гейт проверяет только `Packages/Pure`, `Tools/archlint`,
swift-format и SwiftLint всего дерева. В Xcode на хосте перед первым зелёным `check-mac`:
подключить локальные пакеты `Packages/{Pure,Core,Features,TestSupport}` к таргету `iosApp` и
связать `Shared.framework` с пакетами (`FRAMEWORK_SEARCH_PATHS` → `shared/build/xcode-frameworks`,
build phase `scripts/embed-shared.sh`); завести per-env конфигурации и схемы `Staging|Prod|Demo`
на `Config/*.xcconfig` (в репо — shared-схема `Dev`, с ней работают `mac.sh` и `run-ios`);
добавить UI-test таргет `iosAppUITests` (`TestPlans/UI.xctestplan`, `UITests/AppLaunchTests.swift`);
зафиксировать `Package.resolved` (`sentry-cocoa` 9.28, `swift-snapshot-testing` 1.19.5); записать
эталоны `__Snapshots__/` (`make snapshot-record`). Результат коммитится вместе с
`Package.resolved` и `__Snapshots__/`; `build/gate-logs/mac.last` — отметка верификации
(`make status` → `ios verified`).

## Вербы

| верб | что делает |
|---|---|
| `make mac-doctor` | доступность `MAC_HOST`, Xcode == `.xcode-version`, симулятор из `.test-simulator` |
| `make check-mac` | M1 doctor → M2 `linkDebugFrameworkIosSimulatorArm64` (каталог `Shared.framework` существует) → M3 `iosSimulatorArm64Test` → M4 `apiCheck -Pstit.klibStrict=true` → M5 `xcodebuild build` (`CODE_SIGNING_ALLOWED=NO`) → M6 test plan `Unit` → M7 `Snapshot` (diff'ы обратно) → M8 `UI` при `MAC_UI=1`; пишет `mac.last` |
| `make check-full` | `make check` + `make check-mac`; обязателен перед `/release` |
| `make mac-build` / `mac-test` | отдельные стадии |
| `make snapshot-record` | `SNAPSHOT_TESTING_RECORD=all` на симуляторе из `.test-simulator`; эталоны `__Snapshots__/` копируются обратно — осознанно, в коммит |
| `make run-ios` | сборка схемы `Dev` и запуск на симуляторе |

Логи — `build/gate-logs/<стадия>.log`; xcresult — `build/mac/`.

## Подпись и релиз

Сертификаты и provisioning profiles — `fastlane match` в приватном репо `<org>/ios-certificates`
(пароль в Vaultwarden `apple-signing`) или профили вложениями из Vaultwarden; App Store Connect
API key (`ASC_*`) — для экспорта с `destination: upload` в TestFlight. `xcrun altool`/`fastlane` —
только человек на Mac; агент по SSH выполняет `xcodebuild` с `CODE_SIGNING_ALLOWED=NO` для
проверок и не читает связку ключей. dSYM заливает `sentry-cli` там же (FSL-лицензия — ок для
внутреннего инструмента). Шаги — `.claude/skills/release/references/steps.md`.

## Гигиена

- Снапшоты валидны только для симулятора из `.test-simulator` (device + OS); смена — перезапись
  эталонов через `make snapshot-record`.
- Симуляторные рантаймы не удаляются до конца — чистить диск на хосте периодически.
- Обновление Xcode — через `docs/stack/runtime.md` и `.xcode-version` одним коммитом; дедлайн
  App Store Connect на iOS 27 SDK — апрель 2027.
- Ротация SSH-ключа и профилей — запись в `docs/stack/signing.md`.
