package tech.stit.mobile.core.lib

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

// X-Request-ID, Idempotency-Key, installation id — UUID строкой ≤ 36 символов: длиннее 128 байт сервер молча заменит (07 T-25).
@OptIn(ExperimentalUuidApi::class)
internal fun newUuid(): String = Uuid.random().toString()
