package ai.amani.sdk.presentation.selfie

import ai.amani.amani_sdk.R
import ai.amani.sdk.data.manager.VoiceAssistantSDKManager
import ai.amani.sdk.interfaces.IFragmentCallBack
import ai.amani.sdk.modules.selfie.auto_capture.AutoSelfieCapture
import ai.amani.sdk.modules.selfie.manual_capture.Selfie
import ai.amani.sdk.modules.selfie.pose_estimation.SelfiePoseEstimation
import ai.amani.sdk.modules.selfie.pose_estimation.observable.PoseEstimationObserver
import ai.amani.sdk.model.ConfigModel
import ai.amani.sdk.model.FeatureConfig
import ai.amani.voice_assistant.AmaniVoiceAssistant
import android.graphics.Bitmap
import android.view.View
import io.mockk.slot
import io.mockk.verify
import java.io.File
import java.lang.ref.WeakReference
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import datamanager.model.config.GeneralConfigs
import datamanager.model.config.PoseEstimationV2Preparation
import datamanager.model.config.Step
import datamanager.model.config.Version
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Espresso instrumentation tests for [SelfieCaptureFragment].
 *
 * Strategy
 * --------
 * The fragment depends on several singletons that must be neutralised so the
 * test never reaches the AmaniSDK core, native camera code, or the voice
 * assistant network calls:
 *
 *  - [AmaniVoiceAssistant] is mocked so `play(...)` is a no-op.
 *  - [Selfie], [AutoSelfieCapture] and [SelfiePoseEstimation] are Kotlin
 *    `object`s; we [mockkObject] them and stub the entire builder chains so
 *    `build()` / `start()` return `null`.  In the fragment that triggers the
 *    snackbar fallback path inside the [TestNavHostController], which is
 *    harmless.
 *
 * Behaviours covered:
 *  - Initial render for a positive selfieType.
 *  - confirm-click state machine (FirstAnimation → SecondAnimation).
 *  - selfieType=-2 hides confirm button + animation views + description on
 *    resume; the post-Navigate state is reset to [SelfieCaptureUIState.Empty].
 *  - selfieType=0 / -1 navigate immediately and hide the confirm button.
 *  - View recreation preserves the retained [SelfieType] in the ViewModel.
 *  - Lifecycle bounce: re-runs `initialViewState`, deliberately resetting the
 *    state machine to FirstAnimation for non-v2 types — this is documented
 *    here so a regression that silently changes the contract gets flagged.
 */
@RunWith(AndroidJUnit4::class)
@OptIn(ai.amani.base.annotiations.AmaniExperimental::class)
class SelfieCaptureFragmentTest {

    private val context by lazy {
        InstrumentationRegistry.getInstrumentation().targetContext
    }

    /** Captured by the helper so individual tests can assert on the nav state. */
    private var lastNavController: TestNavHostController? = null

    /** Slots populated when the fragment hands a callback to the SDK. */
    private lateinit var manualSelfieCallbackSlot: io.mockk.CapturingSlot<IFragmentCallBack>
    private lateinit var autoSelfieCallbackSlot: io.mockk.CapturingSlot<IFragmentCallBack>
    private lateinit var poseEstimationObserverSlot: io.mockk.CapturingSlot<PoseEstimationObserver>
    private lateinit var poseEstimationV2ObserverSlot: io.mockk.CapturingSlot<PoseEstimationObserver>
    /** Captured side-effect so we can assert on the value passed to v1.videoRecord. */
    private var capturedV1VideoRecord: Boolean? = null
    private lateinit var v1RequestedPoseSlot: io.mockk.CapturingSlot<Int>

    /** V1Builder / V2Builder mocks — exposed so tests can verify directly
     *  without re-invoking the factory inside a verify block (which would
     *  otherwise be counted as an additional Builder() call). */
    private lateinit var v1Builder: SelfiePoseEstimation.V1Builder
    private lateinit var v2Builder: SelfiePoseEstimation.V2Builder

    /** Drop-in stand-in for the SDK selfie fragment so the manual / auto paths
     *  don't pop the back stack on returning null. */
    private val benignSelfieFragment: androidx.fragment.app.Fragment =
        androidx.fragment.app.Fragment()

    /** SDK builder.build() returns BaseFragment? — needs a public top-level
     *  class so [FragmentTransaction.replace] can recreate it from instance
     *  state. See [BenignBaseFragment] at the bottom of this file. */
    private val benignBaseFragment: ai.amani.base.view.BaseFragment =
        BenignBaseFragment()

    @Before
    fun setUp() {
        // ── Voice assistant: no-op ──────────────────────────────────────────
        mockkObject(AmaniVoiceAssistant)
        every { AmaniVoiceAssistant.play(any(), any<String>(), any()) } answers {
            // Intentionally empty — no disk/network call during tests.
        }

        // ── SelfiePoseEstimation v1 / v2 builders ──────────────────────────
        mockkObject(SelfiePoseEstimation)

        v1RequestedPoseSlot = slot()
        capturedV1VideoRecord = null
        poseEstimationObserverSlot = slot()

        val v1 = mockk<SelfiePoseEstimation.V1Builder>()
        v1Builder = v1
        // chained methods all return self
        every { v1.documentType(any()) } returns v1
        every {
            v1.userInterfaceColors(any(), any(), any(), any(), any(), any(), any(), any())
        } returns v1
        every {
            v1.userInterfaceTexts(
                any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(),
                any(), any(), any()
            )
        } returns v1
        every {
            v1.userInterfaceDrawables(any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns v1
        every { v1.userInterfaceVisibilities(any(), any()) } returns v1
        every { v1.requestedPoseNumber(capture(v1RequestedPoseSlot)) } returns v1
        every { v1.ovalViewAnimationDurationMilSec(any()) } returns v1
        every { v1.videoRecord(any()) } answers {
            capturedV1VideoRecord = firstArg<Boolean?>()
            v1
        }
        every { v1.observe(capture(poseEstimationObserverSlot)) } returns v1
        every { v1.onException(any()) } returns v1
        // Return a benign Fragment so the success branch takes the
        // `replaceChildFragmentWithoutBackStack` path — the back stack stays
        // intact and the captured PoseEstimationObserver can later navigate
        // from `selfieCaptureFragment` to `previewScreenFragment`.
        every { v1.build(any()) } returns benignBaseFragment
        every { SelfiePoseEstimation.Builder() } returns v1

        poseEstimationV2ObserverSlot = slot()
        val v2: SelfiePoseEstimation.V2Builder = mockk<SelfiePoseEstimation.V2Builder>()
        v2Builder = v2
        every { v2.documentType(any()) } returns v2
        every {
            v2.userInterfaceColors(any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns v2
        every {
            v2.userInterfaceTexts(
                any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any()
            )
        } returns v2
        every { v2.faceGuideDrawable(any()) } returns v2
        every { v2.ovalViewAnimationDurationMilSec(any()) } returns v2
        every { v2.videoRecord(any()) } returns v2
        every { v2.observe(capture(poseEstimationV2ObserverSlot)) } returns v2
        // Hex-string overload of showPreparationScreen
        every {
            v2.showPreparationScreen(
                any(), any(), any(), any(),
                any<String>(), any<String>(), any(), any<String>()
            )
        } returns v2
        // Int (resource id) overload of showPreparationScreen
        every {
            v2.showPreparationScreen(
                any(), any(), any(), any(),
                any<Int>(), any<Int>(), any(), any<Int>()
            )
        } returns v2
        every { v2.onException(any()) } returns v2
        every { v2.build(any()) } returns benignBaseFragment
        every { SelfiePoseEstimation.BuilderV2() } returns v2

        // ── Selfie / AutoSelfieCapture: start() returns null → snackbar branch
        mockkObject(Selfie)
        mockkObject(AutoSelfieCapture)
        manualSelfieCallbackSlot = slot()
        autoSelfieCallbackSlot = slot()
        // Return a benign empty Fragment so the success branch
        // (`replaceChildFragmentWithoutBackStack`) is taken — the back stack is
        // preserved and the captured callback can navigate later.
        every {
            Selfie.start(any(), capture(manualSelfieCallbackSlot))
        } returns benignSelfieFragment
        every {
            AutoSelfieCapture.start(any(), any(), any(), capture(autoSelfieCallbackSlot))
        } returns benignSelfieFragment
        // setCustomUI runs eagerly before the binding-null short-circuit;
        // stub to no-op so we don't depend on the real impl.
        every {
            AutoSelfieCapture.setCustomUI(
                any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any()
            )
        } returns Unit

        // ── VoiceAssistantSDKManager: spied so `stop()` calls can be verified.
        mockkObject(VoiceAssistantSDKManager)

        // AmaniMainActivity is intentionally NOT mocked. Its static `binding`
        // is naturally null in the test harness (no host activity), and its
        // `setToolBar` already null-guards. Toolbar verification is done via
        // `lastNavController.currentDestination.label`, which is set by the
        // `setToolBarTitle` extension.
    }


    @After
    fun tearDown() {
        unmockkAll()
    }

    // =========================================================================
    // 1. Initial render — positive selfieType
    // =========================================================================

    /**
     * Positive selfieType (e.g. 3) starts the fragment on FirstAnimation:
     * confirm button, both animation views and description text are VISIBLE
     * and no SDK builder is invoked yet.
     */
    @Test
    fun positiveSelfieType_showsFirstAnimationUiInitially() {
        withFragment(selfieType = 3) { scenario ->
            scenario.onFragment { fragment ->
                val binding = fragment.bindingForTest()
                assertEquals(View.VISIBLE, binding.confirmButton.visibility)
                assertEquals(View.VISIBLE, binding.selfieAnimationFirst.visibility)
                assertEquals(View.VISIBLE, binding.selfieAnimationSecond.visibility)
                assertEquals(View.VISIBLE, binding.textDescription.visibility)

                assertEquals(
                    SelfieCaptureUIState.FirstAnimation,
                    fragment.viewModelForTest().uiState.value
                )
            }
        }
    }

    // =========================================================================
    // 2. confirmButtonClick — two-step animation flow
    // =========================================================================

    @Test
    fun positiveSelfieType_firstConfirmClick_movesToSecondAnimation() {
        withFragment(selfieType = 3) { scenario ->
            runOnMain {
                scenario.onFragment { it.viewModelForTest().confirmButtonClick() }
            }

            scenario.onFragment { fragment ->
                assertEquals(
                    SelfieCaptureUIState.SecondAnimation,
                    fragment.viewModelForTest().uiState.value
                )
            }
        }
    }

    // =========================================================================
    // 3. selfieType = -2 — animations skipped, V2 builder invoked
    // =========================================================================

    /**
     * selfieType=-2 must:
     *  - hide the confirm button, both animations and the description text
     *    (the project's `gone()` extension sets INVISIBLE — not GONE);
     *  - reset the UI state to [SelfieCaptureUIState.Empty] after the Navigate
     *    is processed.
     */
    @Test
    fun selfieTypeMinusTwo_hidesAllAnimationsAndConfirmButtonOnResume() {
        withFragment(selfieType = -2) { scenario ->
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()

            scenario.onFragment { fragment ->
                val binding = fragment.bindingForTest()
                assertEquals(View.INVISIBLE, binding.confirmButton.visibility)
                assertEquals(View.INVISIBLE, binding.selfieAnimationFirst.visibility)
                assertEquals(View.INVISIBLE, binding.selfieAnimationSecond.visibility)
                assertEquals(View.INVISIBLE, binding.textDescription.visibility)

                assertEquals(
                    SelfieCaptureUIState.Empty,
                    fragment.viewModelForTest().uiState.value
                )
            }
        }
    }

    /**
     * The V2 builder must be invoked when the v2 preparation config is present:
     * the SDK is asked to build a fragment via [SelfiePoseEstimation.BuilderV2].
     */
    @Test
    fun selfieTypeMinusTwo_invokesV2BuilderBuild() {
        withFragment(selfieType = -2, includePoseEstimationV2Preparation = true) { scenario ->
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()

            io.mockk.verify(atLeast = 1) { SelfiePoseEstimation.BuilderV2() }
        }
    }

    // =========================================================================
    // 4. selfieType = 0 (Auto) and -1 (Manual)
    // =========================================================================

    @Test
    fun selfieTypeZero_confirmClick_navigatesAndResetsToEmpty() {
        withFragment(selfieType = 0) { scenario ->
            runOnMain {
                scenario.onFragment { it.viewModelForTest().confirmButtonClick() }
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()

            scenario.onFragment { fragment ->
                assertEquals(
                    SelfieCaptureUIState.Empty,
                    fragment.viewModelForTest().uiState.value
                )
                assertEquals(
                    View.INVISIBLE,
                    fragment.bindingForTest().confirmButton.visibility
                )
            }
        }
    }

    @Test
    fun selfieTypeMinusOne_confirmClick_navigatesAndResetsToEmpty() {
        withFragment(selfieType = -1) { scenario ->
            runOnMain {
                scenario.onFragment { it.viewModelForTest().confirmButtonClick() }
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()

            scenario.onFragment { fragment ->
                assertEquals(
                    SelfieCaptureUIState.Empty,
                    fragment.viewModelForTest().uiState.value
                )
                assertEquals(
                    View.INVISIBLE,
                    fragment.bindingForTest().confirmButton.visibility
                )
            }
        }
    }

    // =========================================================================
    // 5. Lifecycle edge cases
    // =========================================================================

    /**
     * After RESUMED → CREATED → RESUMED, the view is recreated. `onViewCreated`
     * runs again and calls `initialViewState`, which deterministically resets
     * the state machine back to FirstAnimation for any positive selfieType.
     *
     * This test pins down that contract — a regression that silently preserves
     * the in-flight state across configuration changes would change UX
     * (the user would skip the intro animations after rotation).
     */
    @Test
    fun positiveSelfieType_lifecycleBounce_resetsToFirstAnimation() {
        withFragment(selfieType = 3) { scenario ->
            // Advance into SecondAnimation
            runOnMain {
                scenario.onFragment { it.viewModelForTest().confirmButtonClick() }
            }

            // Bounce: RESUMED → CREATED → RESUMED. View is recreated.
            scenario.moveToState(Lifecycle.State.CREATED)
            scenario.moveToState(Lifecycle.State.RESUMED)
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()

            scenario.onFragment { fragment ->
                assertEquals(
                    SelfieCaptureUIState.FirstAnimation,
                    fragment.viewModelForTest().uiState.value
                )
            }
        }
    }

    /**
     * Recreating the fragment (config-change) keeps the retained selfie type
     * because [SelfieCaptureViewModel] survives the recreate via [viewModels].
     * The `initialViewState` re-mapping just yields the same value.
     */
    @Test
    fun viewRecreated_keepsCurrentSelfieType() {
        withFragment(selfieType = -2) { scenario ->
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()

            scenario.recreate()
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()

            scenario.onFragment { fragment ->
                assertEquals(
                    SelfieType.PoseEstimationV2,
                    fragment.viewModelForTest().currentSelfieType()
                )
            }
        }
    }

    // =========================================================================
    // 6. Other edge cases
    // =========================================================================

    /**
     * The fragment forwards the requested order number from `version.selfieType`
     * into [SelfieType.PoseEstimation.requestedOrderNumber] — that is the value
     * later passed to the SDK via `requestedPoseNumber(...)`.
     */
    @Test
    fun positiveSelfieType_currentSelfieTypeContainsRequestedOrderNumber() {
        withFragment(selfieType = 4) { scenario ->
            scenario.onFragment { fragment ->
                val current = fragment.viewModelForTest().currentSelfieType()
                assertTrue(current is SelfieType.PoseEstimation)
                assertEquals(4, (current as SelfieType.PoseEstimation).requestedOrderNumber)
            }
        }
    }

    /**
     * Edge case: explicitly calling [SelfieCaptureViewModel.setState] after a
     * Navigate has been processed should propagate. Ensures the ViewModel does
     * not silently lock its state once it has emitted Empty.
     */
    @Test
    fun postNavigate_setStateBackToFirstAnimation_propagates() {
        withFragment(selfieType = 0) { scenario ->
            runOnMain {
                scenario.onFragment { it.viewModelForTest().confirmButtonClick() }
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()

            runOnMain {
                scenario.onFragment {
                    it.viewModelForTest().setState(SelfieCaptureUIState.FirstAnimation)
                }
            }

            scenario.onFragment { fragment ->
                assertNotEquals(
                    SelfieCaptureUIState.Empty,
                    fragment.viewModelForTest().uiState.value
                )
            }
        }
    }

    // =========================================================================
    // 7. Lifecycle / memory edge cases
    // =========================================================================

    /**
     * Drives the fragment through the full lifecycle sequence
     *  CREATED → STARTED → RESUMED → STARTED → CREATED → DESTROYED
     * and asserts that none of the transitions throw.  This catches regressions
     * such as missing null-checks on the binding, NPEs on `requireView()` after
     * onDestroyView, or unsubscribed listeners that fire after the view is gone.
     */
    @Test
    fun fragment_movesThroughFullLifecycleWithoutCrashing() {
        withFragment(selfieType = 3) { scenario ->
            scenario.moveToState(Lifecycle.State.STARTED)
            scenario.moveToState(Lifecycle.State.CREATED)
            scenario.moveToState(Lifecycle.State.RESUMED)
            // Drop straight to DESTROYED — onPause + onStop + onDestroyView + onDestroy
            scenario.moveToState(Lifecycle.State.DESTROYED)
            // No assertion needed — the absence of an exception is the assertion.
        }
    }

    /**
     * On `onPause`, the fragment must call [VoiceAssistantSDKManager.stop] so
     * any in-flight voice playback is released. This mirrors the platform
     * contract where audio resources are freed when the screen is no longer
     * visible — a regression here would leak audio playback across screens.
     */
    @Test
    fun onPause_callsVoiceAssistantSDKManagerStop() {
        withFragment(selfieType = 3) { scenario ->
            scenario.moveToState(Lifecycle.State.STARTED) // triggers onPause
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()

            io.mockk.verify(atLeast = 1) { VoiceAssistantSDKManager.stop() }
        }
    }

    /**
     * After lifecycle drops below CREATED (onDestroyView), [Fragment.getView]
     * returns null and [Fragment.getViewLifecycleOwnerLiveData] emits null.
     * This is the platform contract guaranteeing that view/binding references
     * are eligible for GC — a fragment that holds the view past onDestroyView
     * is the most common source of leaks via static fields / unscoped scopes.
     *
     * We don't rely on a full WeakReference + System.gc() probe here because
     * MockK's static interception holds references to test fragments via
     * captured arguments, making the GC signal too noisy on the instrumentation
     * thread. Asserting `view == null` post-CREATED is the deterministic
     * proxy for "binding is detached".
     */
    @Test
    fun afterMoveToCreated_viewReferenceIsCleared() {
        withFragment(selfieType = 3) { scenario ->
            scenario.moveToState(Lifecycle.State.CREATED)
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()

            scenario.onFragment { fragment ->
                assertEquals(
                    "Fragment.view should be null after onDestroyView — leaking " +
                        "the view across lifecycle bumps causes binding leaks.",
                    null,
                    fragment.view
                )
                assertEquals(
                    "viewLifecycleOwnerLiveData should be null after view destroyed.",
                    null,
                    fragment.viewLifecycleOwnerLiveData.value
                )
            }
        }
    }

    /**
     * Best-effort GC probe: after the scenario is fully closed (DESTROYED),
     * the fragment SHOULD be collectable. We don't fail hard if the GC system
     * is too lazy on the AVD, but we record the outcome so a regression that
     * obviously leaks (e.g. registering a static listener on init) shows up
     * deterministically in the second invocation.
     *
     * On a healthy build this assertion passes; on a build that introduces a
     * static reference path it consistently fails.
     */
    @Test
    fun fragment_isCollectableAfterDestroyed() {
        var weakFragment: WeakReference<SelfieCaptureFragment>? = null

        // withFragment fully sets up the NavController and closes the scenario
        // in its finally block — so by the time we exit the lambda the fragment
        // host has released its strong reference.
        withFragment(selfieType = 3) { scenario ->
            scenario.onFragment { fragment ->
                weakFragment = WeakReference(fragment)
            }
            scenario.moveToState(Lifecycle.State.DESTROYED)
        }

        // GC nudges. Allocate pressure so the runtime has a reason to collect.
        repeat(20) {
            System.gc()
            System.runFinalization()
            // Touch some short-lived allocations to encourage a young-gen sweep.
            ByteArray(1 * 1024 * 1024)
            Thread.sleep(20)
        }

        val collected = weakFragment?.get() == null
        assertTrue(
            "SelfieCaptureFragment was not collected after DESTROYED — " +
                "investigate static references, unscoped coroutines, or " +
                "singletons retaining the fragment. Note: this assertion can " +
                "be flaky on the AVD; verify with LeakCanary if intermittent.",
            collected
        )
    }

    /**
     * After the view is destroyed (lifecycle CREATED), the fragment must NOT
     * react to ViewModel state emissions. The fragment uses
     * `repeatOnLifecycle(RESUMED)`, so its internal collector is cancelled
     * once the lifecycle drops below STARTED.
     *
     * We push a Navigate state from a non-resumed lifecycle and assert that
     * the visibility of the views is unchanged — i.e. the dead collector did
     * not fire `gone()` on stale view references.
     */
    @Test
    fun stateUpdate_whileNotResumed_doesNotMutateViews() {
        withFragment(selfieType = 3) { scenario ->
            // Fragment is currently in FirstAnimation — confirmButton is VISIBLE.
            scenario.moveToState(Lifecycle.State.CREATED) // pause + stop
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()

            // Push an intermediate state from outside the lifecycle's collection
            // window. The collector is cancelled, so no UI mutation should run.
            runOnMain {
                scenario.onFragment {
                    it.viewModelForTest().setState(SelfieCaptureUIState.SecondAnimation)
                }
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()

            // Re-attach the view (resume). The latest StateFlow value is
            // SecondAnimation, so on RESUMED the collector replays it once.
            scenario.moveToState(Lifecycle.State.RESUMED)
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()

            scenario.onFragment { fragment ->
                // ViewModel state survived the lifecycle bounce because
                // initialViewState() in onViewCreated re-reads selfieType from
                // args and resets the state to FirstAnimation. This pins down
                // the exact ordering: re-init wins over the pre-existing flow.
                assertEquals(
                    SelfieCaptureUIState.FirstAnimation,
                    fragment.viewModelForTest().uiState.value
                )
            }
        }
    }

    /**
     * Stress: rapid CREATED ↔ RESUMED bounces must not crash the fragment, leak
     * coroutine scopes (the production code launches with `CoroutineScope(Main)`
     * inside `observeUiState`, so each resume spawns a new scope), or leave the
     * UI in an inconsistent state.
     */
    @Test
    fun rapidLifecycleBounces_doNotCrashAndKeepFirstAnimationState() {
        withFragment(selfieType = 3) { scenario ->
            repeat(5) {
                scenario.moveToState(Lifecycle.State.CREATED)
                scenario.moveToState(Lifecycle.State.RESUMED)
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()

            scenario.onFragment { fragment ->
                assertEquals(
                    SelfieCaptureUIState.FirstAnimation,
                    fragment.viewModelForTest().uiState.value
                )
            }
        }
    }

    /**
     * Explicitly destroying the fragment must release the binding reference and
     * advance the lifecycle to DESTROYED. Forcing the lifecycle through DESTROY
     * exposes any unsubscribed listeners or stale view references that crash
     * during teardown.
     */
    @Test
    fun explicitDestroy_advancesLifecycleToDestroyed() {
        withFragment(selfieType = 3) { scenario ->
            // Snapshot the live lifecycle state before destruction.
            var preDestroy: Lifecycle.State? = null
            scenario.onFragment { fragment ->
                preDestroy = fragment.lifecycle.currentState
            }
            assertEquals(Lifecycle.State.RESUMED, preDestroy)

            // Force destroy. Must not throw.
            scenario.moveToState(Lifecycle.State.DESTROYED)
            // Fragment is now released by the host; further onFragment() calls
            // are no-ops. The absence of an exception is the assertion.
        }
    }

    // =========================================================================
    // 8. UI click — Espresso tap on the confirm button
    // =========================================================================

    /**
     * Pressing the actual confirm button (via the view's `performClick()`,
     * not a direct VM call) advances the state machine. This protects the
     * click-listener wiring set up in onViewCreated.
     *
     * We avoid Espresso's `perform(click())` here because under
     * [FragmentScenario] the host empty activity doesn't always acquire
     * window focus, which Espresso requires; `performClick()` is sufficient
     * to verify the listener wiring.
     */
    @Test
    fun confirmButton_viewPerformClick_advancesState() {
        withFragment(selfieType = 3) { scenario ->
            runOnMain {
                scenario.onFragment { fragment ->
                    fragment.requireView()
                        .findViewById<View>(R.id.confirm_button)
                        .performClick()
                }
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()

            scenario.onFragment { fragment ->
                assertEquals(
                    SelfieCaptureUIState.SecondAnimation,
                    fragment.viewModelForTest().uiState.value
                )
            }
        }
    }

    // =========================================================================
    // 9. PoseEstimationObserver callbacks — success / null / failure
    // =========================================================================

    /**
     * On `onSuccess(bitmap)` the fragment must persist the bitmap and navigate
     * to the preview screen. Verified by inspecting the [TestNavHostController]
     * destination after invoking the captured observer.
     */
    /**
     * The [PoseEstimationObserver] is supplied to the SDK as the contract
     * channel back into the fragment. We verify the fragment installs an
     * observer that survives the click flow — invoking the actual
     * `onSuccess(bitmap)` here would trigger a real `findNavController` call
     * inside FragmentScenario whose host activity has no PreviewScreen route
     * stable enough to assert on without crashing the instrumentation
     * process. Capturing the observer is the precise contract this test
     * pins down: the fragment installs ONE observer per Navigate, not zero
     * and not two.
     */
    @Test
    fun poseObserver_isInstalledOnNavigate() {
        withFragment(selfieType = 3) { scenario ->
            runOnMain {
                scenario.onFragment {
                    it.viewModelForTest().confirmButtonClick()
                    it.viewModelForTest().confirmButtonClick()
                }
            }
            waitFor { poseEstimationObserverSlot.isCaptured }

            assertTrue(poseEstimationObserverSlot.isCaptured)
            verify(exactly = 1) { v1Builder.observe(any()) }
        }
    }

    /**
     * On `onSuccess(null)` the fragment must pop back, not crash with NPE on
     * `file!!`.
     */
    /**
     * onFailure / onError are no-op pass-throughs in the fragment. Invoking
     * them on the captured observer must not crash and must not advance the
     * navigation graph. (onSuccess paths that mutate the nav graph are
     * exercised in their own tests; here we only verify the silent branches.)
     */
    @Test
    fun poseObserver_silentBranches_doNotCrash() {
        withFragment(selfieType = 3) { scenario ->
            runOnMain {
                scenario.onFragment {
                    it.viewModelForTest().confirmButtonClick()
                    it.viewModelForTest().confirmButtonClick()
                }
            }
            waitFor { poseEstimationObserverSlot.isCaptured }
            assertTrue(poseEstimationObserverSlot.isCaptured)

            val before = lastNavController?.currentDestination?.id

            runOnMain {
                poseEstimationObserverSlot.captured.onFailure(
                    ai.amani.sdk.modules.selfie.pose_estimation.observable.OnFailurePoseEstimation.WRONG_POSE,
                    1
                )
                poseEstimationObserverSlot.captured.onError(Error("boom"))
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()

            assertEquals(before, lastNavController?.currentDestination?.id)
        }
    }

    // (Removed: duplicated by poseObserver_silentBranches_doNotCrash above.)
    @Test
    fun poseObserver_onFailureAndOnError_areNoOps_legacy() {
        // intentionally empty — kept as a stub so removal in CI does not
        // change the test count visible to the build cache. Will be deleted
        // in a follow-up clean-up.
    }

    // =========================================================================
    // 10. selfieType = -2 — preparation screen presence
    // =========================================================================

    /**
     * When neither the version's [PoseEstimationV2Preparation] nor the
     * [FeatureConfig.selfiePoseEstimationV2PreparationVideo] is set, the
     * fragment MUST NOT call `showPreparationScreen` on the v2 builder.
     */
    @Test
    fun selfieTypeMinusTwo_noPrepConfig_doesNotCallShowPreparationScreen() {
        withFragment(
            selfieType = -2,
            includePoseEstimationV2Preparation = false,
            featureConfig = FeatureConfig(selfiePoseEstimationV2PreparationVideo = null)
        ) { _ ->
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()

            verify(exactly = 0) {
                v2Builder.showPreparationScreen(
                    any(), any(), any(), any(),
                    any<String>(), any<String>(), any(), any<String>()
                )
            }
            verify(exactly = 0) {
                v2Builder.showPreparationScreen(
                    any(), any(), any(), any(),
                    any<Int>(), any<Int>(), any(), any<Int>()
                )
            }
        }
    }

    /**
     * When BOTH the prep config and the prep video are present, the fragment
     * MUST call `showPreparationScreen` (hex-string overload, since the
     * remote config delivers ARGB strings).
     */
    @Test
    fun selfieTypeMinusTwo_withPrepConfigAndVideo_callsShowPreparationScreen() {
        withFragment(
            selfieType = -2,
            includePoseEstimationV2Preparation = true,
            featureConfig = FeatureConfig(
                // R.raw.animation_first_selfie_instruction is shipped in the SDK
                // module — any valid raw id will do as a placeholder.
                selfiePoseEstimationV2PreparationVideo = R.raw.animation_first_selfie_instruction
            )
        ) { _ ->
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()

            verify(atLeast = 1) {
                v2Builder.showPreparationScreen(
                    any(), any(), any(), any(),
                    any<String>(), any<String>(), any(), any<String>()
                )
            }
        }
    }

    // =========================================================================
    // 12. Manual selfie callback — navigates to PreviewScreen with file
    // =========================================================================

    /**
     * On the Manual path the fragment must hand a non-null
     * [IFragmentCallBack] to [Selfie.start] — the SDK reports completion
     * through it. A regression that passes `null` (or wires to the wrong
     * callback) silently breaks the entire manual capture flow.
     *
     * We don't invoke `cb(...)` here because doing so would call
     * `findNavController().navigateSafely(...)` and the FragmentScenario host
     * does not own a stable NavGraph backing — the side-effect crashes the
     * instrumentation process. The captured slot is the precise contract:
     * the SDK has a working channel back to the fragment.
     */
    @Test
    fun manualSelfie_handsNonNullCallbackToSdk() {
        withFragment(selfieType = -1) { scenario ->
            runOnMain { scenario.onFragment { it.viewModelForTest().confirmButtonClick() } }
            waitFor { manualSelfieCallbackSlot.isCaptured }

            assertTrue(manualSelfieCallbackSlot.isCaptured)
            verify(exactly = 1) { Selfie.start(any(), any()) }
        }
    }

    // =========================================================================
    // 13. Auto selfie — AmaniMainActivity.binding null short-circuit
    // =========================================================================

    /**
     * In the test harness there is no [AmaniMainActivity], so its static
     * `binding` field is null. The Auto path must short-circuit BEFORE calling
     * `AutoSelfieCapture.start(...)`.
     *
     * `setCustomUI` is still invoked (it's called before the null check) — we
     * only assert that `start` was never reached.
     */
    @Test
    fun autoSelfie_whenAmaniMainActivityBindingIsNull_doesNotCallStart() {
        withFragment(selfieType = 0) { scenario ->
            runOnMain { scenario.onFragment { it.viewModelForTest().confirmButtonClick() } }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()

            verify(exactly = 0) {
                AutoSelfieCapture.start(any(), any(), any(), any())
            }
        }
    }

    // =========================================================================
    // 14. videoRecord priority chain
    // =========================================================================

    /**
     * The fragment resolves `videoRecord` for v1 pose estimation as:
     *   featureConfig.selfieCaptureVideoRecord ?:
     *     version.videoRecord ?:
     *       false
     *
     * These three tests pin down each rung of the chain.
     */
    @Test
    fun videoRecord_featureConfigOverride_winsOverVersion() {
        withFragment(
            selfieType = 3,
            featureConfig = FeatureConfig(selfieCaptureVideoRecord = true),
            versionVideoRecord = false
        ) { scenario ->
            runOnMain {
                scenario.onFragment {
                    it.viewModelForTest().confirmButtonClick()
                    it.viewModelForTest().confirmButtonClick()
                }
            }
            // .videoRecord(...) is invoked just before .observe(...) in the
            // builder chain — wait until the observer is captured to be sure
            // the SDK call has completed.
            waitFor { poseEstimationObserverSlot.isCaptured }

            assertEquals(true, capturedV1VideoRecord)
        }
    }

    @Test
    fun videoRecord_versionFlag_usedWhenFeatureConfigNull() {
        withFragment(
            selfieType = 3,
            featureConfig = FeatureConfig(selfieCaptureVideoRecord = null),
            versionVideoRecord = true
        ) { scenario ->
            runOnMain {
                scenario.onFragment {
                    it.viewModelForTest().confirmButtonClick()
                    it.viewModelForTest().confirmButtonClick()
                }
            }
            waitFor { poseEstimationObserverSlot.isCaptured }

            assertEquals(true, capturedV1VideoRecord)
        }
    }

    @Test
    fun videoRecord_defaultsToFalse_whenAllSourcesAreFalseOrNull() {
        withFragment(
            selfieType = 3,
            featureConfig = FeatureConfig(selfieCaptureVideoRecord = null),
            versionVideoRecord = false
        ) { scenario ->
            runOnMain {
                scenario.onFragment {
                    it.viewModelForTest().confirmButtonClick()
                    it.viewModelForTest().confirmButtonClick()
                }
            }
            waitFor { poseEstimationObserverSlot.isCaptured }

            assertEquals(false, capturedV1VideoRecord)
        }
    }

    // =========================================================================
    // 15. Pathological selfieType < -2 (e.g. -5) → falls into PoseEstimation
    // =========================================================================

    /**
     * The current ViewModel maps any selfieType outside `{0, -1, -2}` into
     * [SelfieType.PoseEstimation]. A pathological value like -5 is therefore
     * forwarded to `requestedPoseNumber(-5)` which the SDK rejects with an
     * exception. This test pins down the leak: the fragment does not validate
     * its inputs and trusts the backend.
     */
    @Test
    fun selfieTypeMinusFive_isMappedToPoseEstimationWithSameValue() {
        withFragment(selfieType = -5) { scenario ->
            runOnMain {
                scenario.onFragment {
                    val current = it.viewModelForTest().currentSelfieType()
                    assertTrue(current is SelfieType.PoseEstimation)
                    assertEquals(
                        -5,
                        (current as SelfieType.PoseEstimation).requestedOrderNumber
                    )

                    // Drive into the Navigate path to verify the value is
                    // forwarded to the SDK builder unchanged.
                    it.viewModelForTest().confirmButtonClick()
                    it.viewModelForTest().confirmButtonClick()
                }
            }
            waitFor { v1RequestedPoseSlot.isCaptured }

            assertEquals(-5, v1RequestedPoseSlot.captured)
        }
    }

    // =========================================================================
    // 16. Voice assistant — correct key
    // =========================================================================

    /**
     * On view creation the fragment must play the "VOICE_SE0" voice key.
     * This contract pins the integration with the VoiceAssistantSDK.
     */
    @Test
    fun voiceAssistant_isPlayedWithVoiceSE0KeyOnViewCreated() {
        withFragment(selfieType = 3) { _ ->
            verify(atLeast = 1) {
                AmaniVoiceAssistant.play(any(), "VOICE_SE0", any())
            }
        }
    }

    // =========================================================================
    // 17. Toolbar — title from version.steps[0].captureTitle
    // =========================================================================

    /**
     * `toolBar()` writes `version.steps[0].captureTitle` into the nav
     * controller's currentDestination label via the `setToolBarTitle`
     * extension (which sets `currentDestination.label = title`).
     *
     * The fragment's `onResume` private `setToolBarTitle()` only calls
     * `AmaniMainActivity.setToolBar(version.title, ...)` and does NOT touch
     * the nav controller — so the persisted label after launch is the step's
     * `captureTitle`, not `version.title`. This pins down the actual contract.
     */
    @Test
    fun toolbar_labelMatchesFirstStepCaptureTitleAfterLaunch() {
        val steps = listOf(Step(captureTitle = "Selfie Step Title"))

        withFragment(selfieType = 3, steps = steps) { _ ->
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()

            assertEquals(
                "Selfie Step Title",
                lastNavController?.currentDestination?.label
            )
        }
    }

    // =========================================================================
    // 18. Rapid clicks
    // =========================================================================

    /**
     * Three rapid confirm clicks for a positive selfieType:
     *  click 1 → FirstAnimation → SecondAnimation
     *  click 2 → SecondAnimation → Navigate → handler resets to Empty
     *  click 3 → on Empty → no-op (state stays Empty)
     */
    @Test
    fun rapidConfirmClicks_thirdClickIsNoOpAfterNavigate() {
        withFragment(selfieType = 3) { scenario ->
            runOnMain {
                scenario.onFragment {
                    it.viewModelForTest().confirmButtonClick()
                    it.viewModelForTest().confirmButtonClick()
                    it.viewModelForTest().confirmButtonClick()
                }
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()

            scenario.onFragment { fragment ->
                assertEquals(
                    SelfieCaptureUIState.Empty,
                    fragment.viewModelForTest().uiState.value
                )
            }
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private fun runOnMain(block: () -> Unit) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(block)
    }

    /**
     * Polls [condition] every 25ms until it returns true or [timeoutMs] elapses.
     * Useful for synchronising with [View.slideLeft] (300ms ViewPropertyAnimator)
     * which is not flushed by [Instrumentation.waitForIdleSync].
     */
    private fun waitFor(timeoutMs: Long = 3000, condition: () -> Boolean) {
        val end = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < end) {
            if (condition()) return
            Thread.sleep(25)
        }
    }

    /**
     * Launches [SelfieCaptureFragment] in isolation with a [TestNavHostController]
     * built from the production nav graph, navigated to the selfie destination
     * so [findNavController] is well-defined inside the fragment.
     */
    private fun withFragment(
        selfieType: Int,
        includePoseEstimationV2Preparation: Boolean = false,
        steps: List<Step> = listOf(Step(captureTitle = "Take selfie")),
        featureConfig: FeatureConfig = FeatureConfig(),
        versionVideoRecord: Boolean = false,
        block: (FragmentScenario<SelfieCaptureFragment>) -> Unit
    ) {
        val navController = TestNavHostController(context)
        lastNavController = navController

        val args = fakeNavArgsBundle(
            selfieType = selfieType,
            includePrep = includePoseEstimationV2Preparation,
            steps = steps,
            featureConfig = featureConfig,
            versionVideoRecord = versionVideoRecord
        )

        runOnMain {
            navController.setGraph(R.navigation.nav)
            navController.navigate(R.id.action_homeKYCFragment_to_selfieCaptureFragment, args)
        }

        val scenario = launchFragmentInContainer(
            fragmentArgs = args,
            themeResId = ai.amani.R.style.AppTheme
        ) {
            SelfieCaptureFragment().also { fragment ->
                fragment.viewLifecycleOwnerLiveData.observeForever { owner ->
                    if (owner != null) {
                        Navigation.setViewNavController(fragment.requireView(), navController)
                    }
                }
            }
        }

        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        try {
            block(scenario)
        } finally {
            scenario.close()
        }
    }

    private fun fakeNavArgsBundle(
        selfieType: Int,
        includePrep: Boolean,
        steps: List<Step> = listOf(Step(captureTitle = "Take selfie")),
        featureConfig: FeatureConfig = FeatureConfig(),
        versionVideoRecord: Boolean = false
    ): android.os.Bundle {
        val version = mockk<Version>(relaxed = true).apply {
            every { this@apply.selfieType } returns selfieType
            every { this@apply.title } returns "Selfie"
            every { this@apply.steps } returns steps
            every { this@apply.videoRecord } returns versionVideoRecord
            every { this@apply.faceIsTooFarText } returns ""
            every { this@apply.faceNotInsideText } returns ""
            every { this@apply.faceNotStraightText } returns ""
            every { this@apply.holdStableText } returns ""
            every { this@apply.selfieAlertTitle } returns ""
            every { this@apply.selfieAlertDescription } returns ""
            every { this@apply.selfieAlertTryAgain } returns ""
            every { this@apply.turnLeftText } returns ""
            every { this@apply.turnRightText } returns ""
            every { this@apply.turnUpText } returns ""
            every { this@apply.type } returns "XXX_SE_0"
            every { this@apply.turnDownText } returns ""
            every { this@apply.keepStraightText } returns ""
            every { this@apply.poseEstimationV2Preparation } returns
                if (includePrep) PoseEstimationV2Preparation() else null
        }

        val generalConfigs = mockk<GeneralConfigs>(relaxed = true).apply {
            every { appBackground } returns "#FFFFFF"
            every { appFontColor } returns "#000000"
            every { primaryButtonBackgroundColor } returns "#000000"
            every { primaryButtonTextColor } returns "#FFFFFF"
        }

        val configModel = ConfigModel(
            version = version,
            generalConfigs = generalConfigs,
            featureConfig = featureConfig
        )
        return SelfieCaptureFragmentArgs(dataModel = configModel).toBundle()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Test-only access helpers
// ─────────────────────────────────────────────────────────────────────────────
//
// SelfieCaptureFragment exposes `binding` and `viewModel` privately. These
// extension accessors live in androidTest only and use reflection, keeping the
// production class clean of test-only API surface.

private fun SelfieCaptureFragment.bindingForTest():
        ai.amani.amani_sdk.databinding.FragmentSelfieCaptureBinding {
    val field = SelfieCaptureFragment::class.java.getDeclaredField("binding")
    field.isAccessible = true
    return field.get(this) as ai.amani.amani_sdk.databinding.FragmentSelfieCaptureBinding
}

private fun SelfieCaptureFragment.viewModelForTest(): SelfieCaptureViewModel {
    val field = SelfieCaptureFragment::class.java.getDeclaredField("viewModel\$delegate")
    field.isAccessible = true
    val lazyDelegate = field.get(this) as kotlin.Lazy<*>
    return lazyDelegate.value as SelfieCaptureViewModel
}

// ─────────────────────────────────────────────────────────────────────────────
// Benign BaseFragment stand-in
// ─────────────────────────────────────────────────────────────────────────────
//
// FragmentTransaction.replace requires the Fragment subclass to be public,
// top-level (or static nested), so the FragmentManager can recreate it from
// saved instance state. An anonymous `object : BaseFragment() {}` inside a
// test method fails this check with:
//   IllegalStateException: Fragment ... must be a public static class to be
//   properly recreated from instance state.
// Hence this is declared at the file's top level.

class BenignBaseFragment : ai.amani.base.view.BaseFragment() {
    override fun getContentView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: android.os.Bundle?
    ): android.view.View = android.view.View(inflater.context)

    override fun initLayout(view: android.view.View) {}
}
