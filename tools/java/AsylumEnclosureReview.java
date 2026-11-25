package org.beckenburg;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.beckenburg.LevelDefinition.Vec;
import org.beckenburg.editor.DefaultPlayerMotion;

/** Verifies the built hospital perimeter using the packaged scene and movement kernel. */
final class AsylumEnclosureReview {
    private static final double GATE=92.4;
    private static final double ROAD=32;

    private AsylumEnclosureReview(){}

    public static void main(String[] args)throws Exception{
        var stage=Path.of(args[0]);
        boolean gpu=List.of(args).contains("--gpu");
        if(!gpu){review(stage,false);return;}
        var desktop=Class.forName("org.beckenburg.gpu.GpuDesktop");
        var start=desktop.getDeclaredMethod("startGpu");
        var stop=desktop.getDeclaredMethod("stopReviewGpu");
        start.setAccessible(true);
        stop.setAccessible(true);
        start.invoke(null);
        try{review(stage,true);}finally{stop.invoke(null);}
    }

    private static void review(Path stage,boolean gpu)throws Exception{
        System.setProperty(AsylumNativeLevel.REVIEW_PROPERTY,"true");
        var map=new HouseMap(HouseMap.Site.ASYLUM);
        var scene=map.nativeInvestigation();
        var physics=scene.physics();
        require(map.level().socket("native_perimeter",13)!=null && scene.doors().size()==13 &&
            HidingBudget.candidates(map.level()).size()==3 && RitualSites.valid(map),
            "The enclosed archive lost its physical mission authority");
        var report=new ArrayList<String>();
        report.add("named_revision\t"+map.level().revision());
        report.add("renderer\t"+(gpu?GpuWorldRenderer.active().rendererDescription():"Software"));
        var soil=terrainTriangles(physics);
        report.addAll(MonasteryPerimeterReview.auditRing(physics,stage.resolve("boundary-survey.tsv"),"Asylum",
            (x,z)->ground(soil,x,z)));

        var baseline=stage.resolve("../asylum-flight-rails-pass");
        var previous=baseline.resolve("asylum-stair.hcs2");
        var previousHash=baseline.resolve("arrival.tsv");
        String digest=Files.readAllLines(previousHash).get(0).split("\t")[4];
        try(var input=Files.newInputStream(previous)){
            var before=AuthoredSitePhysics.read(input,digest,List.of());
            require(before.support(ROAD,95,0,2)==null &&
                before.clearSight(new Vec(ROAD,1.1,GATE-1),new Vec(ROAD,1.1,GATE+1)),
                "Old archive should expose the open road edge");
        }
        try(var oldModel=Files.newInputStream(baseline.resolve("asylum-stair.glb"));
            var oldCollision=Files.newInputStream(previous)){
            var old=AsylumNativeLevel.read(rows(previousHash),rows(baseline.resolve("door-survey.tsv")),
                rows(baseline.resolve("mission.tsv")),rows(baseline.resolve("services.tsv")),oldModel,oldCollision);
            var legacy=new HouseMap(HouseMap.Site.ASYLUM,old.definition(),null,old.scene());
            double z=86.55,y=legacy.groundHeight(ROAD,z,13);
            require(legacy.traversable(ROAD,y,z)&&
                !legacy.airborneClear(ROAD,y+.73,z,HunterPosture.CROUCH_HEIGHT),
                "Old archive did not reproduce the invisible limit on a supported road pose");
        }
        report.add("prior_open_edge_and_airborne_limit\tPASS negative controls reproduced on the old archive");
        int grounded=0;
        for(double z=84;z<100;z+=.1){
            if(Math.abs(z-GATE)<.46)continue;
            var bearing=physics.support(ROAD,z,0,.75);
            require(bearing!=null && physics.traversable(new Vec(ROAD,bearing.y(),z),HunterPosture.STANDING_HEIGHT),
                "Discontinuous road at "+z);
            grounded++;
        }
        require(!map.clearLineOfSight(ROAD,1.1,GATE-1,ROAD,1.1,GATE+1),"Closed carriage gate did not block sight");
        report.add("road_seam_footprints\t"+grounded);
        verifyJumpAndSpectral(map,report);

        var captures=stage.resolve(gpu?"captures/opengl":"captures/software");
        Files.createDirectories(captures);
        for(var view:List.of(new Vec[]{new Vec(32,map.groundHeight(32,85.5,13),85.5),new Vec(32,.95,GATE)},
            new Vec[]{new Vec(36,map.groundHeight(36,84,13),84),new Vec(32,1,76)})){
            var at=view[0];
            double d=Math.hypot(view[1].x()-at.x(),view[1].z()-at.z());
            require(map.traversable(at.x(),at.y(),at.z()),"Camera did not stand on the authored approach");
            var renderer=new SceneRenderer();
            renderer.setRenderSize(960,640);
            renderer.setPsxPreset(PsxPipeline.Preset.OFF);
            renderer.x=at.x();renderer.z=at.z();
            renderer.eyeHeight=at.y()+HunterPosture.STANDING_EYE;
            renderer.yaw=Math.atan2(view[1].x()-at.x(),view[1].z()-at.z());
            renderer.pitch=Math.atan2(view[1].y()-renderer.eyeHeight,d);
            renderer.weatherModule().setTimeOfDay(.50);
            renderer.lamp=true;
            renderer.setLampHardware(EquipmentTiers.Tier.III,false);
            NativeSchoolInteriorReview.drawFrame(renderer,map,captures.resolve(
                at.z()>85?"asylum-fitted-carriage-gate.png":"asylum-canonical-van-from-road.png"));
        }
        Files.write(stage.resolve(gpu?"named-opengl.tsv":"named-software.tsv"),report);
        report.forEach(System.out::println);
    }

    private static List<String[]> rows(Path file)throws Exception{
        return Files.readAllLines(file).stream().map(row->row.split("\t",-1)).toList();
    }

    private static List<AuthoredSitePhysics.Triangle> terrainTriangles(AuthoredSitePhysics physics){
        var terrain=new ArrayList<AuthoredSitePhysics.Triangle>();
        for(int index=0;index<physics.triangleCount();index++){
            var triangle=physics.triangle(index);
            String part=physics.componentName(triangle.component());
            if(part.equals("terrain-site:surface")||part.equals("Asylum fitted exterior terrain"))terrain.add(triangle);
        }
        require(!terrain.isEmpty(),"No bound grade triangles");
        return terrain;
    }

    private static double ground(List<AuthoredSitePhysics.Triangle> terrain,double x,double z){
        // The grade check reads the soil itself. At acute mitres an ordinary
        // vertical ray can strike the adjacent coping before reaching the ground.
        for(var triangle:terrain){
            var a=triangle.a();var b=triangle.b();var c=triangle.c();
            if(x<Math.min(a.x(),Math.min(b.x(),c.x()))-.0001 || x>Math.max(a.x(),Math.max(b.x(),c.x()))+.0001 ||
               z<Math.min(a.z(),Math.min(b.z(),c.z()))-.0001 || z>Math.max(a.z(),Math.max(b.z(),c.z()))+.0001)continue;
            double ax=b.x()-a.x(),az=b.z()-a.z(),bx=c.x()-a.x(),bz=c.z()-a.z();
            double area=ax*bz-az*bx;
            if(Math.abs(area)<1e-9)continue;
            double dx=x-a.x(),dz=z-a.z();
            double u=(dx*bz-dz*bx)/area,v=(ax*dz-az*dx)/area;
            if(u>=-1e-5&&v>=-1e-5&&u+v<=1.00001)return a.y()+u*(b.y()-a.y())+v*(c.y()-a.y());
        }
        throw new AssertionError("Missing ground at "+x+","+z);
    }

    private static void verifyJumpAndSpectral(HouseMap map,List<String> report){
        double z=GATE-.98,y=map.groundHeight(ROAD,z,13);
        require(map.traversable(ROAD,y,z)&&map.airborneClear(ROAD,y+.73,z,HunterPosture.CROUCH_HEIGHT),
            "The legacy rectangle still blocks the supported gate approach");
        var motion=new DefaultPlayerMotion();
        var collision=new DefaultPlayerMotion.Collision(){
            public boolean clear(double x,double y,double z,double height){return map.airborneClear(x,y,z,height);}
            public double landing(double x,double z,double from,double to){return map.jumpLanding(x,z,from,to);}
        };
        motion.jump(ROAD,y,z,0,4.65,HunterPosture.CROUCH_HEIGHT,collision);
        for(int i=0;i<18 && motion.active();i++)motion.advance(.1,HunterPosture.CROUCH_HEIGHT,collision);
        require(!motion.active()&&motion.z()>z+.2&&motion.z()<GATE-.13,
            "Running jump did not land behind the fitted gate "+motion.z());
        var ghost=new LanGhostSpatial(map,13);
        var at=ghost.pose();
        for(int i=1;i<=17;i++)ghost.step(100,0,i);
        boolean stopped=false;
        for(int i=18;i<=90;i++){
            try{ghost.step(0,100,i);}
            catch(IllegalArgumentException end){
                require(end.getMessage().equals("spectral room boundary; use DRIFT"),end.getMessage());
                stopped=true;
                break;
            }
        }
        require(stopped&&ghost.pose().room()==13&&ghost.pose().z()>at.z()+5&&ghost.pose().z()<GATE,
            "Spectral traveller did not stay inside the staged grounds "+ghost.pose());
        report.add("running_jump_and_spectral_edge\tPASS jump landed inside the closed road gate and the ghost stopped at its room boundary");
    }

    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
