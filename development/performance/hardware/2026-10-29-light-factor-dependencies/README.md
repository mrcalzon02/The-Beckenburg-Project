# Exact light-contributor dependency gate

Candidate follows native acceptance source `10e2f4e` / evidence `1bffc8d` and reconciled main `3e41ec54`. Parent [GPU40923212823](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/40923212823) and [full40923217003](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/40923217003) both passed. Mesa explicitly completed real House, Farmhouse and School hunter/movement/retreat/debrief/menu cycles. Those are short van routes, not full investigations or physical installed acceptance.

## Rendering change

Conservative spatial light invalidation also touched material batches whose actual visible contributors had not changed. Each retained world-material batch now records the union of its base, normal and emissive probe source IDs, plus exact double factor bits. When the existing region gate requests relighting, unchanged actual factors allow the whole batch to retain its immutable vertices and GPU revision. Changed factors use the existing full RGB path. Doors/topology still rebuild probes; scene release clears dependencies. No light is removed or approximated. `-Dbeckenburg.lighting.factorDependencies.disabled=true` restores the preceding relighting behavior; dense/reference mode also disables this gate.

Exact bits matter: factors can change below the existing spatial gate's 1/1000 threshold before another source triggers the batch. The precision regression perturbs a real contributing input by one ULP and requires invalidation. Cross-scene probes are rejected. Complete sparse/dense float-buffer parity now covers Castle and all ten map types, including flicker, blackout, doors, map changes and immutable published arrays, and passes under a2GiB heap. Java17 complete GPU packaging passes. Actual RTX all-map controls pass; all12 captures exactly match the native-verified parent.

## Measured result

Windows11 / i7-14700K / RTX5070 / NVIDIA616.64 / Java17.0.12, 1280x800. Three JVM trials per variant, four Castle views,90 warmup +240 measured frames. Parent package `EBDE4C0734F00891410A70DA1583676EFDA8D2BAC90767F673809EC6B8E5929C` is shared by both variants; candidate FixtureLighting/SceneRenderer class overlay contains the new implementation. Provenance records each class and JAR hash. Parent trials run first, candidate trials second, with no concurrent local rendering or heavy tests.

Pooled p50 / p95 / p99 milliseconds:

| View | Parent | Candidate |
|---|---:|---:|
| Courtyard | 9.64 /19.55 /24.77 | 9.53 /14.20 /16.35 |
| Store | 9.89 /23.24 /27.48 | 9.88 /15.88 /22.07 |
| Library | 9.55 /14.40 /18.92 | 9.65 /14.72 /17.81 |
| Exterior stress | 10.92 /34.50 /39.46 | 11.14 /20.60 /24.12 |

Exterior p95 improves about40%; medians are essentially unchanged or slightly slower. Library p95 is slightly higher. These are completed-frame scripted camera views, including an aerial exterior stress pose, not collision-proven player routes. Cold/new-view stalls, all-location playable traversal, installed full rounds and release gates remain open. Raw repetitions, exploratory trial, captures and final-package controls follow the source checkpoint; exact-new-source CI remains pending.

Source `99baa79953be63bbc5d1058f6f63266360f40596` is pushed/readback-verified in [PR42](https://github.com/mrcalzon02/The-Beckenburg-Project/pull/42). Exact-source [GPU40927250498](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/40927250498) and [full40927255222](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/40927255222) both completed SUCCESS. The full run passed **282/282, zero skipped**, actual TCP, desktop solo and native executable acceptance. GPU logs independently confirm the new flashlight/UV guards and all three real hunter/movement/retreat/menu cycles. `source-acceptance.json` records the readback. The user explicitly authorized publication and merge; subsequent commits are evidence/metadata only.

The clean-source self-contained native executable also passes all-map controls with exit0. Final candidate and embedded JAR SHA-256 are both `482B72E910FF124A7A2E0E2D85C1F4D321CD4AB3C9181FA5CA0837B958838034`; native provenance records the exact source and clean worktree. A supplementary matched final-package trial reproduces the gain: exterior p95 36.56 to20.09ms, p99 42.76 to25.04ms. Its `package-confirmation/` records the JAR directly, without a class overlay, and supplements rather than replaces the three trials above.

Six House/School/native/Castle cycles return to643,376,200–643,915,080 post-GC Java heap bytes and exactly1158 meshes /607 textures /14,486,784 resident geometry bytes. `lifecycle.csv` preserves intermediate phases, including larger pre-GC heaps. This explicit-GC diagnostic does not measure VRAM or FPS. `image-parity.json` verifies32 image pairs:12 native map images,12 primary matched trial captures,4 exploratory pairs and4 final-package pairs. The shared gallery indexes1164 original PNGs, labels synthetic controls separately, and passes its stale-index check. `ci-ui/` also retains the six own-window captures and CSV from the passed parent case-cycle test.

## Handheld first-surface checks

The user's flashlight/UV question prompted explicit checks without changing their runtime behavior. The production GPU shader uses first-visible-surface depth; software flashlight shading is also depth-tested. UV vertex illumination calls map line-of-sight before adding color. New core regression exercises that real UV method across actual closed/open doors in House/Farmhouse/School and passes, including positive controls.

The new `--handheld-depth-review` runs the production OpenGL flashlight shader against synthetic near/far panels. RTX execution passes9248 foreground pixel comparisons across both submission orders, exact near depth, an unblocked illumination positive control and unchanged pixels beyond finite reach. The same control is included in GPU smoke and retained in CI artifacts. These synthetic panels are labeled as controls, not actual maps. Thin props, loose objects and off-camera dropped lights remain a separate audit; sampled collider visibility is not an exact first-triangle shadow guarantee.
