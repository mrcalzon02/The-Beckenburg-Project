"""Bind the actual cemetery frames, site spaces and open landings into a staged room graph."""
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BASE = ROOT / "development/levels/rebuild"
BOUNDARY = BASE / "cemetery-boundary-pass"
OUTPUT = BASE / "cemetery-door-pass"
SOURCE_LEAF = ROOT / "assets/runtime/models/door_1_loose.glb"

COMMON = {
    "chapel": 0, "cottage": 1, "tool": 2, "west-stair": 3, "east-stair": 4,
    "ossuary": 5, "chamber": 6, "entry": 12, "stage": 13,
}
BASE_OWNERS = {"garden": 7, "east-graves": 8, "old-graves": 9, "avenue": 10, "chapel-court": 11}
ANNEX_OWNERS = {
    "garden": 7, "east-graves": 7, "old-graves": 7, "avenue": 7,
    "mort-hall": 8, "mort-reception": 8, "mort-viewing": 9, "mort-bay": 10,
    "mort-transfer": 10, "mort-office": 10, "mort-prep": 11, "mort-cold": 11, "receiving-yard": 13,
}
COMMON_ANCHORS = {
    "chapel": (15, 0, 47), "cottage": (39, 0, 47), "tool": (50, 0, 47),
    "west-stair": (7.5, 0, 16), "east-stair": (48.5, 0, 16), "ossuary": (28, -3, 16),
    "chamber": (22, -3, 6), "entry": (28, 0, 66), "stage": (25, 0, 74)
}
BASE_ANCHORS = {
    "garden": (15, 0, 29), "east-graves": (41, 0, 29), "old-graves": (28, 0, 12),
    "avenue": (28, 0, 40), "chapel-court": (15, 0, 57.12),
}
ANNEX_ANCHORS = {
    "avenue": (28, 0, 40), "mort-reception": (66, 0, 59), "mort-viewing": (66, 0, 47),
    "mort-bay": (66, 0, 70), "mort-cold": (78, 0, 54),
}
SWINGS = {
    "chapel S 1": (2, 1, -1),
    "chapel N 1": (2, 1, 1),
    "cottage S 1": (2, 1, -1),
    "cottage N 1": (2, 1, 1),
    "west-stair S 1": (2, 1, 1),
    "east-stair S 1": (2, 1, 1),
    "caretaker tool-room partition 1": (1, -1, 1),
    "visitor hall south entry 1": (2, 1, -1),
    "visitor hall shared wall 1": (1, 1, 1),
    "visitor hall shared wall 2": (1, -1, 1),
    "transfer west wall 1": (1, 1, -1),
    "transfer west wall 2": (1, -1, -1),
    "cold storage service face 1": (1, 1, -1),
    "staff office enclosure 1": (1, -1, 1),
    "vehicle receiving door 1": (2, 1, 1),
    "viewing and reception": (1, -1, -1),
    "cold storage and preparation": (1, 1, -1),
}


def load(path):
    return json.loads(path.read_bytes())


def write_tsv(path, rows):
    path.write_bytes("".join("\t".join(map(str, row))+"\n" for row in rows).encode())


def room_rows(layout, owners, anchors, annex, stairs):
    rows = []
    for zone in layout["zones"]:
        name = zone["id"]
        bounds = tuple(zone["bounds"])
        y = zone["y"]
        height = zone["height"]
        outside = zone["kind"] in ("outside", "staging")
        if name == "entry":
            # Both frontages open on the built court. The annex front court
            # continues around the visitor entrance, clear of the service bay.
            bounds = (5 if annex else 20, 55, 60 if annex else 49, 72)
        elif name == "stage":
            bounds = (13, 72, 60 if annex else 43, 86)
        elif name == "east-graves":
            bounds = (32, 20, 51, 38)
        elif name == "ossuary":
            # The main vault starts where the two supported flights land.
            bounds = (14, 12, 42, 20)
            height = 2.85
        rows.append(("SPACE", name, owners[name], *bounds, y, height, str(outside).lower(), zone["name"]))
    if not annex:
        rows.append(("SPACE", "chapel-court", 11, 5, 55, 20, 69, 0, 3, "true", "Chapel forecourt"))
    # The same pavilion owner persists on its actual descent to the vault.
    for name, bounds in (("west-stair", (8.5, 14.9, 14, 17.1)), ("east-stair", (42, 14.9, 47.5, 17.1))):
        rows.append(("SPACE", name+"-flight", owners[name], *bounds, -3, 2.92, "false", name+" lower flight"))
    rows.extend(("ANCHOR", owners[name], name, *point) for name,point in anchors.items())
    rows.extend(("RITUAL", r["rite"].lower(), owners[r["zone"]], *r["point"]) for r in layout["rituals"])
    rows.extend(("TREAD", name) for name in stairs)
    for link in layout["links"]:
        if link.get("player_access") != "SEALED_NO_ROUTE":
            points = [tuple(point) for point in link["points"]]
            # The former survey labels the riser junction as a flat foot
            # point. Put replay stations on the measured vault landings.
            if link["id"] == "L14":
                points[1] = (14.20,-3,16)
            elif link["id"] == "L15":
                points[1] = (41.80,-3,16)
            rows.extend(("POINT", link["id"], *point) for point in points)
    return rows


def transitions(owners, annex):
    openings = [
        ("stage", "entry", 28, 0, 72, "z", 2, "trail"),
        ("entry", "avenue", 28, 0, 59.9, "z", 2, "trail"),
        ("west-stair", "ossuary", 14, -3, 16, "x", 1.5, "stair"),
        ("east-stair", "ossuary", 42, -3, 16, "x", 1.5, "stair"),
        ("ossuary", "chamber", 22, -3, 12, "z", 1.5, "arch"),
    ]
    # The annex keeps single ownership over the continuous grave lawns.
    if not annex:
        openings += [
            ("entry", "chapel-court", 20, 0, 63, "x", 2, "trail"),
            ("garden", "avenue", 24, 0, 29, "x", 2, "trail"),
            ("avenue", "east-graves", 32, 0, 29, "x", 2, "trail"),
            ("garden", "old-graves", 20, 0, 20, "z", 2, "trail"),
            ("old-graves", "east-graves", 36, 0, 20, "z", 2, "trail"),
        ]
    return [("OPEN", 900+i, owners[a], owners[b], x,y,z,axis,width,kind)
            for i,(a,b,x,y,z,axis,width,kind) in enumerate(openings)]


def door_rows(layout, owners, annex, leaf_hash):
    frames = [dict(frame) for frame in layout["authored_doorways"]]
    if annex:
        # These are measured openings in retained walls, including existing
        # lintels. They were absent from the old semantic doorway list.
        frames += [
            dict(id="viewing and reception", rooms=["mort-viewing","mort-reception"], axis="z", boundary=54,
                 along=66, clear_width=1.53, clear_height=2.44, sill_y=0, half_depth=.08),
            dict(id="cold storage and preparation", rooms=["mort-cold","mort-prep"], axis="z", boundary=50,
                 along=78, clear_width=1.53, clear_height=2.44, sill_y=0, half_depth=.08),
        ]
    for i,frame in enumerate(frames, 810):
        key = frame["id"].removeprefix(layout["id"]+" ")
        a,b = frame["rooms"]
        if not annex and key == "chapel S 1":
            b = "chapel-court"
        fixed,tangent,axis = frame["boundary"],frame["along"],frame["axis"]
        x,z = (tangent,fixed) if axis=="z" else (fixed,tangent)
        leaves,hinge,direction = SWINGS[key]
        half=frame.get("half_depth",.12)
        yield (i,key,a,b,owners[a],owners[b],x,frame["sill_y"],z,frame["clear_width"],axis,
               fixed-half,fixed+half,frame["clear_height"],leaves,hinge,direction,leaf_hash)


def prepare():
    leaf_hash=hashlib.sha256(SOURCE_LEAF.read_bytes()).hexdigest()
    for name in ("wld-07", "wld-07-mortuary"):
        annex=name.endswith("mortuary")
        packet=BOUNDARY/name
        authority=load(packet/"construction.json")
        layout=load(ROOT/authority["layout"])
        folder=OUTPUT/name
        folder.mkdir(parents=True,exist_ok=True)
        owners=COMMON | (ANNEX_OWNERS if annex else BASE_OWNERS)
        anchors=COMMON_ANCHORS | (ANNEX_ANCHORS if annex else BASE_ANCHORS)
        rows=room_rows(layout,owners,anchors,annex,authority["stair_components"])
        rows.extend(transitions(owners,annex))
        write_tsv(folder/"room-layout.tsv", rows)
        write_tsv(folder/"door-schedule.tsv", door_rows(layout,owners,annex,leaf_hash))


if __name__=="__main__":
    prepare()
