"""Fit three real hiding cupboards without changing Prison walls, floors or circulation."""
import json
from pathlib import Path
from shutil import copyfile
import sys

import bpy

ROOT=Path(__file__).resolve().parents[1]
sys.path[:0]=[str(ROOT/"tools"),str(ROOT/"development/levels/blockouts")]
from append_static_glb import append
from build_circulation_construction import native_write
from build_prison_plant import PrisonElectrical
from build_prison_sally_gate import export
from build_school_electrical import cupboards
from review_roster_macroterrain import records, sha
from verify_college_containment import triangles
from verify_college_electrical import native_records
from verify_college_services import match_geometry

BASE=ROOT/"development/levels/rebuild/prison-plant-pass"
CUPBOARDS=(
    ("records",20.8,62.94,0),
    ("chapel",66.1,34.94,0),
    ("plant",43.7,22.76,0),
)


def build(out):
    source=json.loads((BASE/"survey.json").read_bytes())
    source_files={ext:BASE/f"prison-plant.{ext}" for ext in ("blend","glb","hcs2")}
    assert all(sha(path)==source["exports"][ext] for ext,path in source_files.items())
    roster=json.loads((BASE/"roster.json").read_bytes())
    row=next(row for row in roster["active_sources"] if row["id"]=="PRISON")
    layout=json.loads((ROOT/row["layout"]).read_bytes())
    zones={room["id"]:room for room in layout["zones"]}
    bpy.ops.wm.open_mainfile(filepath=str(source_files["blend"]))
    bpy.context.preferences.filepaths.save_version=0
    assembly=PrisonElectrical()
    mounted=cupboards(assembly,zones,CUPBOARDS,site="prison")
    for obj in assembly.built:
        obj.name=obj.name.removeprefix("College ")
    out.mkdir(parents=True,exist_ok=True)
    target={ext:out/f"prison-hiding.{ext}" for ext in ("blend","glb","hcs2")}
    additions=out/"_cupboards.glb"
    export(additions,assembly.built)
    append(source_files["glb"],additions,target["glb"],reuse_embedded_images=True)
    additions.unlink()
    native_write(source_files["hcs2"],target["hcs2"],set(),assembly.built,
                 open_components={obj.name for obj in assembly.built})
    previous=native_records(source_files["hcs2"])
    result=native_records(target["hcs2"])
    assert result[:len(previous)]==previous
    bpy.ops.file.pack_all()
    bpy.ops.wm.save_as_mainfile(filepath=str(target["blend"]),compress=True)
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.import_scene.gltf(filepath=str(target["glb"]))
    for part in result[len(previous):]:
        assert match_geometry(triangles(part),[tri for tri,_ in records(bpy.context.scene.objects[part[0]])]),part[0]
    rows=(BASE/"arrival.tsv").read_text(encoding="utf-8").splitlines()
    rows[0]="\t".join(("SOURCE",target["glb"].relative_to(ROOT).as_posix(),sha(target["glb"]),
                              target["hcs2"].relative_to(ROOT).as_posix(),sha(target["hcs2"])))
    (out/"arrival.tsv").write_bytes(("\n".join(rows)+"\n").encode())
    for name in ("gate.tsv","mission.tsv","door-survey.tsv"):
        copyfile(BASE/name,out/name)
    sockets=[(r["id"],r["type"],r["room"],*r["position"],r["yaw"],r["style"]) for r in assembly.sockets]
    (out/"services.tsv").write_bytes((BASE/"services.tsv").read_bytes()+
        "".join("\t".join(map(str,row))+"\n" for row in sockets).encode())
    receipt=dict(cupboards=mounted,preserved_components=len(previous),added_components=len(result)-len(previous),
                 exports={ext:sha(path) for ext,path in target.items()})
    (out/"hiding.json").write_bytes((json.dumps(receipt,indent=2)+"\n").encode())
    print("PASS three cupboards, supported contacts and",len(previous),"unchanged components")


if __name__=="__main__":
    build(Path(sys.argv[sys.argv.index("--")+1]).resolve())
