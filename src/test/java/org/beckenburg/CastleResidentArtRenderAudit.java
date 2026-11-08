package org.beckenburg;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import javax.imageio.ImageIO;

/**
 * Actual Castle character render acceptance. Captures production SceneRenderer
 * images and observes character pixels DIRECTLY where the grounded sprite
 * renderer submits them. Subtracting two world frames is invalid because
 * scenery, GPU readbacks and cached lighting can change independently.
 */
public final class CastleResidentArtRenderAudit {
    private CastleResidentArtRenderAudit(){}
    private static final double[][] CAMERAS={
        {0,2.4},{-2.4,0},{2.4,0},{0,-2.4},
        {1.8,1.8},{-1.8,1.8},{1.8,-1.8},{-1.8,-1.8},
        {0,2.9},{-2.9,0},{2.9,0},{0,-2.9}
    };
    private static final int WIDTH=854,HEIGHT=480;
    private static final double FOCAL=153.0*HEIGHT/200.0;
    private static final double PITCH=-.20;
    private static String slug(String name){
        return name.toLowerCase(Locale.ROOT).replace(' ','-');
    }
    private static void require(boolean ok,String why){
        if(!ok)throw new AssertionError(why);
    }
    private record Contact(int body,int nearSole,int hiddenSole,int lastPixel,
                           int floorPixel,double minGapMetres,int gapPixels,
                           int belowFloor,boolean occluded){
        boolean pass(){
            return !occluded&&body>=80&&nearSole>=2
                &&minGapMetres<=.05&&belowFloor==0;
        }
        String verdict(){return occluded?"OCCLUDED":pass()?"PASS":"FAIL";}
    }
    private static Contact measure(SceneRenderer scene,CastleResidentAnimation.Body body,
                                   DirectionalSprites.HumanRenderTrace trace){
        double dx=body.x()-scene.x,dz=body.z()-scene.z;
        double ahead=dx*Math.sin(scene.yaw)+dz*Math.cos(scene.yaw);
        double floor=-scene.eyeHeight,cy=Math.cos(scene.pitch),sy=Math.sin(scene.pitch);
        double z=floor*sy+ahead*cy,y=floor*cy-ahead*sy;
        int floorPixel=z>0?(int)Math.round(HEIGHT*.5-y/z*FOCAL):-1;
        double gap=trace.closestSole;
        int gapPx=Double.isFinite(gap)&&z>0
            ?(int)Math.round(gap*FOCAL/z):999;
        // A real foreground object can occlude the boots. Such a view is
        // INCONCLUSIVE, not a pass and not a floating-actor conviction.
        boolean obstructed=trace.visibleSole<2&&trace.hiddenSole>3;
        return new Contact(trace.visibleBody,trace.visibleSole,trace.hiddenSole,
            trace.bottomVisible,floorPixel,gap,gapPx,trace.belowFloor,obstructed);
    }
    private static BufferedImage crop(BufferedImage source,int floorRow){
        int left=WIDTH/2-110,top=Math.max(0,Math.min(HEIGHT-170,floorRow-125));
        BufferedImage boot=new BufferedImage(660,510,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=boot.createGraphics();
        try{
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(source,0,0,660,510,left,top,left+220,top+170,null);
        }finally{g.dispose();}
        return boot;
    }
    /** Magenta is exactly the actor's accepted pixels, recorded inside the
     * production sprite compositor. No floor, furniture or lamps are selected
     * by comparing unrelated render passes. Cyan is the ideal foot projection. */
    private static BufferedImage mask(BufferedImage frame,
                                    DirectionalSprites.HumanRenderTrace trace,
                                    int floorRow){
        BufferedImage image=new BufferedImage(WIDTH,HEIGHT,BufferedImage.TYPE_INT_RGB);
        for(int y=0;y<HEIGHT;y++)for(int x=0;x<WIDTH;x++){
            int p=y*WIDTH+x,c=frame.getRGB(x,y);
            int gray=(((c>>16)&255)+((c>>8)&255)+(c&255))/12;
            image.setRGB(x,y,trace.pixels[p]!=0?0xff00ff:(gray<<16)|(gray<<8)|gray);
        }
        if(floorRow>=0&&floorRow<HEIGHT)
            for(int x=WIDTH/2-85;x<=WIDTH/2+85;x++)image.setRGB(x,floorRow,0x00ffff);
        return image;
    }
    public static void main(String[] args)throws Exception{
        Path output=Path.of(args.length==0?"build/castle-resident-art-render":args[0]);
        Files.createDirectories(output);
        CastleHubMap map=new CastleHubMap();
        SceneRenderer scene=new SceneRenderer();
        scene.setRenderSize(WIDTH,HEIGHT);
        scene.setPsxPreset(PsxPipeline.Preset.OFF);
        scene.photographicSoftwareRaster();
        scene.lamp=true;scene.eyeHeight=1.62;
        int images=0,missing=0,grounded=0,failed=0,occluded=0;
        StringBuilder manifest=new StringBuilder(
            "resident,angle,preset,camera_x,camera_z,render_width,render_height,file\n");
        StringBuilder measurements=new StringBuilder(
            "resident,sha,angle,body_pixels,near_sole_pixels,hidden_sole_pixels,sole_gap_metres,sole_gap_screen_px,below_floor_pixels,obstructed,verdict,source_png,mask_png,crop_png\n");
        BufferedImage sheet=new BufferedImage(660*5,546,BufferedImage.TYPE_INT_RGB);
        Graphics2D overview=sheet.createGraphics();
        try{
            overview.setColor(new Color(23,27,31));
            overview.fillRect(0,0,sheet.getWidth(),sheet.getHeight());
            overview.setColor(Color.WHITE);
            overview.setFont(new Font(Font.MONOSPACED,Font.BOLD,18));
            int index=0;
            for(CastleRelations.Resident resident:CastleRelations.Resident.values()){
                var room=CastleRooms.at(resident.x,0,resident.z);
                int views=0,clear=0,pPassed=0,pFailed=0,pOccluded=0;
                for(double[] offset:CAMERAS){
                    double cx=resident.x+offset[0],cz=resident.z+offset[1];
                    var at=CastleRooms.at(cx,0,cz);
                    if(!map.traversable(cx,cz)||at==null||room==null||!room.id().equals(at.id()))
                        continue;
                    scene.x=cx;scene.z=cz;
                    scene.yaw=Math.atan2(resident.x-cx,resident.z-cz);
                    scene.pitch=PITCH;
                    scene.setPsxPreset(PsxPipeline.Preset.OFF);
                    DirectionalSprites.beginHumanRenderTrace(
                        CastleResidentAnimation.identity(resident),WIDTH,HEIGHT);
                    DirectionalSprites.HumanRenderTrace trace;
                    try{
                        scene.drawCastle(map,.5,true);
                    }finally{
                        trace=DirectionalSprites.endHumanRenderTrace();
                    }
                    if(scene.castleResidentNameplate(resident)==null)continue;
                    var body=map.residents().body(resident);
                    Contact evidence=measure(scene,body,trace);
                    String tag=slug(resident.name)+"-angle-"+(views+1);
                    String raw=tag+"-off.png",traceName=tag+"-actor-only-mask.png",
                        bootsName=tag+"-boots-3x.png";
                    BufferedImage real=scene.image(),enlarged=crop(real,evidence.floorPixel());
                    require(ImageIO.write(real,"png",output.resolve(raw).toFile()),"full game PNG write");
                    require(ImageIO.write(enlarged,"png",output.resolve(bootsName).toFile()),"boot crop write");
                    require(ImageIO.write(mask(real,trace,evidence.floorPixel()),"png",
                        output.resolve(traceName).toFile()),"actual actor-only depth PNG write");
                    if(views==0||(!evidence.pass()&&!evidence.occluded()&&pFailed==0)){
                        overview.drawImage(enlarged,index*660,36,null);
                        overview.drawString(resident.name+" "+evidence.verdict()+" "+
                            evidence.gapPixels()+"px",index*660+12,25);
                    }
                    String sha=System.getenv().getOrDefault("GITHUB_SHA","local");
                    measurements.append(slug(resident.name)).append(',').append(sha)
                        .append(',').append(views+1).append(',').append(evidence.body())
                        .append(',').append(evidence.nearSole())
                        .append(',').append(evidence.hiddenSole())
                        .append(',').append(evidence.minGapMetres())
                        .append(',').append(evidence.gapPixels())
                        .append(',').append(evidence.belowFloor())
                        .append(',').append(evidence.occluded())
                        .append(',').append(evidence.verdict())
                        .append(',').append(raw).append(',').append(traceName)
                        .append(',').append(bootsName).append('\n');
                    manifest.append(slug(resident.name)).append(',').append(views+1)
                        .append(",OFF,").append(cx).append(',').append(cz).append(',')
                        .append(WIDTH).append(',').append(HEIGHT).append(',')
                        .append(raw).append('\n');
                    images++;
                    // Preserve the same real scene from the second pipeline too.
                    scene.setPsxPreset(PsxPipeline.Preset.CLASSIC);
                    scene.drawCastle(map,.5,true);
                    String classic=tag+"-classic.png";
                    require(ImageIO.write(scene.image(),"png",output.resolve(classic).toFile()),
                        "classic screenshot write");
                    manifest.append(slug(resident.name)).append(',').append(views+1)
                        .append(",CLASSIC,").append(cx).append(',').append(cz)
                        .append(',').append(WIDTH).append(',').append(HEIGHT)
                        .append(',').append(classic).append('\n');
                    images++;
                    if(evidence.occluded()){pOccluded++;occluded++;}
                    else{
                        clear++;
                        if(evidence.pass())pPassed++;
                        else{pFailed++;failed++;}
                    }
                    System.out.println("CASTLE_REAL_ACTOR_CONTACT "+resident.name+
                        " angle="+(views+1)+" visible="+evidence.body()+
                        " sole="+evidence.nearSole()+" blocked_sole="+evidence.hiddenSole()+
                        " gap_m="+evidence.minGapMetres()+" below_floor="+evidence.belowFloor()+
                        " verdict="+evidence.verdict()+" mask="+traceName);
                    views++;
                    if(clear>=4)break;
                }
                if(views==0){missing++;System.out.println("CASTLE_MISSING_RESIDENT "+resident.name);}
                if(pPassed>=2&&pFailed==0)grounded++;
                else System.out.println("CASTLE_ACTOR_NOT_GROUNDED "+resident.name+
                    " passed="+pPassed+" failed="+pFailed+" occluded="+pOccluded);
                index++;
            }
        }finally{overview.dispose();}
        require(ImageIO.write(sheet,"png",
            output.resolve("actual-game-five-residents-boots-contact-sheet.png").toFile()),
            "contact sheet write");
        Files.writeString(output.resolve("captures.csv"),manifest.toString(),StandardCharsets.UTF_8);
        Files.writeString(output.resolve("boot-contact-measurements.csv"),
            measurements.toString(),StandardCharsets.UTF_8);
        System.out.println("CASTLE_ACTOR_ONLY_VISUAL_DIAGNOSTIC images="+images+
            " grounded="+grounded+"/5 failed="+failed+" occluded="+occluded+
            " no_cameras="+missing);
        require(missing==0&&grounded==5&&failed==0,
            "In-game NPC grounding unresolved: "+grounded+
            "/5 accepted, "+failed+" genuine actor-only failures, "+
            occluded+" view occlusions; inspect real screenshot and actor-only masks");
    }
}
