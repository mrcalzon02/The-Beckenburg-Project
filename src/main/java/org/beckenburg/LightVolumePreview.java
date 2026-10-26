package org.beckenburg;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Editor-only influence-gizmo geometry. The authored radius is a conservative
 * candidate light sphere; 3D castle wall/door/floor rays trim its real visible
 * envelope. Never changes level data or substitutes for runtime shadow checks.
 */
public final class LightVolumePreview {
    private LightVolumePreview(){}
    public record Ray(double x,double y,double z,boolean blocked){}
    public record Volume(double x,double y,double z,double radius,List<Ray> rays){
        public Volume{rays=List.copyOf(rays);}
    }
    public static Volume trace(CastleHubMap map,CastleLevelEdits.Lamp lamp){
        Objects.requireNonNull(map);Objects.requireNonNull(lamp);
        var bounds=VanInteriorLayout.bounds(lamp.on());
        double sx=lamp.x(),sy=lamp.y()-
            (lamp.mount()==CastleHubDecor.Mount.CEILING
                ?(bounds.y1()-bounds.y0())*lamp.scale()*.55:0),sz=lamp.z();
        var rays=new ArrayList<Ray>();
        // Horizontal and upward/downward slices make the volume visible in
        // both top-down and first-person editor cameras.
        for(double elevation:new double[]{-.38,0,.38})
            for(int i=0;i<16;i++){
                double angle=i*(Math.PI*2/16);
                double x=Math.cos(angle)*Math.cos(elevation);
                double y=Math.sin(elevation);
                double z=Math.sin(angle)*Math.cos(elevation);
                double limit=lamp.radius();
                boolean clipped=!FixtureLighting.castleRayClear(map,sx,sy,sz,
                    sx+x*limit,sy+y*limit,sz+z*limit);
                if(clipped){
                    double low=0,high=limit;
                    for(int j=0;j<9;j++){
                        double mid=(low+high)*.5;
                        if(FixtureLighting.castleRayClear(map,sx,sy,sz,
                            sx+x*mid,sy+y*mid,sz+z*mid))low=mid;
                        else high=mid;
                    }
                    limit=low;
                }
                rays.add(new Ray(sx+x*limit,sy+y*limit,sz+z*limit,clipped));
            }
        return new Volume(sx,sy,sz,lamp.radius(),rays);
    }
    private static Point project(SceneRenderer view,double x,double y,double z,
                                 int width,int height){
        double dx=x-view.x,dy=y-view.eyeHeight,dz=z-view.z;
        double cy=Math.cos(view.yaw),sy=Math.sin(view.yaw);
        double cp=Math.cos(view.pitch),sp=Math.sin(view.pitch);
        double rx=-dx*cy+dz*sy,fwd=dx*sy+dz*cy;
        double vy=dy*cp-fwd*sp,depth=dy*sp+fwd*cp;
        if(depth<=.065||!Double.isFinite(depth))return null;
        double focal=(SceneRenderer.W*.5)/
            Math.tan(Math.toRadians(view.fieldOfView()/2))*view.height()/SceneRenderer.H;
        var region=GraphicsOptions.current().viewport(width,height);
        double u=(view.width()*.5+rx/depth*focal)/view.width();
        double v=(view.height()*.5-vy/depth*focal)/view.height();
        return new Point((int)Math.round(region.x()+region.width()*u),
                         (int)Math.round(region.y()+region.height()*v));
    }
    public static void draw(Graphics2D g,SceneRenderer camera,
                            Volume volume,int width,int height){
        if(volume==null)return;
        var origin=project(camera,volume.x,volume.y,volume.z,width,height);
        if(origin==null)return;
        Graphics2D out=(Graphics2D)g.create();
        try{
            out.setStroke(new BasicStroke(1.15f));
            int total=volume.rays().size(),clipped=0;
            for(int i=0;i<total;i++){
                Ray ray=volume.rays().get(i);
                if(ray.blocked())clipped++;
                var point=project(camera,ray.x,ray.y,ray.z,width,height);
                if(point==null)continue;
                out.setColor(ray.blocked()
                    ?new Color(255,144,98,205):new Color(137,229,191,180));
                out.drawLine(origin.x,origin.y,point.x,point.y);
                // Same-altitude slices form a wireframe ring. Break segments
                // at the near clip plane rather than crossing the screen.
                int following=(i/16)*16+(i+1)%16;
                Ray next=volume.rays().get(following);
                var nextPoint=project(camera,next.x,next.y,next.z,width,height);
                if(nextPoint!=null)out.drawLine(point.x,point.y,nextPoint.x,nextPoint.y);
            }
            out.setColor(new Color(255,233,163));
            out.fillOval(origin.x-4,origin.y-4,8,8);
            out.setColor(new Color(8,19,23,210));
            out.fillRoundRect(10,10,Math.min(width-20,395),27,8,8);
            out.setColor(new Color(208,244,230));
            out.drawString(String.format(java.util.Locale.ROOT,
                "LIGHT VOLUME  %.1fm  /  %d of %d rays blocked",
                volume.radius(),clipped,total),20,28);
        }finally{out.dispose();}
    }
}
