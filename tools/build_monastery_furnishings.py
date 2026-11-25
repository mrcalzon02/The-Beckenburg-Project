"""Fit the ground-floor furnishings and supported readable Monastery records."""
import json
import math
from pathlib import Path
import sys

import bpy

ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/"tools"))
from append_static_glb import append
from build_circulation_construction import native_write
from build_college_electrical import point, world
from build_college_grounds import import_scene
from build_deliberate_entrances import write
from build_mine_case import Case, DOWN
from build_monastery import rel
from build_prison_sally_gate import export
from compose_room_terrain import split_native
from prepare_cemetery_doors import write_tsv
from review_roster_macroterrain import records, sha
from verify_college_services import bvh, match_geometry

BASE=ROOT/"development/levels/rebuild"
POWER=BASE/"monastery-vertical-power-pass"
PRIOR=BASE/"monastery-named-pass"
STAGE=BASE/"monastery-furnishing-pass"
OUT=STAGE/"wld-10"
STEM="monastery-with-furnishings"
STATUS="MONASTERY_FIVE_GROUND_FLOOR_ROOMS_FURNISHED_WITH_READABLE_RECORDS"
PIECES=(
    ("gatehouse","gate storage shelf","shelf_mp_6",25.1,58.1,math.pi/2),
    ("gatehouse","gate supply crate","wooden_crate_1",26.3,61.06,0),
    ("gatehouse","gate waiting bench","level_blockout_castle_long_bench",38.7,59.25,-math.pi/2),
    ("archive","working folio shelf","shelf_mp_6",8.78,6.5,math.pi/2),
    ("archive","reference folio shelf","shelf_mp_6",8.78,11.6,math.pi/2),
    ("archive","archive reading table","dinner_table_1",19.74,12.2,0),
    ("refectory","north refectory table","dinner_table_1",56.15,34.6,0),
    ("refectory","north wall dining bench","level_blockout_castle_long_bench",56.15,33.42,math.pi),
    ("refectory","north aisle dining bench","level_blockout_castle_long_bench",56.15,35.82,0),
    ("refectory","south refectory table","dinner_table_1",62.84,46.48,0),
    ("refectory","south aisle dining bench","level_blockout_castle_long_bench",62.84,45.25,math.pi),
    ("refectory","south wall dining bench","level_blockout_castle_long_bench",62.84,47.70,0),
    ("dorm","northwest dorm bed","level_trace_camp_bed",-3.35,28.25,0),
    ("dorm","northeast dorm bed","level_trace_camp_bed",3.65,28.25,0),
    ("dorm","southwest dorm bed","level_trace_camp_bed",-3.35,44,0),
    ("dorm","southeast dorm bed","level_trace_camp_bed",3.65,44,0),
    ("dorm","northwest footlocker","level_work_crew_footlocker",-3.35,30,0),
    ("dorm","northeast footlocker","level_work_crew_footlocker",3.65,30,0),
    ("dorm","southwest footlocker","level_work_crew_footlocker",-3.35,45.75,0),
    ("dorm","southeast footlocker","level_work_crew_footlocker",3.65,45.75,0),
    ("vestry","vestry linen shelf","shelf_mp_6",31.2,4.08,0),
    ("vestry","vestry wash trough","level_trace_wash_trough",38.53,4.14,0),
    ("vestry","vestry waiting bench","level_blockout_castle_long_bench",41.7,12.45,-math.pi/2),
)
STORY=(
    ("ASHEN_BELLS_REGISTER","archive",3,"archive reading table","level_trace_journal",(19.70,12.17),(19.75,0,13.73)),
    ("REFECTORY_RATION_SHEET","refectory",4,"north refectory table","level_story_letters",(56.03,34.64),(54.88,0,34.64)),
)
OBJECTS=(
    ("archive","spare reading folio","archive reading table","book_mp_2",20.19,12.26,.9),
    ("refectory","north unused plate","north refectory table","plate_mp_1",56.68,34.79,.85),
    ("refectory","south plate at first setting","south refectory table","plate_mp_1",62.25,46.55,.9),
    ("refectory","south plate at second setting","south refectory table","plate_mp_1",63.35,46.42,.9),
)


def install(case, rows):
    existing=[o for o in case.scene.objects if o.type=="MESH"]
    obstacles=[o for o in existing if "floor" not in o.get("role","").lower() and "terrain" not in o.get("role","").lower()]
    tracks={}
    for line in rows:
        if line[0]=="POINT":
            tracks.setdefault(line[1],[]).append(tuple(float(v) for v in line[2:5]))
    raster=[]
    for ident,points in tracks.items():
        for a,b in zip(points,points[1:]):
            count=max(1,math.ceil(math.dist(a,b)/.10))
            for i in range(count+1):
                raster.append((ident,tuple(x+(y-x)*i/count for x,y in zip(a,b))))
    supported={}
    fitted=[]
    for room,label,asset,x,z,yaw in PIECES:
        parts,receipt=case.fit(asset,label,room,x,0,z,yaw=yaw)
        tree=bvh(parts)
        hits=bvh(obstacles).overlap(tree)
        assert not hits,(label,[o.name for o in obstacles if bvh([o]).overlap(tree)])
        (x0,y0,z0),(x1,y1,z1)=receipt["bounds"]
        margin,which=min((math.hypot(max(x0-a,0,a-x1),max(z0-c,0,c-z1)),name)
                         for name,(a,b,c) in raster if y0-1.72<=b<=y1+.03)
        assert margin>.46,(label,which,margin)
        supported[label]=parts
        fitted.append(dict(room=room,name=label,route=which,route_clearance=round(margin,4),**receipt))
        obstacles.extend(parts)

    def on_top(room,label,parent,asset,x,z,scale):
        table=supported[parent]
        at,n,_,_=bvh(table).ray_cast(point(x,1.8,z),DOWN,1.72)
        assert at is not None and n.z>.99,(label,at,n)
        parts,receipt=case.fit(asset,label,room,x,at.z+.0007,z,scale=scale,support=table)
        forbidden=[o for o in obstacles if o not in table]
        assert not bvh(forbidden).overlap(bvh(parts)),label
        obstacles.extend(parts)
        return parts,dict(room=room,name=label,desktop=at.z,**receipt)

    clutter=[]
    for room,label,parent,asset,x,z,scale in OBJECTS:
        _,receipt=on_top(room,label,parent,asset,x,z,scale)
        clutter.append(receipt)
    documents=[]
    for ident,room,owner,parent,asset,(x,z),feet in STORY:
        label=ident.lower().replace("_"," ")
        paper,receipt=on_top(room,label,parent,asset,x,z,.64)
        paper_tree=bvh(paper)
        samples=[paper_tree.ray_cast(point(x+dx,receipt["desktop"]+.23,z+dz),DOWN,.26)[0]
                 for dx,dz in ((.027,.024),(-.034,.021),(.06,-.04),(-.06,-.04),(0,0))]
        at=next((hit for hit in samples if hit is not None),None)
        assert at is not None,ident
        eye=point(feet[0],feet[1]+1.63,feet[2])
        ray=at-eye
        trees=[o for o in case.scene.objects if o.type=="MESH" and o not in paper]
        assert bvh(trees).ray_cast(eye,ray.normalized(),ray.length-.005)[0] is None,ident
        aim=world(at)
        assert len(paper)==1,ident
        ident_socket="Monastery furnished "+paper[0].name.removeprefix("Mine case ")
        rows.append(("STORY",ident_socket,ident,owner,asset,*aim,receipt["yaw"],*feet))
        documents.append(dict(id=ident,owner=owner,name=label,room=room,socket=ident_socket,
                              stance=feet,aim=aim,placement=receipt))
        for part in paper:
            part["role"]="desktop-supported readable record"
    for obj in case.built:
        obj.name="Monastery furnished "+obj.name.removeprefix("Mine case ")
        if obj["role"]!="desktop-supported readable record":
            obj["role"]="supported ground-floor furnishing or tabletop object"
    return fitted,clutter,documents


def build():
    roster=json.loads((PRIOR/"roster.json").read_bytes())
    row=next(s for s in roster["active_sources"] if s["id"]=="WLD-10")
    source=ROOT/row["construction"]
    prior=POWER/"wld-10"
    authority=json.loads((POWER/"checkpoint.json").read_bytes())["project_bytes_sha256"]
    names=("runtime-input.tsv","door-schedule.tsv","electrical-schedule.tsv","handrail-schedule.tsv",
           "valley-schedule.tsv","flashing-schedule.tsv","eave-schedule.tsv")
    for path in (source,*(ROOT/row[k] for k in ("blend","glb","native","layout")),*(prior/n for n in names)):
        assert sha(path)==authority[rel(path)],path
    meta=json.loads(source.read_bytes())
    rows=[line.split("\t") for line in (prior/names[0]).read_text(encoding="utf-8").splitlines()]
    bpy.ops.wm.open_mainfile(filepath=str(ROOT/row["blend"]))
    bpy.context.preferences.filepaths.save_version=0
    case=Case()
    fitted,clutter,documents=install(case,rows)

    OUT.mkdir(parents=True,exist_ok=True)
    blend,model,collision=(OUT/(STEM+ext) for ext in (".blend",".glb",".hcs2"))
    fragment=ROOT/"build/monastery-furnishing-addition.glb"
    export(fragment,case.built)
    append(ROOT/row["glb"],fragment,model,reuse_embedded_images=True)
    added=native_write(ROOT/row["native"],collision,set(),case.built,open_components={obj.name for obj in case.built})
    before,after=split_native(ROOT/row["native"]),split_native(collision)
    assert after[:len(before)]==before and len(after)-len(before)==len(added)
    bpy.ops.file.pack_all()
    bpy.ops.wm.save_as_mainfile(filepath=str(blend),compress=True)
    objects=import_scene(model)
    for name,faces in added:
        assert match_geometry([face for face,_ in faces],[face for face,_ in records(objects[name])]),name

    write_tsv(OUT/names[0],rows)
    assert (OUT/names[0]).read_bytes().startswith((prior/names[0]).read_bytes())
    for name in names[1:]:
        (OUT/name).write_bytes((prior/name).read_bytes())
    meta.update(status=STATUS,before_construction=rel(source),before_construction_sha256=sha(source),
                before_layout=row["layout"],before_layout_sha256=sha(ROOT/row["layout"]),
                source_glb=row["glb"],source_glb_sha256=row["glb_sha256"],
                source_physics=row["native"],source_physics_sha256=row["native_sha256"],
                glb_sha256=sha(model),physics_sha256=sha(collision),blend_sha256=sha(blend),
                builder=rel(Path(__file__)),builder_sha256=sha(Path(__file__)),
                retained_native_records=len(before),removed_source_meshes=[],new_interface_parts=[n for n,_ in added],
                furnishing=fitted,tabletop_clutter=clutter,documents=documents)
    construction=OUT/"construction.json"
    write(construction,meta)
    row.update(construction=rel(construction),blend=rel(blend),glb=rel(model),native=rel(collision),
               glb_sha256=sha(model),native_sha256=sha(collision))
    next(s for s in roster["sites"] if s["id"]=="WLD-10")["decision"]=STATUS
    roster["baseline"]=rel(PRIOR/"roster.json")
    write(STAGE/"roster.json",roster)
    resources=(model,collision,*(OUT/n for n in names[:3]))
    write_tsv(STAGE/"source-hashes.tsv",[(p.name,sha(p)) for p in resources])
    print("PASS",len(fitted),"furnishings,",len(clutter),"tabletop objects,",len(documents),
          "supported readable records and",len(added),"matched native components")


if __name__=="__main__":
    build()
