"""Reopen both Battery exports for standing, site and explicit cutaway review."""
from pathlib import Path
import sys

import bpy
from mathutils import Vector

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/"tools"))

from build_college_grounds import import_scene, place_review_van, terrain_tree
from build_measured_approaches import read, relative, sha, write
from build_seacliff_battery import FLIGHTS, LOWER_BAY, PORTAL, STEM, TERRACE, WELL
from prepare_seacliff_battery import BASE, BASELINE, FIELDS, PASS, SITE
from verify_interior_stair_landings import svg_line, svg_rect, svg_text

CAPTURES = BASE/"captures/seacliff-receiving-batch"
VIEWS = {
    "site": ((83,-94,60),(39,-37,4),120),
    "exterior-courts": ((48,-70,16),(37,-57,1.8),30),
    "ramp-to-battery": ((34,-83,1.62),(33,-67,1.43),0),
    "intermediate-court": ((33.65,-57.25,3.62),(39.7,-56.1,2.85),0),
    "director-lower": ((30,-22.12,5.62),(37.5,-22.05,6.8),0),
    "director-upper": ((38.8,-24.35,8.62),(34.5,-22.0,6.44),0),
    "director-section": ((43,-26.7,15.5),(33,-21.75,5.60),17),
}
CUT = ("Ceiling cap director","Wall director S","Wall director E",
       "Battery flight south side region0","Battery southeast return region0")


def verify_bearings(objects):
    walls={name:terrain_tree(obj) for name,obj in objects.items()
           if obj.get("role")=="rough shell wall" and obj.get("zone") in ("records","pump","barracks","plant")}
    rows=[]
    for index,z in enumerate((21.25,23.55),1):
        label=f"Battery transfer beam {index} over lower casemate walls region0"
        beam=objects[label]
        vertices=[beam.matrix_world@v.co for v in beam.data.vertices]
        assert abs(min(v.z for v in vertices)-3)<.00001
        for x in (24,34,44):
            seats=[]
            for name,tree in walls.items():
                hit=tree.ray_cast(Vector((x,-z,3.02)),Vector((0,0,-1)),.06)[0]
                if hit is not None and abs(hit.z-3)<.00001:
                    seats.append(name)
            assert seats,(label,x,z,"missing wall bearing")
            rows.append(dict(beam=label,bearing_point=[x,3,z],retained_walls=seats))
    return rows


def render(folder, glb, layout, label, names, proofs):
    objects=import_scene(glb)
    bearings=verify_bearings(objects) if label=="after" else []
    place_review_van(layout["staging"])
    scene=bpy.context.scene
    scene.render.engine="BLENDER_WORKBENCH"
    scene.display.shading.color_type="MATERIAL"
    scene.display.shading.show_cavity=True
    scene.render.resolution_x,scene.render.resolution_y=1150,790
    scene.render.resolution_percentage=100
    bpy.ops.object.camera_add()
    camera=bpy.context.object
    scene.camera=camera
    for key in names:
        position,focus,scale=VIEWS[key]
        hidden=[]
        if key=="director-section":
            hidden=[name for name in CUT if name in objects]
            assert len(hidden)==len(CUT),hidden
            for name in hidden:objects[name].hide_render=True
        camera.location=position
        camera.rotation_euler=(Vector(focus)-camera.location).to_track_quat("-Z","Y").to_euler()
        camera.data.type="ORTHO" if scale else "PERSP"
        camera.data.ortho_scale=scale or 1
        camera.data.lens=23
        file=folder/f"battery-{key}-{label}.png"
        scene.render.filepath=str(file)
        bpy.ops.render.render(write_still=True)
        proofs.append(dict(image=relative(file),sha256=sha(file),source_glb_sha256=sha(glb),camera=position,
                           focus=focus,hidden_for_section=hidden,canonical_review_van=True))
        for name in hidden:objects[name].hide_render=False
    return bearings


def draw(layout,file):
    body=[svg_text(35,43,"Seacliff Battery","title"),
          svg_text(36,72,"Receiving terraces and director stair · coordinates in metres"),
          svg_text(35,110,"Exterior plan","heading"),svg_text(455,110,"Director stair plan","heading")]

    def planter(px,pz,x0,z0,scale):
        def xy(x,z):
            return px+(x-x0)*scale,pz+(z-z0)*scale
        def box(bounds,css):
            a,b,c,d=bounds
            x,z=xy(a,b)
            body.append(svg_rect(x,z,(c-a)*scale,(d-b)*scale,css))
        def shape(points,css):
            coords=" ".join(f"{x:.2f},{z:.2f}" for x,z in (xy(*p) for p in points))
            body.append(f'<polygon class="{css}" points="{coords}"/>')
        def line(points,css):
            body.append(svg_line([xy(*p) for p in points],css))
        return xy,box,shape,line

    xy,box,shape,line=planter(55,134,22,34,6)
    for _,_,_,_,points in FIELDS[:2]:
        shape(points,"yard")
    for key in ("route-02","route-03"):
        p,q,count=FLIGHTS[key]
        box((p[0]-.95,q[2],p[0]+.95,p[2]),"mass")
        for i in range(count+1):
            z=p[2]+(q[2]-p[2])*i/count
            line(((p[0]-.75,z),(p[0]+.75,z)),"nosing")
    box(TERRACE,"mass")
    for l in layout["links"]:
        if l["id"] in ("route-01","route-02","route-03"):
            line([(p[0],p[2]) for p in l["points"]],"guide")
    body.extend(svg_text(*xy(x,z),label) for x,z,label in (
        (29,41,"Y4 courtyard"),(43,55,"Y2 terrace"),(39,83,"Y0 forecourt"),
        (36,93,"Van and rear ramp"),
    ))

    xy,box,shape,line=planter(459,144,26,18,28)
    box((28,18.95,40,26),"deck")
    box(LOWER_BAY,"lower")
    box(WELL,"well")
    box((24,26,44,31),"lower")
    p,q,count=FLIGHTS["route-10"]
    for i in range(count+1):
        x=p[0]+(q[0]-p[0])*i/count
        line(((x,p[2]-.75),(x,p[2]+.75)),"nosing")
    line(((29.25,26),(30.75,26)),"door")
    route=next(l for l in layout["links"] if l["id"]=="route-10")
    line([(p[0],p[2]) for p in route["points"]],"guide")
    body.extend([svg_text(*xy(31.5,20.3),"Y7 director deck"),
                 svg_text(*xy(30.2,25.4),"Y4 receiving bay"),
                 svg_text(*xy(31.4,27.1),"Framed gallery door · 1.50 m clear"),
                 svg_text(*xy(31.5,29),"Y4 gallery")])
    body.extend([svg_line([(43,531),(82,531)],"guide"),svg_text(94,537,"Circulation guide"),
                 svg_line([(242,531),(281,531)],"door"),svg_text(293,537,"Doorway"),
                 svg_rect(457,521,22,16,"mass"),svg_text(489,537,"Concrete terrace or stairs"),
                 svg_rect(680,521,21,16,"yard"),svg_text(712,537,"Saved graded yard")])
    content=''.join(body)
    file.write_bytes(f'''<svg xmlns="http://www.w3.org/2000/svg" role="img" viewBox="0 0 970 565">
<title>Seacliff Battery measured receiving plan</title>
<style>
text{{font:15px "Segoe UI",sans-serif;fill:#243d48}} .title{{font-size:30px;font-weight:bold}}
.heading{{font-size:19px;font-weight:bold}}
.yard{{fill:#dedec7;stroke:#65684d}} .mass{{fill:#aebfc3;stroke:#4b6971}}
.deck{{fill:#dbe4e6;stroke:#52737a}} .lower{{fill:#edf0e6;stroke:#526b66}}
.well{{fill:#b9c6ca;stroke:#647e8a}}
.nosing,.guide,.door{{fill:none}}
.nosing{{stroke:#52696e;stroke-width:1.1}} .guide{{stroke:#a64f20;stroke-width:2.5;stroke-dasharray:6 4}}
.door{{stroke:#146e60;stroke-width:4}}
</style>
<rect width="970" height="565" fill="#f8f9f3"/>{content}</svg>'''.encode())


def review(target,captures):
    captures.mkdir(parents=True,exist_ok=True)
    source=next(row for row in read(BASELINE)["active_sources"] if row["id"]==SITE)
    folder=target/SITE.lower()
    layout,meta=(read(folder/name) for name in ("authored-layout.json","construction.json"))
    before,after=ROOT/source["glb"],folder/(STEM+".glb")
    assert sha(before)==source["glb_sha256"] and sha(after)==meta["glb_sha256"]
    proofs=[]
    render(captures,before,layout,"before",("site","exterior-courts","director-lower"),proofs)
    bearings=render(captures,after,layout,"after",tuple(VIEWS),proofs)
    plan=folder/"receiving-plan.svg"
    draw(layout,plan)
    write(folder/"visual-review.json",dict(model=relative(after),sha256=sha(after),native_sha256=meta["physics_sha256"],
          plan=relative(plan),plan_sha256=sha(plan),transfer_wall_seats=bearings,captures=proofs))


if __name__=="__main__":
    args=sys.argv[sys.argv.index("--")+1:] if "--" in sys.argv else []
    review((ROOT/args[0]).resolve() if args else PASS,(ROOT/args[1]).resolve() if len(args)>1 else CAPTURES)
