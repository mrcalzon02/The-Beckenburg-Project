# Native stair and route checkpoint — 7 November 2026

Candidate map work, no local editor UI or round-duration tests. Exact-source identity is recorded in stair-movement.json, tread/route-probes.json and flat-routes/route-regressions.json. The JAR supplies dependencies; the current AuthoredSitePhysics source is compiled explicitly ahead of it.

The existing 124 rough instances from 60 staged designs remain allocated across 25 maps. Models remain useful now for space and budget; source/model refinement continues later. Original masters and the four mature maps are retained.

The three-argument reader preserves old behavior. The explicit four-argument reader accepts exact component identities from each hash-bound native contract. It clears only low authored tread lips up to 0.25m during movement; walls, roofs, props, containment and literal body/optical queries retain collision. Four foot supports are still mandatory. World-space float32 join tolerance is bounded to 10 micrometres on those stairs. An arbitrary name is not inferred to be a stair.

102 independent synthetic ascent/descent/edge/roof/wall/gap/high-step/parser/containment checks PASS. Legacy analytic/parser/source checks:34 PASS. Existing Hollow Creek native acceptance retains1041 support points,346 optical rays,14 bank-interior negatives,12 standing zone anchors,969 standing route points and939 sweeps with zero unresolved findings.

Actual before/after furnishing native geometry:170/221 authored vertical segments pass in both directions for each variant. 1686/1720 tread-centre poses traverse; all1720 retain support/clear head-height spines. 39/82 contiguous vertical chains pass each direction. Chains with intermediate flat landings are explicitly excluded and still need full-path auditing. Five complete lighthouse spiral links fail cyclic/top transitions even though individual tread fit improved. **51 failed vertical segments remain blockers.**

Flat-route before/after results: {"samples": 109506, "baseline_valid": 73065, "new_block": 0, "baseline_sweep": 68222, "new_sweep_block": 0}. The36441baseline-invalid samples remain open DEV-371; zero new furnishing regressions is not complete circulation acceptance.

Lighthouse actual source revision replaces140 rectangular tread meshes with mitered ribbon solids, preserving object pivots, elevations, clear widths and12triangles/tread. Independent GLB closed-solids/depth/cap/fixed-boundary checks pass. Saved punched editor terrain has exact float triangle/flag fidelity in both composites. Four before/after isolated export views and four updated lighthouse views were actually inspected. Construction fit3/5, finish1/5; guards, landings, native terrain fringe/foundations and finish remain open. Rejected early sweeps retain their own kernel/source hashes; changes combine movement handling, geometry and numerical-join repair, so their counts are not a geometry-only causal comparison.

Reproduce (headless only):

- python tools/audit_roster_stairs.py --output <directory>
- python tools/audit_roster_routes.py --remaining --furnished --authored-stairs --require-clear-spines --output <directory>
- python tools/audit_furnishing_routes.py --authored-stairs --output <directory>
- Java17 AuthoredStairPhysicsTest, AuthoredSitePhysicsTest, HollowCreekNativePhysicsTest with current compiled kernel before the dependency JAR.
- python tools/review_roster_structures.py --check
- python tools/review_roster_furnishings.py --check
- python tools/index_staged_map_models.py --check
- python tools/build_development_gallery.py --check

No runtime registry/ownership/navigation/AI/network service connection or playable acceptance is claimed. Source power/hiding/story, remaining capacity, mounted models, terrain/roof/boundaries and van arrivals remain separate work. Gallery captures are committed branch evidence; live deployment awaits accepted integration. Main remainscc65453c; sourcefad6c7a9 full42431982917, renderer42431982951 and installer42431982920 re-read SUCCESS separately; previous raw365/0/0/365 belongs to main, not this checkpoint.
