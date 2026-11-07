# Hunter pain and fallen bodies

Additional derivatives of existing Cleric, Observer and Demonologist animation sheets. Existing idle/walk/crouch art is preserved. Each class has four directional standing impact poses and one overhead supine body, with complete head, hands and boots.

Original image_gen sources: `cleric.png`, `observer.png`, `demonologist.png`. The initial Demonologist board has an unacceptable translucent white backdrop and is retained for provenance only; the reviewed `demonologist-alpha-v2.png` is used by the builder.

Rebuild: `java tools/BuildHunterReactions.java` (Java 17). Transparent gutter detection rejects clipped silhouettes; deterministic crop/scale preserves source alpha and packages fifteen PNGs in `assets/runtime/hunter-reactions.zip`. The manifest records source and frame SHA-256 values. Pain: 128x224. Bodies: 384x192. `preview.png` is an asset sheet, not a runtime image.

Confirmed health loss selects a 0.4-second standing flinch. Crouched hunters retain their physically low silhouette. Confirmed incapacitation overrides pain and locomotion with a rounded oval body shell and an oval contact shadow. Dead body snapshots remain available to the hunter team; all six seats are supported and repeated down snapshots are idempotent. Site/departure resets and video/photo parity are retained. Human ghosts receive no hunter damage/down telemetry. No damage, collision or revival authority is introduced.

Runtime captures are separately labeled in `development/levels/previews/hunter-reactions-2026-11-07/`. Bodies are textured three-dimensional ellipsoid shells with depth and curved shading, not ragdolls. Original character/locomotion art remains unchanged.
