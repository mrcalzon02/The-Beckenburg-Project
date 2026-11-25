"""Render the fitted hospital enclosure and draw its measured plan."""
from html import escape
import json
from pathlib import Path

import bpy
from mathutils import Vector

ROOT = Path(__file__).resolve().parents[1]
STAGE = ROOT/"development/levels/rebuild/asylum-enclosure-pass"
CAPTURES = STAGE/"captures/structure"


def draw_plan():
    design = json.loads((STAGE/"construction.json").read_bytes())
    assert design["gate_width"] == 8
    def plot(x,z):
        return 140+7*(x+12),135+7*(z+9)
    def label(text,x,z):
        a,b=plot(x,z)
        return f'<text x="{a:.1f}" y="{b:.1f}" font-size="14">{escape(text)}</text>'
    svg=[
        '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 960 920" role="img" aria-labelledby="title desc">',
        '<title id="title">St. Dymphna Hospital perimeter</title>',
        '<desc id="desc">Physical estate wall and closed carriage gate, with the original hospital, patient court and van shown for context.</desc>',
        '<rect width="960" height="920" fill="#faf7f1"/>',
        '<g font-family="sans-serif" fill="#274038">',
        '<text x="39" y="57" font-size="31">St. Dymphna Hospital</text>',
        '<text x="41" y="88" font-size="18">Surveyed estate boundary and carriage approach</text>',
    ]
    for line in (STAGE/"arrival.tsv").read_text(encoding="utf-8").splitlines():
        row=line.split("\t")
        if row[0]!="SPACE" or float(row[7])!=0 or row[9]=="true":
            continue
        x,z=plot(float(row[3]),float(row[4]))
        w,h=(float(row[5])-float(row[3]))*7,(float(row[6])-float(row[4]))*7
        shade="#ddd2c1" if row[10]=="true" else "#e8e3d6"
        svg.append(f'<rect x="{x:.1f}" y="{z:.1f}" width="{w:.1f}" height="{h:.1f}" fill="{shade}" stroke="#988e7c"/>')
    for line in (STAGE/"boundary-survey.tsv").read_text(encoding="utf-8").splitlines()[1:]:
        span,*data=line.split("\t")
        a,b=plot(*map(float,data[:2])),plot(*map(float,data[2:4]))
        color="#a8543f" if "carriage" in span else "#35554b"
        svg.append(f'<path d="M{a[0]:.2f},{a[1]:.2f}L{b[0]:.2f},{b[1]:.2f}" stroke="{color}" fill="none" stroke-width="4" stroke-linecap="round"/>')
    svg.extend(label(*row) for row in [
        ("Retaining wall",16,-8),("Patient court",10,35),("Admissions",29,57),
        ("Service wall",72,31),("Van",34,76),("Fixed carriage gate",27,99)])
    svg.extend(['<path d="M860 146v-32l-5 10m5-10 5 10" fill="none" stroke="#274038" stroke-width="2"/>',
                '<text x="853" y="105" font-size="14">N</text>',
                '<path d="M45 864h70m-70-4v8m70-8v8" stroke="#274038" stroke-width="1.5"/>',
                '<text x="128" y="869" font-size="14">10 m</text>','</g></svg>'])
    (STAGE/"enclosure-plan.svg").write_text("\n".join(svg)+"\n",encoding="utf-8",newline="\n")


def render():
    CAPTURES.mkdir(parents=True,exist_ok=True)
    bpy.ops.wm.open_mainfile(filepath=str(STAGE/"asylum-stair.blend"))
    scene=bpy.context.scene
    scene.render.engine="BLENDER_WORKBENCH"
    screen=scene.display.shading
    screen.light="STUDIO"
    screen.color_type="MATERIAL"
    screen.show_shadows=True
    screen.show_cavity=True
    screen.cavity_type="BOTH"
    screen.background_type="WORLD"
    scene.world=bpy.data.worlds.new("Asylum exterior survey")
    scene.world.color=(.21,.23,.24)
    scene.view_settings.view_transform="Standard"
    scene.render.resolution_x=1260
    scene.render.resolution_y=850
    scene.render.resolution_percentage=100
    scene.render.image_settings.file_format="PNG"
    bpy.ops.object.camera_add()
    camera=bpy.context.object
    scene.camera=camera
    for name,eye,target,ortho in (
        ("asylum-estate-and-road", (105,-134,107),(31,-43,0),148),
        ("asylum-carriage-gate-and-piers", (24,-85,4),(32,-92.4,.95),0),
        ("asylum-east-service-wall", (65,-39,3.1),(70,-25,1.2),0),
    ):
        camera.location=Vector(eye)
        camera.rotation_euler=(Vector(target)-camera.location).to_track_quat("-Z","Y").to_euler()
        camera.data.type="ORTHO" if ortho else "PERSP"
        camera.data.ortho_scale=ortho or 10
        camera.data.lens=38
        camera.data.clip_end=350
        scene.render.filepath=str(CAPTURES/(name+".png"))
        bpy.ops.render.render(write_still=True)


if __name__=="__main__":
    draw_plan()
    render()
