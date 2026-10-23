package org.beckenburg;
import java.util.*;
import org.beckenburg.LevelDefinition.Vec;
/** Physical optics, surface depth, authentic traces and volume intersections. */
public final class OpticalEvidenceTest {
    static void check(boolean good,String why){if(!good)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception{
        var scene=new SceneRenderer();double base=scene.lampBeam(4,0),previous=base;
        for(var tier:EquipmentTiers.Tier.values()){
            var spec=HandheldLighting.flashlight(tier);scene.setLampHardware(tier,false);
            check(scene.lampBeam(4,0)>previous,"each paid tier really improves mid-range illumination");previous=scene.lampBeam(4,0);
            check(spec.reach()>HandheldLighting.DEFAULT.reach()&&spec.width()>HandheldLighting.DEFAULT.width(),"paid torch must beat default reach/width");
            check(EquipmentStoreDescriptions.describe("FLASHLIGHT",tier).specificationLine().contains(""+spec.reach()),"store reach matches actual hardware");
        }
        scene.lamp=false;check(scene.lampBeam(2,0)==0,"switch off kills beam");
        for(int normal:new int[]{-3,-2,-1,1,2,3}){
            var mark=new UvEvidenceMarks.Mark(0,0,4,5,6,10,normal);var cube=UvSurfaceDecal.cube(mark);int axis=Math.abs(normal)-1;
            for(int i=0;i<8;i++){
                var p=cube.get(i);double value=axis==0?p.x():axis==1?p.y():p.z(),anchor=axis==0?4:axis==1?5:6;
                check(Math.abs(value-(anchor-(i<4?Math.signum(normal)*UvSurfaceDecal.DEPTH:0)))<1e-8,"cube attached and outward face correctly oriented");
            }
            check(UvEvidenceMarks.Mark.decode(LanProtocol.parts(mark.frame())).equals(mark),"surface normal survives host frame");
        }
        var image=UvSurfaceDecal.texture();int opaque=0,clear=0;
        for(int y=0;y<64;y++)for(int x=0;x<64;x++)if((image.getRGB(x,y)>>>24)==0)clear++;else opaque++;
        check(opaque>100&&clear>100,"actual UV texture has irregular splats and transparent background");
        var map=BlackpineLevel.map();double floor=map.floorY(0);var traces=new UvEvidenceMarks();
        var body=new LanWorld.Body(48.5,floor,32.3,0,0);
        check(traces.visible(map,body,EquipmentTiers.Tier.III,10,true).isEmpty(),"a UV beam cannot invent evidence");
        traces.leave(map,new LanGhostSpatial.Pose(0,48.5,floor,34,0),10);
        check(traces.visible(map,body,EquipmentTiers.Tier.I,11,true).size()==1,"real supported ghost passage leaves a visible trace");
        check(traces.visible(map,body,EquipmentTiers.Tier.I,11,false).isEmpty(),"unpowered UV reveals nothing");
        check(traces.visible(map,new LanWorld.Body(body.x(),floor,body.z(),180,0),EquipmentTiers.Tier.I,11,true).isEmpty(),"wrong-facing UV cannot reveal behind the hunter");
        check(traces.visible(map,body,EquipmentTiers.Tier.I,111,true).isEmpty()&&traces.visible(map,body,EquipmentTiers.Tier.II,111,true).size()==1,"tier sensitivity extends genuine residue, not false positives");
        var contact=new StructuredLightVolume.Contact(1,0,0,0,2,10);
        var beam=new StructuredLightVolume.Beam(new Vec(0,1,0),new Vec(0,1,4));
        var interval=StructuredLightVolume.intersection(beam,contact);check(interval!=null&&interval[0]>.4&&interval[1]<.6,"intersection is confined to capsule");
        var bent=StructuredLightVolume.distort(beam,contact,0);check(bent.size()>10,"a real intersection produces spiral segments");
        check(bent.get(0).a().equals(beam.a())&&bent.get(bent.size()-1).b().equals(beam.b()),"beam endpoints remain fixed");
        check(StructuredLightVolume.distort(beam,new StructuredLightVolume.Contact(1,0,2,0,2,10),0).size()==1,"no reaction without geometric intersection");
        DotsRayFieldTest.main(new String[0]);
        check(StructuredLightVolume.radiusAt(.02)<StructuredLightVolume.radiusAt(.8),"capsule ends are rounded");
        var stations=new VanSensorStations();var installed=stations.placeOrRetrieve(0,"LASER_GRID",0,48.5,32.3,EquipmentTiers.Tier.I).placed();
        var rays=StructuredLightVolume.beams(map,installed);check(rays.size()==3,"Tier I has three physical laser rays");
        for(var ray:rays)check(map.clearLineOfSight(ray.a().x(),ray.a().y(),ray.a().z(),ray.b().x(),ray.b().y(),ray.b().z()),"beam clips at real walls/props");
        System.out.println("PASS paid/default beam progression, UV cube six-normal mounting and texture, authentic fading traces, ray-projected DOTS, exact laser intersections/spirals and wall clipping");
    }
}
