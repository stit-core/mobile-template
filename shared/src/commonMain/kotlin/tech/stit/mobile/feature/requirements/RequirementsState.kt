package tech.stit.mobile.feature.requirements

import tech.stit.mobile.core.requirements.AppRequirements
import tech.stit.mobile.core.requirements.RequirementsStatus

data class RequirementsState(
    val checked: Boolean = false,
    val requirements: AppRequirements = AppRequirements.Default,
    val recommendationDismissed: Boolean = false,
) {
    val status: RequirementsStatus get() = requirements.status

    // Блокирующие оверлеи UpdateRequired/Maintenance; update_recommended показывается один раз за запуск.
    val blocking: Boolean get() = status == RequirementsStatus.UpdateRequired || status == RequirementsStatus.Maintenance
    val showRecommendation: Boolean get() = status == RequirementsStatus.UpdateRecommended && !recommendationDismissed
}
