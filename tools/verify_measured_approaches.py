"""Verify joined journeys and exercise the canonical School arrival in the engine."""
import csv
import os
from pathlib import Path
import subprocess

from collections import deque

from build_measured_approaches import OUT, ROOT, read, relative, sha, write

JOURNEYS = {
    "FARMHOUSE": [
        ("Van cargo ramp to the parents' room", "L01 L02 L03 L05 L34 -L13 L16"),
        ("Root store to the upper landing", "-L20 -L19 L35 L34"),
        ("Van cargo ramp to the hayloft", "L01 L02 L23 L28 L36 L29"),
    ],
    "WLD-08": [
        ("Van cargo ramp to the lab through the generator room and tunnel", "L01 L02 L09 L12 -L20 -L19 -L17"),
        ("Radio room to the service shelter", "L10 L14 L16"),
        ("Court and mess hall loop", "L06 L07 L08 L02"),
    ],
    "SCHOOL": [
        ("Van ramp toe to the plant room", "L42 -L35 L04 L09 L08 -L44 L33"),
        ("Van ramp toe to the upper lobby", "L42 -L35 L04 L09 L43 L45"),
        ("North grounds to the front foyer", "L36 L41 -L35"),
    ],
}


def table(path, rows):
    path.write_bytes("".join("\t".join(map(str, row)) + "\n" for row in rows).encode())


def join(points, chain):
    joined = []
    for key in chain:
        part = points[key] if not key.startswith("-") else list(reversed(points[key[1:]]))
        assert not joined or joined[-1] == part[0], (key, joined[-1], part[0])
        joined.extend(part[1:] if joined else part)
    return joined


def reachable_steps(layout):
    neighbors = {space["id"]: [] for space in layout["zones"] if space["gameplay_eligible"]}
    for link in layout["links"]:
        if link.get("player_access") == "SEALED_NO_ROUTE":
            continue
        a, b = link["a"], link["b"]
        if a in neighbors and b in neighbors:
            neighbors[a].append((b,link["points"]))
            neighbors[b].append((a,list(reversed(link["points"]))))
    pending, visited = deque(["stage"]), {"stage"}
    while pending:
        a = pending.popleft()
        for b, points in neighbors[a]:
            if b in visited:
                continue
            visited.add(b)
            pending.append(b)
            yield ("TRANSIT",a,b,*(coord for point in points for coord in point))
    assert visited == neighbors.keys(), sorted(neighbors.keys()-visited)


def main():
    out = OUT / "verification"
    out.mkdir(parents=True, exist_ok=True)
    build = ROOT / "build/measured-approach-review"
    build.mkdir(parents=True, exist_ok=True)
    school_manifest = out/"ramp-runtime-input.tsv"
    school_results = out/"ramp-runtime-proof.tsv"
    suites, components, labels, packets = [], [], {}, {}
    for row in read(OUT/"roster.json")["active_sources"]:
        site = row["id"]
        if site not in JOURNEYS:
            continue
        packets[site] = row
        layout, contract = (read(ROOT/row[key]) for key in ("layout","construction"))
        points = {link["id"]: link["points"] for link in layout["links"]}
        components.extend((f"{site}:after",n) for n in contract["stair_components"])
        for i,(label, route) in enumerate(JOURNEYS[site],1):
            ident = f"J{i:02}"
            chain = route.split()
            coords = join(points,chain)
            labels[site,ident] = dict(name=label, links=chain)
            for segment,(a,b) in enumerate(zip(coords, coords[1:])):
                suites.append((site,"after",row["native"],row["native_sha256"],ident,segment,*a,*b))
        if site == "SCHOOL":
            staging = layout["staging"]
            school_rows = [
                ("SOURCE",row["glb"],row["glb_sha256"],row["native"],row["native_sha256"]),
                ("DOCK",*staging["point"],staging["yaw"]),
            ]
            school_rows.extend(("TREAD",n) for n in contract["stair_components"])
            school_rows.extend(("SPACE",s["id"],s["owner"],*s["bounds"],s["y"],s["height"],
                                str(s["kind"] in ("outside","staging")).lower(),
                                str(s["gameplay_eligible"]).lower()) for s in layout["zones"])
            school_rows.extend(("POINT",*p) for p in join(points,("L42","-L35")))
            school_rows.extend(reachable_steps(layout))
            table(school_manifest,school_rows)

    java = Path("C:/Program Files/Java/jdk-17/bin")
    dependencies = ROOT/"build/beckenburg-game.jar"
    tools = ROOT/"tools/java"
    sources = [tools/"NativeSchoolArrivalProbe.java",tools/"RosterStairMovementProbe.java"]
    subprocess.run([str(java/"javac.exe"),"-encoding","UTF-8","-Xprefer:source",
                    "-sourcepath",str(ROOT/"src/main/java"),"-cp",str(dependencies),
                    "-d",str(build),*map(str,sources)],cwd=ROOT,check=True)
    cp = os.pathsep.join(map(str,(build,ROOT/"src/main/resources",dependencies)))
    vm = [str(java/"java.exe"),"-Djava.awt.headless=true","-Xmx2g","-cp",cp]
    subprocess.run(vm+["org.beckenburg.NativeSchoolArrivalProbe",str(school_manifest),str(school_results)],
                   cwd=ROOT,check=True)
    routes, results = out/"complete-journey-input.tsv",out/"complete-journey-sweeps.tsv"
    names, empty = build/"stair-components.tsv",build/"empty-flights.tsv"
    table(names,components)
    empty.write_bytes(b"")
    table(routes,suites)
    subprocess.run(vm+["RosterStairMovementProbe",str(names),str(empty),str(build/"unused.tsv"),
                       str(results),str(routes)],cwd=ROOT,check=True)
    with results.open(newline="") as stream:
        rows = [dict(row,**labels[row["site"],row["link"]])
                for row in csv.DictReader(stream,delimiter="\t")]
    assert len(rows) == len(labels) and all(row["authored_forward"] == row["authored_reverse"] == "PASS"
                                            for row in rows), rows
    engine = ROOT/"src/main/java/org/beckenburg"
    files = [Path(__file__),*sources,*[engine/name for name in (
             "HouseMap.java","VanStaging.java","VanInteriorLayout.java","AuthoredSitePhysics.java",
             "NativeInvestigationScene.java","HunterPosture.java","GlbProps.java","LevelDefinition.java")]]
    report = dict(native_packets=packets, journeys=rows, source_hashes={relative(p):sha(p) for p in files},
                  dependency_jar_sha256=sha(dependencies),
                  compiled_engine_classes_sha256={
                      path.relative_to(build).as_posix():sha(path)
                      for path in sorted(build.rglob("*.class"))},
                  proof={p.name:sha(p) for p in (school_manifest,school_results,routes,results)},
                  scope="Native joined journeys and one staged production School arrival replay with the listed volumes. Shared investigation services, dynamic building doors and multiplayer acceptance remain open.")
    write(out/"journeys-and-ramp-runtime.json",report)
    print("MEASURED JOINED JOURNEYS",len(rows),flush=True)


if __name__ == "__main__":
    main()
