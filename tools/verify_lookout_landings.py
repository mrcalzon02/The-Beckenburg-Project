"""Draw the authored Lookout and sweep its joined approach and tower journeys."""
import csv
from html import escape
import json
from pathlib import Path
import subprocess

from prepare_lookout_landings import ROOT, PASS, SITE, sha

OUT = PASS/SITE.lower()
VERIFY = PASS/"verification"
JOURNEYS = (
    ("Van ramp to the lookout deck", ("L01","L02","L03","L04","L12","L13","L14","L14B","L15")),
    ("Van ramp to the ranger ruin", ("L01","L02","L03","L04","L07")),
    ("Clearing to the roadhead via the ranger rooms", ("L07","L08","L09","L10")),
    ("Tower halfway landing to the maintenance hut", ("-L13","-L12","L05")),
    ("Clearing loop through the ruin, store and contour return", ("L07","L08","L09","L11")),
)


def draw(layout):
    interface = layout["native_landing_fit"]
    rooms = {row["id"]:row for row in layout["zones"]}
    links = {row["id"]:row for row in layout["links"]}

    def ground(x,z):
        return 73+7.15*x,176+6.25*(z+2)

    def tower(z,y):
        return 603+(z-2)*30,224+(15-y)*19.1

    def coords(points,project):
        return " ".join(f"{x:.2f},{y:.2f}" for x,y in (project(*p) for p in points))

    def polygon(points,project,fill,stroke):
        return f'<polygon points="{coords(points,project)}" fill="{fill}" stroke="{stroke}" stroke-width="1"/>'

    def rect(bounds,project,fill,stroke):
        a,b,c,d = bounds
        return polygon(((a,b),(c,b),(c,d),(a,d)),project,fill,stroke)

    def line(points,project,color,width=2.2,dash=None):
        style = f' stroke-dasharray="{dash}"' if dash else ""
        return f'<polyline points="{coords(points,project)}" fill="none" stroke="{color}" stroke-width="{width}"{style}/>'

    def text(x,y,s,style="body",anchor="start"):
        return f'<text x="{x:.2f}" y="{y:.2f}" class="{style}" text-anchor="{anchor}">{escape(str(s))}</text>'

    svg = [
        '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1160 855" role="img" aria-label="Widow’s Peak Fire Lookout grounds, landings and tower">',
        '<rect width="1160" height="855" fill="#f8f3e8"/>',
        '<style>text{font-family:Segoe UI,Arial,sans-serif;fill:#263a37}.title{font-size:34px;font-weight:650}'
        '.head{font-size:18px;font-weight:650}.body{font-size:14px}.small{font-size:12px}'
        '.room{font-size:12px;font-weight:650;paint-order:stroke;stroke:#f8f3e8;stroke-width:2.5}</style>',
        text(37,62,"Widow’s Peak Fire Lookout","title"),
        text(37,88,"Roadhead, hillside landings and guarded tower access"),
        '<rect x="37" y="113" width="472" height="697" rx="8" fill="#eae5d9"/>',
        '<rect x="529" y="113" width="593" height="697" rx="8" fill="#eee9de"/>',
        text(57,144,"Site plan","head"),
        text(489,144,"North ↑","small","end"),
        text(554,144,"Tower flights and landings by elevation","head"),
    ]
    for area in interface["roadhead"]:
        svg.append(polygon(area["footprint"],ground,"#c8c5a1","#8c896c"))
    for key in ("hut","ruin","store","cab","deck"):
        color = "#f8f1e2" if key!="deck" else "#d1cbc1"
        svg.append(rect(rooms[key]["bounds"],ground,color,"#6d665c"))
    for landing in interface["receiving_landings"]:
        svg.append(rect(landing["bounds"],ground,"#a78e70","#75664f"))
    for key in ("L01","L02","L03","L04","L05","L07","L08","L09","L10","L12","L13","L14B","L15"):
        trace = links[key]["points"]
        svg.append(line([(x,z) for x,y,z in trace],ground,
                        "#735c80" if key in ("L13","L14B","L15") else "#216f79",dash="5 4"))
    for door in layout["authored_doorways"]:
        fixed,along,width = (door[k] for k in ("boundary","along","clear_width"))
        low,high = along-width/2,along+width/2
        edge = ((fixed,low),(fixed,high)) if door["axis"]=="x" else ((low,fixed),(high,fixed))
        svg.append(line(edge,ground,"#a64f2d",width=3.3))
    for point,label,anchor in (
        ((12,19),"Maintenance hut","middle"),((47,20),"Ranger ruin","middle"),
        ((45,34),"Ranger store","middle"),((23.5,47),"Boulder saddle","end"),
        ((31,8),"Lookout tower","start"),((28,75),"Van roadhead","start"),
        ((17,61.1),"Lower court","end"),((43,55),"Contour return","start"),
    ):
        x,y = ground(*point)
        svg.append(text(x,y-8,label,"room",anchor))
    x,_,z = links["L01"]["points"][0]
    px,py = ground(x,z)
    svg.extend([f'<circle cx="{px:.2f}" cy="{py:.2f}" r="4.6" fill="#216f79" stroke="#fff"/>',
                '<path d="M 58 771 H 129.5 M 58 766 v 10 M 129.5 766 v 10" stroke="#263a37"/>',
                text(58,791,"0","small"),text(129.5,791,"10 m","small","end")])

    bands = ((2,16,15),(8.2,10,12.75),(13.9,16,10.5),(8.2,10,8.25),(13.9,16,6))
    for start,end,y in bands:
        svg.append(rect((start,y-.18,end,y),tower,"#a79c8e","#655b4e"))
        sx,sy = tower(1.9,y)
        svg.append(text(sx-10,sy+4,f"+{y:g} m","small","end"))
    cab = rooms["cab"]
    svg.append(rect((cab["bounds"][1],cab["y"],cab["bounds"][3],cab["y"]+cab["height"]),
                    tower,"#ccc3b4","#776b5b"))
    for key,color in (("L13","#5e7275"),("L14","#7d6682"),("L14B","#7d6682")):
        path = links[key]["points"]
        for a,b in zip(path,path[1:]):
            if min(a[1],b[1])<6 or abs(a[1]-b[1])<1:
                continue
            svg.append(line(((a[2],a[1]),(b[2],b[1])),tower,color,width=3.5))
    svg.extend([
        text(554,451,"The two tower lanes alternate across complete guarded landings."),
        text(554,475,"The flights have 1.50 m guided clearance and the cab doors measure 1.25 m."),
        '<path d="M 555 516 H 590" stroke="#a64f2d" stroke-width="3"/>',text(602,521,"Measured doorway"),
        '<path d="M 555 545 H 590" stroke="#216f79" stroke-width="2.2" stroke-dasharray="5 4"/>',
        text(602,550,"Nonphysical approach guide"),
        text(554,592,"Joined movement checks","head"),
    ])
    for i,(label,_) in enumerate(JOURNEYS):
        y = 620+30*i
        svg.extend([text(554,y,label),f'<path d="M 554 {y+9} H 1098" stroke="#d6cdc0"/>'])
    svg.extend([text(554,791,"Brown plan areas identify the three rebuilt receiving landings.","small"),"</svg>"])
    return "\n".join(svg)+"\n"


def verify():
    VERIFY.mkdir(parents=True,exist_ok=True)
    layout = json.loads((OUT/"authored-layout.json").read_bytes())
    meta = json.loads((OUT/"construction.json").read_bytes())
    native = OUT/"lookout-landings.hcs2"
    digest = sha(native)
    assert digest==meta["physics_sha256"]
    (OUT/"plan-and-section.svg").write_bytes(draw(layout).encode())
    links = {row["id"]:row["points"] for row in layout["links"]}
    segments,info = [],{}
    for n,(label,chain) in enumerate(JOURNEYS,1):
        joined = []
        for key in chain:
            points = list(reversed(links[key[1:]])) if key.startswith("-") else links[key]
            assert not joined or joined[-1]==points[0],(label,key,joined[-1],points[0])
            joined.extend(points[1:] if joined else points)
        ident = f"J{n:02}"
        info[ident] = dict(name=label,links=chain)
        for i,(a,b) in enumerate(zip(joined,joined[1:])):
            segments.append("\t".join(map(str,(SITE,"after",native,digest,ident,i,*a,*b))))
    build = ROOT/"build/lookout-landing-java"
    java = Path("C:/Program Files/Java/jdk-17/bin")
    jar = ROOT/"build/beckenburg-game.jar"
    sources = [ROOT/"src/main/java/org/beckenburg/AuthoredSitePhysics.java",
               ROOT/"tools/java/RosterStairMovementProbe.java"]
    subprocess.run([str(java/"javac.exe"),"-cp",str(jar),"-d",str(build),*map(str,sources)],check=True)
    inputs,result = VERIFY/"complete-journey-input.tsv",VERIFY/"complete-journey-sweeps.tsv"
    parts,empty = build/"journey-parts.tsv",build/"empty-flights.tsv"
    inputs.write_bytes(("\n".join(segments)+"\n").encode())
    parts.write_bytes(("\n".join(f"{SITE}:after\t{name}" for name in meta["stair_components"])+"\n").encode())
    empty.write_bytes(b"")
    subprocess.run([str(java/"java.exe"),"-Djava.awt.headless=true","-Xmx2g","-cp",f"{build};{jar}",
                    "RosterStairMovementProbe",str(parts),str(empty),str(build/"empty-results.tsv"),
                    str(result),str(inputs)],check=True)
    with result.open(newline="") as stream:
        rows = [dict(row,**info[row["link"]]) for row in csv.DictReader(stream,delimiter="\t")]
    assert len(rows)==len(info) and all(row["authored_forward"]==row["authored_reverse"]=="PASS" for row in rows),rows
    proof = dict(physics_sha256=digest,journeys=rows,
                 source_sha256={q.relative_to(ROOT).as_posix():sha(q) for q in (*sources,Path(__file__))},
                 evidence_sha256={q.name:sha(q) for q in (inputs,result)})
    (VERIFY/"complete-journeys.json").write_bytes((json.dumps(proof,indent=2)+"\n").encode())
    print("LOOKOUT JOINED WALKS",len(rows))


if __name__=="__main__":
    verify()
