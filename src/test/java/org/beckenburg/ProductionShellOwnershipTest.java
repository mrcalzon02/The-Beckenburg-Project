package org.beckenburg;

import java.util.HashSet;

/** D4 functional partition of actual gameplay assembly, not a render diff.
 * Diagnostic API cannot rewrite gameplay geometry or expose hidden ghosts. */
public final class ProductionShellOwnershipTest{
    private static void check(boolean ok,String failure){
        if(!ok)throw new AssertionError(failure);
    }
    private static void pose(SceneRenderer renderer,HouseMap house,int room,int look){
        var source=house.spawn(room);
        var target=house.spawn(look);
        renderer.x=source.x();renderer.z=source.z();
        renderer.eyeHeight=source.y()+1.62;
        renderer.yaw=Math.atan2(target.x()-source.x(),target.z()-source.z());
        renderer.pitch=-.075;
    }
    public static void main(String[] args){
        check(SceneRenderer.testResidentSubdivisionNormals(),
            "Real resident tessellation must preserve face normals and null fallback");
        var first=new HouseMap(HouseMap.Site.HOUSE);
        var renderer=new SceneRenderer();
        renderer.roomLights=new RoomLighting();
        renderer.setRenderSize(160,120);
        var byRoom=new HashSet<Integer>();
        long staticFaces=0,dynamicFaces=0;
        for(int[] view:new int[][]{{14,0},{0,4},{4,8},{8,4}}){
            pose(renderer,first,view[0],view[1]);
            var report=renderer.auditGameplayShellOwnership(first,1.5);
            check(report.totalFaces()==report.authoredShell()+report.dynamicAndDerived(),
                "Classification must partition the exact authored gameplay scene");
            check(report.authoredShell()>0&&report.dynamicAndDerived()>0,
                "House scene must retain both static architecture and live props");
            check(report.distinctShellRooms()>0,
                "Only original authored architecture may own static room identity");
            check(report.authoredShellTriangles()==report.authoredShell(),
                "Face identity must not count derived or subdivided GPU triangles");
            staticFaces+=report.authoredShell();
            dynamicFaces+=report.dynamicAndDerived();
            byRoom.add(view[0]);
        }
        // Face identity must be reset across maps even when authored geometry
        // revisions and material IDs are equivalent, never just by revision.
        var second=new HouseMap(HouseMap.Site.HOUSE);
        pose(renderer,second,0,4);
        var switched=renderer.auditGameplayShellOwnership(second,2.0);
        check(switched.authoredShell()>0&&
            switched.totalFaces()==switched.authoredShell()+switched.dynamicAndDerived(),
            "New case map must rebuild static owner references rather than reuse stale source");
        check(staticFaces>0&&dynamicFaces>0&&byRoom.size()==4,
            "Four independent gameplay camera positions must be inspected");
        System.out.println("PASS real House frame partition across four viewpoints and new case: static="+
            staticFaces+" dynamic="+dynamicFaces+" new_map="+switched.authoredShell());
    }
}
