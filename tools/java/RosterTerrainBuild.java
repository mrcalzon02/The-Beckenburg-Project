package org.beckenburg;

import org.beckenburg.editor.*;
import java.nio.file.*;
import java.util.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.security.MessageDigest;

/** Offline authoring through the editor's actual persistent punched topology. */
public final class RosterTerrainBuild {
    private static Path inputRoot=Path.of("build/terrain-samples");
    private static Path outputRoot=Path.of("development/levels/rebuild");
    private static Path previewRoot=Path.of("development/levels/rebuild/previews");
    private static boolean cutOnlyGrades;
    private static String hash(Path file)throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));
    }
    private static String hashText(String value)throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }
    /** Sculpt through the existing lower-only editor brush. No source image
     * writes, private layer mutation, new runtime behavior or height filling. */
    private static void cutOnlyGrade(TerrainLayerStack layers,List<TerrainLayerStack.Point> points,double halfWidth,double shoulder){
        int n=layers.cells();double radius=halfWidth+shoulder;int changed=0;
        for(int z=1;z<n;z++)for(int x=1;x<n;x++){
            double distance=Double.POSITIVE_INFINITY,target=0;
            for(int i=1;i<points.size();i++){
                var a=points.get(i-1);var b=points.get(i);double dx=b.x()-a.x(),dz=b.z()-a.z(),ls=dx*dx+dz*dz;
                if(ls<1e-9)continue;
                double t=Math.max(0,Math.min(1,((x-a.x())*dx+(z-a.z())*dz)/ls));double d=Math.hypot(x-a.x()-t*dx,z-a.z()-t*dz);
                if(d<distance){distance=d;target=a.elevation()+(b.elevation()-a.elevation())*t;}
            }
            if(distance>=radius)continue;
            double blend=distance<=halfWidth?1:shoulder==0?0:(radius-distance)/shoulder;
            blend=blend*blend*(3-2*blend);double before=layers.height(x,z),desired=before+(Math.min(before,target)-before)*blend;
            if(desired<before){layers.brush(TerrainLayerStack.Brush.FLATTEN,x,z,.49,1,desired,0,1,TerrainLayerStack.FlattenLimit.LOWER_ONLY);changed++;}
            if(layers.height(x,z)>before+1e-6)throw new IllegalStateException("Cut-only bench raised source landform");
        }
        layers.finishStroke();System.out.println("EDITOR_LOWER_ONLY_BENCH changed="+changed+" raised=0");
    }
    public static void main(String[] args)throws Exception {
        var sites=new ArrayList<String>();
        for(int i=0;i<args.length;i++)switch(args[i]) {
            case "--input-root" -> inputRoot=Path.of(args[++i]);
            case "--output-root" -> outputRoot=Path.of(args[++i]);
            case "--preview-root" -> previewRoot=Path.of(args[++i]);
            case "--cut-only-grades" -> cutOnlyGrades=true;
            default -> sites.add(args[i]);
        }
        if(sites.isEmpty())throw new IllegalArgumentException("Select explicit terrain sites");
        Files.createDirectories(previewRoot);
        for(String site:sites) build(site.toLowerCase(Locale.ROOT));
    }
    private static void build(String site)throws Exception {
        Path input=inputRoot.resolve(site);
        Path root=outputRoot.resolve(site).resolve("terrain-world");
        String[] p=Files.readString(input.resolve("settings.tsv")).strip().split("\t");
        double x0=Double.parseDouble(p[0]),z0=Double.parseDouble(p[1]),x1=Double.parseDouble(p[2]),z1=Double.parseDouble(p[3]);
        var bounds=new TerrainEntranceMesh.Bounds(x0,z0,x1,z1);
        var settings=new StitchedHeightmapWorld.Settings(64,2,x0,z0,-32,32,20261107,4);
        var world=Files.exists(root.resolve("world.properties"))?StitchedHeightmapWorld.open(root):StitchedHeightmapWorld.create(root,settings);
        if(!world.settings().equals(settings))throw new IllegalStateException("Saved terrain settings changed: "+site);
        var heights=Files.readAllLines(input.resolve("heights.txt"));
        if(heights.size()!=65*65)throw new IllegalStateException("Missing source samples");
        var source=new BufferedImage(65,65,BufferedImage.TYPE_USHORT_GRAY);
        for(int z=0;z<65;z++)for(int x=0;x<65;x++) {
            double y=Double.parseDouble(heights.get(z*65+x));
            if(!Double.isFinite(y)||y< -32||y>32)throw new IllegalStateException("Out-of-domain source height");
            source.getRaster().setSample(x,z,0,(int)Math.round((y+32)/64*65535));
        }
        if(!Files.exists(world.tilePath(0,0))) {
            Path stage=input.resolve("source.png");ImageIO.write(source,"png",stage.toFile());
            world.importTile(0,0,stage,false);
        } else {
            var saved=ImageIO.read(world.tilePath(0,0).toFile());
            for(int z=0;z<65;z++)for(int x=0;x<65;x++)
                if(saved.getRaster().getSample(x,z,0)!=source.getRaster().getSample(x,z,0))
                    throw new IllegalStateException("Refusing to replace authored source heights: "+site);
        }
        String heightBefore=hash(world.tilePath(0,0));
        Path grades=input.resolve("grades.tsv"),gradeMarker=root.resolve("authored-path-grading.sha256");
        if(Files.exists(grades)&&!Files.readString(grades).isBlank()) {
            var layers=new TerrainLayerStack(world,0,0);String program=cutOnlyGrades?hashText("LOWER_ONLY\n"+Files.readString(grades)):hash(grades);
            var requested=Files.readAllLines(grades).stream().filter(row->!row.isBlank()).toList();
            var existing=new HashSet<String>();for(var layer:layers.layers())existing.add(layer.name());
            boolean apply=true;
            if(Files.exists(gradeMarker)) {
                var marker=Files.readAllLines(gradeMarker);
                if(marker.size()!=2||!marker.get(1).equals(hash(layers.path())))
                    throw new IllegalStateException("Authored path grading changed; explicit layer reconciliation required");
                apply=!marker.get(0).equals(program);
                if(apply) {
                    var oldRows=requested.stream().filter(row->existing.contains(row.split("\t")[0])).toList();
                    if(!marker.get(0).equals(hashText((cutOnlyGrades?"LOWER_ONLY\n":"")+String.join("\n",oldRows)+"\n")))
                        throw new IllegalStateException("Existing grade definition changed; preserve layer and reconcile explicitly");
                }
            } else for(String row:requested)if(existing.contains(row.split("\t")[0]))
                throw new IllegalStateException("Grade name collides with an existing untracked sculpt layer");
            if(apply) {
                var created=new HashMap<String,Integer>();
                for(String row:requested) {
                    String[] v=row.split("\t");if(existing.contains(v[0]))continue;
                    Integer selected=created.get(v[0]);
                    if(selected==null){selected=layers.addLayer(v[0]);created.put(v[0],selected);}
                    layers.selectLayer(selected);var points=new ArrayList<TerrainLayerStack.Point>();
                    for(int i=3;i<v.length;i+=3)points.add(new TerrainLayerStack.Point(
                        (Double.parseDouble(v[i])-x0)/2,(Double.parseDouble(v[i+1])-z0)/2,Double.parseDouble(v[i+2])));
                    if(cutOnlyGrades)cutOnlyGrade(layers,points,Double.parseDouble(v[1])/2,Double.parseDouble(v[2])/2);
                    else layers.gradePath(points,Double.parseDouble(v[1])/2,Double.parseDouble(v[2])/2);
                }
                layers.save();Files.writeString(gradeMarker,program+"\n"+hash(layers.path())+"\n");
            }
            if(!heightBefore.equals(hash(world.tilePath(0,0))))throw new IllegalStateException("Non-destructive grading changed source PNG");
            System.out.println("SAVED_EDITOR_PATH_GRADE "+site+" source PNG unchanged; layer document "+hash(layers.path()));
        }
        var topology=AdaptiveTerrainTopology.open(world);int before=topology.cutoutCount();
        var cuts=new ArrayList<TerrainEntranceMesh.Bounds>();
        for(String row:Files.readAllLines(input.resolve("cuts.tsv"))) {
            if(row.isBlank())continue;String[] v=row.split("\t");
            var cut=new TerrainEntranceMesh.Bounds(Double.parseDouble(v[0]),Double.parseDouble(v[1]),Double.parseDouble(v[2]),Double.parseDouble(v[3]));
            TerrainFootprintCutout.punch(topology,cut);cuts.add(cut);
        }
        topology.save();
        if(!heightBefore.equals(hash(world.tilePath(0,0))))throw new IllegalStateException("Punch changed source heights");
        var reopened=StitchedHeightmapWorld.open(root);
        var diskTopology=AdaptiveTerrainTopology.open(reopened);
        if(!diskTopology.snapshot().equals(topology.snapshot())||topology.cutoutCount()==0)
            throw new IllegalStateException("Punched topology did not survive disk reopen");
        var scene=new TerrainScenePackage(reopened).openScene();
        var publishedFiles=new ArrayList<Path>();
        if(Math.ceil((x1-x0)/2)*Math.ceil((z1-z0)/2)<=TerrainEntranceMesh.MAX_SOURCE_CELLS) {
            publishedFiles.add(TerrainScenePackage.publish(reopened,scene,"site",bounds).geometryFile());
        } else {
            // Real editor chunk publication, not a raised global export cap.
            // Chunk edges align to source cells; the saved topology controls
            // all shared-edge vertices in every chunk.
            for(double z=z0;z<z1;z+=64)for(double x=x0;x<x1;x+=64) {
                String chunk="site-"+(int)((x-x0)/64)+"-"+(int)((z-z0)/64);
                var cb=new TerrainEntranceMesh.Bounds(x,z,Math.min(x+64,x1),Math.min(z+64,z1));
                publishedFiles.add(TerrainScenePackage.publish(reopened,scene,chunk,cb).geometryFile());
            }
        }
        // Reopen both documents and inspect the actual published scene/OBJ.
        var diskWorld=StitchedHeightmapWorld.open(root);
        var diskScene=new TerrainScenePackage(diskWorld).openScene();
        if(!TerrainScenePackage.inspect(diskWorld,diskScene).isEmpty())throw new IllegalStateException("Stale terrain scene");
        var loaded=TerrainSceneNativePhysics.load(diskWorld,diskScene);
        byte[] nativeBytes=TerrainSceneNativePhysics.encode(diskWorld,diskScene);
        Files.write(root.resolve("terrain-physics.hcs2"),nativeBytes);
        int clear=0;
        for(var cut:cuts) {
            double x=(cut.x0()+cut.x1())/2,z=(cut.z0()+cut.z1())/2;
            if(!loaded.physics().clearSight(new LevelDefinition.Vec(x,24,z),new LevelDefinition.Vec(x,-24,z)))
                throw new IllegalStateException("Saved punch still has terrain optical/body surface: "+site+" "+cut);
            double sourceY=diskWorld.height(x,z);
            if(loaded.physics().bodyBlocked(new LevelDefinition.Vec(x,sourceY-.4,z),.8,.1))
                throw new IllegalStateException("Punched terrain still blocks the body: "+site+" "+cut);
            clear++;
        }
        var t=loaded.physics().triangle(0);double px=(t.a().x()+t.b().x()+t.c().x())/3,pz=(t.a().z()+t.b().z()+t.c().z())/3;
        if(loaded.physics().clearSight(new LevelDefinition.Vec(px,24,pz),new LevelDefinition.Vec(px,-24,pz)))
            throw new IllegalStateException("Unpunched terrain negative lost geometry");
        double py=(t.a().y()+t.b().y()+t.c().y())/3;
        if(!loaded.physics().bodyBlocked(new LevelDefinition.Vec(px,py-.4,pz),.8,.1))
            throw new IllegalStateException("Unpunched terrain body negative lost collision");
        var image=new BufferedImage(1000,1000,BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();
        g.setColor(new java.awt.Color(0x162028));g.fillRect(0,0,1000,1000);
        double scale=Math.min(880/(x1-x0),830/(z1-z0));
        for(int i=0;i<loaded.physics().triangleCount();i++) {
            var tri=loaded.physics().triangle(i);var vs=List.of(tri.a(),tri.b(),tri.c());int[] xx=new int[3],zz=new int[3];
            for(int j=0;j<3;j++){xx[j]=(int)(50+(vs.get(j).x()-x0)*scale);zz[j]=(int)(100+(vs.get(j).z()-z0)*scale);}
            g.setColor(new java.awt.Color(0x708767));g.fillPolygon(xx,zz,3);g.setColor(new java.awt.Color(0x9fae92));g.drawPolygon(xx,zz,3);
        }
        g.setFont(new java.awt.Font("SansSerif",1,18));g.setColor(java.awt.Color.WHITE);g.drawString(site.toUpperCase(Locale.ROOT)+" / SAVED EDITOR TERRAIN / PUNCHED GEOMETRY",30,32);
        g.setFont(new java.awt.Font("SansSerif",0,15));g.drawString(topology.cutoutCount()+" saved punched leaves; "+loaded.triangles()+" actual shared triangles; "+clear+" clear hole probes",30,60);
        g.drawString("Native terrain inspection. Retaining walls, rims, guards and runtime location acceptance remain open.",30,965);g.dispose();
        Path preview=previewRoot.resolve(site+"-saved-terrain.png");ImageIO.write(image,"png",preview.toFile());
        StringBuilder objHashes=new StringBuilder();for(Path file:publishedFiles)objHashes.append(hash(file)).append('\n');
        String geometryHash=publishedFiles.size()==1?hash(publishedFiles.get(0)):hashText(objHashes.toString());
        String report=String.format(Locale.ROOT,"{\n  \"site\":\"%s\",\n  \"cutout_leaves\":%d,\n  \"new_cutouts\":%d,\n  \"clear_hole_probes\":%d,\n  \"triangles\":%d,\n  \"source_height_sha256\":\"%s\",\n  \"physics_sha256\":\"%s\",\n  \"obj_sha256\":\"%s\",\n  \"source_heights_unchanged_by_punch\":true,\n  \"disk_reopen_verified\":true,\n  \"unpunched_optical_negative_verified\":true,\n  \"status\":\"SAVED_EDITOR_TERRAIN_CANDIDATE_NOT_LOCATION_ACCEPTED\"\n}\n",site,topology.cutoutCount(),topology.cutoutCount()-before,clear,loaded.triangles(),heightBefore,hash(root.resolve("terrain-physics.hcs2")),geometryHash);
        if(publishedFiles.size()>1)report=report.replace("\"obj_sha256\":", "\"obj_chunks\":"+publishedFiles.size()+",\n  \"obj_sha256_semantics\":\"SHA256 of ordered chunk SHA256 values with LF\",\n  \"obj_sha256\":");
        report=report.replace("\"unpunched_optical_negative_verified\":true,", "\"unpunched_optical_negative_verified\":true,\n  \"punched_body_clear_and_unpunched_body_blocked\":true,");
        Files.writeString(root.resolve("verification.json"),report);
        Files.copy(input.resolve("provenance.json"),root.resolve("source-provenance.json"),StandardCopyOption.REPLACE_EXISTING);
        Files.copy(input.resolve("cuts.tsv"),root.resolve("authored-cuts.tsv"),StandardCopyOption.REPLACE_EXISTING);
        System.out.println("SAVED_EDITOR_TERRAIN "+site+" "+topology.cutoutCount()+" cuts; "+clear+" clear probes; "+loaded.triangles()+" shared faces");
    }
}
