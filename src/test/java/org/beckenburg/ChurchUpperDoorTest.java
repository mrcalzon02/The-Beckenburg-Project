package org.beckenburg;

import java.util.*;

/** An upper gallery door must penetrate both its deck wall and the containing
 * tall volume, with solid jambs/lintel/lower wall and real closed-door collision. */
public final class ChurchUpperDoorTest {
    private static void require(boolean value,String reason){if(!value)throw new AssertionError(reason);}
    private static double coord(Object value,String name)throws Exception{return ((Number)LevelSceneSource.read(value,name)).doubleValue();}
    private static boolean covers(List<?> faces,double plane,double z,double y)throws Exception{
        for(Object f:faces){
            Object a=LevelSceneSource.read(f,"a"),b=LevelSceneSource.read(f,"b"),c=LevelSceneSource.read(f,"c");
            if(Math.abs(coord(a,"x")-plane)>1e-6||Math.abs(coord(b,"x")-plane)>1e-6||Math.abs(coord(c,"x")-plane)>1e-6)continue;
            double ax=coord(a,"z"),ay=coord(a,"y"),bx=coord(b,"z"),by=coord(b,"y"),cx=coord(c,"z"),cy=coord(c,"y");
            double det=(by-cy)*(ax-cx)+(cx-bx)*(ay-cy);if(Math.abs(det)<1e-9)continue;
            double u=((by-cy)*(z-cx)+(cx-bx)*(y-cy))/det;
            double v=((cy-ay)*(z-cx)+(ax-cx)*(y-cy))/det;
            if(u>=-1e-7&&v>=-1e-7&&u+v<=1+1e-7)return true;
        }
        return false;
    }
    public static void main(String[] args)throws Exception{
        var map=new HouseMap(HouseMap.Site.CHURCH);var level=map.level();
        var method=SceneRenderer.class.getDeclaredMethod("levelWall",List.class,LevelDefinition.class,LevelDefinition.Room.class,boolean.class,double.class,double.class,double.class);
        method.setAccessible(true);
        for(int target:new int[]{8,9}){
            var door=level.portals().stream().filter(p->p.door()&&p.joins(7,target)).findFirst().orElseThrow();
            double x=door.x(),z=door.z(),y=door.y();
            require(!map.traversable(x,y,z),"Closed upper door must block player: "+target);
            require(!map.clearLineOfSight(x-.5,y+1.2,z,x+.5,y+1.2,z),"Closed upper door must occlude: "+target);
            map.openDoorBetweenRooms(7,target);
            require(map.traversable(x,y,z),"Open upper threshold blocked by containing nave/guard: "+target);
            require(map.traversable(x,y,z,1.05),"Crouched upper threshold blocked: "+target);
            require(map.clearLineOfSight(x-.5,y+1.2,z,x+.5,y+1.2,z),"Open upper door still has an invisible optical wall: "+target);
            var faces=new ArrayList<Object>();var nave=level.room(0);
            method.invoke(null,faces,level,nave,false,x,nave.z0(),nave.z1());
            double plane=x+(Math.abs(x-nave.x0())<.01?.08:-.08);
            require(!covers(faces,plane,z,y+1.2),"Rendered tall nave wall caps upper door: "+target);
            require(covers(faces,plane,z+door.width()/2+.25,y+1.2),"Upper opening removed its jamb: "+target);
            require(covers(faces,plane,z,y-.2),"Upper opening removed wall below its floor: "+target);
            require(covers(faces,plane,z,y+2.7),"Upper opening removed its solid lintel: "+target);
        }
        String before=map.doorSnapshot();
        var result=LevelTraversalAudit.inspect(map);
        require(result.passed(),"Church open routes fail: "+result.findings());
        require(result.highest()>=6&&result.lowest()<=-3,"Crypt and belfry must both be physically reached");
        require(before.equals(map.doorSnapshot()),"Audit changed live door state");
        System.out.println("PASS Church upper doors: rendered/physical holes, closed/open/crouch/optical controls, retained jamb/lintel/lower wall; all goals reached through "+result.positions()+" swept positions");
    }
}
