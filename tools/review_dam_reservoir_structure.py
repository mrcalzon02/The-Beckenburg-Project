"""Inspect the exported reservoir shore and before/after native construction."""
from pathlib import Path
import json
import sys
import bpy
from mathutils import Vector

root=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(root/"tools"))
from build_dam_reservoir import ENVELOPE, WATER_Y
from build_dam_valley import grades
from build_roster_foundations import material

packet=root/"development/levels/rebuild/dam-reservoir-pass/wld-09"
meta=json.loads((packet/"construction.json").read_bytes())
bpy.ops.wm.open_mainfile(filepath=str(packet/"dam-with-reservoir.blend"))
ground=grades(bpy.data.objects["Blackwater joined valley terrain"],bpy.data.objects[meta["resurfaced_render_mesh"]])
x0,z0,x1,z1=ENVELOPE
# Reconstruct the actual shore on the survey grid. Continuous lines are
# interpolated only between surveyed terrain heights across a quarter metre.
step=.25
grid={(i,j):ground(x0+i*step,z0+j*step)-WATER_Y for i in range(173) for j in range(97)}
edges={}
for i in range(172):
    for j in range(96):
        corners=((i,j),(i+1,j),(i+1,j+1),(i,j+1))
        contour=[]
        for a,b in zip(corners,corners[1:]+corners[:1]):
            if grid[a]*grid[b]<0:
                t=grid[a]/(grid[a]-grid[b])
                contour.append((round(x0+step*(a[0]+(b[0]-a[0])*t),4),
                                round(z0+step*(a[1]+(b[1]-a[1])*t),4)))
        assert len(contour) in (0,2),(i,j,contour)
        if contour:
            a,b=contour
            edges.setdefault(a,[]).append(b)
            edges.setdefault(b,[]).append(a)
assert all(len(n)==2 for n in edges.values())
start=min(edges)
line=[start,edges[start][0]]
while line[-1]!=start:
    line.append(next(p for p in edges[line[-1]] if p!=line[-2]))
assert len(line)==len(edges)+1

def pos(x,z):
    return (132+(x+8)*12,150+(z+37)*12)
def path(points):
    return " ".join(("%.2f,%.2f"%pos(x,z)) for x,z in points)
bound=[(16,2),(26,-2),(36,-3),(48,-4)]
box=path(((x0,z0),(x1,z0),(x1,z1),(x0,z1)))
outline=path(line)
intake=path(((25.2,-29.47),(30.14,-29.47),(30.14,-26.53),(25.2,-26.53)))
screen=path(((25.92,-29.06),(25.92,-26.94)))
svg=f"""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 910 720" role="img" aria-labelledby="title desc">
  <title id="title">Blackwater Dam north headpond and screened intake</title>
  <desc id="desc">The contour follows actual surveyed terrain at elevation minus 4.10 metres. The intake on the east bank sits north of the existing compound barrier. Its carrier is capped.</desc>
  <style>text{{font-family:Arial,sans-serif;fill:#e7eee6;font-size:16px}} .head{{font-size:27px;font-weight:bold}} .dim{{fill:#a8bec0}} .key{{fill:#efd1a1}}</style>
  <rect width="910" height="720" fill="#19272a"/>
  <text class="head" x="35" y="50">Blackwater Dam north headpond</text>
  <text class="dim" x="35" y="77">Surveyed shore at −4.10 m · North up · 0.25 m terrain sampling</text>
  <polygon points="{box}" fill="none" stroke="#9bb5b0" stroke-width="2" stroke-dasharray="7 6"/>
  <polygon points="{outline}" fill="#345c70" stroke="#9cd3dd" stroke-width="2"/>
  <polygon points="{intake}" fill="#9d9c83" stroke="#ebd9b2" stroke-width="2"/>
  <polyline points="{screen}" fill="none" stroke="#593c2f" stroke-width="3"/>
  <polyline points="{path(bound)}" fill="none" stroke="#bca68d" stroke-width="3"/>
  <path d="M583 258H690V221" fill="none" stroke="#efd1a1"/>
  <text class="key" x="674" y="192">Screened intake</text>
  <text class="dim" x="674" y="215">East carrier capped</text>
  <text x="290" y="292">Natural headpond</text>
  <text class="dim" x="430" y="628">Existing north compound barrier</text>
  <path d="M700 386h120m-120-5v10m120-10v10" stroke="#e7eee6" stroke-width="2"/>
  <text x="743" y="410">10 m</text>
  <path d="M131 665h34" stroke="#a8bec0" stroke-width="2" stroke-dasharray="7 6"/>
  <text x="177" y="670">Water placement extent lies beneath the surrounding ground</text>
</svg>
"""
(packet/"headpond-plan.svg").write_text(svg,encoding="utf8",newline="\n")

folder=packet/"captures/structure"
folder.mkdir(parents=True,exist_ok=True)

def setup():
    scene=bpy.context.scene
    scene.render.engine="BLENDER_WORKBENCH"
    scene.display.shading.color_type="MATERIAL"
    scene.display.shading.show_cavity=True
    scene.display.shading.background_type="WORLD"
    if scene.world is None:
        scene.world=bpy.data.worlds.new("Construction review backdrop")
    scene.world.color=(.22,.26,.25)
    scene.render.resolution_x,scene.render.resolution_y=1200,850
    scene.render.resolution_percentage=100
    bpy.ops.object.camera_add()
    camera=bpy.context.object
    scene.camera=camera
    camera.data.clip_end=360
    return scene,camera

scene,camera=setup()

def preview(name,eye,target,orthographic):
    camera.data.type="ORTHO" if orthographic else "PERSP"
    camera.data.ortho_scale=orthographic or 1
    camera.data.lens=43
    camera.location=eye
    camera.rotation_euler=(Vector(target)-camera.location).to_track_quat("-Z","Y").to_euler()
    scene.render.filepath=str(folder/(name+".png"))
    bpy.ops.render.render(write_still=True)

preview("intake-screen-and-abutments",(20,30.3,-1.75),(26.5,28,-4.14),0)
mesh=bpy.data.meshes.new("Review water plane")
mesh.from_pydata([(x0,-z0,WATER_Y),(x1,-z0,WATER_Y),(x1,-z1,WATER_Y),(x0,-z1,WATER_Y)],[],[(3,2,1,0)])
plane=bpy.data.objects.new("Review water plane",mesh)
scene.collection.objects.link(plane)
mesh.materials.append(material("Review reservoir surface",(.24,.42,.50)))
preview("headpond-terrain-and-compound",(11,57,33),(26,-3,-3.0),96)
west_view=((.6,-20.0,1.3),(10.4,-27.2,-1.48),0)
preview("west-wing-founded",*west_view)
old=Path(meta["previous_construction"]).parent/"dam-with-power.blend"
bpy.ops.wm.open_mainfile(filepath=str(root/old))
scene,camera=setup()
preview("west-wing-before",*west_view)
print("PASS",len(edges),"connected shoreline contour stations and four native structural captures")
