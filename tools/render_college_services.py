"""Draw the measured College utility installation and render inspection cutaways."""
from html import escape
import json
from pathlib import Path
import sys

ROOT=Path(__file__).resolve().parents[1]
PRIOR=ROOT/"development/levels/rebuild/college-containment-pass/college"


def draw(stage,out):
    source=json.loads((stage/"services.json").read_bytes())
    layout=json.loads((PRIOR/"authored-layout.json").read_bytes())
    images=[
        '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1240 930" role="img" aria-labelledby="title desc">',
        '<title id="title">College heating, plumbing and ventilation</title>',
        '<desc id="desc">Native College service installations by storey and separate service connections.</desc>',
        '<rect width="1240" height="930" fill="#f2f0e8"/>',
        '<g font-family="Arial, sans-serif" fill="#223a35">',
        '<text x="39" y="57" font-family="Georgia, serif" font-size="35">College heating, plumbing and ventilation</text>',
        '<text x="41" y="87" font-size="15">Seated fittings, plant access and measured utility routes</text>',
    ]

    def label(x,y,text,size=14,color="#223a35"):
        images.append(f'<text x="{x}" y="{y}" font-size="{size}" fill="{color}">{escape(text)}</text>')

    colors={"plant":"#775e8f","heat":"#a4543e","wet":"#337789","vent":"#b27826"}
    for i,(datum,caption) in enumerate(((-3,"Basement  −3.00 m"),(0,"Ground floor  0.00 m"),(3.6,"Upper floor  +3.60 m"))):
        ox=37+400*i
        label(ox,132,caption,19)
        images.append(f'<rect x="{ox}" y="149" width="367" height="452" fill="#faf9f0" stroke="#a9b5ab"/>')
        def xy(x,z):
            return ox+38+4.08*x,155+4.01*z
        for zone in layout["zones"]:
            if zone["y"]!=datum or zone["kind"] in ("outside","staging"):continue
            a,b,c,d=zone["bounds"]
            x,z=xy(a,b); xx,zz=xy(c,d)
            images.append(f'<rect x="{x:.2f}" y="{z:.2f}" width="{xx-x:.2f}" height="{zz-z:.2f}" fill="#dedfd4" stroke="#8e9e90" stroke-width=".85"/>')
        if datum==-3:
            for run in source["service_runs"]:
                if "campus carrier" not in run["id"] and run["id"]!="south gravity collector":continue
                pairs=" ".join(f"{x:.2f},{z:.2f}" for x,z in [xy(p[0],p[2]) for p in run["nodes"]])
                color=colors["heat"] if run["circuit"] in ("flow","return") else colors["wet"]
                images.append(f'<polyline points="{pairs}" fill="none" stroke="{color}" stroke-width="1.3" stroke-dasharray="4 3"/>')
            x,z=xy(16.38,24)
            images.append(f'<path d="M{x-3.5} {z}h7" stroke="#775e8f" stroke-width="4"/>')
            label(x-42,z+19,"Staff door",11)
            label(ox+41,577,"Dashed lines show buried carriers below the cellar floor.",11)
        for row in source["fixtures"]:
            if row["floor"]!=datum:continue
            a,b=row["size"]
            x,z=xy((a[0]+b[0])/2,(a[2]+b[2])/2)
            key=row["key"]
            kind="plant" if key.startswith("west-plant") else "heat" if "radiator" in key else ("vent" if "extract" in key or "range" in key else "wet")
            images.append(f'<circle cx="{x:.2f}" cy="{z:.2f}" r="3.8" fill="{colors[kind]}" stroke="#fff9eb" stroke-width=".55"><title>{escape(key.replace("-"," "))}</title></circle>')
        for vent in source["ventilation"]:
            x,y,z=vent["pickup"]
            if (y<0 and datum!=-3) or (0<=y<3.6 and datum!=0) or (y>=3.6 and datum!=3.6):continue
            a,b=xy(x,z);c,d=xy(*vent["shaft"])
            images.append(f'<path d="M{a:.2f} {b:.2f} L{c:.2f} {d:.2f}" stroke="{colors["vent"]}" stroke-width="2.6"/>')
    for x,key,t in ((43,"plant","Plant fittings"),(234,"heat","Heat emitters"),(425,"wet","Wet fixtures"),(610,"vent","Air pickup and riser")):
        images.append(f'<circle cx="{x}" cy="636" r="5" fill="{colors[key]}"/>')
        label(x+13,641,t)
    images.append('<path d="M39 662H1200" stroke="#b7c0b6"/>')
    label(42,700,"Service connections",22)
    chains=[
        ("Heating",("Plant boiler","Isolation manifold","Teaching, library and residence radiators"),"heat"),
        ("Domestic water",("Street stop","Cold carrier and boiler","Washrooms, kitchen and laundry"),"wet"),
        ("Sanitary",("Fixture drains","South-falling collector","Inspection chamber beyond the gate"),"wet"),
        ("Ventilation",("Room pickup","Supported air duct","Roof curb and weather cap"),"vent"),
    ]
    for j,(system,chain,theme) in enumerate(chains):
        y=738+j*40
        label(43,y+4,system,14)
        for k,step in enumerate(chain):
            x=(180,414,700)[k]
            width=(193,247,420)[k]
            images.append(f'<rect x="{x}" y="{y-18}" width="{width}" height="29" rx="3" fill="#f8f6ec" stroke="{colors[theme]}"/>')
            label(x+13,y+2,step,13)
            if k<2:
                u=x+width+7
                images.append(f'<path d="M{u} {y-3}h24m-5 -4 5 4-5 4" stroke="{colors[theme]}" fill="none"/>')
    images.append('</g></svg>')
    out.write_text("\n".join(images)+"\n",encoding="utf-8",newline="\n")


def native_render(stage,output):
    import bpy
    from mathutils import Vector
    from build_college_doors import aabb

    bpy.ops.wm.open_mainfile(filepath=str((stage/"college-services.blend").resolve()))
    scene=bpy.context.scene
    scene.render.engine="BLENDER_WORKBENCH"
    shading=scene.display.shading
    shading.light="STUDIO"
    shading.color_type="MATERIAL"
    shading.show_shadows=True
    shading.show_cavity=True
    shading.cavity_type="BOTH"
    shading.background_type="WORLD"
    scene.world=bpy.data.worlds.new("College service review")
    scene.world.color=(.11,.13,.14)
    scene.view_settings.view_transform="Standard"
    scene.render.resolution_x=1280
    scene.render.resolution_y=910
    scene.render.resolution_percentage=100
    scene.render.image_settings.file_format="PNG"
    bpy.ops.object.camera_add()
    camera=bpy.context.object
    camera.data.type="ORTHO"
    scene.camera=camera

    def shot(name,position,target,extent):
        camera.location=Vector(position)
        camera.rotation_euler=(Vector(target)-camera.location).to_track_quat("-Z","Y").to_euler()
        camera.data.ortho_scale=extent
        scene.render.filepath=str((output/(name+".png")).resolve())
        bpy.ops.render.render(write_still=True)

    shot("native-roof-terminations",(82,-98,78),(35,-33,3),98)
    for obj in scene.objects:
        if obj.type!="MESH":continue
        x0,y0,z0,x1,y1,z1=aabb(obj)
        if x1<11.7 or x0>20.25 or z0>34.20 or z1<9.70 or y0>=-.3:
            obj.hide_render=True
        if "earthworks" in obj.name or "College district apron" in obj.name:
            obj.hide_render=True
        if obj.name=="Wall plant E" or obj.name=="Wall maintenance S":
            obj.hide_render=True
    shot("native-plant-and-maintenance-cutaway",(27,-34,10),(16,-21.5,-1.7),32)


if __name__=="__main__":
    args=sys.argv[sys.argv.index("--")+1:] if "--" in sys.argv else sys.argv[1:]
    stage,out=(Path(p) for p in args[:2])
    out.mkdir(parents=True,exist_ok=True)
    draw(stage,out/"college-services-plan.svg")
    if "--render" in args:
        sys.path.insert(0,str(ROOT/"tools"))
        native_render(stage,out)
