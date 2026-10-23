# Flashlights and optical evidence

23 October 2026. Shared implementations and controlled runtime checks; native
hardware and full human/multiplayer mission acceptance remain pending.

[Shared gallery](https://mrcalzon02.github.io/The-Beckenburg-Project/beckenburg-development-gallery.html)
· [Verification](../development/levels/verification/optical-evidence.json)
· [Defect ledger](../development/quality/DEFECT_LEDGER.md)

## Paid flashlight advantage

The default flashlight remains 7.5m reach / 0.72 intensity. Tier I was incorrectly
weaker at 6m / 0.58. All paid tiers now beat the default and each preceding tier.

| Hardware | Reach | Intensity parameter | Cone parameter | Charged runtime |
| --- | --- | --- | --- | --- |
| Default | 7.5m | 0.72 | 0.44 | Existing default behavior |
| Tier I | 9.5m | 0.98 | 0.46 | 280s |
| Tier II | 12.83m | 1.18 | 0.53 | 420s |
| Tier III | 16.63m | 1.40 | 0.62 | 630s |

These are game optics parameters, not real-world lumen ratings. Actual beam
falloff changes in both CPU pixels and GPU-submitted vertices. Battery depletion,
automatic switch-off and van service retain host authority. Store readouts use
the same reach data as the renderer. Firelight keeps its separate short warm spill.

## UV light and surface cubes

UV now toggles a visible purple inspection cone. Genuine ghost passage leaves
bounded, fading residue at actual supported positions. A powered lamp must face
the trace, reach it and have an unobstructed line of sight. Scanning an empty
place creates no evidence. The host sends only nearby inspection-visible marks
to the inspecting hunter; the ghost receives no investigator trace packets.

Each residue is a shallow **12mm cube attached to its surface**, with the
64px irregular splat texture on its **outward face**. That face lies beyond the
surface instead of being coplanar or buried. The cube has backing/edge geometry,
depth testing and an alpha-masked outer texture. Normals for all six cardinal
surfaces survive the network frame and are checked independently. Current
passage production places floor residue; wall/ceiling contact producers and
teammate-source UV spill are further refinements, not silently completed work.

In LAN, use the held UV tool to toggle it. It consumes its real 200/300/450s
tiered battery and turns off when empty; van service restores charge. Solo's
UV action also toggles the lamp and drains charge; service it at the van rack.
Tier sensitivity retains the existing 90/125/160-tick genuine-trace lifetimes.
White light alone does not reveal the splat.

## D.O.T.S.

The projector emits a deterministic array of rays in its deployed heading. Tier I
is stationary; Tier II/III pan that actual array on the host tick. Each ray is
intersected against receiving scene triangles (including props and alpha masks).
Its single dot belongs to the nearest intersection. A host-authorized ghost
crossing that ray replaces the room hit with the nearer hit on its standing
collision capsule. Only the projector-facing hit is drawn: no whole-body dotted
skin, far-side dots, or duplicate dot on the wall behind the ghost.

Host compatibility, quota, battery, actual beam contact and architectural LOS
still gate passage events. Placement heading and ghost floor elevation replicate
to camera views; older placement/passage frames remain readable. The incorrect
capsule-sampling implementation has been removed. Deployment activates battery
drain, and pickup/depletion stops emission.

Photographs contain the actual rendered dots. A recent host-approved optical
exposure in the camera's direction can satisfy the photo-event task; merely
placing a projector does not. No ghost identity or automatic deduction is added.

## Laser grid

The laser grid is physically placed/retrieved as a replicated field station.
Tier I has a sparse three-ray plane; Tier II/III add denser multi-height fans.
Straight beams clip against existing architecture and solid furnishings.

Host-approved structured-light events must also **actually intersect** a beam
and the ghost's capsule. The intersection interval bounds the reaction: the
beam spirals within that interval and rejoins its original path outside it.
No intersection means no distortion. Quota, species, event and battery rules
remain in force. The van labels these stations L and reports actual grid contact,
rather than falling through to a thermometer reading. Power loss stops the grid.

Camera feeds and photographs use these rendered segments. Controlled capture
tests compare straight/distorted pixels and save the resulting photographs.

## Review and reproduce

`OpticalEvidenceCapture` saves controlled runtime fixtures under
`development/levels/previews/optical-evidence/`: UV off/on, default/paid torch,
projected D.O.T.S., straight/distorted lasers and actual photograph outputs.
These are labeled controlled views, not an observed full human mission.

```powershell
# Bootstrap actual packaged model resources if not already built:
python development/levels/verify_mill_lighting.py
python development/levels/verify_optical_evidence.py
python tools/build_defect_ledger.py
python tools/build_development_gallery.py
python tools/build_development_gallery.py --check
```

The focused suite has 21 checks, including real loopback TCP, UV exhaustion and
privacy, photographed pixels, emitted GPU RGB, device facts, physical stations,
map rendering and the corrected Blackpine slab. GPU bridge checks do not stand
in for an actual OpenGL context. Record newly found issues and older-map audits
before promoting any location or calling a release accepted.
