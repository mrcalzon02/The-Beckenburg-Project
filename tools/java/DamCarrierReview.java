package org.beckenburg;

import org.beckenburg.LevelDefinition.Vec;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Checks the assembled, unobstructed carrier without promoting it to player circulation. */
public final class DamCarrierReview {
    private DamCarrierReview() {}

    public static void main(String[] args)throws Exception {
        var packet=Path.of(args[0]);
        var prior=Path.of(args[1]);
        var power=Path.of(args[2]);
        if(!List.of(args).contains("--gpu")){
            review(packet,prior,power,false);
            return;
        }
        var desktop=Class.forName("org.beckenburg.gpu.GpuDesktop");
        var start=desktop.getDeclaredMethod("startGpu");
        var stop=desktop.getDeclaredMethod("stopReviewGpu");
        start.setAccessible(true);
        stop.setAccessible(true);
        start.invoke(null);
        try{review(packet,prior,power,true);}finally{stop.invoke(null);}
    }

    private static void review(Path packet,Path prior,Path power,boolean gpu)throws Exception {
        for(var schedule:List.of("runtime-input.tsv","door-schedule.tsv","electrical-schedule.tsv","blackwater.water")){
            require(Arrays.equals(Files.readAllBytes(packet.resolve(schedule)),Files.readAllBytes(prior.resolve(schedule))),
                "Carrier changed existing diegetic authority "+schedule);
        }
        var water=packet.resolve("blackwater.water");
        var original=DamDoorReview.read(prior,water,"dam-with-reservoir").nativeScene().physics();
        var data=DamDoorReview.read(packet,water,"dam-with-carrier");
        var base=DamPowerReview.read(power,water).level();
        var level=new LevelDefinition(base.id(),data.nativeScene().revision(),base.title(),base.rooms(),
            base.portals(),base.stairs(),base.voids(),base.props(),base.sockets(),base.colliders(),
            base.dock(),base.ambience(),base.water(),base.sounds());
        var map=new HouseMap(HouseMap.Site.USER,level,null,data.nativeScene());
        var physics=data.nativeScene().physics();
        require(map.valid()&&RitualSites.valid(map),"Carrier blocked a fitted spawn or ritual area");
        var route=readProfile(packet.resolve("carrier-profile.tsv"));
        int bore=0,wall=0,body=0;
        for(int i=1;i<route.size();i++){
            Vec a=route.get(i-1),b=route.get(i);
            Vec dir=unit(new Vec(b.x()-a.x(),b.y()-a.y(),b.z()-a.z()));
            Vec sideways=unit(new Vec(-dir.z(),0,dir.x()));
            Vec up=unit(new Vec(-dir.x()*dir.y(),dir.x()*dir.x()+dir.z()*dir.z(),-dir.y()*dir.z()));
            require(data.nativeScene().ownerAt(a.x(),a.y(),a.z())<0,"Carrier passed through a personnel room "+a);
            require(physics.clearSight(a,b),"Carrier centre blocked at station "+i);
            bore++;
            for(int j=0;j<8;j++){
                double angle=Math.PI*j/4;
                Vec radial=new Vec(Math.cos(angle)*sideways.x()+Math.sin(angle)*up.x(),
                                   Math.cos(angle)*sideways.y()+Math.sin(angle)*up.y(),
                                   Math.cos(angle)*sideways.z()+Math.sin(angle)*up.z());
                require(physics.clearSight(offset(a,radial,.174),offset(b,radial,.174)),
                    "Assembled carrier blocks its clear bore at station "+i+" angle "+j);
                bore++;
                if(i%3==0){
                    require(!physics.clearSight(a,offset(a,radial,.34)),"Carrier has a perforated wall at "+i);
                    wall++;
                }
            }
            if(i%5==0){
                for(double h:new double[]{HunterPosture.STANDING_HEIGHT,HunterPosture.CROUCH_HEIGHT}){
                    require(physics.bodyBlocked(new Vec(a.x(),a.y()-.13,a.z()),h,.22),
                        "A player capsule fits the hydraulic carrier at "+a);
                    body++;
                }
            }
        }
        var intakeBefore=new Vec(27.14,-4.04,-28);
        var intakeAfter=new Vec(29.9,-4.04,-28);
        require(!original.clearSight(intakeBefore,intakeAfter)&&physics.clearSight(intakeBefore,intakeAfter),
            "Capped intake did not become a real open connection");
        var tailMouth=new Vec(73.89,-6.93,44.49);
        require(physics.clearSight(tailMouth,new Vec(73.89,-6.93,44.89)),"Tailrace receiver did not connect to the carrier");
        require(!physics.clearSight(tailMouth,new Vec(73.89,-6.93,43.72)),"Existing isolation sluice leaked");
        require(!physics.clearSight(new Vec(73.89,-6.89,46.78),new Vec(73.89,-6.89,46.24)),
            "Receiving bench grade return has an exposed opening");
        require(WorldWater.module(level.water()).bodies().size()==2,"Independent retained water placements were lost");

        var captures=packet.resolve(gpu?"captures/opengl":"captures/software");
        Files.createDirectories(captures);
        var bank=physics.support(32.8,-24,-4,1);
        require(bank!=null&&map.traversable(32.8,bank.y(),-24),"Intake bank review lacks a standing surface");
        int intake=capture(map,new Vec(32.8,bank.y(),-24),new Vec(28.7,-3.97,-28.0),captures.resolve("covered-intake-from-bank.png"));
        var walk=new Vec(72.45,0,42.3);
        require(map.roomAt(walk.x(),walk.y(),walk.z())==11&&map.traversable(walk.x(),walk.y(),walk.z()),
            "Downstream review lost the real catwalk stance");
        var shoulder=physics.support(78.25,41.25,-6.4,.75);
        require(shoulder!=null&&map.traversable(78.25,shoulder.y(),41.25),"Receiving shoulder lacks real grade support");
        capture(map,new Vec(78.25,shoulder.y(),41.25),new Vec(74.2,-6.05,44.9),
            captures.resolve("receiving-headwall-from-east-bank.png"));
        int tail=capture(map,new Vec(72.45,0,34.2),new Vec(73.89,-6.82,24.5),
            captures.resolve("retained-isolated-tailrace.png"));
        require(intake>3500&&tail>350,"Water or native camera disappeared at an existing bank "+intake+"/"+tail);
        var report=List.of("check\tresult","native_scene_revision\t"+data.nativeScene().revision(),
            "collision_sha256\t"+physics.sha256(),"level_revision\t"+level.revision(),
            "water_revision\t"+level.water().revision(),
            "renderer\t"+(gpu?GpuWorldRenderer.active().rendererDescription():"Software"),
            "carrier_stations\t"+route.size(),"unobstructed_axial_and_offset_rays\t"+bore,
            "steel_wall_cross_sections_rejected\t"+wall,"hunter_capsules_rejected\t"+body,
            "occupied_room_crossings\t0","intake_new_open_bore_and_closed_downstream_isolation\tPASS",
            "retained_intake_and_tailrace_water_pixels\t"+intake+"/"+tail);
        Files.write(packet.resolve(gpu?"carrier-opengl.tsv":"carrier-software.tsv"),report);
        report.forEach(System.out::println);
    }

    private static List<Vec> readProfile(Path path)throws Exception {
        var rows=Files.readAllLines(path);
        var route=new ArrayList<Vec>();
        for(String line:rows.subList(1,rows.size())){
            var p=line.split("\t");
            double x=Double.parseDouble(p[2]),y=Double.parseDouble(p[3]),z=Double.parseDouble(p[4]);
            require(Integer.parseInt(p[0])==route.size(),"Carrier station schedule out of order");
            if(p[7].equals("buried"))require(Double.parseDouble(p[6])>=.56,"Buried carrier lost measured cover");
            route.add(new Vec(x,y,z));
        }
        require(route.size()>=470,"Truncated carrier profile");
        return route;
    }

    private static Vec unit(Vec v){
        double length=Math.sqrt(v.x()*v.x()+v.y()*v.y()+v.z()*v.z());
        return new Vec(v.x()/length,v.y()/length,v.z()/length);
    }

    private static Vec offset(Vec p,Vec direction,double distance){
        return new Vec(p.x()+direction.x()*distance,p.y()+direction.y()*distance,p.z()+direction.z()*distance);
    }

    private static int capture(HouseMap map,Vec feet,Vec target,Path image)throws Exception {
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
        renderer.setWeather(new MissionWeather.State(EvidenceSignals.Season.AUTUMN,EvidenceSignals.Weather.CLEAR,
            MissionWeather.Mode.FIXED,1,0));
        renderer.lamp=true;
        renderer.setLampHardware(EquipmentTiers.Tier.III,false);
        NativeSchoolInteriorReview.drawFrame(renderer,map,image);
        return renderer.waterStatistics==null?0:renderer.waterStatistics.pixels();
    }

    private static void require(boolean pass,String message){if(!pass)throw new AssertionError(message);}
}
