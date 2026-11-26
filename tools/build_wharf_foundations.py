"""Fit the Wharf foundation to built deck bays and the cell walls.

Run in Blender background mode from a checkout. The deck, doors, stairs and
saved terrain are retained. Only the dock understructure is changed.
"""
from pathlib import Path
import json
import sys

import bpy
from mathutils import Vector
from mathutils.bvhtree import BVHTree

ROOT = Path(__file__).resolve().parents[1]
sys.path[:0] = [str(ROOT / "tools"), str(ROOT / "development/levels/blockouts")]

from append_static_glb import append
from build_circulation_construction import native_write
from build_roster_foundations import material, prism
from compose_room_terrain import split_native
from export_roster_collision import face_keys
from review_roster_macroterrain import records, sha

BASE = ROOT / "development/levels/rebuild"
SOURCE = BASE / "native-foyer-door-pass/wld-06"
LEGACY = BASE / "terrain-fit-pass/wld-06/construction.json"
OUT = BASE / "wharf-foundation-pass"
STEM = "supported-wharf"
PINNED = {
    "fitted-approaches.blend": "bfcd62d5b236edf2ea13970f56633ad2d09e8605ec0019da119fad5b10fe8363",
    "fitted-approaches.glb": "65b40fb3878ca5e2dae76d6aa487265d069646a04d8612bb0071eb8b33287caf",
    "fitted-approaches.hcs2": "f252b1ccd7f9a043aa5bf58dce841ec31557f3a6f9546d0f6f7a259f4b70d252",
    "authored-layout.json": "7b9030890de464421b6c957cd888a157d20b87936868523808b4b08a8ae2d719",
    LEGACY: "39b4967a4550dabc8fefd7baedf858bec66f664a3d0e71b3ab27c628309ee752",
}


def relative(path):
    return path.relative_to(ROOT).as_posix()


def footprint(x0, z0, x1, z1):
    return [(x0, z0), (x1, z0), (x1, z1), (x0, z1)]


def bounds(obj):
    vertices = [obj.matrix_world @ vertex.co for vertex in obj.data.vertices]
    return tuple(f(point[i] * sign for point in vertices)
                 for f in (min, max) for i, sign in ((0, 1), (2, 1), (1, -1)))


def overlaps(a, b):
    return all(min(a[i + 3], b[i + 3]) - max(a[i], b[i]) > .005 for i in range(3))


def touches(a, b):
    overlaps_by_axis = [min(a[i + 3], b[i + 3]) - max(a[i], b[i]) for i in range(3)]
    return min(overlaps_by_axis) >= -.0001 and sum(gap > .01 for gap in overlaps_by_axis) >= 2


def read_json(path):
    return json.loads(path.read_bytes())


def write_json(path, payload):
    path.write_bytes((json.dumps(payload, indent=2) + "\n").encode())


def terrain_tree(terrain):
    vertices = [terrain.matrix_world @ v.co for v in terrain.data.vertices]
    return BVHTree.FromPolygons(vertices, [tuple(p.vertices) for p in terrain.data.polygons])


def ground(tree, x, z):
    hit = tree.ray_cast(Vector((x, -z, 1.2)), Vector((0, 0, -1)), 12)[0]
    return round(hit.z, 5) if hit is not None else None


def source_route_supports():
    prior = read_json(LEGACY)["construction"]
    route_piles = {row["name"]: row["zone"] for row in prior
                   if row.get("zone", "").startswith("route-")}
    return route_piles | {row["name"]: route_piles[row["supported_by"]] for row in prior
                          if row.get("supported_by") in route_piles}


def create_foundation(originals, removed, tree):
    built = []
    load_paths = []
    timber = material("Wharf tar-treated pier girders", (.24, .135, .075))
    concrete = material("Wharf formed marine concrete", (.36, .39, .37))
    available = {name: bounds(o) for name, o in originals.items()
                 if name not in removed}

    def box(name, x0, z0, x1, z1, low, high, mat, role):
        assert low < high and x0 < x1 and z0 < z1
        obj = prism(name, footprint(x0, z0, x1, z1), high, [low] * 4, mat)
        obj["role"] = role
        built.append(obj)
        available[name] = bounds(obj)
        return name

    def tie(name, x0, z0, x1, z1, points):
        # Deep pile cap and grade beam below the retained floor. Its two
        # driven-pile groups are set in intact soil clear of the open cut.
        supported_by = []
        for index, (x, z) in enumerate(points, 1):
            half = .21
            clip = (x - half, z - half, x + half, z + half)
            soil = [ground(tree, px, pz) for px, pz in [*footprint(*clip), (x, z)]]
            assert all(y is not None and y < -2.86 for y in soil), (name, x, z, soil)
            bottom = min(min(soil) - .55, -4.42)
            pile = box(f"{name} pile {index}", *clip, bottom, -3.10, concrete,
                       "Wharf marine grade-beam driven pile")
            load_paths.append(dict(part=pile, measured_native_ground_y=soil,
                                   bottom_y=round(bottom, 5)))
            supported_by.append(pile)
        beam = box(name, x0, z0, x1, z1, -3.76, -2.765, concrete,
                   "Wharf under-slab marine grade beam")
        assert all(overlaps(available[beam], available[pile]) for pile in supported_by)
        return beam, supported_by

    for label, z in (("north", 26.75), ("south", 35.15)):
        beam, piles = tie(f"Wharf undercroft {label} transverse grade beam",
                           23.15, z - .33, 40.85, z + .33,
                           [(23.48, z), (40.52, z)])
        slab = "Wharf undercroft continuous base region0"
        assert overlaps(available[beam], available[slab]), (beam, slab)
        load_paths.append(dict(part=beam, carried=[slab], supported_by=piles))

    for label, xs in (("west", (17.65, 22.55)), ("east", (41.50, 47.20))):
        for i, x in enumerate(xs, 1):
            beam, piles = tie(f"Wharf {label} stair longitudinal grade beam {i}",
                              x - .33, 28.04, x + .33, 35.68,
                              [(x, 28.35), (x, 35.37)])
            slab = f"Wharf {label} stair-cell foundation region0"
            assert overlaps(available[beam], available[slab]), (beam, slab)
            load_paths.append(dict(part=beam, carried=[slab], supported_by=piles))

    # The stair cuts interrupt the pier piles. Use the existing bent nearest
    # each cut, then carry the overhang into the cast retaining wall by seat.
    girders = [
        ("west north", (20.4, 22.0, 23.6), 27.20, 31.45, "west", 31.45,
         "WLD-06 west jetty driven pile", "west jetty bearing"),
        ("west south", (20.4, 22.0, 23.6), 35.60, 34.55, "west", 34.3,
         "WLD-06 west jetty driven pile", "west jetty bearing"),
        ("east north", (40.4, 42.7, 45.0), 26.40, 31.45, "east", 31.45,
         "WLD-06 east pier-head landing driven pile", "east pier-head landing bearing"),
    ]
    for label, xs, bent, wall_z, cell, wall_station, pile_prefix, frame_name in girders:
        wall = f"Wharf {cell} stair retaining wall {wall_station} region0"
        for x in xs:
            name = f"Wharf {label} stair approach transfer girder at X{x}"
            girder = box(name, x - .20, min(bent - .17, wall_z - .06),
                         x + .20, max(bent + .17, wall_z + .06), -1.13, -.55,
                         timber, "Wharf pier transfer girder")
            b = available[girder]
            piles = [n for n, extent in available.items() if n.startswith(pile_prefix)
                     and overlaps(b, extent)]
            frame = [n for n, extent in available.items() if frame_name in n
                     and overlaps(b, extent)]
            assert len(piles) == 1 and frame and overlaps(b, available[wall]), (name, piles, frame)
            load_paths.append(dict(part=girder, carried=sorted(frame),
                                   supported_by=[piles[0], wall]))

    return built, load_paths


def retain_pile_report(originals, removed, tree):
    retained = []
    for name, obj in sorted(originals.items()):
        if name in removed or obj.get("role") != "native-soil-founded dock pile":
            continue
        x0, low, z0, x1, top, z1 = bounds(obj)
        # Avoid exporter quantization on exact cut borders without dropping
        # the corner test for a pile which partly overlaps an excavation.
        points = [(x0 + .002, z0 + .002), (x0 + .002, z1 - .002),
                  (x1 - .002, z0 + .002), (x1 - .002, z1 - .002),
                  ((x0 + x1) / 2, (z0 + z1) / 2)]
        soil = [ground(tree, x, z) for x, z in points]
        frame = [n for n, mesh in originals.items()
                 if mesh.get("role") in ("continuous dock bearing grid", "rectangular stair-well bearing")
                 and touches(bounds(obj), bounds(mesh))]
        assert all(y is not None and low < y - .12 for y in soil) and frame, (name, soil, frame)
        retained.append(dict(part=name, pile_bottom_y=round(low, 4),
                             measured_native_ground_y=soil, contacts=sorted(frame)))
    return retained


def render(output, name, target, eye, scale, hide_ground=False):
    scene = bpy.context.scene
    if hide_ground:
        for obj in scene.objects:
            obj.hide_render = obj.get("role", "").startswith("saved editor terrain")
    scene.render.engine = "BLENDER_WORKBENCH"
    shading = scene.display.shading
    shading.color_type = "MATERIAL"
    shading.show_cavity = True
    shading.cavity_type = "BOTH"
    scene.render.resolution_x = 1520
    scene.render.resolution_y = 970
    scene.render.resolution_percentage = 100
    scene.camera.location = Vector(eye)
    scene.camera.rotation_euler = (Vector(target) - scene.camera.location).to_track_quat("-Z", "Y").to_euler()
    scene.camera.data.type = "ORTHO"
    scene.camera.data.ortho_scale = scale
    path = output / name
    scene.render.filepath = str(path)
    bpy.ops.render.render(write_still=True)
    return dict(path=relative(path), sha256=sha(path),
                type="Native export underside, terrain hidden" if hide_ground else "Native export site overview")


def main():
    inputs = {SOURCE / path: checksum for path, checksum in PINNED.items()}
    assert all(sha(path) == checksum for path, checksum in inputs.items())
    OUT.mkdir(parents=True, exist_ok=True)
    (OUT / "captures").mkdir(exist_ok=True)
    bpy.ops.wm.open_mainfile(filepath=str(SOURCE / "fitted-approaches.blend"))
    bpy.context.preferences.filepaths.save_version = 0
    originals = {obj.name: obj for obj in bpy.context.scene.objects if obj.type == "MESH"}
    tree = terrain_tree(next(o for o in originals.values()
                             if o.get("role", "").startswith("saved editor terrain")))
    removed = source_route_supports()
    assert removed.keys() <= originals.keys(), removed.keys() - originals.keys()
    retained = retain_pile_report(originals, removed, tree)
    built, paths = create_foundation(originals, removed, tree)
    for name in removed:
        bpy.data.objects.remove(originals[name], do_unlink=True)

    native_before = SOURCE / "fitted-approaches.hcs2"
    native_after = OUT / f"{STEM}.hcs2"
    glb_after = OUT / f"{STEM}.glb"
    blend_after = OUT / f"{STEM}.blend"
    bpy.ops.wm.save_as_mainfile(filepath=str(blend_after), compress=True)
    bpy.ops.object.select_all(action="DESELECT")
    for obj in built:
        obj.select_set(True)
    addition = ROOT / "build/wharf-foundation-addition.glb"
    bpy.ops.export_scene.gltf(filepath=str(addition), export_format="GLB",
                              export_extras=True, use_selection=True)
    append(SOURCE / "fitted-approaches.glb", addition, glb_after,
           remove_mesh_names=sorted(removed))
    parts = native_write(native_before, native_after, removed, built)
    prior = {n: raw for n, _, raw in split_native(native_before)}
    post = {n: raw for n, _, raw in split_native(native_after)}
    expected = {n: raw for n, raw in prior.items() if n.split(":", 1)[-1] not in removed}
    assert {n: post[n] for n in expected} == expected
    assert len(prior) - len(expected) == len(removed)
    assert len(post) == len(expected) + len(parts)

    bpy.ops.wm.open_mainfile(filepath=str(blend_after))
    saved_names = {o.name for o in bpy.context.scene.objects if o.type == "MESH"}
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.import_scene.gltf(filepath=str(glb_after))
    opened = {o.name: o for o in bpy.context.scene.objects if o.type == "MESH"}
    assert set(opened) == saved_names and not (removed.keys() & opened.keys())
    for name, triangles in parts:
        assert face_keys(records(opened[name])) == face_keys(triangles), name

    bpy.ops.object.camera_add()
    bpy.context.scene.camera = bpy.context.object
    captures = [
        render(OUT / "captures", "wharf-deck-and-shore.png",
               (32, -31, 0), (73, -18, 37), 66),
        render(OUT / "captures", "wharf-pier-pile-bays.png",
               (32, -12, -.4), (33, 8, 7), 32),
        render(OUT / "captures", "wharf-founded-undercroft.png",
               (33, -28, -1.8), (53, 3, -15), 40, hide_ground=True),
    ]
    write_json(OUT / "construction.json", dict(
        site="WLD-06", source_hashes={relative(p): h for p, h in inputs.items()},
        builder_sha256=sha(Path(__file__)), removed_guide_supports=removed,
        retained_grid_piles=retained, loads=paths, kept_collision_parts=len(expected),
        new_render_collision_parts=len(parts), captures=captures,
        exports={relative(p): sha(p) for p in (glb_after, blend_after, native_after)},
    ))
    print(f"PASS Wharf foundations with {len(retained)} verified retained piles, "
          f"{len(removed)} route-era parts removed and {len(parts)} new parts", flush=True)


if __name__ == "__main__":
    main()
