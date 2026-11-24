"""Feed the archive and vestry from the unswitched Monastery distribution."""
import math
from pathlib import Path
import sys

sys.path.insert(0,str(Path(__file__).resolve().parent))
from build_college_electrical import point, world
from build_monastery_east_distribution import BASE, export_stage
from verify_college_services import bvh

PRIOR=BASE/"monastery-east-power-pass"
STAGE=BASE/"monastery-north-power-pass"
STEM="monastery-with-north-distribution"
STATUS="MONASTERY_ARCHIVE_AND_VESTRY_POWERED_STAGED"
RAIL=2.68
HIGH=2.797
ROOMS=(
    ("archive",3,16.22,((12.2,7.0),(18.3,5.65),(24.4,11.5))),
    ("vestry",10,34.42,((31.2,8),(36.4,6),(41.4,11))),
)


def install(assembly):
    receivers = {}
    fixings = []

    def attach(label, p, direction, receiver, offset=.043, radius=.027):
        if receiver not in receivers:
            receivers[receiver]=bvh([o for o in assembly.scene.objects if o.type=="MESH"
                and any(o.name.startswith(prefix) for prefix in receiver)])
        origin,normal=point(*p),point(*direction)
        at,n,_,_=receivers[receiver].ray_cast(origin,normal,offset+.04)
        assert at is not None and abs((at-origin).length-offset)<.005 and n.dot(normal)<-.985,(label,p,at)
        assembly.tube(label+" seated saddle",p,world(at+normal*.007),radius,assembly.mount)["role"]="measured masonry or ceiling fixing"
        fixings.append(dict(run=label,point=world(at),receiver=" / ".join(receiver)))

    def run(label,nodes,direction,receiver,radius=.017):
        assembly.raceway(label,nodes,radius)
        for a,b in zip(nodes,nodes[1:]):
            count=max(1,math.ceil(math.dist(a,b)/1.14))
            for i in range(count):
                t=(i+.5)/count
                p=tuple(x+(y-x)*t for x,y in zip(a,b))
                attach(label,p,direction,receiver,radius=radius+.011)

    east=("Monastery western tower elevation","Monastery refectory cloister wall")
    north=("Monastery north range doors",)
    # This starts on the incoming side of the east-cloister switch. A
    # separately clipped rail keeps the northern trunk isolated from MON-02.
    run("MON-NORTH unswitched east wall feed",
        [(49.837,2.76,41.58),(49.837,RAIL,41.58),(49.837,RAIL,16.163)],
        (1,0,0),east,.024)
    run("MON-NORTH north range manifold",
        [(49.837,RAIL,16.163),(34.42,RAIL,16.163),(16.22,RAIL,16.163)],
        (0,0,-1),north,.024)

    for name,owner,x,lamps in ROOMS:
        circuit=f"MON-{owner:02d}"
        room=dict(id=name,owner=owner,y=0,height=3)
        ceiling=(f"Ceiling cap {name}",)
        assembly.switch(room,x,14.78,0,1)
        control=f"monastery-{name}-switch"
        assembly.controllers[-1].update(id=control,circuit=circuit)
        assembly.sockets[-1].update(id=control,style=circuit)
        for index,(px,pz) in enumerate(lamps,1):
            assembly.mounted_lamp(room,f"monastery-{name}-light-{index}",point(px,2.22,pz),
                                  point(0,1,0),.91,point(1,0,0))
            assembly.loads[-1]["circuit"]=circuit

        for z,d in ((16.12,-1),(15.88,1)):
            mouth=(x,RAIL,z-.043*d)
            attach(circuit+" masonry gland",mouth,(0,0,d),north,radius=.053)
        assembly.raceway(circuit+" sealed wall sleeve",
                         [(x,RAIL,16.12),(x,RAIL,15.88)],.022,False)
        run(circuit+" switch feed and return",[(x,RAIL,15.837),(x,1.303,15.837)],
            (0,0,1),north)
        assembly.raceway(circuit+" ceiling riser",[(x,RAIL,15.837),(x,HIGH,15.837)],.016)
        low=min(z for _,z in lamps)
        run(circuit+" supported ceiling trunk",[(x,HIGH,15.837),(x,HIGH,low)],(0,1,0),ceiling)
        for px,pz in lamps:
            run(circuit+" supported fixture branch",[(x,HIGH,pz),(px,HIGH,pz)],(0,1,0),ceiling,.014)
            assembly.raceway(circuit+" fixture termination",[(px,HIGH,pz),(px,2.842,pz)],.014)

    for obj in assembly.built:
        if obj.name.startswith("College "):
            obj.name="Monastery north range "+obj.name.removeprefix("College ").replace("college-switch","wall-switch")
    return fixings


if __name__=="__main__":
    export_stage(PRIOR,STAGE,STEM,STATUS,install,["MON-03","MON-10"],Path(__file__))
