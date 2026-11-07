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
        var spans=new TreeMap<String,List<Span>>();
        for(var entry:runs.entrySet()){
            var run=entry.getValue();run.sort(Comparator.comparingInt(s->Integer.parseInt(s.style().split("\\|")[1])));
            for(int i=1;i<run.size();i++){var a=run.get(i-1);var c=run.get(i);double[] aa={a.x(),a.y(),a.z()},bb={c.x(),c.y(),c.z()};int axis=-1;
                for(int n=0;n<3;n++)if(Math.abs(aa[n]-bb[n])>1e-7){if(axis>=0)throw new IllegalArgumentException("Service needs orthogonal elbows "+entry.getKey());axis=n;}
                if(axis<0)continue;String finish=a.style().split("\\|")[2];String key=axis+"|"+finish;
                for(int n=0;n<3;n++)if(n!=axis)key+="|"+Math.round(aa[n]*1e6);
                spans.computeIfAbsent(key,k->new ArrayList<>()).add(new Span(axis,aa,Math.min(aa[axis],bb[axis]),Math.max(aa[axis],bb[axis]),finish));
            }
        }
        var builder=new HouseRebuildEnvelope.Builder();int index=0;
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
}
