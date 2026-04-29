package ai.amani.sdk.presentation.selfie

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import datamanager.model.config.Version
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation-level tests for [SelfieCaptureViewModel].
 *
 * Why instrumented: although the ViewModel itself is plain Kotlin (StateFlow, no LiveData),
 * the [Version] data class is `@Parcelize` and parts of MockK behave differently on the
 * Android runtime. This suite re-verifies the few behaviours that are most likely to
 * regress on a real device:
 *
 *  - The selfieType=-2 (PoseEstimationV2) path SKIPS the intro animations and
 *    publishes [SelfieCaptureUIState.Navigate] immediately.
 *  - The legacy PoseEstimation flow still gates navigation behind the two-step
 *    animation sequence.
 */
@RunWith(AndroidJUnit4::class)
class SelfieCaptureViewModelInstrumentedTest {

    private lateinit var viewModel: SelfieCaptureViewModel

    @Before
    fun setUp() {
        runOnMain { viewModel = SelfieCaptureViewModel() }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // =========================================================================
    // Critical: v2 must skip intro animations
    // =========================================================================

    @Test
    fun selfieTypeMinusTwo_skipsAnimations_andEmitsNavigateImmediately() {
        runOnMain { viewModel.initialViewState(version(selfieType = -2)) }

        assertEquals(SelfieType.PoseEstimationV2, viewModel.currentSelfieType())
        assertEquals(
            SelfieCaptureUIState.Navigate(SelfieType.PoseEstimationV2),
            viewModel.uiState.value
        )
    }

    @Test
    fun selfieTypeMinusTwo_confirmClick_keepsNavigateState() {
        runOnMain {
            viewModel.initialViewState(version(selfieType = -2))
            viewModel.confirmButtonClick()
        }

        assertEquals(
            SelfieCaptureUIState.Navigate(SelfieType.PoseEstimationV2),
            viewModel.uiState.value
        )
    }

    // =========================================================================
    // Legacy v1 PoseEstimation must still gate navigation behind animations
    // =========================================================================

    @Test
    fun selfieTypePositive_runsTwoStepAnimationBeforeNavigate() {
        runOnMain { viewModel.initialViewState(version(selfieType = 3)) }

        assertEquals(SelfieCaptureUIState.FirstAnimation, viewModel.uiState.value)

        runOnMain { viewModel.confirmButtonClick() }
        assertEquals(SelfieCaptureUIState.SecondAnimation, viewModel.uiState.value)

        runOnMain { viewModel.confirmButtonClick() }
        val state = viewModel.uiState.value
        assertTrue(state is SelfieCaptureUIState.Navigate)
        val target = (state as SelfieCaptureUIState.Navigate).navigateTo
        assertTrue(target is SelfieType.PoseEstimation)
        assertEquals(3, (target as SelfieType.PoseEstimation).requestedOrderNumber)
    }

    // =========================================================================
    // Auto / Manual navigate immediately on confirm
    // =========================================================================

    @Test
    fun selfieTypeZero_autoNavigatesImmediatelyOnConfirm() {
        runOnMain {
            viewModel.initialViewState(version(selfieType = 0))
            viewModel.confirmButtonClick()
        }

        assertEquals(
            SelfieCaptureUIState.Navigate(SelfieType.Auto),
            viewModel.uiState.value
        )
    }

    @Test
    fun selfieTypeMinusOne_manualNavigatesImmediatelyOnConfirm() {
        runOnMain {
            viewModel.initialViewState(version(selfieType = -1))
            viewModel.confirmButtonClick()
        }

        assertEquals(
            SelfieCaptureUIState.Navigate(SelfieType.Manual),
            viewModel.uiState.value
        )
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private fun runOnMain(block: () -> Unit) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(block)
    }

    private fun version(selfieType: Int): Version =
        mockk(relaxed = true) {
            every { this@mockk.selfieType } returns selfieType
        }
}
