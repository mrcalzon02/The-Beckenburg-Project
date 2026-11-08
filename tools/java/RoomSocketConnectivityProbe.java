import java.nio.file.*;
import java.util.*;
import org.beckenburg.AuthoredSitePhysics;
import org.beckenburg.LevelDefinition.Vec;

/** Independent direct room-join replay. Blocked direct joins remain unresolved;
 * this diagnostic never guesses connectivity from a shared room name. */
public final class RoomSocketConnectivityProbe {
    static AuthoredSitePhysics map;
    static Vec point(String s){String[] v=s.split(",");return new Vec(Double.parseDouble(v[0]),Double.parseDouble(v[1]),Double.parseDouble(v[2]));}
    static boolean standing(Vec p){var s=map.support(p.x(),p.z(),p.y(),.02);return s!=null&&Math.abs(s.y()-p.y())<.020001&&map.traversable(p,1.8);}
    static boolean sweep(Vec a,Vec b){
        if(!standing(a)||!standing(b))return false;
        int n=Math.max(1,(int)Math.ceil(Math.hypot(b.x()-a.x(),b.z()-a.z())/.05));Vec current=a;
        for(int i=1;i<=n;i++){
            double x=a.x()+(b.x()-a.x())*i/n,z=a.z()+(b.z()-a.z())*i/n;
            Vec next=map.resolveMove(current,x,z,1.8);
            if(next==null||Math.abs(next.y()-a.y())>.020001)return false;
            current=next;
        }
        return true;
    }
    public static void main(String[] args)throws Exception{
        var names=new HashMap<String,Set<String>>();for(String row:Files.readAllLines(Path.of(args[1]))){String[] p=row.split("\t");names.computeIfAbsent(p[0],k->new HashSet<>()).add(p[1]);}
        var out=new ArrayList<String>();out.add("site\troom\ty\tnodes\tvalid_nodes\tdirect_edges\tcomponents\tinvalid_indices\tconnected_pairs");String previous="";
        for(String row:Files.readAllLines(Path.of(args[0]))){
            String[] p=row.split("\t",-1);
            if(!previous.equals(p[0])){previous=p[0];try(var in=Files.newInputStream(Path.of(p[1]))){map=AuthoredSitePhysics.read(in,p[2],List.of(),names.getOrDefault(p[0],Set.of()));}}
            List<Vec> points=Arrays.stream(p[5].split(";")).map(RoomSocketConnectivityProbe::point).toList();int n=points.size(),valid=0,edges=0;int[] parent=new int[n];boolean[] ok=new boolean[n];var invalid=new StringJoiner(",");var pairs=new StringJoiner(";");
            double[] box=Arrays.stream(p[6].split(",")).mapToDouble(Double::parseDouble).toArray();
            for(int i=0;i<n;i++){Vec v=points.get(i);parent[i]=i;ok[i]=v.x()>=box[0]&&v.x()<=box[2]&&v.z()>=box[1]&&v.z()<=box[3]&&standing(v);if(ok[i])valid++;else invalid.add(""+i);}
            for(int i=0;i<n;i++)for(int j=0;j<i;j++)if(ok[i]&&ok[j]&&sweep(points.get(i),points.get(j))&&sweep(points.get(j),points.get(i))){edges++;pairs.add(j+","+i);int a=i,b=j;while(parent[a]!=a)a=parent[a];while(parent[b]!=b)b=parent[b];parent[a]=b;}
            var components=new HashSet<Integer>();for(int i=0;i<n;i++)if(ok[i]){int a=i;while(parent[a]!=a)a=parent[a];components.add(a);}
            out.add(String.join("\t",p[0],p[3],p[4],""+n,""+valid,""+edges,""+components.size(),invalid.toString(),pairs.toString()));
        }
        Files.write(Path.of(args[2]),out);System.out.println("ROOM_SOCKET_REPLAY "+(out.size()-1)+" actual room/elevation groups");
    }
}
