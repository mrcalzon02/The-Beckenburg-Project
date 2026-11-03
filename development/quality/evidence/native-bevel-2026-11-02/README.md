# Native edge and corner bevel evidence

Actual Model Editor captures, native documents, original source GLB, published GLB and production software renderer capture. Infrastructure fixtures, not production map art.

The Bevel inspector uses actual native edge/vertex selection and one convex support-plane cut. Clipped original faces retain their IDs and uncut attributes; cut corners interpolate each original edge UV/normal independently. The cap inherits the active face material and uses planar UVs. Native frames, PNG data and original source GLB ownership remain intact. Preview/Cancel/Apply use existing native revision guards and shared history/save/reopen. No independent editable scene or agent endpoint is added.

Portable tests verify closed manifold/Euler geometry, outward caps, raw normals, multiple materials, seam interpolation, native v5 and OBJ bundle/GLB readback, source-backed edits, invalid/coplanar/open/oversized/stale/foreign refusals and 119988 input faces under256MiB. The actual GUI test exercises perspective picking, parameter invalidation, edge and corner cuts, one-step undo/redo, native fresh reopen, source archives and every real game corner/UV. Full existing native service/human/material/recovery/headless suite passes. The final source-GLB GUI case also passes with actual production rendering.

Prior72c99b7a standalone41562939268/native41562947315/GPU41562962371 succeeded. Full41562955851 is green advisory but raw308/310; DEV211 atlas and silhouette checks remain open. Exact excerpts retained. Current bevel CI will be pinned after publication.

Scope remains partial G2: one flat convex support-edge/corner cut. Concave/region/segmented bevels, loop/ring selection, sliding, incremental buffers, painting and remaining G0-G12 acceptance stay open. Main Level Editor and separate Material Editor layouts retain their existing ownership.
