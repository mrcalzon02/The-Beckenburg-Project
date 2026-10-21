package org.beckenburg;
import java.util.*;
import java.nio.file.*;

/** Compare the actual shared instance path to a fully expanded textured reference image. */
public final class SceneryInstancesTest {
    static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
    static SceneRenderer renderer(){var s=new SceneRenderer();s.setPsxPreset(PsxPipeline.Preset.OFF);s.roomLights=new RoomLighting();s.x=31;s.z=40;s.yaw=-1.6;return s;}
    static double[] timings(SceneRenderer s,HouseMap map){
        for(int i=0;i<5;i++)s.drawNetwork(map,List.of(),-1,0);
        double[] ms=new double[24];for(int i=0;i<ms.length;i++){long start=System.nanoTime();s.drawNetwork(map,List.of(),-1,0);ms[i]=(System.nanoTime()-start)/1e6;}Arrays.sort(ms);return ms;
    }
    public static void main(String[] args)throws Exception{
        LevelVisualCapture.inspection();var map=BlackpineLevel.map();map.configureHiding(100);for(var p:map.level().portals())if(p.door())map.toggleDoor(p.id());
        var s=renderer();long checked=0,different=0;
        double[][] poses={{31,40,-1.6,0},{31.5,22,-2.5,.1},{63.5,32,Math.PI/2,0},{28.5,13,-2.5,-.1},{27,17.25,Math.PI,0},{8,4,0,.8}};
        for(double[] p:poses){
            s.x=p[0];s.z=p[1];s.yaw=p[2];s.pitch=p[3];s.setInstancedScenery(false);s.drawNetwork(map,List.of(),-1,0);int[] reference=s.pixels().clone();
            s.setInstancedScenery(true);s.drawNetwork(map,List.of(),-1,0);
            int changed=0;for(int i=0;i<reference.length;i++)if(reference[i]!=s.pixels()[i])changed++;
            check(changed<reference.length*.003,"instance frustum/material/depth parity at "+Arrays.toString(p)+": "+changed);different+=changed;checked+=reference.length;
        }
        // Physics and controls live outside this renderer; rendering neither edits the definition nor the UI.
        String revision=map.level().revision();s=renderer();s.setInstancedScenery(false);double[] expanded=timings(s,map);
        s.setInstancedScenery(true);double[] instanced=timings(s,map);var stats=s.instanceStats();
        check(stats.culled()>0&&stats.submittedTriangles()<stats.expandedTriangles(),"whole instances are rejected before triangle processing");
        check(stats.sharedTriangles()<stats.expandedTriangles()/2,"repeated geometry is retained once per model");
        check(map.level().revision().equals(revision),"drawing cannot mutate authoritative level");
        var result=new LinkedHashMap<String,Object>();result.put("status","pass");result.put("renderer","CPU shared indexed meshes with conservative instance frustum culling; not GPU instancing");result.put("image_comparison_pixels",checked);result.put("image_different_pixels",different);result.put("stats",stats);result.put("resolution",List.of(s.width(),s.height()));result.put("samples_per_mode",24);result.put("expanded_median_ms",expanded[12]);result.put("instanced_median_ms",instanced[12]);result.put("expanded_p95_ms",expanded[22]);result.put("instanced_p95_ms",instanced[22]);result.put("limitations",List.of("Headless CPU measurement on this machine; not a target-hardware frame-rate guarantee.","Visible triangles still require transformation, lighting and rasterization.","Existing menus, HUD, settings and input components are unchanged."));
        Files.writeString(Path.of("development/levels/verification/scenery-instancing.json"),DressingReport.json(result)+"\n");
        System.out.printf("PASS instance/reference image parity: %,d/%d pixels differ; %s%nCPU median %.2fms expanded / %.2fms instances%n",different,checked,stats,expanded[12],instanced[12]);
    }
}
