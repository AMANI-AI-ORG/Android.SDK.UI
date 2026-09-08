package ai.amani.sdk.presentation_v2.home_kyc

import ai.amani.sdk.model.customer.Rule
import ai.amani.sdk.utils.AppConstant
import datamanager.model.config.ButtonColor
import datamanager.model.config.ButtonText
import datamanager.model.config.ButtonTextColor
import datamanager.model.config.DocumentList
import datamanager.model.config.GeneralConfigs
import datamanager.model.config.ResGetConfig
import datamanager.model.config.StepConfig
import datamanager.model.config.Version

/**
 * Mock server data for the [HomeKYCScreen] previews: a two-step ID + Selfie flow with the
 * same shapes the SDK receives at runtime ([ResGetConfig] GeneralConfigs / StepConfigs plus
 * the customer [Rule] list). Previews render through [HomeKYCMapper.toUiState] instead of
 * hand-written UI state, so the dot / row / heading rules being previewed are the real ones —
 * in particular the stepper rule that a dot is filled only for an APPROVED step.
 *
 * Debug-only sample data; nothing here is referenced by production code paths.
 */
internal object HomeKYCPreviewData {

    const val RULE_ID_DOCUMENT = "id_card"
    const val RULE_ID_SELFIE = "selfie"

    /** One KYC rule, as the customer endpoint returns it. */
    fun rule(
        id: String,
        title: String,
        status: String,
        sortOrder: Int
    ): Rule = Rule(
        adapter = null,
        attempt = 0,
        documentClasses = emptyList(),
        errors = emptyList(),
        id = id,
        phase = 1,
        sortOrder = sortOrder,
        status = status,
        title = title,
        isShowLoader = false,
        identifier = id
    )

    /**
     * ID + Selfie rules with the given statuses. Selfie is gated on ID (see [config]), so it
     * only becomes the active/current step once ID is approved.
     */
    fun rules(
        documentStatus: String = AppConstant.STATUS_NOT_UPLOADED,
        selfieStatus: String = AppConstant.STATUS_NOT_UPLOADED
    ): List<Rule> = listOf(
        rule(RULE_ID_DOCUMENT, "ID", documentStatus, sortOrder = 0),
        rule(RULE_ID_SELFIE, "Selfie", selfieStatus, sortOrder = 1)
    )

    /**
     * Mock config: brand colors + the v2 home/step strings, one StepConfig per rule. The
     * selfie step declares `mandatoryStepIDs = [id_card]`, mirroring a real gated profile.
     */
    fun config(): ResGetConfig = ResGetConfig(
        GeneralConfigs(
            mainTitleText = "Verification",
            primaryButtonBackgroundColor = "#1B62F2",
            appFontColor = "#0B1220",
            appBackground = "#FFFFFF",
            topBarBackground = "#FFFFFF",
            topBarFontColor = "#0B1220",
            successIconColor = "#12A150",
            errorIconColor = "#E5484D",
            v2HomeInitialTitle = "Let's get you verified",
            v2HomeInitialSubtitle = "{count} quick steps. Should take about a minute.",
            v2HomeProgressTitle = "You're making progress",
            v2HomeProgressSubtitle = "{count} more step to finish verification.",
            v2HomeRejectedTitle = "Verification incomplete",
            v2HomeRejectedSubtitle = "{count} step needs your attention before we can continue.",
            v2StepStartHereLabel = "Start here",
            v2StepUpNextLabel = "Up next",
            v2StepDefaultDuration = "~30 sec"
        ),
        listOf(
            stepConfig(
                id = RULE_ID_DOCUMENT,
                title = "ID",
                notUploadedLabel = "Upload your ID",
                estimatedTime = "~30 sec"
            ),
            stepConfig(
                id = RULE_ID_SELFIE,
                title = "Selfie",
                notUploadedLabel = "Take a selfie",
                estimatedTime = "~20 sec",
                mandatoryStepIDs = listOf(RULE_ID_DOCUMENT)
            )
        )
    )

    private fun stepConfig(
        id: String,
        title: String,
        notUploadedLabel: String,
        estimatedTime: String,
        mandatoryStepIDs: List<String> = emptyList()
    ): StepConfig = StepConfig(
        buttonColor = ButtonColor(
            approved = "#12A150",
            rejected = "#E5484D",
            automaticallyRejected = "#E5484D",
            pendingReview = "#F5A524",
            processing = "#1B62F2",
            notUploaded = "#1B62F2"
        ),
        buttonText = ButtonText(
            approved = "Verified",
            rejected = "Rejected · Action needed",
            automaticallyRejected = "Rejected · Action needed",
            pendingReview = "In review",
            processing = "Processing",
            notUploaded = notUploadedLabel
        ),
        buttonTextColor = ButtonTextColor(
            approved = "#FFFFFF",
            rejected = "#FFFFFF",
            automaticallyRejected = "#FFFFFF",
            pendingReview = "#FFFFFF",
            processing = "#FFFFFF",
            notUploaded = "#FFFFFF"
        ),
        id = id,
        title = title,
        identifier = id,
        mandatoryStepIDs = mandatoryStepIDs,
        mDocuments = arrayListOf(
            DocumentList(
                id = id,
                versions = listOf(
                    Version(
                        isHidden = false,
                        v2EstimatedTime = estimatedTime,
                        v2StepRejectionTitle = "$title could not be verified",
                        v2StepRejectionDescription = "Retake it in better lighting and hold steady."
                    )
                )
            )
        )
    )

    /** Fresh flow: nothing uploaded — ID is current (empty dot), Selfie locked. */
    fun start(): HomeKYCUiState = HomeKYCMapper.toUiState(rules(), config())

    /**
     * ID uploaded and still server-side. Both dots stay EMPTY: PROCESSING is not approval,
     * so nothing is filled yet.
     */
    fun documentProcessing(): HomeKYCUiState = HomeKYCMapper.toUiState(
        rules(documentStatus = AppConstant.STATUS_PROCESSING),
        config(),
        processingRuleId = RULE_ID_DOCUMENT
    )

    /** ID awaiting a reviewer — dot still empty (PENDING_REVIEW is not APPROVED). */
    fun documentPendingReview(): HomeKYCUiState = HomeKYCMapper.toUiState(
        rules(documentStatus = AppConstant.STATUS_PENDING_REVIEW),
        config()
    )

    /** ID approved: first dot FILLED, Selfie now current with an empty dot. */
    fun documentApproved(): HomeKYCUiState = HomeKYCMapper.toUiState(
        rules(documentStatus = AppConstant.STATUS_APPROVED),
        config()
    )

    /** Both steps approved: both dots filled. */
    fun allApproved(): HomeKYCUiState = HomeKYCMapper.toUiState(
        rules(
            documentStatus = AppConstant.STATUS_APPROVED,
            selfieStatus = AppConstant.STATUS_APPROVED
        ),
        config()
    )

    /** ID rejected: dot draws the danger ring (empty), row shows the inline error. */
    fun documentRejected(): HomeKYCUiState = HomeKYCMapper.toUiState(
        rules(documentStatus = AppConstant.STATUS_REJECTED),
        config(),
        errorOverrides = mapOf(RULE_ID_DOCUMENT to "We couldn't read the document clearly.")
    )
}
