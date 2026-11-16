package org.beckenburg;

import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;
import javax.swing.*;

/**
 * Optional graphical diagnostic. Starts a four-seat loopback House case and observes
 * the real ghost panel in a visible window using the production OpenGL worker.
 * One row is recorded when a newly drawn scene requests a repaint. Distinct positive
 * composited_id values identify completed GL images consumed by the panel, independent
 * of how often Swing or the display physically presents them.
 *
 * Run with a desktop display (or Xorg), the gpu-client runtime classpath, and an
 * isolated preference store. The argument is the output directory. Starts 20 warm-up
 * draws, then records idle, held movement and recovery plus a final viewport PNG.
 */
public final class GhostViewportProfile extends RepaintManager {
    private final CountDownLatch ready = new CountDownLatch(1);
    private final CountDownLatch running = new CountDownLatch(1);
    private final CountDownLatch warmed = new CountDownLatch(20);
    private final AtomicInteger accepted = new AtomicInteger();
    private final AtomicInteger muted = new AtomicInteger();
    private final AtomicInteger listened = new AtomicInteger();
    private final AtomicInteger leaks = new AtomicInteger();
    private LanGhostPanel panel;
    private SceneRenderer scene;
    private JFrame window;
    private Field lastReadback;
    private SceneRenderer.FrameDiagnostics previous;
    private boolean active, closed;
    private String phase = "warmup";
    private double startX, startZ, travel;
    private PrintWriter writer;

    private static void waitFor(CountDownLatch latch, String name) throws Exception {
        if (!latch.await(180, TimeUnit.SECONDS)) throw new AssertionError("Timed out: " + name);
    }

    private static void backend(String method) throws Exception {
        var entry = Class.forName("org.beckenburg.gpu.GpuDesktop").getDeclaredMethod(method);
        entry.setAccessible(true);
        entry.invoke(null);
    }

    private void receive(String line) {
        if (line.equals("LOBBY|3|HUMAN|4|WAITING")) ready.countDown();
        if (line.startsWith("INFO|mission started")) running.countDown();
        if (line.equals("ACCEPT|GSTEP")) accepted.incrementAndGet();
        if (line.startsWith("GLISTEN|0|")) muted.incrementAndGet();
        if (line.startsWith("GLISTEN|1|")) listened.incrementAndGet();
        if (line.startsWith("BODY|") || line.startsWith("TEAMMAP|") || line.startsWith("PROGRESS|"))
            leaks.incrementAndGet();
        SwingUtilities.invokeLater(() -> {
            if (panel == null || closed) return;
            if (line.equals("ACCEPT|GVIEW3D")) active = true;
            if (active) panel.onEvent(line);
        });
    }

    @Override public void addDirtyRegion(JComponent component, int x, int y, int width, int height) {
        super.addDirtyRegion(component, x, y, width, height);
        if (component != panel || scene == null || !active || closed || !SwingUtilities.isEventDispatchThread()) return;
        var frame = scene.frameDiagnostics();
        if (frame == null || frame == previous) return;
        previous = frame;
        try {
            var readback = (GpuWorldRenderer.Result) lastReadback.get(scene);
            writer.printf("%s,%d,%d,%d,%d,%d,%d,%d,%d,%d%n", phase, System.nanoTime(),
                frame.totalNs(), frame.sceneBuildNs(), frame.geometryNs(), frame.effectsNs(),
                scene.submittedGpuFrameId(), readback == null ? -1 : readback.frameId(), frame.width(), frame.height());
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
        if (phase.equals("moving")) travel = Math.max(travel, Math.hypot(scene.x - startX, scene.z - startZ));
        if (frame.gpuReadbackAvailable()) warmed.countDown();
    }

    private void forward(boolean down) {
        int id = down ? KeyEvent.KEY_PRESSED : KeyEvent.KEY_RELEASED;
        var event = new KeyEvent(panel, id, System.currentTimeMillis(), 0, KeyEvent.VK_W, KeyEvent.CHAR_UNDEFINED);
        if (down) panel.keyPressed(event);
        else panel.keyReleased(event);
    }

    private void mount(LanClient ghost) throws Exception {
        Field camera = LanGhostPanel.class.getDeclaredField("camera");
        camera.setAccessible(true);
        lastReadback = SceneRenderer.class.getDeclaredField("lastUsableGpuFrame");
        lastReadback.setAccessible(true);
        SwingUtilities.invokeAndWait(() -> {
            RepaintManager.setCurrentManager(this);
            panel = new LanGhostPanel(ghost);
            try {
                scene = (SceneRenderer) camera.get(panel);
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
            scene.setRenderSize(320, 200);
            window = new JFrame();
            window.setContentPane(panel);
            window.setSize(640, 400);
            window.setVisible(true);
            panel.requestFocusInWindow();
        });
    }

    private void measure(Path output) throws Exception {
        waitFor(warmed, "20 composited GL draws");
        SwingUtilities.invokeAndWait(() -> phase = "idle");
        Thread.sleep(10_000);
        SwingUtilities.invokeAndWait(() -> {
            phase = "moving";
            startX = scene.x;
            startZ = scene.z;
            forward(true);
        });
        Thread.sleep(4_000);
        SwingUtilities.invokeAndWait(() -> {
            forward(false);
            phase = "recovery";
        });
        Thread.sleep(2_000);
        SwingUtilities.invokeAndWait(() -> closed = true);
        String results = "accepted_steps=" + accepted + "\nmax_travel_m=" + travel + "\nmuted_packets=" + muted
            + "\nlisten_packets=" + listened + "\nprivate_packet_leaks=" + leaks + "\nghost_room=" + panel.ghostRoom() + "\n";
        Files.writeString(output.resolve("summary.txt"), results);
        System.out.print(results);
        SwingUtilities.invokeAndWait(() -> {
            var image = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_RGB);
            var graphics = image.createGraphics();
            try {
                panel.paint(graphics);
            } finally {
                graphics.dispose();
            }
            try {
                ImageIO.write(image, "png", output.resolve("viewport.png").toFile());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        if (accepted.get() < 1 || travel < .02 || muted.get() < 1 || listened.get() < 1
                || leaks.get() != 0 || panel.ghostRoom() < 0) throw new AssertionError("Ghost gameplay assertions failed");
    }

    private void run(Path output) throws Exception {
        Files.createDirectories(output);
        try (var out = new PrintWriter(Files.newBufferedWriter(output.resolve("frames.csv")));
             var host = new LanHost("127.0.0.1", 0, GhostIdentity.Type.SPIRIT,
                 CustomContract.standard(3), false, true, HouseMap.Site.HOUSE);
             var owner = new LanClient("127.0.0.1", host.port(), false, "Owner", "", host.localOwnerCredential(), s -> {});
             var guestOne = new LanClient("127.0.0.1", host.port(), false, "GuestOne", s -> {});
             var guestTwo = new LanClient("127.0.0.1", host.port(), false, "GuestTwo", s -> {});
             var ghost = new LanClient("127.0.0.1", host.port(), true, "Ghost", this::receive)) {
            writer = out;
            writer.println("phase,time_ns,draw_ns,build_ns,geometry_ns,effects_ns,submitted_id,composited_id,width,height");
            try {
                mount(ghost);
                owner.ready();
                guestOne.ready();
                guestTwo.ready();
                ghost.ready();
                waitFor(ready, "4 ready clients");
                owner.start();
                waitFor(running, "round start");
                ghost.viewGhost3D();
                measure(output);
                if (writer.checkError()) throw new IOException("Could not write ghost viewport timings");
            } finally {
                SwingUtilities.invokeAndWait(() -> {
                    closed = true;
                    if (panel != null) panel.close();
                    if (window != null) window.dispose();
                    RepaintManager.setCurrentManager(null);
                });
            }
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Expected an output directory");
        backend("startGpu");
        try {
            new GhostViewportProfile().run(Path.of(args[0]));
        } finally {
            backend("stopReviewGpu");
        }
    }
}
