"""Verify the joined journeys through the promoted physical deck and fitted doors."""
import csv
import os
from pathlib import Path
import subprocess

from build_fitted_approaches import OUT
from build_measured_approaches import ROOT, read, relative, sha, write
from verify_measured_approaches import table

JOURNEYS={
    "WLD-06":[
        ("Van ramp to the net store across the crossdock", "L01 L04 L07 L08 L09 L12"),
        ("West cottage and quay to the fish works", "L02 L05 L07 L08 L13"),
    ],
    "WLD-16":[
        ("Van ramp through the office to the manager's room", "route-01 route-02 route-05"),
        ("Van ramp through the office to the west guest rooms", "route-01 route-02 route-04 route-17"),
    ],
    "CHURCH":[
        ("Chapel to the crypt memorial", "L16 -L35 L24 L25"),
        ("Rear nave through the choir to the music archive", "L33 L19 L21 L22"),
    ],
}
# The guest door is on the continuous westwalk between two different route endpoints.
WESTWALK_JOIN=([14.5,0,34],[14.5,0,32.5])


def main():
    directory=OUT/"verification"
    directory.mkdir(parents=True,exist_ok=True)
    trials,names,labels=[],[],{}
    for row in read(OUT/"roster.json")["active_sources"]:
        site=row["id"]
        if site not in JOURNEYS:
            continue
        layout=read(ROOT/row["layout"])
        contract=read(ROOT/row["construction"])
        links={link["id"]:link["points"] for link in layout["links"]}
        names.extend((f"{site}:after",name) for name in contract["stair_components"])
        for i,(label,sequence) in enumerate(JOURNEYS[site],1):
            code=f"J{i:02}"
            chain=sequence.split()
            joined=[]
            for link in chain:
                points=list(reversed(links[link[1:]])) if link.startswith("-") else links[link]
                if joined and joined[-1]!=points[0]:
                    assert site=="WLD-16" and (joined[-1],points[0])==WESTWALK_JOIN, (site,label,link)
                    joined.append(points[0])
                joined.extend(points[1:] if joined else points)
            labels[site,code]=dict(journey=label,links=chain)
            trials.extend((site,"after",row["native"],row["native_sha256"],code,k,*a,*b)
                          for k,(a,b) in enumerate(zip(joined,joined[1:])))
    build=ROOT/"build/fitted-approach-journeys"
    build.mkdir(parents=True,exist_ok=True)
    components,empty=build/"components.tsv",build/"empty-flights.tsv"
    table(components,names)
    empty.write_bytes(b"")
    source,output=directory/"joined-input.tsv",directory/"joined-sweeps.tsv"
    table(source,trials)
    java=Path("C:/Program Files/Java/jdk-17/bin")
    jar=ROOT/"build/beckenburg-game.jar"
    native=ROOT/"src/main/java/org/beckenburg/AuthoredSitePhysics.java"
    probe=ROOT/"tools/java/RosterStairMovementProbe.java"
    subprocess.run([str(java/"javac.exe"),"-encoding","UTF-8","-cp",str(jar),"-d",str(build),
                    str(native),str(probe)],cwd=ROOT,check=True)
    cp=os.pathsep.join(map(str,(build,jar)))
    subprocess.run([str(java/"java.exe"),"-Djava.awt.headless=true","-Xmx2g","-cp",cp,
                    "RosterStairMovementProbe",str(components),str(empty),str(build/"unused.tsv"),
                    str(output),str(source)],cwd=ROOT,check=True)
    with output.open(newline="") as stream:
        rows=[dict(r,**labels[r["site"],r["link"]]) for r in csv.DictReader(stream,delimiter="\t")]
    assert len(rows)==len(labels) and all(r["authored_forward"]==r["authored_reverse"]=="PASS" for r in rows), rows
    write(directory/"joined-review.json",dict(journeys=rows,source_hashes={relative(p):sha(p) for p in (
        Path(__file__),native,probe,source,output)},westwalk_join=WESTWALK_JOIN))
    print("JOINED FITTED APPROACHES",len(rows),flush=True)


if __name__=="__main__":
    main()
