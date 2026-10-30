package org.beckenburg;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.util.Comparator;
import javax.imageio.ImageIO;

/**
 * Bake UI thumbnails from approved model-review screenshots, never stand-in drawings.
 * All images are optional except those representing actually purchasable goods.
 * This runs during packaging, not on the game render thread or during menu startup.
 */
public final class UiAssetBake {
    private UiAssetBake() {}
    private static final String[] REQUIRED={
        "uv_light","salt","health_supplies","sanity_medication",
        "emf_reader","thermometer","spirit_box","photo_camera"
    };

    public static void main(String[] args) throws Exception {
        Path root=Path.of(args.length==0?".":args[0]);
        Path output=Path.of(args.length<2?"build/classes/org/beckenburg":args[1]);
        Path previews=root.resolve("development/assets/equipment-catalog/previews");
        Path icons=output.resolve("ui/item-icons");
        Files.createDirectories(icons);
        for(String id:REQUIRED)
            if(!Files.isRegularFile(previews.resolve(id+".png")))
                throw new IOException("Approved store source preview missing: "+id);
        int count=0;
        try(var entries=Files.list(previews)){
            for(Path file:entries.filter(p->p.toString().endsWith(".png"))
                    .sorted(Comparator.comparing(Path::toString)).toList()){
                BufferedImage source=ImageIO.read(file.toFile());
                if(source==null)throw new IOException("Unreadable preview: "+file);
                BufferedImage icon=thumbnail(source,96);
                if(!ImageIO.write(icon,"png",icons.resolve(file.getFileName()).toFile()))
                    throw new IOException("PNG image encoder unavailable");
                count++;
            }
        }
        // Keep the singular Blender renders at full resolution for the large item preview.
        Path tierIcons=root.resolve("assets/runtime/ui/item-tier-icons");
        for(var tool:EquipmentRegistry.all())for(var tier:EquipmentTiers.Tier.values()){
            Path item=tierIcons.resolve(ItemIconAtlas.key(tool)+"_t"+(tier.ordinal()+1)+".png");
            if(!Files.isRegularFile(item))throw new IOException("Missing individual tier icon: "+item);
            Files.copy(item,icons.resolve(item.getFileName()),StandardCopyOption.REPLACE_EXISTING);count++;
        }
        for(int tier=1;tier<=3;tier++){
            Path item=tierIcons.resolve("health_supplies_t"+tier+".png");
            if(!Files.isRegularFile(item))throw new IOException("Missing medical tier icon: "+item);
            Files.copy(item,icons.resolve(item.getFileName()),StandardCopyOption.REPLACE_EXISTING);count++;
        }
        Path source=root.resolve("development/levels/previews/lighting-camera-audit/castle-lit.png");
        if(!Files.isRegularFile(source))throw new IOException("Real Castle splash source missing: "+source);
        BufferedImage actualCastle=ImageIO.read(source.toFile());
        if(actualCastle==null)throw new IOException("Unreadable Castle screenshot: "+source);
        Path brand=output.resolve("ui/branding");
        Files.createDirectories(brand);
        ImageIO.write(splash(actualCastle),"png",brand.resolve("castle-splash.png").toFile());
        // The multiplayer lobby needs the REAL original Hunter character art,
        // not the 128x224 GrabCut directional sprite that cuts away dark limbs.
        // Package intact, original-resolution source-sheet FRONT panels as UI
        // resources. No segmentation, painting, re-lighting or upscaling.
        bakeOriginalHunterLobbyPortraits(root,output);
        // Preserve the actual approved Beckenburg desktop crest, not a procedural stand-in.
        Path desktopSource=root.resolve("assets/runtime/ui/branding/beckenburg.png");
        if(!Files.isRegularFile(desktopSource))
            throw new IOException("Required Beckenburg desktop icon source missing: "+desktopSource);
        BufferedImage mark=ImageIO.read(desktopSource.toFile());
        if(mark==null || mark.getWidth()!=512 || mark.getHeight()!=512)
            throw new IOException("Invalid 512x512 Beckenburg desktop icon: "+desktopSource);
        ImageIO.write(mark,"png",brand.resolve("beckenburg.png").toFile());
        Path installer=Path.of(args.length<3?"build/Beckenburg.ico":args[2]);
        Files.createDirectories(installer.toAbsolutePath().getParent());
        writeIco(installer,mark);
        System.out.println("Baked "+count+" approved model-render UI icons, Castle splash and native desktop icon");
    }

    /**
     * The approved 1448x1086 original concept sheets are the only source for
     * lobby portraits. Rectangles correspond to their authored full-body
     * FRONT view, precisely as recorded in character-directions-manifest.
     * This keeps the original pixels and matte -- including dark fingers,
     * hair, coat edges and boots discarded by the world sprite GrabCut.
     * DirectionalSprites remain separate and unchanged for world rendering.
     */
    static void bakeOriginalHunterLobbyPortraits(Path root,Path output) throws IOException {
        String[][] source={
            {"cleric","24","300","218","727"},
            {"observer","24","294","206","685"},
            {"demonologist","23","291","208","690"}
        };
        Path dir=output.resolve("lobby-hunters");
        Files.createDirectories(dir);
        for(String[] spec:source){
            String name=spec[0];
            Path file=root.resolve("assets/concept-art/v001/01-hunters/"+name+"-character-profile.png");
            if(!Files.isRegularFile(file))
                throw new IOException("Missing actual original Hunter portrait source: "+file);
            BufferedImage master=ImageIO.read(file.toFile());
            if(master==null||master.getWidth()!=1448||master.getHeight()!=1086)
                throw new IOException("Invalid original Hunter concept sheet: "+file);
            int x0=Integer.parseInt(spec[1]),y0=Integer.parseInt(spec[2]),
                x1=Integer.parseInt(spec[3]),y1=Integer.parseInt(spec[4]);
            if(x0<0||y0<0||x1>master.getWidth()||y1>master.getHeight())
                throw new IOException("Original character panel bounds outside original: "+name);
            BufferedImage panel=master.getSubimage(x0,y0,x1-x0,y1-y0);
            Path destination=dir.resolve(name+".png");
            if(!ImageIO.write(panel,"png",destination.toFile()))
                throw new IOException("Unable to encode intact approved Hunter portrait "+name);
            // No rescale: re-read and verify exact per-pixel preservation.
            BufferedImage actual=ImageIO.read(destination.toFile());
            if(actual==null||actual.getWidth()!=panel.getWidth()
                    ||actual.getHeight()!=panel.getHeight())
                throw new IOException("Hunter source copy damaged "+name);
            for(int y=0;y<panel.getHeight();y++)for(int x=0;x<panel.getWidth();x++)
                if(actual.getRGB(x,y)!=panel.getRGB(x,y))
                    throw new IOException("Hunter original source pixel altered: "+name);
        }
    }

    private static BufferedImage thumbnail(BufferedImage source,int size){
        // Model review images are photographed against dark studio backgrounds;
        // retain the entire angled object and its genuine pixels (no guessed silhouettes).
        BufferedImage icon=new BufferedImage(size,size,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=icon.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setColor(new Color(21,25,31));g.fillRect(0,0,size,size);
        double scale=Math.min((size-4.0)/source.getWidth(),(size-4.0)/source.getHeight());
        int w=Math.max(1,(int)Math.round(source.getWidth()*scale));
        int h=Math.max(1,(int)Math.round(source.getHeight()*scale));
        g.drawImage(source,(size-w)/2,(size-h)/2,w,h,null);
        g.dispose();
        return icon;
    }

    private static BufferedImage splash(BufferedImage image){
        final int w=960,h=480;
        BufferedImage out=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        double scale=Math.max((double)w/image.getWidth(),(double)h/image.getHeight());
        int sw=(int)Math.ceil(image.getWidth()*scale),sh=(int)Math.ceil(image.getHeight()*scale);
        g.drawImage(image,(w-sw)/2,(h-sh)/2,sw,sh,null);
        g.setPaint(new GradientPaint(0,0,new Color(7,11,16,185),w,0,new Color(7,11,16,74)));
        g.fillRect(0,0,w,h);
        g.dispose();
        return out;
    }

    /** Windows ICO with genuine PNG frames at normal desktop/taskbar resolutions. */
    private static void writeIco(Path file,BufferedImage source) throws IOException {
        int[] sizes={16,24,32,48,64,128,256};
        byte[][] frames=new byte[sizes.length][];
        for(int i=0;i<sizes.length;i++){
            int size=sizes[i];
            BufferedImage scaled=new BufferedImage(size,size,BufferedImage.TYPE_INT_ARGB);
            Graphics2D g=scaled.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(source,0,0,size,size,null);
            g.dispose();
            ByteArrayOutputStream buffer=new ByteArrayOutputStream();
            if(!ImageIO.write(scaled,"png",buffer))throw new IOException("PNG encoder unavailable");
            frames[i]=buffer.toByteArray();
        }
        try(var out=Files.newOutputStream(file)){
            le16(out,0); // reserved
            le16(out,1); // icon
            le16(out,frames.length);
            int offset=6+16*frames.length;
            for(int i=0;i<frames.length;i++){
                int size=sizes[i];
                out.write(size==256?0:size);
                out.write(size==256?0:size);
                out.write(0);out.write(0); // palette and reserved
                le16(out,1);le16(out,32);
                le32(out,frames[i].length);
                le32(out,offset);
                offset+=frames[i].length;
            }
            for(byte[] frame:frames)out.write(frame);
        }
    }
    private static void le16(java.io.OutputStream out,int value)throws IOException{
        out.write(value&255);out.write((value>>>8)&255);
    }
    private static void le32(java.io.OutputStream out,int value)throws IOException{
        for(int shift=0;shift<32;shift+=8)out.write((value>>>shift)&255);
    }

}
