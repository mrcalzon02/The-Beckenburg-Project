# Castle resident animation and full-boot standing repair

All five residents have idle standing art, two walking strides and a crouch, each with four source directions. Original concept masters remain unchanged. These are generated derivatives of those masters, not direct RGB extractions.

- `*-reference.png`: original packaged standing cutouts used as animation references.
- `<identity>.png`: original image_gen 4-column / 3-row walk A, walk B, crouch boards.
- `*-idle-v2.png`: image_gen standing repairs with complete boot toes, heels and soles.
- `preview.png`: animation asset review sheet, with crouch shown at runtime height.
- Runtime captures are separately labeled under `development/levels/previews/resident-animation-2026-11-04/`.

Rebuild with Java 17:

```
java tools/BuildCastleResidentAnimation.java
java tools/RepairCastleResidentIdle.java
```

Extraction locates transparent gutters, rejects cut silhouettes at cell borders, preserves source alpha and writes deterministic PNG ZIP payloads. Runtime resources and SHA-256 manifests are in `assets/runtime/`. The standing repair preserves existing archive paths and loader formats: four support NPCs at 256x448 and Rook at 128x224.

Residents walk at 0.48m/s within 0.75m of their service anchor, sweep navigation/furnishing clearance, stay in the same room, pause upright within 3.4m of an approaching player and reset on clock/scene discontinuity. Rendering at the same simulation time is idempotent. Dialogue aiming, collision and nameplates use current body positions. These are local Castle work routines; no investigation or quest authority is introduced.

The shared renderer projects soft circular black contact shadows at actual feet/support elevation. Ghost hover and manifestation motion do not move their ground shadows. Exact sprite ray/card depth and floor endpoint projection prevent pitched shoe clipping. The original projection is retained only as a compiled negative-control test fixture in ignored build output; it fails the new floor-contact check.
