# WLD-12: terrain-first batch

Actual saved editor macroterrain and preserved furnished-scene composite: [review.json](review.json). 12695 native terrain triangles; exact render/native terrain parity and original collision bytes preserved. Native reimport and whole-site/terrain-only images reviewed. These are editable construction candidates, not runtime replacements.

Rock cutting crosses a ravine at the trestle; drainage follows that ravine rather than the track centreline.

Track bed requires explicit supported embankment and bridge. Tunnel needs a solid hillside roof; lower ravine route must meet the banks. Reposition trestle approaches if their current angles fight this section.

Current flat-route native comparison: 4173 samples, 4065 baseline-valid poses, 688 newly blocked poses, 670 newly failed qualified sweeps. Every failure stays open; zero new failures is only this comparison's scope. Full-body vertical chains and new terrain routes remain unaudited.

Next: resolve the actual terrain/blockout relationships, real foundations/retaining rims and supports before local route polish. Preserve existing models, room IDs and the40-space limit. Add the canonical van and actual supported arrival before any terrain acceptance. Piles, cave volumes, water, physical enclosure, services/story and runtime/network/hardware gates remain open. No source model or original map master was overwritten.
