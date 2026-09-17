# scaffold › api — ресурс бэкенда

Читает строку `shared-api.md` карты (`ресурс · операции · владелец · идемпотентность · нужен
offline-кэш · status`). Операции должны существовать в `api/openapi.yaml` (advisory-проверка
`/mobile-audit`); отсутствуют — BACKEND-HANDOFF, не выдумывание пути.

## Вопросы до начала (≤ 2)

1. Все операции есть в `api/openapi.yaml` (`grep operationId`)? Нет — `make gen` после
   обновления спеки соседа или BACKEND-HANDOFF.
2. Какие операции мутирующие (нужен `Idempotency-Key` — по умолчанию все POST/PUT/PATCH/DELETE)?

## Раскладка

```
shared/src/commonMain/kotlin/<pkg>/core/api/<Resource>Api.kt      # internal class над ApiClient: suspend fun list(): <Resource>ListOutputBody, get(id), create(…), … (сгенерированные модели)
shared/src/commonTest/kotlin/<pkg>/core/api/<Resource>ApiTest.kt  # на MockEngine: путь, метод, заголовки (X-Request-ID, Idempotency-Key), конверт data, ошибки → ApiError
mock/fixtures/{empty,populated,authenticated,error}/<resource>.json
shared/…/core/mock/MockTransport.kt                                # + маршруты ресурса (GET/POST/PATCH/DELETE) с in-memory состоянием
```

Модели — только сгенерированные (`core/api/generated/`), руками не пишутся. Публичным `Api` не
становится: наружу — репозиторий (`/scaffold entity`).

## Шаги

1. `make gen-check` зелёный (модели актуальны).
2. RED: `<Resource>ApiTest` — каждая операция: URL/метод, заголовки, парсинг `data`,
   `Retry-After`, 422 `errors[]` → `ApiError.Http.fieldErrors`.
3. Клиент над `ApiClient` (retry/idempotency/заголовки уже в клиенте — не дублировать).
4. Фикстуры во всех сценариях + маршруты `MockTransport`; `make mock-check`.
5. `make check-fast`; затем `/scaffold entity`, если сущность новая.

## Anti-patterns

- ❌ Рукописные DTO вместо сгенерированных.
- ❌ Свой `HttpClient`/движок в `Api` — только `ApiClient`.
- ❌ Ветвление по HTTP-статусу вместо `ErrorModel.code`.
- ❌ Путь `/api/v1/...`, которого нет в спеке.
