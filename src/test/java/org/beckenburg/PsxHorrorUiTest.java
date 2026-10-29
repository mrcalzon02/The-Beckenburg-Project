package org.beckenburg;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
/** Headless proof that original archive artwork decodes and paints inside Java Swing. */
public final class PsxHorrorUiTest{
 public static void main(String[] args){
  if(PsxHorrorUi.imageCount()<60)throw new AssertionError("Image catalog incomplete: "+PsxHorrorUi.imageCount());
  BufferedImage canvas=new BufferedImage(900,600,BufferedImage.TYPE_INT_ARGB);
  Graphics2D g=canvas.createGraphics();
  for(int scale:new int[]{1,2,3}){
   PsxHorrorUi.paintPatch(g,"window",30,30,600,400,scale);
   PsxHorrorUi.paintPatch(g,"button_normal",20,450,340,48,scale);
   PsxHorrorUi.paintPatch(g,"panel",380,440,300,130,scale);
   PsxHorrorUi.paintPatch(g,"tab_selected",20,510,160,38,scale);
  }
  g.dispose();
  if((canvas.getRGB(35,35)>>>24)==0)throw new AssertionError("Window not painted");
  JButton b=PsxHorrorUi.button("READY");b.setSize(200,44);b.doLayout();
  BufferedImage surface=new BufferedImage(200,44,BufferedImage.TYPE_INT_ARGB);
  b.paint(surface.getGraphics());
  if((surface.getRGB(5,5)>>>24)==0)throw new AssertionError("Button not painted");
  JTextField field=new JTextField("demo");PsxHorrorUi.styleTextField(field);
  JComboBox<String>[] fields=new JComboBox[6];
  for(int i=0;i<fields.length;i++)fields[i]=new JComboBox<>(new String[]{"Option A","Option B"});
  JPanel form=InvestigationGame.soloContractForm(
      fields[0],fields[1],fields[2],fields[3],fields[4],fields[5]);
  form.setSize(660,380);form.doLayout();
  for(JComboBox<String> combo:fields){
   if(combo.getHeight()<26||combo.getHeight()>48)
    throw new AssertionError("Solo contract dropdown stretched to "+combo.getHeight()+" pixels");
   if(combo.getY()>310)
    throw new AssertionError("Solo contract fields are not grouped at the top");
  }
  JLabel label=(JLabel)form.getComponent(0);
  if(label.getForeground().getRed()<200)
   throw new AssertionError("Solo field label is not light on dark");
  JTabbedPane tabs=new JTabbedPane();
  PsxHorrorUi.styleTabs(tabs);
  tabs.addTab("CONTRACT",PsxMenuScreen.scroll(form));
  tabs.setSize(720,430);tabs.doLayout();
  BufferedImage tabImage=new BufferedImage(720,430,BufferedImage.TYPE_INT_RGB);
  Graphics2D tabGraphics=tabImage.createGraphics();
  tabs.paint(tabGraphics);tabGraphics.dispose();
  int background=tabImage.getRGB(25,400);
  int red=(background>>16)&255,green=(background>>8)&255,blue=background&255;
  if(Math.max(red,Math.max(green,blue))>100)
   throw new AssertionError("Solo contract tab content has a bright platform background: #"+
       Integer.toHexString(background&0xffffff));
  System.out.println("PASS PSX Horror UI / "+PsxHorrorUi.imageCount()+
      " images / 3 scales / dark contract tab / compact readable dropdowns");
 }
}
