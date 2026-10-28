# Equipment and menu workflow — 2026-10-28

The start menu selects Campaign, Multiplayer, Solo Contract, Castle, Options or Extras. Mission configuration no longer occupies the start page. Options is a dedicated page with explicit apply/back controls and an in-window, 15-second display-reversion prompt.

Campaign and multiplayer preparation panels are children of the actual CastleHubPanel service overlay. Start-menu shortcuts enter Castle before opening their workstation. No lobby or mission-view implementation allocates another Window. Returning from a mission restores the Castle workstation.

Campaign preparation has Contracts, Team, Equipment, Archives and Site Pictures. Solo and Co-op use the actual shared equipment/network field implementation. Solo binds a private loopback lobby; Co-op can host guests. A saved chapter supplies the real site, fixed assigned ghost and evidence seed. Unbuilt or locked chapters cannot dispatch.

## One equipment library

`EquipmentCatalogPanel` is the sole equipment library for campaign and multiplayer, including the multiplayer van rack. There is no separate Quartermaster versus Model Library browser. Both modes display the identical 129 item-tier entries, use `EquipmentTiers` prices/capabilities, and show each tier's actual singular icon. Campaign purchases and four-slot preparation persist in the host's profile. Multiplayer shows the host-authoritative contract wallet, stock and equipment; career receipts remain in the separate multiplayer profile. Host snapshots acknowledge network purchases rather than granting optimistic local ownership.

Existing campaign saves migrate their owned instruments and prepared slots. The expanded campaign catalog persists ownership through tiers I–III. Purchases cannot skip tiers or double-charge an owned tier. Loadout changes require owned hardware and four distinct items. The field host receives these saved tiers, capacities, batteries and slots. Campaign medicine consumption saves before healing. Paid mission hires and ordinary equipment use the same campaign funds.

## Campaign team and guest rules

The host occupies one of six hunter slots. Reserve zero to five AI hunters at £180 each for **one mission**. Updating a booking downward refunds it. At dispatch, human guests occupy normal seats first; unused hires are refunded, and only deployed hires are consumed. The hired controllers issue ordinary bounded movement and thermometer inputs without access to secret ghost decisions or automated journal confirmation. The host's selected discipline and earned health/ward benefits apply to the mission.

Campaign guests cannot select a human ghost seat, start the host's case, earn a normal multiplayer payout from that campaign, or advance their own campaign save. A resolved campaign case creates one idempotent receipt in the host's campaign; retreat records a visit without clearing the chapter. Failed settlement saves remain retryable from the owning lobby. Returning from the 3D view stays inside the game window.

## Images and usability repairs

Blender Cycles generated 129 independent 512×512 RGBA images from `EquipmentModelCatalog`'s actual GLBs. Camera framing uses the rendered alpha bounds and re-renders to make the object fill about 84% of the long dimension. Source materials and textures are preserved. Static fixtures correctly retain their source aliases. Store cells and the large preview load `item_t1`, `item_t2` or `item_t3`; three-tier contact sheets are only review artifacts.

The licensed PSX frame, tab and button artwork remains in use. Game prompts use the existing window's glass pane rather than separate dialog windows. The PSX text-field border now excludes its centre when painting: previously its opaque image covered address, nickname and password text. Purchase protocol frames use stable medicine enum identifiers instead of decorated display labels. Minimum-window captures cover 760×510 as well as 1060×725.

## Verification and limits

The checked-in JSON report records focused regression outcomes. `EquipmentTierIconTest` checks every packaged tier image, resolution, close framing, clipping and accidental reuse between different source models. `CampaignCoopLobbyTest` uses actual TCP clients for host authority, six-seat bot substitution/refunds, guest isolation and pre-match purchasing. `CampaignEmbeddedDesktopTest` opens a real solo campaign lobby, deploys two bots with saved catalog equipment, receives advancing 3D frames, persists a retreat receipt and returns to the same window. `BeckenburgDesktopE2ETest` also visits Options, checks an in-window prompt, traverses multiplayer, and starts the real House scene.

These are scoped local checks, not full-release or GPU/performance acceptance. The full regression manifest, installed GPU path, physical two-machine co-op and remote workflow results remain independent recovery gates. Source archives excluded by the initial sparse checkout were restored locally for the clean package build. Initial GitHub operations failed DNS resolution. Connectivity later recovered and the icon checkpoint was pushed; implementation delivery is recorded in the recovery checkpoint. Remote workflow outcomes remain separate from local verification.
