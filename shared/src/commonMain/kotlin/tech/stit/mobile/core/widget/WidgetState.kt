package tech.stit.mobile.core.widget

// Словарь состояний виджета системы: снимок несёт ровно одно, платформа по нему выбирает, что рисовать.
enum class WidgetState {
    // До первых данных: снимка в общем каталоге ещё нет.
    Placeholder,
    Populated,
    Empty,
    SignedOut,

    // Данные есть, но обновить их нечем: сети нет и фон не отработал.
    Stale,
}
