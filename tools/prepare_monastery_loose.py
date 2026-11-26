"""Survey native Monastery loose-object placements against the fitted scene."""
import json
import math
from pathlib import Path
import sys

import bpy
from mathutils import Matrix, Vector

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/"tools"))
from build_college_electrical import ASSETS,point
from build_deliberate_entrances import write
from build_monastery import rel
from build_monastery_ground_drainage import BASE,SCHEDULES
from prepare_cemetery_doors import write_tsv
from review_roster_macroterrain import sha
from verify_college_services import bvh

PRIOR = BASE/"monastery-landscape-pass"
STAGE = BASE/"monastery-loose-pass"
OUT = STAGE/"wld-10"
DOWN = Vector((0,0,-1))
STEM = "monastery-with-landscape"
STATUS = "MONASTERY_NATIVE_LOOSE_BELONGINGS_STAGED"
COLLISION_COMPONENT_NAMES = {"Floor chamber": "119:Floor chamber"}
# These positions use the exact authored levels and indexed worn mesh variants.
PLACEMENTS = (
    ("PORTER_DELIVERY_LEDGER",0,"BOOK","Monastery furnished gate storage shelf 01",25.10,58.04,0,1.58),
    ("ARCHIVE_BRASS_CANDLESTICK",3,"CANDLESTICK","Monastery furnished archive reading table 01",19.43,11.92,.42,1.48),
    ("REFECTORY_COMMON_MUG",4,"MUG","Monastery furnished north refectory table 01",55.62,34.95,-.38,1.48),
    ("REFECTORY_EXTRA_BOWL",4,"BOWL","Monastery furnished south refectory table 01",62.79,46.37,.67,1.48),
    ("BELL_RINGER_TEA_TIN",7,"TIN","Monastery bell floor around upper stair well region0",59.27,24.32,-.52,6.4),
    ("MEMORIAL_OIL_BOTTLE",9,"BOTTLE","Floor chamber",21.75,12.00,.34,-2.55),
)


def fit(scene,record):
    ident,room,kind,parent,x,z,yaw,cast_from = record
    support = bvh([scene.objects[parent]])
    center,normal,_,_ = support.ray_cast(point(x,cast_from,z),DOWN,1.5)
    assert center is not None and normal.z>.99,(ident,parent,x,z)
    height = center.z
    source = ASSETS/f"level_loose_{kind.lower()}_worn.glb"
    before = set(scene.objects)
    bpy.ops.import_scene.gltf(filepath=str(source))
    added = set(scene.objects)-before
    rotation = Matrix.Rotation(yaw,4,"Z")
    positions = [rotation@o.matrix_world@v.co for o in added if o.type=="MESH" for v in o.data.vertices]
    low,high = min(v.z for v in positions),max(v.z for v in positions)
    assert abs(low)<.003 and high<.5,(ident,low,high)
    feet = [v for v in positions if v.z<=low+.003]
    for v in feet:
        contact,n,_,_ = support.ray_cast(point(x+v.x,height+.032,z-v.y),DOWN,.065)
        assert contact is not None and n.z>.98 and abs(contact.z-height)<.003,(ident,v,contact,n)
    # Prove the positioned mesh is free of the retained plates, holders,
    # furniture and architecture. The measured contact is 1 mm above support.
    object_parts = []
    for obj in sorted(added,key=lambda o:o.name):
        if obj.type=="MESH":
            posed=bpy.data.objects.new("Monastery loose "+ident+" "+obj.name,obj.data)
            scene.collection.objects.link(posed)
            posed.matrix_world=Matrix.Translation(point(x,height+.001,z))@rotation@obj.matrix_world
            posed["source_asset"]=source.stem
            object_parts.append(posed)
    for obj in added:
        bpy.data.objects.remove(obj,do_unlink=True)
    bpy.context.view_layer.update()
    others=[o for o in before if o.type=="MESH" and o.name!=parent]
    item_tree=bvh(object_parts)
    overlaps=bvh(others).overlap(item_tree)
    assert not overlaps,(ident,[o.name for o in others if bvh([o]).overlap(item_tree)])
    radius=max(math.hypot(v.x,v.y) for v in positions)
    proof=dict(id=ident,room=room,kind=kind,support=parent,x=x,y=round(height,6),z=z,yaw=yaw,
               source=rel(source),sha256=sha(source),contact_probes=len(feet),mesh_radius=round(radius,6),
               mesh_height=round(high-low,6))
    return proof


def build():
    prior=json.loads((PRIOR/"checkpoint.json").read_bytes())
    roster=json.loads((PRIOR/"roster.json").read_bytes())
    row=next(r for r in roster["active_sources"] if r["id"]=="WLD-10")
    source = {key:ROOT/row[key] for key in ("blend","glb","native","construction")}
    for path in source.values():
        assert sha(path)==prior["project_bytes_sha256"][rel(path)],path
    bpy.ops.wm.open_mainfile(filepath=str(source["blend"]))
    scene=bpy.context.scene
    proof=[fit(scene,record) for record in PLACEMENTS]
    OUT.mkdir(parents=True,exist_ok=True)
    rows=[("LOOSE",r["id"],r["room"],r["kind"],COLLISION_COMPONENT_NAMES.get(r["support"],r["support"]),
           r["x"],r["y"],r["z"],r["yaw"]) for r in proof]
    temporary=ROOT/"build/monastery-loose-rows.tsv"
    write_tsv(temporary,rows)
    old=PRIOR/"wld-10"
    for name in (*SCHEDULES,"drainage-survey.tsv","court-drainage.tsv","north-drainage.tsv","landscape-fittings.tsv"):
        path=old/name
        assert sha(path)==prior["project_bytes_sha256"][rel(path)],path
        data=path.read_bytes()
        if name=="runtime-input.tsv":
            assert data.endswith(b"\n")
            data+=temporary.read_bytes()
        (OUT/name).write_bytes(data)
    write_tsv(OUT/"loose-object-survey.tsv",[("id","room","kind","support","x","y","z","yaw","mesh_contacts"),
              *((r["id"],r["room"],r["kind"],r["support"],r["x"],r["y"],r["z"],r["yaw"],r["contact_probes"])
                for r in proof)])
    receipt=dict(id="WLD-10",status=STATUS,source={k:rel(p) for k,p in source.items()},
                 source_hashes={rel(path):sha(path) for path in source.values()},placements=proof,
                 builder=rel(Path(__file__)),builder_sha256=sha(Path(__file__)))
    write(OUT/"belongings.json",receipt)
    source_paths=[source["glb"],source["native"],*(OUT/name for name in SCHEDULES[:3])]
    write_tsv(STAGE/"source-hashes.tsv",[(p.name,sha(p)) for p in source_paths])
    roster["baseline"]=rel(PRIOR/"roster.json")
    row.update(runtime_input=rel(OUT/"runtime-input.tsv"),loose_object_survey=rel(OUT/"belongings.json"))
    next(s for s in roster["sites"] if s["id"]=="WLD-10")["decision"]=STATUS
    write(STAGE/"roster.json",roster)
    print("PASS",len(proof),"independent worn objects on measured native supports;",
          sum(r["contact_probes"] for r in proof),"source mesh footprint rays and original geometry hashes")


if __name__=="__main__":
    build()
