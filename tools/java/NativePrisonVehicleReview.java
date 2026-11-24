package org.beckenburg;

import java.nio.file.Path;
import java.util.List;
import org.beckenburg.LevelDefinition.Portal;
import org.beckenburg.LevelDefinition.Vec;
import org.beckenburg.NativeSchoolInteriorReview.Peer;

/** Checks the complete vehicle facade, manual controls and occupied-door handling. */
final class NativePrisonVehicleReview {
    private static final int GATE=457;
    private static final Vec SALLY=new Vec(34,0,66.9);
    private static final Vec COURT=new Vec(34,0,69);

    private NativePrisonVehicleReview(){}

    private static void require(boolean valid,String message){
        if(!valid)throw new AssertionError(message);
    }

    static void audit(HouseMap map,HouseMap guest,Path captures,List<String> report)throws Exception{
        var door=map.nativeInvestigation().doors().stream().filter(d->d.spec().portal().id()==GATE).findFirst().orElseThrow();
        var portal=door.spec().portal();
        var physics=map.nativeInvestigation().physics();
        require(portal.width()==3&&door.spec().clearHeight()==3.04&&door.sliding(),"Wrong vehicle bay profile");
        require(map.roomAt(SALLY.x(),0,SALLY.z())==2&&map.roomAt(COURT.x(),0,COURT.z())==13,"Wrong vehicle gate owners");
        int rays=0;
        for(double x:new double[]{26.12,27,29,32.15,32.43,35.57,36,39,41.88})
            for(double y:new double[]{.15,1.25,2.5,3.3,3.75}){
                require(!throughFacade(physics,x,y),"South facade leak at "+x+","+y);
                rays++;
            }
        for(double x:new double[]{32.62,33.4,34,34.6,35.38})
            for(double y:new double[]{3.09,3.48,3.81}){
                require(!throughFacade(physics,x,y),"South lintel leak at "+x+","+y);
                rays++;
            }
        for(double x:new double[]{26.35,28.5,34,39.5,41.65})
            for(double y:new double[]{3.83,3.87,3.95,4.03,4.18}){
                require(!throughFacade(physics,x,y),"Open ceiling or eave seat at "+x+","+y);
                rays++;
            }
        for(double x:new double[]{32.68,33.15,34,34.85,35.32})
            for(double z:new double[]{67.35,68.33,68.95,70,71.5,73})
                require(physics.traversable(new Vec(x,0,z),HunterPosture.STANDING_HEIGHT),"Unsupported existing apron "+x+","+z);
        for(double x=32.52;x<38.625;x+=.13)
            for(double y:new double[]{.14,1.28,2.97})
                require(physics.clearSight(new Vec(x,y,67.532),new Vec(x,y,67.668)),
                    "Static obstruction in the sliding panel's travel at "+x+","+y);
        require(!map.clearLineOfSight(34,1.78,66.9,34,1.78,69)
            &&map.clearLineOfSight(34.17,1.78,66.9,34.17,1.78,69),"Closed vehicle bars do not match their visible gaps");
        NativePrisonGateReview.checkCrossing(map,SALLY,COURT,false);
        aimBoth(map,"Open gate manually");
        capture(map,portal,captures.resolve("prison-vehicle-closed.png"),true);
        var empty=parkedView(map,captures.resolve("prison-vehicle-parking-empty.png"));
        map.toggleDoor(GATE);
        guest.applyDoorSnapshot(map.doorSnapshot());
        NativePrisonGateReview.checkCrossing(guest,SALLY,COURT,true);
        require(!door.openPose().physics().clearSight(new Vec(37.13,1.8,67.46),new Vec(37.13,1.8,67.72)),
            "Parked vehicle gate is missing from the solid pier receiver");
        for(double x:new double[]{-VanStaging.HALF_WIDTH,-.6,0,.6,VanStaging.HALF_WIDTH})
            for(double y:new double[]{.15,.55,1.2,1.95,VanStaging.ROOF_Y}){
                require(map.clearLineOfSight(34+x,y,66.9,34+x,y,69),"Open gap clips the reference vehicle envelope");
            }
        aimBoth(map,"Close gate manually");
        capture(map,portal,captures.resolve("prison-vehicle-open.png"),false);
        var parked=parkedView(map,captures.resolve("prison-vehicle-parked-detail.png"));
        int changed=0;
        for(int i=0;i<parked.length;i++)
            if(channelDifference(empty[i],parked[i],16)>=12||channelDifference(empty[i],parked[i],8)>=12
                ||channelDifference(empty[i],parked[i],0)>=12)changed++;
        require(changed>1000,"Visible parked leaf was culled at the east pier");
        report.add("vehicle_gate_parked_visibility\t"+changed+" changed pixels looking east with the closed threshold behind the operator");
        map.toggleDoor(GATE);
        guest.applyDoorSnapshot(map.doorSnapshot());
        NativePrisonGateReview.checkCrossing(guest,SALLY,COURT,false);
        report.add("south_facade_closure\t"+rays+" solid wall and header rays and 30 supported approach points");
        report.add("vehicle_gate_envelope\t3.00 m by 3.04 m clear opening, 25 canonical vehicle-envelope rays");
        report.add("vehicle_gate_manual\tBoth release handles aimed in both states; body, optics and parked collision verified");
    }

    private static boolean throughFacade(AuthoredSitePhysics physics,double x,double y){
        return physics.clearSight(new Vec(x,y,67.46),new Vec(x,y,68.33));
    }

    private static void aimBoth(HouseMap map,String action){
        NativePrisonGateReview.aim(map,"vehicle-release-forecourt",1,action);
        NativePrisonGateReview.aim(map,"vehicle-release-sally",-1,action);
    }

    private static void capture(HouseMap map,Portal portal,Path path,boolean outside)throws Exception{
        var renderer=new SceneRenderer();
        renderer.setRenderSize(1050,680);
        renderer.setPsxPreset(PsxPipeline.Preset.OFF);
        renderer.x=outside?34.4:35.5;
        renderer.z=outside?72.6:63.1;
        require(map.traversable(renderer.x,0,renderer.z),"Unsupported vehicle review camera");
        renderer.eyeHeight=HunterPosture.STANDING_EYE;
        renderer.yaw=Math.atan2(portal.x()-renderer.x,portal.z()-renderer.z);
        renderer.pitch=.09;
        renderer.lamp=true;
        renderer.setLampHardware(EquipmentTiers.Tier.III,false);
        NativeSchoolInteriorReview.drawFrame(renderer,map,path);
    }

    private static int channelDifference(int a,int b,int shift){
        return Math.abs((a>>shift&255)-(b>>shift&255));
    }

    private static int[] parkedView(HouseMap map,Path path)throws Exception{
        var renderer=new SceneRenderer();
        renderer.setRenderSize(960,640);
        renderer.setPsxPreset(PsxPipeline.Preset.OFF);
        renderer.x=37.8;
        renderer.z=67.13;
        require(map.traversable(renderer.x,0,renderer.z),"Unsupported standing view at the gate parking pier");
        renderer.eyeHeight=HunterPosture.STANDING_EYE;
        renderer.yaw=Math.PI/2;
        renderer.lamp=true;
        renderer.setLampHardware(EquipmentTiers.Tier.III,false);
        NativeSchoolInteriorReview.drawFrame(renderer,map,path);
        return renderer.image().getRGB(0,0,960,640,null,0,960);
    }

    static void live(LevelDefinition definition,NativeInvestigationScene scene,boolean packaged,List<String> report)
            throws Exception {
        var map=packaged?HouseMap.forNetworkRevision(HouseMap.Site.PRISON,definition.revision()):
            new HouseMap(HouseMap.Site.PRISON,definition,null,scene);
        var contract=CustomContract.standard(2);
        try(var host=packaged?new LanHost("127.0.0.1",0,GhostIdentity.Type.SPIRIT,contract,false,false,HouseMap.Site.PRISON):
                new LanHost("127.0.0.1",0,GhostIdentity.Type.SPIRIT,contract,new HouseMap(HouseMap.Site.PRISON,definition,null,scene));
            var operator=new Peer(host,false,"Operator");
            var witness=new Peer(host,false,"Witness");
            var ghost=new Peer(host,true,"Ghost")){
            for(var peer:List.of(operator,witness,ghost)){
                peer.awaitExact("SITE|PRISON|"+definition.revision());
                peer.client.ready();
            }
            operator.awaitExact("LOBBY|2|HUMAN|3|WAITING");
            operator.client.start();
            operator.await("INFO|mission started");
            for(var peer:List.of(operator,witness)){
                peer.client.move(13);
                peer.awaitExact("ACCEPT|MOVE|13");
            }
            ghost.client.viewGhost3D();
            ghost.awaitExact("ACCEPT|GVIEW3D");
            ghost.await("GPOSE|");
            NativeSchoolInteriorReview.ghostTrip(host,witness,ghost);
            operator.client.view3D();
            operator.awaitExact("ACCEPT|VIEW3D");
            operator.await("BODY|0|");
            walk(operator,0,6,-100,0);
            var feet=walk(operator,0,13,0,-100);
            interact(operator,map,feet,"vehicle-release-forecourt","Open gate manually",false);
            observe(witness,map,true);

            witness.client.view3D();
            witness.awaitExact("ACCEPT|VIEW3D");
            witness.await("BODY|1|");
            var inGate=walk(witness,1,17,0,-100);
            require(Math.abs(inGate.z()-67.60)<.18,"Witness missed vehicle leaf plane "+inGate);
            interact(operator,map,feet,"vehicle-release-forecourt","Close gate manually",true);
            walk(witness,1,4,0,100);

            walk(operator,0,6,100,0);
            walk(operator,0,7,0,-100);
            feet=walk(operator,0,6,-100,0);
            require(map.roomAt(feet.x(),feet.y(),feet.z())==2,"Operator failed to cross south vehicle gate");
            interact(operator,map,feet,"vehicle-release-sally","Close gate manually",false);
            observe(witness,map,false);
            Thread.sleep(350);
            interact(operator,map,feet,"vehicle-release-sally","Open gate manually",false);
            observe(witness,map,true);
            walk(operator,0,6,100,0);
            feet=walk(operator,0,7,0,100);
            require(map.roomAt(feet.x(),feet.y(),feet.z())==13,"Operator failed to return through vehicle gate");
            var lights=(RoomLighting)NativeSchoolInteriorReview.field(host,"lights");
            synchronized(NativeSchoolInteriorReview.field(host,"mutex")){
                require(!lights.supplied(),"Manual vehicle gate restored power");
            }
        }
        report.add("tcp_vehicle_gate_blackout\tBoth releases, two real crossings, stable blackout and occupied closure refusal");
    }

    private static Vec walk(Peer peer,int seat,int ticks,int dx,int dz)throws Exception{
        Vec at=null;
        for(int i=0;i<ticks;i++)at=NativeSchoolInteriorReview.step(peer,seat,dx,dz);
        return at;
    }

    private static void observe(Peer witness,HouseMap map,boolean open)throws Exception{
        var frame=witness.awaitExact(open?"DOORS|457":"DOORS|");
        map.applyDoorSnapshot(frame.substring("DOORS|".length()));
        require(map.doorOpen(GATE)==open&&!map.doorOpen(452),"Vehicle state changed the separate yard gate");
    }

    private static void interact(Peer operator,HouseMap map,Vec feet,String id,String action,boolean occupied)throws Exception{
        var release=map.level().sockets().stream().filter(s->s.id().equals(id)).findFirst().orElseThrow();
        double dx=release.x()-feet.x(),dz=release.z()-feet.z();
        double yaw=Math.atan2(dx,dz);
        double pitch=Math.atan2(release.y()-feet.y()-HunterPosture.STANDING_EYE,Math.hypot(dx,dz));
        var eye=new LooseClutter.Observer(0,feet.x(),feet.y(),feet.z(),yaw,pitch,true);
        var target=InteractionFocus.pick(map,eye,List.of());
        require(target!=null&&target.id().equals(Integer.toString(GATE))&&target.name().equals(action),
            "Physical focus failed at "+id+" from "+feet+" got "+target);
        operator.client.turn(Math.floorMod((int)Math.round(Math.toDegrees(yaw)),360));
        operator.client.interact(target,yaw,pitch);
        if(occupied)operator.awaitExact("REJECT|doorway occupied");
        else operator.awaitExact("ACCEPT|INTERACT|"+action);
    }
}
