#!/usr/bin/env python3
"""
Maintain .github/metrics/SIZE_HISTORY.md.

Given a freshly-generated `--section` markdown (produced by measure_apk.py)
and a `--version` label, this script:

  1. Computes the AAR Δ vs the previous most-recent section in the file and
     patches the placeholder `—` cell in the new section's AAR table.
  2. Removes any pre-existing section for that version.
  3. Inserts the new section at the top (just under the file header), so the
     newest release is always first.

The history file is created with the canonical header if it doesn't exist.
Optionally also bumps the version segment in the README size badge URL so it
pins to the version that this action just published.
"""
from __future__ import annotations

import argparse
import json
import re
from datetime import datetime, timezone
from pathlib import Path

HEADER = """# AAR / APK Size History

> **Method:** Shows the release AAR size plus each SDK's approximate impact on a release APK per ABI, measured by building a probe app with and without each SDK against a shared AndroidX/Material baseline. The *UI SDK* column includes Core SDK **and** the UI SDK's own Material/AndroidX libraries — i.e. the full footprint an integrator gets by depending on `:amani-sdk-v1`. APK figures are approximate (~0.1–0.5 MB run-to-run variance); the AAR row is stable.

---

"""


def parse_section(section: str) -> tuple[str, int]:
    """Extract (date_str, aar_bytes) from a generated section.

    The section's AAR mini-table contains a row like
      `| 1.41.1 <!-- bytes=1412473 --> | 1.35 MB | — | 2026-05-13 |`
    where the byte count lives in an HTML comment so we can recover exact
    precision later.
    """
    m = re.search(r"^## .+? — (\d{4}-\d{2}-\d{2})", section, re.MULTILINE)
    date = m.group(1) if m else ""
    m = re.search(r"<!--\s*bytes=(\d+)\s*-->", section)
    if not m:
        raise SystemExit("Cannot find AAR bytes (`<!-- bytes=... -->`) in section markdown")
    return date, int(m.group(1))


def fmt_mb(b: int) -> str:
    return f"{b/1048576:.2f} MB"


def fmt_signed_kb(b: int) -> str:
    sign = "+" if b >= 0 else "−"
    return f"{sign}{abs(b)/1024:.0f} KB"


def find_prev_aar_bytes(text: str, skip_version: str) -> int | None:
    """Return the AAR byte count of the topmost existing section ≠ skip_version."""
    for m in re.finditer(r"^## ([0-9][\w.\-]*) — ", text, re.MULTILINE):
        if m.group(1) == skip_version:
            continue
        # Body runs from this match to the next H2 (or EOF)
        rest = text[m.end():]
        nxt = re.search(r"^## ", rest, re.MULTILINE)
        body = rest if not nxt else rest[: nxt.start()]
        b = re.search(r"<!--\s*bytes=(\d+)\s*-->", body)
        if b:
            return int(b.group(1))
    return None


def patch_section_delta(section: str, delta_text: str) -> str:
    """Patch the placeholder `—` Δ cell in the AAR mini-table."""
    return re.sub(
        r"(<!-- bytes=\d+ --> \| [^|]+ \| )—( \|)",
        lambda m: m.group(1) + delta_text + m.group(2),
        section,
        count=1,
    )


def update_sections(text: str, version: str, section: str) -> str:
    """Drop any existing `## {version}` block, then prepend the new section."""
    # Compute delta vs previous most-recent section, patch the AAR table cell
    prev = find_prev_aar_bytes(text, skip_version=version)
    if prev is not None:
        _, cur = parse_section(section)
        section = patch_section_delta(section, fmt_signed_kb(cur - prev))

    # Drop any pre-existing section for this version
    pattern = re.compile(
        r"^## " + re.escape(version) + r" — .*?(?=^## |\Z)",
        re.DOTALL | re.MULTILINE,
    )
    text = pattern.sub("", text)

    section = section.rstrip() + "\n\n"

    # Insert just after the HEADER's "---\n\n" separator
    marker = "---\n\n"
    if marker in text:
        head, _, tail = text.partition(marker)
        return head + marker + section + tail
    return text.rstrip() + "\n\n" + section


def update_readme_badge_version(readme_path: Path, version: str) -> bool:
    """Bump the version segment in the README size badge URL.

    Matches the AAR size badge endpoint pointing at
    `raw.githubusercontent.com/<org>/<repo>/<ref>/.github/metrics/aar-size.json`
    and replaces `<ref>` (currently `main` or a previous version) with `version`.
    Returns True if README was modified.
    """
    if not readme_path.exists():
        return False
    text = readme_path.read_text()
    pattern = r"(raw\.githubusercontent\.com/[^/]+/[^/]+/)([^/]+)(/\.github/metrics/aar-size\.json)"
    new_text, n = re.subn(pattern, r"\g<1>" + version + r"\g<3>", text, count=1)
    if n and new_text != text:
        readme_path.write_text(new_text)
        return True
    return False


def update_json_outputs(
    measurement: dict,
    latest_path: Path,
    history_path: Path,
) -> None:
    version = measurement["version"]

    releases: list[dict] = []
    if history_path.exists():
        try:
            existing = json.loads(history_path.read_text())
            releases = [r for r in existing.get("releases", []) if r.get("version") != version]
        except (json.JSONDecodeError, KeyError):
            releases = []

    prev_aar = releases[0]["aar"]["bytes"] if releases else None
    cur_aar = measurement["aar"]["bytes"]
    delta_vs_prev = None if prev_aar is None else cur_aar - prev_aar

    latest = dict(measurement)
    latest["deltaVsPrev"] = {"aarBytes": delta_vs_prev}

    releases.insert(0, dict(measurement))

    history = {
        "schemaVersion": 1,
        "updatedAt": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "releases": releases,
    }

    latest_path.parent.mkdir(parents=True, exist_ok=True)
    latest_path.write_text(json.dumps(latest, indent=2) + "\n")
    history_path.write_text(json.dumps(history, indent=2) + "\n")


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--version", required=True)
    ap.add_argument("--section", required=True, type=Path, help="path to section markdown")
    ap.add_argument("--history", default=".github/metrics/SIZE_HISTORY.md", type=Path)
    ap.add_argument(
        "--measurement-json",
        type=Path,
        help="per-run measurement JSON produced by measure_apk.py; "
        "when provided, size-latest.json and size-history.json are written",
    )
    ap.add_argument("--latest-json", default=".github/metrics/size-latest.json", type=Path)
    ap.add_argument("--history-json", default=".github/metrics/size-history.json", type=Path)
    ap.add_argument(
        "--readme",
        type=Path,
        default=None,
        help="path to README.md; when provided, the size-badge URL's branch/tag "
        "segment is rewritten to the new version so the badge pins to this release",
    )
    args = ap.parse_args()

    section_text = args.section.read_text()
    parse_section(section_text)  # validates section format early

    if not args.history.exists():
        args.history.parent.mkdir(parents=True, exist_ok=True)
        args.history.write_text(HEADER)

    text = args.history.read_text()
    text = update_sections(text, args.version, section_text)
    args.history.write_text(text)
    print(f"Updated {args.history}: section for {args.version}")

    if args.measurement_json and args.measurement_json.exists():
        measurement = json.loads(args.measurement_json.read_text())
        update_json_outputs(measurement, args.latest_json, args.history_json)
        print(f"Updated {args.latest_json} and {args.history_json}")

    if args.readme:
        if update_readme_badge_version(args.readme, args.version):
            print(f"Updated {args.readme} size-badge URL → {args.version}")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
