"""Feed the dormitory, west and south cloister, and gatehouse independently."""
from pathlib import Path
import sys

sys.path.insert(0,str(Path(__file__).resolve().parent))
from build_college_electrical import point
from build_monastery_east_distribution import BASE, export_stage
from build_monastery_north_distribution import ConduitMounts

PRIOR=BASE/"monastery-north-power-pass"
STAGE=BASE/"monastery-west-power-pass"
STEM="monastery-with-west-distribution"
STATUS="MONASTERY_WEST_CLOISTER_DORMITORY_AND_GATEHOUSE_POWERED_STAGED"
RAIL=2.68
HIGH=2.797
DORM=("Monastery western dormitory partition",)
SOUTH=("Monastery shared gatehouse and south cloister wall",)
NORTH=("Monastery north range doors",)
WEST_END=("Wall south W",)


def install(assembly):
    mounts=ConduitMounts(assembly)
    run=mounts.run

    def bend(label,nodes,radius=.018):
        assembly.raceway(label,nodes,radius)

    def sleeve(label,a,b,direction,receiver):
        for p,sign in ((a,1),(b,-1)):
            aim=tuple(sign*d for d in direction)
            gland=tuple(v-.043*d for v,d in zip(p,aim))
            mounts.attach(label+" seated gland",gland,aim,receiver,radius=.053)
        assembly.raceway(label,[a,b],.021,False)

    def ceiling(label,nodes,zone):
        run(label,nodes,(0,1,0),(f"Ceiling cap {zone}",),.015)

    def rise(label,x,z,low=RAIL):
        bend(label,[(x,low,z),(x,HIGH,z)],.015)

    def fit(name,owner,switch,fixtures):
        room=dict(id=name,owner=owner,y=0,height=3)
        assembly.switch(room,*switch)
        circuit=f"MON-{owner:02d}"
        control=f"monastery-{name}-switch"
        assembly.controllers[-1].update(id=control,circuit=circuit)
        assembly.sockets[-1].update(id=control,style=circuit)
        for index,(x,z) in enumerate(fixtures,1):
            assembly.mounted_lamp(room,f"monastery-{name}-light-{index}",point(x,2.21,z),
                                  point(0,1,0),.94,point(1,0,0))
            assembly.loads[-1]["circuit"]=circuit

    west=((11,25.7),(11,42.1),(13.2,53),(32,53))
    dorm=((-3.1,26),(3.6,29),(-3.1,43),(3.6,46))
    gate=((27.4,59),(36.9,59))
    fit("west-cloister",1,(9.15,37.75,-1,0),west)
    fit("dormitory",5,(6.7,34.25,1,0),dorm)
    fit("gatehouse",0,(29.42,57.25,0,-1),gate)

    run("MON-WEST north wall bus",[(16.22,RAIL,16.163),(8.123,RAIL,16.163)],(0,0,-1),NORTH,.024)
    run("MON-WEST northwest return",[(8.123,RAIL,16.163),(8.123,RAIL,21.85)],(-1,0,0),("Wall north W",),.024)
    bend("MON-WEST surveyed north corner offset",[(8.123,RAIL,21.85),(8.163,RAIL,22.15)],.023)
    run("MON-WEST unswitched west masonry bus",[(8.163,RAIL,22.15),(8.163,RAIL,49.85)],(-1,0,0),DORM,.024)
    bend("MON-WEST surveyed south corner offset",[(8.163,RAIL,49.85),(8.123,RAIL,50.15)],.023)
    run("MON-WEST southwest return",[(8.123,RAIL,50.15),(8.123,RAIL,55.837)],(-1,0,0),WEST_END,.024)

    # West and south cloisters share the same gameplay room owner. One
    # switched return serves both arms alongside the separate main bus.
    run("MON-01 switch feed and return",[(8.163,RAIL,37.75),(8.163,1.30,37.75)],(-1,0,0),DORM)
    rise("MON-01 west ceiling rise",8.163,37.75)
    ceiling("MON-01 west ceiling crossrun",[(8.163,HIGH,37.75),(11,HIGH,37.75)],"west")
    for z in (25.7,42.1):
        ceiling("MON-01 west aisle takeoff",[(11,HIGH,37.75),(11,HIGH,z)],"west")
        bend("MON-01 west fixture termination",[(11,HIGH,z),(11,2.842,z)],.014)
    top=2.763
    bend("MON-01 switched south riser",[(8.163,RAIL,37.75),(8.163,top,37.75)])
    run("MON-01 switched south rail",[(8.163,top,37.75),(8.163,top,49.85)],(-1,0,0),DORM)
    bend("MON-01 supported corner return",[(8.163,top,49.85),(8.123,top,50.15)])
    run("MON-01 southwest ceiling approach",[(8.123,top,50.15),(8.123,top,53)],(-1,0,0),WEST_END)
    rise("MON-01 south ceiling rise",8.123,53,top)
    ceiling("MON-01 south cloister branch",[(8.123,HIGH,53),(13.2,HIGH,53),(32,HIGH,53)],"south")
    for x in (13.2,32):
        bend("MON-01 south fixture termination",[(x,HIGH,53),(x,2.842,53)],.014)

    sleeve("MON-05 sealed dormitory connection",(8.12,RAIL,34.25),(7.88,RAIL,34.25),(-1,0,0),DORM)
    dx=7.837
    run("MON-05 switch feed and return",[(dx,RAIL,34.25),(dx,1.30,34.25)],(1,0,0),DORM)
    rise("MON-05 dormitory ceiling rise",dx,34.25)
    ceiling("MON-05 supported ceiling crossrun",[(dx,HIGH,34.25),(0,HIGH,34.25)],"dorm")
    for z in (26,46):
        ceiling("MON-05 longitudinal branch",[(0,HIGH,34.25),(0,HIGH,z)],"dorm")
    for x,z in dorm:
        ceiling("MON-05 transverse takeoff",[(0,HIGH,z),(x,HIGH,z)],"dorm")
        bend("MON-05 fixture termination",[(x,HIGH,z),(x,2.842,z)],.014)

    run("MON-00 unswitched gate riser feed",[(8.123,RAIL,55.837),(29.42,RAIL,55.837)],(0,0,1),SOUTH,.022)
    sleeve("MON-00 sealed gate lintel connection",(29.42,RAIL,55.88),(29.42,RAIL,56.12),(0,0,1),SOUTH)
    gz=56.163
    run("MON-00 switch feed and return",[(29.42,RAIL,gz),(29.42,1.30,gz)],(0,0,-1),SOUTH)
    rise("MON-00 gate ceiling rise",29.42,gz)
    ceiling("MON-00 gatehouse ceiling trunk",[(29.42,HIGH,gz),(29.42,HIGH,59)],"gatehouse")
    for x,z in gate:
        ceiling("MON-00 gatehouse fixture feed",[(29.42,HIGH,z),(x,HIGH,z)],"gatehouse")
        bend("MON-00 fixture termination",[(x,HIGH,z),(x,2.842,z)],.014)

    for obj in assembly.built:
        if obj.name.startswith("College "):
            obj.name="Monastery west range "+obj.name.removeprefix("College ").replace("college-switch","wall-switch")
    return mounts.fixings


if __name__=="__main__":
    export_stage(PRIOR,STAGE,STEM,STATUS,install,["MON-01","MON-05","MON-00"],Path(__file__))
