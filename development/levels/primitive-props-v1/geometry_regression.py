#!/usr/bin/env python3
"""Independent source-only geometry regression checks for authored prop catalogue."""
from __future__ import annotations
import json
import math
from pathlib import Path
from build import glb
from audit import read_glb
import fit

ROOT=Path(__file__).resolve().parent
catalog=json.loads((ROOT/"catalog.json").read_text(encoding="utf-8"))
models={m["id"]:m for m in catalog["models"]}
assert len(models)==len(catalog["models"])==178
assert len(catalog["sites"])==29
assert all(m["runtime_approved"] is False for m in models.values())

def item(suffix):
    return models["level_blockout_"+suffix]

shaft=item("shaft_ring")
assert not any(p["type"]=="cylinder" and p["radius"]>1 for p in shaft["parts"])
assert all(math.hypot(p["p"][0],p["p"][2])>1.7 for p in shaft["parts"])

fan=item("mine_ventilation")
blades=[p for p in fan["parts"] if p["type"]=="box" and p["material"]=="accent"]
assert len(blades)==6 and all("roll" in p and "yaw" not in p for p in blades)
for blade in blades:
    x,y,_=blade["p"]
    radius=math.hypot(x,y-0.77)
    assert .39 < radius < .41
    expected=math.atan2(y-0.77,x)-math.pi/2
    assert abs(blade["roll"]-expected)<1e-5
    sx,sy,sz=fit.local_extent(blade)
    assert sx>0 and sy>0 and sz>0

logs=item("log_stack")
timber=[p for p in logs["parts"] if p["type"]=="cylinder"]
assert len(timber)==6
assert sorted(round(p["p"][1],3) for p in timber)==[.23,.23,.23,.675,.675,1.12]

door=item("doorway")
assert not any(p["type"]=="box" and p["size"][0]>.85 and p["size"][1]>1.5
               and p["p"][1]<1.5 for p in door["parts"]), "Door opening blocked"

for suffix in ("sarcophagus_plain","sarcophagus_inscribed","sarcophagus_effigy"):
    model=item(suffix)
    bounds,lo,hi=fit.footprint(model,0,0,0)
    assert -0.005<=lo<=.01 and hi<1.4
    assert bounds[2]-bounds[0] < 1.65 and bounds[3]-bounds[1]<2.9

new_suffixes=(
 "castle_wall_tapestry","ossuary_display_rack","votive_candle_stand",
 "medical_resuscitation_cart","library_book_return_cart","prison_cell_cot",
 "logging_sawbuck","mooring_rope_coils","marsh_reed_cluster",
 "rail_buffer_stop","cannery_cargo_pallet","lighthouse_foghorn",
 "coastal_ammo_crate","research_sample_station","motel_ice_machine",
 "dam_service_grating","cinder_decon_frame","mine_pick_tool_rack"
)
for name in new_suffixes:
    model=item(name)
    bounds,lo,hi=fit.footprint(model,0,0,0)
    assert lo >= -.015 and hi<4.0, (name,lo,hi)
    assert bounds[2]-bounds[0] <=5.5 and bounds[3]-bounds[1]<=5.5
    raw,triangles=glb(model)
    assert 0<triangles<3500 and raw[:4]==b"glTF"
    imported,committed=read_glb(ROOT/"models"/(model["id"]+".glb"))
    assert raw==committed and imported["extras"]["runtimeApproved"] is False

assert len(new_suffixes)==18
print("PASS: 18 new native-decodable GLBs, shaft opening, rolled fan blades, "
      "staggered logging rack, clear doorframe, sarcophagus geometry and all source-only gates")
