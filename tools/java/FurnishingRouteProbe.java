import java.nio.file.*;
import java.util.*;
import org.beckenburg.AuthoredSitePhysics;
import org.beckenburg.LevelDefinition.Vec;

/** Compare actual before/after native movement; never starts the editor/game UI. */
public final class FurnishingRouteProbe {
    public static void main(String[] args)throws Exception {
        var out=new ArrayList<String>();out.add("site\tlink\tlane\tx\ty\tz\tbaseline_valid\tnew_block\tbaseline_sweep\tnew_sweep_block");
        String site="";AuthoredSitePhysics before=null,after=null;
        var previous=new HashMap<String,Vec>();
        for(String row:Files.readAllLines(Path.of(args[0]))) {
            String[] p=row.split("\t");
            if(!site.equals(p[0])) {
                site=p[0];previous.clear();
                try(var in=Files.newInputStream(Path.of(p[1]))){before=AuthoredSitePhysics.read(in,p[2],List.of());}
                try(var in=Files.newInputStream(Path.of(p[3]))){after=AuthoredSitePhysics.read(in,p[4],List.of());}
            }
            double x=Double.parseDouble(p[6]),y=Double.parseDouble(p[7]),z=Double.parseDouble(p[8]);
            var support=before.support(x,z,y,.02);var feet=new Vec(x,support==null?y:support.y(),z);
            boolean valid=support!=null&&!before.bodyBlocked(feet,1.8,.22);
            boolean blocked=valid&&(after.support(x,z,feet.y(),.02)==null||after.bodyBlocked(feet,1.8,.22));
            String key=p[5]+":"+p[9];Vec last=previous.get(key);boolean sweep=false,sweepBlock=false;
            if(valid&&last!=null&&Math.hypot(x-last.x(),z-last.z())<.51&&before.traversable(last,1.8)&&before.traversable(feet,1.8)) {
                sweep=before.resolveMove(last,x,z,1.8)!=null;
                sweepBlock=sweep&&after.resolveMove(last,x,z,1.8)==null;
            }
            if(valid)previous.put(key,feet);else previous.remove(key);
            out.add(String.join("\t",site,p[5],p[9],p[6],p[7],p[8],""+valid,""+blocked,""+sweep,""+sweepBlock));
        }
        Files.write(Path.of(args[1]),out);
    }
}
