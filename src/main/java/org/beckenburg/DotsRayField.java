package org.beckenburg;
import java.util.*;
import org.beckenburg.LevelDefinition.Vec;
/** The projector's actual rays: static Tier I, panning higher tiers.
 * Both host ghost contact and renderer surface projection use this same pattern.
 */
public final class DotsRayField {
    private DotsRayField(){}
    public record Ray(Vec origin,Vec direction,double reach){}
    public record Hit(double distance,Vec position,Vec normal){}
    public static List<Ray> rays(LanDotsProjectors.Placement p,int tick){
        int tier=p.tier().ordinal(),columns=new int[]{17,25,33}[tier],rows=new int[]{11,15,19}[tier];
        double heading=Math.toRadians(p.heading()),pan=tier==0?0:Math.sin(tick*.10*(tier==1?.65:1.05))*(tier==1?.42:.70);
        var origin=new Vec(p.x()+Math.sin(heading)*.18,p.y()+.12,p.z()+Math.cos(heading)*.18);
        double reach=EquipmentTiers.stage("DOTS_PROJECTOR",p.tier()).reachCm()/100.0;var out=new ArrayList<Ray>();
        for(int row=0;row<rows;row++)for(int col=0;col<columns;col++){
            double yaw=heading+pan+(col/(double)(columns-1)-.5)*1.7;
            double pitch=-.28+row/(double)(rows-1)*1.35;
            out.add(new Ray(origin,new Vec(Math.sin(yaw)*Math.cos(pitch),Math.sin(pitch),Math.cos(yaw)*Math.cos(pitch)),reach));
        }
        return List.copyOf(out);
    }
    public record Projection(Hit hit,boolean onGhost){}
    public static Projection project(Ray ray,Mesh receivers,LanGhostSpatial.Pose ghost){
        var surface=receivers.first(ray);var body=ghost==null?null:capsule(ray,ghost);
        if(body!=null&&(surface==null||body.distance()<surface.distance()))return new Projection(body,true);
        return surface==null?null:new Projection(surface,false);
    }
    public static Vec at(Ray r,double t){return new Vec(r.origin().x()+r.direction().x()*t,r.origin().y()+r.direction().y()*t,r.origin().z()+r.direction().z()*t);}
    public static Hit capsule(Ray ray,LanGhostSpatial.Pose body){
        double radius=StructuredLightVolume.RADIUS,lower=body.y()+radius,upper=body.y()+StructuredLightVolume.HEIGHT-radius;
        double ox=ray.origin().x()-body.x(),oz=ray.origin().z()-body.z(),dx=ray.direction().x(),dy=ray.direction().y(),dz=ray.direction().z(),best=ray.reach()+1;
        double a=dx*dx+dz*dz,b=2*(ox*dx+oz*dz),c=ox*ox+oz*oz-radius*radius,disc=b*b-4*a*c;
        if(a>1e-12&&disc>=0)for(double t:new double[]{(-b-Math.sqrt(disc))/(2*a),(-b+Math.sqrt(disc))/(2*a)}){
            double y=ray.origin().y()+dy*t;if(t>.015&&t<best&&y>=lower&&y<=upper)best=t;
        }
        for(double center:new double[]{lower,upper}){
            double oy=ray.origin().y()-center,linear=2*(ox*dx+oy*dy+oz*dz),discriminant=linear*linear-4*(ox*ox+oy*oy+oz*oz-radius*radius);
            if(discriminant>=0)for(double t:new double[]{(-linear-Math.sqrt(discriminant))/2,(-linear+Math.sqrt(discriminant))/2}){
                double y=ray.origin().y()+dy*t;
                if(t>.015&&t<best&&(center==lower?y<=lower:y>=upper))best=t;
            }
        }
        if(best>ray.reach())return null;var point=at(ray,best);
        double centerY=Math.max(lower,Math.min(upper,point.y()));
        return new Hit(best,point,new Vec((point.x()-body.x())/radius,(point.y()-centerY)/radius,(point.z()-body.z())/radius));
    }
    public static List<Hit> ghostHits(HouseMap map,LanDotsProjectors.Placement p,LanGhostSpatial.Pose body,int tick){
        if(body==null||p.room()!=body.room()||Math.abs(p.y()-body.y())>.75)return List.of();
        var out=new ArrayList<Hit>();
        for(var ray:rays(p,tick)){
            var hit=capsule(ray,body);if(hit==null)continue;var target=at(ray,Math.max(.016,hit.distance()-.035));
            if(map==null||map.clearLineOfSight(ray.origin().x(),ray.origin().y(),ray.origin().z(),target.x(),target.y(),target.z()))out.add(hit);
        }
        return List.copyOf(out);
    }
    public record Triangle(Vec a,Vec b,Vec c,int material,double au,double av,double bu,double bv,double cu,double cv){}
    /** Small BVH of actual rendered triangles. Reused until its receiver geometry changes. */
    public static final class Mesh {
        private final Node root;
        private static final class Node {
            double[] lo={Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY},hi={Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY};
            Node left,right;List<Triangle> triangles;
        }
        public Mesh(List<Triangle> faces){root=faces.isEmpty()?null:build(new ArrayList<>(faces));}
        private static double axis(Vec p,int axis){return axis==0?p.x():axis==1?p.y():p.z();}
        private static Node build(ArrayList<Triangle> faces){
            var node=new Node();for(var f:faces)for(var v:new Vec[]{f.a(),f.b(),f.c()})for(int i=0;i<3;i++){node.lo[i]=Math.min(node.lo[i],axis(v,i));node.hi[i]=Math.max(node.hi[i],axis(v,i));}
            if(faces.size()<=12){node.triangles=List.copyOf(faces);return node;}
            int axis=0;for(int i=1;i<3;i++)if(node.hi[i]-node.lo[i]>node.hi[axis]-node.lo[axis])axis=i;
            final int split=axis;faces.sort(Comparator.comparingDouble(f->axis(f.a(),split)+axis(f.b(),split)+axis(f.c(),split)));
            int middle=faces.size()/2;node.left=build(new ArrayList<>(faces.subList(0,middle)));node.right=build(new ArrayList<>(faces.subList(middle,faces.size())));return node;
        }
        private static boolean bounds(Node n,Ray ray,double best){
            double lo=.015,hi=Math.min(best,ray.reach());
            for(int i=0;i<3;i++){
                double start=axis(ray.origin(),i),delta=axis(ray.direction(),i);
                if(Math.abs(delta)<1e-12){if(start<n.lo[i]-1e-7||start>n.hi[i]+1e-7)return false;}
                else{double a=(n.lo[i]-start)/delta,b=(n.hi[i]-start)/delta;lo=Math.max(lo,Math.min(a,b));hi=Math.min(hi,Math.max(a,b));if(lo>hi+1e-7)return false;}
            }
            return true;
        }
        public Hit first(Ray ray){return root==null?null:trace(root,ray,null);}
        private static Hit trace(Node n,Ray r,Hit best){
            if(!bounds(n,r,best==null?r.reach():best.distance()))return best;
            if(n.triangles==null){best=trace(n.left,r,best);return trace(n.right,r,best);}
            for(var f:n.triangles){var hit=triangle(r,f,best==null?r.reach():best.distance());if(hit!=null)best=hit;}return best;
        }
        private static Hit triangle(Ray r,Triangle t,double limit){
            double ex=t.b().x()-t.a().x(),ey=t.b().y()-t.a().y(),ez=t.b().z()-t.a().z(),fx=t.c().x()-t.a().x(),fy=t.c().y()-t.a().y(),fz=t.c().z()-t.a().z();
            double px=r.direction().y()*fz-r.direction().z()*fy,py=r.direction().z()*fx-r.direction().x()*fz,pz=r.direction().x()*fy-r.direction().y()*fx,det=ex*px+ey*py+ez*pz;
            if(Math.abs(det)<1e-10)return null;double inv=1/det,tx=r.origin().x()-t.a().x(),ty=r.origin().y()-t.a().y(),tz=r.origin().z()-t.a().z(),u=(tx*px+ty*py+tz*pz)*inv;
            if(u<0||u>1)return null;double qx=ty*ez-tz*ey,qy=tz*ex-tx*ez,qz=tx*ey-ty*ex,v=(r.direction().x()*qx+r.direction().y()*qy+r.direction().z()*qz)*inv;
            if(v<0||u+v>1)return null;double distance=(fx*qx+fy*qy+fz*qz)*inv;if(distance<=.015||distance>limit)return null;
            if(t.material()>=GlbProps.TEXTURE_BASE&&GlbProps.sampleAlpha(t.material(),t.au()*(1-u-v)+t.bu()*u+t.cu()*v,t.av()*(1-u-v)+t.bv()*u+t.cv()*v)<.5)return null;
            double nx=ey*fz-ez*fy,ny=ez*fx-ex*fz,nz=ex*fy-ey*fx,length=Math.sqrt(nx*nx+ny*ny+nz*nz);if(length<1e-12)return null;
            double sign=nx*r.direction().x()+ny*r.direction().y()+nz*r.direction().z()>0?-1:1;
            return new Hit(distance,at(r,distance),new Vec(nx/length*sign,ny/length*sign,nz/length*sign));
        }
    }
}
