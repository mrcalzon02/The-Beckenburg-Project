package org.beckenburg;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** C2 functional diagnostic: compare actual authored face-frustum visibility
 * with portal reachability WITHOUT enabling portal-based culling. */
public final class PortalVisibilityAuditTest {
    private static void check(boolean yes,String why){
        if(!yes)throw new AssertionError(why);
    }
    private static void pose(SceneRenderer renderer,HouseMap map,int source,int target){
        var eye=map.spawn(source);
        var look=map.spawn(target);
        renderer.x=eye.x();renderer.z=eye.z();renderer.eyeHeight=eye.y()+1.62;
        renderer.yaw=Math.atan2(look.x()-eye.x(),look.z()-eye.z());
        renderer.pitch=-.075;
    }
    public static void main(String[] args)throws Exception{
        var map=new HouseMap(HouseMap.Site.HOUSE);
        var renderer=new SceneRenderer();
        renderer.setRenderSize(160,120);
        StringBuilder csv=new StringBuilder("room,target,door_state,camera_room,visible_faces,listed_visible,unlisted_visible,unowned_visible,candidate_rooms,unlisted_rooms\n");
        int[][] views={{14,0},{13,4},{0,4},{4,8},{8,4}};
        int sampled=0;
        for(int state=0;state<2;state++){
            if(state==1){
                var door=map.level().portals().stream()
                    .filter(p->p.door()).findFirst().orElseThrow();
                map.toggleDoor(door.id());
            }
            for(int[] view:views){
                pose(renderer,map,view[0],view[1]);
                var audit=renderer.auditPortalVisibility(map);
                check(audit.frustumVisibleFaces()>0,
                    "Real House benchmark must contain visible authored architecture");
                check(audit.frustumVisibleFaces()==audit.candidateVisibleFaces()
                     +audit.nonCandidateVisibleFaces()+audit.unownedVisibleFaces(),
                    "Every visible face must be accounted for, including unknown ownership");
                check(audit.nonCandidateVisibleFaces()>=audit.nonCandidateRooms().size(),
                    "Each missing room must own at least one actual visible face");
                if(audit.cameraRoom()<0||map.outside(audit.cameraRoom()))
                    check(audit.nonCandidateVisibleFaces()==0,
                        "Unknown/exterior fallback may not drop any owned visible faces");
                csv.append(view[0]).append(',').append(view[1]).append(',')
                    .append(state==0?"closed":"open").append(',')
                    .append(audit.cameraRoom()).append(',')
                    .append(audit.frustumVisibleFaces()).append(',')
                    .append(audit.candidateVisibleFaces()).append(',')
                    .append(audit.nonCandidateVisibleFaces()).append(',')
                    .append(audit.unownedVisibleFaces()).append(',')
                    .append(audit.candidateRooms()).append(',')
                    .append('"').append(audit.nonCandidateRooms()).append('"').append('\n');
                sampled++;
            }
        }
        Path artifact=Path.of("build","e2e","viewport-profile","portal-candidate-audit.csv");
        Files.createDirectories(artifact.getParent());
        Files.writeString(artifact,csv.toString(),StandardCharsets.UTF_8);
        check(sampled==10&&Files.size(artifact)>150,
            "Ten real House reference viewpoints must be recorded");
        System.out.println("PASS diagnostic portal/face visibility cross-check: "+sampled+
            " real House camera states; no production occlusion enabled. CSV="+artifact);
    }
}
