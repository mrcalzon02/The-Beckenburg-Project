package org.beckenburg;

/** Hunters can reach both ground-floor rooms from the relay using reachable house entrances. */
public final class LanHunterDiningNavigationTest {
    public static void main(String[] args)throws Exception{
        LanHunterStairNavigationTest.roundTrip(3,"Dining nook");
        LanHunterStairNavigationTest.roundTrip(2,"Family kitchen");
    }
}
