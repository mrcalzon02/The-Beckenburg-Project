"""Draw the Dam's reconstructed east hall and test joined service journeys."""
import csv
from html import escape
import json
from pathlib import Path
import subprocess

from prepare_cemetery_grounds import ROOT, BASE, sha

PASS = BASE/"dam-east-hall-pass"
OUT = PASS/"wld-09"
VERIFY = PASS/"verification"


def draw(layout):
    features = layout["east_hall_bearings"]
    links = {row["id"]:row for row in layout["links"]}
    rooms = {row["id"]:row for row in layout["zones"]}
    scale = 11.0

    def ground(x,z):
        return 63+(x-35)*scale,161+(z-21)*scale

    def section(x,y):
        return 669+(x-63.6)*45,227-y*37.5

    def points(nodes,project):
        return " ".join(f"{x:.2f},{y:.2f}" for x,y in (project(*p) for p in nodes))

    def poly(nodes,project,fill,stroke):
        return f'<polygon points="{points(nodes,project)}" fill="{fill}" stroke="{stroke}" stroke-width="1"/>'

    def rect(bounds,project,fill,stroke):
        a,b,c,d = bounds
        return poly(((a,b),(c,b),(c,d),(a,d)),project,fill,stroke)

    def line(nodes,project,color,dash=None,width=2.2):
        style = f' stroke-dasharray="{dash}"' if dash else ""
        return f'<polyline points="{points(nodes,project)}" fill="none" stroke="{color}" stroke-width="{width}"{style}/>'

    def text(x,y,s,style="body",anchor="start"):
        return f'<text x="{x:.2f}" y="{y:.2f}" class="{style}" text-anchor="{anchor}">{escape(str(s))}</text>'

    svg = [
        '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1160 844" role="img" aria-label="Blackwater Hydroelectric Dam east stair and lower service hall">',
        '<rect width="1160" height="844" fill="#f8f3e8"/>',
        '<style>text{font-family:Segoe UI,Arial,sans-serif;fill:#263a37}.title{font-size:34px;font-weight:650}'
        '.head{font-size:18px;font-weight:650}.body{font-size:14px}.small{font-size:12px}'
        '.room{font-size:12px;font-weight:650;paint-order:stroke;stroke:#f8f3e8;stroke-width:2.5}</style>',
        text(37,61,"Blackwater Hydroelectric Dam","title"),
        text(37,88,"East stair structure and service access"),
        '<rect x="37" y="111" width="479" height="681" rx="8" fill="#e9e4d8"/>',
        '<rect x="535" y="111" width="587" height="681" rx="8" fill="#eee9de"/>',
        text(57,139,"Service floor at −6.0 m","head"),
        text(495,139,"North ↑","small","end"),
        text(556,139,"East hall section at Z 34.34 m","head"),
    ]
    room_names = ("turbine","workshop","east-lower-stair","spillway")
    for key in room_names:
        svg.append(rect(rooms[key]["bounds"],ground,"#f8f1e2","#655f54"))
    svg.append(rect(features["well"],ground,"#d7d1c9","#8d8880"))
    mid = features["middle_landing"]
    svg.append(rect(mid,ground,"#beb8af","#777063"))
    for key in ("L08","L09","L12","L18"):
        ps = links[key]["points"]
        for a,b in zip(ps,ps[1:]):
            if a[1]==b[1]==-6:
                svg.append(line(((a[0],a[2]),(b[0],b[2])),ground,"#216f79","5 4"))
    for door in layout["authored_doorways"]:
        if door["sill_y"]!=-6:
            continue
        fixed,along,width = (door[k] for k in ("boundary","along","clear_width"))
        low,high = along-width/2,along+width/2
        shape = ((fixed,low),(fixed,high)) if door["axis"]=="x" else ((low,fixed),(high,fixed))
        svg.append(line(shape,ground,"#a64f2d",width=3.5))
    for key,label in (("turbine","Turbine hall"),("workshop","Workshop"),("spillway","Observation gallery")):
        a,b,c,d = rooms[key]["bounds"]
        x,y = ground((a+c)/2,(b+d)/2)
        svg.append(text(x,y-6,label,"room","middle"))
    gx,gy = ground(68.8,40)
    svg.append(text(gx-30,gy+12,"Service aisle","room","end"))
    svg.extend([
        '<path d="M 58 637 H 113 M 58 632 v 10 M 113 632 v 10" stroke="#263a37"/>',
        text(58,657,"0","small"),text(113,657,"5 m","small","end"),
        '<path d="M 58 686 H 92" stroke="#a64f2d" stroke-width="3"/>',text(105,691,"Fitted door opening"),
        '<path d="M 58 719 H 92" stroke="#216f79" stroke-width="2.2" stroke-dasharray="5 4"/>',
        text(105,724,"Nonphysical movement guide"),
        text(58,754,"The central landing above is shown in grey.","small"),
    ])
    # This section shows the actual girder station, with the west aperture beneath its ledger.
    for bounds in ((64,-6.3,70.5,-6),(64,-3.54,64.24,.75),(70.26,-6,70.5,.75),
                   (64,-.30,65.9,0),(68.1,-.30,70.5,0),(66.1,-3.30,67.9,-3)):
        svg.append(rect(bounds,section,"#bfb6a9","#73695c"))
    svg.extend([
        rect((64.12,-3.62,70.43,-3.30),section,"#536572","#38434a"),
        rect((64.03,-3.62,64.46,-3.30),section,"#405560","#3a474d"),
        rect((70.18,-3.62,70.46,-3.30),section,"#405560","#3a474d"),
        line(((64.4,-5.84),(69.83,-5.84)),section,"#216f79","5 4"),
    ])
    for y in (0,-3,-6):
        _,yy = section(64,y)
        svg.append(text(658,yy+4,f"{y:.0f} m","small","end"))
    x,y = section(67.65,-4.8)
    svg.append(text(x,y,"2.38 m below girder","small","middle"))
    svg.extend([
        text(556,491,"The transfer beams seat in both hall walls."),
        text(556,514,"Separate rim joists end outside the stair well."),
        text(556,562,"Service journeys","head"),
    ])
    flows = (
        "Workshop ↔ turbine hall ↔ guarded east stair ↔ crest",
        "Turbine hall ↔ lower service aisle ↔ observation gallery",
        "Lower return ↔ U-stair landing ↔ lower east hall",
        "Crest ↔ east stair ↔ lower service aisle ↔ observation gallery",
        "Van ramp ↔ crest ↔ east stair ↔ observation gallery",
    )
    for i,flow in enumerate(flows):
        y = 594+36*i
        svg.extend([text(556,y,flow),f'<path d="M 556 {y+10} H 1097" stroke="#d4ccc0"/>'])
    svg.extend([text(556,757,"The −8 m return crosses its existing full landing between guards.","small"),
                "</svg>"])
    return "\n".join(svg)+"\n"


def joined(segments):
    points = []
    for path in segments:
        assert not points or points[-1]==path[0],segments
        points.extend(path[1:] if points else path)
    return points


def verify():
    VERIFY.mkdir(parents=True,exist_ok=True)
    layout = json.loads((OUT/"authored-layout.json").read_bytes())
    (OUT/"plan-and-section.svg").write_bytes(draw(layout).encode())
    m = json.loads((OUT/"construction.json").read_bytes())
    native = OUT/"east-hall-bearings.hcs2"
    digest = sha(native)
    assert digest==m["physics_sha256"]
    paths = {row["id"]:row["points"] for row in layout["links"]}
    aisle = [layout["east_hall_bearings"]["lower_service_aisle_x"],-6,35]
    turbine_in = paths["L11"][:paths["L11"].index(aisle)+1]
    return_in = paths["L18"][:paths["L18"].index(aisle)+1]
    spillway_out = paths["L12"][paths["L12"].index(aisle):]
    journeys = (
        ("Workshop to crest through east stair", joined((paths["L09"],paths["L10"]))),
        ("Turbine hall to spillway along lower bypass", joined((turbine_in,spillway_out))),
        ("Maintenance return to spillway via lower bypass", joined((return_in,spillway_out))),
        ("Crest to spillway through east stair", joined((list(reversed(paths["L10"])),paths["L12"]))),
        ("Van ramp to spillway through crest and east hall", joined((paths["L01"],paths["L02"],list(reversed(paths["L10"])),paths["L12"]))),
    )
    rows,labels = [],{}
    for i,(label,points) in enumerate(journeys,1):
        key = f"J{i:02}"
        labels[key] = dict(name=label,points=points)
        for j,(a,b) in enumerate(zip(points,points[1:])):
            rows.append("\t".join(map(str,("WLD-09","after",native,digest,key,j,*a,*b))))
    build = ROOT/"build/dam-east-hall-java"
    build.mkdir(parents=True,exist_ok=True)
    java = Path("C:/Program Files/Java/jdk-17/bin")
    jar = ROOT/"build/beckenburg-game.jar"
    sources = [ROOT/"src/main/java/org/beckenburg/AuthoredSitePhysics.java",
               ROOT/"tools/java/RosterStairMovementProbe.java"]
    subprocess.run([str(java/"javac.exe"),"-cp",str(jar),"-d",str(build),*map(str,sources)],check=True)
    inputs,result = VERIFY/"complete-journey-input.tsv",VERIFY/"complete-journey-sweeps.tsv"
    inputs.write_bytes(("\n".join(rows)+"\n").encode())
    tags,empty = build/"journey-parts.tsv",build/"empty-flights.tsv"
    tags.write_bytes(("\n".join(f"WLD-09:after\t{name}" for name in m["stair_components"])+"\n").encode())
    empty.write_bytes(b"")
    subprocess.run([str(java/"java.exe"),"-Djava.awt.headless=true","-Xmx2g","-cp",f"{build};{jar}",
                    "RosterStairMovementProbe",str(tags),str(empty),str(build/"empty-results.tsv"),
                    str(result),str(inputs)],check=True)
    with result.open(newline="") as stream:
        proof = [dict(row,**labels[row["link"]]) for row in csv.DictReader(stream,delimiter="\t")]
    assert len(proof)==len(labels) and all(x["authored_forward"]==x["authored_reverse"]=="PASS" for x in proof),proof
    report = dict(geometry=digest,journeys=proof,
                  source_sha256={p.relative_to(ROOT).as_posix():sha(p) for p in (*sources,Path(__file__))},
                  evidence_sha256={p.name:sha(p) for p in (inputs,result)})
    (VERIFY/"complete-journeys.json").write_bytes((json.dumps(report,indent=2)+"\n").encode())
    print("DAM EAST HALL JOINED WALKS",len(proof))


if __name__=="__main__":
    verify()
