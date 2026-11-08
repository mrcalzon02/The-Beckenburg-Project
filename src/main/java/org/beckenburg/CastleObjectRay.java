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
        return hit(p,x,y,z,dx,dy,dz,max,.03);
    }
    static double hit(CastleLevelEdits.Prop p,double x,double y,double z,double dx,double dy,double dz,double max,double minimum){
        return hitMesh(p.mesh(),local(p,x,y,z,true),local(p,dx,dy,dz,false),max,minimum);
    }
    private static double hitMesh(String mesh,double[] a,double[] d,double max,double minimum){
        double best=max;
        if(!bounds(a,d,SceneRenderer.castleBounds(mesh),max))return Double.POSITIVE_INFINITY;
        for(var t:GlbProps.model(mesh).triangles()){
            double ux=t.b().x()-t.a().x(),uy=t.b().y()-t.a().y(),uz=t.b().z()-t.a().z();
            double vx=t.c().x()-t.a().x(),vy=t.c().y()-t.a().y(),vz=t.c().z()-t.a().z();
            double hx=d[1]*vz-d[2]*vy,hy=d[2]*vx-d[0]*vz,hz=d[0]*vy-d[1]*vx,det=ux*hx+uy*hy+uz*hz;
            if(Math.abs(det)<1e-10)continue;
            double sx=a[0]-t.a().x(),sy=a[1]-t.a().y(),sz=a[2]-t.a().z(),u=(sx*hx+sy*hy+sz*hz)/det;if(u<0||u>1)continue;
            double qx=sy*uz-sz*uy,qy=sz*ux-sx*uz,qz=sx*uy-sy*ux,v=(d[0]*qx+d[1]*qy+d[2]*qz)/det;if(v<0||u+v>1)continue;
            double distance=(vx*qx+vy*qy+vz*qz)/det;if(distance>minimum&&distance<best)best=distance;
        }return best<max?best:Double.POSITIVE_INFINITY;
    }
    private static double[] looseLocal(LooseClutter.State s,double x,double y,double z,boolean point){
        if(point){x-=s.x();y-=s.y();z-=s.z();}
        double c=Math.cos(s.yaw()),sn=Math.sin(s.yaw()),r=Math.cos(s.roll()),rs=Math.sin(s.roll()),rx=x*c+z*sn;
        return new double[]{rx*r+y*rs,-rx*rs+y*r+(point?s.kind().half:0),-x*sn+z*c};
    }
    static double looseHit(LooseClutter.State s,double x,double y,double z,double dx,double dy,double dz,double max){
        return hitMesh(s.kind().mesh(),looseLocal(s,x,y,z,true),looseLocal(s,dx,dy,dz,false),max,1e-7);
    }
    /** Highest upward source triangle at this exact native XZ, within the swept interval. */
    static double support(CastleLevelEdits.Prop p,double x,double z,double before,double after){
        var q=local(p,x,0,z,true);var b=SceneRenderer.castleBounds(p.mesh());double best=-100;
        if(Math.abs(q[0]-b[0])>b[3]/2||Math.abs(q[2]-b[2])>b[5]/2)return best;
        for(var t:GlbProps.model(p.mesh()).triangles()){
            double ax=t.b().x()-t.a().x(),az=t.b().z()-t.a().z(),bx=t.c().x()-t.a().x(),bz=t.c().z()-t.a().z(),den=ax*bz-az*bx;
            if(den>=-1e-10)continue;
            double dx=q[0]-t.a().x(),dz=q[2]-t.a().z(),u=(dx*bz-dz*bx)/den,v=(ax*dz-az*dx)/den;
            if(u< -1e-7||v< -1e-7||u+v>1+1e-7)continue;
            double y=p.y()+(t.a().y()+u*(t.b().y()-t.a().y())+v*(t.c().y()-t.a().y())-b[1])*p.scale();
            if(y<=before+.01&&y>=after-.02)best=Math.max(best,y);
        }return best;
    }
    static Hit nearest(double x,double y,double z,double dx,double dy,double dz,double max){
        Hit best=null;for(var p:CastleLevelEdits.current().props()){
            double d=hit(p,x,y,z,dx,dy,dz,best==null?max:best.distance());if(Double.isFinite(d))best=new Hit(p,d);
        }return best;
    }
}
