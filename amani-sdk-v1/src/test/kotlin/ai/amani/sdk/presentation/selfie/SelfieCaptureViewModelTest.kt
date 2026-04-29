package ai.amani.sdk.presentation.selfie

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
import org.junit.runners.JUnit4

/**
 * Unit tests for [SelfieCaptureViewModel].
 *
 * Covers the most important behaviour of the selfie capture screen:
 *  - Mapping `Version.selfieType` to the correct [SelfieType].
 *  - Initial UI state per selfie type — in particular the v2 (-2) flow
 *    which MUST skip the intro animations and navigate directly.
 *  - confirmButtonClick state machine for each selfie type.
 */
@RunWith(JUnit4::class)
class SelfieCaptureViewModelTest {

    private lateinit var viewModel: SelfieCaptureViewModel

    @Before
    fun setUp() {
        viewModel = SelfieCaptureViewModel()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // =========================================================================
    // initialViewState — selfieType → SelfieType + initial UI state
    // =========================================================================

    @Test
    fun `selfieType 0 maps to Auto and starts on FirstAnimation`() {
        viewModel.initialViewState(version(selfieType = 0))

        assertEquals(SelfieType.Auto, viewModel.currentSelfieType())
        assertEquals(SelfieCaptureUIState.FirstAnimation, viewModel.uiState.value)
    }

    @Test
    fun `selfieType -1 maps to Manual and starts on FirstAnimation`() {
        viewModel.initialViewState(version(selfieType = -1))

        assertEquals(SelfieType.Manual, viewModel.currentSelfieType())
        assertEquals(SelfieCaptureUIState.FirstAnimation, viewModel.uiState.value)
    }

    /**
     * The v2 contract: animations must be skipped — the screen navigates
     * straight into the V2 fragment.
     */
    @Test
    fun `selfieType -2 maps to PoseEstimationV2 and skips animations by emitting Navigate immediately`() {
        viewModel.initialViewState(version(selfieType = -2))

        assertEquals(SelfieType.PoseEstimationV2, viewModel.currentSelfieType())
        assertEquals(
            SelfieCaptureUIState.Navigate(SelfieType.PoseEstimationV2),
            viewModel.uiState.value
        )
    }

    @Test
    fun `positive selfieType maps to PoseEstimation with the requested order and starts on FirstAnimation`() {
        viewModel.initialViewState(version(selfieType = 2))

        val current = viewModel.currentSelfieType()
        assertTrue(current is SelfieType.PoseEstimation)
        assertEquals(2, (current as SelfieType.PoseEstimation).requestedOrderNumber)
        assertEquals(SelfieCaptureUIState.FirstAnimation, viewModel.uiState.value)
    }

    // =========================================================================
    // confirmButtonClick — state machine
    // =========================================================================

    @Test
    fun `confirmButtonClick on Auto navigates immediately to Auto`() {
        viewModel.initialViewState(version(selfieType = 0))

        viewModel.confirmButtonClick()

        assertEquals(
            SelfieCaptureUIState.Navigate(SelfieType.Auto),
            viewModel.uiState.value
        )
    }

    @Test
    fun `confirmButtonClick on Manual navigates immediately to Manual`() {
        viewModel.initialViewState(version(selfieType = -1))

        viewModel.confirmButtonClick()

        assertEquals(
            SelfieCaptureUIState.Navigate(SelfieType.Manual),
            viewModel.uiState.value
        )
    }

    @Test
    fun `confirmButtonClick on PoseEstimation goes FirstAnimation then SecondAnimation then Navigate`() {
        viewModel.initialViewState(version(selfieType = 2))

        // First click: FirstAnimation → SecondAnimation
        viewModel.confirmButtonClick()
        assertEquals(SelfieCaptureUIState.SecondAnimation, viewModel.uiState.value)

        // Second click: SecondAnimation → Navigate(PoseEstimation(2))
        viewModel.confirmButtonClick()
        val state = viewModel.uiState.value
        assertTrue(state is SelfieCaptureUIState.Navigate)
        val target = (state as SelfieCaptureUIState.Navigate).navigateTo
        assertTrue(target is SelfieType.PoseEstimation)
        assertEquals(2, (target as SelfieType.PoseEstimation).requestedOrderNumber)
    }

    /**
     * Even if `confirmButtonClick` is invoked for v2 (e.g. via stale UI), it must
     * stay on the Navigate state — never fall back into the animation flow.
     */
    @Test
    fun `confirmButtonClick on PoseEstimationV2 stays on Navigate and never emits animations`() {
        viewModel.initialViewState(version(selfieType = -2))

        viewModel.confirmButtonClick()

        assertEquals(
            SelfieCaptureUIState.Navigate(SelfieType.PoseEstimationV2),
            viewModel.uiState.value
        )
    }

    // =========================================================================
    // setState
    // =========================================================================

    @Test
    fun `setState propagates the requested state`() {
        viewModel.setState(SelfieCaptureUIState.Empty)

        assertEquals(SelfieCaptureUIState.Empty, viewModel.uiState.value)
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private fun version(selfieType: Int): Version =
        mockk(relaxed = true) {
            every { this@mockk.selfieType } returns selfieType
        }
}
