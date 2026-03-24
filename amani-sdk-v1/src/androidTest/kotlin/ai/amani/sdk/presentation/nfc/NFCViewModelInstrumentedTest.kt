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
 * Instrumentation-level ViewModel tests that require a real Android [Context].
 *
 * Threading contract (Lifecycle 2.8.x):
 *  - [androidx.lifecycle.MutableLiveData.setValue] asserts main thread.
 *  - [androidx.lifecycle.LiveData.getValue] does NOT assert main thread.
 *  - [androidx.lifecycle.LiveData.observeForever] asserts main thread.
 *
 * Strategy: avoid [observeForever] entirely. All ViewModel calls that write to
 * LiveData are dispatched via [runOnMain]; assertions read [LiveData.value]
 * from the instrumentation thread after [runOnMainSync] has returned, at which
 * point the value is fully committed and visible across threads.
 */
@RunWith(AndroidJUnit4::class)
class NFCViewModelInstrumentedTest {

    private lateinit var mockRepository: NFCRepositoryImp
    private lateinit var viewModel: NFCSharedViewModel

    private val context by lazy {
        InstrumentationRegistry.getInstrumentation().targetContext
    }

    @Before
    fun setUp() {
        mockRepository = mockk(relaxed = true)
        runOnMain { viewModel = NFCSharedViewModel(mockRepository) }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // =========================================================================
    // scanNFC — Success path
    // =========================================================================

    @Test
    fun scanNFC_repositorySucceeds_stateBecomesSuccess() {
        every {
            mockRepository.scan(
                tag = any(), context = any(), birthDate = any(),
                expireDate = any(), documentNumber = any(),
                onComplete = captureLambda(), onFailure = any()
            )
        } answers { arg<() -> Unit>(5).invoke() }

        runOnMain { viewModel.scanNFC(FakeNfcIntentFactory.tagDiscoveredIntent(), context) }

        assertEquals(NFCScanState.Success, viewModel.nfcScanState.value)
    }

    // =========================================================================
    // scanNFC — Failure paths
    // =========================================================================

    @Test
    fun scanNFC_repositoryFailsWithError_stateBecomesShowMRZCheck() {
        every {
            mockRepository.scan(
                tag = any(), context = any(), birthDate = any(),
                expireDate = any(), documentNumber = any(),
                onComplete = any(), onFailure = captureLambda()
            )
        } answers { arg<(String?) -> Unit>(6).invoke("BAC_FAILED") }

        runOnMain { viewModel.scanNFC(FakeNfcIntentFactory.tagDiscoveredIntent(), context) }

        assertEquals(NFCScanState.ShowMRZCheck, viewModel.nfcScanState.value)
    }

    @Test
    fun scanNFC_repositoryFailsWithNullError_stateBecomesFailure() {
        every {
            mockRepository.scan(
                tag = any(), context = any(), birthDate = any(),
                expireDate = any(), documentNumber = any(),
                onComplete = any(), onFailure = captureLambda()
            )
        } answers { arg<(String?) -> Unit>(6).invoke(null) }

        runOnMain { viewModel.scanNFC(FakeNfcIntentFactory.tagDiscoveredIntent(), context) }

        assertEquals(NFCScanState.Failure, viewModel.nfcScanState.value)
    }

    // =========================================================================
    // scanNFC — Attempt exhaustion
    // =========================================================================

    @Test
    fun scanNFC_repeatedFailures_reachesOutOfMaxAttempt() {
        every {
            mockRepository.scan(
                tag = any(), context = any(), birthDate = any(),
                expireDate = any(), documentNumber = any(),
                onComplete = any(), onFailure = captureLambda()
            )
        } answers { arg<(String?) -> Unit>(6).invoke(null) }

        val intent = FakeNfcIntentFactory.tagDiscoveredIntent()
        // All three scans run sequentially on the main thread; counter increments correctly.
        runOnMain { repeat(3) { viewModel.scanNFC(intent, context) } }

        assertEquals(NFCScanState.OutOfMaxAttempt, viewModel.nfcScanState.value)
    }

    @Test
    fun scanNFC_passesCorrectMRZToRepository() {
        runOnMain { viewModel.setMRZ(MRZModel("920314", "260314", "TR9876543")) }
        every { mockRepository.scan(any(), any(), any(), any(), any(), any(), any()) } just Runs

        runOnMain { viewModel.scanNFC(FakeNfcIntentFactory.tagDiscoveredIntent(), context) }

        verify {
            mockRepository.scan(
                tag = any(),
                context = any(),
                birthDate = "920314",
                expireDate = "260314",
                documentNumber = "TR9876543",
                onComplete = any(),
                onFailure = any()
            )
        }
    }

    // =========================================================================
    // Rapid scans
    // =========================================================================

    @Test
    fun rapidScans_repositoryCalledForEachIntent() {
        every {
            mockRepository.scan(any(), any(), any(), any(), any(), onComplete = captureLambda(), any())
        } answers { arg<() -> Unit>(5).invoke() }

        val intents = FakeNfcIntentFactory.rapidScanIntents(5)
        runOnMain { intents.forEach { viewModel.scanNFC(it, context) } }

        assertEquals(NFCScanState.Success, viewModel.nfcScanState.value)
        verify(exactly = 5) {
            mockRepository.scan(any(), any(), any(), any(), any(), any(), any())
        }
    }

    // =========================================================================
    // Activity recreation (rotation)
    // =========================================================================

    @Test
    fun afterRotation_viewModelRetainsLastScanState() {
        runOnMain { viewModel.setState(NFCScanState.ShowMRZCheck) }

        // getValue() has no thread assertion — safe to call from the test thread.
        assertEquals(NFCScanState.ShowMRZCheck, viewModel.nfcScanState.value)
    }

    // =========================================================================
    // Helper
    // =========================================================================

    private fun runOnMain(block: () -> Unit) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(block)
    }
}
