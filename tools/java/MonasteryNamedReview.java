package org.beckenburg;

import org.beckenburg.LevelDefinition.Vec;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

/** Verifies that named dispatch uses the same archive authority as the portable scene. */
final class MonasteryNamedReview {
    private static final String RESOURCE = "/org/beckenburg/sites/wld-10/";

    private MonasteryNamedReview() {}

    public static void main(String[] args) throws Exception {
        Path stage = Path.of(args[0]);
        if (!List.of(args).contains("--gpu")) {
            review(stage, false);
            return;
        }
        var desktop = Class.forName("org.beckenburg.gpu.GpuDesktop");
        var start = desktop.getDeclaredMethod("startGpu");
        var stop = desktop.getDeclaredMethod("stopReviewGpu");
        start.setAccessible(true);
        stop.setAccessible(true);
        start.invoke(null);
        try { review(stage, true); } finally { stop.invoke(null); }
    }

    private static void review(Path stage, boolean gpu) throws Exception {
        var entry = MissionSites.byId("WLD-10");
        var location = entry.location();
        var site = HouseMap.Site.MONASTERY;
        System.clearProperty(MonasteryNativeLevel.REVIEW_PROPERTY);
        require(location == SiteCatalog.Location.MONASTERY && !entry.playable()
            && !List.of(SiteCatalog.soloLocations()).contains(location) && !SiteCatalog.playable(site),
            "Monastery escaped the default dispatch lock");
        try {
            SiteCatalog.requirePlayable(location);
            throw new AssertionError("Unapproved dispatch accepted");
        } catch (IllegalArgumentException expected) {
            require(expected.getMessage().contains("has no shipped level geometry"), expected.getMessage());
        }
        System.setProperty(MonasteryNativeLevel.REVIEW_PROPERTY, "true");
        require(entry.playable() && List.of(SiteCatalog.soloLocations()).contains(location)
            && SiteCatalog.mapFor(location) == site && SiteCatalog.playable(site),
            "Review property did not enable the named multiplayer and solo entries");
        require(MissionSites.title(site.name()).equals(MonasteryNativeLevel.TITLE), "Changed stable site title");
        var campaign = MissionSites.campaign().stream().map(MissionSites.Entry::id).toList();
        require(campaign.indexOf("WLD-10") == 18 && campaign.get(17).equals("WLD-09") && campaign.get(19).equals("WLD-11"),
            "Ordinary campaign order changed");
        for (var name : List.of("source-hashes.tsv","runtime-input.tsv","door-schedule.tsv","electrical-schedule.tsv",
                                "monastery-with-landscape.glb","monastery-with-landscape.hcs2")) {
            var resource = MonasteryNativeLevel.class.getResource(RESOURCE + name);
            require(resource != null && resource.getProtocol().equals("jar"), "Resource is outside the game archive " + name);
        }
        var source = MonasteryNativeLevel.packaged();
        var map = new HouseMap(site);
        var network = HouseMap.forNetworkRevision(site, map.level().revision());
        require(map.valid() && RitualSites.valid(map) && map.level().equals(source.level())
            && map.nativeInvestigation() == source.scene() && network.nativeInvestigation() == source.scene(),
            "The named local and network sites do not share the same packaged geometry");
        try {
            HouseMap.forNetworkRevision(site, "older-monastery");
            throw new AssertionError("Stale named revision was accepted");
        } catch (IllegalArgumentException expected) {
            require(expected.getMessage().equals("incompatible level revision"), expected.getMessage());
        }

        var paths = new LinkedHashMap<String,List<Vec>>();
        try (var stream = MonasteryNativeLevel.class.getResourceAsStream(RESOURCE + "runtime-input.tsv")) {
            for (var row : WharfNativeLevel.rows(stream)) {
                if (row[0].equals("POINT")) paths.computeIfAbsent(row[1], key -> new ArrayList<>())
                    .add(new Vec(Double.parseDouble(row[2]), Double.parseDouble(row[3]), Double.parseDouble(row[4])));
            }
        }
        var scene = new MonasteryDoorReview.Scene(source.level(), source.scene(), paths);
        for (var door : source.scene().doors()) {
            var frame = door.spec();
            require(NativeSchoolInteriorReview.sweep(source.scene().physics(),frame),"Packaged door lost sweep " + frame.frameId());
            NativeSchoolInteriorReview.audit(map,frame,false);
            map.toggleDoor(frame.portal().id(),System.nanoTime()-2_000_000_000L);
            NativeSchoolInteriorReview.audit(map,frame,true);
        }
        int walks = 0;
        for (var path : paths.entrySet()) {
            var back = new ArrayList<>(path.getValue());
            Collections.reverse(back);
            for (double posture : new double[]{HunterPosture.STANDING_HEIGHT,HunterPosture.CROUCH_HEIGHT}) {
                NativeSchoolInteriorReview.walk(map,path.getValue(),posture,path.getKey());
                NativeSchoolInteriorReview.walk(map,back,posture,path.getKey()+" return");
                walks += 2;
            }
        }
        var report = new ArrayList<>(List.of("check\tresult","packaged_native_scene\t"+source.scene().revision(),
            "named_network_revision\t"+map.level().revision(),"collision_sha256\t"+source.scene().physics().sha256(),
            "renderer\t"+(gpu ? GpuWorldRenderer.active().rendererDescription():"Software"),
            "default_lock_review_flag_campaign_and_stale_revision\tPASS","retained_fitted_doors\t"+source.scene().doors().size(),
            "route_replays\t"+walks,"staged_switches\t12","staged_lamps\t36"));
        NativeSchoolInteriorReview.auditHiding(map,report);
        var captures = stage.resolve(gpu?"captures/opengl":"captures/software");
        Files.createDirectories(captures);
        capture(map,new Vec(35,0,70.6),new Vec(32,1.08,80),captures.resolve("monastery-named-arrival-van.png"));
        capture(map,new Vec(32,0,67),new Vec(32,1.71,62),captures.resolve("monastery-named-public-gate.png"));
        capture(map,new Vec(23,-3,17.42),new Vec(18,-1.4,16),captures.resolve("monastery-named-lower-memorial.png"));
        NativeSchoolInteriorReview.auditWalkInCover(map,captures,report,"monastery",java.util.Map.of(
            3,List.of(new Vec(22,0,7)),5,List.of(new Vec(-3,0,39)),9,List.of(new Vec(23,-3,7))));
        capture(map,new Vec(18,0,9),new Vec(19.74,.85,12.2),captures.resolve("monastery-archive-reading-table.png"));
        capture(map,new Vec(59,0,40),new Vec(56.15,.9,34.6),captures.resolve("monastery-refectory-tables.png"));
        capture(map,new Vec(0,0,36),new Vec(-3.35,.64,44),captures.resolve("monastery-dormitory-beds.png"));
        capture(map,new Vec(36,0,9),new Vec(38.53,1,4.14),captures.resolve("monastery-vestry-wash-trough.png"));
        capture(map,new Vec(32,0,59),new Vec(38.7,.48,59.25),captures.resolve("monastery-gatehouse-porter.png"));
        verifyDrainage(stage.resolve("wld-10/drainage-survey.tsv"),source.scene().physics(),3,.17,report);
        verifyDrainage(stage.resolve("wld-10/north-drainage.tsv"),source.scene().physics(),2,.05,report);
        capture(map,new Vec(26,0,65),new Vec(24.55,-.056,63.2),captures.resolve("monastery-west-gate-overflow.png"));
        capture(map,new Vec(38,0,65),new Vec(39.45,-.056,63.2),captures.resolve("monastery-east-gate-overflow.png"));
        capture(map,new Vec(67.5,0,25),new Vec(70.45,-.06,27.1),captures.resolve("monastery-service-interceptor.png"));
        verifyCourt(stage,map,report);
        capture(map,new Vec(29,0,35.5),new Vec(26.88,-.042,35.5),captures.resolve("monastery-west-herb-court-grate.png"));
        capture(map,new Vec(29,0,35.5),new Vec(31.12,-.042,35.5),captures.resolve("monastery-east-herb-court-grate.png"));
        capture(map,new Vec(16.55,0,25),new Vec(15.7,.045,22.82),captures.resolve("monastery-herb-court-pipe-inlet.png"));
        captureNorth(map,captures);
        verifyLandscape(map,captures,report);
        if (!gpu) {
            BlackwaterNamedReview.solo(map,report);
            MonasteryPowerReview.live(scene,report,site);
        }
        Files.write(stage.resolve(gpu?"named-opengl.tsv":"named-software.tsv"),report);
        report.forEach(System.out::println);
    }

    private static void verifyDrainage(Path survey,AuthoredSitePhysics physics,int channelCount,double minimumFall,List<String> report) throws Exception {
        var profiles = new LinkedHashMap<String,List<Double>>();
        try (var rows = Files.newBufferedReader(survey)) {
            require("swale\tdistance\tx\tnative_invert_y\tz".equals(rows.readLine()), "Invalid drainage survey heading");
            for (String row; (row = rows.readLine()) != null; ) {
                var fields = row.split("\t");
                double x = Double.parseDouble(fields[2]), y = Double.parseDouble(fields[3]), z = Double.parseDouble(fields[4]);
                var hit = physics.firstOpticalHit(new Vec(x,y+.006,z),new Vec(x,y-.006,z));
                require(hit != null && hit.component().equals("WLD-10 surveyed drainage ground")
                    && Math.abs(hit.fraction() - .5) < .035, "Runoff survey misses packaged ground at " + row);
                profiles.computeIfAbsent(fields[0],name -> new ArrayList<>()).add(y);
            }
        }
        require(profiles.size()==channelCount, "Missing drainage profiles in "+survey);
        for (var channel : profiles.entrySet()) {
            var heights = channel.getValue();
            require(heights.size()>30 && heights.get(0)-heights.get(heights.size()-1)>minimumFall, channel.getKey()+" has no fall");
            for (int i=1;i<heights.size();i++)
                require(heights.get(i)-heights.get(i-1)<=.001, channel.getKey()+" has a raised bed");
            report.add(channel.getKey()+"_native_fall\t"+heights.size()+" surveyed native bed points");
        }
    }

    private static void verifyCourt(Path stage,HouseMap map,List<String> report) throws Exception {
        var physics=map.nativeInvestigation().physics();
        var conductors=new LinkedHashMap<String,List<Vec>>();
        try (var rows=Files.newBufferedReader(stage.resolve("wld-10/court-drainage.tsv"))) {
            require("inlet\tx\ty\tz".equals(rows.readLine()),"Invalid herb-court survey heading");
            for (String row; (row=rows.readLine())!=null; ) {
                var f=row.split("\t");
                conductors.computeIfAbsent(f[0],name -> new ArrayList<>()).add(
                    new Vec(Double.parseDouble(f[1]),Double.parseDouble(f[2]),Double.parseDouble(f[3])));
            }
        }
        require(conductors.size()==8,"Incomplete herb-court network");
        int spans=0;
        for (var entry:conductors.entrySet()) {
            var stations=entry.getValue();
            var throat=stations.get(0);
            require(physics.clearSight(new Vec(throat.x()+.012,.010,throat.z()),
                new Vec(throat.x()+.012,-.20,throat.z())),entry.getKey()+" has a covered throat");
            for (int i=1;i<stations.size();i++) {
                var a=stations.get(i-1);
                var b=stations.get(i);
                var start=new Vec(a.x()+.06*(b.x()-a.x()),a.y()+.06*(b.y()-a.y()),a.z()+.06*(b.z()-a.z()));
                var end=new Vec(a.x()+.94*(b.x()-a.x()),a.y()+.94*(b.y()-a.y()),a.z()+.94*(b.z()-a.z()));
                require(a.y()>b.y() && physics.clearSight(start,end),entry.getKey()+" lost its clear falling conductor");
                spans++;
            }
        }
        for (double x:new double[]{26.88,31.12}) {
            var start=new Vec(x+.025,.018,35.5);
            var end=new Vec(start.x(),-2.04,start.z());
            var bed=physics.firstOpticalHit(start,end);
            String side=x<29?"west":"east";
            require(bed!=null && bed.component().equals("Monastery "+side+" herb-court soakaway gravel filter bed region0")
                && Math.abs(bed.fraction()-.94169)<.002,"Shaft cannot reach filter at "+x+": "+bed);
        }
        var route=List.of(new Vec(26.2,-.027,35.5),new Vec(29,0,35.5),new Vec(31.8,-.027,35.5));
        var reverse=new ArrayList<>(route);
        Collections.reverse(reverse);
        for (double posture:new double[]{HunterPosture.STANDING_HEIGHT,HunterPosture.CROUCH_HEIGHT}) {
            NativeSchoolInteriorReview.walk(map,route,posture,"running across both herb-court grates");
            NativeSchoolInteriorReview.walk(map,reverse,posture,"return across both herb-court grates");
        }
        report.add("herb_court_roof_throats_and_falling_pipe_spans\t8 open inlets and "+spans+" clear falling spans");
        report.add("herb_court_inspection_and_traversal\t2 native filter beds through clear shafts and four standing/crouch passes");
    }

    private static void captureNorth(HouseMap map,Path captures) throws Exception {
        var physics = map.nativeInvestigation().physics();
        Vec[] feet = {new Vec(5.75,0,16.7),new Vec(46.15,0,13.5),new Vec(18,0,-.65)};
        Vec[] targets = {new Vec(6.91,.24,16.57),new Vec(44.48,-.23,14.91),new Vec(19.7,-.4,-2.65)};
        String[] names = {"west-north-roof-spillway","east-north-roof-spillway","north-low-ground-receiver"};
        for (int i=0;i<feet.length;i++) {
            var at=feet[i];
            var surface=physics.support(at.x(),at.z(),0,1);
            require(surface!=null,"Missing supported exterior position "+at);
            capture(map,new Vec(at.x(),surface.y(),at.z()),targets[i],captures.resolve("monastery-"+names[i]+".png"));
        }
        require(!physics.clearSight(new Vec(6.3,1.25,12),new Vec(9.6,1.25,12))
            && !physics.clearSight(new Vec(46.1,1.25,10),new Vec(42.1,1.25,10))
            && !physics.clearSight(new Vec(20,1.25,-.6),new Vec(20,1.25,3.3)),
            "North drainage exposed an unintended opening");
    }

    private static void verifyLandscape(HouseMap map,Path captures,List<String> report) throws Exception {
        var ground=map.nativeInvestigation().physics();
        require(!ground.clearSight(new Vec(19.4,.35,35.2),new Vec(22.2,.35,35.2)),
            "Court well lost its rendered masonry collision");
        var approach=List.of(new Vec(29,0,35.5),new Vec(23.0,0,35.5));
        var back=new ArrayList<>(approach);
        Collections.reverse(back);
        for (double height:new double[]{HunterPosture.STANDING_HEIGHT,HunterPosture.CROUCH_HEIGHT}) {
            NativeSchoolInteriorReview.walk(map,approach,height,"garden aisle to the fitted well");
            NativeSchoolInteriorReview.walk(map,back,height,"return from the fitted well to the court aisle");
        }
        daylight(map,new Vec(27.3,0,77.2),new Vec(29,1.2,65.5),captures.resolve("monastery-dressed-forecourt.png"));
        daylight(map,new Vec(37.2,0,73.4),new Vec(34,1.1,79.3),captures.resolve("monastery-dressed-van-approach.png"));
        daylight(map,new Vec(25.3,0,35.5),new Vec(20.8,1,35.2),captures.resolve("monastery-court-well-and-raised-beds.png"));
        daylight(map,new Vec(29,0,44.1),new Vec(34.9,.55,40.5),captures.resolve("monastery-court-planted-soil.png"));
        report.add("landscape_court_well_and_routes\tActual well masonry collision and standing/crouched aisle approach in both directions");
    }

    private static void daylight(HouseMap map,Vec at,Vec target,Path image) throws Exception {
        capture(map,at,target,image,true);
    }

    private static void capture(HouseMap map,Vec at,Vec target,Path image) throws Exception {
        capture(map,at,target,image,false);
    }

    private static void capture(HouseMap map,Vec at,Vec target,Path image,boolean daylight) throws Exception {
        require(map.traversable(at.x(),at.y(),at.z()), "Unsupported named inspection " + at);
        var r = new SceneRenderer();
        r.setRenderSize(960,640);
        r.setPsxPreset(PsxPipeline.Preset.OFF);
        r.x = at.x();
        r.z = at.z();
        r.eyeHeight = at.y()+HunterPosture.STANDING_EYE;
        r.yaw = Math.atan2(target.x()-at.x(),target.z()-at.z());
        r.pitch = Math.atan2(target.y()-r.eyeHeight,Math.hypot(target.x()-at.x(),target.z()-at.z()));
        r.lamp = !daylight;
        if (daylight) r.weatherModule().setTimeOfDay(.50);
        r.setLampHardware(EquipmentTiers.Tier.III,false);
        NativeSchoolInteriorReview.drawFrame(r,map,image);
    }

    private static void require(boolean ok,String message) {
        if (!ok) throw new AssertionError(message);
    }
}
