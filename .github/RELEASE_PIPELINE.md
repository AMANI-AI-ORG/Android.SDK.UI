# Release Pipeline

This project uses a **3-workflow release pipeline** to test, version-bump, and publish releases through GitHub Actions.

---

## Workflow Overview

| Workflow | Trigger | Purpose |
|----------|---------|---------|
| [`release.yml`](workflows/release.yml) | `workflow_dispatch` (manual) | Run tests on `main`, then create `release/{version}` branch and PR |
| [`release-ci.yml`](workflows/release-ci.yml) | `pull_request` opened / synchronize | Run tests on PR branch, push version bump commit if tests pass |
| [`release-publish.yml`](workflows/release-publish.yml) | `pull_request` closed (merged) | Tag, create GitHub Release, delete release branch |

```
┌─────────────────┐          ┌───────────────────┐          ┌────────────────────┐
│   release.yml   │  PR ──►  │  release-ci.yml   │  Merge ► │ release-publish.yml│
│                 │          │                   │          │                    │
│ 1. Unit Tests   │          │ 1. Unit Tests     │          │ 1. Extract version │
│ 2. Android Tests│          │ 2. Android Tests  │          │ 2. Find prev tag   │
│ 3. Create PR    │          │ 3. Version Bump   │          │ 3. Git tag         │
│    (no bump)    │          │    (chore commit) │          │ 4. GitHub Release  │
└─────────────────┘          └───────────────────┘          │ 5. Cleanup branch  │
                                                            └────────────────────┘
```

---

## Full Release Flow

There are two ways to start a release:

### Option A: Manual Trigger (Recommended)

> Tests run on `main` first as an early gate. PR is only created if tests pass.

```
 Developer triggers release.yml
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
    release-ci.yml triggers automatically
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
    release-publish.yml triggers
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

> **Option A** flows into **Option B** at step 2. The difference is Option A validates tests on `main` before creating the PR. Any PR from a branch that does **not** match `release/*` is ignored by CI — all jobs skip silently.

---

## Usage

### 1. Start a Release

Go to **Actions** → **Release Pipeline** → **Run workflow**

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
