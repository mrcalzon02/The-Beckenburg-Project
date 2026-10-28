package org.beckenburg;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/** Authored PSX interior rooms: real purchased meshes, not placeholder furniture. */
public final class CastleHubDecor {
    private CastleHubDecor(){}
    private static CastleHubMap.Furnishing f(String mesh,double x,double z,double scale,
            double yaw,double hx,double hz){
        return new CastleHubMap.Furnishing(mesh,x,z,scale,yaw,hx,hz);
    }
    public static List<CastleHubMap.Furnishing> furnishings(){return FURNISHINGS;}
    private static final List<CastleHubMap.Furnishing> FURNISHINGS=List.of(
        // Armoury: storage, display and evidence cabinets
        f("shelf_mp_6",1.35,3.5,1.30,-Math.PI/2,.27,.60),
        f("shelf_mp_6",1.35,6.1,1.30,-Math.PI/2,.27,.60),
        f("shelf_mp_6",1.35,8.4,1.30,-Math.PI/2,.27,.60),
        f("display_cabinet_mp_2",7.7,1.48,.95,0,.65,.30),
        f("display_cabinet_mp_2",7.7,10.65,.95,Math.PI,.65,.30),
        // Contract and reading room
        f("dinner_table_1",13.5,3.4,1.0,0,.86,.56),
        f("chair_mp_5",15.9,3.7,1.12,Math.PI/2,.35,.39),
        f("chair_mp_5",11.5,5.0,1.12,0,.35,.39),
        f("armchair_1",10.95,9.2,.82,0,.62,.44),
        f("sofa_1",16.0,9.0,.75,Math.PI/2,.48,1.0),
        // Dressing suite: wardrobes and TV lounge
        f("wardrobe_mp_1",25.4,4.0,1.0,Math.PI/2,.34,.70),
        f("wardrobe_mp_1",25.4,8.4,1.0,Math.PI/2,.34,.70),
        f("sofa_1",21.4,9.0,.75,0,1.0,.48),
        f("armchair_1",20.6,3.6,.82,0,.62,.44),
        f("tv_table_1_4",24.35,2.1,1.0,Math.PI/2,.34,.83),
        // Courtyard gathering spot
        f("sofa_1",4.0,20.0,.74,0,.98,.48),
        f("armchair_1",7.25,19.6,.78,Math.PI/2,.44,.60),
        f("dinner_table_1",5.65,18.2,.65,0,.56,.38),
        // Operations and multiplayer offices
        f("display_cabinet_mp_2",11.3,13.3,.75,0,.51,.25),
        f("display_cabinet_mp_2",16.0,13.3,.75,0,.51,.25),
        f("chair_mp_5",24.1,17.7,1.12,0,.35,.39),
        f("tv_table_1_4",25.15,20.0,.82,Math.PI/2,.29,.69),
        f("armchair_1",22.3,20.0,.77,0,.57,.42),
        f("wardrobe_mp_1",25.8,13.7,.75,Math.PI/2,.27,.52),
        // East archive and service wings - furniture, never free field equipment.
        f("shelf_mp_6",29.20,5.4,1.30,-Math.PI/2,.27,.60),
        f("shelf_mp_6",42.35,6.0,1.30,Math.PI/2,.27,.60),
        f("display_cabinet_mp_2",32.1,13.1,.92,0,.57,.31),
        f("dinner_table_1",36.0,11.8,.87,0,.81,.53),
        f("chair_mp_5",33.0,13.5,.94,Math.PI,.35,.38),
        f("chair_mp_5",38.0,12.5,.94,0,.35,.38),
        f("shelf_mp_6",29.20,21.3,1.30,Math.PI/2,.27,.60),
        f("shelf_mp_6",42.35,21.3,1.30,Math.PI/2,.27,.60),
        f("dinner_table_1",35.5,25.2,.9,0,.85,.56),
        f("armchair_1",33.0,27.7,.78,Math.PI,.61,.45),
        f("armchair_1",39.0,27.7,.78,0,.61,.45),
        f("display_cabinet_mp_2",40.5,30.5,.92,Math.PI/2,.32,.72),
        f("sofa_1",5.5,27.8,.77,0,1.05,.48),
        f("dinner_table_1",5.1,24.9,.70,0,.58,.40)
    );
    /** Measured second-storey library furnishings. They sit on the real
     * 3.4m upper deck and share a height-aware collision registry.
     */
    public static final List<CastleHubMap.Furnishing> UPPER_FURNISHINGS=List.of(
        f("shelf_mp_6",11.5,3.8,1.25,-Math.PI/2,.28,.57),
        f("shelf_mp_6",18.3,3.8,1.25,Math.PI/2,.28,.57),
        f("shelf_mp_6",11.5,8.1,1.25,-Math.PI/2,.28,.57),
        f("dinner_table_1",14.6,5.6,.85,0,.73,.47),
        f("chair_mp_5",13.1,7.2,.90,0,.33,.35),
        f("chair_mp_5",16.1,7.2,.90,0,.33,.35),
        f("sofa_1",14.8,2.25,.73,0,.90,.46)
    );
    /** Small furniture-top detail is decorative; no invisible solid boxes. */
    public record Accent(String mesh,double x,double y,double z,double scale,double yaw){}
    public static final List<Accent> ACCENTS=List.of(
        new Accent("open_book_mp_1",13.5,.88,3.4,.48,0),
        new Accent("tv_mp_1",24.35,.75,2.1,.85,Math.PI/2),
        new Accent("tv_mp_1",25.15,.62,20.0,.67,Math.PI/2),
        new Accent("book_mp_1",11.3,1.40,13.3,.42,0),
        new Accent("handheld_tablet_1",16.0,1.40,13.3,.43,0),
        new Accent("book_mp_1",5.65,.56,18.2,.36,0),
        new Accent("open_book_mp_1",35.5,.86,25.2,.40,.2));
    /** Licensed fixture families are explicitly assigned to real building mounts.
     * A fixture cannot be authored in the sky, over the courtyard, or behind
     * an imaginary wall. Placement, rendering and verification use this list.
     */
    /** Model-derived height, shared by renderer and support-attached accents.
     * Cached once per GLB; floor/desk placements never rely on guessed pivots. */
    private static final ConcurrentHashMap<String,Double> MESH_HEIGHT=new ConcurrentHashMap<>();
    public static double meshHeight(String mesh){
        return MESH_HEIGHT.computeIfAbsent(mesh,name->{
            double lo=Double.POSITIVE_INFINITY,hi=Double.NEGATIVE_INFINITY;
            for(var triangle:GlbProps.model(name).triangles())
                for(var v:new GlbProps.Vertex[]{triangle.a(),triangle.b(),triangle.c()}){
                    lo=Math.min(lo,v.y());hi=Math.max(hi,v.y());
                }
            if(!Double.isFinite(lo+hi)||hi<=lo)throw new IllegalStateException("Invalid model bounds "+name);
            return hi-lo;
        });
    }
    /** A book, tablet or screen belongs to its real table or cabinet. If the
     * editor moves/deletes the parent, the accent follows or disappears. */
    public static List<Accent> placedAccents(CastleLevelEdits edits){
        List<Accent> result=new ArrayList<>();
        var props=edits.props();
        for(var accent:ACCENTS){
            int source=-1;
            for(int i=0;i<FURNISHINGS.size();i++){
                var item=FURNISHINGS.get(i);
                if(Math.hypot(item.x()-accent.x(),item.z()-accent.z())<.03){source=i;break;}
            }
            if(source<0){result.add(accent);continue;}
            String id="ground"+source;
            var support=props.stream().filter(p->p.id().equals(id)).findFirst().orElse(null);
            if(support!=null)result.add(new Accent(accent.mesh(),support.x(),
                support.y()+meshHeight(support.mesh())*support.scale()+.008,
                support.z(),accent.scale(),accent.yaw()));
        }
        return List.copyOf(result);
    }
    public enum Mount { CEILING, WALL }
    public record Light(String on,String off,double x,double y,double z,
                        double scale,double yaw,Mount mount){
        public boolean attached(CastleHubMap map){
            if(mount==Mount.CEILING)
                return !map.solid((int)Math.floor(x),(int)Math.floor(z))
                    &&!map.outside(x,z)&&Math.abs(y-CastleRooms.mountHeight(x,z,y>4))<.025;
            return CastleHubArchitecturalPlan.mountingWall(x,y,z,yaw);
        }
    }
    private static Light ceiling(String family,double x,double z,double scale){
        return new Light(family+"_on",family+"_off",x,3.38,z,scale,0,Mount.CEILING);
    }
    private static Light upperCeiling(String family,double x,double z,double scale){
        return new Light(family+"_on",family+"_off",x,6.78,z,scale,0,Mount.CEILING);
    }
    private static Light sconce(double x,double z,double yaw){
        return new Light("lamp_3_on","lamp_3_off",x,2.24,z,.72,yaw,Mount.WALL);
    }
    private static final List<Light> LEGACY_LIGHTS=List.of(
        // Different historical lamp families, fitted flush to stone-vault ceilings.
        ceiling("ceiling_lamp_1",5.0,5.5,.90),
        ceiling("ceiling_lamp_4",13.5,5.2,.67),
        ceiling("ceiling_lamp_1",23.0,5.2,.90),
        ceiling("ceiling_lamp_4",5.0,17.5,.67),
        ceiling("ceiling_lamp_1",24.0,17.5,.85),
        ceiling("ceiling_lamp_4",35.0,10.0,.64),
        ceiling("ceiling_lamp_1",35.0,25.0,.85),
        // Actual bracketed sconces on existing collision-map masonry.
        sconce(10.30,5.0,Math.PI/2),
        sconce(10.70,8.8,-Math.PI/2),
        sconce(19.30,8.0,Math.PI/2),
        sconce(19.70,4.2,-Math.PI/2),
        sconce(15.5,11.30,0),
        sconce(3.0,11.70,Math.PI),
        sconce(10.30,18.3,Math.PI/2),
        sconce(20.70,18.0,-Math.PI/2),
        sconce(28.70,23.0,-Math.PI/2),
        sconce(39.0,17.70,Math.PI),
        // Supplemental room-local emitters: every position is supported by an
        // authored roof/floor, never the uncovered courtyard. Append-only so
        // legacy fixture0..fixture16 identifiers remain stable in scene saves.
        ceiling("ceiling_lamp_1",5.0,9.0,.83),
        ceiling("ceiling_lamp_4",5.2,23.0,.62),
        ceiling("ceiling_lamp_1",5.2,29.0,.82),
        ceiling("ceiling_lamp_1",13.4,8.5,.80),
        ceiling("ceiling_lamp_4",15.4,3.5,.61),
        ceiling("ceiling_lamp_1",23.7,8.7,.80),
        ceiling("ceiling_lamp_1",24.0,26.5,.85),
        ceiling("ceiling_lamp_4",24.0,30.1,.62),
        ceiling("ceiling_lamp_1",34.6,6.7,.82),
        ceiling("ceiling_lamp_4",39.0,12.0,.60),
        ceiling("ceiling_lamp_1",35.0,21.7,.85),
        ceiling("ceiling_lamp_1",39.0,29.0,.82),
        upperCeiling("ceiling_lamp_1",13.5,5.0,.82),
        upperCeiling("ceiling_lamp_4",16.7,8.7,.64));
    /** Refit existing mounts, then fill every room and the exterior circulation. */
    public static final List<Light> LIGHTS=buildLights();
    private static List<Light> buildLights(){
        var out=new ArrayList<Light>();
        for(var l:LEGACY_LIGHTS)out.add(l.mount()==Mount.CEILING?
            new Light(l.on(),l.off(),l.x(),CastleRooms.mountHeight(l.x(),l.z(),l.y()>4),l.z(),l.scale(),l.yaw(),l.mount()):l);
        for(var r:CastleRooms.ALL){
            if(r.outside())continue;
            for(double x=r.x0()+2;x<r.x1()-1;x+=4.0)
                for(double z=r.z0()+2;z<r.z1()-1;z+=4.0){
                    double y=r.ceiling()-CastleRooms.SLAB,px=x,pz=z;
                    if(out.stream().anyMatch(l->Math.abs(l.y()-y)<.1&&Math.hypot(l.x()-px,l.z()-pz)<2.8))continue;
                    out.add(new Light("ceiling_lamp_1_on","ceiling_lamp_1_off",x,y,z,.85,0,Mount.CEILING));
                }
        }
        for(double z:new double[]{14,21,28}){
            out.add(sconce(10.73,z,-Math.PI/2));out.add(sconce(20.27,z,Math.PI/2));
        }
        out.add(sconce(12,33.77,0));out.add(sconce(19,33.77,0));
        return List.copyOf(out);
    }
    public record Switch(double x,double y,double z,double yaw){}
    public static final List<Switch> SWITCHES=List.of(
        new Switch(10.27,1.20,8.75,Math.PI/2),
        new Switch(10.73,1.20,3.0,-Math.PI/2),
        new Switch(19.73,1.20,2.5,-Math.PI/2),
        new Switch(10.27,1.20,14,Math.PI/2),
        new Switch(10.73,1.20,19,-Math.PI/2),
        new Switch(20.73,1.20,14,-Math.PI/2),
        new Switch(28.73,1.20,9,-Math.PI/2),
        new Switch(28.73,1.20,26.0,-Math.PI/2),
        new Switch(10.73,4.60,6.0,-Math.PI/2));
    /** Players must stand on traversable ground and actually look at a switch. */
    public static Switch aimed(CastleHubMap map,double x,double z,double yaw){
        return aimed(map,x,0,z,yaw);
    }
    public static Switch aimed(CastleHubMap map,double x,double elevation,double z,double yaw){
        if(!map.traversable(x,z,elevation)||!Double.isFinite(yaw))return null;
        Switch best=null;double alignment=.77;
        for(Switch s:SWITCHES){
            if(Math.abs(s.y()-(elevation+1.2))>.7)continue;
            double dx=s.x()-x,dz=s.z()-z,d=Math.hypot(dx,dz);
            if(d<.3||d>2.1)continue;
            double forward=(dx*Math.sin(yaw)+dz*Math.cos(yaw))/d;
            if(forward>alignment&&FixtureLighting.castleRayClear(map,x,elevation+1.62,z,s.x(),s.y(),s.z())){alignment=forward;best=s;}
        }
        return best;
    }
}
