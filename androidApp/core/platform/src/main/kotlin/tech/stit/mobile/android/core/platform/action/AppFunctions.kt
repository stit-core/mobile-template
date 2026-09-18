package tech.stit.mobile.android.core.platform.action

import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunction
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

// Реестр действий приложения для ассистента и автоматизаций системы: тела функций только зовут обработчик канала.
// Компилятор AppFunctions дописывает сюда сервис SERVICE_NAME и XML-описание XML_FILE — их и объявляет манифест.
@RequiresApi(APP_FUNCTIONS_API)
@AppFunctionServiceEntryPoint(serviceName = SERVICE_NAME, appFunctionXmlFileName = XML_FILE)
abstract class BaseStitAppFunctionService : AppFunctionService() {
    /**
     * Добавляет задачу в список пользователя.
     *
     * @param title Текст задачи словами пользователя.
     * @return Короткое подтверждение, которое ассистент произносит в ответ.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun addTodo(title: String): String {
        val graph = EntryPointAccessors.fromApplication(applicationContext, AppFunctionEntryPoint::class.java)
        return graph.addTodoActionHandler().add(title)
    }
}

// Сервис AppFunctions создаёт система, а не Hilt: граф приложения достаётся тем же entry point'ом, что и в воркере.
@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface AppFunctionEntryPoint {
    fun addTodoActionHandler(): AddTodoActionHandler
}

// AppFunctions — платформенный API Android 16: ниже система этот сервис не поднимает, и объявление остаётся мёртвым.
internal const val APP_FUNCTIONS_API = 36
internal const val SERVICE_NAME = "StitAppFunctionService"
internal const val XML_FILE = "stit_app_functions"
