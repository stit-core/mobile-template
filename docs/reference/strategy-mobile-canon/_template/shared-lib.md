# shared-lib

> Сквозное поверхности без доменной привязки: локали, реестр ключей ошибок, политика
> логирования, feature flags, события аналитики. Здесь — **что** нужно поверхности; **чем** это
> реализуется (`Logger`/`LogSink`, `os.Logger`, ресурсы) — платформа код-репо (`docs/stack/`). Из секций
> выводятся `values-<lang>/`, `Localizable.xcstrings`, `MessageKey`, `AppRequirements.flags`.

## Locales

- `locale` — код локали · `ru`, `en` · обяз. · `роль` — `default` \| `supported` · `status`.

| locale | роль | status |
|---|---|---|
| `ru` | `default` | `draft` |
| `en` | `supported` | `draft` |

Локали ⊇ `ru`; плюрализация `one/few/many/other` для ru обязательна там, где есть числа.

## i18n

Реестр ключей ошибок API: код бэкенда (`ErrorModel.code`) → ключ `error.<code>` (код в нижнем
регистре, `MessageKey.forErrorCode`) → текст.

| code | ключ | ru | en | status |
|---|---|---|---|---|
| `AUTH_INVALID_CREDENTIALS` | `error.auth_invalid_credentials` | <текст> | <text> | `draft` |
| `AUTH_REFRESH_EXPIRED` | `error.auth_refresh_expired` | <текст> | <text> | `draft` |
| `RATE_LIMITED` | `error.rate_limited` | <текст> | <text> | `draft` |
| `<CODE>` | `error.<code>` | <текст> | <text> | `draft` \| `stable` |

## Logging

| что логируем | уровень | PII |
|---|---|---|
| навигация (`Route`), сеть (`method path status`, `request_id`) | breadcrumb | нет |
| ошибки презентеров | warn/error | без тел запросов, токенов, email |
| `user.id` после логина | контекст | только id |

## Feature flags

Имена флагов — из `GET /api/app/requirements` (`flags`); каждый используется ≥ 1 фичей.

| флаг | зачем | используется | дефолт офлайн | status |
|---|---|---|---|---|
| `demo_login` | demo-режим в prod для ревью | [`<feature>`] | `false` | `draft` |
| `<flag>` | <зачем> | [`<feature>`] | `<value>` | `draft` \| `stable` |

## Analytics events

Только если `analytics` включён в stack (`preset: consumer`); иначе секция пуста.

| событие | параметры | экран/фича | status |
|---|---|---|---|
| `<event>` | [`<param>`] | `<Key>` \| `<feature>` | `draft` \| `stable` |

## Что shared-lib НЕ

- ❌ Выбор библиотек и файлов ресурсов — `docs/stack/` код-репо.
- ❌ Доменная логика — фичи и сущности.
- ❌ Тексты экранов — они живут при экранах; здесь только сквозные реестры.
