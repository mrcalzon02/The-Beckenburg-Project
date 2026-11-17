# House visual timer replay and lifecycle checks

On the Core30 v0.1.335 native walking trace, all 54 solo and all 37 three-seat callbacks following a 35–64 ms render began approximately 65–72 ms after the prior render began. Each of the eight slow solo completions had 21.1–29.0 ms outside its ending render. Every moving GL frame ID in that four-pair comparison advanced by one. The internal buffers were 960×540 in the 640×360 offscreen panel. [Original Windows GPU frame rows](windows-opengl.zip).

The start estimate subtracts the renderer’s instrumented wall duration from the panel completion timestamp. The remainder can include Swing dispatch, HUD, repaint and other work. In the newer staged-row GTX arm the sustained inside stop had median 49.6 ms of preceding render work, 67.9 ms between estimated starts, and 17.9 ms outside the ending render in solo. The corresponding TCP values were 50.0, 67.6 and 17.6 ms. [Physical van exit and stop](windows-stop.md).

The Java 17 replay fed all 286 consecutive walking render durations from the four original post-cache solo native traces into real Swing timers on Linux. Each arm used one fresh JVM per trace, an independent repeating 110 ms input timer, and identical recorded durations in the original order. Each timer occupied each trial-order position once. The trace fixes the work on each frame. A faster real game can sample different camera poses, animation clocks and depths.

| Timer | Render p50 / p95 / p99 ms | >100 ms / intervals | Median wait after a 35–64 ms render | Estimated EDT occupancy | Input gap p95 / p99 / max ms |
|---|---:|---:|---:|---:|---:|
| fixed33 | 33.34 / 72.44 / 122.12 | 8 / 282 | 9.35 ms | 46.49% | 136.01 / 178.58 / 181.59 |
| fixed30 | 30.34 / 88.46 / 115.99 | 9 / 282 | 13.73 ms | 47.61% | 155.76 / 179.88 / 179.88 |
| deadline30 | 30.94 / 64.06 / 98.59 | 1 / 282 | 1.33 ms | 51.23% | 134.44 / 173.46 / 173.46 |
| deadline20 | 20.92 / 63.58 / 98.50 | 0 / 282 | 1.33 ms | 63.64% | 142.49 / 190.26 / 190.26 |

Nearest-rank percentiles pool within-process intervals. EDT occupancy divides total prescribed render duration by total elapsed replay time. The EDT parks during the prescribed work, so the figure estimates unavailable event-thread time at those costs. The input column measures time between successive dispatched timer events. This sample does not measure CPU utilization, rendering on the GPU, direct input-to-display latency or the cost of a different frame sequence.

An actual House panel verified close, suspension while hidden and resumption after becoming visible for the 30 ms repeating timer and each one-shot deadline. The hidden panel kept dispatching the separate 110 ms gameplay timer. Closing it stopped both timers, with no further visual/input ticks observed. The game fixture used the selectable 200p profile and the software renderer for this lifecycle test.

Eight exploratory House routes exercised a rendered solo hunter or one rendered hunter plus two real TCP peers on local Mesa llvmpipe. Both arms used four-lane shading, the game’s balanced 640×360 setting and the same five-second idle, van exit and 0.31 m step/hold inside route. All eight processes completed. In solo, pooled walking p95 was 64.61 ms fixed30 and 64.10 ms deadline30, and stopping p95 was 58.41 and 62.03 ms. In TCP, walking p95 was 313.02 and 293.26 ms, and stopping p95 was 166.32 and 180.81 ms. Local 960×540 attempts recorded one complete fixed30 trial and no complete matched deadline trial. Incomplete processes are excluded.

The source patches schedule one visual callback relative to the start of the previous render and yield for at least one millisecond after an overrun. Both retain the 110 ms gameplay timer. Apply either patch to the v0.1.336 panel. The replay driver accepts the original Windows archive and launches the four-arm matrix. The archive includes the complete four native source CSVs, each replay’s raw timestamps, per-trial and pooled summaries, lifecycle logs, and raw balanced Mesa traces. [Checksummed data and code](timer-replay.zip) · [SHA-256](timer-replay-SHA256SUMS)
