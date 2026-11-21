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
    public static void main(String[] args){
        long totalRejected=0;
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
            System.out.println("PASS "+site+" cached door graph and direct-ray RGB parity");
        }
        System.out.println("PASS portal-gated lighting cross-map RGB/normal parity, "
            +"door/circuit locality; rejected candidate samples="+totalRejected);
    }
}
