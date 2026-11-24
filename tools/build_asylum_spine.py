"""Rebuild the two Asylum ground-spine partitions between measured openings."""
import hashlib
import json
from pathlib import Path
from shutil import copyfile
import sys

import bpy
from mathutils import Vector
from mathutils.bvhtree import BVHTree

ROOT = Path(__file__).resolve().parents[1]
sys.path[:0] = [str(ROOT / "tools"), str(ROOT / "development/levels/blockouts")]
from append_static_glb import append
from build_circulation_construction import native_write, solid
from build_prison_sally_gate import export
from verify_college_electrical import native_records
from verify_college_containment import triangles
from verify_college_services import match_geometry
from review_roster_macroterrain import records

BASE = ROOT / "development/levels/rebuild/asylum-interior-door-pass"
# Each primary wall spans between actual jambs. The named room-shell copies
# are wholly within it and share its datum and thickness.
PARTITIONS = {
    "west": (26, (
        (12, 13.3, ("Flat route shell Wall ground-observation E",)),
        (14.7, 19.8, ("Flat route shell Wall spine W.001", "Flat route shell Wall ground-observation E.001",
                       "Wall ground-linen E", "Flat route shell Wall ground-ward-link E")),
        (21.2, 23.3, ("Flat route shell Wall spine W.002", "Flat route shell Wall ground-ward-link E.001",
                       "Flat route shell Wall ground-wc E")),
        (24.7, 44.3, ("Flat route shell Wall spine W.003", "Flat route shell Wall day E",
                       "Flat route shell Wall ground-wc E.001", "Wall ground-wash E", "Wall lift E")),
        (45.7, 50, ("Flat route shell Wall spine W.004",)),
    )),
    "east": (29, (
        (12, 15.3, ("Flat route shell Wall spine E",)),
        (16.7, 27.3, ("Flat route shell Wall spine E.001", "Flat route shell Wall main-stair W",
                       "Flat route shell Wall staff-office W.001")),
        (28.7, 34.3, ("Flat route shell Wall spine E.002", "Flat route shell Wall clinical-link W",
                       "Flat route shell Wall main-stair W.001")),
        (35.7, 44.3, ("Flat route shell Wall spine E.003", "Flat route shell Wall clinical-link W.001",
                       "Flat route shell Wall dining W")),
        (45.7, 50, ("Flat route shell Wall spine E.004",)),
    )),
}


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def bounds(obj):
    points = [obj.matrix_world @ Vector(corner) for corner in obj.bound_box]
    coords = [[v.x for v in points], [-v.y for v in points], [v.z for v in points]]
    return tuple((round(min(axis), 5), round(max(axis), 5)) for axis in coords)


def finish(material):
    sockets = [node for node in material.node_tree.nodes if node.type == "BSDF_PRINCIPLED"]
    assert len(sockets) == 1
    node = sockets[0]
    base = node.inputs["Base Color"]
    assert not base.is_linked
    return tuple(base.default_value)


def build(out):
    source = json.loads((BASE / "checkpoint.json").read_bytes())["source_authority"]
    for key in ("blend", "glb", "native"):
        assert sha(ROOT / source[key]) == source[key + "_sha256"]
    bpy.ops.wm.open_mainfile(filepath=str(ROOT / source["blend"]))
    bpy.context.preferences.filepaths.save_version = 0
    out.mkdir(parents=True, exist_ok=True)

    built, removed, runs = [], set(), []
    for side, (center, intervals) in PARTITIONS.items():
        x0, x1 = round(center - .08, 5), round(center + .08, 5)
        for number, (z0, z1, names) in enumerate(intervals, 1):
            walls = [bpy.context.scene.objects[name] for name in names]
            finish_source = walls[0].data.materials[0]
            assert bounds(walls[0]) == ((x0, x1), (z0, z1), (0, 3.4)), names[0]
            for obj in walls:
                x, (near, far), y = bounds(obj)
                assert x == (x0, x1) and y == (0, 3.4) and z0 <= near < far <= z1, obj.name
                mat = obj.data.materials[0]
                assert len(obj.data.materials) == 1 and mat.name.startswith("Neutral shell"), (obj.name, mat.name)
                # Preserve the canonical renderer copy for this neutral masonry.
                assert max(abs(a-b) for a,b in zip(finish(mat), finish(finish_source))) < .02, obj.name
            for obj in walls:
                removed.add(obj.name)
                bpy.data.objects.remove(obj, do_unlink=True)
            part = solid(f"Asylum shared spine {side} {number}", [x0, z0, x1, z1], 0, 3.4,
                         finish_source, "shared masonry partition", "spine")
            part["retired_components"] = " | ".join(names)
            built.append(part)
            runs.append(dict(component=part.name, side=side, x0=x0, x1=x1, z0=z0, z1=z1,
                             sill=0, head=3.4, finish=finish_source.name, replaced=list(names)))

    electrical = json.loads((ROOT / source["staged_electrical_installation"]).read_bytes())
    transfers = {}
    for control in electrical["controllers"]:
        previous = control["support"]
        if previous not in removed:
            continue
        run = next(row for row in runs if previous in row["replaced"])
        wall = bpy.context.scene.objects[run["component"]]
        x, y, z = control["at"]
        fixed = run["x1"] if run["side"] == "west" else run["x0"]
        direction = Vector((-1 if run["side"] == "west" else 1, 0, 0))
        surface = BVHTree.FromObject(wall, bpy.context.evaluated_depsgraph_get())
        for dy, dz in ((0, 0), (.046, .047), (-.046, -.047)):
            origin = Vector((x, -(z + dz), y + dy)) - direction * .08
            hit, normal, _, _ = surface.ray_cast(origin, direction, .18)
            assert hit is not None and abs(hit.x - fixed) < .001 and normal.dot(direction) < -.99, control["id"]
        control["support"] = wall.name
        transfers[control["id"]] = {"prior_support": previous, "new_support": wall.name, "contact_x": fixed}
    assert set(transfers) == {"asylum-switch-00"}

    target = {ext: out / f"asylum-spine.{ext}" for ext in ("blend", "glb", "hcs2")}
    bpy.ops.file.pack_all()
    bpy.ops.wm.save_as_mainfile(filepath=str(target["blend"]), compress=True)
    additions = out / "_spine-additions.glb"
    export(additions, built)
    append(ROOT / source["glb"], additions, target["glb"], remove_mesh_names=removed, reuse_embedded_images=True)
    additions.unlink()
    native_write(ROOT / source["native"], target["hcs2"], removed, built)
    old, new = native_records(ROOT / source["native"]), native_records(target["hcs2"])
    retained = [item for item in old if item[0].split(":", 1)[-1] not in removed]
    assert len(old) - len(retained) == len(removed) and new[:len(retained)] == retained
    assert [item[0] for item in new[len(retained):]] == [obj.name for obj in built]
    assert all(item[1:3] == (1, 12) for item in new[len(retained):])
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.import_scene.gltf(filepath=str(target["glb"]))
    for item in new[len(retained):]:
        assert match_geometry(triangles(item), [tri for tri, _ in records(bpy.context.scene.objects[item[0]])]), item[0]

    for name in ("door-survey.tsv", "mission.tsv", "services.tsv"):
        copyfile(BASE / name, out / name)
    rows = (BASE / "arrival.tsv").read_text(encoding="utf-8").splitlines()
    rows[0] = "\t".join(["SOURCE", target["glb"].relative_to(ROOT).as_posix(), sha(target["glb"]),
                         target["hcs2"].relative_to(ROOT).as_posix(), sha(target["hcs2"])])
    (out / "arrival.tsv").write_bytes(("\n".join(rows) + "\n").encode())
    proof = dict(partitions=runs, removed=len(removed), assemblies=len(built), retained_native_records=len(retained),
                 transferred_mounts=transfers, exports={ext: sha(path) for ext, path in target.items()})
    (out / "masonry.json").write_bytes((json.dumps(proof, indent=2) + "\n").encode())
    electrical["prior_power_pass_exports"] = electrical["exports"]
    electrical["exports"] = proof["exports"]
    (out / "electrical.json").write_bytes((json.dumps(electrical, indent=2) + "\n").encode())
    wall_table = [("side", "x0", "x1", "z0", "z1", "sill", "head")]
    wall_table.extend(tuple(run[key] for key in wall_table[0]) for run in runs)
    (out / "wall-runs.tsv").write_bytes(("\n".join("\t".join(map(str, row)) for row in wall_table) + "\n").encode())
    print("PASS", len(removed), "obsolete wall bodies replaced by", len(built), "founded masonry runs;",
          len(retained), "other native components retained byte-for-byte")


if __name__ == "__main__":
    build(Path(sys.argv[sys.argv.index("--") + 1]).resolve())
