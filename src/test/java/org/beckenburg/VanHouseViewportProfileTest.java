package org.beckenburg;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;

/**
 * Diagnostic capture, not a substitute for actual GPU/installed-Windows
 * profiling. Uses the real House scene and renderer while moving a viewport
 * from van -> outdoor -> entrance -> interior. Draws real peer sprites at
 * successive authored points to compare empty vs occupied frame cost.
 *
 * Writes scene PNG + graph HUD and CSV; does not impose machine-specific FPS.
 */
public final class VanHouseViewportProfileTest {
    private record Stop(String id,int room,int lookAt){}
    private static final List<Stop> ROUTE=List.of(
        new Stop("01-van-facing-house",14,0),
        new Stop("02-outside-facing-house",13,0),
        new Stop("03-front-path",12,0),
        new Stop("04-enter-house",0,4),
        new Stop("05-deeper-interior",4,0),
        new Stop("06-upstairs-room",8,4));
    private static SceneRenderer.RemoteBody person(HouseMap map,int room,int slot){
        var spawn=map.spawn(room);
        return new SceneRenderer.RemoteBody(spawn.x(),spawn.y(),spawn.z(),
            slot,0,slot%2==0?"observer":"cleric");
    }
    private static List<SceneRenderer.RemoteBody> actors(HouseMap map,int step){
        // A team starts in the van, advances along authored outdoor positions,
        // then progressively fills the building. No fake ghost/evidence data.
        int[] approach={14,13,12,0,4,8};
        return List.of(person(map,approach[Math.min(step,5)],1),
            person(map,approach[Math.max(0,Math.min(step-1,5))],2),
            person(map,approach[Math.max(0,Math.min(step-2,5))],3),
            person(map,approach[Math.max(0,Math.min(step-3,5))],4));
    }
    private static String csv(String name,int actors,long elapsed,
                              SceneRenderer.FrameDiagnostics m){
        return String.format(Locale.ROOT,
            "%s,%d,%s,%.3f,%.3f,%.3f,%.3f,%d,%d,%d,%d,%d,%s,%s,%d,%d,%d,%d,%d%n",
            name,actors,m.gpu()?"GPU":"CPU-fallback",elapsed/1e6,
            m.sceneBuildNs()/1e6,m.geometryNs()/1e6,m.effectsNs()/1e6,
            m.faces(),m.frustumFaces(),m.sceneryTotal(),m.sceneryVisible(),
            m.submittedTriangles(),m.gpuReadbackAvailable(),m.gpuReadbackFresh(),
            m.lighting().samples(),m.lighting().cacheHits(),
            m.lighting().shadowRays(),m.lighting().normalRays(),
            m.depthRejectedFaces());
    }
    private static void screenshot(Path dest,SceneRenderer renderer,
                                   DebugPerformanceHud hud)throws Exception{
        BufferedImage capture=new BufferedImage(1100,740,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=capture.createGraphics();
        try{
            GraphicsOptions.current().draw(g,renderer.image(),1100,740);
            hud.draw(g,1100,740);
        }finally{g.dispose();}
        if(!ImageIO.write(capture,"png",dest.toFile()))
            throw new AssertionError("No PNG encoder for HUD screenshot");
    }
    public static void main(String[] args)throws Exception{
        if(!DebugPerformanceHud.enabled())
            throw new AssertionError("Profile requires -Dbeckenburg.debug.overlay=true");
        Path output=Path.of("build","e2e","viewport-profile");
        Files.createDirectories(output);
        StringBuilder csv=new StringBuilder("view,actors,backend,total_ms,scene_build_ms,geometry_ms,effects_ms,faces_built,faces_frustum,scenery_instances,scenery_visible,submitted_triangles,readback_available,readback_fresh,light_samples,light_cache_hits,shadow_rays,normal_rays,depth_rejected_faces\n");
        HouseMap map=new HouseMap(HouseMap.Site.HOUSE);
        for(int i=0;i<ROUTE.size();i++){
            Stop stop=ROUTE.get(i);
            SceneRenderer scene=new SceneRenderer();
            var eye=map.spawn(stop.room());
            var target=map.spawn(stop.lookAt());
            scene.x=eye.x();scene.z=eye.z();scene.eyeHeight=eye.y()+1.62;
            scene.yaw=Math.atan2(target.x()-eye.x(),target.z()-eye.z());
            scene.pitch=-.075;
            DebugPerformanceHud hud=new DebugPerformanceHud();
            // Empty first render captures scene setup and first-visible-geometry
            // latency; subsequent occupied render isolates character cost.
            for(int population:new int[]{0,4}){
                List<SceneRenderer.RemoteBody> peers=population==0?List.of():actors(map,i);
                long start=System.nanoTime();
                scene.drawPractice(map,peers,i*.15);
                long elapsed=System.nanoTime()-start;
                var metric=scene.frameDiagnostics();
                if(metric==null||metric.totalNs()<=0||metric.faces()<=0)
                    throw new AssertionError("Missing actual renderer diagnostics for "+stop.id());
                hud.sample(elapsed,scene,map,population,"HOUSE "+stop.id());
                csv.append(csv(stop.id(),population,elapsed,metric));
                System.out.printf(Locale.ROOT,
                    "VIEWPORT_PROFILE %s actors=%d backend=%s total_ms=%.2f build_ms=%.2f geometry_ms=%.2f effects_ms=%.2f faces=%d visible=%d scenery=%d/%d shadow_rays=%d normal_rays=%d depth_rejected=%d%n",
                    stop.id(),population,metric.gpu()?"GPU":"CPU-fallback",
                    elapsed/1e6,metric.sceneBuildNs()/1e6,metric.geometryNs()/1e6,
                    metric.effectsNs()/1e6,metric.faces(),metric.frustumFaces(),
                    metric.sceneryVisible(),metric.sceneryTotal(),
                    metric.lighting().shadowRays(),metric.lighting().normalRays(),
                    metric.depthRejectedFaces());
                if(population==4)screenshot(output.resolve(stop.id()+".png"),scene,hud);
            }
        }
        Files.writeString(output.resolve("viewport-profile.csv"),csv.toString(),StandardCharsets.UTF_8);
        // In addition to the moving peer sprite snapshots above, execute the
        // real solo CampaignAlly pathfinder and swept collision from the van
        // toward room zero. Preserve both navigation timing and route status:
        // a stalled ally must be diagnosed, not silently teleported indoors.
        var session=new InvestigationSession(map,InvestigationSession.Role.OBSERVER,20261025L);
        var actualAlly=new CampaignAlly(map,session);
        var destination=map.spawn(0);
        StringBuilder route=new StringBuilder("step,room,x,y,z,advance_ms,travel_m,report\n");
        double travelled=0;
        var previous=actualAlly.position();
        for(int step=0;step<700;step++){
            long began=System.nanoTime();
            actualAlly.advance(destination.x(),destination.y(),destination.z());
            long advanceNs=System.nanoTime()-began;
            var at=actualAlly.position();
            double moved=Math.hypot(at.x()-previous.x(),at.z()-previous.z());
            travelled+=moved;
            if(step%10==0||step==699||actualAlly.room()==0){
                route.append(String.format(Locale.ROOT,
                    "%d,%d,%.3f,%.3f,%.3f,%.4f,%.3f,%s%n",
                    step,actualAlly.room(),at.x(),at.y(),at.z(),advanceNs/1e6,
                    travelled,actualAlly.report().replace(',', ';').replace('\n',' ')));
            }
            previous=at;
            if(actualAlly.room()==0)break;
        }
        Files.writeString(output.resolve("actual-ally-navigation.csv"),
            route.toString(),StandardCharsets.UTF_8);
        System.out.printf(Locale.ROOT,
            "NPC_ROUTE actual CampaignAlly from van toward House room 0: end_room=%d travel_m=%.3f source=real_stepped_collisions%n",
            actualAlly.room(),travelled);
        if(!(travelled>.4))throw new AssertionError(
            "Actual NPC failed to leave its van starting position; inspect actual-ally-navigation.csv");
        Files.writeString(output.resolve("README.txt"),
            "Controlled Java 17 renderer benchmark on Windows CI. No OpenGL backend is installed in this process: CPU fallback measures CPU geometry, raster and effects only. GPU submission/readback requires separately instrumented OpenGL runner. Scene positions: House van room 14, yard 13/12, entrance 0, interior 4, upstairs 8. Four peer sprites occupy scripted successive physical spawns; actual solo CampaignAlly route and navigation timings are stored separately in actual-ally-navigation.csv. Initial empty render is cold per viewport; full team render is warm on that viewport. Results are not a proof of real-time performance or actual AI navigation. PNGs include real HUD and scene.\n",
            StandardCharsets.UTF_8);
        System.out.println("PASS six authored van-to-house viewport captures, real scene metrics, peer sprites and PNG HUD");
    }
}
