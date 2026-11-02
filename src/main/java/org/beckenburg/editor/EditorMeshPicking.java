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
        return prepare(state,projection).pick(x,y);
    }
    public static Prepared prepare(EditorTopologyProject.State state,Projection projection){return new Prepared(state,projection);}
    /** Immutable spatial index of projected native triangles, never an editable scene. */
    public static final class Prepared {
        private static final int MAX_TESTS=5_000_000;
        private final EditorTopologyProject.State state;
        private final Map<Long,Point> points=new HashMap<>();
        private final Triangle[] triangles;
        private final List<Node> nodes=new ArrayList<>();
        private record Triangle(EditorTopologyProject.Face face,Point a,Point b,Point c,double minX,double minY,double maxX,double maxY){}
        private record Node(double minX,double minY,double maxX,double maxY,int from,int to,int left,int right){}
        private Prepared(EditorTopologyProject.State state,Projection projection){
            this.state=Objects.requireNonNull(state);Objects.requireNonNull(projection);
            for(var v:state.vertices())points.put(v.id(),projection.project(v.x(),v.y(),v.z()));
            var all=new ArrayList<Triangle>();
            for(var f:state.faces()){
                var a=points.get(f.a().vertex());var b=points.get(f.b().vertex());var c=points.get(f.c().vertex());
                double minX=Math.min(a.x(),Math.min(b.x(),c.x())),minY=Math.min(a.y(),Math.min(b.y(),c.y()));
                double maxX=Math.max(a.x(),Math.max(b.x(),c.x())),maxY=Math.max(a.y(),Math.max(b.y(),c.y()));
                if(Double.isFinite(minX)&&Double.isFinite(minY)&&Double.isFinite(maxX)&&Double.isFinite(maxY))all.add(new Triangle(f,a,b,c,minX,minY,maxX,maxY));
            }
            triangles=all.toArray(Triangle[]::new);if(triangles.length>0)build(0,triangles.length);
        }
        private int build(int from,int to){
            double minX=Double.POSITIVE_INFINITY,minY=minX,maxX=Double.NEGATIVE_INFINITY,maxY=maxX;
            for(int i=from;i<to;i++){var t=triangles[i];minX=Math.min(minX,t.minX());minY=Math.min(minY,t.minY());maxX=Math.max(maxX,t.maxX());maxY=Math.max(maxY,t.maxY());}
            int index=nodes.size();nodes.add(null);int left=-1,right=-1;
            if(to-from>8){
                boolean x=maxX-minX>=maxY-minY;
                Arrays.sort(triangles,from,to,Comparator.comparingDouble(t->x?t.minX()*.5+t.maxX()*.5:t.minY()*.5+t.maxY()*.5));
                int mid=(from+to)>>>1;left=build(from,mid);right=build(mid,to);
            }
            nodes.set(index,new Node(minX,minY,maxX,maxY,from,to,left,right));return index;
        }
        private static void finite(double value){if(!Double.isFinite(value))throw new IllegalArgumentException("Nonfinite pick");}
        private static double tolerance(double value){return 64*Math.ulp(Math.abs(value))+1e-10;}
        private Hit query(int index,double x,double y,Hit hit,int[] budget){
            var node=nodes.get(index);double ex=tolerance(x),ey=tolerance(y);
            if(x<node.minX()-ex||x>node.maxX()+ex||y<node.minY()-ey||y>node.maxY()+ey)return hit;
            if(node.left()>=0)return query(node.right(),x,y,query(node.left(),x,y,hit,budget),budget);
            for(int i=node.from();i<node.to();i++){
                if(++budget[0]>MAX_TESTS)throw new IllegalArgumentException("Selection geometry work budget exceeded; select a smaller region");
                var t=triangles[i];var face=t.face();var p=new Point[]{t.a(),t.b(),t.c()};
                double ax=p[0].x()-p[2].x(),ay=p[0].y()-p[2].y(),bx=p[1].x()-p[2].x(),by=p[1].y()-p[2].y();
                double det=ax*by-ay*bx,scale=Math.hypot(ax,ay)*Math.hypot(bx,by);
                if(!Double.isFinite(det)||Math.abs(det)<=scale*1e-12)continue;
                double dx=x-p[2].x(),dy=y-p[2].y(),a=(dx*by-dy*bx)/det,b=(ax*dy-ay*dx)/det,c=1-a-b;
                if(a< -1e-10||b< -1e-10||c< -1e-10)continue;
                double depth=a*p[0].depth()+b*p[1].depth()+c*p[2].depth();
                if(!Double.isFinite(depth)||hit!=null&&(depth<hit.depth()||depth==hit.depth()&&face.id()>hit.faceId()))continue;
                int vertex=0,edge=0;for(int j=1;j<3;j++){
                    if(distance(p[j],x,y)<distance(p[vertex],x,y))vertex=j;
                    if(segment(p[j],p[(j+1)%3],x,y)<segment(p[edge],p[(edge+1)%3],x,y))edge=j;
                }
                var corners=List.of(face.a(),face.b(),face.c());long edgeA=corners.get(edge).vertex(),edgeB=corners.get((edge+1)%3).vertex();
                hit=new Hit(face.id(),corners.get(vertex).vertex(),Math.min(edgeA,edgeB),Math.max(edgeA,edgeB),depth,
                    a*face.a().u()+b*face.b().u()+c*face.c().u(),a*face.a().v()+b*face.b().v()+c*face.c().v());
            }return hit;
        }
        public Optional<Hit> pick(double x,double y){finite(x);finite(y);return nodes.isEmpty()?Optional.empty():Optional.ofNullable(query(0,x,y,null,new int[1]));}
        /** Exact projected vertex inclusion. Visible-only selection tests each candidate against actual nearest triangles. */
        public Set<Long> verticesInBox(double minX,double minY,double maxX,double maxY,boolean xray){
            finite(minX);finite(minY);finite(maxX);finite(maxY);if(minX>maxX||minY>maxY)throw new IllegalArgumentException("Inverted selection box");
            return select(minX,minY,maxX,maxY,xray,null);
        }
        /** Even-odd lasso inclusion, including its boundary. Polygon edges share the bounded work budget. */
        public Set<Long> verticesInLasso(List<Point> polygon,boolean xray){
            polygon=List.copyOf(polygon);if(polygon.size()<3||polygon.size()>2048)throw new IllegalArgumentException("Lasso requires 3 to 2048 points");
            double minX=Double.POSITIVE_INFINITY,minY=minX,maxX=Double.NEGATIVE_INFINITY,maxY=maxX;
            for(var p:polygon){finite(p.x());finite(p.y());minX=Math.min(minX,p.x());minY=Math.min(minY,p.y());maxX=Math.max(maxX,p.x());maxY=Math.max(maxY,p.y());}
            var first=polygon.get(0);Point second=null;boolean area=false;
            for(var p:polygon){
                if(second==null){if(distance(first,p.x(),p.y())>0)second=p;continue;}
                double ax=second.x()-first.x(),ay=second.y()-first.y(),bx=p.x()-first.x(),by=p.y()-first.y();
                if(Math.abs(ax*by-ay*bx)>1e-12*Math.hypot(ax,ay)*Math.hypot(bx,by)){area=true;break;}
            }
            if(!area)throw new IllegalArgumentException("Lasso must enclose a non-collinear region");
            return select(minX,minY,maxX,maxY,xray,polygon);
        }
        private static void work(int[] budget){if(++budget[0]>MAX_TESTS)throw new IllegalArgumentException("Selection geometry work budget exceeded; select a smaller region");}
        private static boolean inside(List<Point> polygon,Point point,int[] budget){
            boolean inside=false;double eps=Math.max(tolerance(point.x()),tolerance(point.y()));
            for(int i=0,j=polygon.size()-1;i<polygon.size();j=i++){
                work(budget);var a=polygon.get(j);var b=polygon.get(i);
                if(segment(a,b,point.x(),point.y())<=eps)return true;
                if((a.y()>point.y())!=(b.y()>point.y())&&point.x()<(b.x()-a.x())*(point.y()-a.y())/(b.y()-a.y())+a.x())inside=!inside;
            }return inside;
        }
        private Set<Long> select(double minX,double minY,double maxX,double maxY,boolean xray,List<Point> polygon){
            var selected=new LinkedHashSet<Long>();var budget=new int[1];
            for(var v:state.vertices()){
                var p=points.get(v.id());if(!Double.isFinite(p.x())||!Double.isFinite(p.y())||!Double.isFinite(p.depth())||p.x()<minX||p.x()>maxX||p.y()<minY||p.y()>maxY)continue;
                if(polygon!=null&&!inside(polygon,p,budget))continue;
                if(xray){selected.add(v.id());continue;}
                var hit=nodes.isEmpty()?null:query(0,p.x(),p.y(),null,budget);
                if(hit!=null&&p.depth()>=hit.depth()-Math.max(tolerance(p.depth()),tolerance(hit.depth())))selected.add(v.id());
            }
            return Collections.unmodifiableSet(selected);
        }
    }
}
