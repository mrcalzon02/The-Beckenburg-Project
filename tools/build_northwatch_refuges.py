"""Seat Northwatch's three walk-in refuges on the retained station slabs."""
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
from build_prison_sally_gate import export
from build_school_electrical import cupboards
from compose_room_terrain import split_native
from review_roster_macroterrain import records, sha
from verify_college_services import match_geometry

BASE=ROOT/"development/levels/rebuild"
DOORS=BASE/"northwatch-door-pass"
OUT=BASE/"northwatch-refuge-pass/wld-08"
STATIONS=(("lab",3.45,40.7,-math.pi/2),
          ("dorm",60.55,30.9,math.pi/2),
          ("shelter",68.55,16.1,math.pi/2))


def build():
    checkpoint=json.loads((DOORS/"checkpoint.json").read_bytes())
    for name,digest in (checkpoint["exports"]|checkpoint["evidence"]).items():
        assert sha(ROOT/name)==digest,name
    prior={Path(name).suffix[1:]:ROOT/name for name in checkpoint["exports"]}
    packet=DOORS/"wld-08"
    runtime=(packet/"runtime-input.tsv").read_bytes()
    zones={}
    for line in runtime.decode().splitlines():
        row=line.split("\t")
        if row[0]=="SPACE":
            zones[row[1]]=dict(owner=int(row[2]),y=float(row[7]),height=float(row[8]))
    bpy.ops.wm.open_mainfile(filepath=str(prior["blend"]))
    bpy.context.preferences.filepaths.save_version=0
    assembly=Assembly()
    fit=cupboards(assembly,zones,STATIONS,site="northwatch",
                  floor_roles=("floor","constant-cell structural floor or roof"))
    for obj in assembly.built:
        obj.name=obj.name.removeprefix("College ")
    OUT.mkdir(parents=True,exist_ok=True)
    paths={kind:OUT/f"station-with-refuges.{kind}" for kind in ("blend","glb","hcs2")}
    fragment=ROOT/"build/northwatch-refuges-fragment.glb"
    export(fragment,assembly.built)
    append(prior["glb"],fragment,paths["glb"],reuse_embedded_images=True)
    fragment.unlink()
    added=native_write(prior["hcs2"],paths["hcs2"],set(),assembly.built,
        open_components={obj.name for obj in assembly.built})
    before,after=(split_native(path) for path in (prior["hcs2"],paths["hcs2"]))
    assert after[:len(before)]==before and len(after)-len(before)==len(added)==3
    bpy.ops.file.pack_all()
    bpy.ops.wm.save_as_mainfile(filepath=str(paths["blend"]),compress=True)
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.import_scene.gltf(filepath=str(paths["glb"]))
    for name,triangles in added:
        actual=records(bpy.context.scene.objects[name])
        assert match_geometry([face for face,_ in triangles],[face for face,_ in actual]),name

    rooms=[("COVER",item["id"],item["owner"],*item["position"],item["yaw"]) for item in fit]
    (OUT/"runtime-input.tsv").write_bytes(runtime+"".join("\t".join(map(str,row))+"\n" for row in rooms).encode())
    (OUT/"door-schedule.tsv").write_bytes((packet/"door-schedule.tsv").read_bytes())
    receipt=dict(exports={p.relative_to(ROOT).as_posix():sha(p) for p in paths.values()},
                 previous_scene=checkpoint["native_scene_revision"],cupboards=fit,
                 retained_collision_parts=len(before),added_collision_parts=len(added))
    (OUT/"construction.json").write_text(json.dumps(receipt,indent=2)+"\n",encoding="utf-8",newline="\n")
    print("PASS Northwatch refuge fittings",[(item["space"],item["base_contacts"]) for item in fit])


if __name__=="__main__":
    build()
