"""Review the actual saved decks and doorway approaches and draw measured plan details."""
from pathlib import Path
import sys
from xml.sax.saxutils import escape

import bpy
from mathutils import Vector

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/"tools"))

from build_college_grounds import import_scene
from build_fitted_approaches import OUT, SITES
from build_measured_approaches import BASE, read, relative, sha, write
from verify_interior_stair_landings import svg_line, svg_rect, svg_text

CAPTURES = BASE/"captures/native-foyer-door-batch"
VIEWS = {
    "WLD-06":("wharf-supported-crossdock",(48,-23,19),(32,-13,0),34),
    "WLD-16":("eight-pines-office-to-manager",(18,-45,1.57),(10,-45,1.28),0),
    "CHURCH":("parish-crypt-entry-threshold",(28.5,-23,1.58),(29.3,-30,1.1),0),
}


def draw(file, layout):
    site=layout["id"]
    links={path["id"]:path["points"] for path in layout["links"]}
    title=layout["title"]
    parts=[svg_text(34,43,title,"title")]

    def setup(x0,z0,scale):
        def xy(x,z):
            return 65+(x-x0)*scale,105+(z-z0)*scale
        return xy

    def floor(box,css="floor"):
        a,b,c,d=box
        x,z=xy(a,b)
        x2,z2=xy(c,d)
        parts.append(svg_rect(x,z,x2-x,z2-z,css))

    def guide(points):
        parts.append(svg_line([xy(x,z) for x,y,z in points],"guide"))

    def gate(axis,wall,middle,width):
        a,b=middle-width/2,middle+width/2
        ends=[xy(a,wall),xy(b,wall)] if axis=="z" else [xy(wall,a),xy(wall,b)]
        parts.append(svg_line(ends,"door"))

    if site=="WLD-06":
        xy=setup(20,6,18)
        floor((20,6,44,10))
        floor((20,10,24,26))
        floor((40,10,44,26))
        floor((24,10,40,24),"building")
        gate("z",10,32,2.4)
        guide(links["L08"])
        guide(links["L09"])
        parts.append(svg_text(227,257,"Fish works"))
        parts.append(svg_text(524,136,"Full crossdock at Z8","caption"))
        parts.append(svg_text(524,174,"Guidance turns north of the Z10 wall.","caption"))
    elif site=="WLD-16":
        xy=setup(6,40,23)
        floor((6,40,13,50),"building")
        floor((13,42,23,50),"building")
        gate("z",50,18,1.05)
        gate("z",42,18,1.05)
        gate("x",13,45,1.05)
        guide(links["route-05"])
        guide([[18,0,52],*links["route-02"][2:]])
        guide(links["route-04"][:3])
        parts.append(svg_text(95,169,"Manager"))
        parts.append(svg_text(320,190,"Office"))
        parts.append(svg_text(526,150,"Three office doors · 1.05 m clear","caption"))
        parts.append(svg_text(526,188,"The west door leads to the manager’s room.","caption"))
    else:
        xy=setup(26,14,19)
        floor((26,14,34,26),"building")
        floor((26,26,34,34))
        floor((30.55,27.35,32.45,32.4),"well")
        gate("z",26,28.5,1.05)
        guide(links["L16"])
        parts.append(svg_text(91,168,"Chapel"))
        parts.append(svg_text(114,475,"Crypt landing","caption"))
        parts.append(svg_text(275,330,"Chapel-to-crypt frame · 1.05 m clear","caption"))
        parts.append(svg_text(275,367,"Full floor west of the guarded well","caption"))

    parts.extend([
        svg_line([(45,521),(77,521)],"guide"),svg_text(90,528,"Nonphysical guidance","caption"),
        svg_line([(279,521),(313,521)],"door"),svg_text(326,528,"Fitted doorway","caption"),
        svg_rect(517,509,18,15,"well"),svg_text(548,528,"Structural well","caption"),
    ])
    svg=f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 840 554" role="img">
<title>{escape(title)} measured approach plan</title>
<style>
text{{font:16px "Segoe UI",sans-serif;fill:#24394a}}
.title{{font-size:28px;font-weight:700}} .caption{{font-size:14px}}
.floor{{fill:#dbe8ee;stroke:#344f63;stroke-width:1.6}} .building{{fill:#e0ddd0;stroke:#64533d;stroke-width:1.6}}
.well{{fill:#e9ccb7;stroke:#8c6448;stroke-width:1.6}} .guide,.door{{fill:none}}
.guide{{stroke:#b45825;stroke-width:2.6;stroke-dasharray:5 4}} .door{{stroke:#15776d;stroke-width:4.5}}
</style>
<rect width="840" height="554" fill="#f9faf7"/>
{''.join(parts)}
</svg>
'''
    file.write_bytes(svg.encode())


def main():
    CAPTURES.mkdir(parents=True,exist_ok=True)
    for row in read(OUT/"roster.json")["active_sources"]:
        site=row["id"]
        if site not in SITES:
            continue
        glb=ROOT/row["glb"]
        assert sha(glb)==row["glb_sha256"]
        layout=read(ROOT/row["layout"])
        folder=OUT/site.lower()
        draw(folder/"measured-plan.svg",layout)
        import_scene(glb)
        name,pos,target,scale=VIEWS[site]
        scene=bpy.context.scene
        scene.render.engine="BLENDER_WORKBENCH"
        scene.display.shading.color_type="MATERIAL"
        scene.display.shading.show_cavity=True
        scene.render.resolution_x,scene.render.resolution_y=1190,810
        scene.render.resolution_percentage=100
        bpy.ops.object.camera_add(location=pos)
        camera=bpy.context.object
        camera.rotation_euler=(Vector(target)-camera.location).to_track_quat("-Z","Y").to_euler()
        camera.data.type="ORTHO" if scale else "PERSP"
        camera.data.ortho_scale=scale or 1
        camera.data.lens=22
        scene.camera=camera
        png=CAPTURES/(name+".png")
        scene.render.filepath=str(png)
        bpy.ops.render.render(write_still=True)
        write(folder/"export-review.json",dict(source_glb_sha256=row["glb_sha256"],
                 source_native_sha256=row["native_sha256"],capture=relative(png),capture_sha256=sha(png),
                 plan_sha256=sha(folder/"measured-plan.svg"),camera=list(pos),focus=list(target)))
        print("MEASURED APPROACH REVIEW",site,flush=True)


if __name__ == "__main__":
    main()
