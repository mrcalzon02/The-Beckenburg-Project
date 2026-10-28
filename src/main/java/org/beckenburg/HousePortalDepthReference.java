package org.beckenburg;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Diagnostic-only depth ownership for authored House triangles. This is an
 * independent CPU reference, NOT a production portal rejection path or a GPU
 * screenshot substitute. All triangles have already been clipped to the near
 * plane by SceneRenderer, using its actual world-space authored geometry.
 */
final class HousePortalDepthReference {
    private HousePortalDepthReference() { }

    record Vertex(double x, double y, double z, double u, double v) { }
    record Triangle(Vertex a, Vertex b, Vertex c, int room, int material) { }
    @FunctionalInterface interface AlphaSampler {
        int alpha(int material, double u, double v);
    }
    record Result(int cameraRoom, int triangles, int surfacedPixels,
                  int missingVisiblePixels, int unownedPixels,
                  int transparentSamples, int[] roomPixels,
                  Set<Integer> candidates, Set<Integer> excludedVisibleRooms,
                  int width, int height, int[] pixelOwners, float[] pixelDepthMetres) {
        Result {
            roomPixels = roomPixels.clone();
            candidates = Set.copyOf(candidates);
            excludedVisibleRooms = Set.copyOf(excludedVisibleRooms);
            pixelOwners = pixelOwners.clone();
            pixelDepthMetres = pixelDepthMetres.clone();
        }
        @Override public int[] roomPixels() { return roomPixels.clone(); }
        /** Defensive copies: -1 owner means unowned or empty; +INF depth means empty. */
        @Override public int[] pixelOwners() { return pixelOwners.clone(); }
        @Override public float[] pixelDepthMetres() { return pixelDepthMetres.clone(); }
        boolean hasPotentialFalseNegative() { return missingVisiblePixels > 0; }
    }

    /** Closed doors block graph traversal; windows, stairs and open portals do
     * not. Unknown/exterior camera ownership fails open to every room. */
    static Set<Integer> candidates(HouseMap map, int cameraRoom) {
        var level = map.level();
        var all = new HashSet<Integer>();
        for (var room : level.rooms()) all.add(room.id());
        if (!all.contains(cameraRoom) || level.room(cameraRoom).outside())
            return Set.copyOf(all);
        var seen = new HashSet<Integer>();
        var queue = new ArrayDeque<Integer>();
        seen.add(cameraRoom);
        queue.add(cameraRoom);
        while (!queue.isEmpty()) {
            int current = queue.removeFirst();
            for (var portal : level.portals()) {
                if (portal.a() != current && portal.b() != current) continue;
                if (portal.door() && !map.doorOpen(portal.id())) continue;
                int other = portal.a() == current ? portal.b() : portal.a();
                if (all.contains(other) && seen.add(other)) queue.addLast(other);
            }
            for (var stair : level.stairs()) {
                if (stair.a() != current && stair.b() != current) continue;
                int other = stair.a() == current ? stair.b() : stair.a();
                if (all.contains(other) && seen.add(other)) queue.addLast(other);
            }
        }
        // Outdoor sightlines have no validated portal planes: retain them.
        for (var room : level.rooms()) if (room.outside()) seen.add(room.id());
        return Set.copyOf(seen);
    }

    static Result audit(HouseMap map, int cameraRoom, int width, int height,
                        double focal, List<Triangle> triangles) {
        return raster(cameraRoom, width, height, focal, triangles,
            candidates(map, cameraRoom), map.level().rooms().size(),
            (material, u, v) -> material >= GlbProps.TEXTURE_BASE
                ? GlbProps.sampleAlpha(material, u, v) : 255);
    }

    /** Pixel ownership from the actual projected triangles; never equate a
     * frustum-intersecting face with a depth-visible pixel. Transparent samples
     * are NON-occluding, so window glass cannot hide basement geometry. */
    static Result raster(int cameraRoom, int width, int height, double focal,
                         List<Triangle> triangles, Set<Integer> candidates,
                         int roomCount, AlphaSampler alpha) {
        if (width < 1 || height < 1 || !Double.isFinite(focal) || focal <= 0
                || roomCount < 1 || alpha == null)
            throw new IllegalArgumentException("invalid depth reference");
        double[] depth = new double[width * height];
        int[] owner = new int[width * height];
        Arrays.fill(depth, Double.POSITIVE_INFINITY);
        Arrays.fill(owner, -1);
        int transparent = 0;
        for (Triangle t : triangles) {
            var a = t.a(); var b = t.b(); var c = t.c();
            if (!(a.z() > 0 && b.z() > 0 && c.z() > 0)) continue;
            double x0 = width * .5 + a.x() / a.z() * focal;
            double x1 = width * .5 + b.x() / b.z() * focal;
            double x2 = width * .5 + c.x() / c.z() * focal;
            double y0 = height * .5 - a.y() / a.z() * focal;
            double y1 = height * .5 - b.y() / b.z() * focal;
            double y2 = height * .5 - c.y() / c.z() * focal;
            if (!Double.isFinite(x0+x1+x2+y0+y1+y2)) continue;
            double denominator = (y1-y2)*(x0-x2) + (x2-x1)*(y0-y2);
            if (Math.abs(denominator) < 1e-10) continue;
            int minX = Math.max(0, (int)Math.floor(Math.min(x0, Math.min(x1,x2))));
            int maxX = Math.min(width-1, (int)Math.ceil(Math.max(x0, Math.max(x1,x2))));
            int minY = Math.max(0, (int)Math.floor(Math.min(y0, Math.min(y1,y2))));
            int maxY = Math.min(height-1, (int)Math.ceil(Math.max(y0, Math.max(y1,y2))));
            for (int py=minY; py<=maxY; py++) for (int px=minX; px<=maxX; px++) {
                double u = ((y1-y2)*(px+.5-x2) + (x2-x1)*(py+.5-y2)) / denominator;
                double v = ((y2-y0)*(px+.5-x2) + (x0-x2)*(py+.5-y2)) / denominator;
                double w = 1-u-v;
                if (u < 0 || v < 0 || w < 0) continue;
                double invZ = u/a.z() + v/b.z() + w/c.z();
                if (!(invZ > 0)) continue;
                double z = 1/invZ;
                int pixel = py*width+px;
                if (z >= depth[pixel]) continue;
                double tu = (u*a.u()/a.z() + v*b.u()/b.z() + w*c.u()/c.z())/invZ;
                double tv = (u*a.v()/a.z() + v*b.v()/b.z() + w*c.v()/c.z())/invZ;
                int opacity;
                try { opacity = alpha.alpha(t.material(), tu, tv); }
                catch (IllegalArgumentException invalidMaterial) { opacity = 0; }
                if (opacity < 255) { transparent++; continue; }
                depth[pixel] = z;
                owner[pixel] = t.room();
            }
        }
        int[] roomPixels = new int[roomCount];
        int surfaced = 0, missing = 0, unowned = 0;
        var excluded = new HashSet<Integer>();
        for (int i=0; i<owner.length; i++) {
            if (!Double.isFinite(depth[i])) continue;
            surfaced++;
            int room = owner[i];
            if (room < 0 || room >= roomCount) { unowned++; continue; }
            roomPixels[room]++;
            if (!candidates.contains(room)) { missing++; excluded.add(room); }
        }
        // Preserve exact per-pixel owner/depth for later same-pose GPU readback.
        // These are diagnostic snapshots only; no production culling uses them.
        float[] pixelDepthMetres = new float[depth.length];
        for (int i=0; i<depth.length; i++) pixelDepthMetres[i]=(float)depth[i];
        return new Result(cameraRoom, triangles.size(), surfaced, missing,
            unowned, transparent, roomPixels, candidates, excluded,
            width, height, owner, pixelDepthMetres);
    }
}
