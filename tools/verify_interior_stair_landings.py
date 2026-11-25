"""Native joined-walk review and dimensioned drawings of the two stair bays."""
import csv
import hashlib
import json
import os
from pathlib import Path
import subprocess
from xml.sax.saxutils import escape

ROOT = Path(__file__).resolve().parents[1]
PASS = ROOT / "development/levels/rebuild/interior-stair-landing-pass"
VERIFY = PASS / "verification"
JOURNEYS = {
    "MANSION": [
        ("Van ramp to the great-stair guest wing", "L01 L02 L05 L06 L58 L36 L37"),
        ("Cellar passage to the attic archive", "-L54 L59 L60 L61 L51 L52"),
        ("Attic passage to the garden through the service stair", "-L51 -L61 -L60 L28"),
        ("Ground service to the upper staff rooms", "L23 L60 -L50 L43 L44 L46"),
    ],
    "SCHOOL": [
        ("Plant room to the north grounds", "-L33 L44 L36"),
        ("Forecourt to the plant room", "-L35 L04 L09 L08 -L44 L33"),
        ("Dining room to the upper lobby", "-L14 L43 L45"),
        ("North grounds to the upper corridor", "-L36 -L08 L43 -L27"),
    ],
}


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def write_json(path, value):
    path.write_bytes((json.dumps(value, indent=2) + "\n").encode())


def svg_text(x, y, content, style=""):
    return f'<text x="{x}" y="{y}" class="{style}">{escape(content)}</text>'


def svg_rect(x, y, w, h, style):
    return f'<rect x="{x:.3f}" y="{y:.3f}" width="{w:.3f}" height="{h:.3f}" class="{style}"/>'


def svg_line(points, style):
    coords = " ".join(f"{a:.3f},{b:.3f}" for a,b in points)
    return f'<polyline points="{coords}" class="{style}"/>'


def plan(box, holes, doors, route, origin, scale, title):
    a,b,c,d = box
    left,top = origin

    def xy(x,z):
        return left+(x-a)*scale,top+(z-b)*scale

    lines = [svg_rect(left,top,(c-a)*scale,(d-b)*scale,"floor"),svg_text(left,top-19,title,"heading")]
    for x,z,xx,zz in holes:
        px,pz = xy(x,z)
        lines.append(svg_rect(px,pz,(xx-x)*scale,(zz-z)*scale,"well"))
    for door in doors:
        assert door["axis"] == "z"
        x,z,w = (door[field] for field in ("along","boundary","clear_width"))
        lines.append(svg_line([xy(x-w/2,z),xy(x+w/2,z)],"door"))
    lines.append(svg_line([xy(x,z) for x,y,z in route],"guide"))
    return lines


def stair_section(floor, hole, flight, origin, scale, title):
    _,b,_,d = floor
    left,baseline = origin

    def xy(z,y):
        return left+(z-b)*scale,baseline-y*scale

    z0,z1,lo,hi,count = flight
    lines = [svg_text(left,baseline-1.55*scale,title,"heading")]
    for z,zz in ((b,hole[1]),(hole[3],d)):
        x,y = xy(z,0)
        lines.append(svg_rect(x,y,(zz-z)*scale,.30*scale,"floor"))
    x,y = xy(b,-3)
    lines.append(svg_rect(x,y,(d-b)*scale,.30*scale,"floor"))
    steps = [xy(z0,lo)]
    for i in range(count):
        steps.append(xy(z0+(z1-z0)*i/count,lo+(hi-lo)*(i+1)/count))
        steps.append(xy(z0+(z1-z0)*(i+1)/count,lo+(hi-lo)*(i+1)/count))
    lines.append(svg_line(steps,"treads"))
    lines.append(svg_text(left-40,baseline+4,"0 m","small"))
    lines.append(svg_text(left-40,baseline+3*scale+4,"−3 m","small"))
    return lines


def worksheet(site, out, layout, meta):
    boxes = layout["interior_stair_landings"]
    doors = {p["id"]: p for p in layout["authored_doorways"]}
    links = {l["id"]: l["points"] for l in layout["links"]}
    structure = []
    if site == "MANSION":
        title = "Blackthorn Manor · service wing floor plans"
        note = "South doors share a 1.35 m clear opening. Ground and first-floor cross-bay aisles are 1.16 m clear."
        by_level = {d["sill_y"]:d for d in doors.values() if d["id"] in boxes["service_door_ids"]}
        for i,(datum,caption,link) in enumerate((
            (-3,"−3.00 m · cellar","L54"), (0,"0.00 m · ground","L23"),
            (3.6,"+3.60 m · first floor","L50"), (7.2,"+7.20 m · attic","L51"),
        )):
            footprint,_,holes = next(row for row in meta["architectural_plan"]["floors"]
                                     if row[1]==datum and row[0][0]==40)
            route = [p for p in links[link] if footprint[1]<=p[2]<=footprint[3]]
            controls = [by_level[datum]]
            if datum == 0:
                controls.append(doors["service-stair n exterior doorway"])
            start = 56+i*235
            structure.extend(plan(footprint, holes, controls, route, (start,150), 23, caption))
            structure.append(svg_text(start,456,"South · fitted service door","small"))
        height = 504
    else:
        title = "Ravenshade School · service stair"
        note = "A 300 mm wall-supported deck surrounds the well. The guarded east aisle is 1.02 m clear."
        floor,well = boxes["service_ground_floor"],boxes["service_ground_well"]
        controls = [doors["SCHOOL-cell-service-stair-N-29.1"],doors["SCHOOL-cell-main-stair-N-28.6"]]
        route = [p for p in links["L36"] if floor[1]<=p[2]<=floor[3]]
        structure.extend(plan(floor,[well],controls,route,(76,144),36,"Ground plan · north up"))
        structure.append(svg_line([(76+(28.70-floor[0])*36,144+(13.22-floor[1])*36),
                                   (76+(28.70-floor[0])*36,144+(18.25-floor[1])*36)],"guard"))
        structure.append(svg_text(76,456,"South · main stair hall","small"))
        structure.append(svg_text(207,295,"Guarded east aisle","small"))
        flight = next(f for f in layout["structural_stair_cells"]["flights"] if f["link"]=="L44")
        section = tuple(flight[k] for k in ("z0","z1","lower_y","upper_y","risers"))
        structure.extend(stair_section(floor, well, section, (450,258), 49,
                                       "Longitudinal section · 17 risers"))
        structure.append(svg_text(442,448,"Ground slab cut shown at the stair centre. Actual north door sits east of the flight.","small"))
        height = 504
    style = """
      text{fill:#203649;font:16px 'Segoe UI',sans-serif}.title{font-size:27px;font-weight:700}
      .heading{font-weight:700}.small{font-size:13px} .floor{fill:#dfebef;stroke:#344d60;stroke-width:2}
      .well{fill:url(#hatch);stroke:#6c4c34;stroke-width:2}.door,.guide,.guard,.treads{fill:none}
      .door{stroke:#0e7568;stroke-width:5}.guide{stroke:#be591f;stroke-width:2.3;stroke-dasharray:5 4}
      .guard{stroke:#435364;stroke-width:4}.treads{stroke:#405466;stroke-width:2.4}
    """
    legend = [
        svg_rect(56,height-31,15,13,"floor"),svg_text(80,height-20,"Floor / slab","small"),
        svg_rect(225,height-31,15,13,"well"),svg_text(250,height-20,"Stair well","small"),
        svg_line([(372,height-24),(394,height-24)],"door"),svg_text(404,height-20,"Door aperture","small"),
        svg_line([(552,height-24),(579,height-24)],"guide"),svg_text(590,height-20,"Nonphysical circulation guide","small"),
    ]
    body = "".join([svg_text(55,44,title,"title"), svg_text(55,76,note)] + structure + legend)
    document = (
        f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1000 {height}" role="img">'
        f'<title>{escape(title)}</title><style>{style}</style><defs>'
        '<pattern id="hatch" width="8" height="8" patternUnits="userSpaceOnUse">'
        '<rect width="8" height="8" fill="#f5eade"/><path d="M0,8 L8,0" stroke="#c7aa91"/></pattern>'
        f'</defs><rect width="1000" height="{height}" fill="#f9faf8"/>{body}</svg>\n'
    )
    name = "service-wing-plans.svg" if site == "MANSION" else "plan-and-section.svg"
    (out/name).write_bytes(document.encode())


def verify():
    roster = json.loads((PASS/"roster.json").read_bytes())
    VERIFY.mkdir(parents=True, exist_ok=True)
    paths, components, labels, bindings = [], [], {}, {}
    for row in roster["active_sources"]:
        site = row["id"]
        if site not in JOURNEYS:
            continue
        layout = json.loads((ROOT/row["layout"]).read_bytes())
        meta = json.loads((ROOT/row["construction"]).read_bytes())
        native = ROOT/row["native"]
        digest = sha(native)
        assert digest==row["native_sha256"]==meta["physics_sha256"]
        bindings[site] = row
        worksheet(site, PASS/site.lower(), layout, meta)
        links = {l["id"]:l["points"] for l in layout["links"]}
        components.extend(f"{site}:after\t{name}" for name in meta["stair_components"])
        for index,(name,encoded) in enumerate(JOURNEYS[site],1):
            chain = encoded.split()
            joined = []
            for key in chain:
                points = links[key] if key[0]!="-" else list(reversed(links[key[1:]]))
                assert not joined or joined[-1]==points[0], (site,name,key,joined[-1],points[0])
                joined.extend(points[1:] if joined else points)
            ident = f"J{index:02}"
            labels[site,ident] = dict(name=name, links=chain)
            for i,(a,b) in enumerate(zip(joined,joined[1:])):
                paths.append("\t".join(map(str,(site,"after",native,digest,ident,i,*a,*b))))

    java = Path("C:/Program Files/Java/jdk-17/bin")
    jar = ROOT/"build/beckenburg-game.jar"
    java_build = ROOT/"build/interior-stair-journeys"
    sources = [ROOT/"src/main/java/org/beckenburg/AuthoredSitePhysics.java",
               ROOT/"tools/java/RosterStairMovementProbe.java"]
    subprocess.run([str(java/"javac.exe"),"-cp",str(jar),"-d",str(java_build),*map(str,sources)],check=True)
    inputs,result = VERIFY/"complete-journey-input.tsv",VERIFY/"complete-journey-sweeps.tsv"
    parts,empty = java_build/"components.tsv",java_build/"empty-flights.tsv"
    parts.write_bytes(("\n".join(components)+"\n").encode())
    inputs.write_bytes(("\n".join(paths)+"\n").encode())
    empty.write_bytes(b"")
    subprocess.run([str(java/"java.exe"),"-Djava.awt.headless=true","-Xmx2g","-cp",
                    os.pathsep.join(map(str,(java_build,jar))),"RosterStairMovementProbe",str(parts),
                    str(empty),str(java_build/"unused.tsv"),str(result),str(inputs)],check=True)
    with result.open(newline="") as stream:
        rows = [dict(row,**labels[row["site"],row["link"]]) for row in csv.DictReader(stream,delimiter="\t")]
    assert len(rows)==len(labels) and all(row["authored_forward"]==row["authored_reverse"]=="PASS" for row in rows),rows
    write_json(VERIFY/"complete-journeys.json",dict(
        sources=bindings,journeys=rows,
        source_sha256={p.relative_to(ROOT).as_posix():sha(p) for p in [*sources,Path(__file__)]},
        evidence_sha256={p.name:sha(p) for p in (inputs,result)},
    ))
    print("JOINED STAIR AND DOOR WALKS",len(rows))


if __name__ == "__main__":
    verify()
