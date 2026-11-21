package org.beckenburg;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Matched physical light-work A/B, not a synthetic FPS prediction.
 * Same authored maps, rooms, source factors, normal probes and real
 * architectural ray algorithm, with only the conservative source-room
 * reachability gate changed. Portal-flux approximation stays disabled.
 */
public final class PortalLightingWorkComparisonTest {
    private static final String GATE="beckenburg.lighting.portalGate.disabled";
    private static final String FLUX="beckenburg.lighting.portalFlux.disabled";
    private static void check(boolean yes,String why){
        if(!yes)throw new AssertionError(why);
    }
    private static FixtureLighting field(HouseMap map,boolean reject){
        var oldGate=System.getProperty(GATE);
        var oldFlux=System.getProperty(FLUX);
        try{
            System.setProperty(FLUX,"true"); // Compare identical direct-ray math.
            if(reject)System.clearProperty(GATE);
            else System.setProperty(GATE,"true");
            return new FixtureLighting(map);
        }finally{
            if(oldGate==null)System.clearProperty(GATE);
            else System.setProperty(GATE,oldGate);
            if(oldFlux==null)System.clearProperty(FLUX);
            else System.setProperty(FLUX,oldFlux);
        }
    }
    private static void probe(FixtureLighting direct,FixtureLighting optimized,
                              int room,double x,double y,double z,String context){
        var a=direct.sample(room,x,y,z),b=optimized.sample(room,x,y,z);
        check(Double.doubleToLongBits(a.r())==Double.doubleToLongBits(b.r())
            &&Double.doubleToLongBits(a.g())==Double.doubleToLongBits(b.g())
            &&Double.doubleToLongBits(a.b())==Double.doubleToLongBits(b.b()),
            context+" RGB mismatch at room="+room+" "+x+","+y+","+z
                +" direct="+a+" gated="+b);
        var na=direct.sampleNormal(room,x,y,z,0,1,0);
        var nb=optimized.sampleNormal(room,x,y,z,0,1,0);
        check(na.equals(nb),context+" directional shadow mismatch room="+room);
    }
    private static int cover(HouseMap map,FixtureLighting direct,
                             FixtureLighting optimized,String context){
        int count=0;
        for(int room=0;room<map.interiorRoomCount();room++){
            var r=map.level().room(room);
            double x0=r.x0()+.52,x1=r.x1()-.52;
            double z0=r.z0()+.52,z1=r.z1()-.52;
            for(double x:new double[]{x0,(r.x0()+r.x1())*.5,x1})
                for(double z:new double[]{z0,(r.z0()+r.z1())*.5,z1})
                    for(double height:new double[]{.42,1.3,2.22}){
                        double y=r.y()+Math.min(height,r.height()-.26);
                        probe(direct,optimized,room,x,y,z,context);
                        count++;
                    }
        }
        return count;
    }
    public static void main(String[] args)throws Exception{
        Path out=Path.of("build","e2e","portal-light-work");
        Files.createDirectories(out);
        StringBuilder csv=new StringBuilder(
            "site,doors,probes,direct_shadow_rays,direct_normal_rays,"
            +"gated_shadow_rays,gated_normal_rays,avoided_architectural_rays,"
            +"gated_source_candidates_rejected,source_room_pairs_pruned,"
            +"local_only_authored_sources,direct_source_candidates,"
            +"gated_source_candidates\n");
        long sumSaved=0,sumRejected=0;
        for(var site:new HouseMap.Site[]{
                HouseMap.Site.HOUSE,HouseMap.Site.FARMHOUSE,HouseMap.Site.SCHOOL}){
            for(boolean open:new boolean[]{false,true}){
                var map=new HouseMap(site);
                if(open)for(var portal:map.level().portals())
                    if(portal.door()&&!map.doorOpen(portal.id()))
                        map.toggleDoor(portal.id());
                var circuits=new RoomLighting();
                var direct=field(map,false);
                var gated=field(map,true);
                direct.frame(circuits,0);
                gated.frame(circuits,0);
                int tested=cover(map,direct,gated,site+" all_doors_open="+open);
                var exact=direct.frameStats();
                var faster=gated.frameStats();
                long rawRays=exact.shadowRays()+exact.normalRays();
                long gatedRays=faster.shadowRays()+faster.normalRays();
                long saved=rawRays-gatedRays;
                check(saved>=0,
                    "A pre-shadow source mask must never cause more architectural rays");
                check(tested>0&&exact.samples()==faster.samples()
                    &&exact.normalSamples()==faster.normalSamples(),
                    "The compared source and receiver probes are not matched");
                var graph=new PortalLightReachability(map,gated.sources());
                csv.append(String.format(Locale.ROOT,
                    "%s,%s,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d%n",
                    site,open,tested,exact.shadowRays(),exact.normalRays(),
                    faster.shadowRays(),faster.normalRays(),saved,
                    faster.portalRejectedCandidates(),
                    graph.sourceBlockedConnectedRooms(),graph.sourceConfined(),
                    exact.consideredSources(),faster.consideredSources()));
                sumSaved+=saved;sumRejected+=faster.portalRejectedCandidates();
                System.out.println("REAL PORTAL LIGHT WORK site="+site
                    +" all_doors_open="+open
                    +" matched_surface_probes="+tested
                    +" direct_shadow_rays="+rawRays
                    +" gated_shadow_rays="+gatedRays
                    +" avoided_shadow_rays="+saved
                    +" candidate_rejections="+faster.portalRejectedCandidates()
                    +" blocked_connected_source_room_pairs="
                        +graph.sourceBlockedConnectedRooms());
            }
        }
        Files.writeString(out.resolve("source-mask-ray-ab.csv"),csv.toString(),
            StandardCharsets.UTF_8);
        check(sumRejected>0,"No light source was excluded by the aperture mask");
        System.out.println("PASS original direct-ray RGB/normal parity on actual"
            +" maps and doors; removed "+sumSaved+" architectural rays and "
            +sumRejected+" source candidates from the matched cold samples");
    }
}
