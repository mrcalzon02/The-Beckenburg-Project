# Castle resident art fidelity repair — source preservation and runtime derivatives

## Defect observed
The Castle Expedition Marshal, Tomas Greaves, is visibly muddy and overprocessed at close range.
Four supported resident IDs (Tomas, Elsbeth Vale, Rowan Pike, Sister Mirelle Ash) were packed as
128 × 224 directional cutouts without any source-to-derivative fidelity audit. The prior
`CastleSupportArtTest` only checked dimensions, transparency and nonempty pixels. At 200p/360p
runtime resolutions, subsequent PSX quantization can further reduce apparent detail.

## Canonical inputs
Use ONLY these unchanged, checked-in master concepts:

- `assets/concept-art/v001/04-npcs/tomas-greaves-master-concept.png`
- `assets/concept-art/v001/04-npcs/elsbeth-vale-master-concept.png`
- `assets/concept-art/v001/04-npcs/rowan-pike-master-concept.png`
- `assets/concept-art/v001/04-npcs/sister-mirelle-ash-master-concept.png`

The packaged derivative is `assets/runtime/castle-support-directions.zip`. **Never update
the master PNGs from a lower-resolution sprite, interpolated image or screenshot.**

## Rebuild process
`tools/repair_castle_sprite_art.py` reads each original master and the four currently
packaged per-resident RGBA cutouts. The OLD sprite supplies alignment cues and the alpha
silhouette only; replacement RGB samples the ORIGINAL high-resolution master directly.
The matcher tries independent X/Y scales because prior 128 × 224 extraction stretched some
narrower concept crops. The derivative is 256 × 448, preserving the existing 4:7 sprite
canvas and in-world entity dimensions.

When matches are ambiguous, a view appears in the audit as `review_needed`; the ZIP is
**not** rewritten. No independent or partial substitutions. The project does not invent
missing turnarounds or extrapolate from a face portrait. The script leaves all master PNGs
byte-for-byte untouched and records their SHA-256 hashes in the audit.

The dedicated GitHub Actions workflow `castle-resident-art-repair.yml` reports sources,
match scores, original-vs-old-vs-rebuilt PNG comparisons and hash details as a **diagnostic
artifact**. It may commit the rebuilt runtime ZIP to `main` only if every view clears the
automated match and separation checks. Git push is not considered successful unless the
generated commit is subsequently visible on the remote branch. The workflow deliberately
cannot force-push. For local review, omit `--write`.

## Runtime safeguards
`DirectionalSprites` accepts both the original 128 × 224 and the repaired higher-resolution
RGBA frames, with a consistent canvas aspect ratio. Actual world-space NPC width, height,
interaction position, collision, and source orientation are unchanged. `CastleSupportArtTest`
checks valid frame proportions and identity resolution, not an arbitrary 128px texture cap.

**The render-quality setting still matters.** 'Balanced 360p' and 'Classic PS1' intentionally
quantize and downsample the final image. To evaluate repaired source texture fidelity, compare
a Castle screenshot using Sharp/High or Ultra render detail and 'Off (original textures)'
PSX treatment against a retro-preset screenshot. This is a visual *diagnostic*, never a
release/build blocker.

## Remaining sign-off
Review every comparison image for correct direction, hair/accessories, hands, boots/feet and
background contamination. The workflow's geometric matching is an automated guardrail, not
a substitute for actual visual inspection. Once a rebuilt asset is confirmed on `main`,
run the packaged Castle smoke test with close-up shots at multiple angles. Do not claim
the in-game defect is fixed until the actual packaged client has been visually checked.
