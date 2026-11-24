package org.beckenburg;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.beckenburg.LevelDefinition.Socket;
import org.beckenburg.LevelDefinition.Vec;
import org.beckenburg.NativeSchoolInteriorReview.Peer;

/** Test fitted switches, fixture ownership and shared power in both staged grounds. */
final class CemeteryDistributionReview {
    private CemeteryDistributionReview() {}

    static void audit(HouseMap map,NativeInvestigationScene scene,Map<Integer,Vec> feet,
                      Path captures,List<String> report,boolean annex,boolean gpu)throws Exception {
        var level=map.level();
        var switches=level.sockets().stream().filter(s->s.type().equals("switch")).toList();
        var loads=level.sockets().stream().filter(s->s.type().equals("light")&&s.room()<12).toList();
        require(switches.size()==(annex?11:7)&&switches.size()==feet.size()&&loads.size()==(annex?15:7),
            "Incorrect Cemetery distribution socket counts");
        var lights=new RoomLighting();
        var fixtures=new FixtureLighting(map);
        double[] mainStopped=unsupplied(fixtures,lights,loads,false);
        double[] mainTripped=unsupplied(fixtures,lights,loads,true);
        for(var control:switches) {
            int owner=control.room();
            var at=feet.get(owner);
            require(at!=null&&map.roomAt(at.x(),at.y(),at.z())==owner,"Wrong switch stance "+control.id());
            var path=approach(map,owner,at);
            for(double height:new double[]{HunterPosture.STANDING_HEIGHT,HunterPosture.CROUCH_HEIGHT}) {
                NativeSchoolInteriorReview.walk(map,path,height,control.id()+" switch approach");
            }
            target(map,at,control);
            var branch=loads.stream().filter(s->s.room()==owner).toList();
            require(!branch.isEmpty(),"Empty switched branch "+owner);
            var supplied=energies(fixtures,lights,branch);
            require(!lights.toggle(owner),"Could not switch off branch "+owner);
            var switchedOff=energies(fixtures,lights,branch);
            for(int other=0;other<HouseMap.INDOOR_ROOMS;other++) {
                require(lights.on(other)==(other!=owner),"Another switch changed state "+other);
            }
            require(lights.toggle(owner),"Could not restore branch "+owner);
            var restored=energies(fixtures,lights,branch);
            for(int i=0;i<branch.size();i++) {
                var load=branch.get(i);
                int index=loads.indexOf(load);
                double on=supplied[i],off=switchedOff[i];
                require(on>off*1.35&&Math.abs(on-restored[i])<.001,
                    "Local switch response failed for "+load.id()+" "+on+" / "+off+" / "+restored[i]);
                require(on>mainStopped[index]*1.35&&Math.abs(mainStopped[index]-mainTripped[index])<.001,
                    "Shared supply cutoffs disagree for "+load.id());
                report.add("fixture_"+load.id()+"\t"+on+" switched on / "+off+" switched off / "+
                    mainStopped[index]+" source stopped and breaker tripped");
            }
        }
        capture(map,level.socket("light",0),map.spawn(0),captures,report);
        int secondary=annex?9:6;
        capture(map,level.socket("light",secondary),map.spawn(secondary),captures,report);
        if(!gpu)live(map.site(),level,scene,feet,report,annex);
    }

    private static List<Vec> approach(HouseMap map,int owner,Vec target) {
        var path=new ArrayList<Vec>();
        path.add(map.spawn(owner));
        switch(owner) {
            case 8 -> path.addAll(List.of(new Vec(60,0,59),new Vec(58,0,59)));
            case 10 -> path.addAll(List.of(new Vec(69,0,65),new Vec(72,0,65),
                new Vec(74,0,65),new Vec(74,0,60.45)));
            default -> {}
        }
        path.add(target);
        return path;
    }

    private static double[] energies(FixtureLighting fixtures,RoomLighting lights,List<Socket> sockets) {
        fixtures.frame(lights,0);
        return sockets.stream().mapToDouble(s->fixtures.sample(s.room(),s.x(),s.y()-.50,s.z()).energy()).toArray();
    }

    private static double[] unsupplied(FixtureLighting fixtures,RoomLighting lights,List<Socket> sockets,
                                       boolean tripped) {
        lights.setBreaker(!tripped);
        lights.setSourceRunning(tripped);
        var measured=energies(fixtures,lights,sockets);
        lights.setBreaker(true);
        lights.setSourceRunning(true);
        return measured;
    }

    private static InteractionFocus.Target target(HouseMap map,Vec at,Socket control) {
        var aim=WharfPowerChecks.focus(map,at,control);
        require(aim.kind()==InteractionFocus.Kind.SWITCH&&aim.id().equals(Integer.toString(control.room())),
            "Wrong switch focus "+control.id()+" / "+aim);
        return aim;
    }

    private static void capture(HouseMap map,Socket load,Vec position,Path captures,List<String> report)throws Exception {
        var lights=new RoomLighting();
        var renderer=new SceneRenderer();
        renderer.setRenderSize(880,580);
        renderer.setPsxPreset(PsxPipeline.Preset.OFF);
        renderer.roomLights=lights;
        renderer.x=position.x();
        renderer.z=position.z();
        renderer.eyeHeight=position.y()+HunterPosture.STANDING_EYE;
        renderer.yaw=Math.atan2(load.x()-position.x(),load.z()-position.z());
        renderer.pitch=.10;
        var supplied=captures.resolve(load.id()+"-on.png");
        var stopped=captures.resolve(load.id()+"-off.png");
        NativeSchoolInteriorReview.drawFrame(renderer,map,supplied);
        lights.toggle(load.room());
        NativeSchoolInteriorReview.drawFrame(renderer,map,stopped);
        var on=ImageIO.read(supplied.toFile());
        var off=ImageIO.read(stopped.toFile());
        int changed=0;
        long gained=0;
        for(int y=0;y<on.getHeight();y++)for(int x=0;x<on.getWidth();x++) {
            int a=on.getRGB(x,y),b=off.getRGB(x,y);
            if(a!=b)changed++;
            for(int shift=0;shift<24;shift+=8)gained+=((a>>shift)&255)-((b>>shift)&255);
        }
        double mean=gained/(3.0*on.getWidth()*on.getHeight());
        require(changed>25_000&&mean>.25,"No visible circuit response for "+load.id()+" "+changed+" / "+mean);
        report.add("standing_"+load.id()+"\t"+changed+" changed pixels, "+mean+" mean RGB increase");
    }

    private static void live(HouseMap.Site site,LevelDefinition level,NativeInvestigationScene scene,Map<Integer,Vec> feet,
                             List<String> report,boolean annex)throws Exception {
        var map=site==HouseMap.Site.USER?new HouseMap(site,level,null,scene):HouseMap.forNetworkRevision(site,level.revision());
        try(var host=new LanHost("127.0.0.1",0,GhostIdentity.Type.SPIRIT,CustomContract.standard(3),map);
            var cottage=new Peer(host,false,"Cottage electrician");
            var archive=new Peer(host,false,"Archive electrician");
            var third=new Peer(host,false,"Cemetery witness");
            var ghost=new Peer(host,true,"Cemetery ghost")) {
            var crew=List.of(cottage,archive,third);
            for(var peer:List.of(cottage,archive,third,ghost)) {
                peer.awaitExact("SITE|"+map.site()+"|"+level.revision());
                peer.client.ready();
            }
            cottage.awaitExact("LOBBY|3|HUMAN|4|WAITING");
            cottage.client.start();
            cottage.await("INFO|mission started");
            char[] mask="111111111111".toCharArray();
            int[] owners=annex?new int[]{1,6,11}:new int[]{1,6};
            var positions=new ArrayList<Vec>();
            for(int seat=0;seat<owners.length;seat++) {
                var peer=crew.get(seat);
                int owner=owners[seat];
                for(int zone:NativeSchoolInteriorReview.missionRoute(map,owner)) {
                    peer.client.move(zone);
                    peer.awaitExact("ACCEPT|MOVE|"+zone);
                }
                peer.client.view3D();
                peer.awaitExact("ACCEPT|VIEW3D");
                var at=NativeSchoolInteriorReview.body(peer.await("BODY|"+seat+"|"));
                for(var waypoint:approach(map,owner,feet.get(owner))) {
                    at=CemeteryRefugeReview.walk(peer,seat,at,waypoint);
                }
                positions.add(at);
                use(map,peer,at,level.socket("switch",owner));
                mask[owner]='0';
                ghost.awaitExact("LIGHTS|"+new String(mask)+"|1");
            }
            int refreshTick=stateTick(ghost)+3;
            while(stateTick(ghost)<refreshTick) {
                // The host rejects a second use by the same actor before three ticks.
            }
            for(int seat=owners.length-1;seat>=0;seat--) {
                int owner=owners[seat];
                use(map,crew.get(seat),positions.get(seat),level.socket("switch",owner));
                mask[owner]='1';
                ghost.awaitExact("LIGHTS|"+new String(mask)+"|1");
            }
        }
        report.add("four_peer_independent_circuits\tPASS aimed cottage and archive toggles with independent restoration"+
            (annex?", also cold store and preparation":""));
    }

    private static int stateTick(Peer peer)throws Exception {
        return Integer.parseInt(peer.await("STATE|").split("\\|",3)[1]);
    }

    private static void use(HouseMap map,Peer peer,Vec at,Socket control)throws Exception {
        var aim=target(map,at,control);
        double dx=control.x()-at.x(),dz=control.z()-at.z();
        double yaw=Math.atan2(dx,dz);
        double pitch=Math.atan2(control.y()-at.y()-HunterPosture.STANDING_EYE,Math.hypot(dx,dz));
        peer.client.turn(Math.floorMod((int)Math.round(Math.toDegrees(yaw)),360));
        peer.client.interact(aim,yaw,pitch);
        peer.awaitExact("ACCEPT|INTERACT|"+aim.name());
    }

    private static void require(boolean pass,String message) {
        if(!pass)throw new AssertionError(message);
    }
}
