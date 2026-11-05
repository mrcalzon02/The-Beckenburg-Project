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
 * In-engine Castle character visibility and boot-floor contact acceptance.
 * Draws the actual full Castle and shipped sprite artwork. The control frame
 * renders the same game geometry without residents to distinguish actor depth
 * writes from the floor, shadows, other props and sky. This is visual QA, not
 * a reason to block playable builds/release packaging.
 */
public final class CastleResidentArtRenderAudit {
    private CastleResidentArtRenderAudit() {}
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
    private record FootEvidence(int opaque,int shoePixels,int bottomRow,
            int floorRow,int gap,boolean blockedByScene){
        boolean pass(){return !blockedByScene&&opaque>=80&&shoePixels>=2&&gap>=-16&&gap<=16;}
        String verdict(){return blockedByScene?"OCCLUDED":pass()?"PASS":"FAIL";}
    }
    private static FootEvidence footContact(SceneRenderer scene,
            double[] groundDepth,int[] groundPixels,CastleRelations.Resident resident){
        double dist=Math.hypot(resident.x-scene.x,resident.z-scene.z);
        double floor=-scene.eyeHeight,cy=Math.cos(scene.pitch),sy=Math.sin(scene.pitch);
        double z=floor*sy+dist*cy,y=floor*cy-dist*sy;
        int projected=(int)Math.round(HEIGHT*.5-y/z*FOCAL);
        int[] actor=scene.pixels();
        double[] actorDepth=scene.depths();
        int opaque=0,shoe=0,bottom=-1;
        for(int py=Math.max(0,projected-145);py<=Math.min(HEIGHT-1,projected+22);py++)
            for(int px=Math.max(0,WIDTH/2-65);px<=Math.min(WIDTH-1,WIDTH/2+65);px++){
                int index=py*WIDTH+px;
                if(Double.isFinite(actorDepth[index])
                        &&actorDepth[index]+.001<groundDepth[index]
                        &&actor[index]!=groundPixels[index]){
                    opaque++;
                    bottom=Math.max(bottom,py);
                    if(Math.abs(py-projected)<=14)shoe++;
                }
            }
        // A high table or another actual scene object can cover the soles.
        // Do not mistake "no visible boot pixels" behind furniture for a
        // character hovering, nor silently call that camera a successful test.
        int blocked=0,receivers=0;
        if(projected>=0&&projected<HEIGHT)for(int px=WIDTH/2-25;px<=WIDTH/2+25;px+=10){
            double receiver=groundDepth[projected*WIDTH+px];
            if(Double.isFinite(receiver)){
                receivers++;
                if(receiver<z-.10)blocked++;
            }
        }
        return new FootEvidence(opaque,shoe,bottom,projected,
            bottom<0?999:projected-bottom,receivers==0||blocked>=3);
    }
    private static BufferedImage bootCrop(BufferedImage frame,int floorRow){
        int left=WIDTH/2-110;
        int top=Math.max(0,Math.min(HEIGHT-170,floorRow-125));
        BufferedImage crop=new BufferedImage(660,510,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=crop.createGraphics();
        try{
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(frame,0,0,660,510,left,top,left+220,top+170,null);
        }finally{g.dispose();}
        return crop;
    }
    /** Diagnostic overlay of ONLY the actor's additional depth writes.
     * Pink = an opaque resident fragment that was not present in the
     * otherwise identical Castle/control frame. Cyan = ideal world floor
     * contact projection. This is evidence, never a simulated picture. */
    private static BufferedImage actorDepthMask(SceneRenderer scene,
            double[] baselineDepth,int[] baselinePixels,int floorRow){
        BufferedImage out=new BufferedImage(WIDTH,HEIGHT,BufferedImage.TYPE_INT_RGB);
        int[] currentPixels=scene.pixels();double[] currentDepth=scene.depths();
        for(int y=0;y<HEIGHT;y++)for(int x=0;x<WIDTH;x++){
            int i=y*WIDTH+x;
            boolean actorPixel=Double.isFinite(currentDepth[i])
                &&currentDepth[i]+.001<baselineDepth[i]
                &&currentPixels[i]!=baselinePixels[i];
            int c=currentPixels[i];
            int grey=(((c>>>16)&255)+((c>>>8)&255)+(c&255))/3;
            int dark=grey/4;
            out.setRGB(x,y,actorPixel?0xff00ff:(dark<<16)|(dark<<8)|dark);
        }
        if(floorRow>=0&&floorRow<HEIGHT)
            for(int x=WIDTH/2-85;x<=WIDTH/2+85;x++)
                out.setRGB(x,floorRow,0x00ffff);
        return out;
    }
    /** A character-free depth buffer must settle before it can serve as the
     * reference for actor-only pixels. Highlight transient world geometry
     * as an audit failure, not as a phantom giant character card. */
    private static int depthDrift(double[] previous,double[] next){
        int changed=0;
        for(int i=0;i<previous.length;i++){
            double a=previous[i],b=next[i];
            if((Double.isInfinite(a)&&Double.isInfinite(b))||
                    (Double.isFinite(a)&&Double.isFinite(b)&&Math.abs(a-b)<.005))continue;
            changed++;
        }
        return changed;
    }
    private static void require(boolean test,String message){
        if(!test)throw new AssertionError(message);
    }
    public static void main(String[] args)throws Exception{
        Path output=Path.of(args.length==0?
            "build/castle-resident-art-render":args[0]);
        Files.createDirectories(output);
        CastleHubMap map=new CastleHubMap();
        SceneRenderer scene=new SceneRenderer();
        scene.setRenderSize(WIDTH,HEIGHT);
        scene.setPsxPreset(PsxPipeline.Preset.OFF);
        scene.photographicSoftwareRaster();
        scene.lamp=true;scene.eyeHeight=1.62;
        int images=0,missing=0,grounded=0,failedAngles=0,occludedAngles=0,unstableBaselines=0;
        StringBuilder manifest=new StringBuilder(
            "resident,angle,preset,camera_x,camera_z,render_width,render_height,file\n");
        StringBuilder contacts=new StringBuilder(
            "resident,source_sha,angle,camera_x,camera_z,baseline_depth_changes,opaque_body_pixels,near_sole_pixels,lowest_opaque_row,projected_floor_row,gap_px,receiver_blocked,verdict,boot_crop\n");
        BufferedImage sheet=new BufferedImage(660*5,546,BufferedImage.TYPE_INT_RGB);
        Graphics2D contactSheet=sheet.createGraphics();
        try{
            contactSheet.setColor(new Color(23,27,31));
            contactSheet.fillRect(0,0,sheet.getWidth(),sheet.getHeight());
            contactSheet.setColor(Color.WHITE);
            contactSheet.setFont(new Font(Font.MONOSPACED,Font.BOLD,18));
            int residentIndex=0;
            for(CastleRelations.Resident resident:CastleRelations.Resident.values()){
                var npcRoom=CastleRooms.at(resident.x,0,resident.z);
                int views=0,clearViews=0,residentPassed=0,residentFailed=0,residentOccluded=0;
                for(double[] delta:CAMERAS){
                    double cameraX=resident.x+delta[0],cameraZ=resident.z+delta[1];
                    var currentRoom=CastleRooms.at(cameraX,0,cameraZ);
                    if(!map.traversable(cameraX,cameraZ)||currentRoom==null
                        ||npcRoom==null||!currentRoom.id().equals(npcRoom.id()))continue;
                    scene.x=cameraX;scene.z=cameraZ;
                    scene.yaw=Math.atan2(resident.x-cameraX,resident.z-cameraZ);
                    scene.pitch=PITCH;
                    scene.castleCharactersVisible=false;
                    // First frame may populate Castle geometry/light caches.
                    // A control captured before those caches settle cannot
                    // isolate resident pixels on a later frame.
                    scene.drawCastle(map,.5,true);
                    double[] coldDepth=scene.depths().clone();
                    scene.drawCastle(map,.5,true);
                    int baselineDrift=depthDrift(coldDepth,scene.depths());
                    if(baselineDrift>0){
                        unstableBaselines++;
                        System.out.println("CASTLE_CONTROL_DEPTH_DRIFT "+resident.name+
                            " angle="+(views+1)+" changed_pixels="+baselineDrift);
                    }
                    // Reference is the stable second real-world frame.
                    double[] emptyDepth=scene.depths().clone();
                    int[] emptyPixels=scene.pixels().clone();
                    scene.castleCharactersVisible=true;
                    scene.setPsxPreset(PsxPipeline.Preset.OFF);
                    scene.drawCastle(map,.5,true);
                    if(scene.castleResidentNameplate(resident)==null){
                        System.out.println("CASTLE_ART_CAMERA_OCCLUDED "+resident.name+
                            " x="+cameraX+" z="+cameraZ);
                        continue;
                    }
                    FootEvidence metric=footContact(scene,emptyDepth,emptyPixels,resident);
                    String slug=slug(resident.name);
                    String cropName=slug+"-angle-"+(views+1)+"-boots-3x.png";
                    BufferedImage crop=bootCrop(scene.image(),metric.floorRow());
                    require(ImageIO.write(crop,"png",output.resolve(cropName).toFile()),
                        "boot crop not saved");
                    // The old test overwrote failing screenshots with a later
                    // passing view of the same resident. Always keep EVERY
                    // evaluated angle; choose the first failure for the sheet.
                    if(views==0||(!metric.pass()&&!metric.blockedByScene()&&residentFailed==0)){
                        contactSheet.drawImage(crop,residentIndex*660,36,null);
                        contactSheet.drawString(resident.name+" | "+metric.verdict()+" "+metric.gap()+"px",
                            residentIndex*660+16,25);
                    }
                    String sha=System.getenv().getOrDefault("GITHUB_SHA","local");
                    contacts.append(slug).append(',').append(sha)
                        .append(',').append(views+1)
                        .append(',').append(cameraX).append(',').append(cameraZ)
                        .append(',').append(baselineDrift)
                        .append(',').append(metric.opaque()).append(',')
                        .append(metric.shoePixels()).append(',')
                        .append(metric.bottomRow()).append(',')
                        .append(metric.floorRow()).append(',')
                        .append(metric.gap()).append(',')
                        .append(metric.blockedByScene()).append(',')
                        .append(metric.verdict()).append(',')
                        .append(cropName).append('\n');
                    if(!metric.pass()){
                        String maskName=slug+"-angle-"+(views+1)+"-actor-depth-mask.png";
                        require(ImageIO.write(actorDepthMask(scene,emptyDepth,emptyPixels,metric.floorRow()),
                            "png",output.resolve(maskName).toFile()),
                            "actual-depth diagnostic mask not saved");
                    }
                    if(metric.blockedByScene()){residentOccluded++;occludedAngles++;}
                    else {
                        clearViews++;
                        if(metric.pass())residentPassed++;
                        else{residentFailed++;failedAngles++;}
                    }
                    System.out.println("CASTLE_BOOT_CONTACT "+resident.name+
                        " angle="+(views+1)+" body="+metric.opaque()+
                        " near_sole="+metric.shoePixels()+" gap="+metric.gap()+
                        " verdict="+metric.verdict()+" crop="+cropName);
                    for(PsxPipeline.Preset preset:new PsxPipeline.Preset[]{
                            PsxPipeline.Preset.OFF,PsxPipeline.Preset.CLASSIC}){
                        scene.setPsxPreset(preset);
                        scene.drawCastle(map,.5,true);
                        String name=slug+"-angle-"+(views+1)+"-"+
                            preset.name().toLowerCase(Locale.ROOT)+".png";
                        require(ImageIO.write(scene.image(),"png",output.resolve(name).toFile()),
                            "game screenshot not written: "+name);
                        manifest.append(slug).append(',').append(views+1)
                            .append(',').append(preset.name()).append(',')
                            .append(cameraX).append(',').append(cameraZ).append(',')
                            .append(WIDTH).append(',').append(HEIGHT)
                            .append(',').append(name).append('\n');
                        images++;System.out.println("CASTLE_ART_SCREENSHOT "+name);
                    }
                    views++;
                    // Search additional room-safe cameras until four UNOBSTRUCTED
                    // angles exist, instead of counting a table hiding both boots.
                    if(clearViews>=4)break;
                }
                if(views==0){missing++;System.out.println("CASTLE_ART_NO_VISIBLE_CAMERA "+resident.name);}
                if(residentPassed>=2&&residentFailed==0)grounded++;
                else System.out.println("CASTLE_ART_NOT_PROVEN "+resident.name+
                    " views="+views+" clear="+clearViews+" passed="+residentPassed+
                    " failed="+residentFailed+" occluded="+residentOccluded);
                residentIndex++;
            }
        }finally{contactSheet.dispose();}
        require(ImageIO.write(sheet,"png",output.resolve("actual-game-five-residents-boots-contact-sheet.png").toFile()),
            "contact sheet not saved");
        Files.writeString(output.resolve("captures.csv"),manifest.toString(),StandardCharsets.UTF_8);
        Files.writeString(output.resolve("boot-contact-measurements.csv"),contacts.toString(),StandardCharsets.UTF_8);
        System.out.println("CASTLE_RESIDENT_REAL_SCENE_ART_DIAGNOSTIC captures="+images+
            " missing_residents="+missing+" grounded="+grounded+
            "/5 failed_angles="+failedAngles+" occluded_angles="+occludedAngles+
            " unstable_control_views="+unstableBaselines+
            "; saved ALL source-rendered frames and per-angle boot crops");
        require(missing==0,"Missing Castle resident camera");
        require(grounded==5&&failedAngles==0&&unstableBaselines==0,
            "NPC floor contact unresolved: "+grounded+"/5 residents with clear views, "
                +failedAngles+" measured failures, "+occludedAngles+" occluded and "
                +unstableBaselines+" unstable Castle baseline draws; inspect depth masks before interpreting gaps");
    }
}
