package org.beckenburg;

import dev.mrcalzon.water.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Functional test: existing DEV room's sample geometry and real river socket. */
public final class DevInfrastructureShowcaseTest {
    private static void check(boolean value,String message){
        if(!value)throw new AssertionError(message);
    }
    public static void main(String[] args)throws Exception{
        var sample=DevInfrastructureShowcase.create();
        check(sample.railFaces()>100,"Real ballast, sleepers and two rails required");
        check(sample.roadFaces()>=12,"Grade and asphalt need separate physical triangles");
        check(sample.poleFaces()>100,"Multiple round poles, crossarms and sagging wires required");
        check(sample.riverFaces()>=8,"River banks and channel bed need sloped surfaces");
        check(sample.waterFaces()==4,"Two module water rectangles must form real triangles");
        check(sample.props().size()>=5,"Use real existing vegetation and rock assets");
        for(var prop:sample.props())
            check(Files.isRegularFile(Path.of("assets/runtime/models",prop.modelId()+".glb")),
                "Missing source GLB "+prop.modelId());
        var doc=sample.water();
        check(doc.placements().size()==2,"Both upstream and downstream water bodies present");
        var a=doc.placements().get(0),b=doc.placements().get(1);
        check(Math.abs(a.y()-b.y())<1e-8&&
            Math.abs((a.z()+a.depth()*.5)-(b.z()-b.depth()*.5))<1e-7,
            "Water socket must align adjacent modules with no seam or overlap");
        var socket=new WaterTerrainSnap.Socket("test-join",8.1,.075,
            a.z()+a.depth()*.5,0,1,a.width());
        var request=new WaterPlacement("new-module",8.1,.075,16.2,
            a.width(),2,0,a.settings(),WaterPlacement.Shape.SQUARE);
        var joined=WaterTerrainSnap.connect(request,socket,.4,false);
        check(joined!=null&&
            Math.abs(joined.water().z()-b.z())<1e-8,
            "Terrain socket must use actual WaterSnap matching and preserve placement");
        check(WaterTerrainSnap.connect(request.moved(5,0,0),socket,.4,false)==null,
            "Distant placement must NOT teleport or snap to unrelated terrain");
        check(WaterTerrainSnap.connect(request.moved(0,.5,0),socket,.4,false)==null,
            "Water height mismatch must reject incompatible socket");
        var editor=new WaterEditorModel(new WaterDocument("dev-infrastructure-demo",
            List.of(request)));
        editor.setTerrainSockets(List.of(socket));
        editor.select(List.of("new-module"));
        editor.beginGesture();
        var selectedMatch=editor.snapSelected(.4,false);
        editor.endGesture(false);
        check(selectedMatch!=null&&
            selectedMatch.targetId().equals("river-socket-test-join")&&
            Math.abs(editor.selected().get(0).z()-b.z())<1e-8,
            "Existing water editor snaps selected water to terrain sockets");
        check(editor.undo()&&editor.selected().get(0).equals(request),
            "Water editor undo restores unsnapped placement as one transaction");
        check(editor.redo()&&Math.abs(editor.selected().get(0).z()-b.z())<1e-8,
            "Water editor redo restores the exact river connection");

        Path folder=Files.createTempDirectory("dev-infrastructure-water");
        try {
            Path target=sample.exportWater(folder.resolve("water.water"));
            WaterDocument reloaded;
            try(var stream=Files.newInputStream(target)){reloaded=WaterDocument.read(stream);}
            check(reloaded.equals(doc),"Actual water module document survives export and reload");
            check(WorldWater.build(reloaded).bodies().size()==2,
                "Water placement geometries build into actual native game water module");
        }finally{
            try(var walk=Files.walk(folder)){
                for(var file:walk.sorted(Comparator.reverseOrder()).toList())
                    Files.deleteIfExists(file);
            }
        }
        System.out.println("PASS real DEV rail/road/poles/river GLBs and water module socket, rollback-safe native readback");
    }
}
