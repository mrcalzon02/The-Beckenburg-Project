package org.beckenburg;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import javax.imageio.ImageIO;

/** Non-release-gating original-art fidelity diagnostics for the LAN lobby. */
public final class OriginalHunterLobbyPortraitTest {
    private OriginalHunterLobbyPortraitTest(){}
    private static void check(boolean value,String message){
        if(!value)throw new AssertionError(message);
    }
    public static void main(String[] args)throws Exception{
        int[][] rects={{24,300,218,727},{24,294,206,685},{23,291,208,690}};
        var identities=CharacterVisuals.Hunter.values();
        Path result=Path.of("build/e2e/hunter-original-lobby");
        Files.createDirectories(result);
        int[] fingerprint=new int[identities.length];
        for(int i=0;i<identities.length;i++){
            var identity=identities[i];
            String name=identity.name().toLowerCase(java.util.Locale.ROOT);
            BufferedImage master=ImageIO.read(
                Path.of("assets/concept-art/v001/01-hunters/"+name+"-character-profile.png").toFile());
            BufferedImage portrait=LobbyGhostAvatar.originalHunterPortrait(identity);
            int[] rect=rects[i];
            check(portrait!=null,"Original artwork was NOT packaged for "+name);
            check(portrait.getWidth()==rect[2]-rect[0]
                    &&portrait.getHeight()==rect[3]-rect[1],
                "The full-resolution source crop must survive packing: "+name);
            // This checks ALL source pixels, including the boots that the
            // alpha-segmented 128x224 world sprite currently amputates.
            for(int y=0;y<portrait.getHeight();y++)for(int x=0;x<portrait.getWidth();x++)
                check(portrait.getRGB(x,y)==master.getRGB(x+rect[0],y+rect[1]),
                    "Original approved Hunter source pixel was lost: "+name+" at "+x+","+y);
            check(portrait.getHeight()>350,"Lobby must not reuse the tiny gameplay sprite");
            // The original master may itself carry an alpha channel. Preserve
            // it EXACTLY rather than rejecting a valid, intact source format.
            check(portrait.getHeight()>DirectionalSprites.frame(name,0).getHeight(),
                "Do not substitute cropped 128x224 directional gameplay art");
            LobbyGhostAvatar.Preview preview=new LobbyGhostAvatar.Preview();
            preview.setSize(208,300);
            preview.showHunter(identity);
            check(preview.displayedHunter()==identity&&!preview.showingGhost(),
                "Hunter selector must display selected original class");
            BufferedImage output=new BufferedImage(208,300,BufferedImage.TYPE_INT_RGB);
            var graphics=output.createGraphics();
            preview.paint(graphics);
            graphics.dispose();
            ImageIO.write(output,"png",result.resolve(name+"-actual-lobby.png").toFile());
            fingerprint[i]=Arrays.hashCode(output.getRGB(0,0,208,300,null,0,208));
        }
        check(Arrays.stream(fingerprint).distinct().count()==3,
            "Each class must have its own actual full-height original art");
        System.out.println("PASS: all three shipped Hunter lobby cards equal their uncut "
            +"original approved concept pixels; 3 real GUI preview screenshots written");
    }
}
