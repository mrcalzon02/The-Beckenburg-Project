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
    private final SoundVolumeAudio authoredAudio=new SoundVolumeAudio(map.sounds());
    private final CastleOrientation orientation=new CastleOrientation();
    private final CastleArchive.Reader archive=new CastleArchive.Reader();
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
    private final JPanel quartermaster;
    private final CampaignStorePanel quartermasterStore;
    private JButton quartermasterClose;
    private JPanel serviceOverlay;
    private Runnable serviceBack;
    private String notice="Welcome to Beckenburg Castle. The equipment is real; look and press E.";
    private int noticeTicks=100;
    public CastleHubPanel(ProfileStore profile,Actions actions){
        this.profile=java.util.Objects.requireNonNull(profile);
        this.actions=java.util.Objects.requireNonNull(actions);
        setFocusable(true);setBackground(new Color(11,12,17));
        // The world panel itself owns the shop overlay; never create a Window.
        setLayout(null);
        quartermasterStore=new CampaignStorePanel(profile,()->{
            GameSfx.play(LanSound.Cue.PICKUP);
            notice="Quartermaster transaction saved. Equipment and stock updated.";
            noticeTicks=145;
        });
        quartermaster=createQuartermasterOverlay();
        quartermaster.setVisible(false);
        add(quartermaster);
        double[] spawn=CastleLevelEdits.current().spawn();
        renderer.x=spawn[0];renderer.z=spawn[1];renderer.yaw=spawn[2];renderer.pitch=-.07;
        look=new MouseLook(this,(dx,dy)->MouseLook.rotate(renderer,dx,dy),()->!archive.opened()&&!storeOpen);
        addKeyListener(this);addFocusListener(this);addFocusListener(look);
        addMouseListener(look);addMouseMotionListener(look);
        addMouseListener(new MouseAdapter(){@Override public void mousePressed(MouseEvent e){if(archive.opened()&&e.getButton()==MouseEvent.BUTTON1){CastleArchive.click(archive,e.getX(),e.getY(),getWidth(),getHeight());repaint();}}});
        addMouseWheelListener(e->{if(archive.opened()){archive.scroll(e.getWheelRotation()*3);repaint();}});
        renderer.drawCastle(map,0);
        timer=new javax.swing.Timer(33,e->frame());timer.start();
    }
    public void startOrientation(){
        orientation.start();
        notice=orientation.objective();noticeTicks=260;
        requestFocusInWindow();
    }
    public void close(){timer.stop();down.clear();look.close();atmosphere.close();authoredAudio.close();}
    @Override public void doLayout(){
        super.doLayout();
        int width=Math.max(0,Math.min(getWidth()-24,1020));
        int height=Math.max(0,Math.min(getHeight()-24,680));
        quartermaster.setBounds((getWidth()-width)/2,(getHeight()-height)/2,width,height);
        if(serviceOverlay!=null)serviceOverlay.setBounds((getWidth()-width)/2,(getHeight()-height)/2,width,height);
    }
    /** Campaign and multiplayer are castle workstations inside this world panel. */
    public void openService(String title,JComponent content,Runnable back){
        closeService();closeQuartermaster();down.clear();look.release();storeOpen=true;serviceBack=back;
        serviceOverlay=PsxHorrorUi.surface(new BorderLayout(0,6));
        serviceOverlay.setName("castle.service.overlay");
        JLabel context=PsxMenuScreen.label("CASTLE BECKENBURG / "+title);
        context.setBorder(BorderFactory.createEmptyBorder(7,10,0,10));
        serviceOverlay.add(context,BorderLayout.NORTH);serviceOverlay.add(content,BorderLayout.CENTER);
        serviceOverlay.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0),"leaveService");
        serviceOverlay.getActionMap().put("leaveService",new AbstractAction(){
            public void actionPerformed(ActionEvent event){if(serviceBack!=null)serviceBack.run();}
        });
        add(serviceOverlay);setComponentZOrder(serviceOverlay,0);revalidate();repaint();
    }
    public void closeService(){
        if(serviceOverlay!=null){remove(serviceOverlay);serviceOverlay=null;serviceBack=null;storeOpen=false;last=System.nanoTime();}
        revalidate();repaint();
    }
    /** Existing PSX panels, buttons and approved model thumbnails stay inside
     * the same world viewport and share the live persisted Castle profile. */
    private JPanel createQuartermasterOverlay(){
        JPanel overlay=PsxHorrorUi.surface(new BorderLayout(8,8));
        overlay.setName("castle.quartermaster.overlay");
        overlay.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
        JPanel heading=new JPanel(new BorderLayout(8,0));
        heading.setOpaque(false);
        JLabel title=new JLabel("CASTLE BECKENBURG   /   QUARTERMASTER");
        title.setForeground(new Color(236,219,183));
        title.setFont(new Font(Font.MONOSPACED,Font.BOLD,17));
        heading.add(title,BorderLayout.CENTER);
        JButton close=PsxHorrorUi.button("RETURN TO CASTLE  [ESC]");
        quartermasterClose=close;
        close.setName("castle.quartermaster.close");
        close.addActionListener(e->closeQuartermaster());
        heading.add(close,BorderLayout.EAST);
        overlay.add(heading,BorderLayout.NORTH);
        overlay.add(quartermasterStore,BorderLayout.CENTER);
        overlay.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0),"closeQuartermaster");
        overlay.getActionMap().put("closeQuartermaster",new AbstractAction(){
            @Override public void actionPerformed(ActionEvent e){closeQuartermaster();}
        });
        return overlay;
    }
    boolean quartermasterOpen(){return storeOpen;}
    private void closeQuartermaster(){
        if(!storeOpen)return;
        storeOpen=false;
        quartermaster.setVisible(false);
        last=System.nanoTime();
        down.clear();
        requestFocusInWindow();
        repaint();
    }
    private void frame(){
        atmosphere.setActive(isShowing());authoredAudio.active(isShowing()&&!storeOpen);authoredAudio.listener(renderer.x,renderer.eyeHeight,renderer.z,renderer.yaw,castleLightsOn);
        if(!isShowing()||storeOpen){last=System.nanoTime();return;}
        if(archive.opened()){last=System.nanoTime();down.clear();repaint();return;}
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
    /** Open the existing shop as part of the Castle game panel. */
    void openQuartermaster(){
        if(storeOpen)return;
        down.clear();
        look.release();
        storeOpen=true;
        quartermasterStore.refresh();
        quartermaster.setVisible(true);
        revalidate();
        repaint();
        SwingUtilities.invokeLater(()->{
            if(storeOpen)quartermasterClose.requestFocusInWindow();
        });
    }
    private void use(CastleHubMap.Station station){
        if(station==CastleHubMap.Station.ARCHIVE){archive.open();down.clear();look.release();noticeTicks=0;repaint();return;}
        if(station==null){
            if(!orientation.active()&&elevation<1.8&&
                CastleWarehouse.aimed(map,renderer.x,renderer.z,renderer.yaw)){
                openQuartermaster();return;
            }
            if(CastleHubDecor.aimed(map,renderer.x,elevation,renderer.z,renderer.yaw)!=null){
                boolean circuitOn=map.toggleCircuit(CastleHubDecor.aimed(map,renderer.x,elevation,renderer.z,renderer.yaw));
                GameSfx.play(LanSound.Cue.SWITCH);
                notice="Room lights "+(circuitOn?"on.":"off.");
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
                case ROLE -> {actions.cycleRole();GameSfx.play(LanSound.Cue.SELECT);notice=actions.role()+" / "+WorldLore.faction(InvestigationSession.Role.valueOf(actions.role())).name();}
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
        if(serviceOverlay!=null&&serviceOverlay.isVisible()){
            if(key==KeyEvent.VK_ESCAPE&&serviceBack!=null)serviceBack.run();return;
        }
        if(storeOpen){
            if(key==KeyEvent.VK_ESCAPE)closeQuartermaster();
            return;
        }
        if(archive.opened()){archive.key(key);repaint();return;}
        if(key==KeyEvent.VK_ESCAPE){actions.menu();return;}
        if(key==KeyEvent.VK_E){
            var portal=map.aimedDoor(renderer.x,elevation,renderer.z,renderer.yaw);
            if(portal!=null){
                boolean open=map.toggleDoor(portal.id());
                GameSfx.play(LanSound.Cue.SWITCH);
                notice=(portal.gate()?"Courtyard portcullis":"Castle door")+
                    (open?" raised / passage clear.":" closed / passage secured.");
                noticeTicks=130;
            }else if(CastleHubDecor.aimed(map,renderer.x,elevation,renderer.z,renderer.yaw)!=null){
                use(null);
            }else if(elevation>1.8){
                notice="Return to the ground floor to use workstations.";noticeTicks=90;
            }else use(CastleHubDecor.aimed(map,renderer.x,elevation,renderer.z,renderer.yaw)!=null?
                null:map.aimed(renderer.x,renderer.z,renderer.yaw));
        }
        if(key==KeyEvent.VK_CONTROL){crouched=!crouched;notice=crouched?"Crouched / Ctrl to stand":"Standing";noticeTicks=60;}
        if(key==KeyEvent.VK_F)renderer.lamp=!renderer.lamp;
        if(key==KeyEvent.VK_L){
            if(CastleHubDecor.aimed(map,renderer.x,elevation,renderer.z,renderer.yaw)!=null)use(null);
            else {notice="Face a mounted wall switch to operate this room's lights.";noticeTicks=120;}
        }
        down.add(key);
    }
    @Override public void keyReleased(KeyEvent e){down.remove(e.getKeyCode());}
    @Override public void keyTyped(KeyEvent e){}
    @Override public void focusGained(FocusEvent e){last=System.nanoTime();}
    @Override public void focusLost(FocusEvent e){down.clear();look.release();}
    /** Use native-resolution text at a genuinely legible physical size on modern displays. */
    static double hudScale(int width,int height){
        return Math.max(1.0,Math.min(2.25,Math.max(width/1280.0,height/800.0)));
    }
    @Override protected void paintComponent(Graphics src){
        super.paintComponent(src);
        Graphics2D g=(Graphics2D)src.create();
        try{
            int pixelWidth=getWidth(),pixelHeight=getHeight();
            GraphicsOptions.current().draw(g,renderer.image(),pixelWidth,pixelHeight);
            if(storeOpen){
                g.setColor(new Color(5,8,12,190));g.fillRect(0,0,pixelWidth,pixelHeight);
                return; // Swing paints the live PSX store child above the dimmed world.
            }
            // Archive input uses physical mouse coordinates: never scale its drawing.
            if(archive.opened()){
                CastleArchive.draw(g,pixelWidth,pixelHeight,archive);
                return;
            }
            double scale=hudScale(pixelWidth,pixelHeight);
            g.scale(scale,scale);
            int w=Math.max(1,(int)Math.ceil(pixelWidth/scale));
            int h=Math.max(1,(int)Math.ceil(pixelHeight/scale));
            g.setFont(new Font(Font.MONOSPACED,Font.BOLD,14));
            g.setColor(new Color(11,17,23,214));
            g.fillRoundRect(12,10,Math.min(w-24,450),42,8,8);
            g.setColor(new Color(243,223,179));
            g.drawString("BECKENBURG CASTLE",25,29);
            g.setFont(new Font(Font.MONOSPACED,Font.PLAIN,12));
            g.setColor(new Color(186,215,202));
            g.drawString("\u00a3"+profile.currency()+"   /   "+actions.role()+
                (elevation>2.0?"   /   UPPER GALLERY":""),25,44);
            g.setColor(new Color(230,215,174));
            g.drawString("+",w/2-4,h/2+4);
            var shelf=CastleWarehouse.aimedExhibit(map,renderer.x,renderer.eyeHeight,renderer.z,renderer.yaw,renderer.pitch);
            if(shelf!=null&&!archive.opened()){
                String item=CastleShelfLabels.title(shelf),tier="TIER "+shelf.tier().name();
                g.setFont(new Font(Font.MONOSPACED,Font.BOLD,Math.max(16,Math.min(22,w/45))));
                int labelWidth=Math.min(w-24,Math.max(280,g.getFontMetrics().stringWidth(item)+32));
                int left=(w-labelWidth)/2;
                g.setColor(new Color(9,15,21,238));g.fillRoundRect(left,h-154,labelWidth,66,8,8);
                g.setColor(new Color(255,242,210));g.drawString(item,left+16,h-128);
                g.setFont(new Font(Font.MONOSPACED,Font.BOLD,16));
                g.setColor(new Color(164,223,242));g.drawString(tier,left+16,h-104);
                g.setFont(new Font(Font.MONOSPACED,Font.PLAIN,12));
            }
            CastleHubMap.Station station=elevation>1.8?null:map.aimed(renderer.x,renderer.z,renderer.yaw);
            boolean atSwitch=elevation<=1.8&&
                CastleHubDecor.aimed(map,renderer.x,elevation,renderer.z,renderer.yaw)!=null;
            boolean atWarehouse=elevation<=1.8&&CastleWarehouse.aimed(map,renderer.x,renderer.z,renderer.yaw);
            var atDoor=map.aimedDoor(renderer.x,elevation,renderer.z,renderer.yaw);
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
