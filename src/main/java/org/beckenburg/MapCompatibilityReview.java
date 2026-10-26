package org.beckenburg;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;

/** Actual completed OpenGL frames; no mission unlock or gameplay acceptance implied. */
public final class MapCompatibilityReview {
    record Shot(String label,LevelOpeningFlyover.Pose pose,boolean breaker,boolean torch){}
    private static final Path ROOT=Path.of(System.getProperty("beckenburg.map.review.folder","development/levels/previews/lighting-camera-audit"));
    private static final StringBuilder REPORT=new StringBuilder("map\tview\tframe\twidth\theight\tmean_luma\tbright_pixels\tsha256\n");
    private static String hash(byte[] b)throws Exception{return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(b));}
    static void capture(String id,Shot shot,SceneRenderer renderer,HouseMap map,CastleHubMap castle)throws Exception{
        Files.createDirectories(ROOT);
        shot.pose.apply(renderer);renderer.roomLights.setBreaker(shot.breaker);renderer.lamp=shot.torch;
        double time=2.5;
        if(map==null)renderer.drawCastle(castle,time,shot.breaker);else renderer.drawNetwork(map,List.of(),-1,time);
        long expected=renderer.submittedGpuFrameId(),deadline=System.nanoTime()+30_000_000_000L;
        var gpu=GpuWorldRenderer.active();if(gpu==null)throw new IllegalStateException("Actual GPU required");
        GpuWorldRenderer.Result result=null;
        while(System.nanoTime()<deadline){var next=gpu.latest();if(next!=null&&next.frameId()==expected){result=next;break;}Thread.sleep(5);}
        if(result==null)throw new IllegalStateException("GPU frame timeout: "+id+"/"+shot.label);
        renderer.pinCompletedGpuFrame(result);
        if(map==null)renderer.drawCastle(castle,time,shot.breaker);else renderer.drawNetwork(map,List.of(),-1,time);
        Path file=ROOT.resolve(id+"-"+shot.label+".png");ImageIO.write(renderer.image(),"png",file.toFile());
        double energy=0;int bright=0;for(int y=0;y<renderer.height();y++)for(int x=0;x<renderer.width();x++){int p=renderer.image().getRGB(x,y);double l=((p>>>16&255)*.2126+(p>>>8&255)*.7152+(p&255)*.0722);energy+=l;if(l>32)bright++;}
        REPORT.append(id).append('\t').append(shot.label).append('\t').append(expected).append('\t').append(renderer.width()).append('\t').append(renderer.height()).append('\t').append(String.format(Locale.ROOT,"%.4f",energy/(renderer.width()*renderer.height()))).append('\t').append(bright).append('\t').append(hash(Files.readAllBytes(file))).append('\n');
        System.out.println("CAPTURE actual OpenGL "+id+" / "+shot.label+" frame="+expected+" bright="+bright);
        Files.writeString(Path.of("development/levels/verification/map-lighting-camera-audit"+System.getProperty("beckenburg.map.review.suffix","")+".tsv"),REPORT);
    }
    public static void execute()throws Exception{
        Files.createDirectories(ROOT);Files.createDirectories(Path.of("development/levels/verification"));
        String filter=System.getProperty("beckenburg.map.review.sites","");
        for(var site:HouseMap.Site.values()){
            if(!filter.isBlank()&&!Arrays.asList(filter.split(",")).contains(site.name()))continue;
            var map=new HouseMap(site);String id=map.level().id();var camera=new SceneRenderer();camera.setRenderSize(Integer.getInteger("beckenburg.map.review.width",480),Integer.getInteger("beckenburg.map.review.width",480)*5/8);camera.roomLights=new RoomLighting();camera.setLampHardware(EquipmentTiers.Tier.II,false);
            var p=map.van().spawn(0);var player=new LevelOpeningFlyover.Pose(p.x(),map.van().floorY()+1.62,p.z(),map.van().placement().yaw(),-.09);var flight=new LevelOpeningFlyover(map,player);
            var at=map.spawn(0);var interior=new LevelOpeningFlyover.Pose(at.x(),at.y()+1.62,at.z(),Math.PI/3,-.12);
            for(var shot:List.of(new Shot("opening",flight.pose(0),true,false),new Shot("approach",flight.pose(.78),true,false),new Shot("lit",interior,true,false),new Shot("blackout",interior,false,false),new Shot("torch",interior,false,true),new Shot("pan",new LevelOpeningFlyover.Pose(at.x(),at.y()+1.62,at.z(),interior.yaw()+Math.PI/2,-.12),true,true)))capture(id,shot,camera,map,null);
        }
        if(!filter.isBlank()&&!Arrays.asList(filter.split(",")).contains("CASTLE"))return;
        var castle=new CastleHubMap();var camera=new SceneRenderer();camera.setRenderSize(Integer.getInteger("beckenburg.map.review.width",480),Integer.getInteger("beckenburg.map.review.width",480)*5/8);camera.roomLights=new RoomLighting();camera.setLampHardware(EquipmentTiers.Tier.II,false);var p=CastleLevelEdits.current().spawn();var pose=new LevelOpeningFlyover.Pose(p[0],1.62,p[1],p[2],-.1);
        for(var shot:List.of(new Shot("lit",pose,true,false),new Shot("blackout",pose,false,false),new Shot("torch",pose,false,true),new Shot("pan",new LevelOpeningFlyover.Pose(p[0],1.62,p[1],p[2]+Math.PI/2,-.1),true,true)))capture("castle",shot,camera,null,castle);
        System.out.println("PASS completed actual OpenGL compatibility captures; human visual, full mission and performance acceptance remain separate");
    }
    private MapCompatibilityReview(){}
}
