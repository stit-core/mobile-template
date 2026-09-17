---
id: stack.api
kind: api
name: api
status: draft
---

# api

## Spec

| поле | значение |
|---|---|
| source | `<backend-path>/api/openapi.yaml` (дефолт `../backend`) — копия `api/openapi.yaml`, снимок `api/openapi.lock` (sha256 спеки · версия генератора · sha256 сгенерированного) |
| min backend version | _тег/коммит бэкенда, против которого собрано приложение_ |
| используемые ресурсы | `auth` (`/api/auth/*`), `users` (`/api/users/me`), `app` (`/api/app/requirements`), `devices` (`/api/v1/devices`), _reference: `todos`_ |

## Codegen

| что | значение |
|---|---|
| генератор | openapi-generator 7.25.0, `kotlin` (library `multiplatform`), **models-only**, kotlinx-serialization, `ignoreUnknownKeys` |
| конфиг | `config/openapi/kotlin-models.yaml` |
| выход | `shared/src/commonMain/kotlin/tech/stit/mobile/core/api/generated/` — коммитится, руками не правится |
| вербы | `make gen` (копия + генерация + lock; нужны `OPENAPI_GENERATOR_JAR`, `python3` + PyYAML) → `make api-dump` (модели — часть фасада `shared`) · `make gen-check` (дрейф) |
| пост-процессинг | дефекты спеки (`$schema`, `data: [array, null]`, `ErrorModel.code`) — в `scripts/tools/gen.sh`, не в копии спеки |

## Client

Ktor 3.6 (`OkHttp` на Android, `Darwin` на iOS) — `core/api/ApiClient`: базовый URL из
`AppConfig`, `X-Request-ID` (UUID на запрос), `X-Client: android|ios` (`web` в cookie-режиме),
`Accept: application/json`, `Idempotency-Key` на не-GET; клиенты ресурсов — рукописные
`internal class <Resource>Api` над сгенерированными моделями.

## Auth scheme

| механизм | детали |
|---|---|
| режим | `bearer` (дефолт) — см. `auth.md` |
| access | в памяти, TTL с сервера (`expires_in`) |
| refresh | `SecureStore`; ротация; single-flight; `REUSED`/`EXPIRED` → logout |

## Errors and retries

`problem+json` `ErrorModel{code,…}` → `ApiError.Http` / `ApiError.Network` (sealed) →
`MessageKey("error.<code>")`; ретраи 408/429/502/503/504 + `Retry-After` (потолок 5 мин), 500 не
ретраится; `Idempotency-Key` на каждую мутацию (UUID, тело байт-в-байт); 422 `errors[]` →
`ApiError.Http.fieldErrors` (ошибки полей формы).

## Compatibility

Аддитивная эволюция без версии в пути; `ignoreUnknownKeys`, `coerceInputValues`, неизвестный
enum не роняет парсинг; старые версии убивает `GET /api/app/requirements` (`min_version`).

## Mock

Сценарии `empty | populated | authenticated | error` (`MockScenario`); фикстуры
`mock/fixtures/<scenario>/` → объект `Fixtures` (`:shared:embedFixtures`); `MockTransport` (Ktor
`MockEngine` + `MockState`), `DemoMode` (сценарий в `Settings`); flavor/схема `demo`; те же
фикстуры в тестах (`FixturesTest`, `make mock-check`).

## Generates

- `api/openapi.yaml`, `api/openapi.lock`, `config/openapi/kotlin-models.yaml`
- `shared/…/core/api/generated/**` (+ `make api-dump`), `shared/…/core/api/{ApiClient,ApiError,ApiJson,RetryPolicy}.kt`, `core/auth/AuthPlugin.kt`, `<Resource>Api.kt`
- `mock/fixtures/**`, `mock/scenarios.yaml`
- BACKEND-HANDOFF при отсутствующих операциях (B-12, B-17, B-18) и полях (B-20 — `UserView.version`)

## Открытые вопросы

- _что_
