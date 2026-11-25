"""Draw the three reviewed ground plans and probe joined doorway-to-doorway walks."""
import csv
from html import escape
import json
from pathlib import Path
import subprocess

from prepare_estate_parish import ROOT, PASS, SITES, sha

JOURNEYS = {
    "MANSION": (
        ("Van to foyer", (1,2)),
        ("Van to ballroom", (1,-31,-30)),
        ("Library to service stairs", (29,-31,-28)),
        ("Receiving to ballroom", (26,-30)),
    ),
    "FARMHOUSE": (
        ("Van to mudroom", (1,2,3)),
        ("Prep to repair workshop", (11,30)),
        ("Barn north to laundry", (-22,33)),
        ("Van to the barn south door", (1,2,23)),
    ),
    "CHURCH": (
        ("Van to vestibule", (1,2)),
        ("Vestry around to the chapel", (18,-17)),
        ("Vestry around to the vestibule", (18,30,2)),
    ),
}
ROOM_LABELS = {
    "MANSION": ("foyer","library","drawing","ballroom","receiving","service-stair","hall","dining","kitchen"),
    "FARMHOUSE": ("mudroom","prep","parlour","dining","barn-aisle","loft-stair","repair","laundry"),
    "CHURCH": ("vestibule","nave","rear-nave","vestry","chapel","crypt-entry","confessional"),
}


def drawing(site, layout, construction, changed):
    interfaces = layout["architectural_interfaces"]
    fields, courts = (interfaces[k] for k in ("fields","terraces"))
    xmin,zmin,xmax,zmax = layout["site_bounds"]
    points = [p for row in fields+courts for p in row["polygon"]]
    xmin,zmin = min(xmin,*(p[0] for p in points)), min(zmin,*(p[1] for p in points))
    xmax,zmax = max(xmax,*(p[0] for p in points)), max(zmax,*(p[1] for p in points))
    unit = min(528/(xmax-xmin),568/(zmax-zmin))
    left = 54+(528-(xmax-xmin)*unit)/2
    top = 163+(568-(zmax-zmin)*unit)/2

    def project(x,z):
        return left+(x-xmin)*unit,top+(z-zmin)*unit

    def coords(nodes):
        return " ".join(f"{x:.2f},{y:.2f}" for x,y in (project(*p) for p in nodes))

    def text(x,y,label,style="body",anchor="start"):
        return f'<text x="{x:.2f}" y="{y:.2f}" class="{style}" text-anchor="{anchor}">{escape(str(label))}</text>'

    links = {row["id"]:row for row in layout["links"]}
    zones = {row["id"]:row for row in layout["zones"]}
    title = escape(layout["title"])
    svg = [
        f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1110 835" role="img" aria-label="{title} ground plan and circulation">',
        '<rect width="1110" height="835" fill="#f9f5ea"/>',
        '<style>text{font-family:Segoe UI,Arial,sans-serif;fill:#233b38}.title{font-size:34px;font-weight:650}'
        '.head{font-size:18px;font-weight:650}.body{font-size:14px}.small{font-size:12px}'
        '.room{font-size:11px;font-weight:650;paint-order:stroke;stroke:#f7efe5;stroke-width:2.5}</style>',
        text(38,63,layout["title"],"title"), text(38,90,"Ground floor and exterior circulation"),
        '<rect x="38" y="112" width="562" height="666" rx="9" fill="#eae5d8"/>',
        '<rect x="620" y="112" width="452" height="666" rx="9" fill="#eee9de"/>',
        text(56,144,"Ground plan at 0 m", "head"),
        text(577,144,"North ↑","small","end"),
        text(644,144,"Door and court connections","head"),
    ]
    for field in fields:
        fill = "#dac5a5" if field["material"]=="GRAVEL" else "#bcc4a5"
        svg.append(f'<polygon points="{coords(field["polygon"])}" fill="{fill}" stroke="#878c70" stroke-width="1"/>')
    for terrace in courts:
        svg.append(f'<polygon points="{coords(terrace["polygon"])}" fill="#b4a797" stroke="#705d49" stroke-width="1.2"/>')
    for zone in zones.values():
        if zone["y"]!=0 or zone["kind"] not in ("inside","circulation","sealed"):
            continue
        a,b,c,d = zone["bounds"]
        points = coords(((a,b),(c,b),(c,d),(a,d)))
        fill = "#c3b59d" if zone["kind"]=="sealed" else "#faf3e4"
        svg.append(f'<polygon points="{points}" fill="{fill}" stroke="#655e53" stroke-width="1"/>')
    for key in changed:
        points = [(x,z) for x,_,z in links[key]["points"]]
        svg.append(f'<polyline points="{coords(points)}" fill="none" stroke="#1d6e77" stroke-width="2.3" stroke-dasharray="5 3" stroke-linecap="round"/>')
    for door in construction["new_portals"]:
        axis,bound,center,width = (door[k] for k in ("axis","boundary","along","clear_width"))
        a,b = center-width/2,center+width/2
        points = coords(((bound,a),(bound,b)) if axis=="x" else ((a,bound),(b,bound)))
        svg.append(f'<polyline points="{points}" stroke="#f7efe5" stroke-width="5"/>')
        svg.append(f'<polyline points="{points}" stroke="#b0512b" stroke-width="3"/>')
    for name in ROOM_LABELS[site]:
        zone = zones[name]
        a,b,c,d = zone["bounds"]
        x,y = project((a+c)/2,(b+d)/2)
        svg.append(text(x,y-5,zone.get("label",name.replace("-"," ").title()),"room","middle"))
    x,y = project(*interfaces["arrival_ramp"])
    svg.extend([f'<circle cx="{x:.2f}" cy="{y:.2f}" r="4.8" fill="#1d6e77" stroke="white"/>',
                text(x+9,y+17,"Van ramp","room")])
    scale = 5*unit
    svg.extend([f'<path d="M 56 744 v 6 h {scale:.2f} v -6" fill="none" stroke="#233b38" stroke-width="1.2"/>',
                text(56,766,"0","small"),text(56+scale,766,"5 m","small","end")])

    for index,key in enumerate(sorted(changed)):
        link = links[key]
        y = 183+41*index
        a,b = (zones[link[k]].get("label",link[k].replace("-"," ").title()) for k in ("a","b"))
        door = next((row for row in construction["new_portals"] if row["link"]==key),None)
        label = f'{door["clear_width"]:.2f} m {door["id"].split()[-3].upper()} door' if door else "open ground"
        svg.extend([text(646,y,key,"small"),text(688,y,f"{a} ↔ {b}"),
                    text(1043,y,label,"small","end"),
                    f'<path d="M 644 {y+12} H 1044" stroke="#d7d0c3"/>'])
    svg.extend([text(644,557,"Built surfaces","head"),
                text(644,585,"Gravel and planted grades are part of the saved terrain."),
                text(644,608,"Stone courts and porch slabs carry the door approaches."),
                '<path d="M 645 642 H 676" stroke="#b0512b" stroke-width="3"/>',
                text(688,647,"Fitted exterior doorway"),
                '<path d="M 645 672 H 676" stroke="#1d6e77" stroke-width="2.3" stroke-dasharray="5 3"/>',
                text(688,677,"Nonphysical walking guidance"),
                text(644,731,"Room shapes show the ground-level program.","small"),
                text(644,751,"Full walls, stairs and foundations are in the native model.","small"),
                "</svg>"])
    return "\n".join(svg)+"\n"


def verify():
    def read(path):
        return json.loads(path.read_bytes())

    directory = PASS/"verification"
    directory.mkdir(parents=True,exist_ok=True)
    roster = read(PASS/"roster.json")
    names,segments = [],[]
    journeys = {}
    for site in SITES:
        source = next(row for row in roster["active_sources"] if row["id"]==site)
        folder = ROOT/source["layout"]
        layout = read(folder)
        construction = read(ROOT/source["construction"])
        changed = read(folder.with_name("quarantine.json"))["superseded_guides"]
        folder.with_name("ground-plan-and-flow.svg").write_bytes(drawing(site,layout,construction,changed).encode())
        native = ROOT/source["native"]
        assert sha(native)==source["native_sha256"]
        names.extend(f'{site}:after\t{name}' for name in construction["stair_components"])
        links = {row["id"]:row["points"] for row in layout["links"]}
        for i,(label,path) in enumerate(JOURNEYS[site],1):
            key = f"J{i:02}"
            points = []
            for number in path:
                part = links[f"L{abs(number):02}"]
                if number<0:
                    part = list(reversed(part))
                assert not points or points[-1]==part[0],(site,label,number)
                points.extend(part if not points else part[1:])
            journeys[site,key] = dict(name=label,links=list(path))
            for index,(a,b) in enumerate(zip(points,points[1:])):
                segments.append("\t".join(map(str,[site,"after",native,sha(native),key,index,*a,*b])))
    build = ROOT/"build/estate-parish-java"
    java = Path("C:/Program Files/Java/jdk-17/bin")
    jar = ROOT/"build/beckenburg-game.jar"
    sources = [ROOT/"src/main/java/org/beckenburg/AuthoredSitePhysics.java",
               ROOT/"tools/java/RosterStairMovementProbe.java"]
    subprocess.run([str(java/"javac.exe"),"-cp",str(jar),"-d",str(build),*map(str,sources)],check=True)
    paths, result = directory/"complete-journey-input.tsv",directory/"complete-journey-sweeps.tsv"
    paths.write_bytes(("\n".join(segments)+"\n").encode())
    tags, empty = build/"journey-names.tsv",build/"empty-flights.tsv"
    tags.write_bytes(("\n".join(names)+"\n").encode())
    empty.write_bytes(b"")
    subprocess.run([str(java/"java.exe"),"-Djava.awt.headless=true","-Xmx2g","-cp",f"{build};{jar}",
                    "RosterStairMovementProbe",str(tags),str(empty),str(build/"empty-results.tsv"),
                    str(result),str(paths)],check=True)
    with result.open(newline="") as lines:
        results = [dict(row,**journeys[row["site"],row["link"]]) for row in csv.DictReader(lines,delimiter="\t")]
    assert len(results)==len(journeys) and all(row["authored_forward"]==row["authored_reverse"]=="PASS" for row in results)
    report = dict(scope="Native joined movement sweeps across actual building entrances and exterior courts.",
                  source_files_sha256={p.relative_to(ROOT).as_posix():sha(p) for p in (*sources,Path(__file__))},
                  evidence_sha256={p.name:sha(p) for p in (paths,result)},journeys=results)
    (directory/"complete-journeys.json").write_bytes((json.dumps(report,indent=2)+"\n").encode())
    print("ESTATE PARISH JOINED JOURNEYS",len(results))


if __name__ == "__main__":
    verify()
