#!/usr/bin/env python3
"""
Build a versioned size report of the Core SDK, the UI SDK and the UI SDK's
optional modules, as their net per-ABI cost in a probe APK.

The probe module (see .github/scripts/probe.sh) builds five flavors:
  bare       — AndroidX/Material baseline only
  withcore   — baseline + Core SDK (`ai.amani.android:AmaniAi`)
  withsdk    — baseline + UI SDK (`:amani-sdk-v1`, which transitively pulls Core)
  withspeech — withsdk + Speech Verifier (optional, compileOnly in the UI SDK)
  withvoice  — withsdk + Voice Assistant (optional, compileOnly in the UI SDK)

Contributions:
  Core SDK          = withcore   − bare
  UI SDK (own)      = withsdk    − withcore   (UI's marginal cost on top of Core)
  UI SDK incl. Core = withsdk    − bare       (what depending on the UI SDK costs)
  Speech Verifier   = withspeech − withsdk
  Voice Assistant   = withvoice  − withsdk

The UI SDK bundles neither optional module, so they are reported in a table of
their own: an integrator pays for one only by adding it to the app. The release
AAR (UI SDK) is captured as the ABI-agnostic baseline for the trend table in
SIZE_HISTORY.md.

Inputs (env):
  AAR_PATH             path to release AAR
                       (default: amani-sdk-v1/build/outputs/aar/amani-sdk-v1-release.aar)
  PROBE_BARE_APK_DIR   directory with bare-flavor probe APKs
                       (default: .ci-probe/build/outputs/apk/bare/release)
  PROBE_CORE_APK_DIR   directory with withcore-flavor probe APKs
                       (default: .ci-probe/build/outputs/apk/withcore/release)
  PROBE_SDK_APK_DIR    directory with withsdk-flavor probe APKs
                       (default: .ci-probe/build/outputs/apk/withsdk/release)
  PROBE_SPEECH_APK_DIR directory with withspeech-flavor probe APKs
                       (default: .ci-probe/build/outputs/apk/withspeech/release)
  PROBE_VOICE_APK_DIR  directory with withvoice-flavor probe APKs
                       (default: .ci-probe/build/outputs/apk/withvoice/release)
                       Without the optional-module APKs the report leaves their
                       table out instead of failing.
  VERSION              label written into the report (default: "current")
  OUTPUT               optional markdown file to write the section to (also printed to stdout)
  JSON_OUTPUT          optional path to also emit a structured per-run measurement JSON
                       (consumed by update_history.py to build size-latest.json /
                       size-history.json for docs/API consumers)
"""
import json
import os
import re
import sys
from datetime import datetime, timezone

ABI_ORDER = ["arm64-v8a", "armeabi-v7a", "x86", "x86_64", "universal"]


def discover_apks(d: str, prefix: str) -> dict[str, str]:
    if not os.path.isdir(d):
        return {}
    found = {}
    for fname in sorted(os.listdir(d)):
        if not (fname.endswith(".apk") and fname.startswith(prefix + "-")):
            continue
        # <prefix>-<abi>-release-unsigned.apk  → abi
        stem = fname[len(prefix) + 1 :].rsplit("-release", 1)[0]
        found[stem] = os.path.join(d, fname)
    return found


def fmt_mb(b: int) -> str:
    return f"{b / 1048576:.2f} MB"


def fmt_signed_mb(b: int) -> str:
    sign = "+" if b >= 0 else "−"
    return f"{sign}{abs(b) / 1048576:.2f} MB"


def read_sdk_version(pattern: str) -> str:
    """First match of [pattern]'s group in amani-sdk-v1/build.gradle, or "".

    Mirrors the logic in probe.sh so each column is labelled with the version
    actually measured.
    """
    try:
        with open("amani-sdk-v1/build.gradle") as f:
            for line in f:
                m = re.match(pattern, line)
                if m:
                    return m.group(1)
    except OSError:
        pass
    return ""


def read_core_sdk_version() -> str:
    """`def amani_sdk = "X.Y.Z"`"""
    return read_sdk_version(r'\s*def\s+amani_sdk\s*=\s*[\'"]([^\'"]+)[\'"]')


def read_speech_verifier_version() -> str:
    """`def speech_verifier = "X.Y.Z"`"""
    return read_sdk_version(r'\s*def\s+speech_verifier\s*=\s*[\'"]([^\'"]+)[\'"]')


def read_voice_assistant_version() -> str:
    """`compileOnly 'ai.amani:Android.SDK.AmaniVoiceAssistant:X.Y.Z'`"""
    return read_sdk_version(
        r'\s*compileOnly\s+[\'"]ai\.amani:Android\.SDK\.AmaniVoiceAssistant:([^\'"]+)[\'"]'
    )


def labelled(name: str, version: str) -> str:
    return f"{name} {version}" if version else name


def table_header(columns: list[str]) -> list[str]:
    """Header and right-aligned separator rows of a markdown table led by an ABI column."""
    return [
        "| ABI | " + " | ".join(columns) + " |",
        "|-----|" + "|".join("-" * (len(c) + 1) + ":" for c in columns) + "|",
    ]


def main() -> int:
    aar_path = os.environ.get(
        "AAR_PATH", "amani-sdk-v1/build/outputs/aar/amani-sdk-v1-release.aar"
    )
    bare_dir = os.environ.get(
        "PROBE_BARE_APK_DIR", ".ci-probe/build/outputs/apk/bare/release"
    )
    core_dir = os.environ.get(
        "PROBE_CORE_APK_DIR", ".ci-probe/build/outputs/apk/withcore/release"
    )
    sdk_dir = os.environ.get(
        "PROBE_SDK_APK_DIR", ".ci-probe/build/outputs/apk/withsdk/release"
    )
    speech_dir = os.environ.get(
        "PROBE_SPEECH_APK_DIR", ".ci-probe/build/outputs/apk/withspeech/release"
    )
    voice_dir = os.environ.get(
        "PROBE_VOICE_APK_DIR", ".ci-probe/build/outputs/apk/withvoice/release"
    )
    version = os.environ.get("VERSION", "current")
    output = os.environ.get("OUTPUT")
    json_output = os.environ.get("JSON_OUTPUT")

    bare_apks = discover_apks(bare_dir, "probe-bare")
    core_apks = discover_apks(core_dir, "probe-withcore")
    sdk_apks = discover_apks(sdk_dir, "probe-withsdk")
    speech_apks = discover_apks(speech_dir, "probe-withspeech")
    voice_apks = discover_apks(voice_dir, "probe-withvoice")

    if not bare_apks:
        print(f"::error::no bare-flavor probe APKs found in {bare_dir}", file=sys.stderr)
        return 1
    if not core_apks:
        print(f"::error::no withcore-flavor probe APKs found in {core_dir}", file=sys.stderr)
        return 1
    if not sdk_apks:
        print(f"::error::no withsdk-flavor probe APKs found in {sdk_dir}", file=sys.stderr)
        return 1

    aar_size = os.path.getsize(aar_path) if os.path.isfile(aar_path) else 0
    date = datetime.now(timezone.utc).strftime("%Y-%m-%d")

    lines = []
    lines.append(f"## {version} — {date}")
    lines.append("")
    if aar_size:
        lines.append("### UI SDK AAR size")
        lines.append("")
        lines.append("| Version | AAR | Δ vs prev | Date (UTC) |")
        lines.append("|---------|----:|----------:|------------|")
        lines.append(
            f"| {version} <!-- bytes={aar_size} --> | {fmt_mb(aar_size)} "
            f"| — | {date} |"
        )
        lines.append("")
    core_version = read_core_sdk_version()
    speech_version = read_speech_verifier_version()
    voice_version = read_voice_assistant_version()
    core_label = labelled("Core SDK", core_version)

    abis = [
        a for a in ABI_ORDER if a in bare_apks and a in core_apks and a in sdk_apks
    ]
    for a in sdk_apks:
        if a not in abis and a in bare_apks and a in core_apks:
            abis.append(a)

    lines.append("### Core & UI SDK impact on a probe APK")
    lines.append("")
    lines.extend(
        table_header(["Total APK", core_label, "UI SDK (own)", "UI SDK incl. Core"])
    )

    per_abi: dict[str, dict] = {}
    for abi in abis:
        bare = os.path.getsize(bare_apks[abi])
        withcore = os.path.getsize(core_apks[abi])
        withsdk = os.path.getsize(sdk_apks[abi])
        core_contrib = withcore - bare
        ui_own = withsdk - withcore
        ui_contrib = withsdk - bare
        lines.append(
            f"| {abi} | {fmt_mb(withsdk)} | {fmt_mb(core_contrib)} "
            f"| {fmt_mb(ui_own)} | {fmt_mb(ui_contrib)} |"
        )
        per_abi[abi] = {
            "bareBytes": bare,
            "withCoreBytes": withcore,
            "withSdkBytes": withsdk,
            "coreSdkContributionBytes": core_contrib,
            "uiSdkContributionBytes": ui_contrib,
            "uiSdkOwnBytes": ui_own,
        }
    lines.append("")

    optional_abis = [a for a in abis if a in speech_apks and a in voice_apks]
    if optional_abis:
        speech_label = labelled("Speech Verifier", speech_version)
        voice_label = labelled("Voice Assistant", voice_version)
        lines.append("### Optional modules on top of the UI SDK")
        lines.append("")
        lines.append(
            "> The UI SDK does not bundle these: an app pays for one only by adding it as a "
            "dependency. They share some libraries, so with both added the total is slightly "
            "below the sum."
        )
        lines.append("")
        lines.extend(table_header([speech_label, voice_label]))
        for abi in optional_abis:
            withsdk = per_abi[abi]["withSdkBytes"]
            withspeech = os.path.getsize(speech_apks[abi])
            withvoice = os.path.getsize(voice_apks[abi])
            speech_contrib = withspeech - withsdk
            voice_contrib = withvoice - withsdk
            lines.append(f"| {abi} | {fmt_mb(speech_contrib)} | {fmt_mb(voice_contrib)} |")
            per_abi[abi]["optionalModules"] = {
                "speechVerifier": {
                    "withModuleBytes": withspeech,
                    "contributionBytes": speech_contrib,
                },
                "voiceAssistant": {
                    "withModuleBytes": withvoice,
                    "contributionBytes": voice_contrib,
                },
            }
        lines.append("")
    else:
        print(
            "::warning::no optional-module probe APKs found, their table is left out",
            file=sys.stderr,
        )

    report = "\n".join(lines) + "\n"

    if output:
        with open(output, "w") as f:
            f.write(report)
        print(f"Report written to {output}", file=sys.stderr)

    if json_output:
        payload = {
            "schemaVersion": 1,
            "version": version,
            "date": date,
            "aar": {"bytes": aar_size},
            "versions": {
                "coreSdk": core_version or None,
                "speechVerifier": speech_version or None,
                "voiceAssistant": voice_version or None,
            },
            "perAbi": per_abi,
        }
        with open(json_output, "w") as f:
            json.dump(payload, f, indent=2)
            f.write("\n")
        print(f"Measurement JSON written to {json_output}", file=sys.stderr)

    print(report, end="")
    return 0


if __name__ == "__main__":
    sys.exit(main())
