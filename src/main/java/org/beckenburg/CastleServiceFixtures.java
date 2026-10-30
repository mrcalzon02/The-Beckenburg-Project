package org.beckenburg;
import java.util.ArrayList;
import java.util.List;

/** Station meshes and collision use one measured placement, including editor-moved stations. */
public final class CastleServiceFixtures {
    private CastleServiceFixtures(){}
    public record Piece(String mesh,double x,double y,double z,double scale,double yaw,boolean solid){}
    private static long revision=-1;
    private static List<Piece> cached=List.of();
    private static void floor(List<Piece> out,String mesh,double x,double z,double scale,double yaw){
        out.add(new Piece(mesh,x,0,z,scale,yaw,true));
    }
    private static void supported(List<Piece> out,String mesh,double x,double z,double scale,
            String support,double supportScale){
        out.add(new Piece(mesh,x,CastleHubDecor.meshHeight(support)*supportScale+.008,z,scale,0,false));
    }
    public static synchronized List<Piece> all(){
        var edits=CastleLevelEdits.current();if(revision==edits.revision())return cached;
        var out=new ArrayList<Piece>();
        for(var station:CastleHubMap.Station.values()){
            var p=edits.station(station);double x=p[0],z=p[1];
            switch(station){
                case ARMORY_UV,ARMORY_SALT,SPECTRAL -> {
                    double scale=station==CastleHubMap.Station.SPECTRAL?.78:.72;
                    floor(out,"display_cabinet_mp_2",x,z,scale,0);
                    // A workstation is not a display plinth with one arbitrary
                    // piece of equipment balanced on its roof. Portable tools
                    // belong in the multiplayer warehouse or mission loadout.
                }
                case ROLE -> floor(out,"wardrobe_mp_1",x,z,.9,Math.PI);
                case CONTRACT -> {
                    floor(out,"dinner_table_1",x,z,.74,0);
                    supported(out,"open_book_mp_1",x,z,.48,"dinner_table_1",.74);
                }
                case EXPEDITION -> {
                    floor(out,"tv_table_1_4",x,z,.84,0);
                    supported(out,"open_book_mp_1",x,z,.48,"tv_table_1_4",.84);
                }
                case MULTIPLAYER -> {
                    floor(out,"tv_table_1_4",x,z,.83,0);
                    floor(out,"pc_mp_1",x-.98,z-.17,.65,0);
                    supported(out,"blender_computer_fixed",x+.08,z-.06,.49,"tv_table_1_4",.83);
                    supported(out,"pc_keyboard_mp_1",x-.10,z+.16,.42,"tv_table_1_4",.83);
                }
                case OPTIONS -> {
                    floor(out,"tv_table_1_4",x,z,.78,0);
                    supported(out,"open_book_mp_1",x,z,.46,"tv_table_1_4",.78);
                }
                case ARCHIVE -> { /* Archive table is an independently editable furnishing. */ }
            }
        }
        cached=List.copyOf(out);revision=edits.revision();return cached;
    }
    public static boolean blocks(double x,double z,double radius){
        for(var p:all())if(p.solid){
            var b=SceneRenderer.castleBounds(p.mesh);double dx=x-p.x,dz=z-p.z;
            double lx=dx*Math.cos(p.yaw)+dz*Math.sin(p.yaw),lz=-dx*Math.sin(p.yaw)+dz*Math.cos(p.yaw);
            if(Math.abs(lx)<b[3]*p.scale*.5+radius&&Math.abs(lz)<b[5]*p.scale*.5+radius)return true;
        }
        return false;
    }
}
