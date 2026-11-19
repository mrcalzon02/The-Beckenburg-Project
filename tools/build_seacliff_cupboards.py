"""Fit the existing hollow cupboards to three surveyed Battery rooms."""
import json
from pathlib import Path
import sys

import bpy
from mathutils import Matrix, Vector

sys.path.insert(0, str(Path(__file__).resolve().parent))
from append_static_glb import append
from build_circulation_construction import native_write
from build_college_electrical import bounds, point
from build_seacliff_partitions import ROOT, write
from review_roster_macroterrain import records, sha
from verify_college_containment import triangles
from verify_college_electrical import native_records
from verify_college_services import bvh, match_geometry

BASE = ROOT / "development/levels/rebuild/seacliff-upper-partitions-pass"
ASSET = ROOT / "assets/runtime/models/level_hiding_cupboard_worn.glb"
STEM = "battery-cupboards"
CUPBOARDS = (
    ("west-casemate-stores", "west", 3, 11.5, 4, 27.45),
    ("east-casemate-stores", "east", 4, 57.25, 4, 27.45),
    ("records-cupboard", "records", 10, 26.5, 0, 15.45),
)


def build(out):
    folder = out / "wld-17"
    folder.mkdir(parents=True, exist_ok=True)
    evidence = out / "verification"
    evidence.mkdir(exist_ok=True)
    source = {ext: BASE / "wld-17" / f"upper-casemate-partitions.{ext}" for ext in ("blend", "glb", "hcs2")}
    construction = json.loads((BASE / "wld-17/construction.json").read_bytes())
    assert all(sha(source[k]) == construction[h] for k, h in (
        ("blend", "blend_sha256"), ("glb", "glb_sha256"), ("hcs2", "physics_sha256")))
    layout = json.loads((BASE / "wld-17/authored-layout.json").read_bytes())
    zones = {zone["id"]: zone for zone in layout["zones"]}

    bpy.ops.wm.open_mainfile(filepath=str(source["blend"]))
    bpy.context.preferences.filepaths.save_version = 0
    scene = bpy.context.scene
    original = set(scene.objects)
    floor = bvh([o for o in original if o.get("role") == "floor"])
    bpy.ops.import_scene.gltf(filepath=str(ASSET))
    imported = set(scene.objects) - original
    meshes = sorted((o for o in imported if o.type == "MESH"), key=lambda o: o.name)
    parts = [(o.data, o.matrix_world.copy()) for o in meshes]
    vertices = [matrix @ v.co for mesh, matrix in parts for v in mesh.vertices]
    bottom = min(v.z for v in vertices)
    contacts = sorted({(round(v.x, 5), round(v.y, 5)) for v in vertices if v.z < bottom + .008})
    assert len(contacts) >= 3, contacts
    for obj in imported:
        bpy.data.objects.remove(obj, do_unlink=True)

    pieces, placement = [], []
    for key, room, owner, x, y, z in CUPBOARDS:
        zone = zones[room]
        assert zone["y"] == y
        for dx, dy in contacts:
            at, normal, _, _ = floor.ray_cast(point(x + dx, y + .16, z - dy), Vector((0, 0, -1)), .32)
            assert at is not None and abs(at.z - y) < .003 and normal.z > .98, (key, dx, dy, at)
        transform = Matrix.Translation(point(x, y - bottom, z))
        placed = []
        for index, (mesh, base) in enumerate(parts, 1):
            obj = bpy.data.objects.new(f"Battery {key} {index:02d}", mesh)
            scene.collection.objects.link(obj)
            obj.matrix_world = transform @ base
            obj["source_asset"] = ASSET.stem
            obj["zone"] = room
            obj["role"] = "founded walk-in cupboard"
            placed.append(obj)
        bpy.context.view_layer.update()
        extent = bounds(placed)
        x0, z0, x1, z1 = zone["bounds"]
        lo, hi = extent
        assert x0 + .24 < lo[0] < hi[0] < x1 - .24 and z0 + .24 < lo[2] < hi[2] < z1 - .24, (key, extent)
        assert hi[1] < y + zone["height"] - .10, (key, extent)
        pieces.extend(placed)
        placement.append(dict(id=key, room=room, owner=owner, feet=[x, y, z], yaw=0, bounds=extent,
                              source_origin=[x, round(y - bottom, 6), z], slab_contacts=len(contacts)))

    targets = {ext: folder / f"{STEM}.{ext}" for ext in source}
    bpy.ops.object.select_all(action="DESELECT")
    for obj in pieces:
        obj.select_set(True)
    additions = folder / "_additions.glb"
    bpy.ops.export_scene.gltf(filepath=str(additions), export_format="GLB", use_selection=True, export_extras=True)
    append(source["glb"], additions, targets["glb"], reuse_embedded_images=True)
    additions.unlink()
    native_write(source["hcs2"], targets["hcs2"], set(), pieces, open_components={o.name for o in pieces})
    prior = native_records(source["hcs2"])
    native = native_records(targets["hcs2"])
    assert native[:len(prior)] == prior
    bpy.ops.file.pack_all()
    bpy.ops.wm.save_as_mainfile(filepath=str(targets["blend"]), compress=True)
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.import_scene.gltf(filepath=str(targets["glb"]))
    for part in native[len(prior):]:
        visible = [triangle for triangle, _ in records(bpy.context.scene.objects[part[0]])]
        assert match_geometry(triangles(part), visible), part[0]

    hashes = {ext: sha(path) for ext, path in targets.items()}
    lines = (BASE / "verification/runtime.tsv").read_text(encoding="utf-8").splitlines()
    lines[0] = "\t".join(("SOURCE", targets["glb"].relative_to(ROOT).as_posix(), hashes["glb"],
                          targets["hcs2"].relative_to(ROOT).as_posix(), hashes["hcs2"]))
    for row in placement:
        fields = ("SOCKET", row["id"], "hide", row["owner"], *row["feet"], row["yaw"], "walk-in-cupboard")
        lines.append("\t".join(map(str, fields)))
    (evidence / "runtime.tsv").write_text("\n".join(lines) + "\n", encoding="utf-8", newline="\n")
    layout["fitted_hiding_cupboards"] = placement
    write(folder / "authored-layout.json", layout)
    write(folder / "cupboards.json", dict(source_asset=ASSET.relative_to(ROOT).as_posix(), asset_sha256=sha(ASSET),
        receiver=construction["glb_sha256"], retained_native_components=len(prior),
        added_native_triangles=sum(part[2] for part in native[len(prior):]), fitted=placement, export=hashes))
    construction.update(status="FITTED_CUPBOARDS_ON_VERIFIED_BATTERY_STRUCTURE", glb_sha256=hashes["glb"], physics_sha256=hashes["hcs2"], blend_sha256=hashes["blend"],
        layout=targets["glb"].with_name("authored-layout.json").relative_to(ROOT).as_posix(),
        layout_sha256=sha(folder / "authored-layout.json"), cupboards="cupboards.json")
    for key, ext in (("glb", "glb"), ("native", "hcs2"), ("blend", "blend")):
        construction[key] = targets[ext].relative_to(ROOT).as_posix()
    write(folder / "construction.json", construction)

    roster = json.loads((BASE / "roster.json").read_bytes())
    active = next(item for item in roster["active_sources"] if item["id"] == "WLD-17")
    active.update(glb=construction["glb"], native=construction["native"], blend=construction["blend"],
                  layout=construction["layout"], glb_sha256=hashes["glb"], native_sha256=hashes["hcs2"],
                  construction=(folder / "construction.json").relative_to(ROOT).as_posix())
    decision = next(item for item in roster["sites"] if item["id"] == "WLD-17")
    decision.update(decision="PACKAGED_SOLO_AND_LAN_REVIEW_COMPLETE_CASE_OPEN",
                    dispatch_checkpoint=(out / "checkpoint.json").relative_to(ROOT).as_posix())
    roster["baseline"] = "0feff426e2c0e7d2eeb4755b7e3843757cf122de"
    write(out / "roster.json", roster)
    print("PASS", len(placement), "cupboards,", len(placement) * len(contacts), "measured slab bearings")


if __name__ == "__main__":
    build(Path(sys.argv[sys.argv.index("--") + 1]).resolve())
