package org.beckenburg;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import javax.imageio.ImageIO;
import org.beckenburg.LevelDefinition.Dock;
import org.beckenburg.LevelDefinition.Portal;
import org.beckenburg.LevelDefinition.Socket;
import org.beckenburg.LevelDefinition.Vec;

/** Headless review of the staged School foyer on the shared game runtime. */
public final class NativeSchoolDoorReview {
    private record Arrival(HouseMap host, HouseMap guest, List<Vec> approach) { }

    private static double d(String[] row,int i) {return Double.parseDouble(row[i]);}
    private static Vec point(String[] row,int i) {return new Vec(d(row,i),d(row,i+1),d(row,i+2));}
    private static void check(boolean valid,String message) {
        if(!valid)throw new AssertionError(message);
    }

    private static Arrival load(Path plan,Path roomPlan)throws Exception {
        var rows=Files.readAllLines(roomPlan).stream().map(line->line.split("\t",-1)).toList();
        var files=rows.get(0);
        var dock=rows.get(1);
        var rooms=new ArrayList<NativeInvestigationScene.Space>();
        var stairs=new HashSet<String>();
        var approach=new ArrayList<Vec>();
        for(var row:rows)switch(row[0]){
            case "SPACE" -> rooms.add(new NativeInvestigationScene.Space(row[1],Integer.parseInt(row[2]),
                  d(row,3),d(row,4),d(row,5),d(row,6),d(row,7),d(row,8),
                  Boolean.parseBoolean(row[9]),Boolean.parseBoolean(row[10])));
            case "TREAD" -> stairs.add(row[1]);
            case "POINT" -> approach.add(point(row,1));
            default -> {}
        }
        var schedule=Files.readAllLines(plan);
        var f=schedule.get(0).split("\t",-1);
        var portal=new Portal(Integer.parseInt(f[0]),Integer.parseInt(f[4]),Integer.parseInt(f[5]),
                d(f,6),d(f,7),d(f,8),d(f,9),f[10].equals("z"),"door",1);
        var spec=new NativeInvestigationDoor.Spec(f[1],f[2],f[3],portal,d(f,11),d(f,12),d(f,13),
                Integer.parseInt(f[14]),Integer.parseInt(f[15]),Integer.parseInt(f[16]),f[17]);
        NativeInvestigationScene scene;
        try(var mesh=Files.newInputStream(Path.of(files[1]));var collision=Files.newInputStream(Path.of(files[3]))){
            scene=NativeInvestigationScene.read(mesh,files[2],collision,files[4],stairs,rooms,List.of(spec));
        }
        var sockets=new ArrayList<Socket>();
        for(String line:schedule.subList(1,schedule.size())){
            var row=line.split("\t",-1);
            check(row[0].equals("RITUAL"),"Unknown native School renderer socket");
            var pos=point(row,3);
            sockets.add(new Socket("school-ritual-"+row[1],"ritual",Integer.parseInt(row[2]),
                    pos.x(),pos.y(),pos.z(),0,row[1]));
        }
        var existing=LevelPlans.buildSource(HouseMap.Site.SCHOOL);
        var candidate=new LevelDefinition("school",scene.revision(),"Ravenshade fitted foyer",
                existing.rooms(),List.of(portal),List.of(),List.of(),List.of(),sockets,List.of(),
                new Dock(d(dock,1),d(dock,2),d(dock,3),d(dock,4)),existing.ambience(),
                existing.water(),existing.sounds());
        return new Arrival(new HouseMap(HouseMap.Site.SCHOOL,candidate,null,scene),
                new HouseMap(HouseMap.Site.SCHOOL,candidate,null,scene),List.copyOf(approach));
    }

    private static Vec walk(HouseMap map,List<Vec> points,double height) {
        Vec at=points.get(0);
        if(!map.traversable(at.x(),at.y(),at.z(),height))return null;
        for(int i=1;i<points.size();i++){
            Vec start=at,end=points.get(i);
            int steps=Math.max(1,(int)Math.ceil(Math.hypot(end.x()-start.x(),end.z()-start.z())/.16));
            for(int n=1;n<=steps;n++){
                double t=(double)n/steps;
                at=map.resolveMove(at,start.x()+t*(end.x()-start.x()),start.z()+t*(end.z()-start.z()),height);
                if(at==null)return null;
            }
        }
        Vec dest=points.get(points.size()-1);
        return Math.abs(at.y()-dest.y())<.045?at:null;
    }

    private static int audit(HouseMap map,boolean open) {
        var door=map.nativeInvestigation().doors().get(0);
        var p=door.spec().portal();
        check(map.doorOpen(p.id())==open,"Wrong state for actual frame");
        Vec outside=new Vec(p.x(),p.y(),p.z()+.68),inside=new Vec(p.x(),p.y(),p.z()-.68);
        check(map.roomAt(outside.x(),outside.y(),outside.z())==p.b()
                &&map.roomAt(inside.x(),inside.y(),inside.z())==p.a(),"Wrong foyer/forecourt ownership");
        for(double h:new double[]{HunterPosture.STANDING_HEIGHT,HunterPosture.CROUCH_HEIGHT}){
            check((walk(map,List.of(outside,inside),h)!=null)==open,"Ingress does not match door state");
            check((walk(map,List.of(inside,outside),h)!=null)==open,"Egress does not match door state");
        }
        for(double lateral:new double[]{-.50,0,.50}){
            double x=p.x()+lateral;
            for(double h:new double[]{.25,1.2,2.1})
                check(map.clearLineOfSight(x,p.y()+h,outside.z(),x,p.y()+h,inside.z())==open,
                        "Foyer optical leak or blockage at "+x+","+h);
        }
        check(map.clearObjectPath(outside.x(),1.05,outside.z(),inside.x(),1.05,inside.z(),.06,.06)==open,
                "Small object path ignores the leaf");
        check(map.clearInteractionSight(outside.x(),1.20,outside.z(),inside.x(),1.20,inside.z(),p.id()),
                "A user must be able to aim at the selected closed leaf");
        check(!map.clearLineOfSight(p.x()+1.1,1.2,outside.z(),p.x()+1.1,1.2,inside.z()),
                "Physical masonry beside the frame must stay solid");
        int local=map.architecturalSoundLoss(outside.x(),outside.y(),outside.z(),inside.x(),inside.y(),inside.z());
        check(local==(open?1:3),"Local acoustic state");
        int graph=LanAcoustics.physicalCostAt(map,p.b(),outside,p.a(),inside);
        check(graph<Integer.MAX_VALUE,"Acoustic portal not connected to actual rooms");
        return graph;
    }

    private static int[] capture(SceneRenderer renderer,HouseMap map,Path file)throws Exception {
        renderer.setRenderSize(780,520);
        renderer.setPsxPreset(PsxPipeline.Preset.OFF);
        renderer.lamp=true;
        renderer.setLampHardware(EquipmentTiers.Tier.III,false);
        renderer.x=22.5;renderer.z=38.8;renderer.eyeHeight=HunterPosture.STANDING_EYE;
        renderer.yaw=Math.PI;renderer.pitch=-.05;
        check(map.traversable(renderer.x,0,renderer.z),"Unsupported arrival camera");
        renderer.drawNetwork(map,List.of(),-1,0);
        check(renderer.frameDiagnostics().faces()>300&&renderer.frameDiagnostics().frustumFaces()>50,
                "The production renderer did not load the staged geometry");
        var frame=renderer.image();
        check(ImageIO.write(frame,"png",file.toFile()),"PNG encoder unavailable");
        return frame.getRGB(0,0,780,520,null,0,780);
    }

    public static void main(String[] args)throws Exception {
        var arrival=load(Path.of(args[0]),Path.of(args[1]));
        var host=arrival.host();
        var guest=arrival.guest();
        var door=host.nativeInvestigation().doors().get(0);
        var portal=door.spec().portal();
        Path out=Path.of(args[3]);
        Files.createDirectories(out);

        int closed=audit(host,false);
        guest.applyDoorSnapshot(host.doorSnapshot());
        check(audit(guest,false)==closed,"Guest closed acoustics");
        check(!host.airborneClear(22.5,.12,(door.spec().frameFrom()+door.spec().frameTo())/2,
                HunterPosture.STANDING_HEIGHT),"Closed leaf lost airborne collision");
        var renderer=new SceneRenderer();
        var closedFrame=capture(renderer,host,out.resolve("school-foyer-closed.png"));
        host.toggleDoor(portal.id());
        guest.applyDoorSnapshot(host.doorSnapshot());
        int open=audit(host,true);
        check(audit(guest,true)==open&&closed==open+4,"Host/guest or acoustic transition differs");
        var openFrame=capture(renderer,host,out.resolve("school-foyer-open.png"));

        // The open leaves occupy the sides of the receiving apron.
        double hinge=portal.width()/2;
        double openDepth=(door.spec().frameFrom()+door.spec().frameTo())/2+portal.width()/4;
        for(int sign:new int[]{-1,1}){
            double x=portal.x()+sign*hinge;
            check(!host.airborneClear(x,.04,openDepth,HunterPosture.STANDING_HEIGHT),
                    "Open leaf missing physical volume at its hinge side");
            check(!host.clearLineOfSight(x-.27,1.25,openDepth,x+.27,1.25,openDepth),
                    "Open leaf missing optical volume at its hinge side");
        }
        var van=host.van();
        var deck=van.world(0,VanStaging.FRONT+.45);
        var path=new ArrayList<Vec>();
        path.add(new Vec(deck.x(),van.floorY(),deck.z()));
        path.addAll(arrival.approach());
        check(walk(host,path,HunterPosture.STANDING_HEIGHT)!=null,"Open van-to-foyer arrival");
        Collections.reverse(path);
        check(walk(guest,path,HunterPosture.CROUCH_HEIGHT)!=null,"Open foyer-to-van return");
        host.toggleDoor(portal.id());
        guest.applyDoorSnapshot(host.doorSnapshot());
        check(audit(guest,false)==closed,"Guest reclose differs");
        check(walk(guest,path,HunterPosture.CROUCH_HEIGHT)==null,"Reclosed route still traverses");

        int pixels=0;
        for(int y=100;y<440;y++)for(int x=190;x<590;x++)
            if(closedFrame[y*780+x]!=openFrame[y*780+x])pixels++;
        check(pixels>2500,"The rendered foyer did not show the two physical poses");
        Files.write(Path.of(args[2]),List.of("check\tresult",
                "native_frame\t"+door.spec().frameId(),"portal_id\t"+portal.id(),
                "scene_revision\t"+host.nativeInvestigation().revision(),
                "leaf_triangles_per_pose\t"+door.closedPose().model().triangles().size(),
                "open_and_closed_host_guest\tPASS","ingress_egress_stand_crouch\tPASS",
                "closed_eye_and_object_rays\tPASS","open_swing_leaf_body_and_ray\tPASS",
                "selected_leaf_interaction\tPASS","existing_jamb_occlusion\tPASS",
                "paired_foyer_acoustics_open\t"+open,"paired_foyer_acoustics_closed\t"+closed,
                "van_to_foyer_and_return\tPASS","reclosed_return\tBLOCKED",
                "changed_foyer_pixels\t"+pixels));
        System.out.println("NATIVE SCHOOL FITTED FOYER PASS");
    }
}
