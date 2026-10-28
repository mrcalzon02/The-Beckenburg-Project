package org.beckenburg;

import java.lang.reflect.*;
import java.util.*;
import java.nio.file.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

/** Exports the exact runtime triangles, UVs, textures and authored sockets for Blender. */
public final class LevelSceneSource {
    record Module(String id,String revision,String title,List<LevelDefinition.Room> rooms,List<LevelDefinition.Socket> sockets,List<LevelDefinition.Portal> portals,List<LevelDefinition.Collider> colliders,LevelDefinition.Dock dock){}
    record MaterialInfo(int id,String name){}
    record LooseSource(LooseClutter.State state,double half,double radius,double mass){}
    static Object read(Object value,String field)throws Exception{
        Method m=value.getClass().getDeclaredMethod(field);m.setAccessible(true);return m.invoke(value);
    }
    static String json(Object v)throws Exception{
        if(v==null)return "null";
        if(v instanceof Enum<?> e)return json(e.name());
        if(v instanceof String s)return "\""+s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n")+"\"";
        if(v instanceof Number||v instanceof Boolean)return v.toString();
        if(v instanceof Collection<?> list){StringJoiner j=new StringJoiner(",","[","]");for(Object x:list)j.add(json(x));return j.toString();}
        if(v.getClass().isRecord()){
            StringJoiner j=new StringJoiner(",","{","}");for(RecordComponent c:v.getClass().getRecordComponents())j.add(json(c.getName())+":"+json(read(v,c.getName())));return j.toString();
        }
        throw new IllegalArgumentException(v.getClass().toString());
    }
    @SuppressWarnings("unchecked")
    public static void main(String[] args)throws Exception{
        if(args.length>0&&args[0].equals("CASTLE_REBUILD")){castle();return;}
        if(args.length>0&&(args[0].equals("ARRIVAL")||args[0].equals("CASTLE_SURROUNDINGS")||args[0].startsWith("ENCLOSURE_"))){module(args[0]);return;}
        HouseMap map=args.length>0&&args[0].equals("BLACKPINE")?BlackpineLevel.map():new HouseMap(args.length>0?HouseMap.Site.valueOf(args[0]):HouseMap.Site.HOUSE);
        map.configureHiding(100); // Editable authoring source includes every potential cupboard.
        Path out=Path.of("build/level-verification/scenes",map.level().id());Files.createDirectories(out.resolve("textures"));
        SceneRenderer scene=new SceneRenderer();scene.x=21;scene.z=22;
        Field all=SceneRenderer.class.getDeclaredField("exportFullLevel");all.setAccessible(true);all.setBoolean(scene,true);
        Method assemble=SceneRenderer.class.getDeclaredMethod("levelArchitecture",List.class,HouseMap.class);assemble.setAccessible(true);
        List<Object> faces=new ArrayList<>();assemble.invoke(scene,faces,map);
        Files.writeString(out.resolve("definition.json"),json(map.level()));
        Files.writeString(out.resolve("loose-objects.json"),json(map.clutter().states().stream().map(s->new LooseSource(s,s.kind().half,s.kind().radius,s.kind().mass)).toList()));
        Files.writeString(out.resolve("event-fixtures.json"),json(map.fixtures().states()));
        Files.writeString(out.resolve("lighting-sources.json"),json(new FixtureLighting(map).sources()));
        Files.writeString(out.resolve("scene-settings.json"),"{\"hidingDensity\":100}");
        writeFaces(out,faces);
    }
    @SuppressWarnings("unchecked")
    static void castle()throws Exception{
        var map=new CastleHubMap();var scene=new SceneRenderer();scene.x=22;scene.z=18;
        Field all=SceneRenderer.class.getDeclaredField("exportFullLevel");all.setAccessible(true);all.setBoolean(scene,true);
        scene.drawCastle(map,2.5,true);
        Field geometry=SceneRenderer.class.getDeclaredField("castleExportGeometry");geometry.setAccessible(true);
        var faces=(List<Object>)geometry.get(scene);
        var rooms=new ArrayList<LevelDefinition.Room>();int index=0;
        for(var r:CastleRooms.ALL)rooms.add(new LevelDefinition.Room(index++,r.name(),r.x0(),r.z0(),r.x1(),r.z1(),r.floor(),
            r.outside()?12:r.ceiling()-CastleRooms.SLAB-r.floor(),"stone",r.outside()?"exterior":"headquarters",
            new LevelDefinition.Vec((r.x0()+r.x1())*.5,r.floor(),(r.z0()+r.z1())*.5)));
        rooms.add(new LevelDefinition.Room(13,"Exterior landscape",-180,-160,220,220,-.65,80,"earth","exterior",new LevelDefinition.Vec(22,0,18)));
        var sockets=new ArrayList<LevelDefinition.Socket>();
        for(var station:CastleHubMap.Station.values()){var p=CastleLevelEdits.current().station(station);
            sockets.add(new LevelDefinition.Socket(station.name(),"station",0,p[0],0,p[1],0,station.label));}
        var definition=new Module("castle-rebuild","room-structure-v2","Castle Beckenburg rebuilt headquarters",rooms,sockets,List.of(),List.of(),new LevelDefinition.Dock(13.3,0,27.7,0));
        Path out=Path.of("build/level-verification/scenes/castle-rebuild");Files.createDirectories(out);
        Files.writeString(out.resolve("definition.json"),json(definition));
        Files.writeString(out.resolve("lighting-sources.json"),json(new FixtureLighting(map).sources()));
        Files.writeString(out.resolve("scene-settings.json"),"{\"hidingDensity\":0}");
        Files.writeString(out.resolve("retail-displays.json"),json(CastleWarehouse.exhibits()));
        writeFaces(out,faces);
    }
    @SuppressWarnings("unchecked")
    static void writeFaces(Path out,List<Object> faces)throws Exception{
        Files.createDirectories(out.resolve("textures"));Files.writeString(out.resolve("geometry.json"),json(faces));
        Set<Integer> mats=new TreeSet<>();for(Object f:faces)mats.add((Integer)read(f,"material"));
        Files.writeString(out.resolve("material-metadata.json"),json(mats.stream().map(id->new MaterialInfo(id,id>=256?GlbProps.materialName(id):id==LevelMaterials.ROCK?"Original CC0 rock":"Environment surface")).toList()));
        Field tex=GlbProps.class.getDeclaredField("textures");tex.setAccessible(true);
        Field factors=GlbProps.class.getDeclaredField("diffuseFactors");factors.setAccessible(true);
        List<BufferedImage> images=(List<BufferedImage>)tex.get(null);List<double[]> colors=(List<double[]>)factors.get(null);
        for(int mat:mats)if(mat>=0){
            BufferedImage im;
            if(mat==GlbProps.VAN_MAP_MATERIAL){
                im=new BufferedImage(256,256,BufferedImage.TYPE_INT_RGB);for(int y=0;y<256;y++)for(int x=0;x<256;x++)im.setRGB(x,y,GlbProps.sample(mat,(x+.5)/256.0,1-(y+.5)/256.0));
            }else if(mat>=256){
                BufferedImage original=images.get(mat-256);im=new BufferedImage(original.getWidth(),original.getHeight(),BufferedImage.TYPE_INT_RGB);
                double[] c=colors.get(mat-256);
                for(int y=0;y<im.getHeight();y++)for(int x=0;x<im.getWidth();x++){
                    int rgb=original.getRGB(x,y);im.setRGB(x,y,(Math.min(255,(int)(((rgb>>16)&255)*c[0]))<<16)|(Math.min(255,(int)(((rgb>>8)&255)*c[1]))<<8)|Math.min(255,(int)((rgb&255)*c[2])));
                }
            }else{
                int size=mat>=32?512:32;im=new BufferedImage(size,size,BufferedImage.TYPE_INT_RGB);
                for(int y=0;y<size;y++)for(int x=0;x<size;x++)im.setRGB(x,y,mat>=32?LevelMaterials.sample(mat,x/(double)size,1-(y+.5)/size):MaterialAtlas.sample(mat,x/(double)size,y/(double)size));
            }
            ImageIO.write(im,"PNG",out.resolve("textures/"+mat+".png").toFile());
        }
        System.out.println("Exact runtime source "+out.getFileName()+": "+faces.size()+" textured triangles, "+mats.size()+" materials");
    }
    static void invoke(SceneRenderer scene,String name,Class<?> argument,Object value,List<Object> faces)throws Exception{var method=SceneRenderer.class.getDeclaredMethod(name,List.class,argument);method.setAccessible(true);method.invoke(scene,faces,value);}
    static void module(String mode)throws Exception{
        var scene=new SceneRenderer();List<Object> faces=new ArrayList<>();String name;Object definition;
        if(mode.equals("CASTLE_SURROUNDINGS")){
            name="castle-surroundings";var method=SceneRenderer.class.getDeclaredMethod("castleSurroundings");method.setAccessible(true);faces.addAll((List<?>)method.invoke(scene));
            definition=new Module(name,"castle-mountain-v1","Castle mountain surroundings",List.of(new LevelDefinition.Room(13,"Castle mountain backdrop",-180,-160,220,220,0,80,"earth","exterior",new LevelDefinition.Vec(15.5,0,34))),List.of(),List.of(),List.of(),new LevelDefinition.Dock(15.5,0,34,Math.PI));
        }else if(mode.equals("ARRIVAL")){
            name="arrival-road";var van=new VanStaging(new VanStaging.Placement(0,0,0));invoke(scene,"arrivalRoadGeometry",VanStaging.class,van,faces);
            var mesh=SceneRenderer.class.getDeclaredMethod("vanModel",List.class,VanStaging.class,Set.class);mesh.setAccessible(true);mesh.invoke(scene,faces,van,Set.of());
            var rooms=List.of(new LevelDefinition.Room(13,"Shared arrival tile",-6.3,-22,6.3,5,0,12,"asphalt","exterior",new LevelDefinition.Vec(0,0,-6)),new LevelDefinition.Room(14,"Van interior",-.892,-2.128,.892,2.128,.5,2.125,"asphalt","van",new LevelDefinition.Vec(0,.5,0)));
            definition=new Module(name,VanArrivalTile.ID,"Shared van and arrival road",rooms,List.of(new LevelDefinition.Socket("van-anchor","arrival_tile",13,0,0,0,0,VanArrivalTile.ID)),List.of(),List.of(),new LevelDefinition.Dock(0,0,0,0));
        }else{
            var setting=LevelEnclosure.Setting.valueOf(mode.substring(10));var template=LevelEnclosure.template(setting);definition=template;name=template.id();
            var method=SceneRenderer.class.getDeclaredMethod("enclosureScenery",LevelDefinition.class);method.setAccessible(true);faces.addAll((List<?>)method.invoke(scene,definition));
            var d=template.dock();var van=new VanStaging(new VanStaging.Placement(d.x(),d.z(),d.yaw()));invoke(scene,"arrivalRoadGeometry",VanStaging.class,van,faces);
            method=SceneRenderer.class.getDeclaredMethod("arrivalSurroundings",List.class,VanStaging.class,LevelEnclosure.Setting.class);method.setAccessible(true);method.invoke(scene,faces,van,setting);
        }
        Path out=Path.of("build/level-verification/scenes",name);Files.createDirectories(out);Files.writeString(out.resolve("definition.json"),json(definition));Files.writeString(out.resolve("scene-settings.json"),"{\"hidingDensity\":0}");
        if(mode.equals("ARRIVAL")){var base=new HouseMap();var emitters=new ArrayList<FixtureLighting.Source>();for(var source:new FixtureLighting(base).sources())if(source.room()==14){var p=base.van().local(source.x(),source.z());emitters.add(new FixtureLighting.Source(source.id(),source.kind(),14,0,p.x(),source.y(),p.z(),0,-1,0,source.radius(),source.power(),source.red(),source.green(),source.blue()));}Files.writeString(out.resolve("lighting-sources.json"),json(emitters));}
        writeFaces(out,faces);
    }

}
