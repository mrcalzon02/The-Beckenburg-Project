package org.beckenburg.editor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Emits actual authored surface/cave-entrance geometry as bounded 3D triangles.
 * Reads the exact heightmap and adaptive leaves, using edgeBreaks so differently
 * subdivided neighbors share conforming edge vertices. Source files unchanged.
 *
 * Parts preserve distinct seam ownership; the OBJ writer never welds vertices
 * between triangles, even where separately authored borders occupy the same
 * physical coordinates. This is a static editor mesh, NOT a game renderer.
 */
public final class TerrainEntranceMesh {
    public static final int MAX_TRIANGLES=16000;
    public static final int MAX_SOURCE_CELLS=2048;
    public static final double MIN_SEGMENT=.125;
    private static final double EPS=1e-7;

    public record Bounds(double x0,double z0,double x1,double z1){
        public Bounds{
            if(!Double.isFinite(x0+z0+x1+z1)||x1<=x0||z1<=z0)
                throw new IllegalArgumentException("Invalid export bounds");
        }
    }
    public record Output(EditorMeshPipeline.Mesh mesh,int sourceLeaves,
                         int exposedWallSegments,int rampTriangles,
                         int tabTriangles,List<String> warnings){
        public Output { warnings=List.copyOf(warnings); }
    }

    private final StitchedHeightmapWorld world;
    private final AdaptiveTerrainTopology topology;
    private final LayeredTerrainDocument geometry;
    private final RampEntranceDocument entrances;
    private final EntranceSeamDocument seams;
    private final LayeredTerrainQueries physical;
    private final ArrayList<EditorMeshPipeline.Triangle> triangles=new ArrayList<>();
    private final ArrayList<String> warnings=new ArrayList<>();
    private int leaves,wallSegments,rampFaces,tabFaces;

    private TerrainEntranceMesh(StitchedHeightmapWorld world,
                                AdaptiveTerrainTopology topology,
                                LayeredTerrainDocument geometry,
                                RampEntranceDocument entrances,
                                EntranceSeamDocument seams){
        this.world=Objects.requireNonNull(world);
        this.topology=Objects.requireNonNull(topology);
        this.geometry=Objects.requireNonNull(geometry);
        this.entrances=Objects.requireNonNull(entrances);
        this.seams=Objects.requireNonNull(seams);
        physical=new LayeredTerrainQueries(world,topology,geometry.floors(),
            geometry.solids(),entrances.ramps(),seams);
    }

    public static Output build(StitchedHeightmapWorld world,Bounds bounds)
            throws IOException{
        return build(world,AdaptiveTerrainTopology.open(world),
            LayeredTerrainDocument.open(world),RampEntranceDocument.open(world),
            EntranceSeamDocument.open(world),bounds);
    }
    public static Output build(StitchedHeightmapWorld world,
                               AdaptiveTerrainTopology topology,
                               LayeredTerrainDocument geometry,
                               RampEntranceDocument entrances,
                               EntranceSeamDocument seams,Bounds bounds)
            throws IOException {
        var builder=new TerrainEntranceMesh(world,topology,geometry,entrances,seams);
        builder.generate(bounds);
        if(builder.triangles.isEmpty())
            throw new IOException("No actual terrain geometry within chosen export bounds");
        return new Output(new EditorMeshPipeline.Mesh("Authored Entrance Geometry",
            builder.triangles,0),builder.leaves,builder.wallSegments,
            builder.rampFaces,builder.tabFaces,builder.warnings);
    }
    /** Export a selected ramp and a little surrounding source terrain. */
    public static Bounds around(LayeredTerrainQueries.Ramp ramp,double margin){
        if(!Double.isFinite(margin)||margin<0||margin>64)
            throw new IllegalArgumentException("Invalid terrain margin");
        return new Bounds(ramp.x0()-margin,ramp.z0()-margin,
            ramp.x1()+margin,ramp.z1()+margin);
    }
    private void add(String part,double[] a,double[] b,double[] c){
        if(triangles.size()>=MAX_TRIANGLES)
            throw new IllegalArgumentException("Entrance mesh exceeds 16,000 triangles; export smaller area");
        // Reject only geometric zero-area triangles, not two points sharing X/Z
        // on vertical cave walls.
        double ux=b[0]-a[0],uy=b[1]-a[1],uz=b[2]-a[2],
               vx=c[0]-a[0],vy=c[1]-a[1],vz=c[2]-a[2];
        double nx=uy*vz-uz*vy,ny=uz*vx-ux*vz,nz=ux*vy-uy*vx;
        if(nx*nx+ny*ny+nz*nz<1e-15)return;
        var av=new EditorMeshPipeline.Vertex(a[0],a[1],a[2],a[0]*.25,a[2]*.25);
        var bv=new EditorMeshPipeline.Vertex(b[0],b[1],b[2],b[0]*.25,b[2]*.25);
        var cv=new EditorMeshPipeline.Vertex(c[0],c[1],c[2],c[0]*.25,c[2]*.25);
        triangles.add(new EditorMeshPipeline.Triangle(part,av,bv,cv));
    }
    private double worldX(long unit){
        return world.settings().originX()+unit*
            world.settings().metresPerCell()/AdaptiveTerrainTopology.UNITS_PER_CELL;
    }
    private double worldZ(long unit){
        return world.settings().originZ()+unit*
            world.settings().metresPerCell()/AdaptiveTerrainTopology.UNITS_PER_CELL;
    }
    private double heightAt(double x,double z)throws IOException {
        // Use the identical composed surface function as physical support and
        // navigation queries. No second renderer or independently recreated mesh.
        return physical.groundHeight(x,z);
    }
    private double[] surface(double x,double z)throws IOException {
        return new double[]{x,heightAt(x,z),z};
    }
    private static double[] vertex(double x,double y,double z){
        return new double[]{x,y,z};
    }
    private void generate(Bounds bounds)throws IOException {
        var settings=world.settings();
        double unit=settings.metresPerCell(),ox=settings.originX(),oz=settings.originZ();
        // Surface geometry exports whole source cells at bounds edges to avoid
        // clipping quadtree leaves and producing arbitrary new crack coordinates.
        long minX=(long)Math.floor((bounds.x0()-ox)/unit);
        long maxX=(long)Math.ceil((bounds.x1()-ox)/unit);
        long minZ=(long)Math.floor((bounds.z0()-oz)/unit);
        long maxZ=(long)Math.ceil((bounds.z1()-oz)/unit);
        if(maxX-minX>MAX_SOURCE_CELLS||maxZ-minZ>MAX_SOURCE_CELLS
            ||(maxX-minX)*(maxZ-minZ)>MAX_SOURCE_CELLS)
            throw new IllegalArgumentException("Too many source terrain cells; select smaller region");
        for(long cz=minZ;cz<maxZ;cz++)for(long cx=minX;cx<maxX;cx++){
            long tx=Math.floorDiv(cx,settings.tileCells());
            long tz=Math.floorDiv(cz,settings.tileCells());
            if(tx < -1_000_000||tx>1_000_000||tz < -1_000_000||tz>1_000_000
                ||!world.hasTile((int)tx,(int)tz))continue;
            for(var leaf:topology.leaves(cx,cz)){
                verifyConformed(leaf);
                leaves++;
                String part=part(leaf);
                if(!topology.isCutout(leaf) ||part.startsWith("tab_"))
                    fan(leaf,part);
                if(!topology.isCutout(leaf))
                    cutWalls(leaf);
            }
        }
        for(var floor:geometry.floors())floor(floor,bounds);
        for(var ramp:entrances.ramps())ramp(ramp,bounds);
        if(triangles.size()>0&&triangles.stream().noneMatch(t->t.part().startsWith("rock_edge")))
            warnings.add("No exposed ramp-edge walls in selected area; source cuts may be absent");
        if(entrances.ramps().isEmpty())
            warnings.add("No authored ramps found; export contains source/tab/floor geometry");
    }
    private void verifyConformed(AdaptiveTerrainTopology.Node leaf){
        double x0=worldX(leaf.x0()),z0=worldZ(leaf.z0());
        double x1=worldX(leaf.x1()),z1=worldZ(leaf.z1());
        for(var edge:seams.edges())if(edge.intersectsInterior(x0,z0,x1,z1))
            throw new IllegalStateException("Un-conformed entrance seam "+edge.id()
                +": run CONFORM LEAVES + PUNCH TABS before exporting 3D geometry");
        double mx=(x0+x1)*.5,mz=(z0+z1)*.5;
        for(var tab:seams.tabs())if(tab.contains(mx,mz)
            &&(!topology.isCutout(leaf)
                ||x0<tab.x0()-EPS||x1>tab.x1()+EPS
                ||z0<tab.z0()-EPS||z1>tab.z1()+EPS))
            throw new IllegalStateException("Detached tab "+tab.id()
                +" is not punched/isolated: run CONFORM LEAVES + PUNCH TABS");
    }
    private String part(AdaptiveTerrainTopology.Node leaf){
        double cx=(worldX(leaf.x0())+worldX(leaf.x1()))*.5;
        double cz=(worldZ(leaf.z0())+worldZ(leaf.z1()))*.5;
        for(var tab:seams.tabs())if(tab.contains(cx,cz))
            return "tab_"+tab.id();
        var section=new StringBuilder("surface");
        // Each side of a de-stitch or newly authored seam is a distinct part,
        // with separately emitted indices. Never weld a forbidden connection.
        for(var edge:seams.edges()){
            double pos=edge.vertical()?cx:cz;
            double line=edge.vertical()?edge.x0():edge.z0();
            double along=edge.vertical()?cz:cx;
            double a=edge.vertical()?Math.min(edge.z0(),edge.z1()):
                Math.min(edge.x0(),edge.x1());
            double b=edge.vertical()?Math.max(edge.z0(),edge.z1()):
                Math.max(edge.x0(),edge.x1());
            if(along>=a-EPS&&along<=b+EPS)
                section.append("_").append(edge.id()).append(pos<line?"_a":"_b");
        }
        return section.toString();
    }
    /** Emit a polygon fan from a real source height sample to all edge-break
     * vertices; coarse/fine leaf borders share exact terrain heights. */
    private void fan(AdaptiveTerrainTopology.Node leaf,String part)throws IOException {
        double x0=worldX(leaf.x0()),x1=worldX(leaf.x1()),
               z0=worldZ(leaf.z0()),z1=worldZ(leaf.z1());
        double[] center=surface((x0+x1)*.5,(z0+z1)*.5);
        var ring=new ArrayList<double[]>();
        // Clockwise in top-down X/Z: north edge W→E, east N→S,
        // south E→W, west S→N. Avoid duplicate corner entries.
        for(long x:topology.edgeBreaks(leaf,AdaptiveTerrainTopology.Edge.NORTH))
            ring.add(surface(worldX(x),z0));
        for(long z:topology.edgeBreaks(leaf,AdaptiveTerrainTopology.Edge.EAST))
            if(z!=leaf.z0())ring.add(surface(x1,worldZ(z)));
        var south=topology.edgeBreaks(leaf,AdaptiveTerrainTopology.Edge.SOUTH);
        for(int i=south.size()-2;i>=0;i--)ring.add(surface(worldX(south.get(i)),z1));
        var west=topology.edgeBreaks(leaf,AdaptiveTerrainTopology.Edge.WEST);
        for(int i=west.size()-2;i>0;i--)ring.add(surface(x0,worldZ(west.get(i))));
        for(int i=0;i<ring.size();i++)
            add(part,center,ring.get((i+1)%ring.size()),ring.get(i));
        if(part.startsWith("tab_"))tabFaces+=ring.size();
    }
    private void cutWalls(AdaptiveTerrainTopology.Node leaf)throws IOException {
        // A visible rock cheek is emitted only beside an actual cutout on an
        // authored ramp. Never fabricate an arbitrary-depth cliff or wall.
        for(var edge:AdaptiveTerrainTopology.Edge.values()){
            var intervals=topology.edgeBreaks(leaf,edge);
            for(int i=0;i<intervals.size()-1;i++){
                long a=intervals.get(i),b=intervals.get(i+1);
                double x0=edge==AdaptiveTerrainTopology.Edge.WEST?worldX(leaf.x0()):
                    edge==AdaptiveTerrainTopology.Edge.EAST?worldX(leaf.x1()):worldX(a);
                double x1=edge==AdaptiveTerrainTopology.Edge.WEST?worldX(leaf.x0()):
                    edge==AdaptiveTerrainTopology.Edge.EAST?worldX(leaf.x1()):worldX(b);
                double z0=edge==AdaptiveTerrainTopology.Edge.NORTH?worldZ(leaf.z0()):
                    edge==AdaptiveTerrainTopology.Edge.SOUTH?worldZ(leaf.z1()):worldZ(a);
                double z1=edge==AdaptiveTerrainTopology.Edge.NORTH?worldZ(leaf.z0()):
                    edge==AdaptiveTerrainTopology.Edge.SOUTH?worldZ(leaf.z1()):worldZ(b);
                double mx=(x0+x1)*.5,mz=(z0+z1)*.5;
                double shift=Math.min((world.settings().metresPerCell()/
                    AdaptiveTerrainTopology.UNITS_PER_CELL)*.2,.005);
                double px=mx,pz=mz;
                switch(edge){
                    case NORTH -> pz-=shift;
                    case SOUTH -> pz+=shift;
                    case EAST -> px+=shift;
                    case WEST -> px-=shift;
                }
                if(physical.support("surface",px,pz)!=null)continue;
                LayeredTerrainQueries.Ramp ramp=null;
                for(var candidate:entrances.ramps())if(candidate.contains(px,pz)){
                    ramp=candidate;break;
                }
                if(ramp==null)continue;
                double upper0=heightAt(x0,z0),upper1=heightAt(x1,z1);
                double lower0=ramp.height(x0,z0),lower1=ramp.height(x1,z1);
                if(upper0-lower0<.02&&upper1-lower1<.02)continue;
                double[] t0=vertex(x0,upper0,z0),t1=vertex(x1,upper1,z1);
                double[] b0=vertex(x0,Math.min(upper0,lower0),z0),
                         b1=vertex(x1,Math.min(upper1,lower1),z1);
                if(edge==AdaptiveTerrainTopology.Edge.NORTH
                    ||edge==AdaptiveTerrainTopology.Edge.EAST){
                    add("rock_edge_"+ramp.id(),t0,t1,b0);
                    add("rock_edge_"+ramp.id(),t1,b1,b0);
                }else{
                    add("rock_edge_"+ramp.id(),t0,b0,t1);
                    add("rock_edge_"+ramp.id(),t1,b0,b1);
                }
                wallSegments++;
            }
        }
    }
    private void floor(LayeredTerrainQueries.Floor f,Bounds bounds){
        double x0=Math.max(f.x0(),bounds.x0()),x1=Math.min(f.x1(),bounds.x1());
        double z0=Math.max(f.z0(),bounds.z0()),z1=Math.min(f.z1(),bounds.z1());
        if(x1<=x0||z1<=z0)return;
        int nx=Math.max(1,(int)Math.ceil((x1-x0)/.5));
        int nz=Math.max(1,(int)Math.ceil((z1-z0)/.5));
        if((long)nx*nz>4000)throw new IllegalArgumentException("Floor mesh region too large");
        for(int j=0;j<nz;j++)for(int i=0;i<nx;i++){
            double ax=x0+(x1-x0)*i/nx,bx=x0+(x1-x0)*(i+1)/nx,
                   az=z0+(z1-z0)*j/nz,bz=z0+(z1-z0)*(j+1)/nz;
            boolean underRamp=false;
            for(var r:entrances.ramps())if(r.targetLayer().equals(f.layer())
                &&r.contains((ax+bx)*.5,(az+bz)*.5)){
                underRamp=true;break;
            }
            if(underRamp)continue;
            double[] a=vertex(ax,f.height(ax,az),az);
            double[] b=vertex(bx,f.height(bx,az),az);
            double[] c=vertex(bx,f.height(bx,bz),bz);
            double[] d=vertex(ax,f.height(ax,bz),bz);
            add("floor_"+f.layer(),a,c,b);
            add("floor_"+f.layer(),a,d,c);
        }
    }
    private void ramp(LayeredTerrainQueries.Ramp r,Bounds bounds){
        if(r.x0()>=bounds.x1()||r.x1()<=bounds.x0()
            ||r.z0()>=bounds.z1()||r.z1()<=bounds.z0())return;
        // Ramp triangles use the *same Hermite height function* as nav collision.
        // Fixed maximum interval gives reproducible geometry and bounded count.
        int slices=(int)Math.ceil(r.length()/.125);
        if(slices>256)throw new IllegalArgumentException(
            "Ramp longer than 32 m; subdivide entrance for bounded mesh emission");
        for(var face:r.triangles(slices)){
            double[] a=vertex(face[0],face[1],face[2]);
            double[] b=vertex(face[3],face[4],face[5]);
            double[] c=vertex(face[6],face[7],face[8]);
            double upward=(b[2]-a[2])*(c[0]-a[0])
                -(b[0]-a[0])*(c[2]-a[2]);
            if(upward>=0)add("ramp_"+r.id(),a,b,c);
            else add("ramp_"+r.id(),a,c,b);
            rampFaces++;
        }
    }
    /** Geometry-only OBJ with stable groups, explicit UVs and no invented
     * textures. Distinct triangles have distinct vertex IDs so no importer
     * can implicitly weld across an authored destitch or detached tab. */
    public static void writeObj(EditorMeshPipeline.Mesh mesh,Path destination)throws IOException{
        Objects.requireNonNull(mesh);Objects.requireNonNull(destination);
        if(mesh.triangles().isEmpty()||mesh.triangles().size()>MAX_TRIANGLES)
            throw new IOException("Invalid entrance OBJ face budget");
        var text=new StringBuilder("# Beckenburg authored 3D entrance geometry\n");
        text.append("# Source terrain, seam ownership and ramp height retained.\n");
        int index=1;String part="";
        for(var face:mesh.triangles()){
            if(!part.equals(face.part())){
                part=face.part();
                text.append("g ").append(part.replaceAll("[^A-Za-z0-9_-]","_")).append('\n');
            }
            for(var v:new EditorMeshPipeline.Vertex[]{face.a(),face.b(),face.c()})
                text.append(String.format(Locale.ROOT,"v %.9f %.9f %.9f\n",
                    v.x(),v.y(),v.z()));
            for(var v:new EditorMeshPipeline.Vertex[]{face.a(),face.b(),face.c()})
                text.append(String.format(Locale.ROOT,"vt %.7f %.7f\n",v.u(),v.v()));
            text.append("f ").append(index).append('/').append(index).append(' ')
                .append(index+1).append('/').append(index+1).append(' ')
                .append(index+2).append('/').append(index+2).append('\n');
            index+=3;
        }
        Path folder=destination.toAbsolutePath().getParent();
        Files.createDirectories(folder);
        Path tmp=Files.createTempFile(folder,".entrance-mesh-",".obj");
        try{
            Files.writeString(tmp,text.toString(),StandardCharsets.UTF_8);
            // Verify that the exact exporter output parses through the editor's
            // real OBJ importer before publishing to the user-selected file.
            var read=EditorMeshPipeline.read(tmp);
            if(read.triangles().size()!=mesh.triangles().size())
                throw new IOException("Exported OBJ did not roundtrip face count");
            Files.move(tmp,destination,java.nio.file.StandardCopyOption.ATOMIC_MOVE,
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }finally{Files.deleteIfExists(tmp);}
    }
}
