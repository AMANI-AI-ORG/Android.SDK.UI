package ai.amani.sdk.presentation_v2.preview_screen

import android.graphics.Bitmap

/**
 * In-memory hand-off for the just-captured frame between a capture screen and its
 * confirm/preview screen.
 *
 * The SDK camera callbacks deliver the frame as a correctly-oriented [Bitmap]; persisting
 * it to disk and re-decoding on the preview screen lost that orientation (EXIF isn't
 * applied by BitmapFactory), showing selfies rotated. Passing the bitmap directly avoids
 * the whole round-trip. Destinations stay Parcelable-small: they carry no image at all and
 * the preview reads [latest] instead.
 *
 * Single-slot on purpose — the flow only ever confirms one just-captured frame at a time
 * (a retake simply overwrites it). Not process-death safe: after process restore the
 * preview falls back to its placeholder and the user retakes.
 */
internal object CapturedFrame {
    var latest: Bitmap? = null
}
