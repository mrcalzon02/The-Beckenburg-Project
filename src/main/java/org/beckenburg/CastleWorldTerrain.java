package org.beckenburg;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Saved editor topology supplies these exact render, body, support and optical faces. */
public final class CastleWorldTerrain {
    private CastleWorldTerrain(){}
    public static final double WEST=-24,NORTH=-8,EAST=48,SOUTH=40;
    public static boolean replaces(double x,double z){return x>=WEST&&x<EAST&&z>=NORTH&&z<SOUTH;}
    private static final class Loaded {
        private static final AuthoredSitePhysics SURFACE=read();
        private static AuthoredSitePhysics read(){
            try(var sha=CastleWorldTerrain.class.getResourceAsStream("/org/beckenburg/sites/castle-world-terrain.sha256");
                var mesh=CastleWorldTerrain.class.getResourceAsStream("/org/beckenburg/sites/castle-world-terrain.hcs2")){
                if(sha==null||mesh==null)throw new IOException("Missing Castle editor terrain export");
                String hash=new String(sha.readAllBytes(),StandardCharsets.US_ASCII).strip();
                if(!hash.matches("[a-f0-9]{64}"))throw new IOException("Invalid Castle terrain fingerprint");
                return AuthoredSitePhysics.read(mesh,hash,List.of());
            }catch(IOException failure){throw new IllegalStateException("Cannot load Castle editor terrain",failure);}
        }
    }
    public static AuthoredSitePhysics surface(){return Loaded.SURFACE;}
}
