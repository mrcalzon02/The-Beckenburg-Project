"""Rebuild the upper assembly hall's full west partition on its retained slab."""
import json
from pathlib import Path
import sys

import bpy

ROOT = Path(__file__).resolve().parents[1]
sys.path[:0] = [str(ROOT / "tools"), str(ROOT / "development/levels/blockouts")]
from append_static_glb import append
from build_circulation_construction import native_write
from build_deliberate_entrances import solid
from build_college_doors import validate_volume
from verify_college_containment import triangles
from verify_college_electrical import native_records
from verify_college_services import match_geometry
from review_roster_macroterrain import records, sha

BASE = ROOT / "development/levels/rebuild"
PRIOR = BASE / "measured-arrival-and-approach-pass/school"
FITTED = BASE / "school-interior-doors-pass"
RETIRED = (
    "Header upper-lobby E",
    "Header assembly W",
    "Flat route shell Wall upper-lobby E",
    "Flat route shell Wall upper-lobby E.001",
    "Flat route shell Wall assembly W",
    "Flat route shell Wall assembly W.001",
)
WALL_X = (29.92, 30.08)
SILL, HEADER, TOP = (3.6, 6.05, 8.1)
FRAME_Z = (32.38, 33.62)


def build(out):
    out.mkdir(parents=True, exist_ok=True)
    source = {ext: PRIOR / f"measured-architecture.{ext}" for ext in ("blend", "glb", "hcs2")}
    meta = json.loads((PRIOR / "construction.json").read_bytes())
    for ext, key in (("blend", "blend_sha256"), ("glb", "glb_sha256"), ("hcs2", "physics_sha256")):
        assert sha(source[ext]) == meta[key], source[ext]
    bpy.ops.wm.open_mainfile(filepath=str(source["blend"]))
    bpy.context.preferences.filepaths.save_version = 0
    old = {obj.name:obj for obj in bpy.context.scene.objects if obj.type == "MESH"}
    assert set(RETIRED) <= set(old)
    material = old["Flat route shell Wall assembly W"].data.materials[0]
    for name in RETIRED:
        bpy.data.objects.remove(old[name], do_unlink=True)
    x0, x1 = WALL_X
    near, far = FRAME_Z
    parts = []
    for label, z0, z1, low in (("north pier", 24, near, SILL),
                                 ("door head", near, far, HEADER),
                                 ("south pier", far, 36, SILL)):
        part = solid(f"School assembly west {label}", [x0,z0,x1,z1], low, TOP, material,
                     "founded assembly hall shared partition", "assembly")
        validate_volume(part)
        parts.append(part)
    originals = native_records(source["hcs2"])
    retained = [r for r in originals if r[0].split(":",1)[-1] not in RETIRED]
    assert len(originals)-len(retained) == len(RETIRED)
    targets = {ext: out / f"assembly-partition.{ext}" for ext in source}
    bpy.ops.object.select_all(action="DESELECT")
    for part in parts:
        part.select_set(True)
    additions = out / "_additions.glb"
    bpy.ops.export_scene.gltf(filepath=str(additions),export_format="GLB",use_selection=True,export_extras=True)
    append(source["glb"],additions,targets["glb"],remove_mesh_names=set(RETIRED))
    additions.unlink()
    native_write(source["hcs2"],targets["hcs2"],set(RETIRED),parts)
    collision = native_records(targets["hcs2"])
    assert collision[:len(retained)] == retained
    bpy.ops.file.pack_all()
    bpy.ops.wm.save_as_mainfile(filepath=str(targets["blend"]),compress=True)
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.import_scene.gltf(filepath=str(targets["glb"]))
    for part in collision[len(retained):]:
        visible = [t for t,_ in records(bpy.context.scene.objects[part[0]])]
        assert match_geometry(triangles(part),visible),part[0]

    rows = (FITTED / "circulation.tsv").read_text(encoding="utf-8").splitlines()
    rows[0] = "\t".join(("SOURCE",targets["glb"].relative_to(ROOT).as_posix(),sha(targets["glb"]),
                                 targets["hcs2"].relative_to(ROOT).as_posix(),sha(targets["hcs2"])))
    # The paved staging area belongs to the forecourt. Only the actual
    # cargo interior receives the extraction owner in the game.
    rows = [row.replace("SPACE\tstage\t14\t","SPACE\tstage\t13\t",1)
            if row.startswith("SPACE\tstage\t14\t") else row for row in rows]
    (out / "circulation.tsv").write_text("\n".join(rows)+"\n",encoding="utf-8",newline="\n")
    leaf=sha(ROOT/"assets/runtime/models/door_1_loose.glb")
    rows=(FITTED/"door-schedule.tsv").read_text(encoding="utf-8").splitlines()
    for portal, name, a, b, owner_a, owner_b, x, z, hinge, swing in (
        (418,"library-upper-corridor","library","upper-corridor",8,7,20,30,-1,-1),
        (419,"classroom-upper-corridor","class-b","upper-corridor",9,7,20,17,1,-1),
        (420,"upper-lobby-assembly","upper-lobby","assembly",7,10,30,33,1,1),
    ):
        rows.append("\t".join(map(str,(portal,name,a,b,owner_a,owner_b,x,SILL,z,1.24,"x",
                                      x-.08,x+.08,2.45,1,hinge,swing,leaf))))
    (out/"door-schedule.tsv").write_text("\n".join(rows)+"\n",encoding="utf-8",newline="\n")
    record=dict(wall_plane_x=WALL_X,run_z=(24,36),sill=SILL,header=HEADER,wall_head=TOP,opening_z=FRAME_Z,
        retired_wall_components=RETIRED,retained_native_components=len(retained),
        triangle_count_added=sum(r[2] for r in collision[len(retained):]),exports={ext:sha(path) for ext,path in targets.items()})
    (out/"partition.json").write_text(json.dumps(record,indent=2)+"\n",encoding="utf-8",newline="\n")
    print(f"PASS rebuilt 12 m partition, retained {len(retained)} exact physical components")


if __name__ == "__main__":
    build(Path(sys.argv[sys.argv.index("--")+1]).resolve())
