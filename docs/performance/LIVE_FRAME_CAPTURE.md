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
