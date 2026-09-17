---
id: stack.distribution
kind: distribution
name: distribution
status: draft
---

# distribution

## Channels

Набор по `service.md#preset`; каналы только для платформ из `service.md#platforms`.

| платформа | канал | роль | артефакт | preset |
|---|---|---|---|---|
| Android | RuStore | основной (RU-first), публикация по API | AAB (или APK) | companion, consumer |
| Android | Google Play | вторичный, free-only, трек `internal` → production со staged rollout | AAB | companion, consumer |
| iOS | App Store / TestFlight | единственный продуктовый канал | IPA | companion, consumer |
| обе | Gitea Release (`gitea`) | внутренние сборки / заказчик; APK как ассет | AAB, APK, IPA | все (STOP-1); канал — enterprise |
| iOS | Apple Business Manager (`abm`) | custom apps вне магазина | IPA через TestFlight-путь | enterprise |

`channels: [rustore, play, testflight]`

## STOP-1 / STOP-2

STOP-1 — Gitea Release на аннотированном теге `vX.Y.Z` с ассетами (AAB + APK + `mapping.txt`,
IPA + dSYM.zip, `checksums.sha256`, release-notes ru/en, `openapi.lock`, `store-handoff.md`),
символы загружены. STOP-2 — Play internal / RuStore модерация / TestFlight. Дальше — человек по
STORE-HANDOFF ([`docs/reference/release.md`](../../reference/release.md)).

## Accounts

| магазин | аккаунт | владелец | статус (D-U-N-S, verification) |
|---|---|---|---|
| RuStore | _VK ID; ИП/ЮЛ_ | _заказчик_ | _…_ |
| Google Play | _organization + D-U-N-S_ | _заказчик; stit — admin_ | _developer verification …_ |
| App Store | _Apple Developer Program_ | _заказчик_ | _оплата из РФ — партнёр/юрлицо_ |

## Moderation

RuStore — 1–5 рабочих дней (практика 2–3), черновик живёт 14 дней; Play — часы–дни, новый
personal-аккаунт: 12 тестеров × 14 дней; App Store — 24–48 ч, TestFlight external — Beta App
Review, сборка живёт 90 дней. Отклонение → тег `Z+1`.

## Release notes

`release-notes.ru.md` + `release-notes.en.md` — черновик из `git log` от предыдущего тега и
плана, правит человек; одинаковый текст во всех магазинах.

## App Review notes

Demo-доступ: flavor/схема `demo` или demo-аккаунт (`entry-points.md` `demo`, живёт дольше
ревью); описание: _что показывает приложение, как войти, что не требует бэкенда_.

## Deadlines

| дата | требование | статус |
|---|---|---|
| 2026-08-31 | Play: `targetSdk ≥ 36` для новых приложений и обновлений | `targetSdk 37` |
| 2027-02 | Play: 16 KB page size (весь граф зависимостей), R8 DEX-оптимизация ≥ 25 % при DEX > 10 МБ | _проверить зависимости_ |
| 2027-04 | App Store Connect: iOS 27 SDK; Play: Restore Credentials | Xcode 27 |
| 2027 | Android developer verification — глобальный этап (sideload) | _аккаунт_ |

## Generates

- `CHANNELS` для `/release` (STOP-2), `dist/`-структура, STORE-HANDOFF-шаблон
- `scripts/release/build-android.sh` (AAB + APK + mapping + checksums)
- App Review notes — блок `TESTFLIGHT` STORE-HANDOFF; дедлайны — `release-preflight`

## Открытые вопросы

- _что_
