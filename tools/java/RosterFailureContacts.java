import java.nio.file.*;
import java.lang.reflect.*;
import java.util.*;
import org.beckenburg.AuthoredSitePhysics;
import org.beckenburg.LevelDefinition.Vec;

/** Read-only failed-pose diagnosis using the unchanged kernel's exact triangle distance.
 * Reflection is limited to our own private geometry helper; it changes no state.
 */
public final class RosterFailureContacts {
    private static final double R=.22,H=1.8;
    private static final Method DISTANCE;
    static {try{DISTANCE=AuthoredSitePhysics.class.getDeclaredMethod("capsuleTriangle",Vec.class,Vec.class,int.class);DISTANCE.setAccessible(true);}catch(Exception e){throw new ExceptionInInitializerError(e);}}
    private record Context(AuthoredSitePhysics map,Set<String> names,double[] tops){}
    private record Finding(Vec feet,String reason,String component){}
    private static Finding describe(Context ctx,Vec feet)throws Exception{
        var map=ctx.map;var centre=map.support(feet.x(),feet.z(),feet.y(),.006);
        if(centre==null)return new Finding(feet,"NO_CENTRE_SUPPORT","-");
        boolean stepping=ctx.names.contains(map.componentName(map.triangle(centre.triangle()).component()));
        var offsets=new double[][]{{R,0},{-R,0},{0,R},{0,-R}};var neighbours=new ArrayList<AuthoredSitePhysics.Support>();
        for(double[] d:offsets){
            var s=map.support(feet.x()+d[0],feet.z()+d[1],feet.y(),.25);
            if(s==null)return new Finding(feet,"NO_FOOT_SUPPORT_"+d[0]+"_"+d[1],"-");
            stepping|=ctx.names.contains(map.componentName(map.triangle(s.triangle()).component()));neighbours.add(s);
        }
        if(!stepping)for(double[] d:offsets)if(map.support(feet.x()+d[0],feet.z()+d[1],feet.y(),.08)==null)return new Finding(feet,"FOOT_HEIGHT_SPREAD","-");
        var a=new Vec(feet.x(),feet.y()+R,feet.z());var b=new Vec(feet.x(),feet.y()+H-R,feet.z());
        for(int i=0;i<map.triangleCount();i++){
            var t=map.triangle(i);if((t.flags()&AuthoredSitePhysics.BODY)==0)continue;
            String name=map.componentName(t.component());if(stepping&&ctx.names.contains(name)&&ctx.tops[t.component()]<=feet.y()+.25+1e-6)continue;
            if(Math.min(t.a().x(),Math.min(t.b().x(),t.c().x()))>feet.x()+R||Math.max(t.a().x(),Math.max(t.b().x(),t.c().x()))<feet.x()-R||Math.min(t.a().z(),Math.min(t.b().z(),t.c().z()))>feet.z()+R||Math.max(t.a().z(),Math.max(t.b().z(),t.c().z()))<feet.z()-R)continue;
            if((double)DISTANCE.invoke(map,a,b,i)<(R-.005)*(R-.005))return new Finding(feet,"PHYSICAL_COMPONENT",name);
        }
        return new Finding(feet,"CLOSED_SOLID_INTERIOR_OR_INVALID_POSE","-");
    }
    private static Finding sweep(Context ctx,Vec from,Vec to)throws Exception{
        var map=ctx.map;var support=map.support(from.x(),from.z(),from.y(),.25);
        if(support==null)return new Finding(from,"START_UNSUPPORTED","-");
        Vec at=new Vec(from.x(),support.y(),from.z());if(!map.traversable(at,H))return describe(ctx,at);
        int n=Math.max(1,(int)Math.ceil(Math.hypot(to.x()-from.x(),to.z()-from.z())/.1));
        for(int i=1;i<=n;i++){
            double x=from.x()+(to.x()-from.x())*i/n,z=from.z()+(to.z()-from.z())*i/n;
            Vec next=map.resolveMove(at,x,z,H);
            if(next==null){
                Vec previous=at;int m=Math.max(1,(int)Math.ceil(Math.hypot(x-at.x(),z-at.z())/.05));
                for(int j=1;j<=m;j++){
                    double sx=at.x()+(x-at.x())*j/m,sz=at.z()+(z-at.z())*j/m;
                    var s=map.support(sx,sz,previous.y(),.25);
                    if(s==null)return new Finding(new Vec(sx,previous.y(),sz),"NO_NEXT_SUPPORT","-");
                    Vec candidate=new Vec(sx,s.y(),sz);if(!map.traversable(candidate,H))return describe(ctx,candidate);previous=candidate;
                }
                throw new AssertionError("Diagnostic diverged from unchanged production sweep");
            }
            at=next;
        }
        return null;
    }
    public static void main(String[] args)throws Exception{
        var names=new HashMap<String,Set<String>>();
        for(String row:Files.readAllLines(Path.of(args[0]))){String[] p=row.split("\t");names.computeIfAbsent(p[0],k->new HashSet<>()).add(p[1]);}
        var out=new ArrayList<String>();out.add("site\tlink\tsegment\tdirection\tx\ty\tz\treason\tcomponent");String key="";Context ctx=null;
        for(String row:Files.readAllLines(Path.of(args[1]))){
            String[] p=row.split("\t");if(!p[1].equals("structure"))continue;String next=p[0]+":"+p[1];
            if(!next.equals(key)){
                key=next;Set<String> steps=names.getOrDefault(key,Set.of());AuthoredSitePhysics map;
                try(var in=Files.newInputStream(Path.of(p[2]))){map=AuthoredSitePhysics.read(in,p[3],List.of(),steps);}
                double[] tops=new double[map.componentCount()];Arrays.fill(tops,Double.NEGATIVE_INFINITY);
                for(int i=0;i<map.triangleCount();i++){var t=map.triangle(i);tops[t.component()]=Math.max(tops[t.component()],Math.max(t.a().y(),Math.max(t.b().y(),t.c().y())));}
                ctx=new Context(map,steps,tops);
            }
            var a=new Vec(Double.parseDouble(p[6]),Double.parseDouble(p[7]),Double.parseDouble(p[8]));var b=new Vec(Double.parseDouble(p[9]),Double.parseDouble(p[10]),Double.parseDouble(p[11]));if(a.y()>b.y()){Vec swap=a;a=b;b=swap;}
            for(String direction:List.of("up","down")){
                Finding f=direction.equals("up")?sweep(ctx,a,b):sweep(ctx,b,a);if(f==null)continue;
                out.add(String.join("\t",p[0],p[4],p[5],direction,""+f.feet.x(),""+f.feet.y(),""+f.feet.z(),f.reason,f.component));
            }
        }
        Files.write(Path.of(args[2]),out);System.out.println("Diagnosed "+(out.size()-1)+" failed directional flights without altering physics");
    }
}
