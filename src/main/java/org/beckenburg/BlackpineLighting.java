package org.beckenburg;
import java.util.*;
import org.beckenburg.LevelDefinition.*;
/** Authored fixture positions; existing mill room circuit owns every emitter. */
public final class BlackpineLighting {
    private BlackpineLighting(){}
    public static final double ROOF_UNDERSIDE=5.96, DROP=1.40;
    public static LevelDefinition apply(LevelDefinition l){
        var sockets=new ArrayList<>(l.sockets());
        sockets.removeIf(s->s.type().equals("light")&&s.room()==0);
        int i=0;
        for(double[] p:new double[][]{{43.2,26.7},{43.2,34.0},{49.0,24.0}}){
            String id="blackpine-mill-pendant-"+i++;
            sockets.add(new Socket(id,"light",0,p[0],ROOF_UNDERSIDE-DROP,p[1],0,"utility"));
            sockets.add(new Socket(id+"-support","landscape_detail",0,p[0],ROOF_UNDERSIDE,p[1],0,"level_mill_suspension|1|1|1"));
        }
        // Preserve the walking deck at Y3; its 160mm structural depth is below it.
        sockets.add(new Socket("blackpine-loft-structural-depth","deck_slab",11,49,3,34,0,".16"));
        // This fixture mounts on the actual lower face of the solid loft,
        // with emission beneath the deck, rather than inside the loft above it.
        sockets.add(new Socket("blackpine-mill-underloft","light",0,48.5,2.84,35.0,0,"utility"));
        return DeckSlabs.physical(new LevelDefinition(l.id(),"blackpine-supported-worklights-v5",l.title(),l.rooms(),l.portals(),l.stairs(),l.voids(),l.props(),sockets,l.colliders(),l.dock(),l.ambience()));
    }
}
