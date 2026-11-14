package org.beckenburg.editor;

import java.util.*;

/** Bounded deterministic Y-up primitive previews. Applying is a separate native project transaction. */
public final class EditorMeshPrimitives {
    public enum Kind {BOX,PLANE,CYLINDER,SPHERE,TORUS}
    public record Parameters(Kind kind,double width,double height,double depth,int segments,int rings){
        public Parameters {
            Objects.requireNonNull(kind);
            for(double size:new double[]{width,height,depth})if(!Double.isFinite(size)||size<.0001||size>10000)throw new IllegalArgumentException("Primitive dimensions must be .0001 to 10000");
            if(segments<3||segments>128||rings<1||rings>64)throw new IllegalArgumentException("Primitive subdivisions exceed bounds");
            if(kind==Kind.SPHERE&&rings<2||kind==Kind.TORUS&&rings<3)throw new IllegalArgumentException("Sphere needs 2 rings; torus needs 3");
            long triangles=switch(kind){case BOX->12L*rings*rings;case PLANE->2L*rings*rings;case CYLINDER->2L*segments*(rings+1);case SPHERE->2L*segments*(rings-1);case TORUS->2L*segments*rings;};
            if(triangles>EditorMeshPipeline.MAX_TRIANGLES)throw new IllegalArgumentException("Primitive triangle budget exceeded");
        }
    }
    private EditorMeshPrimitives(){}
    private static EditorMeshPipeline.Vertex v(double x,double y,double z,double u,double v){return new EditorMeshPipeline.Vertex(x==0?0:x,y==0?0:y,z==0?0:z,u,v);}
    private static void tri(List<EditorMeshPipeline.Triangle> out,String part,EditorMeshPipeline.Vertex a,EditorMeshPipeline.Vertex b,EditorMeshPipeline.Vertex c){out.add(new EditorMeshPipeline.Triangle(part,a,b,c));}
    private static void quad(List<EditorMeshPipeline.Triangle> out,List<EditorMeshQuad> quads,String part,EditorMeshPipeline.Vertex a,EditorMeshPipeline.Vertex b,EditorMeshPipeline.Vertex c,EditorMeshPipeline.Vertex d){quads.add(new EditorMeshQuad(out.size(),out.size()+1));tri(out,part,a,b,c);tri(out,part,a,c,d);}
    private static double angle(int i,int count){return (i%count)*2*Math.PI/count;}
    private static EditorMeshPipeline.Vertex ring(Parameters p,int i,int j,boolean sphere){
        double a=angle(i,p.segments()),t=(double)j/p.rings();
        double radius=sphere?Math.sin(t*Math.PI):1,y=sphere?Math.cos(t*Math.PI)*.5:t-.5;
        if(sphere&&(j==0||j==p.rings()))radius=0;
        return v(Math.cos(a)*radius*p.width()*.5,y*p.height(),Math.sin(a)*radius*p.depth()*.5,(double)i/p.segments(),t);
    }
    private static EditorMeshPipeline.Vertex torus(Parameters p,int i,int j){
        double a=angle(i,p.segments()),b=angle(j,p.rings()),r=.375+.125*Math.cos(b);
        return v(Math.cos(a)*r*p.width(),Math.sin(b)*p.height()*.5,Math.sin(a)*r*p.depth(),(double)i/p.segments(),(double)j/p.rings());
    }
    public static EditorMeshPipeline.Mesh preview(Parameters p){
        Objects.requireNonNull(p);String part=p.kind().name().toLowerCase(Locale.ROOT);var out=new ArrayList<EditorMeshPipeline.Triangle>();
        var quads=new ArrayList<EditorMeshQuad>();
        int n=p.rings(),s=p.segments();
        switch(p.kind()){
            case BOX,PLANE->{
                // Tangent cross bitangent points out of each face; shared grid coordinates are exact.
                int[][] frames=p.kind()==Kind.PLANE?new int[][]{{1,1,2,0}}:new int[][]{{0,1,1,2},{0,-1,2,1},{1,1,2,0},{1,-1,0,2},{2,1,0,1},{2,-1,1,0}};
                double[] sizes={p.width(),p.height(),p.depth()};
                for(int[] f:frames)for(int j=0;j<n;j++)for(int i=0;i<n;i++){
                    var corners=new EditorMeshPipeline.Vertex[4];int[][] steps={{i,j},{i+1,j},{i+1,j+1},{i,j+1}};
                    for(int k=0;k<4;k++){double[] xyz=new double[3];xyz[f[0]]=p.kind()==Kind.PLANE?0:f[1]*sizes[f[0]]*.5;
                        double u=(double)steps[k][0]/n,w=(double)steps[k][1]/n;xyz[f[2]]=(u-.5)*sizes[f[2]];xyz[f[3]]=(w-.5)*sizes[f[3]];
                        corners[k]=v(xyz[0],xyz[1],xyz[2],u,w);}
                    quad(out,quads,part,corners[0],corners[1],corners[2],corners[3]);
                }
            }
            case CYLINDER,SPHERE->{
                boolean sphere=p.kind()==Kind.SPHERE;
                for(int j=0;j<n;j++)for(int i=0;i<s;i++){
                    var a=ring(p,i,j,sphere);var b=ring(p,i+1,j,sphere);var c=ring(p,i+1,j+1,sphere);var d=ring(p,i,j+1,sphere);
                    if(sphere){if(j>0 && j<n-1)quad(out,quads,part,a,b,c,d);else if(j>0)tri(out,part,a,b,c);else tri(out,part,a,c,d);}
                    else quad(out,quads,part,a,d,c,b);
                }
                if(!sphere)for(int i=0;i<s;i++)for(int j:new int[]{0,n}){
                    var a=ring(p,i,j,false);var b=ring(p,i+1,j,false);var center=v(0,(j==0?-.5:.5)*p.height(),0,.5,.5);
                    a=v(a.x(),a.y(),a.z(),a.x()/p.width()+.5,a.z()/p.depth()+.5);b=v(b.x(),b.y(),b.z(),b.x()/p.width()+.5,b.z()/p.depth()+.5);
                    if(j==0)tri(out,part,center,a,b);else tri(out,part,center,b,a);
                }
            }
            case TORUS->{for(int j=0;j<n;j++)for(int i=0;i<s;i++)quad(out,quads,part,torus(p,i,j),torus(p,i,j+1),torus(p,i+1,j+1),torus(p,i+1,j));}
        }
        if (quads.isEmpty()) return new EditorMeshPipeline.Mesh(part,out,0);
        var positions=new HashMap<EditorMeshTopology.Vertex,Long>();
        var ids=new ArrayList<Long>(out.size()*3);
        for(var triangle:out)for(var v:List.of(triangle.a(),triangle.b(),triangle.c())) {
            var position=new EditorMeshTopology.Vertex(part,v.x(),v.y(),v.z());
            ids.add(positions.computeIfAbsent(position,k->(long)positions.size()));
        }
        return new EditorMeshPipeline.Mesh(part,out,0,ids,List.of(),null,List.of(),Map.of(),quads);
    }
}
