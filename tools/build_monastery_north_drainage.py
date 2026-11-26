"""Fit the two northern interceptors and the three north roof-sump overflows."""
import json
import math
from pathlib import Path
import sys

import bpy

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/"tools"))
from append_static_glb import append,read
from build_circulation_construction import native_write
from build_college_grounds import import_scene,terrain_tree
from build_deliberate_entrances import write
from build_mine_drainage import slab
from build_monastery import rel
from build_monastery_ground_drainage import BASE,SCHEDULES,Swale,height,overlap,refine
from build_prison_sally_gate import export
from build_roster_foundations import material
from compose_room_terrain import split_native
from export_roster_collision import face_keys
from prepare_cemetery_doors import write_tsv
from prune_static_glb import compact
from review_roster_macroterrain import records,sha

PRIOR = BASE/"monastery-court-drainage-pass"
STAGE = BASE/"monastery-north-drainage-pass"
OUT = STAGE/"wld-10"
STEM = "monastery-with-north-drainage"
GROUND = "WLD-10 court and exterior ground"
NEW_GROUND = "WLD-10 surveyed drainage ground"
STATUS = "MONASTERY_NORTHERN_SLOPE_DRAINED_STAGED"
CHANNELS = (
    Swale("western hillside interceptor",(
        (6.58,21.57,.172),(6.55,18.6,.133),(6.57,16.57,.108),(6.62,12,.072),
        (6.52,8,.045),(6.48,4.3,.015),(6.73,1.25,-.027),(7.8,-.48,-.062),
        (10.8,-1.3,-.13),(13.4,-2,-.263),(16.1,-2.35,-.35),(19.7,-2.65,-.401)),.20,.55),
    Swale("vestry east and north interceptor",(
        (44.48,14.63,-.369),(45,12,-.378),(45,8,-.387),(45,4,-.397),
        (44.78,1,-.403),(44.78,.24,-.405),(44.30,-.62,-.407),(43.8,-1.23,-.409),(40,-1.9,-.415),(37,-2.25,-.419),
        (34.3,-2.3,-.424),(30.4,-2.51,-.430),(27.7,-2.75,-.436)),.24,.64),
)
# name, direction of spill, centre, stem of the receiving bed
SUMPS = (
    ("dormitory north","west",7.19,21.57,(6.58,21.57)),
    ("north range west","west",7.19,16.57,(6.57,16.57)),
    ("north range east","north",44.48,15.19,(44.48,14.63)),
)


def sculpt(old):
    source_tree = terrain_tree(old)
    points = [old.matrix_world@vertex.co for vertex in old.data.vertices]
    faces = [(tuple(face.vertices),face.material_index) for face in old.data.polygons]
    bounds = [unit.bounds for unit in CHANNELS]
    # These are broad soil receivers. A 0.42 m target preserves the graded
    # channels without applying the court sump's fine resolution to the hill.
    for p in points:
        p *= .5
    faces = refine(points,faces,[tuple(v*.5 for v in box) for box in bounds])
    for p in points:
        p *= 2
    changed = set()
    for i,v in enumerate(points):
        if not any(overlap((v,),box) for box in bounds):
            continue
        x,z,datum = v.x,-v.y,v.z
        # Leave the foundation contact strip untouched. The swales intercept
        # overland flow beyond it and turn before the northern building faces.
        if (z>1.2 and 7.37<=x<=8.4) or (z>.30 and 43.6<=x<=44.32):
            continue
        top = min(unit.cut(x,z,datum) for unit in CHANNELS)
        if top<datum-1e-5:
            v.z = top
            changed.add(i)
    assert changed
    mesh = bpy.data.meshes.new(NEW_GROUND)
    mesh.from_pydata(points,[],[face for face,_ in faces])
    assert not mesh.validate(clean_customdata=False)
    for mat in old.data.materials:
        mesh.materials.append(mat)
    mat_index = len(mesh.materials)
    mesh.materials.append(material("Monastery northern channel washed aggregate",(.33,.28,.21)))
    uv = mesh.uv_layers.new(name="World metre terrain UV")
    for face,(indices,finish) in zip(mesh.polygons,faces):
        face.material_index = finish
        if any(i in changed for i in indices):
            center = face.center
            if any(unit.nearest(center.x,-center.y)[0]<unit.bed+.09 for unit in CHANNELS):
                face.material_index = mat_index
        for loop,i in zip(face.loop_indices,indices):
            uv.data[loop].uv = points[i].x/8,points[i].y/8
    ground = bpy.data.objects.new(NEW_GROUND,mesh)
    bpy.context.scene.collection.objects.link(ground)
    ground["role"] = "saved editor terrain / surveyed runoff north and court"
    bpy.data.objects.remove(old,do_unlink=True)
    return ground,source_tree,len(changed)


def fitted_sills(terrain):
    scene = bpy.context.scene
    built,removed,proof = [],set(),[]
    for key,direction,x,z,end in SUMPS:
        label = "Monastery "+key
        west = direction=="west"
        rim = scene.objects[label+(" sump surround 1" if west else " sump surround 3")]
        material = rim.data.materials[0]
        elevations = [(rim.matrix_world@v.co).z for v in rim.data.vertices]
        lip,crest = min(elevations),max(elevations)
        transverse = z if west else x
        fixed = x if west else z

        def box(lo,hi,across_lo,across_hi):
            return (lo,across_lo,hi,across_hi) if west else (across_lo,lo,across_hi,hi)

        for name,low,high in (("left",transverse-.27,transverse-.10),
                              ("right",transverse+.10,transverse+.27)):
            built.append(slab(label+" overflow rim "+name,box(fixed-.27,fixed-.195,low,high),
                              lambda x,z:lip,lambda x,z:crest,material,"founded stormwater rim"))
        end_y = height(terrain,*end)
        stair = ((fixed-.193,lip+.021),(fixed-.27,lip+.005),(end[0] if west else end[1],end_y+.022))
        assert stair[0][1]>stair[1][1]>stair[2][1],(label,stair)
        for i,((a,y0),(b,y1)) in enumerate(zip(stair,stair[1:]),1):
            def top(x,z):
                t = ((x if west else z)-a)/(b-a)
                return y0+t*(y1-y0)
            def underside(x,z):
                return min(top(x,z)-.035,lip-.014,height(terrain,x,z)-.018)
            built.append(slab(label+f" spillway stone {i}",box(b,a,transverse-.10,transverse+.10),
                              underside,top,material,"supported overflow sill with gravity fall"))
        removed.add(rim.name)
        bpy.data.objects.remove(rim,do_unlink=True)
        proof.append(dict(inlet=key,channel_end=end,section=stair,receiving_ground=end_y))
    return built,removed,proof


def survey(terrain,previous):
    measured = []
    for channel in CHANNELS:
        rows,distance = [],0
        for (ax,az,_),(bx,bz,_) in zip(channel.sections,channel.sections[1:]):
            length = math.hypot(bx-ax,bz-az)
            count = math.ceil(length/.16)
            for i in range(count):
                fraction = i/count
                x,z = ax+(bx-ax)*fraction,az+(bz-az)*fraction
                rows.append((distance+length*fraction,x,height(terrain,x,z),z))
            distance+=length
        x,z,_ = channel.sections[-1]
        rows.append((distance,x,height(terrain,x,z),z))
        fall = rows[0][2]-rows[-1][2]
        rises = [(round(b[0],3),round(b[2]-a[2],5)) for a,b in zip(rows,rows[1:]) if b[2]-a[2]>.001]
        print(channel.name,"fall",round(fall,4),"local rises",rises)
        assert fall>.05 and not rises,(channel.name,fall,rises)
        measured.append(dict(name=channel.name,length=distance,total_fall=fall,profile=rows))
    probes = [(7.66,z) for z in (2.5,4,6,8,10,12,14,16,18,20)]
    probes += [(44.20,z) for z in (2.5,4,6,8,10,12,14,15)]
    probes += [(x,z) for x in (10,15,20,25,30,35,40,44) for z in (.46,.92)]
    for x,z in probes:
        assert abs(height(terrain,x,z)-height(previous,x,z))<.0002,(x,z)
    return measured,len(probes)


def audit_level_budget(path):
    document,_ = read(path)
    roots = [i for scene in document["scenes"] for i in scene["nodes"]]
    visited = set()
    face_count = 0
    vertex_count = 0
    while roots:
        i=roots.pop()
        if i in visited:
            continue
        visited.add(i)
        node=document["nodes"][i]
        roots.extend(node.get("children",[]))
        if "mesh" not in node:
            continue
        for part in document["meshes"][node["mesh"]]["primitives"]:
            vertices=document["accessors"][part["attributes"]["POSITION"]]["count"]
            indices=document["accessors"][part["indices"]]["count"]
            assert 3<=vertices<=50_000 and 3<=indices<=150_000 and indices%3==0,node["name"]
            face_count+=indices//3
            vertex_count+=vertices
    assert face_count<=200_000 and vertex_count<=500_000
    return dict(triangles=face_count,declared_vertices=vertex_count,bytes=path.stat().st_size)


def build():
    receipt = json.loads((PRIOR/"checkpoint.json").read_bytes())
    roster = json.loads((PRIOR/"roster.json").read_bytes())
    row = next(site for site in roster["active_sources"] if site["id"]=="WLD-10")
    source = {k:ROOT/row[k] for k in ("construction","blend","glb","native")}
    for path in source.values():
        assert sha(path)==receipt["project_bytes_sha256"][rel(path)],path
    meta = json.loads(source["construction"].read_bytes())
    bpy.ops.wm.open_mainfile(filepath=str(source["blend"]))
    bpy.context.preferences.filepaths.save_version = 0
    ground,previous,changed = sculpt(bpy.context.scene.objects[GROUND])
    terrain = terrain_tree(ground)
    measurements,probes = survey(terrain,previous)
    fittings,removed,sills = fitted_sills(terrain)
    parts = [ground,*fittings]

    OUT.mkdir(parents=True,exist_ok=True)
    model,collision,blend = (OUT/(STEM+ext) for ext in (".glb",".hcs2",".blend"))
    fragment = ROOT/"build/monastery-north-drainage-fragment.glb"
    intermediate = ROOT/"build/monastery-north-drainage-unpruned.glb"
    export(fragment,parts)
    retired = {GROUND,*removed}
    append(source["glb"],fragment,intermediate,remove_mesh_names=sorted(retired),reuse_embedded_images=True)
    freed = compact(intermediate,model)
    assert freed>0 and model.stat().st_size<16_000_000
    budget = audit_level_budget(model)
    added = native_write(source["native"],collision,retired,parts,open_components={NEW_GROUND})
    retained = [p for p in split_native(source["native"]) if p[0] not in retired]
    after = split_native(collision)
    assert after[:len(retained)]==retained and len(after)==len(retained)+len(added)
    bpy.ops.file.pack_all()
    bpy.ops.wm.save_as_mainfile(filepath=str(blend),compress=True)
    loaded = import_scene(model)
    for name,triangles in added:
        assert face_keys(records(loaded[name]))==face_keys(triangles),name
    # The old compacted node transforms, materials, UVs and mesh bytes were
    # retained by the static append and resource remapping.
    schedules = (*SCHEDULES,"drainage-survey.tsv","court-drainage.tsv")
    for name in schedules:
        p = PRIOR/"wld-10"/name
        assert sha(p)==receipt["project_bytes_sha256"][rel(p)],p
        (OUT/name).write_bytes(p.read_bytes())
    write_tsv(OUT/"north-drainage.tsv",[("swale","distance","x","native_invert_y","z"),
        *((unit["name"],*station) for unit in measurements for station in unit["profile"])])
    meta.update(status=STATUS,before_construction=rel(source["construction"]),terrain_mesh=NEW_GROUND,
        source_glb=rel(source["glb"]),source_glb_sha256=sha(source["glb"]),
        removed_source_meshes=sorted(retired),retained_native_records=len(retained),
        north_drainage=measurements,north_sumps=sills,north_foundation_grade_probes=probes,
        north_modified_vertices=changed,compacted_inactive_glb_bytes=freed,
        north_matched_components=[name for name,_ in added],north_glb_budget=budget,
        builder=rel(Path(__file__)),builder_sha256=sha(Path(__file__)),
        glb_sha256=sha(model),physics_sha256=sha(collision),blend_sha256=sha(blend))
    construction = OUT/"construction.json"
    write(construction,meta)
    row.update(construction=rel(construction),blend=rel(blend),glb=rel(model),native=rel(collision),
               glb_sha256=sha(model),native_sha256=sha(collision))
    next(site for site in roster["sites"] if site["id"]=="WLD-10")["decision"] = STATUS
    roster["baseline"] = rel(PRIOR/"roster.json")
    write(STAGE/"roster.json",roster)
    write_tsv(STAGE/"source-hashes.tsv",[(path.name,sha(path)) for path in
        (model,collision,*(OUT/name for name in SCHEDULES[:3]))])
    print("PASS two north channels, three founded spillways,",probes,
          "unchanged foundation probes and",freed,"discarded inactive GLB bytes")


if __name__=="__main__":
    build()
