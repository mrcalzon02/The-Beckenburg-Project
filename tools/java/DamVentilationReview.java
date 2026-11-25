package org.beckenburg;

import org.beckenburg.LevelDefinition.Vec;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/** Checks the generator's fitted service openings against the ordinary game mesh and collision. */
public final class DamVentilationReview {
    private DamVentilationReview() {}

    public static void main(String[] args)throws Exception {
        Path packet=Path.of(args[0]), prior=Path.of(args[1]), power=Path.of(args[2]);
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
        for(var name:List.of("runtime-input.tsv","door-schedule.tsv","electrical-schedule.tsv","blackwater.water","carrier-profile.tsv"))
            require(Arrays.equals(Files.readAllBytes(packet.resolve(name)),Files.readAllBytes(prior.resolve(name))),
                "Mechanical assembly changed established authority for "+name);
        Path water=packet.resolve("blackwater.water");
        var before=DamDoorReview.read(prior,water,"dam-with-carrier").nativeScene().physics();
        var actual=DamDoorReview.read(packet,water,"dam-with-ventilation");
        var p=actual.nativeScene().physics();
        var base=DamPowerReview.read(power,water).level();
        var level=new LevelDefinition(base.id(),actual.nativeScene().revision(),base.title(),base.rooms(),
            base.portals(),base.stairs(),base.voids(),base.props(),base.sockets(),base.colliders(),
            base.dock(),base.ambience(),base.water(),base.sounds());
        var map=new HouseMap(HouseMap.Site.USER,level,null,actual.nativeScene());
        require(map.valid()&&RitualSites.valid(map),"Fitted mechanical assembly invalidated gameplay owners");
        int open=0,baffles=0,sealed=0,capsules=0;
        for(double x:new double[]{40.40,40.42,40.44}){
            for(double y:new double[]{-4.069,-4.05,-4.031}){
                var a=new Vec(x,y,8.46);
                var b=new Vec(x,y,7.73);
                require(!before.clearSight(a,b)&&p.clearSight(a,b),"Flue sleeve blocks the fitted bore "+a);
                open++;
            }
        }
        for(double y:new double[]{-3.80,-3.61,-3.34,-3.02,-2.74,-2.54}){
            require(!p.clearSight(new Vec(40.42,y,7.54),new Vec(40.60,y,7.54)),
                "External stack has an open side at "+y);
            sealed++;
        }
        for(double x:new double[]{44.02,44.14,44.27,44.40,44.54}){
            for(int blade=0;blade<5;blade++){
                double y=-4.98+.029+blade*.102+.053;
                require(p.clearSight(new Vec(x,y-.024,7.66),new Vec(x,y+.116,8.247)),
                    "Weather louver has no inclined air gap at "+x+"/"+y);
                open++;
            }
            for(int i=0;i<23;i++){
                double y=-4.94+i*.026;
                require(!p.clearSight(new Vec(x,y,7.66),new Vec(x,y,8.24)),
                    "A straight sightline crosses the weather louver "+x+"/"+y);
                baffles++;
            }
            for(double height:new double[]{HunterPosture.STANDING_HEIGHT,HunterPosture.CROUCH_HEIGHT}){
                require(p.bodyBlocked(new Vec(x,-5.14,8.0),height,.22),"A body can enter the air opening");
                capsules++;
            }
        }
        for(double x:new double[]{36.3,38,40.27,40.57,41.25,43.79,44.73,46.9,50,53.7}){
            for(double y:new double[]{-5.6,-5.1,-4.75,-4.32,-3.81,-3.35}){
                require(!p.clearSight(new Vec(x,y,7.68),new Vec(x,y,8.31)),"An unplanned wall opening appeared");
                sealed++;
            }
        }
        var captures=packet.resolve(gpu?"captures/opengl":"captures/software");
        Files.createDirectories(captures);
        var interior=new Vec(42.55,-6,13.3);
        require(map.traversable(interior.x(),interior.y(),interior.z()),"No interior standing inspection");
        capture(map,interior,new Vec(42.20,-4.52,8.17),captures.resolve("generator-flue-and-intake-inside.png"));
        var apron=p.support(42.32,3.87,-6.38,.40);
        require(apron!=null&&map.traversable(42.32,apron.y(),3.87),"No supported exterior ventilation inspection");
        capture(map,new Vec(42.32,apron.y(),3.87),new Vec(42.36,-3.55,7.87),
            captures.resolve("generator-ventilation-outside.png"));
        var report=List.of("check\tresult","native_scene_revision\t"+actual.nativeScene().revision(),
            "collision_sha256\t"+p.sha256(),"level_revision\t"+level.revision(),
            "water_revision\t"+level.water().revision(),
            "renderer\t"+(gpu?GpuWorldRenderer.active().rendererDescription():"Software"),
            "fitted_bore_and_louver_air_rays\t"+open,"straight_louver_sightlines_blocked\t"+baffles,
            "stack_shell_and_retained_wall_rays_blocked\t"+sealed,"player_capsules_rejected\t"+capsules);
        Files.write(packet.resolve(gpu?"ventilation-opengl.tsv":"ventilation-software.tsv"),report);
        report.forEach(System.out::println);
    }

    private static void capture(HouseMap map,Vec feet,Vec target,Path image)throws Exception {
        var renderer=new SceneRenderer();
        renderer.setRenderSize(960,640);
        renderer.setPsxPreset(PsxPipeline.Preset.OFF);
        renderer.x=feet.x();
        renderer.z=feet.z();
        renderer.eyeHeight=feet.y()+HunterPosture.STANDING_EYE;
        renderer.yaw=Math.atan2(target.x()-feet.x(),target.z()-feet.z());
        renderer.pitch=Math.atan2(target.y()-renderer.eyeHeight,Math.hypot(target.x()-feet.x(),target.z()-feet.z()));
        renderer.roomLights=new RoomLighting();
        renderer.weatherModule().setTimeOfDay(.5);
        renderer.lamp=true;
        renderer.setLampHardware(EquipmentTiers.Tier.III,false);
        NativeSchoolInteriorReview.drawFrame(renderer,map,image);
    }

    private static void require(boolean pass,String message){if(!pass)throw new AssertionError(message);}
}
