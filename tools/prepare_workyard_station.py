"""Save the full woodland workyard and station courts on their existing terrain worlds."""
import json
from pathlib import Path
import shutil
import subprocess

from prepare_cemetery_grounds import ROOT, BASE, sha, tsv

PASS = BASE/"workyard-station-pass"
BASELINE = BASE/"estate-parish-pass/roster.json"
CANNERY = "WLD-13"
RAILWAY = "WLD-12"
SITES = (CANNERY, RAILWAY)
SOURCE_WORLDS = {
    CANNERY: BASE/"site-earthworks-pass/worlds/wld-13/terrain-world",
    RAILWAY: BASE/"arrival-grounds-construction-pass/worlds/wld-12/terrain-world",
}
# Full lower receiving bay below the retained trestle deck and its foundation walls.
LOWER_LANDING = (31.25,37.5,33.25,39.25)
FIELDS = {
    CANNERY: (
        ("Woodland workers' court", "GRAVEL", 2.4,
         [(-14.45,31.7),(-11.2,28.9),(-8,28.25),(-8,60.2),
          (-10.1,60.6),(-13.45,57.1),(-14.47,53.1),(-14.47,35.0)]),
    ),
    RAILWAY: (
        ("Station forecourt and van roadhead", "GRAVEL", 4,
         [(16.8,75.95),(29.6,75.95),(35,76),(47.5,76),(47,85),(43.3,88),
          (40.1,91.1),(30,91.1),(27.1,85.5),(20,82.1),(17.2,79.6)]),
        ("Ballast supply court adjoining the siding", "GRAVEL", 3.8,
         [(43.8,74.1),(51,70.6),(57.8,69.85),(61,72),(61.1,75.7),
          (58.65,79.9),(52.2,81.2),(48,82),(43.6,81)]),
    ),
}
# Complete sills and standing areas seat into mineral soil below the surveyed grade.
COURTS = {
    CANNERY: (
        ("Bunkhouse forecourt", [(-11.5,50),(-8,50),(-8,56),(-10.9,56),(-11.5,55.15)], -.64, "forest"),
        ("Mess forecourt", [(-11.5,32),(-8,32),(-8,38),(-11.5,38)], -.69, "forest"),
    ),
    RAILWAY: (
        ("Signal hut south platform", [(16.6,76),(23.4,76),(23.4,78.45),(16.6,78.45)], -.82, "entry"),
    ),
}


def saved_world(site):
    return PASS/"worlds"/site.lower()/"terrain-world"


def program(site):
    return PASS/"program"/site.lower()


def main():
    roster = json.loads(BASELINE.read_bytes())
    java = Path("C:/Program Files/Java/jdk-17/bin")
    jar = ROOT/"build/beckenburg-game.jar"
    classes = ROOT/"build/workyard-station-java"
    classes.mkdir(parents=True, exist_ok=True)
    editor = ROOT/"tools/java/RebuildTieredEarthworks.java"
    excavator = ROOT/"tools/java/RebuildStairCellPunches.java"
    subprocess.run([str(java/"javac.exe"),"-cp",str(jar),"-d",str(classes),str(editor),str(excavator)],check=True)
    for site in SITES:
        source = next(row for row in roster["active_sources"] if row["id"]==site)
        layout = json.loads((ROOT/source["layout"]).read_bytes())
        seed, world, inputs = SOURCE_WORLDS[site], saved_world(site), program(site)
        inputs.mkdir(parents=True, exist_ok=True)
        shutil.copytree(seed,world,dirs_exist_ok=True)
        tsv(inputs/"protected-rooms.tsv", (room["bounds"] for room in layout["zones"]
                                           if room["kind"] in ("inside","circulation","sealed")))
        tsv(inputs/"underlays.tsv", [])
        tsv(inputs/"features.tsv", ([name,mat,feather,"plane:0:0:0",*(v for pt in shape for v in pt)]
                                    for name,mat,feather,shape in FIELDS[site]))
        subprocess.run([str(java/"java.exe"),"-Xmx2g","-cp",f"{classes};{jar}","RebuildTieredEarthworks",
                        str(PASS),site,str(seed)],check=True)
        for retained in ("tile_0_0.png","adaptive-topology.bin","authored-cuts.tsv"):
            assert sha(world/retained)==sha(seed/retained), retained
        grade = json.loads((world/"earthworks-verification.json").read_bytes())
        proof = dict(source_world=seed.relative_to(ROOT).as_posix(), writer=editor.relative_to(ROOT).as_posix(),
                     writer_sha256=sha(editor), grade=grade, native_terrain_sha256=sha(world/"terrain-physics.hcs2"))
        if site==RAILWAY:
            footprint = inputs/"lower-landing-foundations.tsv"
            tsv(footprint,[LOWER_LANDING])
            subprocess.run([str(java/"java.exe"),"-Xmx2g","-cp",f"{classes};{jar}",
                            "RebuildStairCellPunches",str(world),str(footprint)],check=True)
            assert set((seed/"authored-cuts.tsv").read_text().splitlines())<=set((world/"authored-cuts.tsv").read_text().splitlines())
            proof["landing_foundation"] = dict(bounds=LOWER_LANDING, source=excavator.relative_to(ROOT).as_posix(),
                                               source_sha256=sha(excavator),
                                               readback=json.loads((world/"stair-cell-punch-verification.json").read_bytes()))
            proof["native_terrain_sha256"] = sha(world/"terrain-physics.hcs2")
        (world/"source-provenance.json").write_bytes((json.dumps(proof,indent=2)+"\n").encode())
        (world/"verification.json").write_bytes((json.dumps(proof,indent=2)+"\n").encode())


if __name__ == "__main__":
    main()
