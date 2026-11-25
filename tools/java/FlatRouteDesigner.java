import java.nio.file.*;
import java.util.*;
import org.beckenburg.AuthoredSitePhysics;
import org.beckenburg.LevelDefinition.Vec;

/** Design routes on exact native floors. Never carves geometry or changes physics. */
public final class FlatRouteDesigner {
    record Rect(double x0,double z0,double x1,double z1,double y0,double y1) {}
    record Queue(int key,double score) {}
    static AuthoredSitePhysics map;
    static List<Rect> sealed;
    static double y,width,x0,z0;
    static int nx,nz;
    static Map<Integer,Boolean> valid;
    static Map<Long,Boolean> edges;
    static int checked;
    static boolean allowed(double x,double z) {
        for(var r:sealed)if(y+1.8>r.y0&&y<r.y1&&x>r.x0-.23&&x<r.x1+.23&&z>r.z0-.23&&z<r.z1+.23)return false;
        return true;
    }
    static Vec feet(double x,double z) {
        if(!allowed(x,z))return null;
        var s=map.support(x,z,y,.02);
        if(s==null)return null;
        var p=new Vec(x,s.y(),z);
        return map.traversable(p,1.8)?p:null;
    }
    static Vec at(int k){return new Vec(x0+(k%nx)*.5,y,z0+(k/nx)*.5);}
    static boolean node(int k){return valid.computeIfAbsent(k,i->{var p=at(i);return feet(p.x(),p.z())!=null;});}
    static boolean sweep(Vec a,Vec b) {
        var p=feet(a.x(),a.z());if(p==null)return false;
        int n=Math.max(1,(int)Math.ceil(Math.hypot(b.x()-a.x(),b.z()-a.z())/.1));
        for(int i=1;i<=n;i++){
            double x=a.x()+(b.x()-a.x())*i/n,z=a.z()+(b.z()-a.z())*i/n;
            if(!allowed(x,z))return false;
            p=map.resolveMove(p,x,z,1.8);
            if(p==null||Math.abs(p.y()-y)>.020001)return false;
        }
        return true;
    }
    static boolean clear(Vec a,Vec b) {
        checked++;
        double dx=b.x()-a.x(),dz=b.z()-a.z(),length=Math.hypot(dx,dz);
        if(length<1e-7)return feet(a.x(),a.z())!=null;
        // Test the original whole route width, including actor footprint at edges.
        double edge=Math.max(0,width*.5-.22);
        int count=Math.max(1,(int)Math.ceil(2*edge/.25));
        for(int lane=0;lane<=count;lane++){
            double offset=-edge+2*edge*lane/count;
            var from=new Vec(a.x()-dz/length*offset,y,a.z()+dx/length*offset);
            var to=new Vec(b.x()-dz/length*offset,y,b.z()+dx/length*offset);
            if(!sweep(from,to)||!sweep(to,from))return false;
        }
        return sweep(a,b)&&sweep(b,a);
    }
    static boolean edge(int a,int b){long key=((long)Math.min(a,b)<<32)|Math.max(a,b);return edges.computeIfAbsent(key,k->clear(at(a),at(b)));}
    static double distance(Vec a,Vec b){return Math.hypot(a.x()-b.x(),a.z()-b.z());}
    static List<Vec> find(Vec a,Vec b) {
        if(feet(a.x(),a.z())==null||feet(b.x(),b.z())==null)return null;
        if(clear(a,b))return List.of(a,b);
        var q=new PriorityQueue<Queue>(Comparator.comparingDouble(Queue::score));
        var g=new HashMap<Integer,Double>();var parent=new HashMap<Integer,Integer>();var closed=new HashSet<Integer>();
        int ax=(int)Math.round((a.x()-x0)/.5),az=(int)Math.round((a.z()-z0)/.5);
        for(int xx=Math.max(0,ax-2);xx<=Math.min(nx-1,ax+2);xx++)for(int zz=Math.max(0,az-2);zz<=Math.min(nz-1,az+2);zz++){
            int k=zz*nx+xx;if(!node(k)||!clear(a,at(k)))continue;
            double cost=distance(a,at(k));g.put(k,cost);parent.put(k,-1);q.add(new Queue(k,cost+distance(at(k),b)));
        }
        int end=-1;
        while(!q.isEmpty()&&closed.size()<20000){
            int k=q.remove().key;if(!closed.add(k))continue;Vec p=at(k);
            if(distance(p,b)<1.6&&clear(p,b)){end=k;break;}
            int xx=k%nx,zz=k/nx;
            for(int[] d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}){
                int x=xx+d[0],z=zz+d[1];if(x<0||x>=nx||z<0||z>=nz)continue;
                int next=z*nx+x;if(closed.contains(next)||!node(next))continue;
                double cost=g.get(k)+.5;if(cost>=g.getOrDefault(next,Double.POSITIVE_INFINITY)||!edge(k,next))continue;
                g.put(next,cost);parent.put(next,k);q.add(new Queue(next,cost+distance(at(next),b)));
            }
        }
        if(end<0)return null;
        var points=new ArrayList<Vec>();points.add(b);for(int k=end;k!=-1;k=parent.get(k))points.add(at(k));points.add(a);Collections.reverse(points);
        // Remove grid staircasing only after full-width physical proof.
        var result=new ArrayList<Vec>();int i=0;result.add(a);
        while(i<points.size()-1){int j=points.size()-1;while(j>i+1&&!clear(points.get(i),points.get(j)))j--;result.add(points.get(j));i=j;}
        return result;
    }
    static Vec parse(String s){String[] p=s.split(",");return new Vec(Double.parseDouble(p[0]),Double.parseDouble(p[1]),Double.parseDouble(p[2]));}
    static String serial(List<Vec> ps){var s=new StringJoiner(";");for(var p:ps)s.add(p.x()+","+p.y()+","+p.z());return s.toString();}
    public static void main(String[] args)throws Exception {
        var names=new HashMap<String,Set<String>>();for(String row:Files.readAllLines(Path.of(args[1]))){String[] p=row.split("\t");names.computeIfAbsent(p[0],k->new HashSet<>()).add(p[1]);}
        var out=new ArrayList<String>();out.add("site\tlink\tstatus\tchecked_edges\tpoints");String previous="";
        for(String row:Files.readAllLines(Path.of(args[0]))){
            String[] p=row.split("\t",-1);
            if(!previous.equals(p[0])){previous=p[0];try(var in=Files.newInputStream(Path.of(p[1]))){map=AuthoredSitePhysics.read(in,p[2],List.of(),names.getOrDefault(p[0],Set.of()));}}
            width=Double.parseDouble(p[4]);var anchors=Arrays.stream(p[5].split(";")).map(FlatRouteDesigner::parse).toList();y=anchors.get(0).y();
            sealed=new ArrayList<>();if(!p[6].isEmpty())for(String r:p[6].split(";")){double[] v=Arrays.stream(r.split(",")).mapToDouble(Double::parseDouble).toArray();sealed.add(new Rect(v[0],v[1],v[2],v[3],v[4],v[5]));}
            double[] box=Arrays.stream(p[7].split(",")).mapToDouble(Double::parseDouble).toArray();x0=Math.floor(box[0]*2)/2;z0=Math.floor(box[1]*2)/2;nx=(int)Math.ceil((box[2]-x0)*2)+1;nz=(int)Math.ceil((box[3]-z0)*2)+1;
            valid=new HashMap<>();edges=new HashMap<>();checked=0;var route=new ArrayList<Vec>();String status="REROUTED_FULL_WIDTH";
            for(int i=1;i<anchors.size();i++){
                var part=find(anchors.get(i-1),anchors.get(i));
                if(part==null){status=feet(anchors.get(i-1).x(),anchors.get(i-1).z())==null||feet(anchors.get(i).x(),anchors.get(i).z())==null?"ANCHOR_REQUIRES_CONSTRUCTION":"NO_FULL_WIDTH_ROUTE_IN_DESIGN_ENVELOPE";route.clear();break;}
                if(!route.isEmpty())route.remove(route.size()-1);route.addAll(part);
            }
            out.add(String.join("\t",p[0],p[3],status,""+checked,serial(route)));
            Files.write(Path.of(args[2]),out);System.out.println("ROUTE_DESIGN "+p[0]+" "+p[3]+" "+status+" edges="+checked+" points="+route.size());
        }
    }
}
