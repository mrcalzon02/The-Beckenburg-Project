"""Supply the Monastery bell gallery, catacomb passage, and chapter chamber."""
from pathlib import Path
import sys

sys.path.insert(0,str(Path(__file__).resolve().parent))
from build_college_electrical import point
from build_monastery_east_distribution import BASE, export_stage
from build_monastery_north_distribution import ConduitMounts

PRIOR=BASE/"monastery-west-power-pass"
STAGE=BASE/"monastery-vertical-power-pass"
STEM="monastery-with-vertical-distribution"
STATUS="MONASTERY_ALL_INDOOR_CIRCUITS_STAGED"
TOWER=("Monastery tower service elevation",)
BELL=("Wall bell E",)
TOWER_DECK=("Monastery bell floor around upper stair well",)
NORTH=("Monastery north range doors",)
CRYPT_WALL=("Monastery memorial chamber partition",)
VAULT=("Monastery north cloister continuous vaulted deck",)


def install(assembly):
    mounts=ConduitMounts(assembly)
    run=mounts.run

    def bend(label,nodes,radius=.016):
        assembly.raceway(label,nodes,radius)

    def sleeve(label,a,b,direction,receiver):
        for p,sign in ((a,1),(b,-1)):
            aim=tuple(sign*d for d in direction)
            gland=tuple(v-.043*d for v,d in zip(p,aim))
            mounts.attach(label+" flange",gland,aim,receiver,radius=.051)
        assembly.raceway(label,[a,b],.022,False)

    def fixtures(name,owner,level,switch,lamps,deck):
        room=dict(id=name,owner=owner,y=level,height=3)
        assembly.switch(room,*switch)
        circuit=f"MON-{owner:02d}"
        key=f"monastery-{name}-switch"
        assembly.controllers[-1].update(id=key,circuit=circuit)
        assembly.sockets[-1].update(id=key,style=circuit)
        for i,(x,z) in enumerate(lamps,1):
            assembly.mounted_lamp(room,f"monastery-{name}-light-{i}",point(x,deck-.44,z),
                                  point(0,1,0),.62,point(1,0,0))
            assembly.loads[-1]["circuit"]=circuit

    bell=((52.6,20.55),(58.91,24.61),(53.3,25.8))
    passage=((20.8,20.75),(23.1,17.32),(35.25,20.65))
    chamber=((14.8,8.4),(21.9,10.25))
    fixtures("bell-gallery",7,6,(58.78,26.8,1,0),bell,8.84)
    fixtures("catacomb-passage",8,-3,(24.65,17.38,0,-1),passage,-.23)
    fixtures("memorial-chamber",9,-3,(20.25,14.76,0,1),chamber,-.16)

    # The tower return starts at the incoming junction that feeds the tower
    # base switch. The sealed sleeve sits east of the structural stair well.
    run("MON-07 unswitched tower wall feeder",
        [(59.837,2.76,25.15),(59.837,2.76,26.8),(59.837,5.717,26.8)],(1,0,0),TOWER,.025)
    sleeve("MON-07 sealed bell deck sleeve",(59.837,5.76,26.8),(59.837,6,26.8),(0,1,0),TOWER_DECK)
    bend("MON-07 measured upper wall offset",[(59.837,6.043,26.8),(59.877,6.19,26.8)])
    run("MON-07 gallery switch feed and return",[(59.877,6.19,26.8),(59.877,8.797,26.8)],(1,0,0),BELL)
    run("MON-07 gallery ceiling backbone",[(59.877,8.797,26.8),(55.85,8.797,26.8),(55.85,8.797,20.55)],
        (0,1,0),("Ceiling cap bell",),.019)
    for x,z in bell:
        run("MON-07 gallery fixture branch",[(55.85,8.797,z),(x,8.797,z)],
            (0,1,0),("Ceiling cap bell",),.014)
        bend("MON-07 mounted fixture termination",[(x,8.797,z),(x,8.842,z)],.014)

    # The upper riser stays on the north range pier. Its lower exit is
    # beneath the solid part of the vault cover, between the two stair bays.
    run("MON-CRYPT unswitched north pier drop",[(23.15,2.68,16.163),(23.15,.043,16.163)],
        (0,0,-1),NORTH,.024)
    sleeve("MON-CRYPT sealed vault deck sleeve",(23.15,0,16.163),(23.15,-.23,16.163),(0,-1,0),VAULT)
    bend("MON-CRYPT vault face landing",[(23.15,-.273,16.163),(23.15,-.355,16.163)],.019)
    run("MON-08 passage switch feed and return",[(23.15,-.355,16.163),(24.65,-.355,16.163),
        (24.65,-1.70,16.163)],(0,0,-1),CRYPT_WALL)
    bend("MON-08 ceiling rise",[(24.65,-.355,16.163),(24.65,-.273,16.163)])
    run("MON-08 central passage branch",[(24.65,-.273,16.163),(24.65,-.273,20.75)],
        (0,1,0),VAULT,.018)
    for x,z in passage:
        run("MON-08 passage ceiling takeoff",[(24.65,-.273,z),(x,-.273,z)],(0,1,0),VAULT,.014)
        bend("MON-08 seated fixture termination",[(x,-.273,z),(x,-.227,z)],.014)

    # The memorial supply leaves the vault feeder before the passage switch,
    # then crosses a solid memorial pier beside the existing door.
    run("MON-09 unswitched chamber wall feed",[(23.15,-.355,16.163),(20.25,-.355,16.163)],
        (0,0,-1),CRYPT_WALL,.021)
    sleeve("MON-09 sealed memorial pier sleeve",(20.25,-.355,16.12),(20.25,-.355,15.88),
           (0,0,-1),CRYPT_WALL)
    run("MON-09 memorial switch feed and return",[(20.25,-.355,15.837),(20.25,-1.70,15.837)],
        (0,0,1),CRYPT_WALL)
    bend("MON-09 overhead transition",[(20.25,-.355,15.837),(20.25,-.203,15.837)])
    run("MON-09 memorial ceiling trunk",[(20.25,-.203,15.837),(20.25,-.203,8.4)],
        (0,1,0),("Floor archive",),.018)
    for x,z in chamber:
        run("MON-09 memorial ceiling takeoff",[(20.25,-.203,z),(x,-.203,z)],
            (0,1,0),("Floor archive",),.015)
        bend("MON-09 seated fixture termination",[(x,-.203,z),(x,-.157,z)],.014)

    for obj in assembly.built:
        if obj.name.startswith("College "):
            obj.name="Monastery vertical "+obj.name.removeprefix("College ").replace("college-switch","wall-switch")
    return mounts.fixings


if __name__=="__main__":
    export_stage(PRIOR,STAGE,STEM,STATUS,install,["MON-07","MON-08","MON-09"],Path(__file__))
