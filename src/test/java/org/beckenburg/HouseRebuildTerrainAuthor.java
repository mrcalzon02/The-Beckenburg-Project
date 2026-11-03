package org.beckenburg;

import org.beckenburg.editor.*;
import java.nio.file.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.security.MessageDigest;
import java.util.HexFormat;

/** Reproducible editor-world authoring, not an alternate procedural ground mesh. */
public final class HouseRebuildTerrainAuthor {
    public static void main(String[] args)throws Exception{
        Path root=Path.of("development/levels/rebuild/house/terrain-world");
        StitchedHeightmapWorld world;
        if(Files.exists(root.resolve("world.properties")))world=StitchedHeightmapWorld.open(root);
        else{
            world=StitchedHeightmapWorld.create(root,new StitchedHeightmapWorld.Settings(
                64,4.8,-103.2,-105.2,-.65,.35,20261031,4));
            Path input=Files.createTempFile("house-terrain-source-",".png");
            try{
                ImageIO.write(new BufferedImage(65,65,BufferedImage.TYPE_USHORT_GRAY),"png",input.toFile());
                world.importTile(0,0,input,false);
            }finally{Files.deleteIfExists(input);}
        }
        byte[] source=Files.readAllBytes(world.tilePath(0,0));
        var topology=AdaptiveTerrainTopology.open(world);
        var result=TerrainFootprintCutout.punch(topology,new TerrainEntranceMesh.Bounds(12,10,21.6,20.5));
        topology.save();
        if(!java.util.Arrays.equals(source,Files.readAllBytes(world.tilePath(0,0))))
            throw new AssertionError("Hole punching modified source heights");
        var scene=new TerrainScenePackage(world).openScene();
        var published=TerrainScenePackage.publish(world,scene,"world",new TerrainEntranceMesh.Bounds(-39,-48,86,89));
        byte[] bytes=TerrainSceneNativePhysics.encode(world,scene);
        String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        Files.write(Path.of("assets/runtime/sites/briar-rebuild-world.hcs2"),bytes);
        Files.writeString(Path.of("assets/runtime/sites/briar-rebuild-world.sha256"),hash+"\n");
        if(!hash.equals(HouseRebuild.TERRAIN_SHA))throw new AssertionError("Update the explicit runtime terrain binding after review: "+hash);
        System.out.println("Punched editor terrain: "+result+"; "+published.triangles()+" triangles; SHA-256 "+hash);
    }
}
