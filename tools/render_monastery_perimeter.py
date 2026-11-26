"""Render actual Monastery geometry with the roofs and old terrain visible."""
import json
from pathlib import Path
import bpy
from mathutils import Vector

ROOT=Path(__file__).resolve().parents[1]
STAGE=ROOT/"development/levels/rebuild/monastery-perimeter-pass"
OUT=STAGE/"captures/structure"
OUT.mkdir(parents=True,exist_ok=True)
def write_plan():
    packet=STAGE/"wld-10"
    receipt=json.loads((packet/"construction.json").read_bytes())
    def xy(x,z):
        return 100+6.5*(x+20),132+6.5*(z+10)
    fig=[
        '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 960 865" role="img" aria-labelledby="title desc">',
        '<title id="title">Monastery of Ashen Bells perimeter</title>',
        '<desc id="desc">Measured estate wall by the western bank and east ravine, with the fixed south carriage gate beyond the van.</desc>',
        '<rect width="960" height="865" fill="#f8f6ee"/>',
        '<g font-family="sans-serif" fill="#243931">',
        '<text x="40" y="56" font-size="31">Monastery of Ashen Bells</text>',
        '<text x="41" y="84" font-size="17">Fitted estate boundary and carriage road</text>',
    ]
    for row in (packet/"runtime-input.tsv").read_text(encoding="utf-8").splitlines():
        f=row.split("\t")
        if f[0] not in {"ROOM","SPACE"} or float(f[7])!=0:
            continue
        x,z=xy(float(f[3]),float(f[4]))
        w,h=(float(f[5])-float(f[3]))*6.5,(float(f[6])-float(f[4]))*6.5
        fill="#e6e9d5" if f[9]=="true" else "#d7d1c3"
        fig.append(f'<rect x="{x:.1f}" y="{z:.1f}" width="{w:.1f}" height="{h:.1f}" fill="{fill}" stroke="#989e8b"/>')
    for span in receipt["boundary"]:
        a,b=xy(*span["start"]),xy(*span["end"])
        color="#a15235" if span["name"].startswith("fixed") else "#3c5550"
        fig.append(f'<path d="M{a[0]:.2f},{a[1]:.2f}L{b[0]:.2f},{b[1]:.2f}" fill="none" stroke="{color}" stroke-width="4" stroke-linecap="round"/>')
    for label,point in (("Archive",(12,9)),("West bank",(-18,26)),("Herb court",(22,36)),
                        ("Bell tower",(49,14)),("East ravine",(79,37)),("Van",(34,80)),
                        ("Fixed carriage gate",(28,99))):
        x,z=xy(*point)
        fig.append(f'<text x="{x:.1f}" y="{z:.1f}" font-size="14">{label}</text>')
    fig.extend(['<path d="M840 136v-34l-5 10m5-10 5 10" fill="none" stroke="#243931" stroke-width="2"/>',
                '<text x="834" y="94" font-size="14">N</text>',
                '<path d="M112 826h65m-65-4v8m65-8v8" fill="none" stroke="#243931" stroke-width="1.5"/>',
                '<text x="184" y="830" font-size="14">10 m</text>','</g></svg>'])
    (packet/"perimeter-plan.svg").write_text("\n".join(fig)+"\n",encoding="utf-8",newline="\n")


write_plan()
bpy.ops.wm.open_mainfile(filepath=str(STAGE/"wld-10/monastery-with-perimeter.blend"))
scene=bpy.context.scene
scene.render.engine="BLENDER_WORKBENCH"
shading=scene.display.shading
shading.light="STUDIO"
shading.color_type="MATERIAL"
shading.show_shadows=True
shading.show_cavity=True
shading.cavity_type="BOTH"
shading.background_type="WORLD"
scene.world=bpy.data.worlds.new("Monastery perimeter survey")
scene.world.color=(.16,.19,.21)
scene.view_settings.view_transform="Standard"
scene.render.resolution_x=1260
scene.render.resolution_y=850
scene.render.resolution_percentage=100
scene.render.image_settings.file_format="PNG"
bpy.ops.object.camera_add()
camera=bpy.context.object
scene.camera=camera

for name,at,target,scale in (
    ("monastery-estate-and-ravine",(103,-134,99),(33,-45,0),142),
    ("monastery-outer-carriage-gate",(24,-85,4),(32,-94.18,1),0),
    ("monastery-east-wall-and-ravine",(68,-30,4),(88,-42,0),0),
    ("monastery-west-bank-and-coping",(-8,-24,5),(-17,-27,2),0),
):
    camera.location=Vector(at)
    camera.rotation_euler=(Vector(target)-camera.location).to_track_quat("-Z","Y").to_euler()
    camera.data.type="ORTHO" if scale else "PERSP"
    camera.data.ortho_scale=scale or 10
    camera.data.lens=37
    camera.data.clip_end=350
    scene.render.filepath=str(OUT/(name+".png"))
    bpy.ops.render.render(write_still=True)
