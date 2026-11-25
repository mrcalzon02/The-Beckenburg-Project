"""Fit a contained water sump outside Mine circulation."""
import json
from pathlib import Path
import sys

import bpy
from mathutils import Matrix, Vector

ROOT=Path(__file__).resolve().parents[1]
sys.path[:0]=[str(ROOT/"tools"),str(ROOT/"development/levels/blockouts")]
from append_static_glb import append
from build_circulation_construction import native_write
from build_college_electrical import Assembly,point,world
from build_lookout_native_arrival import table
from build_mine_lower_slabs import LOW_FLOOR, ROOM, top_at
from build_mine_yard import combined_plate,punched_plate
from build_prison_sally_gate import export
from build_roster_foundations import material
from review_roster_macroterrain import records,sha
from verify_college_containment import triangles
from verify_college_electrical import native_records
from verify_college_services import match_geometry

BASE=ROOT/"development/levels/rebuild/mine-drainage-pass"
PUMP=ROOT/"development/levels/primitive-props-v1/models/level_blockout_cinder_sump_pump.glb"
HOLE=(17.29,37.40,19.40,39.55)
INNER=(17.49,37.60,19.20,39.35)
WATER_Y=-10.49
BED_Y=-11.12


def basin(a):
    old=a.scene.objects[LOW_FLOOR+" region0"]
    old_name=old.name
    surface=old.data.materials[0]
    x0,z0,x1,z1=HOLE
    for x,z in ((x0,z0),(x0,z1),(x1,z0),(x1,z1),(16.25,38.7),(17.0,39.76)):
        assert abs(top_at(old,x,-10,z)+10)<.0005
    bpy.data.objects.remove(old,do_unlink=True)
    slab=combined_plate("Mine lower workings slab with sump well",punched_plate(ROOM,HOLE),-10,.28,surface)
    slab["role"]="full lower slab with one structurally formed water well"
    slab["zone"]="lower"
    lining=material("Mine sealed sump concrete",(.32,.34,.32))
    wall=combined_plate("Mine lower cast sump walls",punched_plate(HOLE,INNER),-9.865,1.271,lining)
    floor=combined_plate("Mine lower sealed sump invert",[HOLE],BED_Y,.22,lining)
    wall["role"]="water retaining rim and liner"
    floor["role"]="closed water tank floor"
    a.built.extend((slab,wall,floor))
    bpy.context.view_layer.update()
    return old_name,slab,lining


def fit_guard(a,slab):
    steel=material("Mine galvanized sump safety frame",(.33,.35,.33))
    x0,z0,x1,z1=(17.064,37.205,19.62,39.75)
    corners=((x0,z0),(x1,z0),(x1,z1),(x0,z1))
    posts=list(corners)
    posts.extend(((x0+x1)/2,z) for z in (z0,z1))
    posts.extend((x,(z0+z1)/2) for x in (x0,x1))
    for i,(x,z) in enumerate(posts,1):
        for dx,dz in ((-.038,-.038),(.038,-.038),(-.038,.038),(.038,.038)):
            at=top_at(slab,x+dx,-10,z+dz)
            assert at is not None and abs(at+10)<.0005,(x,z)
        a.box(f"sump post bearing plate {i}",(x-.056,z-.056,x+.056,z+.056),-10.005,-9.962,steel,
              "floor-anchored sump guard plate","lower")
        a.tube(f"sump anchored post {i}",(x,-9.968,z),(x,-8.91,z),.033,steel)
    for y,r,label in ((-8.91,.034,"grip"),(-9.43,.026,"midrail")):
        for i,((x,z),(end_x,end_z)) in enumerate(zip(corners,corners[1:]+corners[:1]),1):
            a.tube(f"sump {label} {i}",(x,y,z),(end_x,y,end_z),r,steel)
    return len(posts)*4


def fit_pump(a,slab,lining):
    x,y,z=15.60,-9.95,38.55
    support=(14.53,37.63,16.95,39.47)
    for u in (support[0]+.09,support[2]-.09):
        for v in (support[1]+.09,support[3]-.09):
            assert abs(top_at(slab,u,-10,v)+10)<.0005
    a.box("sump pump foundation",support,-10.006,-9.945,lining,"retained pump foundation","lower")
    existing=set(a.scene.objects)
    bpy.ops.import_scene.gltf(filepath=str(PUMP))
    model,=[o for o in set(a.scene.objects)-existing if o.type=="MESH"]
    transform=Matrix.Translation(point(x,y,z)) @ model.matrix_world
    installed=bpy.data.objects.new("Mine Cinder sump pump and isolated outlet",model.data)
    a.scene.collection.objects.link(installed)
    installed.matrix_world=transform
    installed["source_asset"]=PUMP.stem
    installed["role"]="indexed pump assembly isolated from the active power network"
    a.built.append(installed)
    for ob in set(a.scene.objects)-existing-{installed}:
        bpy.data.objects.remove(ob,do_unlink=True)
    bpy.context.view_layer.update()
    bounds=[world(installed.matrix_world@v.co) for v in installed.data.vertices]
    assert abs(min(p[1] for p in bounds)+9.945)<.003
    steel=material("Mine isolated pump service fittings",(.29,.31,.30))
    brass=material("Mine shut sump isolation flange",(.41,.32,.18))
    a.box("sump service pipe support post",(16.82,39.16,16.98,39.32),-9.999,-6.153,steel,
          "supported service pipe post between slab and soffit","lower")
    intake=[(18.01,-10.90,38.86),(18.01,-9.66,38.86),(17.29,-9.66,38.86),
            (16.30,-9.66,38.86),(16.25,-9.79,38.96)]
    discharge=[(16.87,-9.07,38.96),(16.90,-8.98,38.96),(16.90,-6.51,38.96),
               (19.81,-6.51,38.96),(19.91,-6.36,38.96)]
    for label,route in (("sump intake",intake),("isolated sump outlet",discharge)):
        for i,(p,q) in enumerate(zip(route,route[1:]),1):
            a.tube(f"{label} segment {i}",p,q,.064,steel)
    for y0 in (-8.55,-7.72,-6.75):
        a.tube("sump stand-mounted pipe saddle",(16.90,y0,38.96),(16.90,y0,39.20),.033,steel)
    for x0 in (17.4,18.5,19.4):
        a.tube("sump soffit pipe saddle",(x0,-6.51,38.96),(x0,-6.153,38.96),.031,steel)
    a.tube("sump isolated outlet flange",(19.797,-6.51,38.96),(19.88,-6.51,38.96),.105,brass)
    return dict(source=PUMP.relative_to(ROOT).as_posix(),source_sha256=sha(PUMP),pivot=[x,y,z],
                status="isolated machinery; standing water retained in the physical basin",intake=intake,discharge=discharge)


def water(out):
    x0,z0,x1,z1=INNER
    settings={"bed.depth":WATER_Y-BED_Y,"wave.height":.018,"wave.grid":.35,"wave.speed":.27,
              "shore.height":.004,"foam.opacity":.05,"contact.enabled":"false","spray.enabled":"false"}
    props={"version":1,"level":"wld-05","count":1,"water.0.id":"mine-lower-sump","water.0.shape":"SQUARE",
           "water.0.x":(x0+x1)/2,"water.0.y":WATER_Y,"water.0.z":(z0+z1)/2,
           "water.0.width":x1-x0-.032,"water.0.depth":z1-z0-.032,"water.0.yaw":0}
    props.update({"water.0.setting."+key:value for key,value in settings.items()})
    (out/"mine-sump.water").write_text("".join(f"{k}={v}\n" for k,v in props.items()),encoding="utf-8",newline="\n")


def build(out):
    source=json.loads((BASE/"construction.json").read_bytes())
    for kind,key in (("blend","blend_sha256"),("glb","glb_sha256"),("native","physics_sha256")):
        assert sha(ROOT/source[kind])==source[key]
    bpy.ops.wm.open_mainfile(filepath=str(ROOT/source["blend"]))
    bpy.context.preferences.filepaths.save_version=0
    assembly=Assembly()
    old,slab,lining=basin(assembly)
    guard_contacts=fit_guard(assembly,slab)
    pump=fit_pump(assembly,slab,lining)
    for obj in assembly.built:
        if obj.name.startswith("College "):
            obj.name="Mine "+obj.name.removeprefix("College ")
            if obj.get("role")=="closed electrical service conduit":
                obj["role"]="solid sump guard or pipe assembly"
    out.mkdir(parents=True,exist_ok=True)
    targets={ext:out/f"mine-sump.{ext}" for ext in ("blend","glb","hcs2")}
    addition=out/"_sump.glb"
    export(addition,assembly.built)
    append(ROOT/source["glb"],addition,targets["glb"],remove_mesh_names=[old],reuse_embedded_images=True)
    addition.unlink()
    native_write(ROOT/source["native"],targets["hcs2"],{old},assembly.built,
                 open_components={o.name for o in assembly.built if o.get("source_asset")})
    before=native_records(ROOT/source["native"])
    kept=[part for part in before if part[0]!=old]
    after=native_records(targets["hcs2"])
    assert len(before)-len(kept)==1 and after[:len(kept)]==kept
    bpy.ops.file.pack_all()
    bpy.ops.wm.save_as_mainfile(filepath=str(targets["blend"]),compress=True)
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.import_scene.gltf(filepath=str(targets["glb"]))
    for part in after[len(kept):]:
        assert match_geometry(triangles(part),[t for t,_ in records(bpy.context.scene.objects[part[0]])]),part[0]
    hashes={ext:sha(f) for ext,f in targets.items()}
    rows=[("SOURCE",targets["glb"].relative_to(ROOT).as_posix(),hashes["glb"],
           targets["hcs2"].relative_to(ROOT).as_posix(),hashes["hcs2"])]
    rows.extend(line.split("\t") for line in (BASE/"arrival.tsv").read_text().splitlines()[1:])
    rows.append(("SPACE","sump-well",9,*HOLE,BED_Y,1.12,"false","true"))
    table(out/"arrival.tsv",rows)
    for name in ("door-survey.tsv","mission.tsv","rail-probes.tsv","switches.tsv","drainage-probes.tsv"):
        (out/name).write_bytes((BASE/name).read_bytes())
    probes=[]
    for line in (BASE/"slab-probes.tsv").read_text().splitlines():
        name,x,y,z,expected=line.split("\t")
        x,z=float(x),float(z)
        if float(y)==-10 and HOLE[0]<x<HOLE[2] and HOLE[1]<z<HOLE[3]:
            expected="false"
        probes.append((name,x,y,z,expected))
    table(out/"slab-probes.tsv",probes)
    water(out)
    receipt=dict(exports=hashes,retired=[old],retained_components=len(kept),new_components=len(after)-len(kept),
                 outer_basin=HOLE,wet_basin=INNER,bed=BED_Y,level=WATER_Y,freeboard=round(-9.865-WATER_Y,3),
                 guard_contacts=guard_contacts,pump=pump,water_sha256=sha(out/"mine-sump.water"))
    (out/"sump.json").write_text(json.dumps(receipt,indent=2)+"\n",encoding="utf-8",newline="\n")
    print("PASS formed lower sump",guard_contacts,"anchored guard corners, real basin and exact native parity")


def capture_structure(out):
    bpy.ops.wm.open_mainfile(filepath=str(out/"mine-sump.blend"))
    scene=bpy.context.scene
    scene.render.engine="BLENDER_WORKBENCH"
    shading=scene.display.shading
    shading.color_type="MATERIAL"
    shading.show_cavity=True
    shading.show_shadows=True
    shading.cavity_type="BOTH"
    scene.view_settings.view_transform="Standard"
    scene.render.resolution_x=1360
    scene.render.resolution_y=960
    scene.render.resolution_percentage=100
    scene.render.image_settings.file_format="PNG"
    bpy.ops.object.camera_add()
    camera=bpy.context.object
    camera.data.type="ORTHO"
    scene.camera=camera
    directory=out/"captures"/"structure"
    directory.mkdir(parents=True,exist_ok=True)
    prefixes=("Mine sump","Mine lower workings slab with sump","Mine lower cast sump","Mine lower sealed sump",
              "Mine Cinder sump","Mine isolated sump","Wall lower E","Bayed shell Wall lower S")
    for obj in scene.objects:
        if obj.type=="MESH":
            obj.hide_render=not obj.name.startswith(prefixes)
    eye=point(14.0,-3.5,35.25)
    aim=point(17.2,-9.5,38.4)
    camera.location=eye
    camera.rotation_euler=(aim-eye).to_track_quat("-Z","Y").to_euler()
    camera.data.ortho_scale=7.2
    scene.render.filepath=str(directory/"mine-lower-sump-cutaway.png")
    bpy.ops.render.render(write_still=True)


if __name__=="__main__":
    args=sys.argv[sys.argv.index("--")+1:]
    output=Path(args[0]).resolve()
    if "--captures-only" in args:
        capture_structure(output)
    else:
        build(output)
