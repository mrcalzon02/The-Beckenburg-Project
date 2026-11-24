"""Build the Mine service-yard supply shelter against the accepted handrail scene."""
import json
import math
from pathlib import Path
import sys

import bmesh
import bpy
from mathutils import Matrix, Vector

ROOT = Path(__file__).resolve().parents[1]
sys.path[:0] = [str(ROOT/"tools"), str(ROOT/"development/levels/blockouts")]
from append_static_glb import append
from build_circulation_construction import native_write
from build_college_electrical import Assembly, point
from build_lookout_native_arrival import table
from build_lookout_power import LookoutElectrical
from build_prison_sally_gate import export
from build_roster_foundations import material
from review_roster_macroterrain import records, sha
from verify_college_containment import triangles
from verify_college_electrical import native_records
from verify_college_services import match_geometry

BASE = ROOT/"development/levels/rebuild/mine-handrail-pass"
GROUND = (33.95, 67.35, 39.55, 70.95)
FLOOR = .018
ENTRY = {"id": "mine-service-yard", "owner": 13, "y": FLOOR, "height": 2.7}
APPROACHES = {
    "mine-service-from-arrival": [(28,0,76), (32.4,0,73.6), (36.75,0,72.25),
                                  (36.75,0,71.34), (36.75,FLOOR,70.75), (36.75,FLOOR,69.68)],
    "mine-service-from-yard": [(30,0,69), (32.1,0,71.85), (36.75,0,72.25),
                               (36.75,0,71.34), (36.75,FLOOR,70.75), (36.75,FLOOR,69.68)],
}


def plant_coordinates(x, y, z):
    # The canonical plant package faces back toward the Mine's cargo arrival.
    return 73.55-x, y, 136.95-z


def grade(z):
    return 2.67 + .116*(z-GROUND[1])


def bearing_survey(builder):
    for xi in range(31):
        for zi in range(23):
            x = 33.75 + xi*.20
            z = 67.15 + zi*4.20/22
            hit, _, support = builder.measured(point(x,.35,z), Vector((0,0,-1)), .5, "Mine shed bearing")
            assert abs(hit.z)<.002 and support.name=="WLD-05 saved ground", (x,z,support.name)
    return 31*23


def build_shelter(builder):
    left, back, right, front = GROUND
    frame = material("Mine pressure-treated shed framing", (.25,.19,.12))
    cladding = material("Mine oxidized galvanized cladding", (.27,.31,.29))
    roofing = material("Mine zinc roof panels", (.35,.39,.37))

    def box(name, rect, low, high, mat, role):
        return builder.box("service yard "+name, rect, low, high, mat, role, ENTRY["id"])

    def incline(obj, plane, both=False):
        top = max((obj.matrix_world @ v.co).z for v in obj.data.vertices)
        inverse = obj.matrix_world.inverted()
        for vertex in obj.data.vertices:
            p = obj.matrix_world @ vertex.co
            if both or abs(p.z-top)<.001:
                p.z += grade(-p.y)-plane
                vertex.co = inverse @ p
        obj.data.update()

    # The slab stays level under the plant. Only the arrival-side threshold is beveled.
    outline = [(left,back),(right,back),(right,front-.13),(right,front+.40),
               (left,front+.40),(left,front-.13)]
    vertices = [point(x,-.24,z) for x,z in outline]
    vertices.extend(point(x,-.008 if z>front else FLOOR,z) for x,z in outline)
    faces = [(0,1,2,3,4,5),(6,7,8,11),(8,9,10,11)]
    faces.extend((i,(i+1)%6,(i+1)%6+6,i+6) for i in range(6))
    mesh = bpy.data.meshes.new("Mine service-yard formed slab")
    mesh.from_pydata(vertices,[],faces)
    bm = bmesh.new()
    bm.from_mesh(mesh)
    bmesh.ops.recalc_face_normals(bm,faces=list(bm.faces))
    assert bm.calc_volume(signed=True)>0
    bm.to_mesh(mesh)
    bm.free()
    slab = bpy.data.objects.new(mesh.name,mesh)
    builder.scene.collection.objects.link(slab)
    slab.data.materials.append(builder.concrete)
    slab["role"] = "founded equipment slab with approach bevel"
    builder.built.append(slab)

    for a,b in ((left,left+.18),(right-.18,right)):
        box("side curb",(a,back,b,front-.19),FLOOR,.185,builder.concrete,"slab-supported wall curb")
        side = box("side cladding",(a,back,b,front-.16),.185,2.90,cladding,"curb-supported side elevation")
        incline(side,2.90)
        for z in (back+.15,back+1.25,back+2.40,front-.075):
            box("side frame post",(a,z-.075,b,z+.075),FLOOR,grade(z),frame,"roof-bearing frame post")
    box("rear curb",(left,back,right,back+.18),FLOOR,.185,builder.concrete,"slab-supported rear curb")
    rear = box("rear steel enclosure",(left,back,right,back+.18),.185,2.65,cladding,"curb-supported rear elevation")
    incline(rear,2.65)
    for x in (left+1.34,left+2.8,left+4.24):
        box("rear framing stud",(x-.07,back+.08,x+.07,back+.23),FLOOR,grade(back+.155),frame,"framed equipment backing")
    box("open approach header",(left,front-.16,right,front),2.79,grade(front-.075),frame,"header over full-width approach")

    for z in (back+.10,back+1.25,back+2.40,front-.075):
        purlin = box("roof purlin",(left,z-.07,right,z+.07),2.67,2.805,frame,"roof bearing across side posts")
        incline(purlin,2.67,True)
    top = box("continuous shed roof",(left-.18,back-.18,right+.18,front+.18),2.805,2.885,roofing,
              "roof seated on four bearing purlins")
    incline(top,2.67,True)
    box("light-bearer blocking",(36.19,69.60,37.61,69.91),2.64,grade(69.79)+.015,frame,
        "ceiling fixture blocking carried by a purlin")
    builder.originals.update(obj.name for obj in builder.built)
    bpy.context.view_layer.update()
    builder.deps = bpy.context.evaluated_depsgraph_get()


class MineElectrical(Assembly):
    def install_supply(self):
        package = LookoutElectrical()
        equipment = package.fit_plant()
        rotation = Matrix.Translation(point(73.55,0,136.95)) @ Matrix.Rotation(math.pi,4,"Z")
        branch = "shelter lighting branch on wall and purlin"
        fittings = [obj for obj in package.built if obj.name.startswith("College "+branch)]
        assert len(fittings)==3
        for obj in fittings:
            package.built.remove(obj)
            bpy.data.objects.remove(obj,do_unlink=True)
        package.routes.remove(next(route for route in package.routes if route["id"]==branch))
        self.built.extend(package.built)
        for obj in package.built:
            obj.matrix_world = rotation @ obj.matrix_world
            obj["zone"] = ENTRY["id"]
        for row in package.sockets:
            row["id"] = row["id"].replace("lookout-","mine-")
            row["position"] = plant_coordinates(*row["position"])
            row["yaw"] = (row["yaw"]+math.pi) % (2*math.pi)
            row["style"] = row["style"].replace("WP-","MC-")
        self.sockets.extend(package.sockets)
        for route in package.routes:
            route["points"] = [plant_coordinates(*p) for p in route["points"]]
        self.routes.extend(package.routes)
        for row in equipment.values():
            row["position"] = plant_coordinates(*row["position"])
            row["yaw"] = (row["yaw"]+math.pi) % (2*math.pi)
            low, high = row["bounds"]
            row["bounds"] = [plant_coordinates(high[0],low[1],high[2]),plant_coordinates(low[0],high[1],low[2])]
        bpy.context.view_layer.update()
        return equipment


def build(out):
    source = json.loads((BASE/"construction.json").read_bytes())
    for kind,key in (("blend","blend_sha256"),("glb","glb_sha256"),("native","physics_sha256")):
        assert sha(ROOT/source[kind])==source[key]
    bpy.ops.wm.open_mainfile(filepath=str(ROOT/source["blend"]))
    bpy.context.preferences.filepaths.save_version = 0
    builder = MineElectrical()
    bearings = bearing_survey(builder)
    build_shelter(builder)
    equipment = builder.install_supply()
    light = plant_coordinates(36.68,2,67.16)
    builder.mounted_lamp(ENTRY,"mine-service-light",point(*light),Vector((0,0,1)),1.6,point(1,0,0))
    x, y, z = builder.loads[0]["mount"]
    builder.raceway("shed light branch on rear wall, sidewall and soffit",
                    [(34.97,2.292,67.57),(34.15,2.292,67.57),(34.15,2.292,z),
                     (34.15,2.923,z),(36.148,2.923,z),(36.148,y,z),(x,y,z)],.022)
    for at in (68.0,68.72,69.45):
        builder.box("sidewall conduit saddle",(34.102,at-.04,34.182,at+.04),2.262,2.325,
                    builder.mount,"conduit secured to sidewall",ENTRY["id"])
    for at in (34.42,35.21,35.94):
        builder.box("soffit conduit saddle",(at-.04,z-.034,at+.04,z+.034),2.896,2.962,
                    builder.mount,"conduit clipped to continuous overhead beam",ENTRY["id"])
    builder.box("blocking-mounted lamp junction",(36.131,z-.046,36.208,z+.046),y-.034,y+.052,
                builder.mount,"boxed termination secured to fixture blocking",ENTRY["id"])
    for obj in builder.built:
        if obj.name.startswith("College "):
            obj.name = "Mine "+obj.name.removeprefix("College ")
        elif obj.name.startswith("Lookout "):
            obj.name = "Mine "+obj.name.removeprefix("Lookout ")
    builder.loads[0]["circuit"] = "MC-YARD"

    out.mkdir(parents=True,exist_ok=True)
    exports = {ext:out/f"mine-service-yard.{ext}" for ext in ("blend","glb","hcs2")}
    addition = out/"_additions.glb"
    export(addition,builder.built)
    append(ROOT/source["glb"],addition,exports["glb"],reuse_embedded_images=True)
    addition.unlink()
    native_write(ROOT/source["native"],exports["hcs2"],set(),builder.built,
                 open_components={o.name for o in builder.built if o.get("source_asset")})
    before = native_records(ROOT/source["native"])
    after = native_records(exports["hcs2"])
    assert after[:len(before)]==before
    bpy.ops.file.pack_all()
    bpy.ops.wm.save_as_mainfile(filepath=str(exports["blend"]),compress=True)
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.import_scene.gltf(filepath=str(exports["glb"]))
    for part in after[len(before):]:
        assert match_geometry(triangles(part),[t for t,_ in records(bpy.context.scene.objects[part[0]])]),part[0]
    hashes = {ext:sha(path) for ext,path in exports.items()}
    rows = [("SOURCE",exports["glb"].relative_to(ROOT).as_posix(),hashes["glb"],
             exports["hcs2"].relative_to(ROOT).as_posix(),hashes["hcs2"])]
    rows.extend(line.split("\t") for line in (BASE/"arrival.tsv").read_text().splitlines()[1:])
    rows.extend(("SERVICE",s["id"],s["type"],s["room"],*s["position"],s["yaw"],s["style"]) for s in builder.sockets)
    rows.extend(("POINT",name,*p) for name,points in APPROACHES.items() for p in points)
    table(out/"arrival.tsv",rows)
    for name in ("mission.tsv","door-survey.tsv","rail-probes.tsv"):
        (out/name).write_bytes((BASE/name).read_bytes())
    details = dict(exports=hashes,retained_components=len(before),added_components=len(after)-len(before),
                   bearings=bearings,shelter=GROUND,approaches=APPROACHES,plant=equipment,
                   loads=builder.loads,sockets=builder.sockets,routes=builder.routes)
    (out/"electrical.json").write_text(json.dumps(details,indent=2)+"\n",encoding="utf-8",newline="\n")
    print("PASS Mine supply shelter bears on",bearings,"verified ground points;",len(before),"unchanged native components")


def capture_structure(out):
    bpy.ops.wm.open_mainfile(filepath=str(out/"mine-service-yard.blend"))
    scene = bpy.context.scene
    scene.render.engine = "BLENDER_WORKBENCH"
    shading = scene.display.shading
    shading.color_type = "MATERIAL"
    shading.show_cavity = True
    shading.show_shadows = True
    shading.cavity_type = "BOTH"
    scene.view_settings.view_transform = "Standard"
    scene.render.resolution_x = 1200
    scene.render.resolution_y = 850
    scene.render.resolution_percentage = 100
    scene.render.image_settings.file_format = "PNG"
    bpy.ops.object.camera_add()
    camera = bpy.context.object
    camera.data.type = "ORTHO"
    scene.camera = camera
    folder = out/"captures"/"structure"
    folder.mkdir(parents=True,exist_ok=True)
    aim = point(36.75,1.32,69.05)
    for name,eye,scale in (
            ("service-yard-van-side",point(29.5,5.6,79),12.5),
            ("service-yard-framing-cutaway",point(33.1,7.7,77.3),9.6)):
        if name.endswith("cutaway"):
            for obj in scene.objects:
                if obj.get("role")=="roof seated on four bearing purlins":
                    obj.hide_render = True
        camera.location = eye
        camera.rotation_euler = (aim-eye).to_track_quat("-Z","Y").to_euler()
        camera.data.ortho_scale = scale
        scene.render.filepath = str(folder/(name+".png"))
        bpy.ops.render.render(write_still=True)


if __name__=="__main__":
    args = sys.argv[sys.argv.index("--")+1:]
    output = Path(args[0]).resolve()
    if "--captures-only" in args:
        capture_structure(output)
    else:
        build(output)
