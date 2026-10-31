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
    private static BufferedImage preview(SceneRenderer camera){
        var diagnostics=camera.frameDiagnostics();
        if(diagnostics==null||diagnostics.gpu())
            throw new IllegalStateException("no synchronous camera preview");
        return HunterPhotography.snapshot(camera.image());
    }

    /** Lens view of the hunter's authoritative visible multiplayer world. */
    public static BufferedImage network(SceneRenderer camera,SceneRenderer hunter,
            HouseMap map,List<SceneRenderer.RemoteBody> hunters,
            SceneRenderer.Apparition apparition,double clock,
            List<SceneRenderer.WorldMark> marks,EquipmentTiers.Tier tier){
        Objects.requireNonNull(camera);Objects.requireNonNull(map);
        Objects.requireNonNull(hunters);Objects.requireNonNull(marks);
        renderNetwork(camera,hunter,map,hunters,apparition,clock,marks,tier);
        return finished(camera,tier);
    }
    private static void renderNetwork(SceneRenderer camera,SceneRenderer hunter,
            HouseMap map,List<SceneRenderer.RemoteBody> hunters,
            SceneRenderer.Apparition apparition,double clock,
            List<SceneRenderer.WorldMark> marks,EquipmentTiers.Tier tier){
        camera.photographicSoftwareRaster();
        camera.copyPhotographicWorld(hunter,tier);
        camera.drawNetwork(map,hunters,apparition,clock,marks);
    }
    public static BufferedImage previewNetwork(SceneRenderer camera,SceneRenderer hunter,
            HouseMap map,List<SceneRenderer.RemoteBody> hunters,
            SceneRenderer.Apparition apparition,double clock,
            List<SceneRenderer.WorldMark> marks,EquipmentTiers.Tier tier){
        Objects.requireNonNull(camera);Objects.requireNonNull(map);
        Objects.requireNonNull(hunters);Objects.requireNonNull(marks);
        renderNetwork(camera,hunter,map,hunters,apparition,clock,marks,tier);
        return preview(camera);
    }

    /** Solo uses the same world renderer and actual current session models. */
    public static BufferedImage solo(SceneRenderer camera,SceneRenderer hunter,
            HouseMap map,InvestigationSession session,double clock,
            List<SceneRenderer.RemoteBody> allies,EquipmentTiers.Tier tier){
        Objects.requireNonNull(camera);Objects.requireNonNull(map);
        Objects.requireNonNull(session);Objects.requireNonNull(allies);
        renderSolo(camera,hunter,map,session,clock,allies,tier);
        return finished(camera,tier);
    }
    private static void renderSolo(SceneRenderer camera,SceneRenderer hunter,
            HouseMap map,InvestigationSession session,double clock,
            List<SceneRenderer.RemoteBody> allies,EquipmentTiers.Tier tier){
        camera.photographicSoftwareRaster();
        camera.copyPhotographicWorld(hunter,tier);
        camera.draw(map,session,clock,allies);
    }
    public static BufferedImage previewSolo(SceneRenderer camera,SceneRenderer hunter,
            HouseMap map,InvestigationSession session,double clock,
            List<SceneRenderer.RemoteBody> allies,EquipmentTiers.Tier tier){
        Objects.requireNonNull(camera);Objects.requireNonNull(map);
        Objects.requireNonNull(session);Objects.requireNonNull(allies);
        renderSolo(camera,hunter,map,session,clock,allies,tier);
        return preview(camera);
    }
}
