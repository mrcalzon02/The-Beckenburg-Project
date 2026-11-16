# G2 incremental native mesh update checkpoint

Baseline: `7c4fe3be13e9f330aeaf554002ac0df97dc1ea71` (main).
Execution: Linux x86-64, Temurin JDK 21.0.13 compiler targeting Java 17 (`--release 17`), 256 MiB test heaps.

## Implementation and limits

The existing Model Editor observation path now carries a transient BitSet of changed render faces into the derived half-edge graph. Adjacency updates visit those faces instead of rescanning every face. Connectivity changes retain the full-build fallback. Unchanged native observations reuse both derived caches. UV/normal/material-only edits with the identical immutable native vertex list avoid allocating a whole-model vertex lookup and reuse source render positions. There is no new scene, persistence format or renderer.

The native update still scans faces to validate stable bindings and constructs the immutable derived Mesh. Position edits still build a vertex lookup. This does not establish regional GPU uploads, sculpting or complete G2/G3 acceptance.

## Verification

- Independent editor JAR: built without game classes/assets, Java 17 target.
- Complete host source: compiled with --release 17; only the existing LanLobbyPanel deprecation note.
- EditorMeshUpdateTest: 96 mixed native edits with shared undo/redo, normals and paired UV changes; every incremental mesh/vertex/edge/face/diagnostic count matches a fresh complete native derivation. Same-count connectivity rewires, collapse/repair and old observation immutability are included. Maximum fixture retains 360,000 half-edges and replaces only two render triangles/two corners for one grid-corner move, under 256 MiB.
- EditorTopologyProjectTest: native stable identity, history, deterministic save/reopen/recovery and disk ownership.
- EditorMeshTopologyTest --stress: 120,000 faces / 360,000 vertices and half-edges under 256 MiB.
- EditorMeshMaterialTest, EditorCornerNormalsTest, EditorGlbAuthoringTest, EditorUvTransformTest and EditorMeshEdgesTest: PASS (source attributes, GLB/OBJ/native interchange and existing human-operation backends).

## Allocation diagnostic

`EditorUpdateAllocation.java` is a reproducible diagnostic using HotSpot ThreadMXBean, not a timing/release assertion. It constructs 120,000 disconnected native faces, makes one face UV edit, then measures only observe(previous) plus topology(). Four warmups precede twelve samples. Run in separate JVMs with -Xmx256m using the current standalone JAR, then original baseline EditorTopologyProject/EditorMeshTopology classes first on the classpath. Compile those two baseline files from git show against the current JAR; compile the harness with --release 17.

Observed mean allocated bytes per update: **current 6,344,400; baseline 39,317,720** (83.9% lower). Both runs use the same fixture/JVM/heap. This is per-thread allocation for this editor operation, not total process memory, GC acceptance or game FPS.

## CI and remaining acceptance

At session start, main full-multiplayer workflow 43611015513 was terminal SUCCESS at the baseline SHA; raw manifest was not re-audited in this session. Last renderer 43344719695 was SUCCESS at a3c0047a9819dcb094dd307ff14ee12a25fa9436; installer 43410354362 was SUCCESS at c64691e19ffe0ef5c727fa921071a66fcff9d298. Those older results do not verify this change.

The mixed/max update regression is now wired into the existing standalone workflow topology step, without a new job.

Exact-source Windows standalone/native CI, installed desktop and physical hardware acceptance remain unverified. No release/tag was created. G2 remains partial; next continue the remaining granular update/interchange acceptance before advancing the human sculpt/material stages.

Setup failures retained here: initial standalone attempt found no javac; a local Temurin JDK was obtained. First extra-test compile omitted build/editor-kit/test from its classpath and could not resolve EditorMeshMaterialTest.png; corrected classpath compiled and all four tests passed. Shell git push --dry-run had no credentials; publication uses the connected GitHub API with an expected-head guard.
