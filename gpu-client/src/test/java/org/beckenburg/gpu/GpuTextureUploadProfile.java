package org.beckenburg.gpu;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.GdxNativesLoader;
import org.beckenburg.GlbProps;
import org.beckenburg.GpuWorldRenderer;
import org.beckenburg.HouseMap;
import org.beckenburg.LevelMaterials;
import org.beckenburg.MaterialAtlas;
import org.beckenburg.RoomLighting;
import org.beckenburg.SceneRenderer;
import org.beckenburg.VanTelemetry;
import java.awt.image.BufferedImage;
import java.lang.management.ManagementFactory;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Checks bytes sent to the GL driver, then times CPU-side Pixmap preparation alone. */
public final class GpuTextureUploadProfile {
    private static final OpenGlWorldRenderer renderer = new OpenGlWorldRenderer();

    private static Pixmap reference(int material) {
        int w = GlbProps.textureWidth(material), h = GlbProps.textureHeight(material);
        Pixmap result = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                double u = (x + .5) / w, v = (y + .5) / h;
                int rgb;
                if (material < 0) rgb = 0xffffff;
                else if (material >= GlbProps.TEXTURE_BASE) rgb = GlbProps.sample(material, u, v);
                else if (material >= LevelMaterials.BASE) rgb = LevelMaterials.sample(material, u, v);
                else rgb = MaterialAtlas.sample(material, u, v);
                int alpha = material >= GlbProps.TEXTURE_BASE && material != GlbProps.VAN_MAP_MATERIAL
                    ? GlbProps.sampleAlpha(material, u, v) : 255;
                result.drawPixel(x, y, (rgb << 8) | alpha);
            }
        }
        return result;
    }

    private static int compare(int material) {
        Pixmap original = reference(material);
        try {
            Pixmap candidate = renderer.pixelsFor(material);
            try {
                int difference = original.getPixels().mismatch(candidate.getPixels());
                if (difference >= 0) throw new AssertionError("material=" + material + " byte=" + difference);
                return original.getPixels().remaining();
            } finally { candidate.dispose(); }
        } finally { original.dispose(); }
    }

    private static Set<Integer> firstHouseView() {
        Set<Integer> materials = new LinkedHashSet<>();
        GpuWorldRenderer.install(new GpuWorldRenderer() {
            public boolean ready() { return true; }
            public String rendererDescription() { return "House submission material collector"; }
            public Result latest() { return null; }
            public void submit(Frame frame) {
                for (Surface surface : frame.surfaces()) {
                    if (surface.vertexCount() > 0) materials.add(surface.material());
                }
            }
        });
        HouseMap map = new HouseMap(HouseMap.Site.HOUSE);
        SceneRenderer camera = new SceneRenderer();
        var eye = map.spawn(14);
        var look = map.spawn(13);
        camera.setRenderSize(640,360);
        camera.roomLights = new RoomLighting();
        camera.x = eye.x(); camera.z = eye.z(); camera.eyeHeight = eye.y() + 1.62;
        camera.yaw = Math.atan2(look.x()-eye.x(), look.z()-eye.z()); camera.pitch = -.075;
        camera.drawNetwork(map, List.of(), -1, 0);
        if (materials.isEmpty()) throw new AssertionError("Empty House submission");
        return materials;
    }

    private static void checkUpdates() {
        BufferedImage image = new BufferedImage(256,256,BufferedImage.TYPE_INT_ARGB);
        for (int y=0; y<256; y++) for (int x=0; x<256; x++)
            image.setRGB(x,y,(x<<24) | (y<<16) | ((255-y)<<8) | (x^y));
        compare(GlbProps.surfaceMaterial("GPU texture profile alpha ramp", image));
        int screen = GlbProps.liveSurfaceMaterial("van live screen profile",256,256);
        compare(screen);
        GlbProps.updateLiveSurface(screen,image);
        compare(screen);
        GlbProps.updateVanMap(image);
        compare(GlbProps.VAN_MAP_MATERIAL);
        int tv = GlbProps.televisionMaterial(1,2,3);
        GlbProps.updateTelevision(tv, true, 143);
        compare(tv);
        int crt = GlbProps.model("monitoring_screen_mp_1").triangles().stream()
            .mapToInt(GlbProps.Triangle::material)
            .filter(id -> GlbProps.materialName(id).equals("crt_screen_mp_2"))
            .findFirst().orElseThrow();
        compare(crt);
        GlbProps.updateVanCrt(new VanTelemetry.Display(73, 7, new int[VanTelemetry.HUNTERS]));
        compare(crt);
    }

    private static void measure(int trial, boolean candidate, Set<Integer> materials) {
        var threads = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        long allocations = threads.getCurrentThreadAllocatedBytes(), cpu = threads.getCurrentThreadCpuTime();
        long start = System.nanoTime();
        for (int material : materials) {
            Pixmap bitmap = candidate ? renderer.pixelsFor(material) : reference(material);
            bitmap.dispose();
        }
        long elapsed = System.nanoTime() - start, cpuTime = threads.getCurrentThreadCpuTime() - cpu;
        System.out.printf(Locale.ROOT,"%d,%s,%.3f,%.3f,%d%n", trial, candidate ? "candidate" : "baseline",
            elapsed/1e6, cpuTime/1e6, threads.getCurrentThreadAllocatedBytes()-allocations);
    }

    public static void main(String[] args) {
        GdxNativesLoader.load();
        Set<Integer> materials = firstHouseView();
        long bytes = 0;
        for (int material : materials) bytes += compare(material);
        System.out.println("Exact House upload parity: " + materials.size() + " materials, " + bytes + " RGBA bytes");
        checkUpdates();
        System.out.println("PASS static alpha ramp, updated live RGBA, van map, TV and House dynamic material parity");
        if (args.length==0 || !args[0].equals("--benchmark")) return;
        System.out.println("trial,implementation,wall_ms,cpu_ms,thread_allocated_bytes");
        for (int trial=1; trial<=6; trial++) {
            boolean candidateFirst = trial%2==0;
            measure(trial,candidateFirst,materials);
            measure(trial,!candidateFirst,materials);
        }
    }
}
