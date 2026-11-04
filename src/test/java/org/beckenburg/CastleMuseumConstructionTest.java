package org.beckenburg;

import java.nio.file.*;
import java.util.*;
import org.beckenburg.editor.*;

/** Native museum exhibit, source-size placement, retained access and actual editor captures. */
public final class CastleMuseumConstructionTest {
    private static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
    public static void main(String[] args)throws Exception{
        System.setProperty("user.home",Files.createTempDirectory("castle-museum-").toString());
        var scene=CastleLevelEdits.current();var map=new CastleHubMap();
        var exhibits=scene.props().stream().filter(p->p.id().startsWith("museum-")).toList();
        check(exhibits.size()==3,"Museum armour and original caption missing from native scene");
        for(var p:exhibits){
            check(GlbProps.availableModels().contains(p.mesh()),"Native editor cannot select exhibit source");
            int expected=p.mesh().endsWith("_label")?86:p.mesh().endsWith("_rope")?264:1036;
            check(GlbProps.model(p.mesh()).triangles().size()==expected,"Exhibit exceeds measured budget");
            check(p.scale()==1&&p.y()==0,"Exhibit shrunk or floated to force fit");
            var room=CastleRooms.at(p.x(),p.y(),p.z());check(room!=null&&room.id().equals("archive"),"Exhibit outside retained museum gallery");
            var bounds=SceneRenderer.castleBounds(p.mesh());
            for(var t:GlbProps.model(p.mesh()).triangles())for(var v:List.of(t.a(),t.b(),t.c())){
                double dx=v.x()-bounds[0],dz=v.z()-bounds[2],x=p.x()+dx*Math.cos(p.yaw())-dz*Math.sin(p.yaw()),z=p.z()+dx*Math.sin(p.yaw())+dz*Math.cos(p.yaw()),y=p.y()+v.y()-bounds[1];
                check(x>room.x0()+.21&&x<room.x1()-.21&&z>room.z0()+.21&&z<room.z1()-.21&&y>=0&&y<room.ceiling()-CastleRooms.SLAB,"Exhibit mesh penetrates gallery construction "+p.id());
            }
            var floors=CastleRooms.slabCells(CastleRooms.structuralDecks());
            for(double dx:new double[]{-p.halfX(),0,p.halfX()})for(double dz:new double[]{-p.halfZ(),0,p.halfZ()})
                check(CastleRooms.supported(floors,p.x()+dx,p.z()+dz,0),"Exhibit footprint has no shared structural floor");
            check(CastleRooms.slabBodyBlocks(p.x(),-.3,p.z(),.01,.25),"Exhibit floor lacks physical slab depth");
            check(!FixtureLighting.castleRayClear(map,p.x(),.1,p.z(),p.x(),-.3,p.z()),"Exhibit floor lacks optical slab depth");
            check(!map.traversable(p.x(),p.z()),"Museum plinth/stand lacks body collision");
            for(var q:scene.props())if(!q.id().equals(p.id())){
                double qHeight=SceneRenderer.castleBounds(q.mesh())[4]*q.scale();
                if(q.y()+qHeight<=p.y()||p.y()+bounds[4]<=q.y())continue;
                check(Math.abs(p.x()-q.x())>=p.halfX()+q.halfX()||Math.abs(p.z()-q.z())>=p.halfZ()+q.halfZ(),"Exhibit overlaps retained native furniture "+p.id()+" / "+q.id());
            }
        }
        check(map.traversable(31.5,7.5)&&map.traversable(32.3,6.85),"Display/label approach blocked");
        check(CastleArchive.aimedKeeper(map,36,9,0),"Museum placement blocks the existing Lore Master");
        var snapshot=scene.snapshot();scene.save();scene.reopen();check(snapshot.equals(scene.snapshot()),"Museum native save/reopen mismatch");
        var renderer=new SceneRenderer();renderer.setRenderSize(1000,625);renderer.lamp=false;
        var project=new CastleEditorProject(scene);var service=new EditorAgentService<>(new CastleEditorAgent(project,map,renderer,()->{}),new EditorHistory<>(project,60));
        check(service.inspect().hierarchy().stream().anyMatch(n->n.id().equals("museum-armour-archive")),"Editor agent does not inspect actual native exhibit");
        if(args.length>0){
            var folder=Path.of(args[0]);Files.createDirectories(folder);
            double[][] views={{31.5,8.1,1.62,Math.PI,-.05},{32.3,6.85,1.18,Math.PI,-.20},{34,8,1.62,-2.356,-.05}};
            String[] names={"museum-armour-runtime","museum-caption-runtime","museum-gallery-runtime"};
            for(int i=0;i<views.length;i++){
                var v=views[i];renderer.x=v[0];renderer.z=v[1];renderer.eyeHeight=v[2];renderer.yaw=v[3];renderer.pitch=v[4];
                var capture=service.capture(scene.revision(),folder.resolve(names[i]+".png"));
                check(capture.revision()==scene.revision()&&capture.result().renderer().equals("SceneRenderer.drawCastle"),"Stale/synthetic museum capture");
            }
        }
        System.out.println("PASS Castle museum native armour1036/caption86/barrier264 triangles, source-size floor/contact placement, fitted mesh clearance, preserved furnishings/keeper/routes, native save/reopen and actual editor inspect/capture");
    }
}
