"""Draw measured site circulation and sweep joined walks against the native exports."""
import csv
from html import escape
import json
from pathlib import Path
import subprocess

from prepare_workyard_station import ROOT, PASS, CANNERY, RAILWAY, SITES, sha

FOCUS = {CANNERY: ("L01","L02","L03","L14","L15","L17","L18"),
         RAILWAY: ("L01","L02","L03","L04","L09","L15","L16","L17")}
ROOMS = {CANNERY: ("processing","wash","bunk","mess","office","stores","cold"),
         RAILWAY: ("signal","coach-a","coach-b","trestle","tunnel","refuge","store")}
JOURNEYS = {
    CANNERY: (
        ("Van ramp to main processing", (1,2)),
        ("Van ramp to the bunkhouse via wash", (1,3,14)),
        ("Bunkhouse, woodland court and mess loop", (17,18,-15)),
        ("Mess to processing via the woodland court", (-18,-17,-14,-3,2)),
    ),
    RAILWAY: (
        ("Van ramp to railway cutting via the signal hut", (1,2,3)),
        ("Signal hut to the siding through the forecourt", (-2,4)),
        ("Van ramp to siding store", (1,4,15)),
    ),
}


def plan(site, layout):
    zones = {row["id"]:row for row in layout["zones"]}
    links = {row["id"]:row for row in layout["links"]}
    features = layout["architectural_interfaces"]
    x0,z0,x1,z1 = layout["site_bounds"]
    ratio = min(520/(x1-x0),580/(z1-z0))
    left = 52+(520-(x1-x0)*ratio)/2
    top = 155+(580-(z1-z0)*ratio)/2

    def project(x,z):
        return left+(x-x0)*ratio,top+(z-z0)*ratio

    def coords(shape):
        return " ".join(f"{x:.2f},{y:.2f}" for x,y in (project(*p) for p in shape))

    def text(x,y,label,style="body",anchor="start"):
        return f'<text x="{x:.2f}" y="{y:.2f}" class="{style}" text-anchor="{anchor}">{escape(str(label))}</text>'

    def polygon(shape,fill,stroke):
        return f'<polygon points="{coords(shape)}" fill="{fill}" stroke="{stroke}" stroke-width="1"/>'

    def line(shape,color,dash=None):
        suffix = f' stroke-dasharray="{dash}"' if dash else ""
        return f'<polyline points="{coords(shape)}" fill="none" stroke="{color}" stroke-width="2.2" stroke-linecap="round"{suffix}/>'

    title = layout["title"]
    svg = [
        f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1130 820" role="img" aria-label="{escape(title)} plan and movement guides">',
        '<rect width="1130" height="820" fill="#f8f3e8"/>',
        '<style>text{font-family:Segoe UI,Arial,sans-serif;fill:#233b38}.title{font-size:34px;font-weight:650}'
        '.head{font-size:18px;font-weight:650}.body{font-size:14px}.small{font-size:12px}'
        '.room{font-size:11px;font-weight:650;paint-order:stroke;stroke:#f8f3e8;stroke-width:2.5}</style>',
        text(34,61,title,"title"), text(34,88,"Built grounds, doors and movement"),
        '<rect x="34" y="108" width="556" height="671" rx="9" fill="#e9e4d8"/>',
        '<rect x="612" y="108" width="485" height="671" rx="9" fill="#eee9de"/>',
        text(54,138,"Ground plan at 0 m","head"), text(567,138,"North ↑","small","end"),
        text(634,138,"Door, yard and stair connections","head"),
    ]
    for feature in features["fields"]:
        svg.append(polygon(feature["polygon"],"#c8c6a3","#939173"))
    for court in features["terraces"]:
        svg.append(polygon(court["polygon"],"#b9a58e","#8a7258"))
    for zone in zones.values():
        if zone["y"]!=0 or zone["kind"] not in ("inside","circulation","sealed"):
            continue
        a,b,c,d = zone["bounds"]
        svg.append(polygon([(a,b),(c,b),(c,d),(a,d)],"#f8f1e1","#746a5b"))
    if features["retaining_wall"]:
        a,b = features["retaining_wall"]["x_bounds"]
        z = [datum for datum,_ in features["retaining_wall"]["cap_profile"]]
        svg.append(polygon([(a,z[0]),(b,z[0]),(b,z[-1]),(a,z[-1])],"#847263","#655445"))
    if features["landing_foundation"]:
        a,b,c,d = features["landing_foundation"]
        svg.append(polygon([(a,b),(c,b),(c,d),(a,d)],"#bba7c4","#675074"))
    for key in FOCUS[site]:
        link = links[key]
        projected = [(x,z) for x,_,z in link["points"]]
        svg.append(line(projected,"#6a537b" if link["kind"]=="stairs" else "#216f79","5 4"))
    for door in layout["authored_doorways"]:
        if door["sill_y"]!=0:
            continue
        fixed,along,width = (door[k] for k in ("boundary","along","clear_width"))
        low,high = along-width/2,along+width/2
        shape = ((fixed,low),(fixed,high)) if door["axis"]=="x" else ((low,fixed),(high,fixed))
        svg.append(line(shape,"#b0552f"))
    for room in ROOMS[site]:
        a,b,c,d = zones[room]["bounds"]
        x,y = project((a+c)/2,(b+d)/2)
        svg.append(text(x,y-5,zones[room]["name"],"room","middle"))
    x,y = project(*features["arrival_ramp"])
    svg.extend([f'<circle cx="{x:.2f}" cy="{y:.2f}" r="4.6" fill="#216f79" stroke="#ffffff"/>',
                text(x+8,y+15,"Van ramp","room")])
    svg.extend([f'<path d="M 56 744 v 6 h {10*ratio:.2f} v -6" fill="none" stroke="#233b38"/>',
                text(56,766,"0","small"),text(56+10*ratio,766,"10 m","small","end")])
    for i,key in enumerate(FOCUS[site]):
        row = links[key]
        a,b = (zones[row[k]]["name"] for k in ("a","b"))
        y = 175+i*38
        svg.extend([text(634,y,key,"small"),text(674,y,f"{a} ↔ {b}"),
                    f'<path d="M 634 {y+11} H 1075" stroke="#d6cec1"/>'])
    svg.extend([
        '<path d="M 634 501 H 675" stroke="#b0552f" stroke-width="2.2"/>',
        text(688,506,"Physical door opening"),
        '<path d="M 634 533 H 675" stroke="#216f79" stroke-width="2.2" stroke-dasharray="5 4"/>',
        text(688,538,"Nonphysical movement guide"),
    ])
    if site==RAILWAY:
        svg.extend([
            '<path d="M 634 570 H 675" stroke="#6a537b" stroke-width="2.2" stroke-dasharray="5 4"/>',
            text(688,575,"Guarded stair and lower landing guide"),
            text(634,626,"Trestle lower landing","head"),
            text(634,651,"The service floor and receiving bay sit at −3.0 m."),
            text(634,676,"The deck above stays at 0 m. Retaining walls bound the bay."),
            text(634,701,"Both retained flights have a 1.50 m clear width."),
        ])
    else:
        svg.extend([
            text(634,620,"Woodland workers’ court","head"),
            text(634,645,"Both forecourts sit flush with the occupied rooms."),
            text(634,670,"Each fitted west door has a 1.42 m clear opening."),
            text(634,695,"One founded wall retains the woodland edge."),
        ])
    svg.append("</svg>")
    return "\n".join(svg)+"\n"


def main():
    directory = PASS/"verification"
    directory.mkdir(parents=True,exist_ok=True)
    roster = json.loads((PASS/"roster.json").read_bytes())
    names,rows,labels = [],[],{}
    for site in SITES:
        source = next(row for row in roster["active_sources"] if row["id"]==site)
        layout_file = ROOT/source["layout"]
        layout = json.loads(layout_file.read_bytes())
        layout_file.with_name("ground-plan-and-flow.svg").write_bytes(plan(site,layout).encode())
        native = ROOT/source["native"]
        digest = sha(native)
        assert digest==source["native_sha256"]
        meta = json.loads((ROOT/source["construction"]).read_bytes())
        names.extend(f"{site}:after\t{value}" for value in meta["stair_components"])
        routes = {row["id"]:row["points"] for row in layout["links"]}
        for i,(label,chain) in enumerate(JOURNEYS[site],1):
            key = f"J{i:02}"
            points = []
            for number in chain:
                section = routes[f"L{abs(number):02}"]
                if number<0:
                    section = list(reversed(section))
                assert not points or points[-1]==section[0],(site,label,number)
                points.extend(section if not points else section[1:])
            labels[site,key] = dict(name=label,links=list(chain))
            for index,(a,b) in enumerate(zip(points,points[1:])):
                rows.append("\t".join(map(str,[site,"after",native,digest,key,index,*a,*b])))
    build = ROOT/"build/workyard-station-java"
    java = Path("C:/Program Files/Java/jdk-17/bin")
    jar = ROOT/"build/beckenburg-game.jar"
    sources = [ROOT/"src/main/java/org/beckenburg/AuthoredSitePhysics.java",
               ROOT/"tools/java/RosterStairMovementProbe.java"]
    subprocess.run([str(java/"javac.exe"),"-cp",str(jar),"-d",str(build),*map(str,sources)],check=True)
    inputs,result = directory/"complete-journey-input.tsv",directory/"complete-journey-sweeps.tsv"
    inputs.write_bytes(("\n".join(rows)+"\n").encode())
    tags,empty = build/"journey-names.tsv",build/"empty-flights.tsv"
    tags.write_bytes(("\n".join(names)+"\n").encode())
    empty.write_bytes(b"")
    subprocess.run([str(java/"java.exe"),"-Djava.awt.headless=true","-Xmx2g","-cp",f"{build};{jar}",
                    "RosterStairMovementProbe",str(tags),str(empty),str(build/"empty-results.tsv"),
                    str(result),str(inputs)],check=True)
    with result.open(newline="") as stream:
        measured = [dict(row,**labels[row["site"],row["link"]]) for row in csv.DictReader(stream,delimiter="\t")]
    assert len(measured)==len(labels) and all(row["authored_forward"]==row["authored_reverse"]=="PASS" for row in measured),measured
    report = dict(scope="Joined native walks from the real van ramp and through actual doors and workyards.",
                  source_files_sha256={q.relative_to(ROOT).as_posix():sha(q) for q in (*sources,Path(__file__))},
                  evidence_sha256={q.name:sha(q) for q in (inputs,result)},journeys=measured)
    (directory/"complete-journeys.json").write_bytes((json.dumps(report,indent=2)+"\n").encode())
    print("WORKYARD STATION JOINED WALKS",len(measured))


if __name__ == "__main__":
    main()
