import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.beckenburg.TerrainSceneNativePhysics;
import org.beckenburg.editor.AdaptiveTerrainTopology;
import org.beckenburg.editor.StitchedHeightmapWorld;
import org.beckenburg.editor.TerrainEntranceMesh;
import org.beckenburg.editor.TerrainLayerStack;
import org.beckenburg.editor.TerrainScenePackage;

/** Fit the saved forecourt formation under the full stone porch. */
public final class HousePorchFormation {
    private static String hash(byte[] bytes)throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    public static void main(String[] args)throws Exception {
        var root=Path.of(args[0]);
        var world=StitchedHeightmapWorld.open(root);
        var topo=root.resolve("adaptive-topology.bin");
        String tileHash=hash(Files.readAllBytes(world.tilePath(0,0)));
        String topologyHash=hash(Files.readAllBytes(topo));
        var shape=AdaptiveTerrainTopology.open(world);
        int cutouts=shape.cutoutCount();
        var layers=new TerrainLayerStack(world,0,0);
        String name="House porch foundation formation";
        for(int i=0;i<layers.layerCount();i++){
            if(layers.layers().get(i).name().equals(name))
                throw new IllegalArgumentException("Reopen a clean copy of the previous House world");
        }
        layers.selectLayer(layers.addLayer(name));
        // The original saved 4.80 m grid vertices bracket the full porch and
        // grade back into the surrounding front garden. No cutout is added.
        for(int z:new int[]{26,27})for(int x:new int[]{25,26}){
            double desired=z==26?-.036:-.012;
            layers.brush(TerrainLayerStack.Brush.FLATTEN,x,z,.49,1,desired,0,1,
                    TerrainLayerStack.FlattenLimit.BOTH);
        }
        layers.finishStroke();
        layers.save();
        var reopened=StitchedHeightmapWorld.open(root);
        var reviewed=new TerrainLayerStack(reopened,0,0);
        for(int z:new int[]{26,27})for(int x:new int[]{25,26}){
            if(Math.abs(reviewed.height(x,z)-(z==26?-.036:-.012))>1e-6)
                throw new AssertionError("Formation height did not survive reopen: "+x+","+z+" "+reviewed.height(x,z));
        }
        if(!tileHash.equals(hash(Files.readAllBytes(world.tilePath(0,0))))
                ||!topologyHash.equals(hash(Files.readAllBytes(topo)))
                ||AdaptiveTerrainTopology.open(reopened).cutoutCount()!=cutouts)
            throw new AssertionError("Previous source heights or cellar cutouts changed");
        var scene=new TerrainScenePackage(reopened).openScene();
        var export=TerrainScenePackage.publish(reopened,scene,"world",
                new TerrainEntranceMesh.Bounds(-39,-48,86,89));
        if(!TerrainScenePackage.inspect(reopened,new TerrainScenePackage(reopened).openScene()).isEmpty())
            throw new AssertionError("Saved porch formation is stale");
        byte[] bytes=TerrainSceneNativePhysics.encode(reopened,scene);
        Files.write(Path.of(args[1]),bytes);
        System.out.println("HOUSE PORCH TERRAIN "+hash(bytes)+" triangles "+export.triangles()
                +" preserved cellar cutouts "+cutouts+" source height "+tileHash+" topology "+topologyHash);
    }
}
