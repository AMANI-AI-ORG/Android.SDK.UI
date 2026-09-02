package ai.amani.sdk.utils

import ai.amani.amani_sdk.R
import androidx.annotation.RawRes

/**
 * Which face of a document a guide animation illustrates. Deliberately independent of either
 * presentation layer (v1 Fragments, v2 Compose) so both can feed the same resolver without
 * one depending on the other's navigation types.
 */
enum class DocumentSide { Front, Back }

/**
 * The pre-capture guide illustrations bundled with the SDK, resolved from the document a step
 * is about ([documentId], one of [AmaniDocumentTypes]) and the [side] being captured.
 */
object DocumentGuideAnimation {

    @RawRes
    fun resFor(side: DocumentSide, documentId: String?): Int {
        val back = side == DocumentSide.Back
        return when (documentId) {
            AmaniDocumentTypes.PASSPORT ->
                if (back) R.raw.xx_pa_back else R.raw.xx_pa_front

            AmaniDocumentTypes.DRIVING_LICENSE ->
                if (back) R.raw.xx_dl_back else R.raw.xx_dl_front

            else ->
                if (back) R.raw.xx_id_back else R.raw.xx_id_front
        }
    }
}
