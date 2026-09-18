package tech.stit.mobile

import io.ktor.client.engine.HttpClientEngine
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.SYSTEM
import tech.stit.mobile.core.api.ApiClient
import tech.stit.mobile.core.api.ApiJson
import tech.stit.mobile.core.api.AppApi
import tech.stit.mobile.core.api.AuthApi
import tech.stit.mobile.core.api.DevicesApi
import tech.stit.mobile.core.api.TodosApi
import tech.stit.mobile.core.api.UsersApi
import tech.stit.mobile.core.api.generated.TodoView
import tech.stit.mobile.core.api.generated.UserView
import tech.stit.mobile.core.api.platformHttpEngine
import tech.stit.mobile.core.auth.SessionStore
import tech.stit.mobile.core.auth.TokenPair
import tech.stit.mobile.core.auth.authPlugin
import tech.stit.mobile.core.background.BackgroundRunner
import tech.stit.mobile.core.background.BackgroundTask
import tech.stit.mobile.core.config.ApiMode
import tech.stit.mobile.core.config.AppConfig
import tech.stit.mobile.core.config.AppEnv
import tech.stit.mobile.core.deeplink.DeepLinkParser
import tech.stit.mobile.core.lib.DefaultDispatchers
import tech.stit.mobile.core.lib.DispatcherProvider
import tech.stit.mobile.core.lib.FileCache
import tech.stit.mobile.core.lib.attempt
import tech.stit.mobile.core.mock.DemoControl
import tech.stit.mobile.core.mock.DemoGate
import tech.stit.mobile.core.mock.DemoMode
import tech.stit.mobile.core.mock.DemoStorage
import tech.stit.mobile.core.mock.MockTransport
import tech.stit.mobile.core.observability.Logger
import tech.stit.mobile.core.push.PushRegistrar
import tech.stit.mobile.core.requirements.AppRequirements
import tech.stit.mobile.core.requirements.RequirementsClient
import tech.stit.mobile.entity.device.DeviceRepository
import tech.stit.mobile.entity.session.SessionRepository
import tech.stit.mobile.entity.todo.TodoRepository
import tech.stit.mobile.entity.user.UserRepository
import tech.stit.mobile.feature.auth.DeleteAccountPresenter
import tech.stit.mobile.feature.auth.LoginPresenter
import tech.stit.mobile.feature.auth.SessionsPresenter
import tech.stit.mobile.feature.demo.DemoPanelPresenter
import tech.stit.mobile.feature.profile.ProfilePresenter
import tech.stit.mobile.feature.push.PushOptInPresenter
import tech.stit.mobile.feature.requirements.RequirementsPresenter
import tech.stit.mobile.feature.settings.SettingsPresenter
import tech.stit.mobile.feature.shell.ShellPresenter
import tech.stit.mobile.feature.todos.AddTodoAction
import tech.stit.mobile.feature.todos.CompleteTodoAction
import tech.stit.mobile.feature.todos.RefreshTodosTask
import tech.stit.mobile.feature.todos.TodayTodosWidgetProvider
import tech.stit.mobile.feature.todos.TodayTodosWidgetSnapshot
import tech.stit.mobile.feature.todos.TodoDetailPresenter
import tech.stit.mobile.feature.todos.TodoListPresenter
import tech.stit.mobile.feature.todos.TodoReminderTask
import kotlin.time.Clock
import kotlin.time.Instant

// Composition root (08 §2.4): одна точка входа для Hilt SharedModule и Swift AppEnvironment, без DI-фреймворка.
class SharedContainer internal constructor(
    private val platform: PlatformDeps,
    val config: AppConfig,
    private val dispatchers: DispatcherProvider,
    private val clock: Clock,
    private val fileSystem: FileSystem,
    httpEngine: () -> HttpClientEngine = ::platformHttpEngine,
) {
    constructor(platform: PlatformDeps, config: AppConfig) : this(platform, config, DefaultDispatchers, Clock.System, FileSystem.SYSTEM)

    private val json = ApiJson
    private val cacheDir: Path = platform.filesDir.toPath() / "cache"

    // Demo-сессия в обычной сборке живёт в своих хранилищах: реальные токены и настройки она не читает и не переписывает.
    private val storage = DemoStorage(platform.secureStore, platform.settings)

    // API_MODE=mock: движка платформы не существует, все запросы идут в MockTransport — demo не ходит в сеть.
    val mockTransport: MockTransport? = if (config.apiMode == ApiMode.Mock) newMockTransport() else null
    private val realEngine: HttpClientEngine by lazy { httpEngine() }

    val sessionStore: SessionStore = SessionStore(
        secureStore = storage.secureStore,
        refresher = { token -> authApi.refresh(token)?.let { TokenPair(it.accessToken, it.refreshToken, it.expiresIn) } },
        mode = config.authMode,
        clock = clock,
        logger = logger("SessionStore"),
        onSignedOut = {
            userRepository.clear()
            todoRepository.clear()
        },
    )

    internal val apiClient: ApiClient by lazy {
        ApiClient(
            config,
            platform.appInfo,
            platform.locale,
            mockTransport?.engine ?: realEngine,
            authPlugin(sessionStore),
            platform.telemetry,
            logger("ApiClient"),
            json,
        )
    }
    internal val authApi: AuthApi by lazy { AuthApi(apiClient) }

    val demoMode: DemoMode? by lazy { mockTransport?.let { DemoMode(it, storage.settings, storage.secureStore, sessionStore) } }

    // Обычная сборка: demo включается в рантайме по флагу requirements.demo_login; в demo-сборке переключать нечего.
    internal val demoGate: DemoGate? by lazy {
        if (config.apiMode == ApiMode.Mock) {
            null
        } else {
            DemoGate(
                requirements = requirements,
                sessionStore = sessionStore,
                storage = storage,
                apiClient = apiClient,
                newTransport = ::newMockTransport,
                realEngine = { realEngine },
                // Снимок виджета живёт вне процесса: после смены режима он перевыпускается, иначе на домашнем
                // экране осталась бы выдача прежнего режима.
                afterSwitch = { attempt { todayTodosWidgetProvider().publish() } },
                logger = logger("Demo"),
            )
        }
    }
    val deepLinkParser: DeepLinkParser = DeepLinkParser(config.deepLinkHost, config.deepLinkScheme)

    val requirements: RequirementsClient by lazy {
        RequirementsClient(
            AppApi(apiClient),
            cache("requirements", AppRequirements.serializer()),
            platform.appInfo,
            clock,
            logger("Requirements"),
        )
    }
    val pushRegistrar: PushRegistrar by lazy {
        PushRegistrar(
            api = DevicesApi(apiClient),
            transports = platform.pushTransports,
            permission = platform.notificationPermission,
            appInfo = platform.appInfo,
            locale = platform.locale,
            settings = storage.settings,
            // APNs sandbox / dev-проект FCM — для всех сборок, кроме prod.
            sandbox = config.env != AppEnv.Prod,
            clock = clock,
            logger = logger("Push"),
        )
    }
    val userRepository: UserRepository by lazy {
        UserRepository(UsersApi(apiClient), authApi, cache("user", UserView.serializer()), platform.telemetry)
    }
    val todoRepository: TodoRepository by lazy {
        TodoRepository(TodosApi(apiClient), cache("todos", ListSerializer(TodoView.serializer())))
    }
    val sessionRepository: SessionRepository by lazy {
        SessionRepository(authApi, sessionStore, pushRegistrar, config.authMode, logger("Session"))
    }
    val deviceRepository: DeviceRepository by lazy { DeviceRepository(DevicesApi(apiClient)) }

    // Задачи, которые платформа подтверждает при старте; напоминание планируется в момент, выбранный пользователем.
    val backgroundTasks: List<BackgroundTask> = listOf(RefreshTodosTask.TASK)

    fun backgroundRunner(id: String): BackgroundRunner? = when {
        id == RefreshTodosTask.ID -> RefreshTodosTask(todoRepository, platform.connectivity)

        id.startsWith(TodoReminderTask.ID_PREFIX) ->
            TodoReminderTask(id.removePrefix(TodoReminderTask.ID_PREFIX), todoRepository, config.deepLinkHost)

        else -> null
    }

    fun todoReminderTask(todoId: String, at: Instant): BackgroundTask = TodoReminderTask.task(todoId, at)

    // Действия вне экранов: та же фича, что у презентеров, но без StateFlow — систему интересует один вызов и ответ.
    fun completeTodoAction(): CompleteTodoAction =
        CompleteTodoAction(todoRepository, sessionStore, platform.connectivity, todayTodosWidgetProvider())

    fun addTodoAction(): AddTodoAction = AddTodoAction(todoRepository, sessionStore, platform.connectivity, todayTodosWidgetProvider())

    fun todayTodosWidgetProvider(): TodayTodosWidgetProvider = TodayTodosWidgetProvider(
        todoRepository,
        sessionStore,
        platform.connectivity,
        cache("widget-${TodayTodosWidgetProvider.SLUG}", TodayTodosWidgetSnapshot.serializer()),
        config.deepLinkHost,
        clock,
    )

    fun shellPresenter(): ShellPresenter = ShellPresenter(
        DemoControl(demoMode, demoGate),
        sessionStore,
        userRepository,
        todoRepository,
        pushRegistrar,
        deepLinkParser,
        platform.connectivity,
        logger("Shell"),
        dispatchers,
    )

    fun requirementsPresenter(): RequirementsPresenter = RequirementsPresenter(requirements, dispatchers)

    fun loginPresenter(): LoginPresenter = LoginPresenter(sessionRepository, demoGate, dispatchers)

    fun sessionsPresenter(): SessionsPresenter = SessionsPresenter(sessionRepository, deviceRepository, dispatchers)

    fun deleteAccountPresenter(): DeleteAccountPresenter = DeleteAccountPresenter(userRepository, sessionRepository, dispatchers)

    fun profilePresenter(): ProfilePresenter = ProfilePresenter(userRepository, sessionRepository, dispatchers)

    fun todoListPresenter(): TodoListPresenter = TodoListPresenter(todoRepository, platform.connectivity, dispatchers)

    fun todoDetailPresenter(id: String): TodoDetailPresenter = TodoDetailPresenter(id, todoRepository, platform.connectivity, dispatchers)

    fun settingsPresenter(): SettingsPresenter =
        SettingsPresenter(storage.settings, pushRegistrar, requirements, platform.appInfo, config, dispatchers)

    fun pushOptInPresenter(): PushOptInPresenter = PushOptInPresenter(pushRegistrar, dispatchers)

    fun demoPanelPresenter(): DemoPanelPresenter = DemoPanelPresenter(demoMode, dispatchers)

    private fun newMockTransport(): MockTransport = MockTransport(json, clock, dispatchers.io)

    private fun logger(tag: String): Logger = Logger(tag, platform.logSink)

    private fun <T> cache(name: String, serializer: KSerializer<T>): FileCache<T> =
        FileCache(cacheDir / "$name.json", serializer, json, fileSystem, dispatchers)
}
