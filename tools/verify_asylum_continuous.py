"""Run the connected hospital case on the exact ordinary Asylum archive."""
import argparse
from hashlib import sha256
import os
import subprocess
from zipfile import ZipFile

from verify_blackwater_named import CLASSES, GAME, GPU, ROOT
from verify_northwatch_named import jdk

PAYLOAD = ROOT / "development/levels/rebuild/asylum-enclosure-pass"
NAMES = ("asylum-stair.glb", "asylum-stair.hcs2", "arrival.tsv", "door-survey.tsv", "mission.tsv", "services.tsv")


def check_archive(path, entry):
    with ZipFile(path) as archive:
        for name in NAMES:
            resource = name if name.startswith("asylum-") else "asylum-" + name
            expected = sha256((PAYLOAD / name).read_bytes()).digest()
            assert sha256(archive.read("org/beckenburg/sites/" + resource)).digest() == expected, name
        assert ("Main-Class: " + entry).encode() in archive.read("META-INF/MANIFEST.MF")
    print("PASS", path.name, "contains all six exact St. Dymphna resources", flush=True)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--gpu", action="store_true")
    args = parser.parse_args()
    check_archive(GAME, "org.beckenburg.InvestigationGame")
    if args.gpu:
        check_archive(GPU, "org.beckenburg.gpu.GpuDesktop")
    classes = ROOT / "build/asylum-continuous" / ("opengl" if args.gpu else "software")
    classes.mkdir(parents=True, exist_ok=True)
    sources = [str(ROOT / "tools/java" / (name + ".java")) for name in
               (*CLASSES, "AsylumContinuousReview", "AsylumEnclosureReview", "MonasteryPerimeterReview")]
    subprocess.run([jdk("javac"), "--release", "17", "-encoding", "UTF-8", "-cp", str(GAME),
                    "-d", str(classes), *sources], cwd=ROOT, check=True)
    cp = os.pathsep.join(map(str, (classes, GPU if args.gpu else GAME)))
    command = [jdk("java"), "-Djava.awt.headless=true", "-Xmx3g"]
    native = ROOT / ".lwjgl/3.3.3+5/x64"
    if args.gpu and os.name == "nt" and native.is_dir():
        command.append("-Dorg.lwjgl.librarypath=" + str(native))
    command += ["-cp", cp]
    enclosure = command + ["org.beckenburg.AsylumEnclosureReview", str(PAYLOAD)]
    if args.gpu:
        enclosure.append("--gpu")
    subprocess.run(enclosure, cwd=ROOT, check=True, timeout=120)
    if not args.gpu:
        subprocess.run(command + ["org.beckenburg.AsylumContinuousReview", str(classes / "case.tsv")],
                       cwd=ROOT, check=True, timeout=540)


if __name__ == "__main__":
    main()
