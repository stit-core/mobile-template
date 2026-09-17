package tech.stit.mobile.feature.requirements

sealed class RequirementsIntent {
    data object Start : RequirementsIntent()

    data object Foreground : RequirementsIntent()

    data object Retry : RequirementsIntent()

    data object DismissRecommendation : RequirementsIntent()
}
