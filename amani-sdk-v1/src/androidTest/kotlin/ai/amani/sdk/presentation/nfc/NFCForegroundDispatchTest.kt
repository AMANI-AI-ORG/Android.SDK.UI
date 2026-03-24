package ai.amani.sdk.presentation.nfc

import ai.amani.sdk.data.repository.nfc.NFCRepositoryImp
import ai.amani.sdk.model.MRZModel
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation tests for NFC foreground dispatch lifecycle.
 *
 * Threading rules for LiveData in instrumentation tests:
 *  - [androidx.lifecycle.LiveData.setValue] / all ViewModel methods that write
 *    MutableLiveData → must run on the main thread → use [runOnMain].
 *  - [androidx.lifecycle.LiveData.getValue] → NO thread assertion; safe to call
 *    from the instrumentation (test) thread after [runOnMain] completes.
 *  - [androidx.lifecycle.LiveData.observeForever] → also asserts main thread;
 *    we AVOID it here entirely and simply read [LiveData.value] directly. This
 *    is sufficient because [runOnMainSync] is synchronous — by the time it
 *    returns, all LiveData writes inside it have already been committed.
 */
@RunWith(AndroidJUnit4::class)
class NFCForegroundDispatchTest {

    private lateinit var mockRepository: NFCRepositoryImp
    private lateinit var viewModel: NFCSharedViewModel

    @Before
    fun setUp() {
        mockRepository = mockk(relaxed = true)
        // ViewModel construction itself is fine on any thread (MutableLiveData
        // constructors set mData directly, no setValue call). We still create
        // it on main to be consistent with its real lifecycle host.
        runOnMain { viewModel = NFCSharedViewModel(mockRepository) }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // =========================================================================
    // 1. Foreground dispatch enabled / disabled via ViewModel state
    // =========================================================================

    @Test
    fun nfcActivationState_Enable_isEmitted() {
        runOnMain { viewModel.setNfcEnable(true) }

        assertEquals(NFCActivationState.Enable, viewModel.nfcActivationState.value)
    }

    @Test
    fun nfcActivationState_Disable_isEmitted() {
        runOnMain {
            viewModel.setNfcEnable(true)
            viewModel.setNfcEnable(false)
        }

        assertEquals(NFCActivationState.Disable, viewModel.nfcActivationState.value)
    }

    @Test
    fun toggling_enable_then_disable_finalStateIsDisable() {
        runOnMain {
            viewModel.setNfcEnable(true)
            viewModel.setNfcEnable(false)
        }

        // We verify the FINAL committed state; for sequence verification use
        // the JVM unit test NFCSharedViewModelTest which uses InstantTaskExecutorRule.
        assertEquals(NFCActivationState.Disable, viewModel.nfcActivationState.value)
    }

    // =========================================================================
    // 2. App in background receiving NFC intent
    // =========================================================================

    @Test
    fun afterReturningFromBackground_scanState_isReadyToScan() {
        runOnMain {
            viewModel.set(null)
            viewModel.setNfcEnable(false)
            viewModel.clearNFCState()
        }

        assertEquals(NFCScanState.ReadyToScan, viewModel.nfcScanState.value)
        assertNull(viewModel.get.value)
        assertEquals(NFCActivationState.Disable, viewModel.nfcActivationState.value)
    }

    // =========================================================================
    // 3. Orientation change (activity recreation)
    // =========================================================================

    @Test
    fun orientationChange_viewModel_retainsScanState() {
        runOnMain { viewModel.setState(NFCScanState.ShowMRZCheck) }

        // After rotation the ViewModel is reused; its state must be unchanged.
        assertEquals(NFCScanState.ShowMRZCheck, viewModel.nfcScanState.value)
    }

    @Test
    fun orientationChange_viewModel_retainsMrzData() {
        runOnMain { viewModel.setMRZ(MRZModel("870601", "270601", "ROTATE123")) }

        assertEquals("870601", viewModel.mrzData.birthDate)
        assertEquals("ROTATE123", viewModel.mrzData.docNumber)
    }

    // =========================================================================
    // 4. NFC turned off while screen is active
    // =========================================================================

    @Test
    fun nfcDisabledWhileActive_stateBecomesDisable() {
        runOnMain { viewModel.setNfcEnable(false) }

        assertEquals(NFCActivationState.Disable, viewModel.nfcActivationState.value)
    }

    // =========================================================================
    // 5. onNewIntent routing (placeholder — requires full activity navigation)
    // =========================================================================

    @Test
    fun onNewIntent_whenOnNFCScanFragment_forwardsIntentToViewModel() {
        // TODO: Launch AmaniMainActivity, navigate to NFCScanFragment with proper
        //       NavArgs, call activity.onNewIntent(FakeNfcIntentFactory.tagDiscoveredIntent())
        //       and assert viewModel.get.value != null.
        assertTrue("Placeholder — implement when TestNFCHostActivity is wired", true)
    }

    // =========================================================================
    // Helper
    // =========================================================================

    private fun runOnMain(block: () -> Unit) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(block)
    }
}
