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
        check(entries.get("level_site_academy_terrain").kind()
            ==DevAssetReviewCatalog.Kind.SOURCE_ONLY,
            "Unapproved external terrain must not be silently promoted");
        check(entries.get("level_camp_pavilion_services").kind()
            ==DevAssetReviewCatalog.Kind.SOURCE_ONLY,
            "Unapproved pavilion services must not be silently promoted");
        int sourceOnly=(int)entries.values().stream()
            .filter(e->e.kind()==DevAssetReviewCatalog.Kind.SOURCE_ONLY).count();
        check(sourceOnly==17,
            "Expected remaining 17 review-only terrain/pavilion meshes; got "+sourceOnly);
        System.out.println("PASS native source approval catalog: Matthis shipped, "
            +"17 unapproved meshes remain review-only");
    }
}
