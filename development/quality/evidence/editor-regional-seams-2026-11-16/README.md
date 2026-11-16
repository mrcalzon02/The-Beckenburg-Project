# Regional UV seam updates and glTF material export

## Selected edge in the Material Editor

The Seams tab highlights the chosen edge with cyan dashes. A paired edge identifies the adjacent native face and reports UV continuity independently of the authored mark. The desktop fixture marked two matching edges, moved the selected island, published it and checked every game UV. It canceled a pending clear, then applied the clear and confirmed the UV split persisted. Undo restored the initial matching UVs and marks. The regression also checks that the readout fits the panel and the selected edge appears on the UV canvas. [Desktop fixture](../../../../src/test/java/org/beckenburg/G2TexturedModelGuiTest.java)

![Selected C–A edge on Face 0, with split UVs, a cleared mark, and Face 3 across the edge](uv-seam-inspector.png)

## Mesh allocation

A Material Editor UV shift on one interior triangle changes three seam flags and their twins. The native topology now shares unchanged 256-entry half-edge pages between immutable revisions. A full rebuild still computes all connectivity and diagnostics. The Java 17 regression places 347 twin pairs 1,042 edge entries apart, branches dense and partial edits and compares all five retained snapshots against independent rebuilds. The same test class exercises 120,000-face meshes under a 256 MiB heap.

The allocation fixture uses 120,000 connected triangles and 60,501 vertices. It alternates a 0.125 UV shift on one interior face and verifies the UV and seam results at each iteration. Mean calling-thread allocations below are bytes over eight samples after four warmups, using Temurin 17.0.21+9 and a 256 MiB heap. The isolated topology call repeats the update separately and falls outside the complete edit measurement.

| Operation | Vertex and face IDs | Baseline | Regional pages |
|---|---|---:|---:|
| Isolated half-edge update | Ordinal | 5,760,400 | 8,072 |
| Isolated half-edge update | Sparse 64-bit | 5,760,400 | 8,072 |
| Create, review, apply and read back the edit | Ordinal | 9,610,828 | 3,858,488 |
| Create, review, apply and read back the edit | Sparse 64-bit | 23,070,328 | 17,318,004 |

The sparse fixture uses descending vertex IDs above 2⁴⁰ and face IDs above 2⁴³, with gaps. Its candidate validation still builds ID indexes. These measurements cover authoring-thread allocations and exclude GPU uploads and frame presentation. [All samples and phase breakdowns](allocation-results.txt)

## Interchange

Khronos glTF Validator 2.0.0-dev.3.10 examined 21 files per revision. Twelve exports exercise OPAQUE, BLEND and MASK materials in both GLB and JSON glTF, using either a new mesh or a material added to an imported model. Both sides use the same 1,996-byte source GLB for that comparison. Nine additional files cover float, unsigned-byte and unsigned-short UV inputs, followed by a native edit, save, reopen and export to both formats.

| Validator result across 21 files | Baseline | Updated export |
|---|---:|---:|
| Errors | 0 | 0 |
| Warnings | 17 | 0 |
| Informational messages | 120 | 120 |
| Hints | 156 | 156 |

Every baseline warning identified an `alphaCutoff` written for an OPAQUE or BLEND material. New exports emit the glTF property for MASK and preserve native cutoffs through Beckenburg metadata. Existing third-party source material definitions retain their original fields. Remaining notices include retained unused buffers, view targets and private extension metadata. [Per-file diagnostics](validator-results.tsv)

## Verification on implementation 8173f2ed

The Java 17 build compiled all 493 production sources and 634 test sources. The standalone kit also compiled independently of the host. All 69 portable checks and 31 host checks passed locally, followed by the windowed G2 textured-model regression. [Local command results](local-acceptance.txt)

The Windows standalone job compiled the portable kit and passed the cross-page adjacency and alpha-mode cases. Its downloaded JAR passed both suites independently with a 256 MiB heap. [Standalone run](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/43976139396)

The Windows native and installed-editor run exercises the same GUI fixture and archives its captures. [Native and installed editor](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/43976139586)

The Windows shipping job published the installer, editor kit and checksums at version 0.1.332. [Release](https://github.com/mrcalzon02/The-Beckenburg-Project/releases/tag/beckenburg-v0.1.332) · [Shipping checks](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/43976139475)

The Windows multiplayer job passed the packaged TCP round, gear pickup, van extraction and sound/voice checks. Its broad regression manifest logged 97 completed passes out of 379 entries before a newer push superseded the job. [Multiplayer run](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/43976139517)

## Reproduce

The script compiles [baseline f17b806c](https://github.com/mrcalzon02/The-Beckenburg-Project/commit/f17b806c0ed43d30d9ed250582543e7bbaeec0c6) and [implementation 8173f2ed](https://github.com/mrcalzon02/The-Beckenburg-Project/commit/8173f2edb367da3b602eb045080c90a855fc3441) from their Git objects. Run from a checkout containing both revisions with a Java 17 JDK on PATH. The original before-side validator run used `19161b70157f4f72e865c74cce507776526fa3ff`. Its portable editor sources and the two fixture classes are byte-identical to the benchmark baseline.

```sh
sh development/quality/evidence/editor-regional-seams-2026-11-16/run.sh allocation
sh development/quality/evidence/editor-regional-seams-2026-11-16/run.sh gltf
```

The allocation mode checks numeric and full-rebuild parity in separate JVMs. Small JVM-dependent differences in measured bytes can occur. The glTF mode also requires Node.js, checks the SHA-256 of the pinned official validator package and compares all 42 severity rows and warning locations against the saved evidence. For offline verification, pass the existing validator `.tgz` as a second argument after `gltf`. Set `GLTF_REPORT_DIR` to retain the complete official reports.
