#!/usr/bin/env python3
"""
Build a versioned size report comparing:

  1) The release AAR for `:amani-sdk-v1` (ABI-agnostic).
  2) Per-ABI APK size of an inline "probe" app whose only dependency is
     `:amani-sdk-v1` — i.e. the UI SDK's standalone APK footprint.
  3) Per-ABI APK size of the `:app` sample (UI SDK + sample app code).

The probe APK shell has a small constant overhead (~ a few hundred KB:
empty Activity, Kotlin stdlib already required by the SDK, AndroidX core).
The interesting columns are the per-ABI numbers and the app overhead delta.

Inputs (env):
  AAR_PATH      path to release AAR
                (default: amani-sdk-v1/build/outputs/aar/amani-sdk-v1-release.aar)
  APP_APK_DIR   directory with `:app` per-ABI APKs
                (default: app/build/outputs/apk/release)
  PROBE_APK_DIR directory with probe per-ABI APKs
                (default: .ci-probe/build/outputs/apk/release)
  VERSION       label written into the report (default: "current")
  OUTPUT        optional markdown file to write the section to (also printed to stdout)
  JSON_OUTPUT   optional path to also emit a structured per-run measurement JSON
                (consumed by update_history.py to build size-latest.json /
                size-history.json for docs/API consumers)
"""
import json
import os
import sys
import zipfile
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


def main() -> int:
    aar_path = os.environ.get(
        "AAR_PATH", "amani-sdk-v1/build/outputs/aar/amani-sdk-v1-release.aar"
    )
    app_dir = os.environ.get("APP_APK_DIR", "app/build/outputs/apk/release")
    probe_dir = os.environ.get("PROBE_APK_DIR", ".ci-probe/build/outputs/apk/release")
    version = os.environ.get("VERSION", "current")
    output = os.environ.get("OUTPUT")
    json_output = os.environ.get("JSON_OUTPUT")

    app_apks = discover_apks(app_dir, "app")
    probe_apks = discover_apks(probe_dir, "probe")

    if not app_apks:
        print(f"::error::no :app APKs found in {app_dir}", file=sys.stderr)
        return 1
    if not probe_apks:
        print(f"::error::no probe APKs found in {probe_dir}", file=sys.stderr)
        return 1

    aar_size = os.path.getsize(aar_path) if os.path.isfile(aar_path) else 0
    date = datetime.now(timezone.utc).strftime("%Y-%m-%d")

    lines = []
    lines.append(f"## {version} — {date}")
    lines.append("")
    if aar_size:
        lines.append(
            f"**AAR (`amani-sdk-v1-release.aar`):** {fmt_mb(aar_size)} "
            f"(`{aar_size:,}` bytes)"
        )
        lines.append("")
    lines.append(
        "**Per-ABI APK breakdown** — *UI SDK in APK* is measured by an inline "
        "probe app whose only dependency is `:amani-sdk-v1`; *Full APK* is the "
        "`:app` sample which adds ~600 KB of sample-app code + AndroidX UI deps."
    )
    lines.append("")
    lines.append("| ABI | UI SDK in APK | Full APK (UI SDK + app) | App overhead |")
    lines.append("|-----|--------------:|------------------------:|-------------:|")

    abis = [a for a in ABI_ORDER if a in app_apks and a in probe_apks]
    for a in app_apks:
        if a not in abis and a in probe_apks:
            abis.append(a)

    per_abi: dict[str, dict[str, int]] = {}
    for abi in abis:
        full = os.path.getsize(app_apks[abi])
        sdk = os.path.getsize(probe_apks[abi])
        overhead = full - sdk
        lines.append(
            f"| {abi} | {fmt_mb(sdk)} | {fmt_mb(full)} | {fmt_signed_mb(overhead)} |"
        )
        per_abi[abi] = {
            "uiSdkBytes": sdk,
            "fullApkBytes": full,
            "appOverheadBytes": overhead,
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
