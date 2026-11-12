package org.beckenburg;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import javax.imageio.ImageIO;

/** Native game-camera render and bounded network proof for ghost sound waves. */
public final class GhostSoundRipplesTest {
    private static void check(boolean yes,String reason){if(!yes)throw new AssertionError(reason);}
    private static void rejects(Runnable task){
        try{task.run();}catch(IllegalArgumentException expected){return;}
        throw new AssertionError("Malformed or private spectral sound packet accepted");
    }
    private static int[] pixels(BufferedImage image){
        return image.getRGB(0,0,image.getWidth(),image.getHeight(),null,0,image.getWidth());
    }
    public static void main(String[] args)throws Exception{
        var map=new HouseMap();
        var door=AuthoredTestSupport.door(map,3,4);
        var viewer=AuthoredTestSupport.side(map,door,4,1.45);
        var source=AuthoredTestSupport.side(map,door,3,1.45);
        check(!map.doorOpen(door.id()),"Proof door starts physically shut");
        check(!map.clearLineOfSight(viewer.x(),viewer.y()+1.1,viewer.z(),
                source.x(),source.y()+1.1,source.z()),
            "Proof source is genuinely hidden behind real door geometry");

        var ripple=new GhostSoundRipples.Ripple(LanGhostPursuit.Sound.FOOTSTEP,
            source.x(),source.y(),source.z(),45);
        String packet=ripple.frame();
        check(packet.startsWith("GRIPPLE|FOOTSTEP|")&&packet.split("\\|",-1).length==6,
            "Authoritative ripple frame carries sound kind, emission point and tick only");
        var decoded=GhostSoundRipples.Ripple.decode(packet,map);
        check(Math.abs(decoded.x()-source.x())<=.0005&&
              Math.abs(decoded.z()-source.z())<=.0005,
            "Event location survives bounded millimetre protocol");
        check(!packet.contains("H1|")&&!packet.contains("BODY|")&&!packet.contains("TEAMMAP|"),
            "Ripple packet exposes neither a hunter identity nor inventory/telemetry");
        rejects(()->GhostSoundRipples.Ripple.decode("GRIPPLE|FOOTSTEP|NaN|0|1|45",map));
        rejects(()->GhostSoundRipples.Ripple.decode("GRIPPLE|FOOTSTEP|999999|0|1|45",map));
        rejects(()->GhostSoundRipples.Ripple.decode("GRIPPLE|INVALID|1|0|1|45",map));
        rejects(()->GhostSoundRipples.Ripple.decode("GRIPPLE|FOOTSTEP|1|0|1|45|H1",map));
        var van=map.spawn(HouseMap.EXTRACTION_ROOM);
        rejects(()->GhostSoundRipples.Ripple.decode(
            new GhostSoundRipples.Ripple(LanGhostPursuit.Sound.FOOTSTEP,
                van.x(),van.y(),van.z(),45).frame(),map));

        SceneRenderer camera=new SceneRenderer();
        camera.setRenderSize(480,300);
        camera.x=viewer.x();camera.z=viewer.z();camera.eyeHeight=viewer.y()+1.62;
        camera.yaw=Math.atan2(source.x()-viewer.x(),source.z()-viewer.z());
        camera.pitch=-.08;camera.lamp=true;camera.lampIntensity=1.6;
        camera.renderExactFrame(()->camera.drawPractice(map,java.util.List.of(),0));
        BufferedImage before=new BufferedImage(480,300,BufferedImage.TYPE_INT_RGB);
        Graphics2D first=before.createGraphics();
        first.drawImage(camera.image(),0,0,480,300,null);first.dispose();

        var effect=new GhostSoundRipples();
        long now=1_000_000_000L;
        effect.accept(decoded,45,now);
        BufferedImage after=new BufferedImage(480,300,BufferedImage.TYPE_INT_RGB);
        Graphics2D painted=after.createGraphics();
        painted.drawImage(camera.image(),0,0,480,300,null);
        effect.draw(painted,camera,480,300,now+430_000_000L);
        painted.dispose();
        BufferedImage fullyMuted=new BufferedImage(480,300,BufferedImage.TYPE_INT_RGB);
        Graphics2D muted=fullyMuted.createGraphics();
        muted.drawImage(camera.image(),0,0,480,300,null);
        effect.draw(muted,camera,480,300,now+430_000_000L,0);
        muted.dispose();
        check(Arrays.equals(pixels(before),pixels(fullyMuted)),
            "Zero hearing gain cannot reveal through-wall source information");
        int difference=0;
        int[] original=pixels(before),withWave=pixels(after);
        for(int i=0;i<original.length;i++)if(original[i]!=withWave[i])difference++;
        check(difference>=80,"Actual 3D door scene needs clearly visible through-wall sound rings");
        check(effect.size()==1,"Only one source event is stored, not a persistent hunter body");
        var brief=new GhostSoundRipples.Ripple(LanGhostPursuit.Sound.CHAT,
            source.x(),source.y(),source.z(),45,GhostSoundProfile.chat(2));
        var extendedChat=new GhostSoundRipples.Ripple(LanGhostPursuit.Sound.CHAT,
            source.x(),source.y(),source.z(),45,GhostSoundProfile.chat(80));
        var shortWaves=new GhostSoundRipples(),longWaves=new GhostSoundRipples();
        shortWaves.accept(brief,45,now);longWaves.accept(extendedChat,45,now);
        long late=now+2_850_000_000L;
        BufferedImage longScene=new BufferedImage(480,300,BufferedImage.TYPE_INT_RGB);
        Graphics2D drawn=longScene.createGraphics();
        drawn.drawImage(camera.image(),0,0,480,300,null);
        shortWaves.draw(drawn,camera,480,300,late);
        check(shortWaves.size()==0,"Brief chat ripples expire promptly");
        longWaves.draw(drawn,camera,480,300,late);
        drawn.dispose();
        check(!Arrays.equals(pixels(before),pixels(longScene)),
            "Long accepted chat visibly sustains waves after short chat has faded");
        var decodedExtended=GhostSoundRipples.Ripple.decode(extendedChat.frame(),map);
        check(decodedExtended.profile().equals(extendedChat.profile())
                &&decodedExtended.frame().split("\\|",-1).length==8,
            "Bounded intensity/duration survives wire without raw chat content");
        BufferedImage expired=new BufferedImage(480,300,BufferedImage.TYPE_INT_RGB);
        Graphics2D done=expired.createGraphics();
        done.drawImage(camera.image(),0,0,480,300,null);
        effect.draw(done,camera,480,300,now+3_000_000_000L);done.dispose();
        check(effect.size()==0&&Arrays.equals(pixels(before),pixels(expired)),
            "Sound waves expire and restore exact native image without tracking silence");
        for(int i=0;i<GhostSoundRipples.MAX_ACTIVE+9;i++)
            effect.accept(new GhostSoundRipples.Ripple(
                LanGhostPursuit.Sound.VOICE,source.x(),source.y(),source.z(),i+50),i+50,
                now+i*110_000_000L);
        check(effect.size()<=GhostSoundRipples.MAX_ACTIVE,
            "Bursty footsteps and voice never create unbounded renderer allocations");
        effect.clear();check(effect.size()==0,"Movement clears lingering sound emissions");
        Path output=Path.of("build","e2e","ghost-sound-evidence");
        Files.createDirectories(output);
        ImageIO.write(before,"png",output.resolve("closed-door-before.png").toFile());
        ImageIO.write(after,"png",output.resolve("ghost-sound-ripples-through-closed-door.png").toFile());
        ImageIO.write(longScene,"png",output.resolve("long-chat-sustained-ripples.png").toFile());
        System.out.println("PASS ghost sound rings: source event wire, real hidden door, x-ray 3D world projection, visible expanding circles, expiry, bounded memory and privacy");
    }
}
