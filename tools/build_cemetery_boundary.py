"""Fit the cemetery and mortuary perimeter to the retained saved terrain and roadheads."""
import json
import math
from collections import Counter
from pathlib import Path
import sys

import bpy
from mathutils import Matrix, Vector
from mathutils.geometry import delaunay_2d_cdt

ROOT = Path(__file__).resolve().parents[1]
sys.path[:0] = [str(ROOT / "tools"), str(ROOT / "development/levels/blockouts")]

from append_static_glb import append
from build_circulation_construction import native_write
from build_college_grounds import place_review_van
from build_college_containment import (MeshParts, distance_to_edge_squared, inside, loop_area,
    post_foot, station, strip, surveyed_terrain_loops, terrain_reader)
from build_prison_sally_gate import export
from build_roster_foundations import material
from compose_room_terrain import split_native
from review_roster_macroterrain import records, sha
from verify_college_services import match_geometry

BASE = ROOT / "development/levels/rebuild"
PRIOR = BASE / "cemetery-grounds-pass"
OUTPUT = BASE / "cemetery-boundary-pass"
GATE = ROOT / "development/levels/primitive-props-v1/models/level_blockout_cemetery_gate.glb"
SITES = ("wld-07", "wld-07-mortuary")

# Walkable site functions set the line. The burial enclosure follows two
# raised shoulders past the ossuary flank and closes around the narrower road.
WEST = ((18,84.8),(13.2,79.3),(10,72),(7,67),(4.45,60.5),(4.5,44),(5,36),
        (3.8,27),(4.35,14),(7.5,7.5),(12,3.5),(15,-1.8),(24.5,-2.1),(34.5,-1.5),
        (41.5,1.4),(47,6.2),(52.5,11),(54.2,20),(53.7,29))
BASE_EAST = ((53.8,37.6),(56.1,42),(56.1,53),(53.75,60),(48.4,66.5),(42.6,69),
             (38.65,74),(37.8,81),(35.55,85))
ANNEX_EAST = ((57.6,32.8),(67,33.4),(73,34.4),(83.7,34.4),(87.1,39.4),(87.1,65.4),
              (86.4,73),(84.9,80.3),(80.8,84.9),(74.4,85))
ANNEX_SOUTH = ((56.1,85),(52,84.1),(46.4,83.25),(43.4,83),(40.3,82.25),(38.1,83.6),(35.55,85))
GATE_SCALE = {"road": 1.32, "receiving": 1.45}
PIER_X = 1.46


def gate_ends(x, z, style):
    dx = PIER_X * GATE_SCALE[style]
    return (x + dx, z), (x - dx, z)


def alignment(annex):
    road = gate_ends(28, 85, "road")
    receiving = gate_ends(66, 85, "receiving")
    if annex:
        ring = (*WEST, *ANNEX_EAST, *receiving, *ANNEX_SOUTH, *road)
        gates = {receiving: "receiving", road: "road"}
    else:
        ring = (*WEST, *BASE_EAST, *road)
        gates = {road: "road"}
    return ring, gates


def surface(terrain, annex):
    rim, *cuts = surveyed_terrain_loops(terrain)
    assert sorted(map(len, cuts)) == ([36, 40, 64, 64] if annex else [36, 40, 64])
    east = 89 if annex else 57
    outer = [(-47,-26),(-19,-47),(22,-50),(57,-43),(east+33,-24),(east+47,17),
             (east+51,64),(east+36,107),(east+5,136),(41,147),(19,142),(-12,135),
             (-37,114),(-46,82),(-52,43),(-52,6)]

    def grade(x, z):
        closest = min(rim, key=lambda point: (x - point[0]) ** 2 + (z - point[1]) ** 2)
        distance = math.hypot(x - closest[0], z - closest[1])
        weight = min(1, distance / 13)
        west_hill = 4.5 * math.exp(-((x + 20) / 28) ** 2 - ((z - 28) / 65) ** 2)
        east_hill = 3.4 * math.exp(-((x - east - 20) / 25) ** 2 - ((z - 47) / 53) ** 2)
        north_hill = 2.3 * math.exp(-((z + 28) / 23) ** 2) * (.8 + .19 * math.sin(x * .11))
        distant = -.33 + west_hill + east_hill + north_hill
        return (1 - weight) * closest[2] + weight * distant

    points = list(rim) + [(x, z, grade(x, z)) for x, z in outer]
    offset = len(rim)
    edges = [(i, (i + 1) % offset) for i in range(offset)]
    edges += [(offset+i, offset+(i+1) % len(outer)) for i in range(len(outer))]
    boundary_edges = list(edges)
    roads = []
    lane_centers = (28,66) if annex else (28,)
    road_finish = terrain.data.materials.find("Compacted burial-ground gravel")
    road_edges = []
    for lane in lane_centers:
        width = 2.05 if lane == 28 else 2.25
        side_paths = []
        for side in (-1,1):
            start = min((i for i,p in enumerate(rim) if p[1] == 88),
                        key=lambda i: abs(rim[i][0] - (lane + side*width)))
            assert abs(rim[start][0]-lane-side*width)<.55
            branch = [start]
            drifts = ((97,1.1),(112,4.2),(125,7.8),(135,12.1)) if lane == 28 else (
                      (97,1.8),(111,5.4),(123,10.8),(132,16.7))
            for z,dx in drifts:
                x = lane + dx + side*width
                branch.append(len(points))
                points.append((x,z,grade(x,z)))
            road_edges.extend(zip(branch,branch[1:]))
            side_paths.append(branch)
        first,last=side_paths
        road_edges.append((first[-1],last[-1]))
        roads.append([points[idx] for idx in first+last[::-1]])
        x0,x1=points[first[0]][0],points[last[0]][0]
        for face in terrain.data.polygons:
            x,z=face.center.x,-face.center.y
            if x0 < x < x1 and 84 < z < 88:
                face.material_index=road_finish

    for z in range(-45, 145, 5):
        for x in range(-49, east + 51, 5):
            p = (x,z)
            if not inside(outer, x, z) or inside(rim, x, z):
                continue
            if min(distance_to_edge_squared(p, a, b) for a,b in zip(rim,rim[1:]+rim[:1])) < .06:
                continue
            if min(distance_to_edge_squared(p, a, b) for a,b in zip(outer,outer[1:]+outer[:1])) < .06:
                continue
            if min(distance_to_edge_squared(p, points[a], points[b]) for a,b in road_edges) < .06:
                continue
            points.append((x, z, grade(x, z)))

    _, _, raw, indices, _, _ = delaunay_2d_cdt([Vector(p[:2]) for p in points], edges+road_edges, [], 0, 1e-7)
    assert all(len(ids) == 1 for ids in indices)
    locations = [points[ids[0]] for ids in indices]
    faces = []
    for face in raw:
        p = [locations[i] for i in face]
        x,z = [sum(v[axis] for v in p)/3 for axis in (0,1)]
        if not inside(outer,x,z) or inside(rim,x,z):
            continue
        if loop_area(p) > 0:
            face.reverse()
        faces.append(face)
    edge_counts = Counter(tuple(sorted((a,b))) for face in faces for a,b in zip(face,face[1:]+face[:1]))
    boundary = {tuple(sorted(indices[i][0] for i in edge)) for edge,count in edge_counts.items() if count == 1}
    assert boundary == {tuple(sorted(edge)) for edge in boundary_edges}
    area = sum(abs(loop_area([locations[i] for i in face])) for face in faces)
    assert abs(area - (abs(loop_area(outer)) - abs(loop_area(rim)))) < .002

    mesh = bpy.data.meshes.new("Joined cemetery exterior landform")
    mesh.from_pydata([(x,-z,y) for x,z,y in locations], [], faces)
    mesh.update()
    mesh.materials.append(material("Cemetery exterior woodland loam", (.228,.261,.196)))
    mesh.materials.append(terrain.data.materials["Compacted burial-ground gravel"])
    uv = mesh.uv_layers.new(name="World metre terrain UV")
    for face in mesh.polygons:
        assert face.normal.z > 0
        x,z = face.center.x,-face.center.y
        if any(inside(road,x,z) for road in roads):
            face.material_index = 1
        for slot in face.loop_indices:
            v = mesh.vertices[mesh.loops[slot].vertex_index].co
            uv.data[slot].uv = (v.x/8,v.y/8)
    obj = bpy.data.objects.new(mesh.name, mesh)
    bpy.context.scene.collection.objects.link(obj)
    obj["role"] = "Exterior woodland terrain joined to the exact original rim"
    return obj, dict(matched_rim_edges=len(rim), retained_cut_edges=sorted(map(len,cuts)),
                     triangles=len(faces), square_metres=round(area,3), continuous_road_throats=len(roads))


def build_gate(style, endpoints, grade, finishes):
    a,b = endpoints
    x,z = station(a,b,.5)
    scale = GATE_SCALE[style]
    prior = set(bpy.context.scene.objects)
    bpy.ops.import_scene.gltf(filepath=str(GATE))
    imported = set(bpy.context.scene.objects)-prior
    source, = [o for o in imported if o.type == "MESH"]
    grade_levels = [grade(*p) for p in strip(b,a,.46)]
    assert all(y is not None for y in grade_levels), style
    low, high = min(grade_levels), max(grade_levels)
    elevation = high + .012
    frame = Matrix.Translation((x,-z,elevation)) @ Matrix.Diagonal((scale,1,1,1))
    pose = frame @ source.matrix_world
    gate = bpy.data.objects.new(f"Cemetery closed {style} gate", source.data)
    bpy.context.scene.collection.objects.link(gate)
    gate.matrix_world = pose
    gate["source_asset"] = GATE.stem
    gate["role"] = "Fixed closed outer lane gate with retained source mesh materials"
    for obj in imported:
        bpy.data.objects.remove(obj, do_unlink=True)

    seat = MeshParts(finishes)
    for p in (a,b):
        soils = [grade(*q) for q in post_foot(p,.65)]
        assert all(y is not None for y in soils), (style,p,soils)
        seat.prism(post_foot(p,.65), min(soils)-.32, elevation+.06, 0)
    seat.prism(strip(b,a,.50), low-.23, elevation+.025, 1)
    sill = seat.frame(f"Cemetery founded {style} gate sill", "Bearing seats, buried gate footing and flush road threshold")
    return [gate,sill], dict(style=style, centers=[a,b], source=GATE.relative_to(ROOT).as_posix(),
                              sha256=sha(GATE), elevation=elevation, grade_min=low, grade_max=high, scale_x=scale)


def wall(ring, gates, grade):
    finishes = (material("Cemetery grey foundation stone",(.255,.258,.233)),
                material("Cemetery weathered limestone",(.39,.385,.337)),
                material("Cemetery dressed pale coping stone",(.49,.468,.40)))
    pieces, surveyed, gate_receipts = [], [], []
    pier_seats = {}
    for a,b in zip(ring,ring[1:]+ring[:1]):
        if (a,b) in gates:
            objects, receipt = build_gate(gates[a,b], (a,b), grade, finishes)
            pieces.extend(objects)
            gate_receipts.append(receipt)
            continue
        count = max(1,math.ceil(math.dist(a,b)/5.4))
        for i in range(count):
            p,q = station(a,b,i/count),station(a,b,(i+1)/count)
            length = math.dist(p,q)
            dx,dz = (q[0]-p[0])/length,(q[1]-p[1])/length
            soil = []
            samples = math.ceil(length / .20)
            for j in range(samples+1):
                x,z = station(p,q,j/samples)
                values = [grade(x + dz*side,z - dx*side) for side in (-.30,0,.30)]
                assert all(y is not None for y in values), ("No wall footing soil",p,q,x,z,values)
                soil.extend(values)
            lo,hi = min(soil),max(soil)
            floor = lo-.32
            top = hi+2.39
            first=(p[0]-.023*dx,p[1]-.023*dz)
            last=(q[0]+.023*dx,q[1]+.023*dz)
            masonry = MeshParts(finishes)
            masonry.prism(strip(first,last,.57),floor,hi+.32,0)
            masonry.prism(strip(first,last,.47),hi+.31,top,1)
            masonry.prism(strip(first,last,.56),top-.018,top+.13,2)
            side=(dz,-dx)
            name=f"Cemetery boundary masonry {len(surveyed)+1:02}"
            pieces.append(masonry.frame(name,"Founded mortared wall and weathered coping"))
            surveyed.append(dict(name=name,a=p,b=q,grade_min=lo,grade_max=hi,
                                 footing_bottom=floor,crest=top+.13,samples=len(soil),outward=side))
            for end in (p,q):
                key=tuple(round(v,6) for v in end)
                old=pier_seats.get(key,(floor,top))
                pier_seats[key]=(min(old[0],floor),max(old[1],top))
    reserved = {p for ends in gates for p in ends}
    piers = MeshParts(finishes)
    for p, (floor,top) in pier_seats.items():
        if any(math.dist(p, q)<.001 for q in reserved):
            continue
        piers.prism(post_foot(p,.65),floor,top+.14,1)
        piers.prism(post_foot(p,.74),top+.13,top+.26,2)
    pieces.append(piers.frame("Cemetery enclosure bonded piers","Grounded wall junctions and intermediate buttresses"))
    return pieces, surveyed, gate_receipts


def write_stations(path, spans, gates, grade):
    rows = []
    for span in spans:
        a,b=span["a"],span["b"]
        count=math.ceil(math.dist(a,b)/.56)
        for i in range(count+1):
            x,z=station(a,b,i/count)
            rows.append(("WALL",x,z,*span["outward"],grade(x,z)))
    for gate in gates:
        a,b=gate["centers"]
        count=math.ceil(math.dist(a,b)/.095)
        for i in range(count+1):
            x,z=station(a,b,i/count)
            rows.append(("GATE",x,z,0,1,grade(x,z)))
    path.write_bytes("".join("\t".join(map(str,row))+"\n" for row in rows).encode())


def capture(out, scene, annex):
    bpy.ops.object.camera_add()
    camera=bpy.context.object
    scene.camera=camera
    scene.render.engine="BLENDER_WORKBENCH"
    scene.display.shading.color_type="MATERIAL"
    scene.display.shading.show_cavity=True
    scene.display.shading.cavity_type="BOTH"
    scene.render.resolution_x,scene.render.resolution_y=1540,980
    scene.render.resolution_percentage=100
    for name, eye, target, size in (
        ("boundary-overview", (62 if annex else 48, -111, 78), (46 if annex else 28, -43, .1), 133 if annex else 112),
        ("founded-road-gate", (36,-93,9), (28,-80,1.0), 27),
        ("arrival-inner-gate", (22,-72,1.62), (28,-83,1.30), 0),
    ):
        camera.location=eye
        camera.rotation_euler=(Vector(target)-camera.location).to_track_quat("-Z","Y").to_euler()
        camera.data.type="ORTHO" if size else "PERSP"
        camera.data.ortho_scale=size or 1
        camera.data.lens=25
        path=out/"captures"/"structure"/f"{name}.png"
        path.parent.mkdir(parents=True,exist_ok=True)
        scene.render.filepath=str(path)
        bpy.ops.render.render(write_still=True)


def build(site):
    annex=site.endswith("mortuary")
    source=PRIOR/site/"authored-grounds"
    authority=json.loads((source.parent/"construction.json").read_bytes())
    for extension,key in (("glb","glb"),("hcs2","physics"),("blend","blend")):
        assert sha(source.with_suffix("."+extension))==authority[key+"_sha256"]
    bpy.ops.wm.open_mainfile(filepath=str(source.with_suffix(".blend")))
    bpy.context.preferences.filepaths.save_version=0
    scene=bpy.context.scene
    terrain=next(obj for obj in scene.objects if obj.get("role","").startswith("saved editor terrain"))
    rim,rim_receipt=surface(terrain,annex)
    ring, gate_schedule=alignment(annex)
    grade=terrain_reader(terrain)
    pieces, survey, gates=wall(ring,gate_schedule,grade)
    pieces.append(rim)
    out=OUTPUT/site
    out.mkdir(parents=True,exist_ok=True)
    write_stations(out/"boundary-stations.tsv",survey,gates,grade)
    paths={ext:out/f"bounded-grounds.{ext}" for ext in ("blend","glb","hcs2")}
    fragment=ROOT/"build/cemetery-boundary-fragment.glb"
    export(fragment,[*pieces,terrain])
    append(source.with_suffix(".glb"),fragment,paths["glb"],reuse_embedded_images=True,
           remove_mesh_names={terrain.name})
    fragment.unlink()
    changed=native_write(source.with_suffix(".hcs2"),paths["hcs2"],set(),pieces,open_components={rim.name})
    prior, current=(split_native(p) for p in (source.with_suffix(".hcs2"),paths["hcs2"]))
    assert current[:len(prior)]==prior and len(current)-len(prior)==len(changed)
    bpy.ops.file.pack_all()
    bpy.ops.wm.save_as_mainfile(filepath=str(paths["blend"]),compress=True)
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.import_scene.gltf(filepath=str(paths["glb"]))
    for name,triangles in changed:
        assert match_geometry([face for face,_ in triangles],[face for face,_ in records(bpy.context.scene.objects[name])]),name
    rel=lambda path: path.relative_to(ROOT).as_posix()
    layout_path=source.parent/"authored-layout.json"
    layout_hash=sha(layout_path)
    assert layout_hash==authority["layout_sha256"]
    glb_hash,native_hash=sha(paths["glb"]),sha(paths["hcs2"])
    receipt=dict(id=site.upper(), source_glb=rel(source.with_suffix(".glb")),
        source_physics=rel(source.with_suffix(".hcs2")), source_glb_sha256=authority["glb_sha256"],
        source_physics_sha256=authority["physics_sha256"], source_segment_map=authority["source_segment_map"],
        before_segment_map=authority["source_segment_map"], stair_components=authority["stair_components"],
        before_stair_components=authority["stair_components"], layout=rel(layout_path), before_layout=rel(layout_path),
        layout_sha256=layout_hash, before_layout_sha256=layout_hash, glb_sha256=glb_hash, physics_sha256=native_hash,
        terrain=rim_receipt, perimeter=ring, gates=gates, span_surveys=survey, retained_parts=len(prior),
        added_parts=len(changed), exports={rel(p):sha(p) for p in paths.values()})
    (out/"construction.json").write_text(json.dumps(receipt,indent=2)+"\n",encoding="utf-8",newline="\n")
    # Add the exact runtime van to the inspection scene only.
    layout=json.loads((source.parent/"authored-layout.json").read_bytes())
    bpy.ops.wm.open_mainfile(filepath=str(paths["blend"]))
    assert place_review_van(layout["staging"]) > 0
    capture(out,bpy.context.scene,annex)
    print("PASS",site,len(survey),"founded masonry spans, exact retained native parts",len(prior))


if __name__ == "__main__":
    for site in SITES:
        build(site)
