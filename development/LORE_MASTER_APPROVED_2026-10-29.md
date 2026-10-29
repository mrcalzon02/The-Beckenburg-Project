# Matthis Rook — APPROVED Lore Master / Quartermaster artwork

**Authority:** Four-view master explicitly approved by the user on 2026-10-29.
This is the **same** Castle NPC, not a new Hunter class. His previous
`level_npc_matthis_rook_proxy` mesh was an unapproved prototype and must
not be rendered in Castle Beckenburg.

## Immutable source

- Repository: `assets/concept-art/v001/04-npcs/matthis-rook-lore-master-four-view.png`
- Google Drive: `GAME/Beckenburg Project/Beckenburg Concept Art Bridge/lore-master-matthis-rook-approved-four-view.png`
- Drive asset ID: `1GCNdX5NqFn7-ArGBDp4B3NSVugOVk_GC`
- Original resolution: **1448 × 1086**, RGB
- SHA-256: `4da4a1a16cd4d1c2c6602f718dd6b7e40e5287618fb245708e36ecf125accc34`

The approved four directions are **front, left, back, right**; the extractor packages them as `front`, `side` (left), `back`, `right`. No invented three-quarter view. Retain
his silver hair, spectacles, beard, aged olive coat, burgundy scarf,
waistcoat, shoulder bag, document case, lantern and leather boots.

## Game assets and roles

- Four runtime alpha sprites, 128×224 in
  `assets/runtime/lore-master-directions.zip` (`assets/lore_master/*.png`).
- Source-preserving extractor:
  `development/assets/extract_lore_master.py`.
- Runtime zip SHA-256: `c076ffaca59f915db305e1550652ec8c0f3100093b9cbcb6728981f3086c41de`.
- In Castle: `CastleArchive.KEEPER_X / KEEPER_Z` by the Common Archive.
- Looking at Rook and pressing E opens the **existing** campaign quartermaster.
  The adjacent archive desk keeps its existing lore-reader action.
- NPC is rendered as a directional, depth-tested world-space sprite with a
  collision footprint; he does not use the discarded GLB proxy.
- Four directions are static until the user's separate animation pass is approved.

**Do not** treat the concept presentation itself as a runtime texture. Never
discard the source image or replace approved character identity with generic art.
