package org.beckenburg;

import java.nio.file.Path;
import java.util.List;
import org.beckenburg.LevelDefinition.Socket;
import org.beckenburg.LevelDefinition.Vec;

/** Physically exercises the surveyed sliding leaf and the two independent release faces. */
final class NativePrisonGateReview {
    private static final int GATE=452;

    private NativePrisonGateReview(){}

    private static void require(boolean condition,String label){
        if(!condition)throw new AssertionError(label);
    }

    static void audit(HouseMap map,HouseMap guest,Path captures,List<String> report)throws Exception{
        var gate=map.nativeInvestigation().doors().stream().filter(door->door.sliding()).findFirst().orElseThrow();
        var portal=gate.spec().portal();
        var inside=new Vec(34,0,59.1);
        var outside=new Vec(34,0,57);
        require(map.roomAt(inside.x(),inside.y(),inside.z())==2&&map.roomAt(outside.x(),outside.y(),outside.z())==12,
            "Gate does not connect the sally port and exercise yard");
        require(gate.closedPose().model().triangles().size()==168,"Unexpected source geometry in the movable leaf");
        for(double y:new double[]{.2,1.30,2.51})
            for(double x=32.805;x<37.72;x+=.15)
                require(map.nativeInvestigation().physics().clearSight(new Vec(x,y,58.318),new Vec(x,y,58.446)),
                    "Sliding panel crosses static geometry at "+x+","+y);
        checkCrossing(map,inside,outside,false);
        require(!sight(map,34,1.45)&&sight(map,34.12,1.45),"Solid bars and transparent gaps must agree with the leaf");
        require(!object(map,34,1.45)&&object(map,34.12,1.45),"Thin objects must use the same gaps as sight");
        require(map.architecturalSoundLoss(34,0,57,34,0,59.1)==3
            &&map.architecturalSoundLoss(34.12,0,57,34.12,0,59.1)==1,
            "Closed security bars differ from a sealed acoustical wall");
        NativeSchoolInteriorReview.render(map,portal,captures.resolve("sally-gate-closed-yard.png"),-1);
        overview(map,captures.resolve("sally-mechanism-closed.png"));

        aim(map,"sally-release-yard",-1,"Open gate manually");
        aim(map,"sally-release-sally",1,"Open gate manually");
        map.toggleDoor(GATE);
        guest.applyDoorSnapshot(map.doorSnapshot());
        checkCrossing(map,inside,outside,true);
        checkCrossing(guest,inside,outside,true);
        require(sight(map,34,1.45)&&object(map,34,1.45),"Open gate still blocks the clear aperture");
        require(map.architecturalSoundLoss(34,0,57,34,0,59.1)==1,"Open gate did not clear the acoustic route");
        require(!gate.openPose().physics().clearSight(new Vec(36.53,1.45,58.30),new Vec(36.53,1.45,58.8)),
            "The opened panel lost its real parked position");
        aim(map,"sally-release-yard",-1,"Close gate manually");
        aim(map,"sally-release-sally",1,"Close gate manually");
        NativeSchoolInteriorReview.render(map,portal,captures.resolve("sally-gate-open-yard.png"),-1);
        overview(map,captures.resolve("sally-mechanism-open.png"));
        map.toggleDoor(GATE);
        guest.applyDoorSnapshot(map.doorSnapshot());
        checkCrossing(guest,inside,outside,false);
        report.add("sally_gate_collision\tBoth directions and postures agree with each replicated state");
        report.add("sally_gate_optics\tReal bars block sight and objects, with gaps between the bars");
        report.add("sally_gate_acoustics\tBar and gap paths differ; the open aperture clears the centre line");
        report.add("sally_gate_releases\tBoth manual levers are visible and reachable in both states");
    }

    private static void overview(HouseMap map,Path path)throws Exception{
        var renderer=new SceneRenderer();
        renderer.setRenderSize(960,640);
        renderer.setPsxPreset(PsxPipeline.Preset.OFF);
        renderer.x=35.4;
        renderer.z=62.8;
        require(map.traversable(renderer.x,0,renderer.z),"Unsupported sally port review position");
        renderer.eyeHeight=HunterPosture.STANDING_EYE;
        renderer.yaw=Math.PI;
        renderer.pitch=.06;
        renderer.lamp=true;
        renderer.setLampHardware(EquipmentTiers.Tier.III,false);
        NativeSchoolInteriorReview.drawFrame(renderer,map,path);
    }

    static void liveRelease(HouseMap map,List<String> report)throws Exception{
        try(var host=new LanHost("127.0.0.1",0,GhostIdentity.Type.SPIRIT,CustomContract.standard(3),map);
            var operator=new NativeSchoolInteriorReview.Peer(host,false,"Operator");
            var witness=new NativeSchoolInteriorReview.Peer(host,false,"Witness");
            var scout=new NativeSchoolInteriorReview.Peer(host,false,"Scout");
            var ghost=new NativeSchoolInteriorReview.Peer(host,true,"Ghost")){
            String selection="SITE|PRISON|"+map.level().revision();
            for(var peer:List.of(operator,witness,scout,ghost)){
                require(peer.await("SITE|").equals(selection),"Peer received the wrong Prison revision");
                peer.client.ready();
            }
            operator.await("LOBBY|3|HUMAN|4|WAITING");
            operator.client.start();
            operator.await("INFO|mission started");
            for(int owner:NativeSchoolInteriorReview.missionRoute(map,2)){
                operator.client.move(owner);
                require(operator.await("ACCEPT|MOVE|").equals("ACCEPT|MOVE|"+owner),"Wrong sally approach owner");
            }
            // Inject loss of source at the host for this scope. The Prison plant
            // breaker and circuits are still a separate construction pass.
            var lights=(RoomLighting)NativeSchoolInteriorReview.field(host,"lights");
            synchronized(NativeSchoolInteriorReview.field(host,"mutex")){lights.setBreaker(false);}
            operator.client.view3D();
            operator.await("ACCEPT|VIEW3D");
            var feet=NativeSchoolInteriorReview.body(operator.await("BODY|0|"));
            require(map.roomAt(feet.x(),feet.y(),feet.z())==2,"Wrong physical operator start");
            engage(operator,map,feet,"sally-release-sally","Open gate manually");
            require(witness.await("DOORS|452").equals("DOORS|452"),"Witness missed the gate opening");
            steps(operator,5,100,0);
            steps(operator,7,0,-100);
            feet=steps(operator,5,-100,0);
            require(map.roomAt(feet.x(),feet.y(),feet.z())==12,"TCP mover did not reach the yard");
            engage(operator,map,feet,"sally-release-yard","Close gate manually");
            Thread.sleep(350);
            engage(operator,map,feet,"sally-release-yard","Open gate manually");
            steps(operator,5,100,0);
            feet=steps(operator,7,0,100);
            require(map.roomAt(feet.x(),feet.y(),feet.z())==2,"TCP mover did not return from the exercise yard");
            synchronized(NativeSchoolInteriorReview.field(host,"mutex")){
                require(!lights.supplied()&&map.doorOpen(GATE),"Manual gate changed the host power state");
            }
        }
        report.add("staged_tcp_blackout_release\tLive host began with an injected breaker outage. A hunter opened from sally, closed and reopened from the yard, and walked the actual threshold twice.");
    }

    private static Vec steps(NativeSchoolInteriorReview.Peer operator,int count,int dx,int dz)throws Exception{
        Vec at=null;
        for(int step=0;step<count;step++)at=NativeSchoolInteriorReview.step(operator,0,dx,dz);
        return at;
    }

    private static void engage(NativeSchoolInteriorReview.Peer operator,HouseMap map,Vec feet,String id,String action)
            throws Exception{
        var release=map.level().sockets().stream().filter(s->s.id().equals(id)).findFirst().orElseThrow();
        double dx=release.x()-feet.x(),dz=release.z()-feet.z();
        double yaw=Math.atan2(dx,dz),pitch=Math.atan2(release.y()-feet.y()-HunterPosture.STANDING_EYE,Math.hypot(dx,dz));
        var observer=new LooseClutter.Observer(0,feet.x(),feet.y(),feet.z(),yaw,pitch,true);
        var target=InteractionFocus.pick(map,observer,List.of());
        require(target!=null&&target.id().equals(Integer.toString(GATE))&&target.name().equals(action),
            "TCP actor could not aim at "+id+" from "+feet+" got "+target);
        operator.client.turn(Math.floorMod((int)Math.round(Math.toDegrees(yaw)),360));
        operator.client.interact(target,yaw,pitch);
        operator.await("ACCEPT|INTERACT|"+action);
    }

    private static void checkCrossing(HouseMap map,Vec a,Vec b,boolean open){
        for(double h:new double[]{HunterPosture.STANDING_HEIGHT,HunterPosture.CROUCH_HEIGHT})
            for(var from:List.of(a,b)){
                var to=from.equals(a)?b:a;
                for(double lane:new double[]{-.55,0,.55}){
                    var at=new Vec(from.x()+lane,from.y(),from.z());
                    boolean passes=true;
                    for(int step=1;step<=20;step++){
                        var next=map.resolveMove(at,at.x(),from.z()+(to.z()-from.z())*step/20,h);
                        if(next==null){passes=false;break;}
                        at=next;
                    }
                    require(passes==open,"Sally crossing mismatch "+from+" lane "+lane+" open "+open);
                }
            }
    }

    private static boolean sight(HouseMap map,double x,double y){
        return map.clearLineOfSight(x,y,57,x,y,59.1);
    }

    private static boolean object(HouseMap map,double x,double y){
        return map.clearObjectPath(x,y,57,x,y,59.1,.024,.024);
    }

    static InteractionFocus.Target aim(HouseMap map,String id,int side,String name){
        Socket release=map.level().sockets().stream().filter(s->s.id().equals(id)).findFirst().orElseThrow();
        var feet=new Vec(release.x(),0,release.z()+side*.80);
        require(map.roomAt(feet.x(),feet.y(),feet.z())==release.room()
            &&map.traversable(feet.x(),feet.y(),feet.z()),"Unreachable gate release stance "+id);
        double yaw=side>0?Math.PI:0;
        double pitch=Math.atan2(release.y()-HunterPosture.STANDING_EYE,.80);
        var eye=new LooseClutter.Observer(0,feet.x(),feet.y(),feet.z(),yaw,pitch,true);
        var target=InteractionFocus.pick(map,eye,List.of());
        require(target!=null&&target.kind()==InteractionFocus.Kind.DOOR&&target.id().equals("452")
            &&target.name().equals(name),"Wrong gate aim at "+id+" "+target);
        return target;
    }
}
