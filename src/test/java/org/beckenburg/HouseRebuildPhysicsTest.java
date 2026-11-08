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
        // Later construction stages have their own manifest entries and process deadlines.
        // Keep every stage mandatory without accumulating all work into one 90-second process.
        System.out.println("PASS House construction pilot including editor-punched world terrain; remaining map gates stay open");
    }
}
