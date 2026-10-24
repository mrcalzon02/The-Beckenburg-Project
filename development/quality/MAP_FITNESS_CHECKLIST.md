# Mandatory map/model fitness review

Use on every detail iteration, every reused asset, and retrospective map upgrades.
Record discoveries immediately in [defects.json](defects.json); regenerate the
[shared ledger](DEFECT_LEDGER.md). An unchecked item is unverified, not passed.

0. **Terrain first:** verify continuous ground, water/boundaries, drainage, slopes, foundation pads and supported van-to-site/loop routes before promoting structures or dressing. Preserve existing buildings while retrofitting a missing site foundation.
1. **Architectural joins:** inspect floor/wall, wall/wall corners, wall/roof,
   roof slopes, fascia, thresholds and stair openings at standing/crouched height.
   Floors above occupiable space need explicit depth, a downward-facing underside
   and closed exposed edges. Walking Y, underside Y and fixture mounting Y differ.
2. **Light separation:** matched captures with each adjoining circuit on/off;
   breaker off; portable light aimed at corners; closed/open doors. Test points
   just below/above decks and on both sides of walls and roof joins. Check actual
   rendered surfaces as well as ray tests. Do not treat one passing ray as proof
   that the whole map is leak-free.
3. **Major models:** measured bounds and native pivot; appropriate physical scale
   and purpose; no wall/roof penetration, furniture overlap, blocked windows,
   door swings, stairs, spawns, hiding pockets or ritual reservations.
4. **Supported small objects:** identify their real support mesh/part and its
   top plane. Verify every desk/shelf/counter item against that surface, including
   yaw, full footprint, bottom height, overhang and nearby objects. Reject floating,
   buried, interpenetrating or accidentally stacked objects. A model's overall
   bounding-box top is not necessarily its usable tabletop.
5. **Mounted facilities:** plates touch the actual finished wall/ceiling; brackets,
   suspensions, pipes and cables have continuous support. Refit fixtures when
   structural depth changes. Verify the physical switch and its intended circuit.
6. **Materials:** inspect in-room metal for repeated black/gray bands, timber for
   exaggerated stripes, UV stretching, conflicting coplanar finishes and incorrect
   normals. Keep source textures/UVs; audit each regenerated family independently.
7. **Physical routes:** swept ascent/descent, landings, body/head clearance, door
   approaches and van access. Re-run after any added structural depth or furniture.
   Preserve shared collision/render coordinates and floor openings.
8. **Reproduction and publication:** save failing evidence and the corrected view,
   native/runtime checks, affected-map status and next gate. Label studio/native
   overviews separately from runtime screenshots. Refresh and verify the shared
   GitHub gallery before moving to the next checkpoint.
9. **Optical evidence:** verify paid torches beat default in both renderer paths;
   UV casts purple light and reveals only real traces on outward textured cube
   faces; D.O.T.S. is confined to the body/field intersection; laser reactions are
   confined to real beam/body intersections. Test device power, architecture
   occlusion, camera photographs, networking privacy and empty-scene negatives.

10. **Edges and sky inclusion:** consume the map's edge-sky planning packet. Fit visible barriers to authoritative terrain with closed returns; retain the van/ramp, all route widths, rituals and sparse refuge pockets. Check under/over/around traversal in standing/crouched states and ghost containment. Water, a low log, fog or a painted horizon is not a physical stop. Inspect 360-degree standing/crouched/photo views and the highest windows/platforms: near/middle parallax, skyline sector continuity, open coast horizons, no sky through sealed joins, water horizon alignment, weather and CPU/GPU parity. Proposed drawings are not runtime acceptance.

For an older map, create a per-location audit record listing these categories,
evidence paths and defects found. Never mark other maps corrected because a shared
helper or one Blackpine fixture passed. New maps must consume these checks before
an iteration is described as accepted; rough native assembly alone is not runtime
or multiplayer acceptance.
