package org.beckenburg;
/** Every built indoor location must light its actual authored arrival room. */
public final class AllMapLightingCoverageTest {
    public static void main(String[] args){for(var site:HouseMap.Site.values()){
        var map=new HouseMap(site);if(map.nativeSite())continue;
        var lights=new RoomLighting();var lighting=new FixtureLighting(map);var p=map.spawn(0);
        lighting.frame(lights,0);double on=lighting.sample(0,p.x(),p.y()+.8,p.z()).energy();
        lights.setBreaker(false);lighting.frame(lights,0);double off=lighting.sample(0,p.x(),p.y()+.8,p.z()).energy();
        if(on<=off+.01)throw new AssertionError(site+" entry beyond physical fixture coverage: "+on+" / "+off);
        for(var socket:map.level().sockets())if(socket.type().equals("light")&&socket.room()<12){var room=map.level().room(socket.room());if(socket.y()>room.y()+room.height()+.001)throw new AssertionError("Fixture above ceiling "+socket.id());}
        System.out.println("PASS "+site+" actual entry illumination, immediate blackout and ceiling mounting");
    }}
}
