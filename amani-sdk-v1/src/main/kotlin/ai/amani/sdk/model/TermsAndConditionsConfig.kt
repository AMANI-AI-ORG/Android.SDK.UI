package ai.amani.sdk.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Configuration model for the Terms & Conditions dialog.
 * All color values must be passed as hex strings (e.g. "#FFFFFF").
 */
@Parcelize
data class TermsAndConditionsConfig(
    val titleText: String,
    val descriptionText: String,
    val backgroundColor: String,
    val titleTextColor: String,
    val descriptionTextColor: String,
    val primaryButtonTextColor: String,
    val primaryButtonBackgroundColor: String,
    val primaryButtonBorderColor: String,
    val buttonRadius: Int = 20,
    val secondaryButtonBackgroundColor: String,
    val secondaryButtonTextColor: String,
    val secondaryButtonBorderColor: String,
    val acceptButtonText: String,
    val declineButtonText: String?
) : Parcelable
