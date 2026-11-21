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
