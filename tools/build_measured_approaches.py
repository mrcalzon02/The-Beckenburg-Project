"""Bind circulation to measured thresholds and the existing physical arrival courts."""
import copy
import hashlib
import json
from pathlib import Path
import shutil

ROOT = Path(__file__).resolve().parents[1]
BASE = ROOT / "development/levels/rebuild"
SOURCE = BASE / "interior-stair-landing-pass/roster.json"
OUT = BASE / "measured-arrival-and-approach-pass"
STEM = "measured-architecture"
STATUS = "MEASURED_PHYSICAL_APPROACHES_RUNTIME_OPEN"
LINK_NOTE = "Nonphysical guidance follows measured built geometry and its actual thresholds."

FARM_DOORS = {
    "L05": "FARMHOUSE bay indexed door 2", "L08": "FARMHOUSE bay indexed door 3",
    "L12": "FARMHOUSE bay indexed door 1", "L13": "FARMHOUSE bay indexed door 4",
    "L19": "FARMHOUSE bay indexed door 0", "L28": "FARMHOUSE bay indexed door 6",
}
NORTHWATCH_DOORS = {
    "L03": "WLD-08-x-20-41", "L05": "WLD-08-z-56-11", "L06": "WLD-08-x-44-41",
    "L07": "WLD-08-z-44-53", "L08": "WLD-08-z-56-53", "L09": "WLD-08-z-26-26",
    "L10": "WLD-08-x-32-19", "L11": "WLD-08-z-26-38", "L12": "WLD-08-x-20-19",
    "L14": "WLD-08-x-44-16", "L15": "WLD-08-z-26-53", "L16": "WLD-08-x-62-14",
}


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def read(path):
    return json.loads(path.read_bytes())


def write(path, value):
    path.write_bytes((json.dumps(value, indent=2, ensure_ascii=False) + "\n").encode())


def relative(path):
    return path.relative_to(ROOT).as_posix()


def fit_doors(layout, links, bindings):
    doors = {door["id"]: door for door in layout["authored_doorways"]}
    for link_id, door_id in bindings.items():
        door = doors[door_id]
        x, z = (door["along"], door["boundary"]) if door["axis"] == "z" else (door["boundary"], door["along"])
        at = [x, door["sill_y"], z]
        link = links[link_id]
        assert any(all(min(a[i], b[i]) <= t <= max(a[i], b[i]) for i, t in enumerate(at))
                   and (a[0] == b[0] == x or a[2] == b[2] == z)
                   for a, b in zip(link["points"], link["points"][1:])), (link_id, door_id)
        link.update(portal=door_id, opening="doorway", threshold=at)
        link.pop("threshold_reservation", None)
        link["width"] = min(link["width"], door["clear_width"])
        link["note"] = LINK_NOTE


def farmhouse(layout, links):
    transfer = [23.13, 0, 17]
    routes = {
        "L05": [[23,0,25],[23,0,22],[23,0,20.3],[23.13,0,19.2],transfer],
        "L08": links["L08"]["points"][:4] + [[23.13,0,19.2],transfer],
        "L12": [transfer,[23.13,0,13.05],[23,0,12.65],[23,0,12],[23,0,11]],
        "L34": [transfer,[23.13,0,12.8]] + links["L34"]["points"][2:],
        "L35": links["L35"]["points"][:6] + [[23.13,0,20],transfer],
        "L36": [[39.5,0,17.5],[39.66,0,17.5],[39.66,0,16.8]] + links["L36"]["points"][2:],
    }
    for ident, points in routes.items():
        links[ident].update(points=points, note=LINK_NOTE)
    for ident, width in (("L34",1.10), ("L35",1.10), ("L36",1.05)):
        links[ident]["width"] = width
    fit_doors(layout, links, FARM_DOORS)
    layout["measured_approaches"] = {
        "ground_stair_transfer": transfer, "ground_guard_limits_x": [22.55,23.71],
        "north_turn_before_cleaner_door": [23,0,12.65],
        "loft_wall_to_guard_limits_x": [39.12,40.21],
    }


def northwatch(layout, links):
    routes = {
        "L01": [layout["staging"]["cargo_route"][0]] + links["L01"]["points"][1:],
        "L07": [[54,0,35],[53,0,42.7],[53,0,44],[53,0,50]],
        "L12": [[26,0,19],[20,0,19],[13.35,0,19],[13.35,0,16.05],[11,0,16]],
        "L16": [[53,0,10]] + links["L16"]["points"][1:],
    }
    for ident, points in routes.items():
        links[ident].update(points=points, note=LINK_NOTE)
    links["L01"].pop("threshold_reservation", None)
    links["L01"]["departure"] = "canonical van ramp toe"
    fit_doors(layout, links, NORTHWATCH_DOORS)
    layout["measured_approaches"] = {
        "east_generator_guard_x": [12.185,12.265],
        "north_turn_in_the_continuous_landing": [13.35,0,16.05],
        "radio_to_generator_sill": [20,0,19],
        "canonical_van_ramp_toe": routes["L01"][0],
    }


def school(layout, links):
    staging = layout["staging"]
    assert staging["point"] == [28,0,51]
    toe, apron, foyer = staging["cargo_route"][0], [22.5,0,40.5], [22.5,0,33]
    layout["measured_approaches"] = {
        "canonical_van_ramp_toe": toe, "arrival_walkable_y": 0,
        "forecourt_join": apron, "foyer_sill": [22.5,0,36],
        "physical_receivers": [
            "Saved editor geographic earthworks SCHOOL",
            "SCHOOL south school forecourt pavement region0",
            "SCHOOL architectural entrance landing and soil-bearing plinth",
            "SCHOOL-z-36-22.5",
        ],
    }
    links["L42"].update(points=[toe,[23.75,0,51],[22.5,0,48],apron], note=LINK_NOTE,
                        departure="canonical van ramp toe")
    links["L42"].pop("threshold", None)
    links["L41"].update(points=[[52,0,26],[52,0,39],[22.5,0,39],apron], note=LINK_NOTE)
    links["L41"].pop("threshold", None)
    links["L35"].update(points=[foyer,[22.5,0,36],apron], note=LINK_NOTE)
    fit_doors(layout, links, {"L35": "SCHOOL-z-36-22.5"})
    staging["cargo_route"] = links["L42"]["points"] + [[22.5,0,36]]
    staging["note"] = "The canonical ramp toe joins the saved gravel yard, continuous paved forecourt and fitted foyer sill."


DESIGNS = {"FARMHOUSE": farmhouse, "WLD-08": northwatch, "SCHOOL": school}


def build():
    roster = read(SOURCE)
    for row in roster["active_sources"]:
        site = row["id"]
        if site not in DESIGNS:
            continue
        source = dict(row)
        prior = read(ROOT / row["layout"])
        layout = copy.deepcopy(prior)
        meta = read(ROOT / row["construction"])
        assert sha(ROOT / row["layout"]) == meta["layout_sha256"]
        assert sha(ROOT / row["native"]) == row["native_sha256"] == meta["physics_sha256"]
        assert sha(ROOT / row["glb"]) == row["glb_sha256"] == meta["glb_sha256"]
        links = {link["id"]: link for link in layout["links"]}
        old = {link["id"]: link for link in prior["links"]}
        DESIGNS[site](layout, links)
        assert layout["zones"] == prior["zones"]
        assert layout["authored_doorways"] == prior["authored_doorways"]
        assert layout["staging"]["point"] == prior["staging"]["point"]
        assert layout["staging"]["yaw"] == prior["staging"]["yaw"]
        layout["status"] = STATUS
        previous_map = copy.deepcopy(meta["source_segment_map"])
        for key, segments in previous_map.items():
            points, was = links[key]["points"], old[key]["points"]
            positions = {}
            for index, part in segments.items():
                pair = was[int(index):int(index)+2]
                match = [i for i in range(len(points)-1) if points[i:i+2] == pair]
                assert len(match) == 1, (site, key, pair)
                positions[str(match[0])] = part
            meta["source_segment_map"][key] = positions
        folder = OUT / site.lower()
        folder.mkdir(parents=True, exist_ok=True)
        layout_path = folder / "authored-layout.json"
        write(layout_path, layout)
        for key, ext in (("native", ".hcs2"), ("glb", ".glb"), ("blend", ".blend")):
            original = ROOT / row[key]
            path = folder / (STEM + ext)
            shutil.copyfile(original, path)
            assert sha(path) == sha(original), path
            row[key] = relative(path)
        meta.update(
            status=STATUS, before_layout=source["layout"], before_layout_sha256=sha(ROOT/source["layout"]),
            source_glb=source["glb"], source_glb_sha256=source["glb_sha256"],
            source_physics=source["native"], source_physics_sha256=source["native_sha256"],
            before_stair_components=list(meta["stair_components"]), before_segment_map=previous_map,
            geometry_unchanged=True, layout=relative(layout_path), layout_sha256=sha(layout_path),
            measured_approaches=layout["measured_approaches"],
            approach_builder=relative(Path(__file__)), approach_builder_sha256=sha(Path(__file__)),
            superseded_guides=[old[key] for key in old if old[key] != links[key]],
        )
        contract = folder / "construction.json"
        write(contract, meta)
        row.update(construction=relative(contract), layout=relative(layout_path))
        next(entry for entry in roster["sites"] if entry["id"] == site)["decision"] = STATUS
    roster["baseline"] = relative(SOURCE)
    write(OUT / "roster.json", roster)


if __name__ == "__main__":
    build()
