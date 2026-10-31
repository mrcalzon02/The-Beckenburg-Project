package org.beckenburg;

import java.awt.image.BufferedImage;

/** Confirm old imported covers retain original geometry and readable source
 * details in GPU and software renders. Diagnostic only, never a visual release gate.
 */
public final class ImportedBookMaterialTest {
    private ImportedBookMaterialTest(){}
    static void check(boolean ok,String reason){if(!ok)throw new AssertionError(reason);}
    public static void main(String[] args){
        check(GlbProps.bookCoverMaterial("book_mp_1"),"original book source selected");
        check(GlbProps.bookCoverMaterial("book_mp_5"),"every book cover variant selected");
        check(GlbProps.bookCoverMaterial("open_book_mp_1"),"open book source selected");
        check(!GlbProps.bookCoverMaterial("shelf_mp_6"),"wood shelving unchanged");
        check(!GlbProps.bookCoverMaterial("level_loose_book_worn"),"authored book unchanged");
        check(GlbProps.bookTone(0)==0&&GlbProps.bookTone(255)==255,
            "display transfer preserves black and white endpoints");
        check(GlbProps.bookTone(35)>70&&GlbProps.bookTone(90)>110,
            "deep leather texture colors become legible in low light");
        var original=new BufferedImage(2,1,BufferedImage.TYPE_INT_ARGB);
        original.setRGB(0,0,0x80231918);original.setRGB(1,0,0x00000000);
        int first=original.getRGB(0,0);
        var corrected=GlbProps.readableBookCover(original);
        check(original.getRGB(0,0)==first,"source RGB bytes remain untouched");
        check(corrected!=original,"runtime material uses an independent image");
        check((corrected.getRGB(0,0)>>>24)==128,"original cover transparency preserved");
        check((corrected.getRGB(0,0)&0xffffff)>(first&0xffffff),
            "aged leather RGB detail is more visible");
        var mesh=GlbProps.model("book_mp_1");
        check(mesh.triangles().size()>40,"native book geometry remains a GLB mesh");
        var material=mesh.triangles().get(0).material();
        check(GlbProps.registeredTexture(material)&&
            GlbProps.materialName(material).equals("books_mp_1"),
            "the original book UV/material slot remains bound");
        System.out.println("PASS imported book source geometry, preserved UV, and non-destructive material display");
    }
}
