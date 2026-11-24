"""Fit the Northwatch cistern, two usable wash basins and their service runs."""
import math
from pathlib import Path
import sys

import bpy
from mathutils import Matrix,Vector

sys.path.insert(0,str(Path(__file__).resolve().parent))
from build_northwatch_distribution import BASE, ROOT, build
from build_college_electrical import ASSETS,bounds,point,world
from review_roster_macroterrain import sha
from verify_college_services import bvh

PARENT=BASE/"northwatch-complete-power-pass"
OUT=BASE/"northwatch-water-pass/wld-08"
CISTERN=ROOT/"development/levels/primitive-props-v1/models/level_blockout_lighthouse_cistern.glb"
BASINS=(
    ("shed-wash-basin",5,(3.47,0,8.49),0,((7,0,16),(5.6,0,15),(5.6,0,10.1),(3.47,0,9.68))),
    ("lab-wash-basin",0,(2.49,0,31.65),math.pi/2,((7,0,29.25),(7,0,31.65),(3.68,0,31.65))),
)
COPPER=1.16
WEST=2.30


def install(a):
    structure=[o for o in a.scene.objects if o.type=="MESH"]
    original=bvh(structure)
    anchors,sleeves,fixtures=[],[],[]
    supply=a.housing
    waste=a.conduit

    def measured(at,direction,span):
        p,n,_,_=original.ray_cast(point(*at),point(*direction),span)
        assert p is not None and n.dot(point(*direction))<-.98,(at,direction,p,n)
        return world(p)

    def floor_bearing(tag,xs,zs):
        for x in xs:
            for z in zs:
                seat=measured((x,.40,z),(0,-1,0),.6)
                assert abs(seat[1])<.002,(tag,seat)
                anchors.append(dict(id=tag,contact=seat))

    def form(name,path,radius,material,visible=True):
        a.raceway(name,path,radius,visible)
        for obj in a.built[-(len(path)-1):]:
            obj.data.materials.clear()
            obj.data.materials.append(material)

    def west_clamp(label,y,z):
        contact=measured((2.43,y,z),(-1,0,0),.23)
        assert abs(contact[0]-2.24)<.004,(label,contact)
        a.box(label,(2.235,z-.035,2.347,z+.035),y-.038,y+.038,a.mount,"load-bearing pipe clip","water")
        anchors.append(dict(id=label,contact=contact))

    before=set(a.scene.objects)
    bpy.ops.import_scene.gltf(filepath=str(CISTERN))
    tank=[o for o in set(a.scene.objects)-before if o.type=="MESH"]
    assert len(tank)==1
    tank=tank[0]
    tank.matrix_world=Matrix.Translation(point(3.64,-.005,12.40))@tank.matrix_world
    tank.name="Northwatch insulated gravity cistern"
    tank["source_asset"]=CISTERN.stem
    tank["role"]="floor-supported cistern"
    a.built.append(tank)
    bpy.context.view_layer.update()
    floor_bearing("cistern base",(3.22,3.64,4.06),(11.98,12.40,12.82))
    tank_bounds=bounds([tank])
    assert tank_bounds[0][1]==0 and tank_bounds[1][1]<2.30
    # Welded withdrawal tap seats on the actual shell just below the upper
    # reserve level. The pipe stays lower than the usable head in the tank.
    hit,n,_,_=bvh([tank]).ray_cast(point(2.44,1.53,12.4),point(1,0,0),.65)
    assert hit is not None and n.x<-.93,(hit,n)
    x,y,z=world(hit)
    a.box("cistern withdrawal flange",(x-.032,z-.06,x+.008,z+.06),y-.060,y+.060,
          a.mount,"tank shell-backed withdrawal flange","water")
    form("cistern gravity outlet",[(x+.008,y,z),(WEST,y,z),(WEST,COPPER,z)],.022,supply)
    west_clamp("cistern outlet saddle",1.53,12.40)

    form("water main along shed west wall",[(WEST,COPPER,8.32),(WEST,COPPER,12.40),
         (WEST,COPPER,25.72)],.020,supply)
    for z in (8.8,10.1,11.5,13.2,14.6,16,17.4,18.8,20.2,21.6,23,24.4,25.6):
        west_clamp("shed water main saddle",COPPER,z)
    faces=[]
    for z,d in ((25.76,1),(26.24,-1)):
        p=measured((WEST,COPPER,z-.12*d),(0,0,d),.18)
        assert abs(p[2]-z)<.005
        faces.append(p)
        a.box("closed wall water gland",(WEST-.052,z-.027,WEST+.052,z+.027),
              COPPER-.053,COPPER+.053,a.mount,"sealed wet service entry","water")
    form("closed lab water sleeve",[(WEST,COPPER,25.72),(WEST,COPPER,26.28)],.021,supply,False)
    sleeves.append(dict(id="lab water sleeve",faces=faces))
    form("lab wall water main",[(WEST,COPPER,26.28),(WEST,COPPER,31.65)],.019,supply)
    for z in (26.50,27.7,29,30.25,31.5):
        west_clamp("lab water main saddle",COPPER,z)

    for name,owner,at,yaw,_ in BASINS:
        basin=a.place("level_bath_basin",Matrix.Translation(point(*at))@Matrix.Rotation(yaw,4,"Z"),
                      name,"fitted supported wash basin","water")
        x,_,z=at
        floor_bearing(name,(x-.07,x,x+.07),(z-.07,z,z+.07))
        model=bvh(basin)
        if owner==5:
            for clip_x in (2.67,3.27):
                support=measured((clip_x,COPPER,8.47),(0,0,-1),.27)
                a.box("shed wash supply saddle",(clip_x-.035,8.236,clip_x+.035,8.355),
                      COPPER-.04,COPPER+.04,a.mount,"north wall wet-service clip","water")
                anchors.append(dict(id="shed branch",contact=support))
            path=[(WEST,COPPER,8.32),(x,COPPER,8.32),(x,COPPER,8.257),(x,1.0,8.257)]
            terminal=north_terminal(model,x,z)
            path.append((terminal[0],terminal[1],terminal[2]+.003))
        else:
            terminal=west_terminal(model,x,z)
            path=[(WEST,COPPER,z),(2.257,COPPER,z),(2.257,1.0,z),(terminal[0]+.003,terminal[1],terminal[2])]
        form(name+" spout feed",path,.015,supply)
        # The pedestal's hidden drain joins a capped waste header below the
        # original slab. Keep both bulkheads closed in the native collision mesh.
        below=-.12 if owner==5 else -.17
        out=-.40 if owner==5 else -.46
        form(name+" pedestal waste",[(x,.14,z),(x,below,z),(1.76,out,z)],.024,waste,False)
        form(name+" below-grade collection stub",[(1.76,out,z),(1.58,out-.018,z)],.032,waste,False)
        a.effect(name,"water-basin",owner,(x,.86,z),-yaw,"gravity-cistern")
        fixtures.append(dict(id=name,owner=owner,position=at,yaw=yaw,terminal=terminal,bounds=bounds(basin),
                             waste_endpoint=(1.58,out-.018,z)))
    return dict(anchors=anchors,sleeves=sleeves,fixtures=fixtures,cistern_bounds=tank_bounds,raceways=a.routes,
                canonical_assets={CISTERN.stem:sha(CISTERN),"level_bath_basin":sha(ASSETS/"level_bath_basin.glb")})


def north_terminal(model,x,z):
    p,normal,_,_=model.ray_cast(point(x,1,z-.45),point(0,0,1),.45)
    assert p is not None and normal.y>.90,(p,normal)
    return world(p)


def west_terminal(model,x,z):
    p,normal,_,_=model.ray_cast(point(x-.45,1,z),point(1,0,0),.45)
    assert p is not None and normal.x<-.90,(p,normal)
    return world(p)


if __name__=="__main__":
    build(PARENT,OUT,[(key,owner,None,None,route) for key,owner,_,_,route in BASINS],install,"station-with-water")
