"""Fit the Mine's three walk-in refuges on the existing surface building slabs."""
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
from build_lookout_native_arrival import table
from build_mine_stair_thresholds import owner
from build_prison_sally_gate import export
from build_school_electrical import cupboards
from review_roster_macroterrain import records,sha
from verify_college_containment import triangles
from verify_college_electrical import native_records
from verify_college_services import match_geometry

BASE=ROOT/"development/levels/rebuild/mine-stair-threshold-pass"
ROOMS=(
    ("shack",3.45,59,-math.pi/2),
    ("assay",52.55,58,math.pi/2),
    ("wash",52.55,34,math.pi/2),
)


def build(out):
    prior=json.loads((BASE/"construction.json").read_bytes())
    source={k:ROOT/prior[k] for k in ("blend","glb","native","layout","edited_door_scene")}
    assert all(sha(source[k])==prior[digest] for k,digest in (
        ("blend","blend_sha256"),("glb","glb_sha256"),("native","physics_sha256"),
        ("layout","layout_sha256"),("edited_door_scene","edited_door_scene_sha256")))
    layout=json.loads(source["layout"].read_bytes())
    zones={z["id"]:dict(z,owner=owner(z["id"])) for z in layout["zones"] if z["id"] in {"shack","assay","wash"}}
    bpy.ops.wm.open_mainfile(filepath=str(source["edited_door_scene"]))
    bpy.context.preferences.filepaths.save_version=0
    assembly=Assembly()
    fitted=cupboards(assembly,zones,ROOMS,site="mine",floor_roles=("floor","continuous crew slab"))
    for obj in assembly.built:
        obj.name=obj.name.removeprefix("College ")

    out.mkdir(parents=True,exist_ok=True)
    targets={ext:out/f"mine-cover.{ext}" for ext in ("blend","glb","hcs2")}
    extra=out/"_cupboards.glb"
    export(extra,assembly.built)
    append(source["glb"],extra,targets["glb"],reuse_embedded_images=True)
    extra.unlink()
    native_write(source["native"],targets["hcs2"],set(),assembly.built,open_components={o.name for o in assembly.built})
    before=native_records(source["native"])
    after=native_records(targets["hcs2"])
    assert after[:len(before)]==before and len(after)==len(before)+len(fitted)
    bpy.ops.file.pack_all()
    bpy.ops.wm.save_as_mainfile(filepath=str(targets["blend"]),compress=True)
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.import_scene.gltf(filepath=str(targets["glb"]))
    for part in after[len(before):]:
        actual=[tri for tri,_ in records(bpy.context.scene.objects[part[0]])]
        assert match_geometry(triangles(part),actual),part[0]

    hashes={ext:sha(path) for ext,path in targets.items()}
    rows=[("SOURCE",targets["glb"].relative_to(ROOT).as_posix(),hashes["glb"],
           targets["hcs2"].relative_to(ROOT).as_posix(),hashes["hcs2"])]
    rows.extend(line.split("\t") for line in (BASE/"arrival.tsv").read_text().splitlines()[1:])
    rows.extend(("COVER",s["id"],s["room"],*s["position"],s["yaw"]) for s in assembly.sockets)
    table(out/"arrival.tsv",rows)
    for name in ("mission.tsv","door-survey.tsv"):
        (out/name).write_bytes((BASE/name).read_bytes())
    receipt=dict(exports=hashes,retained_components=len(before),added_components=len(after)-len(before),cupboards=fitted)
    (out/"hiding.json").write_text(json.dumps(receipt,indent=2)+"\n",encoding="utf-8",newline="\n")
    print("PASS three supported Mine cupboards on existing slabs;",len(before),"unchanged native components")


if __name__=="__main__":
    build(Path(sys.argv[sys.argv.index("--")+1]).resolve())
