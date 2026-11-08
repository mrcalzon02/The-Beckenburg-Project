# Shared heavy fog and skybox

Owner-directed urgent all-level implementation. Every Castle and investigation world frame now receives the same radial depth fog after either software or actual OpenGL world composition. The near6m retains detail; the baseline fully converges by24m. Mission/weather density can thicken the baseline, never disable it. Fog never writes or changes depth, collision, map selection or network authority.

The existing world-anchored weather sky supplies the skybox; its horizon and lower hemisphere now converge smoothly to the same fog colour as distant geometry. Independent hard hill bands, duplicate software-triangle fog and native-only haze are removed. No missing physical barrier or unfinished terrain is accepted because fog conceals distant geometry.

Validation:9focused checks (atmosphere controls, sky anchoring, mission/recorded/media weather, GPU readback, safe-area depth, water and Castle integration), one clearly labeled synthetic GPU-composite control,24actual runtime frames in each renderer plus2exterior Castle courtyard frames in each:52total. Actual OpenGL driver: NVIDIA GeForce RTX5070 / OpenGL3.3 /616.64. All5sheets were viewed. Current12runtime sources are captured;17source-only packets are explicitly not reported as loaded or unlocked. All inherit the shared path on integration, as do custom maps.

`capture-manifest.json` binds image hashes and all29roster coverage decisions. Runtime CSVs contain actual cameras and fog/sky pixel counts. `software-checks.json`, `gpu-checks.json` and courtyard records report actual outcomes. Source hashes bind exact implementation and test drivers. Tests use isolated in-memory preferences and workspace temp/home; no editor UI or timed round run.

Local package generation and exact class payload checks are recorded separately. Passing local captures/package are not full exact-source CI, installed multiplayer or sustained hardware performance acceptance. Main recovery/full rebuild gates remain open; the fog source is published on the rebuild branch, with actual source/readback receipts.
