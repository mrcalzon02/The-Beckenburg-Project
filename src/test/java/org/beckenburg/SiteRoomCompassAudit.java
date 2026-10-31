package org.beckenburg;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.imageio.ImageIO;

/**
 * Advisory first-principles architecture audit, directly from the running
 * SceneRenderer and authoritative 3D level geometry. Each known playable room
 * receives north/east/south/west PNGs from its geometric centre (or the nearest
 * physically traversable point, with the displacement recorded). No generated
 * illustration or substitute room model is accepted as a camera capture.
 *
 * All structural deficiencies are observations; the audit never edits maps,
 * source textures, spawn points, evidence, authored stairs, or the 15-zone
 * multiplayer protocol. Visual and semantic observations never block release.
 */
public final class SiteRoomCompassAudit {
    private SiteRoomCompassAudit(){}
    private static final String[] FACING={"north","east","south","west"};
    private static final double[] HEADING={Math.PI,Math.PI/2,0,-Math.PI/2};
    // Camera north is world -Z; player yaw=0 faces +Z.
    private static final String[] RUBRIC={
        "HOUSE|foyer,living,kitchen,dining,bedroom,bathroom,cellar",
        "FARMHOUSE|mudroom,kitchen,bedroom,barn,hayloft,cellar,shed",
        "SCHOOL|reception,classroom,laboratory,gymnasium,cafeteria,library,nurse,student toilets,changing rooms,accessible washroom",
        "MANSION|grand hall,formal dining,library,ballroom,servant access,kitchen,master suite,estate service court",
        "PRISON|sally port,intake,guard station,cell block,secure gallery,isolation,infirmary,visitation,mess,showers,secure yard,perimeter control",
        "ASYLUM|admissions,ward,day room,treatment,seclusion,hydrotherapy,staff facilities,patient toilets,laundry",
        "ACADEMY|cloister,lecture,laboratory,dining,library,dormitory,staff offices,quadrangle",
        "COLLEGE|administration,classroom,laboratory,lecture theatre,library,student union,residence,service access",
        "CHURCH|nave,vestibule,chancel,side chapel,vestry,choir,crypt,belfry",
        "HOLLOW_CREEK|camping,trail,lake,ranger,services,parking,shoreline"
    };
    private static String escape(String s){
        return s==null?"":s.replace("\r"," ").replace("\n"," ").replace(",",";")
            .replace('"','\'');
    }
    private static String file(String s){
        return s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+","-")
            .replaceAll("(^-|-$)","");
    }
    private static String fmt(double value){return String.format(Locale.ROOT,"%.2f",value);}
    private static int[] connectivity(HouseMap map){
        Set<Integer> reached=new HashSet<>();
        ArrayDeque<Integer> todo=new ArrayDeque<>();
        todo.add(HouseMap.EXTRACTION_ROOM);
        while(!todo.isEmpty()){
            int at=todo.removeFirst();
            if(!reached.add(at))continue;
            todo.addAll(map.adjacentRooms(at));
        }
        return reached.stream().mapToInt(Integer::intValue).toArray();
    }
    private record Camera(double x,double y,double z,boolean trueCentre,double offset){}
    private static Camera camera(HouseMap map,LevelDefinition.Room room){
        double cx=(room.x0()+room.x1())/2,cz=(room.z0()+room.z1())/2;
        double fy=map.groundHeight(cx,cz,room.id());
        if(map.traversable(cx,fy,cz)&&map.roomAt(cx,fy,cz)==room.id())
            return new Camera(cx,fy,cz,true,0);
        // Seek the nearest actual game-legal point on the same room/floor.
        Camera best=null;
        for(double dz=-7.0;dz<=7.0;dz+=.35)for(double dx=-7.0;dx<=7.0;dx+=.35){
            double xx=cx+dx,zz=cz+dz;
            if(xx<=room.x0()+.28||xx>=room.x1()-.28
                ||zz<=room.z0()+.28||zz>=room.z1()-.28)continue;
            double y=map.groundHeight(xx,zz,room.id());
            if(Math.abs(y-room.y())>.7)continue;
            if(map.roomAt(xx,y,zz)!=room.id()||!map.traversable(xx,y,zz))continue;
            double distance=Math.hypot(dx,dz);
            if(best==null||distance<best.offset())
                best=new Camera(xx,y,zz,false,distance);
        }
        return best;
    }
    private static boolean graphHas(int[] reachable,int id){
        for(int n:reachable)if(n==id)return true;
        return false;
    }
    private static String portalText(HouseMap map,LevelDefinition.Room room){
        StringBuilder s=new StringBuilder();
        for(var p:map.level().portalsFrom(room.id())){
            if(s.length()>0)s.append("; ");
            int other=p.a()==room.id()?p.b():p.a();
            s.append("P").append(p.id()).append(" ").append(p.kind())
                .append(" -> ").append(other).append(" ")
                .append(map.roomName(other)).append(" ")
                .append(fmt(p.width())).append("m @(")
                .append(fmt(p.x())).append(",").append(fmt(p.y()))
                .append(",").append(fmt(p.z())).append(")");
        }
        for(var stair:map.level().stairs())if(stair.a()==room.id()||stair.b()==room.id()){
            if(s.length()>0)s.append("; ");
            s.append("STAIR").append(stair.id()).append(" ")
                .append(stair.a()).append(" -> ").append(stair.b())
                .append(" floor ").append(fmt(stair.y0())).append(" to ")
                .append(fmt(stair.y1()));
        }
        return s.toString();
    }
    private static int portalCount(HouseMap map,int room,String kind){
        int n=0;
        for(var p:map.level().portalsFrom(room))
            if(p.kind().equals(kind)||kind.equals("access")&&!p.kind().equals("window"))n++;
        return n;
    }
    private static String semantic(HouseMap map){
        StringBuilder out=new StringBuilder();
        String names=String.join(" ",map.level().rooms().stream().map(r->r.name().toLowerCase(Locale.ROOT)).toList());
        String rubric=null;
        for(String entry:RUBRIC)if(entry.startsWith(map.site().name()+"|"))rubric=entry.substring(entry.indexOf('|')+1);
        if(rubric==null)return "No archetype rubric authored yet.";
        String[] wants=rubric.split(",");
        List<String> present=new ArrayList<>(),missing=new ArrayList<>();
        for(String word:wants){
            String tag=word.trim();
            if(names.contains(tag)||tag.equals("reception")&&(names.contains("foyer")||names.contains("admission"))
                ||tag.equals("laboratory")&&names.contains("science classroom")
                ||tag.equals("classroom")&&names.contains("lecture room")
                ||tag.equals("residence")&&names.contains("dormitory")
                ||tag.equals("grand hall")&&names.contains("grand staircase hall")
                ||tag.equals("sally port")&&names.contains("intake"))
                present.add(tag);
            else missing.add(tag);
        }
        out.append("Expected typology features (name-based candidates, NOT a visual pass): ")
            .append(wants.length).append("\n")
            .append("Recognizable by authored zone name: ").append(present).append("\n")
            .append("Not recognizable by zone name: ").append(missing).append("\n")
            .append("A feature may exist within a larger room; inspect screenshots and props before declaring it absent.\n");
        if(Set.of(HouseMap.Site.PRISON,HouseMap.Site.ASYLUM,HouseMap.Site.MANSION,
                HouseMap.Site.ACADEMY,HouseMap.Site.COLLEGE).contains(map.site()))
            out.append("STRUCTURAL PRIORITY: Shares the generic LevelPlans.wing cross-wing floorplan. ")
               .append("Renaming rooms and furnishing them does not establish the claimed institution's spatial function.\n");
        if(map.site()==HouseMap.Site.SCHOOL)
            out.append("USER PRIORITY: inspect student bathrooms, door privacy, accessible circulation, ")
                .append("locker/changing relationship, and their adjacency to classrooms and gym.\n");
        if(map.site()==HouseMap.Site.PRISON)
            out.append("USER PRIORITY: inspect actual individual cells, corridor/gallery supervision, ")
                .append("locked sequence (public -> intake -> controlled circulation -> cells), ")
                .append("visitation separation and secure yard/perimeter.\n");
        return out.toString();
    }
    private static void writePng(BufferedImage image,Path output)throws Exception{
        if(!ImageIO.write(image,"png",output.toFile()))
            throw new IllegalStateException("No PNG codec for "+output);
    }
    private static BufferedImage contact(int w,int h){
        var out=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
        var g=out.createGraphics();
        try{g.setColor(new Color(13,18,26));g.fillRect(0,0,w,h);}
        finally{g.dispose();}
        return out;
    }
    private static void audit(HouseMap.Site site,Path root)throws Exception{
        Path out=root.resolve(site.name().toLowerCase(Locale.ROOT));
        Files.createDirectories(out);
        var map=new HouseMap(site);
        var level=map.level();
        for(var p:level.portals())if(p.door())map.toggleDoor(p.id()); // open for route/capture audit
        var lights=new RoomLighting();
        var scene=new SceneRenderer();scene.setRenderSize(320,200);
        scene.setPsxPreset(PsxPipeline.Preset.CLASSIC);scene.roomLights=lights;
        scene.environmentLight=1.;scene.lamp=true;
        // Never silently apply user graphics preferences or additional ghost effects.
        scene.setHauntings(List.of());
        int[] reachable=connectivity(map);
        StringBuilder rows=new StringBuilder("site,room_id,room_name,floor,area_m2,")
            .append("center_x,center_z,camera_x,camera_z,camera_offset_m,")
            .append("center_physically_traversable,camera_physically_traversable,")
            .append("topologically_connected_to_van,access_openings,doors,windows,stairs,")
            .append("placed_props,interaction_sockets,status,north,east,south,west\n");
        int captures=0,missing=0,uncentered=0;
        for(var room:level.rooms()){
            Camera at=camera(map,room);
            double cx=(room.x0()+room.x1())/2,cz=(room.z0()+room.z1())/2;
            boolean physicalCenter=map.traversable(cx,map.groundHeight(cx,cz,room.id()),cz);
            if(at!=null&&!at.trueCentre())uncentered++;
            boolean reachableRoom=graphHas(reachable,room.id());
            int props=0,sockets=0;
            for(var p:level.props())if(p.room()==room.id())props++;
            for(var s:level.sockets())if(s.room()==room.id())sockets++;
            int countDoors=portalCount(map,room.id(),"door");
            int countWindows=portalCount(map,room.id(),"window");
            int countStairs=portalCount(map,room.id(),"stair");
            int countAccess=portalCount(map,room.id(),"access");
            Path roomDir=out.resolve(String.format(Locale.ROOT,"%02d-%s",room.id(),file(room.name())));
            Files.createDirectories(roomDir);
            BufferedImage panel=contact(640,440);
            Graphics2D cg=panel.createGraphics();
            String[] capturesForRoom=new String[4];
            for(int i=0;i<4;i++){
                String filename=FACING[i]+".png";
                capturesForRoom[i]=roomDir.getFileName()+"/"+filename;
                if(at==null){missing++;capturesForRoom[i]="NO_TRAVERSABLE_CAMERA";continue;}
                scene.x=at.x();scene.z=at.z();scene.eyeHeight=at.y()+1.62;
                scene.yaw=HEADING[i];scene.pitch=-.07;
                try{
                    scene.drawNetwork(map,List.of(),-1,12.0);
                    BufferedImage image=scene.image();
                    writePng(image,roomDir.resolve(filename));captures++;
                    cg.drawImage(image,(i%2)*320,(i/2)*220,320,200,null);
                    cg.setColor(new Color(220,225,233));
                    cg.setFont(new Font(Font.MONOSPACED,Font.BOLD,13));
                    cg.drawString(FACING[i].toUpperCase(Locale.ROOT),6+(i%2)*320,216+(i/2)*220);
                }catch(Exception renderFailure){
                    missing++;
                    capturesForRoom[i]="RENDER_ERROR";
                    Files.writeString(roomDir.resolve(filename+".error.txt"),
                        renderFailure.toString(),StandardCharsets.UTF_8);
                }
            }
            cg.dispose();writePng(panel,roomDir.resolve("four-way.png"));
            String status=at==null?"NO_SAFE_CAMERA":
                !reachableRoom?"TOPOLOGICALLY_ISOLATED":
                !at.trueCentre()?"CENTER_BLOCKED_FALLBACK":"CENTRE_VIEW_OK";
            rows.append(site).append(',').append(room.id()).append(',')
                .append(escape(room.name())).append(',').append(fmt(room.y())).append(',')
                .append(fmt((room.x1()-room.x0())*(room.z1()-room.z0()))).append(',')
                .append(fmt(cx)).append(',').append(fmt(cz)).append(',')
                .append(at==null?"":fmt(at.x())).append(',')
                .append(at==null?"":fmt(at.z())).append(',')
                .append(at==null?"":fmt(at.offset())).append(',')
                .append(physicalCenter).append(',')
                .append(at!=null).append(',').append(reachableRoom).append(',')
                .append(countAccess).append(',').append(countDoors).append(',')
                .append(countWindows).append(',').append(countStairs).append(',')
                .append(props).append(',').append(sockets).append(',')
                .append(status).append(',').append(String.join(",",capturesForRoom))
                .append('\n');
            String notes="Room "+room.id()+": "+room.name()+"\n"
                +"Site: "+level.title()+" / "+level.revision()+"\n"
                +"Bounds (X,Z): "+fmt(room.x0())+","+fmt(room.z0())
                +" -> "+fmt(room.x1())+","+fmt(room.z1())+"; floor "+fmt(room.y())
                +"m; clear height "+fmt(room.height())+"m\n"
                +"Geometric centre: "+fmt(cx)+","+fmt(cz)+"; traversable: "+physicalCenter+"\n"
                +"Actual 4-view camera: "+(at==null?"UNAVAILABLE":
                    fmt(at.x())+","+fmt(at.y())+","+fmt(at.z())+
                    "; displacement "+fmt(at.offset())+"m")+"\n"
                +"Portal access / windows / stairs: "+portalText(map,room)+"\n"
                +"Connected in authored portal graph to van: "+reachableRoom+"\n"
                +"Props: "+props+"; sockets: "+sockets+"\n"
                +"AUDIT QUESTIONS (manual): Can you recognize the room's purpose from its \n"
                +"shape, props, sightlines and doors? Are entrances, circulation and privacy believable?\n"
                +"Do camera views show missing wall segments, misplaced toilets, clutter, dead ends?\n"
                +"Does the actual door and stair network make physical sense?\n";
            Files.writeString(roomDir.resolve("room-essay.txt"),notes,StandardCharsets.UTF_8);
        }
        Files.writeString(out.resolve("room-matrix.csv"),rows,StandardCharsets.UTF_8);
        Files.writeString(out.resolve("site-purpose-review.txt"),
            "SITE "+site+" / "+level.title()+"\n"
            +"Status: "+(SiteCatalog.playable(site)?"PLAYABLE":"PROTOTYPE / NOT DISPATCHABLE")+"\n"
            +"Room capture: "+captures+" actual full-direction PNGs; "+missing
            +" missing/failed views; "+uncentered+" geometric centres obstructed.\n"
            +"Route graph (from van): "+Arrays.toString(reachable)+"\n"
            +"Architecture: "+level.rooms().size()+" gameplay zones; "
            +level.portals().size()+" portals; "+level.stairs().size()+" flights; "
            +level.props().size()+" decorative/physical props.\n\n"
            +semantic(map)
            +"\nMANUAL REVIEW REQUIRED: graphics and architecture cannot be certified by "
            +"automatic room-name matching, frame hashes or prop counts.\n"
            +"Access condition is a door-open baseline; test doors closed/locked and "
            +"escape/fire routes separately before changes.\n",
            StandardCharsets.UTF_8);
        System.out.println("SITE_AUDIT "+site+" captures="+captures
            +" missing="+missing+" blocked_centre="+uncentered
            +" topology_reached="+reachable.length);
    }
    private static void castle(Path root)throws Exception{
        Path out=root.resolve("castle");Files.createDirectories(out);
        CastleHubMap map=new CastleHubMap();
        var scene=new SceneRenderer();scene.setRenderSize(320,200);
        scene.setPsxPreset(PsxPipeline.Preset.CLASSIC);scene.lamp=true;
        StringBuilder csv=new StringBuilder("room_name,floor,camera_x,camera_z,center_traversable,usable_camera,north,east,south,west\n");
        for(var room:CastleRooms.ALL){
            double cx=(room.x0()+room.x1())/2,cz=(room.z0()+room.z1())/2;
            boolean valid=map.traversable(cx,cz,room.floor());
            double px=cx,pz=cz;
            if(!valid){
                double best=Double.POSITIVE_INFINITY;
                for(double x=room.x0()+.5;x<room.x1()-.5;x+=.5)
                    for(double z=room.z0()+.5;z<room.z1()-.5;z+=.5){
                        if(map.traversable(x,z,room.floor())&&Math.hypot(x-cx,z-cz)<best){
                            px=x;pz=z;best=Math.hypot(x-cx,z-cz);
                        }
                    }
                if(!Double.isFinite(best)){px=Double.NaN;pz=Double.NaN;}
            }
            Path roomDir=out.resolve(file(room.id()));
            Files.createDirectories(roomDir);
            String[] filenames=new String[4];
            for(int face=0;face<4;face++){
                filenames[face]=roomDir.getFileName()+"/"+FACING[face]+".png";
                if(!Double.isFinite(px)){filenames[face]="NO_SAFE_CAMERA";continue;}
                scene.x=px;scene.z=pz;scene.eyeHeight=room.floor()+1.62;
                scene.yaw=HEADING[face];scene.pitch=-.07;
                try{
                    scene.drawCastle(map,.5,true);
                    writePng(scene.image(),roomDir.resolve(FACING[face]+".png"));
                }catch(Exception e){filenames[face]="RENDER_ERROR";Files.writeString(
                    roomDir.resolve(FACING[face]+".error.txt"),e.toString());}
            }
            csv.append(escape(room.name())).append(',').append(fmt(room.floor()))
                .append(',').append(Double.isFinite(px)?fmt(px):"").append(',')
                .append(Double.isFinite(pz)?fmt(pz):"").append(',').append(valid)
                .append(',').append(Double.isFinite(px)).append(',')
                .append(String.join(",",filenames)).append('\n');
        }
        Files.writeString(out.resolve("castle-rooms.csv"),csv,StandardCharsets.UTF_8);
        Files.writeString(out.resolve("castle-purpose-review.txt"),
            "Castle Beckenburg is a headquarters, not a 15-zone investigation map. "
            +"Ten authored CastleRooms are audited separately, preserving the independent "
            +"editor/doors/stations geometry. Room names and four cardinal captures "
            +"do not substitute for a full navigability/playtest review.\n");
    }
    public static void main(String[] args)throws Exception{
        Path output=Path.of("build/map-first-principles-audit");
        String chosen="ALL";
        boolean prototypes=false;
        boolean castle=true;
        for(String arg:args){
            if(arg.startsWith("--output="))output=Path.of(arg.substring(9));
            else if(arg.startsWith("--site="))chosen=arg.substring(7).toUpperCase(Locale.ROOT);
            else if(arg.equals("--include-prototypes"))prototypes=true;
            else if(arg.equals("--no-castle"))castle=false;
            else throw new IllegalArgumentException("Unknown audit option "+arg);
        }
        Files.createDirectories(output);
        StringBuilder failures=new StringBuilder();
        for(HouseMap.Site site:HouseMap.Site.values()){
            if(!chosen.equals("ALL")&&!chosen.equals(site.name()))continue;
            if(!prototypes&&!SiteCatalog.playable(site))continue;
            try{audit(site,output);}catch(Exception e){
                failures.append(site).append(" / ").append(e).append('\n');
                Files.createDirectories(output.resolve(site.name().toLowerCase(Locale.ROOT)));
                Files.writeString(output.resolve(site.name().toLowerCase(Locale.ROOT))
                    .resolve("AUDIT_ERROR.txt"),e.toString());
            }
        }
        if(castle&&chosen.equals("ALL")){
            try{castle(output);}catch(Exception e){
                failures.append("CASTLE / ").append(e).append('\n');
                Files.writeString(output.resolve("CASTLE_AUDIT_ERROR.txt"),e.toString());
            }
        }
        Files.writeString(output.resolve("audit-incomplete.txt"),
            failures.length()==0?"No full-site audit exceptions; see individual room status.\n":failures.toString());
        System.out.println("MAP_AUDIT_ARTIFACTS "+output.toAbsolutePath()
            +" / visuals are diagnostics, no automatic architecture approvals.");
    }
}
