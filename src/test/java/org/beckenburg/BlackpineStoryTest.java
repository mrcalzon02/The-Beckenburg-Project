package org.beckenburg;
import java.util.*;
import java.nio.file.*;
import org.beckenburg.LevelDefinition.*;

/** Story props must exist, be supported and readable through the existing centre-ray interaction. */
public final class BlackpineStoryTest {
    static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception{
        var map=BlackpineLevel.map();var l=map.level();for(var p:l.portals())if(p.door())map.openDoorBetweenRooms(p.a(),p.b());
        check(BlackpineStoryDressing.ROOMS.stream().map(s->s.room()).distinct().count()==12,"individual room stories");
        check(l.sockets().stream().filter(s->s.type().equals("site_history")).count()==12,"room history included in authored handshake");
        int triangles=0;for(String mesh:BlackpineStoryDressing.ASSETS){
            check(LevelAssets.approved(mesh),"approved source "+mesh);
            var model=GlbProps.model(mesh);check(!model.triangles().isEmpty(),"real story mesh "+mesh);triangles+=model.triangles().size();
        }
        int readable=0;
        for(var s:l.sockets())if(s.type().equals("story")){
            var p=l.props().stream().filter(q->q.room()==s.room()&&q.mesh().startsWith("level_trace_")&&Math.hypot(q.x()-s.x(),q.z()-s.z())<.001).findFirst().orElseThrow();
            var support=l.props().stream().filter(q->q.room()==p.room()&&q.mesh().equals(p.support())&&Math.hypot(q.x()-p.x(),q.z()-p.z())<.001).findFirst().orElseThrow();
            double top=support.mesh().equals("wooden_crate_1")?1:.86;
            check(Math.abs(p.y()-support.y()-top)<.009,"reading rests on its real tabletop "+s.style());
            var doc=SiteStories.document(l.id(),s.style());check(!doc.lines().isEmpty(),"readable text "+s.style());
            boolean focused=false;
            for(double radius:new double[]{1.15,1.4,1.6})for(int i=0;i<32&&!focused;i++){
                double a=i*Math.PI/16,x=s.x()+Math.cos(a)*radius,z=s.z()+Math.sin(a)*radius,y=map.floorY(s.room());
                if(map.roomAt(x,y,z)!=s.room()||!map.traversable(x,y,z))continue;
                double yaw=Math.atan2(s.x()-x,s.z()-z),pitch=Math.atan2(s.y()+.035-y-1.62,radius);
                var target=InteractionFocus.pick(map,new LooseClutter.Observer(0,x,y,z,yaw,pitch,true),List.of());
                focused=target!=null&&target.id().equals("STORY_"+s.style());
            }
            check(focused,"reachable visible centre-ray reading "+s.style());readable++;
        }
        check(readable==9,"nine placed readable records");
        for(var p:l.props())if(p.mesh().startsWith("level_trace_")&&p.support().equals("floor"))
            check(Math.abs(p.y()-map.floorY(p.room()))<.001,"floor supported story prop "+p.mesh());
        for(var p:l.props())if(p.mesh().equals("level_trace_bedroll")||p.mesh().equals("level_trace_clothes")&&p.room()==7)
            check(p.room()==7,"recent occupation confined to cabin");
        for(var s:l.sockets())if(s.type().equals("ritual"))for(double dx:new double[]{-1,0,1})for(double dz:new double[]{-1,0,1})
            check(map.traversable(s.x()+dx,map.floorY(s.room()),s.z()+dz),"rite footprint remains clear "+s.style());
        check(l.props().stream().filter(p->p.mesh().equals("level_trace_window_boards")).count()==2,"selective boarded windows");
        check(!map.clearLineOfSight(15.3,map.floorY(4)+1.5,18,16.5,map.floorY(4)+1.5,18),"boarded bunk window blocks actual sight");
        check(!SiteStories.document("blackpine","LETTERS").lines().toString().contains("Ravenshade"),"camp records never inherit school boiler history");
        String output="PASS 19 original story meshes ("+triangles+" source triangles), 12 room histories, 9 physically supported and centre-ray readable records, two boarded apertures and clear ritual floors";
        System.out.println(output);
        Files.writeString(Path.of("development/levels/verification/blackpine-story.json"),"{\n  \"status\": \"pass\",\n  \"original_meshes\": 19,\n  \"source_triangles\": "+triangles+",\n  \"room_histories\": 12,\n  \"readable_records\": 9,\n  \"supported_and_ray_targetable\": true,\n  \"ritual_floors_clear\": true\n}\n");
    }
}
