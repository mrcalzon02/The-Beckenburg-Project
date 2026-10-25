package org.beckenburg;

/** Every packaged conversion, including roadside models, must load with its own textures. */
public final class PackagedSourceModelsTest {
    public static void main(String[] args)throws Exception {
        // Assets in the source archive are not all approved for the installed
        // game. Iterate the explicit game/editor registries; for every approved
        // GLB, loading must succeed from the actual packaged classpath.
        java.util.Set<String> names=new java.util.TreeSet<>(GlbProps.availableModels());
        names.addAll(LevelAssets.MODELS);
        names.addAll(LevelAssets.GREEN_GROUND);
        names.addAll(LevelAssets.NATIVE_SITE_TERRAIN);
        names.addAll(LevelAssets.GROUNDED_BERMS);
        names.addAll(LevelAssets.SITE_ART);
        names.addAll(LevelAssets.FITTED_TENTS);
        names.addAll(LevelAssets.TAILORED_FABRIC);
        names.addAll(LevelAssets.AUTHORED_SURFACES);
        names.addAll(TerrainFoundationAssets.MODELS);
        int count=0;
        for(String name:names){
            if(!LevelAssets.approved(name) && !GlbProps.availableModels().contains(name))
                throw new AssertionError("Unapproved asset in shipped registry "+name);
            var model=GlbProps.model(name);
            if(model.triangles().isEmpty())throw new AssertionError("Empty packaged mesh "+name);
            for(var tri:model.triangles())
                GlbProps.sample(tri.material(),tri.a().u(),tri.a().v());
            count++;
        }
        if(count<189)throw new AssertionError("missing registered and packaged models: "+count);
        // A source-only mesh cannot bypass the allowlist by existing in the
        // source directory. Other curated native meshes have their own tests.
        if(EquipmentModelCatalog.approved("blender_unreviewed_3"))
            throw new AssertionError("Unreviewed equipment source admitted");
        if(EquipmentModelCatalog.stageCount()!=129 ||
                EquipmentModelCatalog.variantCount()!=53)
            throw new AssertionError("incomplete physical equipment model map");
        for(EquipmentTiers.Plan plan:EquipmentTiers.all())
            for(EquipmentTiers.Tier tier:EquipmentTiers.Tier.values()){
                var prefab=EquipmentTierVisuals.prefab(plan.id(),tier);
                if(!EquipmentModelCatalog.approved(prefab.modelName()))
                    throw new AssertionError("missing approved default: "+prefab);
            }
        System.out.println("PASS: "+count+" explicitly approved packaged GLBs, real triangles and embedded textures");
    }
}
