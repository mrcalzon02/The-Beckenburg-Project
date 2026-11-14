package org.beckenburg;

import java.util.*;
import org.beckenburg.LevelDefinition.*;

/**
 * A first-class architectural child room assembled from FOUR persisted native
 * wall nodes and one real source-owned door portal. Its floor is the parent
 * room's standing floor; its ceiling is a solid textured/physical slab. The
 * fifteen gameplay-zone IDs and the multiplayer room protocol remain intact.
 *
 * No second layout document exists: discover() reconstructs each room from
 * its actual wall geometry. Missing, modified or orphaned members fail closed.
 */
final class MissionArchitecturalRoom {
    static final String WALL_STEM="archroom-";
    static final String NODE_STEM="beckenburg-architectural-room-";
    static final String ROOF_STEM="beckenburg-architectural-roof-";
    static final String ROOF_TYPE="architectural_roof";
    static final double ROOF_DEPTH=.18, WALL_THICKNESS=.20;
    static final double DOOR_WIDTH=.90, DOOR_HEIGHT=1.92;

    private MissionArchitecturalRoom(){}

    record Cell(String id,int zone,double x0,double z0,double x1,double z1,
                double base,double height,int doorPortalId,String doorSide,int material){
        double cx(){return (x0+x1)*.5;}
        double cz(){return (z0+z1)*.5;}
        double width(){return x1-x0;}
        double depth(){return z1-z0;}
        String nodeId(){return NODE_STEM+id;}
        boolean contains(double x,double y,double z){
            return x>x0+WALL_THICKNESS*.5&&x<x1-WALL_THICKNESS*.5
                &&z>z0+WALL_THICKNESS*.5&&z<z1-WALL_THICKNESS*.5
                &&y>=base-.04&&y<base+height-ROOF_DEPTH;
        }
        Collider roofBody(){
            return new Collider(cx(),base+height-ROOF_DEPTH,cz(),
                width()*.5,depth()*.5,ROOF_DEPTH,0);
        }
        Socket roofSocket(){
            // Both renderer and runtime reconstruct the same physical AABB.
            return new Socket(ROOF_STEM+id,ROOF_TYPE,zone,cx(),
                base+height-ROOF_DEPTH,cz(),0,
                width()+":"+depth()+":"+ROOF_DEPTH+":"+material+":"+base);
        }
        void validate(LevelDefinition source,List<Prop> props){
            if(zone<0||zone>=source.rooms().size())
                throw new IllegalArgumentException("Unknown child-room gameplay owner");
            var parent=source.room(zone);
            if(parent.outside()||Math.abs(parent.y()-base)>.025||
                base+height>parent.y()+parent.height()-.06)
                throw new IllegalArgumentException("Child room must rest on its native indoor floor");
            for(var prop:props)if(prop.room()==zone&&prop.halfX()>0&&prop.halfZ()>0&&
                prop.y()<base+height+.03&&prop.y()+prop.height()>base+height-ROOF_DEPTH-.03&&
                Math.abs(prop.x()-cx())<width()*.5+prop.halfX()&&
                Math.abs(prop.z()-cz())<depth()*.5+prop.halfZ())
                throw new IllegalArgumentException("Child-room ceiling would intersect an authored prop");
        }
    }

    static boolean reserved(String wallId){return wallId!=null&&wallId.startsWith(WALL_STEM);}
    private static String wallId(String id,String side){return WALL_STEM+id+"-"+side;}
    static List<MissionStructuralWall> construct(String id,int zone,double x0,double z0,
            double x1,double z1,double base,double height,int material,String doorSide,int portalId){
        if(id==null||!id.matches("[a-z][a-z0-9_-]{1,23}"))
            throw new IllegalArgumentException("Child-room ID must be 2..24 lowercase identifier characters");
        if(!Double.isFinite(x0+z0+x1+z1+base+height)||
            x1-x0<2.0||x1-x0>3.8||z1-z0<2.0||z1-z0>3.8||
            height<2.20||height>3.1)
            throw new IllegalArgumentException("Child-room footprint or clear roof height outside bounded construction");
        if(!Set.of("N","E","S","W").contains(doorSide))
            throw new IllegalArgumentException("Child-room doorway must choose N/E/S/W");
        var walls=new ArrayList<MissionStructuralWall>();
        walls.add(new MissionStructuralWall(wallId(id,"N"),zone,x0,z0,x1,z0,
            base,height,WALL_THICKNESS,material));
        walls.add(new MissionStructuralWall(wallId(id,"E"),zone,x1,z0,x1,z1,
            base,height,WALL_THICKNESS,material));
        walls.add(new MissionStructuralWall(wallId(id,"S"),zone,x1,z1,x0,z1,
            base,height,WALL_THICKNESS,material));
        walls.add(new MissionStructuralWall(wallId(id,"W"),zone,x0,z1,x0,z0,
            base,height,WALL_THICKNESS,material));
        for(int i=0;i<walls.size();i++){
            var wall=walls.get(i);
            if(wall.id().equals(wallId(id,doorSide)))
                walls.set(i,wall.withOpening(0,DOOR_WIDTH,DOOR_HEIGHT).withDoorPortal(portalId));
        }
        return List.copyOf(walls);
    }

    private static boolean same(double a,double b){return Math.abs(a-b)<=1e-6;}
    /** Reject partial or deformed groups; a room never becomes metadata-only. */
    static List<Cell> discover(List<MissionStructuralWall> walls){
        var groups=new TreeMap<String,Map<String,MissionStructuralWall>>();
        for(var wall:walls){
            if(!reserved(wall.id()))continue;
            String rest=wall.id().substring(WALL_STEM.length());
            int split=rest.lastIndexOf('-');
            if(split<=0||split==rest.length()-1)
                throw new IllegalArgumentException("Malformed architectural child-room wall ID");
            String id=rest.substring(0,split),side=rest.substring(split+1);
            if(!id.matches("[a-z][a-z0-9_-]{1,23}")||
                !Set.of("N","E","S","W").contains(side))
                throw new IllegalArgumentException("Invalid child-room wall identity");
            if(groups.computeIfAbsent(id,k->new HashMap<>()).putIfAbsent(side,wall)!=null)
                throw new IllegalArgumentException("Duplicate child-room wall side");
        }
        var cells=new ArrayList<Cell>();
        for(var group:groups.entrySet()){
            String id=group.getKey();
            var edges=group.getValue();
            if(edges.size()!=4||!edges.keySet().equals(Set.of("N","E","S","W")))
                throw new IllegalArgumentException("Architectural child room "+id+" lacks a physical wall");
            var n=edges.get("N");var e=edges.get("E");
            var s=edges.get("S");var w=edges.get("W");
            double x0=n.x0(),x1=n.x1(),z0=n.z0(),z1=e.z1();
            if(x1-x0<2||x1-x0>3.8||z1-z0<2||z1-z0>3.8||
                !same(n.z1(),z0)||!same(e.x0(),x1)||!same(e.x1(),x1)||
                !same(e.z0(),z0)||!same(s.x0(),x1)||!same(s.x1(),x0)||
                !same(s.z0(),z1)||!same(s.z1(),z1)||
                !same(w.x0(),x0)||!same(w.x1(),x0)||
                !same(w.z0(),z1)||!same(w.z1(),z0))
                throw new IllegalArgumentException("Architectural child room "+id+" has broken corners");
            int portalId=-1;String doorSide=null;
            for(var side:List.of("N","E","S","W")){
                var edge=edges.get(side);
                if(edge.room()!=n.room()||!same(edge.base(),n.base())||
                    !same(edge.height(),n.height())||!same(edge.thickness(),WALL_THICKNESS)||
                    edge.material()!=n.material())
                    throw new IllegalArgumentException("Child-room walls disagree on native structure");
                if(edge.hasDoor()){
                    if(doorSide!=null||!same(edge.cutOffset(),0)||
                        !same(edge.cutWidth(),DOOR_WIDTH)||!same(edge.cutHeight(),DOOR_HEIGHT))
                        throw new IllegalArgumentException("Child-room door geometry changed without migration");
                    doorSide=side;portalId=edge.doorPortalId();
                }else if(edge.hasOpening())
                    throw new IllegalArgumentException("Child room has an unowned wall aperture");
            }
            if(doorSide==null)
                throw new IllegalArgumentException("Architectural child room needs one functional native entrance");
            cells.add(new Cell(id,n.room(),x0,z0,x1,z1,n.base(),
                n.height(),portalId,doorSide,n.material()));
        }
        return List.copyOf(cells);
    }
}
