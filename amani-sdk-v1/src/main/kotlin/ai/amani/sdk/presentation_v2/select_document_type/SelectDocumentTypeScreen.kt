package ai.amani.sdk.presentation_v2.select_document_type

import ai.amani.sdk.presentation_v2.components.DotStep
import ai.amani.sdk.presentation_v2.components.Pill
import ai.amani.sdk.presentation_v2.components.PrimaryButton
import ai.amani.sdk.presentation_v2.components.ScreenHeader
import ai.amani.sdk.presentation_v2.components.StepStatus
import ai.amani.sdk.presentation_v2.theme.AmaniV2Dimens
import ai.amani.sdk.presentation_v2.theme.AmaniV2Theme
import ai.amani.sdk.presentation_v2.theme.AmaniV2Type
import ai.amani.sdk.presentation_v2.theme.amaniV2ContentMaxWidth
import ai.amani.sdk.presentation_v2.theme.scaled
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * A selectable document type. [id] is the stable key the caller maps back to the
 * server [datamanager.model.config.Version] in the wiring phase.
 */
data class DocumentTypeOption(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    // Chips shown on the *selected* card (design v2.6). Null NFC label = the document has
    // no NFC path, chip hidden. Config-driven: Version.v2NfcChipLabel / v2EstimatedTime.
    val nfcChipLabel: String? = null,
    val estimatedTime: String? = null
)

/** State backing [SelectDocumentTypeScreen]. */
data class SelectDocumentTypeUiState(
    val headerTitle: String,
    val dots: List<DotStep>,
    val eyebrow: String,
    val title: String,
    val subtitle: String,
    val options: List<DocumentTypeOption>,
    val selectedId: String?,
    // CTA composition (design v2.6): with a selection the button reads
    // "<continuePrefix> <selected title>"; without one it shows [placeholder] disabled.
    val continuePrefix: String = "Continue with",
    val placeholder: String = "Select an ID type to continue",
    val continueButtonText: String
)

/**
 * Document type selection screen. Stateless: receives a [SelectDocumentTypeUiState]
 * and emits intents via callbacks.
 */
@Composable
fun SelectDocumentTypeScreen(
    state: SelectDocumentTypeUiState,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onSelect: (DocumentTypeOption) -> Unit = {},
    onContinue: () -> Unit = {}
) {
    val palette = AmaniV2Theme.palette
    val contentMaxWidth = amaniV2ContentMaxWidth()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ScreenHeader(
            title = state.headerTitle,
            steps = state.dots,
            onBack = onBack
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .widthIn(max = contentMaxWidth)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AmaniV2Dimens.screenPadding)
        ) {
            Text(
                state.eyebrow.uppercase(),
                style = AmaniV2Type.eyebrow.scaled(),
                color = palette.accent
            )
            Spacer(Modifier.height(8.dp))
            Text(state.title, style = AmaniV2Type.heading.scaled(), color = palette.ink)
            Spacer(Modifier.height(8.dp))
            Text(state.subtitle, style = AmaniV2Type.body.scaled(), color = palette.inkMuted)
            Spacer(Modifier.height(24.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                state.options.forEach { option ->
                    DocumentOptionCard(
                        option = option,
                        selected = option.id == state.selectedId,
                        onClick = { onSelect(option) }
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = contentMaxWidth)
                // Clear the system navigation bar so the button isn't overlapped.
                .navigationBarsPadding()
                .padding(horizontal = AmaniV2Dimens.screenPadding)
                .padding(bottom = 20.dp)
        ) {
            // CTA names the selected document; without a selection it shows the config
            // placeholder ("Select an ID type to continue") and stays disabled.
            val selected = state.options.firstOrNull { it.id == state.selectedId }
            PrimaryButton(
                text = selected?.let { "${state.continuePrefix} ${it.title}" } ?: state.placeholder,
                enabled = selected != null,
                onClick = onContinue
            )
        }
    }
}

@Composable
private fun DocumentOptionCard(
    option: DocumentTypeOption,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = AmaniV2Theme.palette
    val containerColor = if (selected) palette.accentSofter else palette.surface
    val borderColor = if (selected) palette.accent else palette.border
    val borderWidth = if (selected) 1.5.dp else 0.5.dp
    val cardShape = RoundedCornerShape(AmaniV2Dimens.cardRadius.scaled())

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(containerColor, cardShape)
            .border(borderWidth, borderColor, cardShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp.scaled(), vertical = 14.dp.scaled())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp.scaled())
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp.scaled())
                    .background(
                        // Selected card → accent fill (white icon); unselected → soft accent
                        // tint (accent-colored icon).
                        if (selected) palette.accent else palette.accentSoft,
                        RoundedCornerShape(AmaniV2Dimens.iconButtonRadius.scaled())
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    option.icon,
                    contentDescription = null,
                    tint = if (selected) palette.surface else palette.accent,
                    modifier = Modifier.size(22.dp.scaled())
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    option.title,
                    style = AmaniV2Type.rowTitle.scaled(),
                    color = palette.ink
                )
                option.subtitle.takeIf { it.isNotBlank() }?.let { subtitle ->
                    Text(
                        subtitle,
                        style = AmaniV2Type.label.copy(fontWeight = FontWeight.Normal).scaled(),
                        color = palette.inkMuted,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            if (selected) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.size(22.dp.scaled())
                )
            } else {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = palette.inkLight,
                    modifier = Modifier.size(22.dp.scaled())
                )
            }
        }
        // Chips only expand on the selected card (design v2.6): the accent NFC pitch
        // ("Fastest with NFC") when the document supports it, plus the outlined estimated
        // duration ("~30 sec").
        if (selected && (option.nfcChipLabel != null || option.estimatedTime != null)) {
            Row(
                modifier = Modifier.padding(top = 12.dp.scaled()),
                horizontalArrangement = Arrangement.spacedBy(8.dp.scaled())
            ) {
                option.nfcChipLabel?.let { label ->
                    Pill(
                        text = label,
                        containerColor = palette.accent,
                        contentColor = palette.surface,
                        leadingIcon = Icons.Filled.Bolt
                    )
                }
                option.estimatedTime?.let { time ->
                    Pill(
                        text = time,
                        containerColor = palette.surface,
                        contentColor = palette.ink,
                        borderColor = palette.border
                    )
                }
            }
        }
    }
}

// region Sample state (also used by previews)

internal val SampleSelectDocumentType = SelectDocumentTypeUiState(
    headerTitle = "Verification",
    dots = listOf(
        DotStep("ID", StepStatus.Current),
        DotStep("Selfie", StepStatus.Pending),
        DotStep("Address", StepStatus.Pending)
    ),
    eyebrow = "Step 1 of 3",
    title = "Which document will you use?",
    subtitle = "Choose a government-issued ID. Make sure it's valid and not expired.",
    options = listOf(
        DocumentTypeOption(
            id = "id_card",
            title = "ID Card",
            subtitle = "National identity card",
            icon = Icons.Outlined.Badge,
            nfcChipLabel = "Fastest with NFC",
            estimatedTime = "~30 sec"
        ),
        DocumentTypeOption(
            id = "passport",
            title = "Passport",
            subtitle = "Photo page with chip",
            icon = Icons.AutoMirrored.Outlined.MenuBook
        ),
        DocumentTypeOption(
            id = "driver_license",
            title = "Driver's license",
            subtitle = "Front and back",
            icon = Icons.Outlined.DirectionsCar
        ),
        DocumentTypeOption(
            id = "residence_permit",
            title = "Residence permit",
            subtitle = "Photo side",
            icon = Icons.Outlined.CreditCard
        )
    ),
    selectedId = "id_card",
    continueButtonText = "Continue"
)

@Preview(name = "SelectDocumentType — selected", showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun PreviewSelectDocumentTypeSelected() {
    AmaniV2Theme { SelectDocumentTypeScreen(state = SampleSelectDocumentType) }
}

@Preview(name = "SelectDocumentType — none", showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun PreviewSelectDocumentTypeNone() {
    AmaniV2Theme { SelectDocumentTypeScreen(state = SampleSelectDocumentType.copy(selectedId = null)) }
}
