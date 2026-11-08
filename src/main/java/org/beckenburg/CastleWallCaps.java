package org.beckenburg;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/** Exact exposed portions of a wall's horizontal top, after same-height slabs cover it. */
final class CastleWallCaps {
    private CastleWallCaps(){}
    record Patch(double x0,double z0,double x1,double z1) {
        double area(){return (x1-x0)*(z1-z0);}
    }
    static List<Patch> exposed(double x0,double z0,double x1,double z1,double top,
            List<CastleHubArchitecturalPlan.Deck> decks){
        if(!(x0<x1&&z0<z1&&Double.isFinite(x0+z0+x1+z1+top)))
            throw new IllegalArgumentException("Invalid wall cap");
        // Split only at actual slab intersections. A wall can be covered over one
        // room and still need an exposed cap above an adjacent courtyard or void.
        var overlaps=new ArrayList<CastleHubArchitecturalPlan.Deck>();
        var xs=new TreeSet<Double>();var zs=new TreeSet<Double>();
        xs.add(x0);xs.add(x1);zs.add(z0);zs.add(z1);
        for(var deck:decks){
            if(Math.abs(deck.height()-top)>1e-7)continue;
            double xa=Math.max(x0,deck.x0()),xb=Math.min(x1,deck.x1());
            double za=Math.max(z0,deck.z0()),zb=Math.min(z1,deck.z1());
            if(xb-xa<=1e-8||zb-za<=1e-8)continue;
            overlaps.add(deck);
            xs.add(xa);xs.add(xb);zs.add(za);zs.add(zb);
        }
        if(overlaps.isEmpty())return List.of(new Patch(x0,z0,x1,z1));
        var xx=new ArrayList<>(xs);var zz=new ArrayList<>(zs);
        var patches=new ArrayList<Patch>();
        for(int i=0;i<xx.size()-1;i++)for(int j=0;j<zz.size()-1;j++){
            double a=xx.get(i),b=xx.get(i+1),c=zz.get(j),d=zz.get(j+1);
            if(b-a<=1e-8||d-c<=1e-8)continue;
            double mx=(a+b)*.5,mz=(c+d)*.5;
            boolean covered=false;
            for(var deck:overlaps)if(mx>=deck.x0()&&mx<=deck.x1()
                    &&mz>=deck.z0()&&mz<=deck.z1()){covered=true;break;}
            if(!covered)patches.add(new Patch(a,c,b,d));
        }
        return List.copyOf(patches);
    }
}
