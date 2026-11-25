"""Install three measured Wharf walk-in refuges on the retained room slabs."""
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
from verify_college_services import match_geometry
from review_roster_macroterrain import records, sha

BASE=ROOT/"development/levels/rebuild"
LAND=BASE/"wharf-landward-pass"
DOORS=BASE/"wharf-door-pass"
OUT=BASE/"wharf-cover-pass"


def relative(p):
    return p.relative_to(ROOT).as_posix()


def main():
    authority=json.loads((DOORS/"checkpoint.json").read_bytes())
    assert all(sha(ROOT/name)==checksum for name,checksum in authority["geometry_sha256"].items())
    assert all(sha(DOORS/name)==authority["evidence_sha256"][relative(DOORS/name)]
               for name in ("room-layout.tsv","door-schedule.tsv"))
    source=LAND/"enclosed-wharf"
    bpy.ops.wm.open_mainfile(filepath=str(source.with_suffix(".blend")))
    bpy.context.preferences.filepaths.save_version=0
    positions=(("cottage-a",5.45,46,-math.pi/2),("cottage-b",58.55,46,math.pi/2),
               ("net",54.5,21.5,math.pi/2))
    zones={}
    for row in (DOORS/"room-layout.tsv").read_text(encoding="utf-8").splitlines():
        r=row.split("\t")
        if r[0]=="SPACE":
            zones[r[1]]=dict(owner=int(r[2]),y=float(r[7]),height=float(r[8]))
    assembly=Assembly()
    fitted=cupboards(assembly,zones,positions,site="wharf",floor_roles=("floor",))
    for model in assembly.built:
        model.name=model.name.removeprefix('College ')
    OUT.mkdir(exist_ok=True)
    dest=OUT/"wharf-cover"
    fragment=ROOT/"build/wharf-cover-fragment.glb"
    export(fragment,assembly.built)
    append(source.with_suffix(".glb"),fragment,dest.with_suffix(".glb"),reuse_embedded_images=True)
    changed=native_write(source.with_suffix(".hcs2"),dest.with_suffix(".hcs2"),set(),assembly.built,
                         open_components={o.name for o in assembly.built})
    before=split_native(source.with_suffix(".hcs2"))
    after=split_native(dest.with_suffix(".hcs2"))
    assert after[:len(before)]==before and len(after)==len(before)+3
    bpy.ops.file.pack_all()
    bpy.ops.wm.save_as_mainfile(filepath=str(dest.with_suffix(".blend")),compress=True)
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.import_scene.gltf(filepath=str(dest.with_suffix(".glb")))
    for name,tris in changed:
        reopened=[xyz for xyz,_ in records(bpy.context.scene.objects[name])]
        assert match_geometry([xyz for xyz,_ in tris],reopened),name
    rows=[["COVER",f["id"],f["owner"],*f["position"],f["yaw"]] for f in fitted]
    (OUT/"room-layout.tsv").write_bytes((DOORS/"room-layout.tsv").read_bytes()
        +"".join("\t".join(map(str,row))+"\n" for row in rows).encode())
    (OUT/"door-schedule.tsv").write_bytes((DOORS/"door-schedule.tsv").read_bytes())
    result=dict(cupboards=fitted,retained_collision_parts=len(before),added_collision_parts=3,
                exports={relative(dest.with_suffix(e)):sha(dest.with_suffix(e)) for e in (".blend",".glb",".hcs2")})
    (OUT/"construction.json").write_bytes((json.dumps(result,indent=2)+"\n").encode())
    print("PASS three source-backed walk-in refuges; 36 footing checks")


if __name__=="__main__":
    main()
