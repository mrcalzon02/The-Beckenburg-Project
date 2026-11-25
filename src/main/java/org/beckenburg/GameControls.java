package org.beckenburg;

import java.awt.Component;
import java.awt.event.KeyEvent;

/** Default field layout shared by Castle, solo and LAN. No routine function keys. */
public final class GameControls {
    private GameControls(){}
    public static final int RUN=KeyEvent.VK_SHIFT, CROUCH=KeyEvent.VK_CONTROL, JUMP=KeyEvent.VK_SPACE,
        USE=KeyEvent.VK_F, DROP=KeyEvent.VK_R, LIGHT=KeyEvent.VK_T,
        SWAP=KeyEvent.VK_Q, HELP=KeyEvent.VK_H;
    public static final String[] HINTS={
        "WASD move / SHIFT run / CTRL crouch / SPACE jump",
        "E interact / Q cycle / 1-4 slots / F use or raise camera",
        "LMB use or shutter / RMB power / T light / G place",
        "R drop / LMB throw held prop / wheel turn held prop",
        "TAB journal / J casefile / M map / H help / ESC pause",
        "C radio voice / V local voice / ENTER text, TAB channel",
        "K field actions, supplies, rituals and voice options"
    };
    public static String help(){return String.join("\n",HINTS)+
        "\n\nSolo and multiplayer use the same equipment and interaction simulation."+
        "\nGhost: E fixture / Q next target / Z previous target / 1-8 abilities / SPACE drift.";}
    public static int choose(Component parent,String... labels){
        Object selected=GameDialogs.showInputDialog(parent,"Choose an action","FIELD ACTIONS",
            GameDialogs.PLAIN_MESSAGE,null,labels,labels[0]);
        if(selected==null)return -1;
        for(int i=0;i<labels.length;i++)if(labels[i].equals(selected))return i;
        return -1;
    }
}
