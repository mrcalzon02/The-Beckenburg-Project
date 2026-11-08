package org.beckenburg;

import java.awt.Point;
import java.util.*;
import org.beckenburg.editor.EditorTopologyProject;

/** One drag over a single published material-review frame. */
final class MaterialReviewStroke {
    final MaterialReviewFrame frame;
    final EditorTopologyProject.Observation observed;
    final long anchor;
    final String layer;
    final int color,padding;
    private final String material;
    final Map<Long,Set<Integer>> samples=new LinkedHashMap<>();
    final BitSet coverage=new BitSet();
    private int pairs;
    boolean overflow;
    private Point previous;
    MaterialReviewStroke(MaterialReviewFrame frame,EditorTopologyProject.Observation observed,long anchor,String layer,int color,String material,int padding){
        this.frame=frame;this.observed=observed;this.anchor=anchor;this.layer=layer;this.color=color;this.material=material;this.padding=padding;
    }
    void sample(Point p){
        if(frame.pixel(p.x,p.y)<0){previous=null;return;}
        Point a=previous==null?p:previous;
        int steps=Math.max(1,Math.max(Math.abs(p.x-a.x),Math.abs(p.y-a.y)));
        for(int i=0;i<=steps;i++){
            int pixel=frame.pixel(a.x+(int)Math.round((p.x-a.x)*i/(double)steps),a.y+(int)Math.round((p.y-a.y)*i/(double)steps));
            long face=frame.face(pixel);int texel=frame.texel(pixel);
            if(texel>=0 && frame.boundTo(face,material) && !overflow){
                coverage.set(pixel);
                if(samples.computeIfAbsent(face,key->new LinkedHashSet<>()).add(texel) && ++pairs>65536)overflow=true;
            }
        }
        previous=p;
    }
}
