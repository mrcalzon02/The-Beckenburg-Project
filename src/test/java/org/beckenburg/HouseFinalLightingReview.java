package org.beckenburg;

import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;

/** Matched actual rendered states for manual join/circuit review; not automatic acceptance. */
public final class HouseFinalLightingReview {
    private record State(String name,boolean open,boolean own,boolean supplied,boolean torch){}
    private static final List<State> STATES=List.of(
        new State("closed-all-circuits",false,true,true,false),
        new State("closed-own-circuit-off",false,false,true,false),
        new State("closed-breaker-off",false,true,false,false),
        new State("open-own-circuit-off",true,false,true,false),
        new State("open-all-circuits",true,true,true,false),
        new State("closed-breaker-off-torch",false,true,false,true));
    private static final int WIDTH=640,HEIGHT=400,LABEL=24;
    public static void main(String[] args)throws Exception{
        if(args.length<1||args.length>2)throw new IllegalArgumentException("HouseFinalLightingReview <output> [comma-separated rooms]");
        Path out=Path.of(args[0]);Files.createDirectories(out);
        var rooms=new ArrayList<Integer>();
        if(args.length==2)for(String id:args[1].split(","))rooms.add(Integer.parseInt(id));
        else for(int room=0;room<15;room++)rooms.add(room);
        var map=HouseRebuildFurnishings.map();var level=map.level();
        var renderer=new SceneRenderer();renderer.setRenderSize(WIDTH,HEIGHT);
        renderer.setPsxPreset(PsxPipeline.Preset.CLASSIC);renderer.environmentLight=1;
        var csv=new StringBuilder("room,name,x,feetY,z,yaw,pitch,state,doors,ownCircuit,breaker,torch,sha256\n");
        for(int room:rooms)for(boolean lower:new boolean[]{false,true}){
            var space=level.room(room);var p=space.spawn();
            if(!map.traversable(p.x(),p.y(),p.z())||map.roomAt(p.x(),p.y(),p.z())!=room)
                throw new AssertionError("Unsupported review camera "+room);
            double yaw=Math.atan2(space.x0()+.18-p.x(),space.z0()+.18-p.z());
            // Aim upward into the actual ceiling/wall corner, with room fittings in frame.
            double pitch=lower?-.35:room<12?.20:-.04;
            var sheet=new BufferedImage(WIDTH*3,(HEIGHT+LABEL)*2,BufferedImage.TYPE_INT_RGB);
            var graphics=sheet.createGraphics();graphics.setFont(new Font(Font.MONOSPACED,Font.PLAIN,13));
            Path folder=out.resolve(String.format(Locale.ROOT,"room-%02d%s",room,lower?"-floor-joins":""));Files.createDirectories(folder);
            for(int index=0;index<STATES.size();index++){
                var state=STATES.get(index);
                for(var door:level.portals())if(door.door()){
                    if(map.doorOpen(door.id())!=state.open())map.toggleDoor(door.id());
                    map.doorOpenFraction(door.id(),System.nanoTime()+HouseMap.DOOR_SWING_NANOS);
                }
                var circuits=new RoomLighting();
                if(room<HouseMap.INDOOR_ROOMS&&!state.own())circuits.toggle(room);
                circuits.setBreaker(state.supplied());
                renderer.roomLights=circuits;renderer.roomLightTick=12;renderer.lamp=state.torch();
                renderer.x=p.x();renderer.z=p.z();renderer.eyeHeight=p.y()+1.62;renderer.yaw=yaw;renderer.pitch=pitch;
                renderer.drawNetwork(map,List.of(),-1,12);
                Path file=folder.resolve(state.name()+"-runtime.png");ImageIO.write(renderer.image(),"png",file.toFile());
                int x=index%3*WIDTH,y=index/3*(HEIGHT+LABEL);
                graphics.setColor(Color.BLACK);graphics.fillRect(x,y,WIDTH,LABEL);
                graphics.setColor(Color.WHITE);graphics.drawString("room "+room+(lower?" floor":" upper")+" / "+state.name(),x+7,y+17);
                graphics.drawImage(renderer.image(),x,y+LABEL,null);
                String hash=HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));
                csv.append(room).append(',').append(space.name().replace(',',' ')).append(',')
                    .append(p.x()).append(',').append(p.y()).append(',').append(p.z()).append(',').append(yaw).append(',').append(pitch).append(',')
                    .append(state.name()).append(',').append(state.open()).append(',').append(state.own()).append(',')
                    .append(state.supplied()).append(',').append(state.torch()).append(',').append(hash).append('\n');
            }
            graphics.dispose();ImageIO.write(sheet,"png",folder.resolve("matched-lighting-contact-sheet.png").toFile());
            System.out.println("CAPTURED room "+room+(lower?" floor":" upper")+" "+space.name()+": six matched states; visual inspection pending");
        }
        Files.writeString(out.resolve("cameras-and-states.csv"),csv,java.nio.charset.StandardCharsets.UTF_8);
        Files.writeString(out.resolve("source-revision.txt"),HouseRebuildFurnishings.source().revision()+"\n",java.nio.charset.StandardCharsets.UTF_8);
        System.out.println("REVIEW REQUIRED: actual software runtime captures only; no GPU parity, whole-room or physical FPS acceptance");
    }
}
