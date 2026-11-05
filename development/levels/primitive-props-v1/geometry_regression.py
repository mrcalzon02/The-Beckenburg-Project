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
assert len(models)==len(catalog["models"])==298
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

# Refinement pass three: authored, decodable source assets with map assignments.
new_studies=("structural_window_frame","stair_landing_module","stone_arch_open","timber_roof_truss","pipe_riser_support","wall_lantern_sconce","castle_framed_painting","castle_woven_rug","castle_statue_plinth","castle_long_bench","castle_warehouse_field_kit_crates","castle_iron_candelabrum","domestic_pitched_roof_module","domestic_chimney_stack","domestic_stone_fireplace","domestic_porch_stair","domestic_kitchen_range","domestic_wall_bookcase","school_chalkboard","school_locker_bank","science_instrument_table","hospital_curtain_track","institution_linen_cabinet","prison_guard_station","logging_winch_sled","camp_tarp_shelter","cavern_stalagmite_cluster","mine_braced_portal","cemetery_headstone_row","cemetery_iron_lantern","marsh_flatboat","wharf_crab_pot","cannery_overhead_chute","ship_galley_range","ship_chart_cabinet","lighthouse_access_ladder","motel_guest_bed","bunker_observation_scope","cinder_cable_rack","research_battery_bank","rail_trackside_toolbox","dam_pressure_gauge_panel","monastery_stained_window",)
assert len(new_studies)==43
for suffix in new_studies:
    model=item(suffix)
    assert model["authoring_hints"]["runtime_placement"] is False
    assert model["runtime_approved"] is False
    bounds,lo,hi=fit.footprint(model,0,0,0)
    assert lo>=-.04 and hi<=4.2, (suffix,lo,hi)
    assert bounds[2]-bounds[0]<6 and bounds[3]-bounds[1]<6, suffix
    raw,tri=glb(model)
    doc,stored=read_glb(ROOT/"models"/(model["id"]+".glb"))
    assert stored==raw and 0<tri<=3500
    assert doc["extras"]["runtimeApproved"] is False
for suffix in ("structural_window_frame","stone_arch_open","mine_braced_portal"):
    model=item(suffix)
    assert not any(p["type"]=="box" and p["size"][0]>.8 and p["size"][1]>1.2 and abs(p["p"][0])<.35 and p["p"][1]<1.7 for p in model["parts"])
for suffix in ("domestic_pitched_roof_module","timber_roof_truss","camp_tarp_shelter"):
    assert sum(1 for p in item(suffix)["parts"] if p.get("roll",0))>=2
assert sum(1 for p in item("lighthouse_access_ladder")["parts"] if p["type"]=="box")>=16
assert all(len(s["prototypes"])==len(set(s["prototypes"])) for s in catalog["sites"])
print("PASS: 43 new model studies decoded; open arches, roof slopes, per-map coverage")

# Inclined cannery conveyor deck must be physically seated on the posts.
chute=item("cannery_overhead_chute")
deck=chute["parts"][0]
assert math.isclose(deck["roll"],-math.pi/18,abs_tol=1e-6)
for rail in chute["parts"][1:3]:
    assert rail["roll"]==deck["roll"]
posts=[p for p in chute["parts"] if p["type"]=="box" and abs(p["p"][0])==1.09]
assert len(posts)==4
for post in posts:
    height=deck["p"][1]+post["p"][0]*math.tan(deck["roll"])-.05/math.cos(deck["roll"])
    assert abs(post["p"][1]+post["size"][1]/2-height)<.001, (post,height)
    assert abs(post["p"][1]-post["size"][1]/2)<.001
print("PASS inclined cannery deck and 4 true-height supports")

# Frustum regression: pointed tips have zero top radius and never produce
# collapsed duplicate-cap triangles in the committed GLB.
rocks=item("cavern_stalagmite_cluster")
tapers=[p for p in rocks["parts"] if p["type"]=="frustum"]
assert len(tapers)==8 and sum(p["top_radius"]==0 for p in tapers)==4
assert not any(p["type"]=="cylinder" for p in rocks["parts"])
raw,triangle_count=glb(rocks)
doc,stored=read_glb(ROOT/"models"/(rocks["id"]+".glb"))
assert raw==stored and 0<triangle_count<=3500
assert doc["extras"]["runtimeApproved"] is False
print("PASS tapered cavern spires and deterministic source GLB")

# Additional location-specific source-only models and open-shape review.
new_map_models=("camp_pine_cluster","camp_picnic_table","camp_ranger_kiosk","camp_water_tank","camp_cook_shelter","camp_footbridge_module","cave_rockfall_heap","cave_hanging_stalactites","cave_survey_tripod","lookout_fire_map_table","lookout_reservoir_tank","rail_straight_track_bay","rail_cargo_platform","rail_telegraph_pole","rail_signal_relay_box","rail_handcar","rail_coal_bunker","lighthouse_service_step","lighthouse_oil_barrel_rack","lighthouse_weather_vane","lighthouse_rescue_lifebuoy","lighthouse_landing_capstan","mortuary_casket_bier","mortuary_instrument_trolley","mortuary_cold_drawer_wall","mortuary_flower_wreath","hydro_handwheel_valve","marsh_deadwood_snag","marsh_hunting_blind","ship_open_cargo_hatch","wharf_open_fish_crates","bunker_support_arch",)
assert len(new_map_models)==32
for suffix in new_map_models:
    model=item(suffix)
    assert model["runtime_approved"] is False
    assert model["authoring_hints"]["runtime_placement"] is False
    bounds,lo,hi=fit.footprint(model,0,0,0)
    assert lo>=-.08 and hi<=4.1, (suffix,lo,hi)
    assert bounds[2]-bounds[0]<6 and bounds[3]-bounds[1]<6, suffix
    raw,count=glb(model)
    doc,stored=read_glb(ROOT/"models"/(model["id"]+".glb"))
    assert stored==raw and 0<count<=3500 and doc["extras"]["runtimeApproved"] is False
assert sum(p["type"]=="frustum" for m in catalog["models"] for p in m["parts"])>=30
assert len(item("rail_straight_track_bay")["parts"])>=11
assert len(item("lighthouse_rescue_lifebuoy")["parts"])>=12
assert all(len(s["prototypes"])==len(set(s["prototypes"])) for s in catalog["sites"])
print("PASS: 32 location-specific independent GLBs, source-only flags and measured fit")

# The motel guest bed no longer floats 12cm above its source floor.
bed=item("motel_guest_bed")
bounds,low,high=fit.footprint(bed,0,0,0)
assert abs(low)<1e-7 and len(bed["parts"])==10
raw,tri=glb(bed)
doc,committed=read_glb(ROOT/"models"/(bed["id"]+".glb"))
assert raw==committed and tri>0 and doc["extras"]["runtimeApproved"] is False
print("PASS: grounded motel guest bed")

# All source models carry explicitly provisional physical mounting contracts.
mount_types={"floor","wall","ceiling","roof","opening","structure","water","terrain","display_surface"}
assert len(models)==298
assert all(m.get("authoring_hints",{}).get("mount") in mount_types for m in models.values())
assert all(m["authoring_hints"]["mount_confidence"]=="provisional_unreviewed" for m in models.values())
assert all(m["authoring_hints"]["source_pivot_reviewed"] is False for m in models.values())
assert all(m["authoring_hints"]["renderer_orientation_reviewed"] is False for m in models.values())
assert all(m["authoring_hints"]["runtime_placement"] is False for m in models.values())
assert item("ballroom_chandelier")["authoring_hints"]["mount"]=="ceiling"
assert item("ship_hull_bow_section")["authoring_hints"]["mount"]=="structure"
assert item("relic_ossuary_keys")["authoring_hints"]["mount"]=="display_surface"
assert item("cannery_coldroom_door")["authoring_hints"]["mount"]=="opening"
assert item("family_photo_cluster")["authoring_hints"]["mount"]=="wall"
print("PASS: all 253 source models have provisional, unapproved mount contracts")

# Newly introduced map-specific props remain individually decodable and unapproved.
third_wave=("domestic_exterior_shutters","domestic_roof_gutter","domestic_wall_downspout","domestic_picket_fence","domestic_garden_gate","domestic_cellar_steps","camp_bathhouse_privacy_stalls","camp_field_cot","camp_tent_entry_flaps","camp_woodpile_lean_to","camp_manual_water_pump","cave_ledge_rope_guard","cave_plank_rope_bridge","cave_survey_cairns","cave_layered_sandstone","cave_excavation_trestle","rail_station_canopy","rail_semaphore_pole","rail_station_clock","rail_luggage_cart","rail_tunnel_mouth","lighthouse_tower_wall_course","lighthouse_spiral_tread_module","lighthouse_keeper_porch","lighthouse_lens_pedestal","lighthouse_cliff_drain","mortuary_wash_sink_station","mortuary_visitors_registry_desk","castle_crypt_column","castle_sarcophagus_stand",)
assert len(third_wave)==30 and len(models)==298
for suffix in third_wave:
    model=item(suffix)
    h=model["authoring_hints"]
    assert model["runtime_approved"] is False
    assert h["mount_confidence"]=="provisional_unreviewed"
    assert h["source_pivot_reviewed"] is False
    assert h["renderer_orientation_reviewed"] is False
    assert h["runtime_placement"] is False
    bbox,lo,hi=fit.footprint(model,0,0,0)
    assert -.12<=lo and hi<4.1, (suffix,lo,hi)
    assert bbox[2]-bbox[0]<6 and bbox[3]-bbox[1]<6
    raw,tri=glb(model)
    doc,binary=read_glb(ROOT/"models"/(model["id"]+".glb"))
    assert raw==binary and 0<tri<=3500 and doc["extras"]["runtimeApproved"] is False
tower=item("lighthouse_tower_wall_course")
assert sum(p["type"]=="box" for p in tower["parts"])>=12
assert not any(p["type"]=="cylinder" for p in tower["parts"])
assert not any(p["type"]=="box" and p["size"][0]>1.2 and p["size"][1]>1.7
               and abs(p["p"][0])<.5 for p in item("rail_tunnel_mouth")["parts"])
assert all(len(s["prototypes"])==len(set(s["prototypes"])) for s in catalog["sites"])
print("PASS: 30 new independent map geometry studies remain non-playable")

# Refinement batch: grounded service vehicles, supported mobile chart station,
# four standalone photo frames (never one giant backing board).
generator=item("diesel_generator_skid")
wheels=[p for p in generator["parts"] if p["type"]=="cylinder" and p.get("axis")=="x"]
assert len(wheels)==2 and all(abs(p["p"][1]-p["radius"])<1e-7 for p in wheels)
assert abs(fit.footprint(generator,0,0,0)[1])<.03

mobile=item("hospital_chart_station")
assert mobile["authoring_hints"]["mount"]=="floor"
assert fit.footprint(mobile,0,0,0)[1]>=-.025
assert not any(p["type"]=="box" and p["size"][1]>1.7 and p["p"][1]<.8 for p in mobile["parts"])

photos=item("family_photo_cluster")
assert photos["authoring_hints"]["mount"]=="wall"
assert len(photos["parts"])==24
assert not any(p["type"]=="box" and p["size"][0]>1.4 and p["size"][1]>1.4 for p in photos["parts"])
for short in ("diesel_generator_skid","hospital_chart_station","family_photo_cluster"):
    m=item(short)
    raw,tri=glb(m)
    doc,stored=read_glb(ROOT/"models"/(m["id"]+".glb"))
    assert 0<tri<=3500 and raw==stored and doc["extras"]["runtimeApproved"] is False

new_round=("school_cafeteria_table","school_wall_coat_rack","school_auditorium_stage","school_library_globe","hospital_mobile_iv_stand","hospital_ward_screen","hospital_sterilizer_cabinet","hospital_patient_call_panel","dam_service_catwalk","dam_turbine_access_hatch","dam_spillway_guard_screen","cannery_hanging_hook_rail","cannery_rinse_bench","cannery_cart_dolly","prison_visitation_counter",)
assert len(new_round)==15 and len(models)==298
for short in new_round:
    m=item(short)
    meta=m["authoring_hints"]
    assert m["runtime_approved"] is False and meta["runtime_placement"] is False
    assert meta["source_pivot_reviewed"] is False and meta["renderer_orientation_reviewed"] is False
    bounds,lo,hi=fit.footprint(m,0,0,0)
    assert -0.08<=lo and hi<4.4, (short,lo,hi)
    assert bounds[2]-bounds[0]<6 and bounds[3]-bounds[1]<6
    raw,tri=glb(m)
    doc,stored=read_glb(ROOT/"models"/(m["id"]+".glb"))
    assert stored==raw and 0<tri<=3500 and doc["extras"]["runtimeApproved"] is False
print("PASS: 15 location-specific models, grounded rolling supports, separate hanging photo frames")


# Castle relic source QA: the upright astrolabe must sit on its timber
# plinth, not pierce the display shelf. A dial on the XY face uses roll
# around local Z, never yaw around the world's vertical axis.
astrolabe=item("relic_bronze_astrolabe")
dial=[p for p in astrolabe["parts"] if p["material"]=="gilt"]
assert len(dial)==13 and all("roll" in p and "yaw" not in p for p in dial)
supports=[p for p in astrolabe["parts"]
          if p["type"]=="cylinder" and p.get("axis","y")=="y"
          and p["material"]=="bronze"]
assert len(supports)>=2, "Astrolabe dial must have a load-bearing upright"
_,low,high=fit.footprint(astrolabe,0,0,0)
assert -.001<=low<=.02 and high>1, (low,high)
assert astrolabe["runtime_approved"] is False
raw,tri=glb(astrolabe)
doc,stored=read_glb(ROOT/"models"/(astrolabe["id"]+".glb"))
assert raw==stored and tri<=3500 and doc["extras"]["runtimeApproved"] is False
print("PASS: astrolabe bronze dial grounded, true-face roll, support and GLB source identity")
