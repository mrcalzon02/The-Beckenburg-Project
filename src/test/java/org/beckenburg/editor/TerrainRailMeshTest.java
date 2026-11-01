package org.beckenburg.editor;

import java.nio.file.*;
import java.util.*;
/** Real geometry, verified through existing editor OBJ exporter and importer. */
public final class TerrainRailMeshTest {
    private static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception{
        Path root=Files.createTempDirectory("real-rail-mesh");
        try{
            var settings=new StitchedHeightmapWorld.Settings(32,2,0,0,-200,1000,32L,4);
            var world=StitchedHeightmapWorld.create(root.resolve("world"),settings);
            var alignment=new TerrainInfrastructureSplines.Alignment("rail-sample",
                TerrainInfrastructureSplines.Kind.RAIL,
                List.of(new TerrainLayerStack.Point(4,10,15),
                    new TerrainLayerStack.Point(27,10,15.35)),1.55,3.0,4.0);
            var out=TerrainRailMesh.build(settings,alignment,
                TerrainRailMesh.Profile.standard());
            check(out.mesh().triangles().size()>100,"Generated nontrivial geometry");
            check(out.sleepers()>20,"Actual repeated sleeper bodies emitted");
            check(out.lengthMetres()>40,"Path length uses physical world metres");
            Set<String> parts=new HashSet<>();
            double minY=Double.POSITIVE_INFINITY,maxY=Double.NEGATIVE_INFINITY;
            for(var tri:out.mesh().triangles()){
                parts.add(tri.part());
                for(var v:new EditorMeshPipeline.Vertex[]{tri.a(),tri.b(),tri.c()}){
                    minY=Math.min(minY,v.y());maxY=Math.max(maxY,v.y());
                    check(Double.isFinite(v.x())&&Double.isFinite(v.y())&&
                        Double.isFinite(v.z()),"All vertex positions are finite");
                }
            }
            check(parts.equals(Set.of("rail_ballast","rail_left","rail_right","rail_sleepers")),
                "Distinct modeled ballast, two rails, and sleepers");
            check(maxY-minY>.45,"Raised rails and finite-depth ballast are 3D, not sheets");
            // A prism writes its top pair, bottom pair, then its four
            // walls (two triangles each). Elevation changes along the grade,
            // so global Y thresholds misclassify some bottom caps as tops.
            var faceOrdinal=new HashMap<String,Integer>();
            var topVerified=new HashSet<String>();
            var bottomVerified=new HashSet<String>();
            for(var face:out.mesh().triangles()){
                var a=face.a();var b=face.b();var c=face.c();
                double nx=(b.y()-a.y())*(c.z()-a.z())-(b.z()-a.z())*(c.y()-a.y());
                double ny=(b.z()-a.z())*(c.x()-a.x())-(b.x()-a.x())*(c.z()-a.z());
                double nz=(b.x()-a.x())*(c.y()-a.y())-(b.y()-a.y())*(c.x()-a.x());
                check(nx*nx+ny*ny+nz*nz>1e-14,
                    "No zero-area railway triangle may enter renderer or physics");
                int ordinal=faceOrdinal.merge(face.part(),1,Integer::sum)-1;
                if(ordinal%12<2){
                    check(ny>0,"Top surface must face upward on "+face.part());
                    topVerified.add(face.part());
                }else if(ordinal%12<4){
                    check(ny<0,"Bottom surface must face downward on "+face.part());
                    bottomVerified.add(face.part());
                }
            }
            check(topVerified.equals(parts)&&bottomVerified.equals(parts),
                "Every physical ballast, sleeper and steel rail section has oriented top and bottom faces");
            Path file=root.resolve("track.obj");
            TerrainRailMesh.export(world,alignment,TerrainRailMesh.Profile.standard(),file);
            var imported=EditorMeshPipeline.read(file);
            check(imported.triangles().size()==out.mesh().triangles().size(),
                "Uses the actual editor mesh importer and exporter successfully");
            check(imported.triangles().stream().anyMatch(t->t.part().equals("rail_sleepers")),
                "Exported OBJ retains group identity");
            var road=new TerrainInfrastructureSplines.Alignment("road",
                TerrainInfrastructureSplines.Kind.ROAD,alignment.controls(),2,2,12);
            boolean rejected=false;
            try{TerrainRailMesh.build(settings,road,TerrainRailMesh.Profile.standard());}
            catch(IllegalArgumentException expected){rejected=true;}
            check(rejected,"Cannot export decorative rails from unrelated roads");
            var curved=new TerrainInfrastructureSplines.Alignment("tight-curve",
                TerrainInfrastructureSplines.Kind.RAIL,
                List.of(new TerrainLayerStack.Point(2,2,20),
                    new TerrainLayerStack.Point(7,2,20),
                    new TerrainLayerStack.Point(7,7,20),
                    new TerrainLayerStack.Point(12,7,20)),1.55,3,4);
            var strict=new TerrainRailMesh.Profile(1.435,2.65,3.35,.4,
                2.45,.23,.15,.65,.075,.18,10000);
            rejected=false;
            try{TerrainRailMesh.build(settings,curved,strict);}
            catch(IllegalArgumentException expected){rejected=true;}
            check(rejected,"Curvature guard rejects tighter-than-authorized railway");
            check(world.tiles().isEmpty(),"Rail mesh export does not create terrain source tiles");
            System.out.println("PASS real ballast rails and sleeper geometry, curvature, OBJ roundtrip, no terrain mutation");
        }finally {
            try(var walk=Files.walk(root)){
                for(Path path:walk.sorted(Comparator.reverseOrder()).toList())
                    Files.deleteIfExists(path);
            }
        }
    }
}
