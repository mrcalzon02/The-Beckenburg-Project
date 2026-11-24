"""Connect the Mine's service yard to measured fixtures and circuit controls."""
import json
import math
from pathlib import Path
import sys

import bpy
from mathutils import Vector
from mathutils.bvhtree import BVHTree

ROOT = Path(__file__).resolve().parents[1]
sys.path[:0] = [str(ROOT/"tools"), str(ROOT/"development/levels/blockouts")]
from append_static_glb import append
from build_circulation_construction import native_write
from build_college_electrical import Assembly, point, world
from build_lookout_native_arrival import table
from build_prison_sally_gate import export
from review_roster_macroterrain import records, sha
from verify_college_containment import triangles
from verify_college_electrical import native_records
from verify_college_services import match_geometry

BASE = ROOT/"development/levels/rebuild/mine-service-yard-pass"
TOP = -3.36
LINTEL = -3.605
BURIED_Y = -1.20
UP = Vector((0,0,1))
ROOMS = (
    ("shack", (12,0,53), (18.65,55.25,1,0)),
    ("assay", (47,0,52), (41.25,55.20,-1,0)),
    ("stores", (8,0,39), (12.60,40.20,1,0)),
    ("adit", (29,0,52), (30.10,52.85,1,0)),
    ("haul", (29.85,-6,30), (22,40.30,0,1)),
    ("ore-west", (12,-6,32), (3.4,28.2,-1,0)),
    ("ore-east", (45,-6,31), (52.7,30.2,1,0)),
    ("cross", (28,-6,15), (28,9.0,0,-1)),
    ("lift", (48,-6,16), (52.6,14.2,1,0)),
    ("lower", (11,-10,31), (11,40.8,0,1)),
    ("vent", (11,-6,52), (3.26,50.8,-1,0)),
    ("wash", (47,0,39), (41.22,41.0,-1,0)),
)
TRENCHES = {
    "yard service exit": ((33.76,67.57),(33.65,67.57),(33.65,64.90)),
    "crew house branch": ((33.65,64.9),(12.0,64.9),(12.0,62.28)),
    "stores west branch": ((12.0,64.9),(.85,64.90),(.85,44.65),(1.72,44.65)),
    "east yard branch to assay": ((33.65,64.9),(33.65,62.65),(55.2,62.65),(55.2,52.0),(54.30,52.0)),
    "east yard branch to wash": ((55.2,52.0),(55.2,39.0),(54.30,39.0)),
    "headhouse branch": ((33.65,64.9),(32.10,64.9),(32.10,52.85),(31.58,52.85)),
}
# Outside approach, wall normal into the room, and the measured inside finish.
RISERS = {
    0: ((12,62.28),(0,-1),(12,61.86)),
    1: ((54.30,52),(-1,0),(53.875,52)),
    2: ((1.72,44.65),(1,0),(2.125,44.65)),
    3: ((31.58,52.85),(-1,0),(31.075,52.85)),
    11: ((54.30,39),(-1,0),(53.875,39)),
}


def samples(a,b,spacing):
    n = max(1,math.ceil(math.dist(a,b)/spacing))
    return [tuple(v+(w-v)*i/n for v,w in zip(a,b)) for i in range(n+1)]


class MineDistribution(Assembly):
    def __init__(self):
        super().__init__()
        self.runs = []
        self.sealed = []
        self.bearings = []
        self.buried_checks = []
        self.buried_names = set(TRENCHES)
        self.exposed_segments = 0
        vertices, faces, self.receivers = [], [], []
        for obj in self.scene.objects:
            if obj.type!="MESH" or not obj.visible_get():
                continue
            start = len(vertices)
            vertices.extend(obj.matrix_world@v.co for v in obj.data.vertices)
            faces.extend(tuple(start+j for j in p.vertices) for p in obj.data.polygons)
            self.receivers.extend([obj]*len(obj.data.polygons))
        self.structure = BVHTree.FromPolygons(vertices,faces)

    def contact(self,origin,direction,distance):
        hit,normal,index,_=self.structure.ray_cast(origin,direction,distance)
        if index is not None:
            return hit,normal,self.receivers[index]
        return None

    def run(self,name,points,backing=UP,radius=.022):
        points = [tuple(p) for n,p in enumerate(points) if n==0 or p!=points[n-1]]
        if len(points)>1:
            self.runs.append((name,points,backing,radius))

    def sleeve(self,name,a,b):
        self.sealed.append((name,a,b))
        self.run(name,(a,b),None,.029)

    def branch_stand(self,name,p,axis,limit):
        at = point(*p)
        # Incoming bends can be directly below the conduit axis. Start on the axis.
        hit,_,receiver = self.measured(at,axis,limit,name+" backing")
        self.bearings.append((name,p,world(hit),receiver.name))

    def trench(self):
        ground = bpy.data.objects["WLD-05 saved ground"]
        local = ground.matrix_world.inverted()
        for label,polygon in TRENCHES.items():
            coverage = []
            for a,b in zip(polygon,polygon[1:]):
                for x,z in samples(a,b,.30):
                    ok,hit,_,_=ground.ray_cast(local@point(x,8,z),local.to_3x3()@(-UP),distance=16)
                    assert ok,label
                    coverage.append((ground.matrix_world@hit).z-BURIED_Y)
            assert min(coverage)>.70,(label,min(coverage))
            self.buried_checks.append([label,len(coverage),round(min(coverage),3)])
            points = [(x,BURIED_Y,z) for x,z in polygon]
            if label=="yard service exit":
                points.insert(0,(33.76,-.37,67.57))
            self.run(label,points,None,.032)

    def exterior(self,i,top):
        (x,z),(dx,dz),(ix,iz)=RISERS[i]
        axis = point(dx,0,dz)
        grade,_,_,_ = self.structure.ray_cast(point(x,6,z),-UP,8)
        assert grade is not None and top-grade.z>.65,(i,grade,top)
        emergence = grade.z+.035
        # The buried tail emerges beside the wall. Its exposed riser is
        # strapped to the actual face above grade before entering the sleeve.
        tail = f"MC-{i:02d} buried riser foot"
        self.buried_names.add(tail)
        self.run(tail,[(x,BURIED_Y,z),(x,emergence,z)],None,.027)
        self.run(f"MC-{i:02d} outside riser",[(x,emergence,z),(x,top,z)],None,.027)
        for fraction in (.22,.55,.86):
            y=emergence+fraction*(top-emergence)
            self.branch_stand(f"MC-{i:02d} exterior riser",(x,y,z),axis,.32)
        self.sleeve(f"MC-{i:02d} sealed surface wall entry",(x,top,z),(ix,top,iz))
        return (ix,top,iz)

    def install_controls(self):
        for owner,(name,(x,y,z),switch) in enumerate(ROOMS):
            zone = dict(id=name, owner=owner, y=y, height=4.15)
            self.switch(zone,*switch)
            self.mounted_lamp(zone,f"mine-{name}-light",point(x,y+1,z),UP,3.2,point(1,0,0))

    def wired_load(self,i,feed):
        controller = self.controllers[i]
        lamp = self.loads[i]
        drop = next(r for r in self.routes if r["id"]==f"EM-{i:02d} switch drop")
        dx,dy,dz = drop["points"][-1]
        axis = point(*((self.controllers[i]["at"][j]-drop["points"][0][j]) if j!=1 else 0 for j in range(3))).normalized()
        for fraction in (.30,.76):
            low,high=drop["points"]
            h=low[1]+fraction*(high[1]-low[1])
            self.branch_stand(f"MC-{i:02d} switch drop",(dx,h,dz),axis,.16)
        lx,ly,lz = lamp["mount"]
        ceiling = ly-.065
        worktop = (dx,ceiling,dz)
        fx,fy,fz = feed
        # Two cable functions share the high service gutter. One powers the
        # room switch and the return conductor goes to the nearby fixture.
        self.run(f"MC-{i:02d} switched feeder",
                 [feed,(fx,ceiling,fz),(dx,ceiling,fz),worktop,(dx,dy,dz)])
        bend = (dx,ceiling,lz) if i==4 else (lx,ceiling,dz)
        self.run(f"MC-{i:02d} fixture return",
                 [worktop,bend,(lx,ceiling,lz),(lx,ly-.012,lz)])

    def underground(self):
        x = 29.29
        shaft = [(x,2.51,48.85),(x,2.18,48.32),(x,2.09,47.9),(x,-.74,41.40),
                 (x,-.92,39.8),(x,-3.36,33.65),(x,-3.36,32.55)]
        self.run("haul shaft wall raceway",shaft,point(1,0,0),.022)
        self.sleeve("headhouse shaft-lintel sleeve",(x,2.51,49.17),shaft[0])
        self.run("headhouse ceiling to shaft tap",[(x,2.935,52.85),(x,2.935,49.24),
                                                    (x,2.51,49.24),(x,2.51,49.17)])
        hub=(x,TOP,31)
        self.run("haul shaft foot wall return",[shaft[-1],(x,TOP,32.38)],point(1,0,0))
        self.run("haul shaft foot soffit",[(x,TOP,32.38),hub])
        # Keep the cable below each real lintel through the existing opening.
        def portal(name,a,b):
            self.run(name,[(a[0],LINTEL,a[2]),(b[0],LINTEL,b[2])])
            for end in (a,b):
                self.run(name+" end",[end,(end[0],LINTEL,end[2])],None)
        west=(20.22,TOP,31)
        ore_w=(19.78,TOP,31)
        self.run("haul west ceiling feeder",[hub,west])
        portal("haul ore-west opening",west,ore_w)
        east=(35.78,TOP,31)
        ore_e=(36.22,TOP,31)
        self.run("haul east ceiling feeder",[hub,east])
        portal("haul ore-east opening",east,ore_e)
        vent_edge=(11,TOP,41.77)
        vent=(11,TOP,42.23)
        core=(10,TOP,41.855)
        self.run("west ore branch and vent transfer",[ore_w,(11,TOP,31),vent_edge])
        portal("ore-to-vent opening",vent_edge,vent)
        self.run("lower chamber takeoff",[(11,TOP,39.40),(10,TOP,39.40),core])
        self.run("lower upper wall drop",[core,(10,-5.99,core[2])],point(0,0,1))
        self.sleeve("lower inter-storey slab sleeve",(10,-5.99,core[2]),(10,-6.19,core[2]))
        lower=(10,-6.225,core[2])
        self.run("lower ceiling junction",[(10,-6.19,core[2]),lower],None)
        cross_edge=(16,TOP,20.23)
        cross=(16,TOP,19.77)
        self.run("west ore to cross passage",[ore_w,(16,TOP,31),cross_edge])
        portal("cross passage west opening",cross_edge,cross)
        lift_edge=(43.77,TOP,14)
        lift=(44.23,TOP,14)
        self.run("cross passage to lift feeder",[cross,(16,TOP,14),lift_edge])
        portal("lift opening",lift_edge,lift)
        return {4:hub,5:(11,TOP,31),6:ore_e,7:(16,TOP,14),8:lift,9:lower,10:vent}

    def validate_and_place(self):
        # Freeze the measured support list before adding cable hardware to the scene.
        for name,points,axis,radius in self.runs:
            if axis is None:
                continue
            for a,b in zip(points,points[1:]):
                if axis == UP and math.hypot(a[0]-b[0],a[2]-b[2])<.10:
                    continue
                anchors = samples(a,b,1.15)
                for position in anchors[1:-1] or [tuple((x+y)/2 for x,y in zip(a,b))]:
                    self.branch_stand(name,position,axis,.52 if axis==UP else .18)
        sleeves = {name for name,_,_ in self.sealed}
        for name,a,b in self.sealed:
            start,end=point(*a),point(*b)
            offset=end-start
            crossing=self.contact(start,offset.normalized(),offset.length)
            assert crossing is not None,(name,a,b)
        for route in self.routes:
            a,b=map(lambda v:point(*v),route["points"])
            direction=(b-a).normalized()
            assert self.contact(a+direction*.012,direction,(b-a).length-.024) is None,route["id"]
            self.exposed_segments+=1
        for name,points,_,radius in self.runs:
            exposed = name not in self.buried_names
            if exposed and name not in sleeves:
                for a,b in zip(points,points[1:]):
                    start,end = point(*a),point(*b)
                    axis=end-start
                    if axis.length<=.027:
                        continue
                    direction=axis.normalized()
                    crossing=self.contact(start+direction*.012,direction,axis.length-.024)
                    assert crossing is None,(name,a,b,crossing)
                    self.exposed_segments+=1
            self.raceway(name,points,radius,exposed)
        self.verify_continuity()
        for i,(label,p,hit,receiver) in enumerate(self.bearings,1):
            a,b = point(*p),point(*hit)
            if (a-b).length<.012:
                continue
            direction=(b-a).normalized()
            sat=a-direction*.009
            foot=b+direction*.009
            self.tube(f"circuit saddle {i:03d}",world(sat),world(foot),.030,self.mount)

    def verify_continuity(self):
        segments = [(Vector(a),Vector(b)) for route in self.routes
                    for a,b in zip(route["points"],route["points"][1:]) if a!=b]
        def key(p):
            return tuple(round(x,5) for x in p)
        graph = {key(p):set() for pair in segments for p in pair}
        nodes = [(k,Vector(k)) for k in graph]
        for a,b in segments:
            delta=b-a
            along=delta.normalized()
            touches=[]
            for k,p in nodes:
                d=(p-a).dot(along)
                if -.00008<=d<=delta.length+.00008 and (p-a-along*d).length<.00008:
                    touches.append(k)
            for k in touches:
                graph[k].update(touches)
        visited=set()
        pending=[key((33.76,-.37,67.57))]
        while pending:
            node=pending.pop()
            if node in visited:
                continue
            visited.add(node)
            pending.extend(graph[node]-visited)
        for load in self.loads:
            x,y,z=load["mount"]
            assert key((x,y-.012,z)) in visited,load["id"]
        for i in range(len(self.controllers)):
            drop=next(r for r in self.routes if r["id"]==f"EM-{i:02d} switch drop")
            assert key(drop["points"][0]) in visited,drop["id"]



def build(out):
    source=json.loads((BASE/"construction.json").read_bytes())
    for kind,key in (("blend","blend_sha256"),("glb","glb_sha256"),("native","physics_sha256")):
        assert sha(ROOT/source[kind])==source[key]
    bpy.ops.wm.open_mainfile(filepath=str(ROOT/source["blend"]))
    bpy.context.preferences.filepaths.save_version=0
    assembly=MineDistribution()
    assembly.install_controls()
    assembly.trench()
    feeds={i:assembly.exterior(i,assembly.loads[i]["mount"][1]-.065) for i in RISERS}
    feeds.update(assembly.underground())
    assembly.run("headhouse circuit and shaft takeoff",[feeds[3],(29.29,2.935,52.85)])
    for i,feed in sorted(feeds.items()):
        assembly.wired_load(i,feed)
    assembly.validate_and_place()
    for row in assembly.sockets+assembly.controllers+assembly.loads+assembly.routes:
        for key in ("id","style","circuit"):
            if key in row:
                row[key]=row[key].replace("college-","mine-").replace("EM-","MC-")
    for obj in assembly.built:
        obj.name="Mine "+obj.name.removeprefix("College ").replace("EM-","MC-")

    out.mkdir(parents=True,exist_ok=True)
    targets={ext:out/f"mine-distribution.{ext}" for ext in ("blend","glb","hcs2")}
    additions=out/"_additions.glb"
    export(additions,assembly.built)
    append(ROOT/source["glb"],additions,targets["glb"],reuse_embedded_images=True)
    additions.unlink()
    native_write(ROOT/source["native"],targets["hcs2"],set(),assembly.built,
                 open_components={o.name for o in assembly.built if o.get("source_asset")})
    before=native_records(ROOT/source["native"])
    after=native_records(targets["hcs2"])
    assert after[:len(before)]==before
    bpy.ops.file.pack_all()
    bpy.ops.wm.save_as_mainfile(filepath=str(targets["blend"]),compress=True)
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.import_scene.gltf(filepath=str(targets["glb"]))
    for part in after[len(before):]:
        assert match_geometry(triangles(part),[t for t,_ in records(bpy.context.scene.objects[part[0]])]),part[0]
    hashes={ext:sha(f) for ext,f in targets.items()}
    lines=[("SOURCE",targets["glb"].relative_to(ROOT).as_posix(),hashes["glb"],
            targets["hcs2"].relative_to(ROOT).as_posix(),hashes["hcs2"])]
    lines.extend(line.split("\t") for line in (BASE/"arrival.tsv").read_text().splitlines()[1:])
    lines.extend(("SERVICE",s["id"],s["type"],s["room"],*s["position"],s["yaw"],s["style"]) for s in assembly.sockets)
    table(out/"arrival.tsv",lines)
    for name in ("door-survey.tsv","mission.tsv","rail-probes.tsv"):
        (out/name).write_bytes((BASE/name).read_bytes())
    table(out/"switches.tsv",[(row["room"],*row["feet"]) for row in assembly.controllers])
    table(out/"supports.tsv",[(name,*place,*hit,receiver) for name,place,hit,receiver in assembly.bearings])
    report=dict(exports=hashes,retained_components=len(before),added_components=len(after)-len(before),
                buried_checks=assembly.buried_checks, sealed_sleeves=assembly.sealed, loads=assembly.loads,
                exposed_segments=assembly.exposed_segments, connected_endpoints=24,
                controllers=assembly.controllers,routes=assembly.routes)
    (out/"electrical.json").write_text(json.dumps(report,indent=2)+"\n",encoding="utf-8",newline="\n")
    print("PASS Mine surface and workings distribution",len(assembly.loads),"loads,",len(assembly.bearings),"verified cable bearings")


def capture_cutaways(out):
    bpy.ops.wm.open_mainfile(filepath=str(out/"mine-distribution.blend"))
    scene=bpy.context.scene
    scene.render.engine="BLENDER_WORKBENCH"
    shading=scene.display.shading
    shading.color_type="MATERIAL"
    shading.show_cavity=True
    shading.show_shadows=True
    shading.cavity_type="BOTH"
    scene.view_settings.view_transform="Standard"
    scene.render.resolution_x=1350
    scene.render.resolution_y=900
    scene.render.resolution_percentage=100
    scene.render.image_settings.file_format="PNG"
    bpy.ops.object.camera_add()
    camera=bpy.context.object
    camera.data.type="ORTHO"
    scene.camera=camera
    frames=out/"captures"/"structure"
    frames.mkdir(parents=True,exist_ok=True)
    target=point(28.7,-1.15,41.2)
    eye=point(17.0,5.6,47.9)
    prefixes=("Mine haul east retain","Mine haul east return","Mine haul north upper",
              "Mine haul stair supported middle","Mine haul stair high landing","Mine haul full upper",
              "Mine haul re-spaced upper","Mine haul stair flight 1","Mine haul right",
              "Mine haul shaft wall raceway","Mine headhouse haul stair door head",
              "Mine headhouse shaft-lintel sleeve","Mine headhouse roof")
    for obj in scene.objects:
        if obj.type!="MESH":
            continue
        position=world(obj.location)
        shaft_fixing=obj.name.startswith("Mine circuit saddle") and 29.2<position[0]<29.4 and 32.45<position[2]<49
        obj.hide_render=not(obj.name.startswith(prefixes) or shaft_fixing)
    camera.location=eye
    camera.rotation_euler=(target-eye).to_track_quat("-Z","Y").to_euler()
    camera.data.ortho_scale=19.2
    scene.render.filepath=str(frames/"haul-shaft-service-raceway.png")
    bpy.ops.render.render(write_still=True)


if __name__=="__main__":
    args=sys.argv[sys.argv.index("--")+1:]
    output=Path(args[0]).resolve()
    if "--captures-only" in args:
        capture_cutaways(output)
    else:
        build(output)
