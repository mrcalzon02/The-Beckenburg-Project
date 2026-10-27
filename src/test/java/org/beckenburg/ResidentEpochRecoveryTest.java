package org.beckenburg;

import java.util.*;

/** Deterministic producer/GL-owner interleavings; no claim of actual GPU execution. */
public final class ResidentEpochRecoveryTest {
    private static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
    private record Handle(long revision,int vertices){}
    private static final class Backend implements GpuWorldRenderer {
        Frame submitted;Result completed;
        long scene,epoch,restored,uploaded;
        Runnable afterEpochRead;
        final Map<Long,Handle> handles=new HashMap<>();
        public boolean ready(){return true;}
        public void submit(Frame frame){submitted=frame;}
        public Result latest(){return completed;}
        public long residencyEpoch(){
            long captured=epoch;
            var hook=afterEpochRead;afterEpochRead=null;
            if(hook!=null)hook.run();
            return captured;
        }
        public String rendererDescription(){return "split scene-start/completion contract backend";}
        void beginScene(long id){
            if(scene!=id){handles.clear();scene=id;epoch++;}
            // A real GL scene disposes resources before replacing latest().
        }
        void complete(){
            var frame=submitted;beginScene(frame.sceneId());
            for(var surface:frame.surfaces())if(surface.resourceId()>0){
                var wanted=new Handle(surface.revision(),surface.vertexCount());
                if(wanted.equals(handles.get(surface.resourceId())))continue;
                float[] source=surface.vertices();
                if(source.length==0){source=surface.recoveryVertices();restored++;}
                check(source.length==surface.vertexCount()*8,"lost resident handle has a complete recovery payload");
                handles.put(surface.resourceId(),wanted);uploaded++;
            }
            completed=new Result(frame.sceneId(),frame.frameId(),frame.width(),frame.height(),
                new int[frame.width()*frame.height()],new float[frame.width()*frame.height()]);
        }
    }
    private static void frame(SceneRenderer renderer,HouseMap map){renderer.drawNetwork(map,List.of(),-1,0);}
    private static void stable(SceneRenderer renderer,SceneRenderer.LiveHouseStats original){
        var now=renderer.liveHouseStats();
        check(now.builds()==original.builds()&&now.packedChunks()==original.packedChunks()
            &&now.recomputedProbes()==original.recomputedProbes(),
            "resource disposal does not rebuild geometry, repack lighting or recompute probes");
    }
    private static void settle(SceneRenderer renderer,HouseMap map,Backend backend,
                               SceneRenderer.LiveHouseStats original){
        for(int i=0;i<3;i++){
            long uploads=backend.uploaded;
            frame(renderer,map);backend.complete();stable(renderer,original);
            if(i>0){
                check(renderer.liveHouseStats().descriptorBytes()==0,"restored lifetime resumes empty upload descriptors");
                check(backend.uploaded==uploads,"steady resumed frames reuse the resident handles");
            }
        }
    }
    public static void main(String[] args){
        var backend=new Backend();GpuWorldRenderer.install(backend);
        try{
            var map=new HouseMap();var renderer=new SceneRenderer();renderer.setResidentHouse(true);
            renderer.setRenderSize(160,120);renderer.roomLights=new RoomLighting();
            var p=map.spawn(0);var q=map.spawn(4);
            renderer.x=p.x();renderer.z=p.z();renderer.eyeHeight=p.y()+1.62;
            renderer.yaw=Math.atan2(q.x()-p.x(),q.z()-p.z());renderer.pitch=-.075;
            frame(renderer,map);backend.complete();
            frame(renderer,map);backend.complete();
            var original=renderer.liveHouseStats();var oldOwnResult=backend.completed;

            // The completed upload is still awaiting producer ACK when a
            // foreign scene starts, disposes its meshes and keeps old latest.
            backend.beginScene(GpuWorldRenderer.nextId());
            check(backend.completed==oldOwnResult,"foreign startup preserves the old own-scene framebuffer");
            frame(renderer,map);
            check(renderer.liveHouseStats().descriptorBytes()>0,"old-epoch result cannot resurrect upload ACKs");
            check(backend.submitted.surfaces().stream().filter(s->s.resourceId()>0)
                .allMatch(s->s.vertices().length>0),"observed disposal resends every unacknowledged resident payload");
            backend.complete();settle(renderer,map,backend,original);

            // Foreign GL work starts immediately after the producer captures
            // its epoch. The submission legitimately still uses draw-only
            // descriptors, so its immutable payload must recover on GL owner.
            oldOwnResult=backend.completed;
            var expectedOld=oldOwnResult;
            backend.afterEpochRead=()->{
                backend.beginScene(GpuWorldRenderer.nextId());
                check(backend.completed==expectedOld,"late foreign startup also preserves old latest");
            };
            frame(renderer,map);stable(renderer,original);
            check(renderer.liveHouseStats().descriptorBytes()==0,"late disposal exercises draw-only recovery");
            var held=backend.submitted.surfaces().stream().filter(s->s.resourceId()>0).toList();
            check(!held.isEmpty(),"test viewpoint submits resident draw commands");
            var snapshots=new HashMap<Long,float[]>();
            for(var surface:held){
                check(surface.vertices().length==0&&surface.recoveryVertices().length==surface.vertexCount()*8,
                    "draw-only recovery retains complete arrays without changing upload descriptors");
                snapshots.put(surface.resourceId(),surface.recoveryVertices().clone());
            }
            long restored=backend.restored;backend.complete();
            check(backend.restored>restored,"GL owner restores missing handles from draw-only recovery arrays");
            settle(renderer,map,backend,original);

            var reused=backend.submitted.surfaces().stream().filter(s->s.resourceId()>0)
                .collect(java.util.stream.Collectors.toMap(GpuWorldRenderer.Surface::resourceId,s->s));
            for(var surface:held)check(reused.get(surface.resourceId()).recoveryVertices()==surface.recoveryVertices(),
                "camera-only recovery reuses immutable packed arrays without copying");
            backend.handles.replaceAll((id,handle)->new Handle(handle.revision()-1,handle.vertices()));
            restored=backend.restored;frame(renderer,map);backend.complete();
            check(renderer.liveHouseStats().descriptorBytes()==0&&backend.restored>restored,
                "draw-only recovery also replaces an existing handle with a stale revision");
            stable(renderer,original);
            renderer.roomLights.toggle(0);frame(renderer,map);backend.complete();
            check(renderer.liveHouseStats().packedChunks()>original.packedChunks(),"fixture change prepares a new packed light revision");
            for(var surface:held)check(Arrays.equals(snapshots.get(surface.resourceId()),surface.recoveryVertices()),
                "new fixture revisions cannot mutate recovery arrays retained by an older queued frame");
            renderer.close();
            System.out.println("PASS old-result ACK invalidation, both disposal interleavings, immutable recovery and resumed reuse");
        }finally{GpuWorldRenderer.INSTALLED.set(null);}
    }
}
