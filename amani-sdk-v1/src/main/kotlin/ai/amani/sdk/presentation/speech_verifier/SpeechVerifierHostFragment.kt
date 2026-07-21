package ai.amani.sdk.presentation.speech_verifier

import ai.amani.amani_sdk.R
import ai.amani.sdk.model.ConfigModel
import ai.amani.sdk.presentation_v2.speech_verify.SpeechVerifierLauncher
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import timber.log.Timber

/**
 * Nav-graph host for the OPTIONAL AmaniSpeechVerifier module (document id "ST").
 *
 * Reached through the NavController like every other capture screen (safe-args
 * [ConfigModel]); it embeds the module's own fragment as a CHILD fragment inside its
 * container ([R.id.speech_verifier_container]) via [getChildFragmentManager], keeping the
 * module inside the normal V1 fragment tree instead of committing it over the activity.
 *
 * On success the recorded session is uploaded through `SpeechVerifier.upload`; the step
 * verdict then arrives over the AmaniEvent socket (HomeKYCViewModel.listenAmaniEvents →
 * Refresh / congratulations), exactly like the other legs. Any terminal error pops back to
 * Home and surfaces a toast.
 *
 * This fragment is only reached once the module is confirmed present (HomeKYCFragment guards
 * with SpeechVerifierAvailability before navigating), so referencing [SpeechVerifierLauncher]
 * here cannot fail for a correctly-configured app.
 */
class SpeechVerifierHostFragment : Fragment() {

    private val args: SpeechVerifierHostFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_speech_verifier_host, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Centered spinner shown while the module runs a blocking preparation step
        // (onPreparing) — e.g. the identity-question profile fetch — hidden on onReady.
        val loading = view.findViewById<ProgressBar>(R.id.speech_loading)
        // Tint it with the config loader color when provided, so it stays on brand.
        runCatching { Color.parseColor(args.dataModel.generalConfigs?.loaderColor) }
            .getOrNull()
            ?.let { loading.indeterminateTintList = ColorStateList.valueOf(it) }

        // Add the module's fragment once; on recreation the child manager restores it.
        if (savedInstanceState != null) return

        val version = args.dataModel.version ?: run {
            Timber.e("V1 speech: version is null, cannot start")
            popBack()
            return
        }

        // The whole flow — steps (spoken/identity), thresholds, texts, colors, prompts — is
        // driven by the server config carried on this version (see SpeechVerifierLauncher).
        val child = SpeechVerifierLauncher.buildFragment(
            version = version,
            onPreparing = { requireActivity().runOnUiThread { loading.visibility = View.VISIBLE } },
            onReady = { requireActivity().runOnUiThread { loading.visibility = View.GONE } },
            onSuccess = { requireActivity().runOnUiThread { onVerified() } },
            onFailure = { reason, attempt ->
                Timber.d("V1 speech: attempt $attempt failed ($reason)")
            },
            onError = { message ->
                requireActivity().runOnUiThread { finishWithError(message) }
            }
        )

        childFragmentManager.beginTransaction()
            .replace(R.id.speech_verifier_container, child)
            .commit()
    }

    /**
     * Verification passed → fire the upload and return to Home immediately (the same
     * navigate-home-then-upload hand-off the other legs use). The upload runs on the module's
     * own scope against the application context, so it completes even though this fragment is
     * gone; the step verdict then arrives over the AmaniEvent socket
     * (HomeKYCViewModel.listenAmaniEvents → Refresh / congratulations). Upload callbacks are
     * logged only — there is no fragment left to update.
     */
    private fun onVerified() {
        SpeechVerifierLauncher.upload(
            context = requireContext().applicationContext,
            onResult = { status, _ -> Timber.d("V1 speech upload result: $status") },
            onError = { code, message -> Timber.e("V1 speech upload error code=$code msg=$message") }
        )
        popBack()
    }

    private fun finishWithError(message: String) {
        context?.let { Toast.makeText(it, message, Toast.LENGTH_LONG).show() }
        popBack()
    }

    private fun popBack() {
        if (isAdded) findNavController().popBackStack()
    }
}
