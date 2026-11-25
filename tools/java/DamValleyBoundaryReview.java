package org.beckenburg;

import org.beckenburg.LevelDefinition.Vec;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/** Verifies the built perimeter in the ordinary native body and movement kernel. */
public final class DamValleyBoundaryReview {
    private static final double[] POSTURES={HunterPosture.STANDING_HEIGHT,HunterPosture.CROUCH_HEIGHT};
    private DamValleyBoundaryReview() {}

    public static void main(String[] args)throws Exception {
        var folder=Path.of(args[0]);
        var nativePath=folder.resolve("bounded-dam.hcs2");
        var digest=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(nativePath)));
        var physics=AuthoredSitePhysics.read(Files.newInputStream(nativePath),digest,List.of());
        int bank=0,shore=0,gate=0,swept=0;
        var reports=new ArrayList<String>();
        for(var line:Files.readAllLines(folder.resolve("boundary-stations.tsv"))){
            var p=line.split("\t");
            var kind=p[0];
            double x=d(p,1),z=d(p,2),nx=d(p,3),nz=d(p,4),grade=d(p,5);
            for(double height:POSTURES){
                for(double offset:new double[]{.035,.58,1.02}){
                    require(physics.bodyBlocked(new Vec(x,grade+offset,z),height,.22),
                        "Open boundary at "+kind+" "+x+"/"+z+" y "+(grade+offset)+" height "+height);
                }
                var origin=support(physics,x-nx*.68,z-nz*.68,grade);
                var finish=support(physics,x+nx*.68,z+nz*.68,grade);
                if(origin!=null&&finish!=null&&physics.traversable(origin,height)&&physics.traversable(finish,height)){
                    require(physics.resolveMove(origin,finish.x(),finish.z(),height)==null
                        &&physics.resolveMove(finish,origin.x(),origin.z(),height)==null,
                        "Swept body crossed built boundary at "+kind+" "+x+"/"+z);
                    swept+=2;
                }
            }
            switch(kind){
                case "GATE" -> gate++;
                case "SHORE" -> shore++;
                case "BANK" -> bank++;
                default -> throw new AssertionError("Unknown station "+kind);
            }
        }
        require(gate==61&&bank+shore+gate>=640&&swept>400,
            "Too few physical perimeter controls "+bank+"/"+shore+"/"+gate+" swept "+swept);
        int road=0;
        for(var p:List.of(new Vec(36,0,87),new Vec(36,0,88.85),new Vec(37,.29,92.3),
                            new Vec(42,-.13,99),new Vec(45,-.11,111),
                            new Vec(49,-.39,123),new Vec(54,-.74,132))){
            var at=support(physics,p.x(),p.z(),p.y());
            require(at!=null&&physics.traversable(at,HunterPosture.STANDING_HEIGHT),"Unsupported road grade "+p);
            road++;
        }
        require(physics.traversable(new Vec(39.8,0,80.3),HunterPosture.STANDING_HEIGHT),
            "Arrival review did not use a supported floor");
        reports.add("check\tresult");
        reports.add("native_sha256\t"+digest);
        reports.add("bank_stations\t"+bank);
        reports.add("shore_stations\t"+shore);
        reports.add("closed_road_gate_stations\t"+gate);
        reports.add("grounded_and_elevated_capsule_blocks\t"+((gate+shore+bank)*6));
        reports.add("bidirectional_approaches_with_clear_feet_blocked\t"+swept);
        reports.add("supported_roadhead_and_exterior_grade_controls\t"+road);
        reports.add("standing_arrival_capture\tPASS");
        Files.writeString(folder.resolve("boundary-body-review.tsv"),String.join("\n",reports)+"\n");
        reports.forEach(System.out::println);
    }

    private static Vec support(AuthoredSitePhysics physics,double x,double z,double expected){
        var floor=physics.support(x,z,expected,.48);
        return floor==null?null:new Vec(x,floor.y(),z);
    }

    private static double d(String[] parts,int i){return Double.parseDouble(parts[i]);}
    private static void require(boolean value,String reason){if(!value)throw new AssertionError(reason);}
}
