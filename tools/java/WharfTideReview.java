package org.beckenburg;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dev.mrcalzon.water.WaterDocument;
import org.beckenburg.LevelDefinition.Dock;
import org.beckenburg.LevelDefinition.Room;
import org.beckenburg.LevelDefinition.Socket;
import org.beckenburg.LevelDefinition.Vec;

/** Shared game renderer, tide, arrival and body checks on the inspected Wharf export. */
public final class WharfTideReview {
    private WharfTideReview() {}

    public static void main(String[] args) throws Exception {
        if (!List.of(args).contains("--gpu")) {
            review(args);
            return;
        }
        var launcher = Class.forName("org.beckenburg.gpu.GpuDesktop");
        var start = launcher.getDeclaredMethod("startGpu");
        var stop = launcher.getDeclaredMethod("stopReviewGpu");
        start.setAccessible(true);
        stop.setAccessible(true);
        start.invoke(null);
        try {
            review(args);
        } finally {
            stop.invoke(null);
        }
    }

    private static void review(String[] args) throws Exception {
        var packet = Path.of(args[0]);
        var output = Path.of(args[1]);
        var dock = new Dock(32, 0, 74, 0);
        var routes = new LinkedHashMap<String, List<Vec>>();
        var spaces = new ArrayList<NativeInvestigationScene.Space>();
        var rooms = new ArrayList<Room>();
        var sockets = new ArrayList<Socket>();
        var steps = new java.util.HashSet<String>();
        var rays = new ArrayList<double[]>();
        for (String row : Files.readAllLines(Path.of(args[2]))) {
            var tab = row.split("\t");
            switch (tab[0]) {
                case "ROOM" -> {
                    var space = new NativeInvestigationScene.Space(tab[1], Integer.parseInt(tab[2]),
                        d(tab, 3), d(tab, 4), d(tab, 5), d(tab, 6), d(tab, 7), d(tab, 8),
                        Boolean.parseBoolean(tab[9]), Boolean.parseBoolean(tab[10]));
                    spaces.add(space);
                    var origin = new Vec((space.x0()+space.x1())*.5, space.floor(),
                                         (space.z0()+space.z1())*.5);
                    rooms.add(new Room(space.owner(), tab[11], space.x0(), space.z0(), space.x1(),
                        space.z1(), space.floor(), space.height(),
                        space.outside()?"native-outdoor":"wood", space.outside()?"outdoor":"timber", origin));
                }
                case "POINT" -> routes.computeIfAbsent(tab[1], ignored -> new ArrayList<>())
                        .add(new Vec(d(tab, 2), d(tab, 3), d(tab, 4)));
                case "STEP" -> steps.add(tab[1]);
                case "RITUAL" -> sockets.add(new Socket("wharf-"+tab[1],"ritual",Integer.parseInt(tab[2]),
                        d(tab,3),d(tab,4),d(tab,5),0,tab[1]));
                case "WALL" -> rays.add(new double[] {d(tab, 1),d(tab, 2),d(tab, 3),d(tab, 4),d(tab, 5)});
                default -> throw new IllegalArgumentException("Unknown review row");
            }
        }
        rooms.sort(java.util.Comparator.comparingInt(Room::id));
        NativeInvestigationScene scene;
        WaterDocument water;
        try (var input = Files.newInputStream(packet.resolve("wharf-tide.water"))) {
            water = WaterDocument.read(input);
        }
        try (var model = Files.newInputStream(packet.resolve("tidal-wharf.glb"));
             var physics = Files.newInputStream(packet.resolve("tidal-wharf.hcs2"))) {
            scene = NativeInvestigationScene.read(model, args[3], physics, args[4], steps, spaces);
        }
        var level = new LevelDefinition("wld-06", scene.revision(), "Drowned Fisherman's Wharf",
                rooms, List.of(), List.of(), List.of(), List.of(), sockets, List.of(), dock, "coast", water);
        var map = new HouseMap(HouseMap.Site.USER, level, null, scene);
        require(RitualSites.valid(map), "Ritual reservations do not belong to dry accessible rooms");
        var tide = WorldWater.module(map.level().water());
        require(tide.bodies().size() == 8, "Expected a continuous channel around three dry bands");

        var evidence = new ArrayList<>(List.of("check\tresult",
                "scene_revision\t" + scene.revision(), "water_revision\t" + water.revision()));
        int dry = 0;
        for (double z = 24.37; z < 35.68; z += .19) {
            for (double x = 16.35; x < 47.68; x += .23) {
                boolean central = x >= 24.31 && x <= 39.69;
                boolean stairWing = z >= 31.78 && z <= 34.21;
                if (!central && !stairWing) continue;
                require(!wet(tide, x, z), "Tide leaked into the cellar or stair well at " + x + ", " + z);
                dry++;
            }
        }
        for (double[] wall : rays) {
            double x = wall[0], y = wall[1], z = wall[2], nx = wall[3], nz = wall[4];
            require(!map.clearLineOfSight(x-nx*.39,y,z-nz*.39,x+nx*.39,y,z+nz*.39),
                    "Possible opening in the immersed wall near " + x + ", " + y + ", " + z);
        }

        int wet = 0;
        int physicallyStandableWithoutWater = 0;
        for (double[] p : new double[][] {{32,2},{30,20},{6,16},{51,16},{22,27},{45,27},
                                          {12,33},{53,33},{22,44},{42,44},{-4,12},{68,22}}) {
            require(wet(tide, p[0], p[1]), "Dry hole in the tide at " + p[0] + ", " + p[1]);
            var floor = scene.physics().support(p[0], p[1], -4, 3.2);
            require(!scene.physics().clearSight(new Vec(p[0],-1.47,p[1]), new Vec(p[0],-8,p[1])),
                    "Missing opaque submerged bed " + p[0] + ", " + p[1]);
            if (floor != null && floor.y() < -1.47
                    && scene.physics().traversable(new Vec(p[0],floor.y(),p[1]), HunterPosture.STANDING_HEIGHT)) {
                physicallyStandableWithoutWater++;
                require(!map.traversable(p[0], floor.y(), p[1], HunterPosture.STANDING_HEIGHT)
                        && !map.traversable(p[0], floor.y(), p[1], HunterPosture.CROUCH_HEIGHT),
                        "Player can stand on submerged ground");
            }
            wet++;
        }
        evidence.add("dry_samples\t"+dry);
        evidence.add("immersed_wall_rays_blocked\t"+rays.size());
        require(physicallyStandableWithoutWater >= 4,"No submerged movement negative controls");
        evidence.add("submerged_bed_samples\t"+wet);
        evidence.add("water_blocked_otherwise_supported_bed_samples\t"+physicallyStandableWithoutWater);

        var fromVan = new ArrayList<Vec>();
        var cargo = map.van().world(0,0);
        var front = map.van().world(0,VanStaging.FRONT);
        fromVan.add(new Vec(cargo.x(),map.van().floorY(),cargo.z()));
        fromVan.add(new Vec(front.x(),map.van().floorY(),front.z()));
        fromVan.addAll(routes.get("L01"));
        require(map.roomAt(cargo.x(),map.van().floorY(),cargo.z()) == 14, "Van extraction owner changed");
        routes.put("L01",fromVan);
        for (Map.Entry<String,List<Vec>> route : routes.entrySet()) {
            walk(map, route.getKey(), route.getValue());
            var reverse = new ArrayList<>(route.getValue());
            Collections.reverse(reverse);
            walk(map, route.getKey()+" reverse", reverse);
        }
        evidence.add("routes_replayed_both_directions_and_postures\t"+routes.size());
        evidence.add("canonical_cargo_and_ramp\tpassed");

        for (double h : new double[] {HunterPosture.STANDING_HEIGHT, HunterPosture.CROUCH_HEIGHT}) {
            for (double x : new double[] {20.8,22.2,26.0,32.0,37.9,41.9,43.2}) {
                require(map.traversable(x,0,6.72,h), "Foredeck contact lost");
                require(map.resolveMove(new Vec(x,0,6.72),x,5.77,h) == null,
                        "Player crossed crossdock outer rail "+x);
            }
        }
        evidence.add("crossdock_rail_blocked_crossings\t14");

        Files.createDirectories(output.resolve("captures"));
        boolean gpu = GpuWorldRenderer.active()!=null;
        var captures = gpu ? output.resolve("captures/opengl") : output.resolve("captures");
        Files.createDirectories(captures);
        int visibleWater = capture(map,new Vec(32,0,7.2),Math.PI,-.36,
                captures.resolve("wharf-crossdock-tide.png"),false);
        int pierWater = capture(map,new Vec(14,0,37.65),-1.88,-.32,
                captures.resolve("wharf-shore-boardwalk.png"),true);
        int undercroft = capture(map,new Vec(32,-2.5,30),0,-.12,
                captures.resolve("wharf-dry-undercroft.png"),true);
        capture(map,new Vec(27,0,67.7),.67,-.12,captures.resolve("wharf-canonical-arrival.png"),true);
        require(visibleWater>2500 && pierWater>1000 && undercroft==0,"Water draw or dry-cell occlusion mismatch");
        evidence.add("foreground_tide_pixels\t"+visibleWater);
        evidence.add("shore_boardwalk_tide_pixels\t"+pierWater);
        evidence.add("dry_undercroft_tide_pixels\t"+undercroft);
        evidence.add("renderer\t"+(gpu ? GpuWorldRenderer.active().rendererDescription() : "Software"));
        Files.createDirectories(output.resolve("verification"));
        Files.writeString(output.resolve(gpu?"verification/opengl.tsv":"verification/runtime.tsv"),
                          String.join("\n",evidence)+"\n");
        evidence.forEach(System.out::println);
    }

    private static boolean wet(dev.mrcalzon.water.WaterModule tide, double x, double z) {
        return tide.bodies().stream().anyMatch(body -> body.wet(x,z));
    }

    private static void walk(HouseMap map,String name,List<Vec> points) {
        for (double h : new double[] {HunterPosture.STANDING_HEIGHT,HunterPosture.CROUCH_HEIGHT}) {
            Vec at = points.get(0);
            require(map.traversable(at.x(),at.y(),at.z(),h),"Blocked start "+name+" "+at);
            for (int i=1;i<points.size();i++) {
                Vec from=points.get(i-1),to=points.get(i);
                int count=Math.max(1,(int)Math.ceil(Math.hypot(to.x()-from.x(),to.z()-from.z())/.08));
                for (int step=1;step<=count;step++) {
                    double t=(double)step/count;
                    var next=map.resolveMove(at,from.x()+(to.x()-from.x())*t,from.z()+(to.z()-from.z())*t,h);
                    require(next!=null,"Blocked "+name+" segment "+i+" near "+at+" posture "+h);
                    at=next;
                }
            }
            require(Math.abs(at.y()-points.get(points.size()-1).y())<.25,"Wrong end datum "+name);
        }
    }

    private static int capture(HouseMap map,Vec feet,double yaw,double pitch,Path target,boolean lamp)throws Exception {
        require(map.traversable(feet.x(),feet.y(),feet.z()),"Unsupported camera "+feet);
        var renderer=new SceneRenderer();
        renderer.setRenderSize(880,580);
        renderer.setPsxPreset(PsxPipeline.Preset.OFF);
        renderer.x=feet.x();
        renderer.z=feet.z();
        renderer.eyeHeight=feet.y()+HunterPosture.STANDING_EYE;
        renderer.yaw=yaw;
        renderer.pitch=pitch;
        renderer.lamp=lamp;
        renderer.setLampHardware(EquipmentTiers.Tier.III,false);
        NativeSchoolInteriorReview.drawFrame(renderer,map,target);
        require(renderer.waterStatistics!=null,"Shared water pass did not run");
        return renderer.waterStatistics.pixels();
    }

    private static double d(String[] row,int i) {return Double.parseDouble(row[i]);}
    private static void require(boolean valid,String reason) {if(!valid)throw new AssertionError(reason);}
}
