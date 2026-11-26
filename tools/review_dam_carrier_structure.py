"""Draw the measured carrier plan and inspect the finished native terminals."""
import csv
from pathlib import Path
import sys

import bpy
from mathutils import Vector

sys.path.insert(0,str(Path(__file__).resolve().parent))
from build_dam_carrier import OUT, STEM
from build_dam_valley import BOUNDARY


def plan(rows):
    def xy(x,z):
        return 132+(x+12)*4.1,106+(z+38)*4.1

    def section(s,y):
        return 124+s*4.2,680-y*14

    def line(values):
        return " ".join("%.2f,%.2f"%value for value in values)

    route=line(xy(r["x"],r["z"]) for r in rows)
    ground=line(section(r["chainage"],r["lower_side_grade"]) for r in rows)
    pipe=line(section(r["chainage"],r["y"]) for r in rows)
    boundary=line(xy(*p) for p in BOUNDARY)
    inlet=xy(rows[0]["x"],rows[0]["z"])
    outlet=xy(rows[-1]["x"],rows[-1]["z"])
    point=lambda p:f"{p[0]:.2f},{p[1]:.2f}"
    svg=f"""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 855 915" role="img" aria-labelledby="title desc">
  <title id="title">Blackwater Dam covered hydraulic carrier</title>
  <desc id="desc">Plan and longitudinal section from the surveyed reservoir intake to the downstream receiving headwall. The alignment skirts the compound and passes behind the original closed isolation sluice.</desc>
  <style>text{{font:16px Arial,sans-serif;fill:#e8efe8}} .h{{font-size:26px;font-weight:bold}} .muted{{fill:#adbfbb}} .steel{{stroke:#8ed2e7;fill:none}} .soil{{stroke:#c9ae81;fill:none}}</style>
  <rect width="855" height="915" fill="#162629"/>
  <text class="h" x="33" y="42">Blackwater Dam covered hydraulic carrier</text>
  <text class="muted" x="33" y="70">Survey plan · North up · Intake to the closed sluice</text>
  <polygon points="{boundary}" fill="#324046" stroke="#a6b4a7" stroke-width="2"/>
  <text x="272" y="428">Existing investigation compound</text>
  <polyline points="{route}" class="steel" stroke-width="3.5"/>
  <circle cx="{inlet[0]:.2f}" cy="{inlet[1]:.2f}" r="6" fill="#e9ba81"/>
  <circle cx="{outlet[0]:.2f}" cy="{outlet[1]:.2f}" r="6" fill="#e9ba81"/>
  <path d="M{point(inlet)}H246V119" fill="none" stroke="#e9ba81"/>
  <text x="104" y="111">Screened intake</text>
  <path d="M{point(outlet)}H605V392" fill="none" stroke="#e9ba81"/>
  <text x="604" y="416">Receiving headwall</text>
  <path d="M590 559h82m-82-5v10m82-10v10" stroke="#e8efe8" stroke-width="2"/>
  <text class="muted" x="610" y="588">20 m</text>
  <text x="34" y="626" font-weight="bold">Longitudinal section</text>
  <text class="muted" x="34" y="651">Section height exaggerated · metres relative to the crest datum</text>
  <path d="M124 680h631m-631 70h631m-631 70h631" stroke="#647878" stroke-width="1" stroke-dasharray="3 5"/>
  <text class="muted" x="85" y="686">0</text>
  <text class="muted" x="78" y="756">−5</text>
  <text class="muted" x="69" y="826">−10</text>
  <polyline points="{ground}" class="soil" stroke-width="2"/>
  <polyline points="{pipe}" class="steel" stroke-width="3"/>
  <text class="muted" x="122" y="862">0 m · Intake</text>
  <text class="muted" x="626" y="862">150.3 m · Tailrace</text>
  <path d="M126 890h30" class="soil" stroke-width="2"/><text x="162" y="896">Lower side grade</text>
  <path d="M424 890h30" class="steel" stroke-width="3"/><text x="461" y="896">Installed carrier centreline</text>
</svg>
"""
    (OUT/"carrier-plan-and-section.svg").write_text(svg,encoding="utf8",newline="\n")


def captures():
    bpy.ops.wm.open_mainfile(filepath=str(OUT/(STEM+".blend")))
    scene=bpy.context.scene
    scene.render.engine="BLENDER_WORKBENCH"
    scene.display.shading.color_type="MATERIAL"
    scene.display.shading.show_cavity=True
    scene.display.shading.background_type="WORLD"
    if scene.world is None:
        scene.world=bpy.data.worlds.new("Construction review")
    scene.world.color=(.24,.27,.265)
    scene.render.resolution_x,scene.render.resolution_y=1200,850
    scene.render.resolution_percentage=100
    bpy.ops.object.camera_add()
    camera=bpy.context.object
    scene.camera=camera
    camera.data.lens=40
    folder=OUT/"captures/structure"
    folder.mkdir(parents=True,exist_ok=True)

    def view(name,eye,target):
        camera.location=eye
        camera.rotation_euler=(Vector(target)-camera.location).to_track_quat("-Z","Y").to_euler()
        scene.render.filepath=str(folder/(name+".png"))
        bpy.ops.render.render(write_still=True)

    view("intake-fitted-cover",(34.8,32.3,-.7),(29.95,28,-4))
    view("tailrace-receiving-cover",(77.9,-48.8,-3.1),(73.91,-44.95,-6.62))
    keep=("open-bore hydraulic carrier","receiving headwall","receiving toe","upstream closed sluice")
    for obj in scene.objects:
        if obj.type=="MESH":
            obj.hide_render=not any(term in obj.name for term in keep)
    view("carrier-receiving-cutaway",(71.1,-42.3,-5.3),(74.02,-45.35,-7.02))


if __name__=="__main__":
    with (OUT/"carrier-profile.tsv").open(newline="") as stream:
        rows=[{key:float(value) for key,value in row.items() if key!="region"}
              for row in csv.DictReader(stream,delimiter="\t")]
    plan(rows)
    captures()
