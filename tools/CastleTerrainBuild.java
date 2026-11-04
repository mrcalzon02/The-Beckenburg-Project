package org.beckenburg;

import org.beckenburg.editor.*;
import java.nio.file.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.security.MessageDigest;
import java.util.HexFormat;

/** Author the real editor world, then export its inspected shared runtime faces. */
public final class CastleTerrainBuild {
    public static void main(String[] args)throws Exception{
        Path root=Path.of("development/levels/rebuild/castle/terrain-world");
        var settings=new StitchedHeightmapWorld.Settings(16,8,-24,-8,-.65,15.35,20261103,4);
        var world=Files.exists(root.resolve("world.properties"))?StitchedHeightmapWorld.open(root)
            :StitchedHeightmapWorld.create(root,settings);
        if(!world.settings().equals(settings))throw new IllegalStateException("Castle terrain settings changed");
        if(!Files.exists(world.tilePath(0,0))){
            var image=new BufferedImage(17,17,BufferedImage.TYPE_USHORT_GRAY);
            for(int z=0;z<=16;z++)for(int x=0;x<=16;x++){
                double y=CastleSurroundings.groundHeight(-24+x*8,-8+z*8);
                image.getRaster().setSample(x,z,0,(int)Math.round((y+.65)/16*65535));
            }
            Path source=root.resolve("source.png");ImageIO.write(image,"png",source.toFile());
            world.importTile(0,0,source,false);Files.delete(source);
        }
        var topology=AdaptiveTerrainTopology.open(world);
        var cut=TerrainFootprintCutout.punch(topology,new TerrainEntranceMesh.Bounds(28.5,17.5,43,34));
        topology.save();
        var scene=new TerrainScenePackage(world).openScene();
        TerrainScenePackage.publish(world,scene,"castle",new TerrainEntranceMesh.Bounds(-24,-8,48,40));
        var loaded=TerrainSceneNativePhysics.load(world,scene);
        byte[] encoded=TerrainSceneNativePhysics.encode(world,scene);
        Path assets=Path.of("assets/runtime/sites");
        Files.write(assets.resolve("castle-world-terrain.hcs2"),encoded);
        String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(encoded));
        Files.writeString(assets.resolve("castle-world-terrain.sha256"),hash+"\n");
        var image=new BufferedImage(1100,830,BufferedImage.TYPE_INT_RGB);
        var g=image.createGraphics();g.setColor(new java.awt.Color(0x182128));g.fillRect(0,0,1100,830);
        for(int i=0;i<loaded.physics().triangleCount();i++){
            var t=loaded.physics().triangle(i);var v=java.util.List.of(t.a(),t.b(),t.c());
            int[] xx=new int[3],zz=new int[3];
            for(int j=0;j<3;j++){xx[j]=(int)Math.round(50+(v.get(j).x()+24)*13);zz[j]=(int)Math.round(85+(v.get(j).z()+8)*13);}
            g.setColor(new java.awt.Color(0x78856c));g.fillPolygon(xx,zz,3);
            g.setColor(new java.awt.Color(0xa2b18a));g.drawPolygon(xx,zz,3);
        }
        g.setFont(new java.awt.Font("SansSerif",java.awt.Font.BOLD,17));g.setColor(java.awt.Color.WHITE);
        g.drawString("CASTLE / SAVED EDITOR TERRAIN / ACTUAL EXPORTED TRIANGLES",40,30);
        g.setFont(new java.awt.Font("SansSerif",0,14));
        g.drawString(loaded.triangles()+" shared render/body/optical/support faces; "+topology.cutoutCount()+" punched leaves",40,55);
        g.setColor(new java.awt.Color(0xfff3c4));g.drawString("PUNCHED GROUND",743,440);
        g.drawString("28.5..43 / 17.5..34",743,462);
        g.setColor(java.awt.Color.WHITE);g.drawString("Native terrain inspection. Architectural floor openings and cellar stair are separate construction work.",40,755);
        g.dispose();ImageIO.write(image,"png",Path.of("development/levels/rebuild/previews/castle-saved-terrain-inspection.png").toFile());
        System.out.println("PASS Castle saved editor terrain: "+topology.cutoutCount()+" cut leaves; "
            +loaded.triangles()+" shared physical/render faces; new "+cut+"; sha256 "+hash);
    }
}
