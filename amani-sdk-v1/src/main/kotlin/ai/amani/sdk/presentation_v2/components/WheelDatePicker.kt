package ai.amani.sdk.presentation_v2.components

import ai.amani.sdk.presentation_v2.theme.AmaniV2Dimens
import ai.amani.sdk.presentation_v2.theme.AmaniV2Theme
import ai.amani.sdk.presentation_v2.theme.AmaniV2Type
import ai.amani.sdk.presentation_v2.theme.CappedCornerShape
import ai.amani.sdk.presentation_v2.theme.configCornerRadius
import ai.amani.sdk.presentation_v2.theme.scaled
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.drop
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.tooling.preview.Preview
import ai.amani.sdk.presentation_v2.theme.AmaniV2Palette
import androidx.compose.ui.window.Dialog
import java.util.Calendar
import kotlin.math.abs

/** A day/month/year the picker works with, free of any date library. */
data class WheelDate(val day: Int, val month: Int, val year: Int)

/**
 * iOS-style wheel date picker in a dialog: three snapping columns (day, month, year) under a
 * highlighted selection band, with the app's own palette and button styles.
 *
 * Android's platform picker is deliberately not used: this screen states the values in the
 * product's own typography and colors, and a system dialog would break that mid-flow.
 *
 * @param initial date the wheels open on
 * @param yearRange years the year wheel offers, oldest first
 * @param monthNames labels of the month wheel (12 entries)
 */
@Composable
fun WheelDatePickerDialog(
    title: String,
    initial: WheelDate,
    yearRange: IntProgression,
    confirmText: String,
    cancelText: String,
    onDismiss: () -> Unit,
    onConfirm: (WheelDate) -> Unit,
    monthNames: List<String> = DEFAULT_MONTHS
) {
    val palette = AmaniV2Theme.palette
    val years = remember(yearRange) { yearRange.toList() }

    // Keyed on the date the picker was opened with: opening it for another field (or after the
    // value changed) has to start on THAT value, not on whatever the previous opening left here.
    var selectedDay by remember(initial) { mutableStateOf(initial.day.coerceIn(1, 31)) }
    var selectedMonth by remember(initial) { mutableStateOf(initial.month.coerceIn(1, 12)) }
    var selectedYear by remember(initial) {
        mutableStateOf(initial.year.coerceIn(years.first(), years.last()))
    }

    // February and the 30-day months shorten the day wheel; a day past the end follows it down
    // (31 March → 30 April) instead of producing a date that does not exist.
    val daysInMonth = remember(selectedMonth, selectedYear) { daysInMonth(selectedMonth, selectedYear) }
    LaunchedEffect(daysInMonth) {
        if (selectedDay > daysInMonth) selectedDay = daysInMonth
    }

    Dialog(onDismissRequest = onDismiss) {
        WheelDatePickerSurface(
            title = title,
            years = years,
            monthNames = monthNames,
            selectedDay = selectedDay,
            selectedMonth = selectedMonth,
            selectedYear = selectedYear,
            daysInMonth = daysInMonth,
            confirmText = confirmText,
            cancelText = cancelText,
            onDaySelected = { selectedDay = it },
            onMonthSelected = { selectedMonth = it },
            onYearSelected = { selectedYear = it },
            onDismiss = onDismiss,
            onConfirm = { onConfirm(WheelDate(selectedDay, selectedMonth, selectedYear)) }
        )
    }
}

/**
 * The picker's visible surface, without the dialog window around it — the wheels, the selection
 * band and the two actions. Separate so it can be previewed; a dialog does not render in a
 * @Preview.
 */
@Composable
internal fun WheelDatePickerSurface(
    title: String,
    years: List<Int>,
    monthNames: List<String>,
    selectedDay: Int,
    selectedMonth: Int,
    selectedYear: Int,
    daysInMonth: Int,
    confirmText: String,
    cancelText: String,
    onDaySelected: (Int) -> Unit,
    onMonthSelected: (Int) -> Unit,
    onYearSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = AmaniV2Theme.palette
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CappedCornerShape(configCornerRadius()))
            .background(palette.surface)
            .padding(AmaniV2Dimens.screenPadding)
    ) {
        Text(
            title,
            style = AmaniV2Type.rowTitle.scaled().copy(fontWeight = FontWeight.SemiBold),
            color = palette.ink
        )

        Spacer(Modifier.height(16.dp))
        Box(contentAlignment = Alignment.Center) {
            // Selection band behind the wheels, the way an iOS picker marks its centre.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ITEM_HEIGHT)
                    .clip(RoundedCornerShape(10.dp))
                    .background(palette.accent.copy(alpha = 0.12f))
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                WheelColumn(
                    items = (1..daysInMonth).map { it.toString().padStart(2, '0') },
                    selectedIndex = selectedDay - 1,
                    modifier = Modifier.weight(1f),
                    onSelected = { onDaySelected(it + 1) }
                )
                WheelColumn(
                    items = monthNames,
                    selectedIndex = selectedMonth - 1,
                    modifier = Modifier.weight(1.4f),
                    onSelected = { onMonthSelected(it + 1) }
                )
                WheelColumn(
                    items = years.map { it.toString() },
                    selectedIndex = years.indexOf(selectedYear).coerceAtLeast(0),
                    modifier = Modifier.weight(1f),
                    onSelected = { onYearSelected(years[it]) }
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        SecondaryButton(text = cancelText, onClick = onDismiss)
        Spacer(Modifier.height(10.dp))
        PrimaryButton(text = confirmText, onClick = onConfirm)
    }
}

/**
 * One snapping wheel. The entry under the selection band is the selected one; the further an
 * entry sits from it the fainter it reads, which is what gives the wheel its depth.
 *
 * Which entry is "under the band" is read from the layout itself — the item whose centre is
 * closest to the viewport's centre — rather than inferred from the first visible index: with the
 * half-viewport content padding these two differ, which left the bold entry sitting a row below
 * the band and reported a stale value to the caller.
 */
@Composable
private fun WheelColumn(
    items: List<String>,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    onSelected: (Int) -> Unit
) {
    val palette = AmaniV2Theme.palette
    val state = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex.coerceAtLeast(0))
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = state)
    val currentOnSelected by rememberUpdatedState(onSelected)
    val haptics = LocalHapticFeedback.current

    val centeredIndex by remember {
        derivedStateOf {
            val layout = state.layoutInfo
            val viewportCentre = (layout.viewportStartOffset + layout.viewportEndOffset) / 2
            layout.visibleItemsInfo
                .minByOrNull { abs((it.offset + it.size / 2) - viewportCentre) }
                ?.index
                ?: selectedIndex.coerceAtLeast(0)
        }
    }

    // A tick under the finger every time another entry passes the band — the detent an iOS
    // wheel gives. Fired while scrolling, unlike the value report below.
    LaunchedEffect(state, items.size) {
        snapshotFlow { centeredIndex }
            .distinctUntilChanged()
            .drop(1) // the first emission is the wheel settling on its initial value
            .collect { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
    }

    // Report what the user landed on. Only once they are not mid-gesture: reporting during the
    // fling would fight the snap and make the value flicker between neighbours.
    LaunchedEffect(state, items.size) {
        snapshotFlow { centeredIndex to state.isScrollInProgress }
            .distinctUntilChanged()
            .collect { (index, scrolling) ->
                if (!scrolling && index in items.indices) currentOnSelected(index)
            }
    }

    // Follow a selection corrected from outside (a day clamped by a shorter month), never while
    // the user is scrolling.
    LaunchedEffect(selectedIndex, items.size) {
        if (selectedIndex in items.indices &&
            selectedIndex != centeredIndex &&
            !state.isScrollInProgress
        ) {
            state.scrollToItem(selectedIndex)
        }
    }

    LazyColumn(
        state = state,
        flingBehavior = flingBehavior,
        modifier = modifier.height(ITEM_HEIGHT * VISIBLE_ROWS),
        contentPadding = PaddingValues(vertical = ITEM_HEIGHT * (VISIBLE_ROWS / 2)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        itemsIndexed(items) { index, label ->
            val distance = abs(index - centeredIndex)
            val isSelected = distance == 0
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ITEM_HEIGHT)
                    .alpha(if (isSelected) 1f else (0.5f - distance * 0.12f).coerceAtLeast(0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    style = AmaniV2Type.rowTitle.scaled().copy(
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    ),
                    color = if (isSelected) palette.ink else palette.inkMuted,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

private fun daysInMonth(month: Int, year: Int): Int {
    val calendar = Calendar.getInstance()
    calendar.clear()
    calendar.set(Calendar.YEAR, year)
    calendar.set(Calendar.MONTH, month - 1)
    return calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
}

/** Height of one wheel entry; the selection band matches it. */
private val ITEM_HEIGHT = 44.dp

/** Rows the wheel shows at once — odd, so one of them sits in the middle. */
private const val VISIBLE_ROWS = 5

private val DEFAULT_MONTHS = listOf(
    "01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12"
)

@Preview(name = "Wheel date picker", showBackground = true, widthDp = 390, heightDp = 560)
@Composable
private fun WheelDatePickerPreview() {
    AmaniV2Theme(AmaniV2Palette()) {
        Box(
            modifier = Modifier
                .background(AmaniV2Theme.palette.background)
                .padding(AmaniV2Dimens.screenPadding)
        ) {
            WheelDatePickerSurface(
                title = "Date of birth",
                years = (1960..2010).toList(),
                monthNames = listOf(
                    "01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12"
                ),
                selectedDay = 23,
                selectedMonth = 4,
                selectedYear = 1997,
                daysInMonth = 30,
                confirmText = "Confirm",
                cancelText = "Cancel",
                onDaySelected = {},
                onMonthSelected = {},
                onYearSelected = {},
                onDismiss = {},
                onConfirm = {}
            )
        }
    }
}
