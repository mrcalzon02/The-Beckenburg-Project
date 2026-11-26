"""Install founded full-size cupboards on the two exact Cemetery and annex scenes."""
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
from prepare_cemetery_doors import BASE, BOUNDARY, OUTPUT as DOORS, load
from review_roster_macroterrain import records, sha
from verify_college_services import match_geometry

OUTPUT=BASE/"cemetery-refuge-pass"


def build(site, authority):
    annex=site.endswith("mortuary")
    prior=BOUNDARY/site/"bounded-grounds"
    previous=DOORS/site
    assert all(sha(ROOT/p)==sig for p,sig in authority["boundary_exports"].items())
    assert all(sha(ROOT/p)==sig for p,sig in authority["evidence"].items())
    rooms=(previous/"room-layout.tsv").read_bytes()
    zones={}
    for line in rooms.decode().splitlines():
        row=line.split("\t")
        if row[0]=="SPACE":
            zones[row[1]]=dict(owner=int(row[2]),y=float(row[7]),height=float(row[8]))
    bpy.ops.wm.open_mainfile(filepath=str(prior.with_suffix(".blend")))
    bpy.context.preferences.filepaths.save_version=0
    assembly=Assembly()
    stations=(
        ("cottage",33.45,44.3,-math.pi/2),
        ("tool",52.55,49.7,math.pi/2),
        ("mort-office",82.55,63.2,math.pi/2) if annex else ("chapel",7.45,44.6,-math.pi/2),
    )
    fitted=cupboards(assembly,zones,stations,site=site)
    for obj in assembly.built:
        obj.name=obj.name.removeprefix("College ")

    out=OUTPUT/site
    out.mkdir(parents=True,exist_ok=True)
    targets={ext:out/f"grounds-with-refuges.{ext}" for ext in ("glb","hcs2","blend")}
    addition=ROOT/"build/cemetery-refuges-addition.glb"
    export(addition,assembly.built)
    append(prior.with_suffix(".glb"),addition,targets["glb"],reuse_embedded_images=True)
    addition.unlink()
    changed=native_write(prior.with_suffix(".hcs2"),targets["hcs2"],set(),assembly.built,
                         open_components={obj.name for obj in assembly.built})
    before=split_native(prior.with_suffix(".hcs2"))
    after=split_native(targets["hcs2"])
    assert after[:len(before)]==before and len(after)-len(before)==len(changed)==3

    bpy.ops.file.pack_all()
    bpy.ops.wm.save_as_mainfile(filepath=str(targets["blend"]),compress=True)
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.import_scene.gltf(filepath=str(targets["glb"]))
    for name,triangles in changed:
        rendered=records(bpy.context.scene.objects[name])
        assert match_geometry([face for face,_ in triangles],[face for face,_ in rendered]),name
    rows=[("COVER", f["id"],f["owner"],*f["position"],f["yaw"]) for f in fitted]
    (out/"room-layout.tsv").write_bytes(rooms+"".join("\t".join(map(str,row))+"\n" for row in rows).encode())
    (out/"door-schedule.tsv").write_bytes((previous/"door-schedule.tsv").read_bytes())
    result=dict(cupboards=fitted, prior_door_scene=authority["native_scene_revision"],
                retained_collision_parts=len(before), added_collision_parts=len(changed),
                exports={path.relative_to(ROOT).as_posix():sha(path) for path in targets.values()})
    (out/"construction.json").write_text(json.dumps(result,indent=2)+"\n",encoding="utf-8",newline="\n")
    print("PASS",site,"three canonical walk-in cupboards on 36 measured floor contacts")


if __name__ == "__main__":
    door=load(DOORS/"checkpoint.json")
    for name in ("wld-07","wld-07-mortuary"):
        build(name,door["sites"][name.upper()])
