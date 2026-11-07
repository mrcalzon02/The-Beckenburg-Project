package org.beckenburg;
import org.beckenburg.editor.*;
import java.nio.file.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.security.MessageDigest;
import java.util.HexFormat;

/** Persist real 16-bit editor heights and punch the actual cellar before runtime export. */
public final class HouseRebuildLandscapeAuthor {
    public static void main(String[] args)throws Exception{
        var root=Path.of("development/levels/rebuild/house/landscape-world");
        var world=Files.exists(root.resolve("world.properties"))?StitchedHeightmapWorld.open(root):
            StitchedHeightmapWorld.create(root,new StitchedHeightmapWorld.Settings(64,4.8,-103.2,-105.2,-.65,.35,20261031,4));
        var image=new BufferedImage(65,65,BufferedImage.TYPE_USHORT_GRAY);
        for(int z=0;z<=64;z++)for(int x=0;x<=64;x++){
            double wx=-103.2+x*4.8,wz=-105.2+z*4.8;
            double house=Math.hypot(Math.max(0,Math.max(12-wx,wx-21.6)),Math.max(0,Math.max(10-wz,wz-20.5)));
            double road=Math.max(0,Math.max(27-wz,wz-39.4));
            double shoulder=Math.min(1,Math.max(0,(house-1)/4));
            double roadWeight=Math.min(1,road/4),path=Math.min(1,HouseRebuildLandscape.pathDistance(wx,wz)/2.5);
            double hill=.20*Math.exp(-((wx-25)*(wx-25)/90+(wz-6)*(wz-6)/100));
            double swale=-.12*Math.exp(-((wx-7)*(wx-7)/60+(wz-14)*(wz-14)/90));
            double h=(hill+swale+.035*Math.sin(wx*.16)*Math.sin(wz*.13))*shoulder*roadWeight*path;
            image.getRaster().setSample(x,z,0,(int)Math.round((h+.65)*65535));
        }
        Path input=Files.createTempFile("house-landscape-",".png");
        try{ImageIO.write(image,"png",input.toFile());world.importTile(0,0,input,true);}finally{Files.deleteIfExists(input);}
        byte[] before=Files.readAllBytes(world.tilePath(0,0));
        var topology=AdaptiveTerrainTopology.open(world);
        for(long z=22;z<=29;z++)for(long x=22;x<=27;x++){
            for(var node:topology.leaves(x,z))if(node.depth()<2){
                topology.split(node);
                for(var child:topology.leaves(x,z))if(child.depth()<2)topology.split(child);
            }
        }
        var punch=TerrainFootprintCutout.punch(topology,new TerrainEntranceMesh.Bounds(12,10,21.6,20.5));topology.save();
        if(!java.util.Arrays.equals(before,Files.readAllBytes(world.tilePath(0,0))))throw new AssertionError("Punch changed source heights");
        var scene=new TerrainScenePackage(world).openScene();
        var result=TerrainScenePackage.publish(world,scene,"world",new TerrainEntranceMesh.Bounds(-39,-48,86,89));
        byte[] bytes=TerrainSceneNativePhysics.encode(world,scene);
        String sha=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        Files.write(Path.of("assets/runtime/sites/briar-landscape-world.hcs2"),bytes);
        Files.writeString(Path.of("assets/runtime/sites/briar-landscape-world.sha256"),sha+"\n");
        if(!sha.equals(HouseRebuildLandscape.TERRAIN_SHA))throw new AssertionError("Review changed terrain and update its explicit source binding: "+sha);
        System.out.println("AUTHORED House landscape "+sha+" triangles="+result.triangles()+" "+punch);
    }
}
