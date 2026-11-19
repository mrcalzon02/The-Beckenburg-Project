
package org.beckenburg;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.beckenburg.LevelDefinition.Dock;
import org.beckenburg.LevelDefinition.Portal;
import org.beckenburg.LevelDefinition.Socket;
import org.beckenburg.LevelDefinition.Vec;

/** Replays the exact native School leaves and routes through the production game kernel. */
public final class NativeSchoolInteriorReview {
    static double d(String[] row,int i){return Double.parseDouble(row[i]);}
    static Vec point(Portal p,double normal,double tangent,double h){
        return new Vec(p.x()+(p.alongX()?tangent:normal),p.y()+h,p.z()+(p.alongX()?normal:tangent));
    }
    static boolean sweep(AuthoredSitePhysics physics, NativeInvestigationDoor.Spec s) {
        var p=s.portal();
        double baseline=(s.frameFrom()+s.frameTo())/2-(p.alongX()?p.z():p.x());
        for(int side:s.leaves()==1?new int[]{s.hingeSide()}:new int[]{-1,1}){
            for(int a=1;a<=18;a++){
                double angle=Math.PI*a/36;
                for(double fraction:new double[]{.15,.35,.55,.75,.94}){
                    double length=p.width()/s.leaves()*fraction;
                    double tangent=side*(p.width()/2-length*Math.cos(angle));
                    double normal=baseline+s.swingDirection()*length*Math.sin(angle);
                    for(double t:new double[]{0,-.033,.033}){
                        Vec foot=point(p,normal,tangent+t,0);
                        var floor=physics.support(foot.x(),foot.z(),foot.y(),.08);
                        if(floor==null||Math.abs(floor.y()-foot.y())>.045)return false;
                        if(!physics.clearSight(new Vec(foot.x(),foot.y()+.08,foot.z()),
                            new Vec(foot.x(),foot.y()+s.clearHeight()-.07,foot.z())))return false;
                    }
                }
            }
        }
        return true;
    }
    private static void require(boolean pass, String label) {
        if (!pass) throw new AssertionError(label);
    }

    private static boolean crosses(HouseMap map, Vec a, Vec b, double height) {
        Vec at=a;
        for (int i=1;i<=20;i++) {
            at=map.resolveMove(at,a.x()+(b.x()-a.x())*i/20,a.z()+(b.z()-a.z())*i/20,height);
            if (at==null) return false;
        }
        return Math.abs(at.y()-b.y())<.05;
    }

    private static void audit(HouseMap map, NativeInvestigationDoor.Spec spec, boolean open) {
        var p=spec.portal();
        for (double lane:new double[]{-.18,0,.18}) {
            var a=point(p,-.68,lane,0);
            var b=point(p,.68,lane,0);
            if(lane==0) {
                require(Set.of(spec.roomA(),spec.roomB()).equals(Set.of(
                    map.nativeInvestigation().spaceAt(a.x(),a.y()+1,a.z()).id(),map.nativeInvestigation().spaceAt(b.x(),b.y()+1,b.z()).id())),
                    "Architectural owner "+spec.frameId());
                int loss=map.architecturalSoundLoss(a.x(),a.y(),a.z(),b.x(),b.y(),b.z());
                require(loss==(open?1:3),"Native acoustic leak "+spec.frameId()+" "+loss);
            }
            for (double h:new double[]{HunterPosture.STANDING_HEIGHT,HunterPosture.CROUCH_HEIGHT}) {
                require(map.traversable(a.x(),a.y(),a.z(),h)&&map.traversable(b.x(),b.y(),b.z(),h),
                    "Obstructed receiving floor "+spec.frameId());
                require(crosses(map,a,b,h)==open&&crosses(map,b,a,h)==open,
                    "Physical crossing "+spec.frameId()+" lane "+lane+" open "+open);
            }
            for (double h:new double[]{.35,1.2,1.8}) {
                var from=point(p,-.68,lane,h);
                var to=point(p,.68,lane,h);
                require(map.clearLineOfSight(from.x(),from.y(),from.z(),to.x(),to.y(),to.z())==open,"Sight "+spec.frameId());
                require(map.clearObjectPath(from.x(),from.y(),from.z(),to.x(),to.y(),to.z(),.035,.035)==open,
                    "Object path "+spec.frameId());
            }
        }
        if (!open) {
            var eye=point(p,-.84,0,0);
            double yaw=p.alongX()?0:Math.PI/2;
            var actor=new LooseClutter.Observer(0,eye.x(),eye.y(),eye.z(),yaw,-.11,true);
            var target=InteractionFocus.pick(map,actor,List.of());
            require(target!=null&&target.kind()==InteractionFocus.Kind.DOOR&&target.id().equals(Integer.toString(p.id())),
                "Closed handle aim "+spec.frameId());
        }
    }

    private static Vec walk(HouseMap map,List<Vec> route,double height,String label) {
        Vec at=route.get(0);
        require(map.traversable(at.x(),at.y(),at.z(),height),"Unsupported start "+label+" "+at);
        for(int j=1;j<route.size();j++){
            var start=at;
            var end=route.get(j);
            int steps=Math.max(1,(int)Math.ceil(Math.hypot(end.x()-start.x(),end.z()-start.z())/.13));
            for(int k=1;k<=steps;k++){
                double t=(double)k/steps;
                var next=map.resolveMove(at,start.x()+(end.x()-start.x())*t,start.z()+(end.z()-start.z())*t,height);
                require(next!=null,"Movement lost at "+label+" near "+at+" to "+end);
                at=next;
            }
            require(Math.abs(at.y()-end.y())<.05,"Wrong receiving height "+label+" "+at+" versus "+end);
        }
        return at;
    }

    private static void replay(HouseMap map,List<String[]> rows,List<String> report) {
        var van=map.van();
        var cargo=van.world(0,VanStaging.FRONT+.45);
        var arrival=new ArrayList<Vec>();
        arrival.add(new Vec(cargo.x(),van.floorY(),cargo.z()));
        for(var row:rows)if(row[0].equals("POINT"))arrival.add(new Vec(d(row,1),d(row,2),d(row,3)));
        int count=0;
        for(double posture:new double[]{HunterPosture.STANDING_HEIGHT,HunterPosture.CROUCH_HEIGHT}){
            walk(map,arrival,posture,"van to foyer");
            var outbound=new ArrayList<>(arrival);
            Collections.reverse(outbound);
            walk(map,outbound,posture,"foyer to van");
            var reached=new HashMap<String,Vec>();
            reached.put("stage",arrival.get(1));
            for(var row:rows)if(row[0].equals("TRANSIT")){
                var source=reached.get(row[1]);
                require(source!=null,"Unconnected arrival guide "+row[1]);
                var route=new ArrayList<Vec>();
                route.add(source);
                for(int i=6;i<row.length;i+=3)route.add(new Vec(d(row,i),d(row,i+1),d(row,i+2)));
                String label=row[1]+" to "+row[2]+" posture "+posture;
                var next=walk(map,route,posture,label);
                var owner=map.nativeInvestigation().spaceAt(next.x(),next.y(),next.z());
                require(owner!=null&&owner.id().equals(row[2])&&owner.accessible(),"Wrong receiving room "+label);
                reached.put(row[2],next);
                var reversed=new ArrayList<>(route);
                Collections.reverse(reversed);
                reversed.set(0,next);
                walk(map,reversed,posture,"return: "+label);
                count+=2;
            }
            require(reached.size()==40,"Wrong reachable search count "+reached.size());
        }
        report.add("contiguous_cargo_to_foyer_both_ways_and_postures\tPASS");
        report.add("space_link_direction_replays\t"+count);
    }

    private static void render(HouseMap map, Portal p, Path target) throws Exception {
        var renderer=new SceneRenderer();
        renderer.setRenderSize(880,580);
        renderer.setPsxPreset(PsxPipeline.Preset.OFF);
        renderer.lamp=true;
        renderer.setLampHardware(EquipmentTiers.Tier.III,false);
        Vec camera=point(p,-1.45,-.22,0);
        if(!map.traversable(camera.x(),camera.y(),camera.z())) camera=point(p,-.82,0,0);
        require(map.traversable(camera.x(),camera.y(),camera.z()),"Unsupported review camera "+p.id());
        renderer.x=camera.x();
        renderer.z=camera.z();
        renderer.eyeHeight=camera.y()+HunterPosture.STANDING_EYE;
        renderer.yaw=Math.atan2(p.x()-camera.x(),p.z()-camera.z());
        renderer.pitch=-.10;
        renderer.drawNetwork(map,List.of(),-1,0);
        var gpu=GpuWorldRenderer.active();
        if(gpu!=null){
            require(renderer.awaitSubmittedGpuPrewarm(30_000_000_000L,()->false),"No School GPU frame");
            renderer.pinCompletedGpuFrame(gpu.latest());
            renderer.drawNetwork(map,List.of(),-1,0);
            require(renderer.frameDiagnostics().gpuReadbackAvailable(),"Missing School GPU readback");
        }
        require(javax.imageio.ImageIO.write(renderer.image(),"png",target.toFile()),"PNG encoder unavailable");
    }

    public static void main(String[] args)throws Exception{
        if(!List.of(args).contains("--gpu")){review(args);return;}
        var launcher=Class.forName("org.beckenburg.gpu.GpuDesktop");
        var start=launcher.getDeclaredMethod("startGpu");
        var stop=launcher.getDeclaredMethod("stopReviewGpu");
        start.setAccessible(true);
        stop.setAccessible(true);
        start.invoke(null);
        try{review(args);}finally{stop.invoke(null);}
    }

    private static void exportTriangles(NativeInvestigationScene scene, Path file)throws Exception {
        try(var out=Files.newBufferedWriter(file)){
            for(var door:scene.doors())for(boolean open:new boolean[]{false,true}){
                for(var triangle:door.pose(open).model().triangles()){
                    out.write(door.spec().portal().id()+"\t"+(open?"open":"closed"));
                    for(var vertex:List.of(triangle.a(),triangle.b(),triangle.c()))
                        out.write("\t"+vertex.x()+"\t"+vertex.y()+"\t"+vertex.z());
                    out.newLine();
                }
            }
        }
    }

    private static void review(String[] args)throws Exception{
        var rows=Files.readAllLines(Path.of(args[0])).stream().map(line->line.split("\t",-1)).toList();
        var source=rows.get(0);
        var rooms=new ArrayList<NativeInvestigationScene.Space>();
        var stairs=new HashSet<String>();
        for(var r:rows)switch(r[0]){
            case "SPACE" -> rooms.add(new NativeInvestigationScene.Space(r[1],Integer.parseInt(r[2]),
                d(r,3),d(r,4),d(r,5),d(r,6),d(r,7),d(r,8),Boolean.parseBoolean(r[9]),Boolean.parseBoolean(r[10])));
            case "TREAD" -> stairs.add(r[1]);
            default -> {}
        }
        var schedule=Files.readAllLines(Path.of(args[1])).stream().map(line->line.split("\t",-1)).toList();
        var specs=new ArrayList<NativeInvestigationDoor.Spec>();
        var sockets=new ArrayList<Socket>();
        for(var f:schedule){
            if(f[0].equals("RITUAL")){
                sockets.add(new Socket("school-ritual-"+f[1],"ritual",Integer.parseInt(f[2]),d(f,3),d(f,4),d(f,5),0,f[1]));
                continue;
            }
            var p=new Portal(Integer.parseInt(f[0]),Integer.parseInt(f[4]),Integer.parseInt(f[5]),
                d(f,6),d(f,7),d(f,8),d(f,9),f[10].equals("z"),"door",1);
            specs.add(new NativeInvestigationDoor.Spec(f[1],f[2],f[3],p,d(f,11),d(f,12),d(f,13),
                Integer.parseInt(f[14]),Integer.parseInt(f[15]),Integer.parseInt(f[16]),f[17]));
        }
        NativeInvestigationScene scene;
        try(var mesh=Files.newInputStream(Path.of(source[1]));var col=Files.newInputStream(Path.of(source[3]))){
            scene=NativeInvestigationScene.read(mesh,source[2],col,source[4],stairs,rooms,specs);
        }
        var dock=rows.get(1);
        var existing=LevelPlans.buildSource(HouseMap.Site.SCHOOL);
        var definition=new LevelDefinition("school",scene.revision(),"Ravenshade fitted thresholds",
            existing.rooms(),specs.stream().map(NativeInvestigationDoor.Spec::portal).toList(),
            List.of(),List.of(),List.of(),sockets,List.of(),new Dock(d(dock,1),d(dock,2),d(dock,3),d(dock,4)),
            existing.ambience(),existing.water(),existing.sounds());
        var host=new HouseMap(HouseMap.Site.SCHOOL,definition,null,scene);
        var guest=new HouseMap(HouseMap.Site.SCHOOL,definition,null,scene);
        require(specs.size()==17,"Missing a fitted School frame");
        require(RitualSites.valid(host),"Existing School ritual installations lost their supported room ownership");
        var output=Path.of(args[3]);
        Files.createDirectories(output);
        var report=new ArrayList<String>();
        report.add("measure\tresult");
        report.add("native_scene_revision\t"+scene.revision());
        report.add("retained_ritual_anchors\t3 supported and correctly owned");
        for(var spec:specs){
            require(sweep(scene.physics(),spec),"Leaf sweeps an unsupported or solid area "+spec.frameId());
            audit(host,spec,false);
            int id=spec.portal().id();
            boolean capture=id==401||id==404||id==413||id==414||id==417;
            if(capture)render(host,spec.portal(),output.resolve("school-frame-"+id+"-closed.png"));
            host.toggleDoor(id,System.nanoTime()-2_000_000_000L);
            guest.applyDoorSnapshot(host.doorSnapshot());
            audit(host,spec,true);
            audit(guest,spec,true);
            if(capture)render(host,spec.portal(),output.resolve("school-frame-"+id+"-open.png"));
            host.toggleDoor(id,System.nanoTime()-2_000_000_000L);
            guest.applyDoorSnapshot(host.doorSnapshot());
            audit(guest,spec,false);
            report.add(spec.frameId()+"\t"+id+" "+spec.roomA()+" / "+spec.roomB()+" PASS fitted swing, body, sight, objects, aim and acoustics");
            System.out.println(report.get(report.size()-1));
        }
        for(var spec:specs)host.toggleDoor(spec.portal().id(),System.nanoTime()-2_000_000_000L);
        for(var spec:specs)audit(host,spec,true);
        report.add("all_open_crossing\tPASS");
        replay(host,rows,report);
        report.add("spaces\t"+rooms.stream().filter(NativeInvestigationScene.Space::accessible).count());
        Files.write(Path.of(args[2]),report);
        if(args.length>4&&!args[4].equals("--gpu")) exportTriangles(scene,Path.of(args[4]));
    }
}
