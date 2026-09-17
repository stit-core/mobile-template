package tech.stit.mobile.feature

import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import tech.stit.mobile.core.mock.MockScenario
import tech.stit.mobile.core.requirements.AppRequirements
import tech.stit.mobile.core.requirements.RequirementsStatus
import tech.stit.mobile.feature.requirements.RequirementsIntent
import tech.stit.mobile.feature.requirements.RequirementsState
import tech.stit.mobile.testing.testContainer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RequirementsPresenterTest {
    @Test
    fun startChecksBackendAndExposesLinks() = runTest {
        val presenter = testContainer().requirementsPresenter()
        assertFalse(presenter.state.value.checked)
        presenter.dispatch(RequirementsIntent.Start)
        advanceUntilIdle()
        val state = presenter.state.value
        assertTrue(state.checked)
        assertEquals(RequirementsStatus.Ok, state.status)
        assertEquals("https://app.example.com/privacy", state.requirements.links.privacyPolicy)
        assertFalse(state.blocking)
        presenter.close()
    }

    @Test
    fun backendFailureDoesNotBlockTheApp() = runTest {
        val presenter = testContainer(MockScenario.Error).requirementsPresenter()
        presenter.dispatch(RequirementsIntent.Retry)
        advanceUntilIdle()
        assertTrue(presenter.state.value.checked)
        assertEquals(AppRequirements.Default, presenter.state.value.requirements)
        presenter.close()
    }

    @Test
    fun derivedFlagsFollowStatus() {
        val required =
            RequirementsState(checked = true, requirements = AppRequirements.Default.copy(status = RequirementsStatus.UpdateRequired))
        val recommended =
            RequirementsState(checked = true, requirements = AppRequirements.Default.copy(status = RequirementsStatus.UpdateRecommended))
        assertTrue(required.blocking)
        assertTrue(RequirementsState(requirements = AppRequirements.Default.copy(status = RequirementsStatus.Maintenance)).blocking)
        assertTrue(recommended.showRecommendation)
        assertFalse(recommended.copy(recommendationDismissed = true).showRecommendation)
    }

    @Test
    fun dismissingRecommendationIsRemembered() = runTest {
        val presenter = testContainer().requirementsPresenter()
        presenter.dispatch(RequirementsIntent.DismissRecommendation)
        assertTrue(presenter.state.value.recommendationDismissed)
        presenter.dispatch(RequirementsIntent.Foreground)
        advanceUntilIdle()
        assertTrue(presenter.state.value.checked)
        presenter.close()
    }
}
