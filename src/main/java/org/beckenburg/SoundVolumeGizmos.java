package org.beckenburg;
import java.awt.*;

/** X-ray editor overlays, intentionally excluded from playable map rendering. */
final class SoundVolumeGizmos {
    static void draw(Graphics2D g,SceneRenderer renderer,SoundVolumeEditor.View view){
        if(view==null||!view.visible()||view.document().volumes().isEmpty())return;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        for(var v:view.document().volumes()){
            g.setColor(v.id().equals(view.selected())?Color.YELLOW:new Color(95,235,160));
            g.setStroke(new BasicStroke(1.3f));bounds(g,renderer,v,0);
            g.setStroke(new BasicStroke(1,BasicStroke.CAP_BUTT,BasicStroke.JOIN_MITER,10,new float[]{4,4},0));bounds(g,renderer,v,v.falloff());
            var p=renderer.editorProjection(v.x(),v.y(),v.z());if(p!=null)g.drawString(v.id()+" · "+v.source(),(int)p[0]+5,(int)p[1]-5);
        }
        g.setColor(Color.YELLOW);g.drawString("EDITOR SOUND GIZMOS · X-RAY · dashed = falloff bounds",8,renderer.height()-9);
    }
    private static void line(Graphics2D g,SceneRenderer r,double[] a,double[] b){var p=r.editorProjection(a[0],a[1],a[2]);var q=r.editorProjection(b[0],b[1],b[2]);if(p==null||q==null)return;g.drawLine((int)p[0],(int)p[1],(int)q[0],(int)q[1]);}
    private static void bounds(Graphics2D g,SceneRenderer r,SoundVolume v,double extra){
        double x=v.width()/2+extra,y=v.height()/2+extra,z=v.depth()/2+extra;
        if(v.shape()==SoundVolume.Shape.SPHERE){for(int axis=0;axis<3;axis++)for(int i=0;i<48;i++){double a=i*Math.PI/24,b=(i+1)*Math.PI/24;double[] p={v.x(),v.y(),v.z()},q=p.clone();int u=(axis+1)%3,w=(axis+2)%3;p[u]+=Math.cos(a)*x;p[w]+=Math.sin(a)*x;q[u]+=Math.cos(b)*x;q[w]+=Math.sin(b)*x;line(g,r,p,q);}return;}
        double[][] points=new double[8][3];for(int i=0;i<8;i++)points[i]=new double[]{v.x()+((i&1)==0?-x:x),v.y()+((i&2)==0?-y:y),v.z()+((i&4)==0?-z:z)};
        for(int i=0;i<8;i++)for(int bit:new int[]{1,2,4})if((i&bit)==0)line(g,r,points[i],points[i|bit]);
    }
}
