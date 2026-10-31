package org.beckenburg;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Advisory art review: real-world SceneRenderer base + exactly the live
 * GhostHauntingFolio post-effect at the authentic event midpoint. Captures
 * original artwork, not mocked gradients or alternative renderer output.
 * Screenshot quality is never a build/release gate.
 */
public final class HauntingVisualReview {
    private HauntingVisualReview(){}
    private static final String[] SHOTS={
        "corridor_exhale","passing_shadow","distant_presence",
        "floor_chill","hushed_warning","hollow_voice",
        "lamp_convulsion","radio_crawl","bent_perspective",
        "doorframe_figure","shrouded_witness","flicker_walker",
        "frozen_silhouette","blindspot_watcher","pressure_wave",
        "grasping_darkness","paired_revelation","executioners_coming",
        "plague_procession","unblinking_judgment"
    };
    private static BufferedImage copy(int[] pixels,int w,int h){
        var out=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
        out.setRGB(0,0,w,h,pixels,0,w);
        return out;
    }
    public static void main(String[] args)throws Exception{
        Path directory=Path.of(args.length>0?args[0]:"build/haunting-visual-review");
        Files.createDirectories(directory);
        HouseMap world=new HouseMap(HouseMap.Site.HOUSE);
        SceneRenderer render=new SceneRenderer();
        render.setRenderSize(640,360);
        render.roomLights=new RoomLighting();
        render.lamp=true;
        var spawn=world.spawn(4);
        render.x=spawn.x();render.z=spawn.z();render.eyeHeight=spawn.y()+1.62;
        render.yaw=0.37;render.pitch=-0.075;
        render.setPsxPreset(PsxPipeline.Preset.CLASSIC);
        render.setHauntings(List.of());
        render.drawNetwork(world,List.of(),-1,12.0);
        int width=render.width(),height=render.height();
        int[] baseline=render.pixels().clone();
        ImageIO.write(copy(baseline,width,height),"png",
            directory.resolve("00-clean-scene.png").toFile());

        List<BufferedImage> thumbnails=new ArrayList<>();
        var manifest=new StringBuilder("event_id,tier,visual,motif,frame,room,changed_pixels,file\n");
        for(int i=0;i<SHOTS.length;i++){
            var entry=GhostHauntingFolio.entry(SHOTS[i]);
            if(entry==null)throw new IllegalStateException("Unknown authored scene "+SHOTS[i]);
            int tick=100+entry.duration()/2;
            var event=new GhostHauntingFolio.Occurrence(entry,4,100);
            int[] effect=baseline.clone();
            GhostHauntingFolio.paint(effect,width,height,List.of(event),4,tick);
            int changed=0;
            for(int p=0;p<effect.length;p++)if(effect[p]!=baseline[p])changed++;
            String filename=String.format("%02d-%s.png",i+1,entry.id());
            ImageIO.write(copy(effect,width,height),"png",directory.resolve(filename).toFile());
            var score=GhostHauntingChoreography.forEvent(entry.id());
            manifest.append(entry.id()).append(',').append(entry.tier())
                .append(',').append(entry.visual()).append(',')
                .append(score==null?"ORGANIC_GENERIC":score.motif())
                .append(',').append(tick).append(",4,").append(changed)
                .append(',').append(filename).append('\n');
            BufferedImage thumb=new BufferedImage(320,207,BufferedImage.TYPE_INT_RGB);
            Graphics2D g=thumb.createGraphics();
            try{
                g.setColor(new Color(8,11,17));g.fillRect(0,0,320,207);
                g.drawImage(copy(effect,width,height),0,0,320,180,null);
                g.setColor(new Color(225,231,237));
                g.setFont(new Font(Font.MONOSPACED,Font.BOLD,12));
                g.drawString(entry.id().replace('_',' '),7,197);
            }finally{g.dispose();}
            thumbnails.add(thumb);
            System.out.println("HAUNTING_VISUAL_SCREENSHOT "+filename+" changed="+changed);
        }
        int columns=4,rows=(thumbnails.size()+columns-1)/columns;
        BufferedImage sheet=new BufferedImage(columns*320,rows*207,
            BufferedImage.TYPE_INT_RGB);
        Graphics2D cg=sheet.createGraphics();
        try{
            cg.setColor(new Color(10,12,18));
            cg.fillRect(0,0,sheet.getWidth(),sheet.getHeight());
            for(int i=0;i<thumbnails.size();i++)
                cg.drawImage(thumbnails.get(i),(i%columns)*320,(i/columns)*207,null);
        }finally{cg.dispose();}
        ImageIO.write(sheet,"png",directory.resolve("haunting-contact-sheet.png").toFile());
        Files.writeString(directory.resolve("manifest.csv"),manifest);
        System.out.println("DIAGNOSTIC "+thumbnails.size()
            +" real-engine-baseline PSX haunting captures at "+width+"x"+height
            +" / review-only, must not block builds on aesthetics");
    }
}
