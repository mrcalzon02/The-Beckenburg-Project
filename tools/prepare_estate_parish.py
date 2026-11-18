"""Form saved estate, farm, and parish site grades around their standing buildings."""
import json
from pathlib import Path
import shutil
import subprocess

from prepare_cemetery_grounds import ROOT, BASE, VAN, sha, tsv

PASS = BASE / "estate-parish-pass"
BASELINE = BASE / "canyon-marsh-pass/roster.json"
MANSION, FARMHOUSE, CHURCH = "MANSION", "FARMHOUSE", "CHURCH"
SITES = (MANSION, FARMHOUSE, CHURCH)
SOURCE_WORLDS = {
    MANSION: BASE / "terrain-overall-pass/worlds/mansion/terrain-world",
    FARMHOUSE: BASE / "flat-terrain-pass/worlds/farmhouse/terrain-world",
    CHURCH: BASE / "terrain-overall-pass/worlds/church/terrain-world",
}

# Full site landscape features. Bounds follow the terrain survey and building faces.
FIELDS = {
    MANSION: (
        ("Carriage forecourt and drive", "GRAVEL", 4,
         [(9,50),(43,50),(53,51),(53,54),(44,57),(40,64),(36,76),(19,77),(15,71),(16,61),(10,55)]),
        ("West library lawn", "DIRT", 3, [(4,20),(12,20),(12,53),(10,55),(5,52),(4,38)]),
        ("Receiving court and east terraced garden", "GRAVEL", 3,
         [(39,-1),(48,-1),(50,4),(52,9),(56,11),(58,20),(57.8,50),(55,54),
          (46,54),(46,28),(43,20),(43,14),(39,14)]),
    ),
    FARMHOUSE: (
        ("Farm roadhead and gravel workyard", "GRAVEL", 4,
         [(10,27),(21,27),(25,25),(28,25),(32,31),(43,31),(43,36),(39,39),
          (38,50),(14,50),(13,42),(14,37),(10,34)]),
        ("Kitchen garden and west paddock", "DIRT", 3,
         [(8,6),(22,6),(22,12),(12,14),(12,30),(15,33),(6.5,35),(6,15)]),
        ("Barn courts and tractor turning yard", "GRAVEL", 4,
         [(26.2,5.8),(42,5.8),(46.5,8),(46.5,34),(42,36),(37,33),(29,27),(26.2,25),(26.2,9)]),
    ),
    CHURCH: (
        ("Parish forecourt and roadhead", "GRAVEL", 4,
         [(8,38),(18,34),(32,34),(35,40),(35,48),(33,57),(12,58),(8,50)]),
        ("East churchyard and sacristy approach", "DIRT", 3,
         [(18,2.5),(32.5,2.5),(36.5,6),(38,13),(38,32),(34,37),(28,39),
          (26,34),(30,28),(30,10),(18,7)]),
    ),
}

# Solid masonry where the original terrain ends exactly at a cut or is too narrow.
# These are full terraces and foresteps attached to the entrances they serve.
PAVEMENTS = {
    MANSION: (
        ("Service stair landing and north receiving court",
         [(39.8,0.1),(48.7,0.1),(49.4,3.6),(49.4,11),(51.6,14),(54.8,14),(54.8,20),
          (43,20),(43,17),(46,17),(46,2),(39.8,2)], -1.12, "garden"),
        ("East garden and ballroom retaining terrace",
         [(54.8,14.15),(56.85,14.15),(57.1,21),(57.1,50.85),(55.5,52.7),(50.35,52.7),
          (50.35,50),(54,50),(54,30),(53.05,29),(53.05,20),(54.8,20)], -1.5, "garden"),
    ),
    FARMHOUSE: (
        ("Mudroom stoop", [(20.55,28),(25.45,28),(25.45,31.32),(20.55,31.32)], -1.22, "grounds"),
        ("Workshop loading apron", [(27,33),(39,33),(39,35.88),(27,35.88)], -1.02, "grounds"),
        ("Barn north loading apron", [(32,6.75),(41.55,6.75),(41.55,9),(32,9)], -.88, "grounds"),
        ("Barn south working court", [(30.95,24),(43.15,24),(43.15,26),(31,26)], -.92, "grounds"),
    ),
    CHURCH: (
        ("Vestry court and east shoulder",
         [(26.45,3.4),(33.8,3.4),(35.8,5.45),(36.55,7.25),(36.55,11.7),
          (34,11.7),(34,6),(26.45,6)], -.92, "grounds"),
        ("Chapel garden sill", [(34,18.1),(36.55,18.1),(36.55,22),(34,22)], -1.08, "grounds"),
        ("Parish front porch", [(18,39),(26,39),(26,41.4),(18,41.4)], -.82, "entry"),
        ("Crypt exterior court", [(34,29),(37,29),(37,33.35),(34.65,36.9),(27,38.2),
                                  (26,37.8),(26,34),(34,34)], -1.05, "grounds"),
    ),
}


def saved_world(site):
    return PASS / "worlds" / site.lower() / "terrain-world"


def program(site):
    return PASS / "program" / site.lower()


def main():
    roster = json.loads(BASELINE.read_bytes())
    java = Path("C:/Program Files/Java/jdk-17/bin")
    jar = ROOT / "build/beckenburg-game.jar"
    classes = ROOT / "build/estate-parish-java"
    classes.mkdir(parents=True, exist_ok=True)
    sources = [ROOT / "tools/java/RebuildTieredEarthworks.java"]
    subprocess.run([str(java/"javac.exe"), "-cp", str(jar), "-d", str(classes), *map(str, sources)], check=True)
    for site in SITES:
        row = next(row for row in roster["active_sources"] if row["id"] == site)
        layout = json.loads((ROOT / row["layout"]).read_bytes())
        seed, world, inputs = SOURCE_WORLDS[site], saved_world(site), program(site)
        inputs.mkdir(parents=True, exist_ok=True)
        layer_path = Path("terrain-layers/tile_0_0.bin")
        if not (seed / layer_path).exists():
            (world / layer_path).unlink(missing_ok=True)
        shutil.copytree(seed, world, dirs_exist_ok=True)
        tsv(inputs/"protected-rooms.tsv",
            (z["bounds"] for z in layout["zones"] if z["kind"] in ("inside","circulation","sealed")))
        tsv(inputs/"underlays.tsv", [])
        tsv(inputs/"features.tsv", ([name, material, blend, "plane:0:0:0", *(v for p in boundary for v in p)]
                                    for name, material, blend, boundary in FIELDS[site]))
        subprocess.run([str(java/"java.exe"), "-Xmx2g", "-cp", f"{classes};{jar}", "RebuildTieredEarthworks",
                        str(PASS), site, str(seed)], check=True)
        for retained in ("tile_0_0.png", "adaptive-topology.bin", "authored-cuts.tsv"):
            assert sha(world/retained) == sha(seed/retained), retained
        grade = json.loads((world/"earthworks-verification.json").read_bytes())
        record = dict(source_world=seed.relative_to(ROOT).as_posix(), grade=grade,
                      builder_sha256={p.name: sha(p) for p in sources},
                      terrain_sha256=sha(world/"terrain-physics.hcs2"))
        for name in ("verification.json", "source-provenance.json"):
            (world/name).write_bytes((json.dumps(record, indent=2)+"\n").encode())


if __name__ == "__main__":
    main()
