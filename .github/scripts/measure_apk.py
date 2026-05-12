#!/usr/bin/env python3
"""
Build a versioned size report comparing the UI SDK's net per-ABI APK cost.

Two release APK variants of the inline probe app are measured side-by-side:

  bare     — empty Android shell, NO SDK dependency      (baseline)
  withsdk  — same shell + `:amani-sdk-v1`                (with SDK)

Per-ABI SDK contribution is (withsdk − bare). This isolates the SDK's
real impact on an integrator's APK from the unavoidable Android/Kotlin
shell overhead.

The release AAR is also captured as an ABI-agnostic baseline for the
trend table in SIZE_HISTORY.md.

Inputs (env):
  AAR_PATH            path to release AAR
                      (default: amani-sdk-v1/build/outputs/aar/amani-sdk-v1-release.aar)
  PROBE_BARE_APK_DIR  directory with bare-flavor probe APKs
                      (default: .ci-probe/build/outputs/apk/bare/release)
  PROBE_SDK_APK_DIR   directory with withsdk-flavor probe APKs
                      (default: .ci-probe/build/outputs/apk/withsdk/release)
  VERSION             label written into the report (default: "current")
  OUTPUT              optional markdown file to write the section to (also printed to stdout)
  JSON_OUTPUT         optional path to also emit a structured per-run measurement JSON
                      (consumed by update_history.py to build size-latest.json /
                      size-history.json for docs/API consumers)
"""
import json
import os
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


def main() -> int:
    aar_path = os.environ.get(
        "AAR_PATH", "amani-sdk-v1/build/outputs/aar/amani-sdk-v1-release.aar"
    )
    bare_dir = os.environ.get(
        "PROBE_BARE_APK_DIR", ".ci-probe/build/outputs/apk/bare/release"
    )
    sdk_dir = os.environ.get(
        "PROBE_SDK_APK_DIR", ".ci-probe/build/outputs/apk/withsdk/release"
    )
    version = os.environ.get("VERSION", "current")
    output = os.environ.get("OUTPUT")
    json_output = os.environ.get("JSON_OUTPUT")

    bare_apks = discover_apks(bare_dir, "probe-bare")
    sdk_apks = discover_apks(sdk_dir, "probe-withsdk")

    if not bare_apks:
        print(f"::error::no bare-flavor probe APKs found in {bare_dir}", file=sys.stderr)
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
            f"**AAR (`amani-sdk-v1-release.aar`):** {fmt_mb(aar_size)} "
            f"(`{aar_size:,}` bytes)"
        )
        lines.append("")
    lines.append(
        "**Per-ABI SDK contribution** — *Bare APK* is a minimal Android app with "
        "the same AndroidX/Material baseline as the `:app` sample but **no SDK**; "
        "*With SDK APK* is the same baseline plus `:amani-sdk-v1`. Identical "
        "baseline in both flavors makes transitive AndroidX deps deduplicate the "
        "same way, so the delta reflects the SDK's net cost — not packaging noise."
    )
    lines.append("")
    lines.append(
        "> ⚠️ **Values are approximate.** APK builds are not perfectly "
        "reproducible — signing-block entropy, baseline-profile embedding, and "
        "ZIP entry ordering introduce ~0.1–0.5 MB variance between runs. Use "
        "these as **trend signals**, not exact byte budgets. The AAR row in the "
        "trend table above is more stable since it has none of these sources of "
        "variance."
    )
    lines.append("")
    lines.append("| ABI | Bare APK | With SDK APK | SDK contribution |")
    lines.append("|-----|---------:|-------------:|-----------------:|")

    abis = [a for a in ABI_ORDER if a in bare_apks and a in sdk_apks]
    for a in sdk_apks:
        if a not in abis and a in bare_apks:
            abis.append(a)

    per_abi: dict[str, dict[str, int]] = {}
    for abi in abis:
        bare = os.path.getsize(bare_apks[abi])
        withsdk = os.path.getsize(sdk_apks[abi])
        contribution = withsdk - bare
        lines.append(
            f"| {abi} | {fmt_mb(bare)} | {fmt_mb(withsdk)} | "
            f"{fmt_signed_mb(contribution)} |"
        )
        per_abi[abi] = {
            "bareBytes": bare,
            "withSdkBytes": withsdk,
            "sdkContributionBytes": contribution,
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
