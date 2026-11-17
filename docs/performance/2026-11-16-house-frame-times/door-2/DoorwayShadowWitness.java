package org.beckenburg;

import java.util.List;

/** The recorded room-1/source-0 shadow boundary, with independent fresh fields
 * at each sample as the reference. The two points share the older rounded key. */
public final class DoorwayShadowWitness {
    public static void main(String[] args) {
        var map = new HouseMap(HouseMap.Site.HOUSE);
        var field = new FixtureLighting(map);
        var source = field.sources().get(0);
        var a = new double[]{16.125, 1.9375, 22.5625};
        var b = new double[]{16.11049899, 1.94557621, 22.54674685};
        System.out.println("state,point,visible,r,g,b");
        for (int step = 0; step < 4; step++) {
            if (step > 0) map.toggleDoor(2);
            for (var receiver : List.of(a,b)) {
                double x=receiver[0], y=receiver[1], z=receiver[2];
                var fresh = new FixtureLighting(map);
                fresh.frame(null,0);
                var light = fresh.sample(1,x,y,z);
                boolean visible = ArchitecturalLightOcclusion.visible(map,1,source.x(),source.y(),source.z(),x,y,z);
                if(visible != (map.doorOpen(2) && receiver==b))throw new AssertionError("Doorway receiver visibility");
                if(receiver==b&&map.doorOpen(2)&&Math.abs(light.r()-1.0767659844562887)>1e-7)
                    throw new AssertionError("Wrong light on reopened door");
                System.out.printf(java.util.Locale.ROOT,"%b,%s,%b,%.12f,%.12f,%.12f%n",map.doorOpen(2),receiver==a?"A":"B",visible,light.r(),light.g(),light.b());
            }
        }
    }
}
