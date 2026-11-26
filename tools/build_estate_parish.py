"""Rebuild the estate, farm and parish entrances before deriving route guidance."""
import copy
import json
from pathlib import Path
import sys

import bmesh
import bpy
from mathutils import Vector
from mathutils.bvhtree import BVHTree

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT/"tools"))
from append_static_glb import append, read
from build_cemetery_grounds import rel, replace_terrain_on_export, terrain_surface
from build_coast_entrances import Structure
from build_college_grounds import import_scene, place_review_van
from build_deliberate_entrances import write
from build_roster_foundations import material
from check_roster_decks import read_parts
from compose_room_terrain import split_native
from export_roster_collision import face_keys, native_faces
from prepare_estate_parish import (BASE, BASELINE, CHURCH, FARMHOUSE, FIELDS, MANSION, PASS,
                                   PAVEMENTS, SITES, SOURCE_WORLDS, VAN, program, saved_world)
from review_roster_macroterrain import records, sha

STEM = "estate-parish"
CAPTURES = BASE/"captures/estate-parish-batch"
STATUS = "ESTATE_FARM_PARISH_FOUNDED_ENTRANCES_RUNTIME_OPEN"
TITLES = {MANSION: "Blackthorn Manor", FARMHOUSE: "Hollowmere Farm", CHURCH: "St. Brigid Parish"}
# (link, room, outside, axis, face coordinate, facade span, door center, width, facade height, side)
FACADES = {
    MANSION: (
        (2,"foyer","entry","z",50,(20,24),22,1.52,3.48,"S"),
        (26,"receiving","garden","z",20,(43,49),46,1.9,3.6,"N"),
        (28,"service-stair","garden","z",2,(40,46),43,1.30,3.48,"N"),
        (29,"library","garden","x",8,(24,36),30,1.38,3.6,"W"),
        (30,"ballroom","garden","x",54,(30,50),40,1.95,7.2,"E"),
    ),
    FARMHOUSE: (
        (3,"mudroom","grounds","z",28,(20,26),23,1.42,3,"S"),
        (11,"prep","grounds","z",10,(15,20),17,1.35,3,"N"),
        (22,"barn-aisle","grounds","z",9,(35,39),37,2.4,3,"N"),
        (23,"barn-aisle","grounds","z",24,(35,39),37,2.4,2.88,"S"),
        (30,"repair","grounds","z",33,(31,36),34.63,1.56,3,"S"),
        (33,"laundry","grounds","z",33,(27,31),29,1.36,3,"S"),
    ),
    CHURCH: (
        (2,"vestibule","entry","z",39,(18,26),22,1.75,3,"S"),
        (17,"chapel","grounds","x",34,(14,26),20,1.4,4,"E"),
        (18,"vestry","grounds","z",6,(26,31),28.5,1.35,3,"N"),
    ),
}
# Earlier structural passes renamed these remaining pieces of the superseded faces.
EXTRA_FACES = {
    MANSION: ("MANSION seated wall cap 17", "MANSION seated wall cap 18", "MANSION bay indexed door 3",
              "MANSION bay wall ('z', 2, 40, 46, 0, 3.6) pier0 region0",
              "MANSION bay wall ('z', 2, 40, 46, 0, 3.6) lintel0 region0",
              "MANSION bay wall ('z', 2, 40, 46, 0, 3.6) end region0"),
    FARMHOUSE: ("FARMHOUSE seated wall cap 4", "FARMHOUSE seated wall cap 5"),
    CHURCH: (),
}
CAMERAS = {
    MANSION: (("forecourt",(25,-63,1.62),(22,-48,1.65),0),
              ("service-court",(49,-8,17),(45,-12,0),27),
              ("ballroom-terrace",(64,-47,15),(52,-40,1.5),30)),
    FARMHOUSE: (("forecourt",(22,-42,1.62),(27,-27,1.9),0),
                ("working-court",(45,-34,18),(34,-27,0),28),
                ("barn-ports",(45,-7,16),(36,-15,1),24)),
    CHURCH: (("forecourt",(20,-50,1.62),(22,-38,2),0),
             ("vestry-court",(42,-3,17),(30,-8,0),22),
             ("chapel-terrace",(43,-24,13),(32,-22,1.3),22)),
}


def finish_court_surfaces(site, ground, paving):
    """Give overlapping grade faces the same finish as the founded court beneath."""
    mesh = ground.data
    index = len(mesh.materials)
    mesh.materials.append(paving)
    polygons = [boundary for _,boundary,_,_ in PAVEMENTS[site]]
    edges = [list(zip(polygon,polygon[1:]+polygon[:1])) for polygon in polygons]
    for face in mesh.polygons:
        x,y,z = face.center
        if abs(z)>.025:
            continue
        for boundary in edges:
            hits = sum((a[1]>-y)!=(b[1]>-y) and x<a[0]+(-y-a[1])*(b[0]-a[0])/(b[1]-a[1])
                       for a,b in boundary)
            if hits%2:
                face.material_index = index
                break


def build_interfaces(site, s, ground):
    stone_color = {MANSION: (.32,.29,.26), FARMHOUSE: (.42,.32,.22), CHURCH: (.44,.41,.36)}
    stone = material(s.prefix+" exterior masonry", stone_color[site])
    paving = material(s.prefix+" dressed forecourt stone", (.36,.34,.29))
    for label, boundary, foundation, zone in PAVEMENTS[site]:
        s.part(label, [boundary], foundation, -.005, paving, "solid founded architectural terrace", zone)
    s.strips.update(name for name in s.old if " bay retained outside L" in name and " support " in name)
    s.retired.update(s.strips)
    s.retired.update(EXTRA_FACES[site])
    s.retired.add("Arrival end bearing "+site+" 0")
    for number, room, outside, axis, boundary, ends, center, width, height, side in FACADES[site]:
        s.retire_faces((room, side))
        label = f"{room} {side.lower()} exterior doorway"
        s.face(room+" "+side+" facade", axis, boundary, ends, 0, height, stone,
               [(label, center, width, 2.2, [room, outside])], room)
        s.portals[-1]["link"] = f"L{number:02}"
    finish_court_surfaces(site,ground,paving)
    s.areas.update(fields=[dict(name=name, material=surface, grade_y=0, polygon=points)
                         for name,surface,_,points in FIELDS[site]],
                   terraces=[dict(name=name, polygon=points, bed_y=bed, surface_y=-.005, area=zone)
                             for name,points,bed,zone in PAVEMENTS[site]])


def derive_guidance(site, s, layout):
    links = {row["id"]: row for row in layout["links"]}
    doors = {row["link"]: row for row in s.portals}
    replaced = {}
    ax, _, az = layout["staging"]["cargo_route"][0]
    ramp = (ax, az)

    def guide(number, points, width=None):
        key = f"L{number:02}"
        link = links[key]
        replaced[key] = copy.deepcopy(link)
        link["points"] = [[x,0,z] for x,z in points]
        door = doors.get(key)
        if door:
            axis, fixed, center = (door[k] for k in ("axis","boundary","along"))
            threshold = [fixed,0,center] if axis=="x" else [center,0,fixed]
            assert threshold in link["points"], (key, threshold)
            link.update(threshold=threshold, width=width or door["clear_width"], opening="doorway", portal=door["id"])
        else:
            link.pop("threshold",None)
            link.pop("opening",None)
            if width is not None:
                link["width"] = width
        link["note"] = "Guidance across the built site. Locations and clear widths come from the physical assemblies."

    if site == MANSION:
        court = (24,58.6)
        east = (55.15,27.85)
        receiving = (47.7,17.35)
        north_drive = [receiving,(53.6,18.7),(55.15,19.05),east]
        guide(1,[ramp,(23.8,66),court])
        guide(2,[court,(22,54),(22,50),(22,47)])
        guide(26,[(46,23),(46,20),(46,18.2),*north_drive])
        guide(28,[(43,8),(43,2),(43,1.02),(47.4,1.02),(47.7,4.4),*north_drive],1.0)
        guide(29,[(14,30),(8,30),(6.35,30),(6.4,50.85),(8.85,52.15),(15,53.5),court])
        guide(30,[(47,40),(54,40),(55.15,40),east])
        guide(31,[east,(55.15,50.4),(52.4,51.35),(46.5,53.7),(34,55.2),court])
        s.areas["standing_controls"] = [court,east,receiving,(47.4,1.02),(6.35,30),(55.15,40)]
    elif site == FARMHOUSE:
        court = (24.1,32.1)
        entry = (23,38)
        machine_yard = [court,(26.0,35.4),(40.35,35.4),(44.1,32.8)]
        guide(1,[ramp,(22.4,41),entry])
        guide(2,[entry,court])
        guide(3,[court,(23,29.65),(23,28),(23,25)])
        guide(11,[(17.5,11.5),(17,10),(17,7.85),(9.7,7.85),(9.7,29.9),(13.7,31.55),court])
        guide(22,[*machine_yard,(45.25,26.8),(45.25,9.6),(42.55,7.6),(37,7.6),(37,9),(37,16.5)],1.38)
        guide(23,[*machine_yard,(44.55,25.02),(37,25.02),(37,24),(37,16.5)],1.38)
        guide(30,[court,(26,35.4),(34.63,35.4),(34.63,33),(34.63,29.5),(33.5,29.5)],1.44)
        guide(33,[court,(26,35.4),(29,35.4),(29,33),(29,29.5)])
        s.areas["standing_controls"] = [court,entry,(17,7.85),(9.7,24),(37,25.15),(45.25,14)]
    else:
        forecourt = (22,45)
        yard = (35.65,20)
        guide(1,[ramp,(19.5,47),forecourt])
        guide(2,[forecourt,(22,39),(22,36.5)])
        guide(17,[(30,20),(34,20),yard])
        guide(18,[(28.5,10),(28.5,6),(28.5,4.59),(32.65,4.59),(34.8,5.51),(35.45,7.35),(35.55,13.15),yard],1.16)
        guide(30,[yard,(35.55,33.35),(35.55,34.45),(33.96,35.86),(32.3,35.91),(27.85,37),(27.85,42.2),forecourt],1.15)
        s.areas["standing_controls"] = [forecourt,yard,(28.5,4.6),(35.35,8.35),(27.8,37.2)]
    layout["authored_doorways"] = [row for row in layout["authored_doorways"]
                                  if row.get("id") not in s.retired and row.get("link") not in doors]+s.portals
    assert len({row["id"] for row in layout["authored_doorways"]}) == len(layout["authored_doorways"])
    layout["architectural_interfaces"] = dict(s.areas, nonphysical_guidance=True, arrival_ramp=list(ramp))
    layout["arrival_end_bearing"] = {"surface":"saved approach ground","guide":"L01","ramp":[ramp[0],0,ramp[1]]}
    cargo = links["L01"]["points"] + links["L02"]["points"][1:]
    if site == FARMHOUSE:
        cargo += links["L03"]["points"][1:]
    layout["staging"]["cargo_route"] = cargo[:-1]
    layout["staging"]["note"] = "Canonical van pose with verified native footing from the actual rear ramp to the exterior door."
    layout["title"], layout["status"] = TITLES[site], STATUS
    return replaced


def review(site, out, meta, layout):
    glb, native = (out/(STEM+extension) for extension in (".glb",".hcs2"))
    old, old_bytes = read(ROOT/meta["source_glb"])
    new, new_bytes = read(glb)
    assert new_bytes[:len(old_bytes)] == old_bytes
    for field in ("meshes","materials","accessors","bufferViews","images","textures"):
        assert new.get(field,[])[:len(old.get(field,[]))] == old.get(field,[]), field
    removed = set(meta["removed_source_meshes"])
    for index, node in enumerate(old["nodes"]):
        expected = dict(node)
        if node.get("name") in removed:
            expected.pop("mesh",None)
        assert new["nodes"][index] == expected, node.get("name")
    retained = [part for part in split_native(ROOT/meta["source_physics"])
                if not part[0].startswith("terrain-") and part[0].split(":",1)[-1] not in removed]
    assert split_native(native)[:len(retained)] == retained
    active = read_parts(native)
    objects = import_scene(glb)
    assert face_keys(records(objects[meta["terrain_mesh"]])) == face_keys(native_faces(saved_world(site)/"terrain-physics.hcs2"))
    assert not any(obj.get("role","").startswith("rough route support") for obj in objects.values())
    for name in meta["new_interface_parts"]:
        obj = objects[name]
        bm = bmesh.new()
        bm.from_mesh(obj.data)
        bmesh.ops.remove_doubles(bm, verts=list(bm.verts), dist=.00001)
        assert all(edge.is_manifold for edge in bm.edges) and bm.calc_volume(signed=True)>0, name
        bm.free()
        assert face_keys(records(obj)) == face_keys(active[name][1]), name
    vertices, faces = [], []
    for obj in objects.values():
        start = len(vertices)
        vertices.extend(obj.matrix_world @ v.co for v in obj.data.vertices)
        faces.extend(tuple(start+i for i in face.vertices) for face in obj.data.polygons)
    tree = BVHTree.FromPolygons(vertices,faces)

    def ray(a,b):
        a,b = Vector(a),Vector(b)
        return tree.ray_cast(a,(b-a).normalized(),(b-a).length)[0]

    def stand(x,z):
        hit = ray((x,-z,.08),(x,-z,-.16))
        assert hit is not None and abs(hit.z)<.052, (site, (x,z), hit)
        assert ray((x,-z,.081),(x,-z,1.91)) is None, (site, (x,z), "headroom")

    apertures, sills = 0, 0
    for door in meta["new_portals"]:
        fixed, along, width = (door[k] for k in ("boundary","along","clear_width"))
        for delta in (-width*.33,0,width*.33):
            u = along+delta
            for h in (.30,1.21,door["clear_height"]-.12):
                a,b = ((fixed-.42,-u,h),(fixed+.42,-u,h)) if door["axis"]=="x" else ((u,-fixed-.42,h),(u,-fixed+.42,h))
                assert ray(a,b) is None, (site,door["id"],a,b)
                apertures += 1
            for offset in (-.19,.19):
                stand(*((fixed+offset,u) if door["axis"]=="x" else (u,fixed+offset)))
                sills += 1
    controls = layout["architectural_interfaces"]["standing_controls"]
    for point in controls:
        stand(*point)
    van = place_review_van(layout["staging"])
    scene = bpy.context.scene
    scene.render.engine = "BLENDER_WORKBENCH"
    scene.display.shading.color_type = "MATERIAL"
    scene.display.shading.show_cavity = True
    scene.render.resolution_x,scene.render.resolution_y = 1200,850
    bpy.ops.object.camera_add()
    cam = bpy.context.object
    scene.camera = cam
    CAPTURES.mkdir(parents=True, exist_ok=True)
    old_bounds = layout["site_bounds"]
    span = max(old_bounds[2]-old_bounds[0], old_bounds[3]-old_bounds[1])
    cx, cz = (old_bounds[0]+old_bounds[2])/2,(old_bounds[1]+old_bounds[3])/2
    overview = ("overview",(cx+span*.44,-cz-span*.37,span*.88),(cx,-cz,0),span*1.34)
    captures = []
    for label,pos,target,scale in (overview,*CAMERAS[site]):
        cam.data.type = "ORTHO" if scale else "PERSP"
        cam.data.ortho_scale = scale or 1
        cam.data.lens = 23
        cam.location = pos
        cam.rotation_euler = (Vector(target)-cam.location).to_track_quat("-Z","Y").to_euler()
        image = CAPTURES/(site.lower()+"-"+label+".png")
        scene.render.filepath = str(image)
        bpy.ops.render.render(write_still=True)
        captures.append(dict(path=rel(image),sha256=sha(image),source_glb_sha256=sha(glb)))
    write(out/"export-verification.json",dict(glb_sha256=sha(glb),native_sha256=sha(native),
          retained_native_records=len(retained),new_closed_parts=len(meta["new_interface_parts"]),
          aperture_rays=apertures,sill_controls=sills,independent_standing=len(controls),
          canonical_van_triangles=van,canonical_van_source=sha(VAN/"geometry.json"),captures=captures))
    print("ESTATE PARISH EXPORT REVIEW",site,apertures,sills,len(controls),flush=True)


def build(source):
    site = source["id"]
    folder = PASS/site.lower()
    folder.mkdir(parents=True,exist_ok=True)
    layout = json.loads((ROOT/source["layout"]).read_bytes())
    meta = json.loads((ROOT/source["construction"]).read_bytes())
    assert sha(ROOT/source["glb"]) == source["glb_sha256"]
    assert sha(ROOT/source["native"]) == source["native_sha256"]
    terrain_parts = [part for part in split_native(ROOT/source["native"]) if part[0].startswith("terrain-")]
    assert terrain_parts == split_native(SOURCE_WORLDS[site]/"terrain-physics.hcs2")
    objects = import_scene(ROOT/source["glb"])
    old_ground = next(obj for obj in objects.values() if obj.get("role","").startswith("saved editor terrain"))
    world = saved_world(site)
    ground = terrain_surface(site, old_ground, world, program(site))
    ground["role"] = "saved editor terrain / authored building grounds and courts"
    ground.data.materials[-1].name = TITLES[site]+" compacted gravel"
    ground.data.materials[-1].diffuse_color = (.44,.39,.30,1)
    structure = Structure(TITLES[site], objects)
    structure.retired.add(old_ground.name)
    build_interfaces(site,structure,ground)
    original = derive_guidance(site,structure,layout)
    bpy.data.objects.remove(structure.template,do_unlink=True)
    assert structure.retired <= objects.keys(), structure.retired-objects.keys()
    for name in structure.retired:
        bpy.data.objects.remove(objects[name],do_unlink=True)
    files = {rel(path):sha(path) for path in world.rglob("*") if path.is_file() and not path.name.endswith(".lock")}
    layout["authoritative_saved_terrain"] = dict(world=rel(world),files_sha256=files)
    laid = folder/"authored-layout.json"
    write(laid,layout)
    glb,native,blend = (folder/(STEM+extension) for extension in (".glb",".hcs2",".blend"))
    bpy.ops.wm.save_as_mainfile(filepath=str(blend),compress=True)
    bpy.ops.object.select_all(action="DESELECT")
    for obj in [ground,*structure.built]:
        obj.select_set(True)
    addition = ROOT/"build"/(site.lower()+"-estate-parish-additions.glb")
    bpy.ops.export_scene.gltf(filepath=str(addition),export_format="GLB",export_extras=True,use_selection=True)
    append(ROOT/source["glb"],addition,glb,remove_mesh_names=sorted(structure.retired))
    replace_terrain_on_export(ROOT/source["native"],native,structure.retired,structure.built,world,False)
    meta.update(status=STATUS,source_glb=source["glb"],source_glb_sha256=source["glb_sha256"],
                source_physics=source["native"],source_physics_sha256=source["native_sha256"],
                before_layout=source["layout"],before_layout_sha256=sha(ROOT/source["layout"]),
                layout=rel(laid),layout_sha256=sha(laid),before_stair_components=list(meta["stair_components"]),
                stair_components=[name for name in meta["stair_components"] if name.split(":",1)[-1] not in structure.retired],
                before_segment_map=meta.get("source_segment_map",{}),saved_world=rel(world),
                saved_world_files_sha256=files,terrain_mesh=ground.name,
                retired_study_strips=sorted(structure.strips),removed_source_meshes=sorted(structure.retired),
                new_interface_parts=[obj.name for obj in structure.built],new_portals=structure.portals,
                builder=rel(Path(__file__)),builder_sha256=sha(Path(__file__)),
                glb_sha256=sha(glb),physics_sha256=sha(native),blend_sha256=sha(blend))
    write(folder/"construction.json",meta)
    write(folder/"quarantine.json",dict(source=source,retired=sorted(structure.retired),superseded_guides=original))
    return dict(source,construction=rel(folder/"construction.json"),layout=rel(laid),glb=rel(glb),
                native=rel(native),blend=rel(blend),glb_sha256=sha(glb),native_sha256=sha(native))


if __name__ == "__main__":
    roster = json.loads(BASELINE.read_bytes())
    if "--review-only" in sys.argv:
        for site in SITES:
            out = PASS/site.lower()
            review(site,out,json.loads((out/"construction.json").read_bytes()),
                   json.loads((out/"authored-layout.json").read_bytes()))
    else:
        roster["active_sources"] = [build(row) if row["id"] in SITES else row for row in roster["active_sources"]]
        for row in roster["sites"]:
            if row["id"] in SITES:
                row["decision"] = STATUS
        roster["baseline"] = rel(BASELINE)
        write(PASS/"roster.json",roster)
