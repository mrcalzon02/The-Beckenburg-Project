package org.beckenburg;

import java.util.*;

/** Prepared attenuation must preserve real fixture illumination and controls. */
public final class PreparedVertexLightingTest {
    private static void close(FixtureLighting.RGB a,FixtureLighting.RGB b,String label){
        if(Math.abs(a.r()-b.r())>1e-9||Math.abs(a.g()-b.g())>1e-9||Math.abs(a.b()-b.b())>1e-9)
            throw new AssertionError(label+" "+a+" != "+b);
    }
    private static void compare(FixtureLighting lighting,int room,double x,double y,double z){
        var scalar=lighting.prepareVertex(room,MaterialAtlas.STONE,x,y,z,0,0,0,false);
        close(lighting.sample(room,x,y,z),scalar.sample(),"Prepared scalar at "+x+","+y+","+z);
        var normal=lighting.prepareVertex(room,MaterialAtlas.STONE,x,y,z,0,1,0,true);
        close(lighting.sampleNormal(room,x,y,z,0,1,0),normal.sample(),"Prepared authored normal");
    }
    public static void main(String[] args){
        var castle=new CastleHubMap();var lighting=new FixtureLighting(castle);
        lighting.castleFrame(true,25);
        var points=new ArrayList<double[]>();
        for(var room:CastleRooms.ALL)points.add(new double[]{(room.x0()+room.x1())/2,room.floor()+.8,(room.z0()+room.z1())/2});
        for(var p:points)compare(lighting,0,p[0],p[1],p[2]);
        var p=points.get(0);var probe=lighting.prepareVertex(0,MaterialAtlas.STONE,p[0],p[1],p[2],0,0,0,false);
        for(int tick:new int[]{26,31,70,120}){lighting.castleFrame(true,tick);close(lighting.sample(0,p[0],p[1],p[2]),probe.sample(),"Flicker factor remains live");}
        lighting.castleFrame(false,120);close(lighting.sample(0,p[0],p[1],p[2]),probe.sample(),"Blackout remains live");
        long revision=lighting.regionVersion(1,0,1,4,3,4);
        castle.toggleDoor(CastleDoorSlots.all().stream().filter(s->s.gate()).findFirst().orElseThrow().id());
        lighting.castleFrame(false,120);
        if(revision==lighting.regionVersion(1,0,1,4,3,4))throw new AssertionError("Castle door must invalidate stationary occlusion even in blackout");
        for(var site:List.of(HouseMap.Site.HOUSE,HouseMap.Site.SCHOOL)){
            var map=new HouseMap(site);var circuits=new RoomLighting();var field=new FixtureLighting(map);field.frame(circuits,0);
            for(var room:map.level().rooms())compare(field,room.id(),room.cx(),room.y()+.8,room.cz());
            var spawn=map.spawn(0);int room=map.roomAt(spawn.x(),spawn.y(),spawn.z());
            var retained=field.prepareVertex(room,MaterialAtlas.STONE,spawn.x(),spawn.y()+.5,spawn.z(),0,0,0,false);
            circuits.setBreaker(false);field.frame(circuits,1);
            close(field.sample(room,spawn.x(),spawn.y()+.5,spawn.z()),retained.sample(),"Real breaker factors remain live");
        }
        System.out.println("PASS prepared fixture scalar/normal probes, flicker, blackout, real breaker and Castle occluder revisions");
    }
}
