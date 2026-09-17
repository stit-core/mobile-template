# scaffold › entity — сущность

Читает строку `entities.md` карты (`slug · name · description · ключевые поля · source · cache ·
domain-ref · status`). `<pkg>` — Kotlin-пакет проекта (`tech.stit.mobile` в шаблоне).

## Вопросы до начала (≤ 3)

1. `source`: ресурс `shared-api.md` (тогда есть операции в `api/openapi.yaml`) или `local`?
2. `cache`: `none | memory | snapshot` (`snapshot` = `FileCache<T>` для офлайн-чтения)?
3. Есть ли уже сгенерированная модель (`core/api/generated/`) для DTO?

## Раскладка

```
shared/src/commonMain/kotlin/<pkg>/entity/<slug>/
├── <Slug>.kt              # data class Model (immutable, kotlin.time.Instant, без полей транспорта)
├── <Slug>Repository.kt    # class <Slug>Repository internal constructor(api, cache): SSOT в памяти (StateFlow) + FileCache
└── <Slug>Mapper.kt        # internal fun <Slug>View.toModel() над сгенерированной моделью
shared/src/commonTest/kotlin/<pkg>/entity/
├── RepositoriesTest.kt       # сценарии на testContainer(): populated / empty / error / offline (snapshot)
└── MappersTest.kt            # маппер: обязательные поля, дефолты отсутствующих
mock/fixtures/{empty,populated,authenticated,error}/<slug>.json   # формат = ответ API с конвертом data
```

`SharedContainer`: `val <slug>Repository: <Slug>Repository by lazy { <Slug>Repository(<Resource>Api(apiClient), cache("<slug>", …)) }`.
`MockTransport`: маршрут `GET /api/<resource>` → `MockState` из фикстуры сценария.

## Шаги

1. Модель и маппер (RED: `MappersTest`); `source ≠ local` — сгенерированная модель из
   `generated/` (при отсутствии — сначала `/scaffold api <resource>`).
2. Репозиторий: SSOT в памяти (`MutableStateFlow`), `cache: snapshot` — `FileCache<<Slug>View>`;
   ошибки — `ApiError` наружу, offline — последний снимок (`restore()`), мутации не ставятся в очередь.
3. Тесты на `testContainer(scenario)` (`MockTransport` + `FakePlatformDeps`); фикстуры во всех
   четырёх сценариях, `MockState.load` читает новую сущность.
4. Регистрация в `SharedContainer` (`onSignedOut` → `clear()`); `make api-dump` (решение в плане —
   новый публичный тип); `make mock-check`, `make check-fast`.

## Anti-patterns

- ❌ Модель, повторяющая сгенерированную один в один (тогда маппер не нужен — но `generated` наружу не отдаём).
- ❌ Интерфейс репозитория «на всякий случай»: тесты идут через `MockTransport`, не через второй репозиторий.
- ❌ Платформенный кэш (Room/UserDefaults) — только `FileCache`/`Settings` через `PlatformDeps`.
- ❌ Реальные ПДн в фикстурах.
