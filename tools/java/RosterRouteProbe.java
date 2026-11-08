import java.nio.file.*;
import java.util.*;
import org.beckenburg.AuthoredSitePhysics;
import org.beckenburg.LevelDefinition.Vec;

/** Read-only probe of candidate map geometry using the shipped native kernel. */
public final class RosterRouteProbe {
    private static String overhead(AuthoredSitePhysics physics,double x,double y,double z) {
        for(int i=0;i<physics.triangleCount();i++) {
            var t=physics.triangle(i); var a=t.a();var b=t.b();var c=t.c();
            double dx=b.x()-a.x(),dz=b.z()-a.z(),ex=c.x()-a.x(),ez=c.z()-a.z();
            double den=dx*ez-dz*ex;if(Math.abs(den)<1e-12)continue;
            double u=((x-a.x())*ez-(z-a.z())*ex)/den;
            double v=(dx*(z-a.z())-dz*(x-a.x()))/den;
            if(u<0||v<0||u+v>1)continue;
            double hit=a.y()+u*(b.y()-a.y())+v*(c.y()-a.y());
            if(hit>y+.3&&hit<y+1.8)return physics.componentName(t.component());
        }
        return "-";
    }
    public static void main(String[] args) throws Exception {
        var output=new ArrayList<String>();
        output.add("site\tlink\tcomponent\tx\ty\tz\tsupported\tspine_clear\tbody_clear\ttraversable\toverhead_component");
        String current=""; AuthoredSitePhysics physics=null;
        var steps=new HashMap<String,Set<String>>();
        if(args.length>2)for(String row:Files.readAllLines(Path.of(args[2]))){String[] p=row.split("\t");if(!steps.computeIfAbsent(p[0],k->new HashSet<>()).add(p[1]))throw new AssertionError("Duplicate stair identity");}
        for(String row:Files.readAllLines(Path.of(args[0]))) {
            String[] p=row.split("\t");
            if(!current.equals(p[0])) {
                current=p[0];
                try(var in=Files.newInputStream(Path.of(p[1]))) {
                    physics=AuthoredSitePhysics.read(in,p[2],List.of(),steps.getOrDefault(current,Set.of()));
                }
            }
            double x=Double.parseDouble(p[5]),y=Double.parseDouble(p[6]),z=Double.parseDouble(p[7]);
            var feet=new Vec(x,y,z);
            boolean supported=physics.support(x,z,y,.006)!=null;
            boolean clear=physics.clearSight(new Vec(x,y+.3,z),new Vec(x,y+1.8,z));
            output.add(String.join("\t",p[0],p[3],p[4],p[5],p[6],p[7],
                ""+supported,""+clear,""+!physics.bodyBlocked(feet,1.8,.22),""+physics.traversable(feet,1.8),
                clear?"-":overhead(physics,x,y,z)));
        }
        Files.write(Path.of(args[1]),output);
    }
}
