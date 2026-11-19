"""Rebuild the Dam's east stair bearings around its full lower service hall."""
import copy
import json
from pathlib import Path
import sys

import bpy
from mathutils import Vector
from mathutils.bvhtree import BVHTree

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/"tools"))
from append_static_glb import append, read
from build_coast_entrances import Structure
from build_college_grounds import import_scene, place_review_van
from build_dam_spillway_gallery import native_write, rel, validate_solid
from build_deliberate_entrances import write
from build_roster_foundations import material
from check_roster_decks import read_parts
from compose_room_terrain import split_native
from export_roster_collision import face_keys
from prepare_cemetery_grounds import BASE, VAN
from review_roster_macroterrain import records, sha

SITE = "WLD-09"
PASS = BASE/"dam-east-hall-pass"
BASELINE = BASE/"workyard-station-pass/roster.json"
OUT = PASS/SITE.lower()
CAPTURES = BASE/"captures/dam-east-hall-batch"
STEM = "east-hall-bearings"
STATUS = "DAM_EAST_HALL_WALL_BEARINGS_NATIVE_RUNTIME_OPEN"
# Wall and stair coordinates are the existing contained-hall architectural datums.
HALL = (64,22,70.5,48)
WELL = (65.9,25,68.1,45)
MIDDLE = (66.1,34.1,67.9,35.9)
FLOORS = (-6,-3,0)
AISLE = 69.25
TURN = 40.88
STAIR_GUIDE_WIDTH = 1.50
VIEWS = (
    ("overview",(90,-91,101),(38,-45,-2),112),
    ("turbine-to-east-hall",(62.35,-35,-4.38),(69.0,-35,-4.18),0),
    ("east-service-aisle",(69.35,-39.8,-4.38),(68.15,-33.65,-3.7),0),
    ("main-landing",(67,-33.35,-1.67),(67,-37.5,-.85),0),
    ("return-landing",(60.32,-40.68,-6.38),(62.55,-40.85,-6.30),0),
    ("spillway-door",(68,-44,-4.38),(68,-49,-4.25),0),
)
DOOR_CONTROLS = {
    "DAM-HALL-x-64-35--6",
    "DAM-HALL-z-48-68-0",
    "DAM-SPILLWAY-LOWER-68",
    "WLD-09-restart-shared turbine workshop north",
}


def bearings(s):
    steel = material("Dam sealed steel girder",(.29,.32,.34))
    concrete = material("Dam reinforced concrete bearing",(.43,.42,.40))
    names = ("middle landing side bearing","middle landing column","middle landing transverse bearing",
             "rim column","waist bearing")
    superseded = {name for name in s.old if name.startswith(tuple(SITE+" "+n for n in names))
                  and not name.endswith(" west")}
    assert len(superseded)==13,sorted(superseded)
    s.retired.update(superseded)

    def beam(label,bounds,bottom,top,mat=steel):
        return s.box(label,bounds,bottom,top,mat,"wall-seated structural bearing","east-lower-stair")

    # Both ends overlap the existing masonry. The west ledger is above the clear turbine door.
    beam("middle landing west transfer ledger",(64.03,33.15,64.46,36.85),-3.62,-3.30)
    beam("middle landing east wall seat",(70.18,33.95,70.46,36.05),-3.62,-3.30,concrete)
    for z in (34.34,35.66):
        beam(f"middle landing wall-to-wall girder Z{z:g}",(64.12,z-.14,70.43,z+.14),-3.62,-3.30)

    # These floor joists end on the retained side girders outside the rectangular stair well.
    for z in (31.5,38.5):
        beam(f"west rim wall-to-well joist Z{z:g}",(64.09,z-.18,65.82,z+.18),-.70,-.30)
        beam(f"east rim wall-to-well joist Z{z:g}",(68.18,z-.18,70.41,z+.18),-.70,-.30)

    # The lower stair is founded directly on its closed plinth. The upper waist bears at both hall walls.
    beam("lower flight saddle within concrete plinth",(66.1,31.32,67.9,31.68),-4.57,-4.21,concrete)
    beam("upper flight wall-to-wall bearing",(64.09,38.34,70.42,38.66),-2.86,-2.48)


def guidance(layout,meta):
    by_id = {row["id"]:row for row in layout["links"]}
    old_maps = copy.deepcopy(meta["source_segment_map"])
    old_routes = {}
    original_ascent = by_id["L11"]["points"]
    middle = [AISLE,-6,35]
    north = [AISLE,-6,23.5]
    turbine_door = [64,-6,35]
    ascent = [original_ascent[0],turbine_door,middle,north,*original_ascent[3:]]
    new_spillway = list(reversed(ascent[2:]))+[
        [AISLE,-6,46.4],[68,-6,46.4],[68,-6,48],[68,-6,50.3],[70.8,-6,50.3]]
    return_points = copy.deepcopy(by_id["L18"]["points"][:9])
    return_points[5][2] = return_points[6][2] = TURN
    routes = {
        "L09": list(reversed(by_id["L08"]["points"]))+ascent[1:],
        "L11": ascent,
        "L12": new_spillway,
        "L18": return_points+[[62.5,-6,35.0],turbine_door,*ascent[2:]],
    }
    all_points = [list(p) for p in layout["staging"]["cargo_route"]]
    assert all_points[0]==[36,-0.0,78.372] and all_points[-1]==[36,0,69]
    routes["L01"] = [all_points[0],[36,0,76],all_points[-1]]
    routes["L10"] = by_id["L10"]["points"]
    widths = {key:STAIR_GUIDE_WIDTH for key in ("L09","L11","L12","L18")}
    widths["L10"] = next(row["clear_width"] for row in layout["authored_doorways"]
                         if row["id"]=="DAM-HALL-z-48-68-0")
    for key,points in routes.items():
        link = by_id[key]
        old = copy.deepcopy(link)
        old_routes[key] = old
        link["points"] = copy.deepcopy(points)
        link["width"] = widths.get(key,link["width"])
        link["note"] = "Nonphysical guidance on the built service floors and through the measured openings."
        previous = {(tuple(old["points"][int(i)]),tuple(old["points"][int(i)+1])):n
                    for i,n in old_maps[key].items()}
        mapped = {i:previous[(tuple(a),tuple(b))]
                  for i,(a,b) in enumerate(zip(points,points[1:])) if a[1]!=b[1]}
        assert sorted(mapped.values())==sorted(previous.values()),key
        meta["source_segment_map"][key] = mapped
    layout["east_hall_bearings"] = {
        "hall":HALL,"floors":FLOORS,"well":WELL,"middle_landing":MIDDLE,
        "lower_service_aisle_x":AISLE,"guarded_guide_width":STAIR_GUIDE_WIDTH,
        "unobstructed_lower_headroom_under_transfer_m":2.38,
        "west_transfer_ledger":[64.03,33.15,64.46,36.85],
        "middle_transfer_girder_centres_z":[34.34,35.66],
        "rim_joists_z":[31.5,38.5],
        "return_turn_z":TURN,"van_ramp":all_points[0],
        "nonphysical_guidance":True,
    }
    layout["status"] = STATUS
    meta["before_segment_map"] = old_maps
    return old_routes


def build():
    roster = json.loads(BASELINE.read_bytes())
    row = next(entry for entry in roster["active_sources"] if entry["id"]==SITE)
    layout = json.loads((ROOT/row["layout"]).read_bytes())
    meta = json.loads((ROOT/row["construction"]).read_bytes())
    assert sha(ROOT/row["glb"])==row["glb_sha256"] and sha(ROOT/row["native"])==row["native_sha256"]
    objects = import_scene(ROOT/row["glb"])
    design = Structure("Dam east hall",objects)
    # This scope replaces specific bearings. No other study or heritage component is retired here.
    assert not design.strips
    bearings(design)
    old_guides = guidance(layout,meta)
    bpy.data.objects.remove(design.template,do_unlink=True)
    assert design.retired<=objects.keys()
    for name in design.retired:
        bpy.data.objects.remove(objects[name],do_unlink=True)
    laid = OUT/"authored-layout.json"
    write(laid,layout)
    glb,native,blend = (OUT/(STEM+ext) for ext in (".glb",".hcs2",".blend"))
    bpy.ops.wm.save_as_mainfile(filepath=str(blend),compress=True)
    bpy.ops.object.select_all(action="DESELECT")
    for obj in design.built:
        obj.select_set(True)
    addition = ROOT/"build/dam-east-hall-additions.glb"
    bpy.ops.export_scene.gltf(filepath=str(addition),export_format="GLB",export_extras=True,use_selection=True)
    append(ROOT/row["glb"],addition,glb,remove_mesh_names=sorted(design.retired))
    native_write(ROOT/row["native"],native,design.retired,design.built)
    meta.update(status=STATUS,source_glb=row["glb"],source_glb_sha256=row["glb_sha256"],
                source_physics=row["native"],source_physics_sha256=row["native_sha256"],
                before_layout=row["layout"],before_layout_sha256=sha(ROOT/row["layout"]),
                before_stair_components=list(meta["stair_components"]),layout=rel(laid),layout_sha256=sha(laid),
                removed_source_meshes=sorted(design.retired),new_interface_parts=[obj.name for obj in design.built],
                east_hall_bearings=layout["east_hall_bearings"],builder=rel(Path(__file__)),
                builder_sha256=sha(Path(__file__)),glb_sha256=sha(glb),physics_sha256=sha(native),blend_sha256=sha(blend))
    write(OUT/"construction.json",meta)
    write(OUT/"quarantine.json",dict(source=row,retired_structural_parts=sorted(design.retired),
                                     superseded_guides=old_guides))
    new = dict(row,construction=rel(OUT/"construction.json"),layout=rel(laid),glb=rel(glb),native=rel(native),blend=rel(blend),
               glb_sha256=sha(glb),native_sha256=sha(native))
    roster["active_sources"] = [new if entry["id"]==SITE else entry for entry in roster["active_sources"]]
    next(entry for entry in roster["sites"] if entry["id"]==SITE)["decision"] = STATUS
    roster["baseline"] = rel(BASELINE)
    write(PASS/"roster.json",roster)


def render(layout,views,source_sha,suffix=""):
    van = place_review_van(layout["staging"])
    scene = bpy.context.scene
    scene.render.engine = "BLENDER_WORKBENCH"
    scene.display.shading.color_type = "MATERIAL"
    scene.display.shading.show_cavity = True
    scene.render.resolution_x,scene.render.resolution_y = 1200,850
    bpy.ops.object.camera_add()
    cam = bpy.context.object
    scene.camera = cam
    CAPTURES.mkdir(parents=True,exist_ok=True)
    files = []
    for label,pos,target,scale in views:
        cam.data.type = "ORTHO" if scale else "PERSP"
        cam.data.ortho_scale = scale or 1
        cam.data.lens = 21
        cam.location = pos
        cam.rotation_euler = (Vector(target)-cam.location).to_track_quat("-Z","Y").to_euler()
        image = CAPTURES/f"{label}{suffix}.png"
        scene.render.filepath = str(image)
        bpy.ops.render.render(write_still=True)
        files.append(dict(path=rel(image),sha256=sha(image),source_glb_sha256=source_sha))
    return files,van


def review():
    layout = json.loads((OUT/"authored-layout.json").read_bytes())
    meta = json.loads((OUT/"construction.json").read_bytes())
    glb,native = (OUT/(STEM+extension) for extension in (".glb",".hcs2"))
    old,old_buffer = read(ROOT/meta["source_glb"])
    current,new_buffer = read(glb)
    assert new_buffer[:len(old_buffer)]==old_buffer
    for field in ("meshes","materials","accessors","bufferViews","textures","images"):
        assert current.get(field,[])[:len(old.get(field,[]))]==old.get(field,[]),field
    retired = set(meta["removed_source_meshes"])
    for index,node in enumerate(old["nodes"]):
        expected = dict(node)
        if node.get("name") in retired:
            expected.pop("mesh",None)
        assert current["nodes"][index]==expected,node.get("name")
    retained = [part for part in split_native(ROOT/meta["source_physics"]) if part[0].split(":",1)[-1] not in retired]
    actual = split_native(native)
    assert actual[:len(retained)]==retained
    terrain = [part for part in actual if part[0].startswith("terrain-")]
    assert terrain==split_native(ROOT/meta["saved_world"]/"terrain-physics.hcs2")
    objects = import_scene(glb)
    physical = read_parts(native)
    for name in meta["new_interface_parts"]:
        obj = objects[name]
        validate_solid(obj)
        assert face_keys(records(obj))==face_keys(physical[name][1]),name
    vertices,faces = [],[]
    for obj in objects.values():
        start = len(vertices)
        vertices.extend(obj.matrix_world @ v.co for v in obj.data.vertices)
        faces.extend(tuple(start+i for i in face.vertices) for face in obj.data.polygons)
    tree = BVHTree.FromPolygons(vertices,faces)

    def ray(a,b):
        a,b = Vector(a),Vector(b)
        return tree.ray_cast(a,(b-a).normalized(),(b-a).length)[0]

    def stand(x,y,z,head=1.91):
        hit = ray((x,-z,y+.08),(x,-z,y-.15))
        assert hit is not None and abs(hit.z-y)<.052,((x,y,z),hit)
        assert ray((x,-z,y+.081),(x,-z,y+head)) is None,((x,y,z),head)

    door_rows = [row for row in layout["authored_doorways"] if row["id"] in DOOR_CONTROLS]
    assert len(door_rows)==len(DOOR_CONTROLS)
    apertures,sills = 0,0
    for door in door_rows:
        position,centre,y,width = (door[k] for k in ("boundary","along","sill_y","clear_width"))
        for lateral in (centre-width*.30,centre,centre+width*.30):
            for h in (.3,1.21,door["clear_height"]-.12):
                a,b = ((position-.41,-lateral,y+h),(position+.41,-lateral,y+h)) if door["axis"]=="x" else ((lateral,-position-.41,y+h),(lateral,-position+.41,y+h))
                assert ray(a,b) is None,(door["id"],a,b)
                apertures += 1
            for delta in (-.28,.28):
                x,z = (position+delta,lateral) if door["axis"]=="x" else (lateral,position+delta)
                stand(x,y,z)
                sills += 1

    # Wearer clearance at the entire cross-bay transfer, on the two landings and down the east aisle.
    downstairs = [(x,-6,z) for x in (64.7,65.78,66.8,67.7,68.22,69.6)
                  for z in (34.34,35,35.66)]
    lower_aisle = [(x,-6,z) for x in (68.85,69.28,69.72) for z in (25.2,31.5,34.25,35.75,38.5,45.5)]
    landings = [(x,-3,z) for x in (66.55,67,67.45) for z in (34.32,35,35.65)]
    landings += [(x,-8,z) for x in (60.3,61.4,62.5) for z in (40.6,41,41.24)]
    for x,y,z in downstairs+lower_aisle:
        stand(x,y,z,2.30)
    for p in landings:
        stand(*p)
    captures,van = render(layout,VIEWS,meta["glb_sha256"])
    import_scene(ROOT/meta["source_glb"])
    earlier,previous_van = render(layout,VIEWS[1:3],meta["source_glb_sha256"],"-before")
    assert van==previous_van
    write(OUT/"export-verification.json",dict(
        glb_sha256=sha(glb),native_sha256=sha(native),retained_native_records=len(retained),
        retained_terrain_records=len(terrain),new_closed_parts=len(meta["new_interface_parts"]),
        aperture_rays=apertures,sill_controls=sills,headroom_2_30_controls=len(downstairs+lower_aisle),
        landing_standing_controls=len(landings),canonical_van_triangles=van,
        canonical_van_source=sha(VAN/"geometry.json"),captures=captures+earlier))
    print("DAM HALL REVIEW",apertures,sills,len(downstairs+lower_aisle),len(landings),flush=True)


if __name__=="__main__":
    if "--review-only" in sys.argv:
        review()
    else:
        build()
