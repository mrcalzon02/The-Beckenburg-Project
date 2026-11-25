package org.beckenburg.editor;

/** Maps one selected unit square of native UV space to the material's source image. */
public record EditorUvTile(int u,int v) {
    public static final EditorUvTile ORIGIN=new EditorUvTile(0,0);
    public EditorUvTile forMaterial(EditorMeshMaterial material){
        if(material==null)return this;
        int x=material.wrapS()==33071?0:u, y=material.wrapT()==33071?0:v;
        return x==u && y==v?this:new EditorUvTile(x,y);
    }
    private static double orient(double value,int tile,int wrap){
        return wrap==33648 && (tile&1)!=0 ? 1-value : value;
    }
    public double imageU(double nativeU,EditorMeshMaterial material){
        return orient(nativeU-u,u,material==null?10497:material.wrapS());
    }
    public double imageV(double nativeV,EditorMeshMaterial material){
        double local=orient(nativeV-v,v,material==null?10497:material.wrapT());
        return material!=null && material.origin()==EditorMeshMaterial.Origin.TOP_LEFT?local:1-local;
    }
    public double nativeU(double imageU,EditorMeshMaterial material){
        return u+orient(imageU,u,material==null?10497:material.wrapS());
    }
    public double nativeV(double imageV,EditorMeshMaterial material){
        double local=material!=null && material.origin()==EditorMeshMaterial.Origin.TOP_LEFT?imageV:1-imageV;
        return v+orient(local,v,material==null?10497:material.wrapT());
    }
    public static boolean contains(EditorTopologyProject.Face face,double u,double v){
        double area=cross(face.a(),face.b(),face.c().u(),face.c().v());
        if(!Double.isFinite(area)||Math.abs(area)<=1e-15)return false;
        double a=cross(face.a(),face.b(),u,v),b=cross(face.b(),face.c(),u,v),c=cross(face.c(),face.a(),u,v);
        return (a>=0&&b>=0&&c>=0)||(a<=0&&b<=0&&c<=0);
    }
    private static double cross(EditorTopologyProject.Corner a,EditorTopologyProject.Corner b,double u,double v){
        return (b.u()-a.u())*(v-a.v())-(b.v()-a.v())*(u-a.u());
    }
}
