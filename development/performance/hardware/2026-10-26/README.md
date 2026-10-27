# First actual workstation GPU evidence

Windows 11 Pro 10.0.26200; Intel Core i7-14700K; 34,106,302,464 bytes installed RAM; NVIDIA GeForce RTX 5070 with driver 616.64, 12,227 MiB VRAM; Oracle Java 17.0.12. The existing GPU acceptance artifact from run 40433356721 supplies the exact main `f6793bda` baseline and approved resources. Artifact SHA-256 is in its manifest.

These are real OpenGL diagnostic runs at 640×360 with the existing `--gpu-smoke` route. They jump through authored viewpoints and include cold preparation. They are **not installed interactive FPS acceptance**, collision-respecting player navigation, GPU timer queries, or proof of case completion. The CSV's end-to-end time includes polling. No 30 FPS gate is closed.

| Source / run | Process exit | CPU p50 ms | CPU p95 ms | GL issue p95 ms | Readback p95 ms | End-to-end p95 ms |
| --- | --- | --- | --- | --- | --- | --- |
| main f6793bda / 1 | 3221226505 (0xC0000409) | 109.152 | 2895.992 | 10.794 | 8.318 | 2931.982 |
| unified 417980e6 / 1 | 0 | 123.079 | 2820.618 | 8.043 | 8.663 | 2857.725 |
| unified 417980e6 / 2 | 0 | 108.420 | 2852.125 | 8.454 | 6.594 | 2888.018 |
| unified 417980e6 / 3 | 0 | 118.605 | 2796.336 | 11.499 | 8.604 | 2832.004 |

The baseline printed its successful assertion summary but then crashed during abrupt JVM shutdown. That is a **failed process**, not a passed hardware run. The unified source stops/joins the GL owner before JVM exit; three complete runs now exit zero. This establishes diagnostic lifecycle recovery, not a performance improvement. Large CPU preparation/shadow costs and cold hitches remain unresolved.

Unified runs use freshly compiled game/GPU source and the approved main resource overlay, plus the reconciled original UI/rock assets. Manifests distinguish this from a freshly installed distribution. Logs, GC records, full 33-row CSVs and representative unaltered GPU captures are retained. A short socket regression ran alongside part of the final repeat; do not infer small timing gains from this series.

Next: finish unified feature exact-SHA acceptance, establish the actual native installed case baseline and collision-respecting route, then implement the gated resident House path. Keep issue #6 and all-map/hardware acceptance open.
