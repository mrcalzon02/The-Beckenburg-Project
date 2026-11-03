package org.beckenburg;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipInputStream;
import javax.imageio.ImageIO;

/** Cosmetic poses driven by received teammate positions, never local input. */
public final class HunterSpriteAnimation {
    public enum Pose { IDLE, WALK_A, WALK_B, CROUCH }
    private static final String[] VIEWS={"front","front","side","three_quarter",
        "back","three_quarter","side","front"};
    private static final Map<String,BufferedImage> FRAMES=load();
    private HunterSpriteAnimation(){}

    private static Map<String,BufferedImage> load(){
        Map<String,BufferedImage> frames=new HashMap<>();
        try(var source=HunterSpriteAnimation.class.getResourceAsStream("hunter-animation.zip")){
            if(source==null)throw new IOException("Missing hunter-animation.zip");
            try(var zip=new ZipInputStream(source)){
                java.util.zip.ZipEntry entry;
                while((entry=zip.getNextEntry())!=null){
                    String name=entry.getName();
                    if(!name.matches("(cleric|observer|demonologist)/(walk_a|walk_b|crouch)/(front|side|back|three_quarter)[.]png"))
                        throw new IOException("Unexpected hunter animation: "+name);
                    BufferedImage image=ImageIO.read(zip);
                    if(image==null||image.getWidth()!=128||image.getHeight()!=224||!image.getColorModel().hasAlpha())
                        throw new IOException("Invalid hunter animation: "+name);
                    if(frames.put(name.substring(0,name.length()-4),image)!=null)
                        throw new IOException("Duplicate hunter animation: "+name);
                }
            }
            if(frames.size()!=36)throw new IOException("Expected 36 hunter animation frames");
        }catch(IOException e){throw new IllegalStateException("Cannot load hunter animation",e);}
        return Map.copyOf(frames);
    }

    public static BufferedImage frame(String identity,int facing,Pose pose){
        if(!CharacterVisuals.isHunter(identity)||facing<0||facing>=8||pose==null)
            throw new IllegalArgumentException("Invalid hunter pose");
        if(pose==Pose.IDLE)return DirectionalSprites.frame(identity,facing);
        return FRAMES.get(identity+"/"+pose.name().toLowerCase(java.util.Locale.ROOT)+"/"+VIEWS[facing]);
    }

    /** One tracker per renderer; bounded by the six authoritative seat IDs. */
    public static final class Tracker {
        private record Step(double x,double z,double clock,double moved,double distance,String identity){}
        private final Map<Integer,Step> seats=new HashMap<>();
        public Pose sample(SceneRenderer.RemoteBody body,double clock){
            Step old=seats.get(body.slot());
            double moved=Double.NEGATIVE_INFINITY,distance=0;
            if(old!=null&&old.identity().equals(body.appearance())&&clock>=old.clock()&&clock-old.clock()<1){
                double delta=Math.hypot(body.x()-old.x(),body.z()-old.z());
                if(delta<2){
                    distance=old.distance()+delta;
                    moved=delta>.002?clock:old.moved();
                }
            }
            seats.put(body.slot(),new Step(body.x(),body.z(),clock,moved,distance,body.appearance()));
            if(body.crouched())return Pose.CROUCH;
            if(clock-moved>.20)return Pose.IDLE;
            return ((int)Math.floor(distance/.32)&1)==0?Pose.WALK_A:Pose.WALK_B;
        }
    }
}
