package org.beckenburg;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;

/** Real human import dock: read native source, review topology and publish runtime GLB. */
public final class AssetImportTopologyGuiTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static Object field(Object owner,String name)throws Exception{
        var f=owner.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(owner);
    }
    private static void await(java.util.function.BooleanSupplier done)throws Exception{
        long end=System.nanoTime()+30_000_000_000L;
        while(System.nanoTime()<end){var ready=new java.util.concurrent.atomic.AtomicBoolean();
            SwingUtilities.invokeAndWait(()->ready.set(done.getAsBoolean()));if(ready.get())return;Thread.sleep(30);}
        throw new AssertionError("Human import worker did not complete");
    }
    public static void main(String[] args)throws Exception{
        Path output=Path.of("build/editor-agent-evidence/mesh-topology-import.png").toAbsolutePath();Files.createDirectories(output.getParent());
        Path home=Files.createTempDirectory("topology-human-import-");String oldHome=System.getProperty("user.home");
        System.setProperty("user.home",home.toString());var ui=new AtomicReference<AssetImportDialog>();var published=new AtomicReference<String>();
        try{
            Path source=home.resolve("tetra.obj");Files.writeString(source,"""
                o shell
                v 0 0 0
                v 1 0 0
                v 0 1 0
                v 0 0 1
                vt 0 0
                vt 1 0
                vt 0 1
                f 1/1 3/3 2/2
                f 1/1 2/2 4/3
                f 1/1 4/3 3/2
                f 2/2 3/3 4/1
                """);byte[] original=Files.readAllBytes(source);
            SwingUtilities.invokeAndWait(()->{
                var dock=new AssetImportDialog(null,published::set);ui.set(dock);
                try{((JDialog)field(dock,"dialog")).setVisible(true);}catch(Exception e){throw new RuntimeException(e);}
                dock.loadSource(source);
            });
            await(()->{try{return ((JButton)field(ui.get(),"apply")).isEnabled();}catch(Exception e){throw new RuntimeException(e);}});
            SwingUtilities.invokeAndWait(()->{try{
                var dock=ui.get();var report=((JTextArea)field(dock,"details")).getText();
                check(report.contains("4 vertices / 12 half-edges / 1 islands / 0 boundary edges"),"Native import topology counts: "+report);
                check(report.contains("0 issues"),"Oriented closed source is valid");
                check(!report.contains("\\n"),"Human report uses real newlines, not escaped text");
                var dialog=(JDialog)field(dock,"dialog");var content=dialog.getContentPane();
                var image=new BufferedImage(content.getWidth(),content.getHeight(),BufferedImage.TYPE_INT_ARGB);
                var g=image.createGraphics();try{content.printAll(g);}finally{g.dispose();}
                ImageIO.write(image,"png",output.toFile());
                ((JButton)field(dock,"apply")).doClick();
            }catch(Exception e){throw new RuntimeException(e);}});
            await(()->published.get()!=null);
            String id=published.get();check(ImportedAssetLibrary.approved(id),"Actual human publish registered native asset");
            check(GlbProps.model(id).triangles().size()==4,"Actual game decoder resolves published four-face mesh");
            check(Arrays.equals(original,Files.readAllBytes(source))&&Arrays.equals(original,Files.readAllBytes(ImportedAssetLibrary.root().resolve(id+".source.obj"))),"Human topology review preserves exact source and archived bytes");
            System.out.println("PASS actual human mesh import: half-edge review, real UI PNG, native GLB publish/game readback and exact source preservation");
        }finally{
            SwingUtilities.invokeAndWait(()->{if(ui.get()!=null)try{((JDialog)field(ui.get(),"dialog")).dispose();}catch(Exception e){throw new RuntimeException(e);}});
            System.setProperty("user.home",oldHome);
            try(var paths=Files.walk(home)){for(var p:paths.sorted(java.util.Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}
        }
    }
}
