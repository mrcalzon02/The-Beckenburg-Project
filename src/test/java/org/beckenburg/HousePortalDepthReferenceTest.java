package org.beckenburg;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/** C3 diagnostic: actual authored House depth ownership, NOT a GPU capture. */
public final class HousePortalDepthReferenceTest {
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    private static HousePortalDepthReference.Triangle triangle(double depth, int owner, int material) {
        return new HousePortalDepthReference.Triangle(
            new HousePortalDepthReference.Vertex(-.4*depth,-.4*depth,depth,0,0),
            new HousePortalDepthReference.Vertex(.4*depth,-.4*depth,depth,1,0),
            new HousePortalDepthReference.Vertex(0,.4*depth,depth,.5,1),owner,material);
    }
    private static void depthRegressions() {
        var basement=triangle(4,10,32);
        var floor=triangle(2,0,32);
        var blocked=HousePortalDepthReference.raster(0,32,32,20,
            List.of(basement,floor),Set.of(0),15,(material,u,v)->255);
        check(blocked.surfacedPixels()>0 && blocked.missingVisiblePixels()==0
                && blocked.roomPixels()[10]==0,
            "Opaque near floor must hide farther basement pixels regardless of draw order");
        var open=HousePortalDepthReference.raster(0,32,32,20,
            List.of(basement),Set.of(0),15,(material,u,v)->255);
        check(open.missingVisiblePixels()>0 && open.roomPixels()[10]>0
                && open.hasPotentialFalseNegative(),
            "Stair/floor aperture must expose the missing basement room");
        var glass=HousePortalDepthReference.raster(0,32,32,20,
            List.of(basement,triangle(2,0,257)),Set.of(0),15,
            (material,u,v)->material==257?110:255);
        check(glass.roomPixels()[10]==open.roomPixels()[10]
                && glass.missingVisiblePixels()==open.missingVisiblePixels()
                && glass.transparentSamples()>0,
            "Translucent glass may not occlude a farther room");
        var unknown=HousePortalDepthReference.raster(-1,32,32,20,
            List.of(triangle(2,-1,32)),Set.of(0),15,(material,u,v)->255);
        check(unknown.unownedPixels()>0 && unknown.missingVisiblePixels()==0,
            "Unowned geometry must be retained, never attributed to a missing room");
    }
    private static void pose(SceneRenderer renderer,HouseMap map,int source,int target) {
        var eye=map.spawn(source);
        var look=map.spawn(target);
        renderer.x=eye.x();renderer.z=eye.z();renderer.eyeHeight=eye.y()+1.62;
        double dx=look.x()-eye.x(), dz=look.z()-eye.z();
        renderer.yaw=Math.atan2(dx,dz);
        renderer.pitch=Math.atan2(look.y()+1.15-renderer.eyeHeight,Math.hypot(dx,dz));
    }
    private static void stairPose(SceneRenderer renderer,HouseMap map,
                                  LevelDefinition.Stair stair,double t,int target) {
        renderer.x=stair.x0()+(stair.x1()-stair.x0())*t;
        renderer.z=stair.z0()+(stair.z1()-stair.z0())*t;
        renderer.eyeHeight=stair.y0()+(stair.y1()-stair.y0())*t+1.62;
        var look=map.spawn(target);
        double dx=look.x()-renderer.x,dz=look.z()-renderer.z;
        renderer.yaw=Math.atan2(dx,dz);
        renderer.pitch=Math.atan2(look.y()+1.1-renderer.eyeHeight,Math.hypot(dx,dz));
    }
    public static void main(String[] args) throws Exception {
        depthRegressions();
        var map=new HouseMap(HouseMap.Site.HOUSE);
        var renderer=new SceneRenderer();
        renderer.setRenderSize(160,120);
        var stair=map.level().stairs().stream()
            .filter(s->s.a()==9&&s.b()==4 || s.a()==4&&s.b()==9)
            .findFirst().orElseThrow();
        var door=map.level().portals().stream().filter(p->p.door())
            .findFirst().orElseThrow();
        int[][] views={{14,0},{13,4},{0,4},{4,10},{8,4}};
        StringBuilder csv=new StringBuilder(
            "view,door_state,camera_room,triangles,surfaced_pixels,missing_visible_pixels,unowned_pixels,transparent_samples,basement_10_pixels,basement_11_pixels,excluded_visible_rooms,potential_false_negative\n");
        int samples=0;
        for(int doorState=0;doorState<2;doorState++) {
            if(doorState==1)map.toggleDoor(door.id());
            for(int i=0;i<8;i++) {
                String label;
                if(i<views.length) {
                    int source=views[i][0],target=views[i][1];
                    pose(renderer,map,source,target);
                    label=source+"_to_"+target;
                } else {
                    double t=i==5?.15:i==6?.50:.85;
                    stairPose(renderer,map,stair,t,i==6?11:10);
                    label="basement_stair_"+t;
                }
                var audit=renderer.auditHousePortalDepth(map);
                int sum=audit.unownedPixels();
                for(int count:audit.roomPixels())sum+=count;
                check(audit.triangles()>0 && audit.surfacedPixels()>0,
                    "Actual House geometry must produce depth-owned pixels at "+label);
                check(sum==audit.surfacedPixels(),
                    "Every surfaced pixel must have an explicit owner or remain unowned");
                check(audit.hasPotentialFalseNegative()==(audit.missingVisiblePixels()>0),
                    "A missing visible pixel must flag unsafe portal rejection");
                if(audit.cameraRoom()<0 || map.outside(audit.cameraRoom()))
                    check(audit.missingVisiblePixels()==0,
                        "Unknown/outdoor camera must fail open to all rooms");
                csv.append(label).append(',').append(doorState==0?"closed":"open")
                    .append(',').append(audit.cameraRoom()).append(',')
                    .append(audit.triangles()).append(',').append(audit.surfacedPixels())
                    .append(',').append(audit.missingVisiblePixels()).append(',')
                    .append(audit.unownedPixels()).append(',')
                    .append(audit.transparentSamples()).append(',')
                    .append(audit.roomPixels()[10]).append(',')
                    .append(audit.roomPixels()[11]).append(',')
                    .append('"').append(audit.excludedVisibleRooms()).append('"')
                    .append(',').append(audit.hasPotentialFalseNegative()).append('\n');
                samples++;
            }
        }
        var artifact=Path.of("build","e2e","viewport-profile","house-c3-depth-reference.csv");
        Files.createDirectories(artifact.getParent());
        Files.writeString(artifact,csv.toString(),StandardCharsets.UTF_8);
        check(samples==16 && Files.size(artifact)>300,
            "Sixteen actual authored House views must be recorded");
        System.out.println("PASS C3 source-only House depth-ownership reference, "+samples+
            " authored views and synthetic floor/aperture/glass regressions; "+artifact);
        System.out.println("NOT A GPU CAPTURE; hard portal culling remains disabled");
    }
}
