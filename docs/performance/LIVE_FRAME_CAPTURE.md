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
