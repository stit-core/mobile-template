# Бриф для research-агентов: мобильный шаблон экосистемы stit (Android / iOS)

Дата: 2026-09-17. Язык отчётов — русский (технические идентификаторы, имена пакетов,
команды, названия API — как есть, на английском). Этот файл читается **первым** каждым
агентом; поверх него — промпт конкретного потока.

## Зачем

Экосистема stit (org `stit-core`, Gitea `git.stit.tech`) — набор форкаемых шаблонов с
AI-driven процессом: каждый репо несёт агентную обвязку (`CLAUDE.md`, path-scoped
`.claude/rules/`, скиллы `/smartdev` `/plan` `/init-service` `/stack-deep-dive`
`/de-template` `/scaffold` `/release`, planning-first, forward-only, единый gate). Сейчас
есть `go-backend-template`, `spa-frontend-template`, `telegram-bot-template`,
`project-infra-template-compose`, `corp-infra-template`, `business-strategy-template`;
исследован и запланирован `ssr-frontend-template`. В публичном roadmap
(`stit-public/docs/roadmap.md`) строками «в планах» стоят **`android-template` — нативный
Android** и **`ios-template` — нативный iOS**.

Владелец хочет **глубокий ресёрч**, прежде чем проектировать мобильный шаблон:

> Как вообще делать мобильные приложения на Swift и на Kotlin. Какой язык / подход выбрать —
> Flutter точно не хочу. Лучшие практики Android — как сейчас это делают; лучшие практики
> iPhone — какие проблемы, тонкости, какой стек. Самое главное — найти стек, архитектуру,
> подходы к тестам, к коду, к качеству, к структуре, к файловой структуре. Посмотреть, как
> сделан наш go-backend-template — как пример строгости, как мы реализовали шаблон в другой
> области; не копировать один в один. Как индустрия живёт, какие тренды, какие
> фундаментальные устойчивые практики — всё это разведать.

Задача этой сессии — **ресёрч, не код** (исключение — лёгкие spike'и в потоке 06). Результат —
набор отчётов потоков + сводный синтез оркестратора (`00-synthesis.md`) с решениями,
рисками и планом. Отчёты должны быть **достаточными, чтобы по ним затем проектировать и
писать шаблон без повторного ресёрча**: точные версии, точные имена опций/плагинов/API,
листинги конфигов, ловушки с URL. Это устойчивое требование владельца (так же были
написаны отчёты SSR-ресёрча — образец формата: `spa-frontend-template/docs/research/ssr/`,
в частности `round2/08-spike-astro.md` и `00-synthesis.md`).

## Жёсткие ограничения

- **Ничего не менять** в репозиториях под `/home/mm/git/stit-core/` — читать можно всё.
  Писать **только** в свой файл отчёта
  `/home/mm/git/stit-core/mobile-template/docs/research/<NN>-<stream>.md` (и, для потока 06,
  в `…/docs/research/spikes/<name>/`). Черновики — в scratchpad
  `/tmp/claude-1000/-home-mm-git-stit-core-spa-frontend-template/eb7a87b6-f89f-4211-8db1-2a6cec712211/scratchpad/<stream>/`.
- **Scratchpad лежит на tmpfs (свободно ~3 ГБ)** — SDK, toolchain'ы, кеши сборок туда не
  класть. Toolchain'ы — в `~/.cache/stit-mobile-research/<tool>/` (диск, 500+ ГБ свободно);
  Gradle home — дефолтный `~/.gradle`.
- **Knowledge cutoff моделей — июнь 2026, сегодня 2026-09-17.** Версии SDK/библиотек/
  инструментов, статусы (stable/beta/deprecated), политики сторов, законы — **проверять через
  web** (официальные доки, GitHub releases, Maven Central / Google Maven, Swift Package Index,
  developer.apple.com, developer.android.com, kotlinlang.org, первоисточники законов) и
  **цитировать URL**. У каждого факта пометка: «проверено (источник, дата)» или
  «предположение». Не выдумывать API; не удалось проверить — так и писать.
- Веб-инструменты: загрузи схемы `ToolSearch` → `select:WebSearch,WebFetch`. `curl` тоже
  доступен (например, Maven metadata: `https://repo.maven.apache.org/maven2/<group path>/<artifact>/maven-metadata.xml`,
  Google Maven: `https://dl.google.com/dl/android/maven2/<group path>/group-index.xml`,
  GitHub API: `https://api.github.com/repos/<owner>/<repo>/releases/latest`).
- **macOS/Xcode на машине нет** — iOS-факты, требующие Xcode, эмпирически не проверяются;
  честно помечать «по документации», не изображать проверку.
- Машина: Debian 13 x86_64, 20 CPU, 15 ГБ RAM, но **под нагрузкой других процессов** (load
  average ~15, свободно ~3 ГБ + swap) — сборки медленные, это не ошибка. Ограничивать Gradle
  (`org.gradle.jvmargs=-Xmx2g`, `--max-workers=4`), эмулятор Android не поднимать без явной
  необходимости (`/dev/kvm` есть, но памяти мало). Установлено: JDK 21 (Debian, `/usr/bin/java`),
  Docker 29, Node 24, Python 3.13, git 2.47. **Нет**: Swift toolchain, Android SDK, Gradle,
  kotlinc. Сеть до dl.google.com, maven.google.com, repo.maven.apache.org,
  services.gradle.org, download.swift.org, github.com, developer.apple.com,
  plugins.jetbrains.com, rustore.ru — открыта (проверено 2026-09-17).
- Параллельно работают другие агенты. Чужие каталоги не трогать, чужие процессы не убивать
  (`pkill java`/`killall` запрещены — только свои PID). В конце остановить свои фоновые
  процессы (Gradle daemon: `./gradlew --stop`).
- Не тратить > ~25 минут на один тупик — зафиксировать и идти дальше.
- Вопросы владельцу не задавать — фиксировать в разделе «Риски и открытые вопросы».

## Экосистема stit (факты, проверены чтением репозиториев 2026-09-17)

### Шаблоны и контракты

- 6 форкаемых шаблонов `stit-core/`: `corp-infra-template` (корп control-plane: Traefik +
  CrowdSec, Gitea = registry `git.stit.tech`, Authentik, Vaultwarden, MinIO, ntfy),
  `project-infra-template-compose` (→ `<org>/infra`, один VPS на среду, docker-compose +
  Traefik v3), `go-backend-template` (→ `<org>/backend`), `spa-frontend-template`
  (→ `<org>/frontend`), `telegram-bot-template` (→ `<org>/<bot>`),
  `business-strategy-template` (→ `<org>/strategy`, design-time source of truth).
  Библиотеки: `golibinfra` (Go-kits), `pylibinfra` (Python). Канон карты —
  `corp-infra-template/docs/ecosystem.md`. Витрина — `stit-public`.
- **Sibling-checkout**: форки одной org лежат рядом `~/git/<org>/{strategy,infra,backend,frontend,<bot>}`;
  сосед резолвится полями `project-slug` / `strategy-path` (дефолт `../strategy`) / `surface`
  в `docs/stack/service.md` сервис-репо.
- **Операционная модель**: проектирование — в `<org>/strategy`; операционный хаб — `<org>/infra`;
  сервис-репо — исполнители, работают своей обвязкой; **STOP-граница сервис-репо —
  image-in-registry**, деплой тега — `make bump SVC=<svc> TAG=vX.Y.Z` в `<org>/infra`.
  **CI-runner'ов в экосистеме нет по сознательному решению** (Gitea без runner'ов): образы
  собирает разработчик локально (`docker buildx … --push`, скилл `/release`). Для мобильного
  шаблона «образ в registry» не существует — артефакт = подписанный AAB/APK/IPA, а канал
  доставки — сторы/тест-дистрибуция. Что такое STOP-граница мобильного репо — открытый
  вопрос ресёрча.
- **Бэкенд** (`go-backend-template`, `/home/mm/git/stit-core/go-backend-template`):
  huma/v2 + chi + pgx/v5 + sqlc + uber/fx + golibinfra; OpenAPI 3.1 генерируется huma в
  рантайме (эндпоинты `/openapi.json` и `/docs` приложения). **Auth — только cookie**:
  `POST /api/auth/login` ставит httpOnly cookies `access_token` + `refresh_token`
  (`SameSite=Strict`, `Path=/`, `Secure` по env `COOKIE_SECURE`, без атрибута Domain);
  `POST /api/auth/refresh` читает cookie `refresh_token` и ротирует пару; `POST /api/auth/logout`;
  `POST /api/auth/change-password`. OpenAPI security scheme — `Cookie` (apiKey in cookie
  `access_token`). Заголовок `Authorization: Bearer` ни бэкенд, ни `golibinfra/authnkit`
  (`jwt.go`, `hasher.go`, `totp.go`, `permissions.go`) не поддерживают. Бэкенд живёт на
  `api.${DOMAIN}` за Traefik/CrowdSec; healthcheck `/health`; структурные JSON-логи;
  Sentry/OTel — через golibinfra `telemetry`.
- **Строгость go-backend-template (образец «как мы делаем шаблон»)** — читать сами файлы:
  `CLAUDE.md` (ядро ≤ 200 строк + роутер), `.claude/rules/{domain-architecture,tests-pipeline,
  events-and-messaging,sqlc-and-migrations,infra-impact,env-local,comments,forward-only}.md`
  (path-scoped через `paths:`), `.claude/settings.json` (deny `--no-verify`, force-push,
  правка сгенерированного), `Makefile` (гейт `make check` = fmt-check + vet + lint + test +
  build + gen-check + migrations-check + cover-check с полом 75 %), `.golangci.yml` (forbidigo:
  запрет `fmt.Print*`/`slog.*`; depguard: домены не импортят друг друга, cross-domain только
  через порты + адаптеры в `module/adapters.go`; errcheck/gosec/…), `docs/reference/architecture.md`
  (слои домена: `entity.go`/`dto.go`/`errors.go`/`policy.go`/`usecase.go`/`events.go`/`metrics.go`/
  `delivery/{http,nats}`/`infra/postgres`/`module`; обязательность файлов; non-goals),
  `test/README.md` (e2e pytest-харнес поверх docker-compose), `docs/stack/README.md`
  (8 kinds с секцией **Generates**, `status: draft|stable`, вход для `/de-template`),
  reference-домены `auth`/`user`/`verification` (изучить, потом удалить под свой проект).
  Принципы: planning-first (единый `.claude/plans/roadmap.md`), forward-only (текст только о
  текущем состоянии, без «было/стало»), комментарии только WHY, «не городить велосипеды —
  примитив в библиотеку», TDD для багфиксов (RED→GREEN), моки генерируются (mockery),
  `t.Parallel()` везде, coverage-gate.
- **Фронт** (`spa-frontend-template`, `/home/mm/git/stit-core/spa-frontend-template`):
  TypeScript 6 / Vite 8 / React 19.2 + Compiler / TanStack Router (file-based) + Query + Form /
  Zustand / Zod 4 (рукописные контракты API, без кодогена) / Tailwind v4 с собственным
  токен-слоем (design-system-agnostic, без внешней DS) / MSW mock-режим со сценариями
  `empty | populated | authenticated | error` / Lingui runtime-only i18n ru/en / Sentry
  условно + web-vitals / Biome + ESLint + Steiger (архитектурный линтер FSD) / Vitest +
  Playwright / lefthook / size-limit. **FSD** (`app → pages → widgets → features → entities →
  shared`, public API `index.ts`, запрет сегментов по типу). Гейт `pnpm check` = typecheck +
  lint + format:check + test + steiger + build + size. Reference-слайсы `features/auth`,
  `pages/profile`, `pages/todos`, `features/theme`. Runtime config «один образ на все среды»
  (`window.__APP_CONFIG__` из env контейнера). Обвязка: `CLAUDE.md`, rules
  `{fsd-architecture,shared-ui,tests-pipeline,env-local,comments,forward-only}`, 7 скиллов.
- **Бот** (`telegram-bot-template`): aiogram 3 + aiogram_dialog + dishka поверх
  `pylibinfra[tgbotkit]`; ruff + mypy strict + import-linter (7 контрактов слоистости);
  гейт `make check`; вся инфра — в библиотеке, шаблон несёт только бизнес-логику.
- **Strategy** (`business-strategy-template/docs/projects/_template/`): реестр поверхностей
  `project/surface.md` с `type: web | mobile | api | admin | bot | desktop | embed | other`
  и `lives-at` (для mobile — app-id/store); UX-слой `interface/<surface>/` (who, stage,
  situation, zone, block, ux-substrate, data-items); слой композиции `screens/<surface>/`
  (flow, screen, block, shell — где `flow` описывает адаптив «1 desktop-экран ↔ N mobile-экранов»
  как **viewport-вариативность веба**, не нативное приложение); для SPA-поверхностей —
  FSD-карта `fsd/<surface>/` (8 файлов слайсов, вход `/scaffold` и `/de-template` фронта);
  для `type: bot` — `telegram-bot-flow/<surface>/`. **Для `type: mobile` слоя-моста к коду
  нет** — это пробел, который должен закрыть дизайн шаблона (аналог FSD-карты / bot-flow для
  мобильных модулей/экранов).
- **Инфра** (`project-infra-template-compose`): один VPS = одна среда; сервисы — `compose/<svc>.yml`
  с Traefik-labels по сабдоменам; секреты в `.env` VPS; CrowdSec; для мобильного приложения
  инфра-поверхность — `.well-known/assetlinks.json` и `apple-app-site-association` (deep
  links), push-шлюзы, endpoints для нескольких сред (dev/staging/prod = разные VPS/домены).

### Профиль будущих приложений (предположение оркестратора — владелец подтвердит)

Бизнес-приложения-компаньоны к нашим Go-бэкендам (CRM, задачи, звонки, сделки, календарь —
см. проекты `~/git/stit-*`, `voron-crm`, `wewall-*`): авторизация, списки/формы/детали,
push-уведомления, deep links, камера/QR, карты, файлы; offline-tolerant, не offline-first;
локали ru/en; **рынок — в первую очередь РФ** (RuStore, Яндекс-сервисы, законы РФ), но и
Google Play / App Store. Не игры, не медиа-heavy, не AR. Заказчик получает исходники и владеет
ими (open-core: шаблоны Apache-2.0, без vendor-lock и без обязательных SaaS).

## Что хотим получить от ресёрча (сводно)

1. **Канон Android 2026** (поток 01): стек, архитектура, модульность, файловая структура,
   тесты, качество, инструменты, ловушки — «как сейчас делают правильно» с источниками.
2. **Канон iOS 2026** (поток 02): то же для Swift/SwiftUI; проблемы и тонкости платформы.
3. **Развилка «сколько кодовых баз и на чём»** (поток 03): нативно ×2 vs Kotlin Multiplatform
   (shared logic + native UI) vs Compose Multiplatform (общий UI) vs Swift на Android / Skip vs
   Rust/Go-ядро + native UI; Flutter/React Native — вне рассмотрения (только одной строкой
   «почему нет»). Матрица с весами ниже, рекомендация для экосистемы.
4. **Индустрия** (поток 04): тренды 2025–2026, устойчивые практики, как делают известные
   компании (в т. ч. российские), опросы, open-source образцы, AI-агенты в мобильной
   разработке.
5. **Дистрибуция, релизы, РФ-специфика** (поток 05): сторы (Google Play, App Store, RuStore,
   AppGallery, GetApps), подпись, версионирование, тест-дистрибуция, CI для iOS без Mac,
   push в РФ, платежи, аналитика/крэши self-host, законы РФ для мобильных приложений.
6. **Инструменты и agent-friendliness** (поток 06): что реально делается headless на Linux
   (Android — да; Swift — частично), гейт-цепочки для обеих платформ, эмпирический spike
   (Android-проект с гейтом на этой машине; SwiftPM-пакет на Linux), время/дисковая цена,
   AI-тулинг (MCP для Xcode/Gradle).
7. **Контракт с бэкендом и остальной экосистемой** (поток 07): auth (cookie vs bearer),
   OpenAPI-кодоген для Kotlin/Swift, mock-режим, окружения/конфиг, deep links, push,
   observability, i18n, токены дизайна; слой-мост в strategy для `type: mobile`.
8. **Дизайн шаблона** (поток 08, после остальных): один репо или два, имена, форк-цели,
   агентная обвязка (CLAUDE.md, rules, skills), `docs/stack` kinds, reference-фичи, гейт,
   STOP-контракт, cross-repo изменения, план реализации.

## Рабочие критерии оценки подхода (веса из 100; владелец может пересмотреть)

| критерий | вес | что именно |
|---|---|---|
| Нативное качество и полнота платформы | 20 | UX/производительность как у платформы, доступ к API day-0 (iOS 26 / Android 16+), системные компоненты, accessibility, размер приложения |
| Зрелость и риск | 20 | stable-статус, обязательства вендора, каденс ломающих изменений, размер сообщества, найм в РФ, lock-in |
| Agent-friendliness / headless-тулинг | 20 | сборка и тесты из CLI (в идеале на Linux), детерминизм, скорость цикла, качество доков для агентов (`llms.txt`, стабильные API), MCP/CLI-инструменты |
| Стоимость дублирования / доля общего кода | 15 | сколько пишется дважды (домен, сеть, модели, тесты), цена синхронизации двух баз |
| Тестируемость и качество | 15 | unit / UI / snapshot / e2e без эмулятора где можно, линтеры, архитектурные тесты, coverage-гейт |
| Совместимость с экосистемой | 10 | OpenAPI-контракт с Go-бэкендом, self-host observability, без обязательных SaaS, дистрибуция в РФ, open-core |

## Формат отчёта потока

Файл `/home/mm/git/stit-core/mobile-template/docs/research/<NN>-<stream>.md`, по-русски,
подробно, **без экономии** (ориентир — 600–2000 строк, как отчёты SSR-ресёрча):

1. **TL;DR** — 8–15 пунктов главных выводов.
2. **Findings** — по вопросам потока; у каждого факта — источник (URL) и пометка
   «проверено (источник, дата)» / «предположение». Таблицы приветствуются.
3. **Рекомендации для шаблона** — конкретно: структура каталогов, пакеты/плагины с
   версиями, конфиги (листинги), правила для `.claude/rules`, тесты, гейт. Черновики
   кода/конфигов приветствуются (короткие, помеченные как черновик).
4. **Ловушки** — известные проблемы с URL на issue/доки, версии, обходные пути.
5. **Риски и открытые вопросы владельцу** — что решать владельцу, что проверять дальше.
6. **Сводная таблица версий** — всё упомянутое: пакет/инструмент · версия · дата · лицензия · URL.
7. **Источники** — список URL.

В финальном ответе оркестратору — резюме ≤ 300 слов + путь к файлу. Отчёт пишется
инкрементально (сначала каркас с TL;DR-заглушкой, потом секции) — чтобы при обрыве сессии
не потерять сделанное.

## Карта потоков

| файл | поток |
|---|---|
| `01-android-canon.md` | канон Android: стек, архитектура, структура, тесты, качество |
| `02-ios-canon.md` | канон iOS: стек, архитектура, структура, тесты, качество, тонкости |
| `03-shared-strategy.md` | развилка «сколько кодовых баз и на чём», матрица с весами |
| `04-industry.md` | тренды, устойчивые практики, компании, опросы, AI-агенты |
| `05-distribution-ru.md` | сторы, подпись, релизы, CI без Mac, push/платежи/аналитика/право РФ |
| `06-tooling-agent.md` + `spikes/` | headless-тулинг на Linux, гейт, эмпирические spike'и |
| `07-backend-contract.md` | auth, OpenAPI-кодоген, mock, конфиг сред, deep links, push, observability, слой в strategy |
| `08-template-design.md` | дизайн шаблона(ов) и место в экосистеме |
| `00-synthesis.md` | сводный синтез (оркестратор) |
| `prompts/PROMPTS.md` | промпты агентов как запускались |
