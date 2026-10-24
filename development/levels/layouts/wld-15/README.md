# MV Saint Elara

**WLD-15 · Beckenburg coast and hinterland · Measured rough site / metres · rough layout v1**

A retired catcher/processor permanently moored for investigation. Processing, cold storage, crew life and bridge occupy different decks; the ship does not sail.

[Measured floor plans](plan.svg) · [PNG preview](plan.png) · [Editable coordinates](layout.json) · [Volume study](blockout.obj)

Planning only. Bounds and links are reservations; walkability, shell construction and gameplay remain unverified.

## Spaces

| ID | Space | X/Z bounds (m) | Standing Y / height | Function |
|---|---|---|---|---|
| stage | Quayside van apron | [24, 80, 40, 92] | 0 / 3 m | Quayside van apron |
| entry | Quay cargo lane | [24, 76, 40, 80] | 0 / 3 m | Quay cargo lane |
| quay | Mooring quay | [24, 64, 40, 76] | 0 / 3 m | Mooring quay |
| portpier | Port service pier | [16, 40, 22, 72] | 0 / 3 m | Port service pier |
| aft | Aft working deck | [22, 40, 42, 54] | 0 / 3 m | Aft working deck |
| processing | Fish processing hall | [24, 8, 40, 30] | 0 / 3 m | Fish processing hall |
| galley | Galley | [24, 30, 32, 40] | 0 / 3 m | Galley |
| mess | Crew mess | [32, 30, 40, 40] | 0 / 3 m | Crew mess |
| side | Starboard deck walkway | [40, 8, 42, 40] | 0 / 3 m | Starboard deck walkway |
| fore | Foredeck | [24, -2, 40, 8] | 0 / 3 m | Foredeck |
| engine | Engine room | [24, 24, 40, 40] | -3 / 3 m | Engine room |
| hold | Chilled fish hold | [24, 4, 40, 24] | -3 / 3 m | Chilled fish hold |
| pump | Bilge pump room | [24, -2, 40, 4] | -3 / 3 m | Bilge pump room |
| crew | Crew bunks | [24, 30, 40, 40] | 3 / 3 m | Crew bunks |
| bridge | Wheelhouse | [24, 18, 40, 30] | 6 / 3 m | Wheelhouse |
| net | Net and gear store | [24, 8, 40, 18] | 3 / 3 m | Net and gear store |

## Routes and retreat

Two declared graph cycles reserve retreat options. Optional tower/bridge branches must never host mandatory evidence without separate fairness review. Actual physical routes and timing remain unaccepted.

- Loop: quay → aft → portpier → quay
- Loop: processing → fore → side → aft → galley → processing

Every listed loop is an authored graph cycle, not a verified physical pursuit route. Cyan lines require later wall openings, surface support and clearance review.

| Link | Zones | Clear width | Route / construction note |
|---|---|---|---|
| route-01 | stage ↔ entry | 1.5 m | Authored circulation reservation; actual wall apertures, support, swept clearance and sightlines require review. |
| route-02 | entry ↔ quay | 1.5 m | Authored circulation reservation; actual wall apertures, support, swept clearance and sightlines require review. |
| route-03 | quay ↔ aft | 1.5 m | Authored circulation reservation; actual wall apertures, support, swept clearance and sightlines require review. |
| route-04 | quay ↔ portpier | 1.5 m | Authored circulation reservation; actual wall apertures, support, swept clearance and sightlines require review. |
| route-05 | portpier ↔ aft | 1.5 m | Authored circulation reservation; actual wall apertures, support, swept clearance and sightlines require review. |
| route-06 | aft ↔ galley | 1.5 m | Authored circulation reservation; actual wall apertures, support, swept clearance and sightlines require review. |
| route-07 | aft ↔ mess | 1.5 m | Authored circulation reservation; actual wall apertures, support, swept clearance and sightlines require review. |
| route-08 | galley ↔ processing | 1.5 m | Authored circulation reservation; actual wall apertures, support, swept clearance and sightlines require review. |
| route-09 | mess ↔ processing | 1.5 m | Authored circulation reservation; actual wall apertures, support, swept clearance and sightlines require review. |
| route-10 | processing ↔ fore | 1.5 m | Authored circulation reservation; actual wall apertures, support, swept clearance and sightlines require review. |
| route-11 | fore ↔ side | 1.5 m | Authored circulation reservation; actual wall apertures, support, swept clearance and sightlines require review. |
| route-12 | side ↔ aft | 1.5 m | Authored circulation reservation; actual wall apertures, support, swept clearance and sightlines require review. |
| route-13 | processing ↔ engine | 1.5 m | Authored circulation reservation; actual wall apertures, support, swept clearance and sightlines require review. |
| route-14 | fore ↔ pump | 1.5 m | Authored circulation reservation; actual wall apertures, support, swept clearance and sightlines require review. |
| route-15 | pump ↔ hold | 1.5 m | Authored circulation reservation; actual wall apertures, support, swept clearance and sightlines require review. |
| route-16 | hold ↔ engine | 1.5 m | Authored circulation reservation; actual wall apertures, support, swept clearance and sightlines require review. |
| route-17 | aft ↔ crew | 1.5 m | Authored circulation reservation; actual wall apertures, support, swept clearance and sightlines require review. |
| route-18 | crew ↔ net | 1.5 m | Authored circulation reservation; actual wall apertures, support, swept clearance and sightlines require review. |
| route-19 | net ↔ bridge | 1.5 m | Authored circulation reservation; actual wall apertures, support, swept clearance and sightlines require review. |

## Arrival and extraction

Shared van placement: X=32, Z=86, yaw=0 radians. Ground Y=0. Cargo ramp approach: [[32.0, 0, 82.372], [32.0, 0, 78.0]]. Reuse the canonical van on the roadhead/quay; remote travel is an expedition transition. No new driving or sailing system.

## Rite reservations

| Rite | Zone | Centre X/Y/Z (m) | Installation |
|---|---|---|---|
| Banishment | processing | [32.0, 0, 19.0] | Removable modern investigation fitting on a clear 4.4 m floor square; reserve access around it. |
| Cleansing | mess | [36.0, 0, 35.0] | Removable modern investigation fitting on a clear 4.4 m floor square; reserve access around it. |
| Absolution | hold | [32.0, -3, 14.0] | Removable modern investigation fitting on a clear 4.4 m floor square; reserve access around it. |

Each reservation is a 4.4 m clear square. All three investigation outcomes remain independent of the narrative. HQ reservations are separated training bays, not live haunting anchors.

## Systems and environmental identity

Terrain and water boundaries first; then solid architecture with floor depth, fitted doors and circuit isolation. Separate exterior/interior acoustics, supported fixtures and surface-mounted UV evidence. All gameplay integration remains locked.

## Campaign use

Fictional location and records connect to the district investigation; narrative never predetermines the ghost or prevents an evidence-led outcome.

## Refinement risks / next pass

- Terrain-first massing is not terrain acceptance: resolve drainage, incised paths, retaining edges, foundation contacts and every grade before detailed structures.
- Check floor/wall/roof joins, upper-deck undersides and corners with adjacent lights toggled; log every discovered failure.
- Engineer stairs, landings, openings, guards, door swings and two exits before collision/navigation approval.
- Inspect each model purpose, scale, UVs and true support surface before placing furniture or clutter.
- Owner fleet reference informs processor function only; this is an original retired vessel, not a replica.
- Low-deck cutaway and both independent gangway clearances need review; sea must never fill the hold.

## Source basis

- https://www.oceanpeaceinc.com/fleet

## Native rough blockout

[Editable scene](../../blockouts/wld-15/rough-shell.blend) · [GLB export](../../blockouts/wld-15/rough-shell.glb) · [Native overview](../../blockouts/wld-15/preview.png)

The OBJ is a bounding-volume study. Native scenes retain curved tower/ring/hull shapes. Cyan/amber geometry is a route reservation, not finished construction.
