package org.beckenburg;

import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.beckenburg.editor.*;

/** Shared workbench IDs, generated component atlas and real retained Castle placements. */
public final class WorkbenchMaterialTest {
    private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception{
        System.setProperty("user.home",Files.createTempDirectory("workbench-material-").toString());
        for(String suffix:new String[]{"","_worn"}){
            String id="level_workbench"+suffix;var model=GlbProps.model(id);var b=SceneRenderer.castleBounds(id);
            check(model.triangles().size()==688,"Shared bench budget changed");
            check(Math.abs(b[3]-1.8)<1e-6&&Math.abs(b[4]-1.22)<1e-6&&Math.abs(b[5]-.705)<1e-6,"Shared bench bounds changed");
            check(LevelAssets.AUTHORED_SURFACES.contains(id)&&GlbProps.availableModels().contains(id),"Shared native source/UV registration missing");
            byte[] bytes=Files.readAllBytes(Path.of("assets/runtime/models/"+id+".glb"));
            check(Arrays.equals(bytes,Files.readAllBytes(Path.of("development/assets/castle-training/level_castle_preparation_bench"+suffix+".glb"))),"Runtime model differs from inspected export");
            var source=EditorGlbReader.read(bytes);check(source.materials().size()==1,"Generic flat material layers retained");
            var image=ImageIO.read(new java.io.ByteArrayInputStream(source.materials().get(0).png()));check(image.getWidth()==512&&image.getHeight()==512,"Missing budget-sized generated atlas");
            for(double[] uv:new double[][]{{.2,.1},{.4,.3},{.6,.1},{.6,.4},{.9,.2},{.3,.8}}){
                int expected=image.getRGB((int)(uv[0]*512),(int)(uv[1]*512))&0xffffff;
                check(GlbProps.sample(model.triangles().get(0).material(),uv[0],uv[1])==expected,"Workbench atlas origin is reversed");
            }
        }
        var scene=CastleLevelEdits.current();var map=new CastleHubMap();
        var benches=scene.props().stream().filter(p->p.id().equals("ground-repair-bench")||p.id().equals("ground-repair-preparation")).toList();
        check(benches.size()==2,"Existing Castle bench IDs/placements replaced or duplicated");
        for(var p:benches){
            check(p.scale()==1&&p.y()==0,"Workbench floated or rescaled for new materials");
            boolean worn=p.id().equals("ground-repair-bench");
            check(p.mesh().equals(worn?"level_workbench_worn":"level_workbench"),"Existing repair bench source ID replaced");
            check(p.x()==(worn?38.1:41.7)&&p.z()==(worn?18.7:25)&&p.yaw()==(worn?Math.PI/2:0),"Existing repair bench pose changed");
            var room=CastleRooms.at(p.x(),0,p.z());check(room!=null&&room.id().equals("repair"),"Workbench left its real repair room");
        }
        var snapshot=scene.snapshot();scene.save();scene.reopen();check(snapshot.equals(scene.snapshot()),"Shared old-ID bench does not survive native save/reopen");
        if(args.length>0){
            Path out=Path.of(args[0]);Files.createDirectories(out);var r=new SceneRenderer();r.setRenderSize(1000,625);r.lamp=false;
            var project=new CastleEditorProject(scene);var service=new EditorAgentService<>(new CastleEditorAgent(project,map,r,()->{}),new EditorHistory<>(project,60));
            double[][] views={{41.5,22,1.62,Math.atan2(.2,3),-.22},{37.6,21.5,1.62,Math.atan2(.5,-2.8),-.22}};
            for(int i=0;i<views.length;i++){
                var v=views[i];check(map.traversable(v[0],v[1],0),"Bench review camera lacks clearance");r.x=v[0];r.z=v[1];r.eyeHeight=v[2];r.yaw=v[3];r.pitch=v[4];
                check(service.capture(scene.revision(),out.resolve("workbench-material-"+i+"-runtime.png")).result().renderer().equals("SceneRenderer.drawCastle"),"Synthetic workbench capture");
            }
        }
        System.out.println("PASS shared688tri clean/worn workbenches, original bounds/IDs and Castle poses,512px generated component atlas/orientation, actual editor captures and native save/reopen");
    }
}
