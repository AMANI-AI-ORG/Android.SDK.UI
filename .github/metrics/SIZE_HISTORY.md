# AAR / APK Size History

> **Method:** Shows the release AAR size plus each SDK's approximate impact on a release APK per ABI, measured by building a probe app with and without each SDK against a shared AndroidX/Material baseline. *Core SDK* is what Core adds to an empty app; *UI SDK (own)* is what the UI layer adds on top of Core; *UI SDK incl. Core* is the full footprint an integrator gets by depending on `:amani-sdk-v1`. The optional modules (Speech Verifier, Voice Assistant) are not bundled by the UI SDK, so they get a table of their own: each is measured on top of the UI SDK and only counts when the app adds it. Sections before 1.46.0 have a single *UI SDK (incl. Core)* column and leave the optional modules out. APK figures are approximate (~0.1–0.5 MB run-to-run variance); the AAR row is stable.

---

## 1.50.0 — 2026-11-17

### UI SDK AAR size

| Version | AAR | Δ vs prev | Date (UTC) |
|---------|----:|----------:|------------|
| 1.50.0 <!-- bytes=2723094 --> | 2.60 MB | +1280 KB | 2026-11-17 |

### Core & UI SDK impact on a probe APK

| ABI | Total APK | Core SDK 3.24.0 | UI SDK (own) | UI SDK incl. Core |
|-----|----------:|----------------:|-------------:|------------------:|
| arm64-v8a | 47.45 MB | 31.76 MB | 11.34 MB | 43.10 MB |
| armeabi-v7a | 40.57 MB | 24.89 MB | 11.34 MB | 36.22 MB |
| x86 | 39.56 MB | 23.87 MB | 11.34 MB | 35.21 MB |
| x86_64 | 50.45 MB | 34.76 MB | 11.34 MB | 46.10 MB |
| universal | 92.73 MB | 77.00 MB | 11.38 MB | 88.38 MB |

### Optional modules on top of the UI SDK

> The UI SDK does not bundle these: an app pays for one only by adding it as a dependency. They share some libraries, so with both added the total is slightly below the sum.

| ABI | Speech Verifier 1.1.1 | Voice Assistant 1.1.0 |
|-----|----------------------:|----------------------:|
| arm64-v8a | 45.06 MB | 0.00 MB |
| armeabi-v7a | 44.00 MB | 0.00 MB |
| x86 | 45.35 MB | 0.00 MB |
| x86_64 | 45.29 MB | 0.00 MB |
| universal | 74.20 MB | 0.00 MB |

## 1.41.2 — 2026-05-13

### UI SDK AAR size

| Version | AAR | Δ vs prev | Date (UTC) |
|---------|----:|----------:|------------|
| 1.41.2 <!-- bytes=1412473 --> | 1.35 MB | — | 2026-05-13 |

### Impact on release sample APK

| ABI | Total APK | UI SDK (incl. Core) | Core SDK 3.11.4 |
|-----|----------:|--------------------:|---------------:|
| arm64-v8a | 24.71 MB | 20.32 MB | 18.02 MB |
| armeabi-v7a | 23.63 MB | 19.24 MB | 16.94 MB |
| x86 | 25.64 MB | 21.25 MB | 18.95 MB |
| x86_64 | 25.37 MB | 20.98 MB | 18.68 MB |
| universal | 42.89 MB | 38.50 MB | 36.19 MB |

