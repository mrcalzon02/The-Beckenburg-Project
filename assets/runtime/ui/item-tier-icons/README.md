# Individual equipment tier icons

129 independent 512 x 512 RGBA Blender Cycles renders: 42 equipment families plus health supplies, each at tiers I–III. Every file imports the exact runtime GLB selected by EquipmentModelCatalog.java; the manifest records that model. Static van fixtures correctly retain their shared source model.

The camera uses a close three-quarter view and re-renders after checking actual alpha bounds to fill roughly 84% of the long dimension. No three-tier review sheet is used as a store icon. Original materials, textures and source geometry are preserved.

Regenerate from the repository root:

    blender --factory-startup --background --python-exit-code 1 --python development/assets/render_equipment_tier_icons.py -- --force

Verify and generate review sheets with Python/Pillow:

    python development/assets/verify_equipment_tier_icons.py

UiAssetBake packages the full-resolution PNGs. EquipmentTierIconTest checks every packaged entry, framing, clipping and accidental image reuse across different runtime models. Review sheets are model inspection artifacts, not in-game item icons.
