"""Install the Asylum L77 flight guards on the actual stair treads."""
import json
from pathlib import Path
from shutil import copyfile
import sys

import bpy
from mathutils import Vector
from mathutils.bvhtree import BVHTree

sys.path.insert(0, str(Path(__file__).resolve().parent))
from build_asylum_spine import ROOT, append, bounds, export, match_geometry, native_records, native_write, records, sha, triangles

BASE = ROOT / "development/levels/rebuild/asylum-upper-spine-pass"


def point(x, y, z):
    return Vector((x, -z, y))


def bar(name, start, end, thickness, material):
    delta = end - start
    bpy.ops.mesh.primitive_cube_add(size=1, location=(start + end) / 2)
    part = bpy.context.object
    part.name = name
    part.rotation_euler = delta.to_track_quat("Z", "Y").to_euler()
    part.scale = (thickness, thickness, delta.length)
    bpy.ops.object.transform_apply(location=True, rotation=True, scale=True)
    part.data.materials.append(material)
    part["role"] = "main stair flight guard"
    return part


def build(out):
    source = json.loads((BASE / "checkpoint.json").read_bytes())["source_authority"]
    for kind in ("blend", "glb", "native"):
        assert sha(ROOT / source[kind]) == source[kind + "_sha256"]
    bpy.ops.wm.open_mainfile(filepath=str(ROOT / source["blend"]))
    bpy.context.preferences.filepaths.save_version = 0
    scene = bpy.context.scene
    material = scene.objects["Asylum L77 upper deck guard west 1"].data.materials[0]
    parts, supports = [], []
    # Interpolate a handrail over the original 19 treads while seating each
    # post on a measured tread rather than the interpolated ramp surface.
    treads = [scene.objects[f"L77 flight0 step{i}"] for i in (1, 5, 9, 13, 17, 19)]
    stations = [(obj, bounds(obj)) for obj in treads]
    z0, z1 = [(b[1][0] + b[1][1]) / 2 for _, b in (stations[0], stations[-1])]
    y0, y1 = [b[2][1] for _, b in (stations[0], stations[-1])]
    assert abs(y1 - 3.4) < .001 and abs(z1 - 27.297) < .001
    for side, x, deck_x in (("west", 31.26, 31.005), ("east", 32.74, 32.995)):
        label = f"Asylum L77 flight {side}"
        for i, (tread, shape) in enumerate(stations, 1):
            z = (shape[1][0] + shape[1][1]) / 2
            y = shape[2][1]
            top = y0 + (y1 - y0) * (z - z0) / (z1 - z0) + 1.016
            tree = BVHTree.FromPolygons(
                [tread.matrix_world @ vertex.co for vertex in tread.data.vertices],
                [tuple(face.vertices) for face in tread.data.polygons])
            for dx in (-.03, .03):
                for dz in (-.03, .03):
                    hit, normal, _, _ = tree.ray_cast(point(x+dx, y+.1, z+dz), Vector((0, 0, -1)), .20)
                    assert hit is not None and abs(hit.z-y) < .0001 and normal.z > .999, (side, i)
            parts.append(bar(f"{label} post {i}", point(x, y, z), point(x, top, z), .06, material))
            supports.append(dict(side=side, tread=tread.name, x=x, y=y, z=z, head=top, tread_contacts=4))
        for height, width, name in ((1.016, .055, "handrail"), (.53, .042, "midrail")):
            parts.append(bar(f"{label} {name}", point(x, y0+height, z0), point(x,y1+height,z1), width, material))
            # A short return continues into the extant upper landing rail.
            parts.append(bar(f"{label} {name} return", point(x,y1+height,z1),
                     point(deck_x, y1+height, 27.42), width, material))

    out.mkdir(parents=True, exist_ok=True)
    target = {ext: out / f"asylum-stair.{ext}" for ext in ("blend", "glb", "hcs2")}
    bpy.ops.file.pack_all()
    bpy.ops.wm.save_as_mainfile(filepath=str(target["blend"]), compress=True)
    addition = out / "_handrails.glb"
    export(addition, parts)
    append(ROOT / source["glb"], addition, target["glb"], reuse_embedded_images=True)
    addition.unlink()
    native_write(ROOT / source["native"], target["hcs2"], set(), parts)
    old, new = native_records(ROOT / source["native"]), native_records(target["hcs2"])
    assert new[:len(old)] == old and [row[0] for row in new[len(old):]] == [part.name for part in parts]
    assert all(row[1:3] == (1, 12) for row in new[len(old):])
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.import_scene.gltf(filepath=str(target["glb"]))
    for row in new[len(old):]:
        assert match_geometry(triangles(row), [tri for tri, _ in records(bpy.context.scene.objects[row[0]])])
    for name in ("door-survey.tsv", "mission.tsv", "services.tsv", "wall-runs.tsv", "guard-runs.tsv"):
        copyfile(BASE / name, out / name)
    rows = (BASE / "arrival.tsv").read_text(encoding="utf-8").splitlines()
    rows[0] = "\t".join(["SOURCE", target["glb"].relative_to(ROOT).as_posix(), sha(target["glb"]),
                          target["hcs2"].relative_to(ROOT).as_posix(), sha(target["hcs2"])])
    (out / "arrival.tsv").write_bytes(("\n".join(rows) + "\n").encode())
    columns = ("side", "tread", "x", "y", "z", "head", "tread_contacts")
    (out / "handrail-supports.tsv").write_bytes(("\t".join(columns) + "\n" + "".join(
        "\t".join(str(row[key]) for key in columns) + "\n" for row in supports)).encode())
    proof = dict(parts=len(parts), kept_native_components=len(old), tread_contacts=48,
                 clear_width=round(32.74-31.26-.06, 3), exports={ext: sha(path) for ext, path in target.items()})
    (out / "handrails.json").write_bytes((json.dumps(proof, indent=2) + "\n").encode())
    electrical = json.loads((ROOT / source["staged_electrical_installation"]).read_bytes())
    electrical["prior_upper_spine_exports"] = electrical["exports"]
    electrical["exports"] = proof["exports"]
    (out / "electrical.json").write_bytes((json.dumps(electrical, indent=2) + "\n").encode())
    print("PASS", len(parts), "fitted closed parts and", len(old), "unchanged native components")


if __name__ == "__main__":
    build(Path(sys.argv[sys.argv.index("--") + 1]).resolve())
