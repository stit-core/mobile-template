package tech.stit.mobile.feature.push

sealed class PushOptInIntent {
    data object Start : PushOptInIntent()

    data object Accept : PushOptInIntent()

    data object Decline : PushOptInIntent()
}
