# Shared scenery meshes and foliage variation

21 October 2026. Implemented in `SceneryInstances`, `FoliageVariation`, `GlbProps` and `SceneRenderer`. All investigation sites use the new scenery path. The existing Swing menus, account/character panels, HUD, settings, controls, depth-tested character sprites, physical world and network protocol remain in their existing components.

## What was adapted from Malefic Veil

Inspected the user's local `The Malefic Veil/src/main/java/veil/UrbanScene.java`, `PropRenderer.java` and `docs/PERFORMANCE_AND_ENVIRONMENT.md`. That implementation separates a shared mesh map from model placements, culls instances and then projects their triangles on the CPU. Its own performance document explicitly says GPU batching is not implemented. This change adapts that separation and whole-instance culling to Beckenburg's existing perspective textures, real depth buffer, fixture lighting and sprite rendering. It does not copy Malefic's painter-order polygons, substitute its UI or introduce random triangle-subset LOD holes.

## Actual implementation

- One indexed immutable source template per mesh used by the active scenery set; each object stores its placement, scale, seed, materials and conservative bounding sphere. Static prop/landscape copies are no longer retained as expanded world triangles. Scene switching and hiding-density changes replace the active set.
- Bounds include the actual scaled, twisted and bent mesh. Frustum rejection happens before transforming or lighting its triangles. Large and near-plane-crossing objects retain conservative visibility.
- Visible templates share one reusable vertex-position scratch array. Their triangles use the existing lighting, perspective-correct UV, near clipping and depth path. A fully expanded diagnostic/export path remains available for exact visual comparison and editable Blender export.
- Tree height, two width axes, crown asymmetry, orientation, crown twist and gradual lean derive from a stable mesh/position seed. The source model/UVs are never mutated. The lower two metres remain unbent and keep source dimensions; root rotation remains centred on the same base. Blackpine's solid interior tree collision conservatively encloses that rotated bark.
- Sixteen cached leaf texel palettes per original foliage material give subtle warm/cool, value and pixel variation. Bark keeps its source material. Plants also vary; refuge shrubs retain their exact authored shape and central pocket, with leaf texel variation only.
- General imported foliage, including existing Castle backdrop foliage, receives the same deterministic variation. Castle's existing static shell assembly remains its rendering path; its full hub has not been migrated to scenery instance records.

## Evidence and limits

See [measured renderer report](verification/scenery-instancing.json), [foliage report](verification/foliage-variation.json) and [site checks](verification/blackpine-dressing.json). The first six-view comparison covered 384,000 pixels with zero differing pixels between the instance and full expanded paths. Repeated scenery used approximately 106,000 expanded triangles versus 14,000 shared template triangles. Actual warm headless CPU frame times are reported, including p95; the first median remained roughly equal. This is a substantial geometry-storage and off-screen processing reduction, not a claim that repeated visible objects are free or that GPU instancing exists.

Hardware instanced draw calls, GPU buffers/shaders, target-device frame-rate acceptance and migration of the complete Castle hub remain later engineering work. No new external graphics dependency is required for this implementation.
