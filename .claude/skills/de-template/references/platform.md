# de-template › `--platform android|ios`

Форк-пути «только Android» / «только iOS» — не отдельные шаблоны, а обрезка одного. iOS-only
форк **сохраняет** `shared/` (логика на Kotlin, JVM-тесты на Linux) — это цена
ecosystem-constraint'а «агент на Linux»; чистый Swift-проект — вне шаблона.

| шаг | `--platform android` | `--platform ios` |
|---|---|---|
| Удаляется | `iosApp/`, `.xcode-version`, `.swift-version`, `e2e/ios`, `scripts/tools/mac.sh`, `scripts/tools/swift-step.sh`, `scripts/tools/coverage-floor.sh`, `docs/reference/mac-host.md`; в `shared/build.gradle.kts` и `KmpLibraryConventionPlugin` — `iosArm64()`, `iosSimulatorArm64()`, framework `Shared`, klib-дамп `shared/api/shared.klib.api`; iOS-секции `docs/stack/{runtime,distribution,signing,compliance,push,deep-links,styling,config}.md`; правила `ios-ui`, `swift-concurrency`, `xcode-project`; Swift-стадии в `check.sh` (`format swift`), группа `##@ MAC` и Swift-часть `fmt`/`fmt-check`/`cover-check` в `Makefile`; `IOS_SOURCES`; `.gate-fixtures/NF-2x` (Swift) | `androidApp/`, `config/{detekt,lint,env}` (кроме того, что читает `shared`), `e2e/android`, `scripts/release/build-android.sh`, `scripts/tools/android-skills.sh`, `.gate-fixtures/NF-01…10`; Android-плагины `build-logic` (остаются `stit.kmp.library`, `stit.jvm.library`, `stit.quality`); в `shared/` — `androidLibrary {}`, `androidMain`, `androidHostTest`; Android-секции kinds; правила `android-ui`, `design-tokens` (Android-часть), `gradle-build` (сужается до KMP); Android-стадии `check.sh` (`android lint`, `android unit tests`, `screenshots`, `assembleDevDebug`, `assembleProdRelease`), группы `Makefile` |
| Остаётся | `shared/` с `androidLibrary` + `jvm`; Linux-гейт полный; `kmp-check` не нужен — удаляется вместе с таргетами | `shared/` с `jvm` + iOS-таргетами (Linux: klib + `jvmTest`); `check-mac` обязателен для UI |
| `service.md` | `platforms: [android]` | `platforms: [ios]` |
| SKIP-блок `check.sh` | удаляется (`MAC_STAGES` пуст) | остаётся |
| Обратимость | `/init-service --add-platform ios` — из upstream-ветки шаблона (ручной документированный путь) | `--add-platform android` |

## Порядок

1. Убедиться, что `docs/stack/service.md#platforms` уже отражает выбор и iOS/Android-секции
   kinds отсутствуют (Gate `/de-template`).
2. Удалить дерево и файлы по таблице (`git rm -r`), затем `Makefile`/`check.sh` — точечно, в
   стиле соседних строк: строки с `$(SWIFT_STEP)`/`IOS_SOURCES`/группа `##@ MAC` (android-путь);
   стадии и вербы Android (ios-путь).
3. Правила `.claude/rules/` и `CLAUDE.md`: удалить ссылки на отрезанную половину (карта, инварианты,
   команды) — forward-only, без «удалено».
4. `settings.json`: allow/deny на отрезанные вербы/пути — убрать.
5. `docs/reference/architecture.md`, `ecosystem.md`, `release.md`, `distribution.md`,
   `strategy-mapping.md` — оставить одну платформу; `kinship.md` — без изменений.
6. `make check` → 0; `make gate-selftest` → все оставшиеся фикстуры красные.

## Что не делать

- Не оставлять «пустые» секции/каталоги с пометкой «для iOS позже» — forward-only.
- Не вырезать `shared/` в ios-пути: без него нет Linux-цикла.
- Не править `.pbxproj` (android-путь его просто удаляет).
