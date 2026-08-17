package ai.amani.sdk.presentation_v2

import ai.amani.sdk.Amani
import ai.amani.sdk.interfaces.AmaniEventCallBack
import ai.amani.sdk.model.amani_events.error.AmaniError
import ai.amani.sdk.model.amani_events.profile_status.ProfileStatus
import ai.amani.sdk.model.amani_events.steps_result.StepsResult
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Single owner of the core SDK's AmaniEvent listener, fanning every event out to all V2
 * subscribers.
 *
 * The core keeps exactly ONE listener (`AmaniEvent().setListener` replaces the previous one),
 * so screens registering their own silently stole events from each other: a HomeKYC view model
 * created after a pre-KYC screen (a second activity instance, a recomposition) took the socket
 * over, and that screen then waited forever for a verdict that was delivered elsewhere — the
 * profile-info screen hanging on "Continue".
 *
 * Every V2 screen [subscribe]s here instead; the core listener is installed once, on first
 * subscription, and stays installed.
 */
internal object AmaniEventBus {

    interface Subscriber {
        fun onStepsResult(stepsResult: StepsResult?) {}
        fun onProfileStatus(profileStatus: ProfileStatus) {}
        fun onError(type: String?, errors: ArrayList<AmaniError?>?) {}
    }

    private val subscribers = CopyOnWriteArrayList<Subscriber>()

    /**
     * Registers [subscriber] for every AmaniEvent until [unsubscribe]. Returns the same
     * subscriber so callers can keep the handle for removal.
     */
    fun subscribe(subscriber: Subscriber): Subscriber {
        attach()
        subscribers.addIfAbsent(subscriber)
        return subscriber
    }

    fun unsubscribe(subscriber: Subscriber?) {
        subscriber?.let { subscribers.remove(it) }
    }

    /**
     * (Re)installs the bus listener on the core SDK. Safe to call repeatedly: subscribers are
     * untouched, only the core-side registration is refreshed. The host calls this after login,
     * because the core re-creates its event holder there and would otherwise drop the listener
     * installed before it — leaving every step spinning on a verdict that never arrives.
     */
    @Synchronized
    fun attach() {
        Amani.sharedInstance().AmaniEvent().setListener(object : AmaniEventCallBack {
            override fun onError(type: String?, error: ArrayList<AmaniError?>?) {
                subscribers.forEach { it.onError(type, error) }
            }

            override fun profileStatus(profileStatus: ProfileStatus) {
                subscribers.forEach { it.onProfileStatus(profileStatus) }
            }

            override fun stepsResult(stepsResult: StepsResult?) {
                subscribers.forEach { it.onStepsResult(stepsResult) }
            }
        })
    }
}
