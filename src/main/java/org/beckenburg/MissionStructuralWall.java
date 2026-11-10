package org.beckenburg;

import java.util.List;
import java.util.Set;
import org.beckenburg.LevelDefinition.*;

/**
 * One editable, solid, axis-aligned mission partition. This is intentionally
 * NOT a new room, a decorative billboard or a substitute floor/roof API.
 * The same dimensions feed one native collision body and an actual textured
 * renderer mesh through a versioned wall socket in LevelDefinition.
 */
public record MissionStructuralWall(String id,int room,double x0,double z0,double x1,double z1,
                             double base,double height,double thickness,int material) {
    static final String NODE_PREFIX="beckenburg-wall-";
    static final String COMPONENT="beckenburg.level.structural-wall";
    static final String SOCKET_TYPE="structural_wall";
    static final Set<Integer> MATERIALS=Set.of(LevelMaterials.PLASTER,LevelMaterials.WOOD,
        LevelMaterials.TILE,LevelMaterials.CONCRETE,LevelMaterials.STONE,LevelMaterials.WALL_TILE);

    MissionStructuralWall {
        if(id==null||!id.matches("[A-Za-z0-9_-]{1,64}"))
            throw new IllegalArgumentException("Invalid stable wall ID");
        if(!Double.isFinite(x0+z0+x1+z1+base+height+thickness)||
           (x0==x1)==(z0==z1))
            throw new IllegalArgumentException("Wall must be finite, straight and axis aligned");
        double length=Math.hypot(x1-x0,z1-z0);
        if(length<.75||length>4.0||height<1.8||height>4.5||
           thickness<.12||thickness>.50||!MATERIALS.contains(material))
            throw new IllegalArgumentException("Wall length, height, thickness or material out of bounds");
    }
    double length(){return Math.hypot(x1-x0,z1-z0);}
    double cx(){return (x0+x1)/2;}
    double cz(){return (z0+z1)/2;}
    double yaw(){return x0==x1?Math.PI/2:0;}
    double halfX(){return length()/2;}
    double halfZ(){return thickness/2;}
    Collider body(){return new Collider(cx(),base,cz(),halfX(),halfZ(),height,yaw());}
    Socket socket(){
        return new Socket(NODE_PREFIX+id,SOCKET_TYPE,room,cx(),base,cz(),yaw(),
            length()+":"+thickness+":"+height+":"+material);
    }
    void validate(LevelDefinition source,List<MissionStructuralWall> previous){
        if(room<0||room>=source.rooms().size())
            throw new IllegalArgumentException("Unknown structural wall room");
        Room r=source.room(room);
        if(r.outside()||Math.abs(base-r.y())>.03||base+height>r.y()+r.height()-.06)
            throw new IllegalArgumentException("Wall needs an interior standing floor and ceiling clearance");
        double margin=.35;
        if(Math.min(x0,x1)-thickness/2<r.x0()+margin||
           Math.max(x0,x1)+thickness/2>r.x1()-margin||
           Math.min(z0,z1)-thickness/2<r.z0()+margin||
           Math.max(z0,z1)+thickness/2>r.z1()-margin)
            throw new IllegalArgumentException("Wall touches a source room boundary or existing wall");
        // Keep physical approach to existing doors, windows, stairs and scripted
        // site sockets. New partition walls must not sever required services.
        for(Portal p:source.portals())if(!p.kind().equals("stair")&&
            Math.abs(p.y()-base)<height+.4&&near(p.x(),p.z(),.85))
            throw new IllegalArgumentException("Wall intrudes on existing aperture "+p.id());
        for(Stair s:source.stairs())if((s.a()==room||s.b()==room)&&
            (near(s.x0(),s.z0(),1.2)||near(s.x1(),s.z1(),1.2)))
            throw new IllegalArgumentException("Wall intersects a stair landing");
        for(Socket s:source.sockets())if(s.room()==room&&!s.type().equals("ambient")
            &&near(s.x(),s.z(),.80)&&Math.abs(s.y()-base)<height+.5)
            throw new IllegalArgumentException("Wall blocks authored "+s.type()+" socket");
        for(Room other:source.rooms())if(other.id()!=room&&other.y()>=base-.1&&other.y()<base+height+.1
            &&other.x0()<=cx()&&cx()<=other.x1()&&other.z0()<=cz()&&cz()<=other.z1()
            &&!other.outside()&&(other.x1()-other.x0())*(other.z1()-other.z0())<
                (r.x1()-r.x0())*(r.z1()-r.z0())-.01)
            throw new IllegalArgumentException("Wall intrudes into subordinate room "+other.id());
        for(Prop p:source.props())if(p.room()==room&&p.halfX()>0&&p.halfZ()>0
            &&Math.abs(p.y()-base)<height+1&&near(p.x(),p.z(),
                .3+Math.hypot(p.halfX(),p.halfZ())))
            throw new IllegalArgumentException("Wall overlaps a built-in physical prop");
        for(Collider c:source.colliders())if(c.halfX()>0&&c.halfZ()>0&&
            Math.abs(c.y()-base)<height+.2&&near(c.x(),c.z(),
                .2+Math.hypot(c.halfX(),c.halfZ())))
            throw new IllegalArgumentException("Wall overlaps a native collider");
        for(MissionStructuralWall w:previous)if(!w.id().equals(id)&&w.room()==room&&
            Math.abs(w.base()-base)<height+.2&&(near(w.cx(),w.cz(),w.length()/2+.25)
                ||w.near(cx(),cz(),length()/2+.25)))
            throw new IllegalArgumentException("Wall overlaps another authored partition");
        if(near(r.spawn().x(),r.spawn().z(),.75))
            throw new IllegalArgumentException("Wall blocks room arrival or player approach");
    }
    boolean near(double x,double z,double clearance){
        // AABB distance to the occupied physical rectangular wall, X/Z up to
        // half-thickness. Conservative enough for short bounded partitions.
        double a=Math.min(x0,x1)-thickness/2,b=Math.max(x0,x1)+thickness/2;
        double c=Math.min(z0,z1)-thickness/2,d=Math.max(z0,z1)+thickness/2;
        double dx=Math.max(a-x,Math.max(0,x-b)),dz=Math.max(c-z,Math.max(0,z-d));
        return Math.hypot(dx,dz)<clearance;
    }
}
