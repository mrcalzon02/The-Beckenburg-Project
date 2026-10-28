package org.beckenburg;

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import javax.imageio.ImageIO;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
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
    /**
     * C3DP v1: big-endian magic 0x43334450, width, height, then row-major
     * (int32 room owner, float32 camera depth in metres) per pixel.
     * Depth +INF marks an empty pixel; a finite depth with owner -1 is
     * visible but unattributed. PNG is a human-readable owner preview.
     * Neither file is a GPU capture or evidence of GPU parity.
     */
    private static String capture(Path folder,String label,String doorState,
                                  HousePortalDepthReference.Result audit) throws Exception {
        String stem="house-c3-"+label+"-"+doorState;
        int[] owners=audit.pixelOwners();
        float[] depths=audit.pixelDepthMetres();
        int width=audit.width(),height=audit.height();
        check(owners.length==width*height && depths.length==owners.length,
            "Depth-owner buffers must exactly match the reference viewport");
        var binary=folder.resolve(stem+".owner-depth.bin");
        var preview=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
        try(var out=new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(binary)))) {
            out.writeInt(0x43334450);out.writeInt(width);out.writeInt(height);
            for(int i=0;i<owners.length;i++) {
                out.writeInt(owners[i]);out.writeFloat(depths[i]);
                int color;
                if(!Float.isFinite(depths[i])) color=0x000000;
                else if(owners[i]<0) color=0xffffff;
                else {
                    int room=owners[i]+1;
                    color=((room*67)&255)<<16 | ((room*131)&255)<<8 | ((room*197)&255);
                }
                preview.setRGB(i%width,i/width,color);
            }
        }
        check(Files.size(binary)==12L+8L*width*height,
            "C3DP pixel reference must contain all exact depth-owner pairs");
        check(ImageIO.write(preview,"png",folder.resolve(stem+".owners.png").toFile()),
            "Owner preview PNG writer unavailable");
        return binary.getFileName().toString();
    }
    private static String fixed(double value) {
        return String.format(Locale.ROOT,"%.9f",value);
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
        // C3's original 16 views had ZERO basement-owned pixels, including
        // every stair sample. Two actual basement interiors are mandatory
        // positive controls before zero missing-pixel counts mean anything.
        int[][] views={{14,0},{13,4},{0,4},{4,10},{8,4},{10,9},{11,9}};
        StringBuilder csv=new StringBuilder(
            "view,door_state,camera_room,triangles,surfaced_pixels,missing_visible_pixels,unowned_pixels,transparent_samples,basement_10_pixels,basement_11_pixels,excluded_visible_rooms,potential_false_negative,camera_x,camera_eye_y,camera_z,yaw_rad,pitch_rad,focal_px,owner_depth_file\n");
        int samples=0;
        var folder=Path.of("build","e2e","viewport-profile");
        Files.createDirectories(folder);
        for(int doorState=0;doorState<2;doorState++) {
            if(doorState==1)map.toggleDoor(door.id());
            for(int i=0;i<10;i++) {
                String label;
                if(i<views.length) {
                    int source=views[i][0],target=views[i][1];
                    pose(renderer,map,source,target);
                    label=source+"_to_"+target;
                } else {
                    double t=i==7?.15:i==8?.50:.85;
                    stairPose(renderer,map,stair,t,i==8?11:10);
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
                if(i==5)check(audit.roomPixels()[10]>0,
                    "Basement room 10 interior must expose room-10-owned pixels");
                if(i==6)check(audit.roomPixels()[11]>0,
                    "Basement room 11 interior must expose room-11-owned pixels");
                if(audit.cameraRoom()<0 || map.outside(audit.cameraRoom()))
                    check(audit.missingVisiblePixels()==0,
                        "Unknown/outdoor camera must fail open to all rooms");
                String doorLabel=doorState==0?"closed":"open";
                String pixelFile=capture(folder,label,doorLabel,audit);
                double focal=(SceneRenderer.W*.5)/
                    Math.tan(Math.toRadians(renderer.fieldOfView()/2))*
                    renderer.height()/SceneRenderer.H;
                csv.append(label).append(',').append(doorLabel)
                    .append(',').append(audit.cameraRoom()).append(',')
                    .append(audit.triangles()).append(',').append(audit.surfacedPixels())
                    .append(',').append(audit.missingVisiblePixels()).append(',')
                    .append(audit.unownedPixels()).append(',')
                    .append(audit.transparentSamples()).append(',')
                    .append(audit.roomPixels()[10]).append(',')
                    .append(audit.roomPixels()[11]).append(',')
                    .append('"').append(audit.excludedVisibleRooms()).append('"')
                    .append(',').append(audit.hasPotentialFalseNegative())
                    .append(',').append(fixed(renderer.x))
                    .append(',').append(fixed(renderer.eyeHeight))
                    .append(',').append(fixed(renderer.z))
                    .append(',').append(fixed(renderer.yaw))
                    .append(',').append(fixed(renderer.pitch))
                    .append(',').append(fixed(focal))
                    .append(',').append(pixelFile).append('\n');
                samples++;
            }
        }
        var artifact=folder.resolve("house-c3-depth-reference.csv");
        Files.writeString(artifact,csv.toString(),StandardCharsets.UTF_8);
        check(samples==20 && Files.size(artifact)>300,
            "Twenty actual authored House views including basement positive controls required");
        System.out.println("PASS C3 source-only House depth-ownership reference, "+samples+
            " authored views, basement positive controls and 20 pixel owner/depth captures; "+artifact);
        System.out.println("NOT A GPU CAPTURE; hard portal culling remains disabled");
    }
}
