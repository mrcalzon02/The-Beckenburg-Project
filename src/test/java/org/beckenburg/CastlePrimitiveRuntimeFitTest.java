package org.beckenburg;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Opt-in native Castle / source-graybox fit audit. This reads the ACTUAL game
 * walls, doors, native editor props, stations, floor supports and renderer.
 *
 * It does NOT install preview meshes into CastleLevelEdits or runtime assets.
 * Planned CASTLE layout.json is a separate expanded-map design and must never
 * be mistaken for the 44x36 metre playable Castle coordinate system.
 */
public final class CastlePrimitiveRuntimeFitTest {
    private CastlePrimitiveRuntimeFitTest(){}
    record Target(String room,String model,String anchor,double yaw){}
    record Rect(double x0,double z0,double x1,double z1){
        boolean touches(Rect other,double pad){
            return x0<other.x1+pad&&x1>other.x0-pad&&
                z0<other.z1+pad&&z1>other.z0-pad;
        }
        boolean contains(double x,double z){
            return x>=x0&&x<=x1&&z>=z0&&z<=z1;
        }
        double distance(double x,double z){
            return Math.hypot(Math.max(Math.max(x0-x,0),x-x1),
                              Math.max(Math.max(z0-z,0),z-z1));
        }
    }
    record Fit(Target target,CastleRooms.Room room,
               double x,double y,double z,Rect rect,String evidence) {
        SceneRenderer.CastlePrototypeReviewInstance render(){
            return new SceneRenderer.CastlePrototypeReviewInstance(
                target.model(),x,y,z,target.yaw());
        }
    }
    private static final List<Target> CANDIDATES=List.of(
        new Target("armory","level_blockout_quartermaster_counter","NE",Math.PI/2),
        new Target("contracts","level_blockout_map_pinboard","NW",0),
        new Target("common","level_blockout_lecture_lectern","SE",Math.PI),
        new Target("archive","level_blockout_archive_cabinet","NE",0),
        new Target("store","level_blockout_storage_shelf","SE",0),
        new Target("repair","level_blockout_utility_panel","SW",0),
        new Target("infirmary","level_blockout_privacy_screen","NE",Math.PI/2),
        new Target("network","level_blockout_console","SW",Math.PI),
        new Target("library","level_blockout_map_pinboard","NE",0),
        new Target("lodging-2","level_blockout_storage_shelf","NW",0)
    );
    private static void require(boolean condition,String problem){
        if(!condition)throw new AssertionError(problem);
    }
    private static CastleRooms.Room zone(String name){
        return CastleRooms.ALL.stream().filter(r->r.id().equals(name))
            .findFirst().orElseThrow(()->new IllegalArgumentException(
                "No native Castle room: "+name));
    }
    private static Rect worldFootprint(DevAssetReviewCatalog.Audit a,
                                       double x,double z,double yaw){
        double loX=Double.POSITIVE_INFINITY,loZ=loX,hiX=-loX,hiZ=-loX;
        double c=Math.cos(yaw),s=Math.sin(yaw);
        for(double lx:new double[]{a.minX(),a.maxX()})
            for(double lz:new double[]{a.minZ(),a.maxZ()}){
                double wx=x+lx*c-lz*s,wz=z+lx*s+lz*c;
                loX=Math.min(loX,wx);hiX=Math.max(hiX,wx);
                loZ=Math.min(loZ,wz);hiZ=Math.max(hiZ,wz);
            }
        return new Rect(loX,loZ,hiX,hiZ);
    }
    private static boolean withinRoom(Rect b,CastleRooms.Room r){
        double margin=.42;
        return b.x0>=r.x0()+margin&&b.z0>=r.z0()+margin&&
            b.x1<=r.x1()-margin&&b.z1<=r.z1()-margin;
    }
    private static boolean supported(CastleRooms.Room room,Rect b,
                                     CastleHubMap map){
        double x0=b.x0+.07,x1=b.x1-.07,z0=b.z0+.07,z1=b.z1-.07;
        if(x1<x0||z1<z0)return false;
        for(double x:new double[]{x0,(x0+x1)/2,x1})
            for(double z:new double[]{z0,(z0+z1)/2,z1}){
                var at=CastleRooms.at(x,room.floor(),z);
                if(at==null||!at.id().equals(room.id()))return false;
                if(room.floor()>2&&
                        !CastleLevelEdits.current().floorSupports(x,z,room.floor()))
                    return false;
                if(CastleHubArchitecturalPlan.onStairs(x,z))return false;
                if(!map.traversable(x,z,room.floor()))return false;
            }
        return true;
    }
    private static boolean clearsNativeWalls(CastleRooms.Room room,Rect b){
        for(var wall:CastleLevelEdits.current().walls()){
            if(wall.bottom()>=room.floor()+2.25||
               wall.top()<=room.floor()+.05)continue;
            if(wall.hasDoor())continue;
            Rect slab=wall.horizontal()
                ?new Rect(Math.min(wall.x0(),wall.x1()),
                          wall.z0()-.21,Math.max(wall.x0(),wall.x1()),wall.z0()+.21)
                :new Rect(wall.x0()-.21,Math.min(wall.z0(),wall.z1()),
                          wall.x0()+.21,Math.max(wall.z0(),wall.z1()));
            if(b.touches(slab,.25))return false;
        }
        return true;
    }
    private static boolean clearsNativeDoors(CastleRooms.Room room,Rect b){
        for(var socket:CastleDoorSlots.all()){
            if(!CastleDoorSlots.available(socket)
               ||Math.abs(socket.bottom()-room.floor())>.18)continue;
            // Reserve a 1.75m approach around every actual door (open/closed).
            // A temporary study cannot steal egress just because the leaf is open.
            Rect threshold=socket.horizontal()
                ?new Rect(socket.x0(),socket.z0()-.25,socket.x1(),socket.z1()+.25)
                :new Rect(socket.x0()-.25,socket.z0(),socket.x1()+.25,socket.z1());
            if(b.touches(threshold,1.75))return false;
        }
        return true;
    }
    private static boolean clearsExisting(CastleRooms.Room room,Rect b){
        for(var p:CastleLevelEdits.current().props()){
            // Native props are ground-/upper-/cellar-scoped, not billboards.
            if(Math.abs(p.y()-room.floor())>1.15)continue;
            Rect old=new Rect(p.x()-p.halfX(),p.z()-p.halfZ(),
                              p.x()+p.halfX(),p.z()+p.halfZ());
            if(b.touches(old,.40))return false;
        }
        if(Math.abs(room.floor())<.18)
            for(var f:CastleArchive.furnishings()){
                Rect old=new Rect(f.x()-f.halfWidth(),f.z()-f.halfDepth(),
                                  f.x()+f.halfWidth(),f.z()+f.halfDepth());
                if(b.touches(old,.40))return false;
            }
        return true;
    }
    private static boolean clearsAuthorities(CastleRooms.Room room,Rect b){
        if(Math.abs(room.floor())<.18){
            for(var station:CastleHubMap.Station.values())
                if(b.distance(station.x,station.z)<1.75)return false;
            double[] spawn=CastleLevelEdits.current().spawn();
            if(b.distance(spawn[0],spawn[1])<2.0)return false;
        }
        return true;
    }
    private static Fit findFit(Target target,CastleHubMap map,List<Fit> earlier){
        CastleRooms.Room room=zone(target.room());
        require(!room.outside(),"No grayboxes on staging, street or courtyard");
        String id=target.model();
        require(id.startsWith("level_blockout_"),"Wrong model category");
        require(!GlbProps.availableModels().contains(id)&&!LevelAssets.approved(id),
            "Graybox must never enter production model allowlist: "+id);
        var entry=new DevAssetReviewCatalog.Entry(DevAssetReviewCatalog.Kind.SOURCE_ONLY,id);
        var audit=DevAssetReviewCatalog.audit(entry);
        require(audit.loaded()&&audit.state().equals("REVIEW_REGISTRATION")
            &&audit.triangles()>0,
            "Actual source-only GLB was not decoded: "+id+" / "+audit.details());
        double modelHeight=audit.maxY()-audit.minY();
        if(!(modelHeight>0&&modelHeight<4.5))
            throw new AssertionError("Physically invalid graybox height "+id);
        if(!Double.isNaN(room.ceiling())&&modelHeight>room.ceiling()-room.floor()-.22)
            return null;
        double wantX=target.anchor().endsWith("W")?room.x0()+.85:
            target.anchor().endsWith("E")?room.x1()-.85:(room.x0()+room.x1())/2;
        double wantZ=target.anchor().startsWith("N")?room.z0()+.85:
            target.anchor().startsWith("S")?room.z1()-.85:(room.z0()+room.z1())/2;
        var options=new ArrayList<double[]>();
        for(double x=room.x0()+.5;x<=room.x1()-.5;x+=.50)
            for(double z=room.z0()+.5;z<=room.z1()-.5;z+=.50)
                options.add(new double[]{x,z});
        options.sort(Comparator.comparingDouble(p->
            Math.hypot(p[0]-wantX,p[1]-wantZ)));
        for(double[] pos:options){
            double x=pos[0],z=pos[1];
            Rect box=worldFootprint(audit,x,z,target.yaw());
            if(!withinRoom(box,room)
                ||!clearsNativeWalls(room,box)||!clearsNativeDoors(room,box)
                ||!clearsExisting(room,box)||!clearsAuthorities(room,box)
                ||!supported(room,box,map))continue;
            if(earlier.stream().anyMatch(p->Math.abs(p.room().floor()-room.floor())<.5
                &&box.touches(p.rect(),.50)))continue;
            // Actual renderer uses the same source GLB and native XY ground
            // pivot; translating by -minY never buries a mesh underground.
            return new Fit(target,room,x,room.floor()-audit.minY(),z,box,
                "Native walls/doors/floor/furniture/stations/circulation checked");
        }
        return null;
    }
    private static String csv(String v){
        return "\""+v.replace("\"","\"\"").replace("\n"," ")+"\"";
    }
    private static int pixelsChanged(int[] before,int[] after){
        require(before.length==after.length,"Different renderer buffer size");
        int count=0;
        for(int i=0;i<before.length;i++)if(before[i]!=after[i])count++;
        return count;
    }
    private static int capture(Fit fit,CastleHubMap map,Path folder)throws IOException{
        SceneRenderer scene=new SceneRenderer();
        scene.setRenderSize(640,400);
        scene.setPsxPreset(PsxPipeline.Preset.OFF);
        scene.castleCharactersVisible=false;
        scene.lamp=false;
        // Find a free observer position on the same native floor.
        double cx=(fit.room().x0()+fit.room().x1())/2;
        double cz=(fit.room().z0()+fit.room().z1())/2;
        double camX=cx,camZ=cz;
        double best=Double.POSITIVE_INFINITY;
        for(double x=fit.room().x0()+1;x<fit.room().x1()-1;x+=.75)
            for(double z=fit.room().z0()+1;z<fit.room().z1()-1;z+=.75){
                if(!map.traversable(x,z,fit.room().floor()))continue;
                double distance=Math.hypot(x-fit.x(),z-fit.z());
                if(distance<1.6||distance>7)continue;
                double score=Math.hypot(x-cx,z-cz)+Math.abs(distance-3);
                if(score<best){best=score;camX=x;camZ=z;}
            }
        scene.x=camX;scene.z=camZ;
        scene.eyeHeight=fit.room().floor()+1.6;
        scene.yaw=Math.atan2(fit.x()-camX,fit.z()-camZ);
        scene.pitch=-.045;
        scene.drawCastle(map,0,true);
        var before=scene.image().getRGB(0,0,640,400,null,0,640);
        scene.setCastlePrototypeReview(List.of(fit.render()));
        scene.drawCastle(map,0,true);
        var after=scene.image().getRGB(0,0,640,400,null,0,640);
        Files.createDirectories(folder);
        String name=fit.target().room()+"-"+fit.target().model();
        require(ImageIO.write(scene.image(),"png",
                    folder.resolve(name+".png").toFile()),
                "Cannot write native Castle screenshot");
        scene.clearCastlePrototypeReview();
        return pixelsChanged(before,after);
    }
    public static void main(String[] args)throws Exception{
        Path output=args.length==0?Path.of("build","primitive-native-castle"):
            Path.of(args[0]);
        Files.createDirectories(output);
        var map=new CastleHubMap();
        var fits=new ArrayList<Fit>();
        var report=new StringBuilder(
            "room,model,status,x,ground_y,z,yaw,min_x,min_z,max_x,max_z,renderer_changed_pixels,notes\n");
        int blocked=0,screenshots=0,visible=0;
        for(Target target:CANDIDATES){
            Fit found=findFit(target,map,fits);
            if(found==null){
                blocked++;
                report.append(csv(target.room())).append(',').append(csv(target.model()))
                    .append(",BLOCKED,,,,,,,,,0,")
                    .append(csv("Could not prove clearance against current native geometry"))
                    .append('\n');
                System.out.println("NATIVE REVIEW BLOCKED "+target.room()+" / "+target.model());
                continue;
            }
            fits.add(found);
            int delta=0;
            if(screenshots<4){
                delta=capture(found,map,output.resolve("screenshots"));
                screenshots++;
                if(delta>30)visible++;
            }
            Rect b=found.rect();
            report.append(csv(target.room())).append(',').append(csv(target.model()))
                .append(",REVIEW_FIT,")
                .append(String.format(Locale.ROOT,"%.3f,%.3f,%.3f,%.5f,%.3f,%.3f,%.3f,%.3f,%d,",
                    found.x(),found.y(),found.z(),target.yaw(),
                    b.x0,b.z0,b.x1,b.z1,delta))
                .append(csv(found.evidence())).append('\n');
            System.out.println("NATIVE REVIEW FIT "+target.room()+" "+
                target.model()+" at "+found.x()+","+found.y()+","+found.z()+
                " screenshotChanged="+delta);
        }
        Files.writeString(output.resolve("native-castle-fit.csv"),report,
            StandardCharsets.UTF_8);
        Files.writeString(output.resolve("README.txt"),
            "Native Castle default geometry only; 44x36m. Source-only study.\n"+
            "No change to CastleLevelEdits snapshots, collision, multiplayer or runtime model approvals.\n"+
            "REVIEW_FIT is NOT a gameplay-authorized placement.\n"+
            "BLOCKED rows require edited geometry or revised models, not silent overlap.\n"+
            "Screenshots use the ACTUAL Castle renderer with temporary opt-in geometry.\n",
            StandardCharsets.UTF_8);
        require(!fits.isEmpty(),"No source-only model could fit any actual Castle space");
        require(visible>0,"All actual Castle preview renderer deltas were invisible");
        System.out.println("PASS native Castle source-only review fits="+fits.size()+
            " blocked="+blocked+" actualRenderScreenshots="+screenshots+
            " visiblyChanged="+visible+" (NOT playable placement acceptance)");
    }
}
