package org.beckenburg;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Supplemental editor-authored world faces. Building physics remains authoritative. */
final class WorldTerrainSurface {
    private WorldTerrainSurface(){}
    private static final Map<String,AuthoredSitePhysics> CACHE=new ConcurrentHashMap<>();
    static boolean walking(LevelDefinition level){return level.sockets().stream().anyMatch(s->s.type().equals("world-terrain-walk"));}
    static AuthoredSitePhysics of(LevelDefinition level){
        var bindings=level.sockets().stream().filter(s->s.type().equals("world-terrain")).toList();
        if(bindings.isEmpty())return null;
        if(bindings.size()!=1)throw new IllegalArgumentException("One world terrain authority required");
        String reference=bindings.get(0).style();
        if(!reference.matches("[a-z0-9-]+\\.hcs2#[a-f0-9]{64}"))
            throw new IllegalArgumentException("Invalid terrain resource binding");
        return CACHE.computeIfAbsent(reference,key->{
            String[] parts=key.split("#");
            try(var in=WorldTerrainSurface.class.getResourceAsStream("/org/beckenburg/sites/"+parts[0])){
                if(in==null)throw new IOException("Missing editor terrain "+parts[0]);
                return AuthoredSitePhysics.read(in,parts[1],List.of());
            }catch(IOException failure){throw new IllegalStateException("Cannot load verified world terrain",failure);}
        });
    }
}
