# AAR / APK Size History

> **Method:** Shows the release AAR size plus each SDK's approximate impact on a release APK per ABI, measured by building a probe app with and without each SDK against a shared AndroidX/Material baseline. The *UI SDK* column includes Core SDK **and** the UI SDK's own Material/AndroidX libraries — i.e. the full footprint an integrator gets by depending on `:amani-sdk-v1`. APK figures are approximate (~0.1–0.5 MB run-to-run variance); the AAR row is stable.

---

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

