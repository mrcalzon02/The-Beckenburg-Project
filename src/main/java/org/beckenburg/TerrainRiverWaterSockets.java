package org.beckenburg;

import dev.mrcalzon.water.WaterTerrainSnap;
import org.beckenburg.editor.StitchedHeightmapWorld;
import org.beckenburg.editor.TerrainInfrastructureSplines;
import org.beckenburg.editor.TerrainLayerStack;
import java.util.*;

/**
 * Adapter only: authored terrain RIVER spline control points become explicit
 * terrain snap sockets for the already installed WaterModule editor.
 * No duplicated water renderer, footprint test or fluid simulation.
 */
public final class TerrainRiverWaterSockets {
    private TerrainRiverWaterSockets(){}
    public static List<WaterTerrainSnap.Socket> fromRoute(
            TerrainInfrastructureSplines.Alignment route,
            StitchedHeightmapWorld.Settings world){
        Objects.requireNonNull(route);Objects.requireNonNull(world);
        if(route.kind()!=TerrainInfrastructureSplines.Kind.RIVER)
            throw new IllegalArgumentException("Only actual river routes can publish water sockets");
        var controls=route.controls();
        List<WaterTerrainSnap.Socket> sockets=new ArrayList<>();
        double step=world.metresPerCell();
        for(int i=0;i<controls.size();i++){
            var p=controls.get(i);
            TerrainLayerStack.Point before=controls.get(Math.max(0,i-1));
            TerrainLayerStack.Point after=controls.get(Math.min(controls.size()-1,i+1));
            double dx=after.x()-before.x(),dz=after.z()-before.z();
            if(Math.hypot(dx,dz)<1e-7)
                throw new IllegalArgumentException("River spline socket has zero tangent");
            String safe=route.id().replaceAll("[^A-Za-z0-9_-]","_");
            String suffix="-c"+i;
            safe=safe.substring(0,Math.min(safe.length(),48-suffix.length()));
            sockets.add(new WaterTerrainSnap.Socket(safe+suffix,
                world.originX()+p.x()*step,p.elevation(),
                world.originZ()+p.z()*step,dx,dz,
                route.halfWidth()*2*step));
        }
        return List.copyOf(sockets);
    }
}
