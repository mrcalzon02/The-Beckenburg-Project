package org.beckenburg.editor;

import java.util.*;

/** Triangle-level camera queries derived directly from native indexed authoring state. */
public final class EditorMeshPicking {
    public record Point(double x,double y,double depth){}
    public interface Projector {
        Point project(double x,double y,double z);
        default boolean perspective(){return false;}
        default double worldScale(double x,double y,double z){return 1;}
        default Point direction(double x,double y,double z,EditorMeshPipeline.Normal axis){var a=project(x,y,z);var b=project(x+axis.x(),y+axis.y(),z+axis.z());return new Point(b.x()-a.x(),b.y()-a.y(),0);}
        default double deltaAlong(double x,double y,double z,EditorMeshPipeline.Normal axis,double dx,double dy){var v=direction(x,y,z,axis);double length=v.x()*v.x()+v.y()*v.y();if(length<1e-24)throw new IllegalArgumentException("Axis is edge-on to camera");return (dx*v.x()+dy*v.y())/length;}
        default Point axisDirection(double x,double y,double z,int axis){var a=project(x,y,z);var b=project(x+(axis==0?1:0),y+(axis==1?1:0),z+(axis==2?1:0));return new Point(b.x()-a.x(),b.y()-a.y(),0);}
        default double axisDelta(double x,double y,double z,int axis,double dx,double dy){var v=axisDirection(x,y,z,axis);double length=v.x()*v.x()+v.y()*v.y();if(length<1e-24)throw new IllegalArgumentException("Axis is edge-on to camera");return (dx*v.x()+dy*v.y())/length;}
    }
    public enum Projection implements Projector {
        ISOMETRIC,FRONT,TOP,SIDE,PERSPECTIVE;
        public Point project(double x,double y,double z){return switch(this){
            case ISOMETRIC->new Point(.83*x-.55*z,-(.95*y+.24*x+.35*z),-.5225*x+.4225*y-.7885*z);
            case FRONT->new Point(x,-y,-z);
            case TOP->new Point(x,z,y);
            case SIDE->new Point(z,-y,x);
            case PERSPECTIVE->throw new IllegalStateException("Perspective requires the same fitted Camera for display and queries");
        };}
    }
    /** Immutable orbit camera. Coordinates are normalized by source radius; depth is reciprocal eye distance. */
    public static final class Camera implements Projector {
        private final double x,y,z,radius;private final double[] right,up,near;
        public Camera(double x,double y,double z,double radius,double yawDegrees,double pitchDegrees){
            if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z)||!Double.isFinite(radius)||radius<=0||!Double.isFinite(yawDegrees)||!Double.isFinite(pitchDegrees)||Math.abs(pitchDegrees)>89)throw new IllegalArgumentException("Invalid orbit camera");
            this.x=x;this.y=y;this.z=z;this.radius=radius;double yaw=Math.toRadians(yawDegrees),pitch=Math.toRadians(pitchDegrees),sy=Math.sin(yaw),cy=Math.cos(yaw),sp=Math.sin(pitch),cp=Math.cos(pitch);right=new double[]{cy,0,-sy};up=new double[]{-sy*sp,cp,-cy*sp};near=new double[]{sy*cp,sp,cy*cp};
        }
        public boolean sameProjection(Camera other) {
            return other != null && x == other.x && y == other.y && z == other.z && radius == other.radius
                    && Arrays.equals(right, other.right) && Arrays.equals(up, other.up) && Arrays.equals(near, other.near);
        }
        private double dot(double[] axis,double a,double b,double c){return axis[0]*a+axis[1]*b+axis[2]*c;}
        private double distance(double a,double b,double c){return 3-dot(near,(a-x)/radius,(b-y)/radius,(c-z)/radius);}
        public Point project(double a,double b,double c){double d=distance(a,b,c);if(!Double.isFinite(d)||d<=.05)return new Point(Double.NaN,Double.NaN,Double.NaN);return new Point(dot(right,(a-x)/radius,(b-y)/radius,(c-z)/radius)/d,-dot(up,(a-x)/radius,(b-y)/radius,(c-z)/radius)/d,1/d);}
        public boolean perspective(){return true;}
        public double worldScale(double a,double b,double c){return radius*distance(a,b,c);}
        public Point direction(double a,double b,double c,EditorMeshPipeline.Normal axis){var p=project(a,b,c);double d=distance(a,b,c),nearAxis=dot(near,axis.x(),axis.y(),axis.z());return new Point((dot(right,axis.x(),axis.y(),axis.z())+p.x()*nearAxis)/(radius*d),(-dot(up,axis.x(),axis.y(),axis.z())+p.y()*nearAxis)/(radius*d),0);}
        public Point axisDirection(double a,double b,double c,int axis){return direction(a,b,c,unitAxis(axis));}
        public double axisDelta(double a,double b,double c,int axis,double dx,double dy){return deltaAlong(a,b,c,unitAxis(axis),dx,dy);}
        public double deltaAlong(double a,double b,double c,EditorMeshPipeline.Normal axis,double dx,double dy){
            var direction=direction(a,b,c,axis);double length=direction.x()*direction.x()+direction.y()*direction.y();if(!Double.isFinite(length)||length<1e-24)throw new IllegalArgumentException("Axis is edge-on to camera");double along=(dx*direction.x()+dy*direction.y())/length;var p=project(a,b,c);boolean horizontal=Math.abs(direction.x())>=Math.abs(direction.y());double delta=(horizontal?direction.x():direction.y())*along,target=(horizontal?p.x():p.y())+delta,d=distance(a,b,c),e=-dot(near,axis.x(),axis.y(),axis.z())/radius,basis=(horizontal?dot(right,axis.x(),axis.y(),axis.z()):-dot(up,axis.x(),axis.y(),axis.z()))/radius;double denominator=basis-target*e;
            if(Math.abs(denominator)<1e-15/radius)throw new IllegalArgumentException("Axis drag crosses camera horizon");double result=delta*d/denominator;if(!Double.isFinite(result)||d+e*result<=.05)throw new IllegalArgumentException("Axis drag crosses camera near plane");return result;
        }
    }
    /** Stable framing derived from actual authoring vertices, never mutable scene state. */
    public static Camera camera(Collection<EditorMeshPipeline.Vertex> vertices,double yaw,double pitch){
        var bounds = new CameraBounds();
        for (var vertex : vertices) bounds.add(vertex);
        return bounds.camera(yaw, pitch);
    }
    public static Camera cameraForTriangles(List<EditorMeshPipeline.Triangle> triangles, double yaw, double pitch) {
        var bounds = new CameraBounds();
        for (var triangle : triangles) {
            bounds.add(triangle.a()); bounds.add(triangle.b()); bounds.add(triangle.c());
        }
        return bounds.camera(yaw, pitch);
    }
    private static final class CameraBounds {
        double minX = Double.POSITIVE_INFINITY, minY = minX, minZ = minX;
        double maxX = Double.NEGATIVE_INFINITY, maxY = maxX, maxZ = maxX;
        void add(EditorMeshPipeline.Vertex v) {
            minX = Math.min(minX, v.x()); minY = Math.min(minY, v.y()); minZ = Math.min(minZ, v.z());
            maxX = Math.max(maxX, v.x()); maxY = Math.max(maxY, v.y()); maxZ = Math.max(maxZ, v.z());
        }
        Camera camera(double yaw, double pitch) {
            if (minX == Double.POSITIVE_INFINITY) return new Camera(0, 0, 0, 1, yaw, pitch);
            double radius = Math.hypot(Math.hypot(maxX*.5-minX*.5, maxY*.5-minY*.5), maxZ*.5-minZ*.5);
            return new Camera(minX*.5+maxX*.5, minY*.5+maxY*.5, minZ*.5+maxZ*.5, Math.max(1e-6,radius), yaw, pitch);
        }
    }
    public static EditorMeshPipeline.Normal unitAxis(int axis){return switch(axis){case 0->new EditorMeshPipeline.Normal(1,0,0);case 1->new EditorMeshPipeline.Normal(0,1,0);case 2->new EditorMeshPipeline.Normal(0,0,1);default->throw new IllegalArgumentException("Axis must be0..2");};}
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
    /** Retain binary signatures used by existing compiled editor clients. */
    public static Optional<Hit> pick(EditorTopologyProject.State state,Projection projection,double x,double y){return pick(state,(Projector)projection,x,y);}
    public static Prepared prepare(EditorTopologyProject.State state,Projection projection){return prepare(state,(Projector)projection);}
    public static Optional<Hit> pick(EditorTopologyProject.State state,Projector projection,double x,double y){
        return prepare(state,projection).pick(x,y);
    }
    public static Prepared prepare(EditorTopologyProject.State state,Projector projection){
        if(projection==Projection.PERSPECTIVE)throw new IllegalArgumentException("Perspective queries require the display camera");return new Prepared(state,projection);
    }
    /** Immutable spatial index of projected native triangles, never an editable scene. */
    public static final class Prepared {
        private static final int MAX_TESTS=5_000_000;
        private final EditorTopologyProject.State state;
        private final boolean perspective;
        private final Projector projection;
        private final Map<Long,Point> points;
        private final Triangle[] triangles;
        private final List<Node> nodes;
        private record Triangle(int faceIndex,Point a,Point b,Point c,double minX,double minY,double maxX,double maxY){}
        private record Node(double minX,double minY,double maxX,double maxY,int from,int to,int left,int right){}
        private Prepared(EditorTopologyProject.State state,Projector projection){
            this.state=Objects.requireNonNull(state);this.projection=Objects.requireNonNull(projection);perspective=projection.perspective();
            points=new HashMap<>();
            nodes=new ArrayList<>();
            for(var v:state.vertices())points.put(v.id(),projection.project(v.x(),v.y(),v.z()));
            var all=new ArrayList<Triangle>();
            for(int i=0;i<state.faces().size();i++){
                var f=state.faces().get(i);
                var a=points.get(f.a().vertex());var b=points.get(f.b().vertex());var c=points.get(f.c().vertex());
                if (visible(a) && visible(b) && visible(c)) all.add(triangle(i,a,b,c));
            }
            triangles=all.toArray(Triangle[]::new);if(triangles.length>0)build(0,triangles.length);
        }
        private static boolean visible(Point p) { return Double.isFinite(p.x()) && Double.isFinite(p.y()); }
        private static Triangle triangle(int index,Point a,Point b,Point c) {
            return new Triangle(index,a,b,c,Math.min(a.x(),Math.min(b.x(),c.x())),Math.min(a.y(),Math.min(b.y(),c.y())),
                Math.max(a.x(),Math.max(b.x(),c.x())),Math.max(a.y(),Math.max(b.y(),c.y())));
        }
        private Prepared(Prepared previous,EditorTopologyProject.State state,Map<Long,Point> points,
                         Triangle[] triangles,List<Node> nodes) {
            this.state=state;
            projection=previous.projection;
            perspective=previous.perspective;
            this.points=points;
            this.triangles=triangles;
            this.nodes=nodes;
        }
        /** Attribute-only edits keep projected geometry. Hits always read the requested native face. */
        public Prepared update(EditorTopologyProject.State next,Projector camera) {
            if (camera!=projection) return prepare(next,camera);
            if (state==next) return this;
            if (state.vertices().size()!=next.vertices().size() || state.faces().size()!=next.faces().size())
                return prepare(next,camera);
            for (int i=0;i<state.faces().size();i++) {
                var a=state.faces().get(i);
                var b=next.faces().get(i);
                if (a!=b && (a.a().vertex()!=b.a().vertex() || a.b().vertex()!=b.b().vertex() ||
                    a.c().vertex()!=b.c().vertex())) return prepare(next,camera);
            }
            Map<Long,Point> updated=points;
            if (state.vertices()!=next.vertices()) for (int i=0;i<state.vertices().size();i++) {
                var old=state.vertices().get(i);
                var v=next.vertices().get(i);
                if (v.id()!=old.id()) return prepare(next,camera);
                if (v.equals(old)) continue;
                Point projected=camera.project(v.x(),v.y(),v.z());
                if (visible(projected)!=visible(points.get(v.id()))) return prepare(next,camera);
                if (updated==points) updated=new HashMap<>(points);
                updated.put(v.id(),projected);
            }
            if (updated==points) return new Prepared(this,next,points,triangles,nodes);
            var changed=triangles.clone();
            for (int i=0;i<changed.length;i++) {
                var t=triangles[i];
                var face=next.faces().get(t.faceIndex());
                var a=updated.get(face.a().vertex());
                var b=updated.get(face.b().vertex());
                var c=updated.get(face.c().vertex());
                if (a!=t.a() || b!=t.b() || c!=t.c()) changed[i]=triangle(t.faceIndex(),a,b,c);
            }
            var refitted=new ArrayList<>(nodes);
            for (int i=nodes.size()-1;i>=0;i--) {
                var n=nodes.get(i);
                if (n.left()<0) {
                    for (int t=n.from();t<n.to();t++) if (changed[t]!=triangles[t]) {
                        refitted.set(i,bounds(changed,n.from(),n.to()));
                        break;
                    }
                } else {
                    var left=refitted.get(n.left());
                    var right=refitted.get(n.right());
                    if (left!=nodes.get(n.left()) || right!=nodes.get(n.right()))
                        refitted.set(i,new Node(Math.min(left.minX(),right.minX()),Math.min(left.minY(),right.minY()),
                            Math.max(left.maxX(),right.maxX()),Math.max(left.maxY(),right.maxY()),n.from(),n.to(),n.left(),n.right()));
                }
            }
            return new Prepared(this,next,updated,changed,refitted);
        }
        private static Node bounds(Triangle[] triangles,int from,int to){
            double minX=Double.POSITIVE_INFINITY,minY=minX,maxX=Double.NEGATIVE_INFINITY,maxY=maxX;
            for(int i=from;i<to;i++){var t=triangles[i];minX=Math.min(minX,t.minX());minY=Math.min(minY,t.minY());maxX=Math.max(maxX,t.maxX());maxY=Math.max(maxY,t.maxY());}
            return new Node(minX,minY,maxX,maxY,from,to,-1,-1);
        }
        private int build(int from,int to){
            var range=bounds(triangles,from,to);
            int index=nodes.size();nodes.add(null);int left=-1,right=-1;
            if(to-from>8){
                boolean x=range.maxX()-range.minX()>=range.maxY()-range.minY();
                Arrays.sort(triangles,from,to,Comparator.comparingDouble(t->x?t.minX()*.5+t.maxX()*.5:t.minY()*.5+t.maxY()*.5));
                int mid=(from+to)>>>1;left=build(from,mid);right=build(mid,to);
            }
            nodes.set(index,new Node(range.minX(),range.minY(),range.maxX(),range.maxY(),from,to,left,right));return index;
        }
        private static void finite(double value){if(!Double.isFinite(value))throw new IllegalArgumentException("Nonfinite pick");}
        private static double tolerance(double value){return 64*Math.ulp(Math.abs(value))+1e-10;}
        private Hit query(int index,double x,double y,Hit hit,int[] budget){
            var node=nodes.get(index);double ex=tolerance(x),ey=tolerance(y);
            if(x<node.minX()-ex||x>node.maxX()+ex||y<node.minY()-ey||y>node.maxY()+ey)return hit;
            if(node.left()>=0)return query(node.right(),x,y,query(node.left(),x,y,hit,budget),budget);
            for(int i=node.from();i<node.to();i++){
                if(++budget[0]>MAX_TESTS)throw new IllegalArgumentException("Selection geometry work budget exceeded; select a smaller region");
                var t=triangles[i];var face=state.faces().get(t.faceIndex());var p=new Point[]{t.a(),t.b(),t.c()};
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
                if(perspective){double reciprocal=depth;a=a*p[0].depth()/reciprocal;b=b*p[1].depth()/reciprocal;c=c*p[2].depth()/reciprocal;}
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
