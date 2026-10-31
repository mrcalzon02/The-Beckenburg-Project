package org.beckenburg;

/** Advisory asset diagnostic: visual model choice must not block gameplay releases. */
public final class DotsProjectorModelDiagnosticTest {
    private static void check(boolean pass,String why){if(!pass)throw new AssertionError(why);}
    public static void main(String[] args){
        for(var tier:EquipmentTiers.Tier.values()){
            String mesh=SceneRenderer.dotsDeployedMesh(tier);
            check(mesh.equals(EquipmentModelCatalog.variant("DOTS_PROJECTOR",tier,"deployed"))
                &&EquipmentModelCatalog.approved(mesh),
                "projector must use the original catalog's deployed tier: "+tier);
            check(!GlbProps.model(mesh).triangles().isEmpty()
                &&Double.isFinite(SceneRenderer.castleBounds(mesh)[1]),
                "deployed projector model needs valid floor-contact geometry: "+tier);
            System.out.println("DOTS "+tier+" deployed visual: "+mesh);
        }
        System.out.println("DotsProjectorModelDiagnosticTest PASS");
    }
}
