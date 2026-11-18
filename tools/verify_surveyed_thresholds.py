
"""Verify consecutive measured doors and full supported exit/landing movements."""
import csv
from pathlib import Path
import subprocess
from os import pathsep

from build_surveyed_thresholds import OUT, ROOT, read, relative, sha, write
from verify_measured_approaches import table

JOURNEYS = {
    "ACADEMY": ("West corridor to the lower stair hall", ("L40",)),
    "ASYLUM": ("Treatment, changing, baths and drying loop", ("L40","L41","L42","L43")),
    "PRISON": ("East gallery through the front link to the next fitted doorway", ("L050","-L078")),
    "WLD-18": ("West service vault through the framed escape door and up the exit flight", ("route-08",)),
}


def main():
    work = ROOT/"build/surveyed-threshold-journeys"
    work.mkdir(parents=True,exist_ok=True)
    evidence = OUT/"verification"
    evidence.mkdir(parents=True,exist_ok=True)
    rows, names, sources = [],[],{}
    for row in read(OUT/"roster.json")["active_sources"]:
        site = row["id"]
        if site not in JOURNEYS:
            continue
        label, identities = JOURNEYS[site]
        layout, contract = (read(ROOT/row[key]) for key in ("layout","construction"))
        links = {link["id"]:link["points"] for link in layout["links"]}
        joined = []
        for ident in identities:
            points = list(reversed(links[ident[1:]])) if ident.startswith("-") else links[ident]
            if joined:
                assert joined[-1]==points[0], (site,ident)
                joined.extend(points[1:])
            else:
                joined.extend(points)
        names.extend((f"{site}:after",name) for name in contract["stair_components"])
        rows.extend((site,"after",row["native"],row["native_sha256"],"door-journey",i,*a,*b)
                    for i,(a,b) in enumerate(zip(joined,joined[1:])))
        sources[site] = dict(label=label,links=identities,layout=row["layout"],layout_sha256=contract["layout_sha256"],
                             native=row["native"],native_sha256=row["native_sha256"])
    components = work/"stair-components.tsv"
    table(components,names)
    inputs,results = evidence/"journey-input.tsv",evidence/"journey-results.tsv"
    table(inputs,rows)
    empty = work/"empty.tsv"
    empty.write_bytes(b"")
    probe = ROOT/"tools/java/RosterStairMovementProbe.java"
    physics = ROOT/"src/main/java/org/beckenburg/AuthoredSitePhysics.java"
    java = Path("C:/Program Files/Java/jdk-17/bin")
    jar = ROOT/"build/beckenburg-game.jar"
    subprocess.run([str(java/"javac.exe"),"-encoding","UTF-8","-cp",str(jar),"-d",str(work),
                    str(physics),str(probe)],check=True)
    subprocess.run([str(java/"java.exe"),"-Djava.awt.headless=true","-Xmx2g","-cp",
                    pathsep.join(map(str,(work,jar))),"RosterStairMovementProbe",
                    str(components),str(empty),str(work/"unused.tsv"),str(results),str(inputs)],check=True)
    with results.open(newline="") as stream:
        tested = list(csv.DictReader(stream,delimiter="\t"))
    assert len(tested)==len(sources) and all(r["authored_forward"]==r["authored_reverse"]=="PASS" for r in tested),tested
    write(evidence/"joined-journeys.json",dict(routes=[dict(r,**sources[r["site"]]) for r in tested],
          hashes={relative(p):sha(p) for p in (Path(__file__),probe,physics,inputs,results)}))


if __name__=="__main__":
    main()
