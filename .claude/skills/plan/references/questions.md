# Планировочные вопросы

Список для шага 1 skill `plan`. Выбирай 3–5 релевантных задаче — не задавай всё подряд.
Дизайн-вопросы задавай через `AskUserQuestion`. Если на 5+ ответ «не знаю / решай сам» —
задача недозрелая: предложи research-spike или отложить.

## Скоуп
- Что входит в задачу? Что осознанно НЕ входит?
- Это новая фича / рефакторинг / эксперимент? (Багфикс → `triage-failure`, не план.)
- Связано ли с другими задачами/секциями roadmap?

## Платформы
- Обе платформы или одна (`service.md#platforms`)? Что из логики уезжает в `shared`, что
  остаётся платформенным (UI, permissions, ОС-API)?
- Есть Mac-хост для верификации iOS-атомов? Нет — атом «Mac-verify» остаётся открытым.

## Карта в strategy
- Есть ли stable-запись в `mobile/<surface>/` (экран, фича, entity, entry-point, permission)?
  Нет — сначала `/mobile-deep-dive` там или `--from-args` с записью в roadmap.
- Какие состояния экрана (`loading / empty / populated / error / offline`)? Offline-политика?

## Контракт API
- Нужный endpoint/поле есть в `api/openapi.yaml` соседа? Нет — BACKEND-HANDOFF.
- Мутация идемпотентна (`Idempotency-Key`)? Пагинация, `Retry-After`?
- Форма ошибок: какие `ErrorModel.code` → какие `MessageKey`?

## Фасад shared
- Меняется публичный API `shared` (новый `Route`, `State`, презентер)? → `make api-dump` и
  Swift-адаптер (Mac-verify).
- Нужен новый `PlatformDeps`-интерфейс (ОС-возможность) или хватает Foundation-actual?

## Навигация, deep links, push
- Новый маршрут/deep link? Строка в `deep-links.md` + `entry-points.md`, хост в `wellknown`
  (INFRA-HANDOFF)?
- Push-категория/событие? Кто шлёт (бэкенд), payload, дедуп?

## Permissions и право
- Новое разрешение? Rationale в `permissions.md`, строка в `compliance.md`, usage-description.
- Новый SDK? Запись в `dependencies.md`, privacy manifest, трансграничка.
- Меняются собираемые данные (Data safety / Nutrition Label)?

## Безопасность
- Токены/секреты не появляются вне `SecureStore`? Ничего не логируется из auth?
- Кто может вызывать операцию — есть проверка сессии?

## Тесты
- Какой тест RED первым: презентер (`commonTest`), экран (Robolectric + Roborazzi),
  `DeepLinkParser`, `AuthPlugin`? Нужна новая негативная фикстура гейта?
- Эталоны скриншотов меняются осознанно (`make screenshots-record`)?

## Релиз и совместимость
- Старые версии приложения переживут изменение (аддитивная эволюция)? Нужен
  `min_version` в `requirements`?
- Влияет на `versionCode`/каналы/чек-лист магазинов?

## Инфра-поверхность (→ секция Infra impact + skill infra-handoff)
- Новый хост / отпечаток / `SENTRY_DSN` / `MOBILE_*` env для `wellknown`?
- Новый endpoint или auth-режим у бэкенда (B-*)?
- Расхождение с картой `mobile/<surface>/` (S-*)?
