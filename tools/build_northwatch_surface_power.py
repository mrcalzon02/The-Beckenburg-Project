"""Feed Northwatch's cold store, dorm, mess, and array shelter."""
import math
from pathlib import Path
import sys

from mathutils import Vector

sys.path.insert(0,str(Path(__file__).resolve().parent))
from build_northwatch_distribution import BASE, HIGH, build
from build_college_electrical import ASSETS, point, world
from review_roster_macroterrain import sha
from verify_college_services import bvh

PARENT=BASE/"northwatch-distribution-pass"
OUT=BASE/"northwatch-surface-power-pass/wld-08"
ROOMS=(
    ("store",6,(13.05,45.44,0,-1),(10.5,50.2),((11,0,45.32),(11,0,47),(13.05,0,47),(13.05,0,45.44))),
    ("dorm",1,(45.44,38.9,-1,0),(53,32),((45.31,0,41),(46.7,0,41),(46.7,0,38.9),(45.44,0,38.9))),
    ("mess",2,(54.85,45.44,0,-1),(53,50),((53,0,45.31),(53,0,47),(54.85,0,47),(54.85,0,45.44))),
    ("shelter",9,(63.44,12.44,-1,0),(66,16.2),((63.31,0,14),(64.6,0,14),(64.6,0,12.44),(63.44,0,12.44))),
)
BURIED=-.62
TRENCHES=(
    ("east service trunk",((44.06,13.8),(56.5,13.8),(61.94,13.8),(61.94,11.6))),
    ("dorm lateral",((56.5,13.8),(56.5,25.94))),
)
PARTY=(
    ("lab to cold store","z",15,43.76,44.24,"lab S end","store N end"),
    ("dorm to mess","z",59,43.76,44.24,"dorm S end","mess N end"),
)
EXTERIOR=(
    ("records exterior drop","x",13.8,43.76,44.0,"records E pier0","records E pier0",1),
    ("dorm north riser","z",56.5,26.0,26.24,"dorm N end","dorm N end",-1),
    ("shelter west riser","x",11.6,62.0,62.24,"shelter W pier0","shelter W pier0",-1),
)


def install(assembly):
    original=bvh([obj for obj in assembly.scene.objects if obj.type=="MESH"])
    anchors=[]
    soils=[]
    sleeves=[]

    def box(label,x,y,z,low,high,material,role):
        assembly.box(label,(x-.047,z-.047,x+.047,z+.047),y+low,y+high,material,role,"station")

    def overhead(label,path,radius=.016):
        wire=[(x,HIGH,z) for x,z in path]
        assembly.raceway(label,wire,radius)
        for (a,b),(c,d) in zip(path,path[1:]):
            count=max(1,math.ceil(math.hypot(c-a,d-b)/1.22))
            for i in range(count):
                t=(i+.5)/count
                x,z=a+(c-a)*t,b+(d-b)*t
                hit,normal,_,_=original.ray_cast(point(x,HIGH+.025,z),Vector((0,0,1)),.10)
                assert hit is not None and abs(hit.z-2.84)<.006 and normal.z<-.98,(label,x,z,hit)
                box(label+" ceiling saddle",x,HIGH,z,-.027,hit.z-HIGH+.003,
                    assembly.mount,"ceiling-backed conduit saddle")
                anchors.append(dict(id=label,contact=world(hit)))

    def sleeve(label,axis,along,start,end,start_wall,end_wall,outside=None):
        axis_point=lambda n,y=HIGH:(n,y,along) if axis=="x" else (along,y,n)
        direction=point(1,0,0) if axis=="x" else point(0,0,1)
        contacts=[]
        for face,sign,model in ((start,1,start_wall),(end,-1,end_wall)):
            actual,_,wall=assembly.measured(point(*axis_point(face-.16*sign)),sign*direction,.19,label)
            assert model in wall.name and abs((actual-point(*axis_point(face))).dot(direction))<.003,(label,wall.name)
            x,y,z=axis_point(face-.031*sign)
            box(label+" capped terminal",x,y,z,-.05,.05,assembly.housing,"wall-backed sealed terminal")
            contacts.append(dict(face=world(actual),receiver=wall.name))
        offset=.06 if outside else .04
        assembly.raceway(label+" closed wall sleeve",[axis_point(start-offset),axis_point(end+offset)],.021,False)
        sleeves.append(dict(id=label,faces=contacts))
        if outside:
            face=end if outside>0 else start
            at=face+outside*.06
            for y in (.30,1.10,1.90,2.62):
                contact,_,wall=assembly.measured(point(*axis_point(face+outside*.17,y)),-outside*direction,.22,label)
                a,b=sorted((face-outside*.004,at+outside*.044))
                rect=(a,along-.044,b,along+.044) if axis=="x" else (along-.044,a,along+.044,b)
                assembly.box(label+" wall saddle",rect,y-.041,y+.041,
                             assembly.mount,"exterior wall-backed riser saddle","station")
                anchors.append(dict(id=label,contact=world(contact),receiver=wall.name))
            assembly.raceway(label,[axis_point(at,HIGH),axis_point(at,BURIED)],.035)

    # Survey the actual grade before laying the hidden runs below it.
    for name,path in TRENCHES:
        for (a,b),(c,d) in zip(path,path[1:]):
            count=math.ceil(math.hypot(c-a,d-b)/.55)
            for i in range(count+1):
                t=i/count
                x,z=a+(c-a)*t,b+(d-b)*t
                hit,normal,_,_=original.ray_cast(point(x,.70,z),Vector((0,0,-1)),1.30)
                assert hit is not None and normal.z>.88 and hit.z-BURIED>.43,(name,x,z,hit)
                soils.append(dict(id=name,contact=world(hit),cover=round(hit.z-BURIED,5)))
        assembly.raceway(name,[(x,BURIED,z) for x,z in path],.042,False)

    # Fit masonry and ceiling hardware before adding adjacent runs.
    drops={}
    for name,owner,location,lamp,_ in ROOMS:
        zone=dict(id=name,owner=owner,y=0,height=3)
        assembly.switch(zone,*location)
        assert assembly.scene.objects[assembly.controllers[-1]["support"]].get("role")=="authored doorway masonry"
        drops[name]=next(route["points"][-1] for route in assembly.routes if route["id"]==f"EM-{owner:02d} switch drop")
        assembly.mounted_lamp(zone,"northwatch-"+name+"-task-light",point(lamp[0],1.8,lamp[1]),
                              Vector((0,0,1)),1.3,point(1,0,0))
        assert assembly.loads[-1]["support"]=="Ceiling cap "+name
    for passage in PARTY+EXTERIOR:
        sleeve(*passage)

    overhead("NW-MAIN lab feeder",[(15,26.285),(15,43.72)],.023)
    overhead("NW-MAIN records feeder",[(32.29,13.15),(43.70,13.15),(43.70,13.8)],.023)
    overhead("NW-MAIN dorm north feeder",[(56.5,26.30),(56.5,28.2),(59,28.2),(59,43.72)],.023)
    feeds=dict(store=[(15,44.28),(15,44.32),(13.05,44.32)],
               dorm=[(56.5,28.2),(46.2,28.2),(46.2,38.9),(44.31,38.9)],
               mess=[(59,44.28),(59,44.32),(54.85,44.32)],
               shelter=[(62.30,11.6),(62.31,11.6),(62.31,12.44)])
    for name,owner,_,lamp,_ in ROOMS:
        dx,dy,dz=drops[name]
        feed=feeds[name]
        overhead(f"NW-{owner:02d} supply to switch",feed)
        x,z=feed[-1]
        assembly.raceway(f"NW-{owner:02d} junction link",[(x,HIGH,z),(dx,dy,dz)],.014)
        assembly.raceway(f"NW-{owner:02d} switched leg riser",[(dx,dy,dz),(dx,HIGH,dz)],.014)
        x,z=lamp
        elbow=(dx,z) if name in {"store","mess"} else (x,dz)
        overhead(f"NW-{owner:02d} switched leg",[(dx,dz),elbow,(x,z)])

    for rows in (assembly.sockets,assembly.controllers,assembly.loads,assembly.routes):
        for row in rows:
            row["id"]=row["id"].replace("college-","northwatch-").replace("EM-","NW-")
            for key in ("circuit","style"):
                if key in row:
                    row[key]=row[key].replace("EM-","NW-")
    return dict(sleeves=sleeves,anchors=anchors,soil_contacts=soils,minimum_cover=min(s["cover"] for s in soils),
                services=assembly.sockets,loads=assembly.loads,controllers=assembly.controllers,raceways=assembly.routes,
                canonical_assets={key:sha(ASSETS/(key+".glb")) for key in ("light_switch_1","level_utility_light")})


if __name__=="__main__":
    build(PARENT,OUT,ROOMS,install,"station-with-surface-power")
