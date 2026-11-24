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


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--gpu", action="store_true")
    args = parser.parse_args()
    java = Path("C:/Program Files/Java/jdk-17/bin")
    jar = ROOT / "build/beckenburg-game.jar"
    gpu = ROOT / "gpu-client/target/BeckenburgGPU.jar"
    contract = read(STAGE / "construction.json")["exports"]
    glb = contract[(STAGE / "tidal-wharf.glb").relative_to(ROOT).as_posix()]
    native = contract[(STAGE / "tidal-wharf.hcs2").relative_to(ROOT).as_posix()]
    BUILD.mkdir(parents=True, exist_ok=True)
    table = STAGE / "review-input.tsv"
    table.write_bytes("".join("\t".join(map(str, row)) + "\n" for row in inputs()).encode())
    helpers = [ROOT / "tools/java" / name for name in
               ("WharfTideReview.java", "NativeSchoolInteriorReview.java", "NativeSchoolDoorReview.java")]
    subprocess.run([str(java / "javac.exe"), "-encoding", "UTF-8", "-cp", str(jar),
                    "-d", str(BUILD), *map(str, helpers)], cwd=ROOT, check=True)
    classpath = os.pathsep.join(map(str, (BUILD, jar, gpu) if args.gpu else (BUILD, jar)))
    subprocess.run([str(java / "java.exe"), "-Djava.awt.headless=true", "-Xmx3g", "-cp", classpath,
                    "org.beckenburg.WharfTideReview", str(STAGE), str(STAGE), str(table), glb, native,
                    *(["--gpu"] if args.gpu else [])], cwd=ROOT, check=True)


if __name__ == "__main__":
    main()
