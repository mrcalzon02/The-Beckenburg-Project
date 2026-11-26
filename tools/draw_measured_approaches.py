"""Draw measured plan details with guidance on a separate dashed layer."""
from xml.sax.saxutils import escape

from build_measured_approaches import OUT, ROOT, read
from verify_interior_stair_landings import svg_line, svg_rect, svg_text


def detail(layout, contract):
    title = layout["title"]
    site = layout["id"]
    by = {l["id"]:l["points"] for l in layout["links"]}
    doors = {p["id"]:p for p in layout["authored_doorways"]}
    body = [svg_text(36,36,title,"title")]

    def panel(origin, datum, scale):
        ox,oz = origin
        dx,dz = datum

        def xy(x,z):
            return ox+(x-dx)*scale,oz+(z-dz)*scale

        def surface(box, style):
            a,b,c,d = box
            x,z = xy(a,b)
            body.append(svg_rect(x,z,(c-a)*scale,(d-b)*scale,style))

        def guide(points):
            body.append(svg_line([xy(x,z) for x,y,z in points],"guide"))

        def aperture(ident):
            door = doors[ident]
            c,p,w = (door[field] for field in ("along","boundary","clear_width"))
            ends = [(c-w/2,p),(c+w/2,p)] if door["axis"]=="z" else [(p,c-w/2),(p,c+w/2)]
            body.append(svg_line([xy(*point) for point in ends],"door"))

        return xy,surface,guide,aperture

    if site == "FARMHOUSE":
        body.append(svg_text(36,70,"Ground stair spine and barn loft approach · north up"))
        xy,surface,guide,aperture = panel((105,117),(20,12),29)
        surface((20,12,26,22),"floor")
        surface((20.55,13.35,22.45,18.4),"well")
        surface((23.75,13.6,25.25,18.4),"flight")
        for suffix in (1,2,3):
            aperture(f"FARMHOUSE bay indexed door {suffix}")
        for key in ("L05","L08","L12","L34"):
            guide([p for p in by[key] if p[1]==0 and 20<=p[0]<=26 and 12<=p[2]<=22])
        body.append(svg_text(108,429,"Main transfer · 1.16 m between guards","small"))
        xy,surface,guide,aperture = panel((454,117),(39,16),29)
        surface((39,16,43,24),"floor")
        surface((40.25,17.5,41.75,22.3),"flight")
        aperture("FARMHOUSE bay indexed door 6")
        guide([p for p in by["L28"] if p[0]>=38]+[p for p in by["L36"][1:] if p[1]==0])
        body.append(svg_text(454,367,"Loft approach · 1.09 m between wall and guard","small"))

    elif site == "WLD-08":
        body.append(svg_text(36,70,"Generator room and radio-room connection · north up"))
        xy,surface,guide,aperture = panel((70,110),(2,8),17)
        surface((2,8,20,26),"floor")
        surface((20,12,32,26),"floor")
        surface((9.9,17,12.1,22.3),"well")
        body.append(svg_line([xy(12.225,17),xy(12.225,22.425)],"guard"))
        aperture("WLD-08-x-20-19")
        guide(by["L12"])
        body.append(svg_text(100,137,"Generator room","small"))
        body.append(svg_text(452,216,"Radio room","small"))
        body.append(svg_text(257,234,"Turn on full floor north of well","small"))
        body.append(svg_text(399,317,"1.05 m shared doorway","small"))

    else:
        body.append(svg_text(36,70,"Canonical van, existing forecourt and foyer sill · north up"))
        xy,surface,guide,aperture = panel((68,105),(1,30),10)
        outline = contract["earthworks"][0]["polygon"]
        body.append('<polygon class="gravel" points="'+" ".join(f"{x:.2f},{y:.2f}" for x,y in
                     (xy(*point) for point in outline))+'"/>')
        surface((5,36,55,42),"floor")
        surface((20,36,25,39),"flight")
        surface((20,30,25,36),"floor")
        surface((25.872,49.9705,32.396,52.0295),"vehicle")
        surface((24.372,50.34,25.872,51.66),"vehicle")
        guide(by["L42"]+list(reversed(by["L35"][:-1])))
        aperture("SCHOOL-z-36-22.5")
        body.append(svg_text(354,127,"Foyer sill · 1.80 m clear","small"))
        body.append(svg_text(352,201,"Paved forecourt","small"))
        body.append(svg_text(432,339,"Canonical van and rear ramp","small"))
        body.append(svg_text(125,365,"Saved gravel yard","small"))
    body.extend([
        svg_line([(45,467),(75,467)],"guide"),svg_text(84,472,"Nonphysical circulation guide","small"),
        svg_line([(295,467),(328,467)],"door"),svg_text(338,472,"Measured doorway","small"),
        svg_rect(490,458,15,13,"floor"),svg_text(515,471,"Floor or paving","small"),
        svg_rect(641,458,15,13,"well"),svg_text(666,471,"Stair well","small"),
    ])
    return f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 840 496" role="img">
<title>{escape(title)} measured approach plan</title>
<style>
text{{fill:#253847;font:16px "Segoe UI",sans-serif}} .title{{font-size:26px;font-weight:700}} .small{{font-size:13px}}
.floor{{fill:#dae7ed;stroke:#426073;stroke-width:1.7}} .well{{fill:#f2e5d9;stroke:#8a6243;stroke-width:1.6}}
.flight{{fill:#dac7a7;stroke:#896641;stroke-width:1.6}} .vehicle{{fill:#a5aead;stroke:#425758;stroke-width:1.5}}
.gravel{{fill:#e3e5d9;stroke:#7c8572;stroke-width:1.5}}
.door,.guide,.guard{{fill:none}} .door{{stroke:#0b786a;stroke-width:4.5}}
.guide{{stroke:#b34f20;stroke-width:2.5;stroke-dasharray:5 4}} .guard{{stroke:#405466;stroke-width:4}}
</style>
<rect width="840" height="496" fill="#f9faf8"/>
{''.join(body)}
</svg>
'''


if __name__ == "__main__":
    for row in read(OUT/"roster.json")["active_sources"]:
        if row["id"] in {"FARMHOUSE","WLD-08","SCHOOL"}:
            folder = OUT/row["id"].lower()
            drawing = detail(read(ROOT/row["layout"]),read(ROOT/row["construction"]))
            (folder/"measured-plan.svg").write_bytes(drawing.encode())
