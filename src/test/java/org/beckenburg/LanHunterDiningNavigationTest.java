package org.beckenburg;

/** Walk through the House and Farmhouse ground-floor entrances and return to the van relay. */
public final class LanHunterDiningNavigationTest {
    public static void main(String[] args)throws Exception{
        LanHunterStairNavigationTest.roundTrip(3,"Dining nook");
        LanHunterStairNavigationTest.roundTrip(2,"Family kitchen");
        LanHunterStairNavigationTest.roundTrip(HouseMap.Site.FARMHOUSE,0,"Mudroom",9);
        LanHunterStairNavigationTest.roundTrip(HouseMap.Site.FARMHOUSE,4,"Central stair passage",9,6,5,4,10);
    }
}
