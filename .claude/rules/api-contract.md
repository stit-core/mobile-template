---
name: api-contract
description: api/openapi.yaml — копия контракта бэкенда, правится только make gen; модели генерируются, клиенты пишутся руками над ApiClient; конверт data, ErrorModel.code → MessageKey, X-Request-ID, Idempotency-Key, Retry-After; аддитивная эволюция + ignoreUnknownKeys; несовместимость — BACKEND-HANDOFF. После make gen — make api-dump (модели — часть фасада). Грузится при правке api/, config/openapi/, core/api, *Mapper*.kt.
paths:
  - "api/**"
  - "config/openapi/**"
  - "shared/**/core/api/**"
  - "shared/**/entity/**/*Mapper*.kt"
---

# API-контракт

## Инварианты

- **`api/openapi.yaml` — копия `<backend-path>/api/openapi.yaml`** (`docs/stack/api.md`,
  дефолт `../backend`); правится только `make gen` (копирование + генерация + `api/openapi.lock`).
  Правка спеки инструментами агента — deny (`Edit(api/openapi.yaml)`). Нет соседа —
  reference-спека шаблона остаётся как есть.
- **Модели генерируются** (openapi-generator 7.25, `kotlin` multiplatform, models-only,
  kotlinx-serialization, конфиг `config/openapi/kotlin-models.yaml`) в
  `shared/src/commonMain/kotlin/tech/stit/mobile/core/api/generated/` и **коммитятся**;
  руками не правятся (deny + `make gen-check`). Дефекты спеки чинятся пост-процессингом в
  `scripts/tools/gen.sh`, не правкой спеки.
- **Клиенты — руками**: `internal class <Resource>Api` (`App`, `Auth`, `Devices`, `Todos`,
  `Users`) над `ApiClient` (Ktor 3.6): базовый URL из `AppConfig`, `X-Request-ID` (UUID на
  каждый запрос), `X-Client: android|ios` (`web` в cookie-режиме), `User-Agent:
  <appId>/<version>+<build> (<os> <ver>; <model>; <client>)` из `AppInfo`, `Accept-Language` из
  `LocaleProvider`, `Accept: application/json`.
- **Мутации — с `Idempotency-Key`** (UUID на операцию, переиспользуется при ретрае, тело
  байт-в-байт: `Json { encodeDefaults = true }`); ретраи 408/429/502/503/504 с backoff и
  `Retry-After` (потолок 5 мин); 500 не ретраится; POST — только с ключом.
- **Конверт `data` / `data + total`**; списки — всегда массив (`orEmpty()`), ошибки —
  `problem+json` `ErrorModel{code,…}` → `ApiError.Http(status, code, detail, requestId,
  fieldErrors)` / `ApiError.Network` → `MessageKey("error.<code>")` (`Throwable.toMessageKey()`);
  `detail` — только в логи. Ветвление — по `code` (`KnownErrorCodes`), не по `status`.
- **Совместимость — аддитивная**: `ignoreUnknownKeys = true`, `coerceInputValues = true`,
  неизвестный enum не роняет парсинг; версии в пути нет; старые версии приложения убивает
  только `GET /api/app/requirements` (`min_version`).
- **Изменение контракта**: новый endpoint/поле — сначала в спеке бэкенда → `make gen`
  (нужны `OPENAPI_GENERATOR_JAR` и `python3` + PyYAML — `make doctor` предупреждает) →
  `make api-dump` (сгенерированные модели — часть публичного фасада) → `make gen-check`
  зелёный → `FixturesTest` (фикстуры `mock/` парсятся строгими моделями). Отсутствие нужной
  операции у бэкенда — BACKEND-HANDOFF (skill `/infra-handoff`), не правка копии спеки.
- **`api/openapi.lock`** (sha256 спеки · версия генератора · sha256 сгенерированного) —
  часть STOP-1 ассетов; дрейф от соседа — WARN в `make status`, FAIL только при
  рассинхроне копии и моделей.
- Cookie-режим (`AuthMode.Cookie`, `HttpCookies` в памяти) — только по `docs/stack/auth.md#mode`
  (rule `auth-and-session`); pinning выключен (шов — `network_security_config.xml` / `NSPinnedDomains`).

## Сигналы

- `make gen-check` FAIL → модели правлены руками или lock устарел → `make gen`.
- `make status` → `GEN: backend drift` → спека соседа ушла вперёд; `make gen` + сверка
  `FixturesTest`; несовместимость — BACKEND-HANDOFF.
