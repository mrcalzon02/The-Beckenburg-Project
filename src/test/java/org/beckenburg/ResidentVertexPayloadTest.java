package org.beckenburg;

/** Functional regression: a resident resource revision changes if and only if
 * its exact immutable GPU vertex payload changes. No GPU hardware required. */
public final class ResidentVertexPayloadTest {
    private ResidentVertexPayloadTest(){}
    private static void check(boolean condition,String message){
        if(!condition)throw new AssertionError(message);
    }
    public static void main(String[] args){
        // One real GPU vertex: xyz, UV, RGB.
        float[] original={1f,2f,3f,.25f,.75f,.1f,.2f,.3f};
        check(SceneRenderer.sameResidentVertices(original,original),
            "identical immutable VBO must be reusable");
        check(SceneRenderer.sameResidentVertices(original,original.clone()),
            "a conservative lighting rebuild with identical data must not upload");
        float[] colour=original.clone();colour[5]=.4f;
        check(!SceneRenderer.sameResidentVertices(original,colour),
            "a changed door shadow must revise the VBO");
        float[] uv=original.clone();uv[3]=.5f;
        check(!SceneRenderer.sameResidentVertices(original,uv),
            "a real material UV change must revise the VBO");
        float[] position=original.clone();position[0]=4f;
        check(!SceneRenderer.sameResidentVertices(original,position),
            "real topology movement must revise the VBO");
        check(!SceneRenderer.sameResidentVertices(original,new float[0]),
            "a changed vertex count must revise the VBO");
        float[] signedZero=original.clone(),positiveZero=original.clone();
        signedZero[6]=-0f;positiveZero[6]=0f;
        check(!SceneRenderer.sameResidentVertices(signedZero,positiveZero),
            "raw signed-zero differences must not be hidden");
        float[] nanA=original.clone(),nanB=original.clone();
        nanA[7]=Float.intBitsToFloat(0x7fc00001);
        nanB[7]=Float.intBitsToFloat(0x7fc00002);
        check(!SceneRenderer.sameResidentVertices(nanA,nanB),
            "raw NaN payload differences must not be hidden");
        System.out.println("PASS resident VBO payload identity and change detection");
    }
}
