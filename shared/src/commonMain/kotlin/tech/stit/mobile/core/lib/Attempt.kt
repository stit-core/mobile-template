package tech.stit.mobile.core.lib

import kotlinx.coroutines.CancellationException

// runCatching без проглоченной отмены: CancellationException обязана пройти сквозь презентер.
@Suppress("TooGenericExceptionCaught")
internal inline fun <T> attempt(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Result.failure(e)
}
