package org.beckenburg;
import java.nio.file.*;
import javax.imageio.ImageIO;
/** Export exact generated runtime pixels for source/art review, without resampling. */
public final class MaterialAtlasExport {
    public static void main(String[] args)throws Exception{
        if(args.length!=1)throw new IllegalArgumentException("Output directory required");
        Path folder=Path.of(args[0]);Files.createDirectories(folder);var atlas=MaterialAtlas.image();
        ImageIO.write(atlas,"png",folder.resolve("runtime-atlas.png").toFile());
        int x=(MaterialAtlas.IRON%MaterialAtlas.GRID)*MaterialAtlas.TILE,y=(MaterialAtlas.IRON/MaterialAtlas.GRID)*MaterialAtlas.TILE;
        ImageIO.write(atlas.getSubimage(x,y,MaterialAtlas.TILE,MaterialAtlas.TILE),"png",folder.resolve("painted-metal-tile.png").toFile());
    }
}
