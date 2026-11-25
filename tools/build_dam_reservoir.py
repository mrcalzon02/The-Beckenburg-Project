"""Build the Blackwater headpond and its founded screened intake."""
from collections import deque
import json
import math
from pathlib import Path
import sys

import bpy

sys.path.insert(0, str(Path(__file__).resolve().parent))
from append_static_glb import append, read as read_glb
from build_circulation_construction import native_write
from build_college_grounds import import_scene
from build_college_electrical import point
from build_dam_valley import BASE, BOUNDARY, grades, inside, rel
from build_deliberate_entrances import solid, write
from build_prison_sally_gate import export
from build_roster_foundations import material, prism
from compose_room_terrain import split_native
from review_roster_macroterrain import records, sha
from verify_college_services import bvh, match_geometry

ROOT = Path(__file__).resolve().parents[1]
PARENT = BASE / "dam-power-pass"
STAGE = BASE / "dam-reservoir-pass"
OUT = STAGE / "wld-09"
TAILRACE = BASE / "dam-tailrace-pass/wld-09/dam-tailrace.water"
STEM = "dam-with-reservoir"
STATUS = "DAM_NATURAL_HEADPOND_AND_FOUNDED_SCREENED_INTAKE_STAGED"
WATER_Y = -4.10
ENVELOPE = (-8.0, -37.0, 35.0, -13.0)
INVERT = -4.29
FOUNDATION = -4.99
SCREEN_X = 25.92
SCREEN_Z0, SCREEN_Z1 = -29.06, -26.94
PORT = (30.14, -4.13, -28.0)


def floating_grid(box, stride):
    x0,z0,x1,z1 = box
    for i in range(1+round((x1-x0)/stride)):
        for j in range(1+round((z1-z0)/stride)):
            yield x0+i*stride,z0+j*stride


def survey(grade):
    x0,z0,x1,z1 = ENVELOPE
    stride = .25
    terrain = {p: grade(*p) for p in floating_grid(ENVELOPE,stride)}
    edges = {p:y for p,y in terrain.items() if p[0] in (x0,x1) or p[1] in (z0,z1)}
    edge_min = min(edges.values())
    assert edge_min > WATER_Y+.135, (edge_min, WATER_Y)
    wet = {p for p,y in terrain.items() if y < WATER_Y}
    queue = deque([(12.,-28.)])
    reachable = set(queue)
    while queue:
        x,z=queue.popleft()
        for neighbour in ((x-stride,z),(x+stride,z),(x,z-stride),(x,z+stride)):
            if neighbour in wet and neighbour not in reachable:
                reachable.add(neighbour)
                queue.append(neighbour)
    assert reachable == wet, ("detached wet cells", len(wet-reachable))
    assert not any(inside(BOUNDARY,x,z) for x,z in wet)
    assert len(wet) > 8500 and min(terrain.values()) < WATER_Y-1.10
    return dict(surface_y=WATER_Y,footprint=ENVELOPE,step=stride,terrain_samples=len(terrain),
                connected_wet_points=len(wet),wet_area_sampled_m2=len(wet)*stride*stride,
                dry_envelope_points=len(edges),minimum_envelope_bank_y=round(edge_min,5),
                lowest_terrain_y=round(min(terrain.values()),5),compound_overlap=0)


def headworks(grade):
    cast = material("Blackwater headworks shuttered concrete",(.39,.422,.407))
    cap = material("Blackwater headworks coping concrete",(.49,.506,.473))
    wear = material("Blackwater dark intake apron concrete",(.31,.341,.321))
    steel = material("Blackwater intake oxidised steel",(.233,.193,.153))
    bearing = material("Blackwater intake recessed guide and sleeve",(.13,.16,.155))
    built=[]

    def body(name, footprint, low, high, finish, role):
        obj=solid("Blackwater headworks "+name,footprint,low,high,finish,role,"headpond")
        built.append(obj)
        return obj

    foundation_box=(25.2,-29.47,30.14,-26.53)
    soil=[grade(*p) for p in floating_grid((25.25,-29.4,30.0,-26.6),.25)]
    assert FOUNDATION < min(soil)-.28
    body("continuous footing and chamber invert",foundation_box,FOUNDATION,INVERT,wear,
         "continuous grade-founded headworks footing")
    # The chamber ends where rising soil reaches the invert. The bank-facing
    # support beyond the bulkhead is one buried solid with a capped outlet.
    chamber_top=-3.60
    for label,z0,z1 in (("north",-29.47,SCREEN_Z0),("south",SCREEN_Z1,-26.53)):
        body(label+" concrete cheek",(25.80,z0,28.72,z1),INVERT,chamber_top,cast,
             "bearing wall under intake cover")
    body("bank-side anchor block",(28.72,-29.47,30.14,-26.53),INVERT,-3.69,cast,
         "backed intake bulkhead and carrier anchor")
    body("continuous cover and bearing head",(25.78,-29.47,30.14,-26.53),-3.69,-3.47,cap,
         "continuous roof bearing on intake walls and bulkhead")

    throat=[grade(*p) for p in floating_grid((25.75,-29,28.5,-27),.25)]
    assert max(throat)<INVERT-.02, max(throat)
    for sign,label in ((-1,"north"),(1,"south")):
        z=-28
        profile=[(23.95,z+sign*2.25),(25.85,z+sign*1.15),
                 (25.85,z+sign*1.47),(24.10,z+sign*2.56)]
        if sign < 0:
            profile.reverse()
        bottoms=[grade(x,q)-.29 for x,q in profile]
        wing=prism("Blackwater headworks "+label+" splayed training wall",profile,-3.83,bottoms,cast)
        wing["role"]="grade-keyed intake training wall"
        wing["zone"]="headpond"
        built.append(wing)
        body(label+" screen channel",(25.86,z+sign*1.057-.037,25.99,z+sign*1.057+.037),
             INVERT,-3.69,bearing,"cast-in steel screen angle")

    body("rack sill",(25.86,SCREEN_Z0,25.98,SCREEN_Z1),INVERT,-4.246,bearing,"seated rack sill")
    body("rack head rail",(25.86,SCREEN_Z0,25.98,SCREEN_Z1),-3.77,-3.686,bearing,"fixed rack head rail")
    for i in range(14):
        z=SCREEN_Z0+.075+i*.15
        body(f"rack vertical {i+1:02d}",(SCREEN_X-.037,z-.025,SCREEN_X+.037,z+.025),
             -4.254,-3.754,steel,"fixed trash rack vertical steel")
    # The receiving flange projects from the buried anchor block. It remains
    # sealed until the continued line and the remote terminal are installed.
    x,y,z=PORT
    body("capped carrier port casing",(x-.17,z-.47,x+.18,z+.47),y-.19,y+.42,bearing,
         "cast-in outlet chamber sleeve")
    body("blind plate to future covered carrier",(x+.148,z-.43,x+.206,z+.43),y-.188,y+.385,steel,
         "blind-ended carrier plate")
    for px in (28.78,29.28,29.98):
        for pz in (-29.35,-28,-26.65):
            assert grade(px,pz)>INVERT-.16 and grade(px,pz)<-3.72
    receipt=dict(foundation_box=foundation_box,foundation_bottom=FOUNDATION,
        foundation_samples=len(soil),foundation_top=INVERT,ground_range=[round(min(soil),5),round(max(soil),5)],
        throat_samples=len(throat),throat_highest_ground=round(max(throat),5),
        screen_axis_x=SCREEN_X,rack_verticals=14,carrier_end=PORT,carrier_state="sealed",
        personnel_portals=0)
    return built,receipt


def west_wing_bearing(grade):
    """Seat the original control and office slabs at the exposed grade break."""
    parts=[]
    contacts=[]
    cast=material("Blackwater west wing shuttered concrete",(.40,.407,.372))
    foundation=material("Blackwater west wing keyed base",(.35,.37,.34))
    fill=material("Blackwater west wing structural fill",(.31,.32,.29))
    slabs=bvh([bpy.data.objects[name] for name in ("Floor office","Floor control")])

    def part(name,box,y0,y1,finish,role):
        obj=solid("Blackwater "+name,box,y0,y1,finish,role,"control-office-foundation")
        parts.append(obj)
        return obj

    def stem(name,box,checks):
        for x,z in checks:
            face,normal,_,_=slabs.ray_cast(point(x,-.34,z),point(0,1,0),.29)
            assert face is not None and abs(face.z+.16)<.002 and normal.z<-.97,(x,z,face)
        part(name,box,-3.93,-.157,cast,"supported west wing concrete stem")
        contacts.append(dict(id=name,footprint=box,slab_contacts=len(checks)))

    # The existing saved cut extends under the two whole upper rooms. Cast
    # the continuous mass into its side banks and up to the adjacent core at
    # X26; the dense fill is enclosed by stems and the original slab.
    soil_samples=[]
    for z in [22+i*.5 for i in range(53)]:
        for x in (7.52,7.73,7.89):
            soil_samples.append(grade(x,z))
    for x in [8+i*.5 for i in range(36)]:
        for z in (21.71,21.84,21.94,48.12,48.22):
            soil_samples.append(grade(x,z))
    assert len(soil_samples)==339 and min(soil_samples)>-3.53 and max(soil_samples)<-2.18
    part("west wing continuous bank-keyed foundation",(7.49,21.70,26,48.23),
         -6.48,-3.89,foundation,"mass foundation tied into three saved terrain banks")
    part("west wing enclosed structural fill",(8.41,22.41,26,47.62),
         -3.92,-.174,fill,"solid fill enclosed beneath upper room slabs")
    part("west wing continuous upper bearing cap",(8.33,22.31,26,47.72),
         -.265,-.157,cast,"continuous slab bearing cap")
    for z0,z1 in zip((21.92,26,30,34,38,42,46),(26,30,34,38,42,46,48)):
        checks=[(8.18,max(22.05,z0+.05)),(8.18,(z0+z1)/2),(8.18,z1-.05)]
        stem("west foundation wall Z"+str(z0),(7.92,z0,8.42,z1),checks)
    for x0,x1 in zip((8.42,13,18,23),(13,18,23,26)):
        checks=[(x,22.19) for x in (x0+.06,(x0+x1)/2,x1-.06)]
        stem("north foundation return X"+str(x0),(x0,21.92,x1,22.42),checks)
    stem("south closure against crest foundation",(8.42,47.6,26,48),
         [(x,47.76) for x in (8.56,14,17,21,25.85)])
    existing=bvh([obj for obj in bpy.context.scene.objects if obj.type=="MESH" and obj not in parts and
                  "contained" in obj.get("role","").lower()])
    for y in (-5.4,-3.2,-1.0):
        for z in (22.23,28,41,47.3):
            join,_,_,_=existing.ray_cast(point(25.85,y,z),point(1,0,0),.5)
            assert join is not None and abs(join.x-26)<.006,(join,y,z)
    return parts,dict(bearing_parts=len(parts),stem_slabs=contacts,
        bank_key_points=len(soil_samples),bank_height_range=[min(soil_samples),max(soil_samples)],
        stair_core_contact_samples=12,foundation_low=-6.48,finished_floor=0)


def water_document():
    previous=TAILRACE.read_bytes()
    assert b"count=1\n" in previous and previous.endswith(b"\n")
    x0,z0,x1,z1=ENVELOPE
    fields={"id":"dam-north-headpond","shape":"SQUARE","x":(x0+x1)/2,"y":WATER_Y,
            "z":(z0+z1)/2,"width":x1-x0,"depth":z1-z0,"yaw":0}
    settings={"bed.depth":1.18,"wave.height":.017,"wave.grid":.63,"wave.speed":.36,
              "shore.height":.004,"foam.opacity":.08,"contact.enabled":"false","spray.enabled":"false"}
    added=list(fields.items())+[("setting."+key,val) for key,val in settings.items()]
    output=previous.replace(b"count=1\n",b"count=2\n")+(
        "".join(f"water.1.{key}={val}\n" for key,val in added)).encode("ascii")
    (OUT/"blackwater.water").write_bytes(output)


def build():
    roster=json.loads((PARENT/"roster.json").read_bytes())
    row=next(source for source in roster["active_sources"] if source["id"]=="WLD-09")
    prior=ROOT/row["construction"]
    meta=json.loads(prior.read_bytes())
    authority=json.loads((PARENT/"checkpoint.json").read_bytes())["project_bytes_sha256"]
    schedules=[prior.with_name(name) for name in ("runtime-input.tsv","door-schedule.tsv","electrical-schedule.tsv")]
    for source in (prior,*schedules,TAILRACE,*(ROOT/row[k] for k in ("glb","native","blend","layout"))):
        assert sha(source)==authority[rel(source)], source

    bpy.ops.wm.open_mainfile(filepath=str(ROOT/row["blend"]))
    bpy.context.preferences.filepaths.save_version=0
    terrain=grades(bpy.data.objects["Blackwater joined valley terrain"],bpy.data.objects[meta["resurfaced_render_mesh"]])
    basin=survey(terrain)
    built,receipt=headworks(terrain)
    stems,wing=west_wing_bearing(terrain)
    built.extend(stems)
    files={ext:OUT/f"{STEM}.{ext}" for ext in ("blend","glb","hcs2")}
    OUT.mkdir(parents=True,exist_ok=True)
    fragment=ROOT/"build/dam-reservoir-addition.glb"
    export(fragment,built)
    append(ROOT/row["glb"],fragment,files["glb"],reuse_embedded_images=True)
    added=native_write(ROOT/row["native"],files["hcs2"],set(),built)
    previous=split_native(ROOT/row["native"])
    actual=split_native(files["hcs2"])
    assert actual[:len(previous)]==previous and len(actual)-len(previous)==len(built)
    original_doc,original_buffer=read_glb(ROOT/row["glb"])
    final_doc,final_buffer=read_glb(files["glb"])
    assert final_buffer.startswith(original_buffer)
    for part in ("nodes","meshes","materials","accessors","bufferViews","images","textures"):
        original=original_doc.get(part,[])
        assert final_doc.get(part,[])[:len(original)]==original,part
    bpy.ops.file.pack_all()
    bpy.ops.wm.save_as_mainfile(filepath=str(files["blend"]),compress=True)
    models=import_scene(files["glb"])
    for name,faces in added:
        assert match_geometry([face for face,_ in faces],[face for face,_ in records(models[name])]),name

    for source in schedules:
        (OUT/source.name).write_bytes(source.read_bytes())
    water_document()
    meta.update(status=STATUS,builder=rel(Path(__file__)),builder_sha256=sha(Path(__file__)),
                previous_construction=rel(prior),source_glb=row["glb"],source_glb_sha256=row["glb_sha256"],
                source_physics=row["native"],source_physics_sha256=row["native_sha256"],
                removed_source_meshes=[],new_interface_parts=[name for name,_ in added],
                retained_native_records=len(previous),reservoir=basin,intake=receipt,west_bearing=wing,
                water_sha256=sha(OUT/"blackwater.water"),
                glb_sha256=sha(files["glb"]),physics_sha256=sha(files["hcs2"]),blend_sha256=sha(files["blend"]))
    write(OUT/"construction.json",meta)
    row.update(construction=rel(OUT/"construction.json"),glb=rel(files["glb"]),native=rel(files["hcs2"]),
               blend=rel(files["blend"]),glb_sha256=meta["glb_sha256"],native_sha256=meta["physics_sha256"])
    next(site for site in roster["sites"] if site["id"]=="WLD-09")["decision"]=STATUS
    roster["baseline"]=rel(PARENT/"roster.json")
    write(STAGE/"roster.json",roster)
    print("PASS surveyed headpond",basin,"| supported intake",receipt,"| west bearing",wing["bearing_parts"],"| added parts",len(added))


if __name__=="__main__":
    build()
