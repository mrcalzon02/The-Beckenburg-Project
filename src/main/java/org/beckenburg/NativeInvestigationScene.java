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
                                List<Space> spaces, List<NativeInvestigationDoor> doors, String revision) {
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
        doors=List.copyOf(doors);
        var ids=new HashSet<String>();
        for(var space:spaces)if(!ids.add(space.id()))
            throw new IllegalArgumentException("Duplicate native room "+space.id());
    }

    static NativeInvestigationScene read(InputStream glb,String glbSha,InputStream collision,String collisionSha,
                                         Set<String> stairs,List<Space> spaces)throws IOException{
        return read(glb,glbSha,collision,collisionSha,stairs,spaces,List.of());
    }

    static NativeInvestigationScene read(InputStream glb,String glbSha,InputStream collision,String collisionSha,
                                         Set<String> stairs,List<Space> spaces,
                                         List<NativeInvestigationDoor.Spec> frames)throws IOException{
        return read(glb,glbSha,collision,collisionSha,stairs,spaces,frames,Set.of());
    }

    static NativeInvestigationScene read(InputStream glb,String glbSha,InputStream collision,String collisionSha,
                                         Set<String> stairs,List<Space> spaces,List<NativeInvestigationDoor.Spec> frames,
                                         Set<String> grates)throws IOException{
        var bytes=glb.readNBytes(16_000_001);
        if(bytes.length>16_000_000||!digest(bytes).equals(glbSha))
            throw new IOException("Native level GLB source hash mismatch or size exceeded");
        var physics=AuthoredSitePhysics.read(collision,collisionSha,List.of(),stairs,grates);
        String manifest=glbSha+"|"+collisionSha+"|"+stairs.stream().sorted().toList()+"|"+spaces;
        List<NativeInvestigationDoor> doors=new java.util.ArrayList<>();
        if(!frames.isEmpty()){
            var byId=new java.util.HashMap<String,Space>();
            for(var space:spaces)byId.put(space.id(),space);
            var portalIds=new HashSet<Integer>();
            var frameIds=new HashSet<String>();
            String assetSha;
            try(var asset=GlbProps.class.getResourceAsStream("/org/beckenburg/models/"+NativeInvestigationDoor.ASSET+".glb")){
                if(asset==null)throw new IOException("Missing packaged native door leaf");
                var sourceBytes=asset.readNBytes(4_000_001);
                if(sourceBytes.length>4_000_000)throw new IOException("Native door leaf source exceeds budget");
                assetSha=digest(sourceBytes);
            }
            var source=GlbProps.model(NativeInvestigationDoor.ASSET);
            for(var frame:frames.stream().sorted(java.util.Comparator.comparingInt(f->f.portal().id())).toList()){
                var p=frame.portal();
                var a=byId.get(frame.roomA());
                var b=byId.get(frame.roomB());
                if(!assetSha.equals(frame.leafSourceSha256())||a==null||b==null||!a.accessible()||!b.accessible()||a.owner()!=p.a()||b.owner()!=p.b()
                        ||!portalIds.add(p.id())||!frameIds.add(frame.frameId()))
                    throw new IOException("Invalid native door source or ownership for "+frame.frameId());
                doors.add(NativeInvestigationDoor.fit(frame,source));
            }
            manifest+="|native-hinges-v1|"+doors.stream().map(NativeInvestigationDoor::spec).toList();
        }
        if(!grates.isEmpty())manifest+="|native-walking-grates-v1|"+grates.stream().sorted().toList();
        String revision=digest(manifest.getBytes(StandardCharsets.UTF_8));
        return new NativeInvestigationScene(physics,GlbProps.decodeLevel(bytes),spaces,doors,revision);
    }

    NativeInvestigationScene withSlidingDoor(NativeInvestigationDoor.Spec frame,InputStream source,double travel)
            throws IOException {
        var gate=frame.portal();
        var a=spaces.stream().filter(space->space.id().equals(frame.roomA())).findFirst().orElseThrow();
        var b=spaces.stream().filter(space->space.id().equals(frame.roomB())).findFirst().orElseThrow();
        if(!a.accessible()||!b.accessible()||a.owner()!=gate.a()||b.owner()!=gate.b()
                ||!Double.isFinite(travel)||Math.abs(travel)<gate.width()||Math.abs(travel)>4
                ||doors.stream().anyMatch(d->d.spec().portal().id()==gate.id()||d.spec().frameId().equals(frame.frameId())))
            throw new IOException("Invalid native gate travel or ownership for "+frame.frameId());
        byte[] bytes=source.readNBytes(4_000_001);
        if(bytes.length>4_000_000||!digest(bytes).equals(frame.leafSourceSha256()))
            throw new IOException("Native sliding panel source hash mismatch or size exceeded");
        var fitted=new java.util.ArrayList<>(doors);
        fitted.add(NativeInvestigationDoor.slide(frame,GlbProps.decodeLevel(bytes),travel));
        var manifest=revision+"|native-slide-v1|"+frame+"|"+travel;
        return new NativeInvestigationScene(physics,model,spaces,fitted,digest(manifest.getBytes(StandardCharsets.UTF_8)));
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
