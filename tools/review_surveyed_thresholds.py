
"""Draw threshold plan slices from the exact saved geometry and render review views."""
from pathlib import Path
import sys
from xml.sax.saxutils import escape

import bpy
from mathutils import Vector

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools"))

from build_college_grounds import import_scene
from build_surveyed_thresholds import OUT
from build_measured_approaches import BASE, read, relative, sha, write
from verify_interior_stair_landings import svg_line, svg_text

CAPTURES = BASE / "captures/flush-porch-and-threshold-batch"
# Clip the plan slices to the affected bays. The mesh supplies all physical geometry.
VIEWS = {
    "ACADEMY": ("Academy · west receiving floor", (11, 40, 22, 49), 0, ("L40",),
                (13.4,-45.2,1.62), (19.2,-43.2,1.18)),
    "ASYLUM": ("Asylum · hydrotherapy suite", (39, 29, 53, 40), 0, ("L40","L41","L42","L43"),
               (40.1,-34.8,1.62), (47.8,-32.5,1.27)),
    "PRISON": ("Penitentiary · eastern gallery", (47, 39, 58, 57), 0, ("L050",),
               (53,-51.6,1.62), (54.5,-54.5,1.23)),
    "WLD-18": ("Cinder-6 · west escape landing", (4, 30, 15.5, 41), -4, ("route-08",),
               (9.7,-34,-2.38), (6,-33.7,-2.77)),
}


def plan(file, title, bounds, level, chosen, layout, objects):
    x0,z0,x1,z1 = bounds
    scale = min(620/(x1-x0), 355/(z1-z0))

    def project(p):
        return 55+(p[0]-x0)*scale, 103+(p[1]-z0)*scale

    floors, walls = [], []
    for obj in objects.values():
        role = obj.get("role", "")
        if not role.startswith(("structural","architectural","terrain","bearing","boundary",
                                "floor","rough shell wall","rough opening","mounted","continuous")):
            continue
        mesh = obj.data
        points = [(v.x,-v.y,v.z) for v in (obj.matrix_world @ node.co for node in mesh.vertices)]
        if not points:
            continue
        low = [min(p[i] for p in points) for i in range(3)]
        high = [max(p[i] for p in points) for i in range(3)]
        if low[0]>x1 or high[0]<x0 or low[1]>z1 or high[1]<z0 or low[2]>level+1.05 or high[2]<level-.025:
            continue
        mesh.calc_loop_triangles()
        for face in mesh.loop_triangles:
            vertices = [points[i] for i in face.vertices]
            if all(abs(p[2]-level)<.025 for p in vertices):
                area = sum(a[0]*b[1]-a[1]*b[0] for a,b in zip(vertices, vertices[1:]+vertices[:1]))
                if area < -1e-7:  # +Y in game space, reflected in the plan projection.
                    polygon = " ".join(f"{a:.2f},{b:.2f}" for a,b in map(project,vertices))
                    floors.append(f'<polygon points="{polygon}"/>')
            cut = level+1.05
            hits = []
            for a,b in zip(vertices, vertices[1:]+vertices[:1]):
                if (a[2]<=cut<b[2]) or (b[2]<=cut<a[2]):
                    f = (cut-a[2])/(b[2]-a[2])
                    hits.append(project((a[0]+f*(b[0]-a[0]),a[1]+f*(b[1]-a[1]))))
            if len(hits)==2:
                walls.append(svg_line(hits,"linework"))
    assert floors and walls, (title, len(floors), len(walls))
    guides, doors = [], []
    for link in (l for l in layout["links"] if l["id"] in chosen):
        for a,b in zip(link["points"],link["points"][1:]):
            if abs(a[1]-level)<.01 and abs(b[1]-level)<.01:
                guides.append(svg_line([project((a[0],a[2])),project((b[0],b[2]))],"guide"))
        if "portal" in link:
            for door in layout["authored_doorways"]:
                if door["id"]!=link["portal"] or abs(door["sill_y"]-level)>.01:
                    continue
                m,h,w = door["along"],door["clear_width"]/2,door["boundary"]
                ends = ((m-h,w),(m+h,w)) if door["axis"]=="z" else ((w,m-h),(w,m+h))
                doors.append(svg_line([project(p) for p in ends],"door"))
    width,height = (x1-x0)*scale,(z1-z0)*scale
    body = f'''
<defs><clipPath id="window"><rect x="55" y="103" width="{width:.2f}" height="{height:.2f}"/></clipPath></defs>
<text x="38" y="42" class="title">{escape(title)}</text>
<text x="38" y="71">Structural plan at floor Y {level:+.2f} m · north up</text>
<rect x="55" y="103" width="{width:.2f}" height="{height:.2f}" fill="#e8e7de" stroke="#aaa89c"/>
<g clip-path="url(#window)">
<g fill="#cfdee0">{''.join(floors)}</g>{''.join(walls)}{''.join(guides)}{''.join(doors)}
</g>
{svg_line([(56,492),(90,492)],"guide")}{svg_text(99,497,"Circulation guide")}
{svg_line([(272,492),(304,492)],"door")}{svg_text(314,497,"Fitted doorway")}
{svg_line([(467,492),(499,492)],"linework")}{svg_text(509,497,"Structure at +1.05 m")}
{svg_line([(56,467),(56+scale,467)],"linework")}{svg_text(65+scale,472,"1 m")}
'''
    file.write_bytes(f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 750 520" role="img">
<title>{escape(title)}</title>
<style>
text{{font:15px "Segoe UI",sans-serif;fill:#213747}} .title{{font-size:27px;font-weight:700}}
.linework,.guide,.door{{fill:none;stroke-linecap:round}}
.linework{{stroke:#354b57;stroke-width:1.65}} .guide{{stroke:#9e431d;stroke-width:2.4;stroke-dasharray:5 4}}
.door{{stroke:#087267;stroke-width:4}}
</style>
<rect width="750" height="520" fill="#fafaf5"/>{body}</svg>
'''.encode())


def render(file, position, focus):
    scene = bpy.context.scene
    scene.render.engine = "BLENDER_WORKBENCH"
    shading = scene.display.shading
    shading.color_type = "MATERIAL"
    shading.show_cavity = True
    scene.render.resolution_x,scene.render.resolution_y = 1190,810
    scene.render.resolution_percentage = 100
    bpy.ops.object.camera_add(location=position)
    camera = bpy.context.object
    camera.rotation_euler = (Vector(focus)-camera.location).to_track_quat("-Z","Y").to_euler()
    camera.data.lens = 23
    scene.camera = camera
    scene.render.filepath = str(file)
    bpy.ops.render.render(write_still=True)


def main():
    CAPTURES.mkdir(parents=True,exist_ok=True)
    for row in read(OUT/"roster.json")["active_sources"]:
        site = row["id"]
        if site not in VIEWS:
            continue
        title,bounds,level,chosen,position,focus = VIEWS[site]
        model = ROOT/row["glb"]
        assert sha(model)==row["glb_sha256"] and sha(ROOT/row["native"])==row["native_sha256"]
        objects = import_scene(model)
        folder = OUT/site.lower()
        drawing = folder/"measured-plan.svg"
        plan(drawing,title,bounds,level,chosen,read(ROOT/row["layout"]),objects)
        png = CAPTURES/f"{site.lower()}-standing.png"
        render(png,position,focus)
        write(folder/"export-review.json",dict(glb_sha256=row["glb_sha256"],native_sha256=row["native_sha256"],
              measured_plan=relative(drawing),measured_plan_sha256=sha(drawing),layout_sha256=sha(ROOT/row["layout"]),
              view=relative(png),view_sha256=sha(png),camera=list(position),focus=list(focus),
              scope="Headless native standing-height inspection, complete saved mesh and inspection lighting."))
        print("SURVEYED THRESHOLD REVIEW",site,flush=True)


if __name__=="__main__":
    main()
