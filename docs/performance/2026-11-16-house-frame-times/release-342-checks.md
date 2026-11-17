# v0.1.342 gameplay and rendering verification

Every one of the 384 test names in the exact release manifest has a passing receipt. The Windows installer job separately proved packaged solo, multiplayer investigation and installed Castle startup. The public regression workflow stopped when a newer gameplay push replaced it in the shared main-branch queue. The continuation used the pinned release source at `7b33b944df847e1eeb9ced77c43aba8dca5aab33`.

| Evidence | Passed entry points | Added distinct passes | Scope |
|---|---:|---:|---|
| Initial hosted Windows manifest | 248 | 248 | The ordered first 248 tests passed before cancellation, including the UV lifetime and 900-tick four-seat rite. |
| Remaining manifest on isolated Java 17 | 134 | 134 | 136 remaining tests ran from the pinned sources and verified assets. Exact missing fixtures were restored. Local memory limited two Castle renderer tests. |
| Pinned Core30 Windows confirmation | 6 | 2 | All six large-world checks passed. Four repeated local positive results, and the remaining two completed world residency and dense-versus-sparse lighting parity. |
| Total | | 384 | Every release entry point covered. |

[Hosted Windows run](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/44013627932) · [Installer, real solo and full-network-round gate](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/44013628166) · [Independent Core30 process logs](https://github.com/mrcalzon02/Dens-Mortis/actions/runs/44017795316)

## Exact-release Core30 rerun

Each unmodified Java 17 test ran in a new process against the packaged game. The controller checked the source tree, JAR SHA-256, test source blobs and normal process deadlines. The lighting parity check records all submitted vertex floats across Castle and every authored map, changed lamps, circuit loss and door states. The House performance results are in their own GTX frame CSVs.

| Test | Elapsed seconds | Process limit seconds |
|---|---:|---:|
| CastleContainedMenuTest | 8.890 | 90 |
| CastleInventoryWorkstationTest | 8.125 | 90 |
| WorldResidencyTest | 58.016 | 90 |
| PreparedLightBatchParityTest | 58.391 | 180 |
| CastleStoryTest | 12.610 | 90 |
| CastleLooseClutterTest | 7.984 | 90 |

The Core30 packaged game SHA-256 is `d77b9ed09e4881401d556c88b76045147391c513ad4d1b742fbd2fbf7df3a1b8`. Its original raw worker ZIP SHA-256 is `2d33ab356bd0d846f0b82a29993042f5617c14f9ad09c77b44cc9498d7d19e46`.

[Windows test logs and package/source provenance](release-342-windows-checks.zip) · [Remaining manifest, individual results and fixture checks](release-342-regression-tail.zip) · [Local memory-retry diagnostics](release-342-local-memory.zip) · [Rite and UV boundary checks](gameplay-timing-checks.zip) · [SHA-256 checksums](release-342-SHA256SUMS)
