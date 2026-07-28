package ai.amani.sdk.presentation_v2.components

import ai.amani.sdk.presentation_v2.theme.AmaniV2Theme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Centered progress indicator on a transparent surface. Used as the V2 loading state
 * while the SDK fetches GeneralConfigs (colors + strings): the activity window is
 * translucent, so the screen that launched KYC stays visible behind this loader until
 * the real, config-driven content is ready.
 *
 * Deliberately draws no background — keep the container transparent so the previous
 * screen shows through.
 */
@Composable
fun AmaniV2Loader(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            // GeneralConfigs.loaderColor (falls back to the accent inside the palette factory).
            color = AmaniV2Theme.palette.loader,
            strokeWidth = 3.dp
        )
    }
}
