"""Verify Mourning Marsh directly from the ordinary software or OpenGL archive."""
import argparse
import os
from pathlib import Path
import subprocess
from zipfile import ZipFile

from verify_mine_investigation import CLASSES
from verify_northwatch_named import jdk

ROOT = Path(__file__).resolve().parents[1]
STAGE = ROOT / "development/levels/rebuild/mourning-marsh-native-pass"
BUILD = ROOT / "build/mourning-marsh-package-review"
MEMBERS = ("marsh-review.glb", "marsh-review.hcs2", "runtime-input.tsv", "door-schedule.tsv")


def main():
    parser = argparse.ArgumentParser()
    modes = parser.add_mutually_exclusive_group()
    modes.add_argument("--gpu", action="store_true")
    modes.add_argument("--case", action="store_true")
    args = parser.parse_args()
    archive = ROOT / ("gpu-client/target/BeckenburgGPU.jar" if args.gpu else "build/beckenburg-game.jar")
    with ZipFile(archive) as package:
        for name in MEMBERS:
            if package.read("org/beckenburg/sites/wld-11/" + name) != (STAGE / name).read_bytes():
                raise RuntimeError(f"Archive contains a different Mourning Marsh payload: {name}")
    print("PASS four exact Mourning Marsh archive payloads", flush=True)

    mode = "case" if args.case else "opengl" if args.gpu else "software"
    classes = BUILD / mode / "classes"
    classes.mkdir(parents=True, exist_ok=True)
    main_class = "MourningMarshInvestigationReview" if args.case else "MourningMarshNativeReview"
    dependencies = (*(name for name in CLASSES if name != "MineInvestigationReview"), main_class)
    sources = [str(ROOT / "tools/java" / (name + ".java")) for name in dependencies]
    subprocess.run([jdk("javac"), "--release", "17", "-encoding", "UTF-8", "-cp",
                    str(archive), "-d", str(classes), *sources], cwd=ROOT, check=True)
    cp = os.pathsep.join(map(str, (archive, classes)))
    cmd = [jdk("java"), "-Djava.awt.headless=true", "-Xmx3g"]
    if args.gpu and os.name == "nt":
        native = ROOT / ".lwjgl/3.3.3+5/x64"
        if native.is_dir():
            cmd.append("-Dorg.lwjgl.librarypath=" + str(native))
    output = BUILD / ("case.tsv" if args.case else "opengl" if args.gpu else "software")
    if args.case:
        output.unlink(missing_ok=True)
    cmd.extend(["-cp", cp, "org.beckenburg." + main_class, str(output)])
    if args.gpu:
        cmd.append("--gpu")
    subprocess.run(cmd, cwd=ROOT, check=True, timeout=660)


if __name__ == "__main__":
    main()
