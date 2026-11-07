package org.beckenburg;
/** Every preserved construction variant must match actual runtime texture sampling. */
public final class HouseRebuildTextureTest {
    public static void main(String[] args)throws Exception{
        LevelSceneTextureParityTest.main(new String[0]);
        for(String variant:new String[]{"HOUSE_REBUILD_FUNCTIONS","HOUSE_REBUILD_FITOUT","HOUSE_REBUILD_STORY","HOUSE_REBUILD_SERVICES","HOUSE_REBUILD_FURNISHINGS"})LevelSceneTextureParityTest.main(new String[]{variant});
    }
}
