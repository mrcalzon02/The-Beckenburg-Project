"""Promote measured crossdock and threshold approaches with byte-identical source geometry."""
import copy
from pathlib import Path
import shutil

from build_measured_approaches import BASE, ROOT, fit_doors, read, relative, sha, write

SOURCE = BASE/"measured-arrival-and-approach-pass/roster.json"
OUT = BASE/"native-foyer-door-pass"
STEM = "fitted-approaches"
SITES = ("CHURCH","WLD-06","WLD-16")
STATUS = "MEASURED_DECK_AND_THRESHOLD_APPROACHES_RUNTIME_OPEN"


def revise(site, layout):
    links = {link["id"]:link for link in layout["links"]}
    if site == "WLD-06":
        bindings = {"L02":"WLD-06-z-54-12","L03":"WLD-06-z-54-52","L05":"WLD-06-x-20-48",
                    "L06":"WLD-06-x-44-48","L12":"WLD-06-x-44-17"}
        routes = {"L01":layout["staging"]["cargo_route"],
                  "L08":[[22,0,23],[22,0,8],[32,0,8]],"L09":[[32,0,8],[42,0,8],[42,0,23]]}
        links["L01"].pop("threshold_reservation",None)
        links["L01"]["departure"]="canonical van ramp toe"
        surveyed = dict(crossdock_turns=[[22,0,8],[42,0,8]],fish_works_north_wall_z=10)
    elif site == "WLD-16":
        bindings = {"route-02":"WLD-16-z-50-18-y0","route-04":"WLD-16-z-42-18-y0",
                    "route-05":"WLD-16-x-13-45-y0"}
        for side,wall in enumerate((13,39)):
            for i in range(4):
                bindings[f"route-{15+i+side*4}"]=f"WLD-16-x-{wall}-{22.5+i*5}-y0"
        routes = {"route-01":layout["staging"]["cargo_route"]}
        links["route-01"]["departure"]="canonical van ramp toe"
        surveyed = dict(office_sills=[[18,0,50],[18,0,42],[13,0,45]],guest_door_clear_width=1.05)
    else:
        bindings = {f"L{i:02}":f"CHURCH bay indexed door {door}"
                    for i,door in ((16,3),(19,1),(20,0),(21,4),(22,5),(23,6),(24,2))}
        routes = {
            "L08":[[22,0,19],[19.2,0,22],[18,0,22],[16,0,22]],
            "L09":[[16,0,22],[15.05,0,20],[14,0,20],[12,0,20]],
            "L10":[[16,0,22],[15.05,0,24],[14,0,24],[12,0,24]],
            "L12":[[22,0,9],[26,0,9],[27.05,0,9],[28.5,0,10]],
            "L14":[[28.5,0,10],[29.65,0,8],[31,0,8],[32.5,0,8]],
            "L15":[[28.5,0,10],[29.65,0,12],[31,0,12],[32.5,0,12]],
            "L16":[[30,0,20],[28.5,0,24.9],[28.5,0,26],[28.5,0,27.15],
                   [29.4,0,28.4],[29.7,0,30],[30,0,30]],
        }
        surveyed = dict(crypt_frame=[28.5,0,26],west_well_rim_limits_x=[30.45,30.53])
    for ident,points in routes.items():
        links[ident].update(points=points,note="Nonphysical guidance on the measured floor and supported landing.")
    fit_doors(layout,links,bindings)
    layout["measured_approaches"]=surveyed


def main():
    roster=read(SOURCE)
    for row in roster["active_sources"]:
        site=row["id"]
        if site=="SCHOOL":
            row["staged_native_door_schedule"]=relative(OUT/"school/door-schedule.json")
            row["staged_native_door_verification"]=relative(OUT/"verification/school-foyer-runtime.json")
            next(s for s in roster["sites"] if s["id"]==site)["decision"]="STAGED_NATIVE_FOYER_VERIFIED_RUNTIME_OPEN"
            continue
        if site not in SITES:
            continue
        original=dict(row)
        before=read(ROOT/row["layout"])
        meta=read(ROOT/row["construction"])
        assert sha(ROOT/row["layout"])==meta["layout_sha256"]
        assert sha(ROOT/row["glb"])==row["glb_sha256"] and sha(ROOT/row["native"])==row["native_sha256"]
        new=copy.deepcopy(before)
        revise(site,new)
        old_links={link["id"]:link for link in before["links"]}
        new_links={link["id"]:link for link in new["links"]}
        segment_map=meta["source_segment_map"]
        for key,segments in segment_map.items():
            for n in map(int,segments):
                assert new_links[key]["points"][n:n+2]==old_links[key]["points"][n:n+2],(site,key,n)
        for key in ("zones","staging","authored_doorways"):
            assert new[key]==before[key]
        folder=OUT/site.lower()
        folder.mkdir(parents=True,exist_ok=True)
        file=folder/"authored-layout.json"
        new["status"]=STATUS
        write(file,new)
        for key,suffix in (("glb",".glb"),("native",".hcs2"),("blend",".blend")):
            copy_to=folder/(STEM+suffix)
            shutil.copyfile(ROOT/row[key],copy_to)
            assert sha(copy_to)==sha(ROOT/row[key])
            row[key]=relative(copy_to)
        meta.update(status=STATUS,geometry_unchanged=True,
                    layout=relative(file),layout_sha256=sha(file),before_layout=original["layout"],
                    before_layout_sha256=sha(ROOT/original["layout"]),
                    before_stair_components=meta["stair_components"],before_segment_map=segment_map,
                    source_glb=original["glb"],source_glb_sha256=original["glb_sha256"],
                    source_physics=original["native"],source_physics_sha256=original["native_sha256"],
                    approach_builder=relative(Path(__file__)),approach_builder_sha256=sha(Path(__file__)),
                    measured_approaches=new["measured_approaches"],
                    superseded_guides=[link for ident,link in old_links.items() if link!=new_links[ident]])
        contract=folder/"construction.json"
        write(contract,meta)
        row.update(layout=relative(file),construction=relative(contract))
        next(s for s in roster["sites"] if s["id"]==site)["decision"]=STATUS
    roster["baseline"]=relative(SOURCE)
    write(OUT/"roster.json",roster)


if __name__ == "__main__":
    main()
