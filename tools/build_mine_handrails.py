"""Fit Mine handrails to the measured shaft walls and lower workings treads."""
import json
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
from build_prison_sally_gate import export
from build_roster_foundations import material
from review_roster_macroterrain import records,sha
from verify_college_containment import triangles
from verify_college_electrical import native_records
from verify_college_services import match_geometry

BASE=ROOT/"development/levels/rebuild/mine-cover-pass"
RAIL_HEIGHT=.99
RADIUS=.026
SHAFTS=(
    ("haul","z",28.35,((32.4,39.8,-6,-3),(41.4,47.9,-3,0)),48.42),
    ("vent","x",52.0,((11.85,18.15,-6,-3),(19.85,25.35,-3,0)),25.88),
)
LOWER_PITCH=(33.65-27.5)/23


def xyz(axis,tangent,elevation,side):
    return (side,elevation,tangent) if axis=="z" else (tangent,elevation,side)


def profile_y(t,start,end,low,high):
    return low+max(0,min(1,(t-start)/(end-start)))*(high-low)


def shaft_rails(builder,metal,fixings,probes):
    for name,axis,center,flights,top in SHAFTS:
        first,second=flights
        path=[(first[0]+.07,profile_y(first[0]+.07,*first)),(first[1],-3),
              (second[0],-3),(second[1],0),(top,0)]
        for sign in (-1,1):
            wall=center+sign
            rail=wall-sign*.128
            direction=point(sign if axis=="z" else 0,0,sign if axis=="x" else 0)
            label=f"Mine {name} {'left' if sign<0 else 'right'}"
            for start,end,low,high in flights:
                for t in (start+.50,start+(end-start)*.32,start+(end-start)*.63,end-.32):
                    y=profile_y(t,start,end,low,high)+RAIL_HEIGHT
                    backed,_,receiver=builder.measured(point(*xyz(axis,t,y,center)),direction,1.22,label+" bracket")
                    face=backed.x if axis=="z" else -backed.y
                    assert abs(face-wall)<.005 and f"Mine {name}" in receiver.name,receiver.name
                    edge=xyz(axis,t,y,rail)
                    mount=xyz(axis,t,y,wall+sign*.009)
                    builder.tube(label+" anchor stub",edge,mount,.039,metal)
                    fixings.append((label,receiver.name,*mount))
                t=(start+end)/2
                y=profile_y(t,start,end,low,high)+RAIL_HEIGHT
                probes.append((label,*xyz(axis,t,y,rail),axis))
            for i,((a,ya),(b,yb)) in enumerate(zip(path,path[1:]),1):
                builder.tube(label+f" continuous grip {i}",xyz(axis,a,ya+RAIL_HEIGHT,rail),
                             xyz(axis,b,yb+RAIL_HEIGHT,rail),RADIUS,metal)
            for t,y in (path[0],path[-1]):
                builder.tube(label+" closed return",xyz(axis,t,y+RAIL_HEIGHT,rail),
                             xyz(axis,t,y+RAIL_HEIGHT,wall+sign*.009),RADIUS,metal)


def lower_rails(builder,metal,fixings,probes):
    stations=[(26.75,-10)]+[(27.5+LOWER_PITCH*(i+.5),-10+4*(i+1)/23) for i in (0,4,8,12,16,20,22)]+[(34.10,-6)]
    for x in (4.32,5.68):
        label=f"Mine lower {'west' if x<5 else 'east'}"
        for i,(z,y) in enumerate(stations,1):
            for dx,dz in ((-.036,-.041),(-.036,.041),(.036,-.041),(.036,.041)):
                at,n,support=builder.measured(point(x+dx,y+.19,z+dz),Vector((0,0,-1)),.36,label+" foot")
                assert abs(at.z-y)<.004 and n.z>.995,(x,y,z,support.name)
                fixings.append((label,support.name,x+dx,y,z+dz))
            builder.box(label+f" bolted foot {i}",(x-.046,z-.050,x+.046,z+.050),y-.004,y+.026,metal,
                        "tread bearing plate","lower")
            top=profile_y(z,27.5,33.65,-10,-6)+1.07
            builder.tube(label+f" tread-mounted post {i}",(x,y+.024,z),(x,top,z),.029,metal)
        route=((26.75,-10),(27.50,-10),(33.65,-6),(34.10,-6))
        for h,r,member in ((1.07,RADIUS,"handrail"),(.57,.022,"intermediate guard")):
            for i,((a,ya),(b,yb)) in enumerate(zip(route,route[1:]),1):
                builder.tube(label+f" {member} {i}",(x,ya+h,a),(x,yb+h,b),r,metal)
        t=30.55
        probes.append((label,x,profile_y(t,27.5,33.65,-10,-6)+1.07,t,"z"))


def build(out):
    prior=json.loads((BASE/"construction.json").read_bytes())
    paths={kind:ROOT/prior[kind] for kind in ("blend","glb","native")}
    assert all(sha(paths[k])==prior[h] for k,h in (("blend","blend_sha256"),("glb","glb_sha256"),("native","physics_sha256")))
    bpy.ops.wm.open_mainfile(filepath=str(paths["blend"]))
    bpy.context.preferences.filepaths.save_version=0
    builder=Assembly()
    metal=material("Mine aged galvanized handhold steel",(.32,.34,.33))
    fixings,probes=[],[]
    shaft_rails(builder,metal,fixings,probes)
    lower_rails(builder,metal,fixings,probes)
    for obj in builder.built:
        obj.name=obj.name.removeprefix("College ")
        if obj.get("role")=="closed electrical service conduit":
            obj["role"]="physical steel handrail or supported guard member"

    out.mkdir(parents=True,exist_ok=True)
    exports={ext:out/f"mine-handrails.{ext}" for ext in ("blend","glb","hcs2")}
    addition=out/"_handrails.glb"
    export(addition,builder.built)
    append(paths["glb"],addition,exports["glb"],reuse_embedded_images=True)
    addition.unlink()
    native_write(paths["native"],exports["hcs2"],set(),builder.built)
    before=native_records(paths["native"])
    after=native_records(exports["hcs2"])
    assert after[:len(before)]==before
    bpy.ops.file.pack_all()
    bpy.ops.wm.save_as_mainfile(filepath=str(exports["blend"]),compress=True)
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.import_scene.gltf(filepath=str(exports["glb"]))
    for p in after[len(before):]:
        assert match_geometry(triangles(p),[t for t,_ in records(bpy.context.scene.objects[p[0]])]),p[0]
    hashes={ext:sha(path) for ext,path in exports.items()}
    rows=[("SOURCE",exports["glb"].relative_to(ROOT).as_posix(),hashes["glb"],
           exports["hcs2"].relative_to(ROOT).as_posix(),hashes["hcs2"])]
    rows.extend(line.split("\t") for line in (BASE/"arrival.tsv").read_text().splitlines()[1:])
    table(out/"arrival.tsv",rows)
    for name in ("mission.tsv","door-survey.tsv"):
        (out/name).write_bytes((BASE/name).read_bytes())
    table(out/"rail-probes.tsv",probes)
    table(out/"mounts.tsv",fixings)
    receipt=dict(exports=hashes,retained_objects=len(before),added_objects=len(after)-len(before),
        wall_brackets=32,lower_tread_bearing_samples=72,
        clear_shaft_width=round(2*(1-.128-RADIUS),3),clear_lower_rail_width=round(5.68-4.32-2*RADIUS,3),
        clear_shaft_width_at_brackets=1.666,clear_lower_width_at_foot_plates=1.268)
    (out/"handrails.json").write_text(json.dumps(receipt,indent=2)+"\n",encoding="utf-8",newline="\n")
    print("PASS",len(fixings),"real Mine rail mount checks and",len(before),"unchanged native components")


def render_rail_cutaways(out):
    bpy.ops.wm.open_mainfile(filepath=str(out/"mine-handrails.blend"))
    scene=bpy.context.scene
    scene.render.engine="BLENDER_WORKBENCH"
    shading=scene.display.shading
    shading.color_type="MATERIAL"
    shading.show_cavity=True
    shading.show_shadows=True
    shading.cavity_type="BOTH"
    scene.view_settings.view_transform="Standard"
    scene.render.resolution_x=1200
    scene.render.resolution_y=880
    scene.render.resolution_percentage=100
    scene.render.image_settings.file_format="PNG"
    bpy.ops.object.camera_add()
    camera=bpy.context.object
    camera.data.type="ORTHO"
    scene.camera=camera
    dst=out/"captures"/"structure"
    dst.mkdir(parents=True,exist_ok=True)
    views=(
        ("haul-bracket-cutaway",point(35,4.5,38),point(28.4,-1.9,41),14,
            ("Mine haul left","Mine haul right","Mine haul re-spaced","Mine haul stair flight 1",
             "Mine haul stair supported middle landing","Mine haul full upper",
             "Mine haul west retaining wall","Mine haul west return")),
        ("lower-tread-guard-cutaway",point(10.5,-3.6,26.4),point(5,-7.8,30.3),10.2,
            ("Mine lower west","Mine lower east","Entrance rebuild Mine lower",
             "Bayed stair WLD-05 L15 step","Authored stair end WLD-05 L15")),
    )
    for name,eye,target,scale,parts in views:
        for obj in scene.objects:
            if obj.type=="MESH":
                obj.hide_render=not obj.name.startswith(parts)
        camera.location=eye
        camera.rotation_euler=(target-eye).to_track_quat("-Z","Y").to_euler()
        camera.data.ortho_scale=scale
        scene.render.filepath=str(dst/(name+".png"))
        bpy.ops.render.render(write_still=True)


if __name__=="__main__":
    args=sys.argv[sys.argv.index("--")+1:]
    out=Path(args[0]).resolve()
    if "--captures-only" in args:
        render_rail_cutaways(out)
    else:
        build(out)
