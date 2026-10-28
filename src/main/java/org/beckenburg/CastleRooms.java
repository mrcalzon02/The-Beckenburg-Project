package org.beckenburg;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/** Metre-based headquarters construction contract. Raster cells are an editor overlay only. */
public final class CastleRooms {
    private CastleRooms(){}
    public static final double SLAB=.22;
    public record Room(String id,String name,double x0,double z0,double x1,double z1,
                       double floor,double ceiling,boolean outside,int material){
        public boolean contains(double x,double z){return x>=x0&&x<=x1&&z>=z0&&z<=z1;}
    }
    public static final List<Room> ALL=List.of(
        new Room("armory","Armory",1,1,10.5,11.5,0,3.4,false,MaterialAtlas.PARQUET),
        new Room("contracts","Briefing and contracts",10.5,1,19.5,11.5,0,3.4,false,MaterialAtlas.CARPET),
        new Room("wardrobe","Wardrobe and lounge",19.5,1,28.5,11.5,0,3.4,false,MaterialAtlas.PARQUET),
        new Room("practice","Practice and common room",1,11.5,10.5,34,0,3.4,false,MaterialAtlas.STONE),
        new Room("court","Arrival courtyard",10.5,11.5,20.5,34,0,Double.NaN,true,MaterialAtlas.STONE),
        new Room("operations","Expedition operations",20.5,11.5,28.5,34,0,3.4,false,MaterialAtlas.PARQUET),
        new Room("archive","Common archive",28.5,4,43,17.5,0,3.4,false,MaterialAtlas.PARQUET),
        new Room("store","Quartermaster store",28.5,17.5,43,34,0,3.4,false,MaterialAtlas.STONE),
        new Room("gate","Gate threshold",13.2,34,17.8,36,0,Double.NaN,true,MaterialAtlas.STONE),
        new Room("library","Upper library",10.5,1,19.5,11.5,3.4,6.8,false,MaterialAtlas.PARQUET));
    public static Room at(double x,double feet,double z){
        for(var r:ALL)if(Math.abs(r.floor-feet)<.3&&r.contains(x,z))return r;
        return null;
    }
    public static boolean ground(double x,double z){return at(x,0,z)!=null;}
    public static double mountHeight(double x,double z,boolean upper){
        var room=at(x,upper?3.4:0,z);
        if(room==null||room.outside)throw new IllegalArgumentException("No ceiling at "+x+","+z);
        return room.ceiling-SLAB;
    }
    public static String circuit(double x,double y,double z){
        var r=at(x,y>4?3.4:0,z);return r==null?"court":r.id;
    }
    private static long structureRevision=-1;
    private static List<CastleHubArchitecturalPlan.Deck> cachedStructure=List.of();
    public static synchronized List<CastleHubArchitecturalPlan.Deck> structuralDecks(){
        if(structureRevision==CastleLevelEdits.current().revision())return cachedStructure;
        var out=new ArrayList<>(CastleLevelEdits.current().decks());
        for(var r:ALL){
            if(r.floor==0)out.add(new CastleHubArchitecturalPlan.Deck(r.x0,r.z0,r.x1,r.z1,0,r.material));
            if(!r.outside)out.add(new CastleHubArchitecturalPlan.Deck(r.x0,r.z0,r.x1,r.z1,r.ceiling,MaterialAtlas.CEILING));
        }
        structureRevision=CastleLevelEdits.current().revision();return cachedStructure=List.copyOf(out);
    }
    public record Cell(double x0,double z0,double x1,double z1,double top,int material){}
    public record Edge(double x0,double z0,double x1,double z1,double top){}
    /** Split the union at every authored edge. Overlapping decks produce one slab. */
    public static List<Cell> slabCells(List<CastleHubArchitecturalPlan.Deck> decks){
        var heights=new TreeSet<Double>();for(var d:decks)heights.add(d.height());
        var out=new ArrayList<Cell>();
        for(double h:heights){
            var xs=new TreeSet<Double>();var zs=new TreeSet<Double>();
            for(var d:decks)if(d.height()==h){xs.add(d.x0());xs.add(d.x1());zs.add(d.z0());zs.add(d.z1());}
            var xx=new ArrayList<>(xs);var zz=new ArrayList<>(zs);
            for(int i=0;i<xx.size()-1;i++)for(int j=0;j<zz.size()-1;j++){
                double x=(xx.get(i)+xx.get(i+1))*.5,z=(zz.get(j)+zz.get(j+1))*.5;
                for(var d:decks)if(d.height()==h&&x>d.x0()&&x<d.x1()&&z>d.z0()&&z<d.z1()){
                    out.add(new Cell(xx.get(i),zz.get(j),xx.get(i+1),zz.get(j+1),h,d.material()));break;
                }
            }
        }
        return List.copyOf(out);
    }
    public static boolean supported(List<Cell> cells,double x,double z,double h){
        return cells.stream().anyMatch(c->c.top==h&&x>=c.x0&&x<=c.x1&&z>=c.z0&&z<=c.z1);
    }
    public static List<Edge> edges(List<Cell> cells){
        var out=new ArrayList<Edge>();
        for(var c:cells){
            double mx=(c.x0+c.x1)*.5,mz=(c.z0+c.z1)*.5;
            if(!supported(cells,c.x0-.001,mz,c.top))out.add(new Edge(c.x0,c.z0,c.x0,c.z1,c.top));
            if(!supported(cells,c.x1+.001,mz,c.top))out.add(new Edge(c.x1,c.z0,c.x1,c.z1,c.top));
            if(!supported(cells,mx,c.z0-.001,c.top))out.add(new Edge(c.x0,c.z0,c.x1,c.z0,c.top));
            if(!supported(cells,mx,c.z1+.001,c.top))out.add(new Edge(c.x0,c.z1,c.x1,c.z1,c.top));
        }
        return List.copyOf(out);
    }
    private static long guardRevision=-1;
    private static List<Edge> cachedGuards=List.of();
    public static synchronized List<Edge> guards(){
        if(guardRevision==CastleLevelEdits.current().revision())return cachedGuards;
        var out=new ArrayList<Edge>();
        for(var e:edges(slabCells(CastleLevelEdits.current().decks()))){
            if(e.top!=3.4)continue;
            double x=(e.x0+e.x1)*.5,z=(e.z0+e.z1)*.5;
            if(Math.abs(z-22)<.001&&x>=16&&x<=19)continue;
            boolean wall=CastleLevelEdits.current().walls().stream().anyMatch(w->
                !w.hasDoor()&&w.bottom()<=3.4&&w.top()>4.4&&
                (w.horizontal()?Math.abs(z-w.z0())<.03&&x>=w.x0()&&x<=w.x1():
                    Math.abs(x-w.x0())<.03&&z>=w.z0()&&z<=w.z1()));
            if(!wall)out.add(e);
        }
        guardRevision=CastleLevelEdits.current().revision();return cachedGuards=List.copyOf(out);
    }
    public static boolean guardBlocks(double x,double z,double radius){
        for(var e:guards()){
            double px=Math.max(e.x0,Math.min(e.x1,x)),pz=Math.max(e.z0,Math.min(e.z1,z));
            if(Math.hypot(x-px,z-pz)<radius+.055)return true;
        }
        return false;
    }
}
