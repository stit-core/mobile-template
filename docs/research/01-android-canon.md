# 01 — Канон Android 2026: стек, архитектура, структура, тесты, качество

> Поток `01-android-canon` ресёрча мобильного шаблона экосистемы stit.
> Дата сбора фактов — **2026-09-17**. Версии и статусы проверены в этот день по
> первоисточникам: Google Maven (`dl.google.com/dl/android/maven2/<group>/group-index.xml`),
> Maven Central (`repo1.maven.org/maven2/<group>/<artifact>/maven-metadata.xml`),
> `services.gradle.org/versions/current`, `developer.android.com`, `kotlinlang.org`, GitHub.
> Пометки: **[проверено]** — факт подтверждён первоисточником (URL приведён);
> **[предположение]** — вывод/оценка автора отчёта, не факт.

## TL;DR

1. **Kotlin 2.4.20** (07.09.2026) · **AGP 9.4.0** (требует Gradle ≥ 9.6.0, JDK ≥ 17) ·
   **Gradle 9.7.1** · **JDK-toolchain 21** (Robolectric 4.16+ требует 21 для SDK 36/37) ·
   **Compose BOM 2026.09.00** (compose 1.12.1, material3 1.4.0) · **compileSdk/targetSdk 37**
   (Android 17 «Cinnamon Bun» stable с 16.06.2026; потолок AGP 9.4 — тоже 37) ·
   **minSdk 26** (96.1 % устройств, предложение — см. §5.3). [проверено]
2. **AGP 9 сам компилирует Kotlin.** Плагин `org.jetbrains.kotlin.android` больше не применяется и
   *несовместим* с новым DSL: `android.kotlinOptions {}` → `kotlin { compilerOptions { … } }`,
   `kotlin-kapt` не работает (только KSP либо `com.android.legacy-kapt`). Это переписывает все
   гайды и примеры старше 2026 года — главная ловушка ресёрча по памяти. [проверено]
3. **Навигация сменилась.** Официальная рекомендация в «Recommendations for Android architecture» —
   **Navigation 3** (`androidx.navigation3`, 1.0.0 stable с 19.11.2025, сейчас 1.1.7). Это
   back-stack-as-a-list поверх Compose, а не граф. Navigation Compose 2.x (2.10.1) жив, но это
   уже «предыдущее поколение». [проверено]
4. **Архитектура не менялась и меняться не собирается**: слои `ui → domain (опционально) → data`,
   UDF, `ViewModel` + `StateFlow<UiState>` + `collectAsStateWithLifecycle()`, репозиторий как
   SSOT, модели по слоям. Это **фундамент**, а MVI-фреймворки (Orbit, Circuit, Decompose,
   Mavericks) — **мода/ниша**. Отдельно сильная официальная рекомендация:
   «**Do not send events from the ViewModel to the UI**» — то есть one-off-эвенты/side-effects
   как в MVI официально не рекомендуются. [проверено]
5. **DI: Hilt** (`com.google.dagger:hilt-android` 2.60.1) — официальная рекомендация Google и де-факто
   стандарт. **Metro** (Zac Sweers, 1.0 stable с апреля 2026, сейчас 1.4.3) — самый интересный
   претендент (компиляторный плагин, без KSP/kapt, KMP), но для шаблона-для-заказчика это риск
   найма. Koin 4.2.2 — runtime-DI, проще, но теряет compile-time проверку. [проверено]
6. **Модульность — по фичам, а не по слоям**: `app` + `feature:<name>` + `core:<name>`
   (+ `build-logic` как **includeBuild** с convention-плагинами + `gradle/libs.versions.toml`).
   Это канон Now in Android и официального гайда по модуляризации. Аналог FSD-слоёв из
   `spa-frontend-template` — ровно тот же принцип «горизонтальные слои × вертикальные слайсы».
7. **Гейт без эмулятора на Linux реален и закрывает даже визуальную регрессию**: Spotless +
   detekt + Android Lint + JVM-unit (JUnit 4 + coroutines-test + Turbine) + **Robolectric 4.17**
   (поддерживает SDK 23–37) + **Compose-тесты на Robolectric** + **screenshot-тесты на JVM
   (Roborazzi 1.74.0)** + Kover-gate + `assembleRelease` (R8) + `buildHealth`. **Требуют
   устройства/эмулятора**: Espresso/UIAutomator, Gradle Managed Devices, Macrobenchmark и
   генерация Baseline Profiles, Maestro-e2e.
   ⚠️ Официальный screenshot-плагин `com.android.compose.screenshot` **объявлен deprecated в
   alpha16**, не дойдя до беты (преемник — AGP test suites, требует AGP 9.5-alpha) — поэтому
   Roborazzi, а не он.
8. **Архитектурный линтер уровня Steiger/depguard на Android собирается из трёх кусков**:
   Gradle-граф модулей (физически запрещает `feature:a → feature:b`, потому что зависимости нет),
   **detekt `ForbiddenImport`/`ForbiddenMethodCall`** (аналог `forbidigo`: запрет `android.util.Log`,
   `println`, `GlobalScope`, `java.util.Date`, `Toast` напрямую) и **Android Lint** (+ свой
   `lint`-модуль с кастомными `Detector`'ами, как в Now in Android). Konsist как четвёртый
   слой — под вопросом: последний релиз 0.17.3 опубликован **2024-12-08**, почти два года без
   обновлений. [проверено — Maven Central `lastUpdated=20241208194313`]
   **Отдельно — тулинг качества в развилке, и это надо знать до старта:** detekt stable заморожен на
   **1.23.8** (группа `io.gitlab.arturbosch`, последний апдейт 2025-02), а `dev.detekt` 2.0 —
   только **alpha.6**; **compose-rules ≥ 0.5.0 требует detekt 2.0-alpha**, то есть на стабильном
   detekt потолок Compose-правил — **0.4.28**. ktlint при этом сменил владельца: Pinterest
   прекратил поддержку, `com.pinterest.ktlint` заканчивается на 1.8.0, продолжение —
   `io.github.ktlint.core` 2.0.0-ALPHA-4. Оба выбора в шаблоне — осознанный компромисс
   «стабильность вместо свежести». [проверено]
9. **Room 3** (`androidx.room3`, 3.0.3) — новый major с KMP, только Kotlin + KSP, suspend-API.
   Room 2.8.5 (`androidx.room`) остаётся для Android-only. Для offline-tolerant бизнес-приложения
   БД часто вообще не нужна — хватает `DataStore` + Query-кеш в памяти. [проверено]
10. **Сеть**: Retrofit 3.0.0 + OkHttp 5.5.0 + `kotlinx-serialization` 1.11.0 — консервативный
    Android-only выбор; Ktor client 3.6.0 — если есть хоть какой-то горизонт KMP. Обе живы.
    Для нашего cookie-auth бэкенда важен `CookieJar`/`HttpCookies` (детали — поток 07).
11. **Официальный тулинг для агентов появился**: репозиторий `android/skills` (Apache-2.0) с
    AI-оптимизированными скиллами (`agp-9-upgrade`, `navigation-3`, `testing`, `edge-to-edge`,
    `r8-analyzer`, …) и CLI `android skills add <name>`. Это прямой аналог нашей `.claude/skills/`
    и кандидат на переиспользование в шаблоне. [проверено]
12. **Мода, которую в шаблон брать не надо**: MVI-фреймворки как база, `Compose Multiplatform`
    ради «одного UI» (это вопрос потока 03), «Clean Architecture» с обязательным `domain`-слоем и
    use-case на каждый чих, кастомные MVI-редьюсеры, RxJava, kapt, View-система, Dagger вручную,
    `LiveData`, `AndroidViewModel`, `GlobalScope`, Accompanist (почти весь разъехался в
    androidx/Compose).
13. **Что точно фундаментально** (переживёт ещё 5 лет): unidirectional data flow; immutable UiState;
    repository как SSOT; корутины/Flow вместо колбэков; конструкторная инъекция; fakes вместо
    mock'ов; модульность по фичам с convention-плагинами; version catalog; R8; Baseline Profiles;
    отделение бизнес-логики от Android-фреймворка (чтобы тесты шли на JVM).

## Findings

### §1. Версии и платформа (состояние на 2026-09-17)

#### 1.1. Ядро тулчейна

| Компонент | Stable | Дата | Источник |
|---|---|---|---|
| Kotlin | **2.4.20** | 2026-09-07 | `kotlinlang.org/docs/releases.html` [проверено] |
| Kotlin (предыдущий language release) | 2.4.0 | 2026-06-03 | там же |
| Android Gradle Plugin | **9.4.0** | сентябрь 2026 | Google Maven `com.android.tools.build:gradle`; `developer.android.com/build/releases/gradle-plugin` [проверено] |
| Gradle | **9.7.1** | 2026-08-19 | `services.gradle.org/versions/current` [проверено] |
| Gradle (минимум для AGP 9.4) | 9.6.0 | — | таблица совместимости AGP [проверено] |
| JDK (минимум для AGP 9.4) | **17** | — | там же [проверено] |
| SDK Build Tools (для AGP 9.4) | 36.0.0 | — | там же [проверено] |
| compileSdk (потолок AGP 9.4) | **37** | — | там же [проверено] |
| KSP | **2.3.12** | 2026-09-09 | Maven Central `com.google.devtools.ksp:symbol-processing-api` [проверено] |
| Compose BOM | **2026.09.00** | 2026-09-09 | Google Maven `androidx.compose:compose-bom` [проверено] |

**Каденс Kotlin** [проверено, `kotlinlang.org/docs/releases.html`]: language-релиз `2.x.0` — раз в
6 месяцев; tooling-релиз `2.x.20` — через 3 месяца после него; bugfix — по мере надобности.
Следующий — 2.5.0 в декабре 2026, 2.5.20 в марте 2027. Support-window стандартной библиотеки с
2.4.0 — 18 месяцев (2.4 поддерживается до 2027-12-03). Для шаблона это значит: **раз в полгода
обязательный bump Kotlin**, и он тянет за собой KSP + Compose compiler.

**K2** стабилен с Kotlin 2.0.0 (май 2024) — на 2026 год это данность, отдельного вопроса
«переходить ли на K2» больше нет. [проверено]

#### 1.2. AGP 9: встроенный Kotlin — главная ломающая новость

**[проверено]** `developer.android.com/build/releases/agp-9-0-0-release-notes` и
`developer.android.com/build/migrate-to-built-in-kotlin`:

- AGP 9.0 несёт **built-in Kotlin support, включённый по умолчанию** (`android.builtInKotlin=true`).
  Плагин `org.jetbrains.kotlin.android` (`kotlin-android`) **больше не применяется** и
  **несовместим** с новым DSL: при попытке получите
  `Failed to apply plugin 'org.jetbrains.kotlin.android'. Cannot add extension with name 'kotlin',
  as there is an extension already registered with that name`.
- AGP 9.0 имеет runtime-зависимость на KGP (в 9.0.0 — Kotlin 2.2.10); объявлять версию KGP не нужно,
  более низкая будет поднята автоматически.
- AGP 9.0 перешёл на **новые публичные DSL-интерфейсы** (`android.newDsl=true` по умолчанию):
  старые `BaseExtension`-типы и **deprecated variant API удалены**.
- Отказ от новинок (`android.builtInKotlin=false`, `android.newDsl=false`) — временный,
  **в AGP 10.0 отключить нельзя**.

Что это меняет в коде сборки:

```kotlin
// БЫЛО (любой гайд/сэмпл до 2026):
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)          // ← удалить
}
android {
    kotlinOptions { jvmTarget = "17" }          // ← удалить
}

// СТАЛО (AGP 9):
plugins {
    alias(libs.plugins.android.application)
}
kotlin {
    compilerOptions {
        // jvmTarget задавать не нужно: по умолчанию = android.compileOptions.targetCompatibility
        languageVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_4
    }
}
```

Source sets тоже переехали:

```kotlin
// НЕ поддерживается:
kotlin.sourceSets.named("main") { kotlin.srcDir("gen/kotlin") }
android.sourceSets.named("main") { java.directories += "gen/kotlin" }

// Поддерживается:
android.sourceSets.named("main") { kotlin.directories += "gen/kotlin" }
// или через Variant API:
androidComponents.onVariants { variant ->
    variant.sources.kotlin!!.addStaticSourceDirectory("gen/kotlin")
}
```

Другие behavior changes AGP 9.0 (все `false → true`) [проверено]:
`android.uniquePackageNames`, `android.useAndroidx`, `android.enableAppCompileTimeRClass`,
`android.sdk.defaultTargetSdkToCompileSdkIfUnset` (⇒ **обязательно задавать targetSdk явно**),
`android.r8.proguardAndroidTxt.disallowed` (⇒ `proguard-android-optimize.txt`),
`android.r8.globalOptionsInConsumerRules.disallowed`.

Удалено в AGP 9.0: `wearApp`/embedded Wear, таски-отчёты `androidDependencies` и `sourceSets`,
density-split APK, `dexOptions`, `generatePureSplits`, `aidlPackagedList`, DSL `AndroidSourceSet.jni()`,
`BuildType.isRenderscriptDebuggable`, `DensitySplit`, `LanguageSplitOptions`; в variant API —
`ComponentBuilder.enabled()` → `enable`, `Variant.minSdkVersion()` → `minSdk`, и т. п.
Параметризация `CommonExtension<*,*,…>` убрана — в convention-плагинах теперь просто
`CommonExtension` и `defaultConfig.apply { … }`. [проверено]

Миграции `applicationVariants.all { }` → `androidComponents.onVariants { }`:

```kotlin
// AGP 8.13
android.applicationVariants.all { variant ->
    variant.signingConfig.enableV1Signing = false
}
// AGP 9.0
androidComponents {
    onVariants { variant -> variant.signingConfig.enableV1Signing.set(false) }
}
```

**Почему это критично для нашего шаблона**: любой ресёрч «как писать Android», основанный на
статьях/сэмплах 2023–2025, даёт **нерабочие** `build.gradle.kts`. Все листинги ниже приведены в
AGP-9-совместимом виде; ошибки этого класса — самая частая причина, по которой агент не соберёт
проект с первой попытки.

Официальный ассистент миграции: `developer.android.com/build/agp-upgrade-assistant`, рецепты —
`github.com/android/gradle-recipes` (ветка `agp-9.0`), агентный скилл —
`github.com/android/skills/tree/main/build-system/agp/agp-9-upgrade`. [проверено]

#### 1.3. Jetpack / AndroidX — что stable на 2026-09-17

Все цифры ниже — из `developer.android.com/jetpack/androidx/versions` (страница обновлена 2026-09-09/10)
и Google Maven group-index. [проверено]

| Библиотека | Stable | Latest | Дата релиза | Комментарий |
|---|---|---|---|---|
| `androidx.compose:compose-bom` | **2026.09.00** | 2026.09.00 | 2026-09-09 | → compose 1.12.1, material3 1.4.0, adaptive 1.3.0 |
| `androidx.compose.runtime/ui/foundation/animation` | **1.12.1** | 1.13.0-alpha03 | 2026-09-09 | |
| `androidx.compose.material3:material3` | **1.4.0** (stable с 2025-09-24) | 1.5.0-alpha28 (2026-09-09) | — | ⚠️ **Material 3 Expressive в stable НЕТ** — только в 1.5.0-alphaNN, см. §3.4 |
| `androidx.compose.material3.adaptive:adaptive*` | **1.3.0** | 1.4.0-alpha02 | 2026-09-09 | есть артефакт `adaptive-navigation3` |
| `androidx.activity:activity-compose` | **1.13.0** | 1.14.0-alpha02 | 2026-09-09 | `enableEdgeToEdge()` живёт здесь |
| `androidx.lifecycle:*` | **2.11.0** | 2.12.0-alpha03 | 2026-09-09 | в т. ч. `lifecycle-viewmodel-navigation3` |
| `androidx.navigation:navigation-compose` | **2.10.1** | 2.10.1 | 2026-09-09 | Navigation 2 («классика») |
| `androidx.navigation3:navigation3-runtime/-ui` | **1.1.7** | 1.2.0-rc01 | 2026-08-26 / 2026-09-09 | **Navigation 3**, 1.0.0 stable — 2025-11-19 |
| `androidx.navigationevent` | 1.1.2 | 1.2.0-alpha04 | 2026-08-12 | предиктивный back для Compose |
| `androidx.room:room-*` | **2.8.5** | 2.8.5 | 2026-09-09 | Android-only линия |
| `androidx.room3:room3-*` | **3.0.3** | 3.1.0-alpha01 | 2026-09-09 | новый major: KMP, KSP-only, suspend-API |
| `androidx.sqlite` | 2.7.1 | 2.8.0-alpha01 | 2026-09-09 | |
| `androidx.datastore:datastore-preferences` | **1.2.1** | 1.3.0-alpha11 | 2026-09-09 | |
| `androidx.work:work-runtime-ktx` | **2.11.2** | 2.12.0-rc01 | 2026-08-12 | |
| `androidx.paging:paging-compose` | **3.5.1** | 3.5.1 | 2026-08-12 | |
| `androidx.hilt:hilt-navigation-compose`, `hilt-work`, `hilt-compiler` | **1.4.0** | 1.4.0 | 2026-07-01 | «мост» Hilt↔Jetpack |
| `androidx.core:core-ktx` | **1.19.0** | 1.19.0 | — | |
| `androidx.core:core-splashscreen` | 1.2.0 | 1.2.0 | — | |
| `androidx.window:window` | **1.5.1** | 1.6.0-alpha05 | 2026-06-17 | window size classes |
| `androidx.credentials:credentials` | **1.6.0** | 1.7.0-alpha03 | 2026-07-29 | passkeys / Credential Manager |
| `androidx.benchmark:*` (macro, baselineprofile plugin) | **1.5.0** | 1.5.0 | 2026-09-09 | |
| `androidx.profileinstaller` | 1.4.1 | 1.4.1 | 2024-10-02 | |
| `androidx.startup:startup-runtime` | 1.2.0 | 1.2.0 | 2024-09-18 | |
| `androidx.test:espresso-core` | **3.7.0** | 3.7.0 | — | |
| `androidx.test.ext:junit` | **1.3.0** | 1.3.0 | — | |
| `androidx.test.ext:truth` | 1.7.0 | 1.7.0 | — | |
| `androidx.test:runner` / `core-ktx` | 1.7.0 | 1.7.0 | — | |
| `androidx.test.uiautomator:uiautomator` | **2.4.0** | 2.4.0 | 2026-07-01 | |
| `androidx.media3` | 1.11.1 | 1.11.1 | 2026-09-10 | не нужен нашему профилю |
| `androidx.camera` | 1.6.2 | 1.7.0-alpha03 | 2026-08-26 | нужен для QR/скана |
| `androidx.glance` | 1.2.0 | 1.3.0-alpha02 | 2026-08-26 | виджеты; опционально |

**Compose BOM 2026.09.00 → что внутри** (из `compose-bom-2026.09.00.pom`, [проверено]):

```
androidx.compose.runtime:runtime      = 1.12.1
androidx.compose.ui:ui                = 1.12.1
androidx.compose.foundation:foundation= 1.12.1
androidx.compose.animation:animation  = 1.12.1
androidx.compose.material:material    = 1.12.1
androidx.compose.material3:material3  = 1.4.0
androidx.compose.material3.adaptive:adaptive = 1.3.0
androidx.compose.material:material-icons-core = 1.7.8   (заморожен — иконки больше не растут)
```

**Compose compiler.** Отдельный артефакт `androidx.compose.compiler:compiler` заморожен на **1.5.15**
(последний до Kotlin 2.0) — с Kotlin 2.x компилятор Compose поставляется **внутри Kotlin** и
включается плагином `org.jetbrains.kotlin.plugin.compose` версии, равной версии Kotlin. Указывать
`composeOptions { kotlinCompilerExtensionVersion = … }` больше не надо. [проверено — Google Maven
`androidx.compose.compiler` последняя версия 1.5.15; `developer.android.com/jetpack/androidx/versions`
показывает compose.compiler датой 2024-08-07]

#### 1.4. Room 2 vs Room 3

**[проверено]** `developer.android.com/jetpack/androidx/releases/room3`:

- `androidx.room3:room3-*` **3.0.3** (2026-09-09) — отдельные координаты и **отдельный пакет**
  `androidx.room3.*`. Это не «Room 2.9», а параллельная линия.
- Отличия: **только Kotlin + KSP** (ни kapt, ни Java AP), все операции — `suspend`,
  `SQLiteDriver` вместо `SupportSQLite`, `InvalidationTracker` через `Flow`, KMP
  (Android/JVM/iOS/macOS/tvOS/watchOS/JS/WasmJS), FTS5, `@DaoReturnTypeConverter`,
  `kotlin.uuid.Uuid`, конфигурируемый пул соединений.
- Транзакции: `runInTransaction {}` → `withWriteTransaction {}`; сырой доступ —
  `useReaderConnection { connection -> connection.usePrepared("…") { stmt -> … } }`.
- Плагин: `id("androidx.room3")` + блок `room3 { schemaDirectory("$projectDir/schemas") }`.
- Для постепенной миграции есть `androidx.room3:room3-sqlite-wrapper` (`getSupportWrapper()`).

**Вывод для шаблона [предположение]**: для Android-only бизнес-приложения брать **Room 2.8.5**
(вся экосистема и все гайды под неё), а Room 3 — если/когда шаблон пойдёт в KMP (поток 03).
Но ещё важнее: наш профиль — *offline-tolerant, не offline-first*, и в reference-фичах
(auth/profile/todos) **БД скорее всего не нужна вовсе**: `DataStore` для настроек/сессии +
кеш TanStack-подобного слоя (на Android это просто `StateFlow` в репозитории или
`Store`/`SavedStateHandle`). Room имеет смысл добавлять точечно, когда появится оффлайн-список.

#### 1.5. Сеть, сериализация, изображения

| Библиотека | Stable | Опубликовано | Лицензия | Примечание |
|---|---|---|---|---|
| `com.squareup.retrofit2:retrofit` | **3.0.0** | 2025-05-15 | Apache-2.0 | major-версия; `converter-kotlinx-serialization` встроен |
| `com.squareup.okhttp3:okhttp` | **5.5.0** | 2026-08-16 | Apache-2.0 | 5.x — Kotlin-переписанный |
| `com.squareup.okhttp3:mockwebserver3(-junit5)` | 5.5.0 | 2026-08-16 | Apache-2.0 | новые координаты в 5.x |
| `io.ktor:ktor-client-*` | **3.6.0** | 2026-09-16 | Apache-2.0 | KMP; очень активный релиз-цикл |
| `org.jetbrains.kotlinx:kotlinx-serialization-json` | **1.11.0** | — | Apache-2.0 | 1.12.0-RC в пути |
| `org.jetbrains.kotlinx:kotlinx-coroutines-core` | **1.11.0** | 2026-05-08 | Apache-2.0 | |
| `org.jetbrains.kotlinx:kotlinx-datetime` | **0.8.0** | — | Apache-2.0 | всё ещё 0.x; есть вариант `-0.6.x-compat` |
| `io.coil-kt.coil3:coil-compose` | **3.6.2** | 2026-09-04 | Apache-2.0 | KMP-версия Coil; Glide для нового кода не берём |

[проверено — Maven Central maven-metadata.xml каждого артефакта, 2026-09-17]

**Выбор HTTP-клиента [предположение, обоснование ниже].** Для Android-only шаблона, который
разговаривает с нашим Go/huma-бэкендом по OpenAPI-контракту, **Retrofit 3 + OkHttp 5 +
kotlinx-serialization** — более консервативный и «нанимаемый» выбор: меньше движущихся частей,
понятные интерфейсы-API, `MockWebServer` из той же семьи для тестов, `OkHttp` всё равно лежит под
Ktor на Android. **Ktor client 3.6** выигрывает ровно тогда, когда есть горизонт KMP (Retrofit
JVM-only: опирается на `java.lang.reflect.Proxy`) — это решается в потоке 03, и решение
«Retrofit vs Ktor» нужно принимать вместе с ним, а не отдельно. Подробности cookie-авторизации
(наш бэкенд ставит httpOnly `access_token`/`refresh_token`, `Authorization: Bearer` не поддержан) —
поток 07; здесь фиксирую только, что обе библиотеки это умеют: OkHttp — через `CookieJar`
(+ `PersistentCookieJar`/свой на DataStore), Ktor — через плагин `HttpCookies`.

#### 1.6. DI-ландшафт 2026

| Решение | Версия | Опубликовано | Тип | Лицензия |
|---|---|---|---|---|
| **Hilt** (`com.google.dagger:hilt-android`) | **2.60.1** | 2026-07-06 | annotation processor (KSP), compile-time | Apache-2.0 |
| `androidx.hilt:hilt-navigation-compose` / `hilt-work` / `hilt-compiler` | **1.4.0** | 2026-07-01 | мост Hilt↔Jetpack | Apache-2.0 |
| **Metro** (`dev.zacsweers.metro:runtime`) | **1.4.3** | 2026-09-08 | Kotlin compiler plugin, compile-time, KMP | Apache-2.0 |
| **Koin** (`io.insert-koin:koin-android`) | **4.2.2** | — | runtime service locator, KMP | Apache-2.0 |
| **kotlin-inject** (`me.tatarka.inject:kotlin-inject-runtime`) | **0.9.0** | 2026-01-07 | KSP, compile-time, KMP | Apache-2.0 |
| Dagger (голый) | 2.60.1 | 2026-07-06 | KSP/kapt | Apache-2.0 |

[проверено — Maven Central, 2026-09-17]

- **Hilt** — то, что рекомендует официальный гайд («Use Hilt if your project is complex enough —
  for example, if it includes multiple screens with ViewModels, uses WorkManager, or has ViewModels
  scoped to the navigation back stack», `developer.android.com/topic/architecture/recommendations`
  [проверено]). Это Dagger с готовыми Android-компонентами и `@HiltViewModel`. Минусы: KSP-шаг
  сборки (время), Android-ориентированность (не KMP), исторически шумные ошибки генерации.
- **Metro** (Zac Sweers) — 1.0.0 stable вышел в апреле 2026, сейчас 1.4.3 (публикация 2026-09-08 —
  проект очень активен). Реализован как **компиляторный плагин**: нет kapt, нет KSP, нет
  сгенерированных исходников в дереве; граф описывается интерфейсами; есть Anvil-подобный
  contribution-model, assisted injection, multibindings, graph extensions; KMP из коробки.
  Публичный кейс продовой миграции — Vinted (`vinted.engineering/2026/02/12/from-dagger-to-metro/`).
  [проверено — версии Maven Central; блог-посты `zacsweers.dev/introducing-metro/`,
  `zacsweers.dev/metro-is-stable/`]
- **Koin 4.2.2** — runtime-DI: быстрее в сборке, проще входной порог, но ошибки графа всплывают
  в рантайме, а не при компиляции. Для «шаблона, который отдают заказчику» это минус: агент/джун
  сломает граф и узнает об этом на экране, а не в `make check`.

**Рекомендация для шаблона [предположение]**: **Hilt 2.60.1 + androidx.hilt 1.4.0**. Причины:
(1) это официальная рекомендация и де-факто стандарт найма в РФ; (2) все сэмплы Google
(включая Now in Android) на нём — агент будет попадать в знакомый паттерн; (3) compile-time
проверка графа = ещё один гейт; (4) `@HiltViewModel` + `hiltViewModel()` — одна строка на экран.
Metro держать в поле зрения как кандидата на замену, если/когда шаблон уйдёт в KMP (поток 03) —
переход Hilt→Metro относительно механический (обе — конструкторная инъекция с аннотациями).

#### 1.7. UDF/MVI-фреймворки — статус

| Проект | Версия | Опубликовано | Статус | Комментарий |
|---|---|---|---|---|
| **Slack Circuit** (`com.slack.circuit:circuit-foundation`) | **0.39.0** | 2026-09-15 | активен, **до 1.0** | Compose-first UDF: `Screen` + `Presenter` + `Ui`, KMP, Apache-2.0, в проде у Slack |
| **Orbit MVI** (`org.orbit-mvi:orbit-core`) | **12.0.1** | — | активен | «container»-подход, side-effects из коробки |
| **Decompose** (`com.arkivanov.decompose`) | 3.5.0 (3.6.0-beta01) | — | активен | компонентная навигация + lifecycle, KMP |
| **Molecule** (`app.cash.molecule:molecule-runtime`) | **2.2.0** | — | активен | Compose-runtime как способ собирать `StateFlow` из presenters |
| Airbnb Mavericks | — | — | **фактически заброшен** | не проверено детально; в 2026 не встречается в новых проектах [предположение] |

[проверено — Maven Central, 2026-09-17; Circuit — `slackhq.github.io/circuit/`]

**Вывод [предположение, но с опорой на официальные рекомендации]**: база для бизнес-приложения —
**официальный MVVM+UDF** (`ViewModel` + `StateFlow<UiState>` + методы для событий), а не
MVI-фреймворк. Три аргумента:

1. Официальный гайд прямо говорит «**Do not send events from the ViewModel to the UI**» (Strongly
   recommended) — то есть центральная фича MVI-фреймворков (side-effects/one-off events) идёт
   против официальной рекомендации. `developer.android.com/topic/architecture/recommendations`
   [проверено].
2. Любой MVI-фреймворк — это +1 зависимость, которую заказчик наследует и должен сопровождать,
   и +1 барьер найма.
3. Circuit хорош и реально лучше Navigation+ViewModel *в KMP-сценарии* (он же решает навигацию),
   но он **до 1.0** (0.39.0) — для шаблона, отдаваемого клиенту, это неприемлемый риск API-слома.

Circuit остаётся сильным кандидатом, если поток 03 выберет **Compose Multiplatform** — тогда
Circuit закрывает и презентацию, и навигацию единообразно на обеих платформах.


### §2. Архитектура: что реально рекомендуется в 2026

#### 2.1. Официальный «Guide to app architecture» — ядро

Источники [проверено, 2026-09-17]: `developer.android.com/topic/architecture`,
`…/topic/architecture/recommendations`, `…/topic/architecture/ui-layer`,
`…/topic/architecture/data-layer`.

Слои: **UI layer → (Domain layer — опционален) → Data layer**. Минимум — два слоя.
Принципы: separation of concerns, drive UI from data models, **single source of truth (SSOT)**,
**unidirectional data flow (UDF)** во всех слоях.

Таблица официальных рекомендаций (приоритеты — дословно из
`developer.android.com/topic/architecture/recommendations`) [проверено]:

| Тема | Рекомендация | Приоритет |
|---|---|---|
| Слои | Явный **data layer**; репозиторий даже на один источник | Strongly recommended |
| Слои | Явный **UI layer**; Compose — рекомендованный тулкит | Strongly recommended |
| Слои | Доступ к данным **только через репозиторий** (ни ViewModel, ни composable не ходят в БД/сеть) | Strongly recommended |
| Слои | Корутины и Flow для связи между слоями | Strongly recommended |
| Слои | **Domain layer — только в больших приложениях** (переиспользование логики между ViewModel'ями / упрощение сложной VM) | Recommended in big apps |
| UI | UDF: VM отдаёт состояние наблюдаемо, получает действия **вызовами методов** | Strongly recommended |
| UI | AAC `ViewModel` для бизнес-логики и выдачи UiState | Strongly recommended |
| UI | Сбор состояния через **`collectAsStateWithLifecycle()`** | Strongly recommended |
| UI | **«Do not send events from the ViewModel to the UI»** — событие обрабатывается сразу и превращается в обновление состояния | Strongly recommended |
| UI | **Single-activity** приложение; **Navigation 3** между экранами | Strongly recommended |
| UI | Jetpack Compose для новых приложений | Strongly recommended |
| VM | VM не держит ссылок на lifecycle-типы (`Activity`, `Context`, `Resources`) | Strongly recommended |
| VM | VM — **на уровне экрана**, не в переиспользуемых кусках UI | Strongly recommended |
| VM | В переиспользуемом UI — **plain state holder** классы с hoisting | Strongly recommended |
| VM | **Не использовать `AndroidViewModel`** | Recommended |
| VM | Одно свойство `uiState: StateFlow<…>`; `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)` | Recommended |
| DI | Конструкторная инъекция | Strongly recommended |
| DI | **Hilt**, если проект достаточно сложный | Recommended |
| Тесты | Минимум: unit на ViewModel (включая Flow), unit на репозитории/источники, UI-навигационные тесты в CI | Strongly recommended |
| Тесты | **Fakes, а не mocks** | Strongly recommended |
| Тесты | `StateFlow` тестировать через `.value` там, где возможно | Strongly recommended |
| Модели | Своя модель на слой в сложных приложениях | Recommended |
| Имена | Методы — глагольные фразы (`makePayment()`) | Optional |
| Имена | Свойства — именные фразы (`inProgressTopicSelection`) | Optional |
| Имена | Потоки — `get{Model}Stream()` / `get{Models}Stream()` | Optional |
| Имена | Реализации интерфейсов — осмысленно (`OfflineFirstNewsRepository`, `InMemoryNewsRepository`), иначе префикс `Default`; фейки — префикс **`Fake`** | Optional |

Канон UI-слоя [проверено, `…/ui-layer`]:

- `UiState` — **immutable `data class`**, имя = _функциональность_ + `UiState` (`NewsUiState`,
  `SignInUiState`). «Only sources or owners of data should be responsible for updating the data
  they expose.»
- Один объект состояния на связанные данные; отдельные потоки — только для несвязанных/частых.
- `PagingData` **не кладут** в immutable `UiState` — отдельный поток.
- Loading/error — поля состояния (`isFetchingArticles`, `userMessages: List<Message>`), не
  отдельный канал.
- Вся работа в VM должна быть **main-safe** — переключение потоков делает data/domain-слой.
- UI-логика (что и как показать) — в UI/простых state holder'ах, не в VM.

Канон data-слоя [проверено, `…/data-layer`]:

- Имена: `{DataType}Repository` (`NewsRepository`), `{DataType}{Source}DataSource`
  (`NewsRemoteDataSource`, `NewsLocalDataSource`). **Без деталей реализации в имени**
  (не `UserSharedPreferencesDataSource`).
- Подписки — `val data: Flow<T>`, одноразовые операции — `suspend fun`.
- Main-safety — `withContext(ioDispatcher) { … }` внутри репозитория/источника.
- Данные наружу — **immutable**.
- Выбор хранилища: большие запрашиваемые наборы — Room; маленькие key-value — DataStore;
  JSON-куски — файлы. Для offline-first SSOT — локальная БД.
- Типы операций: UI-oriented (живут с VM), app-oriented (scope приложения),
  business-oriented (переживают смерть процесса → **WorkManager**).
- Тесты: unit — на фейках; интеграционные — in-memory Room + MockWebServer.

**Это и есть «фундаментальная устойчивая практика»** [предположение, но обоснованное]: гайд
эволюционирует только в деталях (`collectAsStateWithLifecycle` вместо `repeatOnLifecycle`,
Navigation 3 вместо Navigation 2), а слои/UDF/SSOT/immutable state держатся с 2020 года.
Всё, что сверху — фреймворки презентации — сменилось уже несколько раз.

#### 2.2. Domain layer: нужен ли нам

Официально — «**optional**, use it only when needed». Для нашего профиля (CRM-подобные
приложения-компаньоны к Go-бэкенду: списки/формы/детали, немного бизнес-правил) правильный ответ
[предположение]:

- **Не заводить обязательный `domain`-модуль и не плодить use-case на каждый вызов репозитория.**
  `LoginUseCase`, который состоит из `repository.login(...)`, — чистый шум и главная болезнь
  «Clean Architecture по-андроидному».
- Завести **`core:domain`** как *опциональный* модуль и правило: use-case появляется тогда и
  только тогда, когда логика (а) используется больше чем одной ViewModel, либо (б) комбинирует
  ≥ 2 репозитория, либо (в) содержит нетривиальное правило, которое хочется покрыть
  JVM-тестом отдельно от VM.
- Именование: `<Verb><Noun>UseCase` с `operator fun invoke(...)` — это устоявшийся стиль NiA.

Это точный аналог правила `pages-first` из `spa-frontend-template` («выносим в widget/feature
только когда реально переиспользуется») и правила go-backend-template «I/O — это use-case, не
метод агрегата».

#### 2.3. Навигация: Navigation 3

**[проверено]** Официальные рекомендации архитектуры теперь говорят: «Use a single-activity
application… **Use Navigation 3** to navigate between screens and deep link to your app if your
app has more than one screen» (Strongly recommended).

Статус [проверено, `developer.android.com/jetpack/androidx/releases/navigation3`]:
1.0.0 — 2025-11-19, 1.0.1 — 2026-02-11, 1.1.0 — 2026-04-08, **1.1.7 — 2026-08-26** (stable),
1.2.0-rc01 — 2026-09-09.

Артефакты: `androidx.navigation3:navigation3-runtime`, `androidx.navigation3:navigation3-ui`,
`androidx.lifecycle:lifecycle-viewmodel-navigation3` (ViewModel, скоупленный к записи back stack),
`androidx.compose.material3.adaptive:adaptive-navigation3` (адаптивные Scene-стратегии).

Идея: **back stack — это обычный список**, которым владеет приложение; библиотека рендерит его.
Канонический код (из `github.com/android/nav3-recipes`, файл
`app/src/main/java/com/example/nav3recipes/basicsaveable/BasicSaveableActivity.kt`) [проверено]:

```kotlin
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import kotlinx.serialization.Serializable

@Serializable private data object RouteA : NavKey
@Serializable private data class RouteB(val id: String) : NavKey

setContent {
    val backStack = rememberNavBackStack(RouteA)   // переживает смерть процесса

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = { key ->
            when (key) {
                is RouteA -> NavEntry(key) { /* экран A */ }
                is RouteB -> NavEntry(key) { /* экран B, key.id */ }
                else -> error("Unknown route: $key")
            }
        }
    )
}
```

Есть и DSL-форма `entryProvider` [проверено, `developer.android.com/guide/navigation/navigation-3/basics`]:

```kotlin
entryProvider = entryProvider {
    entry<ProductList> { Text("Product List") }
    entry<ProductDetail>(metadata = mapOf("extraDataKey" to "extraDataValue")) { key ->
        Text("Product ${key.id}")
    }
}
```

Отличия от Navigation Compose 2.x (официально) [проверено]: проще интеграция с Compose;
**полный контроль над back stack**; можно отрисовать **несколько записей одновременно** —
именно это даёт адаптивные layout'ы (list-detail на планшете) без костылей.

Что это значит для шаблона [предположение]:

- Ключи маршрутов — `@Serializable` `data object`/`data class`, реализующие `NavKey`. Это ровно
  типобезопасная навигация, о которой просил фронт-шаблон в лице TanStack Router.
- `rememberNavBackStack` даёт сохранение стека через смерть процесса — обязательное свойство.
- Навигация — не «магический граф», а **состояние приложения**; значит, её можно и нужно
  тестировать unit-тестом (и это отличная замена «UI navigation tests» из рекомендаций).
- **Риск**: экосистема (гайды, статьи, чужой код) всё ещё в основном про Navigation 2.x. Агент
  без явного правила в `CLAUDE.md` напишет `NavHost`/`composable(route = "…")`. Это надо
  зафиксировать правилом и примером в reference-фичах.
- Официальный агентный скилл существует: `android skills add navigation-3`
  (`github.com/android/skills`) [проверено].


### §3. Модульность, файловая структура, токены, i18n

#### 3.1. Официальный канон модуляризации

Источники [проверено, 2026-09-17]: `developer.android.com/topic/modularization`,
`developer.android.com/topic/modularization/patterns`.

Типы модулей:

| Тип | Что внутри | Зависит от |
|---|---|---|
| **app** | точка входа, корневая навигация, wiring DI, манифест; выход — AAB/APK | feature-модули |
| **feature** | изолированный кусок функциональности = экран или серия близких экранов; UI + ViewModel | data/core-модули |
| **data** | инкапсулирует данные и бизнес-логику домена; **наружу торчит репозиторий**, источники спрятаны | core |
| **common/core** | то, что не принадлежит слою: UI-киты, аналитика, сеть, утилиты | — |
| **test** | общий тестовый код, интеграционные тесты | всё, что тестирует |

Принципы (дословные тезисы официального гайда) [проверено]:

- **High cohesion / low coupling**. «Modules shouldn't have knowledge of the inner workings of
  other modules». Если два модуля сильно знают друг о друге — это один модуль; если две части
  модуля почти не общаются — это два модуля.
- **Feature ↔ feature общаются через медиатора** (обычно `app` с корневой навигацией) и через
  **общий data-модуль**. «You shouldn't pass objects as navigation arguments. Instead, use simple
  ids that features can use to access and load desired resources from the data layer.» Это
  один-в-один наше фронтовое правило «cross-feature — только через подъём в entities/shared или
  композицию в widget/page» и бэкендовое «cross-domain — только порты + адаптеры».
- **Expose as little as possible**: `internal`/`private`, `implementation` вместо `api`.
- **Keep configuration consistent**: **version catalogs** + **convention plugins**.
- **Предпочитать чистые Kotlin/Java-модули** Android-модулям («Android modules come with
  overhead») — то есть `core:model`, `core:domain`, `core:common` лучше делать
  `kotlin("jvm")`-модулями, а не android-library.
- **`api`/`impl`-разделение** (dependency inversion) официально рекомендуется **только когда есть
  реальная причина**: несколько реализаций (OpenGL vs Vulkan, Play-биллинг vs свой),
  несколько приложений, независимые команды, очень большая кодовая база. Пример подстановки —
  `releaseImplementation(project(":database:impl:firestore"))` /
  `debugImplementation(project(":database:impl:room"))`.

**Оценка для нашего профиля [предположение]**: `api`/`impl`-раскол окупается на десятках модулей
и нескольких командах. Для бизнес-приложения-компаньона он даст удвоение числа модулей и
церемонии — **не берём в базовый шаблон**, но фиксируем как задокументированную эволюцию
(правило в `.claude/rules`, когда её применять). Ровно как `spa-frontend-template` не тащит
монорепо-пакеты.

#### 3.2. Как это соотносится с FSD фронта и доменами Go-бэкенда

| Понятие | go-backend-template | spa-frontend-template (FSD) | Android (предлагаемое) |
|---|---|---|---|
| вертикальный срез | `internal/domain/<domain>` | слайс `features/<x>`, `pages/<x>` | Gradle-модуль `:feature:<x>` |
| общий фундамент | `internal/platform/*`, `golibinfra` | `shared/*`, `entities/*` | `:core:*` |
| точка сборки | `cmd/api` + `fx` | `app/` (providers, router) | `:app` |
| контракт наружу | пакет + экспортируемые типы | `index.ts` (explicit re-exports) | Kotlin `internal` + минимальный публичный API модуля |
| запрет соседей | depguard (`<domain>-domain-isolation`) | Steiger `forbidden-imports` + `eslint-plugin-boundaries` | **сам Gradle-граф**: `:feature:a` просто не объявляет зависимость на `:feature:b` |
| «сегменты по назначению» | `entity.go/dto.go/usecase.go/...` | `ui/model/api/lib/config` | пакеты `ui/`, `data/`, `domain/`, `navigation/` внутри модуля |

**Ключевое отличие Android в лучшую сторону**: запрет cross-feature импорта обеспечивается
**физически** (нет зависимости в `build.gradle.kts` → нет классов в classpath), а не линтером.
Это сильнее, чем Steiger/depguard. Соответственно, «архитектурный линтер» на Android нужен не
столько для слоёв, сколько для (а) naming-конвенций, (б) запрета API (логгер, Toast, Date),
(в) проверки, что `:feature:*` не подтянул лишнего (это ловит `dependency-analysis`).

**Ключевое отличие в худшую сторону**: цена модуля. Каждый Android-модуль — это отдельная
компиляция, R-класс, манифест, KSP-проход. На слабой машине 12 модулей собираются заметно
дольше, чем один. Поэтому шаблон должен начинаться с **минимального разумного набора модулей**
и иметь описанную процедуру «когда дробить».

#### 3.3. Предлагаемое дерево модулей шаблона (черновик)

```
android-template/
├─ settings.gradle.kts            # pluginManagement + includeBuild("build-logic")
├─ build.gradle.kts               # только plugins { … apply false } + общие таски (detekt/kover)
├─ gradle.properties
├─ gradle/
│  ├─ libs.versions.toml          # единственный источник версий
│  └─ wrapper/…
├─ build-logic/                   # включённая сборка (includeBuild)
│  ├─ settings.gradle.kts
│  └─ convention/
│     ├─ build.gradle.kts         # gradlePlugin { plugins { register(...) } }
│     └─ src/main/kotlin/…        # *ConventionPlugin.kt
├─ app/                           # :app — Application, MainActivity, NavDisplay, DI-корень
├─ core/
│  ├─ model/                      # kotlin-jvm: доменные модели (зеркало Go-entity)
│  ├─ common/                     # kotlin-jvm: Result-обёртки, диспетчеры, утилиты
│  ├─ network/                    # android-lib: Retrofit/OkHttp, cookie-jar, DTO+мапперы
│  ├─ datastore/                  # android-lib: DataStore (сессия, настройки, тема, локаль)
│  ├─ data/                       # android-lib: репозитории (SSOT), склейка network+datastore
│  ├─ domain/                     # kotlin-jvm: use-case'ы — ТОЛЬКО когда оправданы (§2.2)
│  ├─ designsystem/               # android-lib: токен-слой + примитивы UI + превью
│  ├─ ui/                         # android-lib: переиспользуемые композиты поверх designsystem
│  ├─ analytics/                  # android-lib: интерфейс аналитики + no-op/Sentry-реализация
│  └─ testing/                    # android-lib (test-only): правила, фейки, диспетчеры, axe-аналоги
├─ feature/
│  ├─ auth/                       # login (зеркало features/auth фронта)
│  ├─ profile/                    # профиль + смена пароля (зеркало pages/profile)
│  ├─ todos/                      # список + CRUD (зеркало pages/todos)
│  └─ settings/                   # тема + язык (зеркало features/theme)
├─ lint/                          # jvm: кастомные Android Lint checks (аналог forbidigo)
└─ benchmarks/                    # baseline profiles + macrobenchmark (нужен девайс)
```

Внутри `feature/<name>` (черновик):

```
feature/todos/
├─ build.gradle.kts               # plugins { alias(libs.plugins.stit.android.feature) }
└─ src/
   ├─ main/kotlin/<pkg>/feature/todos/
   │  ├─ TodosRoute.kt            # @Serializable data object/class : NavKey + entry<>()
   │  ├─ TodosScreen.kt           # @Composable TodosScreen(uiState, onEvent) — stateless
   │  ├─ TodosViewModel.kt        # @HiltViewModel, uiState: StateFlow<TodosUiState>
   │  ├─ TodosUiState.kt          # sealed interface / data class
   │  └─ components/              # слайс-локальные composable'ы
   ├─ test/kotlin/…               # JVM: ViewModel + Robolectric/Compose + screenshot
   └─ androidTest/kotlin/…        # только если реально нужен девайс
```

Naming-канон (предложение, согласовано с официальными «Optional»-рекомендациями и NiA):

| Сущность | Имя | Где |
|---|---|---|
| Composable-экран (stateless) | `<Feature>Screen` | `feature/<x>/<Feature>Screen.kt` |
| Composable-экран (stateful, с VM) | `<Feature>Route` **или** перегрузка `<Feature>Screen(viewModel)` | там же |
| Ключ навигации | `<Feature>Route` (`@Serializable … : NavKey`) | `feature/<x>/<Feature>Route.kt` |
| ViewModel | `<Feature>ViewModel` | там же |
| Состояние | `<Feature>UiState` (immutable data class / sealed interface) | там же |
| Репозиторий | `<DataType>Repository` + `Default<DataType>Repository`/`Offline<...>` | `core/data` |
| Источник данных | `<DataType>{Remote,Local}DataSource` | `core/network`, `core/datastore` |
| Use-case | `<Verb><Noun>UseCase` с `operator fun invoke` | `core/domain` |
| Фейк | `Fake<Thing>` | `core/testing` |
| Поток данных | `get<Model>Stream(): Flow<…>` | `core/data` |

Обрати внимание: **нет обязательного `Mapper`-класса** — мапперы делаются extension-функциями
`fun NetworkTodo.asExternalModel(): Todo` (стиль NiA), это меньше файлов и лучше тестируется.

#### 3.4. Design-system-agnostic токен-слой на Android

Задача как во фронте: **своя семантика, никакой вендорной DS**, тема переключается в одном месте.
Compose даёт два механизма [проверено, `developer.android.com/develop/ui/compose/designsystems/custom`]:

1. **`MaterialTheme` со своими `ColorScheme`/`Typography`/`Shapes`** — Material-компоненты
   (`Button`, `Card`, `TextField`, `Scaffold`) читают токены из него, значит их не надо
   переписывать. Это и есть «design-system-agnostic» в правильном смысле: Material 3 здесь —
   *движок токенов и примитивов*, а не чужая брендовая DS.
2. **Собственные `CompositionLocal`-системы** для того, чего в Material нет (градиенты,
   elevation-семантика, отступы-шкала, брендовые статусы). Канонический код:

```kotlin
@Immutable
data class AppSpacing(val xs: Dp, val sm: Dp, val md: Dp, val lg: Dp)

val LocalAppSpacing = staticCompositionLocalOf {
    AppSpacing(Dp.Unspecified, Dp.Unspecified, Dp.Unspecified, Dp.Unspecified)
}

object AppTheme {                              // точка доступа
    val spacing: AppSpacing
        @Composable @ReadOnlyComposable get() = LocalAppSpacing.current
}
```

**Именно так делает Now in Android** [проверено — `core/designsystem/.../theme/Theme.kt`,
`Background.kt` в `android/nowinandroid@main`]: `NiaTheme` собирает `colorScheme` (свой
`lightColorScheme`/`darkColorScheme` из палитры, либо `dynamic*ColorScheme`, либо «android»-вариант),
кладёт три собственных `staticCompositionLocalOf` — `LocalGradientColors`, `LocalBackgroundTheme`,
`LocalTintTheme` — и оборачивает всё в `MaterialTheme(colorScheme = …, typography = NiaTypography)`.
Фрагмент:

```kotlin
CompositionLocalProvider(
    LocalGradientColors provides gradientColors,
    LocalBackgroundTheme provides backgroundTheme,
    LocalTintTheme provides tintTheme,
) {
    MaterialTheme(colorScheme = colorScheme, typography = NiaTypography, content = content)
}
```

Соответствие с фронтовым правилом `shared-ui`:

| spa-frontend-template | android-template |
|---|---|
| `src/app/styles/index.css` (`@theme` + `data-scheme`) | `core/designsystem/.../theme/{Color,Type,Shape,Spacing,Theme}.kt` |
| семантические утилиты `bg-action-bg`, `text-muted` | `MaterialTheme.colorScheme.primary`, `AppTheme.spacing.md` |
| запрет `bg-[#18181b]`, `text-zinc-500` | запрет литералов `Color(0xFF…)`/`16.dp` вне `core:designsystem` |
| `features/theme` переключает `data-scheme` | `feature/settings` пишет выбор в DataStore → `AppTheme(darkTheme = …)` |
| `shared/ui/<name>/index.ts` | `core/designsystem/.../component/<Name>.kt` + `@Preview` |

**Dynamic color** (обои пользователя, Android 12+): `dynamicLightColorScheme(context)` /
`dynamicDarkColorScheme(context)`. Для корпоративного приложения с брендом это **выключено по
умолчанию** (NiA так и делает: `disableDynamicTheming: Boolean = true`), но оставлено флагом.

**Material 3 Expressive — в stable его НЕТ, и это меняет решение.** Отдельного артефакта
`material3-expressive` не существует; Expressive — набор API внутри того же
`androidx.compose.material3:material3`. Ключевая цитата из release notes **1.4.0-beta01**
(2025-07-30), раздел Breaking changes [проверено,
`developer.android.com/jetpack/androidx/releases/compose-material3`]:

> «All public APIs tagged with `ExperimentalMaterial3ExpressiveApi` or
> `ExperimentalMaterial3ComponentOverrideApi` **have been removed**, please switch to
> **1.5.0-alpha** to continue enjoying these features.»

То есть Google сознательно вырезал Expressive из линии 1.4.x перед её стабилизацией. Хронология
[проверено, release notes]: анонс на Google I/O 2025 → вырезан из 1.4.x (1.4.0-beta01, 2025-07-30)
→ **1.4.0 stable без Expressive (2025-09-24)** → ветка 1.5.0-alpha01 (2025-07-30) → …
→ **1.5.0-alpha28 (2026-09-09)**. Линия 1.5.0 сидит в альфе более 13 месяцев, беты нет;
Compose BOM 2026.09.00 отдаёт **1.4.0**.

**Вывод для шаблона [предположение]: брать `material3` 1.4.0 через BOM, Expressive не брать.**
(1) альфа AndroidX не даёт гарантий API — реальные ломающие изменения идут *между альфами*:
в 1.5.0-alpha28 stateless-перегрузки `Slider`/`RangeSlider` объявлены deprecated в пользу
`SliderState`/`RangeSliderState`, удалены deprecated-перегрузки `ToggleButton`, у
`SecureTextField` сменился дефолт обфускации; ранее удалён `DropdownMenuItem`,
`SplitButtonLayout` → `SplitButton`. (2) Expressive — про «engaging and desirable» потребительские
продукты, а не про плотные формы и таблицы. (3) Всё практически нужное бизнес-UI уже в 1.4.0:
`SecureTextField`/`OutlinedSecureTextField` (пароли), новые search-bar API (`SearchBar`,
`ExpandedFullScreenSearchBar`, `ExpandedDockedSearchBar`, `SearchBarState`), `TimePickerDialog`,
`autoSize` у `Text`, `VerticalDragHandle`. (4) Компромисс, если захочется «экспрессии» —
`MotionScheme.expressive()` доступен уже в 1.4.0 stable.

**Ловушка при переходе 1.3 → 1.4** [проверено, release notes 1.4.0]: material3 **больше не тянет
`material-icons-core`**, а сама библиотека `androidx.compose.material.icons` **прекращена**
(«We have stopped publishing updates to this library and it has been removed from the latest
Material 3 library release»; причина — «the library can increase the build time of your apps
significantly as it includes all the various icons»). Официальная замена — скачивать Material
Symbols как Vector Drawable XML с `fonts.google.com/icons`. **В шаблоне сразу закладываем
иконки как `res/drawable/*.xml`, а не `material-icons-extended`.** Там же: активный цвет лейбла
`NavigationBarItem`/`NavigationRailItem` сменился `onSurface` → `secondary`; компоненты перешли
на `MotionScheme`.

Запрет литералов надо гейтить. Варианты [предположение]: кастомный Android Lint-детектор
«hardcoded `Color(0x…)` / `.dp` вне `core:designsystem`» (аналог forbidigo) — самый точный;
временный костыль — detekt-правило `ForbiddenMethodCall` на `androidx.compose.ui.graphics.Color`.

#### 3.5. i18n (ru/en)

**[проверено]** `developer.android.com/guide/topics/resources/app-languages`:

Строки — всегда в `res/values/strings.xml` + `res/values-ru/strings.xml` (никаких строк в коде;
это гейтится Android Lint'ом — `HardcodedText`). Per-app language (Android 13+, API 33):

```kotlin
// build.gradle.kts модуля :app
android {
    androidResources {
        generateLocaleConfig = true      // AGP сам соберёт locale_config.xml и впишет в манифест
    }
}
```

+ файл `app/src/main/res/resources.properties`:

```properties
unqualifiedResLocale=en-US
```

Переключение из приложения (работает и ниже API 33 через AppCompat):

```kotlin
AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("ru"))
```

Требования: Activity наследует `AppCompatActivity`, appcompat ≥ 1.6.0; для Android 12 и ниже — в
манифест добавляется сервис-холдер:

```xml
<service
    android:name="androidx.appcompat.app.AppLocalesMetadataHolderService"
    android:enabled="false"
    android:exported="false">
    <meta-data android:name="autoStoreLocales" android:value="true" />
</service>
```

Ловушка [проверено]: при включённом `generateLocaleConfig = true` **нельзя** держать свой
`res/xml/locale_config.xml` — сборка упадёт.

**Сравнение с фронтом**: у SPA — Lingui runtime-only с каталогами; на Android ресурсы —
compile-time, и это проще и правильнее (плюрализация через `plurals`, форматирование —
`getString(R.string.x, arg)`). Аналог «переключателя языка в приложении» у нас уже есть в
`features/theme` фронта — на Android это `feature/settings`.

#### 3.6. Платформенные обязательности, которые шаблон должен нести из коробки

Сводная таблица — **§5.4**; точные API, дедлайны Play и разбор каждой позиции (edge-to-edge,
predictive back, `POST_NOTIFICATIONS`, photo picker, per-app language с `localeFilters`,
foreground services, App Links, Baseline Profiles, Credential Manager) — **§9**.


### §4. Образцы: Now in Android и реальные продовые проекты

Все цифры этого раздела получены 2026-09-17 чтением `main`/`develop`-веток (raw.githubusercontent +
GitHub API + tarball), а не по памяти. **Главный урок раздела: «канон из памяти» устарел** —
NiA за последний год пережил две ломающие перестройки.

#### 4.1. Сводка образцов

| Репозиторий | ★ | Последний коммит | Статус | UI | Модулей | DI | Навигация |
|---|---:|---|---|---|---:|---|---|
| `android/nowinandroid` | 21 814 | 2026-09-02 (`main`) | живой, эталон | Compose + M3 adaptive | **35** | Hilt 2.59 / KSP | **Navigation 3 1.0.0** |
| `android/architecture-samples` | 45 829 | **2025-07-17** | **стагнирует** | Compose | 2 | Hilt 2.53.1 | Navigation Compose 2.8.5 (строковые routes) |
| `android/compose-samples` | 23 461 | 2026-09-16 | живой (витрина UI) | Compose | Jetsnack 1 / Jetcaster 9 | Jetsnack — нет; Jetcaster — Hilt 2.60.1 | Navigation Compose 2.9.8 |
| `chrisbanes/tivi` | 6 725 | **2024-11-12** | **`archived: true`, deprecated** | Compose MP | ~65 | kotlin-inject 0.7.2 | Circuit 0.25.0 |
| `slackhq/circuit` | 1 852 | 2026-09-16 | живой | Compose MP 1.12.0 | библиотека + 9 sample-групп | **Metro 1.4.3** | свой `NavigableCircuitContent` |
| `element-hq/element-x-android` | 2 391 | 2026-09-16 (`develop`) | живой прод | Compose | **266 build-файлов** | **Metro 1.4.2** | Appyx 1.7.1 |
| `bitwarden/android` | 9 366 | 2026-09-16 | живой прод | Compose | **10** | Hilt 2.60.1 | Navigation Compose 2.9.8 + `@Serializable` routes |
| `signalapp/Signal-Android` | 29 344 | 2026-09-16 | живой прод | Compose **+ Fragments/viewBinding** | ~50 | **ручной DI** | Nav2 safe-args **+ Navigation3 1.1.4** в `core:ui` |

**Дисквалифицированные «классики»** — важно для последующих ресёрчей:
`chrisbanes/tivi` **архивирован** (README: репозиторий «завершён» 12.11.2024);
`android/architecture-samples` — последний коммит в `main` 2025-07-17, стек Kotlin 2.1 / AGP 8.7 /
compileSdk 35. Оба остаются в топе поисковой выдачи и **не** являются каноном 2026.

#### 4.2. Now in Android — что там на самом деле сейчас

Две перестройки, поменявшие канон (коммиты в `main`):

- `068d8cc93` (2025-11-19) «Split feature modules into api and impl modules»
- `d30083005` / `c9e38b329` (2025-11-19) «Refactor feature modules to navigation3» / «Refactor app to navigation3»

**Модули (35)**, `settings.gradle.kts`:

```
:app  :app-nia-catalog  :benchmarks  :lint  :ui-test-hilt-manifest
:core:{analytics, common, data, data-test, database, datastore, datastore-proto,
       datastore-test, designsystem, domain, model, navigation, network, notifications,
       screenshot-testing, testing, ui}
:feature:{foryou, interests, bookmarks, topic, search}:{api, impl}   :feature:settings:impl
:sync:work  :sync:sync-test
```

плюс `enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")` → зависимости пишутся
`implementation(projects.core.domain)`.

**`feature:<x>:api` в NiA — это не «api» из гайда по dependency inversion, а навигационный контракт**:
единственный файл `<Feature>NavKey.kt` (`@Serializable … : NavKey` + extension `Navigator.navigateToX()`)
плюс ресурсы, нужные другим фичам. Его convention-плагин — пять строк:

```kotlin
// AndroidFeatureApiConventionPlugin
apply(plugin = "nowinandroid.android.library")
apply(plugin = "org.jetbrains.kotlin.plugin.serialization")
dependencies { "api"(project(":core:navigation")) }
```

Правило связности (из `docs/ModularizationLearningJourney.md`, дословно):
«A feature's `api` module should not depend on another feature's `api` or `impl` module. A feature's
`impl` should only depend on another feature's `api` module.»

**17 convention-плагинов в `build-logic`** (id ↔ класс):

| id | класс | что делает |
|---|---|---|
| `nowinandroid.android.application` | `AndroidApplicationConventionPlugin` | `com.android.application` + lint + dependency-guard, `configureKotlinAndroid`, `targetSdk=36`, `animationsDisabled`, GMD, `printApks`, `badging`, Spotless |
| `nowinandroid.android.application.compose` | `AndroidApplicationComposeConventionPlugin` | + `org.jetbrains.kotlin.plugin.compose` + `configureAndroidCompose` |
| `nowinandroid.android.application.firebase` | `AndroidApplicationFirebaseConventionPlugin` | gms + crashlytics + perf |
| `nowinandroid.android.application.flavors` | `AndroidApplicationFlavorsConventionPlugin` | `demo`/`prod` по `FlavorDimension.contentType` |
| `nowinandroid.android.application.jacoco` / `...library.jacoco` | `*JacocoConventionPlugin` | покрытие (JaCoCo, не Kover) |
| `nowinandroid.android.library` | `AndroidLibraryConventionPlugin` | `com.android.library` + lint, `testOptions.targetSdk=36`, `lint.targetSdk=36`, **авто-`resourcePrefix` из пути модуля**, `disableUnnecessaryAndroidTests` |
| `nowinandroid.android.library.compose` | `AndroidLibraryComposeConventionPlugin` | + Compose |
| `nowinandroid.android.feature.api` | `AndroidFeatureApiConventionPlugin` | library + serialization + `api(:core:navigation)` |
| `nowinandroid.android.feature.impl` | `AndroidFeatureImplConventionPlugin` | library + hilt + `:core:ui`, `:core:designsystem`, lifecycle-compose, `navigation3-runtime` |
| `nowinandroid.hilt` | `HiltConventionPlugin` | KSP + `hilt.compiler`; для jvm-модулей `hilt-core`, для android — `dagger.hilt.android.plugin` + `hilt-android` |
| `nowinandroid.android.room` | `AndroidRoomConventionPlugin` | `androidx.room` плагин + KSP + `room.generateKotlin=true` + `schemaDirectory` |
| `nowinandroid.android.test` | `AndroidTestConventionPlugin` | `com.android.test` + GMD |
| `nowinandroid.android.lint` | `AndroidLintConventionPlugin` | `xmlReport`, `sarifReport`, `checkDependencies = true`, `disable += "GradleDependency"` |
| `nowinandroid.jvm.library` | `JvmLibraryConventionPlugin` | `kotlin.jvm` + lint + Spotless |
| `nowinandroid.root` | `RootPlugin` | graph-таски + Spotless на корне |

**`KotlinAndroid.kt` — точные значения** (дословно из `main`):

```kotlin
commonExtension.apply {
    compileSdk = 36
    defaultConfig.apply { minSdk = 23 }
    compileOptions.apply {
        // Up to Java 11 APIs are available through desugaring
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }
}
…
compilerOptions.apply {
    jvmTarget = JvmTarget.JVM_11
    allWarningsAsErrors = warningsAsErrors            // gradleProperty("warningsAsErrors"), default false
    freeCompilerArgs.add("-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi")
    freeCompilerArgs.add("-Xconsistent-data-class-copy-visibility")
}
```

| параметр | NiA |
|---|---|
| AGP / Kotlin / KSP | 9.3.2 / 2.3.0 / 2.3.4 |
| compileSdk / minSdk / targetSdk | 36 / **23** / 36 |
| `jvmTarget` | JVM_11 + core library desugaring |
| JDK для сборки | ≥ 17 (`check()` в settings), в CI — 21 (zulu) |
| Compose BOM | **`compose-bom-alpha` 2025.09.01** (!) + точечные alpha/rc-пины |
| Navigation | `androidx.navigation3 = 1.0.0` (Nav2 2.8.5 только для `navigation-testing`) |
| Hilt / Room / Retrofit / OkHttp | 2.59 / 2.8.3 / 2.11.0 / 4.12.0 |
| Тесты | Robolectric 4.16, Roborazzi 1.56.0, Turbine 1.2.0, Truth 1.4.4, GMD |
| Формат | Spotless 8.3.0 + ktlint 1.4.0 (`android=true`) |

**ViewModel в NiA получает NavKey через `@AssistedInject`**, а не парсит `SavedStateHandle`:

```kotlin
@HiltViewModel(assistedFactory = InterestsViewModel.Factory::class)
class InterestsViewModel @AssistedInject constructor(
    private val savedStateHandle: SavedStateHandle,
    val userDataRepository: UserDataRepository,
    getFollowableTopics: GetFollowableTopicsUseCase,
    @Assisted val key: InterestsNavKey,
) : ViewModel() {
    @AssistedFactory interface Factory { fun create(key: InterestsNavKey): InterestsViewModel }
}

sealed interface InterestsUiState {
    data object Loading : InterestsUiState
    data class Interests(val selectedTopicId: String?, val topics: List<FollowableTopic>) : InterestsUiState
    data object Empty : InterestsUiState
}
```
Вызов: `hiltViewModel<InterestsViewModel, InterestsViewModel.Factory> { it.create(key) }`.

**Три приёма NiA, которые стоит скопировать в наш шаблон без раздумий** [предположение]:

1. **Кастомный `:lint`-модуль с `lintPublish(projects.lint)`** в `core:designsystem` — правила
   публикуются потребителям модуля. У NiA там `DesignSystemDetector`, запрещающий использовать
   Material-компоненты (`Button`, `MaterialTheme`, …) мимо дизайн-системы. **Это прямой аналог
   `forbidigo` из `.golangci.yml`** и закрывает наше правило `shared-ui`.
2. **`graphUpdate`/`graphDump`** — генерирует mermaid-граф зависимостей модуля и вписывает его в
   `README.md` этого модуля между маркерами `<!--region graph-->…<!--endregion-->`; CI проверяет
   актуальность. README есть у 25 модулей. Для агента это машинно-проверяемая карта архитектуры.
3. **Авто-`resourcePrefix` из пути модуля** — ресурсы `:feature:interests:api` обязаны называться
   `feature_interests_api_*`, иначе сборка падает:
   ```kotlin
   resourcePrefix = path.split("""\W""".toRegex()).drop(1).distinct()
       .joinToString(separator = "_").lowercase() + "_"
   ```

Плюс `Badging.kt` (`checkProdReleaseBadging`) — фиксирует `aapt2 dump badging` (разрешения, фичи,
sdk) как baseline, чтобы APK не «отрастил» разрешение незаметно. Для приложения-компаньона с
камерой/пушами это дешёвый и очень полезный гейт.

**Честные долги NiA** (найдены в коде `main`) — чтобы не копировать их вместе с хорошим:
`// TODO: this violates SSOT, events should go through the ViewModel` в `InterestsScreen`;
`:feature:settings` без `api`-модуля и вызывается напрямую как `SettingsDialog()`;
осиротевшие screenshot-эталоны от старого `feature/foryou`; `AGENTS.md` и
`ModularizationLearningJourney.md` рассинхронизированы с кодом (описывают Navigation 2 и `NiaNavHost`);
Compose BOM из **alpha**-канала; 35 модулей на 310 `.kt`-файлов — по критерию собственного гайда
это «too fine-grained», оправдано только учебной ролью.

#### 4.3. Два лагеря архитектуры презентации

Единого канона нет — есть два живых лагеря [проверено по коду восьми репозиториев]:

| | Google-лагерь | Slack/Cash-лагерь |
|---|---|---|
| Носитель состояния | `ViewModel` + `StateFlow<UiState>` | `@Composable`-presenter, возвращающий `State` |
| DI | Hilt | Metro / kotlin-inject (+Anvil) |
| Навигация | Navigation 3 / Navigation Compose | Circuit `NavigableCircuitContent` / Appyx |
| Тесты презентации | Robolectric + `runTest` + Turbine | Molecule + `circuit-test` + Turbine |
| Носители | NiA, `bitwarden/android`, Jetcaster | `slackhq/circuit`, `element-hq/element-x-android`, tivi (архив) |

**Navigation 3 — единственное, что берут ОБА лагеря и даже Signal** (`androidx.navigation3 1.1.4`
в `core:ui` Signal, `1.0.0` в NiA), независимо друг от друга. Это сильный сигнал, что ставка на
Nav3 в шаблоне — не мода, а направление платформы [предположение, но основано на трёх независимых
проектах].

#### 4.4. Что устарело (наблюдаемо в коде, а не по слухам)

| Практика | Где ещё встречается | Комментарий |
|---|---|---|
| View-система / Fragments / viewBinding | только Signal | у остальных 7 — ноль |
| RxJava | только Signal | везде Coroutines + Flow |
| **kapt** | **нигде из восьми** | полностью вытеснен KSP2 |
| Dagger вручную / сервис-локатор | только Signal (`AppDependencies`) | |
| Строковые navigation-роуты | Jetsnack, architecture-samples | остальные — `@Serializable`-типы |
| Accompanist | только `accompanist-permissions` | остальное разъехалось в androidx |
| compileSdk 35 / Kotlin 2.1 / AGP 8.x | architecture-samples, tivi (архив) | у живых compileSdk 36–37, Kotlin 2.3–2.4, AGP 9.3–9.4 |

#### 4.5. Что общее у ВСЕХ живых (это и есть «фундаментальное»)

1. 100 % Kotlin + Compose + Material 3.
2. AGP 9.x + Gradle 9.x, JDK 21 в toolchain/CI, compileSdk 36–37.
3. **`gradle/libs.versions.toml`** — во всех восьми без исключений (Signal — четыре каталога).
4. **Convention-плагины в `includeBuild`** — у всех, кроме двухмодульных учебных.
5. **KSP2 вместо kapt** — везде.
6. Coroutines + Flow как единственная модель асинхронности.
7. UDF: immutable `UiState` + sealed `Event`; различие только в носителе состояния.
8. Type-safe навигация через `@Serializable`-типы.
9. **Отдельные `*-test`-модули с фейками** — самый универсальный паттерн из всех:
   NiA (`core:testing`, `core:data-test`, `core:datastore-test`, `sync:sync-test`),
   element-x (`features/*/test`, `tests/testutils`), Jetcaster (`core:*-testing`),
   bitwarden (`:testharness`), architecture-samples (`:shared-test`).
10. Turbine + собственные lint/статанализ-правила: NiA (`:lint`), Signal (`:lintchecks`, `:fast-lint`),
    element-x (`:tests:detekt-rules` + **17 Konsist-тестов**).
11. Screenshot-тесты в CI с политикой «эталоны пишет CI, не человек»: NiA и element-x (Roborazzi),
    Signal (**официальный AGP `compose.screenshot`**), Circuit (Roborazzi).
12. **`AGENTS.md` / `CLAUDE.md` в корне** — уже у NiA и element-x. Новый де-факто артефакт
    репозитория 2026. Наша обвязка попадает ровно в этот тренд.

#### 4.6. Два продовых ориентира — противоположные стратегии модульности

**`element-hq/element-x-android`** — максимальная дисциплина: 266 build-файлов,
фича = **три** модуля `api`/`impl`/`test`, и это **навязано кодом**, а не соглашением:

```kotlin
fun Project.setupDependencyInjection(generateNodeFactories: Boolean = shouldApplyAppyxCodegen()) {
    if (project.path.endsWith(":api")) {
        error("api module should not use setupDependencyInjection(). Move the implementation to `:impl` module")
    }
    applyPluginIfNeeded(libs.plugins.metro)
    …
}
```
Плюс: `dependency-analysis` в каждом convention-плагине, detekt 1.23.8 + compose-rules 0.4.28 +
**собственный `:tests:detekt-rules`**, ktlint 1.8.0, Kover 0.9.9, Roborazzi 1.74, sonarqube,
**17 Konsist-тестов** (`KonsistArchitectureTest`, `KonsistClassNameTest`, `KonsistComposableTest`,
`KonsistDiTest`, `KonsistFlowTest`, `KonsistImmutableTest`, `KonsistPresenterTest`,
`KonsistPreviewTest`, …) с намеренно «падающими» фикстурами в `failures/` — тестами самих тестов.
SDK-политика — типизированный `Versions.kt` с CalVer и `require(...)`-инвариантами
(`COMPILE_SDK = 37`, `TARGET_SDK = 37`, `minSdk` 24/33 по флейвору, `JAVA_VERSION = 21`).

**`bitwarden/android`** — противоположность: **10 модулей**, фичи разложены пакетами внутри `:app`,
и при этом самый свежий стек из всех: **AGP 9.4.0, Kotlin 2.4.20, KSP 2.3.11, compileSdk/targetSdk 37,
minSdk 29, jvmTarget 21, Hilt 2.60.1, JUnit 6.1.3 (Jupiter + Vintage), MockK 1.14.11,
Robolectric 4.16.1, Turbine 1.2.1, detekt 1.23.8, Kover 0.9.9**. Плюс **Gradle dependency locking
`LockMode.STRICT`** (`gradle.lockfile`, `buildscript-gradle.lockfile`, `settings-gradle.lockfile`) —
это строже, чем Dependency Guard у NiA, и прямой аналог `go.sum` / `pnpm-lock.yaml`.
Screenshot-тестов и GMD у них нет вовсе.

**Вывод для нашего шаблона [предположение]**: «много модулей» и «мало модулей» — обе рабочие
стратегии в проде. Для приложения-компаньона размером с CRM правильный старт — **между ними**:
один модуль на фичу (без `api`/`impl`-раскола), 6–8 `core`-модулей, и документированный путь
«когда дробить». Дисциплину брать у element-x (проверки в convention-плагине, Konsist-тесты),
свежесть стека — у bitwarden, инструменты — у NiA.


### §5. Платформа, SDK-уровни и требования Google Play

#### 5.1. Версии платформы

| Android | API | `VERSION_CODES(_FULL)` | Статус на 2026-09-17 | Дата |
|---|---|---|---|---|
| Android 17 QPR2 | 37.2 | `CINNAMON_BUN_2` | константа уже в SDK; релиз ожидается Q4 2026 | [предположение по дате] |
| Android 17 QPR1 | 37.1 | `CINNAMON_BUN_1` | **Beta** | — |
| **Android 17** | **37** | `CINNAMON_BUN` | **stable** | **2026-06-16** |
| Android 16 QPR2 | 36.1 | `BAKLAVA_1` | stable — **первый minor SDK release в истории Android** | 2025-12-02 |
| Android 16 | 36 | `BAKLAVA` | stable | 2025-06-10 |
| Android 15 | 35 | `VANILLA_ICE_CREAM` | stable | 2024 |

[проверено — `developer.android.com/about/versions`, `…/reference/android/os/Build.VERSION_CODES_FULL`,
`android-developers.googleblog.com/2026/06/Android-17.html`, `apilevels.com`, 2026-09-17]

**Новая механика — дробные (minor) SDK-релизы.** С 36.1 у платформы появились дробные API-уровни:

```kotlin
Build.VERSION.SDK_INT        // мажор: на Android 17 QPR1 == 37
Build.VERSION.SDK_INT_FULL   // полный: 3700001 == VERSION_CODES_FULL.CINNAMON_BUN_1
```

`compileSdk`/`targetSdk` в Gradle по-прежнему целые. Сравнивать `SDK_INT_FULL` можно **только** с
именованными константами — кодирование целого может измениться.

**Значимое в Android 16 (target 36)** [проверено, `…/about/versions/16/behavior-changes-16`]:

- **Edge-to-edge: opt-out исчез.** «For apps targeting Android 16 (API level 36),
  `R.attr#windowOptOutEdgeToEdgeEnforcement` is deprecated and disabled, and your app can't opt-out
  of going edge-to-edge.»
- **Predictive back включён по умолчанию**, и — важно — «`onBackPressed` is not called and
  `KeyEvent.KEYCODE_BACK` is not dispatched anymore». Временный opt-out —
  `android:enableOnBackInvokedCallback="false"`.
- **Большие экраны: ограничения ресайза/ориентации перестали работать.** «For apps targeting
  Android 16 (API level 36), orientation, resizability, and aspect ratio restrictions no longer
  apply on displays with smallest width >= 600dp.» Игнорируются `android:screenOrientation`,
  `android:resizeableActivity="false"`, `android:minAspectRatio`/`maxAspectRatio`,
  `setRequestedOrientation()`. Временный opt-out — `android.window.PROPERTY_COMPAT_ALLOW_RESTRICTED_RESIZABILITY`,
  и дословно: «The opt-out is temporary and won't apply when targeting API level 37».

**Значимое в Android 17 (target 37)** [проверено, `…/about/versions/17/behavior-changes-17`,
`…/changes/ff-restrictions-ignored`]:

- **Adaptive: opt-out удалён окончательно.** «Android 17 removes the temporary developer opt-out
  for orientation and resizability restrictions on large screen devices that was provided in
  Android 16.» → **adaptive-вёрстка перестала быть опцией**; плюс больше пересозданий Activity
  при повороте («losing user state if not properly preserved») → `SavedStateHandle`/`rememberSaveable`
  обязательны.
- **`ACCESS_LOCAL_NETWORK` обязателен** при target 37 (в 16 был opt-in). Для облачного бэкенда
  не задевает, для LAN/принтеров/касс — да.
- **ECH (Encrypted Client Hello)** включён для target 37; новый элемент Network Security Config
  `<domainEncryption>`.
- **`static final` поля неизменяемы**: рефлексия → `IllegalAccessException`, JNI → краш. Ломает
  часть mock/тест-инструментов и хаков старых SDK.
- **OTP из обычных SMS задержаны на 3 часа** → авторизация по SMS-коду обязана перейти на
  **SMS Retriever / SMS User Consent API**.
- Lock-free `MessageQueue` (ломает библиотеки, рефлексирующие приватные поля); memory limits
  приложения по объёму RAM устройства; лимит памяти виджетов.
- **Для всех приложений, независимо от target**: `usesCleartextTraffic` планируется deprecate;
  **implicit URI grants исчезнут в Android 18** — для `ACTION_SEND`/`ACTION_SEND_MULTIPLE`/
  `ACTION_IMAGE_CAPTURE` система перестанет выдавать URI-permission автоматически. Это прямо про
  **камеру и файлы** в нашем профиле; детектится
  `StrictMode.VmPolicy.Builder().detectImplicitUriPermissionGrant()`, чинится явным
  `intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)`.
- **Стратегическое**: из блога релиза Android 17 — «All new Android APIs and libraries are now
  **Compose-exclusive**» и «Legacy View components … are now in **maintenance mode**. They will
  receive only critical bug fixes, and no new features.» То есть View-система как основа шаблона
  исключена не «по моде», а по политике платформы.

#### 5.2. Требования Google Play к targetSdk — дедлайн УЖЕ наступил

**[проверено, `developer.android.com/google/play/requirements/target-sdk`, 2026-09-17]**

| Что | Требование | С какой даты |
|---|---|---|
| Новые приложения и обновления (телефоны/планшеты/складные) | **targetSdk ≥ 36** (Android 16) | **31 августа 2026** |
| Новые приложения и обновления (Wear OS, Automotive) | targetSdk ≥ 35 | 31 августа 2026 |
| Новые приложения и обновления (TV, XR) | targetSdk ≥ 34 | 31 августа 2026 |
| Существующие приложения — чтобы оставаться доступными новым пользователям на новых ОС | targetSdk ≥ 35 | — |
| Продление | до **1 ноября 2026** по заявке в Play Console | — |

То есть **на дату ресёрча targetSdk 36 — это уже не «на будущее», а обязательный минимум**.
По исторической каденции (Play поднимает планку ежегодно, в конце августа) ожидаемо, что
к 31.08.2027 потребуется targetSdk 37 [предположение — Google на этой странице 2027 год пока
не публикует].

**Вывод для шаблона [предположение]**: `compileSdk = 37`, `targetSdk = 37`. Аргументы:
(1) потолок AGP 9.4 — ровно 37, значит компилировать под 37 можно уже сейчас;
(2) `targetSdk 36` придётся поднять в течение года, и лучше сразу принять адаптивные
behavior changes, чем ловить их в чужом проекте через год;
(3) так делают `bitwarden/android` (compile 37 / target 37) и `element-hq/element-x-android`
(`COMPILE_SDK = 37`, `TARGET_SDK = 37`) — то есть это уже продовая практика, а не край.
Консервативная альтернатива — `compileSdk 37 / targetSdk 36` (как у Signal: compile 37, target 36).

#### 5.3. minSdk

Данные о долях [проверено, `apilevels.com` — Statcounter GlobalStats, данные за апрель 2026,
страница обновлена 2026-05-28]: cumulative usage — API 34: 54.5 %, API 35: 41.0 %, API 36: 22.3 %,
API 37: 0 %. Порог **minSdk 29 → 91.1 %**, **minSdk 23 → 98.0 %**. Jetpack/AndroidX требуют
minSdk ≥ 23 с июня 2025.

Что ставят реальные проекты [проверено, чтение их `libs.versions.toml`/convention-плагинов]:

| Проект | minSdk |
|---|---|
| Now in Android | 23 (`:benchmarks` — 28) |
| `signalapp/Signal-Android` | 23 |
| `element-hq/element-x-android` | 24 (FOSS) / 33 (enterprise-сборка) |
| `bitwarden/android` | **29** |
| `android/architecture-samples` | 21 (устарел) |

**Рекомендация для шаблона [предположение]: `minSdk = 26` (Android 8.0).** Обоснование:

- minSdk 26 снимает основную часть боли API-совместимости: notification channels (обязательны с 26),
  adaptive icons, `java.time` без desugaring в большинстве случаев (полноценно — с 26 через
  desugaring, с 26+ многое доступно нативно), нет ограничений на `WebView`/`AppComponentFactory`.
- Наш профиль — **бизнес-приложения для сотрудников/клиентов компании**, а не масс-маркет:
  «последние 1–2 % на Android 5–7» не являются коммерчески значимыми, зато каждый год поддержки
  древних API стоит времени.
- 26 — удобный компромисс между «23 как в NiA» (максимум охвата, но тянет desugaring и старые
  бранчи) и «29 как в bitwarden» (проще код, меньше охват).
- Значение должно быть **параметром шаблона в `libs.versions.toml`**, который `/de-template`
  спрашивает у владельца проекта, а не хардкодом.

**Открытый вопрос владельцу**: РФ-рынок имеет более длинный хвост старых устройств, чем
глобальный; долей по РФ на apilevels.com нет. Это нужно уточнить по данным RuStore/AppMetrica
(поток 05).

#### 5.4. Обязательное «платформенное поведение» для нового приложения

| Что | Статус | API / артефакт |
|---|---|---|
| **Edge-to-edge** | обязательно де-факто с Android 15/16 для target ≥ 35 | `enableEdgeToEdge()` из `androidx.activity` 1.13.0; инсеты через `WindowInsets` |
| **Adaptive layout / не блокировать ресайз** | при target 37 ограничения ориентации/ресайза **игнорируются** | `androidx.window` 1.5.1, `material3-adaptive` 1.3.0, `currentWindowAdaptiveInfo()` |
| **Predictive back** | обязательно к поддержке в современных приложениях | `android:enableOnBackInvokedCallback="true"`, `androidx.navigationevent` 1.1.2, `NavDisplay` умеет из коробки |
| **`POST_NOTIFICATIONS`** | обязательно с API 33 | рантайм-разрешение; в тестах — `GrantPostNotificationsPermissionRule` (приём NiA) |
| **Photo picker** | замена `READ_EXTERNAL_STORAGE` | `ActivityResultContracts.PickVisualMedia` |
| **16 KB page size** | требование Play | актуально для нативных `.so`; **Kotlin-only приложение без NDK-зависимостей его проходит автоматически** [предположение — проверить по списку транзитивных `.so`, см. §«Ловушки»] |
| **Credential Manager / passkeys** | опционально | `androidx.credentials` 1.6.0; наш бэкенд — cookie-auth, passkeys вне scope (поток 07) |
| **Baseline Profiles** | сильно рекомендовано | `androidx.baselineprofile` плагин + `androidx.profileinstaller` 1.4.1; **требует устройства/эмулятора для генерации** |
| **Per-app language (ru/en)** | нужно нам | `androidResources { generateLocaleConfig = true }` (см. §3.5) |
| **App Links** | нужно нам (deep links) | `android:autoVerify="true"` + `.well-known/assetlinks.json` на домене (инфра — поток 07) |

#### 5.5. Compose-производительность: что должен знать агент

**Strong skipping включён по умолчанию с Kotlin 2.0.20** [проверено,
`developer.android.com/develop/ui/compose/performance/stability/strongskipping`]. Следствия:

- все restartable-композаблы становятся skippable независимо от стабильности параметров;
- лямбды с нестабильными захватами **автоматически** оборачиваются в `remember(...)`;
- нестабильные ключи сравниваются по `===`, стабильные — по `equals()`;
- отказ — `@NonSkippableComposable` / `@DontMemoize`;
- стоимость по размеру — ~4 КБ на приложении масштаба NiA.

Это значит, что **ручные `remember { { … } }` вокруг лямбд в 2026 — карго-культ**, ровно как
ручные `useMemo/useCallback` в React с включённым Compiler'ом (прямая параллель с правилом
`spa-frontend-template`: «код пишется без ручных `useMemo`/`useCallback`/`memo`»).

Стабильность [проверено, `…/performance/stability`]: `List`/`Set`/`Map` Compose **всегда**
считает нестабильными. Лечится `kotlinx-collections-immutable` (`ImmutableList`, 0.5.2) либо
`@Immutable`/`@Stable` на своих типах. Для UiState шаблона правило простое: **`UiState` — `data class`
из `val`, коллекции — `ImmutableList`/`PersistentList`, никаких `var`**.

Диагностика — Compose compiler reports/metrics; конфигурация стабильности сторонних типов —
файл `compose_compiler_config.conf` (так делает NiA, подключая его через
`composeCompiler { stabilityConfigurationFiles.add(...) }`).


### §6. Официальный агентный тулинг Android (появился в 2026 — прямо релевантно нашей обвязке)

Это самая свежая и самая недооценённая часть канона: за 2026 год Google выпустил **официальный
CLI и стандартизованные «скиллы» для AI-агентов**. Для шаблона, вся ценность которого — в агентной
обвязке, это меняет расклад.

#### 6.1. `android` CLI

**[проверено, `developer.android.com/tools/agents/android-cli` — страница обновлена 2026-09-11;
`…/android-cli/download`]**

Один бинарник, позиционируемый как «entry point to official tools, skills, and knowledge»
для агентов, CI и терминальной разработки. Установка на Linux:

```bash
curl -fsSL https://dl.google.com/android/cli/latest/linux_x86_64/install.sh | bash        # в $HOME
curl -fsSL https://dl.google.com/android/cli/latest/linux_x86_64/install_root.sh | bash   # глобально
# либо apt:
curl -fsSL https://dl.google.com/linux/linux_signing_key.pub | sudo tee /etc/apt/keyrings/google.asc >/dev/null
sudo sh -c 'echo "deb [arch=amd64 signed-by=/etc/apt/keyrings/google.asc] http://dl.google.com/android/cli/latest/debian/ stable main" > /etc/apt/sources.list.d/android-cli.list'
sudo apt-get update && sudo apt-get install android-cli
```

Полный набор команд (дословно со страницы):

| Команда | Синтаксис | Зачем нам |
|---|---|---|
| `create` | `android create [--dry-run] [--verbose] [--name=<name>] [--output=<path>] [<template>]` | **скаффолд проекта из официального шаблона**, напр. `empty-activity-agp-9` |
| `create list` | `android create list` | список доступных шаблонов |
| `describe` | `android describe [--project_dir=<dir>]` | разбор структуры проекта и артефактов сборки — готовый `make status` для агента |
| `docs search` / `docs fetch` | `android docs search '<query>'` / `android docs fetch kb://…` | поиск по Android Knowledge Base **из CLI** — агенту не нужно гадать по памяти |
| `sdk install/list/remove/update` | `android sdk install platforms/android-37 build-tools/37.0.0` | headless-установка SDK (важно для потока 06) |
| `emulator create/list/start/stop` | `android emulator create --profile=medium_phone` | AVD из терминала (на Windows отключено) |
| `run` | `android run --apks=app/build/outputs/apk/debug/app-debug.apk [--device=…]` | установка и запуск |
| `screen capture` / `screen resolve` | `android screen capture --output=ui.png --annotate` | скриншот + перевод «визуальных подписей» в координаты |
| `layout` | `android layout [--pretty] [--output] [--diff]` | иерархия UI в JSON, в т. ч. **diff** — это машинно-читаемый аналог accessibility-дерева |
| `skills add/find/list/remove` | `android skills add --all` | установка официальных Android-скиллов для агента |
| `init` | `android init` | подготовка окружения для агентов (ставит скилл `android-cli`) |
| `studio …` | `android studio render-compose-preview … <file> <composable>`, `analyze-file`, `find-usages`, `find-declaration`, `version-lookup` | требует **запущенной** Android Studio (Quail 2 Canary 1+) и включённого Gemini — для headless-Linux не подходит |

Сбор метрик отключается флагом `--no-metrics` или строкой в `~/.androidrc`. Известные ограничения:
на Windows не поддержана загрузка из PowerShell и отключён `android emulator`.

#### 6.2. Android Skills

**[проверено, `github.com/android/skills` (Apache-2.0);
`android-developers.googleblog.com/2026/09/leverage-gemma-4-android-studio-quail.html`]**

- «Android skills are a dedicated repository of AI-optimized, modular instructions» — по сути
  ровно то же, что наши `.claude/skills/` и `.claude/rules/`, но от вендора платформы.
- Android Studio **Quail 4 (2026.1.4)** идёт с **23 предустановленными скиллами**; они следуют
  открытому стандарту **agentskills.io**.
- Вне IDE ставятся через `android skills add --all` / `android skills add <name>`, с таргетингом
  на конкретного агента (`--agent=CLAUDE` в числе поддерживаемых) и на конкретный проект.
- Состав (по каталогам репозитория): `build-system/agp/agp-9-upgrade`, `devtools/android-cli`,
  `ui/compose`, `ui/compose-tv-migration`, `ui/wear-compose-m3`, `navigation-3`, `camera`,
  `device-ai`, `identity`, `media3`, `play`, `performance/r8-analyzer`, `system/edge-to-edge`,
  `security` (intent security), `testing`, `xr`.

**Что это значит для нашего шаблона [предположение]:**

1. Это **не конкурент** нашей обвязке, а **дополняющий слой**: их скиллы — про платформу
   («как мигрировать на AGP 9», «как сделать edge-to-edge»), наши — про процесс и проект
   (`/plan`, `/smartdev`, `/de-template`, `/scaffold`, `/release`).
2. Их стоит **вендорить или ставить процедурой `make setup`**, а в `CLAUDE.md` шаблона явно
   написать «платформенные how-to — в официальных Android skills, не изобретай». Это снимает
   с нас обязанность поддерживать актуальность платформенных знаний, которые протухают за месяцы.
3. `android docs search` + `android docs fetch` — способ дать агенту **свежую** документацию
   вместо памяти. Для нашего кейса (knowledge cutoff отстаёт от платформы на месяцы) это
   критично, и это же снижает риск «агент пишет `kotlinOptions{}` из AGP 8».
4. `android create list` + `android create <template>` — потенциальная альтернатива части
   `/scaffold`; как минимум их шаблон `empty-activity-agp-9` стоит взять за отправную точку
   каркаса шаблона, чтобы не изобретать `build.gradle.kts` вручную.
5. `android describe`, `android layout --diff`, `android screen capture` — готовые кирпичи для
   агентного цикла «сделал → посмотрел → починил», аналог нашего Playwright/`read_page` во фронте.

**Риск [предположение]**: CLI собирает телеметрию по умолчанию (команды, флаги, анонимные
стектрейсы); для наших проектов надо класть `--no-metrics` в `~/.androidrc` или в `make setup`
и зафиксировать это правилом.

#### 6.3. Android Studio Quail 4 — что оттуда важно знать, не запуская IDE

**[проверено, блог Android Developers, сентябрь 2026]**: Gemma 4 встроена локально (минимум
12 ГБ RAM, рекомендуется 32+; полностью офлайн, без квот), MCP-маркетплейс появился в Quail 3
(`Settings > Tools > AI > MCP Servers`), Agent Mode умеет параллельные чаты и мультифайловый
рефакторинг. Для нашей headless-Linux-машины это неприменимо напрямую, но объясняет, почему
`android studio …` подкоманды CLI требуют работающей IDE — они ходят в неё, а не в компилятор.


### §7. Тесты: канон 2026 и что реально работает headless на Linux

Это самый важный раздел для нашего сценария: агент на Debian **без устройства и без эмулятора**
должен иметь полноценный feedback-loop.

#### 7.1. Что где исполняется

| Инструмент | Версия | Headless Debian без устройства | Примечание |
|---|---|---|---|
| JUnit 4 | 4.13.2 | ✅ | базис; runner `androidx.test.ext.junit.runners.AndroidJUnit4` |
| `kotlinx-coroutines-test` | 1.11.0 | ✅ | |
| Turbine | 1.2.1 | ✅ | API стабилен, релизов не было с 2025-06 — это норма |
| MockK | 1.14.11 | ✅ | `mockk-jvm`; `mockk-android` — только androidTest |
| **Robolectric** | **4.17** (2026-09-10) | ✅ | поддерживает **API 23 → 37** |
| Compose UI-тесты (`createComposeRule`) под Robolectric | BOM 2026.09.00 | ✅ | тот же код, что на устройстве |
| **Roborazzi** | **1.74.0** (2026-09-08) | ✅ | screenshot-гейт |
| Paparazzi | 1.3.5 stable / 2.0.0-alpha05 | ✅ | stable не обновлялся с 2024-11-07 |
| `com.android.compose.screenshot` | 0.0.1-alpha16 | ✅ | **deprecated, см. 7.5** |
| Kover | 0.9.9 | ✅ | покрытие только JVM-тестов |
| `mockwebserver3` | 5.5.0 | ✅ | |
| Ktor `MockEngine` | 3.6.0 | ✅ | |
| Hilt-тесты (`@HiltAndroidTest` + Robolectric) | 2.60.1 | ✅ | |
| Espresso | 3.7.0 | ❌ | устройство/эмулятор |
| UIAutomator | 2.4.0 | ❌ | устройство/эмулятор |
| Gradle Managed Devices | AGP 9.4 | ⚠️ технически да, практически нет | без `/dev/kvm` эмулятор «работает, но медленнее» (официальная формулировка) |
| Macrobenchmark / Baseline Profiles | 1.5.0 | ❌ | «rooted physical device, emulator, or Gradle Managed Device» |
| Maestro (e2e) | 2.10.0 | ❌ | «Maestro requires a running target device» |
| `org.gradle.test-retry` | 1.6.6 (2026-09-07) | ✅ | CC-compatible |

[всё проверено 2026-09-17 по официальным докам / Maven / releases.atom]

#### 7.2. ⚠️ Compose Test API получил **v2** — старые точки входа deprecated

**[проверено]** API-референс `androidx.compose.ui.test` и страница миграции
`developer.android.com/develop/ui/compose/testing/migrate-v2`:

```
runComposeUiTest(...)        → This function is deprecated. Use androidx.compose.ui.test.v2.runComposeUiTest instead.
runAndroidComposeUiTest(...) → deprecated → androidx.compose.ui.test.v2.runAndroidComposeUiTest
runEmptyComposeUiTest(...)   → deprecated → androidx.compose.ui.test.v2.runEmptyComposeUiTest
```

Новые пакеты: **`androidx.compose.ui.test.v2`** и **`androidx.compose.ui.test.junit4.v2`**
(там живут `createComposeRule()`, `createAndroidComposeRule<A>()`, `createEmptyComposeRule()`,
`ComposeUiTestConfig`). v2 доступны с `ui-test-junit4`/`ui-test` **1.11.0-alpha03+**,
`ComposeUiTestConfig` — с 1.13.0-alpha01+.

**Поведенческое отличие — ловушка №1 для агента:**

| | v1 | v2 |
|---|---|---|
| диспетчер композиции | `UnconfinedTestDispatcher` (корутины бегут сразу) | **`StandardTestDispatcher`** (очередь, как в `runTest`) |
| default input mode | как получится | **`InputMode.Touch`** |

Официальный пример из доки:

```kotlin
// v1: This works because loadData() executes immediately
viewModel.loadData()
assertEquals(Success, viewModel.state.value)

// v2: This FAILS because loadData() is queued but hasn't run yet
viewModel.loadData()
assertEquals(Success, viewModel.state.value) // Still "Loading"!
```

Лечится `waitForIdle()`, `runOnIdle { }` или `mainClock.scheduler.runCurrent()`.

**Для шаблона [предположение]**: сразу писать на **v2**-импортах и зафиксировать это правилом —
иначе через год придётся мигрировать чужой проект. И явно записать в `CLAUDE.md`, что
`androidx.compose.ui.test.junit4.createComposeRule` (без `.v2`) — ошибка.

#### 7.3. Robolectric — три обязательные настройки, без которых не заведётся

**[проверено, `robolectric.org/compatibility_table/`, `robolectric.org/getting-started/`]**

| Robolectric | API levels | Минимальная Java |
|---|---|---|
| **4.17** | **23 → 37** | Android 16/17 (API 36/37) требуют **Java 21**; API 35/34 — 17 |
| 4.16 | 23 → 36 | Java 21 для API 36 |
| 4.15 | 21 → 35 | — |

1. **`isIncludeAndroidResources = true`** — обязателен, иначе нет merged-ресурсов/манифеста.
2. **`--add-opens` на JDK 17+** — дословная цитата доки: «When running tests on Java 17 or higher,
   the JVM requires `--add-opens` flags so that Robolectric can access internal OpenJDK classes».
   Без них — `InaccessibleObjectException` на ровном месте. **Это главный источник «у меня не
   собирается» при первом заходе на Robolectric в 2026.**
3. **Пин SDK.** Без `@Config(sdk = …)` Robolectric берёт `targetSdk` → при `targetSdk 37`
   потребует JDK 21. Наш выбор — **toolchain 21** + пин `sdk=34` в `robolectric.properties`
   для скриншотов (детерминизм), либо честный `sdk=37`.

Offline-режим (важно для CI без сети и для нашей машины): Robolectric в рантайме тянет
`android-all-instrumented-*.jar` из Maven. Свойства (дословно из таблицы доки):
`robolectric.offline`, `robolectric.dependency.dir`, `robolectric.dependency.repo.url`,
`robolectric.dependency.repo.id`, `…username`/`…password`, `…proxy.host`/`…proxy.port`,
`robolectric.enabledSdks`.

Ключевая цитата, объясняющая, **почему Robolectric — не «второй код-путь»**
[проверено, `robolectric.org/blog/2026/07/03/designing-local-testing-libraries/`]:
«the same `@RunWith(AndroidJUnit4::class)` test runs on Robolectric locally and on a real device
when instrumented… Robolectric is best thought of as a **deployment target for your tests**, not a
separate code path.»

Что **не работает** на Robolectric: accessibility-чеки Compose (`enableAccessibilityChecks` —
no-op, проверяет `Build.FINGERPRINT == "robolectric"`); нет draw-pass; `Thread.sleep`
десинхронизирован с `MainTestClock`; бесконечные анимации при `autoAdvance = true` бросают
`CancellationException`; нельзя смешивать `runComposeUiTest { }` и `ComposeTestRule` в одном тесте.

#### 7.4. JUnit 4 vs JUnit 5 — вывод однозначный

**AGP 9 не поддерживает JUnit 5 нативно** [проверено, DSL-референс
`tools/gradle-api/9.4/…/UnitTestOptions` — там ровно три члена: `all(...)`,
`isIncludeAndroidResources`, `isReturnDefaultValues`; никакого `useJUnitPlatform()`].

Плагин `de.mannodermaus.android-junit` 2.0.1 (репозиторий переименован
`android-junit5` → `android-junit-framework`) в README заявляет совместимость только
«AGP ≥ 8.2.0»; **AGP 9 явно не упомянут**, issue #404 «Usage doc for AGP 9…» открыт с 2026-01-13
без ответа мейнтейнера. Robolectric JUnit5-раннера не имеет (issue #10797 открыт с 2025-11-16),
есть только сторонний `apter-tech/junit5-robolectric-extension` с оговоркой «API might change».

**Now in Android — JUnit 4.13.2**, без MockK, Mockito, Kotest, Kover, Paparazzi и официального
screenshot-плагина. `bitwarden/android` — единственный из изученных, кто взял JUnit 6 (BOM 6.1.3,
Jupiter + Vintage одновременно), и у них нет Robolectric-Compose-скриншотов.

**Решение для шаблона [предположение]: JUnit 4.13.2 + `AndroidJUnit4`.** Причина не в
консерватизме: `AndroidJUnit4` — единственный runner, который один и тот же тест гоняет
локально через Robolectric и на устройстве через инструментацию. Kotest 6.2.5 — нишевый на
Android, не брать.

#### 7.5. Screenshot-тесты: официальный плагин **deprecated, не дойдя до беты**

**[проверено, `developer.android.com/studio/preview/compose-screenshot-testing`, страница
обновлена 2026-09-14]** — два баннера на странице, дословно:

> **Deprecated:** Starting with Android Gradle Plugin (AGP) 9.5.0-alpha03 and Compose Preview
> Screenshot Testing 0.0.1-alpha16, we recommend configuring screenshot tests using AGP test suites.
> The standalone plugin method described on this page is deprecated.

> **Experimental:** Compose Preview Screenshot Testing is still in development.

Google Maven подтверждает: `com.android.compose.screenshot.gradle.plugin` — версии
`0.0.1-alpha01 … 0.0.1-alpha16` и всё. **16 альф за два с лишним года, беты нет.** Преемник
(AGP test suites, `testOptions { screenshotTests.create(...) }`) требует **AGP 9.5.0-alpha03**,
то есть preview-канал, и даёт таски вида `testScreenshotTestDefaultDemoDebugTestSuite`.

Сравнение трёх путей:

| Критерий | `com.android.compose.screenshot` | **Roborazzi 1.74.0** | Paparazzi |
|---|---|---|---|
| Стабильность | 0.0.1-**alpha16**, **deprecated** | **stable**, ~2 релиза/мес | stable = 1.3.5 от 2024-11-07 |
| AGP 9 | преемник требует AGP 9.5-**alpha** | работает | «pre-AGP 9.0 consumers» (alpha05), AGP 9 не заявлен |
| Robolectric-контекст (Hilt, Context, клики) | нет | **да** | нет (несовместим с Robolectric) |
| Что снимаем | только `@Preview` + `@PreviewTest` | любой Compose-node / View / Preview | View / Compose |
| Используется в NiA | нет | **да** | нет |
| Движок рендера | layoutlib | Robolectric Native Graphics (Skia) | layoutlib |

**Рекомендация: Roborazzi 1.74.0** [предположение, но с сильным обоснованием]. Он один закрывает
и «сними превью» (`generateComposePreviewRobolectricTests`), и «сними живой экран после трёх
кликов», и живёт в том же Robolectric-окружении, где уже крутятся unit- и Compose-тесты.
Signal при этом использует официальный плагин — то есть путь рабочий, но у него deprecated-статус.

Таски Roborazzi: `recordRoborazziDebug` (записать эталоны), `compareRoborazziDebug`,
**`verifyRoborazziDebug`** (падает при отличии — это и есть гейт), `verifyAndRecordRoborazziDebug`.

Три обязательные настройки Roborazzi [проверено, README]:

- `@GraphicsMode(GraphicsMode.Mode.NATIVE)` на классе + Robolectric ≥ 4.10;
- `it.systemProperties["robolectric.pixelCopyRenderMode"] = "hardware"` — рекомендация автора
  для точности (issue #296);
- **`separateOutputDirs.set(true)`** — иначе на **Gradle 9** при нескольких Roborazzi-тасках в
  одном прогоне (`check`) hard-fail `Cannot access input property 'roborazziImageInput'` (issue #830).

Бонус для нашей обвязки: Roborazzi **поставляет свою документацию как agent skill** —
`npx skills add takahirom/roborazzi` (Claude Code в списке поддерживаемых). [проверено, README]

**Борьба с флаки-скриншотами** — порядок мер (сначала детерминизм, threshold последним):
1. `kotlin { jvmToolchain(21) }` — фиксированный JDK, а не «системная java»;
2. `@Config(sdk = [34])` или `sdk=34` в `robolectric.properties` — иначе апгрейд `targetSdk`
   молча переписывает все эталоны;
3. `@Config(qualifiers = RobolectricDeviceQualifiers.Pixel5)` — фиксация плотности/размера;
4. запись эталонов **в том же Docker-образе**, что и проверка (свой образ: base + JDK + шрифты);
5. только потом — `changeThreshold = 0.01` + `SimpleImageComparator(maxDistance = 0.007F, vShift = 2, hShift = 2)`;
6. `mainClock.autoAdvance = false` для анимаций; никаких `Thread.sleep`;
7. Git LFS на PNG.

Эталоны **не переносимы между инструментами** (layoutlib vs Robolectric Native Graphics) —
смена инструмента = полная перезапись baseline.

#### 7.6. Fakes vs mocks — официальная позиция

**[проверено, `developer.android.com/training/testing/fundamentals/test-doubles`]**, дословно:

> «Fakes don't require a mocking framework and are lightweight. **They are preferred.**»
> «Fakes are preferred over stubs for simplicity.»
> Про spy: «They are usually avoided for adding complexity. Fakes or mocks are therefore preferred over spies.»

Страница **не называет** ни Mockito, ни MockK — она рекомендует по умолчанию **не использовать
мок-фреймворк**. Now in Android этому следует буквально: `core/testing` содержит только
`kotlinx.coroutines.test`, `androidx.test.rules`, `hilt.android.testing`, `kotlinx.datetime` —
**ни одного мок-фреймворка**.

Порядок выбора для шаблона [предположение]:
1. **Fake** — рукописный in-memory класс в `core:testing` (или `testFixtures`). Дефолт.
2. **MockK 1.14.11** — только там, где нужна mock-семантика («вызван ровно раз»), финальные
   классы чужих SDK, `object`/extension/`suspend`-функции (`coEvery`/`coVerify`).
3. **Mockito** — не заводить в новом Kotlin-проекте.

Naming: официальная рекомендация — префикс `Fake` (`FakeAuthorsRepository`).

#### 7.7. Корутины и Flow

`MainDispatcherRule` из официальной доки и из NiA (`core/testing/.../MainDispatcherRule.kt`) —
дословно один и тот же класс:

```kotlin
class MainDispatcherRule(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(testDispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}
```

Обрати внимание на противоречие, которое надо знать: **дока рекомендует `StandardTestDispatcher`
как более похожий на прод, но собственный `MainDispatcherRule` Google дефолтит
`UnconfinedTestDispatcher`** [проверено, `developer.android.com/kotlin/coroutines/test` + код NiA].
Правило «один `TestCoroutineScheduler` на тест» обязательно.

Канонический тест ViewModel (черновик):

```kotlin
@RunWith(AndroidJUnit4::class)
class TodosViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()
    private val repository = FakeTodosRepository()

    @Test fun uiState_whenLoaded_emitsSuccess() = runTest {
        val viewModel = TodosViewModel(repository)
        viewModel.uiState.test {                       // Turbine
            assertEquals(TodosUiState.Loading, awaitItem())
            repository.emit(listOf(todo("a")))
            val loaded = awaitItem()
            assertIs<TodosUiState.Success>(loaded)
            assertEquals(1, loaded.items.size)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

**Ловушка** [проверено]: если `uiState` построен через
`stateIn(scope, SharingStarted.WhileSubscribed(5_000), …)`, то без активного подписчика upstream
не запускается. Turbine-блок `test {}` подписчика создаёт — поэтому работает; а чтение
`viewModel.uiState.value` напрямую вернёт только initial value. Это контракт `WhileSubscribed`,
не баг.

Turbine API: `test { awaitItem() / awaitComplete() / awaitError() / cancel() /
ensureAllEventsConsumed() }`, несколько потоков — `turbineScope { flow.testIn(backgroundScope) }`.
Непотреблённые события **валят тест** (`AssertionError: Unconsumed events found`).

#### 7.8. Hilt в тестах

**[проверено, `developer.android.com/training/dependency-injection/hilt-testing`]** — важная
деталь, на которой спотыкаются: для Robolectric-тестов процессор подключается как **`kspTest`**,
а не `kspAndroidTest`:

```kotlin
// Robolectric (JVM)
testImplementation("com.google.dagger:hilt-android-testing:2.60.1")
kspTest("com.google.dagger:hilt-android-compiler:2.60.1")
// instrumented
androidTestImplementation("com.google.dagger:hilt-android-testing:2.60.1")
kspAndroidTest("com.google.dagger:hilt-android-compiler:2.60.1")
```

Hilt **работает на Robolectric**: либо `application = dagger.hilt.android.testing.HiltTestApplication`
в `robolectric.properties`, либо `@Config(application = HiltTestApplication::class)`.

Порядок правил обязателен (`HiltAndroidRule` — `order = 0`):

```kotlin
@HiltAndroidTest
@Config(application = HiltTestApplication::class)
class SettingsScreenTest {
    @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
    @get:Rule(order = 1) val composeRule = createAndroidComposeRule<HiltTestActivity>()
}
```

Подмена биндингов: **`@TestInstallIn(components = […], replaces = […])`** — рекомендуемый способ
(глобально, один Hilt-компонент); `@UninstallModules` — точечно, но создаёт отдельный компонент
на каждый тест-класс и замедляет сборку; `@BindValue` / `@BindValueIntoSet` — для одиночной
подмены. NiA реализует это модулями `core:data-test`, `core:datastore-test`, `sync:sync-test`.

Артефакта `ui-test-hilt-manifest` в официальной доке **нет** — это модуль **внутри NiA**
(`@AndroidEntryPoint class HiltComponentActivity : ComponentActivity()` + манифест) как обход
`google/dagger#3394`. Официально нужен `androidx.compose.ui:ui-test-manifest` на
**`debugImplementation`** (есть даже lint-чек `TestManifestGradleConfiguration`, который варнит,
если поставили не туда).

#### 7.9. Coverage: Kover

**[проверено, CHANGELOG `kotlinx-kover`]** — AGP 9 поддержан с 0.9.4 (2025-12-09,
«Added support for Android Gradle Plugin 9.0.0»), исправления в 0.9.5 (build variants, test task
dependencies), 0.9.6 (Hilt), 0.9.7/0.9.8 (Android KMP, project isolation), 0.9.9 (Gradle 9.6
deprecation, 0 % coverage для `com.android.*`). **Брать 0.9.9, не ниже.**
Сайт документации при этом показывает 0.9.8 — отстаёт на релиз.

DSL (дословно из официальной доки):

```kotlin
kover {
    reports {
        verify { rule { minBound(50) } }
    }
}
```

Таски: `koverXmlReport`, `koverHtmlReport`, **`koverVerify`**, `koverBinaryReport`, `koverLog`;
для Android-вариантов имя варианта добавляется в конец — `:app:koverVerifyDebug`.
Агрегация по multi-module — через `dependencies { kover(project(":core:data")) }` в корневом модуле.

**Ограничение, которое надо честно записать в шаблоне** [проверено, дока Kover]: «instrumentation
tests executing on the Android device are not supported yet» — Kover меряет **только JVM-тесты**.
Для нашего headless-гейта это ровно то, что нужно, но нельзя обещать «общее покрытие».

Фильтры (`excludes { classes(...) ; annotatedBy(...) }`) — `annotatedBy`/`inheritedFrom` не
работают с JaCoCo. Kover/JaCoCo подтягиваются в рантайме сборки из `mavenCentral()` — ещё одна
offline-ловушка.

NiA остался на **JaCoCo 0.8.12**; `element-x` и `bitwarden` — на **Kover 0.9.9**. Для
Kotlin-проекта берём Kover.

#### 7.10. Инструментальные тесты и e2e — вне гейта

**Gradle Managed Devices** [проверено, `developer.android.com/studio/test/managed-devices`]:

```kotlin
android {
  testOptions {
    managedDevices {
      localDevices {
        create("pixel2api30") { device = "Pixel 2"; apiLevel = 30; systemImageSource = "aosp" }
      }
      groups { create("ci") { targetDevices.add(devices["pixel2api30"]) } }
    }
  }
}
```

Таски: `./gradlew pixel2api30DebugAndroidTest`, группа — `ciGroupDebugAndroidTest`.
`systemImageSource` ∈ `aosp | google | aosp-atd | google-atd`; **ATD поддерживают только API 30**
и «Screenshot tests depending on hardware rendering are not supported with ATDs».
Для серверов без GPU: `-Pandroid.testoptions.manageddevices.emulator.gpu=swiftshader_indirect`.
Без `/dev/kvm` официальная формулировка — «your virtual machine is slower without the KVM
extensions»; практически это десятки минут на загрузку вместо десятков секунд. **В гейт не ставим.**

**Baseline Profiles** [проверено]: «Run the generator as an instrumented test on a rooted physical
device, emulator, or Gradle Managed Device»; для GMD нужен `systemImageSource = "aosp"` (root);
без root — только Android 13+ (API 33+). Таск `:app:generateBaselineProfile`.
Startup-профиль — `includeInStartupProfile = true` в правиле.

**Maestro 2.10.0** (Apache-2.0, 2026-08-31) [проверено, `docs.maestro.dev`]: требует JDK 17+,
ставится `curl -fsSL "https://get.maestro.mobile.dev" | bash`, и **«Maestro requires a running
target device to execute your tests»**. Есть встроенный MCP-сервер: `maestro mcp`, подключение к
Claude Code — `claude mcp add maestro -- maestro mcp`. ⚠️ Документация на 2026-09-17 всё ещё пишет
«Maestro currently supports API Levels 29, 30, 31, 33, and 34. API 35 and 36 support is arriving in
Q2 2026» — то есть либо текст протух, либо поддержки свежих API до сих пор нет. **Риск, требует
проверки (поток 06).**

#### 7.11. Флаки-политика

`org.gradle.test-retry` **1.6.6** (2026-09-07, первая сторона Gradle, помечен CC-compatible):

```kotlin
test { retry { maxRetries = 2; maxFailures = 20; failOnPassedAfterRetry = true } }
```

`androidx.test.filters.FlakyTest` **сам по себе ничего не ретраит** — это маркер для
`am instrument -e notAnnotation androidx.test.filters.FlakyTest`, и он instrumentation-only.

Политика для шаблона [предположение]:
- **в headless JVM-гейте ретраев нет** — флакующий Robolectric-тест это баг теста;
- ретрай только на instrumentation-stage, `failOnPassedAfterRetry = true` в гейте;
- `testOptions.animationsDisabled = true` — обязательно;
- для screenshot-флаки ретрай бесполезен, работает только фиксация среды.

#### 7.12. Что обязано иметь тест (предложение для правила `tests-pipeline`)

| Слой / артефакт | Обязательный тест | Инструмент |
|---|---|---|
| `core:data` репозиторий | поведение + конфликт источников | JVM + fake DataSource + Turbine |
| `core:network` мапперы DTO→модель | valid/invalid, граничные | JVM, чистый unit |
| `core:network` клиент | коды ответов, ошибки, cookie | `mockwebserver3` |
| `core:domain` use-case | правило целиком | JVM, чистый unit |
| `feature:<x>` ViewModel | все ветки `UiState`, ошибки | `runTest` + `MainDispatcherRule` + Turbine |
| `feature:<x>` Screen (stateless) | рендер каждого состояния | Compose-тест на Robolectric |
| `feature:<x>` Screen | **screenshot** light + dark | Roborazzi |
| `core:designsystem` компонент | screenshot всех состояний | Roborazzi |
| навигация | переходы по back stack | unit-тест над списком `NavKey` (Nav3 это позволяет!) |
| a11y | contentDescription, touch targets | `roborazzi-accessibility-check` (NiA его использует) |


### §8. Качество кода, статический анализ и цепочка гейта

Цель раздела — собрать на Android эквивалент `.golangci.yml` из `go-backend-template`
(forbidigo + depguard) и связки Biome + ESLint + Steiger из `spa-frontend-template`.

#### 8.1. Из чего складывается «архитектурный линтер» на Android

На Android нет одного Steiger — есть **четыре независимых слоя**, и вместе они сильнее:

| Слой | Что ловит | Аналог в наших шаблонах |
|---|---|---|
| **Gradle-граф модулей** | cross-feature импорт — **физически невозможен** (класса нет в classpath) | depguard / Steiger `forbidden-imports`, но жёстче |
| **Android Lint** (+ свой `:lint`-модуль) | запрещённые API, ресурсы, a11y, хардкод строк, свои `Detector`'ы | forbidigo |
| **detekt** (+ compose-rules) | стиль, сложность, `ForbiddenImport`/`ForbiddenMethodCall`, правила Compose | ESLint |
| **Konsist / ArchUnit** (JVM-тесты) | naming, «каждый `*ViewModel` имеет тест», слои внутри модуля | Steiger `inconsistent-naming`, свои проверки |
| **`dependency-analysis`** | unused/undeclared/misused зависимости, `api` vs `implementation` | `pnpm` depcheck / `go mod tidy` |

#### 8.2. Форматирование

| Инструмент | Версия | Опубликовано |
|---|---|---|
| `com.pinterest.ktlint:ktlint-cli` | **1.8.0** | 2025-12-15 |
| Gradle-плагин `org.jlleitschuh.gradle.ktlint` | **14.2.0** | — |
| `com.diffplug.spotless:spotless-plugin-gradle` | **8.10.2** | 2026-09-04 |
| `com.facebook:ktfmt` | 0.64 | — |

[проверено — Maven Central / Gradle Plugin Portal, 2026-09-17]

Что делают образцы: **NiA — Spotless 8.3.0 + ktlint 1.4.0** с `editorConfigOverride(mapOf("android" to "true"))`
и лицензионными заголовками из `spotless/copyright.kt|.kts|.xml`; **element-x — ktlint 1.8.0**;
**Signal — ktlint-gradle 14.2.0**; **bitwarden — Spotless нет вообще** (только detekt с `autoCorrect = true`).

**Рекомендация [предположение]: Spotless + ktlint.** Spotless даёт единый `spotlessApply` на
Kotlin, KTS, XML и лицензионные заголовки — то есть один глагол для агента; ktlint внутри него
даёт официальный Kotlin-стиль с `android=true`. Это прямой аналог `biome format --write` +
`format:check`. `ktfmt` (Facebook) детерминированнее, но расходится с официальным Kotlin style
guide и ломает диффы при миграции — для шаблона, отдаваемого заказчику, лишний спор.

#### 8.3. detekt: брать 1.23.8, а не 2.0

**[проверено, Maven Central 2026-09-17]**

- Старая группа `io.gitlab.arturbosch.detekt` — **1.23.8**, `lastUpdated 2025-02-21`
  (то есть линия заморожена почти два года).
- Новая группа **`dev.detekt`** — только `2.0.0-alpha.0 … 2.0.0-alpha.6`
  (`lastUpdated 2026-08-04`); Gradle-плагин `dev.detekt.gradle.plugin` — те же альфы.
  **Stable 2.0 не вышел.**

Эмпирика (важнее релиз-нот): **`element-hq/element-x-android` (Kotlin 2.4.10, AGP 9.3.2) и
`bitwarden/android` (Kotlin 2.4.20, AGP 9.4.0) держат в проде detekt 1.23.8** — значит связка
работает на актуальном тулчейне.

**Решение: `io.gitlab.arturbosch.detekt` 1.23.8**, с записью в `docs/stack/dependencies.md`
триггера миграции — «когда `dev.detekt` 2.0.0 выйдет stable».

Compose-правила: **`io.nlopez.compose.rules:detekt`** (mrmans0n). ⚠️ Версия привязана к detekt:
на стабильном detekt 1.23.x потолок — **0.4.28**, а 0.5.0+ требует detekt 2.0-alpha (детали — §8.9). Ловит
`ComposableNaming`, `ModifierMissing`, `ModifierReused`, `ModifierWithoutDefault`,
`UnstableCollections`, `ContentEmitterReturningValues`, `ViewModelForwarding`,
`ViewModelInjection`, `MultipleEmitters`, `CompositionLocalAllowlist`, `MutableParams`,
`RememberMissing` и т. п. [предположение в части полного списка правил — ключевые подтверждены
использованием в element-x; точный перечень надо свериться с
`mrmans0n.github.io/compose-rules/rules/` при реализации].

Аналог **forbidigo** делается правилами `ForbiddenImport` / `ForbiddenMethodCall` в `detekt.yml`
(черновик — см. §9.5).

#### 8.4. Android Lint и свой `:lint`-модуль

Конфигурация в convention-плагине (как у NiA, дословно):

```kotlin
private fun Lint.configure() {
    xmlReport = true
    sarifReport = true
    checkDependencies = true
    disable += "GradleDependency"
}
```

Для нашего гейта добавляем `warningsAsErrors = true` и `abortOnError = true`
[предположение — NiA этого не делает, но у нас гейт строже; ср. `allWarningsAsErrors` в Go].

**Свой `:lint`-модуль** — ключевой приём. Зависимости: `com.android.tools.lint:lint-api`,
`lint-checks`, `lint-tests` версии **32.4.0** (правило: версия tools = AGP + 23 мажора;
AGP 9.4.0 ↔ 32.4.0) [проверено — Google Maven `com.android.tools.lint` stable 32.4.0; NiA держит
`androidGradlePlugin = "9.3.2"` ↔ `androidTools = "32.3.2"` с комментарием «AGP and tools should
be updated together»].

Подключение: `lintChecks(projects.lint)` в модуле-потребителе, либо **`lintPublish(projects.lint)`**
в `core:designsystem` — тогда правила уезжают вместе с модулем всем его потребителям (так делает NiA).
У NiA там `DesignSystemDetector`, запрещающий Material-компоненты (`Button`, `MaterialTheme`, …)
мимо дизайн-системы. Для нас это закрывает правило `shared-ui` и запрет хардкода токенов.

#### 8.5. Архитектурные тесты

| Инструмент | Версия | Опубликовано | Комментарий |
|---|---|---|---|
| **Konsist** `com.lemonappdev:konsist` | 0.17.3 | **2024-12-08** | ⚠️ ~21 месяц без релиза, **но** element-x держит на нём 17 тестов на Kotlin 2.4.10 |
| **ArchUnit** `com.tngtech.archunit:archunit` | **1.5.0** | 2026-08-04 | активно развивается, но Java-центричен |
| `dependency-analysis-gradle-plugin` | **3.19.2** | 2026-09-14 | таск `buildHealth`; element-x на 3.19.1 |

[проверено — Maven Central, 2026-09-17]

Konsist «заброшен по релизам, но работает» — это ровно та ситуация, где надо решение владельца.
Набор тестов element-x показывает, что именно им покрывают:
`KonsistArchitectureTest`, `KonsistCallbackTest`, `KonsistClassNameTest`, `KonsistComposableTest`,
`KonsistConfigTest`, `KonsistContentTest`, `KonsistDiTest`, `KonsistFlowTest`,
`KonsistImmutableTest`, `KonsistImportTest`, `KonsistLicenseTest`, `KonsistMethodNameTest`,
`KonsistParameterNameTest`, `KonsistPresenterTest`, `KonsistPreviewTest`, `KonsistTestTest` —
плюс намеренно падающие фикстуры в `failures/` (тесты самих тестов).

**Более дешёвая альтернатива, которую стоит рассмотреть в первую очередь [предположение]:
проверка прямо в convention-плагине**, как у element-x:

```kotlin
if (project.path.endsWith(":api")) {
    error("api module should not use setupDependencyInjection(). Move the implementation to `:impl` module")
}
```

Это ноль зависимостей, ноль времени в тестах, падает на конфигурации — то есть **быстрее и
надёжнее любого Konsist**. Konsist остаётся для того, что графом модулей не выразить: naming,
«у каждого `*ViewModel` есть `*ViewModelTest`», «`UiState` — `data class` из `val`».

#### 8.6. Компиляторные гейты

```kotlin
// AGP 9: настройка через kotlin { }, не android.kotlinOptions
kotlin {
    compilerOptions {
        allWarningsAsErrors.set(true)
        freeCompilerArgs.addAll("-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi")
    }
}
```

NiA делает `allWarningsAsErrors` **управляемым gradle-property и по умолчанию выключенным**
(`providers.gradleProperty("warningsAsErrors").map { it.toBoolean() }.orElse(false)`). Для нашего
шаблона правильнее **включить по умолчанию** [предположение — это соответствует уровню строгости
`go vet`+`golangci-lint` в Go-шаблоне и `typecheck` в SPA].

Compose-компилятор (плагин `org.jetbrains.kotlin.plugin.compose`, версия = версия Kotlin):

```kotlin
composeCompiler {
    stabilityConfigurationFiles.add(isolated.rootProject.projectDirectory.file("compose_compiler_config.conf"))
    // reportsDestination / metricsDestination — включать флагом, не всегда
}
```

Strong skipping — **дефолт с Kotlin 2.0.20**, отдельно включать не нужно.

#### 8.7. R8 — и новый DSL в AGP 9.3+

**[проверено, `developer.android.com/build/shrink-code`]**

- **R8 full mode включён по умолчанию с AGP 8.0** (`android.enableR8.fullMode=false` для отката).
- Классический DSL (до AGP 9.3):
  ```kotlin
  buildTypes {
      release {
          isMinifyEnabled = true
          isShrinkResources = true
          proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      }
  }
  ```
- **Новый упрощённый DSL с AGP 9.3+ (рекомендуемый)**:
  ```kotlin
  buildTypes {
      release {
          optimization { enable = true }   // код + ресурсы
      }
  }
  ```
  При нём оптимизированный resource shrinking всегда включён, дефолтные Android keep-правила
  подключены, а **keep-правила переезжают в `src/<variant>/keepRules/*.keep`**.
- AGP 9.0 **выкинул поддержку глобальных опций (`-dontobfuscate`) в consumer-правилах библиотек** —
  приложения их отфильтруют.
- AGP 9.0 добавил опцию `-processkotlinnullchecks` (`keep` | `remove_message` | `remove`) и сменил
  SourceFile-атрибут на `r8-map-id-<MAP_ID>` для ретрейса.
- `proguard-android.txt` запрещён (`android.r8.proguardAndroidTxt.disallowed=true`) — только
  `proguard-android-optimize.txt`.

Свои keep-правила в 2026 нужны минимально: Retrofit/OkHttp/kotlinx-serialization/Room/Hilt везут
consumer-rules сами [предположение — требует проверки сборкой в потоке 06; это классический
источник «в debug работает, в release падает»]. Поэтому **`assembleRelease` обязан быть в гейте** —
иначе R8-регрессию заметит только заказчик.

#### 8.8. Gradle: конфигурация и производительность

**Configuration cache в Gradle 9 — НЕ дефолт** [проверено,
`docs.gradle.org/current/userguide/configuration_cache.html`]: «This feature will be enabled by
default in **Gradle 10**», но с 9.0 это «the preferred mode of execution». Включается
`org.gradle.configuration-cache=true`, строгий режим — `org.gradle.configuration-cache.problems=fail`.

NiA на `main` идёт максимально агрессивно: **isolated projects включены** (коммит `12f80da651`
«Enable gradle project isolation», 2026-09-02), `org.gradle.configuration-cache.problems=fail`
(коммит `153fba10cf`), `ksp.project.isolation.enabled=true`, Gradle 9.7.1. Это значит, что их
convention-плагины написаны под ограничения isolated-projects (`isolated.rootProject`,
`@Inject BuildFeatures` в `RootPlugin`) — **копировать их надо целиком, а не кусками**.

Черновик `gradle.properties` для машины 15 ГБ RAM под нагрузкой:

```properties
org.gradle.jvmargs=-Xmx3g -XX:MaxMetaspaceSize=1g -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configuration-cache=true
org.gradle.configuration-cache.problems=fail
org.gradle.workers.max=4
android.useAndroidX=true
android.nonTransitiveRClass=true
kotlin.incremental=true
```

**Dependency locking** — приём `bitwarden/android`, стоит взять [предположение]:
`dependencyLocking { lockAllConfigurations(); lockMode.set(LockMode.STRICT) }` в `buildscript` и в
корне → файлы `gradle.lockfile`, `buildscript-gradle.lockfile`, `settings-gradle.lockfile`.
Это прямой аналог `go.sum` / `pnpm-lock.yaml`, которого Gradle по умолчанию не даёт.
Альтернатива уровнем ниже — `dependencyGuard` (Dropbox, использует NiA) и
`verification-metadata.xml` (проверка контрольных сумм).

**Dependency verification** [проверено, `docs.gradle.org/current/userguide/dependency_verification.html`]:
`./gradlew --write-verification-metadata sha256,pgp` инкрементально наполняет
`gradle/verification-metadata.xml`; поддерживаются MD5/SHA-1/SHA-256/SHA-512 («only SHA-256 and
SHA-512 are considered cryptographically secure») и PGP-подписи. Для агентского шаблона это
отдельный supply-chain-гейт: агент может предложить незнакомую зависимость, и verification не даст
молча подтянуть подменённый артефакт.

**Remote build cache — важная новость.** Бесплатный автономный **Develocity Build Cache Node
снимается с поддержки после 31.12.2026**: «no further distribution, support, or updates after
December 31, 2026», замена — Develocity Edge, окончательное снятие — с Develocity 2027.1
[проверено — обсуждение `discuss.gradle.org/t/gradle-build-cache-node-future/51819`,
`docs.develocity.ai/bcn/`, 2026-09-17]. Поскольку `HttpBuildCache` — обычный HTTP
(`PUT`/`GET` по SHA-ключу), self-host возможен на любом совместимом сервере (например,
nginx + MinIO/S3 с тонким прокси) [предположение]. Для нашей экосистемы без CI-runner'ов удалённый
кеш вообще под вопросом — локального `org.gradle.caching=true` может хватить.

#### 8.9. Свежие сдвиги в тулинге, которые надо знать до старта

1. **ktlint сменил владельца и groupId.** CHANGELOG ktlint, запись `[2.0.0-ALPHA-4] - 2026-08-20`,
   дословно: «This project is no longer maintained by Pinterest. As a result the Maven coordinates
   of the project have changed from `com.pinterest.ktlint` to `io.github.ktlint`». На Maven Central
   `com.pinterest.ktlint:ktlint-cli` заканчивается на **1.8.0**; в `io.github.ktlint.core:ktlint-cli`
   есть ровно одна версия — **2.0.0-ALPHA-4** (2026-08-22). Stable 2.0 нет. 2.0 поднимает
   минимальную Java до 12 и даёт нативные GraalVM-бинарники. [проверено]
   **Для шаблона**: берём **ktlint 1.8.0** через Spotless; в `docs/stack/dependencies.md`-аналоге
   фиксируем триггер «когда `io.github.ktlint` 2.0 станет stable и обёртки это подхватят».
2. **`org.jlleitschuh.gradle.ktlint` умеет AGP 9.** Issue #1008 «Add support for AGP 9 with
   built-in Kotlin» — **закрыт** (PR #1016). CHANGELOG: 14.0.1 — «work with gradle 9.1 and Java 25»,
   **14.1.0 — «support for Android projects with new DSL i.e. built-in Kotlin»**,
   14.2.0 — «Make plugin compatible with Isolated Projects». [проверено]
3. **⚠️ compose-rules и detekt разошлись по версиям.** Официальная страница совместимости:
   **detekt 1.23.x ↔ compose-rules ≤ 0.4.23** (последняя в линии — 0.4.28);
   **compose-rules ≥ 0.5.0 требует detekt 2.0.0-alpha.1+** (0.5.7/0.5.8 → alpha.2,
   0.5.9/0.6.0 → alpha.3). [проверено, `mrmans0n.github.io/compose-rules/detekt/` +
   `github.com/mrmans0n/compose-rules/releases`]
   **Следствие для шаблона**: оставаясь на стабильном detekt 1.23.8, мы получаем **потолок
   compose-rules 0.4.28** — то есть более двух лет улучшений Compose-правил недоступны. Это
   осознанный trade-off, который надо записать явно. Альтернатива — пойти на detekt 2.0-alpha
   ради compose-rules 0.6.x, что для шаблона-для-заказчика хуже.
4. **detekt 2.0: что именно меняется.** GroupId и plugin id → **`dev.detekt`** (issue #8432, PR #8446 —
   причина: plugin id и group id обязаны иметь общий top-level namespace);
   **`detekt-formatting` переименован в `detekt-rules-ktlint-wrapper`**; дропается legacy
   K1-резолвер в пользу **Kotlin Analysis API**; alpha.6 собран против Kotlin 2.4.10.
   Побочный эффект переезда, который признают сами авторы: **Renovate не подхватит смену groupId
   автоматически** — bump придётся делать руками. [проверено]
5. **Type resolution в detekt** (одинаково в 1.23.x и 2.0): таск `detekt` — **без** резолва,
   `detektMain`/`detektTest` — **с** резолвом, `detektBaselineMain`/`detektBaselineTest` —
   генерация baseline с резолвом; на Android появляются вариант-специфичные `detekt<Variant>`.
   В гейт идут именно `detektMain detektTest`. [проверено, `detekt.dev/docs/gettingstarted/type-resolution`]
6. **`lint-api` = AGP + 23.0.0** — эмпирически подтверждено по всей линейке на Google Maven:
   AGP 7.x ↔ 30.x, 8.x ↔ 31.x, **9.x ↔ 32.x**; для AGP 9.4.0 → **`com.android.tools.lint:lint-api:32.4.0`**.
   Своему lint-модулю нужен ещё файл-сервис
   `META-INF/services/com.android.tools.lint.client.api.IssueRegistry` с полным именем класса —
   без него AGP правила не увидит. [проверено]
7. **Что именно ловит `:lint` у NiA** (для копирования логики):
   `NiaIssueRegistry` (`api = CURRENT_API`, `minApi = 12`);
   **`TestMethodNameDetector`** — запрещает префикс `test` в имени юнит-теста (с автофиксом через
   `LintFix.create().replace()…autoFix()`) и требует формат `given_when_then`/`when_then` для
   инструментальных (regex `[^\W_]+(_[^\W_]+){1,2}`);
   **`DesignSystemDetector`** (severity **ERROR**) — банит `MaterialTheme`, `Button`,
   `OutlinedButton`, `TextButton`, `NavigationBar(Item)`, `TabRow`, `TopAppBar`-варианты, `Icons`
   в пользу обёрток `Nia*`. Это **готовый шаблон для нашего правила `design-tokens`**. [проверено]
8. **Android Lint не запускается сам**: «We strongly recommend that you explicitly run lint as part
   of a continuous integration build» — таск в гейт нужен явно (`lintRelease` или `lintDebug`).
   Обрати внимание: `checkOnly` **заменяет** весь набор проверок, а не добавляет. [проверено]
9. **R8 full mode — дефолт с AGP 8.0**, и он требует явных keep-правил в четырёх местах
   [проверено, `developer.android.com/topic/performance/app-optimization/full-mode`]:
   (а) атрибуты для рефлексии — «attributes are retained only if the associated class, field, or
   method is explicitly kept»; (б) **конструктор по умолчанию не сохраняется неявно**, даже если
   класс сохранён; (в) R8 меняет видимость методов/полей — ломает рефлексию/JNI по имени;
   (г) **Kotlin-метаданные стрипаются**, если не сохранены явно → обязателен
   `-keep class kotlin.Metadata { *; }` + `-keepattributes RuntimeVisibleAnnotations`.
   `kotlinx.serialization` везёт собственный `rules/r8.pro` (3 правила, явно про full mode) —
   дублировать вручную не нужно.
10. **`explicitApi()` в AGP 9** ставится в том же top-level `kotlin { }`:
    ```kotlin
    kotlin {
        compilerOptions {
            languageVersion.set(KotlinVersion.KOTLIN_2_4)
            allWarningsAsErrors.set(true)
        }
        explicitApi()          // либо explicitApiWarning() на переходный период
    }
    ```
    Для `core:*`-модулей это дешёвый гейт «публичный API объявлен осознанно».
11. **Полный набор опций Compose-компилятора** (`ComposeCompilerGradlePluginExtension`)
    [проверено, kotlinlang.org API-референс]: `enableIntrinsicRemember`,
    `enableNonSkippingGroupOptimization`, `enableStrongSkippingMode` (легаси-путь),
    `featureFlags: SetProperty<ComposeFeatureFlag>`, `generateFunctionKeyMetaClasses`,
    `includeComposeMappingFile`, `includeSourceInformation`, `includeTraceMarkers`,
    `metricsDestination`, `reportsDestination`, `stabilityConfigurationFile(s)`,
    `targetKotlinPlatforms`. Флаги: `IntrinsicRemember`, `OptimizeNonSkippingGroups`,
    `PausableComposition`, `StrongSkipping`.
12. **`sortDependencies`** — `com.squareup:sort-dependencies-gradle-plugin` **0.21.0** (2026-08-12),
    plugin id `com.squareup.sort-dependencies`, таски `sortDependencies`/`checkSortDependencies`
    (последний сам цепляется к `check`). Дешёвый детерминизм в блоках зависимостей — полезно
    именно для агента. [проверено]
13. **`gradle-doctor` под Gradle 9 формально не сертифицирован**: PR #440 «Update Gradle to v9»
    на 2026-09-15 **всё ещё открыт**, апстрим гоняет CI на Gradle 8.14.5. Как диагностический
    плагин, скорее всего, не ломает configuration cache, но подтверждения нет. [проверено]
14. **Renovate** поддерживает `libs.versions.toml` из коробки (менеджер `gradle`); известный
    edge-case — конфликтные PR при апдейте нескольких соседних строк каталога. [проверено]

### §9. Дедлайны Google Play и платформенные обязательности — детали

_(дополняет §5; всё проверено 2026-09-17 по developer.android.com и Play Console Help)_

#### 9.1. Календарь требований Play

| Требование | Порог / критерий | Дата | Источник |
|---|---|---|---|
| **targetSdk ≥ 36** для новых приложений и обновлений | Wear/Automotive ≥ 35, TV/XR ≥ 34 | **31.08.2026** (продление по заявке до **01.11.2026**) | `developer.android.com/google/play/requirements/target-sdk` |
| targetSdk ≥ 35 для существующих приложений (иначе не видны новым пользователям на новых ОС) | — | действует | там же |
| User-perceived **crash rate** | ≤ 1.09 % overall (8 % на модель телефона) | действует | `support.google.com/googleplay/android-developer/answer/17492799` |
| User-perceived **ANR rate** | ≤ 0.47 % overall | действует | там же |
| Excessive partial wake locks | ≤ 5 % | действует | там же |
| 64-bit + **16 KB page size** (Android TV) | — | 01.08.2026 | там же |
| 16 KB page size (Wear OS) | — | 15.09.2026 | там же |
| **16 KB page size (телефоны)** | приложения с targetSdk ≥ 35 | **01.02.2027** | `developer.android.com/guide/practices/page-sizes` |
| **DEX Code Optimization** | минимум 25 % optimization/obfuscation/shrinking; приложения с DEX > 10 МБ | **02.2027** | Play Console tech quality |
| Memory Usage (Anon RSS + Swap) | по тирам RAM, 1–5 ГБ на P90 | 02.2027 | там же |
| Bitmap Memory Usage | > 200 МБ (foreground/background), > 400 МБ (cached) на P90 | 02.2027 | там же |
| **Zero-Tap Sign-In Restoration** (Restore Credentials API) | Android 9+ | **04.2027** | там же |
| **Developer verification** (регионально: Бразилия, Индонезия, Сингапур, Таиланд) | участвующие сторы, Android 7+ | **30.09.2026** | `developer.android.com/developer-verification` |
| Developer verification — **глобально, все приложения на certified-устройствах** | — | **2027** | там же |

Требования на targetSdk-2027 **официально не опубликованы** ни на developer.android.com, ни в
Play Console Help [проверено]. По годовой каденции (планка поднимается 31 августа, объявляется
за 10–12 месяцев) ожидается **targetSdk 37 к ~31.08.2027** [предположение].

**Три следствия для архитектуры шаблона**, которые нельзя отложить:

1. **`isMinifyEnabled = true` в release — не опция, а требование** (DEX Code Optimization,
   февраль 2027). Значит R8 обязан быть частью гейта с первого дня.
2. **Restore Credentials (апрель 2027)** упирается в серверную часть. Наш бэкенд — cookie-auth;
   Restore Credentials «is a particularly recommended feature for apps that already support
   passkeys because of the common underlying server-side implementation». Go-бэкенд надо
   проектировать так, чтобы WebAuthn-эндпоинты добавлялись **без ломки cookie-сессии** — это
   вход в поток 07.
3. **Developer verification** — организационная, а не техническая зависимость (D-U-N-S,
   регистрация юрлица), процесс на недели. Для РФ-дистрибуции вне Play в 2027 это критично —
   поток 05.

#### 9.2. 16 KB page size — что реально делать

Дословно [проверено, `developer.android.com/guide/practices/page-sizes`]:

> «If your app only uses code written in the Java programming language or in Kotlin, including all
> libraries or SDKs, then your app already supports 16 KB devices.»

**Но «Kotlin-only» — про весь граф зависимостей.** В нашем профиле нативный код почти наверняка
придёт с картами (Яндекс MapKit / Google Maps SDK), сканером QR (ML Kit barcode, ZXing-CPP),
иногда SQLCipher/Realm, Sentry NDK, Crashlytics NDK. Проверять надо:

```bash
# 1) выравнивание .so в APK
check_elf_alignment.sh app-release.apk          # ожидаем ALIGNED для всех arm64-v8a
# 2) zip-выравнивание AAB
bundletool dump config --bundle=app.aab | grep alignment   # ожидаем PAGE_ALIGNMENT_16K
# 3) zipalign
$SDK/build-tools/36.0.0/zipalign -v -c -P 16 4 app-release.apk
# 4) по каждой .so
llvm-objdump -p lib.so | grep LOAD              # ожидаем align 2**14
# 5) RELRO-ловушка (иначе segfault в рантайме)
llvm-readelf -Wl lib.so | grep 'RELRO\|Type'    # (VirtAddr + MemSiz) % 0x4000 == 0
```

Плюс APK Analyzer в Studio (колонка **Alignment**), Android Lint и предупреждение в Play Console.

#### 9.3. Edge-to-edge — что именно сломалось и чем чинится

API [проверено]: `WindowCompat.enableEdgeToEdge(window)` — androidx.core, **добавлен в 1.17.0**
(текущий stable core — 1.19.0); `ComponentActivity.enableEdgeToEdge()` — androidx.activity,
**добавлен в 1.8.0**, не deprecated (текущий stable activity — 1.13.0).

Что перестало работать при target ≥ 35 [проверено, `…/about/versions/15/behavior-changes-15`]:

| Элемент | Поведение |
|---|---|
| Gesture nav bar | прозрачный; `setNavigationBarColor` / `R.attr#navigationBarColor` **deprecated и не действуют** |
| 3-button nav | opacity 80 %; `setNavigationBarColor` deprecated, но действует; `navigationBarContrastEnforced = true` |
| Status bar | прозрачный; `setStatusBarColor` / `R.attr#statusBarColor` **deprecated и не действуют** |
| Display cutout | `layoutInDisplayCutoutMode` для non-floating окон принудительно `ALWAYS` |

Чинится Material 3 `Scaffold` («can reduce the work required to be compatible with the Android 15
edge-to-edge enforcement») + явная обработка `WindowInsets.ime` на формах. Для нашего профиля
критичны экраны с нижней панелью действий (сохранить/отменить) и списки с FAB.

#### 9.4. Predictive back — формы с несохранёнными изменениями

Компоненты: `OnBackPressedCallback` / `OnBackPressedDispatcher` (AppCompat 1.6.0-alpha05+ /
activity 1.6.0-alpha05+), платформенный `OnBackInvokedCallback`, Compose — `PredictiveBackHandler`
(с прогрессом) и `BackHandler` (без).

```kotlin
PredictiveBackHandler(enabled = hasUnsavedChanges) { progress: Flow<BackEventCompat> ->
    try {
        progress.collect { backEvent -> /* анимация по backEvent.progress */ }
        // финальное действие — показать диалог / выйти
    } catch (e: CancellationException) {
        // жест отменён — вернуть UI
    }
}
```

**Инвариант для шаблона**: любые «есть несохранённые изменения, выйти?» — только через
`OnBackPressedDispatcher`/`BackHandler`. `onBackPressed()` при target 36+ **не вызывается**,
`KEYCODE_BACK` не диспатчится, и «intercepting back events from `KeyEvent.KEYCODE_BACK` is no
longer supported». [проверено]

#### 9.5. Прочие обязательности с точными API

**`POST_NOTIFICATIONS`** [проверено]: при установке на Android 13+ «your app's notifications are
**off by default**»; при апгрейде устройства/восстановлении из бэкапа разрешение пре-грантится.
FGS запускать можно без него, но уведомление показать — нет. Практика: спрашивать не на старте,
а в контексте (после логина / при включении уведомлений), иначе deny и путь только через
системные настройки.

**Photo picker** [проверено]: `androidx.activity` **1.7.0+**; контракты `PickVisualMedia` /
`PickMultipleVisualMedia`; при отсутствии системного пикера библиотека сама падает в
`ACTION_OPEN_DOCUMENT`. Разрешений **не требует**. `READ_EXTERNAL_STORAGE`/`READ_MEDIA_*` в
бизнес-приложении 2026 быть не должно; произвольные файлы — SAF.

**Per-app language** — уточнение к §3.5: кроме `generateLocaleConfig = true` обязателен
**`localeFilters`**, иначе APK тащит десятки локалей из AppCompat/Play Services:

```kotlin
android {
    androidResources {
        generateLocaleConfig = true            // added in AGP 8.1.0
        localeFilters += listOf("en", "ru")
    }
}
```

⚠️ **Ограничение, которое надо честно заложить в дизайн**: backward-compatible
`AppCompatDelegate.setApplicationLocales` работает «with the `AppCompatActivity` context,
**not the application context**, for Android 12 (API level 32) and earlier». При `minSdk = 26`
это значит, что внутриприложенческий переключатель языка требует **AppCompat и наследования
`AppCompatActivity`** вместо голой `ComponentActivity` — неприятно для Compose-only, но
альтернативы для API 26–32 нет. [проверено]

**Foreground services** [проверено]:
- Android 14 (target 34): тип FGS обязателен, иначе `MissingForegroundServiceTypeException`;
  без соответствующего разрешения — `SecurityException`. Прямая рекомендация доки: «If a use case
  in your app isn't associated with any of these types, we strongly recommend that you **migrate
  your logic to use WorkManager**».
- Android 15 (target 35): `dataSync` — суммарно **6 часов за 24 часа**, дальше `Service.onTimeout()`
  и пара секунд на `stopSelf()`, иначе фатальный
  `RemoteServiceException: "A foreground service of type dataSync did not stop within its timeout"`.
  Новый старт после исчерпания → `ForegroundServiceStartNotAllowedException`.

**Инвариант шаблона**: FCM data-message → **WorkManager**, не FGS. FGS оставить только под явно
пользовательские длительные операции, с типом `dataSync` и реализованным `onTimeout`.

**App Links** [проверено, `…/training/app-links/verify-applinks`]: `android:autoVerify="true"` +
intent-filter с `VIEW` + `BROWSABLE` + `DEFAULT` + scheme `http`/`https`; система тянет
`https://<host>/.well-known/assetlinks.json`, ждать «at least **20 seconds**» после установки.
На Android 11 и ниже — «only if it finds a matching Digital Asset Links file for **all** hosts».
Диагностика:

```bash
adb shell pm get-app-links <pkg>                    # ожидаем state = verified
adb shell pm set-app-links --package <pkg> 0 all    # сброс
adb shell pm verify-app-links --re-verify <pkg>     # Android 12+
```

`assetlinks.json` должен отдаваться под HTTPS с `Content-Type: application/json` и **без
редиректов** — это инфра-задача (`<org>/infra`, поток 07).

**Baseline Profiles** [проверено, `…/topic/performance/baselineprofiles/overview`]: «improve code
execution speed by about **30 %** from the first launch». Критичная деталь конфигурации:
профиль-генерирующий вариант должен иметь `isMinifyEnabled = false` (+ `-dontobfuscate`,
`-dontoptimize` если без плагина), а релиз — `isMinifyEnabled = true`; R8 сам переписывает
правила профиля под обфусцированный код. Рекомендуется брать **и** Baseline, **и** Startup profile.

#### 9.6. Credential Manager при cookie-auth — что нужно и когда

`androidx.credentials:credentials` **1.6.0** (stable, 2026-07-29) [проверено]. Для нашего
cookie-flow:

1. **Сам Credential Manager для логина не нужен** — он выступает лишь менеджером паролей
   (`GetPasswordOption` подставит логин/пароль в форму). httpOnly-cookie он не касается.
2. **Passkeys** потребуют WebAuthn на Go-бэкенде + Digital Asset Links — отдельный проект.
3. **Restore Credentials — станет требованием Play в апреле 2027** (Zero-Tap Sign-In Restoration,
   Android 9+): restore key сохраняется бэкап-сервисом и даёт автологин на новом устройстве.

**Вывод [предположение]**: `androidx.credentials` класть в каталог сразу, использовать поэтапно;
в поток 07 занести требование «Go-бэкенд должен уметь добавить WebAuthn-эндпоинты не ломая
cookie-сессию».

#### 9.7. Adaptive: что требует Play для больших экранов

Формально **обязательных требований Play к большим экранам нет** [проверено] — есть
принуждение со стороны ОС (§5.1) и качественные гайдлайны
`developer.android.com/docs/quality-guidelines/adaptive-app-quality` (обновлены 2026-04-10),
которые «replace and extend the guidance formerly provided in the large screen app quality
guidelines»:

| Tier | Название | Критерий |
|---|---|---|
| 3 | Adaptive ready | «runs full screen … The app is **not letterboxed**; it does not run in compatibility mode» + базовая поддержка клавиатуры/мыши/трекпада/стилуса |
| 2 | Adaptive optimized | layout-оптимизации под все размеры + расширенная поддержка внешнего ввода |
| 1 | Adaptive differentiated | UX под конкретное устройство, мультитаскинг, foldable postures, drag&drop, стилус |

Рекомендация доки — достичь **Tier 2**. Тестовые конфигурации: Foldable 841×701 dp,
8" планшет 1024×640 dp, 10.5" планшет 1280×800 dp, 13" Chromebook 1600×900 dp.

#### 9.8. Матрица совместимости AGP 9.x (полная)

[проверено, release notes каждой версии]

| AGP | Дата | Max API | Gradle (min) | Build Tools | NDK (default) | JDK (min) |
|---|---|---|---|---|---|---|
| **9.4.0** | сентябрь 2026 | **37** | **9.6.0** | 36.0.0 | 28.2.13676358 | **17** |
| 9.3.0 / 9.3.2 | июль 2026 | 37 | 9.5.0 | 36.0.0 | 28.2.13676358 | 17 |
| 9.2.0 / 9.2.1 | апрель 2026 | 37 | 9.4.1 | 36.0.0 | 28.2.13676358 | 17 |
| 9.1.0 / 9.1.1 | апрель 2026 | 37 | 9.3.1 | 36.0.0 | 28.2.13676358 | 17 |
| 9.0.0 / 9.0.1 | январь 2026 | 36.1 | 9.1.0 | 36.0.0 | 28.2.13676358 | 17 |

Kotlin ↔ AGP ↔ R8 [проверено, `developer.android.com/build/kotlin-support`, обновлена 2026-07-06]:

| Kotlin | Required AGP | Required R8 |
|---|---|---|
| **2.4** | **8.5.2+** | 9.1.29 |
| 2.3 | 8.2.2 – 8.13 | 8.13.19 (сноска: «9.x versions before 9.0.28 don't support Kotlin 2.3») |
| 2.2 | 7.3.1 – 8.10 | 8.10.21 |

Android Studio ↔ AGP [проверено, `developer.android.com/studio/releases`, обновлена 2026-09-16]:
**Quail 4 | 2026.1.4** поддерживает AGP **7.1 – 9.4**. Политика: «Each Android Studio version will
support AGP versions released within the previous 3 years». Минимум для API 37 — Studio
Panda 3 | 2025.3.3 Patch 1 и **AGP 9.1.1**.

**Toolchain — каноническая форма Google** [проверено, `developer.android.com/build/jdks`,
обновлена 2026-09-11]:

```kotlin
java {
    toolchain { languageVersion = JavaLanguageVersion.of(17) }
}
```

Дословно: «We recommend that you **always specify the Java toolchain**… Specify the toolchain at
the top level of your module's `build.gradle(.kts)` file. This works if your source is Kotlin,
Java, or a mix of both.» Форма `kotlin { jvmToolchain(N) }` — это KGP-API, **на страницах AGP она
не фигурирует** [предположение: работает, но не рекомендуемая Google форма].
На Kotlin 2.2+ отдельный `jvmTarget` не нужен: он наследуется из
`android.compileOptions.targetCompatibility`.

#### 9.9. Полный список изменённых дефолтов AGP 9.0

[проверено, AGP 9.0 release notes] — это то, что молча меняет поведение существующего проекта:

| Property | Было → Стало | Смысл |
|---|---|---|
| `android.newDsl` | false → **true** | новый DSL, старый variant API недоступен |
| `android.builtInKotlin` | false → **true** | встроенный Kotlin |
| `android.uniquePackageNames` | false → **true** | уникальный package name у каждой библиотеки |
| `android.useAndroidx` | false → **true** | |
| `android.default.androidx.test.runner` | false → **true** | `AndroidJUnitRunner` по умолчанию |
| `android.dependency.useConstraints` | **true → false** | constraints только в AndroidTest |
| `android.enableAppCompileTimeRClass` | false → **true** | non-final R-класс (ломает `switch` по R-константам) |
| **`android.sdk.defaultTargetSdkToCompileSdkIfUnset`** | false → **true** | **незаданный `targetSdk` теперь = `compileSdk`** (раньше = `minSdk`) |
| `android.onlyEnableUnitTestForTheTestedBuildType` | false → **true** | unit-тесты только для debug |
| `android.proguard.failOnMissingFiles` | false → **true** | опечатка в имени keep-файла = ошибка сборки |
| `android.r8.optimizedResourceShrinking` | false → **true** | R8 удаляет больше ресурсов |
| `android.r8.strictFullModeForKeepRules` | false → **true** | `-keep class A` больше не подразумевает `-keep class A { <init>(); }` |
| **`android.defaults.buildfeatures.resvalues`** | **true → false** | `resValues` выключены по умолчанию |
| **`android.defaults.buildfeatures.shaders`** | **true → false** | шейдеры выключены по умолчанию |
| `android.r8.proguardAndroidTxt.disallowed` | false → **true** | только `proguard-android-optimize.txt` |
| `android.r8.globalOptionsInConsumerRules.disallowed` | false → **true** | публикация библиотеки падает при `-dontoptimize`/`-dontobfuscate` в consumer-правилах |

Удалены: `android.defaults.buildfeatures.aidl`, `android.defaults.buildfeatures.renderscript`
(теперь только per-module `buildFeatures { aidl = true }`).
Запрещены (сборка падает): `android.r8.integratedResourceShrinking`,
`android.enableNewResourceShrinker.preciseShrinking`.

**Самое опасное — `defaultTargetSdkToCompileSdkIfUnset`**: молчаливое изменение того, каким
окажется `targetSdk` при его отсутствии, с прямым влиянием на требования Play. Рекомендация доки
и наш инвариант: **всегда задавать `targetSdk` явно** (и для приложения, и для тестов).

Дополнительно AGP 9.4: `android.newDsl.optOut=:module` (временный per-module opt-out;
«Starting with AGP 10, the new Variant API is mandatory for all projects») и строгий 1:1 паритет
flavor-dimensions между app и dynamic feature.

#### 9.10. Dynamic color в брендовом приложении — трёхуровневая схема

`dynamicLightColorScheme(context)` / `dynamicDarkColorScheme(context)` доступны с **Android 12
(API 31)**; при `minSdk 26` ветка fallback обязательна в любом случае. [проверено]

Предлагаемая архитектура токенов (прямая калька того, что в SPA-шаблоне делает
`app/styles/index.css`) [предположение]:

| Уровень | Что | Кто читает |
|---|---|---|
| **1. reference** | брендовая палитра из Material Theme Builder → `lightColorScheme()`/`darkColorScheme()` | только уровень 2 |
| **2. system** | `MaterialTheme.colorScheme` — роли `primary`, `surface`, `onSurface`, `outline`… Сюда, и **только сюда**, подставляется dynamic color при включённом тумблере | M3-компоненты, наши компоненты |
| **3. domain** | собственные семантические токены через `CompositionLocal` + `AppTheme`: статусы («просрочено/в работе/готово»), бейджи, палитра графиков, семантика денег и дельт. **Не** производятся от dynamic color | наши компоненты |

Правило (аналог `shared-ui`): компоненты обращаются **только** к уровням 2 и 3, никогда к сырым
цветам и никогда напрямую к reference-палитре. Тогда включение dynamic color — изменение в одной
точке, и оно физически не может поехать в статусах и графиках.

**Dynamic color по умолчанию выключен** [предположение, 4 аргумента]: (1) бренд-контроль
заказчика; (2) контраст на произвольных обоях недетерминирован, а у нас плотные таблицы и
статусы; (3) семантические статусы всё равно фиксированы → визуальный разнобой;
(4) **скриншот-тесты становятся невоспроизводимыми**. NiA делает так же
(`disableDynamicTheming: Boolean = true`).


## Рекомендации для шаблона

Всё ниже — **черновики**, предназначенные для потока 08 (дизайн шаблона) и потока 06 (эмпирическая
проверка). Ни один листинг не прогонялся сборкой на этой машине (Android SDK не установлен —
это работа потока 06).

### R1. Базовые решения

| Вопрос | Решение | Почему |
|---|---|---|
| Язык / UI | **Kotlin 2.4.20 + Jetpack Compose + Material 3 (1.4.0)** | «All new Android APIs and libraries are now Compose-exclusive», View-система в maintenance mode |
| Архитектура презентации | **официальный MVVM + UDF** (`ViewModel` + `StateFlow<UiState>`), **не** MVI-фреймворк | официальные рекомендации; Circuit до 1.0; +1 зависимость и барьер найма |
| Навигация | **Navigation 3** (`androidx.navigation3` 1.1.7) | официальная рекомендация; берут оба лагеря и Signal |
| DI | **Hilt 2.60.1 + androidx.hilt 1.4.0** | официально, compile-time граф, найм; Metro — кандидат при уходе в KMP |
| Сеть | **Retrofit 3.0.0 + OkHttp 5.5.0 + kotlinx-serialization 1.11.0** | Android-only профиль; решение пересматривается вместе с потоком 03 |
| Хранилище | **DataStore 1.2.1**; Room 2.8.5 — **точечно**, когда появится оффлайн-список | профиль offline-**tolerant**, не offline-first |
| SDK | `compileSdk = 37`, `targetSdk = 37`, `minSdk = 26` | Play требует ≥36 с 31.08.2026; 37 закрывает 2027; minSdk 26 = 96.1 % |
| Toolchain | **JDK 21** (`java { toolchain { … } }`), `jvmTarget`/`compileOptions` — 17 | Robolectric 4.16+ требует JDK 21 для API 36/37; AGP 9.4 требует минимум JDK 17 |
| Модульность | `:app` + `:feature:<x>` (без `api`/`impl`) + 8–10 `:core:*` + `:lint` + `:benchmarks` | между element-x (266) и bitwarden (10); путь дробления задокументирован |
| Screenshot | **Roborazzi 1.74.0** | официальный плагин deprecated в alpha16 |
| Coverage | **Kover 0.9.9**, floor 75 % | зеркалит `cover-check` Go-шаблона |
| Формат | **Spotless 8.10.2 + ktlint 1.8.0** | один глагол `spotlessApply` на Kotlin+KTS+XML |
| Статанализ | **detekt 1.23.8 + compose-rules 0.4.28 + Android Lint (свой `:lint` с `DesignSystemDetector`)** | `dev.detekt` 2.0 ещё alpha; compose-rules 0.5.0+ требует её (§8.9) |

### R2. Черновик `gradle/libs.versions.toml`

Все версии проверены 2026-09-17 по Google Maven / Maven Central.

```toml
[versions]
# ── toolchain ──────────────────────────────────────────────────────────────
agp                  = "9.4.0"      # Gradle ≥ 9.6.0, JDK ≥ 17, maxApi 37
androidTools         = "32.4.0"     # ВСЕГДА = AGP + 23 мажора (lint-api, common)
kotlin               = "2.4.20"
ksp                  = "2.3.12"
jdkToolchain         = "21"         # Robolectric 4.16+ требует 21 для SDK 36/37
javaTarget           = "17"
compileSdk           = "37"
targetSdk            = "37"
minSdk               = "26"

# ── androidx / compose ─────────────────────────────────────────────────────
composeBom           = "2026.09.00" # → compose 1.12.1, material3 1.4.0, adaptive 1.3.0
activity             = "1.13.0"
appcompat            = "1.8.0"      # нужен для per-app language ниже API 33
coreKtx              = "1.19.0"
coreSplashscreen     = "1.2.0"
lifecycle            = "2.11.0"
navigation3          = "1.1.7"
material3Adaptive    = "1.3.0"
window               = "1.5.1"
datastore            = "1.2.1"
room                 = "2.8.5"      # включать только когда нужен оффлайн
work                 = "2.11.2"
paging               = "3.5.1"
profileinstaller     = "1.4.1"
benchmark            = "1.5.0"
credentials          = "1.6.0"
tracing              = "2.0.2"
desugarJdkLibs       = "2.1.5"

# ── DI ─────────────────────────────────────────────────────────────────────
hilt                 = "2.60.1"
hiltExt              = "1.4.0"

# ── сеть / данные ──────────────────────────────────────────────────────────
retrofit             = "3.0.0"
okhttp               = "5.5.0"
kotlinxSerialization = "1.11.0"
kotlinxCoroutines    = "1.11.0"
kotlinxDatetime      = "0.8.0"
kotlinxImmutable     = "0.5.2"
coil                 = "3.6.2"

# ── тесты ──────────────────────────────────────────────────────────────────
junit4               = "4.13.2"
truth                = "1.4.5"
androidxTest         = "1.7.0"
androidxTestExt      = "1.3.0"
espresso             = "3.7.0"
uiautomator          = "2.4.0"
turbine              = "1.2.1"
robolectric          = "4.17"       # поддерживает SDK 23..37
roborazzi            = "1.74.0"
mockk                = "1.14.11"
testRetry            = "1.6.6"

# ── качество ───────────────────────────────────────────────────────────────
detekt               = "1.23.8"     # dev.detekt 2.0 — всё ещё alpha
composeRules         = "0.4.28"     # ⚠️ потолок для detekt 1.23.x; 0.5.0+ требует detekt 2.0-alpha
spotless             = "8.10.2"
ktlint               = "1.8.0"      # последняя на com.pinterest.ktlint; 2.0 → io.github.ktlint.core (alpha)
ktlintGradle         = "14.2.0"     # AGP 9 built-in Kotlin поддержан с 14.1.0
sortDependencies     = "0.21.0"
kover                = "0.9.9"      # AGP 9 поддержан с 0.9.4
dependencyAnalysis   = "3.19.2"
konsist              = "0.17.3"     # ⚠ последний релиз 2024-12-08

[libraries]
# compose
androidx-compose-bom            = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-compose-ui             = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-ui-tooling     = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-compose-ui-test        = { group = "androidx.compose.ui", name = "ui-test-junit4" }
androidx-compose-ui-testManifest= { group = "androidx.compose.ui", name = "ui-test-manifest" }
androidx-compose-material3      = { group = "androidx.compose.material3", name = "material3" }
androidx-compose-material3-windowSizeClass = { group = "androidx.compose.material3", name = "material3-window-size-class" }
androidx-compose-material3-adaptive        = { group = "androidx.compose.material3.adaptive", name = "adaptive", version.ref = "material3Adaptive" }
androidx-compose-material3-adaptive-layout = { group = "androidx.compose.material3.adaptive", name = "adaptive-layout", version.ref = "material3Adaptive" }
androidx-compose-material3-adaptive-nav3   = { group = "androidx.compose.material3.adaptive", name = "adaptive-navigation3", version.ref = "material3Adaptive" }
# ⚠ material-icons-* НЕ подключаем: библиотека прекращена, иконки — Material Symbols в res/drawable

# навигация
androidx-navigation3-runtime    = { group = "androidx.navigation3", name = "navigation3-runtime", version.ref = "navigation3" }
androidx-navigation3-ui         = { group = "androidx.navigation3", name = "navigation3-ui", version.ref = "navigation3" }
androidx-lifecycle-viewmodel-navigation3 = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-navigation3", version.ref = "lifecycle" }

# lifecycle / activity / core
androidx-activity-compose       = { group = "androidx.activity", name = "activity-compose", version.ref = "activity" }
androidx-appcompat              = { group = "androidx.appcompat", name = "appcompat", version.ref = "appcompat" }
androidx-core-ktx               = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
androidx-core-splashscreen      = { group = "androidx.core", name = "core-splashscreen", version.ref = "coreSplashscreen" }
androidx-lifecycle-runtimeCompose   = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycle" }
androidx-lifecycle-viewModelCompose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
androidx-window                 = { group = "androidx.window", name = "window", version.ref = "window" }
androidx-datastore-preferences  = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }
androidx-work-ktx               = { group = "androidx.work", name = "work-runtime-ktx", version.ref = "work" }
androidx-credentials            = { group = "androidx.credentials", name = "credentials", version.ref = "credentials" }
androidx-profileinstaller       = { group = "androidx.profileinstaller", name = "profileinstaller", version.ref = "profileinstaller" }
android-desugarJdkLibs          = { group = "com.android.tools", name = "desugar_jdk_libs", version.ref = "desugarJdkLibs" }

# DI
hilt-android                    = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-core                       = { group = "com.google.dagger", name = "hilt-core", version.ref = "hilt" }
hilt-compiler                   = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
hilt-android-testing            = { group = "com.google.dagger", name = "hilt-android-testing", version.ref = "hilt" }
hilt-navigation-compose         = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hiltExt" }
hilt-ext-work                   = { group = "androidx.hilt", name = "hilt-work", version.ref = "hiltExt" }
hilt-ext-compiler               = { group = "androidx.hilt", name = "hilt-compiler", version.ref = "hiltExt" }

# сеть / данные
retrofit-core                   = { group = "com.squareup.retrofit2", name = "retrofit", version.ref = "retrofit" }
retrofit-kotlinx-serialization  = { group = "com.squareup.retrofit2", name = "converter-kotlinx-serialization", version.ref = "retrofit" }
okhttp-core                     = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }
okhttp-logging                  = { group = "com.squareup.okhttp3", name = "logging-interceptor", version.ref = "okhttp" }
kotlinx-serialization-json      = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinxSerialization" }
kotlinx-coroutines-core         = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version.ref = "kotlinxCoroutines" }
kotlinx-coroutines-android      = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "kotlinxCoroutines" }
kotlinx-datetime                = { group = "org.jetbrains.kotlinx", name = "kotlinx-datetime", version.ref = "kotlinxDatetime" }
kotlinx-collections-immutable   = { group = "org.jetbrains.kotlinx", name = "kotlinx-collections-immutable", version.ref = "kotlinxImmutable" }
coil-compose                    = { group = "io.coil-kt.coil3", name = "coil-compose", version.ref = "coil" }
coil-network-okhttp             = { group = "io.coil-kt.coil3", name = "coil-network-okhttp", version.ref = "coil" }

# тесты
junit4                          = { group = "junit", name = "junit", version.ref = "junit4" }
truth                           = { group = "com.google.truth", name = "truth", version.ref = "truth" }
kotlinx-coroutines-test         = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "kotlinxCoroutines" }
turbine                         = { group = "app.cash.turbine", name = "turbine", version.ref = "turbine" }
robolectric                     = { group = "org.robolectric", name = "robolectric", version.ref = "robolectric" }
mockk                           = { group = "io.mockk", name = "mockk-jvm", version.ref = "mockk" }
mockwebserver3                  = { group = "com.squareup.okhttp3", name = "mockwebserver3", version.ref = "okhttp" }
androidx-test-core              = { group = "androidx.test", name = "core-ktx", version.ref = "androidxTest" }
androidx-test-runner            = { group = "androidx.test", name = "runner", version.ref = "androidxTest" }
androidx-test-rules             = { group = "androidx.test", name = "rules", version.ref = "androidxTest" }
androidx-test-ext-junit         = { group = "androidx.test.ext", name = "junit-ktx", version.ref = "androidxTestExt" }
androidx-test-espresso-core     = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espresso" }
androidx-test-uiautomator       = { group = "androidx.test.uiautomator", name = "uiautomator", version.ref = "uiautomator" }
androidx-benchmark-macro        = { group = "androidx.benchmark", name = "benchmark-macro-junit4", version.ref = "benchmark" }
roborazzi                       = { group = "io.github.takahirom.roborazzi", name = "roborazzi", version.ref = "roborazzi" }
roborazzi-compose               = { group = "io.github.takahirom.roborazzi", name = "roborazzi-compose", version.ref = "roborazzi" }
roborazzi-junit-rule            = { group = "io.github.takahirom.roborazzi", name = "roborazzi-junit-rule", version.ref = "roborazzi" }
roborazzi-a11y-check            = { group = "io.github.takahirom.roborazzi", name = "roborazzi-accessibility-check", version.ref = "roborazzi" }
konsist                         = { group = "com.lemonappdev", name = "konsist", version.ref = "konsist" }

# качество
detekt-formatting               = { group = "io.gitlab.arturbosch.detekt", name = "detekt-formatting", version.ref = "detekt" }
detekt-compose-rules            = { group = "io.nlopez.compose.rules", name = "detekt", version.ref = "composeRules" }

# lint (для своего :lint-модуля)
lint-api                        = { group = "com.android.tools.lint", name = "lint-api", version.ref = "androidTools" }
lint-checks                     = { group = "com.android.tools.lint", name = "lint-checks", version.ref = "androidTools" }
lint-tests                      = { group = "com.android.tools.lint", name = "lint-tests", version.ref = "androidTools" }

# зависимости build-logic
android-gradlePlugin            = { group = "com.android.tools.build", name = "gradle-api", version.ref = "agp" }
android-tools-common            = { group = "com.android.tools", name = "common", version.ref = "androidTools" }
kotlin-gradlePlugin             = { group = "org.jetbrains.kotlin", name = "kotlin-gradle-plugin", version.ref = "kotlin" }
compose-gradlePlugin            = { group = "org.jetbrains.kotlin", name = "compose-compiler-gradle-plugin", version.ref = "kotlin" }
ksp-gradlePlugin                = { group = "com.google.devtools.ksp", name = "com.google.devtools.ksp.gradle.plugin", version.ref = "ksp" }
spotless-gradlePlugin           = { group = "com.diffplug.spotless", name = "spotless-plugin-gradle", version.ref = "spotless" }
detekt-gradlePlugin             = { group = "io.gitlab.arturbosch.detekt", name = "detekt-gradle-plugin", version.ref = "detekt" }
kover-gradlePlugin              = { group = "org.jetbrains.kotlinx", name = "kover-gradle-plugin", version.ref = "kover" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
android-library     = { id = "com.android.library",     version.ref = "agp" }
android-lint        = { id = "com.android.lint",        version.ref = "agp" }
android-test        = { id = "com.android.test",        version.ref = "agp" }
# ⚠ kotlin-android НЕ объявляем — AGP 9 компилирует Kotlin сам
kotlin-jvm          = { id = "org.jetbrains.kotlin.jvm",                 version.ref = "kotlin" }
kotlin-serialization= { id = "org.jetbrains.kotlin.plugin.serialization",version.ref = "kotlin" }
compose             = { id = "org.jetbrains.kotlin.plugin.compose",      version.ref = "kotlin" }
ksp                 = { id = "com.google.devtools.ksp",                  version.ref = "ksp" }
hilt                = { id = "com.google.dagger.hilt.android",           version.ref = "hilt" }
room                = { id = "androidx.room",                            version.ref = "room" }
baselineprofile     = { id = "androidx.baselineprofile",                 version.ref = "benchmark" }
roborazzi           = { id = "io.github.takahirom.roborazzi",            version.ref = "roborazzi" }
kover               = { id = "org.jetbrains.kotlinx.kover",              version.ref = "kover" }
detekt              = { id = "io.gitlab.arturbosch.detekt",              version.ref = "detekt" }
spotless            = { id = "com.diffplug.spotless",                    version.ref = "spotless" }
dependencyAnalysis  = { id = "com.autonomousapps.dependency-analysis",   version.ref = "dependencyAnalysis" }
testRetry           = { id = "org.gradle.test-retry",                    version.ref = "testRetry" }

# плагины самого проекта (build-logic)
stit-android-application         = { id = "stit.android.application" }
stit-android-application-compose = { id = "stit.android.application.compose" }
stit-android-library             = { id = "stit.android.library" }
stit-android-library-compose     = { id = "stit.android.library.compose" }
stit-android-feature             = { id = "stit.android.feature" }
stit-android-lint                = { id = "stit.android.lint" }
stit-android-room                = { id = "stit.android.room" }
stit-android-test                = { id = "stit.android.test" }
stit-hilt                        = { id = "stit.hilt" }
stit-jvm-library                 = { id = "stit.jvm.library" }
stit-screenshot                  = { id = "stit.screenshot" }
stit-root                        = { id = "stit.root" }
```

### R3. Convention-плагины `build-logic`

12 плагинов (вместо 17 у NiA — у нас нет Firebase, flavors и JaCoCo):

| id | Класс | Что включает |
|---|---|---|
| `stit.android.application` | `AndroidApplicationConventionPlugin` | `com.android.application`, `configureKotlinAndroid`, `targetSdk`, `animationsDisabled`, `stit.android.lint`, Spotless, R8-дефолты, dependency locking |
| `stit.android.application.compose` | `AndroidApplicationComposeConventionPlugin` | + плагин compose + `configureAndroidCompose` |
| `stit.android.library` | `AndroidLibraryConventionPlugin` | `com.android.library`, `configureKotlinAndroid`, **авто-`resourcePrefix` из пути модуля**, `disableUnnecessaryAndroidTests`, базовые тестовые зависимости |
| `stit.android.library.compose` | `AndroidLibraryComposeConventionPlugin` | + Compose |
| `stit.android.feature` | `AndroidFeatureConventionPlugin` | `stit.android.library` + `stit.hilt` + `stit.screenshot` + `:core:ui`, `:core:designsystem`, lifecycle-compose, `navigation3-runtime`, `hilt-navigation-compose`; **проверка «feature не зависит от feature»** |
| `stit.hilt` | `HiltConventionPlugin` | KSP + `hilt-compiler`; для jvm — `hilt-core`, для android — плагин Hilt + `hilt-android`; **`kspTest(hilt-compiler)` для Robolectric-тестов** |
| `stit.android.room` | `AndroidRoomConventionPlugin` | плагин `androidx.room` + KSP + `room.generateKotlin=true` + `schemaDirectory` |
| `stit.android.lint` | `AndroidLintConventionPlugin` | `warningsAsErrors`, `abortOnError`, `checkDependencies`, `sarifReport`, `baseline` |
| `stit.android.test` | `AndroidTestConventionPlugin` | `com.android.test` + GMD (для benchmarks) |
| `stit.jvm.library` | `JvmLibraryConventionPlugin` | `kotlin.jvm` + lint + Spotless + `kotlin-test` |
| `stit.screenshot` | `ScreenshotConventionPlugin` | Roborazzi + Robolectric + `@GraphicsMode`-дефолты, `separateOutputDirs`, `pixelCopyRenderMode=hardware`, `--add-opens` |
| `stit.root` | `RootConventionPlugin` | detekt на корне, Kover-агрегация, `graphUpdate`/`graphCheck`, Spotless на корне |

Черновик «сердца» — `KotlinAndroid.kt` (AGP-9-совместимый):

```kotlin
internal fun Project.configureKotlinAndroid(commonExtension: CommonExtension) {
    commonExtension.apply {
        compileSdk = libs.findVersion("compileSdk").get().toString().toInt()
        defaultConfig.apply { minSdk = libs.findVersion("minSdk").get().toString().toInt() }
        compileOptions.apply {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
            isCoreLibraryDesugaringEnabled = true
        }
        testOptions.unitTests.apply {
            isIncludeAndroidResources = true          // обязателен для Robolectric
            isReturnDefaultValues = true
            all {
                // Robolectric на JDK 17+ иначе падает InaccessibleObjectException
                it.jvmArgs(
                    "--add-opens=java.base/java.lang=ALL-UNNAMED",
                    "--add-opens=java.base/java.util=ALL-UNNAMED",
                    "--add-opens=java.base/java.io=ALL-UNNAMED",
                    "--add-opens=java.base/java.net=ALL-UNNAMED",
                    "--add-opens=java.base/java.security=ALL-UNNAMED",
                    "--add-opens=java.base/java.text=ALL-UNNAMED",
                    "--add-opens=java.base/jdk.internal.access=ALL-UNNAMED",
                    "--add-opens=java.desktop/java.awt.font=ALL-UNNAMED",
                    "--add-opens=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
                )
                it.systemProperties["robolectric.pixelCopyRenderMode"] = "hardware"
                it.maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
            }
        }
    }

    extensions.configure<JavaPluginExtension> {
        toolchain { languageVersion = JavaLanguageVersion.of(21) }   // Robolectric SDK 36/37
    }

    extensions.configure<KotlinAndroidProjectExtension> {
        compilerOptions {
            allWarningsAsErrors.set(
                providers.gradleProperty("warningsAsErrors").map(String::toBoolean).orElse(true)
            )
            freeCompilerArgs.addAll("-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi")
        }
    }

    dependencies { "coreLibraryDesugaring"(libs.findLibrary("android.desugarJdkLibs").get()) }
}
```

Минимальный `build.gradle.kts` фича-модуля:

```kotlin
plugins {
    alias(libs.plugins.stit.android.feature)
    alias(libs.plugins.stit.android.library.compose)
}
android { namespace = "tech.stit.template.feature.todos" }
dependencies {
    implementation(projects.core.data)
    implementation(projects.core.model)
    testImplementation(projects.core.testing)
}
```

### R4. Черновик `detekt.yml` — аналог forbidigo

```yaml
build:
  maxIssues: 0

config:
  validation: true
  warningsAsErrors: true

naming:
  FunctionNaming:
    # Composable-функции — PascalCase
    ignoreAnnotated: ['Composable']

style:
  # ⚠️ ключ именно `forbiddenImports` (не `imports`) — сверено с
  # detekt-core/src/main/resources/default-detekt-config.yml
  ForbiddenImport:
    active: true
    forbiddenImports:
      - 'android.util.Log'                # логи — через Logger из core:common
      - 'java.util.Date'                  # время — kotlinx.datetime
      - 'java.text.SimpleDateFormat'      # не thread-safe
      - 'kotlinx.coroutines.GlobalScope'  # не привязан к lifecycle
      - 'androidx.compose.material.*'     # Material 2 запрещён — только material3
      - 'androidx.compose.ui.test.junit4.createComposeRule'  # v1 deprecated → …junit4.v2
    allowedImports: []

  ForbiddenMethodCall:
    active: true
    methods:
      - reason: 'Логи — через Logger из core:common'
        value: 'kotlin.io.println'
      - reason: 'Логи — через Logger из core:common'
        value: 'kotlin.io.print'
      - reason: 'System.out.println — тот же запрет, но на уровне PrintStream'
        value: 'java.io.PrintStream.println'
      - reason: 'Сообщения пользователю — через UiState.userMessages + Snackbar'
        value: 'android.widget.Toast.makeText'
      - reason: 'runBlocking блокирует поток; в тестах — runTest'
        value: 'kotlinx.coroutines.runBlocking'
      - reason: 'Время — через инжектируемый Clock (тестируемость)'
        value: 'java.lang.System.currentTimeMillis'

  ForbiddenComment:
    active: true
    comments:
      - reason: 'FIXME должен стать строкой в .claude/plans/roadmap.md'
        value: 'FIXME:'
      - reason: 'STOPSHIP нельзя мёржить в main'
        value: 'STOPSHIP:'

  # запрет обходить запреты через @Suppress
  ForbiddenSuppress:
    active: true
    rules:
      - 'ForbiddenImport'
      - 'ForbiddenMethodCall'

  MaxLineLength:
    maxLineLength: 120

complexity:
  LongMethod:
    threshold: 60
    ignoreAnnotated: ['Composable', 'Preview']
  TooManyFunctions:
    ignoreAnnotated: ['Composable']

# Compose-правила (io.nlopez.compose.rules)
Compose:
  ComposableAnnotationNaming: { active: true }
  ComposableNaming:           { active: true }
  ComposableParamOrder:       { active: true }
  ContentEmitterReturningValues: { active: true }
  ModifierComposable:         { active: true }
  ModifierMissing:            { active: true }
  ModifierNaming:             { active: true }
  ModifierNotUsedAtRoot:      { active: true }
  ModifierReused:             { active: true }
  ModifierWithoutDefault:     { active: true }
  MultipleEmitters:           { active: true }
  MutableParams:              { active: true }
  ComposableParamOrder:       { active: true }
  PreviewAnnotationNaming:    { active: true }
  PreviewPublic:              { active: true }
  RememberMissing:            { active: true }
  UnstableCollections:        { active: true }   # List → ImmutableList
  ViewModelForwarding:        { active: true }   # не прокидывать VM вглубь
  ViewModelInjection:         { active: true }   # VM только на уровне экрана
```

Подключение (черновик, корневой `build.gradle.kts`):

```kotlin
detekt {
    buildUponDefaultConfig = true
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    parallel = true
    autoCorrect = false            // автофикс — задача Spotless, детерминизм важнее
}
dependencies {
    detektPlugins(libs.detekt.formatting)
    detektPlugins(libs.detekt.compose.rules)
}
```

⚠️ Точный перечень правил `io.nlopez.compose.rules` **надо свериться** с
`mrmans0n.github.io/compose-rules/rules/` при реализации — выше перечислены ключевые
[предположение в части полноты].

### R5. Черновик Spotless, Lint, Kover

```kotlin
// Spotless (в convention-плагине)
spotless {
    kotlin {
        target("src/**/*.kt")
        targetExclude("**/build/**/*.kt")
        ktlint(libs.versions.ktlint.get()).editorConfigOverride(mapOf("android" to "true"))
        licenseHeaderFile(rootProject.file("config/spotless/copyright.kt"))
    }
    kotlinGradle {
        target("*.kts")
        ktlint(libs.versions.ktlint.get())
        licenseHeaderFile(rootProject.file("config/spotless/copyright.kts"), "(^(?![\\/ ]\\*).*$)")
    }
    format("xml") {
        target("src/**/*.xml")
        targetExclude("**/build/**/*.xml")
        licenseHeaderFile(rootProject.file("config/spotless/copyright.xml"), "(<[^!?])")
    }
}

// Android Lint (в stit.android.lint)
private fun Lint.configure() {
    warningsAsErrors = true
    abortOnError = true
    checkDependencies = true
    sarifReport = true
    xmlReport = true
    baseline = file("lint-baseline.xml")   // политика: baseline пустой в шаблоне, растёт только осознанно
    disable += "GradleDependency"          // версии ведёт Renovate, не Lint
}

// Kover (корневой build.gradle.kts)
dependencies {
    kover(projects.core.data)
    kover(projects.core.domain)
    kover(projects.core.network)
    kover(projects.feature.auth)
    kover(projects.feature.profile)
    kover(projects.feature.todos)
    kover(projects.feature.settings)
}
kover {
    reports {
        filters {
            excludes {
                classes(
                    "*_Factory", "*_HiltModules*", "*Hilt_*", "*ComposableSingletons*",
                    "*.BuildConfig", "*.R", "*.R$*", "*_GeneratedInjector",
                )
                annotatedBy("androidx.compose.ui.tooling.preview.Preview")
            }
        }
        verify { rule { minBound(75) } }    // floor 75 %, как cover-check в go-backend-template
    }
}
```

### R6. Черновик архитектурных тестов (Konsist)

Модуль `:core:testing` или отдельный `:architecture-test`:

```kotlin
class ArchitectureTest {

    @Test fun `ViewModels live in feature modules and are named XxxViewModel`() {
        Konsist.scopeFromProduction()
            .classes()
            .withAllParentsOf(ViewModel::class)
            .assertTrue { it.name.endsWith("ViewModel") && it.resideInPackage("..feature..") }
    }

    @Test fun `every ViewModel has a matching test`() {
        Konsist.scopeFromProduction()
            .classes().withNameEndingWith("ViewModel")
            .assertTrue { vm ->
                Konsist.scopeFromTest().classes().any { it.name == "${vm.name}Test" }
            }
    }

    @Test fun `UiState types are immutable data classes or sealed interfaces`() {
        Konsist.scopeFromProduction()
            .classesAndInterfaces().withNameEndingWith("UiState")
            .assertTrue { decl ->
                decl is KoInterfaceDeclaration ||
                    (decl.hasDataModifier && decl.properties().all { it.hasValModifier })
            }
    }

    @Test fun `repositories are interfaces, implementations are prefixed`() {
        Konsist.scopeFromPackage("..core.data..")
            .interfaces().withNameEndingWith("Repository")
            .assertTrue { it.hasPublicOrDefaultModifier }
    }

    @Test fun `no feature imports another feature`() {
        Konsist.scopeFromProduction().files
            .filter { it.packagee?.name?.contains(".feature.") == true }
            .assertTrue { file ->
                val own = Regex("""\.feature\.([a-z]+)""").find(file.packagee!!.name)?.groupValues?.get(1)
                file.imports.none { imp ->
                    val other = Regex("""\.feature\.([a-z]+)""").find(imp.name)?.groupValues?.get(1)
                    other != null && other != own
                }
            }
    }

    @Test fun `composable screens are stateless — no ViewModel parameter except Route`() {
        Konsist.scopeFromProduction()
            .functions().withNameEndingWith("Screen").withAnnotationOf(Composable::class)
            .assertTrue { fn -> fn.parameters.none { it.type.name.endsWith("ViewModel") } }
    }

    @Test fun `data layer does not depend on Android UI`() {
        Konsist.scopeFromPackage("..core.data..")
            .files.assertTrue { f -> f.imports.none { it.name.startsWith("androidx.compose") } }
    }
}
```

⚠️ Листинги написаны по публичному API Konsist и **не прогонялись** — конкретные имена методов
(`withAllParentsOf`, `classesAndInterfaces`, `hasDataModifier`) надо сверить с 0.17.3.
**Перед тем как брать Konsist, оценить альтернативу**: три из семи правил выше (cross-feature
импорт, зависимость data от Compose, размещение ViewModel) закрываются **графом модулей и
convention-плагином** бесплатно и надёжнее.

### R7. Гейт `make check` — цепочка и оценка времени

```makefile
check: ## Канонический gate (pre-push)
	@$(MAKE) fmt-check        # ./gradlew spotlessCheck checkSortDependencies
	@$(MAKE) lint             # ./gradlew detektMain detektTest lintRelease
	@$(MAKE) test             # ./gradlew testDebugUnitTest
	@$(MAKE) screenshot-verify# ./gradlew verifyRoborazziDebug
	@$(MAKE) arch             # ./gradlew :architecture-test:test
	@$(MAKE) cover-check      # ./gradlew koverVerify
	@$(MAKE) build            # ./gradlew assembleDebug assembleRelease  (R8!)
	@$(MAKE) deps-check       # ./gradlew buildHealth
```

| Шаг | Gradle-таск | Что ловит | Оценка (тёплый кеш) [предположение] |
|---|---|---|---|
| 1 | `spotlessCheck` + `checkSortDependencies` | формат Kotlin/KTS/XML, лицензии, порядок зависимостей | 5–20 с |
| 2 | **`detektMain detektTest`** (с type resolution) | стиль, сложность, forbidden API, Compose-правила | 30–90 с (голый `detekt` без резолва — 10–30 с) |
| 3 | `lintRelease` (`checkDependencies = true`) | Android API, ресурсы, a11y, свои `Detector`'ы | 1–3 мин |
| 4 | `testDebugUnitTest` | unit + Robolectric + Compose | 1–4 мин |
| 5 | `verifyRoborazziDebug` | screenshot-регрессии | 1–3 мин |
| 6 | `:architecture-test:test` | Konsist/ArchUnit-правила | 20–60 с |
| 7 | `koverVerify` | покрытие ≥ 75 % | переиспользует шаг 4 |
| 8 | `assembleDebug assembleRelease` | компиляция + **R8 full mode** | 2–6 мин |
| 9 | `buildHealth` | лишние/недообъявленные зависимости, `api` vs `implementation` | 30–90 с |

⚠️ Важно: **`detekt` и `detektMain`/`detektTest` — разные таски**. Первый работает без type
resolution (быстро, но упускает межфайловые и типовые проверки), в гейт идут именно
`detektMain detektTest`. Android даёт ещё вариант-специфичные `detekt<Variant>`. [проверено, §8.9]

Итого **порядка 8–20 минут** на холодную и 3–8 на тёплую на нормальной машине
[предположение; на нашей нагруженной Debian-машине — кратно больше, мерить в потоке 06].
Это **существенно дольше** `pnpm check` (десятки секунд) и `make check` Go (минуты) — что само
по себе проектное ограничение: Android-цикл медленнее, и надо предусмотреть быстрый под-набор.

**pre-commit** (быстрый, < 30 с): `spotlessApply` на изменённых файлах + `detekt` на них же.
**pre-push**: полный `make check`.
**Вне гейта** (отдельные глаголы): `make test-e2e` (Maestro), `make baseline-profile`,
`make screenshot-record`, `make bench`.

Черновик глаголов `Makefile`:

| Глагол | Что делает |
|---|---|
| `make setup` | `android sdk install …`, git-хуки, `android skills add --all`, `--no-metrics` в `~/.androidrc` |
| `make dev` | `./gradlew installDebug` + `android run --apks=…` (нужно устройство) |
| `make build` | `assembleDebug assembleRelease` |
| `make test` | `testDebugUnitTest` |
| `make lint` | `detekt lint` |
| `make fmt` / `make fmt-check` | `spotlessApply` / `spotlessCheck` |
| `make screenshot-record` | `recordRoborazziDebug` (осознанно, вне гейта) |
| `make screenshot-verify` | `verifyRoborazziDebug` |
| `make cover-check` | `koverVerify` |
| `make arch` | Konsist-тесты |
| `make deps-check` | `buildHealth` |
| `make check` | цепочка выше |
| `make bundle` | `bundleRelease` + подпись (артефакт AAB — STOP-граница) |
| `make baseline-profile` | `generateBaselineProfile` (нужен девайс/эмулятор) |
| `make e2e` | `maestro test flows/` (нужен девайс) |
| `make status` / `make map` | `android describe`, граф модулей |

### R8. Правила `.claude/rules` (path-scoped)

| Файл | `paths:` | Ключевые инварианты |
|---|---|---|
| `module-architecture.md` | `**/feature/**`, `**/core/**` | слои `feature → core:data → core:network/datastore`; **feature не импортит feature**; `core:model`/`core:common`/`core:domain` — JVM-модули; use-case только по трём критериям (§2.2); модели по слоям; naming (`*Screen`/`*Route`/`*ViewModel`/`*UiState`/`*Repository`/`*DataSource`/`Fake*`) |
| `compose-ui.md` | `**/*.kt` с Composable | stateless `<X>Screen` + stateful обёртка; ViewModel только на уровне экрана; `collectAsStateWithLifecycle()`; **никаких one-off events из VM** — только состояние; immutable `UiState` из `val`, коллекции — `ImmutableList`; никаких ручных `remember{{}}` вокруг лямбд (strong skipping); `@Preview` на каждый экран и компонент |
| `design-tokens.md` | `core/designsystem/**`, `**/ui/**` | только `MaterialTheme.*` и `AppTheme.*`; запрет `Color(0x…)`, сырых `.dp` вне designsystem; Material 2 (`androidx.compose.material.*`) запрещён; иконки — Material Symbols в `res/drawable`, **не** `material-icons-*`; dynamic color выключен по умолчанию |
| `navigation.md` | `**/navigation/**`, `app/**` | Navigation 3; ключ — `@Serializable data class/object : NavKey`; **в аргументах только идентификаторы, не объекты**; `rememberNavBackStack` (переживает смерть процесса); back-обработка — `BackHandler`/`PredictiveBackHandler`, `onBackPressed()` запрещён |
| `tests-pipeline.md` | `**/test/**`, `**/androidTest/**` | JUnit 4 + `AndroidJUnit4`; **fakes, не mocks**; Turbine для Flow; `MainDispatcherRule`; **Compose-тесты только на v2-импортах**; screenshot — Roborazzi, эталоны пишет `make screenshot-record`; что обязано иметь тест (таблица §7.12); `kspTest` для Hilt |
| `gradle-build.md` | `**/build.gradle.kts`, `build-logic/**`, `gradle/**` | версии **только** в `libs.versions.toml`; модуль подключает `stit.*`-convention-плагин, а не сырые; **`kotlin-android` не применять** (AGP 9); `kapt` запрещён; `targetSdk` задавать явно; новый variant API |
| `env-local.md` | `**` | `local.properties` не коммитим; keystore и пароли — вне репо; runtime-конфиг сред (dev/staging/prod) — build-типы/flavors + `BuildConfig`, секретов в APK нет |
| `comments.md` | `**` | только WHY, на русском, однострочные (переносится из существующих шаблонов) |
| `forward-only.md` | `**` | без изменений (переносится) |

### R9. Что положить в `CLAUDE.md` шаблона (инварианты-ядро)

Коротко, ≤ 200 строк, всё остальное — роутером в `docs/` и `.claude/rules/`:

- **AGP 9: `kotlin-android` не применяется, `android.kotlinOptions{}` нет, `kapt` нет.** Это
  первое, что должен знать агент — иначе он напишет нерабочий `build.gradle.kts` из памяти.
- **Navigation 3, не `NavHost`.** Второе по частоте ошибок из памяти.
- **Compose-тесты — `androidx.compose.ui.test.junit4.v2`**, не v1.
- **Material 3 (1.4.0), не Material 2; Expressive не берём** (только в 1.5.0-alpha).
- **Иконки — `res/drawable` (Material Symbols), не `material-icons-extended`.**
- Состояние: `ViewModel` + `StateFlow<UiState>` + `collectAsStateWithLifecycle()`; **события из
  VM наружу не шлём**.
- Логи — свой `Logger` из `core:common`, не `android.util.Log`/`println` (detekt).
- Токены — `MaterialTheme`/`AppTheme`, не сырые цвета/размеры (Lint).
- **Feature не импортит feature.** Общее — вверх в `core:*` или композиция в `:app`.
- Fakes, не mocks. `core:testing` — единственное место фейков.
- Перед claim «готово» — **`make check`**.
- Изменение — через план (`.claude/plans/roadmap.md`).
- Платформенные how-to — через **официальные Android skills** (`android skills add …`) и
  `android docs search`, не по памяти модели.
- Не коммитить: `local.properties`, `*.jks`, `*.keystore`, `google-services.json` с продовыми
  ключами, `.env`.

### R10. Reference-фичи (зеркало SPA-шаблона)

| Фича | Что демонстрирует |
|---|---|
| `feature:auth` (login) | форма + валидация, cookie-auth с Go-бэкендом (`CookieJar` в OkHttp), обработка ошибок в `UiState`, тест ViewModel + screenshot формы, Hilt-граф |
| `feature:profile` | загрузка данных, смена пароля, `stateIn(WhileSubscribed)`, pull-to-refresh, adaptive-layout list/detail |
| `feature:todos` | CRUD-список, Paging или простой список, оптимистичные обновления, пустое/ошибочное/загрузочное состояния, deep link на элемент |
| `feature:settings` | тема (light/dark/system) + язык (ru/en) через DataStore, `AppCompatDelegate.setApplicationLocales`, тумблер dynamic color |

Плюс `core:designsystem` с каталогом компонентов и screenshot-тестами на каждый (аналог
`app-nia-catalog`, но не отдельным приложением, а набором превью).
Удаляются `/de-template`-скиллом — ровно как reference-слайсы фронта.

### R11. Что проверяется на Linux **без устройства** (для потока 06)

| Можно headless | Требует устройства/эмулятора |
|---|---|
| `spotlessCheck` / `spotlessApply` | `connectedDebugAndroidTest` (Espresso, UIAutomator) |
| `detekt`, `lint`, свои lint-`Detector`'ы (+ `lint-tests`) | Gradle Managed Devices (нужен `/dev/kvm`, иначе минуты→десятки минут) |
| `testDebugUnitTest` (JUnit + Robolectric + Compose + Hilt) | `generateBaselineProfile`, Macrobenchmark |
| `verifyRoborazziDebug` / `recordRoborazziDebug` | Maestro e2e |
| `koverVerify` | ручная проверка UI, `android screen capture` |
| Konsist / ArchUnit | `android studio …` подкоманды (нужна запущенная IDE) |
| `assembleDebug`, `assembleRelease` (R8), `bundleRelease` | установка/запуск приложения |
| `buildHealth`, `dependencies`, dependency locking | |
| `android create`, `android sdk`, `android docs search`, `android describe`, `android skills` | `android emulator …`, `android run` |

**Это и есть главный вывод по agent-friendliness Android**: полноценный цикл
«написал → проверил → починил» закрывается **полностью headless**, включая визуальную регрессию
(Roborazzi). Единственное, что агент не может без железа, — реальный e2e и бенчмарки, и это
ровно та граница, которую в шаблоне надо явно обозначить.


## Ловушки

Каждая — с источником и версией. Разбиты по фазам, в которых бьют.

### Сборка и AGP 9

| # | Ловушка | Симптом / деталь | Источник |
|---|---|---|---|
| 1 | Применён `org.jetbrains.kotlin.android` | `Failed to apply plugin 'org.jetbrains.kotlin.android'. Cannot add extension with name 'kotlin', as there is an extension already registered with that name` | `developer.android.com/build/migrate-to-built-in-kotlin` |
| 2 | `android { kotlinOptions { } }` | блока больше нет → `kotlin { compilerOptions { } }`; `jvmTarget` наследуется из `compileOptions.targetCompatibility` | там же |
| 3 | `kotlin-kapt` | несовместим с built-in Kotlin; только KSP либо `com.android.legacy-kapt` той же версии, что AGP | там же |
| 4 | Старый variant API | `applicationVariants.all`, `variantFilter`, `deviceProvider`, `testServer`, `sdkDirectory` — **удалены**; `ClassCastException: … cannot be cast to class com.android.build.gradle.BaseExtension` | AGP 9.0 release notes |
| 5 | `CommonExtension<*,*,*,*,*,*>` в convention-плагине | параметризация убрана; и `defaultConfig { }` → `defaultConfig.apply { }` | там же |
| 6 | **`targetSdk` не задан явно** | `android.sdk.defaultTargetSdkToCompileSdkIfUnset` стал `true` — раньше падал до `minSdk`, теперь до `compileSdk`. Молча меняет соответствие требованиям Play | там же |
| 7 | `resValues` / шейдеры перестали работать | `android.defaults.buildfeatures.resvalues` и `.shaders` сменили дефолт `true → false` | там же |
| 8 | `proguard-android.txt` | запрещён (`android.r8.proguardAndroidTxt.disallowed=true`) → только `proguard-android-optimize.txt` | там же |
| 9 | `-dontobfuscate`/`-dontoptimize` в consumer-rules библиотеки | публикация падает (`android.r8.globalOptionsInConsumerRules.disallowed=true`) | там же |
| 10 | `switch` по `R.id.*` | `android.enableAppCompileTimeRClass=true` → R-константы не `final` | там же |
| 11 | Opt-out `android.builtInKotlin=false` / `android.newDsl=false` | исчезнет в **AGP 10** | там же |
| 12 | Несовпадение версий AGP и `com.android.tools:*` | `lint-api`/`common` = AGP + 23 мажора (AGP 9.4.0 ↔ **32.4.0**); NiA держит их одной строкой с комментарием «AGP and tools should be updated together» | Google Maven + NiA |
| 13 | В `build-logic` подключён `com.android.tools.build:gradle` | правильный артефакт — **`gradle-api`** (важно для configuration cache / isolated projects) | NiA `build-logic/convention/build.gradle.kts` |
| 14 | Kotlin 2.4 на старом R8 | Kotlin 2.4 требует AGP ≥ 8.5.2 и **R8 9.1.29**; «9.x versions before 9.0.28 don't support Kotlin 2.3» | `developer.android.com/build/kotlin-support` |
| 15 | Configuration cache «по умолчанию» | в Gradle 9 **не включён**; «will be enabled by default in Gradle 10» | `docs.gradle.org/current/userguide/configuration_cache.html` |

### Тесты

| # | Ловушка | Деталь | Источник |
|---|---|---|---|
| 16 | Robolectric на JDK 17+ без `--add-opens` | `InaccessibleObjectException` на ровном месте — **самый частый «у меня не собирается»** | `robolectric.org/getting-started/` |
| 17 | `isIncludeAndroidResources = false` | нет merged-ресурсов/манифеста → Robolectric не видит ресурсы | там же |
| 18 | Нет `@Config(sdk=…)` при `targetSdk 36/37` | Robolectric берёт targetSdk и **требует JDK 21** | `robolectric.org/compatibility_table/` |
| 19 | Robolectric качает `android-all` jar из сети | первый прогон висит/падает без сети; лечится `robolectric.offline` + `robolectric.dependency.dir` | `robolectric.org/configuring/` |
| 20 | **Compose test v1 API** | `runComposeUiTest`, `createComposeRule` из `…test.junit4` — **deprecated**; v2 дефолтит `StandardTestDispatcher`, и тесты, полагавшиеся на немедленное выполнение, **падают** | `developer.android.com/develop/ui/compose/testing/migrate-v2` |
| 21 | `ui-test-manifest` не на `debugImplementation` | `ActivityNotFoundException` в `createComposeRule()`; есть lint-чек `TestManifestGradleConfiguration` | androidx |
| 22 | Hilt на Robolectric с `kspAndroidTest` | нужен **`kspTest`**; иначе «Did you forget to apply the @HiltAndroidTest annotation?» | `developer.android.com/training/dependency-injection/hilt-testing` |
| 23 | Чтение `uiState.value` без подписчика | `stateIn(…, WhileSubscribed(5000), …)` не запускает upstream → только initial value. Читать через Turbine | контракт `SharingStarted` |
| 24 | Roborazzi + Gradle 9, несколько тасков в одном прогоне | hard-fail `Cannot access input property 'roborazziImageInput'` → `separateOutputDirs.set(true)` (меняет расположение эталонов!) | Roborazzi README, issue #830 |
| 25 | Ставка на официальный screenshot-плагин | `com.android.compose.screenshot` — **alpha16 и deprecated**, преемник требует AGP 9.5-alpha | `developer.android.com/studio/preview/compose-screenshot-testing` |
| 26 | Смена screenshot-инструмента | эталоны **не переносимы**: layoutlib (Paparazzi/официальный) ≠ Robolectric Native Graphics (Roborazzi) | Roborazzi README |
| 27 | GMD с `aosp-atd` на API ≠ 30 | «ATDs currently support only API level 30»; плюс «Screenshot tests depending on hardware rendering are not supported with ATDs» | `developer.android.com/studio/test/managed-devices` |
| 28 | Эмулятор без `/dev/kvm` | «your virtual machine is slower without the KVM extensions» — практически десятки минут на загрузку | `developer.android.com/studio/run/emulator-acceleration` |
| 29 | Kover < 0.9.4 на AGP 9 | поддержка AGP 9 добавлена в **0.9.4**, стабилизирована к 0.9.9 | Kover CHANGELOG |
| 30 | Ожидание покрытия инструментальных тестов от Kover | «instrumentation tests executing on the Android device are not supported yet» | Kover docs |
| 31 | Kover/JaCoCo и offline | подтягиваются из `mavenCentral()` в рантайме сборки | там же |
| 32 | Maestro и свежие API | дока на 2026-09-17 всё ещё пишет «API 35 and 36 support is arriving in Q2 2026» — противоречие не разрешено | `docs.maestro.dev` |
| 33 | `@FlakyTest` как «ретрай» | ничего не ретраит — это фильтр для `am instrument -e notAnnotation`, instrumentation-only | `developer.android.com/reference/androidx/test/filters/FlakyTest` |
| 34 | Robolectric + Compose: скрытые ограничения | accessibility-чеки — **no-op**; `Thread.sleep` десинхронизирован с `MainTestClock`; бесконечные анимации при `autoAdvance = true` → `CancellationException`; нельзя смешивать `runComposeUiTest{}` и `ComposeTestRule` | androidx/CORPUS |
| 35 | Baseline Profile с включённой минификацией | генерирующий вариант обязан быть `isMinifyEnabled = false` (+`-dontobfuscate`, `-dontoptimize`), релиз — `true` | `…/baselineprofiles/create-baselineprofile` |

### Compose и UI

| # | Ловушка | Деталь |
|---|---|---|
| 36 | `List`/`Set`/`Map` в `UiState` | Compose **всегда** считает их нестабильными → `ImmutableList` из `kotlinx-collections-immutable` либо `@Immutable` |
| 37 | Ручные `remember { { … } }` вокруг лямбд | **strong skipping по умолчанию с Kotlin 2.0.20** — компилятор мемоизирует сам; ручное — карго-культ |
| 38 | `material-icons-extended` | библиотека **прекращена** и убрана из material3 1.4.0; «can increase the build time of your apps significantly» → Material Symbols в `res/drawable` |
| 39 | Material 3 Expressive | **в stable нет**: вырезан из 1.4.x в beta01 (2025-07-30), живёт только в 1.5.0-alphaNN, где API ломается между альфами (`Slider` → `SliderState`, удалён `DropdownMenuItem`, …) |
| 40 | `setStatusBarColor` / `setNavigationBarColor` | при target ≥ 35 **deprecated и не действуют** (для gesture nav); цвет системных баров задаётся контентом + insets |
| 41 | `onBackPressed()` / `KEYCODE_BACK` | при target 36+ `onBackPressed` **не вызывается**, `KEYCODE_BACK` не диспатчится; «intercepting back events from `KeyEvent.KEYCODE_BACK` is no longer supported» |
| 42 | `android:screenOrientation` / `resizeableActivity=false` | при target 36 игнорируются на sw ≥ 600dp; при target 37 **opt-out удалён** → больше пересозданий Activity, обязателен `SavedStateHandle`/`rememberSaveable` |

### Платформа и сторы

| # | Ловушка | Деталь |
|---|---|---|
| 43 | targetSdk 35 | **с 31.08.2026 Play не принимает** новые приложения и обновления ниже API 36 (продление до 01.11.2026) |
| 44 | 16 KB page size | обязателен с **01.02.2027** для приложений с targetSdk ≥ 35; Kotlin-only проходит автоматически, **но весь граф зависимостей** (карты, ML Kit, SQLCipher, Sentry NDK) — нет |
| 45 | R8 «включим потом» | с **02.2027** Play требует DEX Code Optimization ≥ 25 % для приложений с DEX > 10 МБ |
| 46 | Restore Credentials | станет требованием Play в **04.2027**; упирается в серверную часть (общая с passkeys/WebAuthn) |
| 47 | Developer verification | регионально с **30.09.2026**, глобально в 2027; D-U-N-S и регистрация — организационный процесс на недели |
| 48 | `generateLocaleConfig = true` + свой `locale_config.xml` | **сборка падает** |
| 49 | Отсутствие `localeFilters` | APK тащит десятки локалей из AppCompat/Play Services |
| 50 | Per-app language ниже API 33 | `AppCompatDelegate.setApplicationLocales` работает «with the `AppCompatActivity` context, **not the application context**» → Compose-only приложению нужен AppCompat при `minSdk < 33` |
| 51 | FGS `dataSync` | Android 15: **6 часов за 24**, дальше `onTimeout()` и пара секунд на `stopSelf()`, иначе фатальный `RemoteServiceException`; Android 14: без типа FGS — `MissingForegroundServiceTypeException` |
| 52 | Implicit URI grants | исчезнут в **Android 18** для `ACTION_SEND`/`ACTION_IMAGE_CAPTURE` → добавлять `FLAG_GRANT_READ_URI_PERMISSION` уже сейчас; детектор — `StrictMode…detectImplicitUriPermissionGrant()` |
| 53 | App Links не верифицируются | нужен `assetlinks.json` под HTTPS, `Content-Type: application/json`, **без редиректов**; ждать ≥ 20 с после установки; на Android 11 и ниже — нужен файл для **всех** хостов манифеста |
| 54 | OTP по SMS | при target 37 обычные SMS с кодом **задержаны на 3 часа** → только SMS Retriever / SMS User Consent API |

### Экосистема и источники

| # | Ловушка | Деталь |
|---|---|---|
| 55 | `chrisbanes/tivi` как образец | **архивирован**, последний коммит 2024-11-12, README: «Tivi has now deprecated» |
| 56 | `android/architecture-samples` как «канон 2026» | последний коммит в `main` — 2025-07-17; Kotlin 2.1 / AGP 8.7 / compileSdk 35 |
| 57 | Копирование `libs.versions.toml` из NiA | у них **`compose-bom-alpha`** и точечные alpha/rc-пины (`androidxComposeFoundation = 1.8.0-alpha07`, `androidxMacroBenchmark = 1.5.0-alpha01`) |
| 58 | Копирование convention-плагинов NiA кусками | они написаны под **isolated projects** (`isolated.rootProject`, `@Inject BuildFeatures`) — копировать целиком или переписывать |
| 59 | Konsist | последний релиз **0.17.3 от 2024-12-08**; работает (element-x на Kotlin 2.4.10), но не развивается |
| 60 | detekt 2.0 | группа `dev.detekt` — только **alpha**; stable — старая `io.gitlab.arturbosch.detekt` **1.23.8** (заморожена с 2025-02) |
| 61 | Документация Kover отстаёт | сайт показывает 0.9.8 при релизе 0.9.9 |
| 62 | Официальная дока Hilt отстаёт | сниппеты показывают 2.57.1 при релизе 2.60.1 |
| 63 | `developer.android.com` под `curl` | уходит в 302-петлю на `/oauth2authorize`; обход — cookie `signin_details=x; signin=autosignin` |
| 64 | `api.github.com` | легко ловит `403 rate limit` — использовать `raw.githubusercontent.com`, `releases.atom`, `tags.atom` |
| 65 | **compose-rules ≥ 0.5.0 на detekt 1.23.x** | не заработает: 0.5.0+ требует detekt 2.0.0-alpha.1+; потолок для 1.23.x — **0.4.28** |
| 66 | **ktlint сменил владельца и groupId** | Pinterest прекратил поддержку; `com.pinterest.ktlint` заканчивается на 1.8.0, продолжение — `io.github.ktlint.core`, но там только `2.0.0-ALPHA-4` |
| 67 | `detekt-formatting` в detekt 2.0 | переименован в **`detekt-rules-ktlint-wrapper`**; вместе со сменой groupId это значит, что Renovate апгрейд **не подхватит** — bump руками |
| 68 | Ключ `imports:` в `ForbiddenImport` | правильный ключ — **`forbiddenImports`** (сверено с `default-detekt-config.yml`); `System.out` как field-access вообще не ловится — нужен `java.io.PrintStream.println` в `ForbiddenMethodCall` |
| 69 | `checkOnly` в `lint { }` | **заменяет** весь набор проверок, а не добавляет к нему |
| 70 | R8 full mode (дефолт с AGP 8.0) | не сохраняет неявно: атрибуты рефлексии, **конструктор по умолчанию**, Kotlin-метаданные; меняет видимость методов/полей → нужен минимум `-keep class kotlin.Metadata { *; }` + `-keepattributes RuntimeVisibleAnnotations` |
| 71 | `gradle-doctor` на Gradle 9 | PR #440 «Update Gradle to v9» на 2026-09-15 **открыт**; апстрим на CI держит Gradle 8.14.5 |
| 72 | Бесплатный Develocity Build Cache Node | «no further distribution, support, or updates **after December 31, 2026**» → планировать замену заранее |
| 73 | Своя lint-проверка не подхватывается | забыт файл `META-INF/services/com.android.tools.lint.client.api.IssueRegistry` с FQN класса реестра |

## Риски и открытые вопросы владельцу

### Решения, которые должен принять владелец

1. **`minSdk`.** Предлагается **26** (96.1 % глобально). Альтернативы: 28 (93.5 %, меньше legacy-кода),
   29 (91.1 %, только версии с security-патчами), 23 (98 %, как NiA, но тянет desugaring и старые
   ветки). **Данных по РФ нет** — Google убрал распределение версий с публичного dashboard,
   Statcounter по РФ вытянуть не удалось. Корректный путь — Play Console → Reach and devices по
   существующему приложению (поток 05).
2. **`targetSdk` 36 или 37.** 36 — минимум Play прямо сейчас; 37 — задел на 2027 ценой
   немедленного принятия adaptive-требований и `ACCESS_LOCAL_NETWORK`. Рекомендация — 37.
3. **Konsist: брать или нет.** Последний релиз почти два года назад, но живой продовый
   пользователь есть. Альтернатива — проверки в convention-плагинах + Android Lint (быстрее и
   надёжнее, но покрывает не всё). Третий вариант — ArchUnit 1.5.0 (активен, но Java-центричен).
4. **Retrofit vs Ktor.** Решается **вместе с потоком 03**: если есть горизонт KMP — Ktor;
   если Android-only — Retrofit 3.
5. **Модульность на старте**: один модуль на фичу (предложение) vs `api`/`impl`-раскол сразу
   (как NiA/element-x). Раскол даёт дисциплину и параллельную сборку, но удваивает число модулей
   на медленной машине.
6. **Dependency locking** (`LockMode.STRICT`, как bitwarden) vs Dependency Guard (NiA) vs ничего.
   Первое — самое строгое и ближе всего к `go.sum`, но добавляет шаг в каждый bump версии.
7. **Строгость гейта**: `allWarningsAsErrors = true` и `lint { warningsAsErrors = true }` —
   предлагается включить по умолчанию (NiA держит выключенным). Это сделает bump любой библиотеки
   потенциально ломающим сборку.
8. **Что считать STOP-границей мобильного репо** (вопрос из брифа): предлагаемый ответ —
   **подписанный AAB + mapping-файл + Baseline Profile в артефактах**, дальше загрузка в сторы —
   отдельный процесс (поток 05). Требует решения владельца.

### Что проверить дальше (входы в другие потоки)

- **Поток 06 (тулинг)**: реально ли собрать этот стек на данной машине; сколько занимает
  `make check` при 15 ГБ RAM под нагрузкой; работает ли `android` CLI на Debian 13; действительно
  ли Roborazzi даёт воспроизводимые эталоны между запусками; нужен ли Docker-образ для
  фиксации шрифтов.
- **Поток 07 (контракт с бэкендом)**: cookie-auth через `CookieJar`/`HttpCookies`, ротация
  refresh-токена, OpenAPI-кодоген для Kotlin (или рукописные контракты, как в SPA-шаблоне),
  mock-режим (аналог MSW — `MockWebServer` + сценарии `empty|populated|authenticated|error`),
  **WebAuthn-эндпоинты в Go-бэкенде под Restore Credentials (дедлайн 04.2027)**,
  `assetlinks.json` в `<org>/infra`.
- **Поток 05 (дистрибуция)**: developer verification и D-U-N-S для РФ; требования RuStore к
  targetSdk; доли Android-версий по РФ; push без FCM (RuStore Push / собственный).
- **Поток 03 (сколько кодовых баз)**: если выбирается KMP — пересматриваются Room 2 → Room 3,
  Retrofit → Ktor, Hilt → Metro/kotlin-inject, ViewModel → Circuit/Molecule. То есть **решение
  потока 03 переписывает половину R1**.
- **Поток 08 (дизайн шаблона)**: как поженить `.claude/skills/` с официальными Android skills;
  нужен ли слой-мост для `type: mobile` в `business-strategy-template` (аналог FSD-карты) —
  предлагаемая форма: карта модулей `modules/<surface>/` с kinds `feature`/`core`/`screen`.

### Чего этот отчёт НЕ проверял (честный список)

- Ни один листинг **не прогонялся сборкой** — Android SDK на машине нет (это работа потока 06).
- **Взаимная совместимость** конкретного набора версий из R2 не проверена.
- Полный перечень правил `io.nlopez.compose.rules` — назван по ключевым, не сверен построчно.
- API Konsist 0.17.3 в листингах R6 — написан по публичной документации, не скомпилирован.
- Требования Play на 2027 по targetSdk — **не опубликованы**, экстраполяция помечена как предположение.
- Доли Android-версий по РФ — получить не удалось.
- Статус Appium в 2026 — не исследовался (вне scope).
- Тезис «JaCoCo плохо считает Kotlin inline-функции» — первоисточником не подтверждён.
- Цены Maestro Cloud и существование «Robin» — официальной страницей не подтверждены.
- Совместимость `de.mannodermaus.android-junit` 2.0.1 с AGP 9 — README говорит только «≥ 8.2.0»,
  issue #404 открыт без ответа.


## Сводная таблица версий

Все значения проверены **2026-09-17**. Колонка «Источник»: GM — Google Maven group-index,
MC — Maven Central `maven-metadata.xml`, GPP — Gradle Plugin Portal, DAC — developer.android.com.

### Тулчейн и платформа

| Компонент | Версия | Дата | Лицензия | Источник |
|---|---|---|---|---|
| Kotlin | **2.4.20** | 2026-09-07 | Apache-2.0 | kotlinlang.org/docs/releases.html |
| Kotlin (следующий) | 2.5.0 — план декабрь 2026 | — | Apache-2.0 | там же |
| Android Gradle Plugin | **9.4.0** | сентябрь 2026 | Apache-2.0 | GM `com.android.tools.build:gradle`; DAC |
| `com.android.tools:*` / `lint-api` | **32.4.0** | — | Apache-2.0 | GM |
| Gradle | **9.7.1** | 2026-08-19 | Apache-2.0 | services.gradle.org/versions/current |
| Gradle (min для AGP 9.4) | 9.6.0 | — | — | DAC |
| JDK (min для AGP 9.4) | **17** | — | — | DAC |
| JDK (для Robolectric SDK 36/37) | **21** | — | — | robolectric.org |
| SDK Build Tools | 36.0.0 | — | — | DAC |
| KSP | **2.3.12** | 2026-09-09 | Apache-2.0 | MC |
| Android Studio | **Quail 4 \| 2026.1.4** | 2026-09 | Apache-2.0 | DAC studio/releases |
| Android CLI | latest (релиз 2026-04-16) | — | Google Terms | DAC tools/agents/android-cli |
| Android 17 / API 37 (Cinnamon Bun) | stable | 2026-06-16 | — | DAC, googleblog |
| Android 16 / API 36 (Baklava) | stable | 2025-06-10 | — | DAC |
| Android 16 QPR2 / 36.1 | stable | 2025-12-02 | — | googleblog |
| desugar_jdk_libs | 2.1.5 | — | GPL-2.0-with-classpath-exception | GM |

### AndroidX / Compose

| Артефакт | Stable | Дата | Лицензия | Источник |
|---|---|---|---|---|
| `androidx.compose:compose-bom` | **2026.09.00** | 2026-09-09 | Apache-2.0 | GM |
| compose runtime/ui/foundation/animation | 1.12.1 | 2026-09-09 | Apache-2.0 | GM |
| `compose.material3:material3` | **1.4.0** | 2025-09-24 | Apache-2.0 | GM / DAC |
| `compose.material3:material3` (alpha с Expressive) | 1.5.0-alpha28 | 2026-09-09 | Apache-2.0 | GM |
| `compose.material3.adaptive:*` | 1.3.0 | 2026-09-09 | Apache-2.0 | GM |
| `compose.compiler:compiler` (legacy) | 1.5.15 (заморожен) | 2024 | Apache-2.0 | GM |
| `androidx.activity:activity-compose` | 1.13.0 | 2026-09-09 | Apache-2.0 | GM |
| `androidx.appcompat:appcompat` | 1.8.0 | — | Apache-2.0 | GM |
| `androidx.core:core-ktx` | 1.19.0 | — | Apache-2.0 | GM |
| `androidx.core:core-splashscreen` | 1.2.0 | — | Apache-2.0 | GM |
| `androidx.lifecycle:*` | 2.11.0 | 2026-09-09 | Apache-2.0 | GM |
| `androidx.navigation3:navigation3-runtime/-ui` | **1.1.7** | 2026-08-26 | Apache-2.0 | GM / DAC |
| `androidx.navigation3` 1.0.0 (первый stable) | — | 2025-11-19 | Apache-2.0 | DAC |
| `androidx.navigation:navigation-compose` (Nav2) | 2.10.1 | 2026-09-09 | Apache-2.0 | GM |
| `androidx.navigationevent` | 1.1.2 | 2026-08-12 | Apache-2.0 | GM |
| `androidx.room:room-*` | 2.8.5 | 2026-09-09 | Apache-2.0 | GM |
| `androidx.room3:room3-*` | 3.0.3 | 2026-09-09 | Apache-2.0 | GM / DAC |
| `androidx.sqlite` | 2.7.1 | 2026-09-09 | Apache-2.0 | GM |
| `androidx.datastore:datastore-preferences` | 1.2.1 | 2026-09-09 | Apache-2.0 | GM |
| `androidx.work:work-runtime-ktx` | 2.11.2 | 2026-08-12 | Apache-2.0 | GM |
| `androidx.paging:paging-compose` | 3.5.1 | 2026-08-12 | Apache-2.0 | GM |
| `androidx.hilt:hilt-navigation-compose` / `hilt-work` / `hilt-compiler` | 1.4.0 | 2026-07-01 | Apache-2.0 | GM |
| `androidx.window:window` | 1.5.1 | 2026-06-17 | Apache-2.0 | GM |
| `androidx.credentials:credentials` | 1.6.0 | 2026-07-29 | Apache-2.0 | GM |
| `androidx.benchmark:*` | 1.5.0 | 2026-09-09 | Apache-2.0 | GM |
| `androidx.profileinstaller` | 1.4.1 | 2024-10-02 | Apache-2.0 | GM |
| `androidx.startup:startup-runtime` | 1.2.0 | 2024-09-18 | Apache-2.0 | GM |
| `androidx.tracing:tracing-ktx` | 2.0.2 | — | Apache-2.0 | GM |
| `androidx.annotation:annotation` | 1.10.0 | — | Apache-2.0 | GM |
| `androidx.camera:*` | 1.6.2 | 2026-08-26 | Apache-2.0 | GM |
| `androidx.glance` | 1.2.0 | 2026-08-26 | Apache-2.0 | GM |
| `androidx.media3` | 1.11.1 | 2026-09-10 | Apache-2.0 | GM |

### DI

| Артефакт | Версия | Опубликовано | Лицензия | Источник |
|---|---|---|---|---|
| `com.google.dagger:hilt-android` / `dagger` | **2.60.1** | 2026-07-06 | Apache-2.0 | MC |
| `dev.zacsweers.metro:runtime` | 1.4.3 | 2026-09-08 | Apache-2.0 | MC |
| `io.insert-koin:koin-android` | 4.2.2 | — | Apache-2.0 | MC |
| `me.tatarka.inject:kotlin-inject-runtime` | 0.9.0 | 2026-01-07 | Apache-2.0 | MC |

### Сеть, сериализация, изображения

| Артефакт | Версия | Опубликовано | Лицензия | Источник |
|---|---|---|---|---|
| `com.squareup.retrofit2:retrofit` | **3.0.0** | 2025-05-15 | Apache-2.0 | MC |
| `com.squareup.okhttp3:okhttp` / `mockwebserver3*` | **5.5.0** | 2026-08-16 | Apache-2.0 | MC |
| `io.ktor:ktor-client-*` | 3.6.0 | 2026-09-16 | Apache-2.0 | MC |
| `org.jetbrains.kotlinx:kotlinx-serialization-json` | 1.11.0 | — | Apache-2.0 | MC |
| `org.jetbrains.kotlinx:kotlinx-coroutines-*` | 1.11.0 | 2026-05-08 | Apache-2.0 | MC |
| `org.jetbrains.kotlinx:kotlinx-datetime` | 0.8.0 | — | Apache-2.0 | MC |
| `org.jetbrains.kotlinx:kotlinx-collections-immutable` | 0.5.2 | — | Apache-2.0 | MC |
| `io.coil-kt.coil3:coil-compose` | 3.6.2 | 2026-09-04 | Apache-2.0 | MC |

### Тесты

| Артефакт | Версия | Опубликовано | Лицензия | Источник |
|---|---|---|---|---|
| `junit:junit` | 4.13.2 | — | EPL-1.0 | MC |
| `org.junit:junit-bom` (JUnit 5/6) | 6.1.3 | — | EPL-2.0 | MC |
| `com.google.truth:truth` | 1.4.5 | — | Apache-2.0 | MC |
| `org.robolectric:robolectric` | **4.17** (SDK 23–37) | 2026-09-10 | Apache-2.0 | MC / robolectric.org |
| `app.cash.turbine:turbine` | 1.2.1 | 2025-06-11 | Apache-2.0 | MC |
| `io.mockk:mockk` | 1.14.11 | 2026-05-29 | Apache-2.0 | MC |
| `io.github.takahirom.roborazzi:*` | **1.74.0** | 2026-09-08 | Apache-2.0 | MC |
| `app.cash.paparazzi:paparazzi` | 1.3.5 stable / 2.0.0-alpha05 | 2024-11-07 / 2026-05-20 | Apache-2.0 | MC |
| `com.android.compose.screenshot` (офиц.) | 0.0.1-alpha16 ⚠ **deprecated** | 2026-07-27 | Apache-2.0 | GM / DAC |
| `androidx.test:runner`/`rules`/`core-ktx` | 1.7.0 | 2025-07-30 | Apache-2.0 | GM |
| `androidx.test.ext:junit` | 1.3.0 | 2025-07-30 | Apache-2.0 | GM |
| `androidx.test.espresso:espresso-core` | 3.7.0 | 2025-07-30 | Apache-2.0 | GM |
| `androidx.test.uiautomator:uiautomator` | 2.4.0 | 2026-07-01 | Apache-2.0 | GM |
| `de.mannodermaus.gradle.plugins:android-junit5` | 2.0.1 (id `de.mannodermaus.android-junit`) | 2026-01-14 | Apache-2.0 | MC |
| `io.kotest:kotest-runner-junit5` | 6.2.5 | 2026-09-10 | Apache-2.0 | MC |
| Maestro | 2.10.0 | 2026-08-31 | Apache-2.0 | GitHub |
| `org.gradle.test-retry` | 1.6.6 | 2026-09-07 | Apache-2.0 | GPP |

### Качество

| Артефакт | Версия | Опубликовано | Лицензия | Источник |
|---|---|---|---|---|
| `io.gitlab.arturbosch.detekt` | **1.23.8** (группа заморожена) | 2025-02-21 | Apache-2.0 | MC |
| `dev.detekt:detekt-cli` (detekt 2.0) | 2.0.0-alpha.6 ⚠ alpha | 2026-08-04 | Apache-2.0 | MC / GPP |
| `io.nlopez.compose.rules:detekt` / `:ktlint` | 0.6.6 (⚠️ для detekt 1.23.x — потолок **0.4.28**) | 2026-09-01 | Apache-2.0 | MC |
| `com.pinterest.ktlint:ktlint-cli` | **1.8.0** (последняя в этой группе) | 2025-11-12 / индекс 2025-12-15 | MIT | MC |
| `io.github.ktlint.core:ktlint-cli` (новый владелец) | 2.0.0-ALPHA-4 ⚠ alpha | 2026-08-20 | MIT | MC |
| `com.ncorti.ktfmt.gradle` | 0.27.0 | 2026-08-03 | MIT | GPP |
| `com.squareup:sort-dependencies-gradle-plugin` | 0.21.0 | 2026-08-12 | Apache-2.0 | MC |
| `org.jlleitschuh.gradle.ktlint` | 14.2.0 | — | MIT | GPP |
| `com.diffplug.spotless:spotless-plugin-gradle` | 8.10.2 | 2026-09-04 | Apache-2.0 | MC |
| `com.facebook:ktfmt` | 0.64 | — | Apache-2.0 | MC |
| `org.jetbrains.kotlinx:kover-gradle-plugin` | **0.9.9** | 2026-07-17 | Apache-2.0 | MC |
| `com.lemonappdev:konsist` | 0.17.3 ⚠ 2024-12-08 | 2024-12-08 | Apache-2.0 | MC |
| `com.tngtech.archunit:archunit` | 1.5.0 | 2026-08-04 | Apache-2.0 | MC |
| `com.autonomousapps:dependency-analysis-gradle-plugin` | 3.19.2 | 2026-09-14 | Apache-2.0 | MC |
| `com.osacky.doctor:doctor-plugin` | 0.12.1 | — | Apache-2.0 | MC |
| `com.squareup.leakcanary:leakcanary-android` | 2.14 / 3.0-alpha-9 | — | Apache-2.0 | MC |
| `io.sentry:sentry-android` | 8.57.0 | — | MIT | MC |
| `com.jakewharton.timber:timber` | 5.0.1 (заморожен, 2021) | — | Apache-2.0 | MC |

### UDF/MVI-фреймворки и образцы

| Проект | Версия / состояние | Дата | Лицензия |
|---|---|---|---|
| `com.slack.circuit:circuit-foundation` | 0.39.0 (до 1.0) | 2026-09-15 | Apache-2.0 |
| `org.orbit-mvi:orbit-core` | 12.0.1 | — | Apache-2.0 |
| `com.arkivanov.decompose:decompose` | 3.5.0 / 3.6.0-beta01 | — | Apache-2.0 |
| `app.cash.molecule:molecule-runtime` | 2.2.0 | — | Apache-2.0 |
| `org.jetbrains.compose` (Compose Multiplatform) | 1.12.0 / 1.13.0-alpha01 | — | Apache-2.0 |
| `android/nowinandroid` | `main`, коммит 2026-09-02 | — | Apache-2.0 |
| `android/architecture-samples` | `main`, коммит **2025-07-17** ⚠ стагнирует | — | Apache-2.0 |
| `android/compose-samples` | `main`, коммит 2026-09-16 | — | Apache-2.0 |
| `chrisbanes/tivi` | ⚠ **archived**, 2024-11-12 | — | Apache-2.0 |
| `element-hq/element-x-android` | `develop`, 2026-09-16 | — | AGPL-3.0 |
| `bitwarden/android` | `main`, 2026-09-16 | — | GPL-3.0 |
| `signalapp/Signal-Android` | `main`, 2026-09-16 | — | AGPL-3.0 |
| `android/skills` | активен | — | Apache-2.0 |

## Источники

**Официальная документация Android / Google**

- https://developer.android.com/build/releases/gradle-plugin — таблица совместимости AGP
- https://developer.android.com/build/releases/agp-9-0-0-release-notes — AGP 9.0 breaking changes
- https://developer.android.com/build/migrate-to-built-in-kotlin — built-in Kotlin, миграция
- https://developer.android.com/build/jdks — Java toolchain
- https://developer.android.com/build/kotlin-support — Kotlin ↔ AGP ↔ R8
- https://developer.android.com/build/shrink-code — R8, full mode, AGP 9.3 `optimization {}`
- https://developer.android.com/jetpack/androidx/versions — сводка версий AndroidX
- https://developer.android.com/jetpack/androidx/releases/compose-material3 — material3, Expressive
- https://developer.android.com/jetpack/androidx/releases/navigation3 — Navigation 3
- https://developer.android.com/jetpack/androidx/releases/room3 — Room 3
- https://developer.android.com/jetpack/androidx/releases/benchmark — Benchmark 1.5.0
- https://developer.android.com/guide/navigation/navigation-3 и `/navigation-3/basics` — Nav3 API
- https://github.com/android/nav3-recipes — рецепты Nav3 (`BasicSaveableActivity.kt`)
- https://developer.android.com/topic/architecture — Guide to app architecture
- https://developer.android.com/topic/architecture/recommendations — приоритизированные рекомендации
- https://developer.android.com/topic/architecture/ui-layer — UI-слой, UiState, UDF
- https://developer.android.com/topic/architecture/data-layer — репозитории, SSOT, naming
- https://developer.android.com/topic/modularization и `/patterns` — модуляризация
- https://developer.android.com/develop/ui/compose/designsystems/custom — кастомные design systems
- https://developer.android.com/develop/ui/compose/designsystems/material3 — M3, dynamic color
- https://developer.android.com/develop/ui/compose/performance/stability и `/strongskipping`
- https://developer.android.com/develop/ui/compose/testing и `/testing/apis`
- https://developer.android.com/develop/ui/compose/testing/migrate-v2 — **v2 test API**
- https://developer.android.com/develop/ui/compose/system/insets — insets
- https://developer.android.com/develop/ui/compose/system/predictive-back
- https://developer.android.com/guide/navigation/custom-back/predictive-back-gesture
- https://developer.android.com/training/testing/fundamentals/test-doubles — fakes vs mocks
- https://developer.android.com/kotlin/coroutines/test — `runTest`, диспетчеры, `MainDispatcherRule`
- https://developer.android.com/training/dependency-injection/hilt-testing
- https://developer.android.com/studio/preview/compose-screenshot-testing — **deprecated**
- https://developer.android.com/studio/preview/compose-screenshot-testing-with-testsuites
- https://developer.android.com/studio/test/managed-devices — GMD
- https://developer.android.com/studio/run/emulator-acceleration — KVM
- https://developer.android.com/topic/performance/baselineprofiles/overview и `/create-baselineprofile`
- https://developer.android.com/guide/topics/resources/app-languages — per-app language
- https://developer.android.com/training/data-storage/shared/photo-picker
- https://developer.android.com/develop/ui/compose/notifications/notification-permission
- https://developer.android.com/training/app-links/verify-applinks
- https://developer.android.com/about/versions/14/changes/fgs-types-required
- https://developer.android.com/about/versions/15/behavior-changes-15
- https://developer.android.com/about/versions/16/behavior-changes-16
- https://developer.android.com/about/versions/17 , `/behavior-changes-17`, `/changes/ff-restrictions-ignored`
- https://developer.android.com/reference/android/os/Build.VERSION_CODES_FULL
- https://developer.android.com/google/play/requirements/target-sdk
- https://support.google.com/googleplay/android-developer/answer/11926878 — targetSdk в Play Console
- https://support.google.com/googleplay/android-developer/answer/17492799 — technical quality requirements
- https://developer.android.com/guide/practices/page-sizes — 16 KB
- https://developer.android.com/developer-verification — верификация разработчиков
- https://developer.android.com/docs/quality-guidelines/adaptive-app-quality
- https://developer.android.com/studio/releases — Android Studio Quail 4
- https://developer.android.com/tools/agents/android-cli и `/android-cli/download` — **Android CLI**
- https://developer.android.com/reference/tools/gradle-api/9.4/com/android/build/api/dsl/UnitTestOptions
- https://developer.android.com/reference/tools/gradle-api/9.4/com/android/build/api/dsl/ApplicationAndroidResources
- https://android-developers.googleblog.com/2026/06/Android-17.html
- https://android-developers.googleblog.com/2026/09/leverage-gemma-4-android-studio-quail.html
- https://android-developers.googleblog.com/2025/12/android-16-qpr2-is-released.html
- https://github.com/android/skills — официальные agent skills (Apache-2.0)
- https://github.com/android/gradle-recipes (ветка `agp-9.0`)

**Kotlin / Gradle**

- https://kotlinlang.org/docs/releases.html — версии и каденс
- https://docs.gradle.org/current/userguide/configuration_cache.html
- https://services.gradle.org/versions/current
- https://plugins.gradle.org/plugin/org.gradle.test-retry

**Репозитории-образцы**

- https://github.com/android/nowinandroid (`main`) — `settings.gradle.kts`, `gradle/libs.versions.toml`, `build-logic/convention/**`, `core/designsystem/.../theme/Theme.kt`, `core/testing/.../MainDispatcherRule.kt`, `docs/ModularizationLearningJourney.md`
- https://github.com/android/architecture-samples
- https://github.com/android/compose-samples
- https://github.com/chrisbanes/tivi (archived)
- https://github.com/slackhq/circuit , https://slackhq.github.io/circuit/
- https://github.com/element-hq/element-x-android
- https://github.com/bitwarden/android
- https://github.com/signalapp/Signal-Android

**Инструменты**

- https://robolectric.org/getting-started/ , `/configuring/` , `/compatibility_table/`
- https://robolectric.org/blog/2026/07/03/designing-local-testing-libraries/
- https://github.com/takahirom/roborazzi
- https://github.com/cashapp/paparazzi (CHANGELOG)
- https://github.com/cashapp/turbine
- https://kotlin.github.io/kotlinx-kover/gradle-plugin/ и CHANGELOG
- https://mockk.io/
- https://docs.maestro.dev/ , https://github.com/mobile-dev-inc/Maestro
- https://github.com/LemonAppDev/konsist
- https://github.com/mannodermaus/android-junit-framework (issue #404)
- https://github.com/robolectric/robolectric/issues/10797
- https://ktor.io/docs/client-testing.html
- https://www.zacsweers.dev/introducing-metro/ , https://www.zacsweers.dev/metro-is-stable/
- https://vinted.engineering/2026/02/12/from-dagger-to-metro/
- https://apilevels.com/ — распределение API-уровней (Statcounter, апрель 2026)

**Репозитории Maven (прямые запросы)**

- `https://dl.google.com/dl/android/maven2/<group path>/group-index.xml`
- `https://dl.google.com/dl/android/maven2/androidx/compose/compose-bom/2026.09.00/compose-bom-2026.09.00.pom`
- `https://repo1.maven.org/maven2/<group path>/<artifact>/maven-metadata.xml`
- `https://plugins.gradle.org/m2/<group path>/<artifact>/maven-metadata.xml`
