# Hunter pain and fallen-body acceptance

Baseline: `62c6a66f` on the existing development branch. The isolated 428-file Java 17 main-source export excludes independent pending sanctuary/ghost changes. Its staged runtime source matches the tested export.

Seven exact-source checks pass: HunterReactionSpritesTest, HunterReactionsNetworkTest, HunterSpriteAnimationTest, LanHunterAnimationGameplayTest, LanIncapacitationTest, CrouchNetworkTest and CameraSnapshotTest. See `exact-*.log`.

- Fifteen transparent sprite resources: four pain directions and one overhead fallen body for each of Cleric, Observer and Demonologist.
- Actual TCP host damage resolution sends a timed pain event only for real health loss, then a downed state after incapacitation. Downed BODY snapshots persist; all six seats are accepted; duplicate snapshots preserve interaction feedback. Video/photo state stays consistent. Human ghost clients receive no hunter damage/down telemetry.
- Existing idle/walk/crouch, host damage/clearance/movement authority and original hunter art are preserved.
- Bodies are horizontal floor-plane textures with contact shadows and actual depth rejection for walls/sky/floor holes. They are not ragdolls. Crouched hunters retain their physical low silhouette during pain.
- Review includes twelve raster body views and six actual House pain/body captures per backend. `exact-OpenGL-review.log` confirms the physical NVIDIA RTX 5070 OpenGL 3.3 renderer using the isolated core source and the existing native backend.

Captures: `development/levels/previews/hunter-reactions-2026-11-07/`. `model-production/` is asset review; `raster/` is a synthetic floor control; `runtime/` and `runtime-gpu/` are real gameplay scenery. The initial Demonologist generated board's translucent white backdrop was rejected; only its separate alpha-v2 repair is packaged.

Remote main inspected at `e1b49eb780a3061ef47ea67dfd9747d7ff378497`: installer `42418635872` SUCCESS; full round `42418636058` still running. Latest inspected renderer run `42395022669` SUCCESS at a different SHA `3a44a663`. These runs predate this implementation and do not certify it or an installed update.

DEV-267 records the feature defect; DEV-268 fixes pre-existing Windows-1252 punctuation bytes in the committed Markdown ledger and recovery plan. No older-map slope/ragdoll acceptance or release installer update is claimed.
