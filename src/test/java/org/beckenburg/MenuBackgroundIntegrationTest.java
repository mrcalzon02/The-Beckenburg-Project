package org.beckenburg;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.*;

/** Headless packaging, compositing, and left-navigation acceptance. */
public final class MenuBackgroundIntegrationTest {
    private MenuBackgroundIntegrationTest(){}
    public static void main(String[] args) {
        if(MenuBackgrounds.count()!=10)throw new AssertionError("Expected all ten committed menu scenes");
        for(int i=0;i<MenuBackgrounds.count();i++){
            BufferedImage image=MenuBackgrounds.image(i);
            if(image.getWidth()!=1920||image.getHeight()!=1080)
                throw new AssertionError("Scene "+i+" is not the committed 1920x1080 JPEG");
        }
        if(MenuBackgrounds.sceneFor("start.menu")==MenuBackgrounds.sceneFor("options"))
            throw new AssertionError("Main and options must use distinct backgrounds");
        JButton play=PsxHorrorUi.button("START SOLO INVESTIGATION");
        play.setName("start.solo");
        int[] activations={0};
        play.addActionListener(e->activations[0]++);
        JPanel screen=PsxMenuScreen.screen("start.menu","THE BECKENBURG PROJECT",
            "Select mode",play);
        for(int[] size:new int[][]{{1060,725},{760,510}}){
            screen.setSize(size[0],size[1]);
            screen.doLayout();
            JPanel sheet=(JPanel)screen.getComponent(0);
            if(sheet.getX()>25||sheet.getWidth()>size[0]*0.65)
                throw new AssertionError("Main menu is not left-anchored at "+size[0]);
            if(sheet.getX()+sheet.getWidth()>=size[0]-80)
                throw new AssertionError("Scenic background is obscured at "+size[0]);
            BufferedImage capture=new BufferedImage(size[0],size[1],BufferedImage.TYPE_INT_RGB);
            Graphics2D g=capture.createGraphics();
            screen.paint(g);
            g.dispose();
            if((capture.getRGB(size[0]-10,size[1]/2)&0x00ffffff)==0)
                throw new AssertionError("No artwork on uncovered right side");
        }
        play.doClick();
        if(activations[0]!=1)throw new AssertionError("Real navigation action was broken");
        JPanel dense=PsxMenuScreen.screen("options","OPTIONS","All settings",
            new JLabel("GRAPHICS SETTINGS"));
        dense.setSize(760,510);
        dense.doLayout();
        if(dense.getComponent(0).getWidth()<650)
            throw new AssertionError("Dense settings panel is unusably narrow");
        System.out.println("PASS all ten packaged scenes, left menu, responsive layout and live button");
    }
}
