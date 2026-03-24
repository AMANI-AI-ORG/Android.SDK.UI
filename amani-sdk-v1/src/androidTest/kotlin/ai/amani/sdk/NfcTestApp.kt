package ai.amani.sdk

import android.app.Application

/**
 * Minimal [Application] stub used by the library's androidTest APK.
 *
 * The main manifest of amani-sdk-v1 declares android:name="ai.amani.sample.App",
 * a class that belongs to the :app module and is therefore absent from the
 * library's standalone test APK.  The androidTest manifest below overrides
 * android:name to point to this stub, preventing the ClassNotFoundException
 * crash on test startup.
 */
class NfcTestApp : Application()
