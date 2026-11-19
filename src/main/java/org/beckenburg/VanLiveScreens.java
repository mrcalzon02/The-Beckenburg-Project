package org.beckenburg;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Actual illuminated, independently textured faces of the physical VAN monitors.
 * Data is passed from the same host/solo model that powers the expanded station
 * UI. This class never samples ghost identity, private hidden evidence or AI state.
 *
 * All screens are small and event/throttle driven. No second scene renderer runs
 * inside a screen, and unchanged frames never force world mesh rebuilds.
 */
public final class VanLiveScreens {
    private VanLiveScreens(){}
    public enum Panel { MAP, CAMERA, TEAM, ACTIVITY, SOUND, CONTRACT }
    public static final int WIDTH=480, HEIGHT=270;
    private static final int LAYOUT_W=320, LAYOUT_H=180;
    private static final Color BACK=new Color(8,18,26), INK=new Color(208,237,226),
        GREEN=new Color(123,230,179), AMBER=new Color(240,206,137), DIM=new Color(111,157,163);
    private static final java.util.EnumMap<Panel,Integer> MATERIALS=new java.util.EnumMap<>(Panel.class);
    private static final java.util.EnumMap<Panel,BufferedImage> LATEST=
        new java.util.EnumMap<>(Panel.class);
    /** Identical pixel source for the physical face and its expanded station view. */
    public static synchronized BufferedImage snapshot(Panel panel){return LATEST.get(panel);}
    /** On departure, invalidate the live feed and blank the last GPU texture
     * exactly once. No scene is rendered, photographed or recorded without
     * an in-van viewer, and the screen cannot leak a stale outside-visible frame. */
    public static synchronized void suspendCamera(){
        if(LATEST.remove(Panel.CAMERA)==null)return;
        Integer material=MATERIALS.get(Panel.CAMERA);
        if(material!=null)GlbProps.updateLiveSurface(material,
            new BufferedImage(WIDTH,HEIGHT,BufferedImage.TYPE_INT_RGB));
    }
    public static void drawExpanded(Graphics2D g,Panel panel,int viewportWidth,int viewportHeight){
        BufferedImage image=snapshot(panel);
        int width=Math.min(viewportWidth-40,Math.max(320,viewportWidth*3/4));
        int height=Math.min(viewportHeight-70,Math.max(180,width*HEIGHT/WIDTH));
        width=Math.min(width,height*WIDTH/HEIGHT);
        int x=(viewportWidth-width)/2,y=(viewportHeight-height)/2;
        g.setColor(new Color(5,11,17,246));
        g.fillRoundRect(x-12,y-31,width+24,height+56,10,10);
        g.setColor(GREEN);
        g.drawRoundRect(x-12,y-31,width+23,height+55,10,10);
        g.setFont(new Font(Font.MONOSPACED,Font.BOLD,14));
        g.drawString(panel.name()+" / E OR ESC TO CLOSE",x,y-11);
        if(image!=null)g.drawImage(image,x,y,width,height,null);
        else{
            g.setColor(DIM);
            g.drawString("AWAITING LIVE DISPLAY",x+18,y+40);
        }
    }


    public static Panel forPurpose(String purpose){
        return switch(purpose){
            case "site map" -> Panel.MAP;
            case "camera feed" -> Panel.CAMERA;
            case "sanity telemetry" -> Panel.TEAM;
            case "haunting activity" -> Panel.ACTIVITY;
            case "sound telemetry" -> Panel.SOUND;
            case "contract objective board" -> Panel.CONTRACT;
            default -> throw new IllegalArgumentException("no monitor surface for "+purpose);
        };
    }
    public static synchronized int material(Panel panel){
        if(panel==Panel.MAP)return GlbProps.VAN_MAP_MATERIAL;
        return MATERIALS.computeIfAbsent(panel,p->
            GlbProps.liveSurfaceMaterial("van live screen "+p.name().toLowerCase(),
                WIDTH,HEIGHT));
    }
    /** Render in a sparse virtual layout with higher density and antialiased glyphs. */
    private static Graphics2D panelGraphics(BufferedImage image){
        Graphics2D g=image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.scale(WIDTH/(double)LAYOUT_W,HEIGHT/(double)LAYOUT_H);
        return g;
    }
    /** Never let a text line silently overflow its physical screen. */
    private static void drawFitted(Graphics2D g,String message,int x,int y,int maxWidth){
        String line=clip(message,200);
        FontMetrics fm=g.getFontMetrics();
        if(fm.stringWidth(line)>maxWidth){
            int end=line.length();
            while(end>0&&fm.stringWidth(line.substring(0,end)+"...")>maxWidth)end--;
            line=line.substring(0,end)+"...";
        }
        g.drawString(line,x,y);
    }
    private static BufferedImage canvas(String title,String status){
        BufferedImage out=new BufferedImage(WIDTH,HEIGHT,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=panelGraphics(out);
        try{
            g.setColor(BACK);g.fillRect(0,0,LAYOUT_W,LAYOUT_H);
            g.setColor(new Color(39,71,77));g.drawRect(1,1,LAYOUT_W-3,LAYOUT_H-3);
            g.setColor(GREEN);g.fillRect(0,0,LAYOUT_W,4);
            g.setFont(new Font(Font.MONOSPACED,Font.BOLD,21));
            drawFitted(g,title,12,27,LAYOUT_W-24);
            g.setColor(DIM);g.drawLine(12,35,LAYOUT_W-12,35);
            g.setFont(new Font(Font.MONOSPACED,Font.PLAIN,14));
            drawFitted(g,status,12,LAYOUT_H-8,LAYOUT_W-24);
        }finally{g.dispose();}
        return out;
    }
    private static String clip(String str,int max){
        if(str==null||str.isBlank())return "--";
        String clean=str.replace('\n',' ').replace('\r',' ').strip();
        return clean.length()<=max?clean:clean.substring(0,Math.max(0,max-3))+"...";
    }
    private static void publish(Panel panel,BufferedImage image){
        if(panel==Panel.MAP)throw new IllegalArgumentException("map uses existing live map");
        GlbProps.updateLiveSurface(material(panel),image);
        synchronized(VanLiveScreens.class){LATEST.put(panel,image);}
    }
    public static void contract(String site,String difficulty,List<String> tasks){
        BufferedImage image=canvas("CONTRACT / LIVE",clip(site,34)+"  /  "+clip(difficulty,15));
        Graphics2D g=panelGraphics(image);
        try{
            g.setColor(AMBER);g.setFont(new Font(Font.MONOSPACED,Font.BOLD,20));
            drawFitted(g,site,12,60,LAYOUT_W-24);
            g.setFont(new Font(Font.MONOSPACED,Font.BOLD,15));
            g.setColor(INK);g.drawString("OBJECTIVES",12,82);
            g.setFont(new Font(Font.MONOSPACED,Font.PLAIN,16));
            if(tasks==null||tasks.isEmpty()){
                g.setColor(DIM);drawFitted(g,"Awaiting contract tasks",12,104,LAYOUT_W-24);
            }else for(int i=0;i<Math.min(tasks.size(),3);i++){
                g.setColor(tasks.get(i).startsWith("[DONE]")?GREEN:INK);
                drawFitted(g,tasks.get(i),12,103+i*20,LAYOUT_W-24);
            }
        }finally{g.dispose();}
        publish(Panel.CONTRACT,image);
    }
    public static void team(VanTelemetry.Display values){
        BufferedImage image=canvas("TEAM / SANITY",
            values==null?"AWAITING HOST METERS":"SAMPLED T"+values.tick());
        Graphics2D g=panelGraphics(image);
        try{
            int[] sanity=values==null?new int[]{-1,-1,-1,-1,-1,-1}:values.sanity();
            g.setFont(new Font(Font.MONOSPACED,Font.BOLD,16));
            for(int i=0;i<6;i++){
                int v=sanity[i],y=52+i*19;
                g.setColor(INK);g.drawString("H"+(i+1),13,y);
                g.setColor(new Color(41,57,64));g.fillRect(51,y-9,219,10);
                if(v>=0){
                    g.setColor(v<30?new Color(245,128,118):GREEN);
                    g.fillRect(51,y-9,219*v/100,10);
                }
                g.setColor(v<0?DIM:INK);
                g.drawString(v<0?"--":v+"%",277,y);
            }
        }finally{g.dispose();}
        publish(Panel.TEAM,image);
    }
    public static void activity(VanTelemetry.Display values,List<Integer> samples){
        BufferedImage image=canvas("ACTIVITY / INDIRECT",
            values==null?"AWAITING HOST METERS":"NOISY SENSOR  /  T"+values.tick());
        Graphics2D g=panelGraphics(image);
        try{
            g.setFont(new Font(Font.MONOSPACED,Font.BOLD,24));
            g.setColor(AMBER);g.drawString(values==null?"-- / 10":values.activity()+" / 10",12,61);
            int x=14,y=74,w=290,h=68;
            g.setColor(new Color(23,40,49));g.fillRect(x,y,w,h);
            g.setColor(new Color(59,77,85));
            for(int n=1;n<4;n++)g.drawLine(x,y+n*h/4,x+w,y+n*h/4);
            List<Integer> safe=samples==null?List.of():new ArrayList<>(samples);
            int count=safe.size(),index=0,lastX=-1,lastY=-1;
            g.setColor(GREEN);
            for(int value:safe){
                int px=x+(count<=1?0:index*(w-2)/(count-1));
                int py=y+h-3-Math.max(0,Math.min(10,value))*(h-6)/10;
                if(lastX>=0)g.drawLine(lastX,lastY,px,py);
                lastX=px;lastY=py;index++;
            }
            g.setColor(DIM);g.setFont(new Font(Font.MONOSPACED,Font.PLAIN,13));
            drawFitted(g,"Instrument estimate only",12,155,LAYOUT_W-24);
        }finally{g.dispose();}
        publish(Panel.ACTIVITY,image);
    }
    public static void sound(Map<Integer,VanSiteMapScreen.Reading> readings,
                             List<VanSensorStations.Station> sensors,String caption){
        BufferedImage image=canvas("SOUND / MOTION",clip(caption,42));
        Graphics2D g=panelGraphics(image);
        try{
            g.setFont(new Font(Font.MONOSPACED,Font.BOLD,16));int row=0;
            if(sensors!=null)for(var sensor:sensors){
                if(!sensor.tool().equals("SOUND_SENSOR")&&
                    !sensor.tool().equals("MOTION_SENSOR"))continue;
                if(row>=5)break;
                VanSiteMapScreen.Reading reading=readings==null?null:readings.get(sensor.id());
                String line="R"+sensor.room()+" "+sensor.tool().replace("_SENSOR","")+
                    ": "+(reading==null?"STANDBY":reading.text());
                g.setColor(reading==null?DIM:GREEN);
                drawFitted(g,line,12,57+row*22,LAYOUT_W-24);
                row++;
            }
            if(row==0){
                g.setColor(DIM);g.drawString("NO DEPLOYED SENSOR ARRAY",12,67);
                drawFitted(g,"Install and activate field nodes",12,93,LAYOUT_W-24);
            }
        }finally{g.dispose();}
        publish(Panel.SOUND,image);
    }
    /** Receives already-rendered, host-authorized video only. This never renders
     * an additional scene, and cannot expose undisclosed ghosts. */
    public static void camera(BufferedImage source,String label){
        BufferedImage image=canvas("CAMERA / LIVE",clip(label,42));
        Graphics2D g=panelGraphics(image);
        try{
            if(source!=null){
                g.drawImage(source,10,41,300,119,null);
                g.setColor(GREEN);g.drawRect(9,40,301,120);
            }else{
                g.setColor(DIM);g.setFont(new Font(Font.MONOSPACED,Font.BOLD,25));
                g.drawString("NO CAMERA SIGNAL",48,107);
            }
        }finally{g.dispose();}
        publish(Panel.CAMERA,image);
    }
}
