"""Fit the Wharf's lower surveyed shore and tidal water to the existing dock."""
from pathlib import Path
import json
import math
import sys

import bpy
from mathutils import Vector

ROOT = Path(__file__).resolve().parents[1]
sys.path[:0] = [str(ROOT / "tools"), str(ROOT / "development/levels/blockouts")]

from append_static_glb import append
from build_circulation_construction import native_write
from build_roster_foundations import material
from build_wharf_foundations import relative, terrain_tree, ground, write_json
from compose_room_terrain import split_native
from export_roster_collision import face_keys
from review_roster_macroterrain import records, sha

BASE = ROOT / "development/levels/rebuild"
SOURCE = BASE / "wharf-foundation-pass"
OUT = BASE / "wharf-tidal-pass"
STEM = "tidal-wharf"
TIDE_Y = -1.45

# Soundings from the side of the retained quay. Use a lower surface so the
# original geography still owns the coast where its saved faces are present.
SECTIONS = (2, 24, 34, 36, 38, 41, 44, 46, 48, 50, 52, 54, 56, 58)
WATER_BOUNDS = (-169., -158., 233., 54.)
DRY_BANDS = (
    (24.125, 31.575, 24.125, 39.875),
    (31.575, 34.425, 16.125, 47.875),
    (34.425, 35.875, 24.125, 39.875),
)


def loft_bed(terrain):
    tree = terrain_tree(terrain)
    # Profile bearings use terrain outside the punched architectural cells.
    # Keep the original cut and continue only the soil beneath its floor.
    profile = []
    for z in SECTIONS:
        references = [ground(tree, x, z) for x in (20.65, 21.35, 42.65, 43.35)]
        references = [height for height in references if height is not None]
        if not references:
            references = [ground(tree, 14.0, z), ground(tree, 52.0, z)]
        assert all(height is not None for height in references) and references
        low = min(references)
        bed_y = min(-.36, low - .17)
        if z <= 36:
            bed_y = min(bed_y, -4.48 if z <= 34 else -3.89)
        profile.append((z, round(bed_y, 4), references))

    y_stations = [(-153, -11.5), (-75, -8.3), (-26, -6.1), (-6, -5.1)]
    y_stations.extend((z, bed_y) for z, bed_y, _ in profile)
    x_stations = (-157, -80, -39, -12, -2, 6, 14, 20, 24, 28, 32, 36, 40,
                  44, 50, 58, 66, 76, 104, 155, 222)
    width = len(x_stations)
    verts = []
    for z, y in y_stations:
        for x in x_stations:
            coast = max(0., 1. - abs(x - 32.) / 54.)
            off_axis = max(0., abs(x - 32.) - 31.)
            crossfall = min(.92, off_axis * .013)
            ripple = min(.06, max(0., (43 - z) * .004)) * math.sin(x * .39 + z * .23)
            outer_fall = 3.7 * (1. - coast) if z >= 48 else (1. - coast) * .28
            verts.append((float(x), -float(z), y - crossfall - outer_fall + ripple))
    faces = []
    for row in range(len(y_stations) - 1):
        for col in range(width - 1):
            a = row * width + col
            b, d = a + 1, a + width
            c = d + 1
            if (row + col) % 2:
                faces.extend(((a, d, b), (b, d, c)))
            else:
                faces.extend(((a, d, c), (a, c, b)))

    name = "Wharf continuous lower tidal bed and shore batter"
    mesh = bpy.data.meshes.new(name)
    mesh.from_pydata(verts, [], faces)
    mesh.update()
    for label, color in (("Harbour shale and marine silt", (.11, .135, .125)),
                         ("Tidal sand and cobble", (.35, .32, .26))):
        mesh.materials.append(material(label, color))
    uv = mesh.uv_layers.new(name="Tidal metre coordinates")
    for poly in mesh.polygons:
        poly.material_index = int(poly.center.y < -40)
        assert poly.normal.z > 0
        for li in poly.loop_indices:
            vertex = mesh.vertices[mesh.loops[li].vertex_index].co
            uv.data[li].uv = (vertex.x * .65, -vertex.y * .65)
    obj = bpy.data.objects.new(name, mesh)
    obj["role"] = "continuous submerged estuary terrain below saved shore geography"
    bpy.context.scene.collection.objects.link(obj)
    return obj, profile


def water_document(path):
    xmin, zmin, xmax, zmax = WATER_BOUNDS
    tiles = [(xmin, zmin, xmax, DRY_BANDS[0][0])]
    for z0, z1, dry_x0, dry_x1 in DRY_BANDS:
        tiles.extend(((xmin, z0, dry_x0, z1), (dry_x1, z0, xmax, z1)))
    tiles.append((xmin, DRY_BANDS[-1][1], xmax, zmax))

    settings = {"bed.depth": 5.8, "wave.height": .055, "wave.grid": 1.6,
                "wave.speed": .35, "shore.height": .022,
                "contact.enabled": "false", "spray.enabled": "false"}
    rows = ["version=1", "level=wld-06", f"count={len(tiles)}"]
    for i, (x0, z0, x1, z1) in enumerate(tiles):
        prefix = f"water.{i}."
        fields = {"id": f"wharf-tide-{i}", "shape": "SQUARE", "x": (x0+x1)/2,
                  "y": TIDE_Y, "z": (z0+z1)/2, "width": x1-x0, "depth": z1-z0, "yaw": 0}
        rows.extend(f"{prefix}{key}={value}" for key, value in fields.items())
        rows.extend(f"{prefix}setting.{key}={value}" for key, value in settings.items())
    path.write_bytes(("\n".join(rows) + "\n").encode())
    return tiles


def inspection_view(tiles):
    """Reopened export with the exact mean tide drawn for construction review."""
    surface = material("Mean tide inspection surface", (.13, .26, .31))
    for i, (x0, z0, x1, z1) in enumerate(tiles):
        name = f"Diagnostic mean tide tile {i}"
        mesh = bpy.data.meshes.new(name)
        mesh.from_pydata([(x0,-z0,TIDE_Y),(x0,-z1,TIDE_Y),
                          (x1,-z1,TIDE_Y),(x1,-z0,TIDE_Y)], [], [(0,1,2,3)])
        mesh.materials.append(surface)
        bpy.context.scene.collection.objects.link(bpy.data.objects.new(name,mesh))
    bpy.ops.object.camera_add()
    camera = bpy.context.object
    camera.location = Vector((66, -3, 31))
    camera.rotation_euler = (Vector((30, -28, 0)) - camera.location).to_track_quat("-Z","Y").to_euler()
    camera.data.type = "ORTHO"
    camera.data.ortho_scale = 78
    scene = bpy.context.scene
    scene.camera = camera
    scene.render.engine = "BLENDER_WORKBENCH"
    shading = scene.display.shading
    shading.color_type = "MATERIAL"
    shading.show_cavity = True
    shading.cavity_type = "BOTH"
    scene.render.resolution_x, scene.render.resolution_y = 1560, 980
    scene.render.resolution_percentage = 100
    path = OUT / "captures/structure/wharf-estuary-and-shore.png"
    path.parent.mkdir(parents=True, exist_ok=True)
    scene.render.filepath = str(path)
    bpy.ops.render.render(write_still=True)
    return dict(path=relative(path), sha256=sha(path),
                type="Native export with diagnostic daylight mean-tide planes")


def main():
    source = SOURCE / "supported-wharf"
    previous = json.loads((SOURCE / "checkpoint.json").read_bytes())
    assert all(sha(ROOT / name) == expected for name, expected in previous["exports"].items())
    OUT.mkdir(parents=True, exist_ok=True)
    bpy.ops.wm.open_mainfile(filepath=str(source.with_suffix(".blend")))
    bpy.context.preferences.filepaths.save_version = 0
    terrain = next(obj for obj in bpy.context.scene.objects
                   if obj.get("role", "").startswith("saved editor terrain"))
    bed, profile = loft_bed(terrain)
    export = OUT / STEM
    bpy.ops.wm.save_as_mainfile(filepath=str(export.with_suffix(".blend")), compress=True)
    bpy.ops.object.select_all(action="DESELECT")
    bed.select_set(True)
    fragment = ROOT / "build/wharf-lower-shore.glb"
    bpy.ops.export_scene.gltf(filepath=str(fragment), export_format="GLB",
                              use_selection=True, export_extras=True)
    append(source.with_suffix(".glb"), fragment, export.with_suffix(".glb"))
    new_parts = native_write(source.with_suffix(".hcs2"), export.with_suffix(".hcs2"),
                             set(), [bed], open_components={bed.name})
    before = split_native(source.with_suffix(".hcs2"))
    after = split_native(export.with_suffix(".hcs2"))
    assert len(after) == len(before) + 1 and after[:-1] == before

    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.import_scene.gltf(filepath=str(export.with_suffix(".glb")))
    for name, expected in new_parts:
        assert face_keys(records(bpy.context.scene.objects[name])) == face_keys(expected), name

    water = OUT / "wharf-tide.water"
    tiles = water_document(water)
    inspection = inspection_view(tiles)
    exports = [export.with_suffix(ext) for ext in (".blend", ".glb", ".hcs2")] + [water]
    write_json(OUT / "construction.json", dict(
        site="WLD-06", source_exports=previous["exports"], builder_sha256=sha(Path(__file__)),
        retained_native_parts=len(before), new_native_triangles=len(new_parts[0][1]),
        mean_tide_y=TIDE_Y, sounding_order="Z, new ground Y, retained ground samples",
        lower_bed_stations=profile, dry_cell_bands=DRY_BANDS,
        shared_water_placements=tiles, construction_capture=inspection, exports={relative(p): sha(p) for p in exports},
    ))
    print("PASS retained terrain and collision, revised estuary below the pier grid", flush=True)


if __name__ == "__main__":
    main()
