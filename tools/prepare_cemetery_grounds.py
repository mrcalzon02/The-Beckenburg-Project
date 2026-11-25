"""Paint and save geographic cemetery and mortuary landforms in the editor world."""
import hashlib
import json
from pathlib import Path
import shutil
import subprocess

ROOT = Path(__file__).resolve().parents[1]
BASE = ROOT / "development/levels/rebuild"
PASS = BASE / "cemetery-grounds-pass"
BASELINE = BASE / "college-ground-pass/roster.json"
SITES = ("WLD-07", "WLD-07-MORTUARY")
VAN = ROOT / "build/level-verification/scenes/hollow-creek-canonical-van"
FIELDS = [
    ("Cemetery entrance roadhead", "GRAVEL", 6, [(19,83),(18,72),(20,65),(33,62),(37,67),(38,78),(34,84)]),
    ("Shared chapel and caretaker court", "GRAVEL", 5, [(11,56),(20,56),(25,55),(34,55),(43,56),(46,62),(43,68),(30,70),(17,67),(11,65)]),
    ("Central avenue and burial lawn", "GRASS", 5, [(22,59),(23,50),(23,42),(19,39),(9,39),(7,29),(6,20),(17,19),(24,20),(35,19),(50,19),(49,29),(45,35),(42,39),(34,40),(33,59)]),
    ("Old graves ridge and memorial landing", "GRASS", 5, [(14,19),(14,9),(18,4),(26,1),(33,4),(41,8),(43,16),(39,21),(22,22)]),
]
ANNEX_FIELDS = [
    ("Public mortuary approach court", "GRAVEL", 5, [(33,63),(39,61),(50,61),(57,62),(62,63),(62,70),(54,72),(42,70),(33,68)]),
    ("South receiving yard", "GRAVEL", 5, [(36,76),(44,74),(52,75),(60,76),(72,76),(77,79),(77,84),(67,86),(51,84),(40,83)]),
]


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def tsv(path, rows):
    path.write_bytes("".join("\t".join(map(str,row))+"\n" for row in rows).encode("utf-8"))


def main():
    roster = json.loads(BASELINE.read_bytes())
    jdk = Path("C:/Program Files/Java/jdk-17/bin")
    classes = ROOT / "build/cemetery-earthworks-java"
    classes.mkdir(parents=True,exist_ok=True)
    jar = ROOT / "build/beckenburg-game.jar"
    writer = ROOT / "tools/java/RebuildTieredEarthworks.java"
    subprocess.run([str(jdk/"javac.exe"),"-cp",str(jar),"-d",str(classes),str(writer)],check=True)
    for site in SITES:
        source = next(row for row in roster["active_sources"] if row["id"]==site)
        layout = json.loads((ROOT/source["layout"]).read_bytes())
        old = BASE/site.lower()/"terrain-world"
        world = PASS/"worlds"/site.lower()/"terrain-world"
        program = PASS/"program"/site.lower()
        program.mkdir(parents=True,exist_ok=True)
        shutil.copytree(old,world,dirs_exist_ok=True)
        fields = FIELDS + (ANNEX_FIELDS if site.endswith("MORTUARY") else [])
        tsv(program/"features.tsv",([name,surface,feather,"plane:0:0:0",*(n for pair in polygon for n in pair)]
                                    for name,surface,feather,polygon in fields))
        tsv(program/"protected-rooms.tsv",(z["bounds"] for z in layout["zones"] if z["kind"] not in ("staging","outside")))
        tsv(program/"underlays.tsv",[])
        subprocess.run([str(jdk/"java.exe"),"-Xmx2g","-cp",f"{classes};{jar}","RebuildTieredEarthworks",
                        str(PASS),site,str(old)],cwd=ROOT,check=True)
        for name in ("tile_0_0.png","adaptive-topology.bin","authored-cuts.tsv"):
            assert sha(old/name)==sha(world/name), (site,name)
        receipt = json.loads((world/"earthworks-verification.json").read_bytes())
        assert receipt["disk_reopen_verified"] and sha(world/"terrain-physics.hcs2")==receipt["physics_sha256"]
        provenance = dict(source_world=old.relative_to(ROOT).as_posix(),writer=writer.relative_to(ROOT).as_posix(),
                          writer_sha256=sha(writer),features_sha256=sha(program/"features.tsv"),verification=receipt)
        (world/"source-provenance.json").write_bytes((json.dumps(provenance,indent=2)+"\n").encode())
        (world/"verification.json").write_bytes((world/"earthworks-verification.json").read_bytes())


if __name__=="__main__":
    main()
