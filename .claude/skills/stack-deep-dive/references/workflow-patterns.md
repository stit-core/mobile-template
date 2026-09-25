# Три режима ведения сеанса

Скилл выбирает режим по виду args.

## A. Заведение

**Сигнал**: `<kind>`, файла `docs/stack/<kind>.md` нет.

1. `Read docs/stack/README.md § Kinds` и `docs/stack/_templates/<kind>.md`.
2. `cp docs/stack/_templates/<kind>.md docs/stack/<kind>.md`; frontmatter из имени файла
   (`id: stack.<kind>`, `kind`, `name`, `status: draft`) — без вопросов.
3. `platforms` из `service.md`: при `[android]` iOS-секции удаляются (не пустые — отсутствуют);
   при `[ios]` — Android-секции.
4. Условный kind (`analytics`, `payments`) — только если `preset`/флаг требует (инвариант 7);
   иначе спросить «действительно нужен?».
5. Раунд 1 `AskUserQuestion` — одна фраза роли/фундамента (Other) → режим C.

**Завести недостающие разом**: сводка показала `K из 14 нет` → `AskUserQuestion`: «завести все
недостающие (frontmatter + дефолты шаблона) и копать по волнам» / «только выбранный». По
«все» — копирование, затем C с `service` (или первого по волнам).

## B. Сырьё

**Сигнал**: свободный текст / файл (`build.gradle.kts` другого проекта, `xcconfig`, ТЗ
заказчика про магазины, письмо про аккаунты).

1. Прочитать материал.
2. Извлечь платформенные решения → привязать к kinds: id/bundle → `service`; версии →
   `runtime`; спека/бэкенд → `api`; auth → `auth`; хосты/ссылки → `deep-links`; провайдеры →
   `push`; DSN → `observability`; токены/локали → `styling`; магазины/аккаунты →
   `distribution`; ключи → `signing`; данные/permissions/право → `compliance`; SDK →
   `dependencies`; env → `config`.
3. План в чат («Из материала вижу: …») → `AskUserQuestion` подтверждение по группам.
4. По подтверждению — завести/дополнить драфты; дыры обязательных секций — `**blocker**` в state.
5. `AskUserQuestion` — какой kind углубить первым (дефолт по волнам) → режим C.

Не объявлять извлечённое «зафиксированным»; не выводить `Generates` из сырья; не выдумывать
значения, которых в материале нет.

## C. Углубление

**Сигнал**: `<kind>` существующего артефакта или выбор из сводки.

1. `Read` артефакт целиком; `Read state/<kind>.md`.
2. `Read` соседей по cross-kind sync (`SKILL.md § Cross-kind sync`): rollup `config` — всегда.
3. Пройти обязательные H2 по `references/field-checklists.md`: пустые/плейсхолдерные секции,
   anti-pattern-подозрения, **`Generates`** (главная дыра), cross-kind-дрейф.
4. Раунд `AskUserQuestion` (≤ 4): дыры → Other; anti-patterns → «согласен / нет» (Other);
   дрейф → «добавить в rollup» / «убрать из kind'а» / «оставить и пометить blocker».
5. Применить ответы `Edit`'ом; mirror-обновление `config.md` — черновиком, затем
   `AskUserQuestion` «вот что добавил в config — окей?».
6. Чек-лист `references/draft-stable.md`; все ✅ → промоушн.

Остался `**blocker**` (нет отпечатка, аккаунта, Mac, ответа бэкенда) → зафиксировать в state,
оставить `draft`, предложить следующий kind по волнам.

## Стиль вопросов

| ситуация | формат |
|---|---|
| anti-pattern (токен в `SharedPreferences`, один хост на все среды, Play при `enterprise`) | бинарные опции «соответствует» / «нет, вот почему» (Other) |
| enum (`auth.mode`, `preset`, `receiver`, провайдер push, канал) | опция на значение; дефолт шаблона помечен |
| тест-критерий из `draft-stable.md` | ✅/❌-примеры + Other |
| выбор из карты strategy (пути `entry-points.md`, permissions) | опция на запись + «нужна новая» |
| «promote draft → stable?» | `Да` / `Нет, ещё не уверен` |

Только Other: хосты, `applicationId`/bundle id, отпечатки, аккаунты и их владельцы, DSN,
имена SDK, числа (TTL, пороги), тексты App Review notes, `Generates`.

Чего не делать: предлагать «может, хост `api.acme.ru`?»; > 4 вопросов; закрывать сессию без
чек-листа хотя бы по одному правленому артефакту; пропускать rollup в `config.md`.
