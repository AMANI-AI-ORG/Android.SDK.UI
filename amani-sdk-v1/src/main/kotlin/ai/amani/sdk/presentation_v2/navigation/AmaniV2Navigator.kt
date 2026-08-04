package ai.amani.sdk.presentation_v2.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/**
 * Single source of truth for the V2 KYC back stack.
 *
 * The stack is a Compose snapshot list, so any read of [current] / [canNavigateBack]
 * recomposes when the stack mutates. State is hoisted: screens never navigate
 * themselves — they emit intents and the host calls these methods, keeping the flow
 * unidirectional. Created via [rememberAmaniV2Navigator] so the stack survives
 * configuration changes and process death.
 */
@Stable
class AmaniV2Navigator internal constructor(
    initialBackStack: List<AmaniV2Destination>
) {
    private val backStack = mutableStateListOf<AmaniV2Destination>().apply {
        addAll(initialBackStack)
    }

    /** Destination currently on top of the stack. */
    val current: AmaniV2Destination get() = backStack.last()

    /** True when there is a previous destination to return to. */
    val canNavigateBack: Boolean get() = backStack.size > 1

    /** Current stack depth — lets the host pick a forward vs. back transition. */
    val depth: Int get() = backStack.size

    /**
     * Direction of the most recent stack move: `true` for a forward push, `false` for a
     * back pop. The host reads this to choose the screen-transition animation. Tracked
     * explicitly (rather than diffing depth inside the transition spec, which is racy)
     * so back navigation always animates as "back". Transient UI state — not persisted.
     */
    var isMovingForward by mutableStateOf(true)
        private set

    /** Push a new destination onto the stack. */
    fun navigateTo(destination: AmaniV2Destination) {
        isMovingForward = true
        backStack.add(destination)
    }

    /** Replace the current destination without growing the stack (e.g. start -> approved). */
    fun replaceCurrent(destination: AmaniV2Destination) {
        isMovingForward = true
        backStack[backStack.lastIndex] = destination
    }

    /** Pop to the root, dropping everything above the first destination. */
    fun popToRoot() {
        isMovingForward = false
        while (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }

    /**
     * Pop the top destination. Returns false when already at the root, letting the
     * caller decide what "back" means there (typically exit the activity).
     */
    fun popBackStack(): Boolean {
        if (!canNavigateBack) return false
        isMovingForward = false
        backStack.removeAt(backStack.lastIndex)
        return true
    }

    internal fun snapshot(): List<AmaniV2Destination> = backStack.toList()

    companion object {
        val Saver: Saver<AmaniV2Navigator, *> = listSaver(
            save = { it.snapshot() },
            restore = { AmaniV2Navigator(it) }
        )
    }
}

/**
 * Remembers an [AmaniV2Navigator] whose back stack survives configuration changes
 * and process death (the destinations are Parcelable, so they round-trip through
 * the saved instance state).
 *
 * The initial stack is seeded by [PreKycFlow.initialBackStack]: normally just the
 * [startDestination], but when a questionnaire is pending before KYC (v1's before-KYC
 * routing) it starts on the questionnaire with Home underneath. Decided once here (the
 * factory runs a single time and is then restored from saved state), so completing the
 * questionnaire — which pops back to Home — never re-triggers it.
 */
@Composable
fun rememberAmaniV2Navigator(
    startDestination: AmaniV2Destination = AmaniV2Destination.HomeKYC
): AmaniV2Navigator = rememberSaveable(saver = AmaniV2Navigator.Saver) {
    AmaniV2Navigator(PreKycFlow.initialBackStack(startDestination))
}
