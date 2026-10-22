# Beckenburg — tested Windows releases and self-updating installer

The authoritative application is the **standalone** [The Beckenburg Project](https://github.com/mrcalzon02/The-Beckenburg-Project). Release tags are `beckenburg-vMAJOR.MINOR.PATCH` and the GitHub [Releases](https://github.com/mrcalzon02/The-Beckenburg-Project/releases) feed belongs to this application. There is no dependency on the old multi-product HB-TTRPG-tools repository.

## Continuous Windows package publication

`.github/workflows/beckenburg-release.yml` runs on **every push to `main`** (including documentation-only changes), and can be manually dispatched to select a minor/major bump. Branches and unmerged pull requests **never publish**. A single release job serializes version allocation; the default bump is patch. Superseded revisions are checked again before publication. Tags and releases are not source-branch pushes, so they do not recursively trigger another build.

**Publishing is conditional, not automatic approval.** The Windows runner first runs `build.cmd` with the complete regression manifest; it then executes `build.cmd e2e` for packaged real-TCP multiplayer round checks, loads the packaged JAR through desktop UI acceptance, and packages with jpackage + WiX 3 using the built-in Java runtime. The candidate native executable must launch and reach `PHASE ready` and `PHASE Menu available` on Windows before upload. If *any* required test, setup, packaging or startup step fails, no release is published.

A passing job produces `Beckenburg-X.Y.Z-Windows-x64.exe` (per-user installer), `Beckenburg-X.Y.Z-Windows-x64.msi`, `Beckenburg-X.Y.Z-Windows-x64.zip` (portable directory with runtime), and `SHA256SUMS.txt`. It embeds `release.properties` version and exact source commit **in the built JAR before packaging**, assembles a draft release, verifies remote upload, and only then publishes it as the latest GitHub release. Existing published versions are immutable. This is a development build, not an assertion that Gold acceptance is complete.

## Existing in-game updater

The main menu's **UPDATES** control and daily optional background check use the live GitHub API. They filter only stable `beckenburg-v*` releases; even if unrelated releases appear, they cannot be installed. No Android-signing or app-store provisioning is involved. The client insists on exactly named installer assets and a `SHA256SUMS.txt`, downloads to the user-local staging directory, enforces a maximum size, verifies both size and SHA-256, and only executes the staged installer following **separate explicit approval**. The running game exits for the installer handoff. Failed update checks or downloads leave the installed game and saved profiles untouched. Windows may warn about the *unsigned* installer; checksum verification establishes transfer integrity, not publisher identity or code-signing trust.

The updater deliberately does not replace files in the currently executing game process or silently install updates. Its tests verify correct repository selection and rejection of foreign hosts, older versions, drafts and malformed feeds. The standalone release pipeline still needs a completed source-to-published-assets run and independent installed-client verification before treating a particular revision as a distributed release.

Use [Windows multiplayer acceptance](.github/workflows/beckenburg-full-round-e2e.yml), [production release workflow](.github/workflows/beckenburg-release.yml), and [release acceptance contract](docs/RELEASE_ACCEPTANCE.md) for the separate verification gates.
