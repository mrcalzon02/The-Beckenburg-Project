package org.beckenburg;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

/** Binds an inspected investigation export to its measured collision and room volumes. */
record NativeInvestigationScene(AuthoredSitePhysics physics, GlbProps.Model model,
                                List<Space> spaces, String revision) {
    record Space(String id, int owner, double x0, double z0, double x1, double z1,
                 double floor, double height, boolean outside, boolean accessible) {
        Space {
            if(id.isBlank()||owner<0||owner>=HouseMap.ROOM_COUNT
                ||!(x0<x1&&z0<z1&&height>0)||!Double.isFinite(floor+height+x0+z0+x1+z1))
                throw new IllegalArgumentException("Invalid native room volume "+id);
        }

        boolean contains(double x,double y,double z){
            return x>=x0&&x<=x1&&z>=z0&&z<=z1&&y>=floor-(outside?1.5:.22)&&y<floor+height;
        }

        double area(){return (x1-x0)*(z1-z0);}

        boolean precedes(Space other){
            if(outside!=other.outside)return !outside;
            if(floor!=other.floor)return floor>other.floor;
            return area()<other.area();
        }
    }

    NativeInvestigationScene {
        spaces=List.copyOf(spaces);
        var ids=new HashSet<String>();
        for(var space:spaces)if(!ids.add(space.id()))
            throw new IllegalArgumentException("Duplicate native room "+space.id());
    }

    static NativeInvestigationScene read(InputStream glb,String glbSha,InputStream collision,String collisionSha,
                                         Set<String> stairs,List<Space> spaces)throws IOException{
        var bytes=glb.readNBytes(16_000_001);
        if(bytes.length>16_000_000||!digest(bytes).equals(glbSha))
            throw new IOException("Native level GLB source hash mismatch or size exceeded");
        var physics=AuthoredSitePhysics.read(collision,collisionSha,List.of(),stairs);
        String manifest=glbSha+"|"+collisionSha+"|"+stairs.stream().sorted().toList()+"|"+spaces;
        String revision=digest(manifest.getBytes(StandardCharsets.UTF_8));
        return new NativeInvestigationScene(physics,GlbProps.decodeLevel(bytes),spaces,revision);
    }

    private static String digest(byte[] bytes){
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}
        catch(NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}
    }

    Space spaceAt(double x,double y,double z){
        Space owner=null;
        for(var space:spaces)
            if(space.contains(x,y,z)&&(owner==null||space.precedes(owner)))owner=space;
        return owner;
    }

    int ownerAt(double x,double y,double z){
        var space=spaceAt(x,y,z);
        return space==null||!space.accessible()?-1:space.owner();
    }
}
