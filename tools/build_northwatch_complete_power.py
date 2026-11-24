"""Commission Northwatch's heated service tunnels, instrument array and court."""
import math
from pathlib import Path
import sys
from mathutils import Vector

sys.path.insert(0,str(Path(__file__).resolve().parent))
from build_northwatch_distribution import BASE, HIGH, build
from build_college_electrical import ASSETS, point, world
from review_roster_macroterrain import sha
from verify_college_services import bvh

PARENT=BASE/"northwatch-surface-power-pass"
OUT=BASE/"northwatch-complete-power-pass/wld-08"
LOW=-.349
ROOMS=(
    ("tunnel",7,(25,27.44,0,-1),None,((22, -2.5,27.35),(22,-2.5,29),(25,-2.5,29),(25,-2.5,27.44))),
    ("tunnel-n",8,(13.55,19.44,0,-1),None,((11,-2.5,23.4),(13.55,-2.5,23.4),(13.55,-2.5,19.44))),
    ("array",10,(45.25,12.76,-1,0),None,((45.08,0,16),(46.2,0,16),(46.2,0,12.76),(45.25,0,12.76))),
    ("court",11,(24.6,27.25,0,-1),None,((26,0,26.85),(26,0,28.4),(24.6,0,28.4),(24.6,0,27.25))),
)
FIXTURES=(
    ("array",10,(45,2.47,13),(-1,0,0),(0,0,1),"records E pier0"),
    ("array",10,(61.3,2.47,10.55),(1,0,0),(0,0,1),"shelter W pier0"),
    ("court",11,(23.4,2.47,27.20),(0,0,-1),(1,0,0),"radio S pier0"),
    ("court",11,(34.7,2.47,27.20),(0,0,-1),(1,0,0),"records S pier0"),
    ("tunnel",7,(25,-1.62,29.4),(0,1,0),(1,0,0),"deck"),
    ("tunnel",7,(32,-1.62,31.2),(0,1,0),(1,0,0),"deck"),
    ("tunnel",7,(38.5,-1.62,31.2),(0,1,0),(1,0,0),"deck"),
    ("tunnel",7,(22,-1.62,24.8),(0,1,0),(0,0,1),"roof underside"),
    ("tunnel-n",8,(5,-1.62,23.6),(0,1,0),(1,0,0),"floor with rectangular well"),
    ("tunnel-n",8,(16.5,-1.62,23),(0,1,0),(1,0,0),"floor with rectangular well"),
)


def install(a):
    original=bvh([o for o in a.scene.objects if o.type=="MESH"])
    anchors,soil,sleeves=[],[],[]

    def ray(start,direction,span):
        p,n,_,_=original.ray_cast(point(*start),point(*direction),span)
        assert p is not None and n.dot(point(*direction))<-.96,(start,direction,p,n)
        return world(p)

    def clip(name,center,support):
        x,y,z=center
        a.box(name, (min(x,support[0])-.032,min(z,support[2])-.032,
                     max(x,support[0])+.032,max(z,support[2])+.032),
              min(y,support[1])-.023,max(y,support[1])+.004,a.mount,"verified conduit saddle","station")
        anchors.append(dict(id=name,contact=support))

    def supported(name,path,direction,expected_offset,max_spacing=1.15,radius=.017):
        a.raceway(name,path,radius)
        for start,end in zip(path,path[1:]):
            length=math.dist(start,end)
            count=max(1,math.ceil(length/max_spacing))
            for i in range(count):
                t=(i+.5)/count
                mid=tuple(p+(q-p)*t for p,q in zip(start,end))
                hit=ray(tuple(p+.023*d for p,d in zip(mid,direction)),direction,.22)
                assert abs(math.dist(mid,hit)-expected_offset)<.024,(name,mid,hit)
                clip(name+" saddle",mid,hit)

    def ceiling(name,path,height,radius=.017):
        supported(name,[(x,height,z) for x,z in path],(0,1,0),.049,radius=radius)

    def closed(name,first,last,direction):
        faces=[]
        for at,sign in ((first,1),(last,-1)):
            vec=tuple(sign*d for d in direction)
            hit=ray(tuple(x-.11*d for x,d in zip(at,vec)),vec,.16)
            assert math.dist(hit,at)<.007,(name,at,hit)
            center=tuple(x-.034*d for x,d in zip(at,vec))
            clip(name+" gland",center,hit)
            faces.append(hit)
        a.raceway(name,[tuple(x-.04*d for x,d in zip(first,direction)),
                        tuple(x+.04*d for x,d in zip(last,direction))],.022,False)
        sleeves.append(dict(id=name,faces=faces))

    def short(name,path,visible=True):
        a.raceway(name,path,.015,visible)

    drops={}
    for room,owner,loc,_,_ in ROOMS:
        zone=dict(id=room,owner=owner,y=-2.5 if owner in (7,8) else 0,height=2.2 if owner in (7,8) else 3)
        a.switch(zone,*loc)
        drops[owner]=next(route["points"][-1] for route in a.routes if route["id"]==f"EM-{owner:02d} switch drop")
    for i,(room,owner,origin,direction,tangent,receiver) in enumerate(FIXTURES,1):
        zone=dict(id=room,owner=owner)
        a.mounted_lamp(zone,f"northwatch-{room}-service-light-{i:02d}",point(*origin),point(*direction),1.46,point(*tangent))
        assert receiver in a.loads[-1]["support"]

    # The exterior drop follows the west shed wall above grade and an enclosed
    # conduit below grade. The basement entry lands against its real west wall.
    ceiling("NW-MAIN west shed feeder",[(15,8.29),(3.0,8.29),(3.0,23.6),(2.28,23.6)],HIGH,.023)
    closed("west shed sleeve",(2.0,HIGH,23.6),(2.24,HIGH,23.6),(1,0,0))
    supported("NW-MAIN outside west shed riser",[(1.95,HIGH,23.6),(1.95,1.10,23.6)],(1,0,0),.05,radius=.023)
    short("NW-MAIN below-grade basement entry",[(1.95,1.10,23.6),(1.86,.83,23.6),
          (1.86,LOW,23.6),(1.88,LOW,23.6)],False)
    closed("west basement sleeve",(1.92,LOW,23.6),(2.08,LOW,23.6),(1,0,0))
    ceiling("NW-MAIN generator tunnel overhead",[(2.12,23.6),(3.0,23.6),(3.0,24.72),(19.72,24.72)],LOW,.022)
    closed("generator branch to elbow sleeve",(19.76,LOW,24.72),(20.24,LOW,24.72),(1,0,0))
    ceiling("NW-MAIN elbow overhead",[(20.28,24.72),(23.50,24.72),(23.50,25.72)],LOW,.022)
    closed("elbow to main tunnel sleeve",(23.50,LOW,25.76),(23.50,LOW,26.24),(0,0,1))
    ceiling("NW-MAIN main tunnel feed",[(23.50,26.28),(23.50,27.15),(25,27.15)],LOW,.022)

    for owner,lead,paths in (
        (8,[(13.55,24.72),(13.55,18.32)],
         [[(13.55,23.6),(5,23.6)],[(13.55,23),(16.5,23)]]),
        (7,[(25,27.15),(25,26.32)],[
         [(25,29.4),(25,31.2),(38.5,31.2)],
         [(25,27.15),(23.52,27.15),(23.52,26.27)],
         [(23.52,25.72),(23.52,24.8),(22,24.8)]]),
    ):
        dx,dy,dz=drops[owner]
        ceiling(f"NW-{owner:02d} switch supply",lead,LOW)
        ex,ez=lead[-1]
        short(f"NW-{owner:02d} switch junction",[(ex,LOW,ez),(dx,dy,dz)])
        # A separate return is physically clipped beside the supply. Its
        # circuit conductor passes the same protected tunnel sleeve.
        short(f"NW-{owner:02d} return rise",[(dx,dy,dz),(dx,LOW,dz)])
        for i,path in enumerate(paths,1):
            if i==1:
                head=(dx,dz)
                ceiling(f"NW-{owner:02d} switched return", [head,(head[0],path[0][1]),*path[1:]],LOW)
            else:
                ceiling(f"NW-{owner:02d} lamp branch {i}",path,LOW)

    # Feed the courtyard from the radio ceiling and carry the switched leg
    # under the adjacent exterior eaves, above the operating door heads.
    ceiling("NW-MAIN courtyard takeoff",[(24,13.15),(24,25.72)],HIGH,.022)
    closed("radio court sleeve",(24,HIGH,25.76),(24,HIGH,26.0),(0,0,1))
    for owner,axis,direction,positions in (
        (11,"z",(0,0,-1),[(23.4,26.055),(34.7,26.055)]),
        (10,"x",(-1,0,0),[(44.055,13.0)]),
    ):
        dx,dy,dz=drops[owner]
        takeoff=(24,HIGH,26.042) if owner==11 else (44.06,HIGH,13.8)
        fx,_,fz=takeoff
        near=(dx,HIGH,fz) if axis=="x" else (fx,HIGH,dz)
        route=[takeoff,near,(dx,HIGH,dz)] if near!=takeoff and near!=(dx,HIGH,dz) else [takeoff,(dx,HIGH,dz)]
        for i,(p,q) in enumerate(zip(route,route[1:])):
            short(f"NW-{owner} source junction {i}",[p,q])
        short(f"NW-{owner} controller link",[(dx,HIGH,dz),(dx,dy,dz)])
        for x,z in positions:
            target=(x,HIGH,z)
            start=(x,HIGH,dz) if axis=="x" else (dx,HIGH,z)
            short(f"NW-{owner} switched takeoff",[(dx,dy,dz),start])
            supported(f"NW-{owner} exterior switched run",[start,target],direction,.055)
            short(f"NW-{owner} exterior lamp drop",[target,(x,2.47,z)])

    # Independent switched outdoor conductor follows the existing service
    # alignment in a second protected underground conduit, below the main.
    grade_path=((44.06,12.47),(44.7,12.47),(44.7,13.38),(61.94,13.38),(61.94,10.55))
    depth=-.79
    for start,end in zip(grade_path,grade_path[1:]):
        count=max(1,math.ceil(math.dist(start,end)/.60))
        for i in range(count+1):
            x,z=(p+(q-p)*i/count for p,q in zip(start,end))
            hit=ray((x,.30,z),(0,-1,0),.6)
            assert hit[1]-depth>.65,hit
            soil.append(dict(contact=hit,centreline=depth))
    short("NW-10 switched underground conductor",[(x,depth,z) for x,z in grade_path],False)
    for x,z,normal in ((44.06,12.47,(-1,0,0)),(61.94,10.55,(1,0,0))):
        y=2.47 if x>60 else drops[10][1]
        supported("NW-10 sealed outdoor riser",[(x,y,z),(x,.10,z)],normal,.06)
        short("NW-10 buried riser shoe",[(x,.10,z),(x,depth,z)],False)
    short("NW-10 outbound switch connection",[(drops[10][0],drops[10][1],drops[10][2]),
          (44.06,drops[10][1],drops[10][2]),(44.06,drops[10][1],12.47)])
    for rows in (a.sockets,a.controllers,a.loads,a.routes):
        for row in rows:
            row["id"]=row["id"].replace("college-","northwatch-").replace("EM-","NW-")
            for key in ("circuit","style"):
                if key in row:
                    row[key]=row[key].replace("EM-","NW-")
    return dict(sleeves=sleeves,anchors=anchors,soil_contacts=soil,
                services=a.sockets,loads=a.loads,controllers=a.controllers,raceways=a.routes,
                canonical_assets={k:sha(ASSETS/(k+".glb")) for k in ("light_switch_1","level_utility_light")})


if __name__=="__main__":
    build(PARENT,OUT,ROOMS,install,"station-with-complete-power")
