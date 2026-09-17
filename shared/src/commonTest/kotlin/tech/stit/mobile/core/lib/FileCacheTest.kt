package tech.stit.mobile.core.lib

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import tech.stit.mobile.testing.TestDispatchers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FileCacheTest {
    @Serializable
    data class Snapshot(val items: List<String>)

    private val fileSystem = FakeFileSystem()
    private val path = "/data/app/cache/snapshot.json".toPath()

    private fun cache(scheduler: kotlinx.coroutines.test.TestCoroutineScheduler) =
        FileCache(path, Snapshot.serializer(), Json, fileSystem, TestDispatchers(StandardTestDispatcher(scheduler)))

    @Test
    fun writesReadsAndClears() = runTest {
        val cache = cache(testScheduler)
        assertNull(cache.read())
        cache.write(Snapshot(listOf("a", "b")))
        assertEquals(Snapshot(listOf("a", "b")), cache.read())
        cache.clear()
        assertNull(cache.read())
    }

    @Test
    fun corruptFileReadsAsMissing() = runTest {
        fileSystem.createDirectories(path.parent!!)
        fileSystem.write(path) { writeUtf8("{oops") }
        assertNull(cache(testScheduler).read())
    }
}
