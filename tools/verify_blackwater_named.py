"""Check the ordinary game archive and replay the packaged Blackwater case."""
import argparse
import hashlib
import os
from pathlib import Path
import subprocess
from zipfile import ZipFile

from verify_northwatch_named import jdk

ROOT = Path(__file__).resolve().parents[1]
STAGE = ROOT / "development/levels/rebuild/dam-named-pass"
GAME = ROOT / "build/beckenburg-game.jar"
GPU = ROOT / "gpu-client/target/BeckenburgGPU.jar"
RESOURCE = "org/beckenburg/sites/wld-09/"
CLASSES = (
    "BlackwaterNamedReview", "BlackwaterInvestigationReview", "NativeSchoolInteriorReview", "NativeSchoolDoorReview",
    "CemeteryRefugeReview", "WharfPowerChecks", "NorthwatchDoorRuntime",
    "NorthwatchPowerReview", "NorthwatchDistributionReview", "NorthwatchWaterReview", "NorthwatchFurnishingReview",
)


def check_archive():
    schedule = STAGE / "source-hashes.tsv"
    manifest = schedule.read_bytes()
    with ZipFile(GAME) as archive:
        assert archive.read(RESOURCE + schedule.name) == manifest
        for name, digest in (row.split("\t") for row in manifest.decode().splitlines()):
            assert hashlib.sha256(archive.read(RESOURCE + name)).hexdigest() == digest, name
        assert b"Main-Class: org.beckenburg.InvestigationGame" in archive.read("META-INF/MANIFEST.MF")
    print("PASS ordinary game archive contains all seven Blackwater resources", flush=True)


def main():
    parser = argparse.ArgumentParser()
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument("--gpu", action="store_true")
    mode.add_argument("--investigation", action="store_true")
    args = parser.parse_args()
    check_archive()
    classes = ROOT / "build/blackwater-named" / ("opengl" if args.gpu else "software")
    classes.mkdir(parents=True, exist_ok=True)
    sources = [str(ROOT / "tools/java" / (name + ".java")) for name in CLASSES]
    subprocess.run([jdk("javac"), "--release", "17", "-encoding", "UTF-8", "-cp",
                    str(GAME), "-d", str(classes), *sources], cwd=ROOT, check=True)
    cp = os.pathsep.join(map(str, (classes, GAME, GPU) if args.gpu else (classes, GAME)))
    command = [jdk("java"), "-Djava.awt.headless=true", "-Xmx3g"]
    if args.gpu:
        native = ROOT / ".lwjgl/3.3.3+5/x64"
        if os.name == "nt" and native.is_dir():
            command.append("-Dorg.lwjgl.librarypath=" + str(native))
    if args.investigation:
        report = classes.parent / "investigation.tsv"
        report.unlink(missing_ok=True)
        command.extend(("-cp", cp, "org.beckenburg.BlackwaterInvestigationReview", str(report)))
    else:
        command.extend(("-cp", cp, "org.beckenburg.BlackwaterNamedReview", str(STAGE)))
    if args.gpu:
        command.append("--gpu")
    subprocess.run(command, cwd=ROOT, check=True, timeout=660)


if __name__ == "__main__":
    main()
