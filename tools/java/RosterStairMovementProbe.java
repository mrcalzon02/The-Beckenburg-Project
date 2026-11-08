import java.nio.file.*;
import java.util.*;
import org.beckenburg.AuthoredSitePhysics;
import org.beckenburg.LevelDefinition.Vec;

/** Actual 5cm movement-kernel sweeps through authored flights; no app/editor UI. */
public final class RosterStairMovementProbe {
    private static String chain(AuthoredSitePhysics map,List<Vec> points){
        Vec start=points.get(0);var support=map.support(start.x(),start.z(),start.y(),.25);
        if(support==null)return "START_UNSUPPORTED";
        Vec at=new Vec(start.x(),support.y(),start.z());
        if(!map.traversable(at,1.8))return "START_BLOCKED";
        for(int segment=1;segment<points.size();segment++){
            Vec from=points.get(segment-1),to=points.get(segment);
            int n=Math.max(1,(int)Math.ceil(Math.hypot(to.x()-from.x(),to.z()-from.z())/.1));
            for(int i=1;i<=n;i++){
                Vec next=map.resolveMove(at,from.x()+(to.x()-from.x())*i/n,from.z()+(to.z()-from.z())*i/n,1.8);
                if(next==null)return "BLOCKED_SEGMENT_"+(segment-1)+"_AT_"+i+"_OF_"+n;
                at=next;
            }
        }
        return Math.abs(at.y()-points.get(points.size()-1).y())<=.25+1e-6?"PASS":"WRONG_END_ELEVATION";
    }
    private static String sweep(AuthoredSitePhysics map,Vec from,Vec to){
        var support=map.support(from.x(),from.z(),from.y(),.25);
        if(support==null)return "START_UNSUPPORTED";
        Vec at=new Vec(from.x(),support.y(),from.z());
        if(!map.traversable(at,1.8))return "START_BLOCKED";
        int n=Math.max(1,(int)Math.ceil(Math.hypot(to.x()-from.x(),to.z()-from.z())/.1));
        for(int i=1;i<=n;i++){
            double x=from.x()+(to.x()-from.x())*i/n,z=from.z()+(to.z()-from.z())*i/n;
            Vec next=map.resolveMove(at,x,z,1.8);
            if(next==null)return "BLOCKED_AT_"+i+"_OF_"+n;
            at=next;
        }
        return Math.abs(at.y()-to.y())<=.25+1e-6?"PASS":"WRONG_END_ELEVATION";
    }
    public static void main(String[] args)throws Exception{
        var names=new HashMap<String,Set<String>>();
        for(String row:Files.readAllLines(Path.of(args[0]))){String[] p=row.split("\t");if(!names.computeIfAbsent(p[0],k->new HashSet<>()).add(p[1]))throw new AssertionError("Duplicate stair identity");}
        var out=new ArrayList<String>();out.add("site\tvariant\tlink\tsegment\tlegacy_up\tlegacy_down\tauthored_up\tauthored_down");
        String key="";AuthoredSitePhysics old=null,map=null;
        for(String row:Files.readAllLines(Path.of(args[1]))){
            String[] p=row.split("\t");String next=p[0]+":"+p[1];
            if(!key.equals(next)){
                key=next;try(var in=Files.newInputStream(Path.of(p[2]))){old=AuthoredSitePhysics.read(in,p[3],List.of());}
                try(var in=Files.newInputStream(Path.of(p[2]))){map=AuthoredSitePhysics.read(in,p[3],List.of(),names.getOrDefault(key,Set.of()));}
            }
            var a=new Vec(Double.parseDouble(p[6]),Double.parseDouble(p[7]),Double.parseDouble(p[8]));
            var b=new Vec(Double.parseDouble(p[9]),Double.parseDouble(p[10]),Double.parseDouble(p[11]));
            if(a.y()>b.y()){Vec swap=a;a=b;b=swap;}
            out.add(String.join("\t",p[0],p[1],p[4],p[5],sweep(old,a,b),sweep(old,b,a),sweep(map,a,b),sweep(map,b,a)));
        }
        Files.write(Path.of(args[2]),out);
        var routes=new LinkedHashMap<String,List<String[]>>();
        for(String row:Files.readAllLines(Path.of(args[4]))){String[] p=row.split("\t");routes.computeIfAbsent(p[0]+":"+p[1]+":"+p[4],k->new ArrayList<>()).add(p);}
        var chains=new ArrayList<String>();chains.add("site\tvariant\tlink\tsegments\tlegacy_forward\tlegacy_reverse\tauthored_forward\tauthored_reverse");
        for(var entry:routes.values()){
            String[] p=entry.get(0);var points=new ArrayList<Vec>();boolean contiguous=true;
            for(String[] row:entry){
                var a=new Vec(Double.parseDouble(row[6]),Double.parseDouble(row[7]),Double.parseDouble(row[8]));
                var b=new Vec(Double.parseDouble(row[9]),Double.parseDouble(row[10]),Double.parseDouble(row[11]));
                if(points.isEmpty())points.add(a);else if(!points.get(points.size()-1).equals(a))contiguous=false;
                points.add(b);
            }
            if(!contiguous)throw new AssertionError("Incomplete authored route manifest");
            try(var in=Files.newInputStream(Path.of(p[2]))){old=AuthoredSitePhysics.read(in,p[3],List.of());}
            try(var in=Files.newInputStream(Path.of(p[2]))){map=AuthoredSitePhysics.read(in,p[3],List.of(),names.getOrDefault(p[0]+":"+p[1],Set.of()));}
            var reverse=new ArrayList<>(points);Collections.reverse(reverse);
            chains.add(String.join("\t",p[0],p[1],p[4],""+entry.size(),chain(old,points),chain(old,reverse),chain(map,points),chain(map,reverse)));
        }
        Files.write(Path.of(args[3]),chains);
    }
}
