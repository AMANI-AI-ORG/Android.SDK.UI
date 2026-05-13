#!/usr/bin/env python3
"""
Build a versioned size report comparing the Core SDK and UI SDK's net per-ABI
APK cost.

Three release APK variants of the inline probe app are measured side-by-side:

  bare     — AndroidX/Material baseline only
  withcore — baseline + Core SDK (`ai.amani.android:AmaniAi`)
  withsdk  — baseline + UI SDK (`:amani-sdk-v1`, which transitively pulls Core)

Per-ABI deltas:
  Core SDK contribution = withcore − bare
  UI SDK contribution   = withsdk  − withcore   (UI's marginal cost on top of Core)

This isolates each SDK layer's real impact from the unavoidable Android/Kotlin
+ AndroidX shell. The release AAR (UI SDK) is captured as the ABI-agnostic
baseline for the trend table in SIZE_HISTORY.md.

Inputs (env):
  AAR_PATH             path to release AAR
                       (default: amani-sdk-v1/build/outputs/aar/amani-sdk-v1-release.aar)
  PROBE_BARE_APK_DIR   directory with bare-flavor probe APKs
                       (default: .ci-probe/build/outputs/apk/bare/release)
  PROBE_CORE_APK_DIR   directory with withcore-flavor probe APKs
                       (default: .ci-probe/build/outputs/apk/withcore/release)
  PROBE_SDK_APK_DIR    directory with withsdk-flavor probe APKs
                       (default: .ci-probe/build/outputs/apk/withsdk/release)
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


def read_core_sdk_version() -> str:
    """Extract `def amani_sdk = "X.Y.Z"` from amani-sdk-v1/build.gradle.

    Mirrors the logic in probe.sh so the table can label the Core SDK column
    with the version actually measured. Returns "" if not found.
    """
    try:
        with open("amani-sdk-v1/build.gradle") as f:
            for line in f:
                m = re.match(r'\s*def\s+amani_sdk\s*=\s*[\'"]([^\'"]+)[\'"]', line)
                if m:
                    return m.group(1)
    except OSError:
        pass
    return ""


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
    version = os.environ.get("VERSION", "current")
    output = os.environ.get("OUTPUT")
    json_output = os.environ.get("JSON_OUTPUT")

    bare_apks = discover_apks(bare_dir, "probe-bare")
    core_apks = discover_apks(core_dir, "probe-withcore")
    sdk_apks = discover_apks(sdk_dir, "probe-withsdk")

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
        lines.append(
            f"**AAR size:** {fmt_mb(aar_size)} (`{aar_size:,}` bytes) — "
            "raw `:amani-sdk-v1-release.aar`."
        )
        lines.append("")
    lines.append(
        "**SDK size impact on release APK** — measured by building a probe app "
        "with the same AndroidX/Material baseline as `:app`, with and without "
        "each SDK. The *UI SDK* column includes Core SDK (UI transitively "
        "depends on Core)."
    )
    lines.append("")
    lines.append(
        "> ⚠️ APK figures are approximate (~0.1–0.5 MB run-to-run variance). "
        "The AAR row above is stable."
    )
    lines.append("")
    core_version = read_core_sdk_version()
    core_label = f"Core SDK {core_version}" if core_version else "Core SDK"
    core_sep = "-" * len(core_label)
    lines.append(
        f"| ABI | Total APK | UI SDK (incl. Core) | {core_label} |"
    )
    lines.append(
        f"|-----|----------:|--------------------:|{core_sep}:|"
    )

    abis = [
        a for a in ABI_ORDER if a in bare_apks and a in core_apks and a in sdk_apks
    ]
    for a in sdk_apks:
        if a not in abis and a in bare_apks and a in core_apks:
            abis.append(a)

    per_abi: dict[str, dict[str, int]] = {}
    for abi in abis:
        bare = os.path.getsize(bare_apks[abi])
        withcore = os.path.getsize(core_apks[abi])
        withsdk = os.path.getsize(sdk_apks[abi])
        core_contrib = withcore - bare
        ui_contrib = withsdk - bare
        lines.append(
            f"| {abi} | {fmt_mb(withsdk)} | {fmt_mb(ui_contrib)} "
            f"| {fmt_mb(core_contrib)} |"
        )
        per_abi[abi] = {
            "bareBytes": bare,
            "withCoreBytes": withcore,
            "withSdkBytes": withsdk,
            "coreSdkContributionBytes": core_contrib,
            "uiSdkContributionBytes": ui_contrib,
        }

    lines.append("")
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
