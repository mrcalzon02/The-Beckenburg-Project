package org.beckenburg;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;
/** Same native Castle poses before/after a sky-only fix; actual depth and pixels. */
public final class CastleSkyMatchedReview {
    public static void main(String[] args)throws Exception{
        if(args[0].equals("capture")){
            System.setProperty("user.home",Files.createTempDirectory("castle-sky-review-").toString());
            Path out=Path.of(args[1]);Files.createDirectories(out);var map=new CastleHubMap();var r=new SceneRenderer();r.setRenderSize(320,200);r.setPsxPreset(PsxPipeline.Preset.CLASSIC);r.lamp=true;
            var lines=Files.readAllLines(Path.of("development/levels/rebuild/captures/castle-closeout/castle/castle-rooms.csv"));var headers=Arrays.asList(lines.get(0).split(","));
            for(String id:List.of("east-north-walk","east-bridge","east-south-walk","south-walk","west-walk","gallery")){
                String[] row=lines.stream().skip(1).map(l->l.split(",")).filter(a->a[0].equals(id)).findFirst().orElseThrow();
                r.x=Double.parseDouble(row[headers.indexOf("camera_x")]);r.z=Double.parseDouble(row[headers.indexOf("camera_z")]);double floor=Double.parseDouble(row[headers.indexOf("floor")]);r.eyeHeight=floor+1.62;r.yaw=id.equals("south-walk")?Math.PI:Math.PI/2;r.pitch=-.07;
                if(!map.traversable(r.x,r.z,floor))throw new AssertionError("Unsupported matched pose "+id);
                r.drawCastle(map,.5,true);if(r.weatherModule().sky().cloudCover()!=0)throw new AssertionError("Matched review requires actual clear weather");
                ImageIO.write(r.image(),"png",out.resolve(id+".png").toFile());
                try(var data=new DataOutputStream(Files.newOutputStream(out.resolve(id+".depth")))){for(double d:r.depths())data.writeDouble(d);}
            }
            System.out.println("PASS six matched native Castle clear-night views, actual depths and source poses");
        }else{
            Path before=Path.of(args[1]),after=Path.of(args[2]);int changed=0,geometryChanged=0;
            for(var file:Files.list(before).filter(p->p.toString().endsWith(".png")).toList()){
                String id=file.getFileName().toString().replace(".png","");var a=ImageIO.read(file.toFile());var b=ImageIO.read(after.resolve(file.getFileName()).toFile());
                byte[] oldDepth=Files.readAllBytes(before.resolve(id+".depth")),newDepth=Files.readAllBytes(after.resolve(id+".depth"));
                if(!Arrays.equals(oldDepth,newDepth))throw new AssertionError("Sky fix changes world depth "+id);
                double[] depth=new double[a.getWidth()*a.getHeight()];try(var in=new DataInputStream(new ByteArrayInputStream(newDepth))){for(int i=0;i<depth.length;i++)depth[i]=in.readDouble();}
                int skyChanges=0,solidChanges=0;
                for(int y=2;y<a.getHeight()-2;y++)for(int x=2;x<a.getWidth()-2;x++)if(a.getRGB(x,y)!=b.getRGB(x,y)){
                    if(!Double.isFinite(depth[y*a.getWidth()+x])){skyChanges++;continue;}
                    boolean interior=true;for(int dy=-2;dy<=2;dy++)for(int dx=-2;dx<=2;dx++)interior&=Double.isFinite(depth[(y+dy)*a.getWidth()+x+dx]);
                    if(interior)solidChanges++;
                }
                changed+=skyChanges;geometryChanged+=solidChanges;System.out.println(id+" identical_world_depth=true changed_sky_pixels="+skyChanges+" changed_solid_interior_pixels="+solidChanges);
            }
            if(changed==0||geometryChanged!=0)throw new AssertionError("Expected real sky correction without solid-interior pixel changes");
            System.out.println("PASS actual sky changes="+changed+"; world depth byte-identical; solid-interior changes=0");
        }
    }
}
