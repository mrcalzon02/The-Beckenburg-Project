"""Extend the saved Monastery landform and contain the estate with founded masonry."""
from collections import Counter
import json
import math
from pathlib import Path
import sys

import bpy
import math
from mathutils import Vector
from mathutils.geometry import delaunay_2d_cdt

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT/"tools"))
from append_static_glb import append
from build_circulation_construction import native_write
from build_college_containment import distance_to_edge_squared, inside, loop_area, station, surveyed_terrain_loops
from build_college_grounds import import_scene, terrain_tree
from build_college_electrical import point
from build_deliberate_entrances import write
from build_mine_drainage import mesh_object, slab
from build_monastery import rel
from build_monastery_ground_drainage import BASE, SCHEDULES
from build_monastery_landscape import GROUND
from build_monastery_north_drainage import audit_level_budget
from build_prison_sally_gate import export
from build_roster_foundations import material
from compose_room_terrain import split_native
from prepare_cemetery_doors import write_tsv
from prune_static_glb import compact
from review_roster_macroterrain import records, sha
from verify_college_services import bvh, match_geometry

PRIOR = BASE/"monastery-loose-pass"
STAGE = BASE/"monastery-perimeter-pass"
OUT = STAGE/"wld-10"
PLAN = ROOT/"development/levels/edge-sky-plans/wld-10/plan.json"
STEM = "monastery-with-perimeter"
STATUS = "MONASTERY_FOUNDED_PERIMETER_SOURCE_STAGED"
GATE_A, GATE_B = (36,94.18), (28,94.18)
OUTER = [(-55,-31),(-22,-47),(28,-53),(81,-49),(116,-32),(141,19),
         (147,63),(133,105),(102,128),(65,149),(38,151),(31,154),(24,152),
         (-15,143),(-41,123),(-60,93),(-65,43)]
ROAD_WEST = [(28,94),(28,108),(29.8,126),(31,154)]
ROAD_EAST = [(36,94),(36,108),(37.8,126),(38,151)]


def outside_grade(x,z,rim):
    nearest = min(rim,key=lambda p:(x-p[0])**2+(z-p[1])**2)
    distance = math.hypot(x-nearest[0],z-nearest[1])
    blend = min(1,distance/16)
    west = 7.0*math.exp(-((x+35)/24)**2-((z-36)/62)**2)
    north = 5.1*math.exp(-((z+30)/15)**2)*(.75+.25*math.cos((x-18)/18))
    ravine = -9.5*math.exp(-((x-115)/23)**2-((z-50)/48)**2)
    far = -.72+west+north+ravine
    if z>=94:
        center=32+max(0,min(2,z-108))*.25
        fade=max(0,min(1,(7-abs(x-center))/3))
        far += (-.32-.049*(z-94)-far)*fade
    return nearest[2]*(1-blend)+far*blend


def make_apron(ground,soil,road):
    return fit_terrain_extension(ground,soil,road,OUTER,ROAD_WEST,ROAD_EAST,outside_grade,
        "Monastery fitted exterior terrain","Existing approach joins the western slope, northern ridge and eastern ravine",
        (range(-60,145,4),range(-48,151,4)))


def fit_terrain_extension(ground, soil, road, outer, west_road, east_road, grade, name, role, grid):
    road_outline = west_road + east_road[::-1]
    rim,*cuts = surveyed_terrain_loops(ground)
    points = list(rim)+[(x,z,grade(x,z,rim)) for x,z in outer]
    constraints = [(i,(i+1)%len(rim)) for i in range(len(rim))]
    constraints += [(len(rim)+i,len(rim)+(i+1)%len(outer)) for i in range(len(outer))]
    road_edges=[]
    for side in (west_road,east_road):
        start,=[i for i,p in enumerate(points) if p[:2]==side[0]]
        end,=[i for i,p in enumerate(points) if p[:2]==side[-1]]
        inner=list(range(len(points),len(points)+len(side)-2))
        points.extend((x,z,grade(x,z,rim)) for x,z in side[1:-1])
        road_edges.extend(zip([start,*inner],[*inner,end]))
    constraints_and_road=constraints+road_edges
    for z in grid[1]:
        for x in grid[0]:
            if not inside(outer,x,z) or inside(rim,x,z):
                continue
            if min((x-p[0])**2+(z-p[1])**2 for p in rim)<.24:
                continue
            if any(distance_to_edge_squared((x,z),points[a],points[b])<.03
                   for a,b in constraints_and_road[len(rim):]):
                continue
            points.append((x,z,grade(x,z,rim)))
    _,_,triangles,original,_,_ = delaunay_2d_cdt(
        [Vector(p[:2]) for p in points],constraints_and_road,[],0,1e-7)
    assert all(len(ids)==1 for ids in original)
    vertices = [points[ids[0]] for ids in original]
    faces = []
    for face in triangles:
        polygon = [vertices[v] for v in face]
        x,z = (sum(p[i] for p in polygon)/3 for i in (0,1))
        if not inside(outer,x,z) or inside(rim,x,z):
            continue
        faces.append(face[::-1] if loop_area(polygon)>0 else face)
    incidence = Counter(tuple(sorted((a,b))) for face in faces for a,b in zip(face,face[1:]+face[:1]))
    actual = {tuple(sorted(original[i][0] for i in pair)) for pair,count in incidence.items() if count==1}
    assert actual=={tuple(sorted(edge)) for edge in constraints}, "Open or split exterior terrain seam"
    joined_edges={tuple(sorted(original[i][0] for i in pair)) for pair in incidence}
    assert all(tuple(sorted(edge)) in joined_edges for edge in road_edges), "Unconstrained road finish"
    area=sum(abs(loop_area([vertices[v] for v in face])) for face in faces)
    assert abs(area-(abs(loop_area(outer))-abs(loop_area(rim))))<.001
    mesh = bpy.data.meshes.new(name)
    mesh.from_pydata([(x,-z,y) for x,z,y in vertices],[],faces)
    mesh.materials.append(soil)
    mesh.materials.append(road)
    mesh.update()
    for face in mesh.polygons:
        x,z = face.center.x,-face.center.y
        if inside(road_outline,x,z):
            face.material_index = 1
    obj = bpy.data.objects.new(mesh.name,mesh)
    bpy.context.scene.collection.objects.link(obj)
    obj["role"] = role
    return obj,dict(exterior_seam_edges=len(rim),preserved_interior_edges=[len(p) for p in cuts],
                    exterior_area_m2=round(area,2),new_faces=len(faces),road_boundary_constraints=len(road_edges))


def left_normal(a,b):
    dx,dz = b[0]-a[0],b[1]-a[1]
    distance=math.hypot(dx,dz)
    return -dz/distance,dx/distance


def miter(before,p,after):
    left,right=left_normal(before,p),left_normal(p,after)
    scale=1+sum(a*b for a,b in zip(left,right))
    assert scale>.6
    return tuple((a+b)/scale for a,b in zip(left,right))


def offset(p,n,distance):
    return p[0]+n[0]*distance,p[1]+n[1]*distance


def profile_mesh(name, sections, finish, purpose):
    stride = len(sections[0])
    vertices=[(x,-z,y) for section in sections for x,z,y in section]
    faces=[tuple(range(stride-1,-1,-1))]
    for i in range(len(sections)-1):
        for j in range(stride):
            a=i*stride+j
            b=i*stride+(j+1)%stride
            faces.append((a,b,b+stride,a+stride))
    faces.append(tuple(range(len(vertices)-stride,len(vertices))))
    return mesh_object(name,vertices,faces,finish,purpose)


def wall_section(points, grade, wall_height, thickness):
    sections = []
    bottoms,tops = [],[]
    for p,n in points:
        pairs = [offset(p,n,thickness*f) for f in (-.63,-.5,0,.5,.63)]
        ys = [grade(*q) for q in pairs]
        bottom,top = min(ys)-.31,max(ys)+wall_height
        left,right = pairs[1],pairs[3]
        cap_left,cap_right = pairs[0],pairs[4]
        sections.append([(x,z,y) for (x,z),y in (
            (left,bottom),(right,bottom),(right,top-.14),(cap_right,top-.14),
            (cap_right,top-.065),(p,top),(cap_left,top-.065),(cap_left,top-.14),(left,top-.14))])
        bottoms.append(bottom)
        tops.append(top)
    return sections,min(bottoms),min(tops),max(tops)


def fixed_slab(name, bounds, bottom, top, finish, purpose):
    return slab(name,bounds,lambda *_:bottom,lambda *_:top,finish,purpose)


def make_boundary(ground,apron,plan,stone,timber,iron):
    saved,extended=terrain_tree(ground),terrain_tree(apron)

    def grade(x,z):
        origin=Vector((x,-z,24))
        downward=Vector((0,0,-1))
        contact,*_=saved.ray_cast(origin,downward,64)
        if contact is None:
            contact,*_=extended.ray_cast(origin,downward,64)
        assert contact is not None,("Missing soil bearing",x,z)
        return contact.z

    ring = [tuple(p) for p in plan["edge_envelope"]]
    ring[17:19] = [GATE_A,GATE_B]
    normals=[miter(ring[i-1],p,ring[(i+1)%len(ring)]) for i,p in enumerate(ring)]
    parts=[]
    receipt=[]
    ring_pairs=list(zip(ring,ring[1:]+ring[:1]))
    for i,(a,b) in enumerate(ring_pairs):
        if (a,b)==(GATE_A,GATE_B):
            continue
        treatment=plan["edges"][i]["treatment"]
        thick = .72 if treatment=="bank" else .62
        clearance = 2.20 if treatment=="bank" else 2.06
        divisions=math.ceil(math.dist(a,b)/.45)
        normal=left_normal(a,b)
        stations=[(station(a,b,j/divisions),normal) for j in range(divisions+1)]
        stations[0]=(a,normals[i])
        stations[-1]=(b,normals[(i+1)%len(ring)])
        cross,bottom,crest_min,crest_max = wall_section(stations,grade,clearance,thick)
        name=f"Monastery estate {treatment} masonry {i+1:02}"
        parts.append(profile_mesh(name,cross,stone,"Mortared wall with buried footing and pitched coping"))
        receipt.append(dict(name=name,start=a,end=b,stations=len(cross),exposed_height=clearance,
                            foundation_low=round(bottom,4),crest=[round(crest_min,4),round(crest_max,4)]))
    # A fixed estate carriage gate sits outside the cargo apron. Each leaf
    # bears at a raised masonry pier. Stone returns enter the pier footprints.
    z=GATE_A[1]
    base=min(grade(x,z+dz) for x in range(28,37) for dz in (-.30,0,.30))
    top=max(grade(x,z) for x in range(28,37))+2.60
    for label,x in (("west",27.48),("east",36.52)):
        lowest=min(grade(x+dx,z+dz) for dx in (-.52,0,.52) for dz in (-.52,0,.52))
        parts.append(fixed_slab(f"Monastery outer {label} gate pier",(x-.52,z-.52,x+.52,z+.52),
                          lowest-.38,top+.17,stone,"Founded gate pier with fixed outer leaf"))
        parts.append(fixed_slab(f"Monastery outer {label} pier coping",(x-.60,z-.60,x+.60,z+.60),
                          top+.17,top+.31,stone,"Overhanging headstone with drip edge"))
    for label,x0,x1,hinge_x in (("west",27.91,32.012,28),("east",31.988,36.09,36)):
        parts.append(fixed_slab(f"Monastery outer {label} closed carriage leaf",(x0,z-.16,x1,z+.16),
                          base-.065,top-.17,timber,"Closed timber gate on surveyed sloping threshold"))
        for i,y in enumerate((base+.36,base+1.30,top-.34),1):
            parts.append(fixed_slab(f"Monastery outer {label} gate brace {i}",(x0,z-.22,x1,z-.148),
                              y,y+.12,timber,"Exterior carriage gate cross brace"))
        hinge_inside = hinge_x+(.78 if label=="west" else -.78)
        hinge_outer = hinge_x+(-.19 if label=="west" else .19)
        for i,y in enumerate((base+.41,top-.30),1):
            parts.append(fixed_slab(f"Monastery outer {label} hinge strap {i}",
                          (min(hinge_inside,hinge_outer),z-.245,max(hinge_inside,hinge_outer),z-.205),
                          y-.042,y+.042,iron,"Visible strap connects the timber leaf to its stone pier"))
    parts.append(fixed_slab("Monastery outer gate centre astragal",(31.961,z-.194,32.034,z-.154),
                            base+.068,top-.20,timber,"Timber moulding covers the central weather joint"))
    receipt.append(dict(name="fixed outer carriage gate",start=GATE_A,end=GATE_B,
                        stations=9,exposed_height=round(top-.17-base,4),foundation_low=round(base-.065,4),
                        crest=[round(top-.17,4),round(top+.17,4)]))
    return parts,receipt


def turned_bell_part(name, x, z, profile, mat, role):
    segments=24
    verts=[point(x+r*math.cos(i*math.tau/segments),y,z+r*math.sin(i*math.tau/segments))
           for y,r in profile for i in range(segments)]
    faces=[]
    for row in range(len(profile)):
        a,b=row*segments,((row+1)%len(profile))*segments
        for i in range(segments):
            j=(i+1)%segments
            faces.append((a+i,a+j,b+j,b+i))
    return mesh_object(name,verts,faces,mat,role)


def fit_bell():
    floor=bvh([obj for obj in bpy.context.scene.objects if obj.type=="MESH"
               and "bell floor around upper stair well" in obj.name])
    oak=material("Monastery weathered bell frame oak",(.19,.115,.061))
    bronze=material("Monastery dark cast bell bronze",(.33,.209,.090))
    hemp=material("Monastery twisted bell rope hemp",(.34,.24,.13))
    parts=[]
    for x in (52.19,54.21):
        base=(x-.16,27.77,x+.16,28.09)
        for px in (base[0],base[2]):
            for z in (base[1],base[3]):
                hit,n,_,_=floor.ray_cast(point(px,6.18,z),point(0,-1,0),.27)
                assert hit is not None and abs(hit.z-6)<.005 and n.z>.98,(px,z)
        parts.append(fixed_slab(f"Monastery bell oak post {x}",base,6,8.56,oak,"floor-founded bell bearing"))
    parts.append(fixed_slab("Monastery bell bearing beam",(52.03,27.76,54.37,28.10),
                            8.48,8.74,oak,"crossbeam on bell posts"))
    parts.append(fixed_slab("Monastery bell rope pulley beam",(53.13,26.70,53.27,27.94),
                            8.56,8.69,oak,"bell rope pulley support"))
    parts.append(fixed_slab("Monastery bell hanger",(53.10,27.82,53.30,28.04),
                            8.37,8.51,bronze,"fixed bell hanger"))
    parts.append(turned_bell_part("Monastery cast bronze bell",53.2,27.93,
        ((8.43,.105),(8.43,.17),(8.35,.248),(8.16,.295),(8.05,.345),(7.99,.425),
         (7.94,.425),(7.94,.365),(8.03,.315),(8.16,.25),(8.35,.19),(8.37,.105)),
        bronze,"hollow cast bell on supported frame"))
    rope=turned_bell_part("Monastery bell pull rope",53.2,26.83,
        ((7.07,.018),(7.12,.049),(7.34,.049),(7.43,.020),(8.60,.020),(8.60,.008),(7.07,.008)),
        hemp,"reachable bell pull on supported pulley beam")
    parts.append(rope)
    return parts,rope.name


def build():
    prior=json.loads((PRIOR/"checkpoint.json").read_bytes())
    roster=json.loads((PRIOR/"roster.json").read_bytes())
    row=next(r for r in roster["active_sources"] if r["id"]=="WLD-10")
    source={key:ROOT/row[key] for key in ("blend","glb","native","construction")}
    assert all(sha(path)==prior["loose_belongings"]["source_geometry"][rel(path)] for path in source.values())
    plan=json.loads(PLAN.read_bytes())
    bpy.ops.wm.open_mainfile(filepath=str(source["blend"]))
    bpy.context.preferences.filepaths.save_version=0
    ground=bpy.context.scene.objects[GROUND]
    old_ground=records(ground)
    soil=material("Monastery outer weathered meadow",(.144,.157,.097))
    road=material("Monastery outer worn carriage road",(.246,.231,.187))
    stone=material("Monastery estate wall limestone",(.271,.242,.196))
    wood=material("Monastery road gate weathered oak",(.136,.083,.046))
    apron,survey=make_apron(ground,soil,road)
    iron=material("Monastery carriage gate forged straps",(.052,.054,.050))
    walls,rows=make_boundary(ground,apron,plan,stone,wood,iron)
    assert old_ground==records(ground)
    bell,rope=fit_bell()
    additions=[apron,*walls,*bell]
    OUT.mkdir(parents=True,exist_ok=True)
    model,collision,blend=(OUT/(STEM+ext) for ext in (".glb",".hcs2",".blend"))
    fragment=ROOT/"build/monastery-perimeter-fragment.glb"
    intermediate=ROOT/"build/monastery-perimeter-unpruned.glb"
    export(fragment,additions)
    append(source["glb"],fragment,intermediate,reuse_embedded_images=True)
    compact(intermediate,model)
    budget=audit_level_budget(model)
    assert budget["bytes"]<16_000_000
    def optical_rope(obj):
        triangles=records(obj)
        return [(face,2) for face,_ in triangles] if obj.name==rope else triangles
    native=native_write(source["native"],collision,set(),additions,
                        open_components={apron.name,rope},mesh_records=optical_rope)
    previous,merged=split_native(source["native"]),split_native(collision)
    assert merged[:len(previous)]==previous and len(merged)==len(previous)+len(additions)
    bpy.ops.file.pack_all()
    bpy.ops.wm.save_as_mainfile(filepath=str(blend),compress=True)
    loaded=import_scene(model)
    for name,triangles in native:
        assert match_geometry([f for f,_ in triangles],[f for f,_ in records(loaded[name])]),name
    old=PRIOR/"wld-10"
    for p in old.iterdir():
        if p.is_file():
            assert sha(p)==prior["project_bytes_sha256"][rel(p)],p
            (OUT/p.name).write_bytes(p.read_bytes())
    runtime=OUT/"runtime-input.tsv"
    with runtime.open("a",encoding="utf-8",newline="\n") as manifest:
        manifest.write(f"DEVICE\t{rope}\tTOWER_BELL\t7\t53.2\t7.25\t26.765\t0\n")
    write_tsv(OUT/"boundary-survey.tsv",[("span","x0","z0","x1","z1","stations","clearance","footing_low","crest_min","crest_max"),
              *((r["name"],*r["start"],*r["end"],r["stations"],r["exposed_height"],r["foundation_low"],*r["crest"]) for r in rows)])
    receipt=dict(status=STATUS,prior=rel(PRIOR/"checkpoint.json"),plan=rel(PLAN),plan_sha256=sha(PLAN),
                 surveyed_terrain=survey,boundary=rows,model_budget=budget,prior_native_parts=len(previous),
                 matched_new_parts=[name for name,_ in native], bell_pull=rope, geometry={rel(p):sha(p) for p in (blend,model,collision)})
    construction=OUT/"construction.json"
    write(construction,receipt)
    row.update(construction=rel(construction),blend=rel(blend),glb=rel(model),native=rel(collision),
               glb_sha256=sha(model),native_sha256=sha(collision),runtime_input=rel(OUT/"runtime-input.tsv"),
               loose_object_survey=rel(OUT/"belongings.json"))
    next(r for r in roster["sites"] if r["id"]=="WLD-10")["decision"]=STATUS
    roster["baseline"]=rel(PRIOR/"roster.json")
    write(STAGE/"roster.json",roster)
    write_tsv(STAGE/"source-hashes.tsv",[(p.name,sha(p)) for p in (model,collision,*(OUT/n for n in SCHEDULES[:3]))])
    print("PASS",survey["exterior_seam_edges"],"exact terrain joins;",len(native),"new matched parts;",
          len(previous),"original collision records")


if __name__=="__main__":
    build()
