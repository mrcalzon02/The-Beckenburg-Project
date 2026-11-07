package org.beckenburg;

/** Real construction-pilot checks, distinct from a complete mission acceptance. */
public final class HouseRebuildPhysicsTest {
    public static void main(String[] args)throws Exception{
        String[] pilot={"HOUSE_REBUILD"};
        AuthoredLevelPhysicsTest.main(pilot);
        AuthoredLevelReachabilityTest.main(pilot);
        LevelSurfaceIntegrityTest.main(pilot);
        AuthoredUtilitiesTest.main(pilot);
        HouseRebuildShellTest.main(new String[0]);
        HouseRebuildTerrainTest.main(new String[0]);
        HouseRebuildFittingsTest.main(new String[0]);
        HouseRebuildLandscapeTest.main(new String[0]);
        HouseRebuildEnvelopeTest.main(new String[0]);
        LevelSceneTextureParityTest.main(new String[0]);
        System.out.println("PASS House construction pilot including editor-punched world terrain; remaining map gates stay open");
    }
}
