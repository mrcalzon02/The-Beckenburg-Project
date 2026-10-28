# REC-04 / C3 — House basement depth-owner reference

**Status (2026-10-28):** original C3 CPU-reference Windows CI **passed** at
[7f1515d / run 40617556855](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/40617556855).
The retained CSV has **zero room-10 and zero room-11 surfaced pixels in all
16 viewpoints**, including all six sampled cellar-stair views; therefore
zero reported missing basement pixels is **not** meaningful proof of safe
portal rejection. Real same-pose GPU color/depth parity remains **unverified**.
The subsequent 20-view owner/depth diagnostic **passed its Windows C3
source-level step** at [exact test-source SHA f0ab5e8c / run 40726849050](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/40726849050);
the [retained artifact 12834271099](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/40726849050/artifacts/12834271099)
was independently downloaded/read back. This remains **CPU evidence, not
a passing GPU depth/color comparison**. The overall workflow status and
other jobs must be evaluated separately.
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
floor-aperture/exposed-cellar, transparent-glass and unowned-pixel cases.
The original 16-view run did not surface **any** basement-owned pixels.
The follow-up diagnostic adds two **actual basement interior positive controls**
(rooms 10 and 11, each aimed toward room 9) under both first-door states:
20 actual House viewpoints total, with assertions that each interior
control exposes pixels owned by its corresponding basement room.
The emitted CSV at
`build/e2e/viewport-profile/house-c3-depth-reference.csv` contains
`surfaced_pixels`, `missing_visible_pixels`, room-10/11 pixel counts,
unowned pixels, transparent samples, explicit excluded room IDs, camera
position/orientation, focal length and exact per-pose capture filename.
Each pose also emits a lossless `.owner-depth.bin` diagnostic (C3DP v1:
big-endian magic `0x43334450`, int32 width/height, then row-major
int32 owner and float32 camera-depth-metres per pixel; positive infinity
marks empty pixels, while finite depth with owner -1 means unowned).
A corresponding `.owners.png` visualizes ownership. The snapshots are
defensively copied from the actual CPU depth winner buffer; they provide
the **same-pose reference needed for future GPU comparison**, not GPU
depth samples themselves.
The existing Windows GPU workflow compiles/runs this Java 17 test after
asset packaging and retains the CSV in its viewport-profile artifact.

## Verified 20-view findings — first genuine basement pixels

The 20-view exact-source Windows CSV and its 20 C3DP binaries / 20 PNG
previews are real retained artifacts. Both door-state sweeps reproduce:

| Authored view | Owned room-10 pixels | Owned room-11 pixels | Missing-visible pixels | Excluded visible room |
|---|---:|---:|---:|---|
| Room 10 interior toward landing 9 | 10,887 | 0 | **3,993** | **9** |
| Room 11 interior toward landing 9 | 0 | 12,813 | **665** | **9** |
| Original ground/upper/stair samples | 0 | 0 | 0 | none |

The 3,993 and 665 are **actual depth-winner pixels attributed to room 9**
from within basement rooms 10 and 11, respectively. They are excluded by
the current **closed-door candidate graph**, even though the CPU authored
geometry raster sees them. Thus the strict candidate graph has **real
source-level false negatives** at these sampled views. This is not proof
of GPU driver parity, a particular door-mesh defect, or physical traversal.
The first door toggled by the test is not necessarily either basement door,
so equal open/closed rows do not establish those basement doors are equivalent.

**Do not turn on hard room/door portal rejection** based on this graph.
Investigate the general authored door aperture/leaf/frame geometry and
vertical openings, and verify matched real GPU pixels before deciding whether
a closed door can safely exclude its neighboring room. Do not suppress
the reported false negatives by special-casing room IDs or changing gameplay
visibility authority.

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

**Next:** Reconcile the **overall** exact-SHA workflow conclusion separately
from the passing Windows C3 step. Investigate why authored room-9 depth
pixels remain visible from basement rooms 10/11 when the closed-door
candidate graph excludes room 9. Then collect actual OpenGL color/depth
readbacks at the **identical** recorded camera, door/window/stair states,
compare depth-visible pixels with the CPU reference (accounting for PSX
snap and GPU precision), and collect collision-valid camera-path captures
before proposing any portal rejection. If a real visible basement pixel is
missing, repair the authored vertical-opening topology rather than add
number-specific room whitelists.
