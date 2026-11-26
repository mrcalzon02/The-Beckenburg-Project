"""Build a founded landward enclosure for the Wharf's working shore."""
import json
import math
from pathlib import Path
import shutil
import sys

import bpy
from mathutils import Vector

ROOT = Path(__file__).resolve().parents[1]
sys.path[:0] = [str(ROOT / "tools"), str(ROOT / "development/levels/blockouts")]

from append_static_glb import append
from build_circulation_construction import native_write
from build_college_containment import MeshParts, post_foot, station, strip
from build_roster_foundations import material
from build_wharf_foundations import relative, terrain_tree, ground, write_json
from compose_room_terrain import split_native
from export_roster_collision import face_keys
from review_roster_macroterrain import records, sha

BASE = ROOT / "development/levels/rebuild"
SOURCE = BASE / "wharf-tidal-pass"
OUT = BASE / "wharf-landward-pass"
STEM = "enclosed-wharf"
WEST = ((4.42,54.23),(5.6,55.1),(6.8,58.2),(8.95,60.76),(11.1,62.55),
        (14.7,64.7),(17.35,66.45),(20.6,68.95),(20.8,75.0),(21.1,81.3),
        (22.15,83.45),(28,83.45))
EAST = ((36,83.45),(41.9,83.45),(43.15,81.3),(43.25,75.0),(43.45,68.9),
        (46.25,66.3),(49.25,64.45),(52.95,62.3),(55.1,59.75),(58.15,56.85),
        (59.7,54.78),(59.58,54.23))
ROAD_Z = 83.45
BOUNDARY = WEST + EAST


def build_boundary(tree):
    stone = material("Wharf grey harbour stone", (.31,.32,.28))
    cap = material("Wharf pale dressed coping", (.47,.46,.39))
    iron = material("Wharf dulled teal ironwork", (.135,.22,.225))
    hardware = material("Wharf aged hinge and head fittings", (.24,.22,.18))
    materials = (stone, cap, iron, hardware)
    built = []
    survey = []

    for a, b in zip(BOUNDARY, BOUNDARY[1:]):
        if a == WEST[-1]:
            assert b == EAST[0]
            continue
        name = "Wharf landward retaining span " + str(len(survey) + 1)
        mesh = MeshParts(materials)
        length = math.dist(a,b)
        dx, dz = (b[0]-a[0])/length, (b[1]-a[1])/length
        normal = (dz,-dx)
        count = math.ceil(length / .20)
        values = []
        for j in range(count+1):
            x,z = station(a,b,j/count)
            sample = [ground(tree,x+side*normal[0],z+side*normal[1])
                      for side in (-.20,0,.20)]
            assert all(y is not None for y in sample), (name, x, z, sample)
            values.extend(sample)
        low = min(values)-.25
        high = max(values)
        e0 = (a[0] - dx*.04, a[1] - dz*.04)
        e1 = (b[0] + dx*.04, b[1] + dz*.04)
        mesh.prism(strip(e0,e1,.39),low,high+.86,0)
        mesh.prism(strip(e0,e1,.43),high+.845,high+.955,1)
        for gap in (.98,1.86):
            mesh.prism(strip(e0,e1,.052), high+gap, high+gap+.065, 2)
        stations = math.ceil(length / .21)
        for j in range(1,stations):
            p = station(a,b,j/stations)
            mesh.prism(post_foot(p,.025), high+.91, high+1.865, 2)
        for p in (a,b):
            if p in (WEST[-1], EAST[0], WEST[0], EAST[-1]):
                continue
            mesh.prism(post_foot(p,.092), high+.68, high+1.99, 2)
        built.append(mesh.frame(name,"grounded stone retaining wall with fitted iron upper guard"))
        survey.append(dict(name=name, a=a, b=b, outward=[-normal[0],-normal[1]],
                           ground_y_min=min(values), ground_y_max=high, footing_bottom_y=low,
                           ground_probes=len(values), guard_crest_y=high+1.925))

    # Bridge the last few centimetres to the closed walls near the cottage
    # corners. The rear thresholds are well clear of these bonded returns.
    returns = MeshParts(materials)
    for x in (4.42,59.58):
        outline = [(x-.24,53.86),(x+.24,53.86),(x+.24,54.48),(x-.24,54.48)]
        bed = min(ground(tree,x+.14,54.43), ground(tree,x-.14,54.43))
        returns.prism(outline,bed-.3,1.87,0)
        returns.prism(outline,1.855,1.96,1)
    built.append(returns.frame("Wharf bonded cottage boundary returns","founded boundary to existing cottage walls"))

    gate_piers = MeshParts(materials)
    for x in (28.,36.):
        p = (x,ROAD_Z)
        corners = post_foot(p,.62)
        soils = [ground(tree,*q) for q in corners]
        assert all(y is not None and abs(y)<.018 for y in soils), (p,soils)
        gate_piers.prism(corners,min(soils)-.36,2.22,0)
        gate_piers.prism(post_foot(p,.74),2.21,2.32,1)
    gate_piers.prism(strip((28.,ROAD_Z),(36.,ROAD_Z),.46),-.19,-.012,0)
    built.append(gate_piers.frame("Wharf landward gate piers and sill","founded masonry and road gate sill"))

    for name, x0, x1, hinge in (("west",28.21,31.989,28.23),("east",32.011,35.79,35.77)):
        leaf = MeshParts(materials)
        a,b = (x0,ROAD_Z),(x1,ROAD_Z)
        for low, top, width in ((.105,.23,.087),(.91,1.01,.065),(1.96,2.045,.085)):
            leaf.prism(strip(a,b,width),low,top,2)
        for x in (a[0]+.02,b[0]-.02):
            leaf.prism(post_foot((x,ROAD_Z),.072),.105,2.055,2)
        count = math.ceil((x1-x0)/.19)
        for i in range(1,count):
            p=station(a,b,i/count)
            leaf.prism(post_foot(p,.029),.165,2.02,2)
        for y in (.29,1.83):
            leaf.prism(post_foot((hinge,ROAD_Z),.175),y,y+.108,3)
        latch = x1-.09 if name=="west" else x0+.09
        leaf.prism(post_foot((latch,ROAD_Z),.13),.93,1.12,3)
        built.append(leaf.frame("Wharf shut fixed "+name+" road gate leaf",
                                "fixed closed landward equipment gate leaf"))
    return built, survey


def surrounding_bank():
    # Beyond the walls, the shore rises into scrub on either side of the
    # old lane. The new land starts below the saved shore and carries the
    # view past both shoulders and behind the closed gate.
    sections = (
        (50,32,-2.15,0),(54,32,-.89,0),(62,32,-.76,0),(74,32,-.72,0),
        (82,32,-.62,0),(86,32.2,-.48,.1),(88,33,-.39,.3),(92,35,-.32,.8),
        (98,40,-.04,1.4),(107,45,.5,2.4),(118,49,1.1,3.7),(134,44,2.3,5.2),(160,36,4.,7.1),
    )
    offsets=(-101,-66,-42,-32,-25,-18,-10,-4,-3,0,3,4,10,18,25,32,42,66,101)
    columns=len(offsets)
    points=[]
    for z, lane, base, rise in sections:
        for offset in offsets:
            distance=abs(offset)
            hill=max(0.,min(1.,(distance-4)/34))
            side=max(0.,min(1.,(distance-14)/48))
            elevation=base+rise*hill+side*.26*math.sin(z*.11+offset*.19)
            points.append((lane+offset,-z,elevation))
    faces=[]
    surfaces=[]
    for row in range(len(sections)-1):
        for col in range(columns-1):
            a=row*columns+col
            b,d=a+1,a+columns
            choice=int(sections[row][0]>=82 and -3<=offsets[col] and offsets[col+1]<=3)
            faces.extend(((a,d,b),(b,d,d+1)))
            surfaces.extend((choice,choice))
    name="Wharf rising landward bank and continuing road"
    mesh=bpy.data.meshes.new(name)
    mesh.from_pydata(points,[],faces)
    mesh.update()
    mesh.materials.append(material("Wharf landward grass and shale",(.235,.263,.203)))
    mesh.materials.append(material("Wharf closed road continuation gravel",(.35,.345,.31)))
    uv=mesh.uv_layers.new(name="Harbor ground metres")
    for poly,index in zip(mesh.polygons,surfaces):
        assert poly.normal.z>0
        poly.material_index=index
        for li in poly.loop_indices:
            vertex=mesh.vertices[mesh.loops[li].vertex_index].co
            uv.data[li].uv=(vertex.x*.6,-vertex.y*.6)
    bank=bpy.data.objects.new(name,mesh)
    bank["role"]="continuous non-accessible landward context beyond the parcel boundary"
    bpy.context.scene.collection.objects.link(bank)
    return bank


def inspection(glb):
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.import_scene.gltf(filepath=str(glb))
    bpy.ops.object.camera_add()
    cam = bpy.context.object
    cam.location = Vector((58,-101,34))
    cam.rotation_euler = (Vector((32,-66,.4))-cam.location).to_track_quat("-Z","Y").to_euler()
    cam.data.type = "ORTHO"
    cam.data.ortho_scale = 65
    scene = bpy.context.scene
    scene.camera = cam
    scene.render.engine="BLENDER_WORKBENCH"
    scene.display.shading.color_type="MATERIAL"
    scene.display.shading.show_cavity=True
    scene.display.shading.cavity_type="BOTH"
    scene.render.resolution_x, scene.render.resolution_y = 1520, 960
    scene.render.resolution_percentage=100
    capture = OUT / "captures/structure/wharf-landward-enclosure.png"
    capture.parent.mkdir(parents=True,exist_ok=True)
    scene.render.filepath=str(capture)
    bpy.ops.render.render(write_still=True)
    return capture


def main():
    checkpoint = json.loads((SOURCE/"checkpoint.json").read_bytes())
    assert all(sha(ROOT/f)==h for f,h in checkpoint["exports"].items())
    previous = SOURCE/"tidal-wharf"
    OUT.mkdir(parents=True,exist_ok=True)
    bpy.ops.wm.open_mainfile(filepath=str(previous.with_suffix(".blend")))
    bpy.context.preferences.filepaths.save_version=0
    terrain = next(o for o in bpy.context.scene.objects
                   if o.get("role","").startswith("saved editor terrain"))
    built, survey = build_boundary(terrain_tree(terrain))
    bank = surrounding_bank()
    built.append(bank)
    export = OUT/STEM
    bpy.ops.wm.save_as_mainfile(filepath=str(export.with_suffix(".blend")), compress=True)
    bpy.ops.object.select_all(action="DESELECT")
    for obj in built:
        obj.select_set(True)
    fragment = ROOT/"build/wharf-landward-addition.glb"
    bpy.ops.export_scene.gltf(filepath=str(fragment), export_format="GLB",
                              use_selection=True,export_extras=True)
    append(previous.with_suffix(".glb"),fragment,export.with_suffix(".glb"))
    changed = native_write(previous.with_suffix(".hcs2"),export.with_suffix(".hcs2"),set(),built,open_components={bank.name})
    previous_native = split_native(previous.with_suffix(".hcs2"))
    collision = split_native(export.with_suffix(".hcs2"))
    assert collision[:len(previous_native)] == previous_native
    assert len(collision)-len(previous_native)==len(changed)

    bank_name = bank.name
    capture = inspection(export.with_suffix(".glb"))
    for name, expected in changed:
        assert face_keys(records(bpy.context.scene.objects[name])) == face_keys(expected),name
    water = OUT/"wharf-tide.water"
    shutil.copyfile(SOURCE/water.name,water)
    outputs=[export.with_suffix(ext) for ext in (".blend",".glb",".hcs2")]+[water]
    write_json(OUT/"construction.json",dict(site="WLD-06",roster_authority=relative(SOURCE/"roster.json"),
        preserved_previous=checkpoint["exports"], builder_sha256=sha(Path(__file__)),
        landward_alignment=BOUNDARY, road_gate_reservation=[28.,ROAD_Z,36.,ROAD_Z],
        constructed_spans=survey, founded_spans_probes=sum(r["ground_probes"] for r in survey),
        asset_fit={"gate":"site-fitted iron frame and masonry piers","activation":"fixed closed boundary"},
        landward_context=bank_name, added_parts=len(changed), retained_native_parts=len(previous_native),
        shoreline_diagnostic=dict(path=relative(capture),sha256=sha(capture)),
        exports={relative(path):sha(path) for path in outputs}))
    print("PASS Wharf landward enclosure, founded spans and exact native export",flush=True)


if __name__ == "__main__":
    main()
