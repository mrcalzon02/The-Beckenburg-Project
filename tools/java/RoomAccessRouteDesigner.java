import java.nio.file.*;
import java.util.*;
import org.beckenburg.AuthoredSitePhysics;
import org.beckenburg.LevelDefinition.Vec;

/** Full-width access sockets on existing floors. No geometry or physics edits. */
public final class RoomAccessRouteDesigner {
    static boolean fits(Vec p){
        if(FlatRouteDesigner.feet(p.x(),p.z())==null)return false;
        double radius=Math.max(0,FlatRouteDesigner.width*.5-.22);
        for(int i=0;i<16;i++){double a=i*Math.PI/8;if(FlatRouteDesigner.feet(p.x()+Math.cos(a)*radius,p.z()+Math.sin(a)*radius)==null)return false;}
        return true;
    }
    static List<Vec> choices(Vec original,String encoded){
        var result=new ArrayList<Vec>();
        if(FlatRouteDesigner.feet(original.x(),original.z())!=null)result.add(original);
        if(!encoded.isEmpty())for(String text:encoded.split(";")){
            Vec p=FlatRouteDesigner.parse(text);if(result.contains(p)||!fits(p))continue;result.add(p);if(result.size()>=12)break;
        }
        return result;
    }
    static boolean complete(List<Vec> path){
        if(path==null)return false;
        for(Vec p:path)if(!fits(p))return false;
        for(int i=1;i<path.size();i++)if(!FlatRouteDesigner.clear(path.get(i-1),path.get(i)))return false;
        return true;
    }
    static List<Vec> plan(List<Vec> anchors,List<Vec> starts,List<Vec> ends){
        if(anchors.size()==3){
            Vec door=anchors.get(1);if(!fits(door))return null;
            List<Vec> front=null,rear=null;
            for(Vec a:starts){var p=FlatRouteDesigner.find(a,door);if(complete(p)){front=p;break;}}
            if(front==null)return null;
            for(Vec b:ends){var p=FlatRouteDesigner.find(door,b);if(complete(p)){rear=p;break;}}
            if(rear==null)return null;
            var result=new ArrayList<>(front);result.addAll(rear.subList(1,rear.size()));return result;
        }
        for(Vec a:starts)for(Vec b:ends){var p=FlatRouteDesigner.find(a,b);if(complete(p))return p;}
        return null;
    }
    public static void main(String[] args)throws Exception{
        var names=new HashMap<String,Set<String>>();for(String row:Files.readAllLines(Path.of(args[1]))){String[] p=row.split("\t");names.computeIfAbsent(p[0],k->new HashSet<>()).add(p[1]);}
        var sockets=new HashMap<String,String[]>();for(String row:Files.readAllLines(Path.of(args[2]))){String[] p=row.split("\t",-1);sockets.put(p[0]+":"+p[1],p);}
        var out=new ArrayList<String>();out.add("site\tlink\tstatus\tchecked_edges\tpoints");String previous="";
        for(String row:Files.readAllLines(Path.of(args[0]))){
            String[] p=row.split("\t",-1);String id=p[0]+":"+p[3];
            if(!previous.equals(p[0])){previous=p[0];try(var in=Files.newInputStream(Path.of(p[1]))){FlatRouteDesigner.map=AuthoredSitePhysics.read(in,p[2],List.of(),names.getOrDefault(p[0],Set.of()));}}
            FlatRouteDesigner.width=Double.parseDouble(p[4]);var anchors=Arrays.stream(p[5].split(";")).map(FlatRouteDesigner::parse).toList();FlatRouteDesigner.y=anchors.get(0).y();FlatRouteDesigner.sealed=new ArrayList<>();
            if(!p[6].isEmpty())for(String r:p[6].split(";")){double[] v=Arrays.stream(r.split(",")).mapToDouble(Double::parseDouble).toArray();FlatRouteDesigner.sealed.add(new FlatRouteDesigner.Rect(v[0],v[1],v[2],v[3],v[4],v[5]));}
            double[] box=Arrays.stream(p[7].split(",")).mapToDouble(Double::parseDouble).toArray();FlatRouteDesigner.x0=Math.floor(box[0]*2)/2;FlatRouteDesigner.z0=Math.floor(box[1]*2)/2;FlatRouteDesigner.nx=(int)Math.ceil((box[2]-FlatRouteDesigner.x0)*2)+1;FlatRouteDesigner.nz=(int)Math.ceil((box[3]-FlatRouteDesigner.z0)*2)+1;FlatRouteDesigner.valid=new HashMap<>();FlatRouteDesigner.edges=new HashMap<>();FlatRouteDesigner.checked=0;
            String[] candidates=sockets.get(id);var starts=choices(anchors.get(0),candidates[2]);var ends=choices(anchors.get(anchors.size()-1),candidates[3]);String status;
            List<Vec> path=null;
            if(starts.isEmpty()||ends.isEmpty())status="NO_CLEAR_ACCESS_SOCKET_IN_AUTHORED_ROOM";
            else if(anchors.size()==3&&!fits(anchors.get(1)))status="MANDATORY_DOORWAY_REQUIRES_CONSTRUCTION";
            else if(FlatRouteDesigner.feet(anchors.get(0).x(),anchors.get(0).z())!=null&&FlatRouteDesigner.feet(anchors.get(anchors.size()-1).x(),anchors.get(anchors.size()-1).z())!=null)status="KEEP_FIXED_ENDPOINT_STUDY_SCOPE";
            else {path=plan(anchors,starts,ends);status=path==null?"NO_FULL_WIDTH_CONNECTION_FROM_ROOM_SOCKETS":"REROUTED_ROOM_ACCESS";}
            out.add(String.join("\t",p[0],p[3],status,""+FlatRouteDesigner.checked,path==null?"":FlatRouteDesigner.serial(path)));Files.write(Path.of(args[3]),out);System.out.println("ROOM_ACCESS "+id+" "+status+" edges="+FlatRouteDesigner.checked);
        }
    }
}
