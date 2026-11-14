import java.nio.file.*;
import java.util.*;
import org.beckenburg.AuthoredSitePhysics;
import org.beckenburg.LevelDefinition.Vec;

/** Actual native opacity through newly restored structural plates, both ways. */
public final class FloorClosureOpticalProbe {
    public static void main(String[] args)throws Exception {
        var output=new ArrayList<String>();
        output.add("site\tpart\tx\ty\tz\tbefore_clear_down\tbefore_clear_up\tafter_clear_down\tafter_clear_up");
        String site="";AuthoredSitePhysics before=null,after=null;int repaired=0,existing=0;
        for(String row:Files.readAllLines(Path.of(args[0]))) {
            if(row.isBlank())continue;String[] p=row.split("\t");
            if(!site.equals(p[0])) {
                site=p[0];
                try(var input=Files.newInputStream(Path.of(p[1]))){before=AuthoredSitePhysics.read(input,p[2],List.of(),Set.of());}
                try(var input=Files.newInputStream(Path.of(p[3]))){after=AuthoredSitePhysics.read(input,p[4],List.of(),Set.of());}
            }
            double x=Double.parseDouble(p[6]),y=Double.parseDouble(p[7]),z=Double.parseDouble(p[8]);
            var above=new Vec(x,y+.03,z);var below=new Vec(x,y-.19,z);
            boolean bd=before.clearSight(above,below),bu=before.clearSight(below,above),ad=after.clearSight(above,below),au=after.clearSight(below,above);
            if(ad||au)throw new AssertionError("Restored structural optical leak: "+site+" "+p[5]+" "+x+","+z);
            if(bd||bu)repaired++;else existing++;
            output.add(String.join("\t",site,p[5],p[6],p[7],p[8],""+bd,""+bu,""+ad,""+au));
        }
        if(repaired==0)throw new AssertionError("No measured prior optical leak repaired");
        Files.write(Path.of(args[1]),output);
        System.out.println("PASS native structural opacity both directions; repaired prior open rays="+repaired+", prior already-occluded samples="+existing+"; installed lighting/runtime not inferred");
    }
}
