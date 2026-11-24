"""Add editable closed and parked-open leaf poses to the retained School scene."""
import argparse
import csv
import hashlib
import math
from pathlib import Path
import sys

import bpy
from mathutils import Matrix, Vector


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def leaf_transform(axis, center, frame, width, side, swing, opened):
    x, y, z = center
    along = axis == "z"
    hinge_x = x + side * width / 2 if along else frame
    hinge_z = frame if along else z + side * width / 2
    cos = (0 if along else -swing) if opened else (side if along else 0)
    sin = (-swing if along else 0) if opened else (0 if along else side)
    return (Matrix.Translation(Vector((hinge_x, -hinge_z, y)))
            @ Matrix.Rotation(-math.atan2(sin, cos), 4, "Z"))


def build(source, schedule, asset, target, vertices):
    bpy.ops.wm.open_mainfile(filepath=str(source.resolve()))
    bpy.context.preferences.filepaths.save_version = 0
    scene = bpy.context.scene
    existing = set(scene.objects)
    original_names = {obj.name for obj in existing}
    asset_sha = digest(asset)
    bpy.ops.import_scene.gltf(filepath=str(asset.resolve()))
    imported = set(scene.objects) - existing
    meshes = [(obj.data, obj.matrix_world.copy()) for obj in imported if obj.type == "MESH"]
    assert meshes, "The canonical leaf has no source mesh"
    for obj in imported:
        bpy.data.objects.remove(obj, do_unlink=True)

    for state, opened in (("closed", False), ("open", True)):
        group = bpy.data.collections.new(f"School door leaves {state}")
        scene.collection.children.link(group)
        for row in csv.reader(schedule.open(encoding="utf-8"), delimiter="\t"):
            if not row[0].isdigit():
                continue
            portal, key, room_a, room_b, _, _, x, y, z, width, axis, near, far, height, count, hinge, swing, sha = row
            assert asset_sha == sha, key
            width, height, frame = float(width), float(height), (float(near) + float(far)) / 2
            leaves = int(count)
            scale = Matrix.Diagonal((width / leaves, 1, height / 2.1, 1))
            centered = Matrix.Translation(Vector((0, .03, 0)))
            for side in (-1, 1) if leaves == 2 else (int(hinge),):
                pivot = leaf_transform(axis, (float(x), float(y), float(z)), frame,
                                       width, side, int(swing), opened)
                # Physical hinge is the Blender object origin, while the source mesh
                # is centered on the actual hinge plane and scaled to the clear frame.
                for index, (mesh, local) in enumerate(meshes):
                    fitted = mesh.copy()
                    fitted.transform(scale @ centered @ local)
                    obj = bpy.data.objects.new(f"School {portal} {state} {side:+} {index}", fitted)
                    group.objects.link(obj)
                    obj.matrix_world = pivot
                    obj["portal"] = int(portal)
                    obj["frame"] = key
                    obj["state"] = state
                    obj["rooms"] = room_a + " / " + room_b
                    obj["source_sha256"] = sha
        group.hide_render = opened
        group.hide_viewport = opened
    bpy.ops.file.pack_all()
    bpy.ops.wm.save_as_mainfile(filepath=str(target.resolve()), compress=True)
    bpy.ops.wm.open_mainfile(filepath=str(target.resolve()))

    assert original_names <= {obj.name for obj in bpy.context.scene.objects}
    with vertices.open("w", encoding="utf-8", newline="\n") as report:
        for obj in sorted(bpy.context.scene.objects, key=lambda o: o.name):
            if "portal" not in obj or "source_sha256" not in obj:
                continue
            obj.data.calc_loop_triangles()
            for triangle in obj.data.loop_triangles:
                world = [obj.matrix_basis @ obj.data.vertices[v].co for v in triangle.vertices]
                xyz = [v for point in world for v in (point.x, point.z, -point.y)]
                report.write(f"{obj['portal']}\t{obj['state']}\t" + "\t".join(f"{v:.8f}" for v in xyz) + "\n")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    for name in ("source", "schedule", "asset", "target", "vertices"):
        parser.add_argument(name, type=Path)
    args = parser.parse_args(sys.argv[sys.argv.index("--") + 1:])
    build(args.source, args.schedule, args.asset, args.target, args.vertices)
