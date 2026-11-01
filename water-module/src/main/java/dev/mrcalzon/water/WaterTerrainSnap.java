package dev.mrcalzon.water;

import java.util.List;
import java.util.Objects;

/**
 * A terrain river/channel endpoint presented to the EXISTING WaterSnap edge
 * matcher. These sockets are not additional fluid bodies: they only define
 * connection geometry, leaving water simulation and settings in WaterModule.
 */
public final class WaterTerrainSnap {
    public record Socket(String id,double x,double y,double z,
                         double downstreamX,double downstreamZ,double width) {
        public Socket {
            if(id==null||!id.matches("[A-Za-z0-9_-]{1,48}")||
                !Double.isFinite(x+y+z+downstreamX+downstreamZ+width)||
                Math.hypot(downstreamX,downstreamZ)<1e-7||
                width<.1||width>100)throw new IllegalArgumentException("Invalid river socket");
            double n=Math.hypot(downstreamX,downstreamZ);
            downstreamX/=n;downstreamZ/=n;
        }
    }
    public record Connection(WaterPlacement water,WaterSnap.Match match) {}
    private WaterTerrainSnap(){}

    /** Virtual shallow endpoint inserted into WaterSnap's real facing-edge test. */
    public static WaterPlacement socketFootprint(Socket socket){
        Objects.requireNonNull(socket);
        double lip=.4;
        double yaw=Math.atan2(-socket.downstreamX(),socket.downstreamZ());
        return new WaterPlacement("river-socket-"+socket.id(),
            socket.x()-socket.downstreamX()*lip*.5,
            socket.y(),socket.z()-socket.downstreamZ()*lip*.5,
            socket.width(),lip,yaw,new WaterSettings(),
            WaterPlacement.Shape.SQUARE);
    }
    /**
     * Attach an existing water placement flush to a terrain river endpoint.
     * The normal module controls rotation, height and edge overlap; a failure
     * returns null instead of moving a water body to a guessed location.
     */
    public static Connection connect(WaterPlacement moving,Socket socket,
                                     double tolerance,boolean angleSnap){
        Objects.requireNonNull(moving);Objects.requireNonNull(socket);
        var footprint=socketFootprint(socket);
        var match=WaterSnap.find(moving,List.of(footprint),tolerance,angleSnap);
        if(match==null)return null;
        WaterPlacement connected=WaterSnap.apply(moving,match,moving);
        return new Connection(connected,match);
    }
}
