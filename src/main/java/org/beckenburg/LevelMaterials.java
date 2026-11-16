package org.beckenburg;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicReferenceArray;
import javax.imageio.ImageIO;

/** Original PSX library diffuse images, sampled at their native resolution. */
public final class LevelMaterials {
    private LevelMaterials(){}
    public static final int BASE=32,PLASTER=32,WOOD=33,FLOOR=34,TILE=35,CONCRETE=36,STONE=37,RUG=38,CLADDING=39,WALL_TILE=40,CEILING=41,ROCK=42,ROOF=43;
    private static final String[] FILES={"floor_ceiling_2_1.png","wall_1_1.png","floor_ceiling_1_0.png",
        "floor_ceiling_2_0.png","floor_ceiling_1_1.png","wall_1_plain_0.png","carpet_mp_1_0.png","wall_1_1.png","floor_ceiling_2_1.png","floor_ceiling_2_1.png","beckenburg_rock_worn.png","floor_ceiling_3_1.png"};
    private record Texture(int width, int height, int[] pixels) {}
    private static final AtomicReferenceArray<Texture> TEXTURES = new AtomicReferenceArray<>(FILES.length);

    private static synchronized Texture loadTexture(int index) {
        Texture cached = TEXTURES.get(index);
        if (cached != null) return cached;

        String file = FILES[index];
        try (InputStream in = LevelMaterials.class.getResourceAsStream("/org/beckenburg/level-textures/" + file)) {
            if (in == null) throw new IOException("missing " + file);
            BufferedImage image = ImageIO.read(in);
            if (image == null) throw new IOException("invalid image " + file);
            int width = image.getWidth(), height = image.getHeight();
            Texture texture = new Texture(width, height, image.getRGB(0, 0, width, height, null, 0, width));
            // Several finishes use the same diffuse PNG. Publish its immutable pixels once.
            for (int i = 0; i < FILES.length; i++) {
                if (file.equals(FILES[i])) TEXTURES.set(i, texture);
            }
            return texture;
        } catch (IOException e) {
            throw new IllegalStateException("Original environment texture unavailable", e);
        }
    }
    public static int sample(int id,double u,double v){
        int index = id - BASE;
        if (index < 0 || index >= FILES.length) throw new IllegalArgumentException("level material");
        Texture texture = TEXTURES.get(index);
        if (texture == null) texture = loadTexture(index);
        int x=Math.min(texture.width-1,(int)((u-Math.floor(u))*texture.width));
        int y=Math.min(texture.height-1,(int)((1-(v-Math.floor(v)))*texture.height));
        int rgb=texture.pixels[y*texture.width+x]&0xffffff;
        if(id==ROOF){int grey=(((rgb>>16)&255)*3+((rgb>>8)&255)*5+(rgb&255)*2)/10;return ((grey*65/100)<<16)|((grey*70/100)<<8)|(grey*77/100);}
        if(id==WALL_TILE){
            double tu=u*8-Math.floor(u*8),tv=v*4-Math.floor(v*4);
            if(tu<.04||tv<.04)return 0x858477;
            int grain=((((rgb>>16)&255)+((rgb>>8)&255)+(rgb&255))/3-75)/4;
            return ((Math.min(255,194+grain))<<16)|(Math.min(255,190+grain)<<8)|Math.min(255,172+grain);
        }
        if(id==CEILING){int grain=((((rgb>>16)&255)+((rgb>>8)&255)+(rgb&255))/3-75)/3;
            return ((Math.min(255,157+grain))<<16)|(Math.min(255,151+grain)<<8)|Math.min(255,137+grain);}
        if(id==CLADDING){int grey=(((rgb>>16)&255)*3+((rgb>>8)&255)*5+(rgb&255)*2)/10;
            return (Math.min(255,grey+22)<<16)|(Math.min(255,grey+27)<<8)|Math.min(255,grey+30);}
        return rgb;
    }
    public static int floor(LevelDefinition.Room r){
        if(r.outside())return r.id()==12?MaterialAtlas.FOLIAGE:MaterialAtlas.CONCRETE;
        String f=r.finish().toLowerCase();
        return f.contains("tile")||f.contains("kitchen")||f.contains("bath")?TILE:
            f.contains("stone")?STONE:f.contains("concrete")||r.y()<0?CONCRETE:FLOOR;
    }
    public static int wall(LevelDefinition.Room r){
        String f=r.finish().toLowerCase();
        return f.contains("tile")?WALL_TILE:f.contains("stone")||f.contains("brick")?STONE:
            f.contains("concrete")?PLASTER:r.name().toLowerCase().contains("barn")||r.name().toLowerCase().contains("shed")?WOOD:PLASTER;
    }
    public static boolean panelled(LevelDefinition.Room r){String f=r.finish().toLowerCase();
        return f.contains("wallpaper")||f.contains("parquet")||f.contains("plaster")
            ||r.name().toLowerCase().contains("bedroom")||r.name().toLowerCase().contains("farm parlour");}
}
