package tech.stit.mobile.core.lib

import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path

// Снимок ответа для офлайн-чтения вместо БД (08 §2.4): Room/SQLDelight тянут cinterop и ломают Linux-сборку klib.
internal class FileCache<T>(
    private val path: Path,
    private val serializer: KSerializer<T>,
    private val json: Json,
    private val fileSystem: FileSystem,
    private val dispatchers: DispatcherProvider,
) {
    suspend fun read(): T? = withContext(dispatchers.io) {
        if (!fileSystem.exists(path)) return@withContext null
        // Битый файл — не повод падать при старте: снимок просто считается отсутствующим.
        runCatching { json.decodeFromString(serializer, fileSystem.read(path) { readUtf8() }) }.getOrNull()
    }

    suspend fun write(value: T) = withContext(dispatchers.io) {
        path.parent?.let(fileSystem::createDirectories)
        fileSystem.write(path) { writeUtf8(json.encodeToString(serializer, value)) }
    }

    suspend fun clear() = withContext(dispatchers.io) {
        fileSystem.delete(path, mustExist = false)
    }
}
