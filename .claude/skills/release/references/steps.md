# release › шаги, ассеты, секреты, STORE-HANDOFF

## Версия из тега

`scripts/tools/version-from-tag.sh vX.Y.Z` → `APP_VERSION=X.Y.Z`, `APP_BUILD=X·1e6 + Y·1e3 + Z`
(`v1.4.2` → `1004002`; X ≤ 2099, Y, Z ≤ 999). `versionName` = `CFBundleShortVersionString` =
`X.Y.Z`; `versionCode` = `CFBundleVersion` = `APP_BUILD`. Sentry `release` =
`<applicationId>@X.Y.Z+<APP_BUILD>` на обеих платформах.

## Ассеты STOP-1 (Gitea Release на теге)

| ассет | платформа | кем создан | обязателен |
|---|---|---|---|
| `<app>-<X.Y.Z>-<code>-prod.aab` | Android | `make release-android` (`bundleProdRelease`) | да |
| `<app>-<X.Y.Z>-<code>-prod.apk` | Android | то же (`assembleProdRelease`, universal; RuStore принимает AAB и APK) | да |
| `mapping-<X.Y.Z>-<code>.txt` | Android | R8 (`build-android.sh` → `dist/`); в GlitchTip заливает `sentry-cli upload-proguard` на шаге 2 (Sentry Gradle-плагин не подключён) | да (дубль того, что ушло в GlitchTip) |
| `<app>-<X.Y.Z>-<code>-ios.ipa` | iOS | `xcodebuild -exportArchive` (Mac) | при `ios ∈ platforms` |
| `<app>-<X.Y.Z>-<code>-ios-dSYM.zip` | iOS | из архива (Mac) | при iOS |
| `checksums.sha256` | обе | `build-android.sh` (+ iOS-ассеты дописываются) | да |
| `release-notes.ru.md`, `release-notes.en.md` | обе | черновик — скилл (`git log <prev>..HEAD` + план), правит человек | да |
| `openapi.lock` | обе | копия `api/openapi.lock` | да |
| `store-handoff.md` | обе | скилл (тот же блок, что в чат) | да |

Локальная стадия — `dist/` (gitignored); STOP — только Release.

## Шаг 3 — iOS на Mac-хосте

По `docs/reference/mac-host.md`: `rsync` дерева (`scripts/tools/mac.sh` делает это для `check`),
затем на хосте:

```bash
# схема Prod — per-env схема проекта, заведена в Xcode на Mac (docs/reference/mac-host.md § Первый check-mac)
xcodebuild archive -project iosApp/iosApp.xcodeproj -scheme Prod -destination 'generic/platform=iOS' \
  -archivePath build/mac/App.xcarchive
xcodebuild -exportArchive -archivePath build/mac/App.xcarchive -exportOptionsPlist iosApp/ExportOptions.plist \
  -exportPath build/mac/export          # method app-store-connect; подпись — профили из fastlane match / Vaultwarden
zip -r build/mac/dSYM.zip build/mac/App.xcarchive/dSYMs
sentry-cli debug-files upload --include-sources build/mac/App.xcarchive/dSYMs
```

Результат (`.ipa`, `dSYM.zip`) — обратно в `dist/` (`rsync`), имена — по таблице ассетов,
`checksums.sha256` дописывается.

## STOP-1 — публикация Release

```bash
git push origin vX.Y.Z
# Release на теге (тело — release-notes.ru.md)
curl -sS -X POST -H "Authorization: token $GITEA_TOKEN" -H 'Content-Type: application/json' \
  "https://<gitea>/api/v1/repos/<org>/mobile/releases" \
  -d "{\"tag_name\":\"vX.Y.Z\",\"name\":\"vX.Y.Z\",\"body\":$(jq -Rs . < dist/release-notes.ru.md)}"
# ассеты — по одному (лимит Gitea MAX_FILES=5 на загрузку)
for f in dist/*; do
  curl -sS -X POST -H "Authorization: token $GITEA_TOKEN" -F "attachment=@$f" \
    "https://<gitea>/api/v1/repos/<org>/mobile/releases/<id>/assets?name=$(basename "$f")"
done
curl -sS -H "Authorization: Bearer $NTFY_TOKEN" -d "<app> vX.Y.Z STOP-1: https://<gitea>/<org>/mobile/releases/tag/vX.Y.Z" \
  "https://ntfy.<domain>/$NTFY_TOPIC"
```

`<gitea>`/`<org>` — из `git remote get-url origin`. Проверка: страница Release содержит все
обязательные ассеты, `checksums.sha256` совпадает.

## STOP-2 — каналы

| канал | как | секреты |
|---|---|---|
| `play` | Google Play Developer API: загрузка AAB в трек `internal` (service account) | `PLAY_SERVICE_ACCOUNT_JSON` |
| `rustore` | публичное API RuStore: JWE-токен (RSA-SHA512, живёт 900 с, `timestamp ± 60 с`) → черновик версии → загрузка AAB/APK → отправка на модерацию (1–5 рабочих дней; черновик живёт 14 дней) | `RUSTORE_KEY_ID`, `RUSTORE_PRIVATE_KEY` |
| `testflight` | Mac: `xcodebuild -exportArchive … -exportOptionsPlist` с `destination: upload` (App Store Connect API key) | `ASC_KEY_ID`, `ASC_ISSUER_ID`, `ASC_KEY_P8` |
| `gitea` (`enterprise`) | STOP-1 и есть канал: APK как ассет + страница «установить» | `GITEA_TOKEN` |
| `abm` (`enterprise`) | тем же TestFlight-путём с назначением в Apple Business Manager — человек | — |

После каждого канала — ntfy `releases-<org>`.

## Секреты (env на время команды; источник — Vaultwarden `mobile/<org>/<app>`)

| переменная | запись Vaultwarden | нужна для |
|---|---|---|
| `STIT_KEYSTORE_PATH`, `STIT_KEYSTORE_PASSWORD`, `STIT_KEY_ALIAS`, `STIT_KEY_PASSWORD` | `android-upload-key` (+ `.jks` вложением) | подпись AAB/APK |
| `PLAY_SERVICE_ACCOUNT_JSON` | `play-service-account` | STOP-2 Play |
| `RUSTORE_KEY_ID`, `RUSTORE_PRIVATE_KEY` | `rustore-api-key` | STOP-2 RuStore |
| `ASC_KEY_ID`, `ASC_ISSUER_ID`, `ASC_KEY_P8` | `apple-asc-api-key` | TestFlight / App Store Connect |
| `MATCH_GIT_URL`, `MATCH_PASSWORD` (или профили вложениями) | `apple-signing` | подпись IPA на Mac |
| `SENTRY_AUTH_TOKEN`, `SENTRY_ORG`, `SENTRY_PROJECT_*`, `SENTRY_URL` | `sentry` | символы |
| `GITEA_TOKEN` (scope `write:repository`) | `gitea-release-token` | STOP-1 |
| `NTFY_TOPIC=releases-<org>`, `NTFY_TOKEN` | `ntfy` | уведомления |
| `MAC_HOST` | не секрет | iOS-стадии |

Агент значения не читает и не цитирует; проверка «задано» — `release-preflight`/`build-android.sh`.

## STORE-HANDOFF — форма

```
STORE-HANDOFF · <app> vX.Y.Z (code <N>) · <дата>
ARTIFACTS  Release: <url> · aab/apk/ipa sha256 … · symbols: uploaded (mapping ✓ dSYM ✓)
PLAY       internal track: uploaded · WHO: владелец аккаунта · ACTION: промоушен в production после crash-free ≥ 99.5 % за 48 ч · DECIDE: staged rollout 10 %?
RUSTORE    черновик #… отправлен на модерацию (ожидание 1–5 раб. дней, черновик живёт 14 дней) · ACTION: ответить на замечания; при отказе — /release с Z+1
TESTFLIGHT build <N> processing · ACTION: внешняя группа → App Review (demo-доступ: entry-points `demo`; App Review notes из distribution.md)
BACKEND    min_version в /api/app/requirements → <X.Y.Z> после раскатки (BACKEND-HANDOFF) · openapi.lock: <sha>
ENV        wellknown: отпечатки актуальны ✓ · SENTRY_DSN prod ✓ · MOBILE_* env в <org>/infra ✓
LEGAL      L-10…L-17 (человек): политика опубликована по <url>, Data safety обновлён?, возрастной рейтинг, РКН-статус
RISK       что может отклонить ревью (4.8 / 5.1.1 / 3.1.3), что не проверено на устройстве, что ушло без check-mac (не должно)
```

Блок печатается в чат, пишется в `dist/store-handoff.md` (ассет STOP-1) и разворачивается
секцией с человеческими шагами в `.claude/plans/roadmap.md`.
