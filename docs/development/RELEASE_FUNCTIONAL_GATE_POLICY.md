# Release gates: playable game only (2026-10-29)

The shipping workflow is `.github/workflows/ship-installer.yml` on `main`. The **only legitimate blockers** are evidence that the downloadable game cannot function:

1. Java 17 source + required game resources compile/package as an actual runnable game.
2. A packaged LAN investigation completes against the real host, ghost and hunter clients.
3. The actual desktop client opens directly into Castle, provides its real Solo start with advancing gameplay, and exposes multiplayer; campaign entry works.
4. The Windows app/installer can be created, installed independently and the **installed** EXE can reach `PHASE ready` plus `PHASE Castle home available`, not merely a splash or file-presence assertion.
5. The updater executes from an independent external helper. The published download has nonempty verified assets, correct tag, source commit and checksums.

**Never block publication for:** cosmetic audits, image approvals, precise model counts unrelated to a missing playable runtime, extensive 288+ regression test suites, optional standalone editor production, GPU benchmark/rotation/flyover frame acceptance, performance thresholds without demonstrated unplayability, screenshot review, Pages/docs/gallery failures or secondary audio quality tests. Report these independently with explicit warnings.

The GPU-first backend should automatically fall back to the existing software game when native OpenGL cannot initialize. Required GPU-only acceptance/review modes keep failing loudly instead of pretending to pass. If GPU packaging itself fails, ship the verified software game installer rather than withhold a working update.

The automatic installer flow no longer runs `build.cmd e2e`, because that developer acceptance path also runs unrelated character/asset/camera fidelity tests. It instead builds `build.cmd package`, compiles targeted smoke harnesses, checks a complete packaged TCP round and exercises actual Castle/solo desktop operation. The independent `beckenburg-full-round-e2e.yml` and `gpu-renderer.yml` continue full regression visibility.

The legacy manual `beckenburg-release.yml` may still execute all diagnostics, but its publication status now depends only on game startup and native install/launch, not the extended regression/visual suite. Do not label a build functional if its core smoke fails.

**Important:** no CI configuration proves user hardware performance. A failed cosmetic test is not proof of an unplayable game; conversely a successful compile is not proof that an installer starts.

**Continuous development:** installer jobs serialize rather than cancel in-flight runs. A built, verified commit can publish while newer main commits are being prepared; the next successful installer supersedes it. Publication always checks the built checkout SHA and independently validates uploaded release assets, but it must not mistake a newer main commit for a failure of the working version. Ghost camera validation now generates a legitimate audible equipment event before requesting an action that requires sound, instead of relying on silent co-location.
