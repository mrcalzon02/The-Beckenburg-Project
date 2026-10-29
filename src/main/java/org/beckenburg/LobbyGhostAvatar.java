package org.beckenburg;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Objects;
import java.util.random.RandomGenerator;
import javax.swing.JPanel;

/**
 * Purely cosmetic lobby disguise. It uses only the approved Banshee turnaround artwork;
 * palette and facing are randomized independently of the host's secret archetype.
 * An avatar packet MUST NEVER encode GhostIdentity.Type, behavior, evidence or rite.
 */
public final class LobbyGhostAvatar {
    private LobbyGhostAvatar(){}
    private static final int[] FACINGS={0,2,6};
    private static final Color[] TINTS={
        new Color(115,187,224,105), new Color(169,116,218,105),
        new Color(119,218,175,105), new Color(216,170,118,105),
        new Color(211,122,147,105)
    };
    public record Look(int facing,int palette){
        public Look {
            if(facing!=0&&facing!=2&&facing!=6)
                throw new IllegalArgumentException("unsupported cosmetic facing");
            if(palette<0||palette>=TINTS.length)
                throw new IllegalArgumentException("unsupported cosmetic palette");
        }
        public String frame(){return "LOBBYAVATAR|"+facing+"|"+palette;}
        public static Look decode(String frame){
            var p=LanProtocol.parts(frame);
            if(p.size()!=3||!p.get(0).equals("LOBBYAVATAR"))
                throw new IllegalArgumentException("invalid lobby appearance packet");
            return new Look(LanProtocol.integer(p.get(1),0,7),
                LanProtocol.integer(p.get(2),0,TINTS.length-1));
        }
    }
    public static Look random(RandomGenerator rng){
        Objects.requireNonNull(rng);
        return new Look(FACINGS[rng.nextInt(FACINGS.length)],rng.nextInt(TINTS.length));
    }

    /** Actual approved hunter/ghost directional artwork, never a tiny HUD thumbnail. */
    public static final class Preview extends JPanel {
        private Look look=random(java.util.concurrent.ThreadLocalRandom.current());
        private CharacterVisuals.Hunter hunter=CharacterVisuals.Hunter.OBSERVER;
        private boolean ghost,connected;
        public Preview(){
            setPreferredSize(new Dimension(208,300));
            setMinimumSize(new Dimension(175,250));
            setBackground(new Color(19,21,28));
        }
        public void showHunter(){showHunter(hunter);}
        public void showHunter(CharacterVisuals.Hunter selected){
            hunter=Objects.requireNonNull(selected);
            ghost=false;connected=false;repaint();
        }
        public CharacterVisuals.Hunter displayedHunter(){return hunter;}
        public void showGhost(Look value,boolean isConnected){
            look=Objects.requireNonNull(value);ghost=true;connected=isConnected;repaint();
        }
        public boolean showingGhost(){return ghost;}
        public Look displayedLook(){return look;}
        /** Crop transparent canvas only, never any visible part of the character. */
        static Rectangle artBounds(BufferedImage art){
            int minX=art.getWidth(),minY=art.getHeight(),maxX=-1,maxY=-1;
            for(int y=0;y<art.getHeight();y++)for(int x=0;x<art.getWidth();x++)
                if((art.getRGB(x,y)>>>24)>12){
                    minX=Math.min(minX,x);minY=Math.min(minY,y);
                    maxX=Math.max(maxX,x);maxY=Math.max(maxY,y);
                }
            return maxX<0?new Rectangle(0,0,art.getWidth(),art.getHeight()):
                new Rectangle(minX,minY,maxX-minX+1,maxY-minY+1);
        }
        @Override protected void paintComponent(Graphics graphics){
            super.paintComponent(graphics);
            Graphics2D g=(Graphics2D)graphics.create();
            try{
                int w=getWidth(),h=getHeight();
                g.setColor(new Color(24,29,39));
                g.fillRoundRect(8,8,w-16,h-16,8,8);
                g.setColor(new Color(101,110,128));
                g.drawRoundRect(8,8,w-16,h-16,8,8);
                g.setFont(new Font(Font.MONOSPACED,Font.BOLD,12));
                g.setColor(new Color(226,210,175));
                g.drawString(ghost?"GHOST PLAYER":"HUNTER PLAYER",18,29);

                String identity=ghost?"banshee":hunter.name().toLowerCase(java.util.Locale.ROOT);
                BufferedImage sprite=DirectionalSprites.frame(identity,ghost?look.facing():0);
                Rectangle bounds=artBounds(sprite);
                int availableW=Math.max(1,w-38),availableH=Math.max(1,h-100);
                double scale=Math.min(availableW/(double)bounds.width,
                    availableH/(double)bounds.height);
                int sw=Math.max(1,(int)Math.round(bounds.width*scale));
                int sh=Math.max(1,(int)Math.round(bounds.height*scale));
                int left=(w-sw)/2,top=42+(availableH-sh)/2;
                if(ghost){
                    BufferedImage tinted=new BufferedImage(sprite.getWidth(),sprite.getHeight(),
                        BufferedImage.TYPE_INT_ARGB);
                    Graphics2D ink=tinted.createGraphics();
                    try{
                        ink.drawImage(sprite,0,0,null);
                        ink.setComposite(AlphaComposite.SrcAtop);
                        ink.setColor(TINTS[look.palette()]);
                        ink.fillRect(0,0,tinted.getWidth(),tinted.getHeight());
                    }finally{ink.dispose();}
                    sprite=tinted;
                }
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                g.drawImage(sprite,left,top,left+sw,top+sh,
                    bounds.x,bounds.y,bounds.x+bounds.width,bounds.y+bounds.height,null);
                g.setColor(new Color(176,189,206));
                g.setFont(new Font(Font.MONOSPACED,Font.PLAIN,10));
                g.drawString(ghost?"IDENTITY: UNKNOWN":hunter.name(),18,h-43);
                g.drawString(ghost?(connected?"LOBBY DISGUISE":"RANDOM PREVIEW"):"HUNTER ROLE",
                    18,h-25);
            }finally{g.dispose();}
        }
    }
}
