package org.beckenburg;
import java.util.*;
/** Physical supports, source placement and real circuit behavior in the mill. */
public final class BlackpineLightingTest {
    static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
    public static void main(String[] args){
        var map=BlackpineLevel.map();var l=map.level();double floor=map.floorY(0);
        var lights=l.sockets().stream().filter(s->s.type().equals("light")&&s.room()==0).toList();
        check(lights.size()==4,"three pendants and one underloft fixture");
        for(var s:lights)if(s.id().contains("pendant")){
            var support=l.sockets().stream().filter(p->p.id().equals(s.id()+"-support")).findFirst().orElseThrow();
            check(Math.abs(support.y()-floor-5.96)<.0001,"support touches actual roof underside");
            check(Math.abs(support.y()-s.y()-1.4)<.0001,"fixture touches support crossbar");
            check(s.y()-.16>floor+4,"pendant safely above standing and stair approaches");
        }
        var mesh=GlbProps.model("level_mill_suspension");double lo=999,hi=-999;
        for(var t:mesh.triangles())for(var v:new GlbProps.Vertex[]{t.a(),t.b(),t.c()}){lo=Math.min(lo,v.y());hi=Math.max(hi,v.y());}
        check(Math.abs(lo+1.4)<.0001&&Math.abs(hi)<.0001,"ceiling-origin source retained, never floor-normalized");
        var circuit=new RoomLighting();var field=new FixtureLighting(map);field.frame(circuit,1);
        double blade=field.sample(0,42.25,floor+2.2,34).energy();
        double loft=field.sample(0,48.5,floor+1.4,35).energy();
        check(blade>.45,"circular blade receives useful real fixture light: "+blade);
        check(loft>.6,"underloft light is not hidden above its deck: "+loft);
        var plate=l.socket("switch",0);
        var observer=InteractionFocusTest.find(map,plate.x(),plate.y(),plate.z(),InteractionFocus.Kind.SWITCH,"0",List.of());
        check(observer!=null,"mill's existing physical switch remains reachable and center-ray targetable");
        circuit.toggle(0);field.frame(circuit,2);
        check(field.sample(0,42.25,floor+2.2,34).energy()<blade*.45,"mill switch actually cuts blade lighting");
        check(circuit.on(11),"upper loft keeps its own room circuit");
        circuit.toggle(0);circuit.setBreaker(false);field.frame(circuit,3);
        check(field.sample(0,48.5,floor+1.4,35).energy()<loft*.3,"breaker cuts underloft lighting");
        System.out.println("PASS supported ceiling pivots, below-deck emission, lit blade/aisle, exact switch target, circuit isolation and breaker blackout");
    }
}
