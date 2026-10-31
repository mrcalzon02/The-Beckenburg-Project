package org.beckenburg;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The local developer's eighth ROLE, not an eighth player seat: unrestricted
 * renderer/camera and authoritative read-only inspection. No LanClient for
 * ADMIN, no packets or hidden state sent to the seven gameplay AI clients.
 */
public final class DevAdminPanel implements AutoCloseable {
    private final LanHost host;
    private final Runnable onExit;
    private final Map<String,String> intents;
    private final HouseMap map;
    private final SceneRenderer renderer=new SceneRenderer();
    private final JFrame window=new JFrame("BECKENBURG / DEV MATCH / ADMIN (YOU)");
    private final JPanel view=new View();
    private final JTextArea inspector=new JTextArea();
    private final JLabel status=new JLabel("ADMIN (YOU) / off-seat / connecting");
    private final JTextField command=new JTextField();
    private final javax.swing.Timer timer;
    private final Set<Integer> down=new HashSet<>();
    private volatile boolean open=true;
    private int focus=-1,dragX,dragY;
    private boolean follow=true,wire=true,overview=false;
    private DevAdminSnapshot last;
    private String lastDoors="",lastWindows="";
    private String consoleMessage="admin ready — type help";

    public static DevAdminPanel open(LanHost host,HouseMap.Site site,
                                      Map<String,String> intents,Runnable onExit)throws Exception {
        if(GraphicsEnvironment.isHeadless())
            throw new IllegalStateException("Interactive ADMIN requires a Windows desktop session");
        AtomicReference<DevAdminPanel> created=new AtomicReference<>();
        SwingUtilities.invokeAndWait(()->created.set(
            new DevAdminPanel(host,site,intents,onExit)));
        return created.get();
    }

    private DevAdminPanel(LanHost host,HouseMap.Site site,
                           Map<String,String> intents,Runnable onExit) {
        this.host=host;this.map=new HouseMap(site);this.intents=intents;this.onExit=onExit;
        renderer.lamp=true;
        renderer.yaw=.45;renderer.pitch=-.25;
        var van=map.van().placement();
        renderer.x=van.x();renderer.z=van.z();
        renderer.eyeHeight=van.y()+3;
        window.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        window.setMinimumSize(new Dimension(920,600));
        window.setSize(1330,790);
        window.setLocationRelativeTo(null);
        view.setFocusable(true);
        view.setBackground(Color.BLACK);
        view.addKeyListener(new KeyAdapter(){
            @Override public void keyPressed(KeyEvent e){
                down.add(e.getKeyCode());
                if(e.getKeyCode()>=KeyEvent.VK_1&&e.getKeyCode()<=KeyEvent.VK_6)select(e.getKeyCode()-KeyEvent.VK_1);
                else if(e.getKeyCode()==KeyEvent.VK_7)select(6);
                else if(e.getKeyCode()==KeyEvent.VK_0)select(-1);
                else if(e.getKeyCode()==KeyEvent.VK_F)follow=!follow;
                else if(e.getKeyCode()==KeyEvent.VK_X)wire=!wire;
                else if(e.getKeyCode()==KeyEvent.VK_M)overview=!overview;
            }
            @Override public void keyReleased(KeyEvent e){down.remove(e.getKeyCode());}
        });
        view.addFocusListener(new FocusAdapter(){
            @Override public void focusLost(FocusEvent e){down.clear();}
        });
        view.addMouseListener(new MouseAdapter(){
            @Override public void mousePressed(MouseEvent e){
                dragX=e.getX();dragY=e.getY();view.requestFocusInWindow();
            }
        });
        view.addMouseMotionListener(new MouseMotionAdapter(){
            @Override public void mouseDragged(MouseEvent e){
                renderer.yaw+=Math.toRadians((e.getX()-dragX)*.30);
                renderer.pitch=Math.max(-1.53,Math.min(1.2,
                    renderer.pitch-Math.toRadians((e.getY()-dragY)*.30)));
                dragX=e.getX();dragY=e.getY();
            }
        });
        view.addMouseWheelListener(e->{
            double v=-e.getPreciseWheelRotation()*1.2;
            renderer.x+=Math.sin(renderer.yaw)*v;
            renderer.z+=Math.cos(renderer.yaw)*v;
            follow=false;
        });
        JPanel top=new JPanel(new BorderLayout());
        top.add(status,BorderLayout.CENTER);
        status.setFont(new Font(Font.MONOSPACED,Font.BOLD,14));
        status.setBorder(BorderFactory.createEmptyBorder(7,12,7,12));
        inspector.setEditable(false);
        inspector.setFont(new Font(Font.MONOSPACED,Font.PLAIN,12));
        inspector.setBackground(new Color(23,25,34));
        inspector.setForeground(new Color(222,231,239));
        inspector.setMargin(new Insets(8,9,8,9));
        JScrollPane data=new JScrollPane(inspector);
        data.setPreferredSize(new Dimension(365,550));
        JSplitPane split=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,view,data);
        split.setResizeWeight(.74);
        JPanel bottom=new JPanel(new BorderLayout(6,0));
        bottom.add(new JLabel("  DEV >"),BorderLayout.WEST);
        bottom.add(command,BorderLayout.CENTER);
        command.setToolTipText("help; follow H1..H6/GHOST; free; overview; xray; status; quit");
        command.addActionListener(e->{
            String line=command.getText().trim();command.setText("");
            execute(line);
            view.requestFocusInWindow();
        });
        JButton send=new JButton("Execute");
        send.addActionListener(e->command.postActionEvent());
        bottom.add(send,BorderLayout.EAST);
        window.add(top,BorderLayout.NORTH);
        window.add(split,BorderLayout.CENTER);
        window.add(bottom,BorderLayout.SOUTH);
        window.addWindowListener(new WindowAdapter(){
            @Override public void windowClosed(WindowEvent e){close();}
        });
        timer=new javax.swing.Timer(180,e->refresh());
        timer.start();
        window.setVisible(true);
        view.requestFocusInWindow();
    }

    public boolean isOpen(){return open;}
    private void select(int seat){focus=seat;follow=seat>=0;overview=false;}
    public void execute(String line){
        String cmd=line.toLowerCase(java.util.Locale.ROOT);
        if(cmd.equals("help")){
            consoleMessage="follow H1..H6 | follow GHOST | free | overview | xray | status | quit";
        }else if(cmd.matches("follow h[1-6]")){
            select(cmd.charAt(8)-'1');consoleMessage="tracking H"+(focus+1);
        }else if(cmd.equals("follow ghost")){
            select(6);consoleMessage="tracking GHOST";
        }else if(cmd.equals("free")){
            focus=-1;follow=false;overview=false;consoleMessage="free flight / noclip";
        }else if(cmd.equals("overview")){
            overview=!overview;consoleMessage="overhead view "+(overview?"on":"off");
        }else if(cmd.equals("xray")){
            wire=!wire;consoleMessage="through-wall overlays "+(wire?"on":"off");
        }else if(cmd.equals("status")){
            consoleMessage=last==null?"waiting for host snapshot":
                last.connectedPlayers()+" player sockets / ADMIN off-seat / tick "+last.tick()+
                " / "+last.outcome();
        }else if(cmd.equals("quit")||cmd.equals("exit")){
            close();return;
        }else{
            consoleMessage="unknown admin command: "+line+"  (type help)";
        }
        status.setText("ADMIN (YOU) / "+consoleMessage);
    }

    private void moveCamera(){
        if(follow&&focus>=0&&last!=null){
            double x,z,y;
            if(focus==6){
                var b=last.ghost().body();x=b.x();z=b.z();y=b.y()+1.5;
            }else{
                var b=last.hunters().get(focus).body();x=b.x();z=b.z();y=b.y()+1.6;
            }
            renderer.x=x;renderer.z=z;renderer.eyeHeight=y;
        }
        if(overview){
            renderer.eyeHeight=Math.max(renderer.eyeHeight,21.0);
            renderer.pitch=-1.48;
        }
        double pace=down.contains(KeyEvent.VK_SHIFT)?1.1:.43;
        int f=(down.contains(KeyEvent.VK_W)?1:0)-(down.contains(KeyEvent.VK_S)?1:0);
        int r=(down.contains(KeyEvent.VK_D)?1:0)-(down.contains(KeyEvent.VK_A)?1:0);
        if((f!=0||r!=0)&&(!follow||focus<0)){
            double d=Math.hypot(f,r);
            renderer.x+=pace*(f*Math.sin(renderer.yaw)-r*Math.cos(renderer.yaw))/d;
            renderer.z+=pace*(f*Math.cos(renderer.yaw)+r*Math.sin(renderer.yaw))/d;
        }
        if(!follow||focus<0){
            if(down.contains(KeyEvent.VK_SPACE))renderer.eyeHeight+=pace;
            if(down.contains(KeyEvent.VK_CONTROL))renderer.eyeHeight-=pace;
        }
        if(down.contains(KeyEvent.VK_LEFT))renderer.yaw-=.065;
        if(down.contains(KeyEvent.VK_RIGHT))renderer.yaw+=.065;
        if(!overview){
            if(down.contains(KeyEvent.VK_UP))renderer.pitch=Math.min(1.25,renderer.pitch+.05);
            if(down.contains(KeyEvent.VK_DOWN))renderer.pitch=Math.max(-1.52,renderer.pitch-.05);
        }
    }

    private void refresh(){
        if(!open)return;
        try {
            last=host.devAdminSnapshot(); // single coherent read-only tick
            if(!lastDoors.equals(last.doorSnapshot())){
                map.applyDoorSnapshot(last.doorSnapshot());lastDoors=last.doorSnapshot();
            }
            if(!lastWindows.equals(last.windowSnapshot())){
                map.applyWindowSnapshot(last.windowSnapshot());lastWindows=last.windowSnapshot();
            }
            moveCamera();
            ArrayList<SceneRenderer.RemoteBody> bodies=new ArrayList<>(6);
            for(int i=0;i<last.hunters().size();i++){
                var h=last.hunters().get(i);
                if(h.connected()){
                    var b=h.body();
                    bodies.add(new SceneRenderer.RemoteBody(b.x(),b.y(),b.z(),i,
                        b.heading(),"observer",false));
                }
            }
            renderer.roomLightTick=last.tick();
            SceneRenderer.Apparition manifested=null;
            if(last.ghost().manifested()){
                var b=last.ghost().body();
                manifested=new SceneRenderer.Apparition(b.x(),b.y(),b.z(),
                    LanGhostAnimation.Phase.LURK,b.heading(),0,false,false,
                    CharacterVisuals.ghost(last.trueGhostType()));
            }
            renderer.drawNetwork(map,bodies,manifested,last.tick()*.1);
            updateInspector();
            view.repaint();
        }catch(RuntimeException error){
            consoleMessage="admin viewport error: "+error.getMessage();
            status.setText(consoleMessage);
        }
    }
    private void updateInspector(){
        if(last==null)return;
        StringBuilder s=new StringBuilder();
        s.append("EIGHT ROLES / SEVEN PLAYERS\n");
        s.append("ordinary sockets: ").append(last.connectedPlayers()).append("/7\n");
        s.append("ADMIN (YOU)     HOST LOCAL / NO SEAT\n");
        s.append("tick ").append(last.tick()).append(" / ").append(last.outcome()).append("\n\n");
        for(var h:last.hunters()){
            s.append(h.seat()).append(h.connected()?" [LIVE] ":" [OFF]  ")
                .append("HP ").append(h.health()).append(" ST ").append(h.stress())
                .append(" R").append(h.body().room()).append("\n");
            s.append("   ").append(intents.getOrDefault(h.seat(),"no AI input yet")).append("\n");
        }
        var ghost=last.ghost();
        s.append("GHOST ").append(ghost.clientConnected()?"[LIVE]":"[OFF]")
            .append(" energy ").append(ghost.energy())
            .append(" R").append(ghost.body().room()).append("\n");
        s.append("   ").append(intents.getOrDefault("GHOST","no AI input yet")).append("\n\n");
        s.append("EVIDENCE COMPARISON / ADMIN TRUTH\n");
        s.append("Real species: ").append(last.trueGhostType()).append("\n");
        s.append("Hunter identification: ").append(last.hunterIdentification()).append("\n");
        s.append("Recorded observations: ").append(last.observedCasefile().size()).append("\n");
        var evidence=last.observedCasefile();
        for(int i=Math.max(0,evidence.size()-11);i<evidence.size();i++){
            String line=evidence.get(i);
            s.append(line.length()>90?line.substring(0,90):line).append("\n");
        }
        s.append("\nDEV COMMAND / ").append(consoleMessage).append("\n");
        s.append("1-6 select; 7 ghost; 0 free\n");
        s.append("WASD + Space/Ctrl free-fly, Shift fast\n");
        s.append("drag to look; F follow; X x-ray; M overhead\n");
        inspector.setText(s.toString());
        status.setText("ADMIN (YOU) — "+last.connectedPlayers()+
                "/7 players | tick "+last.tick()+
                " | "+(follow?"FOLLOW "+(focus==6?"GHOST":"H"+(focus+1)):"FREE-FLY")+
                " | "+last.outcome());
    }

    private final class View extends JPanel {
        private void drawBounds(Graphics2D g,double x,double y,double z,double height,
                                double radius,String label,Color color){
            double[] foot=renderer.editorProjection(x,y,z);
            double[] head=renderer.editorProjection(x,y+height,z);
            double[] left=renderer.editorProjection(x-radius,y+height*.5,z);
            double[] right=renderer.editorProjection(x+radius,y+height*.5,z);
            if(foot==null||head==null||left==null||right==null)return;
            double scaleX=getWidth()/(double)renderer.width();
            double scaleY=getHeight()/(double)renderer.height();
            int cx=(int)Math.round((foot[0]+head[0])*.5*scaleX);
            int top=(int)Math.round(Math.min(head[1],foot[1])*scaleY);
            int bottom=(int)Math.round(Math.max(head[1],foot[1])*scaleY);
            int half=Math.max(5,(int)Math.round(Math.hypot(right[0]-left[0],right[1]-left[1])*.5*scaleX));
            if(cx+half<0||cx-half>getWidth()||bottom<0||top>getHeight())return;
            g.setColor(color);
            g.setStroke(new BasicStroke(1.8f));
            g.drawRect(cx-half,top,half*2,Math.max(4,bottom-top));
            g.draw(new Line2D.Double(cx-half,top,cx+half,bottom));
            g.draw(new Line2D.Double(cx+half,top,cx-half,bottom));
            g.setFont(new Font(Font.MONOSPACED,Font.BOLD,12));
            g.drawString(label,cx-half,Math.max(14,top-5));
        }
        @Override protected void paintComponent(Graphics base){
            super.paintComponent(base);
            Graphics2D g=(Graphics2D)base.create();
            try{
                g.drawImage(renderer.image(),0,0,getWidth(),getHeight(),null);
                if(last!=null&&wire){
                    int i=0;
                    for(var h:last.hunters()){
                        if(h.connected()){
                            var b=h.body();
                            drawBounds(g,b.x(),b.y(),b.z(),
                                HunterPosture.STANDING_HEIGHT,.22,h.seat(),
                                i==focus?Color.YELLOW:new Color(109,245,188));
                        }
                        i++;
                    }
                    var b=last.ghost().body();
                    drawBounds(g,b.x(),b.y(),b.z(),HunterPosture.STANDING_HEIGHT,
                        .22,"GHOST",focus==6?Color.YELLOW:new Color(249,117,163));
                }
                g.setColor(new Color(0,0,0,145));
                g.fillRect(10,10,410,39);
                g.setColor(Color.WHITE);
                g.setFont(new Font(Font.MONOSPACED,Font.BOLD,13));
                g.drawString("DEV ADMIN / NOCLIP + THROUGH-WALL DEBUG",19,27);
                g.drawString("X x-ray | M overhead | F follow | 0 free",19,42);
            }finally{g.dispose();}
        }
    }
    @Override public void close(){
        if(!open)return;
        open=false;
        if(timer!=null)timer.stop();
        if(window.isDisplayable())window.dispose();
        onExit.run();
    }
}
