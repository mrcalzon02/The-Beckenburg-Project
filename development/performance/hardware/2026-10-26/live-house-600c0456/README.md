# Source 600c04565cfeda3bc907689a2b0e6408993cbc2e

Same Windows/RTX5070/616.64/712x200/FOV93/Classic PS1 conditions as 6e0c6985. Same JAR SHA256 E73C016049C13685678520AF61361BF37E992F21ED0D01EB7970E6AA5AD0F851 used for all runs. Two sequential fresh-JVM repeats per case; no concurrent local tests. Adds localized door dependencies and exactly equivalent quantization lookup. Stage times and actual shadow rays now included in raw CSV.

Legacy fixed frame p95 123.107/123.738 ms; resident fixed 36.433/34.805 ms. Resident CPU p50 12.169/11.931 ms, versus prior 19.214/19.440 ms. Live-clock frame p95 38.005/51.227 ms; events (two nearest doors plus circuit switch, live clock) 53.545/55.270 ms. **Still fails acceptance**. Fixed camera static upload = zero. Real source/door events upload changed RGB. Two-way GPU depth checks passed: no missing/farther or extra/nearer pixels beyond 5cm at van, inside and upstairs.

Residual dynamic-geometry cold rays, global legacy-light cache clearing and worker scheduling remain evident. End-to-end diagnostics wait for exact completed frames and include the GL worker's 60Hz/idle scheduling. They are not displayed native FPS. Full physical case/installed hardware/other maps remain PENDING.
