package org.beckenburg;

import java.util.Random;

/**
 * Every point inside a real authored light sphere must retain its source in
 * the spatial influence index. Cells entirely beyond that sphere must never
 * be allocated solely for the source. Shader/visibility behavior is unchanged.
 */
public final class LightSphereCellIndexTest {
    private static void ok(boolean yes,String message){
        if(!yes)throw new AssertionError(message);
    }
    private static int floor(double number,double cell){
        return (int)Math.floor(number/cell);
    }
    private static long boxCells(FixtureLighting.Source source,double cell){
        long xx=floor(source.x()+source.radius(),cell)
            -floor(source.x()-source.radius(),cell)+1L;
        long yy=floor(source.y()+source.radius(),cell)
            -floor(source.y()-source.radius(),cell)+1L;
        long zz=floor(source.z()+source.radius(),cell)
            -floor(source.z()-source.radius(),cell)+1L;
        return xx*yy*zz;
    }
    public static void main(String[] args){
        ok(FixtureLighting.sphereTouchesCell(1,1,1,1,0,0,0,2),
            "Light inside a cell does not reach that cell");
        ok(!FixtureLighting.sphereTouchesCell(0,0,0,1,4,4,4,2),
            "A cell far beyond the radius was incorrectly retained");
        ok(FixtureLighting.sphereTouchesCell(0,0,0,2,1,0,0,2),
            "An exactly touching edge must be retained conservatively");
        ok(FixtureLighting.sphereTouchesCell(0,0,0,2,-1,0,0,2),
            "Negative-index cells must be symmetrical");
        var random=new Random(20261121L);
        long saved=0,actual=0,box=0;
        for(var site:new HouseMap.Site[]{
                HouseMap.Site.HOUSE,HouseMap.Site.FARMHOUSE,HouseMap.Site.SCHOOL}){
            var map=new HouseMap(site);
            var lighting=new FixtureLighting(map);
            long cube=0;
            for(var source:lighting.sources()){
                // The module restricts cell lengths to 2, 4 or 8 metres.
                double cell=dev.mrcalzon.lighting.LightingModule.current().cellMetres();
                cube+=boxCells(source,cell);
                for(int index=0;index<32;index++){
                    double dx=random.nextDouble()*2-1,dy=random.nextDouble()*2-1,
                        dz=random.nextDouble()*2-1;
                    double norm=Math.sqrt(dx*dx+dy*dy+dz*dz);
                    if(norm<1e-7){index--;continue;}
                    double reach=source.radius()*(.001+.998*random.nextDouble());
                    double x=source.x()+reach*dx/norm,
                        y=source.y()+reach*dy/norm,
                        z=source.z()+reach*dz/norm;
                    ok(lighting.indexesSourceAt(source.id(),x,y,z),
                        "Authored light source "+source.id()+" lost a point inside "
                        +site+" physical sphere, distance="+reach);
                }
            }
            long indexed=lighting.indexedSourceCellReferences();
            ok(indexed>0&&indexed<=cube,"Invalid source-cell index cardinality");
            ok(indexed<cube,"The source spatial index still covers a full cube");
            actual+=indexed;box+=cube;
            saved+=cube-indexed;
            System.out.println("SPHERE INDEX "+site+" old_cubic_source_cells="
                +cube+" actual_spherical_source_cells="+indexed
                +" avoided_source_cell_entries="+(cube-indexed));
        }
        ok(saved>0,"No physical source references avoided by sphere culling");
        System.out.println("PASS exact authored spherical spatial index: saved "
            +saved+" of "+box+" cubic source-cell entries, retained "+actual);
    }
}
