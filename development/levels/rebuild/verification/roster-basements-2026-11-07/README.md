# Roster source surface/basement checkpoint — 7 November 2026

All25remaining native candidates rebuilt; four mature sources retained,29explicit packets. Existing124rough instances from60designs retain exact source IDs, pivots/UVs, poses and model budgets. No new prop master, runtime unlock, core movement tolerance, local editor UI or round-duration test.

Actual implementation:

-  Repair inward generated route slabs rather than changing physics flags by fiat. Independent prior66942b34HCS2 detects876bad tops among1011flat path solids; current1015flat solids all have outward walking tops. Independent reimport also checks1025outward closed route solids including10sloped ones. Physical thickness remains0.12m; real floors/walls/caps keep their depth/holes.
-  Cemetery and mortuary-annex stairs descend inside their vestibules before joining the continuous lower ossuary. Stable room IDs/counts remain. Approaches and explicit standing reservations move clear of the floor openings. Actual source dimensions/coords retained in layouts; known College/Mansion run/riser hints normalized to built geometry.
-  Base cemetery now uses the existing same-coordinate annex terrain source at immutablede4edf80. All13original zone coordinate definitions match that source. Actual editor topology masks persist/reopen, with1168/2303punched leaves,5491/9014terrain triangles and344/693clear punch probes. Unpunched optical/body negatives remain blocking. Both raw height PNGs remain byte-identical to the prior annex PNG.
-  Saved editable TerrainLayerStack grading establishes provisional connected path shoulders; no terrain holes are used to erase outdoor path contacts. Append-only grade definitions preserve earlier layers, and exact replay leaves24world files unchanged. Overall landform acceptance remains open.
-  Rejecting a terrain-fidelity failure now leaves prior HCS2/metadata untouched. The old exporter negative control reproduces clobbering; the corrected exporter preserves both previous hashes. File replacement follows prevalidation; no crash-atomic two-file transaction is claimed.

Measured geometry/route results:

- 152863structural triangles,7581checked closed solids,1498depth parts,515cap rays,1720treads,192fixed boundary rays. Furnished168983triangles includes16120existing prop triangles. Four saved-editor terrain sources match exact native float triangles/flags in source/furnished composites.
- 186/221vertical segments pass ascent/descent in both variants, up from182.35segments still fail.48/84complete stair links pass both directions;36links remain failing. All1720tread support/head-spines clear;1689tread poses traverse. Point probes do not accept full routes.
- Each cemetery's declared14-segment garden/vestibule/ossuary/east-entrance/surface-return loop passes continuous forward/reverse movement, structural and furnished, without resetting feet. Three earlier multi-link stair chains still pass; four other automatically matched chains remain failing.
- Against the previous furnished checkpoint,108885unchanged-link samples preserve72927valid poses and68127qualified sweeps with zero new blocks. Four deliberately revised cemetery links are excluded from that exact-coordinate comparison and covered by current physical loop/anchor checks.
- Current before/after prop comparison:110409samples,100898valid poses,95690qualified sweeps, zero new blocks.9511baseline-invalid samples remain real unresolved support/body conditions.
- Four current authored standing anchors pass; fourold room-centre reservations correctly fail. Source reservations are not claimed connected to runtime spawns/owners yet.

Comparison correction: each native scene uses its own measured support Y within the original0.02m budget; exact before/after heights are emitted. A separate1cm native-plane fixture reproduces the earlier false collision, while a low roof, absent support and excessive-height change remain rejected. No body/footprint/step query rule was weakened. Failed initial approaches/shoulders and all rejected controls are preserved separately; accepted-revision-routes/ and accepted-prop-routes/ are final results.

The reviewed14roster sheets cover100actual export views; sixcurrent first-person basement views use neutral150Wreview illumination and intact geometry. Construction3/5, finish1/5. Missing terrain/unsupported closure panels/coplanar route surfaces are still open defects, not visual acceptance. Actual PNGs and gallery7932entries are branch evidence; deployment awaits accepted integration.

Owner terrain-priority amendment: overall terrain layers and elevations now precede further isolated connector repair. The measured terrain-overall-pass/ register records18candidates with no continuous source terrain,3proposal landforms,4saved punched sources requiring macro review, and4retained mature-map individual audits. Set landform/strata, slopes, terraces, drainage, retaining edges and foundations, then reshuffle building groups/stair bays where appropriate. Current rough routes are review inputs, not fixed terrain constraints. Preserve40accessible spaces and shared geometry/navigation/owner/services/van coordinates.

Reproduce headlessly:

-  python tools/audit_roster_route_normals.py --require-outward --output <json>
-  python tools/audit_roster_stairs.py --require-authored-chains --output <directory>
-  python tools/audit_roster_routes.py --remaining --furnished --authored-stairs --require-clear-spines --output <directory>
-  python tools/audit_furnishing_routes.py --authored-stairs --revision-ref 66942b34 --output <directory>
-  python tools/audit_furnishing_routes.py --authored-stairs --output <directory>
-  python tools/check_furnishing_route_grounding.py
-  Blender background: tools/check_roster_export_failure.py (legacy control --legacy-ref66942b34 intentionally fails)
-  python tools/review_roster_structures.py --check; python tools/review_roster_furnishings.py --check
-  python tools/index_staged_map_models.py --check; python tools/build_development_gallery.py --check
-  python tools/audit_roster_terrain_envelopes.py --output <directory>

Main last fetchedcc65453c; full42431982917, renderer42431982951 and installer42431982920 atfad6c7a9 remain independently observed SUCCESS, with previous unchanged-source raw365/0/0/365. These are not exact candidate CI/hardware/gameplay/network results. Native room/owner/portal/acoustics/navigation/services, full capacities/mounts, terrain/guards/arrival, power/hiding/story and runtime acceptance remain open. The full29-map and follow-on rebuild goal remains active.
