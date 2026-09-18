import AppIntents
import SystemActions

// Фразы канала voice: провайдер один на приложение и живёт в app-таргете — систему интересует ровно одна его копия.
// Фраза обязана содержать имя приложения, иначе ассистент не поймёт, к кому обращаются.
struct StitAppShortcuts: AppShortcutsProvider {
  static var appShortcuts: [AppShortcut] {
    AppShortcut(
      intent: AddTodoIntent(),
      phrases: ["Добавь задачу в \(.applicationName)"],
      shortTitle: "action.add-todo.short_title",
      systemImageName: "plus.circle"
    )
  }
}
