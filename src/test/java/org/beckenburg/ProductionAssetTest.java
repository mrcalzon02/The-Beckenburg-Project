package org.beckenburg;
import java.nio.file.*;
import org.beckenburg.LevelDefinition.*;
public final class ProductionAssetTest {
    static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception{
        var map=BlackpineLevel.map();var l=map.level();int triangles=0;
        for(String id:BlackpineProductionAssets.ASSETS){
            check(LevelAssets.approved(id),"approved runtime model "+id);var mesh=GlbProps.model(id);check(mesh.triangles().size()>50,"real model "+id);triangles+=mesh.triangles().size();
            for(var p:l.props())if(p.mesh().equals(id)){
                for(var t:mesh.triangles())for(var v:new GlbProps.Vertex[]{t.a(),t.b(),t.c()})
                    check(Math.abs(v.x())<=p.halfX()+.002&&Math.abs(v.z())<=p.halfZ()+.002&&v.y()>=-.002&&v.y()<=p.height()+.002,"solid bounds include actual native source "+id);
                check(Math.abs(p.y()-map.groundHeight(p.x(),p.z(),p.room()))<.01,"native floor pivot supported "+id);
                check(!map.traversable(p.x(),p.y(),p.z()),"matching solid body "+id);
                check(l.sockets().stream().filter(s->s.type().equals("ritual")).noneMatch(s->s.room()==p.room()&&Math.abs(s.x()-p.x())<p.halfX()+1.25&&Math.abs(s.z()-p.z())<p.halfZ()+1.25),"outside rite reservations "+id);
            }
        }
        for(var r:l.rooms())check(map.traversable(r.spawn().x(),r.spawn().y(),r.spawn().z()),"all physical spawns clear "+r.name());
        for(var p:l.portals())if(p.door())map.openDoorBetweenRooms(p.a(),p.b());
        for(var s:l.sockets())if(s.type().equals("ritual"))check(map.traversable(s.x(),s.y(),s.z()),"rite centre clear");
        System.out.println("PASS "+BlackpineProductionAssets.ASSETS.size()+" ready runtime production meshes, "+triangles+" shared source triangles, matching collision/support and clear spawns/rites");
    }
}
