"""Prepare exact Wharf review routes, room volumes and tidal wall probes."""
import argparse
import json
import math
import os
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[1]
BASE = ROOT / "development/levels/rebuild"
SOURCE = BASE / "native-foyer-door-pass/wld-06"
STAGE = BASE / "wharf-tidal-pass"
BUILD = ROOT / "build/wharf-tide-review"
OWNERS = ("cottage-a", "cottage-b", "boathouse", "net", "fish", "undercroft", "west-stair",
          "east-stair", "pier-w", "pier-e", "crossdock", "quay", "entry", "shore-context", "stage")
WALLS = (
    ((24, 24), (40, 24), (0, 1)), ((24, 36), (40, 36), (0, -1)),
    ((24, 24), (24, 31.45), (1, 0)), ((24, 34.55), (24, 36), (1, 0)),
    ((40, 24), (40, 31.45), (-1, 0)), ((40, 34.55), (40, 36), (-1, 0)),
    ((16, 31.45), (24, 31.45), (0, 1)), ((16, 34.55), (24, 34.55), (0, -1)),
    ((16, 31.45), (16, 34.55), (1, 0)), ((40, 31.45), (48, 31.45), (0, 1)),
    ((40, 34.55), (48, 34.55), (0, -1)), ((48, 31.45), (48, 34.55), (-1, 0)),
)


def read(path):
    return json.loads(path.read_bytes())


def inputs():
    layout = read(SOURCE / "authored-layout.json")
    rooms = {r["id"]: r for r in layout["zones"]}
    rooms["shore-context"] = dict(bounds=[-22, -32, -20, -30], y=-4.6, height=1.6,
                                  kind="outside", name="Closed tidal context")
    rows = []
    for owner, identifier in enumerate(OWNERS):
        room = rooms[identifier]
        rows.append(("ROOM", identifier, owner, *room["bounds"], room["y"], room["height"],
                     str(room["kind"] != "inside").lower(),
                     str(identifier != "shore-context").lower(), room["name"]))
    routes = {link["id"]: link["points"] for link in layout["links"]}
    round_trip = []
    for item in "L04 -L15 L16 L17 L18 -L04".split():
        reverse = item.startswith("-")
        points = routes[item.removeprefix("-")]
        if reverse:
            points = list(reversed(points))
        assert not round_trip or round_trip[-1] == points[0]
        round_trip.extend(points[1:] if round_trip else points)
    routes["undercroft-circuit"] = round_trip
    rows.extend(("POINT", identifier, *point) for identifier, points in routes.items() for point in points)
    rows.extend(("STEP", name) for name in read(SOURCE / "construction.json")["stair_components"])
    rows.extend(("RITUAL", rite["rite"].lower(), OWNERS.index(rite["zone"]), *rite["point"])
                for rite in layout["rituals"])
    for (x0, z0), (x1, z1), normal in WALLS:
        count = math.ceil(math.hypot(x1 - x0, z1 - z0) / .20)
        for i in range(count):
            t = (i + .5) / count
            x = x0 + (x1 - x0) * t
            z = z0 + (z1 - z0) * t
            rows.extend(("WALL", x, y, z, *normal) for y in (-2.38, -2.15, -1.8, -1.45, -1.12, -.82))
    return rows


def landward_checks(construction):
    for x,z in construction["landward_alignment"]:
        yield ("EDGE_LIMIT",x,z)
    spans = list(construction["constructed_spans"])
    spans.append(dict(a=[28.,83.45], b=[36.,83.45], outward=[0.,1.], ground_y_max=0.))
    for span in spans:
        x0, z0 = span["a"]
        x1, z1 = span["b"]
        nx, nz = span["outward"]
        count = max(2, math.ceil(math.hypot(x1-x0, z1-z0)/.30))
        for i in range(count+1):
            t = i/count
            yield ("EDGE", x0+t*(x1-x0), z0+t*(z1-z0), nx, nz, span["ground_y_max"])


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--gpu", action="store_true")
    parser.add_argument("--landward", action="store_true")
    args = parser.parse_args()
    stage = BASE / "wharf-landward-pass" if args.landward else STAGE
    stem = "enclosed-wharf" if args.landward else "tidal-wharf"
    java = Path("C:/Program Files/Java/jdk-17/bin")
    jar = ROOT / "build/beckenburg-game.jar"
    gpu = ROOT / "gpu-client/target/BeckenburgGPU.jar"
    construction = read(stage / "construction.json")
    contract = construction["exports"]
    glb = contract[(stage / f"{stem}.glb").relative_to(ROOT).as_posix()]
    native = contract[(stage / f"{stem}.hcs2").relative_to(ROOT).as_posix()]
    BUILD.mkdir(parents=True, exist_ok=True)
    table = stage / "review-input.tsv"
    rows = inputs()
    if args.landward:
        rows.extend(landward_checks(construction))
    table.write_bytes("".join("\t".join(map(str, row)) + "\n" for row in rows).encode())
    helpers = [ROOT / "tools/java" / name for name in
               ("WharfTideReview.java", "WharfLandwardChecks.java", "NativeSchoolInteriorReview.java", "NativeSchoolDoorReview.java")]
    subprocess.run([str(java / "javac.exe"), "-encoding", "UTF-8", "-cp", str(jar),
                    "-d", str(BUILD), *map(str, helpers)], cwd=ROOT, check=True)
    classpath = os.pathsep.join(map(str, (BUILD, jar, gpu) if args.gpu else (BUILD, jar)))
    subprocess.run([str(java / "java.exe"), "-Djava.awt.headless=true", "-Xmx3g", "-cp", classpath,
                    "org.beckenburg.WharfTideReview", str(stage), str(stage), str(table), glb, native,
                    *(["--gpu"] if args.gpu else []), *(["--landward"] if args.landward else [])], cwd=ROOT, check=True)


if __name__ == "__main__":
    main()
