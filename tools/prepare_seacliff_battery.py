"""Grade Seacliff Battery's forecourt, terrace grounds and stair formations."""
from pathlib import Path
from shutil import copytree
from subprocess import run

from build_measured_approaches import BASE, ROOT, read, relative, sha, write
from prepare_cemetery_grounds import tsv

SITE = "WLD-17"
PASS = BASE/"seacliff-receiving-pass"
BASELINE = BASE/"flush-porch-and-threshold-pass/roster.json"
OLD_GROUND = BASE/"stair-connection-pass/terrain/worlds/wld-17/terrain-world"

# These are site areas and supported stair enclosures. Eastings and northings
# reserve the full stepped construction, its cheeks, and the surrounding yards.
FIELDS = (
    ("Battery gravel roadhead and muster ground", "GRAVEL", 3.2, "plane:0:0:0",
     ((28,68),(36,68),(37.8,73),(38,88),(35.5,94),(27.5,94),(25.7,89),(26.5,73))),
    ("Upper concrete emplacement courtyard", "GRAVEL", 1.9, "plane:4:0:0",
     ((24,34),(44,34),(44,43),(43,47.5),(42.2,49),(38,49),(34,46.4),(28,45),(24,42))),
    ("Second exterior stair foundation formation", "GRAVEL", 1.75, "plane:15.65:0:-0.25",
     ((38,48),(42,48),(42,56),(38,56))),
    ("Retained intermediate concrete terrace bed", "GRAVEL", 1.55, "plane:1.65:0:0",
     ((30,56),(42,56),(42,58),(30,58))),
    ("First exterior stair foundation formation", "GRAVEL", 2.1, "plane:13.25:0:-0.2",
     ((30,58),(34,58),(34,68),(30,68))),
)


def prepare(target):
    program = target/"program"/SITE.lower()
    world = target/"worlds"/SITE.lower()/"terrain-world"
    program.mkdir(parents=True, exist_ok=True)
    copytree(OLD_GROUND,world,dirs_exist_ok=True)
    row = next(row for row in read(BASELINE)["active_sources"] if row["id"] == SITE)
    layout = read(ROOT/row["layout"])
    tsv(program/"protected-rooms.tsv",(zone["bounds"] for zone in layout["zones"]
                                     if zone["kind"] in ("inside","circulation","sealed")))
    tsv(program/"underlays.tsv",())
    tsv(program/"features.tsv",([name,ground,feather,grade,*(v for pair in points for v in pair)]
                                for name,ground,feather,grade,points in FIELDS))
    classes = ROOT/"build/seacliff-receiving/terrain-classes"
    classes.mkdir(parents=True,exist_ok=True)
    jdk = Path("C:/Program Files/Java/jdk-17/bin")
    jar = ROOT/"build/beckenburg-game.jar"
    tool = ROOT/"tools/java/RebuildTieredEarthworks.java"
    run([str(jdk/"javac.exe"),"-encoding","UTF-8","-cp",str(jar),"-d",str(classes),str(tool)],check=True)
    run([str(jdk/"java.exe"),"-Xmx2g","-cp",f"{classes};{jar}","RebuildTieredEarthworks",
         str(target),SITE,str(OLD_GROUND)],check=True)
    for name in ("tile_0_0.png","adaptive-topology.bin","authored-cuts.tsv"):
        assert sha(world/name)==sha(OLD_GROUND/name),name
    verification = read(world/"earthworks-verification.json")
    assert verification["disk_reopen_verified"] and sha(world/"terrain-physics.hcs2")==verification["physics_sha256"]
    write(world/"source-provenance.json",dict(source_world=relative(OLD_GROUND),terrain_tool=relative(tool),
          terrain_tool_sha256=sha(tool),fields_sha256=sha(program/"features.tsv"),verified=verification))
    write(world/"verification.json",dict(verification,predecessor_receipt=relative(OLD_GROUND/"verification.json"),
          saved_mesh_sha256=sha(world/"scene-package/meshes/site.obj")))


if __name__=="__main__":
    import sys
    prepare((ROOT/Path(sys.argv[1])).resolve() if len(sys.argv)>1 else PASS)
