"""Save surveyed public, cloister garden and service terraces in the editor world."""
import json
from pathlib import Path
import shutil
import subprocess

from prepare_cemetery_grounds import ROOT, BASE, VAN, sha, tsv

SITE = "WLD-10"
PASS = BASE / "monastery-cloister-pass"
PROGRAM = PASS / "program/wld-10"
OLD_WORLD = BASE / "flat-terrain-pass/worlds/wld-10/terrain-world"
WORLD = PASS / "worlds/wld-10/terrain-world"
BASELINE = BASE / "mine-yard-pass/roster.json"

# Boundaries describe areas of use, including the entire enclosed garden.
# The public terrace opens out where vehicles turn. Stone floors inside the
# ranges stay protected, and the saved catacomb terrain cells stay open.
FIELDS = (
    ("Arrival roadhead and gate forecourt", "GRAVEL", 7, 0,
     [(19, 87), (19, 77), (21, 67), (24, 62), (40, 62), (44, 67), (44, 82), (41, 88), (28, 88)]),
    ("Enclosed cloister garden", "GRAVEL", 3, 0,
     [(14, 22), (44, 22), (44, 50), (14, 50)]),
    ("Bell tower and refectory service court", "GRAVEL", 6, 0,
     [(60, 16), (65, 16), (68, 18), (70, 22), (70, 27), (68, 30), (60, 30)]),
)


def main():
    PROGRAM.mkdir(parents=True, exist_ok=True)
    shutil.copytree(OLD_WORLD, WORLD, dirs_exist_ok=True)
    roster = json.loads(BASELINE.read_bytes())
    row = next(row for row in roster["active_sources"] if row["id"] == SITE)
    layout = json.loads((ROOT / row["layout"]).read_bytes())
    tsv(PROGRAM / "protected-rooms.tsv",
        (zone["bounds"] for zone in layout["zones"] if zone["y"] == 0 and zone["kind"] == "inside"))
    tsv(PROGRAM / "underlays.tsv", [])
    tsv(PROGRAM / "features.tsv",
        ([name, material, feather, f"plane:{height}:0:0", *(v for point in boundary for v in point)]
         for name, material, feather, height, boundary in FIELDS))
    jdk = Path("C:/Program Files/Java/jdk-17/bin")
    jar = ROOT / "build/beckenburg-game.jar"
    classes = ROOT / "build/monastery-cloister-java"
    classes.mkdir(parents=True, exist_ok=True)
    builder = ROOT / "tools/java/RebuildTieredEarthworks.java"
    subprocess.run([str(jdk / "javac.exe"), "-cp", str(jar), "-d", str(classes), str(builder)], check=True)
    subprocess.run([str(jdk / "java.exe"), "-Xmx2g", "-cp", f"{classes};{jar}",
                    "RebuildTieredEarthworks", str(PASS), SITE, str(OLD_WORLD)], check=True)
    unchanged = ("tile_0_0.png", "adaptive-topology.bin", "authored-cuts.tsv")
    assert all(sha(WORLD / name) == sha(OLD_WORLD / name) for name in unchanged)
    receipt = dict(source_world=OLD_WORLD.relative_to(ROOT).as_posix(),
                   builder=builder.relative_to(ROOT).as_posix(), builder_sha256=sha(builder),
                   protected_files_sha256={name: sha(WORLD / name) for name in unchanged},
                   grade=json.loads((WORLD / "earthworks-verification.json").read_bytes()),
                   terrain_sha256=sha(WORLD / "terrain-physics.hcs2"))
    for name in ("verification.json", "source-provenance.json"):
        (WORLD / name).write_bytes((json.dumps(receipt, indent=2) + "\n").encode())


if __name__ == "__main__":
    main()
