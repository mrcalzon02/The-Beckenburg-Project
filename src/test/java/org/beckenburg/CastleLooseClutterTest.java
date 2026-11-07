package org.beckenburg;

import java.nio.file.*;
import java.util.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import javax.swing.*;
import javax.imageio.ImageIO;
import java.util.List;

/** Native supports/openings and real mesh interaction through the shared body engine. */
public final class CastleLooseClutterTest {
    private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
    private static void refused(Runnable action,String why){try{action.run();throw new AssertionError(why);}catch(IllegalArgumentException expected){}}
    private static void layout(Container parent){parent.doLayout();for(var c:parent.getComponents())if(c instanceof Container child)layout(child);}
    private static void capture(CastleHubPanel panel,Path out,String name)throws Exception{layout(panel);var image=new BufferedImage(1000,700,BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();panel.paint(g);g.dispose();Files.createDirectories(out);ImageIO.write(image,"png",out.resolve(name+".png").toFile());}
    private static LooseClutter.Observer observer(CastleHubMap map,LooseClutter.State s){
        double feet=s.y()>3?3.4:s.y()<0?-3:0;
        for(double radius:new double[]{1.2,1.6,1.9})for(int i=0;i<36;i++){
            double angle=i*Math.PI/18,x=s.x()+Math.sin(angle)*radius,z=s.z()+Math.cos(angle)*radius;
            if(!map.traversable(x,z,feet))continue;
            var o=new LooseClutter.Observer(0,x,feet,z,Math.atan2(s.x()-x,s.z()-z),Math.atan2(s.y()-feet-1.62,radius),true);
            var hit=map.clutter().aimed(o);if(hit!=null&&hit.id()==s.id())return o;
        }throw new AssertionError("No real reachable pickup view for object "+s.id());
    }
    public static void main(String[] args)throws Exception{
        System.setProperty("user.home",Files.createTempDirectory("castle-loose-").toString());var scene=CastleLevelEdits.current();var original=scene.snapshot();var map=new CastleHubMap();var objects=map.clutter();var world=new CastleClutterWorld(map);
        check(objects.states().size()==6,"Missing authored native loose-body sockets: "+objects.states());var initial=objects.states();
        check(Math.abs(world.support(17.5,25.75,2.4,2.0)-2.21)<.002,"Small object uses the smooth navigation incline instead of the rendered upper stair tread");
        for(int tick=0;tick<100;tick++)check(objects.advance(List.of()).isEmpty(),"Protected hub clutter damages a player");
        for(var s:objects.states()){
            var before=initial.stream().filter(p->p.id()==s.id()).findFirst().orElseThrow();check(Math.hypot(s.x()-before.x(),s.z()-before.z())<.002&&Math.abs(s.y()-before.y())<.01,"Resting native object drifts/falls through its table "+s.id());
            double bottom=s.y()-s.kind().half;check(Math.abs(world.support(s.x(),s.z(),bottom+.002,bottom-.005)-bottom)<.005,"Object lacks actual source triangle support "+s.id());
            observer(map,s);
        }
        // A 10mm sweep through an actual table side must not inherit the aim ray's 30mm near clip.
        var table=scene.props().stream().filter(p->p.id().equals("ground37")).findFirst().orElseThrow();double[] bounds=SceneRenderer.castleBounds(table.mesh());boolean shortContact=false;
        for(var t:GlbProps.model(table.mesh()).triangles()){
            double ax=t.b().x()-t.a().x(),ay=t.b().y()-t.a().y(),az=t.b().z()-t.a().z(),bx=t.c().x()-t.a().x(),by=t.c().y()-t.a().y(),bz=t.c().z()-t.a().z();
            double nx=ay*bz-az*by,ny=az*bx-ax*bz,nz=ax*by-ay*bx,len=Math.sqrt(nx*nx+ny*ny+nz*nz);if(len<1e-6||Math.abs(ny)>1e-8)continue;
            double cx=table.x()+((t.a().x()+t.b().x()+t.c().x())/3-bounds[0]),cy=table.y()+((t.a().y()+t.b().y()+t.c().y())/3-bounds[1]),cz=table.z()+((t.a().z()+t.b().z()+t.c().z())/3-bounds[2]);
            check(!world.clearSight(cx+nx/len*.005,cy,cz+nz/len*.005,cx-nx/len*.005,cy,cz-nz/len*.005),"Substep passes through real source furniture below the aim near clip");shortContact=true;break;
        }check(shortContact,"No actual table-side substep contact control");
        if(args.length>0){
            Path out=Path.of(args[0]).resolveSibling("castle-loose-placements");Files.createDirectories(out);
            for(var s:objects.states()){
                var view=observer(map,s);var r=new SceneRenderer();r.setRenderSize(1000,625);r.x=view.x();r.z=view.z();r.eyeHeight=view.eyeY();r.yaw=view.yaw();r.pitch=view.pitch();r.drawCastle(map,0);
                ImageIO.write(r.image(),"png",out.resolve("loose-"+s.id()+"-"+s.kind().name().toLowerCase(Locale.ROOT)+"-runtime.png").toFile());
            }
        }
        String stable=objects.snapshot(false);
        var parent=scene.props().stream().filter(p->p.id().equals("upper3")).findFirst().orElseThrow();scene.removeProp(parent.id());check(new CastleHubMap().clutter().states().stream().noneMatch(s->s.id()==3),"Deleted parent leaves a floating seed");
        for(int tick=0;tick<100;tick++)objects.advance(List.of());
        check(objects.states().stream().filter(s->s.id()==3).findFirst().orElseThrow().y()<3.5,"Live parent deletion leaves a sleeping object floating");scene.restore(original);objects.accept(stable.substring("CLUTTER|".length()));
        var target=objects.states().stream().filter(s->s.id()==1).findFirst().orElseThrow();var o=observer(map,target);var baseline=objects.snapshot(false);
        refused(()->objects.action("TAKE",target.id(),new LooseClutter.Observer(1,o.x(),o.y(),o.z(),o.yaw(),o.pitch(),true),1),"Foreign seat owns local hub object");
        refused(()->objects.action("TAKE",target.id(),new LooseClutter.Observer(0,-16,6.5,2,0,0,true),1),"Unsupported local body can take object");
        check(baseline.equals(objects.snapshot(false)),"Rejected owner/origin changed body state");
        double mx=(o.x()+target.x())/2,mz=(o.z()+target.z())/2;boolean horizontal=Math.abs(Math.cos(o.yaw()))>=Math.abs(Math.sin(o.yaw()));
        scene.put(new CastleLevelEdits.Surface("clutter-wall-control",CastleLevelEdits.Kind.WALL,horizontal?mx-1:mx,horizontal?mz:mz-1,horizontal?mx+1:mx,horizontal?mz:mz+1,0,2.8,MaterialAtlas.STONE));
        check(map.traversable(o.x(),o.z(),o.y()),"Wall pickup control obstructs camera");refused(()->objects.action("TAKE",target.id(),o,1),"Pickup crosses actual native wall");scene.restore(original);
        objects.action("TAKE",target.id(),o,1);check(objects.held(0)==target.id(),"Native mesh pickup failed");
        objects.action("ROTATE",target.id(),o,2);check(objects.states().stream().anyMatch(s->s.id()==target.id()&&s.roll()!=0),"Held body cannot rotate");
        var east=new LooseClutter.Observer(0,13.5,0,19.3,Math.PI/2,0,true);check(map.traversable(east.x(),east.z(),east.y()),"Actual +X throw fixture obstructed");
        objects.action("THROW",target.id(),east,3);double x=objects.states().stream().filter(s->s.id()==target.id()).findFirst().orElseThrow().x();
        check(objects.advance(List.of(east)).isEmpty(),"Local throw arms ghost damage");var thrown=objects.states().stream().filter(s->s.id()==target.id()).findFirst().orElseThrow();check(thrown.x()>x+.25&&objects.held(0)<0,"Actual +X throw goes backward or stays held");
        scene.put(new CastleLevelEdits.Surface("clutter-bounce-control",CastleLevelEdits.Kind.WALL,15.8,18.3,15.8,20.3,0,2.8,MaterialAtlas.STONE));double nearest=thrown.x();
        for(int tick=0;tick<20;tick++){objects.advance(List.of(east));double at=objects.states().stream().filter(s->s.id()==target.id()).findFirst().orElseThrow().x();nearest=Math.max(nearest,at);check(at<15.8,"Thrown body tunnels through actual native masonry");}
        check(nearest>15.1,"Thrown-wall control never approaches the actual wall");scene.restore(original);
        check(objects.sanityScatter(0,100)==0,"Protected hub activates ghost scatter");
        for(int tick=0;tick<100;tick++)objects.advance(List.of(east));
        check(objects.states().stream().allMatch(s->Double.isFinite(s.x()+s.y()+s.z()+s.roll())),"Nonfinite body after settling");
        var falling=new CastleHubMap().clutter();falling.accept("99,BOOK,40000,1000,21000,0,0,-1");
        for(int tick=0;tick<100;tick++)falling.advance(List.of());
        var cellar=falling.states().get(0);check(cellar.y()<-.2&&cellar.y()>-3,"Body cannot fall through actual cellar opening to the continuous flight: "+cellar);
        check(original.equals(scene.snapshot()),"Loose-body handling changes persistent native document");
        var file=Path.of(System.getProperty("user.home"),"profile.properties");var profile=new ProfileStore(file);profile.save();byte[] profileBefore=Files.readAllBytes(file);var actions=new java.util.concurrent.atomic.AtomicInteger();Throwable[] failure={null};
        SwingUtilities.invokeAndWait(()->{CastleHubPanel panel=null;JFrame window=null;try{
            panel=new CastleHubPanel(profile,new CastleHubPanel.Actions(){public void begin(){actions.incrementAndGet();}public void network(){actions.incrementAndGet();}public void spectral(){actions.incrementAndGet();}public void menu(){actions.incrementAndGet();}public void campaign(){actions.incrementAndGet();}public void cycleRole(){actions.incrementAndGet();}public void cycleWeather(){actions.incrementAndGet();}public String role(){return "CLERIC";}public String weather(){return "Normal";}});panel.setSize(1000,700);
            if(!GraphicsEnvironment.isHeadless()){window=new JFrame("Castle loose-object verification");window.setContentPane(panel);window.setSize(1020,740);window.setVisible(true);}
            var mf=CastleHubPanel.class.getDeclaredField("map");mf.setAccessible(true);var nativeMap=(CastleHubMap)mf.get(panel);var rf=CastleHubPanel.class.getDeclaredField("renderer");rf.setAccessible(true);var renderer=(SceneRenderer)rf.get(panel);var tick=CastleHubPanel.class.getDeclaredField("clutterTick");tick.setAccessible(true);
            var item=nativeMap.clutter().states().stream().filter(s->s.id()==1).findFirst().orElseThrow();var view=observer(nativeMap,item);renderer.x=view.x();renderer.z=view.z();renderer.eyeHeight=view.eyeY();renderer.yaw=view.yaw();renderer.pitch=view.pitch();renderer.drawCastle(nativeMap,0);
            var warm=renderer.dynamicGeometryStats();renderer.drawCastle(nativeMap,0);check(renderer.dynamicGeometryStats().builds()==warm.builds()&&renderer.dynamicGeometryStats().hits()>=warm.hits()+6,"Resting Castle objects rebuild every rendered frame");
            if(args.length>0)capture(panel,Path.of(args[0]),"common-mug-before-pickup-runtime");
            if(GraphicsEnvironment.isHeadless())check(panel.handleClutter(KeyEvent.VK_F5),"Native pickup action unavailable");else panel.keyPressed(new KeyEvent(panel,KeyEvent.KEY_PRESSED,0,0,KeyEvent.VK_F5,KeyEvent.CHAR_UNDEFINED));
            check(nativeMap.clutter().held(0)==1,"Actual native F5 does not pick up mug");renderer.drawCastle(nativeMap,0);
            check(renderer.dynamicGeometryStats().builds()==warm.builds()+1,"One pickup rebuilds unrelated stationary Castle object meshes");
            if(args.length>0)capture(panel,Path.of(args[0]),"common-mug-held-runtime");
            tick.setInt(panel,1);if(GraphicsEnvironment.isHeadless())panel.handleClutter(KeyEvent.VK_CLOSE_BRACKET);else panel.keyPressed(new KeyEvent(panel,KeyEvent.KEY_PRESSED,0,0,KeyEvent.VK_CLOSE_BRACKET,']'));
            check(nativeMap.clutter().states().stream().anyMatch(s->s.id()==1&&s.roll()!=0),"Native rotate key ignored");
            tick.setInt(panel,2);if(GraphicsEnvironment.isHeadless())panel.handleClutter(KeyEvent.VK_F12);else panel.keyPressed(new KeyEvent(panel,KeyEvent.KEY_PRESSED,0,0,KeyEvent.VK_F12,KeyEvent.CHAR_UNDEFINED));
            check(nativeMap.clutter().held(0)<0,"Native throw key leaves mug held");panel.startOrientation();check(!panel.handleClutter(KeyEvent.VK_F5),"Loose handling interrupts protected orientation");
            if(args.length>0){
                var windowSlot=CastleDoorSlots.windows().get(0);double z=(windowSlot.z0()+windowSlot.z1())/2;
                var review=new SceneRenderer();review.setRenderSize(1000,625);review.lamp=false;review.x=windowSlot.x()+1.4;review.z=z;review.eyeHeight=1.62;review.yaw=-Math.PI/2;review.pitch=.08;
                check(nativeMap.traversable(review.x,review.z,0),"Window review camera unsupported");Path out=Path.of(args[0]).resolveSibling("castle-window-frame");Files.createDirectories(out);
                review.drawCastle(nativeMap,0,true);ImageIO.write(review.image(),"png",out.resolve("native-window-lights-on-runtime.png").toFile());
                review.drawCastle(nativeMap,0,false);ImageIO.write(review.image(),"png",out.resolve("native-window-lights-off-runtime.png").toFile());
            }
        }catch(Throwable t){failure[0]=t;}finally{if(panel!=null)panel.close();if(window!=null)window.dispose();}});
        if(failure[0]!=null)throw new AssertionError("Castle native loose-object input",failure[0]);
        check(actions.get()==0&&Arrays.equals(profileBefore,Files.readAllBytes(file))&&original.equals(scene.snapshot()),"Local handling writes profile, activates case or edits source");
        System.out.println("PASS6 authored native loose bodies: exact mesh support, all reachable pickup views, stable rest, deleted parent, local ownership, +X throw, safe gravity/collision, actual cellar aperture and zero native scene writes");
        System.out.println("PASS "+(GraphicsEnvironment.isHeadless()?"contained actions":"real native F5 / ] / F12 input")+" with orientation protection and no campaign/profile actions");
        System.out.println("Retained source loose-object triangles="+initial.stream().mapToInt(s->GlbProps.model(s.kind().mesh()).triangles().size()).sum()+"; stationary poses reuse6 meshes; one pickup rebuilds1");
    }
}
