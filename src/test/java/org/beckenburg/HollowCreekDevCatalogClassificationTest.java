package org.beckenburg;

import java.util.Map;
import java.util.stream.Collectors;

/** Advisory catalog classification check: read actual game approvals. */
public final class HollowCreekDevCatalogClassificationTest {
    private HollowCreekDevCatalogClassificationTest() {}
    private static void check(boolean condition,String text) {
        if(!condition)throw new AssertionError(text);
    }
    public static void main(String[] args) {
        Map<String,DevAssetReviewCatalog.Entry> entries=
            DevAssetReviewCatalog.entries().stream()
                .collect(Collectors.toMap(DevAssetReviewCatalog.Entry::id,e->e,
                    (oldVal,newVal)->oldVal));
        check(LevelAssets.approved("level_npc_matthis_rook_proxy"),
            "Original Matthis proxy is a production approved mesh");
        check(entries.get("level_npc_matthis_rook_proxy").kind()
            ==DevAssetReviewCatalog.Kind.WORLD_MODEL,
            "Approved Matthis is improperly classified SOURCE_ONLY");
        // These sixteen original terrain meshes are explicitly approved
        // by TerrainFoundationAssets. This does NOT authorize treating their
        // unfinished maps as playable, but the meshes are production assets.
        for(String terrain:TerrainFoundationAssets.MODELS){
            check(LevelAssets.approved(terrain),"Approved terrain unexpectedly denied: "+terrain);
            check(entries.get(terrain).kind()==DevAssetReviewCatalog.Kind.WORLD_MODEL,
                "Approved terrain incorrectly classified SOURCE_ONLY: "+terrain);
        }
        check(entries.get("level_camp_pavilion_services").kind()
            ==DevAssetReviewCatalog.Kind.SOURCE_ONLY,
            "Unapproved pavilion services must not be silently promoted");
        // The source checkout also contains non-shipping primitive studies.
        // Unlike a packaged game, the DEV room is supposed to list them.
        java.nio.file.Path dir=java.nio.file.Path.of("development","levels","primitive-props-v1","models");
        int expectedBlockouts=0;
        if(java.nio.file.Files.isDirectory(dir))try(var files=java.nio.file.Files.list(dir)){
            for(var file:files.toList()){
                String name=file.getFileName().toString();
                if(!name.startsWith("level_blockout_")||!name.endsWith(".glb"))continue;
                expectedBlockouts++;
                String id=name.substring(0,name.length()-4);
                check(entries.containsKey(id),"Missing individual DEV blockout: "+id);
                check(entries.get(id).kind()==DevAssetReviewCatalog.Kind.SOURCE_ONLY,
                    "An unfinished graybox became runtime-approved: "+id);
                check(!GlbProps.availableModels().contains(id)&&!LevelAssets.approved(id),
                    "A graybox leaked into the production model allowlist: "+id);
            }
        }catch(java.io.IOException bad){
            throw new IllegalStateException("Cannot enumerate native graybox source",bad);
        }
        java.nio.file.Path previews=java.nio.file.Path.of("development","levels",
            "primitive-props-v1","previews");
        int expectedLayoutReviews=0;
        if(java.nio.file.Files.isDirectory(previews))try(var files=java.nio.file.Files.list(previews)){
            for(var file:files.toList()){
                String name=file.getFileName().toString();
                if(!name.matches("[a-z0-9-]{1,40}\\.glb"))continue;
                expectedLayoutReviews++;
                String id="level_blockout_layout_"+name.substring(0,name.length()-4);
                check(entries.containsKey(id),"Missing room-layout DEV review: "+id);
                check(entries.get(id).kind()==DevAssetReviewCatalog.Kind.SOURCE_ONLY,
                    "A layout preview became production-approved: "+id);
            }
        }catch(java.io.IOException bad){
            throw new IllegalStateException("Cannot enumerate layout preview source",bad);
        }
        int sourceOnly=(int)entries.values().stream()
            .filter(e->e.kind()==DevAssetReviewCatalog.Kind.SOURCE_ONLY).count();
        check(sourceOnly==1+expectedBlockouts+expectedLayoutReviews,
            "Expected only pavilion and nonshipping DEV studies; got "+sourceOnly+
            " vs "+(1+expectedBlockouts+expectedLayoutReviews));
        System.out.println("PASS native source approval catalog: Matthis and 16 terrain "
            +"meshes approved; one pavilion, "+expectedBlockouts+" primitive studies and "+
            expectedLayoutReviews+" planning-layout previews remain SOURCE_ONLY");
    }
}
