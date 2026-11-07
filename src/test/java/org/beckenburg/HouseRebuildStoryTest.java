package org.beckenburg;
import java.util.*;
import java.nio.file.*;
import javax.imageio.ImageIO;
import org.beckenburg.LevelDefinition.*;
/** Actual supported authored belongings/readable history and functional loose bodies. */
public final class HouseRebuildStoryTest {
    static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
    static LooseClutter.Observer reader(HouseMap map,Prop p,String lore){
        var b=SceneRenderer.castleBounds(p.mesh());double tx=p.x()+b[0],ty=p.y()+b[1]+b[4]/2,tz=p.z()+b[2];
        for(double radius:new double[]{.65,.85,1.05,1.3})for(int i=0;i<64;i++){
            double a=i*Math.PI/32,x=tx+Math.sin(a)*radius,z=tz+Math.cos(a)*radius,y=map.floorY(p.room());if(!map.traversable(x,y,z)||map.roomAt(x,y,z)!=p.room())continue;
            double yaw=Math.atan2(tx-x,tz-z),pitch=Math.atan2(ty-y-1.62,Math.hypot(tx-x,tz-z));var o=new LooseClutter.Observer(0,x,y,z,yaw,pitch,true);var t=InteractionFocus.pick(map,o,List.of());
            if(t!=null&&t.kind()==InteractionFocus.Kind.INTERFACE&&t.id().equals("STORY_"+lore))return o;
        }throw new AssertionError("No actual supported reading approach to "+lore);
    }
    public static void main(String[] args)throws Exception{
        var map=HouseRebuildStory.map();var base=HouseRebuildFitout.source();check(map.level().stairs().equals(base.stairs())&&map.level().voids().equals(base.voids())&&map.level().dock().equals(base.dock()),"Structural/van change");
        var objects=map.clutter().states();check(objects.size()==8,"Authored belongings must replace automatic table/bedroom scatter");
        var sockets=map.level().sockets().stream().filter(s->s.type().equals("loose-object")).toList();check(sockets.size()==8,"Missing authored object bindings");
        for(int i=0;i<sockets.size();i++){var s=sockets.get(i);var item=objects.get(i);check(Math.abs(item.x()-s.x())<1e-6&&Math.abs(item.z()-s.z())<1e-6&&Math.abs(item.y()-item.kind().half-s.y())<1e-6,"Actual seed/source support mismatch "+s.id());}
        for(int n=0;n<20;n++)map.clutter().advance(List.of());
        for(int i=0;i<objects.size();i++){var old=objects.get(i);var now=map.clutter().states().get(i);check(Math.abs(now.y()-old.y())<.015,"Belonging falls through its source support "+sockets.get(i).id());}
        var records=map.level().sockets().stream().filter(s->s.type().equals("story")).toList();check(records.size()==3,"Missing real household records");
        check(SiteCaseFiles.forSite(map.level().id()).address().equals("17 Briar Lane, Beckenburg"),"Construction source lost canonical household history");
        for(var s:records){var p=map.level().props().stream().filter(q->q.room()==s.room()&&Math.abs(q.x()-s.x())<.001&&Math.abs(q.z()-s.z())<.001).findFirst().orElseThrow();reader(map,p,s.style());check(!SiteStories.document(map.level().id(),s.style()).lines().isEmpty(),"Unreadable record "+s.style());}
        var props=new ArrayList<>(map.level().props());props.removeIf(p->p.mesh().equals("dinner_table_1")&&p.room()==3);var level=map.level();var removed=new LevelDefinition(level.id(),"deleted-source-table",level.title(),level.rooms(),level.portals(),level.stairs(),level.voids(),props,level.sockets(),level.colliders(),level.dock(),level.ambience(),level.water(),level.sounds());
        check(new HouseMap(HouseMap.Site.HOUSE,removed).clutter().states().size()==5,"Deleted parent must not retain floating belongings");
        var mug=map.clutter().states().stream().filter(i->i.kind()==LooseClutter.Kind.MUG&&map.roomAt(i.x(),i.y()-i.kind().half,i.z())==3).findFirst().orElseThrow();
        LooseClutter.Observer observer=null;
        for(double radius:new double[]{.7,.9,1.2,1.5})for(int n=0;n<64&&observer==null;n++){
            double a=n*Math.PI/32,x=mug.x()+Math.sin(a)*radius,z=mug.z()+Math.cos(a)*radius,y=map.groundHeight(x,z,3);if(!map.traversable(x,y,z))continue;
            double yaw=Math.atan2(mug.x()-x,mug.z()-z),pitch=Math.atan2(mug.y()-y-1.62,Math.hypot(mug.x()-x,mug.z()-z));var o=new LooseClutter.Observer(0,x,y,z,yaw,pitch,true);var picked=InteractionFocus.pick(map,o,List.of());
            if(picked!=null&&picked.kind()==InteractionFocus.Kind.CLUTTER&&picked.id().equals(""+mug.id()))observer=o;
        }
        check(observer!=null,"Supported actual mesh pickup approach");map.clutter().takeFocused(mug.id(),observer,1);check(map.clutter().held(0)==mug.id(),"Real authored body pickup");check(map.clutter().hint(observer).contains("family mug"),"Authored belonging examination text");
        boolean rejected=false;try{map.clutter().action("THROW",mug.id(),new LooseClutter.Observer(1,observer.x(),observer.y(),observer.z(),observer.yaw(),observer.pitch(),true),2);}catch(IllegalArgumentException expected){rejected=true;}check(rejected&&map.clutter().held(0)==mug.id(),"Another seat cannot steal held body");
        var forward=new LooseClutter.Observer(0,observer.x(),observer.y(),observer.z(),Math.PI/2,0,true);map.clutter().action("THROW",mug.id(),forward,2);double start=map.clutter().states().stream().filter(i->i.id()==mug.id()).findFirst().orElseThrow().x();map.clutter().advance(List.of());
        check(map.clutter().states().stream().filter(i->i.id()==mug.id()).findFirst().orElseThrow().x()>start+.15,"Actual +X throw direction");
        var terrainFixture=HouseRebuildStory.map();double gx=9.4,gz=5.8,ground=terrainFixture.groundHeight(gx,gz,12),relief=0;
        for(double x=6.5;x<26.5;x+=.5)for(double z=3;z<20;z+=.5){double y=terrainFixture.groundHeight(x,z,12);if(Double.isFinite(y)&&Math.abs(y)>relief&&terrainFixture.roomAt(x,y,z)==12&&terrainFixture.traversable(x,y,z)){gx=x;gz=z;ground=y;relief=Math.abs(y);}}
        check(relief>.05,"Actual sloped editor-terrain fixture missing");
        terrainFixture.clutter().acceptOne("1,MUG,"+Math.round(gx*1000)+","+Math.round((ground+1.2+LooseClutter.Kind.MUG.half)*1000)+","+Math.round(gz*1000)+",0,0,-1");
        for(int n=0;n<40;n++){terrainFixture.clutter().advance(List.of());if(n>15){var body=terrainFixture.clutter().states().stream().filter(i->i.id()==1).findFirst().orElseThrow();check(Math.abs(body.y()-body.kind().half-ground)<.015,"Loose body cannot settle against actual editor terrain: expected="+ground+" actual="+(body.y()-body.kind().half));}}var groundBody=terrainFixture.clutter().states().stream().filter(i->i.id()==1).findFirst().orElseThrow();
        check(Math.abs(groundBody.y()-groundBody.kind().half-ground)<.015,"Loose body ignores actual editor terrain: expected="+ground+" actual="+(groundBody.y()-groundBody.kind().half));
        var renderer=new SceneRenderer();var export=SceneRenderer.class.getDeclaredField("exportFullLevel");export.setAccessible(true);export.setBoolean(renderer,true);var assemble=SceneRenderer.class.getDeclaredMethod("levelArchitecture",List.class,HouseMap.class);assemble.setAccessible(true);var faces=new ArrayList<Object>();assemble.invoke(renderer,faces,map);
        var faceType=faces.get(0).getClass();var af=faceType.getDeclaredField("a");var bf=faceType.getDeclaredField("b");var cf=faceType.getDeclaredField("c");for(var f:List.of(af,bf,cf))f.setAccessible(true);var pointType=af.get(faces.get(0)).getClass();var xf=pointType.getDeclaredField("x");var yf=pointType.getDeclaredField("y");var zf=pointType.getDeclaredField("z");for(var f:List.of(xf,yf,zf))f.setAccessible(true);
        for(var stair:map.level().stairs()){
            double x=stair.x0()+(stair.x1()-stair.x0())*.345,z=stair.z0()+(stair.z1()-stair.z0())*.345,approx=stair.yAt(x,z),top=Double.NEGATIVE_INFINITY;
            for(var face:faces){var a=af.get(face);var b=bf.get(face);var c=cf.get(face);double ax=xf.getDouble(a),az=zf.getDouble(a),bx=xf.getDouble(b),bz=zf.getDouble(b),cx=xf.getDouble(c),cz=zf.getDouble(c),den=(bz-cz)*(ax-cx)+(cx-bx)*(az-cz);if(Math.abs(den)<1e-12)continue;double u=((bz-cz)*(x-cx)+(cx-bx)*(z-cz))/den,v=((cz-az)*(x-cx)+(ax-cx)*(z-cz))/den;if(u<0||v<0||u+v>1)continue;double y=yf.getDouble(a)*u+yf.getDouble(b)*v+yf.getDouble(c)*(1-u-v);if(y>=approx-.03&&y<=approx+.25)top=Math.max(top,y);}
            check(Double.isFinite(top),"Actual rendered tread missing");var fixture=HouseRebuildStory.map();fixture.clutter().acceptOne("1,MUG,"+Math.round(x*1000)+","+Math.round((top+.55+LooseClutter.Kind.MUG.half)*1000)+","+Math.round(z*1000)+",0,0,-1");for(int n=0;n<30;n++)fixture.clutter().advance(List.of());var body=fixture.clutter().states().stream().filter(i->i.id()==1).findFirst().orElseThrow();check(Math.abs(body.y()-body.kind().half-top)<.015,"Loose body floats/sinks against actual rendered tread "+stair.id()+" expected="+top+" actual="+(body.y()-body.kind().half));
        }
        var audit=LevelTraversalAudit.inspect(map);check(audit.passed(),"Authored detail blocks route "+audit.findings());
        var terrain=WorldTerrainSurface.of(map.level());int cableSamples=0;
        for(var node:map.level().sockets())if(node.type().equals("service-node")&&node.style().contains("|ground-cable|")){
            var fields=node.style().split("\\|");int index=Integer.parseInt(fields[1]);if(index==0)continue;
            var previous=map.level().sockets().stream().filter(s->s.id().equals(fields[0]+"-"+(index-1))).findFirst().orElseThrow();
            for(int n=0;n<=8;n++){double t=n/8.0,x=previous.x()+(node.x()-previous.x())*t,z=previous.z()+(node.z()-previous.z())*t,y=previous.y()+(node.y()-previous.y())*t;
                check(Math.abs(y-.012-HouseRebuildLandscape.height(terrain,x,z))<.003,"Visible feed floats or sinks off real terrain "+node.id());cableSamples++;}
        }
        check(cableSamples>100&&map.level().sockets().stream().noneMatch(s->s.id().startsWith("BHR-SOURCE-")),"Visible feed must replace buried perimeter supply");
        var crossingStart=new org.beckenburg.LevelDefinition.Vec(24,map.groundHeight(24,21.2,13),21.2);
        var crossing=map.resolveMove(crossingStart,24,20.4);check(crossing!=null&&Math.hypot(crossing.x()-24,crossing.z()-20.4)<.01,"Feed blocks actual garden crossing");
        if(args.length>0){var captureMap=HouseRebuildStory.map();var out=Path.of(args[0]);Files.createDirectories(out);var r=new SceneRenderer();r.setRenderSize(1000,625);r.lamp=true;r.roomLights=new RoomLighting();
            double[][] v={{18.7,12.8,1.62,1.6,-.3},{14.8,13.4,4.42,-2,-.45},{14.6,17.1,4.42,-Math.PI/2,-.3},{15.7,19.2,1.62,-1.2,-.5}};String[] names={"household-records","unfinished-homework","inherited-mending","saved-frame"};
            for(int i=0;i<v.length;i++){var p=v[i];check(captureMap.traversable(p[0],p[2]-1.62,p[1]),"Review camera blocked "+names[i]);r.x=p[0];r.z=p[1];r.eyeHeight=p[2];r.yaw=p[3];r.pitch=p[4];r.drawNetwork(captureMap,List.of(),-1,.4);ImageIO.write(r.image(),"png",out.resolve(names[i]+"-runtime.png").toFile());}
            double[][] feed={{26.0,31.1,26.15,29.6},{25.1,26.4,26.15,24.5},{23.4,12.4,21.692,10.9}};String[] feedNames={"generator-service-outlet","visible-perimeter-feed","house-service-inlet"};
            for(int i=0;i<feed.length;i++){var p=feed[i];double x=p[0],z=p[1],y=captureMap.groundHeight(x,z,13);
                if(!captureMap.traversable(x,y,z)){boolean found=false;for(double radius:new double[]{.65,.85,1.05,1.3,1.6,2.0})for(int n=0;n<64&&!found;n++){double a=n*Math.PI/32,tx=p[2]+Math.sin(a)*radius,tz=p[3]+Math.cos(a)*radius,ty=captureMap.groundHeight(tx,tz,13);
                    double length=Math.sqrt(radius*radius+Math.pow(ty+1.62-.55,2)),t=1-.04/length;
                    if(captureMap.traversable(tx,ty,tz)&&captureMap.clearLineOfSight(tx,ty+1.62,tz,tx+(p[2]-tx)*t,ty+1.62+(.55-ty-1.62)*t,tz+(p[3]-tz)*t)){x=tx;y=ty;z=tz;found=true;}}check(found,"No actual feed review approach "+feedNames[i]);}
                r.x=x;r.z=z;r.eyeHeight=y+1.62;r.yaw=Math.atan2(p[2]-x,p[3]-z);r.pitch=-.55;r.drawNetwork(captureMap,List.of(),-1,.4);ImageIO.write(r.image(),"png",out.resolve(feedNames[i]+"-runtime.png").toFile());}
        }
        System.out.println("PASS8individually authored source-supported loose bodies,20physical steps,no deleted-parent floaters,3actual readable canonical records and required routes="+audit.positions());
    }
}
