"""Grade the lighthouse roadhead, keeper forecourt, and oil-house yard in the saved editor world."""
import json
from pathlib import Path
import shutil
import subprocess

from prepare_cemetery_grounds import ROOT, BASE, VAN, sha, tsv

PASS = BASE / "coast-entrances-pass"
BASELINE = BASE / "monastery-cloister-pass/roster.json"
OLD_WORLD = BASE / "terrain-occupancy-pass/worlds/wld-14/terrain-world"
WORLD = PASS / "worlds/wld-14/terrain-world"
PROGRAM = PASS / "program/wld-14"
FIELDS = (
    ("Keeper roadhead turn and forecourt", "GRAVEL", 6,
     [(19,86),(19,77),(21,69),(24,62),(23,54),(42,54),(45,59),(46,65),(42,71),(43,80),(40,89),(25,91)]),
    ("Oil-house working court", "GRAVEL", 5,
     [(37,54),(42,54),(48,52),(48,46),(58,46),(62,48),(63,53),(60,58),(58,61),(48,63),(42,61)]),
)


def main():
    PROGRAM.mkdir(parents=True, exist_ok=True)
    shutil.copytree(OLD_WORLD, WORLD, dirs_exist_ok=True)
    roster = json.loads(BASELINE.read_bytes())
    row = next(row for row in roster["active_sources"] if row["id"] == "WLD-14")
    layout = json.loads((ROOT / row["layout"]).read_bytes())
    tsv(PROGRAM / "protected-rooms.tsv",
        (z["bounds"] for z in layout["zones"] if z["y"] == 0 and z["kind"] in {"inside","circulation"}))
    tsv(PROGRAM / "underlays.tsv", [])
    tsv(PROGRAM / "features.tsv",
        ([name, mat, feather, "plane:0:0:0", *(v for point in boundary for v in point)]
         for name, mat, feather, boundary in FIELDS))
    jdk = Path("C:/Program Files/Java/jdk-17/bin")
    jar = ROOT / "build/beckenburg-game.jar"
    classes = ROOT / "build/coast-yard-java"
    classes.mkdir(parents=True, exist_ok=True)
    builder = ROOT / "tools/java/RebuildTieredEarthworks.java"
    subprocess.run([str(jdk/"javac.exe"),"-cp",str(jar),"-d",str(classes),str(builder)],check=True)
    subprocess.run([str(jdk/"java.exe"),"-Xmx2g","-cp",f"{classes};{jar}",
                    "RebuildTieredEarthworks",str(PASS),"WLD-14",str(OLD_WORLD)],check=True)
    files = ("tile_0_0.png","adaptive-topology.bin","authored-cuts.tsv")
    assert all(sha(WORLD/name)==sha(OLD_WORLD/name) for name in files)
    record = dict(source_world=OLD_WORLD.relative_to(ROOT).as_posix(),
                  preserved_sha256={name:sha(WORLD/name) for name in files},
                  builder_sha256=sha(builder),
                  terrain=json.loads((WORLD/"earthworks-verification.json").read_bytes()))
    for name in ("verification.json","source-provenance.json"):
        (WORLD/name).write_bytes((json.dumps(record,indent=2)+"\n").encode())


if __name__ == "__main__":
    main()
