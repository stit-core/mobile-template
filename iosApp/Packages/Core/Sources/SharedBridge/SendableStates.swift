import Shared

// Состояния презентеров — immutable data class'ы Kotlin: пересекают акторы только через этот модуль.
extension ShellState: @retroactive @unchecked Sendable {}
extension RequirementsState: @retroactive @unchecked Sendable {}
extension LoginState: @retroactive @unchecked Sendable {}
extension SessionsState: @retroactive @unchecked Sendable {}
extension DeleteAccountState: @retroactive @unchecked Sendable {}
extension ProfileState: @retroactive @unchecked Sendable {}
extension TodoListState: @retroactive @unchecked Sendable {}
extension TodoDetailState: @retroactive @unchecked Sendable {}
extension SettingsState: @retroactive @unchecked Sendable {}
extension PushOptInState: @retroactive @unchecked Sendable {}
extension DemoPanelState: @retroactive @unchecked Sendable {}

// Kotlin-holder вызывается из очереди NWPathMonitor: объект потокобезопасен (new memory model K/N).
extension ConnectivityState: @retroactive @unchecked Sendable {}

// Носители вне экранов система поднимает своими потоками (BGTaskScheduler, делегат уведомлений, интенты),
// поэтому контейнер, описание задачи и её результат пересекают акторы; под новой моделью памяти K/N это безопасно.
extension SharedContainer: @retroactive @unchecked Sendable {}
extension BackgroundTask: @retroactive @unchecked Sendable {}
extension BackgroundResult: @retroactive @unchecked Sendable {}
extension CompleteTodoResult: @retroactive @unchecked Sendable {}
extension AddTodoResult: @retroactive @unchecked Sendable {}
