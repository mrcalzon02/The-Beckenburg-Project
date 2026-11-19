package org.beckenburg;

import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;

/** Actual runtime frame coverage for every currently loadable built-in source.
 * Source-only rebuild packets inherit the common renderer, remain locked, and
 * are never misreported as loaded/playable by this diagnostic.
 */
public final class AllLevelAtmosphereCapture {
    private static boolean courtyardOnly;
    private static void render(SceneRenderer scene,HouseMap map,CastleHubMap castle)throws Exception{
        if(castle!=null)scene.drawCastle(castle,0,true);else scene.drawNetwork(map,List.of(),-1,0);
        if(GpuWorldRenderer.active()!=null){
            if(!scene.awaitSubmittedGpuPrewarm(30_000_000_000L,()->false))throw new AssertionError("No actual completed OpenGL frame");
            var result=GpuWorldRenderer.active().latest();scene.pinCompletedGpuFrame(result);
            if(castle!=null)scene.drawCastle(castle,0,true);else scene.drawNetwork(map,List.of(),-1,0);
        }
    }
    private static void capture(String id,HouseMap map,CastleHubMap castle,Path out,StringBuilder csv)throws Exception{
        var scene=new SceneRenderer();scene.setRenderSize(640,400);scene.setPsxPreset(PsxPipeline.Preset.OFF);scene.lamp=false;
        if(castle!=null){
            scene.x=courtyardOnly?15.5:24;scene.z=courtyardOnly?27:22;scene.eyeHeight=1.62;scene.yaw=courtyardOnly?0:-Math.PI/2;
            if(courtyardOnly&&(!castle.outside(scene.x,scene.z)||!castle.traversable(scene.x,scene.z)))throw new AssertionError("Courtyard camera is not a clear exterior position");
        }
        else{
            boolean found=false;
            for(double side:new double[]{-4,4,-6,6})for(double along:new double[]{0,-4,4,7}){
                if(found)continue;var p=map.van().world(side,along);int room=map.roomAt(p.x(),p.z());double y=map.groundHeight(p.x(),p.z(),room);
                if(!Double.isFinite(y)||!map.traversable(p.x(),y,p.z()))continue;
                scene.x=p.x();scene.z=p.z();scene.eyeHeight=y+1.62;scene.yaw=Math.atan2(map.centerX(0)-scene.x,map.centerZ(0)-scene.z);found=true;
            }
            if(!found)throw new AssertionError("No supported arrival camera for "+id);
        }
        for(String view:List.of("toward-site","outward-sky")){
            if(view.equals("outward-sky"))scene.yaw+=Math.PI;
            scene.pitch=.05;render(scene,map,castle);
            var a=scene.atmosphereFrame();if(a.opaqueMetres()>24||a.clearMetres()!=6)throw new AssertionError("Atmosphere missing for "+id);
            ImageIO.write(scene.image(),"png",out.resolve(id.toLowerCase(Locale.ROOT)+"-"+view+".png").toFile());
            csv.append(id).append(',').append(view).append(',').append(GpuWorldRenderer.active()==null?"software":"actual-OpenGL-composite").append(',').append(a.foggedPixels()).append(',').append(a.skyPixels()).append(',').append(a.opaqueMetres()).append(',').append(scene.x).append(',').append(scene.eyeHeight).append(',').append(scene.z).append('\n');
        }
        System.out.println("ATMOSPHERE_RUNTIME "+id+" / "+(GpuWorldRenderer.active()==null?"software":"actual OpenGL"));
    }
    public static void main(String[] args)throws Exception{
        Path out=Path.of(args.length==0?"build/all-level-atmosphere":args[0]);Files.createDirectories(out);StringBuilder csv=new StringBuilder("site,view,renderer,fogged_pixels,sky_pixels,opaque_metres,camera_x,camera_y,camera_z\n");
        courtyardOnly=args.length>1&&args[1].equals("--courtyard-only");
        if(courtyardOnly){capture("CASTLE-COURTYARD",null,new CastleHubMap(),out,csv);Files.writeString(out.resolve("courtyard-coverage.csv"),csv);System.out.println("PASS2actual supported exterior Castle courtyard frames");return;}
        for(HouseMap.Site site:HouseMap.Site.values())if(site!=HouseMap.Site.USER)capture(site.name(),new HouseMap(site),null,out,csv);
        capture("WLD-01",BlackpineLevel.map(),null,out,csv);capture("CASTLE",null,new CastleHubMap(),out,csv);
        Files.writeString(out.resolve("runtime-coverage.csv"),csv);System.out.println("PASS12runtime sources /24actual frames; unintegrated packets inherit shared policy, not claimed as runtime maps");
    }
}
