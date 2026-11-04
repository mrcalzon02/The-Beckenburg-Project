package org.beckenburg;

import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.beckenburg.LevelDefinition.Vec;

/** Real ground-room routes, permanent pantry seal, fitted household assets and preserved stock. */
public final class CastleGroundConstructionTest {
    private static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
    private static String key(double x,double y,double z){return Math.round(x*8)+":"+Math.round(y*10000)+":"+Math.round(z*8);}
    public static void main(String[] args)throws Exception{
        System.setProperty("user.home",Files.createTempDirectory("castle-ground-").toString());
        var scene=CastleLevelEdits.current();var map=new CastleHubMap();
        check(CastleGround.active(),"Fresh native ground programme is not active");
        for(var slot:CastleDoorSlots.all())if(scene.doorType(slot.id())!=CastleDoorSlots.Type.EMPTY)map.toggleDoor(slot.id());
        var seen=new HashSet<String>();var q=new ArrayDeque<Vec>();double[] spawn=scene.spawn();
        seen.add(key(spawn[0],0,spawn[1]));q.add(new Vec(spawn[0],0,spawn[1]));
        var groundRooms=new HashSet<String>();var lowerRooms=new HashSet<String>();
        while(!q.isEmpty()){
            var p=q.removeFirst();
            if(Math.abs(p.y())<.01)for(var r:CastleGroundLayout.ROOMS)if(r.contains(p.x(),p.z()))groundRooms.add(r.id());
            if(Math.abs(p.y()+3)<.01)for(var r:CastleCellarLayout.ROOMS)if(r.contains(p.x(),p.z()))lowerRooms.add(r.id());
            for(double[] delta:new double[][]{{.125,0},{-.125,0},{0,.125},{0,-.125}}){
                double x=p.x()+delta[0],z=p.z()+delta[1],y=CastleHubArchitecturalPlan.elevation(x,z,p.y());
                String k=key(x,y,z);
                if(!seen.contains(k)&&CastleHubPanel.safeElevationTransition(p.y(),y,x,z)&&map.traversable(x,z,y)){
                    seen.add(k);q.add(new Vec(x,y,z));
                }
            }
            check(seen.size()<400000,"Unbounded real ground route flood");
        }
        for(var r:CastleGroundLayout.ROOMS)check(CastleGroundLayout.SEALED.contains(r.id())?!groundRooms.contains(r.id()):groundRooms.contains(r.id()),
            "Wrong actual access state for "+r.id()+" / reached "+groundRooms);
        for(var r:CastleCellarLayout.ROOMS)check(lowerRooms.contains(r.id()),"Ground partition severed cellar "+r.id());
        check(!map.traversable(6.5,32.5)&&!map.traversable(8.5,31),"Pantry has a physical search breach");
        check(!FixtureLighting.castleRayClear(map,6,1.4,32.5,7,1.4,32.5),"Permanent pantry boundary is optically open");
        check(CastleDoorSlots.all().stream().noneMatch(s->s.bottom()==0&&Math.abs(s.centerX()-6.5)<1e-7&&Math.abs(s.centerZ()-32.5)<1e-7),
            "Sealed context has a player-openable portal");
        var stock=CastleGroundLayout.ROOMS.stream().filter(r->r.id().equals("store")).findFirst().orElseThrow();
        check(CastleWarehouse.racks().size()==15,"Stock fixtures discarded");
        for(var e:CastleWarehouse.exhibits()){
            var rack=CastleWarehouse.racks().get(e.rack());double[] stand=rack.world((e.column()-1)*.66,1.15);
            check(map.traversable(stand[0],stand[1]),"Blocked equipment browsing stand "+e.id()+" / "+Arrays.toString(stand));
            boolean reachable=false;long ix=Math.round(stand[0]*8),iz=Math.round(stand[1]*8);
            for(long dx=-2;dx<=2;dx++)for(long dz=-2;dz<=2;dz++)if(seen.contains(key((ix+dx)/8.0,0,(iz+dz)/8.0)))reachable=true;
            check(reachable,"Equipment aisle is disconnected from native spawn "+e.id());
            check(stock.contains(e.x(),e.z()),"Equipment remains in repair/infirmary instead of stock room");
        }
        long fixtures=scene.props().stream().filter(p->p.id().startsWith("ground-wc-")||p.id().startsWith("ground-wash-")
            ||p.id().startsWith("ground-washup-")||p.id().startsWith("ground-kitchen-")||p.id().startsWith("ground-repair-")||p.id().startsWith("ground-infirmary-")).count();
        check(fixtures==14,"Missing source-sized domestic/service fixture");
        for(var p:scene.props())if(p.id().startsWith("ground-")){
            var room=CastleRooms.at(p.x(),0,p.z());check(room!=null&&!CastleGroundLayout.SEALED.contains(room.id()),"Required fixture in sealed/unsupported space");
            double[] b=SceneRenderer.castleBounds(p.mesh());
            for(var t:GlbProps.model(p.mesh()).triangles())for(var v:List.of(t.a(),t.b(),t.c())){
                double dx=(v.x()-b[0])*p.scale(),dz=(v.z()-b[2])*p.scale();
                double x=p.x()+dx*Math.cos(p.yaw())-dz*Math.sin(p.yaw()),z=p.z()+dx*Math.sin(p.yaw())+dz*Math.cos(p.yaw());
                double y=p.y()+(v.y()-b[1])*p.scale(),half=CastleDoorSlots.MASONRY_THICKNESS/2;
                check(x>room.x0()+half&&x<room.x1()-half&&z>room.z0()+half&&z<room.z1()-half&&y>=0&&y<room.ceiling()-CastleRooms.SLAB,
                    "Source mesh crosses room construction: "+p.id()+" / "+x+","+y+","+z);
            }
            check(!map.traversable(p.x(),p.z()),"Fixture lacks real body collision "+p.id());
        }
        var lighting=new FixtureLighting(map);
        for(var r:CastleGroundLayout.ROOMS)if(!CastleGroundLayout.SEALED.contains(r.id())){
            double x=(r.x0()+r.x1())/2,z=(r.z0()+r.z1())/2;
            lighting.castleFrame(false,10);double dark=lighting.sample(0,x,1.4,z).energy();
            lighting.castleFrame(true,10);double lit=lighting.sample(0,x,1.4,z).energy();
            check(lit>dark+.2,"Missing real room lighting "+r.id()+" / "+dark+" -> "+lit);
            var control=CastleHubDecor.SWITCHES.stream().filter(sw->CastleRooms.circuit(sw.x(),sw.y(),sw.z()).equals(r.id())).findFirst().orElseThrow();
            map.toggleCircuit(control);check(!map.lightOn(x,1.4,z),"Room switch controls wrong circuit "+r.id());map.toggleCircuit(control);
        }
        // Inspect actual fixture vertices: wallBlocks is a standing-body query
        // and incorrectly reaches through the ceiling into the next storey.
        for(var lamp:scene.lamps())if(lamp.mount()==CastleHubDecor.Mount.CEILING){
            double[] b=SceneRenderer.castleBounds(lamp.on());
            for(var t:GlbProps.model(lamp.on()).triangles())for(var v:List.of(t.a(),t.b(),t.c())){
                double dx=(v.x()-b[0])*lamp.scale(),dz=(v.z()-b[2])*lamp.scale();
                double x=lamp.x()+dx*Math.cos(lamp.yaw())-dz*Math.sin(lamp.yaw()),z=lamp.z()+dx*Math.sin(lamp.yaw())+dz*Math.cos(lamp.yaw());
                double y=lamp.y()+(v.y()-b[1]-b[4])*lamp.scale(),half=CastleDoorSlots.MASONRY_THICKNESS/2;
                for(var w:scene.walls())if(!w.hasDoor()&&y>w.bottom()&&y<w.top())
                    check(w.horizontal()?!(x>w.x0()&&x<w.x1()&&Math.abs(z-w.z0())<half):!(z>w.z0()&&z<w.z1()&&Math.abs(x-w.x0())<half),
                        "Actual ceiling mesh intersects partition: "+lamp.id());
            }
        }
        for(var sw:CastleHubDecor.SWITCHES){
            double feet=sw.y()<0?-3:sw.y()>4?3.4:0,x=sw.x()-Math.sin(sw.yaw())*.8,z=sw.z()-Math.cos(sw.yaw())*.8;
            check(map.traversable(x,z,feet)&&CastleHubDecor.aimed(map,x,feet,z,sw.yaw())!=null,"Switch inaccessible after room refit "+sw);
        }
        var low=CastleDoorSlots.require("portal-v-3700-2200");var high=CastleDoorSlots.require("portal-v-3700-2200-y0");
        check(low.bottom()==-3&&high.bottom()==0,"Published cellar portal ID changed");
        scene.installDoor(low.id(),CastleDoorSlots.Type.WOOD);scene.installDoor(high.id(),CastleDoorSlots.Type.WOOD);
        check(!map.traversable(37,22,-3)&&!map.traversable(37,22,0),"Storey leaves do not independently block");
        map.toggleDoor(low.id());check(map.traversable(37,22,-3)&&!map.traversable(37,22,0),"Lower door controls upper opening");
        map.toggleDoor(high.id());check(map.traversable(37,22,0),"Upper door cannot open");
        var doorSnapshot=scene.snapshot();scene.save();scene.reopen();
        check(doorSnapshot.equals(scene.snapshot())&&scene.doorType(low.id())==CastleDoorSlots.Type.WOOD
            &&scene.doorType(high.id())==CastleDoorSlots.Type.WOOD,"Published lower/new upper leaf assignments do not roundtrip independently");
        scene.removeDoor(low.id());scene.removeDoor(high.id());
        var snapshot=scene.snapshot();scene.save();scene.reopen();check(snapshot.equals(scene.snapshot()),"Ground native save/reopen differs");
        for(var surface:List.copyOf(scene.surfaces()))if(surface.id().startsWith("ground-wall"))scene.removeSurface(surface.id());
        check(!CastleGround.active()&&CastleRooms.at(25,0,2).id().equals("wardrobe")&&CastleRooms.at(5,0,32).id().equals("practice"),
            "Older native geometry acquires phantom child rooms/circuits");
        check(CastleHubDecor.GROUND_SWITCHES.stream().noneMatch(CastleHubDecor::switchAvailable),"Older native scene receives phantom room controls");
        scene.installDoor(high.id(),CastleDoorSlots.Type.WOOD);if(map.doorOpen(high.id()))map.toggleDoor(high.id());
        check(!CastleDoorSlots.available(high)&&map.traversable(37,22)&&FixtureLighting.castleRayClear(map,36,1.4,22,38,1.4,22),
            "Absent ground partition leaves a phantom body/optical leaf");
        scene.restore(snapshot);check(CastleGround.active(),"Restoring native room structure does not restore ownership");
        var serviceRenderer=new SceneRenderer();serviceRenderer.setRenderSize(1000,625);serviceRenderer.lamp=false;
        var project=new CastleEditorProject(scene);
        var service=new org.beckenburg.editor.EditorAgentService<>(new CastleEditorAgent(project,map,serviceRenderer,()->{}),
            new org.beckenburg.editor.EditorHistory<>(project,60));
        var inspection=service.inspect();
        check(inspection.state().equals(scene.snapshot())&&inspection.hierarchy().stream().anyMatch(n->n.id().equals("ground-infirmary-bed")),
            "Real editor service does not inspect the rebuilt native ground scene");
        var terrainChecks=service.test(scene.revision(),"castle-world-terrain");
        check(terrainChecks.result().stream().allMatch(org.beckenburg.editor.EditorAgentService.Check::passed),"Real editor terrain diagnostics fail after ground refit");
        if(args.length>0){
            Path folder=Path.of(args[0]);Files.createDirectories(folder);var renderer=new SceneRenderer();renderer.setRenderSize(1000,625);renderer.lamp=false;
            capture(renderer,map,folder,"ground-stock-runtime",32.8,22,1.62,0,-.05);
            capture(renderer,map,folder,"ground-kitchen-runtime",4.7,29.6,1.62,0,-.1);
            capture(renderer,map,folder,"ground-washing-runtime",25.6,4.4,1.62,Math.PI,-.05);
            capture(renderer,map,folder,"ground-infirmary-runtime",38,31.2,1.62,2.43,-.05);
            serviceRenderer.x=4.7;serviceRenderer.z=29.6;serviceRenderer.eyeHeight=1.62;serviceRenderer.yaw=0;serviceRenderer.pitch=-.1;
            var capture=service.capture(scene.revision(),folder.resolve("ground-kitchen-runtime.png"));
            check(capture.revision()==scene.revision()&&capture.result().renderer().equals("SceneRenderer.drawCastle"),"Service capture is stale or synthetic");
            Files.writeString(folder.resolve("ground-editor-service.txt"),"Actual EditorAgentService inspect/test/capture\nscene="+inspection.sceneId()+"\nrevision="+capture.revision()
                +"\nrenderer="+capture.result().renderer()+"\n"+terrainChecks.result()+"\n");
        }
        System.out.println("PASS Castle ground construction: "+seen.size()+" actual native-spawn states; all15 open core rooms and4 cellar rooms, permanent pantry seal,15 retained stock racks/usable aisles,14 fitted source fixtures, room lighting/switches, independent vertical door IDs, native save/reopen and actual editor service inspection/terrain diagnostics/capture.");
    }
    private static void capture(SceneRenderer r,CastleHubMap map,Path folder,String name,double x,double z,double eye,double yaw,double pitch)throws Exception{
        r.x=x;r.z=z;r.eyeHeight=eye;r.yaw=yaw;r.pitch=pitch;r.drawCastle(map,0,true);ImageIO.write(r.image(),"png",folder.resolve(name+".png").toFile());
    }
}
