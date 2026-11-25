package org.beckenburg;

import dev.mrcalzon.water.WaterDocument;
import org.beckenburg.LevelDefinition.Vec;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Reviews the actual headpond, retaining structure and unchanged mission-facing geometry. */
public final class DamReservoirReview {
    private static final double SURFACE=-4.10;
    private static final double INVERT=-4.29;
    private static final double[] POSTURES={HunterPosture.STANDING_HEIGHT,HunterPosture.CROUCH_HEIGHT};

    private DamReservoirReview() {}

    public static void main(String[] args)throws Exception {
        var packet=Path.of(args[0]);
        var prior=Path.of(args[1]);
        var tailrace=Path.of(args[2]);
        if(!List.of(args).contains("--gpu")){
            review(packet,prior,tailrace,false);
            return;
        }
        var desktop=Class.forName("org.beckenburg.gpu.GpuDesktop");
        var start=desktop.getDeclaredMethod("startGpu");
        var stop=desktop.getDeclaredMethod("stopReviewGpu");
        start.setAccessible(true);
        stop.setAccessible(true);
        start.invoke(null);
        try{review(packet,prior,tailrace,true);}finally{stop.invoke(null);}
    }

    private static DamDoorReview.Scene withWater(DamDoorReview.Scene geometry,LevelDefinition base,WaterDocument water){
        var level=new LevelDefinition(base.id(),geometry.nativeScene().revision(),base.title(),base.rooms(),
            base.portals(),base.stairs(),base.voids(),base.props(),base.sockets(),base.colliders(),
            base.dock(),base.ambience(),water,base.sounds());
        return new DamDoorReview.Scene(level,geometry.nativeScene(),geometry.doors(),geometry.paths());
    }

    private static void review(Path packet,Path prior,Path tailrace,boolean gpu)throws Exception {
        for(var file:List.of("runtime-input.tsv","door-schedule.tsv","electrical-schedule.tsv")){
            require(Arrays.equals(Files.readAllBytes(packet.resolve(file)),Files.readAllBytes(prior.resolve(file))),
                "Headpond changed staged room, door or power authority "+file);
        }
        var previous=DamPowerReview.read(prior,tailrace);
        var base=previous.level();
        var geometry=DamDoorReview.read(packet,packet.resolve("blackwater.water"),"dam-with-reservoir");
        var wet=withWater(geometry,base,geometry.level().water());
        var dry=withWater(geometry,base,base.water());
        var wetMap=wet.map();
        var dryMap=dry.map();
        var physics=geometry.nativeScene().physics();
        require(wetMap.valid()&&RitualSites.valid(wetMap),"Headworks affected existing spawns or rites");
        require(!wet.level().revision().equals(dry.level().revision()),"Headpond is absent from handshake identity");
        var bodies=WorldWater.module(wet.level().water()).bodies();
        require(bodies.size()==2&&bodies.get(0).id().equals("dam-isolated-tailrace")&&bodies.get(1).id().equals("dam-north-headpond"),
            "Headpond and downstream reach did not remain separate water placements");
        require(wet.level().water().placements().get(0).equals(base.water().placements().get(0)),
            "Existing tailrace was edited by the upstream work");
        var lake=bodies.get(1);
        require(Math.abs(lake.y()-SURFACE)<1e-6,"Bad headpond datum");

        var report=new ArrayList<>(List.of("check\tresult",
            "native_scene_revision\t"+geometry.nativeScene().revision(),
            "collision_sha256\t"+physics.sha256(),"level_revision\t"+wet.level().revision(),
            "water_revision\t"+wet.level().water().revision(),
            "retained_tailrace_water_revision\t"+base.water().revision(),
            "renderer\t"+(gpu?GpuWorldRenderer.active().rendererDescription():"Software")));
        int basin=0;
        for(double x:new double[]{2,5,8,11,14,17,20,23}){
            for(double z:new double[]{-30,-29,-28,-27,-26,-25,-24}){
                var feet=ground(physics,x,z);
                require(feet.y()<SURFACE-.19&&lake.wet(x,z),"Dry hole in the expected interior "+feet);
                require(geometry.nativeScene().ownerAt(x,feet.y(),z)<0,"Water entered a playable owner "+feet);
                for(double height:POSTURES){
                    require(dryMap.traversable(x,feet.y(),z,height),"Dry bed control lacks native support "+feet);
                    require(!wetMap.traversable(x,feet.y(),z,height),"Submerged bed allowed walking "+feet);
                    basin++;
                }
            }
        }
        int bank=0;
        for(double x=-8;x<=35;x+=.5){
            for(double z:new double[]{-37,-13}){
                require(!physics.clearSight(new Vec(x,SURFACE+.135,z),new Vec(x,6,z)),"Authoring extent leaks along a cross-bank "+x+"/"+z);
                bank++;
            }
        }
        for(double z=-36.5;z<-13;z+=.5){
            for(double x:new double[]{-8,35}){
                require(!physics.clearSight(new Vec(x,SURFACE+.135,z),new Vec(x,6,z)),"Authoring extent leaks along a side bank "+x+"/"+z);
                bank++;
            }
        }
        report.add("supported_dry_bed_stances_blocked_when_wet\t"+basin);
        report.add("native_grade_samples_above_placement_border\t"+bank);
        report.add("reservoir_playable_room_owners\t0");

        var oldPhysics=previous.nativeScene().physics();
        int westGap=0,northGap=0;
        for(double z=22.25;z<48;z+=.25){
            for(double y:new double[]{-.22,-.65,-1.1,-1.55,-1.96}){
                var a=new Vec(7.25,y,z);
                var b=new Vec(8.75,y,z);
                if(oldPhysics.clearSight(a,b))westGap++;
                require(!physics.clearSight(a,b),"Gap remains through the west foundation "+a);

            }
        }
        for(double x=8.20;x<25.90;x+=.25){
            for(double y:new double[]{-.22,-.65,-1.1,-1.55,-2.0,-2.85}){
                var a=new Vec(x,y,21.56);
                var b=new Vec(x,y,22.66);
                if(oldPhysics.clearSight(a,b))northGap++;
                require(!physics.clearSight(a,b),"Gap remains at the north foundation return "+a);

            }
        }
        require(westGap>=350&&northGap>=350,"Previous unsupported faces were not reproduced "+westGap+"/"+northGap);
        report.add("west_open_underfloor_rays_closed_by_supported_stem\t"+westGap);
        report.add("north_open_underfloor_rays_closed_by_supported_return\t"+northGap);

        int rack=0;
        for(int i=0;i<14;i++){
            double z=-29.06+.075+i*.15;
            for(double y:new double[]{-4.19,-3.98,-3.82}){
                require(!physics.clearSight(new Vec(25.68,y,z),new Vec(26.15,y,z)),"Missing intake rack bar "+i);
                if(i<13)
                    require(physics.clearSight(new Vec(25.68,y,z+.075),new Vec(26.15,y,z+.075)),"Intake slot filled "+i);
                rack++;
            }
        }
        for(double z:new double[]{-28.35,-28,-27.65}){
            var floor=physics.support(27,z,INVERT,.045);
            require(floor!=null&&Math.abs(floor.y()-INVERT)<.002,"Intake floor lacks a real invert at "+z);
            require(!physics.clearSight(new Vec(28.45,-3.92,z),new Vec(30.48,-3.92,z)),"Headworks outlet escaped its blind end");
        }
        report.add("screen_steel_and_open_slot_elevations\t"+rack);
        report.add("supported_invert_and_capped_carrier\tPASS");
        for(var frame:wet.doors()){
            require(NativeSchoolInteriorReview.sweep(physics,frame),"Headworks blocked "+frame.frameId());
            NativeSchoolInteriorReview.audit(wetMap,frame,false);
            wetMap.toggleDoor(frame.portal().id(),System.nanoTime()-2_000_000_000L);
            NativeSchoolInteriorReview.audit(wetMap,frame,true);
        }
        require(wet.doors().size()==14,"Dam door count changed");
        for(int room:new int[]{1,4,10})require(wet.level().socket("hide",room)!=null,"Missing fitted refuge "+room);
        require(RoomLighting.breaker(wetMap)!=null&&wet.level().socket("generator",10)!=null,
            "Upstream work lost switchgear controls");
        report.add("doors_retained_and_tested\t14");
        report.add("retained_services_and_refuge_schedule\tPASS");

        var captures=packet.resolve(gpu?"captures/opengl":"captures/software");
        Files.createDirectories(captures);
        var south=ground(physics,15,-14.0);
        var east=ground(physics,32.8,-24.0);
        require(wetMap.traversable(south.x(),south.y(),south.z())&&wetMap.traversable(east.x(),east.y(),east.z()),
            "Outside-site standing review lacks actual bank support");
        var basinTarget=new Vec(14,SURFACE,-26.5);
        int wetPixels=capture(wetMap,south,basinTarget,captures.resolve("headpond-shoreline.png"));
        int dryPixels=capture(dryMap,south,basinTarget,captures.resolve("headpond-dry-control.png"));
        int intakePixels=capture(wetMap,east,new Vec(26.6,-3.98,-28.1),captures.resolve("headpond-intake.png"));
        require(wetPixels>12_000&&dryPixels==0&&intakePixels>3_000,
            "Native water response from the supported banks "+wetPixels+"/"+dryPixels+"/"+intakePixels);
        var catwalk=new Vec(72.45,0,34.2);
        require(wetMap.roomAt(catwalk.x(),catwalk.y(),catwalk.z())==11&&wetMap.traversable(catwalk.x(),catwalk.y(),catwalk.z()),
            "Original tailrace inspection deck lost its floor or room owner");
        int tailPixels=capture(wetMap,catwalk,new Vec(73.89,-6.82,24.5),captures.resolve("retained-tailrace.png"));
        require(tailPixels>400,"Downstream water is no longer visible from its own supported catwalk");
        report.add("upstream_wet_dry_and_intake_pixels\t"+wetPixels+"/"+dryPixels+"/"+intakePixels);
        report.add("retained_tailrace_catwalk_water_pixels\t"+tailPixels);
        Files.write(packet.resolve(gpu?"reservoir-opengl.tsv":"reservoir-software.tsv"),report);
        report.forEach(System.out::println);
    }

    private static Vec ground(AuthoredSitePhysics physics,double x,double z){
        var support=physics.support(x,z,-3.0,4.0);
        require(support!=null,"No terrain bearing at "+x+"/"+z);
        return new Vec(x,support.y(),z);
    }

    private static int capture(HouseMap map,Vec feet,Vec target,Path file)throws Exception {
        var renderer=new SceneRenderer();
        renderer.setRenderSize(960,640);
        renderer.setPsxPreset(PsxPipeline.Preset.OFF);
        renderer.x=feet.x();
        renderer.z=feet.z();
        renderer.eyeHeight=feet.y()+HunterPosture.STANDING_EYE;
        renderer.yaw=Math.atan2(target.x()-feet.x(),target.z()-feet.z());
        renderer.pitch=Math.atan2(target.y()-renderer.eyeHeight,Math.hypot(target.x()-feet.x(),target.z()-feet.z()));
        renderer.roomLights=new RoomLighting();
        renderer.weatherModule().setTimeOfDay(.50);
        renderer.setWeather(new MissionWeather.State(EvidenceSignals.Season.AUTUMN,EvidenceSignals.Weather.CLEAR,MissionWeather.Mode.FIXED,1,0));
        NativeSchoolInteriorReview.drawFrame(renderer,map,file);
        return renderer.waterStatistics==null?0:renderer.waterStatistics.pixels();
    }

    private static void require(boolean pass,String message){if(!pass)throw new AssertionError(message);}
}
