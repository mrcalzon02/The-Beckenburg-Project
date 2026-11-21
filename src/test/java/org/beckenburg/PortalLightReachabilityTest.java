package org.beckenburg;

import java.util.ArrayList;
import java.util.List;

/**
 * Production light-path differential against a full direct-ray oracle.
 * The portal broadphase must never change authored illumination, even across
 * door toggles, circuit changes, elevation or near-wall receiver positions.
 * One rejection count measures candidate work avoided, NOT GPU milliseconds.
 */
public final class PortalLightReachabilityTest {
    private static void ok(boolean yes,String why){
        if(!yes)throw new AssertionError(why);
    }
    private static void same(FixtureLighting a,FixtureLighting b,int room,
            double x,double y,double z,String label){
        var left=a.sample(room,x,y,z);
        var right=b.sample(room,x,y,z);
        ok(Double.doubleToLongBits(left.r())==Double.doubleToLongBits(right.r())
            &&Double.doubleToLongBits(left.g())==Double.doubleToLongBits(right.g())
            &&Double.doubleToLongBits(left.b())==Double.doubleToLongBits(right.b()),
            label+" room="+room+" at "+x+","+y+","+z+" direct="+left+" portal="+right);
        var base=a.sampleNormal(room,x,y,z,0,1,0);
        var fast=b.sampleNormal(room,x,y,z,0,1,0);
        ok(base.equals(fast),label+" normal light changed at "+room);
    }
    private static FixtureLighting make(HouseMap map,boolean disabled){
        String prior=System.getProperty("beckenburg.lighting.portalGate.disabled");
        String oldFlux=System.getProperty("beckenburg.lighting.portalFlux.disabled");
        try{
            System.setProperty("beckenburg.lighting.portalFlux.disabled","true");
            if(disabled)System.setProperty("beckenburg.lighting.portalGate.disabled","true");
            else System.clearProperty("beckenburg.lighting.portalGate.disabled");
            return new FixtureLighting(map);
        }finally{
            if(prior==null)System.clearProperty("beckenburg.lighting.portalGate.disabled");
            else System.setProperty("beckenburg.lighting.portalGate.disabled",prior);
            if(oldFlux==null)System.clearProperty("beckenburg.lighting.portalFlux.disabled");
            else System.setProperty("beckenburg.lighting.portalFlux.disabled",oldFlux);
        }
    }
    private static List<LevelDefinition.Vec> receivers(HouseMap map,int room){
        var r=map.level().room(room);
        double cx=(r.x0()+r.x1())*.5,cz=(r.z0()+r.z1())*.5;
        double y=r.y()+Math.min(1.52,r.height()*.5);
        var points=new ArrayList<LevelDefinition.Vec>();
        for(double xx:new double[]{cx,r.x0()+.32,r.x1()-.32})
            for(double zz:new double[]{cz,r.z0()+.32,r.z1()-.32})
                points.add(new LevelDefinition.Vec(xx,y,zz));
        return points;
    }
    private static void physicalFirstOpeningGate(){
        var map=new HouseMap(HouseMap.Site.HOUSE);
        var door=AuthoredTestSupport.door(map,3,4);
        var at=AuthoredTestSupport.side(map,door,3,2.0);
        var receiving=AuthoredTestSupport.side(map,door,4,.80);
        double sy=door.y()+1.35;
        var isolated=new FixtureLighting.Source(0,FixtureLighting.Kind.BARE_BULB,
            3,0,at.x(),sy,at.z(),0,-1,0,1.05,3,1,.8,.6);
        var facing=new FixtureLighting.Source(1,FixtureLighting.Kind.TELEVISION,
            3,0,at.x(),sy,at.z(),0,0,1,2.25,3,1,1,1);
        var facingAway=new FixtureLighting.Source(2,FixtureLighting.Kind.TELEVISION,
            3,0,at.x(),sy,at.z(),0,0,-1,2.25,3,1,1,1);
        var diffuse=new FixtureLighting.Source(3,FixtureLighting.Kind.FROSTED_DOME,
            3,0,at.x(),sy,at.z(),0,0,-1,2.25,3,1,1,1);
        var sources=List.of(isolated,facing,facingAway,diffuse);
        var gate=new PortalLightReachability(map,sources);
        ok(gate.sourceConfined()==4,
            "Closed-door local lights should not reach unrelated rooms");
        map.toggleDoor(door.id());
        gate.refresh();
        ok(!gate.disconnected(facing,4,receiving.x(),sy,receiving.z()),
            "A forward narrow beam touching the real open doorway was culled");
        ok(!gate.disconnected(diffuse,4,receiving.x(),sy,receiving.z()),
            "A diffuse bulb has a nonzero back emission; do not hard-cull it");
        ok(gate.disconnected(isolated,4,receiving.x(),sy,receiving.z()),
            "A light whose radius ends before every opening cast into another room");
        ok(gate.disconnected(facingAway,4,receiving.x(),sy,receiving.z()),
            "Narrow light aimed away from every opening was sent cross-room");
        ok(gate.sourceConfined()==2,
            "The open doorway should release only the two physically reaching emitters");
        int prior=gate.rebuilds();
        gate.refresh();
        ok(gate.rebuilds()==prior,
            "A static lamp recomputed its aperture/cone on an unchanged frame");
        map.toggleDoor(door.id());
        gate.refresh();
        ok(gate.disconnected(facing,4,receiving.x(),sy,receiving.z()),
            "Closing the real door failed to isolate the forward beam");
        // Ambiguous wall receivers retain exact full ray handling.
        ok(!gate.disconnected(isolated,4,door.x(),sy,door.z()),
            "Wall/door boundary probe was prematurely culled");
        System.out.println("PASS light-locality aperture: radius stops, hard beam cone, "
            +"diffuse backlight, door toggles, boundary fallback and cached frames");
    }
    /** Connected rooms are not automatically illuminated. The same source
     * must physically reach *each* successive aperture on a two-door route. */
    private static void physicalSecondOpeningGate(){
        var map=new HouseMap(HouseMap.Site.HOUSE);
        var first=AuthoredTestSupport.door(map,3,4);
        var second=AuthoredTestSupport.door(map,4,0);
        var local=AuthoredTestSupport.side(map,first,3,1.05);
        double sy=first.y()+1.35;
        double firstDistance=Math.sqrt(Math.pow(local.x()-first.x(),2)
            +Math.pow(local.z()-first.z(),2));
        double nextDistance=Math.sqrt(Math.pow(local.x()-second.x(),2)
            +Math.pow(local.z()-second.z(),2)
            +Math.pow(sy-(second.y()+1.35),2));
        ok(nextDistance>firstDistance+2.0,
            "Two-door test must have genuinely distinct architectural openings");
        double radius=Math.min(firstDistance+1.35,(firstDistance+nextDistance)*.5);
        var light=new FixtureLighting.Source(0,FixtureLighting.Kind.BARE_BULB,
            3,0,local.x(),sy,local.z(),0,-1,0,radius,3,1,.8,.6);
        if(!map.doorOpen(first.id()))map.toggleDoor(first.id());
        if(!map.doorOpen(second.id()))map.toggleDoor(second.id());
        var gate=new PortalLightReachability(map,List.of(light));
        ok(gate.connected(3,0),
            "Open authored door chain must be connected independent of lighting");
        ok(gate.sourceCanReach(light,4),
            "Light that touches first open door must enter its receiving room");
        ok(!gate.sourceCanReach(light,0),
            "Light failed to stop at second door outside its physical sphere");
        ok(gate.sourceBlockedConnectedRooms()>0,
            "Source-specific mask should prune rooms admitted by global topology");
        var point=AuthoredTestSupport.side(map,second,0,1);
        ok(gate.disconnected(light,0,point.x(),second.y()+1.35,point.z()),
            "Second-room ray was still considered beyond the untouched door");
        int built=gate.rebuilds();
        gate.refresh();
        ok(gate.rebuilds()==built,
            "Per-source multi-door masks rebuilt on unchanged simulation frame");
        map.toggleDoor(first.id());
        gate.refresh();
        ok(!gate.sourceCanReach(light,4)&&!gate.sourceCanReach(light,0),
            "Closing the first door must isolate both downstream rooms");
        System.out.println("PASS per-source second-door radius stop, real room "
            +"connectivity, shadow candidate prune and revision-only rebuild");
    }
    public static void main(String[] args){
        physicalFirstOpeningGate();
        physicalSecondOpeningGate();
        long totalRejected=0,totalConfined=0,totalBlocked=0;
        for(var site:new HouseMap.Site[]{HouseMap.Site.HOUSE,
                    HouseMap.Site.FARMHOUSE,HouseMap.Site.SCHOOL}){
            var map=new HouseMap(site);
            var circuits=new RoomLighting();
            var oracle=make(map,true);
            var candidate=make(map,false);
            ok(oracle.portalGraphRebuilds()==0&&candidate.portalGraphRebuilds()==1,
                "Portal graph must be created once on the enabled path");
            var doors=map.level().portals().stream().filter(LevelDefinition.Portal::door)
                    .limit(2).toList();
            int expectedRebuilds=1;
            for(int phase=0;phase<4;phase++){
                if(phase==1&&!doors.isEmpty()){map.toggleDoor(doors.get(0).id());expectedRebuilds++;}
                if(phase==2)circuits.toggle(phase);
                if(phase==3&&doors.size()>1){map.toggleDoor(doors.get(1).id());expectedRebuilds++;}
                oracle.frame(circuits,phase);
                candidate.frame(circuits,phase);
                ok(candidate.portalGraphRebuilds()==expectedRebuilds,
                    "Connectivity rebuilt without a door/window change "+site+" phase "+phase);
                for(int room=0;room<map.interiorRoomCount();room++)
                    for(var pt:receivers(map,room))
                        same(oracle,candidate,room,pt.x(),pt.y(),pt.z(),
                            site+" phase "+phase);
                totalRejected+=candidate.portalCandidatesRejected();
            }
            ok(oracle.portalCandidatesRejected()==0,
                "Opt-out must preserve all exact direct-ray candidates");
            // Re-test the fully open authored graph as a differential oracle.
            // Long routes with real stairs, windows and external zones are
            // substantially harder than the first two door toggles.
            for(var door:map.level().portals())
                if(door.door()&&!map.doorOpen(door.id()))map.toggleDoor(door.id());
            oracle.frame(circuits,8);
            candidate.frame(circuits,8);
            for(int room=0;room<map.interiorRoomCount();room++)
                for(var pt:receivers(map,room))
                    same(oracle,candidate,room,pt.x(),pt.y(),pt.z(),
                        site+" all-open");
            totalRejected+=candidate.portalCandidatesRejected();
            var finalGate=new PortalLightReachability(map,candidate.sources());
            totalConfined+=finalGate.sourceConfined();
            totalBlocked+=finalGate.sourceBlockedConnectedRooms();
            System.out.println("PASS "+site
                +" cached every-door source reach and direct-ray RGB parity"
                +" source_blocked_open_room_pairs="
                +finalGate.sourceBlockedConnectedRooms());
        }
        System.out.println("PASS portal-gated lighting cross-map RGB/normal parity, "
            +"door/circuit locality; rejected candidate samples="+totalRejected
            +" safe local-only authored sources="+totalConfined
            +" connected-but-out-of-reach source-room pairs="+totalBlocked);
    }
}
