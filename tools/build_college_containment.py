"""Fit the College parcel boundary and two unguarded wells to the saved native scene."""
from collections import Counter, defaultdict
from dataclasses import dataclass
from math import ceil, exp, hypot, sin
from pathlib import Path
import json
import sys

import bpy
from mathutils import Vector
from mathutils.geometry import delaunay_2d_cdt
from mathutils.bvhtree import BVHTree

ROOT = Path(__file__).resolve().parents[1]
sys.path[:0] = [str(ROOT/"tools"), str(ROOT/"development/levels/blockouts")]
from append_static_glb import append
from build_circulation_construction import native_write
from build_college_doors import aabb
from build_roster_foundations import material
from review_roster_macroterrain import sha
from verify_college_electrical import native_records

BASE = ROOT/"development/levels/rebuild/college-doors-pass"
EDGE_PLAN = ROOT/"development/levels/edge-sky-plans/college/plan.json"
GUARD_MODEL = ROOT/"development/levels/primitive-props-v1/models/level_blockout_guardrail.glb"
WALL_HEADS = ("Wall plant W", "Wall maintenance W")
GATE = ((36,103),(28,103))

# Clockwise parcel line from the teaching wing's wooded approach. Rounded
# returns follow the usable hillside, buildings and the narrower forecourt.
RING = (
    (4.3,12), (4.3,8.7), (7.3,8.0), (17,5.4), (25,2.4), (45,2.4), (54,3.2),
    (56.6,6.5), (57.5,12), (59.2,18), (65.1,22.6), (68.6,27.5),
    (69.9,37), (70,62), (70,71.4), (64.8,77.2), (57.4,83),
    (48.5,88.4), (41.7,93.2), (41.7,100.8), *GATE, (22.5,101.4),
    (22.1,94), (21.1,90), (18.7,87), (15.4,84), (12,81),
    (9.3,77), (7.5,73), (5.7,65), (4.7,62), (3.4,55), (3.6,44), (3.7,29),
)


@dataclass(frozen=True)
class Well:
    link: str
    title: str
    deck_y: float
    low_y: float
    x0: float
    x1: float
    z0: float
    z1: float
    width: float
    steps: int
    low_at_north: bool = True


WELLS = (
    Well("L80", "West teaching tower", 3.6, 0, 27.55, 29.45, 36.35, 42.36, 1.5, 0),
    Well("L81", "Library tower", 3.6, 0, 30.05, 31.95, 5.75, 11.76, 1.5, 0),
    Well("L82", "North residence tower", 3.6, 0, 64.1, 65.9, 32, 37.76, 1.5, 20),
    Well("L84", "West service cellar", 0, -3, 24.3, 26.1, 37.35, 42.25, 1.4, 0, False),
    Well("L85", "East service cellar", 0, -3, 45.15, 46.85, 68, 72.8, 1.4, 17),
)


class MeshParts:
    """Closed components with world-metric UV coordinates and indexed materials."""

    def __init__(self, materials):
        self.materials = materials
        self.vertices = []
        self.faces = []
        self.face_materials = []

    def prism(self, polygon, bottom, top, finish):
        count = len(polygon)
        if isinstance(bottom, (float,int)):
            bottom = [bottom]*count
        if isinstance(top, (float,int)):
            top = [top]*count
        assert min(t-b for b,t in zip(bottom,top))>1e-5
        i = len(self.vertices)
        points = [(x,-z) for x,z in polygon]
        area = sum(a[0]*b[1]-a[1]*b[0] for a,b in zip(points,points[1:]+points[:1]))
        if area<0:
            points.reverse()
            bottom = bottom[::-1]
            top = top[::-1]
        for ys in (bottom,top):
            self.vertices.extend((x,z,y) for (x,z),y in zip(points,ys))
        self.faces.append(tuple(i+j for j in range(count-1,-1,-1)))
        self.faces.append(tuple(i+count+j for j in range(count)))
        for j in range(count):
            p = (j+1)%count
            self.faces.append((i+j,i+p,i+count+p,i+count+j))
        self.face_materials.extend([finish]*(count+2))

    def frame(self, name, role):
        mesh = bpy.data.meshes.new(name)
        mesh.from_pydata(self.vertices,[],self.faces)
        for mat in self.materials:
            mesh.materials.append(mat)
        mesh.update()
        uv = mesh.uv_layers.new(name="Metres")
        for face, mat in zip(mesh.polygons,self.face_materials):
            face.material_index=mat
            axis = max(range(3),key=lambda i: abs(face.normal[i]))
            a,b = [i for i in range(3) if i!=axis]
            for index in face.loop_indices:
                p = mesh.vertices[mesh.loops[index].vertex_index].co
                uv.data[index].uv = (p[a],p[b])
        obj = bpy.data.objects.new(name,mesh)
        bpy.context.scene.collection.objects.link(obj)
        obj["role"] = role
        return obj


def station(a,b,t):
    return (a[0]+(b[0]-a[0])*t, a[1]+(b[1]-a[1])*t)


def strip(a,b,width):
    dx,dz = b[0]-a[0],b[1]-a[1]
    length = hypot(dx,dz)
    nx,nz = -dz/length*width/2, dx/length*width/2
    return [(a[0]+nx,a[1]+nz),(b[0]+nx,b[1]+nz),
            (b[0]-nx,b[1]-nz),(a[0]-nx,a[1]-nz)]


def post_foot(p,width):
    a,b=p
    w=width/2
    return [(a-w,b-w),(a+w,b-w),(a+w,b+w),(a-w,b+w)]


def terrain_reader(terrain):
    tree = BVHTree.FromPolygons([terrain.matrix_world@v.co for v in terrain.data.vertices],
                                [tuple(p.vertices) for p in terrain.data.polygons])

    def height(x,z):
        hit,_,_,_ = tree.ray_cast(Vector((x,-z,2)),Vector((0,0,-1)),5)
        return None if hit is None else hit.z

    return height


def sampled_ground(grade,a,b,width):
    span = hypot(a[0]-b[0],a[1]-b[1])
    intervals = max(1,ceil(span/.12))
    samples = []
    corners = strip(a,b,width)
    for j in range(intervals+1):
        t=j/intervals
        p=station(corners[0],corners[1],t)
        q=station(corners[3],corners[2],t)
        for offset in (0,.25,.5,.75,1):
            x,z=station(p,q,offset)
            h=grade(x,z)
            assert h is not None, ("No saved terrain bearing",x,z,a,b)
            samples.append(h)
    return samples


def loop_area(points):
    return .5 * sum(a[0]*b[1]-a[1]*b[0] for a,b in zip(points,points[1:]+points[:1]))


def inside(points,x,z):
    hit=False
    for a,b in zip(points,points[1:]+points[:1]):
        if (a[1]>z)!=(b[1]>z) and x < a[0]+(z-a[1])*(b[0]-a[0])/(b[1]-a[1]):
            hit=not hit
    return hit


def distance_to_edge_squared(p, a, b):
    dx,dz=b[0]-a[0],b[1]-a[1]
    t=max(0,min(1,((p[0]-a[0])*dx+(p[1]-a[1])*dz)/(dx*dx+dz*dz)))
    x,z=station(a,b,t)
    return (p[0]-x)**2+(p[1]-z)**2


def surveyed_terrain_loops(terrain):
    coordinates={}
    ids=[]
    locations=[]
    for v in terrain.data.vertices:
        p=terrain.matrix_world@v.co
        key=tuple(round(k,6) for k in p)
        if key not in coordinates:
            coordinates[key]=len(locations)
            locations.append((p.x,-p.y,p.z))
        ids.append(coordinates[key])
    edges=Counter(tuple(sorted((ids[a],ids[b]))) for p in terrain.data.polygons for a,b in p.edge_keys)
    adjacent=defaultdict(list)
    for (a,b),count in edges.items():
        if count==1:
            adjacent[a].append(b)
            adjacent[b].append(a)
    assert all(len(n)==2 for n in adjacent.values()), "Open or branching saved ground edge"
    unused=set(adjacent)
    loops=[]
    while unused:
        start=min(unused)
        trail=[start]
        node=min(adjacent[start])
        while node!=start:
            assert node in unused
            candidates=[v for v in adjacent[node] if v!=trail[-1]]
            trail.append(node)
            node,=candidates
        unused.difference_update(trail)
        loops.append([locations[i] for i in trail])
    return sorted(loops,key=lambda points: abs(loop_area(points)),reverse=True)


def district_apron(terrain):
    # Preserve all three saved loops. Only the external one receives new
    # adjacent faces. The two buried building/cavity loops remain untouched.
    rim,*cuts=surveyed_terrain_loops(terrain)
    assert len(rim)==1083 and sorted(map(len,cuts))==[42,178]
    outer=[
        (-44,-30), (-13,-42), (32,-44), (85,-41), (114,-18), (122,28),
        (117,70), (111,102), (89,139), (64,158), (36,168.5), (32,170), (28,167.92), (7,157),
        (-15,137), (-35,102), (-42,60), (-44,13),
    ]
    soil=terrain.data.materials[0]
    gravel=terrain.data.materials["College forecourt compacted gravel"]
    # Constrain the exact saved terrain edge and a narrow service-road throat
    # beyond it. Wider ground is shaped by the surrounding site's functions.
    def grade(x,z):
        n=min(rim,key=lambda p:(x-p[0])**2+(z-p[1])**2)
        weight=min(1,hypot(x-n[0],z-n[1])/12)
        west=3.8*exp(-((x+22)/24)**2-((z-38)/65)**2)
        east=2.2*exp(-((x-93)/27)**2-((z-43)/42)**2)
        north=.75*sin(x*.06)*exp(-((z+24)/21)**2)
        far=-.38+west+east+north
        if z>=107 and 28<=x<=36:
            far=-.21-.010*(z-107)
        return n[2]*(1-weight)+far*weight

    points=list(rim)+[(x,z,grade(x,z)) for x,z in outer]
    rim_count=len(rim)
    outer_offset=rim_count
    edges=[(i,(i+1)%rim_count) for i in range(rim_count)]
    edges += [(outer_offset+i,outer_offset+(i+1)%len(outer)) for i in range(len(outer))]
    road_ends=(28,167.92),(36,168.5)
    road_edges=[]
    for x,z in road_ends:
        start,= [i for i,p in enumerate(rim) if p[:2]==(x,107)]
        road_edges.append((start,outer_offset+outer.index((x,z))))
    for z in range(-38,163,6):
        for x in range(-39,119,5):
            if not inside(outer,x,z) or inside(rim,x,z):
                continue
            if min((x-p[0])**2+(z-p[1])**2 for p in rim)<=.24:
                continue
            if min(distance_to_edge_squared((x,z),a,b) for a,b in zip(outer,outer[1:]+outer[:1]))<.10:
                continue
            if any(distance_to_edge_squared((x,z),points[a],points[b])<.10 for a,b in road_edges):
                continue
            points.append((x,z,grade(x,z)))
    _,_,triangles,original_ids,_,_=delaunay_2d_cdt([Vector(p[:2]) for p in points],edges+road_edges,[],0,1e-7)
    assert all(len(ids)==1 for ids in original_ids), "Unexpected intersecting ground constraints"
    locations=[points[ids[0]] for ids in original_ids]
    faces=[]
    for triangle in triangles:
        face=[locations[v] for v in triangle]
        x,z=[sum(p[axis] for p in face)/3 for axis in (0,1)]
        if not inside(outer,x,z) or inside(rim,x,z):
            continue
        if loop_area(face)>0:triangle.reverse()
        faces.append(triangle)

    # Every exterior edge must match a single retained saved edge. A raw
    # area check also excludes overlapping apron triangles or stray holes.
    used=Counter(tuple(sorted((a,b))) for f in faces for a,b in zip(f,f[1:]+f[:1]))
    new_boundary={tuple(sorted(original_ids[i][0] for i in e)) for e,n in used.items() if n==1}

    constraints={tuple(sorted(e)) for e in edges}
    missing=constraints-new_boundary
    extra=new_boundary-constraints
    assert not missing and not extra, (len(missing),len(extra),
        [[points[i] for i in e] for e in sorted(missing)[:5]],
        [[points[i] for i in e] for e in sorted(extra)[:5]])
    area=sum(abs(loop_area([locations[v] for v in face])) for face in faces)
    assert abs(area-(abs(loop_area(outer))-abs(loop_area(rim))))<.001

    mesh=bpy.data.meshes.new("College district apron with surveyed seam")
    mesh.from_pydata([(x,-z,y) for x,z,y in locations],[],faces)
    for mat in (soil,gravel):mesh.materials.append(mat)
    mesh.update()
    uv=mesh.uv_layers.new(name="World metre UV")
    for face in mesh.polygons:
        x,z=face.center.x,-face.center.y
        if z>107 and 28<x<36:
            face.material_index=1
        for slot in face.loop_indices:
            v=mesh.vertices[mesh.loops[slot].vertex_index].co
            uv.data[slot].uv=(v.x/8,v.y/8)
    obj=bpy.data.objects.new("College district apron",mesh)
    bpy.context.scene.collection.objects.link(obj)
    obj["role"]="Continuous outside terrain beyond the enclosed parcel"
    return obj,dict(seam_edges=len(rim),retained_cut_loop_edges=sorted(map(len,cuts)),
                    soil_triangles=len(faces),square_metres=round(area,2),outer_limits=outer,
                    road_width=8,road_outer_ends=road_ends)


def finish_forecourt_throat(terrain):
    slot=terrain.data.materials.find("College forecourt compacted gravel")
    assert slot>=0
    changed=0
    for face in terrain.data.polygons:
        x,z,_=terrain.matrix_world@face.center
        if 28<=x<=36 and -z>=101.75 and face.material_index!=slot:
            face.material_index=slot
            changed+=1
    assert changed>0
    return changed


def slab_bearings(scene):
    changed = []
    for name in WALL_HEADS:
        obj = scene.objects[name]
        bounds = aabb(obj)
        assert bounds[1]==-3 and bounds[4]==0 and bounds[0]==11.92 and bounds[3]==12.08
        inverse = obj.matrix_world.inverted()
        for v in obj.data.vertices:
            p = obj.matrix_world@v.co
            if abs(p.z)<.001:
                p.z=-.16
                v.co=inverse@p
        obj.data.update()
        obj.name="College slab bearing "+name
        obj["role"]="Basement masonry head seated against the underside of the ground slab"
        assert aabb(obj)==(*bounds[:4],-.16,bounds[5])
        changed.append(obj)
    return changed


def imported_guard():
    previous = set(bpy.context.scene.objects)
    bpy.ops.import_scene.gltf(filepath=str(GUARD_MODEL))
    imported = set(bpy.context.scene.objects)-previous
    source = next(obj for obj in imported if obj.type=="MESH")
    mesh = source.data.copy()
    for vertex in mesh.vertices:
        vertex.co = source.matrix_world@vertex.co
    for obj in imported:
        bpy.data.objects.remove(obj,do_unlink=True)
    ext = [(min(v.co[i] for v in mesh.vertices), max(v.co[i] for v in mesh.vertices)) for i in range(3)]
    assert abs(ext[0][0]+1)<.001 and abs(ext[0][1]-1)<.001 and abs(ext[2][0])<.001,ext
    return mesh


def well_guards(scene,rail,steel):
    built, tests = [], []

    def rail_span(title,part,a,b,y0,y1,scale=1):
        length = hypot(b[0]-a[0],b[1]-a[1])
        dx,dz = (b[0]-a[0])/length,(b[1]-a[1])/length
        panels=ceil(length/1.75)
        for j in range(panels):
            mesh = rail.copy()
            for v in mesh.vertices:
                t = (j+(v.co.x+1)/2)/panels
                p=station(a,b,t)
                offset=v.co.y
                v.co=(p[0]-dz*offset, -(p[1]+dx*offset), y0+(y1-y0)*t+v.co.z*scale)
            name=f"College {title} {part} rail {j+1}"
            obj=bpy.data.objects.new(name,mesh)
            bpy.context.scene.collection.objects.link(obj)
            obj["role"]="Indexed steel fall guard connected to continuous seat"
            obj["source_asset"]="level_blockout_guardrail"
            built.append(obj)

    def seat(title,part,a,b,y0,y1):
        pieces=MeshParts([steel])
        poly=strip(a,b,.09)
        tops=[y0,y1,y1,y0]
        pieces.prism(poly,[value-.095 for value in tops],tops,0)
        built.append(pieces.frame(f"College {title} {part} shoe","Bolted continuous steel rail shoe"))

    for well in WELLS:
        center=(well.x0+well.x1)/2
        dx=well.width/2
        run=well.z1-well.z0
        rise=well.deck_y-well.low_y
        for i in range(well.steps):
            obj=scene.objects[f"{well.link} flight0 step{i+1}"]
            x0,_,z0,x1,top,z1=aabb(obj)
            expected=(center-dx,well.z0+run*i/well.steps,center+dx,
                      well.low_y+rise*(i+1)/well.steps,well.z0+run*(i+1)/well.steps)
            assert all(abs(a-b)<.001 for a,b in zip((x0,z0,x1,top,z1),expected)), (obj.name,expected)
        if well.steps:
            for edge,x in (("west",center-dx+.045),("east",center+dx-.045)):
                a,b=(x,well.z0),(x,well.z1)
                y0,y1=well.low_y+.015,well.deck_y+.015
                seat(well.link,f"{edge} flight",a,b,y0,y1)
                rail_span(well.link,f"{edge} flight",a,b,y0,y1)
        left,right=well.x0-.06,well.x1+.06
        north=well.z0-(.06 if well.low_at_north else 0)
        south=well.z1+(.06 if not well.low_at_north else 0)
        closed,cross=(north,1) if well.low_at_north else (south,-1)
        for name,a,b,normal in (
            ("low-end return",(left,closed),(right,closed),(0,cross)),
            ("west rim",(left,north),(left,south),(1,0)),
            ("east rim",(right,north),(right,south),(-1,0)),
        ):
            rail_y=well.deck_y+.025
            seat(well.link,name,a,b,rail_y,rail_y)
            rail_span(well.link,name,a,b,rail_y,rail_y,scale=1.48)
            tests.append(dict(well=well.link,name=name,a=a,b=b,y=well.deck_y,
                              height=1.579,drop_direction=normal))
    return built, tests


def parcel(grade):
    stone=material("College warm-grey coursed stone",(.34,.32,.29))
    coping=material("College dressed limestone coping",(.47,.44,.38))
    iron=material("College forecourt blackened iron",(.065,.076,.068))
    postface=material("College entrance ashlar",(.42,.39,.34))
    built,proof=[],[]

    def coping_post(point,base,height,name,gateway=False):
        pieces=MeshParts([stone,coping,iron,postface])
        foot,shaft = (.82,.62) if gateway else (.47,.35)
        grade_points = [grade(*p) for p in post_foot(point,foot)]
        assert all(h is not None for h in grade_points), (point,foot)
        bottom=min(*grade_points,base)-.25
        pieces.prism(post_foot(point,foot),bottom,base+.16,0)
        pieces.prism(post_foot(point,shaft),base+.16,base+height-.12,3)
        pieces.prism(post_foot(point,shaft+.12),base+height-.12,base+height,1)
        built.append(pieces.frame(name,"Founded campus boundary pier"))

    for edge,(a,b) in enumerate(zip(RING,(*RING[1:],RING[0])),1):
        length=hypot(b[0]-a[0],b[1]-a[1])
        gate=(a,b)==GATE
        base_height=.64 if a[0]<25 and a[1]<84 else .43
        panels=2 if gate else ceil(length/2.65)
        ref=sampled_ground(grade,a,b,.44)
        record=dict(edge=edge,a=a,b=b,grade_min=min(ref),grade_max=max(ref),samples=len(ref),gate=gate,panels=[])
        for i in range(panels):
            p,q=station(a,b,i/panels),station(a,b,(i+1)/panels)
            d=hypot(q[0]-p[0],q[1]-p[1])
            grades=sampled_ground(grade,p,q,.39 if not gate else .25)
            floor0,floor1=grade(*p),grade(*q)
            assert floor0 is not None and floor1 is not None
            bottom=min(grades)-.22
            grade_peak=max(grades)
            crest=max(floor0,floor1,grade_peak)+2.18
            pieces=MeshParts([stone,coping,iron,postface])
            if not gate:
                stone_top=grade_peak+base_height
                pieces.prism(strip(p,q,.38),bottom,stone_top,0)
                pieces.prism(strip(p,q,.43),stone_top,stone_top+.08,1)
                bar_bottom=stone_top+.06
            else:
                bar_bottom=grade_peak+.075
            for y in (bar_bottom+.08,crest-.22):
                pieces.prism(strip(p,q,.063),y-.025,y+.025,2)
            counts=ceil(d/.168)
            for j in range(counts+1):
                at=station(p,q,j/counts)
                pieces.prism(post_foot(at,.032),bar_bottom,crest,2)
            if gate:
                u,v=(p,q) if i==0 else (q,p)
                y0,y1=bar_bottom+.13,crest-.23
                pieces.prism(strip(u,v,.073),[y0,y1,y1,y0],[y0+.08,y1+.08,y1+.08,y0+.08],2)
                if i==0:
                    # The captive central stile and latch connect the closed leaves.
                    mid=station(q,p,.03)
                    pieces.prism(strip(mid,station(q,b,.03),.095),bar_bottom+.92,bar_bottom+1.04,2)
            role="Physical closed double-leaf service gate" if gate else "Founded masonry boundary with open ironwork"
            built.append(pieces.frame(f"College parcel {edge:02d} bay {i+1:02d}",role))
            if not gate or i==0:
                gateway=gate or p==GATE[-1]
                coping_post(p,floor0,2.43 if gateway else 1.17,
                            f"College parcel {edge:02d} pier {i+1:02d}",gateway)
            record["panels"].append(dict(a=p,b=q,base=bottom,crest=crest,grade_min=min(grades),grade_max=grade_peak))
        proof.append(record)
    return built,proof


def main(out):
    source=BASE/"college"
    prior=json.loads((source/"construction.json").read_bytes())
    glb=source/"college-doors.glb"
    native=source/"college-doors.hcs2"
    blend=source/"college-doors.blend"
    assert sha(glb)==prior["glb_sha256"] and sha(native)==prior["physics_sha256"] and sha(blend)==prior["blend_sha256"]
    bpy.ops.wm.open_mainfile(filepath=str(blend))
    bpy.context.preferences.filepaths.save_version=0
    scene=bpy.context.scene
    terrain=scene.objects["College saved campus earthworks"]
    grade=terrain_reader(terrain)
    apron, apron_receipt=district_apron(terrain)
    painted=finish_forecourt_throat(terrain)
    original_terrain_node=terrain.name
    terrain.name="College campus earthworks with service throat"
    rails, wells=well_guards(scene,imported_guard(),material("College stair edge blackened steel",(.085,.089,.088)))
    perimeter, boundary=parcel(grade)
    old_rims=[obj for obj in scene.objects if any(
        obj.name.startswith(f"COLLEGE bay {well.link} {label}")
        for well in WELLS if not well.steps for label in ("floor rim","closed floor well end"))]
    assert len(old_rims)==27
    obsolete=set(WALL_HEADS)|{obj.name for obj in old_rims}
    for obj in old_rims:
        bpy.data.objects.remove(obj,do_unlink=True)
    feet=slab_bearings(scene)
    pieces=[*feet,*rails,*perimeter,apron]
    out.mkdir(parents=True,exist_ok=True)
    bpy.ops.object.select_all(action="DESELECT")
    for obj in (*pieces,terrain):
        obj.select_set(True)
    bpy.context.view_layer.update()
    additions=out/"additions.glb"
    exports=[out/("college-containment"+ext) for ext in (".glb",".hcs2",".blend")]
    bpy.ops.export_scene.gltf(filepath=str(additions),export_format="GLB",use_selection=True,export_extras=True)
    append(glb,additions,exports[0],remove_mesh_names=obsolete|{original_terrain_node})
    parts=native_write(native,exports[1],obsolete,pieces,open_components=(apron.name,))
    before=native_records(native)
    retained=[row for row in before if row[0].split(":",1)[-1] not in obsolete]
    after=native_records(exports[1])
    assert len(before)-len(retained)==len(obsolete) and after[:len(retained)]==retained
    bpy.ops.file.pack_all()
    bpy.ops.wm.save_as_mainfile(filepath=str(exports[2]),compress=True)
    receipt=dict(
        source_glb=sha(glb),source_physics=sha(native),source_blend=sha(blend),
        output_sha256={file.name:sha(file) for file in exports},
        edge_plan_sha256=sha(EDGE_PLAN),guard_asset_sha256=sha(GUARD_MODEL),
        retained_native_parts=len(retained),removed_component_names=sorted(obsolete),
        replacement_wall_heads=list(WALL_HEADS),slab_soffit_y=-.16,
        perimeter_spans=boundary,well_guard_spans=wells,district_apron=apron_receipt,
        forecourt_terrain_render_node=original_terrain_node,current_terrain_render_node=terrain.name,
        forecourt_refinished_triangles=painted,
        new_parts=len(parts),new_triangles=sum(len(ts) for _,ts in parts))
    (out/"containment.json").write_text(json.dumps(receipt,indent=2)+"\n",encoding="utf-8",newline="\n")
    manifest=(BASE/"verification/runtime.tsv").read_bytes().splitlines(keepends=True)
    manifest[0]=f"SOURCE\t{sha(exports[0])}\t{sha(exports[1])}\n".encode()
    (out/"runtime.tsv").write_bytes(b"".join(manifest))
    rows=[]
    for span in boundary:
        for i,panel in enumerate(span["panels"],1):
            fields=(span["edge"],i,*panel["a"],*panel["b"],panel["grade_min"],panel["grade_max"],
                    panel["base"],panel["crest"],int(span["gate"]))
            rows.append("\t".join(map(str,("PARCEL",*fields))))
    for guard in wells:
        fields=(guard["well"],guard["name"],*guard["a"],*guard["b"],guard["y"],*guard["drop_direction"])
        rows.append("\t".join(map(str,("WELL",*fields))))
    (out/"containment.tsv").write_text("\n".join(rows)+"\n",encoding="utf-8",newline="\n")
    print(f"College parcel: {len(boundary)} spans, {len(wells)} well rim runs, {len(parts)} native additions")


if __name__=="__main__":
    main(Path(sys.argv[sys.argv.index("--")+1]).resolve())
