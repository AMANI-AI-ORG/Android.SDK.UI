# Release Pipeline

This project uses a **4-workflow release pipeline** to test, version-bump, publish, and measure size of releases through GitHub Actions.

---

## Workflow Overview

The pipeline has three workflows, each owning a distinct phase of the release lifecycle:

| Workflow | File | Trigger | Phase | Purpose |
|----------|------|---------|-------|---------|
| **① Release · Prepare** | [`release.yml`](workflows/release.yml) | `workflow_dispatch` (manual) | Pre-PR gate | Runs tests on `main`, then creates the `release/{version}` branch and opens the release PR |
| **② Release · Verify** | [`release-ci.yml`](workflows/release-ci.yml) | `pull_request` opened / synchronize | PR validation | Runs tests on the PR branch, then pushes the `chore(release): bump version` commit if tests pass |
| **③ Release · Ship** | [`release-publish.yml`](workflows/release-publish.yml) | `pull_request` closed (merged) | Post-merge publish | Creates the git tag, builds the release AAR, attaches it to the GitHub Release, deletes the release branch, and **conditionally** invokes ④ Release · Measure (see below) |
| **④ Release · Measure** | [`release-measure.yml`](workflows/release-measure.yml) | `workflow_dispatch` (manual) **or** called by ③ when opted in | Size telemetry | Builds AAR, `:app` APK splits, and an inline SDK-only probe APK; opens a `chore(metrics): size report for <version>` PR from `metrics/size-<version>` against `main` with the updated [`SIZE_HISTORY.md`](metrics/SIZE_HISTORY.md) and AAR badge — merge it to land the record |

### When does ④ Release · Measure run?

Measure is **opt-in** to keep typical releases fast. It runs in three scenarios:

1. **Manual trigger** — Actions → ④ Release · Measure → Run workflow. Always works regardless of release state. Use this for backfill, branch experiments, or ad-hoc audits.
2. **Tick the box in ① Release · Prepare** — when starting a release, set *"Run ④ Release · Measure after publish"* to true. Prepare adds the `measure-size` label to the PR; Ship sees the label after merge and invokes Measure automatically.
3. **Add the `measure-size` label by hand** to a release PR before merging it — same effect as #2 without re-running Prepare.

If neither the label nor a manual trigger is present, Ship publishes the release without running Measure.

> **How the metrics land in `main`:** Measure opens a `chore(metrics): size report for <version>` PR from `metrics/size-<version>` (re-using the same branch on re-runs via force-with-lease). It does **not** push directly to `main` — review and merge the PR to commit the size record. This avoids needing a branch-protection bypass for the bot.

### How they differ

- **Prepare** is the *entry point* — invoked by a human, runs only on `main`, and produces a PR. It never bumps the version or publishes anything; it's a gate that proves `main` is releasable before opening the PR.
- **Verify** is the *gatekeeper* — runs on every push to a `release/*` PR. It re-runs the same tests against the merge candidate and is the **only** workflow that mutates `build.gradle` (the version bump commit).
- **Ship** is the *publisher* — runs once, after the PR merges. It performs no testing; it only tags the merge commit, creates the GitHub Release with the AAR attached, cleans up the branch, and hands off to Measure.
- **Measure** is the *telemetry* — opt-in via the `measure-size` PR label or a manual trigger. It builds an inline SDK-only probe app to isolate the UI SDK's per-ABI APK footprint, then opens a separate `chore(metrics): ...` PR with the updated [`SIZE_HISTORY.md`](metrics/SIZE_HISTORY.md) for review. Skipped by default to keep ship time fast.

In short: **Prepare opens the door, Verify checks the work, Ship takes it live, Measure tracks its weight.**

```
┌────────────────────┐       ┌────────────────────┐       ┌────────────────────┐       ┌────────────────────┐
│ ① Release·Prepare  │ PR ─► │ ② Release·Verify   │ Mrg ► │ ③ Release·Ship     │ ────► │ ④ Release·Measure  │
│  (release.yml)     │       │  (release-ci.yml)  │       │  (release-publish) │       │  (release-measure) │
│                    │       │                    │       │                    │       │                    │
│ 1. Unit tests      │       │ 1. Unit tests      │       │ 1. Extract version │       │ 1. Build AAR       │
│ 2. Android tests   │       │ 2. Android tests   │       │ 2. Build AAR       │       │ 2. Build :app APKs │
│ 3. Create PR       │       │ 3. Version bump    │       │ 3. Tag             │       │ 3. Build probe APK │
│    (no bump)       │       │    (chore commit)  │       │ 4. GitHub Release  │       │ 4. Generate report │
└────────────────────┘       └────────────────────┘       │    + AAR asset     │       │ 5. Update history  │
                                                          │ 5. Cleanup branch  │       │ 6. Refresh badge   │
                                                          └────────────────────┘       └────────────────────┘
                                                                  │                            ▲
                                                                  └── if PR has ────────────── ┘
                                                                     `measure-size` label
                                                          (or run ④ manually any time)
```

---

## Full Release Flow

There are two ways to start a release. They both end at the same place (a merged release PR that ships) — they only differ in **who creates the PR** and **whether tests run on `main` before the PR exists**.

### TL;DR — which option to pick?

| | **Option A — Assisted (via Prepare)** | **Option B — Manual PR** |
|---|---|---|
| Entry point | Run **Release · Prepare** in Actions UI | Create the branch & PR yourself |
| Tests on `main` before PR? | ✅ Yes — early gate | ❌ No |
| Branch & PR creation | Automated by Prepare | Done by you with `git`/GitHub UI |
| First workflow that runs | `Release · Prepare` → then `Release · Verify` | `Release · Verify` (Prepare is skipped) |
| Version bump commit | Pushed by Verify after tests pass | Pushed by Verify after tests pass |
| Publish step | `Release · Ship` on merge | `Release · Ship` on merge |
| Best for | Standard releases — recommended default | Hotfixes, scripted releases, retries when Prepare's PR creation step itself failed |

> **Rule of thumb:** Use **Option A** unless you have a reason not to. Option A runs *one extra round of tests* (on `main`, before the PR exists), so if `main` is broken you find out **before** a PR clutters the repo.

### Option A: Manual Trigger (Recommended)

> Tests run on `main` first as an early gate. PR is only created if tests pass.
> **Workflows that run:** Prepare → Verify → Ship.

```
 Developer triggers ① Release · Prepare
 (version: "1.5.0", release_notes: "...")
                     │
                     ▼
 ┌───────────────────────────────────┐
 │  1. Unit tests on main            │
 │  2. Android tests on emulator     │
 └──────────────┬────────────────────┘
                │
       ┌── Tests pass? ──┐
       │                  │
      YES                 NO
       │                  │
       ▼                  ▼
 Create release/1.5.0   Pipeline fails
 branch & open PR       (no PR created)
       │
       ▼
 Continues to Option B, step 2 ──►
```

### Option B: Direct Release PR

> Create a `release/*` branch and PR yourself. CI handles the rest.
> **Workflows that run:** Verify → Ship. (Prepare is **never invoked** in this path — you replace it with manual git steps.)

**Example:**
```bash
git checkout -b release/1.5.0
git push origin release/1.5.0
# Then open PR on GitHub: release/1.5.0 → main
# PR title: "release: v1.5.0"
# PR body:
#   ## Release v1.5.0
#   ### Release Notes
#   Feature: Added new document verification screen
#   Fix: Camera preview crash on Android 12+
#   Update: Core SDK version to v3.11.0
```

```
 1. Developer creates release/1.5.0 branch
    & opens PR to main
                     │
                     ▼
    ② Release · Verify triggers automatically
                     │
                     ▼
 ┌───────────────────────────────────┐
 │  2. Unit tests on PR branch       │
 │  3. Android tests on emulator     │
 └──────────────┬────────────────────┘
                │
       ┌── Tests pass? ──┐
       │                  │
      YES                 NO
       │                  │
       ▼                  ▼
 4. Push chore commit   Pipeline fails
    "chore(release):    (no version bump)
     bump version
     to 1.5.0"
       │
       ▼
 5. PR shows version bump
    → Ready for review
       │
       ▼
 6. Reviewer approves & merges PR
       │
       ▼
    ③ Release · Ship triggers
       │
       ▼
 ┌───────────────────────────────────┐
 │  7. Create git tag v1.5.0         │
 │  8. GitHub Release (badged notes) │
 │  9. Delete release/1.5.0 branch   │
 └───────────────────────────────────┘
       │
       ▼
    🚀 Published
    (available on JitPack via tag)
```

> **Option A flows into Option B at step 2.** Once a `release/*` PR exists, the rest of the pipeline (Verify → Ship) is identical regardless of how the PR was created. The only real difference is the **pre-PR test run on `main`** that Option A adds. Any PR from a branch that does **not** match `release/*` is ignored by CI — all jobs skip silently.
>
> **Choosing between them:**
> - Pick **A** when you want the safety net and don't mind clicking a button.
> - Pick **B** when Prepare is unavailable (e.g., its PR-creation step crashed mid-run and you need to recover), or when scripting releases from outside the GitHub UI.

---

## Usage

### 1. Start a Release

Go to **Actions** → **① Release · Prepare** → **Run workflow**

| Input | Example | Description |
|-------|---------|-------------|
| `version` | `1.5.0` | Semver release version (see format below) |
| `release_notes` | See below | One item per line, keywords auto-badge |
| `dry_run` | `false` | Set `true` to run tests only (no PR) |

> **Dry Run:** When `dry_run` is `true`, unit tests and Android instrumented tests run normally. If they pass, the pipeline stops — no branch, no PR, no version bump is created. Useful for validating that tests pass on `main` before committing to a release.

**Version format:** `X.Y.Z` or `X.Y.Z-suffix`. The input is automatically sanitized — common prefixes like `v`, `release:`, `release: v` are stripped.

| Input | Sanitized | Branch | Release type |
|-------|-----------|--------|-------------|
| `1.5.0` | `1.5.0` | `release/1.5.0` | Stable (latest) |
| `v1.5.0` | `1.5.0` | `release/1.5.0` | Stable (latest) |
| `1.5.0-beta` | `1.5.0-beta` | `release/1.5.0-beta` | Pre-release |
| `1.5.0-alpha` | `1.5.0-alpha` | `release/1.5.0-alpha` | Pre-release |
| `1.5.0-rc1` | `1.5.0-rc1` | `release/1.5.0-rc1` | Pre-release |
| `1.5.0-SNAPSHOT-20260416` | `1.5.0-SNAPSHOT-20260416` | `release/1.5.0-SNAPSHOT-20260416` | Pre-release |
| `release: v1.5.0` | `1.5.0` | `release/1.5.0` | Stable (latest) |

Pre-release keywords: `snapshot`, `test`, `beta`, `alpha`, `rc`, `dev`, `canary`, `nightly`, `preview`. Versions containing these are published as **Pre-release** on GitHub and are **not** marked as the latest release.

**Release notes example:**

```
Feature: Added new document verification screen
Fix: Camera preview crash on Android 12+
Update: Core SDK version to v3.11.0
Improvement: Reduced memory usage in image processing
Change: Minimum API level raised to 23
```

**Keyword badges** (applied automatically on publish):

| Keyword | Badge |
|---------|-------|
| `fix` / `fixed` / `fixes` | ![Fix](https://img.shields.io/badge/Fix-🐛-e74c3c) |
| `feature` | ![Feature](https://img.shields.io/badge/Feature-✨-8c730d) |
| `improvement` | ![Improvement](https://img.shields.io/badge/Improvement-🛠️-8e44ad) |
| `update` / `updated` | ![Update](https://img.shields.io/badge/Update-🔄-2980b9) |
| `change` / `changed` | ![Change](https://img.shields.io/badge/Change-♻️-136634) |

### 2. Review the PR

After the workflow completes:
- A PR titled `release: v1.5.0` appears targeting `main`
- CI runs tests on the PR branch automatically
- If tests pass, a `chore(release): bump version to 1.5.0` commit is pushed
- Review the version bump and release notes, then approve & merge

### 3. Release Publishes Automatically

Once merged:
- Git tag `v1.5.0` is created
- GitHub Release is published with badged release notes
- The `release/1.5.0` branch is deleted
- The release is available on JitPack:

```gradle
dependencies {
    implementation 'com.github.AMANI-AI-ORG:Android.SDK.UI:v1.5.0'
}
```
