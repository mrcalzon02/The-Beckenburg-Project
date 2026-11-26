"""Install the Monastery's service-court generator and two switched circuits."""
import json
import math
from pathlib import Path
import sys

import bpy
from mathutils import Matrix, Vector

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools"))
from append_static_glb import append
from build_circulation_construction import native_write
from build_college_electrical import ASSETS, Assembly, bounds, point, world
from build_college_grounds import import_scene
from build_deliberate_entrances import write
from build_monastery import rel
from build_prison_sally_gate import export
from build_roster_enclosed_roofs import slab
from build_roster_foundations import material
from compose_room_terrain import split_native
from prepare_cemetery_doors import write_tsv
from review_roster_macroterrain import records, sha
from verify_college_services import bvh, match_geometry

BASE = ROOT / "development/levels/rebuild"
PRIOR = BASE / "monastery-eave-pass"
STAGE = BASE / "monastery-power-pass"
OUT = STAGE / "wld-10"
STEM = "monastery-with-service-power"
STATUS = "MONASTERY_FOUNDED_SERVICE_SOURCE_AND_TWO_SWITCHED_CIRCUITS_STAGED"
OUTER, INNER = 60.12, 59.88
COURT = dict(id="service-court",owner=11,y=0,height=3)
TOWER = dict(id="tower-base",owner=6,y=0,height=6)


def install(assembly):
    supports, equipment = [], {}
    wall = bvh([obj for obj in assembly.scene.objects
                if obj.type=="MESH" and obj.name.startswith("Monastery tower service elevation")])

    def mount(face,y,z,label):
        direction = 1 if face==INNER else -1
        at,n,_,_ = wall.ray_cast(point(face-.30*direction,y,z),point(direction,0,0),.41)
        assert at is not None and abs(at.x-face)<.002 and n.x*direction<-.98,(label,y,z,at)
        supports.append(dict(id=label,position=world(at),receiver="Tower service elevation"))
        return world(at)

    def stand(name,rect,top):
        x0,z0,x1,z1 = rect
        for x in (x0,(x0+x1)/2,x1):
            for z in (z0,(z0+z1)/2,z1):
                p,_,part = assembly.measured(point(x,.38,z),Vector((0,0,-1)),.6,name)
                assert part.name=="WLD-10 saved ground" and abs(p.z)<.045,(name,x,z,p,part.name)
                supports.append(dict(id=name,position=world(p),receiver=part.name))
        assembly.box(name,rect,-.13,top,assembly.concrete,"founded electrical equipment plinth","service-court")

    def place(key,name,origin,angle):
        frame = Matrix.Translation(point(*origin)) @ Matrix.Rotation(angle,4,"Z")
        objects = assembly.place(key,frame,name,"supported electrical equipment","service-court")
        equipment[name] = dict(asset=key,origin=origin,rotation=angle,bounds=bounds(objects),
                               source=rel(ASSETS/(key+".glb")),sha256=sha(ASSETS/(key+".glb")))
        return bvh(objects)

    def casing(tree,y,z,from_east):
        direction = -1 if from_east else 1
        x = 62 if from_east else 60.13
        p,n,_,_ = tree.ray_cast(point(x,y,z),point(direction,0,0),2.1)
        assert p is not None and n.x*direction<-.93,(y,z,p,n)
        return p.x

    stand("generator plinth",(60.19,17.22,61.22,18.88),.105)
    stand("main board plinth",(60.125,19.05,60.68,21.48),.074)
    generator=place("level_trace_generator_refined","service generator",(60.695,.105,18.05),-math.pi/2)
    board=place("level_electrical_bank_worn","main distribution board",(60.31,.074,20.25),math.pi/2)

    for y in (.57,1.56):
        for z in (19.28,20.25,21.23):
            mount(OUTER,y,z,"board rear rail")
        assembly.box("distribution board anchored backrail",(60.111,19.22,60.27,21.28),y-.04,y+.04,
                     assembly.mount,"anchors into service masonry","service-court")

    x = casing(generator,.64,18.05,True)
    assembly.box("generator starter backing plate",(x-.016,17.97,x+.055,18.13),.605,.845,
                 assembly.housing,"seated generator starter","service-court")
    assembly.effect("monastery-generator","generator",11,(x+.057,.68,18.05),math.pi/2,"MON-MAIN")
    x = casing(board,.91,20.25,True)
    assembly.effect("monastery-breaker","breaker",11,(x+.022,.91,20.25),math.pi/2,"MON-MAIN")

    # The canopy falls away from masonry into the service court. Three
    # compression brackets carry the short projection above the equipment.
    skin=material("Monastery weathered lead service canopy",(.25,.29,.30))
    canopy=slab("Monastery weather hood",[(60.11,2.68,17.03),(61.63,2.52,17.03),
                    (61.63,2.52,21.65),(60.11,2.68,21.65)],.105,skin)
    canopy["role"]="supported sloping electrical weather hood"
    assembly.built.append(canopy)
    for z in (17.20,19.34,21.47):
        mount(OUTER,2.14,z,"weather hood diagonal wall plate")
        mount(OUTER,2.54,z,"weather hood upper rail")
        assembly.tube("canopy masonry diagonal",(60.111,2.14,z),(61.51,2.431,z),.037,assembly.mount)
        assembly.tube("canopy longitudinal bearing arm",(60.11,2.59,z),(61.54,2.433,z),.032,assembly.mount)

    for zone,x,z,dx in ((COURT,61.19,24.63,-1),(TOWER,58.87,24.61,1)):
        assembly.switch(zone,x,z,dx,0)
        circuit=f"MON-{zone['owner']:02d}"
        switch=f"monastery-{zone['id']}-switch"
        assembly.sockets[-1].update(id=switch,style=circuit)
        assembly.controllers[-1].update(id=switch,circuit=circuit)
        for serial,station in enumerate((25.84,28.07) if dx<0 else (20.1,25.9),1):
            assembly.mounted_lamp(zone,f"monastery-{zone['id']}-light-{serial}",point(x,2.39,station),
                                  point(dx,0,0),1.6,point(0,0,1))
            assembly.loads[-1]["circuit"]=circuit

    def race(name,face,nodes,r=.018):
        side=1 if face==OUTER else -1
        x=face+side*.043
        path=[(x,y,z) for y,z in nodes]
        assembly.raceway(name,path,r)
        for (y,z),(ey,ez) in zip(nodes,nodes[1:]):
            count=max(1,math.ceil(math.hypot(ey-y,ez-z)/1.05))
            for i in range(count):
                t=(i+.5)/count
                sy,sz=y+t*(ey-y),z+t*(ez-z)
                mount(face,sy,sz,name)
                assembly.tube(name+" wall clamp",(face-side*.008,sy,sz),(x,sy,sz),r+.012,assembly.mount)
        return x

    feed_y,feed_z=.53,18.49
    pos=casing(generator,feed_y,feed_z,False)
    assembly.raceway("sealed generator output gland",[(pos+.009,feed_y,feed_z),(OUTER+.043,feed_y,feed_z)],.024)
    race("MON-MAIN armored feeder",OUTER,[(feed_y,feed_z),(2.29,feed_z),(2.29,20.25)])
    bx=60.31-.04
    for z in (19.3522,20.1122,20.8722):
        terminal,n,_,_=board.ray_cast(point(bx,2.7,z),Vector((0,0,-1)),1)
        assert terminal is not None and 2.17<terminal.z<2.24 and n.z>.35,(z,terminal,n)
        tz=-terminal.y
        assembly.raceway("distribution terminal into wall bus",[(bx,terminal.z-.004,tz),(bx,2.29,tz),
                            (OUTER+.043,2.29,tz)],.017)
        supports.append(dict(id="distribution terminal",position=world(terminal),receiver="Main distribution board"))
    race("MON-MAIN above service door",OUTER,[(2.29,20.25),(2.76,20.25),(2.76,25.15)],.023)
    for face in (OUTER,INNER):
        mount(face,2.76,25.15,"sealed service sleeve flange")
        side=1 if face==OUTER else -1
        assembly.tube("sealed sleeve gland",(face-side*.009,2.76,25.15),(face+side*.064,2.76,25.15),.052,assembly.housing)
    assembly.raceway("enclosed tower masonry electrical sleeve",[(INNER,2.76,25.15),(OUTER,2.76,25.15)],.022,False)
    for zone,face,station in ((COURT,OUTER,24.63),(TOWER,INNER,24.61)):
        circuit=f"MON-{zone['owner']:02d}"
        race(circuit+" control feed",face,[(2.76,25.15),(2.76,station),(1.29,station)])
        for load in (x for x in assembly.loads if x["room"]==zone["owner"]):
            _,y,z=load["mount"]
            race(circuit+" switched light return",face,[(2.76,station),(2.76,z),(y,z)],.014)

    for obj in assembly.built:
        if obj.name.startswith("College "):
            obj.name="Monastery "+obj.name.removeprefix("College ").replace("college-switch","monastery-switch")
    return equipment,supports


def build():
    roster=json.loads((PRIOR/"roster.json").read_bytes())
    row=next(s for s in roster["active_sources"] if s["id"]=="WLD-10")
    prior=PRIOR/"wld-10"
    source=ROOT/row["construction"]
    schedule=("runtime-input.tsv","door-schedule.tsv","handrail-schedule.tsv","valley-schedule.tsv",
              "flashing-schedule.tsv","eave-schedule.tsv")
    authority=json.loads((PRIOR/"checkpoint.json").read_bytes())["project_bytes_sha256"]
    for p in (source,*(ROOT/row[k] for k in ("glb","native","blend","layout")),*(prior/n for n in schedule)):
        assert sha(p)==authority[rel(p)],p
    meta=json.loads(source.read_bytes())
    bpy.ops.wm.open_mainfile(filepath=str(ROOT/row["blend"]))
    bpy.context.preferences.filepaths.save_version=0
    assembly=Assembly()
    equipment,supports=install(assembly)
    OUT.mkdir(parents=True,exist_ok=True)
    blend,model,collision=(OUT/(STEM+ext) for ext in (".blend",".glb",".hcs2"))
    fragment=ROOT/"build/monastery-power-addition.glb"
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
    for name in schedule:
        (OUT/name).write_bytes((prior/name).read_bytes())
    write_tsv(OUT/"electrical-schedule.tsv",[(s["id"],s["type"],s["room"],*s["position"],s["yaw"],s["style"])
                                            for s in assembly.sockets])
    plan=ROOT/"development/levels/power-plans/wld-10.json"
    meta.update(status=STATUS,before_construction=rel(source),before_construction_sha256=sha(source),
                before_layout=row["layout"],before_layout_sha256=sha(ROOT/row["layout"]),
                source_glb=row["glb"],source_glb_sha256=row["glb_sha256"],
                source_physics=row["native"],source_physics_sha256=sha(ROOT/row["native"]),
                glb_sha256=sha(model),physics_sha256=sha(collision),blend_sha256=sha(blend),
                builder=rel(Path(__file__)),builder_sha256=sha(Path(__file__)),power_plan=rel(plan),
                retained_native_records=len(before),removed_source_meshes=[],new_interface_parts=[n for n,_ in added],
                equipment=equipment,electrical_bearings=supports,loads=assembly.loads,
                controllers=assembly.controllers,services=assembly.sockets,raceways=assembly.routes)
    construction=OUT/"construction.json"
    write(construction,meta)
    row.update(construction=rel(construction),glb=rel(model),native=rel(collision),blend=rel(blend),
               glb_sha256=sha(model),native_sha256=sha(collision))
    next(s for s in roster["sites"] if s["id"]=="WLD-10")["decision"]=STATUS
    roster["baseline"]=rel(PRIOR/"roster.json")
    write(STAGE/"roster.json",roster)
    print("PASS",len(supports),"mounts and footing checks,",len(added),"matched additions,",len(assembly.loads),"loads")


if __name__=="__main__":
    build()
