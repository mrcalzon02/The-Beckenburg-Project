"""Fit three full-volume walk-in cupboards to the existing Dam rooms."""
import json
import math
from pathlib import Path
import sys

import bpy

ROOT=Path(__file__).resolve().parents[1]
sys.path[:0]=[str(ROOT/"tools"),str(ROOT/"development/levels/blockouts")]
from append_static_glb import append
from build_circulation_construction import native_write
from build_college_electrical import Assembly
from build_college_grounds import import_scene
from build_dam_spillway_gallery import rel
from build_deliberate_entrances import write
from build_prison_sally_gate import export
from build_school_electrical import cupboards
from compose_room_terrain import split_native
from prepare_cemetery_doors import write_tsv
from review_roster_macroterrain import records,sha
from verify_college_services import match_geometry

BASE=ROOT/"development/levels/rebuild"
PRIOR=BASE/"dam-door-pass/roster.json"
STAGE=BASE/"dam-refuge-pass"
OUT=STAGE/"wld-09"
GLB,NATIVE,BLEND=(OUT/("dam-with-refuges"+ext) for ext in (".glb",".hcs2",".blend"))
STATUS="DAM_THREE_FOUNDED_WALK_IN_REFUGES_STAGED"
POSITIONS=(("office",9.46,30.8,-math.pi/2),
           ("workshop",49.8,60.60,math.pi),
           ("plant",37.46,12.25,-math.pi/2))


def build():
    roster=json.loads(PRIOR.read_bytes())
    row=next(site for site in roster["active_sources"] if site["id"]=="WLD-09")
    prior=ROOT/row["construction"]
    meta=json.loads(prior.read_bytes())
    for key,digest in (("blend","blend_sha256"),("glb","glb_sha256"),("native","physics_sha256"),("layout","layout_sha256")):
        assert sha(ROOT/row[key])==meta[digest],key
    authority=json.loads((PRIOR.parent/"checkpoint.json").read_bytes())["project_bytes_sha256"]
    runtime=prior.with_name("runtime-input.tsv")
    doors=prior.with_name("door-schedule.tsv")
    for path in (runtime,doors,prior):
        assert sha(path)==authority[rel(path)],path
    zones={}
    for line in runtime.read_text(encoding="utf8").splitlines():
        p=line.split("\t")
        if p[0]=="ROOM" and p[1] in {pos[0] for pos in POSITIONS}:
            zones[p[1]]=dict(owner=int(p[2]),y=float(p[7]),height=float(p[8]))

    bpy.ops.wm.open_mainfile(filepath=str(ROOT/row["blend"]))
    bpy.context.preferences.filepaths.save_version=0
    assembly=Assembly()
    fits=cupboards(assembly,zones,POSITIONS,site="dam",floor_roles=(
        "floor","constant-cell structural floor or roof",
        "contained stair hall floor or roof","solid civil foundation and finished walkable floor"))
    for obj in assembly.built:
        obj.name=obj.name.removeprefix("College ")
    assert len(assembly.built)==3 and all(info["base_contacts"]==12 for info in fits)
    OUT.mkdir(parents=True,exist_ok=True)
    fragment=ROOT/"build/dam-refuges-addition.glb"
    export(fragment,assembly.built)
    append(ROOT/row["glb"],fragment,GLB,reuse_embedded_images=True)
    appended=native_write(ROOT/row["native"],NATIVE,set(),assembly.built,
                          open_components={obj.name for obj in assembly.built})
    retained=split_native(ROOT/row["native"])
    actual=split_native(NATIVE)
    assert actual[:len(retained)]==retained and len(actual)-len(retained)==3
    bpy.ops.file.pack_all()
    bpy.ops.wm.save_as_mainfile(filepath=str(BLEND),compress=True)
    models=import_scene(GLB)
    for name,triangles in appended:
        assert match_geometry([t for t,_ in triangles],[t for t,_ in records(models[name])]),name

    rows=[line.split("\t") for line in runtime.read_text(encoding="utf8").splitlines()]
    rows.extend(("COVER",p["id"],p["owner"],*p["position"],p["yaw"]) for p in fits)
    write_tsv(OUT/"runtime-input.tsv",rows)
    (OUT/"door-schedule.tsv").write_bytes(doors.read_bytes())
    meta.update(status=STATUS,source_glb=row["glb"],source_glb_sha256=row["glb_sha256"],
        source_physics=row["native"],source_physics_sha256=row["native_sha256"],
        before_layout=row["layout"],before_layout_sha256=sha(ROOT/row["layout"]),
        before_segment_map=meta["source_segment_map"],before_stair_components=meta["stair_components"],
        removed_source_meshes=[],new_interface_parts=[name for name,_ in appended],
        retained_native_records=len(retained),refuges=fits,door_schedule_sha256=sha(doors),
        builder=rel(Path(__file__)),builder_sha256=sha(Path(__file__)),
        glb_sha256=sha(GLB),physics_sha256=sha(NATIVE),blend_sha256=sha(BLEND))
    write(OUT/"construction.json",meta)
    row.update(construction=rel(OUT/"construction.json"),glb=rel(GLB),native=rel(NATIVE),blend=rel(BLEND),
               glb_sha256=sha(GLB),native_sha256=sha(NATIVE))
    next(site for site in roster["sites"] if site["id"]=="WLD-09")["decision"]=STATUS
    roster["baseline"]=rel(PRIOR)
    write(STAGE/"roster.json",roster)
    print("PASS",len(fits),"Dam refuges on",sum(fit["base_contacts"] for fit in fits),"slab contacts")


if __name__=="__main__":
    build()
