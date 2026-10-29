package org.beckenburg;

import java.util.*;

/** Exact production-submission parity and array ownership; recording, not GPU rasterization. */
public final class PreparedLightBatchParityTest {
    private static final class Recording implements GpuWorldRenderer {
        Frame frame;Result result;
        public boolean ready(){return true;}
        public String rendererDescription(){return "prepared-light submission recorder; no GPU";}
        public void submit(Frame f){frame=f;result=new Result(f.sceneId(),f.frameId(),f.width(),f.height(),new int[f.width()*f.height()],new float[f.width()*f.height()]);}
        public Result latest(){return result;}
    }
    private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
    private static GpuWorldRenderer.Frame draw(Recording backend,SceneRenderer r,HouseMap map,CastleHubMap castle,double clock,boolean on){
        r.roomLightTick=(int)(clock*10);r.roomLights.setBreaker(on);
        if(castle!=null)r.drawCastle(castle,clock,on);else r.drawNetwork(map,List.of(),-1,clock);
        return backend.frame;
    }
    private static void compare(GpuWorldRenderer.Frame a,GpuWorldRenderer.Frame b){
        check(a.surfaces().size()==b.surfaces().size(),"Complete material batch count");
        for(int i=0;i<a.surfaces().size();i++){
            var x=a.surfaces().get(i);var y=b.surfaces().get(i);
            check(x.material()==y.material()&&x.vertexCount()==y.vertexCount(),"Material and geometry ordering");
            check(Arrays.equals(x.vertices(),y.vertices()),"All production vertex floats match dense relighting at batch "+i);
        }
    }
    public static void main(String[] args){
        var backend=new Recording();GpuWorldRenderer.install(backend);
        String flag="beckenburg.lighting.changedOnly.disabled",previous=System.getProperty(flag);
        SceneRenderer fast,dense;
        try{System.clearProperty(flag);fast=new SceneRenderer();System.setProperty(flag,"true");dense=new SceneRenderer();}
        finally{if(previous==null)System.clearProperty(flag);else System.setProperty(flag,previous);}
        fast.roomLights=new RoomLighting();dense.roomLights=new RoomLighting();fast.lamp=dense.lamp=false;
        for(var site:Arrays.asList(null,HouseMap.Site.HOUSE,HouseMap.Site.FARMHOUSE,HouseMap.Site.SCHOOL)){
            var castle=site==null?new CastleHubMap():null;var map=site==null?null:new HouseMap(site);
            if(map==null){fast.x=dense.x=22;fast.z=dense.z=57;fast.eyeHeight=dense.eyeHeight=24;fast.yaw=dense.yaw=Math.PI;fast.pitch=dense.pitch=-.5;}
            else{var p=map.spawn(0);fast.x=dense.x=p.x();fast.z=dense.z=p.z();fast.eyeHeight=dense.eyeHeight=p.y()+1.62;fast.yaw=dense.yaw=Math.PI/3;fast.pitch=dense.pitch=-.1;}
            var first=draw(backend,fast,map,castle,2.5,true);
            compare(first,draw(backend,dense,map,castle,2.5,true));
            var saved=new IdentityHashMap<float[],float[]>();for(var s:first.surfaces())saved.put(s.vertices(),s.vertices().clone());
            for(double clock:new double[]{2.6,3.1,7.0,12.0}){
                boolean on=clock!=7.0;
                if(clock==3.1&&castle!=null)castle.toggleDoor(CastleDoorSlots.all().stream().filter(s->s.gate()).findFirst().orElseThrow().id());
                compare(draw(backend,fast,map,castle,clock,on),draw(backend,dense,map,castle,clock,on));
                for(var entry:saved.entrySet())check(Arrays.equals(entry.getKey(),entry.getValue()),"Published arrays stay immutable across relighting and doors");
            }
        }
        System.out.println("PASS complete sparse/dense vertex-batch parity, real flicker/blackout/doors, scene changes and immutable published arrays (recording backend)");
    }
}
