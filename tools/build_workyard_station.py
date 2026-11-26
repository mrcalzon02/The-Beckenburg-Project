"""Author founded workyards and real door approaches for Greywater and Gallows Pass."""
import copy
import json
from pathlib import Path
import sys

import bmesh
import bpy
from mathutils import Vector
from mathutils.bvhtree import BVHTree

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/"tools"))
from append_static_glb import append, read
from build_canyon_marsh import profile_wall
from build_cemetery_grounds import rel, replace_terrain_on_export, terrain_surface
from build_coast_entrances import Structure
from build_college_grounds import import_scene, place_review_van
from build_deliberate_entrances import write
from build_roster_foundations import material
from check_roster_decks import read_parts
from compose_room_terrain import split_native
from export_roster_collision import face_keys, native_faces
from prepare_cemetery_grounds import VAN
from prepare_workyard_station import (BASE, BASELINE, CANNERY, COURTS, FIELDS, LOWER_LANDING, PASS, RAILWAY, SITES,
                                       SOURCE_WORLDS, program, saved_world)
from review_roster_macroterrain import records, sha

STEM = "workyard-station"
CAPTURES = BASE/"captures/workyard-station-batch"
STATUS = "FOUNDED_WORKYARD_AND_STATION_APPROACHES_RUNTIME_OPEN"
TITLES = {CANNERY:"Greywater Cannery",RAILWAY:"Gallows Pass Railway"}
# The prior full wall names and room faces define the new openings. Keep portal identities.
FACADES = {
    CANNERY: (
        ("WLD-13-x--8-53","bunk","forest","W",(-8,44,62),53,1.42),
        ("WLD-13-x--8-35","mess","forest","W",(-8,26,44),35,1.42),
    ),
    RAILWAY: (
        ("WLD-12-z-76-20-y0","signal","entry","S",(76,12,28),20,1.52),
    ),
}
WOODLAND_PROFILE = ((35,.49),(38,.70),(41,.75),(45,.72),(49,.84),(51,.81),(53,.70))
CAMERAS = {
    CANNERY: (
        ("overview",(36,-90,91),(23,-46,1),119),
        ("forecourts",(-30,-63,23),(-7,-45,.5),45),
        ("bunkhouse-door",(-13,-53,1.62),(-7,-53,1.69),0),
        ("mess-door",(-13,-35,1.62),(-7,-35,1.69),0),
    ),
    RAILWAY: (
        ("overview",(63,-106,96),(35,-54,0),126),
        ("forecourt",(50,-101,21),(32,-78,0),54),
        ("signal-hut-door",(20,-81.65,1.62),(20,-75,1.6),0),
        ("siding-court",(69,-85,17),(54,-72,0),34),
        ("lower-landing",(32.27,-35.55,-1.38),(30.77,-38.25,-1.57),0),
    ),
}


def build_yards(site, structure, ground):
    stone = material(TITLES[site]+" foundation stone",(.40,.376,.327))
    structure.retired.update(name for name in structure.old
                             if any(name.startswith(f"{site} clean {room} {side} ")
                                    or name==old+" indexed jambs and head"
                                    for old,room,_,side,_,_,_ in FACADES[site]))
    for label,shape,bed,zone in COURTS[site]:
        structure.part(label,[shape],bed,-.005,stone,"founded full-width entrance court",zone)
    if site==CANNERY:
        shoulders = {name for name in structure.old if name.startswith(
            ("Retaining shoulder WLD-13 L17 2 -1 ", "Retaining shoulder WLD-13 L18 0 -1 "))}
        assert len(shoulders)==10
        structure.retired.update(shoulders)
        profile_wall(structure,"woodland retaining wall",-14.9,-14.49,WOODLAND_PROFILE,-.9,stone,"forest")
    else:
        west,north,east,south = LOWER_LANDING
        structure.box("lower landing south retaining wall",(west,south-.24,east,south),-3.3,-.25,stone,
                      "trestle landing retaining structure","lower")
        structure.box("lower landing east retaining wall",(east-.20,north,east,south-.24),-3.3,-.25,stone,
                      "trestle landing retaining structure","lower")
    for old,room,outside,side,(axis_at,lo,hi),centre,width in FACADES[site]:
        assert old+" indexed jambs and head" in structure.retired
        label = f"{room} {side.lower()} exterior doorway"
        axis = "x" if side=="W" else "z"
        structure.face(room+" exterior facade",axis,axis_at,(lo,hi),0,3,stone,
                       [(label,centre,width,2.2,[room,outside])],room)
        portal = structure.portals[-1]
        portal.update(id=old,label=label,side=side,door_type="pedestrian",
                      frame_span=[axis_at-.12,axis_at+.12],frame_depth=.24)
        portal["boundary_center"] = [axis_at,0,centre] if axis=="x" else [centre,0,axis_at]
        portal["frame_center"] = list(portal["boundary_center"])

    # Coincident grade faces take the same paving finish. Walkable triangles stay exact.
    mesh = ground.data
    index = len(mesh.materials)
    mesh.materials.append(stone)
    edges = [tuple(zip(shape,shape[1:]+shape[:1])) for _,shape,_,_ in COURTS[site]]
    for face in mesh.polygons:
        x,back,y = face.center
        if abs(y)>.025:
            continue
        z = -back
        if any(sum((a[1]>z)!=(b[1]>z) and x<a[0]+(z-a[1])*(b[0]-a[0])/(b[1]-a[1])
                   for a,b in boundary)%2 for boundary in edges):
            face.material_index = index


def crosses_door(link, door):
    axis = 0 if door["axis"]=="x" else 2
    other = 2-axis
    at,centre,level = (door[k] for k in ("boundary","along","sill_y"))
    for a,b in zip(link["points"],link["points"][1:]):
        run = b[axis]-a[axis]
        if not run or max(abs(a[1]-level),abs(b[1]-level))>.01:
            continue
        t = (at-a[axis])/run
        if 0<=t<=1 and abs(a[other]+t*(b[other]-a[other])-centre)<.07:
            return True
    return False


def derive_guidance(site, structure, layout):
    staging = layout["staging"]
    links = {row["id"]:row for row in layout["links"]}
    old_doors = layout["authored_doorways"]
    updated = {row["id"]:row for row in structure.portals}
    assert set(updated)<={row["id"] for row in old_doors}
    doors = [updated.get(row["id"],row) for row in old_doors]
    layout["authored_doorways"] = doors
    changed = {}
    ramp_x, ramp_y, ramp_z = staging["cargo_route"][0]

    def guide(number, points):
        key = f"L{number:02}"
        link = links[key]
        changed[key] = copy.deepcopy(link)
        link["points"] = [[x,0,z] for x,z in points]
        link["note"] = "Nonphysical guidance across the saved grade and through measured openings."
        link.pop("threshold_reservation",None)

    if site==CANNERY:
        guide(1,[(ramp_x,ramp_z),(32,76),(32,69),(34,69)])
        guide(17,[(0,53),(-8,53),(-11.8,53),(-11.8,44)])
        guide(18,[(-11.8,44),(-11.8,35),(-8,35),(0,35)])
        checks = [(-11.8,44),(-11.8,35),(-11.8,53),(-12.9,34.5),(-12.9,52.5),(34,69)]
        cargo = links["L01"]["points"]+links["L02"]["points"][1:-1]
    else:
        guide(1,[(ramp_x,ramp_z),(36,88),(36,82)])
        guide(2,[(36,82),(29.4,80.9),(24.1,79.25),(20,78.08),(20,76),(20,69)])
        guide(4,[(36,82),(43,80.5),(48.6,78.1),(54.5,76.75),(57,75.4),(57,53)])
        checks = [(36,82),(29.4,80.9),(20,78.08),(48.6,78.1),(57,75.4),(57,53)]
        cargo = links["L01"]["points"]+links["L02"]["points"][1:-1]
    staging["cargo_route"] = cargo
    assert staging["cargo_route"][0] == [ramp_x,ramp_y,ramp_z]
    layout["arrival_end_bearing"] = dict(guide="L01",ramp=[ramp_x,ramp_y,ramp_z],
                                         surface="saved approach ground")
    for key, link in links.items():
        if link["kind"]!="level":
            continue
        used = [door for door in doors if crosses_door(link,door)]
        if not used:
            continue
        width = min(door["clear_width"] for door in used)
        if key not in changed and width>=link["width"]:
            continue
        changed.setdefault(key,copy.deepcopy(link))
        link["width"] = min(width,link["width"])
        link["doorways"] = [door["id"] for door in used]
        if key in ("L17","L18") and site==CANNERY or key=="L02" and site==RAILWAY:
            link["threshold"] = used[0]["boundary_center"]
        link.pop("threshold_reservation",None)
        link["note"] = "Nonphysical guidance at the clear width of the existing architectural openings."
    stair_turns = []
    if site==RAILWAY:
        # Both retained guarded flights were built to the constant-cell 1.5 m clear dimension.
        for record in layout["constant_stair_cells"]:
            key = record.get("link")
            if key not in ("L16","L17"):
                continue
            link = links[key]
            changed[key] = copy.deepcopy(link)
            link["width"] = record["nominal_clear_width"]
            link["note"] = "Guidance through the measured 1.5 m guarded flight and the full constructed landing."
            if key=="L16":
                link["points"][4][2] = link["points"][5][2] = 26.78
                stair_turns.append(dict(link=key,clear_width=link["width"],datum=-3,standing=[28.9,-3,26.78]))
            else:
                link["points"][1][0] = link["points"][2][0] = 32.27
                stair_turns.append(dict(link=key,clear_width=link["width"],datum=-3,standing=[32.27,-3,38]))
    layout["architectural_interfaces"] = dict(
        fields=[dict(name=name,material=mat,polygon=shape,grade_y=0) for name,mat,_,shape in FIELDS[site]],
        terraces=[dict(name=name,polygon=shape,bed_y=bed,surface_y=-.005,area=zone)
                   for name,shape,bed,zone in COURTS[site]],
        retaining_wall=dict(x_bounds=[-14.9,-14.49],foundation_y=-.9,cap_profile=WOODLAND_PROFILE)
        if site==CANNERY else None,
        landing_foundation=list(LOWER_LANDING) if site==RAILWAY else None,
        standing_controls=checks,stair_turns=stair_turns,arrival_ramp=[ramp_x,ramp_z],nonphysical_guidance=True)
    layout["status"] = STATUS
    return changed


def measure_reimport(site, folder, meta):
    glb, native = (folder/(STEM+suffix) for suffix in (".glb",".hcs2"))
    old, old_bytes = read(ROOT/meta["source_glb"])
    current, current_bytes = read(glb)
    assert current_bytes[:len(old_bytes)]==old_bytes
    for field in ("meshes","materials","accessors","bufferViews","images","textures"):
        assert current.get(field,[])[:len(old.get(field,[]))]==old.get(field,[]),field
    removed = set(meta["removed_source_meshes"])
    for index,node in enumerate(old["nodes"]):
        expected = dict(node)
        if node.get("name") in removed:
            expected.pop("mesh",None)
        assert current["nodes"][index]==expected,node.get("name")
    retained = [part for part in split_native(ROOT/meta["source_physics"])
                if not part[0].startswith("terrain-") and part[0].split(":",1)[-1] not in removed]
    assert split_native(native)[:len(retained)]==retained
    active = read_parts(native)
    objects = import_scene(glb)
    assert not any(obj.get("role","").startswith("rough route support") for obj in objects.values())
    assert face_keys(records(objects[meta["terrain_mesh"]]))==face_keys(native_faces(saved_world(site)/"terrain-physics.hcs2"))
    for name in meta["new_interface_parts"]:
        obj = objects[name]
        bm = bmesh.new()
        bm.from_mesh(obj.data)
        bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=.00001)
        assert all(edge.is_manifold for edge in bm.edges) and bm.calc_volume(signed=True)>0,name
        bm.free()
        assert face_keys(records(obj))==face_keys(active[name][1]),name
    points,faces = [],[]
    for obj in objects.values():
        start = len(points)
        points.extend(obj.matrix_world @ vertex.co for vertex in obj.data.vertices)
        faces.extend(tuple(start+i for i in polygon.vertices) for polygon in obj.data.polygons)
    return BVHTree.FromPolygons(points,faces), len(retained)


def render(site, staging, views, suffix, source_sha):
    count = place_review_van(staging)
    scene = bpy.context.scene
    scene.render.engine = "BLENDER_WORKBENCH"
    scene.display.shading.color_type = "MATERIAL"
    scene.display.shading.show_cavity = True
    scene.render.resolution_x,scene.render.resolution_y = 1200,850
    bpy.ops.object.camera_add()
    cam = bpy.context.object
    scene.camera = cam
    CAPTURES.mkdir(parents=True,exist_ok=True)
    captures = []
    for label,pos,target,scale in views:
        cam.data.type = "ORTHO" if scale else "PERSP"
        cam.data.ortho_scale = scale or 1
        cam.data.lens = 19 if label=="lower-landing" else 24
        cam.location = pos
        cam.rotation_euler = (Vector(target)-cam.location).to_track_quat("-Z","Y").to_euler()
        image = CAPTURES/f"{site.lower()}-{label}{suffix}.png"
        scene.render.filepath = str(image)
        bpy.ops.render.render(write_still=True)
        captures.append(dict(path=rel(image),sha256=sha(image),source_glb_sha256=source_sha))
    return captures,count


def review(site):
    folder = PASS/site.lower()
    meta = json.loads((folder/"construction.json").read_bytes())
    layout = json.loads((folder/"authored-layout.json").read_bytes())
    tree,retained = measure_reimport(site,folder,meta)

    def ray(a,b):
        a,b = Vector(a),Vector(b)
        return tree.ray_cast(a,(b-a).normalized(),(b-a).length)[0]

    def stand(x,z,level=0):
        hit = ray((x,-z,level+.075),(x,-z,level-.16))
        assert hit is not None and abs(hit.z-level)<.052,(site,(x,level,z),hit)
        assert ray((x,-z,level+.076),(x,-z,level+1.91)) is None,(site,(x,level,z),"headroom")

    apertures,sills = 0,0
    for door in meta["new_portals"]:
        fixed,along,width = (door[k] for k in ("boundary","along","clear_width"))
        for u in (along-width*.33,along,along+width*.33):
            for h in (.30,1.21,door["clear_height"]-.12):
                a,b = ((fixed-.42,-u,h),(fixed+.42,-u,h)) if door["axis"]=="x" else ((u,-fixed-.42,h),(u,-fixed+.42,h))
                assert ray(a,b) is None,(site,door["id"],a,b)
                apertures += 1
            for offset in (-.19,.19):
                stand(*((fixed+offset,u) if door["axis"]=="x" else (u,fixed+offset)))
                sills += 1
    controls = layout["architectural_interfaces"]["standing_controls"]
    for point in controls:
        stand(*point)
    turns = layout["architectural_interfaces"]["stair_turns"]
    for turn in turns:
        x,y,z = turn["standing"]
        stand(x,z,y)
    if site==RAILWAY:
        stand(32.38,38.57,-3)
        assert ray((32.4,-38.55,-2.0),(32.4,-39.4,-2.0)) is not None,"south retaining enclosure"
        assert ray((32.4,-38.55,-2.0),(33.4,-38.55,-2.0)) is not None,"east retaining enclosure"
    captures,van = render(site,layout["staging"],CAMERAS[site],"",meta["glb_sha256"])
    import_scene(ROOT/meta["source_glb"])
    before_views = (CAMERAS[site][1],CAMERAS[site][-1]) if site==RAILWAY else (CAMERAS[site][1],)
    before,old_van = render(site,layout["staging"],before_views,"-before",meta["source_glb_sha256"])
    assert old_van==van
    write(folder/"export-verification.json",dict(
        glb_sha256=meta["glb_sha256"],native_sha256=meta["physics_sha256"],retained_native_records=retained,
        new_closed_parts=len(meta["new_interface_parts"]),aperture_rays=apertures,sill_controls=sills,
        independent_standing=len(controls)+len(turns)+(site==RAILWAY),canonical_van_triangles=van,
        canonical_van_source=sha(VAN/"geometry.json"),captures=captures+before))
    print("WORKYARD STATION REVIEW",site,apertures,sills,len(controls),flush=True)


def build(source):
    site = source["id"]
    folder = PASS/site.lower()
    folder.mkdir(parents=True,exist_ok=True)
    layout = json.loads((ROOT/source["layout"]).read_bytes())
    meta = json.loads((ROOT/source["construction"]).read_bytes())
    assert sha(ROOT/source["glb"])==source["glb_sha256"] and sha(ROOT/source["native"])==source["native_sha256"]
    source_terrain = [part for part in split_native(ROOT/source["native"]) if part[0].startswith("terrain-")]
    assert source_terrain==split_native(SOURCE_WORLDS[site]/"terrain-physics.hcs2")
    objects = import_scene(ROOT/source["glb"])
    old_ground = next(obj for obj in objects.values() if obj.get("role","").startswith("saved editor terrain"))
    world = saved_world(site)
    ground = terrain_surface(site,old_ground,world,program(site))
    ground["role"] = "saved editor terrain / authored workyard and station surfaces"
    ground.data.materials[-1].name = TITLES[site]+" compacted gravel"
    ground.data.materials[-1].diffuse_color = (.43,.391,.319,1)
    structure = Structure(TITLES[site],objects)
    structure.retired.add(old_ground.name)
    build_yards(site,structure,ground)
    original = derive_guidance(site,structure,layout)
    bpy.data.objects.remove(structure.template,do_unlink=True)
    assert structure.retired<=objects.keys()
    for name in structure.retired:
        bpy.data.objects.remove(objects[name],do_unlink=True)
    files = {rel(path):sha(path) for path in world.rglob("*") if path.is_file() and not path.name.endswith(".lock")}
    layout["authoritative_saved_terrain"] = dict(world=rel(world),files_sha256=files)
    laid = folder/"authored-layout.json"
    write(laid,layout)
    glb,native,blend = (folder/(STEM+suffix) for suffix in (".glb",".hcs2",".blend"))
    bpy.ops.wm.save_as_mainfile(filepath=str(blend),compress=True)
    bpy.ops.object.select_all(action="DESELECT")
    for obj in [ground,*structure.built]:
        obj.select_set(True)
    addition = ROOT/"build"/f"{site.lower()}-workyard-station-additions.glb"
    bpy.ops.export_scene.gltf(filepath=str(addition),export_format="GLB",export_extras=True,use_selection=True)
    append(ROOT/source["glb"],addition,glb,remove_mesh_names=sorted(structure.retired))
    replace_terrain_on_export(ROOT/source["native"],native,structure.retired,structure.built,world,False)
    meta.update(status=STATUS,source_glb=source["glb"],source_glb_sha256=source["glb_sha256"],
                source_physics=source["native"],source_physics_sha256=source["native_sha256"],
                before_layout=source["layout"],before_layout_sha256=sha(ROOT/source["layout"]),
                layout=rel(laid),layout_sha256=sha(laid),before_stair_components=list(meta["stair_components"]),
                before_segment_map=meta.get("source_segment_map",{}),saved_world=rel(world),
                saved_world_files_sha256=files,terrain_mesh=ground.name,retired_study_strips=sorted(structure.strips),
                removed_source_meshes=sorted(structure.retired),new_interface_parts=[obj.name for obj in structure.built],
                new_portals=structure.portals,doorways=layout["authored_doorways"],builder=rel(Path(__file__)),
                builder_sha256=sha(Path(__file__)),glb_sha256=sha(glb),physics_sha256=sha(native),blend_sha256=sha(blend))
    write(folder/"construction.json",meta)
    write(folder/"quarantine.json",dict(source=source,retired=sorted(structure.retired),superseded_guides=original))
    return dict(source,construction=rel(folder/"construction.json"),layout=rel(laid),glb=rel(glb),
                native=rel(native),blend=rel(blend),glb_sha256=sha(glb),native_sha256=sha(native))


if __name__ == "__main__":
    if "--review-only" in sys.argv:
        for site in SITES:
            review(site)
    else:
        roster = json.loads(BASELINE.read_bytes())
        roster["active_sources"] = [build(row) if row["id"] in SITES else row for row in roster["active_sources"]]
        for row in roster["sites"]:
            if row["id"] in SITES:
                row["decision"] = STATUS
        roster["baseline"] = rel(BASELINE)
        write(PASS/"roster.json",roster)
