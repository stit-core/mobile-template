package tech.stit.mobile.core.api

import io.ktor.client.request.parameter
import io.ktor.client.request.setBody
import tech.stit.mobile.core.api.generated.TodoCreateInputBody
import tech.stit.mobile.core.api.generated.TodoListOutputBody
import tech.stit.mobile.core.api.generated.TodoOutputBody
import tech.stit.mobile.core.api.generated.TodoUpdateInputBody

internal class TodosApi(private val client: ApiClient) {
    suspend fun list(page: Int = 1, size: Int = PAGE_SIZE): TodoListOutputBody = client.get("/api/todos") {
        parameter("page", page)
        parameter("size", size)
    }

    suspend fun get(id: String): TodoOutputBody = client.get("/api/todos/$id")

    suspend fun create(title: String): TodoOutputBody = client.post("/api/todos") {
        setBody(TodoCreateInputBody(title = title))
    }

    suspend fun update(id: String, version: Long, title: String? = null, done: Boolean? = null): TodoOutputBody =
        client.patch("/api/todos/$id") {
            setBody(TodoUpdateInputBody(version = version, title = title, done = done))
        }

    suspend fun delete(id: String) = client.delete("/api/todos/$id")

    private companion object {
        // Максимум по спеке (size ≤ 100): reference-список не пагинируется на экране.
        const val PAGE_SIZE = 100
    }
}
