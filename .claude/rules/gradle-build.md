---
name: gradle-build
description: Конфигурация сборки — только через convention-плагины stit.* из build-logic; версии и SDK-уровни только в gradle/libs.versions.toml; AGP 9 без kotlin-android и kapt; новый модуль = include + плагин + строка в architecture.md; gradle.properties не трогать без плана. Грузится при правке build.gradle.kts, build-logic/, gradle/, settings.gradle.kts, gradle.properties.
paths:
  - "**/build.gradle.kts"
  - "build-logic/**"
  - "gradle/**"
  - "settings.gradle.kts"
  - "gradle.properties"
---

# Gradle и convention-плагины

## Инварианты

- **Модуль применяет только `stit.*`** (`alias(libs.plugins.stit.android.feature)` и т. п.) и
  объявляет `namespace` + собственные зависимости. Сырые `com.android.*`/`org.jetbrains.*`
  плагины — только внутри `build-logic/convention`. Плагины: `stit.android.application`,
  `stit.android.library`, `stit.android.feature`, `stit.android.compose`, `stit.android.hilt`,
  `stit.android.lint`, `stit.kmp.library`, `stit.jvm.library`, `stit.quality`,
  `stit.roborazzi`, `stit.konsist` (реестр — `build-logic/convention/build.gradle.kts`).
- **Версии и SDK-уровни — только `gradle/libs.versions.toml`** (`compileSdk`,
  `compileSdkMinor`, `minSdk`, `targetSdk` — там же; плагины читают `libs.int(...)`).
  Версия в `build.gradle.kts` модуля — нарушение. Обновление версий — через `/plan`, latest
  stable с двухнедельной выдержкой; alpha/beta/rc — только решением владельца со строкой в
  `docs/stack/dependencies.md § Version policy exceptions` (причина + триггер снятия; в шаблоне —
  `androidx.appfunctions`). Пары `kotlin` ↔ `ksp` ↔ `hilt`
  и `agp` ↔ `androidTools` (= AGP + 23) обновляются вместе.
- **AGP 9 со встроенным Kotlin**: `org.jetbrains.kotlin.android` не применяется,
  `kotlinOptions {}` нет (`kotlin { compilerOptions {} }`), kapt нет — только KSP;
  `targetSdk` задан явно; `CommonExtension` не генерик — конфигурировать
  `ApplicationExtension`/`LibraryExtension`, общее — функциями `tech/stit/convention/AndroidDsl.kt`.
- **Одна ось flavors `env`** на все Android-модули: список = файлы `config/env/<name>.properties`;
  имена тасков (`testDevDebugUnitTest`, `lintDevDebug`) едины, гейт про модули не знает.
  Kover-вариант `gate` = `devDebug`; агрегат покрытия — `:androidApp:app`.
- **Новый модуль** = `include(":androidApp:…")` в `settings.gradle.kts` + плагин `stit.*` +
  строка в `docs/reference/architecture.md`. Typesafe accessors (`projects.androidApp.core.ui`).
- **`gradle.properties` не трогать без плана**: `-Xmx2g`, `MaxMetaspaceSize=1g` (ниже —
  Lint падает по Metaspace), `workers.max=4`, configuration/build cache, `HeapDumpPath=build/`,
  `kotlin.native.ignoreDisabledTargets=true`.
- **Плагин ≤ 100 строк**, один WHY-комментарий на неочевидное, без дублирования между
  плагинами. Зависимости отсортированы (`sortDependencies` внутри `make fmt`,
  `checkSortDependencies` — стадия `format kotlin`).
- **JVM 17** для байткода Android-модулей и `:androidApp:lint`; `jvmToolchain(17)` в KMP.
- `gradle/wrapper/gradle-wrapper.properties` — deny на правку; wrapper обновляется через
  план с `distributionSha256Sum`.
- `detekt` читает весь `src/` (иначе `commonMain` без анализа); `**/generated/**` исключён.

## Сигналы

- `Cannot add extension with name 'kotlin'` → применён `kotlin-android`; убрать.
- `Inconsistent JVM-target` → `jvmTarget` ≠ `targetCompatibility` (`JvmLibraryConventionPlugin`).
- `OutOfMemoryError: Metaspace` в Lint → `gradle.properties` тронут.
