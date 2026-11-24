"""Extend the commissioned Monastery supply to the refectory and east cloister."""
import json
import math
from pathlib import Path
import sys

import bpy
from mathutils import Vector

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/"tools"))
from append_static_glb import append
from build_circulation_construction import native_write
from build_college_electrical import ASSETS, Assembly, point, world
from build_college_grounds import import_scene
from build_deliberate_entrances import write
from build_monastery import rel
from build_prison_sally_gate import export
from compose_room_terrain import split_native
from prepare_cemetery_doors import write_tsv
from review_roster_macroterrain import records, sha
from verify_college_services import bvh, match_geometry

BASE=ROOT/"development/levels/rebuild"
PRIOR=BASE/"monastery-power-pass"
STAGE=BASE/"monastery-east-power-pass"
OUT=STAGE/"wld-10"
STEM="monastery-with-east-distribution"
STATUS="MONASTERY_REFECTORY_AND_EAST_CLOISTER_POWERED_STAGED"
REFECTORY=dict(id="refectory",owner=4,y=0,height=3)
CLOISTER=dict(id="east-cloister",owner=2,y=0,height=3)
HEADER=2.76
CEILING=2.797


def install(assembly):
    fixings=[]
    wall_parts=("Monastery tower service elevation","Monastery refectory receiving elevation",
                "Monastery refectory cloister wall","Monastery western tower elevation")
    wall=bvh([o for o in assembly.scene.objects if o.type=="MESH" and o.name.startswith(wall_parts)])
    ceiling=bvh([assembly.scene.objects["Ceiling cap refectory"]])

    def fix(spot,direction,receiver,name,offset):
        start=point(*spot)
        towards=point(*direction)
        tree=ceiling if direction[1] else wall
        at,normal,_,_=tree.ray_cast(start,towards,offset+.048)
        assert at is not None and abs((at-start).length-offset)<.006 and normal.dot(towards)<-.985,(name,spot,at)
        receiver_point=world(at+towards*.007)
        assembly.tube(name+" saddle",spot,receiver_point,.028,assembly.mount)["role"]="structure-backed conduit saddle"
        fixings.append(dict(run=name,point=world(at),receiver=receiver))

    def surface(name,nodes,direction,receiver,radius=.018):
        assembly.raceway(name,nodes,radius)
        for a,b in zip(nodes,nodes[1:]):
            count=max(1,math.ceil(math.dist(a,b)/1.12))
            for i in range(count):
                t=(i+.5)/count
                sample=tuple(u+(v-u)*t for u,v in zip(a,b))
                fix(sample,direction,receiver,name,.043)

    def sleeve(name,a,b,direction,wall_name):
        # The intact masonry remains in collision. The documented sleeve
        # crosses it with two seated face glands and no open hole.
        for side,q in ((1,a),(-1,b)):
            d=tuple(x*side for x in direction)
            gp=tuple(p-.045*n for p,n in zip(q,d))
            fix(gp,d,wall_name,name,.045)
            inner=tuple(p+.009*n for p,n in zip(q,d))
            assembly.tube(name+" seated gland",gp,inner,.052,assembly.housing)["role"]="sealed service sleeve gland"
        assembly.raceway(name,[a,b],.024,False)

    for zone,x,z,dx,dz in ((REFECTORY,62.36,31.18,0,-1),(CLOISTER,48.77,41.58,1,0)):
        assembly.switch(zone,x,z,dx,dz)
        circuit=f"MON-{zone['owner']:02d}"
        name=f"monastery-{zone['id']}-switch"
        assembly.sockets[-1].update(id=name,style=circuit)
        assembly.controllers[-1].update(id=name,circuit=circuit)
    for i,(x,z) in enumerate(((54.1,34.7),(63.8,34.7),(54.1,45.2),(63.8,45.2)),1):
        assembly.mounted_lamp(REFECTORY,f"monastery-refectory-light-{i}",point(x,2.18,z),
                              point(0,1,0),.95,point(1,0,0))
        assembly.loads[-1]["circuit"]="MON-04"
    for i,z in enumerate((27.75,33.85,37.15,46.35),1):
        assembly.mounted_lamp(CLOISTER,f"monastery-east-cloister-light-{i}",point(48.8,2.38,z),
                              point(1,0,0),1.6,point(0,0,1))
        assembly.loads[-1]["circuit"]="MON-02"

    surface("east range feeder at court",[(60.163,HEADER,25.15),(60.163,HEADER,29.837)],
            (-1,0,0),"Tower service elevation",.023)
    surface("east range feeder on receiving wall",[(60.163,HEADER,29.837),(62.36,HEADER,29.837)],
            (0,0,1),"Refectory receiving elevation",.023)
    sleeve("receiving elevation sleeve",(62.36,HEADER,29.88),(62.36,HEADER,30.12),
           (0,0,1),"Refectory receiving elevation")

    surface("MON-04 refectory switch supply",[(62.36,HEADER,30.163),(62.36,1.30,30.163)],
            (0,0,-1),"Refectory interior receiving wall")
    assembly.raceway("MON-04 ceiling transition",[(62.36,HEADER,30.163),(62.36,CEILING,30.163)],.015)
    surface("MON-04 central refectory ceiling trunk",[(62.36,CEILING,30.163),(62.36,CEILING,45.2)],
            (0,1,0),"Refectory ceiling deck")
    for z in (34.7,45.2):
        surface("MON-04 transverse ceiling lighting branch",[(54.1,CEILING,z),(62.36,CEILING,z),(63.8,CEILING,z)],
                (0,1,0),"Refectory ceiling deck",.014)
        for x in (54.1,63.8):
            assembly.raceway("MON-04 fixture takeoff",[(x,CEILING,z),(x,2.845,z)],.014)

    # The cloister branch is taken from the incoming junction before the
    # refectory switch, then carried along the two supported wall faces.
    surface("MON-02 unswitched refectory cross-feed",[(62.36,HEADER,30.163),(50.163,HEADER,30.163)],
            (0,0,-1),"Refectory interior receiving wall",.021)
    surface("MON-02 refectory west wall link",[(50.163,HEADER,30.163),(50.163,HEADER,41.58)],
            (-1,0,0),"Refectory cloister wall",.021)
    sleeve("refectory to cloister sleeve",(50.12,HEADER,41.58),(49.88,HEADER,41.58),
           (-1,0,0),"Refectory cloister wall")
    surface("MON-02 cloister switch supply",[(49.837,HEADER,41.58),(49.837,1.30,41.58)],
            (1,0,0),"Cloister east wall")
    for z in (27.75,46.35):
        surface("MON-02 switched cloister top rail",[(49.837,HEADER,41.58),(49.837,HEADER,z)],
                (1,0,0),"Cloister east masonry",.015)
    for z in (27.75,33.85,37.15,46.35):
        surface("MON-02 cloister lamp takeoff",[(49.837,HEADER,z),(49.837,2.38,z)],
                (1,0,0),"Cloister east masonry",.014)
    for obj in assembly.built:
        if obj.name.startswith("College "):
            obj.name="Monastery east range "+obj.name.removeprefix("College ").replace("college-switch","wall-switch")
    return fixings


def build():
    roster=json.loads((PRIOR/"roster.json").read_bytes())
    row=next(s for s in roster["active_sources"] if s["id"]=="WLD-10")
    prior=PRIOR/"wld-10"
    source=ROOT/row["construction"]
    schedule=("runtime-input.tsv","door-schedule.tsv","handrail-schedule.tsv","valley-schedule.tsv",
              "flashing-schedule.tsv","eave-schedule.tsv","electrical-schedule.tsv")
    authority=json.loads((PRIOR/"checkpoint.json").read_bytes())["project_bytes_sha256"]
    for p in (source,*(ROOT/row[k] for k in ("glb","native","blend","layout")),*(prior/n for n in schedule)):
        assert sha(p)==authority[rel(p)],p
    meta=json.loads(source.read_bytes())
    bpy.ops.wm.open_mainfile(filepath=str(ROOT/row["blend"]))
    bpy.context.preferences.filepaths.save_version=0
    assembly=Assembly()
    fixings=install(assembly)
    OUT.mkdir(parents=True,exist_ok=True)
    blend,model,collision=(OUT/(STEM+ext) for ext in (".blend",".glb",".hcs2"))
    fragment=ROOT/"build/monastery-east-power-addition.glb"
    export(fragment,assembly.built)
    append(ROOT/row["glb"],fragment,model,reuse_embedded_images=True)
    added=native_write(ROOT/row["native"],collision,set(),assembly.built,
                       open_components={obj.name for obj in assembly.built if obj.get("source_asset")})
    before,after=split_native(ROOT/row["native"]),split_native(collision)
    assert after[:len(before)]==before and len(after)-len(before)==len(added)
    bpy.ops.file.pack_all()
    bpy.ops.wm.save_as_mainfile(filepath=str(blend),compress=True)
    objects=import_scene(model)
    for name,faces in added:
        assert match_geometry([face for face,_ in faces],[face for face,_ in records(objects[name])]),name
    for name in schedule[:-1]:
        (OUT/name).write_bytes((prior/name).read_bytes())
    electrical=[line.split("\t") for line in (prior/schedule[-1]).read_text(encoding="utf-8").splitlines()]
    electrical.extend((s["id"],s["type"],s["room"],*s["position"],s["yaw"],s["style"]) for s in assembly.sockets)
    write_tsv(OUT/schedule[-1],electrical)
    assert (OUT/schedule[-1]).read_bytes().startswith((prior/schedule[-1]).read_bytes())
    meta.update(status=STATUS,before_construction=rel(source),before_construction_sha256=sha(source),
                before_layout=row["layout"],before_layout_sha256=sha(ROOT/row["layout"]),
                source_glb=row["glb"],source_glb_sha256=row["glb_sha256"],
                source_physics=row["native"],source_physics_sha256=sha(ROOT/row["native"]),
                glb_sha256=sha(model),physics_sha256=sha(collision),blend_sha256=sha(blend),
                builder=rel(Path(__file__)),builder_sha256=sha(Path(__file__)),
                retained_native_records=len(before),removed_source_meshes=[],new_interface_parts=[n for n,_ in added],
                fixings=fixings,loads=meta["loads"]+assembly.loads,controllers=meta["controllers"]+assembly.controllers,
                services=meta["services"]+assembly.sockets,raceways=meta["raceways"]+assembly.routes,
                added_circuits=["MON-04","MON-02"],fixture_source_sha256=sha(ASSETS/"level_utility_light.glb"))
    construction=OUT/"construction.json"
    write(construction,meta)
    row.update(construction=rel(construction),glb=rel(model),native=rel(collision),blend=rel(blend),
               glb_sha256=sha(model),native_sha256=sha(collision))
    next(s for s in roster["sites"] if s["id"]=="WLD-10")["decision"]=STATUS
    roster["baseline"]=rel(PRIOR/"roster.json")
    write(STAGE/"roster.json",roster)
    print("PASS",len(fixings),"measured fixings,",len(added),"matched components,",len(assembly.loads),"new lights")


if __name__=="__main__":
    build()
