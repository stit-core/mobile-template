package tech.stit.mobile.core.background

// Планировщик ОС за интерфейсом: shared описывает задачу, платформа выбирает механизм по timing и conditions.
interface BackgroundScheduler {
    fun schedule(task: BackgroundTask)

    fun cancel(id: String)
}

// Исполняемая часть задачи; платформа поднимает её по id через SharedContainer.backgroundRunner.
interface BackgroundRunner {
    suspend fun run(): BackgroundResult
}
