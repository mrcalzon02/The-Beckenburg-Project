package org.beckenburg;
import java.util.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;

public final class LegacySiteStoryTest {
    static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception{
        var out=Path.of("development/levels/previews/site-case-files");Files.createDirectories(out);
        Set<Integer> sheets=new HashSet<>();int records=0;
        for(var site:List.of(HouseMap.Site.HOUSE,HouseMap.Site.FARMHOUSE,HouseMap.Site.SCHOOL)){
            var map=new HouseMap(site);var level=map.level();String id=level.id();
            check(LegacySiteStories.rooms(id).size()==12,"every indoor room has authored history");
            check(level.sockets().stream().filter(s->s.type().equals("site_history")).count()==12,"history in authoritative map data");
            check(!SiteCaseFiles.forSite(id).name().equalsIgnoreCase(id),"proper case name");
            check(SiteCaseFiles.forSite(id).history().toString().contains("2022"),"shared Emergence chronology");
            check(sheets.add(SiteCaseFiles.material(map)),"clipboard changes with site");
            ImageIO.write(SiteCaseFiles.paper(id),"png",out.resolve(id+"-physical-clipboard.png").toFile());
            for(var p:level.portals())if(p.door())map.openDoorBetweenRooms(p.a(),p.b());
            for(var s:level.sockets())if(s.type().equals("story")&&s.style().startsWith("ROOM_")){
                records++;var prop=level.props().stream().filter(p->p.room()==s.room()&&Math.hypot(p.x()-s.x(),p.z()-s.z())<.001&&p.mesh().startsWith("level_trace_")).findFirst().orElseThrow();
                check(level.props().stream().anyMatch(p->p.room()==s.room()&&p.mesh().equals(prop.support())&&Math.hypot(p.x()-s.x(),p.z()-s.z())<.3),"new records use actual supports");
                boolean aimed=false;
                for(double radius:new double[]{.7,1,1.3,1.6})for(int i=0;i<96&&!aimed;i++){
                    double a=i*Math.PI/48,x=s.x()+radius*Math.cos(a),z=s.z()+radius*Math.sin(a),y=map.floorY(s.room());if(!map.traversable(x,y,z))continue;
                    var observer=new LooseClutter.Observer(0,x,y,z,Math.atan2(s.x()+.09*Math.cos(prop.yaw())-x,s.z()+.09*Math.sin(prop.yaw())-z),Math.atan2(s.y()+.015-y-1.62,radius),true);
                    var target=InteractionFocus.pick(map,observer,List.of());aimed=target!=null&&target.id().equals("STORY_"+s.style());
                }
                check(aimed,id+" record must be ray readable: "+s.style());
            }
            var reader=new SiteCaseFiles.Reader();reader.key(KeyEvent.VK_RIGHT);check(reader.page==1,"history page");
            reader.key(KeyEvent.VK_DOWN);check(reader.scroll==1,"scroll");reader.key(KeyEvent.VK_RIGHT);check(reader.page==2&&reader.scroll==0,"turn resets scroll");
            for(int width:new int[]{640,1120})for(int page=0;page<3;page++){
                reader.page=page;var im=new BufferedImage(width,width==640?360:700,BufferedImage.TYPE_INT_RGB);var g=im.createGraphics();reader.draw(g,width,im.getHeight(),map,List.of("[OPEN] Return to the van after cleansing"));g.dispose();
                ImageIO.write(im,"png",out.resolve(id+"-case-"+page+"-"+width+".png").toFile());
            }
            // Inspect actual van geometry: exactly the paper front gets a bounded texture.
            var scene=new SceneRenderer();var faces=new ArrayList<>();var method=SceneRenderer.class.getDeclaredMethod("vanModel",List.class,VanStaging.class,Set.class,HouseMap.class);method.setAccessible(true);method.invoke(scene,faces,map.van(),Set.of(),map);
            int paper=0;for(var face:faces)if((int)LevelSceneSource.read(face,"material")==SiteCaseFiles.material(map)){
                paper++;for(String uv:List.of("au","av","bu","bv","cu","cv")){double v=(double)LevelSceneSource.read(face,uv);check(v>.019&&v<.981,"case texture stays inside existing paper");}
            }
            check(paper==2,"case sheet is on original two paper front triangles");
        }
        check(records>=15,"substantial supported narrative dressing across all three maps");
        System.out.println("PASS 36 room histories, "+records+" new ray-readable supported records, 3 distinct physical clipboard sheets, 18 responsive case previews");
        Files.writeString(Path.of("development/levels/verification/legacy-site-stories.json"),"{\"status\":\"pass\",\"room_histories\":36,\"new_readable_records\":"+records+",\"site_clipboards\":3,\"responsive_case_previews\":18}\n");
    }
}
