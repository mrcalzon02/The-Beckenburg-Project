
"""Bind route guidance to existing floors, framed openings and stair landings."""
from copy import deepcopy
from pathlib import Path
from shutil import copyfile

from build_measured_approaches import BASE, ROOT, fit_doors, read, relative, sha, write

SOURCE = BASE / "native-foyer-door-pass/roster.json"
OUT = BASE / "flush-porch-and-threshold-pass"
STEM = "surveyed-thresholds"
STATUS = "MEASURED_THRESHOLDS_AND_LANDINGS_RUNTIME_OPEN"

DOORS = {
    "ASYLUM": {"L22": "ASYLUM-x-35-52", "L23": "ASYLUM-z-54-39.5",
               "L27": "ASYLUM-court-ground-south-a-S"},
    "PRISON": {"L068": "PRISON-x-18-65", "L085": "PRISON-cell-plant-stair-N-30"},
    "WLD-18": {"route-02": "WLD-18-z-70-19-y0", "route-08": "WLD-18-west-escape-34",
               "route-09": "WLD-18-x-10-56-y0", "route-12": "WLD-18-x-42-28-y-4",
               "route-15": "WLD-18-x-42-28-y-8"},
}
APPROACHES = {
    # Ground ward doors and upper stair receiving floor.
    "ASYLUM": {
        "L16": [[24,3.4,23.75],[24.9,3.4,24],[26,3.4,24],[27.05,3.4,24],[27.5,3.4,26]],
        "L37": [[44.5,0,25],[42.5,0,24.85],[42.5,0,26],[42.5,0,28]],
        "L39": [[40,0,35],[42.5,0,31.25],[42.5,0,30],[42.5,0,28]],
        "L40": [[40,0,35],[43.75,0,32.5],[45,0,32.5],[47,0,32.5]],
        "L41": [[47,0,32.5],[49,0,32.5],[50.25,0,32.5],[51.5,0,35]],
        "L42": [[51.5,0,35],[50.25,0,37.5],[49,0,37.5],[47,0,37.5]],
        "L43": [[47,0,37.5],[45,0,37.5],[43.75,0,37.5],[40,0,35]],
        "L56": [[49.5,0,14],[47.5,0,13.25],[47.5,0,12],[47.5,0,8]],
    },
    # Parallel gallery aisles and the medical doorway normal.
    "PRISON": {
        "L034": [[15,0,38],[15,0,52],[15,0,53.1],[13,0,55]],
        "L050": [[53,0,38],[53,0,52],[53,0,53.1],[55,0,55]],
        "L103": [[30,0,10],[30.8,0,13],[32,0,13],[35.5,0,13]],
    },
    # Turn on the west receiving floor away from the lower treads.
    "ACADEMY": {
        "L40": [[20.5,0,31],[20.5,0,43.5],[19,0,43.5],[18.05,0,43.5],[18.05,0,43.08],
                [13,0,43.08],[13,0,47.5]],
        "L48": [[13,3.6,16.5],[10,3.6,15.12],[10,3.6,14],[10,3.6,11.5]],
        "L64": [[33.5,3.6,17.5],[35,3.6,17.15],[35,3.6,16],[35,3.6,14.8],[37,3.6,10]],
    },
    # West door to the receiving side of the flight and clear pump-room turns.
    "WLD-18": {
        "route-08": [[14,-4,34],[7.48,-4,34],[7.48,-4,33.42],[6,-4,33.42],[6,-4,34],[6,0,48]],
        "route-11": [[18,-4,32],[18,-4,31.6],[20.5,-4,31.6],[20.5,-4,28],[23,-4,28]],
        "route-12": [[41,-4,28],[42,-4,28],[43.02,-4,28],[43.02,-4,26.5],[48,-4,26.5]],
        "route-15": [[48,-8,28],[48,-8,26],[43.18,-8,26],[43.18,-8,28],[42,-8,28],[41,-8,28]],
    },
}


def remap_flights(before, after, mapping):
    def flights(points):
        return [(i, (a, b)) for i, (a, b) in enumerate(zip(points, points[1:])) if abs(a[1]-b[1]) > .01]

    revised = deepcopy(mapping)
    for ident in before:
        old_link, new_link = before[ident], after[ident]
        assert (old_link["a"], old_link["b"]) == (new_link["a"], new_link["b"]), ident
        assert old_link["points"][0] == new_link["points"][0] and old_link["points"][-1] == new_link["points"][-1], ident
        old_flights, new_flights = flights(old_link["points"]), flights(new_link["points"])
        assert [pair for _, pair in old_flights] == [pair for _, pair in new_flights], ident
        if not old_flights:
            continue
        saved = mapping.get(ident, {})
        revised[ident] = {str(new): saved.get(str(old), old)
                          for (old, _), (new, _) in zip(old_flights, new_flights)}
    return revised


def main():
    roster = read(SOURCE)
    for row in roster["active_sources"]:
        site = row["id"]
        if site not in APPROACHES:
            continue
        source = dict(row)
        old = read(ROOT / source["layout"])
        contract = read(ROOT / source["construction"])
        assert sha(ROOT / source["layout"]) == contract["layout_sha256"]
        assert sha(ROOT / source["glb"]) == source["glb_sha256"] == contract["glb_sha256"]
        assert sha(ROOT / source["native"]) == source["native_sha256"] == contract["physics_sha256"]
        layout = deepcopy(old)
        before = {link["id"]: link for link in old["links"]}
        links = {link["id"]: link for link in layout["links"]}
        for ident, points in APPROACHES[site].items():
            links[ident].update(points=points, note="Circulation guide over existing floors and supported landings.")
        if site == "WLD-18":
            links["route-08"]["width"] = 1.10
        fit_doors(layout, links, DOORS.get(site, {}))
        assert all(layout[key] == old[key] for key in ("zones", "staging", "authored_doorways"))
        layout["status"] = STATUS

        folder = OUT / site.lower()
        folder.mkdir(parents=True, exist_ok=True)
        file = folder / "authored-layout.json"
        write(file, layout)
        for key, suffix in (("glb",".glb"), ("native",".hcs2"), ("blend",".blend")):
            destination = folder / (STEM+suffix)
            copyfile(ROOT/source[key], destination)
            assert sha(destination) == sha(ROOT/source[key])
            row[key] = relative(destination)
        mapping = contract["source_segment_map"]
        contract.update(status=STATUS, geometry_unchanged=True,
                        layout=relative(file), layout_sha256=sha(file), before_layout=source["layout"],
                        before_layout_sha256=sha(ROOT/source["layout"]), before_segment_map=mapping,
                        source_segment_map=remap_flights(before, links, mapping),
                        before_stair_components=contract["stair_components"],
                        source_glb=source["glb"], source_glb_sha256=source["glb_sha256"],
                        source_physics=source["native"], source_physics_sha256=source["native_sha256"],
                        approach_builder=relative(Path(__file__)), approach_builder_sha256=sha(Path(__file__)),
                        superseded_guides=[link for ident, link in before.items() if link != links[ident]])
        destination = folder / "construction.json"
        write(destination, contract)
        row.update(layout=relative(file), construction=relative(destination))
        next(entry for entry in roster["sites"] if entry["id"] == site)["decision"] = STATUS
    roster["baseline"] = relative(SOURCE)
    house = next(entry for entry in roster["sites"] if entry["id"] == "HOUSE")
    house["porch_repair"] = relative(OUT / "house/construction.md")
    write(OUT / "roster.json", roster)


if __name__ == "__main__":
    main()
