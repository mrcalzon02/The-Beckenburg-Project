package org.beckenburg;

import org.beckenburg.LevelDefinition.Stair;

/** Diagnostic for basement descent: a stair is not a flat ground-floor plane. */
public final class BasementApproachRegressionTest {
    public static void main(String[] args) {
        for (HouseMap.Site site : new HouseMap.Site[]{HouseMap.Site.HOUSE, HouseMap.Site.FARMHOUSE}) {
            HouseMap map = new HouseMap(site);
            boolean found = false;
            for (Stair stair : map.level().stairs()) {
                if (stair.y0() >= 0 || stair.y1() < -.01) continue;
                found = true;
                double t = .5;
                double x = stair.x0() + t * (stair.x1() - stair.x0());
                double z = stair.z0() + t * (stair.z1() - stair.z0());
                double expected = stair.yAt(x,z);
                double actual = map.groundHeight(x,z,stair.b());
                if (Math.abs(actual-expected) > .01)
                    throw new AssertionError(site+" basement height incorrectly follows ground floor: "+actual+" expected "+expected);
                if (!map.level().floorVoid(x,stair.y1(),z))
                    throw new AssertionError(site+" has solid floor across its basement stair aperture");
            }
            if (!found) throw new AssertionError(site+" has no authored basement descent");
        }
    }
}
