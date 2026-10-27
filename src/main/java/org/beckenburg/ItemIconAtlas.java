package org.beckenburg;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

/** UI-only, bounded image cache. Never loads GLBs or renders geometry in a menu. */
final class ItemIconAtlas {
    private ItemIconAtlas(){}
    private static final Map<String,BufferedImage> CACHE=new HashMap<>();
    private static final Map<String,ImageIcon> SIZED=new HashMap<>();
    private static final BufferedImage MISSING=new BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB);

    static String key(EquipmentLoadout.Item item){
        return switch(item){
            case EMF->"emf_reader";
            case THERMOMETER->"thermometer";
            case SPIRIT_BOX->"spirit_box";
            case UV_LAMP->"uv_light";
            case PHOTO_CAMERA->"photo_camera";
            case SALT->"salt";
        };
    }
    static String key(FieldSupplies.Kind kind){
        return kind==FieldSupplies.Kind.HEALTH?"health_supplies":"sanity_medication";
    }
    static String key(EquipmentRegistry.Tool tool){
        return tool.id().toLowerCase(Locale.ROOT);
    }
    static ImageIcon get(String key,int pixels){
        if(key==null||!key.matches("[a-z0-9_]+")||pixels<16||pixels>256)return null;
        synchronized(CACHE){
            String sizeKey=key+":"+pixels;
            ImageIcon ready=SIZED.get(sizeKey);
            if(ready!=null)return ready;
            BufferedImage image=CACHE.get(key);
            if(image==null){
                try(var stream=ItemIconAtlas.class.getResourceAsStream(
                        "/org/beckenburg/ui/item-icons/"+key+".png")){
                    image=stream==null?MISSING:ImageIO.read(stream);
                    if(image==null)image=MISSING;
                }catch(IOException error){image=MISSING;}
                CACHE.put(key,image);
            }
            if(image==MISSING)return null;
            BufferedImage scaled=new BufferedImage(pixels,pixels,BufferedImage.TYPE_INT_ARGB);
            Graphics2D g=scaled.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(image,0,0,pixels,pixels,null);
            g.dispose();
            ready=new ImageIcon(scaled);
            SIZED.put(sizeKey,ready);
            return ready;
        }
    }
}
