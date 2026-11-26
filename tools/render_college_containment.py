"""Render native College inspection views and draw its measured boundary plan."""
from html import escape
import json
from pathlib import Path
import sys

ROOT=Path(__file__).resolve().parents[1]
BASE=ROOT/"development/levels/rebuild/college-doors-pass/college"


def draw(stage,destination):
    layout=json.loads((BASE/"authored-layout.json").read_bytes())
    survey=json.loads((stage/"containment.json").read_bytes())
    pts=[tuple(span["a"]) for span in survey["perimeter_spans"]]
    line=lambda pairs:" ".join(f"{x:.2f},{z:.2f}" for x,z in pairs)
    def P(x,z):return 46+6.25*x,117+6.25*z
    def L(x,y,text,size=15,color="#263a34"):
        return f'<text x="{x}" y="{y}" font-size="{size}" fill="{color}">{escape(text)}</text>'
    svg=[
        '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1200 880" role="img" aria-labelledby="title desc">',
        '<title id="title">Beckenburg College perimeter and fall protection</title>',
        '<desc id="desc">Saved terrain, founded parcel fence and gate, the canonical arrival, and the five actual protected stairwells.</desc>',
        '<rect width="1200" height="880" fill="#f5f2e8"/>',
        '<g font-family="Arial, sans-serif">',
        '<text x="32" y="48" font-family="Georgia, serif" font-size="34" fill="#183027">Beckenburg College perimeter and fall protection</text>',
        L(33,77,"Measured parcel plan and deck edge sections",16),
        '<rect x="28" y="102" width="505" height="700" rx="2" fill="#e9ede0" stroke="#adbaa9"/>',
    ]
    for x in range(0,75,5):
        a,b=P(x,0);_,c=P(x,108)
        svg.append(f'<path d="M{a},{b}V{c}" stroke="#d4dbcc" stroke-width=".8"/>')
    for z in range(0,110,5):
        a,b=P(0,z);c,_=P(75,z)
        svg.append(f'<path d="M{a},{b}H{c}" stroke="#d4dbcc" stroke-width=".8"/>')
    zones=layout["zones"]
    for zone in zones:
        if zone["y"]!=0 or zone["kind"] not in ("inside","circulation"):continue
        a,b,c,d=zone["bounds"]
        x,z=P(a,b);xx,zz=P(c,d)
        svg.append(f'<rect x="{x}" y="{z}" width="{xx-x}" height="{zz-z}" fill="#c6c6b8" stroke="#7a8377" stroke-width=".75"/>')
    svg.append(f'<polygon points="{line([P(*p) for p in pts])}" fill="none" stroke="#315748" stroke-width="2.6" stroke-linejoin="round"/>')
    gate=next(span for span in survey["perimeter_spans"] if span["gate"])
    svg.append(f'<polyline points="{line([P(*gate["a"]),P(*gate["b"])])}" fill="none" stroke="#92563e" stroke-width="6"/>')
    x,z=P(32,96)
    svg.append(f'<rect x="{x-22}" y="{z-7}" width="44" height="14" rx="2" fill="#355a73"/>')
    svg.append(L(x-19,z-11,"Van",12))
    path=[P(*p) for p in [(28.372,96),(27,94),(29,87),(32,84),(38,84),(38,78),(38,65),(24,65),(24,62)]]
    svg.append(f'<polyline points="{line(path)}" fill="none" stroke="#4d7584" stroke-width="2.1" stroke-dasharray="5 4"/>')
    for ix,iz,label in [(10,50,"Teaching"),(31,13,"Library"),(52,59,"Residence"),(53,77,"Forecourt")]:
        x,z=P(ix,iz)
        svg.append(L(x,z,label,12))
    for rim in [r for r in survey["well_guard_spans"] if r["name"]=="west rim"]:
        x=(rim["a"][0]+rim["b"][0])/2
        z=(rim["a"][1]+rim["b"][1])/2
        u,v=P(x,z)
        svg.append(f'<circle cx="{u}" cy="{v}" r="5" fill="#fff8e9" stroke="#92563e" stroke-width="1.6"/>')
        svg.append(L(u+7,v-6,rim["well"],11))
    svg += [L(503,125,"N",12),'<path d="M507,152V133l-4,8m4,-8 4,8" stroke="#31483d" fill="none" stroke-width="1.3"/>',
        L(552,139,"Stairwell section",22),L(552,168,"Guards follow the existing rim. The stair head stays open.",15),
        '<rect x="552" y="193" width="611" height="304" rx="4" fill="#fffdf5" stroke="#c2c8b9"/>',
        '<g stroke-linecap="round" stroke-linejoin="round">',
        '<path d="M585 292H700V309H585Z M1009 292H1130V309H1009Z" fill="#bcaa98" stroke="#735840"/>',
        '<path d="M697 292V218m-94 0h94m-94 0v74M625 218v74m24-74v74m24-74v74" fill="none" stroke="#385952" stroke-width="3"/>',
        '<path d="M700 425h22v-13h22v-13h22v-13h22v-13h22v-13h22v-13h22v-13h22v-13h22v-13h22v-13h22v-13h67" fill="none" stroke="#76563f" stroke-width="3"/>',
        '<path d="M701 413L952 267" stroke="#385952" fill="none" stroke-width="2" stroke-dasharray="6 5"/>',
        '<path d="M718 218V292m-5 0h11m-11-74h11" stroke="#95593d" fill="none" stroke-width="1.2"/>',
        '</g>',L(734,258,"1.58 m rim guard crest",14,"#805039"),L(1000,270,"Open stair head",14),
        L(584,464,"Section through a well. The low-end guard stands on the slab above.",14),
        L(552,538,"Surveyed openings",22),
    ]
    rows=[("L80","Teaching tower","+3.60 m"),("L81","Library","+3.60 m"),("L82","North residence tower","+3.60 m"),
          ("L84","West service cellar","0.00 m"),("L85","East service cellar","0.00 m")]
    for i,(mark,label,level) in enumerate(rows):
        y=575+29*i
        svg.append(f'<path d="M552 {y+8}h611" stroke="#d2d5c7"/>')
        svg.extend([L(553,y,mark,14),L(640,y,label,15),L(1076,y,level,14)])
    svg += [
        '<path d="M553 745h28" stroke="#315748" stroke-width="3"/>',L(590,751,"Founded fence and stone plinth",14),
        '<path d="M870 745h25" stroke="#92563e" stroke-width="6"/>',L(904,751,"Closed service gate in an 8 m bay",14),
        '<path d="M553 779h28" stroke="#4d7584" stroke-width="2" stroke-dasharray="5 4"/>',L(590,784,"Walking guidance to the actual lobby door",14),
        L(33,831,"Plan scale",12),'<path d="M106 827h62.5m-62.5-3v7m62.5-7v7" stroke="#315748"/>', L(176,831,"10 m",12),
        '</g></svg>',
    ]
    destination.write_text("\n".join(svg)+"\n",encoding="utf-8",newline="\n")


def render(stage,output):
    import bpy
    from mathutils import Vector
    bpy.ops.wm.open_mainfile(filepath=str((stage/"college-containment.blend").resolve()))
    scene=bpy.context.scene
    scene.render.engine="BLENDER_WORKBENCH"
    shading=scene.display.shading
    shading.light="STUDIO"
    shading.color_type="MATERIAL"
    shading.show_shadows=True
    shading.show_cavity=True
    shading.cavity_type="BOTH"
    shading.background_type="WORLD"
    scene.world=bpy.data.worlds.new('College site inspection')
    scene.world.color=(.10,.12,.13)
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

    shot("native-parcel", (102,-152,130), (36,-57,0),160)
    shot("native-gate-piers", (40,-94,7.7), (32,-103,.9),17)
    # The near walls, ceiling and roof are hidden for this cutaway only.
    # The flight, floor edges and guards remain in place.
    for obj in scene.objects:
        if obj.type!="MESH":continue
        coords=[obj.matrix_world@v.co for v in obj.data.vertices]
        role=obj.get("role","")
        if not coords:continue
        x0=min(p.x for p in coords)
        z0,z1=min(-p.y for p in coords),max(-p.y for p in coords)
        inside=62-.3<=x0<68.6 and z0<42.3 and z1>29.7
        occluder=("roof" in role or "ceiling" in role or ("wall" in role and x0>=61.8))
        if inside and occluder:obj.hide_render=True
    shot("native-tower-cutaway",(73,-46,11), (65,-35,2.5),16)


if __name__=="__main__":
    args=sys.argv[sys.argv.index("--")+1:] if "--" in sys.argv else sys.argv[1:]
    stage,output=(Path(p) for p in args[:2])
    output.mkdir(parents=True,exist_ok=True)
    draw(stage,output/"college-containment-plan.svg")
    if "--render" in args:
        render(stage,output)
