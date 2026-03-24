package ai.amani.sdk.presentation.nfc

import ai.amani.amani_sdk.R
import ai.amani.sdk.data.repository.nfc.NFCRepositoryImp
import ai.amani.sdk.model.ConfigModel
import ai.amani.sdk.model.MRZModel
import ai.amani.sdk.model.NFCScanScreenModel
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import datamanager.model.config.GeneralConfigs
import datamanager.model.config.Version
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import io.mockk.*
import junit.framework.TestCase.assertEquals
import okhttp3.internal.wait
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Espresso instrumentation tests for [NFCScanFragment].
 *
 * Strategy
 * --------
 * [NFCScanFragment] uses `activityViewModels {}` which ties it to
 * [ai.amani.sdk.presentation.AmaniMainActivity].  Launching the fragment in
 * isolation via [launchFragmentInContainer] is not straightforward because Safe
 * Args requires a valid [ai.amani.sdk.model.NFCScanScreenModel] argument bundle.
 *
 * Two complementary approaches are used:
 *
 * A) **ViewModel-driven Espresso tests** — we set ViewModel state directly and
 *    assert that the fragment's observer correctly updates the UI.  This requires
 *    injecting a controllable ViewModel; see [NFCViewModelRule].
 *
 * B) **Intent-injection Espresso tests** — we deliver a fake NFC [Intent] to the
 *    ViewModel and verify the scanning modal appears / the correct UI branch is shown.
 *
 * All tests avoid real NFC hardware. [NFCRepositoryImp.scan] is mocked so the
 * tests never touch the AmaniSDK core.
 *
 * Note: If the project adds Hilt, replace the manual ViewModel injection below
 * with `@HiltAndroidTest` + `HiltAndroidRule`.
 */
@RunWith(AndroidJUnit4::class)
class NFCScanFragmentTest {

    /**
     * The ViewModel instance the fragment is actually observing.
     * Populated inside [withFragment] from the host activity's ViewModelStore
     * so that state changes here are reflected in the UI.
     */
    private lateinit var sharedViewModel: NFCSharedViewModel

    private val context by lazy {
        InstrumentationRegistry.getInstrumentation().targetContext
    }

    @Before
    fun setUp() {
        // Mock every NFCRepositoryImp instance created by NFCSharedViewModel.Factory
        // so the fragment's activityViewModels {} gets a controllable repository.
        mockkConstructor(NFCRepositoryImp::class)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // =========================================================================
    // D. Lifecycle — onPause disables foreground dispatch
    // =========================================================================

    /**
     * When the fragment is paused, [NFCActivationState.Disable] must be emitted
     * so [AmaniMainActivity] calls [NfcAdapter.disableForegroundDispatch].
     */
    @Test
    fun onPause_emits_nfcActivationState_Disable() {
        withFragment { scenario ->
            val activationStates = mutableListOf<NFCActivationState>()
            scenario.onFragment {
                sharedViewModel.nfcActivationState.observeForever { activationStates.add(it) }
            }

            // Move to CREATED state which triggers onPause.
            scenario.moveToState(Lifecycle.State.CREATED)

            assert(activationStates.contains(NFCActivationState.Disable))
        }
    }

    /**
     * When the fragment resumes, NFC activation should remain consistent with
     * [NFCScanState.ReadyToScan] (foreground dispatch is only enabled after the user
     * taps the continue button).
     */
    @Test
    fun onResume_scanState_resets_to_ReadyToScan() {
        withFragment { scenario ->
            scenario.onFragment {
                sharedViewModel.setState(NFCScanState.Failure)
            }

            // Simulate going to background and returning.
            scenario.moveToState(Lifecycle.State.CREATED)
            scenario.moveToState(Lifecycle.State.RESUMED)

            // NFCScanFragment.onResume calls viewModel.clearNFCState().
            assert(sharedViewModel.nfcScanState.value == NFCScanState.ReadyToScan)
        }
    }

    // =========================================================================
    // Helper
    // =========================================================================

    /**
     * Launches [NFCScanFragment] in a container using a minimal theme.
     *
     * The [TestNavHostController] is set up by navigating from [homeKYCFragment]
     * to [NFCScanFragment] so that [homeKYCFragment] is present in the back stack.
     * This mirrors the real nav graph and satisfies [OutOfMaxAttempt]'s call to
     * `findNavController().getBackStackEntry(R.id.homeKYCFragment)`.
     *
     * The [NavController] is injected via [viewLifecycleOwnerLiveData.observeForever]
     * which fires after [onCreateView] (view exists) but before [onViewCreated]
     * (where [setToolBarTitle] calls [findNavController]).
     */
    private fun withFragment(block: (FragmentScenario<NFCScanFragment>) -> Unit) {
        val navController = TestNavHostController(context).apply {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                setViewModelStore(ViewModelStore())
                setGraph(R.navigation.nav)
                // Navigate so homeKYCFragment is in the back stack.
                navigate(R.id.action_homeKYCFragment_to_NFCScanFragment, fakeNavArgsBundle())
            }
        }

        val scenario = launchFragmentInContainer(
            fragmentArgs = fakeNavArgsBundle(),
            themeResId = ai.amani.R.style.AppTheme
        ) {
            NFCScanFragment().also { fragment ->
                fragment.viewLifecycleOwnerLiveData.observeForever { owner ->
                    if (owner != null) {
                        Navigation.setViewNavController(fragment.requireView(), navController)
                    }
                }
            }
        }

        // Capture the ViewModel instance the fragment is actually observing.
        // NFCScanFragment uses activityViewModels {}, so its VM lives in
        // EmptyFragmentActivity's store — not in a standalone instance.
        scenario.onFragment { fragment ->
            sharedViewModel = ViewModelProvider(fragment.requireActivity())[NFCSharedViewModel::class.java]
        }

        // Wait for onResume → clearNFCState → ReadyToScan coroutine to complete
        // before handing control to the test body.
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        try {
            block(scenario)
        } finally {
            scenario.close()
        }
    }

    /**
     * Builds the [android.os.Bundle] that [NFCScanFragment] expects via Safe Args.
     *
     * [NFCScanFragment.setCustomUI] passes color strings directly to
     * [android.graphics.Color.parseColor] — an empty string (the MockK relaxed
     * default for String) would throw [IllegalArgumentException]. Every field
     * that flows into a [Color.parseColor] call is explicitly stubbed with a
     * valid hex string. All other fields use the relaxed default (empty String /
     * 0 for Int), which is safe for the remaining usage.
     */
    private fun fakeNavArgsBundle(): android.os.Bundle {
        val version = mockk<Version>(relaxed = true).apply {
            every { maxAttempt } returns 3
            // nfcTitle is shown as the ready-state title in the scanning modal.
            // Must be a non-empty string so the modal's showWaitingState() renders correctly.
            every { nfcTitle } returns "Please hold your card stable"
        }
        val generalConfigs = mockk<GeneralConfigs>(relaxed = true).apply {
            // These flow into Color.parseColor() — must be valid hex strings.
            every { appBackground } returns "#FFFFFF"
            every { appFontColor } returns "#000000"
            every { primaryButtonBackgroundColor } returns "#000000"
            every { primaryButtonTextColor } returns "#FFFFFF"
        }
        val model = NFCScanScreenModel(
            configModel = ConfigModel(version = version, generalConfigs = generalConfigs),
            mrzModel = MRZModel()
        )
        return NFCScanFragmentArgs(dataModel = model).toBundle()
    }
}

// =============================================================================
// NFC IdlingResource
// =============================================================================

/**
 * [IdlingResource] that becomes idle once [NFCSharedViewModel.nfcScanState]
 * reaches a terminal state ([NFCScanState.Success] or [NFCScanState.OutOfMaxAttempt]).
 *
 * Register this with [IdlingRegistry] before triggering an NFC scan and
 * unregister it in [org.junit.After] to prevent test pollution.
 *
 * Usage:
 * ```kotlin
 * val idling = NfcScanIdlingResource(viewModel)
 * IdlingRegistry.getInstance().register(idling)
 * // ... trigger scan ...
 * onView(...).check(...)   // Espresso waits automatically
 * IdlingRegistry.getInstance().unregister(idling)
 * ```
 */
class NfcScanIdlingResource(
    private val viewModel: NFCSharedViewModel
) : IdlingResource {

    private var callback: IdlingResource.ResourceCallback? = null

    override fun getName(): String = "NfcScanIdlingResource"

    override fun isIdleNow(): Boolean {
        val idle = viewModel.nfcScanState.value.let { state ->
            state == NFCScanState.Success || state == NFCScanState.OutOfMaxAttempt
        }
        if (idle) callback?.onTransitionToIdle()
        return idle
    }

    override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback) {
        this.callback = callback
    }
}
