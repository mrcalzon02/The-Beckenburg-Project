package org.beckenburg;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.imageio.ImageIO;

/** Camera-facing, depth-tested character art in the first-person software scene.
 * Images are individual approved PNG turnarounds, never geometric cube avatars.
 * The resource ZIP is packed into the actual shipped Beckenburg game JAR.
 */
public final class DirectionalSprites {
    private static final int W=SceneRenderer.W,H=SceneRenderer.H;
    private static final double FOCAL=153.0,NEAR=.065; // exactly match SceneRenderer
    private static final String[] FACINGS={"front","front","side", "back_three_quarter",
            "back","back_three_quarter_mirrored","side_mirrored","front"};
    // Direction names must initialize before load(): Java runs static fields in
    // declaration order, and a null MASTER_DIRECTIONS breaks packaged startup.
    // The master boards actually provide four views: front, side, back, rear 3/4.
    // Diagonal angles reuse only these real source views until dedicated art is approved.
    private static final String[] MASTER_FACINGS={
        "front","front","side","three_quarter","back","three_quarter","side","front"
    };
    private static final String[] MASTER_DIRECTIONS={"front","side","back","three_quarter"};
    // Rook's accepted source presents front, LEFT, back and RIGHT profiles;
    // do not invent an absent three-quarter angle.
    private static final String[] LORE_DIRECTIONS={"front","side","back","right"};
    private static final String[] LORE_FACINGS={
        "front","front","side","back","back","back","right","front"
    };
    private static final String[] SUPPORT_IDENTITIES={
        "elsbeth_vale","rowan_pike","tomas_greaves","sister_mirelle_ash"
    };
    public static boolean isCastleSupport(String identity){
        return java.util.Arrays.asList(SUPPORT_IDENTITIES).contains(identity);
    }
    private static boolean validCastleSpriteSize(BufferedImage image){
        if(image==null||!image.getColorModel().hasAlpha())return false;
        int w=image.getWidth(),h=image.getHeight();
        // Both the original 128x224 cutouts and source-RGB 256x448 repairs.
        // Keep the original world-space silhouette ratio unchanged.
        return w>=128&&w<=512&&h>=224&&h<=896&&w*7==h*4;
    }
    private static final Map<String,BufferedImage> IMAGES=load();
    private DirectionalSprites() { }

    private static Map<String,BufferedImage> load(){
        Map<String,BufferedImage> frames=new HashMap<>();
        try(var source=DirectionalSprites.class.getResourceAsStream("directional-sprites.zip")){
            if(source==null)throw new IllegalStateException("Missing packaged Beckenburg directional-sprites.zip");
            try(var zip=new ZipInputStream(source)){
                ZipEntry entry;
                while((entry=zip.getNextEntry())!=null){
                    String name=entry.getName();
                    if(!entry.isDirectory() && name.matches("assets/(observer|banshee)/[a-z_]+[.]png")){
                        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
                        zip.transferTo(bytes);
                        BufferedImage image=ImageIO.read(new ByteArrayInputStream(bytes.toByteArray()));
                        if(image==null||!image.getColorModel().hasAlpha())
                            throw new IOException("Invalid transparent character frame: "+name);
                        frames.put(name.substring(7,name.length()-4),image);
                    }
                    zip.closeEntry();
                }
            }
        }catch(IOException ex){throw new IllegalStateException("Cannot load Beckenburg sprite frames",ex);}
        for(String person:new String[]{"observer","banshee"}){
            for(String facing:new String[]{"front","side","side_mirrored","back_three_quarter",
                    "back_three_quarter_mirrored","back"}){
                if(!frames.containsKey(person+"/"+facing))
                    throw new IllegalStateException("Missing character frame: "+person+"/"+facing);
            }
        }
        // All six character-specific approved profiles are extracted from their
        // original four-view concept panels. An incomplete asset is a build defect,
        // never a reason to repaint every Hunter as the Observer/Banshee.
        try(var source=DirectionalSprites.class.getResourceAsStream("character-directions.zip")){
            if(source==null)throw new IllegalStateException("Missing approved four-view character resource");
            int count=0;
            try(var zip=new ZipInputStream(source)){
                ZipEntry entry;
                while((entry=zip.getNextEntry())!=null){
                    String name=entry.getName();
                    if(!entry.isDirectory()){
                        String[] parts=name.split("/");
                        if(parts.length!=3||!parts[0].equals("assets")
                                ||!CharacterVisuals.isHunter(parts[1])&&!CharacterVisuals.isElder(parts[1])
                                ||!java.util.Arrays.asList(MASTER_DIRECTIONS).contains(parts[2].replace(".png",""))
                                ||!parts[2].endsWith(".png"))
                            throw new IOException("Unexpected approved sprite entry: "+name);
                        BufferedImage image=ImageIO.read(zip);
                        if(image==null||!image.getColorModel().hasAlpha()
                                ||image.getWidth()!=128||image.getHeight()!=224)
                            throw new IOException("Invalid master sprite dimensions/alpha: "+name);
                        if(frames.put(parts[1]+"/"+parts[2].substring(0,parts[2].length()-4),image)!=null
                                &&!parts[1].equals("observer"))
                            throw new IOException("Duplicate master sprite: "+name);
                        count++;
                    }
                    zip.closeEntry();
                }
            }
            if(count!=24)throw new IOException("Expected 24 master sprite frames, received "+count);
            for(String identity:new String[]{"cleric","observer","demonologist",
                    "elder_blade_demon","elder_plague_demon","elder_nothic"})
                for(String view:MASTER_DIRECTIONS)
                    if(!frames.containsKey(identity+"/"+view))
                        throw new IOException("Missing master sprite "+identity+"/"+view);
        }catch(IOException ex){throw new IllegalStateException("Cannot read approved directional master art",ex);}

        // Twins always have two separately segmented physical actor images.
        try(var source=DirectionalSprites.class.getResourceAsStream("twins-directions.zip")){
            if(source==null)throw new IllegalStateException("Missing shipped two-body Twins character art");
            int count=0;
            try(var zip=new ZipInputStream(source)){
                ZipEntry entry;
                while((entry=zip.getNextEntry())!=null) {
                    if(entry.isDirectory()){zip.closeEntry();continue;}
                    String name=entry.getName();
                    String[] parts=name.split("/");
                    if(parts.length!=3||!parts[0].equals("twins")
                            ||!(parts[1].equals("a")||parts[1].equals("b"))
                            ||!parts[2].endsWith(".png")
                            ||!java.util.Arrays.asList(MASTER_DIRECTIONS)
                                    .contains(parts[2].substring(0,parts[2].length()-4)))
                        throw new IOException("Unexpected Twins actor frame "+name);
                    BufferedImage image=ImageIO.read(zip);
                    if(image==null||image.getWidth()!=128||image.getHeight()!=224
                            ||!image.getColorModel().hasAlpha())
                        throw new IOException("Bad Twins actor image "+name);
                    String key="twins_"+parts[1]+"/"+parts[2].substring(0,parts[2].length()-4);
                    if(frames.putIfAbsent(key,image)!=null)
                        throw new IOException("Duplicate Twins actor image "+key);
                    count++;
                    zip.closeEntry();
                }
            }
            if(count!=8)throw new IOException("Expected eight separate Twins actor images, got "+count);
            for(String actor:new String[]{"twins_a","twins_b"})
                for(String direction:MASTER_DIRECTIONS)
                    if(!frames.containsKey(actor+"/"+direction))
                        throw new IOException("Missing "+actor+"/"+direction);
        }catch(IOException ex){throw new IllegalStateException("Cannot load both canonical Twins",ex);}
        // The approved Matthis Rook board supplies four distinct, full-body views.
        try(var source=DirectionalSprites.class.getResourceAsStream("lore-master-directions.zip")){
            if(source==null)throw new IllegalStateException("Missing approved Lore Master sprite resource");
            int count=0;
            try(var zip=new ZipInputStream(source)){
                ZipEntry entry;
                while((entry=zip.getNextEntry())!=null){
                    if(entry.isDirectory()){zip.closeEntry();continue;}
                    String name=entry.getName();
                    if(!name.startsWith("assets/lore_master/")||!name.endsWith(".png"))
                        throw new IOException("Unexpected Lore Master sprite "+name);
                    String view=name.substring("assets/lore_master/".length(),name.length()-4);
                    if(!java.util.Arrays.asList(LORE_DIRECTIONS).contains(view))
                        throw new IOException("Unknown Lore Master view "+view);
                    BufferedImage image=ImageIO.read(zip);
                    if(image==null||image.getWidth()!=128||image.getHeight()!=224
                            ||!image.getColorModel().hasAlpha())
                        throw new IOException("Damaged Lore Master sprite "+view);
                    if(frames.putIfAbsent("lore_master/"+view,image)!=null)
                        throw new IOException("Duplicate Lore Master view "+view);
                    count++;zip.closeEntry();
                }
            }
            if(count!=4)throw new IOException("Expected four Lore Master views; got "+count);
        }catch(IOException ex){throw new IllegalStateException("Cannot load approved Lore Master sprites",ex);}
        // Four approved Castle residents have separate, full-body source views.
        // They are not workstation geometry and must be included in the game JAR.
        try(var source=DirectionalSprites.class.getResourceAsStream("castle-support-directions.zip")){
            if(source==null)throw new IllegalStateException("Missing Castle support character sprites");
            int count=0;
            try(var zip=new ZipInputStream(source)){
                ZipEntry entry;
                while((entry=zip.getNextEntry())!=null){
                    if(entry.isDirectory()){zip.closeEntry();continue;}
                    String name=entry.getName(),prefix="assets/castle_support/";
                    if(!name.startsWith(prefix)||!name.endsWith(".png"))
                        throw new IOException("Unexpected Castle character entry: "+name);
                    String[] parts=name.substring(prefix.length()).split("/",-1);
                    if(parts.length!=2||!isCastleSupport(parts[0]))
                        throw new IOException("Invalid Castle resident: "+name);
                    String view=parts[1].substring(0,parts[1].length()-4);
                    if(!java.util.Arrays.asList(LORE_DIRECTIONS).contains(view))
                        throw new IOException("Invalid Castle resident view: "+name);
                    BufferedImage image=ImageIO.read(zip);
                    if(!validCastleSpriteSize(image))
                        throw new IOException("Invalid Castle sprite image: "+name);
                    if(frames.putIfAbsent(parts[0]+"/"+view,image)!=null)
                        throw new IOException("Duplicate Castle sprite: "+name);
                    count++;zip.closeEntry();
                }
            }
            if(count!=16)throw new IOException("Expected sixteen Castle sprite views; got "+count);
            for(String identity:SUPPORT_IDENTITIES)
                for(String direction:LORE_DIRECTIONS)
                    if(!frames.containsKey(identity+"/"+direction))
                        throw new IOException("Missing Castle sprite "+identity+"/"+direction);
        }catch(IOException ex){throw new IllegalStateException("Cannot load Castle support sprites",ex);}
        return Map.copyOf(frames);
    }
    public static int frameCount(){return IMAGES.size();}
    public static BufferedImage frame(String character,int facing){
        if(facing<0||facing>=8)throw new IllegalArgumentException("facing must be 0..7");
        if(GhostManifestationVisual.UNREVEALED.equals(character))
            return GhostManifestationVisual.frame(facing);
        String direction=("lore_master".equals(character)||isCastleSupport(character))?LORE_FACINGS[facing]:
            (CharacterVisuals.isHunter(character)||CharacterVisuals.isElder(character)
                ||CharacterVisuals.isTwinActor(character))?MASTER_FACINGS[facing]:FACINGS[facing];
        String identity=character!=null&&character.startsWith("ancient_twins_")
                ?character.substring("ancient_".length()):character;
        BufferedImage image=IMAGES.get(identity+"/"+direction);
        if(image==null)throw new IllegalArgumentException("Unknown character sprite: "+character);
        return image;
    }
    /** Expansion v2 supplies a different appearance for each original/Ancient ghost.
     * Legacy observer/banshee facings remain the graceful fallback when not packaged.
     */
    public static BufferedImage frame(String character,int facing,LanGhostAnimation.Phase phase,int animationFrame){
        if(facing<0||facing>=8||phase==null)throw new IllegalArgumentException("invalid facing/phase");
        if(GhostManifestationVisual.UNREVEALED.equals(character))
            return GhostManifestationVisual.frame(facing);
        // The full 23-identity atlas already includes ten separately green-tinted
        // Ancient appearances. Use their actual packaged art before considering a
        // source fallback; stripping ancient_ would incorrectly draw the base ghost.

        String spriteSource=character;
        if(CharacterVisuals.isTwinActor(spriteSource)||"lore_master".equals(spriteSource)||isCastleSupport(spriteSource))
            return frame(spriteSource,facing);
        // The present 8-state atlases contain only source-derived static cutouts.
        // Do not cycle pseudo-walk/attack frames until coherent animation is approved.
        // Restrict active runtime selection to four genuine source angles.
        int originalView=switch(facing) {
            case 0,1,7 -> 0;
            case 2,6 -> 2;
            case 3,5 -> 3;
            default -> 4;
        };
        BufferedImage expanded=GhostSpriteAtlas.frame(spriteSource,originalView,"idle",0);
        if(expanded!=null)return expanded;
        if(CharacterVisuals.isHunter(spriteSource)||CharacterVisuals.isElder(spriteSource)
                ||spriteSource.equals("banshee"))
            return frame(spriteSource,facing);
        throw new IllegalArgumentException("Missing distinct approved ghost sprite: "+character);
    }
    /** Entity heading zero looks +Z; facing zero when camera looks at its face. */
    public static int facing(int heading,double entityX,double entityZ,double camX,double camZ){
        if(heading<0||heading>=360||!Double.isFinite(entityX)||!Double.isFinite(entityZ)
                ||!Double.isFinite(camX)||!Double.isFinite(camZ))throw new IllegalArgumentException("Bad sprite pose");
        double cameraAngle=Math.toDegrees(Math.atan2(camX-entityX,camZ-entityZ));
        return Math.floorMod((int)Math.floor((cameraAngle-heading+22.5)/45.0),8);
    }
    /** Billboard is drawn after static geometry and tests against the same perspective depth buffer.
     * Rendering changes no collision, coordinates, authority, or ghost visibility decisions.
     */
    public static void draw(int[] pixels,double[] depth,double cameraX,double cameraZ,
            double yaw,double pitch,double entityX,double entityZ,int heading,
            String character,double height,double width,double elevation,int opacity,boolean lamp){
        draw(pixels,depth,cameraX,cameraZ,yaw,pitch,entityX,entityZ,heading,
            character,height,width,elevation,opacity,lamp,W,H,FOCAL);
    }
    public static void draw(int[] pixels,double[] depth,double cameraX,double cameraZ,
            double yaw,double pitch,double entityX,double entityZ,int heading,
            String character,double height,double width,double elevation,int opacity,boolean lamp,
            int renderWidth,int renderHeight,double focal){
        draw(pixels,depth,cameraX,cameraZ,yaw,pitch,entityX,entityZ,heading,
            character,height,width,elevation,opacity,lamp,renderWidth,renderHeight,focal,false);
    }
    /** Ancient forms reuse their parent's alpha-masked directional artwork.
     * Green tint is applied per rendered pixel, without copying sprite assets.
     */
    public static void draw(int[] pixels,double[] depth,double cameraX,double cameraZ,
            double yaw,double pitch,double entityX,double entityZ,int heading,
            String character,double height,double width,double elevation,int opacity,boolean lamp,
            int renderWidth,int renderHeight,double focal,boolean ancientTint){
        draw(pixels,depth,cameraX,cameraZ,yaw,pitch,entityX,entityZ,heading,
            character,height,width,elevation,opacity,lamp,renderWidth,renderHeight,focal,
            ancientTint,LanGhostAnimation.Phase.LURK,0);
    }
    /** Pose is selected only from a host-authorized visible apparition.
     * A v2 Ancient atlas is already green, so never apply the legacy tint twice.
     */
    public static void draw(int[] pixels,double[] depth,double cameraX,double cameraZ,
            double yaw,double pitch,double entityX,double entityZ,int heading,
            String character,double height,double width,double elevation,int opacity,boolean lamp,
            int renderWidth,int renderHeight,double focal,boolean ancientTint,
            LanGhostAnimation.Phase phase,int animationFrame){
        if(pixels.length!=renderWidth*renderHeight||depth.length!=renderWidth*renderHeight
                ||opacity<=0||height<=0)return;
        if(!Double.isFinite(entityX)||!Double.isFinite(entityZ))return;
        double dx=entityX-cameraX,dz=entityZ-cameraZ;
        double right=-dx*Math.cos(yaw)+dz*Math.sin(yaw);
        double ahead=dx*Math.sin(yaw)+dz*Math.cos(yaw);
        if(ahead<NEAR||ahead>24)return;
        double cy=Math.cos(pitch),sy=Math.sin(pitch);
        double baseY=elevation-1.62,topY=elevation+height-1.62;
        double nearBottom=baseY*sy+ahead*cy,nearTop=topY*sy+ahead*cy;
        if(nearBottom<NEAR||nearTop<NEAR)return;
        double foot=renderHeight/2.0-(baseY*cy-ahead*sy)/nearBottom*focal;
        double head=renderHeight/2.0-(topY*cy-ahead*sy)/nearTop*focal;
        if(head>=foot)return;
        BufferedImage sprite=frame(character,facing(heading,entityX,entityZ,cameraX,cameraZ),phase,animationFrame);
        // Grounded people have direction-dependent transparent bottom padding.
        // Move the image sampling rectangle (not its source pixels) so the last
        // visible shoe row stays at the actual world-floor contact projection.
        // Supernatural bodies retain their authored hover/manifestation effects.
        if(GroundedSpriteFeet.isGroundedHuman(character)){
            double shift=GroundedSpriteFeet.bottomPaddingShift(sprite,foot-head);
            foot+=shift;
            head+=shift;
        }
        double anchorDepth=(nearBottom+nearTop)/2;
        double cx=renderWidth/2.0+right/anchorDepth*focal;
        double halfW=focal*width/(2*anchorDepth);
        int x0=Math.max(0,(int)Math.floor(cx-halfW)),x1=Math.min(renderWidth-1,(int)Math.ceil(cx+halfW));
        int y0=Math.max(0,(int)Math.floor(head)),y1=Math.min(renderHeight-1,(int)Math.ceil(foot));
        if(x0>x1||y0>y1)return;
        int sw=sprite.getWidth(),sh=sprite.getHeight();
        double screenWidth=halfW*2,screenHeight=foot-head;
        double fog=Math.min(.75,Math.max(0,(anchorDepth-5)/15));
        double illumination=(lamp?1.22:.55)*Math.max(.22,1-anchorDepth/26.0);
        for(int py=y0;py<=y1;py++){
            int v=(int)((py+.5-head)/screenHeight*sh);
            if(v<0||v>=sh)continue;
            // A pitched camera gives the head and feet different view-space depths.
            // Testing the entire character at its midpoint erroneously buries the
            // feet behind the floor. Interpolate the billboard's projected depth.
            double verticalFraction=Math.max(0,Math.min(1,(py+.5-head)/screenHeight));
            double rowDepth=nearTop+(nearBottom-nearTop)*verticalFraction;
            for(int px=x0;px<=x1;px++){
                int u=(int)((px+.5-(cx-halfW))/screenWidth*sw);
                if(u<0||u>=sw)continue;
                int argb=sprite.getRGB(u,v),alpha=(argb>>>24)*Math.min(255,opacity)/255;
                if(alpha<24)continue;
                int k=py*renderWidth+px;
                if(rowDepth>=depth[k])continue;
                int red=(int)(((argb>>>16)&255)*illumination),
                    green=(int)(((argb>>>8)&255)*illumination),
                    blue=(int)((argb&255)*illumination);
                red=(int)(Math.min(255,red)*(1-fog)+18*fog);
                green=(int)(Math.min(255,green)*(1-fog)+21*fog);
                blue=(int)(Math.min(255,blue)*(1-fog)+30*fog);
                // Ancients already have their approved green variant stored in v3.
                // Double tint is permitted only for the explicitly legacy base fallback.
                if(ancientTint&&!GhostSpriteAtlas.supports(character)){
                    red=(int)(red*.33);
                    green=Math.min(255,(int)(green*.80+53));
                    blue=(int)(blue*.35);
                }
                int bg=pixels[k];
                if(alpha<255){
                    red=(red*alpha+((bg>>>16)&255)*(255-alpha))/255;
                    green=(green*alpha+((bg>>>8)&255)*(255-alpha))/255;
                    blue=(blue*alpha+(bg&255)*(255-alpha))/255;
                }
                pixels[k]=(red<<16)|(green<<8)|blue;
                if(alpha>=220)depth[k]=rowDepth;
            }
        }
    }
}