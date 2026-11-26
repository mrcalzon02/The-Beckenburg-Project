"""Fit the switchgear generator's exhaust and baffled outside-air assembly."""
import json
import math
from pathlib import Path
import sys

import bpy
from mathutils import Matrix, Vector

sys.path.insert(0,str(Path(__file__).resolve().parent))
from append_static_glb import append
from build_circulation_construction import native_write
from build_college_containment import MeshParts
from build_college_electrical import Assembly, bounds, point, world
from build_college_grounds import import_scene
from build_dam_carrier import BASE, ROOT, shell
from build_dam_spillway_gallery import validate_solid
from build_dam_valley import rel
from build_deliberate_entrances import solid, write
from build_prison_sally_gate import export
from build_roster_foundations import material
from compose_room_terrain import split_native
from review_roster_macroterrain import records, sha
from verify_college_services import bvh, match_geometry

PARENT=BASE/"dam-carrier-pass"
STAGE=BASE/"dam-ventilation-pass"
OUT=STAGE/"wld-09"
STEM="dam-with-ventilation"
STATUS="DAM_GENERATOR_EXHAUST_AND_BAFFLED_AIR_STAGED"
WALL="Wall plant N"
INNER,OUTER=.037,.052
X=40.42
FLUE_Y=-4.05
AIR=(43.86,44.68,-4.98,-4.29)
SOCKET=(X,-5.07,8.75)


def rings(positions,radius):
    frames=[]
    for i,p in enumerate(positions):
        axis=(positions[min(i+1,len(positions)-1)]-positions[max(i-1,0)]).normalized()
        side=Vector((1,0,0))
        vertical=axis.cross(side).normalized()
        frames.append([p+radius*(math.cos(math.tau*j/16)*side+math.sin(math.tau*j/16)*vertical)
                       for j in range(16)])
    return frames


def exhaust(center):
    # A fabricated flange fits the canonical muffler. Keep the starter and
    # rear electrical feed untouched. The first straight segment ends at a sweep.
    controls=[point(*p) for p in (center,(X,-4.05,8.75),(X,-4.05,7.54),
                                  (X,-2.29,7.54),(X,-2.29,7.12),(X,-2.60,7.12))]
    route=[controls[0]]
    for a,b,c in zip(controls,controls[1:],controls[2:]):
        r=min(.14,(b-a).length*.33,(c-b).length*.33)
        u=b-(b-a).normalized()*r
        v=b+(c-b).normalized()*r
        for i in range(8):
            t=i/7
            route.append(u*(1-t)**2+b*(2*t*(1-t))+v*t*t)
    route.append(controls[-1])
    return route


def fit():
    original=bpy.data.objects[WALL]
    assert bounds([original])==[[36.,-6.,7.92],[54.,-3.,8.08]]
    facade=bvh([original])
    gen=next(o for o in bpy.context.scene.objects if "auxiliary maintenance generator" in o.name
             and o.get("source_asset")=="level_trace_generator_refined")
    source=bvh([gen])
    local=point(X,-4.98,8.75)
    root,n,_,_=source.ray_cast(local,point(0,-1,0),.14)
    assert root is not None and n.z>.96 and abs(root.z-SOCKET[1])<.006,(root,n)
    for x in (X-.025,X+.025):
        for z in (8.728,8.772):
            hit,_,_,_=source.ray_cast(point(x,-4.98,z),point(0,-1,0),.14)
            assert hit is not None and abs(hit.z-root.z)<.007

    supports=[]
    for x,y in ((X,-3.97),(X,-3.37),(43.84,-4.44),(44.70,-4.80)):
        for dz,start,wanted in ((1,7.64,7.92),(-1,8.34,8.08)):
            p,n,_,_=facade.ray_cast(point(x,y,start),point(0,0,dz),.5)
            assert p is not None and abs(-p.y-wanted)<.002 and n.dot(point(0,0,dz))<-.98
            supports.append(world(p))

    masonry=original.data.materials[0]
    steel=material("Blackwater generator aged stainless flue",(.27,.30,.30))
    dark=material("Blackwater switchgear intake dark louvers",(.19,.22,.209))
    frame=material("Blackwater ventilation flanges",(.32,.343,.329))
    built=[]

    def box(name,rect,low,high,finish=frame):
        obj=solid("Blackwater ventilation "+name,rect,low,high,finish,
                  "measured mechanical assembly or exterior wall closure","plant")
        built.append(obj)

    # Reinstate the whole original wall envelope, with two named openings
    # formed into full-height real piers and reinforced heads.
    a,b,low,high=AIR
    for label,x0,x1 in (("west pier",36,X-.11),("service pier",X+.11,a),("east pier",b,54)):
        box(label,(x0,7.92,x1,8.08),-6,-3,masonry)
    for label,x0,x1,y0,y1 in (("flue",X-.11,X+.11,FLUE_Y-.11,FLUE_Y+.11),("air",a,b,low,high)):
        box(label+" sill",(x0,7.92,x1,8.08),-6,y0,masonry)
        box(label+" head",(x0,7.92,x1,8.08),y1,-3,masonry)

    line=[point(X,FLUE_Y,z) for z in (8.08,7.92)]
    insert=rings(line,OUTER-.001)
    square=[]
    for center,row in zip(line,insert):
        square.append([center+(p-center)*(.11/max(abs(p.x-center.x),abs(p.z-center.z))) for p in row])
    built.append(shell("Blackwater ventilation cast flue sleeve",insert,square,masonry,"solid fitted wall sleeve"))
    route=exhaust(world(root))
    flue=shell("Blackwater ventilation swept exhaust with downward outlet",
               rings(route,INNER),rings(route,OUTER),steel,"continuous enclosed mechanical exhaust")
    built.append(flue)
    for name,ends,r0,r1 in (
        ("muffler adapter",[(X,root.z-.006,8.75),(X,root.z+.067,8.75)],INNER,.068),
        ("indoor collar",[(X,FLUE_Y,8.09),(X,FLUE_Y,8.13)],OUTER-.002,.086),
        ("weather collar",[(X,FLUE_Y,7.87),(X,FLUE_Y,7.93)],OUTER-.002,.083),
    ):
        p=[point(*q) for q in ends]
        built.append(shell("Blackwater ventilation "+name,rings(p,r0),rings(p,r1),frame,"supported flue flange"))
    # Outboard wall ties carry the riser. Keep the fasteners to the sides of
    # the round core so the braces never become a hidden obstruction.
    for y in (-3.92,-3.36):
        for sign in (-1,1):
            c=X+sign*.058
            box("external riser bearer",(c-.013,7.55,c+.013,7.927),y-.028,y+.028)
        p=[point(X,y-.028,7.54),point(X,y+.028,7.54)]
        built.append(shell("Blackwater ventilation fitted stack saddle",rings(p,OUTER-.002),rings(p,.076),
                           frame,"wall-supported flue clamp"))

    for x0,x1 in ((a-.023,a+.036),(b-.036,b+.023)):
        box("louver jamb",(x0,7.72,x1,8.192),low-.02,high+.019)
    box("louver hood",(a-.027,7.63,b+.027,8.097),high-.037,high+.042)
    box("louver drain sill",(a-.02,7.71,b+.02,8.18),low-.025,low+.037)
    # Each folded blade drops toward the outside and overlaps the next in
    # elevation. Air takes the sloped gap while a horizontal ray meets steel.
    for i in range(6):
        y=low+.029+i*.102
        poly=[(a+.035,7.76),(b-.035,7.76),(b-.035,8.145),(a+.035,8.145)]
        parts=MeshParts([dark])
        height=[y,y,y+.092,y+.092]
        parts.prism(poly,height,[v+.016 for v in height],0)
        built.append(parts.frame(f"Blackwater ventilation weather blade {i+1:02d}","weather-shedding intake blade"))
    bpy.data.objects.remove(original,do_unlink=True)
    for obj in built:
        obj["zone"]="plant"
        validate_solid(obj)
    return built,dict(muffler_contact=world(root),wall_supports=supports,through_wall=[7.92,8.08],
        intake_opening=AIR,flue_axis=[world(v) for v in route],intake_blades=6,
        combustion_air_and_exhaust_are_separate=True,ambient_airflow_simulated=False)



def furnish():
    assembly = Assembly()
    installed = []
    for asset, zone, label, x, y, z in (
        ("level_workbench_worn", "control", "dispatch desk", 12.5, 0, 37.1),
        ("level_workbench_worn", "office", "shift records desk", 12.5, 0, 25.5),
        ("level_work_repair_bench", "workshop", "maintenance bench", 39.8, -6, 54.2),
    ):
        parts = assembly.place(asset, Matrix.Translation(point(x,y,z)),
                               label, "floor-bearing dam furnishings", zone)
        low, high = bounds(parts)
        assert abs(low[1]-y)<.008, (label,low)
        for px in (low[0]+.025,high[0]-.025):
            for pz in (low[2]+.025,high[2]-.025):
                contact, _, _ = assembly.measured(point(px,y+.18,pz),point(0,-1,0),.25,label)
                assert abs(contact.z-y)<.008, (label,contact)
        installed.append(dict(asset=asset,zone=zone,label=label,bounds=[low,high]))
        if zone=="control":
            hit, normal, _, _ = bvh(parts).ray_cast(point(x,y+1.6,z),point(0,-1,0),1.5)
            assert hit is not None and normal.z>.98, "Dispatch desk needs a supported radio"
            radio = assembly.place("level_event_radio_worn",Matrix.Translation(point(x,hit.z+.002,z)),
                                   "dispatch radio","working control-room radio",zone)
            assert len(radio)==1
            radio[0].name="Blackwater dispatch radio"
            base, top = bounds(radio)
            assert abs(base[1]-hit.z)<.012, (base,hit)
            face, _, _, _ = bvh(radio).ray_cast(point(x,base[1]+.075,top[2]+.8),point(0,0,-1),1.6)
            assert face is not None, "Dispatch radio needs an exposed casing"
            fx, fy, fz = world(face)
            device = ("DEVICE",radio[0].name,"RADIO",0,fx,fy,fz+.035,0)
    for part in assembly.built:
        part.name=part.name.replace("College ","Blackwater ",1)
    return assembly.built, installed, device


def build():
    roster=json.loads((PARENT/"roster.json").read_bytes())
    row=next(r for r in roster["active_sources"] if r["id"]=="WLD-09")
    prior=ROOT/row["construction"]
    meta=json.loads(prior.read_bytes())
    authority=json.loads((PARENT/"checkpoint.json").read_bytes())["project_bytes_sha256"]
    schedules=[prior.with_name(name) for name in (
        "runtime-input.tsv","door-schedule.tsv","electrical-schedule.tsv","blackwater.water","carrier-profile.tsv")]
    for path in (prior,*schedules,*(ROOT/row[k] for k in ("glb","native","blend","layout"))):
        assert sha(path)==authority[rel(path)],path
    bpy.ops.wm.open_mainfile(filepath=str(ROOT/row["blend"]))
    bpy.context.preferences.filepaths.save_version=0
    built,service=fit()
    furniture, furnishing_schedule, device = furnish()
    built.extend(furniture)
    OUT.mkdir(parents=True,exist_ok=True)
    files={ext:OUT/f"{STEM}.{ext}" for ext in ("glb","hcs2","blend")}
    fragment=ROOT/"build/dam-ventilation-fragment.glb"
    export(fragment,built)
    append(ROOT/row["glb"],fragment,files["glb"],remove_mesh_names={WALL},reuse_embedded_images=True)
    added=native_write(ROOT/row["native"],files["hcs2"],{WALL},built,
                       open_components={obj.name for obj in furniture})
    previous=split_native(ROOT/row["native"])
    retained=[part for part in previous if part[0].split(":",1)[-1]!=WALL]
    assert len(previous)-len(retained)==1
    assert split_native(files["hcs2"])[:len(retained)]==retained
    bpy.ops.file.pack_all()
    bpy.ops.wm.save_as_mainfile(filepath=str(files["blend"]),compress=True)
    objects=import_scene(files["glb"])
    for name,tris in added:
        assert match_geometry([f for f,_ in tris],[f for f,_ in records(objects[name])]),name
    for path in schedules:
        content=path.read_bytes()
        if path.name=="runtime-input.tsv":
            content=content.rstrip(b"\r\n")+b"\n"+"\t".join(map(str,device)).encode("utf-8")+b"\n"
        (OUT/path.name).write_bytes(content)
    meta.update(status=STATUS,previous_construction=rel(prior),builder=rel(Path(__file__)),builder_sha256=sha(Path(__file__)),
                source_glb=row["glb"],source_glb_sha256=row["glb_sha256"],
                source_physics=row["native"],source_physics_sha256=row["native_sha256"],
                glb_sha256=sha(files["glb"]),blend_sha256=sha(files["blend"]),physics_sha256=sha(files["hcs2"]),
                removed_source_meshes=[WALL],retained_native_records=len(retained),
                new_interface_parts=[name for name,_ in added],generator_ventilation=service,
                furnishings=furnishing_schedule,dispatch_radio=list(device))
    write(OUT/"construction.json",meta)
    row.update(construction=rel(OUT/"construction.json"),glb=rel(files["glb"]),native=rel(files["hcs2"]),
               blend=rel(files["blend"]),glb_sha256=meta["glb_sha256"],native_sha256=meta["physics_sha256"])
    next(r for r in roster["sites"] if r["id"]=="WLD-09")["decision"]=STATUS
    roster["baseline"]=rel(PARENT/"roster.json")
    write(STAGE/"roster.json",roster)
    print("PASS",len(added),"ventilation and wall pieces on",len(service["wall_supports"]),"measured wall faces")


if __name__=="__main__":
    build()
