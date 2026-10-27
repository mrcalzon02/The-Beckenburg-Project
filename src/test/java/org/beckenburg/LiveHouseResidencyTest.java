package org.beckenburg;

import java.util.*;

/** Actual drawNetwork assembly: residency, dropped frames, tools and scene lifetime. */
public final class LiveHouseResidencyTest {
    static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
    static final class Backend implements GpuWorldRenderer {
        Frame submitted;Result completed;long scene;
        final Map<Long,Long> handles=new HashMap<>();
        public boolean ready(){return true;}
        public void submit(Frame frame){submitted=frame;}
        public Result latest(){return completed;}
        public String rendererDescription(){return "functional contract backend; not GPU evidence";}
        void complete(){
            var f=submitted;if(scene!=f.sceneId()){handles.clear();scene=f.sceneId();}
            for(var s:f.surfaces())if(s.resourceId()>0){
                if(s.vertices().length>0)handles.put(s.resourceId(),s.revision());
                else check(Objects.equals(handles.get(s.resourceId()),s.revision()),"draw-only handle was not delivered");
            }
            completed=new Result(f.sceneId(),f.frameId(),f.width(),f.height(),new int[f.width()*f.height()],new float[f.width()*f.height()]);
        }
    }
    static void pose(SceneRenderer renderer,HouseMap map,int room,int look){
        var p=map.spawn(room);var q=map.spawn(look);renderer.x=p.x();renderer.z=p.z();renderer.eyeHeight=p.y()+1.62;
        renderer.yaw=Math.atan2(q.x()-p.x(),q.z()-p.z());renderer.pitch=-.075;
    }
    static void frame(SceneRenderer renderer,HouseMap map){renderer.drawNetwork(map,List.of(),-1,0);}
    public static void main(String[] args){
        var gpu=new Backend();GpuWorldRenderer.install(gpu);
        try{
            var map=new HouseMap();var renderer=new SceneRenderer();renderer.setResidentHouse(true);
            renderer.setRenderSize(160,120);renderer.roomLights=new RoomLighting();pose(renderer,map,14,0);
            frame(renderer,map);var first=renderer.liveHouseStats();System.out.println("COLD "+first);
            check(first.active()&&first.chunks()>0&&first.sourceFaces()>350,"real House architecture/scenery, not the 350-face proof");
            check(gpu.submitted.surfaces().stream().anyMatch(s->s.resourceId()>0),"normal gameplay must submit resident surfaces");
            frame(renderer,map);gpu.complete(); // Deliberately discard the first submitted frame.
            frame(renderer,map);gpu.complete();
            check(renderer.liveHouseStats().descriptorBytes()==0,"acknowledged stationary chunks do not resend arrays");
            long builds=first.builds(),packed=renderer.liveHouseStats().packedChunks(),probes=renderer.liveHouseStats().recomputedProbes();
            for(int[] view:new int[][]{{14,0},{13,0},{0,4},{4,8},{8,4},{14,0}}){
                pose(renderer,map,view[0],view[1]);frame(renderer,map);gpu.complete();
                var stat=renderer.liveHouseStats();
                check(stat.builds()==builds&&stat.packedChunks()==packed&&stat.recomputedProbes()==probes,
                    "camera-only turns cannot rebuild geometry, pack arrays or refresh stationary probes");
                check(stat.descriptorBytes()==0,"camera-only turns after warmup cannot upload static arrays");
            }
            renderer.setRenderSize(320,200);frame(renderer,map);gpu.complete();
            check(renderer.liveHouseStats().descriptorBytes()==0,"resize preserves resident topology");
            renderer.roomLights.toggle(0);frame(renderer,map);gpu.complete();
            check(renderer.liveHouseStats().builds()==builds,"fixture switch preserves geometry");
            var door=map.level().portals().stream().filter(p->p.door()).findFirst().orElseThrow();map.toggleDoor(door.id());
            frame(renderer,map);gpu.complete();check(renderer.liveHouseStats().builds()==builds,"door changes update lighting/dynamic leaf without structural rebuild");
            renderer.uvLamp=true;frame(renderer,map);gpu.complete();
            check(!renderer.liveHouseStats().active()&&gpu.submitted.surfaces().stream().noneMatch(s->s.resourceId()>0),"UV retains complete existing optical/occlusion renderer");
            renderer.uvLamp=false;frame(renderer,map);gpu.complete();
            check(renderer.liveHouseStats().active()&&renderer.liveHouseStats().builds()==builds,"leaving UV fallback preserves stationary resources");
            var other=new SceneRenderer();other.setRenderSize(160,120);pose(other,map,0,4);frame(other,map);gpu.complete();
            frame(renderer,map);gpu.complete();check(renderer.liveHouseStats().descriptorBytes()>0,"return from another GPU scene must restore disposed handles");
            var second=new HouseMap();pose(renderer,second,0,4);frame(renderer,second);gpu.complete();
            check(renderer.liveHouseStats().builds()==builds+1,"new map owns a new resource generation");
            check(renderer.liveHouseStats().preparedVertices()<=2L*600000*3,"two generations respect static vertex budget");
            System.out.println("PASS actual House drawNetwork residency, dropped upload delivery, all-room camera reuse, resize, fixtures, doors, UV fallback and scene/map ownership");
        }finally{GpuWorldRenderer.INSTALLED.set(null);}
    }
}
