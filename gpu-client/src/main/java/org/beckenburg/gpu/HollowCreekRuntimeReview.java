package org.beckenburg.gpu;
import org.beckenburg.*;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
/** Real game renderer, current assets/physics, scripted accessible camera.
 * No native renders, no fabricated HUD, no LAN/human acceptance claim.
 */
public final class HollowCreekRuntimeReview {
 private record View(String name,double x,double z,double tx,double ty,double tz){}
 public static void execute(GpuWorldRenderer gpu)throws Exception{
  HouseMap map=new HouseMap(HouseMap.Site.HOLLOW_CREEK);if(!map.valid())throw new AssertionError("Invalid game map");
  var renderer=new SceneRenderer();renderer.roomLights=new RoomLighting();renderer.setLampHardware(EquipmentTiers.Tier.II,false);renderer.setRenderSize(640,360);renderer.setPsxPreset(PsxPipeline.Preset.SOFT);
  var views=List.of(new View("water-beach",34,5,30,-1.95,-3),new View("water-riverside-east",55,15,50,-1.95,18),new View("water-riverside-west",42,13,47,-1.95,18),new View("water-dock-boat",26.2,-4,23.9,-1.83,-5));
  int frames=Integer.getInteger("beckenburg.review.frames",72);if(frames<1||frames>120)throw new IllegalArgumentException("Capture frame budget");String only=System.getProperty("beckenburg.review.view","");
  String stage=System.getProperty("beckenburg.review.stage","water");if(!stage.matches("[a-z-]+"))throw new IllegalArgumentException("stage");
  Path stills=Path.of("development/levels/previews/hollow-creek-runtime",stage);Files.createDirectories(stills);var report=new StringBuilder("{\n\"kind\":\"actual game renderer / scripted accessible cameras\",\n\"native_or_LAN_acceptance\":false,\n\"flashlight_tier\":\"II\",\n\"psx_preset\":\"SOFT\",\n\"renderer\":\"").append(gpu.rendererDescription().replace("\\","\\\\").replace("\"","\\\"")).append("\",\n\"views\":[");boolean comma=false;
  for(var view:views){
   if(!only.isEmpty()&&!view.name.equals(only))continue;
   double feet=map.groundHeight(view.x,view.z,map.roomAt(view.x,view.z));if(!map.traversable(view.x,feet,view.z))throw new AssertionError("Inaccessible review camera "+view.name+" Y"+feet);
   renderer.x=view.x;renderer.z=view.z;renderer.eyeHeight=feet+HunterPosture.STANDING_EYE;double yaw=Math.atan2(view.tx-view.x,view.tz-view.z);renderer.pitch=Math.atan2(view.ty-renderer.eyeHeight,Math.hypot(view.tx-view.x,view.tz-view.z));
   Path dir=Path.of("build/hollow-creek-runtime-frames",stage,view.name);Files.createDirectories(dir);long start=System.nanoTime();int hullSamples=0,waterSamples=0;
   for(int frame=0;frame<frames;frame++){
    double clock=frame/12.0;renderer.yaw=yaw;
    renderer.drawNetwork(map,List.of(),-1,clock);long submitted=renderer.submittedGpuFrameId();long deadline=System.nanoTime()+60_000_000_000L;
    while((gpu.latest()==null||gpu.latest().frameId()<submitted)&&System.nanoTime()<deadline)Thread.sleep(15);
    var gpuFrame=gpu.latest();if(gpuFrame==null||gpuFrame.frameId()<submitted)throw new AssertionError("No fresh actual GPU view "+view.name);
    // Same camera/clock consumes that exact completed world/depth readback and water layer.
    renderer.drawNetwork(map,List.of(),-1,clock);
    if(renderer.waterStatistics!=null){hullSamples+=renderer.waterStatistics.excludedHullSamples();waterSamples+=renderer.waterStatistics.pixels();}
    ImageIO.write(renderer.image(),"PNG",dir.resolve(String.format("frame-%03d.png",frame)).toFile());
    if(frame==0)ImageIO.write(renderer.image(),"PNG",stills.resolve(view.name+"-runtime.png").toFile());
   }
   if(comma)report.append(',');comma=true;report.append("{\"name\":\"").append(view.name).append("\",\"x\":").append(view.x).append(",\"feet_y\":").append(feet).append(",\"z\":").append(view.z).append(",\"frames\":").append(frames).append(",\"capture_wall_ms\":").append((System.nanoTime()-start)/1_000_000).append(",\"water_samples\":").append(waterSamples).append(",\"dry_hull_samples\":").append(hullSamples).append('}');
   System.out.println("RUNTIME VIEW "+view.name+" "+frames+" fresh GPU frames; water samples="+waterSamples+" dry hull="+hullSamples);
  }
  report.append("]\n}\n");Files.writeString(Path.of("development/levels/verification/hollow-creek-game-captures-"+stage+".json"),report);System.out.println("PASS actual native-site game renderer captures; no full gameplay/LAN/human acceptance inferred");
 }
 private HollowCreekRuntimeReview(){}
}
