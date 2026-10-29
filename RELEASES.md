# Beckenburg Windows builds, developer downloads and acceptance

**Active shipping workflow:** [`.github/workflows/ship-installer.yml`](.github/workflows/ship-installer.yml), not the historical manually dispatched release workflow. This document describes the actual current `main` publication route as of the 2026-10-25 recovery gate. Full details and subsequent checkpoints live in [the durable recovery plan](development/RECOVERY_PLAN_2026-10-25.md).

## What the current shipping workflow actually publishes

On a `main` push, a serialized Windows job (cancelling superseded, not-yet-published builds) derives the next `beckenburg-vX.Y.Z` patch number, compiles the Java 17 game, bundles the GPU-first LibGDX/LWJGL launcher and OpenGL 3.3 backend, produces an Inno Setup Windows x64 EXE, attaches `LevelEditorKit-Java17.jar` and `SHA256SUMS.txt`, and creates a GitHub release. The editor is included under the installed application's `tools` directory. Do **not** claim this active job produces MSI or portable ZIP: those are built by a different, manually invoked historical workflow.

Before the recovery change, `ship-installer.yml` ran `build.cmd package` and **intentionally skipped tests**, even when the separate full-multiplayer acceptance failed. In particular `beckenburg-v0.1.69` was distributed from a failing E2E revision. This is a historical defect, not acceptable evidence of a verified release.

**Recovery enforcement:** the active workflow now requires `build.cmd e2e` (authored map regressions and a genuine packaged TCP multiplayer round), the complete `BeckenburgRegressionSuite`, and `BeckenburgDesktopE2ETest` **from the same checkout** before the GPU packaging, editor inclusion, native installer generation, checksums, and publication steps can run. All test commands fail the job on a nonzero exit. A red full-round result therefore blocks a newly published shipping release; simply producing a binary does not satisfy acceptance. The standalone GPU renderer acceptance workflow remains an additional, independent gate; passing CI is not a substitute for physical GPU performance and user interaction verification.

## Development builds and the updater

These are **development builds**, not final game releases. The current updater deliberately discovers only published `beckenburg-v*` releases without GitHub's prerelease flag. The shipping workflow therefore retains a normal published release flag for existing installed clients, even while noting development status; do not silently change this and break the ability to install test builds. Separate faster/unverified developer artifacts can be retained under GitHub Actions, but they must be explicitly marked *untested* and never described as full-round verified or silently supplied as the latest in-game update.

The in-game **UPDATES** UI and optional background check verify that the downloaded installer has the expected name, size and SHA-256 before asking for separate explicit approval. Failed checks do not modify installed saves. Windows executables remain unsigned; checksum integrity does not assert publisher authenticity. Installed-client upgrade through multiple successive releases still requires actual manual acceptance.

## Release acceptance tiers

1. **Compiled/package artifact:** all required resource bytes present and Java/game launcher compiles. This alone does not prove boot, multiplayer, GUI or hardware.
2. **CI-tested development release:** exact checkout passes packaged TCP multiplayer E2E, full regression manifest, desktop menu/UI and native packaging/startup probes. Report workflow run ID/SHA; do not label successful on a still-running or failing run.
3. **Physical test accepted:** clean Windows install, actual GPU path at representative resolutions, stable case transition, evidence/ritual/retreat victory, return to Castle, persistence, editor launch, multiplayer and updater from an already installed build. Capture logs and issues.
4. **Product release-ready:** all critical recovery gates, map/ghost/equipment acceptance, accessibility/licensing, persistence and multi-machine test coverage are closed with visible evidence.

For baseline details and current blockers, read `development/RECOVERY_PLAN_2026-10-25.md` and [the release acceptance contract](docs/RELEASE_ACCEPTANCE.md); do not infer completion from an obsolete stage ledger or a previously published downloadable EXE.

## Windows installer handoff and diagnostic logs

The game downloads the Inno Setup EXE to `%LOCALAPPDATA%\Beckenburg\Updates` and records the verified release SHA-256. On consent, it launches a separately executing Windows PowerShell helper in that directory, exits Beckenburg, and only then allows the helper to recheck the installer SHA-256 and start setup. The helper does not run from the install folder or depend on Beckenburg's bundled runtime. If the helper cannot proceed, the downloaded EXE remains available for manual execution **after all Beckenburg processes are closed**.

Game and updater session logs: `%USERPROFILE%\.beckenburg\diagnostics\session-*.log`. Detached helper logs: `update-handoff-*.log`. Inno Setup logs: `update-setup-*.log` in the same diagnostics directory. The handoff log records waiting, game shutdown, integrity verification, installer PID and final exit code or failure. Logs are local and never automatically uploaded. This is a source-level handoff repair: Windows installation acceptance must still be demonstrated on a real installed build before marking the updater gate closed.
