"""Mount the Asylum distribution bank and circuits on the retained plant and room structure."""
import json
import math
from pathlib import Path
from shutil import copyfile
import sys

import bpy
from mathutils import Vector

ROOT=Path(__file__).resolve().parents[1]
sys.path[:0]=[str(ROOT/"tools"),str(ROOT/"development/levels/blockouts")]
from append_static_glb import append
from build_circulation_construction import native_write
from build_college_electrical import Assembly, point
from build_prison_plant import install
from build_prison_sally_gate import export
from build_school_electrical import cupboards
from review_roster_macroterrain import records,sha
from verify_college_containment import triangles
from verify_college_electrical import native_records
from verify_college_services import match_geometry

BASE=ROOT/"development/levels/rebuild/asylum-patient-court-pass"
CIRCUITS=(
    ("spine",(27.3,36.8,-1,0),((27.5,16.5),(27.5,32),(27.5,47))),
    ("admission",(32.3,56.85,0,1),((29,54),(32.5,54))),
    ("day",(13.5,48.85,0,1),((15,45),(22,44))),
    ("dining",(36.2,48.85,0,1),((33,44),(38,46))),
    ("ground-ward-link",(18.5,20.1,0,-1),((13,20.5),(23,20.5))),
    ("treatment",(41,38.8,0,1),((38,33),(41,37))),
    ("hydro-change",(47.8,31.7,1,0),((46.7,31.5),(47.4,33.6))),
    ("upper-spine",(27.3,33.6,-1,0),((27.5,15.5),(27.5,25),(27.5,35))),
    ("records",(41,56.8,0,1),((41,56),)),
    ("quiet-lobby",(52.35,13.2,0,-1),((47.5,14),(51.5,14))),
    ("chapel",(19,8.8,0,1),((14,6),(18,6))),
    ("plant",(37.4,25.15,0,-1),((40,26),(37.6,32),(42.4,33))),
)

CUPBOARDS=(("day",12.1,48.9,math.pi),("records",42.75,56.95,math.pi),("plant",43.6,24.85,0))

class AsylumElectrical(Assembly):
    def effect(self,key,kind,owner,xyz,yaw,style):
        super().effect(key.replace("college-","asylum-"),kind,owner,xyz,yaw,style.replace("EM-","AS-"))


def build(out):
    source=json.loads((BASE/"checkpoint.json").read_bytes())["source_authority"]
    assert sha(ROOT/source["glb"])==source["glb_sha256"] and sha(ROOT/source["native"])==source["native_sha256"]
    original=json.loads((ROOT/source["construction"]).read_bytes())
    assert sha(ROOT/source["blend"])==original["blend_sha256"] and sha(ROOT/source["layout"])==original["layout_sha256"]
    layout=json.loads((ROOT/source["layout"]).read_bytes())
    zones={s["id"]:s for s in layout["zones"]}
    bpy.ops.wm.open_mainfile(filepath=str(ROOT/source["blend"]))
    bpy.context.preferences.filepaths.save_version=0
    assembly=AsylumElectrical()
    hiding=cupboards(assembly,zones,CUPBOARDS,site="asylum")
    board=install(assembly,zones["plant"],main_x=40,probe_z=25.3,site="Asylum",circuit="AS-MAIN")
    for owner,(room,control,positions) in enumerate(CIRCUITS):
        zone=zones[room]
        assert zone["owner"]==owner
        assembly.switch(zone,*control)
        mount=assembly.controllers[-1]
        mount["id"]=f"asylum-switch-{owner:02d}"
        mount["circuit"]=f"AS-{owner:02d}"
        for index,(x,z) in enumerate(positions,1):
            tangent=Vector((1,0,0))
            if "spine" in room:tangent=Vector((0,1,0))
            assembly.mounted_lamp(zone,f"asylum-{room}-load-{index}",point(x,zone["y"]+.95,z),
                                 Vector((0,0,1)),6,tangent)
    for lamp in assembly.loads:
        lamp["circuit"]=lamp["circuit"].replace("EM-","AS-")
    for obj in assembly.built:
        obj.name=obj.name.removeprefix("College ").replace("EM-","AS-")
    out.mkdir(parents=True,exist_ok=True)
    target={ext:out/f"asylum-power.{ext}" for ext in ("blend","glb","hcs2")}
    additions=out/"_asylum-additions.glb"
    export(additions,assembly.built)
    append(ROOT/source["glb"],additions,target["glb"],reuse_embedded_images=True)
    additions.unlink()
    native_write(ROOT/source["native"],target["hcs2"],set(),assembly.built,
                  open_components={obj.name for obj in assembly.built if obj.get("source_asset")})
    retained=native_records(ROOT/source["native"])
    after=native_records(target["hcs2"])
    assert after[:len(retained)]==retained
    bpy.ops.file.pack_all()
    bpy.ops.wm.save_as_mainfile(filepath=str(target["blend"]),compress=True)
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.import_scene.gltf(filepath=str(target["glb"]))
    for part in after[len(retained):]:
        assert match_geometry(triangles(part),[tri for tri,_ in records(bpy.context.scene.objects[part[0]])]),part[0]
    rows=(BASE/"arrival.tsv").read_text(encoding="utf-8").splitlines()
    rows[0]="\t".join(("SOURCE",target["glb"].relative_to(ROOT).as_posix(),sha(target["glb"]),
                              target["hcs2"].relative_to(ROOT).as_posix(),sha(target["hcs2"])))
    (out/"arrival.tsv").write_bytes(("\n".join(rows)+"\n").encode())
    for name in ("door-survey.tsv","mission.tsv"):
        copyfile(BASE/name,out/name)
    sockets=[(s["id"],s["type"],s["room"],*s["position"],s["yaw"],s["style"]) for s in assembly.sockets]
    (out/"services.tsv").write_bytes("".join("\t".join(map(str,row))+"\n" for row in sockets).encode())
    proof=dict(board=board,hiding_cupboards=hiding,controllers=assembly.controllers,loads=assembly.loads,routes=assembly.routes,
        retained_components=len(retained),new_components=len(after)-len(retained),
        exports={ext:sha(path) for ext,path in target.items()})
    (out/"electrical.json").write_bytes((json.dumps(proof,indent=2)+"\n").encode())
    print("PASS",len(assembly.controllers),"mounted switches and",len(assembly.loads),"loads")


if __name__=="__main__":
    build(Path(sys.argv[sys.argv.index("--")+1]).resolve())
