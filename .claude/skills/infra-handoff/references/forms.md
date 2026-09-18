# infra-handoff › формы блоков

Три адресата — одна структура `WHAT / WHY / ACTION / WHO / DECIDE / RISK`. Блок печатается в чат
и копируется в `**Infra impact.**` активного плана.

## INFRA-HANDOFF → `<org>/infra` (I-*)

```
=== INFRA HANDOFF · передай инфра-админу (<org>/infra) ===
WHAT   сервис wellknown: env MOBILE_ANDROID_APP_ID=<applicationId>, MOBILE_ANDROID_SHA256_UPLOAD=<sha256>,
       MOBILE_ANDROID_SHA256_PLAY=<sha256 из Play Console>, MOBILE_ANDROID_SHA256_RUSTORE=<sha256>,
       MOBILE_IOS_TEAM_ID=<team>, MOBILE_IOS_BUNDLE_ID=<bundle>; per env (dev/staging/prod — свои id)
WHY    App Links / Universal Links для хоста app.<domain> (deep-links.md, план <slug> A3)
ACTION заполнить env на КАЖДОМ VPS-среде, make ship; проверить:
       curl -sI https://app.<domain>/.well-known/apple-app-site-association → 200, application/json, без 301/302;
       curl -s https://app.<domain>/.well-known/assetlinks.json | grep <sha256>
WHO    <org>/infra
DECIDE хосты со ссылками: только <domain> или ещё app.<domain>? (AASA нужен на каждом сабдомене)
RISK   ссылки открываются в браузере; Apple CDN кэширует до 24 ч, Android ре-верифицирует до 7 дней
===
```

Типовые I-*: `wellknown` env (I-1), `SENTRY_DSN` / GlitchTip-проект per env (I-5), URL политики
ПДн и веб-удаления аккаунта (I-6), домены `API_BASE_URL` per env (I-3).

## BACKEND-HANDOFF → `<org>/backend` (B-*)

```
=== BACKEND HANDOFF · передай владельцу бэкенда (<org>/backend) ===
WHAT   POST /api/auth/refresh {refresh_token} → TokenPair {access_token, refresh_token, token_type, expires_in};
       X-Client: android|ios на /api/auth/login → токены в теле без Set-Cookie (B-1…B-3)
WHY    мобильный канон — Bearer + refresh в JSON (docs/stack/auth.md#mode = bearer); сейчас приложение
       работает в AuthMode.Cookie («оставаться в системе» не гарантируется)
ACTION добавить операции в api/openapi.yaml бэкенда (make openapi), реализовать в auth-домене,
       JWT_REFRESH_TTL_MOBILE=2160h, ротация + reuse-detect; после мержа — make gen здесь
WHO    <org>/backend
DECIDE TTL access (15m) и refresh (90d) — ок? сессии /api/auth/sessions — в этом же PR?
RISK   без B-1…B-3 stable у auth.md невозможен; пользователи логинятся каждые 30 дней
===
```

Типовые B-*: `POST /api/auth/refresh` + `X-Client` (B-1…B-3), `GET/DELETE /api/auth/sessions`
(B-4…B-6), `Idempotency-Key`-store (B-9), `make openapi` + закоммиченная спека (B-12), дефекты
спеки `$schema` / `data: [array, null]` / `ErrorModel.code` (B-13…B-15), `GET /api/app/requirements`
(B-17), `PUT /api/v1/devices` + `pushkit` (B-18), `UserView.version` в ответах `/api/users/*` — версия для
`UserUpdateInputBody.version` (B-20; до неё клиент подставляет `1`), `min_version` после раскатки версии.

## STRATEGY-HANDOFF → `<org>/strategy` (S-*)

```
=== STRATEGY HANDOFF · передай владельцу карты (<org>/strategy) ===
WHAT   экран OrderDetail (route-key OrderDetail) реализован, записи в mobile/<surface>/screens.md нет;
       entry-point deep-link /orders/{id} — тоже
WHY    код разошёлся с картой (docs/reference/strategy-mapping.md); /scaffold работает только по stable
ACTION в <org>/strategy: /mobile-deep-dive <slug> <surface> screen:OrderDetail — завести и довести до stable;
       entry-points.md — строка deep-link /orders/{id} → OrderDetail
WHO    <org>/strategy
DECIDE состояния экрана: loading/empty/error/offline — все нужны? offline: read-only или blocked?
RISK   следующий /scaffold или /de-template не увидит экран; аудит карты покажет расхождение
===
```

Типовые S-*: канон слоя `mobile/` отсутствует в форке strategy (S-1 — путь без PR:
`docs/reference/strategy-mobile-canon/`), запись без источника, расхождение `status`,
реализованные вне экранов возможности без записи — голосовое действие или кнопка уведомления
(`system-actions.md`), виджет (`widgets.md`), фоновая работа на устройстве
(`background-tasks.md`).

## Куда попадает

- В чат — с фразой «передай <адресату> out-of-band».
- В `.claude/plans/roadmap.md` → активная секция → `**Infra impact.**` (одна строка на блок:
  `I-1 wellknown env для app.<domain> — блок в чате <дата>`); `/release` переносит в
  STORE-HANDOFF (`BACKEND`, `ENV`).
