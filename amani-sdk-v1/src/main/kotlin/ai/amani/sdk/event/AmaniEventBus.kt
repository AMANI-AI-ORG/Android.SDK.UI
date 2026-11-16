package ai.amani.sdk.event

import ai.amani.sdk.Amani
import ai.amani.sdk.interfaces.AmaniEventCallBack
import ai.amani.sdk.model.amani_events.error.AmaniError
import ai.amani.sdk.model.amani_events.profile_status.ProfileStatus
import ai.amani.sdk.model.amani_events.steps_result.StepsResult
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Single owner of the core SDK's AmaniEvent listener, fanning every event out to all
 * subscribers of both the V1 and the V2 flows.
 *
 * The core keeps exactly ONE listener (`AmaniEvent().setListener` replaces the previous one),
 * so screens registering their own silently stole events from each other: a HomeKYC view model
 * created after a pre-KYC screen (a second activity instance, a recomposition) took the socket
 * over, and that screen then waited forever for a verdict that was delivered elsewhere — the
 * profile-info screen hanging on "Continue".
 *
 * Every screen [subscribe]s here instead; the core listener is installed once, on first
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

    /** True once the bus listener is installed on the core SDK. */
    @Volatile
    private var installed = false

    /**
     * Installs the bus listener on the core SDK, once. Later calls do nothing: every
     * `setListener` makes the core forget what it delivered and read the profile again, and a
     * read started just before an upload answers with the pre-upload NOT_UPLOADED after the
     * upload began (the step's loader flickered off). Screens may call this on every resume.
     */
    @Synchronized
    fun attach() {
        if (installed) return
        install()
    }

    /**
     * Re-installs the bus listener even if it is installed. The host calls this after login,
     * because the core re-creates its event holder there and would otherwise drop the listener
     * installed before it, leaving every step spinning on a verdict that never arrives.
     */
    @Synchronized
    fun reattach() {
        install()
    }

    private fun install() {
        installed = true
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
