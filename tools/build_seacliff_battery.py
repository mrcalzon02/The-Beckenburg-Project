"""Replace Battery access strips and the director stair pocket with founded assemblies."""
from copy import deepcopy
from pathlib import Path
import math
import sys

import bpy
from mathutils import Vector

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/"tools"))

from append_static_glb import append
from build_cemetery_grounds import replace_terrain_on_export, terrain_surface
from build_coast_entrances import Structure
from build_college_grounds import import_scene, terrain_tree
from build_dam_spillway_gallery import validate_solid
from build_measured_approaches import read, relative, sha, write, fit_doors
from build_roster_foundations import material
from compose_room_terrain import split_native
from export_roster_collision import face_keys, native_faces
from prepare_seacliff_battery import BASE, BASELINE, OLD_GROUND, PASS, SITE
from review_roster_macroterrain import records

STEM = "founded-receiving-stairs"
STATUS = "FOUNDED_BATTERY_STAIRS_AND_TERRACES_RUNTIME_OPEN"
PORTAL = "WLD-17-gallery-north-stair"
RAIL = ROOT/"development/levels/primitive-props-v1/models/level_blockout_guardrail.glb"
# Architectural datums. Each flight occupies one constant-width cell.
FLIGHTS = {
    "route-02": ((32,0,68),(32,2,58),12),
    "route-03": ((40,2,56),(40,4,48),12),
    "route-10": ((31.1,4,22),(38,7,22),17),
}
TERRACE = (30.75,56,41.25,58)
LOWER_BAY = (28.84,20.9,31.1,26)
WELL = (31.1,21.1,38,22.9)


def grade(tree,x,z):
    hit = tree.ray_cast(Vector((x,-z,24)),Vector((0,0,-1)),80)[0]
    assert hit is not None,(x,z,"no saved ground")
    return round(hit.z,6)


def geometry(structure,ground,materials):
    concrete,stairs,wall = materials
    tree = terrain_tree(ground)
    seat = []

    def box(label,bounds,bottom,top,role,zone,mat=concrete):
        return structure.box(label,bounds,bottom,top,mat,role,zone)

    def bedded(label,bounds,lower,top):
        a,b,c,d = bounds
        measurements = [[x,z,grade(tree,x,z)] for x in (a+.05,(a+c)/2,c-.05)
                         for z in (b+.05,(b+d)/2,d-.05)]
        assert all(lower<y<top-.015 for x,z,y in measurements),(label,measurements)
        obj = box(label,bounds,lower,top,"founded open-air receiving terrace","approach")
        seat.append(dict(part=obj.name,bounds=bounds,footing_y=lower,floor_y=top,soil_samples=measurements))

    bedded("intermediate cast concrete switchback court",TERRACE,1.34,2)
    openings = [(PORTAL,30,1.5,2.30,("gallery","director-stair"))]
    structure.face("gallery passage north face","z",26,(24,44),4,3,wall,openings,"gallery")
    door = structure.portals[0]
    door.update(door_type="service",frame_depth=.24,frame_span=[25.88,26.12],frame_center=[30,4,26])

    # The new deck hangs over the two lower casemates. The two deep transfer
    # members sit at the tops of the original walls, above the existing caps.
    # Their soffits remain at the lower rooms' existing head height.
    for i,z in enumerate((21.25,23.55),1):
        box(f"transfer beam {i} over lower casemate walls",(23.95,z-.18,44.05,z+.18),3,3.72,
            "reinforced concrete transfer girder seated on casemate cross walls","director-stair")
    box("lower gallery access and turning slab",LOWER_BAY,3.68,4,
        "reinforced gallery receiving floor","director-stair")
    box("director stair base deck",(31.10,20.9,38,23.1),3.68,4,
        "continuous base deck under single director flight","director-stair")
    for label,bounds in (
        ("west return",(28.84,20.9,29.04,26)),
        ("north side",(29.04,20.9,38,21.1)),
        ("southeast return",(30.9,23.1,31.1,26)),
        ("flight south side",(31.1,22.9,38,23.1)),
    ):
        box(label,bounds,4,6.84,"director stair enclosure under deck","director-stair",wall)

    # Entire upper floor is reconstructed around a single rectangular well.
    # The former west end of the opening is filled over the enlarged landing.
    x0,z0,x1,z1 = WELL
    parts = [
        ((28,10),(x0,10),(x0,26),(28,26)),
        ((x0,10),(x1,10),(x1,z0),(x0,z0)),
        ((x0,z1),(x1,z1),(x1,26),(x0,26)),
        ((x1,10),(40,10),(40,26),(x1,26)),
    ]
    structure.part("director continuous deck and rectangular stair well",parts,6.84,7,concrete,
                   "director structural upper floor","director")

    step_names = []
    for key,(p,q,count) in FLIGHTS.items():
        x,y,z = p
        u,v,w = q
        axis = "x" if u!=x else "z"
        width = .75
        rise = (v-y)/count
        for i in range(count):
            a,b = i/count,(i+1)/count
            low,end = ((x+(u-x)*a,x+(u-x)*b) if axis=="x" else (z+(w-z)*a,z+(w-z)*b))
            bounds = (low,z-width,end,z+width) if axis=="x" else (x-width,end,x+width,low)
            top = y+rise*(i+1)
            # The protected flight bears on its floor. Exterior shallow risers
            # seat on a continuous cast sloping foundation and its own cheeks.
            bottom = 3.84 if axis=="x" else y+rise*i-.23
            step = box(f"{key} cast nosing {i+1:02}",bounds,bottom,top,"structural-stair-step",
                       "director-stair" if axis=="x" else "approach",stairs)
            step["link"] = key
            step_names.append(step.name)
        if axis=="x":
            continue

        def sloping(label,left,right,low,high,role):
            obj = box(label,(left,w,right,z),y+low,y+high,role,"approach")
            for vertex in obj.data.vertices:
                vertex.co.z += (z+vertex.co.y)*(v-y)/(z-w)
            return obj

        found = sloping(key+" continuous founded stair spine",x-.75,x+.75,-.65,-.14,
                        "sloping reinforced exterior stair foundation")
        samples = [[wx,wz,grade(tree,wx,wz)] for wx in (x-.63,x,x+.63)
                   for wz in [w+.35,(z+w)*.5,z-.35]]
        for wx,wz,fy in samples:
            base = y+(z-wz)*(v-y)/(z-w)
            assert base-.70 < fy < base+.07,(key,wx,wz,fy,base)
        seat.append(dict(part=found.name,soil_samples=samples,lower_nosing=p,upper_nosing=q))
        for side,a,c in (("west",x-.95,x-.75),("east",x+.75,x+.95)):
            sloping(key+" "+side+" anchored parapet",a,c,-.65,1.23,"founded stair sidewall")

    prior = set(bpy.context.scene.objects)
    bpy.ops.import_scene.gltf(filepath=str(RAIL))
    guard_template = next(o for o in set(bpy.context.scene.objects)-prior if o.type=="MESH").data.copy()
    for obj in set(bpy.context.scene.objects)-prior:
        bpy.data.objects.remove(obj,do_unlink=True)
    for label,a,b in (("north",(x0-.04,z0-.075),(x1-.10,z0-.075)),
                      ("south",(x0-.04,z1+.075),(x1-.10,z1+.075)),
                      ("lower end",(x0-.075,z0-.075),(x0-.075,z1+.075))):
        ax,az = a
        bx,bz = b
        length = math.dist(a,b)
        nx,nz = (bx-ax)/length,(bz-az)/length
        count = math.ceil(length/2)
        for i in range(count):
            mesh = guard_template.copy()
            obj = bpy.data.objects.new(f"Battery director well {label} guard {i+1}",mesh)
            bpy.context.collection.objects.link(obj)
            for vertex in mesh.vertices:
                x,y,z = vertex.co
                t = ((x+1)/2+i)*length/count
                vertex.co = (ax+nx*t+nz*y,-(az+nz*t-nx*y),6.98+z)
            obj["role"],obj["source_asset"],obj["zone"] = "floor-mounted indexed guard",RAIL.stem,"director"
            structure.built.append(obj)
    return seat,step_names


def derive(layout,metadata,portal):
    previous = deepcopy(metadata["source_segment_map"])
    changed = {}
    links = {l["id"]:l for l in layout["links"]}
    study = read(BASE/"flush-porch-and-threshold-pass/verification/seacliff-landing-study.json")
    paths = {
        "route-01": layout["staging"]["cargo_route"],
        "route-02": [[32,0,75],[32,0,68],list(FLIGHTS["route-02"][1])],
        "route-03": [list(FLIGHTS["route-02"][1]),[32,2,57],[40,2,57],
                     *map(list,FLIGHTS["route-03"][:2])],
        "route-10": [[30,4,30],[30,4,26],[30,4,22],*map(list,FLIGHTS["route-10"][:2])],
        **{key:turn["after"] for key,turn in study["trial_turns"].items()},
    }
    for key,points in paths.items():
        link = links[key]
        changed[key] = deepcopy(link)
        link.update(points=points,note="Circulation guide across actual grounds, framed openings and supported stair landings.")
        old = [i for i,(a,b) in enumerate(zip(changed[key]["points"],changed[key]["points"][1:])) if abs(a[1]-b[1])>.01]
        new = [i for i,(a,b) in enumerate(zip(points,points[1:])) if abs(a[1]-b[1])>.01]
        assert len(old)==len(new),(key,old,new)
        if old:
            refs = previous.get(key,{})
            metadata["source_segment_map"][key] = {str(c):refs.get(str(a),a) for a,c in zip(old,new)}
    layout.setdefault("authored_doorways",[]).append(portal)
    fit_doors(layout,links,{"route-10":PORTAL})
    metadata["before_segment_map"] = previous
    layout["status"] = STATUS
    layout["arrival_and_director_stair"] = dict(
        forecourt_y=0,patrol_court_y=4,intermediate_terrace=TERRACE,intermediate_terrace_y=2,
        protected_lower_stair_bay=LOWER_BAY,upper_well=WELL,well_datum=7,stair_access_portal=PORTAL,
        clear_stair_width=1.5,guide_has_no_geometry=True)
    return changed


def build(target):
    source = next(row for row in read(BASELINE)["active_sources"] if row["id"]==SITE)
    before_glb, before_hcs = ROOT/source["glb"], ROOT/source["native"]
    layout, meta = read(ROOT/source["layout"]), read(ROOT/source["construction"])
    assert sha(before_glb)==source["glb_sha256"] and sha(before_hcs)==source["native_sha256"]
    program,world = target/"program"/SITE.lower(),target/"worlds"/SITE.lower()/"terrain-world"
    expected = split_native(OLD_GROUND/"terrain-physics.hcs2")
    assert [p for p in split_native(before_hcs) if p[0].startswith("terrain-")]==expected
    folder = target/SITE.lower()
    folder.mkdir(parents=True,exist_ok=True)
    objects = import_scene(before_glb)
    old_ground = next(o for o in objects.values() if o.get("role","").startswith("saved editor terrain"))
    ground = terrain_surface(SITE,old_ground,world,program)
    ground["role"] = "saved editor terrain / Battery forecourt and protected stair beds"
    ground["saved_world"] = relative(world)
    ground.data.materials[-1].name = "Battery weathered compacted yard aggregate"
    ground.data.materials[-1].diffuse_color = (.41,.40,.35,1)
    structure = Structure("Battery",objects)
    # Keep other physical blockout areas independently available for review.
    structure.retired = {old_ground.name}
    structure.retired.update(name for name,obj in objects.items() if
       obj.get("link") in FLIGHTS and obj.get("role")=="structural-stair-step"
       or obj.get("zone")=="director" and obj.get("role")=="floor")
    structure.retired.update(("route-01 support 0","route-02 landing0","route-03 landing0","route-10 landing0",
        "route-04 support 0","route-05 support 0","East stair hub bearing ledge 0 region0",
        "Arrival end bearing WLD-17 0",
        "Bay landing WLD-17 route-02 2 region0","Bay landing WLD-17 route-02 2 region1",
        "Bay landing WLD-17 route-03 1 region0","Bay landing WLD-17 route-03 2 region0"))
    structure.retire_faces(("gallery","N"))
    assert structure.retired<=objects.keys(),sorted(structure.retired-objects.keys())
    concrete = material("Battery cast concrete",(.435,.428,.405))
    stair_surface = material("Battery worn concrete stair treads",(.49,.48,.45))
    seats,new_steps = geometry(structure,ground,(concrete,stair_surface,objects["Flat route shell Wall gallery N"].data.materials[0]))
    routes = derive(layout,meta,structure.portals[0])

    bpy.data.objects.remove(structure.template,do_unlink=True)
    for name in sorted(structure.retired):
        bpy.data.objects.remove(objects[name],do_unlink=True)
    for obj in structure.built:
        validate_solid(obj)
    bpy.context.view_layer.update()
    before_stairs = meta["stair_components"]
    normalized = {n.split(":",1)[-1] for n in before_stairs}
    assert sum(name in normalized for name in structure.retired)>=41
    stairs = [name for name in before_stairs if name.split(":",1)[-1] not in structure.retired]+new_steps
    glb,native,blend = (folder/(STEM+ext) for ext in (".glb",".hcs2",".blend"))
    bpy.ops.wm.save_as_mainfile(filepath=str(blend),compress=True)
    bpy.ops.object.select_all(action="DESELECT")
    for obj in [ground,*structure.built]:
        obj.select_set(True)
    addition = ROOT/"build/seacliff-receiving/architecture-addition.glb"
    bpy.ops.export_scene.gltf(filepath=str(addition),export_format="GLB",export_extras=True,use_selection=True)
    retention = append(before_glb,addition,glb,remove_mesh_names=sorted(structure.retired))
    replace_terrain_on_export(before_hcs,native,structure.retired,structure.built,world,False)
    written = split_native(native)
    kept = [p for p in split_native(before_hcs) if not p[0].startswith("terrain-")
            and p[0].split(":",1)[-1] not in structure.retired]
    assert written[:len(kept)]==kept
    assert [p for p in written if p[0].startswith("terrain-")]==split_native(world/"terrain-physics.hcs2")
    verification = {o.name:face_keys(records(o)) for o in [ground,*structure.built]}
    ground_name = ground.name
    reopened = import_scene(glb)
    for name,signatures in verification.items():
        assert face_keys(records(reopened[name]))==signatures,(name,"render/author parity")
    assert face_keys(records(reopened[ground_name]))==face_keys(native_faces(world/"terrain-physics.hcs2"))
    laid = folder/"authored-layout.json"
    write(laid,layout)
    meta.update(status=STATUS,layout=relative(laid),layout_sha256=sha(laid),before_layout=source["layout"],
        before_layout_sha256=sha(ROOT/source["layout"]),source_glb=source["glb"],source_glb_sha256=source["glb_sha256"],
        source_physics=source["native"],source_physics_sha256=source["native_sha256"],before_stair_components=before_stairs,
        stair_components=stairs,glb_sha256=sha(glb),physics_sha256=sha(native),blend_sha256=sha(blend),
        saved_world=relative(world),saved_world_sha256=sha(world/"terrain-physics.hcs2"),
        retired_objects=sorted(structure.retired),reconstructed_components=list(verification),
        exact_retained_native_parts=len(kept),retained_glb_resources=retention,footing_contacts=seats,
        physical_stair_cells=FLIGHTS,derived_source_routes=routes,generator_sha256=sha(Path(__file__)))
    write(folder/"construction.json",meta)
    print("BATTERY FOUNDED STAIRS",len(verification),"subassemblies",len(kept),"retained parts",flush=True)


if __name__=="__main__":
    args=sys.argv[sys.argv.index("--")+1:] if "--" in sys.argv else []
    build((ROOT/args[0]).resolve() if args else PASS)
