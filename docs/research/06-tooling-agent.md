# 06 · Инструменты и agent-friendliness: что реально делает агент на Linux без Mac

Все числа ниже **измерены 2026-09-17** на этой машине: Debian 13 (trixie) x86_64,
20 CPU, 15 ГБ RAM (под нагрузкой других агентов; load average в момент прогонов
1–8), NVMe, сеть ~50 МБ/с. Mac нет, Android Studio нет, эмулятор не поднимался.

Эмпирическая база — два spike'а, оба **собраны и прогнаны до зелёного**:

| spike | что это | где |
|---|---|---|
| `android-gate` | 7-модульный Android-проект (NiA в миниатюре) с полным гейтом | `docs/research/spikes/android-gate/` |
| `swift-linux` | SwiftPM-пакет Domain/Data/ApiClient + Swift Testing + OpenAPI-кодоген | `docs/research/spikes/swift-linux/` |

Пометки: **проверено** = я это выполнил и вижу вывод; **по докам** = прочитано в
первоисточнике, эмпирически не проверялось (всё, что требует macOS/Xcode).

---

## TL;DR

1. **Android-гейт полностью выполним headless на Linux — целиком, без эмулятора и
   без Android Studio.** Собран 7-модульный проект (AGP 9.4.0 / Gradle 9.7.1 /
   Kotlin 2.4.20 / Compose BOM 2026.09.00 / Hilt+KSP), в гейте: Spotless, detekt +
   compose-rules, Android Lint (`warningsAsErrors`), JVM-тесты, **Robolectric
   Compose UI-тесты**, **Roborazzi screenshot-тесты**, **Konsist архитектурные
   тесты**, Kover coverage-gate, `assembleDebug`, подписанный `assembleRelease`
   (R8) и `bundleRelease` (AAB). Всё зелёное.
2. **Цикл агента быстрый**: полный `make check` при изменении одного файла — **37 с**,
   при отсутствии изменений — **4.2 с**, «свежий клон при тёплом `~/.gradle`» — **94 с**.
   Вывод гейта — **12 строк** (concise-обёртка), полные логи на диске.
3. **Агент видит UI без устройства**: Roborazzi рендерит Compose в PNG под
   Robolectric за ~2.5 с/тест; я открыл получившийся `todos_ready.png` и прочитал
   на нём кириллицу и зачёркнутый текст. Это буквально «глаза» агента на Linux.
4. **Google выпустил agent-first CLI**: `cmdline-tools 23.0` содержит новый
   бинарь **`android`** (v1.0.16261425), который объявляет `sdkmanager` deprecated
   и добавляет `android docs search/fetch` (Android Knowledge Base), `android skills`,
   `android create` (шаблон проекта уже на AGP 9 + Navigation 3), `android layout`,
   `android screen capture --annotate`, `android emulator`, `android describe`
   (JSON-карта артефактов сборки). Это меняет оценку agent-friendliness Android
   в большую сторону.
5. **Цена входа по диску — около 9.5 ГБ** и ~8 минут скачиваний: Android SDK 806 МБ,
   JDK 346 МБ, `~/.gradle` 4.0 ГБ (из них wrapper 309 МБ, jdks 502 МБ, modules-2 1.2 ГБ),
   `~/.konan` 822 МБ (KMP), Swift 3.6 ГБ, SwiftLint 245 МБ.
6. **Память — узкое место, а не CPU.** Gradle-демон в пике 3.6 ГБ RSS + Kotlin-демон
   1.2 ГБ = **4.8 ГБ**, при `-Xmx2g`. Android Lint **падает с `OutOfMemoryError: Metaspace`**
   при `-XX:MaxMetaspaceSize=512m` — нужен минимум **1 ГБ метаспейса**.
7. **Kotlin Multiplatform на Linux компилирует iOS-код**: `compileKotlinIosArm64` и
   `compileKotlinIosSimulatorArm64` собирают klib **успешно** (66 с с учётом
   скачивания Kotlin/Native). Но `linkDebugFrameworkIosArm64` **молча SKIPPED
   с кодом возврата 0** — Gradle не сообщает об этом ошибкой. Для гейта это
   ловушка: «зелёная» сборка не значит, что framework собрался.
8. **Swift 6.4 (релиз 2026-09-14) имеет официальный тарболл под Debian 13** и
   работает из коробки: ни одного `apt install` не понадобилось, `ldd` чист.
   `swift build` 4 с, `swift test` (Swift Testing) **0.72 с тёплый**, `swift-format`
   28 мс, SwiftLint 0.65.1 (нативный Linux-бинарь) 180 мс. Для доменного/сетевого
   слоя iOS-приложения Linux-петля отличная.
9. **Оба OpenAPI-кодогена работают headless на Linux, и оба ломают наш cookie-auth**
   (ответ потоку 07). Swift OpenAPI Generator 1.13.1 сгенерировал клиент из
   OpenAPI 3.1 в стиле контракта huma и собрался (59 с холодных), **но
   `securitySchemes` игнорирует полностью** — куки остаются задачей `ClientMiddleware`.
   OpenAPI Generator CLI 7.25.0 (`-g kotlin`) отработал за **0.86 с** и 20 файлов,
   но `in: cookie` **молча смапил в `query`**: `ApiKeyAuth("query", "access_token")`,
   а в сгенерированном `ApiKeyAuth.apply` ветки `"cookie"` нет вовсе — токен ушёл бы
   в URL. Вывод: из кодогена берём модели, транспорт и аутентификацию пишем руками.
10. **Всё, что касается iOS UI, на Linux недоступно**: `import SwiftUI` и
    `import UIKit` → `error: no such module`. `URLSession` на Linux требует
    `import FoundationNetworking` (в Foundation его нет). Симулятор, подпись,
    `xcodebuild test`, `xcresult` — только Mac.
11. **Два формата — два конфликта, оба воспроизведены.** На Android:
    `detekt-formatting 1.23.8` несёт **ktlint 0.50.0**, Spotless — ktlint 1.8.0;
    вместе они переписывают код друг за другом и гейт не сходится. На Swift:
    SwiftLint `trailing_comma` **запрещает** висящую запятую, `swift-format`
    `TrailingComma` — **требует**. Вывод: одна автофиксилка формата на язык.
12. **compose-rules 0.5.0+ требует detekt 2.0.0-alpha** (`dev.detekt`), а detekt
    2.0 всё ещё alpha.6. Стабильная связка — **detekt 1.23.8 + compose-rules 0.4.28**.
13. **`dependency-analysis` 3.19.2 не поддерживает AGP 9.4.0** (знает до 9.3.1) и
    не регистрирует project-health → в гейт пока не берём.
14. **AGP 9 сломал канон convention-плагинов Now in Android**: `CommonExtension`
    больше не генерик (`CommonExtension<*,*,*,*,*,*>` не компилируется), и
    `defaultConfig`/`compileOptions`/`testOptions`/`lint` скрыты из Kotlin-видимости
    `CommonExtension`. Рабочий паттерн — конфигурировать `ApplicationExtension`/
    `LibraryExtension` напрямую, общее выносить функциями поверх `Lint`/`CompileOptions`/
    `TestOptions`. Плюс `compileSdk` теперь минорный: `compileSdk = 37` + `compileSdkMinor = 2`.
15. **Итог по циклу «правка → проверка»**: Android — **полный цикл на Linux**
    (сборка, тесты, UI-тесты, скриншоты, линтеры, подписанный артефакт).
    iOS — **частичный**: домен/данные/сеть/тесты/линтеры на Linux, всё UI-, симуляторное
    и подписное — на удалённый Mac по SSH (`make check-mac`), с fail-fast, если Mac недоступен.

---

## 1. Окружение: что поставлено, чем и почём

### 1.1 Неожиданность №0: на машине не было JDK

В брифе значилось «JDK 21 (Debian, `/usr/bin/java`)». Фактически стоял **только JRE**:

```
$ dpkg -l | grep -i jdk    → пусто
$ ls /usr/lib/jvm/java-21-openjdk-amd64/bin
java  jpackage  keytool  rmiregistry        # javac нет
```

Gradle-демон на JRE стартует и даже компилирует Kotlin (Kotlin-компилятор свой),
но любой Android-модуль требует `compileXxxJavaWithJavac`. Обход **без sudo/apt** —
пользовательский JDK:

```bash
curl -sSL -o temurin21.tar.gz \
  "https://api.adoptium.net/v3/binary/latest/21/ga/linux/x64/jdk/hotspot/normal/eclipse"
tar xzf temurin21.tar.gz -C ~/.cache/stit-mobile-research/jdk
export JAVA_HOME=~/.cache/stit-mobile-research/jdk/jdk-21.0.12.1+1
```

| метрика | значение |
|---|---|
| скачивание (207 МБ) | **158 с** |
| на диске | **346 МБ** |
| версия | Temurin `21.0.12.1+1` (LTS), `javac 21.0.12.1` |

Второй путь (и он же рекомендация для шаблона) — **Gradle toolchain
auto-provisioning**: `id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"`
в `settings.gradle.kts`. Проверено: при `jvmToolchain(17)` и отсутствии JDK 17 Gradle
сам скачал его в `~/.gradle/jdks` (**502 МБ**), задача прошла. Без резолвера ошибка
предельно ясная:

```
Cannot find a Java installation on your machine (Linux ... amd64) matching:
{languageVersion=17, vendor=any vendor, ...}. Toolchain download repositories
have not been configured.
```

### 1.2 Android SDK headless: `cmdline-tools` 23.0 и новый CLI `android`

Актуальную ссылку брал не со страницы `developer.android.com/studio` (она
JS-рендерится), а из машинного индекса Google — это надёжный способ и для скриптов:

```bash
curl -sSL -o repo2-3.xml https://dl.google.com/android/repository/repository2-3.xml
# cmdline-tools;latest → rev 23.0 → commandlinetools-linux-16111833_latest.zip (181.1 MB)
```

| шаг | команда | время | диск |
|---|---|---|---|
| скачать cmdline-tools | `curl -sSL -o cmdline-tools.zip https://dl.google.com/android/repository/commandlinetools-linux-16111833_latest.zip` | **25 с** (181 052 239 B) | — |
| распаковать в `$SDK/cmdline-tools/latest` | `unzip -q …` | **0.6 с** | 174 МБ |
| поставить пакеты | `android sdk install platform-tools platforms/android-37.2 build-tools/37.0.0` | **168 с** | → 513 МБ |
| +`platforms/android-36` (доставился автоматически шаблоном `android create`) | — | — | → **806 МБ** |

SHA-256 скачанного архива: `0877a1d048fe4a24efe2eff536ca4223f7adeb58648bb81909d33c446918cfa8`.

**Главное открытие потока.** В `cmdline-tools 23.0` `sdkmanager` печатает:

```
WARNING: The SDK Manager CLI tool (sdkmanager) is deprecated. Android CLI will be used instead.
The 'android' binary can also be found in the cmdline-tools directory, and 'android sdk' is
the replacement for 'sdkmanager'.
To learn more about the Android CLI ... see https://d.android.com/tools/agents/android-cli
```

URL говорит сам за себя: `/tools/**agents**/android-cli`. Google выпустил
**agent-first CLI**. `android --help` (проверено):

| команда | назначение (проверено на своей машине, если помечено ✅) |
|---|---|
| `android sdk install/list/remove/update` | ✅ замена `sdkmanager`; `android sdk list --all` за 4 с, 631 строка |
| `android create [--list] --name … --output …` | ✅ скаффолд проекта из шаблона; единственный шаблон `empty-activity` с тегами `compose,activity,agp-9` |
| `android describe` | анализ проекта → JSON с путями артефактов сборки (для инструментов/агентов) |
| `android docs search` / `docs fetch` | поиск и выгрузка из Android Knowledge Base |
| `android skills add/list/find` | установка «Android skills» для агентов (`android skills add --agent='gemini' edge-to-edge`) |
| `android init` | подготовка среды для агента, ставит скилл `android-cli` |
| `android emulator create/list/start/stop` | замена `avdmanager` (на Linux доступна; на Windows отключена) |
| `android run --apks=…` | сборка/деплой/запуск на устройстве |
| `android install` | инкрементальная установка APK быстрее `adb` |
| `android layout --pretty --output=hierarchy.json` | дерево UI приложения в JSON |
| `android screen capture --output=ui.png --annotate` | скриншот устройства с аннотацией элементов |
| `android screen resolve --screenshot=ui.png --string="input tap #5"` | перевод «визуальных меток» в координаты |
| `android studio check / analyze-file / find-usages / render-compose-preview / version-lookup` | мост в запущенную Android Studio (требует Studio Quail 2 Canary 1+ и Gemini) |
| `android update` | самообновление CLI |

Практические детали, которые важны для шаблона (проверено):
- **Лицензии принимаются автоматически**: после `android sdk install` появился
  `$SDK/licenses/android-sdk-license` без единого интерактивного вопроса.
  Старый ритуал `yes | sdkmanager --licenses` не нужен.
- CLI **шлёт телеметрию**; глушится флагом `--no-metrics`, и его можно зафиксировать
  в `~/.androidrc` (туда же `--sdk=<path>`).
- Формат путей пакетов сменился с `platforms;android-36` (точка с запятой) на
  **`platforms/android-36`** (слэш). Старый синтаксис `sdkmanager` тоже принимается,
  но в скриптах шаблона стоит писать новый.

### 1.3 Актуальные уровни Android SDK (проверено `android sdk list --all`)

Версионирование платформ стало **минорным** — это ново и ломает привычные конфиги:

```
platforms/android-36      platforms/android-36.1
platforms/android-37.0    platforms/android-37.1    platforms/android-37.2
build-tools/36.0.0  36.1.0  37.0.0        platform-tools 37.0.1
```

Стабильный максимум на 2026-09-17 — **API 37.2**; AGP 9.4.0 объявляет «Max API level 37».
В DSL это `compileSdk = 37` + **`compileSdkMinor = 2`**. Если оставить `compileSdk = 36`,
сборка падает (проверено) на зависимостях AndroidX:

```
Dependency 'androidx.core:core-ktx:1.19.0' requires libraries and applications that
depend on it to compile against version 37 or later of the Android APIs.
:app is currently compiled against android-36.
Recommended action: Update this project to use a newer compileSdk of at least 37,
for example 37.2.
```

### 1.4 Gradle

```
$ ./gradlew --version        # первый запуск: скачивание дистрибутива
Gradle 9.7.1   (build time 2026-08-19)   Kotlin: 2.4.0   Launcher JVM: 21.0.12.1
COLD: 20 с ; ~/.gradle после: 309 МБ
```

`gradle-wrapper.properties` фиксируем c контрольной суммой (её отдаёт
`https://services.gradle.org/distributions/gradle-9.7.1-bin.zip.sha256`, **обязательно
с `-L`**, иначе прилетает HTML 301):

```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-9.7.1-bin.zip
distributionSha256Sum=acd53f1edaf02f1a8ff99879f8a34b302661a057d9b063ae9e35b552f804d20a
validateDistributionUrl=true
```

### 1.5 Swift 6.4 на Debian 13

Релиз **6.4.0 от 2026-09-14** — первый, у которого в официальном списке платформ
есть **Debian 13** (и Ubuntu 26.04, и RHEL UBI 10, и SDK под **Android**, Wasm, static-musl).
Машинно-читаемый индекс: `https://www.swift.org/api/v1/install/releases.json`.

```bash
curl -sSL -o swift.tar.gz \
  https://download.swift.org/swift-6.4.0-release/debian13/swift-6.4.0-RELEASE/swift-6.4.0-RELEASE-debian13.tar.gz
tar xzf swift.tar.gz -C ~/.cache/stit-mobile-research/swift
export PATH=~/.cache/stit-mobile-research/swift/swift-6.4.0-RELEASE-debian13/usr/bin:$PATH
```

| метрика | значение |
|---|---|
| размер тарболла | **1 122 646 127 B** (1.12 ГБ), скачался за ~2 мин |
| распаковка | **14 с** |
| на диске | **3.6 ГБ** |
| `swift --version` | `Swift version 6.4 (swift-6.4-RELEASE)`, target `x86_64-unknown-linux-gnu` |
| системные зависимости | **ни одной не потребовалось**: `ldd swift-frontend` — все библиотеки найдены на голом Debian 13 |

В тарболле сразу: `swift-format`, `sourcekit-lsp`, `clangd`, `lldb`, `clang-21`,
`wasmkit`, конфиги кросс-компиляции под `*-swift-linux-musl`. Отдельно ставить
`swift-format` не нужно — он часть тулчейна (`swift format …`).

### 1.6 Сводка «цена входа»

| каталог | размер | что это |
|---|---|---|
| `~/.cache/stit-mobile-research/android-sdk` | **806 МБ** | cmdline-tools 174 + build-tools 152 + platforms (36 и 37.2) 167+… + platform-tools 22 |
| `~/.cache/stit-mobile-research/jdk` | **346 МБ** | Temurin JDK 21 |
| `~/.cache/stit-mobile-research/swift` | **3.6 ГБ** | Swift 6.4 toolchain |
| `~/.cache/stit-mobile-research/swiftlint` | **245 МБ** | SwiftLint 0.65.1 (два бинаря: динамический 97 МБ + static 93 МБ) |
| `~/.gradle` | **4.0 ГБ** | `caches` 3.2 ГБ (`modules-2` 1.2 ГБ, `build-cache-1` 144 МБ), `wrapper` 309 МБ, `jdks` 502 МБ |
| `~/.konan` | **822 МБ** | Kotlin/Native 2.4.20 prebuilt (только если включён KMP) |
| `android-gate/**/build` | ~200 МБ | `app/build` 127–153 МБ, `feature/todos/build` 24 МБ, остальное по 5–11 МБ |
| `swift-linux/.build` | 52 МБ → **657 МБ** после подключения swift-openapi-* | |

Итого «с нуля до зелёного гейта обеих платформ» — **≈ 9.5 ГБ** и **≈ 8 минут** чистых
скачиваний при 50 МБ/с.

---

## 2. Spike `android-gate`

### 2.1 Дерево

```
android-gate/
├── Makefile                          # словарь верб для агента
├── settings.gradle.kts               # includeBuild(build-logic), typesafe accessors, foojay
├── build.gradle.kts                  # пины версий плагинов (apply false) + kover/spotless
├── gradle.properties                 # ресурсные потолки под нагруженную машину
├── gradle/libs.versions.toml         # ЕДИНСТВЕННОЕ место версий, включая SDK-уровни
├── .editorconfig                     # настройки ktlint (он их читает)
├── config/detekt/detekt.yml          # профиль поверх detekt default + Compose-правила
├── scripts/tools/
│   ├── check.sh                      # гейт, печатает OK:/FAIL: по строке на шаг
│   ├── gradle-step.sh                # один шаг: тихо, а при падении — выжимка лога
│   └── doctor.sh                     # fail-fast preflight (JDK, SDK, лицензии, RAM)
├── build-logic/                      # included build — вся общая конфигурация сборки
│   ├── settings.gradle.kts
│   └── convention/
│       ├── build.gradle.kts
│       └── src/main/kotlin/
│           ├── tech/stit/convention/KotlinAndroid.kt    # общие куски DSL + доступ к каталогу
│           ├── AndroidApplicationConventionPlugin.kt     # stit.android.application
│           ├── AndroidApplicationComposeConventionPlugin.kt
│           ├── AndroidLibraryConventionPlugin.kt         # stit.android.library
│           ├── AndroidLibraryComposeConventionPlugin.kt
│           ├── AndroidFeatureConventionPlugin.kt         # stit.android.feature (профиль фичи)
│           ├── AndroidHiltConventionPlugin.kt            # stit.android.hilt
│           ├── AndroidRoborazziConventionPlugin.kt       # stit.android.roborazzi
│           ├── JvmLibraryConventionPlugin.kt             # stit.jvm.library
│           └── QualityConventionPlugin.kt                # stit.quality (detekt+kover на всех)
├── app/                              # :app — Application, MainActivity, Konsist-тесты
├── core/model/                       # чистый Kotlin/JVM: домен без Android
├── core/data/                        # репозиторий + Hilt-модуль
├── core/designsystem/                # токены через CompositionLocal поверх Material 3
├── core/testing/                     # FakeTodoRepository, MainDispatcherRule
├── feature/todos/                    # Compose-экран + ViewModel + UiState + 3 вида тестов
└── shared/                           # KMP: androidLibrary + jvm + iosArm64/SimulatorArm64/X64
```

67 файлов исходников (без `build/`).

### 2.2 `gradle/libs.versions.toml` — фактические версии

Все версии проверены по Google Maven / Maven Central / Gradle Plugin Portal
**2026-09-17** и все **собрались вместе**:

```toml
[versions]
agp = "9.4.0"
kotlin = "2.4.20"
ksp = "2.3.12"
hilt = "2.60.1"
androidxCore = "1.19.0"
androidxActivity = "1.13.0"
androidxLifecycle = "2.11.0"
androidxComposeBom = "2026.09.00"
androidxHiltNavigationCompose = "1.4.0"
androidxTestExtJunit = "1.3.0"
coroutines = "1.11.0"
kotlinxSerialization = "1.11.0"
turbine = "1.2.1"
junit4 = "4.13.2"
robolectric = "4.17"
roborazzi = "1.74.0"
detekt = "1.23.8"
detektComposeRules = "0.4.28"     # 0.5.0+ требует detekt 2.0.0-alpha — см. ловушку L-04
konsist = "0.17.3"
kover = "0.9.9"
spotless = "8.10.2"
ktlint = "1.8.0"
dependencyAnalysis = "3.19.2"     # вне гейта: не знает AGP 9.4 — см. L-05

# SDK-уровни — тоже версии: одно место правки
compileSdk = "37"
compileSdkMinor = "2"
minSdk = "26"
targetSdk = "37"
```

Приём «SDK-уровни как версии в каталоге» окупается сразу: convention-плагин читает
их через `libs.findVersion("compileSdk")`, и ни один модуль не знает чисел.

### 2.3 `settings.gradle.kts`

```kotlin
pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories { google { content { /* те же три regex */ } }; mavenCentral() }
}

plugins {
    // Toolchain auto-provisioning: на машине без нужного JDK Gradle скачает его
    // в ~/.gradle/jdks — шаблон не зависит от того, что стоит у разработчика.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "android-gate"

include(":app")
include(":core:model"); include(":core:data"); include(":core:designsystem"); include(":core:testing")
include(":feature:todos")
include(":shared")
```

`TYPESAFE_PROJECT_ACCESSORS` даёт `projects.core.model` вместо строк — это ещё и
защита агента от опечаток в именах модулей (ошибка компиляции, а не runtime).

### 2.4 `gradle.properties` под нагруженную машину

```properties
# --- JVM/ресурсы ---
org.gradle.jvmargs=-Xmx2g -XX:MaxMetaspaceSize=1g -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8
org.gradle.workers.max=4
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configuration-cache=true
org.gradle.configuration-cache.parallel=true

# --- Kotlin/KSP демоны ---
kotlin.daemon.jvmargs=-Xmx1g -XX:MaxMetaspaceSize=384m
kotlin.code.style=official

# --- AndroidX / AGP ---
android.useAndroidX=true
android.nonTransitiveRClass=true

# --- Kotlin Multiplatform ---
kotlin.native.ignoreDisabledTargets=true
```

Комментарий к `MaxMetaspaceSize=1g`: со значением `512m` **Android Lint падает**
(см. ловушку L-02). Это не теория — я на это наступил.

Из `gradle.properties` пришлось **удалить** `android.defaults.buildfeatures.buildconfig=false`
(и `resvalues`, `shaders`): AGP 9 их выкинул и предупреждает —

```
WARNING: The option 'android.defaults.buildfeatures.buildconfig' is deprecated.
The current default is 'false'. It was removed in version 9.0 of the Android Gradle plugin.
```

### 2.5 Convention-плагины: как это выглядит после AGP 9

Канон Now in Android (`configureKotlinAndroid(commonExtension: CommonExtension<*,*,*,*,*,*>)`)
на AGP 9.4.0 **не компилируется**. Первая попытка дала:

```
e: KotlinAndroid.kt:18:71 No type arguments expected for 'interface CommonExtension : ExtensionAware'.
```

После снятия генериков — вторая волна:

```
e: KotlinAndroid.kt:22:9 Unresolved reference 'defaultConfig'.
e: KotlinAndroid.kt:26:9 Unresolved reference 'compileOptions'.
e: KotlinAndroid.kt:31:9 Unresolved reference 'testOptions'.
e: KotlinAndroid.kt:39:9 Unresolved reference 'lint'.
```

Разобрался разбором самого `gradle-api-9.4.0.jar`: методы `getDefaultConfig()`,
`getCompileOptions()`, `getLint()`, `getTestOptions()` в байткоде `CommonExtension`
**есть**, но из Kotlin-видимости убраны (new DSL AGP 9; ср. `android.newDsl.optOut`
в release notes 9.4). На `LibraryExtension`/`ApplicationExtension` они доступны и
в property-, и в action-форме (`lint(Function1)`).

Рабочий паттерн — общие куски вынести поверх **конкретных DSL-интерфейсов**:

```kotlin
// build-logic/convention/src/main/kotlin/tech/stit/convention/KotlinAndroid.kt
package tech.stit.convention

import com.android.build.api.dsl.CompileOptions
import com.android.build.api.dsl.Lint
import com.android.build.api.dsl.TestOptions
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

fun VersionCatalog.int(alias: String): Int = findVersion(alias)
    .orElseThrow { IllegalStateException("нет версии '$alias' в libs.versions.toml") }
    .requiredVersion
    .toInt()

// AGP 9 убрал compileOptions/lint/testOptions/defaultConfig из Kotlin-видимости
// CommonExtension (new DSL), поэтому общие куски — отдельными функциями поверх
// конкретных DSL-интерфейсов; вызываются из ApplicationExtension/LibraryExtension.

internal fun configureJava(options: CompileOptions) {
    options.sourceCompatibility = JavaVersion.VERSION_17
    options.targetCompatibility = JavaVersion.VERSION_17
}

internal fun configureUnitTests(options: TestOptions) {
    options.unitTests.apply {
        // Robolectric/Roborazzi: без этого нет доступа к res/assets в JVM-тестах
        isIncludeAndroidResources = true
        isReturnDefaultValues = true
    }
}

internal fun Project.configureLint(lint: Lint) {
    lint.apply {
        warningsAsErrors = true
        abortOnError = true
        checkDependencies = true
        xmlReport = true
        sarifReport = true
        htmlReport = false
        textReport = true
        // baseline осознанно НЕ заводим: шаблон стартует с нулём долгов.
        // Для миграции существующего кода — `./gradlew updateLintBaseline`.
        disable += "GradleDependency"
    }
}
```

```kotlin
// AndroidLibraryConventionPlugin.kt
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.library")
        pluginManager.apply("stit.quality")

        extensions.configure<LibraryExtension> {
            compileSdk = libs.int("compileSdk")
            compileSdkMinor = libs.int("compileSdkMinor")
            defaultConfig { minSdk = libs.int("minSdk") }
            compileOptions { configureJava(this) }
            testOptions { configureUnitTests(this) }
            lint { configureLint(this) }
        }
    }
}
```

Профиль фичи — один плагин на весь класс модулей, модуль объявляет только namespace:

```kotlin
// AndroidFeatureConventionPlugin.kt
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("stit.android.library.compose")
        pluginManager.apply("stit.android.hilt")
        pluginManager.apply("stit.android.roborazzi")

        dependencies {
            add("implementation", project(":core:designsystem"))
            add("implementation", project(":core:model"))
            add("implementation", libs.findLibrary("androidx-compose-material3").get())
            add("implementation", libs.findLibrary("androidx-lifecycle-runtime-compose").get())
            add("implementation", libs.findLibrary("androidx-lifecycle-viewmodel-compose").get())
            add("implementation", libs.findLibrary("androidx-hilt-navigation-compose").get())
            add("testImplementation", project(":core:testing"))
        }
    }
}
```

…и тогда весь `feature/todos/build.gradle.kts` — это:

```kotlin
plugins { alias(libs.plugins.stit.android.feature) }
android { namespace = "tech.stit.mobiletemplate.feature.todos" }
dependencies {
    implementation(projects.core.data)
    implementation(libs.kotlinx.coroutines.core)
}
```

Для агента это важнее, чем для человека: **6 строк вместо 60** — меньше поверхности
для расхождений между модулями и меньше контекста на чтение.

Профиль качества — один на все модули:

```kotlin
// QualityConventionPlugin.kt
class QualityConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("io.gitlab.arturbosch.detekt")
        // kover должен стоять на каждом модуле — корень только агрегирует
        pluginManager.apply("org.jetbrains.kotlinx.kover")

        extensions.configure<DetektExtension> {
            buildUponDefaultConfig = true
            allRules = false
            config.setFrom(rootProject.files("config/detekt/detekt.yml"))
            parallel = true
            autoCorrect = false
        }

        // detekt-formatting НЕ подключаем: он несёт ktlint 0.50.0 и конфликтует
        // со Spotless/ktlint 1.8.0. Форматирование — одна инстанция, Spotless.
        dependencies { add("detektPlugins", libs.findLibrary("detekt-compose-rules").get()) }

        tasks.withType<Detekt>().configureEach {
            reports {
                sarif.required.set(true); xml.required.set(true); txt.required.set(true)
                html.required.set(false); md.required.set(false)
            }
        }
    }
}
```

Важная деталь, на которой я потерял время: **Kover нужно применять к каждому
модулю**, корневой `kover(projects.app)` без этого падает невнятной ошибкой
разрешения вариантов (`Incompatible because this component declares 'org.gradle.usage'
= 'java-runtime' and the consumer needed 'kover'`).

### 2.6 Корневой `build.gradle.kts`

```kotlin
plugins {
    // Версии плагинов пинятся здесь (apply false) — convention-плагины из build-logic
    // применяют их по id, не зная про classpath.
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.roborazzi) apply false

    alias(libs.plugins.kover)
    alias(libs.plugins.spotless)
}

dependencies {
    kover(projects.app); kover(projects.core.model); kover(projects.core.data)
    kover(projects.core.designsystem); kover(projects.feature.todos)
}

kover {
    reports {
        filters {
            excludes {
                classes(
                    "*Hilt_*", "*_Factory*", "*_HiltModules*", "*_MembersInjector",
                    "*ComposableSingletons*", "*_Impl", "dagger.hilt.*",
                    "hilt_aggregated_deps.*", "*.BuildConfig", "*Preview*",
                )
                annotatedBy("androidx.compose.ui.tooling.preview.Preview")
            }
        }
        verify {
            rule("покрытие строк доменных модулей") {
                bound {
                    minValue = 60
                    coverageUnits = kotlinx.kover.gradle.plugin.dsl.CoverageUnit.LINE
                }
            }
        }
    }
}

spotless {
    kotlin {
        target("**/*.kt"); targetExclude("**/build/**", "**/generated/**")
        ktlint(libs.versions.ktlint.get())
        trimTrailingWhitespace(); endWithNewline()
    }
    kotlinGradle { target("**/*.kts"); targetExclude("**/build/**"); ktlint(libs.versions.ktlint.get()) }
}
```

Фактическое покрытие spike'а (`koverXmlReport`): **LINE 83.2 %** (89/107),
INSTRUCTION 76.0 %, BRANCH 44.1 %, METHOD 72.2 %, CLASS 65.0 %. Порог `minValue = 60`
по строкам выбран как «проходит, но не даром»; для шаблона реальный пол обсуждается
в рекомендациях.

### 2.7 `.editorconfig` — единственное место настройки ktlint

ktlint (и через Spotless тоже) читает `.editorconfig`; без него ktlint 1.8.0 валит
Compose-код правилом `standard:function-naming`.

```ini
root = true

[*]
charset = utf-8
end_of_line = lf
insert_final_newline = true
trim_trailing_whitespace = true
indent_style = space
indent_size = 4

[*.{kt,kts}]
max_line_length = 140
ktlint_code_style = intellij_idea
# @Composable-функции именуются с большой буквы — это канон Compose
ktlint_function_naming_ignore_when_annotated_with = Composable
# class X @Inject constructor(...) читается лучше в одну строку
ktlint_standard_class-signature = disabled
# порядок импортов: всё, потом java/javax/kotlin/алиасы
ij_kotlin_imports_layout = *,java.**,javax.**,kotlin.**,^

[*.{yml,yaml,json,toml}]
indent_size = 2

[*.md]
trim_trailing_whitespace = false
```

Без `ktlint_standard_class-signature = disabled` `spotlessApply` разворачивает
`class InMemoryTodoRepository @Inject constructor() : TodoRepository {` в три строки
с лишним отступом — это воспроизведено и откачено.

### 2.8 `config/detekt/detekt.yml`

```yaml
# Профиль поверх detekt default (buildUponDefaultConfig = true) — только дельты.
build:
  maxIssues: 0

config:
  validation: true
  warningsAsErrors: true

complexity:
  LongParameterList:
    functionThreshold: 8
    constructorThreshold: 10
    ignoreAnnotated: ['Composable', 'Inject']
  TooManyFunctions:
    ignoreAnnotatedFunctions: ['Composable']
    thresholdInFiles: 20
    thresholdInClasses: 20

style:
  MagicNumber:
    # Именованная константа — и есть починка MagicNumber; токены дизайн-системы
    # (0xFF……) живут именно в property-декларациях.
    ignorePropertyDeclaration: true
    ignoreAnnotated: ['Composable', 'Preview']
  ForbiddenImport:
    active: true
    imports:
      - value: 'android.util.Log'
        reason: 'Логи идут через core:logging (structured), не через android.util.Log.'
      - value: 'java.util.Date'
        reason: 'Только kotlinx-datetime / java.time.'
    forbiddenPatterns: ''
  ForbiddenComment:
    active: true
    comments:
      - value: 'FIXME:'
        reason: 'FIXME в main-ветке не живёт — заводи запись в roadmap.'
  MaxLineLength:
    maxLineLength: 140
    excludeCommentStatements: true

naming:
  FunctionNaming:
    ignoreAnnotated: ['Composable']       # @Composable — с большой буквы

Compose:                                   # io.nlopez.compose.rules:detekt
  ComposableAnnotationNaming: { active: true }
  ComposableNaming: { active: true }
  ComposableParamOrder: { active: true }
  CompositionLocalAllowlist:
    active: true
    allowedCompositionLocals: 'LocalStitSpacing,LocalStitColors'   # строка, НЕ список
  CompositionLocalNaming: { active: true }
  ContentEmitterReturningValues: { active: true }
  ModifierComposed: { active: true }
  ModifierMissing: { active: true }
  ModifierNotUsedAtRoot: { active: true }
  ModifierReused: { active: true }
  ModifierWithoutDefault: { active: true }
  MultipleEmitters: { active: true }
  MutableParams: { active: true }
  PreviewAnnotationNaming: { active: true }
  PreviewPublic: { active: true }
  RememberMissing: { active: true }
  UnstableCollections: { active: false }
  ViewModelForwarding: { active: true }
  ViewModelInjection: { active: true }
```

`config.validation: true` — очень агент-дружественная вещь: любая опечатка в
ключе валит таск с точным сообщением

```
Run failed with 2 invalid config properties.
- Property 'Compose>CompositionLocalAllowlist>allowedCompositionLocals' is misspelled or does not exist.
- Property 'Compose>ComposeUnstableCollections' is misspelled or does not exist.
```

а не тихо игнорирует правило. Именно так я и обнаружил, что `allowedCompositionLocals`
ждёт **строку через запятую**, а правило зовётся `UnstableCollections`, а не
`ComposeUnstableCollections`.

Что detekt поймал в моём собственном коде (все находки настоящие, не выдуманные):
`MagicNumber` ×6 на hex-цветах токенов, `ImportOrdering` ×3, `MaximumLineLength`,
`ArgumentListWrapping` ×2, `UnusedPrivateMember`.

### 2.9 Konsist — архитектурные тесты как обычные unit-тесты

Konsist сканирует **файловую систему**, а не граф Gradle, поэтому живёт в
`app/src/test/kotlin/.../ArchitectureTest.kt` и покрывает весь репозиторий.
5 правил, все зелёные, прогон **0.52 с**:

```kotlin
class ArchitectureTest {

    @Test
    fun `feature-модуль не импортирует другой feature-модуль`() {
        Konsist.scopeFromProject()
            .files
            .filter { it.path.contains("/feature/") }
            .assertTrue { file ->
                val ownFeature = Regex("/feature/([^/]+)/").find(file.path)?.groupValues?.get(1)
                file.imports.none { import ->
                    import.name.startsWith("tech.stit.mobiletemplate.feature.") &&
                        ownFeature != null &&
                        !import.name.startsWith("tech.stit.mobiletemplate.feature.$ownFeature")
                }
            }
    }

    @Test
    fun `core model не знает про Android`() {
        Konsist.scopeFromProject()
            .files
            .filter { it.path.contains("/core/model/") }
            .flatMap { it.imports }
            .filter { it.name.startsWith("android.") || it.name.startsWith("androidx.") }
            .assertEmpty()
    }

    @Test
    fun `классы-наследники ViewModel называются XxxViewModel`() {
        Konsist.scopeFromProject().classes()
            .withParentClassOf(ViewModel::class)
            .assertTrue { it.name.endsWith("ViewModel") }
    }

    @Test
    fun `Composable-функции экранов лежат в файлах XxxScreen`() {
        Konsist.scopeFromProject().functions()
            .withAllAnnotationsOf(Composable::class)
            .filter { it.name.endsWith("Screen") }
            .assertTrue { it.containingFile.name.endsWith("Screen") }
    }

    @Test
    fun `в data-слое нет публичных Hilt-модулей`() {
        Konsist.scopeFromProject().classes(includeNested = true)
            .filter { it.name.endsWith("Module") && it.path.contains("/core/data/") }
            .assertTrue { it.hasInternalModifier }
    }
}
```

Это прямой аналог `depguard`-контрактов `.golangci.yml` в `go-backend-template` и
`import-linter` в `telegram-bot-template` — только в виде тестов, а значит с
человекочитаемым сообщением о нарушении и без отдельного инструмента в гейте.

**Подводный камень**: поскольку Konsist ходит по диску, он видит и модули, не
включённые в `settings.gradle.kts` (я это проверил негативной фикстурой — правило
сработало на модуле `feature/other`, которого не было в `include`). Это скорее
плюс (никаких «спрятанных» нарушений), но об этом надо знать.

### 2.10 Robolectric + Roborazzi: UI-тесты и скриншоты без устройства

`AndroidRoborazziConventionPlugin` подключает всё разом, модулю знать не нужно:

```kotlin
class AndroidRoborazziConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("io.github.takahirom.roborazzi")
        dependencies {
            add("testImplementation", libs.findLibrary("robolectric").get())
            add("testImplementation", libs.findLibrary("roborazzi").get())
            add("testImplementation", libs.findLibrary("roborazzi-compose").get())
            add("testImplementation", libs.findLibrary("roborazzi-junit-rule").get())
            add("testImplementation", libs.findLibrary("androidx-compose-ui-test-junit4").get())
            add("testImplementation", libs.findLibrary("androidx-test-ext-junit").get())
            add("debugImplementation", libs.findLibrary("androidx-compose-ui-test-manifest").get())
        }
    }
}
```

Сам screenshot-тест:

```kotlin
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h640dp-xhdpi")
class TodosScreenScreenshotTest {
    @get:Rule val composeRule = createComposeRule()

    @get:Rule
    val roborazziRule = RoborazziRule(
        options = RoborazziRule.Options(outputDirectoryPath = "src/test/screenshots"),
    )

    @Test
    fun todosScreenReady() {
        composeRule.setContent {
            StitTheme(darkTheme = false) {
                TodosScreen(
                    state = TodosUiState.Ready(
                        listOf(
                            Todo(TodoId("1"), "Прочитать бриф"),
                            Todo(TodoId("2"), "Собрать гейт", done = true),
                        ),
                    ),
                    onToggle = {},
                )
            }
        }
        composeRule.onRoot().captureRoboImage("src/test/screenshots/todos_ready.png")
    }

    @Test
    fun todosScreenLoading() { /* тёмная тема, состояние Loading */ }
}
```

Результат (проверено): `./gradlew recordRoborazziDebug` за **8.6 с** положил
`feature/todos/src/test/screenshots/todos_ready.png` и `todos_loading_dark.png`.
**Я открыл `todos_ready.png` как изображение** — на нём читаемый заголовок «Задачи»,
пункт «Прочитать бриф» и зачёркнутый «Собрать гейт». То есть агент на Linux может
*посмотреть* на свой UI и оценить результат, не имея ни устройства, ни эмулятора,
ни Mac. Это, вероятно, самый ценный факт всего потока.

Таски Roborazzi (проверено `gradlew tasks --all`): `recordRoborazziDebug`,
`verifyRoborazziDebug`, `compareRoborazziDebug`, `verifyAndRecordRoborazziDebug`,
`clearRoborazziDebug`, `restoreOutputDirRoborazziDebug`. В гейт идёт
`verifyRoborazziDebug`; при расхождении Roborazzi пишет `*_compare.png`
(эталон | новый | diff) — тоже пригодный для чтения агентом артефакт.

Рядом — второй, «официальный» путь: **Compose Preview Screenshot Testing**
(плагин `com.android.compose.screenshot`). По докам (проверено чтением
developer.android.com 2026-09-17): версия **0.0.1-alpha16** — то есть спустя два
года всё ещё alpha, а сам standalone-плагин **депрекирован с AGP 9.5.0-alpha03** в
пользу AGP test suites. Тесты хостовые (устройство не нужно), задачи
`updateDebugScreenshotTest` / `validateDebugScreenshotTest`, эталоны в
`src/screenshotTestDebug/reference/`, требуется
`android.experimental.enableScreenshotTest=true` и часто
`android.compose.screenshot.maxHeapSize=4g`. Вывод: **для шаблона сейчас Roborazzi**
(стабильный 1.74.0, богаче API, уже работает), а плагин Google держать в поле
зрения до выхода из alpha в форме test suites.

### 2.11 Android Lint

```kotlin
lint {
    warningsAsErrors = true
    abortOnError = true
    checkDependencies = true
    xmlReport = true
    sarifReport = true
    htmlReport = false
    textReport = true
    disable += "GradleDependency"     // иначе шумит на каждое доступное обновление
}
```

Реальные находки на пустом месте (все исправлены в spike'е, не заглушены):
`MissingApplicationIcon`, `DataExtractionRules` (атрибут `android:allowBackup`
депрекирован с Android 12; нужны `android:dataExtractionRules` **и**
`android:fullBackupContent`, если `minSdk < 31`), `ObsoleteSdkInt` (папка
`mipmap-anydpi-v26` не нужна при `minSdk = 26`).

Важное предупреждение, которое лечится одной строкой в convention-плагине
JVM-модуля:

```
Warning: Lint will treat :core:model as an external dependency and not analyze it.
 * Recommended Action: Apply the 'com.android.lint' plugin to java library project :core:model.
```

→ в `JvmLibraryConventionPlugin` добавлено `pluginManager.apply("com.android.lint")`.

**Baseline мы не заводим.** При наличии `baseline = file("lint-baseline.xml")` и
отсутствии файла lint *создаёт* его и **всё равно валит сборку** («Also breaking the
build in case this was not intentional») — поведение, которое агент почти наверняка
поймёт неправильно и закоммитит baseline с долгом. Шаблон стартует с нулём долгов;
для миграции чужого кода есть `./gradlew updateLintBaseline`.

### 2.12 Подпись и релизные артефакты

Ключ генерится **вне репозитория**, путь и пароли — из `gradle.properties`/env:

```bash
keytool -genkeypair -v -keystore ~/.cache/stit-mobile-research/keys/stit-spike.jks \
  -storetype JKS -alias stit -keyalg RSA -keysize 2048 -validity 365 \
  -storepass … -keypass … -dname "CN=stit spike, OU=research, O=stit, C=RU"
```

```kotlin
signingConfigs {
    create("releaseLocal") {
        // Ключ живёт вне репозитория; путь/пароли — из gradle.properties или env.
        val storePath = providers.gradleProperty("stitKeystorePath")
            .orElse(providers.environmentVariable("STIT_KEYSTORE_PATH"))
        if (storePath.isPresent) {
            storeFile = file(storePath.get())
            storePassword = providers.environmentVariable("STIT_KEYSTORE_PASSWORD").getOrElse("")
            keyAlias = providers.environmentVariable("STIT_KEY_ALIAS").getOrElse("")
            keyPassword = providers.environmentVariable("STIT_KEY_PASSWORD").getOrElse("")
        }
    }
}
buildTypes {
    debug { applicationIdSuffix = ".debug" }
    release {
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        signingConfig = signingConfigs.getByName("releaseLocal")
    }
}
```

Результаты (проверено):

| артефакт | размер | время |
|---|---|---|
| `app-release.apk` (R8 + shrinkResources, подписан) | **976 644 B** | `assembleRelease` 58 с холодный / 25 с в гейте |
| `app-release.aab` | **2 391 669 B** | `bundleRelease` +2.3 с поверх release |
| `mapping.txt` (R8) | 19.8 МБ | — |
| baseline profile | генерится AGP автоматически (`outputs/apk/release/baselineProfiles`) | |

Проверка подписи:

```
$ $ANDROID_HOME/build-tools/37.0.0/apksigner verify --print-certs -v app-release.apk
Verifies
Verified using v2 scheme (APK Signature Scheme v2): true
Number of signers: 1
V2 Signer: certificate DN: CN=stit spike, OU=research, O=stit, L=NA, ST=NA, C=RU
V2 Signer: certificate SHA-256 digest: 103b87e364eecbd57209fcd26f403330fdb2865120ddc13351c82a9e78266e20
```

Замечание: подписалось **только v2** (v1 JAR-signing выключен, v3/v3.1 не включены).
Для стора этого достаточно при `minSdk 26`, но в шаблоне стоит явно
`enableV3Signing = true` / `enableV4Signing` (см. поток 05).

### 2.13 DI: Hilt + KSP на Kotlin 2.4.20 — работает

Опасение было обоснованным: последняя KSP — **2.3.12** (KSP2 перешёл на
независимое версионирование, ветки `2.4.x` пока нет), а Kotlin у нас 2.4.20.
Проверено эмпирически: связка **Kotlin 2.4.20 + KSP 2.3.12 + Hilt 2.60.1 + AGP 9.4.0
собирается и работает** — задачи `:app:kspDebugKotlin`, `:app:hiltAggregateDepsDebug`,
`:app:hiltJavaCompileDebug`, `:feature:todos:kspDebugUnitTestKotlin` прошли,
`@HiltAndroidApp`/`@AndroidEntryPoint`/`@HiltViewModel`/`@Binds` отработали.
Альтернативы на случай, если связка разъедется (не проверялись, только версии):
Koin 4.2.2 (runtime, без кодогена) и Metro 1.4.4 (компиляторный плагин, `dev.zacsweers.metro`).

Замечено попутно (депрекация 2026 года): `androidx.hilt.navigation.compose.hiltViewModel`
**переехал** в `androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel`; старый путь
предупреждает, а с `warningsAsErrors` для Kotlin это стало бы ошибкой.

### 2.14 Что ещё поменялось в API (замечено компилятором, полезно потокам 01/03)

- `androidx.compose.ui.test.junit4.createComposeRule` **депрекирован** в пользу
  `androidx.compose.ui.test.junit4.v2.createComposeRule`: «v2 APIs use
  StandardTestDispatcher instead of UnconfinedTestDispatcher… Tests relying on
  immediate execution may require explicit synchronization».
- Официальный шаблон `android create empty-activity` **не применяет** плагин
  `org.jetbrains.kotlin.android` вообще — только `com.android.application` +
  `kotlin.plugin.compose` + `kotlin.plugin.serialization`. Это **встроенная
  поддержка Kotlin в AGP 9**. Версия Kotlin при этом определяется тем, что попало
  на classpath: AGP 9.4.0 в своём POM тянет `kotlin-gradle-plugin:2.2.10`, а наш
  корневой `alias(libs.plugins.kotlin.android) apply false` поднимает его до 2.4.20.
  Для шаблона это надо делать **явно**, а не полагаться на транзитивный resolve.
- Шаблон Google по умолчанию — **Navigation 3** (`androidx.navigation3:navigation3-runtime`
  + `navigation3-ui` + `androidx.lifecycle:lifecycle-viewmodel-navigation3`,
  `NavDisplay`/`rememberNavBackStack`/`entryProvider`/`NavKey`), а не Navigation 2 Compose.
  Стабильная навигация 3 на Google Maven — **1.1.7** (1.2.0-rc01 в пути);
  `navigation-compose` (2.x) — 2.10.1.

---

## 3. Таблица измерений

Единая методика: `date +%s.%N` вокруг вызова; «холодный» = после `./gradlew --stop`
и удаления `build/`, `.gradle/` проекта (кеши в `~/.gradle` сохранены — это состояние
«свежий клон на обжитой машине»); «тёплый» = демон жив, ничего не менялось;
«инкремент» = изменён один файл в `core/model`.

### 3.1 Разовая установка

| шаг | время | диск после |
|---|---|---|
| `curl` cmdline-tools 23.0 (181 МБ) | 25 с | — |
| `unzip` cmdline-tools | 0.6 с | 174 МБ |
| `android sdk install platform-tools platforms/android-37.2 build-tools/37.0.0` | **168 с** | 513 МБ |
| `android create` (доставил `platforms/android-36`) | ~30 с | 806 МБ |
| Temurin JDK 21 (207 МБ) | 158 с | +346 МБ |
| `./gradlew --version` (дистрибутив Gradle 9.7.1) | **20 с** | `~/.gradle` 309 МБ |
| первый `./gradlew help` (компиляция `build-logic`, резолв всех плагинов) | **140 с** | `~/.gradle` ~1.5 ГБ |
| первый `:app:assembleDebug` (скачивание Compose BOM, Hilt, KSP…) | **115 с** (упал) → **48 с** (успех) | — |
| первый `test` (Robolectric тянет `android-all`) | **152 с** | — |
| первый `detekt` (скачивание detekt + compose-rules) | **142 с** | — |
| первый `spotlessCheck` (ktlint 1.8.0) | **33 с** | — |
| Kotlin/Native prebuilt 2.4.20 (в `:shared:compileKotlinIosArm64`) | **66 с** | `~/.konan` 822 МБ |
| foojay-provisioned JDK 17 (в `:shared:jvmTest`) | **79 с** | `~/.gradle/jdks` 502 МБ |
| Swift 6.4 тарболл (1.12 ГБ) + распаковка | ~2 мин + 14 с | 3.6 ГБ |
| SwiftLint 0.65.1 Linux (67 МБ zip) | 16 с | 245 МБ |

### 3.2 Отдельные шаги гейта

| шаг | холодный | тёплый | инкремент | заметка |
|---|---|---|---|---|
| `help` (конфигурация) | 140 с (первый, с компиляцией build-logic) | **1.1 с** | 14 с (после правки convention-плагина) | configuration cache даёт «Reusing configuration cache» |
| `spotlessCheck` | 14 с | **1–2 с** | 1 с | падает с diff-ом и подсказкой `./gradlew spotlessApply` |
| `detekt` (6 модулей) | 1 с (после прогрева) / 142 с самый первый | **0.5–1 с** | 1 с | параллельно, конфиг валидируется |
| `lint` (все модули) | **44 с** | 0–1 с | 7 с | самый тяжёлый по памяти |
| `test` (все модули) | 16 с | **1 с** | 14 с | 16 тестов в 6 классах |
| `verifyRoborazziDebug` | 12 с | 1 с | 11 с | 2 скриншота |
| `koverVerify` | 5 с | 1 с | 2 с | агрегирует 5 модулей |
| `:app:assembleDebug` | 2 с (из build-cache) / 48 с без кеша | 1 с | 1 с | |
| `:app:assembleRelease` (R8 + подпись) | **25 с** (в гейте) / 58 с первый | — | — | |
| `:app:bundleRelease` | 2.3 с поверх release | — | — | |
| `recordRoborazziDebug` | 8.6 с | — | — | |
| `buildHealth` (dependency-analysis) | 8.5 с | — | — | **бесполезен**: «No project health reports found» на AGP 9.4 |

### 3.3 Гейт целиком

```
$ make check        # тёплый, ничего не менялось
→ gate: android-gate
OK:   format                   2s
OK:   detekt                   0s
OK:   android lint             0s
OK:   unit tests               1s
OK:   screenshots              0s
OK:   coverage                 1s
OK:   assembleDebug            0s
✓ check clean
                                             ИТОГО 4.2 с, 9 строк вывода
```

```
$ make check        # после правки одного файла в core/model (с намеренной ошибкой)
→ gate: android-gate
OK:   format                   1s
FAIL: detekt                   1s  (лог: build/gate-logs/detekt.log)
…/core/model/src/main/kotlin/…/Todo.kt:24:24: Private function `unusedOld` is unused. [UnusedPrivateMember]
FAILURE: Build failed with an exception.
> Analysis failed with 1 weighted issues.
OK:   android lint             7s
OK:   unit tests              14s
OK:   screenshots             11s
OK:   coverage                 2s
OK:   assembleDebug            1s
✗ check: провалено шагов — 1
                                             ИТОГО 37.0 с, 12 строк вывода
```

| сценарий | время | строк вывода |
|---|---|---|
| `make check` — ничего не менялось | **4.2 с** | 9 |
| `make check` — правка одного файла | **37.0 с** | 12 |
| `make check` — свежий клон, тёплый `~/.gradle` | **94.3 с** | 9 |
| `GATE_RELEASE=1 make check` (плюс подписанный релиз) | +25 с → **32.5 с** тёплый | 10 |

### 3.4 Память

```
$ ps -u $(id -u) -o pid,rss,args | grep java
2759428   3661 MB   …/jdk-21/bin/java … GradleDaemon
2761620   1171 MB   …/jdk-21/bin/java … KotlinCompileDaemon
                    Σ  4832 MB
```

При `org.gradle.jvmargs=-Xmx2g` и `kotlin.daemon.jvmargs=-Xmx1g` фактический RSS
пары демонов — **4.8 ГБ**. Разница с `-Xmx` — metaspace, code cache, native-память
и mmap'нутые jar'ы. Это надо закладывать в требования шаблона: **минимум 6 ГБ
свободной RAM** на Android-гейт (с эмулятором — 10+).

### 3.5 Диск в конце прогона

| путь | размер |
|---|---|
| `~/.gradle` | 4.0 ГБ (`caches` 3.2 · `modules-2` 1.2 · `build-cache-1` 144 МБ · `wrapper` 309 МБ · `jdks` 502 МБ) |
| `~/.konan` | 822 МБ |
| `~/.cache/stit-mobile-research/android-sdk` | 806 МБ |
| `~/.cache/stit-mobile-research/jdk` | 346 МБ |
| `~/.cache/stit-mobile-research/swift` | 3.6 ГБ |
| `~/.cache/stit-mobile-research/swiftlint` | 245 МБ |
| `android-gate/**/build` | ~200 МБ |
| `swift-linux/.build` | 657 МБ (52 МБ без swift-openapi-*) |

---

## 4. Шум, детерминизм и concise-обёртки

### 4.1 Что именно шумит в Gradle (замерено)

| источник | как выглядит | лечение |
|---|---|---|
| `> Task :…` на каждую задачу | 110–257 строк на прогон | `--console=plain` не убирает; фильтровать `grep -v '^> Task'` или собирать лог в файл |
| «Deprecated Gradle features were used…» | 5 строк в **каждом** прогоне | `--warning-mode=none` |
| «The Dependency Analysis plugin is only known to work with AGP between 8.10.0 and 9.3.1» | 1 строка в каждом прогоне | убрать плагин из гейта |
| «Parallel Configuration Cache is an incubating feature» / «Type-safe project accessors is an incubating feature» | 2 строки | цена за фичи; можно фильтровать |
| «[Incubating] Problems report is available at …html» | 1 строка | фильтровать |
| `w: file:///…` предупреждения Kotlin | по 3–4 строки на каждое (длинный текст депрекации) | либо чинить, либо `allWarningsAsErrors` — тогда это ошибки, не шум |
| падение Android Lint по метаспейсу | **13.8 КБ одного стектрейса**, три вложенных «Stack: \`…\`» с экранированием | поднять метаспейс; в обёртке — обрезать |
| «AAPT: error: resource mipmap/ic_launcher not found» после переименования ресурсной папки | ложное, лечится пере-сборкой (`--rerun-tasks`) | знать |

Худший случай: провалившийся `lintAnalyzeDebug` выдал **13 815 байт** сплошного
стектрейса, где полезной информации — три слова (`Message: Metaspace`). Агент,
читающий сырой вывод, потратит на это контекст и может не найти причину.

### 4.2 Что, наоборот, отлично машиночитаемо

| артефакт | путь | зачем агенту |
|---|---|---|
| JUnit XML | `<module>/build/test-results/testDebugUnitTest/TEST-*.xml` | `tests/failures/errors/time` по классам без парсинга текста |
| detekt txt | `<module>/build/reports/detekt/detekt.txt` | одна находка = одна строка `Rule - [Entity] at file:line:col` |
| detekt SARIF | `<module>/build/reports/detekt/detekt.sarif` | нормальный машинный формат, если понадобится агрегировать |
| lint SARIF + XML + txt | `app/build/reports/lint-results-debug.{sarif,xml}` | |
| Kover XML | `build/reports/kover/report.xml` — `<counter type="LINE" missed=… covered=…>` | точный процент без regex по логу |
| Roborazzi PNG | `feature/todos/src/test/screenshots/*.png` и `*_compare.png` | **агент их видит глазами** |
| `output-metadata.json` | `app/build/outputs/apk/*/output-metadata.json` | путь и имя артефакта для релиза |
| `android describe` | JSON-карта таргетов и артефактов | замена ручному поиску APK |

Вывод: **гейт должен печатать одну строку на шаг и оставлять машинные артефакты на
диске**, а не вываливать всё в stdout.

### 4.3 Concise-обёртка (в стиле `scripts/tools/test.sh` Go-шаблона)

`scripts/tools/gradle-step.sh` — один шаг:

```bash
#!/usr/bin/env bash
# Один шаг гейта: гоняет Gradle-таск, печатает одну строку OK:/FAIL:.
# При падении — только релевантный кусок лога (а не 400 строк Gradle-шума).
set -uo pipefail
LABEL="$1"; shift
LOGDIR="${STIT_LOGDIR:-build/gate-logs}"
mkdir -p "$LOGDIR"
LOG="$LOGDIR/${LABEL// /_}.log"
start=$(date +%s)
./gradlew "$@" --console=plain --warning-mode=none > "$LOG" 2>&1
rc=$?
dur=$(( $(date +%s) - start ))
if [ $rc -eq 0 ]; then
  printf 'OK:   %-22s %3ds\n' "$LABEL" "$dur"
else
  printf 'FAIL: %-22s %3ds  (лог: %s)\n' "$LABEL" "$dur" "$LOG"
  grep -E '^(e:|w: .*error)|^\S+\.(kt|kts|xml|java):[0-9]+|Analysis failed|There were failing tests|format violations|Error:|FAILURE:' "$LOG" \
    | grep -vE 'Run with --|Get more help|^\* Try|Deprecated Gradle' | head -25
fi
exit $rc
```

`scripts/tools/check.sh` — сам гейт:

```bash
#!/usr/bin/env bash
set -uo pipefail
cd "$(dirname "$0")/../.."
STEP=scripts/tools/gradle-step.sh
fails=0
run() { bash "$STEP" "$@" || fails=$((fails+1)); }

echo "→ gate: android-gate"
run "format"        spotlessCheck
run "detekt"        detekt
run "android lint"  lint
run "unit tests"    test
run "screenshots"   verifyRoborazziDebug
run "coverage"      koverVerify
run "assembleDebug" :app:assembleDebug
[ "${GATE_RELEASE:-0}" = "1" ] && run "assembleRelease" :app:assembleRelease

[ $fails -eq 0 ] && echo "✓ check clean" || echo "✗ check: провалено шагов — $fails"
exit $fails
```

Замечание по порядку шагов: **format идёт первым** (он самый дешёвый и чаще всего
и ломается), **lint и tests — в середине**, **assemble — последним**. Гейт
намеренно **не** `fail-fast`: агенту полезнее получить все провалившиеся шаги за
один прогон, чем узнавать о них по одному.

`scripts/tools/doctor.sh` — fail-fast preflight (прямой аналог `make doctor` Go-шаблона):

```
$ make doctor
JDK                          OK: javac 21.0.12.1
Android SDK                  OK: /home/mm/.cache/…/android-sdk (android-36 android-37.2 )
SDK licenses                 OK
RAM                          OK: 7734MB
✓ doctor clean
```

Он ловит ровно те три грабли, на которые я наступил: нет `javac`, нет `ANDROID_HOME`,
мало памяти (порог 3 ГБ → предупреждение про Metaspace).

### 4.4 Детерминизм

| инструмент | детерминизм | комментарий |
|---|---|---|
| detekt | **полный** | одинаковый вывод на повторах, `config.validation` ловит опечатки |
| Spotless/ktlint | **полный** | `spotlessApply` идемпотентен при корректном `.editorconfig` |
| Android Lint | **полный** по находкам; **нестабилен по памяти** | при нехватке метаспейса падает с внутренней ошибкой, а не с «мало памяти» |
| Konsist | **полный** | сканирует диск, не зависит от порядка сборки |
| Robolectric | стабилен, но **медленный на первом классе** (25.9 с против 2.5 с на следующих — скачивание `android-all`) | |
| Roborazzi | **побайтовое сравнение PNG**; на одной и той же машине/JDK стабильно | между разными JVM/архитектурами рендер может отличаться — эталоны надо писать в одном окружении (для нас = локально, CI-раннеров в экосистеме нет) |
| Kover | полный | |
| R8 | детерминирован при фиксированной версии build-tools | |

---

## 5. KMP на Linux (ответ потоку 03)

Модуль `:shared` (Kotlin Multiplatform 2.4.20) с таргетами `androidLibrary` (новый
AGP-9-плагин `com.android.kotlin.multiplatform.library`), `jvm()`, `iosArm64()`,
`iosSimulatorArm64()`, `iosX64()`:

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
}

kotlin {
    jvmToolchain(17)

    androidLibrary {
        namespace = "tech.stit.mobiletemplate.shared"
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = libs.versions.minSdk.get().toInt()
    }

    jvm()
    listOf(iosArm64(), iosSimulatorArm64(), iosX64()).forEach { target ->
        target.binaries.framework { baseName = "StitShared"; isStatic = true }
    }

    sourceSets {
        commonMain.dependencies { implementation(libs.kotlinx.coroutines.core) }
        commonTest.dependencies { implementation(kotlin("test")) }
    }
}
```

### Результаты (все проверены)

| проверка | результат | время | комментарий |
|---|---|---|---|
| `:shared:jvmTest` | ✅ **проходит** | 79 с первый (включая скачивание JDK 17 через foojay), далее секунды | быстрый агентный цикл по общему коду |
| `:shared:compileKotlinIosArm64` | ✅ **klib собирается на Linux** | 66 с (обе iOS-цели, включая скачивание Kotlin/Native) | артефакт: `shared/build/classes/kotlin/iosArm64/main/klib/shared/default/{ir,linkdata,manifest,targets}` |
| `:shared:compileKotlinIosSimulatorArm64` | ✅ **klib собирается** | (в тех же 66 с) | |
| `:shared:linkDebugFrameworkIosArm64` | ⚠️ **SKIPPED, exit code 0** | 0.5 с | framework не создаётся, каталога `shared/build/bin` нет; **никакого предупреждения** — даже при `kotlin.native.ignoreDisabledTargets=false` |
| `:shared:iosSimulatorArm64Test` | ⛔ отключён с внятным сообщением | — | `Task 'iosSimulatorArm64Test' for target 'ios_simulator_arm64' cannot run on the current host (linux-x86_64). Reason: simulator tests require macOS` |
| `~/.konan` | **822 МБ** (`kotlin-native-prebuilt-linux-x86_64-2.4.20`) | — | одна цена на все iOS-таргеты |

**Главный вывод для потока 03.** На Linux агент может:
- писать и **компилировать** общий Kotlin-код под iOS (klib) — то есть ловить
  ошибки типов, `expect/actual`, зависимостей **без Mac**;
- прогонять всю общую логику тестами через `jvmTest` (секунды);
- **не может** собрать `.framework` (линковка требует Apple-линкера и SDK) и
  **не может** прогнать `iosTest` (нужен симулятор → macOS).

**Ловушка, которую обязательно надо обойти в гейте**: `linkDebugFrameworkIosArm64`
на Linux не «падает», а тихо пропускается с кодом 0. Если поставить link-таск в
`make check`, гейт будет зелёным при полностью несобирающемся framework. Правильно:
- в Linux-гейт кладём **только `compileKotlinIos*`** (`make kmp-ios`);
- линковку и `iosTest` — в Mac-часть гейта;
- дополнительно в скрипте проверять **наличие файла** артефакта, а не только код возврата.

Побочные наблюдения:
- `kotlin.native.ignoreDisabledTargets=true` глушит предупреждения о `*Test`-тасках —
  тише, но и честного сигнала меньше. Для шаблона: включать (иначе каждый прогон
  тащит 8 строк), но в документации явно писать, что iOS-таргеты не тестируются на Linux.
- AGP 9 `androidLibrary {}` предупреждает: `The 'commonTest' source directory exists,
  but android host tests are not enabled. To enable android host tests, add
  `withHostTest {}` to your android target configuration` — то есть `commonTest` на
  Android-таргете по умолчанию **не запускается**; это надо включать явно.
- Compose Multiplatform в spike не проверялся (стабильная версия плагина
  `org.jetbrains.compose:compose-gradle-plugin` — **1.12.0**, 1.13.0-alpha01 в пути);
  `jvm`-desktop-таргет CMP на Linux собирается по докам, но это отдельный час работы
  и к вопросу «iOS на Linux» отношения не имеет.

---

## 6. Swift на Linux

### 6.1 Установка — см. §1.5. Итог: работает из коробки на Debian 13, `apt` не нужен

Отдельно проверено: `ldd` на `swift-frontend` не показывает ни одной ненайденной
библиотеки на голом Debian 13 (в тарболле свои `libcurl`, `libxml2`, ICU и т. д.).
Это снимает классическую боль «Swift на Linux требует полтора десятка `-dev` пакетов».
`swiftly` не понадобился; для шаблона он всё же интересен как менеджер версий
(`SWIFTLY_HOME_DIR`/`SWIFTLY_BIN_DIR` позволяют держать его целиком в проектном каталоге).

### 6.2 Spike `swift-linux`

```
swift-linux/
├── Package.swift                 # swift-tools-version: 6.2
├── .swift-format                 # конфиг форматтера (отступ 4, lineLength 120, строгие правила)
├── .swiftlint.yml                # профиль линтера + custom_rules
├── Sources/
│   ├── Domain/                   # Todo, TodoID, TodoError, TodoRepository (protocol), SortTodosForDisplay
│   ├── Data/                     # InMemoryTodoRepository — actor
│   └── ApiClient/
│       ├── openapi.yaml          # OpenAPI 3.1 в стиле huma: cookie-auth, /api/auth/login, /api/v1/todos
│       ├── openapi-generator-config.yaml
│       └── TodoApi.swift         # тонкая обёртка над сгенерированным клиентом
└── Tests/
    ├── DomainTests/              # Swift Testing: @Suite/@Test/#expect + параметризованный тест
    └── DataTests/                # async-тесты actor'а, await #expect(throws:)
```

`Package.swift`:

```swift
// swift-tools-version: 6.2
import PackageDescription

let package = Package(
    name: "StitMobileCore",
    platforms: [.iOS(.v17), .macOS(.v14)],
    products: [
        .library(name: "Domain", targets: ["Domain"]),
        .library(name: "Data", targets: ["Data"]),
        .library(name: "ApiClient", targets: ["ApiClient"]),
    ],
    dependencies: [
        .package(url: "https://github.com/apple/swift-openapi-generator", from: "1.13.1"),
        .package(url: "https://github.com/apple/swift-openapi-runtime", from: "1.12.1"),
        .package(url: "https://github.com/apple/swift-openapi-urlsession", from: "1.3.1"),
    ],
    targets: [
        .target(name: "Domain"),
        .target(name: "Data", dependencies: ["Domain"]),
        .target(
            name: "ApiClient",
            dependencies: [
                .product(name: "OpenAPIRuntime", package: "swift-openapi-runtime"),
                .product(name: "OpenAPIURLSession", package: "swift-openapi-urlsession"),
            ],
            plugins: [.plugin(name: "OpenAPIGenerator", package: "swift-openapi-generator")]
        ),
        .testTarget(name: "DomainTests", dependencies: ["Domain"]),
        .testTarget(name: "DataTests", dependencies: ["Domain", "Data"]),
    ]
)
```

Мелочь, на которой спотыкается и человек, и агент: **порядок именованных аргументов
в `Package` фиксирован** — `products` обязан идти до `dependencies`, иначе
`error: argument 'products' must precede argument 'dependencies'`.

### 6.3 Swift Testing — да, и он быстрый

```swift
@Suite("SortTodosForDisplay")
struct SortTodosForDisplayTests {
    @Test("невыполненные идут первыми, внутри группы — по заголовку")
    func ordering() {
        let sorted = SortTodosForDisplay()([
            Todo(id: TodoID("1"), title: "Beta", done: true),
            Todo(id: TodoID("2"), title: "alpha"),
            Todo(id: TodoID("3"), title: "Gamma"),
        ])
        #expect(sorted.map(\.title) == ["alpha", "Gamma", "Beta"])
    }

    @Test("регистр в заголовке не влияет на порядок", arguments: [
        (["b", "A"], ["A", "b"]), (["Z", "a"], ["a", "Z"]), (["mm", "MA"], ["MA", "mm"]),
    ])
    func caseInsensitive(input: [String], expected: [String]) { … }
}
```

```swift
@Test("пустой заголовок отвергается")
func rejectsEmptyTitle() async {
    let repo = InMemoryTodoRepository()
    await #expect(throws: TodoError.emptyTitle) { _ = try await repo.add(title: "   ") }
}
```

| метрика | значение |
|---|---|
| `swift build` холодный | **4.1 с** |
| `swift test` холодный | **2.5 с** |
| `swift test` тёплый | **0.72 с** |
| `swift test --parallel --enable-code-coverage` | 2.1 с |
| `.build` | 52 МБ (до подключения OpenAPI-зависимостей) |
| тестов | 6 (2 suite; параметризованный даёт 3 кейса) |
| кириллица в именах тестов и `@Suite`/`@Test` | ✅ работает, в выводе читается |

Вывод Swift Testing очень «агентный»: каждая строка — `✔`/`✘` + имя теста + время,
и он показывает каждый кейс параметризованного теста отдельно.

Покрытие: SwiftPM кладёт `llvm-cov`-профиль в `$(swift build --show-bin-path)/codecov/`.
**Важное отличие Swift 6.4**: классический рецепт
`llvm-cov report .build/debug/<Pkg>PackageTests.xctest` **не работает** — в новой
системе сборки бинарники лежат в `.build/out/Products/Debug-linux-x86_64/` и разбиты
по таргетам (`DomainTests-test-runner`, `DataTests-test-runner`), единого
`*.xctest` нет. Рабочий путь — JSON-экспорт, который SwiftPM пишет сам:

```bash
BIN=$(swift build --show-bin-path)
python3 -c "
import json; d=json.load(open('$BIN/codecov/StitMobileCore.json'))
t=d['data'][0]['totals']
print('lines', t['lines']['percent'])"
# → lines 98.46 %   (64/65), functions 93.8 %, regions 96.2 %
```

### 6.4 `swift-format` (в тулчейне) — форматтер

```json
{
  "version": 1,
  "indentation": { "spaces": 4 },
  "lineLength": 120,
  "respectsExistingLineBreaks": true,
  "prioritizeKeepingFunctionOutputTogether": true,
  "rules": {
    "AlwaysUseLowerCamelCase": true,
    "AmbiguousTrailingClosureOverload": true,
    "NeverForceUnwrap": true,
    "NeverUseForceTry": true,
    "NeverUseImplicitlyUnwrappedOptionals": true,
    "OrderedImports": true,
    "UseLetInEveryBoundCaseVariable": true,
    "UseShorthandTypeNames": true,
    "UseSynthesizedInitializer": true,
    "ValidateDocumentationComments": true,
    "AllPublicDeclarationsHaveDocumentation": false
  }
}
```

| команда | поведение | время |
|---|---|---|
| `swift format lint --strict --recursive Sources Tests` | **rc=1** и построчные `file:line:col: error: [Rule] …` | **0.028 с** |
| `swift format --in-place --recursive Sources Tests` | автофикс | 0.025 с |
| повторный lint | **rc=0** | — |

Дефолт `swift-format` — отступ **2 пробела**; без `.swift-format` с `"spaces": 4`
он завалит любой код, написанный в 4. Версия, встроенная в тулчейн 6.4, отдаёт
`swift format --version` → `main` (не семвер) — для шаблона это значит «версия
форматтера = версия тулчейна», что удобно: одна цифра в `.swift-version`.

### 6.5 SwiftLint 0.65.1 на Linux — да, официальный бинарь

В релизе `0.65.1` есть ассеты `swiftlint_linux_amd64.zip` (67 МБ) и
`swiftlint_linux_arm64.zip`. Внутри два бинаря: динамический `swiftlint` (97 МБ) и
`swiftlint-static` (93 МБ). `ldd` чист, `swiftlint version` → `0.65.1`.

```
$ swiftlint lint --strict --quiet
…/SortTodosForDisplayTests.swift:12:50: error: Trailing Comma Violation: … (trailing_comma)
rc=2,  время 0.18 с
```

`--fix` чинит на месте. `analyzer_rules` (`unused_declaration`, `unused_import`)
требуют `swiftlint analyze --compiler-log-path`, то есть предварительной сборки —
в базовый гейт их лучше не включать, а вынести в отдельную верб `make analyze`.

### 6.6 Swift OpenAPI Generator 1.13.1 на Linux — работает

`openapi-generator-config.yaml`:

```yaml
generate: [types, client]
accessModifier: public
namingStrategy: idiomatic
```

Сборка (`swift build --product ApiClient`): **59 с** холодная с резолвом зависимостей,
**12.5 с** сама компиляция. Сгенерировано **911 строк** в
`.build/plugins/outputs/swift-linux/ApiClient/destination/OpenAPIGenerator/GeneratedSources/`:
`Client.swift` (241), `Types+Operations.swift` (423), `Types+Components+Schemas.swift` (97) и др.
Генератор сам расставил `#if os(Linux)` вокруг `public import struct Foundation.URL/Data/Date`
— то есть кросс-платформенность учтена авторами.

**Критично для потока 07**: генератор **полностью игнорирует `securitySchemes`**.
В сгенерированном коде нет ни одного упоминания cookie/security — только
`setCookie: Swift.String?` как заголовок ответа, потому что я объявил его явно в
`responses.204.headers`. Значит:

- cookie-аутентификация (наш контракт `access_token`/`refresh_token`) реализуется
  **`ClientMiddleware`** вручную;
- `swift-openapi-urlsession` на iOS отдаёт куки в `HTTPCookieStorage` «сам»
  (по докам), но на Linux `URLSession` живёт в `FoundationNetworking` — поведение
  надо проверять отдельно;
- рефреш токена (`401 → POST /api/auth/refresh → retry`) — тоже middleware.

Для генерации в CI/агентом без сборки есть CLI-режим (`swift-openapi-generator generate`),
но плагин SwiftPM удобнее: код не коммитится, регенерируется при каждой сборке,
дрейф контракта невозможен по построению.

### 6.7 Что на Linux невозможно (проверено компилятором)

```
$ swiftc -typecheck UI.swift
UI.swift:1:8: error: no such module 'SwiftUI'
1 | import SwiftUI
  |        `- error: no such module 'SwiftUI'

$ swiftc -typecheck UIK.swift
UIK.swift:1:8: error: no such module 'UIKit'
```

```
$ swiftc -typecheck Net.swift     # import Foundation; let s = URLSession.shared
Net.swift:2:20: error: type 'URLSession' (aka 'AnyObject') has no member 'shared'
```

То есть на Linux:

| возможность | статус |
|---|---|
| `Foundation` (модели, `Codable`, `Date`, строки) | ✅ swift-foundation, работает |
| `URLSession` | ⚠️ только через `#if canImport(FoundationNetworking) import FoundationNetworking` |
| `SwiftUI`, `UIKit`, `WidgetKit`, `CoreData`, `AVFoundation`, … | ⛔ модулей нет |
| `XCTest` unit-тесты | ✅ есть (в тулчейне), но UI-тесты (`XCUITest`) — нет |
| Swift Testing | ✅ 6.4, полностью |
| симулятор, `xcodebuild`, `xcresult`, подпись, App Store | ⛔ только macOS |
| SwiftLint / swift-format / sourcekit-lsp | ✅ нативно |
| Swift OpenAPI Generator | ✅ |
| Swift на Android | по докам: в релизе 6.4 есть отдельный **Android SDK** (`android-sdk` в списке платформ `releases.json`) — предмет для потока 03, не проверялось |

### 6.8 xtool — сборка iOS-приложений на Linux (по докам, **не** проверялось)

`github.com/xtool-org/xtool`, MIT, ~5.5k звёзд, версия 1.2.0. Заявляет:
собрать SwiftPM-пакет в iOS-приложение, подписать и установить на устройство,
работать с Apple Developer Services — с Linux/WSL/macOS.

Что требуется (по DeepWiki/форуму Swift):
- **`Xcode.xip`, скачанный с developer.apple.com** — из него `xtool setup` извлекает
  iOS SDK и собирает Swift SDK с именем `darwin`;
- Apple ID: либо App Store Connect API Key (платное членство), либо
  password-flow для бесплатного Apple ID с «анизет»-провижнингом.

Оценка для шаблона: **не брать**. Извлечение Darwin SDK из Xcode.xip находится в
серой зоне Xcode EULA (лицензия ограничивает использование Apple SDK машинами Apple),
симулятора нет, App Store-подача через xtool не декларирована, а весь UI-цикл
(превью, отладка, инструменты) всё равно остаётся на Mac. Это исследовательский
инструмент, а не основа продуктового гейта. Xcode.xip я **не скачивал и не пробовал**.

---

## 7. AI-тулинг мобильной разработки

### 7.1 Android

| инструмент | что даёт агенту | статус |
|---|---|---|
| **Android CLI (`android`)** из cmdline-tools 23.0 | Единая точка: SDK, скаффолд проекта, **`docs search`/`docs fetch` по Android Knowledge Base**, `skills add/list/find`, эмулятор, `run`/`install`, `layout` (дерево UI в JSON), `screen capture --annotate`, `screen resolve` (визуальная метка → координаты), `describe` (JSON-карта артефактов сборки) | ✅ **поставлен и опробован**, v1.0.16261425. Док: `developer.android.com/tools/agents/android-cli` |
| **Android Studio Quail 4** (2026.1.4) | Agent Mode, **23 Android-specific agent skills**, локальная Gemma 4, MCP Marketplace (Settings → Tools → AI → MCP Servers), поддержка AGP 7.1–9.4 | по докам; Studio мы не ставим и в headless-гейте она не нужна |
| **`android studio …` подкоманды** | Мост CLI → запущенная Studio: `analyze-file`, `find-declaration`, `find-usages`, **`render-compose-preview --print-semantics --output-image-file=preview.png`**, `version-lookup` | требует Studio Quail 2 Canary 1+ и Gemini in Android Studio — **для headless-Linux-агента неприменимо** |
| **Roborazzi** | Скриншоты Compose в PNG без устройства, `*_compare.png` для diff | ✅ проверено (§2.10) |
| **Compose Preview Screenshot Testing** | То же «официально», хостовые тесты | alpha16; standalone-плагин депрекирован с AGP 9.5.0-alpha03 в пользу AGP test suites |
| **Maestro** | YAML-флоу e2e; CLI ставится на **Linux**; **MCP-сервер встроен в CLI — `maestro mcp`, ничего доставлять не надо**; Apache-2.0; требует Java 17+ | по докам (github.com/mobile-dev-inc/maestro). Android на Linux-эмуляторе реалистичен; iOS — симулятор ⇒ macOS |
| **`adb`** | В `platform-tools` 37.0.1; логи/скриншоты/ввод для агента | ✅ поставлен вместе с SDK |
| **Gradle `--console=plain`, JUnit XML, SARIF** | См. §4.2 | ✅ |
| **`llms.txt`** | `kotlinlang.org/llms.txt` — **есть** (индекс + `kotlinlang.org/docs/_llms/<page>.txt` на каждую страницу); `docs.gradle.org/llms.txt` — **есть** (+ `llms-full.txt`); `developer.android.com/llms.txt` — **404**; `jetbrains.com/llms.txt` — **404** | ✅ проверено `curl` |

Практический вывод: **для Kotlin и Gradle у агента есть машинные доки**
(`https://kotlinlang.org/docs/_llms/<topic>.txt`, `https://docs.gradle.org/llms-full.txt`),
а у Android-платформы официального `llms.txt` нет — его роль играет
**`android docs search|fetch`**, ходящий в ту же Knowledge Base. Это надо прописать
в `CLAUDE.md` мобильного шаблона как первый способ посмотреть платформенную доку
(вместо веб-поиска).

### 7.2 iOS

| инструмент | что даёт | статус |
|---|---|---|
| **Xcode 26.3 «agentic coding»** (анонс 2026-02-03, релиз 2026-02-26) | Claude Agent и OpenAI Codex прямо в Xcode; агент разбирает архитектуру проекта, ищет в документации Apple, правит несколько файлов, **запускает сборки и тесты**, снимает скриншоты Xcode Previews; возможности Xcode отдаются **через MCP** | по докам (apple.com/newsroom, TechCrunch, 9to5Mac, MacRumors). **Требует macOS** |
| **XcodeBuildMCP** (поддерживает Sentry, MIT, ~6.4k★, 1208 коммитов) | MCP-сервер + CLI: сборка, тесты, управление симулятором, захват логов, stateful-отладка. `brew tap getsentry/xcodebuildmcp && brew install xcodebuildmcp` или `npm i -g xcodebuildmcp` | требует **macOS 14.5+ и Xcode 16+**, Node 18+; **на Linux не работает** |
| **Maestro** | e2e на iOS-симуляторе | требует macOS |
| **`sosumi.ai`** | стороннее markdown-зеркало документации Apple для LLM (отвечает 200) | обход отсутствия `llms.txt`: `developer.apple.com/llms.txt` → 404, `www.swift.org/llms.txt` → 404 |
| **SwiftLint / swift-format / sourcekit-lsp** | линт, формат, LSP | ✅ нативно на Linux (§6) |
| **xtool** | сборка/подпись iOS-приложения с Linux | см. §6.8 — требует Xcode.xip, серая зона EULA, **не берём** |

Итог: **весь iOS-специфичный агентный тулинг 2026 года живёт на macOS.** На Linux
агенту остаются язык, пакетный менеджер, тесты, линтеры, LSP и кодоген — но не платформа.

### 7.3 Цикл «правка → проверка»

**Android на Linux — цикл полный (проверен целиком):**

```
правка .kt
  └─ make check                        4 с (без изменений) / 37 с (одна правка) / 94 с (свежий клон)
       ├─ spotlessCheck                формат; авто-фикс `make fmt`
       ├─ detekt                       стиль, сложность, запрещённые импорты, Compose-правила
       ├─ lint                         платформенные ошибки (warningsAsErrors)
       ├─ test                         ViewModel/репозитории + Robolectric Compose UI + Konsist
       ├─ verifyRoborazziDebug         скриншоты; при расхождении — *_compare.png, агент их ВИДИТ
       ├─ koverVerify                  порог покрытия
       └─ assembleDebug                APK
  └─ make assemble / make bundle       подписанный APK / AAB (R8), когда нужен артефакт
  └─ (опц.) android emulator + Maestro e2e — если на машине ≥ 10 ГБ свободной RAM
```

На Linux недостаёт только `connectedAndroidTest` (инструментальные тесты на
устройстве/эмуляторе). Robolectric + Roborazzi закрывают подавляющую часть их пользы
и на порядок быстрее.

**iOS — цикл частичный: Linux + удалённый Mac по SSH.**

| шаг | где | команда |
|---|---|---|
| формат | Linux | `swift format lint --strict --recursive Sources Tests` |
| линт | Linux | `swiftlint lint --strict` |
| сборка Domain/Data/ApiClient | Linux | `swift build` |
| unit-тесты | Linux | `swift test` |
| покрытие | Linux | `swift test --enable-code-coverage` + `codecov/<Pkg>.json` |
| кодоген OpenAPI | Linux | плагин SwiftPM (при сборке) |
| компиляция iOS-таргета приложения | **Mac** | `xcodebuild -scheme App -destination 'generic/platform=iOS' build` |
| UI-тесты / snapshot | **Mac** | `xcodebuild test -destination 'platform=iOS Simulator,name=iPhone 17'` |
| подпись, архив, TestFlight | **Mac** | `xcodebuild -exportArchive`, `xcrun notarytool`/App Store Connect API |

Оркестрация из одного `make check` с fail-fast при недоступном Mac — §8.3.

---

## 8. Рекомендации

### 8.1 Android-шаблон: `Makefile` и `make check` (фактически прогнан)

```makefile
.PHONY: help setup doctor fmt fmt-check lint detekt test screenshots screenshots-record \
        build assemble bundle check cover deps clean status map search kmp-ios

help: ## сгруппированный список команд
	@awk 'BEGIN {FS = ":.*## "} \
		/^##@ / {printf "\n\033[1m%s\033[0m\n", substr($$0, 5); next} \
		/^[a-zA-Z0-9_-]+:.*## / {printf "  \033[36m%-18s\033[0m %s\n", $$1, $$2}' $(MAKEFILE_LIST)

##@ LOOP
fmt:                ## Автоформат (Spotless/ktlint) — единственная автофиксилка формата
	./gradlew spotlessApply --console=plain -q
fmt-check:          ## Проверка формата без мутации дерева (шаг гейта)
	@bash scripts/tools/gradle-step.sh "format" spotlessCheck
detekt:             ## Статический анализ Kotlin + compose-rules
	@bash scripts/tools/gradle-step.sh "detekt" detekt
lint:               ## Android Lint (warningsAsErrors) по всем модулям
	@bash scripts/tools/gradle-step.sh "android lint" lint
test:               ## JVM-тесты: ViewModel, репозитории, Robolectric, Konsist
	@bash scripts/tools/gradle-step.sh "unit tests" test
screenshots:        ## Сверить скриншоты (Roborazzi verify) — без устройства
	@bash scripts/tools/gradle-step.sh "screenshots" verifyRoborazziDebug
screenshots-record: ## Перезаписать эталоны скриншотов
	./gradlew recordRoborazziDebug --console=plain -q
build:              ## Debug APK
	@bash scripts/tools/gradle-step.sh "assembleDebug" :app:assembleDebug
cover:              ## Отчёт покрытия (XML+HTML) и вывод процента
	./gradlew koverXmlReport koverHtmlReport koverLog --console=plain -q
check:              ## Канонический gate (pre-push). GATE_RELEASE=1 добавит assembleRelease
	@bash scripts/tools/check.sh

##@ RELEASE
assemble:           ## Подписанный release APK (нужны STIT_KEYSTORE_* в env)
	./gradlew :app:assembleRelease --console=plain
bundle:             ## Подписанный AAB для сторов
	./gradlew :app:bundleRelease --console=plain

##@ KMP
kmp-ios:            ## Общий код компилируется под iOS (klib, без линковки)
	@bash scripts/tools/gradle-step.sh "kmp ios klib" \
	  :shared:compileKotlinIosArm64 :shared:compileKotlinIosSimulatorArm64

##@ INTEL
status:             ## Модули проекта
	@./gradlew projects --console=plain -q | sed 's/^/  /'
map:                ## Список тасков верхнего уровня
	@./gradlew tasks --console=plain -q | sed 's/^/  /'
deps:               ## Дерево зависимостей (M=:feature:todos C=debugRuntimeClasspath)
	@./gradlew $(if $(M),$(M):,)dependencies $(if $(C),--configuration $(C),) --console=plain -q
search:             ## Поиск по коду (Q=pattern [P=path])
	@if command -v rg >/dev/null 2>&1; then rg -n --max-count=80 -- "$(Q)" $(if $(P),$(P),.); \
	else grep -rn --exclude-dir=build --exclude-dir=.gradle -- "$(Q)" $(if $(P),$(P),.); fi

##@ SETUP
setup:              ## Проверить окружение и прогреть демон
	@bash scripts/tools/doctor.sh
	./gradlew help --console=plain -q
doctor:             ## Fail-fast preflight: JDK, Android SDK, лицензии, RAM
	@bash scripts/tools/doctor.sh
clean:              ## Остановить демон и снести build/
	./gradlew --stop || true
	./gradlew clean --console=plain -q || true
```

Фактический вывод (прогон на этой машине):

```
$ make check
→ gate: android-gate
OK:   format                   2s
OK:   detekt                   0s
OK:   android lint             0s
OK:   unit tests               1s
OK:   screenshots              0s
OK:   coverage                 1s
OK:   assembleDebug            0s
✓ check clean
```

**Состав гейта и обоснование** (времена — из §3):

| # | таск | тёплый | холодный | зачем |
|---|---|---|---|---|
| 1 | `spotlessCheck` | 1–2 с | 14 с | формат; ломается чаще всего, потому первый и самый дешёвый |
| 2 | `detekt` | ~1 с | 1 с (142 с самый первый — скачивание) | стиль, сложность, `ForbiddenImport`, Compose-правила |
| 3 | `lint` | 0–7 с | 44 с | платформенные ошибки, невидимые компилятору |
| 4 | `test` | 1–16 с | 16 с | домен + Robolectric UI + **Konsist архитектура** |
| 5 | `verifyRoborazziDebug` | 1–12 с | 12 с | визуальные регрессии |
| 6 | `koverVerify` | 1–5 с | 5 с | пол покрытия |
| 7 | `:app:assembleDebug` | 1–2 с | 2 с (из build-cache) / 48 с без него | «оно вообще собирается» |
| 8 (опц.) | `:app:assembleRelease` | 25 с | 58 с | R8 ломает то, что живо в debug; `GATE_RELEASE=1` |

Гейт **не** fail-fast: агенту полезнее получить все провалившиеся шаги за один прогон.

Вне гейта: `connectedAndroidTest` (нужно устройство), `buildHealth`
(несовместим с AGP 9.4 — L-05), e2e-Maestro (отдельный стейдж, как `make e2e` в Go-шаблоне).

**Пол покрытия.** На spike'е фактически 83.2 % LINE при пороге 60. Для шаблона
предлагаю **70 % LINE по агрегату** с исключениями сгенерированного кода
(`*Hilt_*`, `*_Factory*`, `ComposableSingletons*`, `*Preview*`, `BuildConfig`) —
сопоставимо с `COVER_FLOOR ?= 75` Go-шаблона, но честно учитывает, что UI закрывается
скриншот-тестами, а не строками.

### 8.2 `gradle.properties` под агента (итоговый, проверенный)

```properties
# Память: RSS пары демонов ~4.8 ГБ даже при Xmx2g.
# МЕНЬШЕ 1g метаспейса ломает Android Lint (OutOfMemoryError: Metaspace) — см. L-02.
org.gradle.jvmargs=-Xmx2g -XX:MaxMetaspaceSize=1g -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8
kotlin.daemon.jvmargs=-Xmx1g -XX:MaxMetaspaceSize=384m

# Параллелизм: не больше 4 воркеров — иначе на нагруженной машине уходим в swap
org.gradle.workers.max=4
org.gradle.parallel=true

# Кеши: configuration cache даёт «Reusing configuration cache» и экономит ~10 с на прогон
org.gradle.caching=true
org.gradle.configuration-cache=true
org.gradle.configuration-cache.parallel=true

android.useAndroidX=true
android.nonTransitiveRClass=true
kotlin.code.style=official

# KMP: iOS-таргеты на Linux не тестируются; иначе 8 строк предупреждений на каждый прогон
kotlin.native.ignoreDisabledTargets=true
```

Замечание про `-XX:+HeapDumpOnOutOfMemoryError`: он спасает при разборе OOM, но
кладёт в корень проекта многосотмегабайтный `java_pid<N>.hprof` (у меня — 476 МБ
после падения Lint). В шаблоне обязателен `*.hprof` в `.gitignore`, а лучше
`-XX:HeapDumpPath=build/` — иначе дамп ляжет рядом с `settings.gradle.kts`.

Что **нельзя** класть (AGP 9 их удалил — L-08): `android.defaults.buildfeatures.buildconfig`,
`…resvalues`, `…shaders`.

### 8.3 iOS-шаблон: Linux-часть + Mac-часть + матрица

```makefile
##@ LOOP (Linux — быстрый цикл)
fmt:        ## swift-format --in-place
	swift format --in-place --recursive Sources Tests
fmt-check:  ## Проверка формата (шаг гейта)
	@swift format lint --strict --recursive Sources Tests && echo "OK:   format" \
	 || { echo "FAIL: format — запусти make fmt"; exit 1; }
lint:       ## SwiftLint --strict
	@swiftlint lint --strict --quiet && echo "OK:   lint" || { echo "FAIL: lint"; exit 1; }
build:      ## swift build (Domain/Data/ApiClient)
	swift build
test:       ## swift test (Swift Testing)
	swift test
cover:      ## Покрытие из codecov/<Pkg>.json
	@swift test --enable-code-coverage >/dev/null && \
	 python3 scripts/tools/coverage.py "$$(swift build --show-bin-path)/codecov"
gen:        ## Регенерировать клиент из openapi.yaml (плагин делает это при сборке)
	swift build --product ApiClient
check:      ## Гейт: Linux-часть + Mac-часть (MAC_HOST), fail-fast без Mac
	@bash scripts/tools/check.sh

##@ MAC (удалённый хост MAC_HOST=user@mac)
mac-doctor: ## Проверить доступность Mac и версию Xcode
	@bash scripts/tools/mac.sh doctor
mac-build:  ## xcodebuild build под generic/platform=iOS
	@bash scripts/tools/mac.sh build
mac-test:   ## xcodebuild test на симуляторе + разбор .xcresult
	@bash scripts/tools/mac.sh test
mac-archive:## Архив + экспорт .ipa (подпись)
	@bash scripts/tools/mac.sh archive
```

`scripts/tools/check.sh` (черновик — **Mac-часть не прогонялась, Mac нет**):

```bash
#!/usr/bin/env bash
set -uo pipefail
mkdir -p build/gate-logs
fails=0
step() { local l="$1"; shift
  if "$@" >"build/gate-logs/${l// /_}.log" 2>&1; then printf 'OK:   %-18s\n' "$l"
  else printf 'FAIL: %-18s (build/gate-logs/%s.log)\n' "$l" "${l// /_}"; fails=$((fails+1)); fi; }

step "format"   swift format lint --strict --recursive Sources Tests
step "lint"     swiftlint lint --strict --quiet
step "build"    swift build
step "test"     swift test
step "coverage" bash scripts/tools/coverage.sh

# Mac-часть: fail-fast, а не тихий пропуск — иначе «зелёный» гейт ничего не значит
if [ -n "${MAC_HOST:-}" ]; then
  if ssh -o BatchMode=yes -o ConnectTimeout=5 "$MAC_HOST" true 2>/dev/null; then
    step "mac build" bash scripts/tools/mac.sh build
    step "mac test"  bash scripts/tools/mac.sh test
  else
    echo "FAIL: mac unreachable ($MAC_HOST)"; fails=$((fails+1))
  fi
elif [ "${MAC_REQUIRED:-1}" = "1" ]; then
  echo "FAIL: MAC_HOST не задан — iOS-часть гейта не выполнена."
  echo "      Локальная итерация по домену: MAC_REQUIRED=0 make check"
  fails=$((fails+1))
fi

[ $fails -eq 0 ] && echo "✓ check clean" || echo "✗ check: провалено шагов — $fails"
exit $fails
```

`scripts/tools/mac.sh` (черновик): `rsync -a --delete --exclude .build ./ "$MAC_HOST:~/ci/<slug>/"`,
затем `ssh "$MAC_HOST" 'cd ~/ci/<slug> && set -o pipefail && xcodebuild … | xcbeautify'`,
затем обратно `scp` `*.xcresult`/`*.ipa` и разбор
`xcrun xcresulttool get test-results summary --format json`. Принцип тот же, что у
Go-шаблона: один глагол, fail-fast, понятная причина.

**Матрица «что где выполнимо» (iOS):**

| действие | Linux | Mac |
|---|---|---|
| формат (`swift format`) | ✅ 0.03 с | ✅ |
| линт (SwiftLint 0.65.1) | ✅ 0.18 с | ✅ |
| сборка Domain/Data/ApiClient | ✅ 4.1 с холодная | ✅ |
| unit-тесты (Swift Testing) | ✅ 0.72 с тёплые | ✅ |
| покрытие (llvm-cov) | ✅ 98.5 % LINE на spike'е | ✅ |
| кодоген OpenAPI (SwiftPM-плагин) | ✅ | ✅ |
| `sourcekit-lsp` (навигация для агента) | ✅ в тулчейне | ✅ |
| компиляция SwiftUI/UIKit-таргета | ⛔ `no such module 'SwiftUI'` | ✅ |
| превью SwiftUI, snapshot-тесты | ⛔ | ✅ |
| симулятор, XCUITest, Maestro iOS | ⛔ | ✅ |
| подпись, архив, TestFlight/App Store | ⛔ | ✅ |
| Instruments, профилирование | ⛔ | ✅ |
| `URLSession` | ⚠️ только через `FoundationNetworking` | ✅ |

### 8.4 KMP-вариант: что агент проверяет на Linux

```makefile
kmp-check:  ## Всё, что можно про общий код, без Mac
	@bash scripts/tools/gradle-step.sh "shared jvm tests" :shared:jvmTest
	@bash scripts/tools/gradle-step.sh "shared ios klib"  :shared:compileKotlinIosArm64 :shared:compileKotlinIosSimulatorArm64
	@bash scripts/tools/gradle-step.sh "shared android"   :shared:assemble
	@test -d shared/build/classes/kotlin/iosArm64/main/klib \
	  || { echo "FAIL: klib iosArm64 не собрался (таск мог быть SKIPPED с кодом 0)"; exit 1; }
```

Последняя строка обязательна: на Linux `linkDebugFramework*` **SKIPPED с кодом 0**,
доверять одному exit-коду нельзя (L-10). Проверка наличия артефакта на диске —
единственный честный сигнал.

Что агент на Linux по KMP **может**: писать общий код, компилировать его под
`iosArm64`/`iosSimulatorArm64`/`iosX64` (ошибки типов, `expect/actual`, зависимости
ловятся), гонять всю общую логику через `jvmTest` за секунды, собирать Android-таргет.
Что **не может**: линковать `.framework`, запускать `iosTest`.

### 8.5 `.claude/settings.json` — `permissions.allow` без промптов

```json
{
  "$schema": "https://json.schemastore.org/claude-code-settings.json",
  "effortLevel": "max",
  "autoMemoryEnabled": false,
  "permissions": {
    "allow": [
      "Bash(make help)", "Bash(make doctor)", "Bash(make setup)",
      "Bash(make fmt)", "Bash(make fmt-check)", "Bash(make lint)", "Bash(make detekt)",
      "Bash(make test*)", "Bash(make screenshots*)", "Bash(make build)",
      "Bash(make cover)", "Bash(make check)", "Bash(make kmp-ios)", "Bash(make kmp-check)",
      "Bash(make status*)", "Bash(make map*)", "Bash(make deps*)", "Bash(make search*)",
      "Bash(./gradlew help*)", "Bash(./gradlew tasks*)", "Bash(./gradlew projects*)",
      "Bash(./gradlew dependencies*)", "Bash(./gradlew :*:dependencies*)",
      "Bash(./gradlew spotlessApply*)", "Bash(./gradlew spotlessCheck*)",
      "Bash(./gradlew detekt*)", "Bash(./gradlew lint*)", "Bash(./gradlew updateLintBaseline)",
      "Bash(./gradlew test*)", "Bash(./gradlew :*:test*)",
      "Bash(./gradlew *Roborazzi*)", "Bash(./gradlew kover*)",
      "Bash(./gradlew assembleDebug*)", "Bash(./gradlew :app:assembleDebug*)",
      "Bash(./gradlew compileKotlin*)", "Bash(./gradlew :shared:*)",
      "Bash(./gradlew --stop)", "Bash(./gradlew --version)",
      "Bash(android sdk list*)", "Bash(android docs*)", "Bash(android describe*)",
      "Bash(android --version)", "Bash(adb devices)", "Bash(adb logcat -d*)",
      "Bash(swift build*)", "Bash(swift test*)", "Bash(swift format*)",
      "Bash(swiftlint lint*)", "Bash(swiftlint version)",
      "Bash(rg *)", "Bash(grep *)", "Bash(find *)",
      "Bash(git status)", "Bash(git diff*)", "Bash(git log*)", "Bash(git show*)",
      "Bash(git branch*)", "Bash(git ls-files*)", "Bash(git rev-parse*)", "Bash(git tag -l*)",
      "Read(*)", "Glob(*)", "Grep(*)"
    ],
    "deny": [
      "Bash(*--no-verify*)",
      "Bash(git push --force*)", "Bash(git push -f*)",
      "Bash(git reset --hard*)", "Bash(git clean -f*)", "Bash(rm -rf *)",
      "Bash(./gradlew assembleRelease*)", "Bash(./gradlew bundleRelease*)",
      "Bash(./gradlew publish*)", "Bash(keytool*)",
      "Bash(pkill*)", "Bash(killall*)",
      "Bash(android sdk install*)", "Bash(sdkmanager*)",
      "Read(**/*.jks)", "Read(**/*.keystore)", "Read(**/*.p12)", "Read(**/*.mobileprovision)",
      "Edit(**/*.jks)", "Edit(**/*.keystore)",
      "Edit(**/build/**)", "Write(**/build/**)",
      "Edit(**/GeneratedSources/**)", "Write(**/GeneratedSources/**)",
      "Edit(gradle/wrapper/gradle-wrapper.properties)",
      "Write(**/.claude/projects/**/memory/**)"
    ]
  }
}
```

Логика списков:
- **allow** — всё, что читает или проверяет, плюс `spotlessApply`/`swift format`:
  единственные безопасные мутации, потому что детерминированы;
- **deny на `assembleRelease`/`bundleRelease`/`keytool`** — релизный артефакт и
  ключи создаёт человек (или скилл `/release`) осознанно, не агент «по пути»;
- **deny на `android sdk install`** — агент не должен молча качать гигабайты;
- **deny на правку `build/`, `GeneratedSources/`, wrapper-properties** — прямой
  аналог `Edit(**/infra/postgres/db/**)` и `Edit(**/mocks/*.go)` в Go-шаблоне;
- **`pkill`/`killall` в deny** — на машине разработчика живут чужие Gradle/Kotlin-демоны.

### 8.6 Негативные фикстуры гейта (что реально красное — проверено)

| фикстура | что подкладываем | кто ловит | фактический результат |
|---|---|---|---|
| **NF-01** запрещённый импорт | файл с `import android.util.Log` + `Log.d(...)` | detekt `style>ForbiddenImport` | ✅ `rc=1`; `BadLogging.kt:3:1: The import 'android.util.Log' has been forbidden: Логи идут через core:logging…`; `Analysis failed with 1 weighted issues` |
| **NF-02** feature → feature | модуль `feature/other` с `import tech.stit.mobiletemplate.feature.todos.TodosUiState` | Konsist `feature-модуль не импортирует другой feature-модуль` | ✅ `rc=1`; `5 tests completed, 1 failed` |
| **NF-03** формат | строка не по ktlint | Spotless | ✅ `rc=1`, diff-блок + `Run './gradlew spotlessApply' to fix all violations.` |
| **NF-04** мёртвый код | неиспользуемая `private fun` | detekt `UnusedPrivateMember` | ✅ `rc=1`; `Todo.kt:24:24: Private function 'unusedOld' is unused.` |
| **NF-05** платформенная ошибка | манифест без `android:icon` | Android Lint `MissingApplicationIcon` через `warningsAsErrors` | ✅ `rc=1`; `2 errors` |
| **NF-06** магическое число | сырой `0xFF…` в теле композабла | detekt `MagicNumber` | ✅ ловилось на реальном коде до починки (6 находок) |
| **NF-07** опечатка в конфиге линтера | неверный ключ в `detekt.yml` | detekt `config.validation` | ✅ `rc=1`; `Run failed with 2 invalid config properties` |
| **NF-08** несогласованный jvmTarget | Kotlin 21 vs Java 17 | Kotlin Gradle Plugin | ✅ `rc=1`; `⛔ Inconsistent JVM-target compatibility` |
| NF-09 визуальная регрессия | поменять токен цвета без перезаписи эталона | Roborazzi `verifyRoborazziDebug` | не прогонял отдельно; механика `*_compare.png` подтверждена |
| NF-10 просадка покрытия | удалить тест | Kover `koverVerify` | не прогонял (порог 60 при факте 83) |

Их стоит зафиксировать **как самотест гейта шаблона** — верб `make gate-selftest`,
которая по очереди подкладывает фикстуру, требует ненулевой код возврата и
откатывает. Это прямой перенос практики «линтер обязан упасть на заведомом
нарушении» из SSR-синтеза.

### 8.7 Мелкие, но важные решения

1. **Одна автофиксилка формата на язык.** Android — Spotless(ktlint 1.8.0),
   `detekt-formatting` **не подключать**. Swift — `swift-format`, у SwiftLint
   выключить `trailing_comma`. Иначе гейт не сходится (L-01, L-03).
2. **`.editorconfig` — обязательная часть шаблона**: без него ktlint 1.8.0 валит
   каждую `@Composable`-функцию правилом `standard:function-naming`.
3. **Konsist вместо отдельного архитектурного линтера.** Даёт то же, что `depguard`
   в Go-шаблоне и `import-linter` в боте, но без нового инструмента в гейте:
   правила — обычные тесты, сообщения человекочитаемые, прогон 0.5 с.
4. **SDK-уровни — в `libs.versions.toml`** (`compileSdk`/`compileSdkMinor`/`minSdk`/
   `targetSdk`), convention-плагины читают их оттуда; модули чисел не знают.
5. **Никаких baseline'ов** (ни lint, ни detekt) в свежем шаблоне (L-13).
6. **`android` CLI вместо `sdkmanager`** во всех скриптах, с `--no-metrics` или
   `~/.androidrc`.
7. **`make doctor` первым делом** в `/smartdev` мобильного шаблона: ловит «нет javac»,
   «нет ANDROID_HOME», «мало RAM» — три самых частых стопора.
8. **`./gradlew --stop` в конце сессии** (`make clean`): 4.8 ГБ RSS на машине, где
   работают другие агенты, заметны.
9. **`*.hprof` в `.gitignore`** и `-XX:HeapDumpPath=build/` — иначе OOM оставляет
   полугигабайтный дамп в корне репозитория.

### 8.8 Бонус потоку 07: OpenAPI Generator для Kotlin — работает, но ломает cookie-auth

Проверено на том же `openapi.yaml`, что и Swift-клиент (cookie-схема
`access_token`, `/api/auth/login`, `/api/v1/todos`), генератором
`org.openapitools:openapi-generator-cli:7.25.0` (jar лежал в общем кеше
`~/.cache/stit-mobile-research/openapi-generator/`):

```bash
java -jar openapi-generator-cli-7.25.0.jar generate \
  -i openapi.yaml -g kotlin -o out \
  --additional-properties=library=jvm-ktor,serializationLibrary=kotlinx_serialization,packageName=tech.stit.mobiletemplate.api
```

| метрика | значение |
|---|---|
| время | **0.86 с** (rc=0) |
| сгенерировано | **20 `.kt`-файлов**: `models/` (Todo, LoginRequest, ErrorModel, CreateTodoRequest), `apis/DefaultApi.kt`, `infrastructure/ApiClient.kt`, `auth/{ApiKeyAuth,HttpBasicAuth,HttpBearerAuth,OAuth,Authentication}.kt` + скелеты тестов |
| headless на Linux | ✅ полностью, нужен только JDK |

**⚠️ Критическая находка для контракта с нашим бэкендом.** Kotlin-генератор
**не умеет `in: cookie`** и молча деградирует до `query`:

```kotlin
// infrastructure/ApiClient.kt — сгенерировано из securitySchemes.cookieAuth (in: cookie)
private val authentications: Map<String, Authentication> by lazy {
    mapOf("cookieAuth" to ApiKeyAuth("query", "access_token"))
}
```

```kotlin
// auth/ApiKeyAuth.kt — ветки "cookie" не существует вообще
when (location) {
    "query"  -> query[paramName] = listOf(value)
    "header" -> headers[paramName] = value
}
```

То есть при использовании `setApiKey(...)` токен уйдёт **в query-строку URL**
(попадёт в логи прокси, Referer и историю) вместо заголовка `Cookie`. Для нашего
cookie-only бэкенда это неприемлемо.

Вывод для шаблона (и для потока 07): **брать из OpenAPI-кодогена только модели и
сигнатуры операций, а транспорт и аутентификацию писать руками** — на Android это
Ktor-плагин `HttpCookies` / `OkHttp CookieJar`, на iOS — `ClientMiddleware`. Совпадает
с тем, что показал Swift-генератор (§6.6): он `securitySchemes` просто игнорирует.
Третий вариант, консистентный с `spa-frontend-template` (где контракты — рукописные
Zod-схемы без кодогена): **вообще не генерировать, а писать типизированный клиент
руками** — решать потоку 07/08.

---

## 9. Ловушки (все воспроизведены на этой машине, с URL)

**L-01. `detekt-formatting` ⟂ Spotless/ktlint — гейт не сходится никогда.**
`io.gitlab.arturbosch.detekt:detekt-formatting:1.23.8` тянет
`com.pinterest.ktlint:ktlint-ruleset-standard:**0.50.0**`, Spotless использует
ktlint 1.8.0. Фактически: `spotlessApply` переписал `InMemoryTodoRepository.kt` →
detekt выдал **21 нарушение `Indentation`** в том же файле.
**Обход**: не подключать `detekt-formatting`; формат — только Spotless.
→ https://repo1.maven.org/maven2/io/gitlab/arturbosch/detekt/detekt-formatting/1.23.8/detekt-formatting-1.23.8.pom

**L-02. Android Lint падает при `-XX:MaxMetaspaceSize=512m`.**
Симптом — не «мало памяти», а внутренняя ошибка с тремя вложенными стектрейсами
на **13 815 байт**:
```
Unexpected failure during lint analysis of InMemoryTodoRepositoryTest.kt
(this is a bug in lint or one of the libraries it depends on)
Message: Metaspace
Stack: `OutOfMemoryError:ClassLoader.defineClass1 … LLFirTypeTargetResolver … SymbolLightSimpleMethod`
```
**Обход**: `-XX:MaxMetaspaceSize=1g` — с ним `lint` проходит за 43 с.
Побочный эффект `-XX:+HeapDumpOnOutOfMemoryError`: в корне проекта появился
`java_pid<N>.hprof` на **476 МБ** (я его удалил; в шаблоне — `*.hprof` в `.gitignore`
и `-XX:HeapDumpPath=build/`).

**L-03. SwiftLint `trailing_comma` ⟂ swift-format `TrailingComma`.**
SwiftLint по умолчанию **запрещает** висящую запятую, swift-format **требует**.
После `swiftlint --fix` → `swift format lint` даёт rc=1; после `swift format --in-place`
→ `swiftlint --strict` даёт rc=2. Бесконечный цикл.
**Обход**: `disabled_rules: [trailing_comma]` в `.swiftlint.yml`.
→ https://realm.github.io/SwiftLint/trailing_comma.html · https://github.com/swiftlang/swift-format

**L-04. compose-rules 0.5.0+ требует detekt 2.0.0-alpha.**
`io.nlopez.compose.rules:detekt:0.6.6` в POM зависит от `dev.detekt:detekt-core:2.0.0-alpha.6`.
С detekt 1.23.8 правила подгружаются, но конфиг не валидируется:
```
Run failed with 2 invalid config properties.
- Property 'Compose>CompositionLocalAllowlist>allowedCompositionLocals' is misspelled or does not exist.
```
**Обход**: `detekt 1.23.8` + `io.nlopez.compose.rules:detekt:0.4.28` (её POM зависит
от `detekt-core:1.23.7`). Также: `allowedCompositionLocals` принимает **строку через
запятую**, а не YAML-список, и правило зовётся `UnstableCollections`, а не
`ComposeUnstableCollections`.
→ https://mrmans0n.github.io/compose-rules/detekt/ · https://repo1.maven.org/maven2/io/nlopez/compose/rules/detekt/0.6.6/detekt-0.6.6.pom

**L-05. `dependency-analysis` 3.19.2 не знает AGP 9.4.**
```
The Dependency Analysis plugin is only known to work with versions of AGP between
8.10.0 and 9.3.1. You are using 9.4.0. Proceed at your own risk.
buildHealth → No project health reports found.
```
**Обход**: держать вне гейта до совместимой версии.
→ https://github.com/autonomousapps/dependency-analysis-gradle-plugin

**L-06. AGP 9 сломал канон convention-плагинов Now in Android.**
`CommonExtension<*,*,*,*,*,*>` → `No type arguments expected for 'interface CommonExtension : ExtensionAware'`.
После снятия генериков `defaultConfig`/`compileOptions`/`testOptions`/`lint` дают
`Unresolved reference` (в байткоде `gradle-api-9.4.0.jar` методы есть — скрыты из
Kotlin-видимости; это «new DSL», ср. `android.newDsl.optOut` в release notes 9.4).
**Обход**: конфигурировать `ApplicationExtension`/`LibraryExtension` напрямую, общее —
функциями поверх `Lint`/`CompileOptions`/`TestOptions` (§2.5).
→ https://developer.android.com/build/releases/gradle-plugin

**L-07. `compileSdk = 36` больше не проходит.**
```
Dependency 'androidx.core:core-ktx:1.19.0' requires libraries and applications that
depend on it to compile against version 37 or later of the Android APIs.
Recommended action: Update this project to use a newer compileSdk of at least 37, for example 37.2.
```
**Обход**: `compileSdk = 37` + **`compileSdkMinor = 2`**, установить `platforms/android-37.2`.

**L-08. AGP 9 удалил `android.defaults.buildfeatures.*`.**
`WARNING: The option 'android.defaults.buildfeatures.buildconfig' is deprecated… It was
removed in version 9.0 of the Android Gradle plugin.` **Обход**: удалить из
`gradle.properties` (`buildConfig` и так выключен по умолчанию).

**L-09. Kover нужен на каждом модуле, не только в корне.**
Иначе корневой `kover(projects.x)` падает на разрешении вариантов:
`Incompatible because this component declares attribute 'org.gradle.usage' with value
'java-runtime' and the consumer needed 'kover'`. **Обход**: применять
`org.jetbrains.kotlinx.kover` в общем quality-плагине.
→ https://github.com/Kotlin/kotlinx-kover

**L-10. `linkDebugFrameworkIosArm64` на Linux — SKIPPED с кодом 0.**
Ни ошибки, ни предупреждения — даже при `-Pkotlin.native.ignoreDisabledTargets=false`
(предупреждают только `*Test`-таски). Каталога `shared/build/bin` не появляется.
**Обход**: в Linux-гейте использовать только `compileKotlinIos*` **и** проверять
наличие klib-каталога на диске.

**L-11. Lint не смотрит внутрь чистых JVM-модулей.**
`Warning: Lint will treat :core:model as an external dependency and not analyze it.
Recommended Action: Apply the 'com.android.lint' plugin to java library project :core:model.`
**Обход**: `pluginManager.apply("com.android.lint")` в JVM-convention-плагине.

**L-12. Несогласованный `jvmTarget` между Kotlin и Java.**
```
⛔ Inconsistent JVM Target Compatibility Between Java and Kotlin Tasks
Inconsistent JVM-target compatibility detected for tasks 'compileJava' (17) and 'compileKotlin' (21).
```
**Обход**: `compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }` в JVM-модуле
(или единый `jvmToolchain(17)` + foojay-резолвер).
→ https://kotl.in/gradle/jvm/target-validation

**L-13. Lint сам создаёт baseline и всё равно валит сборку.**
При `baseline = file("lint-baseline.xml")` и отсутствии файла:
`Created baseline file …/lint-baseline.xml` + `Also breaking the build in case this was
not intentional.` Агент почти наверняка закоммитит baseline и «починит» гейт долгом.
**Обход**: baseline в шаблоне не объявлять.

**L-14. `sha256` Gradle-дистрибутива отдаётся через 301.**
`curl -s https://services.gradle.org/distributions/gradle-9.7.1-bin.zip.sha256` вернёт
HTML `301 Moved Permanently`, который молча уедет в `distributionSha256Sum`.
**Обход**: `curl -sL`.

**L-15. Ложная AAPT-ошибка после переименования ресурсной папки.**
`ERROR: AAPT: error: resource mipmap/ic_launcher … not found` при физически
существующем `mipmap-anydpi/ic_launcher.xml` — устаревшие merged-ресурсы;
`--rerun-tasks` вылечил.

**L-16. `Package.swift`: порядок именованных аргументов фиксирован.**
`error: argument 'products' must precede argument 'dependencies'`.
→ https://docs.swift.org/package-manager/

**L-17. Классический рецепт llvm-cov для SwiftPM сломан в Swift 6.4.**
`.build/debug/<Pkg>PackageTests.xctest` не существует: артефакты в
`.build/out/Products/Debug-linux-x86_64/` и разбиты по таргетам
(`DomainTests-test-runner`, `DataTests-test-runner`).
**Обход**: читать `$(swift build --show-bin-path)/codecov/<Package>.json`.

**L-18. `swift-format` по умолчанию форматирует в 2 пробела** — без `.swift-format`
с `"indentation": {"spaces": 4}` он завалит весь код, написанный в 4.

**L-19. Первый Robolectric-класс медленный** — 25.9 с против 2.5 с у последующих
(скачивание `android-all` jar). Прогревать в `make setup`.
→ https://github.com/robolectric/robolectric/releases

**L-20. `androidx.hilt.navigation.compose.hiltViewModel` переехал**
в `androidx.hilt.lifecycle.viewmodel.compose`. Со строгими предупреждениями — ошибка.

**L-21. `createComposeRule` депрекирован** в пользу
`androidx.compose.ui.test.junit4.v2.createComposeRule`: «v2 APIs use
StandardTestDispatcher instead of UnconfinedTestDispatcher… Tests relying on immediate
execution may require explicit synchronization».

**L-22. Swift OpenAPI Generator игнорирует `securitySchemes`.**
В 911 сгенерированных строках нет ни одного упоминания security/cookie/Authorization.
Аутентификация — только `ClientMiddleware` руками.
→ https://github.com/apple/swift-openapi-generator

**L-23. OpenAPI Generator (Kotlin) мапит `in: cookie` в `query` — токен уходит в URL.**
`mapOf("cookieAuth" to ApiKeyAuth("query", "access_token"))`, а в `ApiKeyAuth.apply`
веток всего две: `"query"` и `"header"`. Ветки `"cookie"` нет.
**Обход**: транспорт и аутентификацию писать руками; из кодогена брать только модели.
→ https://github.com/OpenAPITools/openapi-generator

**L-24. `sdkmanager` депрекирован в cmdline-tools 23.0.**
Печатает предупреждение и делегирует новому `android`. Формат путей сменился с
`platforms;android-37.2` на `platforms/android-37.2`.
→ https://developer.android.com/tools/agents/android-cli

**L-25. Android CLI шлёт телеметрию по умолчанию.** Глушится `--no-metrics` или
строкой в `~/.androidrc`. Для корпоративной среды это надо прописать в шаблоне явно.

---

## 10. Риски и открытые вопросы владельцу

1. **Mac нужен в любом варианте с iOS.** Ни xtool, ни Skip, ни облачные сервисы не
   снимают необходимость Mac для UI, симулятора, подписи и подачи в App Store.
   **Вопрос**: покупаем/арендуем Mac mini как «mac-runner» экосистемы (тогда
   `make check` iOS-шаблона ходит туда по SSH), или iOS-шаблон делаем заведомо
   «двуместным» — Linux-агент пишет домен, человек на Mac доводит UI?
2. **Порог покрытия мобильного гейта.** В Go-шаблоне 75 % по доменным пакетам. На
   Android домен тонкий, а UI закрывается скриншотами. Предлагаю 70 % LINE по
   агрегату с исключениями сгенерированного кода. Нужно решение.
3. **`assembleRelease` в гейте или нет.** R8 ломает то, что живо в debug (reflection,
   Hilt, kotlinx-serialization), и это ловится только релизной сборкой. Цена +25 с на
   каждый `make check`. Предлагаю: `GATE_RELEASE=1` в `lefthook pre-push` и
   обязательно в `/release`, но не в ручном `make check`.
4. **Хранение подписных ключей.** В экосистеме есть Vaultwarden (corp-infra).
   Keystore / `.p12` / `.mobileprovision` — туда? Или отдельное хранилище с ротацией?
   Блокирует дизайн `/release` мобильного шаблона (потоки 05/08).
5. **STOP-граница мобильного репо.** Для сервисов это «образ в registry». Для
   мобильного — подписанный AAB/IPA. Кто и куда его забирает (Gitea Releases? MinIO?
   ручная заливка в стор)? Влияет на состав верб `Makefile`.
6. **detekt 2.0 всё ещё alpha** (alpha.6 на 2026-09-17), compose-rules ушли вперёд.
   Стартуем на 1.23.8 + compose-rules 0.4.28 и берём миграцию на себя, когда 2.0
   станет stable? Или сразу на alpha? Рекомендую stable.
7. **Эмулятор не проверен.** Было < 6 ГБ доступной RAM, AVD не поднимался (условие
   потока). Gradle Managed Devices + `aosp-atd` (образы `system-images/android-3x/
   aosp_atd/x86_64` в SDK есть) позволяют описать девайс декларативно и запускать
   `./gradlew <device>DebugAndroidTest` без ручного AVD — **цену по памяти/времени
   надо померить отдельно** на менее нагруженной машине.
8. **Воспроизводимость Roborazzi-эталонов.** Рендер зависит от JVM/платформы.
   CI-раннеров в экосистеме нет, эталоны пишутся локально ⇒ у разных разработчиков
   может получаться разный PNG. Решить: жёстко фиксировать JDK через toolchain
   (уже сделано) + правило «эталоны пишет тот, у кого стандартное окружение», либо
   допускать порог различий (`RoborazziOptions.CompareOptions(changeThreshold)`).
9. **Kotlin 2.4.20 + KSP 2.3.12 работает, но связка нестабильна по определению**
   (KSP отстаёт от Kotlin на минорную ветку). Смотреть ли на Metro 1.4.4
   (компиляторный плагин, без KSP) как на способ убрать эту зависимость из гейта?
10. **Swift 6.4 вышел 2026-09-14 — за три дня до прогона.** Всё работает, но это
    самый свежий релиз. Политика «latest» или «latest-1» (6.3.3 от 2026-06-29)?
11. **OpenAPI-кодоген vs рукописные контракты.** Swift-генератор игнорирует
    `securitySchemes`, Kotlin-генератор мапит cookie в query (L-22, L-23).
    `spa-frontend-template` уже ушёл от кодогена к рукописным Zod-контрактам.
    Повторяем этот выбор на мобиле или берём «модели из кодогена + транспорт руками»?
    Вопрос потоку 07, но решение владельца.
12. **Общий кеш `~/.cache/stit-mobile-research/` делится между потоками.** Я
    воспользовался чужим `openapi-generator-cli-7.25.0.jar` (только чтение) для
    проверки Kotlin-кодогена; своего ничего там не удалял. Если владелец будет
    чистить — Android SDK, JDK, Swift и SwiftLint в этом каталоге ставил я.

---

## 11. Сводная таблица версий

Проверено 2026-09-17 через `./gradlew --version`, `swift --version`, `android --version`,
`swiftlint version`, `libs.versions.toml` и `maven-metadata.xml` первоисточников.

| инструмент / пакет | версия | статус / дата | лицензия | источник |
|---|---|---|---|---|
| Android cmdline-tools | **23.0** (`commandlinetools-linux-16111833_latest.zip`, 181 МБ) | stable | Android SDK ToS | `dl.google.com/android/repository/repository2-3.xml` |
| Android CLI (`android`) | **1.0.16261425** | в cmdline-tools 23.0 | Android SDK ToS | developer.android.com/tools/agents/android-cli |
| Android SDK Platform | **37.2** (есть 36, 36.1, 37.0, 37.1) | stable | Android SDK ToS | `android sdk list --all` |
| Android Build-Tools | **37.0.0** (дефолт AGP — 36.0.0) | stable | Android SDK ToS | там же |
| Android Platform-Tools (adb) | **37.0.1** | stable | Android SDK ToS | там же |
| Android Gradle Plugin | **9.4.0** (9.5.0-alpha05 в пути) | stable | Apache-2.0 | Google Maven |
| Gradle | **9.7.1** (2026-08-19; embedded Kotlin 2.4.0) | stable | Apache-2.0 | gradle.org/releases |
| Kotlin | **2.4.20** | stable | Apache-2.0 | Maven Central |
| KSP | **2.3.12** (ветки 2.4.x нет) | stable | Apache-2.0 | Maven Central |
| Compose BOM | **2026.09.00** | stable | Apache-2.0 | Google Maven |
| Compose Material3 | 1.4.0 (в BOM) | stable | Apache-2.0 | Google Maven |
| Compose UI / ui-test | 1.12.1 | stable | Apache-2.0 | Google Maven |
| AndroidX Core KTX | **1.19.0** | stable | Apache-2.0 | Google Maven |
| AndroidX Activity Compose | **1.13.0** | stable | Apache-2.0 | Google Maven |
| AndroidX Lifecycle | **2.11.0** | stable | Apache-2.0 | Google Maven |
| Navigation Compose (2.x) | 2.10.1 | stable | Apache-2.0 | Google Maven |
| Navigation 3 | **1.1.7** (1.2.0-rc01) | stable | Apache-2.0 | Google Maven |
| AndroidX Room | 2.8.5 | stable | Apache-2.0 | Google Maven |
| AndroidX DataStore | 1.2.1 | stable | Apache-2.0 | Google Maven |
| Hilt (Dagger) | **2.60.1** | stable | Apache-2.0 | Maven Central |
| androidx.hilt navigation-compose | 1.4.0 | stable | Apache-2.0 | Google Maven |
| kotlinx.coroutines | **1.11.0** | stable | Apache-2.0 | Maven Central |
| kotlinx.serialization | 1.11.0 | stable | Apache-2.0 | Maven Central |
| Turbine | 1.2.1 | stable | Apache-2.0 | Maven Central |
| Robolectric | **4.17** | stable | MIT | Maven Central |
| Roborazzi | **1.74.0** | stable | Apache-2.0 | Maven Central |
| Konsist | **0.17.3** | stable | Apache-2.0 | Maven Central |
| Kover | **0.9.9** | stable | Apache-2.0 | Maven Central |
| detekt | **1.23.8** | stable (2.0.0-alpha.6 под `dev.detekt`) | Apache-2.0 | Maven Central |
| compose-rules (detekt) | **0.4.28** для detekt 1.23.x (0.6.6 требует detekt 2.0-alpha) | stable | Apache-2.0 | Maven Central |
| Spotless | **8.10.2** | stable | Apache-2.0 | Gradle Plugin Portal |
| ktlint (движок Spotless) | **1.8.0** | stable | MIT | Maven Central |
| ktlint-gradle (альтернатива) | 14.2.0 | stable | MIT | Gradle Plugin Portal |
| dependency-analysis | 3.19.2 (**не поддерживает AGP 9.4**) | stable | Apache-2.0 | Maven Central |
| Compose Preview Screenshot | 0.0.1-alpha16 (standalone депрекирован с AGP 9.5.0-alpha03) | alpha | Apache-2.0 | Google Maven |
| Koin (альтернатива DI) | 4.2.2 | stable | Apache-2.0 | Maven Central |
| Metro (альтернатива DI) | 1.4.4 | stable | Apache-2.0 | Maven Central |
| Compose Multiplatform | 1.12.0 (1.13.0-alpha01) | stable | Apache-2.0 | Maven Central |
| foojay-resolver-convention | 1.0.0 | stable | Apache-2.0 | Gradle Plugin Portal |
| Temurin JDK | **21.0.12.1+1** LTS | stable | GPLv2+CE | api.adoptium.net |
| Kotlin/Native prebuilt | 2.4.20 (822 МБ в `~/.konan`) | stable | Apache-2.0 | — |
| Swift | **6.4.0** (2026-09-14; предыдущий 6.3.3 от 2026-06-29) — **есть тарболл Debian 13** | stable | Apache-2.0 | swift.org/api/v1/install/releases.json |
| swift-format | в тулчейне 6.4 (`--version` → `main`) | stable | Apache-2.0 | тулчейн |
| SwiftLint | **0.65.1** (Linux amd64/arm64 бинари в релизе) | stable | MIT | github.com/realm/SwiftLint/releases |
| swift-openapi-generator | **1.13.1** | stable | Apache-2.0 | github.com/apple/swift-openapi-generator |
| swift-openapi-runtime | 1.12.1 | stable | Apache-2.0 | github |
| swift-openapi-urlsession | 1.3.1 | stable | Apache-2.0 | github |
| OpenAPI Generator CLI (Kotlin) | **7.25.0** | stable | Apache-2.0 | Maven Central `org.openapitools:openapi-generator-cli` |
| Maestro | Apache-2.0, `maestro mcp` встроен в CLI, Linux поддержан, Java 17+ | stable | Apache-2.0 | github.com/mobile-dev-inc/maestro |
| XcodeBuildMCP | ~6.4k★; macOS 14.5+/Xcode 16+/Node 18+ | stable | MIT | github.com/cameroncooke/XcodeBuildMCP |
| xtool | 1.2.0 | stable | MIT | github.com/xtool-org/xtool |
| Xcode | 26.3 (агентный режим; анонс 2026-02-03, релиз 2026-02-26) | stable | Apple EULA | apple.com/newsroom |
| Android Studio | Quail 4 (2026.1.4), 23 agent skills, Gemma 4, MCP Marketplace | stable | Apache-2.0 | android-developers.googleblog.com |

---

## 12. Источники

Все ссылки проверены 2026-09-17.

**Android / Gradle / Kotlin**
- https://developer.android.com/tools/agents/android-cli — новый `android` CLI для агентов
- https://developer.android.com/studio#command-line-tools-only — cmdline-tools
- https://dl.google.com/android/repository/repository2-3.xml — машинный индекс пакетов SDK
- https://developer.android.com/build/releases/gradle-plugin — AGP 9.4.0, матрица совместимости
- https://gradle.org/releases/ — Gradle 9.7.1
- https://services.gradle.org/distributions/gradle-9.7.1-bin.zip.sha256 — контрольная сумма (только с `-L`)
- https://kotlinlang.org/docs/releases.html — Kotlin 2.4.20
- https://kotlinlang.org/llms.txt + https://kotlinlang.org/docs/_llms/ — машинные доки Kotlin
- https://docs.gradle.org/llms.txt , https://docs.gradle.org/llms-full.txt — машинные доки Gradle
- https://developer.android.com/studio/preview/compose-screenshot-testing — Compose Preview Screenshot Testing
- https://developer.android.com/studio/gemini/agent-mode , https://developer.android.com/studio/gemini/add-mcp-server
- https://android-developers.googleblog.com/2026/09/leverage-gemma-4-android-studio-quail.html — Quail 4, 23 Android skills
- https://github.com/takahirom/roborazzi — Roborazzi 1.74.0
- https://github.com/robolectric/robolectric/releases — Robolectric 4.17
- https://github.com/detekt/detekt/releases — detekt 1.23.8 / 2.0.0-alpha.6
- https://mrmans0n.github.io/compose-rules/detekt/ — конфигурация compose-rules
- https://github.com/LemonAppDev/konsist — Konsist 0.17.3
- https://github.com/Kotlin/kotlinx-kover — Kover 0.9.9
- https://github.com/diffplug/spotless — Spotless 8.10.2
- https://github.com/JLLeitschuh/ktlint-gradle — ktlint-gradle 14.2.0
- https://github.com/autonomousapps/dependency-analysis-gradle-plugin — 3.19.2
- https://github.com/google/ksp/releases — KSP 2.3.12
- https://github.com/OpenAPITools/openapi-generator — CLI 7.25.0 (Kotlin-генератор)
- https://kotl.in/gradle/jvm/target-validation — валидация jvmTarget
- https://repo1.maven.org/maven2/ и https://dl.google.com/dl/android/maven2/ — `maven-metadata.xml` всех артефактов
- https://plugins.gradle.org/m2/ — метаданные Gradle-плагинов
- https://api.adoptium.net/v3/binary/latest/21/ga/linux/x64/jdk/hotspot/normal/eclipse — Temurin JDK 21

**Swift / iOS**
- https://www.swift.org/install/linux/ , https://www.swift.org/api/v1/install/releases.json — Swift 6.4.0 и список платформ (Debian 13, Android SDK, Wasm, static-musl)
- https://download.swift.org/swift-6.4.0-release/debian13/swift-6.4.0-RELEASE/swift-6.4.0-RELEASE-debian13.tar.gz
- https://github.com/swiftlang/swiftly — менеджер тулчейнов
- https://github.com/swiftlang/swift-format — форматтер (входит в тулчейн)
- https://github.com/realm/SwiftLint/releases — 0.65.1 и Linux-бинари (`swiftlint_linux_amd64.zip`)
- https://github.com/apple/swift-openapi-generator — 1.13.1
- https://github.com/apple/swift-openapi-runtime — 1.12.1
- https://github.com/apple/swift-openapi-urlsession — 1.3.1
- https://github.com/xtool-org/xtool , https://deepwiki.com/xtool-org/xtool/3.2-linux-installation — сборка iOS на Linux и её требования (Xcode.xip)
- https://forums.swift.org/t/xtool-cross-platform-xcode-replacement-build-ios-apps-on-linux-and-more/79803
- https://github.com/cameroncooke/XcodeBuildMCP — MCP для Xcode (macOS)
- https://www.apple.com/newsroom/2026/02/xcode-26-point-3-unlocks-the-power-of-agentic-coding/
- https://9to5mac.com/2026/02/03/apple-announces-agentic-coding-in-xcode-with-claude-agent-and-codex-integration/
- https://techcrunch.com/2026/02/03/xcode-moves-into-agentic-coding-with-deeper-openai-and-anthropic-integrations/
- https://www.macrumors.com/2026/02/26/apple-releases-xcode-26-3/
- https://sosumi.ai/ — markdown-зеркало документации Apple для LLM

**Кросс-платформенное / e2e**
- https://github.com/mobile-dev-inc/maestro — Maestro, встроенный `maestro mcp`, Linux CLI

**Внутренние образцы формы гейта**
- `/home/mm/git/stit-core/go-backend-template/Makefile` — вербы, `make check`, concise-обёртки, `COVER_FLOOR`
- `/home/mm/git/stit-core/go-backend-template/.claude/settings.json` — форма allow/deny
- `/home/mm/git/stit-core/spa-frontend-template/package.json` — `pnpm check`

---

## Приложение: состояние spike'ов после прогона

- `spikes/android-gate/` — 67 файлов исходников, гейт зелёный (`make check` → `✓ check clean`).
  Сборочные выходы (`build/`, `.gradle/`) оставлены для просмотра владельцем, в `.gitignore`.
  Heap-dump `java_pid2713260.hprof` (476 МБ), появившийся при OOM Android Lint, **удалён**.
- `spikes/swift-linux/` — пакет собирается, 6 тестов зелёные, `swift format lint` и
  `swiftlint --strict` чисты, OpenAPI-клиент генерируется и компилируется.
  `.build/` (657 МБ) оставлен, в `.gitignore`.
- Тулчейны в `~/.cache/stit-mobile-research/`: `android-sdk` 806 МБ, `jdk` 346 МБ,
  `swift` 3.6 ГБ, `swiftlint` 245 МБ, `keys/stit-spike.jks` (тестовый ключ подписи),
  `dl/` (скачанные архивы — можно удалять). Gradle-кеши в `~/.gradle` (4.0 ГБ),
  Kotlin/Native в `~/.konan` (822 МБ).
- Gradle- и Kotlin-демоны остановлены (`./gradlew --stop`).
