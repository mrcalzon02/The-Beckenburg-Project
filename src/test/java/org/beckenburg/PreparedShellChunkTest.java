package org.beckenburg;
import java.util.List;

/** Functional geometry check, not a cosmetic image-diff test.
 * Static AABB rejection must preserve all triangles accepted by the
 * established per-face frustum rule, including upper floors and entrances. */
public final class PreparedShellChunkTest {
    private static void ok(boolean condition,String why){
        if(!condition)throw new AssertionError(why);
    }
    private static void pose(SceneRenderer renderer,HouseMap map,int room,int targetRoom){
        var eye=map.spawn(room);var target=map.spawn(targetRoom);
        renderer.x=eye.x();renderer.z=eye.z();renderer.eyeHeight=eye.y()+1.62;
        renderer.yaw=Math.atan2(target.x()-eye.x(),target.z()-eye.z());
        renderer.pitch=-.075;
    }
    public static void main(String[] args){
        var map=new HouseMap(HouseMap.Site.HOUSE);
        var renderer=new SceneRenderer();
        renderer.setRenderSize(160,120);
        renderer.roomLights=new RoomLighting();
        long excluded=0;
        int[][] views={{14,0},{13,4},{0,4},{8,0}};
        for(int[] view:views){
            pose(renderer,map,view[0],view[1]);
            renderer.setShellChunkCulling(false);
            renderer.drawNetwork(map,List.of(),-1,0);
            var ref=renderer.frameDiagnostics();
            var baseline=renderer.shellChunkStats();
            ok(baseline.rejectedFaces()==0&&baseline.rejectedChunks()==0,
                "Reference camera should not reject authored shell chunks");
            renderer.setShellChunkCulling(true);
            renderer.drawNetwork(map,List.of(),-1,0);
            var optimized=renderer.frameDiagnostics();
            var chunks=renderer.shellChunkStats();
            ok(optimized.faces()==ref.faces()
                &&optimized.frustumFaces()==ref.frustumFaces()
                &&optimized.submittedTriangles()==ref.submittedTriangles(),
                "Chunk culling changed gameplay geometry at room "+view[0]);
            ok(chunks.totalChunks()>1&&chunks.totalFaces()==baseline.totalFaces()
                &&chunks.candidateFaces()+chunks.rejectedFaces()==chunks.totalFaces(),
                "Prepared chunks must partition the exact static shell");
            excluded+=chunks.rejectedFaces();
        }
        ok(excluded>0,"The House camera route must reject some out-of-frustum shell chunks");
        System.out.println("PASS static-shell visible geometry parity across van, yard, entrance, upstairs; rejected_faces="+excluded);
    }
}
