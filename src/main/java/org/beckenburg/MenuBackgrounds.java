package org.beckenburg;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.imageio.ImageIO;

/** The actual ten batch-01 scene JPEGs, packaged in the runnable game JAR.
 * Decode only on first display and retain at most two images (~16 MB RGB);
 * never decode or resize a JPEG every Swing repaint.
 */
final class MenuBackgrounds {
    private static final String ROOT="/org/beckenburg/menu-backgrounds/";
    private static final String[] FILES={
        "01_castle_over_mist_menu_1920x1080.jpg",
        "02_lamplit_old_town_menu_1920x1080.jpg",
        "03_candlelit_study_menu_1920x1080.jpg",
        "04_stone_archway_menu_1920x1080.jpg",
        "05_night_lakeshore_menu_1920x1080.jpg",
        "06_forest_shrine_menu_1920x1080.jpg",
        "07_cloister_courtyard_menu_1920x1080.jpg",
        "08_mill_and_river_menu_1920x1080.jpg",
        "09_chapel_interior_menu_1920x1080.jpg",
        "10_ruined_viaduct_menu_1920x1080.jpg"
    };
    private static final Map<Integer,BufferedImage> CACHE=new LinkedHashMap<>(4,0.75f,true){
        @Override protected boolean removeEldestEntry(Map.Entry<Integer,BufferedImage> entry){
            return size()>2;
        }
    };
    private MenuBackgrounds(){}

    static int count(){return FILES.length;}
    static int sceneFor(String screen){
        return switch(screen){
            case "start.menu" -> 0;        // Castle over the mist
            case "extras" -> 1;            // Lamplit town
            case "options" -> 2;           // Study
            case "controls" -> 3;          // Stone archway
            case "solo" -> 4;              // Night lakeshore
            case "ghost" -> 5;             // Forest shrine, reserved for ghost lobby
            case "campaign" -> 6;          // Cloister courtyard, reserved for campaign
            case "network" -> 7;           // Mill and river, reserved for multiplayer
            case "display.confirm" -> 8;   // Chapel
            case "notice" -> 9;            // Ruined viaduct
            default -> 0;
        };
    }
    static synchronized BufferedImage image(int scene){
        if(scene<0||scene>=FILES.length)throw new IllegalArgumentException("Menu scene: "+scene);
        BufferedImage cached=CACHE.get(scene);
        if(cached!=null)return cached;
        try(var stream=MenuBackgrounds.class.getResourceAsStream(ROOT+FILES[scene])){
            if(stream==null)throw new IllegalStateException("Packaged menu background missing: "+FILES[scene]);
            BufferedImage decoded=ImageIO.read(stream);
            if(decoded==null)throw new IOException("Unrecognized menu JPEG: "+FILES[scene]);
            CACHE.put(scene,decoded);
            return decoded;
        }catch(IOException failure){throw new IllegalStateException("Cannot read packaged menu scene "+FILES[scene],failure);}
    }
    static void paint(Graphics2D graphics,int scene,int width,int height){
        if(width<=0||height<=0)return;
        BufferedImage source=image(scene);
        // 16:9 cover (no stretching); crop the LEFT on narrow windows to
        // preserve the authored focal point on the right of the navigation.
        double scale=Math.max((double)width/source.getWidth(),(double)height/source.getHeight());
        int drawnWidth=(int)Math.ceil(source.getWidth()*scale);
        int drawnHeight=(int)Math.ceil(source.getHeight()*scale);
        int x=Math.min(0,width-drawnWidth),y=(height-drawnHeight)/2;
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.drawImage(source,x,y,drawnWidth,drawnHeight,null);
        graphics.setPaint(new GradientPaint(0,0,new Color(5,8,14,175),
            Math.max(1,(int)(width*0.72)),0,new Color(5,8,14,0)));
        graphics.fillRect(0,0,width,height);
    }
}
