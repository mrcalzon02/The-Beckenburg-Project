"""Stage the measured Lookout spaces, doors and approach guides for native review."""
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "development/levels/rebuild/lookout-landing-pass/wld-04"
OUT = ROOT / "development/levels/rebuild/lookout-native-arrival-pass"

# The whole-site contract has fourteen investigation owners and owner 14 is
# still the expedition van. Additional slabs use their adjoining site owner.
OWNERS = {
    "lower": (0, 14.6, 0, 61.35), "bend": (1, 28, 3, 36.10), "clearing": (2, 30, 5.65, 22),
    "hut": (3, 12, 6, 20), "ruin": (4, 47, 6, 22), "store": (5, 45, 6, 35),
    "return": (6, 43, 3, 51), "foot": (7, 30.8, 6, 15), "landing": (8, 29.5, 10.5, 15),
    "upper-turn-platform": (9, 28.2, 12.75, 9.1), "cab": (10, 27, 15, 5),
    "deck": (11, 33, 15, 5), "cab-access-gallery": (12, 25.5, 15, 12), "entry": (13, 28, 0, 66),
}

# Whole deck and apron bounds, including stacked half-flights and both real
# landings alongside the cab. These volumes provide ownership only. Native
# floors and solid collision determine the supported points within them.
EXTERIOR = (
    ("entry", 13, 19.5, 58, 49.5, 72, 0, 3),
    ("lower", 0, 8, 42, 24, 65, 0, 3.2),
    ("bend", 1, 18.7, 30, 36, 54, 3, 3),
    ("clearing", 2, 20, 14, 40, 32, 5.65, 3),
    ("return", 6, 36, 41.5, 50, 54, 3, 3.1),
    ("foot", 7, 24, 8.2, 36, 16.9, 6, 4.5),
    ("landing", 8, 27.3, 8.2, 31.7, 16, 10.5, 2.25),
    ("upper-turn-platform", 9, 27.3, 8.2, 31.7, 13.9, 12.75, 2.25),
    ("cab-access-gallery", 12, 24, 8, 27.3, 16, 15, 2.8),
    ("top-stair-landing", 12, 27.3, 13.9, 29.1, 16, 15, 2.8),
    ("ruin-porch", 2, 38.2, 20.65, 40, 23.35, 6, 2.8),
    ("store-stoop", 6, 43.75, 40, 46.25, 46.3, 6, 2.8),
    ("east-return-flight", 6, 41.75, 51.7, 44.25, 59.5, 3, 2.6),
    ("east-return-lower-flight", 6, 41.75, 53.2, 44.25, 59.5, 0, 2.7),
    ("stairs-west-flight", 12, 27.3, 10, 29.1, 13.9, 14, 2.2),
    ("hut-porch", 2, 20, 18.8, 21.8, 21.2, 6, 2.8),
    ("hut-south-porch", 2, 10.8, 26, 13.2, 27.8, 6, 2.8),
    ("hut-south-shelf", 2, 10.7, 26.65, 24.5, 27.82, 5.65, 2.8),
    ("hut-south-stair-foot", 2, 10.7, 27.8, 14.4, 29, 5.65, 2.8),
)

# Coincident sill and first-riser stations can select different support when
# approached in opposite directions. Put each lower station on the adjacent
# landing before the riser. These corrections never alter physical geometry.
NOSING_STATIONS = {
    (20, 0, 58): (20, 0, 58.10),
    (28, 3, 36): (28, 3, 36.10),
    (22.6, 5.65, 20): (22.70, 5.65, 20),
    (37.4, 5.65, 22): (37.30, 5.65, 22),
    (45, 3, 46.3): (45, 3, 46.40),
    (43, 0, 58): (43, 0, 58.10),
    (30.8, 5.650079727172852, 16.8): (30.8, 5.65, 16.90),
    (30.8, 6, 13.9): (30.8, 6, 14.00),
    (28.2, 8.25, 10): (28.2, 8.25, 9.90),
    (30.8, 10.5, 13.9): (30.8, 10.5, 14.00),
    (28.2, 12.75, 10): (28.2, 12.75, 9.90),
}

# Retained route owners connect only at the actual graded or constructed
# interfaces. Door owners are assigned independently by the fitted wall.
TRANSITIONS = (
    (610, 13, 0, 24, 0, 64.774, 1.4, "x", "trail"),
    (611, 0, 1, 20, 3, 53.2, 1.4, "z", "stair"),
    (612, 1, 2, 28, 5.65, 31.2, 1.4, "z", "stair"),
    (614, 6, 13, 43, 0, 59.5, 1.4, "z", "trail"),
    (615, 2, 7, 30.8, 6, 16, 1.4, "z", "stair"),
    (616, 7, 8, 28.2, 10.5, 13.9, 1.4, "z", "stair"),
    (617, 8, 9, 30.8, 12.75, 10, 1.4, "z", "stair"),
    (618, 9, 12, 28.2, 15, 13.9, 1.4, "z", "stair"),
)

DOORS = (
    ("WLD-04-restart-cab south", "cab", 10, "cab-access-gallery", 12, 1, -1),
    ("WLD-04-restart-cab east", "cab", 10, "deck", 11, 1, -1),
    ("WLD-04-store-south-normal", "store", 5, "store-stoop", 6, 2, -1),
    ("WLD-04-ruin-west-22", "ruin", 4, "ruin-porch", 2, 2, 1),
    ("WLD-04-ruin-store-45", "ruin", 4, "store", 5, 2, 1),
    ("WLD-04-hut-east-20", "hut", 3, "hut-porch", 2, 2, -1),
    ("WLD-04-hut-south-12", "hut", 3, "hut-south-porch", 2, 2, -1),
)


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def table(path, rows):
    path.write_bytes("".join("\t".join(map(str, row)) + "\n" for row in rows).encode())


def build(out=OUT):
    layout = json.loads((SOURCE / "authored-layout.json").read_bytes())
    contract = json.loads((SOURCE / "construction.json").read_bytes())
    model, physics = (SOURCE / filename for filename in ("lookout-landings.glb", "lookout-landings.hcs2"))
    assert sha(model) == contract["glb_sha256"] and sha(physics) == contract["physics_sha256"]
    assert sha(SOURCE / "authored-layout.json") == contract["layout_sha256"]
    assert sha(SOURCE / "lookout-landings.blend") == contract["blend_sha256"]
    staging = layout["staging"]
    rows = [("SOURCE", model.relative_to(ROOT).as_posix(), sha(model), physics.relative_to(ROOT).as_posix(), sha(physics)),
            ("DOCK", *staging["point"], staging["yaw"])]
    exterior = {row[0] for row in EXTERIOR}
    for zone in layout["zones"]:
        key = zone["id"]
        if key not in exterior:
            rows.append(("SPACE", key, 13 if key == "stage" else OWNERS[key][0], *zone["bounds"], zone["y"],
                         zone["height"], str(zone["kind"] != "inside").lower(), "true"))
    rows.extend(("SPACE", *row, "true", "true") for row in EXTERIOR)
    rows.extend(("TREAD", name) for name in contract["stair_components"])
    for rite in layout["rituals"]:
        x, y, z = rite["point"]
        if rite["zone"] == "clearing":
            y = OWNERS["clearing"][2]
        rows.append(("RITUAL", rite["rite"].lower(), OWNERS[rite["zone"]][0], x, y, z))
    for link in layout["links"]:
        rows.extend(("POINT", link["id"], *NOSING_STATIONS.get(tuple(point), point)) for point in link["points"])
    # The alternate hut return follows the actual south stair and the level
    # terrain band close to the building. It stays clear of the berm southward.
    rows.extend(("POINT", "L05S", *point) for point in (
        (12, 6, 20), (12, 6, 26), (12, 6, 27.8), (12, 5.624, 28.84),
        (14, 5.624, 28.84), (14, 5.65, 27.25), (24, 5.65, 27.25),
        (24, 5.65, 22), (30, 5.65, 22)))
    entries = [("ANCHOR", owner, key, x, y, z) for key, (owner, x, y, z) in OWNERS.items()]
    entries.extend(("TRANSITION", *row) for row in TRANSITIONS)
    apertures = {frame["id"]: frame for frame in layout["authored_doorways"]}
    # Surveyed in the unchanged source: Header hut E runs from Z19 to 21
    # at Y8.45, and the jambs are on the two ends of the east wall.
    apertures["WLD-04-hut-east-20"] = dict(axis="x", boundary=20, along=20,
        sill_y=6, clear_width=2, clear_height=2.45, frame_span=(19.92, 20.08))
    # Bayed shell Wall hut S still has its own intact south doorway and sill.
    apertures["WLD-04-hut-south-12"] = dict(axis="z", boundary=26, along=12,
        sill_y=6, clear_width=2.04, clear_height=2.2, frame_span=(25.92, 26.08))
    asset = sha(ROOT / "assets/runtime/models/door_1_loose.glb")
    doors = []
    for pid, (key, a, owner_a, b, owner_b, leaves, swing) in enumerate(DOORS, 600):
        aperture = apertures[key]
        axis = aperture["axis"]
        x, z = (aperture["along"], aperture["boundary"]) if axis == "z" else (aperture["boundary"], aperture["along"])
        doors.append((pid, key, a, b, owner_a, owner_b, x, aperture["sill_y"], z, aperture["clear_width"],
                      axis, *aperture["frame_span"], aperture["clear_height"], leaves, 1, swing, asset))
    out.mkdir(exist_ok=True, parents=True)
    table(out / "arrival.tsv", rows)
    table(out / "door-survey.tsv", doors)
    table(out / "mission.tsv", entries)


if __name__ == "__main__":
    build()
