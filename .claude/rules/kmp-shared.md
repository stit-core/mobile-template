---
name: kmp-shared
description: Что можно и нельзя в shared/ (KMP) — чистый commonMain без platform API, expect/actual только для currentPlatform/ioDispatcher/platformHttpEngine, остальное — интерфейсы PlatformDeps, iosMain на Foundation + Security, cinterop и CocoaPods запрещены, презентер = StateFlow<State> + dispatch(Intent) + close(), слои core < entity < feature. Terse; «как и почему» — docs/reference/architecture.md. Грузится при правке shared/.
paths:
  - "shared/**"
---

# shared/ — KMP-ядро

Полное описание слоёв и пакетов — `docs/reference/architecture.md`. Здесь — что обязан
соблюдать при правке `shared/`.

## Инварианты

- **`commonMain` без platform API.** Импорты `android.*`, `androidx.*`, `java.*`, `javax.*`,
  `platform.*` в `commonMain` запрещены (Konsist `shared/src/jvmTest/…/ArchitectureTest.kt`,
  стадия `shared jvm tests`; стадия `kmp`: `compileKotlinIosArm64`). Время —
  `kotlin.time.Instant`/`Clock` (stdlib); файлы — `okio`; UUID — `kotlin.uuid`.
- **`expect/actual` — только `currentPlatform`, `ioDispatcher`, `platformHttpEngine`**
  (`core/lib/Platform.*.kt`, `Dispatchers.*.kt`, `core/api/HttpEngine.*.kt`). Всё остальное
  платформенное — интерфейсы, собранные в `PlatformDeps` (`SecureStore`, `Settings`,
  `PushTransport`, `NotificationPermission`, `TelemetrySink`, `LogSink`, `LocaleProvider`,
  `ConnectivityMonitor`, `AppInfo`, `filesDir`); реализации — `androidMain`
  (`KeystoreSecureStore`, `DataStoreSettings`, `LogcatSink`), `iosMain` (`KeychainSecureStore`,
  `UserDefaultsSettings`, `NSLogSink`), остальное — `androidApp/core/platform` и Swift `Core/Platform`.
- **`iosMain` — только Foundation и Security** (`NSUserDefaults`, `NSLog`, Keychain через
  `platform.Security`, Darwin-движок Ktor). UserNotifications/LocalAuthentication/Network —
  Swift-реализации интерфейсов.
- **cinterop и CocoaPods запрещены** (`kmp-check.sh` grep по `shared/build.gradle.kts`): они
  ломают компиляцию Apple-klib на Linux. Зависимость без iOS-таргета в `commonMain` не живёт.
- **Слои: `core/**` < `entity/**` < `feature/**`.** `core` не импортирует `entity`/`feature`;
  `entity` не импортирует `feature`; `feature/<a>` не импортирует `feature/<b>` — общее
  поднимается в `entity` или `core`. `core/**` — граница будущего kit'а (08 §1.6).
- **Презентер = `class <X>Presenter internal constructor(deps) { val state: StateFlow<XState>; fun dispatch(intent: XIntent); fun close() }`**
  в `feature/<group>/` (`auth`, `todos`, `profile`, `settings`, `push`, `requirements`, `shell`,
  `demo`); `XState` — immutable `data class`, `XIntent` — `sealed class`; ошибки ловит
  `attempt {}` (`core/lib/Attempt.kt`) → `MessageKey` в state. Без one-off событий, без
  generic-«движков» (Konsist).
- **Composition root — `SharedContainer(platform: PlatformDeps, config: AppConfig)`**: ленивые
  `val` (репозитории, `sessionStore`, `pushRegistrar`, `requirements`, `demoMode`), фабрики
  `<x>Presenter()`. DI-фреймворка в `shared/` нет (Koin/Metro — вне шаблона).
- **Persistence v1 — без БД**: in-memory кэш + `FileCache<T>` (okio, JSON) + `Settings` +
  `SecureStore`. Room/SQLDelight — шов `entity/<slug>/cache`, запись в `dependencies.md`.
- **Сеть — только `core/api/ApiClient`** (Ktor 3.6; OkHttp на Android, Darwin на iOS).
  Retrofit, `security-crypto` — detekt `ForbiddenImport`.
- **Логи — `Logger(tag, sink)` из `core/observability`** (`LogSink` даёт платформа), не
  `println`/`android.util.Log`; в `TelemetrySink` — только breadcrumb `method path → status`.
- **Тест каждой логики — `commonTest`** (запуск `:shared:jvmTest`; там же Konsist
  `ArchitectureTest`), `androidMain` — `androidHostTest` (Robolectric, `robolectric.properties`);
  фейки `testing/Fakes.kt` + `testContainer()` на `MockTransport`, не моки; детерминизм через
  `FakeClock`/`TestDispatchers`/фикстуры. Пол — 80 % строк (`koverVerify`, без `generated/**`).
- Публичный фасад — rule `kmp-public-api`; изменение = `make api-dump` по решению в плане.

## Сигналы

- `make kmp-check` красный → platform API протёк в `commonMain` или появился cinterop.
- `make api-check` красный → фасад изменился без `make api-dump`.
