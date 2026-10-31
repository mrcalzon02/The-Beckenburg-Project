package org.beckenburg;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Objects;

/** A shutter exposure re-renders the real world from the raised camera lens.
 * The still is never copied from a HUD, monitor, earlier framebuffer or
 * guessed evidence. Software rasterization is intentionally synchronous:
 * normal GPU gameplay and remotely rendered video cameras are unaffected.
 */
public final class CameraSnapshot {
    private CameraSnapshot(){}

    private static BufferedImage finished(SceneRenderer camera,EquipmentTiers.Tier tier){
        var frame=camera.frameDiagnostics();
        if(frame==null||frame.gpu()||camera.image()==null)
            throw new IllegalStateException("no completed camera lens frame");
        return HunterPhotography.snapshot(camera.image(),tier);
    }

    /** Lens view of the hunter's authoritative visible multiplayer world. */
    public static BufferedImage network(SceneRenderer camera,SceneRenderer hunter,
            HouseMap map,List<SceneRenderer.RemoteBody> hunters,
            SceneRenderer.Apparition apparition,double clock,
            List<SceneRenderer.WorldMark> marks,EquipmentTiers.Tier tier){
        Objects.requireNonNull(camera);Objects.requireNonNull(map);
        Objects.requireNonNull(hunters);Objects.requireNonNull(marks);
        camera.photographicSoftwareRaster();
        camera.copyPhotographicWorld(hunter,tier);
        camera.drawNetwork(map,hunters,apparition,clock,marks);
        return finished(camera,tier);
    }

    /** Solo uses the same world renderer and actual current session models. */
    public static BufferedImage solo(SceneRenderer camera,SceneRenderer hunter,
            HouseMap map,InvestigationSession session,double clock,
            List<SceneRenderer.RemoteBody> allies,EquipmentTiers.Tier tier){
        Objects.requireNonNull(camera);Objects.requireNonNull(map);
        Objects.requireNonNull(session);Objects.requireNonNull(allies);
        camera.photographicSoftwareRaster();
        camera.copyPhotographicWorld(hunter,tier);
        camera.draw(map,session,clock,allies);
        return finished(camera,tier);
    }
}
