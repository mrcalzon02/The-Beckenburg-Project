package org.beckenburg;

import java.util.*;

/** Native cellar construction and the continuous flight through its real floor opening. */
public final class CastleCellar {
    private CastleCellar(){}
    public static final double FLOOR=-3;
    public static final int LIGHT_COUNT=CastleCellarLayout.ROOMS.size()+2;
    public static final double LEFT=CastleCellarLayout.FLIGHT_X-CastleCellarLayout.WIDTH/2;
    public static final double RIGHT=CastleCellarLayout.FLIGHT_X+CastleCellarLayout.WIDTH/2;
    public static final double HOLE_LEFT=LEFT-.2,HOLE_RIGHT=RIGHT+.2;
    public static final double HOLE_NORTH=CastleCellarLayout.LOW_Z-1.2,HOLE_SOUTH=CastleCellarLayout.HIGH_Z;
    private static final double RUN=(CastleCellarLayout.HIGH_Z-CastleCellarLayout.LOW_Z)/CastleCellarLayout.RISERS;
    private static final double RISE=3.0/CastleCellarLayout.RISERS;
    private static long activeRevision=Long.MIN_VALUE;
    private static boolean active;
    public static boolean active(){
        var scene=CastleLevelEdits.current();
        if(activeRevision!=scene.revision()){
            active=scene.surfaces().stream().anyMatch(s->s.id().equals("cellar-floor-cellar-stair")
                &&s.kind()==CastleLevelEdits.Kind.FLOOR&&s.base()==FLOOR);
            activeRevision=scene.revision();
        }
        return active;
    }
    public static boolean opening(double x,double z){
        return CastleLevelEdits.current().floorOpening(x,z,0);
    }
    public static boolean openingRim(double x,double z){
        return CastleLevelEdits.current().openings().stream().anyMatch(s->s.base()==0
            &&x>=s.x0()-1e-7&&x<=s.x1()+1e-7&&z>=s.z0()-1e-7&&z<=s.z1()+1e-7);
    }
    public static boolean onFlight(double x,double z){
        return active()&&x>=LEFT&&x<=RIGHT&&z>=CastleCellarLayout.LOW_Z&&z<CastleCellarLayout.HIGH_Z;
    }
    public static double stairHeight(double z){
        int step=Math.max(0,Math.min(CastleCellarLayout.RISERS-1,
            (int)Math.floor((z-CastleCellarLayout.LOW_Z)/RUN+1e-8)));
        return FLOOR+step*RISE;
    }
    /** Returns NaN outside the lower construction; the original upper stair remains authoritative. */
    public static double elevation(double x,double z,double old){
        if(!active()||old>0.3)return Double.NaN;
        if(onFlight(x,z))return stairHeight(z);
        if(old<0&&old>=-.26&&!opening(x,z)
            &&(CastleRooms.ground(x,z)||CastleLevelEdits.current().floorSupports(x,z,0)))return 0;
        if(old<-.1&&CastleLevelEdits.current().floorSupports(x,z,FLOOR))return FLOOR;
        return Double.NaN;
    }
    public static CastleRooms.Room roomAt(double x,double feet,double z){
        if(!active()||feet>-.1)return null;
        if(onFlight(x,z)&&Math.abs(feet-stairHeight(z))<.18)return CastleCellarLayout.ROOMS.get(0);
        if(Math.abs(feet-FLOOR)>.3||!CastleLevelEdits.current().floorSupports(x,z,FLOOR))return null;
        for(var r:CastleCellarLayout.ROOMS)if(r.contains(x,z))return r;
        return null;
    }
    public static void installDefaults(Map<String,CastleLevelEdits.Surface> surfaces,Map<String,CastleLevelEdits.Lamp> lamps){
        surfaces.put("cellar-stair-opening",new CastleLevelEdits.Surface("cellar-stair-opening",CastleLevelEdits.Kind.VOID,
            HOLE_LEFT,HOLE_NORTH,HOLE_RIGHT,HOLE_SOUTH,0,CastleRooms.SLAB,MaterialAtlas.STONE));
        for(var r:CastleCellarLayout.ROOMS){
            String id="cellar-floor-"+r.id();
            surfaces.put(id,new CastleLevelEdits.Surface(id,CastleLevelEdits.Kind.FLOOR,
                r.x0(),r.z0(),r.x1(),r.z1(),FLOOR,CastleRooms.SLAB,MaterialAtlas.STONE));
            // The stair light is mounted on the side bypass ceiling, clear of the actual opening.
            double x=r.id().equals("cellar-stair")?38:(r.x0()+r.x1())/2;
            id="cellar-light-"+r.id();
            lamps.put(id,new CastleLevelEdits.Lamp(id,"ceiling_lamp_1_on","ceiling_lamp_1_off",
                x,-CastleRooms.SLAB,(r.z0()+r.z1())/2,.8,0,CastleHubDecor.Mount.CEILING,9,3.2));
        }
        for(int step=0;step<CastleCellarLayout.RISERS;step++){
            String id="cellar-step-"+step;double top=FLOOR+step*RISE;
            surfaces.put(id,new CastleLevelEdits.Surface(id,CastleLevelEdits.Kind.FLOOR,
                LEFT,CastleCellarLayout.LOW_Z+step*RUN,RIGHT,CastleCellarLayout.LOW_Z+(step+1)*RUN,
                top,Math.max(CastleRooms.SLAB,top-FLOOR),MaterialAtlas.STONE));
        }
        for(double x:new double[]{HOLE_LEFT,HOLE_RIGHT}){
            String side=x==HOLE_LEFT?"west":"east";
            String id="cellar-stair-cheek-"+side;
            surfaces.put(id,new CastleLevelEdits.Surface(id,CastleLevelEdits.Kind.WALL,
                x,CastleCellarLayout.LOW_Z,x,HOLE_SOUTH,FLOOR,3,MaterialAtlas.STONE));
            id="cellar-guard-"+side;
            surfaces.put(id,new CastleLevelEdits.Surface(id,CastleLevelEdits.Kind.WALL,
                x,HOLE_NORTH,x,HOLE_SOUTH,0,1.05,MaterialAtlas.STONE));
        }
        surfaces.put("cellar-guard-north",new CastleLevelEdits.Surface("cellar-guard-north",CastleLevelEdits.Kind.WALL,
            HOLE_LEFT,HOLE_NORTH,HOLE_RIGHT,HOLE_NORTH,0,1.05,MaterialAtlas.STONE));
        lamps.put("cellar-light-stair-low",new CastleLevelEdits.Lamp("cellar-light-stair-low","lamp_3_on","lamp_3_off",
            HOLE_LEFT+.23,-.85,20.4,.5,-Math.PI/2,CastleHubDecor.Mount.WALL,5,3.2));
        lamps.put("cellar-light-stair-high",new CastleLevelEdits.Lamp("cellar-light-stair-high","lamp_3_on","lamp_3_off",
            HOLE_RIGHT-.23,.45,22.4,.5,Math.PI/2,CastleHubDecor.Mount.WALL,5,3.2));
    }
    /** Subtract the stairwell from the actual ground slab, retaining all four surrounding strips. */
    public static List<CastleHubArchitecturalPlan.Deck> cut(CastleHubArchitecturalPlan.Deck d){
        List<CastleHubArchitecturalPlan.Deck> parts=List.of(d);
        for(var hole:CastleLevelEdits.current().openings()){
            if(Math.abs(hole.base()-d.height())>1e-7)continue;
            var next=new ArrayList<CastleHubArchitecturalPlan.Deck>();
            for(var part:parts)next.addAll(cutOne(part,hole));
            parts=next;
        }
        return List.copyOf(parts);
    }
    private static List<CastleHubArchitecturalPlan.Deck> cutOne(CastleHubArchitecturalPlan.Deck d,CastleLevelEdits.Surface hole){
        if(d.x1()<=hole.x0()||d.x0()>=hole.x1()||d.z1()<=hole.z0()||d.z0()>=hole.z1())return List.of(d);
        var out=new ArrayList<CastleHubArchitecturalPlan.Deck>();
        double a=Math.max(d.x0(),hole.x0()),b=Math.min(d.x1(),hole.x1());
        double c=Math.max(d.z0(),hole.z0()),e=Math.min(d.z1(),hole.z1());
        add(out,d,d.x0(),d.z0(),a,d.z1());add(out,d,b,d.z0(),d.x1(),d.z1());
        add(out,d,a,d.z0(),b,c);add(out,d,a,e,b,d.z1());
        return List.copyOf(out);
    }
    private static void add(List<CastleHubArchitecturalPlan.Deck> out,CastleHubArchitecturalPlan.Deck d,
                            double x0,double z0,double x1,double z1){
        if(x1-x0>1e-8&&z1-z0>1e-8)out.add(new CastleHubArchitecturalPlan.Deck(x0,z0,x1,z1,d.height(),d.material()));
    }
}
