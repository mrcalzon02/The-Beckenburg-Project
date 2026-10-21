# Reusable woodland and camp dressing

21 October 2026. Original CC0 geometry and packed procedural diffuse textures, authored in metres. The 20 native-pivot GLBs have individual editable Blender sources and mesh studio renders. [Machine-readable sizes, bounds, triangle counts and hashes](woodland-detail-assets.json).

The set provides a covered stone well with winch, rope, pail and sealed grille; flush flagstone patio; supported timber lean-to canopy; cold stone hearth with grate, logs and mantel mug; through-roof masonry chimney with open flue and flashing; barked timber stacks on skids; working sawhorse and hand saw; covered rainwater butt and downpipe; human-scale bench; grass, fern, small flowers and low scrub; stump, mossy boulder and deadfall; irregular soil/litter stamps, a forest-floor diffuse material and a low background bank.

Regenerate with Blender using `build_woodland_details.py`. The library is approved through `BlackpineDressing.ASSETS`; it does not require or imply worn copies of these already weathered standalone assets. Placements, solid colliders and landscape transforms are authored by `BlackpineDressing.java` and are exported in the actual level definition. Each patio has two matching post colliders and an overhead roof obstruction with standing head clearance. The well is sealed scenery, and hearths are cold scenery; water-drawing, fire lighting, damage and chimney smoke are not implemented by these assets.

Natural details are thin/low penetrable decoration. Only the three existing authored refuge shrubs conceal a crouched hunter. Ground stamps remain separate from authoritative floor support; flagstones finish flush at zero and do not introduce an unsupported raised platform. Trees outside the existing visible/collidable fence are backdrop. The road has an explicit exclusion corridor through that backdrop.

The first real deployment is [Blackpine](BLACKPINE_BUILD.md). Do not place another full site until this camp's remaining gameplay and presentation gates are met.
