package org.beckenburg;

import java.awt.event.KeyEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JComponent;
import javax.swing.RepaintManager;
import javax.swing.SwingUtilities;

/**
 * Measure the live hunter panel's timer-driven world completions and TCP arrivals.
 * One offscreen viewport, two hunters and a human-role ghost on loopback, or a solo case.
 * This observes completed CPU renders, not desktop presentation or physical-network delay.
 * Optional diagnostic: run in its own JVM with isolated VerificationPreferencesFactory.
 */
public final class LanViewportPerformanceReview extends RepaintManager {
    private record Frame(String phase, long time, long compositedFrame, SceneRenderer.FrameDiagnostics cost) {}
    private java.lang.reflect.Field lastReadback;
    private record State(String phase, int tick, long received, long delivered) {}
    private final List<Frame> frames = new ArrayList<>();
    private final List<State> states = new ArrayList<>();
    private final CountDownLatch ready = new CountDownLatch(1);
    private final CountDownLatch started = new CountDownLatch(1);
    private final CountDownLatch entered = new CountDownLatch(1);
    private final CountDownLatch warmed = new CountDownLatch(10);
    private final AtomicInteger acceptedSteps = new AtomicInteger();
    private volatile String phase = "warmup";
    private LanWorldPanel panel;
    private SceneRenderer scene;
    private SceneRenderer.FrameDiagnostics previous;
    private boolean inView, finished;
    private double startX, startZ, maxTravel;
    private int moveKey;

    @Override public void addDirtyRegion(JComponent component, int x, int y, int w, int h) {
        super.addDirtyRegion(component, x, y, w, h);
        if (component != panel || scene == null || !inView || finished || !SwingUtilities.isEventDispatchThread()) return;
        var cost = scene.frameDiagnostics();
        if (cost == null || cost == previous) return;
        previous = cost;
        long now = System.nanoTime();
        long composited = -1;
        if (cost.gpuReadbackAvailable()) {
            try {
                var result = (GpuWorldRenderer.Result) lastReadback.get(scene);
                if (result != null) composited = result.frameId();
            } catch (IllegalAccessException e) { throw new AssertionError(e); }
        }
        frames.add(new Frame(phase, now, composited, cost));
        if (phase.equals("moving"))
            maxTravel = Math.max(maxTravel, Math.hypot(scene.x - startX, scene.z - startZ));
        warmed.countDown();
    }

    private void receive(String frame) {
        long received = System.nanoTime();
        String atArrival = phase;
        if (frame.equals("LOBBY|2|HUMAN|3|WAITING")) ready.countDown();
        if (frame.startsWith("INFO|mission started")) started.countDown();
        if (frame.equals("ACCEPT|STEP")) acceptedSteps.incrementAndGet();
        SwingUtilities.invokeLater(() -> {
            if (panel == null || finished) return;
            if (frame.equals("ACCEPT|VIEW3D")) inView = true;
            if (!inView) return;
            if (frame.startsWith("STATE|")) {
                int tick = Integer.parseInt(frame.substring(6, frame.indexOf('|', 6)));
                states.add(new State(atArrival, tick, received, System.nanoTime()));
            }
            panel.onEvent(frame);
            if (frame.startsWith("SITE|HOUSE|")) {
                key(KeyEvent.VK_SPACE, true);
                key(KeyEvent.VK_SPACE, false);
                entered.countDown();
            }
        });
    }

    private void key(int code, boolean down) {
        int id = down ? KeyEvent.KEY_PRESSED : KeyEvent.KEY_RELEASED;
        var event = new KeyEvent(panel, id, System.currentTimeMillis(), 0, code, KeyEvent.CHAR_UNDEFINED);
        if (down) panel.keyPressed(event); else panel.keyReleased(event);
    }

    private int walkKey() {
        var origin = new LevelDefinition.Vec(scene.x, scene.eyeHeight - HunterPosture.STANDING_EYE, scene.z);
        int[] keys = {KeyEvent.VK_W, KeyEvent.VK_S, KeyEvent.VK_A, KeyEvent.VK_D};
        double[] offsets = {0, Math.PI, Math.PI / 2, -Math.PI / 2};
        for (int i = 0; i < keys.length; i++) {
            double yaw = scene.yaw + offsets[i];
            var target = panel.worldMap().resolveMove(origin, origin.x() + .6 * Math.sin(yaw),
                origin.z() + .6 * Math.cos(yaw), HunterPosture.STANDING_HEIGHT);
            if (target != null && Math.hypot(target.x() - origin.x(), target.z() - origin.z()) > .5)
                return keys[i];
        }
        throw new AssertionError("No walkable cardinal direction from the authored van spawn");
    }

    private static void await(CountDownLatch latch, String description) throws Exception {
        if (!latch.await(60, TimeUnit.SECONDS)) throw new AssertionError("Timed out awaiting " + description);
    }

    private void write(Path output, long movingNs, int peers, boolean solo) throws Exception {
        Files.createDirectories(output);
        try (var out = Files.newBufferedWriter(output.resolve("frames.csv"))) {
            out.write("phase,time_ns,render_ns,build_ns,geometry_ns,effects_ns,width,height,faces,shadow_rays,composited_gl_frame_id\n");
            for (var frame : frames) {
                var d = frame.cost();
                out.write(String.format(Locale.ROOT, "%s,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d%n",
                    frame.phase(), frame.time(), d.totalNs(), d.sceneBuildNs(), d.geometryNs(), d.effectsNs(),
                    d.width(), d.height(), d.faces(), d.lighting().shadowRays(), frame.compositedFrame()));
            }
        }
        if (!solo) try (var out = Files.newBufferedWriter(output.resolve("state.csv"))) {
            out.write("phase,host_tick,client_received_ns,edt_dispatch_ns\n");
            for (var s : states)
                out.write(String.format(Locale.ROOT, "%s,%d,%d,%d%n", s.phase(), s.tick(), s.received(), s.delivered()));
        }
        String details = String.format(Locale.ROOT, "mode=%s%nrendered_hunters=1%nother_tcp_peers=%d%nviewport=offscreen%nrenderer=%s%nvisible_peer_hunters=%d%nmoving_seconds=%.6f%naccepted_steps=%d%nmax_travel_metres=%.6f%n",
            solo ? "solo" : "multiplayer", solo ? 0 : 2, GpuWorldRenderer.active() == null ? "software" : GpuWorldRenderer.active().rendererDescription(), peers, movingNs / 1e9, solo ? -1 : acceptedSteps.get(), maxTravel);
        if (GpuWorldRenderer.active() != null && frames.stream()
                .filter(f -> f.phase().equals("idle") && f.compositedFrame() >= 0)
                .mapToLong(Frame::compositedFrame).distinct().count() < 2)
            throw new AssertionError("Live panel never composited advancing OpenGL readbacks");
        Files.writeString(output.resolve("summary.txt"), details);
        System.out.print(details);
        if ((!solo && (peers < 1 || states.isEmpty() || acceptedSteps.get() == 0)) || maxTravel < .1 ||
                frames.stream().filter(f -> f.phase().equals("idle")).count() < 2)
            throw new AssertionError("Missing live peer, timed frame, snapshot or host-approved movement");
    }

    private void mount(java.util.function.Supplier<LanWorldPanel> open) throws Exception {
        var field = LanWorldPanel.class.getDeclaredField("renderer");
        field.setAccessible(true);
        lastReadback = SceneRenderer.class.getDeclaredField("lastUsableGpuFrame");
        lastReadback.setAccessible(true);
        SwingUtilities.invokeAndWait(() -> {
            RepaintManager.setCurrentManager(this);
            panel = open.get();
            panel.setSize(640, 360);
            panel.addNotify();
            try { scene = (SceneRenderer) field.get(panel); }
            catch (IllegalAccessException e) { throw new AssertionError(e); }
        });
    }

    private void unmount() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            finished = true;
            if (panel != null) { panel.close(); panel.removeNotify(); }
            RepaintManager.setCurrentManager(null);
        });
    }

    private void measure(Path output, boolean solo) throws Exception {
        await(warmed, "ten scheduled world renders");
        Thread.sleep(4000);
        SwingUtilities.invokeAndWait(() -> phase = "idle");
        Thread.sleep(TimeUnit.SECONDS.toMillis(Integer.getInteger("beckenburg.profile.idleSeconds", 8)));
        SwingUtilities.invokeAndWait(() -> {
            moveKey = walkKey();
            startX = scene.x; startZ = scene.z;
            phase = "moving";
            key(moveKey, true);
        });
        long begin = System.nanoTime();
        Thread.sleep(3000);
        SwingUtilities.invokeAndWait(() -> { key(moveKey, false); phase = "cooldown"; });
        long duration = System.nanoTime() - begin;
        Thread.sleep(500);
        int[] peers = new int[1];
        SwingUtilities.invokeAndWait(() -> { finished = true; peers[0] = panel.visiblePeers(); });
        write(output, duration, peers[0], solo);
    }

    private void solo(Path output) throws Exception {
        var profile = new ProfileStore(output.resolve("profile.properties"));
        try (var game = new LocalInvestigation(profile, SiteCatalog.Location.HOUSE,
                CharacterVisuals.Hunter.OBSERVER, MissionWeather.Mode.FIXED,
                EvidenceSignals.Weather.CLEAR, 1, null, 0)) {
            try {
                mount(game::openView);
                SwingUtilities.invokeAndWait(() -> {
                    inView = true;
                    key(KeyEvent.VK_SPACE, true);
                    key(KeyEvent.VK_SPACE, false);
                });
                measure(output, true);
                if (!game.running()) throw new AssertionError("Solo case exited before measurement finished");
            } finally { unmount(); }
        }
    }

    private void run(Path output) throws Exception {
        try (var host = new LanHost("127.0.0.1", 0, GhostIdentity.Type.SPIRIT,
                CustomContract.standard(2), false, true, HouseMap.Site.HOUSE);
             var owner = new LanClient("127.0.0.1", host.port(), false, "FrameReview", "",
                host.localOwnerCredential(), this::receive);
             var guest = new LanClient("127.0.0.1", host.port(), false, "Peer", s -> {});
             var ghost = new LanClient("127.0.0.1", host.port(), true, "Ghost", s -> {})) {
            try {
                mount(() -> new LanWorldPanel(owner));
                owner.ready(); guest.ready(); ghost.ready();
                await(ready, "three ready clients");
                owner.start();
                await(started, "host round start");
                owner.view3D();
                await(entered, "House 3D site");
                measure(output, false);
            } finally {
                unmount();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        var options = new ArrayList<>(List.of(args));
        boolean gpu = options.remove("--gpu"), solo = options.remove("--solo");
        if (options.size() != 1)
            throw new IllegalArgumentException("Usage: [--gpu] [--solo] <trial output directory>");
        java.lang.reflect.Method stop = null;
        if (gpu) {
            var desktop = Class.forName("org.beckenburg.gpu.GpuDesktop");
            var start = desktop.getDeclaredMethod("startGpu"); start.setAccessible(true);
            stop = desktop.getDeclaredMethod("stopReviewGpu"); stop.setAccessible(true);
            start.invoke(null);
        }
        try {
            var review = new LanViewportPerformanceReview();
            var output = Path.of(options.get(0));
            if (solo) review.solo(output); else review.run(output);
        } finally {
            if (stop != null) stop.invoke(null);
        }
    }
}
