"""Rebuild the two Mine slabs around the measured lower working stair."""
import json
import math
from pathlib import Path
import sys

import bpy
from mathutils import Vector

ROOT=Path(__file__).resolve().parents[1]
sys.path[:0]=[str(ROOT/"tools"),str(ROOT/"development/levels/blockouts")]
from append_static_glb import append
from build_circulation_construction import native_write
from build_college_electrical import Assembly, point
from build_lookout_native_arrival import table
from build_mine_yard import combined_plate, punched_plate
from build_prison_sally_gate import export
from build_roster_foundations import material
from review_roster_macroterrain import records,sha
from verify_college_containment import triangles
from verify_college_electrical import native_records
from verify_college_services import match_geometry

BASE=ROOT/"development/levels/rebuild/mine-distribution-pass"
ROOM=(2,20,20,42)
OPENING=(4.25,27.50,5.75,33.65)
LOW_FLOOR="Mine lower workings continuous floor"
UPPER_FLOOR="Mine ore-west slab with lower stair opening"
REPLACED_LOWER={
    "Bayed shell Floor lower",
    "Bayed stair WLD-05 L15 step2",
    "Bayed stair WLD-05 L15 step3",
    "Bayed stair WLD-05 L15 step4",
    "Indoor route bearing WLD-05 lower L15 3 region0",
}
REPLACED_UPPER={
    "Bayed shell Floor ore-west.001",
    "Bayed shell Floor ore-west.002",
    "Restored bay WLD-05 ore-west floor region0",
    "Floor ore-west",
    "Bayed stair WLD-05 L15 step0",
    "Bayed stair WLD-05 L15 step1",
    "Authored stair end WLD-05 L15 0 high region0",
    *(f"Floor ore-west.{i:03d}" for i in range(3,9)),
}
REPLACED=REPLACED_UPPER|REPLACED_LOWER


def top_at(obj,x,y,z):
    inverse=obj.matrix_world.inverted()
    direction=inverse.to_3x3()@Vector((0,0,-1))
    ok,hit,normal,_=obj.ray_cast(inverse@point(x,y+.2,z),direction,distance=.27)
    if ok and (obj.matrix_world.to_3x3()@normal).z>.98:
        return (obj.matrix_world@hit).z
    return None


def replacement(builder):
    scene=builder.scene
    assert REPLACED<={o.name for o in scene.objects}
    low_material=scene.objects["Bayed shell Floor lower"].data.materials[0]
    upper_material=scene.objects["Floor ore-west"].data.materials[0]
    low_witness=(5,26.70)
    top_witness=(5.25,34.10)
    overlaps={}
    for name,y,probe in (("lower", -10,low_witness),("ore-west",-6,top_witness)):
        bearing=[]
        for obj in scene.objects:
            if obj.type!="MESH" or obj.name not in REPLACED:
                continue
            at=top_at(obj,probe[0],y,probe[1])
            if at is not None and abs(at-y)<.0005:
                bearing.append(obj.name)
        assert len(bearing)==3,(name,bearing)
        overlaps[name]={"point":[probe[0],y,probe[1]],"former_surfaces":sorted(bearing)}

    # Use the existing perimeter, wall finishes, flight and datum. The
    # deeper slab is a ground-bearing casting and the upper slab keeps the
    # exact previous soffit for fixtures in the lower chamber.
    perimeters=[]
    for da,db in ((2.24,19.76),(19.76,2.24)):
        dx=math.copysign(1,da-db)
        for z in (22,25,30,35,40):
            hit,_,wall=builder.measured(point(da,-9.95,z),point(dx,0,0),.28,"lower side wall bearing")
            assert abs(hit.x-(2.08 if dx<0 else 19.92))<.002 and "Wall lower" in wall.name
            perimeters.append(wall.name)
    for z,dz in ((20.24,-1),(41.76,1)):
        for x in (3.4,6.3,10.8,15.7,18.4):
            hit,_,wall=builder.measured(point(x,-9.95,z),point(0,0,dz),.28,"lower end wall bearing")
            assert abs(-hit.y-(20.08 if dz<0 else 41.92))<.002 and "Wall lower" in wall.name
            perimeters.append(wall.name)

    for name in sorted(REPLACED):
        bpy.data.objects.remove(scene.objects[name],do_unlink=True)
    lower=combined_plate(LOW_FLOOR,[ROOM],-10,.28,low_material)
    lower["role"]="ground-bearing slab across the complete lower chamber and stair receiving area"
    lower["zone"]="lower"
    upper=combined_plate(UPPER_FLOOR,punched_plate(ROOM,OPENING),-6,.16,upper_material)
    upper["role"]="continuous ore-west suspended slab with one formed stair opening"
    upper["zone"]="ore-west"
    bpy.context.view_layer.update()
    proof=[]
    for obj,level in ((lower,-10),(upper,-6)):
        for nx in range(36):
            for nz in range(44):
                x=2.25+nx*.5
                z=20.25+nz*.5
                through_stair=obj==upper and OPENING[0]<x<OPENING[2] and OPENING[1]<z<OPENING[3]
                if x in (OPENING[0],OPENING[2]):
                    x+=.011
                    through_stair=obj==upper and OPENING[0]<x<OPENING[2] and OPENING[1]<z<OPENING[3]
                at=top_at(obj,x,level,z)
                assert (at is None if through_stair else at is not None and abs(at-level)<.0005),(obj.name,x,z,at)
                proof.append((obj.name,x,level,z,not through_stair))
    for x in (4.32,5.68):
        for z,y,obj in ((26.75,-10,lower),(34.1,-6,upper)):
            for dx,dz in ((-.036,-.041),(-.036,.041),(.036,-.041),(.036,.041)):
                at=top_at(obj,x+dx,y,z+dz)
                assert at is not None and abs(at-y)<.0005,("existing guard foot",x+dx,y,z+dz)
    for x,z in ((11,31),(10,41.855)):
        local=upper.matrix_world.inverted()
        hit,at,normal,_=upper.ray_cast(local@point(x,-6.55,z),local.to_3x3()@Vector((0,0,1)),distance=.5)
        assert hit and abs((upper.matrix_world@at).z+6.16)<.002 and normal.z<-.98
    builder.built.extend((lower,upper))
    return lower,upper,proof,overlaps,perimeters


def frame_ceiling(builder,lower,upper):
    steel=material("Mine ore-west bearing steel",(.29,.31,.29))
    zone="lower"
    for x in (8.20,14.20):
        for z,dz in ((20.25,-1),(41.75,1)):
            hit,_,wall=builder.measured(point(x,-6.35,z),point(0,0,dz),.27,"girder end bearing")
            assert abs(-hit.y-(20.08 if dz<0 else 41.92))<.002 and "Wall lower" in wall.name
        for z in (22.95,28.95,35.10,40.10):
            for dx,dz in ((-.19,-.19),(-.19,.19),(.19,-.19),(.19,.19)):
                assert abs(top_at(lower,x+dx,-10,z+dz)+10)<.0005
            builder.box("lower ore-west column bearing foot",(x-.285,z-.285,x+.285,z+.285),
                        -10.006,-9.947,steel,"working slab bearing plate",zone)
            builder.box("lower ore-west bearing column",(x-.187,z-.187,x+.187,z+.187),
                        -9.95,-6.465,steel,"load bearing steel shaft column",zone)
        for z in (22.95,28.95,35.10,40.10):
            invert=upper.matrix_world.inverted()
            ok,p,n,_=upper.ray_cast(invert@point(x,-6.54,z),invert.to_3x3()@Vector((0,0,1)),distance=.50)
            assert ok and abs((upper.matrix_world@p).z+6.16)<.0005 and n.z<-.98
        builder.box("lower ore-west ceiling girder",(x-.215,20.025,x+.215,41.975),-6.49,-6.152,steel,
                    "seated continuous girder between lower chamber end walls",zone)
    left,bottom,right,top=OPENING
    strips=((left-.18,bottom-.18,left,top+.18),(right,bottom-.18,right+.18,top+.18),
            (left,bottom-.18,right,bottom),(left,top,right,top+.18))
    for i,strip in enumerate(strips,1):
        builder.box(f"lower stair shaft edge member {i}",strip,-6.41,-6.151,steel,
                    "reinforced structural opening edge outside tread clearance",zone)
    return 32


def guard_opening(builder,slab):
    metal=material("Mine aged galvanized handhold steel",(.32,.34,.33))
    left,right,north,south=4.065,5.935,27.315,33.65
    posts=[]
    for x in (left,right):
        posts.extend((x,z) for z in (north,28.85,30.45,32.05,south))
    posts.append((5,north))
    for i,(x,z) in enumerate(posts,1):
        for dx,dz in ((-.035,-.037),(-.035,.037),(.035,-.037),(.035,.037)):
            at=top_at(slab,x+dx,-6,z+dz)
            assert at is not None and abs(at+6)<.0005,("new stairwell guard foot",x,z)
        builder.box(f"ore-west shaft guard foot {i}",(x-.054,z-.054,x+.054,z+.054),-6.005,-5.966,
                    metal,"bolted foot backed by the formed ore-west slab","ore-west")
        builder.tube(f"ore-west shaft guard upright {i}",(x,-5.969,z),(x,-4.945,z),.035,metal)
    for y,r,label in ((-4.945,.032,"grip"),(-5.44,.026,"middle rail")):
        for i,(a,b) in enumerate((((left,y,south),(left,y,north)),((left,y,north),(right,y,north)),
                                   ((right,y,north),(right,y,south))),1):
            builder.tube(f"ore-west shaft guard {label} {i}",a,b,r,metal)
    return len(posts)*4


def build(out):
    source=json.loads((BASE/"construction.json").read_bytes())
    for kind,key in (("blend","blend_sha256"),("glb","glb_sha256"),("native","physics_sha256")):
        assert sha(ROOT/source[kind])==source[key]
    bpy.ops.wm.open_mainfile(filepath=str(ROOT/source["blend"]))
    bpy.context.preferences.filepaths.save_version=0
    builder=Assembly()
    lower,upper,proof,overlaps,bearing=replacement(builder)
    column_contacts=frame_ceiling(builder,lower,upper)
    guard_contacts=guard_opening(builder,upper)
    for obj in builder.built:
        if obj.name.startswith("College "):
            obj.name="Mine "+obj.name.removeprefix("College ")
            if obj.get("role")=="closed electrical service conduit":
                obj["role"]="closed ore-west stairway guard member"
    out.mkdir(parents=True,exist_ok=True)
    targets={ext:out/f"mine-lower-slabs.{ext}" for ext in ("blend","glb","hcs2")}
    addition=out/"_slabs.glb"
    export(addition,builder.built)
    append(ROOT/source["glb"],addition,targets["glb"],remove_mesh_names=sorted(REPLACED),reuse_embedded_images=True)
    addition.unlink()
    native_write(ROOT/source["native"],targets["hcs2"],REPLACED,builder.built)
    before=native_records(ROOT/source["native"])
    after=native_records(targets["hcs2"])
    kept=[r for r in before if r[0].split(":",1)[-1] not in REPLACED]
    assert len(before)-len(kept)==len(REPLACED) and after[:len(kept)]==kept
    bpy.ops.file.pack_all()
    bpy.ops.wm.save_as_mainfile(filepath=str(targets["blend"]),compress=True)
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.import_scene.gltf(filepath=str(targets["glb"]))
    assert not REPLACED&{o.name for o in bpy.context.scene.objects if o.type=="MESH"}
    for part in after[len(kept):]:
        assert match_geometry(triangles(part),[t for t,_ in records(bpy.context.scene.objects[part[0]])]),part[0]
    hashes={ext:sha(path) for ext,path in targets.items()}
    rows=[("SOURCE",targets["glb"].relative_to(ROOT).as_posix(),hashes["glb"],
           targets["hcs2"].relative_to(ROOT).as_posix(),hashes["hcs2"])]
    for line in (BASE/"arrival.tsv").read_text().splitlines()[1:]:
        row=line.split("\t")
        if row[0]!="TREAD" or row[1] not in REPLACED:
            rows.append(row)
    table(out/"arrival.tsv",rows)
    for name in ("door-survey.tsv","mission.tsv","rail-probes.tsv","switches.tsv"):
        (out/name).write_bytes((BASE/name).read_bytes())
    table(out/"slab-probes.tsv",[(name,x,y,z,str(support).lower()) for name,x,y,z,support in proof])
    verdict=dict(exports=hashes,removed=sorted(REPLACED),retained_components=len(kept),added_components=len(after)-len(kept),
                 former_overlap=overlaps,room=ROOM,formed_stair_opening=OPENING,slab_probes=len(proof),
                 fixed_wall_bearings=len(bearing),guard_mount_corner_checks=guard_contacts,
                 retained_existing_guard_contact_checks=16,column_bearing_corner_checks=column_contacts,
                 retained_lower_fixture_and_sleeve_soffit=-6.16)
    (out/"slabs.json").write_text(json.dumps(verdict,indent=2)+"\n",encoding="utf-8",newline="\n")
    print("PASS rebuilt Mine receiving slabs",len(REPLACED),"former components removed;",len(proof),
          "slab probes,",guard_contacts,"new guard corner checks")


def capture_structure(out):
    bpy.ops.wm.open_mainfile(filepath=str(out/"mine-lower-slabs.blend"))
    scene=bpy.context.scene
    scene.render.engine="BLENDER_WORKBENCH"
    shading=scene.display.shading
    shading.color_type="MATERIAL"
    shading.show_cavity=True
    shading.show_shadows=True
    shading.cavity_type="BOTH"
    scene.view_settings.view_transform="Standard"
    scene.render.resolution_x=1340
    scene.render.resolution_y=900
    scene.render.resolution_percentage=100
    scene.render.image_settings.file_format="PNG"
    bpy.ops.object.camera_add()
    camera=bpy.context.object
    camera.data.type="ORTHO"
    scene.camera=camera
    pictures=out/"captures"/"structure"
    pictures.mkdir(parents=True,exist_ok=True)
    stair=("Bayed stair WLD-05 L15 step","Mine lower east","Mine lower west")
    rows=(
        ("ore-west-formed-stair-opening",point(12,.5,22.9),point(5.3,-6.7,30.4),12.4,
         stair+(UPPER_FLOOR,"Mine ore-west shaft guard")),
        ("lower-chamber-bearing-frame",point(7.2,-3,17.5),point(10,-8.2,31.3),24.0,
         stair+(LOW_FLOOR,"Mine lower ore-west","Mine lower stair shaft edge","Wall lower W","Bayed shell Wall lower S")),
    )
    for name,eye,aim,scale,prefixes in rows:
        for obj in scene.objects:
            if obj.type=="MESH":
                obj.hide_render=not obj.name.startswith(prefixes)
        camera.location=eye
        camera.rotation_euler=(aim-eye).to_track_quat("-Z","Y").to_euler()
        camera.data.ortho_scale=scale
        scene.render.filepath=str(pictures/(name+".png"))
        bpy.ops.render.render(write_still=True)


if __name__=="__main__":
    args=sys.argv[sys.argv.index("--")+1:]
    output=Path(args[0]).resolve()
    if "--captures-only" in args:
        capture_structure(output)
    else:
        build(output)
