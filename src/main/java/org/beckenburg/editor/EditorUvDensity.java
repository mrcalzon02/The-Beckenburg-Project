package org.beckenburg.editor;

import java.util.Optional;

/** Area-equivalent texels per model unit and principal stretch ratio for a native triangle. */
public record EditorUvDensity(double texelsPerUnit,double stretch) {
    private record Map2D(double length,double along,double altitude,double uu,double uv,double vu,double vv) {
        double determinant(){return uu*vv-vu*uv;}
        EditorUvDensity density(){
            double det=Math.abs(determinant());
            if(det==0)return new EditorUvDensity(0,Double.POSITIVE_INFINITY);
            double largest=.5*Math.hypot(uu+vv,vu-uv)+.5*Math.hypot(uu-vv,vu+uv);
            double density=Math.sqrt(det),relative=largest/density;
            return new EditorUvDensity(density,Math.max(1,relative*relative));
        }
    }
    private static Optional<Map2D> map(EditorMeshPipeline.Triangle face,int width,int height){
        if(width<1||height<1)throw new IllegalArgumentException("Texture dimensions must be positive");
        var a=face.a();var b=face.b();var c=face.c();
        double x=b.x()-a.x(),y=b.y()-a.y(),z=b.z()-a.z();
        double bx=c.x()-a.x(),by=c.y()-a.y(),bz=c.z()-a.z();
        double length=Math.hypot(Math.hypot(x,y),z);
        if(!(length>0) || !Double.isFinite(length))return Optional.empty();
        x/=length;y/=length;z/=length;
        double along=x*bx+y*by+z*bz;
        double altitude=Math.hypot(Math.hypot(y*bz-z*by,z*bx-x*bz),x*by-y*bx);
        if(!(altitude>0) || !Double.isFinite(altitude))return Optional.empty();
        double u1=(b.u()-a.u())*width,v1=(b.v()-a.v())*height;
        double u2=(c.u()-a.u())*width,v2=(c.v()-a.v())*height;
        // Columns of the affine UV map in an orthonormal basis on the face.
        double j00=u1/length,j10=v1/length;
        double j01=(u2-j00*along)/altitude,j11=(v2-j10*along)/altitude;
        var map=new Map2D(length,along,altitude,j00,j01,j10,j11);
        return Double.isFinite(map.determinant())?Optional.of(map):Optional.empty();
    }
    public static Optional<EditorUvDensity> measure(EditorMeshPipeline.Triangle face,int width,int height){
        return map(face,width,height).map(Map2D::density);
    }
    static EditorTopologyProject.Face correct(EditorTopologyProject.Face face,EditorMeshPipeline.Triangle geometry,int width,int height){
        requireUnpinned(face);
        var m=map(geometry,width,height).orElseThrow(()->new IllegalArgumentException("Degenerate geometry cannot be relaxed"));
        var density=m.density();
        if(density.texelsPerUnit()==0)throw new IllegalArgumentException("Collapsed UVs require an initial mapping");
        if(density.stretch()<=1+1e-10)throw new IllegalArgumentException("Face already has uniform texel scale");
        // Orthogonal part of the UV Jacobian, preserving reflection for mirrored islands.
        double sign=Math.signum(m.determinant());
        double angle=Math.atan2(m.vu()-sign*m.uv(),m.uu()+sign*m.vv());
        double cos=Math.cos(angle),sin=Math.sin(angle);
        var corners=new EditorTopologyProject.Corner[]{face.a(),face.b(),face.c()};
        double centerU=face.a().u()/3+face.b().u()/3+face.c().u()/3;
        double centerV=face.a().v()/3+face.b().v()/3+face.c().v()/3;
        double centerX=(m.length()+m.along())/3,centerY=sign*m.altitude()/3;
        for(int i=0;i<3;i++){
            double x=(i==0?0:i==1?m.length():m.along())-centerX;
            double y=(i==2?sign*m.altitude():0)-centerY;
            var old=corners[i];
            corners[i]=new EditorTopologyProject.Corner(old.vertex(),centerU+density.texelsPerUnit()*(cos*x-sin*y)/width,
                centerV+density.texelsPerUnit()*(sin*x+cos*y)/height,old.normal());
        }
        var corrected=new EditorMeshPipeline.Triangle(face.part(),withUv(geometry.a(),corners[0]),withUv(geometry.b(),corners[1]),
            withUv(geometry.c(),corners[2]),face.material());
        var result=map(corrected,width,height).orElseThrow(()->new IllegalArgumentException("Corrected UVs exceed numeric range"));
        var measured=result.density();
        if(Math.signum(result.determinant())!=sign || measured.stretch()>1+1e-7 ||
            Math.abs(measured.texelsPerUnit()/density.texelsPerUnit()-1)>1e-7)
            throw new IllegalArgumentException("UV coordinates cannot represent the requested correction");
        return new EditorTopologyProject.Face(face.id(),face.part(),corners[0],corners[1],corners[2],face.material());
    }
    private static EditorMeshPipeline.Vertex withUv(EditorMeshPipeline.Vertex v,EditorTopologyProject.Corner c){
        return new EditorMeshPipeline.Vertex(v.x(),v.y(),v.z(),c.u(),c.v(),v.normal());
    }
    /** Minimize area-weighted conformal stretch over unit-determinant affine changes of the whole island. */
    static java.util.List<EditorTopologyProject.Face> balance(java.util.List<EditorTopologyProject.Face> faces,int[] island,
            java.util.function.Function<EditorTopologyProject.Face,EditorMeshPipeline.Triangle> geometry,int width,int height){
        double xx=0,xy=0,yy=0,area=0,centerU=0,centerV=0;
        for(int index:island){
            var face=faces.get(index);
            requireUnpinned(face);
            var m=map(geometry.apply(face),width,height).orElseThrow(()->new IllegalArgumentException("Island has degenerate geometry"));
            double det=Math.abs(m.determinant());
            if(det==0)throw new IllegalArgumentException("Island contains collapsed UVs");
            double world=m.length()*m.altitude(),weight=world/det;
            xx+=weight*(m.uu()*m.uu()+m.uv()*m.uv());
            xy+=weight*(m.uu()*m.vu()+m.uv()*m.vv());
            yy+=weight*(m.vu()*m.vu()+m.vv()*m.vv());
            area+=world;
            centerU+=world*(face.a().u()/3+face.b().u()/3+face.c().u()/3);
            centerV+=world*(face.a().v()/3+face.b().v()/3+face.c().v()/3);
        }
        centerU/=area;centerV/=area;
        double scale=Math.max(xx,yy);
        xx/=scale;xy/=scale;yy/=scale;
        double largest=(xx+yy+Math.hypot(xx-yy,2*xy))/2;
        double smallest=(xx*yy-xy*xy)/largest;
        double factor=Math.pow(smallest/largest,.25);
        if(!(factor>0) || !Double.isFinite(centerU) || !Double.isFinite(centerV))
            throw new IllegalArgumentException("Island distortion exceeds numeric range");
        if(factor>=1-1e-10)throw new IllegalArgumentException("Island has no common stretch direction to correct");
        double angle=.5*Math.atan2(2*xy,xx-yy),cos=Math.cos(angle),sin=Math.sin(angle);
        double a=factor*cos*cos+sin*sin/factor,b=(factor-1/factor)*sin*cos,d=factor*sin*sin+cos*cos/factor;
        var output=new java.util.ArrayList<>(faces);
        for(int index:island){
            var face=faces.get(index);
            var corners=new EditorTopologyProject.Corner[]{face.a(),face.b(),face.c()};
            for(int i=0;i<3;i++){
                var c=corners[i];double x=(c.u()-centerU)*width,y=(c.v()-centerV)*height;
                double u=centerU+(a*x+b*y)/width,v=centerV+(b*x+d*y)/height;
                if(!Double.isFinite(u)||!Double.isFinite(v)||Math.abs(u)>1e9||Math.abs(v)>1e9)
                    throw new IllegalArgumentException("Island correction exceeds native UV range");
                corners[i]=new EditorTopologyProject.Corner(c.vertex(),u,v,c.normal());
            }
            var next=new EditorTopologyProject.Face(face.id(),face.part(),corners[0],corners[1],corners[2],face.material());
            double ratio=signedUvArea(next)/signedUvArea(face);
            if(!Double.isFinite(ratio) || Math.abs(ratio-1)>1e-7)
                throw new IllegalArgumentException("Island correction cannot preserve signed UV areas");
            output.set(index,next);
        }
        return output;
    }
    private static double signedUvArea(EditorTopologyProject.Face f){
        return (f.b().u()-f.a().u())*(f.c().v()-f.a().v())-(f.b().v()-f.a().v())*(f.c().u()-f.a().u());
    }
    private static void requireUnpinned(EditorTopologyProject.Face f){
        if(f.a().pinned()||f.b().pinned()||f.c().pinned())
            throw new IllegalArgumentException("Unpin the UVs before changing the island transform");
    }
}
