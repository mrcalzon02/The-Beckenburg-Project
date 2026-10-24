package org.beckenburg;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;

/** Pixel/UV agreement against independently decoded committed GLB atlas samples. */
public final class HollowCreekAuthoredSurfacesTest {
    public static void main(String[] args)throws Exception {
        var seen=new HashSet<String>();int samples=0,triangles=0;
        for(String line:Files.readAllLines(Path.of(args[0]))){
            String[] p=line.split("\t");String id=p[0];
            if(!LevelAssets.AUTHORED_SURFACES.contains(id))throw new AssertionError("Uncurated surface "+id);
            var m=GlbProps.model(id);
            if(seen.add(id)){
                if(m.triangles().size()>=50000)throw new AssertionError("Geometry budget "+id);
                triangles+=m.triangles().size();
                for(var t:m.triangles())for(var v:new GlbProps.Vertex[]{t.a(),t.b(),t.c()})
                    if(!Double.isFinite(v.x()+v.y()+v.z()+v.u()+v.v()))throw new AssertionError("Nonfinite "+id);
            }
            var t=m.triangles().get(Integer.parseInt(p[1]));
            double u=(t.a().u()+t.b().u()+t.c().u())/3,v=(t.a().v()+t.b().v()+t.c().v())/3;
            if(Math.abs(u-Double.parseDouble(p[2]))>1e-8||Math.abs(v-Double.parseDouble(p[3]))>1e-8)
                throw new AssertionError("Wrong atlas UV channel "+id);
            int expected=Integer.parseInt(p[4],16),actual=GlbProps.sample(t.material(),u,v);
            if(actual!=expected)throw new AssertionError("Atlas orientation/pixel mismatch "+id+" triangle "+p[1]);
            samples++;
        }
        if(seen.size()!=14||samples!=1008)throw new AssertionError("Incomplete family/pixel coverage");
        System.out.println("PASS 14 actual runtime GLBs; "+triangles+" triangles; 1008 independent atlas UV/pixel checks");
    }
}
