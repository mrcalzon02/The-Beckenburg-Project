package org.beckenburg;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashSet;
import java.util.Set;

/** Walkable castle preparation lobby; service desks and purchases exist as world interactions. */
public final class CastleHubPanel extends JPanel implements KeyListener,FocusListener {
    public interface Actions {
        void begin();void network();void spectral();void menu();void campaign();
        void cycleRole();void cycleWeather();String role();String weather();
    }
    private final CastleHubMap map=new CastleHubMap();
    private final CastleOrientation orientation=new CastleOrientation();
    private final SceneRenderer renderer=new SceneRenderer();
    private final ProfileStore profile;
    private final Actions actions;
    private final MouseLook look;
    private final Set<Integer> down=new HashSet<>();
    private final javax.swing.Timer timer;
    private final AtmosphereAudio atmosphere=new AtmosphereAudio(AtmosphereAudio.Scene.CASTLE);
    private long last=System.nanoTime();
    private double clock;
    private double elevation;
    private boolean castleLightsOn=true,crouched,storeOpen;
    private String notice="Welcome to Beckenburg Castle. The equipment is real; look and press E.";
    private int noticeTicks=100;
    public CastleHubPanel(ProfileStore profile,Actions actions){
        this.profile=java.util.Objects.requireNonNull(profile);
        this.actions=java.util.Objects.requireNonNull(actions);
        setFocusable(true);setBackground(new Color(11,12,17));
        double[] spawn=CastleLevelEdits.current().spawn();
        renderer.x=spawn[0];renderer.z=spawn[1];renderer.yaw=spawn[2];renderer.pitch=-.07;
        look=new MouseLook(this,(dx,dy)->MouseLook.rotate(renderer,dx,dy));
        addKeyListener(this);addFocusListener(this);addFocusListener(look);
        addMouseListener(look);addMouseMotionListener(look);
        renderer.drawCastle(map,0);
        timer=new javax.swing.Timer(33,e->frame());timer.start();
    }
    public void startOrientation(){
        orientation.start();
        notice=orientation.objective();noticeTicks=260;
        requestFocusInWindow();
    }
    public void close(){timer.stop();down.clear();look.close();atmosphere.close();}
    private void frame(){
        atmosphere.setActive(isShowing());
        if(!isShowing()||storeOpen){last=System.nanoTime();return;}
        long now=System.nanoTime();double dt=Math.min(.07,Math.max(0,(now-last)/1e9));last=now;
        if(down.contains(KeyEvent.VK_LEFT))renderer.yaw+=dt*1.65;
        if(down.contains(KeyEvent.VK_RIGHT))renderer.yaw-=dt*1.65;
        if(down.contains(KeyEvent.VK_UP))renderer.pitch=Math.min(1.05,renderer.pitch+dt);
        if(down.contains(KeyEvent.VK_DOWN))renderer.pitch=Math.max(-1.05,renderer.pitch-dt);
        double forward=(down.contains(KeyEvent.VK_W)?1:0)-(down.contains(KeyEvent.VK_S)?1:0);
        double strafe=(down.contains(KeyEvent.VK_D)?1:0)-(down.contains(KeyEvent.VK_A)?1:0);
        double norm=Math.hypot(forward,strafe);
        if(norm>0){
            double priorX=renderer.x,priorZ=renderer.z;
            double speed=(crouched?2.6*HunterPosture.CROUCH_SPEED:down.contains(KeyEvent.VK_SHIFT)?3.9:2.6)*dt/norm;
            double dx=(Math.sin(renderer.yaw)*forward-Math.cos(renderer.yaw)*strafe)*speed;
            double dz=(Math.cos(renderer.yaw)*forward+Math.sin(renderer.yaw)*strafe)*speed;
            double targetX=renderer.x+dx,targetZ=renderer.z+dz;
            double nextX=CastleHubArchitecturalPlan.elevation(targetX,renderer.z,elevation);
            if(safeElevationTransition(elevation,nextX,targetX,renderer.z)&&map.traversable(targetX,renderer.z,nextX)){
                renderer.x=targetX;elevation=nextX;
            }
            double nextZ=CastleHubArchitecturalPlan.elevation(renderer.x,targetZ,elevation);
            if(safeElevationTransition(elevation,nextZ,renderer.x,targetZ)&&map.traversable(renderer.x,targetZ,nextZ)){
                renderer.z=targetZ;elevation=nextZ;
            }
            if(Math.hypot(renderer.x-priorX,renderer.z-priorZ)>.0001)
                GameSfx.play(map.footstep(renderer.x,renderer.z,elevation));
        }
        clock+=dt;
        renderer.eyeHeight=HunterPosture.eye(crouched)+elevation;
        GraphicsOptions.current().configure(renderer,getWidth(),getHeight(),true);
        renderer.drawCastle(map,clock,castleLightsOn);
        if(noticeTicks>0)noticeTicks--;
        repaint();
    }
    /** Never teleport from upper gallery to the ground by crossing an unguarded edge. */
    static boolean safeElevationTransition(double from,double to,double x,double z){
        if(from>2.0 && to<0.5 && !CastleHubArchitecturalPlan.onStairs(x,z))return false;
        return true;
    }
    /** One persistent account and the existing quartermaster inventory for both armoury counters. */
    private void openQuartermaster(){
        down.clear();look.release();storeOpen=true;
        java.awt.Window owner=SwingUtilities.getWindowAncestor(this);
        JDialog window=new JDialog(owner,"Castle Beckenburg — Quartermaster",Dialog.ModalityType.APPLICATION_MODAL);
        window.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        window.setContentPane(new CampaignStorePanel(profile,()->{
            GameSfx.play(LanSound.Cue.PICKUP);
            notice="Quartermaster purchase saved. Equipment and supplies updated.";
            noticeTicks=145;
        }));
        window.setSize(730,430);
        window.setMinimumSize(new Dimension(570,330));
        window.setLocationRelativeTo(this);
        try{window.setVisible(true);}
        finally{storeOpen=false;last=System.nanoTime();requestFocusInWindow();}
    }
    private void use(CastleHubMap.Station station){
        if(station==null){
            if(!orientation.active()&&elevation<1.8&&
                CastleWarehouse.aimed(map,renderer.x,renderer.z,renderer.yaw)){
                openQuartermaster();return;
            }
            if(CastleHubDecor.aimed(map,renderer.x,renderer.z,renderer.yaw)!=null){
                castleLightsOn=!castleLightsOn;
                GameSfx.play(LanSound.Cue.SWITCH);
                notice="Castle lights "+(castleLightsOn?"on.":"off.");
                if(orientation.lights())notice="Orientation complete. Contract board is ready.";
                noticeTicks=160;return;
            }
            notice="Look at a station or light switch.";noticeTicks=75;return;
        }
        if(orientation.active()){
            boolean moved=orientation.visit(station);
            if(moved&&station==CastleHubMap.Station.ROLE)actions.cycleRole();
            notice=(moved?"Training: ":"Find the marked station. ")+orientation.objective();
            noticeTicks=220;
            return;
        }
        try{
            switch(station){
                case ARMORY_UV,ARMORY_SALT -> {openQuartermaster();return;}
                case ROLE -> {actions.cycleRole();GameSfx.play(LanSound.Cue.SELECT);notice="Hunter discipline: "+actions.role();}
                case CONTRACT -> {actions.campaign();return;}
                case EXPEDITION -> {actions.begin();return;}
                case MULTIPLAYER -> {actions.network();return;}
                case SPECTRAL -> {actions.spectral();return;}
            }
        }catch(RuntimeException e){
            BeckenburgDiagnostics.fail("Castle workstation interaction",e);
            notice="Workstation unavailable: "+e.getMessage();
        }
        noticeTicks=110;
    }
    @Override public void keyPressed(KeyEvent e){
        if(!isShowing())return;
        int key=e.getKeyCode();if(down.contains(key))return;
        if(key==KeyEvent.VK_ESCAPE){actions.menu();return;}
        if(key==KeyEvent.VK_E){
            var portal=elevation<=1.8?map.aimedDoor(renderer.x,renderer.z,renderer.yaw):null;
            if(portal!=null){
                boolean open=map.toggleDoor(portal.id());
                GameSfx.play(LanSound.Cue.SWITCH);
                notice=(portal.gate()?"Courtyard portcullis":"Castle door")+
                    (open?" raised / passage clear.":" closed / passage secured.");
                noticeTicks=130;
            }else if(elevation>1.8){
                notice="Return to the ground floor to use workstations.";noticeTicks=90;
            }else use(CastleHubDecor.aimed(map,renderer.x,renderer.z,renderer.yaw)!=null?
                null:map.aimed(renderer.x,renderer.z,renderer.yaw));
        }
        if(key==KeyEvent.VK_CONTROL){crouched=!crouched;notice=crouched?"Crouched / Ctrl to stand":"Standing";noticeTicks=60;}
        if(key==KeyEvent.VK_F)renderer.lamp=!renderer.lamp;
        if(key==KeyEvent.VK_L){
            castleLightsOn=!castleLightsOn;GameSfx.play(LanSound.Cue.SWITCH);
            if(orientation.lights()){notice="Orientation complete. Use the contract board to select a case.";noticeTicks=220;}
        }
        down.add(key);
    }
    @Override public void keyReleased(KeyEvent e){down.remove(e.getKeyCode());}
    @Override public void keyTyped(KeyEvent e){}
    @Override public void focusGained(FocusEvent e){last=System.nanoTime();}
    @Override public void focusLost(FocusEvent e){down.clear();look.release();}
    @Override protected void paintComponent(Graphics src){
        super.paintComponent(src);
        Graphics2D g=(Graphics2D)src.create();
        try{
            int w=getWidth(),h=getHeight();
            GraphicsOptions.current().draw(g,renderer.image(),w,h);

            g.setFont(new Font(Font.MONOSPACED,Font.BOLD,14));
            g.setColor(new Color(11,17,23,214));
            g.fillRoundRect(12,10,Math.min(w-24,450),42,8,8);
            g.setColor(new Color(243,223,179));
            g.drawString("BECKENBURG CASTLE",25,29);
            g.setFont(new Font(Font.MONOSPACED,Font.PLAIN,12));
            g.setColor(new Color(186,215,202));
            g.drawString("£"+profile.currency()+"   /   "+actions.role()+
                (elevation>2.0?"   /   UPPER GALLERY":""),25,44);
            g.setColor(new Color(230,215,174));
            g.drawString("+",w/2-4,h/2+4);
            CastleHubMap.Station station=elevation>1.8?null:map.aimed(renderer.x,renderer.z,renderer.yaw);
            boolean atSwitch=elevation<=1.8&&
                CastleHubDecor.aimed(map,renderer.x,renderer.z,renderer.yaw)!=null;
            boolean atWarehouse=elevation<=1.8&&CastleWarehouse.aimed(map,renderer.x,renderer.z,renderer.yaw);
            var atDoor=elevation<=1.8?map.aimedDoor(renderer.x,renderer.z,renderer.yaw):null;
            if(atDoor!=null||station!=null||atSwitch||atWarehouse){
                String label=atDoor!=null?
                    (map.doorOpen(atDoor.id())?"CLOSE ":"OPEN ")+
                    (atDoor.gate()?"COURTYARD PORTCULLIS":"CASTLE DOOR"):
                    atSwitch?"CASTLE LIGHT SWITCH":atWarehouse?
                    "WAREHOUSE COUNTER / PURCHASE AND PREPARE":station.label;
                int labelWidth=Math.min(w-24,Math.max(250,g.getFontMetrics().stringWidth(label)+50));
                int left=(w-labelWidth)/2;
                g.setColor(new Color(11,16,21,220));g.fillRoundRect(left,h-75,labelWidth,36,8,8);
                g.setColor(new Color(247,211,153));g.drawString("[ E ]  "+label,left+12,h-52);
            }
            if(orientation.active()){
                g.setColor(new Color(11,17,23,223));
                g.fillRoundRect(12,103,Math.max(230,Math.min(w-24,650)),46,8,8);
                g.setColor(new Color(247,211,153));
                g.drawString("ORIENTATION  "+(orientation.progress()+1)+"/5",24,121);
                g.setColor(new Color(186,215,202));g.drawString(orientation.objective(),24,139);
            }
            // Only context prompts and transient notices overlay the scene; controls
            // belong to the dedicated menu, not a permanent bottom-screen ticker.
            if(noticeTicks>0){
                String text=notice.length()>70?notice.substring(0,67)+"...":notice;
                int boxWidth=Math.min(w-24,Math.max(290,g.getFontMetrics().stringWidth(text)+24));
                g.setColor(new Color(12,24,27,220));g.fillRoundRect(12,60,boxWidth,35,8,8);
                g.setColor(new Color(162,223,193));g.drawString(text,24,82);
            }
            g.setColor(new Color(11,17,23,212));g.fillRoundRect(Math.max(12,w-172),12,160,30,8,8);
            g.setColor(new Color(216,220,212));g.drawString("ESC  /  MAIN MENU",Math.max(20,w-162),32);
        }finally{g.dispose();}
    }
}
