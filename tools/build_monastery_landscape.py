"""Fit Monastery grounds dressing and a surface finish to the surveyed landscape."""
import json
import math
from pathlib import Path
import struct
import sys
import zlib

import bmesh
import bpy
from mathutils import Matrix, Vector
import numpy as np

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/"tools"))
from append_static_glb import append,read
from build_circulation_construction import native_write
from build_college_electrical import ASSETS,bounds,point
from build_college_grounds import import_scene,terrain_tree
from build_deliberate_entrances import write
from build_mine_drainage import mesh_object
from build_monastery import GARDEN_BEDS,rel
from build_monastery_ground_drainage import BASE,SCHEDULES,SWALES
from build_monastery_north_drainage import CHANNELS,NEW_GROUND,audit_level_budget
from build_prison_sally_gate import export
from build_roster_foundations import material
from compose_room_terrain import split_native
from export_roster_collision import face_keys
from prepare_cemetery_doors import write_tsv
from prune_static_glb import compact,pack
from review_roster_macroterrain import records,sha
from verify_college_services import bvh,match_geometry

PRIOR = BASE/"monastery-north-drainage-pass"
STAGE = BASE/"monastery-landscape-pass"
OUT = STAGE/"wld-10"
STEM = "monastery-with-landscape"
STATUS = "MONASTERY_TERRACE_HERB_COURT_AND_HILLSIDE_FINISHED_STAGED"
ROUGH = ROOT/"development/levels/primitive-props-v1/models"
GROUND = NEW_GROUND
DOWN = Vector((0,0,-1))
ATLAS_SIZE = 1024

LANDMARKS = (
    ("court well","level_blockout_monastery_well",20.8,35.2,.27,.84,"garden"),
    ("west cloister garden bench","level_camp_bench",16.15,29.4,math.pi/2,1,"garden"),
    ("east cloister garden bench","level_camp_bench",41.95,40.1,-math.pi/2,1,"garden"),
    ("arrival waiting bench","level_camp_bench",22.8,72.8,math.pi/2,1,"entry"),
)
ROCKS = (
    ("forecourt west shoulder","level_woodland_boulder",20.65,69.35,.62,.91),
    ("roadhead west shoulder","level_woodland_boulder",19.9,76.8,-.31,.76),
    ("roadhead east shoulder","level_woodland_boulder",44.6,74.1,.45,.90),
    ("forecourt eastern scree","level_woodland_boulder",45.05,67.05,2.42,.80),
    ("western hillside outcrop","level_woodland_boulder",3.95,12.4,2.72,1.16),
    ("eastern hillside outcrop","level_woodland_boulder",48.86,9.55,1.22,1.16),
    ("old roadhead stump","level_woodland_stump",20.15,81.70,.55,.96),
)
PINES = (
    ("western roadhead pine","level_camp_pine_refined_a",18.80,72.6,.33,.78),
    ("eastern roadhead pine","level_camp_pine_refined_b",45.63,80.6,1.31,.86),
    ("western hillside pine","level_camp_pine_refined_b",2.25,11.50,2.32,.82),
)
TUFTS = (
    (19.7,69.0),(20.1,71.8),(18.7,75.3),(20.8,78.1),(22.1,81.2),(20.7,83.1),
    (44.5,67.9),(45.9,69.7),(43.7,71.6),(45.3,74.4),(45.1,77),
    (42.95,80.35),(43.2,83.8),(4.2,8.4),(4.92,13.0),(3.1,14.4),(47.75,10.9),
    (48.92,12.3),(48.02,14.05),(12.1,-.5),(16.55,-.92),(39.65,-.40),
)


def png(path, rgb):
    def chunk(kind, contents):
        return struct.pack(">I",len(contents))+kind+contents+struct.pack(">I",zlib.crc32(kind+contents))
    rows = b"".join(b"\0"+row.tobytes() for row in rgb[::-1])
    header = struct.pack(">IIBBBBB",rgb.shape[1],rgb.shape[0],8,2,0,0,0)
    path.write_bytes(b"\x89PNG\r\n\x1a\n"+chunk(b"IHDR",header)+chunk(b"IDAT",zlib.compress(rows,7))+chunk(b"IEND",b""))


def lattice_noise(rng,n,steps):
    values = rng.random((steps+1,steps+1))
    axis = np.linspace(0,steps,n,endpoint=False)
    lo = axis.astype(int)
    blend = axis-lo
    blend = blend*blend*(3-2*blend)
    a,b = lo[None,:],lo[:,None]
    u,v = blend[None,:],blend[:,None]
    return (1-v)*((1-u)*values[b,a]+u*values[b,a+1])+v*((1-u)*values[b+1,a]+u*values[b+1,a+1])


def finish_ground(ground,path):
    n = ATLAS_SIZE
    points = [ground.matrix_world@v.co for v in ground.data.vertices]
    x0,x1 = min(p.x for p in points),max(p.x for p in points)
    y0,y1 = min(p.y for p in points),max(p.y for p in points)
    source = [face.material_index for face in ground.data.polygons]
    mesh = terrain_tree(ground)
    tags = np.zeros((n,n),dtype=np.uint8)
    for j in range(n):
        y = y0+(j+.5)/n*(y1-y0)
        for i in range(n):
            x = x0+(i+.5)/n*(x1-x0)
            _,_,face,_ = mesh.ray_cast(Vector((x,y,8)),DOWN,12)
            if face is not None:
                tags[j,i]=source[face]
    rng = np.random.default_rng(10124)
    broad = lattice_noise(rng,n,19)
    grain = lattice_noise(rng,n,151)
    fine = rng.random((n,n))
    palettes = np.array([[86,91,64],[129,119,100],[99,91,72],[101,92,74]],dtype=np.float32)
    # Blend the actual existing terrain bands only in the color atlas. The
    # physical riverbed, banks, riding surface and existing supports stay put.
    rgb = np.zeros((n,n,3),dtype=np.float32)
    for off in range(len(palettes)):
        weight = (tags==off).astype(np.float32)
        for _ in range(4):
            edge = np.pad(weight,1,mode="edge")
            weight = (weight*4+edge[1:-1,:-2]+edge[1:-1,2:]+edge[:-2,1:-1]+edge[2:,1:-1])/8
        rgb += weight[:,:,None]*palettes[off]
    tone = 35*(broad-.5)+26*(grain-.5)+13*(fine-.5)
    # Scattered grain and sparse dark inclusions bring mineral variation into
    # the laid gravel while the woodland soil stays softer.
    exposed = tags!=0
    tone += np.where(exposed,(fine>.92)*14-(fine<.055)*13,(fine<.14)*-10)
    rgb += tone[:,:,None]
    png(path,np.clip(rgb,0,255).astype(np.uint8))
    image = bpy.data.images.load(str(path))
    image.pack()
    # Keep separate Blender material slots so the large terrain still meets
    # the index/vertex budget of each individual packaged primitive.
    for i,old in enumerate(tuple(ground.data.materials)):
        finish = material(old.name+" color finish",tuple(float(v/255) for v in palettes[i]))
        tree = finish.node_tree
        sample = tree.nodes.new("ShaderNodeTexImage")
        sample.image = image
        sample.extension = "EXTEND"
        tree.links.new(sample.outputs["Color"],tree.nodes.get("Principled BSDF").inputs["Base Color"])
        ground.data.materials[i] = finish
    uv = ground.data.uv_layers.active
    for loop in ground.data.loops:
        p = points[loop.vertex_index]
        uv.data[loop.index].uv = ((p.x-x0)/(x1-x0),(p.y-y0)/(y1-y0))
    return dict(path=rel(path),sha256=sha(path),size=n,terrain_extent=(x0,-y1,x1,-y0))


class Landscape:
    def __init__(self,ground,layout):
        self.scene = bpy.context.scene
        self.ground = ground
        self.soil = terrain_tree(ground)
        self.original = tuple(o for o in self.scene.objects if o.type=="MESH" and o!=ground)
        self.routes = route_samples(layout)
        self.models = {}
        self.render = []
        self.solid = []
        self.proof = []

    def close_well_masonry(self):
        fit = next(f for f in self.proof if f["name"]=="court well")
        x,y,z = fit["root"]
        well = self.scene.objects[fit["components"][0]]
        assert all("blockout" in mat.name for mat in well.data.materials)
        bench = self.scene.objects[next(f for f in self.proof if f["name"]=="west cloister garden bench")["components"][0]]
        well.data.materials[1] = bench.data.materials[0]
        well.data.materials[2] = material("Monastery well blackened iron",(.15,.13,.104))
        well.data.materials[3] = material("Monastery well hemp line",(.26,.19,.12))
        stone = material("Monastery well fitted limestone",(.39,.365,.31))
        for name,inside,outside,low,high in (
            ("stone liner",.545,.739,y-.008,y+.972),
            ("continuous coping",.548,.868,y+.955,y+1.015),
        ):
            n = 24
            profile = ((inside,low),(outside,low),(outside,high),(inside,high))
            vertices = [point(x+r*math.cos(i*math.tau/n),datum,z+r*math.sin(i*math.tau/n))
                        for r,datum in profile for i in range(n)]
            faces = [tuple(j*n+i for j,i in ((k,a),(k,b),(m,b),(m,a)))
                     for k,m in ((0,1),(1,2),(2,3),(3,0))
                     for a,b in ((i,(i+1)%n) for i in range(n))]
            obj = mesh_object("Monastery landscape court well "+name,vertices,faces,stone,"continuous well masonry")
            self.render.append(obj)
            self.solid.append(obj)
            fit["components"].append(obj.name)
            fit["native_components"].append(obj.name)
        # Both the original blocks and their new lining bear on the same
        # surveyed patio. The lining closes the visual and physical gaps.
        assert all(abs(self.soil.ray_cast(point(x+.74*math.cos(a),1,z+.74*math.sin(a)),DOWN,2)[0].z-y)<.003
                   for a in (i*math.tau/16 for i in range(16)))

    def load(self,asset):
        if asset not in self.models:
            source = (ROUGH if asset.startswith("level_blockout_") else ASSETS)/(asset+".glb")
            before = set(self.scene.objects)
            bpy.ops.import_scene.gltf(filepath=str(source))
            objects = set(self.scene.objects)-before
            bpy.context.view_layer.update()
            pieces = []
            for obj in sorted(objects,key=lambda o:o.name):
                if obj.type!="MESH":
                    continue
                if asset.startswith("level_camp_pine_"):
                    for bark in (True,False):
                        mesh = obj.data.copy()
                        bm = bmesh.new()
                        bm.from_mesh(mesh)
                        rejected = [f for f in bm.faces if ("bark" in mesh.materials[f.material_index].name.lower())!=bark]
                        bmesh.ops.delete(bm,geom=rejected,context="FACES")
                        if bm.faces:
                            bm.to_mesh(mesh)
                            pieces.append((mesh,obj.matrix_world.copy(),bark))
                        bm.free()
                else:
                    pieces.append((obj.data,obj.matrix_world.copy(),True))
            assert pieces,asset
            for obj in objects:
                bpy.data.objects.remove(obj,do_unlink=True)
            self.models[asset] = (source,pieces)
        return self.models[asset]

    def place(self,label,asset,x,z,yaw,scale,zone,landmark=False,foliage=False,bed=None):
        source,pieces = self.load(asset)
        rotation = Matrix.Rotation(yaw,4,"Z")@Matrix.Scale(scale,4)
        vertices = [rotation@matrix@v.co for mesh,matrix,_ in pieces for v in mesh.vertices]
        low = min(v.z for v in vertices)
        roots = [v for v in vertices if v.z<=low+.12*scale]
        cx = (min(v.x for v in roots)+max(v.x for v in roots))*.5
        cz = (min(v.y for v in roots)+max(v.y for v in roots))*.5
        support = self.soil if bed is None else bvh([bed])
        samples = []
        for v in roots:
            px,pz = x+(v.x-cx),z-(v.y-cz)
            contact,_,_,_ = support.ray_cast(point(px,5,pz),DOWN,10)
            assert contact is not None,(label,px,pz)
            samples.append((px,contact.z,pz))
        ground_low,ground_high = min(y for _,y,_ in samples),max(y for _,y,_ in samples)
        assert ground_high-ground_low<.35,(label,ground_low,ground_high)
        if landmark:
            assert ground_high-ground_low<.006,(label,ground_low,ground_high)
        datum = ground_low-(0 if landmark else .039*scale)
        frame = Matrix.Translation(point(x,datum,z)-Vector((cx,cz,low)))@rotation
        new,collision = [],[]
        for i,(mesh,matrix,bark) in enumerate(pieces,1):
            part = bpy.data.objects.new(f"Monastery landscape {label} {i:02d}",mesh)
            self.scene.collection.objects.link(part)
            part.matrix_world = frame@matrix
            part["source_asset"],part["zone"] = asset,zone
            solid = bark and not foliage
            part["role"] = "anchored physical grounds fitting" if solid else "supported visual planting"
            new.append(part)
            if solid:
                collision.append(part)
        bpy.context.view_layer.update()
        lo,hi = bounds(new)
        margin,route = min((math.hypot(max(lo[0]-a,0,a-hi[0]),max(lo[2]-c,0,c-hi[2])),key)
                          for key,(a,b,c) in self.routes if lo[1]-1.75<=b<=hi[1]+.2)
        assert margin>(.95 if asset.startswith("level_camp_pine") else .48),(label,route,margin)
        if landmark:
            fixtures = [o for o in self.original if not any(word in o.name.lower() for word in ("floor","paving","soakaway gravel filter bed"))]
            assert not bvh(fixtures).overlap(bvh(new)),label
        if zone!="garden":
            assert not any(swale.nearest(px,pz)[0] < swale.bank+.13 for px,_,pz in samples for swale in (*SWALES,*CHANNELS)),label
        self.render.extend(new)
        self.solid.extend(collision)
        self.proof.append(dict(name=label,asset=asset,asset_sha256=sha(source),root=(x,datum,z),support_range=(ground_low,ground_high),
          mesh_bounds=(lo,hi),shortest_route=route,route_clearance=margin,components=[part.name for part in new],
          native_components=[part.name for part in collision],bearing_probes=len(samples)))


def route_samples(layout):
    routes = {}
    for line in layout.read_text(encoding="utf-8").splitlines():
        row = line.split("\t")
        if row[0]=="POINT":
            routes.setdefault(row[1],[]).append(tuple(map(float,row[2:5])))
    records = []
    for key,points in routes.items():
        for a,b in zip(points,points[1:]):
            n = max(1,math.ceil(math.dist(a,b)/.18))
            records.extend((key,tuple(x+(y-x)*i/n for x,y in zip(a,b))) for i in range(n+1))
    return records


def lay_out(landscape):
    for label,asset,x,z,angle,scale,zone in LANDMARKS:
        landscape.place(label,asset,x,z,angle,scale,zone,landmark=True)
    landscape.close_well_masonry()
    for label,asset,x,z,angle,scale in (*ROCKS,*PINES):
        landscape.place(label,asset,x,z,angle,scale,"hillside")
    plants = ("level_woodland_grass","level_woodland_fern","level_woodland_scrub","level_woodland_flowers")
    for i,(x,z) in enumerate(TUFTS):
        landscape.place(f"shoulder planting {i+1:02}",plants[i%3],x,z,i*.77,.65+.09*(i%4),"hillside",foliage=True)
    # Isolated bands of herbs and moisture-tolerant foliage inside all four
    # stone planters, with harvest access from the existing aisle.
    for i,(a,b,c,d) in enumerate(GARDEN_BEDS,1):
        receiver = landscape.scene.objects[f"Monastery garden bed {i} planted soil region0"]
        for row in range(3):
            for n in range(4):
                x = a+1.08+(c-a-2.1)*n/3+.16*(row%2)
                z = b+1.08+(d-b-2.1)*row/2+.12*(n%2)
                kind = plants[(i+row+n)%len(plants)]
                landscape.place(f"bed {i} planting {row*4+n+1:02}",kind,x,z,n*.68+row*1.17,
                                 .64+.06*((i+n+row)%4),"garden",foliage=True,bed=receiver)


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
    ground = bpy.context.scene.objects[GROUND]
    ground_records = records(ground)
    old = PRIOR/"wld-10"
    landscape = Landscape(ground,old/"runtime-input.tsv")
    lay_out(landscape)

    OUT.mkdir(parents=True,exist_ok=True)
    atlas = finish_ground(ground,OUT/"monastery-ground-finish.png")
    assert ground_records==records(ground)
    model,collision,blend = (OUT/(STEM+ext) for ext in (".glb",".hcs2",".blend"))
    fragment = ROOT/"build/monastery-landscape-fragment.glb"
    intermediate = ROOT/"build/monastery-landscape-unpruned.glb"
    export(fragment,[ground,*landscape.render])
    # Give the render-only terrain replacement a distinct scene name during export.
    # Its native component retains the previous identifier and bytes.
    doc,buf = read(fragment)
    new_ground, = [n for n in doc["nodes"] if n.get("name")==GROUND]
    new_ground["name"] += " finished"
    fragment.write_bytes(pack(doc,buf))
    append(assets["glb"],fragment,intermediate,remove_mesh_names=[GROUND],reuse_embedded_images=True)
    compact(intermediate,model)
    budget = audit_level_budget(model)
    assert budget["bytes"]<16_000_000
    added = native_write(assets["native"],collision,set(),landscape.solid,
                         open_components={o.name for o in landscape.solid})
    before,after = split_native(assets["native"]),split_native(collision)
    assert after[:len(before)]==before and len(after)==len(before)+len(added)
    bpy.ops.file.pack_all()
    bpy.ops.wm.save_as_mainfile(filepath=str(blend),compress=True)
    loaded = import_scene(model)
    assert face_keys(records(loaded[GROUND+" finished"]))==face_keys(ground_records)
    for name,triangles in added:
        assert match_geometry([face for face,_ in triangles],[face for face,_ in records(loaded[name])]),name
    schedules = (*SCHEDULES,"drainage-survey.tsv","court-drainage.tsv","north-drainage.tsv")
    for name in schedules:
        p = old/name
        assert sha(p)==prior["project_bytes_sha256"][rel(p)]
        (OUT/name).write_bytes(p.read_bytes())
    write_tsv(OUT/"landscape-fittings.tsv",[("name","asset","x","y","z","route_clearance","native_parts"),
               *((r["name"],r["asset"],*r["root"],r["route_clearance"],len(r["native_components"]))
                 for r in landscape.proof)])
    meta.update(status=STATUS,before_construction=rel(assets["construction"]),source_glb=rel(assets["glb"]),
        source_glb_sha256=sha(assets["glb"]),terrain_color_atlas=atlas,landscape_fittings=landscape.proof,
        retained_native_records=len(before),landscape_added_matched_parts=[name for name,_ in added],
        landscape_glb_budget=budget,render_terrain_mesh=GROUND+" finished",
        builder=rel(Path(__file__)),builder_sha256=sha(Path(__file__)),glb_sha256=sha(model),
        physics_sha256=sha(collision),blend_sha256=sha(blend))
    construction = OUT/"construction.json"
    write(construction,meta)
    row.update(construction=rel(construction),blend=rel(blend),glb=rel(model),native=rel(collision),
               glb_sha256=sha(model),native_sha256=sha(collision))
    next(r for r in roster["sites"] if r["id"]=="WLD-10")["decision"]=STATUS
    roster["baseline"]=rel(PRIOR/"roster.json")
    write(STAGE/"roster.json",roster)
    write_tsv(STAGE/"source-hashes.tsv",[(p.name,sha(p)) for p in (model,collision,*(OUT/n for n in SCHEDULES[:3]))])
    print("PASS",len(landscape.proof),"grounded placements,",len(added),"matching native fittings, and exact prior terrain")


if __name__=="__main__":
    build()
