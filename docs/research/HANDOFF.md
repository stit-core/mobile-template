# Handoff: ресёрч мобильного шаблона (Android / iOS) — закрыт 2026-09-17

Итог — `00-synthesis.md` (сводный синтез восьми потоков: решения §1, платформы §2, эмпирика
§3, развилка кодовых баз §4, дистрибуция и РФ §5, контракт с бэкендом §6, индустрия §7, форма
шаблона §8, cross-repo §9, вопросы владельцу §10, риски §11, план §12). HTML-версия синтеза
опубликована как артефакт claude.ai — `https://claude.ai/code/artifact/7f0f2478-e584-49c3-b6f5-9ba6a462884c`
(при обновлении публиковать с `url`, favicon не менять; исходник —
scratchpad-шаблон, в который подставляется текст `00-synthesis.md`). Папка
`mobile-template/` создана как место ресёрча; git-репозитория в ней нет — инициализация и состав
коммита решает владелец (сборочные выходы spike'ов уже в `.gitignore`).

## Решение

**Один шаблон `mobile-template` (форк `<org>/mobile`): Kotlin Multiplatform-ядро `shared/` +
нативный UI `androidApp/` (Compose) и `iosApp/` (SwiftUI).** Не два (`android-template` +
`ios-template`) и не четыре (два шаблона + два kit'а): Android-only / iOS-only — пути
`/de-template --platform`, продуктовое деление — пресеты `companion | consumer | enterprise`,
kit-библиотеки — по триггеру «второй форк + общий фикс». Две строки публичного roadmap
сворачиваются в одну (формулировки — синтез §8.1). Запасной вариант — два нативных шаблона
(не Compose Multiplatform), откат дешёв через `subtree split`. Обоснование — синтез §1 п. 1–2,
матрица — §4, форма — §8, план — §12.

## Что где

| файл | что |
|---|---|
| `00-synthesis.md` | сводный синтез — единственный «вход» для реализации |
| `brief.md` | факты об экосистеме, ограничения, критерии и веса, формат отчётов |
| `01-android-canon.md` | канон Android 2026 (AGP 9, Nav3, Compose Test v2, модульность, тесты, гейт, черновики конфигов, ловушки) |
| `02-ios-canon.md` | канон iOS 2026 (Xcode 27 / Swift 6.4 / iOS 27, approachable concurrency, SwiftUI + `@Observable`, SwiftPM без Tuist, Swift Testing, snapshot, 30 ловушек, черновики) |
| `03-shared-strategy.md` | развилка «сколько кодовых баз»: матрица, KMP/CMP/Skip/Rust/Go, Shopify, найм hh.ru, формы шаблона, триггеры пересмотра |
| `04-industry.md` | тренды 2025–2026, опросы, компании (мир/РФ), фундамент vs мода, AI-агенты, community-шаблоны, «зашить/опция/избегать», 20 инвариантов |
| `05-distribution-ru.md` | сторы (RuStore API, Play, App Store), push в РФ, платежи/карты/вход, observability, право, CI без Mac, STOP-контракт, `/release`, cross-repo |
| `06-tooling-agent.md` + `spikes/{android-gate,swift-linux}/` | эмпирика на Linux: Android-гейт (времена, память, диск), KMP-klib под iOS, Swift 6.4 + Swift Testing + OpenAPI Generator, конфликты форматтеров, AGP 9 и convention-плагины, `android` CLI, permissions, негативные фикстуры |
| `07-backend-contract.md` | auth (Bearer + BACKEND-HANDOFF), спек и кодоген (эмпирика), mock, среды/конфиг, deep links (`wellknown`), push, observability, i18n/токены, слой `mobile/<surface>/` в strategy, kinds |
| `08-template-design.md` | форма шаблона: сколько репо, дерево, обвязка (CLAUDE.md-листинг, 23 правила, settings, 9 скиллов), гейт, docs/kinds, слой-мост, STOP, cross-repo, план, 44 вопроса, риски |
| `_synthesis-notes.md` | рабочие заметки оркестратора (сжатые выжимки потоков) |
| `prompts/PROMPTS.md` | промпты агентов как запускались, модели, хроника прогона (в т. ч. падение по лимиту и возобновление) |

Тулчейны spike'ов лежат вне репо: `~/.cache/stit-mobile-research/` (Android SDK 806 МБ, JDK 346 МБ,
Swift 3,6 ГБ, SwiftLint 245 МБ, openapi-generator 31 МБ, `dl/` 1,3 ГБ тарболлов), `~/.gradle`
(4,0 ГБ), `~/.konan` (822 МБ). Для повторного прогона spike'ов: `spikes/android-gate` —
`ANDROID_HOME=~/.cache/stit-mobile-research/android-sdk JAVA_HOME=~/.cache/stit-mobile-research/jdk/jdk-21.0.12.1+1 make check`
(см. `06-tooling-agent.md` §8.1); `spikes/swift-linux` — `~/.cache/stit-mobile-research/swift/*/usr/bin/swift test`.
Всё это можно удалить целиком — репо от каталогов не зависит.

## Как продолжать

1. Решение зафиксировано в `DECISION.md` (один репо `mobile-template`, стек, гейт, STOP, обвязка,
   принятые дефолты, отложенные вопросы, условия пересмотра). Блокирующих вопросов для сборки
   репозитория нет; отложенные (Mac, Apple-аккаунт, store-аккаунты, правовые тексты) касаются
   iOS-ветки и STOP-2 реального проекта.
2. Следующая сессия — сборка репозитория по `prompts/NEXT-SESSION.md`: промпт основной сессии,
   оркестрация Fable-агентов (каркас + гейт → `shared/` ∥ обвязка → `androidApp/` → проверка
   основным агентом), готовые промпты агентов, стандарт качества, тестовая политика, протокол
   проверки. Критический путь до Android-релиза — без Mac.
3. Cross-repo P0 (синтез §9: corp-канон C-1…C-3, бэкенд B-1…B-3/B-12/B-17, инфра I-1/I-6,
   strategy S-1…S-3, stit-public P-1) — отдельные сессии в соседних репо; шаблон стартует с
   fallback'ами и им не блокируется.
4. Если понадобится доисследовать: агенты запускаются по `prompts/PROMPTS.md` (≤ 5 одновременно;
   упавшего можно возобновить `SendMessage` по его id — контекст сохраняется).

## Что не проверено (сводно; полные списки — в отчётах)

- Ни один iOS-факт не проверен на Mac/Xcode (02, 05, 07, 08 — «по документации»); спайки S-1
  (SKIE + direct integration), S-10 (buildable folders + umbrella `Features`), S-15
  (`exportArchive` без fastlane) — только на Mac.
- Compose Multiplatform на Linux не собирался (06 §5); эмулятор Android / Gradle Managed Devices не
  поднимались (мало RAM); воспроизводимость Roborazzi-эталонов между машинами (S-6).
- Кодоген моделей `kotlin multiplatform` проверен на JVM, но не скомпилирован в iOS-klib (S-4);
  BCV klib-дамп на Linux (S-2); Konsist по KMP-source-set'ам (S-7).
- RuStore publish API и Google Play Developer API — по документации, без аккаунтов (S-12);
  Gitea Release API с 8–10 ассетами через Traefik (S-11).
- Статус Android developer verification для РФ (05 N-1) — прямого подтверждения на домене Google
  нет; закон о RuStore на iPhone и позиция Apple — по прессе; доступность FCM/GA-доменов измерена
  с одной RU-машины.
- Доли Android-версий по РФ не получены (01); hh.ru-цифры — одна точка замера 2026-09-17 (03).
- Все версии — срез 2026-09-17: Xcode 27 / Swift 6.4 вышли за три дня до ресёрча, Kotlin 2.4.20 —
  за десять; политика «latest stable с пином и двухнедельной выдержкой» — вопрос № 10.
