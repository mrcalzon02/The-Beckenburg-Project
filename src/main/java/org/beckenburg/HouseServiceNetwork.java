package org.beckenburg;
import java.util.*;
import org.beckenburg.LevelDefinition.*;
/** Closed textured conduit geometry and collision share serialized authored nodes. */
final class HouseServiceNetwork {
    private HouseServiceNetwork(){}
    private static final Map<List<Socket>,HouseRebuildEnvelope.Geometry> CACHE=new LinkedHashMap<>(16,.75f,true){
        @Override protected boolean removeEldestEntry(Map.Entry<List<Socket>,HouseRebuildEnvelope.Geometry> entry){return size()>32;}
    };
    static HouseRebuildEnvelope.Geometry geometry(LevelDefinition level){return geometry(level.sockets());}
    static synchronized HouseRebuildEnvelope.Geometry geometry(List<Socket> sockets){
        var nodes=sockets.stream().filter(s->s.type().equals("service-node")).toList();
        return nodes.isEmpty()?null:CACHE.computeIfAbsent(nodes,HouseServiceNetwork::build);
    }
    static String fingerprint(List<Socket> sockets){var g=geometry(sockets);return g==null?"":g.digest();}
    static HouseRebuildEnvelope.Geometry build(List<Socket> nodes){
        var materials=new HashMap<String,Integer>();
        for(var t:GlbProps.model("level_trace_electric_branch").triangles())for(String name:List.of("ivory","black"))if(GlbProps.materialName(t.material()).endsWith(" "+name))materials.put(name,t.material());
        var runs=new TreeMap<String,List<Socket>>();for(var s:nodes){var values=s.style().split("\\|");if(values.length!=5||!values[4].equals("conduit-24mm-v1"))throw new IllegalArgumentException("Unknown service definition");runs.computeIfAbsent(values[0],k->new ArrayList<>()).add(s);}
        var spans=new TreeMap<String,List<Span>>();var builder=new HouseRebuildEnvelope.Builder();int cable=0;
        for(var entry:runs.entrySet()){
            var run=entry.getValue();run.sort(Comparator.comparingInt(s->Integer.parseInt(s.style().split("\\|")[1])));
            for(int i=1;i<run.size();i++){var a=run.get(i-1);var c=run.get(i);double[] aa={a.x(),a.y(),a.z()},bb={c.x(),c.y(),c.z()};int axis=-1;
                if(a.style().split("\\|")[3].equals("ground-cable")){cable(builder,"ground-feed-"+(cable++),a,c,Objects.requireNonNull(materials.get(a.style().split("\\|")[2])));continue;}
                for(int n=0;n<3;n++)if(Math.abs(aa[n]-bb[n])>1e-7){if(axis>=0)throw new IllegalArgumentException("Service needs orthogonal elbows "+entry.getKey());axis=n;}
                if(axis<0)continue;String finish=a.style().split("\\|")[2];String key=axis+"|"+finish;
                for(int n=0;n<3;n++)if(n!=axis)key+="|"+Math.round(aa[n]*1e6);
                spans.computeIfAbsent(key,k->new ArrayList<>()).add(new Span(axis,aa,Math.min(aa[axis],bb[axis]),Math.max(aa[axis],bb[axis]),finish));
            }
        }
        int index=0;
        for(var group:spans.values()){
            group.sort(Comparator.comparingDouble(Span::low));var first=group.get(0);double low=first.low(),high=first.high();
            for(int i=1;i<=group.size();i++){
                if(i<group.size()&&group.get(i).low()<=high+1e-7){high=Math.max(high,group.get(i).high());continue;}
                double[] lo=first.point().clone(),hi=first.point().clone();lo[first.axis()]=low;hi[first.axis()]=high;double r=.012;
                builder.box("shared-conduit-"+(index++),lo[0]-r,lo[1]-r,lo[2]-r,hi[0]+r,hi[1]+r,hi[2]+r,Objects.requireNonNull(materials.get(first.finish())),-1,false);
                if(i<group.size()){first=group.get(i);low=first.low();high=first.high();}
            }
        }
        return builder.finish(0,0,1,1,1,2);
    }
    private record Span(int axis,double[] point,double low,double high,String finish){}
    /** Vertical cross section follows the actual terrain grade; original material and shared physics. */
    private static void cable(HouseRebuildEnvelope.Builder b,String name,Socket a,Socket c,int material){
        double dx=c.x()-a.x(),dz=c.z()-a.z(),length=Math.hypot(dx,dz);if(length<1e-8)throw new IllegalArgumentException("Ground cable requires a horizontal run");
        double nx=-dz/length*.012,nz=dx/length*.012;
        Vec[] v=new Vec[8];for(int end=0;end<2;end++){var p=end==0?a:c;
            v[end*4]=new Vec(p.x()-nx,p.y()-.012,p.z()-nz);v[end*4+1]=new Vec(p.x()+nx,p.y()-.012,p.z()+nz);
            v[end*4+2]=new Vec(p.x()+nx,p.y()+.012,p.z()+nz);v[end*4+3]=new Vec(p.x()-nx,p.y()+.012,p.z()-nz);}
        b.triangles=new ArrayList<>();b.quad(v[3],v[2],v[1],v[0],material,true);b.quad(v[4],v[5],v[6],v[7],material,true);
        for(int j=0;j<4;j++)b.quad(v[j],v[(j+1)%4],v[4+(j+1)%4],v[4+j],material,true);
        b.parts.add(new HouseRebuildEnvelope.Part(name,b.triangles,false));
    }
}
