package org.beckenburg;
import org.beckenburg.LevelDefinition.*;
/** Continuous architectural shadow rays, including the receiving side of joins.
 * Interaction/movement sampling stays unchanged. Open portals and stair voids
 * remain genuine apertures; endpoint bias may never skip an architectural face.
 */
public final class ArchitecturalLightOcclusion {
    private ArchitecturalLightOcclusion(){}
    private static final double EPS=.003,HALF=LevelDefinition.WALL_HALF_THICKNESS;
    public static boolean visible(HouseMap map,int receiver,double ax,double ay,double az,double x,double y,double z){
        if(!Double.isFinite(ax+ay+az+x+y+z))return false;
        if(receiver>=0&&receiver<12){
            var r=map.level().room(receiver);
            // A shared edge belongs to each face's authored room. Nudge it onto
            // that room's side, instead of pulling it toward the light source.
            x=inside(x,r.x0()+HALF,r.x1()-HALF);
            z=inside(z,r.z0()+HALF,r.z1()-HALF);
            if(Math.abs(y-r.y())<.001)y+=EPS;
            if(Math.abs(y-(r.y()+r.height()-.04))<.001)y-=EPS;
        }
        if(!architecturalClear(map,ax,ay,az,x,y,z))return false;
        // Existing collider sight handles props without self-shadowing their
        // receiving surface. Architecture was checked to the FULL endpoint.
        double dx=x-ax,dy=y-ay,dz=z-az,d=Math.sqrt(dx*dx+dy*dy+dz*dz);
        double back=Math.min(.12,d*.20)/Math.max(.001,d);
        return map.clearLineOfSight(ax,ay,az,x-dx*back,y-dy*back,z-dz*back);
    }
    private static double inside(double value,double lo,double hi){
        if(value>=lo-HALF-.001&&value<=lo+.001)return lo+EPS;
        if(value<=hi+HALF+.001&&value>=hi-.001)return hi-EPS;
        return value;
    }
    public static boolean architecturalClear(HouseMap map,double ax,double ay,double az,double bx,double by,double bz){
        for(var r:map.level().rooms())if(!r.outside()){
            for(double deck:new double[]{r.y(),r.y()+r.height()-.04}){
                double t=at(ay,by,deck);
                if(hit(t)){
                    double x=ax+(bx-ax)*t,z=az+(bz-az)*t;
                    double voidY=Math.abs(deck-r.y())<.001?r.y():r.y()+r.height();
                    if(x>=r.x0()-1e-7&&x<=r.x1()+1e-7&&z>=r.z0()-1e-7&&z<=r.z1()+1e-7&&!map.level().floorVoid(x,voidY,z))return false;
                }
            }
            for(boolean alongX:new boolean[]{true,false}){
                double low=alongX?r.z0():r.x0(),high=alongX?r.z1():r.x1();
                for(double centre:new double[]{low,high})for(double side:new double[]{-HALF,HALF}){
                    double t=at(alongX?az:ax,alongX?bz:bx,centre+side);
                    if(!hit(t))continue;
                    double y=ay+(by-ay)*t,tangent=alongX?ax+(bx-ax)*t:az+(bz-az)*t;
                    double start=alongX?r.x0():r.z0(),end=alongX?r.x1():r.z1();
                    if(y<r.y()-1e-7||y>r.y()+r.height()+1e-7||tangent<start-HALF||tangent>end+HALF)continue;
                    if(!aperture(map,r.id(),alongX,centre,tangent,y))return false;
                }
            }
        }
        return true;
    }
    private static boolean aperture(HouseMap map,int room,boolean alongX,double plane,double tangent,double y){
        for(var p:map.level().portals())if((p.a()==room||p.b()==room)&&p.alongX()==alongX&&!p.kind().equals("stair")&&Math.abs((alongX?p.z():p.x())-plane)<.01){
            if(Math.abs(tangent-(alongX?p.x():p.z()))>=p.width()/2-.001)continue;
            if(p.kind().equals("window")){if(y>p.y()+1.0&&y<p.y()+2.35)return true;}
            else if((!p.door()||map.doorOpen(p.id()))&&y>=p.y()-.10&&y<=p.y()+2.45)return true;
        }
        return false;
    }
    private static double at(double a,double b,double plane){return Math.abs(b-a)<1e-10?-1:(plane-a)/(b-a);}
    private static boolean hit(double t){return t>1e-7&&t<1-1e-7;}
}
