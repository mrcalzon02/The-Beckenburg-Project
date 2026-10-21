# Beckenburg: directional character visual coverage

Authority: `The-Beckenburg-Project` on `main`. The homebrew repository is frozen.
Artwork selection policy accepted 2026-10-20: **use each identity's approved
base design for unfinished alternate-gender or alternative-model runtime art**.
No fabricated alternate-gender turnaround frames or art-style substitutes.

## Current runtime binding

| Family | Runtime identities | Source | Current 3D presentation |
|---|---|---|---|
| Hunters | Cleric, Observer, Demonologist | `assets/concept-art/v001/01-hunters/*-character-profile.png` | Individual transparent 4-view static billboards from `character-directions.zip`; selectable class-specific visual through the LAN lobby |
| Original ghosts | Possessive, Poltergeist, Demon, Yokai, Banshee, Wraith, Shade, Specter, Spirit, Hantu, Devil, Mimic, Twins | Approved concept profiles; `ghost-sprite-expansion-v3.zip` | Identity-specific statically held idle artwork selected by camera-relative heading, not simulated walk frames |
| Ancient ghosts | Ancient Possessive, Poltergeist, Demon, Yokai, Banshee, Wraith, Shade, Specter, Hantu, Twins | Individual green-tinted variants already bundled in the v3 archive | Distinct Ancient art, **not** ordinary parent sprite and not tinted twice |
| Elder ghosts | Elder Blade Demon, Elder Plague Demon, Elder Nothic | Three approved Elder master concept PNGs | Individual four-view static billboards from `character-directions.zip`; body scale is larger than a Hunter |

There are **29 current runtime identities**: three Hunters and 26 ghosts. They
contain 24 new approved-master-derived PNG views (6 identities × 4),
plus the existing 23 original/Ancient ghost atlases and legacy viewer support.
The new extraction pipeline records source SHA-256 hashes, image dimensions,
individual view hashes, PNG alpha and review board at:

- `assets/runtime/character-directions-manifest.json`
- `development/verification/character-directions-preview.png`
- `tools/build_directional_art.py`

**Art acceptance limitation:** approved master images are the canonical designs.
The new crop/alpha extraction is generated deterministically, but requires
a game-resolution visual review for cropped weapons, black matte, silhouette and
relative body size. Passing PNG/depth tests is not the same as accepting final
sprite art. The 8-state v3 ghost atlases are **not** approved articulated animation
loops: the renderer presently samples approved static idle views while movement
and headings remain host-authoritative.

## Variant binding contract

The `CharacterVisuals` registry tracks `BASE`,
`ALTERNATE_GENDER`, and `ALTERNATIVE_MODEL`.

- `BASE` maps to its own identity with no substitution.
- Incomplete Hunter variants resolve to their **own class's** base appearance.
- Incomplete original/Ancient ghost variants resolve to their **own ghost
  identity's** base appearance; Ancient green identity is preserved.
- Elder variants cannot be requested because the master list does not contain
  alternate-gender Elder art.
- The lobby only offers three currently complete Hunter base choices. We do
  not display an unfinished gender variant as if it were a separate selectable
  visual or introduce false distinctions between actual art.
- Equipment, ghost species, evidence, ability, rite and damage mechanics are
  never derived from an art choice.

## Future acceptance gate (per one identity/variant)

1. Locate the accepted master and approved separate variant board in the Drive
   source bridge. Confirm variant's ID, proportions, handedness and equipment.
2. Author transparent front, side, back and rear three-quarter cutouts as a
   **separate named appearance**, retaining design continuity and foot pivot.
3. Add provenance/checksums to a variant manifest; do not replace the base.
4. Add the exact four-view runtime archive entry and binding, verified in an
   isolated packaged Java 17 test.
5. Capture actual in-world orbit and walking/pursuit screenshot(s) from
   GitHub-hosted Windows acceptance; visually approve that appearance.
6. Only then permit UI selection of the new variant. Full frame animation is a
   separate later production step.

## Privacy and scene rules

Network Hunter cosmetics are announced only to Hunter clients. A hidden ghost
never sends species art information until the host authorizes a visible
apparition, and the ghost lobby preview remains an unrelated neutral disguise.
Ghost imagery never constitutes hard evidence by itself. Character quads must
respect the exact same Z-buffer and line-of-sight rules as the level geometry.
