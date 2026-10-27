# Beckenburg Quartermaster visuals and native branding — 2026-10-26

## Source-of-truth rule
Use the **already approved 3D item model inspection renders** in
`development/assets/equipment-catalog/previews/*.png`. Never replace an available
model with an emoji, icon-font, arbitrary silhouette, or hand-invented item picture.
`UiAssetBake` performs the reduction once at package build time, producing 96px
PNGs embedded into the game JAR under `org/beckenburg/ui/item-icons/`. The
full-resolution source files remain untouched and are still the authoritative
model comparison evidence. Dynamic 3D renderer work is never performed to paint
a shop list; the original mesh is never modified for a UI thumbnail.

## Delivered on the development branch
- Quartermaster purchase rows show approved, angled model inspection thumbnails
  and actual item ownership or stock, cost and account balance. The large selected
  preview uses the same source thumbnail.
- Equipment rack selection also uses the same art pipeline.
- A separate **model library** allows inspection of available model previews
  without misrepresenting unfinished equipment as currently purchasable.
- Buying and stocking remain under the same existing persisted `ProfileStore`
  account. Consumables may be sold back **one unit at a time** at 50% of listed
  price with disk persistence and rollback on save failure. Permanent tools and
  starters are not resold because they can be mission loadout dependencies.
- A real Castle render at
  `development/levels/previews/lighting-camera-audit/castle-lit.png` provides
  the launch splash background. The game displays actual startup milestones
  and Diagnostics instead of simulating an artificial loading sequence.
- A simple Beckenburg B-and-shield **app identity mark** is generated to
  `build/Beckenburg.ico`, included in the game's window icon, native jpackage
  Windows desktop shortcut/executable, and Inno Setup packaging. The motif
  is the game's own PSX-horror presentation, not unrelated fantasy cover art.

## Explicit boundaries
Only the actual implemented six-item legacy campaign instrument roster and
medicines remain purchasable in `CampaignStorePanel`. The larger 42-family
model registry is a visual reference until the full live economics, unlock and
field functionality are authored. No invented shop stock, arbitrary pricing, or
free inventory spawning is allowed. Sell only stock that is actually owned.

## Acceptance and remaining work
- The branch needs Windows packaged regression and desktop verification before
  being promoted to `main`; successful source commits alone do **not** mean
  a tested installer was published.
- Inspect the actual 96px thumbnails from the game JAR at 100% and 150% scaling.
  Review camera angle, object centering and dark-background readability for
  every item; make localized source/render corrections rather than introducing
  unrelated stand-in icons.
- Expand shared stock presentation to the physical Castle shelves and van
  staging only after their active state and underlying inventory logic are
  verified. Add permanent equipment buyback only after ownership, loadout and
  mission-outcome invariants are specified.
- Record build SHA, automated acceptance run, and user-tested Windows desktop
  icon/splash evidence at merge time.
