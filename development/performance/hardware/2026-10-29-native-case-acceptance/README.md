# Native packaging and playable-case acceptance

This checkpoint retains the rendering and lighting implementation from runtime `82bd31accb17ea8fb154998de2afd7a22f019616` and strengthens its acceptance diagnostic. It changes no models, textures, authored lights, collision, session rules or rendering hot path.

The preceding runtime's exact-source [GPU40880772640](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/40880772640) and [full40880777632](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/40880777632) both completed successfully. The latter includes the complete manifest, packaged TCP, desktop solo startup and native executable checks. These certify that preceding source, not the new lifecycle assertions.

## Stronger real-UI control

`--gpu-ui-smoke` now selects House, Farmhouse and School explicitly through the public solo lobby. It requires sized first-frame readiness, authored intro initiation, skip handoff for House/School and natural completion for Farmhouse, advancing gameplay frames and actual GPU readback. It sends WASD through the production key listeners over a short collision-valid route in the van, reverses toward spawn, then uses the real X extraction handler, checks RETREATED, dismisses the real debrief and requires the menu before dispatching again. Reflection reads private state only; the test does not assign player coordinates, outcomes or readiness.

UI checks have bounded waits with at most one queued EDT task. The former unbounded `invokeAndWait` could hang before its deadline was checked. Captures paint only the test game's own window; partial case CSV rows survive a later failure. CI retains `build/e2e/gpu/ui/` with its existing GPU artifacts. Override `beckenburg.gpu.uiSmoke.sites` or `beckenburg.gpu.uiSmoke.output` for focused diagnosis.

Java 17 Maven packaging passes locally. The real UI test is reserved for the isolated CI desktop; no additional local game windows were opened. Its new exact-source CI result is pending at the implementation checkpoint. DEV-161 records this acceptance gap.

## Separate native executable verification

`tools/verify_native_gpu_preview.ps1` builds a fresh self-contained diagnostic app-image from exactly one candidate JAR. It refuses existing output directories, verifies the embedded JAR hash, uses an isolated home, launches hidden OpenGL controls and records stdout, stderr and provenance. It does not replace an installed game. The verification image is explicitly headless and is not a player distribution.

First local execution on Windows 11 / Java 17.0.12 / RTX 5070 / NVIDIA 616.64 passed with exit 0 and all ten map-type controls: unchanged-upload reuse, torch, candle flicker, blackout, doors, resize, return and live TV/map pixels. Twelve captures and raw `frames.csv` / `live-textures.csv` are retained locally under `build/native-acceptance-20261029/`. The compiled diagnostic candidate JAR and embedded JAR both hash to `17435AE3E96B12BC7740EDE728544367C9E72491572663A150778E2B0191B166`; its implementation files were still uncommitted during this first packaging run. A clean source checkpoint and retained evidence follow.

This native diagnostic is not installed full-game or physical input acceptance. The short van retreat loop is not a successful investigation or an interior route/performance benchmark. Full A-G, playable interior routes, installed solo/TCP loops and release gates remain open.
