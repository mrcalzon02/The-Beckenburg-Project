package org.beckenburg;

import java.util.Random;

/** Cosmetic appearance is independent of secret species; not an evidence channel. */
public final class LobbyGhostAvatarTest {
    private static void ok(boolean yes,String why){if(!yes)throw new AssertionError(why);}
    public static void main(String[] args){
        var random=new Random(26);
        java.util.HashSet<String> seen=new java.util.HashSet<>();
        for(int i=0;i<150;i++){
            var look=LobbyGhostAvatar.random(random);
            ok(LobbyGhostAvatar.Look.decode(look.frame()).equals(look),"appearance frame round trip");
            ok(!look.frame().contains("WRAITH")&&!look.frame().contains("BANSHEE"),
                "no ghost archetype disclosed");
            ok(DirectionalSprites.frame("banshee",look.facing())!=null,
                "only real packaged approved ghost art used");
            seen.add(look.frame());
        }
        ok(seen.size()>4,"randomized ghost appearances");
        var preview=new LobbyGhostAvatar.Preview();
        preview.setSize(208,300);
        ok(preview.getPreferredSize().height>=280,"full-height character preview is reserved");
        int[] fingerprints=new int[CharacterVisuals.Hunter.values().length];
        for(var hunter:CharacterVisuals.Hunter.values()){
            preview.showHunter(hunter);
            ok(preview.displayedHunter()==hunter&&!preview.showingGhost(),
                "selected Hunter art identity is displayed");
            var art=DirectionalSprites.frame(hunter.name().toLowerCase(java.util.Locale.ROOT),0);
            var bounds=LobbyGhostAvatar.Preview.artBounds(art);
            ok(bounds.height>0&&bounds.width>0&&bounds.width<=art.getWidth()&&bounds.height<=art.getHeight(),
                "real hunter art fits without losing visible pixels");
            var rendered=new java.awt.image.BufferedImage(208,300,java.awt.image.BufferedImage.TYPE_INT_RGB);
            var g=rendered.createGraphics();preview.paint(g);g.dispose();
            int[] pixels=rendered.getRGB(0,0,208,300,null,0,208);
            fingerprints[hunter.ordinal()]=java.util.Arrays.hashCode(pixels);
        }
        ok(java.util.Arrays.stream(fingerprints).distinct().count()==fingerprints.length,
            "all three approved Hunter portraits render distinctly");
        try{LobbyGhostAvatar.Look.decode("LOBBYAVATAR|4|0");
            throw new AssertionError("unsupported orientation accepted");
        }catch(IllegalArgumentException expected){}
        try{LobbyGhostAvatar.Look.decode("LOBBYAVATAR|0|99");
            throw new AssertionError("invalid palette accepted");
        }catch(IllegalArgumentException expected){}
        System.out.println("PASS: independent cosmetic lobby ghost, valid art, no species disclosure");
    }
}
