package org.beckenburg;

import dev.mrcalzon.water.WaterDocument;
import org.beckenburg.LevelDefinition.Dock;
import org.beckenburg.LevelDefinition.Room;
import org.beckenburg.LevelDefinition.Socket;
import org.beckenburg.LevelDefinition.Vec;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;

/** Reviews the actual downstream water in the shared scene and movement pipeline. */
public final class DamTailraceReview {
    private static final double BED=-7.18,WATER=-6.82;
    private static final double[] POSTURES={HunterPosture.STANDING_HEIGHT,HunterPosture.CROUCH_HEIGHT};
    private DamTailraceReview() {}

    public static void main(String[] args)throws Exception {
        boolean gpu=List.of(args).contains("--gpu");
        var packet=Path.of(args[0]);
        if(!gpu){review(packet,false);return;}
        var desktop=Class.forName("org.beckenburg.gpu.GpuDesktop");
        var start=desktop.getDeclaredMethod("startGpu");
        var stop=desktop.getDeclaredMethod("stopReviewGpu");
        start.setAccessible(true);
        stop.setAccessible(true);
        start.invoke(null);
        try{review(packet,true);}finally{stop.invoke(null);}
    }

    private static void review(Path packet,boolean gpu)throws Exception {
        var report=new ArrayList<String>();
        report.add("check\tresult");
        var spaces=new ArrayList<NativeInvestigationScene.Space>();
        var rooms=new Room[HouseMap.ROOM_COUNT];
        var sockets=new ArrayList<Socket>();
        var steps=new HashSet<String>();
        for(String row:Files.readAllLines(packet.resolve("runtime-zones.tsv"))){
            var p=row.split("\t");
            if(p[0].equals("STEP")){steps.add(p[1]);continue;}
            if(p[0].equals("RITUAL")){
                sockets.add(new Socket("dam-"+p[1],"ritual",Integer.parseInt(p[2]),d(p,3),d(p,4),d(p,5),0,p[1]));
                continue;
            }
            require(p[0].equals("ROOM")||p[0].equals("SPACE"),"Unknown Dam survey row "+row);
            var space=new NativeInvestigationScene.Space(p[1],Integer.parseInt(p[2]),d(p,3),d(p,4),
                d(p,5),d(p,6),d(p,7),d(p,8),Boolean.parseBoolean(p[9]),!p[1].equals("dam-closed-channel"));
            spaces.add(space);
            if(p[0].equals("ROOM")){
                require(rooms[space.owner()]==null,"Conflicting Dam survey owner "+space.owner());
                rooms[space.owner()]=new Room(space.owner(),p[10],space.x0(),space.z0(),space.x1(),space.z1(),
                    space.floor(),space.height(),space.outside()?"native-outdoor":"metal",
                    space.outside()?"outdoor":"concrete",new Vec(d(p,11),d(p,12),d(p,13)));
            }
        }
        var dock=new Dock(36,0,82,0);
        var van=new VanStaging(new VanStaging.Placement(dock.x(),dock.y(),dock.z(),dock.yaw()));
        rooms[14]=new Room(14,"Expedition van",34.74,79,37.26,84.4,van.floorY(),
            VanStaging.INTERIOR_HEIGHT,"van","metal",new Vec(36,van.floorY(),82));
        require(Arrays.stream(rooms).allMatch(java.util.Objects::nonNull),"Missing surveyed Dam room");

        var glb=packet.resolve("tailrace.glb");
        var collision=packet.resolve("tailrace.hcs2");
        var modelHash=sha(glb);
        var collisionHash=sha(collision);
        NativeInvestigationScene scene;
        try(var model=Files.newInputStream(glb);var physics=Files.newInputStream(collision)){
            scene=NativeInvestigationScene.read(model,modelHash,physics,collisionHash,steps,spaces);
        }
        var water=WaterDocument.read(Files.newInputStream(packet.resolve("dam-tailrace.water")));
        var dry=new WaterDocument(water.level(),List.of());
        var wetMap=map(scene,List.of(rooms),sockets,dock,water);
        var dryMap=map(scene,List.of(rooms),sockets,dock,dry);
        require(!wetMap.level().revision().equals(dryMap.level().revision()),"Water revision absent from level identity");
        var bodies=WorldWater.module(wetMap.level().water()).bodies();
        require(bodies.size()==1&&bodies.get(0).id().equals("dam-isolated-tailrace"),"Wrong staged tailrace");
        var waterBody=bodies.get(0);
        int supported=0;
        for(double z:new double[]{15.73,18,23,29.7,36,41,43.33}){
            for(double x:new double[]{73.31,73.89,74.46}){
                var bed=scene.physics().support(x,z,BED,.025);
                require(bed!=null&&Math.abs(bed.y()-BED)<.002
                    &&waterBody.wet(x,z)&&Math.abs(waterBody.depth(x,z)-(WATER-BED))<.002,
                    "No water on surveyed invert at "+x+"/"+z);
                require(scene.physics().clearSight(new Vec(x,WATER,z),new Vec(x,BED+.02,z)),
                    "Unexpected obstruction within the isolated reach");
                for(double height:POSTURES){
                    require(dryMap.traversable(x,BED,z,height),"Dry control stopped at the channel invert "+x+"/"+z+" height "+height);
                    require(!wetMap.traversable(x,BED,z,height),"Wet channel allowed submerged movement");
                    supported++;
                }
            }
        }
        require(scene.ownerAt(73.89,BED,30)==-1,"Closed reach marked as an explorable room");
        int blocked=0;
        for(double y:new double[]{BED+.04,WATER-.025,WATER+.18}){
            for(double z:new double[]{16.1,23,29.7,38,43.3}){
                for(double x:new double[]{72.59,75.21}){
                    require(!waterBody.wet(x,z)&&!wetMap.clearLineOfSight(73.89,y,z,x,y,z),
                        "Channel side extends beyond or lacks the retaining face");
                    blocked++;
                }
            }
            for(double z:new double[]{14.82,44.79}){
                require(!waterBody.wet(73.89,z)&&!wetMap.clearLineOfSight(73.89,y,29.7,73.89,y,z),
                    "Closed sluice does not bound the channel");
                blocked++;
            }
        }
        int guardChecks=0;
        for(double z:new double[]{35.5,39.2,42.3}){
            for(double height:POSTURES){
                var deck=new Vec(72.45,0,z);
                require(wetMap.roomAt(deck.x(),deck.y(),deck.z())==11
                    &&wetMap.traversable(deck.x(),deck.y(),deck.z(),height),"Lost dry catwalk contact");
                require(wetMap.resolveMove(deck,73.67,z,height)==null,"Catwalk rail permitted channel crossing");
                guardChecks++;
            }
        }
        var cargo=wetMap.van().world(0,0);
        require(wetMap.roomAt(cargo.x(),wetMap.van().floorY(),cargo.z())==14
            &&wetMap.traversable(cargo.x(),wetMap.van().floorY(),cargo.z()),"Canonical cargo origin drifted");

        var output=packet.resolve(gpu?"captures/opengl":"captures/software");
        Files.createDirectories(output);
        var catwalk=new Vec(72.45,0,34.2);
        var reach=new Vec(73.89,WATER,24.5);
        int wetPixels=capture(wetMap,catwalk,reach,output.resolve("dam-tailrace-downstream.png"));
        int dryPixels=capture(dryMap,catwalk,reach,output.resolve("dam-tailrace-dry-control.png"));
        require(wetPixels>400&&dryPixels==0,"Shared camera water coverage not established "+wetPixels+"/"+dryPixels);
        int deckPixels=capture(wetMap,new Vec(72.70,0,35.2),new Vec(73.89,WATER,20.5),
            output.resolve("dam-tailrace-from-guard.png"));
        require(deckPixels>100,"Water is not visible through the dry catwalk guard "+deckPixels);
        report.add("scene_revision\t"+scene.revision());
        report.add("level_revision\t"+wetMap.level().revision());
        report.add("water_revision\t"+water.revision());
        report.add("collision_sha256\t"+collisionHash);
        report.add("renderer\t"+(gpu?GpuWorldRenderer.active().rendererDescription():"Software"));
        report.add("dry_invert_stands_blocked_when_wet\t"+supported);
        report.add("retaining_faces_and_sluices_blocked_rays\t"+blocked);
        report.add("supported_catwalk_guard_crossings_rejected\t"+guardChecks);
        report.add("water_pixels_from_both_catwalk_views\t"+wetPixels+"/"+deckPixels);
        report.add("dry_control_water_pixels\t"+dryPixels);
        report.add("canonical_van_cargo\tPASS");
        Files.writeString(packet.resolve(gpu?"tailrace-opengl.tsv":"tailrace-software.tsv"),String.join("\n",report)+"\n");
        report.forEach(System.out::println);
    }

    private static HouseMap map(NativeInvestigationScene scene,List<Room> rooms,List<Socket> sockets,Dock dock,WaterDocument water){
        var level=new LevelDefinition("wld-09",scene.revision(),"Blackwater Hydroelectric Dam",
            rooms,List.of(),List.of(),List.of(),List.of(),sockets,List.of(),dock,"highland",water);
        return new HouseMap(HouseMap.Site.USER,level,null,scene);
    }

    private static int capture(HouseMap map,Vec feet,Vec target,Path file)throws Exception {
        var renderer=new SceneRenderer();
        renderer.setRenderSize(960,640);
        renderer.setPsxPreset(PsxPipeline.Preset.OFF);
        require(map.traversable(feet.x(),feet.y(),feet.z()),"Unsupported Dam camera "+feet);
        renderer.x=feet.x();
        renderer.z=feet.z();
        renderer.eyeHeight=feet.y()+HunterPosture.STANDING_EYE;
        renderer.yaw=Math.atan2(target.x()-feet.x(),target.z()-feet.z());
        renderer.pitch=Math.atan2(target.y()-renderer.eyeHeight,Math.hypot(target.x()-feet.x(),target.z()-feet.z()));
        renderer.lamp=true;
        renderer.setLampHardware(EquipmentTiers.Tier.III,false);
        renderer.roomLights=new RoomLighting();
        renderer.drawNetwork(map,List.of(),-1,0);
        var gpu=GpuWorldRenderer.active();
        if(gpu!=null){
            require(renderer.awaitSubmittedGpuPrewarm(30_000_000_000L,()->false),"Missing Dam OpenGL frame");
            renderer.pinCompletedGpuFrame(gpu.latest());
            renderer.drawNetwork(map,List.of(),-1,0);
            require(renderer.frameDiagnostics().gpuReadbackAvailable(),"Missing Dam OpenGL readback");
        }
        require(javax.imageio.ImageIO.write(renderer.image(),"png",file.toFile()),"PNG writer unavailable");
        return renderer.waterStatistics==null?0:renderer.waterStatistics.pixels();
    }

    private static String sha(Path path)throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
    }
    private static double d(String[] row,int field){return Double.parseDouble(row[field]);}
    private static void require(boolean value,String reason){if(!value)throw new AssertionError(reason);}
}
