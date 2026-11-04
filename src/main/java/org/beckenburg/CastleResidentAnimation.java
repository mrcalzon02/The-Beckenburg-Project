package org.beckenburg;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;
import java.util.zip.ZipInputStream;
import javax.imageio.ImageIO;

/** Small, collision checked work routines; residents remain near their services. */
public final class CastleResidentAnimation {
    private CastleResidentAnimation(){}
    public record Body(double x,double z,int heading,HunterSpriteAnimation.Pose pose){}
    public static String identity(CastleRelations.Resident resident){
        return switch(resident){
            case MATTHIS_ROOK -> "lore_master";
            case ELSBETH_VALE -> "elsbeth_vale";
            case ROWAN_PIKE -> "rowan_pike";
            case TOMAS_GREAVES -> "tomas_greaves";
            case MIRELLE_ASH -> "sister_mirelle_ash";
        };
    }
    private static final class Art {
        static final Map<String,BufferedImage> FRAMES=load();
        private static Map<String,BufferedImage> load(){
            var frames=new HashMap<String,BufferedImage>();
            try(var source=CastleResidentAnimation.class.getResourceAsStream("castle-resident-animation.zip")){
                if(source==null)throw new IOException("Missing resident animation atlas");
                try(var zip=new ZipInputStream(source)){
                    java.util.zip.ZipEntry entry;
                    while((entry=zip.getNextEntry())!=null){
                        String name=entry.getName();
                        if(!name.matches("(lore_master|elsbeth_vale|rowan_pike|tomas_greaves|sister_mirelle_ash)/(walk_a|walk_b|crouch)/(front|side|back|three_quarter|right)[.]png"))
                            throw new IOException("Unexpected resident frame: "+name);
                        var image=ImageIO.read(zip);
                        if(image==null||image.getWidth()!=128||image.getHeight()!=224||!image.getColorModel().hasAlpha()
                                ||frames.put(name.substring(0,name.length()-4),image)!=null)
                            throw new IOException("Invalid resident frame: "+name);
                    }
                }
                if(frames.size()!=60)throw new IOException("Expected 60 resident frames");
                return Map.copyOf(frames);
            }catch(IOException e){throw new IllegalStateException("Cannot load resident animation",e);}
        }
    }
    public static BufferedImage frame(CastleRelations.Resident resident,int facing,HunterSpriteAnimation.Pose pose){
        Objects.requireNonNull(resident);Objects.requireNonNull(pose);
        if(facing<0||facing>7)throw new IllegalArgumentException("Invalid facing");
        String id=identity(resident);
        if(pose==HunterSpriteAnimation.Pose.IDLE)return DirectionalSprites.frame(id,facing);
        String[] views=resident==CastleRelations.Resident.MATTHIS_ROOK
            ?new String[]{"front","front","side","back","back","back","right","front"}
            :new String[]{"front","front","side","three_quarter","back","three_quarter","side","front"};
        var image=Art.FRAMES.get(id+"/"+pose.name().toLowerCase(Locale.ROOT)+"/"+views[facing]);
        if(image==null)throw new IllegalStateException("Missing resident pose "+id+pose+facing);
        return image;
    }
    public static final class Routine {
        private final EnumMap<CastleRelations.Resident,Body> bodies=new EnumMap<>(CastleRelations.Resident.class);
        private double previous=Double.NaN;
        private final double[] distance=new double[CastleRelations.Resident.values().length];
        public Body body(CastleRelations.Resident r){return bodies.getOrDefault(r,new Body(r.x,r.z,0,HunterSpriteAnimation.Pose.IDLE));}
        public void update(CastleHubMap map,double clock,double playerX,double playerZ){
            if(!Double.isFinite(clock))return;
            if(!Double.isFinite(previous)||clock<previous||clock-previous>1){
                bodies.clear();Arrays.fill(distance,0);previous=clock;return;
            }
            double dt=clock-previous;previous=clock;
            if(dt<=0)return;
            for(var r:CastleRelations.Resident.values()){
                Body old=body(r);int index=r.ordinal();
                double phase=(clock+index*3.7)%22;
                // Pause upright for an approaching player, including during dialogue.
                if(Math.hypot(old.x-playerX,old.z-playerZ)<3.4){
                    bodies.put(r,new Body(old.x,old.z,old.heading,HunterSpriteAnimation.Pose.IDLE));continue;
                }
                double targetX=r.x,targetZ=r.z;
                if(phase>=5&&phase<13){
                    // Find a supported short walk beside this workstation, never across rooms.
                    for(int direction=0;direction<8;direction++){
                        double a=(direction+index)*Math.PI/4;
                        double tx=r.x+Math.sin(a)*.75,tz=r.z+Math.cos(a)*.75;
                        if(clearPath(map,r.x,r.z,tx,tz)){targetX=tx;targetZ=tz;break;}
                    }
                }
                double dx=targetX-old.x,dz=targetZ-old.z,len=Math.hypot(dx,dz),step=Math.min(len,dt*.48);
                double nx=len>.0001?old.x+dx/len*step:old.x,nz=len>.0001?old.z+dz/len*step:old.z;
                boolean moving=step>.0001&&clearPath(map,old.x,old.z,nx,nz)
                    &&Math.hypot(nx-playerX,nz-playerZ)>.7;
                if(moving){
                    distance[index]+=step;
                    int heading=(int)Math.round(Math.toDegrees(Math.atan2(dx,dz))+360)%360;
                    bodies.put(r,new Body(nx,nz,heading,((int)(distance[index]/.32)&1)==0
                        ?HunterSpriteAnimation.Pose.WALK_A:HunterSpriteAnimation.Pose.WALK_B));
                }else bodies.put(r,new Body(old.x,old.z,old.heading,phase>=10&&phase<13
                    ?HunterSpriteAnimation.Pose.CROUCH:HunterSpriteAnimation.Pose.IDLE));
            }
        }
        private static boolean clearPath(CastleHubMap map,double x,double z,double tx,double tz){
            var room=CastleRooms.at(x,0,z);var target=CastleRooms.at(tx,0,tz);
            if(room==null||target==null||!room.id().equals(target.id()))return false;
            int steps=Math.max(1,(int)Math.ceil(Math.hypot(tx-x,tz-z)/.08));
            for(int i=1;i<=steps;i++)if(!map.residentPlacementClear(x+(tx-x)*i/steps,z+(tz-z)*i/steps))return false;
            return true;
        }
    }
}
