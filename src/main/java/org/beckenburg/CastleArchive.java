package org.beckenburg;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;

/** Readable Common Archive and its prototype resident; no combat/quest/AI authority. */
public final class CastleArchive {
    private CastleArchive(){}
    public static final double KEEPER_X=36,KEEPER_Z=10.35;
    public static final List<CastleHubDecor.Accent> ACCENTS=List.of(
        new CastleHubDecor.Accent("level_trace_records",35.70,.754,11.80,1,0),
        new CastleHubDecor.Accent("level_trace_journal",36.45,.754,11.50,1,.10));
    public static List<CastleHubMap.Furnishing> furnishings(){return List.of(
        new CastleHubMap.Furnishing("level_npc_matthis_rook_proxy",KEEPER_X,KEEPER_Z,1,0,.42,.28));}
    public static final class Reader {
        private boolean opened;private int page,scroll;
        public boolean opened(){return opened;}public int page(){return page;}public int scroll(){return scroll;}
        public void open(){opened=true;page=0;scroll=0;}
        public void close(){opened=false;}
        public void turn(int direction){page=Math.floorMod(page+direction,WorldLore.PAGES.size());scroll=0;}
        public void scroll(int amount){scroll=Math.max(0,Math.min(1000,scroll+amount));}
        public void key(int key){switch(key){
            case KeyEvent.VK_ESCAPE,KeyEvent.VK_E->close();
            case KeyEvent.VK_LEFT,KeyEvent.VK_PAGE_UP->turn(-1);
            case KeyEvent.VK_RIGHT,KeyEvent.VK_PAGE_DOWN->turn(1);
            case KeyEvent.VK_UP->scroll(-1);case KeyEvent.VK_DOWN->scroll(1);
            case KeyEvent.VK_HOME->scroll=0;
            default->{} // Movement and actions stay paused while the book is open.
        }}
    }
    private static BufferedImage portrait;
    public static synchronized BufferedImage portrait(){
        if(portrait==null)try(var in=CastleArchive.class.getResourceAsStream(WorldLore.PORTRAIT)){
            if(in==null)throw new IllegalStateException("Missing approved lorekeeper portrait");
            portrait=ImageIO.read(in);if(portrait==null)throw new IllegalStateException("Unreadable lorekeeper portrait");
        }catch(IOException e){throw new IllegalStateException("Lorekeeper portrait",e);}
        return portrait;
    }
    /** The approved source art is the castle resident, not the old proxy mesh. */
    public static int paintKeeper(int[] pixels,double[] depth,double cameraX,double cameraZ,
            double yaw,double pitch,double eyeHeight,int width,int height,double focal){
        return paintKeeper(pixels,depth,cameraX,cameraZ,yaw,pitch,eyeHeight,width,height,focal,portrait());
    }
    /** Injected image supports occlusion tests independently of asset pixel colors. */
    static int paintKeeper(int[] pixels,double[] depth,double cameraX,double cameraZ,
            double yaw,double pitch,double eyeHeight,int width,int height,double focal,BufferedImage sprite){
        if(sprite==null||pixels.length!=width*height||depth.length!=width*height
                ||width<=0||height<=0||!Double.isFinite(focal)||focal<=0)return 0;
        double dx=KEEPER_X-cameraX,dz=KEEPER_Z-cameraZ;
        double right=-dx*Math.cos(yaw)+dz*Math.sin(yaw);
        double forward=dx*Math.sin(yaw)+dz*Math.cos(yaw);
        if(forward<=.10||forward>24)return 0;
        double sy=Math.sin(pitch),cy=Math.cos(pitch);
        double bottom=-eyeHeight,top=1.82-eyeHeight;
        double zBottom=bottom*sy+forward*cy,zTop=top*sy+forward*cy;
        if(zBottom<=.065||zTop<=.065)return 0;
        double foot=height*.5-(bottom*cy-forward*sy)/zBottom*focal;
        double head=height*.5-(top*cy-forward*sy)/zTop*focal;
        if(head>=foot)return 0;
        double distance=(zBottom+zTop)*.5;
        double center=width*.5+right/distance*focal;
        double worldWidth=Math.max(.65,Math.min(1.18,1.82*sprite.getWidth()/sprite.getHeight()));
        double half=focal*worldWidth/(2*distance);
        int left=Math.max(0,(int)Math.floor(center-half));
        int rightEdge=Math.min(width-1,(int)Math.ceil(center+half));
        int topEdge=Math.max(0,(int)Math.floor(head));
        int bottomEdge=Math.min(height-1,(int)Math.ceil(foot));
        if(left>rightEdge||topEdge>bottomEdge)return 0;
        int drawn=0;
        for(int py=topEdge;py<=bottomEdge;py++){
            int v=(int)((py+.5-head)/(foot-head)*sprite.getHeight());
            if(v<0||v>=sprite.getHeight())continue;
            for(int px=left;px<=rightEdge;px++){
                int u=(int)((px+.5-(center-half))/(half*2)*sprite.getWidth());
                if(u<0||u>=sprite.getWidth())continue;
                int argb=sprite.getRGB(u,v),alpha=argb>>>24;
                if(alpha<24)continue;
                int i=py*width+px;
                if(distance>=depth[i])continue;
                int source=argb&0xffffff;
                if(alpha<255){
                    int target=pixels[i],inverse=255-alpha;
                    int r=(((source>>>16)&255)*alpha+((target>>>16)&255)*inverse)/255;
                    int g=(((source>>>8)&255)*alpha+((target>>>8)&255)*inverse)/255;
                    int b=((source&255)*alpha+(target&255)*inverse)/255;
                    source=(r<<16)|(g<<8)|b;
                }
                pixels[i]=source;depth[i]=distance;drawn++;
            }
        }
        return drawn;
    }
    private static List<String> wrap(Graphics2D g,String text,int width){
        var lines=new ArrayList<String>();String row="";
        for(String word:text.split("\\s+")){
            String next=row.isEmpty()?word:row+" "+word;
            if(!row.isEmpty()&&g.getFontMetrics().stringWidth(next)>width){lines.add(row);row=word;}else row=next;
        }if(!row.isEmpty())lines.add(row);return lines;
    }
    private static Rectangle panel(int width,int height){int w=Math.min(1020,width-24),h=Math.min(690,height-24);return new Rectangle((width-w)/2,(height-h)/2,w,h);}
    public static void click(Reader reader,int x,int y,int width,int height){
        var p=panel(width,height);if(y<p.y+p.height-46||y>p.y+p.height||x<p.x||x>p.x+p.width)return;
        if(x<p.x+p.width/3)reader.turn(-1);else if(x>p.x+p.width*2/3)reader.turn(1);else reader.close();
    }
    public static void draw(Graphics2D source,int width,int height,Reader reader){
        Graphics2D g=(Graphics2D)source.create();try{
            g.setColor(new Color(0,0,0,185));g.fillRect(0,0,width,height);var p=panel(width,height);
            g.setColor(new Color(21,25,25));g.fillRoundRect(p.x,p.y,p.width,p.height,8,8);
            g.setColor(new Color(229,208,163));g.setFont(new Font(Font.SANS_SERIF,Font.BOLD,17));
            g.drawString("THE COMMON ARCHIVE",p.x+18,p.y+30);
            int tx=p.x+20,tw=p.width-40,top=p.y+61,bottom=p.y+p.height-85;
            if(p.width>=730){
                int pw=Math.min(245,p.width/3),ph=Math.min(bottom-top-25,(int)(pw*1.2));var im=portrait();
                double fit=Math.min((double)pw/im.getWidth(),(double)ph/im.getHeight());int iw=(int)(fit*im.getWidth()),ih=(int)(fit*im.getHeight());
                g.drawImage(im,p.x+20+(pw-iw)/2,top,iw,ih,null);
                g.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,12));g.setColor(new Color(184,195,188));g.drawString(WorldLore.KEEPER,p.x+22,top+ih+20);
                tx+=pw+20;tw-=pw+20;
            }
            var page=WorldLore.PAGES.get(reader.page());g.setFont(new Font(Font.SANS_SERIF,Font.BOLD,16));g.setColor(new Color(229,208,163));
            for(String line:wrap(g,page.title(),tw)){g.drawString(line,tx,top);top+=22;}top+=12;
            g.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,14));var lines=new ArrayList<String>();
            for(String paragraph:page.paragraphs()){lines.addAll(wrap(g,paragraph,tw));lines.add("");}
            int rows=Math.max(1,(bottom-top)/20);reader.scroll=Math.min(reader.scroll,Math.max(0,lines.size()-rows));
            g.setColor(new Color(217,220,211));for(int i=0;i<rows&&reader.scroll+i<lines.size();i++)g.drawString(lines.get(reader.scroll+i),tx,top+i*20);
            g.setColor(new Color(166,184,175));g.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,12));
            String help=lines.size()>rows?"UP / DOWN / WHEEL TO SCROLL":"Testimony and history / not live paranormal evidence";
            g.drawString(help,p.x+20,p.y+p.height-61);
            String count=(reader.page()+1)+" / "+WorldLore.PAGES.size();g.drawString(count,p.x+p.width-75,p.y+30);
            g.setColor(new Color(229,208,163));g.drawString("< PREVIOUS",p.x+20,p.y+p.height-23);
            String close="E / ESC CLOSE";g.drawString(close,p.x+(p.width-g.getFontMetrics().stringWidth(close))/2,p.y+p.height-23);
            String next="NEXT >";g.drawString(next,p.x+p.width-20-g.getFontMetrics().stringWidth(next),p.y+p.height-23);
        }finally{g.dispose();}
    }
}
