---
name: kmp-public-api
description: Публичный фасад shared/ для Swift — final-классы, data/sealed, suspend/StateFlow, без generic'ов и Any в публичных сигнатурах; apiDump (JVM + klib) коммитится, изменение фасада — осознанное решение в плане. Грузится при правке shared/src/commonMain.
paths:
  - "shared/src/commonMain/**"
---

# Фасад Kotlin → Swift

`shared` потребляется Swift-кодом через Obj-C-экспорт `Shared.framework` (SKIE не подключён —
фасад должен быть удобен и без него). Публичное API `shared` = контракт для `iosApp/` и
`androidApp/`; его снимок — `shared/api/jvm/shared.api` + `shared/api/shared.klib.api`.

## Инварианты

- **Публично — только то, что потребляет UI.** `<Resource>Api`, `ApiClient`, мапперы,
  `FileCache`, `MockState` — `internal`; конструкторы репозиториев, презентеров, `SessionStore`,
  `PushRegistrar` — `internal constructor`. `SharedContainer` — единственная точка входа для
  Swift и для Hilt-модуля.
- **Final-классы, `data class`, `sealed`.** Без `open`/абстрактных базовых классов в
  публичных сигнатурах; интерфейсы — только с абстрактными членами (default-методы в
  Obj-C не экспортируются).
- **Без generic'ов, `Any`, `inline`/value-классов и `suspend`-лямбд в публичных сигнатурах.**
  Возвращай конкретные типы; коллекции — `List<Model>` без вложенных generic-параметров
  пользовательских типов.
- **Асинхронность — `suspend` и `StateFlow`.** Никаких callback-параметров и Rx.
  Презентер: `state: StateFlow<State>`, `dispatch(intent: Intent)`.
- **`Route`, `Intent`, `ApiError` — `sealed`; `State` — `data class`** (Konsist
  `ArchitectureTest` в `jvmTest`). Swift видит их как классы; имена вариантов —
  существительные без коллизий с Foundation (`Loading`, не `Error`).
- **Исключения — только `ApiError`** (`Http`/`Network`); внутри `shared` презентеры ловят их
  через `attempt {}` → `Result` и кладут `MessageKey` в `State`. Публичная `suspend`-функция,
  из которой он может вылететь, помечена `@Throws(ApiError::class, CancellationException::class)`
  (второй класс требует Kotlin/Native): без аннотации Obj-C-экспорт пропускает в Swift только
  `CancellationException`, любое другое исключение роняет процесс.
- **Числа и время**: `Long`/`Int`/`Double` (не `UInt`), `kotlin.time.Instant`; строки —
  `String`; деньги — строка decimal + валюта.
- **Имена файлов и пакетов** не менять ради Swift: Obj-C-имена выводятся из `baseName =
  "Shared"` и имени класса; коллизии решаются `@ObjCName`.

## Процедура изменения фасада

1. Решение в плане (`/plan`): что и зачем меняется в публичном API.
2. Правка кода + тесты (`commonTest`).
3. `make api-dump` (обновляет оба дампа в `shared/api/`) — дампы коммитятся в том же коммите.
4. `make api-check` зелёный; Swift-адаптеры в `iosApp/` — атом «Mac-verify».

Изменение фасада проходит только через верб `make api-dump` и план — Gradle-таск `apiDump`
напрямую не зови (`./gradlew apiDump*` — в `permissions.deny`). `shared/api/*.api` руками не
редактируй (`Edit(shared/api/**)` — deny).
