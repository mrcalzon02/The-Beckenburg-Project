"""Build complete receiving landings and framed openings at Widow's Peak."""
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
from build_cemetery_grounds import rel, replace_terrain_on_export, terrain_surface
from build_coast_entrances import Structure
from build_college_grounds import import_scene, place_review_van, terrain_tree
from build_dam_spillway_gallery import validate_solid
from build_deliberate_entrances import write
from build_roster_foundations import material
from check_roster_decks import read_parts
from compose_room_terrain import split_native
from export_roster_collision import face_keys, native_faces
from prepare_cemetery_grounds import VAN
from prepare_lookout_landings import BASE, BASELINE, FIELDS, INPUTS, PASS, SITE, SOURCE_WORLD, WORLD
from review_roster_macroterrain import records, sha

OUT = PASS/SITE.lower()
STEM = "lookout-landings"
CAPTURES = BASE/"captures/lookout-landing-batch"
STATUS = "LOOKOUT_FOUNDED_LANDINGS_AND_FITTED_DOORS_RUNTIME_OPEN"
LANDINGS = (
    ("Lower stair receiving court","Lookout terrace L03 lower abutment region0",
     (17.75,58,22.25,61.2),-.55,-.005,"lower"),
    ("Boulder saddle full landing","Lookout terrace L04 lower abutment region0",
     (25.9,36,30.1,38.8),2.29,2.995,"bend"),
    ("Ranger ruin full west porch","Lookout terrace ruin west level porch region0",
     (38.2,20.65,40,23.35),5.28,6,"ruin"),
)
# Full exterior load lines. The south return is only the exposed ruin wing.
FOUNDATIONS = (
    ("Maintenance hut north","z",14,(4,20),-1,5.32,"hut"),
    ("Maintenance hut south","z",26,(4,20),1,5.32,"hut"),
    ("Maintenance hut west","x",4,(14,26),-1,5.32,"hut"),
    ("Maintenance hut east","x",20,(14,26),1,5.32,"hut"),
    ("Ranger ruin north","z",14,(40,54),-1,5.30,"ruin"),
    ("Ranger ruin west","x",40,(14,30),-1,5.30,"ruin"),
    ("Ranger ruin east","x",54,(14,30),1,5.30,"ruin"),
    ("Ranger ruin south return","z",30,(50,54),1,5.30,"ruin"),
    ("Ranger store west","x",40,(30,40),-1,2.96,"store"),
    ("Ranger store east","x",50,(30,40),1,3.18,"store"),
    ("Ranger store south","z",40,(40,50),1,2.91,"store"),
)
FACADES = (
    ("WLD-04-ruin-west-22","ruin west entrance","x",40,(14,30),22,1.62,("ruin","clearing")),
    ("WLD-04-ruin-store-45","ranger room shared entrance","z",30,(40,54),45,1.62,("ruin","store")),
)
VIEWS = (
    ("overview",(69,-102,97),(29,-43,6),112),
    ("roadhead",(35,-89,24),(26,-69,2),47),
    ("lower-court",(16,-62.8,1.62),(20,-56.0,1.84),0),
    ("boulder-saddle",(31,-41.4,7.8),(28,-36.5,3.7),16),
    ("ruin-porch",(35.3,-24.8,7.27),(40.5,-22,7.32),0),
    ("tower-midturn",(30.66,-9.03,9.87),(28.05,-9.17,9.7),0),
    ("cab-east-door",(27,-5,16.62),(32.3,-5,16.58),0),
)


def render(layout,views,source_sha,suffix=""):
    van = place_review_van(layout["staging"])
    scene = bpy.context.scene
    scene.render.engine = "BLENDER_WORKBENCH"
    shading = scene.display.shading
    shading.color_type = "MATERIAL"
    shading.show_cavity = True
    scene.render.resolution_x,scene.render.resolution_y = 1200,850
    bpy.ops.object.camera_add()
    camera = bpy.context.object
    scene.camera = camera
    CAPTURES.mkdir(parents=True,exist_ok=True)
    captures = []
    for label,position,target,scale in views:
        camera.data.type = "ORTHO" if scale else "PERSP"
        camera.data.ortho_scale = scale or 1
        camera.data.lens = 21
        camera.location = position
        camera.rotation_euler = (Vector(target)-camera.location).to_track_quat("-Z","Y").to_euler()
        image = CAPTURES/f"{label}{suffix}.png"
        scene.render.filepath = str(image)
        bpy.ops.render.render(write_still=True)
        captures.append(dict(path=rel(image),sha256=sha(image),source_glb_sha256=source_sha))
    return captures,van


def build_structure(structure,saved_ground):
    stone = material("Lookout founded stair masonry",(.44,.402,.35))
    wall = material("Lookout ranger station wall finish",(.37,.355,.325))
    ground = terrain_tree(saved_ground)
    support = []
    landing_names = {}
    for label,previous,bounds,bottom,top,zone in LANDINGS:
        assert previous in structure.old,previous
        structure.retired.add(previous)
        a,b,c,d = bounds
        samples = []
        for x in (a+.12,(a+c)/2,c-.12):
            for z in (b+.12,(b+d)/2,d-.12):
                hit = ground.ray_cast(Vector((x,-z,24)),Vector((0,0,-1)),60)[0]
                assert hit is not None and bottom < hit.z < top+.02,(label,x,z,hit)
                samples.append([x,z,round(hit.z,6)])
        obj = structure.box(label,bounds,bottom,top,stone,"closed founded stair or door landing",zone)
        landing_names[previous] = obj.name
        support.append(dict(label=label,bounds=bounds,foundation_y=bottom,floor_y=top,zone=zone,
                            soil_samples=samples,part=obj.name))
    structure.retire_faces(("ruin","W"),("ruin","S"),("store","N"))
    assert {"Header ruin W","Header ruin S","Flat route shell Wall store N.001"}<=structure.retired
    for key,label,axis,fixed,extent,along,width,rooms in FACADES:
        structure.face(label,axis,fixed,extent,6,3,wall,[(label,along,width,2.2,list(rooms))],rooms[0])
        p = structure.portals[-1]
        p.update(id=key,label=label,door_type="pedestrian",frame_depth=.24,frame_span=[fixed-.12,fixed+.12])
        p["boundary_center"] = [fixed,6,along] if axis=="x" else [along,6,fixed]
        p["frame_center"] = list(p["boundary_center"])
    foundations = []
    for label,axis,boundary,(lo,hi),side,bottom,zone in FOUNDATIONS:
        samples = []
        for fraction in (.035,.20,.40,.60,.80,.965):
            along = lo+(hi-lo)*fraction
            out = boundary+.14*side
            x,z = (out,along) if axis=="x" else (along,out)
            hit = ground.ray_cast(Vector((x,-z,24)),Vector((0,0,-1)),60)[0]
            assert hit is not None and bottom+.2<hit.z<5.76,(label,x,z,hit)
            samples.append([x,z,round(hit.z,6)])
        a,b = boundary-.19,boundary+.19
        bounds = (a,lo-.19,b,hi+.19) if axis=="x" else (lo-.19,a,hi+.19,b)
        wall = structure.box(label+" foundation",bounds,bottom,5.76,stone,
                             "continuous bedded exterior foundation",zone)
        foundations.append(dict(part=wall.name,footprint=bounds,floor_seat_y=5.76,bed_y=bottom,
                                soil_samples=samples))
    return landing_names,support,foundations


def derive(layout,meta,structures,support,foundations):
    by_id = {row["id"]:row for row in layout["links"]}
    before = {}
    old_maps = copy.deepcopy(meta["source_segment_map"])
    old = {key:copy.deepcopy(by_id[key]["points"]) for key in ("L03","L07","L08","L09","L11","L13","L14B","L15")}
    van = layout["staging"]["cargo_route"][0]
    entry = [28,0,66]
    lower = [14.6,0,61.35]
    return_turn = 47.24
    routes = {
        "L01": [van,[28,0,72],entry],
        "L02": [entry,[21.8,0,64.1],lower],
        "L03": [lower,[20,0,61.25],*old["L03"][2:]],
        "L07": [*old["L07"][:3],[40,6,22],old["L07"][-1]],
        "L08": [*old["L08"][:2],[45,6,30],old["L08"][-1]],
        "L09": [old["L09"][0],[45,6,40],*old["L09"][1:3],[45,3,return_turn],
                [43,3,return_turn],old["L09"][-1]],
        "L11": [old["L11"][0],[43,3,return_turn],[45,3,return_turn],*old["L11"][3:5],
                [45,6,40],old["L11"][5],[45,6,30],old["L11"][6],[40,6,22],*old["L11"][7:]],
        "L13": old["L13"],
        "L14B": old["L14B"],
        "L15": [old["L15"][0],[29.9,15,5],old["L15"][-1]],
    }
    widths = {"L03":1.75,"L07":1.62,"L09":1.8,"L11":1.62,"L13":1.5,"L14B":1.25,"L15":1.25}
    thresholds = {
        "L07":["WLD-04-ruin-west-22"],
        "L08":["WLD-04-ruin-store-45"],
        "L09":["WLD-04-store-south-normal"],
        "L11":["WLD-04-store-south-normal","WLD-04-ruin-store-45","WLD-04-ruin-west-22"],
        "L14B":["WLD-04-restart-cab south"],
        "L15":["WLD-04-restart-cab east"],
    }
    for key,path in routes.items():
        row = by_id[key]
        before[key] = copy.deepcopy(row)
        row.update(points=path,width=widths.get(key,row["width"]),
                   note="Nonphysical guide between supported landings or fitted clear openings.")
        row.pop("threshold_reservation",None)
        if key in thresholds:
            row["doorways"] = thresholds[key]
        previous = {(tuple(before[key]["points"][int(i)]),tuple(before[key]["points"][int(i)+1])):n
                    for i,n in old_maps[key].items()}
        remap = {i:previous[(tuple(a),tuple(b))]
                 for i,(a,b) in enumerate(zip(path,path[1:])) if abs(a[1]-b[1])>.01}
        assert sorted(remap.values())==sorted(previous.values()),key
        meta["source_segment_map"][key] = remap
    layout["authored_doorways"] += structures.portals
    assert len({p["id"] for p in layout["authored_doorways"]})==len(layout["authored_doorways"])
    layout["native_landing_fit"] = dict(
        roadhead=[dict(name=name,footprint=shape,datum=0) for name,_,_,shape in FIELDS],
        receiving_landings=support,perimeter_foundations=foundations,guarded_tower_width=1.5,tower_door_width=1.25,
        lower_trail_court=lower,switchback_return_z=return_turn,nonphysical_guidance=True)
    layout["status"] = STATUS
    meta["before_segment_map"] = old_maps
    return before


def build():
    roster = json.loads(BASELINE.read_bytes())
    source = next(row for row in roster["active_sources"] if row["id"]==SITE)
    layout = json.loads((ROOT/source["layout"]).read_bytes())
    meta = json.loads((ROOT/source["construction"]).read_bytes())
    assert sha(ROOT/source["native"])==source["native_sha256"] and sha(ROOT/source["glb"])==source["glb_sha256"]
    expected = [part for part in split_native(ROOT/source["native"]) if part[0].startswith("terrain-")]
    assert expected==split_native(SOURCE_WORLD/"terrain-physics.hcs2")
    objects = import_scene(ROOT/source["glb"])
    old_ground = next(o for o in objects.values() if o.get("role","").startswith("saved editor terrain"))
    ground = terrain_surface(SITE,old_ground,WORLD,INPUTS)
    ground["role"] = "saved editor terrain / authored roadhead and full lower court"
    ground.data.materials[-1].name = "Lookout compacted mountain gravel"
    ground.data.materials[-1].diffuse_color = (.434,.393,.327,1)
    structure = Structure("Widow's Peak",objects)
    assert not structure.strips
    structure.retired.add(old_ground.name)
    step_map,support,foundations = build_structure(structure,ground)
    old_routes = derive(layout,meta,structure,support,foundations)
    bpy.data.objects.remove(structure.template,do_unlink=True)
    for name in structure.retired:
        bpy.data.objects.remove(objects[name],do_unlink=True)
    files = {rel(path):sha(path) for path in WORLD.rglob("*") if path.is_file() and not path.name.endswith(".lock")}
    layout["authoritative_saved_terrain"] = dict(world=rel(WORLD),files_sha256=files)
    laid = OUT/"authored-layout.json"
    write(laid,layout)
    glb,native,blend = (OUT/(STEM+ext) for ext in (".glb",".hcs2",".blend"))
    bpy.ops.wm.save_as_mainfile(filepath=str(blend),compress=True)
    bpy.ops.object.select_all(action="DESELECT")
    for obj in [ground,*structure.built]:
        obj.select_set(True)
    addition = ROOT/"build/lookout-landings-additions.glb"
    bpy.ops.export_scene.gltf(filepath=str(addition),export_format="GLB",export_extras=True,use_selection=True)
    append(ROOT/source["glb"],addition,glb,remove_mesh_names=sorted(structure.retired))
    replace_terrain_on_export(ROOT/source["native"],native,structure.retired,structure.built,WORLD,False)
    past = list(meta["stair_components"])
    assert set(step_map)<=set(past)
    meta.update(status=STATUS,source_glb=source["glb"],source_glb_sha256=source["glb_sha256"],
                source_physics=source["native"],source_physics_sha256=source["native_sha256"],
                before_layout=source["layout"],before_layout_sha256=sha(ROOT/source["layout"]),
                layout=rel(laid),layout_sha256=sha(laid),before_stair_components=past,
                stair_components=[step_map.get(name,name) for name in past],saved_world=rel(WORLD),
                saved_world_files_sha256=files,terrain_mesh=ground.name,
                removed_source_meshes=sorted(structure.retired),new_interface_parts=[obj.name for obj in structure.built],
                new_portals=structure.portals,doorways=layout["authored_doorways"],landing_founding=support,foundation_founding=foundations,
                builder=rel(Path(__file__)),builder_sha256=sha(Path(__file__)),
                glb_sha256=sha(glb),physics_sha256=sha(native),blend_sha256=sha(blend))
    write(OUT/"construction.json",meta)
    write(OUT/"quarantine.json",dict(source=source,retired=sorted(structure.retired),superseded_guides=old_routes))
    new = dict(source,construction=rel(OUT/"construction.json"),layout=rel(laid),glb=rel(glb),
               native=rel(native),blend=rel(blend),glb_sha256=sha(glb),native_sha256=sha(native))
    roster["active_sources"] = [new if row["id"]==SITE else row for row in roster["active_sources"]]
    next(row for row in roster["sites"] if row["id"]==SITE)["decision"] = STATUS
    roster["baseline"] = rel(BASELINE)
    write(PASS/"roster.json",roster)


def review():
    m = json.loads((OUT/"construction.json").read_bytes())
    layout = json.loads((OUT/"authored-layout.json").read_bytes())
    glb,native = (OUT/(STEM+ext) for ext in (".glb",".hcs2"))
    before,buffer = read(ROOT/m["source_glb"])
    after,new_buffer = read(glb)
    assert new_buffer[:len(buffer)]==buffer
    for field in ("meshes","materials","accessors","bufferViews","textures","images"):
        assert after.get(field,[])[:len(before.get(field,[]))]==before.get(field,[]),field
    retired = set(m["removed_source_meshes"])
    for index,node in enumerate(before["nodes"]):
        expected = dict(node)
        if node.get("name") in retired:
            expected.pop("mesh",None)
        assert after["nodes"][index]==expected,node.get("name")
    retained = [part for part in split_native(ROOT/m["source_physics"])
                if not part[0].startswith("terrain-") and part[0].split(":",1)[-1] not in retired]
    native_parts = split_native(native)
    assert native_parts[:len(retained)]==retained
    terrain = [part for part in native_parts if part[0].startswith("terrain-")]
    assert terrain==split_native(WORLD/"terrain-physics.hcs2")
    physical = read_parts(native)
    objects = import_scene(glb)
    assert face_keys(records(objects[m["terrain_mesh"]]))==face_keys(native_faces(WORLD/"terrain-physics.hcs2"))
    for name in m["new_interface_parts"]:
        obj = objects[name]
        validate_solid(obj)
        assert face_keys(records(obj))==face_keys(physical[name][1]),name
    vertices,faces = [],[]
    for obj in objects.values():
        start = len(vertices)
        vertices.extend(obj.matrix_world@v.co for v in obj.data.vertices)
        faces.extend(tuple(start+i for i in p.vertices) for p in obj.data.polygons)
    tree = BVHTree.FromPolygons(vertices,faces)

    def ray(a,b):
        a,b = Vector(a),Vector(b)
        return tree.ray_cast(a,(b-a).normalized(),(b-a).length)[0]

    def stand(x,y,z):
        hit = ray((x,-z,y+.08),(x,-z,y-.15))
        assert hit is not None and abs(hit.z-y)<.052,((x,y,z),hit)
        assert ray((x,-z,y+.081),(x,-z,y+1.91)) is None,((x,y,z),"standing headroom")

    apertures,sills = 0,0
    for door in layout["authored_doorways"]:
        fixed,along,y,width = (door[k] for k in ("boundary","along","sill_y","clear_width"))
        for place in (along-width*.29,along,along+width*.29):
            for h in (.3,1.21,door["clear_height"]-.12):
                a,b = ((fixed-.31,-place,y+h),(fixed+.31,-place,y+h)) if door["axis"]=="x" else ((place,-fixed-.31,y+h),(place,-fixed+.31,y+h))
                assert ray(a,b) is None,(door["id"],a,b)
                apertures += 1
            for delta in (-.28,.28):
                x,z = (fixed+delta,place) if door["axis"]=="x" else (place,fixed+delta)
                stand(x,y,z)
                sills += 1
    controls = [(28,0,74.372),(20,0,58.55),(19,0,60.4),(28,3,37.75),(27,3,36.9),
                (38.5,6,21.7),(39.8,6,22),(30.8,8.25,9.1),(29.5,8.25,9.1),
                (28.2,10.5,14.8),(29.5,10.5,15),(28.2,12.75,9.1),(25.5,15,9.5)]
    for point in controls:
        stand(*point)
    seam_checks = [
        ((39.69,-z,5.675),(40.29,-z,5.675),0,39.81) for z in (15,18,24,28)
    ]+[
        ((x,-40.32,4.85),(x,-39.70,4.85),1,-40.19) for x in (41,43,47,49)
    ]+[
        ((x,-26.32,5.675),(x,-25.7,5.675),1,-26.19) for x in (5,9,15,19)
    ]
    for start,end,axis,expected in seam_checks:
        hit = ray(start,end)
        assert hit is not None and abs(hit[axis]-expected)<.018,(start,end,hit,"foundation closure")
    captures,van = render(layout,VIEWS,m["glb_sha256"])
    import_scene(ROOT/m["source_glb"])
    old,cargo = render(layout,(VIEWS[2],VIEWS[3],VIEWS[4]),m["source_glb_sha256"],"-before")
    assert van==cargo
    write(OUT/"export-verification.json",dict(
        glb_sha256=sha(glb),native_sha256=sha(native),retained_non_terrain_records=len(retained),
        new_closed_parts=len(m["new_interface_parts"]),apertures=apertures,sill_checks=sills,
        standing_controls=len(controls),foundation_closure_rays=len(seam_checks),
        foundation_soil_receivers=sum(len(row["soil_samples"]) for row in m["foundation_founding"]),canonical_van_triangles=van,canonical_van_source=sha(VAN/"geometry.json"),
        captures=captures+old))
    print("LOOKOUT LANDINGS REVIEW",apertures,sills,len(controls),flush=True)


if __name__=="__main__":
    if "--review-only" in sys.argv:
        review()
    else:
        build()
