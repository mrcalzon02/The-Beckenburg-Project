"""Build herb-court roof drains and two below-grade infiltration galleries."""
import json
import math
from pathlib import Path
import sys

import bpy
from mathutils import Vector

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT/"tools"))
from append_static_glb import append
from build_circulation_construction import native_write
from build_college_electrical import point
from build_college_grounds import import_scene, terrain_tree
from build_deliberate_entrances import write
from build_mine_drainage import Runoff
from build_mine_yard import combined_plate
from build_monastery import GARDEN_BEDS, rel
from build_monastery_ground_drainage import BASE, SCHEDULES, height, refine
from build_prison_sally_gate import export
from build_roster_foundations import material
from compose_room_terrain import split_native
from export_roster_collision import face_keys
from prepare_cemetery_doors import write_tsv
from review_roster_macroterrain import records, sha
from verify_college_services import bvh

PRIOR = BASE/"monastery-ground-drainage-pass"
STAGE = BASE/"monastery-court-drainage-pass"
OUT = STAGE/"wld-10"
STEM = "monastery-with-court-drainage"
GROUND = "WLD-10 graded exterior ground"
STATUS = "MONASTERY_HERB_COURT_DRAINAGE_STAGED"
DOWN = Vector((0,0,-1))
# The galleries sit clear of the raised bed footings, central passage and crypt.
GALLERIES = ((25.85,27.95,26.88),(30.05,32.15,31.12))
# Actual eight ground outlets from the herb-court gutter schedule.
# Four conductors approach each gallery from open perimeter and cross aisles.
PANS = (
    ("north herb court",1,15.70,22.82,((15.70,24.25),(25.65,24.25),(27.44,28.64))),
    ("west herb court",1,14.82,24.55,((14.82,25.11),(25.17,25.12),(26.35,28.64))),
    ("south herb court",1,15.70,49.18,((15.70,48.30),(26.57,48.30),(27.44,43.36))),
    ("west herb court",2,14.82,47.45,((15.34,47.35),(25.20,47.34),(26.35,43.36))),
    ("north herb court",2,42.25,22.82,((42.25,24.25),(32.35,24.25),(30.56,28.64))),
    ("east herb court",1,43.18,24.55,((43.18,25.11),(32.83,25.12),(31.65,28.64))),
    ("south herb court",2,42.25,49.18,((42.25,48.30),(31.43,48.30),(30.56,43.36))),
    ("east herb court",2,43.18,47.45,((42.66,47.35),(32.80,47.34),(31.65,43.36))),
)


def ring(x,z,outer,inner):
    return ((x-outer,z-outer,x-inner,z+outer),(x+inner,z-outer,x+outer,z+outer),
            (x-inner,z-outer,x+inner,z-inner),(x-inner,z+inner,x+inner,z+outer))


def sculpt(previous):
    old = terrain_tree(previous)
    positions, indices, addresses = [], {}, []
    for vertex in previous.data.vertices:
        pos = previous.matrix_world@vertex.co
        if tuple(pos) not in indices:
            indices[tuple(pos)] = len(positions)
            positions.append(pos)
        addresses.append(indices[tuple(pos)])
    faces = [(tuple(addresses[i] for i in face.vertices),face.material_index)
             for face in previous.data.polygons]
    collars = [(x,z,.23) for _,_,x,z,_ in PANS]
    intakes = [(x,35.5,1.72) for _,_,x in GALLERIES]
    areas = [(x-r-.23,z-r-.23,x+r+.23,z+r+.23) for x,z,r in collars+intakes]
    faces = refine(positions,faces,areas)
    # Two shallow surface falls feed the flush inspection grates.
    for v in positions:
        x,z = v.x,-v.y
        for cx,cz,r in intakes:
            distance = math.hypot(x-cx,z-cz)
            if distance<r:
                v.z -= .048*(1-distance/r)
    # The original pan footing and its new square throat are built above
    # this excavation. Split again close to each small collar.
    faces = refine_collars(positions,faces,collars+[(x,35.5,.29) for _,_,x in GALLERIES])
    apertures = [(x,z,.083) for _,_,x,z,_ in PANS]+[(x,35.5,.192) for _,_,x in GALLERIES]
    def in_aperture(face):
        center = sum((positions[i] for i in face),Vector())/3
        return any(max(abs(center.x-x),abs(-center.y-z))<radius for x,z,radius in apertures)
    faces = [(face,finish) for face,finish in faces if not in_aperture(face)]
    mesh = bpy.data.meshes.new("WLD-10 court and exterior ground")
    mesh.from_pydata(positions,[],[face for face,_ in faces])
    assert not mesh.validate(clean_customdata=False)
    for mat in previous.data.materials:
        mesh.materials.append(mat)
    uv = mesh.uv_layers.new(name="World metre terrain UV")
    for face,(vertices,finish) in zip(mesh.polygons,faces):
        face.material_index = finish
        for corner,i in zip(face.loop_indices,vertices):
            uv.data[corner].uv = positions[i].x/8,positions[i].y/8
    result = bpy.data.objects.new(mesh.name,mesh)
    bpy.context.scene.collection.objects.link(result)
    result["role"] = "saved editor terrain / herb-court and exterior drainage"
    bpy.data.objects.remove(previous,do_unlink=True)
    return result,old


def refine_collars(points,faces,collars):
    # Scale the existing conforming splitter's local length criterion to
    # 52.5 mm for only the sub-metre inlet pockets.
    bounds = [(4*(x-r-.04),4*(z-r-.04),4*(x+r+.04),4*(z+r+.04)) for x,z,r in collars]
    for p in points:
        p *= 4
    refined = refine(points,faces,bounds)
    for p in points:
        p *= .25
    return refined


class Court:
    def __init__(self,ground):
        self.ground = ground
        self.built = []
        self.retired = set()
        self.stone = material("Monastery dressed drainage limestone",(.35,.33,.28))
        self.gravel = material("Monastery washed soakaway aggregate",(.28,.245,.183))
        self.drains = material("Monastery fired clay storm conductors",(.37,.23,.15))
        self.iron = material("Monastery black cast iron drainage grates",(.105,.105,.091))
        self.network = []

    def plate(self,name,bounds,bottom,top,finish):
        obj = combined_plate("Monastery "+name,bounds,top,top-bottom,finish)
        obj["role"] = "founded court drainage"
        self.built.append(obj)

    def grate(self,name,x,z,half,datum):
        self.plate(name+" grate frame",ring(x,z,half,half-.022),datum-.028,datum,self.iron)
        bars = tuple((x+dx-.009,z-half+.021,x+dx+.009,z+half-.021)
                     for dx in (-.75*half,-.5*half,-.25*half,0,.25*half,.5*half,.75*half))
        crossbars = ((x-half+.020,z-half+.020,x+half-.020,z-half+.033),
                     (x-half+.020,z+half-.033,x+half-.020,z+half-.020))
        self.plate(name+" drain bars",bars+crossbars,datum-.022,datum,self.iron)

    def pan(self,label,index,x,z,path):
        name = f"{label} outlet {index}"
        original = "Monastery "+name
        for suffix in (" footing"," gravel bed"):
            obj = bpy.context.scene.objects[original+suffix]
            positions = [obj.matrix_world@v.co for v in obj.data.vertices]
            assert min(p.x for p in positions)<=x<=max(p.x for p in positions)
            assert min(-p.y for p in positions)<=z<=max(-p.y for p in positions)
            self.retired.add(obj.name)
            bpy.data.objects.remove(obj,do_unlink=True)
        self.plate(name+" pierced footing",ring(x,z,.24,.107),-.145,.023,self.stone)
        self.plate(name+" gravel apron",ring(x,z,.174,.107),.023,.047,self.gravel)
        self.plate(name+" clay inlet liner",ring(x,z,.115,.087),-.18,-.145,self.drains)
        self.grate(name,x,z,.107,.045)
        stations = [(x,z),*path]
        length = sum(math.dist(a,b) for a,b in zip(stations,stations[1:]))
        run = 0.0
        points = [(x,-.30,z)]
        for a,b in zip(stations,stations[1:]):
            run += math.dist(a,b)
            points.append((b[0],-.30-.35*run/length,b[1]))
        assert .018<.35/length<.06
        # Flared entry under the real pan and fully descending interior after it.
        pipe = Runoff.hollow_pipe(self,name+" to filter gallery",[(x,-.162,z),*points],.059)
        pipe.name = "Monastery "+name+" gravity conductor"
        self.network.append(dict(name=name,pipe=pipe.name,route=points,grade=.35/length,filter_end=path[-1]))

    def gallery(self,left,right,x):
        label = "west" if x<29 else "east"
        name = label+" herb-court soakaway"
        self.plate(name+" gravel filter bed",((left,28.45,right,43.55),),-2.11,-1.92,self.gravel)
        self.plate(name+" west gravel filter side",((left,28.45,left+.12,43.55),),-1.92,-.32,self.gravel)
        self.plate(name+" east gravel filter side",((right-.12,28.45,right,43.55),),-1.92,-.32,self.gravel)
        # Feed mouths pass over the low end filters into the open gravel trench.
        self.plate(name+" north gravel break",((left+.12,28.45,right-.12,28.54),),-1.92,-.80,self.gravel)
        self.plate(name+" south gravel break",((left+.12,43.46,right-.12,43.55),),-1.92,-.80,self.gravel)
        # A flush grated catch chamber closes the walking surface. Its sleeve
        # enters the filter from the side and retains a stone bearing collar.
        self.plate(name+" intake collar",ring(x,35.5,.31,.235),-.195,-.042,self.stone)
        self.grate(name,x,35.5,.235,-.042)
        self.plate(name+" inspection shaft",ring(x,35.5,.24,.201),-.228,-.195,self.stone)
        # Leave a deliberate access throat in the buried cover.
        throat = ((left,28.45,right,35.299),(left,35.701,right,43.55),
                  (left,35.299,x-.201,35.701),(x+.201,35.299,right,35.701))
        self.plate(name+" serviceable trench cover",throat,-.34,-.23,self.stone)


def survey(court, reference):
    terrain = terrain_tree(court.ground)
    guides = [(29,z) for z in range(23,50,2)]
    for x,z in guides:
        assert abs(height(terrain,x,z)-height(reference,x,z))<.0002
    cast = bvh(court.built)
    for _,_,x in GALLERIES:
        # Sight through cast-iron gaps reaches the actual filter bed.
        at,_,_,_=cast.ray_cast(point(x+.025,.1,35.5),DOWN,2.5)
        assert at is not None and abs(at.z+1.92)<.003,(x,at)
        hit,_,_,_=terrain.ray_cast(point(x+.025,.1,35.5),DOWN,2.5)
        assert hit is None,(x,hit)
    for _,_,x,z,_ in PANS:
        hit,_,_,_=terrain.ray_cast(point(x+.012,.01,z),DOWN,.46)
        assert hit is None,(x,z,hit)
    for entry in court.network:
        assert entry["route"][0][1]>entry["route"][-1][1]
        for x,y,z in entry["route"][1:-1]:
            assert height(reference,x,z)-y>.13 and 22<z<50
        for a,b in zip(entry["route"],entry["route"][1:]):
            start,end = point(*a),point(*b)
            delta = end-start
            hit,_,_,_=cast.ray_cast(start+delta*.06,delta.normalized(),delta.length*.88)
            assert hit is None,(entry["name"],a,b,hit)
    assert all(22<z<50 for _,_,_,_,path in PANS for _,z in path)
    return len(guides)


def build():
    prior = json.loads((PRIOR/"checkpoint.json").read_bytes())
    roster = json.loads((PRIOR/"roster.json").read_bytes())
    row = next(r for r in roster["active_sources"] if r["id"]=="WLD-10")
    assets = {k:ROOT/row[k] for k in ("construction","blend","glb","native")}
    for path in assets.values():
        assert sha(path)==prior["project_bytes_sha256"][rel(path)],path
    meta = json.loads(assets["construction"].read_bytes())
    bpy.ops.wm.open_mainfile(filepath=str(assets["blend"]))
    bpy.context.preferences.filepaths.save_version = 0
    ground,before = sculpt(bpy.context.scene.objects[GROUND])
    court = Court(ground)
    for pan in PANS:
        court.pan(*pan)
    for gallery in GALLERIES:
        court.gallery(*gallery)
    probes = survey(court,before)

    OUT.mkdir(parents=True,exist_ok=True)
    model,collision,blend = (OUT/(STEM+ext) for ext in (".glb",".hcs2",".blend"))
    all_objects = [ground,*court.built]
    fragment = ROOT/"build/monastery-court-drainage.glb"
    export(fragment,all_objects)
    retired = {GROUND,*court.retired}
    append(assets["glb"],fragment,model,remove_mesh_names=sorted(retired),reuse_embedded_images=True)
    added = native_write(assets["native"],collision,retired,all_objects,open_components={ground.name})
    unchanged = [p for p in split_native(assets["native"]) if p[0] not in retired]
    output = split_native(collision)
    assert output[:len(unchanged)]==unchanged and len(output)==len(unchanged)+len(added)
    bpy.ops.file.pack_all()
    bpy.ops.wm.save_as_mainfile(filepath=str(blend),compress=True)
    loaded = import_scene(model)
    for name,triangles in added:
        assert face_keys(records(loaded[name]))==face_keys(triangles),name

    for name in (*SCHEDULES,"drainage-survey.tsv"):
        input_path=PRIOR/"wld-10"/name
        assert sha(input_path)==prior["project_bytes_sha256"][rel(input_path)]
        data=input_path.read_bytes()
        if name=="runtime-input.tsv":
            data+=b"".join(f"GRATE\tMonastery {side} herb-court soakaway drain bars region0\n".encode()
                           for side in ("west","east"))
        (OUT/name).write_bytes(data)
    write_tsv(OUT/"court-drainage.tsv",[("inlet","x","y","z"),
        *((entry["name"],*at) for entry in court.network for at in entry["route"])])
    meta.update(status=STATUS,before_construction=rel(assets["construction"]),
        terrain_mesh=added[0][0],source_glb=rel(assets["glb"]),source_glb_sha256=sha(assets["glb"]),
        removed_source_meshes=sorted(retired),retained_native_records=len(unchanged),
        matched_court_drainage_components=[name for name,_ in added],court_drainage=court.network,
        court_gallery_intervals=GALLERIES,court_main_walk_grade_probes=probes,
        builder=rel(Path(__file__)),builder_sha256=sha(Path(__file__)),
        glb_sha256=sha(model),physics_sha256=sha(collision),blend_sha256=sha(blend))
    construction=OUT/"construction.json"
    write(construction,meta)
    row.update(construction=rel(construction),blend=rel(blend),glb=rel(model),native=rel(collision),
               glb_sha256=sha(model),native_sha256=sha(collision))
    next(s for s in roster["sites"] if s["id"]=="WLD-10")["decision"] = STATUS
    roster["baseline"]=rel(PRIOR/"roster.json")
    write(STAGE/"roster.json",roster)
    write_tsv(STAGE/"source-hashes.tsv",[(p.name,sha(p)) for p in (model,collision,*(OUT/n for n in SCHEDULES[:3]))])
    print("PASS eight roof connections and two supported filter galleries,",len(added),"matching components, and",probes,"preserved court walking datums")


if __name__=="__main__":
    build()
