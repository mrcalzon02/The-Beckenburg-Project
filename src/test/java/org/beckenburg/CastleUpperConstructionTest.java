package org.beckenburg;

import java.nio.file.*;
import java.util.*;
import org.beckenburg.LevelDefinition.Vec;

/** Real native-spawn upper routes, household clearance, physical casements and saved fixtures. */
public final class CastleUpperConstructionTest {
    private static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
    private static String key(double x,double y,double z){return Math.round(x*8)+":"+Math.round(y*10000)+":"+Math.round(z*8);}
    public static void main(String[] args)throws Exception{
        System.setProperty("user.home",Files.createTempDirectory("castle-upper-").toString());
        var scene=CastleLevelEdits.current();var map=new CastleHubMap();
        check(CastleUpper.active(),"Native upper programme absent");
        for(var slot:CastleDoorSlots.all())if(scene.doorType(slot.id())!=CastleDoorSlots.Type.EMPTY)map.toggleDoor(slot.id());
        var seen=new HashSet<String>();var q=new ArrayDeque<Vec>();double[] spawn=scene.spawn();
        seen.add(key(spawn[0],0,spawn[1]));q.add(new Vec(spawn[0],0,spawn[1]));var reached=new HashSet<String>();
        while(!q.isEmpty()){
            var p=q.removeFirst();var room=CastleRooms.at(p.x(),p.y(),p.z());if(room!=null)reached.add(room.id());
            for(double[] d:new double[][]{{.125,0},{-.125,0},{0,.125},{0,-.125}}){
                double x=p.x()+d[0],z=p.z()+d[1],y=CastleHubArchitecturalPlan.elevation(x,z,p.y());String k=key(x,y,z);
                if(!seen.contains(k)&&CastleHubPanel.safeElevationTransition(p.y(),y,x,z)&&map.traversable(x,z,y)){
                    seen.add(k);q.add(new Vec(x,y,z));
                }
            }
            check(seen.size()<400000,"Unbounded native route flood");
        }
        for(var r:CastleUpperLayout.ROOMS)check(CastleUpperLayout.SEALED.contains(r.id())?!reached.contains(r.id()):reached.contains(r.id()),"Upper access state "+r.id()+" / "+reached);
        var sealed=new HashSet<>(CastleGroundLayout.SEALED);sealed.addAll(CastleUpperLayout.SEALED);
        var expected=new HashSet<String>();for(var r:CastleRooms.ALL)if(!sealed.contains(r.id()))expected.add(r.id());
        for(var r:CastleAnnex.SPACES)expected.add(r.id());for(var r:CastleCellarLayout.ROOMS)expected.add(r.id());
        check(expected.size()==39&&reached.equals(expected),"Whole-site accessible-space cap/access mismatch: expected "+expected+" reached "+reached);
        for(var sw:CastleUpper.SWITCHES){
            String circuit=CastleRooms.circuit(sw.x(),sw.y(),sw.z());map.toggleCircuit(sw);
            for(var r:CastleUpperLayout.ROOMS)if(!CastleUpperLayout.SEALED.contains(r.id())){
                double x=(r.x0()+r.x1())/2,z=(r.z0()+r.z1())/2;String owner=r.outside()?"ramparts":r.id();
                check(map.lightOn(x,5.0,z)!=owner.equals(circuit),"Upper circuit affects wrong room "+circuit+" / "+r.id());
            }
            check(map.lightOn(13.5,1.5,5),"Upper switch disables ground contract hall");map.toggleCircuit(sw);
        }
        check(scene.props().stream().filter(p->p.id().startsWith("upper-bed-")).count()==6,"Six lodging beds required");
        check(scene.props().stream().filter(p->p.id().startsWith("upper-")).count()==27,"Missing native household fixture");
        for(var p:scene.props())if(p.id().startsWith("upper-")){
            var r=CastleRooms.at(p.x(),p.y(),p.z());check(r!=null&&!CastleUpperLayout.SEALED.contains(r.id()),"Fixture in absent/sealed room "+p.id());
            var b=SceneRenderer.castleBounds(p.mesh());double half=CastleDoorSlots.MASONRY_THICKNESS/2;
            for(var t:GlbProps.model(p.mesh()).triangles())for(var v:List.of(t.a(),t.b(),t.c())){
                double dx=(v.x()-b[0])*p.scale(),dz=(v.z()-b[2])*p.scale();
                double x=p.x()+dx*Math.cos(p.yaw())-dz*Math.sin(p.yaw()),z=p.z()+dx*Math.sin(p.yaw())+dz*Math.cos(p.yaw()),y=p.y()+(v.y()-b[1])*p.scale();
                check(x>r.x0()+half&&x<r.x1()-half&&z>r.z0()+half&&z<r.z1()-half&&y>=3.4&&y<r.ceiling()-CastleRooms.SLAB,"Actual upper mesh crosses construction "+p.id()+" / "+x+","+y+","+z);
            }
            check(!map.traversable(p.x(),p.z(),3.4),"Upper fixture lacks collision "+p.id());
        }
        check(!map.traversable(9,15,4),"Upper floor permits hovering");
        check(scene.lamps().stream().filter(l->l.id().startsWith("upper-light-")).count()==CastleUpper.LIGHT_COUNT,"Upper lighting count");
        for(var l:scene.lamps())if(l.id().startsWith("upper-light-"))check(l.fixture().attached(map),"Unsupported upper lamp "+l.id());
        for(var sw:CastleUpper.SWITCHES){
            double x=sw.x()-Math.sin(sw.yaw())*.8,z=sw.z()-Math.cos(sw.yaw())*.8;
            check(map.traversable(x,z,3.4)&&CastleHubDecor.aimed(map,x,3.4,z,sw.yaw())!=null,"Unusable upper control "+sw);
        }
        var upperDoor=CastleDoorSlots.require("portal-v-1050-650-y340");
        check(upperDoor.bottom()==3.4&&CastleDoorSlots.require("portal-v-1050-650").bottom()==0,"Published ground portal identity changed");
        scene.installDoor(upperDoor.id(),CastleDoorSlots.Type.WOOD);
        check(!map.traversable(10.5,6.5,3.4)&&!FixtureLighting.castleRayClear(map,10,4.8,6.5,11,4.8,6.5),"Closed upper leaf lacks body/optical collision");map.toggleDoor(upperDoor.id());
        check(map.traversable(10.5,6.5,3.4),"Open upper library connection blocked");
        for(var w:CastleUpperLayout.WINDOWS){
            double z=(w.z0()+w.z1())/2,y=(w.sill()+w.head())/2;
            check(FixtureLighting.castleRayClear(map,w.x()-.5,y,z,w.x()+.5,y,z),"Casement is a painted solid wall "+w.id());
            check(!map.traversable(w.x(),z,3.4),"Casement sill permits body exit "+w.id());
        }
        var snapshot=scene.snapshot();
        for(var surface:List.copyOf(scene.surfaces()))if(surface.id().startsWith("upper-floor-"))scene.removeSurface(surface.id());
        check(!CastleUpper.active()&&!CastleDoorSlots.available(upperDoor)&&CastleUpper.SWITCHES.stream().noneMatch(CastleHubDecor::switchAvailable),"Absent native programme retains phantom doors/controls");
        check(CastleDoorSlots.windows().size()==2&&CastleRooms.at(9,3.4,15)==null,"Absent programme retains upper room/window metadata");
        scene.restore(snapshot);
        var renderer=new SceneRenderer();renderer.setRenderSize(1000,625);renderer.lamp=false;
        var project=new CastleEditorProject(scene);
        var service=new org.beckenburg.editor.EditorAgentService<>(new CastleEditorAgent(project,map,renderer,()->{}),new org.beckenburg.editor.EditorHistory<>(project,60));
        check(service.inspect().hierarchy().stream().anyMatch(n->n.id().equals("upper-bed-0")),"Actual editor service cannot inspect lodging assets");
        check(service.test(scene.revision(),"castle-world-terrain").result().stream().allMatch(org.beckenburg.editor.EditorAgentService.Check::passed),"Actual terrain diagnostics fail after upper revision");
        if(args.length>0){
            var folder=Path.of(args[0]);Files.createDirectories(folder);
            double[][] views={{7.0,15,5.02,-Math.PI/2,-.15},{9,11,5.02,Math.PI,-.03},{6.4,6,5.02,Math.PI,-.12},{11.5,18,5.02,0,-.05},{15.1,30.9,5.02,Math.PI/2,-.05}};
            String[] names={"upper-lodging-runtime","upper-hall-runtime","upper-wash-runtime","upper-west-walk-runtime","upper-south-walk-runtime"};
            for(int i=0;i<views.length;i++){
                var v=views[i];renderer.x=v[0];renderer.z=v[1];renderer.eyeHeight=v[2];renderer.yaw=v[3];renderer.pitch=v[4];
                var capture=service.capture(scene.revision(),folder.resolve(names[i]+".png"));
                check(capture.revision()==scene.revision()&&capture.result().renderer().equals("SceneRenderer.drawCastle"),"Stale or synthetic upper capture");
            }
            Files.writeString(folder.resolve("upper-editor-service.txt"),"Actual EditorAgentService inspect/test/capture; native revision="+scene.revision()+"; SceneRenderer.drawCastle; lamp=false\n");
        }
        scene.save();var reopened=new CastleLevelEdits(scene.path());
        check(reopened.snapshot().props().equals(scene.snapshot().props()),"Upper props changed on reopen");
        var older=new Properties();try(var input=Files.newInputStream(scene.path())){older.load(input);}
        older.remove("upper.layout");older.setProperty("lamps.count","17");
        try(var output=Files.newOutputStream(scene.path())){older.store(output,"older upper native programme");}
        check(new CastleLevelEdits(scene.path()).lamps().stream().filter(l->l.id().startsWith("upper-light-")).count()==CastleUpper.LIGHT_COUNT,"Supported old upper fixtures did not migrate");
        scene.reopen();scene.removeLamp("upper-light-lodging-1-0");scene.save();
        check(new CastleLevelEdits(scene.path()).lamps().stream().noneMatch(l->l.id().equals("upper-light-lodging-1-0")),"Deleted upper lamp resurrected");
        System.out.println("PASS Castle upper construction: "+seen.size()+" actual native-spawn states, open upper rooms, sealed context, six source-sized beds/27 fixtures, supported lights, controls, actual casements, independent storey door and native persistence");
    }
}
