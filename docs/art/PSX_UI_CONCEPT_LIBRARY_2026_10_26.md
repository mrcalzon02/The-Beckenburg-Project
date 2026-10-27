# Beckenburg PSX UI concept library — 2026-10-26

**Status: reference and review only. No game integration or release is claimed.**

This is an intentionally preserved exploratory art batch intended to inform *The Beckenburg Project* UI, not define gameplay mechanics. The generated art repeatedly presents non-authoritative prices, map names, dates, ghost identities, and button labels. **Those details are not canonical.** Use the repository's game data and implemented UI flows instead.

## Source archive, manifest and preview (verified uploaded to Google Drive)

- [Drive art-library folder](https://drive.google.com/drive/folders/1gofTm9DG7NRhhnOWXoDRLhFmPiZFtKET)
- [Complete versioned 47 MB ZIP](https://drive.google.com/file/d/12poAs28WE1tEZ7qf4TTcxJmHKoshY09G/view)
- [JSON manifest — source hashes, 55 crop bounds and candidate metadata](https://drive.google.com/file/d/1ohJQM8saEb32sKXieglvOWRIGGmEAmy9/view)
- [Sortable slice CSV](https://drive.google.com/file/d/13dwNV7E0Y_OswHlqcLCuigLsbwWISk8c/view)
- [Text-free procedural UI skin preview](https://drive.google.com/file/d/1kJ-8xG8-19ERIkume66siXrUYcaFOQl-/view)
- [Reproducible local slicing script](https://drive.google.com/file/d/1TFUGUnYT4brqsxKRYqaFTRvVatl956wc/view)

The archive contains **11 original Beckenburg-branded PNG concept sheets, 55 extracted reference panels, and 21 optional text-free button/panel/slot skins**. All were generated/sliced locally, and the uploaded ZIP and sidecars were verified by listing the Drive destination. ZIP SHA-256: `9055a8884c5d622a3107bc349794268337646756bf37d0fe41721527e72e5efc`.

## Asset policy

1. Every generated image or crop starts **UNAPPROVED / reference-only**. Mechanical slices are not alpha-cleaned, and may include adjacent labels or panels.
2. Game title is **The Beckenburg Project**. Reject unrelated fake titles and platform branding.
3. The project owns its actual gameplay copy, evidence mechanics and 22-map campaign. **Never import** invented lore, map order, ghost names, payout, prices, ratings or required evidence counts from generated screenshots.
4. Existing real GLB equipment is the source of truth for item portraits; see [owner asset integration](../OWNER_ASSET_EQUIPMENT_INTEGRATION.md). Make reproducible 3/4-angle renders instead of substituting imaginary AI props.
5. Keep decorative textures **text-free** and render wording with the game font at runtime. Buttons must support idle, hover, keyboard/controller focus, pressed, disabled and accessible readability. Nine-slice the borders.
6. Approve and test one UI family at a time (start menu, case selection, journal, shop, loadout, garage, ritual, HUD, results). No blind overlay of concept composites over functional controls.
7. No cosmetic-only UI test gate may block functional builds. A runtime change is not complete without actual in-game screenshots and working controls.

## Visual-language draft (unapproved)

- Material family: deep midnight indigo charcoal; distressed brushed-steel trim; sparse dark-red alarm/selection states; parchment for notes and occult documentation; muted candle-amber in scenery.
- Forms: square-corner PSX-style frames, 1–2 px bright inner edge, bevel/shadow, lightweight grunge, compact tightly spaced information rows.
- Fonts: existing in-engine supported fonts, no generated labels or substituted unreadable glyphs.
- Interaction: selected/focused states remain visible without color alone; separate error, warning and success treatment.
- Image assets: preserve authored castle / van 3D silhouette. Use existing level cameras for map cards and runtime device renders for shop/inventory slots.

## Selection and implementation ledger

| ID | Family | Starting decision | Integration guard |
|---|---|---|---|
| UI-REF-01 | Main menu & splash | Candidate for art direction | Use actual castle/HQ screenshot, dynamic menu copy |
| UI-REF-02 | Navigation & button states | Candidate for texture extraction | Real keyboard/gamepad/mouse states; 9-slice |
| UI-REF-03 | Case selector & briefing | Reference only | Canonical maps/order/data; no fake prices |
| UI-REF-04 | Store & loadout | Reference only | Existing purchasable registry and real GLB icons |
| UI-REF-05 | Evidence journal | Reference only | Manual investigator evidence, no auto-certification |
| UI-REF-06 | Rituals | Reference only | Canonical Banishment/Cleansing/Absolution |
| UI-REF-07 | Van/garage | Reference only | Real van geometry and working equipment |
| UI-REF-08 | HUD/alerts | Candidate for treatment | Clear prompts, sightlines, low overhead |
| UI-REF-09 | Loading/save/results | Reference only | No pretend PSX memory-card dependence on PC |

### Approval pipeline
`reference_only` → `selected_for_refinement` → `model_and_text_verified` → `implemented_behind_flag` → `render_verified` → `approved`.

**Repository/Drive division:** all original PNG bytes and 55 sliced references reside in the verified Drive archive. This GitHub commit records provenance, workflow and design rules. **Do not misrepresent a GitHub reference or manifest link as committing the PNG binaries to the repository.** The sprites can be imported under `software/beckenburg/assets/runtime/ui/` only after approval.
