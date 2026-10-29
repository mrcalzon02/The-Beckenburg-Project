package org.beckenburg;
import java.util.*;
import java.awt.event.KeyEvent;
import java.nio.file.*;
import org.beckenburg.LevelDefinition.*;
public final class WorldLoreTest {
    static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception{
        check(WorldLore.EMERGENCE.equals("Late 2022"),"user's event date, without invented exact day");
        check(WorldLore.SITES.size()==23&&WorldLore.SITES.stream().map(s->s.id()).distinct().count()==23,"complete atlas coverage");
        for(var role:InvestigationSession.Role.values())check(WorldLore.faction(role).role()==role,"all playable classes have major factions");
        check(WorldLore.FACTIONS.size()==InvestigationSession.Role.values().length,"one faction mapping per class");
        check(WorldLore.MASTER_ART_STATUS.startsWith("No approved master art"),"portrait approval is not full master approval");
        check(CastleArchive.portrait().getWidth()>500,"actual generated portrait packaged for game");
        var model=GlbProps.model("level_npc_matthis_rook_proxy");check(model.triangles().size()>100,"physical original NPC proxy");
        var castle=new CastleHubMap();check(!castle.traversable(CastleArchive.KEEPER_X,CastleArchive.KEEPER_Z),"cannot walk through resident");
        check(castle.traversable(36,13.3)&&castle.aimed(36,13.3,Math.PI)==CastleHubMap.Station.ARCHIVE,"reachable existing east records desk");
        var r=new CastleArchive.Reader();r.open();check(r.opened()&&r.page()==0,"keeper greeting opens first");
        r.key(KeyEvent.VK_RIGHT);check(r.page()==1,"next page");r.scroll(20);r.key(KeyEvent.VK_RIGHT);check(r.scroll()==0,"page turn resets scroll");
        r.key(KeyEvent.VK_W);check(r.opened(),"movement keys do not close archive");
        r.key(KeyEvent.VK_ESCAPE);check(!r.opened(),"escape closes book");r.open();r.key(KeyEvent.VK_LEFT);check(r.page()==WorldLore.PAGES.size()-1,"previous wraps");
        CastleArchive.click(r,450,575,900,600);check(!r.opened(),"visible centre close button works");
        for(var site:List.of(HouseMap.Site.HOUSE,HouseMap.Site.FARMHOUSE,HouseMap.Site.SCHOOL)){
            var map=new HouseMap(site);for(var p:map.level().portals())if(p.door())map.openDoorBetweenRooms(p.a(),p.b());
            var s=map.level().sockets().stream().filter(q->q.type().equals("story")&&q.style().equals("EMERGENCE")).findFirst().orElseThrow();
            var prop=map.level().props().stream().filter(p->p.mesh().equals("level_trace_journal")&&p.room()==s.room()&&Math.hypot(p.x()-s.x(),p.z()-s.z())<.001).findFirst().orElseThrow();
            var support=map.level().props().stream().filter(p->p.room()==s.room()&&p.mesh().equals(prop.support())).findFirst().orElseThrow();
            check(Math.abs(prop.y()-support.y()-.85)<.001,"new survivor record uses actual sideboard");
            boolean aimed=false;
            for(double radius:new double[]{.7,1,1.3,1.6})for(int i=0;i<64&&!aimed;i++){
                double a=i*Math.PI/32,x=s.x()+radius*Math.cos(a),z=s.z()+radius*Math.sin(a),y=map.floorY(s.room());if(!map.traversable(x,y,z))continue;
                var o=new LooseClutter.Observer(0,x,y,z,Math.atan2(s.x()-x,s.z()-z),Math.atan2(s.y()+.035-y-1.62,radius),true);
                var target=InteractionFocus.pick(map,o,List.of());aimed=target!=null&&target.id().equals("STORY_EMERGENCE");
            }
            check(aimed,"survivor note visible and centre-ray readable: "+site);
            check(SiteStories.document(map.level().id(),"EMERGENCE").lines().toString().contains("late 2022"),"same canon at site");
        }
        check(SiteStories.document("blackpine","SHELTER").lines().toString().contains("dead returned in late 2022"),"recent cabin linked to catastrophe");
        System.out.println("PASS 23 site threads, 3 class factions, portrait/proxy contracts, physical archive approach, reader controls and 3 supported ray-readable survivor notes");
        Files.writeString(Path.of("development/levels/verification/world-lore.json"),"{\"status\":\"pass\",\"site_threads\":23,\"class_factions\":3,\"archive_pages\":"+WorldLore.PAGES.size()+",\"in_game_portrait\":true,\"master_art\":\"pending\",\"survivor_notes_ray_readable\":3,\"blackpine_survivor_records\":true}\n");
    }
}
