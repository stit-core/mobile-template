package tech.stit.mobile.core.auth

enum class AuthState {
    // До restore(): экран входа не мигает, пока сессия читается из SecureStore.
    Unknown,
    SignedOut,
    SignedIn,
}
