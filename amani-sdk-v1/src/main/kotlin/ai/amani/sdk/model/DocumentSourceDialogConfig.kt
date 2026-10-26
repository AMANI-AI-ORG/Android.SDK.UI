package ai.amani.sdk.model

import ai.amani.sdk.presentation.common.document_picker.DocumentSourceChoiceTexts
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Configuration of the V1 source dialog of an `optional` document.
 * All color values must be passed as hex strings (e.g. "#FFFFFF").
 *
 * @param versionType type of the document the choice is made for, echoed back with the choice
 * so the host can resolve the version even after it was recreated
 */
@Parcelize
data class DocumentSourceDialogConfig(
    val versionType: String?,
    val texts: DocumentSourceChoiceTexts,
    val backgroundColor: String,
    val fontColor: String,
    val primaryButtonBackgroundColor: String,
    val primaryButtonTextColor: String,
    val primaryButtonBorderColor: String,
    val secondaryButtonBackgroundColor: String,
    val secondaryButtonTextColor: String,
    val secondaryButtonBorderColor: String,
    val buttonRadius: Int = 20
) : Parcelable
