"""Build the founded Northwatch windbreak and road closure around the saved station."""
from collections import Counter
import json
import math
from pathlib import Path
import sys

import bpy
from mathutils import Matrix, Vector
from mathutils.geometry import delaunay_2d_cdt

ROOT = Path(__file__).resolve().parents[1]
sys.path[:0] = [str(ROOT / "tools"), str(ROOT / "development/levels/blockouts")]

from append_static_glb import append
from build_circulation_construction import native_write
from build_college_containment import (MeshParts, distance_to_edge_squared, inside, loop_area,
    post_foot, station, strip, surveyed_terrain_loops)
from build_college_electrical import point, world
from build_college_grounds import place_review_van
from build_prison_sally_gate import export
from build_roster_foundations import material
from compose_room_terrain import split_native
from review_roster_macroterrain import records, sha
from verify_college_services import bvh, match_geometry

BASE = ROOT / "development/levels/rebuild"
AUTHORITY = BASE / "cemetery-named-pass/roster.json"
OUT = BASE / "northwatch-boundary-pass/wld-08"
GATE_ASSET = ROOT / "assets/runtime/models/level_camp_boundary_gate.glb"

ROAD_GATE = ((35,85.2),(29,85.2))
RING = (ROAD_GATE[1],(13.7,83.0),(6.5,77),(-2.3,64),(-2.6,51),(-2.2,36),
        (-2.4,20),(-1.05,5),(8.3,-2.4),(28,-4),(44,-3.2),(56,-2.4),(64,2.4),
        (72.2,6.5),(72.2,20.5),(69,33),(67.4,48.5),(65.7,62),(56.5,73),(46,82),ROAD_GATE[0])
OUTER = ((-41,-12),(-23,-33),(18,-39),(65,-40),(96,-27),(106,2),(113,38),(110,67),
         (93,99),(66,123),(38,133),(12,128),(-14,118),(-39,99),(-51,71),(-53,24))


def write(path, content):
    path.write_text(json.dumps(content, indent=2) + "\n", encoding="utf-8", newline="\n")


def relative(path):
    return path.relative_to(ROOT).as_posix()


def closed_ring(points):
    return list(zip(points, (*points[1:], points[0])))


def exterior(original):
    rim, *cuts = surveyed_terrain_loops(original)
    assert len(rim)==1004 and sorted(map(len,cuts))==[20,128]
    rim_edges=closed_ring(rim)
    outer_edges=closed_ring(OUTER)

    def ground(x,z):
        nearest=min(rim,key=lambda p:(x-p[0])**2+(z-p[1])**2)
        weight=min(1,math.hypot(x-nearest[0],z-nearest[1])/23)
        west=3.8*math.exp(-((x+19)/24)**2-((z-46)/47)**2)
        east=5.2*math.exp(-((x-89)/27)**2-((z-11)/37)**2)
        north=2.3*math.exp(-((x-24)/52)**2-((z+26)/21)**2)
        return nearest[2]*(1-weight)+(-.5+west+east+north)*weight

    points=list(rim)+[(x,z,ground(x,z)) for x,z in OUTER]
    offset=len(rim)
    boundaries=[(i,(i+1)%offset) for i in range(offset)]
    boundaries.extend((offset+i,offset+(i+1)%len(OUTER)) for i in range(len(OUTER)))
    road=[]
    road_edges=[]
    for side in (-1,1):
        index=next(i for i,(x,z,_) in enumerate(rim) if (x,z)==(32+side*4,90))
        path=[index]
        for x,z,y in ((33,99,-.45),(35.4,108,-.70),(40,118,-.96),(44,128,-1.20)):
            path.append(len(points))
            points.append((x+side*4,z,y))
        road_edges.extend(zip(path,path[1:]))
        road.append(path)
    road_edges.append((road[0][-1],road[1][-1]))
    throat=[points[i] for i in road[0]+road[1][::-1]]
    seed_edges=rim_edges+outer_edges+[(points[a],points[b]) for a,b in road_edges]
    for x in range(-53,115,5):
        for z in range(-39,133,5):
            if inside(OUTER,x,z) and not inside(rim,x,z) and min(
                    distance_to_edge_squared((x,z),a,b) for a,b in seed_edges)>.06:
                y=ground(x,z)
                if inside(throat,x,z):
                    y=-.233-(z-90)*.025
                points.append((x,z,y))

    _,_,triangles,owners,_,_=delaunay_2d_cdt(
        [Vector(p[:2]) for p in points],boundaries+road_edges,[],0,1e-7)
    assert all(len(ids)==1 for ids in owners)
    vertices=[points[ids[0]] for ids in owners]
    faces=[]
    gravel=[]
    for tri in triangles:
        polygon=[vertices[i] for i in tri]
        x,z=[sum(v[axis] for v in polygon)/3 for axis in (0,1)]
        if inside(OUTER,x,z) and not inside(rim,x,z):
            faces.append(tri[::-1] if loop_area(polygon)>0 else tri)
            gravel.append(inside(throat,x,z))
    counts=Counter(tuple(sorted((a,b))) for face in faces for a,b in closed_ring(face))
    returned={tuple(sorted(owners[i][0] for i in edge)) for edge,count in counts.items() if count==1}
    assert returned=={tuple(sorted(edge)) for edge in boundaries}
    area=sum(abs(loop_area([vertices[i] for i in face])) for face in faces)
    assert abs(area-(abs(loop_area(OUTER))-abs(loop_area(rim))))<.002

    mesh=bpy.data.meshes.new("Northwatch joined valley and service lane")
    mesh.from_pydata([point(x,y,z) for x,z,y in vertices],[],faces)
    mesh.materials.append(original.data.materials[0])
    mesh.materials.append(original.data.materials[1])
    mesh.update()
    uv=mesh.uv_layers.new(name="Metres")
    for face,road_finish in zip(mesh.polygons,gravel):
        face.material_index=int(road_finish)
        for slot in face.loop_indices:
            v=mesh.vertices[mesh.loops[slot].vertex_index].co
            uv.data[slot].uv=(v.x/8,v.y/8)
    for face in original.data.polygons:
        x,_,z=world(original.matrix_world@face.center)
        if 28<x<36 and 85.2<z<=90:
            face.material_index=1
    obj=bpy.data.objects.new(mesh.name,mesh)
    bpy.context.scene.collection.objects.link(obj)
    obj["role"]="Exterior frozen valley joined to the exact saved station rim"
    receipt=dict(joined_rim_edges=len(rim),retained_cut_edges=sorted(map(len,cuts)),
                 exterior_faces=len(faces),road_faces=sum(gravel),area_m2=round(area,3))
    return obj,receipt


def ground_query(objects):
    tree=bvh(objects)

    def grade(x,z):
        hit,_,_,_=tree.ray_cast(point(x,5,z),Vector((0,0,-1)),10)
        assert hit is not None,("Unsupported perimeter footing",x,z)
        return hit.z

    return grade


def samples(a,b,grade,step=.24):
    count=math.ceil(math.dist(a,b)/step)
    return [grade(*station(a,b,i/count)) for i in range(count+1)]


def trim_gate_overlaps(source):
    # The source leaf is made of separate closed timber slats. The slats
    # overlap on their front and back planes, causing in-game depth fighting.
    # Trim the fitted instance to butt joints without replacing source UVs.
    coordinates=[source.matrix_world@v.co for v in source.data.vertices]
    keys=[tuple(round(value,5) for value in point) for point in coordinates]
    adjacent={}
    timber=[p for p in source.data.polygons if p.material_index==0]
    for face in timber:
        items=[keys[i] for i in face.vertices]
        for a,b in closed_ring(items):
            adjacent.setdefault(a,set()).add(b)
            adjacent.setdefault(b,set()).add(a)
    remaining=set(adjacent)
    boards=[]
    while remaining:
        first=remaining.pop()
        group={first}
        todo=[first]
        for a in todo:
            additions=adjacent[a]&remaining
            remaining.difference_update(additions)
            group.update(additions)
            todo.extend(additions)
        lo=[min(p[i] for p in group) for i in range(3)]
        hi=[max(p[i] for p in group) for i in range(3)]
        if .20<hi[0]-lo[0]<.23 and 2.10<hi[2]-lo[2]<2.14:
            boards.append(dict(keys=group,left=lo[0],right=hi[0],lo=lo[0],hi=hi[0]))
    boards.sort(key=lambda b:b["left"])
    assert len(boards)>30,len(boards)
    overlaps=[]
    for a,b in zip(boards,boards[1:]):
        overlap=a["right"]-b["left"]
        assert .018<overlap<.075,(a["right"],b["left"],overlap)
        seam=(a["right"]+b["left"])/2
        a["hi"]=seam
        b["lo"]=seam
        overlaps.append(overlap)
    selected={point:board for board in boards for point in board["keys"]}
    local=source.matrix_world.inverted()
    for vert,p,key in zip(source.data.vertices,coordinates,keys):
        if key in selected:
            side=selected[key]
            p.x=min(max(p.x,side["lo"]),side["hi"])
            vert.co=local@p
    source.data.update()
    return dict(seated_slats=len(boards),overlapping_joints_corrected=len(overlaps),
                source_max_overlap_m=round(max(overlaps),5))


def gate(grade,finishes):
    a,b=ROAD_GATE
    x,z=station(a,b,.5)
    soil=[grade(*p) for p in strip(a,b,.48)]
    y=max(soil)
    earlier=set(bpy.context.scene.objects)
    bpy.ops.import_scene.gltf(filepath=str(GATE_ASSET))
    imported=set(bpy.context.scene.objects)-earlier
    source,=[obj for obj in imported if obj.type=="MESH"]
    trim=trim_gate_overlaps(source)
    bounds=[world(source.matrix_world@v.co) for v in source.data.vertices]
    lo,hi=([fn(p[i] for p in bounds) for i in range(3)] for fn in (min,max))
    center=point((lo[0]+hi[0])/2,lo[1],(lo[2]+hi[2])/2)
    scale=math.dist(a,b)/(hi[0]-lo[0])
    transform=Matrix.Translation(point(x,y-.015,z))@Matrix.Scale(scale,4)@Matrix.Translation(-center)
    closed=bpy.data.objects.new("Northwatch closed vehicle gate",source.data)
    bpy.context.scene.collection.objects.link(closed)
    closed.matrix_world=transform@source.matrix_world
    closed["source_asset"]=GATE_ASSET.stem
    closed["role"]="Closed perimeter vehicle gate at the station approach"
    for item in imported:
        bpy.data.objects.remove(item,do_unlink=True)
    frame=MeshParts(finishes)
    for p in (a,b):
        footprint=post_foot(p,.68)
        frame.prism(footprint,min(grade(*c) for c in footprint)-.40,y+.115,0)
    frame.prism(strip(a,b,.51), min(soil)-.30,y+.012,0)
    return [closed,frame.frame("Northwatch road gate foundations","Founded hinge seats and flush gate sill")],dict(
        asset=relative(GATE_ASSET),sha256=sha(GATE_ASSET),endpoints=ROAD_GATE,ground=y,uniform_scale=scale,slat_fit=trim)


def barrier(grade):
    finishes=(material("Northwatch cast footing",(.34,.386,.41)),
              material("Northwatch zinc-coated windbreak",(.48,.52,.55)),
              material("Northwatch dark steel channels",(.185,.22,.25)))
    pieces=[]
    receipts=[]
    near=[]
    fixed_gate, gate_receipt=gate(grade,finishes)
    for a,b in closed_ring(RING):
        if (a,b)==ROAD_GATE:
            continue
        count=math.ceil(math.dist(a,b)/3.25)
        for i in range(count):
            p,q=station(a,b,i/count),station(a,b,(i+1)/count)
            width=.59
            section=strip(p,q,width)
            soil=samples(p,q,grade)+samples(section[0],section[1],grade)+samples(section[3],section[2],grade)
            low,high=min(soil),max(soil)
            finish=high+.19
            crest=finish+2.02
            parts=MeshParts(finishes)
            parts.prism(section,low-.38,finish,0)
            parts.prism(strip(p,q,.115),finish-.03,crest,1)
            for rail in (finish+.16,finish+1.03,crest-.09):
                parts.prism(strip(p,q,.20),rail-.04,rail+.04,2)
            length=math.dist(p,q)
            dx,dz=(q[0]-p[0])/length,(q[1]-p[1])/length
            for t in (.23,.50,.77):
                x,z=station(p,q,t)
                f=(x-.017*dx,z-.017*dz)
                e=(x+.017*dx,z+.017*dz)
                parts.prism(strip(f,e,.151),finish+.19,crest-.12,2)
            for end in (p,q):
                near.append((end,low-.38,crest))
            number=len(receipts)+1
            name=f"Northwatch windbreak span {number:02}"
            pieces.append(parts.frame(name,"Surveyed foundation and braced windbreak panel"))
            receipts.append(dict(name=name,a=p,b=q,soil_min=low,soil_max=high,soil_samples=len(soil),
                                 footing_low=low-.38,crest=crest,outward=(dz,-dx)))
    columns={}
    for p,bottom,top in near:
        key=tuple(round(x,5) for x in p)
        lo,hi=columns.get(key,(bottom,top))
        columns[key]=(min(lo,bottom),max(hi,top))
    piers=MeshParts(finishes)
    for p,(bottom,top) in columns.items():
        if p in ROAD_GATE:
            continue
        foot=post_foot(p,.67)
        soil=[grade(*q) for q in foot]
        pedestal=max(soil)+.19
        piers.prism(foot,min(min(soil)-.33,bottom),pedestal,0)
        piers.prism(post_foot(p,.236),pedestal-.018,top+.075,2)
    pieces.append(piers.frame("Northwatch braced post line","Founded posts tying every stepped panel junction"))
    return [*pieces,*fixed_gate],receipts,gate_receipt


def capture(paths, layout):
    bpy.ops.wm.open_mainfile(filepath=str(paths["blend"]))
    assert place_review_van(layout["staging"]) > 0
    scene=bpy.context.scene
    bpy.ops.object.camera_add()
    camera=bpy.context.object
    scene.camera=camera
    scene.render.engine="BLENDER_WORKBENCH"
    scene.display.shading.color_type="MATERIAL"
    scene.display.shading.show_cavity=True
    scene.display.shading.cavity_type="BOTH"
    scene.render.resolution_x,scene.render.resolution_y=1440,900
    scene.render.resolution_percentage=100
    folder=OUT/"captures/structure"
    folder.mkdir(parents=True,exist_ok=True)
    for label,eye,target,scale in (
        ("station-perimeter",(89,75,109),(34,0,43),125),
        ("roadhead-and-vehicle-gate",(40,8,98),(32,1.25,82),23),
        ("array-windbreak",(56,1.64,5.5),(59,1.24,-1),0),
    ):
        camera.location=point(*eye)
        camera.rotation_euler=(point(*target)-camera.location).to_track_quat("-Z","Y").to_euler()
        camera.data.type="ORTHO" if scale else "PERSP"
        camera.data.ortho_scale=scale or 1
        camera.data.lens=26
        scene.render.filepath=str(folder/(label+".png"))
        bpy.ops.render.render(write_still=True)


def build():
    roster=json.loads(AUTHORITY.read_bytes())
    source=next(item for item in roster["active_sources"] if item["id"]=="WLD-08")
    prior={key:ROOT/source[key] for key in ("glb","blend","native","layout","construction")}
    authority=json.loads(prior["construction"].read_bytes())
    for key,expected in (("glb",source["glb_sha256"]),("native",source["native_sha256"]),
                         ("blend",authority["blend_sha256"]),("layout",authority["layout_sha256"])):
        assert sha(prior[key])==expected,key
    layout=json.loads(prior["layout"].read_bytes())
    bpy.ops.wm.open_mainfile(filepath=str(prior["blend"]))
    bpy.context.preferences.filepaths.save_version=0
    original=next(obj for obj in bpy.context.scene.objects if "geographic arrival and site clearings" in obj.get("role",""))
    original_name=original.name
    extension, terrain=exterior(original)
    bpy.context.view_layer.update()
    grade=ground_query([original,extension])
    parts, surveys, gate_info=barrier(grade)
    parts.append(extension)
    OUT.mkdir(parents=True,exist_ok=True)
    paths={ext:OUT/f"bounded-station.{ext}" for ext in ("blend","glb","hcs2")}

    fragment=ROOT/"build/northwatch-boundary-fragment.glb"
    export(fragment,[original,*parts])
    append(prior["glb"],fragment,paths["glb"],reuse_embedded_images=True,remove_mesh_names={original.name})
    fragment.unlink()
    changed=native_write(prior["native"],paths["hcs2"],set(),parts,
                         open_components={extension.name,"Northwatch closed vehicle gate"})
    before,after=(split_native(p) for p in (prior["native"],paths["hcs2"]))
    assert after[:len(before)]==before and len(after)-len(before)==len(changed)
    bpy.ops.file.pack_all()
    bpy.ops.wm.save_as_mainfile(filepath=str(paths["blend"]),compress=True)
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.import_scene.gltf(filepath=str(paths["glb"]))
    for name,triangles in changed:
        assert match_geometry([f for f,_ in triangles],[f for f,_ in records(bpy.context.scene.objects[name])]),name
    assert bpy.context.scene.objects.get(original_name) is not None
    rows=[]
    for span in surveys:
        a,b=span["a"],span["b"]
        count=math.ceil(math.dist(a,b)/.61)
        for i in range(count+1):
            x,z=station(a,b,i/count)
            rows.append(("WALL",x,z,*span["outward"],grade(x,z)))
    for i in range(61):
        x,z=station(*ROAD_GATE,i/60)
        rows.append(("GATE",x,z,0,1,grade(x,z)))
    (OUT/"boundary-stations.tsv").write_bytes("".join("\t".join(map(str,row))+"\n" for row in rows).encode())
    receipt=dict(id="WLD-08",layout=relative(prior["layout"]),before_layout=relative(prior["layout"]),
        layout_sha256=sha(prior["layout"]),before_layout_sha256=sha(prior["layout"]),
        source_glb=relative(prior["glb"]),source_glb_sha256=sha(prior["glb"]),
        source_physics=relative(prior["native"]),source_physics_sha256=sha(prior["native"]),
        glb_sha256=sha(paths["glb"]),physics_sha256=sha(paths["hcs2"]),
        before_segment_map=authority["source_segment_map"],source_segment_map=authority["source_segment_map"],
        before_stair_components=authority["stair_components"],stair_components=authority["stair_components"],
        exports={relative(path):sha(path) for path in paths.values()},terrain=terrain,alignment=RING,
        spans=surveys,gate=gate_info,prior_parts=len(before),added_parts=len(changed))
    write(OUT/"construction.json",receipt)
    capture(paths,layout)
    print("PASS Northwatch",len(surveys),"founded spans",len(rows),"stations",len(before),"retained parts")


if __name__ == "__main__":
    build()
