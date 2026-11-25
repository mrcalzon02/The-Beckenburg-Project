package org.beckenburg;

import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Narrow real-engine diagnostic for the Castle MCP editor lamp regression.
 * Uses one native CastleLevelEdits singleton, authoritative lighting and real
 * wall/slab occlusion. Prints source coordinates, unobstructed rays and
 * receiver values; never calls a mock material or simulated light implementation.
 *
 * Intentionally diagnostic-only until an actual render and end-to-end MCP run
 * prove the expected visual change.
 */
public final class CastleAgentLampProbe {
    private CastleAgentLampProbe() {}

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        var temp = Files.createTempDirectory("beckenburg-native-lamp-probe-");
        String oldHome = System.getProperty("user.home");
        System.setProperty("user.home", temp.toString());
        try {
            var scene = CastleLevelEdits.current();
            String id = "agent-lamp-probe";
            double lx = 6.4, lz = 13.9, floorY = 3.4;
            scene.placeCeilingLamp(id, lx, floorY, lz, 8.0, 0.0);
            var map = new CastleHubMap();

            double[][] receivers = new double[][] {
                {6.4, 5.1, 13.9}, {6.4, 4.35, 13.9}, {6.4, 3.54, 13.9},
                {6.0, 4.2, 13.3}, {5.75, 4.2, 13.5}, {4.7, 3.45, 14.0}
            };
            var off = new FixtureLighting(map);
            off.castleFrame(true, 0);
            var sourceOff = target(off, lx, lz);
            scene.editLamp(id, Map.of(CastleLevelEdits.Field.POWER, 8.0,
                                      CastleLevelEdits.Field.RADIUS, 9.0));
            var on = new FixtureLighting(map);
            on.castleFrame(true, 0);
            var sourceOn = target(on, lx, lz);

            System.out.printf(Locale.ROOT, "lamp native xyz=(%.4f,%.4f,%.4f); ceiling underside=%.4f%n",
                lx, scene.lampById(id).y(), lz, CastleRooms.mountHeight(lx, lz, true));
            System.out.printf(Locale.ROOT, "source off xyz=(%.4f,%.4f,%.4f) radius=%.4f power=%.4f%n",
                sourceOff.x(), sourceOff.y(), sourceOff.z(), sourceOff.radius(), sourceOff.power());
            System.out.printf(Locale.ROOT, "source on xyz=(%.4f,%.4f,%.4f) radius=%.4f power=%.4f%n",
                sourceOn.x(), sourceOn.y(), sourceOn.z(), sourceOn.radius(), sourceOn.power());
            double max = 0;
            for(double[] p : receivers) {
                var a = off.sample(0,p[0],p[1],p[2]);
                var b = on.sample(0,p[0],p[1],p[2]);
                boolean clear = FixtureLighting.castleRayClear(map,
                    sourceOn.x(),sourceOn.y(),sourceOn.z(),p[0],p[1],p[2]);
                double delta = Math.max(Math.abs(b.r()-a.r()),
                    Math.max(Math.abs(b.g()-a.g()),Math.abs(b.b()-a.b())));
                max = Math.max(max,delta);
                System.out.printf(Locale.ROOT,
                    "receiver=(%.2f,%.2f,%.2f) clear=%s before=(%.5f,%.5f,%.5f) after=(%.5f,%.5f,%.5f) delta=%.6f%n",
                    p[0],p[1],p[2],clear,a.r(),a.g(),a.b(),b.r(),b.g(),b.b(),delta);
            }
            System.out.printf(Locale.ROOT,"CASTLE_AGENT_LAMP_PROBE_MAX_DELTA=%.8f%n",max);
            if(sourceOn.y()>=CastleRooms.mountHeight(lx,lz,true)-0.02)
                throw new AssertionError("Native emitter buried in the upper ceiling slab");
            if(max<0.005)
                throw new AssertionError("Edited native ceiling lamp emits no verified lighting to room receivers");
            System.out.println("PASS authoritative Castle lamp emissive placement, LOS and power receiver regression");
        } finally {
            System.setProperty("user.home", oldHome);
            // Diagnostic temp data is isolated from the machine's real editor.
        }
    }
    private static FixtureLighting.Source target(FixtureLighting probe,double x,double z) {
        var s = probe.sources().stream().filter(c -> Math.hypot(c.x()-x,c.z()-z)<0.0001).toList();
        if(s.size()!=1)throw new IllegalStateException("Expected exactly one native lamp source, observed "+s.size());
        return s.get(0);
    }
}
