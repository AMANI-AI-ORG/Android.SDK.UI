# ─────────────────────────────────────────────────────────────────────────────
# Consumer ProGuard/R8 rules — applied in the INTEGRATOR app's build when it runs
# minify. The UI SDK ships unobfuscated, so nothing here hides code: these keeps cover
# what the platform and the SDK resolve BY NAME, which R8 in the host app would
# otherwise rename or strip.
# ─────────────────────────────────────────────────────────────────────────────

# ── Public entry point. AmaniSDKUI has no package declaration — it is a top-level
#    object integrators call by name from both Java and Kotlin. ──
-keep class AmaniSDKUI { *; }

# ── The SDK and the Core SDK it wraps: activities named in the manifest, @Parcelize
#    models crossing Intent/Bundle boundaries, and the config/network model layers whose
#    Gson-style (de)serialisation maps JSON onto FIELD NAMES. Renaming any of them breaks
#    at runtime, not at build time, so the whole tree is kept. ──
-keep class ai.amani.** { *; }
-dontwarn ai.amani.**
-keep class datamanager.** { *; }
-dontwarn datamanager.**
-keep class networkmanager.** { *; }
-dontwarn networkmanager.**

# ── Fragments in the v1 flow are recreated by the FragmentManager after a configuration
#    change or process death, which needs the class name and a no-arg constructor. ──
-keep class * extends androidx.fragment.app.Fragment { public <init>(...); }

# ── Parcelable CREATOR is read reflectively when a bundle is unmarshalled. ──
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# ── Optional speech-verification module: a compileOnly dependency the SDK probes with
#    Class.forName("ai.amani.speechverifier.SpeechVerifier"). The literal name must keep
#    matching in apps that ship the artifact, and must not warn in apps that do not.
#    (The -keep above already covers the ai.amani tree; this is the -dontwarn half.) ──
-dontwarn ai.amani.speechverifier.**

# ── Navigation safe-args: argument classes are looked up by name from the destination. ──
-keepnames class * extends androidx.navigation.NavArgs
-keepclassmembers class * implements androidx.navigation.NavArgs {
    public static *** fromBundle(android.os.Bundle);
}
