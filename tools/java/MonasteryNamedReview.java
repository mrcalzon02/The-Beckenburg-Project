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
                                "monastery-with-vertical-distribution.glb","monastery-with-vertical-distribution.hcs2")) {
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
        if (!gpu) {
            BlackwaterNamedReview.solo(map,report);
            MonasteryPowerReview.live(scene,report,site);
        }
        Files.write(stage.resolve(gpu?"named-opengl.tsv":"named-software.tsv"),report);
        report.forEach(System.out::println);
    }

    private static void capture(HouseMap map,Vec at,Vec target,Path image) throws Exception {
        require(map.traversable(at.x(),at.y(),at.z()), "Unsupported named inspection " + at);
        var r = new SceneRenderer();
        r.setRenderSize(960,640);
        r.setPsxPreset(PsxPipeline.Preset.OFF);
        r.x = at.x();
        r.z = at.z();
        r.eyeHeight = at.y()+HunterPosture.STANDING_EYE;
        r.yaw = Math.atan2(target.x()-at.x(),target.z()-at.z());
        r.pitch = Math.atan2(target.y()-r.eyeHeight,Math.hypot(target.x()-at.x(),target.z()-at.z()));
        r.lamp = true;
        r.setLampHardware(EquipmentTiers.Tier.III,false);
        NativeSchoolInteriorReview.drawFrame(r,map,image);
    }

    private static void require(boolean ok,String message) {
        if (!ok) throw new AssertionError(message);
    }
}
