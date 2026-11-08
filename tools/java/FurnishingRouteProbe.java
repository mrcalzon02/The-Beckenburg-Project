import java.nio.file.*;
import java.util.*;
import org.beckenburg.AuthoredSitePhysics;
import org.beckenburg.LevelDefinition.Vec;

/** Compare actual before/after native movement; never starts the editor/game UI. */
public final class FurnishingRouteProbe {
    public static void main(String[] args)throws Exception {
        var out=new ArrayList<String>();out.add("site\tlink\tlane\tx\ty\tz\tbaseline_valid\tnew_block\tbaseline_sweep\tnew_sweep_block\tbefore_ground_y\tafter_ground_y");
        String site="";AuthoredSitePhysics before=null,after=null;
        var steps=new HashMap<String,Set<String>>();
        if(args.length>2)for(String row:Files.readAllLines(Path.of(args[2]))){String[] p=row.split("\t");if(!steps.computeIfAbsent(p[0],k->new HashSet<>()).add(p[1]))throw new AssertionError("Duplicate stair identity");}
        var previous=new HashMap<String,Vec[]>();
        for(String row:Files.readAllLines(Path.of(args[0]))) {
            String[] p=row.split("\t");
            if(!site.equals(p[0])) {
                site=p[0];previous.clear();
                try(var in=Files.newInputStream(Path.of(p[1]))){before=AuthoredSitePhysics.read(in,p[2],List.of(),steps.getOrDefault(site+":before",Set.of()));}
                try(var in=Files.newInputStream(Path.of(p[3]))){after=AuthoredSitePhysics.read(in,p[4],List.of(),steps.getOrDefault(site+":after",Set.of()));}
            }
            double x=Double.parseDouble(p[6]),y=Double.parseDouble(p[7]),z=Double.parseDouble(p[8]);
            var support=before.support(x,z,y,.02);var feet=new Vec(x,support==null?y:support.y(),z);
            boolean valid=support!=null&&!before.bodyBlocked(feet,1.8,.22);
            var afterSupport=after.support(x,z,feet.y(),.02);
            Vec afterFeet=afterSupport==null?null:new Vec(x,afterSupport.y(),z);
            boolean blocked=valid&&(afterFeet==null||after.bodyBlocked(afterFeet,1.8,.22));
            String key=p[5]+":"+p[9];Vec[] last=previous.get(key);boolean sweep=false,sweepBlock=false;
            if(valid&&last!=null&&Math.hypot(x-last[0].x(),z-last[0].z())<.51&&before.traversable(last[0],1.8)&&before.traversable(feet,1.8)) {
                sweep=before.resolveMove(last[0],x,z,1.8)!=null;
                sweepBlock=sweep&&(last[1]==null||afterFeet==null||after.resolveMove(last[1],x,z,1.8)==null);
            }
            if(valid)previous.put(key,new Vec[]{feet,blocked?null:afterFeet});else previous.remove(key);
            out.add(String.join("\t",site,p[5],p[9],p[6],p[7],p[8],""+valid,""+blocked,""+sweep,""+sweepBlock,""+feet.y(),afterFeet==null?"-":""+afterFeet.y()));
        }
        Files.write(Path.of(args[1]),out);
    }
}
