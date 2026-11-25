"""Re-run the measured Wharf plans against the exact support-stage collision."""
import json
import os
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "development/levels/rebuild/native-foyer-door-pass/wld-06"
STAGE = ROOT / "development/levels/rebuild/wharf-foundation-pass"
BUILD = ROOT / "build/wharf-foundation-review"


def read(path):
    return json.loads(path.read_bytes())


def main():
    java = Path("C:/Program Files/Java/jdk-17/bin")
    jar = ROOT / "build/beckenburg-game.jar"
    layout = read(SOURCE / "authored-layout.json")
    contract = read(SOURCE / "construction.json")
    packet = read(STAGE / "construction.json")
    native = STAGE / "supported-wharf.hcs2"
    expected = packet["exports"][native.relative_to(ROOT).as_posix()]
    links = {r["id"]: r["points"] for r in layout["links"]}
    rows = [("LINK", name, *sum(points, [])) for name, points in links.items()]
    chains = {
        "van-to-net-store-via-crossdock": "L01 L04 L07 L08 L09 L12",
        "west-cottage-to-fish-works": "L02-reverse L04 L07 L08 L13",
        "van-undercroft-return-loop": "L01 L04 L15-reverse L16 L17 L18 L04-reverse L01-reverse",
    }
    for name, chain in chains.items():
        points = []
        for link in chain.split():
            key, _, direction = link.partition("-")
            part = list(reversed(links[key])) if direction else links[key]
            assert not points or points[-1] == part[0], (name, link, points[-1], part[0])
            points.extend(part[1:] if points else part)
        rows.append(("WALK", name, *sum(points, [])))
    rows.extend(("DOOR", d["id"], d["axis"], d["boundary"], d["along"], d["clear_width"])
                for d in layout["authored_doorways"])
    BUILD.mkdir(parents=True, exist_ok=True)
    (STAGE / "verification").mkdir(exist_ok=True)
    route_input = BUILD / "routes.tsv"
    stairs = BUILD / "stairs.txt"
    route_input.write_text("".join("\t".join(map(str, row)) + "\n" for row in rows), encoding="utf-8")
    stairs.write_text("\n".join(contract["stair_components"]) + "\n", encoding="utf-8")
    review = ROOT / "tools/java/WharfFoundationReview.java"
    subprocess.run([str(java / "javac.exe"), "-encoding", "UTF-8", "-cp", str(jar),
                    "-d", str(BUILD), str(review)], cwd=ROOT, check=True)
    classpath = os.pathsep.join(map(str, (BUILD, jar)))
    output = STAGE / "verification/physics.tsv"
    subprocess.run([str(java / "java.exe"), "-Djava.awt.headless=true", "-cp", classpath,
                    "org.beckenburg.WharfFoundationReview", str(native), expected, str(stairs),
                    str(route_input), str(output)], cwd=ROOT, check=True)


if __name__ == "__main__":
    main()
