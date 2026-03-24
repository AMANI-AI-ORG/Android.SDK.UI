package ai.amani.sdk.presentation.nfc

import ai.amani.sdk.data.repository.nfc.NFCRepositoryImp
import ai.amani.sdk.model.MRZModel
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import datamanager.model.config.Version
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Unit tests for [NFCSharedViewModel].
 *
 * Covers:
 * - NFC activation state transitions (Enable / Disable / Empty)
 * - Scan state machine (ReadyToScan, Cancelled, OutOfMaxAttempt, ShowMRZCheck, Failure, Success)
 * - Max attempt enforcement and counter reset
 * - MRZ data management
 * - scanNFC success / failure routing through a mocked repository
 */
@RunWith(JUnit4::class)
class NFCSharedViewModelTest {

    // Executes LiveData tasks synchronously on the calling thread.
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    // ---------------------------------------------------------------------------
    // Collaborators
    // ---------------------------------------------------------------------------

    private lateinit var mockRepository: NFCRepositoryImp
    private lateinit var viewModel: NFCSharedViewModel

    // Observers kept alive so LiveData emissions are recorded.
    private val scanStateValues = mutableListOf<NFCScanState>()
    private val activationStateValues = mutableListOf<NFCActivationState>()
    private lateinit var scanStateObserver: Observer<NFCScanState>
    private lateinit var activationStateObserver: Observer<NFCActivationState>

    // ---------------------------------------------------------------------------
    // Setup / Teardown
    // ---------------------------------------------------------------------------

    @Before
    fun setUp() {
        mockRepository = mockk(relaxed = true)
        viewModel = NFCSharedViewModel(mockRepository)

        scanStateObserver = Observer { state -> scanStateValues.add(state) }
        activationStateObserver = Observer { state -> activationStateValues.add(state) }

        viewModel.nfcScanState.observeForever(scanStateObserver)
        viewModel.nfcActivationState.observeForever(activationStateObserver)
    }

    @After
    fun tearDown() {
        viewModel.nfcScanState.removeObserver(scanStateObserver)
        viewModel.nfcActivationState.removeObserver(activationStateObserver)
        unmockkAll()
    }

    // =========================================================================
    // 1. Initial State
    // =========================================================================

    @Test
    fun `initial nfcScanState is ReadyToScan`() {
        assertEquals(NFCScanState.ReadyToScan, viewModel.nfcScanState.value)
    }

    @Test
    fun `initial nfcActivationState is Empty`() {
        assertEquals(NFCActivationState.Empty, viewModel.nfcActivationState.value)
    }

    @Test
    fun `initial intent LiveData is null`() {
        assertNull(viewModel.get.value)
    }

    // =========================================================================
    // 2. NFC Activation State
    // =========================================================================

    @Test
    fun `setNfcEnable true emits Enable`() {
        viewModel.setNfcEnable(true)

        assertEquals(NFCActivationState.Enable, viewModel.nfcActivationState.value)
    }

    @Test
    fun `setNfcEnable false emits Disable`() {
        viewModel.setNfcEnable(false)

        assertEquals(NFCActivationState.Disable, viewModel.nfcActivationState.value)
    }

    @Test
    fun `toggling nfc enable then disable records both states`() {
        viewModel.setNfcEnable(true)
        viewModel.setNfcEnable(false)

        assertTrue(activationStateValues.contains(NFCActivationState.Enable))
        assertTrue(activationStateValues.contains(NFCActivationState.Disable))
    }

    // =========================================================================
    // 3. Scan State — continueBtnClick / clearNFCState / setState
    // =========================================================================

    @Test
    fun `continueBtnClick emits ReadyToScan`() {
        viewModel.setState(NFCScanState.ShowMRZCheck) // put in a different state first
        viewModel.continueBtnClick()

        assertEquals(NFCScanState.ReadyToScan, viewModel.nfcScanState.value)
    }

    @Test
    fun `clearNFCState resets to ReadyToScan from any state`() {
        viewModel.setState(NFCScanState.Failure)
        viewModel.clearNFCState()

        assertEquals(NFCScanState.ReadyToScan, viewModel.nfcScanState.value)
    }

    @Test
    fun `setState propagates arbitrary state`() {
        viewModel.setState(NFCScanState.ShowMRZCheck)
        assertEquals(NFCScanState.ShowMRZCheck, viewModel.nfcScanState.value)

        viewModel.setState(NFCScanState.Failure)
        assertEquals(NFCScanState.Failure, viewModel.nfcScanState.value)
    }

    // =========================================================================
    // 4. cancelScan — attempt counter & state machine
    // =========================================================================

    @Test
    fun `first cancelScan below maxAttempt emits Cancelled`() {
        viewModel.cancelScan() // attempt 1 of 3

        assertEquals(NFCScanState.Cancelled, viewModel.nfcScanState.value)
    }

    @Test
    fun `second cancelScan below maxAttempt emits Cancelled`() {
        repeat(2) { viewModel.cancelScan() } // attempts 1 and 2 of 3

        assertEquals(NFCScanState.Cancelled, viewModel.nfcScanState.value)
    }

    @Test
    fun `third cancelScan reaches maxAttempt and emits OutOfMaxAttempt`() {
        repeat(3) { viewModel.cancelScan() }

        assertEquals(NFCScanState.OutOfMaxAttempt, viewModel.nfcScanState.value)
    }

    @Test
    fun `counter resets after OutOfMaxAttempt so first subsequent cancel emits Cancelled`() {
        repeat(3) { viewModel.cancelScan() } // triggers OutOfMaxAttempt, resets counter
        viewModel.cancelScan()               // attempt 1 of fresh cycle

        assertEquals(NFCScanState.Cancelled, viewModel.nfcScanState.value)
    }

    @Test
    fun `setMaxAttempt changes threshold — single cancel at limit emits OutOfMaxAttempt`() {
        val version = mockk<Version>(relaxed = true)
        every { version.maxAttempt } returns 1

        viewModel.setMaxAttempt(version)
        viewModel.cancelScan()

        assertEquals(NFCScanState.OutOfMaxAttempt, viewModel.nfcScanState.value)
    }

    @Test
    fun `setMaxAttempt with null version keeps default threshold of 3`() {
        viewModel.setMaxAttempt(null) // should be a no-op
        repeat(2) { viewModel.cancelScan() }

        // After 2 cancels with default max=3, still Cancelled
        assertEquals(NFCScanState.Cancelled, viewModel.nfcScanState.value)
    }

    @Test
    fun `rapid cancels produce correct sequence of Cancelled then OutOfMaxAttempt`() {
        repeat(3) { viewModel.cancelScan() }

        val nonInitialStates = scanStateValues.drop(1) // drop the initial ReadyToScan
        assertEquals(NFCScanState.Cancelled, nonInitialStates[0])
        assertEquals(NFCScanState.Cancelled, nonInitialStates[1])
        assertEquals(NFCScanState.OutOfMaxAttempt, nonInitialStates[2])
    }

    // =========================================================================
    // 5. MRZ Data Management
    // =========================================================================

    @Test
    fun `setMRZ stores all MRZ fields`() {
        val mrz = MRZModel(birthDate = "990101", expireDate = "280101", docNumber = "ABC123456")
        viewModel.setMRZ(mrz)

        assertEquals("990101", viewModel.mrzData.birthDate)
        assertEquals("280101", viewModel.mrzData.expireDate)
        assertEquals("ABC123456", viewModel.mrzData.docNumber)
    }

    @Test
    fun `mrzData defaults to empty strings`() {
        assertEquals("", viewModel.mrzData.birthDate)
        assertEquals("", viewModel.mrzData.expireDate)
        assertEquals("", viewModel.mrzData.docNumber)
    }

    @Test
    fun `setMRZ overwrites previously stored data`() {
        viewModel.setMRZ(MRZModel(birthDate = "800101", expireDate = "250101", docNumber = "OLD001"))
        viewModel.setMRZ(MRZModel(birthDate = "950505", expireDate = "300505", docNumber = "NEW002"))

        assertEquals("950505", viewModel.mrzData.birthDate)
        assertEquals("NEW002", viewModel.mrzData.docNumber)
    }

    @Test
    fun `setMRZ with empty strings clears previous values`() {
        viewModel.setMRZ(MRZModel("990101", "280101", "ABC123"))
        viewModel.setMRZ(MRZModel())

        assertEquals("", viewModel.mrzData.birthDate)
        assertEquals("", viewModel.mrzData.docNumber)
    }

    // =========================================================================
    // 6. Intent Forwarding
    // =========================================================================

    @Test
    fun `set(intent) updates the get LiveData`() {
        val fakeIntent = mockk<android.content.Intent>(relaxed = true)
        viewModel.set(fakeIntent)

        assertEquals(fakeIntent, viewModel.get.value)
    }

    @Test
    fun `set(null) clears the intent`() {
        viewModel.set(mockk(relaxed = true))
        viewModel.set(null)

        assertNull(viewModel.get.value)
    }

    // =========================================================================
    // 7. scanNFC — repository delegation and state routing
    // =========================================================================

    /**
     * We avoid constructing a real android.nfc.Tag (final class, requires framework).
     * Instead we mock the Intent + the repository scan call, and verify the
     * ViewModel routes the callbacks to the correct LiveData states.
     */
    @Test
    fun `scanNFC on repository success emits Success state`() {
        val mockIntent = buildMockIntentWithTag()

        every {
            mockRepository.scan(
                tag = any(),
                context = any(),
                birthDate = any(),
                expireDate = any(),
                documentNumber = any(),
                onComplete = captureLambda(),
                onFailure = any()
            )
        } answers {
            // Immediately invoke the onComplete lambda
            val onComplete = arg<() -> Unit>(5)
            onComplete()
        }

        viewModel.scanNFC(mockIntent, mockk(relaxed = true))

        assertEquals(NFCScanState.Success, viewModel.nfcScanState.value)
    }

    @Test
    fun `scanNFC on failure with non-null error and attempt below max emits ShowMRZCheck`() {
        val mockIntent = buildMockIntentWithTag()

        every {
            mockRepository.scan(
                tag = any(), context = any(), birthDate = any(),
                expireDate = any(), documentNumber = any(),
                onComplete = any(), onFailure = captureLambda()
            )
        } answers {
            val onFailure = arg<(String?) -> Unit>(6)
            onFailure("AUTH_FAILED")
        }

        viewModel.scanNFC(mockIntent, mockk(relaxed = true))

        assertEquals(NFCScanState.ShowMRZCheck, viewModel.nfcScanState.value)
    }

    @Test
    fun `scanNFC on failure with null error and attempt below max emits Failure`() {
        val mockIntent = buildMockIntentWithTag()

        every {
            mockRepository.scan(
                tag = any(), context = any(), birthDate = any(),
                expireDate = any(), documentNumber = any(),
                onComplete = any(), onFailure = captureLambda()
            )
        } answers {
            val onFailure = arg<(String?) -> Unit>(6)
            onFailure(null)
        }

        viewModel.scanNFC(mockIntent, mockk(relaxed = true))

        assertEquals(NFCScanState.Failure, viewModel.nfcScanState.value)
    }

    @Test
    fun `scanNFC on failure at maxAttempt emits OutOfMaxAttempt`() {
        val mockIntent = buildMockIntentWithTag()

        every {
            mockRepository.scan(
                tag = any(), context = any(), birthDate = any(),
                expireDate = any(), documentNumber = any(),
                onComplete = any(), onFailure = captureLambda()
            )
        } answers {
            val onFailure = arg<(String?) -> Unit>(6)
            onFailure(null)
        }

        // Default maxAttempt = 3; scan 3 times to exhaust the budget.
        repeat(3) {
            viewModel.scanNFC(mockIntent, mockk(relaxed = true))
        }

        assertEquals(NFCScanState.OutOfMaxAttempt, viewModel.nfcScanState.value)
    }

    @Test
    fun `scanNFC passes stored MRZ values to repository`() {
        val mrz = MRZModel("990101", "280101", "DOC9876")
        viewModel.setMRZ(mrz)

        val mockIntent = buildMockIntentWithTag()
        every { mockRepository.scan(any(), any(), any(), any(), any(), any(), any()) } just Runs

        viewModel.scanNFC(mockIntent, mockk(relaxed = true))

        verify {
            mockRepository.scan(
                tag = any(),
                context = any(),
                birthDate = "990101",
                expireDate = "280101",
                documentNumber = "DOC9876",
                onComplete = any(),
                onFailure = any()
            )
        }
    }

    // =========================================================================
    // 8. Edge Cases
    // =========================================================================

    @Test
    fun `multiple rapid scan successes only emit one Success then reset`() {
        val mockIntent = buildMockIntentWithTag()

        every {
            mockRepository.scan(any(), any(), any(), any(), any(), onComplete = captureLambda(), any())
        } answers {
            arg<() -> Unit>(5).invoke()
        }

        viewModel.scanNFC(mockIntent, mockk(relaxed = true))
        viewModel.clearNFCState()
        viewModel.scanNFC(mockIntent, mockk(relaxed = true))

        assertEquals(NFCScanState.Success, viewModel.nfcScanState.value)
    }

    @Test
    fun `mix of cancel and scan failures accumulates attempts correctly`() {
        val mockIntent = buildMockIntentWithTag()

        every {
            mockRepository.scan(any(), any(), any(), any(), any(), any(), onFailure = captureLambda())
        } answers { arg<(String?) -> Unit>(6).invoke(null) }

        viewModel.cancelScan()                          // attempt=1 → Cancelled
        viewModel.scanNFC(mockIntent, mockk(relaxed = true)) // attempt=2 → Failure
        viewModel.cancelScan()                          // attempt=3 → OutOfMaxAttempt

        assertEquals(NFCScanState.OutOfMaxAttempt, viewModel.nfcScanState.value)
    }

    @Test
    fun `late onFailure callback after cancelScan does not double-count the attempt`() {
        // Capture the onFailure lambda without invoking it immediately — simulates
        // an in-flight NFC operation whose result arrives after the user cancels.
        val lateOnFailure = slot<(String?) -> Unit>()
        every {
            mockRepository.scan(any(), any(), any(), any(), any(), any(), capture(lateOnFailure))
        } just Runs

        val version = mockk<Version>(relaxed = true)
        every { version.maxAttempt } returns 2
        viewModel.setMaxAttempt(version) // maxAttempt = 2

        viewModel.scanNFC(buildMockIntentWithTag(), mockk(relaxed = true)) // scan started, callback pending

        viewModel.cancelScan() // user cancels: attempt = 1 → Cancelled
        assertEquals(NFCScanState.Cancelled, viewModel.nfcScanState.value)

        // Late NFC callback arrives after cancel — must be ignored.
        lateOnFailure.captured.invoke(null)

        // Without the guard: attempt would become 2 → OutOfMaxAttempt (wrong).
        // With the guard: attempt stays at 1, Cancelled state is preserved.
        assertEquals(NFCScanState.Cancelled, viewModel.nfcScanState.value)
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Builds a mocked [android.content.Intent] that returns a mocked [android.nfc.Tag]
     * from its extras, satisfying the `intent.extras!!.parcelable<Tag>(NfcAdapter.EXTRA_TAG)` call
     * inside [NFCSharedViewModel.scanNFC].
     */
    private fun buildMockIntentWithTag(): android.content.Intent {
        val mockTag = mockk<android.nfc.Tag>(relaxed = true)
        val mockBundle = mockk<android.os.Bundle>(relaxed = true)
        every { mockBundle.getParcelable<android.nfc.Tag>(android.nfc.NfcAdapter.EXTRA_TAG) } returns mockTag
        @Suppress("DEPRECATION")
        every { mockBundle.get(android.nfc.NfcAdapter.EXTRA_TAG) } returns mockTag

        val mockIntent = mockk<android.content.Intent>(relaxed = true)
        every { mockIntent.extras } returns mockBundle

        return mockIntent
    }
}
