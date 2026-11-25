"""Fit full headhouse receiving landings inside the existing Mine stair shafts."""
import json
from pathlib import Path
import sys

import bpy
from mathutils import Vector

ROOT = Path(__file__).resolve().parents[1]
sys.path[:0] = [str(ROOT/"tools"), str(ROOT/"development/levels/blockouts")]
from append_static_glb import append
from build_circulation_construction import native_write
from build_mine_yard import MAIN_RUNS, MAIN_X, TREADS, VENT_RUNS, VENT_Z, WIDTH, combined_plate
from build_prison_sally_gate import export
from build_school_door_poses import build as fit_editor_doors
from review_roster_macroterrain import records, sha
from verify_college_containment import triangles
from verify_college_electrical import native_records
from verify_college_services import match_geometry
from build_lookout_native_arrival import table

PRIOR = ROOT/"development/levels/rebuild/mine-yard-pass"
SHAFTS = (
    ("haul", "z", MAIN_X, MAIN_RUNS[1][0], MAIN_RUNS[1][1], 47.90, 49.0),
    ("vent", "x", VENT_Z, VENT_RUNS[1][0], VENT_RUNS[1][1], 25.35, 26.50),
)
OWNERS = {key:i for i,key in enumerate(
    ("shack","assay","stores","adit","haul","ore-west","ore-east","cross","lift","lower",
     "vent","wash","adit-court","entry"))}
OWNER_ALIASES={"haul-stair":"haul","vent-stair":"vent","stage":"entry"}


ANCHORS={
    "shack": (11,0,54), "assay": (47,0,54), "stores": (8,0,38), "adit": (30,0,52),
    "haul": (28,-6,31), "ore-west": (11,-6,31), "ore-east": (45,-6,31), "cross": (28,-6,14),
    "lift": (49,-6,14), "lower": (11,-10,31), "vent": (11,-6,52), "wash": (47,0,38),
    "adit-court": (30,0,58), "entry": (30,0,69),
}
OPEN_INTERFACES=(
    ("entry","adit-court",30,0,62,"z","trail"),
    ("assay","wash",47,0,46,"z","open"),
    ("haul","ore-west",20,-6,31,"x","open"),
    ("ore-west","cross",16,-6,20,"z","open"),
    ("cross","ore-east",40,-6,20,"z","open"),
    ("ore-east","haul",36,-6,31,"x","open"),
    ("cross","lift",44,-6,14,"x","open"),
    ("ore-west","lower",5,-6,33.65,"z","stair"),
    ("ore-west","vent",11,-6,42,"z","open"),
)


def owner(key):
    return OWNERS[OWNER_ALIASES.get(key,key)]


def write(path,data):
    path.write_bytes((json.dumps(data,ensure_ascii=False,indent=2)+"\n").encode())


def build(out,editor_vertices):
    source=json.loads((PRIOR/"checkpoint.json").read_bytes())["source"]
    original=json.loads((ROOT/source["construction"]).read_bytes())
    layout=json.loads((ROOT/source["layout"]).read_bytes())
    assert sha(ROOT/source["glb"])==original["glb_sha256"]
    assert sha(ROOT/source["native"])==original["physics_sha256"]
    assert sha(ROOT/source["blend"])==original["blend_sha256"]
    assert sha(ROOT/source["layout"])==original["layout_sha256"]
    bpy.ops.wm.open_mainfile(filepath=str(ROOT/source["blend"]))
    bpy.context.preferences.filepaths.save_version=0
    removed=set()
    built=[]
    schedule=[]
    for name,axis,centre,start,old_top,landing_start,door in SHAFTS:
        old_steps=[bpy.data.objects[f"Mine {name} stair flight 2 riser {i:02} region0"] for i in range(1,TREADS+1)]
        old_landing=bpy.data.objects[f"Mine {name} stair high landing at headhouse region0"]
        material=old_landing.data.materials[0]
        removed.update(o.name for o in [old_landing,*old_steps])
        def interval(a,b,low,high,tag,role):
            half=WIDTH/2
            rect=(centre-half,a,centre+half,b) if axis=="z" else (a,centre-half,b,centre+half)
            obj=combined_plate(f"Mine {name} {tag}",[rect],high,high-low,material)
            obj["role"]=role
            obj["zone"]=f"{name}-stair"
            built.append(obj)
        step_run=(landing_start-start)/TREADS
        for i in range(TREADS):
            interval(start+i*step_run,start+(i+1)*step_run,-6.22,-3+3*(i+1)/TREADS,
                     f"re-spaced upper tread {i+1:02}","founded upper stair tread")
        interval(landing_start,door,-.34,0,"full upper receiving landing",
                 "whole threshold landing continuous with headhouse floor")
        schedule.append(dict(shaft=name,axis=axis,center=centre,old_landing_depth=round(door-old_top,3),
            landing_depth=round(door-landing_start,3),clear_width=WIDTH,run=round(landing_start-start,3),
            rise=3,tread_depth=round(step_run,6),riser_height=round(3/TREADS,6)))
    for name in sorted(removed):
        bpy.data.objects.remove(bpy.data.objects[name],do_unlink=True)

    revisions={
      "L09": [(30,0,52),(28.35,0,52),(28.35,0,49),(28.35,0,47.90),(28.35,-3,41.30),
              (28.35,-3,39.90),(28.35,-6,32.30),(28.35,-6,31),(28,-6,31)],
      "L17": [(11,-6,52),(11.75,-6,52),(18.05,-3,52),(19.75,-3,52),(25.35,0,52),
              (26.5,0,52),(30,0,52)],
    }
    for link in layout["links"]:
        if link["id"] in revisions:
            link["points"]=revisions[link["id"]]
            link["note"]="Measured guide passes through the framed level sill and whole upper landing."
        if link["id"] in {"L15","L16"}:
            link["points"]=[(5,-10,27.40) if point==[5,-10,27.5] else point for point in link["points"]]
            link["note"]="Receiving station sits on the lower side of the first tread on the existing lower flight."
    layout["fitted_headhouse_landings"]=schedule
    layout_path=out/"authored-layout.json"
    out.mkdir(parents=True,exist_ok=True)
    write(layout_path,layout)
    staged={ext:out/f"mine-stair-thresholds.{ext}" for ext in ("blend","glb","hcs2")}
    temp=out/"_upper-flights.glb"
    export(temp,built)
    append(ROOT/source["glb"],temp,staged["glb"],remove_mesh_names=sorted(removed),reuse_embedded_images=True)
    temp.unlink()
    native_write(ROOT/source["native"],staged["hcs2"],removed,built)
    prior_parts=native_records(ROOT/source["native"])
    current_parts=native_records(staged["hcs2"])
    kept=[r for r in prior_parts if r[0] not in removed]
    assert len(prior_parts)-len(kept)==len(removed)==36
    assert current_parts[:len(kept)]==kept
    bpy.ops.file.pack_all()
    bpy.ops.wm.save_as_mainfile(filepath=str(staged["blend"]),compress=True)
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.import_scene.gltf(filepath=str(staged["glb"]))
    for part in current_parts[len(kept):]:
        assert match_geometry(triangles(part),[tri for tri,_ in records(bpy.context.scene.objects[part[0]])]),part[0]

    frames=layout["authored_doorways"]
    leaf=ROOT/"assets/runtime/models/door_1_loose.glb"
    directions=(-1,1,-1,1,1,1,1,1)
    doors=[]
    for pid,(frame,swing) in enumerate(zip(frames,directions,strict=True),700):
        a,b=frame["rooms"]
        fixed,along,axis=frame["boundary"],frame["along"],frame["axis"]
        x,z=(along,fixed) if axis=="z" else (fixed,along)
        doors.append((pid,frame["id"],a,b,owner(a),owner(b),x,frame["sill_y"],z,frame["clear_width"],
            axis,fixed-.12,fixed+.12,frame["clear_height"],2,1,swing,sha(leaf)))
    table(out/"door-survey.tsv",doors)
    fitted=out/"mine-fitted-doors.blend"
    fit_editor_doors(staged["blend"],out/"door-survey.tsv",leaf,fitted,editor_vertices,"Dead Manâ€™s Claim")
    hashes={ext:sha(path) for ext,path in staged.items()}
    result=dict(source=source,exports=hashes,edited_door_scene=fitted.relative_to(ROOT).as_posix(),
        edited_door_scene_sha256=sha(fitted),retired=sorted(removed),
        retained_objects=len(kept),new_objects=len(current_parts)-len(kept),landings=schedule,
        stair_components=[name for name in original["stair_components"] if name not in removed]
            +[r[0] for r in current_parts[len(kept):]])
    write(out/"construction.json",result)
    draw_plan(out,schedule)
    staging=layout["staging"]
    rows=[("SOURCE",staged["glb"].relative_to(ROOT).as_posix(),hashes["glb"],
           staged["hcs2"].relative_to(ROOT).as_posix(),hashes["hcs2"]),
          ("DOCK",*staging["point"],staging["yaw"])]
    for zone in layout["zones"]:
        name=zone["id"]
        rows.append(("SPACE",name,owner(name),*zone["bounds"],zone["y"],zone["height"],
                      str(zone["kind"] in {"outside","staging"}).lower(),"true"))
    rows.extend(("TREAD",part) for part in result["stair_components"])
    rows.extend(("RITUAL",r["rite"].lower(),owner(r["zone"]),*r["point"]) for r in layout["rituals"])
    for link in layout["links"]:
        rows.extend(("POINT",link["id"],*point) for point in link["points"])
    rows.extend(("POINT","stores-work-door",*point) for point in (
        (8,0,38),(10,0,42),(14,0,42),(18,0,42)))
    table(out/"arrival.tsv",rows)
    entries=[("ANCHOR",index,name,*ANCHORS[name]) for name,index in OWNERS.items()]
    entries.extend(("TRANSITION",pid,owner(a),owner(b),x,y,z,1.4,axis,kind)
        for pid,(a,b,x,y,z,axis,kind) in enumerate(OPEN_INTERFACES,710))
    table(out/"mission.tsv",entries)
    print("PASS two rebuilt upper flights,",len(kept),"retained native components, and eight editable doorway fittings")


def draw_plan(out,schedule):
    width,height=1110,760
    ink="#152b33"
    outline="#71848a"
    floor="#e6e9e5"
    landing="#a4dacf"
    door="#b84a34"
    lines=[f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {width} {height}" role="img" aria-labelledby="title">']
    lines.extend(('<title id="title">Dead Man&#8217;s Claim headhouse threshold plan and sections</title>',
        '<defs><marker id="arrow" viewBox="0 0 10 10" refX="8" refY="5" markerWidth="6" markerHeight="6" orient="auto"><path d="M0 0 L10 5 L0 10 Z" fill="#b84a34"/></marker></defs>',
        f'<rect width="{width}" height="{height}" fill="#f7f6f0"/>'))
    def label(x,y,text,size=14,weight=400):
        lines.append(f'<text x="{x}" y="{y}" font-family="Arial, sans-serif" font-size="{size}" font-weight="{weight}" fill="{ink}">{text}</text>')
    def rect(x,y,w,h,fill,stroke=outline):
        lines.append(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" fill="{fill}" stroke="{stroke}"/>')
    def line(x,y,u,v,stroke=outline,dash="",arrow=False):
        marker=' marker-end="url(#arrow)"' if arrow else ""
        lines.append(f'<line x1="{x}" y1="{y}" x2="{u}" y2="{v}" stroke="{stroke}" stroke-width="2" stroke-dasharray="{dash}"{marker}/>')

    label(36,55,"Dead Man&#8217;s Claim",35,700)
    label(36,87,"Headhouse door swings and stair receiving landings",20)
    label(36,128,"PLAN AT ROADHEAD DATUM  &#183;  Y 0.00 m",14,700)
    k=36
    def xy(x,z):
        return 82+(x-19)*k,148+(z-44)*k
    def box(x,z,far_x,far_z,color):
        u,v=xy(x,z)
        rect(u,v,(far_x-x)*k,(far_z-z)*k,color)
    box(26.5,49,31.25,54.25,floor)
    box(27.35,44.4,29.35,49,floor)
    box(19.6,51,26.5,53,floor)
    box(27.35,47.9,29.35,49,landing)
    box(25.35,51,26.5,53,landing)
    for t in range(1,8):
        z=47.9-t*(6.5/17)
        if z>44.4:
            line(*xy(27.35,z),*xy(29.35,z))
        x=25.35-t*(5.5/17)
        line(*xy(x,51),*xy(x,53))
    line(*xy(27.45,49),*xy(27.45,49.9),door,arrow=True)
    line(*xy(29.25,49),*xy(29.25,49.9),door,arrow=True)
    line(*xy(26.5,51.1),*xy(27.4,51.1),door,arrow=True)
    line(*xy(26.5,52.9),*xy(27.4,52.9),door,arrow=True)
    line(*xy(28.1,54.25),*xy(28.1,55.15),door,arrow=True)
    line(*xy(29.9,54.25),*xy(29.9,55.15),door,arrow=True)
    label(387,386,"Shared level headhouse",15,700)
    label(100,389,"Ventilation stair",15,700)
    label(111,407,"1.15 m receiving landing",13)
    label(402,186,"Haul stair",15,700)
    label(402,204,"1.10 m receiving landing",13)
    label(386,579,"Roadhead approach",14)
    rect(42,642,15,15,landing)
    label(67,654,"Whole, 2.00 m wide level shaft-side landings",13)
    line(42,681,60,681,door,arrow=True)
    label(67,685,"Door leaf opens onto the flat headhouse or roadhead",13)

    for y,source in zip((133,424),schedule,strict=True):
        name=source["shaft"].capitalize()
        rect(600,y,469,249,"#ffffff","#c6d0cf")
        label(623,y+34,name+" upper flight and receiving landing",16,700)
        step_scale=43
        start=633
        low=y+190
        points=[(start,low)]
        for i in range(TREADS):
            x=start+i*(source["run"]/TREADS)*step_scale
            top=low-(i+1)*(3/TREADS)*step_scale
            points.extend(((x,points[-1][1]),(x,top)))
        full=start+source["run"]*step_scale
        front=full+source["landing_depth"]*step_scale
        top=low-3*step_scale
        points.extend(((full,top),(front,top)))
        profile=" ".join(f"{x:.2f},{y0:.2f}" for x,y0 in points)
        lines.append(f'<polyline points="{profile}" fill="none" stroke="{ink}" stroke-width="2.4"/>')
        line(full,top,front,top,landing)
        line(front,top-17,front,top+26,door)
        line(full,top+12,front,top+12,outline)
        label(623,y+218,f'17 equal risers over {source["run"]:.2f} m; landing {source["landing_depth"]:.2f} m deep.',13)
    lines.append("</svg>")
    (out/"headhouse-threshold-plan.svg").write_text("\n".join(lines)+"\n",encoding="utf-8",newline="\n")


def render_landings(out):
    bpy.ops.wm.open_mainfile(filepath=str(out/"mine-fitted-doors.blend"))
    scene=bpy.context.scene
    scene.render.engine="BLENDER_WORKBENCH"
    shading=scene.display.shading
    shading.color_type="MATERIAL"
    shading.show_cavity=True
    shading.show_shadows=True
    shading.cavity_type="BOTH"
    scene.view_settings.view_transform="Standard"
    scene.render.resolution_x=1280
    scene.render.resolution_y=920
    scene.render.resolution_percentage=100
    scene.render.image_settings.file_format="PNG"
    bpy.ops.object.camera_add()
    camera=bpy.context.object
    scene.camera=camera
    camera.data.type="ORTHO"
    dst=out/"captures"/"structure"
    dst.mkdir(parents=True,exist_ok=True)
    target=Vector((26,-47,-.4))
    camera.location=Vector((43,-68,31))
    camera.rotation_euler=(target-camera.location).to_track_quat("-Z","Y").to_euler()
    camera.data.ortho_scale=34
    shafts={"Mine headhouse roof region0","Mine haul stair weather roof region0","Mine vent outside weather roof region0"}
    assert all(bpy.data.objects.get(n) is not None for n in shafts)
    for name in shafts:
        bpy.data.objects[name].hide_render=True
    scene.render.filepath=str(dst/"headhouse-roof-cutaway.png")
    bpy.ops.render.render(write_still=True)
    # A second view looks straight across the two physical top landings.
    target=Vector((27.9,-50.3,0))
    camera.location=Vector((35,-57.5,16))
    camera.rotation_euler=(target-camera.location).to_track_quat("-Z","Y").to_euler()
    camera.data.ortho_scale=12
    next(c for c in bpy.data.collections if c.name.endswith("door leaves closed")).hide_render=True
    next(c for c in bpy.data.collections if c.name.endswith("door leaves open")).hide_render=False
    scene.render.filepath=str(dst/"headhouse-open-landings.png")
    bpy.ops.render.render(write_still=True)


if __name__=="__main__":
    args=sys.argv[sys.argv.index("--")+1:]
    output=Path(args[0]).resolve()
    if "--captures-only" in args:
        render_landings(output)
        draw_plan(output,json.loads((output/"construction.json").read_bytes())["landings"])
    else:
        build(output,Path(args[1]).resolve())
