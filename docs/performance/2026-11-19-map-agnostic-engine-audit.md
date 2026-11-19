# Beckenburg map-agnostic engine performance audit — 19 November 2026

## Purpose and release policy

Improve actual smoothness and resource efficiency in the Java 17 engine without slowing player/ghost locomotion, AI, physics, evidence timers, or the authoritative host. An FPS improvement is not successful when callback gaps, input latency, simulation coverage, visibility, or live game behavior regress. Benchmark timing/pixel measurements are advisory diagnostics; required build/launch, multiplayer, updater, and genuinely broken gameplay remain functional gates.

The starting point for this audit is main at \`d63cff9422935fa7ab7860e0ace0613249e38c4b\`. Subsequent source and tests are independently identified by the commit that adds this report. Prefer current main before each comparison. Do not reuse an old captured camera route as proof of newer level geometry.

## Actual code-path findings, not wishlist items

| Cost | Existing implementation | Concrete next work |
| --- | --- | --- |
| Frustum and object checks | \`SceneRenderer.faceInFrustum\` originally recomputed four trigonometric functions, FOV ratios and allocated \`new Point[]{...}\` per tested face; \`shellChunkInFrustum\` and \`objectInView\` repeated the camera math | Reuse per-pose camera basis and FOV projection, avoid per-face temporary vertex array. Verify exact old/new conservative classifications for changing pose, FOV, render size and near plane. No visibility threshold change |
| Distant native-site pixels | \`SceneRenderer.render\` cloned sky pixels and \`DistanceDetail.apply\` cloned completed world pixels. At 960x540 the two per-frame clones allocated 4,147,200 bytes before array overhead | Keep two independent renderer-owned scratch buffers (sky and world source) per framebuffer size; copy each frame. Prove bit-exact output on repeated/changing frames and 0/near/far/invalid depths |
| Static OpenGL texture state | \`OpenGlWorldRenderer.draw\` called \`texture(material).bind(0)\` for each batch, including consecutive identical immutable material IDs | Skip only redundant binds of consecutive non-imported static materials. Never skip a dynamic monitor/GLB binding. Add bind/avoided counters and driver opt-out \`-Dbeckenburg.gpu.staticBindReuse.disabled=true\` |
| GPU frame transfers | Live GL path reads both RGBA bytes and floating-point depth synchronously every frame with \`glReadPixels\`; GL worker also converts into Java \`int[]\` and \`float[]\` for the Swing compositor | At 960x540, 4,147,200 bytes/frame of base readback, ~207 MB/s at 50 fps before synchronization, CPU copies or allocation. Investigate driver-supported asynchronous pixel-pack buffers with a bounded ring and exact scene/frame IDs. **Never add control delay or accept wrong-pose results to claim FPS** |
| UI/input frame contention | \`LanWorldPanel.drawVisibleWorld\` and \`LanGhostPanel.drawVisibleWorld\` render on Swing event thread; pointer/key events and render callbacks compete on the same EDT | Profile actual input callback gaps, paint time and render-work stages. Move heavyweight rendering off EDT only behind immutable snapshots, correct network/evidence projection, and proved lifecycle/shutdown tests |
| Network host contention | \`LanHost.serve\` handles each command under the same \`mutex\` as the 100ms \`tickSafely\` simulation; the tick performs many ghost, weather, fixture, lighting and broadcast operations while holding that lock | Instrument host mutex wait and hold times before refactoring; separate expensive derived work from the authoritative state transition, retain action ordering, privacy and anti-replay; never reduce tick or legal action frequency |
| World meshes | Stationary shell and scenery use resident VBOs, revision checks, region-aware lighting and frustum rejection. \`lod\` values in these paths predominantly mean lighting/subdivision LOD, not general asset mesh LOD | Introduce authored static geometry LOD/impostors with projected pixel thresholds, hysteresis, real lower triangle counts and collision-independent world physics; do not decimate moving doors, stairs or ghost/evidence silhouettes |
| Textures | The opt-in \`RuntimeTextureMipPolicy\` mip chain applies only to immutable LevelMaterials. Live screens, imported GLB alpha and labels are not covered | Profile GPU upload costs, residency and visual aliasing. Extend only after proving source UV padding, alpha coverage, screens, labels and close-range image parity. Mipmap memory is extra, not free |
| Existing useful work | Retained static geometry, per-material/shell cache invalidation, shader preparation, CPU/GPU stage timings, visibility culling and bounded GPU readback already exist | Preserve accepted gains and compare against current source/driver; do not replace real rendering with repeated cached positions or fewer gameplay updates |

## Immediate code changes

- Reuse camera trigonometry/projection on all authored face, shell chunk and generic object frustum tests. Maintains the exact original signed plane checks and near/horizontal/vertical margins.
- Reuse both sky/background and distance-filter input buffers per SceneRenderer. Removes both full-frame native-site clones; resize invalidates the two buffers. Legacy public overloads remain and pixel output is unchanged.
- Skip redundant sequential static-material texture bindings on GPU, reporting real attempted/avoided calls. A JVM flag restores old binding behavior for driver A/B. No change to monitor/alpha/GLB texture updates.
- Fix one GPU diagnostic's false assumption: an authored map with no active alarm clock or flashing fire alarm cannot be expected to change geometry between ticks 27 and 28. Actual dynamic fixtures still require exact change at their animation boundary.

## Hardware and game acceptance

For each candidate, use four counterbalanced A/B pairs on physical Windows GTX 1660 SUPER (Core30) and a Mesa/CPU correctness control; use matching SHA, Java 17 runtime, render size, preset, driver, map revision, route, weather/lighting and mode. Each trial must preserve one uninterrupted moving hunter/ghost and at least one three-seat TCP role set. Add other authored locations to functional/visual smoke for map-agnostic coverage; use the latest rebuilt House, not a retired reference.

Collect actual distinctive framebuffer IDs, p50/p95/p99/max distinct frame intervals and counts >33.3/>50/>100ms, GL draw/readback/conversion wall stages, shell/scenery triangle count, uploads, texture binds, CPU and GC allocations, process memory, first-run shaders/assets, thread contention, input-to-host and input-to-visible-pose latency, control gaps, collision/recovery, walking meters/second, ghost hearing timing, van map readability, UV/photography/ghost manifestations, and complete evidence/ritual settlement.

Render output must not be silently weakened: near/far wall geometry, floor/stair continuity, door/window opening, van CRT text, UV marks, DOTS/ghost visibility and other functional clues remain identical or explicitly reviewed. The benchmark target is sustained smooth control and consistent pacing, not just a mean FPS headline. Distinguish monitor scanout from offscreen GL consumption; do not invent GPU timestamps from CPU GL-wall measurements.

A test failure is recorded as failure, not reclassified as performance success. Preserve CI artifacts and the last known playable baseline; no force-push, retagging, or blanket cancellation of unrelated workflows.
