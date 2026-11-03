package org.beckenburg;

import org.beckenburg.editor.EditorGlbAuthoringTest;

/** Test the game decoder itself, including negative controls against the prior runtime. */
public final class GlbSourceAuthorityTest {
    public static void main(String[] args)throws Exception{
        var model=GlbProps.decode(EditorGlbAuthoringTest.externalFixture());
        if(args.length==0||args[0].equals("matrix")){
            var v=model.triangles().get(0).a();
            if(Math.abs(v.x()-3)>1e-5||Math.abs(v.z()-1)>1e-5)throw new AssertionError("Actual runtime ignores original node matrix/pivot: "+v);
        }
        if(args.length==0||args[0].equals("uv")){
            int pixel=GlbProps.sampleRgba(model.triangles().get(0).material(),.25,.25);
            if(pixel!=0xffff0000)throw new AssertionError("Actual external GLB has vertically flipped original PNG: "+Integer.toHexString(pixel));
        }
        System.out.println("PASS actual runtime original matrix/ancestor pivot and external GLB upper-left PNG");
    }
}
