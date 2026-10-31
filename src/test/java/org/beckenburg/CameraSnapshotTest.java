package org.beckenburg;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.util.List;
import javax.imageio.ImageIO;

/** Functional acceptance: shutter renders a fresh perspective frame and writes real pixels. */
public final class CameraSnapshotTest {
    private CameraSnapshotTest(){}
    private static void check(boolean ok,String message){
        if(!ok)throw new AssertionError(message);
    }
    private static int differing(BufferedImage a,BufferedImage b){
        check(a.getWidth()==b.getWidth()&&a.getHeight()==b.getHeight(),"compare sizes");
        int count=0;
        for(int y=0;y<a.getHeight();y++)for(int x=0;x<a.getWidth();x++)
            if(a.getRGB(x,y)!=b.getRGB(x,y))count++;
        return count;
    }
    public static void main(String[] args)throws Exception{
        HouseMap map=BlackpineLevel.map();
        SceneRenderer hunter=new SceneRenderer(),lens=new SceneRenderer();
        hunter.x=48.5;hunter.z=32.3;hunter.eyeHeight=map.floorY(0)+1.62;
        hunter.yaw=0;hunter.pitch=0;hunter.lamp=false;
        BufferedImage ahead=CameraSnapshot.network(lens,hunter,map,List.of(),
            null,1.0,List.of(),EquipmentTiers.Tier.II);
        check(ahead.getWidth()==320&&ahead.getHeight()==200,
            "Tier II photograph size uses actual rendered lens image");
        check(lens.frameDiagnostics()!=null&&!lens.frameDiagnostics().gpu(),
            "still camera must never copy an async GPU frame");
        check(Math.abs(lens.x-hunter.x)<1e-6
            &&Math.abs(lens.z-(hunter.z+.12))<1e-6,
            "camera lens must be in front of eye, not a stale player-view buffer");
        hunter.yaw=Math.PI;
        BufferedImage behind=CameraSnapshot.network(lens,hunter,map,List.of(),
            null,1.1,List.of(),EquipmentTiers.Tier.II);
        check(differing(ahead,behind)>150,
            "turning the camera must photograph different actual scene geometry");
        check(hunter.x==48.5&&hunter.z==32.3,
            "photo must not move the hunter's gameplay position");
        var photo=CameraSnapshot.network(lens,hunter,map,List.of(),
            null,1.2,List.of(),EquipmentTiers.Tier.I);
        check(photo.getWidth()==160&&photo.getHeight()==160,
            "instant camera captures a square print from the same world lens");
        var folder=Files.createTempDirectory("beckenburg-camera-test-");
        try{
            var path=HunterPhotography.save(photo,folder,0,1);
            var stored=ImageIO.read(path.toFile());
            check(stored!=null&&differing(stored,photo)==0,
                "PNG must preserve actual photo pixels, not placeholder art");
        }finally{
            try(var paths=Files.list(folder)){for(var path:paths.toList())Files.deleteIfExists(path);}
            Files.deleteIfExists(folder);
        }
        System.out.println("PASS synchronous real-lens world perspective, yaw, film optics and PNG pixels");
    }
}
