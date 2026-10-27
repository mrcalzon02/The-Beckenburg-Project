package org.beckenburg;

import java.awt.*;
import java.awt.geom.Path2D;
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
        Path source=root.resolve("development/levels/previews/lighting-camera-audit/castle-lit.png");
        if(!Files.isRegularFile(source))throw new IOException("Real Castle splash source missing: "+source);
        BufferedImage actualCastle=ImageIO.read(source.toFile());
        if(actualCastle==null)throw new IOException("Unreadable Castle screenshot: "+source);
        Path brand=output.resolve("ui/branding");
        Files.createDirectories(brand);
        ImageIO.write(splash(actualCastle),"png",brand.resolve("castle-splash.png").toFile());
        BufferedImage mark=mark(256);
        ImageIO.write(mark,"png",brand.resolve("beckenburg.png").toFile());
        Path installer=Path.of(args.length<3?"build/Beckenburg.ico":args[2]);
        Files.createDirectories(installer.toAbsolutePath().getParent());
        writeIco(installer,mark);
        System.out.println("Baked "+count+" approved model-render UI icons, Castle splash and native desktop icon");
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

    /** Beckenburg B over a restrained iron shield, suitable for small Windows shortcut sizes. */
    private static BufferedImage mark(int size){
        BufferedImage image=new BufferedImage(size,size,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(15,20,27));
        g.fillRoundRect(3,3,size-6,size-6,size/7,size/7);
        Path2D shield=new Path2D.Double();
        shield.moveTo(size*.19,size*.13);shield.lineTo(size*.81,size*.13);
        shield.lineTo(size*.79,size*.69);shield.quadTo(size*.67,size*.87,size*.50,size*.94);
        shield.quadTo(size*.33,size*.87,size*.21,size*.69);shield.closePath();
        g.setColor(new Color(37,46,55));g.fill(shield);
        g.setStroke(new BasicStroke(size*.023f));
        g.setColor(new Color(166,143,106));g.draw(shield);
        g.setFont(new Font(Font.SERIF,Font.BOLD,(int)(size*.64)));
        g.setColor(new Color(219,207,177));
        FontMetrics m=g.getFontMetrics();String label="B";
        g.drawString(label,(size-m.stringWidth(label))/2,(int)(size*.72));
        g.dispose();return image;
    }
    /** Modern Windows ICO: embed a lossless 256px PNG without third-party dependencies. */
    private static void writeIco(Path file,BufferedImage source) throws IOException {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        ImageIO.write(source,"png",bytes);
        byte[] png=bytes.toByteArray();
        try(var output=Files.newOutputStream(file)){
            output.write(new byte[]{0,0,1,0,1,0,0,0,0,0,1,0,32,0});
            le32(output,png.length);
            le32(output,22);
            output.write(png);
        }
    }
    private static void le32(java.io.OutputStream out,int value)throws IOException{
        for(int shift=0;shift<32;shift+=8)out.write((value>>>shift)&255);
    }
}
