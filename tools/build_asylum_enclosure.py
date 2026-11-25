"""Fit the hospital boundary to the saved native terrain and canonical van road."""
import json
import math
from pathlib import Path
import shutil
import sys

import bpy
from mathutils import Vector

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/"tools"))
from build_asylum_spine import append, export, match_geometry, native_records, native_write, records, sha, triangles
from build_college_grounds import terrain_tree
from build_deliberate_entrances import write
from build_monastery_perimeter import fit_terrain_extension, fixed_slab, left_normal, miter, profile_mesh, station, wall_section
from build_roster_foundations import material
from prepare_cemetery_doors import write_tsv

BASE = ROOT/"development/levels/rebuild"
PRIOR = BASE/"asylum-flight-rails-pass"
OUT = BASE/"asylum-enclosure-pass"
PLAN = ROOT/"development/levels/edge-sky-plans/asylum/plan.json"
GROUND = "Saved editor geographic earthworks ASYLUM"
GATE = ((36,92.4),(28,92.4))
OUTER = [(-48,-33),(-20,-49),(25,-53),(69,-49),(105,-26),(116,24),(114,77),(87,112),
         (63,137),(42,145),(34,149),(26,148),(-3,139),(-36,115),(-51,72),(-54,24)]
WEST_ROAD = [(28,87),(28,103),(30,124),(34,149)]
EAST_ROAD = [(36,87),(36,103),(38,124),(42,145)]


def outside_grade(x,z,rim):
    nearest=min(rim,key=lambda p:(x-p[0])**2+(z-p[1])**2)
    d=math.hypot(x-nearest[0],z-nearest[1])
    northwest=5.3*math.exp(-((x-16)/48)**2-((z+23)/20)**2)
    east=2.4*math.exp(-((x-84)/21)**2-((z-37)/58)**2)
    west=-3.4*math.exp(-((x+29)/22)**2-((z-58)/36)**2)
    target=-.28+northwest+east+west
    if z>=87:
        mid=32+max(0,min(6,(z-103)*.13))
        fade=max(0,min(1,(8-abs(x-mid))/4))
        target+=(-.023-.036*(z-87)-target)*fade
    blend=min(1,d/19)
    return nearest[2]*(1-blend)+target*blend


def enclosure(ground,apron,design,brick,stone,wood,iron):
    saved,extended=terrain_tree(ground),terrain_tree(apron)
    down=Vector((0,0,-1))
    def grade(x,z):
        above=Vector((x,-z,17))
        hit,*_=saved.ray_cast(above,down,34)
        if hit is None:
            hit,*_=extended.ray_cast(above,down,34)
        assert hit is not None,("Missing hospital footing",x,z)
        return hit.z

    ring=[tuple(edge["points"][0]) for edge in design["edges"]]
    # The northeast planning diagonal overlaps the retained treatment footprint.
    ring[8:10]=[(63,-5.45),(69,5)]
    ring[17:19]=GATE
    normals=[miter(ring[i-1],p,ring[(i+1)%len(ring)]) for i,p in enumerate(ring)]
    parts,rows=[],[]
    for i,(a,b) in enumerate(zip(ring,ring[1:]+ring[:1])):
        treatment=design["edges"][i]["treatment"]
        if (a,b)==GATE:
            continue
        height,width=(2.28,.78) if treatment=="bank" else (2.14,.51)
        count=math.ceil(math.dist(a,b)/.42)
        normal=left_normal(a,b)
        stations=[(station(a,b,n/count),normal) for n in range(count+1)]
        stations[0]=(a,normals[i])
        stations[-1]=(b,normals[(i+1)%len(ring)])
        mesh,low,top0,top1=wall_section(stations,grade,height,width)
        label=f"Asylum estate {treatment} wall {i+1:02}"
        wall=profile_mesh(label,mesh,stone if treatment=="bank" else brick,
                          "Mortared hospital boundary seated in original or joined terrain")
        wall.data.materials.append(stone)
        for face in wall.data.polygons:
            if face.normal.z>.24:
                face.material_index=1
        parts.append(wall)
        rows.append((label,*a,*b,len(mesh),height,low,top0,top1))
        # Fitted brick piers give the long service and forecourt runs a bay rhythm.
        if treatment in {"wall","gate"}:
            bays=max(2,math.ceil(math.dist(a,b)/5.1))
            for n in range(1,bays):
                x,z=station(a,b,n/bays)
                bounds=(x-.37,z-.37,x+.37,z+.37)
                footing=min(grade(x+dx,z+dz) for dx in (-.37,.37) for dz in (-.37,.37))-.32
                cap=grade(x,z)+2.36
                parts.append(fixed_slab(f"{label} pier {n}",bounds,footing,cap,brick,"Continuous rooted brick pier"))
                parts.append(fixed_slab(f"{label} pier coping {n}",(x-.43,z-.43,x+.43,z+.43),
                                        cap,cap+.13,stone,"Limestone pier head with drip edge"))

    x1,z=GATE[0]
    x0=GATE[1][0]
    support=min(grade(x,z+o) for x in range(x0,x1+1) for o in (-.22,0,.22))
    upper=max(grade(x,z) for x in range(x0,x1+1))+2.50
    for end,x in (("west",x0-.50),("east",x1+.50)):
        fit=min(grade(x+dx,z+dz) for dx in (-.5,.5) for dz in (-.50,.50))
        parts.append(fixed_slab(f"Asylum outer {end} entrance pier",(x-.50,z-.50,x+.50,z+.50),fit-.38,upper+.34,brick,
                                "Founded brick entrance pier"))
        parts.append(fixed_slab(f"Asylum outer {end} pier cap",(x-.59,z-.59,x+.59,z+.59),upper+.34,upper+.51,stone,
                                "Overhanging limestone cap"))
    for end,a,b,pivot in (("west",27.91,32.015,28),("east",31.985,36.09,36)):
        parts.append(fixed_slab(f"Asylum outer {end} closed carriage leaf",(a,z-.13,b,z+.13),
                                support-.06,upper,wood,"Closed hospital carriage gate leaf"))
        for i,y in enumerate((support+.38,upper-.35),1):
            parts.append(fixed_slab(f"Asylum outer {end} rail {i}",(a,z-.175,b,z-.123),
                                    y-.075,y+.075,wood,"Gate shear rail"))
            x0s,x1s=sorted((pivot-.16 if end=="west" else pivot+.16,pivot+.88 if end=="west" else pivot-.88))
            parts.append(fixed_slab(f"Asylum outer {end} strap {i}",(x0s,z-.205,x1s,z-.173),
                                    y-.045,y+.045,iron,"Hinge strap spans from leaf to brick pier"))
    parts.append(fixed_slab("Asylum outer gate centre astragal",(31.95,z-.175,32.03,z-.128),
                            support+.02,upper,wood,"Board closes the joint between leaves"))
    rows.append(("Asylum outer fixed carriage gate",*GATE[0],*GATE[1],9,upper-support,support-.06,upper,upper+.51))
    return parts,rows


def build():
    parent=json.loads((PRIOR/"checkpoint.json").read_bytes())
    source=parent["source_authority"]
    for key in ("blend","glb","native"):
        assert sha(ROOT/source[key])==source[key+"_sha256"]
    design=json.loads(PLAN.read_bytes())
    bpy.ops.wm.open_mainfile(filepath=str(ROOT/source["blend"]))
    bpy.context.preferences.filepaths.save_version=0
    ground=bpy.context.scene.objects[GROUND]
    original=records(ground)
    soil=material("Asylum exterior hillside soil",(.19,.206,.143))
    road=material("Asylum exterior compacted road",(.283,.246,.199))
    brick=material("Asylum boundary clinker brick",(.34,.184,.126))
    stone=material("Asylum weathered pale coping",(.44,.40,.34))
    wood=material("Asylum dark painted estate gate",(.114,.133,.128))
    iron=material("Asylum estate gate forged iron",(.065,.057,.052))
    apron,terrain=fit_terrain_extension(ground,soil,road,OUTER,WEST_ROAD,EAST_ROAD,outside_grade,
        "Asylum fitted exterior terrain","Surveyed road and hillside ground beyond the original hospital rim",
        (range(-52,116,4),range(-48,149,4)))
    walls,rows=enclosure(ground,apron,design,brick,stone,wood,iron)
    assert records(ground)==original and terrain["exterior_seam_edges"]==971
    OUT.mkdir(parents=True,exist_ok=True)
    model,collision,blend=(OUT/f"asylum-stair.{ext}" for ext in ("glb","hcs2","blend"))
    fragment=ROOT/"build/asylum-enclosure-fragment.glb"
    additions=[apron,*walls]
    export(fragment,additions)
    append(ROOT/source["glb"],fragment,model,reuse_embedded_images=True)
    native_write(ROOT/source["native"],collision,set(),additions,open_components={apron.name})
    before,after=native_records(ROOT/source["native"]),native_records(collision)
    assert after[:len(before)]==before and len(after)==len(before)+len(additions)
    bpy.ops.file.pack_all()
    bpy.ops.wm.save_as_mainfile(filepath=str(blend),compress=True)
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.import_scene.gltf(filepath=str(model))
    for row in after[len(before):]:
        assert match_geometry(triangles(row),[f for f,_ in records(bpy.context.scene.objects[row[0]])]),row[0]
    export_names=("door-survey.tsv","mission.tsv","services.tsv")
    for name in export_names:
        shutil.copyfile(PRIOR/name,OUT/name)
    arrival=(PRIOR/"arrival.tsv").read_text(encoding="utf-8").splitlines()
    arrival[0]="\t".join(("SOURCE",model.relative_to(ROOT).as_posix(),sha(model),
                           collision.relative_to(ROOT).as_posix(),sha(collision)))
    # The runtime only trusts this enclosure marker after checking the named
    # physical component in the hash-bound collision export.
    arrival.append("PERIMETER\tAsylum outer west closed carriage leaf\t32\t92.4")
    (OUT/"arrival.tsv").write_text("\n".join(arrival)+"\n",encoding="utf-8",newline="\n")
    write_tsv(OUT/"boundary-survey.tsv",[("span","x0","z0","x1","z1","stations","clearance",
                                       "footing_low","crest_min","crest_max"),*rows])
    authority=dict(source,glb=model.relative_to(ROOT).as_posix(),native=collision.relative_to(ROOT).as_posix(),
                   blend=blend.relative_to(ROOT).as_posix(),glb_sha256=sha(model),native_sha256=sha(collision),
                   blend_sha256=sha(blend),fitted_leaf_editor_scene=source["fitted_leaf_editor_scene"],
                   staged_native_door_schedule=(OUT/"door-survey.tsv").relative_to(ROOT).as_posix(),
                   staged_room_bindings=(OUT/"mission.tsv").relative_to(ROOT).as_posix(),
                   staged_services=(OUT/"services.tsv").relative_to(ROOT).as_posix())
    write(OUT/"construction.json",dict(site="ASYLUM",plan_sha256=sha(PLAN),landform=terrain,
          prior_native_parts=len(before),added_matching_parts=len(additions),closed_spans=len(rows),
          gate_width=8,gate_station=92.4,gate_axis=32,source_authority=authority))
    roster=json.loads((PRIOR/"roster.json").read_bytes())
    roster["baseline"]=(PRIOR/"roster.json").relative_to(ROOT).as_posix()
    row=next(r for r in roster["active_sources"] if r["id"]=="ASYLUM")
    saved=row.copy()
    row.update(authority,construction=(OUT/"construction.json").relative_to(ROOT).as_posix())
    assert row["layout"]==saved["layout"]
    next(r for r in roster["sites"] if r["id"]=="ASYLUM")["decision"]="ASYLUM_FOUNDED_PHYSICAL_ENCLOSURE_STAGED"
    write(OUT/"roster.json",roster)
    print("PASS",terrain["exterior_seam_edges"],"exact edge joins,",len(additions),"matched new parts,",len(before),"retained parts")


if __name__=="__main__":
    build()
