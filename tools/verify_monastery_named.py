"""Verify the original Monastery resources in ordinary software and OpenGL archives."""
import argparse
from hashlib import sha256
import os
from pathlib import Path
import subprocess
from zipfile import ZipFile

from verify_blackwater_named import CLASSES as SHARED_REVIEW_CLASSES
from verify_northwatch_named import jdk

ROOT = Path(__file__).resolve().parents[1]
STAGE = ROOT / "development/levels/rebuild/monastery-court-drainage-pass"
GAME = ROOT / "build/beckenburg-game.jar"
GPU = ROOT / "gpu-client/target/BeckenburgGPU.jar"
RESOURCE = "org/beckenburg/sites/wld-10/"
CLASSES = (*SHARED_REVIEW_CLASSES, "MonasteryNamedReview", "MonasteryPowerReview", "MonasteryDoorReview")


def verify_archive(path, main_class):
    manifest = (STAGE / "source-hashes.tsv").read_bytes()
    entries = dict(row.split("\t") for row in manifest.decode("utf-8").splitlines())
    assert len(entries) == 5
    with ZipFile(path) as archive:
        assert archive.read(RESOURCE + "source-hashes.tsv") == manifest
        for name, digest in entries.items():
            assert sha256(archive.read(RESOURCE + name)).hexdigest() == digest, name
        assert ("Main-Class: " + main_class).encode() in archive.read("META-INF/MANIFEST.MF")
    print("PASS", path.name, "contains the exact Monastery geometry, door, room and power resources", flush=True)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--gpu", action="store_true")
    args = parser.parse_args()
    verify_archive(GAME, "org.beckenburg.InvestigationGame")
    if args.gpu:
        verify_archive(GPU, "org.beckenburg.gpu.GpuDesktop")
    classes = ROOT / "build/monastery-named" / ("opengl" if args.gpu else "software")
    classes.mkdir(parents=True, exist_ok=True)
    sources = [str(ROOT / "tools/java" / (name + ".java")) for name in CLASSES]
    subprocess.run([jdk("javac"), "--release", "17", "-encoding", "UTF-8", "-cp",
                    str(GAME), "-d", str(classes), *sources], cwd=ROOT, check=True)
    archive = GPU if args.gpu else GAME
    cp = os.pathsep.join(map(str, (classes, archive)))
    command = [jdk("java"), "-Djava.awt.headless=true", "-Xmx3g"]
    if args.gpu:
        native = ROOT / ".lwjgl/3.3.3+5/x64"
        if os.name == "nt" and native.is_dir():
            command.append("-Dorg.lwjgl.librarypath=" + str(native))
    command.extend(("-cp", cp, "org.beckenburg.MonasteryNamedReview", str(STAGE)))
    if args.gpu:
        command.append("--gpu")
    subprocess.run(command, cwd=ROOT, check=True, timeout=120 if args.gpu else 480)


if __name__ == "__main__":
    main()
