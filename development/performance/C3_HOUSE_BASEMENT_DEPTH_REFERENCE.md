# REC-04 / C3 — House basement depth-owner reference

**Status:** source implementation and test registration on `main`; **exact-SHA CI and
real GPU acceptance pending** until the linked workflow run is observed.
**Scope:** authored House room 0 → room 4, basement rooms 10/11, ground/upper
views and three sampled points along the actual 4↔9 cellar stair.

## Why this exists

The C2 [frustum-only candidate audit](https://github.com/mrcalzon02/The-Beckenburg-Project/issues/20)
reported 425 noncandidate authored faces from the room-0→4 viewpoint.
That is **not** evidence of 425 missing visible surfaces. The C2 prototype
commit `1918b289` is on a **diverged candidate history**, not present in
the starting `main` tree. This C3 implementation uses current `main` House
geometry instead of silently assuming that C2's diagnostic was merged.

## Actual implementation

`SceneRenderer.auditHousePortalDepth` obtains the same authored House
architecture, floor/ceiling openings, stair surfaces, window rails and
open/closed door leaves used by the normal renderer. It temporarily disables
the existing **frustum-chunk** optimization only while collecting the reference,
then restores the setting. It does **not** alter live render submissions.

`HousePortalDepthReference` clips these triangles through the existing
near-plane clipper, then independently projects them into a 160×120 software
depth-owner buffer. Every winning opaque pixel is counted by authored room.
Perspective-correct UV sampling skips non-opaque imported-material pixels,
including glass; transparent pixels cannot hide the geometry behind them.
Unknown ownership remains explicit. A separate conservative room graph treats
windows/stairs as openings, closed doors as graph barriers, and unknown/outdoor
cameras as fail-open.

`HousePortalDepthReferenceTest` validates synthetic front-floor/hidden-cellar,
floor-aperture/exposed-cellar, transparent-glass and unowned-pixel cases,
then samples 16 actual House viewpoints (eight camera states × first-door
closed/open). The emitted CSV at
`build/e2e/viewport-profile/house-c3-depth-reference.csv` contains
`surfaced_pixels`, `missing_visible_pixels`, room-10/11 pixel counts,
unowned pixels, transparent samples and explicit excluded room IDs.
The existing Windows GPU workflow compiles/runs this Java 17 test after
asset packaging and retains the CSV in its viewport-profile artifact.

## Safety and acceptance boundary

A nonzero `missing_visible_pixels` is **direct evidence against** rejecting
those rooms at the sampled camera. Zero is only a **sampled CPU result**, not
proof that portal culling is safe: the reference omits gameplay sprites,
particle/evidence overlays, alpha-blended color accumulation and true GPU
driver rasterization. It does not replicate PSX pixel snap, multisampling,
runtime depth precision or every possible physical player viewpoint. All
non-opaque pixels are treated as non-occluding, which is deliberately
conservative for possible missed geometry but not an exact final image.

**Hard portal culling remains disabled.** No ghost/evidence/network authority,
gameplay input, release gate or installed hardware FPS target is changed.
The reference is a diagnostic, not a culling algorithm or a passing GPU capture.

**Next:** Inspect the exact commit's Windows CI CSV and failures, compare
pixel owners against the real GPU color/depth output for the same camera,
door/window/stair states, and collect collision-valid camera-path captures
before proposing any portal rejection. If a real visible basement pixel is
missing, repair the authored vertical-opening topology rather than add
number-specific room whitelists.
