"""Fit the Blackwater closed hydraulic carrier to the reservoir and tailrace terminals."""
import json
import math
from pathlib import Path
import sys

import bmesh
import bpy
from mathutils import Vector

sys.path.insert(0, str(Path(__file__).resolve().parent))
from append_static_glb import append, read as read_glb
from build_circulation_construction import native_write
from build_college_containment import MeshParts
from build_college_electrical import point
from build_college_grounds import import_scene
from build_dam_reservoir import BASE, ROOT
from build_dam_spillway_gallery import validate_solid
from check_roster_decks import read_parts
from build_dam_valley import grades, rel
from build_deliberate_entrances import solid, write
from build_prison_sally_gate import export
from build_roster_foundations import material
from compose_room_terrain import split_native
from dam_carrier_terrain import INTAKE_BOX, TAILRACE_BOX, VALLEY_NAME, replace_native, replace_surface
from prepare_cemetery_doors import write_tsv
from review_roster_macroterrain import records, sha
from verify_college_services import bvh, match_geometry

PARENT = BASE / "dam-reservoir-pass"
STAGE = BASE / "dam-carrier-pass"
OUT = STAGE / "wld-09"
STEM = "dam-with-carrier"
STATUS = "DAM_COVERED_HYDRAULIC_CARRIER_AND_ISOLATED_TERMINALS_STAGED"
INNER = .218
OUTER = .270
DIRECTIONS = 16
CONTROL = (
    (28.70,-4.04,-28), (30.62,-4.04,-28), (33,-5.24,-28),
    (44,-5.85,-24), (62,-8.75,-18), (82,-10.18,-10), (96,-8.89,0),
    (99,-7.52,14), (97,-7.62,31), (94,-8.05,45), (88,-8.30,53),
    (80,-8.48,54), (75.7,-8.42,51), (73.89,-8.16,47.8),
    (73.89,-8.05,46.18), (73.89,-6.93,45.06), (73.89,-6.93,44.74),
)
INTAKE_PREFIX = "Entrance rebuild Blackwater headworks "
REMOVED = {
    INTAKE_PREFIX+"bank-side anchor block",
    INTAKE_PREFIX+"capped carrier port casing",
    INTAKE_PREFIX+"blind plate to future covered carrier",
}


def grade_aligned_path():
    control=[point(*p) for p in CONTROL]
    bends=[control[0]]
    for a,b,c in zip(control,control[1:],control[2:]):
        distance=min(.74,(b-a).length*.23,(c-b).length*.23)
        u=b-(b-a).normalized()*distance
        v=b+(c-b).normalized()*distance
        for i in range(9):
            t=i/8
            bends.append(u*(1-t)**2+b*(2*t*(1-t))+v*t*t)
    bends.append(control[-1])
    return bends


def survey_stations(bends):
    path=[bends[0]]
    for a,b in zip(bends,bends[1:]):
        count=max(1,math.ceil((b-a).length/.38))
        path.extend(a.lerp(b,i/count) for i in range(1,count+1))
    return path


def ring(center, axis, radius):
    lateral=axis.cross(Vector((0,0,1))).normalized()
    crown=lateral.cross(axis).normalized()
    return [center+radius*(lateral*math.cos(math.tau*i/DIRECTIONS)+
                           crown*math.sin(math.tau*i/DIRECTIONS)) for i in range(DIRECTIONS)]


def shell(name,inner,outer,finish,role):
    assert len(inner)==len(outer) and len(inner)>=2
    n=len(inner[0])
    count=len(inner)
    vertices=[vertex for row in inner+outer for vertex in row]
    faces=[]
    for offset in (0,count*n):
        for row in range(count-1):
            for i in range(n):
                j=(i+1)%n
                a=offset+row*n
                faces.append((a+i,a+j,a+n+j,a+n+i))
    for a in (0,(count-1)*n):
        for i in range(n):
            j=(i+1)%n
            faces.append((a+i,a+j,count*n+a+j,count*n+a+i))
    mesh=bpy.data.meshes.new(name)
    mesh.from_pydata(vertices,[],faces)
    mesh.update()
    body=bmesh.new()
    body.from_mesh(mesh)
    bmesh.ops.recalc_face_normals(body,faces=list(body.faces))
    assert body.calc_volume(signed=True)>0
    body.to_mesh(mesh)
    body.free()
    obj=bpy.data.objects.new(name,mesh)
    bpy.context.scene.collection.objects.link(obj)
    mesh.materials.append(finish)
    obj["role"]=role
    obj["zone"]="hydraulic-carrier"
    return obj


def cast_wall(name,centers,axis,side,low,high,half,finish):
    lateral=axis.cross(Vector((0,0,1))).normalized()
    crown=lateral.cross(axis).normalized()
    angles={math.tau*i/DIRECTIONS for i in range(DIRECTIONS)}
    for height in (low,high):
        for half_side in (-half,half):
            corner=side*half_side+Vector((0,0,height-centers[0].z))
            angles.add(math.atan2(corner.dot(crown),corner.dot(lateral))%math.tau)
    unit=[lateral*math.cos(a)+crown*math.sin(a) for a in sorted(angles)]
    rings=[[p+d*(INNER+.0015) for d in unit] for p in centers]
    outer=[]
    for center,inner in zip(centers,rings):
        row=[]
        for vertex in inner:
            offset=vertex-center
            limit=half/abs(offset.dot(side)) if abs(offset.dot(side))>.00001 else math.inf
            rise=offset.z
            vertical=((high if rise>0 else low)-center.z)/rise if abs(rise)>.00001 else math.inf
            row.append(center+offset*min(limit,vertical))
        outer.append(row)
    return shell(name,rings,outer,finish,"concrete terminal bulkhead cast around an open bore")


def install(soil):
    concrete=material("Blackwater carrier formed concrete",(.401,.418,.393))
    steel=material("Blackwater buried steel carrier",(.245,.273,.245))
    cap=material("Blackwater carrier inspection saddle concrete",(.435,.447,.416))
    route=grade_aligned_path()
    directions=[(route[min(i+1,len(route)-1)]-route[max(i-1,0)]).normalized()
                for i in range(len(route))]
    inner=[ring(p,d,INNER) for p,d in zip(route,directions)]
    outer=[ring(p,d,OUTER) for p,d in zip(route,directions)]
    geometric_stations=len(route)
    tube=shell("Blackwater continuous open-bore hydraulic carrier",inner,outer,steel,
               "continuous steel pressure carrier outside personnel rooms")
    built=[tube]

    def box(name,coords,base,top,finish=concrete):
        obj=solid("Blackwater carrier "+name,coords,base,top,finish,
                  "founded carrier terminal construction","hydraulic-carrier")
        built.append(obj)
        return obj

    east=point(1,0,0)
    intake_axis=[point(x,-4.04,-28) for x in (28.72,30.14)]
    built.append(cast_wall("Blackwater intake bulkhead with fitted bore",intake_axis,east,point(0,0,1),
                           -4.29,-3.69,1.47,concrete))
    # The concrete top is laterally supported, then drops into the bank as the
    # pressure line descends. The collar encloses the visible bank transition.
    box("intake extension apron",(30.13,-28.62,31.06,-27.38),-4.88,-4.63)
    for z0,z1 in ((-28.62,-28.286),(-27.714,-27.38)):
        box("intake collar side",(30.13,z0,31.06,z1),-4.64,-3.72)
    box("intake collar cover",(30.13,-28.62,31.06,-27.38),-3.72,-3.55,cap)
    def slope(name,coords,bottom,top,finish):
        x0,z0,x1,z1=coords
        shape=[(x0,z0),(x1,z0),(x1,z1),(x0,z1)]
        pieces=MeshParts([finish])
        pieces.prism(shape,bottom,top,0)
        obj=pieces.frame("Blackwater carrier "+name,"formed sloped carrier terminal construction")
        obj["zone"]="hydraulic-carrier"
        built.append(obj)

    intake_top=[-3.55,-4.04,-4.04,-3.55]
    intake_soffit=[y-.186 for y in intake_top]
    slope("tapered bank cover",(31.06,-28.61,34,-27.39),intake_soffit,intake_top,cap)
    for z0,z1 in ((-28.61,-28.301),(-27.699,-27.39)):
        middle=intake_soffit[0]+(intake_soffit[1]-intake_soffit[0])*(32.4-31.06)/(34-31.06)
        slope("tapered bank bearing",(31.06,z0,32.4,z1),-5.40,
              [y+.004 for y in (intake_soffit[0],middle,middle,intake_soffit[0])],concrete)
        slope("bank cover shallow grade key",(32.4,z0,34,z1),-4.34,
              [y+.004 for y in (middle,intake_soffit[1],intake_soffit[1],middle)],concrete)

    # The receiving chamber ends just behind the original closed sluice.
    # It bears on the existing invert at the seam. The isolation leaf stays
    # fixed and the western catwalk cannot descend into the race.
    box("tailrace receiving toe",(72.72,44.56,75.06,44.99),-8.43,-7.18)
    box("tailrace west receiving wall",(72.72,44.56,73.00,45.12),-7.18,-6.17)
    box("tailrace east receiving wall",(74.78,44.56,75.06,45.12),-7.18,-6.17)
    box("tailrace receiving cover",(73,44.56,74.78,45.12),-6.38,-6.165,cap)
    axis=point(0,0,-1)
    downstream=[point(73.89,-6.93,z) for z in (45.01,44.77)]
    built.append(cast_wall("Blackwater receiving headwall with open carrier sleeve",downstream,axis,point(1,0,0),
                           -7.18,-6.38,.89,concrete))
    # Both terminal grading changes are underneath continuous covers.
    # Four small shoes transfer the short exposed lengths into concrete, and
    # the remainder of the line bears on enclosed ground.
    tail_top=[-6.165,-6.165,-6.61,-6.61]
    tail_soffit=[y-.175 for y in tail_top]
    for x0,x1 in ((73.10,73.55),(74.23,74.68)):
        slope("receiving bench side wall",(x0,45.12,x1,46.40),-8.60,
              [y+.004 for y in tail_soffit],concrete)
    slope("tapered receiving bench cover",(73.10,45.12,74.68,46.40),tail_soffit,tail_top,cap)
    box("sealed receiving bench grade return",(73.10,46.37,74.68,46.57),-7.63,-6.607)
    bore=bvh([tube])
    shoes=((31.16,-28,.057,.43,-4.97),(31.52,-28,.057,.43,-5.35),
           (73.89,45.58,.42,.048,-8.24),(73.89,46.04,.42,.048,-8.58))
    for i,(x,z,dx,dz,bottom) in enumerate(shoes,1):
        hit,_,_,_=bore.ray_cast(point(x,bottom+.03,z),point(0,1,0),1.4)
        assert hit is not None and hit.z>bottom+.08,(x,z,bottom,hit)
        box(f"bench collar shoe {i}",(x-dx,z-dz,x+dx,z+dz),bottom,hit.z+.006)
    for obj in built:
        validate_solid(obj)

    route=survey_stations(route)
    directions=[(route[min(i+1,len(route)-1)]-route[max(i-1,0)]).normalized()
                for i in range(len(route))]
    profile=[]
    length=0
    for i,(p,axis) in enumerate(zip(route,directions)):
        if i:
            length+=(p-route[i-1]).length
        x,y,z=p.x,p.z,-p.y
        lateral=axis.cross(Vector((0,0,1))).normalized()
        earth=[soil((p+lateral*d).x,-(p+lateral*d).y) for d in (-.41,0,.41)]
        clearance=min(earth)-y-OUTER
        region="buried"
        if x<33 and z<-27:
            region="intake collar"
        elif x<76 and z>=44.70 and clearance<.56:
            region="receiving chamber"
        else:
            assert clearance>=.56,(i,x,y,z,earth,clearance)
        assert not (28<x<73 and 8<z<62),("Entered powerhouse footprint",x,y,z)
        profile.append((i,round(length,5),round(x,6),round(y,6),round(z,6),
                        round(min(earth),6),round(clearance,6),region))
    burial=[p for p in profile if p[-1]=="buried"]
    assert len(burial)>350 and len([p for p in profile if p[-1]=="receiving chamber"])<24
    receipt=dict(length_m=round(length,3),stations=len(profile),geometric_stations=geometric_stations,
        inner_radius_m=INNER,outer_radius_m=OUTER,
        minimum_buried_cover_m=min(p[-2] for p in burial),buried_stations=len(burial),
        owned_personnel_space_crossings=0,source_terminal=CONTROL[0],receiving_mouth=CONTROL[-1],
        isolation_leaf="existing closed upstream tailrace sluice",active_hydraulic_simulation=False)
    return built,profile,receipt


def build():
    roster=json.loads((PARENT/"roster.json").read_bytes())
    row=next(r for r in roster["active_sources"] if r["id"]=="WLD-09")
    prior=ROOT/row["construction"]
    meta=json.loads(prior.read_bytes())
    authority=json.loads((PARENT/"checkpoint.json").read_bytes())["project_bytes_sha256"]
    schedules=[prior.with_name(name) for name in ("runtime-input.tsv","door-schedule.tsv","electrical-schedule.tsv","blackwater.water")]
    for p in (prior,*schedules,*(ROOT/row[key] for key in ("glb","native","blend","layout"))):
        assert sha(p)==authority[rel(p)],p
    bpy.ops.wm.open_mainfile(filepath=str(ROOT/row["blend"]))
    bpy.context.preferences.filepaths.save_version=0
    original_saved=meta["resurfaced_render_mesh"]
    soil=grades(bpy.data.objects[VALLEY_NAME],bpy.data.objects[original_saved])
    for name in REMOVED:
        bpy.data.objects.remove(bpy.data.objects[name],do_unlink=True)
    built,profile,carrier=install(soil)
    valley,valley_faces=replace_surface(bpy.data.objects[VALLEY_NAME],
                                       VALLEY_NAME+" at intake bench",INTAKE_BOX)
    bank,bank_faces=replace_surface(bpy.data.objects[original_saved],
                                    original_saved+" at receiving bench",TAILRACE_BOX)
    valley_name,bank_name=valley.name,bank.name
    removed_render=REMOVED|{VALLEY_NAME,original_saved}
    OUT.mkdir(parents=True,exist_ok=True)
    fragment=ROOT/"build/dam-carrier-fragment.glb"
    export(fragment,[*built,valley,bank])
    outputs={ext:OUT/f"{STEM}.{ext}" for ext in ("glb","hcs2","blend")}
    append(ROOT/row["glb"],fragment,outputs["glb"],remove_mesh_names=removed_render,reuse_embedded_images=True)
    bench_native=ROOT/"build/dam-carrier-benches.hcs2"
    cut_faces,patched=replace_native(ROOT/row["native"],bench_native)
    assert sum(patched.values())==valley_faces+bank_faces,(cut_faces,valley_faces,bank_faces)
    added=native_write(bench_native,outputs["hcs2"],REMOVED,built)
    previous=split_native(ROOT/row["native"])
    retained=[p for p in split_native(bench_native) if p[0] not in REMOVED]
    before={name:raw for name,_,raw in previous}
    assert len(previous)-len(retained)==len(REMOVED)
    assert all(before[name]==raw for name,_,raw in retained if name not in patched)
    current=split_native(outputs["hcs2"])
    assert current[:len(retained)]==retained and len(current)-len(retained)==len(added)
    old,_=read_glb(ROOT/row["glb"])
    new,_=read_glb(outputs["glb"])
    for a,b in zip(old["nodes"],new["nodes"]):
        expected=dict(a)
        if a.get("name") in removed_render:
            expected.pop("mesh",None)
        assert b==expected,a.get("name")
    bpy.ops.file.pack_all()
    bpy.ops.wm.save_as_mainfile(filepath=str(outputs["blend"]),compress=True)
    meshes=import_scene(outputs["glb"])
    for name,triangles in added:
        assert match_geometry([t for t,_ in triangles],[t for t,_ in records(meshes[name])]),name
    native_parts=read_parts(outputs["hcs2"])
    for model,tris in ((valley_name,native_parts[VALLEY_NAME][1]),(bank_name,[
            face for name,(_,faces) in native_parts.items() if name.startswith("terrain-") for face in faces])):
        assert match_geometry([face for face,_ in records(meshes[model])],[face for face,_ in tris]),model
    for p in schedules:
        (OUT/p.name).write_bytes(p.read_bytes())
    write_tsv(OUT/"carrier-profile.tsv",[
        ("station","chainage","x","y","z","lower_side_grade","minimum_cover","region"),*profile])
    meta.update(status=STATUS,previous_construction=rel(prior),builder=rel(Path(__file__)),builder_sha256=sha(Path(__file__)),
                source_glb=row["glb"],source_glb_sha256=row["glb_sha256"],
                source_physics=row["native"],source_physics_sha256=row["native_sha256"],
                glb_sha256=sha(outputs["glb"]),blend_sha256=sha(outputs["blend"]),physics_sha256=sha(outputs["hcs2"]),
                removed_source_meshes=sorted(REMOVED),retained_native_records=len(retained)-len(patched),
                joined_valley_render_mesh=valley_name,resurfaced_render_mesh=bank_name,
                terrain_benches=dict(intake=INTAKE_BOX,receiving=TAILRACE_BOX,locally_cut_faces=cut_faces,
                                     amended_native_records=patched,seated_pipe_shoes=4),
                new_interface_parts=[name for name,_ in added],carrier=carrier)
    meta["intake"]=dict(meta["intake"],carrier_state="connected behind closed tailrace isolation gate")
    write(OUT/"construction.json",meta)
    row.update(construction=rel(OUT/"construction.json"),glb=rel(outputs["glb"]),native=rel(outputs["hcs2"]),
               blend=rel(outputs["blend"]),glb_sha256=meta["glb_sha256"],native_sha256=meta["physics_sha256"])
    next(r for r in roster["sites"] if r["id"]=="WLD-09")["decision"]=STATUS
    roster["baseline"]=rel(PARENT/"roster.json")
    write(STAGE/"roster.json",roster)
    print("PASS",carrier,"replaced",len(REMOVED),"temporary headworks parts; installed",len(added))


if __name__=="__main__":
    build()
