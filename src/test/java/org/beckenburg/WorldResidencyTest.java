package org.beckenburg;

import java.util.*;

/** CPU-side production submission contract. This recording backend does not
 * rasterize and is explicitly not evidence of hardware FPS or image parity. */
public final class WorldResidencyTest {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static final class Recording implements GpuWorldRenderer {
        Frame frame;Result result;
        public boolean ready(){return true;}
        public String rendererDescription(){return "submission recorder; no GPU";}
        public void submit(Frame f){frame=f;result=new Result(f.sceneId(),f.frameId(),f.width(),f.height(),new int[f.width()*f.height()],new float[f.width()*f.height()]);}
        public Result latest(){return result;}
    }
    private static Map<Long,GpuWorldRenderer.Surface> resident(GpuWorldRenderer.Frame frame){
        var result=new HashMap<Long,GpuWorldRenderer.Surface>();
        for(var surface:frame.surfaces())if(surface.resourceId()>0)result.put(surface.resourceId(),surface);
        return result;
    }
    private static int retained(Map<Long,GpuWorldRenderer.Surface> before,Map<Long,GpuWorldRenderer.Surface> after){
        int count=0;
        for(var entry:before.entrySet()){
            var next=after.get(entry.getKey());
            if(next!=null&&next.revision()==entry.getValue().revision()){
                check(next.vertices()==entry.getValue().vertices(),"Unchanged resource must retain the actual array, not merely equal repacked data");count++;
            }
        }
        return count;
    }
    public static void main(String[] args){
        var backend=new Recording();GpuWorldRenderer.install(backend);
        var renderer=new SceneRenderer();renderer.lamp=false;
        var castle=new CastleHubMap();renderer.x=13.5;renderer.z=20.5;renderer.yaw=.1;renderer.pitch=-.05;
        renderer.drawCastle(castle,2.5,true);var first=resident(backend.frame);
        check(first.size()>20,"Actual Castle world must reach the resident path");
        var stats=renderer.worldResidentStats();renderer.drawCastle(castle,2.5,true);
        check(stats.builds()==renderer.worldResidentStats().builds(),"Identical Castle frame must not rebuild VBO source data");
        check(stats.topologyBuilds()==renderer.worldResidentStats().topologyBuilds(),"Identical Castle frame must not reconstruct props");
        check(retained(first,resident(backend.frame))==first.size(),"Every unchanged visible Castle buffer is retained");
        renderer.lamp=true;renderer.drawCastle(castle,2.5,true);
        check(backend.frame.lampOn(),"Flashlight still updates shader uniforms");
        check(retained(first,resident(backend.frame))==first.size(),"Flashlight must not rebuild static Castle lighting");
        renderer.yaw+=.04;renderer.drawCastle(castle,2.5,true);
        check(retained(first,resident(backend.frame))>first.size()/2,"Camera rotation must retain shared visible source arrays");
        check(stats.topologyBuilds()==renderer.worldResidentStats().topologyBuilds(),"Camera motion must not reconstruct Castle topology");
        renderer.drawCastle(castle,2.5,true);var beforeFlicker=resident(backend.frame);
        renderer.drawCastle(castle,4.3,true);
        var flicker=resident(backend.frame);int changed=0;
        for(var e:beforeFlicker.entrySet())if(flicker.containsKey(e.getKey())&&flicker.get(e.getKey()).revision()!=e.getValue().revision())changed++;
        check(changed>0,"Real candle flicker must remain animated");
        check(renderer.frameDiagnostics().lighting().shadowRays()==0,"Cosmetic flicker must not repeat architectural shadow rays");
        check(renderer.frameDiagnostics().lighting().normalRays()==0,"Cosmetic flicker must not repeat normal shadow rays");
        long scene=backend.frame.sceneId();
        var gate=CastleDoorSlots.all().stream().filter(s->s.gate()).findFirst().orElseThrow();
        castle.toggleDoor(gate.id());renderer.drawCastle(castle,4.3,true);
        check(scene==backend.frame.sceneId(),"Door state must not rebuild unrelated world topology");
        check(renderer.frameDiagnostics().lighting().shadowRays()>0,"A real door change must refresh visibility");
        renderer.setRenderSize(640,400);renderer.drawCastle(castle,4.3,true);
        check(backend.frame.width()==640&&backend.frame.height()==400,"Resize must submit the actual new framebuffer");
        long topology=renderer.worldResidentStats().topologyBuilds();
        CastleLevelEdits.current().paintFloor(2,2,MaterialAtlas.STONE);renderer.drawCastle(castle,4.3,true);
        check(renderer.worldResidentStats().topologyBuilds()==topology+1,"Authored geometry revision must invalidate retained topology");
        check(scene!=backend.frame.sceneId(),"Editor topology changes must dispose old GPU generation");
        for(var site:List.of(HouseMap.Site.HOUSE,HouseMap.Site.SCHOOL,HouseMap.Site.HOLLOW_CREEK)){
            var map=new HouseMap(site);var spawn=map.spawn(0);
            renderer.x=spawn.x();renderer.z=spawn.z();renderer.eyeHeight=spawn.y()+1.62;renderer.yaw=Math.PI/3;
            renderer.drawNetwork(map,List.of(),-1,2.5);var mapFirst=resident(backend.frame);
            check(!mapFirst.isEmpty(),"Real "+site+" scenery must be resident");
            var mapStats=renderer.worldResidentStats();renderer.drawNetwork(map,List.of(),-1,2.5);
            check(retained(mapFirst,resident(backend.frame))==mapFirst.size(),"Unchanged "+site+" must retain arrays and revisions");
            check(mapStats.builds()==renderer.worldResidentStats().builds(),"Unchanged "+site+" must not relight/repack resident scenery");
            check(renderer.worldResidentStats().bytes()>=0&&renderer.worldResidentStats().bytes()<=128L*1024*1024,"Bounded resident geometry across scene transitions");
            renderer.uvLamp=true;renderer.drawNetwork(map,List.of(),-1,2.5);
            check(backend.frame.surfaces().stream().noneMatch(s->s.resourceId()>0),"Live UV must use its correct dynamic light path");
            renderer.uvLamp=false;renderer.drawNetwork(map,List.of(),-1,2.5);
            check(!resident(backend.frame).isEmpty(),"Residency must recover after dynamic UV use");
        }
        renderer.drawCastle(castle,4.3,true);
        check(!resident(backend.frame).isEmpty(),"Returning to Castle must recover the correct scene resources");
        CastleLevelEdits.current().reset();
        System.out.println("PASS real Castle/House/School/native-map submission residency, light updates, occlusion, UV, resize, editor and scene transitions (recording backend)");
    }
}
