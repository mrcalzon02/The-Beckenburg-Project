"""Shape the Monastery's exterior runoff in the editable ground mesh."""
from dataclasses import dataclass
import json
import math
from pathlib import Path
import struct
import sys

import bpy
from mathutils import Vector

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools"))
from append_static_glb import append
from build_circulation_construction import native_write
from build_college_electrical import point
from build_college_grounds import import_scene, terrain_tree
from build_deliberate_entrances import write
from build_mine_drainage import slab
from build_monastery import rel
from build_prison_sally_gate import export
from build_roster_foundations import material
from compose_room_terrain import split_native
from export_roster_collision import face_keys
from prepare_cemetery_doors import write_tsv
from review_roster_macroterrain import records, sha

BASE = ROOT / "development/levels/rebuild"
PRIOR = BASE / "monastery-furnishing-pass"
STAGE = BASE / "monastery-ground-drainage-pass"
OUT = STAGE / "wld-10"
STEM = "monastery-with-ground-drainage"
STATUS = "MONASTERY_FORECOURT_AND_SERVICE_RUNOFF_STAGED"
GROUND = "WLD-10 saved ground"
DOWN = Vector((0, 0, -1))
SCHEDULES = ("runtime-input.tsv", "door-schedule.tsv", "electrical-schedule.tsv",
             "handrail-schedule.tsv", "valley-schedule.tsv", "flashing-schedule.tsv", "eave-schedule.tsv")


@dataclass(frozen=True)
class Swale:
    name: str
    sections: tuple
    bed: float
    bank: float

    @property
    def bounds(self):
        xs, zs, _ = zip(*self.sections)
        margin = self.bank + .6
        return min(xs)-margin, min(zs)-margin, max(xs)+margin, max(zs)+margin

    def nearest(self, x, z):
        best = (math.inf, 0, 0, 0)
        for i, ((ax, az, ay), (bx, bz, by)) in enumerate(zip(self.sections, self.sections[1:])):
            dx, dz = bx-ax, bz-az
            along = max(0, min(1, ((x-ax)*dx+(z-az)*dz)/(dx*dx+dz*dz)))
            distance = math.hypot(x-ax-along*dx, z-az-along*dz)
            if distance < best[0]:
                best = distance, i, along, ay+along*(by-ay)
        return best

    def cut(self, x, z, original):
        distance, section, along, invert = self.nearest(x, z)
        if distance >= self.bank:
            return original
        bank_fraction = max(0, (distance-self.bed)/(self.bank-self.bed))
        # Rounded banks meet the undisturbed terrace tangentially.
        blend = bank_fraction*bank_fraction*(3-2*bank_fraction)
        depth = max(0, original-invert)
        if self.name.endswith("gate overflow"):
            depth *= max(0, min(1, (z-63.055)/.13)) if section<2 else 1
        if section==len(self.sections)-2:
            depth *= 1-along
        return original-depth*(1-blend)


# These are stormwater sections in surveyed exterior soil. They have no
# relationship to the room connections or the runtime route guide.
SWALES = (
    Swale("west gate overflow", (
        (24.55, 63.20, -.064), (24.55, 63.56, -.080), (23.45, 64.0, -.101),
        (22.0, 63.88, -.135), (20.8, 62.8, -.214), (19.7, 61.6, -.31),
        (18.8, 60.4, -.369), (17.7, 59.65, -.394), (16.7, 58.6, -.412)), .24, .76),
    Swale("east gate overflow", (
        (39.45, 63.20, -.064), (39.45, 63.58, -.080), (40.95, 64.12, -.106),
        (42.5, 63.94, -.144), (43.9, 62.9, -.213), (45.7, 62.45, -.292), (48.3, 62.08, -.354),
        (50.10, 61.94, -.387), (51.25, 61.94, -.506)), .24, .76),
    Swale("service court interceptor", (
        (70.45, 18.55, .13), (70.55, 21, .034), (70.56, 24, -.021),
        (70.45, 27.1, -.062), (70.30, 29.6, -.135), (70.22, 31.7, -.248),
        (70.00, 34.4, -.461), (69.8, 36.0, -.56)), .30, .96),
)


def overlap(coords, bounds):
    x0, z0, x1, z1 = bounds
    return (min(p.x for p in coords)<=x1 and max(p.x for p in coords)>=x0
            and min(-p.y for p in coords)<=z1 and max(-p.y for p in coords)>=z0)


def edge(a, b):
    return (a,b) if a<b else (b,a)


def refine(points, faces, bounds):
    for _ in range(5):
        selected = set()
        for (a,b,c), _ in faces:
            if not any(overlap([points[a],points[b],points[c]], box) for box in bounds):
                continue
            selected.update(edge(u,v) for u,v in ((a,b),(b,c),(c,a)) if (points[u]-points[v]).length>.21)
        if not selected:
            break
        middle = {}
        for a,b in sorted(selected):
            middle[a,b] = len(points)
            points.append((points[a]+points[b])*.5)
        refined = []
        for (a,b,c), finish in faces:
            ab,bc,ca = middle.get(edge(a,b)),middle.get(edge(b,c)),middle.get(edge(c,a))
            if all(mid is not None for mid in (ab,bc,ca)):
                triangles = [(a,ab,ca),(ab,b,bc),(ca,bc,c),(ab,bc,ca)]
            else:
                triangles = [(a,b,c)]
                for u,v,mid in ((a,b,ab),(b,c,bc),(c,a,ca)):
                    if mid is None:
                        continue
                    index, triangle = next((i,t) for i,t in enumerate(triangles) if u in t and v in t)
                    a0,a1,a2 = next((t0,t1,t2) for t0,t1,t2 in
                                    (triangle,triangle[1:]+triangle[:1],triangle[2:]+triangle[:2])
                                    if {t0,t1}=={u,v})
                    triangles[index:index+1] = [(a0,mid,a2),(mid,a1,a2)]
            refined.extend((triangle,finish) for triangle in triangles)
        faces = refined
    return faces


def sculpt(ground):
    before = terrain_tree(ground)
    points, indices, addresses = [], {}, []
    for v in ground.data.vertices:
        pos = ground.matrix_world @ v.co
        key = tuple(pos)
        if key not in indices:
            indices[key] = len(points)
            points.append(pos)
        addresses.append(indices[key])
    assert all(len(face.vertices)==3 for face in ground.data.polygons)
    faces = [(tuple(addresses[i] for i in face.vertices), face.material_index)
             for face in ground.data.polygons]
    bounds = [unit.bounds for unit in SWALES]
    faces = refine(points,faces,bounds)
    aggregate = material("Monastery swale washed river aggregate", (.31, .275, .208))
    materials = [*ground.data.materials,aggregate]
    finish_index = len(materials)-1
    modified = set()
    for i, pos in enumerate(points):
        if not any(overlap((pos,), box) for box in bounds):
            continue
        x,z,y = pos.x,-pos.y,pos.z
        new_y = min(unit.cut(x,z,y) for unit in SWALES)
        if new_y < y-.00001:
            pos.z = new_y
            modified.add(i)
    assert modified
    mesh = bpy.data.meshes.new("WLD-10 graded exterior ground")
    mesh.from_pydata(points,[],[face for face,_ in faces])
    assert not mesh.validate(verbose=True,clean_customdata=False)
    for mat in materials:
        mesh.materials.append(mat)
    uv = mesh.uv_layers.new(name="World metre terrain UV")
    for face,(vertices,finish) in zip(mesh.polygons,faces):
        face.material_index = finish
        if any(i in modified for i in vertices):
            center = sum((points[i] for i in vertices),Vector())/3
            if any(unit.nearest(center.x,-center.y)[0]<=unit.bed+.09 for unit in SWALES):
                face.material_index = finish_index
        for corner,i in zip(face.loop_indices,vertices):
            uv.data[corner].uv = (points[i].x/8,points[i].y/8)
    surface = bpy.data.objects.new(mesh.name,mesh)
    bpy.context.scene.collection.objects.link(surface)
    surface["role"] = "saved editor terrain / exterior stormwater grading"
    bpy.data.objects.remove(ground,do_unlink=True)
    return surface,before,len(modified)


def height(tree, x, z):
    hit, _, _, _ = tree.ray_cast(point(x,10,z),DOWN,20)
    assert hit is not None, (x,z)
    return hit.z


def sample_sections(tree):
    receipts = []
    for unit in SWALES:
        rows = []
        station = 0
        for (ax,az,_), (bx,bz,_) in zip(unit.sections,unit.sections[1:]):
            length = math.hypot(bx-ax,bz-az)
            n = math.ceil(length/.2)
            for i in range(n):
                fraction = i/n
                x, z = ax+(bx-ax)*fraction, az+(bz-az)*fraction
                rows.append((station+length*fraction,x,height(tree,x,z),z))
            station += length
        x,z,_ = unit.sections[-1]
        rows.append((station,x,height(tree,x,z),z))
        rises = [(round(b[0],3),round(b[2]-a[2],4)) for a,b in zip(rows,rows[1:]) if b[2]>a[2]+.001]
        fall = rows[0][2]-rows[-1][2]
        print("Swale", unit.name, "fall", fall, "local rises", rises)
        assert fall>.17 and not rises, (unit.name,fall,rises)
        receipts.append(dict(name=unit.name, total_fall=fall, length=station, centerline=rows,
                             bed_half_width=unit.bed, bank_half_width=unit.bank))
    return receipts


def overflows(ground, retired):
    built = []
    for i, x in enumerate((24.55,39.45), 1):
        # The landward lip of each forecourt gravel box receives a spill notch
        # over its intact footing. The three remaining original rims stay put.
        prefix = f"Monastery public gate outlet {i}"
        name = prefix+" rim 4"
        source = bpy.context.scene.objects[name]
        mat = source.data.materials[0]
        base = min((source.matrix_world @ v.co).z for v in source.data.vertices)
        crest = max((source.matrix_world @ v.co).z for v in source.data.vertices)
        assert abs(base-.023)<.002 and abs(crest-.073)<.002
        for side, (lo, hi) in zip(("west","east"), ((x-.174,x-.09),(x+.09,x+.174))):
            built.append(slab(prefix+" notched rim "+side,(lo,62.994,hi,63.06),
                              lambda x,z:base,lambda x,z:crest,mat,"founded overflow rim"))
        def top(x,z):
            return .045-(z-62.985)*.26
        def bottom(x,z):
            return min(top(x,z)-.025,.018) if z<=63.06 else min(top(x,z)-.035,height(ground,x,z)-.01)
        bounds = (x-.09,62.985,x+.09,63.2)
        built.append(slab(prefix+" stone overflow sill",bounds,bottom,top,mat,
                          "spillway from the gravel sump to the formed swale"))
        assert top(x,63.2)>height(ground,x,63.2)
        retired.add(name)
        bpy.data.objects.remove(source,do_unlink=True)
    return built


def compare_ground(before, after):
    # Public arrival, garden and service slab remain on their original mesh datums.
    probes = [(x,z) for x in (27,29,31,32,33,35,37) for z in (63,65,67,70,75,80,85)]
    probes += [(x,z) for x in (61,64,67,68.6) for z in (18,21,24,27,29)]
    probes += [(x,z) for x in (16,22,29,35,42) for z in (24,30,36,42,48)]
    for x,z in probes:
        assert abs(height(before,x,z)-height(after,x,z))<.0002,(x,z)
    return len(probes)


def build():
    receipt = json.loads((PRIOR/"checkpoint.json").read_bytes())
    roster = json.loads((PRIOR/"roster.json").read_bytes())
    row = next(site for site in roster["active_sources"] if site["id"]=="WLD-10")
    source = ROOT/row["construction"]
    old = PRIOR/"wld-10"
    inputs = (source, *(ROOT/row[k] for k in ("blend","glb","native")), *(old/name for name in SCHEDULES))
    for path in inputs:
        assert sha(path)==receipt["project_bytes_sha256"][rel(path)],path
    meta = json.loads(source.read_bytes())
    bpy.ops.wm.open_mainfile(filepath=str(ROOT/row["blend"]))
    bpy.context.preferences.filepaths.save_version = 0
    ground = bpy.context.scene.objects[GROUND]
    prior_native = ROOT/row["native"]
    original = split_native(prior_native)
    original_ground = [part for part in original if part[0].startswith("terrain-")]
    assert original_ground
    ground, before, modified = sculpt(ground)
    after = terrain_tree(ground)
    probes = compare_ground(before,after)
    swales = sample_sections(after)
    retired = set()
    built = overflows(after,retired)
    assert retired=={f"Monastery public gate outlet {i} rim 4" for i in (1,2)}

    OUT.mkdir(parents=True,exist_ok=True)
    blend,model,collision = (OUT/(STEM+ext) for ext in (".blend",".glb",".hcs2"))
    fragment = ROOT/"build/monastery-ground-drainage.glb"
    export(fragment,[ground,*built])
    append(ROOT/row["glb"],fragment,model,remove_mesh_names=[GROUND,*sorted(retired)],reuse_embedded_images=True)
    retained = [part for part in original if part[0] not in retired and part not in original_ground]
    filtered = ROOT/"build/monastery-ground-drainage-retained.hcs2"
    filtered.write_bytes(struct.pack(">III",0x48435332,len(retained),sum(part[1] for part in retained))
                         +b"".join(part[2] for part in retained))
    added = native_write(filtered,collision,set(),[ground,*built],open_components={ground.name})
    exported = split_native(collision)
    assert exported[:len(retained)]==retained and len(exported)-len(retained)==len(added)
    bpy.ops.file.pack_all()
    bpy.ops.wm.save_as_mainfile(filepath=str(blend),compress=True)
    objects = import_scene(model)
    for name, triangles in added:
        assert face_keys(records(objects[name]))==face_keys(triangles),name

    for name in SCHEDULES:
        (OUT/name).write_bytes((old/name).read_bytes())
    write_tsv(OUT/"drainage-survey.tsv",[
        ("swale","distance","x","native_invert_y","z"),
        *((unit["name"],*point) for unit in swales for point in unit["centerline"])])
    meta.update(status=STATUS,before_construction=rel(source),before_construction_sha256=sha(source),
                source_glb=row["glb"],source_glb_sha256=sha(ROOT/row["glb"]),
                source_physics=row["native"],source_physics_sha256=sha(prior_native),
                builder=rel(Path(__file__)),builder_sha256=sha(Path(__file__)),
                glb_sha256=sha(model),physics_sha256=sha(collision),blend_sha256=sha(blend),
                removed_source_meshes=sorted({GROUND,*retired}), terrain_mesh=added[0][0],
                retained_native_records=len(retained), native_matched_parts=[name for name,_ in added],
                swales=swales, unmodified_circulation_probes=probes, modified_terrain_vertices=modified)
    construction = OUT/"construction.json"
    write(construction,meta)
    row.update(construction=rel(construction),blend=rel(blend),glb=rel(model),native=rel(collision),
               glb_sha256=sha(model),native_sha256=sha(collision))
    next(site for site in roster["sites"] if site["id"]=="WLD-10")["decision"] = STATUS
    roster["baseline"] = rel(PRIOR/"roster.json")
    write(STAGE/"roster.json",roster)
    write_tsv(STAGE/"source-hashes.tsv",[(path.name,sha(path)) for path in
                                        (model,collision,*(OUT/name for name in SCHEDULES[:3]))])
    print("PASS gravity profiles, matched terrain and spillways, preserved native construction and",probes,"ground datums")


if __name__=="__main__":
    build()
