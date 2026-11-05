package org.beckenburg;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;

/**
 * REAL-world renderer screenshot acceptance for loading fly-in even when no
 * OpenGL backend is installed. The old software path was a 2D map and passed
 * camera-path unit tests because the world camera was never rendered.
 */
public final class LevelOpeningCinematicVisualTest {
    private static void check(boolean ok,String message){
        if(!ok)throw new AssertionError(message);
    }
    private static void image(Path root,String name,BufferedImage frame)throws Exception{
        check(frame!=null&&frame.getWidth()>0&&frame.getHeight()>0,
            "Missing genuine 3D world render: "+name);
        check(ImageIO.write(frame,"png",root.resolve(name).toFile()),
            "Could not persist real game-frame PNG "+name);
    }
    private static int diversity(BufferedImage picture){
        var colors=new java.util.HashSet<Integer>();
        for(int y=0;y<picture.getHeight();y+=7)
            for(int x=0;x<picture.getWidth();x+=7)
                colors.add(picture.getRGB(x,y)&0xffffff);
        return colors.size();
    }
    public static void main(String[] args)throws Exception {
        Path root=Path.of("build/e2e/true-3d-loading");
        Files.createDirectories(root);
        var map=new HouseMap(HouseMap.Site.HOUSE);
        var start=map.van().spawn(0);
        var player=new LevelOpeningFlyover.Pose(start.x(),
            map.van().floorY()+1.62,start.z(),-Math.PI/2,-.09);
        var gameplay=new SceneRenderer();
        player.apply(gameplay);
        gameplay.setRenderSize(426,240);
        // Prewarm exactly the renderer handed to gameplay, without mutating
        // the authoritative van or player's future first-person camera.
        var warm=InvestigationScenePrewarm.prepare(map,gameplay,
            new LevelOpeningFlyover(map,player),()->false);
        check(gameplay.x==player.x()&&gameplay.z==player.z()
                &&gameplay.eyeHeight==player.y()
                &&gameplay.yaw==player.yaw()&&gameplay.pitch==player.pitch(),
            "Loading prewarm must restore the real player camera");
        check(gameplay.width()==426&&gameplay.height()==240,
            "Loading prewarm must restore the original render target");
        check(map.van().spawn(0).equals(start),
            "Prewarming must not move the real van, spawn or collision world");
        // Save four real 3D camera frames through the van cargo entrance.
        var pathCamera=new LevelOpeningFlyover(map,player);
        var witness=new SceneRenderer();
        witness.photographicSoftwareRaster();
        witness.setRenderSize(426,240);
        for(double time:new double[]{.90,.965,.975,1.0}){
            var view=pathCamera.pose(time);
            view.apply(witness);
            witness.drawPractice(map,List.of(),0);
            String filename=switch((int)Math.round(time*1000)){
                case 900->"real-3d-approach-outside-van.png";
                case 965->"real-3d-crossing-cargo-door.png";
                case 975->"real-3d-inside-cargo-aisle.png";
                default->"real-3d-hunter-camera-handoff.png";
            };
            check(diversity(witness.image())>45,
                "World-space van approach must show textured 3D geometry: "+filename);
            image(root,filename,witness.image());
        }
        check(warm.passes()>0&&warm.passes()<=2&&warm.staticShellChunks()>0,
            "World prewarm must actually retain constructed shell chunks");

        try(var cinematic=new LevelOpeningCinematic(map,gameplay,640,360)){
            check(cinematic.uses3DWorldCamera(),
                "Software-only clients must also render the world-space approach");
            long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(20);
            while(!cinematic.previewStatus().firstFrameReady()
                    &&System.nanoTime()<deadline){
                cinematic.update(640,360);
                Thread.sleep(40);
            }
            var status=cinematic.previewStatus();
            check(status.firstFrameReady(),
                "3D fly-in did not render a real scene (requests="+status.requests()
                +", failures="+status.failures()+")");
            var raw=cinematic.latest3DFrame();
            image(root,"real-world-3d-first-aerial.png",raw);
            Files.writeString(root.resolve("aerial-diagnostics.txt"),
                "rendered_pixels="+raw.getWidth()*raw.getHeight()
                +"\nunique_colors="+diversity(raw)
                +"\nfirst_frame_ms="+status.firstFrameElapsedMs()
                +"\nfailures="+status.failures()+"\n",StandardCharsets.UTF_8);
            check(diversity(raw)>45,
                "3D loading renderer produced an empty/flat aerial image: "
                    +diversity(raw)+" sampled colors, screenshot preserved");
            BufferedImage composed=new BufferedImage(640,360,BufferedImage.TYPE_INT_RGB);
            Graphics2D g=composed.createGraphics();
            try{cinematic.draw(g,640,360);}finally{g.dispose();}
            image(root,"real-world-3d-loading-screen.png",composed);
            String summary="3d_world_frame=true\nsoftware_renderer=true\n"
                +"preview_requests="+status.requests()+"\nfailures="+status.failures()
                +"\nframe_latency_ms="+status.firstFrameElapsedMs()
                +"\nunique_pixels="+diversity(raw)+"\nprewarm_passes="+warm.passes()
                +"\nprewarm_ms="+warm.elapsedMs()
                +"\nstatic_shell_chunks="+warm.staticShellChunks()
                +"\nstatic_shell_faces="+warm.staticFaces()+"\n";
            Files.writeString(root.resolve("verification.txt"),summary,StandardCharsets.UTF_8);
            System.out.println("PASS true 3D map-to-van approach on SOFTWARE backend, "
                +diversity(raw)+" distinct sampled colors, prewarm="+warm.passes()
                +" source shell chunks="+warm.staticShellChunks());
        }
    }
}
