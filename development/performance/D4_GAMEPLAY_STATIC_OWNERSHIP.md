# D4 — Real gameplay shell ownership audit

This is source preparation for the eventual live GPU-resident House renderer. The authored House architecture and room-owned chunks (B2b/B2c) are already preserved by scene revision; but the normal gameplay scene also builds event props, doors, ghosts, equipment, van and animated objects. A GPU-resident batch must never misclassify one of those simply because it has identical material/room or triangle coordinates.

The D4 diagnostic uses **actual immutable Face object identity** (not structural equality) to tag only the prepared static shell in the normal gameplay scene assembly. It reports actual visible static and derived face counts and distinct static room ownership from the van, entrance, indoor and upstairs cameras, plus a freshly created second HouseMap. The tag set updates only when static source geometry revision/map changes and never changes face order, draw calls, geometry clipping, shadows, entities or network information.

The Windows functional gate verifies static + derived exactly equals the real scene total and that both categories are present, including after map transition. This is groundwork, not the production GPU VBO source split yet: D3 proved the versioned room/material VBO path on authored House samples, while D4 shows which live-world faces are safe to assign to it. Next D5 must prove per-chunk fixture normal parity, move the static source to separate cached GPU packets, preserve legacy depth and dynamic overlay behavior, and test full level/door/camera transitions before the default route switches.

Portable lesson: preserve **immutable asset ownership** independently of geometric/texture equality; dynamic objects must not acquire stale GPU resource identities.
