package org.beckenburg;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.beckenburg.LevelDefinition.Dock;
import org.beckenburg.LevelDefinition.Room;
import org.beckenburg.LevelDefinition.Socket;
import org.beckenburg.LevelDefinition.Vec;
import org.beckenburg.editor.DefaultPlayerMotion;

/** Independent physical and standing-view checks of the two native boundary exports. */
public final class CemeteryBoundaryReview {
    private static final double[] HEIGHTS = {HunterPosture.STANDING_HEIGHT,HunterPosture.CROUCH_HEIGHT};

    private CemeteryBoundaryReview() {}

    public static void main(String[] args) throws Exception {
        boolean gpu = List.of(args).contains("--gpu");
        Method stop = null;
        if (gpu) {
            var launcher = Class.forName("org.beckenburg.gpu.GpuDesktop");
            var start = launcher.getDeclaredMethod("startGpu");
            stop = launcher.getDeclaredMethod("stopReviewGpu");
            start.setAccessible(true);
            stop.setAccessible(true);
            start.invoke(null);
        }
        try { review(args, gpu); }
        finally { if (stop != null) stop.invoke(null); }
    }

    private static void review(String[] args, boolean gpu) throws Exception {
        var folder = Path.of(args[0]);
        boolean annex = folder.getFileName().toString().endsWith("mortuary");
        var sceneSpace = new NativeInvestigationScene.Space("boundary-review",12,
            1,-4,annex?91:59,90,0,3,true,true);
        var spaces=new ArrayList<>(List.of(sceneSpace));
        var sockets=new ArrayList<Socket>();
        for (String line : Files.readAllLines(Path.of(args[6]))) {
            var row=line.split("\t");
            int owner=Integer.parseInt(row[0]);
            spaces.add(new NativeInvestigationScene.Space(row[2],owner,d(row,3),d(row,4),d(row,5),d(row,6),
                       d(row,7),d(row,8),Boolean.parseBoolean(row[9]),true));
            sockets.add(new Socket("cemetery-"+row[1],"ritual",owner,d(row,10),d(row,11),d(row,12),0,row[1]));
        }
        NativeInvestigationScene scene;
        AuthoredSitePhysics prior;
        try (var glb = Files.newInputStream(folder.resolve("bounded-grounds.glb"));
             var nativeModel = Files.newInputStream(folder.resolve("bounded-grounds.hcs2"));
             var old = Files.newInputStream(Path.of(args[3]))) {
            scene = NativeInvestigationScene.read(glb,args[1],nativeModel,args[2],Set.of(),spaces);
            prior = AuthoredSitePhysics.read(old,args[4],List.of());
        }
        var physics = scene.physics();
        var rooms = new ArrayList<Room>();
        // Boundary-only review volumes supply the renderer and van with an
        // outdoor owner. The investigation room graph is a later fitting task.
        for (int i=0;i<14;i++) rooms.add(new Room(i,"Boundary review",1,-4,annex?91:59,90,0,3,
            "native-outdoor","outdoor",new Vec(28,0,66)));
        var dock = new Dock(28,0,78,0);
        var van = new VanStaging(new VanStaging.Placement(dock.x(),dock.y(),dock.z(),dock.yaw()));
        rooms.add(new Room(14,"Expedition van",26.74,75,29.26,80.4,van.floorY(),VanStaging.INTERIOR_HEIGHT,
                           "van","metal",new Vec(28,van.floorY(),78)));
        String id = annex?"wld-07-mortuary":"wld-07";
        var level = new LevelDefinition(id,scene.revision(),"Cemetery boundary review",rooms,List.of(),List.of(),
            List.of(),List.of(),sockets,List.of(),dock,"woodland");
        var map = new HouseMap(HouseMap.Site.USER,level,null,scene);

        require(RitualSites.valid(map),"The perimeter changed a supported ritual reservation");

        int body = 0, jump = 0, optics = 0, supOutside = 0, supportedCrossing = 0;
        for (String line : Files.readAllLines(Path.of(args[5]))) {
            var row = line.split("\t");
            double x = d(row,1), z = d(row,2), nx = d(row,3), nz = d(row,4);
            double y=d(row,5);
            require(!prior.clearSight(new Vec(x,y+.10,z),new Vec(x,y-.10,z)),
                    "No original soil at surveyed footing "+line);
            var at=new Vec(x,y,z);
            double apex=DefaultPlayerMotion.TAKEOFF_SPEED*DefaultPlayerMotion.TAKEOFF_SPEED
                        /(2*DefaultPlayerMotion.GRAVITY);
            for (double height : HEIGHTS) {
                require(physics.bodyBlocked(at,height,.22),"Open boundary body position "+line);
                body++;
                require(physics.bodyBlocked(new Vec(x,y+apex,z),height,.22),"Open jump-height position "+line);
                jump++;
            }
            var inside = prior.support(x-nx*.8,z-nz*.8,y,.8);
            var outside = physics.support(x+nx*.8,z+nz*.8,y,.8);
            if (outside != null) supOutside++;
            if (inside!=null) {
                var start = new Vec(x-nx*.8,inside.y(),z-nz*.8);
                for (double height : HEIGHTS) if (physics.traversable(start,height)) {
                    require(physics.resolveMove(start,x+nx*.8,z+nz*.8,height)==null,
                            "Walk through physically grounded boundary "+line);
                    supportedCrossing++;
                }
            }
            if (row[0].equals("WALL")) {
                require(!physics.clearSight(new Vec(x-nx*.7,y+1.40,z-nz*.7),
                                            new Vec(x+nx*.7,y+1.40,z+nz*.7)),"Open optical masonry seam "+line);
                optics++;
            }
        }
        require(supportedCrossing > 350 && supOutside > 280,"Insufficient physical crossing and exterior support checks");
        for (double x : annex?new double[]{28,66}:new double[]{28}) {
            require(prior.clearSight(new Vec(x,1.38,84.2),new Vec(x,1.38,86)),
                    "Baseline gate position already occluded");
            int opaque=0, openings=0;
            for (int i=-28;i<=28;i++) {
                double in=x+i*.055;
                if (physics.clearSight(new Vec(in,1.38,84.2),new Vec(in,1.38,86))) openings++;
                else opaque++;
            }
            require(opaque>6&&openings>12,"Gate lacks the real bar and gap optical pattern");
        }

        var lip = van.world(0,VanStaging.FRONT);
        var toCourt=List.of(new Vec(28,van.floorY(),78),new Vec(lip.x(),van.floorY(),lip.z()),
                           new Vec(28,0,72),new Vec(28,0,66));
        var back=new ArrayList<>(toCourt);
        java.util.Collections.reverse(back);
        for (double height : HEIGHTS) {
            NativeSchoolInteriorReview.walk(map,toCourt,height,"Cemetery canonical ramp and court");
            NativeSchoolInteriorReview.walk(map,back,height,"Cemetery court and canonical ramp");
        }
        require(map.roomAt(28,van.floorY(),78)==14,"Van staging has the wrong owner");

        var captures=folder.resolve(gpu?"captures/opengl":"captures");
        Files.createDirectories(captures);
        render(map, physics, 23.5, 76.5, 1.13, -.07, "boundary-van-road-gate", captures);
        render(map, physics, 24.7, 81.1, .70, -.08, "boundary-road-gate-close", captures);
        render(map, physics, 9.0, 60.0, -1.64, -.11, "boundary-chapel-shoulder", captures);
        if (annex) render(map, physics, 62.2, 80.8, .73, -.04, "boundary-receiving-gate", captures);
        var report=List.of("check\tresult","scene_revision\t"+scene.revision(),
            "renderer\t"+(gpu?GpuWorldRenderer.active().rendererDescription():"Software"),
            "boundary_capsule_contacts\t"+body,"boundary_jump_height_contacts\t"+jump,
            "masonry_optical_contacts\t"+optics,"sampled_exterior_support\t"+supOutside,
            "supported_crossings_blocked\t"+supportedCrossing,
            "gate_bar_and_gap_controls\t"+(annex?2:1),
            "canonical_ramp_and_court_both_directions_and_postures\tPASS");
        Files.writeString(folder.resolve(gpu?"boundary-opengl.tsv":"boundary-software.tsv"),
                           String.join("\n",report)+"\n");
        report.forEach(System.out::println);
    }

    private static void render(HouseMap map, AuthoredSitePhysics physics, double x, double z,
                               double yaw, double pitch, String name, Path folder) throws Exception {
        var floor = physics.support(x,z,0,.95);
        require(floor!=null&&map.traversable(x,floor.y(),z),"Unsupported boundary camera "+name);
        var renderer=new SceneRenderer();
        renderer.setRenderSize(960,640);
        renderer.setPsxPreset(PsxPipeline.Preset.OFF);
        renderer.x=x;
        renderer.z=z;
        renderer.eyeHeight=floor.y()+HunterPosture.STANDING_EYE;
        renderer.yaw=yaw;
        renderer.pitch=pitch;
        renderer.lamp=true;
        renderer.setLampHardware(EquipmentTiers.Tier.III,false);
        NativeSchoolInteriorReview.drawFrame(renderer,map,folder.resolve(name+".png"));
    }

    private static double d(String[] row,int i) { return Double.parseDouble(row[i]); }
    private static void require(boolean ok,String message) { if(!ok) throw new AssertionError(message); }
}
