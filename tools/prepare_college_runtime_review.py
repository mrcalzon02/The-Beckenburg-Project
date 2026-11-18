"""Prepare a one-run review manifest from the exact inspected College sources."""
import argparse
import hashlib
import json
from pathlib import Path

PACKET = Path("development/levels/rebuild/college-ground-pass/college")
# Standing points in the middle of the named architectural compartments.
ANCHORS = [
    ("teaching-hall", 22, 32), ("admin", 32, 50), ("seminar", 13, 29),
    ("lab", 11, 16), ("cafe", 46, 58), ("lecture", 13, 44), ("issue", 31, 18),
    ("bridge-lobby", 44, 18), ("upper-library-landing", 31, 18),
    ("upper-res-hall", 52, 44), ("upper-teaching", 22, 32),
    ("maintenance", 16, 29), ("green", 38, 40), ("entry", 38, 84),
]
# Apertures surveyed on the source walls, with tangential width and opening axis.
# Keep the alternate multi-room paths in the route audit, not the portal graph.
OPENINGS = [
    ("L05", "teaching-hall", "lecture", 20, 0, 44, 1.4, "x"),
    ("L06", "teaching-hall", "seminar", 20, 0, 28, 1.4, "x"),
    ("L07", "teaching-hall", "lab-entry", 20, 0, 23, 1.4, "x"),
    ("L14", "teaching-lobby", "meeting", 28, 0, 56, 1.4, "x"),
    ("L52", "upper-library-landing", "bridge-lobby", 34, 3.6, 18, 1.4, "x"),
    ("L86", "upper-teaching", "teaching-library-link", 24, 3.6, 14, 1.4, "x"),
]
STAIR_LINKS = ("L80", "L81", "L82", "L84")


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("output", type=Path)
    parser.add_argument("--electrical", type=Path, help="Native College installation directory")
    args = parser.parse_args()
    path = args.output
    layout_file = PACKET / "authored-layout.json"
    layout = json.loads(layout_file.read_bytes())
    construction = json.loads((PACKET / "construction.json").read_bytes())
    assert digest(layout_file) == construction["layout_sha256"]
    assert digest(PACKET / "campus-grounds.glb") == construction["glb_sha256"]
    assert digest(PACKET / "campus-grounds.hcs2") == construction["physics_sha256"]
    zones = {space["id"]: space for space in layout["zones"]}
    links = {link["id"]: link for link in layout["links"] if link["player_access"] == "OPEN_ROUTE"}
    rows = []

    def row(*columns):
        rows.append("\t".join(str(value) for value in columns))

    electrical = None
    render_sha, physics_sha = construction["glb_sha256"], construction["physics_sha256"]
    if args.electrical:
        electrical = json.loads((args.electrical / "electrical.json").read_bytes())
        assert electrical["layout_sha256"] == construction["layout_sha256"]
        assert electrical["baseline_glb_sha256"] == render_sha
        assert electrical["baseline_physics_sha256"] == physics_sha
        render_sha = digest(args.electrical / "college-electrical.glb")
        physics_sha = digest(args.electrical / "college-electrical.hcs2")
        assert render_sha == electrical["glb_sha256"] and physics_sha == electrical["physics_sha256"]
    row("SOURCE", render_sha, physics_sha)
    staging = layout["staging"]
    row("DOCK", *staging["point"], staging["yaw"])
    for zone in zones.values():
        # Forecourt ownership is exterior. The canonical cargo alone is owner 14.
        owner = 13 if zone["kind"] == "staging" else zone["owner"]
        row("SPACE", zone["id"], owner, *zone["bounds"], zone["y"], zone["height"],
            int(zone["kind"] in ("outside", "staging")), int(zone["gameplay_eligible"]))
    for number, (name, x, z) in enumerate(ANCHORS):
        zone = zones[name]
        assert zone["owner"] == number and zone["gameplay_eligible"]
        row("ANCHOR", number, zone["name"], name, x, zone["y"], z)
    for ritual in layout["rituals"]:
        row("RITUAL", ritual["rite"].lower(), zones[ritual["zone"]]["owner"], *ritual["point"])
    for name in construction["stair_components"]:
        row("TREAD", name)
    for door in construction["new_portals"]:
        x, z = (door["along"], door["boundary"]) if door["axis"] == "z" else (door["boundary"], door["along"])
        row("PORTAL", door["id"], *door["rooms"], x, door["sill_y"], z, door["clear_width"], door["axis"], "opening")
    for opening in OPENINGS:
        row("PORTAL", *opening, "opening")
    row("PORTAL", "L02", "entry", "green", 38, 0, 78, 1.4, "z", "trail")
    for key in STAIR_LINKS:
        link = links[key]
        row("PORTAL", key, link["a"], link["b"], *link["points"][0], link["width"], "z", "stair")
    for key, link in links.items():
        row("ROUTE", key, link["kind"], *(value for point in link["points"] for value in point))
    if electrical:
        for socket in electrical["sockets"]:
            row("SOCKET", socket["id"], socket["type"], socket["room"],
                *socket["position"], socket["yaw"], socket["style"])
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(("\n".join(rows) + "\n").encode("utf-8"))


if __name__ == "__main__":
    main()
