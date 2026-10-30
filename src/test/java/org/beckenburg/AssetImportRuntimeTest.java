package org.beckenburg;

import org.beckenburg.editor.EditorGlbWriter;
import org.beckenburg.editor.EditorMeshPipeline;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * End-to-end model import: portable OBJ -> optimized GLB -> real runtime decoder ->
 * editor registry -> persistent castle placement -> loader readback.
 */
public final class AssetImportRuntimeTest {
    private static void check(boolean value,String what){if(!value)throw new AssertionError(what);}
    public static void main(String[] args)throws Exception{
        String home=System.getProperty("user.home");
        Path root=Files.createTempDirectory("beckenburg-import-game-");
        try{
            System.setProperty("user.home",root.toString());
            Path source=root.resolve("test_head.obj");
            Files.writeString(source,"""
                o Head
                v 0 0 0
                v 0 0 1
                v 1 0 0
                v 0 1 0
                vt 0 0
                vt 1 0
                vt 1 1
                vt 0 1
                f 1/1 2/2 3/3
                f 1/1 4/4 2/2
                f 1/1 3/3 4/4
                f 2/2 4/4 3/3
                """);
            var imported=EditorMeshPipeline.read(source);
            var output=EditorGlbWriter.encode(EditorMeshPipeline.regress(imported,0).optimized());
            check(GlbProps.decode(output).triangles().size()==4,
                "Generated GLB must load with the real game rasterizer's decoder");
            String id=ImportedAssetLibrary.publish(source,output,0,4,4,"fallback atlas");
            check(ImportedAssetLibrary.approved(id),"Locally registered model");
            check(GlbProps.availableModels().contains(id),"Game available-model index recognizes import");
            check(GlbProps.model(id).triangles().size()==4,
                "Game renderer can resolve imported GLB from local library");
            check(Files.isRegularFile(ImportedAssetLibrary.root().resolve(id+".source.obj")),
                "Editable source is retained as a sibling");
            Path level=root.resolve("castle.properties");
            var castle=new CastleLevelEdits(level);
            castle.put(new CastleLevelEdits.Prop("import_test",id,21,0,20,1,0,.3,.3,false));
            castle.save();
            var loaded=new CastleLevelEdits(level);
            check(loaded.props().stream().anyMatch(p->p.id().equals("import_test")
                &&p.mesh().equals(id)),"Castle save/load references real imported model");
            System.out.println("PASS OBJ > generated GLB > game decode > registry > authored prop > reload");
        }finally{
            System.setProperty("user.home",home);
            try(var walk=Files.walk(root)){
                walk.sorted(java.util.Comparator.reverseOrder()).forEach(p->{
                    try{Files.deleteIfExists(p);}catch(Exception ignored){}
                });
            }
        }
    }
}
