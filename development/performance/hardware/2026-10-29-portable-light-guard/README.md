# Portable-light guard follow-up — 2026-10-29

Runtime source: `36a41c7086b7f1fbf8b3e257912de58846a9035a` in PR #37. Parent renderer: `f82fb0857ec7b5ba984c9d33e034cda3cd904dbf`. Approved meshes, textures, gameplay and backend are shared. The parent SceneRenderer source was extracted with `git archive`, compiled separately with Java 17, and placed first on the baseline classpath; all other classes and assets use the candidate GPU JAR.

GPU JAR SHA-256: `87F647AD0D4FEFC22059177FBE0D26D1264D03EFA22FB42026D70016AAA3A34A`.

Local `build.cmd package` passed with installed Java 17.0.12 and Blender 5.0. Offline Maven GPU packaging passed. `WorldResidencyTest` passed Castle and all ten map types, including both portable-light kinds powered off/on/off, UV, buffer reuse, doors, resize and editor revisions. Prepared scenery exact pixels, prepared lighting and camera visibility checks passed.

The actual RTX 5070 / NVIDIA 616.64 OpenGL review passed Castle and all ten map types. Every unchanged repeat uploaded zero geometry bytes. Castle reused 18,251,520 bytes on its initial repeated frame. Live TV/map updates changed their pixels with zero geometry uploads; unchanged texture repeats uploaded zero bytes. Raw evidence is in `frames.csv`, `live-textures.csv` and `captures/`. Captures are runtime GPU diagnostics, not whole-map visual acceptance.

## Small matched House measurement

640x400, existing PSX preset, 90 warmup and 120 measured frames per view, identical scripted camera sweep. One trial per candidate, with a separate local regression process active during both trials. These results establish a small allocation change and image parity; they do not establish a statistically repeatable FPS gain or installed gameplay acceptance.

| View | Parent p50 / p95 / p99 ms | Candidate p50 / p95 / p99 ms | Parent / candidate median allocated bytes |
| --- | --- | --- | --- |
| Van | 9.06 / 11.11 / 12.35 | 8.55 / 11.34 / 12.79 | 5,954,560 / 5,951,968 |
| Interior | 9.14 / 12.59 / 13.43 | 8.99 / 11.38 / 13.12 | 6,657,912 / 6,648,832 |

Final before/after PNG SHA-256 hashes match exactly in each view: van `086E397C39C92AF14082C929BDDAF1796037EACDCFF2C01939B1D8DBC93BF6D8`, interior `A64AE7F4A6879CBDA751B921BF7BDD27B47F0AA7B438657E13FA7F00B34B8F72`. Raw matched CSV and environment records are in `before/` and `after/`.

Source acceptance workflows: [GPU 40869821516](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/40869821516), [full multiplayer 40869825759](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/40869825759). Both were running when this evidence was recorded; their outcomes must be read independently. Full A–G, installed interaction and a released installer remain pending.
