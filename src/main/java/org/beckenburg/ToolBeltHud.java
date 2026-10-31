package org.beckenburg;

import java.awt.*;
import java.util.Locale;
import javax.swing.ImageIcon;

/** Shared solo/LAN four-slot tool belt. Readings come only from game/host state. */
public final class ToolBeltHud {
    private ToolBeltHud(){}
    public static boolean placeable(String id){
        return switch(normal(id)){
            case "DOTS_PROJECTOR","VIDEO_CAMERA","GHOST_WRITING_BOOK","FLASHLIGHT",
                "FIRELIGHT","SALT","EMF_ARRAY","THERMAL_BEACON","MOTION_SENSOR",
                "SOUND_SENSOR","LASER_GRID","TRIPOD" -> true;
            default -> false;
        };
    }
    public static boolean switchable(String id){
        return switch(normal(id)){
            case "UV_LIGHT","UV_LAMP","FLASHLIGHT","FIRELIGHT","HEAD_GEAR" -> true;
            default -> false;
        };
    }
    public static boolean repeatable(String id){
        return switch(normal(id)){
            case "EMF_READER","EMF","THERMOMETER","SPIRIT_BOX","SPECTRAL_PRISM",
                "ANCHOR_TRIANGULATOR","RESONANCE_TUNER","BAROMETRIC_PROBE",
                "SEISMIC_RECORDER","RITUAL_ANCHOR_METER","WARD_STABILITY_PROBE",
                "OBJECT_IMPRINT_SCANNER","SPECTRAL_FREQUENCY_ANALYZER",
                "PARABOLIC_MICROPHONE" -> true;
            default -> false;
        };
    }
    private static String normal(String name){return name==null?"":name.toUpperCase(Locale.ROOT);}
    public static final class Grip {
        private boolean held;
        private double amount;
        public boolean held(){return held;}
        public double amount(){return amount;}
        public void hold(){held=true;}
        public void release(){held=false;}
        public void reset(){held=false;amount=0;}
        public void advance(){
            amount+=((held?1.0:0.0)-amount)*.48;
            if(!held&&amount<.01)amount=0;
        }
    }
    public static void draw(Graphics2D original,int width,int height,String[] slots,
            int active,String reading,boolean held,boolean powered,boolean placing,int rotation){
        if(original==null||slots==null||slots.length!=4||active<0||active>=4
                ||width<310||height<220)return;
        Graphics2D g=(Graphics2D)original.create();
        try{
            int cell=Math.min(72,Math.max(48,(width-40)/4));
            int total=cell*4+15,x0=(width-total)/2,y=height-cell-12;
            String id=normal(slots[active]);
            String actions=placing?"LMB PLACE  WHEEL ROTATE  G CANCEL":
                "LMB USE"+(switchable(id)?"  RMB POWER":"")+
                (placeable(id)?"  G PLACE":"")+"  E INTERACT";
            int barW=Math.min(width-24,Math.max(310,total+22));
            g.setColor(new Color(8,17,23,228));
            g.fillRoundRect((width-barW)/2,y-34,barW,29,7,7);
            g.setFont(new Font(Font.MONOSPACED,Font.BOLD,11));
            while(actions.length()>0&&g.getFontMetrics().stringWidth(actions)>barW-15)
                actions=actions.substring(0,actions.length()-1);
            g.setColor(new Color(211,231,213));
            g.drawString(actions,(width-barW)/2+9,y-15);
            for(int i=0;i<4;i++){
                int sx=x0+i*(cell+5);
                g.setColor(new Color(12,20,28,229));
                g.fillRoundRect(sx,y,cell,cell,7,7);
                g.setColor(i==active?new Color(125,244,180):new Color(100,122,127));
                g.setStroke(new BasicStroke(i==active?3f:1f));
                g.drawRoundRect(sx+1,y+1,cell-2,cell-2,7,7);
                String tool=normal(slots[i]);
                ImageIcon icon=ItemIconAtlas.get(tool.toLowerCase(Locale.ROOT),Math.max(24,cell-18));
                if(icon!=null)icon.paintIcon(null,g,sx+(cell-icon.getIconWidth())/2,y+6);
                else{
                    String text=tool.replace('_',' ');
                    g.setColor(new Color(211,220,207));
                    g.setFont(new Font(Font.MONOSPACED,Font.BOLD,9));
                    g.drawString(text.substring(0,Math.min(9,text.length())),sx+5,y+cell/2);
                }
                g.setFont(new Font(Font.MONOSPACED,Font.BOLD,11));
                g.setColor(new Color(188,237,196));g.drawString(Integer.toString(i+1),sx+5,y+14);
            }
            String status=id.replace('_',' ')+(switchable(id)?powered?" / ON":" / OFF":"")+
                (held?" / ACTIVE":"");
            g.setColor(new Color(9,19,22,220));
            int statusW=Math.min(width-24,Math.max(170,g.getFontMetrics().stringWidth(status)+16));
            g.fillRoundRect((width-statusW)/2,y-59,statusW,22,5,5);
            g.setColor(new Color(181,233,200));
            g.drawString(status,(width-statusW)/2+8,y-44);
            if(reading!=null&&!reading.isBlank()&&held){
                String clipped=reading.substring(0,Math.min(reading.length(),Math.max(18,(width-48)/8)));
                int rw=Math.min(width-24,g.getFontMetrics().stringWidth(clipped)+16);
                g.setColor(new Color(11,27,24,235));
                g.fillRoundRect((width-rw)/2,y-84,rw,22,5,5);
                g.setColor(new Color(140,235,183));
                g.drawString(clipped,(width-rw)/2+8,y-69);
            }
            if(placing){
                int cx=width/2,cy=height/2;
                ImageIcon preview=ItemIconAtlas.get(id.toLowerCase(Locale.ROOT),64);
                Composite old=g.getComposite();
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,.45f));
                if(preview!=null)preview.paintIcon(null,g,cx-32,cy-32);
                else{g.setColor(new Color(79,239,133));g.fillRoundRect(cx-27,cy-24,54,48,6,6);}
                g.setComposite(old);
                g.setColor(new Color(124,241,160));g.setStroke(new BasicStroke(2f));
                g.drawOval(cx-40,cy-40,80,80);
                double a=Math.toRadians(Math.floorMod(rotation,360)-90);
                g.drawLine(cx,cy,cx+(int)(Math.cos(a)*49),cy+(int)(Math.sin(a)*49));
                g.drawString("ROT "+Math.floorMod(rotation,360)+"°",cx+45,cy+6);
            }
        }finally{g.dispose();}
    }
}
