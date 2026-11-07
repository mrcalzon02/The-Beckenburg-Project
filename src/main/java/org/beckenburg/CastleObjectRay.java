package org.beckenburg;

/** Double-sided ray hits against the actual transformed native prop triangles. */
final class CastleObjectRay {
    private CastleObjectRay(){}
    record Hit(CastleLevelEdits.Prop prop,double distance){}
    private static double[] local(CastleLevelEdits.Prop p,double x,double y,double z,boolean point){
        double[] b=SceneRenderer.castleBounds(p.mesh());double c=Math.cos(p.yaw()),s=Math.sin(p.yaw());
        if(point){x-=p.x();y-=p.y();z-=p.z();}
        return new double[]{(x*c+z*s)/p.scale()+(point?b[0]:0),y/p.scale()+(point?b[1]:0),(-x*s+z*c)/p.scale()+(point?b[2]:0)};
    }
    private static boolean bounds(double[] a,double[] d,double[] b,double max){
        double lo=0,hi=max;
        for(int i=0;i<3;i++){
            double min=i==1?b[1]:b[i]-b[i+3]/2,maxAxis=min+b[i+3];
            if(Math.abs(d[i])<1e-10){if(a[i]<min||a[i]>maxAxis)return false;continue;}
            double t0=(min-a[i])/d[i],t1=(maxAxis-a[i])/d[i];lo=Math.max(lo,Math.min(t0,t1));hi=Math.min(hi,Math.max(t0,t1));if(lo>hi)return false;
        }return true;
    }
    static double hit(CastleLevelEdits.Prop p,double x,double y,double z,double dx,double dy,double dz,double max){
        var a=local(p,x,y,z,true);var d=local(p,dx,dy,dz,false);double best=max;
        if(!bounds(a,d,SceneRenderer.castleBounds(p.mesh()),max))return Double.POSITIVE_INFINITY;
        for(var t:GlbProps.model(p.mesh()).triangles()){
            double ux=t.b().x()-t.a().x(),uy=t.b().y()-t.a().y(),uz=t.b().z()-t.a().z();
            double vx=t.c().x()-t.a().x(),vy=t.c().y()-t.a().y(),vz=t.c().z()-t.a().z();
            double hx=d[1]*vz-d[2]*vy,hy=d[2]*vx-d[0]*vz,hz=d[0]*vy-d[1]*vx,det=ux*hx+uy*hy+uz*hz;
            if(Math.abs(det)<1e-10)continue;
            double sx=a[0]-t.a().x(),sy=a[1]-t.a().y(),sz=a[2]-t.a().z(),u=(sx*hx+sy*hy+sz*hz)/det;if(u<0||u>1)continue;
            double qx=sy*uz-sz*uy,qy=sz*ux-sx*uz,qz=sx*uy-sy*ux,v=(d[0]*qx+d[1]*qy+d[2]*qz)/det;if(v<0||u+v>1)continue;
            double distance=(vx*qx+vy*qy+vz*qz)/det;if(distance>.03&&distance<best)best=distance;
        }return best<max?best:Double.POSITIVE_INFINITY;
    }
    static Hit nearest(double x,double y,double z,double dx,double dy,double dz,double max){
        Hit best=null;for(var p:CastleLevelEdits.current().props()){
            double d=hit(p,x,y,z,dx,dy,dz,best==null?max:best.distance());if(Double.isFinite(d))best=new Hit(p,d);
        }return best;
    }
}
