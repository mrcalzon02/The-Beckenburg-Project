package org.beckenburg;
import dev.mrcalzon.water.*;
import dev.mrcalzon.weather.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Actual game-renderer companion to the portable water plan viewport. */
final class WaterGamePreview extends JPanel implements AutoCloseable {
 record Camera(double x,double y,double z,double yaw,double pitch){}
 record Weather(WeatherType type,double hour,double temperature,double wind,double heading,boolean clock,int revision){void apply(WeatherModule m){m.setWeather(type,.4);m.setTimeOfDay(hour/24);m.setTemperature(temperature);m.weather().setWind(wind,Math.toRadians(heading));m.pauseClock(!clock);}}
 private static final class Scene {final HouseMap map;final CastleHubMap castle;final SceneRenderer renderer=new SceneRenderer();int weatherRevision=-1;Scene(HouseMap map,CastleHubMap castle){this.map=map;this.castle=castle;renderer.setRenderSize(640,400);renderer.roomLights=new RoomLighting();renderer.setLampHardware(EquipmentTiers.Tier.II,false);}}
 private final ExecutorService worker=Executors.newSingleThreadExecutor(r->{var t=new Thread(r,"water-editor-game-preview");t.setDaemon(true);return t;});private final AtomicBoolean busy=new AtomicBoolean();private final javax.swing.Timer timer;private final Set<Integer> keys=new HashSet<>();private final long started=System.nanoTime();
 interface Listener {void update(double x,double y,double z,double yaw);}
 private Listener listener=(x,y,z,yaw)->{};private volatile SoundVolumeEditor.View soundView;
 void listener(Listener listener){this.listener=listener;}void sounds(SoundVolumeEditor.View v){soundView=v;}
 private volatile Scene scene;private volatile WaterModule water;private volatile Weather weather=new Weather(WeatherType.CLEAR,20.64,12,1,0,false,0);private Camera camera=new Camera(13.5,1.62,18,0,-.3);private volatile BufferedImage image;private int lastX,lastY;private final JLabel status=new JLabel("ACTUAL GAME PREVIEW · right-drag look · WASD/QE fly");
 private final JPanel view=new JPanel(){protected void paintComponent(Graphics g){super.paintComponent(g);var im=image;if(im==null)return;double scale=Math.min(getWidth()/(double)im.getWidth(),getHeight()/(double)im.getHeight());g.drawImage(im,(int)((getWidth()-im.getWidth()*scale)/2),(int)((getHeight()-im.getHeight()*scale)/2),(int)(im.getWidth()*scale),(int)(im.getHeight()*scale),null);}};
 WaterGamePreview(){super(new BorderLayout(3,3));view.setBackground(Color.BLACK);view.setFocusable(true);add(view,BorderLayout.CENTER);add(status,BorderLayout.SOUTH);view.addMouseListener(new MouseAdapter(){public void mousePressed(MouseEvent e){lastX=e.getX();lastY=e.getY();view.requestFocusInWindow();}});view.addMouseMotionListener(new MouseMotionAdapter(){public void mouseDragged(MouseEvent e){if(!SwingUtilities.isRightMouseButton(e))return;camera=new Camera(camera.x,camera.y,camera.z,camera.yaw-(e.getX()-lastX)*.006,Math.max(-1.5,Math.min(1.5,camera.pitch-(e.getY()-lastY)*.006)));lastX=e.getX();lastY=e.getY();}});view.addMouseWheelListener(e->camera=new Camera(camera.x+Math.sin(camera.yaw)*e.getPreciseWheelRotation(),camera.y,camera.z+Math.cos(camera.yaw)*e.getPreciseWheelRotation(),camera.yaw,camera.pitch));view.addKeyListener(new KeyAdapter(){public void keyPressed(KeyEvent e){keys.add(e.getKeyCode());}public void keyReleased(KeyEvent e){keys.remove(e.getKeyCode());}});view.addFocusListener(new FocusAdapter(){public void focusLost(FocusEvent e){keys.clear();}});timer=new javax.swing.Timer(90,e->tick());timer.start();}
 void map(HouseMap map,CastleHubMap castle,Camera camera,WaterModule water){var previous=scene;scene=new Scene(map,castle);if(previous!=null)previous.renderer.close();this.camera=camera;this.water=water;image=null;}
 void water(WaterModule water){this.water=water;}void weather(Weather weather){this.weather=weather;}
 void focus(WaterPlacement p){double distance=Math.max(5,Math.min(25,Math.max(p.width(),p.depth())));camera=new Camera(p.x(),p.y()+distance*.6,p.z()-distance,0,-.5);}
 private static void draw(Scene s,double time){if(s.map==null)s.renderer.drawCastle(s.castle,time,true);else s.renderer.drawNetwork(s.map,List.of(),-1,time);}
 private void tick(){if(scene==null)return;double f=(keys.contains(KeyEvent.VK_W)?1:0)-(keys.contains(KeyEvent.VK_S)?1:0),side=(keys.contains(KeyEvent.VK_D)?1:0)-(keys.contains(KeyEvent.VK_A)?1:0),up=(keys.contains(KeyEvent.VK_E)?1:0)-(keys.contains(KeyEvent.VK_Q)?1:0),speed=keys.contains(KeyEvent.VK_SHIFT)?.8:.25;camera=new Camera(camera.x+(Math.sin(camera.yaw)*f+Math.cos(camera.yaw)*side)*speed,camera.y+up*speed,camera.z+(Math.cos(camera.yaw)*f-Math.sin(camera.yaw)*side)*speed,camera.yaw,camera.pitch);listener.update(camera.x,camera.y,camera.z,camera.yaw);if(!busy.compareAndSet(false,true))return;var active=scene;var pose=camera;var module=water;var wx=weather;double time=(System.nanoTime()-started)/1e9;
  worker.execute(()->{try{var r=active.renderer;r.x=pose.x;r.eyeHeight=pose.y;r.z=pose.z;r.yaw=pose.yaw;r.pitch=pose.pitch;r.lamp=true;r.setWaterPreview(module);if(active.weatherRevision!=wx.revision){wx.apply(r.weatherModule());active.weatherRevision=wx.revision;}draw(active,time);var gpu=GpuWorldRenderer.active();if(gpu!=null){long id=r.submittedGpuFrameId(),deadline=System.nanoTime()+3_000_000_000L;boolean received=false;while(System.nanoTime()<deadline){var frame=gpu.latest();if(frame!=null&&frame.frameId()>=id)try{r.pinCompletedGpuFrame(frame);received=true;break;}catch(IllegalArgumentException ignored){}Thread.sleep(10);}if(!received)return;draw(active,time);}var copy=new BufferedImage(r.width(),r.height(),BufferedImage.TYPE_INT_RGB);var g=copy.createGraphics();g.drawImage(r.image(),0,0,null);SoundVolumeGizmos.draw(g,r,soundView);g.dispose();if(scene==active){image=copy;SwingUtilities.invokeLater(()->status.setText("ACTUAL "+(gpu==null?"SOFTWARE":"OPENGL")+" GAME PREVIEW · right-drag look · WASD/QE fly"));}}catch(Exception e){SwingUtilities.invokeLater(()->status.setText("Preview: "+e.getMessage()));}finally{busy.set(false);SwingUtilities.invokeLater(view::repaint);}});
 }
 public void close(){timer.stop();worker.shutdownNow();var active=scene;if(active!=null)active.renderer.close();}
}
