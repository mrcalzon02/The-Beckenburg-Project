package org.beckenburg.editor;

import java.util.*;

/** Triangle-level orthographic queries derived directly from native indexed authoring state. */
public final class EditorMeshPicking {
    public record Point(double x,double y,double depth){}
    public enum Projection {
        ISOMETRIC,FRONT,TOP,SIDE;
        public Point project(double x,double y,double z){return switch(this){
            case ISOMETRIC->new Point(.83*x-.55*z,-(.95*y+.24*x+.35*z),-.5225*x+.4225*y-.7885*z);
            case FRONT->new Point(x,-y,-z);
            case TOP->new Point(x,z,y);
            case SIDE->new Point(z,-y,x);
        };}
    }
    public record Hit(long faceId,long vertexId,long edgeA,long edgeB,double depth,double u,double v){}
    private EditorMeshPicking(){}
    private static double distance(Point a,double x,double y){return Math.hypot(a.x()-x,a.y()-y);}
    private static double segment(Point a,Point b,double x,double y){
        double dx=b.x()-a.x(),dy=b.y()-a.y(),length=dx*dx+dy*dy;
        double t=length==0?0:Math.max(0,Math.min(1,((x-a.x())*dx+(y-a.y())*dy)/length));
        return Math.hypot(x-a.x()-t*dx,y-a.y()-t*dy);
    }
    /** Returns the nearest actual triangle, then the closest projected vertex/edge on that triangle.
     * Coordinates are projection-space values, before the UI's scale and translation.
     */
    public static Optional<Hit> pick(EditorTopologyProject.State state,Projection projection,double x,double y){
        Objects.requireNonNull(state);Objects.requireNonNull(projection);
        if(!Double.isFinite(x)||!Double.isFinite(y))throw new IllegalArgumentException("Nonfinite pick");
        var points=new HashMap<Long,Point>();for(var v:state.vertices())points.put(v.id(),projection.project(v.x(),v.y(),v.z()));
        Hit hit=null;
        for(var face:state.faces()){
            var corners=List.of(face.a(),face.b(),face.c());var p=new Point[]{points.get(face.a().vertex()),points.get(face.b().vertex()),points.get(face.c().vertex())};
            double ax=p[0].x()-p[2].x(),ay=p[0].y()-p[2].y(),bx=p[1].x()-p[2].x(),by=p[1].y()-p[2].y();
            double det=ax*by-ay*bx,scale=Math.hypot(ax,ay)*Math.hypot(bx,by);
            if(!Double.isFinite(det)||Math.abs(det)<=scale*1e-12)continue;
            double dx=x-p[2].x(),dy=y-p[2].y(),a=(dx*by-dy*bx)/det,b=(ax*dy-ay*dx)/det,c=1-a-b;
            if(a< -1e-10||b< -1e-10||c< -1e-10)continue;
            double depth=a*p[0].depth()+b*p[1].depth()+c*p[2].depth();
            if(!Double.isFinite(depth)||hit!=null&&(depth<hit.depth()||depth==hit.depth()&&face.id()>hit.faceId()))continue;
            int vertex=0,edge=0;for(int i=1;i<3;i++){
                if(distance(p[i],x,y)<distance(p[vertex],x,y))vertex=i;
                if(segment(p[i],p[(i+1)%3],x,y)<segment(p[edge],p[(edge+1)%3],x,y))edge=i;
            }
            long edgeA=corners.get(edge).vertex(),edgeB=corners.get((edge+1)%3).vertex();
            hit=new Hit(face.id(),corners.get(vertex).vertex(),Math.min(edgeA,edgeB),Math.max(edgeA,edgeB),depth,
                a*face.a().u()+b*face.b().u()+c*face.c().u(),a*face.a().v()+b*face.b().v()+c*face.c().v());
        }
        return Optional.ofNullable(hit);
    }
}
