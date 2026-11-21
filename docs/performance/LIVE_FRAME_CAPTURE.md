# Live first-person frame capture

In a **hunter or player-ghost investigation**, use **Ctrl+Shift+F7** to
show/hide the existing performance overlay. Use **Ctrl+Shift+F8** to start a
bounded frame recording and press it again to stop and save the CSV. The overlay
shows recording/saving/success/failure status; held F8 repeats are ignored.
Capture continues if the HUD is hidden. The host, ghosts, multiplayer network
input and movement simulation rate are unchanged.

Default CSV folder: ~/Beckenburg/performance/ . Override it with the JVM
property beckenburg.performance.capture.dir . Each UTC-named CSV is created
without overwriting earlier evidence. Sampling uses preallocated primitive
arrays with **no per-frame file I/O or heap allocation** from the recorder.
An independent daemon writer saves the stopped capture; verify the overlay
shows SAVED before closing the game.

Each capture has at most **120,000 render callbacks** (approximately 33 minutes
at 60 FPS) and saves automatically at the cap. Records contain elapsed time,
render work, backend, GPU frame ID, new-versus-duplicate presented frames,
presented-frame interval, scene-build, geometry, effects and room. The header
includes p50/p95/p99, stalls over 33/50/100ms and duplicate readback counts.
Backend switches start a fresh interval instead of inventing a huge hitch.

This is **client render/presentation evidence**, not GPU timer-query output,
and not automatic proof of physical hardware performance. Record driver,
GPU, resolution, presets, build SHA, solo/seat count and cold/warm state with
each trace. Reproduce the same movement route across comparable test runs.

When a live case ends or the player leaves the viewport while capture is
still active, the closing view writes the unfinished trace synchronously.
This does not introduce disk I/O into ongoing game frames. Explicit F8 stop
remains asynchronous; wait for SAVED before exiting immediately afterward.
Starting a capture does **not** automatically draw the performance HUD:
turn it on separately with F7 to observe capture status, or use the normal
in-game feedback line. This keeps overlay drawing out of benchmarks unless
the tester deliberately enables it.

### Input backlog diagnosis

The recorder also logs keyboard and mouse-movement **event-queue delay**
without changing input delivery. Each frame includes the number of callbacks
dispatched since the preceding sample, the **maximum delay before those events
reached the Swing EDT** (in milliseconds), and the time from the most recently
dispatched input callback to that frame's sample. The latter is recorded as
`-1` when no new event occurred during the interval. Summary headers count
frames with input queue delay over 50 and 100ms and report its maximum.

The delay is estimated from the AWT event timestamp and local callback wall
clock, so system-clock adjustments and synthetic mouse-centering events can
affect the number. It is **not** a measured input-to-photon latency. Compare it
alongside genuine GPU frame IDs, client render intervals and authoritative
movement traces. Both calculations are local; no events, key data, mouse
positions, chat messages or network traffic are serialized into the CSV.

### Provenance and rapid recordings

Every saved CSV includes the packaged source revision (when stamped), active
renderer description, OS/architecture, Java version, selected graphics detail,
display/aspect, filter, antialiasing, PSX preset and FOV. These values are
captured once at the start of recording. A renderer description is not a GPU
timestamp query. The CSV intentionally contains no home paths, hostnames,
credentials, key identities, mouse coordinates or conversations.

Recordings started within the same UTC millisecond are saved to unique sibling
filenames (`-2`, `-3`, etc.) using exclusive file creation. A pending explicit
F8 export is also given a bounded three-second completion window when leaving
the case. Sudden power loss or forcibly terminating the process cannot guarantee
preservation; always verify the saved file exists before clearing the test run.

### Presentation cadence (independent of simulation)

In Castle **OPTIONS → GRAPHICS**, or inside a live investigation at
**ESC → Field Settings → GRAPHICS**, choose a **client frame target** of
50 (compatibility), 60 (default), 90 or 120 FPS. This is a presentation
cap, not a claim that the GPU will deliver that rate: a slow frame is never
overlapped by another Swing draw. When a draw exceeds its frame budget, its
next callback is scheduled promptly instead of waiting a second full
interval. The authoritative host world cadence and the independent
50 Hz HINPUT sender are *not changed*. Switching presentation targets
does not change movement speed, evidence, AI or collision.

Use 50 versus 60 for a controlled renderer-scheduling A/B on identical
hardware and settings. Distinct GPU readback frames, not repeated Swing
repaints, determine actual presented FPS. Every CSV includes
`presentation_target_hz` so comparisons identify this setting.

### Post-render stage diagnosis and sanctuary column precompute

The physical GTX 1660 SUPER House trace shows walking-tail latency
concentrated after geometry submission. The capture now records independent
CPU timings for characters, world/weather, sanctuary and hiding haze,
PSX quantization, graphics post-processing and haunting overlays. The live
Ctrl+Shift+F7 HUD presents these in two compact CPU-labeled rows, in both
Castle and investigation views; Castle's unused haunting stage is zero
rather than a stale House value. These are
not GPU timestamp queries; compare them with frame intervals and verified host
movement before identifying a bottleneck.

Horizontal screen ray coordinates depend only on column, width and focal
length. The sanctuary renderer precomputes them exactly once per camera
resolution/FOV instead of dividing per shaded pixel. Ray intersections,
opacity, occlusion, motion and final colors remain live. A deterministic
RGB/depth parity test covers motion, resized viewports, animation and six-frame
stop-and-go held poses with changing opaque scene depths. The existing GPU
workflow compiles and runs that deterministic Java 17 acceptance contract. To
recover the original per-pixel division for comparison, set the JVM property
beckenburg.haze.columnCache.disabled=true. A physical A/B remains necessary
before claiming a performance gain from this change.

### Doorway reachability fast reject (exact light output)

Authored non-native investigation maps now build a tiny light connectivity graph
once per real door/window topology state. This graph conservatively admits all
outdoor regions and physical window apertures, and deliberately refuses to
shortcut overlapping regions, boundary surface probes, native sites, or uncertain
emitter placement. For fully disconnected interior room components it skips
source candidates *before* the expensive light attenuation and architecture ray.
Open or indirectly connected rooms still use the full original ray/normal/prop
occlusion calculations, so the graph is not a substitute for pixel-correct
shadows or the planned bounded directional portal-flux transfer.

Set `-Dbeckenburg.lighting.portalGate.disabled=true` for the direct-ray oracle.
The `PortalLightReachabilityTest` in the existing GPU acceptance job compares
bit-for-bit RGB and normal shading on House, Farmhouse and School over actual
door/circuit transitions and asserts that the small connectivity graph is not
rebuilt on unrelated frames. `FixtureLighting.portalCandidatesRejected()` counts
source candidate exclusions, not measured GPU rays or FPS. A physical GTX
1660 SUPER A/B is still required to claim a latency improvement. Castle uses
this same FixtureLighting engine but retains full exact rays until its
architectural room IDs are available for conservative component rejection.

### Production adjacent-room doorway light flux (reversible)

`PortalFluxCache` replaces direct source-to-target-room architectural rays
for safe adjacent, non-overlapping indoor rooms with open authored doors.
It computes fixed source-to-aperture incidence on door topology revision;
subsequent per-vertex or CPU light probes reuse a short receiver-side shadow
ray across all eligible sources at the same doorway. Transmitted energy is
bounded, directional on receiving normals, retains the original source RGB
and is multiplied by live circuit/fixture factors. When door state changes,
the portal flux and per-receiver visibility cache are invalidated together.
Shut doors have no virtual light; direct same-room shadows remain exact.
Window and stair transfer, multi-hop routes, exterior zones, overlapping
storeys and Castle (without authored room boundaries) continue using exact
ray tracing. The in-game simulation cadence and ghost evidence are untouched.

For A/B: use `-Dbeckenburg.lighting.portalFlux.disabled=true` to restore the
exact pre-transport shading; independent `portalGate.disabled` still controls
the earlier conservative reachability reject. This is a **lighting model
change**, so do not claim byte-identical final RGB across open doors; compare
live screenshots and verify no new light leaks. `PortalFluxCacheTest`
asserts source incidence reuse, shared portal receiver rays, exact-static
repeats and door-state invalidation in the existing GPU workflow.

### Quantifying doorway transport in local frame traces

The performance HUD now distinguishes *actual direct shadow rays*, source
candidates rejected by the portal connectivity graph, and the number of
probes resolved by the portal-flux path. It also shows active cached transfer
count plus cumulative incident rays, receiver rays and receiver-ray reuses.
The HUD's lower graph is moved down to keep these counters readable.

Ctrl+Shift+F8 captures include separate machine-readable columns
`light_shadow_rays`, `portal_rejected_candidates`,
`portal_flux_resolved`, and `portal_transfers`. The three
`portal_*_cumulative` ray totals are **not per-frame counts**; subtract
consecutive samples to attribute work to an interval. These figures count
CPU architectural-ray decisions, not GPU timestamps or proven frames-per-second.
No extra simulation ticks, network traffic or per-frame file writes are added.

### Source-owned radius and beam exit gating

A light with no physically reachable *first* opening is now explicitly local to
its authored room: source-to-other-room candidates are skipped before the
source shadow rays or portal flux path. The cached `PortalLightReachability`
rebuild checks both a conservative **entire aperture** radius/volume bound and
hard directional beams (fluorescent, TV). It never uses only a doorway centre
sample to decide a source could not reach a jamb or lintel. Frosted dome,
table lamp and diffuser beams include nonzero backlight and are not falsely
treated as hard cones. Opaque closed doors block graph transfer; physical
windows and conservative vertical stair connections remain permitted. Moving
player flashlights and all source-internal lighting are unchanged.

For compatibility the first aperture is source-specific; paths farther along
the connected topology stay deliberately optimistic until tested multi-hop
propagation. Distal surfaces still undergo the original exact architectural
occlusion and distance checks. The prepared GPU vertex and CPU probe paths
share this same gating. `PortalLightReachabilityTest` verifies an actual open
House door with a narrow forward beam, a beam facing away, a short-radius
local-only emitter, diffuse backlight, closing/reopening and frame reuse,
alongside bit-exact oracle RGB/normal parity on House, Farmhouse and School.

### Second and subsequent doorway stop test (per-light, per-portal)

Authoritative doors now form a bounded per-source flood. Every edge is
individually admitted only if the *original light's* sphere intersects the
full physical opening, and hard-zero beam cones can illuminate any part of
that opening. Opening one doorway no longer grants that light access to
every room in the connected component. Open external zones are treated
conservatively because they have no indoor wall between them; stair and
overlapping upper/lower room ambiguities still fall back to exact rays.

The flood runs once per door/window snapshot revision; no portal graph walk
or cone test occurs in the normal vertex or CPU light sampling loop.
`PortalLightReachabilityTest` now exercises a REAL connected House path
through two open doors, with light that reaches door #1 but physically cannot
reach door #2, then compares bit-exact RGB and normals against the ungated
direct-ray oracle with ALL authored House/Farmhouse/School doors open.
`sourceBlockedConnectedRooms()` counts live emitter/room graph pairs pruned
specifically by each light's own geometric limits, not frame milliseconds.

### Exact spherical spatial light index (not a cube)

`FixtureLighting.eachInfluenceCell` now registers a light source only in
2/4/8-metre index cells intersecting its actual light sphere. The former
cube broadphase allocated candidates and performed later rejections in
corners of that cube where a sphere could never illuminate a receiver.
The same radius-safe cell set is used for light intensity invalidation.

No physically reachable point can be removed: the minimum distance to the
complete cell AABB must exceed the authored radius to prune it. The
`LightSphereCellIndexTest` exercises real source lists from House,
Farmhouse and School and randomly samples interior points for all emitters,
asserting membership plus an exact count of removed cubic cell entries.
This is an exact geometry and memory/candidate optimization, not a
lighting visual approximation; normal sample and GPU parity still apply.
